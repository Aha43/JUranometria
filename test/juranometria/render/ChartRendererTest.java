package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartRendererTest {

    static final SkyPosition M31_CENTRE = new SkyPosition(10.684708, 41.268750);
    static final Star NU_AND = new Star("nu And", new SkyPosition(12.453526, 41.078911), 4.53);

    static final ChartViewport VIEWPORT = new ChartViewport(M31_CENTRE, 8.0, 900, 700);
    static final ChartScene SCENE =
            new ChartScene(VIEWPORT, List.of(NU_AND), List.of(), "Test chart", 8.0);

    static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT);

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
    }

    @Test
    void renderingTheSameSceneTwiceIsPixelIdentical() {
        assertArrayEquals(pixels(RENDERER.renderToImage(SCENE)),
                pixels(RENDERER.renderToImage(SCENE)));
    }

    @Test
    void paperInsideTheFrameIsWhite() {
        BufferedImage image = RENDERER.renderToImage(SCENE);
        assertEquals(0xFFFFFFFF, image.getRGB(10, 10));
        assertEquals(0xFFFFFFFF, image.getRGB(889, 10));
    }

    @Test
    void aKnownStarLandsAsInkAtItsProjectedPixel() {
        BufferedImage image = RENDERER.renderToImage(SCENE);
        PixelPoint pixel = new ViewportMapping(VIEWPORT)
                .toPixel(new GnomonicProjection(M31_CENTRE)
                        .project(NU_AND.position()).orElseThrow());
        int rgb = image.getRGB((int) Math.round(pixel.x()), (int) Math.round(pixel.y()));
        int red = (rgb >> 16) & 0xFF;
        assertTrue(red < 64, "star centre must be near-black ink, got 0x"
                + Integer.toHexString(rgb));
    }

    @Test
    void starsBehindTheTangentPlaneAreSkippedNotFailed() {
        Star antipode = new Star("antipode",
                new SkyPosition(190.684708, -41.268750), 1.0);
        ChartScene scene =
                new ChartScene(VIEWPORT, List.of(antipode), List.of(), "Test chart", 8.0);
        BufferedImage image = RENDERER.renderToImage(scene);
        assertEquals(0xFFFFFFFF, image.getRGB(450, 350),
                "an unprojectable star must simply not be drawn");
    }

    @Test
    void galaxyEllipsesFollowTheirPositionAngle() {
        // Position angle 90 degrees points the major axis east, which is
        // horizontal on the chart: a 60' x 20' galaxy at the centre must be
        // filled 40 px sideways but not 30 px above the centre.
        DeepSkyObject galaxy = new DeepSkyObject("TEST", List.of(), DsoType.GALAXY,
                M31_CENTRE, 60.0, 20.0, 90.0, 5.0, 1);
        ChartScene scene =
                new ChartScene(VIEWPORT, List.of(), List.of(galaxy), "Test chart", 8.0);
        BufferedImage image = RENDERER.renderToImage(scene);
        assertTrue(image.getRGB(450 - 40, 350) != 0xFFFFFFFF,
                "inside the major axis must be filled");
        assertTrue(image.getRGB(450 + 40, 350) != 0xFFFFFFFF,
                "inside the major axis must be filled");
        assertEquals(0xFFFFFFFF, image.getRGB(450, 350 - 30),
                "beyond the minor axis must stay paper");
    }

    @Test
    void labelsPreferTheMessierNameAndSkipLowPriorityObjects() {
        DeepSkyObject m31Like = new DeepSkyObject("NGC 224",
                List.of("M 31", "Andromeda Galaxy"), DsoType.GALAXY,
                M31_CENTRE, 60.0, 20.0, 90.0, 3.4, 1);
        assertEquals("M 31", ChartRenderer.labelFor(m31Like));
        DeepSkyObject unnamed = new DeepSkyObject("NGC 404", List.of(), DsoType.GALAXY,
                M31_CENTRE, 60.0, 20.0, 90.0, 10.0, 2);
        assertEquals("NGC 404", ChartRenderer.labelFor(unnamed));

        // A priority-2 galaxy draws its symbol but no label beside it.
        ChartScene scene =
                new ChartScene(VIEWPORT, List.of(), List.of(unnamed), "Test chart", 8.0);
        BufferedImage image = RENDERER.renderToImage(scene);
        assertTrue(image.getRGB(450, 350) != 0xFFFFFFFF, "the symbol still draws");
        assertEquals(0, countInk(image, 450 + 62, 335, 60, 30),
                "no label ink beyond the symbol for low-priority objects");
    }

    @Test
    void everyDsoTypeMapsToItsConventionSymbol() {
        assertEquals(ChartRenderer.Symbol.ELLIPSE, symbolOf(DsoType.GALAXY));
        assertEquals(ChartRenderer.Symbol.ELLIPSE, symbolOf(DsoType.GALAXY_PAIR));
        assertEquals(ChartRenderer.Symbol.DOTTED_CIRCLE, symbolOf(DsoType.OPEN_CLUSTER));
        assertEquals(ChartRenderer.Symbol.CROSSED_CIRCLE,
                symbolOf(DsoType.GLOBULAR_CLUSTER));
        assertEquals(ChartRenderer.Symbol.BOX, symbolOf(DsoType.CLUSTER_WITH_NEBULA));
        assertEquals(ChartRenderer.Symbol.BOX, symbolOf(DsoType.EMISSION_NEBULA));
        assertEquals(ChartRenderer.Symbol.PLANETARY, symbolOf(DsoType.PLANETARY_NEBULA));
        assertEquals(ChartRenderer.Symbol.NONE, symbolOf(DsoType.STAR));
        assertEquals(ChartRenderer.Symbol.NONE, symbolOf(DsoType.STELLAR_ASSOCIATION));
    }

    private static ChartRenderer.Symbol symbolOf(DsoType type) {
        return ChartRenderer.symbolFor(new DeepSkyObject("T", List.of(), type,
                M31_CENTRE, 30.0, 20.0, 0.0, 5.0, 2));
    }

    @Test
    void eachSymbolFamilyLeavesItsCharacteristicInk() {
        // 30x20 arcmin at the centre of the 8-degree viewport: about
        // 56 x 37 px, PA 0 so the major axis is vertical, unrotated.
        assertTrue(inkAt(DsoType.GLOBULAR_CLUSTER, 450, 350),
                "the globular cross passes through the centre");
        assertTrue(inkAt(DsoType.CLUSTER_WITH_NEBULA, 450 + 18, 350 - 28),
                "the nebula box has a corner where the ellipse would not reach");
        assertTrue(inkAt(DsoType.PLANETARY_NEBULA, 450, 350),
                "the planetary cross passes through the centre");
        boolean dottedInk = false;
        for (int dy = -29; dy <= -26 && !dottedInk; dy++) {
            for (int dx = -2; dx <= 2 && !dottedInk; dx++) {
                dottedInk = inkAt(DsoType.OPEN_CLUSTER, 450 + dx, 350 + dy);
            }
        }
        assertTrue(dottedInk, "the dotted circle leaves ink near its top arc");
        assertTrue(!inkAt(DsoType.STELLAR_ASSOCIATION, 450, 350)
                        && !inkAt(DsoType.STAR, 450, 350),
                "undrawn types leave no ink");
    }

    private static boolean inkAt(DsoType type, int x, int y) {
        DeepSkyObject dso = new DeepSkyObject("T", List.of(), type,
                M31_CENTRE, 30.0, 20.0, 0.0, 5.0, 2);
        ChartScene scene = new ChartScene(VIEWPORT, List.of(), List.of(dso),
                "Test chart", 8.0);
        return RENDERER.renderToImage(scene).getRGB(x, y) != 0xFFFFFFFF;
    }

    @Test
    void tinyGalaxiesGetThePracticalMinimumSymbol() {
        DeepSkyObject speck = new DeepSkyObject("SPECK", List.of(), DsoType.GALAXY,
                M31_CENTRE, 0.2, 0.1, 0.0, 9.0, 2);
        ChartScene scene =
                new ChartScene(VIEWPORT, List.of(), List.of(speck), "Test chart", 8.0);
        BufferedImage image = RENDERER.renderToImage(scene);
        assertTrue(image.getRGB(450, 350) != 0xFFFFFFFF,
                "a below-minimum galaxy must still be visibly drawn");
    }

    @Test
    void labelsAndTitleBlockLeaveInkWhereExpected(){
        DeepSkyObject galaxy = new DeepSkyObject("TEST", List.of(), DsoType.GALAXY,
                M31_CENTRE, 60.0, 20.0, 90.0, 5.0, 1);
        ChartScene scene =
                new ChartScene(VIEWPORT, List.of(), List.of(galaxy), "Test chart", 8.0);
        BufferedImage image = RENDERER.renderToImage(scene);
        assertTrue(countInk(image, 450 + 45, 335, 60, 30) > 10,
                "the label must sit right of the symbol");
        assertTrue(countInk(image, 12, 700 - 12 - 70, 220, 70) > 100,
                "the title block must occupy the lower-left corner");
    }

    @Test
    void theRegionalPolicyGovernsInkAtWideFields() {
        // Issue #56: at 24 degrees a 2-arcmin faint galaxy is far below
        // the practical minimum. It vanishes entirely - the page with it
        // is pixel-identical to the page without - unless it is the
        // searched target, in which case it is drawn clamped and labelled.
        ChartViewport wide = new ChartViewport(M31_CENTRE, 24.0, 900, 700);
        DeepSkyObject speck = new DeepSkyObject("PGC 1234", List.of(),
                DsoType.GALAXY, M31_CENTRE, 2.0, 1.0, 0.0, 14.0, 3);

        BufferedImage without = RENDERER.renderToImage(new ChartScene(
                wide, List.of(), List.of(), "Test chart", 8.0));
        BufferedImage with = RENDERER.renderToImage(new ChartScene(
                wide, List.of(), List.of(speck), "Test chart", 8.0));
        assertArrayEquals(pixels(without), pixels(with),
                "a sub-minimum faint object leaves no ink at regional fields");

        BufferedImage asTarget = RENDERER.renderToImage(new ChartScene(
                wide, List.of(), List.of(speck), "Test chart", 8.0, "PGC 1234"));
        assertTrue(asTarget.getRGB(450, 350) != 0xFFFFFFFF,
                "the searched target is drawn clamped at the centre");
        assertTrue(countInk(asTarget, 450 + 8, 335, 80, 30) > 10,
                "the searched target carries its label");
    }

    @Test
    void aClampedMessierSymbolDrawsButFallsSilentRegionally() {
        // The M32/M110 cure: priority-1 ink survives at 36 degrees, its
        // label does not, because the symbol is clamp-inflated not true-size.
        ChartViewport wide = new ChartViewport(M31_CENTRE, 36.0, 900, 700);
        DeepSkyObject m32Like = new DeepSkyObject("NGC 221", List.of("M 32"),
                DsoType.GALAXY, M31_CENTRE, 6.5, 5.4, 0.0, 8.1, 1);
        BufferedImage image = RENDERER.renderToImage(new ChartScene(
                wide, List.of(), List.of(m32Like), "Test chart", 8.0));
        assertTrue(image.getRGB(450, 350) != 0xFFFFFFFF,
                "the Messier symbol is always drawn, clamped when necessary");
        assertEquals(0, countInk(image, 450 + 10, 335, 80, 30),
                "no label beside a clamp-inflated Messier symbol");

        BufferedImage classic = RENDERER.renderToImage(new ChartScene(
                new ChartViewport(M31_CENTRE, 8.0, 900, 700),
                List.of(), List.of(m32Like), "Test chart", 8.0));
        assertTrue(countInk(classic, 450 + 10, 335, 80, 30) > 10,
                "the same object keeps its label at the released fields");
    }

    @Test
    void starsFainterThanTheSceneLimitAreNotDrawn() {
        Star atLimit = new Star("at limit", M31_CENTRE, 8.0);
        Star beyondLimit = new Star("beyond limit", M31_CENTRE, 8.01);

        ChartScene faintScene = new ChartScene(
                VIEWPORT, List.of(beyondLimit), List.of(), "Test chart", 8.0);
        assertEquals(0xFFFFFFFF, RENDERER.renderToImage(faintScene).getRGB(450, 350),
                "a star fainter than the stated limit must not be drawn");

        ChartScene limitScene = new ChartScene(
                VIEWPORT, List.of(atLimit), List.of(), "Test chart", 8.0);
        assertTrue(RENDERER.renderToImage(limitScene).getRGB(450, 350) != 0xFFFFFFFF,
                "a star exactly at the stated limit is still drawn");
    }

    @Test
    void anIntermediateMagnitudeLimitCullsHonestly() {
        // A V 6.0 scene: a 5.5 star at the centre draws, a 6.5 star does not,
        // and the field width is untouched by the magnitude change.
        Star bright = new Star("bright", M31_CENTRE, 5.5);
        Star faint = new Star("faint", M31_CENTRE, 6.5);

        ChartScene brightScene = new ChartScene(
                VIEWPORT, List.of(bright), List.of(), "Test chart", 6.0);
        assertTrue(RENDERER.renderToImage(brightScene).getRGB(450, 350) != 0xFFFFFFFF);

        ChartScene faintScene = new ChartScene(
                VIEWPORT, List.of(faint), List.of(), "Test chart", 6.0);
        assertEquals(0xFFFFFFFF, RENDERER.renderToImage(faintScene).getRGB(450, 350));
        assertEquals(8.0, faintScene.viewport().fieldWidthDegrees());
    }

    @Test
    void chartNotationIsIndependentOfTheDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("nb-NO"));
            int[] norwegian = pixels(RENDERER.renderToImage(SCENE));
            Locale.setDefault(Locale.US);
            int[] us = pixels(RENDERER.renderToImage(SCENE));
            assertArrayEquals(norwegian, us,
                    "rendering must not depend on the machine locale");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void titleBlockIsOmittedWhenTheViewportCannotHoldIt() {
        ChartViewport tiny = new ChartViewport(M31_CENTRE, 8.0, 120, 90);
        ChartScene scene = new ChartScene(tiny, List.of(), List.of(), "Test chart", 8.0);
        BufferedImage image = RENDERER.renderToImage(scene);
        assertEquals(0, countInk(image, 3, 3, 114, 84),
                "a viewport too small for the title block omits it instead of clipping");
    }

    /**
     * Counts CONTENT ink: pixels meaningfully darker than the
     * equatorial grid's quiet band (lines 216, labels 150), so the
     * always-on graticule of docs/decisions/coordinate-grid.md never
     * counts as symbols, labels, or title ink in these assertions.
     */
    private static int countInk(BufferedImage image, int x, int y, int w, int h) {
        int ink = 0;
        for (int px = x; px < x + w; px++) {
            for (int py = y; py < y + h; py++) {
                if (((image.getRGB(px, py) >> 16) & 0xFF) < 140) {
                    ink++;
                }
            }
        }
        return ink;
    }
}
