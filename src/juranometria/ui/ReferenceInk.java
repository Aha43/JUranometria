package juranometria.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
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
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.CurveRun;
import juranometria.project.GreatCirclePage;
import juranometria.project.PageRegion;
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

        Projection projection =
                Projections.forViewport(scene.viewport());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        Rectangle2D paper = ChartRenderer.paperOf(scene);
        // The paper, and the limb if this projection has one: a
        // curve is clipped to where there is sky, not only to where
        // there is paper.
        PageRegion region = mapping.regionFor(scene.viewport(), projection);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(paper);
            // Circles first, then their names, then the points.
            //
            // The names are placed together rather than each with its
            // own line, because at an overview's fields four
            // reference lines leave the paper near the same corner
            // and a name written where the last one already is is not
            // a name. Production has never needed the rule - at 42
            // degrees no page carries more than two - so it is
            // written to change nothing there, and the released pages
            // are the check that it does not
            // (docs/decisions/overview-projection.md).
            for (OverlayRegistry.Owned owned : reference) {
                if (owned.geometry()
                        instanceof OverlayContribution.GreatCircle circle) {
                    drawCircle(g2, projection, mapping, region, circle,
                            palette);
                }
            }
            // The names are the published decision, written rather
            // than decided again here: one placement, drawn by this
            // and readable by anyone (issue #313).
            List<Rectangle2D> taken = new ArrayList<>();
            g2.setColor(palette.gridLabelInk());
            g2.setFont(EquatorialGrid.GRID_LABEL_FONT);
            FontMetrics metrics = g2.getFontMetrics();
            for (NamePlacement placed
                    : namePlacements(scene, contributions)) {
                g2.drawString(placed.name(),
                        (float) placed.box().getMinX(),
                        (float) (placed.box().getMaxY()
                                - metrics.getDescent()));
                taken.add(placed.box());
            }
            for (OverlayRegistry.Owned owned : reference) {
                if (owned.geometry()
                        instanceof OverlayContribution.Point point) {
                    drawPoint(g2, projection, mapping, paper, point,
                            taken, palette);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    /** A line's name, and the end of it the name belongs to. */
    private record Named(PixelPoint anchor, String name,
                         String moduleId) {
    }

    private static void drawCircle(Graphics2D g,
                                   Projection projection,
                                   ViewportMapping mapping,
                                   PageRegion region,
                                   OverlayContribution.GreatCircle circle,
                                   juranometria.render.ChartPalette palette) {
        List<CurveRun> runs = GreatCirclePage.clip(projection, mapping,
                region, circle.pole());
        if (runs.isEmpty()) {
            // Silence. The circle does not cross this page, and a
            // line drawn anyway would be a promise the sky has not
            // made.
            return;
        }
        g.setColor(palette.figureInk());
        g.setStroke(strokeFor(circle.reference()));
        for (CurveRun run : runs) {
            g.draw(shapeOf(run));
        }
    }

    /** One reference name, and the box it is written in. */
    public record NamePlacement(String moduleId, String name,
                                Rectangle2D box) {
    }

    /**
     * Where this page's reference names go, without drawing them.
     *
     * <p>The reference layer draws its curves and their names under
     * one reader switch, so nothing outside it could tell the two
     * apart - a study measuring what a name covered got the curve as
     * well (issue #310). This publishes the names' own decisions, the
     * way the star-label pass has published its since #154, and
     * {@link #paint} writes precisely this list.
     */
    public static List<NamePlacement> namePlacements(ChartScene scene,
            List<OverlayRegistry.Owned> contributions) {
        List<OverlayRegistry.Owned> reference = referenceOf(contributions);
        if (reference.isEmpty()) {
            return List.of();
        }
        Projection projection = Projections.forViewport(scene.viewport());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        Rectangle2D paper = ChartRenderer.paperOf(scene);
        PageRegion region = mapping.regionFor(scene.viewport(), projection);
        List<Named> names = new ArrayList<>();
        for (OverlayRegistry.Owned owned : reference) {
            if (owned.geometry()
                    instanceof OverlayContribution.GreatCircle circle) {
                PixelPoint anchor = labelAnchor(GreatCirclePage.clip(
                        projection, mapping, region, circle.pole()));
                if (anchor != null) {
                    names.add(new Named(anchor, circle.accessibleName(),
                            owned.moduleId()));
                }
            }
        }
        FontMetrics metrics = EquatorialGrid.labelMetrics();
        List<NamePlacement> placed = new ArrayList<>();
        List<Rectangle2D> taken = new ArrayList<>();
        for (Named named : names) {
            Rectangle2D box = boxFor(paper, named.anchor(), named.name(),
                    metrics, taken);
            if (box != null) {
                taken.add(box);
                placed.add(new NamePlacement(named.moduleId(),
                        named.name(), box));
            }
        }
        return List.copyOf(placed);
    }

    private static List<OverlayRegistry.Owned> referenceOf(
            List<OverlayRegistry.Owned> contributions) {
        List<OverlayRegistry.Owned> reference = new ArrayList<>();
        for (OverlayRegistry.Owned owned : contributions) {
            if (owned.geometry().role() == InkRole.REFERENCE_LINE) {
                reference.add(owned);
            }
        }
        reference.sort(Comparator.comparing(OverlayRegistry.Owned::key));
        return reference;
    }

    /**
     * Where one name fits, or null when the page has no room left
     * below the ones already written. The decision, with no graphics
     * in it, so it can be published as well as drawn.
     */
    private static Rectangle2D boxFor(Rectangle2D paper, PixelPoint anchor,
                                      String name, FontMetrics metrics,
                                      List<Rectangle2D> taken) {
        double line = metrics.getHeight();
        Rectangle2D box = labelBox(paper, anchor, name, metrics);
        while (overlaps(box, taken)
                && box.getMaxY() + line <= paper.getMaxY()) {
            box = new Rectangle2D.Double(box.getX(), box.getY() + line,
                    box.getWidth(), box.getHeight());
        }
        return overlaps(box, taken) ? null : box;
    }

    /**
     * One name written where {@link #boxFor} says it goes, or not
     * written at all - the placement rule the curves' names publish,
     * used here for a point's name so there is one rule and not two.
     */
    private static void write(Graphics2D g, Rectangle2D paper,
                              PixelPoint anchor, String name,
                              List<Rectangle2D> taken,
                              juranometria.render.ChartPalette palette) {
        g.setColor(palette.gridLabelInk());
        g.setFont(EquatorialGrid.GRID_LABEL_FONT);
        FontMetrics metrics = g.getFontMetrics();
        Rectangle2D box = boxFor(paper, anchor, name, metrics, taken);
        if (box == null) {
            return;
        }
        taken.add(box);
        g.drawString(name, (float) box.getMinX(),
                (float) (box.getMaxY() - metrics.getDescent()));
    }

    private static boolean overlaps(Rectangle2D box,
                                    List<Rectangle2D> taken) {
        for (Rectangle2D each : taken) {
            if (each.intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A run, as something Java2D can draw.
     *
     * <p>The projection package says where the ink goes in numbers
     * and never in shapes: it draws nothing, and a scan of its
     * compiled classes holds it to that. Turning a run into a shape
     * is the chart's, which is the older rule as well - a module says
     * where, a projection says where that lands, and the chart
     * decides what it looks like.
     */
    public static Shape shapeOf(CurveRun run) {
        if (run instanceof CurveRun.Segment segment) {
            return new Line2D.Double(segment.start().x(),
                    segment.start().y(), segment.end().x(),
                    segment.end().y());
        }
        CurveRun.Arc arc = (CurveRun.Arc) run;
        // Java2D measures its arcs anticlockwise from the positive x
        // axis, and a page's y runs down, so the same angles arrive
        // negated.
        Arc2D.Double drawn = new Arc2D.Double(
                -arc.radiusAlong(), -arc.radiusAcross(),
                2.0 * arc.radiusAlong(), 2.0 * arc.radiusAcross(),
                Math.toDegrees(-arc.startRadians()),
                Math.toDegrees(-arc.spanRadians()), Arc2D.OPEN);
        AffineTransform onto = new AffineTransform();
        onto.translate(arc.centreX(), arc.centreY());
        onto.rotate(arc.tiltRadians());
        return onto.createTransformedShape(drawn);
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
     * A line's own name goes where the line leaves the paper.
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

    /**
     * The end a name hangs on: the upper one, and the right one if
     * they are level, across every run the page cut the curve into.
     *
     * <p>Null when the curve closed on itself and has no ends at all.
     */
    public static PixelPoint labelAnchor(List<CurveRun> runs) {
        PixelPoint best = null;
        for (CurveRun run : runs) {
            for (Optional<PixelPoint> end
                    : List.of(run.from(), run.to())) {
                if (end.isEmpty()) {
                    continue;
                }
                PixelPoint at = end.get();
                if (best == null || at.y() < best.y()
                        || (at.y() == best.y() && at.x() > best.x())) {
                    best = at;
                }
            }
        }
        return best;
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
                                       PixelPoint end,
                                       String name, FontMetrics metrics) {
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
                                  Projection projection,
                                  ViewportMapping mapping,
                                  Rectangle2D paper,
                                  OverlayContribution.Point point,
                                  List<Rectangle2D> taken,
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
        // A landmark's name keeps clear of the lines' names as well
        // as of the other landmarks': the marks are drawn in their
        // own order and only the words are placed against what is
        // already written.
        write(g, paper, at, point.accessibleName(), taken, palette);
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
