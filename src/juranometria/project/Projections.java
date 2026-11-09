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
 *
 * <p>The #686 study opened a thread-scoped door here so that a
 * candidate projection could reach production ink before it existed;
 * #687 made the candidate a real kind and removed the door, as the
 * study said it would.
 */
public final class Projections {

    private Projections() {
    }

    public static Projection of(ChartProjection kind, SkyPosition centre) {
        return switch (kind) {
            case GNOMONIC -> new GnomonicProjection(centre);
            case STEREOGRAPHIC -> new StereographicProjection(centre);
            case ORTHOGRAPHIC -> new OrthographicProjection(centre);
            case HAMMER -> new HammerProjection(centre);
        };
    }

    /** The projection a viewport is drawn by. */
    public static Projection forViewport(ChartViewport viewport) {
        return of(viewport.projection(), viewport.centre());
    }
}
