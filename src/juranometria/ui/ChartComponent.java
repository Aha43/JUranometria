package juranometria.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
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
    private final double fieldWidthDegrees;
    private final List<Star> stars;

    public ChartComponent(SkyPosition centre, double fieldWidthDegrees, List<Star> stars) {
        this.centre = centre;
        this.fieldWidthDegrees = fieldWidthDegrees;
        this.stars = List.copyOf(stars);
        setOpaque(true);
        setPreferredSize(new Dimension(900, 700));
        getAccessibleContext().setAccessibleName("Star chart");
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
                new ChartViewport(centre, fieldWidthDegrees, getWidth(), getHeight()),
                stars);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            renderer.render(g2, scene);
        } finally {
            g2.dispose();
        }
    }
}
