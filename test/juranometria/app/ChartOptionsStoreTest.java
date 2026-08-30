package juranometria.app;

import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartOptionsStoreTest {

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

            node.put("chart.starLabels", "false");
            assertFalse(store.load().starLabels(),
                    "the star-label option persists under its own key");
            assertFalse(loaded.effectiveConstellationNames(),
                    "the dependency applies to loaded values too");
        } finally {
            node.removeNode();
        }
    }
}
