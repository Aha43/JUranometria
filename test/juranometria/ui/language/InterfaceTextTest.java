package juranometria.ui.language;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the application says, and what it refuses to say
 * (Sprint 33, issue #350).
 *
 * <p>The mechanism before the hundreds of strings. A resource scheme
 * is easy to get subtly wrong in ways no reader reports: a key shown
 * as itself, a translation that drifts from the English it falls back
 * to, a pattern whose arguments do not match the one beside it. Each
 * of those is held here.
 *
 * <p>The rule the gate set is that <strong>missing interface text
 * falls back to English</strong>. The important half is what that
 * refuses: {@code settings.ok.label} is not a word in any language,
 * and a reader meeting one has been handed a defect to interpret.
 */
class InterfaceTextTest {

    private static final String NORWEGIAN = "nb-NO";

    private static final Path STRINGS =
            Path.of("src/resources/interface-language");

    /** Values in a pattern, by number: {0}, {1}. */
    private static final Pattern ARGUMENT =
            Pattern.compile("\\{(\\d+)[^}]*}");

    // ---- the mechanism ---------------------------------------------

    /** A translated key is said in the translation. */
    @Test
    void aTranslatedKeyIsSaidInTheTranslation() {
        assertEquals("Innstillinger",
                InterfaceText.forLanguage(NORWEGIAN).say("settings.title"));
        assertEquals("Settings",
                InterfaceText.forLanguage("en").say("settings.title"));
    }

    /**
     * An untranslated key falls back to English, not to itself.
     *
     * <p>The contract that lets a translation be incomplete without
     * being broken. nb-NO deliberately does not translate
     * "Latin (IAU)" - a nomenclature is not a language the atlas
     * speaks - so it reads in English on a Norwegian interface, which
     * is correct and visible.
     */
    @Test
    void anUntranslatedKeyFallsBackToEnglishAndNotToItself() {
        InterfaceText norsk = InterfaceText.forLanguage(NORWEGIAN);

        assertEquals("Latin (IAU)",
                norsk.say("settings.language.chart.latin"),
                "a key the translation leaves alone reads in English");
        assertTrue(!norsk.say("settings.language.chart.latin")
                        .contains("settings."),
                "and never as the key itself - a key is not a word in"
                        + " any language, and a reader meeting one has"
                        + " been handed a defect to interpret");
    }

    /**
     * A key English does not define is a mistake, and says so.
     *
     * <p>Not a translation gap: English is what every language falls
     * back to, so a key with no English behind it has nowhere to go.
     * It throws where a developer meets it rather than resolving to
     * something a reader would.
     */
    @Test
    void aKeyWithNoEnglishBehindItRefusesRatherThanResolving() {
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> InterfaceText.forLanguage(NORWEGIAN)
                        .say("settings.invented.nonsense"));
        assertTrue(refused.getMessage().contains("settings.invented.nonsense")
                        && refused.getMessage().contains("English"),
                refused.getMessage());
    }

    /** Values go where the translation puts them, not where English does. */
    @Test
    void aPatternDecidesWhereItsValuesGo() {
        InterfaceText said = InterfaceText.forLanguage(NORWEGIAN,
                path -> new ByteArrayInputStream((path.endsWith("en.properties")
                        ? "t.x = English says {0} then {1}\n"
                        : "t.x = Norsk sier {1} først\n")
                        .getBytes(StandardCharsets.UTF_8)));

        assertEquals("Norsk sier second først",
                said.say("t.x", "first", "second"),
                "a translation may use its arguments in another order,"
                        + " or not use one at all - which is exactly"
                        + " why sentences are not assembled from"
                        + " translated fragments");
    }

    /** A build with no English text at all refuses to start. */
    @Test
    void aBuildWithNoEnglishTextIsReportedRatherThanLimpedThrough() {
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> InterfaceText.forLanguage(NORWEGIAN, path -> null));
        assertTrue(refused.getMessage().contains("English"),
                refused.getMessage());
    }

    /** A duplicated key is refused; one of the two would be unsaid. */
    @Test
    void aKeyDefinedTwiceIsRefused() {
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> InterfaceText.forLanguage("en",
                        path -> new ByteArrayInputStream(
                                "a.b = one\na.b = two\n"
                                        .getBytes(StandardCharsets.UTF_8))));
        assertTrue(refused.getMessage().contains("twice"),
                refused.getMessage());
    }

    /**
     * A pattern may not ask for a value it was not given.
     *
     * <p>The defect this was written for is exact: #350 shipped
     * {@code inspector.size.across.pa} as {@code "{0}\u2032 across  at
     * PA {2}\u00b0"} while its caller passed two arguments. A reader
     * would have seen the placeholder itself.
     *
     * <p>It fails in ENGLISH, before any other language is loaded.
     * The English/Norwegian comparison checks that two patterns agree
     * with each other, which says nothing about whether either agrees
     * with the code that calls it.
     */
    @Test
    void aPatternMayNotAskForAnArgumentItWasNotGiven() {
        InterfaceText english = InterfaceText.forLanguage("en",
                path -> new ByteArrayInputStream(
                        "t.gap = {0} across at PA {2}\n"
                                .getBytes(StandardCharsets.UTF_8)));

        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> english.say("t.gap", "3.4", "17"));
        assertTrue(refused.getMessage().contains("t.gap")
                        && refused.getMessage().contains("{2}")
                        && refused.getMessage().contains("2 arguments"),
                "the failure names the key, the index it asked for and"
                        + " the count it was given: "
                        + refused.getMessage());
    }

    /** What the check must NOT refuse. */
    @Test
    void repeatedAndUnusedArgumentsStayAllowed() {
        InterfaceText said = InterfaceText.forLanguage("en",
                path -> new ByteArrayInputStream(
                        ("t.twice = {0} and {0} again\n"
                                + "t.spare = only {0}\n")
                                .getBytes(StandardCharsets.UTF_8)));

        assertEquals("M 42 and M 42 again", said.say("t.twice", "M 42"),
                "saying a value twice is a choice a language may need");
        assertEquals("only first", said.say("t.spare", "first", "second"),
                "and a translation may legitimately not need a value"
                        + " that English uses");
    }

    /** A malformed pattern is reported against its key. */
    @Test
    void aMalformedPatternNamesTheKeyItCameFrom() {
        InterfaceText said = InterfaceText.forLanguage("en",
                path -> new ByteArrayInputStream(
                        "t.broken = a {0 b\n"
                                .getBytes(StandardCharsets.UTF_8)));

        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> said.say("t.broken", "x"));
        assertTrue(refused.getMessage().contains("t.broken"),
                refused.getMessage());
    }

    // ---- the shipped files ------------------------------------------

    /**
     * No translation invents a key English does not have.
     *
     * <p>An orphan is a string nothing will ever say: the key it was
     * written for has been renamed or removed, and the translation
     * now carries words for a control that does not exist. Silent,
     * and it accumulates.
     */
    @Test
    void noTranslationCarriesAKeyEnglishDoesNotDefine() throws Exception {
        InterfaceText english = InterfaceText.forLanguage("en");
        List<String> orphans = new ArrayList<>();
        for (String tag : shipped()) {
            if (tag.equals("en")) {
                continue;
            }
            for (String key : InterfaceText.forLanguage(tag).keys()) {
                if (!english.englishKeys().contains(key)) {
                    orphans.add(tag + ": " + key);
                }
            }
        }
        assertEquals(List.of(), orphans,
                "a translated key with no English behind it is words"
                        + " for a control that no longer exists, and"
                        + " nothing will ever say them");
    }

    /**
     * Every translation takes the same values as its English.
     *
     * <p>A mismatch is the failure that reaches a reader as garbage:
     * a pattern expecting {0} and {1} handed one value prints the
     * placeholder, and one expecting none silently drops what it was
     * given. Neither is visible until somebody reads that screen in
     * that language.
     */
    @Test
    void everyTranslationTakesTheSameValuesAsItsEnglish() throws Exception {
        InterfaceText english = InterfaceText.forLanguage("en");
        List<String> mismatched = new ArrayList<>();
        int compared = 0;
        for (String tag : shipped()) {
            if (tag.equals("en")) {
                continue;
            }
            InterfaceText said = InterfaceText.forLanguage(tag);
            for (String key : said.keys()) {
                String mine = said.patternIn(key);
                String theirs = english.englishPattern(key);
                if (theirs == null) {
                    continue;
                }
                compared++;
                if (!argumentsOf(mine).equals(argumentsOf(theirs))) {
                    mismatched.add(tag + " " + key + ": takes "
                            + argumentsOf(mine) + ", English takes "
                            + argumentsOf(theirs));
                }
            }
        }
        assertTrue(compared > 20,
                "the premise: there are translated keys to compare - "
                        + compared);
        assertEquals(List.of(), mismatched,
                "a pattern that takes different values from the one it"
                        + " falls back to prints a placeholder or drops"
                        + " a value, and only a reader of that language"
                        + " would ever see it");
    }

    /** Nothing shipped reads like a key. */
    @Test
    void noShippedTextIsAKeyWearingTheLookOfProse() throws Exception {
        List<String> suspicious = new ArrayList<>();
        for (String tag : shipped()) {
            InterfaceText said = InterfaceText.forLanguage(tag);
            for (String key : said.englishKeys()) {
                String text = said.say(key);
                if (text.isBlank() || text.equals(key)
                        || text.matches("[a-z]+(\\.[a-z0-9]+)+")) {
                    suspicious.add(tag + " " + key + " = \"" + text + "\"");
                }
            }
        }
        assertEquals(List.of(), suspicious,
                "every key resolves to words a person could read");
    }

    private static java.util.Set<String> argumentsOf(String pattern) {
        java.util.Set<String> numbers = new TreeSet<>();
        Matcher found = ARGUMENT.matcher(pattern);
        while (found.find()) {
            numbers.add(found.group(1));
        }
        return numbers;
    }

    /** The interface languages this build ships strings for. */
    private static List<String> shipped() throws Exception {
        List<String> tags = new ArrayList<>();
        try (var files = Files.list(STRINGS)) {
            for (Path file : files.sorted().toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".properties")) {
                    tags.add(name.substring(0,
                            name.length() - ".properties".length()));
                }
            }
        }
        assertTrue(tags.contains("en") && tags.contains(NORWEGIAN),
                "the premise: both languages ship strings - " + tags);
        return tags;
    }
}
