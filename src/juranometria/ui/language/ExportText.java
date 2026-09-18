package juranometria.ui.language;

import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;

/**
 * What the atlas tells a reader while handing them a file
 * (Sprint 33, issue #350).
 *
 * <p>{@code SheetFormat} and {@code PaperSize} own the formats and the
 * papers; this owns the words for them. Both enums carried a readable
 * name and, in one case, an English sentence per constant - the third
 * and fourth instances of a defect already repaired in
 * {@code SymbolFamily} and {@code PageVisibility}.
 *
 * <p><strong>The identities are not words.</strong> {@code SVG},
 * {@code PDF}, {@code PNG}, {@code A4} and {@code US Letter} are the
 * same to every reader: a Norwegian print shop orders US Letter by
 * that name, and a translated form would name nothing anyone could
 * ask for. They are handed to sentences as arguments and never
 * written into a translated value, so no translator is invited to
 * invent another paper name.
 *
 * <p>The whole export journey is one surface. A reader who presses
 * Export meets the chooser, perhaps an overwrite question, and either
 * a confirmation or a refusal; localising only the first window would
 * leave the consequential half in English. The words for all of it
 * are here, whichever class happens to raise them.
 */
public final class ExportText {

    private final InterfaceText said;

    private ExportText(InterfaceText said) {
        this.said = said;
    }

    /** The export words for a language a caller states. */
    public static ExportText in(InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the words have to be in some language");
        }
        return new ExportText(said);
    }

    /** The language these words are in, for callers that pass it on. */
    public InterfaceText words() {
        return said;
    }

    /** What a format is called: its identity, unchanged. */
    public String name(SheetFormat format) {
        return format.identity();
    }

    /** What that format is for, in the reader's own language. */
    public String explanation(SheetFormat format) {
        return said.say(key(format));
    }

    /** What a paper is called: its identity, unchanged. */
    public String name(PaperSize paper) {
        return paper.identity();
    }

    /**
     * A resolution, as a reader reads it.
     *
     * <p>The number is notation and goes in as an argument; "dots per
     * inch" is a phrase, and was concatenated onto the number until
     * #350.
     */
    public String resolution(int dotsPerInch) {
        return said.say("export.resolution.item",
                String.valueOf(dotsPerInch));
    }

    /** Asking whether a file already there should be replaced. */
    public String replaceQuestion(String fileName, String folder) {
        return said.say("export.replace.question", fileName, folder);
    }

    /** The title over that question. */
    public String replaceTitle() {
        return said.say("export.replace.title");
    }

    private static String key(SheetFormat format) {
        return switch (format) {
            case SVG -> "export.format.svg.explain";
            case PDF -> "export.format.pdf.explain";
            case PNG -> "export.format.png.explain";
        };
    }
}
