package juranometria.app;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chart Options speaks whole sentences, in either language
 * (Sprint 33, issue #350).
 *
 * <p>This surface was chosen because it is where a resource scheme
 * can fail in interesting ways, and it did. Three defects were in it
 * before any translation began:
 *
 * <ul>
 *   <li>five spoken descriptions read "NGC 3628.." because a frame
 *       appended a period to a sentence that already had one;
 *   <li>a dependency clause was built by lowercasing a visible label
 *       with {@code Locale.ROOT} - English's rule about English
 *       words, offered as though it were universal;
 *   <li>a family's whole description was assembled in the enum, in
 *       English order, from a description, a fixed phrase and a list
 *       of catalogue identifiers.
 * </ul>
 *
 * <p>All three are the same mistake: prose manufactured at runtime.
 * What is held here is that it stays gone.
 */
class ChartOptionsLanguageTest {

    private static final String NORWEGIAN = "nb-NO";

    /**
     * A dependency is named by the translation, not by lowercasing.
     *
     * <p>The mutation this test exists for is restoring
     * {@code ChartKeys.toggle(dependsOn).label().toLowerCase(ROOT)}.
     * That path produces "deep-sky objects" in every language,
     * because it transforms an English label rather than asking for
     * a word - so Norwegian would read "…deep-sky objects må være
     * slått på", which no Norwegian wrote and no test comparing
     * English to English would notice.
     */
    @Test
    void aDependencyIsNamedByTheTranslationAndNotByLowercasing()
            throws Exception {
        List<String> spoken = spokenIn(NORWEGIAN);
        List<String> depends = spoken.stream()
                .filter(said -> said.contains("må være slått på"))
                .toList();

        assertTrue(depends.size() >= 7,
                "the premise: this surface really does have switches"
                        + " that wait for a master - " + depends.size());
        List<String> english = depends.stream()
                .filter(said -> said.contains("deep-sky")
                        || said.contains("constellation figures"))
                .toList();
        assertEquals(List.of(), english,
                "no dependency clause carries an English label put"
                        + " through toLowerCase. The Norwegian name is"
                        + " its own resource, in the form this"
                        + " sentence needs");
        assertTrue(depends.stream().anyMatch(said ->
                        said.contains("Dyphimmelobjekter må være slått på")),
                "and it reads as Norwegian: " + depends.get(0));
    }

    /** The keystroke connector is language; the keys are notation. */
    @Test
    void theShortcutConnectorIsTranslatedAndTheKeysAreNot()
            throws Exception {
        List<String> norsk = spokenIn(NORWEGIAN);
        List<String> english = spokenIn("en");

        // The prefix comes from the platform, spelled by the same
        // source production asks. Hard-coding it made this test pass
        // alone and fail in the suite: the menu modifier is the
        // toolkit's, and it answered ⌃K headless and ⌘K under a
        // display. prefixText() is the WHOLE first keystroke - "⌘K",
        // not "⌘" - which is the second thing this got wrong.
        //
        // The CONNECTOR is what this test is about. The keystrokes
        // are notation and are whatever this desktop calls them.
        String prefix = ChartKeys.prefixText();
        assertTrue(norsk.stream().anyMatch(s ->
                        s.contains(prefix + " deretter ")),
                "Norwegian joins two keystrokes with its own word,"
                        + " after the platform's own prefix \"" + prefix
                        + "\"");
        assertTrue(english.stream().anyMatch(s ->
                        s.contains(prefix + " then ")),
                "and English with its own");
        assertTrue(norsk.stream().noneMatch(s -> s.contains(" then ")),
                "with no English connector left in the Norwegian: "
                        + norsk.stream().filter(s -> s.contains(" then "))
                                .findFirst().orElse(""));
        assertTrue(norsk.stream().anyMatch(s -> s.contains(prefix)),
                "while the modifier and the letter are notation and"
                        + " are the same in both");
    }

    /**
     * Catalogue identifiers survive translation untouched.
     *
     * <p>They are data, and the translation places them where its own
     * grammar wants them rather than receiving a sentence already
     * built in English order.
     */
    @Test
    void catalogueIdentifiersAreNotTranslated() throws Exception {
        for (String language : new String[] {"en", NORWEGIAN}) {
            List<String> spoken = spokenIn(language);
            for (String expected : List.of("M 31, M 51, NGC 3628",
                    "M 45, M 44, NGC 869", "M 13, M 22, NGC 5139")) {
                assertTrue(spoken.stream().anyMatch(s -> s.contains(expected)),
                        expected + " reaches a reader unchanged in "
                                + language);
            }
        }
    }

    /** Every spoken description is a sentence, in both languages. */
    @Test
    void everySpokenDescriptionIsWholeSentences() throws Exception {
        for (String language : new String[] {"en", NORWEGIAN}) {
            List<String> spoken = spokenIn(language);
            assertTrue(spoken.size() >= 17,
                    "the premise: there are descriptions to read in "
                            + language + " - " + spoken.size());
            List<String> malformed = new ArrayList<>();
            for (String said : spoken) {
                if (said.contains("..") || said.contains(" .")
                        || said.matches(".*[a-zA-ZæøåÆØÅ] [A-ZÆØÅ][a-zæøå]+ det .*")
                        || !said.matches(".*[.!?]$")) {
                    malformed.add(said);
                }
            }
            assertEquals(List.of(), malformed,
                    "no doubled mark, no sentence running into the"
                            + " next, and every one ending as a"
                            + " sentence ends, in " + language);
        }
    }

    /** What the dialog speaks, in a language stated explicitly. */
    private static List<String> spokenIn(String language) throws Exception {
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-chartoptions-"
                        + System.nanoTime());
        try {
            List<String> said = new ArrayList<>();
            JComponent[] content = new JComponent[1];
            SwingUtilities.invokeAndWait(() -> content[0] =
                    ChartOptionsDialog.content(
                            new ChartOptionsController(
                                    ChartOptionsStore.forNode(node)),
                            () -> { }, () -> { },
                            InterfaceText.forLanguage(language)));
            SwingUtilities.invokeAndWait(() -> { });
            collect(content[0], said);
            return said;
        } finally {
            node.removeNode();
        }
    }

    private static void collect(java.awt.Container from, List<String> said) {
        for (java.awt.Component child : from.getComponents()) {
            if (child instanceof AbstractButton button
                    && button.getAccessibleContext() != null) {
                String spoken = button.getAccessibleContext()
                        .getAccessibleDescription();
                if (spoken != null && !spoken.isBlank()) {
                    said.add(spoken);
                }
            }
            if (child instanceof java.awt.Container nested) {
                collect(nested, said);
            }
        }
    }
}
