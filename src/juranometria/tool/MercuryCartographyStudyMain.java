package juranometria.tool;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
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
import java.util.Set;
import javax.imageio.ImageIO;
import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.PageBasis;
import juranometria.project.PageWords;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.ChartStructure;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;
import juranometria.sky.Observer;
import juranometria.solar.MercuryService;
import juranometria.solar.MercuryService.MercuryObservation;
import juranometria.solar.SolarSystemService;
import juranometria.solar.VenusService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Mercury on the atlas's pages, measured, and the closer fields of #481
 * drawn (Sprint 50, issue #539): what drawing Mercury honestly needs,
 * through the production projections, the production renderer and the
 * production bodies layer, with Mercury offered to that layer by this
 * study as a {@link OverlayContribution.Body} built from the accepted
 * Mercury service exactly as the Solar System module builds Venus's -
 * under rule T (no minimum mark, the Sun's and the Moon's C1) and rule M
 * (Venus's phased 6 px mark, #512 ruling 1). No production Mercury layer
 * exists; the page's words for Mercury are this study's proposal,
 * supplied around the production words and named in the report.
 *
 * <p>The closer fields are real pages: a viewport below the ladder is a
 * thing a measurement may ask for ({@link ChartViewport}), so the 0.5°
 * and 0.25° candidates are painted by the same renderer over the same
 * catalogue the 1° page queried. The ladder itself is untouched -
 * {@link ChartViewState} still refuses any field below 1°.
 *
 * <p>Deterministic: the bundled packs, the committed named instants
 * (Espenak's rows in {@code docs/studies/mercury/mercury-events.txt},
 * the four appearances in the Horizons fixture), Oslo at sea level,
 * {@code Locale.ROOT} numbers. The portable report goes to stdout
 * ({@code make mercury-cartography-study}); the font-dependent
 * placements to {@code platform.md}; every page is written beside them
 * and says STUDY in its corner.
 */
public final class MercuryCartographyStudyMain {

    private static final File DIR = new File("docs/studies/mercury-cartography");
    private static final Path EVENTS = Path.of("docs/studies/mercury/mercury-events.txt");
    private static final Path APPEARANCES =
            Path.of("docs/studies/mercury/horizons/appearances-oslo-199.txt");
    private static final Path VENUS_EVENTS = Path.of("docs/studies/venus/venus-events.txt");
    static final int PAGE_W = 900;
    static final int PAGE_H = 700;
    /** The contract's observer (#528): Oslo at sea level. */
    static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-02-19T18:00:00Z"));
    /** The readability criterion the handovers carry as a study measure (#500, #528, ruling 8). */
    static final double READABLE_DISC_PX = VenusContractStudyMain.READABLE_DISC_PX;
    static final double READABLE_PART_PX = VenusContractStudyMain.READABLE_PART_PX;
    /** Rule M's mark: Venus's ruled 6 px (#512, ruling 1). */
    static final double MARK_PX = SolarSystemModule.VENUS_MINIMUM_MARK_PX;
    /** The candidate closer fields #539 asks to be drawn. */
    static final double[] CLOSER_FIELDS = {0.5, 0.25};
    /** The proposed page identity and state keys (no production key exists). */
    static final String MERCURY = "mercury";
    static final String IN_FRONT = "mercury.inFront";
    static final String BEHIND = "mercury.behind";

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();
    private static MercuryService mercury;
    private static VenusService venus;
    private static SolarSystemService solar;

    /** One instant's Mercury. */
    record Week(Instant when, MercuryObservation m) {
        double diameter() {
            return m.solidBodyDiameterArcseconds();
        }

        double k() {
            return m.illuminatedFraction();
        }

        double readableBelowDegrees(double pxPerArcsecAt1) {
            double d = diameter() * pxPerArcsecAt1;
            return Math.min(d / READABLE_DISC_PX,
                    Math.min(k() * d / READABLE_PART_PX, (1.0 - k()) * d / READABLE_PART_PX));
        }
    }

    /** A named instant, as the fixtures quote it. */
    record Named(String name, Instant when) {
    }

    /** What the bodies layer did with Mercury on one painted page. */
    record Drawn(double discPx, boolean symbol, boolean named, String state,
                 PixelPoint centre, Rectangle2D box, String spoken) {
    }

    private MercuryCartographyStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    private static void q(String line) {
        PLATFORM.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        PLATFORM.setLength(0);
        DIR.mkdirs();
        mercury = MercuryService.load();
        venus = VenusService.load();
        solar = SolarSystemService.load();
        PlatformEvidence.preface(PLATFORM,
                "Mercury's names on the study pages, as this desktop's fonts place them",
                "Sprint 50, issue #539.");
        p("# Mercury on the page, and the closer fields: measurements and study pages");
        p("");
        p("Sprint 50, issue #539. What drawing Mercury honestly needs, measured through the"
                + " production projections, the production renderer and the production bodies"
                + " layer, with Mercury offered to that layer by this study as a body built"
                + " from the accepted Mercury service (#529) exactly as the Solar System module"
                + " builds Venus's: its solid-body disc from the equatorial 2440.53 km, phased by"
                + " k, χ of date and i, under **rule T** (true scale always, the Sun's and the"
                + " Moon's C1) or **rule M** (the larger of the true disc and Venus's phased "
                + fmt(MARK_PX) + " px mark, #512 ruling 1). No production Mercury layer exists:"
                + " the page's words for Mercury (*Mercury* / *Merkur*, its symbol note and its"
                + " two states) are this study's proposal, supplied around the production words."
                + " The closer fields of #481 are real pages - the same renderer over the same"
                + " catalogue the 1° page queried, through viewports the ladder does not offer;"
                + " the ladder is untouched. The readability criterion (a disc of at least "
                + fmt(READABLE_DISC_PX) + " px, its lit and dark parts each at least "
                + fmt(READABLE_PART_PX) + " px along the bright limb's axis) is the handovers'"
                + " study measure, not a rendering rule (#528, ruling 8). Every page says STUDY"
                + " in its corner. Regenerate with `make mercury-cartography-study`.");
        p("");
        List<Week> weekly = weekly();
        List<Named> events = events();
        List<Named> appearances = appearances();
        double at1 = JovianCartographyStudyMain.pxPerArcsec(
                ChartViewState.normalMinimumFieldDegrees(), PAGE_W, PAGE_H);
        size(weekly, appearances, at1);
        readability(weekly, events, at1);
        rules(appearances, events);
        conjunctions(events);
        frame(weekly);
        labels(events, appearances);
        paper(appearances);
        closer(weekly, appearances, at1);
        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        System.out.flush();
        PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
    }

    // ---- the inputs ------------------------------------------------------------

    /** Every 7 days 1900-01-01 to 2100-12-31 at Oslo, as the #528 study sampled Horizons. */
    private static List<Week> weekly() {
        List<Week> out = new ArrayList<>();
        Instant t = Instant.parse("1900-01-01T00:00:00Z");
        Instant end = Instant.parse("2100-12-31T00:00:00Z");
        while (!t.isAfter(end)) {
            out.add(new Week(t, mercury.observe(OSLO.at(t))));
            t = t.plus(Duration.ofDays(7));
        }
        return out;
    }

    private static List<Named> readEvents(Path file) throws IOException {
        List<Named> out = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            String iso = f[1].length() == 17 ? f[1].substring(0, 16) + ":00Z" : f[1];
            out.add(new Named(f[0], Instant.parse(iso)));
        }
        return out;
    }

    /** Espenak's almanac rows and the NASA/GSFC transits, read from the fixture, never retyped. */
    private static List<Named> events() throws IOException {
        return readEvents(EVENTS);
    }

    /** The four representative 2026 appearances at Oslo, read from the Horizons fixture. */
    private static List<Named> appearances() throws IOException {
        List<Named> out = new ArrayList<>();
        boolean in = false;
        DateTimeFormatter horizons = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss.SSS",
                Locale.ROOT);
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
                out.add(new Named("appearance " + stamp.substring(0, 11),
                        LocalDateTime.parse(stamp, horizons).toInstant(ZoneOffset.UTC)));
            }
        }
        return out;
    }

    private static Named named(List<Named> events, String name) {
        return events.stream().filter(e -> e.name().equals(name)).findFirst().orElseThrow();
    }

    // ---- A. size ----------------------------------------------------------------

    private static void size(List<Week> weekly, List<Named> appearances, double at1) {
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
        double minInk = 2.0 * StarSizePolicy.DEFAULT.minimumRadiusPx();
        p("## A. Size on every page and sheet");
        p("");
        p(String.format(Locale.ROOT, "Over 1900-2100 (%d weekly instants at Oslo from the service,"
                + " topocentric) Mercury's solid-body diameter is %.2f″ (%s) to %.2f″ (%s). The"
                + " atlas's smallest star mark is %.1f px across; rule M's mark %.0f px.",
                weekly.size(), far.diameter(), day(far.when()), near.diameter(),
                day(near.when()), minInk, MARK_PX));
        p("");
        int a4W = chartPx(PaperSize.A4);
        int letterW = chartPx(PaperSize.LETTER);
        p(String.format(Locale.ROOT, "Pixels on the %d × %d page at its centre through each field's"
                + " own projection - the ladder's fields, then the two candidates below it -"
                + " and on the A4 (%.0f mm, %d px at %d dpi) and Letter (%.0f mm, %d px) sheets'"
                + " chart areas. The paper columns use the chart area's width; the #512 Venus"
                + " study used the whole sheet's (%d px for A4), which overstates its paper"
                + " pixels by %.0f %% - a slip in that report, not in anything drawn.", PAGE_W,
                PAGE_H, PaperSize.A4.chartWideMm(), a4W, PngSheetWriter.DEFAULT_RESOLUTION,
                PaperSize.LETTER.chartWideMm(), letterW,
                PngSheetWriter.widePixels(PaperSize.A4, PngSheetWriter.DEFAULT_RESOLUTION),
                100.0 * (PngSheetWriter.widePixels(PaperSize.A4, PngSheetWriter.DEFAULT_RESOLUTION)
                        - a4W) / a4W));
        p("");
        p("| field | diameter px (min-max) | under the smallest star mark | rule M draws | A4 mm (max) | A4 px at 300 dpi (min-max) | Letter px (min-max) |");
        p("|---:|---|---|---|---:|---|---|");
        List<Double> fields = new ArrayList<>();
        for (double f : ChartViewState.fieldWidthSteps()) {
            fields.add(f);
        }
        for (double f : CLOSER_FIELDS) {
            fields.add(f);
        }
        for (double field : fields) {
            double perArcsec = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            double a4Px = a4W / (field * 3600.0);
            double letterPx = letterW / (field * 3600.0);
            double dMin = far.diameter() * perArcsec;
            double dMax = near.diameter() * perArcsec;
            String m = dMax < MARK_PX ? "the mark, always" : dMin < MARK_PX
                    ? "the mark, or the disc when near" : "the true disc, always";
            p(String.format(Locale.ROOT, "| %s°%s | %.2f-%.2f | %s | %s | %.2f | %.1f-%.1f | %.1f-%.1f |",
                    fmt(field), field < ChartViewState.normalMinimumFieldDegrees()
                            ? " (candidate)" : "", dMin, dMax,
                    dMax < minInk ? "always" : dMin < minInk ? "at its smallest" : "never", m,
                    near.diameter() * PaperSize.A4.chartWideMm() / (field * 3600.0),
                    far.diameter() * a4Px, near.diameter() * a4Px,
                    far.diameter() * letterPx, near.diameter() * letterPx));
        }
        p("");
        p(String.format(Locale.ROOT, "- The true disc reaches rule M's %.0f px mark at %.2f° at its"
                + " largest and %.2f° at its smallest: **below the ordinary 1° floor in every"
                + " week**. On the 1° page it is %.2f-%.2f px, under the mark in %.1f %% of the"
                + " weeks.", MARK_PX, near.diameter() * at1 / MARK_PX,
                far.diameter() * at1 / MARK_PX, far.diameter() * at1, near.diameter() * at1,
                100.0 * weekly.stream().filter(w -> w.diameter() * at1 < MARK_PX).count()
                        / weekly.size()));
        p("");
        p("The four representative 2026 appearances (Horizons' weekly Oslo rows, #528):");
        p("");
        p("| instant (UTC) | k | phase | diameter ″ | 1° px | 0.5° px | 0.25° px |");
        p("|---|---:|---|---:|---:|---:|---:|");
        for (Named n : appearances) {
            MercuryObservation m = mercury.observe(OSLO.at(n.when()));
            double d = m.solidBodyDiameterArcseconds();
            p(String.format(Locale.ROOT, "| %s | %.3f | %s | %.2f | %.2f | %.2f | %.2f |",
                    day(n.when()), m.illuminatedFraction(), word(m), d, d * at1, d * at1 * 2.0,
                    d * at1 * 4.0));
        }
        p("");
    }

    // ---- B. the phase -------------------------------------------------------------

    private static boolean readable(double discPx, double k) {
        return discPx >= READABLE_DISC_PX && k * discPx >= READABLE_PART_PX
                && (1.0 - k) * discPx >= READABLE_PART_PX;
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

    private static void readability(List<Week> weekly, List<Named> events, double at1) throws IOException {
        p("## B. When the phase can be read");
        p("");
        int a4W = chartPx(PaperSize.A4);
        p("The share of the century's weeks whose phase reads by the criterion, on the screen"
                + " page through each field's own projection and on the A4 sheet at "
                + PngSheetWriter.DEFAULT_RESOLUTION + " dpi; Venus's from the same criterion and"
                + " the same projections, for #481's comparison.");
        p("");
        List<VenusService.VenusObservation> venusWeeks = new ArrayList<>();
        for (Week w : weekly) {
            venusWeeks.add(venus.observe(OSLO.at(w.when())));
        }
        p("| field | Mercury, screen | Mercury, A4 | Venus, screen |");
        p("|---:|---:|---:|---:|");
        double[] fields = {3.0, 2.0, 1.0, 0.5, 0.25, 0.1};
        for (double field : fields) {
            double perArcsec = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            int v = 0;
            for (VenusService.VenusObservation o : venusWeeks) {
                if (readable(o.solidBodyDiameterArcseconds() * perArcsec, o.illuminatedFraction())) {
                    v++;
                }
            }
            p(String.format(Locale.ROOT, "| %s°%s | %s | %s | %.1f %% |", fmt(field),
                    field < 1.0 ? " (candidate)" : "", share(weekly, perArcsec),
                    field >= 1.0 ? share(weekly, a4W / (field * 3600.0)) : "-",
                    100.0 * v / venusWeeks.size()));
        }
        p("");
        p("**By phase word** (the share of that word's weeks that read):");
        p("");
        p("| phase | weeks | at 1° | at 0.5° | at 0.25° | at 0.1° |");
        p("|---|---:|---:|---:|---:|---:|");
        Map<MercuryService.Phase, List<Week>> bands = new EnumMap<>(MercuryService.Phase.class);
        for (Week w : weekly) {
            bands.computeIfAbsent(w.m().phase(), x -> new ArrayList<>()).add(w);
        }
        for (Map.Entry<MercuryService.Phase, List<Week>> e : bands.entrySet()) {
            p(String.format(Locale.ROOT, "| %s | %d | %s | %s | %s | %s |",
                    e.getKey().name().toLowerCase(Locale.ROOT).replace('_', ' '),
                    e.getValue().size(), share(e.getValue(), at1), share(e.getValue(), at1 * 2.0),
                    share(e.getValue(), at1 * 4.0), share(e.getValue(), at1 * 10.0)));
        }
        p("");
        double[] below = new double[weekly.size()];
        for (int i = 0; i < weekly.size(); i++) {
            below[i] = weekly.get(i).readableBelowDegrees(at1);
        }
        java.util.Arrays.sort(below);
        p(String.format(Locale.ROOT, "For each week the widest field at which its phase reads:"
                + " a quarter of the weeks read once the field is %.2f° or narrower, half at"
                + " %.2f°, three quarters at %.2f°. The weeks that never read at any field the"
                + " study considers are the near-full and near-conjunction ones, whose dark or"
                + " lit part stays under a pixel.",
                below[below.length - 1 - below.length / 4], below[below.length / 2],
                below[below.length / 4]));
        p("");
        p("**The 2026 apparitions, day by day** (00:00 UTC at Oslo): the days whose phase reads,"
                + " evening (east of the Sun) and morning (west).");
        p("");
        p("| field | evening days | morning days |");
        p("|---:|---:|---:|");
        Instant from = Instant.parse("2026-01-22T00:00:00Z");
        Instant to = Instant.parse("2027-01-01T00:00:00Z");
        for (double field : new double[] {1.0, 0.5, 0.25}) {
            double perArcsec = at1 / field;
            int east = 0;
            int west = 0;
            for (Instant t = from; t.isBefore(to); t = t.plus(Duration.ofDays(1))) {
                MercuryObservation m = mercury.observe(OSLO.at(t));
                if (readable(m.solidBodyDiameterArcseconds() * perArcsec, m.illuminatedFraction())) {
                    if (m.side() == SolarSystemService.Side.EAST_OF_SUN) {
                        east++;
                    } else {
                        west++;
                    }
                }
            }
            p(String.format(Locale.ROOT, "| %s° | %d | %d |", fmt(field), east, west));
        }
        p("");
    }

    // ---- painting ---------------------------------------------------------------

    /** Mercury as the bodies layer would be offered it, under a rule's mark. */
    static OverlayContribution.Body mercuryBody(Observer at, boolean horizon, double markPx) {
        MercuryObservation m = mercury.observe(at);
        String state = switch (m.phase()) {
            case IN_FRONT_OF_SUN -> IN_FRONT;
            case BEHIND_SUN -> BEHIND;
            default -> null;
        };
        return new OverlayContribution.Body(MERCURY, "Mercury", m.astrometricJ2000(),
                m.solidBodyDiameterArcseconds(),
                new OverlayContribution.Lit(m.illuminatedFraction(), m.brightLimbAngleDegrees(),
                        m.phaseAngleDegrees()),
                horizon && m.horizontal().altitudeDegrees() < 0.0, m.distanceKm(), InkRole.BODY,
                OverlayContribution.Moment.of(at), true, markPx, state);
    }

    /** The bodies a page is offered: the Sun (and Venus when asked) from the production module, Mercury from the study. */
    static OverlayRegistry offering(Observer at, boolean horizon, double markPx, boolean sun,
                                    boolean withVenus) {
        SolarSystemModule module = new SolarSystemModule(() -> at, () -> solar, () -> horizon);
        module.sunShowing(sun);
        module.venusShowing(withVenus);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        registry.offer("study.mercury", () -> List.of(mercuryBody(at, horizon, markPx)));
        return registry;
    }

    /** A page at any field, through a viewport, from the catalogue the nearest ladder field queries. */
    static ChartScene scene(SkyPosition centre, double field) {
        double ladder = field;
        if (field < ChartViewState.normalMinimumFieldDegrees()) {
            ladder = ChartViewState.normalMinimumFieldDegrees();
        }
        ChartScene queried = Atlas.assembler().assemble(
                new ChartViewState(centre, ladder, 8.0, null, null), PAGE_W, PAGE_H);
        if (ladder == field) {
            return queried;
        }
        return new ChartScene(new ChartViewport(centre, field, PAGE_W, PAGE_H), queried.stars(),
                queried.deepSkyObjects(), queried.title(), queried.limitingMagnitude(),
                queried.targetIdentity(), queried.geography());
    }

    /** The study's words: the production words, with its proposed Mercury words around them. */
    static PageWords words(PageText production, boolean norsk) {
        return (PageWords) java.lang.reflect.Proxy.newProxyInstance(
                PageWords.class.getClassLoader(), new Class<?>[] {PageWords.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (args != null && args.length == 1 && args[0] instanceof String s) {
                        if (name.equals("bodyName") && s.equals(MERCURY)) {
                            return norsk ? "Merkur" : "Mercury";
                        }
                        if (name.equals("bodySymbol") && s.equals(MERCURY)) {
                            return norsk ? "(et kartografisk symbol, ikke Merkurs tilsynelatende diameter)"
                                    : "(a cartographic symbol, not Mercury's apparent diameter)";
                        }
                        if (name.equals("bodyState") && s.equals(IN_FRONT)) {
                            return norsk ? "foran Solen" : "in front of the Sun";
                        }
                        if (name.equals("bodyState") && s.equals(BEHIND)) {
                            return norsk ? "bak Solen" : "behind the Sun";
                        }
                    }
                    try {
                        return method.invoke(production, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /** Paints a page through the production renderer and bodies layer; returns what was drawn of Mercury. */
    static Drawn paint(BufferedImage into, ChartScene scene, OverlayRegistry registry,
                       ChartPalette palette, PageWords words, Set<ChartStructure> raised) {
        List<ReferenceInk.BodyPlacement> placed = new ArrayList<>();
        Graphics2D g = into.createGraphics();
        try {
            ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, words);
            renderer.render(g, scene, ChartOptions.DEFAULTS.withPalette(palette),
                    (layerG, layerScene, reserved) -> ReferenceInk.paint(layerG, layerScene,
                            registry.collect(), palette, words, reserved, raised),
                    (layerG, layerScene, reserved) -> {
                        DrawnPage page = DrawnPage.of(layerScene);
                        placed.addAll(ReferenceInk.paintBodies(layerG, page, registry.collect(),
                                palette, words, reserved, ReferenceInk.referenceBoxes(page,
                                        registry.collect(), words, reserved,
                                        ChartStructure.membersOf(raised)),
                                ChartStructure.membersOf(raised)));
                    }, null, raised);
        } finally {
            g.dispose();
        }
        for (ReferenceInk.BodyPlacement b : placed) {
            if (MERCURY.equals(b.identity())) {
                return new Drawn(b.disc().getBounds2D().getWidth(), b.symbol(), b.box() != null,
                        b.state(), b.centre(), b.box(), spoken(b, words));
            }
        }
        return null;
    }

    /** What a reader who cannot see the page hears of Mercury, by the component's own rule. */
    private static String spoken(ReferenceInk.BodyPlacement body, PageWords words) {
        StringBuilder said = new StringBuilder(body.name() != null ? body.name()
                : words.bodyName(body.identity()));
        if (body.state() != null) {
            said.append(", ").append(words.bodyState(body.state()));
        }
        if (body.symbol()) {
            said.append(' ').append(words.bodySymbol(body.identity()));
        }
        return said.append('.').toString();
    }

    /** Writes a study page, STUDY in its corner and the caption's first words at its foot. */
    private static void write(BufferedImage page, String name, String corner) throws IOException {
        Graphics2D g = page.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g.setColor(new Color(170, 30, 30));
            g.drawString("STUDY " + corner, page.getWidth() - 12 - g.getFontMetrics()
                    .stringWidth("STUDY " + corner), 18);
        } finally {
            g.dispose();
        }
        ImageIO.write(page, "png", new File(DIR, name + ".png"));
    }

    private static Drawn page(String name, Observer at, double field, ChartPalette palette,
                              boolean norsk, double markPx, boolean sun, boolean horizon,
                              String corner) throws IOException {
        MercuryObservation m = mercury.observe(at);
        BufferedImage image = new BufferedImage(PAGE_W, PAGE_H, BufferedImage.TYPE_INT_RGB);
        Drawn d = paint(image, scene(m.astrometricJ2000(), field), offering(at, horizon, markPx,
                sun, false), palette, words(norsk ? NORSK : ENGLISH, norsk), Set.of());
        write(image, name, corner);
        return d;
    }

    // ---- C. rule T against rule M --------------------------------------------------

    private static void rules(List<Named> appearances, List<Named> events) throws IOException {
        p("## C. Rule T against rule M, drawn");
        p("");
        p("Each page is centred on Mercury at the instant, painted by the production renderer"
                + " and bodies layer; under rule M the 6 px mark stands in while the true disc is"
                + " smaller, phased by the page's own lit region, and is a cartographic symbol -"
                + " beneath the stars, not opaque, spoken as one - exactly as Venus's is. The"
                + " drawn width is the bodies layer's own.");
        p("");
        p("| page | instant (UTC) | phase | field | rule | drawn px | symbol | spoken |");
        p("|---|---|---|---:|---|---:|---|---|");
        Named elongation = named(events, "2026-greatest-elongation-east-1");
        Observer at = OSLO.at(elongation.when());
        for (double field : new double[] {8.0, 3.0, 1.0}) {
            for (boolean m : new boolean[] {false, true}) {
                String name = "elongation-" + (m ? "mark" : "true") + "-" + fmt(field);
                Drawn d = page(name, at, field, ChartPalette.WHITE_PAPER, false, m ? MARK_PX : 0.0,
                        false, false, (m ? "rule M" : "rule T") + ", " + fmt(field) + "°");
                row(name, elongation.when(), field, m, d);
            }
        }
        // The four appearances at 1° under rule M, each phase; the black sky and Norwegian.
        String[] tags = {"gibbous-evening", "crescent-evening", "crescent-morning", "gibbous-morning"};
        for (int i = 0; i < appearances.size(); i++) {
            Named n = appearances.get(i);
            String name = "appearance-" + tags[i] + "-1";
            Drawn d = page(name, OSLO.at(n.when()), 1.0, i % 2 == 0 ? ChartPalette.WHITE_PAPER
                    : ChartPalette.BLACK_SKY, i == 1, MARK_PX, false, false, "rule M, 1°");
            row(name, n.when(), 1.0, true, d);
        }
        // Below the drawn horizon: the 2026 superior conjunction's eve at 18:00 is under it.
        Named superior = named(events, "2026-superior-conjunction-1");
        String name = "below-horizon-mark-3";
        Drawn d = page(name, OSLO.at(superior.when()), 3.0, ChartPalette.WHITE_PAPER, true,
                MARK_PX, true, true, "rule M, 3°");
        row(name, superior.when(), 3.0, true, d);
        p("");
        p("Rule T draws Mercury at 1.2-3.1 px on the 1° page and under a pixel from 4° wide - a"
                + " dot fainter than the faintest star, and no phase at any ordinary field; rule"
                + " M keeps a phased 6 px symbol findable at every field, the phase carried by"
                + " the mark's lit region, never by a size the disc does not have. Near full the"
                + " mark reads full; near inferior conjunction it is dark with its limb; no"
                + " sliver is invented (C3, #512 ruling 2).");
        p("");
    }

    private static void row(String name, Instant when, double field, boolean m, Drawn d) {
        MercuryObservation o = mercury.observe(OSLO.at(when));
        p(String.format(Locale.ROOT, "| `%s` | %s | %s | %s° | %s | %s | %s | %s |", name,
                day(when), word(o), fmt(field), m ? "M" : "T",
                d == null ? "not drawn" : String.format(Locale.ROOT, "%.2f", d.discPx()),
                d == null ? "-" : d.symbol() ? "yes" : "no", d == null ? "-" : d.spoken()));
    }

    // ---- D. the conjunctions ----------------------------------------------------------

    private static void conjunctions(List<Named> events) throws IOException {
        p("## D. At the conjunctions, painted");
        p("");
        p("With the Sun on, Oslo, the page centred on the Sun; the bodies layer paints the"
                + " farther first, so the nearer covers it (C4), and a symbol in front of an"
                + " opaque disc is painted over it (#514). The spoken state is the proposal's -"
                + " the table's word appended, as Venus's is (#512 ruling 3).");
        p("");
        p("| page | event | instant (UTC) | field | rule | elongation | topocentric limb margin | phase | drawn px | spoken |");
        p("|---|---|---|---:|---|---:|---:|---|---:|---|");
        String[][] cases = {
                {"transit-2019", "2019-transit", "1"},
                {"transit-2019", "2019-transit", "0.25"},
                {"grazing-1937", "1937-transit", "1"},
                {"grazing-1937", "1937-transit", "0.25"},
                {"superior-2026", "2026-superior-conjunction-2", "1"},
                {"inferior-2026", "2026-inferior-conjunction-1", "1"}};
        for (String[] c : cases) {
            Named n = named(events, c[1]);
            double field = Double.parseDouble(c[2]);
            Observer at = OSLO.at(n.when());
            MercuryObservation m = mercury.observe(at);
            double margin = m.elongationDegrees() * 3600.0
                    - (m.sunAngularRadiusArcseconds() + m.solidBodyDiameterArcseconds() / 2.0);
            BufferedImage image = new BufferedImage(PAGE_W, PAGE_H, BufferedImage.TYPE_INT_RGB);
            // Centred on the Sun when Mercury is within a quarter degree of it, so both
            // stand on the page; else on Mercury.
            SkyPosition centre = field < 1.0 || m.elongationDegrees() > 0.25
                    ? m.astrometricJ2000()
                    : ((SolarSystemService.SunObservation) solar.observe(
                            SolarSystemService.Body.SUN, at)).astrometricJ2000();
            Drawn d = paint(image, scene(centre, field), offering(at, false, MARK_PX, true, false),
                    ChartPalette.WHITE_PAPER, words(ENGLISH, false), Set.of());
            String name = c[0] + "-" + fmt(field);
            write(image, name, "rule M, " + fmt(field) + "°");
            p(String.format(Locale.ROOT, "| `%s` | %s | %s | %s° | M | %.4f° | %+.3f″ | %s | %s | %s |",
                    name, c[1], day(n.when()), fmt(field), m.elongationDegrees(), margin, word(m),
                    d == null ? "not drawn" : String.format(Locale.ROOT, "%.2f", d.discPx()),
                    d == null ? "-" : d.spoken()));
        }
        p("");
        // Every transit at greatest transit, geocentric: where Mercury's centre stands
        // against the Sun's centre dot, which the production Sun paints at R/6.
        int inside = 0;
        int partly = 0;
        StringBuilder which = new StringBuilder();
        for (Named n : events) {
            if (!n.name().endsWith("-transit")) {
                continue;
            }
            MercuryObservation g = mercury.observeGeocentric(n.when());
            double sep = g.elongationDegrees() * 3600.0;
            // The dot as it was drawn before #541: a sixth of the radius.
            double dot = g.sunAngularRadiusArcseconds() / 6.0;
            double markArcsecAt1 = MARK_PX / JovianCartographyStudyMain.pxPerArcsec(1.0, PAGE_W, PAGE_H) / 2.0;
            if (sep + markArcsecAt1 <= dot) {
                inside++;
                which.append(which.length() == 0 ? "" : ", ").append(n.name().substring(0, 4));
            } else if (sep - markArcsecAt1 < dot) {
                partly++;
                which.append(which.length() == 0 ? "" : ", ").append(n.name().substring(0, 4))
                        .append(" (partly)");
            }
        }
        p(String.format(Locale.ROOT, "**The Sun's centre dot met central transits; it is now"
                + " capped.** The production Sun drew a centre dot of a sixth of its radius -"
                + " about 40 px on the 1° page, 160 px on the 0.25° page. Of the 28 transits, at"
                + " greatest transit (geocentric), Mercury's 1° mark stood wholly on that dot in"
                + " %d and partly in %d (%s), its dark tone a faint grey on the dot's ink, and"
                + " below 1° the dot filled the page. That finding is #539's, ruled on (ruling"
                + " 2) and answered by #541: the dot is now capped at %.0f px, so these pages"
                + " show the mark on the Sun's ground beside a small dot. Venus's 2012 transit"
                + " stood outside the dot either way.", inside, partly, which,
                ReferenceInk.SUN_DOT_MAX_RADIUS_PX));
        p("");
        p("A transit paints Mercury's dark mark (at 1°) or dark disc (at 0.25°) over the Sun's"
                + " ground-filled ring - nearer, so later, the #514 rule for a symbol in front of"
                + " an opaque disc. Behind the Sun the mark is laid in the reference layer,"
                + " beneath the stars, and the Sun's ground-filled disc then covers it: the"
                + " bodies layer reports a 6 px symbol, the page shows none, and the spoken"
                + " description says *behind the Sun*. The 1937 grazing transit seen from Oslo is"
                + " *near inferior conjunction* with the centre +5.3″ outside the limb sum -"
                + " parallax moves it there; the geocentric margin the tests hold under ruling"
                + " 6's amendment is +0.017″. The mark stands on the ring's edge, no state is"
                + " spoken. The 2026 inferior conjunction stands 3.6° from the Sun: the page is"
                + " centred on Mercury, its mark dark with its limb, the lit sliver under a"
                + " pixel at every field.");
        p("");
    }

    // ---- E. the frame --------------------------------------------------------------

    private static void frame(List<Week> weekly) {
        p("## E. The lit shape on the J2000 page");
        p("");
        double[] maxTurn = new double[3];
        Instant[] at = new Instant[3];
        double[] fields = {0.25, 1.0, 8.0};
        for (int i = 0; i < weekly.size(); i += 13) {
            Week w = weekly.get(i);
            for (int f = 0; f < fields.length; f++) {
                SkyPosition centre = JovianChartGeometry.offset(w.m().astrometricJ2000(), 90.0,
                        0.45 * fields[f]);
                DrawnPage own = JovianCartographyStudyMain.emptyPage(centre, fields[f], PAGE_W, PAGE_H);
                double[] north = PageBasis.at(own, w.m().astrometricJ2000()).orElseThrow()
                        .direction(0.0);
                double turn = Math.abs(Math.toDegrees(Math.atan2(north[0], -north[1])));
                if (turn > maxTurn[f]) {
                    maxTurn[f] = turn;
                    at[f] = w.when();
                }
            }
        }
        p(String.format(Locale.ROOT, "The lit region is the page's own (`ReferenceInk.litRegion`,"
                + " C3): the half disc towards the bright limb with the terminator's half-ellipse"
                + " of semi-axis r·|cos i|, turned to χ of date through the page's north and east"
                + " at Mercury (`PageBasis`), never recomputed from J2000 inputs. With Mercury"
                + " near the page's edge the basis turns from the page's top by up to %.3f° on"
                + " the 0.25° page, %.2f° on the 1° page and %.2f° on the 8° page (every 13th week"
                + " over the century); the 0.25° page turns least, as a narrower gnomonic page"
                + " must. `MercuryCartographyTest` holds the round trip on the candidate pages:"
                + " Mercury's J2000 place projects to the page and back to within a"
                + " microarcsecond, the bodies layer centres the mark on that pixel, and the lit"
                + " area is k of the disc's.", maxTurn[0], maxTurn[1], maxTurn[2]));
        p("");
    }

    // ---- F. labels -----------------------------------------------------------------

    private static void labels(List<Named> events, List<Named> appearances) throws IOException {
        p("## F. Labels, at every named instant");
        p("");
        p("Mercury's name through the bodies layer's ordinary candidates - adjacent boxes"
                + " around the mark, refused rather than moved when none is clean (C4-C6) - at"
                + " every named instant, Oslo, 1°, rule M, the Sun on. Where each name landed is"
                + " a font's answer and stands in `platform.md`; here, what kind of answer it"
                + " was.");
        p("");
        q("## Mercury's name at each named instant, 1°, rule M, the Sun on");
        q("");
        q("| instant | name | box (x, y, w, h) |");
        q("|---|---|---|");
        int named = 0;
        int refused = 0;
        int covered = 0;
        List<Named> all = new ArrayList<>(events);
        all.addAll(appearances);
        for (Named n : all) {
            Observer at = OSLO.at(n.when());
            MercuryObservation m = mercury.observe(at);
            BufferedImage image = new BufferedImage(PAGE_W, PAGE_H, BufferedImage.TYPE_INT_RGB);
            Drawn d = paint(image, scene(m.astrometricJ2000(), 1.0), offering(at, false, MARK_PX,
                    true, false), ChartPalette.WHITE_PAPER, words(ENGLISH, false), Set.of());
            if (d == null) {
                covered++;
            } else if (d.named()) {
                named++;
            } else {
                refused++;
            }
            q(String.format(Locale.ROOT, "| %s | %s | %s |", day(n.when()), n.name(),
                    d == null ? "not drawn" : d.box() == null ? "refused"
                            : String.format(Locale.ROOT, "%.0f, %.0f, %.0f, %.0f", d.box().getX(),
                                    d.box().getY(), d.box().getWidth(), d.box().getHeight())));
        }
        q("");
        p(String.format(Locale.ROOT, "Of the %d named instants (Espenak's almanac rows, the 28"
                + " transits, the four appearances): Mercury drawn and named, drawn with its"
                + " name refused, or not drawn (behind the Sun) - the counts are in"
                + " `platform.md`, beside each name's box. On the capture machine a name was"
                + " refused only where Mercury's mark stood on or inside the Sun's ink - the"
                + " transits and the 2026 superior conjunction - as Venus's at 1°-2° (#514, C4);"
                + " the 1937 grazing transit and two transits near the limb kept a box outside"
                + " the ring.", all.size()));
        p("");
        // A crowded field: Mercury's closest pass to a bright star in the weekly rows near
        // the Milky Way - the label refused or placed by the ordinary rule.
        Instant crowded = Instant.parse("2026-12-24T00:00:00Z");
        Observer at = OSLO.at(crowded);
        BufferedImage image = new BufferedImage(PAGE_W, PAGE_H, BufferedImage.TYPE_INT_RGB);
        MercuryObservation m = mercury.observe(at);
        Drawn d = paint(image, scene(m.astrometricJ2000(), 3.0), offering(at, false, MARK_PX,
                false, false), ChartPalette.WHITE_PAPER, words(ENGLISH, false), Set.of());
        write(image, "crowded-sagittarius-3", "rule M, 3°");
        q(String.format(Locale.ROOT, "Crowded field, %s at 3° (Sagittarius): Mercury's name %s.",
                day(crowded), d == null ? "not drawn" : d.named() ? "placed" : "refused"));
        q("");
        q(String.format(Locale.ROOT, "Totals: %d named, %d refused, %d not drawn.", named,
                refused, covered));
        q("");
        p(String.format(Locale.ROOT, "A crowded page: %s at 3°, Mercury in Sagittarius among the"
                + " Milky Way's stars (`crowded-sagittarius-3.png`); the name is placed or"
                + " refused by the same rule, never moved onto a star.", day(crowded)));
        p("");
        p("The Solar System labels class (#526) raises Mercury's name with the others the"
                + " moment the module offers it - the class is the bodies layer's, not a list."
                + " Under #537 (outside this sprint) zoom would keep the class raised; it is"
                + " already immune to zoom, its availability being a contributed body, not a"
                + " drawn line.");
        p("");
    }

    // ---- G. paper ------------------------------------------------------------------

    private static void paper(List<Named> appearances) throws Exception {
        p("## G. The exported sheet, from the same geometry");
        p("");
        Named n = appearances.get(1);
        Observer at = OSLO.at(n.when());
        MercuryObservation m = mercury.observe(at);
        ChartViewState state = new ChartViewState(m.astrometricJ2000(), 1.0, 8.0, null, null);
        OverlayRegistry registry = offering(at, false, MARK_PX, false, false);
        PageWords w = words(ENGLISH, false);
        List<ReferenceInk.BodyPlacement> placed = new ArrayList<>();
        double[] sceneWidth = new double[1];
        SheetRecording sheet = ChartSheet.record(Atlas.assembler()::assemble, state,
                ChartOptions.DEFAULTS,
                (g, scene, reserved) -> ReferenceInk.paint(g, scene, registry.collect(),
                        ChartPalette.WHITE_PAPER, w, reserved, Set.of()),
                (g, scene, reserved) -> {
                    DrawnPage page = DrawnPage.of(scene);
                    sceneWidth[0] = scene.viewport().widthPx();
                    placed.addAll(ReferenceInk.paintBodies(g, page, registry.collect(),
                            ChartPalette.WHITE_PAPER, w, reserved, ReferenceInk.referenceBoxes(page,
                                    registry.collect(), w, reserved, s -> false)));
                },
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4, w, Set.of());
        byte[] png = SheetWriters.write(sheet, SheetFormat.PNG, 150);
        Files.write(new File(DIR, "printable-a4-1.png").toPath(), png);
        ReferenceInk.BodyPlacement onPaper = placed.stream()
                .filter(b -> MERCURY.equals(b.identity())).findFirst().orElseThrow();
        double perArcsec = sceneWidth[0] / 3600.0;
        double trueWidth = m.solidBodyDiameterArcseconds() * perArcsec;
        double scale = chartPx(PaperSize.A4) / sceneWidth[0];
        p(String.format(Locale.ROOT, "The A4 sheet at 1°, %s (the crescent evening appearance),"
                + " recorded by `ChartSheet` with the same reference and bodies layers the screen"
                + " uses (`printable-a4-1.png`, written at 150 dpi for the record). The recording"
                + " lays the page out %.0f units wide for 1°, so Mercury's %.2f″ disc is %.2f"
                + " units and the bodies layer drew %.2f units, %s - the screen's rule in the"
                + " recording's own units. At %d dpi the chart area is %d px, %.2f px a unit: the"
                + " true disc prints %.1f px (%.2f mm) and the mark %.1f px (%.2f mm). **So rule"
                + " M's mark is a fixed fraction of the page, not of the paper's resolution**: on"
                + " A4 it prints %.1f times the true disc's width at 1°, which is honest as a"
                + " symbol and spoken as one, but a 300 dpi sheet could show the true disc at"
                + " %.1f px - phase-readable by the criterion in the weeks the A4 column of"
                + " section B counts.", day(n.when()), sceneWidth[0],
                m.solidBodyDiameterArcseconds(), trueWidth,
                onPaper.disc().getBounds2D().getWidth(), onPaper.symbol()
                        ? "the 6-unit mark (a symbol)" : "the true disc",
                PngSheetWriter.DEFAULT_RESOLUTION, chartPx(PaperSize.A4), scale, trueWidth * scale,
                trueWidth * scale * 25.4 / PngSheetWriter.DEFAULT_RESOLUTION,
                onPaper.disc().getBounds2D().getWidth() * scale,
                onPaper.disc().getBounds2D().getWidth() * scale * 25.4
                        / PngSheetWriter.DEFAULT_RESOLUTION,
                onPaper.disc().getBounds2D().getWidth() / trueWidth, trueWidth * scale));
        p("");
    }

    // ---- H. the closer fields --------------------------------------------------------

    private static void closer(List<Week> weekly, List<Named> appearances, double at1)
            throws Exception {
        p("## H. The closer fields of #481, painted");
        p("");
        p("Real pages at 1°, 0.5° and 0.25°, centred on the body, through the production"
                + " renderer and bodies layer; the field below the ladder through a viewport"
                + " the ladder does not offer, over the stars the 1° page queried. Mercury"
                + " under rule M; Venus as the production module draws it.");
        p("");
        p("| page | body | instant (UTC) | phase | field | drawn px | symbol | lit px | dark px | readable | stars on the page | grid steps (RA, Dec) and spacing | the page says |");
        p("|---|---|---|---|---:|---:|---|---:|---:|---|---:|---|---|");
        List<Named> venusEvents = readEvents(VENUS_EVENTS);
        Object[][] bodies = {
                {"mercury-elongation", OSLO.at(Instant.parse("2026-02-19T18:00:00Z")), false},
                {"mercury-crescent", OSLO.at(appearances.get(1).when()), false},
                {"mercury-gibbous", OSLO.at(appearances.get(0).when()), false},
                {"venus-elongation", OSLO.at(named(venusEvents, "2026-greatest-elongation-east").when()), true},
                {"venus-crescent", OSLO.at(Instant.parse("2026-10-06T00:00:00Z")), true}};
        BufferedImage strip = new BufferedImage(3 * 240, bodies.length * 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = strip.createGraphics();
        sg.setColor(Color.WHITE);
        sg.fillRect(0, 0, strip.getWidth(), strip.getHeight());
        int row = 0;
        for (Object[] b : bodies) {
            String tag = (String) b[0];
            Observer at = (Observer) b[1];
            boolean isVenus = (Boolean) b[2];
            int col = 0;
            for (double field : new double[] {1.0, 0.5, 0.25}) {
                BufferedImage image = new BufferedImage(PAGE_W, PAGE_H, BufferedImage.TYPE_INT_RGB);
                SkyPosition centre;
                double k;
                String phase;
                OverlayRegistry registry;
                if (isVenus) {
                    VenusService.VenusObservation v = venus.observe(at);
                    centre = v.astrometricJ2000();
                    k = v.illuminatedFraction();
                    phase = v.phase().name().toLowerCase(Locale.ROOT).replace('_', ' ');
                    SolarSystemModule module = new SolarSystemModule(() -> at, () -> solar, () -> false);
                    module.venusShowing(true);
                    registry = new OverlayRegistry();
                    registry.offer(SolarSystemModule.ID, module::contributedGeometry);
                } else {
                    MercuryObservation m = mercury.observe(at);
                    centre = m.astrometricJ2000();
                    k = m.illuminatedFraction();
                    phase = word(m);
                    registry = offering(at, false, MARK_PX, false, false);
                }
                ChartScene scene = scene(centre, field);
                List<ReferenceInk.BodyPlacement> placed = new ArrayList<>();
                PageWords w = words(ENGLISH, false);
                Graphics2D g = image.createGraphics();
                try {
                    new ChartRenderer(StarSizePolicy.DEFAULT, w).render(g, scene,
                            ChartOptions.DEFAULTS,
                            (lg, ls, reserved) -> ReferenceInk.paint(lg, ls, registry.collect(),
                                    ChartPalette.WHITE_PAPER, w, reserved, Set.of()),
                            (lg, ls, reserved) -> {
                                DrawnPage page = DrawnPage.of(ls);
                                placed.addAll(ReferenceInk.paintBodies(lg, page, registry.collect(),
                                        ChartPalette.WHITE_PAPER, w, reserved,
                                        ReferenceInk.referenceBoxes(page, registry.collect(), w,
                                                reserved, s -> false)));
                            }, null, Set.of());
                } finally {
                    g.dispose();
                }
                ReferenceInk.BodyPlacement body = placed.get(0);
                double px = body.disc().getBounds2D().getWidth();
                int stars = 0;
                DrawnPage drawnPage = DrawnPage.of(scene);
                ViewportMapping mapping = new ViewportMapping(drawnPage);
                for (juranometria.chart.Star s : scene.stars()) {
                    if (s.magnitude() > scene.limitingMagnitude()) {
                        continue;
                    }
                    PixelPoint pp = drawnPage.projection().project(s.position())
                            .map(mapping::toPixel).orElse(null);
                    if (pp != null && pp.x() >= 0 && pp.x() < PAGE_W && pp.y() >= 0 && pp.y() < PAGE_H) {
                        stars++;
                    }
                }
                juranometria.render.EquatorialGrid.GridSpec grid =
                        juranometria.render.EquatorialGrid.spec(scene.viewport());
                double pxPerDeg = PAGE_W / field;
                // Seconds of time below a minute, since #543 gave the closer
                // fields finer steps.
                String raStep = grid.raStepDegrees() < 0.25
                        ? String.format(Locale.ROOT, "%.0fs", grid.raStepDegrees() * 240.0)
                        : String.format(Locale.ROOT, "%.0fm", grid.raStepDegrees() * 4.0);
                String lines = String.format(Locale.ROOT, "%s, %.0f′ - %.0f and %.0f px apart",
                        raStep, grid.decStepDegrees() * 60.0,
                        grid.raStepDegrees() * Math.cos(Math.toRadians(centre.decDegrees())) * pxPerDeg,
                        grid.decStepDegrees() * pxPerDeg);
                String says = drawnPage.describe(w);
                says = says.substring(says.indexOf("Field"), says.indexOf("projection") + 10);
                String name = tag + "-" + fmt(field);
                write(image, name, (isVenus ? "Venus" : "rule M") + ", " + fmt(field) + "°"
                        + (field < 1.0 ? " (not offered)" : ""));
                p(String.format(Locale.ROOT, "| `%s` | %s | %s | %s | %s° | %.2f | %s | %.2f | %.2f | %s | %d | %s | %s |",
                        name, isVenus ? "Venus" : "Mercury", day(at.instant()), phase, fmt(field),
                        px, body.symbol() ? "yes" : "no", k * px, (1.0 - k) * px,
                        readable(px, k) && !body.symbol() ? "yes" : "no", stars, lines, says));
                // The strip: a 240 px window around the body, at the page's own pixels.
                PixelPoint c = body.centre();
                int x0 = (int) Math.round(c.x()) - 120;
                int y0 = (int) Math.round(c.y()) - 120;
                for (int y = 0; y < 240; y++) {
                    for (int x = 0; x < 240; x++) {
                        int sx = x0 + x;
                        int sy = y0 + y;
                        if (sx >= 0 && sx < PAGE_W && sy >= 0 && sy < PAGE_H) {
                            strip.setRGB(col * 240 + x, row * 240 + y, image.getRGB(sx, sy));
                        }
                    }
                }
                sg.setColor(new Color(170, 30, 30));
                sg.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                sg.drawString(fmt(field) + "°", col * 240 + 6, row * 240 + 14);
                col++;
            }
            row++;
        }
        sg.setColor(new Color(200, 200, 200));
        for (int i = 1; i < 3; i++) {
            sg.drawLine(i * 240, 0, i * 240, strip.getHeight());
        }
        for (int i = 1; i < bodies.length; i++) {
            sg.drawLine(0, i * 240, strip.getWidth(), i * 240);
        }
        sg.dispose();
        ImageIO.write(strip, "png", new File(DIR, "closer-fields.png"));
        p("");
        p("`closer-fields.png` cuts a 240 px window around the body from each of these pages,"
                + " at the page's own pixels - no magnification: what each field shows. The"
                + " findings on the pages themselves, beyond the body:");
        p("");
        p("- **The page's own words rounded the field.** As measured for #539, the title"
                + " block and the spoken description printed one decimal: the 0.25° page said"
                + " *Field 0.3°*. A closer field needs the field printed to its own precision."
                + " *Met by #543:* these pages now say 0.25 (the table's last column).");
        p("- **The grid's finest steps were built for the ladder.** As measured for #539, its"
                + " smallest steps were 1m of right ascension and 15′ of declination; at 0.25°"
                + " one step spanned the whole page, so at most one line each way was drawn,"
                + " often none - the reader lost the grid's scale exactly where scale matters"
                + " most. A closer field needs finer steps (15s, 2′) before it is offered."
                + " *Met by #543:* the table's grid column shows the steps these pages now take.");
        p("- **The page may hold no star at all.** A 0.25° field near the ecliptic often"
                + " contains no catalogue star at V 8.0; the body then stands alone, which is"
                + " honest but leaves the reader nothing to orient by except the body and the"
                + " title.");
        p("- **The catalogue query is the 1° page's.** The study reuses it, which is exact"
                + " (the narrower page is inside it); production would query its own field."
                + " *Met by #543:* production does, proven in `docs/studies/closer-fields`.");
        p("");
    }

    /** The chart area's width on a sheet at the default resolution, in pixels. */
    static int chartPx(PaperSize paper) {
        return (int) Math.round(paper.chartWideMm() / 25.4 * PngSheetWriter.DEFAULT_RESOLUTION);
    }

    // ---- formatting -------------------------------------------------------------------

    private static String word(MercuryObservation m) {
        return m.phase().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    static String fmt(double v) {
        if (v == Math.rint(v)) {
            return String.format(Locale.ROOT, "%.0f", v);
        }
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
    }

    private static String day(Instant t) {
        return t.toString().substring(0, 16).replace('T', ' ');
    }

    /** Area check for the tests: the lit share of a disc at a page point. */
    static double litShare(PixelPoint centre, double radius, PageBasis basis,
                           OverlayContribution.Lit lit) {
        Area region = ReferenceInk.litRegion(centre, radius, basis, lit);
        return area(region) / (Math.PI * radius * radius);
    }

    /** A shape's area by the shoelace formula over its flattened outline. */
    static double area(java.awt.Shape shape) {
        double total = 0.0;
        double[] c = new double[6];
        double sx = 0;
        double sy = 0;
        double px = 0;
        double py = 0;
        for (java.awt.geom.PathIterator it = shape.getPathIterator(null, 0.01); !it.isDone();
                it.next()) {
            switch (it.currentSegment(c)) {
                case java.awt.geom.PathIterator.SEG_MOVETO -> {
                    sx = c[0];
                    sy = c[1];
                    px = c[0];
                    py = c[1];
                }
                case java.awt.geom.PathIterator.SEG_LINETO -> {
                    total += px * c[1] - c[0] * py;
                    px = c[0];
                    py = c[1];
                }
                case java.awt.geom.PathIterator.SEG_CLOSE -> {
                    total += px * sy - sx * py;
                    px = sx;
                    py = sy;
                }
                default -> throw new IllegalStateException("a flattened path has lines only");
            }
        }
        return Math.abs(total) / 2.0;
    }
}
