package juranometria.tool.overview;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Builds a great circle's page curve, and proves the form it chose
 * (issue #296).
 *
 * <p>The choice is not made by asking which projection is in use. It
 * is made by <strong>determining</strong> a candidate form from the
 * fewest points that fix it - two for a line, three for a circle,
 * five for an ellipse - and then <strong>measuring</strong> that form
 * against a great many more. A projection that quietly stopped being
 * what it says it is would fail the measurement rather than draw a
 * wrong curve.
 *
 * <p>The measurement is made against the curve that would actually be
 * <em>drawn</em>, not against the equation it was fitted from. A form
 * whose coefficients were right and whose shape came out rotated a
 * quarter turn would satisfy the fit and fail this.
 */
final class PageCurves {

    /**
     * How near a determined form must hold, in page units.
     *
     * <p>A thousandth of a page unit is a thousandth of the thinnest
     * line the atlas draws. The measured misses run from 1e-13 to
     * 1e-8, so this is not a threshold anything sits near: it
     * separates "exact" from "not this form at all".
     */
    static final double EXACT = 1.0e-3;

    private PageCurves() {
    }

    /** What was built, and the evidence for it. */
    record Built(PageCurve curve, double residual, int points,
                 int samples) {
    }

    static Optional<Built> greatCircle(StudyMapping mapping,
                                       SkyPosition pole, int samples) {
        List<Point2D> page = new ArrayList<>(samples);
        for (SkyPosition position : CurveForm.around(pole, samples)) {
            mapping.pageOf(position).ifPresent(page::add);
        }
        if (page.size() < 2) {
            return Optional.empty();
        }
        Point2D[] spread = spread(page);

        // Each candidate is determined from the fewest points that
        // fix it, and then measured against every point there is -
        // and measured against the curve that would actually be
        // drawn, not against the equation it was fitted from. A form
        // whose arithmetic was right and whose shape came out
        // rotated a quarter turn would pass the second check only by
        // being correct.
        PageCurve straight = through(spread[0], spread[1]);
        double miss = missOf(straight, page);
        if (miss < EXACT) {
            return Optional.of(new Built(straight, miss, 2, page.size()));
        }
        if (page.size() >= 3) {
            double[] circle = circleThrough(spread[0], spread[1], spread[2]);
            if (circle != null) {
                PageCurve curve = new PageCurve.Circular(circle[0],
                        circle[1], circle[2]);
                miss = missOf(curve, page);
                if (miss < EXACT) {
                    return Optional.of(
                            new Built(curve, miss, 3, page.size()));
                }
            }
        }
        if (page.size() >= 5) {
            PageCurve ellipse = ellipseThrough(page);
            if (ellipse != null) {
                miss = missOf(ellipse, page);
                if (miss < EXACT) {
                    return Optional.of(
                            new Built(ellipse, miss, 5, page.size()));
                }
            }
        }
        return Optional.of(new Built(
                new PageCurve.Sampled(page, 360.0 / samples),
                Double.NaN, page.size(), page.size()));
    }

    /**
     * The worst distance from a projected point to the curve that
     * would be drawn through it.
     *
     * <p>Taken along the curve's own parameter rather than
     * perpendicular to it, which can only overstate the miss - the
     * safe direction for a number that decides whether a form is
     * exact.
     */
    static double missOf(PageCurve curve, List<Point2D> points) {
        double worst = 0.0;
        for (Point2D point : points) {
            worst = Math.max(worst, missAt(curve, point));
        }
        return worst;
    }

    private static double missAt(PageCurve curve, Point2D point) {
        return switch (curve) {
            case PageCurve.Straight line -> {
                double dx = line.line().x2 - line.line().x1;
                double dy = line.line().y2 - line.line().y1;
                double length = Math.hypot(dx, dy);
                yield length == 0.0 ? Double.MAX_VALUE
                        : Math.abs(dx * (line.line().y1 - point.getY())
                                - dy * (line.line().x1 - point.getX()))
                                / length;
            }
            case PageCurve.Circular circle -> Math.abs(
                    Math.hypot(point.getX() - circle.centreX(),
                            point.getY() - circle.centreY())
                            - circle.radius());
            case PageCurve.Elliptical ellipse -> {
                java.awt.geom.AffineTransform onto =
                        new java.awt.geom.AffineTransform();
                onto.translate(ellipse.centreX(), ellipse.centreY());
                onto.rotate(ellipse.tiltRadians());
                onto.scale(ellipse.radiusAlong(), ellipse.radiusAcross());
                try {
                    Point2D unit = onto.createInverse()
                            .transform(point, null);
                    double length = Math.hypot(unit.getX(), unit.getY());
                    if (length == 0.0) {
                        yield Double.MAX_VALUE;
                    }
                    Point2D on = onto.transform(new Point2D.Double(
                            unit.getX() / length, unit.getY() / length),
                            null);
                    yield point.distance(on);
                } catch (java.awt.geom.NoninvertibleTransformException flat) {
                    yield Double.MAX_VALUE;
                }
            }
            case PageCurve.Sampled sampled -> 0.0;
        };
    }

    /**
     * Three points far apart on the projected curve.
     *
     * <p>Neighbouring points would determine a line or a circle from
     * a short arc and let a wrong form pass on rounding alone, which
     * is the way this kind of fit is usually got wrong.
     */
    private static Point2D[] spread(List<Point2D> page) {
        return new Point2D[] {page.get(0), page.get(page.size() / 3),
                page.size() >= 3 ? page.get(2 * page.size() / 3)
                        : page.get(page.size() - 1)};
    }

    /** A line long enough to cross any page, through two of its points. */
    private static PageCurve through(Point2D a, Point2D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double length = Math.hypot(dx, dy);
        double far = 1.0e6 / length;
        return new PageCurve.Straight(new Line2D.Double(
                a.getX() - far * dx, a.getY() - far * dy,
                a.getX() + far * dx, a.getY() + far * dy));
    }

    /** Centre and radius of the circle through three points. */
    private static double[] circleThrough(Point2D a, Point2D b, Point2D c) {
        double d = 2.0 * (a.getX() * (b.getY() - c.getY())
                + b.getX() * (c.getY() - a.getY())
                + c.getX() * (a.getY() - b.getY()));
        if (Math.abs(d) < 1.0e-12) {
            return null;  // collinear: this is a line, not a circle
        }
        double aa = a.getX() * a.getX() + a.getY() * a.getY();
        double bb = b.getX() * b.getX() + b.getY() * b.getY();
        double cc = c.getX() * c.getX() + c.getY() * c.getY();
        double x = (aa * (b.getY() - c.getY()) + bb * (c.getY() - a.getY())
                + cc * (a.getY() - b.getY())) / d;
        double y = (aa * (c.getX() - b.getX()) + bb * (a.getX() - c.getX())
                + cc * (b.getX() - a.getX())) / d;
        return new double[] {x, y,
                Math.hypot(a.getX() - x, a.getY() - y)};
    }

    /**
     * The ellipse through five spread points.
     *
     * <p>Five points determine a conic; which conic it is comes out
     * of the coefficients. Only an ellipse is accepted here - a
     * parabola or a hyperbola is not a great circle on a page, it is
     * a sign that the points were not on one curve.
     */
    private static PageCurve ellipseThrough(List<Point2D> page) {
        // Fitted about the points' own centre, at their own scale.
        // Page coordinates run to several hundred, so the squared
        // terms of a conic reach a million against a constant term
        // of one, and the answer that comes back is arithmetic noise
        // - every orthographic curve in an earlier run of this study
        // fell through to sampling for no better reason than that.
        // Translating and scaling an ellipse only translates and
        // scales its centre and axes, so the fit is exact and the
        // way back is arithmetic.
        double meanX = 0.0;
        double meanY = 0.0;
        for (Point2D at : page) {
            meanX += at.getX();
            meanY += at.getY();
        }
        meanX /= page.size();
        meanY /= page.size();
        double spread = 0.0;
        for (Point2D at : page) {
            spread += Math.hypot(at.getX() - meanX, at.getY() - meanY);
        }
        spread /= page.size();
        if (!(spread > 0.0) || !Double.isFinite(spread)) {
            return null;
        }

        double[][] rows = new double[5][6];
        for (int i = 0; i < 5; i++) {
            Point2D at = page.get(i * (page.size() - 1) / 4);
            double x = (at.getX() - meanX) / spread;
            double y = (at.getY() - meanY) / spread;
            rows[i] = new double[] {x * x, x * y, y * y, x, y, 1.0};
        }
        double[] conic = nullVector(rows);
        if (conic == null) {
            return null;
        }
        double a = conic[0];
        double b = conic[1];
        double c = conic[2];
        double d = conic[3];
        double e = conic[4];
        double f = conic[5];
        if (!(b * b - 4.0 * a * c < 0.0)) {
            return null;  // a parabola or a hyperbola is not this
        }
        // The centre is where both partial derivatives vanish, so it
        // is divided by the determinant 4AC-B^2 - and not by the
        // discriminant B^2-4AC, which is the same number negated and
        // put the centre on the wrong side of every curve, whereupon
        // both axes came out imaginary and every orthographic page
        // fell back to sampling.
        double determinant = 4.0 * a * c - b * b;
        double centreX = (b * e - 2.0 * c * d) / determinant;
        double centreY = (b * d - 2.0 * a * e) / determinant;
        double atCentre = a * centreX * centreX + b * centreX * centreY
                + c * centreY * centreY + d * centreX + e * centreY + f;
        double tilt = 0.5 * Math.atan2(b, a - c);
        double cos = Math.cos(tilt);
        double sin = Math.sin(tilt);
        double along = a * cos * cos + b * cos * sin + c * sin * sin;
        double across = a * sin * sin - b * sin * cos + c * cos * cos;
        if (-atCentre / along <= 0.0 || -atCentre / across <= 0.0) {
            return null;
        }
        return new PageCurve.Elliptical(centreX * spread + meanX,
                centreY * spread + meanY,
                spread * Math.sqrt(-atCentre / along),
                spread * Math.sqrt(-atCentre / across), tilt);
    }

    /** A non-zero solution of five equations in six unknowns. */
    private static double[] nullVector(double[][] rows) {
        int columns = 6;
        int[] pivots = new int[5];
        java.util.Arrays.fill(pivots, -1);
        boolean[] used = new boolean[columns];
        for (int row = 0; row < 5; row++) {
            int pivot = -1;
            double best = 1.0e-12;
            for (int column = 0; column < columns; column++) {
                if (!used[column] && Math.abs(rows[row][column]) > best) {
                    best = Math.abs(rows[row][column]);
                    pivot = column;
                }
            }
            if (pivot < 0) {
                continue;
            }
            used[pivot] = true;
            pivots[row] = pivot;
            double scale = rows[row][pivot];
            for (int column = 0; column < columns; column++) {
                rows[row][column] /= scale;
            }
            for (int other = 0; other < 5; other++) {
                if (other == row || rows[other][pivot] == 0.0) {
                    continue;
                }
                double factor = rows[other][pivot];
                for (int column = 0; column < columns; column++) {
                    rows[other][column] -= factor * rows[row][column];
                }
            }
        }
        int free = -1;
        for (int column = 0; column < columns; column++) {
            if (!used[column]) {
                free = column;
                break;
            }
        }
        if (free < 0) {
            return null;
        }
        double[] answer = new double[columns];
        answer[free] = 1.0;
        for (int row = 0; row < 5; row++) {
            if (pivots[row] >= 0) {
                answer[pivots[row]] = -rows[row][free];
            }
        }
        return answer;
    }
}
