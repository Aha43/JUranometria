package juranometria.module;

import java.util.List;
import java.util.function.Consumer;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.page.PageContents;
import juranometria.page.WorkingMarksModel;
import juranometria.render.ChartOptions;

/**
 * What the chart offers a module (Sprint 24, issue #215).
 *
 * <p>The chart publishes services; modules consume them. The core
 * knows nothing of tables, sidebars, crosses, meridians, observers,
 * clocks or planets, and the atlas constructs and renders its
 * ordinary chart with every module absent.
 *
 * <p>These are the reviewed capabilities, and there is deliberately
 * no eighth: no catalogue handle, no renderer, no graphics context,
 * no window. A module that needed one of those would be a module
 * inventing cartography the atlas has not decided.
 */
public interface ChartServices {

    /** The page as it stands: the immutable state a reader chose. */
    ChartViewState viewState();

    /** The assembled scene that state produced. */
    ChartScene scene();

    /** The options the page is drawn under. */
    ChartOptions options();

    /** What is on the paper, with visibility from production truth. */
    PageContents inventory();

    /** Told whenever the page changes; returns the unsubscribe handle. */
    Runnable onPageChange(Consumer<PageContents> listener);

    /** Sky to page and back - the renderer's own, never reimplemented. */
    Projection projection();

    /** The chart's existing singular selection. */
    SelectionModel selection();

    /** The ordered marked set and its lead. */
    WorkingMarksModel workingMarks();

    /**
     * Asks the chart to move. Deliberate and explicit: a module
     * never moves the chart as a side effect of a reader reading.
     */
    void request(NavigationRequest request);

    /**
     * Offers geometry for the chart to ink, under this module's own
     * name, and returns the handle that withdraws it.
     *
     * <p>Owned, because a bare list let one module replace another's
     * ink and left a detaching module no way to take back its own
     * (review). Pulled rather than pushed: the chart asks when it
     * paints, so a module never guesses when the page is next drawn.
     */
    Runnable contribute(String moduleId,
                        java.util.function.Supplier<List<OverlayContribution>>
                                geometry);

    /** Sky to page and back, as a module is allowed to ask it. */
    interface Projection {

        /**
         * Where this sky position lands on the page, or empty when
         * the projection has nothing to say.
         */
        java.util.Optional<double[]> toPage(
                juranometria.chart.SkyPosition position);

        /** What sky a page position stands over, or null off the sky. */
        juranometria.chart.SkyPosition toSky(double x, double y);
    }
}
