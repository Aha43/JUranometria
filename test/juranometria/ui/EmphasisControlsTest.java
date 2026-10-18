package juranometria.ui;

import java.time.Instant;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.render.ChartStructure;
import juranometria.sky.Observer;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Controller's Emphasis group (issue #525): one immediate box per
 * choice of the toolbar's Emphasis popup, over the chart's one raised
 * set - driven through the group's own press seams, {@code choose} and
 * {@code settle}, a headless content test of the #443 kind - a change from either host shows in the other at once; the
 * ecliptic and the meridian raised and the grid lowered in one visit;
 * an unavailable structure's box unavailable; building it changes
 * nothing; the words in both languages; no access letters.
 */
class EmphasisControlsTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");

    /** The packaged combinations journey's page (#361): all six line structures available. */
    private static ChartComponent[] chartWithModules() throws Exception {
        ChartComponent[] chart = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            chart[0] = new ChartComponent(Atlas.assembler(), PageText.in(EN));
            chart[0].setSize(900, 700);
            chart[0].setViewState(new ChartViewState(new SkyPosition(83.0, 0.0), 42.0, 6.0));
            ChartModuleHost host = new ChartModuleHost(chart[0],
                    new juranometria.chart.SelectionModel(), request -> { });
            MeridianModule meridian = host.attach(new MeridianModule(new Observer(80.0, -58.7,
                    Instant.parse("2026-03-20T21:33:00Z"))));
            meridian.showing(true, true, false);
            host.attach(new EclipticModule()).showing(true);
        });
        return chart;
    }

    @Test
    void buildingTheGroupChangesNothingAndShowsTheChart() throws Exception {
        ChartComponent[] chart = chartWithModules();
        chart[0].toggleEmphasis(ChartStructure.EQUATORIAL_GRID);
        EmphasisControls[] group = new EmphasisControls[1];
        SwingUtilities.invokeAndWait(() -> group[0] = new EmphasisControls(chart[0], EN));
        assertEquals(Set.of(ChartStructure.EQUATORIAL_GRID), chart[0].emphasizedSet(),
                "building the group changes no chart setting");
        assertTrue(group[0].box(ChartStructure.EQUATORIAL_GRID).isSelected());
        assertFalse(group[0].box(ChartStructure.MERIDIAN).isSelected());
        assertTrue(group[0].normal().isEnabled(), "something is raised, so Normal has work");
        assertFalse(group[0].box(ChartStructure.SOLAR_SYSTEM_LABELS).isEnabled(),
                "no body is drawn: the label class cannot be chosen");
        SwingUtilities.invokeAndWait(() -> group[0].inController());
        assertEquals(Set.of(ChartStructure.EQUATORIAL_GRID), chart[0].emphasizedSet(),
                "and showing it changes nothing either");
    }

    @Test
    void theEclipticAndTheMeridianUpAndTheGridDownInOneVisit() throws Exception {
        ChartComponent[] chart = chartWithModules();
        chart[0].toggleEmphasis(ChartStructure.EQUATORIAL_GRID);
        EmphasisControls[] group = new EmphasisControls[1];
        SwingUtilities.invokeAndWait(() -> {
            group[0] = new EmphasisControls(chart[0], EN);
            group[0].choose(ChartStructure.ECLIPTIC);
            group[0].choose(ChartStructure.MERIDIAN);
            group[0].choose(ChartStructure.EQUATORIAL_GRID);
        });
        assertEquals(Set.of(ChartStructure.MERIDIAN, ChartStructure.ECLIPTIC),
                chart[0].emphasizedSet(), "three immediate changes, one visit");
        for (ChartStructure s : ChartStructure.values()) {
            assertEquals(chart[0].emphasizedSet().contains(s), group[0].box(s).isSelected(),
                    s + " shows the chart's answer");
        }
        SwingUtilities.invokeAndWait(() -> group[0].settle());
        assertEquals(Set.of(), chart[0].emphasizedSet(), "Normal settles the whole set");
        assertFalse(group[0].normal().isEnabled());
    }

    @Test
    void theToolbarsPopupAndTheGroupAreOneAuthority() throws Exception {
        ChartComponent[] chart = chartWithModules();
        EmphasisControls[] group = new EmphasisControls[1];
        SwingUtilities.invokeAndWait(() -> group[0] = new EmphasisControls(chart[0], EN));
        // The popup's figures row (Normal, separator, then the structures).
        // What the popup's figures row does when chosen (ChartActions).
        SwingUtilities.invokeAndWait(() -> chart[0].toggleEmphasis(
                ChartStructure.CONSTELLATION_FIGURES));
        assertTrue(group[0].box(ChartStructure.CONSTELLATION_FIGURES).isSelected(),
                "a change in the popup shows in the group at once");
        SwingUtilities.invokeAndWait(() -> group[0].choose(ChartStructure.HORIZON));
        javax.swing.JPopupMenu menu = ChartActions.emphasisMenu(chart[0], EN);
        assertTrue(((javax.swing.JCheckBoxMenuItem) menu.getComponent(5)).isSelected(),
                "and a change in the group shows in the popup the next time it opens");
    }

    @Test
    void aStructureNavigationStopsDrawingKeepsItsBox() throws Exception {
        // #537, amending #361: navigation is not a choice.
        ChartComponent[] chart = chartWithModules();
        EmphasisControls[] group = new EmphasisControls[1];
        SwingUtilities.invokeAndWait(() -> {
            group[0] = new EmphasisControls(chart[0], EN);
            group[0].choose(ChartStructure.CONSTELLATION_FIGURES);
            // Narrow the field past where the figures are drawn.
            chart[0].setViewState(new ChartViewState(new SkyPosition(83.0, 0.0), 1.0, 8.0));
        });
        JCheckBox figures = group[0].box(ChartStructure.CONSTELLATION_FIGURES);
        assertTrue(chart[0].emphasizedSet().contains(ChartStructure.CONSTELLATION_FIGURES),
                "the choice survives the zoom");
        assertTrue(figures.isSelected() && figures.isEnabled(),
                "and its box stays checked and available");
        JCheckBox boundaries = group[0].box(ChartStructure.CONSTELLATION_BOUNDARIES);
        assertTrue(boundaries.isEnabled(),
                "an unchecked structure absent from this field may be raised");
        SwingUtilities.invokeAndWait(() ->
                group[0].choose(ChartStructure.CONSTELLATION_BOUNDARIES));
        assertTrue(boundaries.isSelected()
                && chart[0].emphasizedSet().contains(ChartStructure.CONSTELLATION_BOUNDARIES));
    }

    @Test
    void theWordsInBothLanguagesWithNoAccessLetters() throws Exception {
        ChartComponent[] chart = chartWithModules();
        for (InterfaceText said : new InterfaceText[] {EN, NB}) {
            EmphasisControls[] group = new EmphasisControls[1];
            SwingUtilities.invokeAndWait(() -> group[0] = new EmphasisControls(chart[0], said));
            for (ChartStructure s : ChartStructure.values()) {
                JCheckBox box = group[0].box(s);
                assertEquals(said.say("emphasis." + s.token()), box.getText());
                assertEquals(box.getText(), box.getAccessibleContext().getAccessibleName());
                assertTrue(box.getAccessibleContext().getAccessibleDescription()
                        .contains(box.getText()), "described with its own name");
                assertEquals(0, box.getMnemonic(), "no access letter in a Controller group");
                assertTrue(box.isFocusable(), "reached by keyboard");
            }
        }
        assertEquals("Fremheving", NB.say("emphasis.title"));
        assertEquals("Solsystemetiketter", NB.say("emphasis.solar-system-labels"));
    }
}
