package juranometria.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartRenderer;

/**
 * The atlas page. Scenes are assembled by the {@link SceneAssembler} when
 * the view state or the component size changes; painting only renders the
 * current scene and performs no catalogue query. Repainting an unchanged
 * view reuses the assembled scene untouched.
 *
 * The page's height is capped by the assembler's coverage rule: in a
 * window taller than the bundled data can honestly fill, the page is
 * letterboxed on the theme's neutral surface instead of showing silently
 * sparse sky.
 */
public final class ChartComponent extends JComponent {

    private final ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
    private final SceneAssembler assembler;
    private ChartViewState viewState = ChartViewState.DEFAULT;
    private juranometria.render.ChartOptions chartOptions =
            juranometria.render.ChartOptions.DEFAULTS;
    private ChartScene scene;
    private String highlighted;

    public ChartComponent(SceneAssembler assembler) {
        if (assembler == null) {
            throw new IllegalArgumentException("scene assembler must not be null");
        }
        this.assembler = assembler;
        setOpaque(true);
        setPreferredSize(new Dimension(900, 700));
        setBackground(javax.swing.UIManager.getColor("Panel.background") != null
                ? javax.swing.UIManager.getColor("Panel.background")
                : java.awt.Color.LIGHT_GRAY);
        getAccessibleContext().setAccessibleName("Star chart");
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                assembleScene();
            }
        });
    }

    /** Adopts a new view state, assembles its scene, and repaints. */
    public void setViewState(ChartViewState viewState) {
        if (viewState == null) {
            throw new IllegalArgumentException("view state must not be null");
        }
        this.viewState = viewState;
        assembleScene();
    }

    public ChartViewState viewState() {
        return viewState;
    }

    /**
     * Adopts the reader's chart options and repaints. Options are
     * presentation state consumed by the renderer: no scene assembly,
     * no catalogue or geography query, no navigation change
     * (docs/decisions/chart-options.md).
     */
    public void setChartOptions(juranometria.render.ChartOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("chart options must not be null");
        }
        this.chartOptions = options;
        repaint();
    }

    public juranometria.render.ChartOptions chartOptions() {
        return chartOptions;
    }

    ChartScene scene() {
        return scene;
    }

    /** The page as assembled, for consumers outside this package. */
    public ChartScene currentScene() {
        return scene;
    }

    /**
     * The object the reader has selected, marked on the page (issue
     * #170). Presentation only: it changes no view state, assembles
     * no scene, and asks the catalogue nothing.
     */
    public void setHighlightedObject(String catalogueId) {
        if (java.util.Objects.equals(highlighted, catalogueId)) {
            return;
        }
        this.highlighted = catalogueId;
        repaint();
    }

    private void assembleScene() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        int pageHeight = Math.min(getHeight(), assembler.maxPageHeightPx(
                viewState.centre(), viewState.fieldWidthDegrees(), getWidth()));
        scene = assembler.assemble(viewState, getWidth(), pageHeight);
        repaint();
    }

    /** Top of the paper page inside the (possibly letterboxed) canvas. */
    public int pageOffsetY() {
        return scene == null ? 0
                : (getHeight() - scene.viewport().heightPx()) / 2;
    }

    /** Whether a component point lies on the paper page, not the chrome. */
    boolean isOnPaper(java.awt.Point point) {
        if (scene == null) {
            return false;
        }
        int top = pageOffsetY();
        return point.x >= 0 && point.x < scene.viewport().widthPx()
                && point.y >= top
                && point.y < top + scene.viewport().heightPx();
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        // Plain JComponent subclasses supply no accessible context of
        // their own; provide one so the chart is exposed as a canvas.
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.CANVAS;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (scene == null || scene.viewport().widthPx() != getWidth()
                || scene.viewport().heightPx() != Math.min(getHeight(),
                        assembler.maxPageHeightPx(viewState.centre(),
                                viewState.fieldWidthDegrees(), getWidth()))) {
            // A resize event is already on its way for this geometry; skip
            // the stale frame rather than querying inside painting.
            return;
        }
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(0, pageOffsetY());
            renderer.render(g2, scene, chartOptions);
            renderer.drawSelectionHighlight(g2, scene, chartOptions,
                    highlighted);
        } finally {
            g2.dispose();
        }
    }
}
