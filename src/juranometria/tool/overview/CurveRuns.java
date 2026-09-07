package juranometria.tool.overview;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * Turning crossings into visible runs (issue #296).
 *
 * <p>A closed curve crosses the page boundary at some set of angles.
 * Between two consecutive crossings it is wholly on the paper or
 * wholly off it, so one point decides each piece, and the pieces
 * that are on are joined up into runs. A circle and an ellipse share
 * this entirely: the only thing that differs is the arithmetic that
 * finds the crossings, which is why it lives here once.
 *
 * <p>The joining is the part that is easy to get wrong. Not every
 * crossing is a way in or out - a curve that grazes an edge, or
 * passes through a corner where two edges both report it, produces
 * angles that divide a run without ending it.
 */
final class CurveRuns {

    /** Two crossings nearer than this are one grazing touch. */
    static final double GRAZE = 1.0e-7;

    private CurveRuns() {
    }

    /**
     * @param crossings angles where the curve meets the page edges
     * @param at        the curve's point at an angle
     * @param arc       the curve's arc from an angle through a span
     * @param whole     the entire closed curve, for a run with no ends
     */
    static List<PlaneCurve.Run> of(List<Double> crossings,
                                  DoubleFunction<Point2D> at,
                                  Arc arc, PageRegion region,
                                  Shape whole) {
        if (crossings.isEmpty()) {
            // Either the curve lies wholly on the paper, or nowhere
            // near it. One point on it decides which.
            return region.contains(at.apply(0.0))
                    ? List.of(new PlaneCurve.Run(whole, null, null))
                    : List.of();
        }
        crossings = new ArrayList<>(crossings);
        crossings.sort(Double::compare);
        for (int i = crossings.size() - 1; i > 0; i--) {
            if (crossings.get(i) - crossings.get(i - 1) < GRAZE) {
                crossings.remove(i);
            }
        }
        if (crossings.size() > 1 && crossings.get(0) + 2.0 * Math.PI
                - crossings.get(crossings.size() - 1) < GRAZE) {
            crossings.remove(crossings.size() - 1);
        }

        int count = crossings.size();
        boolean[] inside = new boolean[count];
        for (int i = 0; i < count; i++) {
            double start = crossings.get(i);
            inside[i] = region.contains(at.apply(start
                    + span(crossings.get((i + 1) % count), start) / 2.0));
        }
        boolean all = true;
        for (boolean each : inside) {
            all &= each;
        }
        if (all) {
            return List.of(new PlaneCurve.Run(whole, null, null));
        }

        List<PlaneCurve.Run> runs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (!inside[i] || inside[(i - 1 + count) % count]) {
                continue;  // not the start of a run
            }
            int last = i;
            while (inside[(last + 1) % count]) {
                last = (last + 1) % count;
            }
            double start = crossings.get(i);
            double through = span(crossings.get((last + 1) % count), start);
            runs.add(new PlaneCurve.Run(arc.from(start, through),
                    at.apply(start), at.apply(start + through)));
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

    /** A curve's arc, from an angle through a span. */
    interface Arc {
        Shape from(double start, double span);
    }
}
