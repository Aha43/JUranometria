package juranometria.app;

import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.WorkingSelection;
import juranometria.page.PageContents;
import juranometria.page.PageInventory;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Inspector's <strong>Working set</strong> section (issue #261,
 * mock-ups reviewed by the #258 gate): the complete cross-page set
 * in joining order, the lead named, off-page members labelled in
 * words, a per-member remove, choose-to-lead, and Clear selection.
 * The gate's irremovable-member mutation check lives here: a member
 * whose remove control does not remove it fails these tests.
 */
class InspectorWorkingSetTest {

    private static ChartScene page() {
        return Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(10.68, 41.27), 8.0, 8.0,
                        null, null), 900, 700);
    }

    private record Fixture(InspectorPanel panel, SelectionModel model,
                           WorkingSelection working) {
    }

    private static Fixture fixture() throws Exception {
        ChartScene scene = page();
        PageContents inventory =
                PageInventory.of(scene, ChartOptions.DEFAULTS);
        SelectionModel model = new SelectionModel();
        WorkingSelection working = new WorkingSelection();
        InspectorPanel[] panel = new InspectorPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new InspectorPanel(model, () -> scene,
                    () -> ChartOptions.DEFAULTS, chosen -> { });
            panel[0].showWorkingSet(working, () -> inventory);
        });
        return new Fixture(panel[0], model, working);
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    void theSectionListsEveryMemberWithLeadAndOffPageInWords()
            throws Exception {
        Fixture fixture = fixture();
        assertFalse(fixture.panel().workingSetShown(),
                "an empty set shows no section - nothing to manage");

        fixture.working().add("NGC 224");
        fixture.working().add("NGC 1976");   // wide Orion: off this page
        fixture.working().add("NGC 221");
        flush();

        assertTrue(fixture.panel().workingSetShown());
        assertEquals(List.of("NGC 224",
                        "NGC 1976 — off this page",
                        "◉ NGC 221"),
                fixture.panel().workingSetLines(),
                "every member across pages in joining order, the lead"
                        + " marked, the off-page state said in words"
                        + " rather than colour alone");
    }

    @Test
    void choosingAMemberMakesItTheLeadAndRemovesNothing()
            throws Exception {
        Fixture fixture = fixture();
        fixture.working().add("NGC 224");
        fixture.working().add("NGC 221");
        flush();

        SwingUtilities.invokeAndWait(() -> fixture.panel()
                .workingSetMemberButton("NGC 224").doClick());
        flush();

        assertEquals("NGC 224", fixture.working().lead());
        assertEquals(List.of("NGC 224", "NGC 221"),
                fixture.working().members(),
                "a lead change is never a silent removal");
        assertEquals(List.of("◉ NGC 224", "NGC 221"),
                fixture.panel().workingSetLines());
    }

    @Test
    void everyMemberIsRemovableAndTheLeadRulesHold() throws Exception {
        // The irremovable-member mutation check: each ✕ removes its
        // own member and only that member, and removing the lead
        // passes the lead to the last-marked remaining member.
        Fixture fixture = fixture();
        fixture.working().add("NGC 224");
        fixture.working().add("NGC 1976");
        fixture.working().add("NGC 221");
        flush();

        SwingUtilities.invokeAndWait(() -> fixture.panel()
                .workingSetRemoveButton("NGC 221").doClick());
        flush();
        assertEquals(List.of("NGC 224", "NGC 1976"),
                fixture.working().members());
        assertEquals("NGC 1976", fixture.working().lead(),
                "removing the lead passes it to the last-marked"
                        + " remaining member - off this page or not");

        SwingUtilities.invokeAndWait(() -> fixture.panel()
                .workingSetRemoveButton("NGC 1976").doClick());
        flush();
        assertEquals(List.of("NGC 224"), fixture.working().members());

        SwingUtilities.invokeAndWait(() -> fixture.panel()
                .workingSetRemoveButton("NGC 224").doClick());
        flush();
        assertTrue(fixture.working().members().isEmpty(),
                "every member is removable, one by one");
        assertNull(fixture.working().lead());
        assertFalse(fixture.panel().workingSetShown());
    }

    @Test
    void clearSelectionEmptiesTheWholeSetExplicitly() throws Exception {
        Fixture fixture = fixture();
        fixture.working().add("NGC 224");
        fixture.working().add("NGC 1976");
        flush();

        SwingUtilities.invokeAndWait(() -> fixture.panel()
                .clearSelectionButton().doClick());
        flush();

        assertTrue(fixture.working().members().isEmpty(),
                "one action, one name: the whole set empties");
        assertFalse(fixture.panel().workingSetShown());
    }

    @Test
    void theSectionSpeaksToAssistiveTechnology() throws Exception {
        Fixture fixture = fixture();
        fixture.working().add("NGC 1976");
        fixture.working().add("NGC 224");
        flush();

        assertEquals("NGC 1976, off this page",
                fixture.panel().workingSetMemberButton("NGC 1976")
                        .getAccessibleContext().getAccessibleName(),
                "off-page state travels with the accessible name");
        assertEquals("NGC 224, lead",
                fixture.panel().workingSetMemberButton("NGC 224")
                        .getAccessibleContext().getAccessibleName());
        assertEquals("Remove NGC 1976",
                fixture.panel().workingSetRemoveButton("NGC 1976")
                        .getAccessibleContext().getAccessibleName());
    }
}
