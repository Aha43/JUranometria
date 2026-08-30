package juranometria.app;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
