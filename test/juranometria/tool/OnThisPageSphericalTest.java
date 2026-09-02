package juranometria.tool;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartHitTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether an object's angular ellipse reaches the paper (Sprint 24,
 * issue #214), checked from the opposite direction.
 *
 * <p>The rule walks the ellipse's boundary <em>on the sphere</em>
 * and projects each point onto the page. An oracle that did the same
 * thing would only prove the code runs twice, so this one works
 * inward: it samples the <strong>paper</strong>, turns each pixel
 * back into a sky position through the atlas's own inverse - what
 * grab-to-pan uses - and asks whether that position lies inside the
 * object's angular ellipse.
 *
 * <p>Forward and inverse are independent enough to disagree if the
 * geometry is wrong, and they must not: a page either shows part of
 * an object or it does not.
 *
 * <p>This matters most where the review said it would. The Large
 * Magellanic Cloud is nearly eleven degrees across, and a gnomonic
 * page has no single scale - so an ellipse sized once at the page
 * centre is the wrong shape by the time it reaches an edge, which is
 * precisely where the question is asked.
 */
class OnThisPageSphericalTest {

    /** Membership in an angular ellipse: true separation and bearing. */
    private static boolean insideEllipse(SkyPosition centre,
                                         SkyPosition point,
                                         double semiMajorDeg,
                                         double semiMinorDeg,
                                         double positionAngleDeg) {
        double r = centre.separationDegrees(point);
        if (r > semiMajorDeg) {
            return false;
        }
        double bearing = bearingDegrees(centre, point);
        double offset = Math.toRadians(bearing - positionAngleDeg);
        double along = r * Math.cos(offset);
        double across = r * Math.sin(offset);
        return (along * along) / (semiMajorDeg * semiMajorDeg)
                + (across * across) / (semiMinorDeg * semiMinorDeg) <= 1.0;
    }

    /** Bearing east of north, on the sphere. */
    private static double bearingDegrees(SkyPosition from, SkyPosition to) {
        double dec1 = Math.toRadians(from.decDegrees());
        double dec2 = Math.toRadians(to.decDegrees());
        double dRa = Math.toRadians(to.raDegrees() - from.raDegrees());
        double y = Math.sin(dRa) * Math.cos(dec2);
        double x = Math.cos(dec1) * Math.sin(dec2)
                - Math.sin(dec1) * Math.cos(dec2) * Math.cos(dRa);
        double degrees = Math.toDegrees(Math.atan2(y, x)) % 360.0;
        return degrees < 0 ? degrees + 360.0 : degrees;
    }

    /** Does any pixel of the paper fall inside the angular ellipse? */
    private static boolean oracleReaches(ChartScene scene,
                                         SkyPosition centre,
                                         double semiMajorDeg,
                                         double semiMinorDeg,
                                         double positionAngleDeg) {
        int width = scene.viewport().widthPx();
        int height = scene.viewport().heightPx();
        for (int y = 1; y <= height - 1; y += 3) {
            for (int x = 1; x <= width - 1; x += 3) {
                SkyPosition sky = ChartHitTest.skyAt(scene, x + 0.5, y + 0.5);
                if (sky != null && insideEllipse(centre, sky, semiMajorDeg,
                        semiMinorDeg, positionAngleDeg)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The rule, reached through the study's own entry point. */
    private static boolean rule(ChartScene scene, SkyPosition centre,
                                double semiMajorDeg, double semiMinorDeg,
                                double positionAngleDeg) {
        return OnThisPageStudyMain.reachesPaper(scene, centre,
                semiMajorDeg, semiMinorDeg, positionAngleDeg);
    }

    private static ChartScene page(double ra, double dec, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(ra, dec), field, 8.0),
                900, 700);
    }

    @Test
    void theSphericalRuleAgreesWithTheInverseOracle() {
        // Pages chosen where a flat, centre-scale ellipse is most
        // likely to be wrong: a wide field, a pole, and the seam.
        List<ChartScene> pages = List.of(
                page(80.894, -69.756, 36.0),
                page(80.894, -69.756, 8.0),
                page(0.0, 85.0, 18.0),
                page(0.0, 20.0, 36.0),
                page(10.684708, 41.268750, 1.0));

        int checked = 0;
        int reaching = 0;
        for (ChartScene scene : pages) {
            SkyPosition pageCentre = scene.viewport().centre();
            double field = scene.viewport().fieldWidthDegrees();
            for (double offset : new double[] {0.0, field * 0.3,
                    field * 0.55, field * 0.8}) {
                for (double bearing : new double[] {0, 45, 90, 135, 180,
                        225, 270, 315}) {
                    SkyPosition centre = OnThisPageStudyMain.offsetOf(
                            pageCentre, offset, bearing);
                    for (double semiMajor : new double[] {field * 0.05,
                            field * 0.2, field * 0.6}) {
                        for (double ratio : new double[] {1.0, 0.3}) {
                            for (double pa : new double[] {0, 60, 120}) {
                                boolean mine = rule(scene, centre,
                                        semiMajor, semiMajor * ratio, pa);
                                boolean oracle = oracleReaches(scene,
                                        centre, semiMajor,
                                        semiMajor * ratio, pa);
                                if (oracle) {
                                    reaching++;
                                    assertTrue(mine, String.format(
                                            "the oracle finds this on the"
                                                    + " page and the rule"
                                                    + " does not:"
                                                    + " centre %.3f,%.3f"
                                                    + " semi-major %.3f°"
                                                    + " ratio %.1f pa %.0f",
                                            centre.raDegrees(),
                                            centre.decDegrees(),
                                            semiMajor, ratio, pa));
                                }
                                checked++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked > 1000, "a real sweep: " + checked);
        assertTrue(reaching > 50 && reaching < checked - 50,
                "the sweep straddles the page edge: " + reaching
                        + " of " + checked);
    }

    @Test
    void aCloudElevenDegreesAcrossIsNotAFlatEllipse() {
        // The review's own case. The Large Magellanic Cloud's
        // recorded major axis is 646 arcminutes - 10.8 degrees - and
        // a gnomonic page stretches away from its centre, so an
        // ellipse sized once at the middle is the wrong shape at the
        // edge. Placed off-centre on a wide page, the rule and the
        // inverse oracle must still agree.
        ChartScene scene = page(80.894, -69.756, 36.0);
        SkyPosition cloud = OnThisPageStudyMain.offsetOf(
                scene.viewport().centre(), 16.0, 90.0);
        double semiMajor = 646.0 / 120.0;

        assertEquals(oracleReaches(scene, cloud, semiMajor,
                        semiMajor * 0.86, 170.0),
                rule(scene, cloud, semiMajor, semiMajor * 0.86, 170.0),
                "the Cloud, off-centre on a 36° page");
    }
}
