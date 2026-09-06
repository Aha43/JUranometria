package juranometria.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PlanePoint;
import juranometria.sky.Ecliptic;
import juranometria.sky.GreatCircle;

/**
 * How far past 36 degrees the atlas can honestly go, measured
 * (Sprint 29, issue #283).
 *
 * <p>The reader who asked for this had already written Python to
 * make printable charts for a club evening, and asked to go
 * <em>a little</em> beyond 36 degrees for a bright-star overview -
 * accepting more projection error, but wanting to know what he was
 * accepting. This is that number.
 *
 * <p>Pure geometry, deterministic on every platform: no fonts, no
 * rendering, no clock, no catalogue. The rendered candidate pages
 * and the sheet study are produced separately; this is the
 * arithmetic the decision stands on.
 */
public final class PrintableChartStudyMain {

    private PrintableChartStudyMain() {
    }

    /** The page the atlas draws, and the one the study measures. */
    private static final int PAGE_WIDTH = 900;
    private static final int PAGE_HEIGHT = 700;

    /** Today's widest, and the modest widenings the issue names. */
    private static final double[] FIELDS = {36.0, 40.0, 42.0, 45.0, 48.0};

    public static void main(String[] args) {
        p("# How far past 36 degrees the chart can honestly go");
        p("");
        p("Measured by `make printable-chart-study`. Reproduced"
                + " byte-for-byte by the evidence contracts. Pure"
                + " geometry: no fonts, no rendering, no clock, no"
                + " catalogue in these numbers.");
        p("");
        theAsk();
        howMuchSkyAPageHolds();
        whatTheEdgeCosts();
        whatAStraightLineCosts();
        whatTheReaderWouldMeasure();
    }

    private static void theAsk() {
        p("## What was asked");
        p("");
        p("> Jeg laget egne stjernekart for print til Fanafjellet"
                + " sist. Da kodet eg noe i python og det var mye"
                + " knot, dette med en eksport til SVG ville vært"
                + " veldig nyttig.");
        p("");
        p("*(\"I made my own star charts for printing for Fanafjellet"
                + " last time. I coded something in Python and it was"
                + " a lot of hassle; an export to SVG would have been"
                + " very useful.\")*");
        p("");
        p("And, separately: a little past 36°, not dramatically —"
                + " accepting more projection error for a practical"
                + " bright-star overview.");
        p("");
        p("**\"A little\" is the whole question.** The atlas stops at"
                + " 36° because that is where its gnomonic projection"
                + " stops being quiet, not because 36 is a round"
                + " number. So the gate measures what each further"
                + " degree costs, and where the cost stops being"
                + " something a reader would accept without being"
                + " told.");
        p("");
    }

    // ---- how much sky is actually on the page ------------------------

    private static void howMuchSkyAPageHolds() {
        p("## How much sky a page holds");
        p("");
        p("Field width is the *horizontal* extent. On a 900×700 page"
                + " the corner is further out than the edge, and that"
                + " corner is where every distortion below is worst —"
                + " so it is the corner, not the field width, that the"
                + " rest of this measures.");
        p("");
        p("| field | half-width | corner, gnomonic | corner, stereographic |");
        p("|---:|---:|---:|---:|");
        for (double field : FIELDS) {
            p(String.format(Locale.ROOT,
                    "| %.0f° | %.1f° | **%.1f°** | %.1f° |",
                    field, field / 2.0,
                    cornerAngleGnomonic(field),
                    cornerAngleStereographic(field)));
        }
        p("");
        p("The two are not the same page. Holding the left and right"
                + " edges to the same sky, the stereographic page"
                + " reaches slightly *further* into the corners,"
                + " because its scale grows more slowly.");
        p("");
    }

    /** Where the page corner falls, in degrees from centre. */
    private static double cornerAngleGnomonic(double fieldDegrees) {
        double halfWidthPlane = Math.tan(Math.toRadians(fieldDegrees) / 2.0);
        double halfHeightPlane =
                halfWidthPlane * (PAGE_HEIGHT / (double) PAGE_WIDTH);
        return Math.toDegrees(Math.atan(Math.hypot(halfWidthPlane,
                halfHeightPlane)));
    }

    private static double cornerAngleStereographic(double fieldDegrees) {
        double halfWidthPlane =
                2.0 * Math.tan(Math.toRadians(fieldDegrees) / 4.0);
        double halfHeightPlane =
                halfWidthPlane * (PAGE_HEIGHT / (double) PAGE_WIDTH);
        double r = Math.hypot(halfWidthPlane, halfHeightPlane);
        return Math.toDegrees(2.0 * Math.atan(r / 2.0));
    }

    // ---- what the edge costs -----------------------------------------

    private static void whatTheEdgeCosts() {
        p("## What the corner costs");
        p("");
        p("Two different costs, and conflating them is how a page"
                + " that looks fine hides a false claim:");
        p("");
        p("- **scale growth** — a degree of sky near the corner"
                + " occupies more paper than a degree at the centre,"
                + " so distances read wrong;");
        p("- **anisotropy** — the radial and tangential scales differ,"
                + " so *shapes* are stretched: a round cluster becomes"
                + " an ellipse pointing at the page centre.");
        p("");
        p("Measured numerically, by projecting small offsets and"
                + " comparing pixel distances — the implementation is"
                + " being measured here, not only the formula.");
        p("");
        p("| field | gnomonic: scale at corner | anisotropy | stereographic: scale | anisotropy |");
        p("|---:|---:|---:|---:|---:|");
        for (double field : FIELDS) {
            double[] gnomonic = scaleAtCorner(field, true);
            double[] stereo = scaleAtCorner(field, false);
            p(String.format(Locale.ROOT,
                    "| %.0f° | %+.1f%% | %+.1f%% | %+.1f%% | %+.2f%% |",
                    field,
                    (gnomonic[0] - 1.0) * 100.0,
                    (gnomonic[0] / gnomonic[1] - 1.0) * 100.0,
                    (stereo[0] - 1.0) * 100.0,
                    (stereo[0] / stereo[1] - 1.0) * 100.0));
        }
        p("");
        p("**The stereographic anisotropy column is the point of"
                + " that projection.** It is conformal: shapes are"
                + " preserved everywhere, at every field, and the"
                + " residual above is the measurement's own noise"
                + " rather than a distortion. Gnomonic stretches"
                + " radially, and the stretch is what a reader sees"
                + " as elongated clusters near the corners.");
        p("");
    }

    /**
     * Radial and tangential pixel scale at the page corner, relative
     * to the scale at the page centre.
     */
    private static double[] scaleAtCorner(double fieldDegrees,
                                          boolean gnomonic) {
        double corner = gnomonic ? cornerAngleGnomonic(fieldDegrees)
                : cornerAngleStereographic(fieldDegrees);
        SkyPosition centre = new SkyPosition(180.0, 0.0);
        // Along the equator: radial is the RA direction, tangential
        // the declination direction, and the arithmetic stays simple.
        SkyPosition at = new SkyPosition(180.0 + corner, 0.0);
        double delta = 1.0e-4;

        double radial = pixelsBetween(centre, at,
                new SkyPosition(180.0 + corner + delta, 0.0), gnomonic,
                fieldDegrees) / delta;
        double tangential = pixelsBetween(centre, at,
                new SkyPosition(180.0 + corner, delta), gnomonic,
                fieldDegrees) / delta;
        double atCentre = pixelsBetween(centre, centre,
                new SkyPosition(180.0 + delta, 0.0), gnomonic,
                fieldDegrees) / delta;
        return new double[] {radial / atCentre, tangential / atCentre};
    }

    private static double pixelsBetween(SkyPosition centre, SkyPosition from,
                                        SkyPosition to, boolean gnomonic,
                                        double fieldDegrees) {
        double[] a = toPixels(centre, from, gnomonic, fieldDegrees);
        double[] b = toPixels(centre, to, gnomonic, fieldDegrees);
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    private static double[] toPixels(SkyPosition centre, SkyPosition at,
                                     boolean gnomonic,
                                     double fieldDegrees) {
        if (gnomonic) {
            PlanePoint plane = new GnomonicProjection(centre).project(at)
                    .orElseThrow();
            double scale = PAGE_WIDTH / (2.0
                    * Math.tan(Math.toRadians(fieldDegrees) / 2.0));
            return new double[] {plane.xiEast() * scale,
                    plane.etaNorth() * scale};
        }
        PlanePoint plane = new StereographicCandidate(centre).project(at)
                .orElseThrow();
        double scale = StereographicCandidate.pixelsPerPlaneUnit(
                PAGE_WIDTH, fieldDegrees);
        return new double[] {plane.xiEast() * scale,
                plane.etaNorth() * scale};
    }

    // ---- what a straight line costs -----------------------------------

    private static void whatAStraightLineCosts() {
        p("## What a straight line costs");
        p("");
        p("The atlas draws its reference circles — the meridian, the"
                + " mathematical horizon, the ecliptic — by clipping"
                + " an **infinite great circle to the paper"
                + " analytically**, with no sampling and no tolerance."
                + " That is possible because a gnomonic projection"
                + " maps every great circle to a straight line"
                + " exactly. `GreatCirclePage.clip` is built on it,"
                + " and so is the Sprint 25 finding that a polyline"
                + " cannot answer a page lying between its own"
                + " vertices.");
        p("");
        p("A stereographic projection maps great circles to"
                + " **circles**. The straightness is not approximately"
                + " lost; it is lost. Measured as the greatest"
                + " departure of a projected great circle from the"
                + " straight chord joining where it leaves the paper:");
        p("");
        p("| field | chord measured | gnomonic | stereographic |");
        p("|---:|---:|---:|---:|");
        for (double field : FIELDS) {
            p(String.format(Locale.ROOT,
                    "| %.0f° | %.0f px | %.4f px | **%.1f px** |",
                    field, chord(field, true), sagitta(field, true),
                    sagitta(field, false)));
        }
        p("");
        p("So adopting stereographic is not a change of formula."
                + " **It does not end analytic clipping**: a circle"
                + " and a rectangle intersect exactly too. What it"
                + " ends is the existing *straight-line* clipper,"
                + " which returns a chord between two page crossings,"
                + " and an arc is not a chord. The cost is an exact"
                + " arc representation and clipper carried through the"
                + " module seam - a new geometry kind every"
                + " reference-ink consumer must learn - not a forced"
                + " return to sampled polylines.");
        p("");
    }

    /**
     * The greatest departure of a projected great circle from the
     * straight line through its two page crossings, in pixels.
     */
    private static double sagitta(double fieldDegrees, boolean gnomonic) {
        // A circle that does NOT pass through the page centre. Any
        // great circle through the projection centre is a line of
        // symmetry and stays straight under BOTH projections - a
        // first version measured the ecliptic on a page centred on
        // it and reported zero for both, which is true and says
        // nothing. Here the page is centred twelve degrees north of
        // the celestial equator, and the equator is the circle.
        SkyPosition centre = new SkyPosition(0.0, 12.0);
        GreatCircle circle = new GreatCircle(new SkyPosition(0.0, 90.0));
        List<double[]> onPage = new ArrayList<>();
        for (SkyPosition each : circle.around(4096)) {
            double[] pixel;
            try {
                pixel = toPixels(centre, each, gnomonic, fieldDegrees);
            } catch (RuntimeException offTheSphere) {
                continue;
            }
            if (Math.abs(pixel[0]) <= PAGE_WIDTH / 2.0
                    && Math.abs(pixel[1]) <= PAGE_HEIGHT / 2.0) {
                onPage.add(pixel);
            }
        }
        if (onPage.size() < 3) {
            return 0.0;
        }
        double[] from = onPage.get(0);
        double[] to = onPage.get(onPage.size() - 1);
        double length = Math.hypot(to[0] - from[0], to[1] - from[1]);
        if (length < 1.0) {
            return 0.0;
        }
        double worst = 0.0;
        for (double[] point : onPage) {
            double cross = Math.abs((to[0] - from[0]) * (from[1] - point[1])
                    - (from[0] - point[0]) * (to[1] - from[1]));
            worst = Math.max(worst, cross / length);
        }
        return worst;
    }

    /** How long the measured chord was, so a zero cannot hide. */
    private static double chord(double fieldDegrees, boolean gnomonic) {
        SkyPosition centre = new SkyPosition(0.0, 12.0);
        GreatCircle circle = new GreatCircle(new SkyPosition(0.0, 90.0));
        double[] first = null;
        double[] last = null;
        for (SkyPosition each : circle.around(4096)) {
            double[] pixel;
            try {
                pixel = toPixels(centre, each, gnomonic, fieldDegrees);
            } catch (RuntimeException offTheSphere) {
                continue;
            }
            if (Math.abs(pixel[0]) <= PAGE_WIDTH / 2.0
                    && Math.abs(pixel[1]) <= PAGE_HEIGHT / 2.0) {
                if (first == null) {
                    first = pixel;
                }
                last = pixel;
            }
        }
        return first == null ? 0.0
                : Math.hypot(last[0] - first[0], last[1] - first[1]);
    }

    // ---- what a reader would actually mis-measure ----------------------

    private static void whatTheReaderWouldMeasure() {
        p("## What a reader would mis-measure");
        p("");
        p("The costs above are properties of the projection. This is"
                + " what they do to somebody holding the printed"
                + " sheet: a separation measured with a ruler near the"
                + " corner, read against the scale the page centre"
                + " implies.");
        p("");
        p("A one-degree pair at the corner, measured as if the page"
                + " had one scale:");
        p("");
        p("| field | gnomonic reads | error | stereographic reads | error |");
        p("|---:|---:|---:|---:|---:|");
        for (double field : FIELDS) {
            double[] gnomonic = scaleAtCorner(field, true);
            double[] stereo = scaleAtCorner(field, false);
            p(String.format(Locale.ROOT,
                    "| %.0f° | %.3f° | %+.1f%% | %.3f° | %+.1f%% |",
                    field, gnomonic[0], (gnomonic[0] - 1.0) * 100.0,
                    stereo[0], (stereo[0] - 1.0) * 100.0));
        }
        p("");
        p("At 36° the atlas already asks a reader to accept 15% in"
                + " the corner and says so by stopping there. The"
                + " question the gate answers is how much further that"
                + " is honest on **paper**, where there is no zooming"
                + " out of a mistake and no tooltip to correct it.");
        p("");
    }

    private static void p(String line) {
        System.out.println(line);
    }
}
