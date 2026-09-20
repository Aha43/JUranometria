package juranometria.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One status, in one place, and every companion agreeing with it
 * (Sprint 33, issue #350).
 *
 * <p>Twelve study companions describe the Norwegian interface, and
 * each of them used to carry its own sentence about whether the
 * translation had been reviewed. Twelve sentences is twelve places
 * for the descriptor to be right and the evidence to be wrong, and a
 * stale one reads exactly like a current one - there is nothing in a
 * committed report that says when it was written.
 *
 * <p>`SettingsSheetMain` is the example that made this concrete. Its
 * status line was followed by "Nothing here has been reviewed by a
 * person who reads it", which is prose no status change would have
 * touched: the word would have moved and the sentence beneath it
 * would have gone on denying it.
 *
 * <p>So the claim here is not that the companions <em>say</em>
 * reviewed. It is that they say what the manifest says, whatever
 * that is, and that they cannot say anything else.
 */
class InterfaceLanguageStatusTest {

    private static final Path COMPANIONS =
            Path.of("docs/studies/interface-language");

    /** The twelve companions that describe the Norwegian interface. */
    private static final List<String> EVERY_COMPANION = List.of(
            "about-strings.md", "chartkeyboard-strings.md",
            "chartoptions-strings.md", "export-strings.md",
            "inspector-strings.md", "menu-strings.md",
            "onthispage-strings.md", "page-language-strings.md",
            "placeandtime-strings.md", "settings-strings.md",
            "swing-chrome-strings.md", "toolbar-strings.md");

    @Test
    void theManifestIsWhereTheStatusLives() {
        assertEquals("reviewed", InterfaceLanguageStatus.of("nb-NO"),
                "the owner has been through every surface this sprint"
                        + " covers");
        assertEquals("complete", InterfaceLanguageStatus.of("en"),
                "English is what the application's own words are"
                        + " written in, so there is no translation to"
                        + " review");
        assertEquals("Norsk bokmål",
                InterfaceLanguageStatus.displayName("nb-NO"));
    }

    /**
     * The word is not "verified", and the distinction is the point.
     *
     * <p>In this repository verified means checked against a source
     * that could have contradicted it - which is what the
     * constellation names next door carry. These are original
     * translations written for this application; there is no source
     * to check them against, and borrowing the stronger word would
     * promise corroboration nobody performed.
     */
    @Test
    void reviewedIsNotVerifiedAndTheManifestSaysWhy() throws Exception {
        String manifest = Files.readString(Path.of(
                "src/resources/interface-language/nb-NO.manifest"));
        assertTrue(manifest.contains("status=reviewed"),
                "the descriptor records the status");
        assertTrue(manifest.contains("not\n# \"verified\"")
                        || manifest.contains("deliberately not")
                        || manifest.contains("not \"verified\""),
                "and says, in the file itself, why it is not"
                        + " verified - a reader of the descriptor"
                        + " should not have to find this test");
        assertTrue(manifest.contains("ORIGINAL TRANSLATIONS"),
                "because there is no source to corroborate against");
    }

    /**
     * Every committed companion says what the manifest says.
     *
     * <p>The whole paragraph, not the word: prose around a correct
     * word is exactly what goes stale.
     */
    @Test
    void everyCommittedCompanionAgreesWithTheManifest()
            throws Exception {
        String statement = InterfaceLanguageStatus.statement("nb-NO");
        List<String> silent = new ArrayList<>();
        for (String name : EVERY_COMPANION) {
            Path file = COMPANIONS.resolve(name);
            assertTrue(Files.exists(file), name + " is committed");
            if (!Files.readString(file).contains(statement)) {
                silent.add(name);
            }
        }
        assertEquals(List.of(), silent,
                "a companion that does not carry the manifest's own"
                        + " words has either been hand-edited or was"
                        + " generated before the status moved."
                        + " Regenerate it; do not edit it");
    }

    /**
     * And none of them carries a status the manifest does not state.
     *
     * <p>The mutation this exists for: leaving `**Norsk bokmål —
     * draft.**` in a companion while the manifest says reviewed. The
     * test above would still pass if a generator appended the new
     * paragraph and left the old one standing.
     */
    @Test
    void noCompanionCarriesAStatusTheManifestDoesNotState()
            throws Exception {
        String current = InterfaceLanguageStatus.of("nb-NO");
        String name = InterfaceLanguageStatus.displayName("nb-NO");
        List<String> stale = new ArrayList<>();
        for (String companion : EVERY_COMPANION) {
            String said =
                    Files.readString(COMPANIONS.resolve(companion));
            for (String other : List.of("draft", "reviewed",
                    "complete")) {
                if (other.equals(current)) {
                    continue;
                }
                if (said.contains("**" + name + " — " + other + ".**")
                        || said.contains("Norwegian is **" + other
                                + "**")) {
                    stale.add(companion + ": " + other);
                }
            }
        }
        assertEquals(List.of(), stale,
                "a second status paragraph left standing beside the"
                        + " current one is worse than none: a reader"
                        + " cannot tell which is the live claim");
    }

    /** No generator may state a status of its own. */
    @Test
    void noGeneratorWritesAStatusIntoItsOwnReport() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (var files = Files.walk(Path.of("src/juranometria/tool"))) {
            for (Path file : files.filter(f ->
                    f.toString().endsWith("SheetMain.java")).toList()) {
                String code = Files.readString(file);
                if (code.contains("Norwegian is **")
                        || code.contains("**draft**")
                        || code.contains("**reviewed**")) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }
        assertEquals(List.of(), offenders,
                "the status is read from the manifest through"
                        + " InterfaceLanguageStatus. A literal here is"
                        + " a second source of truth, and the one that"
                        + " will not be updated");
    }

    /**
     * An unrecognised status refuses rather than printing itself.
     *
     * <p>The vocabulary is deliberately small because each word says
     * who has read the translation and on what basis. A fourth would
     * say nothing while looking like it said something.
     */
    @Test
    void anUnrecognisedStatusIsRefused() {
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> InterfaceLanguageStatus.statement("no-such-tag"));
        assertTrue(refused.getMessage().contains("no-such-tag"),
                "and says which language it could not find: "
                        + refused.getMessage());
    }
}
