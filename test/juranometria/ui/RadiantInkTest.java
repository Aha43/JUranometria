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
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.render.BelowHorizon;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the page draws a meteor shower's radiant (#716, R-13 to R-15 and
 * ruling C-4): six rays about an empty centre, fixed in page pixels at
 * every field, a mark that hides nothing and can be pointed at as an
 * occurrence, named by the page and given the horizon's words when it
 * is below. The place is arbitrary: the page draws whatever radiant it
 * is offered, and no shower's data is needed to prove how.
 */
class RadiantInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    /** An empty stretch of sky, so the mark's own ink is all there is at its centre. */
    private static final SkyPosition PLACE = new SkyPosition(200.0, 30.0);
    private static final OverlayContribution.Moment MOMENT = new OverlayContribution.Moment(
            Instant.parse("2030-08-12T22:00:00Z"), 60.0, 10.0);

    private static ChartScene page(double field) {
        return Atlas.assembler().assemble(new ChartViewState(PLACE, field, 6.0, null, null),
                900, 700);
    }

    private static OverlayRegistry offering(boolean below) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("meteor-showers", () -> List.of(new OverlayContribution.Radiant(
                "meteor-shower.PER", "Perseids", PLACE, below, MOMENT, true)));
        return registry;
    }

    private static List<ReferenceInk.BodyPlacement> placed(ChartScene scene, OverlayRegistry r,
                                                           PageText words) {
        return ReferenceInk.bodyPlacements(DrawnPage.of(scene), r.collect(),
                ChartPalette.WHITE_PAPER, words, List.of());
    }

    private static BufferedImage painted(ChartScene scene, OverlayRegistry registry) {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);
            renderer.render(g, scene, juranometria.render.ChartOptions.DEFAULTS
                            .withPalette(ChartPalette.WHITE_PAPER),
                    (layerG, layerScene, reserved) -> ReferenceInk.paint(layerG, layerScene,
                            registry.collect(), ChartPalette.WHITE_PAPER, ENGLISH, reserved,
                            java.util.Set.of()),
                    (layerG, layerScene, reserved) -> {
                        DrawnPage page = DrawnPage.of(layerScene);
                        ReferenceInk.paintBodies(layerG, page, registry.collect(),
                                ChartPalette.WHITE_PAPER, ENGLISH, reserved,
                                ReferenceInk.referenceBoxes(page, registry.collect(), ENGLISH,
                                        reserved, s -> false));
                    }, null, java.util.Set.of());
        } finally {
            g.dispose();
        }
        return image;
    }

    @Test
    void theMarkIsTheSameSizeOnThePageAtEveryField() {
        for (double field : new double[] {60.0, 24.0, 8.0, 1.0}) {
            ReferenceInk.BodyPlacement radiant = placed(page(field), offering(false), ENGLISH)
                    .get(0);
            assertEquals(2.0 * ReferenceInk.RADIANT_OUTER,
                    radiant.disc().getBounds2D().getWidth(), 1e-9, field + " degrees");
        }
    }

    @Test
    void itIsAMarkThatHidesNothingAndCanBePointedAt() {
        ReferenceInk.BodyPlacement radiant = placed(page(24.0), offering(false), ENGLISH).get(0);
        assertEquals("meteor-shower.PER", radiant.identity());
        assertTrue(radiant.symbol(), "a mark, not a disc");
        assertFalse(radiant.covers(radiant.centre().x(), radiant.centre().y()),
                "a click through it reaches what is under it");
        assertTrue(radiant.pointedAt(radiant.centre().x(), radiant.centre().y(), 4.0));
        assertTrue(radiant.pointedAt(radiant.centre().x() + 8.0, radiant.centre().y(), 4.0),
                "anywhere on the rays");
        assertFalse(radiant.pointedAt(radiant.centre().x() + 20.0, radiant.centre().y(), 4.0));
        assertEquals(MOMENT, radiant.moment(), "the moment travels with the placement");
    }

    @Test
    void sixRaysLeaveTheCentreEmpty() {
        ChartScene scene = page(24.0);
        ReferenceInk.BodyPlacement radiant = placed(scene, offering(false), ENGLISH).get(0);
        assertEquals(6, ReferenceInk.radiantRays(radiant.centre()).size());
        BufferedImage with = painted(scene, offering(false));
        BufferedImage without = painted(scene, new OverlayRegistry());
        int cx = (int) Math.round(radiant.centre().x());
        int cy = (int) Math.round(radiant.centre().y());
        assertEquals(without.getRGB(cx, cy), with.getRGB(cx, cy),
                "nothing is drawn at the radiant's own position");
        // The first ray points straight up: ink between the inner and outer radius above.
        int up = cy - (int) Math.round((ReferenceInk.RADIANT_INNER
                + ReferenceInk.RADIANT_OUTER) / 2.0);
        assertFalse(without.getRGB(cx, up) == with.getRGB(cx, up), "ink on the upward ray");
        // Between two rays, at the same distance, there is none (30 degrees off a ray).
        double r = (ReferenceInk.RADIANT_INNER + ReferenceInk.RADIANT_OUTER) / 2.0;
        int gx = cx + (int) Math.round(r * Math.cos(Math.toRadians(-60.0)));
        int gy = cy + (int) Math.round(r * Math.sin(Math.toRadians(-60.0)));
        assertEquals(without.getRGB(gx, gy), with.getRGB(gx, gy), "and none between rays");
    }

    @Test
    void theNameIsThePagesAndBelowTheHorizonItSaysSoAndIsQuieted() {
        ChartScene scene = page(24.0);
        ReferenceInk.BodyPlacement up = placed(scene, offering(false), ENGLISH).get(0);
        assertEquals("Perseids", up.name());
        assertTrue(up.box() != null, "named on an empty stretch of sky");
        ReferenceInk.BodyPlacement down = placed(scene, offering(true), ENGLISH).get(0);
        assertEquals("Perseids (below the horizon)", down.name());
        assertTrue(down.belowHorizon());
        assertEquals("Perseidene (under horisonten)",
                placed(scene, offering(true), NORSK).get(0).name());

        BufferedImage bright = painted(scene, offering(false));
        BufferedImage quiet = painted(scene, offering(true));
        int cx = (int) Math.round(up.centre().x());
        int y = (int) Math.round(up.centre().y()) - 7;
        Color full = new Color(bright.getRGB(cx, y));
        Color dimmed = new Color(quiet.getRGB(cx, y));
        Color expected = BelowHorizon.subdued(ChartPalette.WHITE_PAPER.textInk(),
                ChartPalette.WHITE_PAPER.ground());
        assertTrue(dimmed.getRed() > full.getRed(), "quieter on white paper: " + full + " "
                + dimmed);
        assertTrue(Math.abs(dimmed.getRed() - expected.getRed()) <= 40,
                "by the horizon's own rule: " + dimmed + " against " + expected);
    }

    @Test
    void theDescriptionSaysWhatTheMarkIs() {
        ChartScene scene = page(24.0);
        String said = ChartComponent.withBodies("The page.",
                placed(scene, offering(true), ENGLISH), ENGLISH);
        assertEquals("The page. Perseids (below the horizon) (the radiant of a meteor"
                + " shower).", said);
        assertTrue(ChartComponent.withBodies("Siden.", placed(scene, offering(false), NORSK),
                NORSK).contains("Perseidene (radianten til en meteorsverm)."));
    }

    @Test
    void aRadiantOffThePageIsNotDrawn() {
        ChartScene elsewhere = Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(20.0, -40.0), 8.0, 6.0, null, null), 900, 700);
        assertTrue(placed(elsewhere, offering(false), ENGLISH).isEmpty());
    }
}
