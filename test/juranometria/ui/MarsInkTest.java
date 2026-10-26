package juranometria.ui;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.solar.MarsService;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the page draws Mars (issue #606, under the rulings on #605): rule M
 * as for Venus and Mercury - the larger of its true equatorial disc and
 * the 6 px mark, phased by the page's own lit region, in the page's ink
 * with no colour (R-2); on the ordinary ladder the mark, a cartographic
 * symbol spoken as one and pointed at (R-5); the true disc at 1° near the
 * closest opposition; behind the Sun covered, with the state spoken. The
 * instants are Espenak's 2003 opposition hour (mars-events.txt) and the
 * hour the service itself finds Mars deepest behind the Sun in 1912.
 */
class MarsInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final SolarSystemService SOLAR = SolarSystemService.load();
    private static final MarsService MARS = MarsService.load();
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;
    /** Espenak's 2003 opposition hour (docs/studies/mars-system/mars-events.txt). */
    private static final Instant OPPOSITION_2003 = Instant.parse("2003-08-28T16:00:00Z");

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, 8.0, null, null),
                900, 700);
    }

    private static MarsService.MarsObservation mars(Instant when) {
        return MARS.observe(new Observer(OSLO_LAT, OSLO_LON, when));
    }

    /** The module's own contribution for the instant, through the production module. */
    private static OverlayRegistry offering(Instant when, boolean withSun) {
        SolarSystemModule module = new SolarSystemModule(
                () -> new Observer(OSLO_LAT, OSLO_LON, when), () -> SOLAR, () -> false);
        module.withMars(() -> MARS);
        module.marsShowing(true);
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
    void atTheOrdinaryFieldsTheMarkStandsInAndIsASymbol() {
        MarsService.MarsObservation m = mars(OPPOSITION_2003);
        ChartScene scene = page(m.astrometricJ2000(), 8.0);
        List<ReferenceInk.BodyPlacement> drawn = placed(scene, offering(OPPOSITION_2003, false), ENGLISH);
        assertEquals(1, drawn.size());
        ReferenceInk.BodyPlacement mars = drawn.get(0);
        assertEquals(SolarSystemModule.MARS, mars.identity());
        assertEquals("mars", SolarSystemModule.MARS, "the page's identity, derived from the service's name");
        assertEquals(SolarSystemModule.MARS_MINIMUM_MARK_PX, mars.disc().getBounds2D().getWidth(), 0.05,
                "8°: the 6 px mark - the true disc is under a pixel");
        assertTrue(mars.symbol(), "a cartographic symbol");
        assertFalse(mars.covers(mars.centre().x(), mars.centre().y()), "not opaque");
        assertTrue(mars.pointedAt(mars.centre().x(), mars.centre().y(), 4.0),
                "pointed at as an occurrence (#513, R-5)");
        assertEquals("Mars", mars.name());
        String said = ChartComponent.withBodies("The page.", drawn, ENGLISH);
        assertTrue(said.contains("Mars (a cartographic symbol, not Mars's apparent diameter)."), said);
        ReferenceInk.BodyPlacement norsk = placed(scene, offering(OPPOSITION_2003, false), NORSK).get(0);
        assertEquals("Mars", norsk.name(), "Mars in Norwegian too");
        assertTrue(ChartComponent.withBodies("Siden.", List.of(norsk), NORSK)
                .contains("et kartografisk symbol"), "spoken as a symbol in Norwegian");
    }

    @Test
    void atOneDegreeNearTheClosestOppositionTheTrueDiscIsDrawnPhased() {
        MarsService.MarsObservation m = mars(OPPOSITION_2003);
        ChartScene scene = page(m.astrometricJ2000(), ChartViewState.normalMinimumFieldDegrees());
        ReferenceInk.BodyPlacement mars = placed(scene, offering(OPPOSITION_2003, false), ENGLISH).get(0);
        double width = mars.disc().getBounds2D().getWidth();
        assertTrue(width > SolarSystemModule.MARS_MINIMUM_MARK_PX, "the true disc is larger than the mark: " + width);
        assertFalse(mars.symbol(), "the true disc, not a symbol");
        BufferedImage image = painted(scene, offering(OPPOSITION_2003, false), ChartPalette.WHITE_PAPER, ENGLISH);
        // Measured inside the 1 px outline, which on a 6 px disc is a third of its pixels.
        double[] share = litShare(image, mars.centre().x(), mars.centre().y(), width / 2.0 - 1.0);
        assertTrue(share[1] > 0 && share[0] > 0.9, "near full at opposition: lit share " + share[0]
                + " of " + share[1] + " pixels inside the outline, against k = " + m.illuminatedFraction());
    }

    /** The hour of 1912 the service puts Mars deepest behind the Sun, geocentrically. */
    private static Instant deepestBehindTheSun() {
        Instant best = null;
        double least = 360.0;
        for (Instant day = Instant.parse("1912-01-01T00:00:00Z"); day.isBefore(Instant.parse("1913-01-01T00:00:00Z"));
                day = day.plus(Duration.ofDays(1))) {
            double e = MARS.observeGeocentric(day).elongationDegrees();
            if (e < least) {
                least = e;
                best = day;
            }
        }
        Instant around = best;
        for (int h = -24; h <= 24; h++) {
            Instant t = around.plus(Duration.ofHours(h));
            double e = MARS.observeGeocentric(t).elongationDegrees();
            if (e < least) {
                least = e;
                best = t;
            }
        }
        return best;
    }

    @Test
    void behindTheSunTheMarkIsCoveredAndThePageSaysSo() {
        Instant behind = deepestBehindTheSun();
        MarsService.MarsObservation m = mars(behind);
        assertEquals(MarsService.Phase.BEHIND_SUN, m.phase(), "behind the Sun's disc at " + behind);
        ChartScene scene = page(m.astrometricJ2000(), ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> drawn = placed(scene, offering(behind, true), ENGLISH);
        ReferenceInk.BodyPlacement mars = drawn.stream()
                .filter(b -> SolarSystemModule.MARS.equals(b.identity())).findFirst().orElseThrow();
        assertEquals(SolarSystemModule.MARS_BEHIND, mars.state());
        BufferedImage image = painted(scene, offering(behind, true), ChartPalette.WHITE_PAPER, ENGLISH);
        assertEquals(Color.WHITE.getRGB(), image.getRGB((int) Math.floor(mars.centre().x()),
                (int) Math.floor(mars.centre().y())), "the Sun's ground-filled disc covers it");
        String said = ChartComponent.withBodies("The page.", drawn, ENGLISH);
        assertTrue(said.contains("Mars, behind the Sun"), said);
        assertTrue(ChartComponent.withBodies("Siden.", placed(scene, offering(behind, true), NORSK), NORSK)
                .contains("Mars, bak Solen"), "and in Norwegian");
    }

    @Test
    void outsideItsYearsNothingIsDrawnAndWithoutAServiceNothingIsAsked() {
        OverlayRegistry before = offering(Instant.parse("1899-06-01T00:00:00Z"), false);
        assertTrue(before.collect().isEmpty(), "before 1900 the chart draws nothing for Mars");
        SolarSystemModule bare = new SolarSystemModule(
                () -> new Observer(OSLO_LAT, OSLO_LON, OPPOSITION_2003), () -> SOLAR, () -> false);
        bare.marsShowing(true);
        assertTrue(bare.contributedGeometry().isEmpty(), "a module given no Mars service draws no Mars");
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
