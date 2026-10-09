package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.JovianSystemService.Configuration;
import juranometria.solar.JovianSystemService.DiscRelation;
import juranometria.solar.JovianSystemService.JupiterObservation;
import juranometria.solar.JovianSystemService.Moon;
import juranometria.solar.JovianSystemService.MoonPlace;
import juranometria.solar.JovianSystemService.ShadowRelation;
import juranometria.ui.ChartComponent;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Jupiter and the Galilean moons on the atlas's pages, measured
 * (Sprint 45, issue #482): what a cartography rule would mean, through
 * the production projections and the accepted Jovian service, with
 * candidate marks drawn by this study over pages the production
 * composition painted - the Sun-Moon cartography study's method (#414).
 * Nothing here is production rendering; every image says MOCKUP in its
 * corner, and a magnified inset says it is a study magnification, not
 * a field the atlas offers.
 *
 * <p>Deterministic: fixed instants, fixed observers (Oslo at sea
 * level, as the contract's), the bundled packs, {@code Locale.ROOT}
 * numbers, nominal label boxes rather than measured fonts. The report
 * goes to stdout ({@code make jovian-cartography-study}); the mock-ups
 * are written beside it.
 */
public final class JovianCartographyStudyMain {

    private static final File DIR = new File("docs/studies/jovian-cartography");
    private static final int PAGE_W = 900;
    private static final int PAGE_H = 700;
    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-12-11T22:45:00Z"));

    /** The fields the atlas draws, its own ladder (ChartViewState's steps). */
    private static final double[] FIELDS = {180.0, 120.0, 90.0, 60.0, 42.0, 36.0, 24.0,
            18.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0};

    /**
     * The atlas's smallest visible star mark, as a diameter: twice the
     * production star policy's minimum radius (StarSizePolicy.DEFAULT).
     */
    private static final double MIN_INK_PX = 2.0 * juranometria.chart.StarSizePolicy.DEFAULT
            .minimumRadiusPx();
    /** The largest star mark, as a diameter. */
    private static final double MAX_STAR_PX = 2.0 * juranometria.chart.StarSizePolicy.DEFAULT
            .maximumRadiusPx();

    /** Proposed: Jupiter's minimum cartographic mark, a disc this many pixels across. */
    static final double JUPITER_MARK_PX = 6.0;
    /** Proposed: a moon's symbol, a dot this many pixels across - never an apparent size. */
    static final double MOON_SYMBOL_PX = 3.0;
    /** Proposed: two moon symbols are separable at this page distance, centre to centre. */
    static final double SEPARABLE_PX = MOON_SYMBOL_PX + 3.0;
    /** A nominal label line, for the threshold at which moon labels have room. */
    static final double LABEL_ROOM_PX = 14.0;

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));

    private static final StringBuilder REPORT = new StringBuilder();
    private static JovianSystemService service;
    /** The observer a page's drawn horizon is for, set before a horizon page. */
    private static Observer horizonObserver = OSLO;

    private JovianCartographyStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        DIR.mkdirs();
        service = JovianSystemService.load();
        p("# Jupiter and the Galilean moons on the page: measurements and mockups");
        p("");
        p("Sprint 45, issue #482. What a cartography rule would mean on the atlas's own"
                + " pages, measured through the production projections and the accepted"
                + " Jovian service (#473), with candidate marks drawn by this study over"
                + " pages the production composition painted, as the Sun-Moon study did"
                + " (#414). Nothing here is production rendering; every image is a mockup"
                + " and says so in its corner. Regenerate with `make"
                + " jovian-cartography-study`.");
        p("");
        Ranges ranges = ranges();
        scale(ranges);
        thresholds(ranges);
        frame();
        mockups();
        proposals(ranges);
        System.out.print(REPORT);
        System.out.flush();
    }

    // ---- the century's ranges ------------------------------------------

    /** Extremes over 2000-2100, weekly, geocentric: what a page must hold. */
    record Ranges(double eqMin, double eqMax, double polMin, double polMax,
                  double[] moonMin, double[] moonMax, double extent, String extentAt,
                  double tableDifference, double poleAgreement, double poleTurnMax,
                  int samples) {
    }

    static Ranges ranges() {
        double eqMin = 1e9, eqMax = 0, polMin = 1e9, polMax = 0, extent = 0;
        double[] mMin = {1e9, 1e9, 1e9, 1e9};
        double[] mMax = {0, 0, 0, 0};
        String extentAt = "";
        double tableDiff = 0, poleAgree = 0, poleTurn = 0;
        int n = 0;
        Instant t = Instant.parse("2000-01-01T00:00:00Z");
        Instant end = Instant.parse("2100-12-31T00:00:00Z");
        while (t.isBefore(end)) {
            Configuration c = service.observeMoonsGeocentric(t);
            JupiterObservation j = c.jupiter();
            n++;
            eqMin = Math.min(eqMin, j.equatorialDiameterArcseconds());
            eqMax = Math.max(eqMax, j.equatorialDiameterArcseconds());
            polMin = Math.min(polMin, j.polarDiameterArcseconds());
            polMax = Math.max(polMax, j.polarDiameterArcseconds());
            for (MoonPlace m : c.moons()) {
                int i = m.moon().ordinal();
                mMin[i] = Math.min(mMin[i], m.angularDiameterArcseconds());
                mMax[i] = Math.max(mMax[i], m.angularDiameterArcseconds());
                if (m.separationArcseconds() > extent) {
                    extent = m.separationArcseconds();
                    extentAt = m.moon() + " " + t.toString().substring(0, 10);
                }
                double[] j2000 = j2000Offset(j, m);
                tableDiff = Math.max(tableDiff, Math.hypot(j2000[0] - m.xArcseconds(),
                        j2000[1] - m.yArcseconds()));
            }
            double jd = service.timeScales().tt(t).jdTt();
            double derived = JovianChartGeometry.positionAngleJ2000(j.astrometricJ2000(),
                    JovianChartGeometry.pole(service.pack().constants(), jd));
            double carried = JovianChartGeometry.ofDateAngleCarriedToJ2000(j.apparentOfDate(),
                    j.poleAngleDegrees(), jd);
            poleAgree = Math.max(poleAgree, Math.abs(wrap(derived - carried)));
            poleTurn = Math.max(poleTurn, Math.abs(wrap(derived - j.poleAngleDegrees())));
            t = t.plus(Duration.ofDays(7));
        }
        return new Ranges(eqMin, eqMax, polMin, polMax, mMin, mMax, extent, extentAt,
                tableDiff, poleAgree, poleTurn, n);
    }

    /** A moon's offset from Jupiter in J2000, arcseconds, east and north, from the two J2000 places. */
    static double[] j2000Offset(JupiterObservation j, MoonPlace m) {
        double dra = wrap(m.astrometricJ2000().raDegrees() - j.astrometricJ2000().raDegrees())
                * Math.cos(Math.toRadians(j.astrometricJ2000().decDegrees())) * 3600.0;
        double ddec = (m.astrometricJ2000().decDegrees() - j.astrometricJ2000().decDegrees()) * 3600.0;
        return new double[] {dra, ddec};
    }

    // ---- A. true scale ----------------------------------------------------

    private static void scale(Ranges r) {
        p("## A. True scale on every page");
        p("");
        p(String.format(Locale.ROOT, "Over 2000-2100 (%d weekly geocentric instants from the"
                + " service): Jupiter's equatorial diameter is %.1f-%.1f″ and its polar"
                + " diameter %.1f-%.1f″; Io %.2f-%.2f″, Europa %.2f-%.2f″, Ganymede"
                + " %.2f-%.2f″, Callisto %.2f-%.2f″; the system's largest extent is a moon"
                + " %.0f″ (%.1f′) from Jupiter's centre (%s). The atlas's smallest star"
                + " mark is %.1f px across and its largest %.1f px (the production star"
                + " policy).", r.samples, r.eqMin, r.eqMax, r.polMin, r.polMax,
                r.moonMin[0], r.moonMax[0], r.moonMin[1], r.moonMax[1], r.moonMin[2],
                r.moonMax[2], r.moonMin[3], r.moonMax[3], r.extent, r.extent / 60.0,
                r.extentAt, MIN_INK_PX, MAX_STAR_PX));
        p("");
        int a4W = (int) Math.round(PaperSize.A4.chartWidePoints()
                * PngSheetWriter.DEFAULT_RESOLUTION / 72.0);
        p(String.format(Locale.ROOT, "Pixels on the 900 × 700 page at its centre (each field's"
                + " own projection); millimetres on the A4 sheet's chart area (%.0f mm wide,"
                + " %d px at %d dpi) and the Letter sheet's (%.0f mm).",
                PaperSize.A4.chartWideMm(), a4W, PngSheetWriter.DEFAULT_RESOLUTION,
                PaperSize.LETTER.chartWideMm()));
        p("");
        p("| field | Jupiter equator px (min-max) | polar px (max) | equator − polar px (max) | largest moon px | system extent px | Jupiter on A4 mm (max) | extent on A4 mm | Jupiter on Letter mm (max) |");
        p("|---:|---|---:|---:|---:|---:|---:|---:|---:|");
        for (double field : FIELDS) {
            double perArcsec = pxPerArcsec(field, PAGE_W, PAGE_H);
            double mmPerArcsec = PaperSize.A4.chartWideMm() / (field * 3600.0);
            double letterMm = PaperSize.LETTER.chartWideMm() / (field * 3600.0);
            p(String.format(Locale.ROOT, "| %.0f° | %.2f-%.2f | %.2f | %.3f | %.4f | %.1f | %.3f | %.2f | %.3f |",
                    field, r.eqMin * perArcsec, r.eqMax * perArcsec, r.polMax * perArcsec,
                    (r.eqMax - r.polMax) * perArcsec, r.moonMax[2] * perArcsec,
                    r.extent * perArcsec, r.eqMax * mmPerArcsec, r.extent * mmPerArcsec,
                    r.eqMax * letterMm));
        }
        p("");
    }

    /** Pixels per arcsecond at a page's centre, through its own projection. */
    static double pxPerArcsec(double field, int w, int h) {
        SkyPosition centre = new SkyPosition(150.0, 15.0);
        DrawnPage page = emptyPage(centre, field, w, h);
        ViewportMapping mapping = new ViewportMapping(page);
        Projection projection = page.projection();
        PixelPoint a = mapping.toPixel(projection.project(centre).orElseThrow());
        PixelPoint b = mapping.toPixel(projection.project(
                JovianChartGeometry.offset(centre, 0.0, 60.0 / 3600.0)).orElseThrow());
        return Math.hypot(a.x() - b.x(), a.y() - b.y()) / 60.0;
    }

    static DrawnPage emptyPage(SkyPosition centre, double field, int w, int h) {
        ChartViewport viewport = new ChartViewport(centre, field, w, h);
        return DrawnPage.of(new ChartScene(viewport, List.of(), List.of(), "study", 8.0));
    }

    // ---- B. thresholds ------------------------------------------------------

    private static void thresholds(Ranges r) {
        p("## B. Where things become resolvable or separable");
        p("");
        double perArcsecAt1 = pxPerArcsec(1.0, PAGE_W, PAGE_H);
        // fields at which a quantity reaches a number of pixels, scaling as 1/field
        p(String.format(Locale.ROOT, "- **Jupiter's disc reaches the atlas's smallest star"
                + " mark** (%.1f px) at %.1f° at its largest and %.1f° at its smallest; it"
                + " reaches the largest star mark (%.1f px) at %.2f° and %.2f°.",
                MIN_INK_PX, r.eqMax * perArcsecAt1 / MIN_INK_PX, r.eqMin * perArcsecAt1 / MIN_INK_PX,
                MAX_STAR_PX, r.eqMax * perArcsecAt1 / MAX_STAR_PX, r.eqMin * perArcsecAt1 / MAX_STAR_PX));
        p(String.format(Locale.ROOT, "- **Jupiter's disc is a shape, not a dot** (3 px across)"
                + " from %.1f° at its largest, %.1f° at its smallest; it reaches the"
                + " proposed %.0f px minimum mark at %.2f° and %.2f°.",
                r.eqMax * perArcsecAt1 / 3.0, r.eqMin * perArcsecAt1 / 3.0, JUPITER_MARK_PX,
                r.eqMax * perArcsecAt1 / JUPITER_MARK_PX, r.eqMin * perArcsecAt1 / JUPITER_MARK_PX));
        double flat = (r.eqMax - r.polMax) * perArcsecAt1;
        p(String.format(Locale.ROOT, "- **Jupiter's oblateness is honestly resolved** - the"
                + " equatorial and polar diameters a whole pixel apart - only at %.2f° on"
                + " the 900 px page at its largest (%.2f px apart at 1°): **below the"
                + " atlas's smallest field**. At 1° the true disc is %.1f-%.1f px across,"
                + " a disc whose oblate outline is within a pixel of a circle. On the A4"
                + " sheet at 300 dpi it is resolved from %.1f°. Drawing the outline oblate"
                + " is truthful at any size; seeing it needs a field below 1° (#481) or"
                + " paper.", flat, flat, r.eqMin * perArcsecAt1, r.eqMax * perArcsecAt1,
                (r.eqMax - r.polMax) * PaperSize.A4.chartWidePoints()
                        * PngSheetWriter.DEFAULT_RESOLUTION / 72.0 / 3600.0));
        p(String.format(Locale.ROOT, "- **The moons' true discs are below a pixel on every"
                + " page** (Ganymede, the largest, %.2f px at 1°): any moon mark is a symbol."
                + " Proposed symbol %.0f px; two symbols are separable %.0f px apart"
                + " (centre to centre).", r.moonMax[2] * perArcsecAt1, MOON_SYMBOL_PX, SEPARABLE_PX));
        p("");
        p("Separability over 2026 (every 6 hours at Oslo, 1 460 configurations): the share in"
                + " which every pair of moons, and every moon and Jupiter's mark, are at least"
                + " the given page distance apart, by field. *Marks* uses the symbol rule"
                + " above against Jupiter's drawn radius; *labels* uses a nominal label line"
                + " (" + (int) LABEL_ROOM_PX + " px) between every pair. A moon behind Jupiter is"
                + " left out (it is not drawn under any candidate).");
        p("");
        p("| field | marks separable | labels have room | median closest pair (px) |");
        p("|---:|---:|---:|---:|");
        List<Configuration> year = new ArrayList<>();
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plus(Duration.ofHours(6))) {
            year.add(service.observeMoons(OSLO.at(t)));
        }
        for (double field : new double[] {12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0}) {
            double perArcsec = pxPerArcsec(field, PAGE_W, PAGE_H);
            int marks = 0, labels = 0;
            List<Double> closest = new ArrayList<>();
            for (Configuration c : year) {
                double jupiterRadius = Math.max(JUPITER_MARK_PX,
                        c.jupiter().equatorialDiameterArcseconds() * perArcsec) / 2.0;
                double least = Double.MAX_VALUE;
                List<double[]> at = new ArrayList<>();
                for (MoonPlace m : c.moons()) {
                    if (m.discRelation() == DiscRelation.BEHIND) {
                        continue;
                    }
                    double[] o = j2000Offset(c.jupiter(), m);
                    at.add(new double[] {o[0] * perArcsec, o[1] * perArcsec});
                    double fromLimb = Math.hypot(o[0], o[1]) * perArcsec - jupiterRadius;
                    if (m.discRelation() == DiscRelation.CLEAR) {
                        least = Math.min(least, fromLimb + MOON_SYMBOL_PX / 2.0);
                    }
                }
                for (int a = 0; a < at.size(); a++) {
                    for (int b = a + 1; b < at.size(); b++) {
                        least = Math.min(least, Math.hypot(at.get(a)[0] - at.get(b)[0],
                                at.get(a)[1] - at.get(b)[1]));
                    }
                }
                if (least >= SEPARABLE_PX) {
                    marks++;
                }
                if (least >= LABEL_ROOM_PX) {
                    labels++;
                }
                closest.add(least);
            }
            closest.sort(Double::compare);
            p(String.format(Locale.ROOT, "| %.0f° | %.1f %% | %.1f %% | %.1f |", field,
                    100.0 * marks / year.size(), 100.0 * labels / year.size(),
                    closest.get(closest.size() / 2)));
        }
        p("");
    }

    // ---- C. the frame contract ------------------------------------------------

    private static void frame() {
        Ranges r = ranges();
        p("## C. The frame contract");
        p("");
        p("The chart is J2000 (ICRS); the table's X/Y and pole angle are apparent and of"
                + " date. The study does **not** rotate the table's values by an assumed"
                + " correction. It proposes, and `JovianChartFrameTest` holds:");
        p("");
        p("- **Positions**: each body's own astrometric J2000 place from the service -"
                + " Jupiter's and each moon's - through the page's ordinary projection, as"
                + " every star's and the Sun's and the Moon's.");
        p(String.format(Locale.ROOT, "- **Jupiter's pole for the ellipse**: the PCK's pole"
                + " vector is stated in the ICRS, the chart's frame, so its direction on the"
                + " sky at Jupiter's J2000 place is derived there, from J2000 north through"
                + " east, and turned onto the page by the page's projected north and east at"
                + " that place (the Moon's phase contract, C3). Measured over 2000-2100: the"
                + " derived angle agrees with the table's of-date pole angle carried back by"
                + " the atlas's own of-date-to-J2000 rotation to **%.4f°**; the two frames'"
                + " angles themselves differ by up to %.2f° - which is why the table's angle"
                + " is never drawn as it stands.", r.poleAgreement, r.poleTurnMax));
        p(String.format(Locale.ROOT, "- **The table's offsets are not the page's**: a"
                + " moon's J2000 offset from Jupiter differs from the table's apparent X/Y by"
                + " up to **%.2f″** over the century - the frames' rotation at Callisto's"
                + " distance - so a page draws each moon at its own J2000 place, never at"
                + " Jupiter's place plus X/Y.", r.tableDifference));
        p("- **Round trip and neighbours**: every body's J2000 place projected and"
                + " unprojected returns to itself; a moon's page offset from Jupiter equals"
                + " its J2000 offset turned by the page's tangents, across the sky (the"
                + " poles, the equator, RA 0/24h) and through Jupiter's JUP365 segment"
                + " boundary at 1997-01-16 (Jupiter only: the moons answer from 2000).");
        p("- **Agreement**: the page position of each body is the service's J2000 position,"
                + " not a second computation; the test compares them directly.");
        p("");
    }

    // ---- D. mockups ---------------------------------------------------------------

    /** A body as this study draws it. */
    record Mark(String label, SkyPosition at, double eqArcsec, double polArcsec,
                double poleAngleJ2000, boolean jupiter, DiscRelation disc,
                ShadowRelation shadow, boolean belowHorizon) {
    }

    private static void mockups() throws Exception {
        p("## D. Mockups on the atlas's own pages");
        p("");
        p("Each page is painted by the production composition (`ChartComponent` over the"
                + " bundled catalogue, with the meridian module where the page has a"
                + " horizon). Over it this study draws, from the service's J2000 places for"
                + " the instant and observer stated: **Jupiter** as a disc at its true"
                + " equatorial diameter or the proposed " + (int) JUPITER_MARK_PX + " px minimum"
                + " mark, whichever is larger, its outline turning from a circle to the true"
                + " oblate ellipse as the true disc grows past the minimum, the minor axis"
                + " along the pole derived in the chart's frame; **each moon** as a "
                + (int) MOON_SYMBOL_PX + " px symbol at its own J2000 place, in the state"
                + " vocabulary of candidate A (section E) unless the caption says otherwise;"
                + " labels to the right, refused when they would overlap. Where the true"
                + " system is a few pixels across, an inset magnifies it - a **study"
                + " magnification, not a field the atlas offers** - so the states can be"
                + " judged; the page itself is unmagnified.");
        p("");

        Instant triple = Instant.parse("2026-12-11T22:45:00Z");
        // 1. The field series at the triple transit.
        for (double field : new double[] {36.0, 8.0, 3.0, 1.0}) {
            Configuration c = service.observeMoons(OSLO.at(triple));
            page(String.format(Locale.ROOT, "field-%.0f", field),
                    String.format(Locale.ROOT, "The triple transit, 2026-12-11 22:45 UTC at Oslo, %.0f° field", field),
                    new ChartViewState(c.jupiter().astrometricJ2000(), field, 8.0, null, null),
                    field == 8.0 ? NORSK : ENGLISH,
                    field == 3.0 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    false, marks(c, false), field == 1.0 ? 12 : field == 3.0 ? 30 : 0, 'A',
                    String.format(Locale.ROOT, "Jupiter's true disc is %.1f px here; %s",
                            c.jupiter().equatorialDiameterArcseconds() * pxPerArcsec(field, PAGE_W, PAGE_H),
                            field >= 8.0 ? "the minimum mark stands for it and the moons share its few pixels."
                                    : "the moons are symbols; the inset shows the arrangement."));
        }
        // 2. The 11 December sequence at 1°, with an inset.
        for (String hhmm : List.of("22:30", "22:45", "22:55", "23:00")) {
            Instant t = Instant.parse("2026-12-11T" + hhmm + ":00Z");
            Configuration c = service.observeMoons(OSLO.at(t));
            page("triple-" + hhmm.replace(":", ""),
                    "The 11 December 2026 sequence at " + hhmm + " UTC, Oslo, 1° field",
                    new ChartViewState(c.jupiter().astrometricJ2000(), 1.0, 8.0, null, null),
                    hhmm.equals("22:55") ? NORSK : ENGLISH,
                    hhmm.equals("23:00") ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    false, marks(c, false), 12, 'A', states(c));
        }
        // 3. A moon behind Jupiter; full and partial shadow (Horizons' codes, December 2026 at Oslo).
        for (String[] k : new String[][] {
                {"behind", "2026-12-02T07:00:00Z", "Io behind Jupiter (Horizons O at this hour)"},
                {"shadow-full", "2026-12-02T04:00:00Z", "Io wholly in Jupiter's shadow (Horizons u)"},
                {"shadow-partial", "2026-12-21T15:00:00Z", "Io partly in Jupiter's shadow (Horizons p)"}}) {
            Configuration c = service.observeMoons(OSLO.at(Instant.parse(k[1])));
            page(k[0] + "-1", k[2] + ", " + k[1].replace("T", " ").replace(":00Z", " UTC") + ", 1° field",
                    new ChartViewState(c.jupiter().astrometricJ2000(), 1.0, 8.0, null, null),
                    ENGLISH, ChartPalette.WHITE_PAPER, false, marks(c, false), 12, 'A', states(c));
        }
        // 4. The two state vocabularies side by side, one real configuration per state.
        vocabulary();
        // 5. Below a drawn horizon, and with the horizon hidden.
        Instant low = belowTheHorizon();
        Configuration lc = service.observeMoons(OSLO.at(low));
        SkyPosition horizonCentre = nearestHorizonPoint(OSLO.at(low), lc.jupiter().astrometricJ2000());
        horizonObserver = OSLO.at(low);
        page("horizon-drawn-8", "Jupiter just below the drawn horizon, " + low.toString().replace("T", " ").replace(":00Z", " UTC") + ", Oslo, 8°",
                new ChartViewState(horizonCentre, 8.0, 8.0, null, null), ENGLISH,
                ChartPalette.WHITE_PAPER, true, marks(lc, true), 0, 'A',
                String.format(Locale.ROOT, "Jupiter at %.1f° altitude, drawn in the ground's dimmed ink with its status, as the Sun's and the Moon's ruling (C4) has it.",
                        lc.jupiter().horizontal().altitudeDegrees()));
        page("horizon-hidden-8", "The same instant with the horizon hidden",
                new ChartViewState(horizonCentre, 8.0, 8.0, null, null), NORSK,
                ChartPalette.WHITE_PAPER, false, marks(lc, false), 0, 'A',
                "With no horizon drawn the celestial chart draws Jupiter normally (C4).");
        // 6. Labels: crowded near Jupiter, and beside a bright star's label.
        Configuration crowd = crowded();
        page("labels-crowded-3", "The most crowded 2026 configuration with all four moons clear, 3° field",
                new ChartViewState(crowd.jupiter().astrometricJ2000(), 3.0, 8.0, null, null),
                ENGLISH, ChartPalette.WHITE_PAPER, false, marks(crowd, false), 30, 'A',
                "Moon labels refused on the page where they would overlap; the inset shows the moons.");
        SkyPosition regulus = new SkyPosition(152.093, 11.967);
        Configuration nearRegulus = service.observeMoons(OSLO.at(Instant.parse("2027-01-15T22:00:00Z")));
        SkyPosition between = new SkyPosition(
                (regulus.raDegrees() + nearRegulus.jupiter().astrometricJ2000().raDegrees()) / 2.0,
                (regulus.decDegrees() + nearRegulus.jupiter().astrometricJ2000().decDegrees()) / 2.0);
        page("labels-regulus-8", "Jupiter beside Regulus's label, 2027-01-15 22:00 UTC, 8° field",
                new ChartViewState(between, 8.0, 8.0, null, null), NORSK,
                ChartPalette.WHITE_PAPER, false, marks(nearRegulus, false), 0, 'A',
                String.format(Locale.ROOT, "Jupiter %.1f° from Regulus: its label takes a free adjacent place or is refused; it never overwrites the star's.",
                        nearRegulus.jupiter().astrometricJ2000().separationDegrees(regulus)));
        // 7. Near a page edge.
        Configuration edge = service.observeMoons(OSLO.at(triple));
        SkyPosition offCentre = JovianChartGeometry.offset(edge.jupiter().astrometricJ2000(), 90.0, -1.40);
        page("page-edge-3", "The system near the page's left edge, 3° field",
                new ChartViewState(offCentre, 3.0, 8.0, null, null), ENGLISH,
                ChartPalette.WHITE_PAPER, false, marks(edge, false), 0, 'A',
                "Bodies off the page are not drawn, as stars are not (C4).");
        // 8. Printable scale: the A4 sheet's chart area at 150 dpi, 8°.
        int sheetW = PngSheetWriter.widePixels(PaperSize.A4, 150);
        int sheetH = (int) Math.round(PaperSize.A4.chartHighPoints() * 150 / 72.0);
        Configuration print = service.observeMoons(OSLO.at(triple));
        page("printable-a4-8", String.format(Locale.ROOT, "The A4 sheet's chart area at 150 dpi (%d × %d px), 8° field", sheetW, sheetH),
                new ChartViewState(print.jupiter().astrometricJ2000(), 8.0, 8.0, null, null),
                ENGLISH, ChartPalette.WHITE_PAPER, false, marks(print, false), 0, 'A',
                sheetW, sheetH, String.format(Locale.ROOT, "Jupiter's true disc is %.2f mm on the sheet: the minimum mark stands for it.",
                        print.jupiter().equatorialDiameterArcseconds() * PaperSize.A4.chartWideMm() / (8.0 * 3600.0)));
    }

    /**
     * The state vocabularies, A above B: one cell per state, each Jupiter and
     * one moon from a real configuration (Horizons' code at that hour named),
     * the moon at its true J2000 offset, at a stated study magnification.
     */
    private static void vocabulary() throws Exception {
        String[][] cells = {
                {"clear of Jupiter", "2026-12-11T22:30:00Z", "IO", "Io, Horizons *"},
                {"in front of Jupiter", "2026-12-11T22:45:00Z", "EUROPA", "Europa, Horizons t"},
                {"behind Jupiter", "2026-12-02T07:00:00Z", "IO", "Io, Horizons O"},
                {"in Jupiter's shadow", "2026-12-02T04:00:00Z", "IO", "Io, Horizons u"},
                {"partly in shadow", "2026-12-21T15:00:00Z", "IO", "Io, Horizons p"}};
        int cell = 170;
        int w = cell * cells.length;
        int h = 2 * cell + 40;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        StringBuilder note = new StringBuilder();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            double scale = 2.0; // pixels per arcsecond: a study magnification
            for (int row = 0; row < 2; row++) {
                char vocabulary = row == 0 ? 'A' : 'B';
                for (int col = 0; col < cells.length; col++) {
                    Configuration c = service.observeMoons(OSLO.at(Instant.parse(cells[col][1])));
                    MoonPlace m = c.moon(Moon.valueOf(cells[col][2]));
                    double cx = col * cell + cell / 2.0;
                    double cy = row * cell + cell / 2.0 + 10;
                    double jd = service.timeScales().tt(c.instant()).jdTt();
                    double pole = JovianChartGeometry.positionAngleJ2000(c.jupiter().astrometricJ2000(),
                            JovianChartGeometry.pole(service.pack().constants(), jd));
                    double eq = c.jupiter().equatorialDiameterArcseconds() * scale;
                    double pol = c.jupiter().polarDiameterArcseconds() * scale;
                    // north up, east left, as the atlas's pages
                    double pa = Math.toRadians(pole);
                    AffineTransform keep = g.getTransform();
                    g.translate(cx, cy);
                    g.rotate(Math.atan2(-Math.cos(pa), -Math.sin(pa)) + Math.PI / 2.0);
                    Ellipse2D disc = new Ellipse2D.Double(-eq / 2, -pol / 2, eq, pol);
                    g.setColor(new Color(215, 205, 175));
                    g.fill(disc);
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(1f));
                    g.draw(disc);
                    g.setTransform(keep);
                    double[] off = j2000Offset(c.jupiter(), m);
                    double mx = cx - off[0] * scale;
                    double my = cy - off[1] * scale;
                    double r = MOON_SYMBOL_PX / 2.0;
                    boolean behind = m.discRelation() == DiscRelation.BEHIND;
                    boolean dim = m.shadowRelation() != ShadowRelation.SUNLIT;
                    Ellipse2D dot = new Ellipse2D.Double(mx - r, my - r, 2 * r, 2 * r);
                    if (behind && vocabulary == 'A') {
                        // not drawn
                    } else if (behind) {
                        g.setColor(Color.BLACK);
                        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                                10f, new float[] {1.5f, 1.5f}, 0f));
                        g.draw(new Ellipse2D.Double(mx - r - 1, my - r - 1, 2 * r + 2, 2 * r + 2));
                    } else if (m.discRelation() == DiscRelation.IN_FRONT) {
                        if (vocabulary == 'A') {
                            g.setColor(Color.WHITE);
                            g.fill(new Ellipse2D.Double(mx - r - 1, my - r - 1, 2 * r + 2, 2 * r + 2));
                        }
                        g.setColor(Color.BLACK);
                        g.fill(dot);
                    } else if (dim) {
                        if (vocabulary == 'A') {
                            g.setColor(Color.BLACK);
                            g.setStroke(new BasicStroke(1f));
                            g.draw(dot);
                        } else {
                            g.setColor(new Color(150, 150, 150));
                            g.fill(dot);
                        }
                    } else {
                        g.setColor(Color.BLACK);
                        g.fill(dot);
                    }
                    g.setColor(Color.DARK_GRAY);
                    g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
                    if (row == 0) {
                        g.drawString(cells[col][0], (int) (col * cell + 6), 14);
                    }
                    g.drawString(cells[col][3] + " · " + vocabulary, (int) (col * cell + 6),
                            (int) (row * cell + cell + 4));
                    if (row == 0) {
                        note.append(String.format(Locale.ROOT, "%s: %s at %s, offset %.1f″ east, %.1f″ north; ",
                                cells[col][0], cells[col][3], cells[col][1].replace("T", " ").replace(":00Z", " UTC"),
                                off[0], off[1]));
                    }
                }
            }
            g.setColor(Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
            g.drawString("MOCKUP - state vocabularies A (top) and B (bottom); study magnification 2 px/″, north up, east left (#482)",
                    6, h - 6);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, "states-vocabulary.png"));
        p("### The state vocabularies, A above B");
        p("");
        p("![](states-vocabulary.png)");
        p("");
        p("One cell per state, each a real configuration at Oslo (Horizons' visibility code at"
                + " that hour named), Jupiter's true oblate outline and pole and the moon at its"
                + " true J2000 offset, at a study magnification of 2 px per arcsecond. A: behind"
                + " not drawn; in front ringed in paper over the disc; in shadow a hollow ring."
                + " B: behind a dashed ghost; in shadow grey. Drawn: " + note.toString().strip());
        p("");
    }

    private static String states(Configuration c) {
        StringBuilder s = new StringBuilder("States: ");
        for (MoonPlace m : c.moons()) {
            s.append(name(m.moon())).append(' ').append(m.state().name().toLowerCase(Locale.ROOT)
                    .replace('_', ' ')).append("; ");
        }
        return s.toString().strip();
    }

    private static String name(Moon m) {
        return m.name().charAt(0) + m.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private static List<Mark> marks(Configuration c, boolean horizonDrawn) {
        List<Mark> marks = new ArrayList<>();
        JupiterObservation j = c.jupiter();
        double jd = service.timeScales().tt(c.instant()).jdTt();
        double pole = JovianChartGeometry.positionAngleJ2000(j.astrometricJ2000(),
                JovianChartGeometry.pole(service.pack().constants(), jd));
        boolean below = horizonDrawn && j.horizontal() != null && j.horizontal().altitudeDegrees() < 0;
        marks.add(new Mark("Jupiter", j.astrometricJ2000(), j.equatorialDiameterArcseconds(),
                j.polarDiameterArcseconds(), pole, true, DiscRelation.CLEAR, ShadowRelation.SUNLIT, below));
        for (MoonPlace m : c.moons()) {
            marks.add(new Mark(name(m.moon()), m.astrometricJ2000(), m.angularDiameterArcseconds(),
                    m.angularDiameterArcseconds(), 0.0, false, m.discRelation(), m.shadowRelation(), below));
        }
        return marks;
    }

    /** The first 2026 evening hour at Oslo with Jupiter between 1° and 4° below the horizon. */
    private static Instant belowTheHorizon() {
        for (Instant t = Instant.parse("2026-12-01T12:00:00Z");; t = t.plus(Duration.ofMinutes(10))) {
            double alt = service.observeJupiter(OSLO.at(t)).horizontal().altitudeDegrees();
            if (alt < -1.0 && alt > -4.0) {
                return t;
            }
        }
    }

    private static SkyPosition nearestHorizonPoint(Observer o, SkyPosition near) {
        juranometria.sky.LocalSky sky = new juranometria.sky.LocalSky(o);
        SkyPosition best = near;
        double least = Double.MAX_VALUE;
        for (SkyPosition on : sky.horizon().around(720)) {
            double sep = on.separationDegrees(near);
            if (sep < least) {
                least = sep;
                best = on;
            }
        }
        return best;
    }

    /** The 2026 configuration (6-hourly, Oslo) with all four moons clear and the closest pair nearest. */
    private static Configuration crowded() {
        Configuration best = null;
        double least = Double.MAX_VALUE;
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plus(Duration.ofHours(6))) {
            Configuration c = service.observeMoons(OSLO.at(t));
            if (c.moons().stream().anyMatch(m -> m.state() != JovianSystemService.VisibilityState.CLEAR_OF_JUPITER)) {
                continue;
            }
            for (int a = 0; a < 4; a++) {
                for (int b = a + 1; b < 4; b++) {
                    double[] oa = j2000Offset(c.jupiter(), c.moons().get(a));
                    double[] ob = j2000Offset(c.jupiter(), c.moons().get(b));
                    double d = Math.hypot(oa[0] - ob[0], oa[1] - ob[1]);
                    if (d < least) {
                        least = d;
                        best = c;
                    }
                }
            }
        }
        return best;
    }

    private static void page(String name, String title, ChartViewState state, PageText words,
                             ChartPalette palette, boolean withMeridian, List<Mark> marks,
                             int inset, char vocabulary, String caption) throws Exception {
        page(name, title, state, words, palette, withMeridian, marks, inset, vocabulary,
                PAGE_W, PAGE_H, caption);
    }

    private static void page(String name, String title, ChartViewState state, PageText words,
                             ChartPalette palette, boolean withMeridian, List<Mark> marks,
                             int inset, char vocabulary, int w, int h, String caption)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(w, h);
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            holder[0].setViewState(state);
            if (withMeridian) {
                MeridianModule meridian = new MeridianModule(horizonObserver);
                holder[0].overlays().offer(MeridianModule.ID, meridian::contributedGeometry);
            }
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
        ChartComponent chart = holder[0];
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        DrawnPage drawn = DrawnPage.of(chart.currentScene());
        ViewportMapping mapping = new ViewportMapping(drawn);
        Projection projection = drawn.projection();
        boolean dark = palette == ChartPalette.BLACK_SKY;
        Graphics2D g = image.createGraphics();
        StringBuilder note = new StringBuilder();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            List<Rectangle2D> taken = new ArrayList<>();
            drawSystem(g, projection, mapping, marks, 1.0, 0, 0, vocabulary, dark, taken, note, w, h);
            if (inset > 0) {
                Mark jupiter = marks.get(0);
                PixelPoint c = mapping.toPixel(projection.project(jupiter.at()).orElseThrow());
                int box = 300;
                int bx = w - box - 12;
                int by = 12;
                g.setColor(dark ? new Color(10, 10, 20) : new Color(252, 252, 248));
                g.fillRect(bx, by, box, box);
                g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                g.drawRect(bx, by, box, box);
                Graphics2D gi = (Graphics2D) g.create();
                gi.clipRect(bx + 1, by + 1, box - 1, box - 1);
                drawSystem(gi, projection, mapping, marks, inset, bx + box / 2.0 - c.x() * inset,
                        by + box / 2.0 - c.y() * inset, vocabulary, dark, new ArrayList<>(),
                        new StringBuilder(), w, h);
                gi.dispose();
                g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                g.setFont(g.getFont().deriveFont(10f));
                g.drawString(String.format(Locale.ROOT, "study magnification ×%d - not a field the atlas offers", inset),
                        bx + 6, by + box - 6);
            }
            g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
            g.drawString("MOCKUP - study ink over a production page (#482)", 8, h - 8);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        p("### " + title);
        p("");
        p("![](" + name + ".png)");
        p("");
        p(caption + " Drawn: " + note.toString().strip());
        p("");
    }

    /** Jupiter and its moons, at a magnification about the page (1 for the page itself). */
    private static void drawSystem(Graphics2D g, Projection projection, ViewportMapping mapping,
                                   List<Mark> marks, double zoom, double ox, double oy,
                                   char vocabulary, boolean dark, List<Rectangle2D> taken,
                                   StringBuilder note, int w, int h) {
        Mark jupiter = marks.get(0);
        var projected = projection.project(jupiter.at());
        if (projected.isEmpty()) {
            return;
        }
        PixelPoint jc = mapping.toPixel(projected.get());
        double perArcsec = pxPerArcsecAt(projection, mapping, jupiter.at());
        double trueEq = jupiter.eqArcsec() * perArcsec * zoom;
        double truePol = jupiter.polArcsec() * perArcsec * zoom;
        double minimum = JUPITER_MARK_PX;
        double drawnEq = Math.max(trueEq, minimum);
        // Continuous transition: a circle at the minimum, the true axis ratio once the
        // true disc is twice the minimum, linear between.
        double blend = Math.max(0.0, Math.min(1.0, (trueEq - minimum) / minimum));
        double ratio = 1.0 - (1.0 - truePol / trueEq) * blend;
        double drawnPol = drawnEq * ratio;
        double x = jc.x() * zoom + ox;
        double y = jc.y() * zoom + oy;
        boolean onPage = zoom != 1.0 || (x > -drawnEq && x < w + drawnEq && y > -drawnEq && y < h + drawnEq);
        if (!onPage) {
            note.append("Jupiter off the page, nothing drawn; ");
            return;
        }
        Color ink = jupiter.belowHorizon() ? (dark ? new Color(90, 90, 90) : new Color(170, 170, 170))
                : (dark ? Color.WHITE : Color.BLACK);
        Color paper = dark ? new Color(10, 10, 20) : Color.WHITE;
        double[] up = JovianChartGeometry.pageDirection(projection, mapping, jupiter.at(),
                jupiter.poleAngleJ2000());
        double rot = Math.atan2(up[1], up[0]) + Math.PI / 2.0;
        AffineTransform keep = g.getTransform();
        g.translate(x, y);
        g.rotate(rot);
        Ellipse2D disc = new Ellipse2D.Double(-drawnEq / 2, -drawnPol / 2, drawnEq, drawnPol);
        g.setColor(dark ? new Color(200, 190, 160) : new Color(215, 205, 175));
        g.fill(disc);
        g.setColor(ink);
        g.setStroke(new BasicStroke(1f));
        g.draw(disc);
        g.setTransform(keep);
        if (zoom != 1.0) {
            // the pole direction, for the study's eye only
            g.setColor(new Color(200, 60, 60));
            g.drawLine((int) x, (int) y, (int) (x + up[0] * drawnPol * 0.8), (int) (y + up[1] * drawnPol * 0.8));
        }
        note.append(String.format(Locale.ROOT, "Jupiter at (%.0f, %.0f), true %.1f × %.1f px, drawn %.1f × %.1f px, pole %.1f° (J2000); ",
                x, y, trueEq, truePol, drawnEq, drawnPol, jupiter.poleAngleJ2000()));
        label(g, jupiter.label() + (jupiter.belowHorizon() ? " (below the horizon)" : ""),
                x + drawnEq / 2 + 4, y + 4, ink, taken, zoom == 1.0 ? 12f : 11f);
        // Proposal 4: on the page itself the moons are drawn only where the
        // system is separable - every drawn pair, and every clear moon and
        // Jupiter's drawn edge, at least the separability distance apart.
        if (zoom == 1.0 && !separable(projection, mapping, marks, drawnEq / 2.0)) {
            note.append("moons not separable at this field, not drawn; ");
            return;
        }
        for (Mark m : marks.subList(1, marks.size())) {
            var p = projection.project(m.at());
            if (p.isEmpty()) {
                continue;
            }
            PixelPoint mc = mapping.toPixel(p.get());
            double mx = mc.x() * zoom + ox;
            double my = mc.y() * zoom + oy;
            double r = MOON_SYMBOL_PX / 2.0;
            Ellipse2D dot = new Ellipse2D.Double(mx - r, my - r, 2 * r, 2 * r);
            boolean behind = m.disc() == DiscRelation.BEHIND;
            boolean dim = m.shadow() != ShadowRelation.SUNLIT;
            if (behind && vocabulary == 'A') {
                note.append(m.label()).append(" behind, not drawn; ");
                continue;
            }
            if (behind) {
                g.setColor(ink);
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {1.5f, 1.5f}, 0f));
                g.draw(new Ellipse2D.Double(mx - r - 1, my - r - 1, 2 * r + 2, 2 * r + 2));
            } else if (m.disc() == DiscRelation.IN_FRONT) {
                if (vocabulary == 'A') {
                    g.setColor(paper);
                    g.fill(new Ellipse2D.Double(mx - r - 1, my - r - 1, 2 * r + 2, 2 * r + 2));
                }
                g.setColor(ink);
                g.fill(dot);
            } else if (dim) {
                if (vocabulary == 'A') {
                    g.setColor(ink);
                    g.setStroke(new BasicStroke(1f));
                    g.draw(dot);
                } else {
                    g.setColor(new Color(150, 150, 150));
                    g.fill(dot);
                }
            } else {
                g.setColor(ink);
                g.fill(dot);
            }
            if (zoom == 1.0) {
                note.append(String.format(Locale.ROOT, "%s at (%.0f, %.0f) %s; ", m.label(), mx, my,
                        behind ? "behind" : m.disc() == DiscRelation.IN_FRONT ? "in front"
                                : dim ? "shadowed" : "clear"));
            }
            label(g, m.label(), mx + r + 3, my + 4, ink, taken, zoom == 1.0 ? 10f : 10f);
        }
    }

    /** Whether the moons' symbols can be told apart on the page (section B's rule). */
    private static boolean separable(Projection projection, ViewportMapping mapping,
                                     List<Mark> marks, double jupiterRadiusPx) {
        PixelPoint jc = mapping.toPixel(projection.project(marks.get(0).at()).orElseThrow());
        List<PixelPoint> drawn = new ArrayList<>();
        for (Mark m : marks.subList(1, marks.size())) {
            if (m.disc() == DiscRelation.BEHIND) {
                continue;
            }
            PixelPoint c = mapping.toPixel(projection.project(m.at()).orElseThrow());
            if (m.disc() == DiscRelation.CLEAR && Math.hypot(c.x() - jc.x(), c.y() - jc.y())
                    - jupiterRadiusPx + MOON_SYMBOL_PX / 2.0 < SEPARABLE_PX) {
                return false;
            }
            for (PixelPoint other : drawn) {
                if (Math.hypot(c.x() - other.x(), c.y() - other.y()) < SEPARABLE_PX) {
                    return false;
                }
            }
            drawn.add(c);
        }
        return true;
    }

    /** A label to the right of its mark, refused where it would overlap one already placed. */
    private static void label(Graphics2D g, String text, double x, double y, Color ink,
                              List<Rectangle2D> taken, float size) {
        g.setFont(g.getFont().deriveFont(Font.PLAIN, size));
        // a nominal box: 0.6 em a character, one line high - not a measured font
        Rectangle2D box = new Rectangle2D.Double(x, y - size, text.length() * size * 0.6, size + 2);
        for (Rectangle2D other : taken) {
            if (other.intersects(box)) {
                return;
            }
        }
        taken.add(box);
        g.setColor(ink);
        g.drawString(text, (float) x, (float) y);
    }

    private static double pxPerArcsecAt(Projection projection, ViewportMapping mapping, SkyPosition at) {
        PixelPoint a = mapping.toPixel(projection.project(at).orElseThrow());
        PixelPoint b = mapping.toPixel(projection.project(
                JovianChartGeometry.offset(at, 0.0, 10.0 / 3600.0)).orElseThrow());
        return Math.hypot(a.x() - b.x(), a.y() - b.y()) / 10.0;
    }

    // ---- E. proposals -------------------------------------------------------------

    private static void proposals(Ranges r) {
        p("## E. What the measurements say, and what is proposed for ruling");
        p("");
        p("The issue's eight product questions, each with the measured reason and the"
                + " proposal; nothing is built until the owner rules.");
        p("");
        p("1. **One remembered switch, `Jupiter and moons on the chart`, off by default** -"
                + " the Sun's and the Moon's model: one switch for the body, the Controller's"
                + " box and View's item following it, remembered like theirs.");
        p(String.format(Locale.ROOT, "2. **Jupiter at ordinary fields: a cartographic mark"
                + " plus its label.** Unlike the Sun and the Moon (C1: true scale, always),"
                + " Jupiter's true disc is %.2f-%.2f px at 8°, below the atlas's smallest"
                + " star mark (%.1f px) at every field above %.1f°: true scale alone would"
                + " lose it. Proposed: a disc of %.0f px or its true size, whichever is"
                + " larger, in its own neutral ink, with its label - a symbol, stated as one"
                + " in the legend and the inspector, never offered as its size.",
                r.eqMin * pxPerArcsec(8.0, PAGE_W, PAGE_H), r.eqMax * pxPerArcsec(8.0, PAGE_W, PAGE_H),
                MIN_INK_PX, r.eqMax * pxPerArcsec(1.0, PAGE_W, PAGE_H) / MIN_INK_PX, JUPITER_MARK_PX));
        p(String.format(Locale.ROOT, "3. **Jupiter when resolvable: the true oblate disc**,"
                + " its minor axis along the pole derived in the chart's frame, no bands, Great"
                + " Red Spot or rotation. The transition is continuous and measured: the mark"
                + " is a circle at its %.0f px minimum and becomes the true ellipse, axis ratio"
                + " %.3f, as the true disc grows to twice that; at 1° the true disc is"
                + " %.1f-%.1f px, so the outline is near a circle until a finer field (#481) or"
                + " paper. The pole is drawn in no mark - only the outline shows it.",
                JUPITER_MARK_PX, r.polMax / r.eqMax, r.eqMin * pxPerArcsec(1.0, PAGE_W, PAGE_H),
                r.eqMax * pxPerArcsec(1.0, PAGE_W, PAGE_H)));
        p(String.format(Locale.ROOT, "4. **Moons: symbols, never apparent diameters.** Their"
                + " true discs are below a pixel on every page (Ganymede %.2f px at 1°)."
                + " Proposed: a %.0f px dot, stated as a symbol; drawn only where the system"
                + " is separable (section B) - at wider fields the moons are not drawn and"
                + " Jupiter's mark alone stands for the system.",
                r.moonMax[2] * pxPerArcsec(1.0, PAGE_W, PAGE_H), MOON_SYMBOL_PX));
        p("5. **Hidden, eclipsed and transiting moons** - two vocabularies are drawn"
                + " (section D). **Proposed: candidate A** - clear: a filled dot; in front of"
                + " Jupiter: a filled dot ringed in paper so it reads over the disc; behind"
                + " Jupiter: not drawn, its label refused, the table saying where it is; in"
                + " Jupiter's shadow (wholly or partly): a hollow ring, there but dark. B"
                + " (behind as a dashed ghost, shadow as grey) is the alternative; it draws"
                + " something the reader cannot see. No satellite shadow spots on Jupiter:"
                + " the service does not compute them.");
        p("6. **Moon labels**: drawn only where section B gives them room (a label line"
                + " between every pair), to the right of the mark, refused rather than"
                + " overlapping; Jupiter's label as the Sun's and the Moon's (C4): adjacent"
                + " candidates, refused rather than overwriting a star's.");
        p("7. **Horizon, page edge, stars, clicking, export** - as the Sun's and the Moon's"
                + " ruling (C4, C6): below a drawn horizon, dimmed with its status; with the"
                + " horizon hidden, drawn normally; off the page, nothing; the marks do not"
                + " hide stars (Jupiter's mark is a symbol, not an opaque disc of its true"
                + " size, until it is resolvable); clicking Jupiter may consume the hit but"
                + " never exposes a hidden star; exported sheets draw what the page draws, at"
                + " the sheet's scale.");
        p("8. **Centre on chart for Jupiter** - #483's accepted action, its seam prepared:"
                + " compute the typed draft, enable the layer, centre on Jupiter's J2000"
                + " place, choose 1° (the chart's normal minimum; the target may ask for a"
                + " finer field once #481 offers one), bring the chart forward, leave the"
                + " Controller or dialog open. The button lands with the visible module (#484).");
        p("");
        p("**Future close zoom (#481)**: the projection, the sizing rule and the labels above"
                + " are field-independent - the mark's minimum, the transition and the"
                + " separability thresholds are in pixels, the positions in J2000 - so a field"
                + " below 1° needs no change to them. No sub-1° field is exposed or"
                + " implemented here.");
    }

    private static double wrap(double degrees) {
        double d = juranometria.sky.SkyFrame.normalise(degrees);
        return d > 180.0 ? d - 360.0 : d;
    }

    // unused import guard
    @SuppressWarnings("unused")
    private static final Class<?> PLANE = PlanePoint.class;
}
