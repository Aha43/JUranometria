package juranometria.ui;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.render.ChartOptions;
import juranometria.render.ChartStructure;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transient emphasis behaves like a reading aid, not a mode (issue
 * #361).
 *
 * <p>The rules the ruling states, held one by one: emphasis survives
 * the reader's navigation and dies with anything that makes its
 * structure unavailable - a switched-off layer, a field the detail
 * policy keeps the layer off, a detached module. It is never
 * persisted, and it is independent of selection in both directions.
 */
class StructureEmphasisSessionTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private ChartComponent chart() {
        ChartComponent chart = new ChartComponent(Atlas.assembler(),
                ENGLISH);
        chart.setSize(900, 700);
        chart.setViewState(ChartViewState.DEFAULT);
        return chart;
    }

    @Test
    void emphasisSurvivesTheReadersNavigation() {
        ChartComponent chart = chart();
        chart.toggleEmphasis(ChartStructure.EQUATORIAL_GRID);
        assertEquals(java.util.Set.of(ChartStructure.EQUATORIAL_GRID),
                chart.emphasizedSet());

        chart.setViewState(new ChartViewState(
                new SkyPosition(271.0, -24.0), 42.0, 6.0));
        assertEquals(java.util.Set.of(ChartStructure.EQUATORIAL_GRID),
                chart.emphasizedSet(),
                "panning and zooming keep the structure the reader is"
                        + " following");
        // And nothing was persisted: the options the dialog edits and
        // the store saves carry no trace of it.
        assertEquals(ChartOptions.DEFAULTS, chart.chartOptions(),
                "emphasis lives beside the options, never inside them");
    }

    @Test
    void settlingIsTheSameCallAndAnUnavailableAskSettlesToo() {
        ChartComponent chart = chart();
        chart.toggleEmphasis(ChartStructure.EQUATORIAL_GRID);
        chart.clearEmphasis();
        assertTrue(chart.emphasizedSet().isEmpty(), "Normal settles the page");

        chart.toggleEmphasis(ChartStructure.MERIDIAN);
        assertTrue(chart.emphasizedSet().isEmpty(),
                "no module contributes a meridian here, so there is"
                        + " nothing to raise and no latent mode to"
                        + " hold");
    }

    @Test
    void aSwitchedOffLayerSettlesTheEmphasisImmediately() {
        ChartComponent chart = chart();
        chart.toggleEmphasis(ChartStructure.EQUATORIAL_GRID);
        assertEquals(java.util.Set.of(ChartStructure.EQUATORIAL_GRID),
                chart.emphasizedSet());

        ChartOptions on = ChartOptions.DEFAULTS;
        chart.setChartOptions(new ChartOptions(on.deepSkyObjects(),
                on.deepSkyLabels(), on.constellationFigures(),
                on.constellationBoundaries(), on.constellationNames(),
                on.starNames(), on.bayerLetters(), on.flamsteedNumbers(),
                false, on.titleBlock(), on.magnitudeKey(), on.galaxies(),
                on.openClusters(), on.globularClusters(), on.nebulae(),
                on.planetaryNebulae(), on.palette()));
        assertTrue(chart.emphasizedSet().isEmpty(),
                "a hidden grid cannot stay emphasized: the mode"
                        + " settles with the layer");
    }

    @Test
    void navigationKeepsWhatTheDetailPolicyStopsDrawing() {
        // #537, amending #361: crossing the policy's field in either
        // direction is navigation, not a choice. Figures and boundaries
        // are drawn at 42 degrees and not on the default 8-degree page.
        ChartComponent chart = chart();
        chart.setViewState(new ChartViewState(
                new SkyPosition(83.0, 0.0), 42.0, 8.0));
        chart.toggleEmphasis(ChartStructure.CONSTELLATION_FIGURES);
        chart.toggleEmphasis(ChartStructure.CONSTELLATION_BOUNDARIES);
        java.util.Set<ChartStructure> raised = java.util.Set.of(
                ChartStructure.CONSTELLATION_FIGURES,
                ChartStructure.CONSTELLATION_BOUNDARIES);
        assertEquals(raised, chart.emphasizedSet());

        chart.setViewState(new ChartViewState(
                new SkyPosition(83.0, 0.0), 8.0, 8.0));
        assertEquals(raised, chart.emphasizedSet(),
                "zooming past where the policy draws them keeps the choices");
        assertTrue(chart.emphasisAvailable(ChartStructure.CONSTELLATION_FIGURES)
                        && chart.emphasisAvailable(ChartStructure.CONSTELLATION_BOUNDARIES),
                "and both stay toggleable while their ink is absent");
        chart.toggleEmphasis(ChartStructure.CONSTELLATION_BOUNDARIES);
        assertEquals(java.util.Set.of(ChartStructure.CONSTELLATION_FIGURES),
                chart.emphasizedSet(), "unchecked while absent");
        chart.toggleEmphasis(ChartStructure.CONSTELLATION_BOUNDARIES);
        assertEquals(raised, chart.emphasizedSet(), "and raised again while absent");

        chart.setViewState(new ChartViewState(
                new SkyPosition(200.0, -60.0), 8.0, 8.0));
        assertEquals(raised, chart.emphasizedSet(), "panning keeps them");
        chart.setViewState(new ChartViewState(
                new SkyPosition(83.0, 0.0), 42.0, 8.0));
        assertEquals(raised, chart.emphasizedSet(),
                "and back where they are drawn, they are drawn emphasized");
    }

    @Test
    void switchingTheFiguresOffStillSettlesTheirEmphasis() {
        ChartComponent chart = chart();
        chart.setViewState(new ChartViewState(
                new SkyPosition(83.0, 0.0), 8.0, 8.0));
        chart.toggleEmphasis(ChartStructure.CONSTELLATION_FIGURES);
        assertEquals(java.util.Set.of(ChartStructure.CONSTELLATION_FIGURES),
                chart.emphasizedSet(), "raised where none are drawn (#537)");
        ChartOptions on = ChartOptions.DEFAULTS;
        chart.setChartOptions(new ChartOptions(on.deepSkyObjects(),
                on.deepSkyLabels(), false,
                on.constellationBoundaries(), on.constellationNames(),
                on.starNames(), on.bayerLetters(), on.flamsteedNumbers(),
                on.equatorialGrid(), on.titleBlock(), on.magnitudeKey(),
                on.galaxies(), on.openClusters(), on.globularClusters(),
                on.nebulae(), on.planetaryNebulae(), on.palette()));
        assertTrue(chart.emphasizedSet().isEmpty(),
                "switching the layer off is a choice: the emphasis settles (#361)");
        assertFalse(chart.emphasisAvailable(ChartStructure.CONSTELLATION_FIGURES),
                "and cannot be raised until the layer is back");
    }

    @Test
    void aDetachedModuleSettlesItsStructure() {
        ChartComponent chart = chart();
        ChartModuleHost host = new ChartModuleHost(chart,
                new SelectionModel(), request -> { });
        MeridianModule module = host.attach(new MeridianModule(
                new Observer(59.9, 10.7,
                        Instant.parse("2026-03-20T21:33:00Z"))));
        module.showing(true, true, true);

        chart.toggleEmphasis(ChartStructure.MERIDIAN);
        assertEquals(java.util.Set.of(ChartStructure.MERIDIAN),
                chart.emphasizedSet(),
                "the module's meridian is on the chart, so it can"
                        + " be raised");

        module.detach();
        assertTrue(chart.emphasizedSet().isEmpty(),
                "a detached module's structure settles immediately -"
                        + " never an invisible latent mode");
    }

    @Test
    void aHiddenLineSettlesItsStructure() {
        ChartComponent chart = chart();
        ChartModuleHost host = new ChartModuleHost(chart,
                new SelectionModel(), request -> { });
        MeridianModule module = host.attach(new MeridianModule(
                new Observer(59.9, 10.7,
                        Instant.parse("2026-03-20T21:33:00Z"))));
        module.showing(true, true, true);
        chart.toggleEmphasis(ChartStructure.MERIDIAN);

        module.showing(false, false, false);
        assertTrue(chart.emphasizedSet().isEmpty(),
                "hiding the lines settles the emphasis with them");
        module.detach();
    }

    @Test
    void selectionAndEmphasisNeverTouchEachOther() {
        ChartComponent chart = chart();
        chart.toggleEmphasis(ChartStructure.EQUATORIAL_GRID);

        chart.setWorkingSelection(java.util.List.of("star:hip-24436"),
                "star:hip-24436");
        assertEquals(java.util.Set.of(ChartStructure.EQUATORIAL_GRID),
                chart.emphasizedSet(),
                "selecting changes no emphasis");

        chart.toggleEmphasis(ChartStructure.CONSTELLATION_FIGURES);
        assertEquals(java.util.List.of("star:hip-24436"),
                chart.selectedMembers(),
                "and emphasizing changes no selection");
    }

    @Test
    void theMenuSaysWhatCanBeRaisedAndWhatIsRaised() {
        ChartComponent chart = chart();
        // Wide enough that the geography policy draws the figures.
        chart.setViewState(new ChartViewState(
                new SkyPosition(271.0, -24.0), 42.0, 6.0));
        chart.toggleEmphasis(ChartStructure.EQUATORIAL_GRID);

        AtlasToolbar bar = new AtlasToolbar(
                new ChartViewController(Atlas.assembler()::fits),
                new SearchField(Atlas.search(), Atlas.assembler(),
                        new ChartViewController(Atlas.assembler()::fits),
                        juranometria.ui.language.InterfaceText
                                .forLanguage("en")),
                juranometria.ui.language.InterfaceText.forLanguage("en"));
        bar.attachEmphasis(chart);
        javax.swing.JPopupMenu menu = bar.emphasisMenu(chart);

        // Normal, a separator, then the six structures in the ruled
        // order, each an independent checkmark.
        javax.swing.JMenuItem normal =
                (javax.swing.JMenuItem) menu.getComponent(0);
        assertTrue(normal.isEnabled(),
                "something is raised, so Normal has work to do");
        javax.swing.JCheckBoxMenuItem meridian =
                (javax.swing.JCheckBoxMenuItem) menu.getComponent(2);
        assertTrue(!meridian.isEnabled(),
                "no module offers a meridian, so it cannot be chosen");
        javax.swing.JCheckBoxMenuItem grid =
                (javax.swing.JCheckBoxMenuItem) menu.getComponent(4);
        assertTrue(grid.isEnabled() && grid.isSelected(),
                "the raised grid is checked and choosable");
        javax.swing.JCheckBoxMenuItem figures =
                (javax.swing.JCheckBoxMenuItem) menu.getComponent(7);
        assertTrue(figures.isEnabled() && !figures.isSelected(),
                "an unraised available structure is unchecked");

        // Toggling adds without touching the grid's membership;
        // toggling again removes only itself.
        chart.toggleEmphasis(ChartStructure.CONSTELLATION_FIGURES);
        assertEquals(java.util.Set.of(ChartStructure.EQUATORIAL_GRID,
                        ChartStructure.CONSTELLATION_FIGURES),
                chart.emphasizedSet(),
                "choosing a structure toggles only that structure");
        chart.toggleEmphasis(ChartStructure.CONSTELLATION_FIGURES);
        assertEquals(java.util.Set.of(ChartStructure.EQUATORIAL_GRID),
                chart.emphasizedSet(),
                "and toggling it again removes only itself");

        // With nothing raised, Normal rests.
        chart.clearEmphasis();
        assertTrue(!((javax.swing.JMenuItem) bar
                        .emphasisMenu(chart).getComponent(0))
                        .isEnabled(),
                "the settled page gives Normal nothing to do");
    }
}
