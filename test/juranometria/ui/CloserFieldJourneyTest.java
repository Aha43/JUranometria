package juranometria.ui;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.project.PlanePoint;
import juranometria.sky.Observer;
import juranometria.solar.MercuryService.MercuryObservation;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.solar.SunChartSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The closer fields offered (#544, closing #481, under rulings 4-6 on
 * #539), through the component, the controller and the Solar System
 * module wired as the application wires them: each closer step is
 * offered where Mercury or Venus, as drawn, lies on the page it would
 * display - about the centre for buttons and keys, about the pointer
 * for the wheel - and nowhere else; panning keeps the field, a page the
 * body has left says why there is no closer step, Home is Home, and
 * at 0.25° Mercury's phase is on the screen. The instant is the 2026
 * greatest eastern elongation Espenak's row cites
 * ({@code docs/studies/mercury/mercury-events.txt}), Oslo.
 */
class CloserFieldJourneyTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final SolarSystemService SERVICE = SolarSystemService.load();
    private static final double OSLO_LAT = 59.91;
    private static final double OSLO_LON = 10.75;

    private ChartComponent chart;
    private ChartViewController controller;
    private SolarSystemModule module;
    private final AtomicInteger availability = new AtomicInteger();

    private static Instant elongation() throws Exception {
        for (String line : Files.readAllLines(Path.of("docs/studies/mercury/mercury-events.txt"),
                StandardCharsets.UTF_8)) {
            if (line.startsWith("2026-greatest-elongation-east-1\t")) {
                String iso = line.split("\t")[1];
                return Instant.parse(iso.length() == 17 ? iso.substring(0, 16) + ":00Z" : iso);
            }
        }
        throw new IllegalStateException("the cited elongation is in the fixture");
    }

    private static Observer oslo() throws Exception {
        return new Observer(OSLO_LAT, OSLO_LON, elongation());
    }

    private static SkyPosition mercury(Observer at) {
        return ((MercuryObservation) SERVICE.observe(Body.MERCURY, at)).astrometricJ2000();
    }

    /** The application's wiring (JUranometriaMain), at a page centred where asked. */
    private void build(Observer observer, SkyPosition centre, double field) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart.setSize(900, 700);
            controller = new ChartViewController();
            controller.onChange(chart::setViewState);
            chart.setCloserFieldBodies(Set.of(SolarSystemModule.MERCURY, SolarSystemModule.VENUS));
            controller.offerCloserFields(chart::offersCloserField);
            chart.onPageContentsChange(controller::closerOfferChanged);
            controller.onAvailabilityChange(availability::incrementAndGet);
            controller.recenter(centre, field);
            ChartModuleHost host = new ChartModuleHost(chart, new SelectionModel(),
                    request -> { });
            MeridianModule meridian = host.attach(new MeridianModule(observer));
            meridian.showing(false, false, false);
            module = SunChartSession.begin(host,
                    () -> meridian.attached() ? meridian.observer() : null,
                    () -> SERVICE, meridian::horizonShowing);
            module.sunShowing(false);
        });
        flush();
    }

    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    private BufferedImage paint() throws Exception {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return image;
    }

    @Test
    void theCloserStepsAreOfferedWhereMercuryIsDrawnAndThePhaseIsOnTheScreen() throws Exception {
        Observer at = oslo();
        build(at, mercury(at), 1.0);
        assertFalse(controller.canZoomIn(), "Mercury hidden: 1° is the floor");

        int before = availability.get();
        SwingUtilities.invokeAndWait(() -> module.mercuryShowing(true));
        flush();
        assertTrue(availability.get() > before,
                "switching Mercury on tells the zoom controls to ask again");
        assertTrue(controller.canZoomIn(), "Mercury on the page 0.5° would show");
        assertEquals(0.5, controller.zoomInTarget().orElseThrow().fieldWidthDegrees());

        SwingUtilities.invokeAndWait(controller::zoomIn);
        flush();
        assertEquals(0.5, controller.state().fieldWidthDegrees());
        SwingUtilities.invokeAndWait(controller::zoomIn);
        flush();
        assertEquals(0.25, controller.state().fieldWidthDegrees());
        assertFalse(controller.canZoomIn(), "0.25° is the closest");

        // Ruling 4's acceptance: at 0.25° Mercury's true disc is larger
        // than the mark, and its lit and dark sides are both drawn.
        BufferedImage image = paint();
        ReferenceInk.BodyPlacement drawn = chart.renderedBodies().stream()
                .filter(b -> SolarSystemModule.MERCURY.equals(b.identity()))
                .findFirst().orElseThrow();
        assertFalse(drawn.symbol(), "the true disc, not the symbol");
        double diameter = drawn.disc().getBounds2D().getWidth();
        assertTrue(diameter > SolarSystemModule.MERCURY_MINIMUM_MARK_PX, "" + diameter);
        int lit = 0;
        int dark = 0;
        double cx = drawn.centre().x() + chart.pageOffsetX();
        double cy = drawn.centre().y() + chart.pageOffsetY();
        double inner = diameter / 2.0 - 1.0;
        for (int y = (int) (cy - inner); y <= (int) (cy + inner); y++) {
            for (int x = (int) (cx - inner); x <= (int) (cx + inner); x++) {
                if (Math.hypot(x + 0.5 - cx, y + 0.5 - cy) > inner) {
                    continue;
                }
                int rgb = image.getRGB(x, y);
                int luminance = ((rgb >> 16 & 0xff) + (rgb >> 8 & 0xff) + (rgb & 0xff)) / 3;
                if (luminance > 200) {
                    lit++;
                } else if (luminance < 120) {
                    dark++;
                }
            }
        }
        assertTrue(lit >= 3 && dark >= 3,
                "a phase a reader can see: " + lit + " lit and " + dark + " dark pixels");
        assertTrue(chart.getAccessibleContext().getAccessibleDescription()
                .contains("Closer field 0.25 degrees wide"),
                chart.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void panningKeepsTheFieldAndAPageWithoutTheBodySaysWhy() throws Exception {
        Observer at = oslo();
        SkyPosition place = mercury(at);
        build(at, place, 0.5);
        SwingUtilities.invokeAndWait(() -> module.mercuryShowing(true));
        flush();
        paint();
        assertNull(chart.closerFieldNote(chart.currentScene()), "Mercury is on this page");
        assertTrue(controller.canZoomIn());

        // Mercury leaves the page: a pan of a degree.
        SwingUtilities.invokeAndWait(() -> controller.recenter(
                new SkyPosition(place.raDegrees() + 1.0, place.decDegrees())));
        flush();
        assertEquals(0.5, controller.state().fieldWidthDegrees(), "panning keeps the field");
        assertFalse(controller.canZoomIn(), "no body, no closer step");
        assertEquals(ENGLISH.closerFieldWithoutBody(),
                chart.closerFieldNote(chart.currentScene()), "and the title says why");
        assertNotNull(ENGLISH.closerFieldWithoutBody());
        assertTrue(controller.canZoomOut());

        SwingUtilities.invokeAndWait(controller::reset);
        flush();
        assertEquals(ChartViewState.DEFAULT, controller.state(), "Home is the opening page");
    }

    @Test
    void theWheelAsksThePageAboutThePointerAndNotAboutTheCentre() throws Exception {
        Observer at = oslo();
        SkyPosition place = mercury(at);
        // Mercury four tenths of a degree west of the page centre: off
        // the 0.5° page about the centre, on the one about a pointer
        // beside it, off the one about a pointer at the far edge.
        SkyPosition centre = new SkyPosition(place.raDegrees()
                + 0.4 / Math.cos(Math.toRadians(place.decDegrees())), place.decDegrees());
        build(at, centre, 1.0);
        SwingUtilities.invokeAndWait(() -> module.mercuryShowing(true));
        flush();
        assertFalse(controller.canZoomIn(), "the button's page is about the centre");

        PlanePoint onMercury = new juranometria.project.GnomonicProjection(centre)
                .project(place).orElseThrow();
        assertTrue(onMercury.xiEast() < 0, "Mercury is west of the centre");
        ChartViewController.PointerZoomOutcome far = controller.zoomAt(
                new PlanePoint(-onMercury.xiEast(), 0.0), true, 900, 700);
        assertEquals(ChartViewController.PointerZoomOutcome.AT_BOUND, far);
        assertEquals(1.0, controller.state().fieldWidthDegrees());

        ChartViewController.PointerZoomOutcome near = controller.zoomAt(onMercury, true,
                900, 700);
        assertEquals(ChartViewController.PointerZoomOutcome.ACCEPTED, near);
        assertEquals(0.5, controller.state().fieldWidthDegrees());
    }

    @Test
    void theSunNeverOffersACloserField() throws Exception {
        Observer at = oslo();
        SkyPosition sun = ((SolarSystemService.SunObservation) SERVICE.observe(Body.SUN, at))
                .astrometricJ2000();
        build(at, sun, 1.0);
        SwingUtilities.invokeAndWait(() -> module.sunShowing(true));
        flush();
        assertFalse(controller.canZoomIn(), "the Sun fills the ordinary pages already");
    }

    // ---- Jupiter (#544, added by the owner at the checkpoint) ----------------

    /** The 11 December 2026 triple transit, the named Sprint 44 fixture (TripleTransitTest). */
    private static final Instant TRIPLE = Instant.parse("2026-12-11T22:45:00Z");

    private juranometria.jovianchart.JovianModule jovian;

    private void buildJovian(SkyPosition centre, double field) throws Exception {
        Observer at = new Observer(OSLO_LAT, OSLO_LON, TRIPLE);
        juranometria.solar.JovianSystemService service =
                juranometria.solar.JovianSystemService.load();
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart.setSize(900, 700);
            controller = new ChartViewController();
            controller.onChange(chart::setViewState);
            chart.setCloserFieldBodies(Set.of(SolarSystemModule.MERCURY, SolarSystemModule.VENUS,
                    juranometria.jovianchart.JovianModule.JUPITER,
                    juranometria.saturnchart.SaturnModule.SATURN));
            controller.offerCloserFields(chart::offersCloserField);
            chart.onPageContentsChange(controller::closerOfferChanged);
            controller.onAvailabilityChange(availability::incrementAndGet);
            controller.recenter(centre, field);
            ChartModuleHost host = new ChartModuleHost(chart, new SelectionModel(),
                    request -> { });
            MeridianModule meridian = host.attach(new MeridianModule(at));
            meridian.showing(false, false, false);
            jovian = host.attach(new juranometria.jovianchart.JovianModule(
                    () -> meridian.attached() ? meridian.observer() : null,
                    () -> service, meridian::horizonShowing));
            jovian.showing(false);
        });
        flush();
    }

    private static SkyPosition jupiter() {
        return juranometria.solar.JovianSystemService.load()
                .observeJupiter(new Observer(OSLO_LAT, OSLO_LON, TRIPLE)).astrometricJ2000();
    }

    @Test
    void jupiterAtTheTripleTransitOffersTheCloserFields() throws Exception {
        SkyPosition place = jupiter();
        buildJovian(place, 1.0);
        assertFalse(controller.canZoomIn(), "Jupiter hidden: 1° is the floor");
        int before = availability.get();
        SwingUtilities.invokeAndWait(() -> jovian.showing(true));
        flush();
        assertTrue(availability.get() > before, "switching Jupiter on tells the controls");
        assertTrue(controller.canZoomIn(), "Jupiter on the page 0.5° would show");

        SwingUtilities.invokeAndWait(controller::zoomIn);
        flush();
        SwingUtilities.invokeAndWait(controller::zoomIn);
        flush();
        assertEquals(0.25, controller.state().fieldWidthDegrees());
        paint();
        assertTrue(chart.renderedBodies().stream().anyMatch(b ->
                        juranometria.jovianchart.JovianModule.JUPITER.equals(b.identity())),
                "Jupiter is drawn on the closer page: " + chart.renderedBodies());
        assertTrue(chart.getAccessibleContext().getAccessibleDescription()
                        .contains("Closer field 0.25 degrees wide"),
                chart.getAccessibleContext().getAccessibleDescription());
        assertNull(chart.closerFieldNote(chart.currentScene()));

        // The sheet says what the screen says, Jupiter's ink and all.
        juranometria.sheet.SheetRecording sheet = juranometria.sheet.ChartSheet.record(
                Atlas.assembler()::assemble, controller.state(),
                juranometria.render.ChartOptions.DEFAULTS,
                SheetInk.reference(chart, Set.of()), SheetInk.bodies(chart, Set.of()),
                juranometria.render.ChartRenderer.ReferenceLayer.NONE,
                juranometria.sheet.PaperSize.A4, ENGLISH, Set.of());
        assertTrue(sheet.metadata().description().contains("Closer field 0.25 degrees wide"),
                sheet.metadata().description());

        // Jupiter leaves: the field is kept and the title says why.
        SwingUtilities.invokeAndWait(() -> controller.recenter(new SkyPosition(
                place.raDegrees() + 1.0 / Math.cos(Math.toRadians(place.decDegrees())),
                place.decDegrees())));
        flush();
        assertEquals(0.25, controller.state().fieldWidthDegrees(), "panning keeps the field");
        assertFalse(controller.canZoomIn());
        String note = chart.closerFieldNote(chart.currentScene());
        assertEquals(ENGLISH.closerFieldWithoutBody(), note);
        assertTrue(note.contains("Mercury, Venus, Mars, Jupiter, Saturn or Uranus"), note);
        SwingUtilities.invokeAndWait(controller::zoomOut);
        SwingUtilities.invokeAndWait(controller::zoomOut);
        assertEquals(1.0, controller.state().fieldWidthDegrees(), "back to the ladder");
    }

    @Test
    void theWheelAsksAboutJupiterAtThePointer() throws Exception {
        SkyPosition place = jupiter();
        SkyPosition centre = new SkyPosition(place.raDegrees()
                + 0.4 / Math.cos(Math.toRadians(place.decDegrees())), place.decDegrees());
        buildJovian(centre, 1.0);
        SwingUtilities.invokeAndWait(() -> jovian.showing(true));
        flush();
        assertFalse(controller.canZoomIn(), "the button's page is about the centre");
        PlanePoint onJupiter = new juranometria.project.GnomonicProjection(centre)
                .project(place).orElseThrow();
        assertEquals(ChartViewController.PointerZoomOutcome.AT_BOUND,
                controller.zoomAt(new PlanePoint(-onJupiter.xiEast(), 0.0), true, 900, 700));
        assertEquals(ChartViewController.PointerZoomOutcome.ACCEPTED,
                controller.zoomAt(onJupiter, true, 900, 700));
        assertEquals(0.5, controller.state().fieldWidthDegrees());
    }

    // ---- Saturn (#579; ruling 6 on #578) --------------------------------------

    /** The contract's card instant (#574): the 2026 Oslo day nearest the Earth. */
    private static final Instant CARD = Instant.parse("2026-10-05T00:00:00Z");

    private juranometria.saturnchart.SaturnModule saturnModule;

    private void buildSaturn(SkyPosition centre, double field) throws Exception {
        Observer at = new Observer(OSLO_LAT, OSLO_LON, CARD);
        juranometria.solar.SaturnSystemService service =
                juranometria.solar.SaturnSystemService.load();
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart.setSize(900, 700);
            controller = new ChartViewController();
            controller.onChange(chart::setViewState);
            chart.setCloserFieldBodies(Set.of(SolarSystemModule.MERCURY, SolarSystemModule.VENUS,
                    juranometria.jovianchart.JovianModule.JUPITER,
                    juranometria.saturnchart.SaturnModule.SATURN));
            controller.offerCloserFields(chart::offersCloserField);
            chart.onPageContentsChange(controller::closerOfferChanged);
            controller.onAvailabilityChange(availability::incrementAndGet);
            controller.recenter(centre, field);
            ChartModuleHost host = new ChartModuleHost(chart, new SelectionModel(),
                    request -> { });
            MeridianModule meridian = host.attach(new MeridianModule(at));
            meridian.showing(false, false, false);
            saturnModule = host.attach(new juranometria.saturnchart.SaturnModule(
                    () -> meridian.attached() ? meridian.observer() : null,
                    () -> service, meridian::horizonShowing));
            saturnModule.showing(false);
        });
        flush();
    }

    @Test
    void saturnAtTheCardInstantOffersTheCloserFields() throws Exception {
        SkyPosition place = juranometria.solar.SaturnSystemService.load()
                .observeSaturn(new Observer(OSLO_LAT, OSLO_LON, CARD)).astrometricJ2000();
        buildSaturn(place, 1.0);
        assertFalse(controller.canZoomIn(), "Saturn hidden: 1° is the floor");
        int before = availability.get();
        SwingUtilities.invokeAndWait(() -> saturnModule.showing(true));
        flush();
        assertTrue(availability.get() > before, "switching Saturn on tells the controls");
        assertTrue(controller.canZoomIn(), "Saturn on the page 0.5° would show");
        SwingUtilities.invokeAndWait(controller::zoomIn);
        flush();
        SwingUtilities.invokeAndWait(controller::zoomIn);
        flush();
        assertEquals(0.25, controller.state().fieldWidthDegrees());
        paint();
        ReferenceInk.BodyPlacement saturn = chart.renderedBodies().stream()
                .filter(b -> juranometria.saturnchart.SaturnModule.SATURN.equals(b.identity()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "Saturn is drawn on the closer page: " + chart.renderedBodies()));
        assertFalse(saturn.symbol(), "at 0.25° the true figure, the rings about 44 px across");
        String said = chart.getAccessibleContext().getAccessibleDescription();
        assertTrue(said.contains(" Saturn. ") || said.endsWith(" Saturn."), said);
    }

    @Test
    void theMoonNeverOffersACloserField() throws Exception {
        Observer at = oslo();
        SkyPosition moon = ((SolarSystemService.MoonObservation) SERVICE.observe(Body.MOON, at))
                .astrometricJ2000();
        build(at, moon, 1.0);
        SwingUtilities.invokeAndWait(() -> module.moonShowing(true));
        flush();
        assertFalse(controller.canZoomIn(), "the Moon fills the ordinary pages already");
    }
}
