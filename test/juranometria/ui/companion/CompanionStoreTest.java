package juranometria.ui.companion;

import java.awt.Rectangle;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import juranometria.app.SwingSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the companion remembers (#434): its own geometry, whether it
 * was open, and which sections were collapsed - and nothing of the
 * Place and Time state it shows.
 */
class CompanionStoreTest {

    @Test
    void neverChosenRememberedAndUnreadable() throws Exception {
        SwingSession.scratchPreferences("companion-store", this::held);
    }

    private void held(Preferences node) throws Exception {
        CompanionStore store = CompanionStore.forNode(node);
        assertEquals(Optional.empty(), store.bounds(), "never placed");
        assertFalse(store.visible(), "never opened");
        assertFalse(store.collapsed("placeandtime"), "never collapsed");

        store.saveBounds(new Rectangle(-1200, 40, 360, 330));
        store.saveVisible(true);
        store.saveCollapsed("placeandtime", true);
        CompanionStore again = CompanionStore.forNode(node);
        assertEquals(Optional.of(new Rectangle(-1200, 40, 360, 330)),
                again.bounds(), "a display left of the main one is a"
                        + " negative x, and kept as one");
        assertTrue(again.visible());
        assertTrue(again.collapsed("placeandtime"));

        for (String key : node.keys()) {
            assertTrue(key.startsWith("companion."),
                    "only the window's own keys: " + key);
        }

        node.putInt("companion.width", 0);
        assertEquals(Optional.empty(), again.bounds(),
                "a rectangle with no width is no rectangle");
        node.put("companion.visible", "perhaps");
        assertFalse(again.visible(), "anything unreadable is never chosen");
    }
}
