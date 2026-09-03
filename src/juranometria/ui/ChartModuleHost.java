package juranometria.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.NavigationRequest;
import juranometria.module.OverlayContribution;
import juranometria.page.LeadSelection;
import juranometria.page.PageContents;
import juranometria.page.PageInventory;
import juranometria.page.WorkingMarksModel;
import juranometria.render.ChartOptions;

/**
 * The chart, offered to its modules (Sprint 24, issue #216).
 *
 * <p>This is the only place that knows both sides. The chart core
 * still knows nothing of modules; a module still knows nothing of
 * windows, components or layout. What sits between them is this
 * host: it builds the inventory when the page changes, keeps the
 * working marks, hands navigation requests to the chart's own
 * controller, and collects contributed geometry for the component to
 * ink.
 *
 * <p>The inventory is <strong>cached</strong> and rebuilt on a page
 * change - centre, field, size, magnitude limit or a chart option -
 * and never during painting, which is what makes "painting performs
 * no catalogue query" true of the running application rather than
 * only of the service.
 */
public final class ChartModuleHost implements ChartServices {

    private final ChartComponent chart;
    private final SelectionModel selection;
    private final WorkingMarksModel marks = new WorkingMarksModel();
    private final Consumer<NavigationRequest> navigation;
    private final List<Consumer<PageContents>> pageListeners =
            new ArrayList<>();
    private final List<ChartModule> attached = new ArrayList<>();

    private PageContents inventory = PageContents.EMPTY;

    public ChartModuleHost(ChartComponent chart, SelectionModel selection,
                           Consumer<NavigationRequest> navigation) {
        if (chart == null || selection == null || navigation == null) {
            throw new IllegalArgumentException(
                    "the host needs a chart, its selection and a way to"
                            + " honour a navigation request");
        }
        this.chart = chart;
        this.selection = selection;
        this.navigation = navigation;
        rebuild();
        chart.onSceneChange(this::rebuild);
        // And when the reader changes what the chart draws: the
        // scene is the same sky, but what can be seen of it is not,
        // and an inventory that reported visibility from a stale
        // options set would call a hidden object drawn.
        chart.onChartOptionsChange(this::rebuild);
        LeadSelection.connect(marks, selection, this::inventory);
        // A mark changes what the page shows, and nothing else does
        // it for us: the crosses are painted from the marks, so the
        // component has to be told to paint again.
        marks.onChange(change -> chart.repaint());
    }

    /** Attaches a module and returns it, for the caller to keep. */
    public <T extends ChartModule> T attach(T module) {
        module.attach(this);
        attached.add(module);
        return module;
    }

    /** Releases every module, in reverse order of attachment. */
    public void detachAll() {
        for (int i = attached.size() - 1; i >= 0; i--) {
            attached.get(i).detach();
        }
        attached.clear();
    }

    /** Rebuilds the inventory and tells whoever is listening. */
    private void rebuild() {
        ChartScene scene = chart.currentScene();
        inventory = scene == null ? PageContents.EMPTY
                : PageInventory.of(scene, chart.chartOptions());
        for (Consumer<PageContents> listener : List.copyOf(pageListeners)) {
            listener.accept(inventory);
        }
        chart.repaint();
    }

    // ---- the services --------------------------------------------

    @Override
    public ChartViewState viewState() {
        return chart.viewState();
    }

    @Override
    public ChartScene scene() {
        return chart.currentScene();
    }

    @Override
    public ChartOptions options() {
        return chart.chartOptions();
    }

    @Override
    public PageContents inventory() {
        return inventory;
    }

    @Override
    public Runnable onPageChange(Consumer<PageContents> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        pageListeners.add(listener);
        listener.accept(inventory);
        return () -> pageListeners.remove(listener);
    }

    @Override
    public Projection projection() {
        return new Projection() {
            @Override
            public Optional<double[]> toPage(SkyPosition position) {
                ChartScene scene = chart.currentScene();
                if (scene == null) {
                    return Optional.empty();
                }
                return new juranometria.project.GnomonicProjection(
                        scene.viewport().centre()).project(position)
                        .map(plane -> {
                            var pixel = new juranometria.project
                                    .ViewportMapping(scene.viewport())
                                    .toPixel(plane);
                            return new double[] {pixel.x(), pixel.y()};
                        });
            }

            @Override
            public SkyPosition toSky(double x, double y) {
                ChartScene scene = chart.currentScene();
                return scene == null ? null
                        : juranometria.render.ChartHitTest.skyAt(scene, x, y);
            }
        };
    }

    @Override
    public SelectionModel selection() {
        return selection;
    }

    @Override
    public WorkingMarksModel workingMarks() {
        return marks;
    }

    @Override
    public void request(NavigationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("a request to honour");
        }
        // Handed on, not obeyed here: whether the chart can go there
        // at this field is the controller's decision, and a module
        // must not be able to put the page somewhere the atlas would
        // refuse to draw.
        navigation.accept(request);
    }

    @Override
    public void redraw() {
        // Paint, and nothing else: no rebuild, so no catalogue query
        // and no inventory. What a module changed is its own
        // geometry, which the chart pulls when it paints.
        chart.repaint();
    }

    @Override
    public Runnable contribute(String moduleId,
                               Supplier<List<OverlayContribution>> geometry) {
        Runnable withdraw = chart.overlays().offer(moduleId, geometry);
        chart.repaint();
        return () -> {
            withdraw.run();
            chart.repaint();
        };
    }
}
