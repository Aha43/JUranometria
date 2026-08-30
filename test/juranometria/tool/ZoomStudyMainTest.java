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
    private static final double REVERSAL_TOLERANCE_DEGREES = 1e-4;

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
                                    || step.solution().pastPole()
                                    || step.solution().ambiguous(),
                            "ordinary geometry solves exactly and"
                                    + " unambiguously at " + centre);
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
        SkyPosition mid = ZoomStudyMain.acceptStep(centre, 8.0, 6.0,
                pointer).orElseThrow();
        SkyPosition landed = ZoomStudyMain.acceptStep(mid, 6.0, 8.0,
                pointer).orElseThrow();
        assertTrue(landed.separationDegrees(centre)
                        < REVERSAL_TOLERANCE_DEGREES,
                "an accepted step's accepted reverse restores the centre");
    }

    @Test
    void theAcceptanceContractIsExactAndPreflightReversible() {
        // The decided contract (PR #127 follow-up), at its edges: an
        // ordinary step accepts and its reverse accepts; a
        // constrained polar step refuses (its anchor miss would be
        // visible); the near-pole step whose forward solve is exact
        // but whose reverse is two-branch refuses at the preflight -
        // so the wheel can never enter a view the opposite movement
        // refuses.
        assertTrue(ZoomStudyMain.acceptStep(
                        new SkyPosition(83.818667, -5.389667), 18.0, 12.0,
                        new PixelPoint(300.0, 200.0)).isPresent(),
                "ordinary steps accept");
        assertTrue(ZoomStudyMain.acceptStep(
                        new SkyPosition(37.946619, 85.0), 24.0, 36.0,
                        new PixelPoint(1.0, 350.0)).isEmpty(),
                "a constrained polar step refuses");
        SkyPosition nearPole = new SkyPosition(37.946619, 89.9);
        PixelPoint overPole = new PixelPoint(450.0, 1.0);
        var forward = ZoomStudyMain.solve(nearPole, 36.0, 24.0, overPole);
        assertFalse(forward.solution().ambiguous(),
                "the forward solve alone is exact...");
        assertTrue(ZoomStudyMain.acceptStep(nearPole, 36.0, 24.0,
                        overPole).isEmpty(),
                "...but the preflight sees the two-branch reverse and"
                        + " refuses the acceptance");
    }

    @Test
    void polarOutcomesAreClassifiedNeverQuietlyWrong() {
        // The near-pole page from the study: sweeping its pointers
        // over representative transitions must produce every
        // classified category - including the ambiguity the decision
        // refuses - and no unexplained emptiness (the solver throws
        // on those by contract).
        SkyPosition nearPole = new SkyPosition(37.946619, 89.9);
        int exact = 0;
        int constrained = 0;
        int pastPole = 0;
        int ambiguous = 0;
        for (double x : new double[] {1, 450, 899}) {
            for (double y : new double[] {1, 350, 699}) {
                for (double[] transition : new double[][] {
                        {36, 24}, {24, 36}, {12, 8}, {8, 12}}) {
                    var step = ZoomStudyMain.solve(nearPole, transition[0],
                            transition[1], new PixelPoint(x, y));
                    if (step.solution().pastPole()) {
                        pastPole++;
                    } else if (step.solution().ambiguous()) {
                        ambiguous++;
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
        assertTrue(ambiguous > 0,
                "two-branch requests are classified for refusal - the"
                        + " decision's rule, never a silent branch switch");
    }

    @Test
    void theAmbiguousTransitionIsDetectedAndDecidedRefused() {
        // The case the review surfaced: a pointer anchoring sky beyond
        // the pole reverses onto a second exact centre 28 degrees
        // away. The refusal is one-sided exactly as the decision
        // states: the forward zoom-in is unique and accepted; the
        // reverse zoom-out is the two-branch problem, now REPORTED by
        // the solver and refused by the decision - so production
        // never takes the silent 28-degree jump. The ambiguity is
        // genuine: the original centre still solves the reverse
        // problem exactly.
        SkyPosition centre = new SkyPosition(37.946619, 89.9);
        PixelPoint pointer = new PixelPoint(450.0, 1.0);
        var out = ZoomStudyMain.solve(centre, 36.0, 24.0, pointer);
        assertFalse(out.solution().ambiguous(),
                "the forward zoom-in is unique and accepted");
        SkyPosition mid = out.solution().centre().orElseThrow();
        var back = ZoomStudyMain.solve(mid, 24.0, 36.0, pointer);
        assertTrue(back.solution().ambiguous(),
                "the reverse jump is classified ambiguous -> refused");
        assertTrue(back.solution().centre().orElseThrow()
                        .separationDegrees(centre) > 1.0,
                "the branch the tie-break would have taken is far from"
                        + " the original - the jump the refusal prevents");
        assertTrue(ZoomStudyMain.solvesExactly(centre, out.anchor(),
                        back.target()),
                "the ambiguity is genuine: the original centre remains"
                        + " an exact solution of the reverse problem");
    }

    @Test
    void letterboxPaperSolvesExactlyAndChromeAnchorsNothing() {
        // The decision's letterbox rule at the geometry: the paper is
        // the viewport - a pointer on the paper of a letterboxed
        // window solves exactly; the chrome bands are outside the
        // viewport by construction and anchor no sky (refused at the
        // component boundary in production).
        SkyPosition centre = new SkyPosition(37.946619, 85.0);
        int paperHeight = 4712;
        var step = ZoomStudyMain.solveOn(centre, 36.0, 24.0,
                new PixelPoint(300.0, paperHeight - 100.0), paperHeight);
        assertFalse(step.solution().constrained()
                        || step.solution().pastPole()
                        || step.solution().ambiguous(),
                "a paper pointer on a letterboxed window is ordinary");
        assertTrue(ZoomStudyMain.pointerDriftOn(step, 24.0,
                        new PixelPoint(300.0, paperHeight - 100.0),
                        paperHeight) < DRIFT_TOLERANCE_PX,
                "and keeps its sky subpixel");
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
