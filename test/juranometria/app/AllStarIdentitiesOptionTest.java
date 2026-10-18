package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

import juranometria.render.ChartOptions;
import juranometria.ui.companion.CompanionStore;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The all-star-identities switch as Chart Options holds it (#558, rulings 1
 * and 5 on #557): remembered with the other options and off for every store
 * that predates it; one state for the dialog and the Controller; unavailable
 * while names, letters and numbers are all off; named in both languages with
 * no shortcut promised; stated in the view report.
 */
class AllStarIdentitiesOptionTest {

    @Test
    void rememberedAndOffForAnOlderStore() throws Exception {
        Preferences node = Preferences.userRoot().node("juranometria-558-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            assertFalse(store.load().allStarIdentities(), "a store without the key: off");
            store.save(ChartOptions.DEFAULTS.withAllStarIdentities(true));
            assertTrue(store.load().allStarIdentities(), "remembered on");
            store.save(ChartOptions.DEFAULTS);
            assertFalse(store.load().allStarIdentities(), "and off");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void oneStateForBothHostsAndUnavailableWithNothingToWiden() throws Exception {
        SwingSession.scratchPreferences("all-star-identities", node -> {
            InterfaceText en = InterfaceText.forLanguage("en");
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            JComponent dialog = ChartOptionsDialog.content(controller, () -> { },
                    () -> false, en);
            JComponent companion = new ChartOptionsControls(controller, () -> false, en)
                    .inCompanion(CompanionStore.forNode(node), en);
            JCheckBox inDialog = box(dialog);
            JCheckBox inController = box(companion);
            assertEquals("All star identities where they fit", inDialog.getText());
            assertFalse(inDialog.isSelected() || inController.isSelected(), "off");
            // The box's own transition, as a press reaches it: a headless
            // content test of the controls' kind.
            inController.setSelected(true);
            for (java.awt.event.ActionListener listener : inController.getActionListeners()) {
                listener.actionPerformed(new java.awt.event.ActionEvent(inController,
                        java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
            }
            assertTrue(controller.options().allStarIdentities(), "the Controller sets it");
            assertTrue(inDialog.isSelected(), "and the dialog shows the same state");
            controller.accept(ChartSwitches.withChart(ChartSwitches.withChart(
                    ChartSwitches.withChart(controller.options(), "chart.starNames", false),
                    "chart.bayerLetters", false), "chart.flamsteedNumbers", false));
            assertFalse(inDialog.isEnabled() || inController.isEnabled(),
                    "all three off: unavailable in both");
            assertTrue(inDialog.isSelected(), "and it keeps the reader's choice");
            assertFalse(inDialog.getToolTipText().contains("⌘")
                    || inDialog.getAccessibleContext().getAccessibleDescription()
                            .contains("then"), "no shortcut is promised");
        });
    }

    @Test
    void namedInBothLanguages() {
        InterfaceText nb = InterfaceText.forLanguage("nb-NO");
        assertEquals("Alle stjerneidentiteter der de får plass",
                nb.say("chartoptions.allStarIdentities.label"));
        assertTrue(nb.say("chartoptions.allStarIdentities.explain").contains("12°"));
    }

    private static JCheckBox box(Component root) {
        List<Component> all = new ArrayList<>();
        collect(root, all);
        return (JCheckBox) all.stream().filter(c -> "allStarIdentities".equals(c.getName()))
                .findFirst().orElseThrow();
    }

    private static void collect(Component c, List<Component> into) {
        into.add(c);
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, into);
            }
        }
    }
}
