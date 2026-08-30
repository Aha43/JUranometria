package juranometria.tool;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pointer-zoom geometry contract of docs/decisions/
 * pointer-zoom.md, locked before production implementation (#124):
 * subpixel anchor preservation at ordinary positions, the stated
 * reversal tolerance, and honestly classified constrained,
 * past-pole, and second-branch outcomes at the poles.
 */
class ZoomStudyMainTest {

    private static final double DRIFT_TOLERANCE_PX = 1e-2;
    private static final double REVERSAL_TOLERANCE_DEGREES = 1e-6;

    @Test
    void ordinaryPointersKeepTheirSkySubpixelAcrossEveryStep() {
        // Representative ordinary pages including the RA wrap; corner
        // and edge pointers; each adjacent transition both ways.
        SkyPosition[] centres = {
                new SkyPosition(10.684708, 41.268750),
                new SkyPosition(83.818667, -5.389667),
                new SkyPosition(0.3, 45.0),
                new SkyPosition(186.649563, -63.099093)};
        PixelPoint[] pointers = {
                new PixelPoint(1.0, 1.0), new PixelPoint(899.0, 349.0),
                new PixelPoint(450.0, 699.0), new PixelPoint(300.0, 200.0)};
        double[][] transitions = {{36, 24}, {24, 18}, {18, 12}, {12, 8},
                {8, 6}, {6, 4}, {4, 3}, {3, 2}, {2, 1},
                {1, 2}, {8, 12}, {24, 36}};
        for (SkyPosition centre : centres) {
            for (PixelPoint pointer : pointers) {
                for (double[] transition : transitions) {
                    var step = ZoomStudyMain.solve(centre, transition[0],
                            transition[1], pointer);
                    assertFalse(step.solution().constrained()
                                    || step.solution().pastPole(),
                            "ordinary geometry solves exactly at " + centre);
                    assertTrue(ZoomStudyMain.pointerDriftPx(step,
                                    transition[1], pointer)
                                    < DRIFT_TOLERANCE_PX,
                            "the sky under the pointer stays under the"
                                    + " pointer");
                }
            }
        }
    }

    @Test
    void reversingAnAcceptedStepRestoresTheOriginalViewWithinTolerance() {
        SkyPosition centre = new SkyPosition(0.0, -85.0);
        PixelPoint pointer = new PixelPoint(899.0, 350.0);
        var out = ZoomStudyMain.solve(centre, 8.0, 6.0, pointer);
        SkyPosition mid = out.solution().centre().orElseThrow();
        var back = ZoomStudyMain.solve(mid, 6.0, 8.0, pointer);
        assertTrue(back.solution().centre().orElseThrow()
                        .separationDegrees(centre)
                        < REVERSAL_TOLERANCE_DEGREES,
                "one step and its reverse restore the centre");
        assertTrue(ZoomStudyMain.pointerDriftPx(back, 8.0, pointer)
                        < DRIFT_TOLERANCE_PX,
                "and the pointer's sky lands back at the pointer");
    }

    @Test
    void polarOutcomesAreClassifiedNeverQuietlyWrong() {
        // The near-pole page from the study: sweeping its pointers
        // over one wide transition must produce every classified
        // category and no unexplained emptiness (the solver throws on
        // those by contract).
        SkyPosition nearPole = new SkyPosition(37.946619, 89.9);
        int exact = 0;
        int constrained = 0;
        int pastPole = 0;
        for (double x : new double[] {1, 450, 899}) {
            for (double y : new double[] {1, 350, 699}) {
                for (double[] transition : new double[][] {
                        {36, 24}, {24, 36}, {12, 8}, {8, 12}}) {
                    var step = ZoomStudyMain.solve(nearPole, transition[0],
                            transition[1], new PixelPoint(x, y));
                    if (step.solution().pastPole()) {
                        pastPole++;
                    } else if (step.solution().constrained()) {
                        constrained++;
                    } else {
                        exact++;
                    }
                }
            }
        }
        assertTrue(exact > 0, "the pole still zooms where geometry allows");
        assertTrue(constrained > 0,
                "infeasible anchors clamp to the boundary, classified");
        assertTrue(pastPole > 0,
                "past-pole requests are classified for refusal, decided"
                        + " as a complete no-op in production");
    }

    @Test
    void theSecondExactBranchAtThePoleIsRealAndVerified() {
        // The documented alternate-meridian physics: a pointer
        // anchoring sky beyond the pole reverses onto a second exact
        // centre. Honesty requires BOTH facts: the branches differ,
        // and the original centre still solves the reverse problem
        // exactly.
        SkyPosition centre = new SkyPosition(37.946619, 89.9);
        PixelPoint pointer = new PixelPoint(450.0, 1.0);
        var out = ZoomStudyMain.solve(centre, 36.0, 24.0, pointer);
        SkyPosition mid = out.solution().centre().orElseThrow();
        var back = ZoomStudyMain.solve(mid, 24.0, 36.0, pointer);
        SkyPosition landed = back.solution().centre().orElseThrow();
        assertTrue(landed.separationDegrees(centre) > 1.0,
                "the tie-break takes the nearer branch, not the original");
        assertTrue(ZoomStudyMain.solvesExactly(centre, out.anchor(),
                        back.target()),
                "the original centre remains an exact solution - two"
                        + " branches, both honest");
        assertTrue(ZoomStudyMain.pointerDriftPx(back, 36.0, pointer)
                        < DRIFT_TOLERANCE_PX,
                "the anchor invariant holds on the branch taken");
    }

    @Test
    void theAnchorIsTheExactInverseOfThePage() {
        // The anchor recovery underlying every step: pixel to plane to
        // sky and back is the identity within the solver's tolerance.
        SkyPosition centre = new SkyPosition(0.3, 45.0);
        var viewport = new juranometria.chart.ChartViewport(
                centre, 18.0, 900, 700);
        PixelPoint pixel = new PixelPoint(123.0, 456.0);
        SkyPosition anchor = PanSolver.skyFromPlane(centre,
                PanSolver.planeFromPixel(viewport, pixel));
        var landed = new juranometria.project.ViewportMapping(viewport)
                .toPixel(new juranometria.project.GnomonicProjection(centre)
                        .project(anchor).orElseThrow());
        assertTrue(Math.hypot(landed.x() - pixel.x(),
                        landed.y() - pixel.y()) < DRIFT_TOLERANCE_PX,
                "pixel -> sky -> pixel is the identity");
    }
}
