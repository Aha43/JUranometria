package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A layout with one answer (Sprint 33, issue #350).
 *
 * <p>Chart Options had <strong>two</strong>. Each explanation was
 * re-wrapped to the width it had been given on the previous layout
 * pass; that width depends on whether a scroll bar is showing; the
 * scroll bar depends on how tall the wrapped text turned out. The
 * circle has been in the code since #311, with a comment describing
 * it and a loop re-wrapping twice to get round it.
 *
 * <p>Twice is not enough when the circle has two solutions rather
 * than none. From identical inputs the dialog settled 419 px wide on
 * 25 runs in 30 and 420 on the other five, and both were
 * self-consistent - packing again confirmed whichever the run had
 * found. It surfaced as evidence that would not reproduce, but a
 * reader could meet either dialog.
 *
 * <p>The repair is a declared line width, {@code EXPLANATION_WIDTH}:
 * the dialog says how wide a line of prose should be, the label
 * computes its height for that, and the window has nothing left to
 * feed back. These are the claims that keep it that way.
 */
class ChartOptionsLayoutTest {

    /** Every switch on the dialog, across its four tabs. */
    private static final int SWITCHES = 17;

    /**
     * One geometry per language, from differing predecessors.
     *
     * <p>The predecessors matter: the old defect chose its answer
     * from whatever width the labels had been measured at last, so a
     * dialog built after a narrow one settled differently from one
     * built after a wide one. Building each repetition after a
     * deliberately different predecessor is what makes this a test
     * of the repair rather than of a quiet machine.
     */
    @Test
    void everyBuildSettlesToOneGeometryPerLanguage() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real dialog has to be laid out");
        for (String language : List.of("en", "nb-NO")) {
            Set<String> geometries = new LinkedHashSet<>();
            for (int width : List.of(0, 280, 700, 420, 300)) {
                geometries.add(geometryAfterPredecessor(language, width));
            }
            assertEquals(1, geometries.size(),
                    "the " + language + " dialog settles to one"
                            + " geometry however wide the dialog"
                            + " before it was. It settled to "
                            + geometries.size() + ": " + geometries);
        }
    }

    /**
     * Narrower, wider, and back to the same dialog.
     *
     * <p>Resized the way a reader resizes it - by dragging the
     * window. <strong>The dialog registers no component listener</strong>,
     * so dragging it narrow does not re-wrap the prose, before this
     * change or after; the wrap is decided when the dialog is built
     * and when its sizing policy is re-applied. This asserts what
     * the dialog does rather than a re-flow it has never had: every
     * switch stays where a reader can reach it, and re-applying the
     * policy brings back exactly the dialog we started with.
     *
     * <p>That last part is the one that would have failed before.
     * A layout that remembers how wide it was last measured comes
     * back a different width, and the two answers are both stable.
     */
    @Test
    void resizingNarrowerAndWiderReturnsToTheSameDialog()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real dialog has to be laid out");
        withDialog("nb-NO", (frame, dialog) -> {
            JComponent content = (JComponent) dialog.getContentPane();
            String canonical = geometryOf(content);
            String switchPlaces = switchPlacesOf(content);

            assertTrue(everyLineFits(content),
                    "at the size the dialog chooses, every wrapped"
                            + " line fits the label that draws it");

            resize(dialog, 300);
            assertEquals(SWITCHES, switchesIn(content).size(),
                    "dragged narrow, every switch is still there");
            assertTrue(allReachable(content),
                    "and still inside the dialog, where a reader can"
                            + " press them");

            resize(dialog, 700);
            assertEquals(SWITCHES, switchesIn(content).size(),
                    "and dragged wide");

            SwingUtilities.invokeAndWait(() ->
                    ChartOptionsDialog.settle(dialog));
            assertEquals(canonical, geometryOf(content),
                    "and settling afterwards returns the dialog it"
                            + " started as. A layout that remembered"
                            + " how wide it had been is the defect"
                            + " this is here for");
            assertEquals(switchPlaces, switchPlacesOf(content),
                    "with every switch back where it was");
        });
    }

    /**
     * The words fit, nothing is lost, and a screen reader gets the
     * sentence whole.
     *
     * <p>Wrapping is a visual arrangement. The reader who listens
     * must get the sentence as it was written - not with the line
     * breaks read out, and not truncated to what fitted.
     */
    @Test
    void nothingIsTruncatedAndTheSpokenSentenceIsWhole()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a real dialog has to be laid out");
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText said = InterfaceText.forLanguage(language);
            withDialog(language, (frame, dialog) -> {
                JComponent content = (JComponent) dialog.getContentPane();
                assertTrue(everyLineFits(content),
                        language + ": every wrapped line fits inside"
                                + " the label that draws it");

                List<JCheckBox> switches = switchesIn(content);
                assertEquals(SWITCHES, switches.size(),
                        language + ": all " + SWITCHES + " switches"
                                + " are present");

                String whole = said.say(
                        "chartoptions.deepSkyObjects.explain");
                boolean spoken = switches.stream().anyMatch(box ->
                        box.getAccessibleContext()
                                .getAccessibleDescription() != null
                                && box.getAccessibleContext()
                                        .getAccessibleDescription()
                                        .contains(whole));
                assertTrue(spoken, language + ": a screen reader is"
                        + " given the sentence unwrapped and entire."
                        + " Looking for: " + whole);

                for (JLabel label : wrappedLabels(content)) {
                    assertTrue(!label.getText().contains("<br><br>"),
                            language + ": no empty line is wrapped"
                                    + " into the prose");
                }
            });
        }
    }

    // ---- the machinery ----------------------------------------

    private interface Body {
        void run(JFrame frame, ChartOptionsDialog dialog)
                throws Exception;
    }

    private static void withDialog(String language, Body body)
            throws Exception {
        java.util.prefs.Preferences node = java.util.prefs.Preferences
                .userRoot().node("juranometria-layout-"
                        + System.nanoTime());
        JFrame[] frame = new JFrame[1];
        ChartOptionsDialog[] dialog = new ChartOptionsDialog[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new JFrame("layout");
                dialog[0] = ChartOptionsDialog.packedForStudy(frame[0],
                        language, new ChartOptionsController(
                                ChartOptionsStore.forNode(node)));
            });
            SwingUtilities.invokeAndWait(() -> { });
            body.run(frame[0], dialog[0]);
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (dialog[0] != null) {
                    dialog[0].dispose();
                }
                if (frame[0] != null) {
                    frame[0].dispose();
                }
            });
            node.removeNode();
        }
    }

    /** Builds a dialog after one of a stated width, and measures it. */
    private static String geometryAfterPredecessor(String language,
                                                   int predecessorWidth)
            throws Exception {
        if (predecessorWidth > 0) {
            withDialog(language, (frame, before) ->
                    resize(before, predecessorWidth));
        }
        String[] geometry = new String[1];
        withDialog(language, (frame, dialog) -> {
            JComponent content = (JComponent) dialog.getContentPane();
            JTabbedPane tabs = ChartOptionsDialog.tabsOf(content);
            StringBuilder all = new StringBuilder();
            for (int tab = 0; tab < tabs.getTabCount(); tab++) {
                int which = tab;
                SwingUtilities.invokeAndWait(() -> {
                    tabs.setSelectedIndex(which);
                    ChartOptionsDialog.settle(dialog);
                });
                all.append(geometryOf(content)).append('\n');
            }
            geometry[0] = all.toString();
        });
        return geometry[0];
    }

    /** Dragged, the way a reader drags it: no re-wrap follows. */
    private static void resize(ChartOptionsDialog dialog, int width)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            dialog.setSize(width, dialog.getHeight());
            dialog.validate();
        });
        SwingUtilities.invokeAndWait(() -> { });
    }

    /** Every switch inside the dialog, where it can be pressed. */
    private static boolean allReachable(JComponent content)
            throws Exception {
        boolean[] reachable = {true};
        SwingUtilities.invokeAndWait(() -> {
            for (JCheckBox box : switchesIn(content)) {
                java.awt.Point at = SwingUtilities.convertPoint(box,
                        0, 0, content);
                if (at.x < 0 || at.y < 0 || box.getWidth() <= 0
                        || box.getHeight() <= 0) {
                    reachable[0] = false;
                }
            }
        });
        return reachable[0];
    }

    /** Every wrapped line inside the label that draws it. */
    private static boolean everyLineFits(JComponent content)
            throws Exception {
        boolean[] fits = {true};
        SwingUtilities.invokeAndWait(() -> {
            for (JLabel label : wrappedLabels(content)) {
                java.awt.Insets insets = label.getInsets();
                int room = label.getWidth() - insets.left - insets.right;
                if (room <= 0) {
                    continue;
                }
                java.awt.FontMetrics metrics =
                        label.getFontMetrics(label.getFont());
                for (String line : label.getText()
                        .replace("<html>", "").replace("</html>", "")
                        .split("<br>")) {
                    String plain = line.replace("&amp;", "&")
                            .replace("&lt;", "<").replace("&gt;", ">");
                    if (metrics.stringWidth(plain) > room) {
                        // WrappedText leaves a single word longer
                        // than the room rather than cutting it: a
                        // name split mid-word is worse than one that
                        // overhangs, and the caller who chose the
                        // width is better placed to widen it. So a
                        // line with nowhere to break is allowed.
                        if (plain.trim().contains(" ")) {
                            fits[0] = false;
                            System.out.println("OVERLONG: room="
                                    + room + " width="
                                    + metrics.stringWidth(plain)
                                    + " line=" + plain);
                        }
                    }
                }
            }
        });
        return fits[0];
    }

    private static String geometryOf(JComponent content)
            throws Exception {
        StringBuilder out = new StringBuilder();
        SwingUtilities.invokeAndWait(() -> append(out, content));
        return out.toString();
    }

    /**
     * Where every switch sits, which is what a reader reaches for.
     *
     * <p>Asserted beside the full geometry rather than instead of
     * it. The two together say the dialog came back the same size
     * AND that its controls came back to the same places.
     */
    private static String switchPlacesOf(JComponent content)
            throws Exception {
        StringBuilder out = new StringBuilder();
        SwingUtilities.invokeAndWait(() -> {
            for (JCheckBox box : switchesIn(content)) {
                out.append(box.getText()).append('@')
                        .append(SwingUtilities.convertPoint(box, 0, 0,
                                content))
                        .append(' ').append(box.getWidth()).append('x')
                        .append(box.getHeight()).append(';');
            }
        });
        return out.toString();
    }

    /**
     * Every component's bounds, except the tab strip's own arrows.
     *
     * <p>{@code ScrollableTabButton} is the pair of arrows a
     * {@code JTabbedPane} shows when its tabs do not fit. Dragging
     * the dialog to 300 px realises them, and Swing does not
     * un-realise them when it is dragged back - on Linux they stay
     * at 16x16 where they had been 0x0. That is the toolkit's
     * behaviour and it is reader-visible, but it is not this
     * dialog's layout answer: the dialog and the tabbed pane come
     * back to exactly the bounds they had, and so does every
     * switch.
     *
     * <p>Excluded by NAME and for a stated reason, rather than by
     * loosening the comparison. Anything else moving still fails.
     */
    private static void append(StringBuilder out, Component from) {
        if (from.getClass().getSimpleName()
                .equals("ScrollableTabButton")) {
            return;
        }
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

    private static List<JLabel> wrappedLabels(Container from) {
        List<JLabel> found = new ArrayList<>();
        for (Component child : from.getComponents()) {
            if (child instanceof JLabel label
                    && label.getText() != null
                    && label.getText().startsWith("<html>")) {
                found.add(label);
            }
            if (child instanceof Container nested) {
                found.addAll(wrappedLabels(nested));
            }
        }
        return found;
    }

    private static List<JCheckBox> switchesIn(Container from) {
        List<JCheckBox> found = new ArrayList<>();
        for (Component child : from.getComponents()) {
            if (child instanceof JCheckBox box) {
                found.add(box);
            }
            if (child instanceof Container nested) {
                found.addAll(switchesIn(nested));
            }
        }
        return found;
    }
}
