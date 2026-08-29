package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartComponentTest {

    static final SkyPosition M31 = new SkyPosition(10.684708, 41.268750);

    /**
     * setSize posts COMPONENT_RESIZED to the event queue rather than firing
     * it synchronously, so every interaction runs on the EDT and a barrier
     * flushes the queue before assertions.
     */
    private static ChartComponent sizedComponent(
            SceneAssemblerTest.CountingCatalogue catalogue) throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(
                    new SceneAssembler(catalogue, M31, 10.0, 1.5));
            holder[0].setSize(300, 200);
        });
        flushEventQueue();
        return holder[0];
    }

    private static void flushEventQueue() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static void paint(ChartComponent component) {
        BufferedImage image = new BufferedImage(
                component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        component.paintComponent(image.createGraphics());
    }

    // Regression guard: a plain JComponent subclass has no accessible
    // context by default, and setting the accessible name during
    // construction crashed the application at startup.
    @Test
    void exposesAnAccessibleContextWithAName() {
        ChartComponent component = new ChartComponent(new SceneAssembler(
                new SceneAssemblerTest.CountingCatalogue(), M31, 10.0, 1.5));
        assertNotNull(component.getAccessibleContext());
        assertEquals("Star chart", component.getAccessibleContext().getAccessibleName());
    }

    @Test
    void stateChangesQueryOnceAndRepaintsQueryNever() throws Exception {
        SceneAssemblerTest.CountingCatalogue catalogue =
                new SceneAssemblerTest.CountingCatalogue();
        ChartComponent component = sizedComponent(catalogue);
        assertEquals(1, catalogue.starQueries, "sizing assembles the first scene");

        paint(component);
        paint(component);
        assertEquals(1, catalogue.starQueries,
                "repainting an unchanged view performs no catalogue query");

        SwingUtilities.invokeAndWait(() ->
                component.setViewState(ChartViewState.DEFAULT.zoomIn()));
        flushEventQueue();
        assertEquals(2, catalogue.starQueries, "a state change assembles a new scene");
        assertEquals(6.0, component.scene().viewport().fieldWidthDegrees());

        paint(component);
        assertEquals(2, catalogue.starQueries);

        SwingUtilities.invokeAndWait(() ->
                component.setViewState(ChartViewState.DEFAULT));
        flushEventQueue();
        assertEquals(3, catalogue.starQueries, "reset assembles the default scene");
        assertEquals(8.0, component.scene().viewport().fieldWidthDegrees());
        assertEquals(8.0, component.scene().limitingMagnitude());
    }

    @Test
    void tallWindowsLetterboxThePageToTheHonestHeight() throws Exception {
        SceneAssemblerTest.CountingCatalogue catalogue =
                new SceneAssemblerTest.CountingCatalogue();
        ChartComponent component = sizedComponent(catalogue);

        SwingUtilities.invokeAndWait(() -> component.setSize(400, 1000));
        flushEventQueue();

        assertEquals(400, component.scene().viewport().widthPx());
        assertTrue(component.scene().viewport().heightPx() < 1000,
                "the page must not promise sky beyond the bundled coverage");
        paint(component);
    }

    @Test
    void resizesReassembleForTheNewGeometry() throws Exception {
        SceneAssemblerTest.CountingCatalogue catalogue =
                new SceneAssemblerTest.CountingCatalogue();
        ChartComponent component = sizedComponent(catalogue);
        int afterSizing = catalogue.starQueries;

        SwingUtilities.invokeAndWait(() -> component.setSize(500, 400));
        flushEventQueue();
        assertEquals(afterSizing + 1, catalogue.starQueries);
        assertEquals(500, component.scene().viewport().widthPx());
    }
}
