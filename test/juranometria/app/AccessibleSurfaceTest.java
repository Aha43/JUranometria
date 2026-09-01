package juranometria.app;

import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartViewController;
import juranometria.ui.SearchField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 1.0 contract's accessibility promise, checked mechanically
 * rather than by reading the sources (issue #145): "every control and
 * dialog carries an accessible name".
 *
 * The walk covers the surfaces a reader actually meets - the toolbar
 * with its search field, all three dialogs, and the menu bar - and
 * fails with the offending control named, so a future control added
 * without a name cannot reach a release quietly.
 */
class AccessibleSurfaceTest {

    /**
     * Controls the reader operates. Scroll bars are excluded with
     * their parts: their increment and decrement buttons belong to
     * the look and feel, are skipped by focus traversal, and are not
     * ours to name.
     */
    private static boolean isReaderControl(Component component) {
        return component instanceof AbstractButton
                || component instanceof JTextField
                || component instanceof JComboBox
                || component instanceof JSpinner;
    }

    private static boolean insideScrollBar(Component component) {
        for (Component c = component; c != null; c = c.getParent()) {
            if (c instanceof JScrollBar) {
                return true;
            }
        }
        return false;
    }

    private static void collectUnnamed(Component component, String surface,
                                       List<String> unnamed) {
        if (isReaderControl(component) && !insideScrollBar(component)) {
            String name = ((JComponent) component).getAccessibleContext()
                    .getAccessibleName();
            if (name == null || name.isBlank()) {
                String text = component instanceof AbstractButton button
                        ? button.getText() : "";
                unnamed.add(surface + ": "
                        + component.getClass().getSimpleName()
                        + " [" + text + "]");
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectUnnamed(child, surface, unnamed);
            }
        }
    }

    @Test
    void theTabStripsOwnControlsNameThemselvesWhenTheyAppear()
            throws Exception {
        // The scrolling tab layout adds arrow buttons and a
        // hidden-tabs button for itself when the four titles do not
        // fit, and they arrive unnamed. On Linux they appear at the
        // dialog's ordinary width; on macOS the titles fit and they
        // do not - which is how this reached CI green locally. Laying
        // the dialog out narrow forces them on any platform.
        List<String> unnamed = new ArrayList<>();
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-a11y-" + System.nanoTime());
        javax.swing.LookAndFeel inherited =
                javax.swing.UIManager.getLookAndFeel();
        try {
            SwingUtilities.invokeAndWait(() -> {
                // The application's own look and feel: the tab strip's
                // controls are the look and feel's, so asking any
                // other one proves nothing.
                UiTheme.apply(false);
                JComponent content = ChartOptionsDialog.content(
                        new ChartOptionsController(
                                ChartOptionsStore.forNode(node)),
                        () -> { }, () -> { });
                content.setSize(240, 500);
                content.doLayout();
                content.validate();
                javax.swing.JTabbedPane tabs =
                        ChartOptionsDialog.tabsOf(content);
                int buttons = 0;
                for (java.awt.Component child : tabs.getComponents()) {
                    if (child instanceof javax.swing.AbstractButton button) {
                        buttons++;
                        String name = button.getAccessibleContext()
                                .getAccessibleName();
                        if (name == null || name.isBlank()) {
                            unnamed.add(button.getClass().getSimpleName());
                        }
                    }
                }
                assertTrue(buttons > 0,
                        "the strip must actually need scrolling for"
                                + " this to prove anything");
            });
        } finally {
            node.removeNode();
            if (inherited != null) {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        javax.swing.UIManager.setLookAndFeel(inherited);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
        }
        assertEquals(List.of(), unnamed,
                "the tab strip's own controls name themselves too");
    }

    @Test
    void everyControlTheReaderCanOperateCarriesAnAccessibleName()
            throws Exception {
        List<String> unnamed = new ArrayList<>();
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-a11y-" + System.nanoTime());
        try {
            SwingUtilities.invokeAndWait(() -> {
                ChartViewController navigation =
                        new ChartViewController(Atlas.assembler()::fits);
                SearchField search = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                collectUnnamed(new AtlasToolbar(navigation, search),
                        "toolbar", unnamed);
                collectUnnamed(SettingsDialog.content(false, false,
                        dark -> { }), "Settings", unnamed);
                collectUnnamed(ChartOptionsDialog.content(
                                new ChartOptionsController(
                                        ChartOptionsStore.forNode(node)),
                                () -> { }, () -> { }),
                        "Chart Options", unnamed);
                collectUnnamed(AboutDialog.compactContent(() -> { }),
                        "About", unnamed);
                collectUnnamed(AboutDialog.noticesContent(),
                        "About notices", unnamed);

                JMenuBar menuBar = AppMenuBar.create(navigation,
                        () -> { }, () -> { }, () -> { });
                for (int i = 0; i < menuBar.getMenuCount(); i++) {
                    JMenu menu = menuBar.getMenu(i);
                    if (menu.getAccessibleContext()
                            .getAccessibleName() == null) {
                        unnamed.add("menu: " + menu.getText());
                    }
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.getAccessibleContext()
                                .getAccessibleName() == null) {
                            unnamed.add("menu item: " + item.getText());
                        }
                    }
                }
            });
        } finally {
            node.removeNode();
        }

        assertEquals(List.of(), unnamed,
                "every control a reader operates must name itself to"
                        + " assistive technology");
    }

    @Test
    void everyDialogNamesItselfAndClosesOnEscape() throws Exception {
        // Ownership and Escape are the contract's other two dialog
        // promises. Escape is registered on the root pane, so it is
        // visible without a screen: the three dialogs each install
        // it through the same shared helper.
        for (String source : List.of("AboutDialog.java", "SettingsDialog.java",
                "ChartOptionsDialog.java")) {
            String text = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/juranometria/app/" + source));
            assertTrue(text.contains("installEscapeToClose")
                            || text.contains("VK_ESCAPE"),
                    source + " must close on Escape");
            assertTrue(text.contains("setAccessibleName"),
                    source + " must name itself");
            assertTrue(text.contains("super(owner"),
                    source + " must be owned by the atlas window");
        }
    }
}
