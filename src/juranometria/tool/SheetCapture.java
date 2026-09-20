package juranometria.tool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * One rule for photographing a real widget (Sprint 33, issue #350).
 *
 * <p>The twelve interface-language photographers each had their own
 * idea of when a window was ready to be painted, and two of them were
 * wrong about one run in six. `chartoptions-nb-NO-1.png` came out
 * with its button row shifted <strong>one pixel</strong> to the
 * right: everything left of x=192 identical, everything from x=253
 * moved by one. The export dialog reported a packed width of 304 px
 * instead of 295, which is the previous format's width, because it
 * chose a format and packed in the same event block.
 *
 * <p>Both are the same mistake. A Swing component's size is not
 * settled when the call that changed it returns: listeners revalidate,
 * and revalidation is posted. Painting at that moment photographs a
 * layout on its way somewhere, and the picture is right most of the
 * time - which is worse than being wrong every time, because it looks
 * like evidence.
 *
 * <p>So capture is a <strong>declared state</strong> rather than a
 * moment: the queue is drained until it is empty, the layout is
 * required to reach a <strong>fixed point</strong> (two validations
 * producing the same geometry), and the focus owner is required to be
 * nobody. If any of those cannot be established this
 * <strong>refuses</strong>. A generator that cannot say what it is
 * photographing must not write a file.
 *
 * <p><strong>This controls evidence, not the application.</strong>
 * Nothing here disables focus painting; the atlas a reader runs draws
 * its focus rings exactly as before. What is fixed is only which
 * state gets photographed.
 */
public final class SheetCapture {

    private SheetCapture() {
    }

    /**
     * How many validate-and-drain rounds a layout is allowed before
     * it is called unsettled.
     *
     * <p>Generous, because refusing a slow machine would be a false
     * alarm; the races actually seen settle in one extra round.
     */
    private static final int ROUNDS = 40;

    /**
     * Photographs a component once its state is established.
     *
     * <p>Call from off the event thread: this drives the event queue
     * and would deadlock on it.
     *
     * @param window the realised window the component lives in, or
     *     {@code null} for a component with no window of its own
     * @param content what to paint
     * @param to where the PNG goes
     */
    public static void write(Window window, JComponent content, Path to)
            throws Exception {
        BufferedImage drawn = of(window, content);
        ImageIO.write(drawn, "png", to.toFile());
    }

    /** The same, when a caller wants the image rather than a file. */
    public static BufferedImage of(Window window, JComponent content)
            throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("capture drives the event"
                    + " queue and cannot run on it");
        }
        if (content == null) {
            throw new IllegalArgumentException(
                    "there is nothing to photograph");
        }
        settle(window, content);
        BufferedImage[] drawn = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            neutralFocusNow();
            drawn[0] = paint(content);
        });
        return drawn[0];
    }

    /**
     * Settles a component and whatever window it lives in.
     *
     * <p>The form most of the photographers use: they already hold
     * the component they are about to paint, and the window is
     * whatever contains it.
     */
    public static void settle(JComponent content) throws Exception {
        if (content == null) {
            throw new IllegalArgumentException(
                    "there is nothing to settle");
        }
        settle(SwingUtilities.getWindowAncestor(content), content);
    }

    /**
     * Photographs a window that decides its own size.
     *
     * <p>Most windows here are packed, so their size is their
     * layout's preference and re-packing to a fixed point is the
     * right rule. Chart Options is not: it calls
     * {@code sizeToScreen(this, ORDINARY_WIDTH)}, so its width is a
     * <strong>policy</strong> the dialog states, and packing it
     * overrides that policy with a preference. That is what this
     * coordinator was doing - photographing the dialog 394 px wide
     * on some runs and 420 on others, the first being what pack
     * wanted and the second what the dialog had asked for.
     *
     * <p>A photographer may decide when to take the picture. It may
     * not decide how big the application is.
     */
    public static void writeSelfSized(Window window, JComponent content,
                                      Path to) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("capture drives the event"
                    + " queue and cannot run on it");
        }
        drain();
        canonicalise(content);
        settleLayout(window, content);
        BufferedImage[] drawn = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            neutralFocusNow();
            drawn[0] = paint(content);
        });
        ImageIO.write(drawn[0], "png", to.toFile());
    }

    /**
     * Establishes the state a photograph is allowed to be taken in.
     *
     * <p>Public because a generator that paints by another route -
     * one that needs the image at a size of its own - still has to
     * establish the same state first.
     */
    public static void settle(Window window, JComponent content)
            throws Exception {
        drain();
        canonicalise(content);
        packToFixedPoint(window, content);
        settleLayout(window, content);
    }

    /** Validates until validating again moves nothing. */
    private static void settleLayout(Window window, JComponent content)
            throws Exception {
        String geometry = geometryOf(content);
        for (int round = 0; round < ROUNDS; round++) {
            SwingUtilities.invokeAndWait(() -> {
                if (window != null) {
                    window.validate();
                }
                content.validate();
            });
            drain();
            String now = geometryOf(content);
            if (now.equals(geometry)) {
                // A fixed point: validating again moved nothing, and
                // the queue is empty. This is the earliest moment the
                // picture can be said to be OF something.
                //
                // Focus is NOT settled here. It cannot be: a shown
                // window can be given focus back by the desktop at
                // any moment, and ChartKeyboard's palette does
                // exactly that - clearing it here and painting later
                // refused about one run in three. Focus is declared
                // in the same event block as the paint instead,
                // where nothing can intervene.
                return;
            }
            geometry = now;
        }
        throw new IllegalStateException("this layout never settled:"
                + " " + ROUNDS + " validations in a row each moved"
                + " something. A photograph taken now would be of a"
                + " layout on its way somewhere, which is how a sheet"
                + " came out one pixel wide of itself about one run in"
                + " six.");
    }

    /**
     * Rebuilds every wrapping label's view, so packing starts from
     * one place.
     *
     * <p>This layout has <strong>two self-consistent answers</strong>.
     * A label whose text is HTML reports a preferred width that
     * depends on the width it was last laid out at, and both 415 and
     * 420 satisfy "the component is the size it asks to be" - so
     * requiring a fixed point is not enough, because there are two.
     * Which one a run falls into is decided by the width the label
     * happened to be measured at first.
     *
     * <p>Swing caches that measurement as a {@code View} in the
     * {@code "html"} client property. Setting the same text again
     * throws it away, so every run starts from no measurement at all
     * and the pack that follows has one answer rather than two.
     */
    private static void canonicalise(JComponent content)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> rebuildHtml(content));
        drain();
    }

    private static void rebuildHtml(Component from) {
        if (from instanceof JComponent widget
                && widget.getClientProperty("html") != null) {
            if (from instanceof javax.swing.JLabel label) {
                label.setText(label.getText());
            } else if (from instanceof javax.swing.AbstractButton button) {
                button.setText(button.getText());
            }
        }
        if (from instanceof Container parent) {
            for (Component child : parent.getComponents()) {
                rebuildHtml(child);
            }
            parent.invalidate();
        }
    }

    /**
     * Packs until packing stops changing the answer.
     *
     * <p>This is where the Chart Options flicker actually lived, and
     * it is not a pixel: the same dialog, built four times in one
     * JVM, packed to <strong>420</strong> wide and then
     * <strong>415</strong> and then 420 again. A label that wraps
     * reports a preferred width that depends on whether its view has
     * been laid out yet, so the first pack asks a question the
     * component cannot answer and the second gets a different
     * answer.
     *
     * <p>One pack is a guess. Packing until the size repeats is the
     * component's own answer, and if it never repeats this refuses -
     * a dialog that cannot say how wide it is has no business being
     * photographed.
     */
    private static void packToFixedPoint(Window window,
                                         JComponent content)
            throws Exception {
        if (window == null) {
            return;
        }
        java.awt.Dimension was = null;
        for (int round = 0; round < ROUNDS; round++) {
            java.awt.Dimension[] now = new java.awt.Dimension[1];
            boolean[] wanted = new boolean[1];
            SwingUtilities.invokeAndWait(() -> {
                window.pack();
                now[0] = window.getSize();
                // The condition that matters, and the one two equal
                // packs do NOT give you: the component is the size
                // it asks to be. A wrapping label's preferred width
                // depends on the width it was last laid out at, so
                // the answer can change AFTER the pack that used it
                // - leaving a window 420 wide around a layout that
                // now wants 415. Two packs agreed; the picture was
                // five pixels wrong.
                wanted[0] = content.getSize()
                        .equals(content.getPreferredSize());
            });
            drain();
            if (wanted[0] && now[0].equals(was)) {
                return;
            }
            was = now[0];
        }
        throw new IllegalStateException("this window never settled at"
                + " the size its own layout asks for, in " + ROUNDS
                + " attempts. Something's preferred size depends on"
                + " the width it was last given, and a photograph"
                + " would be of one of the answers rather than of"
                + " the dialog.");
    }

    /**
     * Empties the event queue, rather than assuming one round-trip
     * did.
     *
     * <p>A single {@code invokeAndWait} only guarantees that events
     * posted <em>before</em> it have run. A revalidation posts more
     * while it runs, and those are exactly the ones that move a
     * layout after the call that caused it has returned.
     */
    public static void drain() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            SwingUtilities.invokeAndWait(() -> { });
            if (Toolkit.getDefaultToolkit().getSystemEventQueue()
                    .peekEvent() == null) {
                return;
            }
        }
        throw new IllegalStateException("the event queue never"
                + " emptied: something is posting work faster than it"
                + " is consumed, and nothing painted now would be"
                + " reproducible");
    }

    /**
     * Prepares a window that has to be shown.
     *
     * <p>Most of these sheets photograph a window that is packed but
     * never shown, and nothing in such a window can hold focus. Two
     * must be shown - the toolbar, whose result list is a window of
     * its own, and the chart keyboard's palette - and a shown window
     * is given focus by the desktop, which hands it to a button.
     *
     * <p>Clearing that focus does not work: {@code
     * clearGlobalFocusOwner} does not take effect within the block
     * that calls it, so the state cannot be established where a
     * paint could rely on it. Making the window unfocusable
     * <em>before</em> it is shown does work, and works the same on
     * every desktop, because the question of who gets focus never
     * arises.
     *
     * <p>The application is untouched: a reader's toolbar takes
     * focus and draws its focus rings exactly as before. This is the
     * difference between a photograph and the thing photographed.
     */
    public static void prepareShownWindow(Window window) {
        if (window == null) {
            throw new IllegalArgumentException(
                    "there is no window to prepare");
        }
        window.setFocusableWindowState(false);
        window.setAutoRequestFocus(false);
    }

    /**
     * The declared focus state, established where it holds.
     *
     * <p>Nobody owns focus: every one of these sheets photographs a
     * surface a reader has not typed into yet, and neutral is the
     * only state portable across desktops - a window the desktop has
     * not activated cannot be made to hold focus at all, so
     * "focus this component and wait" would hang on some machines
     * and not others.
     *
     * <p><strong>Call this in the same event block as the paint.</strong>
     * Focus changes arrive as posted events, so nothing can take
     * focus back between this line and a paint on the same block -
     * and that is the whole guarantee. Clearing focus earlier and
     * painting later was refused about one run in three, because
     * ChartKeyboard's palette is shown and the desktop hands it
     * focus again.
     */
    public static void neutralFocusNow() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("the focus state has to"
                    + " be declared on the event thread, in the same"
                    + " block as the paint it applies to");
        }
        KeyboardFocusManager manager =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.clearGlobalFocusOwner();
        requireNeutralFocus(manager.getFocusOwner(),
                manager.getPermanentFocusOwner());
    }

    /**
     * The rule, separated from the environment that satisfies it.
     *
     * <p>Package-private so a test can hand it a state that must be
     * refused. Proving the refusal by arranging an unclearable focus
     * owner would be proving something about a desktop; this proves
     * what the code does with an answer it does not like.
     */
    static void requireNeutralFocus(Component owner,
                                    Component permanent) {
        if (owner == null && permanent == null) {
            return;
        }
        throw new IllegalStateException("the declared focus state for"
                + " a sheet is nobody, and it was not reached: focus"
                + " owner " + describe(owner) + ", permanent "
                + describe(permanent) + ". A focus ring that appears"
                + " in one run and not the next is not evidence of"
                + " anything, so this refuses rather than writing a"
                + " file it cannot describe.");
    }

    private static String describe(Component component) {
        return component == null ? "nobody"
                : component.getClass().getSimpleName()
                        + (component.getName() == null ? ""
                                : " \"" + component.getName() + "\"");
    }

    /**
     * Every component's bounds, as one string.
     *
     * <p>What "the layout has stopped moving" means, made
     * comparable. The one-pixel button shift that started this would
     * have changed this string and nothing else about the run.
     */
    private static String geometryOf(JComponent content)
            throws Exception {
        StringBuilder out = new StringBuilder();
        SwingUtilities.invokeAndWait(() -> append(out, content));
        return out.toString();
    }

    private static void append(StringBuilder out, Component from) {
        out.append(from.getClass().getSimpleName()).append('@')
                .append(from.getX()).append(',').append(from.getY())
                .append(' ').append(from.getWidth()).append('x')
                .append(from.getHeight()).append(';');
        if (from instanceof Container parent) {
            for (Component child : parent.getComponents()) {
                append(out, child);
            }
        }
    }

    /** The paint itself, with the hints every sheet has always used. */
    private static BufferedImage paint(JComponent content) {
        BufferedImage drawn = new BufferedImage(
                Math.max(1, content.getWidth()),
                Math.max(1, content.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = drawn.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(content.getBackground() == null
                    ? Color.WHITE : content.getBackground());
            g.fillRect(0, 0, drawn.getWidth(), drawn.getHeight());
            content.paint(g);
        } finally {
            g.dispose();
        }
        return drawn;
    }
}
