package juranometria.app;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two furniture options (issue #180): what they draw, what they
 * default to, and what an upgrading reader gets.
 */
class ChartFurnitureTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private static ChartScene page() {
        return Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(10.68, 41.27), 8.0, 8.0,
                        null, null), 900, 700);
    }

    private static ChartOptions with(boolean titleBlock,
                                     boolean magnitudeKey) {
        ChartOptions d = ChartOptions.DEFAULTS;
        return new ChartOptions(d.deepSkyObjects(), d.deepSkyLabels(),
                d.constellationFigures(), d.constellationBoundaries(),
                d.constellationNames(), d.starNames(), d.bayerLetters(),
                d.flamsteedNumbers(), d.equatorialGrid(),
                titleBlock, magnitudeKey);
    }

    private static byte[] render(ChartOptions options) throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(
                RENDERER.renderToImage(page(), options), "png", out);
        return out.toByteArray();
    }

    /** Ink in a region of the page, for locating furniture. */
    private static long inkIn(BufferedImage image, int x, int y,
                              int width, int height) {
        long inked = 0;
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) {
                if ((image.getRGB(column, row) & 0xff) < 200) {
                    inked++;
                }
            }
        }
        return inked;
    }

    @Test
    void theReleasedDefaultKeepsItsTitleBlockAndHasNoKey() {
        assertTrue(ChartOptions.DEFAULTS.titleBlock(),
                "the title block draws as it always has");
        assertFalse(ChartOptions.DEFAULTS.magnitudeKey(),
                "and the key waits to be asked for, by the Sprint 20"
                        + " measurement of what it covers");
    }

    @Test
    void allFourCombinationsDrawWhatTheyPromise() {
        ChartScene scene = page();
        int width = scene.viewport().widthPx();
        int height = scene.viewport().heightPx();
        // The two corners the furniture occupies.
        int[] lowerLeft = {8, height - 120, 300, 112};
        int[] upperRight = {width - 200, 8, 192, 90};

        record Case(boolean title, boolean key) { }
        for (Case each : new Case[] {new Case(true, true),
                new Case(true, false), new Case(false, true),
                new Case(false, false)}) {
            BufferedImage page = RENDERER.renderToImage(scene,
                    with(each.title(), each.key()));
            long title = inkIn(page, lowerLeft[0], lowerLeft[1],
                    lowerLeft[2], lowerLeft[3]);
            long key = inkIn(page, upperRight[0], upperRight[1],
                    upperRight[2], upperRight[3]);

            if (each.title()) {
                assertTrue(title > 400,
                        "the title block draws when asked: " + title);
            } else {
                assertTrue(title < 400,
                        "and leaves only sky behind when not: " + title);
            }
            if (each.key()) {
                assertTrue(key > 400,
                        "the key draws when asked: " + key);
            } else {
                assertTrue(key < 400,
                        "and leaves only sky behind when not: " + key);
            }
        }
    }

    @Test
    void theTwoAreIndependent() throws Exception {
        assertNotEquals(render(with(true, true)).length,
                render(with(true, false)).length,
                "the key changes the page on its own");
        byte[] noFurniture = render(with(false, false));
        byte[] keyOnly = render(with(false, true));
        assertFalse(java.util.Arrays.equals(noFurniture, keyOnly),
                "and so does the title block on its own");
    }

    @Test
    void aStarLabelIsNeverPlacedWhereTheKeyWillCoverIt() {
        // The decided precedence: furniture draws last and opaque, so
        // the label pass must not put text under it.
        ChartScene scene = page();
        BufferedImage probe = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = probe.createGraphics();
        try {
            var metrics = g.getFontMetrics(ChartRenderer.labelFont());
            java.awt.Rectangle key =
                    ChartRenderer.magnitudeKeyBounds(metrics, scene);
            var withKey = RENDERER.starLabelPlacements(metrics, scene,
                    with(true, true),
                    new juranometria.render.RegionalDetailPolicy(scene,
                            new juranometria.project.ViewportMapping(
                                    scene.viewport()).pixelsPerPlaneUnit()),
                    new juranometria.project.GnomonicProjection(
                            scene.viewport().centre()),
                    new juranometria.project.ViewportMapping(
                            scene.viewport()));
            for (var placement : withKey) {
                assertFalse(placement.box().intersects(key.x, key.y,
                                key.width, key.height),
                        "no label is placed under the key: "
                                + placement.text());
            }
        } finally {
            g.dispose();
        }
    }

    @Test
    void aStoreFromOneOneZeroKeepsEveryChoiceAndGainsTheDecidedDefaults()
            throws Exception {
        // The complete key set 1.1.0 wrote: nine chart.* keys and no
        // furniture at all.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            node.put("chart.deepSkyObjects", "true");
            node.put("chart.deepSkyLabels", "false");
            node.put("chart.constellationFigures", "true");
            node.put("chart.constellationBoundaries", "false");
            node.put("chart.constellationNames", "true");
            node.put("chart.starNames", "false");
            node.put("chart.bayerLetters", "true");
            node.put("chart.flamsteedNumbers", "false");
            node.put("chart.equatorialGrid", "false");

            ChartOptions loaded = ChartOptionsStore.forNode(node).load();

            assertFalse(loaded.deepSkyLabels(), "every 1.1.0 choice survives");
            assertFalse(loaded.constellationBoundaries());
            assertFalse(loaded.starNames());
            assertTrue(loaded.bayerLetters());
            assertFalse(loaded.equatorialGrid());
            assertTrue(loaded.titleBlock(),
                    "the title block keeps drawing, as it did in 1.1.0");
            assertFalse(loaded.magnitudeKey(),
                    "and the key an upgrading reader never asked for"
                            + " stays off");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void theKeyIsTheOneOptionADamagedValueCannotTurnOn() throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            for (String damaged : new java.util.ArrayList<>(java.util.List.of(
                    "TRUE", "yes", "1", "", "  ", "banana"))) {
                node.put("chart.magnitudeKey", damaged);
                assertFalse(ChartOptionsStore.forNode(node).load()
                                .magnitudeKey(),
                        "only the literal 'true' shows the key, so a"
                                + " damaged store cannot add furniture:"
                                + " '" + damaged + "'");
                node.put("chart.titleBlock", damaged);
                assertTrue(ChartOptionsStore.forNode(node).load()
                                .titleBlock(),
                        "while a damaged title-block value keeps the"
                                + " released default, on: '" + damaged + "'");
            }
            node.put("chart.magnitudeKey", "true");
            assertTrue(ChartOptionsStore.forNode(node).load().magnitudeKey(),
                    "and the reader's real choice is honoured");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void bothFurnitureChoicesRoundTrip() throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            for (boolean title : new boolean[] {true, false}) {
                for (boolean key : new boolean[] {true, false}) {
                    ChartOptions saved = with(title, key);
                    store.save(saved);
                    assertEquals(saved, ChartOptionsStore.forNode(node).load(),
                            "a restart reads back exactly what was"
                                    + " confirmed");
                }
            }
        } finally {
            node.removeNode();
        }
    }
}
