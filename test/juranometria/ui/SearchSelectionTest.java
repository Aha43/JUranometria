package juranometria.ui;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.search.SearchResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Search and selection, as the gate decided (issue #170, review):
 * finding an object by name selects it.
 *
 * <p>This is the whole keyboard-only route into the inspector. There
 * is deliberately no cursor that walks from star to star - "the next
 * star" across a projected page is a design of its own - so a reader
 * without a pointer reaches an object by searching for it, and works
 * the panel from there. Without this, the inspector would be
 * unreachable for them altogether.
 */
class SearchSelectionTest {

    private record Fixture(SearchField field, ChartViewController navigation,
                           SelectionModel selection) {
    }

    private static Fixture fixture() throws Exception {
        ChartViewController navigation =
                new ChartViewController(Atlas.assembler()::fits);
        SelectionModel selection = new SelectionModel();
        SearchField[] field = new SearchField[1];
        SwingUtilities.invokeAndWait(() -> {
            field[0] = new SearchField(Atlas.search(), Atlas.assembler(),
                    navigation);
            field[0].setSelectionModel(selection);
        });
        return new Fixture(field[0], navigation, selection);
    }

    private static SearchResult find(String query) {
        return Atlas.search().search(query).get(0);
    }

    @Test
    void findingAStarByNameSelectsIt() throws Exception {
        Fixture fixture = fixture();
        SearchResult betelgeuse = find("betelgeuse");

        SwingUtilities.invokeAndWait(() -> fixture.field()
                .apply(betelgeuse));

        Selection.Object selected = assertInstanceOf(Selection.Object.class,
                fixture.selection().selection());
        assertEquals(Selection.Object.Kind.STAR, selected.kind());
        assertEquals(betelgeuse.identity(), selected.catalogueId(),
                "the star the reader looked up is the one selected");
        assertEquals(1, fixture.selection().candidates().size(),
                "unambiguously: they named it themselves");
    }

    @Test
    void findingADeepSkyObjectSelectsIt() throws Exception {
        Fixture fixture = fixture();
        SearchResult m42 = find("M42");

        SwingUtilities.invokeAndWait(() -> fixture.field().apply(m42));

        Selection.Object selected = assertInstanceOf(Selection.Object.class,
                fixture.selection().selection());
        assertEquals(Selection.Object.Kind.DEEP_SKY, selected.kind());
        assertEquals(m42.identity(), selected.catalogueId());
    }

    @Test
    void searchingCoordinatesSelectsNothingBecauseNothingWasFound()
            throws Exception {
        // A reader who asks for a place has not asked about an
        // object, and no object was found there.
        Fixture fixture = fixture();
        SwingUtilities.invokeAndWait(() ->
                fixture.field().apply(find("betelgeuse")));
        assertInstanceOf(Selection.Object.class,
                fixture.selection().selection());

        SwingUtilities.invokeAndWait(() ->
                fixture.field().apply(find("83.82 -5.39")));

        assertInstanceOf(Selection.None.class,
                fixture.selection().selection(),
                "coordinates clear the selection rather than pretending"
                        + " to have found something");
    }

    @Test
    void theChartStillMovesExactlyAsItAlwaysDid() throws Exception {
        // Selection is added beside search's behaviour, not instead
        // of it: the target, the title and the recentre are untouched.
        Fixture fixture = fixture();
        SearchResult betelgeuse = find("betelgeuse");

        SwingUtilities.invokeAndWait(() -> fixture.field()
                .apply(betelgeuse));

        assertEquals(betelgeuse.identity(),
                fixture.navigation().state().targetIdentity(),
                "the searched target is set as before");
        assertTrue(fixture.navigation().state().targetLabel()
                        .contains("Betelgeuse"),
                "and titles the page: "
                        + fixture.navigation().state().targetLabel());
        assertEquals(betelgeuse.position().raDegrees(),
                fixture.navigation().state().centre().raDegrees(), 1e-9,
                "and the chart recentred on it");
    }

    @Test
    void searchWorksWithNothingListening() throws Exception {
        // The selection model is optional: an application without one
        // must still search. The inspector is a consumer, never a
        // requirement.
        ChartViewController navigation =
                new ChartViewController(Atlas.assembler()::fits);
        SearchField[] field = new SearchField[1];
        SwingUtilities.invokeAndWait(() -> field[0] = new SearchField(
                Atlas.search(), Atlas.assembler(), navigation));

        SearchResult betelgeuse = find("betelgeuse");
        SwingUtilities.invokeAndWait(() -> field[0].apply(betelgeuse));

        assertEquals(betelgeuse.identity(),
                navigation.state().targetIdentity(),
                "search recentres with no selection model at all");
    }

    // ---- the working-selection route (issue #261) ----------------

    private record WorkingFixture(SearchField field,
                                  juranometria.chart.WorkingSelection working,
                                  juranometria.chart.SelectionMode mode) {
    }

    private static WorkingFixture workingFixture() throws Exception {
        ChartViewController navigation =
                new ChartViewController(Atlas.assembler()::fits);
        juranometria.chart.WorkingSelection working =
                new juranometria.chart.WorkingSelection();
        juranometria.chart.SelectionMode mode =
                new juranometria.chart.SelectionMode();
        SearchField[] field = new SearchField[1];
        SwingUtilities.invokeAndWait(() -> {
            field[0] = new SearchField(Atlas.search(), Atlas.assembler(),
                    navigation);
            field[0].setSelectionModel(new SelectionModel());
            field[0].setWorkingSelection(working, mode);
        });
        return new WorkingFixture(field[0], working, mode);
    }

    @Test
    void anOrdinarySearchReplacesTheWorkingSelection() throws Exception {
        // The decided search semantics: recentre as today, replace
        // the set with the found object, and it leads.
        WorkingFixture fixture = workingFixture();
        fixture.working().replaceWith(
                java.util.List.of("NGC 224", "NGC 221"), "NGC 221");

        SearchResult m42 = find("M42");
        SwingUtilities.invokeAndWait(() -> fixture.field().apply(m42));

        assertEquals(java.util.List.of(m42.identity()),
                fixture.working().members(),
                "the ordinary search is a change of mind about the"
                        + " whole set");
        assertEquals(m42.identity(), fixture.working().lead());
    }

    @Test
    void anAdditiveSearchAddsTheFoundObjectAndItLeads() throws Exception {
        WorkingFixture fixture = workingFixture();
        fixture.working().replaceWith(
                java.util.List.of("NGC 224"), "NGC 224");
        fixture.mode().accumulate(true);

        SearchResult m42 = find("M42");
        SwingUtilities.invokeAndWait(() -> fixture.field().apply(m42));

        assertEquals(java.util.List.of("NGC 224", m42.identity()),
                fixture.working().members(),
                "while gestures accumulate, search adds rather than"
                        + " replaces");
        assertEquals(m42.identity(), fixture.working().lead());
    }

    @Test
    void searchingCoordinatesEditsNoMembership() throws Exception {
        // A place is a question, not an edit - the set is not an
        // answer to be cleared by asking where something is.
        WorkingFixture fixture = workingFixture();
        fixture.working().replaceWith(
                java.util.List.of("NGC 224"), "NGC 224");

        SwingUtilities.invokeAndWait(() ->
                fixture.field().apply(find("83.82 -5.39")));

        assertEquals(java.util.List.of("NGC 224"),
                fixture.working().members(),
                "coordinates leave the working selection untouched");
    }
}
