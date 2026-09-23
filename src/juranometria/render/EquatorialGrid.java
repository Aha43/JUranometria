package juranometria.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

/**
 * The ICRS/J2000 equatorial graticule of docs/decisions/
 * coordinate-grid.md, computed as a pure deterministic function of
 * the viewport alone: constant-RA meridians and constant-Dec
 * parallels sampled along the sky and clipped per piece (the
 * geography pass's approach; measured worst chord error 0.004 px
 * against the true projected midpoint, stated tolerance 0.05 px);
 * one adaptive interval rule per axis (the smallest pleasant step
 * spacing at least MINIMUM_SPACING_PX at the page centre, RA scaled
 * by cos of the centre declination, capping at 6h so the pole draws
 * four radiating meridians through ringed parallels); page-edge
 * labels in grid notation whose ONE exact bounds calculation governs
 * placement, paper containment, title suppression, and drawing.
 *
 * The seam sees only {@link ChartViewport} (plus font metrics and
 * the title rectangle for label suppression): no catalogue or
 * geography query is reachable by type. Gate reference measured by
 * {@code make grid-study}; this class IS that geometry, moved to
 * production rather than mirrored (issue #133).
 */
public final class EquatorialGrid {

    private EquatorialGrid() {
    }

    /**
     * The white-paper grid inks, published for tests and studies
     * that name the released values; the drawing itself asks the
     * chart's {@link ChartPalette} (issue #246).
     */
    public static final Color GRID_INK =
            ChartPalette.WHITE_PAPER.gridInk();
    public static final Color GRID_LABEL_INK =
            ChartPalette.WHITE_PAPER.gridLabelInk();
    public static final Font GRID_LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    /** Pleasant right-ascension steps, in degrees (1m ... 6h of time). */
    private static final double[] RA_STEPS_DEGREES = {
            0.25, 0.5, 1.25, 2.5, 5.0, 7.5, 15.0, 30.0, 45.0, 90.0};
    /** Pleasant declination steps, in degrees (15' ... 15°). */
    private static final double[] DEC_STEPS_DEGREES = {
            0.25, 0.5, 1.0, 2.0, 5.0, 10.0, 15.0};

    /**
     * The adaptive interval rule: the smallest pleasant step whose
     * on-page spacing at the page centre is at least this many pixels
     * - bounded below here and above by the next step's refusal, so
     * adjacent field widths cannot jump between distracting densities.
     * RA spacing uses cos(centre declination): meridians converge, so
     * high-declination pages choose wider RA steps by the same rule.
     */
    public static final double MINIMUM_SPACING_PX = 110.0;

    public record GridSpec(double raStepDegrees, double decStepDegrees) {
    }

    /** Curve pieces in page pixels plus the labels the page carries. */
    public record Grid(GridSpec spec, List<List<PixelPoint>> meridians,
                List<List<PixelPoint>> parallels, List<Label> labels,
                int suppressedLabels, int subdivisionSamples,
                double maxChordErrorPx) {
    }

    public record Label(String text, double x, double y) {
    }

    /**
     * The ONE label-bounds calculation (PR #137 review, P1): exact
     * font geometry, shared by placement, paper containment, title
     * suppression, and drawing - never a guessed per-character width.
     * {@code x}/{@code y} anchor the drawn baseline.
     */
    public static java.awt.geom.Rectangle2D labelBounds(
            Label label, java.awt.FontMetrics metrics) {
        return new java.awt.geom.Rectangle2D.Double(
                label.x() - 1.0, label.y() - metrics.getAscent(),
                metrics.stringWidth(label.text()) + 2.0,
                metrics.getHeight());
    }

    /** Metrics for the grid label face, headless-safe and shared. */
    public static java.awt.FontMetrics labelMetrics() {
        Graphics2D g = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_RGB).createGraphics();
        java.awt.FontMetrics metrics = g.getFontMetrics(GRID_LABEL_FONT);
        g.dispose();
        return metrics;
    }

    /** Whether a label's exact box lies fully on the paper. */
    public static boolean fitsPaper(Label label, java.awt.FontMetrics metrics,
                             ChartViewport viewport) {
        var box = labelBounds(label, metrics);
        return box.getMinX() >= 0 && box.getMinY() >= 0
                && box.getMaxX() <= viewport.widthPx()
                && box.getMaxY() <= viewport.heightPx();
    }

    public static GridSpec spec(ChartViewport viewport) {
        double pxPerDegree = viewport.widthPx()
                / viewport.fieldWidthDegrees();
        double cosDec = Math.max(1e-6, Math.cos(Math.toRadians(
                viewport.centre().decDegrees())));
        double raStep = RA_STEPS_DEGREES[RA_STEPS_DEGREES.length - 1];
        for (double candidate : RA_STEPS_DEGREES) {
            if (candidate * cosDec * pxPerDegree >= MINIMUM_SPACING_PX) {
                raStep = candidate;
                break;
            }
        }
        double decStep = DEC_STEPS_DEGREES[DEC_STEPS_DEGREES.length - 1];
        for (double candidate : DEC_STEPS_DEGREES) {
            if (candidate * pxPerDegree >= MINIMUM_SPACING_PX) {
                decStep = candidate;
                break;
            }
        }
        return new GridSpec(raStep, decStep);
    }

    /**
     * The candidate grid for a viewport: every meridian and parallel
     * of the chosen intervals sampled along the sky (the geography
     * pass's approach), kept as polylines of consecutive on-page
     * pieces; edge labels; and the measured worst chord error against
     * the true projected curve midpoint.
     */
    public static Grid gridFor(DrawnPage page,
                               java.awt.Rectangle... furniture) {
        ChartViewport viewport = page.scene().viewport();
        GridSpec spec = spec(viewport);
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        double sampleStep = viewport.fieldWidthDegrees() / 180.0;
        SkyBounds bounds = boundsFor(page);
        java.awt.FontMetrics metrics = labelMetrics();

        List<List<PixelPoint>> meridians = new ArrayList<>();
        List<Label> labels = new ArrayList<>();
        List<Missed> missed = new ArrayList<>();
        int[] samples = {0};
        double[] worstError = {0.0};
        int suppressed = 0;

        double decLo = Math.max(-89.99,
                bounds.decMin() - spec.decStepDegrees());
        double decHi = Math.min(89.99,
                bounds.decMax() + spec.decStepDegrees());
        final double decSpan = decHi - decLo;
        for (double raCursor = 0.0; raCursor < 360.0 - 1e-9;
                raCursor += spec.raStepDegrees()) {
            final double ra = raCursor;
            if (!bounds.containsRa(ra, spec.raStepDegrees())) {
                continue;
            }
            final double lo = decLo;
            List<List<PixelPoint>> pieces = sampleCurve(
                    step -> new SkyPosition(normalizeRa(ra),
                            lo + step * decSpan),
                    Math.max(2, (int) Math.ceil(decSpan / sampleStep)),
                    projection, mapping, viewport, samples, worstError);
            meridians.addAll(pieces);
            if (!pieces.isEmpty()) {
                Label label = edgeLabel(pieces, raLabel(ra), viewport, true);
                if (label != null) {
                    if (!fitsPaper(label, metrics, viewport)
                            || intersectsFurniture(label, metrics, furniture)) {
                        suppressed++;
                    } else {
                        labels.add(label);
                    }
                } else {
                    // Only here: the bottom edge is never crossed.
                    // A bottom figure that existed and was
                    // suppressed took the branch above and stays
                    // suppressed (issue #360).
                    //
                    // Held for the second pass. Placing it now would
                    // let it choose a spot no parallel has claimed
                    // yet, because the Dec loop below has not run.
                    missed.add(new Missed(pieces, raLabel(ra), ra, true));
                }
            }
        }

        List<List<PixelPoint>> parallels = new ArrayList<>();
        final double raStart = bounds.fullRa() ? 0.0
                : bounds.raCentre() - bounds.raHalfSpan() - 1.0;
        final double raSpan = bounds.fullRa() ? 360.0
                : 2.0 * (bounds.raHalfSpan() + 1.0);
        for (double dec = firstMultipleAbove(
                        Math.max(-89.5, decLo), spec.decStepDegrees());
                dec <= Math.min(89.5, decHi);
                dec += spec.decStepDegrees()) {
            final double parallelDec = dec;
            List<List<PixelPoint>> pieces = sampleCurve(
                    step -> new SkyPosition(
                            normalizeRa(raStart + step * raSpan), parallelDec),
                    Math.max(2, (int) Math.ceil(raSpan / sampleStep)),
                    projection, mapping, viewport, samples, worstError);
            parallels.addAll(pieces);
            if (!pieces.isEmpty()) {
                String notation = decLabel(parallelDec,
                        spec.decStepDegrees());
                Label label = edgeLabel(pieces, notation, viewport, false);
                if (label != null) {
                    if (!fitsPaper(label, metrics, viewport)
                            || intersectsFurniture(label, metrics, furniture)) {
                        suppressed++;
                    } else {
                        labels.add(label);
                    }
                } else {
                    // Only here: the left edge is never crossed.
                    missed.add(new Missed(pieces, notation, parallelDec,
                            false));
                }
            }
        }
        // ---- second pass: the curves that missed their edge -------
        // Every ordinary figure now exists, for meridians AND
        // parallels, so a fallback can be held to all of them. Run
        // inside the RA loop, a rescued meridian figure could only
        // see other meridian figures, and a parallel's ordinary
        // figure - added afterwards, and never overlap-checked
        // itself - would land on top of it. That is how `6h` came to
        // be drawn through `-30°` on the south-pole page (#360).
        //
        // A fallback still only ever adds. It never moves or
        // suppresses an ordinary figure: the hierarchy is that a
        // preferred-edge figure outranks a repair, and a repair that
        // cannot find room simply does not appear.
        for (Missed curve : missed) {
            Label elsewhere = fallbackLabel(curve.pieces(),
                    curve.notation(), curve.coordinate(),
                    curve.meridian(), viewport, metrics, labels,
                    furniture);
            if (elsewhere != null) {
                labels.add(elsewhere);
            }
        }
        return new Grid(spec, meridians, parallels, labels, suppressed,
                samples[0], worstError[0]);
    }

    /**
     * A visible curve whose preferred edge was never crossed, held
     * until every ordinary figure has been placed.
     */
    private record Missed(List<List<PixelPoint>> pieces, String notation,
                          double coordinate, boolean meridian) {
    }

    /** RA grid notation: whole hours bare, otherwise hours+minutes. */
    public static String raLabel(double raDegrees) {
        int totalMinutes = (int) Math.round(normalizeRa(raDegrees) * 4.0);
        int hours = (totalMinutes / 60) % 24;
        int minutes = totalMinutes % 60;
        return minutes == 0 ? hours + "h" : hours + "h " + minutes + "m";
    }

    /** Dec grid notation: signed degrees, arcminutes only when needed. */
    public static String decLabel(double decDegrees, double stepDegrees) {
        String sign = decDegrees < 0 ? "−" : "+";
        double magnitude = Math.abs(decDegrees);
        int whole = (int) magnitude;
        int arcmin = (int) Math.round((magnitude - whole) * 60.0);
        if (stepDegrees >= 1.0 || arcmin == 0) {
            return sign + whole + "°";
        }
        return sign + whole + "° " + arcmin + "′";
    }

    private static double normalizeRa(double ra) {
        return (ra % 360.0 + 360.0) % 360.0;
    }

    /**
     * The page's sky extent, measured from its own border pixels
     * through the exact inverse projection: the declination range,
     * and the right-ascension span around the centre - full-circle
     * when a celestial pole lies on the page, where every meridian
     * is a candidate.
     */
    public record SkyBounds(double raCentre, double raHalfSpan, boolean fullRa,
                     double decMin, double decMax) {

        boolean containsRa(double ra, double margin) {
            if (fullRa) {
                return true;
            }
            double delta = Math.abs((((ra - raCentre) % 360.0) + 540.0)
                    % 360.0 - 180.0);
            return delta <= raHalfSpan + margin;
        }
    }

    /** The paper's own four edges, for a page that is sky to them. */
    private static java.util.List<PixelPoint> aroundTheEdges(
            ChartViewport viewport, int perEdge) {
        java.util.List<PixelPoint> walk = new java.util.ArrayList<>();
        for (int i = 0; i <= perEdge; i++) {
            double fx = (double) i / perEdge * (viewport.widthPx() - 1);
            double fy = (double) i / perEdge * (viewport.heightPx() - 1);
            walk.add(new PixelPoint(fx, 0));
            walk.add(new PixelPoint(fx, viewport.heightPx() - 1));
            walk.add(new PixelPoint(0, fy));
            walk.add(new PixelPoint(viewport.widthPx() - 1, fy));
        }
        return walk;
    }

    /**
     * The limb, for a page whose sky ends inside the paper.
     *
     * <p>Sampled a hair inside it, because the limb is where the
     * inverse is at its worst conditioned: the whole far hemisphere
     * is squeezed into the last sliver of plane radius, and a point
     * one part in a million outside answers nothing at all.
     */
    private static java.util.List<PixelPoint> aroundTheLimb(
            ViewportMapping mapping,
            juranometria.project.Projection projection, int around) {
        java.util.List<PixelPoint> walk = new java.util.ArrayList<>();
        double radius = projection.visiblePlaneRadius() * (1.0 - 1.0e-9);
        for (int i = 0; i < around; i++) {
            double angle = 2.0 * Math.PI * i / around;
            walk.add(mapping.toPixel(
                    new juranometria.project.PlanePoint(
                            radius * Math.cos(angle),
                            radius * Math.sin(angle))));
        }
        walk.add(mapping.toPixel(
                new juranometria.project.PlanePoint(0.0, 0.0)));
        return walk;
    }

    public static SkyBounds boundsFor(DrawnPage page) {
        ChartViewport viewport = page.scene().viewport();
        var projection = page.projection();
        var mapping = new ViewportMapping(page);
        double centreRa = viewport.centre().raDegrees();
        double decMin = 90.0;
        double decMax = -90.0;
        double maxDelta = 0.0;
        // Where a page's sky ends, which is not the same question on
        // every page. A chart page is sky to its corners, so its own
        // edges are the boundary to walk. A globe's edges are paper:
        // walking them finds nothing, and a first attempt at this
        // simply skipped the samples that had no sky - which left a
        // hemisphere with a declination span of nothing and ONE
        // parallel drawn across it (#329). The boundary of a bounded
        // page is its limb, so that is what gets walked.
        int perEdge = 32;
        for (PixelPoint pixel : page.bounded()
                ? aroundTheLimb(mapping, projection, 4 * perEdge)
                : aroundTheEdges(viewport, perEdge)) {
            var under = juranometria.project.PanSolver
                    .skyAt(viewport, juranometria.project.PanSolver
                            .planeFromPixel(viewport, pixel));
            if (under.isEmpty()) {
                continue;
            }
            SkyPosition sky = under.get();
            decMin = Math.min(decMin, sky.decDegrees());
            decMax = Math.max(decMax, sky.decDegrees());
            double raw = (((sky.raDegrees() - centreRa) % 360.0)
                    + 540.0) % 360.0 - 180.0;
            maxDelta = Math.max(maxDelta, Math.abs(raw));
        }
        boolean fullRa = false;
        for (double pole : new double[] {89.9999, -89.9999}) {
            var plane = projection.project(new SkyPosition(centreRa, pole));
            if (plane.isPresent()) {
                PixelPoint pixel = mapping.toPixel(plane.get());
                if (pixel.x() >= 0 && pixel.x() < viewport.widthPx()
                        && pixel.y() >= 0
                        && pixel.y() < viewport.heightPx()) {
                    fullRa = true;
                    decMin = pole > 0 ? decMin : -89.99;
                    decMax = pole > 0 ? 89.99 : decMax;
                }
            }
        }
        return new SkyBounds(centreRa, maxDelta, fullRa, decMin, decMax);
    }

    private static double firstMultipleAbove(double floor, double step) {
        return Math.ceil(floor / step) * step;
    }

    private interface CurvePoint {
        SkyPosition at(double unitStep);
    }

    /**
     * Samples one sky curve into on-page polylines, accumulating the
     * worst distance between each chord midpoint and the true
     * projected curve midpoint - the honest measure that the drawn
     * grid is projection-correct, never a straight screen-space chord.
     */
    private static List<List<PixelPoint>> sampleCurve(
            CurvePoint curve, int steps, Projection projection,
            ViewportMapping mapping, ChartViewport viewport,
            int[] samples, double[] worstError) {
        List<List<PixelPoint>> pieces = new ArrayList<>();
        List<PixelPoint> current = new ArrayList<>();
        PixelPoint previous = null;
        SkyPosition previousSky = null;
        for (int i = 0; i <= steps; i++) {
            SkyPosition sky = curve.at((double) i / steps);
            var plane = projection.project(sky);
            samples[0]++;
            if (plane.isEmpty()) {
                previous = null;
                previousSky = null;
                current = flush(pieces, current);
                continue;
            }
            PixelPoint pixel = mapping.toPixel(plane.get());
            if (previous != null) {
                var chord = new java.awt.geom.Line2D.Double(
                        previous.x(), previous.y(), pixel.x(), pixel.y());
                if (chord.intersects(0, 0, viewport.widthPx(),
                        viewport.heightPx())) {
                    if (current.isEmpty()) {
                        current.add(previous);
                    }
                    current.add(pixel);
                    SkyPosition midSky = midpoint(previousSky, sky);
                    var midPlane = projection.project(midSky);
                    if (midPlane.isPresent()) {
                        PixelPoint trueMid = mapping.toPixel(midPlane.get());
                        double error = java.awt.geom.Line2D.ptSegDist(
                                previous.x(), previous.y(), pixel.x(),
                                pixel.y(), trueMid.x(), trueMid.y());
                        if (error > worstError[0]) {
                            worstError[0] = error;
                        }
                    }
                } else {
                    current = flush(pieces, current);
                }
            }
            previous = pixel;
            previousSky = sky;
        }
        flush(pieces, current);
        return pieces;
    }

    private static SkyPosition midpoint(SkyPosition a, SkyPosition b) {
        double raA = a.raDegrees();
        double raB = b.raDegrees();
        if (Math.abs(raB - raA) > 180.0) {
            raB += raB < raA ? 360.0 : -360.0;
        }
        return new SkyPosition(normalizeRa((raA + raB) / 2.0),
                (a.decDegrees() + b.decDegrees()) / 2.0);
    }

    private static List<PixelPoint> flush(List<List<PixelPoint>> pieces,
                                          List<PixelPoint> current) {
        if (current.size() >= 2) {
            pieces.add(current);
        }
        return new ArrayList<>();
    }

    /**
     * One label per curve at the page edge: meridians label where
     * they meet the bottom edge (RA along the bottom, the reading
     * convention), parallels where they meet the left edge. A curve
     * that never meets its labelling edge stays unlabelled - honest
     * omission over invented placement.
     */
    private static Label edgeLabel(List<List<PixelPoint>> pieces,
                                   String text, ChartViewport viewport,
                                   boolean meridian) {
        for (List<PixelPoint> piece : pieces) {
            for (int i = 1; i < piece.size(); i++) {
                PixelPoint a = piece.get(i - 1);
                PixelPoint b = piece.get(i);
                if (meridian) {
                    double edgeY = viewport.heightPx() - 1.0;
                    if (Math.min(a.y(), b.y()) <= edgeY
                            && Math.max(a.y(), b.y()) >= edgeY) {
                        double t = (edgeY - a.y()) / (b.y() - a.y());
                        double x = a.x() + t * (b.x() - a.x());
                        if (x >= 0 && x < viewport.widthPx()) {
                            return new Label(text, x + 3.0, edgeY - 4.0);
                        }
                    }
                } else {
                    double edgeX = 0.0;
                    if (Math.min(a.x(), b.x()) <= edgeX
                            && Math.max(a.x(), b.x()) >= edgeX) {
                        double t = (edgeX - a.x()) / (b.x() - a.x());
                        double y = a.y() + t * (b.y() - a.y());
                        if (y >= 10 && y < viewport.heightPx() - 2) {
                            return new Label(text, 3.0, y - 3.0);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * How close to an edge's end a figure may not be placed.
     *
     * <p>A figure straddling a corner belongs to neither edge, and a
     * reader cannot tell which line it names.
     */
    static final double CORNER_CLEARANCE_PX = 28.0;

    /**
     * How square a crossing must be before a figure may claim it.
     *
     * <p>Text laid along a curve that only grazes the frame reads as
     * belonging to the frame rather than to the line, however exact
     * the mathematical intersection is.
     *
     * <p>Twenty degrees, and the number is deliberately uninteresting:
     * measured over the discovery matrix, 15, 20 and 25 degrees
     * rescue the same fourteen curves and 30 rescues thirteen. The
     * classification existed before the result was seen, and nothing
     * in the result pulls it either way. It separates two genuinely
     * shallow pairs - one at 17 degrees, one at 3 - from Orion's
     * 26-degree parallel.
     */
    static final double MINIMUM_INCIDENCE_DEGREES = 20.0;

    /** Which side of the frame a crossing is on. */
    enum Edge { BOTTOM, TOP, LEFT, RIGHT }

    /**
     * One real crossing of a grid curve with the rectangular frame.
     *
     * <p>The seam issue #360 asked for: a curve publishes where it
     * genuinely meets the frame, and one selection policy chooses
     * among those crossings. The projection mathematics stays in the
     * sampled curve - nothing here computes a second one.
     */
    record Candidate(boolean meridian, double coordinate, String notation,
                     Edge edge, double x, double y,
                     double incidenceDegrees, double cornerClearancePx,
                     Label label, java.awt.geom.Rectangle2D bounds) {
    }

    /**
     * Every genuine crossing of a sampled curve with the frame.
     *
     * <p>All four edges, every piece, every segment - including a
     * curve that leaves and re-enters, which produces two crossings
     * of the same edge. Orion's minus-45 parallel does exactly that,
     * and a search that stopped at the first would have described
     * half of it.
     */
    static List<Candidate> candidates(List<List<PixelPoint>> pieces,
                                      String notation, double coordinate,
                                      boolean meridian,
                                      ChartViewport viewport,
                                      java.awt.FontMetrics metrics) {
        double w = viewport.widthPx();
        double h = viewport.heightPx();
        List<Candidate> found = new ArrayList<>();
        for (List<PixelPoint> piece : pieces) {
            for (int i = 1; i < piece.size(); i++) {
                PixelPoint a = piece.get(i - 1);
                PixelPoint b = piece.get(i);
                crossHorizontal(found, a, b, h - 1.0, Edge.BOTTOM, w, h,
                        notation, coordinate, meridian, viewport, metrics);
                crossHorizontal(found, a, b, 0.0, Edge.TOP, w, h,
                        notation, coordinate, meridian, viewport, metrics);
                crossVertical(found, a, b, 0.0, Edge.LEFT, w, h,
                        notation, coordinate, meridian, viewport, metrics);
                crossVertical(found, a, b, w - 1.0, Edge.RIGHT, w, h,
                        notation, coordinate, meridian, viewport, metrics);
            }
        }
        return found;
    }

    private static void crossHorizontal(List<Candidate> found, PixelPoint a,
                                        PixelPoint b, double edgeY, Edge edge,
                                        double w, double h, String notation,
                                        double coordinate, boolean meridian,
                                        ChartViewport viewport,
                                        java.awt.FontMetrics metrics) {
        if (Math.min(a.y(), b.y()) > edgeY || Math.max(a.y(), b.y()) < edgeY
                || Math.abs(b.y() - a.y()) < 1e-9) {
            return;
        }
        double t = (edgeY - a.y()) / (b.y() - a.y());
        double x = a.x() + t * (b.x() - a.x());
        if (x < 0 || x >= w) {
            return;
        }
        double incidence = Math.toDegrees(Math.atan2(
                Math.abs(b.y() - a.y()), Math.abs(b.x() - a.x())));
        found.add(at(meridian, coordinate, notation, edge, x, edgeY,
                incidence, Math.min(x, w - x), viewport, metrics));
    }

    private static void crossVertical(List<Candidate> found, PixelPoint a,
                                      PixelPoint b, double edgeX, Edge edge,
                                      double w, double h, String notation,
                                      double coordinate, boolean meridian,
                                      ChartViewport viewport,
                                      java.awt.FontMetrics metrics) {
        if (Math.min(a.x(), b.x()) > edgeX || Math.max(a.x(), b.x()) < edgeX
                || Math.abs(b.x() - a.x()) < 1e-9) {
            return;
        }
        double t = (edgeX - a.x()) / (b.x() - a.x());
        double y = a.y() + t * (b.y() - a.y());
        if (y < 0 || y >= h) {
            return;
        }
        double incidence = Math.toDegrees(Math.atan2(
                Math.abs(b.x() - a.x()), Math.abs(b.y() - a.y())));
        found.add(at(meridian, coordinate, notation, edge, edgeX, y,
                incidence, Math.min(y, h - y), viewport, metrics));
    }

    /** Where a figure sits for a crossing, and what it then occupies. */
    private static Candidate at(boolean meridian, double coordinate,
                                String notation, Edge edge, double x, double y,
                                double incidence, double clearance,
                                ChartViewport viewport,
                                java.awt.FontMetrics metrics) {
        Label label = switch (edge) {
            case BOTTOM -> new Label(notation, x + 3.0,
                    viewport.heightPx() - 1.0 - 4.0);
            case TOP -> new Label(notation, x + 3.0,
                    metrics.getAscent() + 3.0);
            case LEFT -> new Label(notation, 3.0, y - 3.0);
            case RIGHT -> new Label(notation,
                    viewport.widthPx() - metrics.stringWidth(notation) - 3.0,
                    y - 3.0);
        };
        return new Candidate(meridian, coordinate, notation, edge, x, y,
                incidence, clearance, label, labelBounds(label, metrics));
    }

    /**
     * A figure for a curve that never reaches its preferred edge.
     *
     * <p>Called <strong>only</strong> when {@code edgeLabel} found no
     * crossing at all of the preferred edge. A preferred figure that
     * was found and then suppressed - by the title block or by paper
     * containment - stays suppressed: that is a different decision,
     * about furniture, and issue #360 does not reopen it. Putting
     * this in the {@code else} of "a preferred crossing exists" is
     * what makes that structural rather than remembered.
     *
     * <p>Order, from the measured pages: the opposite parallel edge
     * first - top for a meridian, right for a parallel - because a
     * curve that leaves by one side usually arrives at the other, and
     * the figure reads in the same orientation the reader already
     * learned. Then the perpendicular edges, ranked by incidence,
     * then by corner clearance, then by position so that two runs of
     * the same page cannot disagree.
     *
     * <p>Orion's minus-45 parallel reaches neither vertical edge and
     * is rescued by neither: both of its bottom crossings are
     * refused, one by the title block and one by an existing figure.
     * It stays anonymous, and honestly so.
     *
     * <p><strong>A figure restored here participates in placement
     * before ordinary sky labels, and they yield to it.</strong> So
     * restoring a figure can change the lower-priority solution:
     * when the grid takes edge space it had not taken before, the
     * placement pass finds a different valid arrangement for star
     * and constellation names. Promoting this repair moved the
     * label-placement study's counts from 239 star labels to 238
     * and from 200 constellation names to 201 - not one label lost,
     * but a different answer to a constrained problem.
     *
     * <p>That is the stated hierarchy doing what it says, and it is
     * recorded rather than tidied away: requiring those counts to
     * stay fixed would be a second constraint, and it would
     * sometimes contradict the first.
     */
    private static Label fallbackLabel(List<List<PixelPoint>> pieces,
                                       String notation, double coordinate,
                                       boolean meridian,
                                       ChartViewport viewport,
                                       java.awt.FontMetrics metrics,
                                       List<Label> accepted,
                                       java.awt.Rectangle... furniture) {
        Edge preferred = meridian ? Edge.BOTTOM : Edge.LEFT;
        Edge opposite = meridian ? Edge.TOP : Edge.RIGHT;
        List<Candidate> sameOrientation = new ArrayList<>();
        List<Candidate> perpendicular = new ArrayList<>();
        for (Candidate candidate : candidates(pieces, notation, coordinate,
                meridian, viewport, metrics)) {
            if (candidate.edge() == preferred) {
                // The preferred edge had its chance and was refused,
                // so it does not get a second one here. Stated
                // rather than inferred: the only crossings
                // `edgeLabel` rejects but this would see are on the
                // left edge within ten pixels of an end, and the
                // corner clearance below refuses those too. No page
                // in the discovery matrix distinguishes the two
                // rules - this one says which is meant.
                continue;
            }
            if (candidate.edge() == opposite) {
                sameOrientation.add(candidate);
            } else {
                perpendicular.add(candidate);
            }
        }
        java.util.Comparator<Candidate> rank = java.util.Comparator
                .comparingDouble(Candidate::incidenceDegrees).reversed()
                .thenComparing(java.util.Comparator
                        .comparingDouble(Candidate::cornerClearancePx)
                        .reversed())
                .thenComparingDouble(Candidate::x)
                .thenComparingDouble(Candidate::y);
        sameOrientation.sort(rank);
        perpendicular.sort(rank);

        List<Candidate> order = new ArrayList<>(sameOrientation);
        order.addAll(perpendicular);
        for (Candidate candidate : order) {
            if (candidate.cornerClearancePx() < CORNER_CLEARANCE_PX
                    || candidate.incidenceDegrees()
                            < MINIMUM_INCIDENCE_DEGREES
                    || !fitsPaper(candidate.label(), metrics, viewport)
                    || intersectsFurniture(candidate.label(), metrics,
                            furniture)
                    || overlapsAccepted(candidate, metrics, accepted)) {
                continue;
            }
            return candidate.label();
        }
        return null;
    }

    /** One figure per curve, and never on top of another curve's. */
    private static boolean overlapsAccepted(Candidate candidate,
                                            java.awt.FontMetrics metrics,
                                            List<Label> accepted) {
        for (Label other : accepted) {
            if (candidate.bounds().intersects(labelBounds(other, metrics))) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersectsFurniture(Label label,
                                           java.awt.FontMetrics metrics,
                                           java.awt.Rectangle... furniture) {
        // Grid notation yields to the furniture that will actually
        // draw over it - the title block as it always has, and the
        // magnitude key on the same terms. Furniture the reader has
        // switched off reserves nothing, so switching a block off
        // gives back the labels it was suppressing (Sprint 20
        // review).
        if (furniture == null) {
            return false;
        }
        java.awt.geom.Rectangle2D bounds = labelBounds(label, metrics);
        for (java.awt.Rectangle reserved : furniture) {
            if (reserved != null && bounds.intersects(reserved)) {
                return true;
            }
        }
        return false;
    }

    /** Draws a computed grid in the released white-paper ink. */
    public static void draw(Graphics2D g, Grid grid) {
        draw(g, grid, ChartPalette.WHITE_PAPER);
    }

    /**
     * Draws a computed grid onto a graphics context, quietest ink.
     *
     * <p><strong>Not clipped to the limb, and deliberately.</strong>
     * The grid is the one sky family that needs no clip: its curves
     * are built through {@code PageRegion}, which on a bounded page
     * already ends at the limb, so not a point of them lies outside
     * it. Clipping them again would not make the page more truthful -
     * it would hide the day one of them stopped being bounded, where
     * {@code GlobeClipTest} fails on it instead (Sprint 32, issue
     * #331).
     *
     * <p>The first attempt at that rule clipped this call as a whole,
     * which would also have cut the grid's notation - and a
     * coordinate cut in half reads as a different coordinate. On a
     * globe there is no notation to cut, which is its own recorded
     * limitation.
     */
    public static void draw(Graphics2D g, Grid grid, ChartPalette palette) {
        draw(g, grid, palette, false);
    }

    /**
     * The same grid, optionally as the reader's emphasized structure
     * (issue #361). Emphasis is ink only: the computed grid, its
     * geometry and its notation placement are exactly the canonical
     * ones. It includes the coordinate notation with the curves,
     * because a grid a reader is following is read through its
     * numbers.
     */
    public static void draw(Graphics2D g, Grid grid, ChartPalette palette,
                            boolean emphasized) {
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawCurves(g, grid, palette, emphasized);
        drawNotation(g, grid, palette, emphasized);
    }

    private static void drawCurves(Graphics2D g, Grid grid,
                                   ChartPalette palette,
                                   boolean emphasized) {
        StructureStyle.Style style = StructureStyle.resolve(palette,
                ChartStructure.EQUATORIAL_GRID, emphasized,
                palette.gridInk(), new BasicStroke(1.0f));
        g.setColor(style.color());
        g.setStroke(style.stroke());
        for (List<List<PixelPoint>> family
                : List.of(grid.meridians(), grid.parallels())) {
            for (List<PixelPoint> piece : family) {
                for (int i = 1; i < piece.size(); i++) {
                    g.draw(new java.awt.geom.Line2D.Double(
                            piece.get(i - 1).x(), piece.get(i - 1).y(),
                            piece.get(i).x(), piece.get(i).y()));
                }
            }
        }
    }

    private static void drawNotation(Graphics2D g, Grid grid,
                                     ChartPalette palette,
                                     boolean emphasized) {
        g.setColor(StructureStyle.resolve(palette,
                ChartStructure.EQUATORIAL_GRID, emphasized,
                palette.gridLabelInk(), new BasicStroke(1.0f)).color());
        g.setFont(GRID_LABEL_FONT);
        for (Label label : grid.labels()) {
            g.drawString(label.text(), (float) label.x(), (float) label.y());
        }
    }

}
