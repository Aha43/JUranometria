package juranometria.app;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartOptionsDialogTest {

    @Test
    void checkboxesBindLivePreviewDependenciesAndTheProtocol() throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            store.save(new ChartOptions(true, true, true, false, true));
            ChartOptionsController controller =
                    new ChartOptionsController(store);
            int[] cancelled = new int[1];
            int[] confirmed = new int[1];
            JComponent content = ChartOptionsDialog.content(controller,
                    () -> cancelled[0]++, () -> confirmed[0]++);

            // Controls display the persisted/current value honestly.
            assertTrue(box(content, "Deep-sky objects").isSelected());
            assertFalse(box(content, "Constellation boundaries").isSelected());
            assertTrue(box(content, "Deep-sky labels").isEnabled());

            // Every change previews live through the controller.
            box(content, "Constellation boundaries").doClick();
            assertTrue(controller.options().constellationBoundaries(),
                    "a click previews immediately");
            assertEquals(false, store.load().constellationBoundaries(),
                    "previewing persists nothing");

            // Dependency enablement: symbols off disables labels, which
            // remembers its state.
            box(content, "Deep-sky objects").doClick();
            assertFalse(box(content, "Deep-sky labels").isEnabled(),
                    "labels are effective only while symbols are on");
            assertTrue(box(content, "Deep-sky labels").isSelected(),
                    "the disabled checkbox remembers its state");
            assertFalse(controller.options().effectiveDeepSkyLabels());
            box(content, "Deep-sky objects").doClick();
            assertTrue(box(content, "Deep-sky labels").isEnabled());

            box(content, "Constellation figures").doClick();
            assertFalse(box(content, "Constellation names").isEnabled(),
                    "names are effective only while figures are on");

            // Restore Defaults previews the released chart and re-enables
            // every dependent control.
            AboutDialogTest.button(content, "Restore Defaults").doClick();
            assertEquals(ChartOptions.DEFAULTS, controller.options());
            assertTrue(box(content, "Constellation names").isEnabled());
            assertTrue(box(content, "Constellation boundaries").isSelected());

            // OK and Cancel run exactly their wired protocol actions.
            AboutDialogTest.button(content, "OK").doClick();
            assertEquals(1, confirmed[0]);
            AboutDialogTest.button(content, "Cancel").doClick();
            assertEquals(1, cancelled[0]);
        } finally {
            node.removeNode();
        }
    }

    @Test
    void theDialogIsSingleInstanceAndEscapeIsCancel() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "window behaviour needs a display; content is tested headless");
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        JFrame[] frame = new JFrame[1];
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            ChartOptionsController controller =
                    new ChartOptionsController(store);
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new JFrame("options-test");
                ChartOptionsDialog.open(frame[0], controller);
                ChartOptionsDialog.open(frame[0], controller);
            });
            flush();
            assertEquals(1, openDialogCount(),
                    "opening twice never multiplies the dialog");

            // Preview a change, then Escape: the revert protocol runs.
            JDialog dialog = findDialog();
            SwingUtilities.invokeAndWait(() ->
                    box(dialog.getContentPane(), "Deep-sky objects").doClick());
            assertFalse(controller.options().deepSkyObjects(),
                    "premise: a live preview is active");
            SwingUtilities.invokeAndWait(() -> {
                var action = dialog.getRootPane().getActionForKeyStroke(
                        javax.swing.KeyStroke.getKeyStroke(
                                java.awt.event.KeyEvent.VK_ESCAPE, 0));
                action.actionPerformed(new java.awt.event.ActionEvent(
                        dialog.getRootPane(), 0, "escape"));
            });
            flush();
            assertTrue(controller.options().deepSkyObjects(),
                    "Escape reverts the preview to the open-time snapshot");
            assertEquals(ChartOptions.DEFAULTS, store.load(),
                    "Escape persists nothing");
            assertFalse(dialog.isDisplayable(), "Escape closes the dialog");
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

    private static int openDialogCount() {
        int count = 0;
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                count++;
            }
        }
        return count;
    }

    private static JDialog findDialog() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    static JCheckBox box(java.awt.Component component, String accessibleName) {
        if (component instanceof JCheckBox checkBox
                && accessibleName.equals(checkBox
                        .getAccessibleContext().getAccessibleName())) {
            return checkBox;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                JCheckBox found = box(child, accessibleName);
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
