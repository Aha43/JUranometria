package juranometria.tool.overview;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * A great circle on the page, and the evidence that it is right
 * (issue #296).
 *
 * <p>The curve comes from the projection, which states it in closed
 * form. Nothing here decides what form a curve takes and nothing here
 * samples the sky to find out - that was the gate's first proposal,
 * and a review was right that it amounted to sampling, a type check
 * and a promise to widen the vocabulary later.
 *
 * <p>What is here is the <strong>check</strong>: the stated curve is
 * measured against a few hundred genuinely projected points, and
 * measured against the curve that would actually be <em>drawn</em>
 * rather than against any equation. A projection whose arithmetic
 * had drifted from its name, or a form that came out rotated a
 * quarter turn, fails this rather than reaching a page.
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

    /** The curve the projection stated, and how well it holds. */
    record Built(PlaneCurve curve, double residual, int samples) {
    }

    static Optional<Built> greatCircle(StudyMapping mapping,
                                       SkyPosition pole, int samples) {
        // The projection is asked what the curve is. Nothing here
        // fits one, and nothing here samples the sky to find out -
        // the sampling below is the check, not the method.
        Optional<PlaneConic> stated =
                mapping.projection().greatCircle(pole);
        if (stated.isEmpty()) {
            return Optional.empty();
        }
        PlaneCurve onPage = mapping.onPage(stated.get());

        List<Point2D> page = onOrNearThePage(mapping, pole, samples);
        if (page.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Built(onPage, missOf(onPage, page),
                page.size()));
    }

    /**
     * The same curve found the other way: determined from the fewest
     * points that fix a form - two for a line, three for a circle,
     * five for an ellipse - and measured against every projected
     * point there is.
     *
     * <p>This is not how a page is drawn. It is how the closed forms
     * are checked, and it is deliberately a different method of
     * arriving at the answer: a projection whose stated curve and
     * whose projected points disagreed would be caught by one
     * contradicting the other, where a single derivation used for
     * both could be wrong in the same way twice.
     */
    static Optional<PlaneCurve> fitted(StudyMapping mapping,
                                       SkyPosition pole, int samples) {
        // The same points the stated curve is measured against, and
        // for the same reason: a stereographic circle running out
        // towards the antipode has points two thousand million page
        // units away, and a form fitted to include them is a form
        // fitted to a place no drawing happens.
        List<Point2D> page = onOrNearThePage(mapping, pole, samples);
        if (page.size() < 2) {
            return Optional.empty();
        }
        Point2D[] spread = spread(page);

        PlaneCurve straight = through(spread[0], spread[1]);
        if (missOf(straight, page) < EXACT) {
            return Optional.of(straight);
        }
        if (page.size() >= 3) {
            double[] circle = circleThrough(spread[0], spread[1], spread[2]);
            if (circle != null) {
                PlaneCurve curve = new PlaneCurve.Circular(circle[0],
                        circle[1], circle[2]);
                if (missOf(curve, page) < EXACT) {
                    return Optional.of(curve);
                }
            }
        }
        if (page.size() >= 5) {
            PlaneCurve ellipse = ellipseThrough(page);
            if (ellipse != null && missOf(ellipse, page) < EXACT) {
                return Optional.of(ellipse);
            }
        }
        return Optional.empty();
    }

    /**
     * Projected points on the page, or within a page of it.
     *
     * <p>Points far off the paper are excluded, and the reason is
     * not tidiness. A stereographic great circle whose pole is
     * nearly square to the centre runs out towards the antipode, and
     * one of its sampled points lands two thousand million page
     * units away: measuring a curve against that point asks whether
     * the drawing is right in a place no drawing happens, and it
     * dominated every number in the degenerate band until it was
     * noticed. What the atlas owes is a curve that is right where it
     * is drawn.
     */
    private static List<Point2D> onOrNearThePage(StudyMapping mapping,
                                                 SkyPosition pole,
                                                 int samples) {
        double wide = 2.0 * mapping.pageCentreX();
        double high = 2.0 * mapping.pageCentreY();
        java.awt.geom.Rectangle2D near = new java.awt.geom.Rectangle2D
                .Double(-wide / 2.0, -high / 2.0, 2.0 * wide, 2.0 * high);
        List<Point2D> page = new ArrayList<>(samples);
        for (SkyPosition position : CurveForm.around(pole, samples)) {
            mapping.pageOf(position)
                    .filter(near::contains)
                    .ifPresent(page::add);
        }
        return page;
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
    static double missOf(PlaneCurve curve, List<Point2D> points) {
        double worst = 0.0;
        for (Point2D point : points) {
            worst = Math.max(worst, missAt(curve, point));
        }
        return worst;
    }

    private static double missAt(PlaneCurve curve, Point2D point) {
        return switch (curve) {
            case PlaneCurve.Straight line ->
                    line.distanceFrom(point.getX(), point.getY());
            case PlaneCurve.Circular circle -> Math.abs(
                    Math.hypot(point.getX() - circle.centreX(),
                            point.getY() - circle.centreY())
                            - circle.radius());
            case PlaneCurve.Elliptical ellipse -> {
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

    /** The line through two points, as its own equation. */
    private static PlaneCurve through(Point2D a, Point2D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        return PlaneCurve.Straight.of(dy * a.getX() - dx * a.getY(),
                -dy, dx);
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
    private static PlaneCurve ellipseThrough(List<Point2D> page) {
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
        return new PlaneCurve.Elliptical(centreX * spread + meanX,
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
