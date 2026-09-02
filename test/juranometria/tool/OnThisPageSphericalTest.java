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

    /**
     * Does any pixel of the paper fall inside the angular ellipse?
     *
     * <p>Every pixel, not every third: a three-pixel step can step
     * over a sliver, and an oracle that misses what the rule finds
     * would report the rule wrong for being right (gate review).
     * This is slow and exhaustive, which is what an oracle is for.
     */
    private static boolean oracleReaches(ChartScene scene,
                                         SkyPosition centre,
                                         double semiMajorDeg,
                                         double semiMinorDeg,
                                         double positionAngleDeg) {
        int width = scene.viewport().widthPx();
        int height = scene.viewport().heightPx();
        for (int y = 1; y <= height - 1; y++) {
            for (int x = 1; x <= width - 1; x++) {
                SkyPosition sky = ChartHitTest.skyAt(scene, x + 0.5, y + 0.5);
                if (sky != null && OnThisPageStudyMain.insideAngularEllipse(
                        centre, sky, semiMajorDeg, semiMinorDeg,
                        positionAngleDeg)) {
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
            for (double offset : new double[] {field * 0.35,
                    field * 0.6}) {
                for (double bearing : new double[] {0, 90, 200}) {
                    SkyPosition centre = OnThisPageStudyMain.offsetOf(
                            pageCentre, offset, bearing);
                    for (double semiMajor : new double[] {field * 0.12,
                            field * 0.45}) {
                        for (double ratio : new double[] {1.0, 0.3}) {
                            for (double pa : new double[] {0, 60}) {
                                boolean mine = rule(scene, centre,
                                        semiMajor, semiMajor * ratio, pa);
                                boolean oracle = oracleReaches(scene,
                                        centre, semiMajor,
                                        semiMajor * ratio, pa);
                                // Both directions. Asserting only
                                // that the rule finds what the
                                // oracle finds would let a rule
                                // that answers yes to everything
                                // pass (gate review).
                                assertEquals(oracle, mine, String.format(
                                        "centre %.3f,%.3f semi-major"
                                                + " %.3f° ratio %.1f"
                                                + " pa %.0f on a %.0f°"
                                                + " page",
                                        centre.raDegrees(),
                                        centre.decDegrees(), semiMajor,
                                        ratio, pa,
                                        scene.viewport()
                                                .fieldWidthDegrees()));
                                if (oracle) {
                                    reaching++;
                                }
                                checked++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked >= 100, "a real sweep: " + checked);
        assertTrue(reaching > 5 && reaching < checked - 5,
                "the sweep straddles the page edge: " + reaching
                        + " of " + checked);
    }

    @Test
    void anObjectHoldingTheWholePageIsOnIt() {
        // The case a boundary test can never see: nothing of the
        // outline is on the paper and the centre is off it, yet
        // every pixel a reader sees is inside the object. A rule
        // built only from sampled boundary points answers no
        // (gate review).
        ChartScene scene = page(80.894, -69.756, 1.0);
        SkyPosition centre = OnThisPageStudyMain.offsetOf(
                scene.viewport().centre(), 4.0, 45.0);
        double semiMajor = 8.0;

        assertTrue(oracleReaches(scene, centre, semiMajor, semiMajor, 0.0),
                "the premise: this object covers the whole page");
        assertTrue(rule(scene, centre, semiMajor, semiMajor, 0.0),
                "so it is on the page, though no part of its outline"
                        + " is and its centre is not");
    }

    @Test
    void anObjectWhoseOutlineCrossesTheHorizonIsStillOnThePage() {
        // Larger than anything the catalogue holds, and the reason
        // the closed path cannot be the whole answer: part of this
        // object's boundary is more than 90° from the page centre,
        // where a gnomonic projection has nothing to say. The
        // outline is not a closed curve on this page, so containment
        // has to be asked of the paper instead.
        ChartScene scene = page(80.894, -69.756, 1.0);
        SkyPosition centre = OnThisPageStudyMain.offsetOf(
                scene.viewport().centre(), 50.0, 20.0);

        // Both directions, because what is left of the outline is
        // closed by a chord rather than by the true curve, and a
        // chord can as easily claim the page as miss it.
        for (double semiMajor : new double[] {70.0, 55.0, 45.0, 30.0}) {
            for (double ratio : new double[] {1.0, 0.4}) {
                assertEquals(
                        oracleReaches(scene, centre, semiMajor,
                                semiMajor * ratio, 30.0),
                        rule(scene, centre, semiMajor,
                                semiMajor * ratio, 30.0),
                        String.format("a %.0f° object at %.0f° ratio"
                                + " %.1f, its outline running off the"
                                + " projection", semiMajor, 50.0, ratio));
            }
        }
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
