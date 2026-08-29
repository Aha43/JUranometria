package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.search.LocalSearch;
import juranometria.search.SearchResult;
import juranometria.ui.SearchField.Outcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SearchFieldTest {

    static final SkyPosition DATA_CENTRE = new SkyPosition(10.684708, 41.268750);
    static final SkyPosition M31_POSITION = new SkyPosition(10.684792, 41.269056);
    static final SkyPosition FIVE_DEGREES_OUT = new SkyPosition(10.684708, 46.268750);
    static final SkyPosition FAR_BEYOND = new SkyPosition(10.684708, 49.968750);

    /** A search field over three objects with distinct coverage behaviour. */
    private static final class Fixture {
        final SceneAssemblerTest.CountingCatalogue catalogue =
                new SceneAssemblerTest.CountingCatalogue();
        final SceneAssembler assembler =
                new SceneAssembler(catalogue, DATA_CENTRE, "Test chart", 10.0, 1.5);
        final ChartViewController controller = new ChartViewController(this::valid);
        final SearchField field;

        private boolean valid(ChartViewState state) {
            return assembler.fits(state);
        }

        Fixture() throws Exception {
            LocalSearch search = new LocalSearch(List.of(), List.of(
                    new DeepSkyObject("NGC 224", List.of("M 31", "Andromeda Galaxy"),
                            DsoType.GALAXY, M31_POSITION, 177.83, 69.66, 35.0, 3.44, 1),
                    new DeepSkyObject("FAR 1", List.of(), DsoType.GALAXY,
                            FIVE_DEGREES_OUT, 4.0, 2.0, 10.0, 12.0, 2),
                    new DeepSkyObject("EDGE 1", List.of(), DsoType.GALAXY,
                            FAR_BEYOND, 4.0, 2.0, 10.0, 12.0, 2)));
            SearchField[] holder = new SearchField[1];
            SwingUtilities.invokeAndWait(() ->
                    holder[0] = new SearchField(search, assembler, controller));
            field = holder[0];
        }

        Outcome handle(String query) throws Exception {
            Outcome[] outcome = new Outcome[1];
            SwingUtilities.invokeAndWait(() -> outcome[0] = field.handle(query));
            return outcome[0];
        }
    }

    @Test
    void enterOnAnExactMatchRecentresOnceThroughTheRealField() throws Exception {
        Fixture fixture = new Fixture();
        List<ChartViewState> seen = new java.util.ArrayList<>();
        fixture.controller.onChange(seen::add);

        SwingUtilities.invokeAndWait(() -> {
            fixture.field.setText("m 31");
            fixture.field.postActionEvent();
        });

        assertEquals(M31_POSITION, fixture.controller.state().centre());
        assertEquals(8.0, fixture.controller.state().fieldWidthDegrees(),
                "the fitting field width is preserved");
        assertEquals(2, seen.size(), "registration plus exactly one change");
    }

    @Test
    void coverageDrivenFieldAdjustmentTakesTheWidestFittingStep() throws Exception {
        Fixture fixture = new Fixture();
        assertEquals(Outcome.RECENTERED_NARROWER, fixture.handle("far 1"));
        assertEquals(FIVE_DEGREES_OUT, fixture.controller.state().centre());
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees(),
                "8 degrees does not fit at a 5-degree offset; 6 is the widest step");
        assertEquals(8.0, fixture.controller.state().limitingMagnitude(),
                "the magnitude limit is untouched");
    }

    @Test
    void anUnfittableResultLeavesTheChartUnchanged() throws Exception {
        Fixture fixture = new Fixture();
        ChartViewState before = fixture.controller.state();
        assertEquals(Outcome.NO_FIT, fixture.handle("edge 1"));
        assertSame(before, fixture.controller.state(), "the chart must not move");
    }

    @Test
    void unmatchedAndEmptyQueriesNeverMoveTheChart() throws Exception {
        Fixture fixture = new Fixture();
        ChartViewState before = fixture.controller.state();
        assertEquals(Outcome.NO_MATCH, fixture.handle("betelgeuse"));
        assertEquals(Outcome.EMPTY, fixture.handle("   "));
        assertSame(before, fixture.controller.state());
    }

    @Test
    void coordinatesFollowTheSameRecentreAndCoverageRules() throws Exception {
        Fixture fixture = new Fixture();
        assertEquals(Outcome.RECENTERED, fixture.handle("12.45 41.08"));
        assertEquals(12.45, fixture.controller.state().centre().raDegrees());

        assertEquals(Outcome.NO_FIT, fixture.handle("10.684708 49.968750"),
                "out-of-coverage coordinates are refused like objects");
    }

    @Test
    void keyboardListSelectionRunsTheSameApplyPolicy() throws Exception {
        Fixture fixture = new Fixture();
        // "1" partially matches all three; select the far object from the list.
        List<SearchResult> results = new LocalSearch(List.of(), List.of(
                new DeepSkyObject("FAR 1", List.of(), DsoType.GALAXY,
                        FIVE_DEGREES_OUT, 4.0, 2.0, 10.0, 12.0, 2))).search("far");
        SwingUtilities.invokeAndWait(() -> {
            JPopupMenu popup = fixture.field.resultsPopup(results);
            ((JMenuItem) popup.getComponent(0)).doClick();
        });
        assertEquals(FIVE_DEGREES_OUT, fixture.controller.state().centre());
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees());
    }

    @Test
    void zoomingOutOfCoverageAfterRecentringIsDisabledAndRefused() throws Exception {
        // Codex review P1: search opens a 5-degree offset at the fitting
        // 6-degree field; zoom out to 8 degrees would leave coverage. The
        // real button must be disabled, and the transition refused with no
        // notification even if forced.
        Fixture fixture = new Fixture();
        AtlasToolbar[] toolbar = new AtlasToolbar[1];
        SwingUtilities.invokeAndWait(() ->
                toolbar[0] = new AtlasToolbar(fixture.controller, fixture.field));

        assertEquals(Outcome.RECENTERED_NARROWER, fixture.handle("far 1"));
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees());

        List<ChartViewState> seen = new java.util.ArrayList<>();
        fixture.controller.onChange(seen::add);

        javax.swing.JButton zoomOut = findButton(toolbar[0], "Zoom out");
        SwingUtilities.invokeAndWait(() -> {
            org.junit.jupiter.api.Assertions.assertFalse(zoomOut.isEnabled(),
                    "zoom out must be disabled when the wider field leaves coverage");
            fixture.controller.zoomOut();
        });
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees(),
                "a forced zoom out is refused before notification");
        assertEquals(1, seen.size(), "only the registration callback fired");
        SwingUtilities.invokeAndWait(() ->
                org.junit.jupiter.api.Assertions.assertTrue(
                        findButton(toolbar[0], "Zoom in").isEnabled(),
                        "narrowing always stays inside coverage"));
    }

    private static javax.swing.JButton findButton(AtlasToolbar toolbar, String name) {
        for (java.awt.Component component : toolbar.getComponents()) {
            if (component instanceof javax.swing.JButton button && name.equals(
                    button.getAccessibleContext().getAccessibleName())) {
                return button;
            }
        }
        throw new AssertionError("no toolbar button named " + name);
    }

    @Test
    void resetRestoresTheDefaultAndClearsTheSearchField() throws Exception {
        Fixture fixture = new Fixture();
        AtlasToolbar[] toolbar = new AtlasToolbar[1];
        SwingUtilities.invokeAndWait(() ->
                toolbar[0] = new AtlasToolbar(fixture.controller, fixture.field));

        fixture.handle("far 1");
        SwingUtilities.invokeAndWait(() -> fixture.field.setText("far 1"));
        SwingUtilities.invokeAndWait(() -> {
            for (java.awt.Component component : toolbar[0].getComponents()) {
                if (component instanceof javax.swing.JButton button
                        && "Reset view".equals(
                                button.getAccessibleContext().getAccessibleName())) {
                    button.doClick();
                }
            }
        });
        assertEquals(ChartViewState.DEFAULT, fixture.controller.state());
        assertEquals("", fixture.field.getText(), "reset clears the search text");
    }
}
