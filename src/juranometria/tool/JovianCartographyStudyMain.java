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
 * the ruled marks drawn by this study over pages the production
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
                + " Jovian service (#473), with the ruled marks drawn by this study over"
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
        rulings(ranges);
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
                + " ruled %.0f px minimum mark at %.2f° and %.2f°.",
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
        p("**Each moon decided on its own (the ruled rule 4).** Over 2026 (every 6 hours at"
                + " Oslo, 1 460 configurations, 5 840 moon places) each moon is decided"
                + " independently by the study's `decide`: behind Jupiter, omitted; at the chart's"
                + " normal minimum field (" + fmt(ChartViewState.normalMinimumFieldDegrees())
                + "°, read from the chart and never assumed permanent) every other moon drawn at"
                + " its exact J2000 place, overlap allowed; above it, a moon drawn only when its own"
                + " " + (int) MOON_SYMBOL_PX + " px mark is distinguishable - " + (int) SEPARABLE_PX
                + " px from Jupiter's drawn edge and from every higher-priority moon already drawn"
                + " (in front, then clear, then shadowed; then Ganymede, Callisto, Io, Europa), and"
                + " a moon in front only when Jupiter's drawn disc is at least "
                + (int) (2 * SEPARABLE_PX) + " px. A collision suppresses only the lower-priority"
                + " mark. Shares are of the moon places that are not behind Jupiter.");
        p("");
        p("| field | drawn | at Jupiter's edge | collides | in front, unresolved | configurations with a moon drawn | one suppressed, another drawn |");
        p("|---:|---:|---:|---:|---:|---:|---:|");
        List<Configuration> year = new ArrayList<>();
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plus(Duration.ofHours(6))) {
            year.add(service.observeMoons(OSLO.at(t)));
        }
        double[] ladder = {12.0, 8.0, 6.0, 4.0, 3.0, 2.0, ChartViewState.normalMinimumFieldDegrees()};
        java.awt.FontMetrics font;
        Graphics2D probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            font = juranometria.render.ChartRenderer.TextMetrics.of(probe).labels();
        } finally {
            probe.dispose();
        }
        List<String> labelRows = new ArrayList<>();
        for (double field : ladder) {
            double perArcsec = pxPerArcsec(field, PAGE_W, PAGE_H);
            int[] count = new int[Decision.values().length];
            int seen = 0, anyDrawn = 0, independent = 0;
            int placed = 0, refused = 0, mixed = 0, allRefused = 0, moved = 0;
            for (Configuration c : year) {
                double jupiterPx = Math.max(JUPITER_MARK_PX,
                        c.jupiter().equatorialDiameterArcseconds() * perArcsec);
                List<double[]> off = pageOffsets(c, perArcsec);
                List<Decision> d = decide(field, ChartViewState.normalMinimumFieldDegrees(),
                        jupiterPx, c.moons(), off);
                int drawnHere = 0;
                boolean suppressed = false;
                for (Decision x : d) {
                    count[x.ordinal()]++;
                    if (x != Decision.BEHIND) {
                        seen++;
                    }
                    drawnHere += x == Decision.DRAWN ? 1 : 0;
                    suppressed |= x == Decision.COLLIDES || x == Decision.AT_JUPITER
                            || x == Decision.IN_FRONT_UNRESOLVED;
                }
                anyDrawn += drawnHere > 0 ? 1 : 0;
                independent += suppressed && drawnHere > 0 ? 1 : 0;
                int[] l = labels(jupiterPx, c, off, d, font);
                placed += l[0];
                refused += l[1];
                moved += l[2];
                mixed += l[0] > 0 && l[1] > 0 ? 1 : 0;
                allRefused += l[0] == 0 && l[1] > 0 ? 1 : 0;
            }
            p(String.format(Locale.ROOT, "| %s° | %.1f %% | %.1f %% | %.1f %% | %.1f %% | %.1f %% | %d |",
                    fmt(field), 100.0 * count[Decision.DRAWN.ordinal()] / seen,
                    100.0 * count[Decision.AT_JUPITER.ordinal()] / seen,
                    100.0 * count[Decision.COLLIDES.ordinal()] / seen,
                    100.0 * count[Decision.IN_FRONT_UNRESOLVED.ordinal()] / seen,
                    100.0 * anyDrawn / year.size(), independent));
            labelRows.add(String.format(Locale.ROOT, "| %s° | %d | %d | %d | %d | %d |", fmt(field),
                    placed, moved, refused, mixed, allRefused));
        }
        p("");
        p("**Labels, decided separately from marks (the ruled rule 6).** For every drawn moon,"
                + " a label request through the production `LabelPlacement`, with production's"
                + " eight adjacent candidates (LabelGeometry's order, 3 px gap), every drawn mark"
                + " and Jupiter's label as obstacles, on an otherwise empty 900 × 700 page; a"
                + " label is refused when none of its candidates is free - where production"
                + " would place it under duress over the least ink - and withdrawn, so it"
                + " blocks no other label. *Moved* counts"
                + " labels that took a candidate other than the right-hand one.");
        p("");
        p("| field | moon labels placed | of which moved | refused | configurations with both | configurations with every moon label refused |");
        p("|---:|---:|---:|---:|---:|---:|");
        labelRows.forEach(JovianCartographyStudyMain::p);
        p("");
        p("The rejected all-or-nothing rule (every moon drawn only when the whole system is"
                + " separable) is no longer measured; its figures are in the history of PR #489.");
        p("");
    }

    /** The label outcome for one configuration on an empty page: placed, refused, moved off the right. */
    private static int[] labels(double jupiterPx, Configuration c, List<double[]> off,
                                List<Decision> d, java.awt.FontMetrics font) {
        double cx = PAGE_W / 2.0;
        double cy = PAGE_H / 2.0;
        double r = MOON_SYMBOL_PX / 2.0;
        List<juranometria.render.LabelPlacement.Obstacle> marks = new ArrayList<>();
        List<juranometria.render.LabelPlacement.Request> requests = new ArrayList<>();
        marks.add(new juranometria.render.LabelPlacement.Obstacle(
                juranometria.render.LabelPlacement.Refusal.MARK, "jupiter",
                new Ellipse2D.Double(cx - jupiterPx / 2, cy - jupiterPx / 2, jupiterPx, jupiterPx)));
        requests.add(new juranometria.render.LabelPlacement.Request(
                juranometria.render.LabelPlacement.Family.STAR, "jupiter", "Jupiter", cx, cy,
                around(cx, cy, jupiterPx / 2, font.stringWidth("Jupiter"), font.getHeight(), font.getAscent()),
                "jupiter", null, false, 0));
        for (int i = 0; i < d.size(); i++) {
            if (d.get(i) != Decision.DRAWN) {
                continue;
            }
            String id = c.moons().get(i).moon().name();
            String text = name(c.moons().get(i).moon());
            double x = cx + off.get(i)[0];
            double y = cy + off.get(i)[1];
            marks.add(new juranometria.render.LabelPlacement.Obstacle(
                    juranometria.render.LabelPlacement.Refusal.MARK, id,
                    new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r)));
            requests.add(new juranometria.render.LabelPlacement.Request(
                    juranometria.render.LabelPlacement.Family.STAR, id, text, x, y,
                    around(x, y, r, font.stringWidth(text), font.getHeight(), font.getAscent()),
                    id, null, false, i + 1));
        }
        int[] out = new int[3];
        for (juranometria.render.LabelPlacement.Placement placed
                : placeRefusing(PAGE_W, PAGE_H, marks,
                        juranometria.render.LabelPlacement.Page.paper(PAGE_W, PAGE_H), requests)) {
            if (placed.request().id().equals("jupiter")) {
                continue;
            }
            if (placed.omitted()) {
                out[1]++;
            } else {
                out[0]++;
                out[2] += placed.candidate() != 0 ? 1 : 0;
            }
        }
        return out;
    }

    private static String fmt(double degrees) {
        return degrees == Math.rint(degrees) ? String.valueOf((long) degrees) : String.valueOf(degrees);
    }

    // ---- C. the frame contract ------------------------------------------------

    private static void frame() {
        Ranges r = ranges();
        p("## C. The frame contract");
        p("");
        p("The chart is J2000 (ICRS); the table's X/Y and pole angle are apparent and of"
                + " date. The study does **not** rotate the table's values by an assumed"
                + " correction. As ruled, `JovianChartFrameTest` holds:");
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
                ShadowRelation shadow, boolean belowHorizon, MoonPlace place) {
    }

    private static void mockups() throws Exception {
        p("## D. Mockups on the atlas's own pages");
        p("");
        p("Each page is painted by the production composition (`ChartComponent` over the"
                + " bundled catalogue, with the meridian module where the page has a"
                + " horizon). Over it this study draws, from the service's J2000 places for"
                + " the instant and observer stated, by the owner's rulings (section E):"
                + " **Jupiter** as a disc at its true equatorial diameter or the "
                + (int) JUPITER_MARK_PX + " px minimum - a cartographic symbol, not Jupiter's"
                + " apparent diameter - whichever is larger, its outline turning from a circle"
                + " to the true oblate ellipse as the true disc grows past the minimum, the"
                + " minor axis along the pole derived in the chart's frame; **each moon** as a "
                + (int) MOON_SYMBOL_PX + " px symbol at its own J2000 place, decided on its own"
                + " (section B), in state vocabulary A; **labels** through the production"
                + " `LabelPlacement` with its eight adjacent candidates and the page's own text"
                + " and marks as obstacles, each refused alone when no candidate is free. Where"
                + " the true system is a few pixels across, an inset magnifies it - a **study"
                + " magnification, not a field the atlas offers** - so the states can be"
                + " judged; the page itself is unmagnified, and every caption ends with what"
                + " the page itself drew.");
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
                    false, marks(c, false), field == 1.0 ? 12 : field == 3.0 ? 30 : 0,
                    String.format(Locale.ROOT, "Jupiter's true disc is %.1f px here; %s",
                            c.jupiter().equatorialDiameterArcseconds() * pxPerArcsec(field, PAGE_W, PAGE_H),
                            field > ChartViewState.normalMinimumFieldDegrees()
                                    ? "above the normal minimum field each moon is drawn only where its own mark is distinguishable; the inset shows the arrangement."
                                    : "at the normal minimum field every moon not behind Jupiter is drawn at its exact place, the three in front overlapping Jupiter's mark on purpose."));
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
                    false, marks(c, false), 12, states(c));
        }
        // 3. A moon behind Jupiter; full and partial shadow (Horizons' codes, December 2026 at Oslo).
        for (String[] k : new String[][] {
                {"behind", "2026-12-02T07:00:00Z", "Io behind Jupiter (Horizons O at this hour)"},
                {"shadow-full", "2026-12-02T04:00:00Z", "Io wholly in Jupiter's shadow (Horizons u)"},
                {"shadow-partial", "2026-12-21T15:00:00Z", "Io partly in Jupiter's shadow (Horizons p)"}}) {
            Configuration c = service.observeMoons(OSLO.at(Instant.parse(k[1])));
            page(k[0] + "-1", k[2] + ", " + k[1].replace("T", " ").replace(":00Z", " UTC") + ", 1° field",
                    new ChartViewState(c.jupiter().astrometricJ2000(), 1.0, 8.0, null, null),
                    ENGLISH, ChartPalette.WHITE_PAPER, false, marks(c, false), 12, states(c));
        }
        // 4. State vocabulary A on both palettes, one real configuration per state.
        vocabulary();
        // 5. Below a drawn horizon, and with the horizon hidden.
        Instant low = belowTheHorizon();
        Configuration lc = service.observeMoons(OSLO.at(low));
        SkyPosition horizonCentre = nearestHorizonPoint(OSLO.at(low), lc.jupiter().astrometricJ2000());
        horizonObserver = OSLO.at(low);
        page("horizon-drawn-8", "Jupiter just below the drawn horizon, " + low.toString().replace("T", " ").replace(":00Z", " UTC") + ", Oslo, 8°",
                new ChartViewState(horizonCentre, 8.0, 8.0, null, null), ENGLISH,
                ChartPalette.WHITE_PAPER, true, marks(lc, true), 0,
                String.format(Locale.ROOT, "Jupiter at %.1f° altitude, drawn in the ground's dimmed ink with its status, as the Sun's and the Moon's ruling (C4) has it.",
                        lc.jupiter().horizontal().altitudeDegrees()));
        page("horizon-hidden-8", "The same instant with the horizon hidden",
                new ChartViewState(horizonCentre, 8.0, 8.0, null, null), NORSK,
                ChartPalette.WHITE_PAPER, false, marks(lc, false), 0,
                "With no horizon drawn the celestial chart draws Jupiter normally (C4).");
        // 6. Labels: crowded near Jupiter, and beside a bright star's label.
        Configuration crowd = crowded();
        page("labels-crowded-3", "The most crowded 2026 configuration with all four moons clear, 3° field",
                new ChartViewState(crowd.jupiter().astrometricJ2000(), 3.0, 8.0, null, null),
                ENGLISH, ChartPalette.WHITE_PAPER, false, marks(crowd, false), 30,
                "Each mark and each label is decided on its own: a label with no free candidate is refused alone, and the others stand; the inset shows the moons.");
        // Independent suppression: one crowded pair loses only its lower-priority mark.
        Configuration pair = independentSuppression(3.0);
        page("independent-suppression-3", "A crowded pair at 3°: only the lower-priority mark is suppressed",
                new ChartViewState(pair.jupiter().astrometricJ2000(), 3.0, 8.0, null, null),
                ENGLISH, ChartPalette.BLACK_SKY, false, marks(pair, false), 30,
                pair.instant().toString().replace("T", " ").replace(":00Z", " UTC")
                        + " at Oslo. Two moons collide at this field: the lower-priority one is"
                        + " not drawn, while every other distinguishable moon is.");
        // Labels around Jupiter: adjacent candidates, not right-only.
        Configuration around = labelsAround();
        page("labels-around-1", "Labels on adjacent candidates around Jupiter, 1° field",
                new ChartViewState(around.jupiter().astrometricJ2000(), 1.0, 8.0, null, null),
                NORSK, ChartPalette.WHITE_PAPER, false, marks(around, false), 0,
                around.instant().toString().replace("T", " ").replace(":00Z", " UTC")
                        + " at Oslo. Each label takes the first of its eight adjacent candidates"
                        + " that covers no mark or text; a crowded one is refused alone.");
        SkyPosition regulus = new SkyPosition(152.093, 11.967);
        Configuration nearRegulus = service.observeMoons(OSLO.at(Instant.parse("2027-01-15T22:00:00Z")));
        SkyPosition between = new SkyPosition(
                (regulus.raDegrees() + nearRegulus.jupiter().astrometricJ2000().raDegrees()) / 2.0,
                (regulus.decDegrees() + nearRegulus.jupiter().astrometricJ2000().decDegrees()) / 2.0);
        page("labels-regulus-8", "Jupiter beside Regulus's label, 2027-01-15 22:00 UTC, 8° field",
                new ChartViewState(between, 8.0, 8.0, null, null), NORSK,
                ChartPalette.WHITE_PAPER, false, marks(nearRegulus, false), 0,
                String.format(Locale.ROOT, "Jupiter %.1f° from Regulus: its label takes a free adjacent place or is refused; it never overwrites the star's.",
                        nearRegulus.jupiter().astrometricJ2000().separationDegrees(regulus)));
        // 7. Near a page edge.
        Configuration edge = service.observeMoons(OSLO.at(triple));
        SkyPosition offCentre = JovianChartGeometry.offset(edge.jupiter().astrometricJ2000(), 90.0, -1.40);
        page("page-edge-3", "The system near the page's left edge, 3° field",
                new ChartViewState(offCentre, 3.0, 8.0, null, null), ENGLISH,
                ChartPalette.WHITE_PAPER, false, marks(edge, false), 0,
                "Bodies off the page are not drawn, as stars are not (C4).");
        // 8. Printable scale: the A4 sheet's chart area at 150 dpi, 8°.
        int sheetW = PngSheetWriter.widePixels(PaperSize.A4, 150);
        int sheetH = (int) Math.round(PaperSize.A4.chartHighPoints() * 150 / 72.0);
        Configuration print = service.observeMoons(OSLO.at(triple));
        page("printable-a4-8", String.format(Locale.ROOT, "The A4 sheet's chart area at 150 dpi (%d × %d px), 8° field", sheetW, sheetH),
                new ChartViewState(print.jupiter().astrometricJ2000(), 8.0, 8.0, null, null),
                ENGLISH, ChartPalette.WHITE_PAPER, false, marks(print, false), 0,
                sheetW, sheetH, String.format(Locale.ROOT, "Jupiter's true disc is %.2f mm on the sheet: the minimum mark stands for it.",
                        print.jupiter().equatorialDiameterArcseconds() * PaperSize.A4.chartWideMm() / (8.0 * 3600.0)));
    }

    /**
     * State vocabulary A on white paper above and black sky below: one cell per state, each Jupiter and
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
            g.fillRect(0, 0, w, cell + 10);
            g.setColor(new Color(10, 10, 20));
            g.fillRect(0, cell + 10, w, h - cell - 10);
            double scale = 2.0; // pixels per arcsecond: a study magnification
            for (int row = 0; row < 2; row++) {
                boolean dark = row == 1;
                Color ink = dark ? Color.WHITE : Color.BLACK;
                Color paper = dark ? new Color(10, 10, 20) : Color.WHITE;
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
                    g.setColor(dark ? new Color(200, 190, 160) : new Color(215, 205, 175));
                    g.fill(disc);
                    g.setColor(ink);
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
                    if (behind) {
                        // vocabulary A: a moon that cannot be seen is not drawn
                    } else if (m.discRelation() == DiscRelation.IN_FRONT) {
                        g.setColor(paper);
                        g.fill(new Ellipse2D.Double(mx - r - 1, my - r - 1, 2 * r + 2, 2 * r + 2));
                        g.setColor(ink);
                        g.fill(dot);
                    } else if (dim) {
                        g.setColor(ink);
                        g.setStroke(new BasicStroke(1f));
                        g.draw(dot);
                    } else {
                        g.setColor(ink);
                        g.fill(dot);
                    }
                    g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                    g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
                    if (row == 0) {
                        g.drawString(cells[col][0], (int) (col * cell + 6), 14);
                    }
                    g.drawString(cells[col][3] + (dark ? " · sky" : " · paper"),
                            (int) (col * cell + 6), (int) (row * cell + cell + 4));
                    if (row == 0) {
                        note.append(String.format(Locale.ROOT, "%s: %s at %s, offset %.1f″ east, %.1f″ north; ",
                                cells[col][0], cells[col][3], cells[col][1].replace("T", " ").replace(":00Z", " UTC"),
                                off[0], off[1]));
                    }
                }
            }
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
            g.drawString("MOCKUP - state vocabulary A, as ruled, on white paper and black sky; study magnification 2 px/″, north up, east left (#482)",
                    6, h - 6);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, "states-vocabulary.png"));
        p("### State vocabulary A, as ruled, on both palettes");
        p("");
        p("![](states-vocabulary.png)");
        p("");
        p("One cell per state, each a real configuration at Oslo (Horizons' visibility code at"
                + " that hour named), Jupiter's true oblate outline and pole and the moon at its"
                + " true J2000 offset, at a study magnification of 2 px per arcsecond, on white"
                + " paper above and black sky below: clear a filled dot; in front a filled dot"
                + " ringed in the paper's own colour over the disc; behind not drawn; wholly or"
                + " partly in shadow a hollow ring (the table keeps the distinction). Drawn: "
                + note.toString().strip());
        p("");
    }

    /** A 2026 configuration (6-hourly, Oslo) where, at a field, one moon collides and another is drawn. */
    private static Configuration independentSuppression(double field) {
        double perArcsec = pxPerArcsec(field, PAGE_W, PAGE_H);
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plus(Duration.ofHours(6))) {
            Configuration c = service.observeMoons(OSLO.at(t));
            List<Decision> d = decide(field, ChartViewState.normalMinimumFieldDegrees(),
                    Math.max(JUPITER_MARK_PX, c.jupiter().equatorialDiameterArcseconds() * perArcsec),
                    c.moons(), pageOffsets(c, perArcsec));
            if (d.contains(Decision.COLLIDES) && d.stream().filter(x -> x == Decision.DRAWN).count() >= 2) {
                return c;
            }
        }
        throw new IllegalStateException("no such configuration in 2026");
    }

    /** A 2026 night configuration with all four moons clear and well apart at 1°: their labels must move. */
    private static Configuration labelsAround() {
        double perArcsec = pxPerArcsec(1.0, PAGE_W, PAGE_H);
        Configuration best = null;
        double tightest = Double.MAX_VALUE;
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plus(Duration.ofHours(6))) {
            Configuration c = service.observeMoons(OSLO.at(t));
            if (c.moons().stream().anyMatch(m -> m.state() != JovianSystemService.VisibilityState.CLEAR_OF_JUPITER)) {
                continue;
            }
            List<double[]> off = pageOffsets(c, perArcsec);
            double least = Double.MAX_VALUE;
            for (int a = 0; a < 4; a++) {
                for (int b = a + 1; b < 4; b++) {
                    least = Math.min(least, Math.hypot(off.get(a)[0] - off.get(b)[0],
                            off.get(a)[1] - off.get(b)[1]));
                }
            }
            // crowded enough that right-hand labels collide, open enough that marks are distinct
            if (least >= SEPARABLE_PX && least < tightest) {
                tightest = least;
                best = c;
            }
        }
        return best;
    }

    /** Each moon's offset from Jupiter in page pixels at a scale, east to page-left, north up. */
    static List<double[]> pageOffsets(Configuration c, double perArcsec) {
        List<double[]> out = new ArrayList<>();
        for (MoonPlace m : c.moons()) {
            double[] o = j2000Offset(c.jupiter(), m);
            out.add(new double[] {-o[0] * perArcsec, -o[1] * perArcsec});
        }
        return out;
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
                j.polarDiameterArcseconds(), pole, true, DiscRelation.CLEAR, ShadowRelation.SUNLIT, below, null));
        for (MoonPlace m : c.moons()) {
            marks.add(new Mark(name(m.moon()), m.astrometricJ2000(), m.angularDiameterArcseconds(),
                    m.angularDiameterArcseconds(), 0.0, false, m.discRelation(), m.shadowRelation(), below, m));
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

    /** Why a moon is or is not drawn on a page (the owner's ruling 4 on #482). */
    enum Decision {
        DRAWN, BEHIND, AT_JUPITER, COLLIDES, IN_FRONT_UNRESOLVED
    }

    /** The order a collision is settled in: in front, then clear, then shadowed; then the larger moon. */
    static final List<Moon> SIZE_ORDER = List.of(Moon.GANYMEDE, Moon.CALLISTO, Moon.IO, Moon.EUROPA);

    static int priority(MoonPlace m) {
        int state = m.discRelation() == DiscRelation.IN_FRONT ? 0
                : m.shadowRelation() == ShadowRelation.SUNLIT ? 1 : 2;
        return state * 10 + SIZE_ORDER.indexOf(m.moon());
    }

    /**
     * Which moons a page draws, each decided on its own (ruling 4):
     * a moon behind Jupiter is omitted; at the chart's normal minimum
     * field (1° today, never assumed permanent) every other moon is
     * drawn at its exact place, its symbol overlapping Jupiter's or
     * another's when the real configuration is unresolved; above it, a
     * moon in front of Jupiter is drawn only when Jupiter's drawn disc
     * can hold a symbol with room (twice the separability distance), a
     * clear or shadowed moon only when its symbol stands clear of
     * Jupiter's drawn edge, and of every moon of higher priority already
     * drawn - so a crowded pair loses only its lower-priority mark.
     *
     * @param offsetsPx each moon's page offset from Jupiter's centre, in pixels, in table order
     */
    static List<Decision> decide(double fieldDegrees, double normalMinimumDegrees,
                                 double jupiterDrawnPx, List<MoonPlace> moons,
                                 List<double[]> offsetsPx) {
        Decision[] out = new Decision[moons.size()];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < moons.size(); i++) {
            order.add(i);
        }
        order.sort((x, y) -> Integer.compare(priority(moons.get(x)), priority(moons.get(y))));
        boolean floor = fieldDegrees <= normalMinimumDegrees + 1e-9;
        List<double[]> drawn = new ArrayList<>();
        double r = MOON_SYMBOL_PX / 2.0;
        for (int i : order) {
            MoonPlace m = moons.get(i);
            double[] at = offsetsPx.get(i);
            if (m.discRelation() == DiscRelation.BEHIND) {
                out[i] = Decision.BEHIND;
                continue;
            }
            if (floor) {
                out[i] = Decision.DRAWN;
                drawn.add(at);
                continue;
            }
            if (m.discRelation() == DiscRelation.IN_FRONT) {
                out[i] = jupiterDrawnPx >= 2.0 * SEPARABLE_PX ? Decision.DRAWN
                        : Decision.IN_FRONT_UNRESOLVED;
            } else if (Math.hypot(at[0], at[1]) - jupiterDrawnPx / 2.0 < SEPARABLE_PX / 2.0 + r) {
                out[i] = Decision.AT_JUPITER;
            } else {
                out[i] = Decision.DRAWN;
                for (double[] other : drawn) {
                    if (Math.hypot(at[0] - other[0], at[1] - other[1]) < SEPARABLE_PX) {
                        out[i] = Decision.COLLIDES;
                        break;
                    }
                }
            }
            if (out[i] == Decision.DRAWN) {
                drawn.add(at);
            }
        }
        return List.of(out);
    }


    /**
     * The production placement, refusing where production would place
     * under duress (the ruled rule 6: refused when there is no room).
     * A refused label is withdrawn and the rest placed again, so it
     * never blocks another; the result holds a placement per request,
     * a refused one with no box.
     */
    static List<juranometria.render.LabelPlacement.Placement> placeRefusing(
            double w, double h, List<juranometria.render.LabelPlacement.Obstacle> obstacles,
            juranometria.render.LabelPlacement.Page page,
            List<juranometria.render.LabelPlacement.Request> requests) {
        List<juranometria.render.LabelPlacement.Request> live = new ArrayList<>(requests);
        java.util.Map<String, juranometria.render.LabelPlacement.Placement> out = new java.util.LinkedHashMap<>();
        while (true) {
            List<juranometria.render.LabelPlacement.Placement> placed =
                    new juranometria.render.LabelPlacement(w, h, obstacles, page).placeAll(live);
            juranometria.render.LabelPlacement.Placement first = placed.stream()
                    .filter(x -> x.underDuress() || x.omitted()).findFirst().orElse(null);
            if (first == null) {
                placed.forEach(x -> out.put(x.request().id(), x));
                break;
            }
            out.put(first.request().id(), new juranometria.render.LabelPlacement.Placement(
                    first.request(), null, first.candidate(), first.refusals(), false));
            live.remove(first.request());
        }
        List<juranometria.render.LabelPlacement.Placement> ordered = new ArrayList<>();
        for (juranometria.render.LabelPlacement.Request r : requests) {
            ordered.add(out.get(r.id()));
        }
        return ordered;
    }

    /** The eight adjacent label boxes around a mark, in production's own order (LabelGeometry). */
    static List<Rectangle2D> around(double x, double y, double reach, double width,
                                    double height, double ascent) {
        double[][] steps = {{1, 0}, {-1, 0}, {1, -1}, {-1, -1}, {1, 1}, {-1, 1}, {0, -1}, {0, 1}};
        double gap = 3.0;
        List<Rectangle2D> boxes = new ArrayList<>();
        for (double[] step : steps) {
            double left = step[0] > 0 ? x + reach + gap
                    : step[0] < 0 ? x - reach - gap - width : x - width / 2.0;
            double baseline = y + step[1] * (reach + gap + height / 2.0) + ascent / 2.0 - 1.0;
            boxes.add(new Rectangle2D.Double(left, baseline - ascent, width, height));
        }
        return boxes;
    }

    /** A body this study drew, with where its label went. */
    record Drawn(String id, String label, double x, double y, double reach, Mark mark) {
    }

    private static void page(String name, String title, ChartViewState state, PageText words,
                             ChartPalette palette, boolean withMeridian, List<Mark> marks,
                             int inset, String caption) throws Exception {
        page(name, title, state, words, palette, withMeridian, marks, inset, PAGE_W, PAGE_H, caption);
    }

    private static void page(String name, String title, ChartViewState state, PageText words,
                             ChartPalette palette, boolean withMeridian, List<Mark> marks,
                             int inset, int w, int h, String caption) throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        ChartOptions options = ChartOptions.DEFAULTS.withPalette(palette);
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(w, h);
            holder[0].setChartOptions(options);
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
        ChartScene scene = chart.currentScene();
        DrawnPage drawnPage = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(drawnPage);
        Projection projection = drawnPage.projection();
        boolean dark = palette == ChartPalette.BLACK_SKY;
        Graphics2D g = image.createGraphics();
        StringBuilder note = new StringBuilder();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // The page's own obstacles and accepted text, asked of the
            // production renderer the way production asks it.
            juranometria.render.ChartRenderer renderer = new juranometria.render.ChartRenderer(
                    juranometria.chart.StarSizePolicy.DEFAULT, chart.words());
            juranometria.render.ChartRenderer.TextMetrics tm =
                    juranometria.render.ChartRenderer.TextMetrics.of(g);
            List<juranometria.render.LabelPlacement.Obstacle> obstacles =
                    new ArrayList<>(renderer.textObstacles(tm, scene, options));
            for (juranometria.render.LabelPlacement.Placement placed
                    : renderer.textPlacements(tm, scene, options)) {
                if (!placed.omitted()) {
                    obstacles.add(new juranometria.render.LabelPlacement.Obstacle(
                            juranometria.render.LabelPlacement.Refusal.TEXT,
                            placed.request().id(), placed.at()));
                }
            }
            drawSystem(g, projection, mapping, marks, scene.viewport().fieldWidthDegrees(), 1.0, 0, 0,
                    dark, obstacles, new juranometria.render.LabelPlacement.Page(
                            new Rectangle2D.Double(0, 0, w, h), "the paper"), tm.labels(), note, w, h);
            if (inset > 0) {
                PixelPoint c = mapping.toPixel(projection.project(marks.get(0).at()).orElseThrow());
                int box = 300;
                int bx = w - box - 12;
                int by = 12;
                g.setColor(dark ? new Color(10, 10, 20) : new Color(252, 252, 248));
                g.fillRect(bx, by, box, box);
                g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                g.drawRect(bx, by, box, box);
                Graphics2D gi = (Graphics2D) g.create();
                gi.clipRect(bx + 1, by + 1, box - 1, box - 1);
                StringBuilder insetNote = new StringBuilder();
                drawSystem(gi, projection, mapping, marks, 0.0, inset,
                        bx + box / 2.0 - c.x() * inset, by + box / 2.0 - c.y() * inset, dark,
                        new ArrayList<>(), new juranometria.render.LabelPlacement.Page(
                                new Rectangle2D.Double(bx + 2, by + 2, box - 4, box - 20), "the inset"),
                        tm.labels(), insetNote, w, h);
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
        p(caption + " Drawn on the page: " + note.toString().strip());
        p("");
    }

    /**
     * Jupiter and its moons by the ruled rules: the page itself (zoom 1,
     * at its field) or a study magnification (every moon not behind
     * Jupiter, since the inset resolves them). Marks first, then labels
     * through the production LabelPlacement over the given obstacles.
     */
    private static void drawSystem(Graphics2D g, Projection projection, ViewportMapping mapping,
                                   List<Mark> marks, double fieldDegrees, double zoom, double ox,
                                   double oy, boolean dark,
                                   List<juranometria.render.LabelPlacement.Obstacle> obstacles,
                                   juranometria.render.LabelPlacement.Page paper,
                                   java.awt.FontMetrics font, StringBuilder note, int w, int h) {
        Mark jupiter = marks.get(0);
        var projected = projection.project(jupiter.at());
        if (projected.isEmpty()) {
            return;
        }
        PixelPoint jc = mapping.toPixel(projected.get());
        double perArcsec = pxPerArcsecAt(projection, mapping, jupiter.at());
        double trueEq = jupiter.eqArcsec() * perArcsec * zoom;
        double truePol = jupiter.polArcsec() * perArcsec * zoom;
        double drawnEq = Math.max(trueEq, JUPITER_MARK_PX);
        double blend = Math.max(0.0, Math.min(1.0, (trueEq - JUPITER_MARK_PX) / JUPITER_MARK_PX));
        double drawnPol = drawnEq * (1.0 - (1.0 - truePol / trueEq) * blend);
        double x = jc.x() * zoom + ox;
        double y = jc.y() * zoom + oy;
        if (zoom == 1.0 && !(x > -drawnEq && x < w + drawnEq && y > -drawnEq && y < h + drawnEq)) {
            note.append("Jupiter off the page, nothing drawn; ");
            return;
        }
        Color ink = jupiter.belowHorizon() ? (dark ? new Color(90, 90, 90) : new Color(170, 170, 170))
                : (dark ? Color.WHITE : Color.BLACK);
        Color paperInk = dark ? new Color(10, 10, 20) : Color.WHITE;
        double[] up = JovianChartGeometry.pageDirection(projection, mapping, jupiter.at(),
                jupiter.poleAngleJ2000());
        AffineTransform keep = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.atan2(up[1], up[0]) + Math.PI / 2.0);
        Ellipse2D disc = new Ellipse2D.Double(-drawnEq / 2, -drawnPol / 2, drawnEq, drawnPol);
        g.setColor(dark ? new Color(200, 190, 160) : new Color(215, 205, 175));
        g.fill(disc);
        g.setColor(ink);
        g.setStroke(new BasicStroke(1f));
        g.draw(disc);
        g.setTransform(keep);
        note.append(String.format(Locale.ROOT, "Jupiter at (%.0f, %.0f), true %.1f × %.1f px, drawn %.1f × %.1f px%s; ",
                x, y, trueEq, truePol, drawnEq, drawnPol,
                trueEq < JUPITER_MARK_PX ? " (the minimum mark: a cartographic symbol)" : ""));
        List<Drawn> drawn = new ArrayList<>();
        drawn.add(new Drawn("jupiter", jupiter.label() + (jupiter.belowHorizon() ? " (below the horizon)" : ""),
                x, y, drawnEq / 2.0, jupiter));
        List<MoonPlace> places = new ArrayList<>();
        List<Mark> moonMarks = marks.subList(1, marks.size());
        List<double[]> offsets = new ArrayList<>();
        List<double[]> at = new ArrayList<>();
        for (Mark m : moonMarks) {
            PixelPoint mc = mapping.toPixel(projection.project(m.at()).orElseThrow());
            double mx = mc.x() * zoom + ox;
            double my = mc.y() * zoom + oy;
            at.add(new double[] {mx, my});
            offsets.add(new double[] {mx - x, my - y});
            places.add(m.place());
        }
        List<Decision> decisions = zoom == 1.0
                ? decide(fieldDegrees, ChartViewState.normalMinimumFieldDegrees(), drawnEq, places, offsets)
                : decide(0.0, ChartViewState.normalMinimumFieldDegrees(), drawnEq, places, offsets);
        double r = MOON_SYMBOL_PX / 2.0;
        for (int i = 0; i < moonMarks.size(); i++) {
            Mark m = moonMarks.get(i);
            double mx = at.get(i)[0];
            double my = at.get(i)[1];
            Decision d = decisions.get(i);
            if (d != Decision.DRAWN) {
                note.append(m.label()).append(' ').append(d.name().toLowerCase(Locale.ROOT)
                        .replace('_', ' ')).append(", not drawn; ");
                continue;
            }
            Ellipse2D dot = new Ellipse2D.Double(mx - r, my - r, 2 * r, 2 * r);
            boolean dim = m.shadow() != ShadowRelation.SUNLIT;
            if (m.disc() == DiscRelation.IN_FRONT) {
                g.setColor(paperInk);
                g.fill(new Ellipse2D.Double(mx - r - 1, my - r - 1, 2 * r + 2, 2 * r + 2));
                g.setColor(ink);
                g.fill(dot);
            } else if (dim) {
                g.setColor(ink);
                g.setStroke(new BasicStroke(1f));
                g.draw(dot);
            } else {
                g.setColor(ink);
                g.fill(dot);
            }
            note.append(String.format(Locale.ROOT, "%s at (%.0f, %.0f) %s; ", m.label(), mx, my,
                    m.disc() == DiscRelation.IN_FRONT ? "in front" : dim ? "shadowed" : "clear"));
            drawn.add(new Drawn(m.label().toLowerCase(Locale.ROOT), m.label(), mx, my, r, m));
        }
        // Labels, decided separately from marks: the production placement,
        // its eight adjacent candidates, refusing rather than overlapping.
        List<juranometria.render.LabelPlacement.Obstacle> occupied = new ArrayList<>(obstacles);
        for (Drawn d : drawn) {
            occupied.add(new juranometria.render.LabelPlacement.Obstacle(
                    juranometria.render.LabelPlacement.Refusal.MARK, d.id(),
                    new Ellipse2D.Double(d.x() - d.reach(), d.y() - d.reach(), 2 * d.reach(), 2 * d.reach())));
        }
        List<juranometria.render.LabelPlacement.Request> requests = new ArrayList<>();
        for (int i = 0; i < drawn.size(); i++) {
            Drawn d = drawn.get(i);
            double width = font.stringWidth(d.label());
            requests.add(new juranometria.render.LabelPlacement.Request(
                    juranometria.render.LabelPlacement.Family.STAR, d.id(), d.label(), d.x(), d.y(),
                    around(d.x(), d.y(), d.reach(), width, font.getHeight(), font.getAscent()),
                    d.id(), null, false, i));
        }
        g.setFont(font.getFont());
        for (juranometria.render.LabelPlacement.Placement placed
                : placeRefusing(w, h, occupied, paper, requests)) {
            if (placed.omitted()) {
                if (zoom == 1.0) {
                    note.append(placed.request().text()).append("'s label refused; ");
                }
                continue;
            }
            g.setColor(ink(placed.request().id(), drawn, dark));
            Rectangle2D box = placed.at();
            g.drawString(placed.request().text(), (float) box.getX(),
                    (float) (box.getY() + font.getAscent()));
            if (zoom == 1.0 && placed.candidate() != 0) {
                note.append(String.format(Locale.ROOT, "%s's label at candidate %d; ",
                        placed.request().text(), placed.candidate()));
            }
        }
    }

    private static Color ink(String id, List<Drawn> drawn, boolean dark) {
        for (Drawn d : drawn) {
            if (d.id().equals(id) && d.mark().belowHorizon()) {
                return dark ? new Color(90, 90, 90) : new Color(170, 170, 170);
            }
        }
        return dark ? Color.WHITE : Color.BLACK;
    }

    private static double pxPerArcsecAt(Projection projection, ViewportMapping mapping, SkyPosition at) {
        PixelPoint a = mapping.toPixel(projection.project(at).orElseThrow());
        PixelPoint b = mapping.toPixel(projection.project(
                JovianChartGeometry.offset(at, 0.0, 10.0 / 3600.0)).orElseThrow());
        return Math.hypot(a.x() - b.x(), a.y() - b.y()) / 10.0;
    }

    // ---- E. the rulings -------------------------------------------------------------

    private static void rulings(Ranges r) {
        p("## E. The owner's rulings, with their measured reasons");
        p("");
        p("The issue's product questions, as the owner ruled them on #482. Nothing is"
                + " built here: production rendering is #484.");
        p("");
        p("1. **One switch, `Jupiter and moons on the chart`, off by default and"
                + " remembered** - the Sun's and the Moon's model.");
        p(String.format(Locale.ROOT, "2. **Jupiter's mark is %.0f px or its true size, whichever"
                + " is larger.** True scale alone would lose it: its true disc is %.2f-%.2f px at"
                + " 8°, below the atlas's smallest star mark (%.1f px) at every field above"
                + " %.1f°. The evidence and the accessible text call the minimum *a cartographic"
                + " symbol, not Jupiter's apparent diameter*.",
                JUPITER_MARK_PX, r.eqMin * pxPerArcsec(8.0, PAGE_W, PAGE_H),
                r.eqMax * pxPerArcsec(8.0, PAGE_W, PAGE_H), MIN_INK_PX,
                r.eqMax * pxPerArcsec(1.0, PAGE_W, PAGE_H) / MIN_INK_PX));
        p(String.format(Locale.ROOT, "3. **The true figure and pole, through the continuous"
                + " transition and the J2000-derived pole.** The mark is a circle at its %.0f px"
                + " minimum and reaches the true axis ratio (%.3f) as the true disc grows to"
                + " twice that; the minor axis lies along the pole derived in the chart's frame."
                + " No bands, Great Red Spot or rotation. **The flattening is not visibly"
                + " resolved at the current %s° floor**: there the true disc is %.1f-%.1f px and"
                + " its two axes under a pixel apart.",
                JUPITER_MARK_PX, r.polMax / r.eqMax,
                fmt(ChartViewState.normalMinimumFieldDegrees()),
                r.eqMin * pxPerArcsec(1.0, PAGE_W, PAGE_H), r.eqMax * pxPerArcsec(1.0, PAGE_W, PAGE_H)));
        p(String.format(Locale.ROOT, "4. **Moons are %.0f px symbolic marks, each decided on its"
                + " own** (section B). At the normal minimum field every moon not behind Jupiter"
                + " is drawn at its exact J2000 place, overlap allowed; a moon in front of"
                + " Jupiter overlaps it on purpose; a moon behind is omitted; outside the disc a"
                + " collision suppresses only the lower-priority mark; above the minimum a moon"
                + " is drawn only when its own mark is distinguishable. The rejected rule - all"
                + " moons only when the whole system is separable - is gone; the 22:45 triple"
                + " transit keeps its three in-front marks at 1° (section D).", MOON_SYMBOL_PX));
        p("5. **State vocabulary A** (section D, both palettes): clear, a filled dot; in front,"
                + " a filled dot with a paper or background ring; behind, omitted; wholly or"
                + " partly in shadow, a hollow ring (the two share it; the table keeps the"
                + " difference). No ghost. No satellite shadow spots on Jupiter: the service does"
                + " not compute them.");
        p("6. **Labels through adjacent candidates and the existing collision machinery**,"
                + " decided separately from marks: production's eight candidates, the page's own"
                + " text and marks as obstacles, a label refused alone when none of its"
                + " candidates is free. No fixed right-hand rule; one crowded pair never"
                + " suppresses every moon label.");
        p("7. **Horizon, page edge, stars, clicks and export**: dimmed below a drawn horizon;"
                + " drawn normally with the horizon hidden; nothing off the page; the symbolic"
                + " mark never erases a real star; its hit area never exposes a hidden star;"
                + " export draws through the same module.");
        p("8. **Centre on chart for Jupiter** through #483's accepted seam; its button lands"
                + " with the visible module (#484).");
        p("9. **Future close zoom (#481)**: no geometry, label or evidence code may assume 1°"
                + " is permanent. The study reads the chart's normal minimum field rather than"
                + " naming 1°; the mark's minimum, the transition and the distinguishability"
                + " distances are in pixels and the positions in J2000.");
        p("");
    }

    private static double wrap(double degrees) {
        double d = juranometria.sky.SkyFrame.normalise(degrees);
        return d > 180.0 ? d - 360.0 : d;
    }

    // unused import guard
    @SuppressWarnings("unused")
    private static final Class<?> PLANE = PlanePoint.class;
}
