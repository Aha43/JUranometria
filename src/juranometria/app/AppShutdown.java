package juranometria.app;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The application's one way out (Sprint 23, issue #198).
 *
 * <p>Until now leaving was <code>EXIT_ON_CLOSE</code> and nothing
 * else: the window vanished and the JVM stopped, which happens to be
 * adequate for an atlas with no unsaved document. Adding a second
 * exit surface makes that inadequate, because two surfaces that each
 * terminate in their own way are two behaviours a reader has to
 * learn. So there is one path, and the toolbar button, the window's
 * close box and the platform's Quit all take it.
 *
 * <p>The order matters and is fixed here:
 *
 * <ol>
 *   <li><strong>detach</strong> what was attached, newest first, so a
 *       listener cannot be handed an event about a window that is
 *       already going;</li>
 *   <li><strong>flush preferences</strong>, so what the reader chose
 *       is on disk before anything can stop the process. The platform
 *       flushes on a clean exit anyway; doing it deliberately means
 *       the promise does not depend on that;</li>
 *   <li><strong>dispose every window</strong>, dialogs included, so
 *       nothing is left holding native resources;</li>
 *   <li><strong>terminate</strong>.</li>
 * </ol>
 *
 * <p>Termination is injected rather than called, so the first three
 * steps can be proved without stopping the test suite that is proving
 * them. Production hands it {@code System::exit}; a test hands it a
 * recorder, and a subprocess proves the real thing.
 *
 * <p>Asking twice does nothing the second time. A reader who presses
 * the button and then reaches for the close box is not making two
 * requests, and half a shutdown run twice is worse than one run once.
 */
public final class AppShutdown {

    /** What each step is called, in the order they run; for tests. */
    public static final List<String> STEPS =
            List.of("detach", "flush", "dispose", "terminate");

    private final List<Runnable> detachments = new ArrayList<>();
    private final Runnable terminate;
    private final Preferences preferences;
    private boolean requested;

    /** The production path: the real preferences, and a real exit. */
    public static AppShutdown real() {
        return new AppShutdown(() -> System.exit(0),
                Preferences.userRoot().node("juranometria"));
    }

    public AppShutdown(Runnable terminate, Preferences preferences) {
        if (terminate == null) {
            throw new IllegalArgumentException(
                    "a shutdown must end somewhere");
        }
        this.terminate = terminate;
        this.preferences = preferences;
    }

    /**
     * Something to let go of on the way out - a listener, a panel's
     * subscription, a watcher. Run newest first.
     */
    public void onShutdown(Runnable detach) {
        if (detach != null) {
            detachments.add(detach);
        }
    }

    /** Whether the application is already on its way out. */
    public boolean isRequested() {
        return requested;
    }

    /**
     * Leaves, once. Every surface calls this and none of them calls
     * {@code System.exit} itself, so there is one behaviour to reason
     * about and one place to change it.
     */
    public void request() {
        if (requested) {
            return;
        }
        requested = true;
        for (int i = detachments.size() - 1; i >= 0; i--) {
            // One detachment that throws must not strand the rest,
            // nor keep the reader in an application they asked to
            // leave.
            try {
                detachments.get(i).run();
            } catch (RuntimeException ignored) {
                // Leaving is not the moment to argue about it.
            }
        }
        flushPreferences();
        for (Window window : Window.getWindows()) {
            try {
                window.dispose();
            } catch (RuntimeException ignored) {
                // As above: a window that will not close must not
                // keep the application open.
            }
        }
        terminate.run();
    }

    private void flushPreferences() {
        if (preferences == null) {
            return;
        }
        try {
            preferences.flush();
        } catch (BackingStoreException ignored) {
            // A preference store that cannot be written is a problem
            // the reader already has; refusing to quit does not
            // improve it, and nothing here was theirs to lose.
        }
    }
}
