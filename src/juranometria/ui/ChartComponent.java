package juranometria.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartRenderer;

/**
 * The atlas page. Holds pre-loaded chart content and delegates painting to
 * the renderer; painting fetches nothing and mutates nothing.
 */
public final class ChartComponent extends JComponent {

    private final ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
    private final SkyPosition centre;
    private final String title;
    private final List<Star> stars;
    private final List<DeepSkyObject> deepSkyObjects;
    private ChartViewState viewState = ChartViewState.DEFAULT;

    public ChartComponent(SkyPosition centre, String title,
                          List<Star> stars, List<DeepSkyObject> deepSkyObjects) {
        this.centre = centre;
        this.title = title;
        this.stars = List.copyOf(stars);
        this.deepSkyObjects = List.copyOf(deepSkyObjects);
        setOpaque(true);
        setPreferredSize(new Dimension(900, 700));
        getAccessibleContext().setAccessibleName("Star chart");
    }

    /** Adopts a new view state and repaints; the scene is rebuilt on paint. */
    public void setViewState(ChartViewState viewState) {
        if (viewState == null) {
            throw new IllegalArgumentException("view state must not be null");
        }
        this.viewState = viewState;
        repaint();
    }

    public ChartViewState viewState() {
        return viewState;
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
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        ChartScene scene = new ChartScene(
                new ChartViewport(centre, viewState.fieldWidthDegrees(),
                        getWidth(), getHeight()),
                stars, deepSkyObjects, title, viewState.limitingMagnitude());
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            renderer.render(g2, scene);
        } finally {
            g2.dispose();
        }
    }
}
