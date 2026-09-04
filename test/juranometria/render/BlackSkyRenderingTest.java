package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.GeoSegment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The black-sky chart at the renderer (Sprint 26, issue #246): the
 * reviewed finite palette, the geometry unchanged across grounds,
 * the faintest inks present rather than assumed, and the white-paper
 * chart byte-identical whether the palette component is stated,
 * defaulted by a legacy constructor, or absent from a store.
 */
class BlackSkyRenderingTest {

    static final SkyPosition CENTRE = new SkyPosition(83.818667, -5.389667);
    static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    static final DeepSkyObject GALAXY = new DeepSkyObject("NGC 1",
            List.of("M 90"), DsoType.GALAXY,
            new SkyPosition(82.0, -3.0), 90.0, 45.0, 30.0, 5.0, 1);
    static final DeepSkyObject NEBULA = new DeepSkyObject("NGC 1976",
            List.of("M 42"), DsoType.NEBULA, CENTRE, 65.0, 60.0, 0.0,
            4.0, 1);
    static final GeoSegment FIGURE = new GeoSegment("Ori",
            new SkyPosition(80.0, -5.389667), new SkyPosition(88.0, -5.389667));
    static final GeoSegment BOUNDARY = new GeoSegment("Ori",
            new SkyPosition(80.0, -1.0), new SkyPosition(88.0, -1.0));
    static final SceneGeography GEOGRAPHY = new SceneGeography(
            List.of(FIGURE), List.of(BOUNDARY), Map.of("Ori", "Orion"));

    private static ChartScene scene() {
        return new ChartScene(new ChartViewport(CENTRE, 18.0, 900, 700),
                List.of(new Star("HIP 1", new SkyPosition(84.5, -4.0), 1.5),
                        new Star("HIP 2", new SkyPosition(85.5, -7.0), 6.0)),
                List.of(GALAXY, NEBULA), "Black sky test", 8.0,
                "NGC 1976", GEOGRAPHY);
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
    }

    private static ChartOptions blackSky() {
        return ChartOptions.DEFAULTS.withPalette(ChartPalette.BLACK_SKY);
    }

    @Test
    void whitePaperIsTheDefaultOnEveryRoute() {
        assertEquals(ChartPalette.WHITE_PAPER,
                ChartOptions.DEFAULTS.palette(),
                "the released chart is white paper");
        assertEquals(ChartPalette.WHITE_PAPER,
                new ChartOptions(true, true, true, true, true, true,
                        true, true, true, true, false, true, true,
                        true, true, true).palette(),
                "a caller of the pre-palette constructor gets the"
                        + " chart it always got");
        ChartScene scene = scene();
        assertArrayEquals(pixels(RENDERER.renderToImage(scene)),
                pixels(RENDERER.renderToImage(scene,
                        ChartOptions.DEFAULTS)),
                "the default page is byte-identical with the palette"
                        + " component present");
    }

    @Test
    void blackSkyDrawsOnlyTheReviewedFinitePalette() {
        BufferedImage page = RENDERER.renderToImage(scene(), blackSky());
        java.util.Map<Integer, Integer> census = new java.util.TreeMap<>();
        for (int rgb : pixels(page)) {
            int r = (rgb >> 16) & 0xff;
            assertTrue(r == ((rgb >> 8) & 0xff) && r == (rgb & 0xff),
                    "every black-sky pixel is grey: "
                            + Integer.toHexString(rgb));
            census.merge(rgb, 1, Integer::sum);
        }
        java.util.Set<Integer> reviewed = java.util.Set.of(
                ChartPalette.BLACK_SKY.ground().getRGB(),
                ChartPalette.BLACK_SKY.starInk().getRGB(),
                ChartPalette.BLACK_SKY.frameInk().getRGB(),
                ChartPalette.BLACK_SKY.textInk().getRGB(),
                ChartPalette.BLACK_SKY.figureInk().getRGB(),
                ChartPalette.BLACK_SKY.boundaryInk().getRGB(),
                ChartPalette.BLACK_SKY.galaxyFill().getRGB(),
                ChartPalette.BLACK_SKY.deepSkyOutline().getRGB(),
                ChartPalette.BLACK_SKY.nebulaOutline().getRGB(),
                ChartPalette.BLACK_SKY.gridInk().getRGB(),
                ChartPalette.BLACK_SKY.gridLabelInk().getRGB());
        int total = page.getWidth() * page.getHeight();
        for (Map.Entry<Integer, Integer> colour : census.entrySet()) {
            if (colour.getValue() >= total * 0.005) {
                assertTrue(reviewed.contains(colour.getKey()),
                        "a bulk colour is a reviewed palette value,"
                                + " never an accidental island: grey "
                                + (colour.getKey() & 0xff) + " on "
                                + colour.getValue() + " pixels");
            }
        }
        int commonest = census.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        assertEquals(ChartPalette.BLACK_SKY.ground().getRGB(), commonest,
                "the sky itself is the black ground");
    }

    @Test
    void blackSkyLeavesTheGeometryUnchanged() {
        ChartScene scene = scene();
        BufferedImage paper = RENDERER.renderToImage(scene,
                ChartOptions.DEFAULTS);
        BufferedImage black = RENDERER.renderToImage(scene, blackSky());
        int paperGround = ChartPalette.WHITE_PAPER.ground().getRGB();
        int blackGround = ChartPalette.BLACK_SKY.ground().getRGB();
        int[] onPaper = pixels(paper);
        int[] onBlack = pixels(black);
        int agree = 0;
        for (int i = 0; i < onPaper.length; i++) {
            if ((onPaper[i] != paperGround) == (onBlack[i] != blackGround)) {
                agree++;
            }
        }
        assertTrue(agree >= onPaper.length * 0.999,
                "the non-ground mask agrees across grounds to the"
                        + " study's measured bound - the palette"
                        + " changes ink, never geometry: "
                        + (onPaper.length - agree) + " of "
                        + onPaper.length + " differ");
        assertEquals(
                RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS).size(),
                RENDERER.drawnMarks(scene, blackSky()).size(),
                "what a reader can point at is the same set of marks");
        assertFalse(java.util.Arrays.equals(onPaper, onBlack),
                "and the intended palette pixels really differ");
    }

    @Test
    void chartInkIsIdenticalUnderBothApplicationThemesOnBlackSky()
            throws Exception {
        // The chart owns its palette on either ground; look-and-feel
        // is process-wide state, put back by the shared guard (#224).
        juranometria.app.SwingSession.restoring(() -> {
            com.formdev.flatlaf.FlatLightLaf.setup();
            int[] light = pixels(RENDERER.renderToImage(scene(),
                    blackSky()));
            com.formdev.flatlaf.FlatDarkLaf.setup();
            int[] dark = pixels(RENDERER.renderToImage(scene(),
                    blackSky()));
            assertArrayEquals(light, dark,
                    "black-sky ink is the chart's own, independent"
                            + " of the application theme");
        });
    }

    @Test
    void theFaintestInksAreOnThePageNotMerelyInTheEnum() {
        // The two palette cases closest to their readability
        // thresholds, measured on rendered ink: a mutant that maps
        // either to the ground would pass every layer test and
        // silently erase the mark's substance.
        BufferedImage page = RENDERER.renderToImage(scene(), blackSky());
        int fill = 0;
        int nebula = 0;
        for (int rgb : pixels(page)) {
            if (rgb == ChartPalette.BLACK_SKY.galaxyFill().getRGB()) {
                fill++;
            }
            if (rgb == ChartPalette.BLACK_SKY.nebulaOutline().getRGB()) {
                nebula++;
            }
        }
        assertTrue(fill > 0, "the galaxy's pale wash is present on"
                + " the black ground");
        assertTrue(nebula > 0, "the nebula box survives above its"
                + " contrast floor");
    }

    @Test
    void theSelectionRingWearsItsBlackSkyInk() {
        ChartScene scene = scene();
        BufferedImage page = RENDERER.renderToImage(scene, blackSky());
        java.awt.Graphics2D g = page.createGraphics();
        try {
            RENDERER.drawSelectionHighlight(g, scene, blackSky(),
                    "NGC 1976");
        } finally {
            g.dispose();
        }
        int ring = 0;
        for (int rgb : pixels(page)) {
            if (rgb == ChartPalette.BLACK_SKY.selectionInk().getRGB()) {
                ring++;
            }
        }
        assertTrue(ring > 0, "the reader's ring is drawn in the"
                + " black-sky selection ink");
    }

    @Test
    void aPaletteChangeNeverRetiresTheTarget() {
        // The dialog reaches the chart through TargetRetirement;
        // choosing a ground is not hiding a family, and must never
        // cost a reader their search.
        assertFalse(juranometria.app.TargetRetirement.retires(scene(),
                        ChartOptions.DEFAULTS, blackSky()),
                "switching to black sky retires nothing");
        assertFalse(juranometria.app.TargetRetirement.retires(scene(),
                        blackSky(), ChartOptions.DEFAULTS),
                "and switching back retires nothing either");
    }
}
