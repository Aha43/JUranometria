package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.ChartProjection;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PanSolver;
import juranometria.project.Projections;
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

    /**
     * Where the given sky position lands on the state's page.
     *
     * <p>Through the page's own projection. This asked a tangent
     * plane whatever page it was measuring until #299 put rungs on
     * the ladder that another one draws, at which point it began
     * reporting that pointer zoom had lost the sky when what it had
     * lost was the projection - the same fault, in a test, that the
     * projection strategy was built to make impossible in
     * production.
     */
    private static PixelPoint pixelOf(ChartViewState state, SkyPosition sky) {
        ChartViewport viewport = new ChartViewport(
                state.centre(), state.fieldWidthDegrees(), WIDTH, HEIGHT);
        return new ViewportMapping(viewport, juranometria.project.Projections.of(viewport.projection(), viewport.centre())).toPixel(
                Projections.forViewport(viewport).project(sky).orElseThrow());
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
                                controller.state().projection(),
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
        // At-bound: the widest page cannot zoom out. That is the
        // widest overview rung now, not the sheet page - the ladder
        // did not stop at 42 degrees once a projection arrived that
        // could carry further (#299).
        ChartViewController atBound = controllerAt(
                new SkyPosition(83.818667, -5.389667), 120.0);
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
        int[] polarNotified = {0};
        polar.onChange(state -> polarNotified[0]++);
        polarNotified[0] = 0;
        assertEquals(PointerZoomOutcome.INFEASIBLE_POINTER,
                polar.zoomAt(plane(polarBefore, new PixelPoint(1, 350)),
                        false));
        assertEquals(polarBefore, polar.state());
        assertEquals(0, polarNotified[0],
                "a constrained refusal notifies nobody");

        // Infeasible pointer: exact forward, two-branch reverse - the
        // preflight refuses before the state can change.
        ChartViewController nearPole = controllerAt(
                new SkyPosition(37.946619, 89.9), 36.0);
        ChartViewState nearPoleBefore = nearPole.state();
        int[] nearPoleNotified = {0};
        nearPole.onChange(state -> nearPoleNotified[0]++);
        nearPoleNotified[0] = 0;
        assertEquals(PointerZoomOutcome.INFEASIBLE_POINTER,
                nearPole.zoomAt(plane(nearPoleBefore,
                        new PixelPoint(450, 1)), true));
        assertEquals(nearPoleBefore, nearPole.state());
        assertEquals(0, nearPoleNotified[0],
                "a preflight refusal notifies nobody");

        // Coverage: a predicate that refuses the candidate leaves the
        // exact same state, target, and field behind.
        ChartViewController fenced = new ChartViewController(
                state -> state.fieldWidthDegrees() >= 12.0);
        fenced.recenter(new SkyPosition(83.818667, -5.389667), 12.0,
                "M 42 region", "NGC 1976");
        ChartViewState fencedBefore = fenced.state();
        int[] fencedNotified = {0};
        fenced.onChange(state -> fencedNotified[0]++);
        fencedNotified[0] = 0;
        assertEquals(PointerZoomOutcome.REFUSED_COVERAGE,
                fenced.zoomAt(plane(fencedBefore, new PixelPoint(300, 200)),
                        true));
        assertEquals(fencedBefore, fenced.state());
        assertEquals("NGC 1976", fenced.state().targetIdentity(),
                "a refused transition keeps the target untouched");
        assertEquals(0, fencedNotified[0],
                "a coverage refusal notifies nobody");
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
    void aTwoBranchStepIsRefusedWhenItsCentreIsAJumpRatherThanAZoom() {
        // The rule that replaced the blanket ambiguity refusal
        // (#299): a two-branch solve is accepted only when the centre
        // it returns is no further from the previous one than the
        // anchor is. That is a bound on the length of the step, which
        // is what separates a zoom from a jump - the direction cannot
        // separate them, because every candidate the solver returns
        // has been verified to put the anchor exactly where the
        // target asks.
        //
        // Both sides are held here, because a predicate that refused
        // everything and a predicate that refused nothing would each
        // pass half of it.
        // A page over Crux at 60 degrees, zooming out with a pointer
        // low on the paper: the solve is exact, unconstrained, and
        // its reverse restores the centre - every other condition of
        // the acceptance contract passes - and the root it returns is
        // further from the previous centre than the anchor is. This
        // step is refused by the branch rule and by nothing else,
        // which is what makes it a check of the rule.
        ChartViewController jump = controllerAt(
                new SkyPosition(186.649563, -63.099093), 60.0);
        ChartViewState before = jump.state();
        assertEquals(PointerZoomOutcome.INFEASIBLE_POINTER,
                jump.zoomAt(plane(before, new PixelPoint(597.0, 581.0)),
                        false),
                "a root the step could not have reached is refused");
        assertEquals(before, jump.state(), "and nothing moved");

        // And an ordinary two-branch step - a corner of the widest
        // overview page, where the second root is on the far side of
        // the sky and the nearest one is a few degrees away - is
        // taken, which is the whole reason the rule changed.
        ChartViewController wide = controllerAt(
                new SkyPosition(83.818667, -5.389667), 120.0);
        SkyPosition anchor = PanSolver.skyFromPlane(
                wide.state().projection(), wide.state().centre(),
                plane(wide.state(), new PixelPoint(899.0, 699.0)));
        assertTrue(anchor.separationDegrees(wide.state().centre()) > 60.0,
                "the corner of this page anchors sky a long way out: "
                        + anchor.separationDegrees(wide.state().centre()));
        assertEquals(PointerZoomOutcome.ACCEPTED,
                wide.zoomAt(plane(wide.state(),
                        new PixelPoint(899.0, 699.0)), true));
        assertTrue(wide.state().centre().separationDegrees(
                        new SkyPosition(83.818667, -5.389667))
                        <= anchor.separationDegrees(
                                new SkyPosition(83.818667, -5.389667)),
                "and the step it took is no longer than the anchor's"
                        + " own offset, which is the rule");
    }

    @Test
    void aWideSouthernCornerRefusesByTheContractNotByAccident() {
        // A near-polar page whose corner pointer anchors sky past
        // the north-up feasibility bound: the reviewed contract
        // refuses constrained steps rather than miss the pointer
        // visibly. The same page zooms freely where the anchor stays
        // feasible.
        //
        // The Crux page this used to hold is no longer one of these.
        // It refused because its reverse solve was *ambiguous* - a
        // second exact root existed - rather than because anything
        // was constrained, and #299 replaced the blanket ambiguity
        // refusal with the branch test it was standing in for. The
        // step is exact, reversible to 1e-14 degrees, and taken now.
        // The comment here said "constrained"; the code was refusing
        // for a different reason, and only the overview's fields made
        // the difference visible.
        SkyPosition nearPole = new SkyPosition(0.0, 89.9);
        ChartViewController wide = controllerAt(nearPole, 36.0);
        ChartViewState before = wide.state();
        assertEquals(PointerZoomOutcome.INFEASIBLE_POINTER,
                wide.zoomAt(plane(before, new PixelPoint(899.0, 1.0)),
                        true));
        assertEquals(before, wide.state());

        SkyPosition crux = new SkyPosition(186.649563, -63.099093);
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
        SkyPosition anchor = PanSolver.skyFromPlane(ChartProjection.GNOMONIC, centre, pointer);
        assertEquals(PointerZoomOutcome.ACCEPTED,
                controller.zoomAt(pointer, true));
        ChartViewport zoomed = new ChartViewport(
                controller.state().centre(),
                controller.state().fieldWidthDegrees(), WIDTH, paperHeight);
        PixelPoint landed = new ViewportMapping(zoomed, juranometria.project.Projections.of(zoomed.projection(), zoomed.centre())).toPixel(
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
