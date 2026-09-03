package juranometria.project;

import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Where a great circle crosses a page (Sprint 25, issue #226).
 *
 * <p>Geometry, not astronomy. This class is given a circle's
 * <strong>pole</strong> and a rectangle of page, and it answers with
 * the arc a reader would see - it has never heard of meridians,
 * horizons, observers or time, and the same code would clip a
 * galactic equator.
 *
 * <p>It takes the pole rather than points on the circle. An earlier
 * version took a list of positions, which left every caller
 * responsible for the sampling the gate rejected and made it their
 * job to sample finely enough - exactly the failure mode the
 * analytic clip exists to remove (review). Given the pole, there is
 * no sampling left to be responsible for.
 *
 * <p>Its boundary is {@link Page}, four numbers, rather than an AWT
 * rectangle: this is projection geometry, and a package that draws
 * nothing should not need a windowing toolkit to describe a
 * rectangle (review).
 *
 * <h2>Why it can be exact</h2>
 *
 * <p>A gnomonic projection maps every great circle to a straight
 * line, and the line has a closed form. A visible direction
 * {@code p} at plane coordinates {@code (xi, eta)} is
 * {@code p = cos(d) * (xi*east + eta*north + centre)}, so the
 * circle's defining condition {@code pole . p = 0} becomes
 *
 * <pre>  (pole.east) * xi + (pole.north) * eta + (pole.centre) = 0</pre>
 *
 * <p>which is the equation of a straight line in the plane, with no
 * approximation and nothing to choose. The pixel map is affine, so
 * that line stays a line in pixels, and it is then clipped to the
 * rectangle analytically. Not one number a reader sees comes from a
 * sample or a tolerance.
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

    /**
     * The page to clip against: the rectangle of pixels the chart
     * is willing to draw in.
     */
    public record Page(double minX, double minY, double maxX, double maxY) {

        public Page {
            if (!(maxX > minX) || !(maxY > minY)) {
                // Not-a-number fails this too: no comparison with
                // NaN is true, so a NaN edge never gets past it.
                throw new IllegalArgumentException(
                        "a page has width and height: " + minX + ","
                                + minY + " to " + maxX + "," + maxY);
            }
            if (Double.isInfinite(minX) || Double.isInfinite(minY)
                    || Double.isInfinite(maxX) || Double.isInfinite(maxY)) {
                // An infinite edge passes the width and height check
                // and then poisons the clipping: the parameter along
                // the line comes out as infinity or NaN, and the arc
                // that comes back has ends no renderer can draw
                // (review). A page is a rectangle of pixels, and
                // there is no such pixel.
                throw new IllegalArgumentException(
                        "a page is a finite rectangle of pixels: "
                                + minX + "," + minY + " to " + maxX
                                + "," + maxY);
            }
        }

        public boolean contains(double x, double y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
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
     * Where the great circle with this pole crosses the page.
     *
     * @param pole a direction perpendicular to every point of the
     *     circle - the whole of what this needs to be told
     */
    public static Optional<Arc> clip(GnomonicProjection projection,
                                     ViewportMapping mapping,
                                     Page paper,
                                     SkyPosition pole) {
        double[] axis = unit(pole);
        double[] centre = unit(projection.centre());
        double ra = Math.toRadians(projection.centre().raDegrees());
        double dec = Math.toRadians(projection.centre().decDegrees());
        double[] east = {-Math.sin(ra), Math.cos(ra), 0};
        double[] north = {-Math.sin(dec) * Math.cos(ra),
                -Math.sin(dec) * Math.sin(ra), Math.cos(dec)};

        double a = dot(axis, east);
        double b = dot(axis, north);
        double c = dot(axis, centre);
        double gradient = a * a + b * b;
        if (gradient < 1e-24) {
            // The pole is the page's own centre: the circle is the
            // projection's horizon, ninety degrees away in every
            // direction, and no point of it has an image at all.
            return Optional.empty();
        }
        // The point of the line closest to the plane's origin, and
        // the line's direction - two points, exactly on the line.
        double nearXi = -a * c / gradient;
        double nearEta = -b * c / gradient;
        PixelPoint from = mapping.toPixel(new PlanePoint(nearXi, nearEta));
        PixelPoint to = mapping.toPixel(
                new PlanePoint(nearXi - b, nearEta + a));
        return clipToRectangle(from, to, paper);
    }

    private static double[] unit(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /**
     * The infinite line through two points, clipped to a rectangle -
     * Liang-Barsky, so a page the line only catches at a corner is
     * answered exactly rather than by inspecting vertices.
     */
    private static Optional<Arc> clipToRectangle(PixelPoint a, PixelPoint b,
                                                 Page paper) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double enter = Double.NEGATIVE_INFINITY;
        double leave = Double.POSITIVE_INFINITY;
        double[][] edges = {
            {-dx, a.x() - paper.minX()},
            {dx, paper.maxX() - a.x()},
            {-dy, a.y() - paper.minY()},
            {dy, paper.maxY() - a.y()},
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
