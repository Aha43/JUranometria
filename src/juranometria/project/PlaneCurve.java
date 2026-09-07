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
 *       and any circle through the page centre under the other;</li>
 *   <li>a <strong>circular</strong> run - every other stereographic
 *       great circle;</li>
 * </ul>
 *
 * <p>Two words, because two are what the atlas's two projections
 * demonstrate. The gate measured a third - an ellipse, which is what
 * a projection showing a hemisphere makes of a great circle - and
 * measured it exactly, over seventeen real pages, precisely so that
 * adding it would be an addition rather than a redesign. It is not
 * here because no page the atlas can draw crosses one, and a word
 * nothing can exercise is a word no test can defend.
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

}
