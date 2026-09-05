package juranometria.ui;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.SelectionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The visible <strong>Accumulate</strong> control (issue #261,
 * decided by the #258 gate): a toolbar toggle over the one shared
 * mode, so the operation is discoverable and accessible without
 * remembering a platform modifier. The control holds no state - the
 * mode is the truth, and the button says what it holds however it
 * was changed.
 */
class AccumulateControlTest {

    private record Fixture(AtlasToolbar toolbar, SelectionMode mode) {
    }

    private static Fixture fixture() throws Exception {
        AtlasToolbar[] made = new AtlasToolbar[1];
        SelectionMode mode = new SelectionMode();
        SwingUtilities.invokeAndWait(() -> {
            ChartViewController navigation = new ChartViewController();
            made[0] = new AtlasToolbar(navigation,
                    new SearchField(Atlas.search(), Atlas.assembler(),
                            navigation),
                    null, "1.0.0", () -> { }, mode);
        });
        return new Fixture(made[0], mode);
    }

    @Test
    void theControlReadsAndWritesTheOneSharedMode() throws Exception {
        Fixture fixture = fixture();
        assertFalse(fixture.toolbar().accumulateButton().isSelected(),
                "off at every start, like the mode itself");

        SwingUtilities.invokeAndWait(() ->
                fixture.toolbar().accumulateButton().doClick());
        assertTrue(fixture.mode().accumulate(),
                "pressing turns accumulation on through the mode");

        // Changed from anywhere else - a future shortcut, a test -
        // the button follows, because the mode is the only truth.
        SwingUtilities.invokeAndWait(() ->
                fixture.mode().accumulate(false));
        assertFalse(fixture.toolbar().accumulateButton().isSelected(),
                "the button reports the mode, never a private copy");
    }

    @Test
    void theControlSaysExactlyWhatItChanges() throws Exception {
        Fixture fixture = fixture();
        javax.swing.JToggleButton button =
                fixture.toolbar().accumulateButton();
        assertEquals("Accumulate selection",
                button.getAccessibleContext().getAccessibleName());
        String description = button.getAccessibleContext()
                .getAccessibleDescription();
        assertTrue(description.contains("working selection")
                        && description.contains("instead of replacing"),
                "the description states the operation, not a vibe: "
                        + description);
        assertTrue(description.contains("modifier always works"),
                "and says the platform modifier is never disabled by"
                        + " the control being off");
        assertTrue(button.isFocusable(),
                "reachable by keyboard, like every control on this bar");
    }

    @Test
    void aToolbarBuiltWithoutTheModeSimplyHasNoControl() throws Exception {
        AtlasToolbar[] made = new AtlasToolbar[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartViewController navigation = new ChartViewController();
            made[0] = new AtlasToolbar(navigation,
                    new SearchField(Atlas.search(), Atlas.assembler(),
                            navigation),
                    null, "1.0.0", () -> { });
        });
        assertNull(made[0].accumulateButton(),
                "the control exists exactly where a working selection"
                        + " does");
    }
}
