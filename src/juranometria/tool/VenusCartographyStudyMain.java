package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.module.OverlayContribution;
import juranometria.project.DrawnPage;
import juranometria.project.PageBasis;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.VenusService;
import juranometria.solar.VenusService.VenusObservation;
import juranometria.ui.ChartComponent;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Venus on the atlas's pages, measured (Sprint 48, issue #512): what
 * drawing Venus honestly needs, through the production projections and
 * the accepted Venus service (#501), with the two candidate rules drawn
 * by this study over pages the production composition painted - the
 * Sun-Moon and Jovian cartography studies' method (#414, #482). Its
 * lit shape is the page's own: {@link ReferenceInk#litRegion}, the
 * Moon's ruled geometry (C3), turned to χ of date through the page's
 * north and east at Venus. Nothing here is production rendering; every
 * image says MOCKUP in its corner, and a magnified inset says it is a
 * study magnification, not a field the atlas offers. The #481
 * measurement (fields below 1°) is made through the projection alone:
 * the atlas's ladder refuses such a field, and no page is painted at
 * one.
 *
 * <p>Deterministic: the bundled packs, the committed named instants
 * (Espenak's rows in {@code docs/studies/venus/venus-events.txt}, the
 * representative appearances in the Horizons fixture), fixed observers
 * (Oslo at sea level, as the contract's), {@code Locale.ROOT} numbers.
 * The report goes to stdout ({@code make venus-cartography-study}); the
 * mock-ups are written beside it.
 */
public final class VenusCartographyStudyMain {

    private static final File DIR = new File("docs/studies/venus-cartography");
    private static final Path EVENTS = Path.of("docs/studies/venus/venus-events.txt");
    private static final Path APPEARANCES =
            Path.of("docs/studies/venus/horizons/appearances-oslo-299.txt");
    private static final int PAGE_W = 900;
    private static final int PAGE_H = 700;
    /** The contract's observer (#500): Oslo at sea level. */
    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-08-15T06:00:00Z"));

    /** The readability criterion the handover carries as a study measure (#500, ruling 8). */
    static final double READABLE_DISC_PX = VenusContractStudyMain.READABLE_DISC_PX;
    static final double READABLE_PART_PX = VenusContractStudyMain.READABLE_PART_PX;
    /** Candidate: Jupiter's ruled minimum mark, if Venus were given one (#482, ruling 2). */
    static final double CANDIDATE_MARK_PX = juranometria.jovianchart.JovianModule.MINIMUM_MARK_PX;
    /** Fields below the ladder measured for #481, through the projection only. */
    static final double[] CLOSER_FIELDS = {0.5, 0.25, 0.1};

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static VenusService service;
    private static SolarSystemService solar;

    /** One instant's Venus, with the page quantities derived once. */
    record Week(Instant when, VenusObservation venus) {
        double diameter() {
            return venus.solidBodyDiameterArcseconds();
        }

        double k() {
            return venus.illuminatedFraction();
        }

        /**
         * The widest field at which this week reads under the criterion,
         * degrees, with the page's scale taken as 1/field from the 1°
         * page's measured pixels per arcsecond.
         */
        double readableBelowDegrees(double pxPerArcsecAt1) {
            double d = diameter() * pxPerArcsecAt1;
            return Math.min(d / READABLE_DISC_PX,
                    Math.min(k() * d / READABLE_PART_PX, (1.0 - k()) * d / READABLE_PART_PX));
        }
    }

    /** A named instant, as the fixtures quote it. */
    record Named(String name, Instant when) {
    }

    private VenusCartographyStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        DIR.mkdirs();
        service = VenusService.load();
        solar = SolarSystemService.load();
        p("# Venus on the page: measurements and mock-ups");
        p("");
        p("Sprint 48, issue #512. What drawing Venus honestly needs, measured through the"
                + " production projections and the accepted Venus service (#501) over the"
                + " bundled packs, with the two candidate rules drawn by this study over pages"
                + " the production composition painted, as the Sun-Moon (#414) and Jovian"
                + " (#482) studies did. Venus's lit shape is the page's own `litRegion` - the"
                + " Moon's ruled geometry (C3) - turned to χ of date through the page's north"
                + " and east at Venus. Nothing here is production rendering; every image is a"
                + " mock-up and says so in its corner. The readability criterion (a disc of at"
                + " least " + fmt(READABLE_DISC_PX) + " px, its lit and dark parts each at least "
                + fmt(READABLE_PART_PX) + " px along the bright limb's axis) is the handover's"
                + " study measure, not a rendering rule (#500, ruling 8). Regenerate with `make"
                + " venus-cartography-study`.");
        p("");
        List<Week> weekly = weekly();
        List<Named> events = events();
        List<Named> appearances = appearances();
        double at1 = JovianCartographyStudyMain.pxPerArcsec(
                ChartViewState.normalMinimumFieldDegrees(), PAGE_W, PAGE_H);
        size(weekly, events, appearances, at1);
        readability(weekly, at1);
        apparition(events, at1);
        conjunctions(events, at1);
        frame(weekly);
        closer(weekly, at1);
        mockups(events, appearances);
        proposals(weekly, at1);
        System.out.print(REPORT);
        System.out.flush();
    }

    // ---- the inputs, from the service and the committed fixtures ---------

    /** Every 7 days 1900-01-01 to 2100-12-31 at Oslo, as the #500 study sampled Horizons. */
    private static List<Week> weekly() {
        List<Week> out = new ArrayList<>();
        Instant t = Instant.parse("1900-01-01T00:00:00Z");
        Instant end = Instant.parse("2100-12-31T00:00:00Z");
        while (!t.isAfter(end)) {
            out.add(new Week(t, service.observe(OSLO.at(t))));
            t = t.plus(Duration.ofDays(7));
        }
        return out;
    }

    /** Espenak's nine rows, read from the fixture, never retyped. */
    private static List<Named> events() throws IOException {
        List<Named> out = new ArrayList<>();
        for (String line : Files.readAllLines(EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            String iso = f[1].length() == 17 ? f[1].substring(0, 16) + ":00Z" : f[1];
            out.add(new Named(f[0], Instant.parse(iso)));
        }
        return out;
    }

    /** The four representative appearances at Oslo, read from the Horizons fixture's rows. */
    private static List<Named> appearances() throws IOException {
        List<Named> out = new ArrayList<>();
        boolean in = false;
        DateTimeFormatter horizons = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss.SSS", Locale.ROOT);
        for (String line : Files.readAllLines(APPEARANCES, StandardCharsets.UTF_8)) {
            if (line.startsWith("$$SOE")) {
                in = true;
                continue;
            }
            if (line.startsWith("$$EOE")) {
                break;
            }
            if (in) {
                String stamp = line.split(",")[0].strip();
                LocalDateTime when = LocalDateTime.parse(stamp, horizons);
                out.add(new Named("appearance " + stamp.substring(0, 11), when.toInstant(ZoneOffset.UTC)));
            }
        }
        return out;
    }

    private static Named named(List<Named> events, String name) {
        return events.stream().filter(e -> e.name().equals(name)).findFirst().orElseThrow();
    }

    // ---- A. size on every page -------------------------------------------------

    private static void size(List<Week> weekly, List<Named> events, List<Named> appearances,
                             double at1) {
        Week near = weekly.get(0);
        Week far = weekly.get(0);
        for (Week w : weekly) {
            if (w.diameter() > near.diameter()) {
                near = w;
            }
            if (w.diameter() < far.diameter()) {
                far = w;
            }
        }
        double minInk = 2.0 * juranometria.chart.StarSizePolicy.DEFAULT.minimumRadiusPx();
        double maxStar = 2.0 * juranometria.chart.StarSizePolicy.DEFAULT.maximumRadiusPx();
        p("## A. Size on every page");
        p("");
        p(String.format(Locale.ROOT, "Over 1900-2100 (%d weekly instants at Oslo from the"
                + " service, topocentric) Venus's solid-body diameter is %.2f″ (%s, farthest)"
                + " to %.2f″ (%s, nearest). The atlas's smallest star mark is %.1f px across and"
                + " its largest %.1f px (the production star policy); Jupiter's ruled minimum"
                + " mark is %.0f px.", weekly.size(), far.diameter(), day(far.when()),
                near.diameter(), day(near.when()), minInk, maxStar, CANDIDATE_MARK_PX));
        p("");
        int a4W = PngSheetWriter.widePixels(PaperSize.A4, PngSheetWriter.DEFAULT_RESOLUTION);
        int letterW = PngSheetWriter.widePixels(PaperSize.LETTER, PngSheetWriter.DEFAULT_RESOLUTION);
        p(String.format(Locale.ROOT, "Pixels on the %d × %d page at its centre, through each"
                + " field's own projection; millimetres on the A4 sheet's chart area (%.0f mm"
                + " wide, %d px at %d dpi) and the Letter sheet's (%.0f mm, %d px).", PAGE_W,
                PAGE_H, PaperSize.A4.chartWideMm(), a4W, PngSheetWriter.DEFAULT_RESOLUTION,
                PaperSize.LETTER.chartWideMm(), letterW));
        p("");
        p("| field | diameter px (min-max) | under the smallest star mark | A4 mm (max) | Letter mm (max) | A4 px at 300 dpi (min-max) | Letter px (max) |");
        p("|---:|---|---|---:|---:|---|---:|");
        for (double field : ChartViewState.fieldWidthSteps()) {
            double perArcsec = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            double a4Mm = PaperSize.A4.chartWideMm() / (field * 3600.0);
            double letterMm = PaperSize.LETTER.chartWideMm() / (field * 3600.0);
            double a4Px = a4W / (field * 3600.0);
            double letterPx = letterW / (field * 3600.0);
            double dMin = far.diameter() * perArcsec;
            double dMax = near.diameter() * perArcsec;
            p(String.format(Locale.ROOT, "| %s° | %.2f-%.2f | %s | %.2f | %.2f | %.1f-%.1f | %.1f |",
                    fmt(field), dMin, dMax,
                    dMax < minInk ? "always" : dMin < minInk ? "at its smallest" : "never",
                    near.diameter() * a4Mm, near.diameter() * letterMm,
                    far.diameter() * a4Px, near.diameter() * a4Px, near.diameter() * letterPx));
        }
        p("");
        p(String.format(Locale.ROOT, "- **Where the true disc reaches a size** (scale as 1/field"
                + " from the 1° page's %.3f px per arcsecond): the smallest star mark (%.1f px)"
                + " at %.1f° at its largest and %.1f° at its smallest; a shape rather than a dot"
                + " (3 px) at %.1f° and %.1f°; Jupiter's %.0f px mark at %.2f° and %.2f°; the"
                + " largest star mark (%.0f px) at %.2f° and %.2f°.", at1, minInk,
                near.diameter() * at1 / minInk, far.diameter() * at1 / minInk,
                near.diameter() * at1 / 3.0, far.diameter() * at1 / 3.0, CANDIDATE_MARK_PX,
                near.diameter() * at1 / CANDIDATE_MARK_PX, far.diameter() * at1 / CANDIDATE_MARK_PX,
                maxStar, near.diameter() * at1 / maxStar, far.diameter() * at1 / maxStar));
        p(String.format(Locale.ROOT, "- **At 1°, the ordinary minimum field, the true disc is"
                + " %.2f-%.2f px**: a shape in every week, and under Jupiter's 6 px mark in"
                + " %.1f %% of them.", far.diameter() * at1, near.diameter() * at1,
                100.0 * weekly.stream().filter(w -> w.diameter() * at1 < CANDIDATE_MARK_PX).count()
                        / weekly.size()));
        p("");
        p("### The named instants at 1°, Oslo");
        p("");
        p("| instant (UTC) | what | diameter ″ | px | lit px (k·D) | dark px | k | phase | χ of date | readable |");
        p("|---|---|---:|---:|---:|---:|---:|---|---:|---|");
        List<Named> all = new ArrayList<>(events);
        all.addAll(appearances);
        for (Named n : all) {
            VenusObservation v = service.observe(OSLO.at(n.when()));
            double dpx = v.solidBodyDiameterArcseconds() * at1;
            double k = v.illuminatedFraction();
            p(String.format(Locale.ROOT, "| %s | %s | %.2f | %.2f | %.2f | %.2f | %.3f | %s | %.1f° | %s |",
                    day(n.when()), n.name(), v.solidBodyDiameterArcseconds(), dpx, k * dpx,
                    (1.0 - k) * dpx, k, word(v), v.brightLimbAngleDegrees(),
                    readable(dpx, k) ? "yes" : "no"));
        }
        p("");
    }

    private static boolean readable(double discPx, double k) {
        return discPx >= READABLE_DISC_PX && k * discPx >= READABLE_PART_PX
                && (1.0 - k) * discPx >= READABLE_PART_PX;
    }

    private static String word(VenusObservation v) {
        return v.phase().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    // ---- B. the phase on the page --------------------------------------------

    private static void readability(List<Week> weekly, double at1) {
        p("## B. The phase on the page");
        p("");
        p("A week counts as readable when the disc is at least " + fmt(READABLE_DISC_PX)
                + " px across and its lit and dark parts, along the bright limb's axis, are"
                + " each at least " + fmt(READABLE_PART_PX) + " px (k·D and (1 − k)·D). The"
                + " share of the century's weeks that read, by field; the screen page through"
                + " each field's own projection, the sheets at " + PngSheetWriter.DEFAULT_RESOLUTION
                + " dpi.");
        p("");
        int a4W = PngSheetWriter.widePixels(PaperSize.A4, PngSheetWriter.DEFAULT_RESOLUTION);
        int letterW = PngSheetWriter.widePixels(PaperSize.LETTER, PngSheetWriter.DEFAULT_RESOLUTION);
        p("| field | screen page | A4 at 300 dpi | Letter at 300 dpi |");
        p("|---:|---:|---:|---:|");
        for (double field : ChartViewState.fieldWidthSteps()) {
            if (field > 12.0) {
                continue;
            }
            p(String.format(Locale.ROOT, "| %s° | %s | %s | %s |", fmt(field),
                    share(weekly, JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H)),
                    share(weekly, a4W / (field * 3600.0)), share(weekly, letterW / (field * 3600.0))));
        }
        p("");
        p("**By phase word, at 1° on the screen page** (the share of that word's weeks that read):");
        p("");
        p("| phase | weeks | readable at 1° | at 0.5° | at 0.25° |");
        p("|---|---:|---:|---:|---:|");
        Map<VenusService.Phase, List<Week>> bands = new EnumMap<>(VenusService.Phase.class);
        for (Week w : weekly) {
            bands.computeIfAbsent(w.venus().phase(), x -> new ArrayList<>()).add(w);
        }
        for (Map.Entry<VenusService.Phase, List<Week>> e : bands.entrySet()) {
            p(String.format(Locale.ROOT, "| %s | %d | %s | %s | %s |",
                    e.getKey().name().toLowerCase(Locale.ROOT).replace('_', ' '), e.getValue().size(),
                    share(e.getValue(), at1), share(e.getValue(), at1 * 2.0), share(e.getValue(), at1 * 4.0)));
        }
        p("");
        p("Fields below 1° are measured through the projection at 1/field of the 1° page's"
                + " scale; the atlas's ladder offers none of them (#481, section F).");
        p("");
    }

    private static String share(List<Week> weeks, double perArcsec) {
        int n = 0;
        for (Week w : weeks) {
            if (readable(w.diameter() * perArcsec, w.k())) {
                n++;
            }
        }
        return String.format(Locale.ROOT, "%.1f %%", 100.0 * n / weeks.size());
    }

    /** The 2026-27 apparition day by day, between the cited conjunctions. */
    private static void apparition(List<Named> events, double at1) {
        Instant superior = named(events, "2026-superior-conjunction").when();
        Instant inferior = named(events, "2026-inferior-conjunction").when();
        p("### The 2026-27 apparition, day by day");
        p("");
        p("From the 2026 superior conjunction (" + day(superior) + ", Espenak) through the"
                + " inferior conjunction (" + day(inferior) + ") to the end of 2027, at 00:00"
                + " UTC each day at Oslo: on which days the phase reads, at 1° and at the"
                + " candidate closer fields.");
        p("");
        p("| field | evening (east of the Sun) days readable | first | last | morning (west) days readable | first | last |");
        p("|---:|---:|---|---|---:|---|---|");
        double[] fields = {1.0, 0.5, 0.25, 0.1};
        for (double field : fields) {
            double perArcsec = at1 * (1.0 / field);
            int[] count = new int[2];
            Instant[] first = new Instant[2];
            Instant[] last = new Instant[2];
            for (Instant t = superior.plus(Duration.ofDays(1)).truncatedTo(java.time.temporal.ChronoUnit.DAYS);
                    t.isBefore(Instant.parse("2028-01-01T00:00:00Z")); t = t.plus(Duration.ofDays(1))) {
                VenusObservation v = service.observe(OSLO.at(t));
                int half = t.isBefore(inferior) ? 0 : 1;
                if (readable(v.solidBodyDiameterArcseconds() * perArcsec, v.illuminatedFraction())) {
                    count[half]++;
                    if (first[half] == null) {
                        first[half] = t;
                    }
                    last[half] = t;
                }
            }
            p(String.format(Locale.ROOT, "| %s° | %d | %s | %s | %d | %s | %s |", fmt(field),
                    count[0], first[0] == null ? "—" : day(first[0]).substring(0, 10),
                    last[0] == null ? "—" : day(last[0]).substring(0, 10),
                    count[1], first[1] == null ? "—" : day(first[1]).substring(0, 10),
                    last[1] == null ? "—" : day(last[1]).substring(0, 10)));
        }
        p("");
        p("The morning half runs only to the end of 2027 here; the apparition's own end is the"
                + " next superior conjunction, outside the cited rows.");
        p("");
    }

    // ---- C. the conjunctions ----------------------------------------------------

    private static void conjunctions(List<Named> events, double at1) {
        p("## C. At the conjunctions");
        p("");
        p("What the page would hold at the cited instants, Oslo, 1°: Venus's disc, the Sun's,"
                + " and which is nearer - the page paints the farther first, so the nearer"
                + " covers it (#414, ruling C4 as the Moon over the Sun at a new Moon).");
        p("");
        p("| instant (UTC) | event | elongation | Venus px | lit px | Sun px | nearer | state | what the page would show |");
        p("|---|---|---:|---:|---:|---:|---|---|---|");
        for (String name : List.of("2012-inferior-conjunction", "2026-superior-conjunction",
                "2026-inferior-conjunction", "2100-inferior-conjunction")) {
            Named n = named(events, name);
            VenusObservation v = service.observe(OSLO.at(n.when()));
            SolarSystemService.SunObservation sun = (SolarSystemService.SunObservation)
                    solar.observe(SolarSystemService.Body.SUN, OSLO.at(n.when()));
            double vpx = v.solidBodyDiameterArcseconds() * at1;
            double spx = sun.angularDiameterArcseconds() * at1;
            boolean overlap = v.elongationDegrees() * 3600.0
                    < v.sunAngularRadiusArcseconds() + v.solidBodyDiameterArcseconds() / 2.0;
            String shows = overlap
                    ? (v.distanceKm() < sun.distanceKm()
                            ? "Venus's dark disc inside the Sun's ring, painted over the Sun's ground-filled disc"
                            : "nothing of Venus: the Sun's ground-filled disc covers it")
                    : v.illuminatedFraction() * vpx < 0.1
                            ? "a dark disc with its limb; the lit sliver is under a tenth of a pixel"
                            : (1.0 - v.illuminatedFraction()) * vpx < 0.1
                                    ? String.format(Locale.ROOT, "a full disc of %.1f px beside the Sun's ring, %.2f° away: the published conjunction is in ecliptic longitude, and the disc clears the Sun's", vpx, v.elongationDegrees())
                                    : "a phased disc";
            p(String.format(Locale.ROOT, "| %s | %s | %.2f° | %.2f | %.3f | %.1f | %s | %s | %s |",
                    day(n.when()), name, v.elongationDegrees(), vpx, v.illuminatedFraction() * vpx,
                    spx, v.distanceKm() < sun.distanceKm() ? "Venus" : "the Sun", word(v), shows));
        }
        p("");
        p("The Sun is drawn as a ring with its centre dot, its disc filled with the ground; a"
                + " transit therefore shows as a dark disc inside the ring on either palette, and"
                + " a superior conjunction shows nothing - both without a rule of their own, by"
                + " painting order and distance. At inferior conjunction clear of the Sun the"
                + " page draws what the geometry gives: a disc whose lit part is below a pixel."
                + " What a reader who cannot see the page is told at each is a Part II question"
                + " (section G).");
        p("");
    }

    // ---- D. the frame -----------------------------------------------------------

    private static void frame(List<Week> weekly) {
        p("## D. The lit shape on the J2000 page");
        p("");
        // Venus near the page's edge - the page centred 0.45 of the field
        // east of it - at 1° and at 8°: how far the page's local north at
        // Venus turns from the page's top, every 13th week.
        double[] maxTurn = new double[2];
        Instant[] at = {weekly.get(0).when(), weekly.get(0).when()};
        double[] fields = {1.0, 8.0};
        DrawnPage page = JovianCartographyStudyMain.emptyPage(new SkyPosition(150.0, 15.0), 1.0, PAGE_W, PAGE_H);
        for (int i = 0; i < weekly.size(); i += 13) {
            Week w = weekly.get(i);
            for (int f = 0; f < fields.length; f++) {
                SkyPosition centre = JovianChartGeometry.offset(w.venus().astrometricJ2000(), 90.0, 0.45 * fields[f]);
                DrawnPage own = JovianCartographyStudyMain.emptyPage(centre, fields[f], PAGE_W, PAGE_H);
                double[] north = PageBasis.at(own, w.venus().astrometricJ2000()).orElseThrow().direction(0.0);
                double turn = Math.abs(Math.toDegrees(Math.atan2(north[0], -north[1])));
                if (turn > maxTurn[f]) {
                    maxTurn[f] = turn;
                    at[f] = w.when();
                }
            }
        }
        p(String.format(Locale.ROOT, "The lit region is the page's own (`ReferenceInk.litRegion`,"
                + " #414 ruling C3): the half disc towards the bright limb with the terminator's"
                + " half-ellipse of semi-axis r·|cos i| added when gibbous and taken away when"
                + " crescent, its area k = (1 + cos i)/2 of the disc's; turned to χ through the"
                + " page's north and east at Venus (`PageBasis`), never recomputed from J2000"
                + " inputs. At the page's centre local north is the page's top; with Venus"
                + " near the page's edge the basis turns with the projection - by up to %.2f°"
                + " on the 1° page (largest at %s) and up to %.2f° on the 8° page (at %s), every"
                + " 13th week over the century - as the Moon's does. χ is of date (#500, ruling 3); the #500 study measured the"
                + " J2000 χ up to the precession since 2000 away from it, which is why the page"
                + " rotates the of-date angle into its own basis rather than recomputing.",
                maxTurn[0], day(at[0]), maxTurn[1], day(at[1])));
        p("");
        p("Two things the Moon's geometry does not settle for Venus, for the ruling: (1) at"
                + " inferior conjunction clear of the Sun the lit sliver is under a tenth of a"
                + " pixel on the 1° page (section C) - the disc is drawn dark with its limb, as"
                + " the geometry gives, and no sliver is invented; (2) near full the dark"
                + " sliver is likewise under a pixel, and the disc reads as full.");
        p("");
        p("The projection's page rectangle is " + PAGE_W + " × " + PAGE_H + " at 1° here; the"
                + " basis is read from `" + page.projection().getClass().getSimpleName() + "`.");
        p("");
    }

    // ---- F. #481 ---------------------------------------------------------------

    private static void closer(List<Week> weekly, double at1) {
        p("## F. Fields below 1° (#481), measured");
        p("");
        double[] below = new double[weekly.size()];
        for (int i = 0; i < weekly.size(); i++) {
            below[i] = weekly.get(i).readableBelowDegrees(at1);
        }
        java.util.Arrays.sort(below);
        p("For each week the widest field at which its phase reads, from the criterion and the"
                + " 1° page's scale; the field at which a share of the century's weeks read is"
                + " that share's quantile. The atlas offers no field below "
                + fmt(ChartViewState.normalMinimumFieldDegrees()) + "°; these are what #481 would"
                + " have to offer, measured through the projection alone.");
        p("");
        p("| share of weeks whose phase reads | needs a field of at most |");
        p("|---:|---:|");
        for (double q : new double[] {0.25, 0.5, 0.75, 0.9, 0.95}) {
            int i = (int) Math.floor((1.0 - q) * (below.length - 1));
            p(String.format(Locale.ROOT, "| %.0f %% | %.2f° |", 100.0 * q, below[i]));
        }
        p("");
        p("| candidate field | screen page: weeks readable | Venus px (min-max) | the 2026 greatest elongation's disc px |");
        p("|---:|---:|---|---:|");
        double dMin = weekly.stream().mapToDouble(Week::diameter).min().orElseThrow();
        double dMax = weekly.stream().mapToDouble(Week::diameter).max().orElseThrow();
        VenusObservation elong = service.observe(OSLO.at(Instant.parse("2026-08-15T06:00:00Z")));
        for (double field : new double[] {1.0, 0.5, 0.25, 0.1}) {
            double perArcsec = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            p(String.format(Locale.ROOT, "| %s° | %s | %.1f-%.1f | %.1f |", fmt(field),
                    share(weekly, perArcsec), dMin * perArcsec, dMax * perArcsec,
                    elong.solidBodyDiameterArcseconds() * perArcsec));
        }
        p("");
        p("Measured through the projection, half the century's weeks read on screen at about"
                + " 0.5°, two thirds at 0.25° and three quarters at 0.1°; the rest are the gibbous"
                + " and near-full weeks whose dark part is under a pixel at any of these fields -"
                + " by the criterion's construction, a full disc never \"reads\" as a phase."
                + " Jupiter's record (#482) measured its oblateness resolved only below 1° on"
                + " screen and on A4 from 2.9°, and ruled no close zoom then; Mercury and Saturn"
                + " have no service and are not measured here - their needs are each planet's"
                + " own study's. What projection, labels, export, keyboard zoom, persistence and"
                + " evidence would have to prove below 1° is listed in section G for the ruling,"
                + " not decided.");
        p("");
    }

    // ---- E. mock-ups --------------------------------------------------------------

    private static void mockups(List<Named> events, List<Named> appearances) throws Exception {
        p("## E. Mock-ups on the atlas's own pages");
        p("");
        p("Each page is painted by the production composition (`ChartComponent` over the"
                + " bundled catalogue). Over it this study draws, from the service's J2000 place"
                + " for the instant and observer stated: **rule T**, Venus at its true solid-body"
                + " diameter, phased by the page's own lit region - the Sun's and the Moon's"
                + " rule (C1); or **rule M**, the larger of the true disc and a " + fmt(CANDIDATE_MARK_PX)
                + " px minimum mark phased the same way - Jupiter's rule (#482, ruling 2) with the"
                + " phase kept. The Sun, where it is on the page, as its ring. Where the true"
                + " disc is a few pixels across an inset magnifies it - a **study magnification,"
                + " not a field the atlas offers**. Captions name the rule drawn.");
        p("");
        Named elongation = named(events, "2026-greatest-elongation-east");
        // 1. The field series at the greatest eastern elongation, rule T; rule M beside it at 3°.
        for (double field : new double[] {8.0, 3.0, 1.0}) {
            page(String.format(Locale.ROOT, "elongation-true-%s", fmt(field)),
                    String.format(Locale.ROOT, "Rule T at the 2026 greatest eastern elongation, %s UTC, Oslo, %s° field",
                            day(elongation.when()), fmt(field)),
                    elongation.when(), field, field == 8.0 ? NORSK : ENGLISH,
                    field == 3.0 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    false, field == 1.0 ? 16 : 40,
                    "Venus near half lit, drawn at true scale; the inset shows the phase the page cannot.");
        }
        page("elongation-mark-3", "Rule M at the same instant, 3° field",
                elongation.when(), 3.0, ENGLISH, ChartPalette.WHITE_PAPER, true, 40,
                "The " + fmt(CANDIDATE_MARK_PX) + " px minimum mark stands for the true disc, phased; a reader sees a shape that is not Venus's apparent size.");
        page("elongation-mark-8", "Rule M at the same instant, 8° field",
                elongation.when(), 8.0, NORSK, ChartPalette.BLACK_SKY, true, 0,
                "At 8° the true disc is under a pixel; rule M draws the mark, rule T a dot the size of a faint star.");
        // 2. The four representative appearances at 1°, rule T.
        int i = 1;
        for (Named a : appearances) {
            VenusObservation v = service.observe(OSLO.at(a.when()));
            page("appearance-" + i, "Rule T, " + a.name() + " (" + word(v) + "), " + day(a.when()) + " UTC, Oslo, 1° field",
                    a.when(), 1.0, i % 2 == 0 ? NORSK : ENGLISH,
                    i > 2 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER, false, 16,
                    String.format(Locale.ROOT, "k = %.2f, χ = %.1f°; lit %.1f px of %.1f px.",
                            v.illuminatedFraction(), v.brightLimbAngleDegrees(),
                            v.illuminatedFraction() * v.solidBodyDiameterArcseconds()
                                    * JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H),
                            v.solidBodyDiameterArcseconds()
                                    * JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H)));
            i++;
        }
        // 3. The conjunctions at 1°.
        page("inferior-conjunction-1", "Rule T at the 2026 inferior conjunction, " + day(named(events, "2026-inferior-conjunction").when()) + " UTC, Oslo, 1° field",
                named(events, "2026-inferior-conjunction").when(), 1.0, ENGLISH, ChartPalette.WHITE_PAPER, false, 16,
                "The largest disc of the apparition, its lit sliver under a tenth of a pixel: drawn dark with its limb, as the geometry gives.");
        page("transit-1", "Rule T at the 2012 transit, " + day(named(events, "2012-inferior-conjunction").when()) + " UTC, Oslo, 1° field",
                named(events, "2012-inferior-conjunction").when(), 1.0, NORSK, ChartPalette.WHITE_PAPER, false, 16,
                "In front of the Sun: the nearer body painted last, Venus's dark disc inside the Sun's ring.");
        Named superior = named(events, "2026-superior-conjunction");
        VenusObservation atSuperior = service.observe(OSLO.at(superior.when()));
        page("superior-conjunction-1", "Rule T at the 2026 superior conjunction, " + day(superior.when()) + " UTC, Oslo, 1° field",
                superior.when(), 1.0, ENGLISH, ChartPalette.BLACK_SKY, false, 16,
                atSuperior.phase() == VenusService.Phase.BEHIND_SUN
                        ? "Behind the Sun: Venus painted first, the Sun's ground-filled disc over it - nothing of Venus shows, and the inset shows what is covered."
                        : String.format(Locale.ROOT, "At the published hour Venus is %.2f° from the Sun - the conjunction is in ecliptic longitude, and the disc clears the Sun's - so the page shows a full disc beside the Sun's ring (k = %.2f); only inside the ring would the Sun's ground-filled disc cover it.",
                                atSuperior.elongationDegrees(), atSuperior.illuminatedFraction()));
        // 4. The closer fields, as a study strip: not pages.
        strip(elongation, appearances);
        // 5. Printable: the A4 sheet's chart area at 150 dpi, 1°.
        int sheetW = PngSheetWriter.widePixels(PaperSize.A4, 150);
        int sheetH = (int) Math.round(PaperSize.A4.chartHighPoints() * 150 / 72.0);
        page("printable-a4-1", String.format(Locale.ROOT, "Rule T on the A4 sheet's chart area at 150 dpi (%d × %d px), 1° field", sheetW, sheetH),
                elongation.when(), 1.0, ENGLISH, ChartPalette.WHITE_PAPER, false, 0, sheetW, sheetH,
                "On paper the phase reads at 1° without magnification.");
    }

    /**
     * A page painted by the production composition, Venus (and the
     * Sun, if on the page) drawn over it by this study at its field,
     * with an optional magnified inset.
     */
    private static void page(String name, String title, Instant when, double field, PageText words,
                             ChartPalette palette, boolean mark, int inset, String caption)
            throws Exception {
        page(name, title, when, field, words, palette, mark, inset, PAGE_W, PAGE_H, caption);
    }

    private static void page(String name, String title, Instant when, double field, PageText words,
                             ChartPalette palette, boolean mark, int inset, int w, int h,
                             String caption) throws Exception {
        Observer observer = OSLO.at(when);
        VenusObservation venus = service.observe(observer);
        SolarSystemService.SunObservation sun = (SolarSystemService.SunObservation)
                solar.observe(SolarSystemService.Body.SUN, observer);
        ChartViewState state = new ChartViewState(venus.astrometricJ2000(), field, 8.0, null, null);
        ChartComponent[] holder = new ChartComponent[1];
        ChartOptions options = ChartOptions.DEFAULTS.withPalette(palette);
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(w, h);
            holder[0].setChartOptions(options);
            holder[0].setViewState(state);
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
            drawBodies(g, drawnPage, projection, mapping, venus, sun, palette, mark, 1.0, 0, 0, note);
            if (inset > 0) {
                PixelPoint c = mapping.toPixel(projection.project(venus.astrometricJ2000()).orElseThrow());
                int box = 300;
                int bx = w - box - 12;
                int by = 12;
                g.setColor(dark ? new Color(10, 10, 20) : new Color(252, 252, 248));
                g.fillRect(bx, by, box, box);
                g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                g.drawRect(bx, by, box, box);
                Graphics2D gi = (Graphics2D) g.create();
                gi.clipRect(bx + 1, by + 1, box - 1, box - 1);
                drawBodies(gi, drawnPage, projection, mapping, venus, sun, palette, mark, inset,
                        bx + box / 2.0 - c.x() * inset, by + box / 2.0 - c.y() * inset, new StringBuilder());
                gi.dispose();
                g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                g.setFont(g.getFont().deriveFont(10f));
                g.drawString(String.format(Locale.ROOT, "study magnification ×%d - not a field the atlas offers", inset),
                        bx + 6, by + box - 6);
            }
            g.setColor(dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
            g.drawString("MOCKUP - study ink over a production page (#512)", 8, h - 8);
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
     * Venus and the Sun on the page, farthest first; Venus by rule T or
     * rule M, phased by the page's own lit region; the Sun as its ring
     * with its disc filled with the ground (C2).
     */
    private static void drawBodies(Graphics2D g, DrawnPage page, Projection projection,
                                   ViewportMapping mapping, VenusObservation venus,
                                   SolarSystemService.SunObservation sun, ChartPalette palette,
                                   boolean mark, double zoom, double dx, double dy, StringBuilder note) {
        boolean venusNearer = venus.distanceKm() < sun.distanceKm();
        if (!venusNearer) {
            drawVenus(g, page, projection, mapping, venus, palette, mark, zoom, dx, dy, note);
        }
        drawSun(g, projection, mapping, sun, palette, zoom, dx, dy, note);
        if (venusNearer) {
            drawVenus(g, page, projection, mapping, venus, palette, mark, zoom, dx, dy, note);
        }
    }

    private static void drawSun(Graphics2D g, Projection projection, ViewportMapping mapping,
                                SolarSystemService.SunObservation sun, ChartPalette palette,
                                double zoom, double dx, double dy, StringBuilder note) {
        var projected = projection.project(sun.astrometricJ2000());
        if (projected.isEmpty()) {
            return;
        }
        PixelPoint c = mapping.toPixel(projected.get());
        double r = radiusPx(projection, mapping, sun.astrometricJ2000(),
                sun.angularDiameterArcseconds() / 7200.0) * zoom;
        double x = c.x() * zoom + dx;
        double y = c.y() * zoom + dy;
        if (x + r < 0 || y + r < 0 || x - r > PAGE_W * Math.max(1.0, zoom) + Math.abs(dx)
                || y - r > PAGE_H * Math.max(1.0, zoom) + Math.abs(dy)) {
            return;
        }
        Ellipse2D disc = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
        g.setColor(palette.ground());
        g.fill(disc);
        g.setColor(palette.starInk());
        g.setStroke(new BasicStroke(1.0f));
        g.draw(disc);
        g.fill(new Ellipse2D.Double(x - 1.0, y - 1.0, 2.0, 2.0));
        note.append(String.format(Locale.ROOT, "the Sun's ring, %.1f px; ", 2 * r / zoom));
    }

    private static void drawVenus(Graphics2D g, DrawnPage page, Projection projection,
                                  ViewportMapping mapping, VenusObservation venus,
                                  ChartPalette palette, boolean mark, double zoom, double dx,
                                  double dy, StringBuilder note) {
        var projected = projection.project(venus.astrometricJ2000());
        if (projected.isEmpty()) {
            return;
        }
        PixelPoint c = mapping.toPixel(projected.get());
        double truePx = 2.0 * radiusPx(projection, mapping, venus.astrometricJ2000(),
                venus.solidBodyDiameterArcseconds() / 7200.0);
        double drawnPx = mark ? Math.max(truePx, CANDIDATE_MARK_PX) : truePx;
        double r = drawnPx / 2.0 * zoom;
        PixelPoint at = new PixelPoint(c.x() * zoom + dx, c.y() * zoom + dy);
        Ellipse2D disc = new Ellipse2D.Double(at.x() - r, at.y() - r, 2 * r, 2 * r);
        Color lighter = brightness(palette.ground()) >= brightness(palette.starInk())
                ? palette.ground() : palette.starInk();
        Color darker = lighter == palette.ground() ? palette.starInk() : palette.ground();
        Color darkSide = mix(darker, lighter, ReferenceInk.darkSide());
        g.setColor(darkSide);
        g.fill(disc);
        PageBasis basis = PageBasis.at(page, venus.astrometricJ2000()).orElse(null);
        if (basis != null) {
            g.setColor(lighter);
            g.fill(ReferenceInk.litRegion(at, r, basis, new OverlayContribution.Lit(
                    venus.illuminatedFraction(), venus.brightLimbAngleDegrees(),
                    venus.phaseAngleDegrees())));
        }
        g.setColor(palette.starInk());
        g.setStroke(new BasicStroke(r >= 4.0 ? 1.0f : 0.75f));
        g.draw(disc);
        note.append(String.format(Locale.ROOT, "Venus %s, true disc %.2f px, drawn %.2f px, k = %.2f, χ = %.1f°; ",
                mark ? "by rule M" : "by rule T", truePx, drawnPx, venus.illuminatedFraction(),
                venus.brightLimbAngleDegrees()));
    }

    /** A disc's page radius at a place, through the projection: half the diameter offset north. */
    private static double radiusPx(Projection projection, ViewportMapping mapping, SkyPosition at,
                                   double radiusDegrees) {
        PixelPoint a = mapping.toPixel(projection.project(at).orElseThrow());
        PixelPoint b = mapping.toPixel(projection.project(
                JovianChartGeometry.offset(at, 0.0, radiusDegrees)).orElseThrow());
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    /**
     * The closer fields as a strip, not a page: Venus at the 2026
     * elongation and at the crescent appearance, at the scale each
     * candidate field's projection would give, side by side - a study
     * figure of what #481 would show, drawn by the same lit region.
     */
    private static void strip(Named elongation, List<Named> appearances) throws Exception {
        double[] fields = {1.0, 0.5, 0.25, 0.1};
        List<Named> rows = new ArrayList<>();
        rows.add(elongation);
        Named crescent = null;
        for (Named a : appearances) {
            if (service.observe(OSLO.at(a.when())).phase() == VenusService.Phase.CRESCENT) {
                crescent = a;
            }
        }
        if (crescent != null) {
            rows.add(crescent);
        }
        int cell = 180;
        int w = cell * fields.length + 120;
        int h = cell * rows.size() + 30;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(ChartPalette.WHITE_PAPER.ground());
            g.fillRect(0, 0, w, h);
            g.setColor(Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(11f));
            for (int r = 0; r < rows.size(); r++) {
                Named n = rows.get(r);
                VenusObservation v = service.observe(OSLO.at(n.when()));
                DrawnPage page = JovianCartographyStudyMain.emptyPage(v.astrometricJ2000(), 1.0, PAGE_W, PAGE_H);
                PageBasis basis = PageBasis.at(page, v.astrometricJ2000()).orElseThrow();
                g.setColor(Color.DARK_GRAY);
                g.drawString(day(n.when()).substring(0, 10), 8, r * cell + cell / 2);
                for (int f = 0; f < fields.length; f++) {
                    double perArcsec = JovianCartographyStudyMain.pxPerArcsec(fields[f], PAGE_W, PAGE_H);
                    double rad = v.solidBodyDiameterArcseconds() * perArcsec / 2.0;
                    PixelPoint at = new PixelPoint(120 + f * cell + cell / 2.0, r * cell + cell / 2.0);
                    Ellipse2D disc = new Ellipse2D.Double(at.x() - rad, at.y() - rad, 2 * rad, 2 * rad);
                    g.setColor(mix(ChartPalette.WHITE_PAPER.starInk(), ChartPalette.WHITE_PAPER.ground(),
                            ReferenceInk.darkSide()));
                    g.fill(disc);
                    g.setColor(ChartPalette.WHITE_PAPER.ground());
                    g.fill(ReferenceInk.litRegion(at, rad, basis, new OverlayContribution.Lit(
                            v.illuminatedFraction(), v.brightLimbAngleDegrees(), v.phaseAngleDegrees())));
                    g.setColor(ChartPalette.WHITE_PAPER.starInk());
                    g.setStroke(new BasicStroke(1.0f));
                    g.draw(disc);
                    g.setColor(Color.DARK_GRAY);
                    g.drawString(String.format(Locale.ROOT, "%s°: %.1f px%s", fmt(fields[f]), 2 * rad,
                            fields[f] < ChartViewState.normalMinimumFieldDegrees() ? " (not offered)" : ""),
                            120 + f * cell + 8, r * cell + 16);
                }
            }
            g.setColor(Color.DARK_GRAY);
            g.drawString("MOCKUP - Venus at the scale each field's projection gives, the page's own lit region; fields below 1° are not offered (#481, #512)", 8, h - 8);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, "closer-fields.png"));
        p("### The closer fields, as a strip");
        p("");
        p("![](closer-fields.png)");
        p("");
        p("The greatest eastern elongation" + (crescent == null ? "" : " and the crescent appearance")
                + " at the scale of each candidate field, through its projection; not a page the"
                + " atlas offers below 1°.");
        p("");
    }

    // ---- G. proposals ------------------------------------------------------------

    private static void proposals(List<Week> weekly, double at1) {
        p("## G. For the rulings");
        p("");
        p("1. **Size: rule T (true scale, always) or rule M (the larger of true scale and a "
                + fmt(CANDIDATE_MARK_PX) + " px mark).** At 1° the true disc is never under 2 px;"
                + " at 3° it is "
                + String.format(Locale.ROOT, "%.1f-%.1f", weekly.stream().mapToDouble(Week::diameter).min().orElseThrow()
                        * JovianCartographyStudyMain.pxPerArcsec(3.0, PAGE_W, PAGE_H),
                        weekly.stream().mapToDouble(Week::diameter).max().orElseThrow()
                        * JovianCartographyStudyMain.pxPerArcsec(3.0, PAGE_W, PAGE_H))
                + " px, at 8° under a pixel - a dot the size of a faint star, as the Sun and"
                + " Moon never are (C1) and Jupiter is (ruling 2 on #482); at 1° the true disc"
                + " is under the 6 px mark in three quarters of the weeks. The study draws both;"
                + " it recommends **rule M**, Jupiter's, with the mark phased: Venus is the"
                + " second body to vanish at true scale on most pages, and a mark that keeps the"
                + " phase tells the truth the table tells.");
        p("2. **At the conjunctions**: painting order and distance already give the transit"
                + " (a dark disc in the Sun's ring) and the superior conjunction (covered); the"
                + " dark disc near inferior conjunction is what the geometry gives. The study"
                + " proposes no sliver be invented, and asks what the spoken description says"
                + " at each.");
        p("3. **#481**: on screen the phase reads in a fifth of the century's weeks at 1°, half"
                + " of them at about 0.5° and two thirds at 0.25° (section F); on A4 at 1° in"
                + " two thirds."
                + " Venus can be drawn honestly at the ordinary floor under either rule; closer"
                + " zoom is what makes its phase readable at most dates. The study asks the"
                + " owner to rule whether 5.2.0 draws Venus at the ordinary ladder alone, or"
                + " opens an object-dependent closer field; if the latter, which fields, the"
                + " qualifying-object rule, the navigation back to the ordinary floor, and the"
                + " proofs due first - projection, labels, export, keyboard zoom, persistence and"
                + " evidence below 1°.");
        p("4. **The readability criterion** stays a study measure unless ruled otherwise.");
        p("");
    }

    // ---- helpers -------------------------------------------------------------------

    private static String day(Instant t) {
        return t.toString().replace("T", " ").replace(":00Z", "");
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.format(Locale.ROOT, "%.0f", v)
                : String.format(Locale.ROOT, "%s", v);
    }

    private static double brightness(Color c) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private static Color mix(Color a, Color b, double share) {
        return new Color(
                (int) Math.round(a.getRed() * (1 - share) + b.getRed() * share),
                (int) Math.round(a.getGreen() * (1 - share) + b.getGreen() * share),
                (int) Math.round(a.getBlue() * (1 - share) + b.getBlue() * share));
    }
}
