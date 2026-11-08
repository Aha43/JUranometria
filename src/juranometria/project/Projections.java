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
        Projection told = STUDY.get();
        if (told != null) {
            return told;
        }
        return switch (kind) {
            case GNOMONIC -> new GnomonicProjection(centre);
            case STEREOGRAPHIC -> new StereographicProjection(centre);
            case ORTHOGRAPHIC -> new OrthographicProjection(centre);
        };
    }

    /**
     * <strong>Study door (#686), removed by #687.</strong>
     *
     * <p>Runs {@code work} on this thread with every projection the
     * atlas derives answered by {@code told}, so the all-sky study can
     * see real production ink - the renderer, the reference layer,
     * the modules' contributions, labels, hit testing - drawn by a
     * candidate whole-sky projection that production does not have.
     * The same need, and the same answer, as the celestial-globe
     * gate's doors (docs/decisions/celestial-globe.md, "The three
     * study doors"): nothing else could show the study what the
     * existing passes do with a seamed map.
     *
     * <p>Only the tool package may open it ({@code StudyDoorTest}),
     * it is shut by default and after the work however the work ends,
     * and it changes nothing about any page drawn outside it. #687
     * removes it whatever the ruling: if a projection is admitted it
     * becomes a real kind, and if not the study that needed it goes.
     */
    public static <T> T drawnForStudy(Projection told,
                                      java.util.concurrent.Callable<T> work)
            throws Exception {
        if (told == null || work == null) {
            throw new IllegalArgumentException(
                    "a study door opens onto a projection and some work");
        }
        if (STUDY.get() != null) {
            throw new IllegalStateException("the study door is already open");
        }
        STUDY.set(told);
        try {
            return work.call();
        } finally {
            STUDY.remove();
        }
    }

    /** Whether the study door is open on this thread. */
    public static boolean studyDoorOpen() {
        return STUDY.get() != null;
    }

    private static final ThreadLocal<Projection> STUDY = new ThreadLocal<>();

    /** The projection a viewport is drawn by. */
    public static Projection forViewport(ChartViewport viewport) {
        return of(viewport.projection(), viewport.centre());
    }
}
