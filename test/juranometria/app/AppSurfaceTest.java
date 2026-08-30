package juranometria.app;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The menu bar, the appearance boundary, and the Settings content. */
class AppSurfaceTest {

    // ------------------------------------------------------------ menu

    @Test
    void theMenuBarIsRestrainedAndWiresItsTwoActions() {
        int[] settings = new int[1];
        int[] about = new int[1];
        JMenuBar bar = AppMenuBar.create(
                () -> settings[0]++, () -> about[0]++);
        assertEquals(2, bar.getMenuCount(),
                "File and Help - nothing else");
        assertEquals("File", bar.getMenu(0).getText());
        assertEquals(1, bar.getMenu(0).getItemCount(),
                "no placeholder items beside Settings");
        assertEquals("Help", bar.getMenu(1).getText());
        assertEquals(1, bar.getMenu(1).getItemCount(),
                "no placeholder items beside About");

        JMenuItem settingsItem = bar.getMenu(0).getItem(0);
        assertEquals("Settings...", settingsItem.getText());
        settingsItem.doClick();
        assertEquals(1, settings[0]);

        JMenuItem aboutItem = bar.getMenu(1).getItem(0);
        assertEquals("About " + AppInfo.NAME, aboutItem.getText());
        assertTrue(aboutItem.getAccessibleContext().getAccessibleName()
                .contains("About"));
        aboutItem.doClick();
        assertEquals(1, about[0]);
    }

    @Test
    void withoutSettingsTheMenuBarCarriesHelpAlone() {
        JMenuBar bar = AppMenuBar.create(null, () -> { });
        assertEquals(1, bar.getMenuCount());
        assertEquals("Help", bar.getMenu(0).getText());
    }

    // ------------------------------------------------------ appearance

    @Test
    void theStoreRoundTripsWithoutTouchingRealPreferences() throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            AppearanceStore store = AppearanceStore.forNode(node);
            assertEquals(Optional.empty(), store.load());
            assertFalse(store.storedDark(), "no preference means light");

            store.save(AppearanceStore.DARK);
            assertEquals(Optional.of("dark"), store.load());
            assertTrue(store.storedDark());

            store.save(AppearanceStore.LIGHT);
            assertFalse(store.storedDark());

            assertThrows(IllegalArgumentException.class,
                    () -> store.save("sepia"),
                    "only the two known appearances persist");

            node.put("appearance", "banana");
            assertFalse(store.storedDark(),
                    "corrupt or unknown stored values mean the light"
                            + " default, never a launch failure");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void theDarkFlagWinsForTheSessionWithoutRewritingTheStore() throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            AppearanceStore store = AppearanceStore.forNode(node);
            store.save(AppearanceStore.LIGHT);

            assertTrue(AppearanceStore.sessionDark(true, store),
                    "--dark wins for the launch");
            assertEquals(Optional.of("light"), store.load(),
                    "the override never rewrites the stored choice");

            store.save(AppearanceStore.DARK);
            assertTrue(AppearanceStore.sessionDark(false, store),
                    "without the flag the stored choice decides");
        } finally {
            node.removeNode();
        }
    }

    // -------------------------------------------------------- settings

    @Test
    void onlyOkConfirmsAndItReportsTheSelectedAppearance() {
        java.util.List<Boolean> confirmed = new java.util.ArrayList<>();
        JComponent content = SettingsDialog.content(false, confirmed::add);

        JRadioButton dark = radio(content, "Dark appearance");
        JRadioButton light = radio(content, "Light appearance");
        assertTrue(light.isSelected(), "the current appearance is preselected");

        dark.doClick();
        assertTrue(confirmed.isEmpty(),
                "selecting a radio button applies and persists nothing");

        AboutDialogTest.button(content, "OK").doClick();
        assertEquals(java.util.List.of(true), confirmed,
                "OK confirms exactly the selected appearance");

        // Cancel on a fresh panel confirms nothing.
        java.util.List<Boolean> cancelled = new java.util.ArrayList<>();
        JComponent second = SettingsDialog.content(true, cancelled::add);
        assertTrue(radio(second, "Dark appearance").isSelected(),
                "a dark session preselects Dark");
        AboutDialogTest.button(second, "Cancel").doClick();
        assertTrue(cancelled.isEmpty(),
                "Cancel leaves setting and appearance untouched");
    }

    private static JRadioButton radio(java.awt.Component component,
                                      String accessibleName) {
        if (component instanceof JRadioButton radioButton
                && accessibleName.equals(radioButton
                        .getAccessibleContext().getAccessibleName())) {
            return radioButton;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                JRadioButton found = radio(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
