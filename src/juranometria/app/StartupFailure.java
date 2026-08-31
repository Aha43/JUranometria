package juranometria.app;

import java.awt.GraphicsEnvironment;
import java.util.prefs.BackingStoreException;

import javax.swing.JOptionPane;

/**
 * The last line of defence at launch (issue #145): a failure while
 * building the atlas must say what happened and what to do about it.
 *
 * The packaged application has no console. Before this existed, a
 * damaged or incomplete download - a truncated catalogue tile, a
 * missing notice, a half-unpacked archive - threw on the event
 * dispatch thread, printed a stack trace nowhere the reader could
 * see it, and left the process alive with no window at all: on
 * macOS, an icon in the dock that never opens anything. That is the
 * one failure mode a reader cannot diagnose, so it is the one the
 * application must explain.
 *
 * **A remedy is only useful if it is the right one** (audit review,
 * P1). Re-downloading repairs a damaged archive and nothing else; it
 * cannot fix an unreadable preferences store, and offering it for an
 * unrecognised failure is a confident lie. So the failure is
 * classified by where it actually came from, and an unrecognised
 * failure says exactly that rather than guessing.
 */
final class StartupFailure {

    private StartupFailure() {
    }

    /** Where a launch failure came from, and therefore its remedy. */
    enum Kind {
        /** The verifying loaders: bundled data that is not what was published. */
        BUNDLED_DATA,
        /** The JDK preferences store: the reader's saved settings. */
        SETTINGS,
        /** Anything else - honestly unrecognised. */
        UNRECOGNISED,
    }

    /**
     * Classified by the frames the failure actually came through,
     * not by matching words in its message: the loaders that verify
     * bundled packs live in {@code juranometria.catalog} and
     * {@code juranometria.geo}, and the preferences store is the
     * JDK's own {@code java.util.prefs}. Both can throw plain
     * {@link IllegalStateException}, which is exactly why the type
     * alone cannot decide this.
     */
    static Kind classify(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof BackingStoreException) {
                return Kind.SETTINGS;
            }
            for (StackTraceElement frame : t.getStackTrace()) {
                String className = frame.getClassName();
                if (className.startsWith("java.util.prefs.")) {
                    return Kind.SETTINGS;
                }
                if (className.startsWith("juranometria.catalog.")
                        || className.startsWith("juranometria.geo.")) {
                    return Kind.BUNDLED_DATA;
                }
            }
        }
        return Kind.UNRECOGNISED;
    }

    /**
     * The message shown to the reader: what failed, in the loader's
     * own words, and the remedy that fits that failure. Kept
     * separate from the display so it can be asserted without a
     * screen.
     */
    static String message(Throwable failure) {
        return "JUranometria could not start.\n\n"
                + describe(failure) + "\n\n"
                + remedy(classify(failure));
    }

    private static String remedy(Kind kind) {
        return switch (kind) {
            case BUNDLED_DATA -> """
                    The application verifies its bundled star catalogue, \
                    constellation geography, and star identities against \
                    their own checksummed manifests as it loads, so this \
                    means the files on disk are not the files that were \
                    published.

                    Download the release again, check it against the \
                    published SHA-256 checksum, and unpack it fresh into \
                    an empty folder.""";
            case SETTINGS -> """
                    This is the store of your own saved settings - \
                    appearance and chart options - not the atlas itself. \
                    Removing it makes JUranometria start again with its \
                    defaults; nothing else is lost.

                    macOS:   ~/Library/Preferences/com.apple.java.util.prefs.plist
                    Linux:   ~/.java/.userPrefs/juranometria
                    Windows: HKEY_CURRENT_USER\\Software\\JavaSoft\\Prefs\\juranometria

                    If the store cannot be written at all, check that \
                    your user account's home directory is present and \
                    writable.""";
            case UNRECOGNISED -> """
                    This is not a failure JUranometria recognises, so it \
                    has no remedy to offer: it is more likely a defect in \
                    the application than a problem with your download or \
                    your settings.

                    Please report it with the message above, your \
                    platform, and this version at
                    """ + AppInfo.REPO_URL + "/issues";
        };
    }

    /** The most specific cause available, never an empty line. */
    private static String describe(Throwable failure) {
        Throwable cause = failure;
        while (cause.getMessage() == null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String detail = cause.getMessage();
        return detail == null || detail.isBlank()
                ? cause.getClass().getName() : detail;
    }

    /**
     * Reports a launch failure and ends the process: the stack trace
     * to the console for whoever has one, the message to the screen
     * for whoever does not, and a non-zero exit so no invisible
     * window-less process is left behind.
     */
    static void reportAndExit(Throwable failure) {
        // Both readers are served: the message for whoever is looking
        // at a terminal, the trace beneath it for whoever is looking
        // into a bug report.
        System.err.println(message(failure));
        failure.printStackTrace();
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                JOptionPane.showMessageDialog(null, message(failure),
                        AppInfo.NAME + " could not start",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Throwable ignored) {
                // A screen that cannot even show a dialog must not
                // swallow the exit below; the console line stands.
            }
        }
        Runtime.getRuntime().halt(1);
    }
}
