package juranometria.project;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * Turning crossings into visible runs (issue #298).
 *
 * <p>A closed curve crosses a page's boundary at some set of angles.
 * Between two consecutive crossings it is wholly on the page or
 * wholly off it, so one point decides each piece, and the pieces
 * that are on are joined into runs. A circle and an ellipse share
 * this entirely; only the arithmetic that finds the crossings
 * differs, which is why it lives here once.
 *
 * <p>The joining is the part that is easy to get wrong. Not every
 * crossing is a way in or out: a curve that grazes an edge, or
 * passes through a corner where two edges both report it, produces
 * angles that divide a run without ending it. The gate found this by
 * getting a page's equator back in three pieces on one side and one
 * on the other, from geometry that is symmetric.
 */
final class CurveRuns {

    /** Two crossings nearer than this are one grazing touch. */
    static final double GRAZE = 1.0e-7;

    private CurveRuns() {
    }

    /**
     * How many points decide a curve that crosses nothing (Sprint 32,
     * issue #331, step four).
     *
     * <p>Such a curve is wholly on the page or wholly off it, so in
     * principle one point settles it. In practice one point did not,
     * and the point chosen was the worst available: a great circle
     * seen on a hemisphere projects to an ellipse <em>inscribed</em>
     * in the limb, touching it exactly at the two ends of its major
     * axis - and the point asked was at angle zero, which is one of
     * them.
     *
     * <p>So the answer came from the last bit of a subtraction. The
     * sample sat 405.000000000 page units from the middle and the sky
     * stopped at 405.000000000; the difference was a tenth of a
     * picometre of nothing, and its sign decided whether the line was
     * drawn. Swept over 132 orientations, that silently removed the
     * ecliptic from 44 per cent of them, the meridian from 33 and the
     * mathematical horizon from 36 - a reader turning the globe saw
     * them blink.
     *
     * <p>Several points, evenly spaced, so that no one of them is
     * privileged. What makes that enough - with no tolerance anywhere
     * - is the shape of the failure rather than the count: an
     * inscribed ellipse touches the limb only at the two ends of its
     * major axis, and the ends of its <em>minor</em> axis are
     * strictly inside by the whole difference between the two radii,
     * which is pixels and not picometres. Any sample away from the
     * tangency decides the curve honestly.
     *
     * <p>A tolerance for points evaluating a unit or two in the last
     * place outside the limb was written here and then taken out. The
     * case it was written for is the circle that <em>is</em> the limb
     * - the horizon of a page centred on the zenith - and measuring
     * it showed it was never at risk: its radius comes out exactly
     * equal to the limb's, and its samples evaluate at the radius or
     * a hair inside it, never outside. No mutation could make the
     * tolerance matter, and a guard nothing can exercise is a guard
     * nobody can trust.
     */
    private static final int DECIDING_SAMPLES = 8;

    /** What a run of this curve looks like, from an angle and a span. */
    interface ArcOf {
        CurveRun.Arc from(double startRadians, double spanRadians,
                          PixelPoint start, PixelPoint end);
    }

    /**
     * Whether any of this curve is on the page, asked of the curve
     * rather than of one point of it.
     *
     * <p>Sound because of what brought us here: the caller found no
     * crossings, so the curve does not pass through the boundary
     * anywhere, so one sample being on the page means all of it is.
     * Touching is not crossing - an inscribed ellipse touches the
     * limb twice and never leaves - which is why a sample lying on
     * the limb within its own evaluation error counts as on the sky.
     */
    private static boolean anywhereOn(DoubleFunction<PixelPoint> at,
                                      PageRegion region) {
        for (int sample = 0; sample < DECIDING_SAMPLES; sample++) {
            PixelPoint point = at.apply(
                    sample * 2.0 * Math.PI / DECIDING_SAMPLES);
            if (region.contains(point.x(), point.y())) {
                return true;
            }
        }
        return false;
    }

    static List<CurveRun> of(List<Double> crossings,
                             DoubleFunction<PixelPoint> at, ArcOf arc,
                             PageRegion region) {
        if (crossings.isEmpty()) {
            return anywhereOn(at, region)
                    ? List.of(arc.from(0.0, 2.0 * Math.PI, null, null))
                    : List.of();
        }
        List<Double> sorted = new ArrayList<>(crossings);
        sorted.sort(Double::compare);
        for (int i = sorted.size() - 1; i > 0; i--) {
            if (sorted.get(i) - sorted.get(i - 1) < GRAZE) {
                sorted.remove(i);
            }
        }
        if (sorted.size() > 1 && sorted.get(0) + 2.0 * Math.PI
                - sorted.get(sorted.size() - 1) < GRAZE) {
            sorted.remove(sorted.size() - 1);
        }

        int count = sorted.size();
        boolean[] inside = new boolean[count];
        for (int i = 0; i < count; i++) {
            double start = sorted.get(i);
            PixelPoint middle = at.apply(start
                    + span(sorted.get((i + 1) % count), start) / 2.0);
            inside[i] = region.contains(middle.x(), middle.y());
        }
        boolean all = true;
        for (boolean each : inside) {
            all &= each;
        }
        if (all) {
            return List.of(arc.from(0.0, 2.0 * Math.PI, null, null));
        }

        List<CurveRun> runs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (!inside[i] || inside[(i - 1 + count) % count]) {
                continue;  // not the start of a run
            }
            int last = i;
            while (inside[(last + 1) % count]) {
                last = (last + 1) % count;
            }
            double start = sorted.get(i);
            double through = span(sorted.get((last + 1) % count), start);
            runs.add(arc.from(start, through, at.apply(start),
                    at.apply(start + through)));
        }
        return runs;
    }

    static double span(double end, double start) {
        double span = end - start;
        return span <= 0.0 ? span + 2.0 * Math.PI : span;
    }

    /** Normalised to [0, 2pi). */
    static double angle(double y, double x) {
        double angle = Math.atan2(y, x);
        return angle < 0.0 ? angle + 2.0 * Math.PI : angle;
    }
}
