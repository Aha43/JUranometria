package juranometria.app;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the inspector says (issue #170): the reviewed facts, the
 * catalogue's silences stated as silences, and no route from the
 * panel to the chart except the one the reader presses.
 */
class InspectorPanelTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private static ChartScene page() {
        return juranometria.app.Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(10.68, 41.27), 8.0, 8.0,
                        null, null), 900, 700);
    }

    private record Fixture(InspectorPanel panel, SelectionModel model,
                           ChartScene scene, List<Selection> centred) {
    }

    private static Fixture fixture() throws Exception {
        ChartScene scene = page();
        SelectionModel model = new SelectionModel();
        List<Selection> centred = new ArrayList<>();
        InspectorPanel[] panel = new InspectorPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new InspectorPanel(
                model, () -> scene, centred::add));
        return new Fixture(panel[0], model, scene, centred);
    }

    private static ChartRenderer.DrawnMark firstStar(ChartScene scene) {
        return RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS).stream()
                .filter(mark -> mark.star() != null)
                .findFirst().orElseThrow();
    }

    private static ChartRenderer.DrawnMark firstDeepSky(ChartScene scene) {
        return RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS).stream()
                .filter(mark -> mark.deepSky() != null)
                .findFirst().orElseThrow();
    }

    @Test
    void withNothingSelectedItInvitesRatherThanAccuses() throws Exception {
        Fixture fixture = fixture();
        assertEquals("Nothing selected", fixture.panel().lines().get(0));
        assertTrue(String.join(" ", fixture.panel().lines())
                        .contains("Click a star"),
                "it says what to do: " + fixture.panel().lines());
    }

    @Test
    void aStarShowsItsDesignationsMagnitudeAndPlace() throws Exception {
        Fixture fixture = fixture();
        ChartRenderer.DrawnMark mark = firstStar(fixture.scene());
        fixture.model().select(ChartHitTest.selectionFor(mark));

        List<String> lines = fixture.panel().lines();
        String all = String.join(" | ", lines);
        assertTrue(all.contains(mark.star().id()),
                "the catalogue identity is shown: " + all);
        assertTrue(all.contains("visual magnitude"),
                "the magnitude names its band: " + all);
        assertTrue(all.contains("ICRS J2000"),
                "and the frame its coordinates are in: " + all);
        assertTrue(all.contains("h ") && all.contains("′"),
                "with real coordinates: " + all);
    }

    @Test
    void aDeepSkyObjectNeverInventsWhatTheCatalogueDoesNotRecord()
            throws Exception {
        // 19.4% of the pack has no position angle and 68.1% no visual
        // magnitude. The panel must say so rather than print a zero.
        Fixture fixture = fixture();
        ChartRenderer.DrawnMark mark = firstDeepSky(fixture.scene());
        fixture.model().select(ChartHitTest.selectionFor(mark));

        String all = String.join(" | ", fixture.panel().lines());
        DeepSkyObject dso = mark.deepSky();
        switch (dso.recorded().band()) {
            case VISUAL -> assertTrue(all.contains("visual magnitude"), all);
            case BLUE -> assertTrue(all.contains("blue magnitude")
                    && all.contains("no V recorded"), all);
            case NONE -> assertTrue(all.contains("magnitude not recorded"),
                    all);
        }
        if (!dso.recorded().hasSize()) {
            assertTrue(all.contains("size not recorded"), all);
        }
        if (dso.recorded().hasSize()
                && !dso.recorded().hasPositionAngle()) {
            assertTrue(all.contains("orientation not recorded"), all);
        }
        assertFalse(all.contains("OTHER") || all.contains("GALAXY"),
                "the type reads as words, not as an enum: " + all);
    }

    @Test
    void everyKindOfSilenceHasItsOwnSentence() {
        // The three unknown cases, on objects built to have them, so
        // the sentences themselves are covered whatever the pack
        // happens to carry near M31.
        DeepSkyObject noPhotometry = new DeepSkyObject("NGC 0001",
                List.of(), juranometria.chart.DsoType.GALAXY,
                new SkyPosition(1.0, 1.0), 2.0, 1.0, 30.0, Double.NaN, 3,
                new DeepSkyObject.Recorded(2.0, 1.0, 30.0,
                        DeepSkyObject.Recorded.Band.NONE));
        DeepSkyObject blueOnly = new DeepSkyObject("NGC 0002",
                List.of(), juranometria.chart.DsoType.GALAXY,
                new SkyPosition(1.0, 1.0), 2.0, 1.0, 0.0, 14.2, 3,
                new DeepSkyObject.Recorded(2.0, null, null,
                        DeepSkyObject.Recorded.Band.BLUE));
        DeepSkyObject nothingKnown = new DeepSkyObject("NGC 0003",
                List.of(), juranometria.chart.DsoType.OTHER,
                new SkyPosition(1.0, 1.0), 1.0, 1.0, 0.0, Double.NaN, 3,
                DeepSkyObject.Recorded.NOTHING);

        assertEquals("magnitude not recorded",
                InspectorPanel.magnitudeLine(noPhotometry));
        assertTrue(InspectorPanel.magnitudeLine(blueOnly)
                .contains("blue magnitude; no V recorded"));
        assertEquals("size not recorded",
                InspectorPanel.sizeLine(nothingKnown));
        assertTrue(InspectorPanel.sizeLine(blueOnly)
                        .contains("orientation not recorded"),
                InspectorPanel.sizeLine(blueOnly));
        assertTrue(InspectorPanel.sizeLine(noPhotometry).contains("PA 30"),
                "and a recorded orientation is stated: "
                        + InspectorPanel.sizeLine(noPhotometry));
        assertEquals("type not classified",
                InspectorPanel.readableType(nothingKnown));
    }

    @Test
    void emptySkyIsAnAnswerWithCoordinates() throws Exception {
        Fixture fixture = fixture();
        fixture.model().selectEmptySky(new SkyPosition(11.0, 40.0));

        List<String> lines = fixture.panel().lines();
        assertEquals("Empty sky", lines.get(0));
        String all = String.join(" | ", lines);
        assertTrue(all.contains("ICRS J2000"), all);
        assertTrue(all.contains("No catalogued object within reach"), all);
    }

    @Test
    void anAmbiguousClickOffersTheChoiceAndChangesNothingElse()
            throws Exception {
        Fixture fixture = fixture();
        Selection.Object star = ChartHitTest.selectionFor(
                firstStar(fixture.scene()));
        Selection.Object dso = ChartHitTest.selectionFor(
                firstDeepSky(fixture.scene()));
        fixture.model().selectAmong(List.of(star, dso));

        assertEquals("2 objects here", fixture.panel().lines().get(0));
        assertEquals(2, fixture.panel().candidateLines().size(),
                "both candidates are offered: "
                        + fixture.panel().candidateLines());

        fixture.model().chooseCandidate(1);
        String all = String.join(" | ", fixture.panel().lines());
        assertTrue(all.contains(dso.catalogueId()),
                "choosing shows the other one: " + all);
        assertTrue(fixture.centred().isEmpty(),
                "and choosing never moves the chart");
    }

    @Test
    void onlyCenterHereEverAsksToMoveTheChart() throws Exception {
        Fixture fixture = fixture();
        Selection.Object star = ChartHitTest.selectionFor(
                firstStar(fixture.scene()));

        fixture.model().select(star);
        fixture.model().selectEmptySky(new SkyPosition(9.0, 42.0));
        fixture.model().select(star);
        assertTrue(fixture.centred().isEmpty(),
                "selecting, re-selecting and empty sky move nothing");

        SwingUtilities.invokeAndWait(() -> centreButton(fixture.panel())
                .doClick());
        assertEquals(1, fixture.centred().size(),
                "and the button asks exactly once");
        assertEquals(star.position(),
                fixture.centred().get(0).position());
    }

    @Test
    void theSelectionOutlivesTheInspector() throws Exception {
        // The panel is a consumer, not the owner: closing it changes
        // nothing about what is selected.
        Fixture fixture = fixture();
        Selection.Object star = ChartHitTest.selectionFor(
                firstStar(fixture.scene()));
        fixture.model().select(star);
        fixture.panel().dispose();

        List<String> whenClosed = fixture.panel().lines();
        fixture.model().selectEmptySky(new SkyPosition(9.0, 42.0));

        assertTrue(fixture.model().selection() instanceof Selection.EmptySky,
                "selection production continues without an inspector");
        assertEquals(whenClosed, fixture.panel().lines(),
                "and the closed panel simply stops hearing, rather than"
                        + " keeping a grip on the state it left");
        assertFalse(String.join(" ", fixture.panel().lines())
                        .contains("Empty sky"),
                "it never saw the change that came after it left");
    }

    @Test
    void thePanelNamesItselfAndItsControlsToAssistiveTechnology()
            throws Exception {
        Fixture fixture = fixture();
        assertEquals("Inspector", fixture.panel().getAccessibleContext()
                .getAccessibleName());
        assertNotNull(fixture.panel().getAccessibleContext()
                .getAccessibleDescription());
        assertEquals("Center here", centreButton(fixture.panel())
                .getAccessibleContext().getAccessibleName());
        assertNotNull(fixture.panel().getInputMap(
                        javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                        .get(javax.swing.KeyStroke.getKeyStroke(
                                java.awt.event.KeyEvent.VK_ESCAPE, 0)),
                "Escape closes the inspector, as it closes every dialog");
    }

    private static javax.swing.JButton centreButton(
            java.awt.Container container) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof javax.swing.JButton button
                    && "Center here".equals(button.getText())) {
                return button;
            }
            if (component instanceof java.awt.Container inner) {
                javax.swing.JButton found = centreButton(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
