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
        chart.emphasize(ChartStructure.EQUATORIAL_GRID);
        assertEquals(ChartStructure.EQUATORIAL_GRID, chart.emphasized());

        chart.setViewState(new ChartViewState(
                new SkyPosition(271.0, -24.0), 42.0, 6.0));
        assertEquals(ChartStructure.EQUATORIAL_GRID, chart.emphasized(),
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
        chart.emphasize(ChartStructure.EQUATORIAL_GRID);
        chart.emphasize(null);
        assertNull(chart.emphasized(), "Normal settles the page");

        chart.emphasize(ChartStructure.MERIDIAN);
        assertNull(chart.emphasized(),
                "no module contributes a meridian here, so there is"
                        + " nothing to raise and no latent mode to"
                        + " hold");
    }

    @Test
    void aSwitchedOffLayerSettlesTheEmphasisImmediately() {
        ChartComponent chart = chart();
        chart.emphasize(ChartStructure.EQUATORIAL_GRID);
        assertEquals(ChartStructure.EQUATORIAL_GRID, chart.emphasized());

        ChartOptions on = ChartOptions.DEFAULTS;
        chart.setChartOptions(new ChartOptions(on.deepSkyObjects(),
                on.deepSkyLabels(), on.constellationFigures(),
                on.constellationBoundaries(), on.constellationNames(),
                on.starNames(), on.bayerLetters(), on.flamsteedNumbers(),
                false, on.titleBlock(), on.magnitudeKey(), on.galaxies(),
                on.openClusters(), on.globularClusters(), on.nebulae(),
                on.planetaryNebulae(), on.palette()));
        assertNull(chart.emphasized(),
                "a hidden grid cannot stay emphasized: the mode"
                        + " settles with the layer");
    }

    @Test
    void theDetailPolicySettlesWhatItStopsDrawing() {
        ChartComponent chart = chart();
        // Figures are drawn at 42 degrees; the default 8-degree page
        // is below the policy's narrowest figure field.
        chart.setViewState(new ChartViewState(
                new SkyPosition(83.0, 0.0), 42.0, 8.0));
        chart.emphasize(ChartStructure.CONSTELLATION_FIGURES);
        assertEquals(ChartStructure.CONSTELLATION_FIGURES,
                chart.emphasized());

        // Below the policy's narrowest figure field the page draws
        // no figures, so the emphasis settles with them.
        chart.setViewState(new ChartViewState(
                new SkyPosition(83.0, 0.0), 8.0, 8.0));
        assertNull(chart.emphasized(),
                "a field the policy keeps figures off cannot keep"
                        + " them emphasized");
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

        chart.emphasize(ChartStructure.MERIDIAN);
        assertEquals(ChartStructure.MERIDIAN, chart.emphasized(),
                "the module's meridian is on the chart, so it can"
                        + " be raised");

        module.detach();
        assertNull(chart.emphasized(),
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
        chart.emphasize(ChartStructure.MERIDIAN);

        module.showing(false, false, false);
        assertNull(chart.emphasized(),
                "hiding the lines settles the emphasis with them");
        module.detach();
    }

    @Test
    void selectionAndEmphasisNeverTouchEachOther() {
        ChartComponent chart = chart();
        chart.emphasize(ChartStructure.EQUATORIAL_GRID);

        chart.setWorkingSelection(java.util.List.of("star:hip-24436"),
                "star:hip-24436");
        assertEquals(ChartStructure.EQUATORIAL_GRID, chart.emphasized(),
                "selecting changes no emphasis");

        chart.emphasize(ChartStructure.CONSTELLATION_FIGURES);
        assertEquals(java.util.List.of("star:hip-24436"),
                chart.selectedMembers(),
                "and emphasizing changes no selection");
    }

    @Test
    void theMenuSaysWhatCanBeRaisedAndWhatIsRaised() {
        ChartComponent chart = chart();
        chart.emphasize(ChartStructure.EQUATORIAL_GRID);

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
        // order.
        javax.swing.JRadioButtonMenuItem normal =
                (javax.swing.JRadioButtonMenuItem) menu.getComponent(0);
        assertTrue(!normal.isSelected(),
                "something is raised, so Normal is not marked");
        javax.swing.JRadioButtonMenuItem meridian =
                (javax.swing.JRadioButtonMenuItem) menu.getComponent(2);
        assertTrue(!meridian.isEnabled(),
                "no module offers a meridian, so it cannot be chosen");
        javax.swing.JRadioButtonMenuItem grid =
                (javax.swing.JRadioButtonMenuItem) menu.getComponent(4);
        assertTrue(grid.isEnabled() && grid.isSelected(),
                "the raised grid is marked and choosable");

        // The choice rule the items are wired to, held by name:
        // the active target settles, anything else raises.
        assertNull(AtlasToolbar.chosen(
                        ChartStructure.EQUATORIAL_GRID,
                        ChartStructure.EQUATORIAL_GRID),
                "choosing the active target again settles the page");
        assertEquals(ChartStructure.MERIDIAN, AtlasToolbar.chosen(
                        ChartStructure.EQUATORIAL_GRID,
                        ChartStructure.MERIDIAN),
                "choosing another raises it instead");

        // And with nothing raised, Normal is the marked state.
        chart.emphasize(null);
        assertTrue(((javax.swing.JRadioButtonMenuItem) bar
                        .emphasisMenu(chart).getComponent(0)).isSelected(),
                "the settled page reads as Normal");
    }
}
