package juranometria.ui.placeandtime;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.time.Instant;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.meridian.MeridianModule;
import juranometria.module.TestChartServices;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dialog as a window, not as a panel (Sprint 25, issue #228).
 *
 * <p>The other tests hold the semantics against the dialog's content
 * built headless, and every commitment there went through the Enter
 * path or the listener list; none of it proves the packed window -
 * its single instance, its close paths, or a commit made the way a
 * reader actually makes one, by pressing Tab (review). So this
 * builds the real thing on a real display, and aborts rather than
 * pretends where the desktop refuses a window focus.
 */
class PlaceAndTimeDialogLifecycleTest {

    private static final Instant WHEN =
            Instant.parse("2026-03-20T21:33:00Z");

    private final Preferences node = Preferences.userRoot().node(
            "juranometria-test-place-life-" + System.nanoTime());

    private JFrame owner;

    @AfterEach
    void closeEverything() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Window window : Window.getWindows()) {
                if (window.isDisplayable()) {
                    window.dispose();
                }
            }
        });
        node.removeNode();
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static int dialogsShowing() {
        int count = 0;
        for (Window window : Window.getWindows()) {
            if (window instanceof PlaceAndTimeDialog
                    && window.isDisplayable()) {
                count++;
            }
        }
        return count;
    }

    private static java.awt.Component named(java.awt.Component root,
                                            String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        if (root instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                java.awt.Component found = named(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void theDialogOpensOnceHoldsItsFloorAndCommitsOnRealFocusLoss()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a dialog is a window, and a window needs a display");

        MeridianModule module = new MeridianModule(
                new Observer(59.913, 10.752, WHEN));
        module.attach(new TestChartServices());
        PlaceStore store = PlaceStore.forNode(node);

        SwingUtilities.invokeAndWait(() -> {
            owner = new JFrame("lifecycle");
            owner.setSize(600, 400);
            owner.setVisible(true);
        });
        SwingUtilities.invokeAndWait(() -> PlaceAndTimeDialog.open(
                owner, module, store, () -> WHEN));
        flush();

        assertEquals(1, dialogsShowing(), "one dialog");
        PlaceAndTimeDialog dialog = (PlaceAndTimeDialog) java.util.Arrays
                .stream(Window.getWindows())
                .filter(w -> w instanceof PlaceAndTimeDialog
                        && w.isDisplayable())
                .findFirst().orElseThrow();
        assertEquals("Place and Time", dialog.getTitle());
        assertTrue(dialog.getWidth() >= PlaceAndTimeDialog.ORDINARY_WIDTH,
                "the packed window holds the reviewed floor: "
                        + dialog.getWidth() + " px");

        // Opening again brings the one dialog forward rather than
        // multiplying stale copies - the Chart Options discipline.
        SwingUtilities.invokeAndWait(() -> PlaceAndTimeDialog.open(
                owner, module, store, () -> WHEN));
        flush();
        assertEquals(1, dialogsShowing(),
                "opened twice is still one dialog");

        // Every control fits inside the packed window: the claim the
        // photographs make, asserted on the real geometry.
        for (String name : new String[] {"latitudeField",
                "longitudeField", "instantField", "nowButton",
                "centreButton", "showMeridian",
                "showMathematicalhorizon", "showZenith"}) {
            java.awt.Component control = named(dialog.getContentPane(),
                    name);
            assertTrue(control != null, "the dialog carries " + name);
            assertTrue(control.getWidth() > 0 && control.getHeight() > 0
                            && control.getWidth()
                                    >= control.getMinimumSize().width,
                    name + " is laid out unclipped: " + control.getWidth()
                            + "x" + control.getHeight() + " against a"
                            + " minimum of "
                            + control.getMinimumSize().width);
        }

        // A commitment made the way a reader makes one: type in the
        // latitude field and Tab away. The desktop must actually give
        // the window focus for that to mean anything, and where it
        // will not, this aborts rather than pretends.
        JTextField latitude = (JTextField) named(dialog.getContentPane(),
                "latitudeField");
        JTextField longitude = (JTextField) named(dialog.getContentPane(),
                "longitudeField");
        SwingUtilities.invokeAndWait(() -> {
            dialog.toFront();
            latitude.requestFocusInWindow();
        });
        flush();
        Assumptions.assumeTrue(latitude.isFocusOwner(),
                "this desktop would not give the dialog the keyboard"
                        + " focus, so leaving a field cannot happen");

        SwingUtilities.invokeAndWait(() -> latitude.setText("-33.87"));
        assertEquals(59.913, module.observer().latitudeDegrees(),
                "typed and not yet left: the module holds what it"
                        + " held");
        SwingUtilities.invokeAndWait(longitude::requestFocusInWindow);
        flush();
        assertEquals(-33.87, module.observer().latitudeDegrees(),
                "moving to the next field committed the last one -"
                        + " real focus traversal, not a synthesized"
                        + " event");

        // Escape closes; the committed state stays committed.
        SwingUtilities.invokeAndWait(() -> dialog.getRootPane()
                .dispatchEvent(new java.awt.event.KeyEvent(
                        dialog.getRootPane(),
                        java.awt.event.KeyEvent.KEY_PRESSED,
                        System.currentTimeMillis(), 0,
                        java.awt.event.KeyEvent.VK_ESCAPE,
                        java.awt.event.KeyEvent.CHAR_UNDEFINED)));
        flush();
        assertEquals(0, dialogsShowing(), "Escape closed the window");
        assertEquals(-33.87, module.observer().latitudeDegrees(),
                "and closing takes nothing back: a committed field has"
                        + " already spoken");
    }
}
