package juranometria.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.app.InspectorPanel;
import juranometria.chart.SelectionModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One switch, however a reader reaches it (issue #180): the toolbar
 * button, the View menu item, and a window too narrow to show the
 * panel must never disagree about whether the Inspector is there.
 */
class InspectorToggleTest {

    private record Wiring(InspectorToggle toggle, InspectorPanel panel,
                          AtlasToolbar toolbar,
                          List<InspectorToggle.State> heard) {
    }

    private static Wiring wire() throws Exception {
        SelectionModel selection = new SelectionModel();
        List<InspectorToggle.State> heard = new ArrayList<>();
        InspectorToggle toggle = new InspectorToggle();
        InspectorPanel[] panel = new InspectorPanel[1];
        AtlasToolbar[] toolbar = new AtlasToolbar[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartComponent chart = new ChartComponent(Atlas.assembler());
            panel[0] = new InspectorPanel(selection, chart::currentScene,
                    () -> juranometria.render.ChartOptions.DEFAULTS,
                    chosen -> { });
            toggle.bind(() -> panel[0].setRequestedVisible(
                            !panel[0].isRequestedVisible()),
                    panel[0]::canShow);
            panel[0].onVisibilityChange(toggle::report);
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            SearchField search = new SearchField(Atlas.search(),
                    Atlas.assembler(), navigation);
            toolbar[0] = new AtlasToolbar(navigation, search, toggle);
            toggle.onChange(heard::add);
        });
        return new Wiring(toggle, panel[0], toolbar[0], heard);
    }

    private static javax.swing.JToggleButton inspectorButton(
            java.awt.Container container) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof javax.swing.JToggleButton button
                    && "Inspector".equals(button.getAccessibleContext()
                            .getAccessibleName())) {
                return button;
            }
            if (component instanceof java.awt.Container inner) {
                javax.swing.JToggleButton found = inspectorButton(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void theToolbarCarriesAnAccessibleInspectorControl() throws Exception {
        Wiring wiring = wire();
        javax.swing.JToggleButton button = inspectorButton(wiring.toolbar());

        assertNotNull(button, "the toolbar carries the control");
        assertEquals("Inspector", button.getAccessibleContext()
                .getAccessibleName());
        assertNotNull(button.getToolTipText(), "with a useful tooltip");
        assertTrue(button.isFocusable(), "and it takes keyboard focus");
        assertFalse(button.isSelected(),
                "starting closed, like the panel itself");
    }

    @Test
    void theButtonAndThePanelAgreeInBothDirections() throws Exception {
        Wiring wiring = wire();
        javax.swing.JToggleButton button = inspectorButton(wiring.toolbar());

        // Pressed on the toolbar.
        SwingUtilities.invokeAndWait(button::doClick);
        assertTrue(wiring.panel().isVisible(),
                "the button opened the panel");
        assertTrue(button.isSelected(), "and shows it as open");

        SwingUtilities.invokeAndWait(button::doClick);
        assertFalse(wiring.panel().isVisible(), "and closed it again");
        assertFalse(button.isSelected());

        // Asked for elsewhere - what the View menu item does.
        SwingUtilities.invokeAndWait(wiring.toggle()::toggle);
        assertTrue(wiring.panel().isVisible(),
                "the other route opens the same panel");
        assertTrue(button.isSelected(),
                "and the toolbar follows without being touched");
    }

    @Test
    void aNarrowWindowMakesTheControlSayItCannotRatherThanLie()
            throws Exception {
        Wiring wiring = wire();
        javax.swing.JToggleButton button = inspectorButton(wiring.toolbar());
        SwingUtilities.invokeAndWait(button::doClick);
        assertTrue(button.isSelected(), "open, and saying so");

        SwingUtilities.invokeAndWait(() ->
                wiring.panel().setAvailableWidth(600));
        assertFalse(wiring.panel().isVisible(),
                "the window closed it for the reader");
        assertFalse(button.isSelected(),
                "so the control must not claim the panel is there");
        assertFalse(button.isEnabled(),
                "and says plainly that it cannot be shown");
        assertTrue(button.getToolTipText().contains("too narrow"),
                "in words: " + button.getToolTipText());

        SwingUtilities.invokeAndWait(() ->
                wiring.panel().setAvailableWidth(1240));
        assertTrue(wiring.panel().isVisible(),
                "widening restores what the reader asked for");
        assertTrue(button.isSelected() && button.isEnabled(),
                "and the control tells the truth again");
    }

    @Test
    void everyChangeIsAnnouncedOnceWithBothFacts() throws Exception {
        Wiring wiring = wire();
        int before = wiring.heard().size();

        SwingUtilities.invokeAndWait(wiring.toggle()::toggle);
        assertEquals(before + 1, wiring.heard().size(),
                "one flip, one announcement");
        InspectorToggle.State state =
                wiring.heard().get(wiring.heard().size() - 1);
        assertTrue(state.showing() && state.available(),
                "carrying what is and what is possible: " + state);

        SwingUtilities.invokeAndWait(() ->
                wiring.panel().setAvailableWidth(500));
        InspectorToggle.State narrow =
                wiring.heard().get(wiring.heard().size() - 1);
        assertFalse(narrow.showing(), "not showing");
        assertFalse(narrow.available(), "and not possible either");
    }

    @Test
    void theToolbarKnowsNothingAboutWindowsOrPanels() throws Exception {
        // The seam exists so the toolbar cannot grow application
        // knowledge (issue #180). This is where that stops being a
        // claim.
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/juranometria/ui/AtlasToolbar.java"));
        assertFalse(source.contains("JFrame") || source.contains("Window")
                        || source.contains("InspectorPanel")
                        || source.contains("juranometria.app"),
                "the toolbar asks a switch and is told an answer;"
                        + " it does not reach into the application");
    }
}
