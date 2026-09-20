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
 *
 * <p><strong>It owns the answers, not the words for them</strong>
 * (#350). This enum used to carry an English label and an English
 * sentence for each constant, which made a domain type the home of
 * reader prose - the same defect found in {@code SymbolFamily}, and
 * repaired the same way. What a reader is told lives in
 * {@code PageVisibilityText}, keyed off these identities, so a
 * translation changes the words and nothing here moves.
 *
 * <p>The constants keep their order, and callers sort by it. A table
 * sorted by translated spelling would put its rows in a different
 * order in every language, and the order means something: it runs
 * from drawn to least drawn.
 */
public enum PageVisibility {

    /** The page draws it. */
    DRAWN,

    /** Its family is switched off in Chart Options. */
    FAMILY_HIDDEN,

    /** Fainter than this page's limiting magnitude. */
    BELOW_LIMIT,

    /** The atlas has no symbol for its catalogue type. */
    NO_SYMBOL,

    /** Too small to draw honestly at this field. */
    TOO_SMALL
}
