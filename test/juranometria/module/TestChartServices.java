package juranometria.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.page.PageContents;
import juranometria.page.PageInventory;
import juranometria.page.WorkingMarksModel;
import juranometria.render.ChartOptions;

/**
 * The chart, as a module is allowed to see it.
 *
 * <p>Shared by every module's tests, so that "what a module can
 * reach" is written down once. It records what modules asked for -
 * the navigation they requested, the redraws they wanted, how often
 * they read the inventory - because most of what a module must
 * <em>not</em> do is a question about which of these was called.
 */
public final class TestChartServices implements ChartServices {

    private final ChartViewState state = ChartViewState.DEFAULT;
    private final ChartScene scene =
            Atlas.assembler().assemble(ChartViewState.DEFAULT, 900, 700);
    private final ChartOptions options = ChartOptions.DEFAULTS;
    private final PageContents inventory =
            PageInventory.of(
                Atlas.assembler().assemble(ChartViewState.DEFAULT,
                        900, 700), ChartOptions.DEFAULTS);
    private final SelectionModel selection = new SelectionModel();
    private final WorkingMarksModel marks = new WorkingMarksModel();
    public final List<NavigationRequest> requested = new ArrayList<>();
    public final OverlayRegistry overlays = new OverlayRegistry();

    /** How many times a module asked for the page to be drawn again. */
    public int redraws;

    /** How many times a module read what is on the page. */
    public int inventoryReads;

    @Override public ChartViewState viewState() {
        return state;
    }

    @Override public ChartScene scene() {
        return scene;
    }

    @Override public ChartOptions options() {
        return options;
    }

    @Override public PageContents inventory() {
        inventoryReads++;
        return inventory;
    }

    @Override public Runnable onPageChange(Consumer<PageContents> listener) {
        listener.accept(inventory);
        return () -> { };
    }

    @Override public Projection projection() {
        return new Projection() {
            @Override public Optional<double[]> toPage(SkyPosition at) {
                return new juranometria.project.GnomonicProjection(
                        scene.viewport().centre()).project(at)
                        .map(plane -> {
                            var pixel = new juranometria.project
                                    .ViewportMapping(scene.viewport())
                                    .toPixel(plane);
                            return new double[] {pixel.x(), pixel.y()};
                        });
            }

            @Override public SkyPosition toSky(double x, double y) {
                return juranometria.render.ChartHitTest.skyAt(scene, x, y);
            }
        };
    }

    @Override public SelectionModel selection() {
        return selection;
    }

    @Override public WorkingMarksModel workingMarks() {
        return marks;
    }

    @Override public void request(NavigationRequest request) {
        requested.add(request);
    }

    @Override public void redraw() {
        redraws++;
    }

    @Override public Runnable contribute(String moduleId,
            java.util.function.Supplier<List<OverlayContribution>> geometry) {
        return overlays.offer(moduleId, geometry);
    }
}
