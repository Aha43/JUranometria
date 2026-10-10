package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.jovianchart.JovianModule;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.JovianSystemService.Configuration;
import juranometria.solar.JovianSystemService.MoonPlace;
import juranometria.ui.ChartComponent;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Jupiter and the Galilean moons on the production chart (Sprint 45,
 * issue #486): the merged Jovian module (#484, #485) through the real
 * {@code ChartComponent}, on the journeys #485 names - a normal spread,
 * the 11 December 2026 triple-transit sequence, a moon behind Jupiter and
 * in and partly in its shadow, the 36°, 8°, 3° and 1° views, both palettes
 * and both languages, and the drawn and hidden horizon. Production ink
 * only: no study mark. The portable report holds positions against the
 * service, each moon's decision and state, and what cannot resolve at
 * each scale; where each name went depends on this desktop's fonts and is
 * the platform record beside it.
 */
public final class JupiterOnTheChartStudyMain {

    private static final File DIR = new File("docs/studies/jupiter-on-the-chart");
    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-12-11T22:45:00Z"));
    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();
    private static JovianSystemService service;

    private JupiterOnTheChartStudyMain() {
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
        service = JovianSystemService.load();
        PlatformEvidence.preface(PLATFORM,
                "Jupiter's and its moons' names, as this desktop's fonts place them",
                "Sprint 45, issue #486.");
        q("## Names on each page");
        q("");
        q("A name not listed as placed was refused on its own: no clean adjacent box.");
        q("");
        p("# Jupiter and the Galilean moons on the chart");
        p("");
        p("Sprint 45, issue #486. The production composition - `ChartComponent` over the"
                + " bundled catalogue with the Jovian module (#484, #485) attached and"
                + " *Jupiter and moons on the chart* on - on the journeys #485 names."
                + " Production ink only. Jupiter is drawn at its true size or as a 6 px"
                + " cartographic symbol, whichever is larger; each moon as a 3 px"
                + " cartographic symbol, never its apparent diameter, which is below a"
                + " pixel on every page. Positions are the Jovian service's astrometric"
                + " J2000 places for the instant and observer stated (Oslo, 59.913° N,"
                + " 10.752° E). Regenerate with `make jupiter-on-the-chart-study`; where"
                + " each name went is in `platform.md`.");
        p("");

        Instant triple = Instant.parse("2026-12-11T22:45:00Z");
        for (double field : new double[] {36.0, 8.0, 3.0, 1.0}) {
            page(String.format(Locale.ROOT, "triple-%.0f", field),
                    String.format(Locale.ROOT, "The triple transit, 2026-12-11 22:45 UTC, %.0f° field",
                            field),
                    OSLO.at(triple), field, field == 3.0 ? NORSK : ENGLISH,
                    field == 8.0 ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER, false);
        }
        for (String hhmm : List.of("22:30", "22:55", "23:00")) {
            page("sequence-" + hhmm.replace(":", ""),
                    "The 11 December 2026 sequence at " + hhmm + " UTC, 1° field",
                    OSLO.at(Instant.parse("2026-12-11T" + hhmm + ":00Z")), 1.0,
                    hhmm.equals("22:55") ? NORSK : ENGLISH,
                    hhmm.equals("23:00") ? ChartPalette.BLACK_SKY : ChartPalette.WHITE_PAPER,
                    false);
        }
        page("spread-1", "A normal spread, all four clear, 2026-03-06 06:00 UTC, 1° field",
                OSLO.at(Instant.parse("2026-03-06T06:00:00Z")), 1.0, ENGLISH,
                ChartPalette.WHITE_PAPER, false);
        page("spread-3-dark", "The same spread at 3° on the black sky",
                OSLO.at(Instant.parse("2026-03-06T06:00:00Z")), 3.0, ENGLISH,
                ChartPalette.BLACK_SKY, false);
        page("io-behind-1", "Io behind Jupiter (Horizons O), 2026-12-02 07:00 UTC, 1° field",
                OSLO.at(Instant.parse("2026-12-02T07:00:00Z")), 1.0, ENGLISH,
                ChartPalette.WHITE_PAPER, false);
        page("io-shadow-1", "Io wholly in Jupiter's shadow (Horizons u), 2026-12-02 04:00 UTC, 1° field",
                OSLO.at(Instant.parse("2026-12-02T04:00:00Z")), 1.0, NORSK,
                ChartPalette.WHITE_PAPER, false);
        page("io-partly-shadow-1",
                "Io partly in Jupiter's shadow (Horizons p), 2026-12-21 15:00 UTC, 1° field",
                OSLO.at(Instant.parse("2026-12-21T15:00:00Z")), 1.0, ENGLISH,
                ChartPalette.BLACK_SKY, false);
        Observer low = OSLO.at(Instant.parse("2026-12-01T12:20:00Z"));
        page("horizon-drawn-8", "Jupiter below the drawn horizon, 2026-12-01 12:20 UTC, 8° field",
                low, 8.0, ENGLISH, ChartPalette.WHITE_PAPER, true);
        page("horizon-hidden-8", "The same page with the horizon hidden",
                low, 8.0, ENGLISH, ChartPalette.WHITE_PAPER, false);
        scales();
        sheet();
        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        System.out.flush();
        PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
    }

    /** One production page, written as an image and reported. */
    private static void page(String name, String title, Observer observer, double field,
                             PageText words, ChartPalette palette, boolean horizon)
            throws Exception {
        SkyPosition jupiterAt = service.observeJupiter(observer).astrometricJ2000();
        SkyPosition centre = jupiterAt;
        if (horizon) {
            // The page centred on the horizon point nearest Jupiter, so the
            // drawn horizon and Jupiter below it share the page.
            double least = Double.MAX_VALUE;
            for (SkyPosition on : new LocalSky(observer).horizon().around(720)) {
                double sep = on.separationDegrees(jupiterAt);
                if (sep < least) {
                    least = sep;
                    centre = on;
                }
            }
        }
        SkyPosition at = centre;
        ChartComponent[] holder = new ChartComponent[1];
        JovianModule[] module = new JovianModule[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(900, 700);
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            holder[0].setViewState(new ChartViewState(at, field, 8.0, null, null));
            MeridianModule meridian = new MeridianModule(observer);
            meridian.showing(horizon, horizon, false);
            if (horizon) {
                holder[0].overlays().offer(MeridianModule.ID, meridian::contributedGeometry);
            }
            module[0] = new JovianModule(() -> observer, () -> service,
                    meridian::horizonShowing);
            module[0].showing(true);
            holder[0].overlays().offer(JovianModule.ID, module[0]::contributedGeometry);
        });
        SwingUtilities.invokeAndWait(() -> { });
        ChartComponent chart = holder[0];
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        List<ReferenceInk.BodyPlacement> drawn = chart.renderedBodies();
        DrawnPage page = DrawnPage.of(chart.currentScene());
        Map<String, String> decided = ReferenceInk.satelliteDecisions(page,
                chart.overlays().collect());
        ViewportMapping mapping = new ViewportMapping(page);
        Configuration c = service.observeMoons(observer);
        p("## " + title);
        p("");
        p("![](" + name + ".png)");
        p("");
        p(String.format(Locale.ROOT, "%s, %s. Jupiter %.1f° %s the horizon.",
                palette == ChartPalette.BLACK_SKY ? "Black sky" : "White paper",
                words == NORSK ? "Norwegian" : "English",
                Math.abs(c.jupiter().horizontal().altitudeDegrees()),
                c.jupiter().horizontal().altitudeDegrees() < 0 ? "below" : "above"));
        p("");
        p("| body | state | page decision | drawn at (px) | off the service's place (px) | mark |");
        p("|---|---|---|---|---:|---|");
        ReferenceInk.BodyPlacement jupiter = find(drawn, JovianModule.JUPITER);
        if (jupiter != null) {
            PixelPoint expected = mapping.toPixel(page.projection()
                    .project(c.jupiter().astrometricJ2000()).orElseThrow());
            java.awt.geom.Rectangle2D b = jupiter.disc().getBounds2D();
            p(String.format(Locale.ROOT, "| Jupiter | %s | drawn | (%.1f, %.1f) | %.4f | %s, %.1f px |",
                    jupiter.belowHorizon() ? "below the horizon" : "-",
                    jupiter.centre().x(), jupiter.centre().y(),
                    Math.hypot(jupiter.centre().x() - expected.x(), jupiter.centre().y() - expected.y()),
                    jupiter.symbol() ? "6 px cartographic symbol" : "true disc",
                    Math.max(b.getWidth(), b.getHeight())));
        } else {
            p("| Jupiter | - | off the page | - | - | - |");
        }
        for (MoonPlace m : c.moons()) {
            String id = JovianModule.identityOf(m.moon());
            ReferenceInk.BodyPlacement placed = find(drawn, id);
            String state = m.state().name().toLowerCase(Locale.ROOT).replace('_', ' ');
            String decision = decided.getOrDefault(id, "OFF_PAGE")
                    .toLowerCase(Locale.ROOT).replace('_', ' ');
            if (placed != null) {
                PixelPoint expected = mapping.toPixel(page.projection()
                        .project(m.astrometricJ2000()).orElseThrow());
                p(String.format(Locale.ROOT, "| %s | %s | %s | (%.1f, %.1f) | %.4f | 3 px symbol%s |",
                        name(m), state, decision, placed.centre().x(), placed.centre().y(),
                        Math.hypot(placed.centre().x() - expected.x(),
                                placed.centre().y() - expected.y()),
                        placed.state() == null ? "" : ", " + placed.state().replace("jovian.", "")));
            } else {
                p(String.format(Locale.ROOT, "| %s | %s | %s | - | - | not drawn |", name(m),
                        state, decision));
            }
        }
        p("");
        String said = chart.getAccessibleContext().getAccessibleDescription();
        int from = said.indexOf("Jupiter");
        p("Spoken: " + (from < 0 ? "(Jupiter not on the page)" : said.substring(from)));
        p("");
        StringBuilder names = new StringBuilder();
        for (ReferenceInk.BodyPlacement b : drawn) {
            names.append(b.name()).append(b.box() == null ? " refused; " : " placed; ");
        }
        q("- **" + title + "** (`" + name + ".png`): " + names.toString().strip());
    }

    /** What resolves at each field of the ladder, at the triple transit and at the spread. */
    private static void scales() throws Exception {
        p("## What resolves at each scale");
        p("");
        p("On the 900 × 700 page. A moon not drawn above the normal minimum field is"
                + " recorded with its reason - at Jupiter's edge, colliding with a"
                + " higher-precedence moon, or in front of a disc too small to hold its"
                + " mark - never silently enlarged. At the normal minimum field every"
                + " moon not behind Jupiter is drawn.");
        p("");
        p("| instant | field | Jupiter | Io | Europa | Ganymede | Callisto |");
        p("|---|---:|---|---|---|---|---|");
        for (String when : List.of("2026-12-11T22:45:00Z", "2026-03-06T06:00:00Z")) {
            Observer observer = OSLO.at(Instant.parse(when));
            for (double field : new double[] {36.0, 24.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0,
                    ChartViewState.normalMinimumFieldDegrees()}) {
                ChartComponent[] holder = new ChartComponent[1];
                SwingUtilities.invokeAndWait(() -> {
                    holder[0] = new ChartComponent(Atlas.assembler(), ENGLISH);
                    holder[0].setSize(900, 700);
                    holder[0].setViewState(new ChartViewState(
                            service.observeJupiter(observer).astrometricJ2000(), field, 8.0,
                            null, null));
                    JovianModule module = new JovianModule(() -> observer, () -> service,
                            () -> false);
                    module.showing(true);
                    holder[0].overlays().offer(JovianModule.ID, module::contributedGeometry);
                });
                DrawnPage page = DrawnPage.of(holder[0].currentScene());
                java.util.List<juranometria.module.OverlayRegistry.Owned> offered =
                        holder[0].overlays().collect();
                Map<String, String> decided = ReferenceInk.satelliteDecisions(page, offered);
                ReferenceInk.BodyPlacement j = find(ReferenceInk.bodyPlacements(page, offered,
                        ChartPalette.WHITE_PAPER, ENGLISH, List.of()), JovianModule.JUPITER);
                StringBuilder row = new StringBuilder(String.format(Locale.ROOT,
                        "| %s | %s° | %s |", when.substring(0, 16).replace('T', ' '),
                        trim(field), j == null ? "off the page"
                                : j.symbol() ? "6 px symbol" : "true disc"));
                for (JovianSystemService.Moon m : JovianSystemService.Moon.values()) {
                    row.append(' ').append(decided.getOrDefault(JovianModule.identityOf(m), "-")
                            .toLowerCase(Locale.ROOT).replace('_', ' ')).append(" |");
                }
                p(row.toString());
            }
        }
        p("");
    }

    /** The sheet's moment: the triple transit of 11 December 2026 at Oslo. */
    public static final Instant SHEET_MOMENT = Instant.parse("2026-12-11T22:45:00Z");
    /** The supported field rungs measured for the sheet, widest first; the last is read from the chart. */
    private static final double[] SHEET_LADDER = {42.0, 36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0,
            3.0, 2.0};

    /** The Jovian module at the sheet's moment, switched on, in a registry of its own. */
    private static juranometria.module.OverlayRegistry sheetModule(JovianSystemService jovian) {
        Observer at = OSLO.at(SHEET_MOMENT);
        juranometria.module.OverlayRegistry registry = new juranometria.module.OverlayRegistry();
        JovianModule module = new JovianModule(() -> at, () -> jovian, () -> false);
        module.showing(true);
        registry.offer(JovianModule.ID, module::contributedGeometry);
        return registry;
    }

    /** Each moon's decision on the A4 sheet's own page at a field. */
    private static Map<String, String> onTheSheet(JovianSystemService jovian, double field) {
        SkyPosition centre = jovian.observeJupiter(OSLO.at(SHEET_MOMENT)).astrometricJ2000();
        juranometria.chart.ChartScene scene = Atlas.assembler().assemble(
                new ChartViewState(centre, field, 8.0, null, null),
                juranometria.sheet.PaperSize.A4.chartWideUnits(),
                juranometria.sheet.PaperSize.A4.chartHighUnits());
        return ReferenceInk.satelliteDecisions(DrawnPage.of(scene),
                sheetModule(jovian).collect());
    }

    /**
     * The sheet's field, measured rather than assumed (#486): the widest
     * supported field at which every moon not behind Jupiter is drawn on
     * the A4 sheet itself, at its own scale - the normal minimum field if
     * none wider does.
     */
    public static double sheetField(JovianSystemService jovian) {
        for (double field : SHEET_LADDER) {
            if (onTheSheet(jovian, field).values().stream()
                    .allMatch(d -> d.equals("DRAWN") || d.equals("BEHIND"))) {
                return field;
            }
        }
        return ChartViewState.normalMinimumFieldDegrees();
    }

    /**
     * The Jovian sheet's recording (#486), for the study and the packaged
     * journey: A4, the measured field, centred on Jupiter at the triple
     * transit, the Jovian module as the bodies layer - and its symbols in
     * the reference layer, beneath the stars - exactly as an export from
     * the screen carries them.
     */
    public static juranometria.sheet.SheetRecording jovianSheet(JovianSystemService jovian) {
        juranometria.module.OverlayRegistry registry = sheetModule(jovian);
        SkyPosition centre = jovian.observeJupiter(OSLO.at(SHEET_MOMENT)).astrometricJ2000();
        return juranometria.sheet.ChartSheet.record(Atlas.assembler()::assemble,
                new ChartViewState(centre, sheetField(jovian), 8.0, null, null),
                ChartOptions.DEFAULTS,
                (g, painted, reserved) -> ReferenceInk.paint(g, painted, registry.collect(),
                        ChartPalette.WHITE_PAPER, ENGLISH, reserved),
                (g, painted, reserved) -> {
                    DrawnPage page = DrawnPage.of(painted);
                    ReferenceInk.paintBodies(g, page, registry.collect(),
                            ChartPalette.WHITE_PAPER, ENGLISH, reserved,
                            ReferenceInk.referenceBoxes(page, registry.collect(), ENGLISH,
                                    reserved, structure -> false));
                },
                juranometria.render.ChartRenderer.ReferenceLayer.NONE,
                juranometria.sheet.PaperSize.A4, ENGLISH, java.util.Set.of());
    }

    private static void sheet() throws Exception {
        p("## The exported sheet, at a measured scale");
        p("");
        p("The existing two-body sheet is 42° wide; the four moons do not resolve there."
                + " The sheet's field is measured instead: at the triple transit (2026-12-11"
                + " 22:45 UTC, Oslo), on the A4 sheet's own page, each supported field from"
                + " the widest, and the first at which every moon not behind Jupiter is"
                + " drawn. Moons in front of Jupiter need a disc that can hold their marks,"
                + " which the sheet reaches only at the normal minimum field.");
        p("");
        p("| field | Io | Europa | Ganymede | Callisto |");
        p("|---:|---|---|---|---|");
        double[] all = java.util.Arrays.copyOf(SHEET_LADDER, SHEET_LADDER.length + 1);
        all[SHEET_LADDER.length] = ChartViewState.normalMinimumFieldDegrees();
        for (double field : all) {
            Map<String, String> d = onTheSheet(service, field);
            StringBuilder row = new StringBuilder("| " + trim(field) + "° |");
            for (JovianSystemService.Moon m : JovianSystemService.Moon.values()) {
                row.append(' ').append(d.getOrDefault(JovianModule.identityOf(m), "-")
                        .toLowerCase(Locale.ROOT).replace('_', ' ')).append(" |");
            }
            p(row.toString());
        }
        double chosen = sheetField(service);
        p("");
        juranometria.sheet.SheetRecording sheet = jovianSheet(service);
        int dpi = juranometria.sheet.PngSheetWriter.DEFAULT_RESOLUTION;
        byte[] png = juranometria.sheet.PngSheetWriter.write(sheet, dpi);
        java.nio.file.Files.write(new File(DIR, "sheet-a4-jovian.png").toPath(), png);
        byte[] pdf = juranometria.sheet.PdfSheetWriter.write(sheet);
        java.nio.file.Files.write(new File(DIR, "sheet-a4-jovian.pdf").toPath(), pdf);
        p(String.format(Locale.ROOT, "**The sheet: A4, %s°**, centred on Jupiter, as"
                + " `sheet-a4-jovian.png` (%d dpi, %d × %d px) and `sheet-a4-jovian.pdf`."
                + " On paper Jupiter's true disc is %.1f mm and each moon's mark %.1f mm;"
                + " the screen and the exported sheet draw the same Jupiter centre, pole,"
                + " figure, moon positions, states and names - the packaged journey"
                + " *jovian sheet OK* holds them pixel for pixel.",
                trim(chosen), dpi,
                juranometria.sheet.PngSheetWriter.widePixels(juranometria.sheet.PaperSize.A4, dpi),
                juranometria.sheet.PngSheetWriter.highPixels(juranometria.sheet.PaperSize.A4, dpi),
                service.observeJupiter(OSLO.at(SHEET_MOMENT)).equatorialDiameterArcseconds()
                        / (chosen * 3600.0) * juranometria.sheet.PaperSize.A4.chartWidePoints()
                        * 25.4 / 72.0,
                JovianModule.MOON_MARK_PX * 25.4 / 72.0));
        p("");
    }

    private static String trim(double field) {
        return field == Math.rint(field) ? String.valueOf((long) field) : String.valueOf(field);
    }

    private static ReferenceInk.BodyPlacement find(List<ReferenceInk.BodyPlacement> all,
                                                   String identity) {
        for (ReferenceInk.BodyPlacement b : all) {
            if (b.identity().equals(identity)) {
                return b;
            }
        }
        return null;
    }

    private static String name(MoonPlace m) {
        String n = m.moon().name();
        return n.charAt(0) + n.substring(1).toLowerCase(Locale.ROOT);
    }
}
