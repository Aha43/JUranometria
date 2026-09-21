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

    /**
     * A layout with two answers, which is what #364 was made of.
     *
     * <p>The export dialog's note label wraps to two lines for one
     * paper size and one for another, and a wrapped label's
     * preferred width depends on the width it was last laid out at.
     * So the same dialog has two stable geometries - 333x223 and
     * 326x206, the exact pair the recurrence disagreed over.
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
                    SheetCapture.applicationSized("test.floorPolicy", policy, policy));
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
                    SheetCapture.applicationSized("test.floorPolicy", policy, policy)).getWidth();
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
                            SheetCapture.applicationSized("test.neverSettles", never, never)));
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
                        SheetCapture.applicationSized("test.nothing", () -> { }, () -> { })),
                "a component with no window is a fixed canvas, not a"
                        + " window with a policy. Succeeding here"
                        + " would make an impossible declaration look"
                        + " valid");
    }

    /**
     * No policy operation runs between restoring and painting.
     *
     * <p>The last thing that was still doing harm. The coordinator
     * had already restored the content pane to the proved 420, and
     * a policy call after it pulled the pane back to 324 - because
     * on an unshown window the peer answers {@code setSize} with the
     * packed width, and validating the window carries that answer
     * down into the content. The correction was succeeding and the
     * policy was undoing it.
     *
     * <p>So the policy establishes the geometry before the proof is
     * taken, and the block that paints only puts the snapshot back.
     * This counts: the sizing must be asked to state its size
     * exactly once, and not again where nothing may move.
     */
    @Test
    void noPolicyOperationRunsBetweenRestoringAndPainting()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        int[] stated = {0};
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new JDialog(owner[0]);
                content[0] = canvas(326, 263);
                dialog[0].setContentPane(content[0]);
                dialog[0].pack();
            });
            Runnable floorOnly = () -> {
                stated[0]++;
                dialog[0].setSize(Math.max(dialog[0].getWidth(), 420),
                        dialog[0].getHeight());
                dialog[0].invalidate();
                dialog[0].validate();
            };
            Runnable establish = () -> {
                dialog[0].pack();
                floorOnly.run();
            };

            var drawn = SheetCapture.of(dialog[0], content[0],
                    SheetCapture.applicationSized("test.counted",
                            establish, floorOnly));

            int duringEstablish = stated[0];
            assertTrue(duringEstablish > 0,
                    "the premise: the policy did state its size while"
                            + " the geometry was being established");
            assertEquals(420, drawn.getWidth(),
                    "and the picture is the established width");

            // The count is what matters: every statement of the size
            // belongs to establishing it, and none to the block that
            // paints. A policy call reintroduced there would raise
            // this above what establishing needed.
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(duringEstablish, stated[0],
                    "and nothing asked the policy again after the"
                            + " geometry was proved");
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * An application-sized window is put back to its own geometry.
     *
     * <p>Place and Time's shape, and the recurrence's numbers. The
     * dialog's layout prefers <strong>326</strong>; its policy
     * raises that to a reviewed <strong>420</strong> floor, which is
     * what a reader meets and what is committed. The capture settles
     * at 420. Then the geometry drifts back to the packed 326 before
     * the paint - which is what the peer does to an unshown window
     * an event cycle later, and what the retained pair showed.
     *
     * <p>Restoring only packed captures was not enough: that is
     * exactly the gap this fell through, because Place and Time is
     * not packed. The coordinator restores after every sizing's
     * hold, and this is the contract that says so.
     */
    @Test
    void anApplicationSizedWindowIsPutBackToItsPolicysGeometry()
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
            // The dialog's own policy, as PlaceAndTimeDialog states
            // it: pack, then raise the width to the reviewed floor.
            // Place and Time's shape exactly: establishing packs
            // and then raises to the floor; re-stating raises to the
            // floor and does not pack.
            Runnable floorOnly = () -> {
                dialog[0].setSize(Math.max(dialog[0].getWidth(), 420),
                        dialog[0].getHeight());
                dialog[0].invalidate();
                dialog[0].validate();
            };
            Runnable policy = () -> {
                dialog[0].pack();
                floorOnly.run();
            };
            var drawn = SheetCapture.of(dialog[0], content[0],
                    SheetCapture.applicationSized(
                            "test.reviewedFloor", policy, floorOnly));

            assertEquals(420, drawn.getWidth(),
                    "the picture is the width a reader meets. 326 is"
                            + " the packed width the peer pulls an"
                            + " unshown dialog back to, and it is"
                            + " what the retained pair was"
                            + " photographed at");
            assertEquals(263, drawn.getHeight());
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * A premise that disagrees refuses, and nothing is drawn.
     *
     * <p>Export asserted its premise in an event cycle of its own,
     * between settling and painting. It agreed, and the sheet was
     * wrong anyway. A premise is only a statement about the moment
     * it is asked in, so it is asked in the block that paints.
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
                                return draw(content[0]);
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
     * A premise sees the geometry that will be painted.
     *
     * <p>It is asked after the proved snapshot is restored and
     * before anything is drawn, so what it reasons about is what the
     * picture has.
     */
    @Test
    void thePremiseSeesTheGeometryThatWillBePainted()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JPanel[] content = new JPanel[1];
        int[] saw = {-1};
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("sees");
                content[0] = canvas(300, 200);
                owner[0].setContentPane(content[0]);
                owner[0].setSize(900, 700);
            });
            var drawn = SheetCapture.take(owner[0], content[0],
                    SheetCapture.packed(),
                    held -> {
                        saw[0] = held.getWidth();
                        return null;
                    },
                    () -> draw(content[0]));
            assertEquals(300, saw[0],
                    "the premise sees the proved width");
            assertEquals(300, drawn.getWidth(),
                    "and so does the picture");
        } finally {
            dispose(owner[0]);
        }
    }

    /**
     * A layout that moves after it is settled on refuses.
     *
     * <p>Nothing may change the geometry once the snapshot is
     * restored - not the premise, which is a read, and not the
     * drawing. If either does, the pixels are of one state and the
     * record of another, and a sheet that disagrees with its own
     * trace is what this whole issue has been about.
     */
    @Test
    void aLayoutThatMovesAfterItIsSettledOnRefuses() throws Exception {
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
            IllegalStateException refused = assertThrows(
                    IllegalStateException.class,
                    () -> SheetCapture.take(dialog[0], content[0],
                            SheetCapture.packed(),
                            held -> {
                                // A premise is a read. This one is
                                // not, and that must not be allowed
                                // to change what is photographed.
                                content[0].narrow = true;
                                content[0].invalidate();
                                dialog[0].invalidate();
                                dialog[0].pack();
                                return null;
                            },
                            () -> draw(content[0])));
            assertTrue(refused.getMessage().contains("333x223")
                            && refused.getMessage().contains("326x206"),
                    "the capture refuses rather than photographing"
                            + " the other stable answer, and names"
                            + " both: " + refused.getMessage());
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * A fixed canvas is established before proof, never while drawn.
     *
     * <p>{@code FIXED_CANVAS} means the study owns the canvas
     * geometry - not that the geometry may first be chosen while
     * drawing. The settings study sized and laid itself out inside
     * its picture, moving from 0x0 to 560x309 as it drew; that was
     * legitimate in the old model and is the wrong phase in this
     * one, because drawing must not establish state.
     *
     * <p>Both halves are held here: establishing moves the component
     * to its declared size before anything is proved, and a picture
     * that changes the size is refused. Without the second half the
     * workaround could quietly move back into the picture.
     */
    @Test
    void aFixedCanvasIsEstablishedBeforeProofAndNotWhileDrawing()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a component still has to exist");
        JPanel[] content = new JPanel[1];
        int[] sawWhenDrawing = {-1};
        SwingUtilities.invokeAndWait(() -> {
            content[0] = new JPanel();
            content[0].setPreferredSize(new Dimension(560, 309));
            content[0].setOpaque(true);
        });

        // Nothing has sized it: a canvas with no window starts at
        // nothing at all.
        assertEquals(0, content[0].getWidth(),
                "the premise: an unsized canvas is 0 wide");

        var drawn = SheetCapture.take(null, content[0],
                SheetCapture.fixedCanvas(() -> {
                    content[0].setSize(560,
                            content[0].getPreferredSize().height);
                    content[0].doLayout();
                }),
                SheetCapture.Premise.none(),
                () -> {
                    sawWhenDrawing[0] = content[0].getWidth();
                    return draw(content[0]);
                });

        assertEquals(560, sawWhenDrawing[0],
                "the picture sees a canvas that was already"
                        + " established, so it has only to draw it");
        assertEquals(560, drawn.getWidth(),
                "and the image is that size");

        // And the other half: drawing may not change it.
        JPanel[] second = new JPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            second[0] = new JPanel();
            second[0].setPreferredSize(new Dimension(560, 309));
            second[0].setOpaque(true);
        });
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> SheetCapture.take(null, second[0],
                        SheetCapture.fixedCanvas(() -> {
                            second[0].setSize(560, 309);
                            second[0].doLayout();
                        }),
                        SheetCapture.Premise.none(),
                        () -> {
                            // Establishing state, in the wrong phase.
                            second[0].setSize(400, 309);
                            second[0].doLayout();
                            return draw(second[0]);
                        }));
        assertTrue(refused.getMessage().contains("drawing")
                        && refused.getMessage().contains("560x309")
                        && refused.getMessage().contains("400x309"),
                "a picture that establishes geometry is refused, and"
                        + " named: " + refused.getMessage());
    }

    /**
     * A capture's name cannot reach the next capture's trace.
     *
     * <p>The markers exist so a retained pair maps to its sheet by
     * reading rather than by counting - which is how the first
     * retained Place and Time pair could not be read at all. A
     * marker that outlived its capture would be worse than none: it
     * would label a line with the wrong sheet, and a diagnosis that
     * trusted it would read the wrong window.
     *
     * <p>These generators run one after another in a single JVM, so
     * this is not hypothetical.
     */
    @Test
    void aSubjectMarkerDoesNotOutliveItsCapture() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        Path trace = Files.createTempFile("subject-trace", ".tsv");
        JFrame[] owner = new JFrame[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("subjects");
                content[0] = canvas(300, 200);
                owner[0].setContentPane(content[0]);
            });
            traced(trace, () -> {
                SheetCapture.tracing("first-sheet.png");
                SheetCapture.of(owner[0], content[0],
                        SheetCapture.packed());
                // The next capture names nothing.
                SheetCapture.of(owner[0], content[0],
                        SheetCapture.packed());
                return 0;
            });
            List<String> lines = Files.readAllLines(trace,
                    StandardCharsets.UTF_8);
            List<String> named = lines.stream()
                    .filter(one -> one.contains("subject=")).toList();
            List<String> prePaints = lines.stream()
                    .filter(one -> one.startsWith("pre-paint")).toList();

            assertEquals(2, prePaints.size(),
                    "the premise: two captures happened");
            assertTrue(named.stream().allMatch(one ->
                            one.contains("subject=first-sheet.png")),
                    "only the capture that named itself carries a"
                            + " name: " + named);
            assertFalse(prePaints.get(1).contains("subject="),
                    "and the second capture carries none at all,"
                            + " rather than inheriting the first"
                            + " one's: " + prePaints.get(1));
        } finally {
            dispose(owner[0]);
            Files.deleteIfExists(trace);
        }
    }

    /**
     * A size that only exists until the queue runs is not a size.
     *
     * <p>The second failure mode. An application policy sets the
     * window to 420 and the peer answers, an event cycle later, by
     * pulling an unshown window back to its packed width. Read
     * inside the block that applied it, the answer is 420 every
     * time and the capture settles immediately on a width the
     * window does not have by the time anything is painted. Read
     * after the queue has been drained, the answer is what the
     * window actually kept.
     *
     * <p>So this policy must never settle, and must refuse naming
     * itself - rather than reporting a confident 420 that was true
     * for one event cycle. This is the shape of what was
     * photographed: 324x263 with 420x263 recorded as proved.
     */
    @Test
    void aSizeThatOnlyLastsUntilTheQueueRunsIsNotSettled()
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
            // States 420, and something undoes it on the next
            // cycle - which is precisely what the peer does to an
            // unshown window - landing somewhere slightly different
            // each time, as this dialog's two stable widths do.
            //
            // Read inside the block that applied it, the answer is
            // 420 every round and the policy looks settled at once.
            // Read after the queue has run, the answer is what the
            // window kept, and it is never twice the same.
            int[] kept = {324};
            Runnable transient420 = () -> {
                dialog[0].setSize(420, dialog[0].getHeight());
                dialog[0].invalidate();
                dialog[0].validate();
                int back = kept[0]++;
                SwingUtilities.invokeLater(() -> {
                    dialog[0].setSize(back, dialog[0].getHeight());
                    dialog[0].invalidate();
                    dialog[0].validate();
                });
            };

            IllegalStateException refused = assertThrows(
                    IllegalStateException.class,
                    () -> SheetCapture.of(dialog[0], content[0],
                            SheetCapture.applicationSized(
                                    "test.transientFloor", transient420,
                                    transient420)));
            assertTrue(refused.getMessage()
                            .contains("test.transientFloor"),
                    "the refusal names the policy: "
                            + refused.getMessage());
            assertTrue(refused.getMessage().contains("never")
                            && refused.getMessage().contains("settled"),
                    "and says it never settled, rather than reporting"
                            + " the 420 it held for one event cycle: "
                            + refused.getMessage());
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
