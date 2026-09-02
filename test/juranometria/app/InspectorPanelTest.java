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
                model, () -> scene,
                () -> juranometria.render.ChartOptions.DEFAULTS,
                centred::add));
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
    void theInspectorNamesTheTypeEvenWhenTheChartGroupsIt() {
        // Sprint 21, issue #184: the chart draws a lone galaxy, a
        // pair, a triplet and a group with one and the same ellipse.
        // Grouping them for the reader's controls must not cost the
        // Inspector the catalogue's own word for the object in front
        // of them.
        List<String> said = new java.util.ArrayList<>();
        for (juranometria.chart.DsoType type : new juranometria.chart
                .DsoType[] {juranometria.chart.DsoType.GALAXY,
                juranometria.chart.DsoType.GALAXY_PAIR,
                juranometria.chart.DsoType.GALAXY_TRIPLET,
                juranometria.chart.DsoType.GALAXY_GROUP}) {
            DeepSkyObject dso = new DeepSkyObject("NGC 0004", List.of(),
                    type, new SkyPosition(1.0, 1.0), 2.0, 1.0, 0.0,
                    9.0, 3, new DeepSkyObject.Recorded(2.0, 1.0, 0.0,
                            DeepSkyObject.Recorded.Band.VISUAL));
            assertEquals(juranometria.render.ChartRenderer.Symbol.ELLIPSE,
                    juranometria.render.ChartRenderer.symbolFor(dso),
                    "one family on the page");
            said.add(InspectorPanel.readableType(dso));
        }

        assertEquals(List.of("galaxy", "galaxy pair", "galaxy triplet",
                        "galaxy group"), said,
                "and four distinct answers in the panel");
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


    @Test
    void aNarrowWindowClosesThePanelAndAWideOneGivesItBack()
            throws Exception {
        // The reviewed layout rule (review, P1): the chart keeps at
        // least 400 px of page beside a 240 px panel, and below that
        // the panel is what yields.
        Fixture fixture = fixture();
        List<Boolean> announced = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            fixture.panel().onVisibilityChange(announced::add);
            fixture.panel().setRequestedVisible(true);
        });
        assertTrue(fixture.panel().isVisible(), "the reader asked for it");

        SwingUtilities.invokeAndWait(() ->
                fixture.panel().setAvailableWidth(600));
        assertFalse(fixture.panel().isVisible(),
                "600 px cannot hold both, so the panel yields");
        assertTrue(fixture.panel().isRequestedVisible(),
                "but the reader's wish is remembered, not cancelled");

        SwingUtilities.invokeAndWait(() ->
                fixture.panel().setAvailableWidth(1200));
        assertTrue(fixture.panel().isVisible(),
                "and a window that widens gives it back without being"
                        + " asked twice");
        assertEquals(List.of(true, false, true), announced,
                "every appearance and disappearance is announced, so"
                        + " the menu can show what is actually there");

        assertFalse(InspectorPanel.fitsBeside(639));
        assertTrue(InspectorPanel.fitsBeside(640));
    }

    @Test
    void thePanelTakesOnlyWhatIsLeftAboveTheChartsFloor() {
        // Review, P1: 640 px passed the fits test and then the panel
        // took its full preferred 320, leaving the chart 320 - the
        // exact squeeze the decision said the panel would absorb.
        assertEquals(240, InspectorPanel.widthBeside(640),
                "at the boundary the panel takes its floor, so the"
                        + " chart keeps the 400 px it was promised");
        assertEquals(300, InspectorPanel.widthBeside(700));
        assertEquals(320, InspectorPanel.widthBeside(720),
                "and only at 720 does it reach its preference");
        assertEquals(320, InspectorPanel.widthBeside(1600),
                "beyond which the extra room goes to the chart");
        assertEquals(240, InspectorPanel.widthBeside(500),
                "never below its own floor");

        for (int width = 640; width <= 2000; width += 7) {
            assertTrue(width - InspectorPanel.widthBeside(width)
                            >= InspectorPanel.MINIMUM_CHART_WIDTH,
                    "the chart never drops below 400 px at width "
                            + width);
        }
    }

    @Test
    void theChartReallyKeepsItsWidthInARealLayout() throws Exception {
        // The formula is one thing; what BorderLayout actually gives
        // the chart is another, and the review's finding was about
        // the second. Measured on a real container laid out at exact
        // widths - not a JFrame, whose decorations differ by platform
        // and theme and would measure something other than the rule.
        SelectionModel model = new SelectionModel();
        java.awt.Container[] parts = new java.awt.Container[2];
        SwingUtilities.invokeAndWait(() -> {
            juranometria.ui.ChartComponent chart =
                    new juranometria.ui.ChartComponent(
                            juranometria.app.Atlas.assembler());
            InspectorPanel panel = new InspectorPanel(model,
                    chart::currentScene,
                    () -> juranometria.render.ChartOptions.DEFAULTS,
                    selection -> { });
            javax.swing.JPanel window = new javax.swing.JPanel(
                    new java.awt.BorderLayout());
            window.add(chart, java.awt.BorderLayout.CENTER);
            window.add(panel, java.awt.BorderLayout.EAST);
            parts[0] = chart;
            parts[1] = panel;
            for (int width : new int[] {640, 700, 720, 900, 1400}) {
                panel.setAvailableWidth(width);
                panel.setRequestedVisible(true);
                window.setSize(width, 700);
                window.doLayout();
                if (chart.getWidth() < InspectorPanel.MINIMUM_CHART_WIDTH) {
                    throw new AssertionError("at window width " + width
                            + " the chart got only " + chart.getWidth()
                            + " px, with " + panel.getWidth()
                            + " px taken by the inspector");
                }
            }
            panel.setAvailableWidth(640);
            window.setSize(640, 700);
            window.doLayout();
        });

        assertEquals(400, parts[0].getWidth(),
                "at the accepted boundary the chart gets exactly its"
                        + " promised 400 px");
        assertEquals(240, parts[1].getWidth(),
                "and the inspector takes only its floor");
    }

    @Test
    void aToggleTheWindowRefusesIsReportedAsRefused() throws Exception {
        // Review, P2: a checkbox menu item flips itself when clicked.
        // If the panel stays silent because nothing changed, the menu
        // ends up checked while the inspector is not there.
        Fixture fixture = fixture();
        List<Boolean> announced = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            fixture.panel().onVisibilityChange(announced::add);
            fixture.panel().setAvailableWidth(600);
            announced.clear();
            fixture.panel().setRequestedVisible(true);
        });

        assertFalse(fixture.panel().isVisible(),
                "the window is too narrow to honour it");
        assertEquals(List.of(false), announced,
                "and the panel says so, so the menu can uncheck itself"
                        + " rather than claim a panel that is not there");
    }

    @Test
    void enterFromTheListMovesIntoTheFactsNotOntoTheButton()
            throws Exception {
        // Review: the decision moves focus to the panel's body -
        // where the answer is written - not onward to the control
        // that would move the chart.
        Fixture fixture = fixture();
        assertNotNull(fixture.panel().focusTarget());
        assertTrue(fixture.panel().focusTarget().isFocusable(),
                "and the facts can actually take focus");
        assertFalse(fixture.panel().focusTarget()
                        instanceof javax.swing.JButton,
                "Enter must not land the reader on Center here, which"
                        + " is the one control that moves the chart");
        assertEquals("Details of the selected object",
                fixture.panel().focusTarget().getAccessibleContext()
                        .getAccessibleName(),
                "and it names itself, since focus can now rest there");
    }

    @Test
    void afterNavigationItStopsDescribingWhatIsNoLongerDrawn()
            throws Exception {
        // Review, P1: the panel used to go on describing an object
        // the reader had panned away from, because only selection
        // changes reached it.
        ChartScene first = page();
        ChartScene[] current = {first};
        SelectionModel model = new SelectionModel();
        InspectorPanel[] panel = new InspectorPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new InspectorPanel(
                model, () -> current[0],
                () -> juranometria.render.ChartOptions.DEFAULTS,
                selection -> { }));

        ChartRenderer.DrawnMark mark = firstStar(first);
        model.select(ChartHitTest.selectionFor(mark));
        assertTrue(String.join(" ", panel[0].lines())
                        .contains(mark.star().id()),
                "described while it is on the page");

        // The reader pans to another part of the sky.
        current[0] = juranometria.app.Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(200.0, -40.0), 8.0, 8.0,
                        null, null), 900, 700);
        SwingUtilities.invokeAndWait(panel[0]::refresh);

        String all = String.join(" | ", panel[0].lines());
        assertTrue(all.contains("Not on this page any more"),
                "and afterwards it says so plainly: " + all);
        assertFalse(all.contains("visual magnitude"),
                "rather than repeating facts it can no longer read: "
                        + all);
    }

    @Test
    void escapeClosesItAndHandsTheReaderBackToTheChart() throws Exception {
        Fixture fixture = fixture();
        List<String> focused = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            fixture.panel().onClose(() -> focused.add("chart"));
            fixture.panel().setRequestedVisible(true);
            fixture.panel().getActionMap().get("close").actionPerformed(
                    new java.awt.event.ActionEvent(fixture.panel(), 0,
                            "close"));
        });

        assertFalse(fixture.panel().isVisible(), "Escape closes it");
        assertFalse(fixture.panel().isRequestedVisible(),
                "and it stays closed until asked for again");
        assertEquals(List.of("chart"), focused,
                "focus goes back to the chart, not into a panel that"
                        + " is no longer there");
    }

    @Test
    void theCandidateListIsWalkableAndSettles() throws Exception {
        Fixture fixture = fixture();
        Selection.Object star = ChartHitTest.selectionFor(
                firstStar(fixture.scene()));
        Selection.Object dso = ChartHitTest.selectionFor(
                firstDeepSky(fixture.scene()));
        fixture.model().selectAmong(List.of(star, dso));

        javax.swing.JList<?> list = candidateList(fixture.panel());
        assertNotNull(list, "the choice is a list a reader can walk");
        assertTrue(list.isFocusable(), "and it takes focus");
        assertNotNull(list.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                        .get(javax.swing.KeyStroke.getKeyStroke(
                                java.awt.event.KeyEvent.VK_ENTER, 0)),
                "Enter settles on the walked-to candidate");

        // Walking the list is what changes the answer.
        SwingUtilities.invokeAndWait(() -> list.setSelectedIndex(1));
        assertEquals(1, fixture.model().currentIndex(),
                "arrowing to a candidate selects it");
        assertTrue(String.join(" ", fixture.panel().lines())
                        .contains(dso.catalogueId()),
                "and the panel follows");
        assertTrue(fixture.centred().isEmpty(),
                "while the chart stays exactly where it was");
    }

    private static javax.swing.JList<?> candidateList(
            java.awt.Container container) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof javax.swing.JList<?> list) {
                return list;
            }
            if (component instanceof java.awt.Container inner) {
                javax.swing.JList<?> found = candidateList(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
