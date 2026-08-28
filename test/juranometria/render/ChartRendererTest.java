package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
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
    static final ChartScene SCENE = new ChartScene(VIEWPORT, List.of(NU_AND));

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
        assertEquals(0xFFFFFFFF, image.getRGB(889, 689));
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
        ChartScene scene = new ChartScene(VIEWPORT, List.of(antipode));
        BufferedImage image = RENDERER.renderToImage(scene);
        assertEquals(0xFFFFFFFF, image.getRGB(450, 350),
                "an unprojectable star must simply not be drawn");
    }
}
