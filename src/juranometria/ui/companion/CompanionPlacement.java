package juranometria.ui.companion;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

/**
 * Where the companion window goes (#434, ruled on #433), as
 * arithmetic over rectangles so it can be held without a display.
 *
 * <p>A remembered rectangle is used only if its heading strip - the
 * part a reader grabs to move it - lies on a screen that exists now.
 * Then it is fitted to that screen: no larger than its usable area,
 * and moved inside it. Otherwise, or with nothing remembered, the
 * companion goes beside the chart window's trailing edge on that
 * window's screen when there is room, and over that edge when there
 * is not - an owned window stays above its owner, so overlapping it
 * hides chart, never the companion.
 */
public final class CompanionPlacement {

    /** How tall the strip a reader moves the window by is taken to be. */
    static final int HEADING = 32;

    /** How much of that strip must be on a screen to count as reachable. */
    static final int REACHABLE_WIDTH = 64;

    /** The gap left between the chart window and the companion. */
    static final int GAP = 8;

    private CompanionPlacement() {
    }

    /**
     * @param remembered what the store holds, if anything
     * @param screens    every screen's usable area, now
     * @param owner      the chart window's bounds
     * @param preferred  the companion's preferred size
     * @param minimum    the narrowest it may be
     */
    public static Rectangle place(Optional<Rectangle> remembered,
                                  List<Rectangle> screens,
                                  Rectangle owner, Dimension preferred,
                                  Dimension minimum) {
        if (screens.isEmpty()) {
            throw new IllegalArgumentException(
                    "a window is placed on some screen");
        }
        if (remembered.isPresent()) {
            Rectangle was = remembered.get();
            Rectangle heading = new Rectangle(was.x, was.y, was.width,
                    Math.min(HEADING, was.height));
            for (Rectangle screen : screens) {
                Rectangle seen = heading.intersection(screen);
                if (!seen.isEmpty() && seen.width >= Math.min(
                        REACHABLE_WIDTH, was.width)
                        && seen.height >= Math.min(HEADING / 2, was.height)) {
                    return fitted(was, screen, minimum);
                }
            }
        }
        Rectangle screen = screenOf(owner, screens);
        int width = Math.min(Math.max(preferred.width, minimum.width),
                screen.width);
        int height = Math.min(Math.max(preferred.height, minimum.height),
                screen.height);
        int beside = owner.x + owner.width + GAP;
        int x = beside + width <= screen.x + screen.width
                ? beside
                : Math.max(screen.x,
                        Math.min(owner.x + owner.width, screen.x + screen.width)
                                - width);
        int y = Math.max(screen.y, Math.min(owner.y,
                screen.y + screen.height - height));
        return new Rectangle(x, y, width, height);
    }

    /** Fitted to a screen: no larger than it, and inside it. */
    static Rectangle fitted(Rectangle was, Rectangle screen,
                            Dimension minimum) {
        int width = Math.min(Math.max(was.width, minimum.width), screen.width);
        int height = Math.min(Math.max(was.height, minimum.height),
                screen.height);
        int x = Math.max(screen.x, Math.min(was.x,
                screen.x + screen.width - width));
        int y = Math.max(screen.y, Math.min(was.y,
                screen.y + screen.height - height));
        return new Rectangle(x, y, width, height);
    }

    /** The screen holding most of the window; the first if none does. */
    static Rectangle screenOf(Rectangle window, List<Rectangle> screens) {
        Rectangle best = screens.get(0);
        long most = -1;
        for (Rectangle screen : screens) {
            Rectangle overlap = window.intersection(screen);
            long area = overlap.isEmpty() ? 0
                    : (long) overlap.width * overlap.height;
            if (area > most) {
                most = area;
                best = screen;
            }
        }
        return best;
    }
}
