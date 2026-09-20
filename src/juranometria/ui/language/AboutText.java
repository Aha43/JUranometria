package juranometria.ui.language;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import juranometria.app.AboutDialog;

/**
 * What the About surface says, and what it must not touch
 * (Sprint 33, issue #350).
 *
 * <p>Three kinds of words meet on this one surface, and the whole
 * design is keeping them apart:
 *
 * <ul>
 *   <li><strong>Application prose</strong> - the title, the
 *       description, the buttons, the explanations, the seven section
 *       headings. Translatable, and owned here.</li>
 *   <li><strong>Identity and fact</strong> - {@code JUranometria},
 *       the version, {@code Tycho-2}, {@code OpenNGC},
 *       {@code CC BY-NC 3.0 IGO}. Data, handed to sentences as
 *       arguments, exact in every language.</li>
 *   <li><strong>Bundled legal documents</strong> - 25 235 bytes of
 *       upstream notice and licence text. <strong>Never
 *       translated</strong>, never edited, never reflowed. A language
 *       pack cannot reach them, and {@link #noticesBody} exists to
 *       say so in code rather than in a comment.</li>
 * </ul>
 *
 * <p>The window title was {@code "About " + AppInfo.NAME}: an English
 * word concatenated to an identity. The menu had already solved this
 * - {@code menu.about.label = About {0}} - and the dialog kept its own
 * copy. It is a pattern here too.
 *
 * <p><strong>The compact summary is a document, not a string.</strong>
 * It states JUranometria's own account of its legal position,
 * including the non-commercial consequence of the Tycho-2 data, and a
 * contract holds each licence in the same paragraph as the source it
 * belongs to. So a translation of it is a whole document that must
 * pass that same contract - not a set of keys. English is the
 * canonical one; a language that has not written its own gets the
 * English document <strong>entire</strong>, because a summary
 * assembled from two languages is a legal statement nobody wrote.
 */
public final class AboutText {

    private static final String STEM = "about.";

    /**
     * Where a language keeps its own summary, if it has one.
     *
     * <p>Beside the language packs rather than beside the canonical
     * English document, because it belongs to a language: the
     * canonical summary is the atlas's, and a translation of it is
     * that language's rendering of the same map.
     */
    public static final String SUMMARY_DIRECTORY =
            "/resources/interface-language/";

    /** The suffix that names a language's own summary document. */
    public static final String SUMMARY_SUFFIX = ".about-summary.txt";

    /**
     * Where the packaged documents are read from.
     *
     * <p>A seam, not a convenience. The fallback rule - a language
     * with no summary of its own reads the canonical English document
     * <em>entire</em> - can only be tested against a language that
     * definitely has no document, and "definitely" cannot mean "no
     * such file exists today": the moment Norwegian writes one, a
     * test relying on its absence stops testing anything and says so
     * to nobody (review).
     */
    @FunctionalInterface
    public interface Documents {

        /** The document at this path, or null when there is none. */
        InputStream open(String path);
    }

    /** The packaged documents, which is what the application reads. */
    public static final Documents PACKAGED =
            AboutText.class::getResourceAsStream;

    private final InterfaceText said;
    private final Documents documents;

    private AboutText(InterfaceText said, Documents documents) {
        this.said = said;
        this.documents = documents;
    }

    /** The About words for a language a caller states. */
    public static AboutText in(InterfaceText said) {
        return in(said, PACKAGED);
    }

    /** The same, reading its documents from a stated place. */
    public static AboutText in(InterfaceText said, Documents documents) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the words have to be in some language");
        }
        if (documents == null) {
            throw new IllegalArgumentException(
                    "and the documents have to come from somewhere");
        }
        return new AboutText(said, documents);
    }

    /** The language these words are in, for callers that pass it on. */
    public InterfaceText words() {
        return said;
    }

    // ---- the window ------------------------------------------------

    /** The window's title, with the application's name in it. */
    public String title(String applicationName) {
        return said.say(STEM + "title", applicationName);
    }

    /** What a screen reader is told the window is. */
    public String windowExplain() {
        return said.say(STEM + "window.explain");
    }

    /**
     * The name and version together, as the heading shows them.
     *
     * <p>Both are identity: {@code AppInfo} owns them and no language
     * touches either. They are arguments, so a language decides only
     * whether anything sits between them.
     */
    public String heading(String applicationName, String version) {
        return said.say(STEM + "heading", applicationName, version);
    }

    /** What the atlas is, in one sentence. */
    public String description() {
        return said.say(STEM + "description");
    }

    // ---- the compact view ------------------------------------------

    /** What a screen reader calls the summary. */
    public String summaryName() {
        return said.say(STEM + "summary.a11y");
    }

    /** What the summary is, and where the rest of it lives. */
    public String summaryExplain() {
        return said.say(STEM + "summary.explain");
    }

    /** The button that opens the bundled documents. */
    public String noticesButton() {
        return said.say(STEM + "notices.label");
    }

    /** Its spoken name, without the ellipsis a pointer reader sees. */
    public String noticesButtonName() {
        return said.say(STEM + "notices.a11y");
    }

    /** What pressing it does. */
    public String noticesButtonExplain() {
        return said.say(STEM + "notices.explain");
    }

    // ---- the notices view ------------------------------------------

    /** What a screen reader calls the bundled documents. */
    public String noticesName() {
        return said.say(STEM + "noticesview.a11y");
    }

    /** What they are. */
    public String noticesExplain() {
        return said.say(STEM + "noticesview.explain");
    }

    /** The way out, on both views. */
    public String closeButton() {
        return said.say(STEM + "close.label");
    }

    /** What it does. */
    public String closeExplain() {
        return said.say(STEM + "close.explain");
    }

    /**
     * What a bundled document is called, above its own text.
     *
     * <p>Keyed by the notice's identity. The heading used to sit in
     * the registry beside the resource path - half identity, half
     * English, in one array - which is the defect repaired in
     * {@code ChartKeys} a checkpoint earlier.
     */
    public String heading(AboutDialog.Notice notice) {
        String key = STEM + "notice." + notice.id() + ".label";
        String value = said.say(key);
        if (value == null || value.isBlank() || value.equals(key)) {
            throw new IllegalStateException("the About surface has no"
                    + " heading for \"" + notice.id() + "\". A bundled"
                    + " document shown under its own identity would be"
                    + " a defect wearing the shape of a title, and"
                    + " English is what every other language falls"
                    + " back to - a gap here is a gap everywhere.");
        }
        return value;
    }

    // ---- what no language may touch --------------------------------

    /**
     * A bundled document, character for character.
     *
     * <p>Upstream notice and licence text. The packaged bytes are
     * decoded as UTF-8 and returned unchanged - nothing trims,
     * reflows or re-encodes them - and the file itself is never
     * written. <strong>Character</strong>, not byte, is the accurate
     * word once the bytes have become a {@code String}: what a test
     * downstream of this can hold is that every character of the
     * document appears in what is shown, in order, which is the claim
     * that matters to a licence text.
     *
     * <p>It takes no language, deliberately, so that nothing can pass
     * one to it.
     */
    public static String noticesBody(AboutDialog.Notice notice) {
        return read(PACKAGED, notice.resourcePath());
    }

    /**
     * The compact summary in this language, whole, or the canonical
     * English document whole.
     *
     * <p><strong>Never a mixture.</strong> A summary half-translated
     * is a legal statement nobody wrote and nobody reviewed, so the
     * choice is per document and not per paragraph. A language that
     * ships its own must carry every source family with its own
     * licence and the Tycho-2 non-commercial consequence - the same
     * contract the English one is held to, expressed in another
     * language rather than replaced by a weaker one.
     */
    public String summary() {
        String own = readOrNull(documents, summaryPath());
        return own != null ? own : canonicalSummary(documents);
    }

    /** The English summary, which is the one the contract is written on. */
    public static String canonicalSummary() {
        return canonicalSummary(PACKAGED);
    }

    /** The same, from a stated place. */
    public static String canonicalSummary(Documents documents) {
        return read(documents, AboutDialog.SUMMARY_RESOURCE);
    }

    /** Whether this language ships a summary of its own. */
    public boolean hasOwnSummary() {
        return readOrNull(documents, summaryPath()) != null;
    }

    /** Where this language's own summary would live. */
    public String summaryPath() {
        return SUMMARY_DIRECTORY + said.language() + SUMMARY_SUFFIX;
    }

    private static String read(Documents documents, String resource) {
        String text = readOrNull(documents, resource);
        if (text == null) {
            throw new IllegalStateException(
                    "missing packaged notice resource: " + resource);
        }
        return text;
    }

    private static String readOrNull(Documents documents,
                                     String resource) {
        try (InputStream stream = documents.open(resource)) {
            return stream == null ? null
                    : new String(stream.readAllBytes(),
                            StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
