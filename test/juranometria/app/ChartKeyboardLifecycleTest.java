package juranometria.app;

import java.awt.AWTEvent;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.render.ChartOptions;
import juranometria.ui.ReaderInput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the palette leaves behind, which must be nothing (Sprint 31,
 * issue #312).
 *
 * <p>It listens to the whole toolkit while it is open - every mouse
 * press in the application, so that a click anywhere else closes it -
 * and it listens to its window. Both are the kind of listener that is
 * easy to add and easy to forget, and a reader who opens and closes
 * the keyboard fifty times in an evening would be paying for every
 * one of them.
 *
 * <p>So every way out is walked, repeatedly, and the toolkit is asked
 * afterwards how many listeners it is holding. Including the way out
 * nobody plans for: the window going away while the palette is still
 * on it.
 *
 * <p>Each route is taken through the shared helper, with its premise
 * - the window is really focused, the click really lands on a control
 * a pointer could reach - because a route that only appears to happen
 * proves nothing about a listener it never woke.
 */
class ChartKeyboardLifecycleTest {

    @Test
    void everyWayOutLeavesTheToolkitAsItFoundIt() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the palette is shown in a real window");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            JFrame[] window = new JFrame[1];
            JButton[] elsewhere = new JButton[1];
            SwingSession.guarded(() -> {
                int before = mouseListeners();

                // A window per round, and the routes in this order.
                // Looking away sets the focused window to none, and a
                // desktop with a window manager hands the focus back
                // when the reader returns while a bare X server does
                // not - so the route that needs a focused window is
                // taken before the route that takes the focus away,
                // and the next round starts from a window freshly
                // shown (CI, #312).
                for (int round = 0; round < 3; round++) {
                    show(window, elsewhere, round);
                    int focusListeners = onEdt(() ->
                            window[0].getWindowFocusListeners().length);

                    leaveBy("Escape", window[0], keyboard ->
                            ReaderInput.shortcut(keyboard,
                                    KeyEvent.VK_ESCAPE, 0));
                    assertEquals(before, mouseListeners(),
                            "Escape leaves the toolkit as it found it,"
                                    + " round " + round);
                    assertEquals(focusListeners, onEdt(() -> window[0]
                                    .getWindowFocusListeners().length),
                            "and the window too");

                    leaveBy("a click elsewhere", window[0],
                            keyboard -> ReaderInput.click(elsewhere[0]));
                    assertEquals(before, mouseListeners(),
                            "a click elsewhere leaves nothing behind,"
                                    + " round " + round);
                    assertEquals(focusListeners, onEdt(() -> window[0]
                                    .getWindowFocusListeners().length),
                            "and nothing on the window");

                    leaveBy("looking away", window[0],
                            ReaderInput::lookAway);
                    assertEquals(before, mouseListeners(),
                            "and so does looking away, round " + round);
                    assertEquals(focusListeners, onEdt(() -> window[0]
                                    .getWindowFocusListeners().length),
                            "with the window's own listeners back");

                    SwingUtilities.invokeAndWait(window[0]::dispose);
                    flush();
                    assertEquals(before, mouseListeners(),
                            "and the round ends where it began, round "
                                    + round);
                }

                // The way out nobody plans for.
                show(window, elsewhere, 3);
                ChartKeyboard open = ChartKeyboard.of(
                        ChartKeysTest.switches(ChartOptions.DEFAULTS));
                SwingUtilities.invokeAndWait(() ->
                        open.showIn(window[0].getRootPane()));
                flush();
                assertTrue(onEdt(open::isOpen),
                        "the palette is open when the window closes");
                assertTrue(mouseListeners() > before,
                        "and is listening while it is");
                SwingUtilities.invokeAndWait(window[0]::dispose);
                flush();
                assertEquals(before, mouseListeners(),
                        "a window disposed with the palette open still"
                                + " takes its listeners with it");
                assertFalse(onEdt(open::isOpen),
                        "and the palette knows it is gone");
            }, () -> SwingUtilities.invokeAndWait(() -> {
                if (window[0] != null) {
                    window[0].dispose();
                }
            }));
        });
    }

    /** A window with something in it that is not the palette. */
    private static void show(JFrame[] window, JButton[] elsewhere,
                             int round) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame(
                    "chart keyboard lifecycle " + round);
            elsewhere[0] = new JButton("somewhere else");
            frame.add(elsewhere[0]);
            frame.setSize(600, 400);
            window[0] = frame;
            frame.setVisible(true);
        });
        flush();
    }

    /** Opens the palette, leaves it the given way, and checks it went. */
    private void leaveBy(String how, JFrame window, Exit exit)
            throws Exception {
        ChartKeyboard keyboard = ChartKeyboard.of(
                ChartKeysTest.switches(ChartOptions.DEFAULTS));
        SwingUtilities.invokeAndWait(() ->
                keyboard.showIn(window.getRootPane()));
        flush();
        assertTrue(onEdt(keyboard::isOpen), how + ": it opened");
        exit.leave(keyboard);
        flush();
        assertFalse(onEdt(keyboard::isOpen), how + ": and it closed");
    }

    @FunctionalInterface
    private interface Exit {
        void leave(ChartKeyboard keyboard) throws Exception;
    }

    private static int mouseListeners() throws Exception {
        return onEdt(() -> Toolkit.getDefaultToolkit()
                .getAWTEventListeners(AWTEvent.MOUSE_EVENT_MASK).length);
    }

    private static <T> T onEdt(java.util.concurrent.Callable<T> ask)
            throws Exception {
        Object[] answer = new Object[1];
        Exception[] trouble = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                answer[0] = ask.call();
            } catch (Exception failure) {
                trouble[0] = failure;
            }
        });
        if (trouble[0] != null) {
            throw trouble[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) answer[0];
        return typed;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
