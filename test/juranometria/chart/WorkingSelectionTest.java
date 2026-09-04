package juranometria.chart;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The working selection's decided semantics (issues #258/#260):
 * every transition, the ordering and lead rules, the reentrant
 * delivery discipline, and the subscription lifecycle - plus the
 * gate's named mutation targets as tests that would catch them.
 */
class WorkingSelectionTest {

    @Test
    void togglePerformsTheAdditiveVerbBothWays() {
        WorkingSelection selection = new WorkingSelection();
        selection.toggle("M 31");
        assertEquals(List.of("M 31"), selection.members(),
                "absent: added");
        assertEquals("M 31", selection.lead(), "and it leads");
        selection.toggle("M 32");
        assertEquals(List.of("M 31", "M 32"), selection.members());
        assertEquals("M 32", selection.lead());
        selection.toggle("M 31");
        assertEquals(List.of("M 32"), selection.members(),
                "present: removed, the rest stay");
        assertEquals("M 32", selection.lead(),
                "removing a non-lead leaves the lead alone");
    }

    @Test
    void removingTheLeadPassesItToTheLastMarkedRemainingMember() {
        WorkingSelection selection = new WorkingSelection();
        selection.add("M 31");
        selection.add("M 32");
        selection.add("NGC 206");
        selection.remove("NGC 206");
        assertEquals("M 32", selection.lead(),
                "the standing rule, kept: the last-marked remaining"
                        + " member leads");
        assertEquals(List.of("M 31", "M 32"), selection.members());
        selection.remove("M 31");
        assertEquals("M 32", selection.lead(),
                "removing a non-lead changes no lead");
        selection.remove("M 32");
        assertNull(selection.lead(), "an empty set has no lead");
        assertTrue(selection.members().isEmpty());
    }

    @Test
    void reAddingAMemberTakesTheLeadWithoutMovingItsPlace() {
        WorkingSelection selection = new WorkingSelection();
        selection.add("M 31");
        selection.add("M 32");
        selection.add("M 31");
        assertEquals(List.of("M 31", "M 32"), selection.members(),
                "order is first membership; re-adding does not move");
        assertEquals("M 31", selection.lead(),
                "but the newest question leads");
    }

    @Test
    void leadChangesNeverRemoveAndOutsidersMayNotLead() {
        WorkingSelection selection = new WorkingSelection();
        selection.add("M 31");
        selection.add("M 32");
        selection.lead("M 31");
        assertEquals(List.of("M 31", "M 32"), selection.members(),
                "a lead change is never a silent removal");
        assertEquals("M 31", selection.lead());
        assertThrows(IllegalArgumentException.class,
                () -> selection.lead("NGC 206"),
                "only a member can lead");
    }

    @Test
    void duplicatesAndBlankIdentitiesAreImpossible() {
        WorkingSelection selection = new WorkingSelection();
        assertThrows(IllegalArgumentException.class, () ->
                selection.replaceWith(List.of("M 31", "M 31"), "M 31"));
        assertThrows(IllegalArgumentException.class, () ->
                selection.add(" "));
        assertThrows(IllegalArgumentException.class, () ->
                new WorkingSelection.Change(List.of("M 31"), "M 32"));
        assertThrows(IllegalArgumentException.class, () ->
                new WorkingSelection.Change(List.of("M 31"), null));
    }

    @Test
    void aReplacingGesturePublishesOnce() {
        WorkingSelection selection = new WorkingSelection();
        selection.add("M 31");
        List<WorkingSelection.Change> heard = new ArrayList<>();
        Runnable release = selection.onChange(heard::add);
        heard.clear();             // drop the subscription snapshot
        selection.replaceWith(List.of("M 42", "M 45", "M 13"), "M 13");
        assertEquals(1, heard.size(),
                "one gesture, one publish - never a run of"
                        + " intermediate states");
        assertEquals(List.of("M 42", "M 45", "M 13"),
                heard.get(0).members());
        assertEquals("M 13", heard.get(0).lead());
        release.run();
    }

    @Test
    void nestedTransitionsBuildOnQueuedStateAndDeliverInOrder() {
        // The gate's stale-state mutation target: a listener that
        // reacts during a delivery must observe fields matching the
        // event in flight, and its own change must build on the
        // queued state rather than silently undoing it.
        WorkingSelection selection = new WorkingSelection();
        List<String> log = new ArrayList<>();
        boolean[] reacted = {false};
        selection.onChange(change -> {
            log.add(change.members() + "/" + change.lead());
            assertEquals(change.members(), selection.members(),
                    "the state visible during a notification is the"
                            + " transition being delivered");
            assertEquals(change.lead(), selection.lead());
            if (!reacted[0] && change.members().contains("M 31")) {
                reacted[0] = true;
                selection.add("M 32");     // nested add
                selection.remove("M 31");  // nested remove
            }
        });
        selection.add("M 31");
        assertEquals(List.of("M 32"), selection.members(),
                "the nested changes built on each other: the add"
                        + " survived, the remove removed");
        assertEquals("M 32", selection.lead());
        assertEquals(List.of("[]/null", "[M 31]/M 31",
                        "[M 31, M 32]/M 32", "[M 32]/M 32"), log,
                "every consumer hears one order, whole states only");
    }

    @Test
    void nestedClearAndLeadChangeDeliverWholeStates() {
        WorkingSelection selection = new WorkingSelection();
        selection.replaceWith(List.of("M 31", "M 32"), "M 32");
        List<String> log = new ArrayList<>();
        boolean[] reacted = {false};
        selection.onChange(change -> {
            log.add(String.valueOf(change.lead()));
            if (!reacted[0]) {
                reacted[0] = true;
                selection.lead("M 31");    // nested lead change
                selection.clear();         // nested clear
            }
        });
        assertTrue(selection.members().isEmpty(),
                "the nested clear is the final state");
        assertEquals(List.of("M 32", "M 31", "null"), log);
    }

    @Test
    void subscribersAttachObserveAndReleaseCleanly() {
        WorkingSelection selection = new WorkingSelection();
        List<String> heard = new ArrayList<>();
        Runnable release = selection.onChange(change ->
                heard.add("a:" + change.members().size()));
        assertEquals(List.of("a:0"), heard,
                "a subscriber is told the current state immediately");
        release.run();
        selection.add("M 31");
        assertEquals(List.of("a:0"), heard,
                "a released subscription hears nothing more");
    }

    @Test
    void theModelHasNoPruneAndNoPersistenceVerb() throws Exception {
        // The gate's accidental-pruning and persistence mutation
        // targets, structurally: no method of the model mentions
        // pruning, saving, storing or loading - navigation and
        // stores simply have nothing to call.
        for (java.lang.reflect.Method method
                : WorkingSelection.class.getMethods()) {
            String name = method.getName().toLowerCase(
                    java.util.Locale.ROOT);
            for (String verb : new String[] {"prune", "save", "store",
                    "load", "persist", "write"}) {
                assertTrue(!name.contains(verb),
                        "the model offers no " + verb + " route: "
                                + method.getName());
            }
        }
        // And by the scanner's own preference-door rule: the source
        // opens no real preference node through any derived door.
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of(
                        "src/juranometria/chart/WorkingSelection.java"));
        assertTrue(!juranometria.tool.TestEvidenceScan
                        .opensRealPreferences(source),
                "no member identity can reach the reader's store");
    }
}
