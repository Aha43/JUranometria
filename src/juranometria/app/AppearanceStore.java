package juranometria.app;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * The tiny injectable preference boundary for the appearance setting
 * (issue #99): one value, JDK-only storage, kept out of Swing
 * components so persistence stays testable without touching the
 * developer's real preferences.
 *
 * The precedence rule, recorded here as the single place it lives:
 * the stored choice decides the startup appearance; a {@code --dark}
 * launch argument wins for that session only and never rewrites the
 * stored choice; a missing, corrupt, or unknown stored value means
 * the light default. Only confirming the Settings dialog persists.
 */
public interface AppearanceStore {

    String LIGHT = "light";
    String DARK = "dark";

    /** The raw stored value, if any. */
    Optional<String> load();

    /** Persists an accepted choice ({@link #LIGHT} or {@link #DARK}). */
    void save(String appearance);

    /**
     * Whether the stored value asks for the dark appearance; anything
     * missing, corrupt, or unknown is the light default, never a
     * launch failure.
     */
    default boolean storedDark() {
        return load().map(DARK::equals).orElse(false);
    }

    /**
     * The session's effective appearance: the {@code --dark} override
     * wins for this launch without rewriting the stored choice.
     */
    static boolean sessionDark(boolean darkOverride, AppearanceStore store) {
        return darkOverride || store.storedDark();
    }

    /** The JDK-preferences implementation used by the application. */
    static AppearanceStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    /** An implementation over an explicit node; tests use a test node. */
    static AppearanceStore forNode(Preferences node) {
        return new AppearanceStore() {
            @Override
            public Optional<String> load() {
                return Optional.ofNullable(node.get("appearance", null));
            }

            @Override
            public void save(String appearance) {
                if (!LIGHT.equals(appearance) && !DARK.equals(appearance)) {
                    throw new IllegalArgumentException(
                            "unknown appearance: " + appearance);
                }
                node.put("appearance", appearance);
            }
        };
    }
}
