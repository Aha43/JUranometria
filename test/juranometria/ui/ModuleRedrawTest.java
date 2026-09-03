package juranometria.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.module.ChartServices;
import juranometria.page.PageContents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That {@code redraw()} is paint-only, asked of production
 * (Sprint 25, issue #227).
 *
 * <p>The seam's contract is that a module asking for its lines to be
 * drawn again causes no catalogue query, no scene reassembly and no
 * inventory rebuild. The module's own tests hold the module to that
 * against a fake service - which proves the module calls nothing
 * else, and proves nothing about what the real host <em>does</em>
 * when called (review). A production {@code redraw()} that quietly
 * rebuilt would have passed every one of those tests.
 *
 * <p>And paint-only cuts both ways: it must do nothing more than
 * paint, and it must not do less. A first version of this class
 * proved only the first half, so a production {@code redraw()}
 * replaced with a no-op would still have passed - the module's
 * lines simply never updating on screen (review).
 *
 * <p>So this asks the real {@link ChartModuleHost} on a real
 * {@link ChartComponent}, driven by a real module change, and
 * watches both sides at once. Identity is the evidence for "nothing
 * more": the host caches its inventory and the component its scene,
 * so a rebuild of either produces a <em>new object</em>, and the
 * same object afterwards means the work was not done. A recording
 * {@link RepaintManager} is the evidence for "nothing less": every
 * repaint request Swing is handed passes through it, so the test
 * observes the request itself without ever painting. The control
 * test proves the instrument can tell - the same identities survive
 * a real page change nowhere - and the two failure directions are
 * caught by different assertions, so each dies alone: a
 * {@code redraw()} that rebuilds fails the identities, and one that
 * does nothing fails the repaint.
 */
class ModuleRedrawTest {

    /**
     * Sees every repaint request in the JVM while installed. Global,
     * like the look-and-feel, so it is restored in {@code finally}
     * whatever happens.
     */
    private static final class RecordingRepaints extends RepaintManager {

        final List<JComponent> asked = new ArrayList<>();

        @Override
        public synchronized void addDirtyRegion(JComponent component,
                                                int x, int y,
                                                int width, int height) {
            asked.add(component);
            super.addDirtyRegion(component, x, y, width, height);
        }
    }

    private static final class Rig {
        final ChartComponent chart;
        final ChartModuleHost host;
        final List<PageContents> announced = new ArrayList<>();

        Rig() {
            chart = new ChartComponent(Atlas.assembler());
            chart.setSize(900, 700);
            chart.setViewState(ChartViewState.DEFAULT);
            host = new ChartModuleHost(chart, new SelectionModel(),
                    request -> { });
            host.onPageChange(announced::add);
            announced.clear();
        }
    }

    @Test
    void aModuleChangeRepaintsTheChartAndRebuildsNothing() {
        RepaintManager before = RepaintManager.currentManager(null);
        RecordingRepaints repaints = new RecordingRepaints();
        RepaintManager.setCurrentManager(repaints);
        try {
            Rig rig = new Rig();
            MeridianModule module = rig.host.attach(new MeridianModule(
                    new juranometria.sky.Observer(59.913, 10.752,
                            java.time.Instant.parse(
                                    "2026-03-20T21:33:00Z"))));
            ChartScene sceneBefore = rig.chart.currentScene();
            PageContents inventoryBefore = rig.host.inventory();
            rig.announced.clear();
            repaints.asked.clear();

            // A real change, through the module's own surface: the
            // reader is now somewhere else, and the lines are wrong
            // until the chart paints again.
            module.observer(module.observer().from(-33.87, 151.21));

            assertTrue(repaints.asked.contains(rig.chart),
                    "the chart was asked to repaint: a redraw() that"
                            + " did nothing would leave stale lines on"
                            + " the page until something else happened"
                            + " to move");
            assertSame(sceneBefore, rig.chart.currentScene(),
                    "and the scene is the same object: nothing was"
                            + " reassembled, so no catalogue was"
                            + " queried");
            assertSame(inventoryBefore, rig.host.inventory(),
                    "and the inventory is the same object: nothing"
                            + " was rebuilt");
            assertEquals(List.of(), rig.announced,
                    "and no page-change was announced, because the"
                            + " page did not change - a module"
                            + " redrawing its own lines is not news");
        } finally {
            RepaintManager.setCurrentManager(before);
        }
    }

    @Test
    void redrawItselfRebuildsNothingHoweverOftenItIsAsked() {
        Rig rig = new Rig();
        ChartScene sceneBefore = rig.chart.currentScene();
        PageContents inventoryBefore = rig.host.inventory();
        ChartServices services = rig.host;

        for (int askedAgain = 0; askedAgain < 5; askedAgain++) {
            services.redraw();
        }

        assertSame(sceneBefore, rig.chart.currentScene(),
                "the scene is the same object after five of them");
        assertSame(inventoryBefore, rig.host.inventory(),
                "and so is the inventory");
        assertEquals(List.of(), rig.announced,
                "and nobody was told the page changed, because it"
                        + " did not");
    }

    @Test
    void theSameIdentitiesDoNotSurviveARealPageChange() {
        // The control. If a rebuild also returned the same instances,
        // the assertions above would pass whatever redraw() did, and
        // this whole class would be an elaborate way of asserting
        // nothing.
        Rig rig = new Rig();
        ChartScene sceneBefore = rig.chart.currentScene();
        PageContents inventoryBefore = rig.host.inventory();

        rig.chart.setViewState(new ChartViewState(
                new SkyPosition(83.822, -5.391), 8.0, 8.0));

        assertNotSame(sceneBefore, rig.chart.currentScene(),
                "a real page change reassembles the scene");
        assertNotSame(inventoryBefore, rig.host.inventory(),
                "and rebuilds the inventory");
        assertTrue(!rig.announced.isEmpty(),
                "and is announced - so the silence after redraw() is"
                        + " evidence, not deafness");
    }
}
