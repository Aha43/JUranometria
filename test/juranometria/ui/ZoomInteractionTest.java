package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.SwingUtilities;

import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wheel zoom through real MouseWheelEvents on the real chart
 * component (issue #125): direction, one notch one step, off-centre
 * pointer invariance, trackpad accumulation with direction-reversal
 * reset, bounds and refused pointers consuming without moving,
 * letterbox chrome left alone, and RA-wrap and polar pages following
 * the reviewed contract.
 */
class ZoomInteractionTest {

    /** A full-page all-sky fixture with real wheel events on the EDT. */
    private static final class Fixture {
        final SceneAssemblerTest.CountingCatalogue catalogue =
                new SceneAssemblerTest.CountingCatalogue();
        final ChartViewController controller;
        final ChartComponent chart;
        final ZoomInteraction interaction;

        Fixture() throws Exception {
            this(900, 700);
        }

        Fixture(int width, int height) throws Exception {
            SceneAssembler assembler = SceneAssembler.allSky(catalogue, 1.5);
            controller = new ChartViewController(assembler::fits);
            ChartComponent[] chartHolder = new ChartComponent[1];
            ZoomInteraction[] interactionHolder = new ZoomInteraction[1];
            SwingUtilities.invokeAndWait(() -> {
                chartHolder[0] = new ChartComponent(assembler);
                interactionHolder[0] = ZoomInteraction.install(
                        chartHolder[0], controller);
                controller.onChange(chartHolder[0]::setViewState);
                chartHolder[0].setSize(width, height);
            });
            flush();
            chart = chartHolder[0];
            interaction = interactionHolder[0];
        }

        MouseWheelEvent wheel(int x, int y, double rotation)
                throws Exception {
            MouseWheelEvent event = new MouseWheelEvent(chart,
                    MouseEvent.MOUSE_WHEEL, System.nanoTime() / 1_000_000,
                    0, x, y, x, y, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1,
                    (int) rotation, rotation);
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(event));
            flush();
            return event;
        }

        static void flush() throws Exception {
            SwingUtilities.invokeAndWait(() -> { });
        }
    }

    private static final double DRIFT_TOLERANCE_PX = 1e-2;

    private static SkyPosition anchorAt(ChartViewState state, int x, int y,
                                        int width, int height) {
        return PanSolver.skyFromPlane(state.centre(),
                PanSolver.planeFromPixel(new ChartViewport(state.centre(),
                        state.fieldWidthDegrees(), width, height),
                        new PixelPoint(x, y)));
    }

    private static double driftPx(ChartViewState state, SkyPosition anchor,
                                  int x, int y, int width, int height) {
        ChartViewport viewport = new ChartViewport(state.centre(),
                state.fieldWidthDegrees(), width, height);
        PixelPoint landed = new ViewportMapping(viewport).toPixel(
                new GnomonicProjection(state.centre())
                        .project(anchor).orElseThrow());
        return Math.hypot(landed.x() - x, landed.y() - y);
    }

    @Test
    void oneNotchIsOneStepAndTheSkyStaysBeneathThePointer() throws Exception {
        Fixture fixture = new Fixture();
        SkyPosition anchor = anchorAt(fixture.controller.state(),
                300, 200, 900, 700);
        MouseWheelEvent in = fixture.wheel(300, 200, -1.0);
        assertTrue(in.isConsumed(), "the chart owns its wheel on paper");
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees(),
                "rotation away from the reader zooms in one step");
        assertTrue(driftPx(fixture.controller.state(), anchor,
                        300, 200, 900, 700) < DRIFT_TOLERANCE_PX,
                "the sky beneath the pointer stays beneath the pointer");

        MouseWheelEvent out = fixture.wheel(300, 200, 1.0);
        assertTrue(out.isConsumed());
        assertEquals(8.0, fixture.controller.state().fieldWidthDegrees(),
                "rotation toward the reader zooms back out");
        assertTrue(fixture.controller.state().centre().separationDegrees(
                        ChartViewState.DEFAULT.centre()) < 1e-4,
                "the round trip at the same pointer restores the centre");
    }

    @Test
    void trackpadRotationsAccumulateAndReversalDiscardsTheRemainder()
            throws Exception {
        Fixture fixture = new Fixture();
        fixture.wheel(300, 200, -0.4);
        fixture.wheel(300, 200, -0.4);
        assertEquals(8.0, fixture.controller.state().fieldWidthDegrees(),
                "fractions below a whole step bank without zooming");
        assertEquals(-0.8, fixture.interaction.accumulatedRotation(), 1e-9);
        fixture.wheel(300, 200, -0.4);
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees(),
                "the step fires when the magnitude reaches one");
        assertEquals(-0.2, fixture.interaction.accumulatedRotation(), 1e-9,
                "the remainder is kept");

        fixture.wheel(300, 200, 0.5);
        assertEquals(0.5, fixture.interaction.accumulatedRotation(), 1e-9,
                "a direction reversal discards the opposing remainder");
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees());
    }

    @Test
    void aMultiNotchBurstAppliesEveryStepAsItsOwnHonestPage()
            throws Exception {
        Fixture fixture = new Fixture();
        fixture.wheel(450, 350, 3.0);
        assertEquals(24.0, fixture.controller.state().fieldWidthDegrees(),
                "three notches out are three discrete steps (8-12-18-24)");
        fixture.wheel(450, 350, -2.0);
        assertEquals(12.0, fixture.controller.state().fieldWidthDegrees());
    }

    @Test
    void boundsAndRefusedPointersConsumeWithoutMoving() throws Exception {
        Fixture fixture = new Fixture();
        // Out to the widest page, then one more: consumed, unchanged.
        fixture.wheel(450, 350, 5.0);
        assertEquals(36.0, fixture.controller.state().fieldWidthDegrees());
        ChartViewState atBound = fixture.controller.state();
        MouseWheelEvent beyond = fixture.wheel(450, 350, 1.0);
        assertTrue(beyond.isConsumed(),
                "the wheel is consumed at the sequence end");
        assertEquals(atBound, fixture.controller.state(),
                "and the state does not move");

        // A polar page whose corner pointer the contract refuses:
        // consumed, unchanged, honestly inert.
        SwingUtilities.invokeAndWait(() -> fixture.controller.recenter(
                new SkyPosition(37.946619, 85.0), 24.0));
        Fixture.flush();
        ChartViewState polar = fixture.controller.state();
        MouseWheelEvent refused = fixture.wheel(1, 350, 1.0);
        assertTrue(refused.isConsumed());
        assertEquals(polar, fixture.controller.state(),
                "an infeasible pointer refuses without movement");
    }

    @Test
    void letterboxChromeIsLeftAlone() throws Exception {
        // A window taller than the widest page's projection-sanity
        // cap: the chart letterboxes and the chrome bands anchor no
        // sky - unconsumed, unmoved.
        Fixture fixture = new Fixture(900, 4800);
        SwingUtilities.invokeAndWait(() -> fixture.controller.recenter(
                fixture.controller.state().centre(), 36.0));
        Fixture.flush();
        int offset = fixture.chart.pageOffsetY();
        assertTrue(offset > 0, "the tall window letterboxes at 36 degrees");
        ChartViewState before = fixture.controller.state();
        MouseWheelEvent chrome = fixture.wheel(450, offset / 2, 1.0);
        assertFalse(chrome.isConsumed(),
                "chrome events are left alone for the platform");
        assertEquals(before, fixture.controller.state());

        // Near the paper's top the anchor would reach past the pole -
        // the contract refuses there - so the paper case zooms from a
        // mid-paper pointer, where the letterboxed page is ordinary.
        MouseWheelEvent paper = fixture.wheel(450,
                fixture.chart.getHeight() / 2 + 60, -1.0);
        assertTrue(paper.isConsumed(), "the paper still zooms");
        assertEquals(24.0, fixture.controller.state().fieldWidthDegrees());
    }

    @Test
    void raWrapPagesZoomLikeAnyOther() throws Exception {
        Fixture fixture = new Fixture();
        SwingUtilities.invokeAndWait(() -> fixture.controller.recenter(
                new SkyPosition(0.3, 45.0), 12.0));
        Fixture.flush();
        SkyPosition anchor = anchorAt(fixture.controller.state(),
                150, 500, 900, 700);
        fixture.wheel(150, 500, -1.0);
        assertEquals(8.0, fixture.controller.state().fieldWidthDegrees());
        assertTrue(driftPx(fixture.controller.state(), anchor,
                        150, 500, 900, 700) < DRIFT_TOLERANCE_PX,
                "the wrap page keeps its sky beneath the pointer");
    }

    @Test
    void wheelAndToolbarPathsAgreeOnTargetHonesty() throws Exception {
        Fixture fixture = new Fixture();
        SwingUtilities.invokeAndWait(() -> fixture.controller.recenter(
                new SkyPosition(10.684708, 41.268750), 8.0,
                "M31 · Andromeda Galaxy region", "NGC 224"));
        Fixture.flush();
        // Toolbar path: centre-preserving, target kept.
        SwingUtilities.invokeAndWait(fixture.controller::zoomOut);
        Fixture.flush();
        assertEquals("NGC 224", fixture.controller.state().targetIdentity());
        // Wheel path off-centre: the centre moves, so the step is an
        // anonymous recenter - target and title clear together.
        fixture.wheel(200, 150, 1.0);
        assertEquals(18.0, fixture.controller.state().fieldWidthDegrees());
        assertEquals(null, fixture.controller.state().targetIdentity(),
                "a wheel step that moves the centre is anonymous");
    }
}
