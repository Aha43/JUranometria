package juranometria.tool.labels;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;

/**
 * One piece of text a candidate policy would draw, and where (Sprint
 * 31, issue #310).
 *
 * <p>A candidate page is the production page with the text families
 * the candidate is responsible for switched off, and these written
 * back on top in the same fonts and the same ink. That is what makes
 * a candidate answerable to the same oracle as production: a study
 * that only counted its own rectangles would be measuring its own
 * arithmetic, and the pages here are painted, read back, and
 * attributed exactly as the released page is.
 *
 * @param family which family this text belongs to
 * @param id the participant's stable identity - a catalogue id or a
 *     constellation abbreviation, never the rendered words
 * @param text the glyphs drawn
 * @param x left edge of the drawn string
 * @param baseline the baseline the string is drawn on
 * @param box the box the policy reserved, which is not the ink
 * @param anchorX where the thing being named actually is
 * @param anchorY where the thing being named actually is
 */
public record PlacedText(Participant.Family family, String id, String text,
                         double x, double baseline, Rectangle2D box,
                         double anchorX, double anchorY, Font font,
                         Color ink) {

    /** How far this text sits from the thing it names, in pixels. */
    public double displacement(double homeX, double homeBaseline) {
        return Math.hypot(x - homeX, baseline - homeBaseline);
    }
}
