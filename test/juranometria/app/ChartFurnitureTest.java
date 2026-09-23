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

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);

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

    /**
     * The magnitude key wins over grid notation, and only there.
     *
     * <p>The claim this test used to make - that the key and the
     * grid do not compete, because right-ascension figures run along
     * the bottom and declination figures down the left while the key
     * sits upper right - was true only while a figure could live
     * nowhere else. Issue #360 restores figures at the top and right
     * edges when a curve never reaches its preferred one, so they
     * can now meet the key, and the key wins.
     *
     * <p>So the contract is semantic rather than a count. Reserving
     * the key may remove a figure, and may do nothing else:
     *
     * <ul>
     *   <li>every removed figure's exact box intersects the key;</li>
     *   <li>every figure whose box misses the key survives, at the
     *       same place and with the same text;</li>
     *   <li>the key creates no figure and moves none.</li>
     * </ul>
     *
     * <p>The fixture case is `18h` on the polar page at 37.9,
     * +89.26, which is a fallback figure sitting inside the key's
     * rectangle. Being a fallback is <strong>not</strong> the rule -
     * the rule is geometric, and a preferred-edge figure that ever
     * landed under the key would be removed by it too.
     */
    @Test
    void theMagnitudeKeyWinsOverGridNotation() {
        var metrics = juranometria.render.EquatorialGrid.labelMetrics();
        int removedAnywhere = 0;
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
            var page = juranometria.project.DrawnPage.of(scene);
            var free = juranometria.render.EquatorialGrid
                    .gridFor(page, (java.awt.Rectangle) null).labels();
            var kept = juranometria.render.EquatorialGrid
                    .gridFor(page, null, key).labels();
            String at = " at " + where[0] + ", " + where[1];

            // Nothing is created, and nothing moves.
            for (var k : kept) {
                assertTrue(free.stream().anyMatch(f ->
                                f.text().equals(k.text())
                                        && f.x() == k.x() && f.y() == k.y()),
                        "reserving the key creates and moves no figure,"
                                + " but " + k.text() + " appears at "
                                + k.x() + "," + k.y() + at);
            }

            for (var f : free) {
                boolean survives = kept.stream().anyMatch(k ->
                        k.text().equals(f.text())
                                && k.x() == f.x() && k.y() == f.y());
                boolean touchesKey = juranometria.render.EquatorialGrid
                        .labelBounds(f, metrics).intersects(key);
                if (survives) {
                    continue;
                }
                removedAnywhere++;
                assertTrue(touchesKey,
                        "a figure is removed only where its own box"
                                + " meets the key, and " + f.text()
                                + " does not" + at);
            }
            for (var f : free) {
                boolean touchesKey = juranometria.render.EquatorialGrid
                        .labelBounds(f, metrics).intersects(key);
                if (touchesKey) {
                    continue;
                }
                assertTrue(kept.stream().anyMatch(k ->
                                k.text().equals(f.text())
                                        && k.x() == f.x() && k.y() == f.y()),
                        "a figure clear of the key is untouched by it,"
                                + " but " + f.text() + " went missing"
                                + at);
            }
        }
        assertTrue(removedAnywhere > 0,
                "the premise: somewhere in this matrix the key really"
                        + " does take a figure, or this contract is"
                        + " passing on an empty hand");
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
        var unreserved = juranometria.render.EquatorialGrid.gridFor(juranometria.project.DrawnPage.of(scene), (java.awt.Rectangle) null);
        java.awt.Rectangle overTheBottomEdge = new java.awt.Rectangle(
                0, scene.viewport().heightPx() - 40,
                scene.viewport().widthPx(), 40);

        var reserved = juranometria.render.EquatorialGrid.gridFor(juranometria.project.DrawnPage.of(scene), null, overTheBottomEdge);

        assertTrue(reserved.labels().size() < unreserved.labels().size(),
                "furniture over the labels suppresses them: "
                        + unreserved.labels().size() + " unreserved, "
                        + reserved.labels().size() + " reserved");
        assertEquals(unreserved.labels().size(),
                juranometria.render.EquatorialGrid.gridFor(juranometria.project.DrawnPage.of(scene), null, null).labels().size(),
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
