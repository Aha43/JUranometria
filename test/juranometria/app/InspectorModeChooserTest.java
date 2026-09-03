package juranometria.app;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That a reader can read the modes they are being offered
 * (Sprint 24, issue #217).
 *
 * <p>At 320 px and 18 pt the chooser's second button read
 * <strong>"O"</strong> — the control that opens the whole feature,
 * clipped to one letter. Two things had gone wrong: a
 * {@code BoxLayout} column whose children disagreed about their
 * alignment, which left the header and the chooser 171 px wide
 * inside a 288 px panel; and two buttons in a row that was simply
 * too narrow for them.
 *
 * <p>The evidence here is deliberately <strong>measured, not
 * pinned</strong>. A test that asserted "the button is 141 px wide"
 * would pass on the machine it was written on and say nothing
 * anywhere else: fonts differ by platform, look-and-feel and the
 * reader's own text size. So each label is measured in the font the
 * button is actually using, on the panel as it is actually laid out.
 */
class InspectorModeChooserTest {

    /** The sizes a reader can put the Inspector in, and the fonts. */
    private static final int[] WIDTHS = {
            InspectorPanel.MINIMUM_PANEL_WIDTH,
            InspectorPanel.PREFERRED_PANEL_WIDTH,
            420};
    private static final int[] POINTS = {11, 12, 14, 18, 24};

    @Test
    void everyModeIsFullyReadableAtEveryWidthAndTextSize()
            throws Exception {
        Font was = UIManager.getFont("defaultFont");
        List<String> clipped = new ArrayList<>();
        try {
            for (int points : POINTS) {
                UIManager.put("defaultFont",
                        new Font(Font.SANS_SERIF, Font.PLAIN, points));
                for (int width : WIDTHS) {
                    InspectorPanel panel = laidOut(width, points);
                    for (JToggleButton mode : List.of(
                            panel.selectedModeButton(),
                            panel.pageModeButton())) {
                        int needed = mode.getFontMetrics(mode.getFont())
                                .stringWidth(mode.getText());
                        int room = mode.getWidth()
                                - mode.getInsets().left
                                - mode.getInsets().right;
                        if (room < needed) {
                            clipped.add(String.format(
                                    "\"%s\" at %d pt in %d px: %d px of"
                                            + " room for %d px of name",
                                    mode.getText(), points, width, room,
                                    needed));
                        }
                    }
                }
            }
        } finally {
            UIManager.put("defaultFont", was);
        }
        assertEquals(List.of(), clipped,
                "a mode a reader cannot read is a mode they will not"
                        + " find");
    }

    @Test
    void theChooserTakesTwoLinesOnlyWhenOneWillNotDo() throws Exception {
        // Stacking costs vertical space, so it is not the default -
        // it is what happens when the names stop fitting side by
        // side, and nothing else.
        Font was = UIManager.getFont("defaultFont");
        try {
            UIManager.put("defaultFont",
                    new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            InspectorPanel roomy = laidOut(420, 12);
            assertEquals(rowHeight(roomy), chooserHeight(roomy),
                    "side by side while both names fit");

            UIManager.put("defaultFont",
                    new Font(Font.SANS_SERIF, Font.PLAIN, 24));
            InspectorPanel cramped =
                    laidOut(InspectorPanel.MINIMUM_PANEL_WIDTH, 24);
            assertTrue(chooserHeight(cramped) > rowHeight(cramped),
                    "and a line each when they do not: two rows of"
                            + " readable control beat one row of"
                            + " initials");
        } finally {
            UIManager.put("defaultFont", was);
        }
    }

    @Test
    void theColumnIsLaidOutFromTheLeftEdgeInwards() throws Exception {
        // The alignment defect, asserted rather than described: a
        // BoxLayout whose children disagree about their alignment
        // puts the whole column on a fractional axis, and every
        // left-aligned child is indented and squeezed.
        InspectorPanel panel = laidOut(
                InspectorPanel.PREFERRED_PANEL_WIDTH, 18);
        JPanel chooser = (JPanel) panel.pageModeButton().getParent();

        assertEquals(0, chooser.getX(),
                "the chooser starts where its column starts");
        assertTrue(chooser.getWidth() >= panel.getWidth()
                        - panel.getInsets().left - panel.getInsets().right,
                "and uses the whole width it has: " + chooser.getWidth()
                        + " of " + panel.getWidth());
    }

    // ----------------------------------------------------------------

    private static int chooserHeight(InspectorPanel panel) {
        return panel.pageModeButton().getParent().getHeight();
    }

    private static int rowHeight(InspectorPanel panel) {
        return Math.max(panel.selectedModeButton().getHeight(),
                panel.pageModeButton().getHeight());
    }

    /** A panel with a module's view installed, laid out for real. */
    private static InspectorPanel laidOut(int width, int points)
            throws Exception {
        ChartScene scene = Atlas.assembler()
                .assemble(ChartViewState.DEFAULT, 900, 700);
        InspectorPanel[] made = new InspectorPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            made[0] = new InspectorPanel(new SelectionModel(), () -> scene,
                    () -> juranometria.render.ChartOptions.DEFAULTS,
                    chosen -> { });
            JPanel view = new JPanel();
            made[0].showPageView(view);
            made[0].setRequestedVisible(true);
            made[0].setSize(width, 520);
            made[0].doLayout();
            // Twice: the chooser decides its shape from the room it
            // has, and it only has room once it has been laid out.
            made[0].setSize(width, 520);
            layOut(made[0]);
        });
        return made[0];
    }

    private static void layOut(java.awt.Component component) {
        component.doLayout();
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }
}
