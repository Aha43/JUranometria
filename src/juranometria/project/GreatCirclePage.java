package juranometria.project;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Where a great circle crosses a page (Sprint 25, issue #226).
 *
 * <p>Geometry, not astronomy. This class is given a circle's pole
 * and a rectangle, and it answers with the arc a reader would see -
 * it has never heard of meridians, horizons, observers or time, and
 * the same code would clip a galactic equator.
 *
 * <h2>Why it can be exact</h2>
 *
 * <p>A gnomonic projection maps every great circle to a straight
 * line. So the projected circle is fixed <em>exactly</em> by any two
 * of its points that project at all, and the line is then clipped to
 * the rectangle analytically. Nothing a reader sees is decided by
 * sampling: sampling is used only to find two points somewhere on
 * the visible half, which is half the sky.
 *
 * <p>That is the opposite of Sprint 24's deep-sky extents, which are
 * not great circles and did need subdivision to a measured bound.
 * The gate measured this one at 0.0000 px of deviation across every
 * field the atlas offers.
 *
 * <h2>What it refuses</h2>
 *
 * <p>A circle whose pole is the page's own centre <em>is</em> the
 * projection's horizon: it projects to infinity in every direction
 * and no part of it is on any page. That returns empty, as does a
 * circle that simply misses the paper. Silence is the honest answer
 * to a line that is not there.
 */
public final class GreatCirclePage {

    private GreatCirclePage() {
    }

    /** The two ends of the arc a page shows. */
    public record Arc(PixelPoint from, PixelPoint to) {

        public Arc {
            if (from == null || to == null) {
                throw new IllegalArgumentException("an arc has two ends");
            }
        }

        /** How long the arc is on the page, in pixels. */
        public double lengthPx() {
            return Math.hypot(to.x() - from.x(), to.y() - from.y());
        }
    }

    /**
     * Where the great circle with this pole crosses the rectangle.
     *
     * @param points positions around the circle, from which two
     *     that project are taken; what they are is unimportant, and
     *     none of them need be on the page
     */
    public static Optional<Arc> clip(GnomonicProjection projection,
                                     ViewportMapping mapping,
                                     Rectangle2D paper,
                                     List<SkyPosition> points) {
        PixelPoint first = null;
        PixelPoint furthest = null;
        double apart = -1;
        for (SkyPosition point : points) {
            PixelPoint at = projection.project(point)
                    .map(mapping::toPixel).orElse(null);
            if (at == null) {
                continue;
            }
            if (first == null) {
                first = at;
                continue;
            }
            double away = Math.hypot(at.x() - first.x(), at.y() - first.y());
            if (away > apart) {
                apart = away;
                furthest = at;
            }
        }
        if (first == null || furthest == null || apart < 1e-9) {
            return Optional.empty();
        }
        return clipToRectangle(first, furthest, paper);
    }

    /**
     * The infinite line through two points, clipped to a rectangle -
     * Liang-Barsky, so a page the line only catches at a corner is
     * answered exactly rather than by inspecting vertices.
     */
    private static Optional<Arc> clipToRectangle(PixelPoint a, PixelPoint b,
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
                    return Optional.empty();
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
        return Optional.of(new Arc(
                new PixelPoint(a.x() + enter * dx, a.y() + enter * dy),
                new PixelPoint(a.x() + leave * dx, a.y() + leave * dy)));
    }
}
