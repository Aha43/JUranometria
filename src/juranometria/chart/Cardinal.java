package juranometria.chart;

/**
 * The observer's cardinal directions on the mathematical horizon
 * (issue #359).
 *
 * <p>An identity, deliberately without words. N on a Norwegian page
 * is the same north as N on an English one; the drawn letter and the
 * spoken name belong to the page's language seam
 * ({@code PageWords}), not to the direction itself. What this type
 * owns is which direction a mark <em>is</em> - stable across
 * languages, sessions and exports.
 *
 * <p>These are the observer's directions, not the page's: a chart
 * remains celestial north up, celestial east left whatever the
 * observer faces, and keeping the two vocabularies in separate types
 * is part of keeping them apart on the page.
 */
public enum Cardinal {

    NORTH("cardinal-north"),
    EAST("cardinal-east"),
    SOUTH("cardinal-south"),
    WEST("cardinal-west");

    private final String identity;

    Cardinal(String identity) {
        this.identity = identity;
    }

    /** The stable identity a contribution carries. */
    public String identity() {
        return identity;
    }
}
