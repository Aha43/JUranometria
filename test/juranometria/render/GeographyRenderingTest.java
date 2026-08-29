package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.GeoSegment;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeographyRenderingTest {

    static final SkyPosition CENTRE = new SkyPosition(83.818667, -5.389667);
    static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT);

    /** A horizontal figure segment passing through the page centre. */
    static final GeoSegment FIGURE = new GeoSegment("Ori",
            new SkyPosition(80.0, -5.389667), new SkyPosition(88.0, -5.389667));
    static final GeoSegment BOUNDARY = new GeoSegment("Ori",
            new SkyPosition(80.0, -2.0), new SkyPosition(88.0, -2.0));
    static final SceneGeography GEOGRAPHY = new SceneGeography(
            List.of(FIGURE), List.of(BOUNDARY), Map.of("Ori", "Orion"));

    private static ChartScene scene(double field, SceneGeography geography) {
        return new ChartScene(new ChartViewport(CENTRE, field, 900, 700),
                List.of(), List.of(), "Geography test", 8.0, null, geography);
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
    }

    @Test
    void theRendererRefusesGeographyOnCloseFieldsEvenIfTheSceneCarriesIt() {
        // The policy is enforced at the renderer too: a hand-built 8-degree
        // scene with geography renders identical to one without.
        assertArrayEquals(
                pixels(RENDERER.renderToImage(scene(8.0, SceneGeography.EMPTY))),
                pixels(RENDERER.renderToImage(scene(8.0, GEOGRAPHY))));
    }

    @Test
    void figuresAndNamesDrawAtTwelveDegreesButBoundariesWait() {
        BufferedImage image = RENDERER.renderToImage(scene(12.0, GEOGRAPHY));
        // The figure passes through the page centre horizontally.
        assertTrue(inkNear(image, 450, 350, 3) > 0,
                "the figure segment leaves ink at the centre");
        boolean quietGrey = false;
        for (int dy = -3; dy <= 3 && !quietGrey; dy++) {
            int rgb = image.getRGB(450, 350 + dy);
            int red = (rgb >> 16) & 0xFF;
            quietGrey = red > 90 && red < 230 && rgb != 0xFFFFFFFF;
        }
        assertTrue(quietGrey, "figure ink is quiet grey, not star black");

        // The boundary at dec -2.0 projects ~3.4 degrees north of centre;
        // at 12 degrees over 900 px that row must stay pure paper.
        BufferedImage without = RENDERER.renderToImage(
                scene(12.0, new SceneGeography(
                        List.of(FIGURE), List.of(), Map.of("Ori", "Orion"))));
        assertArrayEquals(pixels(without),
                pixels(RENDERER.renderToImage(scene(12.0, GEOGRAPHY))),
                "boundaries leave no ink below eighteen degrees");

        // The name draws at the centroid of the visible figure ink.
        BufferedImage named = RENDERER.renderToImage(scene(12.0, GEOGRAPHY));
        BufferedImage anonymous = RENDERER.renderToImage(scene(12.0,
                new SceneGeography(List.of(FIGURE), List.of(), Map.of())));
        assertTrue(countDifferent(named, anonymous) > 20,
                "the ORION name adds text ink beside the figure");
    }

    @Test
    void chartInkIsIdenticalUnderBothApplicationThemes() throws Exception {
        // Sprint 7 finish: the chart owns its palette; geography included,
        // a page renders byte-identically whichever theme the chrome uses.
        // Look-and-feel is process-wide state: restore it afterwards so
        // this structural guarantee never orders other tests (PR #70).
        javax.swing.LookAndFeel previous =
                javax.swing.UIManager.getLookAndFeel();
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
            int[] light = pixels(RENDERER.renderToImage(scene(36.0, GEOGRAPHY)));
            com.formdev.flatlaf.FlatDarkLaf.setup();
            int[] dark = pixels(RENDERER.renderToImage(scene(36.0, GEOGRAPHY)));
            assertArrayEquals(light, dark,
                    "geography ink is the chart's own, independent of theme");
        } finally {
            if (previous != null) {
                javax.swing.UIManager.setLookAndFeel(previous);
            }
        }
    }

    @Test
    void nameOrderIsDeterministicWhateverTheInsertionOrder() {
        // PR #69 review: iteration order reaches the renderer, so
        // overlapping name text must stack identically on every render.
        java.util.Map<String, String> forwards = new java.util.LinkedHashMap<>();
        forwards.put("Ori", "Orion");
        forwards.put("Eri", "Eridanus");
        forwards.put("Lep", "Lepus");
        java.util.Map<String, String> backwards = new java.util.LinkedHashMap<>();
        backwards.put("Lep", "Lepus");
        backwards.put("Eri", "Eridanus");
        backwards.put("Ori", "Orion");
        SceneGeography a = new SceneGeography(List.of(), List.of(), forwards);
        SceneGeography b = new SceneGeography(List.of(), List.of(), backwards);
        assertEquals(List.copyOf(a.latinNames().keySet()),
                List.copyOf(b.latinNames().keySet()),
                "names iterate in one sorted order regardless of insertion");
        assertEquals(List.of("Eri", "Lep", "Ori"),
                List.copyOf(a.latinNames().keySet()));
    }

    @Test
    void boundariesDrawDottedFromEighteenDegrees() {
        BufferedImage with = RENDERER.renderToImage(scene(18.0, GEOGRAPHY));
        BufferedImage without = RENDERER.renderToImage(scene(18.0,
                new SceneGeography(List.of(FIGURE), List.of(),
                        Map.of("Ori", "Orion"))));
        assertTrue(countDifferent(with, without) > 10,
                "the boundary leaves dotted ink at eighteen degrees");
    }

    @Test
    void geographyStaysUnderStarsAndTheTitleBlock() {
        // A bright star on the figure line: its black ink wins by order.
        ChartScene withStar = new ChartScene(
                new ChartViewport(CENTRE, 12.0, 900, 700),
                List.of(new juranometria.chart.Star("test", CENTRE, 2.0)),
                List.of(), "Geography test", 8.0, null, GEOGRAPHY);
        BufferedImage image = RENDERER.renderToImage(withStar);
        assertEquals(0xFF000000, image.getRGB(450, 350),
                "star ink covers the figure line");

        // The title block's paper panel erases the geography under it: a
        // segment crossing the block's interior changes nothing inside it.
        GnomonicProjection projection = new GnomonicProjection(CENTRE);
        ViewportMapping mapping = new ViewportMapping(
                new ChartViewport(CENTRE, 12.0, 900, 700));
        GeoSegment throughBlock = segmentThroughPixel(projection, mapping,
                new PixelPoint(40, 650));
        BufferedImage blocked = RENDERER.renderToImage(scene(12.0,
                new SceneGeography(List.of(throughBlock), List.of(), Map.of())));
        BufferedImage plain = RENDERER.renderToImage(
                scene(12.0, SceneGeography.EMPTY));
        int changedInsideBlock = 0;
        int changedOutside = 0;
        for (int y = 0; y < 700; y++) {
            for (int x = 0; x < 900; x++) {
                if (blocked.getRGB(x, y) == plain.getRGB(x, y)) {
                    continue;
                }
                // Strictly inside the block's interior band.
                if (x >= 20 && x <= 100 && y >= 636 && y <= 680) {
                    changedInsideBlock++;
                } else {
                    changedOutside++;
                }
            }
        }
        assertTrue(changedOutside > 0,
                "premise: the segment leaves ink outside the block");
        assertEquals(0, changedInsideBlock,
                "the title block paints over geography beneath it");
    }

    /** Builds a short horizontal segment whose arc crosses the pixel. */
    private static GeoSegment segmentThroughPixel(GnomonicProjection projection,
                                                  ViewportMapping mapping,
                                                  PixelPoint pixel) {
        // Search declination whose projected row passes through the pixel.
        double bestDec = 0.0;
        double bestError = Double.MAX_VALUE;
        for (double dec = -12.0; dec <= 0.0; dec += 0.01) {
            var plane = projection.project(new SkyPosition(85.5, dec));
            if (plane.isEmpty()) {
                continue;
            }
            double error = Math.abs(mapping.toPixel(plane.get()).y() - pixel.y());
            if (error < bestError) {
                bestError = error;
                bestDec = dec;
            }
        }
        return new GeoSegment("Ori", new SkyPosition(82.0, bestDec),
                new SkyPosition(88.0, bestDec));
    }

    private static int inkNear(BufferedImage image, int x, int y, int radius) {
        int ink = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (image.getRGB(x + dx, y + dy) != 0xFFFFFFFF) {
                    ink++;
                }
            }
        }
        return ink;
    }

    private static int countDifferent(BufferedImage a, BufferedImage b) {
        int[] pa = pixels(a);
        int[] pb = pixels(b);
        int different = 0;
        for (int i = 0; i < pa.length; i++) {
            if (pa[i] != pb[i]) {
                different++;
            }
        }
        return different;
    }
}
