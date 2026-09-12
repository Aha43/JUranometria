package juranometria.ui;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a drag does near the limb, held to what it measured (Sprint
 * 32, issue #330).
 *
 * <p>The measurement came first and found nothing to repair: the
 * grabbed sky stays beneath the pointer at every radius out to 0.995
 * - 2.3e-5 px at worst, against the atlas's reviewed 1e-2 - a gesture
 * that leaves the disc and returns brings the page back to a
 * billionth of a degree, and the two rungs below are untouched. What
 * it also found is that the page moves <strong>38 times faster per
 * pixel</strong> at r = 0.995 than at the centre.
 *
 * <p>That is not a defect and has deliberately not been damped. It is
 * {@code 1/cos} of the angle from the centre - the physical cost of
 * looking at a sphere from outside - and every cure costs more than
 * it buys: damping would break the promise this test opens with, the
 * sky staying under the hand; refusing ambiguous solves would make
 * everything past r = 0.75 intermittently immovable, since ambiguity
 * begins there; and a refusal band near the limb would be the invented
 * radial cutoff #301 already ruled against, over a region one or two
 * screen pixels wide.
 *
 * <p>So what is held here is the behaviour as measured. A future
 * change that damps, clamps or refuses more than "no sky under the
 * pointer" fails these.
 */
class GlobeDragQualityTest {

    private static final int WIDE_PX = 1200;

    private static final int HIGH_PX = 800;

    /** The disc as the production mapping lays it down on this page. */
    private static final double DISC_PX = 0.90 * HIGH_PX / 2.0;

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    /**
     * The atlas's own reviewed drift tolerance, not a new one.
     *
     * <p>A first draft of this test asserted the drift was exactly
     * zero, on the grounds that a projection and its inverse ought to
     * give a pixel back unchanged. They do not quite: the solve is a
     * round trip through spherical trigonometry and returns a centre
     * good to about 1e-14 degrees, which is a few hundredths of a
     * millionth of a pixel here. Raising the number until it passed
     * would have been setting a bound from an observation.
     *
     * <p>So the bound is the one the pointer-zoom gate reviewed and
     * the atlas has been held to since: <strong>1e-2 px</strong>
     * (docs/decisions/pointer-zoom.md, where the same question is
     * asked of a zoom step and answered at 5.4e-4 px). A drag is the
     * same promise about the same pixel, so it is held to the same
     * number rather than to one invented for the globe.
     *
     * <p>What the globe actually measures is 2.3e-5 px at worst,
     * inside that by three orders of magnitude - reported here so a
     * reader can see the margin rather than take the tolerance for
     * the result.
     */
    private static final double DRIFT_TOLERANCE_PX = 1.0e-2;

    @Test
    void theGrabbedSkyStaysUnderThePointerOutToTheLimb() {
        for (double radius : new double[] {0.0, 0.25, 0.5, 0.75, 0.9,
                0.95, 0.98, 0.995}) {
            for (double step : new double[] {1.0, 4.0, 16.0, 64.0}) {
                for (double bearing : new double[] {0.0, 90.0, 180.0,
                        270.0}) {
                    Dragged dragged = drag(180.0, radius, step, bearing);
                    assertTrue(dragged.moved(),
                            "a drag from r=" + radius + " by " + step
                                    + " px on bearing " + bearing
                                    + " is a gesture the atlas offers");
                    assertEquals(0.0, dragged.driftPx(), DRIFT_TOLERANCE_PX,
                            "the grabbed sky stays under the pointer at"
                                    + " r=" + radius + " after " + step
                                    + " px");
                }
            }
        }
    }

    @Test
    void anAmbiguousSolveTakesTheNearRootAndNotTheFarOne() {
        // Ambiguity is real here and begins at r = 0.75: the centre
        // equation has two roots, and both put the grabbed sky under
        // the pointer. What decides is which centre the reader was
        // already at, and the far root is on the other side of the
        // sky - a jump rather than a drag.
        int ambiguous = 0;
        for (double radius : new double[] {0.75, 0.9, 0.95, 0.98,
                0.995}) {
            for (double bearing = 0.0; bearing < 360.0; bearing += 45.0) {
                Dragged dragged = drag(180.0, radius, 4.0, bearing);
                if (!dragged.ambiguous()) {
                    continue;
                }
                ambiguous++;
                assertTrue(dragged.movedDegrees() < 90.0,
                        "an ambiguous solve at r=" + radius
                                + " took the near root: the centre"
                                + " moved " + dragged.movedDegrees()
                                + " degrees for a 4 px drag");
                assertEquals(0.0, dragged.driftPx(), DRIFT_TOLERANCE_PX,
                        "and it is a root that keeps the promise");
            }
        }
        assertTrue(ambiguous >= 20,
                "the near-limb band really is ambiguous, so this test"
                        + " is not passing by never meeting the case: "
                        + ambiguous + " ambiguous solves");
    }

    @Test
    void aFinelySampledDragTakesTheNearRootAtEveryStep() {
        // The first version of this test could not fail. It asserted
        // that the grabbed sky still projects onto the page and that
        // the centre's distance from the start does not decrease -
        // and a branch switch satisfies both. Every candidate the
        // solver verifies has already reprojected the grabbed sky
        // onto the target, so the first is true of either root; and a
        // jump further around the globe increases the distance from
        // the start, so the second is true of the far one too
        // (review of PR #338).
        //
        // What tells the roots apart is which centre the reader was
        // already at. The solver takes the verified candidate nearest
        // its previousCentre, so asking it again from the antipode of
        // that centre returns the other root - both answers, without
        // the solver needing a new way to be asked. The step is then
        // held to the near one directly.
        ChartViewState start = new ChartViewState(SAGITTARIUS, 180.0, 5.0);
        ChartViewport page = viewportFor(start);
        ChartViewController controller = new ChartViewController();
        controller.recenter(start.centre(), 180.0);

        // Grabbed where the second root is actually there to be
        // taken. Due east of the centre at r = 0.95 the solve has one
        // verified candidate throughout, so a gesture along it could
        // assert the near root was chosen while never facing a choice
        // - the first draft of this test did exactly that and passed
        // for that reason. Off the horizontal, at r = 0.75, every
        // step of this drag is two-rooted.
        PixelPoint press = new PixelPoint(
                WIDE_PX / 2.0 + 0.75 * DISC_PX * Math.cos(Math.PI / 4.0),
                HIGH_PX / 2.0 + 0.75 * DISC_PX * Math.sin(Math.PI / 4.0));
        SkyPosition grabbed = PanSolver.skyAt(page,
                PanSolver.planeFromPixel(page, press)).orElseThrow();

        int steps = 0;
        int twoRooted = 0;
        double worstJumpPastTheNearRoot = 0.0;
        for (double back = 0.5; back <= 120.0; back += 0.5) {
            SkyPosition previous = controller.state().centre();
            PlanePoint target = PanSolver.planeFromPixel(page,
                    new PixelPoint(press.x() - back, press.y()));

            // Both roots, and then *this test* decides which of them
            // is the near one. Taking the solver's answer as the
            // definition of near would be a measurement agreeing with
            // itself: a solver that took the far root would report
            // the far root as near, and the comparison would pass
            // while the page jumped.
            SkyPosition one = PanSolver.solveCentre(
                    ChartProjection.ORTHOGRAPHIC, grabbed, target,
                    previous).centre().orElse(null);
            SkyPosition other = PanSolver.solveCentre(
                    ChartProjection.ORTHOGRAPHIC, grabbed, target,
                    antipodeOf(previous)).centre().orElse(null);
            SkyPosition near = one;
            SkyPosition far = other;
            if (one != null && other != null
                    && other.separationDegrees(previous)
                            < one.separationDegrees(previous)) {
                near = other;
                far = one;
            }

            if (!controller.pan(grabbed, target)) {
                continue;
            }
            steps++;
            SkyPosition took = controller.state().centre();

            assertEquals(0.0, took.separationDegrees(near), 1.0e-9,
                    "the step took the root nearest where the reader"
                            + " already was, after " + back + " px");

            // The solver's own threshold for "two answers rather
            // than the double root's numerical twins", so this test
            // and the solver cannot disagree about what ambiguity is.
            if (far == null || far.separationDegrees(near)
                    <= PanSolver.AMBIGUITY_SEPARATION_DEGREES) {
                continue;
            }
            twoRooted++;
            // Both roots put the grabbed sky under the pointer; only
            // one of them is a drag. The other is a jump, and its
            // distance from the near root is how big a jump it would
            // have been - so the step must stay far closer to the
            // near root than the two roots are to each other.
            double toNear = took.separationDegrees(near);
            double toFar = took.separationDegrees(far);
            assertTrue(toNear < toFar,
                    "after " + back + " px the step landed on the far"
                            + " root: " + toNear + " degrees from the"
                            + " near one against " + toFar
                            + " from the far one");
            worstJumpPastTheNearRoot =
                    Math.max(worstJumpPastTheNearRoot, toNear);
        }

        assertTrue(steps > 200,
                "a finely sampled gesture, not three points: " + steps);
        assertTrue(twoRooted >= 50,
                "and one that meets the two-rooted case it is about: "
                        + twoRooted + " of " + steps + " steps had a"
                        + " second root to be wrong about");
        assertEquals(0.0, worstJumpPastTheNearRoot, 1.0e-9,
                "no step drifted off the near root at all");
    }

    /** The point opposite a centre, for asking after the other root. */
    private static SkyPosition antipodeOf(SkyPosition centre) {
        return new SkyPosition((centre.raDegrees() + 180.0) % 360.0,
                -centre.decDegrees());
    }

    @Test
    void leavingTheDiscAndComingBackReturnsThePageExactly() {
        ChartViewState start = new ChartViewState(SAGITTARIUS, 180.0, 5.0);
        ChartViewport page = viewportFor(start);
        ChartViewController controller = new ChartViewController();
        controller.recenter(start.centre(), 180.0);

        PixelPoint press = new PixelPoint(
                WIDE_PX / 2.0 + 0.4 * DISC_PX, HIGH_PX / 2.0);
        SkyPosition grabbed = PanSolver.skyAt(page,
                PanSolver.planeFromPixel(page, press)).orElseThrow();

        int refused = 0;
        List<Double> path = new ArrayList<>();
        for (double out = 0.0; out <= 260.0; out += 4.0) {
            path.add(out);
        }
        for (double back = 260.0; back >= 0.0; back -= 4.0) {
            path.add(back);
        }
        for (double at : path) {
            if (!controller.pan(grabbed, PanSolver.planeFromPixel(page,
                    new PixelPoint(press.x() + at, press.y())))) {
                refused++;
            }
        }

        assertTrue(refused > 0,
                "the hand really did leave the disc, or this proves"
                        + " nothing about coming back");
        assertEquals(0.0, start.centre()
                        .separationDegrees(controller.state().centre()),
                1.0e-9,
                "a gesture that returns to its own press returns the"
                        + " page");
        assertEquals(start.fieldWidthDegrees(),
                controller.state().fieldWidthDegrees(),
                "on the same rung");
        assertEquals(start.projection(),
                controller.state().projection(),
                "drawn by the same projection");
        assertEquals(start.limitingMagnitude(),
                controller.state().limitingMagnitude(),
                "at the same limit - the gesture moved the page and"
                        + " nothing else");
    }

    @Test
    void theRungsBelowTheGlobeAreUntouched() {
        for (double field : new double[] {42.0, 120.0}) {
            for (double radius : new double[] {0.0, 0.5, 0.9, 0.99}) {
                for (double step : new double[] {1.0, 16.0, 64.0}) {
                    Dragged dragged = drag(field, radius, step, 0.0);
                    assertTrue(dragged.moved(),
                            field + " degrees is sky to its corners, so"
                                    + " no drag there is refused");
                    assertEquals(0.0, dragged.driftPx(), DRIFT_TOLERANCE_PX,
                            field + " degrees at r=" + radius
                                    + " keeps the sky under the"
                                    + " pointer");
                    assertFalse(dragged.ambiguous(),
                            field + " degrees at r=" + radius
                                    + " is not an ambiguous solve: the"
                                    + " near-limb band belongs to the"
                                    + " globe alone");
                }
            }
        }
        assertEquals(ChartProjection.GNOMONIC,
                ChartProjection.forField(42.0));
        assertEquals(ChartProjection.STEREOGRAPHIC,
                ChartProjection.forField(120.0));
    }

    /** What one drag did, through the production path. */
    private record Dragged(boolean moved, double driftPx,
                           double movedDegrees, boolean ambiguous) {
    }

    /**
     * One drag, exactly as {@code PanInteraction.mouseDragged} makes
     * it: the sky taken at the press, the pointer's plane point on
     * the page being looked at, and the controller between them.
     */
    private static Dragged drag(double field, double radius,
                                double stepPx, double bearingDegrees) {
        ChartViewState start = new ChartViewState(SAGITTARIUS, field, 5.0);
        ChartViewport page = viewportFor(start);
        // From the state, which names its own projection and is
        // refused if it disagrees with its field - not from the
        // viewport, because only DrawnPage.of may work a projection
        // out from viewport state, and a study asking again would be
        // the fourteenth copy that rule exists to catch (#335).
        Projection projection = drawnBy(start);
        double reach = Double.isFinite(projection.visiblePlaneRadius())
                ? DISC_PX : WIDE_PX / 2.0 - 2.0;

        double bearing = Math.toRadians(bearingDegrees);
        PixelPoint press = new PixelPoint(
                WIDE_PX / 2.0 + radius * reach * Math.cos(bearing),
                HIGH_PX / 2.0 + radius * reach * Math.sin(bearing));
        SkyPosition grabbed = PanSolver.skyAt(page,
                PanSolver.planeFromPixel(page, press)).orElseThrow(
                        () -> new AssertionError("no sky at r=" + radius
                                + " on a " + field + " degree page"));

        double towardsX = WIDE_PX / 2.0 - press.x();
        double towardsY = HIGH_PX / 2.0 - press.y();
        double length = Math.hypot(towardsX, towardsY);
        PixelPoint to = length == 0.0
                ? new PixelPoint(press.x() + stepPx, press.y())
                : new PixelPoint(press.x() + stepPx * towardsX / length,
                        press.y() + stepPx * towardsY / length);

        PlanePoint target = PanSolver.planeFromPixel(page, to);
        var solution = PanSolver.solveCentre(page.projection(), grabbed,
                target, start.centre());
        ChartViewController controller = new ChartViewController();
        controller.recenter(start.centre(), field);
        if (!controller.pan(grabbed, target)) {
            return new Dragged(false, 0.0, 0.0, solution.ambiguous());
        }

        ChartViewport after = viewportFor(controller.state());
        Projection afterProjection = drawnBy(controller.state());
        PixelPoint landed = new ViewportMapping(after, afterProjection)
                .toPixel(afterProjection.project(grabbed).orElseThrow());
        return new Dragged(true,
                Math.hypot(landed.x() - to.x(), landed.y() - to.y()),
                start.centre().separationDegrees(
                        controller.state().centre()),
                solution.ambiguous());
    }

    /** The projection a state names, asked of the registry once. */
    private static Projection drawnBy(ChartViewState state) {
        return Projections.of(state.projection(), state.centre());
    }

    private static ChartViewport viewportFor(ChartViewState state) {
        return new ChartViewport(state.centre(),
                state.fieldWidthDegrees(), WIDE_PX, HIGH_PX,
                state.projection());
    }
}
