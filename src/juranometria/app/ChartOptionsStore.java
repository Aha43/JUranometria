package juranometria.app;

import java.util.prefs.Preferences;

import juranometria.render.ChartOptions;

/**
 * The tiny injectable persistence boundary for chart options (issue
 * #104), following the appearance store's JDK-only pattern: one key
 * per option under the application's preferences node, missing or
 * unknown values falling back to the released default (on), tests
 * using dedicated nodes and never the developer's real preferences.
 * Chart options and navigation persist separately by construction:
 * Home resets navigation only, Restore Defaults changes options only.
 */
public interface ChartOptionsStore {

    ChartOptions load();

    void save(ChartOptions options);

    /** The JDK-preferences implementation used by the application. */
    static ChartOptionsStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    /** An implementation over an explicit node; tests use a test node. */
    static ChartOptionsStore forNode(Preferences node) {
        return new ChartOptionsStore() {
            @Override
            public ChartOptions load() {
                return new ChartOptions(
                        flag(node, "chart.deepSkyObjects"),
                        flag(node, "chart.deepSkyLabels"),
                        flag(node, "chart.constellationFigures"),
                        flag(node, "chart.constellationBoundaries"),
                        flag(node, "chart.constellationNames"),
                        identifierFlag(node, "chart.starNames"),
                        identifierFlag(node, "chart.bayerLetters"),
                        identifierFlag(node, "chart.flamsteedNumbers"),
                        flag(node, "chart.equatorialGrid"));
            }

            @Override
            public void save(ChartOptions options) {
                node.put("chart.deepSkyObjects",
                        Boolean.toString(options.deepSkyObjects()));
                node.put("chart.deepSkyLabels",
                        Boolean.toString(options.deepSkyLabels()));
                node.put("chart.constellationFigures",
                        Boolean.toString(options.constellationFigures()));
                node.put("chart.constellationBoundaries",
                        Boolean.toString(options.constellationBoundaries()));
                node.put("chart.constellationNames",
                        Boolean.toString(options.constellationNames()));
                node.put("chart.starNames",
                        Boolean.toString(options.starNames()));
                node.put("chart.bayerLetters",
                        Boolean.toString(options.bayerLetters()));
                node.put("chart.flamsteedNumbers",
                        Boolean.toString(options.flamsteedNumbers()));
                node.put("chart.equatorialGrid",
                        Boolean.toString(options.equatorialGrid()));
            }
        };
    }

    /**
     * A star-identifier flag with the Sprint 17 migration precedence
     * (docs/decisions/bayer-notation.md): its own key decides
     * whenever present; otherwise the legacy single control
     * {@code chart.starLabels} decides that layer, so a reader who
     * switched all star text off keeps it off; otherwise the
     * released default, on. Confirming the dialog writes all three
     * keys, so from the first confirmation the legacy key can never
     * override a newer choice - and it is left in place rather than
     * deleted, so an older build reading the same store keeps
     * working.
     */
    private static boolean identifierFlag(Preferences node, String key) {
        if (node.get(key, null) != null) {
            return flag(node, key);
        }
        if (node.get("chart.starLabels", null) != null) {
            return flag(node, "chart.starLabels");
        }
        return true;
    }

    /**
     * A stored option flag: only the explicit string "false" disables a
     * layer; anything missing, corrupt, or unknown means the released
     * default (on), never a launch failure.
     */
    private static boolean flag(Preferences node, String key) {
        return !"false".equals(node.get(key, null));
    }
}
