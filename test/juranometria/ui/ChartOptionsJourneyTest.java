package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.chart.ChartViewState;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 12 acceptance journey through the production paths: the
 * real search field, the real View menu action, the real dialog
 * controls, the production options controller and persistence
 * boundary, and the real chart component - proving the reader can
 * choose the chart without moving it, that Home and Restore Defaults
 * keep their distinct scopes, and that a restart honours exactly what
 * was confirmed. Requires a display; aborted by assumption on
 * headless runners, where the pipeline and content layers are fully
 * tested headless.
 */
class ChartOptionsJourneyTest {

    @Test
    void chooseTheChartExploreAndComeHome() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the dialog journey needs a display");
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        JFrame[] frame = new JFrame[1];
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            ChartOptionsController options = new ChartOptionsController(store);
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartComponent[] chart = new ChartComponent[1];
            SearchField[] search = new SearchField[1];

            SwingUtilities.invokeAndWait(() -> {
                chart[0] = new ChartComponent(Atlas.assembler());
                navigation.onChange(chart[0]::setViewState);
                options.onChange(chart[0]::setChartOptions);
                search[0] = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                frame[0] = new JFrame("options-journey");
                frame[0].setJMenuBar(AppMenuBar.create(null,
                        () -> ChartOptionsDialog.open(frame[0], options),
                        () -> { }));
                frame[0].setLayout(new java.awt.BorderLayout());
                frame[0].add(new AtlasToolbar(navigation, search[0]),
                        java.awt.BorderLayout.NORTH);
                frame[0].add(chart[0], java.awt.BorderLayout.CENTER);
                frame[0].pack();
                frame[0].setSize(900, 700);
                // Shown, because the premises insist: the old back
                // doors typed into a window no reader could see
                // (#243).
                frame[0].setVisible(true);
            });
            flush();

            // Released defaults, then out to wide Orion via real search.
            assertEquals(ChartOptions.DEFAULTS, options.options());
            ReaderInput.typeAndEnter(search[0], "m 42");
            SwingUtilities.invokeAndWait(() -> {
                while (navigation.state().fieldWidthDegrees() < 36.0) {
                    navigation.zoomOut();
                }
            });
            flush();
            ChartViewState wideOrion = navigation.state();
            assertEquals("NGC 1976", wideOrion.targetIdentity());

            // Open Chart Options through the real View menu action and
            // hide labels, then the deep-sky layer: the chart must not
            // move, the title must stay the target's, and nothing may
            // reassemble or query.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog dialog = optionsDialog();
            assertNotNull(dialog, "the View menu opened the dialog");
            juranometria.chart.ChartScene sceneBefore = chart[0].scene();
            // A label toggle first: the crowd's labels hide while the
            // target's rides its always-drawn symbol (PR #110 review).
            ReaderInput.click(box(dialog.getContentPane(),
                    "Deep-sky labels"));
            assertFalse(options.options().effectiveDeepSkyLabels(),
                    "the label choice previews live");
            assertSame(sceneBefore, chart[0].scene(),
                    "a label toggle re-renders the identical scene object");
            // Then the content toggle.
            ReaderInput.click(box(dialog.getContentPane(),
                    "Deep-sky objects"));
            assertFalse(options.options().deepSkyObjects(),
                    "the choice previews live");
            assertSame(wideOrion, navigation.state(),
                    "choosing the chart never moves it");
            assertEquals("NGC 1976", chart[0].scene().targetIdentity(),
                    "the scene still carries the searched target, which"
                            + " the renderer keeps drawn and labelled");
            assertTrue(chart[0].scene().title().startsWith("M 42"),
                    "the title honestly remains the shown target's");
            assertSame(sceneBefore, chart[0].scene(),
                    "option interaction is repaint-only: the very same"
                            + " scene object, so no reassembly and no query");

            // Home while options remain chosen: navigation resets,
            // the reader's choices stay.
            SwingUtilities.invokeAndWait(navigation::reset);
            flush();
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertFalse(options.options().deepSkyObjects(),
                    "Home leaves the chosen options alone");

            // Restore Defaults and confirm: the released chart returns
            // and persists; then a restarted session reads it.
            ReaderInput.click(button(dialog.getContentPane(),
                    "Restore Defaults"));
            ReaderInput.click(button(dialog.getContentPane(), "OK"));
            assertEquals(ChartOptions.DEFAULTS, options.options());
            assertEquals(ChartOptions.DEFAULTS, store.load(),
                    "OK persisted the restored defaults");
            assertEquals(ChartOptions.DEFAULTS,
                    new ChartOptionsController(store).options(),
                    "a restarted session reads exactly what was confirmed");

            // The journey ends on the exact released M31 chart.
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertEquals("M31 · Andromeda Galaxy region",
                    chart[0].scene().title());
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
