package juranometria.ui.solar;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Whether the reader last chose to see the Sun on the chart (Sprint
 * 37, issue #415), kept the way the ecliptic's choice is kept: a
 * preference that says yes, no, or was never written.
 *
 * <p>The released default is hidden: a star atlas does not put the
 * Sun on every page unasked, and a reader who never chose gets the
 * page they had. Anything unreadable is treated as never chosen rather
 * than as one of the two answers.
 */
public interface SunChartStore {

    boolean DEFAULT_SHOWN = false;

    Optional<Boolean> shown();

    void save(boolean shown);

    default boolean shownOrDefault() {
        return shown().orElse(DEFAULT_SHOWN);
    }

    void flush();

    static SunChartStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    static SunChartStore forNode(Preferences node) {
        if (node == null) {
            throw new IllegalArgumentException("a store is kept somewhere");
        }
        return new SunChartStore() {
            private static final String KEY = "sunOnChartShown";

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
                            "the Sun-on-chart preference could not be"
                                    + " written", failure);
                }
            }
        };
    }
}
