package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.render.BelowHorizon;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Ecliptic;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.ui.ecliptic.EclipticSession;
import juranometria.ui.ecliptic.EclipticStore;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ecliptic's longitude scale and the solar-longitude marker on the
 * page (issue #719), as the owner ruled them: the step by the field,
 * no stroke at a landmark, zero never written as 360, the scale
 * subdued with its line and the marker never, and nothing drawn until
 * a reader asks.
 */
class EclipticLongitudeInkTest {

    private static final juranometria.project.PageWords ENGLISH =
            PageText.in(InterfaceText.forLanguage("en"));
    private static final juranometria.project.PageWords NORWEGIAN =
            PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);
    private static final Observer OSLO = new Observer(59.9139, 10.7522,
            Instant.parse("2027-12-14T20:00:00Z"));

    private static ChartScene field(double longitude, double field) {
        return Atlas.assembler().assemble(new ChartViewState(
                Ecliptic.toEquatorial(longitude, 0.0), field,
                ChartViewState.defaultMagnitudeFor(field)), 1200, 800);
    }

    private static ChartScene wholeSky(double centralLongitude) {
        return Atlas.assembler().assemble(ChartViewState.wholeSky(
                Ecliptic.toEquatorial(centralLongitude, 0.0), 5.0), 1600, 900);
    }

    private static OverlayContribution.LongitudeScale scale() {
        EclipticModule module = new EclipticModule();
        module.showing(true);
        module.scaleShowing(true);
        return module.contributedGeometry().stream()
                .filter(OverlayContribution.LongitudeScale.class::isInstance)
                .map(OverlayContribution.LongitudeScale.class::cast)
                .findFirst().orElseThrow();
    }

    private static OverlayContribution.LongitudeMarker marker(double longitude) {
        return new OverlayContribution.LongitudeMarker("solar-longitude-marker", "ecliptic",
                Ecliptic.toEquatorial(longitude, 0.0), Ecliptic.POLE, longitude, 1950.0,
                InkRole.REFERENCE_LINE);
    }

    private static List<OverlayRegistry.Owned> offered(EclipticModule module,
            OverlayContribution... more) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(EclipticModule.ID, module::contributedGeometry);
        if (more.length > 0) {
            registry.offer("solar-system", () -> List.of(more));
        }
        return registry.collect();
    }

    private static BufferedImage painted(ChartScene scene, List<OverlayRegistry.Owned> ink,
            ChartPalette palette, SkyPosition zenith) {
        BufferedImage image = new BufferedImage(scene.viewport().widthPx(),
                scene.viewport().heightPx(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, ChartOptions.DEFAULTS.withPalette(palette),
                    (layerG, drawn, reserved) -> ReferenceInk.paint(layerG, drawn, ink, palette,
                            ENGLISH, reserved, java.util.Set.of(),
                            zenith == null ? BelowHorizon.NONE
                                    : BelowHorizon.of(zenith, drawn)));
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int differing(BufferedImage a, BufferedImage b) {
        int n = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    n++;
                }
            }
        }
        return n;
    }

    @Test
    void theStepFollowsTheFieldAsRuled() {
        assertEquals(new ReferenceInk.LongitudeCadence(10.0, 30.0),
                ReferenceInk.LongitudeCadence.forField(360.0));
        assertEquals(new ReferenceInk.LongitudeCadence(10.0, 30.0),
                ReferenceInk.LongitudeCadence.forField(36.0));
        assertEquals(new ReferenceInk.LongitudeCadence(5.0, 10.0),
                ReferenceInk.LongitudeCadence.forField(24.0));
        assertEquals(new ReferenceInk.LongitudeCadence(5.0, 10.0),
                ReferenceInk.LongitudeCadence.forField(12.0));
        assertEquals(new ReferenceInk.LongitudeCadence(1.0, 5.0),
                ReferenceInk.LongitudeCadence.forField(8.0));
        assertEquals(new ReferenceInk.LongitudeCadence(1.0, 5.0),
                ReferenceInk.LongitudeCadence.forField(2.0));
    }

    @Test
    void theWholeSkyCarriesEveryTenthDegreeButTheFourLandmarks() {
        List<ReferenceInk.LongitudeStroke> strokes = ReferenceInk.longitudeStrokes(
                DrawnPage.of(wholeSky(55.0)), scale());
        assertEquals(32, strokes.size(), "thirty-six tenths of the turn less four landmarks");
        for (ReferenceInk.LongitudeStroke stroke : strokes) {
            assertTrue(stroke.degrees() % 90.0 != 0.0,
                    "the diamond is the graduation at " + stroke.degrees());
            assertEquals(stroke.degrees() % 30.0 == 0.0, stroke.figured(),
                    "a figure every thirty degrees: " + stroke.degrees());
        }
    }

    @Test
    void aStrokeExactlyOnTheSeamIsNotDrawn() {
        // Centred on 50 degrees the page is cut at 230, a tenth degree.
        List<ReferenceInk.LongitudeStroke> strokes = ReferenceInk.longitudeStrokes(
                DrawnPage.of(wholeSky(50.0)), scale());
        assertEquals(31, strokes.size());
        assertTrue(strokes.stream().noneMatch(s -> s.degrees() == 230.0));
    }

    @Test
    void anOrdinaryFieldShowsOnlyThePartOnItsPage() {
        List<ReferenceInk.LongitudeStroke> close = ReferenceInk.longitudeStrokes(
                DrawnPage.of(field(83.8, 8.0)), scale());
        assertFalse(close.isEmpty(), "an 8 degree page is graduated by the degree");
        for (ReferenceInk.LongitudeStroke stroke : close) {
            assertTrue(Math.abs(stroke.degrees() - 83.8) < 6.0,
                    "only what the page holds: " + stroke.degrees());
            assertEquals(stroke.degrees() % 5.0 == 0.0, stroke.figured());
        }
        assertTrue(ReferenceInk.longitudeStrokes(DrawnPage.of(field(83.8, 60.0)), scale())
                .stream().allMatch(s -> s.degrees() % 10.0 == 0.0));
    }

    @Test
    void theFiguresAreThePagesOwnAndZeroIsNeverThreeHundredAndSixty() {
        assertEquals("30°", ENGLISH.longitudeTick(30.0));
        assertEquals("0°", ENGLISH.longitudeTick(360.0));
        assertEquals("262.2°", ENGLISH.solarLongitudeFigure(262.2));
        assertEquals("262.2°", NORWEGIAN.solarLongitudeFigure(262.2),
                "a point in both languages, as the meteor panel writes it");
        assertEquals("0.0°", ENGLISH.solarLongitudeFigure(359.96));
        assertTrue(ENGLISH.solarLongitudeSpoken("262.2°").contains("262.2°"));
        assertTrue(NORWEGIAN.solarLongitudeSpoken("262.2°").contains("sollengde"));
    }

    @Test
    void nothingIsDrawnUntilAskedForAndTheScaleRidesTheLine() {
        EclipticModule module = new EclipticModule();
        module.scaleShowing(true);
        module.solarMarkerShowing(true);
        assertTrue(module.contributedGeometry().isEmpty(),
                "a hidden ecliptic carries no scale");
        assertFalse(module.solarMarkerDrawn(), "and no marker");
        module.showing(true);
        module.scaleShowing(false);
        ChartScene scene = field(83.8, 60.0);
        BufferedImage bare = painted(scene, offered(module), ChartPalette.WHITE_PAPER, null);
        module.scaleShowing(true);
        BufferedImage graduated = painted(scene, offered(module), ChartPalette.WHITE_PAPER,
                null);
        assertTrue(differing(bare, graduated) > 0, "the scale adds ink");
        module.scaleShowing(false);
        assertEquals(0, differing(bare, painted(scene, offered(module),
                ChartPalette.WHITE_PAPER, null)), "and off, the page is the page");
    }

    @Test
    void theMarkerIsFullInkBelowTheHorizonAndTheScaleIsSubdued() {
        // In December the Sun's place is below Oslo's horizon in the evening.
        SkyPosition zenith = new LocalSky(OSLO).zenith();
        double longitude = 262.2;
        ChartScene scene = field(longitude, 60.0);
        EclipticModule module = new EclipticModule();
        module.showing(true);
        for (ChartPalette palette : new ChartPalette[] {ChartPalette.WHITE_PAPER,
                ChartPalette.BLACK_SKY}) {
            BufferedImage without = painted(scene, offered(module), palette, zenith);
            BufferedImage with = painted(scene, offered(module, marker(longitude)), palette,
                    zenith);
            ReferenceInk.LongitudeMarkerPlacement placed = ReferenceInk.longitudeMarker(
                    DrawnPage.of(scene), marker(longitude)).orElseThrow();
            java.awt.geom.Rectangle2D caret = placed.north().getBounds2D();
            int x = (int) Math.round(caret.getCenterX());
            int y = (int) Math.round(caret.getCenterY());
            assertEquals(palette.textInk().getRGB(), with.getRGB(x, y),
                    "a notation of the chart's time is never subdued");
            assertTrue(differing(without, with) > 0);
            assertTrue(placed.tipPx() >= ReferenceInk.CARET_TIP);
        }
    }

    @Test
    void theCaretsStandClearOfTheSunsLimbOnACloseField() {
        ChartScene scene = field(83.8, 8.0);
        ReferenceInk.LongitudeMarkerPlacement placed = ReferenceInk.longitudeMarker(
                DrawnPage.of(scene), marker(83.8)).orElseThrow();
        // 1950 arcseconds across on a page 1200 pixels to 8 degrees.
        double radius = 1950.0 / 2.0 / 3600.0 * 1200.0 / 8.0;
        assertEquals(radius + ReferenceInk.CARET_CLEAR_OF_DISC, placed.tipPx(), 1.0);
    }

    @Test
    void theSwitchRemembersBothNotationsBesideTheLine() {
        EclipticStore store = EclipticStore.inMemory();
        EclipticModule first = new EclipticModule();
        EclipticSession.Switch one = EclipticSession.of(first, store);
        int[] told = new int[1];
        one.followAll(() -> told[0]++);
        one.scale(true);
        one.solarMarker(true);
        assertTrue(told[0] >= 3, "a box hears at once and at every change");
        assertTrue(first.scaleShowing());
        assertTrue(first.solarMarkerShowing());

        EclipticModule second = new EclipticModule();
        EclipticSession.Switch next = EclipticSession.of(second, store);
        next.follow(showing -> { });
        next.restore();
        assertTrue(second.scaleShowing(), "a second session reads what the first chose");
        assertTrue(second.solarMarkerShowing());
        assertFalse(EclipticStore.inMemory().scaleShown(), "off until asked for");
        assertFalse(EclipticStore.inMemory().solarMarkerShown());
    }
}
