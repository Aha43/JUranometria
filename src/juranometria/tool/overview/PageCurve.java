package juranometria.tool.overview;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The proposed vocabulary for a great circle on a page (issue #296).
 *
 * <p>Production has one word for this, and it is
 * {@code GreatCirclePage.Arc}: two endpoints, drawn as a
 * {@link Line2D}, because a great circle is straight under the
 * gnomonic projection and under no other. {@link CurveForm}'s
 * measurements say what the smallest honest replacement is:
 *
 * <ul>
 *   <li>a <strong>straight</strong> run - gnomonic always, and either
 *       of the others when the circle passes through the page
 *       centre;</li>
 *   <li>a <strong>circular</strong> run - stereographic otherwise,
 *       exactly, to about 1e-14 of a page unit;</li>
 *   <li>an <strong>elliptical</strong> run - every orthographic great
 *       circle, exactly, and nothing else needs it;</li>
 *   <li>a <strong>sampled</strong> run - a last resort that no page
 *       measured in this study fell back to, kept so that a
 *       projection nobody has thought of yet is drawn coarsely
 *       rather than wrongly.</li>
 * </ul>
 *
 * <p>Two things here are not in production's vocabulary at all, and
 * both are found by drawing rather than by reasoning:
 *
 * <ol>
 *   <li>a curve can cross one page in <strong>more than one
 *       run</strong> - a circle meets a rectangle in up to four
 *       points - where production's {@code Optional<Arc>} can only
 *       say "once" or "not at all";</li>
 *   <li>a curve can be <strong>closed</strong> on the page, wholly
 *       inside the paper with no ends at all, which is what the
 *       celestial equator does on a pole-centred overview. The label
 *       rule "where the line leaves the paper" has no anchor for
 *       it.</li>
 * </ol>
 */
sealed interface PageCurve {

    /** The whole projected curve, for handing to a Graphics2D. */
    Shape shape();

    /** What this run is, for the report. */
    String form();

    /** The visible runs of this curve on a page, in drawing order. */
    List<Run> clipTo(Rectangle2D page);

    /**
     * One visible run: what to draw, and the two ends a label may
     * use - absent when the run closes on itself.
     */
    record Run(Shape shape, Point2D from, Point2D to) {

        boolean closed() {
            return from == null;
        }
    }

    /** A great circle through the page centre, and every gnomonic one. */
    record Straight(Line2D.Double line) implements PageCurve {

        @Override
        public Shape shape() {
            return line;
        }

        @Override
        public String form() {
            return "straight";
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
                double t = q[edge] / p[edge];
                if (p[edge] < 0.0) {
                    window[0] = Math.max(window[0], t);
                } else {
                    window[1] = Math.min(window[1], t);
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

    /** A great circle under stereographic that misses the page centre. */
    record Circular(double centreX, double centreY, double radius)
            implements PageCurve {

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
                    (from, span) -> new java.awt.geom.Arc2D.Double(
                            centreX - radius, centreY - radius,
                            2.0 * radius, 2.0 * radius,
                            Math.toDegrees(-from), Math.toDegrees(-span),
                            java.awt.geom.Arc2D.OPEN),
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
     * vocabulary that can be extended by writing one more record is
     * a different thing from one that has to be redesigned.
     *
     * <p>Clipping needs no new geometry either. An ellipse is a
     * circle under one affine change of variables, so the page's own
     * edges are carried into the frame where the curve is a unit
     * circle, cut there, and the answers carried back.
     */
    record Elliptical(double centreX, double centreY, double radiusAlong,
                      double radiusAcross, double tiltRadians)
            implements PageCurve {

        /** Unit circle to page. */
        private AffineTransform toPage() {
            AffineTransform onto = new AffineTransform();
            onto.translate(centreX, centreY);
            onto.rotate(tiltRadians);
            onto.scale(radiusAlong, radiusAcross);
            return onto;
        }

        @Override
        public Shape shape() {
            return toPage().createTransformedShape(
                    new Ellipse2D.Double(-1, -1, 2, 2));
        }

        @Override
        public String form() {
            return "elliptical";
        }

        @Override
        public List<Run> clipTo(Rectangle2D page) {
            AffineTransform onto = toPage();
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
                            new java.awt.geom.Arc2D.Double(-1, -1, 2, 2,
                                    Math.toDegrees(-from),
                                    Math.toDegrees(-span),
                                    java.awt.geom.Arc2D.OPEN)),
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

    /** Anything the two exact forms cannot hold, at a stated step. */
    record Sampled(List<Point2D> points, double stepDegrees)
            implements PageCurve {

        @Override
        public Shape shape() {
            Path2D.Double path = new Path2D.Double();
            boolean started = false;
            for (Point2D point : points) {
                if (!started) {
                    path.moveTo(point.getX(), point.getY());
                    started = true;
                } else {
                    path.lineTo(point.getX(), point.getY());
                }
            }
            return path;
        }

        @Override
        public String form() {
            return "sampled";
        }

        @Override
        public List<Run> clipTo(Rectangle2D page) {
            List<Run> runs = new ArrayList<>();
            Path2D.Double open = null;
            Point2D first = null;
            Point2D last = null;
            for (Point2D point : points) {
                if (page.contains(point)) {
                    if (open == null) {
                        open = new Path2D.Double();
                        open.moveTo(point.getX(), point.getY());
                        first = point;
                    } else {
                        open.lineTo(point.getX(), point.getY());
                    }
                    last = point;
                } else if (open != null) {
                    runs.add(new Run(open, first, last));
                    open = null;
                }
            }
            if (open != null) {
                runs.add(new Run(open, first, last));
            }
            return runs;
        }
    }
}
