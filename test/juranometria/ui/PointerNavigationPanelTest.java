package juranometria.ui;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Controller's Pointer navigation section (#691, C-5 and C-6 on
 * #690): four exclusive, named and explained choices over the one
 * session mode, in both languages.
 */
class PointerNavigationPanelTest {

    private static PointerNavigationPanel panel(PointerDrag drag, String language)
            throws Exception {
        PointerNavigationPanel[] out = new PointerNavigationPanel[1];
        SwingUtilities.invokeAndWait(() ->
                out[0] = new PointerNavigationPanel(drag, InterfaceText.forLanguage(language)));
        return out[0];
    }

    /** What a press on a choice does, without a synthetic click. */
    private static void choose(JRadioButton choice) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            choice.setSelected(true);
            for (var listener : choice.getActionListeners()) {
                listener.actionPerformed(new ActionEvent(choice, ActionEvent.ACTION_PERFORMED, ""));
            }
        });
    }

    @Test
    void fourExclusiveChoicesFreeChosenAtEveryStart() throws Exception {
        PointerDrag drag = new PointerDrag();
        PointerNavigationPanel p = panel(drag, "en");
        assertEquals(List.of("Free", "Horizontal only", "Vertical only", "Locked"),
                java.util.Arrays.stream(PointerDrag.Mode.values())
                        .map(m -> p.choice(m).getText()).toList());
        assertTrue(p.choice(PointerDrag.Mode.FREE).isSelected());
        for (PointerDrag.Mode mode : PointerDrag.Mode.values()) {
            JRadioButton choice = p.choice(mode);
            assertEquals(PointerNavigationPanel.NAME_PREFIX + mode.token(), choice.getName());
            assertEquals(choice.getText(), choice.getAccessibleContext().getAccessibleName());
            assertNotNull(choice.getAccessibleContext().getAccessibleDescription(),
                    "explained: " + mode);
        }
    }

    @Test
    void choosingSetsTheSessionModeAndTheModeSelectsTheChoice() throws Exception {
        PointerDrag drag = new PointerDrag();
        PointerNavigationPanel p = panel(drag, "en");
        choose(p.choice(PointerDrag.Mode.VERTICAL));
        assertEquals(PointerDrag.Mode.VERTICAL, drag.mode());
        assertFalse(p.choice(PointerDrag.Mode.FREE).isSelected());
        SwingUtilities.invokeAndWait(() -> drag.choose(PointerDrag.Mode.LOCKED));
        assertTrue(p.choice(PointerDrag.Mode.LOCKED).isSelected());
        assertFalse(p.choice(PointerDrag.Mode.VERTICAL).isSelected());
    }

    @Test
    void theNorwegianWordsAreTheRuledOnes() throws Exception {
        PointerNavigationPanel p = panel(new PointerDrag(), "nb-NO");
        assertEquals(List.of("Fri", "Bare vannrett", "Bare loddrett", "Låst"),
                java.util.Arrays.stream(PointerDrag.Mode.values())
                        .map(m -> p.choice(m).getText()).toList());
        assertEquals("Navigering med pekeren",
                InterfaceText.forLanguage("nb-NO").say("pointer.title"));
    }
}
