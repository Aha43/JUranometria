package juranometria.chart;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared selection state (issue #169): one coherent transition
 * per change, consumers that can come and go safely, and no
 * knowledge of any user interface.
 */
class SelectionModelTest {

    private static Selection.Object star(String id, double ra, double dec) {
        return new Selection.Object(Selection.Object.Kind.STAR, id,
                new SkyPosition(ra, dec));
    }

    @Test
    void aNewSubscriberIsToldWhatIsAlreadySelected() {
        SelectionModel model = new SelectionModel();
        model.select(star("TYC 1-2-3", 10.0, 20.0));

        List<SelectionModel.Change> seen = new ArrayList<>();
        model.onChange(seen::add);

        assertEquals(1, seen.size(),
                "a subscriber never has to ask what it missed");
        assertEquals("TYC 1-2-3",
                ((Selection.Object) seen.get(0).selection()).catalogueId());
    }

    @Test
    void twoIndependentObserversSeeTheSameTransition() {
        // The seam the sprint's journey depends on, and the whole of
        // the "future module" provision: a listener list.
        SelectionModel model = new SelectionModel();
        List<SelectionModel.Change> inspector = new ArrayList<>();
        List<SelectionModel.Change> other = new ArrayList<>();
        model.onChange(inspector::add);
        model.onChange(other::add);

        model.select(star("TYC 9-9-9", 30.0, -10.0));

        assertEquals(inspector.size(), other.size(),
                "both consumers hear every change");
        assertEquals(inspector.get(inspector.size() - 1),
                other.get(other.size() - 1),
                "and hear exactly the same thing");
    }

    @Test
    void anObserverCanLeaveWithoutDisturbingTheOthers() {
        SelectionModel model = new SelectionModel();
        List<SelectionModel.Change> staying = new ArrayList<>();
        List<SelectionModel.Change> leaving = new ArrayList<>();
        model.onChange(staying::add);
        Runnable unsubscribe = model.onChange(leaving::add);

        unsubscribe.run();
        model.select(star("TYC 4-5-6", 1.0, 2.0));

        assertEquals(1, leaving.size(),
                "the departed consumer heard only its own arrival");
        assertEquals(2, staying.size(),
                "and the remaining one was not disturbed");
    }

    @Test
    void anObserverMayUnsubscribeWhileBeingToldSomething() {
        // A consumer that closes itself on the very change it is
        // hearing must not break the notification in progress.
        SelectionModel model = new SelectionModel();
        List<SelectionModel.Change> after = new ArrayList<>();
        Runnable[] handle = new Runnable[1];
        handle[0] = model.onChange(change -> {
            if (change.selection() instanceof Selection.Object) {
                handle[0].run();
            }
        });
        model.onChange(after::add);

        model.select(star("TYC 7-7-7", 5.0, 5.0));
        model.select(star("TYC 8-8-8", 6.0, 6.0));

        assertEquals(3, after.size(),
                "the other consumer heard its arrival and both changes");
    }


    @Test
    void aChangeMadeWhileTellingSomeoneArrivesAfterIt() {
        // Reproduced in review (P1): a consumer that selects B on
        // hearing A used to leave a second consumer holding A as its
        // last word while the model rested at B - permanently stale.
        SelectionModel model = new SelectionModel();
        Selection.Object a = star("A", 1.0, 1.0);
        Selection.Object b = star("B", 2.0, 2.0);
        List<String> second = new ArrayList<>();

        model.onChange(change -> {
            if (change.selection() instanceof Selection.Object object
                    && object.catalogueId().equals("A")) {
                model.select(b);
            }
        });
        model.onChange(change -> second.add(
                change.selection() instanceof Selection.Object object
                        ? object.catalogueId() : "none"));

        model.select(a);

        assertEquals(List.of("none", "A", "B"), second,
                "every consumer sees one order, and it is the order"
                        + " the changes happened in");
        assertEquals("B", ((Selection.Object) model.selection())
                        .catalogueId(),
                "and the last thing each consumer heard is where the"
                        + " model actually rests");
    }

    @Test
    void aChangeCannotDescribeAStateThatMakesNoSense() {
        // The public record is a contract of its own (review).
        Selection.Object one = star("A", 1.0, 1.0);
        assertThrows(IllegalArgumentException.class,
                () -> new SelectionModel.Change(one, List.of(), 0),
                "an index with no candidates");
        assertThrows(IllegalArgumentException.class,
                () -> new SelectionModel.Change(one, List.of(one), -1),
                "candidates with no index");
        assertThrows(IllegalArgumentException.class,
                () -> new SelectionModel.Change(one, List.of(one), 1),
                "an index past the end");
        assertThrows(IllegalArgumentException.class,
                () -> new SelectionModel.Change(star("B", 2.0, 2.0),
                        List.of(one), 0),
                "a current candidate that is not the selection");
    }

    @Test
    void anAmbiguousClickOffersEveryCandidateAndResolvesNothing() {
        SelectionModel model = new SelectionModel();
        List<Selection.Object> candidates = List.of(
                star("TYC 1-1-1", 10.0, 10.0),
                star("TYC 2-2-2", 10.01, 10.0),
                star("TYC 3-3-3", 10.02, 10.0));

        model.selectAmong(candidates);

        assertTrue(model.candidates().size() == 3);
        assertEquals(0, model.currentIndex());
        assertEquals(candidates.get(0), model.selection());

        model.chooseCandidate(2);
        assertEquals(candidates.get(2), model.selection(),
                "the reader chooses; the atlas does not choose for them");
        assertEquals(3, model.candidates().size(),
                "and the other candidates remain on offer");
        assertThrows(IndexOutOfBoundsException.class,
                () -> model.chooseCandidate(3));
    }

    @Test
    void emptySkyIsASelectionAndNotACandidate() {
        SelectionModel model = new SelectionModel();
        model.selectEmptySky(new SkyPosition(12.0, -3.0));

        assertTrue(model.selection() instanceof Selection.EmptySky);
        assertTrue(model.candidates().isEmpty(),
                "there is nothing to choose between");
        assertEquals(-1, model.currentIndex());
        assertFalse(new SelectionModel.Change(model.selection(),
                model.candidates(), model.currentIndex()).isAmbiguous());
    }

    @Test
    void aSelectionRefusesToBeHalfStated() {
        assertThrows(IllegalArgumentException.class,
                () -> new Selection.Object(Selection.Object.Kind.STAR, "  ",
                        new SkyPosition(1.0, 1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> new Selection.Object(Selection.Object.Kind.DEEP_SKY,
                        "NGC 1", null));
        assertThrows(IllegalArgumentException.class,
                () -> new Selection.EmptySky(null));
        assertThrows(IllegalArgumentException.class,
                () -> new SelectionModel().selectAmong(List.of()));
    }

    @Test
    void nothingHereKnowsAboutAUserInterface() throws Exception {
        // The state is UI-independent by construction, and this is
        // where that stops being a claim: neither source mentions
        // Swing, AWT, or the chart component.
        for (String source : List.of("Selection.java", "SelectionModel.java")) {
            String text = java.nio.file.Files.readString(
                    java.nio.file.Path.of("src/juranometria/chart/" + source));
            assertFalse(text.contains("javax.swing")
                            || text.contains("java.awt")
                            || text.contains("juranometria.ui"),
                    source + " must not know about any user interface");
        }
    }
}
