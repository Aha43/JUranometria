package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pointing at the real chart component (issue #170): a click asks a
 * question, a drag pans, and the two never take each other's turn.
 */
class SelectInteractionTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private record Fixture(ChartComponent chart, ChartViewController navigation,
                           SelectionModel selection,
                           juranometria.chart.WorkingSelection working,
                           juranometria.chart.SelectionMode mode) {
    }

    private static Fixture fixture(double ra, double dec, double field)
            throws Exception {
        ChartComponent[] chart = new ChartComponent[1];
        ChartViewController navigation =
                new ChartViewController(Atlas.assembler()::fits);
        SelectionModel selection = new SelectionModel();
        juranometria.chart.WorkingSelection working =
                new juranometria.chart.WorkingSelection();
        juranometria.chart.SelectionMode mode =
                new juranometria.chart.SelectionMode();
        SwingUtilities.invokeAndWait(() -> {
            chart[0] = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart[0]::setViewState);
            PanInteraction.install(chart[0], navigation);
            SelectInteraction.install(chart[0], selection, working, mode);
            chart[0].setSize(900, 760);
            navigation.recenter(new SkyPosition(ra, dec), field);
        });
        flush();
        return new Fixture(chart[0], navigation, selection, working, mode);
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static void mouse(ChartComponent chart, int id, int x, int y)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                new MouseEvent(chart, id, System.nanoTime() / 1_000_000,
                        MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                        MouseEvent.BUTTON1)));
        flush();
    }

    private static void click(ChartComponent chart, int x, int y)
            throws Exception {
        mouse(chart, MouseEvent.MOUSE_PRESSED, x, y);
        mouse(chart, MouseEvent.MOUSE_RELEASED, x, y);
    }

    /** A mark on the page, in component coordinates. */
    private static int[] pointAt(Fixture fixture,
                                 ChartRenderer.DrawnMark mark) {
        return new int[] {(int) Math.round(mark.centre().x()),
                (int) Math.round(mark.centre().y())
                        + fixture.chart().pageOffsetY()};
    }

    private static List<ChartRenderer.DrawnMark> marks(ChartScene scene) {
        return RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS);
    }

    @Test
    void aClickOnAStarSelectsThatStar() throws Exception {
        Fixture fixture = fixture(10.68, 41.27, 8.0);
        ChartScene scene = fixture.chart().currentScene();
        ChartRenderer.DrawnMark star = marks(scene).stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 100
                        && mark.centre().x() < 800
                        && mark.centre().y() > 100
                        && mark.centre().y() < 600)
                .findFirst().orElseThrow();

        int[] point = pointAt(fixture, star);
        click(fixture.chart(), point[0], point[1]);

        Selection.Object selected = assertInstanceOf(Selection.Object.class,
                fixture.selection().selection());
        assertEquals(star.star().id(), selected.catalogueId(),
                "the star under the pointer, by its catalogue identity");
    }

    @Test
    void selectingMovesNothing() throws Exception {
        // The contract the whole feature rests on: a question is not
        // a command.
        Fixture fixture = fixture(10.68, 41.27, 8.0);
        ChartViewState before = fixture.navigation().state();
        ChartScene sceneBefore = fixture.chart().currentScene();
        ChartRenderer.DrawnMark star = marks(sceneBefore).stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 200
                        && mark.centre().x() < 700)
                .findFirst().orElseThrow();

        int[] point = pointAt(fixture, star);
        click(fixture.chart(), point[0], point[1]);

        assertEquals(before, fixture.navigation().state(),
                "no centre, field, magnitude or target changed");
        assertSame(sceneBefore, fixture.chart().currentScene(),
                "and no scene was assembled: the page is the same page");
    }

    @Test
    void aDragPansAndAsksNothing() throws Exception {
        Fixture fixture = fixture(10.68, 41.27, 8.0);
        ChartViewState before = fixture.navigation().state();

        mouse(fixture.chart(), MouseEvent.MOUSE_PRESSED, 450, 380);
        mouse(fixture.chart(), MouseEvent.MOUSE_DRAGGED, 650, 400);
        mouse(fixture.chart(), MouseEvent.MOUSE_RELEASED, 650, 400);

        assertFalse(before.equals(fixture.navigation().state()),
                "the drag panned the chart");
        assertInstanceOf(Selection.None.class,
                fixture.selection().selection(),
                "and asked no question: a hand that travels is panning");
    }

    @Test
    void aClickOnLetterboxChromeAsksNothing() throws Exception {
        // The page's height is capped by the coverage rule, so a
        // component taller than the cap is letterboxed. The cap is
        // about 4,800 px at 36 degrees, which no ordinary window
        // reaches - so the geometry is built here rather than hoped
        // for. This test used to assume its way past the case when
        // the offset came out zero, which meant it could pass
        // without ever clicking chrome (sprint review).
        ChartComponent[] chart = new ChartComponent[1];
        ChartViewController navigation =
                new ChartViewController(Atlas.assembler()::fits);
        SelectionModel selection = new SelectionModel();
        SwingUtilities.invokeAndWait(() -> {
            chart[0] = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart[0]::setViewState);
            SelectInteraction.install(chart[0], selection,
                    new juranometria.chart.WorkingSelection(),
                    new juranometria.chart.SelectionMode());
            chart[0].setSize(900, 6000);
            navigation.recenter(new SkyPosition(83.8, 0.0), 36.0);
        });
        flush();

        int offset = chart[0].pageOffsetY();
        assertTrue(offset > 100,
                "the page really is letterboxed here, so the case"
                        + " cannot silently skip: offset " + offset);

        click(chart[0], 450, offset / 2);
        assertInstanceOf(Selection.None.class, selection.selection(),
                "chrome above the paper is not sky");

        click(chart[0], 450, 6000 - offset / 2);
        assertInstanceOf(Selection.None.class, selection.selection(),
                "nor is chrome below it");

        // And the paper between them still answers, so the geometry
        // is sane rather than merely unreachable.
        ChartRenderer.DrawnMark onPaper = marks(chart[0].currentScene())
                .stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 100
                        && mark.centre().x() < 800)
                .findFirst().orElseThrow();
        click(chart[0], (int) onPaper.centre().x(),
                (int) onPaper.centre().y() + offset);
        assertInstanceOf(Selection.Object.class, selection.selection(),
                "while the paper between the bands does answer");
    }

    @Test
    void aClickOnQuietPaperAnswersWithEmptySky() throws Exception {
        Fixture fixture = fixture(40.0, -35.0, 8.0);
        ChartScene scene = fixture.chart().currentScene();
        // A page point that reaches nothing.
        int[] empty = null;
        List<ChartRenderer.DrawnMark> drawn = marks(scene);
        outer:
        for (int x = 120; x < 780; x += 23) {
            for (int y = 120; y < 560; y += 29) {
                final double px = x;
                final double py = y;
                if (drawn.stream().noneMatch(
                        mark -> mark.hitBy(px, py, 12.0))) {
                    empty = new int[] {x, y + fixture.chart().pageOffsetY()};
                    break outer;
                }
            }
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(empty != null,
                "the quiet page must have quiet places");

        click(fixture.chart(), empty[0], empty[1]);

        assertInstanceOf(Selection.EmptySky.class,
                fixture.selection().selection(),
                "empty paper answers with its coordinates");
    }

    @Test
    void aWideFieldClickOffersItsCandidates() throws Exception {
        Fixture fixture = fixture(83.8, 0.0, 36.0);
        ChartScene scene = fixture.chart().currentScene();
        // A point where the reviewed rule finds more than one mark.
        ChartRenderer.DrawnMark crowded = marks(scene).stream()
                .filter(mark -> mark.centre().x() > 100
                        && mark.centre().x() < 800
                        && mark.centre().y() > 100
                        && mark.centre().y() < 600)
                .filter(mark -> marks(scene).stream()
                        .filter(other -> other.hitBy(mark.centre().x(),
                                mark.centre().y(), 4.0))
                        .count() > 1)
                .findFirst().orElseThrow();

        int[] point = pointAt(fixture, crowded);
        click(fixture.chart(), point[0], point[1]);

        assertTrue(fixture.selection().candidates().size() > 1,
                "the reader is offered the choice: "
                        + fixture.selection().candidates().size());
        assertEquals(0, fixture.selection().currentIndex(),
                "with the first current, and nothing resolved for them");
    }
}
