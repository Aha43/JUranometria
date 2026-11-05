package juranometria.ui.selection;

import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Where the selected-object box's one choice is remembered (#692, ruled
 * C-7 on #690): a store of its own, as the ecliptic's is, so the chart's
 * options and every sheet's canonical options stay as they were. On by
 * default; a value a reader never wrote is never chosen for them.
 */
public interface SelectedObjectStore {

    /** On until a reader turns it off (C-7). */
    boolean DEFAULT_SHOWN = true;

    /** The remembered choice, or empty if none was ever made. */
    Optional<Boolean> shown();

    /** Remembers the choice. */
    void save(boolean shown);

    /** The remembered choice, or the default. */
    default boolean shownOrDefault() {
        return shown().orElse(DEFAULT_SHOWN);
    }

    /** Writes through to the platform store, where there is one. */
    void flush();

    /** A store that remembers nothing between processes; for tests and studies. */
    static SelectedObjectStore inMemory() {
        return new SelectedObjectStore() {
            private Optional<Boolean> kept = Optional.empty();

            @Override
            public Optional<Boolean> shown() {
                return kept;
            }

            @Override
            public void save(boolean shown) {
                kept = Optional.of(shown);
            }

            @Override
            public void flush() {
            }
        };
    }

    /** The reader's own store. */
    static SelectedObjectStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    /** A store in the given node. */
    static SelectedObjectStore forNode(Preferences node) {
        if (node == null) {
            throw new IllegalArgumentException("a store is kept somewhere");
        }
        return new SelectedObjectStore() {

            private static final String KEY = "selectedObjectBox";

            @Override
            public Optional<Boolean> shown() {
                String stored = node.get(KEY, null);
                if ("true".equals(stored)) {
                    return Optional.of(true);
                }
                if ("false".equals(stored)) {
                    return Optional.of(false);
                }
                // Anything unreadable is never chosen, not one of the answers.
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
                } catch (BackingStoreException ignored) {
                    // A store that cannot write keeps the session's choice.
                }
            }
        };
    }
}
