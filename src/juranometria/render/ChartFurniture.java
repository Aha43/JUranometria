package juranometria.render;

import java.util.List;

/**
 * A box of furniture the page carries beside its own title block and
 * magnitude key (Sprint 51, issue #259, the rulings on #556).
 *
 * <p>Text and nothing else: the renderer allocates its corner, lays it
 * out in the title block's face, margin and padding, reserves it against
 * the grid's figures and every placed name, and inks it in the page's
 * palette. Where it came from - a module, and what the module means by
 * it - is no business of the renderer's.
 *
 * @param key stable and unique on the page, the name the page reports
 *     and hit-tests the box by
 * @param heading the first line, in the title block's bold
 * @param lines the lines under it
 * @param action the framed control's words on screen, or null - paper
 *     carries no control, and its honest sentence comes as a line
 */
public record ChartFurniture(String key, String heading, List<String> lines,
                             String action) {

    public ChartFurniture {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("furniture is named on the page");
        }
        if (heading == null || heading.isBlank()) {
            throw new IllegalArgumentException("furniture has a heading: " + key);
        }
        lines = List.copyOf(lines);
        if (action != null && action.isBlank()) {
            throw new IllegalArgumentException(
                    "an action is named, or there is none: " + key);
        }
    }

    /**
     * The corners furniture may take, in the order the chart offers them
     * (ruling 3 on #556): the lower right, then the upper right while
     * the magnitude key does not hold it, then the upper left. The title
     * block keeps the lower left.
     */
    public enum Corner {
        LOWER_RIGHT, UPPER_RIGHT, UPPER_LEFT;

        /** How the view report writes it: lower-right, upper-right, upper-left. */
        public String token() {
            return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        }
    }

    /**
     * Where the page put a box: its corner, the rectangle it is laid out
     * and drawn in, the rectangle it covers (one pixel wider and taller,
     * as the title block's is), and its framed control, if any.
     */
    public record Placed(ChartFurniture furniture, Corner corner,
                         java.awt.Rectangle box, java.awt.Rectangle covers,
                         java.awt.Rectangle action) {
    }
}
