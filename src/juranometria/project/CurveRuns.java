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

    /** What a run of this curve looks like, from an angle and a span. */
    interface ArcOf {
        CurveRun.Arc from(double startRadians, double spanRadians,
                          PixelPoint start, PixelPoint end);
    }

    static List<CurveRun> of(List<Double> crossings,
                             DoubleFunction<PixelPoint> at, ArcOf arc,
                             PageRegion region) {
        if (crossings.isEmpty()) {
            PixelPoint any = at.apply(0.0);
            return region.contains(any.x(), any.y())
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
