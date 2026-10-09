package juranometria.ui.solar;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Whether the reader last chose to see Jupiter and its moons on the chart
 * (Sprint 45, issue #484; ruling 1 on #482), kept exactly as the Sun's
 * and the Moon's choices are kept ({@link MoonChartStore}): a preference
 * that says yes, no, or was never written.
 *
 * <p>The default is hidden: a reader who never chose gets the page they
 * had. Anything unreadable is treated as never chosen.
 */
public interface JovianChartStore {

    boolean DEFAULT_SHOWN = false;

    Optional<Boolean> shown();

    void save(boolean shown);

    default boolean shownOrDefault() {
        return shown().orElse(DEFAULT_SHOWN);
    }

    void flush();

    static JovianChartStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    static JovianChartStore forNode(Preferences node) {
        if (node == null) {
            throw new IllegalArgumentException("a store is kept somewhere");
        }
        return new JovianChartStore() {
            private static final String KEY = "jupiter.onChartShown";

            @Override
            public Optional<Boolean> shown() {
                String stored = node.get(KEY, null);
                if ("true".equals(stored)) {
                    return Optional.of(Boolean.TRUE);
                }
                if ("false".equals(stored)) {
                    return Optional.of(Boolean.FALSE);
                }
                return Optional.empty();
            }

            @Override
            public void save(boolean shown) {
                node.put(KEY, Boolean.toString(shown));
            }

            @Override
            public void flush() {
                try {
                    node.flush();
                } catch (java.util.prefs.BackingStoreException failure) {
                    throw new IllegalStateException(
                            "the Jupiter-on-chart preference could not be"
                                    + " written", failure);
                }
            }
        };
    }
}
