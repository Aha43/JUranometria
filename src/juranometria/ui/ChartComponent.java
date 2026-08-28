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
        scene = assembler.assemble(viewState, getWidth(), getHeight());
        repaint();
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
                || scene.viewport().heightPx() != getHeight()) {
            // A resize event is already on its way for this geometry; skip
            // the stale frame rather than querying inside painting.
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            renderer.render(g2, scene);
        } finally {
            g2.dispose();
        }
    }
}
