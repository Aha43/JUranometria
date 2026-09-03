package juranometria.tool;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import juranometria.chart.ChartScene;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;

/**
 * Where a great circle crosses the paper (Sprint 25, issue #225 —
 * the correction the gate review asked for).
 *
 * <p>The first draft of this gate said that a gnomonic projection
 * makes subdivision unnecessary and that the existing
 * {@code OverlayContribution.Path} was therefore sufficient. Both
 * halves of that were wrong together (review). A {@code Path} is a
 * <strong>polyline</strong>: a list of sky positions, which is
 * subdivision by another name — the study was passing 720 of them —
 * and a polyline cannot answer where the <em>infinite</em> circle
 * crosses the paper when every vertex it was given lies outside it.
 *
 * <p>So the gate names the smallest generic extension instead: the
 * chart should accept a <strong>great circle</strong>, and clip it
 * itself. A great circle is geometry, not astronomy — it is given by
 * its pole, and the chart learns nothing about meridians, horizons,
 * observers or time by being handed one.
 *
 * <h2>Why it can be exact</h2>
 *
 * <p>A gnomonic projection maps every great circle to a straight
 * line. So the projected circle is determined <em>exactly</em> by
 * any two points of it that project at all, and the line can then be
 * clipped to the paper analytically. No sampling decides what a
 * reader sees; sampling is used only to find two points anywhere on
 * the visible half, which is a hemisphere-sized target.
 *
 * <p>This is deliberately the opposite of Sprint 24's deep-sky
 * extents, which are not great circles and did need subdivision with
 * a measured bound.
 */
public final class GreatCircleOnPage {

    private GreatCircleOnPage() {
    }

    /** The two ends of the arc a page shows, in pixels. */
    public record Crossing(PixelPoint from, PixelPoint to) {
    }

    /**
     * Where the great circle with this pole crosses the paper, or
     * empty when it does not cross it at all.
     *
     * @param pole a direction perpendicular to every point of the
     *     circle - the whole of what the chart needs to be told
     */
    public static Optional<Crossing> clip(ChartScene scene,
                                          SkyPosition pole) {
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        Rectangle2D paper = ChartRenderer.paperOf(scene);

        // Two points of the visible half, which is half the sky:
        // a coarse walk always finds them if any exist, and their
        // pixels fix the line exactly because the projection is
        // gnomonic.
        List<PixelPoint> seen = new ArrayList<>();
        for (SkyPosition point : sample(pole, 180)) {
            projection.project(point).map(mapping::toPixel)
                    .ifPresent(seen::add);
        }
        if (seen.size() < 2) {
            return Optional.empty();      // the circle is the horizon
        }
        PixelPoint first = seen.get(0);
        PixelPoint second = furthestFrom(first, seen);
        if (Math.hypot(second.x() - first.x(), second.y() - first.y())
                < 1e-9) {
            return Optional.empty();
        }
        return clipLineToPaper(first, second, paper);
    }

    /**
     * Points around the great circle with this pole - used to find
     * the line, never to decide what is drawn.
     */
    public static List<SkyPosition> sample(SkyPosition pole, int samples) {
        double[] up = SkyOrientation.toVector(pole);
        double[] any = Math.abs(up[2]) < 0.9 ? new double[] {0, 0, 1}
                : new double[] {1, 0, 0};
        double[] first = normalised(cross(any, up));
        double[] second = normalised(cross(up, first));
        List<SkyPosition> circle = new ArrayList<>();
        for (int i = 0; i < samples; i++) {
            double angle = 2 * Math.PI * i / samples;
            double[] point = new double[3];
            for (int axis = 0; axis < 3; axis++) {
                point[axis] = Math.cos(angle) * first[axis]
                        + Math.sin(angle) * second[axis];
            }
            circle.add(SkyOrientation.toPosition(point));
        }
        return circle;
    }

    /**
     * The infinite line through two points, clipped to the paper -
     * Liang-Barsky, so a page whose corners the line only clips is
     * answered exactly rather than by inspecting vertices.
     */
    private static Optional<Crossing> clipLineToPaper(PixelPoint a,
                                                      PixelPoint b,
                                                      Rectangle2D paper) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double enter = Double.NEGATIVE_INFINITY;
        double leave = Double.POSITIVE_INFINITY;
        double[][] edges = {
            {-dx, a.x() - paper.getMinX()},
            {dx, paper.getMaxX() - a.x()},
            {-dy, a.y() - paper.getMinY()},
            {dy, paper.getMaxY() - a.y()},
        };
        for (double[] edge : edges) {
            double p = edge[0];
            double q = edge[1];
            if (Math.abs(p) < 1e-12) {
                if (q < 0) {
                    return Optional.empty();   // parallel and outside
                }
                continue;
            }
            double t = q / p;
            if (p < 0) {
                enter = Math.max(enter, t);
            } else {
                leave = Math.min(leave, t);
            }
        }
        if (enter > leave) {
            return Optional.empty();
        }
        return Optional.of(new Crossing(
                new PixelPoint(a.x() + enter * dx, a.y() + enter * dy),
                new PixelPoint(a.x() + leave * dx, a.y() + leave * dy)));
    }

    private static PixelPoint furthestFrom(PixelPoint from,
                                           List<PixelPoint> among) {
        PixelPoint best = from;
        double distance = -1;
        for (PixelPoint candidate : among) {
            double apart = Math.hypot(candidate.x() - from.x(),
                    candidate.y() - from.y());
            if (apart > distance) {
                distance = apart;
                best = candidate;
            }
        }
        return best;
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalised(double[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / norm, v[1] / norm, v[2] / norm};
    }

    // ---- the poles of the three geometries -------------------------

    /** The pole of the observer's meridian, in the chart's frame. */
    public static SkyPosition meridianPole(SkyOrientation.Observer observer,
                                           java.time.Instant instant) {
        // Perpendicular to both the celestial pole of date and the
        // zenith: the meridian is the circle through them both.
        double jd = SkyOrientation.julianDate(instant);
        double lst = SkyOrientation.normalise(SkyOrientation.gastDegrees(jd)
                + observer.eastLongitudeDegrees());
        return SkyOrientation.toJ2000(
                new SkyPosition(SkyOrientation.normalise(lst + 90.0), 0.0),
                jd, SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
    }

    /** The pole of the horizon is the zenith itself. */
    public static SkyPosition horizonPole(SkyOrientation.Observer observer,
                                          java.time.Instant instant) {
        return SkyOrientation.zenith(observer, instant,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
    }
}
