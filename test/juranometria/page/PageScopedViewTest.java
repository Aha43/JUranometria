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
    void theViewRefusesBlanksLikeEverythingElse() {
        assertThrows(IllegalArgumentException.class, () ->
                new WorkingMarksModel.Change(List.of(" "), " "));
    }
}
