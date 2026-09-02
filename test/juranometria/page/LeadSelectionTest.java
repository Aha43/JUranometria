package juranometria.page;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lead, fed to the chart's singular selection (issue #215).
 *
 * <p>A reader marks several objects and reads the facts of one. The
 * chart already says "this is the object being read about" exactly
 * one way, and teaching that model about sets would put
 * multi-selection into something every other surface depends on
 * being singular. So the lead is fed to it, and the set keeps its
 * own seam.
 */
class LeadSelectionTest {

    private static ChartScene defaultPage() {
        return Atlas.assembler().assemble(ChartViewState.DEFAULT, 900, 700);
    }

    private static PageContents pageContents() {
        return PageInventory.of(defaultPage(), ChartOptions.DEFAULTS);
    }

    @Test
    void theLeadBecomesTheSelectionAndTheSetStaysWhereItIs() {
        WorkingMarksModel marks = new WorkingMarksModel();
        SelectionModel selection = new SelectionModel();
        PageContents page = pageContents();
        LeadSelection.connect(marks, selection, () -> page);

        marks.mark("NGC 224");
        Selection.Object first = assertInstanceOf(Selection.Object.class,
                selection.selection());
        assertEquals("NGC 224", first.catalogueId());

        marks.mark("NGC 205");
        assertEquals("NGC 205", assertInstanceOf(Selection.Object.class,
                selection.selection()).catalogueId(),
                "the newest mark leads, and the selection follows it");

        assertEquals(List.of("NGC 224", "NGC 205"), marks.marks(),
                "while the marked set keeps both: the selection is"
                        + " singular and the set is not");
        assertEquals(1, selection.candidates().size(),
                "and the selection is not quietly turned into a list of"
                        + " everything marked");
    }

    @Test
    void theSelectionDoesNotSilentlyMarkAnything() {
        // One way only. A reader who clicks the chart has selected
        // something, not marked it - and if selecting marked things,
        // a page would fill with crosses nobody asked for.
        WorkingMarksModel marks = new WorkingMarksModel();
        SelectionModel selection = new SelectionModel();
        PageContents page = pageContents();
        LeadSelection.connect(marks, selection, () -> page);

        selection.select(LeadSelection.selectable(page, "NGC 224")
                .orElseThrow());

        assertTrue(marks.marks().isEmpty(),
                "selecting is not marking: " + marks.marks());
    }

    @Test
    void anEmptySetSelectsNothing() {
        WorkingMarksModel marks = new WorkingMarksModel();
        SelectionModel selection = new SelectionModel();
        PageContents page = pageContents();
        LeadSelection.connect(marks, selection, () -> page);

        marks.mark("NGC 224");
        marks.clear();

        assertEquals(Selection.NOTHING, selection.selection(),
                "nothing is marked, so nothing is being read about");
    }

    @Test
    void aLeadThatLeftThePageSelectsNothingRatherThanAGuess() {
        // Pruning removes it from the set; the bridge must not leave
        // the reader's facts panel showing an object that is no
        // longer on the page.
        WorkingMarksModel marks = new WorkingMarksModel();
        SelectionModel selection = new SelectionModel();
        List<PageContents> current = new ArrayList<>();
        current.add(pageContents());
        LeadSelection.connect(marks, selection, () -> current.get(0));

        marks.mark("NGC 224");
        assertInstanceOf(Selection.Object.class, selection.selection());

        // The reader moves the chart to an empty page.
        current.set(0, PageContents.EMPTY);
        marks.pruneTo(PageContents.EMPTY);

        assertTrue(marks.marks().isEmpty(), "the mark left with the page");
        assertEquals(Selection.NOTHING, selection.selection(),
                "and the facts went with it, in the same transition");
    }

    @Test
    void disconnectingLeavesTheSelectionAlone() {
        WorkingMarksModel marks = new WorkingMarksModel();
        SelectionModel selection = new SelectionModel();
        PageContents page = pageContents();
        Runnable disconnect = LeadSelection.connect(marks, selection,
                () -> page);

        marks.mark("NGC 224");
        disconnect.run();
        marks.mark("NGC 205");

        assertEquals("NGC 224", assertInstanceOf(Selection.Object.class,
                selection.selection()).catalogueId(),
                "a removed module stops driving the chart: the seam is"
                        + " released, not merely ignored");
    }
}
