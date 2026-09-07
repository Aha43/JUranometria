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

    /** The visible runs of this curve inside a region. */
    List<Run> clipTo(PageRegion region);

    /** On a page with no edge to the sky. */
    default List<Run> clipTo(Rectangle2D paper) {
        return clipTo(PageRegion.of(paper));
    }

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

    /**
     * A great circle through the centre, and every gnomonic one.
     *
     * <p>Held as the equation {@code b x + c y + a = 0} with
     * {@code b} and {@code c} a unit vector, so {@code a} is the
     * signed distance from the origin and evaluating the equation at
     * a point gives that point's distance from the line. Two far
     * apart points would be the obvious representation and it is the
     * wrong one: a line carried far enough out to cross any page
     * loses digits to cancellation when anything is measured against
     * it, and how far is far enough depends on a scale the curve
     * does not know.
     */
    record Straight(double a, double b, double c) implements PlaneCurve {

        /** Normalised, so that the coefficients mean a distance. */
        static Straight of(double a, double b, double c) {
            double length = Math.hypot(b, c);
            return new Straight(a / length, b / length, c / length);
        }

        /** How far a point lies off this line. */
        double distanceFrom(double x, double y) {
            return Math.abs(b * x + c * y + a);
        }

        /** A segment of this line long enough to cross a rectangle. */
        private Line2D.Double across(Rectangle2D page) {
            // Built about the page rather than about the origin, so
            // the numbers stay the size of the page.
            double middleX = page.getCenterX();
            double middleY = page.getCenterY();
            double off = b * middleX + c * middleY + a;
            double nearX = middleX - off * b;
            double nearY = middleY - off * c;
            double reach = Math.hypot(page.getWidth(), page.getHeight());
            return new Line2D.Double(nearX + reach * c, nearY - reach * b,
                    nearX - reach * c, nearY + reach * b);
        }

        @Override
        public Shape shape() {
            return across(new Rectangle2D.Double(-1000, -1000, 2000, 2000));
        }

        /**
         * The part of a clipped segment that is inside the limb.
         *
         * <p>A line meets a circle at the roots of one quadratic, so
         * this is exact and needs no sampling. Null when the segment
         * is wholly outside.
         */
        private static double[] withinLimb(Line2D.Double line, double dx,
                                           double dy, double[] window,
                                           PageRegion region) {
            double offsetX = line.x1 - region.limbX();
            double offsetY = line.y1 - region.limbY();
            double a = dx * dx + dy * dy;
            double b = 2.0 * (offsetX * dx + offsetY * dy);
            double c = offsetX * offsetX + offsetY * offsetY
                    - region.limbRadius() * region.limbRadius();
            double discriminant = b * b - 4.0 * a * c;
            if (discriminant <= 0.0) {
                return null;  // the line misses the globe entirely
            }
            double root = Math.sqrt(discriminant);
            double lo = Math.max(window[0], (-b - root) / (2.0 * a));
            double hi = Math.min(window[1], (-b + root) / (2.0 * a));
            return lo >= hi ? null : new double[] {lo, hi};
        }

        @Override
        public String form() {
            return "straight";
        }

        @Override
        public PlaneCurve mapped(double scale, double intoX, double intoY) {
            // x = intoX - scale * xi and y = intoY - scale * eta, so
            // substituting and clearing the scale leaves the same
            // line with its normal turned about.
            return new Straight(b * intoX + c * intoY + a * scale,
                    -b, -c);
        }

        @Override
        public List<Run> clipTo(PageRegion region) {
            Rectangle2D page = region.paper();
            Line2D.Double line = across(page);
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
            if (region.bounded()) {
                // And then to the limb, which cuts the same segment
                // a second time. A straight run is where the ink left
                // the globe: the equator on a committed orthographic
                // page ran clean across the corners of the paper,
                // outside the hemisphere it belongs to.
                double[] cut = withinLimb(line, dx, dy, window, region);
                if (cut == null) {
                    return List.of();
                }
                window = cut;
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
        public List<Run> clipTo(PageRegion region) {
            Rectangle2D page = region.paper();
            List<Double> crossings = new ArrayList<>();
            crossings.addAll(meetsLimb(region));
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
                    region, shape());
        }

        /**
         * Angles where this circle crosses the limb.
         *
         * <p>Two circles meet on their radical line, which is exact
         * arithmetic and no more work than meeting an edge.
         */
        private List<Double> meetsLimb(PageRegion region) {
            if (!region.bounded()) {
                return List.of();
            }
            double apartX = region.limbX() - centreX;
            double apartY = region.limbY() - centreY;
            double apart = Math.hypot(apartX, apartY);
            if (apart == 0.0) {
                return List.of();  // concentric: never crosses
            }
            double along = (apart * apart + radius * radius
                    - region.limbRadius() * region.limbRadius())
                    / (2.0 * apart);
            double square = radius * radius - along * along;
            if (square <= 0.0) {
                return List.of();
            }
            double off = Math.sqrt(square);
            double unitX = apartX / apart;
            double unitY = apartY / apart;
            double footX = centreX + along * unitX;
            double footY = centreY + along * unitY;
            return List.of(
                    CurveRuns.angle(footY - off * unitX - centreY,
                            footX + off * unitY - centreX),
                    CurveRuns.angle(footY + off * unitX - centreY,
                            footX - off * unitY - centreX));
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
        public List<Run> clipTo(PageRegion region) {
            Rectangle2D page = region.paper();
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
            // No limb crossings are collected here, and that is a
            // claim rather than an omission: the only projection with
            // a limb is orthographic, and every orthographic great
            // circle lies inside its own limb, because a point an
            // angle t from the centre lands at sin(t) and the limb is
            // at one. The two touch and never cross. It is measured
            // in OverviewProjectionGateTest; a projection with both a
            // limb and elliptical curves that left it would need the
            // ellipse-meets-circle quartic this does not have.
            return CurveRuns.of(crossings, angle -> at(onto, angle),
                    (from, span) -> onto.createTransformedShape(
                            new Arc2D.Double(-1, -1, 2, 2,
                                    Math.toDegrees(-from),
                                    Math.toDegrees(-span), Arc2D.OPEN)),
                    region, shape());
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
