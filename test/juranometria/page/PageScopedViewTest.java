package juranometria.page;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import juranometria.chart.WorkingSelection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The adapter is a page-scoped view, never a pruner (issue #260
 * review): navigation narrows what the Sprint 24 surfaces see and
 * leaves the session model exactly as it was - the foundation's
 * defining cross-page invariant, held where it was found broken.
 */
class PageScopedViewTest {

    /** A page holding exactly these identities - the marks test's
     * own fixture shape, real entries and all. */
    private static PageContents pageOf(String... identities) {
        List<PageEntry> entries = new ArrayList<>();
        for (String identity : identities) {
            entries.add(new PageEntry.DeepSky(
                    new juranometria.chart.DeepSkyObject(identity,
                            List.of(),
                            juranometria.chart.DsoType.GALAXY,
                            new juranometria.chart.SkyPosition(10.0, 41.0),
                            2.0, 2.0, 0.0, 9.0, 1,
                            new juranometria.chart.DeepSkyObject.Recorded(
                                    2.0, 2.0, 0.0,
                                    juranometria.chart.DeepSkyObject
                                            .Recorded.Band.VISUAL)),
                    PageVisibility.DRAWN, 0.5));
        }
        return new PageContents(entries);
    }

    @Test
    void navigationNarrowsTheViewAndNeverTheModel() {
        WorkingSelection model = new WorkingSelection();
        WorkingMarksModel view = new WorkingMarksModel(model);
        model.replaceWith(List.of("M 31", "M 42", "NGC 206"), "M 42");

        List<WorkingMarksModel.Change> heard = new ArrayList<>();
        view.onChange(heard::add);
        heard.clear();

        view.pruneTo(pageOf("M 31", "NGC 206"));
        assertEquals(List.of("M 31", "M 42", "NGC 206"),
                model.members(),
                "the session model keeps every member across the"
                        + " page change - its defining invariant");
        assertEquals("M 42", model.lead(),
                "and its lead: the model heard nothing");
        assertEquals(List.of("M 31", "NGC 206"), view.marks(),
                "while the view shows the page-bound picture the"
                        + " surfaces were reviewed against");
        assertEquals("NGC 206", view.lead(),
                "with the last-marked visible member leading the"
                        + " view");
        assertEquals(1, heard.size(),
                "one page change, one view change");

        view.pruneTo(pageOf("M 31", "M 42", "NGC 206"));
        assertEquals(List.of("M 31", "M 42", "NGC 206"), view.marks(),
                "a page that holds a member again shows it again -"
                        + " the cross-page invariant visible through"
                        + " the view, because nothing was ever"
                        + " removed");
        assertEquals("M 42", view.lead(),
                "and the model's own lead returns with it");
    }

    @Test
    void aScopedWriteStillLandsInTheOneModel() {
        WorkingSelection model = new WorkingSelection();
        WorkingMarksModel view = new WorkingMarksModel(model);
        model.replaceWith(List.of("M 42"), "M 42");
        view.pruneTo(pageOf("M 31", "NGC 206"));
        view.mark("M 31");
        assertEquals(List.of("M 42", "M 31"), model.members(),
                "a write through the view lands in the one truth,"
                        + " scope or no scope");
        assertEquals(List.of("M 31"), view.marks(),
                "and the view keeps showing only its page");
    }

    @Test
    void twoListenersHearOneOrderWhenAListenerPrunesMidDelivery() {
        // The review's exact failure: with a subscription per
        // listener and a mid-delivery broadcast, the first listener
        // heard [before, after] while the second heard
        // [after, after]. One model subscription and one serialized
        // view queue give every listener the same history.
        WorkingSelection model = new WorkingSelection();
        WorkingMarksModel view = new WorkingMarksModel(model);
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();
        boolean[] reacted = {false};
        view.onChange(change -> {
            first.add(change.marks().toString());
            if (!reacted[0] && change.marks().contains("M 42")) {
                reacted[0] = true;
                view.pruneTo(pageOf("M 31"));   // nested scope change
            }
        });
        view.onChange(change -> second.add(change.marks().toString()));
        first.clear();
        second.clear();

        model.replaceWith(List.of("M 31", "M 42"), "M 42");
        assertEquals(first, second,
                "every view listener hears the same states in the"
                        + " same order, nested prune included");
        assertEquals(List.of("[M 31, M 42]", "[M 31]"), first,
                "the model's state first, the narrowed scope after -"
                        + " queued, never broadcast midway");
    }

    @Test
    void twoListenersHearOneOrderWhenAListenerWritesDuringAPrune() {
        // The other direction: a listener reacting to a scope
        // change with a model write. The write's transition queues
        // behind the scope change for everyone alike.
        WorkingSelection model = new WorkingSelection();
        WorkingMarksModel view = new WorkingMarksModel(model);
        model.replaceWith(List.of("M 31", "M 42"), "M 42");
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();
        boolean[] reacted = {false};
        view.onChange(change -> {
            first.add(change.marks().toString());
            if (!reacted[0] && !change.marks().contains("M 42")) {
                reacted[0] = true;
                view.mark("NGC 206");          // nested model write
            }
        });
        view.onChange(change -> second.add(change.marks().toString()));
        first.clear();
        second.clear();

        view.pruneTo(pageOf("M 31", "NGC 206"));
        assertEquals(first, second,
                "one order for every listener in this direction too");
        assertEquals(List.of("[M 31]", "[M 31, NGC 206]"), first,
                "the scope change first, the nested write queued"
                        + " after it");
        assertEquals(List.of("M 31", "M 42", "NGC 206"),
                model.members(),
                "and the model holds everything: the scoped-out"
                        + " member survived both transitions");
    }

    @Test
    void offPageModelChangesStaySilentAtTheQueueBoundary() {
        // The review's remaining P1: a model transition entirely
        // outside the scope maps to an identical view and must not
        // publish a duplicate event.
        WorkingSelection model = new WorkingSelection();
        WorkingMarksModel view = new WorkingMarksModel(model);
        model.replaceWith(List.of("M 31", "M 42"), "M 31");
        view.pruneTo(pageOf("M 31"));
        List<String> heard = new ArrayList<>();
        view.onChange(change -> heard.add(change.marks() + "/"
                + change.lead()));
        heard.clear();

        model.add("NGC 206");          // off this page
        model.lead("M 42");            // off-page lead movement
        model.remove("NGC 206");       // off-page removal
        assertEquals(List.of(), heard,
                "changes the view cannot show publish nothing");
        assertEquals(List.of("M 31", "M 42"), model.members(),
                "while the model heard every one of them");

        // But a change that moves what is visible - here the
        // fallback lead, when a visible member arrives while the
        // model's lead is off the page - differs and publishes.
        view.pruneTo(pageOf("M 31", "NGC 205"));
        heard.clear();
        model.add("NGC 205");
        assertEquals(List.of("[M 31, NGC 205]/NGC 205"), heard,
                "a visible arrival publishes once, leading the view");
        model.lead("M 42");            // off-page again
        assertEquals(List.of("[M 31, NGC 205]/NGC 205"), heard,
                "an off-page lead change leaves the visible fallback"
                        + " exactly as it was - silent");
        model.remove("NGC 205");
        assertEquals(2, heard.size(),
                "removing the visible fallback changes the view");
        assertEquals("[M 31]/M 31", heard.get(1),
                "and the fallback passes to the remaining visible"
                        + " member");
    }

    @Test
    void theViewRefusesBlanksLikeEverythingElse() {
        assertThrows(IllegalArgumentException.class, () ->
                new WorkingMarksModel.Change(List.of(" "), " "));
    }
}
