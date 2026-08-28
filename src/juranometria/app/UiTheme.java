package juranometria.app;

import java.awt.Color;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;

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

    /** Icon stroke colors per theme; SVG files stay monochrome black. */
    private static final Color ICON_LIGHT_THEME = new Color(68, 68, 68);
    private static final Color ICON_DARK_THEME = new Color(204, 204, 204);

    /** Installs the application look and feel. Must run before UI creation. */
    public static void apply(boolean dark) {
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        // Tabler outline icons draw with currentColor (black); recolor the
        // strokes for contrast in the active theme. Chart ink is untouched —
        // the renderer never draws through this filter.
        FlatSVGIcon.ColorFilter.getInstance()
                .add(Color.BLACK, ICON_LIGHT_THEME, ICON_DARK_THEME);
    }
}
