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
import juranometria.solar.MercuryService.MercuryObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the page draws Mercury (Sprint 50, issue #542, under the rulings on
 * #539): rule M as for Venus - the larger of its true solid-body disc and
 * the 6 px mark, phased by the page's own lit region; on the ordinary
 * ladder always the mark, a cartographic symbol beneath the stars, spoken
 * as one, still pointed at; in front of the Sun at a transit painted over
 * the Sun's disc with the state spoken, and with the Sun's centre dot
 * capped (#541) it reads there; behind the Sun covered. Every instant a
 * cited one from {@code docs/studies/mercury/mercury-events.txt}.
 */
class MercuryInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final SolarSystemService SERVICE = SolarSystemService.load();
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;

    private static Instant event(String name) throws Exception {
        for (String line : Files.readAllLines(Path.of("docs/studies/mercury/mercury-events.txt"),
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

    private static MercuryObservation mercury(Instant when) {
        return (MercuryObservation) SERVICE.observe(Body.MERCURY,
                new Observer(OSLO_LAT, OSLO_LON, when));
    }

    /** The module's own contribution for the instant, through the production module. */
    private static OverlayRegistry offering(Instant when, boolean withSun) {
        SolarSystemModule module = new SolarSystemModule(
                () -> new Observer(OSLO_LAT, OSLO_LON, when), () -> SERVICE, () -> false);
        module.mercuryShowing(true);
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
    void atTheOrdinaryFieldsTheMarkStandsInPhasedAndIsASymbol() throws Exception {
        Instant elongation = event("2026-greatest-elongation-east-1");
        MercuryObservation m = mercury(elongation);
        for (double field : new double[] {8.0, ChartViewState.normalMinimumFieldDegrees()}) {
            ChartScene scene = page(m.astrometricJ2000(), field);
            List<ReferenceInk.BodyPlacement> drawn = placed(scene, offering(elongation, false),
                    ENGLISH);
            assertEquals(1, drawn.size());
            ReferenceInk.BodyPlacement mercury = drawn.get(0);
            assertEquals(SolarSystemModule.MERCURY, mercury.identity());
            assertEquals(SolarSystemModule.MERCURY_MINIMUM_MARK_PX,
                    mercury.disc().getBounds2D().getWidth(), 0.05,
                    field + "°: the 6 px mark - the true disc is under it on every ordinary field");
            assertTrue(mercury.symbol(), "a cartographic symbol");
            assertFalse(mercury.covers(mercury.centre().x(), mercury.centre().y()), "not opaque");
            assertTrue(mercury.pointedAt(mercury.centre().x(), mercury.centre().y(), 4.0),
                    "pointed at as an occurrence (#513)");
            assertEquals("Mercury", mercury.name());
            String said = ChartComponent.withBodies("The page.", drawn, ENGLISH);
            assertTrue(said.contains("Mercury (a cartographic symbol, not Mercury's apparent"
                    + " diameter)."), said);
            BufferedImage image = painted(scene, offering(elongation, false),
                    ChartPalette.WHITE_PAPER, ENGLISH);
            double[] share = litShare(image, mercury.centre().x(), mercury.centre().y(),
                    mercury.disc().getBounds2D().getWidth() / 2.0);
            assertTrue(Math.abs(share[0] - m.illuminatedFraction()) < 0.25,
                    field + "°: near half lit, drawn " + share[0] + " against k = "
                            + m.illuminatedFraction());
        }
        ReferenceInk.BodyPlacement norsk = placed(page(m.astrometricJ2000(), 1.0),
                offering(elongation, false), NORSK).get(0);
        assertEquals("Merkur", norsk.name(), "named in the page's language");
    }

    @Test
    void inFrontOfTheSunTheMarkReadsOverTheDiscAndThePageSaysSo() throws Exception {
        Instant transit = event("2019-transit");
        MercuryObservation m = mercury(transit);
        ChartScene scene = page(m.astrometricJ2000(), ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> drawn = placed(scene, offering(transit, true), ENGLISH);
        assertEquals(2, drawn.size(), "the Sun and Mercury");
        ReferenceInk.BodyPlacement sun = drawn.get(0);
        ReferenceInk.BodyPlacement mercury = drawn.get(1);
        assertEquals(SolarSystemModule.SUN, sun.identity(), "the farther first");
        assertEquals(SolarSystemModule.MERCURY_IN_FRONT, mercury.state());
        assertTrue(sun.disc().getBounds2D().contains(mercury.disc().getBounds2D()));
        BufferedImage image = painted(scene, offering(transit, true),
                ChartPalette.WHITE_PAPER, ENGLISH);
        // With the Sun's dot capped (#541) the mark stands on the Sun's ground:
        // its dark tone, not the dot's black, at Mercury's centre.
        int red = new Color(image.getRGB((int) Math.floor(mercury.centre().x()),
                (int) Math.floor(mercury.centre().y()))).getRed();
        assertEquals(Math.round(255 * ReferenceInk.darkSide()), red, 30.0,
                "the dark mark reads on the Sun's disc: " + red);
        String said = ChartComponent.withBodies("The page.", drawn, ENGLISH);
        assertTrue(said.contains("Mercury, in front of the Sun"), said);
    }

    @Test
    void behindTheSunTheMarkIsCoveredAndThePageSaysSo() throws Exception {
        Instant superior = event("2026-superior-conjunction-2");
        MercuryObservation m = mercury(superior);
        ChartScene scene = page(m.astrometricJ2000(), ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> drawn = placed(scene, offering(superior, true), ENGLISH);
        ReferenceInk.BodyPlacement mercury = drawn.stream()
                .filter(b -> SolarSystemModule.MERCURY.equals(b.identity())).findFirst().orElseThrow();
        assertEquals(SolarSystemModule.MERCURY_BEHIND, mercury.state());
        BufferedImage image = painted(scene, offering(superior, true),
                ChartPalette.WHITE_PAPER, ENGLISH);
        assertEquals(Color.WHITE.getRGB(), image.getRGB((int) Math.floor(mercury.centre().x()),
                        (int) Math.floor(mercury.centre().y())),
                "the Sun's ground-filled disc covers it");
        String said = ChartComponent.withBodies("The page.", drawn, ENGLISH);
        assertTrue(said.contains("Mercury, behind the Sun"), said);
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
