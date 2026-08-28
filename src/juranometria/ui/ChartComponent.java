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
    private ChartScene scene;

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

    ChartScene scene() {
        return scene;
    }

    private void assembleScene() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        int pageHeight = Math.min(getHeight(),
                assembler.maxPageHeightPx(viewState.fieldWidthDegrees(), getWidth()));
        scene = assembler.assemble(viewState, getWidth(), pageHeight);
        repaint();
    }

    private int pageOffsetY() {
        return (getHeight() - scene.viewport().heightPx()) / 2;
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
                        assembler.maxPageHeightPx(viewState.fieldWidthDegrees(), getWidth()))) {
            // A resize event is already on its way for this geometry; skip
            // the stale frame rather than querying inside painting.
            return;
        }
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(0, pageOffsetY());
            renderer.render(g2, scene);
        } finally {
            g2.dispose();
        }
    }
}
