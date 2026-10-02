package juranometria.ui;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Whether the reader last locked zoom (Sprint 38, issue #428), kept as
 * the Sun's and the Moon's switches are kept: a preference that says
 * yes, no, or was never written.
 *
 * <p>The released default is unlocked: a fresh profile zooms with the
 * wheel exactly as before. Anything unreadable is treated as never
 * chosen rather than as one of the two answers.
 */
public interface ZoomLockStore {

    boolean DEFAULT_LOCKED = false;

    Optional<Boolean> locked();

    void save(boolean locked);

    default boolean lockedOrDefault() {
        return locked().orElse(DEFAULT_LOCKED);
    }

    void flush();

    static ZoomLockStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    static ZoomLockStore forNode(Preferences node) {
        if (node == null) {
            throw new IllegalArgumentException("a store is kept somewhere");
        }
        return new ZoomLockStore() {
            private static final String KEY = "zoomLocked";

            @Override
            public Optional<Boolean> locked() {
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
            public void save(boolean locked) {
                node.put(KEY, Boolean.toString(locked));
            }

            @Override
            public void flush() {
                try {
                    node.flush();
                } catch (java.util.prefs.BackingStoreException failure) {
                    throw new IllegalStateException(
                            "the zoom-lock preference could not be"
                                    + " written", failure);
                }
            }
        };
    }
}
