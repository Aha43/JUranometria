package juranometria.tool;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.render.ChartFurniture;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.placeandtime.PlaceAndTimeFurniture;
import juranometria.ui.placeandtime.UpdateToNow;

/**
 * Place and Time's box as production draws it (Sprint 51, issue #259,
 * built to the rulings on #556 - docs/decisions/chart-instant.md).
 *
 * <p>Where the #556 study drew a proposed box over finished pages, every
 * box here is the chart's own: a production {@code ChartComponent} under
 * a production {@code ChartModuleHost}, with the Place and Time module
 * attached and its box offered exactly as the application offers it,
 * the corner allocated, the box laid out, inked and reserved by the
 * renderer. The study's pages are drawn again - sparse, dense, a body
 * alone, wide, narrow, narrow with the magnitude key, polar, the globe
 * and a letterboxed page on both application themes' bands - with the
 * exported sheet, which carries no control. The report records what is
 * the same on every desktop: which pages carry the box, in which corner,
 * and what the box says; the box's size and what it displaced are this
 * desktop's fonts' answer and stand in {@code platform.md}.
 *
 * <p>The sample instant is the cited 2026 greatest eastern elongation of
 * Venus, {@code docs/studies/venus/venus-events.txt}, as the study's;
 * the observer is Oslo at sea level.
 */
public final class ChartInstantFurnitureMain {

    private static final File DIR = new File("docs/studies/chart-instant-furniture");
    private static final Path VENUS_EVENTS = Path.of("docs/studies/venus/venus-events.txt");
    private static final double OSLO_LAT = 59.91;
    private static final double OSLO_LON = 10.75;

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");

    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();

    private record Case(String name, String caption, ChartViewState state, int width,
                        int height, ChartPalette palette, boolean key, boolean norwegian,
                        boolean lines, boolean venus, Color band) {
    }

    private ChartInstantFurnitureMain() {
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
        Instant instant = elongation();
        Observer oslo = new Observer(OSLO_LAT, OSLO_LON, instant);
        SolarSystemService service = SolarSystemService.load();
        PlatformEvidence.preface(PLATFORM,
                "Place and Time's box as production draws it, as this desktop's fonts size it",
                "Sprint 51, issue #259.");

        p("# Place and Time's box, as built");
        p("");
        p("Sprint 51, issue #259, to the rulings on #556 (`docs/decisions/chart-instant.md`)."
                + " Every page below is the production `ChartComponent` under a production"
                + " `ChartModuleHost`, the Place and Time module attached and its box offered as"
                + " the application offers it; the corner, the layout, the ink and every"
                + " collision are the renderer's. The sample instant is Venus's cited 2026"
                + " greatest eastern elongation (" + PlaceAndTimeFurniture.shown(instant)
                + " UTC, from `docs/studies/venus/venus-events.txt`), the observer Oslo at sea"
                + " level. The box's size and what it displaced are in `platform.md`."
                + " Regenerate with `make chart-instant-furniture`.");
        p("");
        p("## What the box says");
        p("");
        p("| line | English | Norwegian |");
        p("|---|---|---|");
        var en = PlaceAndTimeFurniture.box(oslo, EN);
        var nb = PlaceAndTimeFurniture.box(oslo, NB);
        p("| heading | " + en.heading() + " | " + nb.heading() + " |");
        for (int i = 0; i < en.lines().size(); i++) {
            p("| " + (i + 1) + " | " + en.lines().get(i) + " | " + nb.lines().get(i) + " |");
        }
        p("| control on screen | " + en.action().label() + " | " + nb.action().label() + " |");
        p("| on paper instead | " + en.action().paperLine() + " | "
                + nb.action().paperLine() + " |");
        p("");
        p("Spoken with the page: \"" + en.spoken() + "\"");
        p("");
        p("## The pages");
        p("");
        q("## What each box displaced");
        q("");
        q("| page | box (px) | corner | grid figures yielded | names yielded |");
        q("|---|---|---|---:|---:|");
        for (Case c : cases(oslo, service)) {
            page(c, oslo, service);
        }
        q("");
        sheet(oslo);

        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        System.out.flush();
        PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
    }

    private static List<Case> cases(Observer oslo, SolarSystemService service) {
        Color light = band(new com.formdev.flatlaf.FlatLightLaf());
        Color dark = band(new com.formdev.flatlaf.FlatDarkLaf());
        SkyPosition m31 = ChartViewState.DEFAULT.centre();
        SkyPosition south = new SkyPosition(64.0, -28.0);
        SkyPosition venus = ((juranometria.solar.VenusService.VenusObservation)
                service.observe(SolarSystemService.Body.VENUS, oslo)).astrometricJ2000();
        List<Case> out = new ArrayList<>();
        out.add(new Case("default-8", "The released opening page, nothing drawn for the"
                + " instant: no box, the page as released.", ChartViewState.DEFAULT, 900, 700,
                ChartPalette.WHITE_PAPER, false, false, false, false, light));
        out.add(new Case("sparse-lines-8", "Sparse: Eridanus on Oslo's meridian, the meridian"
                + " and the mathematical horizon crossing the page.",
                new ChartViewState(south, 8.0, 8.0), 900, 700, ChartPalette.WHITE_PAPER, false,
                false, true, false, light));
        out.add(new Case("sparse-lines-8-dark", "The same on the black sky.",
                new ChartViewState(south, 8.0, 8.0), 900, 700, ChartPalette.BLACK_SKY, false,
                false, true, false, light));
        out.add(new Case("sparse-lines-8-nb", "The same in Norwegian.",
                new ChartViewState(south, 8.0, 8.0), 900, 700, ChartPalette.WHITE_PAPER, false,
                true, true, false, light));
        out.add(new Case("dense-lines-8", "Dense: the Milky Way in Sagittarius, the lines on"
                + " but off this page - the box is drawn all the same (ruling 1).",
                new ChartViewState(new SkyPosition(270.0, -25.0), 8.0, 8.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, false, true, false, light));
        out.add(new Case("venus-3", "Venus alone at 3°, the lines off: a body makes the page"
                + " depend on its instant.", new ChartViewState(venus, 3.0, 8.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, false, false, true, light));
        out.add(new Case("wide-lines-60", "Wide: a 60° overview page with the lines on.",
                new ChartViewState(new SkyPosition(30.0, 40.0), 60.0, 5.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, false, true, false, light));
        out.add(new Case("narrow-lines-8", "Narrow: a 520 px window - the lower right meets"
                + " the title block, so the upper right.", new ChartViewState(m31, 8.0, 8.0),
                520, 700, ChartPalette.WHITE_PAPER, false, false, true, false, light));
        out.add(new Case("narrow-key-lines-8", "Narrow with the stellar-magnitude key holding"
                + " the upper right: the upper left.", new ChartViewState(m31, 8.0, 8.0), 520,
                700, ChartPalette.WHITE_PAPER, true, false, true, false, light));
        out.add(new Case("polar-lines-8", "Polar: two degrees from the celestial pole.",
                new ChartViewState(new SkyPosition(0.0, 88.0), 8.0, 8.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, false, true, false, light));
        out.add(new Case("globe-lines-180", "The celestial globe with the lines on.",
                new ChartViewState(new SkyPosition(30.0, 40.0), 180.0, 5.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, false, true, false, light));
        out.add(new Case("letterbox-lines-120", "Letterboxed: a 120° page in a 600 × 1900"
                + " window, on the light theme's band.", new ChartViewState(m31, 120.0, 4.0),
                600, 1900, ChartPalette.WHITE_PAPER, false, false, true, false, light));
        out.add(new Case("letterbox-lines-120-dark-theme", "The same on the dark theme's band:"
                + " the band is the application's, the page and its box the chart's.",
                new ChartViewState(m31, 120.0, 4.0), 600, 1900, ChartPalette.BLACK_SKY, false,
                false, true, false, dark));
        return out;
    }

    /** A theme's letterbox band, read from its defaults without installing it. */
    private static Color band(javax.swing.LookAndFeel theme) {
        Color panel = theme.getDefaults().getColor("Panel.background");
        return new Color(panel.getRGB());
    }

    private static void page(Case c, Observer oslo, SolarSystemService service)
            throws Exception {
        InterfaceText said = c.norwegian() ? NB : EN;
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartComponent chart = new ChartComponent(Atlas.assembler(), PageText.in(said));
            // Stated, so the band does not depend on whichever main ran before.
            chart.setBackground(c.band());
            chart.setSize(c.width(), c.height());
            chart.setChartOptions(withKey(c.key()).withPalette(c.palette()));
            chart.setViewState(c.state());
            ChartModuleHost host = new ChartModuleHost(chart, new SelectionModel(), r -> { });
            MeridianModule meridian = host.attach(new MeridianModule(oslo));
            meridian.showing(c.lines(), c.lines(), c.lines());
            boolean[] bodies = {false};
            if (c.venus()) {
                SolarSystemModule solar = host.attach(new SolarSystemModule(
                        () -> meridian.observer(), () -> service, () -> false));
                solar.venusShowing(true);
                bodies[0] = true;
            }
            PlaceAndTimeFurniture.offer(meridian, () -> bodies[0],
                    new UpdateToNow(meridian, () -> oslo.instant()), said);
            holder[0] = chart;
        });
        SwingUtilities.invokeAndWait(() -> { });
        ChartComponent chart = holder[0];
        BufferedImage image = new BufferedImage(c.width(), c.height(),
                BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        ImageIO.write(image, "png", new File(DIR, c.name() + ".png"));
        List<ChartComponent.DrawnFurniture> drawn = chart.renderedFurniture();
        p("### " + c.name());
        p("");
        p(c.caption() + (drawn.isEmpty() ? " No box."
                : " The box stands in the " + drawn.get(0).placed().corner().token()
                        .replace('-', ' ') + ".")
                + (chart.pageOffsetY() > 0 ? " The page is "
                        + chart.currentScene().viewport().heightPx() + " px tall in a "
                        + c.height() + " px window." : ""));
        p("");
        p("![](" + c.name() + ".png)");
        p("");
        if (drawn.isEmpty()) {
            return;
        }
        ChartFurniture.Placed placed = drawn.get(0).placed();
        ChartScene scene = chart.currentScene();
        ChartOptions options = chart.drawnOptions();
        FontMetrics metrics = ChartRenderer.TextMetrics.offscreen().labels();
        ChartRenderer without = new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT,
                PageText.in(said));
        ChartRenderer with = new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT,
                PageText.in(said));
        with.setFurniture(() -> List.of(placed.furniture()));
        int figures = without.gridLabelPlacements(metrics, scene, options).size()
                - with.gridLabelPlacements(metrics, scene, options).size();
        int names = placedNames(without, scene, options) - placedNames(with, scene, options);
        Rectangle box = placed.covers();
        q("| " + c.name() + " | " + box.width + "×" + box.height + " | "
                + placed.corner().token() + " | " + figures + " | " + names + " |");
    }

    private static int placedNames(ChartRenderer renderer, ChartScene scene,
                                   ChartOptions options) {
        int placed = 0;
        for (var text : renderer.textPlacements(ChartRenderer.TextMetrics.offscreen(), scene,
                options)) {
            if (!text.omitted()) {
                placed++;
            }
        }
        return placed;
    }

    /** The exported sheet: the box with no control, the frozen sentence in its place. */
    private static void sheet(Observer oslo) throws Exception {
        ChartComponent chart = new ChartComponent(Atlas.assembler(), PageText.in(EN));
        chart.setSize(900, 700);
        ChartViewState state = new ChartViewState(new SkyPosition(64.0, -28.0), 8.0, 8.0);
        chart.setViewState(state);
        ChartModuleHost host = new ChartModuleHost(chart, new SelectionModel(), r -> { });
        MeridianModule meridian = host.attach(new MeridianModule(oslo));
        meridian.showing(true, true, true);
        PlaceAndTimeFurniture.offer(meridian, () -> false,
                new UpdateToNow(meridian, () -> oslo.instant()), EN);
        juranometria.sheet.SheetRecording sheet = juranometria.sheet.ChartSheet.record(
                Atlas.assembler()::assemble, state, ChartOptions.DEFAULTS,
                juranometria.ui.SheetInk.reference(chart), ChartRenderer.ReferenceLayer.NONE,
                ChartRenderer.ReferenceLayer.NONE, juranometria.sheet.PaperSize.A4,
                PageText.in(EN), java.util.Set.of(), chart.paperFurniture());
        Files.write(new File(DIR, "sheet-a4.png").toPath(),
                juranometria.sheet.SheetWriters.write(sheet,
                        juranometria.sheet.SheetFormat.PNG, 96));
        p("## On paper");
        p("");
        p("The sparse page exported to A4: the box carries no control - it cannot be pressed"
                + " - and says *" + PlaceAndTimeFurniture.box(oslo, EN).action().paperLine()
                + "* in its place. The sheet's own description ends: \""
                + PlaceAndTimeFurniture.box(oslo, EN).described() + "\"");
        p("");
        p("![](sheet-a4.png)");
        p("");
    }

    private static ChartOptions withKey(boolean key) {
        ChartOptions o = ChartOptions.DEFAULTS;
        return new ChartOptions(o.deepSkyObjects(), o.deepSkyLabels(),
                o.constellationFigures(), o.constellationBoundaries(),
                o.constellationNames(), o.starNames(), o.bayerLetters(),
                o.flamsteedNumbers(), o.equatorialGrid(), o.titleBlock(), key,
                o.galaxies(), o.openClusters(), o.globularClusters(), o.nebulae(),
                o.planetaryNebulae(), o.palette());
    }

    /** Venus's cited 2026 greatest eastern elongation, read as the study reads it. */
    private static Instant elongation() throws java.io.IOException {
        for (String line : Files.readAllLines(VENUS_EVENTS,
                java.nio.charset.StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            if (f[0].equals("2026-greatest-elongation-east")) {
                String iso = f[1].length() == 17 ? f[1].substring(0, 16) + ":00Z" : f[1];
                return Instant.parse(iso);
            }
        }
        throw new IllegalStateException("no 2026-greatest-elongation-east in " + VENUS_EVENTS);
    }
}
