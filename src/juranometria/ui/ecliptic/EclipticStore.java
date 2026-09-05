package juranometria.ui.ecliptic;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Whether the reader wants the ecliptic drawn, remembered (Sprint 28,
 * issue #274).
 *
 * <p>The gate decided this one preference and no other
 * (docs/decisions/ecliptic.md). Whether a reader wants the ecliptic
 * shown is a stable display choice, like the palette or the grid -
 * unlike place-and-time's <em>instant</em>, a frozen snapshot that
 * would come back stale. The ecliptic has no date and no observer,
 * so there is nothing else here to store.
 *
 * <h2>Three states, kept apart</h2>
 *
 * <p>{@link #shown()} answers with an {@link Optional}, not a boolean
 * with a default baked into the read:
 *
 * <ul>
 *   <li><strong>empty</strong> - the reader has never chosen, and
 *       gets the released default;</li>
 *   <li><strong>false</strong> - the reader chose to hide it;</li>
 *   <li><strong>true</strong> - the reader chose to show it.</li>
 * </ul>
 *
 * <p>The first two draw the same chart today, and they are still not
 * collapsed: a stated choice must outlive a change of default, and
 * keeping them apart is what lets the module be removed and returned
 * without silently resetting what the reader asked for.
 *
 * <p>Same discipline as {@code ChartOptionsStore} and
 * {@code PlaceStore}: an interface over an explicit node, so tests
 * use dedicated nodes and never the developer's real preferences.
 */
public interface EclipticStore {

    /**
     * The released default, for a reader who has never chosen.
     *
     * <p><strong>Hidden.</strong> The atlas had already decided this
     * once - the meridian module is attached at startup and
     * immediately told to show nothing - and for a removable module
     * there is a second reason: if the ecliptic defaulted to shown,
     * installing the module would change the default page for every
     * reader who never asked, which is the module making a decision
     * about the atlas rather than offering one to the reader.
     */
    boolean DEFAULT_SHOWN = false;

    /** What the reader chose, or empty if they never have. */
    Optional<Boolean> shown();

    /** Remembers this choice. */
    void save(boolean shown);

    /**
     * What to draw: the reader's choice if they made one, and the
     * released default if they did not.
     */
    default boolean shownOrDefault() {
        return shown().orElse(DEFAULT_SHOWN);
    }

    /**
     * Pushes what has been saved to the backing store, so a fresh
     * session can read it.
     */
    void flush();

    /** The store the running application uses. */
    static EclipticStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    /** An implementation over an explicit node; tests use a test node. */
    static EclipticStore forNode(Preferences node) {
        if (node == null) {
            throw new IllegalArgumentException(
                    "a store is kept somewhere");
        }
        return new EclipticStore() {

            /**
             * One key, and it is absent until a reader chooses. The
             * absence is the third state, so nothing may write a
             * default into it.
             */
            private static final String KEY = "eclipticShown";

            @Override
            public Optional<Boolean> shown() {
                String stored = node.get(KEY, null);
                if (stored == null) {
                    return Optional.empty();
                }
                // Anything unreadable is treated as never chosen
                // rather than as one of the two answers: a store a
                // reader never wrote cannot be allowed to speak for
                // them.
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
                            "the ecliptic preference could not be"
                                    + " written", failure);
                }
            }
        };
    }
}
