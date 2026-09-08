package juranometria.tool.labels;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import juranometria.chart.ChartScene;
import juranometria.chart.SkyPosition;
import juranometria.geo.GeoSegment;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;

/**
 * The part of the page a constellation owns (Sprint 31, issue #310).
 *
 * <p>A constellation name may move, but not so far that it is naming
 * a different piece of sky. What it may not leave is its own figure,
 * and "its own figure" has to be a shape rather than a rectangle: the
 * bounding box of Eridanus, which wanders half the sky, contains most
 * of Orion, and a name allowed anywhere in that box could be written
 * over the wrong constellation entirely and still pass.
 *
 * <p>So the region is the <strong>convex hull of the figure's visible
 * ink</strong>, and "visible ink" is the renderer's own definition:
 * the drawn pieces its half-degree subdivision cuts each segment into,
 * counted where the piece crosses the paper. The hull is taken over
 * those pieces' ends, because it has to contain the ink and the ink
 * runs from end to end; the <em>centroid</em> is averaged over their
 * midpoints, because that is what the renderer averages, so the anchor
 * here is the anchor there. Convex rather than the ink
 * itself, because a name has to sit <em>among</em> a figure's lines
 * rather than on one: the ink is a few hairlines and nothing but the
 * hairlines would be inside it.
 *
 * <p>Geometry is used here for speed, and checked against ink: the
 * gate test asserts that every pixel the renderer actually inks for a
 * constellation's figure falls inside that constellation's hull.
 */
public final class FigureRegion {

    /** The renderer's own subdivision step along a segment. */
    private static final double STEP_DEGREES = 0.5;

    private final Map<String, Path2D.Double> hulls;
    private final Map<String, double[]> centroids;

    private FigureRegion(Map<String, Path2D.Double> hulls,
                         Map<String, double[]> centroids) {
        this.hulls = hulls;
        this.centroids = centroids;
    }

    /** The regions this page's constellations own. */
    public static FigureRegion of(Page page) {
        ChartScene scene = page.scene();
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        Projection projection = Projections.forViewport(scene.viewport());
        Map<String, List<double[]>> points = new LinkedHashMap<>();
        Map<String, List<double[]>> hullPoints = new LinkedHashMap<>();
        java.awt.geom.Rectangle2D paper = new java.awt.geom.Rectangle2D.Double(
                0, 0, page.wide(), page.high());
        for (GeoSegment segment : scene.geography().figureSegments()) {
            int steps = Math.max(1, (int) Math.ceil(
                    separation(segment.from(), segment.to())
                            / STEP_DEGREES));
            PixelPoint previous = null;
            for (int at = 0; at <= steps; at++) {
                var plane = projection.project(slerp(segment.from(),
                        segment.to(), (double) at / steps));
                if (plane.isEmpty()) {
                    previous = null;
                    continue;
                }
                PixelPoint pixel = mapping.toPixel(plane.get());
                if (previous != null
                        && new java.awt.geom.Line2D.Double(previous.x(),
                                previous.y(), pixel.x(), pixel.y())
                                .intersects(paper)) {
                    // The piece's ENDS go into the hull, because the
                    // hull has to contain the ink and the ink runs
                    // from end to end. Its midpoint goes into the
                    // centroid, because that is what the renderer
                    // averages. An earlier draft built the hull from
                    // midpoints too, and eight pixels of Aquarius's
                    // figure fell outside the region Aquarius owns.
                    hullPoints.computeIfAbsent(segment.constellationId(),
                            key -> new ArrayList<>())
                            .add(new double[] {previous.x(), previous.y()});
                    hullPoints.get(segment.constellationId())
                            .add(new double[] {pixel.x(), pixel.y()});
                    // The midpoint of a drawn piece, which is what the
                    // renderer accumulates for the name's anchor - and
                    // a piece counts when it CROSSES the paper, not
                    // when its ends are on it. Pyxis on the 90-degree
                    // Orion page is drawn with no sampled point inside
                    // the page at all, and an inside-only rule left it
                    // unnamed on a page the atlas names it on.
                    points.computeIfAbsent(segment.constellationId(),
                            key -> new ArrayList<>())
                            .add(new double[] {
                                    (previous.x() + pixel.x()) / 2.0,
                                    (previous.y() + pixel.y()) / 2.0});
                }
                previous = pixel;
            }
        }
        Map<String, Path2D.Double> hulls = new LinkedHashMap<>();
        Map<String, double[]> centroids = new LinkedHashMap<>();
        for (Map.Entry<String, List<double[]>> entry : points.entrySet()) {
            Path2D.Double hull = hullOf(hullPoints.getOrDefault(
                    entry.getKey(), entry.getValue()));
            if (hull != null) {
                hulls.put(entry.getKey(), hull);
            }
            double x = 0.0;
            double y = 0.0;
            for (double[] point : entry.getValue()) {
                x += point[0];
                y += point[1];
            }
            centroids.put(entry.getKey(), new double[] {
                    x / entry.getValue().size(),
                    y / entry.getValue().size()});
        }
        return new FigureRegion(hulls, centroids);
    }

    /**
     * Whether a name written in this box would be written across the
     * figure it names.
     *
     * <p>Overlap rather than "its centre is inside", because a figure
     * can be smaller than its own name: Crater on a 90-degree page
     * leaves six pixels by five of visible ink and CRATER is fifty
     * pixels wide, so no placement of it could put its centre inside
     * its own figure and the strict rule would have refused every
     * candidate and taught nothing. What the rule is for is stopping a
     * name drifting onto somebody else's constellation, and overlap
     * says that.
     */
    public boolean ownsBox(String constellationId,
                           java.awt.geom.Rectangle2D box) {
        Path2D.Double hull = hulls.get(constellationId);
        return hull != null && hull.intersects(box);
    }

    /** Whether this constellation owns this point of the page. */
    public boolean owns(String constellationId, double x, double y) {
        Path2D.Double hull = hulls.get(constellationId);
        // A constellation with no visible figure owns nothing, and its
        // name is not drawn either - the renderer anchors names on
        // visible ink and so does this.
        return hull != null && hull.contains(x, y);
    }

    /**
     * Where a constellation's name is anchored: the centroid of its
     * visible figure ink, which is the renderer's own rule.
     *
     * <p>Over the sampled points that land on the paper, not over the
     * segments' endpoints. A constellation whose figure crosses the
     * page while both ends of every segment lie off it - Pyxis on the
     * 90-degree Orion page - has visible ink and no visible endpoint,
     * and an endpoint-based centroid does not name it at all. The
     * released page does.
     */
    public double[] centroidOf(String constellationId) {
        return centroids.get(constellationId);
    }

    /** Whether this page knows a region for this constellation. */
    public boolean knows(String constellationId) {
        return hulls.containsKey(constellationId);
    }

    /** The region itself, for a study to draw or a test to check. */
    public java.awt.Shape regionOf(String constellationId) {
        return hulls.get(constellationId);
    }

    public java.util.Set<String> constellations() {
        return java.util.Set.copyOf(hulls.keySet());
    }

    /**
     * The convex hull, by monotone chain - sorted input, integer-free
     * cross products, no tolerance and no iteration, so the same
     * points give the same hull everywhere.
     */
    private static Path2D.Double hullOf(List<double[]> points) {
        if (points.size() < 3) {
            return null;
        }
        List<double[]> sorted = new ArrayList<>(points);
        sorted.sort((one, other) -> one[0] != other[0]
                ? Double.compare(one[0], other[0])
                : Double.compare(one[1], other[1]));
        List<double[]> lower = new ArrayList<>();
        for (double[] point : sorted) {
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2),
                    lower.get(lower.size() - 1), point) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(point);
        }
        List<double[]> upper = new ArrayList<>();
        for (int at = sorted.size() - 1; at >= 0; at--) {
            double[] point = sorted.get(at);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2),
                    upper.get(upper.size() - 1), point) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(point);
        }
        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        if (lower.size() < 3) {
            return null;
        }
        Path2D.Double hull = new Path2D.Double();
        hull.moveTo(lower.get(0)[0], lower.get(0)[1]);
        for (int at = 1; at < lower.size(); at++) {
            hull.lineTo(lower.get(at)[0], lower.get(at)[1]);
        }
        hull.closePath();
        return hull;
    }

    private static double cross(double[] origin, double[] one,
                                double[] other) {
        return (one[0] - origin[0]) * (other[1] - origin[1])
                - (one[1] - origin[1]) * (other[0] - origin[0]);
    }

    private static double separation(SkyPosition from, SkyPosition to) {
        double[] one = unit(from);
        double[] other = unit(to);
        return Math.toDegrees(Math.acos(Math.clamp(
                one[0] * other[0] + one[1] * other[1] + one[2] * other[2],
                -1.0, 1.0)));
    }

    private static SkyPosition slerp(SkyPosition from, SkyPosition to,
                                     double t) {
        double[] one = unit(from);
        double[] other = unit(to);
        double omega = Math.acos(Math.clamp(
                one[0] * other[0] + one[1] * other[1] + one[2] * other[2],
                -1.0, 1.0));
        double first;
        double second;
        if (omega < 1e-9) {
            first = 1.0 - t;
            second = t;
        } else {
            first = Math.sin((1.0 - t) * omega) / Math.sin(omega);
            second = Math.sin(t * omega) / Math.sin(omega);
        }
        double x = first * one[0] + second * other[0];
        double y = first * one[1] + second * other[1];
        double z = first * one[2] + second * other[2];
        double length = Math.sqrt(x * x + y * y + z * z);
        return new SkyPosition(
                Math.toDegrees(Math.atan2(y / length, x / length)) < 0
                        ? Math.toDegrees(Math.atan2(y / length, x / length))
                                + 360.0
                        : Math.toDegrees(Math.atan2(y / length, x / length)),
                Math.toDegrees(Math.asin(Math.clamp(z / length, -1.0, 1.0))));
    }

    private static double[] unit(SkyPosition at) {
        double ra = Math.toRadians(at.raDegrees());
        double dec = Math.toRadians(at.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }
}
