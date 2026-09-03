package juranometria.ui;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
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
 * <p>So this asks the real {@link ChartModuleHost} on a real
 * {@link ChartComponent}, and identity is the evidence: the host
 * caches its inventory and the component its scene, so a rebuild of
 * either produces a <em>new object</em>, and the same object
 * afterwards means the work was not done. The last test proves the
 * instrument can tell - the same identities survive a real page
 * change nowhere, so same-instance is a measurement and not a
 * tautology.
 */
class ModuleRedrawTest {

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
    void redrawRebuildsNothingAndTellsNobodyThePageChanged() {
        Rig rig = new Rig();
        ChartScene sceneBefore = rig.chart.currentScene();
        PageContents inventoryBefore = rig.host.inventory();
        ChartServices services = rig.host;

        for (int askedAgain = 0; askedAgain < 5; askedAgain++) {
            services.redraw();
        }

        assertSame(sceneBefore, rig.chart.currentScene(),
                "the scene is the same object: nothing was"
                        + " reassembled, so no catalogue was queried");
        assertSame(inventoryBefore, rig.host.inventory(),
                "and the inventory is the same object: nothing was"
                        + " rebuilt");
        assertEquals(List.of(), rig.announced,
                "and no page-change was announced, because the page"
                        + " did not change - a module redrawing its"
                        + " own lines is not news");
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
