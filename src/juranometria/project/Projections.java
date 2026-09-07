package juranometria.project;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;

/**
 * The one place a projection name becomes a projection.
 *
 * <p>Everything else asks a {@link Projection} for an answer rather
 * than asking which projection it is, so this is the only switch on
 * the kind in the atlas - which is what keeps a second projection
 * from becoming a second set of special cases scattered through the
 * chart.
 */
public final class Projections {

    private Projections() {
    }

    public static Projection of(ChartProjection kind, SkyPosition centre) {
        return switch (kind) {
            case GNOMONIC -> new GnomonicProjection(centre);
            case STEREOGRAPHIC -> new StereographicProjection(centre);
        };
    }

    /** The projection a viewport is drawn by. */
    public static Projection forViewport(ChartViewport viewport) {
        return of(viewport.projection(), viewport.centre());
    }
}
