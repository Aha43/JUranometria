package juranometria.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import juranometria.chart.ChartScene;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.GnomonicProjection;
import juranometria.project.GreatCirclePage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;

/**
 * How the chart inks a module's reference geometry (Sprint 25,
 * issue #227).
 *
 * <p>The module says <em>where</em>, <em>what for</em> and
 * <em>what it is</em>; the chart decides what that looks like, where
 * it sits in the stack and whether it is drawn at all. Nothing here
 * knows what a meridian or a horizon is: it is given poles, points
 * and names.
 *
 * <p>The ink was chosen by drawing it over real pages in both themes
 * (docs/decisions/place-and-time.md):
 *
 * <ul>
 *   <li>a line across the sky: solid, 1 px, the grey the chart
 *       already uses for constellation figures;</li>
 *   <li>a boundary of what can be seen: dashed 6-on 4-off, the same
 *       weight and grey - a boundary of visibility is not a thing in
 *       the sky;</li>
 *   <li>a reference point: a small open ring with an upward tick - a
 *       <em>place</em>, and deliberately not the cross Sprint 24
 *       uses for working marks.</li>
 * </ul>
 *
 * <p>Labels are the geometry's own accessible name, drawn once where
 * the line leaves the paper, in the grid-label grey. They take no
 * part in the star-label collision policy: reference ink is
 * furniture, and a meridian that displaced a star's name would be
 * the observer editing the sky.
 *
 * <p><strong>Off the page is silence.</strong> On most pages none of
 * this crosses the paper at all, and the chart draws nothing rather
 * than promising a line that is not there. The clipping is analytic
 * - a great circle is straight under this projection - so what is on
 * the paper is decided by the geometry and never by a threshold in
 * pixels.
 */
public final class ReferenceInk {

    private ReferenceInk() {
    }

    private static final BasicStroke SOLID = new BasicStroke(1.0f);

    /** Six on, four off: seen as a boundary, read as a line. */
    private static final BasicStroke DASHED = new BasicStroke(1.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {6.0f, 4.0f}, 0.0f);

    /**
     * Long dash, short dot: the cartographic datum line.
     *
     * <p>Chosen by the Sprint 28 gate by drawing the candidates over
     * production pages beside the meridian, in both grounds
     * (docs/decisions/ecliptic.md). It is distinct from all three
     * lines it must not be confused with: the meridian's solid, the
     * horizon's even dash, and the constellation boundaries' fine
     * dots - which is what disqualified a plain dotted candidate.
     */
    private static final BasicStroke DASH_DOT = new BasicStroke(1.0f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {12.0f, 4.0f, 2.0f, 4.0f}, 0.0f);

    /** The zenith ring and its tick, in pixels. */
    private static final double RING = 5.0;
    private static final double TICK = 4.0;

    /** Half the diagonal of a landmark's diamond, in pixels. */
    private static final double DIAMOND = 6.0;

    /** How far a label sits off the paper's edge. */
    private static final double LABEL_INSET = 4.0;

    /**
     * Paints every contributed reference geometry.
     *
     * <p>The order is the chart's, not the modules'. Circles are
     * drawn before points, so a zenith ring is never buried under a
     * line through it, and within each kind the contributions are
     * taken in key order rather than in the order their modules
     * happened to attach. Two modules attached the other way round
     * produce the same page.
     */
    public static void paint(Graphics2D g, ChartScene scene,
                      List<OverlayRegistry.Owned> contributions,
                      juranometria.render.ChartPalette palette) {
        if (contributions.isEmpty()) {
            return;
        }
        List<OverlayRegistry.Owned> reference = new ArrayList<>();
        for (OverlayRegistry.Owned owned : contributions) {
            if (owned.geometry().role() == InkRole.REFERENCE_LINE) {
                reference.add(owned);
            }
        }
        if (reference.isEmpty()) {
            return;
        }
        reference.sort(Comparator.comparing(OverlayRegistry.Owned::key));

        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        Rectangle2D paper = ChartRenderer.paperOf(scene);
        GreatCirclePage.Page page = new GreatCirclePage.Page(paper.getMinX(),
                paper.getMinY(), paper.getMaxX(), paper.getMaxY());

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(paper);
            for (OverlayRegistry.Owned owned : reference) {
                if (owned.geometry()
                        instanceof OverlayContribution.GreatCircle circle) {
                    drawCircle(g2, projection, mapping, page, paper, circle,
                            palette);
                }
            }
            for (OverlayRegistry.Owned owned : reference) {
                if (owned.geometry()
                        instanceof OverlayContribution.Point point) {
                    drawPoint(g2, projection, mapping, paper, point,
                            palette);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    private static void drawCircle(Graphics2D g,
                                   GnomonicProjection projection,
                                   ViewportMapping mapping,
                                   GreatCirclePage.Page page,
                                   Rectangle2D paper,
                                   OverlayContribution.GreatCircle circle,
                                   juranometria.render.ChartPalette palette) {
        Optional<GreatCirclePage.Arc> crossing = GreatCirclePage.clip(
                projection, mapping, page, circle.pole());
        if (crossing.isEmpty()) {
            // Silence. The circle does not cross this page, and a
            // line drawn anyway would be a promise the sky has not
            // made.
            return;
        }
        GreatCirclePage.Arc arc = crossing.get();
        g.setColor(palette.figureInk());
        g.setStroke(strokeFor(circle.reference()));
        g.draw(new Line2D.Double(arc.from().x(), arc.from().y(),
                arc.to().x(), arc.to().y()));
        label(g, paper, arc, circle.accessibleName(), palette);
    }

    /**
     * What each kind of reference line looks like.
     *
     * <p>The chart's decision, from the module's statement of what
     * the geometry is: a line across the sky, the boundary of what
     * can be seen of it, or a permanent circle of the sphere.
     */
    private static BasicStroke strokeFor(
            OverlayContribution.Reference reference) {
        return switch (reference) {
            case LINE -> SOLID;
            case BOUNDARY -> DASHED;
            case PERMANENT -> DASH_DOT;
        };
    }

    /**
     * The line's own name, once, where it leaves the paper.
     *
     * <p>At the <strong>upper</strong> end, and at the right one if
     * they are level. A rule, so that two runs of the same page put
     * the word in the same place and a reader learns where to look -
     * and the upper end rather than the lower because the lower edge
     * is where the grid writes its right-ascension notation and the
     * title block sits. Reference ink takes no part in the star-label
     * collision policy, which is a decision about not displacing the
     * sky; it is no argument for printing a word on top of the
     * chart's own furniture.
     */
    private static void label(Graphics2D g, Rectangle2D paper,
                              GreatCirclePage.Arc arc, String name,
                              juranometria.render.ChartPalette palette) {
        g.setColor(palette.gridLabelInk());
        g.setFont(EquatorialGrid.GRID_LABEL_FONT);
        Rectangle2D box = labelBox(paper, arc, name,
                g.getFontMetrics());
        g.drawString(name, (float) box.getMinX(),
                (float) (box.getMaxY() - g.getFontMetrics().getDescent()));
    }

    /**
     * Where this arc's name lands on the page: at the upper end (the
     * right one if they are level), inset and clamped to the paper.
     *
     * <p>Public, because the journey that audits the page needs the
     * same truth the painter uses - a test that re-guessed the
     * layout would either drift from it or have to allow ink a broad
     * catchment around every anchor, and a review rightly refused
     * the catchment.
     */
    public static Rectangle2D labelBox(Rectangle2D paper,
                                       GreatCirclePage.Arc arc,
                                       String name, FontMetrics metrics) {
        PixelPoint end = arc.to().y() < arc.from().y()
                || (arc.to().y() == arc.from().y()
                        && arc.to().x() > arc.from().x())
                ? arc.to() : arc.from();
        double width = metrics.stringWidth(name);
        double x = Math.min(Math.max(end.x() + LABEL_INSET,
                        paper.getMinX() + LABEL_INSET),
                paper.getMaxX() - LABEL_INSET - width);
        double baseline = Math.min(Math.max(end.y() + LABEL_INSET
                        + metrics.getAscent(),
                        paper.getMinY() + LABEL_INSET + metrics.getAscent()),
                paper.getMaxY() - LABEL_INSET);
        return new Rectangle2D.Double(x,
                baseline - metrics.getAscent(), width,
                metrics.getAscent() + metrics.getDescent());
    }

    private static void drawPoint(Graphics2D g,
                                  GnomonicProjection projection,
                                  ViewportMapping mapping,
                                  Rectangle2D paper,
                                  OverlayContribution.Point point,
                                  juranometria.render.ChartPalette palette) {
        PixelPoint at = projection.project(point.at())
                .map(mapping::toPixel).orElse(null);
        if (at == null || !paper.contains(at.x(), at.y())) {
            return;
        }
        g.setColor(palette.figureInk());
        g.setStroke(SOLID);
        switch (point.mark()) {
            case PLACE -> {
                g.draw(new Ellipse2D.Double(at.x() - RING, at.y() - RING,
                        2.0 * RING, 2.0 * RING));
                // Upward, because a place overhead has a direction
                // and a ring alone would read as an object.
                g.draw(new Line2D.Double(at.x(), at.y() - RING,
                        at.x(), at.y() - RING - TICK));
            }
            // An open diamond: no other mark on this chart is one.
            // Stars are filled discs, deep-sky objects are ellipses,
            // dotted and crossed circles, boxes and spoked squares,
            // a working mark is a gapped cross, and a place is the
            // ring and tick above. It reads as a marked position on
            // a line rather than as a place or an object.
            case LANDMARK -> g.draw(diamond(at));
        }
        label(g, paper, new GreatCirclePage.Arc(at, at),
                point.accessibleName(), palette);
    }

    /** A landmark's open diamond, about its position. */
    private static java.awt.geom.Path2D diamond(PixelPoint at) {
        java.awt.geom.Path2D shape = new java.awt.geom.Path2D.Double();
        shape.moveTo(at.x(), at.y() - DIAMOND);
        shape.lineTo(at.x() + DIAMOND, at.y());
        shape.lineTo(at.x(), at.y() + DIAMOND);
        shape.lineTo(at.x() - DIAMOND, at.y());
        shape.closePath();
        return shape;
    }
}
