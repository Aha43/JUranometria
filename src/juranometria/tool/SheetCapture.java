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

    /*
     * There is no capture that does not say which kind of window it
     * is photographing. The forms that did - write(window, content,
     * to) and of(window, content) - packed whatever they were given,
     * which is right for eight of these twelve photographers and
     * wrong for the other four. Chart Options and Place and Time
     * were each photographed at a width no reader meets before their
     * kinds were declared.
     */

    /**
     * What a photographer is holding still.
     *
     * <p>Declared by each generator as a typed constant, so the
     * compiler carries it and the evidence gate reads it rather than
     * a table someone maintains beside the code. Chart Options was
     * photographed at its packed 394 px and Place and Time at 326 -
     * widths no reader meets - because the kind was assumed.
     */
    public enum Kind {
        /** The size is the layout's preference. */
        PACKED,
        /** The size is a policy the application states. */
        APPLICATION_SIZED,
        /** No window: a component laid out at a chosen width. */
        FIXED_CANVAS
    }

    /**
     * How a window comes by its size, declared rather than assumed.
     *
     * <p>Two kinds, and a photographer must say which it is holding:
     *
     * <ul>
     *   <li><strong>packed</strong> - the size is the layout's
     *       preference, and packing to a fixed point is right;</li>
     *   <li><strong>application-sized</strong> - the size is a
     *       <em>policy</em> the application states, and packing
     *       overrides it with a preference the reader never
     *       meets.</li>
     * </ul>
     *
     * <p>The distinction was learned twice. Chart Options declares
     * {@code ORDINARY_WIDTH} through {@code sizeToScreen} and was
     * photographed at its packed 394 px until it was classified.
     * Place and Time raises its packed width to the same floor, and
     * was photographed at 326 px - under load, once in two runs -
     * because the coordinator packed it and the generator kept its
     * own copy of the arithmetic to undo that. A reader meets
     * neither width.
     *
     * <p>So the photographer no longer decides how big the
     * application is. It says which kind of window this is, and for
     * an application-sized one hands over the application's own
     * policy, which is re-applied until the size stops changing.
     */
    public interface Sizing {

        /** Brings the window to its size, or throws if it cannot. */
        void bring(Window window, JComponent content) throws Exception;

        /**
         * Records what bringing actually settled on.
         *
         * <p>Called on the event thread in the <strong>same
         * block</strong> that confirms the fixed point. Not after it:
         * reading the geometry and remembering it were two blocks
         * once, and a run slipped through the gap - painted 326x206
         * with a pre-paint record reading preferred=332x206, because
         * what {@code hold} put back was a geometry that had already
         * drifted before it was recorded.
         *
         * <p>This exists so that {@code hold} can restore a proved
         * geometry rather than derive one again. Re-deriving is what
         * the fixed point was reached to avoid: a wrapped label's
         * preferred width depends on the width it was last laid out
         * at, so a fresh pack can settle on the other answer - 326
         * where 333 was proved - and a hold that re-packs would
         * reintroduce exactly the defect it is there to prevent.
         */
        default void proved(Window window, JComponent content) {
        }

        /**
         * Holds that size in the block that paints.
         *
         * <p>Bringing a window to its size and painting it later is
         * not enough, and this is the second time that lesson has
         * been learned here: focus had to be declared in the paint
         * block because the desktop grants it asynchronously, and a
         * size has to be held there because <strong>the peer pulls
         * an unshown window back to its packed size an event cycle
         * later</strong>. Place and Time's generator carried a
         * hand-written restore for exactly that, immediately before
         * painting, and it was right to.
         *
         * <p>Runs on the event thread, with nothing between it and
         * the paint.
         */
        default void hold(Window window, JComponent content) {
        }
    }

    /**
     * What a photograph claims to be a picture of.
     *
     * <p>Checked <strong>inside</strong> the block that holds the
     * size and paints, because a premise checked anywhere else is a
     * statement about a moment that has already passed. Export's was
     * asserted between settling and painting, in an event cycle of
     * its own, and that cycle is where #364's recurrence happened:
     * the dialog agreed about its format, paper and marks, and was
     * then painted 326x206 where its own settled record said
     * 333x223.
     *
     * <p>A premise that disagrees means nothing is written. A missing
     * sheet is a question; a mislabelled one is an answer nobody
     * checks.
     */
    @FunctionalInterface
    public interface Premise {

        /**
         * What disagrees with what was asked, or {@code null}.
         *
         * <p>Runs on the event thread, with nothing between it and
         * the paint.
         */
        String disagreement(JComponent content);

        /** A photograph that claims nothing beyond its geometry. */
        static Premise none() {
            return content -> null;
        }
    }

    /**
     * The picture itself, drawn where the state cannot move.
     *
     * <p>The coordinator owns the <em>sequence</em>; a photographer
     * owns the <em>picture</em>. That division is deliberate: these
     * studies do not all paint one component onto one image. The menu
     * sheet composites a bar and its popups at computed offsets, the
     * toolbar sizes to whichever of bar and popup is wider, the
     * settings study paints at a width it chose, and several collect
     * the words they show while the components still exist. None of
     * that is the coordinator's business, and absorbing it would have
     * moved bytes for no reason.
     *
     * <p>What is the coordinator's business is that no event cycle
     * runs between settling the size and drawing it.
     */
    @FunctionalInterface
    public interface Picture {

        /**
         * Draws, on the event thread, inside the held block.
         *
         * @return the image, which is written exactly as returned
         */
        BufferedImage draw() throws Exception;
    }

    /**
     * A window whose size is its layout's preference.
     *
     * <p>Holds by <strong>restoring the geometry already proved
     * stable</strong>, never by deriving one again. A packed
     * window's hold was empty until #364 recurred, which made "hold
     * that size" vacuous for eight of the twelve photographers - the
     * size was brought to a fixed point and then simply hoped to
     * stay there across whatever event cycles the photographer took
     * to paint.
     *
     * <p>The first repair of that held by re-packing, which is worse
     * than empty. Packing is how the fixed point is <em>found</em>,
     * and for a layout with two answers it can find either: a
     * wrapped label's preferred width depends on the width it was
     * last laid out at. A hold that packs afresh can therefore
     * choose 326 where 333 was proved, which is the defect, arriving
     * through the repair. So what was proved is remembered and put
     * back.
     */
    public static Sizing packed() {
        return new Sizing() {

            private java.awt.Dimension windowWas;
            private java.awt.Dimension contentWas;

            @Override
            public void bring(Window window, JComponent content)
                    throws Exception {
                packToFixedPoint(window, content);
            }

            @Override
            public void proved(Window window, JComponent content) {
                windowWas = window == null ? null : window.getSize();
                contentWas = content.getSize();
            }

            @Override
            public void hold(Window window, JComponent content) {
                if (contentWas == null) {
                    // Nothing was proved, so there is nothing to put
                    // back. Refusing here would turn a capture that
                    // never settled into a confusing second failure.
                    return;
                }
                if (window != null && windowWas != null
                        && !windowWas.equals(window.getSize())) {
                    window.setSize(windowWas);
                }
                if (!contentWas.equals(content.getSize())) {
                    content.setSize(contentWas);
                }
                // Lays the children out AT that size, which is how a
                // wrapped label comes back to the width it was proved
                // at rather than to the one it drifted to.
                content.validate();
            }
        };
    }

    /**
     * A window whose size is a policy the application states.
     *
     * @param policy the application's own sizing, re-applied here
     *     rather than imitated
     */
    public static Sizing applicationSized(String name,
                                          Runnable policy) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a policy is named so"
                    + " that a refusal points at something a reader"
                    + " can open. A lambda's generated class name"
                    + " does not");
        }
        if (policy == null) {
            throw new IllegalArgumentException("an application-sized"
                    + " window is sized by the application, so there"
                    + " has to be a policy to apply");
        }
        return new Sizing() {

            @Override
            public void hold(Window window, JComponent content) {
                // Idempotent, and applied where nothing can undo it.
                policy.run();
            }

            @Override
            public void bring(Window window, JComponent content)
                    throws Exception {
                requireWindow(window);
                java.awt.Dimension was = null;
                for (int round = 0; round < ROUNDS; round++) {
                    java.awt.Dimension[] now =
                            new java.awt.Dimension[1];
                    SwingUtilities.invokeAndWait(() -> {
                        policy.run();
                        now[0] = window.getSize();
                    });
                    drain();
                    if (now[0].equals(was)) {
                        return;
                    }
                    was = now[0];
                }
                throw new IllegalStateException("this window never"
                        + " settled under its own sizing policy in "
                        + ROUNDS + " applications: "
                        + name
                        + " last brought the window to " + was
                        + ", while its content is "
                        + sizeOf(content) + " and prefers "
                        + preferredOf(content) + ". A photograph would"
                        + " be of one of the sizes it passed"
                        + " through.");
            }
        };
    }

    /*
     * There is deliberately no public "settle here, paint later".
     *
     * <p>There was, and nine photographers used it: they settled
     * through this coordinator and then painted in event blocks of
     * their own. Every one of those was a gap, and #364 is what came
     * through one - a sheet whose own settled record said 333x223
     * beside pixels that were 326x206. Two of them were painting off
     * the event thread entirely.
     *
     * <p>So the sequence is not offered in halves. A photographer
     * hands over what to draw and gets back an image; where and when
     * it is drawn is not its decision to make.
     */

    /** An application policy needs a window to apply itself to. */
    private static void requireWindow(Window window) {
        if (window == null) {
            throw new IllegalArgumentException("an application-sized"
                    + " capture has no window to size. A component"
                    + " with no window of its own is a fixed canvas,"
                    + " not a window with a policy, and declaring it"
                    + " one makes an impossible claim look valid.");
        }
    }

    private static String sizeOf(JComponent content) throws Exception {
        String[] said = new String[1];
        SwingUtilities.invokeAndWait(() -> said[0] =
                content.getWidth() + "x" + content.getHeight());
        return said[0];
    }

    private static String preferredOf(JComponent content)
            throws Exception {
        String[] said = new String[1];
        SwingUtilities.invokeAndWait(() -> said[0] =
                content.getPreferredSize().width + "x"
                        + content.getPreferredSize().height);
        return said[0];
    }

    /**
     * A component laid out at a width the study chose.
     *
     * <p>No window, and therefore no window policy: Settings
     * photographs its content at a declared width, and the
     * page-language sheets are renderer output with no component
     * hierarchy to size at all. Calling this says so, where
     * defaulting to a packed window would have said something
     * untrue.
     *
     * <p>It brings nothing, because the caller has already chosen
     * the canvas. What it does is make the choice <em>visible</em> -
     * in the source, where the compiler carries it, rather than in a
     * table that can drift.
     */
    public static Sizing fixedCanvas() {
        return (window, content) -> {
            if (window != null) {
                throw new IllegalArgumentException("a fixed canvas"
                        + " has no window. This one has "
                        + window.getClass().getSimpleName()
                        + ", so it is a window with a size, and its"
                        + " kind should say which");
            }
        };
    }

    /** Photographs a window of a stated kind. */
    public static void write(Window window, JComponent content,
                             Sizing sizing, Path to) throws Exception {
        BufferedImage drawn = of(window, content, sizing);
        ImageIO.write(drawn, "png", to.toFile());
    }

    /** The same, returning the image. */
    public static BufferedImage of(Window window, JComponent content,
                                   Sizing sizing) throws Exception {
        return take(window, content, sizing, Premise.none(),
                () -> paint(content));
    }

    /**
     * One uninterrupted capture, which is the whole point of it.
     *
     * <p>Five steps, and the reason they are one sequence rather than
     * five things a photographer does in order:
     *
     * <ol>
     *   <li>settle the declared size;</li>
     *   <li>hold it;</li>
     *   <li>verify the state the photograph claims;</li>
     *   <li>record what is about to be painted;</li>
     *   <li>paint.</li>
     * </ol>
     *
     * <p>Steps 2 to 5 run in a single event block, with nothing
     * between them. Steps 1 drives the queue and cannot.
     *
     * <p>#364 is what a split sequence costs. Nine photographers
     * settled through this coordinator and then painted in blocks of
     * their own, one or more event cycles later. Export was the one
     * that got caught: its settled record said 333x223, its pixels
     * were 326x206 - the geometry of the previous state, whose note
     * label had not yet wrapped to two lines - and the premise it
     * asserted in between agreed, because it asked about format,
     * paper and marks and never about size. Between the fixed point
     * and the paint there were event cycles, and something used them.
     *
     * <p>The repair is not to guess what. It is that there are no
     * cycles left to use.
     */
    public static BufferedImage take(Window window, JComponent content,
                                     Sizing sizing, Premise premise,
                                     Picture picture) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("capture drives the event"
                    + " queue and cannot run on it");
        }
        if (content == null) {
            throw new IllegalArgumentException(
                    "there is nothing to photograph");
        }
        if (sizing == null) {
            throw new IllegalArgumentException("a photographer says"
                    + " which kind of window this is");
        }
        if (premise == null || picture == null) {
            throw new IllegalArgumentException("a capture needs what"
                    + " it claims to show and what to draw. Premise"
                    + " .none() says a photograph claims nothing"
                    + " beyond its geometry; null says nothing at"
                    + " all");
        }
        drain();
        canonicalise(content);
        sizing.bring(window, content);
        // Settling reaches the fixed point AND records it, in the
        // same event block, so nothing can move between confirming
        // the geometry and remembering it.
        settleLayout(window, content, sizing);

        String[] wrong = new String[1];
        BufferedImage[] drawn = new BufferedImage[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            sizing.hold(window, content);
            neutralFocusNow();
            wrong[0] = premise.disagreement(content);
            if (wrong[0] != null) {
                // Traced anyway: a refusal is exactly when somebody
                // wants to know what the geometry was.
                tracePrePaint(window, content);
                return;
            }
            tracePrePaint(window, content);
            try {
                drawn[0] = picture.draw();
            } catch (Exception cannot) {
                failed[0] = cannot;
            }
        });
        if (wrong[0] != null) {
            throw new IllegalStateException("this photograph does not"
                    + " show what it says it does: " + wrong[0]);
        }
        if (failed[0] != null) {
            throw failed[0];
        }
        return drawn[0];
    }

    /** The same, writing the image where the study keeps it. */
    public static void take(Window window, JComponent content,
                            Sizing sizing, Premise premise,
                            Picture picture, Path to) throws Exception {
        ImageIO.write(take(window, content, sizing, premise, picture),
                "png", to.toFile());
    }

    /**
     * The same, for a photographer that holds only the component.
     *
     * <p>The window is whatever contains it, which for a fixed
     * canvas is nothing.
     */
    public static BufferedImage take(JComponent content, Sizing sizing,
                                     Premise premise, Picture picture)
            throws Exception {
        if (content == null) {
            throw new IllegalArgumentException(
                    "there is nothing to photograph");
        }
        return take(SwingUtilities.getWindowAncestor(content), content,
                sizing, premise, picture);
    }

    /**
     * Where a trace of every settled capture is appended, or null.
     *
     * <p>Set by {@code -Djuranometria.capture.trace=<file>}. Off by
     * default and written by nothing in production.
     *
     * <p>It exists because the two occurrences of #364 were both
     * discovered <em>after</em> the state that produced them had
     * been disposed: the gate compares artifacts once both runs are
     * finished, and by then the windows are gone. A trace written
     * while the layout is still settled is the only record that can
     * outlive it.
     */
    private static String traceFile() {
        // Read when used, not once at class-init: a contract that
        // points the trace at its own file would otherwise be
        // writing to wherever the first loader happened to look.
        return System.getProperty("juranometria.capture.trace");
    }

    /** Records one settled capture, if anybody asked for a trace. */
    static void trace(String what, String detail) {
        String file = traceFile();
        if (file == null) {
            return;
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(file),
                    what + "\t" + detail + "\n",
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ignored) {
            // A trace that cannot be written must not change what is
            // being traced.
        }
    }

    private static void settleLayout(Window window, JComponent content,
                                     Sizing sizing) throws Exception {
        String geometry = geometryOf(content);
        for (int round = 0; round < ROUNDS; round++) {
            SwingUtilities.invokeAndWait(() -> {
                if (window != null) {
                    window.validate();
                }
                content.validate();
            });
            drain();
            // Reading the geometry and remembering it must be ONE
            // block. They were two, and the gap between them was
            // enough: a run photographed 326x206 whose pre-paint
            // record said preferred=332x206, because the drift
            // arrived after the fixed point was read and before it
            // was remembered, so the "proved" geometry that hold put
            // back was already the wrong one.
            String was = geometry;
            String[] seen = new String[1];
            SwingUtilities.invokeAndWait(() -> {
                StringBuilder out = new StringBuilder();
                append(out, content);
                seen[0] = out.toString();
                if (seen[0].equals(was)) {
                    sizing.proved(window, content);
                }
            });
            String now = seen[0];
            if (now.equals(geometry)) {
                traceSettled(window, content, now, round);
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
     * What is about to be painted, recorded where nothing can move
     * it.
     *
     * <p>Distinct from the `settled` record, and both are kept. For
     * an application-sized window {@code Sizing.hold} runs between
     * them and may change the size: a retained failure whose trace
     * described only the settled geometry would describe a state
     * other than the one in the pixels, which is the exact ambiguity
     * a trace exists to remove.
     *
     * <p>Called on the event thread, with nothing between it and the
     * paint.
     */
    public static void tracePrePaint(Window window,
                                     JComponent content) {
        String file = traceFile();
        if (file == null) {
            return;
        }
        java.awt.KeyboardFocusManager manager =
                java.awt.KeyboardFocusManager
                        .getCurrentKeyboardFocusManager();
        StringBuilder geometry = new StringBuilder();
        append(geometry, content);
        trace("pre-paint", "content=" + content.getWidth() + "x"
                + content.getHeight()
                + " preferred=" + content.getPreferredSize().width
                + "x" + content.getPreferredSize().height
                + " window=" + (window == null ? "none"
                        : window.getWidth() + "x" + window.getHeight())
                + " focusOwner=" + describe(manager.getFocusOwner())
                + " geometry=" + geometry.toString().hashCode()
                + " | " + geometry);
    }

    /** What the capture was of, while it is still true. */
    private static void traceSettled(Window window, JComponent content,
                                     String geometry, int rounds)
            throws Exception {
        String file = traceFile();
        if (file == null) {
            return;
        }
        String[] state = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            java.awt.KeyboardFocusManager manager =
                    java.awt.KeyboardFocusManager
                            .getCurrentKeyboardFocusManager();
            state[0] = "rounds=" + rounds
                    + " content=" + content.getWidth() + "x"
                    + content.getHeight()
                    + " preferred=" + content.getPreferredSize().width
                    + "x" + content.getPreferredSize().height
                    + " window=" + (window == null ? "none"
                            : window.getWidth() + "x"
                                    + window.getHeight()
                                    + " insets=" + window.getInsets()
                                    + " showing=" + window.isShowing()
                                    + " focusable="
                                    + window.getFocusableWindowState())
                    + " focusOwner=" + describe(manager.getFocusOwner())
                    + " permanent="
                    + describe(manager.getPermanentFocusOwner())
                    + " geometry=" + geometry.hashCode()
                    + " | " + geometry;
        });
        trace("settled", state[0]);
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
        // An empty queue is the common case and worth waiting for,
        // but it is NOT a condition to refuse on. This threw on CI,
        // where a live X display posts work continuously and the
        // queue is essentially never observed empty - a false alarm
        // about a machine rather than a finding about a layout.
        //
        // What determinism actually rests on is measured on the
        // thing that matters: the window packs to a size that stops
        // changing, the geometry reaches a fixed point, and the
        // focus state is declared in the same block as the paint.
        // Those refuse. This just yields the thread and moves on.
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
