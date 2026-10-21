package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.saturnchart.SaturnModule;
import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.SaturnObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Saturn on production pages (Sprint 53, issue #579, the first pull
 * request of the owner's rulings on #578): the production
 * {@link ChartComponent} over the bundled catalogue with the Saturn
 * module attached, drawing Saturn's figure - globe and rings - where the
 * accepted service puts it, at the fields of the ladder and the two
 * closer fields the controller reaches, at the contract's named ring
 * openings, below a drawn horizon, and on an exported A4 sheet. Nothing
 * here is a mock-up: every page is what the application paints.
 *
 * <p>Deterministic: the bundled packs, the contract's named instants
 * and instants derived from the service by stated rules, Oslo at sea
 * level, {@code Locale.ROOT} numbers. The portable report goes to stdout
 * ({@code make saturn-on-the-chart-study}); where each page's names
 * found room, which this desktop's fonts decide, goes to
 * {@code platform.md}.
 */
public final class SaturnOnTheChartStudyMain {

    private static final File DIR = new File("docs/studies/saturn-on-the-chart");
    /** The contract's card instant (#574): the 2026 Oslo day nearest the Earth. */
    static final Instant CARD = Instant.parse("2026-10-05T00:00:00Z");
    /** The contract's named ring-plane crossing by the Earth (service-accuracy.md). */
    static final Instant EARTH_CROSSING = Instant.parse("2025-03-23T17:51:00Z");
    private static final Observer OSLO = new Observer(59.91, 10.75, CARD);
    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();
    private static SaturnSystemService service;

    private SaturnOnTheChartStudyMain() {
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
        service = SaturnSystemService.load();
        PlatformEvidence.preface(PLATFORM,
                "Saturn's name on the production pages, as this desktop's fonts place it",
                "Sprint 53, issue #579.");
        q("## Names on the pages");
        q("");
        p("# Saturn on the chart: production pages");
        p("");
        p("Sprint 53, issue #579 - the figure, the first of #579's two pull requests under"
                + " the owner's rulings on #578. Each page is the production `ChartComponent`"
                + " over the bundled catalogue with the Saturn module attached and switched"
                + " on: Saturn's globe and rings at their true size where the rings' major"
                + " axis is at least the 6 px minimum, otherwise the 6 px cartographic"
                + " symbol; the rings' far half behind the globe and the near half over it;"
                + " a line when edge-on; dimmed when the unlit face is seen; dimmed below a"
                + " drawn horizon. The closer fields are reached as the application reaches"
                + " them, through the controller's zoom in at the floor. Regenerate with"
                + " `make saturn-on-the-chart-study`.");
        p("");
        scales();
        // The card's instant, the ladder, then the closer fields.
        page("card-36", "The card's instant, 36° field", OSLO.at(CARD), 36.0, NORSK,
                ChartPalette.WHITE_PAPER, false, 0);
        page("card-8", "The card's instant, 8° field", OSLO.at(CARD), 8.0, ENGLISH,
                ChartPalette.WHITE_PAPER, false, 0);
        page("card-3-dark", "The card's instant, 3° field, black sky", OSLO.at(CARD), 3.0,
                ENGLISH, ChartPalette.BLACK_SKY, false, 0);
        page("card-1", "The card's instant, 1° field", OSLO.at(CARD), 1.0, ENGLISH,
                ChartPalette.WHITE_PAPER, false, 0);
        page("card-0.5", "The card's instant, the 0.5° closer field", OSLO.at(CARD), 1.0, NORSK,
                ChartPalette.WHITE_PAPER, false, 1);
        page("card-0.25-dark", "The card's instant, the 0.25° closer field, black sky",
                OSLO.at(CARD), 1.0, ENGLISH, ChartPalette.BLACK_SKY, false, 2);
        // The ring openings: widest north and south (2000-2100, weekly
        // geocentric, the #578 study's rule), edge-on, the unlit face.
        Instant north = widestOpening(true);
        Instant south = widestOpening(false);
        Instant unlit = unlitFace();
        page("open-north-1", "The rings widest to the north face, " + when(north) + ", 1° field",
                OSLO.at(north), 1.0, ENGLISH, ChartPalette.WHITE_PAPER, false, 0);
        page("open-north-0.25", "The rings widest to the north face, " + when(north)
                + ", the 0.25° closer field", OSLO.at(north), 1.0, ENGLISH,
                ChartPalette.WHITE_PAPER, false, 2);
        page("open-south-0.25-dark", "The rings widest to the south face, " + when(south)
                + ", the 0.25° closer field, black sky", OSLO.at(south), 1.0, NORSK,
                ChartPalette.BLACK_SKY, false, 2);
        page("edge-on-0.25", "The Earth's ring-plane crossing, " + when(EARTH_CROSSING)
                + ", the 0.25° closer field", OSLO.at(EARTH_CROSSING), 1.0, NORSK,
                ChartPalette.WHITE_PAPER, false, 2);
        page("unlit-0.25", "The unlit face seen, " + when(unlit) + ", the 0.25° closer field",
                OSLO.at(unlit), 1.0, ENGLISH, ChartPalette.WHITE_PAPER, false, 2);
        // Below a drawn horizon, and with the horizon hidden.
        Instant low = belowTheHorizon();
        page("horizon-drawn-8", "Saturn below the drawn horizon, " + when(low) + ", 8° field",
                OSLO.at(low), 8.0, ENGLISH, ChartPalette.WHITE_PAPER, true, 0);
        page("horizon-hidden-8", "The same page with the horizon hidden", OSLO.at(low), 8.0,
                NORSK, ChartPalette.WHITE_PAPER, false, 0);
        sheet();
        System.out.print(REPORT);
        System.out.flush();
        PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
    }

    private static String when(Instant t) {
        return t.toString().replace("T", " ").replace(":00Z", " UTC");
    }

    private static Instant widestOpening(boolean north) {
        Instant best = null;
        double extreme = 0;
        for (Instant t = Instant.parse("2000-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2101-01-01T00:00:00Z")); t = t.plus(Duration.ofDays(7))) {
            double b = service.observeSaturnGeocentric(t).ringOpeningDegrees();
            if (north ? b > extreme : b < extreme) {
                extreme = b;
                best = t;
            }
        }
        return best;
    }

    /** The 2025 day between the Earth's and the Sun's crossings where the unlit face is seen most openly. */
    private static Instant unlitFace() {
        Instant best = null;
        double widest = -1;
        for (Instant t = Instant.parse("2025-03-01T00:00:00Z");
                t.isBefore(Instant.parse("2025-06-01T00:00:00Z")); t = t.plus(Duration.ofDays(1))) {
            SaturnObservation s = service.observeSaturnGeocentric(t);
            if (s.unlitFaceSeen() && Math.abs(s.ringOpeningDegrees()) > widest) {
                widest = Math.abs(s.ringOpeningDegrees());
                best = t;
            }
        }
        return best;
    }

    /** The first December 2026 evening ten-minute step at Oslo with Saturn 1° to 4° below the horizon. */
    private static Instant belowTheHorizon() {
        for (Instant t = Instant.parse("2026-12-01T12:00:00Z");; t = t.plus(Duration.ofMinutes(10))) {
            double alt = service.observeSaturn(OSLO.at(t)).horizontal().altitudeDegrees();
            if (alt < -1.0 && alt > -4.0) {
                return t;
            }
        }
    }

    /** A page of the application's, through its component, module and controller. */
    private static void page(String name, String title, Observer observer, double field,
                             PageText words, ChartPalette palette, boolean horizon,
                             int closerSteps) throws Exception {
        SaturnObservation saturn = service.observeSaturn(observer);
        ChartComponent[] holder = new ChartComponent[1];
        ChartViewController[] controller = new ChartViewController[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(900, 700);
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            controller[0] = new ChartViewController();
            controller[0].onChange(holder[0]::setViewState);
            holder[0].setCloserFieldBodies(java.util.Set.of(SolarSystemModule.MERCURY,
                    SolarSystemModule.VENUS, juranometria.jovianchart.JovianModule.JUPITER,
                    SaturnModule.SATURN));
            controller[0].offerCloserFields(holder[0]::offersCloserField);
            holder[0].onPageContentsChange(controller[0]::closerOfferChanged);
            controller[0].recenter(saturn.astrometricJ2000(), field);
            MeridianModule meridian = new MeridianModule(observer);
            meridian.showing(horizon, horizon, false);
            if (horizon) {
                holder[0].overlays().offer(MeridianModule.ID, meridian::contributedGeometry);
            }
            SaturnModule module = new SaturnModule(() -> observer, () -> service,
                    meridian::horizonShowing);
            module.showing(true);
            holder[0].overlays().offer(SaturnModule.ID, module::contributedGeometry);
        });
        for (int i = 0; i < closerSteps; i++) {
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(controller[0]::zoomIn);
        }
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
        ViewportMapping mapping = new ViewportMapping(page);
        p("## " + title);
        p("");
        p("![](" + name + ".png)");
        p("");
        p(String.format(Locale.ROOT, "%s, %s; the page's field %s°. B = %+.2f° (%s face%s),"
                + " B′ = %+.2f°; Saturn %.1f° %s the horizon.",
                palette == ChartPalette.BLACK_SKY ? "Black sky" : "White paper",
                words == NORSK ? "Norwegian" : "English",
                trim(chart.currentScene().viewport().fieldWidthDegrees()),
                saturn.ringOpeningDegrees(), saturn.ringOpeningDegrees() >= 0 ? "north" : "south",
                saturn.unlitFaceSeen() ? ", the unlit face seen" : "", saturn.sunRingOpeningDegrees(),
                Math.abs(saturn.horizontal().altitudeDegrees()),
                saturn.horizontal().altitudeDegrees() < 0 ? "below" : "above"));
        p("");
        ReferenceInk.BodyPlacement placed = find(drawn, SaturnModule.SATURN);
        if (placed != null) {
            PixelPoint expected = mapping.toPixel(page.projection()
                    .project(saturn.astrometricJ2000()).orElseThrow());
            java.awt.geom.Rectangle2D b = placed.disc().getBounds2D();
            p(String.format(Locale.ROOT, "Saturn drawn at (%.1f, %.1f), %.4f px off the service's"
                    + " place; %s, its ink %.1f × %.1f px; its name %s%s.",
                    placed.centre().x(), placed.centre().y(),
                    Math.hypot(placed.centre().x() - expected.x(), placed.centre().y() - expected.y()),
                    placed.symbol() ? "the 6 px cartographic symbol" : "the true figure",
                    b.getWidth(), b.getHeight(), placed.box() == null ? "refused" : "placed",
                    placed.belowHorizon() ? "; below the horizon, dimmed" : ""));
        } else {
            p("Saturn off the page: nothing drawn.");
        }
        p("");
        String said = chart.getAccessibleContext().getAccessibleDescription();
        int from = said.indexOf("Saturn");
        p("Spoken: " + (from < 0 ? "(Saturn not on the page)" : said.substring(from)));
        p("");
        q("- **" + title + "** (`" + name + ".png`): "
                + (placed == null ? "Saturn off the page."
                        : placed.name() + (placed.box() == null ? " refused." : " placed.")));
    }

    /** What the figure is at each field of the ladder and the closer fields, at the card's instant. */
    private static void scales() throws Exception {
        p("## What the figure is at each scale");
        p("");
        p("On the 900 × 700 page at the card's instant: the mark the page draws, and the"
                + " drawn ink's width and height, by the ruled switch - the true figure where"
                + " the rings' major axis is at least 6 px, the symbol otherwise.");
        p("");
        p("| field | mark | ink px (width × height) |");
        p("|---:|---|---|");
        for (double field : new double[] {36.0, 24.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0,
                ChartViewState.normalMinimumFieldDegrees(), 0.5, 0.25}) {
            int closer = field < ChartViewState.normalMinimumFieldDegrees()
                    ? (field == 0.5 ? 1 : 2) : 0;
            ChartComponent[] holder = new ChartComponent[1];
            ChartViewController[] controller = new ChartViewController[1];
            SaturnObservation saturn = service.observeSaturn(OSLO);
            SwingUtilities.invokeAndWait(() -> {
                holder[0] = new ChartComponent(Atlas.assembler(), ENGLISH);
                holder[0].setSize(900, 700);
                controller[0] = new ChartViewController();
                controller[0].onChange(holder[0]::setViewState);
                holder[0].setCloserFieldBodies(java.util.Set.of(SaturnModule.SATURN));
                controller[0].offerCloserFields(holder[0]::offersCloserField);
                holder[0].onPageContentsChange(controller[0]::closerOfferChanged);
                controller[0].recenter(saturn.astrometricJ2000(),
                        closer > 0 ? ChartViewState.normalMinimumFieldDegrees() : field);
                SaturnModule module = new SaturnModule(() -> OSLO, () -> service, () -> false);
                module.showing(true);
                holder[0].overlays().offer(SaturnModule.ID, module::contributedGeometry);
            });
            for (int i = 0; i < closer; i++) {
                SwingUtilities.invokeAndWait(() -> { });
                SwingUtilities.invokeAndWait(controller[0]::zoomIn);
            }
            SwingUtilities.invokeAndWait(() -> { });
            DrawnPage page = DrawnPage.of(holder[0].currentScene());
            ReferenceInk.BodyPlacement s = find(ReferenceInk.bodyPlacements(page,
                    holder[0].overlays().collect(), ChartPalette.WHITE_PAPER, ENGLISH, List.of()),
                    SaturnModule.SATURN);
            java.awt.geom.Rectangle2D b = s == null ? null : s.disc().getBounds2D();
            p(String.format(Locale.ROOT, "| %s° | %s | %s |",
                    trim(page.scene().viewport().fieldWidthDegrees()),
                    s == null ? "off the page" : s.symbol() ? "6 px symbol" : "true figure",
                    b == null ? "-" : String.format(Locale.ROOT, "%.1f × %.1f", b.getWidth(), b.getHeight())));
        }
        p("");
    }

    /**
     * The Saturn sheet's recording (#579, ruling 7 on #578): A4, the 1°
     * field, centred on Saturn at the card's instant, the Saturn module as
     * the bodies layer - and its symbol in the reference layer, beneath
     * the stars - exactly as an export from the screen carries them.
     */
    public static juranometria.sheet.SheetRecording saturnSheet(SaturnSystemService saturnSystem) {
        Observer at = OSLO.at(CARD);
        juranometria.module.OverlayRegistry registry = new juranometria.module.OverlayRegistry();
        SaturnModule module = new SaturnModule(() -> at, () -> saturnSystem, () -> false);
        module.showing(true);
        registry.offer(SaturnModule.ID, module::contributedGeometry);
        SkyPosition centre = saturnSystem.observeSaturn(at).astrometricJ2000();
        return juranometria.sheet.ChartSheet.record(Atlas.assembler()::assemble,
                new ChartViewState(centre, ChartViewState.normalMinimumFieldDegrees(), 8.0, null, null),
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
        p("## The exported sheet");
        p("");
        juranometria.sheet.SheetRecording sheet = saturnSheet(service);
        int dpi = juranometria.sheet.PngSheetWriter.DEFAULT_RESOLUTION;
        byte[] png = juranometria.sheet.PngSheetWriter.write(sheet, dpi);
        java.nio.file.Files.write(new File(DIR, "sheet-a4-saturn.png").toPath(), png);
        byte[] pdf = juranometria.sheet.PdfSheetWriter.write(sheet);
        java.nio.file.Files.write(new File(DIR, "sheet-a4-saturn.pdf").toPath(), pdf);
        SaturnObservation s = service.observeSaturn(OSLO.at(CARD));
        double field = ChartViewState.normalMinimumFieldDegrees();
        p(String.format(Locale.ROOT, "**The sheet: A4, %s°**, centred on Saturn at the card's"
                + " instant, as `sheet-a4-saturn.png` (%d dpi, %d × %d px) and"
                + " `sheet-a4-saturn.pdf`. The figure is drawn in page units, as the screen"
                + " draws it: the rings' major axis is %.2f mm on the sheet and the globe"
                + " %.2f mm; the screen and the exported sheet draw the same centre, pole,"
                + " opening, figure and name (ruling 7 on #578).",
                trim(field), dpi,
                juranometria.sheet.PngSheetWriter.widePixels(juranometria.sheet.PaperSize.A4, dpi),
                juranometria.sheet.PngSheetWriter.highPixels(juranometria.sheet.PaperSize.A4, dpi),
                2.0 * s.ringSemiMajorArcseconds(s.ringRadiiKm().get(3)) / (field * 3600.0)
                        * juranometria.sheet.PaperSize.A4.chartWidePoints() * 25.4 / 72.0,
                s.equatorialDiameterArcseconds() / (field * 3600.0)
                        * juranometria.sheet.PaperSize.A4.chartWidePoints() * 25.4 / 72.0));
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
}
