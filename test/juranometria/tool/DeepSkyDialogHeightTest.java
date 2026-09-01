package juranometria.tool;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The proposed Chart Options dialog fits the screen it opens on
 * (Sprint 21, issue #184; gate review P1).
 *
 * <p>The first version of the gate capped the dialog at a constant
 * 780 px, measured on a tall display. On a 768 px screen with a
 * taskbar that puts the OK button under the taskbar, and a reader
 * cannot resize what they cannot reach. A scroll bar answers a tab
 * that is too tall; nothing answers an action button that is off the
 * screen.
 *
 * <p>So the ceiling is derived from the screen's usable bounds, and
 * these tests hold it to that on a screen this machine may not have.
 */
class DeepSkyDialogHeightTest {

    /** A 768 px display with a 40 px taskbar. */
    private static final int SHORT_SCREEN =
            DeepSkyVocabularyMockupMain.SHORT_SCREEN;

    @Test
    void theCeilingFollowsTheScreenRatherThanAConstant() {
        // The rule itself, before any window exists: taller screens
        // give taller dialogs, and every ceiling leaves the window's
        // own decoration room inside the usable area.
        for (int usable : new int[] {600, SHORT_SCREEN, 900, 1400}) {
            int ceiling =
                    DeepSkyVocabularyMockupMain.ceilingForUsableHeight(usable);
            assertTrue(ceiling <= usable
                            - DeepSkyVocabularyMockupMain.WINDOW_CHROME
                    || ceiling == DeepSkyVocabularyMockupMain.MINIMUM_CEILING,
                    "a " + usable + " px screen must not be given a "
                            + ceiling + " px dialog");
            assertTrue(ceiling >= DeepSkyVocabularyMockupMain.MINIMUM_CEILING,
                    "and never a dialog too short to hold its own tabs");
        }
        assertTrue(DeepSkyVocabularyMockupMain.ceilingForUsableHeight(1400)
                        > DeepSkyVocabularyMockupMain
                                .ceilingForUsableHeight(SHORT_SCREEN),
                "a taller screen earns a taller dialog");

        // The failure this replaces: the old constant.
        assertTrue(DeepSkyVocabularyMockupMain
                        .ceilingForUsableHeight(SHORT_SCREEN) < 780,
                "the cap that shipped in the first gate would not have"
                        + " fitted this screen");
    }

    @Test
    void onAShortScreenTheActionButtonsStayReachable() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the dialog's height is a question about a real window");
        restoringTheme(this::onAShortScreen);
    }

    @Test
    void andItLeavesTheThemeAsItFoundIt() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "installing a look and feel needs a toolkit");
        // The proof that the cleanup above is a restoration and not a
        // preference. The trap is to end by applying the light theme,
        // which passes every run that happened to start light and
        // quietly hands the next test a session state nobody chose
        // (Sprint 19 review). So this stands in for a session that was
        // using the dark theme, and requires it back afterwards.
        restoringTheme(() -> {
            SwingUtilities.invokeAndWait(
                    () -> juranometria.app.UiTheme.apply(true));
            String stoodIn = UIManager.getLookAndFeel().getName();
            float stoodInText = UIManager.getFont("Label.font").getSize2D();

            restoringTheme(this::onAShortScreen);

            assertEquals(stoodIn, UIManager.getLookAndFeel().getName(),
                    "the dialog put back the theme it found");
            assertEquals(stoodInText,
                    UIManager.getFont("Label.font").getSize2D(), 0.01f,
                    "and left no enlarged font behind");
        });
    }

    /** Something a test does that disturbs the global look and feel. */
    private interface Body {
        void run() throws Exception;
    }

    /**
     * Runs a body and puts the look and feel and default font back the
     * way they were - whatever they were.
     *
     * <p>Opening the dialog installs both, and both are global to the
     * JVM the whole suite shares. The state is captured before the
     * body can fail, so a failure inside it cannot leak a theme into
     * whatever runs next.
     */
    private static void restoringTheme(Body body) throws Exception {
        javax.swing.LookAndFeel inherited = UIManager.getLookAndFeel();
        try {
            body.run();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                // Cleared rather than put back: the enlarged font is
                // an override laid over the look and feel's own, and
                // reinstalling the look and feel restores what it
                // wanted. Nothing else in the suite writes this key.
                UIManager.put("defaultFont", null);
                if (inherited != null) {
                    try {
                        UIManager.setLookAndFeel(inherited);
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "could not restore " + inherited.getName(),
                                e);
                    }
                }
            });
        }
    }

    private void onAShortScreen() throws Exception {
        // The dialog is built exactly as the study builds it, told to
        // believe in a screen shorter than this machine's.
        for (double textScale : new double[] {1.0, 1.5}) {
            JFrame[] window = new JFrame[1];
            String[] verdict = new String[1];
            int[] measured = new int[2];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    JFrame frame = DeepSkyVocabularyMockupMain.open(false,
                            DeepSkyVocabularyMockupMain.ORDINARY_WIDTH,
                            textScale, true, "Deep sky", SHORT_SCREEN);
                    window[0] = frame;
                    measured[0] = frame.getHeight();
                    measured[1] = DeepSkyVocabularyMockupMain
                            .ceilingForUsableHeight(SHORT_SCREEN);
                    verdict[0] =
                            DeepSkyVocabularyMockupMain.actionButtons(frame);
                });

                assertTrue(measured[0] <= measured[1],
                        "at " + textScale + "x the dialog grew to "
                                + measured[0] + " px on a screen that"
                                + " allows " + measured[1]);
                assertEquals("on screen, tab-reachable", verdict[0],
                        "OK, Cancel and Restore Defaults at " + textScale
                                + "x text");

                // The tab strip is the other thing scrolling cannot
                // rescue: a reader who cannot see the tabs cannot
                // reach three quarters of the dialog.
                SwingUtilities.invokeAndWait(() -> {
                    javax.swing.JTabbedPane tabs =
                            DeepSkyVocabularyMockupMain.tabsOf(window[0]);
                    Rectangle strip = new Rectangle(
                            tabs.getLocationOnScreen(),
                            new java.awt.Dimension(tabs.getWidth(), 1));
                    assertTrue(window[0].getBounds().contains(strip),
                            "the tab strip is inside the window");
                    assertTrue(tabs.getTabCount() == 4
                                    && tabs.getBoundsAt(0) != null,
                            "and all four tabs are laid out");
                });
            } finally {
                JFrame frame = window[0];
                if (frame != null) {
                    SwingUtilities.invokeAndWait(frame::dispose);
                }
            }
        }
    }
}
