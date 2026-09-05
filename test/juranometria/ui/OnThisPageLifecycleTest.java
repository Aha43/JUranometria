package juranometria.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.WorkingSelection;
import juranometria.ui.onthispage.OnThisPageModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A module's lifecycle, in the running application (issue #216).
 *
 * <p>Removable is a claim about the way out as much as the way in.
 * Disposing the panel a module put its table in is not releasing the
 * module: it would still hold its subscriptions, still be
 * contributing geometry, and still be listening to a chart on its
 * way out (review). The architecture is only as good as the last
 * line that honours it, and the next module inherits whatever this
 * one establishes.
 */
class OnThisPageLifecycleTest {

    private static ChartComponent chart() throws Exception {
        ChartComponent[] made = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            made[0] = new ChartComponent(Atlas.assembler());
            made[0].setSize(900, 700);
            made[0].setViewState(ChartViewState.DEFAULT);
        });
        return made[0];
    }

    @Test
    void detachingReleasesEverythingTheModuleWasHolding() throws Exception {
        ChartComponent chart = chart();
        ChartModuleHost host = new ChartModuleHost(chart,
                new SelectionModel(), request -> { });
        OnThisPageModule module = host.attach(new OnThisPageModule());

        String first = host.inventory().entries().get(0).identity();
        host.workingSelection().add(first);
        assertTrue(chart.overlays().holds(OnThisPageModule.ID),
                "attached, it is contributing");

        host.detachAll();

        assertTrue(!chart.overlays().holds(OnThisPageModule.ID),
                "detached, its ink is withdrawn");
        assertEquals(0, chart.overlays().collect().size(),
                "and the chart has nothing left of it to draw");

        // Its table is unsubscribed too, so a later change reaches
        // nobody: a module that kept listening after being released
        // is a leak wearing a lifecycle.
        List<WorkingSelection.Change> afterwards = new ArrayList<>();
        host.workingSelection().onChange(afterwards::add);
        afterwards.clear();
        host.workingSelection().clear();
        assertEquals(1, afterwards.size(),
                "the model still works for whoever is left listening");
        assertEquals(0, chart.overlays().collect().size(),
                "and the detached module contributes nothing to it");
    }

    @Test
    void aDetachedModuleCanBeAttachedAgain() throws Exception {
        // Because a lifecycle that only works once is a lifecycle
        // that has not been thought through: the registry refuses a
        // second registration under one name, so detaching has to
        // have really let go.
        ChartComponent chart = chart();
        ChartModuleHost host = new ChartModuleHost(chart,
                new SelectionModel(), request -> { });
        host.attach(new OnThisPageModule());
        host.detachAll();

        host.attach(new OnThisPageModule());
        assertTrue(chart.overlays().holds(OnThisPageModule.ID),
                "attached again, on the same chart");
        host.detachAll();
    }

    @Test
    void movingThePageDoesNotRevivetADetachedModule() throws Exception {
        ChartComponent chart = chart();
        ChartModuleHost host = new ChartModuleHost(chart,
                new SelectionModel(), request -> { });
        host.attach(new OnThisPageModule());
        host.workingSelection().add(host.inventory().entries().get(0).identity());
        host.detachAll();

        SwingUtilities.invokeAndWait(() -> chart.setViewState(
                new ChartViewState(new SkyPosition(83.822, -5.391), 8.0, 8.0)));
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(0, chart.overlays().collect().size(),
                "a page change reaches no module that has gone");
    }

    @Test
    void theApplicationReleasesItsModulesOnTheWayOut() throws Exception {
        // The wiring itself, because this is the line the review
        // found missing: the application disposed the panel and left
        // the module attached to a chart that was being torn down.
        String code = Files.readString(
                        Path.of("src/juranometria/app/JUranometriaMain.java"))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");

        assertTrue(code.contains("shutdown.onShutdown(modules::detachAll)"),
                "leaving releases the modules on the same path that"
                        + " flushes preferences and disposes windows,"
                        + " rather than only disposing what they put"
                        + " on screen");
        assertTrue(code.indexOf("shutdown.onShutdown(modules::detachAll)")
                        > code.indexOf("shutdown.onShutdown(inspector::dispose)"),
                "and registered after the inspector, so it is detached"
                        + " before the panel holding its table goes -"
                        + " detachments run newest first");
    }
}
