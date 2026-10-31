package juranometria.ui;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.junit.jupiter.api.Test;

import juranometria.chart.SelectionMode;
import juranometria.chart.SkyPosition;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The toolbar's Place and Time and Controls buttons (#675, stage 2,
 * ruled on #674): icon-only, attached by the application after the bar
 * is built, each the same action as the Chart menu's item; Controls a
 * checked toggle whose tick and words follow the Controller window.
 */
class ToolbarContextButtonsTest {

    private static AtlasToolbar bar(InspectorToggle inspector) {
        ChartViewController controller = new ChartViewController();
        controller.recenter(new SkyPosition(10.684708, 41.268750), 8.0);
        return new AtlasToolbar(controller,
                new SearchField(juranometria.app.Atlas.search(),
                        juranometria.app.Atlas.assembler(), controller,
                        InterfaceText.forLanguage("en")),
                inspector, "0.0.0", () -> { }, new SelectionMode(),
                new ZoomLock(), InterfaceText.forLanguage("en"));
    }

    private static int indexOf(JToolBar bar, Component component) {
        return bar.getComponentIndex(component);
    }

    @Test
    void bothAreAbsentUntilTheApplicationAttachesThem() {
        AtlasToolbar bar = bar(new InspectorToggle());
        assertNull(bar.placeAndTimeButton(), "no window to open yet");
        assertNull(bar.controlsButton(), "no window to switch yet");
    }

    @Test
    void placeAndTimeIsAnIconOnlyButtonAfterResetViewRunningTheOneOpener() {
        AtlasToolbar bar = bar(new InspectorToggle());
        int[] opened = {0};
        JButton button = bar.attachPlaceAndTime(() -> opened[0]++);
        assertNotNull(button.getIcon(), "icon-only: the glyph is the button");
        assertEquals("", button.getText() == null ? "" : button.getText(),
                "and no text beside it");
        assertEquals("Place and Time",
                button.getAccessibleContext().getAccessibleName(),
                "so the spoken name carries the word");
        assertTrue(button.getToolTipText().startsWith("Place and Time"),
                "and the hover: " + button.getToolTipText());
        assertTrue(button.getAccessibleContext().getAccessibleDescription()
                .contains("Chart's Place and Time"),
                "naming the menu item it shares its action with");
        assertTrue(button.isFocusable(), "reachable by keyboard (#449)");
        assertEquals(ChartControls.PLACE_AND_TIME, button.getName());
        // After Reset view and its separator: the chart's own context,
        // its first page then its place and instant, stand together.
        int home = indexOf(bar, bar.controls().home());
        assertTrue(bar.getComponent(home + 1) instanceof JToolBar.Separator);
        assertEquals(button, bar.getComponent(home + 2));
        assertTrue(bar.getComponent(home + 3) instanceof JToolBar.Separator);
        button.doClick();
        assertEquals(1, opened[0], "pressing opens the window, once");
        assertThrows(IllegalStateException.class,
                () -> bar.attachPlaceAndTime(() -> { }), "attached once");
    }

    @Test
    void controlsIsACheckedToggleBesideTheInspectorFollowingTheWindow() {
        AtlasToolbar bar = bar(new InspectorToggle());
        int[] toggled = {0};
        JToggleButton button = bar.attachControls(() -> toggled[0]++);
        assertNotNull(button.getIcon(), "icon-only");
        assertEquals("Controls", button.getAccessibleContext().getAccessibleName());
        assertEquals(ChartControls.CONTROLS, button.getName());
        assertTrue(button.isFocusable());
        assertEquals(indexOf(bar, bar.controls().inspector()) + 1, indexOf(bar, button),
                "directly after the Inspector's toggle: the two window switches together");

        assertFalse(button.isSelected(), "hidden until the window says otherwise");
        assertTrue(button.getToolTipText().startsWith("Show the Controller"),
                "hidden: the hover offers to show it - " + button.getToolTipText());
        button.doClick();
        assertEquals(1, toggled[0], "pressing asks the window's switch");
        // Pressing does not decide the state: the window does, through
        // the host, as the menu item's tick does.
        bar.controlsShowing(true);
        assertTrue(button.isSelected(), "the window opened: the tick says so");
        assertTrue(button.getToolTipText().startsWith("Hide the Controller"),
                "showing: the hover offers to hide it - " + button.getToolTipText());
        assertTrue(button.getAccessibleContext().getAccessibleDescription()
                .startsWith("Showing;"));
        bar.controlsShowing(false);
        assertFalse(button.isSelected());
        assertTrue(button.getAccessibleContext().getAccessibleDescription()
                .startsWith("Hidden;"));
    }

    @Test
    void withoutAnInspectorControlsTakesItsPlaceBeforeAccumulate() {
        AtlasToolbar bar = bar(null);
        JToggleButton button = bar.attachControls(() -> { });
        int at = indexOf(bar, button);
        assertEquals(indexOf(bar, bar.controls().accumulate()), at + 2,
                "Controls, a separator, then Accumulate");
        assertTrue(bar.getComponent(at + 1) instanceof JToolBar.Separator);
    }

    @Test
    void bothSpeakTheSessionsLanguage() {
        ChartViewController controller = new ChartViewController();
        InterfaceText norsk = InterfaceText.forLanguage("nb-NO");
        AtlasToolbar bar = new AtlasToolbar(controller,
                new SearchField(juranometria.app.Atlas.search(),
                        juranometria.app.Atlas.assembler(), controller, norsk),
                new InspectorToggle(), "0.0.0", () -> { }, new SelectionMode(),
                new ZoomLock(), norsk);
        assertEquals("Sted og tid", bar.attachPlaceAndTime(() -> { })
                .getAccessibleContext().getAccessibleName());
        JToggleButton controls = bar.attachControls(() -> { });
        assertEquals("Kontroller", controls.getAccessibleContext().getAccessibleName());
        assertTrue(controls.getToolTipText().startsWith("Vis Kontrolleren"));
    }
}
