package juranometria.ui;

import java.awt.Rectangle;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Where the chart window was (Sprint 41, issue #450, ruled on #449):
 * its last ordinary bounds, and whether it was maximised. Remembered
 * so the observatory arrangement - the chart maximised on the
 * external monitor, Controls on the laptop - is not rebuilt at every
 * launch.
 *
 * <p>The <em>ordinary</em> bounds: the window's size and place while
 * it was in its normal state. They are kept while the window is
 * maximised, so leaving the maximised state returns it to where it
 * was, and they are never taken from native macOS full screen, which
 * is not remembered at all (ruled on #449).
 *
 * <p>Anything unreadable is "never chosen": a rectangle with a
 * missing or non-positive side is no rectangle, and the window opens
 * where it always has.
 */
public interface ChartWindowStore {

    Optional<Rectangle> bounds();

    void saveBounds(Rectangle bounds);

    /** Whether the window was maximised; false when never chosen. */
    boolean maximized();

    void saveMaximized(boolean maximized);

    /** The reader's own store, beside the application's other preferences. */
    static ChartWindowStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    static ChartWindowStore forNode(Preferences node) {
        return new ChartWindowStore() {

            @Override
            public Optional<Rectangle> bounds() {
                int x = node.getInt("window.x", Integer.MIN_VALUE);
                int y = node.getInt("window.y", Integer.MIN_VALUE);
                int width = node.getInt("window.width", 0);
                int height = node.getInt("window.height", 0);
                if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE
                        || width <= 0 || height <= 0) {
                    return Optional.empty();
                }
                return Optional.of(new Rectangle(x, y, width, height));
            }

            @Override
            public void saveBounds(Rectangle bounds) {
                node.putInt("window.x", bounds.x);
                node.putInt("window.y", bounds.y);
                node.putInt("window.width", bounds.width);
                node.putInt("window.height", bounds.height);
            }

            @Override
            public boolean maximized() {
                return "true".equals(node.get("window.maximized", null));
            }

            @Override
            public void saveMaximized(boolean maximized) {
                node.put("window.maximized", Boolean.toString(maximized));
            }
        };
    }
}
