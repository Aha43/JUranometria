package juranometria.render;

import juranometria.chart.Star;
import juranometria.chart.StarIdentity;

/**
 * The reviewed star-label scale policy (docs/decisions/
 * star-identity.md and docs/decisions/bayer-notation.md),
 * option-free like every policy class: which identity form labels a
 * star at the page's field width, and in what notation.
 *
 * Notation: the proper name as recorded; a Bayer designation as its
 * letter with component digits raised (pi-3 as a superscript,
 * alpha-1 likewise); a Flamsteed number bare. Where a star has both
 * a name and a letter and both qualify, they travel together
 * ("Betelgeuse a") - one glyph teaching the constellation's Bayer
 * sequence where the reader is already looking. Priority when only
 * one qualifies is name, then letter, then number.
 *
 * Magnitude limits per band, and the post-omega Latin hold-back
 * (Latin Bayer letters read as stray capitals among Greek on a
 * constellation page, so they wait for the regional fields):
 *
 *   24-36 degrees: names V &lt;= 2.5, Greek letters V &lt;= 3.5;
 *   12-18 degrees: names V &lt;= 3.0, Greek letters V &lt;= 4.5;
 *   &lt;= 8 degrees:  names V &lt;= 4.5, all letters V &lt;= 5.0,
 *                  Flamsteed numbers V &lt;= 5.0.
 */
public final class StarLabelPolicy {

    /** Raised forms of the component digits the pack records. */
    private static final char[] SUPERSCRIPTS = {
            '⁰', '¹', '²', '³', '⁴',
            '⁵', '⁶', '⁷', '⁸', '⁹'};

    private final double fieldWidthDegrees;

    public StarLabelPolicy(double fieldWidthDegrees) {
        this.fieldWidthDegrees = fieldWidthDegrees;
    }

    /**
     * Conventional Bayer notation for an identity, or null when it
     * carries no letter. The component digits are part of the
     * catalogue's own {@code bayer} value; this lifts that value's
     * trailing digit run and never re-derives a designation from
     * rendered text. A value that is only digits carries no letter
     * to draw and is omitted rather than rendered as broken
     * notation - the data boundary's rule, applied here too.
     */
    public static String bayerNotation(StarIdentity identity) {
        String bayer = identity == null ? null : identity.bayer();
        if (bayer == null) {
            return null;
        }
        int split = bayer.length();
        while (split > 0 && isComponentDigit(bayer.charAt(split - 1))) {
            split--;
        }
        if (split == 0) {
            return null;
        }
        StringBuilder out = new StringBuilder(bayer.substring(0, split));
        for (int i = split; i < bayer.length(); i++) {
            out.append(SUPERSCRIPTS[bayer.charAt(i) - '0']);
        }
        return out.toString();
    }

    private static boolean isComponentDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /** Whether a Bayer designation begins with a Greek letter. */
    public static boolean isGreek(String bayer) {
        char first = bayer.charAt(0);
        return first >= 'α' && first <= 'ω';
    }

    /** The text the policy labels this star with, or null: none. */
    public String labelFor(Star star) {
        StarIdentity identity = star.identity();
        if (identity == null) {
            return null;
        }
        double magnitude = star.magnitude();
        String name = identity.name() != null && magnitude <= nameLimit()
                ? identity.name() : null;
        String letter = lettersDrawn(identity) && magnitude <= bayerLimit()
                ? bayerNotation(identity) : null;
        if (name != null && letter != null) {
            return name + " " + letter;
        }
        if (name != null) {
            return name;
        }
        if (letter != null) {
            return letter;
        }
        if (identity.flamsteed() != null && magnitude <= flamsteedLimit()) {
            return identity.flamsteed();
        }
        return null;
    }

    /**
     * Whether this identity's letter may draw at this field at all:
     * Greek letters everywhere, post-omega Latin letters only on the
     * regional pages.
     */
    private boolean lettersDrawn(StarIdentity identity) {
        return identity.bayer() != null
                && (isGreek(identity.bayer()) || fieldWidthDegrees < 12.0);
    }

    /**
     * The searched star's guaranteed label: its best identity,
     * exempt from every magnitude threshold (the same shape as the
     * deep-sky target exemption), in the same notation the ordinary
     * pass would use - name and letter together when it has both.
     * Null when the star has no designation to show.
     */
    public static String guaranteedLabelFor(Star star) {
        StarIdentity identity = star.identity();
        if (identity == null) {
            return null;
        }
        String letter = bayerNotation(identity);
        if (identity.name() != null) {
            return letter == null ? identity.name()
                    : identity.name() + " " + letter;
        }
        return letter != null ? letter : identity.flamsteed();
    }

    double nameLimit() {
        // 2.5, not 2.0: the reviewed adjustment (PR #120) - the
        // 2.0-2.5 magnitude band holds the sky's asterism anchors
        // (Alpheratz, Mirach, Dubhe, Mizar...), and a figure whose
        // principal stars stay anonymous is less useful cartography.
        return fieldWidthDegrees >= 24.0 ? 2.5
                : fieldWidthDegrees >= 12.0 ? 3.0 : 4.5;
    }

    double bayerLimit() {
        // Letters reach the constellation pages (Sprint 17): the
        // whole point of a constellation map is naming the pattern
        // it draws.
        return fieldWidthDegrees >= 24.0 ? 3.5
                : fieldWidthDegrees >= 12.0 ? 4.5 : 5.0;
    }

    double flamsteedLimit() {
        // Unchanged from the release: a bare number near a deep-sky
        // label reads as a Messier number, so numbers stay the
        // regional last resort.
        return fieldWidthDegrees >= 12.0 ? Double.NEGATIVE_INFINITY : 5.0;
    }
}
