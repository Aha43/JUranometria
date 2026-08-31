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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The menu bar, the appearance boundary, and the Settings content. */
class AppSurfaceTest {

    // ------------------------------------------------------------ menu

    @Test
    void theMenuBarIsRestrainedAndWiresItsThreeActions() {
        int[] settings = new int[1];
        int[] chartOptions = new int[1];
        int[] about = new int[1];
        JMenuBar bar = AppMenuBar.create(() -> settings[0]++,
                () -> chartOptions[0]++, () -> about[0]++);
        assertEquals(3, bar.getMenuCount(),
                "File, View, and Help - nothing else");
        assertEquals("File", bar.getMenu(0).getText());
        assertEquals(1, bar.getMenu(0).getItemCount(),
                "no placeholder items beside Settings");
        assertEquals("View", bar.getMenu(1).getText());
        assertEquals(1, bar.getMenu(1).getItemCount(),
                "no placeholder items beside Chart Options");
        assertEquals("Help", bar.getMenu(2).getText());
        assertEquals(1, bar.getMenu(2).getItemCount(),
                "no placeholder items beside About");

        JMenuItem settingsItem = bar.getMenu(0).getItem(0);
        assertEquals("Settings...", settingsItem.getText());
        settingsItem.doClick();
        assertEquals(1, settings[0]);

        JMenuItem chartOptionsItem = bar.getMenu(1).getItem(0);
        assertEquals("Chart Options...", chartOptionsItem.getText());
        assertEquals("Chart Options", chartOptionsItem
                .getAccessibleContext().getAccessibleName());
        chartOptionsItem.doClick();
        assertEquals(1, chartOptions[0]);

        JMenuItem aboutItem = bar.getMenu(2).getItem(0);
        assertEquals("About " + AppInfo.NAME, aboutItem.getText());
        assertTrue(aboutItem.getAccessibleContext().getAccessibleName()
                .contains("About"));
        aboutItem.doClick();
        assertEquals(1, about[0]);
    }


    @Test
    void theInspectorItemShowsWhetherThePanelIsActuallyThere() {
        // Review, P2: a plain menu item cannot say whether the panel
        // is showing - and a narrow window can close it without the
        // reader asking.
        javax.swing.JMenuBar bar = AppMenuBar.create(null, () -> { },
                () -> { }, () -> { }, () -> { });
        javax.swing.JCheckBoxMenuItem item = AppMenuBar.inspectorItem(bar);

        assertNotNull(item, "View carries an Inspector item");
        assertEquals("Inspector",
                item.getAccessibleContext().getAccessibleName());
        assertNotNull(item.getAccelerator(),
                "with a platform shortcut, so it is reachable without"
                        + " the mouse");
        assertFalse(item.isSelected(), "and it starts unchecked");

        item.setSelected(true);
        assertTrue(item.isSelected(),
                "the panel drives this, including when the window's"
                        + " width closes it rather than the reader");
    }

    @Test
    void withoutOptionalActionsTheMenuBarCarriesHelpAlone() {
        JMenuBar bar = AppMenuBar.create(null, null, () -> { });
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
    void theDarkOverrideStaysAuthoritativeThroughTheSettingsPath() throws Exception {
        // Sprint 11 Codex review, P1: the override must survive the
        // production Settings-confirmation path, not just startup.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            AppearanceStore store = AppearanceStore.forNode(node);
            store.save(AppearanceStore.LIGHT);
            AppearanceSession overridden = new AppearanceSession(store, true);

            assertTrue(overridden.startupDark(), "--dark wins for the launch");
            assertFalse(overridden.savedDark(),
                    "the dialog preselects the saved preference, never the"
                            + " override's effect");

            // Merely confirming the preselected (saved) choice cannot
            // convert the override into a stored Dark preference.
            assertTrue(overridden.confirmChoice(overridden.savedDark()),
                    "the overridden session stays dark");
            assertEquals(Optional.of("light"), store.load(),
                    "confirming the preselection rewrote nothing");

            // An explicit Light choice persists for the next ordinary
            // launch but cannot turn this overridden session light.
            assertTrue(overridden.confirmChoice(false),
                    "the session remains pinned dark by the override");
            assertEquals(Optional.of("light"), store.load());

            // An explicit Dark choice is a real preference change.
            assertTrue(overridden.confirmChoice(true));
            assertEquals(Optional.of("dark"), store.load(),
                    "an explicit choice persists for future launches");

            // Without the override, the choice governs the live session.
            AppearanceSession ordinary = new AppearanceSession(store, false);
            assertTrue(ordinary.startupDark(),
                    "an ordinary launch follows the stored choice");
            assertFalse(ordinary.confirmChoice(false),
                    "without the override, choosing Light turns the"
                            + " session light");
            assertEquals(Optional.of("light"), store.load());
        } finally {
            node.removeNode();
        }
    }

    // -------------------------------------------------------- settings

    @Test
    void onlyOkConfirmsAndItReportsTheSelectedAppearance() {
        java.util.List<Boolean> confirmed = new java.util.ArrayList<>();
        JComponent content = SettingsDialog.content(false, false,
                confirmed::add);

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
        JComponent second = SettingsDialog.content(true, false,
                cancelled::add);
        assertTrue(radio(second, "Dark appearance").isSelected(),
                "a saved dark preference preselects Dark");
        AboutDialogTest.button(second, "Cancel").doClick();
        assertTrue(cancelled.isEmpty(),
                "Cancel leaves setting and appearance untouched");
    }

    @Test
    void anActiveOverrideIsExplainedInsideTheDialog() {
        JComponent overridden = SettingsDialog.content(false, true, b -> { });
        assertTrue(hasLabelContaining(overridden, "--dark"),
                "the dialog says the session is overridden and when the"
                        + " choice applies");
        JComponent ordinary = SettingsDialog.content(false, false, b -> { });
        assertFalse(hasLabelContaining(ordinary, "--dark"),
                "no note without an override");
    }

    private static boolean hasLabelContaining(java.awt.Component component,
                                              String text) {
        if (component instanceof javax.swing.JLabel label
                && label.getText() != null
                && label.getText().contains(text)) {
            return true;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                if (hasLabelContaining(child, text)) {
                    return true;
                }
            }
        }
        return false;
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
