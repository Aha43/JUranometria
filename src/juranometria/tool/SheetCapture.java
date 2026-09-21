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

        /**
         * Discovers the geometry. May pack.
         *
         * <p>Packing is how a size is <em>found</em>, and finding is
         * allowed to be expensive and allowed to be repeated until
         * it stops moving. What it may not be is repeated later, in
         * the block that paints, because a layout with more than one
         * stable answer can be found at a different one.
         */
        void establish(Window window, JComponent content)
                throws Exception;

        /**
         * The second stage of establishing: states the size at the
         * fixed point, without packing.
         *
         * <p>Called on the event thread in the same block that
         * confirms the fixed point, <strong>immediately before the
         * geometry is proved</strong> - so what gets proved is the
         * policy's answer rather than whatever the layout drifted to
         * while it settled. Establishing converged on 420 and the
         * fixed point found 326, and 326 was what got written down,
         * until this stage existed.
         *
         * <p>It must not pack. Packing is stage one's business,
         * because packing is how a size is <em>discovered</em> and a
         * layout with two stable answers can be discovered at
         * either. Stage two only says the size that stage one
         * arrived at.
         *
         * <p>This does <strong>not</strong> run in the block that
         * paints. Nothing does: the paint block restores the proved
         * snapshot and draws it, and an earlier version of this
         * method which ran there was measurably destructive - it
         * addressed the unshown native window and its validation
         * pulled the content back to 324 px, undoing a restoration
         * that had already succeeded.
         */
        default void restate(Window window, JComponent content) {
        }
    }

    /**
     * What a photograph claims to be a picture of.
     *
     * <p>Checked <strong>inside</strong> the block that restores
     * the geometry and paints, because a premise checked anywhere else is a
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
     * <p>One stage only: packing to a fixed point <em>is</em> the
     * establishment. Nothing is restated afterwards, and the
     * geometry proved at that fixed point is put back by the
     * coordinator in the block that paints.
     *
     * <p>Where that restoration lives was learned in four steps. It
     * did not exist, which made holding a size vacuous for eight of
     * twelve photographers. Restoring by re-packing was worse than
     * nothing: packing is how a fixed point is <em>found</em>, and a
     * layout with two answers can be found at either, so a fresh
     * pack could choose 326 where 333 had been proved. Keeping the
     * restoration in the packed policy alone left the
     * application-sized captures without it. And calling any policy
     * in the paint block, before or after the restoration, was
     * destructive in both orders.
     */
    public static Sizing packed() {
        return SheetCapture::packToFixedPoint;
    }

    /**
     * The three things converging on an application's size needs.
     *
     * <p>Named separately from the window so the loop below can be
     * driven without one. Two contracts used to reach that loop
     * through a real unshown dialog, inventing sizes and relying on
     * the window to keep them; under a virtual display it does not
     * always, and both passed on one machine and failed on another
     * from identical inputs - which is the defect they exist to
     * catch, committed in the tests themselves.
     */
    interface Settling {

        /** Applies the application's own sizing policy once. */
        void apply() throws Exception;

        /** Lets everything the policy queued actually run. */
        void letTheQueueRun() throws Exception;

        /**
         * The size now.
         *
         * <p>Called after the queue has run, never inside the block
         * that applied the policy: a size read there can be a 420
         * the peer has already answered with 324, and recording
         * that transient is how a capture came to hold a geometry
         * the window no longer had.
         */
        java.awt.Dimension observe() throws Exception;

        /** What else a refusal should say. */
        default String describe() throws Exception {
            return "nothing further is known about it";
        }
    }

    /**
     * Applies a policy until the size it produces stops changing.
     *
     * <p>Apply, let the queue run, observe - in that order, every
     * round. A policy whose answer survives its own queue twice
     * running has settled; one that does not has no answer to
     * photograph, and this refuses rather than choosing one of the
     * sizes it passed through.
     *
     * @return the size it settled on
     */
    static java.awt.Dimension converge(String name, Settling settling)
            throws Exception {
        java.awt.Dimension was = null;
        for (int round = 0; round < ROUNDS; round++) {
            settling.apply();
            settling.letTheQueueRun();
            java.awt.Dimension now = settling.observe();
            if (now.equals(was)) {
                return now;
            }
            was = now;
        }
        throw new IllegalStateException("this window never settled"
                + " under its own sizing policy in " + ROUNDS
                + " applications: " + name
                + " last brought the window to " + was
                + ", while " + settling.describe()
                + ". A photograph would be of one of the sizes it"
                + " passed through.");
    }

    /**
     * A window whose size is a policy the application states.
     *
     * <p>Two operations, because discovering a size and re-stating
     * one are different acts. Establishing may pack - that is how
     * the height is found. Re-stating may not, because packing a
     * layout with more than one stable answer can return a
     * different one, and on an unshown window the peer can answer a
     * pack before the floor is applied.
     *
     * @param name the policy's name, so a refusal points somewhere
     *     a reader can open
     * @param establish the application's own sizing, applied until
     *     it stops changing
     * @param restate stage two: the same size stated at the fixed
     *     point, without packing
     */
    public static Sizing applicationSized(String name,
                                          Runnable establish,
                                          Runnable restate) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a policy is named so"
                    + " that a refusal points at something a reader"
                    + " can open. A lambda's generated class name"
                    + " does not");
        }
        if (establish == null || restate == null) {
            throw new IllegalArgumentException("an application-sized"
                    + " window is sized by the application, so there"
                    + " has to be a policy to apply and a way to say"
                    + " it again without packing");
        }
        return new Sizing() {

            @Override
            public void restate(Window window, JComponent content) {
                restate.run();
            }

            @Override
            public void establish(Window window, JComponent content)
                    throws Exception {
                requireWindow(window);
                converge(name, new Settling() {

                    @Override
                    public void apply() throws Exception {
                        SwingUtilities.invokeAndWait(establish);
                    }

                    @Override
                    public void letTheQueueRun() throws Exception {
                        drain();
                    }

                    @Override
                    public java.awt.Dimension observe() throws Exception {
                        java.awt.Dimension[] now =
                                new java.awt.Dimension[1];
                        SwingUtilities.invokeAndWait(() ->
                                now[0] = window.getSize());
                        return now[0];
                    }

                    @Override
                    public String describe() throws Exception {
                        return "its content is " + sizeOf(content)
                                + " and prefers "
                                + preferredOf(content);
                    }
                });
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
        return fixedCanvas(() -> { });
    }

    /**
     * A canvas the study sizes itself, with the operation that does
     * it.
     *
     * <p>A fixed canvas means <strong>the study owns the canvas
     * geometry</strong> - not that the geometry may first be chosen
     * while drawing. The settings study laid itself out inside its
     * picture, moving from 0x0 to 560x309 as it drew, and under the
     * movement contract that is drawing establishing state. It was
     * legitimate in the old model and is in the wrong phase in this
     * one.
     *
     * <p>So the canvas is established here, before anything is
     * proved, and the picture allocates and paints from a geometry
     * that was already settled on.
     *
     * @param establish sizes the component and lays it out, on the
     *     event thread
     */
    public static Sizing fixedCanvas(Runnable establish) {
        if (establish == null) {
            throw new IllegalArgumentException("a fixed canvas is"
                    + " sized by the study, so there has to be an"
                    + " operation that sizes it - or the no-argument"
                    + " form, for a canvas already established");
        }
        return (window, content) -> {
            if (window != null) {
                throw new IllegalArgumentException("a fixed canvas"
                        + " has no window. This one has "
                        + window.getClass().getSimpleName()
                        + ", so it is a window with a size, and its"
                        + " kind should say which");
            }
            SwingUtilities.invokeAndWait(establish);
            drain();
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
     *   <li>restore it;</li>
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
        holding = true;
        try {
            return photograph(window, content, sizing, premise,
                    picture);
        } finally {
            // Written once the picture is taken, never between the
            // steps it describes.
            flushCheckpoints();
        }
    }

    private static BufferedImage photograph(Window window,
                                            JComponent content,
                                            Sizing sizing,
                                            Premise premise,
                                            Picture picture)
            throws Exception {
        drain();
        canonicalise(content);
        sizing.establish(window, content);
        // Settling reaches the fixed point AND records it, in the
        // same event block, so nothing can move between confirming
        // the geometry and remembering it. The COORDINATOR records
        // it, not the sizing: every kind of capture has a geometry
        // that was proved, so every kind gets it put back, and a
        // policy cannot forget to.
        java.awt.Dimension[] proved = new java.awt.Dimension[2];
        settleLayout(window, content, sizing, proved);

        String[] wrong = new String[1];
        BufferedImage[] drawn = new BufferedImage[1];
        Exception[] failed = new Exception[1];
        String[] moved = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            // FOCUS FIRST, before the geometry is settled on.
            //
            // It used to run between the policy and the paint, and
            // it was the only active operation in that gap - the gap
            // where a dialog whose policy had just stated 420 was
            // photographed at 326, with no drift recorded because
            // the geometry was still right when the restoration
            // looked. Clearing the focus owner reaches the peer, and
            // a peer asked about an unshown window can answer by
            // resizing it. So focus is declared first and the
            // geometry is established after it, leaving nothing
            // between the geometry and the paint.
            neutralFocusNow();

            // The proved snapshot is put back, for every kind of
            // capture, and that is the whole of it. No sizing
            // operation runs here.
            //
            // Both halves of that are measured. Calling the
            // application's policy after the restoration undid it:
            // the content pane was already back at the proved 420,
            // and addressing the unshown window pulled it to 324.
            // And calling the policy BEFORE the restoration was
            // worse still - Place and Time disagreed with its
            // committed bytes in 23 of 32 runs, because the geometry
            // recorded at the fixed point was sometimes the packed
            // width the peer had pulled it back to. Both problems
            // belong to establishment, and that is where they are
            // now solved.
            // The proved snapshot is put back, and NOTHING else
            // touches the geometry after it.
            //
            // A policy call used to follow this, and it was the last
            // thing left doing harm: the coordinator had already
            // restored the content pane to the proved 420, and
            // re-stating the size through the window pulled it back
            // to 324, because on an unshown window the peer answers
            // setSize with the packed width. The correction was
            // succeeding and the policy was undoing it. So the
            // policy establishes the geometry BEFORE the proof, and
            // the block that paints only puts the snapshot back.
            restoreProved(window, content, proved);

            // Checked, not assumed: if the geometry is not what was
            // proved even after restoring it, nothing may be
            // painted.
            moved[0] = whatMoved("restoring its geometry", content,
                    proved[1]);
            if (moved[0] != null) {
                tracePrePaint(window, content);
                return;
            }

            // From here the geometry is the answer, and anything
            // that changes it invalidates the photograph rather than
            // altering it.
            java.awt.Dimension settledOn = content.getSize();
            wrong[0] = premise.disagreement(content);
            if (wrong[0] != null) {
                // Recorded anyway: a refusal is exactly when
                // somebody wants to know what the geometry was.
                tracePrePaint(window, content);
                return;
            }
            moved[0] = whatMoved("asking its premise", content,
                    settledOn);
            if (moved[0] != null) {
                tracePrePaint(window, content);
                return;
            }
            tracePrePaint(window, content);
            try {
                drawn[0] = picture.draw();
            } catch (Exception cannot) {
                failed[0] = cannot;
                return;
            }
            moved[0] = whatMoved("drawing", content, settledOn);
        });
        // Cleared once the capture is over, so the next one cannot
        // inherit this one's name. A stale subject is worse than
        // none: it labels a line with the wrong sheet, and a
        // diagnosis that trusts it reads the wrong window.
        tracing(null);
        if (wrong[0] != null) {
            throw new IllegalStateException("this photograph does not"
                    + " show what it says it does: " + wrong[0]);
        }
        if (moved[0] != null) {
            throw new IllegalStateException(moved[0]);
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
    /**
     * What the next trace lines are about, in the generator's words.
     *
     * <p>A trace used to be read by counting lines: the fourth
     * `settled` was the fourth sheet, and a reader had to know the
     * generator's order to say which. That is exactly the kind of
     * knowledge a diagnostic should not require - the one occurrence
     * anybody needed to read was mapped to its sheet by hand, from
     * the order the sheets happen to be written in.
     *
     * <p>Set by the generator before each capture; every line
     * afterwards carries it. Off by default, like the rest of the
     * trace, and it changes nothing that is written to a study.
     */
    private static volatile String subject = "";

    /** Names what is about to be captured, for the trace. */
    public static void tracing(String what) {
        subject = what == null ? "" : what;
    }

    /**
     * Checkpoints held in memory until the picture is taken.
     *
     * <p>Writing a trace line is file I/O, and doing it on the event
     * thread between the steps being traced changes their timing.
     * That is not a theory: a Place and Time disagreement reproduced
     * 2 times in 32 with six checkpoints, and 0 in 32 with eight -
     * the instrument moved what it was measuring, and the extra
     * writes hid the race rather than explaining it.
     *
     * <p>So checkpoints are appended to a list while a capture runs
     * and written once it is over. The record is identical; the
     * timing of the thing recorded is left alone.
     */
    private static final java.util.List<String> CHECKPOINTS =
            new java.util.ArrayList<>();

    /** Whether checkpoints are being held rather than written. */
    private static volatile boolean holding;

    /** Writes everything held, and stops holding. */
    private static void flushCheckpoints() {
        holding = false;
        java.util.List<String> said;
        synchronized (CHECKPOINTS) {
            if (CHECKPOINTS.isEmpty()) {
                return;
            }
            said = new java.util.ArrayList<>(CHECKPOINTS);
            CHECKPOINTS.clear();
        }
        String file = traceFile();
        if (file == null) {
            return;
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(file),
                    String.join("", said),
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ignored) {
            // A trace that cannot be written must not change what is
            // being traced.
        }
    }

    static void trace(String what, String detail) {
        String file = traceFile();
        if (file == null) {
            return;
        }
        if (holding) {
            synchronized (CHECKPOINTS) {
                CHECKPOINTS.add(what + "\t"
                        + (subject.isEmpty() ? ""
                                : "subject=" + subject + " ")
                        + detail + "\n");
            }
            return;
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(file),
                    what + "\t" + (subject.isEmpty() ? ""
                            : "subject=" + subject + " ") + detail + "\n",
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ignored) {
            // A trace that cannot be written must not change what is
            // being traced.
        }
    }

    /**
     * Whether the geometry moved under a step that must not move it.
     *
     * <p>Asking a premise and drawing are both reads. If either
     * changes the size, the pixels are of one state and the record
     * of another - and a sheet that disagrees with its own trace is
     * the thing this whole issue is about. So it refuses rather than
     * writes, and names the step, which is more than any of #364's
     * occurrences said for themselves.
     */
    private static String whatMoved(String step, JComponent content,
                                    java.awt.Dimension settledOn) {
        java.awt.Dimension now = content.getSize();
        if (now.equals(settledOn)) {
            return null;
        }
        return "the layout moved while " + step + ": it was "
                + settledOn.width + "x" + settledOn.height
                + " and is " + now.width + "x" + now.height
                + ". A photograph taken now would disagree with the"
                + " record of what it is a photograph of.";
    }

    /**
     * Puts back the geometry the fixed point proved.
     *
     * <p>Runs on the event thread, before anything is recorded or
     * painted, for <strong>every</strong> kind of capture. No sizing
     * operation runs beside it. Which is the point: a packed window had this
     * and an application-sized one did not, and it was an
     * application-sized one - Place and Time - that was next
     * photographed at 326 px where its policy states 420 and its own
     * settled record said 420.
     *
     * <p>Restoring is not the same as re-deriving. Packing afresh
     * asks a bistable layout a question that has two answers;
     * reapplying a policy asks the application a question whose
     * answer depends on the size it is asked at. The geometry that
     * survived {@code packToFixedPoint} and {@code settleLayout} is
     * the answer, and it is put back rather than sought again.
     *
     * <p>A restoration that had to change something is traced,
     * because that is the drift itself - the moment a retained pair
     * exists to explain.
     */
    private static void restoreProved(Window window, JComponent content,
                                      java.awt.Dimension[] proved) {
        java.awt.Dimension contentWas = proved[1];
        if (contentWas == null) {
            // Nothing was proved, so there is nothing to put back.
            return;
        }
        java.awt.Dimension windowWas = proved[0];
        boolean moved = false;
        if (window != null && windowWas != null
                && !windowWas.equals(window.getSize())) {
            trace("drift", "window was " + windowWas.width + "x"
                    + windowWas.height + " and is "
                    + window.getWidth() + "x" + window.getHeight());
            window.setSize(windowWas);
            moved = true;
        }
        if (!contentWas.equals(content.getSize())) {
            trace("drift", "content was " + contentWas.width + "x"
                    + contentWas.height + " and is "
                    + content.getWidth() + "x" + content.getHeight());
            content.setSize(contentWas);
            moved = true;
        }
        if (moved) {
            // Laid out AT the restored size, which is how a wrapped
            // label comes back to the width it was proved at.
            content.validate();
            trace("restored", "content=" + content.getWidth() + "x"
                    + content.getHeight()
                    + " window=" + (window == null ? "none"
                            : window.getWidth() + "x"
                                    + window.getHeight()));
        }
    }

    private static void settleLayout(Window window, JComponent content,
                                     Sizing sizing,
                                     java.awt.Dimension[] proved)
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
            // Reading the geometry and remembering it must be ONE
            // block. They were two, and the gap between them was
            // enough: a run photographed 326x206 whose pre-paint
            // record said preferred=332x206, because the drift
            // arrived after the fixed point was read and before it
            // was remembered, so the "proved" geometry the paint
            // block put back was already the wrong one.
            String was = geometry;
            int at = round;
            String[] seen = new String[1];
            SwingUtilities.invokeAndWait(() -> {
                StringBuilder out = new StringBuilder();
                append(out, content);
                seen[0] = out.toString();
                if (seen[0].equals(was)) {
                    // The policy states its size once more before
                    // anything is recorded, without packing. For a
                    // packed or fixed-canvas capture this does
                    // nothing; for an application-sized one it is
                    // the difference between recording the geometry
                    // the application asks for and recording the
                    // one the peer drifted to while the layout was
                    // settling. Establish converged at 420 and this
                    // fixed point found 326, and 326 was what got
                    // written down.
                    java.awt.Dimension beforeStageTwo =
                            content.getSize();
                    sizing.restate(window, content);
                    if (!beforeStageTwo.equals(content.getSize())) {
                        // Stage two moved the layout, which is the
                        // whole reason it exists: what gets proved
                        // below is the policy's answer rather than
                        // whatever settling drifted to.
                        trace("restated", "content was "
                                + beforeStageTwo.width + "x"
                                + beforeStageTwo.height + " and is "
                                + content.getWidth() + "x"
                                + content.getHeight());
                    }
                    proved[0] = window == null ? null : window.getSize();
                    proved[1] = content.getSize();
                    trace("proved", "round=" + at
                            + " content=" + content.getWidth() + "x"
                            + content.getHeight()
                            + " preferred="
                            + content.getPreferredSize().width + "x"
                            + content.getPreferredSize().height
                            + " window=" + (window == null ? "none"
                                    : window.getWidth() + "x"
                                            + window.getHeight()));
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
        traceGeometry("before-canonicalise", content);
        SwingUtilities.invokeAndWait(() -> rebuildHtml(content));
        drain();
        traceGeometry("after-canonicalise", content);
    }

    /**
     * One component's size and preference, at a named stage.
     *
     * <p>Rebuilding a wrapping label's view is where this layout's
     * two answers are decided, so the geometry either side of it is
     * the first thing a diagnosis wants and the first thing the
     * trace did not have.
     */
    private static void traceGeometry(String stage, JComponent content) {
        if (traceFile() == null) {
            return;
        }
        try {
            String[] said = new String[1];
            SwingUtilities.invokeAndWait(() -> said[0] =
                    "content=" + content.getWidth() + "x"
                            + content.getHeight()
                            + " preferred="
                            + content.getPreferredSize().width + "x"
                            + content.getPreferredSize().height);
            trace(stage, said[0]);
        } catch (Exception ignored) {
            // A trace that cannot be written must not change what is
            // being traced.
        }
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
            trace("pack-round", "round=" + round
                    + " window=" + now[0].width + "x" + now[0].height
                    + " wasWindow=" + (was == null ? "none"
                            : was.width + "x" + was.height)
                    + " contentIsItsPreference=" + wanted[0]);
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
     * <p>Distinct from the `settled` record, and both are kept. The
     * proved snapshot is restored between them, and a restoration
     * that had work to do says so - a retained failure whose trace
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
