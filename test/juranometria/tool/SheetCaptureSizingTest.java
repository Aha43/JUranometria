package juranometria.tool;

import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * <p>And an application's size cannot be read back from its window:
 * the peer pulls an unshown window back to its packed size, sometimes
 * inside the very block that set it. So an application policy
 * declares its content size as a value, and the capture is held to
 * that (#380).
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
     * A geometry the policy did not establish is never proved (#376).
     *
     * <p>The transition, exactly as the trace of the failed macOS 27
     * gate recorded it for {@code export-nb-NO-5-pdf-letter.png}: the
     * pack established 333x223 twice running, with the content at
     * its preference; a resize left over from the previous sheet -
     * 326x206, {@code export-nb-NO-4-svg-a4} - then landed while the
     * layout settled; and the fixed point proved 326x206 with the
     * preference still 333x223. The paint block restored that proof
     * faithfully and painted it, because every check after the
     * proof compared against the proof.
     *
     * <p>The stale resize is queued here rather than waited for, so
     * the transition happens every run instead of once in two. The
     * layout keeps one answer throughout: nothing about the content
     * changed, so a capture has no business proving another size.
     */
    @Test
    void aGeometryThePolicyDidNotEstablishIsRefused() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        Bistable[] content = new Bistable[1];
        Dimension[] stale = new Dimension[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new JDialog(owner[0]);
                content[0] = new Bistable();
                content[0].setOpaque(true);
                dialog[0].setContentPane(content[0]);
                // The previous sheet's window, which is the size the
                // leftover resize puts back.
                content[0].narrow = true;
                dialog[0].pack();
                stale[0] = dialog[0].getSize();
                content[0].narrow = false;
            });
            SheetCapture.Sizing packedThenStale = (window, held) -> {
                SheetCapture.Established packed =
                        SheetCapture.packed().establish(window, held);
                SwingUtilities.invokeLater(() -> window.setSize(stale[0]));
                return packed;
            };
            IllegalStateException refused = assertThrows(
                    IllegalStateException.class,
                    () -> SheetCapture.take(dialog[0], content[0],
                            packedThenStale, SheetCapture.Premise.none(),
                            () -> draw(content[0])),
                    "a capture that proves a geometry its policy did"
                            + " not establish paints the previous"
                            + " sheet's size under this sheet's name");
            assertTrue(refused.getMessage().contains("333x223")
                            && refused.getMessage().contains("326x206"),
                    "the refusal names what was established and what"
                            + " the layout drifted to: "
                            + refused.getMessage());
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * A fixed canvas resized after the study chose it is refused.
     */
    @Test
    void aFixedCanvasResizedAfterItWasChosenIsRefused() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a component still has to exist");
        JPanel[] content = new JPanel[1];
        SwingUtilities.invokeAndWait(() -> content[0] = canvas(560, 309));
        SheetCapture.Sizing chosen = SheetCapture.fixedCanvas(() -> {
            content[0].setSize(560, 309);
            content[0].doLayout();
        });
        SheetCapture.Sizing thenResized = (window, held) -> {
            SheetCapture.Established said = chosen.establish(window, held);
            SwingUtilities.invokeLater(() -> content[0].setSize(420, 309));
            return said;
        };
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> SheetCapture.take(null, content[0], thenResized,
                        SheetCapture.Premise.none(),
                        () -> draw(content[0])));
        assertTrue(refused.getMessage().contains("560x309")
                        && refused.getMessage().contains("420x309"),
                "the study's canvas, and the size it was moved to: "
                        + refused.getMessage());
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

    // ---- application-sized: a declared value, restated once (#380) --

    /** The reviewed floor these contracts use, as Place and Time's. */
    private static final int FLOOR = 420;

    /**
     * Place and Time's rule in miniature: packed, with the width
     * raised to the floor. The declaration is computed from the
     * layout's preference and the window's insets, the application
     * operation packs and raises - two statements of one rule.
     */
    private static SheetCapture.ApplicationPolicy floorPolicy(
            JDialog dialog) {
        return new SheetCapture.ApplicationPolicy() {

            @Override
            public Dimension declaredContent() {
                dialog.addNotify();
                java.awt.Insets chrome = dialog.getInsets();
                Dimension packed = dialog.getPreferredSize();
                return new Dimension(
                        Math.max(packed.width, FLOOR)
                                - chrome.left - chrome.right,
                        packed.height - chrome.top - chrome.bottom);
            }

            @Override
            public void apply() {
                dialog.pack();
                dialog.setSize(Math.max(dialog.getWidth(), FLOOR),
                        dialog.getHeight());
                dialog.invalidate();
                dialog.validate();
            }
        };
    }

    /** A packed dialog around a content, so it has a native peer. */
    private static JDialog packedAround(JFrame[] owner, JPanel content,
                                        JDialog dialog) {
        if (owner[0] == null) {
            owner[0] = new JFrame("owner");
        }
        JDialog made = dialog != null ? dialog : new JDialog(owner[0]);
        made.setContentPane(content);
        made.pack();
        return made;
    }

    /** The floor's content width on this window. */
    private static int floorContent(JDialog dialog) {
        java.awt.Insets chrome = dialog.getInsets();
        return FLOOR - chrome.left - chrome.right;
    }

    /**
     * The declared size is what is painted, not the preference.
     */
    @Test
    void applicationSizedPaintsTheDeclaredGeometry() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                content[0] = canvas(300, 200);
                dialog[0] = packedAround(owner, content[0], null);
            });
            var drawn = SheetCapture.of(dialog[0], content[0],
                    SheetCapture.applicationSized("test.floorPolicy",
                            floorPolicy(dialog[0])));
            assertEquals(floorContent(dialog[0]), drawn.getWidth(),
                    "the policy decides, not the preference. Packing"
                            + " would have given 300 - a width no"
                            + " reader meets");
            assertEquals(200, drawn.getHeight());
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * Packing an application-sized window gives the wrong picture.
     *
     * <p>The mutation, in miniature: the same window, photographed
     * both ways, disagreeing exactly as Place and Time did before it
     * was classified.
     */
    @Test
    void packingAnApplicationSizedWindowProducesTheWidthNobodySees()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        // Two dialogs, one per kind. Photographing one dialog both
        // ways let the native reply to the first capture's sizing
        // land in the second - the #376 transition, which the
        // coordinator correctly refuses - and this compares the two
        // kinds, not what one capture leaves behind for the next.
        JFrame[] owner = new JFrame[1];
        JDialog[] byPolicyDialog = new JDialog[1];
        JDialog[] byPackingDialog = new JDialog[1];
        JPanel[] policyContent = new JPanel[1];
        JPanel[] packedContent = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                policyContent[0] = canvas(326, 263);
                packedContent[0] = canvas(326, 263);
                byPolicyDialog[0] = packedAround(owner, policyContent[0],
                        null);
                byPackingDialog[0] = packedAround(owner,
                        packedContent[0], null);
            });
            int byPolicy = SheetCapture.of(byPolicyDialog[0],
                    policyContent[0], SheetCapture.applicationSized(
                            "test.floorPolicy",
                            floorPolicy(byPolicyDialog[0]))).getWidth();
            int byPacking = SheetCapture.of(byPackingDialog[0],
                    packedContent[0], SheetCapture.packed()).getWidth();

            assertEquals(floorContent(byPolicyDialog[0]), byPolicy,
                    "the reader's width");
            assertEquals(326, byPacking,
                    "and the one packing gives, which is the defect"
                            + " this distinction exists to prevent");
        } finally {
            dispose(byPolicyDialog[0], byPackingDialog[0], owner[0]);
        }
    }

    /** An application policy needs a window to size. */
    @Test
    void anApplicationPolicyWithoutAWindowIsRefused() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a component still has to exist");
        JPanel[] content = new JPanel[1];
        SwingUtilities.invokeAndWait(() -> content[0] = canvas(300, 200));
        assertThrows(IllegalArgumentException.class,
                () -> SheetCapture.take(null, content[0],
                        SheetCapture.applicationSized("test.nothing",
                                new SheetCapture.ApplicationPolicy() {
                                    @Override
                                    public Dimension declaredContent() {
                                        return new Dimension(300, 200);
                                    }

                                    @Override
                                    public void apply() {
                                    }
                                }),
                        SheetCapture.Premise.none(),
                        () -> draw(content[0])),
                "a component with no window is a fixed canvas, and"
                        + " declaring it application-sized makes an"
                        + " impossible claim look valid");
    }

    /**
     * Declared once, applied once, restated once - with the frozen
     * value.
     *
     * <p>The declaration is asked for exactly once and copied: this
     * policy hands back an object it then changes, and would answer
     * differently if asked again. The restatement is the second and
     * last statement of the size, and it states the original value.
     */
    @Test
    void theDeclarationIsFrozenAndRestatedExactlyOnce() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        Path trace = Files.createTempFile("restated-once", ".tsv");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        int[] asked = {0};
        int[] applied = {0};
        try {
            SwingUtilities.invokeAndWait(() -> {
                content[0] = canvas(300, 200);
                dialog[0] = packedAround(owner, content[0], null);
            });
            SheetCapture.ApplicationPolicy rule = floorPolicy(dialog[0]);
            Dimension[] handedOut = new Dimension[1];
            var drawn = traced(trace, () -> SheetCapture.of(dialog[0],
                    content[0], SheetCapture.applicationSized(
                            "test.frozen",
                            new SheetCapture.ApplicationPolicy() {
                                @Override
                                public Dimension declaredContent() {
                                    asked[0]++;
                                    handedOut[0] = asked[0] == 1
                                            ? rule.declaredContent()
                                            : new Dimension(360, 180);
                                    return handedOut[0];
                                }

                                @Override
                                public void apply() {
                                    applied[0]++;
                                    rule.apply();
                                    // Changes what it handed out.
                                    handedOut[0].setSize(360, 180);
                                }
                            })).getWidth());
            List<String> lines = Files.readAllLines(trace,
                    StandardCharsets.UTF_8);
            long restated = lines.stream()
                    .filter(one -> one.startsWith("restating")).count();

            assertEquals(1, asked[0], "declared once");
            assertEquals(1, applied[0], "applied once, and not again");
            assertEquals(1, restated,
                    "restated exactly once: " + lines);
            assertTrue(lines.stream().filter(one ->
                                    one.startsWith("restating"))
                            .allMatch(one -> one.contains("content="
                                    + floorContent(dialog[0]) + "x200")),
                    "with the value frozen when it was declared, not"
                            + " the one the policy changed it to: "
                            + lines);
            assertEquals(floorContent(dialog[0]), drawn,
                    "and that value is what is painted");
        } finally {
            dispose(dialog[0], owner[0]);
            Files.deleteIfExists(trace);
        }
    }

    /**
     * A window whose every size request is rolled back to 326 -
     * except the restatement's.
     *
     * <p>The pre-restatement rollback, made persistent: anything the
     * policy asks for through {@code setSize(int, int)} (which is
     * also how {@code pack} sizes a window) is answered with 326, as
     * the macOS peer answers an unshown dialog. Only the capture's
     * restatement of the frozen declaration, which states a
     * {@code Dimension}, is honoured. So nothing but the restatement
     * can bring this window to the declared width.
     */
    private static final class RolledBackUntilRestated extends JDialog {

        RolledBackUntilRestated(JFrame owner) {
            super(owner);
        }

        @Override
        public void setSize(int width, int height) {
            super.setSize(326, height);
        }

        @Override
        public void setSize(Dimension size) {
            // Past this class's own rollback, to Window's.
            super.setSize(size.width, size.height);
        }
    }

    /**
     * A rollback before the restatement is corrected by it.
     *
     * <p>Still part of establishing the size: the window answers the
     * policy's own request with the packed width, and the one
     * restatement of the frozen declaration is what puts the policy's
     * size in place before anything is proved.
     */
    @Test
    void aRollbackBeforeTheRestatementIsCorrected() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        RolledBackUntilRestated[] dialog = new RolledBackUntilRestated[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new RolledBackUntilRestated(owner[0]);
                content[0] = canvas(326, 263);
                packedAround(owner, content[0], dialog[0]);
            });
            var drawn = SheetCapture.of(dialog[0], content[0],
                    SheetCapture.applicationSized("test.rolledBackEarly",
                            floorPolicy(dialog[0])));
            assertEquals(floorContent(dialog[0]), drawn.getWidth(),
                    "the policy's request came back as 326; the"
                            + " restatement of the declared width is"
                            + " what put it in place");
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /** A window that the "peer" rolls back after the restatement. */
    private static final class RolledBackAfterRestating extends JDialog {

        private boolean armed;

        RolledBackAfterRestating(JFrame owner) {
            super(owner);
        }

        @Override
        public void setSize(Dimension size) {
            super.setSize(size);
            if (armed) {
                // The measured macOS case: the peer's answer to an
                // earlier request, landing inside the restatement's
                // own block.
                super.setSize(326, size.height);
            }
        }
    }

    /**
     * A rollback after the restatement contradicts what was
     * established, and refuses.
     */
    @Test
    void aRollbackAfterTheRestatementRefuses() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        RolledBackAfterRestating[] dialog = new RolledBackAfterRestating[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("owner");
                dialog[0] = new RolledBackAfterRestating(owner[0]);
                content[0] = canvas(300, 263);
                packedAround(owner, content[0], dialog[0]);
            });
            SheetCapture.ApplicationPolicy rule = floorPolicy(dialog[0]);
            IllegalStateException refused = assertThrows(
                    IllegalStateException.class,
                    () -> SheetCapture.of(dialog[0], content[0],
                            SheetCapture.applicationSized(
                                    "test.rolledBackLate",
                                    new SheetCapture.ApplicationPolicy() {
                                        @Override
                                        public Dimension declaredContent() {
                                            return rule.declaredContent();
                                        }

                                        @Override
                                        public void apply() {
                                            rule.apply();
                                            dialog[0].armed = true;
                                        }
                                    })));
            assertTrue(refused.getMessage().contains("content "
                            + floorContent(dialog[0]) + "x263")
                            && refused.getMessage().contains("326x"),
                    "the declared width is what was established, and"
                            + " the 326 the window was rolled back to"
                            + " after its one restatement is refused: "
                            + refused.getMessage());
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /**
     * Content that lags its window catches up to the declaration.
     *
     * <p>Linux, measured: the window already held the policy's 420
     * while the content still reported 374. Laying out at the
     * restatement brings it to the declared width.
     */
    @Test
    void aLaggingContentCatchesUpToTheDeclaration() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                content[0] = canvas(300, 263);
                dialog[0] = packedAround(owner, content[0], null);
            });
            SheetCapture.ApplicationPolicy rule = floorPolicy(dialog[0]);
            var drawn = SheetCapture.of(dialog[0], content[0],
                    SheetCapture.applicationSized("test.lagging",
                            new SheetCapture.ApplicationPolicy() {
                                @Override
                                public Dimension declaredContent() {
                                    return rule.declaredContent();
                                }

                                @Override
                                public void apply() {
                                    // The window takes the width; the
                                    // content is left where a lagging
                                    // layout leaves it.
                                    dialog[0].setSize(FLOOR,
                                            dialog[0].getHeight());
                                    content[0].setSize(374, 263);
                                }
                            }));
            assertEquals(floorContent(dialog[0]), drawn.getWidth(),
                    "the lagging 374 was laid out at the declared"
                            + " width, and that is what is painted");
        } finally {
            dispose(dialog[0], owner[0]);
        }
    }

    /** A window that cannot be made wider than 300 content pixels. */
    private static final class NarrowerThanDeclared extends JDialog {

        NarrowerThanDeclared(JFrame owner) {
            super(owner);
        }

        @Override
        public void setSize(Dimension size) {
            java.awt.Insets chrome = getInsets();
            super.setSize(Math.min(size.width,
                    300 + chrome.left + chrome.right), size.height);
        }
    }

    /**
     * Invalid or unrealizable declarations are refused.
     *
     * <p>What the coordinator can honestly check. Whether a plausible
     * declaration is the RIGHT value is its policy's own contract -
     * the coordinator cannot tell a false declaration from a native
     * rollback, and does not try. It refuses a declaration that is
     * missing or not a size, one made for content that is not the
     * window's content pane, and one the window cannot be brought to.
     */
    @Test
    void invalidOrUnrealizableDeclarationsAreRefused() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        JFrame[] owner = new JFrame[1];
        JDialog[] dialog = new JDialog[1];
        NarrowerThanDeclared[] narrow = new NarrowerThanDeclared[1];
        JPanel[] content = new JPanel[1];
        JPanel[] inside = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                content[0] = canvas(300, 200);
                inside[0] = canvas(100, 50);
                content[0].add(inside[0]);
                dialog[0] = packedAround(owner, content[0], null);
                narrow[0] = new NarrowerThanDeclared(owner[0]);
            });
            for (Dimension said : new Dimension[] {
                    null, new Dimension(0, 200), new Dimension(420, -1)}) {
                IllegalStateException refused = assertThrows(
                        IllegalStateException.class,
                        () -> SheetCapture.of(dialog[0], content[0],
                                SheetCapture.applicationSized(
                                        "test.notASize",
                                        declaring(said))));
                assertTrue(refused.getMessage().contains("test.notASize"),
                        "a declaration that is not a size: "
                                + refused.getMessage());
            }

            IllegalStateException notItsPane = assertThrows(
                    IllegalStateException.class,
                    () -> SheetCapture.of(dialog[0], inside[0],
                            SheetCapture.applicationSized(
                                    "test.notTheContentPane",
                                    declaring(new Dimension(100, 50)))));
            assertTrue(notItsPane.getMessage().contains("content pane"),
                    "content the window does not hold as its content"
                            + " pane: " + notItsPane.getMessage());

            JPanel[] held = new JPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                held[0] = canvas(300, 200);
                packedAround(owner, held[0], narrow[0]);
            });
            IllegalStateException unrealizable = assertThrows(
                    IllegalStateException.class,
                    () -> SheetCapture.of(narrow[0], held[0],
                            SheetCapture.applicationSized(
                                    "test.unrealizable",
                                    floorPolicy(narrow[0]))));
            assertTrue(unrealizable.getMessage().contains("300x"),
                    "and a declaration the window cannot be brought"
                            + " to, which is refused at the proof: "
                            + unrealizable.getMessage());
        } finally {
            dispose(dialog[0], narrow[0], owner[0]);
        }
    }

    /** A policy that declares a stated value and applies nothing. */
    private static SheetCapture.ApplicationPolicy declaring(Dimension said) {
        return new SheetCapture.ApplicationPolicy() {
            @Override
            public Dimension declaredContent() {
                return said;
            }

            @Override
            public void apply() {
            }
        };
    }

    /**
     * Packed and fixed-canvas captures never restate anything.
     *
     * <p>The restatement is the application policy's second
     * statement. A packed window's size is its layout's preference and
     * a fixed canvas is the study's; neither has a declaration to
     * state, and neither may acquire the operation.
     */
    @Test
    void packedAndFixedCanvasCapturesAreNeverRestated() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real window has to be sized");
        Path trace = Files.createTempFile("never-restated", ".tsv");
        JFrame[] owner = new JFrame[1];
        JPanel[] content = new JPanel[1];
        JPanel[] canvasAlone = new JPanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("packed");
                content[0] = canvas(300, 200);
                owner[0].setContentPane(content[0]);
                canvasAlone[0] = canvas(560, 309);
            });
            traced(trace, () -> {
                SheetCapture.of(owner[0], content[0],
                        SheetCapture.packed());
                SheetCapture.take(null, canvasAlone[0],
                        SheetCapture.fixedCanvas(() -> {
                            canvasAlone[0].setSize(560, 309);
                            canvasAlone[0].doLayout();
                        }),
                        SheetCapture.Premise.none(),
                        () -> draw(canvasAlone[0]));
                return 0;
            });
            List<String> lines = Files.readAllLines(trace,
                    StandardCharsets.UTF_8);
            assertEquals(2, lines.stream().filter(one ->
                            one.startsWith("pre-paint")).count(),
                    "the premise: both captures happened");
            assertTrue(lines.stream().noneMatch(one ->
                            one.startsWith("restating")
                                    || one.startsWith("declared")),
                    "and neither declared nor restated anything: "
                            + lines);
        } finally {
            dispose(owner[0]);
            Files.deleteIfExists(trace);
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
