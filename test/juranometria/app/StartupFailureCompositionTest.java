package juranometria.app;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.catalog.PackIntegrityException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a failing launch may say in another language, and what it may
 * not (Sprint 33, issue #347).
 *
 * <p>The audit found 73 unique strings a reader can meet in the
 * startup-failure dialog, quoted verbatim from {@code getMessage} at
 * the one moment the application has already let them down. Leaving
 * that surface in English while the menus speak Norwegian would fail
 * the reader exactly where it matters most.
 *
 * <p>But translating the whole sentence is the wrong repair, and
 * would make support harder rather than easier. The message is a
 * composition, and the three parts have different rules:
 *
 * <pre>
 *   localised stable framing          may be translated
 *   + preserved technical cause       must survive exactly
 *   + preserved identifiers and paths must survive exactly
 * </pre>
 *
 * <p>A pathname, a checksum or an operating-system message is
 * evidence. A reader reads it to us over the phone, or pastes it
 * into an issue; a translated copy of it is worth nothing. So these
 * hold the composition itself: the framing is a fixed template with
 * a known shape, and whatever the failure said travels through
 * untouched.
 *
 * <p>These also pin the template's <strong>placeholder
 * signature</strong> - how many insertions there are and what kind
 * of value each carries - so a future Norwegian resource that drops
 * the cause, or writes a path where a count belongs, fails here
 * rather than in front of a reader.
 */
class StartupFailureCompositionTest {

    /**
     * The evidence a failure carries reaches the reader untouched.
     *
     * <p>Each of these is something a reader would be asked to
     * repeat back: a file path, a checksum, the operating system's
     * own words. None may be paraphrased, localised or trimmed.
     */
    @Test
    void theTechnicalCauseSurvivesVerbatim() {
        List<String> evidence = List.of(
                "/Applications/JUranometria.app/Contents/app/bright-sky",
                "sha-256 3f7a0c11d4e5 does not match 9b2e44ff0071",
                "java.util.prefs.BackingStoreException: Couldn't flush");

        for (String said : evidence) {
            String shown = StartupFailure.message(
                    new PackIntegrityException(said));

            assertTrue(shown.contains(said),
                    "the reader is shown the evidence exactly as the"
                            + " loader stated it, because they may have"
                            + " to read it back to someone: expected \""
                            + said + "\" inside\n" + shown);
        }
    }

    /**
     * The framing is a fixed template, not part of the evidence.
     *
     * <p>What may be translated is asserted positively: the opening
     * sentence and the remedy are the application's own words about
     * what happened and what to do, and they are the same for every
     * failure of that kind whatever the cause said.
     */
    @Test
    void theFramingIsStableAcrossDifferentCauses() {
        String first = StartupFailure.message(
                new PackIntegrityException("one cause"));
        String second = StartupFailure.message(
                new PackIntegrityException("an entirely different cause"));

        assertTrue(first.startsWith("JUranometria could not start."),
                "the opening framing is the application's own: " + first);
        assertEquals(framingOf(first), framingOf(second),
                "and the framing does not vary with the cause, which"
                        + " is what makes it translatable once rather"
                        + " than per failure");
    }

    /**
     * The template's placeholder signature, pinned.
     *
     * <p>Three parts, in this order, with the cause in the middle. A
     * translation that drops the cause insertion would silently
     * discard the evidence; one that reorders the parts so a path
     * lands where the framing belongs would read as nonsense. Both
     * are caught here rather than by a reader whose atlas has
     * already failed to open.
     */
    @Test
    void theTemplateKeepsItsThreePartsInOrder() {
        String cause = "a distinctive cause 4815162342";
        String shown = StartupFailure.message(
                new PackIntegrityException(cause));

        int opening = shown.indexOf("JUranometria could not start.");
        int evidence = shown.indexOf(cause);
        int remedy = shown.indexOf("Download the release again");

        assertTrue(opening == 0,
                "the framing opens the message");
        assertTrue(evidence > opening,
                "the cause follows it, not the other way round");
        assertTrue(remedy > evidence,
                "and the remedy comes last, after the reader has seen"
                        + " what went wrong: " + shown);

        // Exactly one insertion of the cause. A template that
        // repeated it would double the evidence; one that dropped it
        // would lose it, and both look fine until someone needs the
        // detail.
        assertEquals(1, shown.split(java.util.regex.Pattern.quote(cause),
                        -1).length - 1,
                "the cause is inserted exactly once");
    }

    /** The message with its cause removed: the framing alone. */
    private static String framingOf(String message) {
        return message.replaceAll("(?s)\\n\\n.*?\\n\\n", "\n\n…\n\n");
    }
}
