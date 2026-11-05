package juranometria.app;

import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.ui.language.InterfaceText;
import juranometria.ui.selection.SelectedObjectStore;
import juranometria.ui.selection.SelectedObjectSwitch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Selected object box in Chart Options (#692, ruled C-7 on #690): on
 * by default, a switch of its own beside the title block in both hosts,
 * remembered in its own store, and absent from a host without the switch.
 */
class SelectedObjectOptionTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");

    @Test
    void theBoxDrivesItsSwitchAndFollowsItInEveryHost() throws Exception {
        Preferences node = Preferences.userRoot().node("juranometria-test-selected-option-"
                + System.nanoTime());
        try {
            SwingUtilities.invokeAndWait(() -> {
                ChartOptionsController controller = new ChartOptionsController(
                        ChartOptionsStore.forNode(node));
                SelectedObjectSwitch shown = new SelectedObjectSwitch(
                        SelectedObjectStore.forNode(node));
                ChartOptionsControls dialog = new ChartOptionsControls(controller,
                        () -> false, null, shown, EN);
                ChartOptionsControls companion = new ChartOptionsControls(controller,
                        () -> false, null, shown, EN);
                JCheckBox box = dialog.selectedObjectBox();
                assertNotNull(box);
                assertEquals(ChartOptionsControls.SELECTED_OBJECT_BOX, box.getName());
                assertEquals("Selected object box", box.getText());
                assertTrue(box.isSelected(), "on by default");
                box.setSelected(false);
                for (var listener : box.getActionListeners()) {
                    listener.actionPerformed(new ActionEvent(box, ActionEvent.ACTION_PERFORMED, ""));
                }
                assertFalse(shown.showing(), "the box is the switch");
                assertFalse(companion.selectedObjectBox().isSelected(), "the other host follows");
                assertFalse(SelectedObjectStore.forNode(node).shownOrDefault(), "and it is remembered");
                assertEquals(ChartOptionsStore.forNode(node).load(), controller.options(),
                        "the chart's options are untouched");
                assertNull(new ChartOptionsControls(controller, () -> false, EN)
                        .selectedObjectBox(), "a host without the switch leaves the box out");
            });
        } finally {
            node.removeNode();
        }
    }
}
