package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;

/**
 * The Sprint 15 coordinate-grid study (issue #132): the candidate
 * ICRS/J2000 equatorial grid - constant-RA meridians and constant-Dec
 * parallels projected through the REAL gnomonic viewport and mapping,
 * subdivided in the sky like the geography pass, clipped per piece,
 * with page-edge labels that yield to the production title block
 * (shared {@code ChartRenderer.titleBlockBounds}). Renders
 * representative pages at every released field width, reports chosen
 * intervals, curve and label counts, suppressed collisions,
 * subdivision counts, the measured maximum curve-approximation error,
 * and warm cost. Run via "make grid-study"; pages land in
 * build/grid-study/.
 *
 * Composition note, stated honestly: the grid belongs BENEATH every
 * other ink. The study emulates that exactly by rendering the grid on
 * white and compositing the finished chart over it, replacing the
 * chart's pure-paper pixels with grid pixels - identical to true
 * under-drawing except at antialiased ink fringes that cross a grid
 * line, where production (#133) blends against the grid instead of
 * the paper. Production parity is verified when #133 regenerates
 * these pages through the real pass, the Sprint 13 pattern.
 */
public final class GridStudyMain {

    static final Color GRID_INK = new Color(216, 216, 216);
    static final Color GRID_LABEL_INK = new Color(150, 150, 150);
    static final Font GRID_LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    /** Pleasant right-ascension steps, in degrees (1m ... 6h of time). */
    static final double[] RA_STEPS_DEGREES = {
            0.25, 0.5, 1.25, 2.5, 5.0, 7.5, 15.0, 30.0, 45.0, 90.0};
    /** Pleasant declination steps, in degrees (15' ... 15°). */
    static final double[] DEC_STEPS_DEGREES = {
            0.25, 0.5, 1.0, 2.0, 5.0, 10.0, 15.0};

    /**
     * The adaptive interval rule: the smallest pleasant step whose
     * on-page spacing at the page centre is at least this many pixels
     * - bounded below here and above by the next step's refusal, so
     * adjacent field widths cannot jump between distracting densities.
     * RA spacing uses cos(centre declination): meridians converge, so
     * high-declination pages choose wider RA steps by the same rule.
     */
    static final double MINIMUM_SPACING_PX = 110.0;

    record GridSpec(double raStepDegrees, double decStepDegrees) {
    }

    /** Curve pieces in page pixels plus the labels the page carries. */
    record Grid(GridSpec spec, List<List<PixelPoint>> meridians,
                List<List<PixelPoint>> parallels, List<Label> labels,
                int suppressedLabels, int subdivisionSamples,
                double maxChordErrorPx) {
    }

    record Label(String text, double x, double y) {
    }

    static GridSpec spec(ChartViewport viewport) {
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
    static Grid gridFor(ChartViewport viewport, java.awt.Rectangle titleBlock) {
        GridSpec spec = spec(viewport);
        GnomonicProjection projection =
                new GnomonicProjection(viewport.centre());
        ViewportMapping mapping = new ViewportMapping(viewport);
        double sampleStep = viewport.fieldWidthDegrees() / 180.0;
        SkyBounds bounds = boundsFor(viewport);

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
                    if (intersectsTitle(label, titleBlock, viewport)) {
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
                    if (intersectsTitle(label, titleBlock, viewport)) {
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
    static String raLabel(double raDegrees) {
        int totalMinutes = (int) Math.round(normalizeRa(raDegrees) * 4.0);
        int hours = (totalMinutes / 60) % 24;
        int minutes = totalMinutes % 60;
        return minutes == 0 ? hours + "h" : hours + "h " + minutes + "m";
    }

    /** Dec grid notation: signed degrees, arcminutes only when needed. */
    static String decLabel(double decDegrees, double stepDegrees) {
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
    record SkyBounds(double raCentre, double raHalfSpan, boolean fullRa,
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

    static SkyBounds boundsFor(ChartViewport viewport) {
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
                                           java.awt.Rectangle titleBlock,
                                           ChartViewport viewport) {
        if (titleBlock == null) {
            return false;
        }
        // A conservative text box for the 10pt label.
        var box = new java.awt.geom.Rectangle2D.Double(
                label.x() - 2, label.y() - 10,
                label.text().length() * 6.5 + 4, 13);
        return titleBlock.intersects(box.getBounds());
    }

    /** Draws a computed grid onto a graphics context, quietest ink. */
    static void draw(Graphics2D g, Grid grid) {
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

    public static void main(String[] args) throws Exception {
        File outDir = new File("build/grid-study");
        outDir.mkdirs();
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);

        record Page(String name, double ra, double dec, double field,
                    int width, int height) {
        }
        List<Page> pages = List.of(
                new Page("m31-08", 10.684708, 41.268750, 8.0, 900, 700),
                new Page("orion-36", 83.818667, 0.0, 36.0, 900, 700),
                new Page("orion-12", 83.818667, -5.389667, 12.0, 900, 700),
                new Page("orion-03", 83.818667, -5.389667, 3.0, 900, 700),
                new Page("m42-01", 83.818667, -5.389667, 1.0, 900, 700),
                new Page("ra-wrap-24", 0.3, 45.0, 24.0, 900, 700),
                new Page("polar-36", 37.946619, 89.9, 36.0, 900, 700),
                new Page("dec60-18", 37.946619, 60.0, 18.0, 900, 700),
                new Page("crux-18", 186.649563, -63.099093, 18.0, 900, 700),
                new Page("pleiades-08", 56.869167, 24.105278, 8.0, 900, 700),
                new Page("minwin-08", 10.684708, 41.268750, 8.0, 500, 400),
                new Page("letterbox-36", 37.946619, 85.0, 36.0, 900, 4712));

        System.out.printf(Locale.ROOT,
                "%-14s %5s | %9s %8s | %5s %5s | %6s %5s | %7s %9s%n",
                "page", "field", "RA-step", "Dec-step", "merid", "paral",
                "labels", "supp", "samples", "worst-err");
        long warmNanos = 0;
        int warmed = 0;
        for (Page page : pages) {
            ChartViewState state = new ChartViewState(
                    new SkyPosition(page.ra(), page.dec()), page.field(),
                    8.0, null, null);
            ChartScene scene = Atlas.assembler().assemble(
                    state, page.width(), page.height());
            BufferedImage chart = renderer.renderToImage(scene);
            Graphics2D probe = chart.createGraphics();
            java.awt.Rectangle titleBlock =
                    ChartRenderer.titleBlockBounds(probe, scene);
            probe.dispose();

            long t0 = System.nanoTime();
            Grid grid = gridFor(scene.viewport(), titleBlock);
            long t1 = System.nanoTime();
            warmNanos += t1 - t0;
            warmed++;

            // Under-drawing, emulated exactly (see the class comment):
            // grid on white, chart composited over it, paper pixels
            // letting the grid show through.
            BufferedImage composite = new BufferedImage(page.width(),
                    page.height(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = composite.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, page.width(), page.height());
            draw(g, grid);
            g.dispose();
            int white = Color.WHITE.getRGB();
            for (int y = 0; y < page.height(); y++) {
                for (int x = 0; x < page.width(); x++) {
                    int pixel = chart.getRGB(x, y);
                    if (pixel != white) {
                        composite.setRGB(x, y, pixel);
                    }
                }
            }
            ImageIO.write(composite, "png",
                    new File(outDir, page.name() + ".png"));

            System.out.printf(Locale.ROOT,
                    "%-14s %4.0f° | %8.2f° %7.1f° | %5d %5d"
                            + " | %6d %5d | %7d %9.3f%n",
                    page.name(), page.field(), grid.spec().raStepDegrees(),
                    grid.spec().decStepDegrees(), grid.meridians().size(),
                    grid.parallels().size(), grid.labels().size(),
                    grid.suppressedLabels(), grid.subdivisionSamples(),
                    grid.maxChordErrorPx());
        }
        System.out.printf(Locale.ROOT,
                "mean grid computation: %.2f ms per page (drawing is"
                        + " part of the ordinary paint)%n",
                warmNanos / 1e6 / warmed);

        // The comparison the decision needs: absent, lines only, and
        // the labelled proposal on the same page.
        ChartViewState state = new ChartViewState(
                new SkyPosition(10.684708, 41.268750), 8.0, 8.0, null, null);
        ChartScene scene = Atlas.assembler().assemble(state, 900, 700);
        BufferedImage absent = renderer.renderToImage(scene);
        ImageIO.write(absent, "png", new File(outDir, "m31-08-absent.png"));
        Graphics2D probe = absent.createGraphics();
        java.awt.Rectangle titleBlock =
                ChartRenderer.titleBlockBounds(probe, scene);
        probe.dispose();
        Grid grid = gridFor(scene.viewport(), titleBlock);
        Grid linesOnly = new Grid(grid.spec(), grid.meridians(),
                grid.parallels(), List.of(), 0, grid.subdivisionSamples(),
                grid.maxChordErrorPx());
        for (var variant : List.of(
                java.util.Map.entry("m31-08-lines-only", linesOnly),
                java.util.Map.entry("m31-08-labelled", grid))) {
            BufferedImage composite = new BufferedImage(900, 700,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = composite.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 900, 700);
            draw(g, variant.getValue());
            g.dispose();
            int white = Color.WHITE.getRGB();
            for (int y = 0; y < 700; y++) {
                for (int x = 0; x < 900; x++) {
                    int pixel = absent.getRGB(x, y);
                    if (pixel != white) {
                        composite.setRGB(x, y, pixel);
                    }
                }
            }
            ImageIO.write(composite, "png",
                    new File(outDir, variant.getKey() + ".png"));
        }
        System.out.println("Pages written to " + outDir);
    }
}
