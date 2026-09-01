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
                    RENDERER.magnitudeKeyBounds(metrics, scene);
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
    void switchingTheTitleBlockOffGivesBackTheGridLabelsItWasHiding() {
        // Review, P2: the renderer always reserved the title
        // rectangle from grid notation, so switching the block off
        // left a hole in the labels where it used to be - the block
        // vanished but its collision reservation did not.
        ChartScene scene = page();
        java.awt.Rectangle title;
        BufferedImage probe = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = probe.createGraphics();
        try {
            title = ChartRenderer.titleBlockBounds(g, scene);
        } finally {
            g.dispose();
        }
        assertTrue(title != null, "the page has a title block to hide");

        var withBlock = juranometria.render.EquatorialGrid.gridFor(
                scene.viewport(), title);
        var withoutBlock = juranometria.render.EquatorialGrid.gridFor(
                scene.viewport(), (java.awt.Rectangle) null);

        assertTrue(withoutBlock.labels().size() > withBlock.labels().size(),
                "the block really was suppressing labels: "
                        + withBlock.labels().size() + " with it, "
                        + withoutBlock.labels().size() + " without");

        // And the rendered page agrees: with the block off, ink
        // appears where the block used to suppress it.
        long inkWithBlock = inkIn(RENDERER.renderToImage(scene,
                        with(true, false)),
                title.x, title.y, title.width, title.height);
        long inkWithout = inkIn(RENDERER.renderToImage(scene,
                        with(false, false)),
                title.x, title.y, title.width, title.height);
        assertTrue(inkWithout > 0,
                "the freed area carries grid notation again: "
                        + inkWithout + " px");
        assertTrue(inkWithBlock > inkWithout,
                "while the block itself is the heavier ink: "
                        + inkWithBlock + " against " + inkWithout);
    }

    @Test
    void theKeyStandsClearOfTheGridsOwnLabels() {
        // The claim worth making about the key and the grid, and the
        // one an earlier version of this test did not make: they do
        // not compete. Right-ascension labels run along the bottom
        // and declination labels down the left, so the upper-right
        // key covers none of them - which is why the placement was
        // chosen. This fails if either the key or the labels move.
        for (double[] where : new double[][] {{10.68, 41.27, 8.0},
                {83.8, 0.0, 36.0}, {37.9, 89.26, 18.0},
                {186.6, -60.0, 18.0}}) {
            ChartScene scene = Atlas.assembler().assemble(
                    new ChartViewState(new SkyPosition(where[0], where[1]),
                            where[2], 8.0, null, null), 900, 700);
            java.awt.Rectangle key;
            BufferedImage probe = new BufferedImage(1, 1,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = probe.createGraphics();
            try {
                key = RENDERER.magnitudeKeyBounds(
                        g.getFontMetrics(ChartRenderer.labelFont()), scene);
            } finally {
                g.dispose();
            }
            var unreserved = juranometria.render.EquatorialGrid.gridFor(
                    scene.viewport(), (java.awt.Rectangle) null);
            var reserved = juranometria.render.EquatorialGrid.gridFor(
                    scene.viewport(), null, key);
            assertEquals(unreserved.labels().size(),
                    reserved.labels().size(),
                    "the key suppresses no grid label, because it stands"
                            + " clear of them - at " + where[0] + ", "
                            + where[1]);
        }
    }

    @Test
    void furnitureAnywhereOnThePageSuppressesTheLabelsBeneathIt() {
        // The reservation itself, proved where it can actually bite:
        // a box laid over the bottom edge, where the right-ascension
        // labels run. The key's own slot in the reservation list is
        // the one used, so what is tested is the path the key takes -
        // not a claim that the key happens to overlap something,
        // which on a real page it never does (sprint review).
        ChartScene scene = page();
        var unreserved = juranometria.render.EquatorialGrid.gridFor(
                scene.viewport(), (java.awt.Rectangle) null);
        java.awt.Rectangle overTheBottomEdge = new java.awt.Rectangle(
                0, scene.viewport().heightPx() - 40,
                scene.viewport().widthPx(), 40);

        var reserved = juranometria.render.EquatorialGrid.gridFor(
                scene.viewport(), null, overTheBottomEdge);

        assertTrue(reserved.labels().size() < unreserved.labels().size(),
                "furniture over the labels suppresses them: "
                        + unreserved.labels().size() + " unreserved, "
                        + reserved.labels().size() + " reserved");
        assertEquals(unreserved.labels().size(),
                juranometria.render.EquatorialGrid.gridFor(
                        scene.viewport(), null, null).labels().size(),
                "and reserving nothing suppresses nothing");
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
