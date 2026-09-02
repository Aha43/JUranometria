package juranometria.page;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader's working marks (Sprint 24, issue #215).
 *
 * <p>What is asserted here is not the arithmetic of a set - it is
 * that a consumer is never shown a state that cannot be true. A
 * table drawing crosses from a lead that has already left the page,
 * or from a set two events out of date, draws the wrong page and
 * blames the reader.
 */
class WorkingMarksModelTest {

    @Test
    void aMarkedSetKeepsItsOrderAndItsNewestIsTheLead() {
        WorkingMarksModel marks = new WorkingMarksModel();
        marks.mark("NGC 224");
        marks.mark("NGC 221");
        marks.mark("NGC 205");

        assertEquals(List.of("NGC 224", "NGC 221", "NGC 205"), marks.marks(),
                "in the order the reader marked them");
        assertEquals("NGC 205", marks.lead(),
                "and the newest question is the one being asked");
    }

    @Test
    void aMarkAlreadyMarkedAndLeadingSaysNothingFurther() {
        WorkingMarksModel marks = new WorkingMarksModel();
        List<WorkingMarksModel.Change> heard = new ArrayList<>();
        marks.onChange(heard::add);
        marks.mark("NGC 224");
        int after = heard.size();

        marks.mark("NGC 224");
        assertEquals(after, heard.size(),
                "nothing a consumer could observe has changed, so"
                        + " nothing is published");
    }

    @Test
    void unmarkingTheLeadHandsTheLeadToWhatIsLeft() {
        WorkingMarksModel marks = new WorkingMarksModel();
        marks.mark("A");
        marks.mark("B");
        marks.unmark("B");

        assertEquals(List.of("A"), marks.marks());
        assertEquals("A", marks.lead(), "the set never leads with nothing"
                + " while it still holds something");

        marks.unmark("A");
        assertTrue(marks.marks().isEmpty());
        assertNull(marks.lead(), "and an empty set leads with nothing");
    }

    @Test
    void onlyAMarkedIdentityCanLead() {
        WorkingMarksModel marks = new WorkingMarksModel();
        marks.mark("A");
        assertThrows(IllegalArgumentException.class,
                () -> marks.lead("B"),
                "a lead that is not in the set is a state no consumer"
                        + " could draw");
    }

    @Test
    void everyPublishedStateIsOneAConsumerCouldDraw() {
        WorkingMarksModel marks = new WorkingMarksModel();
        List<WorkingMarksModel.Change> heard = new ArrayList<>();
        marks.onChange(heard::add);

        marks.mark("A");
        marks.mark("B");
        marks.unmark("A");
        marks.clear();

        for (WorkingMarksModel.Change change : heard) {
            if (change.lead() == null) {
                assertTrue(change.isEmpty(),
                        "a state with no lead holds nothing: " + change);
            } else {
                assertTrue(change.marks().contains(change.lead()),
                        "the lead is always one of the marks: " + change);
            }
        }
        assertEquals(5, heard.size(),
                "the current state on subscribing, then one per change");
    }

    @Test
    void aListenerThatMarksWhileBeingToldDoesNotInterleaveTwoStories() {
        // The reentrancy the SelectionModel settled, and for the same
        // reason: a consumer that reacts by changing the model must
        // not have its reaction delivered before the change it is
        // reacting to.
        WorkingMarksModel marks = new WorkingMarksModel();
        List<String> order = new ArrayList<>();
        boolean[] reacted = {false};

        marks.onChange(change -> {
            order.add("first sees " + change.marks());
            if (!reacted[0] && change.marks().equals(List.of("A"))) {
                reacted[0] = true;
                marks.mark("B");
            }
        });
        marks.onChange(change -> order.add("second sees " + change.marks()));

        marks.mark("A");

        assertEquals(List.of(
                        "first sees []",
                        "second sees []",
                        "first sees [A]",
                        "second sees [A]",
                        "first sees [A, B]",
                        "second sees [A, B]"),
                order,
                "both consumers see every state, in one order, and the"
                        + " reaction arrives after the change that"
                        + " caused it");
    }

    @Test
    void theModelNeverReadsAheadOfTheEventBeingDelivered() {
        // Queueing the events was not enough (review). A listener
        // that marks something while being told about a mark moves
        // the model at once, so the next listener was handed the
        // older change and, if it asked the model, told a newer
        // state - it would draw one page and read another.
        WorkingMarksModel marks = new WorkingMarksModel();
        List<String> readByTheSecond = new ArrayList<>();
        boolean[] reacted = {false};

        marks.onChange(change -> {
            if (!reacted[0] && change.marks().equals(List.of("A"))) {
                reacted[0] = true;
                marks.mark("B");
            }
        });
        marks.onChange(change ->
                readByTheSecond.add(change.marks() + " reads as "
                        + marks.marks() + ", lead " + marks.lead()));

        marks.mark("A");

        assertEquals(List.of(
                        "[] reads as [], lead null",
                        "[A] reads as [A], lead A",
                        "[A, B] reads as [A, B], lead B"),
                readByTheSecond,
                "the model answers as the change being delivered, so a"
                        + " consumer that asks during delivery is told"
                        + " what it was just handed");
    }

    @Test
    void aModelStateAlwaysAgreesWithTheEventBeingDelivered() {
        WorkingMarksModel marks = new WorkingMarksModel();
        List<String> disagreements = new ArrayList<>();
        marks.onChange(change -> {
            if (!change.marks().equals(marks.marks())
                    || !java.util.Objects.equals(change.lead(),
                            marks.lead())) {
                disagreements.add(change + " against " + marks.marks()
                        + " lead " + marks.lead());
            }
        });

        marks.mark("A");
        marks.mark("B");
        marks.lead("A");
        marks.unmark("A");

        assertEquals(List.of(), disagreements,
                "a consumer that asks the model during delivery is told"
                        + " the same thing it was just handed");
    }

    @Test
    void aPageChangeRemovesWhatLeftItOnceAndInOneTransition() {
        WorkingMarksModel marks = new WorkingMarksModel();
        marks.mark("STAYS");
        marks.mark("GOES");
        List<WorkingMarksModel.Change> heard = new ArrayList<>();
        marks.onChange(heard::add);
        heard.clear();

        marks.pruneTo(pageHolding("STAYS"));

        assertEquals(1, heard.size(),
                "one coherent transition, not one event per mark: "
                        + heard);
        assertEquals(List.of("STAYS"), heard.get(0).marks());
        assertEquals("STAYS", heard.get(0).lead(),
                "and the lead moved with it, rather than pointing off"
                        + " the page for an instant");
    }

    @Test
    void aPageThatChangedNothingSaysNothing() {
        // An unrelated repaint must not publish: a table that redrew
        // its crosses on every paint would be a table flickering for
        // no reason.
        WorkingMarksModel marks = new WorkingMarksModel();
        marks.mark("STAYS");
        List<WorkingMarksModel.Change> heard = new ArrayList<>();
        marks.onChange(heard::add);
        heard.clear();

        marks.pruneTo(pageHolding("STAYS"));
        marks.pruneTo(pageHolding("STAYS"));

        assertEquals(List.of(), heard,
                "nothing left the page, so nothing is published");
    }

    @Test
    void marksNeverOutliveTheSession() {
        // Not a behaviour that can be asserted by calling something,
        // so it is asserted structurally: a model that wrote a
        // preference would have to know about one.
        String source;
        try {
            source = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/juranometria/page/WorkingMarksModel.java"));
        } catch (java.io.IOException e) {
            throw new AssertionError("the model's own source", e);
        }
        assertTrue(!source.contains("Preferences")
                        && !source.contains("prefs")
                        && !source.contains("java.io.File"),
                "working marks are ephemeral: no preferences, no"
                        + " observing lists, no notes, no export");
    }

    // ----------------------------------------------------------------

    /** A page holding exactly these identities. */
    private static PageContents pageHolding(String... identities) {
        List<PageEntry> entries = new ArrayList<>();
        for (String identity : identities) {
            entries.add(new PageEntry.DeepSky(
                    new juranometria.chart.DeepSkyObject(identity, List.of(),
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
}
