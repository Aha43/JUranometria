package juranometria.ui.language;

import juranometria.page.PageVisibility;

/**
 * What the atlas tells a reader about why an object is or is not
 * drawn (Sprint 33, issue #350).
 *
 * <p>{@link PageVisibility} owns the answers; this owns the words for
 * them. The enum carried both until #350, which made a domain type
 * the home of English reader prose - the same defect {@code
 * SymbolFamily} had, found the same way and repaired the same way.
 *
 * <p><strong>Two registers, two keys.</strong> The Chart column shows
 * a compact word a reader scans down a column; the hover and the
 * spoken description give the whole answer. They are not the same
 * sentence at two lengths, and neither is derived from the other: a
 * language that can shorten "fainter than the magnitude limit" to one
 * word may not be able to shorten the next one, and a derivation
 * would decide that for it. Owner ruling, 2026-09-18.
 *
 * <p>The keys come from the enum identity through a switch, never
 * from a label. Renaming what a state is CALLED must not silently
 * change which resource answers for it.
 */
public final class PageVisibilityText {

    private final InterfaceText said;

    private PageVisibilityText(InterfaceText said) {
        this.said = said;
    }

    /** The visibility words for a language a caller states. */
    public static PageVisibilityText in(InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the words have to be in some language");
        }
        return new PageVisibilityText(said);
    }

    /**
     * The compact word the Chart column shows.
     *
     * <p>Short so a reader can scan a column of them, and never a
     * private code: the whole answer is always one hover away, and
     * rides the spoken channel unconditionally.
     */
    public String label(PageVisibility state) {
        return said.say(key(state) + ".label");
    }

    /** The whole answer, for the hover and the spoken description. */
    public String explanation(PageVisibility state) {
        return said.say(key(state) + ".explain");
    }

    private static String key(PageVisibility state) {
        return switch (state) {
            case DRAWN -> "onthispage.state.drawn";
            case FAMILY_HIDDEN -> "onthispage.state.familyHidden";
            case BELOW_LIMIT -> "onthispage.state.belowLimit";
            case NO_SYMBOL -> "onthispage.state.noSymbol";
            case TOO_SMALL -> "onthispage.state.tooSmall";
        };
    }
}
