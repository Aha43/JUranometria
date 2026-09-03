package juranometria.tool;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;

/**
 * What it costs to orient a fixed chart to a place and an instant
 * (Sprint 25, issue #225).
 *
 * <p>Measured by {@code make place-and-time-study}. Every number
 * here comes from the atlas's own projection and viewport mapping,
 * on pages its own assembler builds, so an error stated in
 * arcseconds is also stated in the pixels a reader would see it in.
 *
 * <p>No timings: they differ between machines and between two runs
 * on one machine, and a study whose output moves cannot be
 * reproduced.
 */
public final class PlaceAndTimeStudyMain {

    private PlaceAndTimeStudyMain() {
    }

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    /** The field steps the atlas offers, widest first. */
    private static final double[] FIELDS =
            {36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0};

    private record Place(String name, SkyOrientation.Observer observer) {
    }

    private static final List<Place> PLACES = List.of(
            new Place("Oslo", new SkyOrientation.Observer(59.913, 10.752)),
            new Place("Greenwich", new SkyOrientation.Observer(51.478, -0.0015)),
            new Place("Quito", new SkyOrientation.Observer(-0.180, -78.468)),
            new Place("Sydney", new SkyOrientation.Observer(-33.869, 151.209)),
            new Place("the north pole", new SkyOrientation.Observer(90.0, 0.0)),
            new Place("the south pole",
                    new SkyOrientation.Observer(-90.0, 0.0)));

    private record Moment(String name, Instant instant) {
    }

    private static final List<Moment> MOMENTS = List.of(
            new Moment("J2000 itself", utc(2000, 1, 1, 12, 0)),
            new Moment("March equinox 2026", utc(2026, 3, 20, 14, 33)),
            new Moment("June solstice 2026", utc(2026, 6, 21, 8, 24)),
            new Moment("December solstice 2026", utc(2026, 12, 21, 20, 3)),
            new Moment("2050", utc(2050, 7, 4, 3, 0)),
            new Moment("1975", utc(1975, 11, 30, 23, 59)));

    public static void main(String[] args) {
        System.out.println("# Orienting a fixed chart to a place and"
                + " an instant");
        System.out.println();
        System.out.println("Measured by `make place-and-time-study`."
                + " Every angle is turned into pixels through the"
                + " atlas's own gnomonic projection and viewport"
                + " mapping, on pages its own assembler builds, so"
                + " an error is stated in what a reader would see"
                + " rather than only in arcseconds.");
        System.out.println();

        theFrameProblem();
        theEpochShortcut();
        whatTimeCosts();
        theThreeGeometries();
        wherePolesMeet();
        howMuchIsOnAPage();
        howFinelyACurveMustBeDrawn();
    }

    // ---------------------------------------------------------------

    private static void theFrameProblem() {
        System.out.println("## The frame problem");
        System.out.println();
        System.out.println("The catalogue and the chart are ICRS/J2000."
                + " A meridian, a zenith and a horizon belong to the"
                + " observer's own date: they are defined against the"
                + " true equator and equinox of that instant. Drawing"
                + " them on a J2000 page is therefore a change of"
                + " frame, and the only question is whether the atlas"
                + " performs it or pretends it is unnecessary.");
        System.out.println();
        System.out.println("A page is 900 px wide, so a degree of sky"
                + " is worth this much of it:");
        System.out.println();
        System.out.println("| field | px per degree | one pixel is |");
        System.out.println("|---:|---:|---:|");
        for (double field : FIELDS) {
            double perDegree = pixelsPerDegree(field);
            System.out.printf(Locale.ROOT, "| %.0f° | %.1f | %s |%n",
                    field, perDegree, angle(1.0 / perDegree));
        }
        System.out.println();
    }

    private static void theEpochShortcut() {
        System.out.println("## What the shortcut costs");
        System.out.println();
        System.out.println("The shortcut is to compute the zenith's"
                + " right ascension from sidereal time and plot it on"
                + " the J2000 page unchanged. It is wrong by the whole"
                + " of precession since 2000, and it gets worse every"
                + " year the atlas is used.");
        System.out.println();
        System.out.println("| instant | shortcut | precession only |"
                + " worst pixels at 36° | worst pixels at 1° |");
        System.out.println("|---|---:|---:|---:|---:|");
        for (Moment moment : MOMENTS) {
            double shortcut = 0;
            double precessionOnly = 0;
            for (Place place : PLACES) {
                SkyPosition truth = SkyOrientation.zenith(place.observer(),
                        moment.instant(),
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                shortcut = Math.max(shortcut, truth.separationDegrees(
                        SkyOrientation.zenith(place.observer(),
                                moment.instant(),
                                SkyOrientation.Fidelity.EPOCH_SHORTCUT)));
                precessionOnly = Math.max(precessionOnly,
                        truth.separationDegrees(SkyOrientation.zenith(
                                place.observer(), moment.instant(),
                                SkyOrientation.Fidelity.PRECESSION_ONLY)));
            }
            System.out.printf(Locale.ROOT,
                    "| %s | %s | %s | %,.0f px | %,.0f px |%n",
                    moment.name(), angle(shortcut), angle(precessionOnly),
                    shortcut * pixelsPerDegree(36.0),
                    shortcut * pixelsPerDegree(1.0));
        }
        System.out.println();
        System.out.println("**The shortcut is rejected.** At the"
                + " atlas's widest field it puts the zenith a third of"
                + " a degree from where it belongs today, and at the"
                + " narrowest it is off the page. **Precession alone**"
                + " leaves nutation as the residual - small, but"
                + " several pixels at the narrowest field, and free to"
                + " avoid. The atlas carries **precession and"
                + " nutation**.");
        System.out.println();
    }

    private static void whatTimeCosts() {
        System.out.println("## What the atlas does not know about time");
        System.out.println();
        System.out.println("Three simplifications, each a decision with"
                + " a price rather than an oversight. The atlas ships"
                + " no time-scale data and makes no network call, so"
                + " each price is paid deliberately.");
        System.out.println();
        System.out.println("| simplification | worst error | at 36° |"
                + " at 1° |");
        System.out.println("|---|---:|---:|---:|");

        // UT1 - UTC: bounded by international agreement at 0.9 s.
        double ut1 = 0;
        for (Place place : PLACES) {
            for (Moment moment : MOMENTS) {
                SkyPosition onTime = SkyOrientation.zenith(place.observer(),
                        moment.instant(),
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                SkyPosition late = SkyOrientation.zenith(place.observer(),
                        moment.instant().plusMillis(900),
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                ut1 = Math.max(ut1, onTime.separationDegrees(late));
            }
        }
        row("UTC stands in for UT1 (up to 0.9 s apart)", ut1);

        // TT - UTC, which only enters the precession and nutation
        // arguments: 69.184 s in 2026.
        double tt = 0;
        for (Place place : PLACES) {
            for (Moment moment : MOMENTS) {
                double jd = SkyOrientation.julianDate(moment.instant());
                SkyPosition asIs = SkyOrientation.toJ2000(
                        new SkyPosition(80.0, 20.0), jd,
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                SkyPosition shifted = SkyOrientation.toJ2000(
                        new SkyPosition(80.0, 20.0), jd + 69.184 / 86400.0,
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                tt = Math.max(tt, asIs.separationDegrees(shifted));
            }
        }
        row("UTC stands in for TT in the precession arguments", tt);

        // The nutation series, truncated - measured against the
        // published full-series value rather than against a shorter
        // version of itself.
        //
        // The first draft compared twenty terms with four and called
        // the difference the truncation cost (gate review). That
        // measures what terms 5-20 contribute; it says nothing about
        // the terms beyond the twentieth, which are the ones
        // actually omitted.
        double againstPublished = nutationAgainstPublished();
        row("the nutation series stops at twenty terms, against the"
                + " published full-series value for 1987 April 10",
                againstPublished);
        double tail = omittedTailBound();
        row("the same, bounded for any date by the series' own"
                + " ordering (see below)", tail);

        // The precession model itself is a choice, and two accepted
        // ones do not agree exactly.
        double models = precessionModelSpread();
        row("IAU 1976 precession against the IAU 2006 form, over two"
                + " centuries", models);
        System.out.println();
        System.out.println("Polar motion (under 0.5\"), diurnal"
                + " aberration (under 0.3\") and refraction are not"
                + " modelled at all: the first two are below a pixel"
                + " at every field, and the third is a property of air"
                + " rather than of the sky, which is why the horizon"
                + " here is named **mathematical**.");
        System.out.println();
        System.out.println("**How the nutation tail is bounded.** The"
                + " IAU 1980 series is ordered by decreasing"
                + " amplitude, and the twentieth term's coefficient in"
                + " longitude is 0.0046\". Every omitted term is"
                + " therefore no larger than that, and the row above"
                + " sums the whole tail as though all of them fell in"
                + " phase at their maximum - which they cannot. The"
                + " measured residual against the published"
                + " full-series value for the same date is the"
                + " realistic figure; the bound is the honest"
                + " worst case.");
        System.out.println();
        double budget = ut1 + tail + models;
        System.out.printf(Locale.ROOT,
                "**The accuracy contract.** Adding the terms above at"
                        + " their worst - %s for UT1, %s for the"
                        + " nutation tail, %s for the choice of"
                        + " precession model - the atlas places the"
                        + " zenith, meridian and horizon within"
                        + " **%s** of the observer's own frame. That"
                        + " is %.2f px at the widest field and %.1f px"
                        + " at the narrowest, and it is dominated by"
                        + " not knowing UT1: every other term together"
                        + " is worth %s.%n%n",
                angle(ut1), angle(tail), angle(models), angle(budget),
                budget * pixelsPerDegree(36.0),
                budget * pixelsPerDegree(1.0), angle(tail + models));
    }

    /**
     * This twenty-term series against the published full-series
     * value for Meeus's worked example (22.a, 1987 April 10):
     * dpsi = -3.788", deps = +9.443".
     */
    private static double nutationAgainstPublished() {
        double t = SkyOrientation.centuries(
                SkyOrientation.julianDate(utc(1987, 4, 10, 0, 0)));
        double[] mine = SkyOrientation.nutationDegrees(t);
        double inLongitude = Math.abs(mine[0] - (-3.788 / 3600.0));
        double inObliquity = Math.abs(mine[1] - (9.443 / 3600.0));
        return inLongitude + inObliquity;
    }

    /**
     * The worst the omitted terms can be worth, from the series'
     * own ordering: none of them exceeds the twentieth, and the
     * IAU 1980 series has 106 terms in all.
     */
    private static double omittedTailBound() {
        double largestOmitted = 46 / 10000.0 / 3600.0;   // 0.0046"
        return (106 - 20) * largestOmitted;
    }

    /**
     * Two accepted precession models, over the range the atlas is
     * meant to be used in. The spread is a real uncertainty in the
     * frame rather than an error in either.
     */
    private static double precessionModelSpread() {
        double worst = 0;
        for (int year : new int[] {1900, 1950, 2000, 2026, 2050, 2100}) {
            double jd = SkyOrientation.julianDate(utc(year, 6, 15, 0, 0));
            double t = SkyOrientation.centuries(jd);
            double days = jd - 2451545.0;
            double classical = SkyOrientation.gmstDegrees(jd);
            double era = 360.0 * (0.7790572732640
                    + 1.00273781191135448 * days)
                    + (0.014506 + 4612.156534 * t + 1.3915817 * t * t
                            - 0.00000044 * t * t * t
                            - 0.000029956 * t * t * t * t) / 3600.0;
            double apart = Math.abs(((classical - era) % 360.0 + 540.0)
                    % 360.0 - 180.0);
            worst = Math.max(worst, apart);
        }
        return worst;
    }

    private static void row(String what, double degrees) {
        System.out.printf(Locale.ROOT, "| %s | %s | %.2f px | %.2f px |%n",
                what, angle(degrees), degrees * pixelsPerDegree(36.0),
                degrees * pixelsPerDegree(1.0));
    }

    private static void theThreeGeometries() {
        System.out.println("## The three geometries");
        System.out.println();
        System.out.println("| geometry | what it is | where it comes from |");
        System.out.println("|---|---|---|");
        System.out.println("| **zenith** | the point overhead | right"
                + " ascension = apparent sidereal time + east"
                + " longitude, declination = latitude, of date |");
        System.out.println("| **meridian** | the great circle through"
                + " both celestial poles and the zenith | hour angle 0"
                + " and 12ʰ, drawn as one closed curve |");
        System.out.println("| **horizon** | every direction ninety"
                + " degrees from the zenith | the great circle whose"
                + " pole is the zenith |");
        System.out.println();
        System.out.println("The **anti-meridian** and the **nadir** get"
                + " no vocabulary of their own. The meridian is drawn"
                + " as one closed circle, so a page holding the far"
                + " half shows it without a reader being taught a"
                + " second name; the nadir is the zenith's opposite"
                + " and is never marked, because a mark for the point"
                + " beneath a reader's feet is a mark for something"
                + " they cannot look at.");
        System.out.println();
        System.out.println("Sample zeniths, in the chart's own frame:");
        System.out.println();
        System.out.println("| place | instant | zenith (J2000) |"
                + " of date | apart |");
        System.out.println("|---|---|---|---|---:|");
        for (Place place : List.of(PLACES.get(0), PLACES.get(3),
                PLACES.get(4))) {
            for (Moment moment : List.of(MOMENTS.get(1), MOMENTS.get(4))) {
                SkyPosition j2000 = SkyOrientation.zenith(place.observer(),
                        moment.instant(),
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                SkyPosition ofDate = SkyOrientation.zenith(place.observer(),
                        moment.instant(),
                        SkyOrientation.Fidelity.EPOCH_SHORTCUT);
                System.out.printf(Locale.ROOT,
                        "| %s | %s | %s | %s | %s |%n",
                        place.name(), moment.name(), position(j2000),
                        position(ofDate),
                        angle(j2000.separationDegrees(ofDate)));
            }
        }
        System.out.println();
    }

    private static void howMuchIsOnAPage() {
        System.out.println("## How much of it is on a page");
        System.out.println();
        System.out.println("A meridian and a horizon are great circles;"
                + " a page is a few degrees of sky. What a reader"
                + " actually sees is a short arc, or nothing at all,"
                + " and the module must be honest about the"
                + " difference.");
        System.out.println();
        System.out.println("Measured on pages centred on the released"
                + " default (M31) and on the zenith itself, for an"
                + " observer in Oslo at the March 2026 equinox:");
        System.out.println();
        System.out.println("| page | field | meridian on paper |"
                + " horizon on paper | zenith on paper |");
        System.out.println("|---|---:|---:|---:|---|");
        SkyOrientation.Observer oslo = PLACES.get(0).observer();
        Instant when = MOMENTS.get(1).instant();
        SkyPosition zenith = SkyOrientation.zenith(oslo, when,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
        List<SkyPosition> meridian = SkyOrientation.meridian(oslo, when,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION, 3600);
        List<SkyPosition> horizon = SkyOrientation.horizon(oslo, when,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION, 3600);

        for (String where : new String[] {"M31", "the zenith"}) {
            SkyPosition centre = "M31".equals(where)
                    ? ChartViewState.DEFAULT.centre() : zenith;
            for (double field : new double[] {36.0, 8.0, 1.0}) {
                ChartScene scene = page(centre, field);
                System.out.printf(Locale.ROOT,
                        "| %s | %.0f° | %.1f%% | %.1f%% | %s |%n",
                        where, field, onPaper(scene, meridian) * 100,
                        onPaper(scene, horizon) * 100,
                        onPaper(scene, List.of(zenith)) > 0 ? "yes" : "no");
            }
        }
        System.out.println();
        System.out.println("So on most pages a reader sees an arc of"
                + " each, or none: the module must draw what crosses"
                + " the paper and say nothing where nothing crosses"
                + " it, rather than promising a line that is not"
                + " there.");
        System.out.println();
    }

    private static void wherePolesMeet() {
        System.out.println("## What the frame difference looks like");
        System.out.println();
        System.out.println("On a polar page the difference stops being"
                + " a number in a table. The meridian passes through"
                + " the celestial pole **of date**, and that is not"
                + " the pole the chart is drawn around - so the line"
                + " misses the chart's own pole by a measurable"
                + " distance.");
        System.out.println();
        System.out.println("(An earlier draft of this study said the"
                + " meridian *turns* there. It does not: a gnomonic"
                + " projection maps every great circle to a straight"
                + " line, and the kink in the first rendering was the"
                + " study's own drawing breaking the curve wherever"
                + " two samples landed far apart. The line is"
                + " straight; it simply does not go where a reader"
                + " might assume.)");
        System.out.println();
        System.out.println("| instant | pole of date, in J2000 |"
                + " from the chart's pole | at 36° | at 4° |"
                + " meridian misses the chart's pole by |");
        System.out.println("|---|---|---:|---:|---:|---:|");
        SkyOrientation.Observer oslo = PLACES.get(0).observer();
        for (Moment moment : MOMENTS) {
            SkyPosition poleOfDate = SkyOrientation.toJ2000(
                    new SkyPosition(0, 90),
                    SkyOrientation.julianDate(moment.instant()),
                    SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
            double apart = 90.0 - poleOfDate.decDegrees();
            // How far the drawn line passes from the chart's own
            // pole, measured on the sky rather than guessed at.
            double miss = 90.0;
            for (SkyPosition point : SkyOrientation.meridian(oslo,
                    moment.instant(),
                    SkyOrientation.Fidelity.PRECESSION_AND_NUTATION,
                    200000)) {
                miss = Math.min(miss,
                        point.separationDegrees(new SkyPosition(0, 90)));
            }
            System.out.printf(Locale.ROOT,
                    "| %s | %s | %s | %.0f px | %.0f px | %s |%n",
                    moment.name(), position(poleOfDate), angle(apart),
                    apart * pixelsPerDegree(36.0),
                    apart * pixelsPerDegree(4.0), angle(miss));
        }
        System.out.println();
        System.out.println("A reader who opens a polar page sees the"
                + " meridian pass beside the chart's pole rather than"
                + " through it. That is not an error to be hidden: it"
                + " is the J2000 chart and the observer's own sky,"
                + " drawn honestly on one page, and it is the"
                + " clearest argument in this study for carrying the"
                + " frames properly rather than pretending one is the"
                + " other.");
        System.out.println();
    }

    private static void howFinelyACurveMustBeDrawn() {
        System.out.println("## How finely a curve must be drawn");
        System.out.println();
        System.out.println("A great circle is not a straight line on a"
                + " gnomonic page - except when it is, which is the"
                + " one thing that makes this cheap: a gnomonic"
                + " projection maps every great circle to a straight"
                + " line. The meridian and the horizon are great"
                + " circles, so two projected points would be enough"
                + " if the frame rotation did not intervene, and the"
                + " rotation is rigid, so it still is.");
        System.out.println();
        System.out.println("Measured rather than asserted: the"
                + " furthest a projected sample falls from the chord"
                + " through its neighbours, at each field.");
        System.out.println();
        System.out.println("| field | meridian | horizon |");
        System.out.println("|---:|---:|---:|");
        SkyOrientation.Observer oslo = PLACES.get(0).observer();
        Instant when = MOMENTS.get(1).instant();
        SkyPosition zenith = SkyOrientation.zenith(oslo, when,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
        for (double field : new double[] {36.0, 8.0, 1.0}) {
            ChartScene scene = page(zenith, field);
            System.out.printf(Locale.ROOT, "| %.0f° | %.4f px | %.4f px |%n",
                    field,
                    straightness(scene, SkyOrientation.meridian(oslo, when,
                            SkyOrientation.Fidelity.PRECESSION_AND_NUTATION,
                            720)),
                    straightness(scene, SkyOrientation.horizon(oslo, when,
                            SkyOrientation.Fidelity.PRECESSION_AND_NUTATION,
                            720)));
        }
        System.out.println();
        System.out.println("Straight to a thousandth of a pixel, which"
                + " is the projection's own property and not a"
                + " tolerance: **#227 may draw each geometry as a"
                + " small number of segments**, and needs no"
                + " subdivision of the kind the deep-sky extents"
                + " required in Sprint 24.");
        System.out.println();
    }

    // ---------------------------------------------------------------

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(centre, field, 8.0), WIDTH, HEIGHT);
    }

    private static double pixelsPerDegree(double field) {
        ChartScene scene = page(ChartViewState.DEFAULT.centre(), field);
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        return mapping.pixelsPerPlaneUnit() * Math.PI / 180.0;
    }

    /** What fraction of a curve's samples land on the paper. */
    private static double onPaper(ChartScene scene, List<SkyPosition> curve) {
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        java.awt.geom.Rectangle2D paper = ChartRenderer.paperOf(scene);
        int on = 0;
        for (SkyPosition point : curve) {
            PixelPoint pixel = projection.project(point)
                    .map(mapping::toPixel).orElse(null);
            if (pixel != null && paper.contains(pixel.x(), pixel.y())) {
                on++;
            }
        }
        return on / (double) curve.size();
    }

    /**
     * The furthest a projected point strays from the chord through
     * its neighbours - zero for a great circle on a gnomonic page.
     */
    private static double straightness(ChartScene scene,
                                       List<SkyPosition> curve) {
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        java.awt.geom.Rectangle2D paper = ChartRenderer.paperOf(scene);
        List<PixelPoint> shown = new ArrayList<>();
        for (SkyPosition point : curve) {
            PixelPoint pixel = projection.project(point)
                    .map(mapping::toPixel).orElse(null);
            if (pixel != null && paper.contains(pixel.x(), pixel.y())) {
                shown.add(pixel);
            }
        }
        double worst = 0;
        for (int i = 1; i + 1 < shown.size(); i++) {
            PixelPoint before = shown.get(i - 1);
            PixelPoint here = shown.get(i);
            PixelPoint after = shown.get(i + 1);
            worst = Math.max(worst, java.awt.geom.Line2D.ptSegDist(
                    before.x(), before.y(), after.x(), after.y(),
                    here.x(), here.y()));
        }
        return worst;
    }

    private static String position(SkyPosition position) {
        return String.format(Locale.ROOT, "%.3f°, %+.3f°",
                position.raDegrees(), position.decDegrees());
    }

    /** An angle in the unit a reader of a table can compare. */
    private static String angle(double degrees) {
        double arcseconds = degrees * 3600.0;
        if (arcseconds < 0.01) {
            return String.format(Locale.ROOT, "%.4f\"", arcseconds);
        }
        if (arcseconds < 60) {
            return String.format(Locale.ROOT, "%.2f\"", arcseconds);
        }
        if (arcseconds < 3600) {
            return String.format(Locale.ROOT, "%.2f'", arcseconds / 60.0);
        }
        return String.format(Locale.ROOT, "%.3f°", degrees);
    }

    private static Instant utc(int year, int month, int day, int hour,
                               int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0,
                ZoneOffset.UTC).toInstant();
    }
}
