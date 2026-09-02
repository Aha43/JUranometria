package juranometria.tool;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;

import juranometria.project.PixelPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether a recorded ellipse reaches the paper (Sprint 24, issue
 * #214), checked against an oracle that knows nothing of Java2D.
 *
 * <p>The gate first asked whether a circle of the recorded
 * half-major reached the paper. That circle contains the ellipse
 * whichever way it lies, so it never misses - and it says yes for a
 * thin object lying away from the page whose known ellipse never
 * comes near it (review). The rule is the ellipse now, and this is
 * where that is shown to be true rather than asserted.
 *
 * <p>The oracle samples the ellipse's own boundary and asks whether
 * any of it lands on the paper, and asks whether the ellipse holds a
 * corner of the paper. Between them those catch every way a convex
 * shape can meet a rectangle: crossing an edge, sitting inside, or
 * swallowing it whole.
 */
class OnThisPageGeometryTest {

    private static final Rectangle2D PAPER =
            new Rectangle2D.Double(1, 1, 898, 698);

    /** Independent of the shape library: samples and containment. */
    private static boolean oracleReaches(double cx, double cy,
                                         double majorPx, double minorPx,
                                         double positionAngle) {
        double a = minorPx / 2.0;
        double b = majorPx / 2.0;
        double theta = -Math.toRadians(positionAngle);
        for (int i = 0; i < 4000; i++) {
            double t = 2 * Math.PI * i / 4000.0;
            double lx = a * Math.cos(t);
            double ly = b * Math.sin(t);
            double x = cx + lx * Math.cos(theta) - ly * Math.sin(theta);
            double y = cy + lx * Math.sin(theta) + ly * Math.cos(theta);
            if (PAPER.contains(x, y)) {
                return true;
            }
        }
        // Or the ellipse swallows the paper whole: test its corners.
        for (double[] corner : new double[][] {
                {PAPER.getMinX(), PAPER.getMinY()},
                {PAPER.getMaxX(), PAPER.getMinY()},
                {PAPER.getMinX(), PAPER.getMaxY()},
                {PAPER.getMaxX(), PAPER.getMaxY()}}) {
            double dx = corner[0] - cx;
            double dy = corner[1] - cy;
            double lx = dx * Math.cos(-theta) - dy * Math.sin(-theta);
            double ly = dx * Math.sin(-theta) + dy * Math.cos(-theta);
            if ((lx * lx) / (a * a) + (ly * ly) / (b * b) <= 1.0) {
                return true;
            }
        }
        return false;
    }

    private static boolean production(double cx, double cy, double majorPx,
                                      double minorPx, double positionAngle) {
        Shape ellipse = OnThisPageStudyMain.recordedEllipse(
                new PixelPoint(cx, cy), majorPx, minorPx, positionAngle);
        return ellipse.intersects(PAPER);
    }

    @Test
    void theRuleAgreesWithAnOracleThatKnowsNoJava2D() {
        int checked = 0;
        int reaching = 0;
        // A sweep around and across the page edge, at every
        // orientation, for shapes from round to very thin.
        for (double cx = -300; cx <= 1200; cx += 75) {
            for (double cy = -300; cy <= 1000; cy += 75) {
                for (double major : new double[] {40, 200, 600}) {
                    for (double ratio : new double[] {1.0, 0.5, 0.08}) {
                        for (double pa : new double[] {0, 23, 45, 90, 157}) {
                            double x = cx;
                            double y = cy;
                            boolean mine = production(x, y, major,
                                    major * ratio, pa);
                            boolean oracle = oracleReaches(x, y, major,
                                    major * ratio, pa);
                            assertEquals(oracle, mine, String.format(
                                    "ellipse at %.0f,%.0f major %.0f ratio"
                                            + " %.2f pa %.0f", x, y,
                                    major, ratio, pa));
                            checked++;
                            if (oracle) {
                                reaching++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked > 5000, "a real sweep: " + checked);
        // The premise: the sweep contains both answers, or agreement
        // would be worth nothing.
        assertTrue(reaching > 100 && reaching < checked - 100,
                "the sweep straddles the page edge: " + reaching
                        + " of " + checked + " reach it");
    }

    @Test
    void aThinEllipseLyingAwayIsNotOnThePageThoughItsCircleWouldBe() {
        // The case the envelope got wrong, named so it cannot come
        // back: a long thin object just past the right edge, lying
        // along the edge rather than towards it. Its half-major
        // reaches the paper; the object does not.
        double cx = PAPER.getMaxX() + 90;
        double cy = 350;
        double major = 400;
        double minor = 20;

        assertFalse(production(cx, cy, major, minor, 0.0),
                "lying north-south beside the page, it never reaches it");
        assertFalse(oracleReaches(cx, cy, major, minor, 0.0),
                "and the oracle agrees");

        double halfMajor = major / 2.0;
        assertTrue(cx - PAPER.getMaxX() < halfMajor,
                "the premise: a circle of the half-major WOULD have"
                        + " reached, which is what made the envelope"
                        + " wrong");

        // Turned to face the page, the same object does reach it.
        assertTrue(production(cx, cy, major, minor, 90.0),
                "turned east-west, it reaches across the edge");
    }
}
