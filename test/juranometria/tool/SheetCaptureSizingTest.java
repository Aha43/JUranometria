package juranometria.tool;

import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How big a photograph is, and who decides (issue #364's family).
 *
 * <p>Two kinds of window, and the difference is not cosmetic. A
 * <strong>packed</strong> window's size is its layout's preference.
 * An <strong>application-sized</strong> window's size is a policy the
 * application states — Place and Time raises its packed width to a
 * reviewed 420 px floor, Chart Options states {@code ORDINARY_WIDTH}
 * through {@code sizeToScreen} — and packing such a window
 * photographs a width no reader ever meets.
 *
 * <p>Both were found the hard way. Chart Options was photographed at
 * its packed 394 px; Place and Time at 326 px, under load, in three
 * runs out of sixteen, while the committed sheet and every reader
 * had 420.
 *
 * <p>And a policy brought once is not enough: the peer pulls an
 * unshown window back to its packed size an event cycle later, so the
 * policy is held again in the block that paints. The first repair
 * omitted that and still produced 324 px three times in sixteen.
 */
class SheetCaptureSizingTest {

    /** Draws a component at whatever size it currently has. */
    private static java.awt.image.BufferedImage draw(JPanel content) {
        java.awt.image.BufferedImage drawn =
                new java.awt.image.BufferedImage(
                        Math.max(1, content.getWidth()),
                        Math.max(1, content.getHeight()),
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = drawn.createGraphics();
        try {
            content.paint(g);
        } finally {
            g.dispose();
        }
        return drawn;
    }

    /** A panel whose preference is its own, and known. */
    private static JPanel canvas(int wide, int high) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(wide, high));
        panel.setOpaque(true);
        return panel;
    }

    @Test
    void packedPaintsTheLayoutsPreference() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("packed");
                content[0] = canvas(300, 200);
                owner[0].setContentPane(content[0]);
                owner[0].setSize(900, 700);
            });
            var drawn = SheetCapture.of(owner[0], content[0],
                    SheetCapture.packed());
            assertEquals(300, drawn.getWidth(),
                    "a packed window is brought to what its layout"
                            + " prefers, whatever size it happened to"
                            + " be beforehand");
            assertEquals(200, drawn.getHeight());
        } finally {
            dispose(owner[0]);
        }
    }

    @Test
    void applicationSizedPaintsThePolicysGeometry() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new JDialog(owner[0]);
                content[0] = canvas(300, 200);
                dialog[0].setContentPane(content[0]);
                dialog[0].pack();
            });
            // The same shape as Place and Time: pack, then raise the
            // width to a floor the application states.
            Runnable policy = () -> {
                dialog[0].pack();
                dialog[0].setSize(Math.max(dialog[0].getWidth(), 420),
                        dialog[0].getHeight());
                dialog[0].invalidate();
                dialog[0].validate();
            };
            var drawn = SheetCapture.of(dialog[0], content[0],
                    SheetCapture.applicationSized("test.floorPolicy", policy));
            assertEquals(420, drawn.getWidth(),
                    "the policy decides, not the preference. Packing"
                            + " would have given 300 - a width no"
                            + " reader meets");
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * Packing an application-sized window gives the wrong picture.
     *
     * <p>The mutation, in miniature: the same window, photographed
     * both ways, disagreeing exactly as Place and Time did before
     * the repair.
     */
    @Test
    void packingAnApplicationSizedWindowProducesTheWidthNobodySees()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new JDialog(owner[0]);
                content[0] = canvas(326, 263);
                dialog[0].setContentPane(content[0]);
                dialog[0].pack();
            });
            Runnable policy = () -> {
                dialog[0].pack();
                dialog[0].setSize(Math.max(dialog[0].getWidth(), 420),
                        dialog[0].getHeight());
                dialog[0].invalidate();
                dialog[0].validate();
            };
            int byPolicy = SheetCapture.of(dialog[0], content[0],
                    SheetCapture.applicationSized("test.floorPolicy", policy)).getWidth();
            int byPacking = SheetCapture.of(dialog[0], content[0],
                    SheetCapture.packed()).getWidth();

            assertEquals(420, byPolicy, "the reader's width");
            assertEquals(326, byPacking,
                    "and the one packing gives, which is the defect"
                            + " this distinction exists to prevent");
            assertTrue(byPolicy != byPacking,
                    "the two kinds must be able to disagree, or"
                            + " declaring the kind would be"
                            + " decoration");
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * A policy that never settles refuses, and says enough to act on.
     *
     * <p>The refusal used to say only that it had not settled in N
     * applications, which tells the next reader nothing about what
     * to look at.
     */
    @Test
    void aPolicyThatNeverSettlesRefusesAndNamesTheGeometry()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new JDialog(owner[0]);
                content[0] = canvas(300, 200);
                dialog[0].setContentPane(content[0]);
                dialog[0].pack();
            });
            int[] grows = {400};
            Runnable never = () -> dialog[0].setSize(grows[0]++,
                    dialog[0].getHeight());

            IllegalStateException refused = assertThrows(
                    IllegalStateException.class,
                    () -> SheetCapture.of(dialog[0], content[0],
                            SheetCapture.applicationSized("test.neverSettles", never)));
            String said = refused.getMessage();
            assertTrue(said.contains("never") && said.contains("settled"),
                    said);
            assertTrue(said.contains("prefers") && said.contains("300x200"),
                    "the refusal names what the content prefers: "
                            + said);
            assertTrue(said.contains("brought the window to"),
                    "and the size the policy last produced: " + said);
            assertTrue(said.contains("test.neverSettles"),
                    "and NAMES the policy - a lambda's generated"
                            + " class name points nowhere a reader"
                            + " can open: " + said);
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /** An application policy with no window is an impossible claim. */
    @Test
    void anApplicationPolicyWithoutAWindowIsRefused() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a component still has to exist");
        JPanel[] content = new JPanel[1];
        SwingUtilities.invokeAndWait(() -> content[0] = canvas(300, 200));
        assertThrows(IllegalArgumentException.class,
                () -> SheetCapture.of(null, content[0],
                        SheetCapture.applicationSized("test.nothing", () -> { })),
                "a component with no window is a fixed canvas, not a"
                        + " window with a policy. Succeeding here"
                        + " would make an impossible declaration look"
                        + " valid");
    }

    /**
     * The pre-paint record describes what was painted.
     *
     * <p>The mutation the ruling asked for: a policy that changes
     * the width in {@code hold}, after the settled record is
     * written. The pre-paint record must carry the changed width, or
     * a retained failure would keep correct pixels beside a trace of
     * some other state.
     */
    @Test
    void thePrePaintRecordCarriesWhatHoldChanged() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        Path trace = Files.createTempFile("capture-trace", ".tsv");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new JDialog(owner[0]);
                content[0] = canvas(300, 200);
                dialog[0].setContentPane(content[0]);
                dialog[0].pack();
            });
            SheetCapture.Sizing widening = new SheetCapture.Sizing() {
                @Override
                public void bring(java.awt.Window window,
                                  javax.swing.JComponent panel) {
                    dialog[0].pack();
                }

                @Override
                public void hold(java.awt.Window window,
                                 javax.swing.JComponent panel) {
                    // Changes the width AFTER the settled record.
                    dialog[0].setSize(500, dialog[0].getHeight());
                    dialog[0].invalidate();
                    dialog[0].validate();
                }
            };
            int painted = traced(trace, () ->
                    SheetCapture.of(dialog[0], content[0], widening)
                            .getWidth());

            assertEquals(500, painted,
                    "hold decides the pixels");
            List<String> lines = Files.readAllLines(trace,
                    StandardCharsets.UTF_8);
            String prePaint = lines.stream()
                    .filter(one -> one.startsWith("pre-paint"))
                    .reduce((a, b) -> b).orElse("");
            assertTrue(prePaint.contains("content=500x"),
                    "and the pre-paint record carries that width, not"
                            + " the settled one: " + prePaint);
            String settled = lines.stream()
                    .filter(one -> one.startsWith("settled"))
                    .reduce((a, b) -> b).orElse("");
            assertTrue(settled.contains("content=300x"),
                    "while the settled record still says what it saw,"
                            + " so the two can be told apart: "
                            + settled);
        } finally {
            dispose(dialog[0], owner[0]);
            Files.deleteIfExists(trace);
        }
    }

    /**
     * A layout with two answers, which is what #364 was made of.
     *
     * <p>The export dialog's note label wraps to two lines for one
     * paper size and one for another, and a wrapped label's
     * preferred width depends on the width it was last laid out at.
     * So the same dialog has two stable geometries - 333x223 and
     * 326x206, the exact pair the recurrence disagreed over - and
     * which one a fresh pack finds depends on where it starts.
     *
     * <p>A flag stands in for the wrap, so the bistability is
     * deterministic instead of once in sixteen runs under load.
     */
    private static final class Bistable extends JPanel {

        private boolean narrow;

        @Override
        public Dimension getPreferredSize() {
            return narrow ? new Dimension(326, 206)
                    : new Dimension(333, 223);
        }
    }

    /** Wraps a sizing so a change can be posted at a chosen moment. */
    private static SheetCapture.Sizing changing(
            SheetCapture.Sizing inner, Runnable atProved) {
        return new SheetCapture.Sizing() {
            @Override
            public void bring(java.awt.Window window,
                              javax.swing.JComponent panel)
                    throws Exception {
                inner.bring(window, panel);
            }

            @Override
            public void proved(java.awt.Window window,
                               javax.swing.JComponent panel) {
                inner.proved(window, panel);
                // Posted once the geometry is proved and before the
                // block that paints: pending BETWEEN settling and
                // painting, which is where #364 lived.
                SwingUtilities.invokeLater(atProved);
            }

            @Override
            public void hold(java.awt.Window window,
                             javax.swing.JComponent panel) {
                inner.hold(window, panel);
            }
        };
    }

    /**
     * A change between settling and painting does not reach the
     * picture.
     *
     * <p>The deterministic model of the recurrence. The layout is
     * proved at 333x223; a re-layout to the other answer becomes
     * pending before the paint block runs; the picture must still be
     * the proved one.
     *
     * <p>This fails if {@code hold} is empty - which it was, for
     * eight of twelve photographers - and it fails just as surely if
     * {@code hold} re-packs, because packing is how a fixed point is
     * <em>found</em> and a bistable layout has two to find. Putting
     * back what was proved is the only hold that passes.
     */
    @Test
    void aChangeBetweenSettlingAndPaintingDoesNotReachThePicture()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        Bistable[] content = new Bistable[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new JDialog(owner[0]);
                content[0] = new Bistable();
                content[0].setOpaque(true);
                dialog[0].setContentPane(content[0]);
                dialog[0].pack();
            });
            SheetCapture.Sizing packed = SheetCapture.packed();
            SheetCapture.Sizing drifting = changing(packed, () -> {
                content[0].narrow = true;
                // A re-layout invalidates up the tree, which is what
                // makes the new answer reachable at all: a valid
                // container returns its CACHED preferred size, so a
                // pack alone would quietly keep the old one.
                content[0].invalidate();
                dialog[0].invalidate();
                dialog[0].pack();
            });

            var drawn = SheetCapture.of(dialog[0], content[0], drifting);

            assertEquals(333, drawn.getWidth(),
                    "the picture is of the geometry that was proved."
                            + " 326 here is the other stable answer -"
                            + " the one the recurrence painted while"
                            + " its own settled record said 333");
            assertEquals(223, drawn.getHeight());
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * Nothing runs between verifying the state and painting it.
     *
     * <p>The other half of the sequence. Here the change becomes
     * pending from inside the block, after the premise has agreed:
     * if the paint is in that same block it cannot land, and if the
     * paint is moved to a block of its own it lands in between - a
     * dialog that agreed about its format and was then painted at
     * another state's geometry, which is what the retained pair
     * showed.
     */
    @Test
    void nothingRunsBetweenVerifyingTheStateAndPaintingIt()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        Bistable[] content = new Bistable[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new JDialog(owner[0]);
                content[0] = new Bistable();
                content[0].setOpaque(true);
                dialog[0].setContentPane(content[0]);
                dialog[0].pack();
            });

            var drawn = SheetCapture.take(dialog[0], content[0],
                    SheetCapture.packed(),
                    held -> {
                        SwingUtilities.invokeLater(() -> {
                            content[0].narrow = true;
                            content[0].invalidate();
                            dialog[0].invalidate();
                            dialog[0].pack();
                        });
                        return null;
                    },
                    () -> draw(content[0]));

            assertEquals(333, drawn.getWidth(),
                    "the premise agreed and the paint followed it in"
                            + " the same block, so the change queued"
                            + " between them had nowhere to run");
            assertEquals(223, drawn.getHeight());
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * The premise is asked inside the same block, and refuses there.
     *
     * <p>Export asserted its premise in an event cycle of its own,
     * between settling and painting. It agreed, and the sheet was
     * wrong anyway. A premise is only a statement about the moment
     * it is asked in.
     */
    @Test
    void aPremiseThatDisagreesRefusesAndNothingIsDrawn()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JPanel[] content = new JPanel[1];
        boolean[] drew = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("premise");
                content[0] = canvas(300, 200);
                owner[0].setContentPane(content[0]);
            });
            IllegalStateException refused = assertThrows(
                    IllegalStateException.class,
                    () -> SheetCapture.take(owner[0], content[0],
                            SheetCapture.packed(),
                            held -> "the format is letter, not a4",
                            () -> {
                                drew[0] = true;
                                return new java.awt.image.BufferedImage(
                                        1, 1, java.awt.image.BufferedImage
                                                .TYPE_INT_RGB);
                            }));
            assertTrue(refused.getMessage().contains("letter"),
                    "the refusal carries what disagreed: "
                            + refused.getMessage());
            assertFalse(drew[0],
                    "and nothing is drawn. A missing sheet is a"
                            + " question; a mislabelled one is an"
                            + " answer nobody checks");
        } finally {
            dispose(owner[0]);
        }
    }

    /**
     * The premise sees the size that will be painted, not the
     * settled one.
     *
     * <p>It is asked after {@code hold}, so an application-sized
     * window's premise can reason about the geometry the reader
     * actually gets.
     */
    @Test
    void thePremiseSeesTheHeldGeometry() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        int[] sawWidth = {-1};
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new JDialog(owner[0]);
                content[0] = canvas(300, 200);
                dialog[0].setContentPane(content[0]);
                dialog[0].pack();
            });
            SheetCapture.Sizing widening = new SheetCapture.Sizing() {
                @Override
                public void bring(java.awt.Window window,
                                  javax.swing.JComponent panel) {
                    dialog[0].pack();
                }

                @Override
                public void hold(java.awt.Window window,
                                 javax.swing.JComponent panel) {
                    content[0].setSize(480, content[0].getHeight());
                    content[0].validate();
                }
            };
            SheetCapture.take(dialog[0], content[0], widening,
                    held -> {
                        sawWidth[0] = held.getWidth();
                        return null;
                    },
                    () -> new java.awt.image.BufferedImage(1, 1,
                            java.awt.image.BufferedImage.TYPE_INT_RGB));
            assertEquals(480, sawWidth[0],
                    "the premise is asked after hold, so it sees the"
                            + " geometry the picture will have rather"
                            + " than the one it was settled at");
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /** Runs a body with the capture trace pointed at a file. */
    private static int traced(Path trace,
                              java.util.concurrent.Callable<Integer> body)
            throws Exception {
        String was = System.getProperty("juranometria.capture.trace");
        System.setProperty("juranometria.capture.trace",
                trace.toString());
        try {
            return body.call();
        } finally {
            if (was == null) {
                System.clearProperty("juranometria.capture.trace");
            } else {
                System.setProperty("juranometria.capture.trace", was);
            }
        }
    }

    private static void dispose(java.awt.Window... windows)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (java.awt.Window one : windows) {
                if (one != null) {
                    one.dispose();
                }
            }
        });
    }
}
