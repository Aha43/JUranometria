package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.app.InspectorPanel;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 19 acceptance journey through the production paths
 * (issue #171): from the released M31 page, a reader points at an
 * unlabelled star and learns what it is; points at a deep-sky symbol
 * and reads what the catalogue records and what it does not; meets
 * an overlap and is offered the choice rather than given a guess;
 * clicks empty sky and is told where they clicked; travels to wide,
 * wrapped, polar and southern skies and points there too; presses
 * Center here once, deliberately; searches by name and finds that
 * selected as well; works the panel by keyboard; closes and reopens
 * it; and comes Home to the exact released default.
 *
 * <p>Throughout, the promise the whole feature rests on is checked
 * rather than assumed: <strong>selecting moves nothing</strong> - not
 * the centre, not the field, not the target, not even the assembled
 * scene. A second, independent observer runs beside the inspector to
 * prove the shared state carries more than one reader.
 *
 * <p>Requires a display.
 */
class MapExplorationJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private ChartComponent chart;
    private ChartViewController navigation;
    private SelectionModel selection;
    private InspectorPanel inspector;
    private SearchField searchField;
    /** The second consumer: proof the seam is not the inspector's alone. */
    private final List<SelectionModel.Change> witness = new ArrayList<>();

    @Test
    void askTheMapWhatItIsShowingAndComeHomeUnmoved() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the exploration journey drives a real window");

        JFrame[] frame = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            navigation = new ChartViewController(Atlas.assembler()::fits);
            chart = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart::setViewState);
            PanInteraction.install(chart, navigation);
            ZoomInteraction.install(chart, navigation);
            selection = new SelectionModel();
            SelectInteraction.install(chart, selection);
            inspector = new InspectorPanel(selection, chart::currentScene,
                    chosen -> navigation.recenter(chosen.position()));
            chart.onSceneChange(inspector::refresh);
            selection.onChange(change -> chart.setHighlightedObject(
                    change.selection() instanceof Selection.Object object
                            ? object.catalogueId() : null));
            selection.onChange(witness::add);
            searchField = new SearchField(Atlas.search(), Atlas.assembler(),
                    navigation);
            searchField.setSelectionModel(selection);

            frame[0] = new JFrame("exploration journey");
            frame[0].setLayout(new BorderLayout());
            frame[0].add(chart, BorderLayout.CENTER);
            frame[0].add(inspector, BorderLayout.EAST);
            frame[0].setSize(1240, 800);
            frame[0].setVisible(true);
            inspector.setAvailableWidth(1240);
            inspector.setRequestedVisible(true);
        });
        flush();

        try {
            // The released default page, as every reader meets it.
            assertEquals(8.0, navigation.state().fieldWidthDegrees());
            assertTrue(navigation.state().targetLabel().contains("M31"));
            assertTrue(inspector.isVisible(), "the reader opened it");
            int witnessedAtStart = witness.size();

            // 1. An unlabelled star. The chart draws hundreds; almost
            // none carry a name, and until now the only way to ask
            // was to guess one and search for it.
            ChartRenderer.DrawnMark star = someStar();
            ChartViewState beforeAsking = navigation.state();
            ChartScene sceneBeforeAsking = chart.currentScene();
            clickOn(star);

            Selection.Object identified = assertInstanceOf(
                    Selection.Object.class, selection.selection());
            assertEquals(star.star().id(), identified.catalogueId(),
                    "the star under the pointer is the one identified");
            String said = String.join(" | ", inspector.lines());
            assertTrue(said.contains(star.star().id()),
                    "and the panel says which star it is: " + said);
            assertTrue(said.contains("magnitude") && said.contains("ICRS"),
                    "with its brightness and its place: " + said);

            // The promise: a question is not a command.
            assertEquals(beforeAsking, navigation.state(),
                    "asking moved nothing");
            assertSame(sceneBeforeAsking, chart.currentScene(),
                    "and assembled nothing: the page is the same page");
            assertTrue(witness.size() > witnessedAtStart,
                    "the second observer heard it too");
            assertEquals(selection.selection(),
                    witness.get(witness.size() - 1).selection(),
                    "and heard exactly what the inspector heard");

            // 2. A deep-sky symbol, with the catalogue's silences
            // stated as silences.
            ChartRenderer.DrawnMark symbol = someDeepSky();
            clickOn(symbol);
            String deepSky = String.join(" | ", inspector.lines());
            switch (symbol.deepSky().recorded().band()) {
                case VISUAL -> assertTrue(deepSky.contains("visual magnitude"),
                        deepSky);
                case BLUE -> assertTrue(deepSky.contains("blue magnitude"),
                        "a blue magnitude is never labelled visual: "
                                + deepSky);
                case NONE -> assertTrue(
                        deepSky.contains("magnitude not recorded"), deepSky);
            }
            assertFalse(deepSky.contains("PA 0°")
                            && !symbol.deepSky().recorded()
                                    .hasPositionAngle(),
                    "an unrecorded orientation is never printed as zero: "
                            + deepSky);

            // 3. An overlap: offered, never resolved for the reader.
            ChartViewState beforeCrowd = navigation.state();
            navigation.recenter(new SkyPosition(83.8, 0.0), 36.0);
            flush();
            ChartRenderer.DrawnMark crowded = crowdedMark();
            clickOn(crowded);
            assertTrue(selection.candidates().size() > 1,
                    "the reader is offered every candidate: "
                            + selection.candidates().size());
            assertEquals(0, selection.currentIndex());
            assertTrue(inspector.candidateLines().size() > 1,
                    "and the panel lists them: "
                            + inspector.candidateLines());

            ChartScene beforeChoosing = chart.currentScene();
            selection.chooseCandidate(1);
            flush();
            assertEquals(1, selection.currentIndex(),
                    "choosing another changes the answer");
            assertSame(beforeChoosing, chart.currentScene(),
                    "and reassembles nothing at all");

            // 4. Empty sky is an answer.
            clickOnEmptySky();
            assertInstanceOf(Selection.EmptySky.class, selection.selection());
            assertTrue(String.join(" ", inspector.lines())
                            .contains("No catalogued object"),
                    "the reader is told what is there: nothing");

            // 5. The far corners of the sky answer like anywhere else.
            for (double[] place : new double[][] {
                    {0.5, 5.0, 18.0},      // across the RA wrap
                    {37.9, 89.26, 18.0},   // the northern pole
                    {186.6, -60.0, 18.0}}) { // far south
                navigation.recenter(new SkyPosition(place[0], place[1]),
                        place[2]);
                flush();
                ChartRenderer.DrawnMark there = someStar();
                clickOn(there);
                // The star is always OFFERED. It is not always first:
                // in Crux this very star lies inside IC 2944's
                // outline, and the reviewed rule puts ink before
                // nearness, so the nebula the reader is standing on
                // leads. That is the contract working, not failing -
                // and the reader can still take the star.
                List<String> offered = selection.candidates().stream()
                        .map(Selection.Object::catalogueId).toList();
                assertTrue(offered.contains(there.star().id()),
                        "the star is among the candidates at "
                                + place[0] + ", " + place[1] + ": "
                                + offered);
                int which = offered.indexOf(there.star().id());
                if (which > 0) {
                    selection.chooseCandidate(which);
                    flush();
                }
                assertEquals(there.star().id(),
                        ((Selection.Object) selection.selection())
                                .catalogueId(),
                        "and the reader can reach it at " + place[0]
                                + ", " + place[1]);
                assertTrue(String.join(" ", inspector.lines())
                                .contains(there.star().id()),
                        "with the panel describing it");
            }

            // 6. Center here: the one action that moves the chart, and
            // only when pressed.
            navigation.recenter(new SkyPosition(10.68, 41.27), 8.0);
            flush();
            ChartRenderer.DrawnMark offCentre = offCentreStar();
            clickOn(offCentre);
            SkyPosition wasCentred = navigation.state().centre();
            SwingUtilities.invokeAndWait(() ->
                    centreButton(inspector).doClick());
            flush();
            assertFalse(wasCentred.equals(navigation.state().centre()),
                    "Center here moved the chart");
            assertTrue(navigation.state().centre().separationDegrees(
                            offCentre.star().position()) < 1e-6,
                    "onto the selected star");

            // 7. Search finds and selects: the keyboard-only route.
            SwingUtilities.invokeAndWait(() -> searchField.apply(
                    Atlas.search().search("betelgeuse").get(0)));
            flush();
            Selection.Object searched = assertInstanceOf(
                    Selection.Object.class, selection.selection());
            assertTrue(String.join(" ", inspector.lines())
                            .contains(searched.catalogueId()),
                    "what the reader looked up is what the panel"
                            + " describes");
            assertEquals(searched.catalogueId(),
                    navigation.state().targetIdentity(),
                    "and search still titles the chart as it always did");

            // 8. Panning away: the panel stops describing what is gone
            // rather than repeating stale facts.
            navigation.recenter(new SkyPosition(200.0, -40.0), 8.0);
            flush();
            assertTrue(String.join(" ", inspector.lines())
                            .contains("Not on this page any more"),
                    "the panel is honest about having lost sight of it: "
                            + inspector.lines());

            // 9. Closed and reopened, the selection survives.
            Selection kept = selection.selection();
            SwingUtilities.invokeAndWait(() ->
                    inspector.setRequestedVisible(false));
            flush();
            assertFalse(inspector.isVisible(), "the reader closed it");
            assertEquals(kept, selection.selection(),
                    "closing the panel forgets nothing");
            SwingUtilities.invokeAndWait(() ->
                    inspector.setRequestedVisible(true));
            flush();
            assertTrue(inspector.isVisible());

            // 10. Home: the released default, exactly.
            SwingUtilities.invokeAndWait(navigation::reset);
            flush();
            assertEquals(ChartViewState.DEFAULT, navigation.state(),
                    "the journey ends where every reader begins");
            assertTrue(navigation.state().targetLabel().contains("M31"));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                inspector.dispose();
                frame[0].dispose();
            });
        }
    }

    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private List<ChartRenderer.DrawnMark> marks() {
        return RENDERER.drawnMarks(chart.currentScene(),
                ChartOptions.DEFAULTS);
    }

    /** A star comfortably inside the page. */
    private ChartRenderer.DrawnMark someStar() {
        return marks().stream()
                .filter(mark -> mark.star() != null)
                .filter(this::wellInside)
                .findFirst().orElseThrow();
    }

    private ChartRenderer.DrawnMark offCentreStar() {
        return marks().stream()
                .filter(mark -> mark.star() != null)
                .filter(this::wellInside)
                .filter(mark -> Math.abs(mark.centre().x() - 450) > 150)
                .findFirst().orElseThrow();
    }

    private ChartRenderer.DrawnMark someDeepSky() {
        return marks().stream()
                .filter(mark -> mark.deepSky() != null)
                .filter(this::wellInside)
                .findFirst().orElseThrow();
    }

    private ChartRenderer.DrawnMark crowdedMark() {
        List<ChartRenderer.DrawnMark> all = marks();
        return all.stream()
                .filter(this::wellInside)
                .filter(mark -> all.stream()
                        .filter(other -> other.hitBy(mark.centre().x(),
                                mark.centre().y(),
                                ChartHitTest.TOLERANCE_PX))
                        .count() > 1)
                .findFirst().orElseThrow();
    }

    private boolean wellInside(ChartRenderer.DrawnMark mark) {
        ChartScene scene = chart.currentScene();
        return mark.centre().x() > 60
                && mark.centre().x() < scene.viewport().widthPx() - 60
                && mark.centre().y() > 60
                && mark.centre().y() < scene.viewport().heightPx() - 60;
    }

    private void clickOn(ChartRenderer.DrawnMark mark) throws Exception {
        click((int) Math.round(mark.centre().x()),
                (int) Math.round(mark.centre().y()) + chart.pageOffsetY());
    }

    private void clickOnEmptySky() throws Exception {
        List<ChartRenderer.DrawnMark> drawn = marks();
        ChartScene scene = chart.currentScene();
        for (int x = 80; x < scene.viewport().widthPx() - 80; x += 19) {
            for (int y = 80; y < scene.viewport().heightPx() - 80; y += 23) {
                final double px = x;
                final double py = y;
                if (drawn.stream().noneMatch(
                        mark -> mark.hitBy(px, py, 16.0))) {
                    click(x, y + chart.pageOffsetY());
                    return;
                }
            }
        }
        throw new AssertionError("this page has no empty sky to click");
    }

    private void click(int x, int y) throws Exception {
        for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                MouseEvent.MOUSE_RELEASED}) {
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                    new MouseEvent(chart, id,
                            System.nanoTime() / 1_000_000,
                            MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                            MouseEvent.BUTTON1)));
            flush();
        }
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
