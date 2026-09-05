package juranometria.ui;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.Selection;
import juranometria.chart.SelectionMode;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.chart.WorkingSelection;
import juranometria.page.LeadSelection;
import juranometria.page.PageInventory;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart click as a working-selection gesture (issue #261,
 * semantics from docs/decisions/working-selection.md): ordinary
 * clicks replace, additive clicks toggle, empty sky answers without
 * editing when additive, and the ambiguous additive click is a
 * captured transaction - toggle exactly one candidate against the
 * pre-click set, replayed whole as the chooser cycles. The gate's
 * ambiguous-cycle mutation check lives here: a cycle that
 * accumulates members, or sheds extra ones, fails these tests.
 *
 * <p>Every gesture is a real mouse event on the real component, with
 * the production lead bridge connected, so the answering model and
 * the membership are exercised together as the application wires
 * them.
 */
class WorkingSelectionChartGestureTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private record Fixture(ChartComponent chart, SelectionModel selection,
                           WorkingSelection working, SelectionMode mode) {
    }

    private static Fixture fixture(double ra, double dec, double field)
            throws Exception {
        ChartComponent[] chart = new ChartComponent[1];
        SelectionModel selection = new SelectionModel();
        WorkingSelection working = new WorkingSelection();
        SelectionMode mode = new SelectionMode();
        SwingUtilities.invokeAndWait(() -> {
            chart[0] = new ChartComponent(Atlas.assembler());
            chart[0].setSize(900, 760);
            chart[0].setViewState(new juranometria.chart.ChartViewState(
                    new SkyPosition(ra, dec), field, 8.0));
            SelectInteraction.install(chart[0], selection, working, mode);
            // The production wiring, both directions: the lead feeds
            // the answering model, and the membership feeds the ink.
            LeadSelection.connect(working, selection,
                    () -> PageInventory.of(chart[0].currentScene(),
                            chart[0].chartOptions()));
            working.onChange(change -> chart[0].setWorkingSelection(
                    change.members(), change.lead()));
        });
        flush();
        return new Fixture(chart[0], selection, working, mode);
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static int toggleMask() {
        return java.awt.Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMaskEx();
    }

    private static void click(ChartComponent chart, int x, int y,
                              int keyMask) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            chart.dispatchEvent(new MouseEvent(chart,
                    MouseEvent.MOUSE_PRESSED, System.nanoTime() / 1_000_000,
                    InputEvent.BUTTON1_DOWN_MASK | keyMask, x, y, 1, false,
                    MouseEvent.BUTTON1));
            chart.dispatchEvent(new MouseEvent(chart,
                    MouseEvent.MOUSE_RELEASED, System.nanoTime() / 1_000_000,
                    keyMask, x, y, 1, false, MouseEvent.BUTTON1));
        });
        flush();
    }

    private static List<ChartRenderer.DrawnMark> marks(ChartScene scene) {
        return RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS);
    }

    /** A mark hit by nothing else, in component coordinates. */
    private static int[] lonely(Fixture fixture, int skip) {
        ChartScene scene = fixture.chart().currentScene();
        int seen = 0;
        for (ChartRenderer.DrawnMark mark : marks(scene)) {
            double x = mark.centre().x();
            double y = mark.centre().y();
            if (x < 100 || x > 800 || y < 100 || y > 600) {
                continue;
            }
            long within = marks(scene).stream()
                    .filter(other -> other.hitBy(x, y, 4.0)).count();
            if (within == 1 && seen++ == skip) {
                return new int[] {(int) Math.round(x),
                        (int) Math.round(y) + fixture.chart().pageOffsetY(),
                        0};
            }
        }
        throw new IllegalStateException("no lonely mark to click");
    }

    private static String identityAt(Fixture fixture, int[] point) {
        ChartScene scene = fixture.chart().currentScene();
        double y = point[1] - fixture.chart().pageOffsetY();
        return marks(scene).stream()
                .filter(mark -> mark.hitBy(point[0], y, 4.0))
                .findFirst().map(mark -> mark.star() != null
                        ? mark.star().id() : mark.deepSky().id())
                .orElseThrow();
    }

    @Test
    void ordinaryClicksReplaceAndAdditiveClicksToggle() throws Exception {
        Fixture fixture = fixture(10.68, 41.27, 8.0);
        int[] first = lonely(fixture, 0);
        int[] second = lonely(fixture, 1);
        String firstId = identityAt(fixture, first);
        String secondId = identityAt(fixture, second);

        click(fixture.chart(), first[0], first[1], 0);
        assertEquals(List.of(firstId), fixture.working().members(),
                "an ordinary click replaces the set with that object");

        click(fixture.chart(), second[0], second[1], toggleMask());
        assertEquals(List.of(firstId, secondId),
                fixture.working().members(),
                "the platform's additive modifier toggles the second in");
        assertEquals(secondId, fixture.working().lead(), "and it leads");
        assertEquals(secondId, assertInstanceOf(Selection.Object.class,
                fixture.selection().selection()).catalogueId(),
                "the answering model reads the same lead");

        click(fixture.chart(), second[0], second[1], toggleMask());
        assertEquals(List.of(firstId), fixture.working().members(),
                "toggling again removes it and only it");
        assertEquals(firstId, fixture.working().lead(),
                "the lead passes to the last-marked remaining member");
        assertEquals(firstId, assertInstanceOf(Selection.Object.class,
                fixture.selection().selection()).catalogueId(),
                "and the answer moves with it");

        fixture.mode().accumulate(true);
        click(fixture.chart(), second[0], second[1], 0);
        assertEquals(List.of(firstId, secondId),
                fixture.working().members(),
                "Accumulate makes a plain click additive - the visible"
                        + " control and the modifier are one switch");
    }

    @Test
    void emptySkyAnswersThePlaceAndEditsOnlyWhenOrdinary()
            throws Exception {
        Fixture fixture = fixture(10.68, 41.27, 8.0);
        int[] mark = lonely(fixture, 0);
        String markId = identityAt(fixture, mark);
        click(fixture.chart(), mark[0], mark[1], 0);
        assertEquals(List.of(markId), fixture.working().members());

        // Empty paper: a point no mark reaches.
        ChartScene scene = fixture.chart().currentScene();
        int emptyX = -1;
        int emptyY = -1;
        outer:
        for (int x = 120; x < 780; x += 7) {
            for (int y = 120; y < 580; y += 7) {
                final double px = x;
                final double py = y;
                if (marks(scene).stream()
                        .noneMatch(m -> m.hitBy(px, py, 4.0))) {
                    emptyX = x;
                    emptyY = y + fixture.chart().pageOffsetY();
                    break outer;
                }
            }
        }
        assertTrue(emptyX > 0, "the page has empty paper to click");

        click(fixture.chart(), emptyX, emptyY, toggleMask());
        assertEquals(List.of(markId), fixture.working().members(),
                "an additive empty-sky click is a question, not an"
                        + " edit: membership untouched");
        assertInstanceOf(Selection.EmptySky.class,
                fixture.selection().selection(),
                "while the Inspector still answers the place");

        click(fixture.chart(), emptyX, emptyY, 0);
        assertTrue(fixture.working().members().isEmpty(),
                "the ordinary empty-sky click replaces with the empty"
                        + " set");
        assertInstanceOf(Selection.EmptySky.class,
                fixture.selection().selection());
    }

    /** A point the reviewed rule finds several marks at. */
    private static int[] crowded(Fixture fixture) {
        ChartScene scene = fixture.chart().currentScene();
        ChartRenderer.DrawnMark mark = marks(scene).stream()
                .filter(m -> m.centre().x() > 100 && m.centre().x() < 800
                        && m.centre().y() > 100 && m.centre().y() < 600)
                .filter(m -> marks(scene).stream()
                        .filter(other -> other.hitBy(m.centre().x(),
                                m.centre().y(), 4.0)).count() > 1)
                .findFirst().orElseThrow();
        return new int[] {(int) Math.round(mark.centre().x()),
                (int) Math.round(mark.centre().y())
                        + fixture.chart().pageOffsetY()};
    }

    @Test
    void theAmbiguousAdditiveCycleCanNeitherAccumulateNorShed()
            throws Exception {
        // The gate's mutation check, verbatim: cycling the chooser
        // over members and non-members alike is one toggle against
        // the pre-click snapshot, never the previous step with
        // another toggle.
        Fixture fixture = fixture(83.8, 0.0, 36.0);
        int[] point = crowded(fixture);

        // A set built before the click, one lonely member.
        int[] elsewhere = lonely(fixture, 0);
        String elsewhereId = identityAt(fixture, elsewhere);
        click(fixture.chart(), elsewhere[0], elsewhere[1], 0);
        assertEquals(List.of(elsewhereId), fixture.working().members());

        click(fixture.chart(), point[0], point[1], toggleMask());
        List<Selection.Object> candidates = fixture.selection().candidates();
        assertTrue(candidates.size() > 1, "the click really is ambiguous");
        String c0 = candidates.get(0).catalogueId();
        String c1 = candidates.get(1).catalogueId();
        assertEquals(List.of(elsewhereId, c0), fixture.working().members(),
                "the current candidate is toggled in against the"
                        + " snapshot");

        SwingUtilities.invokeAndWait(() ->
                fixture.selection().chooseCandidate(1));
        flush();
        assertEquals(List.of(elsewhereId, c1), fixture.working().members(),
                "cycling retracts the transaction's own effect and"
                        + " replays the single toggle - never both"
                        + " applied");
        assertEquals(candidates, fixture.selection().candidates(),
                "and the chooser stays open for the next cycle");

        for (int cycle = 0; cycle < 3; cycle++) {
            final int index = cycle % 2;
            SwingUtilities.invokeAndWait(() ->
                    fixture.selection().chooseCandidate(index));
            flush();
        }
        assertEquals(2, fixture.working().members().size(),
                "cycling can neither accumulate members nor shed extra"
                        + " ones: " + fixture.working().members());
        assertEquals(elsewhereId, fixture.working().members().get(0),
                "and the pre-click member is untouched throughout");
    }

    @Test
    void aCandidatePresentInTheSnapshotIsShownRemoved() throws Exception {
        Fixture fixture = fixture(83.8, 0.0, 36.0);
        int[] point = crowded(fixture);

        // Learn the candidates, then rebuild the pre-click set so
        // the FIRST candidate is already a member and leads.
        click(fixture.chart(), point[0], point[1], 0);
        List<Selection.Object> candidates = fixture.selection().candidates();
        String c0 = candidates.get(0).catalogueId();
        String c1 = candidates.get(1).catalogueId();
        int[] elsewhere = lonely(fixture, 0);
        String elsewhereId = identityAt(fixture, elsewhere);
        fixture.working().replaceWith(List.of(elsewhereId, c0), c0);
        flush();

        click(fixture.chart(), point[0], point[1], toggleMask());
        assertEquals(List.of(elsewhereId), fixture.working().members(),
                "the current candidate was present in the snapshot, so"
                        + " it is shown removed");
        assertEquals(elsewhereId, fixture.working().lead(),
                "with the lead by the removal rule on"
                        + " (snapshot - candidate)");

        SwingUtilities.invokeAndWait(() ->
                fixture.selection().chooseCandidate(1));
        flush();
        assertEquals(List.of(elsewhereId, c0, c1),
                fixture.working().members(),
                "flipping to an absent candidate flips between exactly"
                        + " those two outcomes: the removal is retracted"
                        + " and the addition replayed");
        assertEquals(c1, fixture.working().lead());
    }

    @Test
    void anotherGesturesTransitionEndsTheTransaction() throws Exception {
        Fixture fixture = fixture(83.8, 0.0, 36.0);
        int[] point = crowded(fixture);
        click(fixture.chart(), point[0], point[1], toggleMask());
        List<Selection.Object> candidates = fixture.selection().candidates();
        assertTrue(candidates.size() > 1);
        String c0 = candidates.get(0).catalogueId();

        // Another surface edits membership - a table gesture, say.
        fixture.working().add("NGC 1976");
        flush();
        List<String> settled = fixture.working().members();
        assertEquals(List.of(c0, "NGC 1976"), settled);
        assertEquals(1, fixture.selection().candidates().size(),
                "the transition drives the answering model and the"
                        + " candidate list collapses to the new lead");

        // A stale chooser cannot reopen the finished click: even a
        // direct candidate choice on the collapsed list replays no
        // snapshot.
        SwingUtilities.invokeAndWait(() ->
                fixture.selection().chooseCandidate(0));
        flush();
        assertEquals(settled, fixture.working().members(),
                "the transaction is over; nothing edits membership");
    }

    @Test
    void ordinaryAmbiguousChoosingRetargetsInOneTransition()
            throws Exception {
        Fixture fixture = fixture(83.8, 0.0, 36.0);
        int[] point = crowded(fixture);
        click(fixture.chart(), point[0], point[1], 0);
        List<Selection.Object> candidates = fixture.selection().candidates();
        assertTrue(candidates.size() > 1);
        String c1 = candidates.get(1).catalogueId();

        List<WorkingSelection.Change> heard = new ArrayList<>();
        Runnable release = fixture.working().onChange(heard::add);
        heard.clear();
        SwingUtilities.invokeAndWait(() ->
                fixture.selection().chooseCandidate(1));
        flush();
        release.run();

        assertEquals(1, heard.size(),
                "one choice, one whole transition");
        assertEquals(List.of(c1), heard.get(0).members(),
                "member and lead retargeted together under replace"
                        + " semantics");
        assertEquals(c1, heard.get(0).lead());
    }
}
