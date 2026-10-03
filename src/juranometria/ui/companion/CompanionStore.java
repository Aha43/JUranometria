package juranometria.ui.companion;

import java.awt.Rectangle;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * What the companion window remembers about itself (#434, ruled on
 * #433): where it was, how large, whether it was open, and which of
 * its sections were collapsed.
 *
 * <p>Only the window. The place, the instant and the lines it shows
 * are the module's and the place store's, and nothing here touches
 * them: visibility is remembered independently of the Place and Time
 * state, as ruled.
 *
 * <p>Anything unreadable is "never chosen": a rectangle with a
 * missing or non-positive side is no rectangle, and the window is
 * placed as if nothing had been remembered.
 */
public interface CompanionStore {

    Optional<Rectangle> bounds();

    void saveBounds(Rectangle bounds);

    /** Whether the companion was open; false when never chosen. */
    boolean visible();

    void saveVisible(boolean visible);

    /** Whether a section was collapsed; false when never chosen. */
    default boolean collapsed(String section) {
        return collapsed(section, false);
    }

    /**
     * Whether a section was collapsed, or the host's own default when
     * the reader never chose (#443: Deep sky's group is introduced
     * collapsed). A presentation default - nothing to do with what the
     * chart draws.
     */
    boolean collapsed(String section, boolean ifNeverChosen);

    void saveCollapsed(String section, boolean collapsed);

    /** The reader's own store, beside the application's other preferences. */
    static CompanionStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    static CompanionStore forNode(Preferences node) {
        return new CompanionStore() {

            @Override
            public Optional<Rectangle> bounds() {
                int x = node.getInt("companion.x", Integer.MIN_VALUE);
                int y = node.getInt("companion.y", Integer.MIN_VALUE);
                int width = node.getInt("companion.width", 0);
                int height = node.getInt("companion.height", 0);
                if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE
                        || width <= 0 || height <= 0) {
                    return Optional.empty();
                }
                return Optional.of(new Rectangle(x, y, width, height));
            }

            @Override
            public void saveBounds(Rectangle bounds) {
                node.putInt("companion.x", bounds.x);
                node.putInt("companion.y", bounds.y);
                node.putInt("companion.width", bounds.width);
                node.putInt("companion.height", bounds.height);
            }

            @Override
            public boolean visible() {
                return "true".equals(node.get("companion.visible", null));
            }

            @Override
            public void saveVisible(boolean visible) {
                node.put("companion.visible", Boolean.toString(visible));
            }

            @Override
            public boolean collapsed(String section, boolean ifNeverChosen) {
                String chosen = node.get("companion.collapsed." + section, null);
                if ("true".equals(chosen)) {
                    return true;
                }
                if ("false".equals(chosen)) {
                    return false;
                }
                return ifNeverChosen;
            }

            @Override
            public void saveCollapsed(String section, boolean collapsed) {
                node.put("companion.collapsed." + section,
                        Boolean.toString(collapsed));
            }
        };
    }
}
