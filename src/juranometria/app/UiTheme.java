package juranometria.app;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Applies the FlatLaf look and feel for application chrome.
 *
 * The chart renderer must not use these theme colors; the atlas page keeps
 * its own explicit palette regardless of the application theme.
 */
public final class UiTheme {

    private UiTheme() {
    }

    /** Installs the light application look and feel. */
    public static void apply() {
        apply(false);
    }

    /** Installs the application look and feel. Must run before UI creation. */
    public static void apply(boolean dark) {
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }
}
