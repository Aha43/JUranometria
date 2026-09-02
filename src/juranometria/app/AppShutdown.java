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
    private final Runnable flush;
    private final Runnable dispose;
    private final Runnable terminate;
    private boolean requested;

    /** The production path: real preferences, real windows, real exit. */
    public static AppShutdown real() {
        Preferences node = Preferences.userRoot().node("juranometria");
        return new AppShutdown(() -> flushPreferences(node),
                AppShutdown::disposeEveryWindow, () -> System.exit(0));
    }

    /**
     * Each step as its own action, so the order this class promises
     * can be <em>observed</em> rather than described (review). A test
     * passes a recorder for every one and reads back what happened;
     * production passes the real three.
     */
    public AppShutdown(Runnable flush, Runnable dispose,
                       Runnable terminate) {
        if (flush == null || dispose == null || terminate == null) {
            throw new IllegalArgumentException(
                    "a shutdown flushes, disposes and ends");
        }
        this.flush = flush;
        this.dispose = dispose;
        this.terminate = terminate;
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
     *
     * <p><strong>Nothing may keep a reader in an application they
     * asked to leave</strong> (review). Each step is guarded on its
     * own, so a failure in one does not skip the ones after it - a
     * preference backend that throws must still let the windows close
     * - and termination is in a {@code finally}, so it happens
     * whatever any of them did. The earlier version guarded only the
     * checked exception the flush declares, which left every runtime
     * failure from the backing store able to strand the application
     * half-closed.
     */
    public void request() {
        if (requested) {
            return;
        }
        requested = true;
        try {
            for (int i = detachments.size() - 1; i >= 0; i--) {
                quietly(detachments.get(i));
            }
            quietly(flush);
            quietly(dispose);
        } finally {
            terminate.run();
        }
    }

    /**
     * Runs a step and swallows whatever it throws. Leaving is not the
     * moment to argue, and there is nowhere left to report it to.
     */
    private static void quietly(Runnable step) {
        try {
            step.run();
        } catch (RuntimeException | LinkageError ignored) {
            // Deliberate. A step that fails on the way out has
            // nothing the reader can do about it, and refusing to
            // close is not an improvement.
        }
    }

    /**
     * What the reader chose, on disk before anything can stop the
     * process. The platform flushes on a clean exit anyway; doing it
     * deliberately means the promise does not depend on that.
     */
    public static void flushPreferences(Preferences node) {
        if (node == null) {
            return;
        }
        try {
            node.flush();
        } catch (BackingStoreException | IllegalStateException ignored) {
            // A preference store that cannot be written is a problem
            // the reader already has; refusing to quit does not
            // improve it.
        }
    }

    /** Every window, dialogs included, so none holds native resources. */
    public static void disposeEveryWindow() {
        for (Window window : Window.getWindows()) {
            try {
                window.dispose();
            } catch (RuntimeException ignored) {
                // A window that will not close must not keep the
                // application open.
            }
        }
    }
}
