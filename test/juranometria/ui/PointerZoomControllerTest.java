package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.ViewportMapping;
import juranometria.ui.ChartViewController.PointerZoomOutcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The production pointer-zoom transition (issue #124) against the
 * reviewed contract (docs/decisions/pointer-zoom.md): accepted steps
 * preserve the sky beneath the pointer within the reviewed pixel
 * tolerance and notify exactly once; refusals - at-bound, infeasible
 * pointer, coverage - change nothing and notify nobody; a step and
 * its reverse restore the original state within the stated
 * tolerance; and the target survives exactly when the centre
 * survives.
 */
class PointerZoomControllerTest {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    private static final double DRIFT_TOLERANCE_PX = 1e-2;
    private static final double REVERSAL_TOLERANCE_DEGREES = 1e-4;

    /** The pointer's plane point on the current page, as the UI computes it. */
    private static PlanePoint plane(ChartViewState state, PixelPoint pixel) {
        return PanSolver.planeFromPixel(new ChartViewport(
                state.centre(), state.fieldWidthDegrees(), WIDTH, HEIGHT),
                pixel);
    }

    /** Where the given sky position lands on the state's page. */
    private static PixelPoint pixelOf(ChartViewState state, SkyPosition sky) {
        ChartViewport viewport = new ChartViewport(
                state.centre(), state.fieldWidthDegrees(), WIDTH, HEIGHT);
        return new ViewportMapping(viewport).toPixel(
                new GnomonicProjection(state.centre())
                        .project(sky).orElseThrow());
    }

    private static ChartViewController controllerAt(SkyPosition centre,
                                                    double field) {
        ChartViewController controller = new ChartViewController();
        controller.recenter(centre, field);
        return controller;
    }

    @Test
    void everyAdjacentStepPreservesTheSkyBeneathThePointer() {
        List<Double> fields = ChartViewState.fieldWidthSteps();
        SkyPosition[] centres = {
                new SkyPosition(10.684708, 41.268750),
                new SkyPosition(0.3, 45.0),
                new SkyPosition(83.818667, -5.389667)};
        PixelPoint[] pointers = {
                new PixelPoint(450.0, 350.0), new PixelPoint(300.0, 200.0),
                new PixelPoint(1.0, 350.0), new PixelPoint(899.0, 699.0)};
        for (SkyPosition centre : centres) {
            for (int i = 0; i + 1 < fields.size(); i++) {
                for (boolean zoomIn : new boolean[] {true, false}) {
                    double from = zoomIn ? fields.get(i) : fields.get(i + 1);
                    for (PixelPoint pointer : pointers) {
                        ChartViewController controller =
                                controllerAt(centre, from);
                        SkyPosition anchor = PanSolver.skyFromPlane(
                                controller.state().centre(),
                                plane(controller.state(), pointer));
                        int[] notified = {0};
                        controller.onChange(state -> notified[0]++);
                        notified[0] = 0;
                        assertEquals(PointerZoomOutcome.ACCEPTED,
                                controller.zoomAt(plane(controller.state(),
                                        pointer), zoomIn));
                        assertEquals(1, notified[0],
                                "one atomic transition, one notification");
                        PixelPoint landed = pixelOf(controller.state(), anchor);
                        assertTrue(Math.hypot(landed.x() - pointer.x(),
                                        landed.y() - pointer.y())
                                        < DRIFT_TOLERANCE_PX,
                                "the sky beneath the pointer stays beneath"
                                        + " the pointer at " + centre);
                    }
                }
            }
        }
    }

    @Test
    void zoomInThenOutAtTheSamePointerRestoresTheOriginalState() {
        ChartViewController controller = controllerAt(
                new SkyPosition(83.818667, -5.389667), 18.0);
        SkyPosition origin = controller.state().centre();
        PixelPoint pointer = new PixelPoint(250.0, 500.0);
        assertEquals(PointerZoomOutcome.ACCEPTED,
                controller.zoomAt(plane(controller.state(), pointer), true));
        assertEquals(PointerZoomOutcome.ACCEPTED,
                controller.zoomAt(plane(controller.state(), pointer), false));
        assertEquals(18.0, controller.state().fieldWidthDegrees());
        assertTrue(controller.state().centre().separationDegrees(origin)
                        < REVERSAL_TOLERANCE_DEGREES,
                "the round trip restores the centre within the reviewed"
                        + " tolerance");
    }

    @Test
    void refusalsChangeNothingAndNotifyNobody() {
        // At-bound: the widest page cannot zoom out.
        ChartViewController atBound = controllerAt(
                new SkyPosition(83.818667, -5.389667), 36.0);
        ChartViewState before = atBound.state();
        int[] notified = {0};
        atBound.onChange(state -> notified[0]++);
        notified[0] = 0;
        assertEquals(PointerZoomOutcome.AT_BOUND,
                atBound.zoomAt(plane(before, new PixelPoint(300, 200)),
                        false));
        assertEquals(before, atBound.state());
        assertEquals(0, notified[0]);

        // Infeasible pointer: the gate's constrained polar case.
        ChartViewController polar = controllerAt(
                new SkyPosition(37.946619, 85.0), 24.0);
        ChartViewState polarBefore = polar.state();
        assertEquals(PointerZoomOutcome.INFEASIBLE_POINTER,
                polar.zoomAt(plane(polarBefore, new PixelPoint(1, 350)),
                        false));
        assertEquals(polarBefore, polar.state());

        // Infeasible pointer: exact forward, two-branch reverse - the
        // preflight refuses before the state can change.
        ChartViewController nearPole = controllerAt(
                new SkyPosition(37.946619, 89.9), 36.0);
        ChartViewState nearPoleBefore = nearPole.state();
        assertEquals(PointerZoomOutcome.INFEASIBLE_POINTER,
                nearPole.zoomAt(plane(nearPoleBefore,
                        new PixelPoint(450, 1)), true));
        assertEquals(nearPoleBefore, nearPole.state());

        // Coverage: a predicate that refuses the candidate leaves the
        // exact same state, target, and field behind.
        ChartViewController fenced = new ChartViewController(
                state -> state.fieldWidthDegrees() >= 12.0);
        fenced.recenter(new SkyPosition(83.818667, -5.389667), 12.0,
                "M 42 region", "NGC 1976");
        ChartViewState fencedBefore = fenced.state();
        assertEquals(PointerZoomOutcome.REFUSED_COVERAGE,
                fenced.zoomAt(plane(fencedBefore, new PixelPoint(300, 200)),
                        true));
        assertEquals(fencedBefore, fenced.state());
        assertEquals("NGC 1976", fenced.state().targetIdentity(),
                "a refused transition keeps the target untouched");
    }

    @Test
    void theTargetSurvivesExactlyWhenTheCentreSurvives() {
        // Off-centre pointer: the centre moves, so the step is an
        // anonymous recenter - target and title clear together.
        ChartViewController moved = new ChartViewController();
        moved.recenter(new SkyPosition(88.792939, 7.407064), 8.0,
                "Betelgeuse · α Ori region", "TYC 129-1873-1");
        assertEquals(PointerZoomOutcome.ACCEPTED,
                moved.zoomAt(plane(moved.state(), new PixelPoint(200, 150)),
                        false));
        assertNull(moved.state().targetIdentity(),
                "a step that moves the centre is anonymous");
        assertNull(moved.state().targetLabel());

        // The exact page centre: the solve degenerates to the
        // toolbar's centre-preserving transition and keeps the target.
        ChartViewController centred = new ChartViewController();
        centred.recenter(new SkyPosition(88.792939, 7.407064), 8.0,
                "Betelgeuse · α Ori region", "TYC 129-1873-1");
        assertEquals(PointerZoomOutcome.ACCEPTED,
                centred.zoomAt(new PlanePoint(0.0, 0.0), false));
        assertEquals("TYC 129-1873-1", centred.state().targetIdentity(),
                "the target survives exactly when the centre survives");
        assertEquals(12.0, centred.state().fieldWidthDegrees());
    }

    @Test
    void aWideSouthernCornerRefusesByTheContractNotByAccident() {
        // Crux at 36 degrees, corner pointer: the anchor sits near
        // dec -83, past the north-up feasibility bound - the reviewed
        // contract refuses constrained steps rather than miss the
        // pointer visibly. The same page zooms freely at regional
        // fields where the anchor stays feasible.
        SkyPosition crux = new SkyPosition(186.649563, -63.099093);
        ChartViewController wide = controllerAt(crux, 36.0);
        ChartViewState before = wide.state();
        assertEquals(PointerZoomOutcome.INFEASIBLE_POINTER,
                wide.zoomAt(plane(before, new PixelPoint(899.0, 699.0)),
                        true));
        assertEquals(before, wide.state());

        ChartViewController regional = controllerAt(crux, 18.0);
        assertEquals(PointerZoomOutcome.ACCEPTED,
                regional.zoomAt(plane(regional.state(),
                        new PixelPoint(899.0, 699.0)), true));
    }

    @Test
    void letterboxedPaperGeometryZoomsExactly() {
        // The paper is the viewport: a pointer's plane point computed
        // from a letterboxed paper (the projection-sanity height)
        // accepts and preserves its sky like any page. Chrome never
        // reaches the controller - the interaction layer's rule.
        SkyPosition centre = new SkyPosition(37.946619, 85.0);
        int paperHeight = 4712;
        ChartViewController controller = controllerAt(centre, 36.0);
        PixelPoint pixel = new PixelPoint(300.0, paperHeight - 100.0);
        PlanePoint pointer = PanSolver.planeFromPixel(new ChartViewport(
                centre, 36.0, WIDTH, paperHeight), pixel);
        SkyPosition anchor = PanSolver.skyFromPlane(centre, pointer);
        assertEquals(PointerZoomOutcome.ACCEPTED,
                controller.zoomAt(pointer, true));
        ChartViewport zoomed = new ChartViewport(
                controller.state().centre(),
                controller.state().fieldWidthDegrees(), WIDTH, paperHeight);
        PixelPoint landed = new ViewportMapping(zoomed).toPixel(
                new GnomonicProjection(controller.state().centre())
                        .project(anchor).orElseThrow());
        assertTrue(Math.hypot(landed.x() - pixel.x(),
                        landed.y() - pixel.y()) < DRIFT_TOLERANCE_PX,
                "letterboxed paper geometry preserves its sky");
    }

    @Test
    void toolbarZoomSemanticsAreUntouched() {
        ChartViewController controller = new ChartViewController();
        controller.recenter(new SkyPosition(10.684708, 41.268750), 8.0,
                "M31 · Andromeda Galaxy region", "NGC 224");
        controller.zoomOut();
        assertEquals("NGC 224", controller.state().targetIdentity(),
                "centre-preserving zoom keeps the target, exactly as"
                        + " released");
        assertEquals(12.0, controller.state().fieldWidthDegrees());
        assertEquals(new SkyPosition(10.684708, 41.268750),
                controller.state().centre());
    }
}
