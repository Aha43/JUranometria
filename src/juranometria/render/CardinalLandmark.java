package juranometria.render;

import java.awt.FontMetrics;

import juranometria.chart.Cardinal;
import juranometria.project.PageWords;

/**
 * The size of a cardinal landmark on the page: the diamond at its
 * exact horizon point, the gap to its letter, and the room the whole
 * landmark needs beside it (#359 and its completion).
 *
 * <p>One definition, read by the ink that draws the landmark and by
 * the furniture that must leave it room, so the two cannot drift
 * apart.
 */
public final class CardinalLandmark {

    /** Half the diamond's diagonal, in pixels. */
    public static final double DIAMOND = 6.0;

    /** From the exact point to the near edge of an adjacent letter. */
    public static final double GAP = DIAMOND + 3.0;

    private CardinalLandmark() {
    }

    /**
     * How far a landmark reaches to one side of its exact point with
     * its letter beside it: the gap and the widest letter this page's
     * language writes for a direction, in the face letters are
     * drawn in.
     */
    public static double clearance(FontMetrics letters, PageWords words) {
        double widest = 0.0;
        for (Cardinal direction : Cardinal.values()) {
            widest = Math.max(widest,
                    letters.stringWidth(words.directionLetter(direction)));
        }
        return GAP + widest;
    }
}
