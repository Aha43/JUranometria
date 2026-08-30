package juranometria.app;

import org.junit.jupiter.api.Test;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import juranometria.chart.SkyPosition;
import juranometria.ui.ChartViewController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The platform zoom shortcuts and View-menu zoom items (issue #125):
 * every practical plus/minus form binds through the shared
 * centre-preserving actions on the platform menu mask, enablement
 * follows the controller like the toolbar, and unmodified typing is
 * never intercepted - the bindings all carry the mask, and the
 * Search field keeps plain keystrokes to itself.
 */
class ZoomShortcutsTest {

    private static final int MASK = AppMenuBar.menuShortcutMask();

    private static Action boundAction(JRootPane root, KeyStroke stroke) {
        Object key = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(stroke);
        return key == null ? null : root.getActionMap().get(key);
    }

    private static void fire(Action action) {
        action.actionPerformed(new ActionEvent(new Object(), 0, "test"));
    }

    @Test
    void everyPracticalPlusAndMinusFormZoomsAboutTheCentre() {
        ChartViewController navigation = new ChartViewController();
        navigation.recenter(new SkyPosition(10.684708, 41.268750), 8.0,
                "M31 · Andromeda Galaxy region", "NGC 224");
        JRootPane root = new JRootPane();
        AppMenuBar.installZoomShortcuts(root, navigation);

        KeyStroke[] zoomIn = {
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
                        MASK | InputEvent.SHIFT_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, MASK)};
        KeyStroke[] zoomOut = {
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, MASK)};

        for (KeyStroke stroke : zoomIn) {
            Action action = boundAction(root, stroke);
            assertNotNull(action, stroke + " binds zoom in");
            SkyPosition centre = navigation.state().centre();
            double field = navigation.state().fieldWidthDegrees();
            fire(action);
            assertTrue(navigation.state().fieldWidthDegrees() < field,
                    stroke + " zooms in one step");
            assertEquals(centre, navigation.state().centre(),
                    "keyboard zoom is centre-preserving");
            assertEquals("NGC 224", navigation.state().targetIdentity(),
                    "and keeps the target, exactly like the toolbar");
            fire(boundAction(root,
                    KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MASK)));
        }
        for (KeyStroke stroke : zoomOut) {
            Action action = boundAction(root, stroke);
            assertNotNull(action, stroke + " binds zoom out");
            double field = navigation.state().fieldWidthDegrees();
            fire(action);
            assertTrue(navigation.state().fieldWidthDegrees() > field,
                    stroke + " zooms out one step");
            fire(boundAction(root,
                    KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, MASK)));
        }
    }

    @Test
    void shortcutsRespectBoundsAndCoverageLikeTheToolbar() {
        ChartViewController navigation = new ChartViewController();
        navigation.recenter(new SkyPosition(10.684708, 41.268750), 36.0);
        JRootPane root = new JRootPane();
        AppMenuBar.installZoomShortcuts(root, navigation);
        int[] notified = {0};
        navigation.onChange(state -> notified[0]++);
        notified[0] = 0;
        fire(boundAction(root,
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MASK)));
        assertEquals(36.0, navigation.state().fieldWidthDegrees(),
                "zoom out at the widest page is a guarded no-op");
        assertEquals(0, notified[0], "and notifies nobody");

        // A coverage predicate that rejects the candidate: the guarded
        // action consults canZoom* - the same predicate path as the
        // toolbar - so the refused shortcut changes nothing and
        // notifies nobody either.
        ChartViewController fenced = new ChartViewController(
                state -> state.fieldWidthDegrees() >= 12.0);
        fenced.recenter(new SkyPosition(10.684708, 41.268750), 12.0,
                "M31 · Andromeda Galaxy region", "NGC 224");
        JRootPane fencedRoot = new JRootPane();
        AppMenuBar.installZoomShortcuts(fencedRoot, fenced);
        int[] fencedNotified = {0};
        fenced.onChange(state -> fencedNotified[0]++);
        fencedNotified[0] = 0;
        fire(boundAction(fencedRoot,
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, MASK)));
        assertEquals(12.0, fenced.state().fieldWidthDegrees(),
                "a coverage-refused shortcut changes nothing");
        assertEquals("NGC 224", fenced.state().targetIdentity(),
                "and keeps the target untouched");
        assertEquals(0, fencedNotified[0], "and notifies nobody");
    }

    @Test
    void theViewMenuGainsCentrePreservingZoomItemsWithAccelerators() {
        ChartViewController navigation = new ChartViewController();
        navigation.recenter(new SkyPosition(10.684708, 41.268750), 36.0);
        JMenuBar bar = AppMenuBar.create(navigation, null, () -> { },
                () -> { });
        JMenu view = bar.getMenu(0);
        assertEquals("View", view.getText());
        assertEquals("Chart Options...", view.getItem(0).getText(),
                "Chart Options stays the first View item");
        JMenuItem zoomIn = itemNamed(view, "Zoom In");
        JMenuItem zoomOut = itemNamed(view, "Zoom Out");
        assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, MASK),
                zoomIn.getAccelerator());
        assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MASK),
                zoomOut.getAccelerator());
        assertFalse(zoomOut.isEnabled(),
                "enablement follows the controller: the widest page"
                        + " cannot zoom out");
        assertTrue(zoomIn.isEnabled());
        zoomIn.doClick();
        assertEquals(24.0, navigation.state().fieldWidthDegrees(),
                "the menu item is the toolbar's centre-preserving step");
        assertTrue(zoomOut.isEnabled(),
                "enablement updates with the state");
    }

    @Test
    void unmodifiedTypingIsNeverIntercepted() {
        // Every binding carries the platform mask, so plain '=', '+',
        // and '-' have no window-level binding at all - they remain
        // ordinary text on their way to the Search field.
        ChartViewController navigation = new ChartViewController();
        JRootPane root = new JRootPane();
        AppMenuBar.installZoomShortcuts(root, navigation);
        for (int key : new int[] {KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS,
                KeyEvent.VK_MINUS, KeyEvent.VK_ADD, KeyEvent.VK_SUBTRACT}) {
            assertNull(root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                            .get(KeyStroke.getKeyStroke(key, 0)),
                    "unmodified keys bind nothing");
        }
    }

    private static JMenuItem itemNamed(JMenu menu, String name) {
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && name.equals(item.getText())) {
                return item;
            }
        }
        throw new AssertionError("no View item named " + name);
    }
}
