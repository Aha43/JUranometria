package juranometria.ui.language;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * What a reader is told when the atlas cannot start
 * (Sprint 33, issue #350).
 *
 * <p>The hardest surface in the issue, because it is the one that
 * runs when everything else has failed. Two of the three failures it
 * reports are reasons it might fail itself: a damaged download is a
 * damaged language pack, and an unreadable preference store is where
 * the chosen language is kept. A reporter that threw while reporting
 * would leave a reader with a dead process and no sentence at all.
 *
 * <p><strong>Documents, not keys.</strong> Each remedy is a
 * paragraph or several, and two of them carry things a one-line
 * resource grammar cannot hold: a Windows registry path with real
 * backslashes, and a shell command spanning lines. The
 * interface-language pack reads one line per key with no escape
 * handling, and teaching it escapes would make every existing value
 * in every language pay for this one surface's shape. So a remedy is
 * a whole UTF-8 file, read and used entire, and the English stays
 * embedded in {@code StartupFailure} where nothing can fail to reach
 * it.
 *
 * <p><strong>Nothing here throws.</strong> Every method catches
 * {@code Throwable} - not {@code Exception}: a broken classpath
 * raises errors, and this is exactly the situation where one might.
 * An absent, blank or unreadable document is answered with
 * {@code null}, which the caller reads as "use English".
 */
public final class StartupText {

    /** Where a language keeps its startup documents. */
    public static final String DIRECTORY =
            "/resources/interface-language/";

    /** What each classified failure's document is called. */
    public enum Remedy {

        /** Bundled data that is not what was published. */
        BUNDLED_DATA("startup-bundled-data"),
        /** The reader's own settings store. */
        SETTINGS("startup-settings"),
        /** Something the atlas does not recognise. */
        UNRECOGNISED("startup-unrecognised");

        private final String slug;

        Remedy(String slug) {
            this.slug = slug;
        }

        /** The file this remedy lives in, for a language. */
        public String documentFor(String language) {
            return DIRECTORY + language + "." + slug + ".txt";
        }
    }

    /** Where the documents are read from; the classpath, in production. */
    @FunctionalInterface
    public interface Documents {

        /** The document at this path, or null when there is none. */
        InputStream open(String path);
    }

    /** The packaged documents, which is what the application reads. */
    public static final Documents PACKAGED =
            StartupText.class::getResourceAsStream;

    private StartupText() {
    }

    /**
     * The headline and the remedy together, or nothing at all.
     *
     * <p><strong>Atomic.</strong> A displayed failure is one piece of
     * writing: a Norwegian headline over an English remedy would look
     * like a bug inside the report of a bug, and would leave a reader
     * unsure which half to trust. So both halves are read first, and
     * either both are used or neither is. The caller has complete
     * English for both and uses it whenever this answers null.
     *
     * @return the two halves, or null if this language cannot supply
     *     both of them completely
     */
    public static Said forFailure(String language, Remedy remedy,
                                  Documents documents) {
        try {
            if (language == null || remedy == null
                    || documents == null) {
                return null;
            }
            String headline = headline(language);
            if (headline == null) {
                return null;
            }
            String text = document(remedy.documentFor(language),
                    documents);
            if (text == null) {
                return null;
            }
            return new Said(headline, text);
        } catch (Throwable nothingDoing) {
            return null;
        }
    }

    /** The same, from the packaged documents. */
    public static Said forFailure(String language, Remedy remedy) {
        return forFailure(language, remedy, PACKAGED);
    }

    /** Both halves of a failure message, in one language. */
    public record Said(String headline, String remedy) {
    }

    /**
     * This language's own headline, or null.
     *
     * <p>Its <em>own</em>: {@code InterfaceText} falls back to
     * English for a key a language has not translated, and an English
     * headline returned here would be indistinguishable from a
     * Norwegian one, defeating the atomic rule. So the language's
     * pack is read directly and English is not consulted.
     */
    private static String headline(String language) {
        try {
            String value = InterfaceText.forLanguage(language)
                    .sayIfDefined("startup.failed");
            return value == null || value.isBlank() ? null : value;
        } catch (Throwable unreadable) {
            return null;
        }
    }

    /** A whole document, or null if it is missing, blank or unreadable. */
    private static String document(String path, Documents documents) {
        try (InputStream stream = documents.open(path)) {
            if (stream == null) {
                return null;
            }
            String text = new String(stream.readAllBytes(),
                    StandardCharsets.UTF_8);
            return text.isBlank() ? null : text;
        } catch (IOException | RuntimeException | Error unreadable) {
            return null;
        }
    }
}
