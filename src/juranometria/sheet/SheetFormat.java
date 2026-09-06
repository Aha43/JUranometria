package juranometria.sheet;

/**
 * What a reader can be handed (Sprint 29, issue #286).
 *
 * <p>Three, and each is here because it answers a question the others
 * do not: something to edit, something to print, something to send.
 * The gate rejected a fourth and rejected offering the choice as a
 * list of file extensions with no explanation.
 */
public enum SheetFormat {

    /** Vector, editable, the master. */
    SVG("SVG", "svg", "Vector, with the labels as text you can edit"),

    /** Vector, fixed, the printable. */
    PDF("PDF", "pdf", "Vector, sized for the page, ready to print"),

    /** Pixels, at a stated physical size, the shareable. */
    PNG("PNG", "png", "A picture of the sheet, for sharing");

    private final String readableName;
    private final String extension;
    private final String explanation;

    SheetFormat(String readableName, String extension, String explanation) {
        this.readableName = readableName;
        this.extension = extension;
        this.explanation = explanation;
    }

    public String readableName() {
        return readableName;
    }

    /** The file extension, without the dot. */
    public String extension() {
        return extension;
    }

    /** What this format is for, in the reader's terms. */
    public String explanation() {
        return explanation;
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
