package juranometria.sky;

import java.util.ArrayList;
import java.util.List;

import juranometria.chart.SkyPosition;

/**
 * A great circle, carried by its pole (Sprint 25, issue #226).
 *
 * <p>The gate decided this shape after rejecting a polyline. A list
 * of positions is subdivision by another name, and no list of
 * vertices can say where the <em>infinite</em> circle crosses a page
 * when every vertex lies outside it - a one-degree page with a
 * circle sampled at eight points has no vertex anywhere near the
 * paper.
 *
 * <p>A pole is a direction, and nothing more: this type knows
 * nothing of meridians, horizons, observers or time, and a future
 * module drawing a galactic equator would use it unchanged.
 *
 * <p>Because a gnomonic projection maps every great circle to a
 * straight line, a chart given the pole can be <strong>exact</strong>
 * about where the circle crosses its paper, with no sampling
 * deciding what a reader sees. {@link #around} exists for callers
 * that want points anyway - a test, or a projection that is not
 * gnomonic - and is deliberately not how the chart draws it.
 */
public record GreatCircle(SkyPosition pole) {

    public GreatCircle {
        if (pole == null) {
            throw new IllegalArgumentException(
                    "a great circle is given by its pole");
        }
    }

    /** Whether a position lies on this circle, to a tolerance. */
    public boolean contains(SkyPosition position, double toleranceDegrees) {
        return Math.abs(90.0 - pole.separationDegrees(position))
                <= toleranceDegrees;
    }

    /**
     * Points around the circle, evenly spaced in angle.
     *
     * <p>For consumers that need positions rather than the circle
     * itself. What a page shows is decided by clipping the projected
     * line, not by these.
     */
    public List<SkyPosition> around(int samples) {
        if (samples < 3) {
            throw new IllegalArgumentException(
                    "a circle needs at least three points to be walked:"
                            + " " + samples);
        }
        double[] up = SkyFrame.toVector(pole);
        double[] seed = Math.abs(up[2]) < 0.9
                ? new double[] {0, 0, 1} : new double[] {1, 0, 0};
        double[] first = normalised(cross(seed, up));
        double[] second = normalised(cross(up, first));

        List<SkyPosition> circle = new ArrayList<>();
        for (int i = 0; i < samples; i++) {
            double angle = 2 * Math.PI * i / samples;
            double[] point = new double[3];
            for (int axis = 0; axis < 3; axis++) {
                point[axis] = Math.cos(angle) * first[axis]
                        + Math.sin(angle) * second[axis];
            }
            circle.add(SkyFrame.toPosition(point));
        }
        return List.copyOf(circle);
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalised(double[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / norm, v[1] / norm, v[2] / norm};
    }
}
