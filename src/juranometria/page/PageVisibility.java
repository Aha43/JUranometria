package juranometria.page;

/**
 * Why an object on this page can or cannot be seen on it.
 *
 * <p>Measured during the gate: on the released page 1,524 of 2,481
 * objects are present and undrawn, so "on this page" and "visible on
 * this page" are different questions and a reader is owed both
 * answers. Each state is a different silence, and saying which one
 * it is is the whole value of the panel.
 *
 * <p>Every answer comes from production - {@code symbolForType},
 * {@code permitted}, the detail policy and the scene's own limiting
 * magnitude - never from a second copy of those rules.
 */
public enum PageVisibility {

    /** The page draws it. */
    DRAWN("drawn"),

    /** Its family is switched off in Chart Options. */
    FAMILY_HIDDEN("hidden by a chart option"),

    /** Fainter than this page's limiting magnitude. */
    BELOW_LIMIT("fainter than the magnitude limit"),

    /** The atlas has no symbol for its catalogue type. */
    NO_SYMBOL("no chart symbol for its type"),

    /** Too small to draw honestly at this field. */
    TOO_SMALL("below the detail policy at this field");

    private final String prose;

    PageVisibility(String prose) {
        this.prose = prose;
    }

    /** The words a reader is shown. */
    public String prose() {
        return prose;
    }
}
