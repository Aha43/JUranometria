package juranometria.app;

import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import juranometria.render.ChartOptions;
import juranometria.render.SymbolFamily;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That a failing acceptance run gives the reader their settings back
 * (Sprint 24, issue #217).
 *
 * <p>The packaged run changes a real preference to prove the
 * application comes back wearing the reader's choices. Restoring it
 * on the way out restores it <em>only when the run passes</em> — so
 * the run that finds a defect is also the run that leaves a reader
 * with their galaxies switched off (sprint review). A check that
 * damages what it is checking is worse than no check, and this is
 * the failure path rather than the happy one.
 */
class PackagedAcceptanceRestoresTest {

    private static ChartOptionsStore store() {
        return ChartOptionsStore.forNode(Preferences.userRoot()
                .node("juranometria-acceptance-restore-test"));
    }

    @org.junit.jupiter.api.AfterEach
    void leaveNoNodeBehind() throws Exception {
        // The gate's real-leak finding (#241): this node outlived
        // every run and accumulated in the developer's preference
        // store. Removed after each test, whatever happened in it.
        Preferences.userRoot()
                .node("juranometria-acceptance-restore-test")
                .removeNode();
    }

    @Test
    void aFailingRunPutsTheReadersChoiceBack() throws Exception {
        ChartOptionsStore store = store();
        ChartOptions theirs = ChartOptions.DEFAULTS;
        store.save(theirs);
        ChartOptions temporary =
                theirs.withFamily(SymbolFamily.GALAXIES, false);

        IllegalStateException failed = assertThrows(
                IllegalStateException.class,
                () -> PackagedAcceptanceMain.withTemporaryOptions(store,
                        temporary, () -> {
                            // The premise: the temporary choice really
                            // was in force when the run failed.
                            assertEquals(temporary, store.load());
                            throw new IllegalStateException(
                                    "the acceptance found something");
                        }));

        assertEquals("the acceptance found something", failed.getMessage(),
                "the failure is not swallowed by the restoration");
        assertEquals(theirs, store.load(),
                "and the reader's own choice is back, though the run"
                        + " ended in the middle");
    }

    @Test
    void aPassingRunAlsoPutsItBack() throws Exception {
        ChartOptionsStore store = store();
        ChartOptions theirs = ChartOptions.DEFAULTS
                .withFamily(SymbolFamily.NEBULAE, false);
        store.save(theirs);
        ChartOptions temporary =
                theirs.withFamily(SymbolFamily.GALAXIES, false);
        boolean[] ran = {false};

        PackagedAcceptanceMain.withTemporaryOptions(store, temporary,
                () -> ran[0] = true);

        assertTrue(ran[0], "the body ran");
        assertEquals(theirs, store.load(),
                "and their nebulae are still switched off, which is a"
                        + " choice they made and this run did not");
        assertNotEquals(temporary, store.load());
    }

    /** A store that can be made to fail where a real one might. */
    private static final class Fragile implements ChartOptionsStore {
        private ChartOptions held = ChartOptions.DEFAULTS;
        int flushes;
        boolean failNextSave;
        boolean failNextFlush;

        @Override public ChartOptions load() {
            return held;
        }

        @Override public void save(ChartOptions options) {
            if (failNextSave) {
                failNextSave = false;
                throw new IllegalStateException("the store refused a write");
            }
            held = options;
        }

        @Override public void flush() {
            flushes++;
            if (failNextFlush) {
                failNextFlush = false;
                throw new IllegalStateException("the store would not settle");
            }
        }
    }

    @Test
    void aFlushThatFailsAfterTheWriteStillPutsTheChoiceBack() {
        // The interval has to start before the mutation. With the
        // save and its flush outside the guard, this left the
        // reader wearing the run's choice and nobody to undo it
        // (sprint review).
        Fragile store = new Fragile();
        ChartOptions theirs = ChartOptions.DEFAULTS
                .withFamily(SymbolFamily.NEBULAE, false);
        store.save(theirs);
        store.failNextFlush = true;

        IllegalStateException failed = assertThrows(
                IllegalStateException.class,
                () -> PackagedAcceptanceMain.withTemporaryOptions(store,
                        theirs.withFamily(SymbolFamily.GALAXIES, false),
                        () -> { }));

        assertEquals("the store would not settle", failed.getMessage());
        assertEquals(theirs, store.load(),
                "the write happened and the flush failed, and the"
                        + " reader's choice is still back");
    }

    @Test
    void aWriteThatFailsLeavesTheChoiceAsItWas() {
        Fragile store = new Fragile();
        ChartOptions theirs = ChartOptions.DEFAULTS;
        store.save(theirs);
        store.failNextSave = true;

        assertThrows(IllegalStateException.class,
                () -> PackagedAcceptanceMain.withTemporaryOptions(store,
                        theirs.withFamily(SymbolFamily.GALAXIES, false),
                        () -> { }));

        assertEquals(theirs, store.load(),
                "nothing was applied, and nothing is left applied");
    }

    @Test
    void aFailingRestorationDoesNotHideTheFailureItWasCleaningUpAfter() {
        Fragile store = new Fragile();
        store.save(ChartOptions.DEFAULTS);

        IllegalStateException failed = assertThrows(
                IllegalStateException.class,
                () -> PackagedAcceptanceMain.withTemporaryOptions(store,
                        ChartOptions.DEFAULTS
                                .withFamily(SymbolFamily.GALAXIES, false),
                        () -> {
                            store.failNextSave = true;   // the restore
                            throw new IllegalStateException(
                                    "the acceptance found something");
                        }));

        assertEquals("the acceptance found something", failed.getMessage(),
                "the run's own failure is what a reader is told about");
        assertEquals(1, failed.getSuppressed().length,
                "with the restoration's failure carried alongside it"
                        + " rather than in place of it");
    }

    @Test
    void theStoreItWasGivenIsTheStoreItSettles() {
        // It used to flush the application's own node whatever store
        // it was handed, which is a helper reaching past its
        // argument to something it assumed (sprint review).
        Fragile store = new Fragile();
        store.save(ChartOptions.DEFAULTS);

        assertEquals(0, store.flushes);
        assertDoesNotThrow(() ->
                PackagedAcceptanceMain.withTemporaryOptions(store,
                        ChartOptions.DEFAULTS
                                .withFamily(SymbolFamily.GALAXIES, false),
                        () -> { }));
        assertEquals(2, store.flushes,
                "settled once for the temporary choice and once for"
                        + " the restoration, on the store it was"
                        + " given");
    }

    @Test
    void theTemporaryChoiceIsReadableFromAFreshStoreWhileItHolds()
            throws Exception {
        // Because the packaged run's whole point is that a *restart*
        // reads it: a value held only in memory would prove nothing.
        ChartOptionsStore store = store();
        store.save(ChartOptions.DEFAULTS);
        ChartOptions temporary = ChartOptions.DEFAULTS
                .withFamily(SymbolFamily.GALAXIES, false);

        PackagedAcceptanceMain.withTemporaryOptions(store, temporary,
                () -> assertEquals(temporary,
                        ChartOptionsStore.forNode(Preferences.userRoot()
                                .node("juranometria-acceptance-restore-test"))
                                .load(),
                        "a fresh store reads the temporary choice"));

        assertEquals(ChartOptions.DEFAULTS, store.load());
    }
}
