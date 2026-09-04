package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.app.TargetRetirement;
import juranometria.chart.ChartViewState;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The black-sky acceptance journey (Sprint 26, issue #246), through
 * the production paths: the real View menu, the real Chart tab and
 * its Black sky control pressed as a reader presses them, the
 * production controller, persistence, and the real chart component's
 * own painted pixels. The switch is repaint-only, Cancel reverts,
 * OK persists across a restart, Restore Defaults returns to white
 * paper, and neither direction of the theme/chart boundary leaks:
 * choosing a ground changes no chrome, and changing the theme
 * changes no ground. Requires a display; aborted by assumption on
 * headless runners, where the renderer, store and dialog layers are
 * fully tested headless.
 */
class BlackSkyJourneyTest {

    @Test
    void chooseTheBlackSkyCancelItConfirmItAndKeepItThroughThemes()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the dialog journey needs a display");
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        JFrame[] frame = new JFrame[1];
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            ChartOptionsController options =
                    new ChartOptionsController(store);
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartComponent[] chart = new ChartComponent[1];

            SwingUtilities.invokeAndWait(() -> {
                chart[0] = new ChartComponent(Atlas.assembler());
                navigation.onChange(chart[0]::setViewState);
                // The production wiring, not a hand copy of it.
                TargetRetirement.connect(options, chart[0], navigation);
                frame[0] = new JFrame("black-sky-journey");
                frame[0].setJMenuBar(AppMenuBar.create(null,
                        () -> ChartOptionsDialog.open(frame[0], options),
                        () -> { }));
                frame[0].setLayout(new java.awt.BorderLayout());
                frame[0].add(chart[0], java.awt.BorderLayout.CENTER);
                frame[0].pack();
                frame[0].setSize(900, 700);
                frame[0].setVisible(true);
            });
            flush();

            // The page really paints, and its ground is paper white:
            // the before of the before/after, from the component's
            // own pixels.
            assertEquals(ChartPalette.WHITE_PAPER.ground().getRGB(),
                    pageGround(chart[0]),
                    "the journey starts on the released white page");

            // Through the real View menu (the recorded menu-item
            // convention), to the Chart tab, to the control - each
            // by the route a reader takes.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0)
                            .doClick());
            flush();
            JDialog dialog = optionsDialog();
            assertNotNull(dialog, "the View menu opened the dialog");
            juranometria.chart.ChartScene sceneBefore = chart[0].scene();
            ChartViewState placeBefore = navigation.state();
            var lookAndFeel = javax.swing.UIManager.getLookAndFeel();
            ReaderInput.chooseTab(tabsIn(dialog.getContentPane()),
                    "Chart");
            ReaderInput.click(box(dialog.getContentPane(), "Black sky"));

            assertEquals(ChartPalette.BLACK_SKY,
                    options.options().palette(),
                    "the choice previews live");
            assertSame(sceneBefore, chart[0].scene(),
                    "the ground is repaint-only: the very same scene"
                            + " object, no reassembly, no query");
            assertSame(placeBefore, navigation.state(),
                    "choosing the ground never moves the chart");
            assertTrue(lookAndFeel
                            == javax.swing.UIManager.getLookAndFeel(),
                    "and never alters application chrome");
            assertEquals(ChartPalette.BLACK_SKY.ground().getRGB(),
                    pageGround(chart[0]),
                    "the component's own pixels wear the black sky");

            // Cancel is a real revert: white paper back, nothing
            // persisted.
            ReaderInput.click(button(dialog.getContentPane(), "Cancel"));
            flush();
            assertEquals(ChartPalette.WHITE_PAPER,
                    options.options().palette(),
                    "Cancel reverts the preview");
            assertEquals(ChartPalette.WHITE_PAPER,
                    store.load().palette(),
                    "and persisted nothing");
            assertEquals(ChartPalette.WHITE_PAPER.ground().getRGB(),
                    pageGround(chart[0]),
                    "the page is paper again");

            // Choose it for real: reopen, choose, OK. A restarted
            // session reads the black sky back.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0)
                            .doClick());
            flush();
            JDialog again = optionsDialog();
            ReaderInput.chooseTab(tabsIn(again.getContentPane()),
                    "Chart");
            ReaderInput.click(box(again.getContentPane(), "Black sky"));
            ReaderInput.click(button(again.getContentPane(), "OK"));
            flush();
            assertEquals(ChartPalette.BLACK_SKY, store.load().palette(),
                    "OK persisted the reader's sky");
            assertEquals(ChartPalette.BLACK_SKY,
                    new ChartOptionsController(store).options()
                            .palette(),
                    "a restarted session reads exactly what was"
                            + " confirmed");

            // The other direction of the boundary: the application
            // theme, either way, never alters the chart choice or
            // the chart's own pixels. Look-and-feel is process-wide
            // state, restored by the shared guard (#224).
            juranometria.app.SwingSession.restoring(() -> {
                SwingUtilities.invokeAndWait(() -> {
                    juranometria.app.UiTheme.apply(true);
                    com.formdev.flatlaf.FlatLaf.updateUI();
                });
                flush();
                assertEquals(ChartPalette.BLACK_SKY,
                        options.options().palette(),
                        "a dark chrome does not touch the chart"
                                + " choice");
                assertEquals(ChartPalette.BLACK_SKY.ground().getRGB(),
                        pageGround(chart[0]),
                        "nor the chart's own ground");
                SwingUtilities.invokeAndWait(() -> {
                    juranometria.app.UiTheme.apply(false);
                    com.formdev.flatlaf.FlatLaf.updateUI();
                });
                flush();
                assertEquals(ChartPalette.BLACK_SKY,
                        options.options().palette(),
                        "and a light chrome does not either");
                assertEquals(ChartPalette.BLACK_SKY.ground().getRGB(),
                        pageGround(chart[0]));
            });

            // Restore Defaults, driven for real, ends the journey on
            // the released white-paper chart.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0)
                            .doClick());
            flush();
            JDialog last = optionsDialog();
            ReaderInput.click(button(last.getContentPane(),
                    "Restore Defaults"));
            ReaderInput.click(button(last.getContentPane(), "OK"));
            flush();
            assertEquals(ChartOptions.DEFAULTS, store.load(),
                    "Restore Defaults returns to the released chart,"
                            + " white paper included");
            assertEquals(ChartPalette.WHITE_PAPER.ground().getRGB(),
                    pageGround(chart[0]),
                    "and the page shows it");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
                if (frame[0] != null) {
                    frame[0].dispose();
                }
            });
            node.removeNode();
        }
    }

    /**
     * The ground of the component's own painting: the commonest
     * pixel inside the page area - the sky itself on either ground,
     * whatever mark happens to sit at any single point (the default
     * page's centre is M31's own pale wash, which is how a
     * centre-pixel probe failed first). Painted through
     * {@code paint}, so the stale-geometry guard is honest: a
     * skipped frame would come back as the image's initial zero and
     * fail the white-paper assertion first.
     */
    private static int pageGround(ChartComponent chart)
            throws Exception {
        int[] pixel = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage shot = new BufferedImage(chart.getWidth(),
                    chart.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = shot.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
            java.util.Map<Integer, Integer> census =
                    new java.util.HashMap<>();
            int pageTop = chart.pageOffsetY();
            int pageBottom = Math.min(shot.getHeight(), pageTop
                    + chart.scene().viewport().heightPx());
            for (int y = Math.max(0, pageTop); y < pageBottom; y++) {
                for (int x = 0; x < shot.getWidth(); x++) {
                    census.merge(shot.getRGB(x, y), 1, Integer::sum);
                }
            }
            pixel[0] = census.entrySet().stream()
                    .max(java.util.Map.Entry.comparingByValue())
                    .orElseThrow().getKey();
        });
        return pixel[0];
    }

    private static javax.swing.JTabbedPane tabsIn(
            java.awt.Component component) {
        if (component instanceof javax.swing.JTabbedPane tabs) {
            return tabs;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = tabsIn(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JDialog optionsDialog() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private static javax.swing.JCheckBox box(java.awt.Component component,
                                             String accessibleName) {
        if (component instanceof javax.swing.JCheckBox checkBox
                && accessibleName.equals(checkBox
                        .getAccessibleContext().getAccessibleName())) {
            return checkBox;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = box(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static javax.swing.JButton button(java.awt.Component component,
                                              String accessibleName) {
        if (component instanceof javax.swing.JButton buttonComponent
                && accessibleName.equals(buttonComponent
                        .getAccessibleContext().getAccessibleName())) {
            return buttonComponent;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = button(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }
}
