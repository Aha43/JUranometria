package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.FieldWidthText;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFileName;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;
import juranometria.sky.Observer;
import juranometria.solar.MercuryService.MercuryObservation;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.VenusService.VenusObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReferenceInk;
import juranometria.ui.SheetInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * The closer fields' foundations, proven on production pages before any
 * surface offers them (#543, part of #481; ruled on #539, ruling 5).
 *
 * <p>Every page is the production {@code ChartComponent} over the bundled
 * catalogue, set to a state at 1°, 0.5° or 0.25° - a state the view state
 * holds and, since #544, zoom in offers where Mercury, Venus, Mars, Jupiter, Saturn or Uranus stands. The report records what is the
 * same on every desktop: the field's words on every surface, the grid's
 * steps and line counts, the catalogue's answer at the page's own field,
 * each body's mark, the zoom back to the ladder and the exported sheet's
 * statement. Where names and grid figures found room is this desktop's
 * fonts' answer and stands in {@code platform.md}. The instants are the
 * cited rows in {@code docs/studies/mercury/mercury-events.txt} and
 * {@code docs/studies/venus/venus-events.txt}.
 */
public final class CloserFieldsStudyMain {

    private static final File DIR = new File("docs/studies/closer-fields");
    private static final Path MERCURY_EVENTS =
            Path.of("docs/studies/mercury/mercury-events.txt");
    private static final Path VENUS_EVENTS =
            Path.of("docs/studies/venus/venus-events.txt");

    private static final int W = 900;
    private static final int H = 700;
    private static final double OSLO_LAT = 59.91;
    private static final double OSLO_LON = 10.75;
    private static final SkyPosition PLEIADES = new SkyPosition(56.75, 24.12);
    private static final double[] FIELDS = {1.0, 0.5, 0.25};

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    private static final PageText ENGLISH = PageText.in(EN);
    private static final PageText NORSK = PageText.in(NB);

    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();

    private CloserFieldsStudyMain() {
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
        SolarSystemService service = SolarSystemService.load();
        Map<String, Instant> mercuryEvents = events(MERCURY_EVENTS);
        Map<String, Instant> venusEvents = events(VENUS_EVENTS);
        PlatformEvidence.preface(PLATFORM,
                "Names and grid figures on the closer-field pages, as this desktop's fonts place them",
                "Sprint 50, issue #543.");

        p("# The closer fields: foundations and the offer");
        p("");
        p("Sprint 50, issues #543 and #544 (#481). The four foundations ruling 5 on #539"
                + " requires before any surface offers 0.5° or 0.25° (#543), proven on production"
                + " pages: the production `ChartComponent` over the bundled catalogue, set to a"
                + " state at the field; then the offer itself (#544, section 5): zoom in at the"
                + " floor is the closer step where Mercury, Venus, Mars, Jupiter, Saturn or Uranus, as drawn, lies on the page"
                + " that step would display, through the same controller and component the"
                + " application wires. The"
                + " instants are the cited rows in `docs/studies/mercury/mercury-events.txt` and"
                + " `docs/studies/venus/venus-events.txt`; positions are the services' for Oslo"
                + " at sea level. Where names and grid figures found room is in `platform.md`."
                + " Regenerate with `make closer-fields-study`.");
        p("");

        words();
        grid();
        catalogue();
        Instant elongation = mercuryEvents.get("2026-greatest-elongation-east-1");
        p("## 4. Mercury, Venus and Jupiter at the closer fields");
        p("");
        bodies("4.1 Mercury at its 2026 greatest eastern elongation", "mercury-elongation",
                elongation, service, Body.MERCURY, false, "");
        Instant inferior = venusEvents.get("2026-inferior-conjunction");
        Instant crescent = inferior.minus(Duration.ofDays(21));
        bodies("4.2 Venus three weeks before its 2026 inferior conjunction", "venus-crescent",
                crescent, service, Body.VENUS, false, "");
        bodies("4.3 The 2019 transit of Mercury, greatest transit", "transit-2019",
                mercuryEvents.get("2019-transit"), service, Body.MERCURY, true,
                "For #544: at 0.25° the page lies wholly inside the Sun's disc - its ring is"
                        + " off the page and its ground is the page's - so what reads is Mercury"
                        + " and the Sun's centre dot, capped by #541; nothing on the page itself"
                        + " says the reader is looking at the Sun.");
        jupiter();
        offered(elongation, service);
        jupiterOffered();
        sheet(elongation, service);
        jupiterSheet();

        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        System.out.flush();
        PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
    }

    // ---- 1. the field's words -------------------------------------------------------

    private static void words() throws Exception {
        p("## 1. The field's words on every surface");
        p("");
        p("The ladder's forms are unchanged - one decimal in the title block and the spoken"
                + " description, whole degrees in the toolbar, the export's description and its"
                + " file name - and a closer field prints to its own precision in all of them"
                + " (`FieldWidthText`). The Pleiades, V 8.0.");
        p("");
        p("| field | title block (en) | title block (nb-NO) | spoken | toolbar | export description"
                + " | file name | view report |");
        p("|---:|---|---|---|---|---|---|---|");
        for (double field : FIELDS) {
            ChartViewState state = new ChartViewState(PLEIADES, field, 8.0);
            ChartScene scene = Atlas.assembler().assemble(state, W, H);
            DrawnPage page = DrawnPage.of(scene);
            String projection = page.projectionName();
            String titleEn = ENGLISH.titleFacts(FieldWidthText.title(field), "8.0",
                    ENGLISH.projection(projection));
            String titleNb = NORSK.titleFacts(FieldWidthText.title(field), "8.0",
                    NORSK.projection(projection));
            String spoken = sentence(page.describe(ENGLISH), "Field ");
            String readout = EN.say("toolbar.readout", FieldWidthText.whole(field), "8.0");
            SheetRecording sheet = record(state, null);
            String description = sentence(sheet.metadata().description(), "Field ");
            String name = SheetFileName.suggest(state, scene, SheetFormat.PNG);
            p(String.format(Locale.ROOT, "| %s° | %s | %s | %s | %s | %s | `%s` | `field-degrees: %s` |",
                    FieldWidthText.plain(field), first(titleEn), first(titleNb), spoken, readout,
                    description, name, Double.toString(state.fieldWidthDegrees())));
        }
        p("");
    }

    // ---- 2. the grid ------------------------------------------------------------------

    private static void grid() throws Exception {
        p("## 2. The grid's finer steps");
        p("");
        p("The ladder's finest steps (1m of time, 15′ of arc) are a page apart at a closer"
                + " field. Below the floor the same spacing rule (at least "
                + fmt(EquatorialGrid.MINIMUM_SPACING_PX) + " px between lines at the page"
                + " centre) is offered 15s and 30s of time and 2′, 5′ and 10′ of arc first; a"
                + " ladder page never is, so no released grid moves. Meridians and parallels"
                + " counted on the page; their figures' placement is in `platform.md`.");
        p("");
        p("| field | RA step | Dec step | meridians | parallels |");
        p("|---:|---|---|---:|---:|");
        q("## The grid's figures on the Pleiades pages");
        q("");
        for (double field : FIELDS) {
            ChartViewState state = new ChartViewState(PLEIADES, field, 8.0);
            ChartComponent chart = compose(state, ENGLISH, null, null, Set.of());
            BufferedImage image = paint(chart);
            String file = "pleiades-" + FieldWidthText.plain(field) + ".png";
            ImageIO.write(image, "png", new File(DIR, file));
            DrawnPage page = DrawnPage.of(chart.currentScene());
            EquatorialGrid.GridSpec spec = EquatorialGrid.spec(page.scene().viewport());
            EquatorialGrid.Grid grid = EquatorialGrid.gridFor(page);
            p(String.format(Locale.ROOT, "| %s° | %s | %s | %d | %d |",
                    FieldWidthText.plain(field), raStep(spec.raStepDegrees()),
                    decStep(spec.decStepDegrees()), grid.meridians().size(),
                    grid.parallels().size()));
            StringBuilder labels = new StringBuilder();
            for (EquatorialGrid.Label label : grid.labels()) {
                labels.append(labels.length() == 0 ? "" : ", ").append(label.text());
            }
            q(String.format(Locale.ROOT, "- %s°: %s; %d suppressed", FieldWidthText.plain(field),
                    labels.length() == 0 ? "no figure placed" : labels.toString(),
                    grid.suppressedLabels()));
        }
        q("");
        p("");
        for (double field : FIELDS) {
            p("![](pleiades-" + FieldWidthText.plain(field) + ".png)");
            p("");
        }
    }

    // ---- 3. the catalogue -------------------------------------------------------------

    private static void catalogue() {
        p("## 3. The catalogue at the page's own field");
        p("");
        p("The #539 study drew its closer pages from the 1° page's stars. Production asks the"
                + " catalogue for the closer page itself: its own corners plus the catalogue's"
                + " object margin, about its own centre. Stars returned by that query, and those"
                + " of them that land on the page.");
        p("");
        p("| page | field | stars queried | stars on the page |");
        p("|---|---:|---:|---:|");
        for (double field : FIELDS) {
            ChartScene scene = Atlas.assembler().assemble(
                    new ChartViewState(PLEIADES, field, 8.0), W, H);
            p(String.format(Locale.ROOT, "| the Pleiades | %s° | %d | %d |",
                    FieldWidthText.plain(field), scene.stars().size(), onPage(scene)));
        }
        p("");
    }

    // ---- 4. bodies ----------------------------------------------------------------------

    private static void bodies(String title, String stem, Instant when,
                               SolarSystemService service, Body body, boolean withSun,
                               String note)
            throws Exception {
        Observer at = new Observer(OSLO_LAT, OSLO_LON, when);
        SkyPosition place = body == Body.MERCURY
                ? ((MercuryObservation) service.observe(Body.MERCURY, at)).astrometricJ2000()
                : ((VenusObservation) service.observe(Body.VENUS, at)).astrometricJ2000();
        p("### " + title + ", " + when + ", Oslo");
        p("");
        p("Centred on the body; rule M's mark is " + fmt(SolarSystemModule.MERCURY_MINIMUM_MARK_PX)
                + " px, and the true disc takes over where it is the larger."
                + (note.isEmpty() ? "" : " " + note));
        p("");
        p("| field | stars queried | stars on the page | drawn |");
        p("|---:|---:|---:|---|");
        q("## " + title);
        q("");
        for (double field : FIELDS) {
            ChartViewState state = new ChartViewState(place, field, 8.0);
            ChartComponent chart = compose(state, ENGLISH, at, service,
                    withSun ? Set.of(Body.SUN, body) : Set.of(body));
            BufferedImage image = paint(chart);
            String file = stem + "-" + FieldWidthText.plain(field) + ".png";
            ImageIO.write(image, "png", new File(DIR, file));
            StringBuilder drawn = new StringBuilder();
            StringBuilder names = new StringBuilder();
            for (ReferenceInk.BodyPlacement placed : chart.renderedBodies()) {
                drawn.append(drawn.length() == 0 ? "" : "; ").append(String.format(Locale.ROOT,
                        "%s %s %.1f px%s", placed.name(), placed.symbol() ? "symbol" : "disc",
                        placed.disc().getBounds2D().getWidth(),
                        placed.state() == null ? "" : ", " + placed.state()));
                names.append(names.length() == 0 ? "" : "; ").append(placed.name())
                        .append(placed.box() == null ? " refused" : " placed");
            }
            ChartScene scene = chart.currentScene();
            p(String.format(Locale.ROOT, "| %s° | %d | %d | %s |", FieldWidthText.plain(field),
                    scene.stars().size(), onPage(scene),
                    drawn.length() == 0 ? "nothing" : drawn.toString()));
            q(String.format(Locale.ROOT, "- %s°: %s", FieldWidthText.plain(field),
                    names.length() == 0 ? "nothing drawn" : names.toString()));
        }
        q("");
        p("");
        for (double field : FIELDS) {
            p("![](" + stem + "-" + FieldWidthText.plain(field) + ".png)");
            p("");
        }
    }

    // ---- 5. the closer step offered -----------------------------------------------------

    private static void offered(Instant elongation, SolarSystemService service)
            throws Exception {
        Observer at = new Observer(OSLO_LAT, OSLO_LON, elongation);
        SkyPosition place = ((MercuryObservation) service.observe(Body.MERCURY, at))
                .astrometricJ2000();
        p("## 5. The closer step offered, and the way back");
        p("");
        p("Mercury at its 2026 greatest eastern elongation, wired as the application wires"
                + " the chart (#544): the controller asks the component whether Mercury, Venus, Mars, Jupiter, Saturn or Uranus,"
                + " as the Solar System module contributes them, lies on the page a closer step"
                + " would display. The toolbar, the View menu, the shortcuts and the wheel all"
                + " make these controller calls; what the zoom-in control explains is the"
                + " toolbar's own sentence. A pan of a degree then takes Mercury off the 0.25°"
                + " page.");
        p("");
        SolarSystemModule module = new SolarSystemModule(() -> at, () -> service, () -> false);
        ChartComponent[] holder = new ChartComponent[1];
        ChartViewController controller = new ChartViewController();
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), ENGLISH);
            holder[0].setSize(W, H);
            holder[0].setChartOptions(ChartOptions.DEFAULTS);
            controller.onChange(holder[0]::setViewState);
            holder[0].setCloserFieldBodies(CLOSER_BODIES);
            controller.offerCloserFields(holder[0]::offersCloserField);
            controller.recenter(place, 1.0);
            module.sunShowing(false);
            module.mercuryShowing(false);
            holder[0].overlays().offer(SolarSystemModule.ID, module::contributedGeometry);
        });
        ChartComponent chart = holder[0];
        p("| step | field | toolbar | zoom in | zoom in explains | title note |");
        p("|---|---:|---|---|---|---|");
        row("Mercury hidden", controller, chart);
        module.mercuryShowing(true);
        row("Mercury shown", controller, chart);
        SwingUtilities.invokeAndWait(controller::zoomIn);
        row("zoom in", controller, chart);
        SwingUtilities.invokeAndWait(controller::zoomIn);
        row("zoom in", controller, chart);
        SwingUtilities.invokeAndWait(() -> controller.recenter(new SkyPosition(
                place.raDegrees() + 1.0 / Math.cos(Math.toRadians(place.decDegrees())),
                place.decDegrees())));
        row("pan a degree east", controller, chart);
        ImageIO.write(paint(chart), "png", new File(DIR, "mercury-left-0.25.png"));
        for (int i = 0; i < 3; i++) {
            SwingUtilities.invokeAndWait(controller::zoomOut);
            row("zoom out", controller, chart);
        }
        p("");
        p("The page Mercury has left, at 0.25° - the field kept, the title block saying why"
                + " there is no closer step:");
        p("");
        p("![](mercury-left-0.25.png)");
        p("");
        p("Nothing persists the view: a new controller - every start - opens at the released"
                + " default, " + FieldWidthText.plain(ChartViewState.DEFAULT.fieldWidthDegrees())
                + "° on M31 (" + (new ChartViewController().state().equals(ChartViewState.DEFAULT)
                        ? "held" : "NOT HELD") + "), so a closer field is never saved.");
        p("");
    }

    private static void row(String step, ChartViewController controller, ChartComponent chart)
            throws Exception {
        String[] note = new String[1];
        SwingUtilities.invokeAndWait(() -> note[0] = chart.closerFieldNote(chart.currentScene()));
        ChartViewState state = controller.state();
        java.util.Optional<ChartViewState> next = controller.zoomInTarget();
        String explains = next.isPresent()
                ? EN.say(next.get().closerField() ? "toolbar.zoomIn.closer.explain"
                        : "toolbar.zoomIn.explain")
                : EN.say(state.closerStep().isPresent() ? "toolbar.zoomIn.closer.unavailable"
                        : "toolbar.zoomIn.end");
        p(String.format(Locale.ROOT, "| %s | %s° | %s | %s | %s | %s |", step,
                FieldWidthText.plain(state.fieldWidthDegrees()),
                EN.say("toolbar.readout", FieldWidthText.whole(state.fieldWidthDegrees()),
                        String.format(Locale.ROOT, "%.1f", state.limitingMagnitude())),
                next.map(n -> "to " + FieldWidthText.plain(n.fieldWidthDegrees()) + "°")
                        .orElse("unavailable"),
                explains, note[0] == null ? "-" : note[0]));
    }

    // ---- Jupiter (#544, added by the owner at the checkpoint) -----------------------------

    /** The bodies the application names for the closer fields, as JUranometriaMain does. */
    private static final Set<String> CLOSER_BODIES = Set.of(SolarSystemModule.MERCURY,
            SolarSystemModule.VENUS, juranometria.jovianchart.JovianModule.JUPITER,
            juranometria.saturnchart.SaturnModule.SATURN);

    /**
     * The 11 December 2026 triple transit at 22:45 UTC, inside the common
     * transit: the Jupiter study's sheet moment, held equal to the named
     * Sprint 44 fixture (IMCCE, docs/studies/jovian-system/imcce-2026-12-11.txt).
     */
    private static final Instant TRIPLE = JupiterOnTheChartStudyMain.SHEET_MOMENT;

    private static ChartComponent composeJovian(ChartViewState state,
                                                juranometria.jovianchart.JovianModule module)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), ENGLISH);
            holder[0].setSize(W, H);
            holder[0].setChartOptions(ChartOptions.DEFAULTS);
            holder[0].setViewState(state);
            holder[0].overlays().offer(juranometria.jovianchart.JovianModule.ID,
                    module::contributedGeometry);
        });
        SwingUtilities.invokeAndWait(() -> { });
        return holder[0];
    }

    private static juranometria.jovianchart.JovianModule jovianModule(Observer at,
            juranometria.solar.JovianSystemService service) {
        juranometria.jovianchart.JovianModule module = new juranometria.jovianchart.JovianModule(
                () -> at, () -> service, () -> false);
        module.showing(true);
        return module;
    }

    private static void jupiter() throws Exception {
        juranometria.solar.JovianSystemService service =
                juranometria.solar.JovianSystemService.load();
        Observer at = new Observer(OSLO_LAT, OSLO_LON, TRIPLE);
        SkyPosition place = service.observeJupiter(at).astrometricJ2000();
        p("### 4.4 Jupiter at the 11 December 2026 triple transit, " + TRIPLE + ", Oslo");
        p("");
        p("Jupiter joined the closer fields at the owner's #544 checkpoint (Saturn waits for"
                + " its own cartography study). Its oblate disc at true scale, the Galilean"
                + " moons by the Jovian layer's own rules (a moon in Jupiter's shadow a hollow"
                + " ring). The shadows the moons cast on Jupiter's disc are not drawn; they"
                + " remain a future motivation for closer zoom, not part of #544.");
        p("");
        p("| field | stars queried | stars on the page | drawn |");
        p("|---:|---:|---:|---|");
        q("## 4.4 Jupiter at the triple transit");
        q("");
        for (double field : FIELDS) {
            ChartComponent chart = composeJovian(new ChartViewState(place, field, 8.0),
                    jovianModule(at, service));
            BufferedImage image = paint(chart);
            String file = "jupiter-triple-" + FieldWidthText.plain(field) + ".png";
            ImageIO.write(image, "png", new File(DIR, file));
            StringBuilder drawn = new StringBuilder();
            StringBuilder names = new StringBuilder();
            for (ReferenceInk.BodyPlacement placed : chart.renderedBodies()) {
                drawn.append(drawn.length() == 0 ? "" : "; ").append(String.format(Locale.ROOT,
                        "%s %s %.1f px", placed.name(), placed.symbol() ? "symbol" : "disc",
                        placed.disc().getBounds2D().getWidth()));
                names.append(names.length() == 0 ? "" : "; ").append(placed.name())
                        .append(placed.box() == null ? " refused" : " placed");
            }
            ChartScene scene = chart.currentScene();
            p(String.format(Locale.ROOT, "| %s° | %d | %d | %s |", FieldWidthText.plain(field),
                    scene.stars().size(), onPage(scene),
                    drawn.length() == 0 ? "nothing" : drawn.toString()));
            q(String.format(Locale.ROOT, "- %s°: %s", FieldWidthText.plain(field),
                    names.length() == 0 ? "nothing drawn" : names.toString()));
        }
        q("");
        p("");
        for (double field : FIELDS) {
            p("![](jupiter-triple-" + FieldWidthText.plain(field) + ".png)");
            p("");
        }
    }

    private static void jupiterOffered() throws Exception {
        juranometria.solar.JovianSystemService service =
                juranometria.solar.JovianSystemService.load();
        Observer at = new Observer(OSLO_LAT, OSLO_LON, TRIPLE);
        SkyPosition place = service.observeJupiter(at).astrometricJ2000();
        p("### 5.1 The same steps for Jupiter, at the triple transit");
        p("");
        juranometria.jovianchart.JovianModule module = new juranometria.jovianchart.JovianModule(
                () -> at, () -> service, () -> false);
        module.showing(false);
        ChartComponent[] holder = new ChartComponent[1];
        ChartViewController controller = new ChartViewController();
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), ENGLISH);
            holder[0].setSize(W, H);
            holder[0].setChartOptions(ChartOptions.DEFAULTS);
            controller.onChange(holder[0]::setViewState);
            holder[0].setCloserFieldBodies(CLOSER_BODIES);
            controller.offerCloserFields(holder[0]::offersCloserField);
            controller.recenter(place, 1.0);
            holder[0].overlays().offer(juranometria.jovianchart.JovianModule.ID,
                    module::contributedGeometry);
        });
        ChartComponent chart = holder[0];
        p("| step | field | toolbar | zoom in | zoom in explains | title note |");
        p("|---|---:|---|---|---|---|");
        row("Jupiter hidden", controller, chart);
        module.showing(true);
        row("Jupiter shown", controller, chart);
        SwingUtilities.invokeAndWait(controller::zoomIn);
        row("zoom in", controller, chart);
        SwingUtilities.invokeAndWait(controller::zoomIn);
        row("zoom in", controller, chart);
        SwingUtilities.invokeAndWait(() -> controller.recenter(new SkyPosition(
                place.raDegrees() + 1.0 / Math.cos(Math.toRadians(place.decDegrees())),
                place.decDegrees())));
        row("pan a degree east", controller, chart);
        for (int i = 0; i < 2; i++) {
            SwingUtilities.invokeAndWait(controller::zoomOut);
            row("zoom out", controller, chart);
        }
        p("");
    }

    private static void jupiterSheet() throws Exception {
        juranometria.solar.JovianSystemService service =
                juranometria.solar.JovianSystemService.load();
        Observer at = new Observer(OSLO_LAT, OSLO_LON, TRIPLE);
        SkyPosition place = service.observeJupiter(at).astrometricJ2000();
        ChartViewState state = new ChartViewState(place, 0.25, 8.0);
        ChartComponent chart = composeJovian(state, jovianModule(at, service));
        SheetRecording sheet = record(state, chart);
        Files.write(new File(DIR, "sheet-jupiter-0.25-a4.png").toPath(),
                SheetWriters.write(sheet, SheetFormat.PNG, 150));
        p("### 6.1 Jupiter's sheet at 0.25°, the triple transit");
        p("");
        p("> " + sheet.metadata().description());
        p("");
        p("![](sheet-jupiter-0.25-a4.png)");
        p("");
    }

    // ---- 6. the exported sheet ----------------------------------------------------------

    private static void sheet(Instant elongation, SolarSystemService service) throws Exception {
        Observer at = new Observer(OSLO_LAT, OSLO_LON, elongation);
        SkyPosition place = ((MercuryObservation) service.observe(Body.MERCURY, at))
                .astrometricJ2000();
        ChartViewState state = new ChartViewState(place, 0.25, 8.0);
        ChartComponent chart = compose(state, ENGLISH, at, service, Set.of(Body.MERCURY));
        SheetRecording sheet = record(state, chart);
        byte[] png = SheetWriters.write(sheet, SheetFormat.PNG, 150);
        Files.write(new File(DIR, "sheet-mercury-0.25-a4.png").toPath(), png);
        p("## 6. The exported sheet at 0.25°");
        p("");
        p("Mercury's 2026 greatest eastern elongation at 0.25°, recorded by `ChartSheet` with"
                + " the screen's own reference and bodies layers (`SheetInk`), A4, written at"
                + " 150 dpi for the record. It says of itself:");
        p("");
        p("> " + sheet.metadata().description());
        p("");
        p("and would be offered as `" + SheetFileName.suggest(state,
                Atlas.assembler().assemble(state, W, H), SheetFormat.PNG) + "`.");
        p("");
        p("![](sheet-mercury-0.25-a4.png)");
        p("");
    }

    // ---- helpers ------------------------------------------------------------------------

    private static SheetRecording record(ChartViewState state, ChartComponent chart) {
        return ChartSheet.record(Atlas.assembler()::assemble, state, ChartOptions.DEFAULTS,
                chart == null ? ChartRenderer.ReferenceLayer.NONE
                        : SheetInk.reference(chart, Set.of()),
                chart == null ? ChartRenderer.ReferenceLayer.NONE
                        : SheetInk.bodies(chart, Set.of()),
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4, ENGLISH, Set.of());
    }

    private static ChartComponent compose(ChartViewState state, PageText words, Observer at,
                                          SolarSystemService service, Set<Body> bodies)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(W, H);
            holder[0].setChartOptions(ChartOptions.DEFAULTS);
            holder[0].setViewState(state);
            if (!bodies.isEmpty()) {
                SolarSystemModule module = new SolarSystemModule(() -> at, () -> service,
                        () -> false);
                module.sunShowing(bodies.contains(Body.SUN));
                module.mercuryShowing(bodies.contains(Body.MERCURY));
                module.venusShowing(bodies.contains(Body.VENUS));
                holder[0].overlays().offer(SolarSystemModule.ID, module::contributedGeometry);
            }
        });
        SwingUtilities.invokeAndWait(() -> { });
        return holder[0];
    }

    private static BufferedImage paint(ChartComponent chart) throws Exception {
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return image;
    }

    private static int onPage(ChartScene scene) {
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        int count = 0;
        for (Star star : scene.stars()) {
            var plane = page.projection().project(star.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint pixel = mapping.toPixel(plane.get());
            if (pixel.x() >= 0 && pixel.x() < scene.viewport().widthPx()
                    && pixel.y() >= 0 && pixel.y() < scene.viewport().heightPx()) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, Instant> events(Path file) throws IOException {
        Map<String, Instant> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            String iso = f[1].length() == 17 ? f[1].substring(0, 16) + ":00Z" : f[1];
            out.put(f[0], Instant.parse(iso));
        }
        return out;
    }

    /** The sentence of a description that begins with the given words. */
    private static String sentence(String text, String start) {
        int from = text.indexOf(start);
        if (from < 0) {
            return text;
        }
        int to = text.indexOf('.', from + start.length() + 4);
        return text.substring(from, to < 0 ? text.length() : to + 1);
    }

    /** The title facts' first clause, the field's own. */
    private static String first(String facts) {
        int at = facts.indexOf(" · ");
        return at < 0 ? facts : facts.substring(0, at);
    }

    private static String raStep(double degrees) {
        double seconds = degrees * 240.0;
        return seconds < 60.0 ? fmt(seconds) + "s" : fmt(seconds / 60.0) + "m";
    }

    private static String decStep(double degrees) {
        double arcmin = degrees * 60.0;
        return arcmin < 60.0 ? fmt(arcmin) + "′" : fmt(degrees) + "°";
    }

    private static String fmt(double value) {
        return Math.rint(value) == value
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.2f", value);
    }
}
