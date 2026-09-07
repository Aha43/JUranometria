package juranometria.tool.overview;

import java.util.ArrayList;
import java.util.List;

import juranometria.chart.SkyPosition;
import juranometria.project.PlanePoint;

/**
 * What a great circle looks like on the page (Sprint 30, issue #296).
 *
 * <p>The atlas draws three reference circles - the meridian, the
 * mathematical horizon and the ecliptic - and it draws them
 * <em>exactly</em>: gnomonic maps a great circle to a straight line,
 * so `GreatCirclePage` clips a line and never samples one. Sprint 25
 * rejected the polyline alternative by measurement, and Sprint 29's
 * gate kept gnomonic for the printable sheet on the same grounds.
 *
 * <p>So the question this gate has to answer is not "does another
 * projection look nicer" but "does another projection still let a
 * great circle be drawn exactly, and as what". This measures it the
 * only way that settles it: assume the form, fit it to the fewest
 * points that determine it, and then check every other point on the
 * circle against that fit. A form that is exact leaves a residual at
 * machine precision. A form that is merely close does not.
 */
final class CurveForm {

    private CurveForm() {
    }

    /** What was assumed, and how far the rest of the curve missed it. */
    record Fit(String form, double worstResidual, int points,
               boolean degenerate) {
    }

    /** Positions evenly around the great circle with the given pole. */
    static List<SkyPosition> around(SkyPosition pole, int samples) {
        double[] p = unit(pole);
        // Two directions spanning the circle's own plane.
        double[] any = Math.abs(p[2]) < 0.9
                ? new double[] {0, 0, 1} : new double[] {1, 0, 0};
        double[] u = normalise(cross(any, p));
        double[] v = cross(p, u);

        List<SkyPosition> positions = new ArrayList<>(samples);
        for (int i = 0; i < samples; i++) {
            double t = 2.0 * Math.PI * i / samples;
            positions.add(sky(new double[] {
                    Math.cos(t) * u[0] + Math.sin(t) * v[0],
                    Math.cos(t) * u[1] + Math.sin(t) * v[1],
                    Math.cos(t) * u[2] + Math.sin(t) * v[2]}));
        }
        return positions;
    }

    /** Points along the great circle with the given pole, projected. */
    static List<PlanePoint> project(StudyProjection projection,
                                    SkyPosition pole, int samples) {
        List<PlanePoint> points = new ArrayList<>();
        for (SkyPosition position : around(pole, samples)) {
            projection.project(position).ifPresent(points::add);
        }
        return points;
    }

    /**
     * A straight line through the first and last points, and the
     * worst distance from it of everything between.
     */
    static Fit asLine(List<PlanePoint> points) {
        if (points.size() < 3) {
            return new Fit("line", Double.NaN, points.size(), true);
        }
        PlanePoint a = points.get(0);
        PlanePoint b = points.get(points.size() - 1);
        double dx = b.xiEast() - a.xiEast();
        double dy = b.etaNorth() - a.etaNorth();
        double length = Math.hypot(dx, dy);
        if (length < 1e-12) {
            return new Fit("line", Double.NaN, points.size(), true);
        }
        double worst = 0.0;
        for (PlanePoint point : points) {
            double area = Math.abs(dx * (a.etaNorth() - point.etaNorth())
                    - (a.xiEast() - point.xiEast()) * dy);
            worst = Math.max(worst, area / length);
        }
        return new Fit("line", worst, points.size(), false);
    }

    /**
     * The circle through three well-separated points, and the worst
     * radial miss of everything else.
     */
    static Fit asCircle(List<PlanePoint> points) {
        if (points.size() < 6) {
            return new Fit("circle", Double.NaN, points.size(), true);
        }
        PlanePoint a = points.get(0);
        PlanePoint b = points.get(points.size() / 3);
        PlanePoint c = points.get(2 * points.size() / 3);

        double ax = a.xiEast();
        double ay = a.etaNorth();
        double bx = b.xiEast();
        double by = b.etaNorth();
        double cx = c.xiEast();
        double cy = c.etaNorth();
        double d = 2.0 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by));
        if (Math.abs(d) < 1e-12) {
            // Three points in a line: the circle is a line, which is
            // the degenerate case a vocabulary has to allow for.
            return new Fit("circle", Double.NaN, points.size(), true);
        }
        double a2 = ax * ax + ay * ay;
        double b2 = bx * bx + by * by;
        double c2 = cx * cx + cy * cy;
        double centreX = (a2 * (by - cy) + b2 * (cy - ay) + c2 * (ay - by)) / d;
        double centreY = (a2 * (cx - bx) + b2 * (ax - cx) + c2 * (bx - ax)) / d;
        double radius = Math.hypot(ax - centreX, ay - centreY);

        double worst = 0.0;
        for (PlanePoint point : points) {
            worst = Math.max(worst, Math.abs(radius - Math.hypot(
                    point.xiEast() - centreX, point.etaNorth() - centreY)));
        }
        return new Fit("circle", worst, points.size(), false);
    }

    /**
     * The conic through five points, and the worst algebraic miss of
     * the rest - reported as a distance by dividing by the gradient,
     * so the number is in plane units rather than in whatever the
     * conic's own scale happens to be.
     */
    static Fit asConic(List<PlanePoint> points) {
        if (points.size() < 12) {
            return new Fit("conic", Double.NaN, points.size(), true);
        }
        int step = points.size() / 5;
        double[][] rows = new double[5][6];
        for (int i = 0; i < 5; i++) {
            PlanePoint p = points.get(i * step);
            double x = p.xiEast();
            double y = p.etaNorth();
            rows[i] = new double[] {x * x, x * y, y * y, x, y, 1.0};
        }
        double[] conic = nullVector(rows);
        if (conic == null) {
            return new Fit("conic", Double.NaN, points.size(), true);
        }

        double worst = 0.0;
        for (PlanePoint point : points) {
            double x = point.xiEast();
            double y = point.etaNorth();
            double value = conic[0] * x * x + conic[1] * x * y
                    + conic[2] * y * y + conic[3] * x + conic[4] * y
                    + conic[5];
            double gradient = Math.hypot(
                    2 * conic[0] * x + conic[1] * y + conic[3],
                    conic[1] * x + 2 * conic[2] * y + conic[4]);
            if (gradient > 1e-12) {
                worst = Math.max(worst, Math.abs(value) / gradient);
            }
        }
        boolean ellipse = conic[1] * conic[1] - 4 * conic[0] * conic[2] < 0;
        return new Fit(ellipse ? "ellipse" : "conic", worst, points.size(),
                false);
    }

    /** A vector spanning the null space of five rows of six. */
    private static double[] nullVector(double[][] rows) {
        double[][] m = new double[5][6];
        for (int i = 0; i < 5; i++) {
            m[i] = rows[i].clone();
        }
        int[] pivotOf = new int[5];
        java.util.Arrays.fill(pivotOf, -1);
        int row = 0;
        boolean[] isPivot = new boolean[6];
        for (int col = 0; col < 6 && row < 5; col++) {
            int best = -1;
            double biggest = 1e-12;
            for (int r = row; r < 5; r++) {
                if (Math.abs(m[r][col]) > biggest) {
                    biggest = Math.abs(m[r][col]);
                    best = r;
                }
            }
            if (best < 0) {
                continue;
            }
            double[] swap = m[row];
            m[row] = m[best];
            m[best] = swap;
            for (int r = 0; r < 5; r++) {
                if (r == row) {
                    continue;
                }
                double factor = m[r][col] / m[row][col];
                for (int k = col; k < 6; k++) {
                    m[r][k] -= factor * m[row][k];
                }
            }
            pivotOf[row] = col;
            isPivot[col] = true;
            row++;
        }
        int free = -1;
        for (int col = 5; col >= 0; col--) {
            if (!isPivot[col]) {
                free = col;
                break;
            }
        }
        if (free < 0) {
            return null;
        }
        double[] answer = new double[6];
        answer[free] = 1.0;
        for (int r = 4; r >= 0; r--) {
            if (pivotOf[r] < 0) {
                continue;
            }
            answer[pivotOf[r]] = -m[r][free] / m[r][pivotOf[r]];
        }
        return answer;
    }

    private static double[] unit(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static SkyPosition sky(double[] unit) {
        double ra = Math.toDegrees(Math.atan2(unit[1], unit[0]));
        return new SkyPosition((ra % 360.0 + 360.0) % 360.0,
                Math.toDegrees(Math.asin(Math.clamp(unit[2], -1.0, 1.0))));
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalise(double[] v) {
        double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / length, v[1] / length, v[2] / length};
    }
}
