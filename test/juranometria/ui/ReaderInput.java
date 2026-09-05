package juranometria.ui;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a journey presses what a reader presses (Sprint 26, issue
 * #243).
 *
 * <p>The discipline Sprint 25's review rounds built - premises
 * first, then real events - written once instead of once per
 * journey. Every helper here states what it needs before it acts:
 *
 * <ul>
 *   <li>a <strong>pointer</strong> target must be showing, sized,
 *       and its clicked point within the visible rectangle, because
 *       dispatched events land on a clipped or zero-sized control
 *       just as happily as on a real one;</li>
 *   <li>a <strong>keyboard</strong> target must own the focus,
 *       because dispatched keystrokes run a control's bindings
 *       whether or not a reader's keys could ever have reached it -
 *       where the desktop refuses focus, the journey aborts with a
 *       stated reason rather than pretending.</li>
 * </ul>
 *
 * <p>The one accepted back door is {@code doClick()} on a
 * {@code JMenuItem}: a menu item's action is its whole surface, and
 * the decision document records the convention. Buttons, check
 * boxes and text fields inside windows are pressed and typed at
 * through here.
 */
public final class ReaderInput {

    private ReaderInput() {
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    /**
     * A pointer click at the middle of a control the premises first
     * prove a pointer could reach.
     */
    public static void click(JComponent control) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(control.isShowing(),
                    name(control) + " is on screen, in a window a"
                            + " reader can see");
            assertTrue(control.getWidth() > 0 && control.getHeight() > 0,
                    name(control) + " has a size a pointer could hit: "
                            + control.getWidth() + "x"
                            + control.getHeight());
            int x = control.getWidth() / 2;
            int y = control.getHeight() / 2;
            assertTrue(control.getVisibleRect().contains(x, y),
                    "the point clicked on " + name(control)
                            + " is one a reader could reach: " + x + ","
                            + y + " within " + control.getVisibleRect());
            for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                    MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED}) {
                control.dispatchEvent(new MouseEvent(control, id,
                        System.nanoTime() / 1_000_000, 0, x, y, 1,
                        false, MouseEvent.BUTTON1));
            }
        });
        flush();
    }

    /**
     * A pointer click at a chosen point of a control - a table row,
     * a header cell - with the modifiers the reader's other hand
     * holds. The same premises as the centred click, proven for the
     * actual point before anything is dispatched: the control is
     * showing in a window, it has a size, and the point lies inside
     * its visible rectangle - which is only evidence of
     * reachability when the control is really on screen, so showing
     * is asserted first (post-approval review, #261).
     */
    public static void click(JComponent control, int x, int y,
                             int modifiersEx) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(control.isShowing(),
                    name(control) + " is on screen, in a window a"
                            + " reader can see");
            assertTrue(control.getWidth() > 0 && control.getHeight() > 0,
                    name(control) + " has a size a pointer could hit: "
                            + control.getWidth() + "x"
                            + control.getHeight());
            assertTrue(control.getVisibleRect().contains(x, y),
                    "the point clicked on " + name(control)
                            + " is one a reader could reach: " + x + ","
                            + y + " within " + control.getVisibleRect());
            for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                    MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED}) {
                control.dispatchEvent(new MouseEvent(control, id,
                        System.nanoTime() / 1_000_000,
                        id == MouseEvent.MOUSE_PRESSED
                                ? java.awt.event.InputEvent
                                        .BUTTON1_DOWN_MASK | modifiersEx
                                : modifiersEx,
                        x, y, 1, false, MouseEvent.BUTTON1));
            }
        });
        flush();
    }

    /**
     * Types into a field the way a reader does: click in, prove the
     * keyboard arrived, platform select-all, the characters, Enter.
     * Aborts honestly where the desktop refuses the field focus.
     */
    public static void typeAndEnter(JTextField field, String text)
            throws Exception {
        click(field);
        // Insist, the way every focused journey already does: under
        // xvfb no window manager hands focus to a click, and the CI
        // display run proved a bare click-then-check aborts there
        // while insisting succeeds (#243). Where the desktop truly
        // refuses, this still aborts with the focus subsystem's own
        // state as the reason.
        java.awt.Window window =
                SwingUtilities.getWindowAncestor(field);
        Assumptions.assumeTrue(window != null,
                name(field) + " sits in a window a desktop could"
                        + " focus");
        FocusedWindow.insistOnFocus(window, field);
        press(field, KeyEvent.VK_A,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        for (char typed : text.toCharArray()) {
            SwingUtilities.invokeAndWait(() -> field.dispatchEvent(
                    new KeyEvent(field, KeyEvent.KEY_TYPED,
                            System.nanoTime() / 1_000_000, 0,
                            KeyEvent.VK_UNDEFINED, typed)));
        }
        press(field, KeyEvent.VK_ENTER, 0);
        flush();
    }

    /**
     * Chooses a tab by its title, with a pointer, at the tab's own
     * header - the route a reader takes to reach controls that live
     * on it. Controls on an unchosen tab are not showing, and the
     * premises will refuse them: the first migration run caught a
     * journey toggling a checkbox on a tab nobody had opened.
     */
    public static void chooseTab(javax.swing.JTabbedPane tabs,
                                 String title) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(tabs.isShowing(),
                    "the tab strip is on screen");
            int index = tabs.indexOfTab(title);
            assertTrue(index >= 0, "the strip offers " + title);
            java.awt.Rectangle header = tabs.getBoundsAt(index);
            assertTrue(header != null && header.width > 0,
                    title + "'s header has a place a pointer could"
                            + " press");
            int x = header.x + header.width / 2;
            int y = header.y + header.height / 2;
            assertTrue(tabs.getVisibleRect().contains(x, y),
                    "the point pressed on " + title + "'s header is"
                            + " one a reader could reach: " + x + ","
                            + y + " within " + tabs.getVisibleRect());
            for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                    MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED}) {
                tabs.dispatchEvent(new MouseEvent(tabs, id,
                        System.nanoTime() / 1_000_000, 0, x, y, 1,
                        false, MouseEvent.BUTTON1));
            }
            assertTrue(tabs.getSelectedIndex() == index,
                    "the pointer chose " + title);
        });
        flush();
    }

    /**
     * A platform shortcut, with its premise: the target's window
     * must hold the keyboard focus, insisted on the way the focused
     * journeys insist, because a WHEN_IN_FOCUSED_WINDOW binding a
     * reader could fire needs a focused window to fire in - and a
     * dispatched key runs the binding either way, which is how two
     * journeys pressed shortcuts no reader could have pressed
     * (review). Aborts with the focus subsystem's own state where
     * the desktop refuses.
     */
    public static void shortcut(JComponent target, int keyCode,
                                int modifiers) throws Exception {
        java.awt.Window window =
                SwingUtilities.getWindowAncestor(target);
        Assumptions.assumeTrue(window != null,
                name(target) + " sits in a window a desktop could"
                        + " focus");
        Assumptions.assumeTrue(FocusedWindow.tryToFocus(window),
                "this desktop would not give the window the keyboard"
                        + " focus, so a shortcut has nowhere to"
                        + " arrive. " + FocusedWindow.state(window));
        press(target, keyCode, modifiers);
    }

    /**
     * A shortcut whose binding needs the target itself focused - a
     * tab strip's Ctrl-PageUp, a table's arrows - insisted on
     * component-deep, not merely window-deep (review): window focus
     * proves a reader could press an accelerator, and only target
     * focus proves they could walk this control.
     */
    public static void shortcutOn(JComponent target, int keyCode,
                                  int modifiers) throws Exception {
        java.awt.Window window =
                SwingUtilities.getWindowAncestor(target);
        Assumptions.assumeTrue(window != null,
                name(target) + " sits in a window a desktop could"
                        + " focus");
        FocusedWindow.insistOnFocus(window, target);
        press(target, keyCode, modifiers);
    }

    /**
     * A key pressed and released at a control - the one raw
     * dispatcher in the whole suite, pinned to this file by the
     * gate, and private (review): a public premise-free press was a
     * bypass wearing the helper's name, and two journeys walked
     * through it behind a requestFocusInWindow the desktop is free
     * to refuse silently. Every outside caller takes a route that
     * proves its premise - typeAndEnter, shortcut, or shortcutOn.
     */
    private static void press(JComponent control, int keyCode,
                              int modifiers) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (int id : new int[] {KeyEvent.KEY_PRESSED,
                    KeyEvent.KEY_RELEASED}) {
                control.dispatchEvent(new KeyEvent(control, id,
                        System.nanoTime() / 1_000_000, modifiers,
                        keyCode, KeyEvent.CHAR_UNDEFINED));
            }
        });
        flush();
    }

    private static String name(JComponent control) {
        if (control.getName() != null) {
            return control.getName();
        }
        String accessible = control.getAccessibleContext()
                .getAccessibleName();
        return accessible != null ? accessible
                : control.getClass().getSimpleName();
    }
}
