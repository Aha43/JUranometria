package juranometria.ui.solar;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Whether the reader last chose to see the Moon on the chart (Sprint
 * 37, issue #416), kept exactly as the Sun's choice is kept
 * ({@link SunChartStore}): a preference that says yes, no, or was
 * never written.
 *
 * <p>The released default is hidden: a star atlas does not put the
 * Moon on every page unasked, and a reader who never chose gets the
 * page they had. Anything unreadable is treated as never chosen rather
 * than as one of the two answers.
 */
public interface MoonChartStore {

    boolean DEFAULT_SHOWN = false;

    Optional<Boolean> shown();

    void save(boolean shown);

    default boolean shownOrDefault() {
        return shown().orElse(DEFAULT_SHOWN);
    }

    void flush();

    static MoonChartStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    static MoonChartStore forNode(Preferences node) {
        if (node == null) {
            throw new IllegalArgumentException("a store is kept somewhere");
        }
        return new MoonChartStore() {
            private static final String KEY = "moonOnChartShown";

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
                            "the Moon-on-chart preference could not be"
                                    + " written", failure);
                }
            }
        };
    }
}
