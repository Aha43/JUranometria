package juranometria.ui;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.solar.UranusService;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the page draws Uranus (issue #638, under the rulings on #637): rule M
 * with the 6 px mark always standing in (R-1) - Uranus's true disc never
 * reaches it, not even at the closest field - drawn full and round in the
 * page's ink with no colour (R-2), a cartographic symbol spoken as one and
 * pointed at (R-8); behind the Sun covered, with the state spoken. The
 * instants are Espenak's 2026 opposition and conjunction hours
 * (docs/studies/uranus-system/uranus-events.txt).
 */
class UranusInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final SolarSystemService SOLAR = SolarSystemService.load();
    private static final UranusService URANUS = UranusService.load();
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;
    /** Espenak's 2026 opposition and conjunction hours (uranus-events.txt). */
    private static final Instant OPPOSITION_2026 = Instant.parse("2026-11-25T23:00:00Z");
    private static final Instant CONJUNCTION_2026 = Instant.parse("2026-05-22T16:00:00Z");

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, 8.0, null, null),
                900, 700);
    }

    private static UranusService.UranusObservation uranus(Instant when) {
        return URANUS.observe(new Observer(OSLO_LAT, OSLO_LON, when));
    }

    /** The module's own contribution for the instant, through the production module. */
    private static OverlayRegistry offering(Instant when, boolean withSun) {
        SolarSystemModule module = new SolarSystemModule(
                () -> new Observer(OSLO_LAT, OSLO_LON, when), () -> SOLAR, () -> false);
        module.withUranus(() -> URANUS);
        module.uranusShowing(true);
        module.sunShowing(withSun);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        return registry;
    }

    private static List<ReferenceInk.BodyPlacement> placed(ChartScene scene, OverlayRegistry r,
                                                           PageText words) {
        return ReferenceInk.bodyPlacements(DrawnPage.of(scene), r.collect(),
                ChartPalette.WHITE_PAPER, words, List.of());
    }

    private static BufferedImage painted(ChartScene scene, OverlayRegistry registry,
                                         ChartPalette palette, PageText words) {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, words);
            renderer.render(g, scene, juranometria.render.ChartOptions.DEFAULTS.withPalette(palette),
                    (layerG, layerScene, reserved) -> ReferenceInk.paint(layerG, layerScene,
                            registry.collect(), palette, words, reserved, java.util.Set.of()),
                    (layerG, layerScene, reserved) -> {
                        DrawnPage page = DrawnPage.of(layerScene);
                        ReferenceInk.paintBodies(layerG, page, registry.collect(), palette,
                                words, reserved, ReferenceInk.referenceBoxes(page,
                                        registry.collect(), words, reserved, s -> false));
                    }, null, java.util.Set.of());
        } finally {
            g.dispose();
        }
        return image;
    }

    @Test
    void onEveryFieldTheMarkStandsInAndIsASymbol() {
        UranusService.UranusObservation u = uranus(OPPOSITION_2026);
        for (double field : new double[] {8.0, ChartViewState.normalMinimumFieldDegrees()}) {
            ChartScene scene = page(u.astrometricJ2000(), field);
            List<ReferenceInk.BodyPlacement> drawn = placed(scene, offering(OPPOSITION_2026, false), ENGLISH);
            // Since #638 part 2 a moon standing clear is drawn too (at 1°, Titania and Oberon).
            ReferenceInk.BodyPlacement uranus = drawn.stream()
                    .filter(b -> SolarSystemModule.URANUS.equals(b.identity())).findFirst().orElseThrow();
            assertEquals(SolarSystemModule.URANUS, uranus.identity());
            assertEquals(SolarSystemModule.URANUS_MINIMUM_MARK_PX, uranus.disc().getBounds2D().getWidth(), 0.05,
                    field + "°: the 6 px mark - the true disc is about a pixel at most");
            assertTrue(uranus.symbol(), "a cartographic symbol");
            assertFalse(uranus.covers(uranus.centre().x(), uranus.centre().y()), "not opaque");
            assertTrue(uranus.pointedAt(uranus.centre().x(), uranus.centre().y(), 4.0),
                    "pointed at as an occurrence (#513, R-8)");
            assertEquals("Uranus", uranus.name());
        }
        assertEquals("uranus", SolarSystemModule.URANUS, "the page's identity, derived from the service's name");
        ChartScene scene = page(u.astrometricJ2000(), 8.0);
        String said = ChartComponent.withBodies("The page.",
                placed(scene, offering(OPPOSITION_2026, false), ENGLISH), ENGLISH);
        assertTrue(said.contains("Uranus (a cartographic symbol, not Uranus's apparent diameter)."), said);
        assertTrue(ChartComponent.withBodies("Siden.", placed(scene, offering(OPPOSITION_2026, false), NORSK), NORSK)
                .contains("et kartografisk symbol, ikke Uranus' tilsynelatende diameter"),
                "spoken as a symbol in Norwegian");
    }

    @Test
    void theMarkIsDrawnFullAndRoundNeverPhased() {
        UranusService.UranusObservation u = uranus(OPPOSITION_2026);
        ChartScene scene = page(u.astrometricJ2000(), ChartViewState.normalMinimumFieldDegrees());
        ReferenceInk.BodyPlacement uranus = placed(scene, offering(OPPOSITION_2026, false), ENGLISH).stream()
                .filter(b -> SolarSystemModule.URANUS.equals(b.identity())).findFirst().orElseThrow();
        BufferedImage image = painted(scene, offering(OPPOSITION_2026, false), ChartPalette.WHITE_PAPER, ENGLISH);
        double width = uranus.disc().getBounds2D().getWidth();
        double[] share = litShare(image, uranus.centre().x(), uranus.centre().y(), width / 2.0 - 1.0);
        assertTrue(share[1] > 0 && share[0] == 1.0, "every pixel inside the outline lit (R-2): " + share[0]
                + " of " + share[1] + " pixels");
    }

    @Test
    void behindTheSunTheMarkIsCoveredAndThePageSaysSo() {
        UranusService.UranusObservation u = uranus(CONJUNCTION_2026);
        assertTrue(u.behindSun(), "behind the Sun's disc at Espenak's 2026 conjunction hour");
        ChartScene scene = page(u.astrometricJ2000(), ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> drawn = placed(scene, offering(CONJUNCTION_2026, true), ENGLISH);
        ReferenceInk.BodyPlacement uranus = drawn.stream()
                .filter(b -> SolarSystemModule.URANUS.equals(b.identity())).findFirst().orElseThrow();
        assertEquals(SolarSystemModule.URANUS_BEHIND, uranus.state());
        BufferedImage image = painted(scene, offering(CONJUNCTION_2026, true), ChartPalette.WHITE_PAPER, ENGLISH);
        assertEquals(Color.WHITE.getRGB(), image.getRGB((int) Math.floor(uranus.centre().x()),
                (int) Math.floor(uranus.centre().y())), "the Sun's ground-filled disc covers it");
        String said = ChartComponent.withBodies("The page.", drawn, ENGLISH);
        assertTrue(said.contains("Uranus, behind the Sun"), said);
        assertTrue(ChartComponent.withBodies("Siden.", placed(scene, offering(CONJUNCTION_2026, true), NORSK), NORSK)
                .contains("Uranus, bak Solen"), "and in Norwegian");
    }

    @Test
    void outsideItsYearsNothingIsDrawnAndWithoutAServiceNothingIsAsked() {
        OverlayRegistry before = offering(Instant.parse("1899-06-01T00:00:00Z"), false);
        assertTrue(before.collect().isEmpty(), "before 1900 the chart draws nothing for Uranus");
        SolarSystemModule bare = new SolarSystemModule(
                () -> new Observer(OSLO_LAT, OSLO_LON, OPPOSITION_2026), () -> SOLAR, () -> false);
        bare.uranusShowing(true);
        assertTrue(bare.contributedGeometry().isEmpty(), "a module given no Uranus service draws no Uranus");
    }

    private static double[] litShare(BufferedImage image, double cx, double cy, double r) {
        double lit = 0;
        double all = 0;
        int darkTone = (int) Math.round(255 * ReferenceInk.darkSide());
        for (int y = (int) Math.floor(cy - r); y <= Math.ceil(cy + r); y++) {
            for (int x = (int) Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
                if (Math.hypot(x + 0.5 - cx, y + 0.5 - cy) > r - 0.5 || x < 0 || y < 0
                        || x >= image.getWidth() || y >= image.getHeight()) {
                    continue;
                }
                int red = new Color(image.getRGB(x, y)).getRed();
                all++;
                if (red > (darkTone + 255) / 2) {
                    lit++;
                }
            }
        }
        return new double[] {all == 0 ? 0 : lit / all, all};
    }
}
