package juranometria.project;

import java.util.ArrayList;
import java.util.List;

/**
 * What a great circle becomes on a page (issue #298).
 *
 * <p>The atlas had one word for this, and it was two endpoints drawn
 * as a line - because a great circle is straight under the gnomonic
 * projection and under no other. These are the three words that
 * cover both of the atlas's projections, and Sprint 30's gate
 * measured that there is no fourth: over 108 pages of real sky, every
 * great circle took one of them, exactly, and none was sampled
 * (docs/decisions/overview-projection.md).
 *
 * <ul>
 *   <li>a <strong>straight</strong> run - every gnomonic great circle,
 *       and any circle through the page centre under the other two;</li>
 *   <li>a <strong>circular</strong> run - every other stereographic
 *       great circle;</li>
 *   <li>an <strong>elliptical</strong> run - every great circle of a
 *       projection that shows a hemisphere, and nothing else.</li>
 * </ul>
 *
 * <p>The third word is here although neither projection the atlas
 * ships makes one, and that is the gate's decision rather than a
 * guess: it measured the form exactly over seventeen real pages so
 * that #301 would be an addition rather than a redesign, and it
 * assigned all three words to this issue. Leaving it out would have
 * put the redesign back. It is exercised on the pages the gate
 * measured it on - a hemisphere's, built here from the projection's
 * own closed form - which is what "a page it actually crosses"
 * means for a form no shipped page can reach yet.
 *
 * <p>What an elliptical run does <em>not</em> settle is which half of
 * it a hemisphere shows. A great circle's far half projects onto the
 * same ellipse as its near half, and the conic cannot tell them
 * apart; hiding one is a policy about a globe and is listed in #301
 * as something that issue decides. This says what the curve is.
 *
 * <p>Nothing here is named for a meridian, a horizon, an ecliptic or
 * a projection. A module says what a circle <em>means</em>; this says
 * only what it looks like.
 *
 * <p>Two things this can say that the old word could not, both found
 * by clipping real pages rather than by thinking about them: a curve
 * can cross one page in <strong>more than one run</strong>, since a
 * circle and a rectangle meet in up to eight points; and a curve can
 * <strong>close</strong>, with no ends for a label to hang on.
 */
public sealed interface PlaneCurve {

    /** What this is, for a report or a test to name. */
    String form();

    /** The visible runs of this curve on a page, in drawing order. */
    List<CurveRun> clipTo(PageRegion region);

    /**
     * A straight run, held as the equation
     * {@code b x + c y + a = 0} with {@code b} and {@code c} a unit
     * vector, so that {@code a} is the signed distance from the
     * origin and evaluating the equation at a point gives that
     * point's distance from the line.
     *
     * <p>Two far-apart points would be the obvious representation and
     * are the wrong one: a line carried far enough out to cross any
     * page loses digits to cancellation when anything is measured
     * against it, and how far is far enough depends on a scale the
     * curve does not know.
     */
    record Straight(double a, double b, double c) implements PlaneCurve {

        public Straight {
            // The normalisation is the whole meaning of the record:
            // without it {@code a} is not a distance and
            // distanceFrom answers in units of nothing. Held here so
            // that a caller reaching past of() cannot quietly make a
            // line that measures wrongly.
            if (!Double.isFinite(a) || !Double.isFinite(b)
                    || !Double.isFinite(c)
                    || Math.abs(Math.hypot(b, c) - 1.0) > 1.0e-9) {
                throw new IllegalArgumentException(
                        "a line's direction is a unit vector: " + a + ", "
                                + b + ", " + c);
            }
        }

        /** Normalised, so the coefficients mean a distance. */
        public static Straight of(double a, double b, double c) {
            double length = Math.hypot(b, c);
            return new Straight(a / length, b / length, c / length);
        }

        /** How far a point lies off this line. */
        public double distanceFrom(double x, double y) {
            return Math.abs(b * x + c * y + a);
        }

        @Override
        public String form() {
            return "straight";
        }

        @Override
        public List<CurveRun> clipTo(PageRegion region) {
            // Built about the page rather than about the origin, so
            // the numbers stay the size of the page.
            double middleX = region.centreX();
            double middleY = region.centreY();
            double off = b * middleX + c * middleY + a;
            double nearX = middleX - off * b;
            double nearY = middleY - off * c;
            double reach = 2.0 * region.reach();
            double fromX = nearX + reach * c;
            double fromY = nearY - reach * b;
            double dx = -2.0 * reach * c;
            double dy = 2.0 * reach * b;

            double[] window = {0.0, 1.0};
            double[] p = {-dx, dx, -dy, dy};
            double[] q = {fromX - region.minX(), region.maxX() - fromX,
                    fromY - region.minY(), region.maxY() - fromY};
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
                // And then to the limb, which cuts the same run a
                // second time. A line meets a circle at the roots of
                // one quadratic, so this is exact and samples
                // nothing.
                double offsetX = fromX - region.limbX();
                double offsetY = fromY - region.limbY();
                double aa = dx * dx + dy * dy;
                double bb = 2.0 * (offsetX * dx + offsetY * dy);
                double cc = offsetX * offsetX + offsetY * offsetY
                        - region.limbRadius() * region.limbRadius();
                double discriminant = bb * bb - 4.0 * aa * cc;
                if (discriminant <= 0.0) {
                    return List.of();  // it misses the sky entirely
                }
                double root = Math.sqrt(discriminant);
                window[0] = Math.max(window[0], (-bb - root) / (2.0 * aa));
                window[1] = Math.min(window[1], (-bb + root) / (2.0 * aa));
                if (window[0] >= window[1]) {
                    return List.of();
                }
            }
            return List.of(new CurveRun.Segment(
                    new PixelPoint(fromX + window[0] * dx,
                            fromY + window[0] * dy),
                    new PixelPoint(fromX + window[1] * dx,
                            fromY + window[1] * dy)));
        }
    }

    /** A circular run: the general stereographic great circle. */
    record Circular(double centreX, double centreY, double radius)
            implements PlaneCurve {

        public Circular {
            if (!Double.isFinite(centreX) || !Double.isFinite(centreY)
                    || !Double.isFinite(radius) || radius <= 0.0) {
                throw new IllegalArgumentException(
                        "a circle has a centre and a radius: " + centreX
                                + "," + centreY + " r " + radius);
            }
        }

        @Override
        public String form() {
            return "circular";
        }

        @Override
        public List<CurveRun> clipTo(PageRegion region) {
            List<Double> crossings = new ArrayList<>();
            crossings.addAll(meetsLimb(region));
            crossings.addAll(meets(region.minY(), region.minX(),
                    region.maxX(), true));
            crossings.addAll(meets(region.maxY(), region.minX(),
                    region.maxX(), true));
            crossings.addAll(meets(region.minX(), region.minY(),
                    region.maxY(), false));
            crossings.addAll(meets(region.maxX(), region.minY(),
                    region.maxY(), false));
            return CurveRuns.of(crossings, this::at,
                    (start, span, one, other) -> new CurveRun.Arc(
                            centreX, centreY, radius, radius, 0.0,
                            start, span, one, other),
                    region);
        }

        /** A point on this circle, at an angle about its centre. */
        public PixelPoint at(double angle) {
            return new PixelPoint(centreX + radius * Math.cos(angle),
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

        /** Two circles meet on their radical line: exact, and no more
         *  work than meeting an edge. */
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
    }

    /**
     * An elliptical arc: the visible half of a bounded projection's
     * great circle, carried with its own parameter window so the
     * invisible half is excluded before anything is drawn (owner
     * ruling on the pan regression).
     *
     * <p>{@code radiusAlong} may be zero: that is the degeneracy's
     * continuous limit - the arc IS the diameter - and it is reached
     * by the same formula rather than by substitution. Clipping
     * works exactly as the full ellipse's does, in the frame where
     * the curve is a unit circle, with the window's two ends joining
     * the page's crossings; the limb guard is the same test with the
     * same graze, unrelaxed.
     */
    record EllipticalArc(double centreX, double centreY,
                         double radiusAlong, double radiusAcross,
                         double tiltRadians, double fromRadians,
                         double toRadians)
            implements PlaneCurve {

        public EllipticalArc {
            if (!Double.isFinite(centreX) || !Double.isFinite(centreY)
                    || !Double.isFinite(tiltRadians)
                    || !Double.isFinite(radiusAlong)
                    || !Double.isFinite(radiusAcross)
                    || radiusAlong < 0.0 || radiusAcross <= 0.0
                    || !(fromRadians < toRadians)
                    || toRadians - fromRadians > 2.0 * Math.PI + 1e-9) {
                throw new IllegalArgumentException(
                        "an elliptical arc has a centre, two radii and"
                                + " a window: " + centreX + "," + centreY
                                + " " + radiusAlong + " by " + radiusAcross
                                + " over [" + fromRadians + ","
                                + toRadians + "]");
            }
        }

        @Override
        public String form() {
            return "elliptical-arc";
        }

        /** A point at an angle in the ellipse's own frame. */
        public PixelPoint at(double angle) {
            double cos = Math.cos(tiltRadians);
            double sin = Math.sin(tiltRadians);
            double along = radiusAlong * Math.cos(angle);
            double across = radiusAcross * Math.sin(angle);
            return new PixelPoint(centreX + along * cos - across * sin,
                    centreY + along * sin + across * cos);
        }

        /**
         * Thinner than any line the atlas draws: clipped as the
         * segment it is, capped at its own window ends rather than
         * carried to the page edge as an infinite line would be.
         */
        private static final double SLIVER_PX = 1.0e-6;

        @Override
        public List<CurveRun> clipTo(PageRegion region) {
            if (radiusAlong < SLIVER_PX) {
                return sliver(region);
            }
            double cos = Math.cos(tiltRadians);
            double sin = Math.sin(tiltRadians);
            double[][] corners = new double[4][];
            double[][] page = {
                    {region.minX(), region.minY()},
                    {region.maxX(), region.minY()},
                    {region.maxX(), region.maxY()},
                    {region.minX(), region.maxY()}};
            for (int at = 0; at < 4; at++) {
                double dx = page[at][0] - centreX;
                double dy = page[at][1] - centreY;
                corners[at] = new double[] {
                        (dx * cos + dy * sin) / radiusAlong,
                        (-dx * sin + dy * cos) / radiusAcross};
            }
            List<Double> crossings = new ArrayList<>();
            for (int at = 0; at < 4; at++) {
                crossings.addAll(Elliptical.meets(corners[at],
                        corners[(at + 1) % 4]));
            }
            Elliptical.refuseALimbThisCannotCut(centreX, centreY,
                    Math.max(radiusAlong, radiusAcross), region);
            return CurveRuns.ofWindow(crossings, this::at,
                    (start, span, one, other) -> new CurveRun.Arc(
                            centreX, centreY, radiusAlong, radiusAcross,
                            tiltRadians, start, span, one, other),
                    region, fromRadians, toRadians);
        }

        /** The degenerate limit: a diameter segment, param-clipped. */
        private List<CurveRun> sliver(PageRegion region) {
            PixelPoint one = at(fromRadians);
            PixelPoint other = at(toRadians);
            double dx = other.x() - one.x();
            double dy = other.y() - one.y();
            double t0 = 0.0;
            double t1 = 1.0;
            double[][] walls = {
                    {1.0, 0.0, region.minX(), region.maxX()},
                    {0.0, 1.0, region.minY(), region.maxY()}};
            for (double[] wall : walls) {
                double p0 = wall[0] * one.x() + wall[1] * one.y();
                double d = wall[0] * dx + wall[1] * dy;
                if (Math.abs(d) < 1e-12) {
                    if (p0 < wall[2] || p0 > wall[3]) {
                        return List.of();
                    }
                    continue;
                }
                double enter = (wall[2] - p0) / d;
                double leave = (wall[3] - p0) / d;
                if (enter > leave) {
                    double swap = enter;
                    enter = leave;
                    leave = swap;
                }
                t0 = Math.max(t0, enter);
                t1 = Math.min(t1, leave);
            }
            if (t0 >= t1) {
                return List.of();
            }
            return List.of(new CurveRun.Segment(
                    new PixelPoint(one.x() + t0 * dx, one.y() + t0 * dy),
                    new PixelPoint(one.x() + t1 * dx,
                            one.y() + t1 * dy)));
        }
    }

    /**
     * An elliptical run: what a projection showing a hemisphere makes
     * of a great circle.
     *
     * <p>{@code radiusAlong} lies along the tilt and
     * {@code radiusAcross} square to it, so a point of the curve is
     * at angle {@code t} in the frame where the ellipse is a unit
     * circle. That frame is the whole of the clipping: crossings are
     * found there, where the curve is a circle and an edge is an
     * ordinary segment, and the answer is carried back out. One
     * affine change of variables, and no sampling.
     */
    record Elliptical(double centreX, double centreY, double radiusAlong,
                      double radiusAcross, double tiltRadians)
            implements PlaneCurve {

        public Elliptical {
            if (!Double.isFinite(centreX) || !Double.isFinite(centreY)
                    || !Double.isFinite(tiltRadians)
                    || !Double.isFinite(radiusAlong)
                    || !Double.isFinite(radiusAcross)
                    || radiusAlong <= 0.0 || radiusAcross <= 0.0) {
                throw new IllegalArgumentException(
                        "an ellipse has a centre and two radii: " + centreX
                                + "," + centreY + " " + radiusAlong + " by "
                                + radiusAcross);
            }
        }

        @Override
        public String form() {
            return "elliptical";
        }

        /** A point on this ellipse, at an angle in its own frame. */
        public PixelPoint at(double angle) {
            double cos = Math.cos(tiltRadians);
            double sin = Math.sin(tiltRadians);
            double along = radiusAlong * Math.cos(angle);
            double across = radiusAcross * Math.sin(angle);
            return new PixelPoint(centreX + along * cos - across * sin,
                    centreY + along * sin + across * cos);
        }

        /** The same point in the frame where this is a unit circle. */
        private double[] frameOf(double x, double y) {
            double cos = Math.cos(tiltRadians);
            double sin = Math.sin(tiltRadians);
            double dx = x - centreX;
            double dy = y - centreY;
            return new double[] {(dx * cos + dy * sin) / radiusAlong,
                    (-dx * sin + dy * cos) / radiusAcross};
        }

        @Override
        public List<CurveRun> clipTo(PageRegion region) {
            double[][] corners = {
                    frameOf(region.minX(), region.minY()),
                    frameOf(region.maxX(), region.minY()),
                    frameOf(region.maxX(), region.maxY()),
                    frameOf(region.minX(), region.maxY())};
            List<Double> crossings = new ArrayList<>();
            for (int at = 0; at < 4; at++) {
                crossings.addAll(meets(corners[at], corners[(at + 1) % 4]));
            }
            refuseALimbThisCannotCut(region);
            return CurveRuns.of(crossings, this::at,
                    (start, span, one, other) -> new CurveRun.Arc(
                            centreX, centreY, radiusAlong, radiusAcross,
                            tiltRadians, start, span, one, other),
                    region);
        }

        /**
         * No limb crossings are collected, and that is a claim rather
         * than an omission - checked here rather than assumed.
         *
         * <p>An ellipse meets a circle at the roots of a quartic,
         * which this vocabulary does not have. The gate measured that
         * it does not need one: the only projection with a limb is
         * orthographic, and every orthographic great circle lies
         * inside its own limb, because a point an angle {@code t}
         * from the centre lands at {@code sin t} and the limb is at
         * one. The two touch and never cross.
         *
         * <p>So the containment is tested instead of trusted. An
         * ellipse lies inside the circle about its own centre of its
         * larger radius, so that circle inside the limb settles it
         * without a quartic - and an ellipse that would leave says so
         * loudly rather than being drawn through a boundary the
         * clipping never looked at.
         */
        private void refuseALimbThisCannotCut(PageRegion region) {
            refuseALimbThisCannotCut(centreX, centreY,
                    Math.max(radiusAlong, radiusAcross), region);
        }

        static void refuseALimbThisCannotCut(double centreX,
                                             double centreY,
                                             double maxRadius,
                                             PageRegion region) {
            if (!region.bounded()) {
                return;
            }
            double apart = Math.hypot(centreX - region.limbX(),
                    centreY - region.limbY());
            double outermost = apart + maxRadius;
            if (outermost <= region.limbRadius()
                    * (1.0 + CurveRuns.GRAZE)) {
                return;
            }
            throw new IllegalStateException(String.format(
                    java.util.Locale.ROOT,
                    "this ellipse reaches %.3f page units from the limb's"
                            + " centre and the sky stops at %.3f, so it"
                            + " leaves the visible region: cutting it there"
                            + " is the ellipse-meets-circle quartic the"
                            + " gate measured to be unreachable and issue"
                            + " #301 would owe",
                    outermost, region.limbRadius()));
        }

        /** Where the unit circle meets one edge, now a segment. */
        private static List<Double> meets(double[] from, double[] to) {
            double dx = to[0] - from[0];
            double dy = to[1] - from[1];
            double a = dx * dx + dy * dy;
            if (a == 0.0) {
                return List.of();
            }
            double b = 2.0 * (from[0] * dx + from[1] * dy);
            double c = from[0] * from[0] + from[1] * from[1] - 1.0;
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
                found.add(CurveRuns.angle(from[1] + along * dy,
                        from[0] + along * dx));
            }
            return found;
        }
    }
}
