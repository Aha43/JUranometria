package juranometria.ui.language;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import juranometria.app.ChartKeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart keyboard's words, and the boundary they live behind
 * (Sprint 33, issue #350).
 *
 * <p>These are the claims that make the ownership change real rather
 * than tidy: that no switch can reach a reader without words, that a
 * language's silence about a note is different from a gap in its pack,
 * and that nothing recovers an identity and prints it.
 */
class ChartKeyboardTextTest {

    private static final InterfaceText EN =
            InterfaceText.forLanguage("en");
    private static final ChartKeyboardText SAID =
            ChartKeyboardText.in(EN);

    @Test
    void everyRegisteredSwitchHasEnglishWords() {
        assertEquals(20, ChartKeys.toggles().size(),
                "the premise: the registry still holds twenty");
        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            String label = SAID.label(toggle);
            assertTrue(label != null && !label.isBlank(),
                    toggle.id() + " has a name");
            assertTrue(!label.contains(toggle.id()),
                    toggle.id() + " is named, not identified: " + label);
            // note() either answers prose or answers null, and either
            // way it does not throw - which is the whole claim.
            SAID.note(toggle);
        }
        for (ChartKeys.Refusal refusal : ChartKeys.refused()) {
            assertTrue(!SAID.refusedRow(refusal).isBlank(),
                    refusal.id() + " says why it is refused");
            assertTrue(!SAID.refusedSpoken(refusal).isBlank(),
                    "and says it to a screen reader too");
        }
    }

    /**
     * A switch with no words fails loudly.
     *
     * <p>{@code InterfaceText} answers an unknown key with the key,
     * which is right for a surface that would rather show something
     * than nothing and wrong here: these are a fixed set a test can
     * walk, and {@code chartkeyboard.chart.newLayer.label} on a row
     * is a defect shaped like a word.
     */
    @Test
    void aSwitchWithNoWordsIsRefusedRatherThanNamedAfterItsId() {
        ChartKeys.Toggle invented =
                new ChartKeys.Toggle("chart.notInAnyPack", 'Q', null, true);
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> SAID.label(invented));
        assertTrue(thrown.getMessage().contains("chart.notInAnyPack"),
                "and says which switch: " + thrown.getMessage());
    }

    /**
     * Silence about a note is written down, not left out.
     *
     * <p>Three of the twenty explain why their letter differs from
     * the one on their own control. That is a fact about <em>this
     * language's</em> mnemonics: another language's labels may collide
     * differently, or not at all. So the pack declares a note or
     * declares {@link ChartKeyboardText#NO_NOTE}, and a key simply
     * missing is a mistake somebody is told about rather than a
     * seventeen-way silent shrug.
     */
    @Test
    void anIntentionalSilenceIsNotTheSameAsAMissingKey() {
        List<String> noted = new ArrayList<>();
        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            if (SAID.note(toggle) != null) {
                noted.add(toggle.id());
            }
        }
        assertEquals(List.of("chart.flamsteedNumbers",
                        "chart.magnitudeKey", "chart.blackSky"), noted,
                "exactly the three whose letter differs from their own"
                        + " control's say so");
        assertNull(SAID.note(ChartKeys.toggle("chart.galaxies")),
                "and a switch with nothing to explain answers null");

        // The distinction the constant exists for: declared silence
        // reads as silence, an absent key does not read at all.
        ChartKeys.Toggle invented =
                new ChartKeys.Toggle("chart.noNoteDeclared", 'Q', null, true);
        assertThrows(IllegalStateException.class,
                () -> SAID.note(invented),
                "a pack that says nothing about a note is a pack with"
                        + " a hole in it, not a switch without a note");
    }

    /** Every master declares the two forms a sentence needs of it. */
    @Test
    void everyMasterDeclaresTheFormsASentenceNeeds() {
        Set<String> masters = new LinkedHashSet<>();
        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            if (toggle.dependsOn() != null) {
                masters.add(toggle.dependsOn());
            }
        }
        assertEquals(Set.of("chart.deepSkyObjects",
                        "chart.constellationFigures"), masters,
                "the premise: two switches are waited for");
        for (String id : masters) {
            ChartKeys.Toggle master = ChartKeys.toggle(id);
            assertNotNull(master, id + " is a switch");
            assertTrue(!SAID.masterToEnable(master).isBlank(),
                    id + " has a form that reads after \"enable\"");
            assertTrue(!SAID.masterSatisfied(master).isBlank(),
                    id + " has a clause saying it is on");
        }
        // The repair: "deep-sky objects is on" was not English.
        assertTrue(SAID.masterSatisfied(
                        ChartKeys.toggle("chart.deepSkyObjects"))
                        .contains("are on"),
                "and the clause agrees in number, which is why the"
                        + " clause is translated whole: "
                        + SAID.masterSatisfied(ChartKeys.toggle(
                                "chart.deepSkyObjects")));
    }

    /**
     * No reader ever meets an identity.
     *
     * <p>The ids are a namespace the atlas talks to itself in. Every
     * sentence this adapter can produce, in every state a switch can
     * be in, is checked for all twenty of them and for the refusal's
     * id - because a fallback that printed an id would look like a
     * word to everything except a reader.
     */
    @Test
    void nothingAReaderIsShownOrToldContainsAnIdentity() {
        List<String> everything = new ArrayList<>();
        everything.add(SAID.title());
        everything.add(SAID.explain());
        everything.add(SAID.heading());
        everything.add(SAID.instruction());
        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            for (boolean on : new boolean[] {true, false}) {
                everything.add(SAID.row(toggle, SAID.state(on)));
                everything.add(SAID.spoken(toggle, on));
                everything.add(SAID.announce(toggle, on));
            }
            if (toggle.dependsOn() != null) {
                ChartKeys.Toggle master =
                        ChartKeys.toggle(toggle.dependsOn());
                everything.add(SAID.row(toggle,
                        SAID.stateUnavailable(master)));
                everything.add(SAID.spokenUnavailable(toggle, master));
                everything.add(SAID.announceUnavailable(toggle, master));
            }
        }
        for (ChartKeys.Refusal refusal : ChartKeys.refused()) {
            everything.add(SAID.refusedRow(refusal));
            everything.add(SAID.refusedSpoken(refusal));
        }
        assertTrue(everything.size() >= 130,
                "the premise: every state of every switch was asked - "
                        + everything.size());

        List<String> leaked = new ArrayList<>();
        for (String said : everything) {
            for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
                if (said.contains(toggle.id())) {
                    leaked.add(toggle.id() + " in \"" + said + "\"");
                }
            }
            for (ChartKeys.Refusal refusal : ChartKeys.refused()) {
                if (said.contains(refusal.id())) {
                    leaked.add(refusal.id() + " in \"" + said + "\"");
                }
            }
            assertTrue(!said.contains(ChartKeyboardText.NO_NOTE),
                    "and a declared silence stays behind the adapter: "
                            + said);
        }
        assertEquals(List.of(), leaked,
                "nothing a reader is shown or told carries an"
                        + " identity");
    }

    /**
     * The registry holds no words, and the type says so.
     *
     * <p>Read off the record's own components rather than off a list
     * written down here, so putting {@code label} or {@code note}
     * back fails this whether or not anybody remembers to update a
     * test.
     */
    @Test
    void theRegistryCarriesIdentityAndBindingAndNothingElse() {
        List<String> members = new ArrayList<>();
        for (var component : ChartKeys.Toggle.class.getRecordComponents()) {
            members.add(component.getName());
        }
        assertEquals(List.of("id", "key", "dependsOn", "persistent"),
                members,
                "ChartKeys.Toggle is binding and policy. A label or a"
                        + " note here is prose in an application"
                        + " registry, and the next consumer would read"
                        + " it (#350)");
        assertEquals(List.of("id"), List.of(ChartKeys.Refusal.class
                        .getRecordComponents()[0].getName()),
                "and a refusal is an identity, not an English word"
                        + " doubling as a component name");
    }

    /**
     * Nothing bends a word to fit a sentence.
     *
     * <p>{@code ChartKeyboard} lowercased a master's label with
     * {@code Locale.ROOT} to drop it into "enable X first". Whether a
     * noun is lower case mid-sentence is a language's rule - German
     * would be wrong outright - and {@code Locale.ROOT} is the locale
     * for data nobody reads aloud. Held as a source contract because
     * English happens to want lower case there, so no English output
     * would change if it came back.
     */
    @Test
    void noSurfaceChangesTheCaseOfAWordToMakeItFit() throws Exception {
        for (String file : List.of(
                "src/juranometria/app/ChartKeyboard.java",
                "src/juranometria/ui/language/ChartKeyboardText.java")) {
            String code = Files.readString(Path.of(file))
                    .replaceAll("(?s)/\\*.*?\\*/", " ")
                    .replaceAll("(?m)//.*$", " ");
            assertTrue(!code.contains("toLowerCase"),
                    file + " bends no word to fit a sentence");
            // Character.toUpperCase on a key is notation - a
            // keystroke is the same keystroke in every language -
            // and is the only case change allowed here. A String
            // being re-cased is a word being re-cased.
            assertTrue(!code.contains("String.valueOf")
                            || !code.contains(".toUpperCase("),
                    file + " re-cases no string");
        }
    }

    /**
     * The English the palette is built from, in one place.
     *
     * <p>A wording change lands here first, where it can be read as a
     * sentence rather than inferred from a screenshot.
     */
    @Test
    void theEnglishReadsAsEnglish() {
        ChartKeys.Toggle galaxies = ChartKeys.toggle("chart.galaxies");
        ChartKeys.Toggle deepSky = ChartKeys.toggle("chart.deepSkyObjects");
        assertEquals("Galaxies, unavailable until deep-sky objects are"
                        + " on, ⌘K then G",
                SAID.spokenUnavailable(galaxies, deepSky),
                "the stress case: a name, a condition naming another"
                        + " switch, and two keystrokes");
        assertEquals("G   Galaxies — unavailable — enable deep-sky"
                        + " objects first",
                SAID.row(galaxies, SAID.stateUnavailable(deepSky)),
                "and the row a reader sees");
        assertEquals("M   Flamsteed numbers — on  (F names the figures)",
                SAID.row(ChartKeys.toggle("chart.flamsteedNumbers"),
                        SAID.state(true)),
                "a note, in brackets the language chose");
        assertEquals("Your meridian on — for this session.",
                SAID.announce(ChartKeys.toggle("module.meridian"), true),
                "what the palette says about something it will not"
                        + " remember");
        assertEquals("Equatorial grid on — saved.",
                SAID.announce(ChartKeys.toggle("chart.equatorialGrid"),
                        true),
                "and about something it will");
        assertEquals("Press a letter to switch a layer. Escape or ⌘K"
                        + " closes the palette.", SAID.instruction(),
                "the instruction no longer reads as though the letters"
                        + " changed nothing either");
        assertEquals("The chart's layers, their shortcuts where"
                        + " available, and their current state. Press a"
                        + " listed letter to switch a layer, or Escape"
                        + " or ⌘K to close.", SAID.explain(),
                "and the description covers both what the palette"
                        + " switches and the row it deliberately does"
                        + " not: it claimed to list \"every layer the"
                        + " chart can show\" while showing the zenith,"
                        + " which has no shortcut at all");
    }
}
