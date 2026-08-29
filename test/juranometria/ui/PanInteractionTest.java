package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanInteractionTest {

    /** A full-page all-sky fixture with real mouse events on the EDT. */
    private static final class Fixture {
        final SceneAssemblerTest.CountingCatalogue catalogue =
                new SceneAssemblerTest.CountingCatalogue();
        final ChartViewController controller;
        final ChartComponent chart;
        final PanInteraction interaction;

        Fixture() throws Exception {
            this(900, 700);
        }

        Fixture(int width, int height) throws Exception {
            SceneAssembler assembler = SceneAssembler.allSky(catalogue, 1.5);
            controller = new ChartViewController(assembler::fits);
            ChartComponent[] chartHolder = new ChartComponent[1];
            PanInteraction[] interactionHolder = new PanInteraction[1];
            SwingUtilities.invokeAndWait(() -> {
                chartHolder[0] = new ChartComponent(assembler);
                interactionHolder[0] = PanInteraction.install(
                        chartHolder[0], controller);
                controller.onChange(chartHolder[0]::setViewState);
                chartHolder[0].setSize(width, height);
            });
            flush();
            chart = chartHolder[0];
            interaction = interactionHolder[0];
        }

        void press(int x, int y) throws Exception {
            dispatch(MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON1);
        }

        void drag(int x, int y) throws Exception {
            dispatch(MouseEvent.MOUSE_DRAGGED, x, y, MouseEvent.BUTTON1);
        }

        void release(int x, int y) throws Exception {
            dispatch(MouseEvent.MOUSE_RELEASED, x, y, MouseEvent.BUTTON1);
        }

        void move(int x, int y) throws Exception {
            dispatch(MouseEvent.MOUSE_MOVED, x, y, MouseEvent.NOBUTTON);
        }

        private void dispatch(int id, int x, int y, int button)
                throws Exception {
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                    new MouseEvent(chart, id, System.nanoTime() / 1_000_000,
                            button == MouseEvent.BUTTON1
                                    ? MouseEvent.BUTTON1_DOWN_MASK : 0,
                            x, y, 1, false, button)));
            flush();
        }

        static void flush() throws Exception {
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> { });
        }
    }

    /** The page pixel now showing the given sky position. */
    private static PixelPoint pixelOf(ChartComponent chart, SkyPosition sky) {
        var viewport = chart.scene().viewport();
        return new ViewportMapping(viewport).toPixel(
                new GnomonicProjection(viewport.centre())
                        .project(sky).orElseThrow());
    }

    @Test
    void theGrabbedSkyFollowsThePointerAcrossTheReleasedFields() throws Exception {
        for (double field : new double[] {8.0, 18.0, 36.0}) {
            Fixture fixture = new Fixture();
            while (fixture.controller.state().fieldWidthDegrees() < field) {
                SwingUtilities.invokeAndWait(fixture.controller::zoomOut);
            }
            Fixture.flush();
            SkyPosition grabbed = PanSolver.skyFromPlane(
                    fixture.controller.state().centre(),
                    PanSolver.planeFromPixel(fixture.chart.scene().viewport(),
                            new PixelPoint(450, 350)));

            fixture.press(450, 350);
            fixture.drag(560, 280);
            fixture.release(560, 280);

            PixelPoint now = pixelOf(fixture.chart, grabbed);
            assertTrue(Math.hypot(now.x() - 560, now.y() - 280) < 1e-3,
                    "the grabbed sky sits under the pointer at " + field);
        }
    }

    @Test
    void subThresholdMovementIsAClickNotAPan() throws Exception {
        Fixture fixture = new Fixture();
        ChartViewState before = fixture.controller.state();
        int queries = fixture.catalogue.starQueries;

        fixture.press(450, 350);
        fixture.drag(452, 351);
        fixture.release(452, 351);

        assertSame(before, fixture.controller.state(),
                "jitter below the threshold changes nothing");
        assertEquals(queries, fixture.catalogue.starQueries,
                "jitter assembles no scene");
        assertFalse(fixture.interaction.dragging());
    }

    @Test
    void aRealDragDepartsTheSearchedTargetHonestly() throws Exception {
        Fixture fixture = new Fixture();
        SwingUtilities.invokeAndWait(() -> fixture.controller.recenter(
                new SkyPosition(83.818667, -5.389667),
                "M 42 · Great Orion Nebula region", "NGC 1976"));
        Fixture.flush();

        fixture.press(450, 350);
        fixture.drag(500, 390);
        fixture.release(500, 390);

        assertEquals(null, fixture.controller.state().targetLabel());
        assertEquals(null, fixture.controller.state().targetIdentity());
        assertTrue(fixture.chart.scene().title().contains("h "),
                "the panned page titles by coordinates: "
                        + fixture.chart.scene().title());
        assertEquals(8.0, fixture.controller.state().fieldWidthDegrees());
        assertEquals(8.0, fixture.controller.state().limitingMagnitude());
    }

    @Test
    void panningFeelsLikePaperAcrossTheRaWrap() throws Exception {
        Fixture fixture = new Fixture();
        SwingUtilities.invokeAndWait(() -> fixture.controller.recenter(
                new SkyPosition(0.2, -30.0)));
        Fixture.flush();

        fixture.press(450, 350);
        fixture.drag(360, 350); // pull the sky westward across RA 0
        fixture.release(360, 350);

        double ra = fixture.controller.state().centre().raDegrees();
        assertTrue(ra > 300.0 && ra < 360.0,
                "the centre crossed RA 0 continuously westward: " + ra);
        assertEquals(-30.0, fixture.controller.state().centre().decDegrees(),
                0.2, "a horizontal drag keeps the declination");
    }

    @Test
    void letterboxChromeAndSecondaryButtonsNeverPan() throws Exception {
        // A 400-wide window taller than the honest page: rows above and
        // below the paper are letterbox chrome.
        SceneAssemblerTest.CountingCatalogue catalogue =
                new SceneAssemblerTest.CountingCatalogue();
        SceneAssembler regional = new SceneAssembler(catalogue,
                new SkyPosition(10.684708, 41.268750), 10.0, 1.5);
        ChartViewController controller = new ChartViewController(regional::fits);
        ChartComponent[] holder = new ChartComponent[1];
        PanInteraction[] interaction = new PanInteraction[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(regional);
            interaction[0] = PanInteraction.install(holder[0], controller);
            controller.onChange(holder[0]::setViewState);
            holder[0].setSize(400, 1000);
        });
        Fixture.flush();
        assertTrue(holder[0].pageOffsetY() > 4,
                "premise: the tall window letterboxes the page");
        ChartViewState before = controller.state();

        // Press in the letterbox above the paper.
        SwingUtilities.invokeAndWait(() -> holder[0].dispatchEvent(new MouseEvent(
                holder[0], MouseEvent.MOUSE_PRESSED, 1, 0, 200, 2, 1, false,
                MouseEvent.BUTTON1)));
        SwingUtilities.invokeAndWait(() -> holder[0].dispatchEvent(new MouseEvent(
                holder[0], MouseEvent.MOUSE_DRAGGED, 2,
                MouseEvent.BUTTON1_DOWN_MASK, 200, 60, 1, false,
                MouseEvent.BUTTON1)));
        Fixture.flush();
        assertSame(before, controller.state(), "letterbox chrome is not sky");

        // A secondary-button press on the paper.
        SwingUtilities.invokeAndWait(() -> holder[0].dispatchEvent(new MouseEvent(
                holder[0], MouseEvent.MOUSE_PRESSED, 3, 0, 200,
                holder[0].pageOffsetY() + 50, 1, false, MouseEvent.BUTTON3)));
        SwingUtilities.invokeAndWait(() -> holder[0].dispatchEvent(new MouseEvent(
                holder[0], MouseEvent.MOUSE_DRAGGED, 4,
                MouseEvent.BUTTON3_DOWN_MASK, 260,
                holder[0].pageOffsetY() + 90, 1, false, MouseEvent.BUTTON3)));
        Fixture.flush();
        assertSame(before, controller.state(), "secondary buttons never pan");
    }

    @Test
    void cursorsFollowTheGestureAndNoDragStateSticks() throws Exception {
        Fixture fixture = new Fixture();
        fixture.move(450, 350);
        Cursor openHand = fixture.chart.getCursor();
        assertNotEquals(Cursor.getDefaultCursor(), openHand,
                "the paper offers the open hand");

        fixture.press(450, 350);
        fixture.drag(520, 400);
        assertTrue(fixture.interaction.dragging());
        assertNotEquals(openHand, fixture.chart.getCursor(),
                "a live gesture closes the hand");

        // Release outside the component: the gesture ends everywhere.
        fixture.release(-40, -40);
        assertFalse(fixture.interaction.dragging(), "no stuck drag state");
        ChartViewState after = fixture.controller.state();
        fixture.move(400, 300);
        assertEquals(openHand, fixture.chart.getCursor(),
                "the open hand returns after release");
        fixture.drag(500, 350);
        assertSame(after, fixture.controller.state(),
                "motion without a press never pans");
    }

    @Test
    void offCentreGrabsPanThePolarPageFreelyThroughTheMouse() throws Exception {
        Fixture fixture = new Fixture();
        SwingUtilities.invokeAndWait(() -> fixture.controller.recenter(
                new SkyPosition(37.946619, 89.264135)));
        Fixture.flush();
        SkyPosition grabbed = PanSolver.skyFromPlane(
                fixture.controller.state().centre(),
                PanSolver.planeFromPixel(fixture.chart.scene().viewport(),
                        new PixelPoint(200, 550)));

        fixture.press(200, 550);
        fixture.drag(400, 470); // a genuine diagonal polar drag
        fixture.release(400, 470);

        PixelPoint now = pixelOf(fixture.chart, grabbed);
        assertTrue(Math.hypot(now.x() - 400, now.y() - 470) < 1e-3,
                "an off-centre diagonal polar drag tracks the pointer exactly");
    }

    @Test
    void aPinnedPolarGrabFollowsAsFarAsGeometryAllowsWithoutWaste()
            throws Exception {
        Fixture fixture = new Fixture();
        SwingUtilities.invokeAndWait(() -> fixture.controller.recenter(
                new SkyPosition(37.946619, 89.264135)));
        Fixture.flush();
        SkyPosition before = fixture.controller.state().centre();
        var viewport = fixture.chart.scene().viewport();
        SkyPosition grabbed = PanSolver.skyFromPlane(before,
                PanSolver.planeFromPixel(viewport, new PixelPoint(450, 350)));

        // A diagonal pull on the near-polar grab: the horizontal
        // component clamps to the feasibility boundary while the nonzero
        // vertical component tracks exactly - a real two-axis event.
        fixture.press(450, 350);
        fixture.drag(650, 310);
        assertTrue(fixture.controller.state().centre()
                        .separationDegrees(before) > 0.0,
                "the sky follows the hand partway - never frozen");
        var achieved = new GnomonicProjection(
                fixture.controller.state().centre())
                .project(grabbed).orElseThrow();
        var requested = PanSolver.planeFromPixel(viewport,
                new PixelPoint(650, 310));
        assertTrue(Math.abs(requested.etaNorth()) > 0.0,
                "premise: the drag has a nonzero vertical component");
        assertEquals(requested.etaNorth(), achieved.etaNorth(), 1e-6,
                "the vertical component tracks exactly");
        assertTrue(Math.abs(achieved.xiEast())
                        < Math.abs(requested.xiEast()),
                "the horizontal component stops at the feasibility boundary");
        // The solver itself classifies the event as a constrained follow.
        assertTrue(PanSolver.solveCentre(grabbed, requested, before)
                        .constrained(),
                "the solver classifies the diagonal event as constrained");

        // For a grab this close to the pole the feasibility boundary
        // runs hard against the pole: the constrained follow carries the
        // centre deep into the polar cap (exactly to the pole when the
        // vertical component is zero).
        assertTrue(fixture.controller.state().centre().decDegrees() > 89.0,
                "the boundary centre presses against the polar cap: "
                        + fixture.controller.state().centre());

        // Pulling further along the pinned axis is saturated: the solver
        // returns the identical boundary centre (present, not past-pole),
        // so the controller refuses the no-op and nothing reassembles.
        int queries = fixture.catalogue.starQueries;
        SkyPosition saturatedFrom = fixture.controller.state().centre();
        var saturatedTarget = PanSolver.planeFromPixel(viewport,
                new PixelPoint(850, 310));
        var saturated = PanSolver.solveCentre(grabbed, saturatedTarget,
                saturatedFrom);
        assertFalse(saturated.pastPole(),
                "saturation is not a past-pole hold");
        assertEquals(saturatedFrom, saturated.centre().orElseThrow(),
                "the saturated solve lands on the identical centre");
        fixture.drag(850, 310);
        assertEquals(queries, fixture.catalogue.starQueries,
                "a saturated event assembles no new scene");

        // From the pole, pulling the grab downward would cross it - the
        // solver classifies past-pole - while upward resumes the follow.
        var heldTarget = PanSolver.planeFromPixel(viewport,
                new PixelPoint(850, 420));
        assertTrue(PanSolver.solveCentre(grabbed, heldTarget,
                        fixture.controller.state().centre()).pastPole(),
                "the solver classifies the downward pull as past-pole");
        fixture.drag(850, 420);
        assertEquals(queries, fixture.catalogue.starQueries,
                "a past-pole event within the gesture holds");
        fixture.drag(850, 280);
        assertTrue(fixture.catalogue.starQueries > queries,
                "upward movement resumes panning within the gesture");
        fixture.release(850, 280);
        assertFalse(fixture.interaction.dragging());
    }

    @Test
    void aPastPoleHoldChangesNothingAndLeavesNoStuckState() throws Exception {
        // The gate's genuine past-pole geometry, through the mouse: the
        // near-south-pole grab pulled far upward at the 36-degree field
        // has no valid centre anywhere.
        Fixture fixture = new Fixture();
        SwingUtilities.invokeAndWait(() -> {
            fixture.controller.recenter(new SkyPosition(80.893750, -85.0));
            while (fixture.controller.state().fieldWidthDegrees() < 36.0) {
                fixture.controller.zoomOut();
            }
        });
        Fixture.flush();

        var viewport = fixture.chart.scene().viewport();
        SkyPosition grabbed = PanSolver.skyFromPlane(
                fixture.controller.state().centre(),
                PanSolver.planeFromPixel(viewport, new PixelPoint(450, 350)));
        fixture.press(450, 350);
        fixture.drag(450, 340); // arm the gesture past the threshold
        ChartViewState afterArm = fixture.controller.state();
        int armedQueries = fixture.catalogue.starQueries;
        // The solver itself classifies this exact event as past-pole -
        // the no-op is a hold, not saturation or a coverage refusal.
        assertTrue(PanSolver.solveCentre(grabbed,
                        PanSolver.planeFromPixel(viewport,
                                new PixelPoint(450, 170)),
                        afterArm.centre()).pastPole(),
                "the held event carries the solver's past-pole evidence");
        fixture.drag(450, 170);
        assertSame(afterArm, fixture.controller.state(),
                "a past-pole event holds the previous centre");
        assertEquals(armedQueries, fixture.catalogue.starQueries,
                "a held event assembles no scene");
        assertTrue(fixture.interaction.dragging(),
                "the gesture stays live through the hold");

        // Pulling back inside the feasible range resumes the follow.
        fixture.drag(450, 330);
        assertTrue(fixture.catalogue.starQueries > armedQueries,
                "the gesture resumes when the pointer returns");
        fixture.release(450, 330);
        assertFalse(fixture.interaction.dragging(), "no stuck drag state");
        fixture.move(450, 330);
        assertNotEquals(Cursor.getDefaultCursor(), fixture.chart.getCursor(),
                "the open hand returns after the held gesture");
    }

    @Test
    void zoomMagnitudeAndHomeKeepWorkingAfterADrag() throws Exception {
        Fixture fixture = new Fixture();
        fixture.press(450, 350);
        fixture.drag(600, 250);
        fixture.release(600, 250);
        SkyPosition panned = fixture.controller.state().centre();

        SwingUtilities.invokeAndWait(() -> {
            fixture.controller.zoomOut();
            fixture.controller.decreaseMagnitudeLimit();
        });
        Fixture.flush();
        assertEquals(panned, fixture.controller.state().centre());
        assertEquals(12.0, fixture.controller.state().fieldWidthDegrees());
        assertEquals(7.0, fixture.controller.state().limitingMagnitude());

        SwingUtilities.invokeAndWait(fixture.controller::reset);
        Fixture.flush();
        assertEquals(ChartViewState.DEFAULT, fixture.controller.state(),
                "Home after a drag restores the exact released default");
    }
}
