package juranometria.tool.overview;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * What a great circle becomes on a plane (issue #296).
 *
 * <p>Production has one word for this, and it is
 * {@code GreatCirclePage.Arc}: two endpoints, drawn as a
 * {@link Line2D}, because a great circle is straight under the
 * gnomonic projection and under no other. These are the three words
 * that cover all three candidates, and there is no fourth:
 *
 * <ul>
 *   <li>a <strong>straight</strong> run - every gnomonic great circle,
 *       and any circle through the centre under the other two;</li>
 *   <li>a <strong>circular</strong> run - every other stereographic
 *       great circle;</li>
 *   <li>an <strong>elliptical</strong> run - every orthographic great
 *       circle, and nothing else needs it.</li>
 * </ul>
 *
 * <p>There is deliberately no sampled member. A projection states its
 * own curve in closed form (`StudyProjection.greatCircle`), so no
 * caller ever samples one, and because all three forms exist now,
 * adding the orthographic globe in issue #301 widens nothing. A
 * caller draws {@link #shape()} and clips with {@link #clipTo}
 * without asking which of the three it holds.
 *
 * <p>The same three words serve in the projection's plane and on the
 * page, because {@link StudyMapping#onPage} is a similarity: it
 * scales, turns through half a turn, and moves. A line stays a line,
 * a circle stays a circle, and an ellipse keeps its axes.
 */
sealed interface PlaneCurve {

    /** The whole curve, for handing to a Graphics2D. */
    Shape shape();

    /** What this curve is, for the report. */
    String form();

    /** The visible runs of this curve inside a rectangle. */
    List<Run> clipTo(Rectangle2D page);

    /** The same curve after a similarity: scale, half turn, move. */
    PlaneCurve mapped(double scale, double centreX, double centreY);

    /**
     * One visible run: what to draw, and the two ends a label may
     * use - absent when the run closes on itself.
     */
    record Run(Shape shape, Point2D from, Point2D to) {

        boolean closed() {
            return from == null;
        }
    }

    /** A great circle through the centre, and every gnomonic one. */
    record Straight(Line2D.Double line) implements PlaneCurve {

        /**
         * The line {@code a + b xi + c eta = 0}, as two points far
         * enough apart to cross any page.
         */
        static Straight of(double a, double b, double c) {
            double length = Math.hypot(b, c);
            // Nearest point to the origin, then a long way each way
            // along the perpendicular.
            double nearestX = -a * b / (length * length);
            double nearestY = -a * c / (length * length);
            double alongX = -c / length;
            double alongY = b / length;
            // Far enough to cross any page at any scale the atlas
            // uses, and no further: a line carried out to a million
            // plane units becomes a billion page units, where the
            // arithmetic that measures a point's distance from it
            // loses seven digits to cancellation. It showed up as a
            // worst miss of 1.1e-07 where the circles were reading
            // 1e-11.
            double far = 1.0e3;
            return new Straight(new Line2D.Double(
                    nearestX - far * alongX, nearestY - far * alongY,
                    nearestX + far * alongX, nearestY + far * alongY));
        }

        @Override
        public Shape shape() {
            return line;
        }

        @Override
        public String form() {
            return "straight";
        }

        @Override
        public PlaneCurve mapped(double scale, double centreX,
                                 double centreY) {
            return new Straight(new Line2D.Double(
                    centreX - scale * line.x1, centreY - scale * line.y1,
                    centreX - scale * line.x2, centreY - scale * line.y2));
        }

        @Override
        public List<Run> clipTo(Rectangle2D page) {
            // Liang-Barsky: one run or none, which is exactly what
            // production's analytic clipper already promises.
            double dx = line.x2 - line.x1;
            double dy = line.y2 - line.y1;
            double[] window = {0.0, 1.0};
            double[] p = {-dx, dx, -dy, dy};
            double[] q = {line.x1 - page.getMinX(),
                    page.getMaxX() - line.x1,
                    line.y1 - page.getMinY(),
                    page.getMaxY() - line.y1};
            for (int edge = 0; edge < 4; edge++) {
                if (p[edge] == 0.0) {
                    if (q[edge] < 0.0) {
                        return List.of();
                    }
                    continue;
                }
                double at = q[edge] / p[edge];
                if (p[edge] < 0.0) {
                    window[0] = Math.max(window[0], at);
                } else {
                    window[1] = Math.min(window[1], at);
                }
            }
            if (window[0] >= window[1]) {
                return List.of();
            }
            Point2D from = new Point2D.Double(line.x1 + window[0] * dx,
                    line.y1 + window[0] * dy);
            Point2D to = new Point2D.Double(line.x1 + window[1] * dx,
                    line.y1 + window[1] * dy);
            return List.of(new Run(new Line2D.Double(from, to), from, to));
        }
    }

    /** A great circle under stereographic that misses the centre. */
    record Circular(double centreX, double centreY, double radius)
            implements PlaneCurve {

        @Override
        public Shape shape() {
            return new Ellipse2D.Double(centreX - radius, centreY - radius,
                    2.0 * radius, 2.0 * radius);
        }

        @Override
        public String form() {
            return "circular";
        }

        @Override
        public PlaneCurve mapped(double scale, double intoX, double intoY) {
            return new Circular(intoX - scale * centreX,
                    intoY - scale * centreY, scale * radius);
        }

        @Override
        public List<Run> clipTo(Rectangle2D page) {
            List<Double> crossings = new ArrayList<>();
            crossings.addAll(meets(page.getMinY(), page.getMinX(),
                    page.getMaxX(), true));
            crossings.addAll(meets(page.getMaxY(), page.getMinX(),
                    page.getMaxX(), true));
            crossings.addAll(meets(page.getMinX(), page.getMinY(),
                    page.getMaxY(), false));
            crossings.addAll(meets(page.getMaxX(), page.getMinY(),
                    page.getMaxY(), false));
            return CurveRuns.of(crossings, this::at,
                    (from, span) -> new Arc2D.Double(centreX - radius,
                            centreY - radius, 2.0 * radius, 2.0 * radius,
                            Math.toDegrees(-from), Math.toDegrees(-span),
                            Arc2D.OPEN),
                    page, shape());
        }

        private Point2D at(double angle) {
            return new Point2D.Double(centreX + radius * Math.cos(angle),
                    centreY + radius * Math.sin(angle));
        }

        /** Angles where this circle meets one edge of the page. */
        private List<Double> meets(double fixed, double lo, double hi,
                                   boolean horizontal) {
            double offset = horizontal ? fixed - centreY : fixed - centreX;
            if (Math.abs(offset) > radius) {
                return List.of();
            }
            double half = Math.sqrt(radius * radius - offset * offset);
            double along = horizontal ? centreX : centreY;
            List<Double> found = new ArrayList<>(2);
            for (double side : new double[] {-half, half}) {
                double on = along + side;
                if (on < lo || on > hi) {
                    continue;
                }
                double x = horizontal ? on : fixed;
                double y = horizontal ? fixed : on;
                found.add(CurveRuns.angle(y - centreY, x - centreX));
            }
            return found;
        }
    }

    /**
     * A great circle under the orthographic projection.
     *
     * <p>The third word, and the one issue #301 would need. It is
     * here because the alternative was a sampled polyline, which
     * this gate is not allowed to compromise on - and because a
     * vocabulary written whole today is one #301 does not have to
     * widen.
     *
     * <p>Clipping needs no new geometry either. An ellipse is a
     * circle under one affine change of variables, so the page's own
     * edges are carried into the frame where the curve is a unit
     * circle, cut there with the same arithmetic, and the answers
     * carried back.
     */
    record Elliptical(double centreX, double centreY, double radiusAlong,
                      double radiusAcross, double tiltRadians)
            implements PlaneCurve {

        /** Unit circle to this ellipse. */
        private AffineTransform onto() {
            AffineTransform onto = new AffineTransform();
            onto.translate(centreX, centreY);
            onto.rotate(tiltRadians);
            onto.scale(radiusAlong, radiusAcross);
            return onto;
        }

        @Override
        public Shape shape() {
            return onto().createTransformedShape(
                    new Ellipse2D.Double(-1, -1, 2, 2));
        }

        @Override
        public String form() {
            return "elliptical";
        }

        @Override
        public PlaneCurve mapped(double scale, double intoX, double intoY) {
            // Half a turn leaves an axis on the same line, so only
            // the centre and the two radii move.
            return new Elliptical(intoX - scale * centreX,
                    intoY - scale * centreY, scale * radiusAlong,
                    scale * radiusAcross, tiltRadians);
        }

        @Override
        public List<Run> clipTo(Rectangle2D page) {
            AffineTransform onto = onto();
            AffineTransform back;
            try {
                back = onto.createInverse();
            } catch (java.awt.geom.NoninvertibleTransformException flat) {
                return List.of();  // a degenerate ellipse draws nothing
            }
            Point2D[] corners = {
                    new Point2D.Double(page.getMinX(), page.getMinY()),
                    new Point2D.Double(page.getMaxX(), page.getMinY()),
                    new Point2D.Double(page.getMaxX(), page.getMaxY()),
                    new Point2D.Double(page.getMinX(), page.getMaxY())};
            List<Double> crossings = new ArrayList<>();
            for (int at = 0; at < 4; at++) {
                crossings.addAll(meets(back.transform(corners[at], null),
                        back.transform(corners[(at + 1) % 4], null)));
            }
            return CurveRuns.of(crossings, angle -> at(onto, angle),
                    (from, span) -> onto.createTransformedShape(
                            new Arc2D.Double(-1, -1, 2, 2,
                                    Math.toDegrees(-from),
                                    Math.toDegrees(-span), Arc2D.OPEN)),
                    page, shape());
        }

        private static Point2D at(AffineTransform onto, double angle) {
            return onto.transform(new Point2D.Double(Math.cos(angle),
                    Math.sin(angle)), null);
        }

        /** Where the unit circle meets one edge, now an ordinary segment. */
        private static List<Double> meets(Point2D from, Point2D to) {
            double dx = to.getX() - from.getX();
            double dy = to.getY() - from.getY();
            double a = dx * dx + dy * dy;
            if (a == 0.0) {
                return List.of();
            }
            double b = 2.0 * (from.getX() * dx + from.getY() * dy);
            double c = from.getX() * from.getX()
                    + from.getY() * from.getY() - 1.0;
            double discriminant = b * b - 4.0 * a * c;
            if (discriminant < 0.0) {
                return List.of();
            }
            double root = Math.sqrt(discriminant);
            List<Double> found = new ArrayList<>(2);
            for (double sign : new double[] {-1.0, 1.0}) {
                double along = (-b + sign * root) / (2.0 * a);
                if (along < 0.0 || along > 1.0) {
                    continue;
                }
                found.add(CurveRuns.angle(from.getY() + along * dy,
                        from.getX() + along * dx));
            }
            return found;
        }
    }
}
