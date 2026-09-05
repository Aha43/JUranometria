package juranometria.ui.ecliptic;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.app.SwingSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one preference the ecliptic keeps (Sprint 28, issue #274).
 *
 * <p>Against scratch nodes, never the developer's own preferences.
 */
class EclipticStoreTest {

    @Test
    void aReaderWhoHasNeverChosenGetsTheReleasedDefault()
            throws Exception {
        SwingSession.scratchPreferences("ecliptic-fresh", node -> {
            EclipticStore store = EclipticStore.forNode(node);
            assertEquals(Optional.empty(), store.shown(),
                    "nothing is remembered until a reader chooses");
            assertFalse(store.shownOrDefault(),
                    "and the released default is hidden: installing a"
                            + " removable module must not change the"
                            + " page for someone who never asked");
            assertFalse(EclipticStore.DEFAULT_SHOWN,
                    "which is stated once, where #275 can read it");
        });
    }

    @Test
    void theStoresDefaultAndTheModulesOwnAgree() {
        // Two things state "hidden": this store's released default,
        // and the module's own initial state for anyone who attaches
        // it without a store. They must not drift apart, so the
        // agreement is asserted rather than assumed.
        assertEquals(EclipticStore.DEFAULT_SHOWN,
                new juranometria.ecliptic.EclipticModule().showing(),
                "a module attached without a store shows what a reader"
                        + " who never chose would be shown");
    }

    @Test
    void aChoiceIsRememberedAndReadBackByAFreshStore() throws Exception {
        SwingSession.scratchPreferences("ecliptic-remember", node -> {
            EclipticStore.forNode(node).save(true);
            EclipticStore.forNode(node).flush();

            EclipticStore fresh = EclipticStore.forNode(node);
            assertEquals(Optional.of(Boolean.TRUE), fresh.shown(),
                    "a second session reads what the first chose");
            assertTrue(fresh.shownOrDefault(),
                    "and draws it");
        });
    }

    @Test
    void hidingItIsAChoiceAndNotAnAbsence() throws Exception {
        // The three states the gate insisted stay distinct. Today an
        // explicit false and a missing key draw the same chart, which
        // is exactly why collapsing them would go unnoticed: a stated
        // choice has to outlive a change of default, and a removal
        // and return must not silently reset what a reader asked for.
        SwingSession.scratchPreferences("ecliptic-hidden", node -> {
            EclipticStore store = EclipticStore.forNode(node);
            assertEquals(Optional.empty(), store.shown(),
                    "never chosen");

            store.save(false);
            assertEquals(Optional.of(Boolean.FALSE), store.shown(),
                    "chose to hide it - which is not the same answer"
                            + " as never having chosen");
            assertFalse(store.shownOrDefault(),
                    "and both draw the same chart today, which is why"
                            + " the distinction has to be asserted");

            store.save(true);
            assertEquals(Optional.of(Boolean.TRUE), store.shown(),
                    "and chose to show it");
        });
    }

    @Test
    void anUnreadableValueIsTreatedAsNeverChosen() throws Exception {
        SwingSession.scratchPreferences("ecliptic-rubbish", node -> {
            node.put("eclipticShown", "perhaps");
            assertEquals(Optional.empty(),
                    EclipticStore.forNode(node).shown(),
                    "a store the reader never wrote cannot speak for"
                            + " them: it says nothing rather than one"
                            + " of the two answers");
        });
    }

    @Test
    void anAbsentModuleNeitherReadsNorWritesTheChoice() throws Exception {
        // The gate's requirement: the preference survives the
        // module's absence and is honoured when it returns. Nothing
        // but a session with the module present may touch it.
        SwingSession.scratchPreferences("ecliptic-absent", node -> {
            EclipticStore.forNode(node).save(true);
            EclipticStore.forNode(node).flush();

            // A session with no ecliptic module: the atlas runs, and
            // this key is not its business.
            assertEquals(Optional.of(Boolean.TRUE),
                    EclipticStore.forNode(node).shown(),
                    "the reader's choice is still there afterwards,"
                            + " untouched, and is honoured when the"
                            + " module returns");
            assertEquals(1, node.keys().length,
                    "and the module keeps exactly one key, so nothing"
                            + " else of the reader's is at stake: "
                            + java.util.Arrays.toString(node.keys()));
        });
    }
}
