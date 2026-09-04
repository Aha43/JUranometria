package juranometria.app;

import java.util.prefs.Preferences;

/**
 * Answers, from a JVM of its own, whether a preference node exists
 * in the backing store (Sprint 26, issue #224).
 *
 * <p>Same-JVM checks cannot prove a deletion persisted: an
 * unflushed removal looks gone to the process that made it and is
 * still there for everyone else. This probe syncs a fresh view and
 * exits 0 when the node is absent, 1 when it survives.
 */
public final class PrefsExistsProbe {

    private PrefsExistsProbe() {
    }

    public static void main(String[] args) throws Exception {
        Preferences.userRoot().sync();
        System.exit(Preferences.userRoot().nodeExists(args[0]) ? 1 : 0);
    }
}
