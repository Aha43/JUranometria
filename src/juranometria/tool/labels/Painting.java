package juranometria.tool.labels;

import java.util.List;

import juranometria.tool.labels.Participant.Family;

/**
 * The order the atlas paints its layers in (Sprint 31, issue #310).
 *
 * <p>Read off {@code ChartRenderer.render}, which draws the grid
 * first "beneath geography, stars, and every label", then boundaries,
 * figures and constellation names, then the reference layer "above the
 * grid and the figures, below every mark", then deep-sky symbols and
 * star marks, then star labels, then deep-sky labels, and last the
 * furniture, "last and opaque, in the decided order".
 *
 * <p>This matters because ink drawn later hides ink drawn earlier, and
 * a collision has a victim. It is stated here rather than guessed at
 * per pair, and it is <em>checked</em> rather than trusted: for every
 * family pair a page collides in, the study asks the opposite question
 * too and reports whether the answer was bigger. Where antialiasing
 * blends two inks in one pixel both directions have something to say,
 * and the later one always has more of it.
 */
public final class Painting {

    private Painting() {
    }

    /** Earliest first. */
    private static final List<Family> ORDER = List.of(
            Family.GRID_INK,
            Family.BOUNDARY_LINE,
            Family.FIGURE_LINE,
            Family.CONSTELLATION_NAME,
            Family.REFERENCE_INK,
            Family.DEEP_SKY_SYMBOL,
            Family.STAR_DISC,
            Family.STAR_LABEL,
            Family.DEEP_SKY_LABEL,
            Family.SELECTION_RING,
            Family.TITLE_BLOCK,
            Family.MAGNITUDE_KEY);

    /** Where in the paint order this family draws. */
    public static int at(Family family) {
        int index = ORDER.indexOf(family);
        if (index < 0) {
            throw new IllegalArgumentException(
                    "no place in the paint order: " + family);
        }
        return index;
    }

    /** Whether the first family draws over the second. */
    public static boolean over(Family later, Family earlier) {
        return at(later) > at(earlier);
    }

    /** The families in the order the page paints them. */
    public static List<Family> order() {
        return ORDER;
    }
}
