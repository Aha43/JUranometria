package juranometria.page;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.WorkingSelection;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lead, fed to the chart's singular selection (issue #215,
 * retargeted to the session model by issue #261).
 *
 * <p>A reader selects several objects and reads the facts of one.
 * The chart already says "this is the object being read about"
 * exactly one way, and teaching that model about sets would put
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
        WorkingSelection working = new WorkingSelection();
        SelectionModel selection = new SelectionModel();
        PageContents page = pageContents();
        LeadSelection.connect(working, selection, () -> page);

        working.add("NGC 224");
        Selection.Object first = assertInstanceOf(Selection.Object.class,
                selection.selection());
        assertEquals("NGC 224", first.catalogueId());

        working.add("NGC 205");
        assertEquals("NGC 205", assertInstanceOf(Selection.Object.class,
                selection.selection()).catalogueId(),
                "the newest member leads, and the selection follows it");

        assertEquals(List.of("NGC 224", "NGC 205"), working.members(),
                "while the working set keeps both: the selection is"
                        + " singular and the set is not");
        assertEquals(1, selection.candidates().size(),
                "and the selection is not quietly turned into a list of"
                        + " everything selected");
    }

    @Test
    void theSelectionDoesNotSilentlyMarkAnything() {
        // One way only. The answering model answers questions; the
        // gestures that edit membership write the working selection
        // themselves, and the bridge never writes backwards.
        WorkingSelection working = new WorkingSelection();
        SelectionModel selection = new SelectionModel();
        PageContents page = pageContents();
        LeadSelection.connect(working, selection, () -> page);

        selection.select(LeadSelection.selectable(page, "NGC 224")
                .orElseThrow());

        assertTrue(working.members().isEmpty(),
                "selecting is not membership: " + working.members());
    }

    @Test
    void anEmptySetSelectsNothing() {
        WorkingSelection working = new WorkingSelection();
        SelectionModel selection = new SelectionModel();
        PageContents page = pageContents();
        LeadSelection.connect(working, selection, () -> page);

        working.add("NGC 224");
        working.clear();

        assertEquals(Selection.NOTHING, selection.selection(),
                "nothing is selected, so nothing is being read about");
    }

    @Test
    void aLeadTheModelAlreadyAnswersIsLeftAlone() {
        // The bridge's deliberate silence: a click that offered
        // candidates has said more than the lead's identity, and
        // re-asserting the same lead would collapse the choice the
        // reader is in the middle of making.
        WorkingSelection working = new WorkingSelection();
        SelectionModel selection = new SelectionModel();
        PageContents page = pageContents();
        LeadSelection.connect(working, selection, () -> page);

        Selection.Object first =
                LeadSelection.selectable(page, "NGC 224").orElseThrow();
        Selection.Object second =
                LeadSelection.selectable(page, "NGC 205").orElseThrow();
        selection.selectAmong(List.of(first, second));

        working.add("NGC 224");

        assertEquals(2, selection.candidates().size(),
                "the candidates survive: the model was already"
                        + " answering with this lead");
        assertEquals("NGC 224", assertInstanceOf(Selection.Object.class,
                selection.selection()).catalogueId());

        // A lead the model is NOT answering is fed to it, which is
        // what collapses a finished click's chooser.
        working.add("NGC 205");
        assertEquals(1, selection.candidates().size(),
                "a membership transition from another gesture drives"
                        + " the answer, and the stale chooser closes");
        assertEquals("NGC 205", assertInstanceOf(Selection.Object.class,
                selection.selection()).catalogueId());
    }

    @Test
    void anOffPageLeadIsStillAnswered() {
        // Navigation never mutates the set (#258), so a lead can be
        // off the page. Every member joined through a gesture on the
        // page that showed it, so the bridge learned where it is -
        // and the facts panel says "not on this page any more"
        // rather than going blank while the working set names a lead.
        WorkingSelection working = new WorkingSelection();
        SelectionModel selection = new SelectionModel();
        List<PageContents> current = new ArrayList<>();
        current.add(pageContents());
        LeadSelection.connect(working, selection, () -> current.get(0));

        working.add("NGC 224");
        working.add("NGC 205");
        assertEquals(List.of("NGC 224", "NGC 205"), working.members());

        // The reader moves the chart to an empty page; the set stays.
        current.set(0, PageContents.EMPTY);
        assertEquals(List.of("NGC 224", "NGC 205"), working.members(),
                "the page changed and the membership did not");

        // A lead change to a member the page no longer holds still
        // reaches the answering model, resolved from what the bridge
        // learned when the page held it.
        working.lead("NGC 224");
        assertEquals("NGC 224", assertInstanceOf(Selection.Object.class,
                selection.selection()).catalogueId(),
                "the off-page lead is answered, not dropped");
    }

    @Test
    void disconnectingLeavesTheSelectionAlone() {
        WorkingSelection working = new WorkingSelection();
        SelectionModel selection = new SelectionModel();
        PageContents page = pageContents();
        Runnable disconnect = LeadSelection.connect(working, selection,
                () -> page);

        working.add("NGC 224");
        disconnect.run();
        working.add("NGC 205");

        assertEquals("NGC 224", assertInstanceOf(Selection.Object.class,
                selection.selection()).catalogueId(),
                "a released bridge stops driving the chart: the seam is"
                        + " released, not merely ignored");
    }
}
