package juranometria.sheet;

/**
 * What a reader can be handed (Sprint 29, issue #286).
 *
 * <p>Three, and each is here because it answers a question the others
 * do not: something to edit, something to print, something to send.
 * The gate rejected a fourth and rejected offering the choice as a
 * list of file extensions with no explanation.
 *
 * <p><strong>It owns the formats, not the words for them</strong>
 * (#350). This enum carried a readable name and an English sentence
 * per constant, which made a domain type the home of reader prose -
 * the same defect found in {@code SymbolFamily} and
 * {@code PageVisibility}, repaired the same way. What a reader is
 * told lives in {@code SheetFormatText}; the identity, the extension
 * and whether a resolution means anything stay here.
 */
public enum SheetFormat {

    /** Vector, editable, the master. */
    SVG("SVG", "svg"),

    /** Vector, fixed, the printable. */
    PDF("PDF", "pdf"),

    /** Pixels, at a stated physical size, the shareable. */
    PNG("PNG", "png");

    private final String identity;
    private final String extension;

    SheetFormat(String identity, String extension) {
        this.identity = identity;
        this.extension = extension;
    }

    /**
     * What this format is CALLED, everywhere and in every language.
     *
     * <p>{@code SVG}, {@code PDF} and {@code PNG} are format
     * identities, not words: they are the same three letters to every
     * reader, and a translation that rendered them differently would
     * be naming a different thing. A reader's <em>explanation</em> of
     * what each is for is presentation and lives in
     * {@code SheetFormatText} (#350).
     */
    public String identity() {
        return identity;
    }

    /** The file extension, without the dot. */
    public String extension() {
        return extension;
    }

    /** Whether a resolution is a meaningful choice for this format. */
    public boolean hasResolution() {
        return this == PNG;
    }

    /** The format a filename names, or empty when it names none. */
    public static java.util.Optional<SheetFormat> ofFileName(String name) {
        if (name == null) {
            return java.util.Optional.empty();
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        for (SheetFormat format : values()) {
            if (lower.endsWith("." + format.extension)) {
                return java.util.Optional.of(format);
            }
        }
        return java.util.Optional.empty();
    }
}
