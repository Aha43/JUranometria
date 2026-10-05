package juranometria.ui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

/**
 * Where the chart window opens, and which of its bounds are worth
 * remembering (Sprint 41, issue #450, ruled on #449) - as arithmetic
 * over rectangles, so it can be held without a display.
 *
 * <p><strong>Opening.</strong> A remembered rectangle is used only if
 * its title strip - the part a reader grabs to move it - lies on a
 * screen that exists now; then it is fitted to that screen, no larger
 * than its usable area and moved inside it. Otherwise the window opens
 * as it always has: packed to its chart and centred, which the caller
 * does. The rule is the companion's (#434), so a monitor that has gone
 * leaves both windows reachable.
 *
 * <p><strong>Remembering.</strong> Only ordinary bounds are worth
 * keeping: not a maximised window's, which the maximised flag
 * remembers separately, and not native macOS full screen's, which is
 * not remembered at all. Full screen is a window in its normal state
 * that fills its screen below the menu bar - measured on #449: a
 * full-screen frame at 0,33 1512 × 949 on a 1512 × 982 screen with a
 * 33 px bar - and a window that fills the usable area while the dock
 * hides would be read the same way, which is the stated limit of the
 * rule.
 */
public final class ChartWindowPlacement {

    /** How tall the strip a reader moves the window by is taken to be. */
    static final int TITLE = 32;

    /** How much of that strip must be on a screen to count as reachable. */
    static final int REACHABLE_WIDTH = 64;

    private ChartWindowPlacement() {
    }

    /**
     * @param remembered what the store holds, if anything
     * @param screens    every screen's usable area, now
     * @param minimum    the smallest the window may be
     * @return where to open, or empty to open as the application always has
     */
    public static Optional<Rectangle> opening(Optional<Rectangle> remembered,
                                              List<Rectangle> screens,
                                              Dimension minimum) {
        if (remembered.isEmpty() || screens.isEmpty()) {
            return Optional.empty();
        }
        Rectangle was = remembered.get();
        Rectangle title = new Rectangle(was.x, was.y, was.width,
                Math.min(TITLE, was.height));
        for (Rectangle screen : screens) {
            Rectangle seen = title.intersection(screen);
            if (!seen.isEmpty() && seen.width >= Math.min(REACHABLE_WIDTH, was.width)
                    && seen.height >= Math.min(TITLE / 2, was.height)) {
                int width = Math.min(Math.max(was.width, minimum.width), screen.width);
                int height = Math.min(Math.max(was.height, minimum.height),
                        screen.height);
                int x = Math.max(screen.x, Math.min(was.x,
                        screen.x + screen.width - width));
                int y = Math.max(screen.y, Math.min(was.y,
                        screen.y + screen.height - height));
                return Optional.of(new Rectangle(x, y, width, height));
            }
        }
        return Optional.empty();
    }

    /** Every screen's usable area, now: its bounds less the system's own bars. */
    public static List<Rectangle> screensNow() {
        List<Rectangle> usable = new java.util.ArrayList<>();
        for (java.awt.GraphicsDevice device : java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment().getScreenDevices()) {
            java.awt.GraphicsConfiguration configuration =
                    device.getDefaultConfiguration();
            Rectangle bounds = configuration.getBounds();
            Insets bars = java.awt.Toolkit.getDefaultToolkit()
                    .getScreenInsets(configuration);
            usable.add(new Rectangle(bounds.x + bars.left, bounds.y + bars.top,
                    bounds.width - bars.left - bars.right,
                    bounds.height - bars.top - bars.bottom));
        }
        return usable;
    }

    /**
     * Whether a window's bounds are ordinary ones worth remembering,
     * read from the window's own screen.
     */
    public static boolean ordinary(java.awt.Frame frame) {
        java.awt.GraphicsConfiguration configuration =
                frame.getGraphicsConfiguration();
        if (configuration == null) {
            return false;
        }
        return ordinary(frame.getBounds(),
                frame.getExtendedState() == java.awt.Frame.NORMAL,
                configuration.getBounds(),
                java.awt.Toolkit.getDefaultToolkit().getScreenInsets(configuration));
    }

    /**
     * Whether these bounds are ordinary ones worth remembering: the
     * window is in its normal state, and it does not fill its screen
     * from the menu bar down, which is what native full screen looks
     * like to the toolkit.
     *
     * @param bounds    the window's bounds now
     * @param normal    whether the window's extended state is normal
     * @param screen    the whole screen the window is on
     * @param bars      that screen's insets: the menu bar, the dock
     */
    public static boolean ordinary(Rectangle bounds, boolean normal,
                                   Rectangle screen, Insets bars) {
        if (!normal) {
            return false;
        }
        boolean fillsBelowTheBar = bounds.x == screen.x
                && bounds.width == screen.width
                && bounds.y == screen.y + bars.top
                && bounds.y + bounds.height == screen.y + screen.height;
        return !fillsBelowTheBar;
    }
}
