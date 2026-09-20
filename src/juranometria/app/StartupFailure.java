package juranometria.app;

import java.awt.GraphicsEnvironment;
import java.util.prefs.BackingStoreException;

import juranometria.catalog.PackIntegrityException;

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
     * Classified by closed signals only (audit review, P1).
     *
     * <p>Damaged data is recognised by {@link PackIntegrityException},
     * which only the verification paths throw. An earlier version
     * asked which package the stack frames came from, which was not
     * closed at all: a programming defect anywhere in
     * {@code juranometria.catalog} or {@code juranometria.geo} would
     * have told the reader their download was damaged and sent them
     * to fetch a perfectly good file again.
     *
     * <p>A settings failure is recognised by the JDK's own
     * {@link BackingStoreException}, or by frames inside
     * {@code java.util.prefs} - a package that contains no
     * JUranometria code, so a defect of ours cannot be mistaken for
     * one of its failures.
     */
    static Kind classify(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof PackIntegrityException) {
                return Kind.BUNDLED_DATA;
            }
            if (t instanceof BackingStoreException) {
                return Kind.SETTINGS;
            }
            for (StackTraceElement frame : t.getStackTrace()) {
                if (frame.getClassName().startsWith("java.util.prefs.")) {
                    return Kind.SETTINGS;
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
        return message(failure, readersLanguage(),
                juranometria.ui.language.StartupText.PACKAGED);
    }

    /**
     * What the reader is told, in their language when all of it can
     * be read and in English otherwise.
     *
     * <p><strong>The framing is atomic.</strong> A headline and its
     * remedy are one piece of writing: a Norwegian headline over an
     * English remedy would look like a defect inside the report of a
     * defect, and would leave a reader unsure which half to believe.
     * So both are asked for together and either both are used or
     * neither is.
     *
     * <p>The technical cause between them is the loader's own words
     * and is never translated - it is what a reader quotes in an
     * issue - and it appears exactly once whichever framing is used.
     */
    static String message(Throwable failure, String language,
                          juranometria.ui.language.StartupText.Documents
                                  documents) {
        Kind kind = classify(failure);
        String cause = describe(failure);
        juranometria.ui.language.StartupText.Said said = null;
        try {
            said = juranometria.ui.language.StartupText.forFailure(
                    language, remedyOf(kind), documents);
        } catch (Throwable reportingFailed) {
            // A failure while reporting a failure must not replace
            // it, and must not leave the process silent. English is
            // embedded below and cannot fail to be reached.
            said = null;
        }
        return said == null
                ? ENGLISH_FAILED + "\n\n" + cause + "\n\n"
                        + englishRemedy(kind)
                : said.headline() + "\n\n" + cause + "\n\n"
                        + said.remedy();
    }

    /**
     * The headline in English: embedded, and the last line of
     * defence.
     *
     * <p>This and {@link #englishRemedy} are the one place in the
     * atlas where a hard-coded reader sentence is right. They are
     * reached without opening a file, a preference node or a language
     * class, because each of those is something this surface may be
     * reporting the failure of.
     */
    static final String ENGLISH_FAILED = "JUranometria could not start.";

    /** The reader's stored language, or null if it cannot be read. */
    private static String readersLanguage() {
        try {
            return juranometria.ui.language.SkyLanguageStore.user()
                    .choice(Atlas.languages()).interfaceLanguage();
        } catch (Throwable unavailable) {
            // The settings store is one of the three things that may
            // have failed; asking it here must not fail again.
            return null;
        }
    }

    private static juranometria.ui.language.StartupText.Remedy remedyOf(
            Kind kind) {
        return switch (kind) {
            case BUNDLED_DATA -> juranometria.ui.language.StartupText
                    .Remedy.BUNDLED_DATA;
            case SETTINGS -> juranometria.ui.language.StartupText
                    .Remedy.SETTINGS;
            case UNRECOGNISED -> juranometria.ui.language.StartupText
                    .Remedy.UNRECOGNISED;
        };
    }

    /** The remedy in English: embedded, and the last line of defence. */
    static String englishRemedy(Kind kind) {
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
                    Removing JUranometria's own settings makes it start \
                    again with its defaults; nothing else is lost.

                    Linux:   delete the directory
                             ~/.java/.userPrefs/juranometria
                    Windows: delete the registry key
                             HKEY_CURRENT_USER\\Software\\JavaSoft\\Prefs\\juranometria
                    macOS:   Java preferences share ONE file with every \
                    other Java application, so do not delete it. Remove \
                    only this application's entry, with the atlas closed:

                      /usr/libexec/PlistBuddy -c 'Delete ":/:juranometria/"' \\
                        ~/Library/Preferences/com.apple.java.util.prefs.plist

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

    /**
     * Everything the chain has to say, never an empty line.
     *
     * <p>Stopping at the first wrapper carrying a message hid the
     * useful one beneath it (audit review, P1): "failed to read
     * catalogue tile X" is a worse sentence than the checksum
     * mismatch it wraps, and the reader needs the second. So every
     * distinct message in the chain is kept, outermost first, and
     * only a chain with nothing to say at all falls back to naming
     * its type.
     */
    private static String describe(Throwable failure) {
        java.util.List<String> said = new java.util.ArrayList<>();
        for (Throwable t = failure; t != null && said.size() < 4;
                t = t.getCause()) {
            String detail = t.getMessage();
            if (detail != null && !detail.isBlank()
                    && !said.contains(detail)) {
                said.add(detail);
            }
        }
        if (said.isEmpty()) {
            Throwable deepest = failure;
            while (deepest.getCause() != null) {
                deepest = deepest.getCause();
            }
            return deepest.getClass().getName();
        }
        return String.join("\n\n", said);
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
