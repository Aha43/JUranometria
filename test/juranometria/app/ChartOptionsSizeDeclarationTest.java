package juranometria.app;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What Chart Options' size declaration means (#380).
 *
 * <p>The capture coordinator treats the declared content size as
 * authoritative and cannot check it, because an unshown window's size
 * is the native peer's to change. So the declaration's truth is
 * proved here: {@link ChartOptionsDialog#ORDINARY_WIDTH} wide, and as
 * tall as the tallest tab laid out at that width, whichever tab is
 * showing.
 *
 * <p>The expected height comes from laying the content out, tab by
 * tab, through the dialog's own measurement - never from the window
 * and never through the coordinator, which would force the declared
 * size and make any declaration look right.
 */
class ChartOptionsSizeDeclarationTest {

    @Test
    void theDeclarationIsTheOrdinaryWidthAndTheTallestTab()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a dialog needs a display");
        java.util.prefs.Preferences node = java.util.prefs.Preferences
                .userRoot().node("juranometria-declaration-"
                        + System.nanoTime());
        JFrame[] frame = new JFrame[1];
        ChartOptionsDialog[] dialog = new ChartOptionsDialog[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new JFrame("declaration");
                dialog[0] = ChartOptionsDialog.packedForStudy(frame[0],
                        "en", new ChartOptionsController(
                                ChartOptionsStore.forNode(node)));
                JComponent content =
                        (JComponent) dialog[0].getContentPane();
                JTabbedPane tabs = ChartOptionsDialog.tabsOf(content);
                Insets chrome = dialog[0].getInsets();
                int inner = ChartOptionsDialog.ORDINARY_WIDTH
                        - chrome.left - chrome.right;

                // The tallest tab at that width, by the dialog's own
                // measurement, with no ceiling in the way.
                int tallest = ChartOptionsDialog.tallestTab(content,
                        inner, Integer.MAX_VALUE / 4);
                // Declared with the LAST tab showing, so a declaration
                // that measured only what happens to be showing, or
                // moved the selection, would be caught.
                int showing = tabs.getTabCount() - 1;
                tabs.setSelectedIndex(showing);
                int ceiling = ChartOptionsDialog.ceilingForUsableHeight(
                        ChartOptionsDialog.usableHeight(
                                dialog[0].getGraphicsConfiguration()));
                assertTrue(tallest + chrome.top + chrome.bottom
                                < ceiling,
                        "the premise: this screen holds the tallest tab,"
                                + " so the height is the tab's and not"
                                + " the screen's cap");

                Dimension said =
                        ChartOptionsDialog.settledContentSize(dialog[0]);

                assertEquals(ChartOptionsDialog.ORDINARY_WIDTH
                                - chrome.left - chrome.right, said.width,
                        "420 wide, less the window's sides");
                assertEquals(tallest, said.height,
                        "as tall as the tallest tab, not the one"
                                + " showing, and not the window with its"
                                + " title bar");
                assertEquals(showing, tabs.getSelectedIndex(),
                        "and declaring moves no tab");
            });
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
}
