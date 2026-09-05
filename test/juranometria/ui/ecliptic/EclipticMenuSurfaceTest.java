package juranometria.ui.ecliptic;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
import juranometria.app.SwingSession;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ecliptic's control on a real, shown menu (Sprint 28, issue
 * #274).
 *
 * <p>The gate chose the View menu partly because a popup is sized by
 * its own content rather than by the window it hangs from, so a
 * narrow window cannot truncate it - the property that disqualified
 * the Inspector for place-and-time at its 240 px floor. The gate's
 * own images were arrangement mock-ups in a panel, because a popup
 * paints nothing until it is shown, and the review made #274 owe
 * this: a real shown menu, both themes, enlarged text, nothing
 * clipped.
 *
 * <p>So this shows the menu. It needs a display and says so, in the
 * same way the other window-behaviour tests do; the arrangement and
 * the wiring are covered headlessly elsewhere.
 */
class EclipticMenuSurfaceTest {

    /** The narrowest window the atlas supports. */
    private static final int NARROW = 640;

    @Test
    void theItemFitsAndIsReachableAtEverySizeAndBothThemes()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a shown menu needs a display; the arrangement and the"
                        + " wiring are tested headless");

        for (boolean dark : new boolean[] {false, true}) {
            for (int points : new int[] {12, 18}) {
                showAndCheck(dark, points);
            }
        }
    }

    private void showAndCheck(boolean dark, int points) throws Exception {
        SwingSession.restoring(() -> {
            JFrame[] frame = new JFrame[1];
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

                    frame[0] = new JFrame("menu surface");
                    frame[0].setJMenuBar(AppMenuBar.create(null, null,
                            () -> { }, () -> { }, () -> { }, () -> { },
                            () -> { }));
                    // The narrowest window the atlas supports: the
                    // point of the menu is that this does not decide
                    // the item's width.
                    frame[0].setSize(NARROW, 480);
                    frame[0].setVisible(true);
                });
                SwingUtilities.invokeAndWait(() -> { });

                JMenuBar bar = frame[0].getJMenuBar();
                JCheckBoxMenuItem item = AppMenuBar.eclipticItem(bar);
                assertNotNull(item, describe(dark, points)
                        + ": the ecliptic item is on the bar");

                JMenu view = viewMenu(bar);
                SwingUtilities.invokeAndWait(() ->
                        view.setPopupMenuVisible(true));
                SwingUtilities.invokeAndWait(() -> { });
                try {
                    assertTrue(view.getPopupMenu().isShowing(),
                            describe(dark, points)
                                    + ": the View menu is open");

                    // Nothing clipped: the item is given at least the
                    // width its own text and its tick need. This is
                    // the claim the gate could not make from a panel
                    // mock-up.
                    Dimension needs = item.getPreferredSize();
                    Rectangle given = item.getBounds();
                    assertTrue(given.width >= needs.width,
                            describe(dark, points) + ": the item is"
                                    + " given the width it needs - "
                                    + given.width + " against "
                                    + needs.width);
                    assertTrue(given.height >= needs.height,
                            describe(dark, points) + ": and the height");

                    int textWidth = item.getFontMetrics(item.getFont())
                            .stringWidth(item.getText());
                    assertTrue(given.width > textWidth,
                            describe(dark, points) + ": the name is not"
                                    + " truncated - " + given.width
                                    + " px of item for " + textWidth
                                    + " px of text");

                    // And the window did not decide it: the popup is
                    // wider than nothing and the item sits inside it,
                    // on a window narrower than many dialogs.
                    assertTrue(item.isShowing(),
                            describe(dark, points) + ": the item is on"
                                    + " screen and reachable");
                    assertTrue(item.isEnabled(),
                            describe(dark, points) + ": and enabled");
                    assertTrue(view.getPopupMenu().getWidth()
                                    >= given.width,
                            describe(dark, points) + ": the popup holds"
                                    + " it whole");
                } finally {
                    SwingUtilities.invokeAndWait(() ->
                            view.setPopupMenuVisible(false));
                }
            } finally {
                if (frame[0] != null) {
                    SwingUtilities.invokeAndWait(frame[0]::dispose);
                }
            }
        });
    }

    private static JMenu viewMenu(JMenuBar bar) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            if ("View".equals(bar.getMenu(i).getText())) {
                return bar.getMenu(i);
            }
        }
        throw new IllegalStateException("the bar has a View menu");
    }

    private static String describe(boolean dark, int points) {
        return (dark ? "dark" : "light") + " at " + points + " pt on a "
                + NARROW + " px window";
    }
}
