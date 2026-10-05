package juranometria.ui;

import java.awt.Rectangle;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the chart window remembers about its own chrome and place
 * (#450, ruled on #449): the toolbar visible unless the reader chose
 * otherwise, and anything unreadable being "never chosen"; the last
 * ordinary bounds and the maximised flag, kept apart.
 */
class ChartChromeStoreTest {

    @Test
    void theToolbarIsShownUnlessTheReaderChoseOtherwise() throws Exception {
        juranometria.app.SwingSession.scratchPreferences("chart-chrome-store", node -> {
            ChartChromeStore store = ChartChromeStore.forNode(node);
            assertEquals(Optional.empty(), store.toolbarShown(), "never chosen");
            assertTrue(store.toolbarShownOrDefault(), "visible is the default");
            store.saveToolbarShown(false);
            assertFalse(ChartChromeStore.forNode(node).toolbarShownOrDefault(),
                    "hidden at a clean quit is hidden at the next start");
            node.put("chartToolbarShown", "sideways");
            assertTrue(ChartChromeStore.forNode(node).toolbarShownOrDefault(),
                    "anything unreadable is never chosen, and the bar shows");
            assertEquals("chartToolbarShown", node.keys()[0]);
        });
    }

    @Test
    void theWindowRemembersOrdinaryBoundsAndTheMaximisedFlagApart() throws Exception {
        juranometria.app.SwingSession.scratchPreferences("chart-window-store", node -> {
            ChartWindowStore store = ChartWindowStore.forNode(node);
            assertEquals(Optional.empty(), store.bounds());
            assertFalse(store.maximized());
            Rectangle was = new Rectangle(306, 75, 900, 785);
            store.saveBounds(was);
            store.saveMaximized(true);
            ChartWindowStore again = ChartWindowStore.forNode(node);
            assertEquals(Optional.of(was), again.bounds(),
                    "the ordinary bounds are kept while maximised");
            assertTrue(again.maximized());
            node.putInt("window.width", 0);
            assertEquals(Optional.empty(), ChartWindowStore.forNode(node).bounds(),
                    "a rectangle with no width is no rectangle");
            for (String key : node.keys()) {
                assertTrue(key.startsWith("window."), key);
            }
        });
    }
}
