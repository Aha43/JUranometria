package juranometria.app;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.ui.language.StartupText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The failure reporter, in two languages and under every way of
 * failing to be in either (Sprint 33, issue #350).
 *
 * <p>This is the surface that runs when everything else has failed,
 * and two of the three failures it reports are reasons it might fail
 * itself. So the claims here are mostly about what happens when
 * something is missing, blank, broken or throwing - and the answer is
 * always a complete English message rather than a half-translated
 * one or a second exception.
 */
class StartupLanguageTest {

    private static final String CAUSE = "kaputt-marker-9271";

    /** A failure whose message is unmistakable in the output. */
    private static Throwable bundledData() {
        return new juranometria.catalog.PackIntegrityException(CAUSE);
    }

    private static Throwable settings() {
        return new java.util.prefs.BackingStoreException(CAUSE);
    }

    private static Throwable unrecognised() {
        return new IllegalStateException(CAUSE);
    }

    // ---- the Norwegian message, whole -----------------------------

    @Test
    void eachClassifiedFailureReadsItsOwnNorwegianDocument() {
        for (Throwable failure : List.of(bundledData(), settings(),
                unrecognised())) {
            String said = StartupFailure.message(failure, "nb-NO",
                    StartupText.PACKAGED);
            assertTrue(said.startsWith("JUranometria kunne ikke starte."),
                    "the headline is Norwegian: " + first(said));
            assertTrue(!said.contains("could not start"),
                    "and English is not also present: " + first(said));
            assertEquals(1, occurrences(said, CAUSE),
                    "the technical cause appears exactly once, between"
                            + " the two stable parts");
        }

        // Each failure selects its matching document, not another's.
        assertTrue(StartupFailure.message(bundledData(), "nb-NO",
                        StartupText.PACKAGED)
                        .contains("SHA-256-sjekksummen"),
                "the bundled-data failure reads the bundled-data"
                        + " document");
        assertTrue(StartupFailure.message(settings(), "nb-NO",
                        StartupText.PACKAGED)
                        .contains("PlistBuddy"),
                "the settings failure reads the settings document");
        assertTrue(StartupFailure.message(unrecognised(), "nb-NO",
                        StartupText.PACKAGED)
                        .contains("/issues"),
                "and the unrecognised one reads its own");
    }

    /**
     * The characters a one-line resource grammar could not have
     * carried.
     *
     * <p>This is why the remedies are documents. A backslash stays a
     * backslash, a newline stays a newline, and a shell command
     * spanning two lines arrives whole.
     */
    @Test
    void realNewlinesBackslashesAndCommandsSurviveIntoTheMessage() {
        String said = StartupFailure.message(settings(), "nb-NO",
                StartupText.PACKAGED);
        assertTrue(said.contains(
                        "HKEY_CURRENT_USER\\Software\\JavaSoft\\Prefs"
                                + "\\juranometria"),
                "the Windows registry path keeps its real backslashes");
        assertTrue(said.contains("/usr/libexec/PlistBuddy -c "
                        + "'Delete \":/:juranometria/\"' \\\n"),
                "the macOS command keeps its quotes, its trailing"
                        + " backslash and its line break");
        assertTrue(said.contains("~/.java/.userPrefs/juranometria"),
                "and the Linux path arrives whole");
        assertTrue(said.split("\n").length > 10,
                "the document is many lines, not one: "
                        + said.split("\n").length);
    }

    // ---- every way of not being in that language ------------------

    @Test
    void aMissingDocumentMakesTheWholeMessageEnglish() {
        StartupText.Documents withoutSettings = path ->
                path.endsWith("startup-settings.txt") ? null
                        : StartupText.PACKAGED.open(path);

        String said = StartupFailure.message(settings(), "nb-NO",
                withoutSettings);
        assertEnglishThroughout(said, "a missing document");

        // And the others are unaffected: the atomic rule is per
        // displayed failure, not per language.
        assertTrue(StartupFailure.message(bundledData(), "nb-NO",
                        withoutSettings)
                        .startsWith("JUranometria kunne ikke starte."),
                "a failure whose own document is present is still"
                        + " Norwegian");
    }

    @Test
    void aBlankDocumentIsTreatedAsNoDocument() {
        StartupText.Documents blank = path ->
                path.endsWith(".txt")
                        ? new ByteArrayInputStream(
                                "   \n\n  ".getBytes(StandardCharsets.UTF_8))
                        : StartupText.PACKAGED.open(path);
        assertEnglishThroughout(
                StartupFailure.message(settings(), "nb-NO", blank),
                "a blank document");
    }

    @Test
    void aReaderThatThrowsIsAnsweredWithEnglishRatherThanAnException() {
        StartupText.Documents hostile = path -> {
            throw new IllegalStateException("the reader is broken too");
        };
        assertEnglishThroughout(
                StartupFailure.message(unrecognised(), "nb-NO", hostile),
                "a reader that throws");

        StartupText.Documents midStream = path -> new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("half a document");
            }
        };
        assertEnglishThroughout(
                StartupFailure.message(unrecognised(), "nb-NO", midStream),
                "a stream that fails while being read");
    }

    @Test
    void aLanguageWithNoHeadlineOfItsOwnGetsEnglishThroughout() {
        // English defines the headline for pack parity and never
        // uses it: no en.startup-*.txt ships, so the atomic rule
        // finds no document and the embedded English is used entire.
        assertEnglishThroughout(
                StartupFailure.message(settings(), "en",
                        StartupText.PACKAGED),
                "English itself, whose remedies are embedded rather"
                        + " than shipped as documents");
        assertEnglishThroughout(
                StartupFailure.message(settings(), null,
                        StartupText.PACKAGED),
                "no language at all - the preference store is one of"
                        + " the things that may have failed");
        assertEnglishThroughout(
                StartupFailure.message(settings(), "zz-ZZ",
                        StartupText.PACKAGED),
                "a language the atlas has never heard of");

        // The atomic rule's own claim: a language WITH a document but
        // WITHOUT a headline of its own must not borrow English's.
        StartupText.Documents norwegianDocumentOnly = path ->
                StartupText.PACKAGED.open(path.replace("zz-ZZ", "nb-NO"));
        assertEnglishThroughout(
                StartupFailure.message(settings(), "zz-ZZ",
                        norwegianDocumentOnly),
                "a language whose remedy reads but whose headline is"
                        + " not its own");
    }

    /** The whole displayed failure is English, and complete. */
    private static void assertEnglishThroughout(String said, String when) {
        assertTrue(said.startsWith("JUranometria could not start."),
                when + ": the English headline");
        assertTrue(!said.contains("kunne ikke starte"),
                when + ": and no Norwegian headline above it");
        assertTrue(!said.contains("Meld den gjerne")
                        && !said.contains("slett katalogen")
                        && !said.contains("SHA-256-sjekksummen"),
                when + ": and no Norwegian remedy beneath it");
        assertTrue(said.contains("This is the store of your own saved")
                        || said.contains("The application verifies its")
                        || said.contains("This is not a failure"),
                when + ": the complete English remedy is present");
        assertEquals(1, occurrences(said, CAUSE),
                when + ": with the cause still exactly once");
    }

    // ---- what must ship ------------------------------------------

    @Test
    void allThreeNorwegianDocumentsShipAndAreNotBlank() throws Exception {
        for (StartupText.Remedy remedy : StartupText.Remedy.values()) {
            String path = remedy.documentFor("nb-NO");
            try (InputStream stream =
                         StartupText.PACKAGED.open(path)) {
                assertTrue(stream != null, path + " ships");
                String text = new String(stream.readAllBytes(),
                        StandardCharsets.UTF_8);
                assertTrue(!text.isBlank(), path + " says something");
                assertTrue(text.length() > 120,
                        path + " is a remedy, not a word: "
                                + text.length() + " characters");
            }
            assertTrue(Files.exists(Path.of("src" + path)),
                    "and is in the source tree at src" + path);
        }
    }

    /**
     * The English is embedded, not read.
     *
     * <p>A source contract: the last line of defence may not depend
     * on opening anything, because every kind of opening is something
     * this surface may be reporting the failure of.
     */
    @Test
    void theEnglishFallbackIsEmbeddedInTheReporter() throws Exception {
        String code = Files.readString(
                        Path.of("src/juranometria/app/StartupFailure.java"))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");
        assertTrue(code.contains("\"JUranometria could not start.\""),
                "the headline is a literal here");
        assertTrue(code.contains("The application verifies its bundled"),
                "and so is every remedy");
        assertTrue(!code.contains("getResourceAsStream"),
                "and the reporter opens nothing of its own");
    }

    private static int occurrences(String said, String needle) {
        int count = 0;
        for (int at = said.indexOf(needle); at >= 0;
                at = said.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }

    private static String first(String said) {
        int end = said.indexOf('\n');
        return end < 0 ? said : said.substring(0, end);
    }
}
