package juranometria.ui;

import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Constrained pointer dragging through the production drag (#691, ruled
 * C-1 to C-5 on #690): real mouse events on a real chart, each mode a
 * statement about which screen pixel the grabbed sky stands under.
 */
class PointerDragInteractionTest {

    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static final class Fixture {
        final SceneAssemblerTest.CountingCatalogue catalogue =
                new SceneAssemblerTest.CountingCatalogue();
        final ChartViewController controller;
        final ChartComponent chart;
        final PanInteraction interaction;
        final PointerDrag drag = new PointerDrag();

        Fixture() throws Exception {
            SceneAssembler assembler = SceneAssembler.allSky(catalogue, 1.5);
            controller = new ChartViewController(assembler::fits);
            ChartComponent[] chartHolder = new ChartComponent[1];
            PanInteraction[] interactionHolder = new PanInteraction[1];
            SwingUtilities.invokeAndWait(() -> {
                chartHolder[0] = new ChartComponent(assembler, ENGLISH);
                interactionHolder[0] = PanInteraction.install(chartHolder[0], controller, drag);
                controller.onChange(chartHolder[0]::setViewState);
                chartHolder[0].setSize(900, 700);
            });
            flush();
            chart = chartHolder[0];
            interaction = interactionHolder[0];
        }

        void press(int x, int y, boolean shift) throws Exception {
            dispatch(MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON1, shift);
        }

        void drag(int x, int y, boolean shift) throws Exception {
            dispatch(MouseEvent.MOUSE_DRAGGED, x, y, MouseEvent.BUTTON1, shift);
        }

        void release(int x, int y) throws Exception {
            dispatch(MouseEvent.MOUSE_RELEASED, x, y, MouseEvent.BUTTON1, false);
        }

        void move(int x, int y) throws Exception {
            dispatch(MouseEvent.MOUSE_MOVED, x, y, MouseEvent.NOBUTTON, false);
        }

        void choose(PointerDrag.Mode mode) throws Exception {
            SwingUtilities.invokeAndWait(() -> drag.choose(mode));
            flush();
        }

        SkyPosition skyAt(int x, int y) {
            return PanSolver.skyFromPlane(controller.state().projection(),
                    controller.state().centre(),
                    PanSolver.planeFromPixel(chart.scene().viewport(), new PixelPoint(x, y)));
        }

        PixelPoint pixelOf(SkyPosition sky) {
            var viewport = chart.scene().viewport();
            var projection = juranometria.project.Projections.of(viewport.projection(),
                    viewport.centre());
            return new ViewportMapping(viewport, projection)
                    .toPixel(projection.project(sky).orElseThrow());
        }

        private void dispatch(int id, int x, int y, int button, boolean shift) throws Exception {
            int mods = (button == MouseEvent.BUTTON1 ? InputEvent.BUTTON1_DOWN_MASK : 0)
                    | (shift ? InputEvent.SHIFT_DOWN_MASK : 0);
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                    new MouseEvent(chart, id, System.nanoTime() / 1_000_000, mods,
                            x, y, 1, false, button)));
            flush();
        }

        static void flush() throws Exception {
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> { });
        }
    }

    private static void assertAt(PixelPoint expected, PixelPoint actual, String why) {
        assertTrue(Math.hypot(expected.x() - actual.x(), expected.y() - actual.y()) < 1e-3,
                why + ": expected " + expected + ", was " + actual);
    }

    @Test
    void everyLaunchStartsFreeAndAFreeDragIsTheReleasedDrag() throws Exception {
        Fixture f = new Fixture();
        assertEquals(PointerDrag.Mode.FREE, f.drag.mode());
        SkyPosition grabbed = f.skyAt(450, 350);
        f.press(450, 350, false);
        f.drag(520, 330, false);
        f.drag(610, 395, false);
        f.release(610, 395);
        assertAt(new PixelPoint(610, 395), f.pixelOf(grabbed), "under the pointer");
        assertEquals("", f.chart.pointerNote(), "free says nothing");
    }

    @Test
    void horizontalOnlyKeepsTheGrabbedSkyOnThePressRow() throws Exception {
        Fixture f = new Fixture();
        f.choose(PointerDrag.Mode.HORIZONTAL);
        SkyPosition grabbed = f.skyAt(450, 350);
        f.press(450, 350, false);
        f.drag(520, 362, false);
        f.drag(610, 395, false);
        f.release(610, 395);
        assertAt(new PixelPoint(610, 350), f.pixelOf(grabbed), "the pointer's column, the press row");
    }

    @Test
    void verticalOnlyKeepsTheGrabbedSkyOnThePressColumn() throws Exception {
        Fixture f = new Fixture();
        f.choose(PointerDrag.Mode.VERTICAL);
        SkyPosition grabbed = f.skyAt(450, 350);
        f.press(450, 350, false);
        f.drag(463, 300, false);
        f.drag(480, 200, false);
        f.release(480, 200);
        assertAt(new PixelPoint(450, 200), f.pixelOf(grabbed), "the press column, the pointer's row");
    }

    @Test
    void lockedNeverPansAndOffersNoHand() throws Exception {
        Fixture f = new Fixture();
        f.choose(PointerDrag.Mode.LOCKED);
        SkyPosition before = f.controller.state().centre();
        f.move(450, 350);
        assertEquals(Cursor.getDefaultCursor(), f.chart.getCursor(), "no grab offered");
        f.press(450, 350, false);
        f.drag(560, 420, false);
        assertFalse(f.interaction.dragging());
        f.release(560, 420);
        assertEquals(before, f.controller.state().centre(), "the chart did not move");
    }

    @Test
    void shiftHeldAtThePressKeepsAFreeDragToItsFirstAxisForThatDragOnly() throws Exception {
        Fixture f = new Fixture();
        SkyPosition grabbed = f.skyAt(450, 350);
        f.press(450, 350, true);
        f.drag(457, 353, true);           // first real movement: across
        f.drag(560, 430, false);          // Shift let go, the drag turns: still across
        f.release(560, 430);
        assertAt(new PixelPoint(560, 350), f.pixelOf(grabbed), "kept to the press row");
        assertEquals(PointerDrag.Mode.FREE, f.drag.mode(), "no mode left behind");

        SkyPosition next = f.skyAt(450, 350);
        f.press(450, 350, false);
        f.drag(530, 420, false);
        f.release(530, 420);
        assertAt(new PixelPoint(530, 420), f.pixelOf(next), "the next drag is free");
    }

    @Test
    void shiftHeldAtThePressOfAnUpwardDragKeepsItVertical() throws Exception {
        Fixture f = new Fixture();
        SkyPosition grabbed = f.skyAt(450, 350);
        f.press(450, 350, true);
        f.drag(452, 344, true);
        f.drag(520, 220, true);
        f.release(520, 220);
        assertAt(new PixelPoint(450, 220), f.pixelOf(grabbed), "kept to the press column");
    }

    @Test
    void shiftAddsNothingToAConstrainedMode() throws Exception {
        Fixture f = new Fixture();
        f.choose(PointerDrag.Mode.VERTICAL);
        SkyPosition grabbed = f.skyAt(450, 350);
        f.press(450, 350, true);
        f.drag(470, 352, true);           // across first: still vertical
        f.drag(600, 300, true);
        f.release(600, 300);
        assertAt(new PixelPoint(450, 300), f.pixelOf(grabbed), "vertical stays vertical");
    }

    @Test
    void aModeChosenMidDragWaitsForTheNextPress() throws Exception {
        Fixture f = new Fixture();
        SkyPosition grabbed = f.skyAt(450, 350);
        f.press(450, 350, false);
        f.drag(480, 360, false);
        f.choose(PointerDrag.Mode.HORIZONTAL);
        f.drag(560, 410, false);
        f.release(560, 410);
        assertAt(new PixelPoint(560, 410), f.pixelOf(grabbed), "this drag stayed free");

        SkyPosition next = f.skyAt(450, 350);
        f.press(450, 350, false);
        f.drag(530, 420, false);
        f.release(530, 420);
        assertAt(new PixelPoint(530, 350), f.pixelOf(next), "the next one is horizontal");
    }

    @Test
    void theChartSaysAConstrainedOrLockedDragAndNothingForAFreeOne() throws Exception {
        Fixture f = new Fixture();
        String free = f.chart.getAccessibleContext().getAccessibleDescription();
        f.choose(PointerDrag.Mode.HORIZONTAL);
        assertEquals("Dragging keeps to horizontal movement on the screen.", f.chart.pointerNote());
        assertTrue(f.chart.getAccessibleContext().getAccessibleDescription()
                .endsWith(" Dragging keeps to horizontal movement on the screen."));
        f.choose(PointerDrag.Mode.LOCKED);
        assertEquals("Dragging does not move the chart.", f.chart.pointerNote());
        f.choose(PointerDrag.Mode.FREE);
        assertEquals("", f.chart.pointerNote());
        assertEquals(free, f.chart.getAccessibleContext().getAccessibleDescription(),
                "free: the released words");
    }
}
