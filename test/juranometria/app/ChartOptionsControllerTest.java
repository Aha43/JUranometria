package juranometria.app;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartOptionsControllerTest {

    @Test
    void theControllerOwnsTheWholeDialogProtocolOutsideSwing() throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            store.save(new ChartOptions(true, true, false, true, true));
            ChartOptionsController controller =
                    new ChartOptionsController(store);
            assertEquals(store.load(), controller.options(),
                    "the controller starts from the persisted options");

            List<ChartOptions> seen = new ArrayList<>();
            controller.onChange(seen::add);
            assertEquals(1, seen.size(), "registration hands the current value");

            // Live preview: one notification per real change, none for
            // a no-op.
            ChartOptions snapshot = controller.options();
            ChartOptions previewed = new ChartOptions(
                    false, true, false, true, true);
            controller.apply(previewed);
            controller.apply(previewed);
            assertEquals(2, seen.size(), "one notification per real change");
            assertEquals(previewed, controller.options());
            assertEquals(snapshot, store.load(),
                    "previewing persists nothing");

            // Restore Defaults is an ordinary previewed transition.
            controller.restoreDefaults();
            assertEquals(ChartOptions.DEFAULTS, controller.options());
            assertEquals(snapshot, store.load(),
                    "Restore Defaults previews; it does not persist");

            // Cancel: back to the open-time snapshot, still unpersisted.
            controller.revertTo(snapshot);
            assertEquals(snapshot, controller.options());
            assertEquals(4, seen.size());

            // OK: the current previewed value persists.
            controller.apply(previewed);
            controller.confirm();
            assertEquals(previewed, store.load(),
                    "confirm persists exactly the previewed options");
            assertTrue(new ChartOptionsController(store).options()
                            .equals(previewed),
                    "a fresh session reads the confirmed options");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void homeAndRestoreDefaultsNeverCrossTheirStateBoundaries() throws Exception {
        // PR #108 follow-up: the real navigation and options controllers
        // side by side - Home resets navigation only, Restore Defaults
        // resets options only, and neither notifies the other's path.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            ChartOptionsController optionsController =
                    new ChartOptionsController(store);
            juranometria.ui.ChartViewController navigation =
                    new juranometria.ui.ChartViewController();

            java.util.List<ChartOptions> optionChanges =
                    new java.util.ArrayList<>();
            java.util.List<juranometria.chart.ChartViewState> viewChanges =
                    new java.util.ArrayList<>();
            optionsController.onChange(optionChanges::add);
            navigation.onChange(viewChanges::add);

            // A wandered, customized session: panned and zoomed
            // navigation, non-default options.
            navigation.recenter(new juranometria.chart.SkyPosition(
                    83.818667, -5.389667), "M 42 region", "NGC 1976");
            navigation.zoomOut();
            navigation.decreaseMagnitudeLimit();
            ChartOptions custom = new ChartOptions(false, false, true,
                    false, true);
            optionsController.apply(custom);
            juranometria.chart.ChartViewState wandered = navigation.state();
            int viewNotifications = viewChanges.size();
            int optionNotifications = optionChanges.size();

            // Home: navigation returns to the released default; chart
            // options and their notification path are untouched.
            navigation.reset();
            assertEquals(juranometria.chart.ChartViewState.DEFAULT,
                    navigation.state());
            assertEquals(custom, optionsController.options(),
                    "Home leaves chart options unchanged");
            assertEquals(optionNotifications, optionChanges.size(),
                    "Home notifies no options listener");

            // Re-wander, then Restore Defaults: options return to the
            // released chart; complete navigation state and its
            // notification path are untouched.
            navigation.recenter(wandered.centre(), wandered.targetLabel(),
                    wandered.targetIdentity());
            navigation.zoomOut();
            juranometria.chart.ChartViewState beforeRestore =
                    navigation.state();
            int viewBeforeRestore = viewChanges.size();
            optionsController.restoreDefaults();
            assertEquals(ChartOptions.DEFAULTS, optionsController.options());
            assertSame(beforeRestore, navigation.state(),
                    "Restore Defaults leaves complete navigation state"
                            + " unchanged - centre, field, magnitude, and"
                            + " target identity included");
            assertEquals(viewBeforeRestore, viewChanges.size(),
                    "Restore Defaults notifies no navigation listener");
            assertEquals("NGC 1976", navigation.state().targetIdentity());
        } finally {
            node.removeNode();
        }
    }
}
