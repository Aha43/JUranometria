package juranometria.ui;

import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Whether the chart window shows its toolbar (Sprint 41, issue #450,
 * ruled on #449): the reader's choice, remembered. Visible is the
 * default and the released behaviour; a bar hidden at a clean quit is
 * hidden at the next start, and View's item or its shortcut shows it
 * again.
 *
 * <p>Only the bar's visibility. What the bar's controls show is the
 * controller's and the switches'; what the companion shows is its own
 * store's. Hiding the bar is remembered independently of everything
 * it holds.
 */
public interface ChartChromeStore {

    /** The reader's choice, or empty when never chosen. */
    Optional<Boolean> toolbarShown();

    void saveToolbarShown(boolean shown);

    /** Shown, unless the reader chose otherwise: visible is the default. */
    default boolean toolbarShownOrDefault() {
        return toolbarShown().orElse(true);
    }

    /** The reader's own store, beside the application's other preferences. */
    static ChartChromeStore user() {
        return forNode(Preferences.userRoot().node("juranometria"));
    }

    static ChartChromeStore forNode(Preferences node) {
        return new ChartChromeStore() {

            @Override
            public Optional<Boolean> toolbarShown() {
                String chosen = node.get("chartToolbarShown", null);
                if ("true".equals(chosen)) {
                    return Optional.of(Boolean.TRUE);
                }
                if ("false".equals(chosen)) {
                    return Optional.of(Boolean.FALSE);
                }
                // Anything unreadable is "never chosen", and the
                // default stands: a bar that vanished because a
                // preference was damaged would be a bar nothing
                // explained.
                return Optional.empty();
            }

            @Override
            public void saveToolbarShown(boolean shown) {
                node.put("chartToolbarShown", Boolean.toString(shown));
            }
        };
    }
}
