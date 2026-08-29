package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import juranometria.chart.ChartViewState;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.app.Atlas;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 8 acceptance journey through the real application objects
 * and real controls: search M42, zoom out until Orion's geography
 * establishes the region, grab the paper and pan along the
 * constellation with real mouse events, verify the honest coordinate
 * title, keep exploring around the new centre, and return Home with
 * the real toolbar button.
 */
class ExplorationJourneyTest {

    @Test
    void searchZoomGrabPanExploreAndHome() throws Exception {
        ChartComponent[] chartHolder = new ChartComponent[1];
        ChartViewController controller =
                new ChartViewController(Atlas.assembler()::fits);
        SearchField[] searchHolder = new SearchField[1];
        AtlasToolbar[] toolbarHolder = new AtlasToolbar[1];
        SwingUtilities.invokeAndWait(() -> {
            chartHolder[0] = new ChartComponent(Atlas.assembler());
            PanInteraction.install(chartHolder[0], controller);
            controller.onChange(chartHolder[0]::setViewState);
            searchHolder[0] = new SearchField(
                    Atlas.search(), Atlas.assembler(), controller);
            toolbarHolder[0] = new AtlasToolbar(controller, searchHolder[0]);
            chartHolder[0].setSize(900, 700);
        });
        flush();
        ChartComponent chart = chartHolder[0];

        // Search M42 through the real field action - typed text and the
        // Enter key's action event, not the test seam beneath it.
        SwingUtilities.invokeAndWait(() -> {
            searchHolder[0].setText("m 42");
            searchHolder[0].postActionEvent();
        });
        SwingUtilities.invokeAndWait(() -> {
            while (controller.state().fieldWidthDegrees() < 18.0) {
                controller.zoomOut();
            }
        });
        flush();
        assertEquals("NGC 1976", controller.state().targetIdentity());
        assertTrue(chart.scene().geography().latinNames()
                        .containsValue("Orion"),
                "Orion's geography establishes the region before the grab");

        // Grab the paper and pan along the constellation.
        SkyPosition grabbed = PanSolver.skyFromPlane(
                controller.state().centre(),
                PanSolver.planeFromPixel(chart.scene().viewport(),
                        new PixelPoint(450, 350)));
        mouse(chart, MouseEvent.MOUSE_PRESSED, 450, 350);
        mouse(chart, MouseEvent.MOUSE_DRAGGED, 520, 260);
        mouse(chart, MouseEvent.MOUSE_DRAGGED, 610, 210);
        mouse(chart, MouseEvent.MOUSE_RELEASED, 610, 210);

        PixelPoint now = new ViewportMapping(chart.scene().viewport())
                .toPixel(new GnomonicProjection(chart.scene().viewport()
                        .centre()).project(grabbed).orElseThrow());
        assertTrue(Math.hypot(now.x() - 610, now.y() - 210) < 1e-3,
                "the grabbed sky released under the moved pointer");
        assertEquals(null, controller.state().targetLabel());
        assertEquals(null, controller.state().targetIdentity());
        assertTrue(chart.scene().title().contains("h "),
                "the panned page titles by coordinates: "
                        + chart.scene().title());
        assertEquals(18.0, controller.state().fieldWidthDegrees());

        // Keep exploring around the new centre.
        SkyPosition panned = controller.state().centre();
        SwingUtilities.invokeAndWait(() -> {
            controller.zoomOut();
            controller.decreaseMagnitudeLimit();
        });
        flush();
        assertEquals(panned, controller.state().centre());
        assertEquals(24.0, controller.state().fieldWidthDegrees());
        assertEquals(7.0, controller.state().limitingMagnitude());

        // Home, through the real toolbar button - exactly one exists.
        int[] resetButtons = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            for (java.awt.Component component
                    : toolbarHolder[0].getComponents()) {
                if (component instanceof javax.swing.JButton button
                        && "Reset view".equals(button.getAccessibleContext()
                                .getAccessibleName())) {
                    resetButtons[0]++;
                    button.doClick();
                }
            }
        });
        flush();
        assertEquals(1, resetButtons[0],
                "exactly one Reset view button exists and was clicked");
        assertEquals(ChartViewState.DEFAULT, controller.state(),
                "Home restores the exact released default");
        assertEquals("M31 · Andromeda Galaxy region", chart.scene().title());
        assertEquals(SceneGeography.EMPTY, chart.scene().geography(),
                "the released 8-degree page carries no geography");
    }

    private static void mouse(ChartComponent chart, int id, int x, int y)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(new MouseEvent(
                chart, id, System.nanoTime() / 1_000_000,
                MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                MouseEvent.BUTTON1)));
        flush();
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }
}
