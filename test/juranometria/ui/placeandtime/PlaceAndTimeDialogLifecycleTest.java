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
 *
 * <p>And through the doors a reader actually uses (review): the
 * dialog is opened by clicking the View menu's own item, closed once
 * by Escape and once by the window's close box, and reopened - a
 * first version called {@code open()} directly and closed only with
 * Escape, so the menu wiring and the closing path could both have
 * broken while every test stayed green.
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
            // The real menu bar with the real wiring, so what is
            // clicked below is the route a reader takes - not a call
            // straight past it into open() (review).
            owner.setJMenuBar(juranometria.app.AppMenuBar.create(null,
                    () -> { }, () -> { }, () -> { }, null,
                    () -> PlaceAndTimeDialog.open(owner, module, store,
                            () -> WHEN)));
            owner.setVisible(true);
        });
        SwingUtilities.invokeAndWait(() -> menuItem(owner,
                "Place and Time...").doClick());
        flush();

        assertEquals(1, dialogsShowing(),
                "the View menu's item opened the dialog");
        PlaceAndTimeDialog dialog = (PlaceAndTimeDialog) java.util.Arrays
                .stream(Window.getWindows())
                .filter(w -> w instanceof PlaceAndTimeDialog
                        && w.isDisplayable())
                .findFirst().orElseThrow();
        assertEquals("Place and Time", dialog.getTitle());
        assertTrue(dialog.getWidth() >= PlaceAndTimeDialog.ORDINARY_WIDTH,
                "the packed window holds the reviewed floor: "
                        + dialog.getWidth() + " px");

        // Choosing the item again brings the one dialog forward
        // rather than multiplying stale copies - the Chart Options
        // discipline, exercised through the same menu.
        SwingUtilities.invokeAndWait(() -> menuItem(owner,
                "Place and Time...").doClick());
        flush();
        assertEquals(1, dialogsShowing(),
                "chosen twice is still one dialog");

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

        // Escape closes; the committed state stays committed. With
        // its premise, through the shared shortcut route - the
        // anti-masking pin caught this very dispatch borrowing a
        // premise from the field traversal above (#243 review).
        juranometria.ui.ReaderInput.shortcut(dialog.getRootPane(),
                java.awt.event.KeyEvent.VK_ESCAPE, 0);
        flush();
        assertEquals(0, dialogsShowing(), "Escape closed the window");
        assertEquals(-33.87, module.observer().latitudeDegrees(),
                "and closing takes nothing back: a committed field has"
                        + " already spoken");

        // Reopened from the menu after Escape: a singleton that only
        // checked for null would refuse to ever open again, and no
        // test had asked (review).
        SwingUtilities.invokeAndWait(() -> menuItem(owner,
                "Place and Time...").doClick());
        flush();
        assertEquals(1, dialogsShowing(),
                "the menu opens a fresh dialog after Escape closed"
                        + " the last one");
        PlaceAndTimeDialog reopened = (PlaceAndTimeDialog)
                java.util.Arrays.stream(Window.getWindows())
                        .filter(w -> w instanceof PlaceAndTimeDialog
                                && w.isDisplayable())
                        .findFirst().orElseThrow();
        assertEquals("-33.87", ((JTextField) named(
                        reopened.getContentPane(), "latitudeField"))
                        .getText(),
                "and it wears the module's committed state, because"
                        + " the state was never the dialog's to lose");

        // The other way out: the window's own close box, which
        // arrives as WINDOW_CLOSING rather than a keystroke.
        SwingUtilities.invokeAndWait(() -> reopened.dispatchEvent(
                new java.awt.event.WindowEvent(reopened,
                        java.awt.event.WindowEvent.WINDOW_CLOSING)));
        flush();
        assertEquals(0, dialogsShowing(),
                "the close box closes the window too");
        assertEquals(-33.87, module.observer().latitudeDegrees(),
                "and takes nothing back either");

        // And one more reopen, so neither closing path is the one
        // that quietly breaks the next opening.
        SwingUtilities.invokeAndWait(() -> menuItem(owner,
                "Place and Time...").doClick());
        flush();
        assertEquals(1, dialogsShowing(),
                "the dialog opens again after the close box closed"
                        + " it");
    }

    /** The named item on the frame's real menu bar. */
    private static javax.swing.JMenuItem menuItem(JFrame frame,
                                                  String text) {
        javax.swing.JMenuBar bar = frame.getJMenuBar();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            var menu = bar.getMenu(i);
            for (int j = 0; j < menu.getItemCount(); j++) {
                var item = menu.getItem(j);
                if (item != null && text.equals(item.getText())) {
                    return item;
                }
            }
        }
        throw new AssertionError("the menu carries " + text);
    }
}
