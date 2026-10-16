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
                        10.0, 1.5),
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
        assertTrue(button(toolbar, "Zoom out").isEnabled(),
                "regional steps open above the 8-degree default");
        assertTrue(button(toolbar, "Zoom in").isEnabled());

        while (controller.state().canZoomOut()) {
            controller.zoomOut();
        }
        // The globe, reached by zooming out from the released 8
        // degrees: the ladder's rule carries the brighter limit up
        // with the reader, so the V 4.0 the overview's rungs impose
        // is what arrives here rather than the globe's own V 5.0
        // (#329).
        assertEquals("Field 180° · Stars to V 4.0", readout(toolbar).getText(),
                "the widest rung is the globe's, at the limit the"
                        + " reader carried to it");
        assertFalse(button(toolbar, "Zoom out").isEnabled(),
                "zoom out is disabled at the widest bound");
        controller.reset();

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
        assertTrue(button(toolbar, "Zoom out").isEnabled());
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
        assertTrue(button(toolbar, "Zoom out").isEnabled(),
                "regional steps stay available after reset");
        assertFalse(button(toolbar, "More stars").isEnabled());
        assertTrue(button(toolbar, "Zoom in").isEnabled());
        assertTrue(button(toolbar, "Fewer stars").isEnabled());
    }

    @Test
    void aCloserFieldReadsToItsOwnPrecisionAndZoomsOutBackToTheLadder() {
        // #543, part of #481: no surface offers a closer field yet, but a
        // state at one reads as itself - never "Field 0°" - with zoom in
        // unavailable and zoom out walking back to the floor. The
        // keyboard's and the menu's zoom are the same controller calls.
        ChartViewController controller = new ChartViewController();
        AtlasToolbar toolbar = toolbar(controller);
        juranometria.chart.SkyPosition pleiades =
                new juranometria.chart.SkyPosition(56.75, 24.12);

        controller.recenter(pleiades, 0.25);
        assertEquals("Field 0.25° · Stars to V 8.0", readout(toolbar).getText());
        assertFalse(button(toolbar, "Zoom in").isEnabled(),
                "zoom in is not the closer step");
        assertTrue(button(toolbar, "Zoom out").isEnabled());
        assertEquals(ChartViewController.PointerZoomOutcome.AT_BOUND,
                controller.zoomAt(new juranometria.project.PlanePoint(0.001, 0.0),
                        true, 900, 700),
                "nor is the wheel's");

        controller.zoomOut();
        assertEquals("Field 0.5° · Stars to V 8.0", readout(toolbar).getText());
        assertEquals(ChartViewController.PointerZoomOutcome.ACCEPTED,
                controller.zoomAt(new juranometria.project.PlanePoint(0.001, 0.0),
                        false, 900, 700),
                "the wheel zooms out of a closer field about the pointer");
        assertEquals("Field 1° · Stars to V 8.0", readout(toolbar).getText());
        assertEquals(1.0, controller.state().fieldWidthDegrees());
    }

    @Test
    void aCloserFieldIsNeverPersisted() {
        // Ruling 5: a closer field is never saved. Nothing saves the view
        // at all - every controller, and so every start, opens at the
        // released default page.
        ChartViewController controller = new ChartViewController();
        controller.recenter(new juranometria.chart.SkyPosition(56.75, 24.12), 0.25);
        assertEquals(juranometria.chart.ChartViewState.DEFAULT,
                new ChartViewController().state());
    }

    @Test
    void zoomInNamesTheCloserStepWhenOfferedAndSaysWhyWhenNot() {
        // #544: at the floor zoom in is the closer step where the chart
        // offers one, and says so; where it does not, it says why rather
        // than claiming the atlas draws nothing narrower.
        ChartViewController controller = new ChartViewController();
        AtlasToolbar toolbar = toolbar(controller);
        juranometria.chart.SkyPosition somewhere =
                new juranometria.chart.SkyPosition(56.75, 24.12);
        controller.recenter(somewhere, 1.0);
        JButton zoomIn = button(toolbar, "Zoom in");
        assertFalse(zoomIn.isEnabled());
        assertEquals(juranometria.ui.language.InterfaceText.forLanguage("en")
                        .say("toolbar.zoomIn.closer.unavailable"),
                zoomIn.getAccessibleContext().getAccessibleDescription());

        boolean[] offered = {false};
        controller.offerCloserFields(candidate -> offered[0]);
        offered[0] = true;
        controller.closerOfferChanged();
        assertTrue(zoomIn.isEnabled(), "the offer reaches the button without a new view");
        assertEquals(juranometria.ui.language.InterfaceText.forLanguage("en")
                        .say("toolbar.zoomIn.closer.explain"),
                zoomIn.getAccessibleContext().getAccessibleDescription());
        assertTrue(zoomIn.getToolTipText().startsWith("Zoom in to a closer field"),
                zoomIn.getToolTipText());

        controller.zoomIn();
        assertEquals("Field 0.5° · Stars to V 8.0", readout(toolbar).getText());
        controller.zoomIn();
        assertEquals(0.25, controller.state().fieldWidthDegrees());
        assertFalse(zoomIn.isEnabled());
        assertEquals(juranometria.ui.language.InterfaceText.forLanguage("en")
                        .say("toolbar.zoomIn.end"),
                zoomIn.getAccessibleContext().getAccessibleDescription(),
                "0.25° is the narrowest field the atlas draws");
    }
}
