package juranometria.app;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class ChartOptionsDialogHeightTest {

    /** A 768 px display with a 40 px taskbar. */
    private static final int SHORT_SCREEN = 728;

    @Test
    void theCeilingFollowsTheScreenRatherThanAConstant() {
        // The rule itself, before any window exists: taller screens
        // give taller dialogs, and every ceiling leaves the window's
        // own decoration room inside the usable area.
        for (int usable : new int[] {600, SHORT_SCREEN, 900, 1400}) {
            int ceiling =
                    ChartOptionsDialog.ceilingForUsableHeight(usable);
            assertTrue(ceiling <= usable
                            - ChartOptionsDialog.WINDOW_CHROME
                    || ceiling == ChartOptionsDialog.MINIMUM_CEILING,
                    "a " + usable + " px screen must not be given a "
                            + ceiling + " px dialog");
            assertTrue(ceiling >= ChartOptionsDialog.MINIMUM_CEILING,
                    "and never a dialog too short to hold its own tabs");
        }
        assertTrue(ChartOptionsDialog.ceilingForUsableHeight(1400)
                        > ChartOptionsDialog
                                .ceilingForUsableHeight(SHORT_SCREEN),
                "a taller screen earns a taller dialog");

        // The failure this replaces: the old constant.
        assertTrue(ChartOptionsDialog
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

    @Test
    void andAFontSomeoneElseChoseComesBackExactly() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "installing a look and feel needs a toolkit");
        // The other half of restoring: the dialog clears the
        // `defaultFont` override on its way in, so a session that had
        // set one would have had it deleted rather than given back.
        // A font nobody would choose by accident, so passing cannot
        // mean the look and feel happened to supply the same one.
        javax.swing.plaf.FontUIResource distinctive =
                new javax.swing.plaf.FontUIResource("Serif",
                        java.awt.Font.BOLD, 21);
        restoringTheme(() -> {
            SwingUtilities.invokeAndWait(
                    () -> UIManager.put("defaultFont", distinctive));

            restoringTheme(this::onAShortScreen);

            assertEquals(distinctive, UIManager.get("defaultFont"),
                    "the override this session had chosen is back,"
                            + " exactly");
        });

        // And the other direction: where nothing was chosen, nothing
        // is invented. A freshly installed theme publishes a default
        // font of its own, and reading the key cannot tell that apart
        // from an override - both answer the same. What tells them
        // apart is a change of theme: an override survives one and
        // pins the font against it, and a theme's own value does not.
        // Metal declares no default font, so it is the question put
        // plainly.
        restoringTheme(() -> {
            SwingUtilities.invokeAndWait(
                    () -> juranometria.app.UiTheme.apply(false));
            assertNotNull(UIManager.get("defaultFont"),
                    "the theme publishes a font of its own, which is"
                            + " what makes this worth checking");

            restoringTheme(this::onAShortScreen);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(
                            new javax.swing.plaf.metal.MetalLookAndFeel());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            assertNull(UIManager.get("defaultFont"),
                    "an override invented by the cleanup would have"
                            + " survived this change of theme");
        });
    }

    @Test
    void andSoDoesAChosenFontThatMatchesTheThemeAnyway() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "installing a look and feel needs a toolkit");
        // The case a value comparison cannot see: a session that
        // chose the font the theme was already using. It reads back
        // identically to no choice at all - and it is still a choice,
        // because it outlives the theme it was made under, which is
        // exactly what choosing it was for.
        restoringTheme(() -> {
            SwingUtilities.invokeAndWait(
                    () -> juranometria.app.UiTheme.apply(false));
            java.awt.Font themes =
                    (java.awt.Font) UIManager.get("defaultFont");
            javax.swing.plaf.FontUIResource sameButChosen =
                    new javax.swing.plaf.FontUIResource(themes.getFamily(),
                            themes.getStyle(), themes.getSize());
            assertEquals(themes, sameButChosen,
                    "the point of this test: the chosen font and the"
                            + " theme's are equal");
            SwingUtilities.invokeAndWait(
                    () -> UIManager.put("defaultFont", sameButChosen));

            restoringTheme(this::onAShortScreen);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(
                            new javax.swing.plaf.metal.MetalLookAndFeel());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            assertEquals(sameButChosen, UIManager.get("defaultFont"),
                    "a chosen font that matched the theme is still a"
                            + " choice, and must survive a change of"
                            + " theme");
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
        Object inheritedFont = fontOverride();
        try {
            body.run();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                // The look and feel first, then the exact override
                // that was found - a font somebody chose, or nothing
                // at all. Clearing it unconditionally, which is what
                // this did at first, deletes a choice the session had
                // made rather than restoring it.
                if (inherited != null) {
                    try {
                        UIManager.setLookAndFeel(inherited);
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "could not restore " + inherited.getName(),
                                e);
                    }
                }
                UIManager.put("defaultFont", inheritedFont);
            });
        }
    }

    /**
     * The `defaultFont` somebody has chosen, or null when nobody has.
     *
     * <p>Reading the value cannot answer this: a look and feel
     * publishes a default font of its own, and an override laid over
     * it reads back the same way. Comparing values cannot answer it
     * either, because a session is entitled to choose the font the
     * theme already uses - that is still a choice, and it still
     * outlives the theme it was made under.
     *
     * <p>What answers it is presence. {@code UIManager}'s own table
     * holds overrides only; the look and feel's values live in tables
     * behind it, and {@code containsKey} does not consult them.
     * Nothing here is written, so asking costs nothing.
     */
    private static Object fontOverride() {
        return UIManager.getDefaults().containsKey("defaultFont")
                ? UIManager.get("defaultFont")
                : null;
    }

    /**
     * The production dialog's content in a window told to believe in
     * a screen this machine does not have. The dialog sizes itself
     * through the very method it uses when a reader opens it.
     */
    private static JFrame shortScreenDialog(double textScale) {
        UIManager.put("defaultFont", null);
        juranometria.app.UiTheme.apply(false);
        if (textScale != 1.0) {
            java.awt.Font base = UIManager.getFont("Label.font");
            UIManager.put("defaultFont", base.deriveFont(
                    (float) (base.getSize2D() * textScale)));
            juranometria.app.UiTheme.apply(false);
        }
        java.util.prefs.Preferences scratch =
                java.util.prefs.Preferences.userRoot()
                        .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(scratch));
            JFrame frame = new JFrame("Chart Options");
            frame.setContentPane(
                    ChartOptionsDialog.contentForStudy(controller));
            ChartOptionsDialog.sizeToScreen(frame,
                    ChartOptionsDialog.ORDINARY_WIDTH, SHORT_SCREEN);
            frame.setLocation(40, 40);
            frame.setVisible(true);
            return frame;
        } finally {
            try {
                scratch.removeNode();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Whether the dialog's action buttons are still where a reader
     * can use them: inside the window, inside the screen's usable
     * area, and reachable by Tab. A scroll bar answers a tab that is
     * too tall; nothing answers an OK button under a taskbar.
     */
    private static String actionButtons(JFrame frame) {
        Rectangle usable = frame.getGraphicsConfiguration().getBounds();
        java.awt.Insets screen = java.awt.Toolkit.getDefaultToolkit()
                .getScreenInsets(frame.getGraphicsConfiguration());
        usable = new Rectangle(usable.x + screen.left, usable.y + screen.top,
                usable.width - screen.left - screen.right,
                usable.height - screen.top - screen.bottom);
        java.util.List<String> lost = new java.util.ArrayList<>();
        java.util.List<String> unreachable = new java.util.ArrayList<>();
        for (String name
                : java.util.List.of("OK", "Cancel", "Restore Defaults")) {
            javax.swing.AbstractButton button =
                    button(frame.getContentPane(), name);
            if (button == null || !button.isShowing()) {
                lost.add(name);
                continue;
            }
            Rectangle onScreen = new Rectangle(button.getLocationOnScreen(),
                    button.getSize());
            if (!frame.getBounds().contains(onScreen)
                    || !usable.contains(onScreen)) {
                lost.add(name);
            }
            if (!reachableByTab(frame, button)) {
                unreachable.add(name);
            }
        }
        if (lost.isEmpty() && unreachable.isEmpty()) {
            return "on screen, tab-reachable";
        }
        return "**" + (lost.isEmpty() ? "" : "off screen: " + lost)
                + (unreachable.isEmpty() ? ""
                        : " no keyboard route: " + unreachable) + "**";
    }

    private static boolean reachableByTab(JFrame frame,
                                          java.awt.Component target) {
        java.awt.FocusTraversalPolicy policy =
                frame.getFocusTraversalPolicy();
        if (policy == null) {
            return false;
        }
        java.awt.Component at = policy.getFirstComponent(frame);
        for (int step = 0; at != null && step < 200; step++) {
            if (at == target) {
                return true;
            }
            at = policy.getComponentAfter(frame, at);
        }
        return false;
    }

    private static javax.swing.AbstractButton button(
            java.awt.Container container, String text) {
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof javax.swing.AbstractButton button
                    && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof java.awt.Container inner) {
                javax.swing.AbstractButton found = button(inner, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
                    JFrame frame = shortScreenDialog(textScale);
                    window[0] = frame;
                    measured[0] = frame.getHeight();
                    measured[1] = ChartOptionsDialog
                            .ceilingForUsableHeight(SHORT_SCREEN);
                    verdict[0] = actionButtons(frame);
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
                            ChartOptionsDialog.tabsOf(window[0].getContentPane());
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
