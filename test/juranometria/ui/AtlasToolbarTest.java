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
        AtlasToolbar toolbar = new AtlasToolbar(new ChartViewController());
        for (String name : new String[] {"Zoom in", "Zoom out", "Reset view"}) {
            JButton button = button(toolbar, name);
            assertEquals(name, button.getToolTipText());
            assertNotNull(button.getIcon(), name + " must have an icon");
        }
    }

    @Test
    void readoutAndEnablementStaySynchronizedWithTheState() {
        ChartViewController controller = new ChartViewController();
        AtlasToolbar toolbar = new AtlasToolbar(controller);

        assertEquals("Field 8°", readout(toolbar).getText());
        assertFalse(button(toolbar, "Zoom out").isEnabled(),
                "zoom out is disabled at the 8-degree bound");
        assertTrue(button(toolbar, "Zoom in").isEnabled());

        while (controller.state().canZoomIn()) {
            controller.zoomIn();
        }
        assertEquals("Field 1°", readout(toolbar).getText());
        assertFalse(button(toolbar, "Zoom in").isEnabled(),
                "zoom in is disabled at the 1-degree bound");
        assertTrue(button(toolbar, "Zoom out").isEnabled());

        controller.reset();
        assertEquals("Field 8°", readout(toolbar).getText());
        assertTrue(button(toolbar, "Zoom in").isEnabled());
        assertFalse(button(toolbar, "Zoom out").isEnabled());
    }
}
