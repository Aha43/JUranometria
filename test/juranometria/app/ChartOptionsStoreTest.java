package juranometria.app;

import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartOptionsStoreTest {

    @Test
    void aPreSplitStoreMigratesThenYieldsToEveryNewerChoice()
            throws Exception {
        // The Sprint 17 migration precedence, end to end: a store
        // written before the split carries only chart.starLabels, so
        // it governs all three identifier layers; the moment a layer
        // is confirmed on its own key, that key decides forever -
        // so a reader who once switched everything off can
        // re-enable Bayer letters alone and have it stick.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            node.put("chart.starLabels", "false");
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            ChartOptions migrated = store.load();
            assertFalse(migrated.starNames(),
                    "the legacy control governs every identifier layer");
            assertFalse(migrated.bayerLetters());
            assertFalse(migrated.flamsteedNumbers());
            assertTrue(migrated.deepSkyObjects(),
                    "and nothing else is affected");

            store.save(new ChartOptions(true, true, true, true, true,
                    false, true, false, true));
            ChartOptions reloaded = store.load();
            assertTrue(reloaded.bayerLetters(),
                    "the re-enabled layer sticks despite the legacy key");
            assertFalse(reloaded.starNames());
            assertFalse(reloaded.flamsteedNumbers());
            assertEquals("false", node.get("chart.starLabels", null),
                    "the legacy key is left in place for older builds");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void theChartGroundPersistsAndFallsBackToWhitePaper()
            throws Exception {
        // Sprint 26, issue #246: the palette is a token, not a flag.
        // A pre-1.7.0 store has no key; unknown or corrupt values
        // mean the released white paper, never a launch failure; and
        // a black-sky choice survives the round trip like any other.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            assertEquals(juranometria.render.ChartPalette.WHITE_PAPER,
                    store.load().palette(),
                    "an upgrading store keeps the chart it left"
                            + " behind");

            store.save(ChartOptions.DEFAULTS.withPalette(
                    juranometria.render.ChartPalette.BLACK_SKY));
            assertEquals(juranometria.render.ChartPalette.BLACK_SKY,
                    store.load().palette(),
                    "a black-sky choice sticks");
            assertEquals("black-sky", node.get("chart.palette", null),
                    "stored as its token");

            node.put("chart.palette", "octarine");
            assertEquals(juranometria.render.ChartPalette.WHITE_PAPER,
                    store.load().palette(),
                    "a corrupt token means white paper, not a"
                            + " failure");
            assertEquals(ChartOptions.DEFAULTS.withPalette(
                            juranometria.render.ChartPalette.BLACK_SKY),
                    ChartOptions.DEFAULTS.withPalette(
                            juranometria.render.ChartPalette.BLACK_SKY)
                            .withFamily(juranometria.render.SymbolFamily
                                    .GALAXIES, true),
                    "withFamily threads the palette rather than"
                            + " dropping it");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void optionsRoundTripAndToleratesTheUnknownWithoutRealPreferences()
            throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            assertEquals(ChartOptions.DEFAULTS, store.load(),
                    "no stored values mean the released defaults");

            ChartOptions mixed = new ChartOptions(true, false, false, true, true);
            store.save(mixed);
            assertEquals(mixed, store.load(), "every option round-trips");

            node.put("chart.deepSkyObjects", "banana");
            assertTrue(store.load().deepSkyObjects(),
                    "a corrupt value means the default on, never a failure");

            node.put("chart.futureOption", "false");
            assertEquals(true, store.load().deepSkyObjects(),
                    "unknown future keys are ignored");

            node.put("chart.constellationFigures", "false");
            ChartOptions loaded = store.load();
            assertFalse(loaded.constellationFigures(),
                    "only the explicit string false disables a layer");

            // The legacy key cannot override this store: saving has
            // already written all three identifier keys, and a
            // present new key always decides (the Sprint 17
            // precedence). Migration is exercised on a store that
            // predates the split, below.
            node.put("chart.starLabels", "false");
            assertTrue(store.load().starNames(),
                    "a present new key outranks the legacy control");

            // Migration: a store written before the grid existed has
            // no chart.equatorialGrid key - it loads as the gate's
            // default (on), never a launch failure.
            assertTrue(store.load().equatorialGrid(),
                    "a missing grid key migrates to the decided default");
            node.put("chart.equatorialGrid", "false");
            assertFalse(store.load().equatorialGrid(),
                    "only the explicit string false disables the grid");
            node.put("chart.equatorialGrid", "banana");
            assertTrue(store.load().equatorialGrid(),
                    "a corrupt grid value means the default, on");
            assertFalse(loaded.effectiveConstellationNames(),
                    "the dependency applies to loaded values too");
        } finally {
            node.removeNode();
        }
    }
}
