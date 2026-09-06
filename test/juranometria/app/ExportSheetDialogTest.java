package juranometria.app;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.ui.ReaderInput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The export dialog a reader actually meets (Sprint 29, issue #286).
 *
 * <p>A dialog that is right at one text size, in one theme, in a
 * window someone dragged wide is a dialog that has been looked at
 * once. So it is built at ordinary and enlarged text, on both
 * grounds, and required to fit the narrowest window the atlas
 * supports - and its controls are pressed rather than called.
 */
class ExportSheetDialogTest {

    /** The narrowest window the atlas supports, from the layout rule. */
    private static final int NARROWEST_WINDOW_PX = 640;

    private static ExportSheet.Request defaults() {
        return ExportSheetSession.defaults();
    }

    @SuppressWarnings("unchecked")
    private static <T extends JComponent> T named(JComponent root,
                                                  String name) {
        if (name.equals(root.getName())) {
            return (T) root;
        }
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JComponent component) {
                T found = named(component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void everyChoiceIsThereAndNothingElseIs() {
        List<ExportSheet.Request> chosen = new ArrayList<>();
        JComponent content = ExportSheetDialog.content(defaults(),
                chosen::add, () -> { });

        JComboBox<SheetFormat> format =
                named(content, ExportSheetDialog.FORMAT_BOX);
        JComboBox<PaperSize> paper =
                named(content, ExportSheetDialog.PAPER_BOX);
        JComboBox<Integer> resolution =
                named(content, ExportSheetDialog.RESOLUTION_BOX);
        JCheckBox working = named(content, ExportSheetDialog.WORKING_BOX);

        assertEquals(3, format.getItemCount(),
                "three formats, and the gate approved no fourth");
        assertEquals(2, paper.getItemCount(),
                "two papers, neither guessed from a locale");
        assertEquals(3, resolution.getItemCount(),
                "and the resolutions a sheet is worth printing at");
        assertFalse(working.isSelected(),
                "the transient marks are off unless asked for");

        // Nothing the gate did not find meaningful: no projection, no
        // magnitude, no palette. The chart already owns those, and a
        // sheet is the chart.
        for (String absent : List.of("Projection", "Magnitude",
                "Colour", "Color", "Field")) {
            assertFalse(labels(content).contains(absent),
                    "the dialog does not re-ask what the chart"
                            + " already decides: " + absent);
        }
    }

    @Test
    void resolutionIsOfferedOnlyWhereItMeansSomething() {
        JComponent content = ExportSheetDialog.content(defaults(),
                request -> { }, () -> { });
        JComboBox<SheetFormat> format =
                named(content, ExportSheetDialog.FORMAT_BOX);
        JComboBox<Integer> resolution =
                named(content, ExportSheetDialog.RESOLUTION_BOX);

        format.setSelectedItem(SheetFormat.SVG);
        assertFalse(resolution.isEnabled(),
                "a vector sheet has no resolution to choose");
        format.setSelectedItem(SheetFormat.PDF);
        assertFalse(resolution.isEnabled(), "nor a PDF");
        format.setSelectedItem(SheetFormat.PNG);
        assertTrue(resolution.isEnabled(),
                "and the one that does, offers it");

        // Greyed rather than hidden: a control that vanishes moves
        // everything below it, and a reader loses their place.
        assertTrue(resolution.isVisible(),
                "the control stays where it was, and says it does not"
                        + " apply");
    }

    @Test
    void pressingExportReportsExactlyWhatWasChosen() {
        List<ExportSheet.Request> chosen = new ArrayList<>();
        List<String> cancelled = new ArrayList<>();
        JComponent content = ExportSheetDialog.content(defaults(),
                chosen::add, () -> cancelled.add("cancelled"));

        JComboBox<SheetFormat> format =
                named(content, ExportSheetDialog.FORMAT_BOX);
        JComboBox<PaperSize> paper =
                named(content, ExportSheetDialog.PAPER_BOX);
        JComboBox<Integer> resolution =
                named(content, ExportSheetDialog.RESOLUTION_BOX);
        JCheckBox working = named(content, ExportSheetDialog.WORKING_BOX);

        format.setSelectedItem(SheetFormat.PNG);
        paper.setSelectedItem(PaperSize.LETTER);
        resolution.setSelectedItem(600);
        working.setSelected(true);

        assertEquals(List.of(), chosen,
                "nothing is chosen until the reader says so");
        ((JButton) named(content, ExportSheetDialog.EXPORT_BUTTON))
                .doClick();

        assertEquals(1, chosen.size(), "one press, one request");
        assertEquals(new ExportSheet.Request(SheetFormat.PNG,
                        PaperSize.LETTER, 600, true), chosen.get(0),
                "carrying every choice the reader made");
        assertEquals(List.of(), cancelled,
                "and Export is not Cancel");
    }

    @Test
    void cancellingChoosesNothing() {
        List<ExportSheet.Request> chosen = new ArrayList<>();
        List<String> cancelled = new ArrayList<>();
        JComponent content = ExportSheetDialog.content(defaults(),
                chosen::add, () -> cancelled.add("cancelled"));

        ((JButton) named(content, ExportSheetDialog.CANCEL_BUTTON))
                .doClick();
        assertEquals(List.of("cancelled"), cancelled, "the way out works");
        assertEquals(List.of(), chosen,
                "and nothing was chosen, so nothing can be written");
    }

    @Test
    void itFitsTheNarrowestWindowAtEveryTextSizeAndOnBothGrounds()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "laying a dialog out needs a toolkit");

        for (boolean dark : new boolean[] {false, true}) {
            for (float scale : new float[] {1.0f, 1.5f}) {
                Dimension[] packed = new Dimension[1];
                SwingSession.restoring(() -> SwingUtilities.invokeAndWait(
                        () -> {
                    // On the event thread, which is where a look and
                    // feel is changed: doing it from the test thread
                    // raced whatever else was laying out, and a later
                    // dialog packed to a stale floor once in a full
                    // suite run before this was fixed.
                    UiTheme.apply(dark);
                    JComponent content = ExportSheetDialog.content(
                            defaults(), request -> { }, () -> { });
                    // The text is enlarged on this tree rather than
                    // in UIManager: a global font override outlives
                    // the look and feel the guard restores, and the
                    // next display test inherits it (found by the
                    // suite - two later journeys started laying out
                    // to nothing).
                    enlarge(content, scale);
                    JDialog dialog = new JDialog();
                    dialog.setContentPane(content);
                    dialog.pack();
                    packed[0] = dialog.getSize();
                    dialog.dispose();
                }));
                Dimension size = packed[0];

                // The first version of this dialog put each format's
                // explanation inside the format list, and a combo is
                // as wide as its widest entry: 725 px on the CI
                // display at this text size, against 640 of window.
                // Measured on both platforms after the fix - 428 px
                // here, 441 in a Linux container.
                assertTrue(size.width <= NARROWEST_WINDOW_PX,
                        "the export dialog fits the narrowest window"
                                + " the atlas supports at "
                                + (dark ? "dark" : "light") + " x"
                                + scale + ": " + size.width + " px wide");
                assertTrue(size.height <= 480,
                        "and does not need a tall one either: "
                                + size.height + " px");
            }
        }
    }

    @Test
    void theReaderCanReachTheControlsWithAPointer() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "pressing a real control needs a display");

        JFrame[] window = new JFrame[1];
        List<ExportSheet.Request> chosen = new ArrayList<>();
        SwingSession.guarded(() -> {
            JComponent[] holder = new JComponent[1];
            SwingUtilities.invokeAndWait(() -> {
                holder[0] = ExportSheetDialog.content(defaults(),
                        chosen::add, () -> { });
                window[0] = new JFrame("export-dialog");
                window[0].setContentPane(holder[0]);
                window[0].pack();
                window[0].setVisible(true);
            });
            SwingUtilities.invokeAndWait(() -> { });

            // Pressed where a reader would press it, with the
            // premises the shared helper proves: on screen, sized,
            // and the point inside the visible rectangle.
            ReaderInput.click(named(holder[0],
                    ExportSheetDialog.EXPORT_BUTTON));
            assertEquals(1, chosen.size(),
                    "a pointer press on Export exports");
            assertEquals(SheetFormat.SVG, chosen.get(0).format(),
                    "with the defaults a reader who changed nothing"
                            + " sees");
        }, () -> SwingUtilities.invokeAndWait(() -> {
            if (window[0] != null) {
                window[0].dispose();
            }
        }));
    }

    /** Enlarges every font in a tree, without touching UIManager. */
    private static void enlarge(java.awt.Component component, float scale) {
        Font font = component.getFont();
        if (font != null) {
            component.setFont(font.deriveFont(font.getSize2D() * scale));
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                enlarge(child, scale);
            }
        }
    }

    /** Every label's text in the dialog, for asking what is absent. */
    private static List<String> labels(JComponent root) {
        List<String> found = new ArrayList<>();
        collect(root, found);
        return found;
    }

    private static void collect(java.awt.Container root,
                                List<String> found) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof javax.swing.JLabel label
                    && label.getText() != null) {
                found.add(label.getText());
            }
            if (child instanceof javax.swing.AbstractButton button) {
                found.add(button.getText());
            }
            if (child instanceof java.awt.Container container) {
                collect(container, found);
            }
        }
    }
}
