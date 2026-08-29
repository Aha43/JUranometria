package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JLabel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasToolbarTest {

    private static AtlasToolbar toolbar(ChartViewController controller) {
        return new AtlasToolbar(controller, new SearchField(
                new juranometria.search.LocalSearch(java.util.List.of(), java.util.List.of()),
                new SceneAssembler(new SceneAssemblerTest.CountingCatalogue(),
                        new juranometria.chart.SkyPosition(10.684708, 41.268750),
                        "Test chart", 10.0),
                controller));
    }

    private static JButton button(AtlasToolbar toolbar, String accessibleName) {
        for (Component component : toolbar.getComponents()) {
            if (component instanceof JButton button && accessibleName.equals(
                    button.getAccessibleContext().getAccessibleName())) {
                return button;
            }
        }
        throw new AssertionError("no toolbar button named " + accessibleName);
    }

    private static JLabel readout(AtlasToolbar toolbar) {
        for (Component component : toolbar.getComponents()) {
            if (component instanceof JLabel label) {
                return label;
            }
        }
        throw new AssertionError("no readout label on the toolbar");
    }

    @Test
    void buttonsCarryAccessibleNamesTooltipsAndIcons() {
        AtlasToolbar toolbar = toolbar(new ChartViewController());
        for (String name : new String[] {
                "Zoom in", "Zoom out", "Fewer stars", "More stars", "Reset view"}) {
            JButton button = button(toolbar, name);
            assertNotNull(button.getToolTipText(), name + " must have a tooltip");
            assertNotNull(button.getIcon(), name + " must have an icon");
        }
    }

    @Test
    void readoutAndEnablementStaySynchronizedWithTheState() {
        ChartViewController controller = new ChartViewController();
        AtlasToolbar toolbar = toolbar(controller);

        assertEquals("Field 8° · Stars to V 8.0", readout(toolbar).getText());
        assertFalse(button(toolbar, "Zoom out").isEnabled(),
                "zoom out is disabled at the 8-degree bound");
        assertTrue(button(toolbar, "Zoom in").isEnabled());

        while (controller.state().canZoomIn()) {
            controller.zoomIn();
        }
        assertEquals("Field 1° · Stars to V 8.0", readout(toolbar).getText());
        assertFalse(button(toolbar, "Zoom in").isEnabled(),
                "zoom in is disabled at the 1-degree bound");
        assertTrue(button(toolbar, "Zoom out").isEnabled());

        controller.reset();
        assertEquals("Field 8° · Stars to V 8.0", readout(toolbar).getText());
        assertTrue(button(toolbar, "Zoom in").isEnabled());
        assertFalse(button(toolbar, "Zoom out").isEnabled());
    }

    @Test
    void magnitudeControlsWalkTheirBoundsAndStaySynchronized() {
        ChartViewController controller = new ChartViewController();
        AtlasToolbar toolbar = toolbar(controller);

        assertFalse(button(toolbar, "More stars").isEnabled(),
                "the fixture holds nothing fainter than V 8.0");
        assertTrue(button(toolbar, "Fewer stars").isEnabled());

        controller.decreaseMagnitudeLimit();
        assertEquals("Field 8° · Stars to V 7.0", readout(toolbar).getText());
        assertTrue(button(toolbar, "More stars").isEnabled());

        while (controller.state().canDecreaseMagnitudeLimit()) {
            controller.decreaseMagnitudeLimit();
        }
        assertEquals("Field 8° · Stars to V 4.0", readout(toolbar).getText());
        assertFalse(button(toolbar, "Fewer stars").isEnabled(),
                "fewer-stars is disabled at the V 4.0 bound");
    }

    @Test
    void buttonsDriveTheControllerThroughTheirActionListeners() throws Exception {
        // Codex review, PR #20: activate the real buttons rather than the
        // controller, so a missing ActionListener cannot pass the suite.
        ChartViewController controller = new ChartViewController();
        AtlasToolbar toolbar = toolbar(controller);

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            button(toolbar, "Zoom in").doClick();
            button(toolbar, "Fewer stars").doClick();
        });
        assertEquals(6.0, controller.state().fieldWidthDegrees());
        assertEquals(7.0, controller.state().limitingMagnitude());

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            button(toolbar, "Zoom out").doClick();
            button(toolbar, "More stars").doClick();
        });
        assertEquals(juranometria.chart.ChartViewState.DEFAULT, controller.state());

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            button(toolbar, "Zoom in").doClick();
            button(toolbar, "Fewer stars").doClick();
            button(toolbar, "Reset view").doClick();
        });
        assertEquals(juranometria.chart.ChartViewState.DEFAULT, controller.state(),
                "reset via its button restores the default after both changed");
        assertEquals("Field 8° · Stars to V 8.0", readout(toolbar).getText());
    }

    @Test
    void resetRestoresTheCompleteDefaultAfterBothControlsChanged() {
        ChartViewController controller = new ChartViewController();
        AtlasToolbar toolbar = toolbar(controller);

        controller.zoomIn();
        controller.zoomIn();
        controller.decreaseMagnitudeLimit();
        controller.decreaseMagnitudeLimit();
        assertEquals("Field 4° · Stars to V 6.0", readout(toolbar).getText());

        controller.reset();
        assertEquals("Field 8° · Stars to V 8.0", readout(toolbar).getText());
        assertFalse(button(toolbar, "Zoom out").isEnabled());
        assertFalse(button(toolbar, "More stars").isEnabled());
        assertTrue(button(toolbar, "Zoom in").isEnabled());
        assertTrue(button(toolbar, "Fewer stars").isEnabled());
    }
}
