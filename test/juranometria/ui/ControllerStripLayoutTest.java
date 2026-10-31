package juranometria.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.SwingSession;
import juranometria.chart.SelectionMode;
import juranometria.chart.SkyPosition;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Controller's control strip (#675, stage 3, ruled on #674): one
 * row whenever it fits, wrapping at group boundaries into as many rows
 * as the actual width needs, recomputed on resize; the groups' order
 * is the keyboard's order on every row; nothing is clipped - at the
 * Controller's default width and at an external display's, in both
 * languages, both themes and at larger text.
 */
class ControllerStripLayoutTest {

    private static final int NARROW = juranometria.ui.companion.CompanionWindow.DEFAULT_WIDTH;
    private static final int WIDE = 760;

    private static final List<String> ORDER = List.of("Zoom in", "Zoom out",
            "Lock zoom", "Fewer stars", "More stars", "Reset view", "Inspector",
            "Accumulate selection");

    private static JComponent section(InterfaceText said) {
        ChartViewController controller = new ChartViewController();
        controller.recenter(new SkyPosition(10.684708, 41.268750), 8.0);
        ChartControls controls = new ChartControls(controller,
                new SearchField(juranometria.app.Atlas.search(),
                        juranometria.app.Atlas.assembler(), controller, said),
                new InspectorToggle(), new SelectionMode(), new ZoomLock(),
                new ChartActions(controller), said);
        return controls.inCompanion();
    }

    private static List<AbstractButton> buttons(Container root) {
        List<AbstractButton> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof AbstractButton button) {
                found.add(button);
            } else if (child instanceof Container inner) {
                found.addAll(buttons(inner));
            }
        }
        return found;
    }

    private static int rows(JComponent strip) {
        java.util.Set<Integer> tops = new java.util.TreeSet<>();
        for (Component group : strip.getComponents()) {
            tops.add(group.getY());
        }
        return tops.size();
    }

    @Test
    void theGroupsKeepTheKeyboardsOrderAndStayFocusableWhateverTheWidth() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a laid-out strip needs a display");
        for (String language : List.of("en", "nb-NO")) {
            for (boolean dark : new boolean[] {false, true}) {
                for (int points : new int[] {12, 18}) {
                    for (int width : new int[] {NARROW, WIDE}) {
                        check(language, dark, points, width);
                    }
                }
            }
        }
    }

    private static void check(String language, boolean dark, int points, int width)
            throws Exception {
        String what = language + (dark ? " dark " : " light ") + points + " pt at " + width + " px";
        SwingSession.restoring(() -> {
            JFrame[] frame = new JFrame[1];
            JComponent[] section = new JComponent[1];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    if (dark) {
                        com.formdev.flatlaf.FlatDarkLaf.setup();
                    } else {
                        com.formdev.flatlaf.FlatLightLaf.setup();
                    }
                    Font base = UIManager.getFont("defaultFont");
                    UIManager.put("defaultFont", base == null
                            ? new Font(Font.SANS_SERIF, Font.PLAIN, points)
                            : base.deriveFont((float) points));
                    section[0] = section(InterfaceText.forLanguage(language));
                    frame[0] = new JFrame("strip");
                    frame[0].setContentPane(section[0]);
                    frame[0].pack();
                    frame[0].setSize(width, frame[0].getHeight() + 200);
                    frame[0].setVisible(true);
                });
                SwingUtilities.invokeAndWait(() -> { });
                SwingUtilities.invokeAndWait(() -> {
                    section[0].revalidate();
                    frame[0].validate();
                });
                SwingUtilities.invokeAndWait(() -> { });
                JComponent strip = ChartControls.strip(section[0]);
                assertTrue(strip != null, what + ": the section holds the strip");
                // The order, whatever row each group landed on.
                List<String> names = new ArrayList<>();
                for (AbstractButton button : buttons(strip)) {
                    names.add(button.getAccessibleContext().getAccessibleName());
                    assertTrue(button.isFocusable(), what + ": " + names.get(names.size() - 1)
                            + " is reachable by keyboard");
                }
                InterfaceText said = InterfaceText.forLanguage(language);
                List<String> expected = List.of(said.say("toolbar.zoomIn.a11y"),
                        said.say("toolbar.zoomOut.a11y"), said.say("toolbar.zoomLock.a11y"),
                        said.say("toolbar.fewerStars.a11y"), said.say("toolbar.moreStars.a11y"),
                        said.say("toolbar.reset.a11y"), said.say("toolbar.inspector.a11y"),
                        said.say("toolbar.accumulate.a11y"));
                assertEquals(expected, names, what + ": the groups' order is the keyboard's");
                // Nothing clipped: every group inside the strip's width,
                // and the strip as tall as its rows.
                int bottom = 0;
                for (Component group : strip.getComponents()) {
                    assertTrue(group.getX() + group.getWidth() <= strip.getWidth(),
                            what + ": a group is cut by the edge - " + group.getBounds()
                                    + " in " + strip.getWidth());
                    bottom = Math.max(bottom, group.getY() + group.getHeight());
                }
                assertTrue(strip.getHeight() >= bottom,
                        what + ": the strip is given the height its rows take - "
                                + strip.getHeight() + " for rows to " + bottom);
                int rows = rows(strip);
                if (width == WIDE) {
                    assertEquals(1, rows, what + ": one row on a wide display");
                } else {
                    assertTrue(rows >= 1 && rows <= 3, what + ": " + rows + " rows at the"
                            + " Controller's default width");
                }
            } finally {
                if (frame[0] != null) {
                    SwingUtilities.invokeAndWait(frame[0]::dispose);
                }
            }
        });
    }

    @Test
    void englishAtDefaultWidthAndLargerTextWrapsIntoMoreRowsNotFewer() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        int[] rows = new int[2];
        int i = 0;
        for (int points : new int[] {12, 18}) {
            int index = i++;
            SwingSession.restoring(() -> {
                JFrame[] frame = new JFrame[1];
                JComponent[] section = new JComponent[1];
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        com.formdev.flatlaf.FlatLightLaf.setup();
                        Font base = UIManager.getFont("defaultFont");
                        UIManager.put("defaultFont", base == null
                                ? new Font(Font.SANS_SERIF, Font.PLAIN, points)
                                : base.deriveFont((float) points));
                        section[0] = section(InterfaceText.forLanguage("en"));
                        frame[0] = new JFrame("strip");
                        frame[0].setContentPane(section[0]);
                        frame[0].pack();
                        frame[0].setSize(NARROW, frame[0].getHeight() + 200);
                        frame[0].setVisible(true);
                    });
                    SwingUtilities.invokeAndWait(() -> { });
                    rows[index] = rows(ChartControls.strip(section[0]));
                } finally {
                    if (frame[0] != null) {
                        SwingUtilities.invokeAndWait(frame[0]::dispose);
                    }
                }
            });
        }
        assertTrue(rows[1] >= rows[0], "larger text wraps into at least as many rows: "
                + rows[0] + " at 12 pt, " + rows[1] + " at 18 pt");
    }
}
