package juranometria.ui.placeandtime;

import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.meridian.MeridianModule;
import juranometria.sky.Observer;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The frozen note is information, not an unavailable control
 * (Sprint 33, issue #350).
 *
 * <p>It was quietened with {@code setEnabled(false)}, and a screen
 * reader announces a disabled control as unavailable - so the one
 * sentence in this window that a reader can only read was spoken as
 * something they could not use. The same defect the toolbar's version
 * label carried, one surface later.
 *
 * <p>Both halves are held, because the repair could satisfy either
 * alone and be wrong: it must not be disabled, and it must still look
 * subordinate. Under both themes, because the subdued colour is
 * resolved per theme and a colour written down once would be wrong in
 * the other.
 */
class FrozenNoteTest {

    @Test
    void itIsQuietWithoutBeingAnnouncedAsUnavailable() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a colour is resolved by a realised look and feel");
        juranometria.app.SwingSession.restoring(() -> {
            for (boolean dark : new boolean[] {false, true}) {
                SwingUtilities.invokeAndWait(() -> {
                    juranometria.app.UiTheme.apply(dark);
                    com.formdev.flatlaf.FlatLaf.updateUI();
                });
                check(dark);
            }
        });
    }

    private static void check(boolean dark) throws Exception {
        InterfaceText said = InterfaceText.forLanguage("en");
        Preferences node = Preferences.userRoot()
                .node("juranometria-frozen-" + System.nanoTime());
        JFrame[] owner = new JFrame[1];
        JComponent[] content = new JComponent[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                MeridianModule module = new MeridianModule(new Observer(
                        59.913, 10.752,
                        Instant.parse("2026-03-20T21:33:00Z")));
                content[0] = PlaceAndTimeDialog.contentForStudy(module,
                        PlaceStore.forNode(node), said);
                owner[0] = new JFrame("frozen");
                owner[0].setContentPane(content[0]);
                owner[0].pack();
            });
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> {
                JLabel note = labelNamed(content[0], "frozenNote");
                assertTrue(note != null, "the premise: the note is shown");
                assertTrue(note.isEnabled(),
                        "the note is not a disabled control in "
                                + (dark ? "dark" : "light")
                                + ": a screen reader announces disabled"
                                + " as unavailable, and this is the one"
                                + " thing here a reader can only read");
                assertTrue(!note.isFocusable(),
                        "and it is still not in the tab order");
                assertEquals(UIManager.getColor("Label.disabledForeground"),
                        note.getForeground(),
                        "while staying visually subordinate: it wears"
                                + " the theme's own subdued colour,"
                                + " resolved for "
                                + (dark ? "dark" : "light"));
                assertNotEquals(new JLabel().getForeground(),
                        note.getForeground(),
                        "which is not an ordinary label's colour in "
                                + (dark ? "dark" : "light"));
                assertTrue(note.getText().contains("Nothing ticks"),
                        "and it still says what it says: "
                                + note.getText());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
            node.removeNode();
        }
    }

    /**
     * The dialog speaks the language it is given, both ways.
     *
     * <p>Not a composition test - `JUranometriaMain` opens this
     * dialog from a lambda rather than through `AtlasChrome`, so the
     * nearest honest claim is that the production entry point carries
     * a language and uses it. That is asserted here; whether the
     * application hands it the session's is held by the source of
     * `JUranometriaMain`, which passes `language.interfaceLanguage()`
     * at the single call site.
     */
    @Test
    void theDialogSaysWhatTheLanguageItIsGivenSays() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the dialog is wired when it is built");
        InterfaceText norsk = InterfaceText.forLanguage("nb-NO");
        InterfaceText english = InterfaceText.forLanguage("en");
        assertNotEquals(english.say("placeandtime.latitude.label"),
                norsk.say("placeandtime.latitude.label"),
                "the premise: this label differs by language");

        java.util.List<String> said = new java.util.ArrayList<>();
        Preferences node = Preferences.userRoot()
                .node("juranometria-pt-lang-" + System.nanoTime());
        JFrame[] owner = new JFrame[1];
        try {
            JComponent[] content = new JComponent[1];
            SwingUtilities.invokeAndWait(() -> {
                MeridianModule module = new MeridianModule(new Observer(
                        59.913, 10.752,
                        Instant.parse("2026-03-20T21:33:00Z")));
                content[0] = PlaceAndTimeDialog.contentForStudy(module,
                        PlaceStore.forNode(node), norsk);
                owner[0] = new JFrame("lang");
                owner[0].setContentPane(content[0]);
                owner[0].pack();
            });
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> words(content[0], said));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
            node.removeNode();
        }
        assertTrue(said.contains(norsk.say("placeandtime.latitude.label")),
                "it says the Norwegian label: " + said);
        assertTrue(!said.contains(english.say("placeandtime.latitude.label")),
                "and no English one");
        assertTrue(said.stream().anyMatch(w -> w.contains("⌘K")),
                "the keystroke reaches a reader unchanged: " + said);
        assertTrue(said.stream().noneMatch(w -> w.contains(" then ")),
                "with no English connector between two keystrokes: "
                        + said.stream().filter(w -> w.contains(" then "))
                                .findFirst().orElse(""));
    }

    private static void words(Container from, java.util.List<String> said) {
        for (Component child : from.getComponents()) {
            if (child instanceof JComponent widget) {
                if (widget instanceof javax.swing.AbstractButton button) {
                    add(said, button.getText());
                }
                if (widget instanceof JLabel label) {
                    add(said, label.getText());
                }
                add(said, widget.getToolTipText());
                if (widget.getAccessibleContext() != null) {
                    add(said, widget.getAccessibleContext()
                            .getAccessibleName());
                    add(said, widget.getAccessibleContext()
                            .getAccessibleDescription());
                }
            }
            if (child instanceof Container nested) {
                words(nested, said);
            }
        }
    }

    private static void add(java.util.List<String> said, String text) {
        if (text != null && !text.isBlank()) {
            said.add(text.replaceAll("<[^>]*>", " ")
                    .replaceAll("\\s+", " ").trim());
        }
    }

    private static JLabel labelNamed(Container from, String name) {
        for (Component child : from.getComponents()) {
            if (child instanceof JLabel label && name.equals(label.getName())) {
                return label;
            }
            if (child instanceof Container nested) {
                JLabel found = labelNamed(nested, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
