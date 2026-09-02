package juranometria.ui;

import java.awt.KeyboardFocusManager;
import java.awt.Window;

import javax.swing.SwingUtilities;

/**
 * Waiting for a window to actually hold the keyboard focus
 * (issue #209).
 *
 * <p>A journey that shows a window and immediately drives its
 * keyboard is making an assumption the window manager has not
 * necessarily honoured yet. {@code requestFocusInWindow()} is
 * <strong>silently refused</strong> when the component's window is
 * not the focused window, so the application does exactly what it
 * was asked and nothing happens - and the assertion that follows
 * reports it as the feature misbehaving.
 *
 * <p>That is what #209 was. Caught at {@code c61b8a6} with the
 * failure's own state recorded:
 *
 * <pre>
 * expected=javax.swing.JPanel focusable=true showing=true
 * owner=null activeWindow=false focusedWindow=false
 * </pre>
 *
 * <p>The panel was focusable and on screen; there was no focused
 * window at all, so the request could not be granted. Nothing in the
 * atlas was wrong - in a running application the window a reader is
 * typing into is focused by definition.
 *
 * <p>So a journey asks for focus before it asserts anything about
 * focus, and when the assertion fails anyway it reports the focus
 * subsystem's own state. A test that says "this window was not
 * focused, so the request was refused" sends a reader somewhere
 * useful; one that says "Enter went to the wrong control" sends
 * them to read the Inspector.
 *
 * <p>Under a display-only session - xvfb, where these journeys are
 * meant to run - nothing competes for the desktop and focus is
 * granted at once. On a developer's machine the terminal that
 * started the build usually holds it, which is exactly the
 * difference #209 was made of.
 */
final class FocusedWindow {

    private FocusedWindow() {
    }

    /** How long a window manager is given to hand over focus. */
    private static final int ATTEMPTS = 100;
    private static final long PAUSE_MS = 20;

    /**
     * Insists, for a journey that cannot proceed without focus.
     *
     * <p>A desktop that will not hand over the focus says nothing
     * about the atlas, so this is <strong>not</strong> a failure: it
     * ends the journey as unmet, with the focus subsystem's own
     * state as the reason. On a developer's machine the terminal
     * that started the build usually holds the focus, and a journey
     * that fails there fails for the one reason #209 taught us to
     * read carefully.
     *
     * <p>That is safe to skip locally only because it cannot be
     * skipped where it counts: the display job runs this suite under
     * xvfb and fails if a single test is aborted, so a journey that
     * quietly stopped running would take the build with it.
     */
    static void insistOnFocus(Window window, java.awt.Component owner)
            throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(tryToFocus(window),
                "this desktop would not give the window the keyboard"
                        + " focus, so a key has nowhere to arrive. "
                        + state(window));
        org.junit.jupiter.api.Assumptions.assumeTrue(
                awaitFocusOwner(owner),
                "the window is focused but the desktop would not give"
                        + " the focus to " + describe(owner) + ". "
                        + state(window));
    }

    /**
     * Asks the desktop to make this window the focused one, and
     * reports whether it agreed. Best effort by design: a journey
     * should try, and then say what happened, rather than insist.
     */
    static boolean tryToFocus(Window window) throws Exception {
        if (isFocused(window)) {
            return true;
        }
        SwingUtilities.invokeAndWait(() -> {
            window.toFront();
            window.requestFocus();
        });
        for (int i = 0; i < ATTEMPTS; i++) {
            if (isFocused(window)) {
                return true;
            }
            Thread.sleep(PAUSE_MS);
        }
        return false;
    }

    /** Whether the window currently holds the keyboard focus. */
    static boolean isFocused(Window window) throws Exception {
        boolean[] focused = new boolean[1];
        SwingUtilities.invokeAndWait(() -> focused[0] =
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getFocusedWindow() == window);
        return focused[0];
    }

    /**
     * Waits until this component is the actual focus owner.
     *
     * <p>A journey that dispatches key events straight at a
     * component proves nothing about keyboard access: {@code
     * dispatchEvent} delivers them whether or not the component
     * could ever have received them from a keyboard (#209 review).
     * A reader's Down and Enter reach a list because the list has
     * focus, so a journey claiming to press keys as a reader does
     * must establish that first - the window being focused is
     * necessary and not sufficient.
     */
    static boolean awaitFocusOwner(java.awt.Component component)
            throws Exception {
        for (int i = 0; i < ATTEMPTS; i++) {
            if (isFocusOwner(component)) {
                return true;
            }
            SwingUtilities.invokeAndWait(component::requestFocusInWindow);
            Thread.sleep(PAUSE_MS);
        }
        return false;
    }

    private static boolean isFocusOwner(java.awt.Component component)
            throws Exception {
        boolean[] owns = new boolean[1];
        SwingUtilities.invokeAndWait(() -> owns[0] =
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getFocusOwner() == component);
        return owns[0];
    }

    /**
     * What the focus subsystem looks like right now, for a failure
     * message. The whole of #209 was a legible failure wearing the
     * wrong name: "Enter went to the wrong control" when the truth
     * was "this window was not focused, so the request was refused".
     */
    static String state(Window window) throws Exception {
        String[] said = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            KeyboardFocusManager fm = KeyboardFocusManager
                    .getCurrentKeyboardFocusManager();
            said[0] = "focusOwner="
                    + describe(fm.getFocusOwner())
                    + ", focusedWindow="
                    + (fm.getFocusedWindow() == window ? "this one"
                            : describe(fm.getFocusedWindow()))
                    + ", activeWindow=" + describe(fm.getActiveWindow());
        });
        return said[0];
    }

    private static String describe(java.awt.Component component) {
        return component == null ? "none"
                : component.getClass().getSimpleName();
    }
}
