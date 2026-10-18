package juranometria.module;

import java.util.List;

/**
 * A box of chart furniture a module asks the chart to carry (Sprint
 * 51, issue #259, from the rulings on #556 in
 * docs/decisions/chart-instant.md).
 *
 * <p>The first furniture that is not the renderer's own. The module
 * supplies <strong>meaning and text</strong> - what the box says, line
 * by line, in the page's language, and the identity of the one action
 * it offers - and nothing else. It never supplies a graphics context,
 * a component, a colour, a rectangle or a hit-test path: the chart
 * allocates the corner, lays the box out in the title block's face,
 * inks it in the page's palette, reserves it against every other piece
 * of text, hit-tests the action, speaks the box and decides what paper
 * gets. Removing the module removes the box and its action and leaves
 * the page byte for byte as it was.
 *
 * <p>The core learns no latitude, instant, clock or sidereal time from
 * this: the lines are text, and the action is a name the chart hands
 * back to the module that offered it.
 *
 * @param identity stable, unique among the module's furniture - the
 *     view report and the hit test name the box by it
 * @param heading the box's first line, set in the title block's bold
 * @param lines the lines under the heading, in order
 * @param action the one action the box offers on screen, or null
 * @param spoken what a reader who cannot see the page is told while
 *     the box is drawn: one sentence, appended to the chart's
 *     description
 * @param described the sentence an exported sheet's own description
 *     gains while the box is drawn on it
 */
public record Furniture(String identity, String heading, List<String> lines,
                        Action action, String spoken, String described) {

    public Furniture {
        if (identity == null || identity.isBlank()) {
            throw new IllegalArgumentException(
                    "furniture carries an identity, so the chart can name it");
        }
        if (heading == null || heading.isBlank()) {
            throw new IllegalArgumentException(
                    "furniture says what it is in its heading: " + identity);
        }
        lines = List.copyOf(lines);
        if (spoken == null || spoken.isBlank()) {
            throw new IllegalArgumentException(
                    "furniture is spoken as well as drawn: " + identity);
        }
        if (described == null || described.isBlank()) {
            throw new IllegalArgumentException(
                    "furniture on a sheet is described as well as drawn: "
                            + identity);
        }
    }

    /**
     * The box's one action: what the chart draws as a framed control on
     * screen, what paper prints in its place, and what a hovering reader
     * is told.
     *
     * @param identity handed back to the module when a reader activates
     *     the action, by pointer, keyboard or assistive technology
     * @param label the control's words on screen and its accessible name
     * @param paperLine the honest static sentence a sheet prints where
     *     the control would be - a control on paper cannot be pressed
     * @param explanation what the control does, on hover and as its
     *     accessible description
     */
    public record Action(String identity, String label, String paperLine,
                         String explanation) {

        public Action {
            if (identity == null || identity.isBlank()) {
                throw new IllegalArgumentException(
                        "an action has an identity the module answers to");
            }
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException(
                        "an action is named on the page: " + identity);
            }
            if (paperLine == null || paperLine.isBlank()) {
                throw new IllegalArgumentException(
                        "an action says what paper prints instead of it: "
                                + identity);
            }
            if (explanation == null || explanation.isBlank()) {
                throw new IllegalArgumentException(
                        "an action says what it does: " + identity);
            }
        }
    }
}
