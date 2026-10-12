package juranometria.ui;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.VenusService.VenusObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the page draws Venus (Sprint 48, issue #514, under the rulings on
 * #512): rule M - the larger of its true solid-body disc and a 6 px
 * mark, phased either way by the page's own lit region; while the mark
 * stands in for it, a cartographic symbol painted beneath the stars,
 * not opaque, that the page calls a symbol and a reader can still point
 * at; once the true disc reaches the mark, the body itself, opaque over
 * what it covers; in front of the Sun at a transit, its dark disc over
 * the Sun's ring and the page saying so; named in the page's language.
 * Every instant is a cited one: the 2026 greatest eastern elongation
 * and the 2012 transit from the Espenak rows in
 * {@code docs/studies/venus/venus-events.txt}.
 */
class VenusInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final SolarSystemService SERVICE = SolarSystemService.load();
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;

    private static Instant event(String name) throws Exception {
        for (String line : Files.readAllLines(Path.of("docs/studies/venus/venus-events.txt"),
                StandardCharsets.UTF_8)) {
            if (line.startsWith(name + "\t")) {
                String iso = line.split("\t")[1];
                return Instant.parse(iso.length() == 17 ? iso.substring(0, 16) + ":00Z" : iso);
            }
        }
        throw new IllegalStateException("no such cited event: " + name);
    }

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, 8.0, null, null),
                900, 700);
    }

    private static VenusObservation venus(Instant when) {
        return (VenusObservation) SERVICE.observe(Body.VENUS,
                new Observer(OSLO_LAT, OSLO_LON, when));
    }

    /** The module's own contribution for the instant, through the production module. */
    private static OverlayRegistry offering(Instant when, boolean withSun) {
        SolarSystemModule module = new SolarSystemModule(
                () -> new Observer(OSLO_LAT, OSLO_LON, when), () -> SERVICE, () -> false);
        module.venusShowing(true);
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

    /** The page with both layers: the reference layer beneath the marks, the bodies above. */
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
    void atEightDegreesTheMarkStandsInPhasedAndIsASymbol() throws Exception {
        Instant elongation = event("2026-greatest-elongation-east");
        VenusObservation v = venus(elongation);
        ChartScene scene = page(v.astrometricJ2000(), 8.0);
        List<ReferenceInk.BodyPlacement> drawn = placed(scene, offering(elongation, false), ENGLISH);
        assertEquals(1, drawn.size());
        ReferenceInk.BodyPlacement venus = drawn.get(0);
        assertEquals(SolarSystemModule.VENUS, venus.identity());
        assertEquals(SolarSystemModule.VENUS_MINIMUM_MARK_PX,
                venus.disc().getBounds2D().getWidth(), 0.05, "the 6 px mark: the true disc"
                        + " is under a pixel at 8°");
        assertTrue(venus.symbol(), "a cartographic symbol");
        assertFalse(venus.covers(venus.centre().x(), venus.centre().y()),
                "not opaque: a click reaches what is under it, unless Venus is pointed at");
        assertTrue(venus.pointedAt(venus.centre().x(), venus.centre().y(), 4.0),
                "and Venus is pointed at as an occurrence (#513)");
        assertEquals("Venus", venus.name());
        assertEquals(null, venus.state(), "clear of the Sun: no state spoken");
        // Phased: the mark's lit pixels are about k of it, beneath the stars.
        BufferedImage image = painted(scene, offering(elongation, false),
                ChartPalette.WHITE_PAPER, ENGLISH);
        double[] share = litShare(image, venus.centre().x(), venus.centre().y(),
                venus.disc().getBounds2D().getWidth() / 2.0);
        assertTrue(share[1] > 10, "the mark has pixels: " + share[1]);
        assertTrue(Math.abs(share[0] - v.illuminatedFraction()) < 0.25,
                "near half lit at the elongation: drawn " + share[0] + " against k = "
                        + v.illuminatedFraction());
    }

    @Test
    void atTheMinimumFieldTheTrueDiscIsItselfOpaqueAndPhased() throws Exception {
        Instant elongation = event("2026-greatest-elongation-east");
        VenusObservation v = venus(elongation);
        ChartScene scene = page(v.astrometricJ2000(), ChartViewState.normalMinimumFieldDegrees());
        ReferenceInk.BodyPlacement venus = placed(scene, offering(elongation, false), NORSK).get(0);
        assertFalse(venus.symbol(), "about 6.1 px at 1°: the true disc, just over the mark");
        assertTrue(venus.disc().getBounds2D().getWidth() > SolarSystemModule.VENUS_MINIMUM_MARK_PX,
                "drawn at its true size: " + venus.disc().getBounds2D().getWidth());
        assertTrue(venus.covers(venus.centre().x(), venus.centre().y()), "opaque");
        assertEquals("Venus", venus.name(), "named in the page's language");
        BufferedImage image = painted(scene, offering(elongation, false),
                ChartPalette.BLACK_SKY, NORSK);
        double[] share = litShare(image, venus.centre().x(), venus.centre().y(),
                venus.disc().getBounds2D().getWidth() / 2.0);
        assertTrue(Math.abs(share[0] - v.illuminatedFraction()) < 0.3,
                "phased on the black sky too: drawn " + share[0] + " against k = "
                        + v.illuminatedFraction());
    }

    @Test
    void inFrontOfTheSunItsDarkDiscLiesOverTheRingAndThePageSaysSo() throws Exception {
        Instant transit = event("2012-inferior-conjunction");
        VenusObservation v = venus(transit);
        ChartScene scene = page(v.astrometricJ2000(), ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> drawn = placed(scene, offering(transit, true), ENGLISH);
        assertEquals(2, drawn.size(), "the Sun and Venus");
        ReferenceInk.BodyPlacement sun = drawn.get(0);
        ReferenceInk.BodyPlacement venus = drawn.get(1);
        assertEquals(SolarSystemModule.SUN, sun.identity(), "the farther first");
        assertEquals(SolarSystemModule.VENUS, venus.identity(), "Venus, nearer, painted last");
        assertEquals(SolarSystemModule.VENUS_IN_FRONT, venus.state(),
                "the geometric state, for the spoken description (ruling 3 on #512)");
        assertTrue(sun.disc().getBounds2D().contains(venus.disc().getBounds2D()),
                "Venus's disc inside the Sun's");
        assertFalse(venus.symbol(), "14 px at 1°: the true disc");
        BufferedImage image = painted(scene, offering(transit, true),
                ChartPalette.WHITE_PAPER, ENGLISH);
        double[] share = litShare(image, venus.centre().x(), venus.centre().y(),
                venus.disc().getBounds2D().getWidth() / 2.0);
        assertTrue(share[0] < 0.1, "a dark disc, its lit sliver under a pixel: " + share[0]);
        String said = ChartComponent.withBodies("The page.", drawn, ENGLISH);
        assertTrue(said.contains("Venus, in front of the Sun."), said);
    }

    /** The lit share of the pixels inside a disc on white paper, and their count. */
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
