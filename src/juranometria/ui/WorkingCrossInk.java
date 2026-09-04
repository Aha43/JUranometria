package juranometria.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;

import juranometria.chart.ChartScene;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

/**
 * How the chart inks a module's interaction geometry (Sprint 24,
 * issue #216).
 *
 * <p>The module says <em>where</em> and <em>what for</em>; the chart
 * decides what that looks like. That division is the whole point of
 * the seam: a module handed a {@code Graphics2D} would be inventing
 * cartography, and this class is the atlas keeping that decision.
 *
 * <p>A working cross is <strong>restrained</strong> - two short
 * strokes with a gap at the middle, so the position it marks stays
 * visible through it - and it is <strong>not</strong> cartographic
 * vocabulary. It carries no label, takes no part in label collision,
 * and never appears in ordinary or reference rendering: it is
 * painted here, after the chart, rather than by the renderer.
 *
 * <p>The lead wears the chart's existing selection treatment, so a
 * reader reading one object's facts sees the same ring whether that
 * object has a symbol of its own or only a cross.
 */
final class WorkingCrossInk {

    private WorkingCrossInk() {
    }

    private static final BasicStroke STROKE = new BasicStroke(1.0f);
    private static final BasicStroke LEAD_STROKE = new BasicStroke(1.5f);

    /** Arm and gap, in pixels: seen, and not shouted. */
    private static final double ARM = 7.0;
    private static final double GAP = 3.0;
    private static final double LEAD_RING = 9.0;

    /**
     * Paints every contributed interaction point, in the order the
     * chart collected them.
     *
     * @param leadIdentity the object whose facts a reader is
     *     reading, which wears the selection treatment; may be null
     */
    static void paint(Graphics2D g, ChartScene scene,
                      List<OverlayRegistry.Owned> contributions,
                      String leadIdentity,
                      juranometria.render.ChartPalette palette) {
        if (contributions.isEmpty()) {
            return;
        }
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        java.awt.geom.Rectangle2D paper =
                juranometria.render.ChartRenderer.paperOf(scene);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            // As prominent as a star on either ground: the cross is
            // the reader's own mark and must never sink into the sky.
            g2.setColor(palette.interactionInk());
            for (OverlayRegistry.Owned owned : contributions) {
                if (owned.geometry().role() != InkRole.INTERACTION
                        || !(owned.geometry()
                                instanceof OverlayContribution.Point point)) {
                    // The other roles are for modules this sprint
                    // does not build. Silence is the honest response
                    // to geometry the chart has not decided how to
                    // ink yet.
                    continue;
                }
                PixelPoint at = projection.project(point.at())
                        .map(mapping::toPixel).orElse(null);
                if (at == null || !paper.contains(at.x(), at.y())) {
                    continue;
                }
                boolean leads = point.identity().equals(leadIdentity);
                g2.setStroke(leads ? LEAD_STROKE : STROKE);
                cross(g2, at.x(), at.y());
                if (leads) {
                    g2.draw(new Ellipse2D.Double(at.x() - LEAD_RING,
                            at.y() - LEAD_RING,
                            2.0 * LEAD_RING, 2.0 * LEAD_RING));
                }
            }
        } finally {
            g2.dispose();
        }
    }

    /** Four strokes around a gap, never through the position. */
    private static void cross(Graphics2D g, double x, double y) {
        g.draw(new Line2D.Double(x - ARM, y, x - GAP, y));
        g.draw(new Line2D.Double(x + GAP, y, x + ARM, y));
        g.draw(new Line2D.Double(x, y - ARM, x, y - GAP));
        g.draw(new Line2D.Double(x, y + GAP, x, y + ARM));
    }
}
