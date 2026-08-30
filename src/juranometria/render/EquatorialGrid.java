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
import juranometria.project.GnomonicProjection;
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

    public static final Color GRID_INK = new Color(216, 216, 216);
    public static final Color GRID_LABEL_INK = new Color(150, 150, 150);
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
    public static Grid gridFor(ChartViewport viewport, java.awt.Rectangle titleBlock) {
        GridSpec spec = spec(viewport);
        GnomonicProjection projection =
                new GnomonicProjection(viewport.centre());
        ViewportMapping mapping = new ViewportMapping(viewport);
        double sampleStep = viewport.fieldWidthDegrees() / 180.0;
        SkyBounds bounds = boundsFor(viewport);
        java.awt.FontMetrics metrics = labelMetrics();

        List<List<PixelPoint>> meridians = new ArrayList<>();
        List<Label> labels = new ArrayList<>();
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
                            || intersectsTitle(label, metrics, titleBlock)) {
                        suppressed++;
                    } else {
                        labels.add(label);
                    }
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
                Label label = edgeLabel(pieces,
                        decLabel(parallelDec, spec.decStepDegrees()),
                        viewport, false);
                if (label != null) {
                    if (!fitsPaper(label, metrics, viewport)
                            || intersectsTitle(label, metrics, titleBlock)) {
                        suppressed++;
                    } else {
                        labels.add(label);
                    }
                }
            }
        }
        return new Grid(spec, meridians, parallels, labels, suppressed,
                samples[0], worstError[0]);
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

    public static SkyBounds boundsFor(ChartViewport viewport) {
        var projection = new GnomonicProjection(viewport.centre());
        var mapping = new ViewportMapping(viewport);
        double centreRa = viewport.centre().raDegrees();
        double decMin = 90.0;
        double decMax = -90.0;
        double maxDelta = 0.0;
        int perEdge = 32;
        for (int i = 0; i <= perEdge; i++) {
            double fx = (double) i / perEdge * (viewport.widthPx() - 1);
            double fy = (double) i / perEdge * (viewport.heightPx() - 1);
            for (PixelPoint pixel : new PixelPoint[] {
                    new PixelPoint(fx, 0),
                    new PixelPoint(fx, viewport.heightPx() - 1),
                    new PixelPoint(0, fy),
                    new PixelPoint(viewport.widthPx() - 1, fy)}) {
                SkyPosition sky = juranometria.project.PanSolver
                        .skyFromPlane(viewport.centre(),
                                juranometria.project.PanSolver
                                        .planeFromPixel(viewport, pixel));
                decMin = Math.min(decMin, sky.decDegrees());
                decMax = Math.max(decMax, sky.decDegrees());
                double raw = (((sky.raDegrees() - centreRa) % 360.0)
                        + 540.0) % 360.0 - 180.0;
                maxDelta = Math.max(maxDelta, Math.abs(raw));
            }
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
            CurvePoint curve, int steps, GnomonicProjection projection,
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

    private static boolean intersectsTitle(Label label,
                                           java.awt.FontMetrics metrics,
                                           java.awt.Rectangle titleBlock) {
        return titleBlock != null
                && labelBounds(label, metrics).intersects(titleBlock);
    }

    /** Draws a computed grid onto a graphics context, quietest ink. */
    public static void draw(Graphics2D g, Grid grid) {
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(GRID_INK);
        g.setStroke(new BasicStroke(1.0f));
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
        g.setColor(GRID_LABEL_INK);
        g.setFont(GRID_LABEL_FONT);
        for (Label label : grid.labels()) {
            g.drawString(label.text(), (float) label.x(), (float) label.y());
        }
    }

}
