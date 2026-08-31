package juranometria.app;

import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The upgrade half of the 1.0 contract's preferences promise (issue
 * #145): a store written by an older release loads into 1.0 with
 * documented behaviour, and no stored value - however damaged - can
 * turn a launch into a failure.
 *
 * Each shape below is the complete set of keys the named release
 * actually wrote, so these are upgrades rather than fragments: a
 * reader who last ran 0.13 and one who last ran 0.17 both arrive
 * here.
 */
class PreferenceUpgradeTest {

    /** A store as some earlier release left it, then this node. */
    private static Preferences node() {
        return Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
    }

    private static void writeLayerKeys(Preferences node, String value) {
        node.put("chart.deepSkyObjects", value);
        node.put("chart.deepSkyLabels", value);
        node.put("chart.constellationFigures", value);
        node.put("chart.constellationBoundaries", value);
        node.put("chart.constellationNames", value);
    }

    @Test
    void aStoreFromBeforeTheGridExistedKeepsItsChoicesAndGainsTheDefault()
            throws Exception {
        // v0.13/v0.14 wrote five layer keys plus the single star
        // label control, and no grid key at all.
        Preferences node = node();
        try {
            writeLayerKeys(node, "true");
            node.put("chart.deepSkyLabels", "false");
            node.put("chart.starLabels", "true");

            ChartOptions loaded = ChartOptionsStore.forNode(node).load();

            assertFalse(loaded.deepSkyLabels(),
                    "the reader's own choice survives the upgrade");
            assertTrue(loaded.equatorialGrid(),
                    "a key that release never wrote loads as the"
                            + " decided default, on");
            assertTrue(loaded.starNames(), "and the identifier layers"
                    + " follow the control that release did write");
            assertTrue(loaded.bayerLetters());
            assertTrue(loaded.flamsteedNumbers());
        } finally {
            node.removeNode();
        }
    }

    @Test
    void the015StoreLoadsIntoOneZeroUnchanged() throws Exception {
        // The contract names 0.15 explicitly: five layer keys, the
        // single star-label control, and the grid.
        Preferences node = node();
        try {
            writeLayerKeys(node, "true");
            node.put("chart.constellationFigures", "false");
            node.put("chart.starLabels", "false");
            node.put("chart.equatorialGrid", "false");
            node.put("appearance", "dark");

            ChartOptions loaded = ChartOptionsStore.forNode(node).load();

            assertFalse(loaded.constellationFigures(), "each 0.15 choice"
                    + " loads into 1.0 as the reader left it");
            assertFalse(loaded.equatorialGrid());
            assertTrue(loaded.deepSkyObjects());
            assertFalse(loaded.starNames(), "and the one star-text"
                    + " control governs all three identifier layers");
            assertFalse(loaded.bayerLetters());
            assertFalse(loaded.flamsteedNumbers());
            assertTrue(AppearanceStore.forNode(node).storedDark(),
                    "the appearance choice is untouched by any of it");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void the017StoreLoadsExactlyAsWritten() throws Exception {
        // The current release writes all nine keys, and keeps the
        // legacy control in place for older builds sharing the node.
        Preferences node = node();
        try {
            ChartOptions written = new ChartOptions(true, false, true, true,
                    true, false, true, false, true);
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            store.save(written);
            node.put("chart.starLabels", "false");

            assertEquals(written, store.load(),
                    "every 0.17 key round-trips, and the legacy control"
                            + " cannot reach past keys that exist");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void noStoredValueHoweverDamagedCanFailALaunch() throws Exception {
        Preferences node = node();
        try {
            // Every key damaged in a different way a real store can
            // be damaged: wrong case, wrong type, empty, whitespace,
            // and a value from some other application entirely.
            node.put("chart.deepSkyObjects", "FALSE");
            node.put("chart.deepSkyLabels", "0");
            node.put("chart.constellationFigures", "");
            node.put("chart.constellationBoundaries", "  ");
            node.put("chart.constellationNames", "null");
            node.put("chart.starNames", "yes");
            node.put("chart.bayerLetters", " ");
            node.put("chart.flamsteedNumbers", "false ");
            node.put("chart.equatorialGrid", "TRUE");
            node.put("chart.starLabels", "nonsense");
            node.put("appearance", "chartreuse");

            ChartOptions loaded = ChartOptionsStore.forNode(node).load();

            assertEquals(ChartOptions.DEFAULTS, loaded,
                    "every damaged value means the released default,"
                            + " never a launch failure: " + loaded);
            assertFalse(AppearanceStore.forNode(node).storedDark(),
                    "an unknown appearance is the light default");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void anUnreadableNodeIsAFailureTheLaunchSurfaceCanExplain()
            throws Exception {
        // The one preference failure the store cannot absorb: the
        // backing node itself is gone. It must throw where the
        // launch handler can explain it, not corrupt the reader's
        // settings silently.
        Preferences node = node();
        node.put("chart.deepSkyObjects", "false");
        ChartOptionsStore store = ChartOptionsStore.forNode(node);
        node.removeNode();

        IllegalStateException thrown = org.junit.jupiter.api.Assertions
                .assertThrows(IllegalStateException.class, store::load);
        assertTrue(StartupFailure.message(thrown).contains(
                        "Download the release again"),
                "and the launch surface turns it into an instruction");
    }
}
