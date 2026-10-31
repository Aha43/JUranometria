package juranometria.ui.ecliptic;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.SwingSession;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ecliptic's control on a real, shown surface (Sprint 28, issue
 * #274; the control moved in #675).
 *
 * <p>Until 6.0 the control was an item on the View menu, and this
 * test showed that menu. Since #675 it is the Ecliptic box in Chart
 * Options' Chart checklist, so this shows that checklist's column in
 * a window at the narrowest width Chart Options supports, both
 * themes, enlarged text, nothing clipped - the same owing as before,
 * on the control's new home.
 *
 * <p>It needs a display and says so, in the same way the other
 * window-behaviour tests do; the arrangement and the wiring are
 * covered headlessly elsewhere.
 */
class EclipticMenuSurfaceTest {

    /** The narrowest Chart Options the atlas supports. */
    private static final int NARROW = juranometria.app.ChartOptionsDialog.MINIMUM_WIDTH;

    @Test
    void theBoxFitsAndIsReachableAtEverySizeAndBothThemes()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a shown surface needs a display; the arrangement and"
                        + " the wiring are tested headless");
        for (boolean dark : new boolean[] {false, true}) {
            for (int points : new int[] {12, 18}) {
                showAndCheck(dark, points);
            }
        }
    }

    private void showAndCheck(boolean dark, int points) throws Exception {
        SwingSession.restoring(() -> {
            JFrame[] frame = new JFrame[1];
            javax.swing.JCheckBox[] item = new javax.swing.JCheckBox[1];
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
                    juranometria.app.ChartOptionsControls controls =
                            new juranometria.app.ChartOptionsControls(
                                    new juranometria.app.ChartOptionsController(
                                            juranometria.app.ChartOptionsStore.forNode(
                                                    java.util.prefs.Preferences.userRoot().node(
                                                            "juranometria/test/ecliptic-surface-"
                                                                    + System.nanoTime()))),
                                    () -> false,
                                    EclipticSession.of(new juranometria.ecliptic.EclipticModule(),
                                            EclipticStore.inMemory()),
                                    juranometria.ui.language.InterfaceText.forLanguage("en"));
                    item[0] = controls.eclipticBox();
                    frame[0] = new JFrame("ecliptic surface");
                    frame[0].setContentPane(controls.subjects().stream()
                            .filter(subject -> subject.id().equals("chart"))
                            .findFirst().orElseThrow().column());
                    frame[0].setSize(NARROW, 480);
                    frame[0].setVisible(true);
                });
                SwingUtilities.invokeAndWait(() -> { });
                assertNotNull(item[0], describe(dark, points)
                        + ": the Ecliptic box is in the Chart checklist");
                Dimension needs = item[0].getPreferredSize();
                Rectangle given = item[0].getBounds();
                assertTrue(given.width >= needs.width,
                        describe(dark, points) + ": the box is"
                                + " given the width it needs - "
                                + given.width + " against "
                                + needs.width);
                assertTrue(given.height >= needs.height,
                        describe(dark, points) + ": and the height");
                int textWidth = item[0].getFontMetrics(item[0].getFont())
                        .stringWidth(item[0].getText());
                assertTrue(given.width > textWidth,
                        describe(dark, points) + ": the name is not"
                                + " truncated - " + given.width
                                + " px of box for " + textWidth
                                + " px of text");
                assertTrue(item[0].isShowing(),
                        describe(dark, points) + ": the box is on"
                                + " screen and reachable");
                assertTrue(item[0].isEnabled(),
                        describe(dark, points) + ": and enabled");
                assertTrue(frame[0].getContentPane().getWidth()
                                >= given.x + given.width,
                        describe(dark, points) + ": the column holds"
                                + " it whole");
            } finally {
                if (frame[0] != null) {
                    SwingUtilities.invokeAndWait(frame[0]::dispose);
                }
            }
        });
    }

    private static String describe(boolean dark, int points) {
        return (dark ? "dark" : "light") + " at " + points + " pt in a "
                + NARROW + " px column";
    }
}
