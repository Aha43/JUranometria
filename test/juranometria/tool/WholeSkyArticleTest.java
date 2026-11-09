package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole-sky article (issue #705) is held to the whole sky as ruled
 * on #686 and built by #687 for the release that follows 6.4: every
 * figure is an evidence page byte for byte, every cited number stands
 * in the report it is cited from, every word of the page or the
 * interface it quotes stands where the atlas says it, the two languages
 * carry the same numbers, both are listed where a reader finds them,
 * and the text describes what is destined for 6.5 without claiming that
 * 6.5 has been released. Text is compared with its whitespace collapsed,
 * so a line break in the prose or the report never hides a citation.
 */
class WholeSkyArticleTest {

    private static final Path ENGLISH = Path.of("docs/articles/the-whole-sky-at-one-time.md");
    private static final Path NORWEGIAN = Path.of("docs/articles/hele-himmelen-pa-ett-tidspunkt.md");
    private static final Path IMAGES = Path.of("docs/articles/images");
    private static final String STUDY_DIR = "docs/studies/whole-sky/";

    /** Each figure, and the evidence page it is a byte-for-byte copy of. */
    private static final Map<String, String> FIGURES = Map.of(
            "whole-sky-evening-en-white", "evening-en-white",
            "whole-sky-evening-en-white-grid", "evening-en-white-grid",
            "whole-sky-evening-nb-black", "evening-nb-black",
            "whole-sky-evening-en-white-subdue-off", "evening-en-white-subdue-off",
            "whole-sky-spread-en-white", "spread-en-white",
            "whole-sky-gathering-en-white", "gathering-en-white",
            "whole-sky-journey-open-here", "journey-2-open-here");

    private static final String DECISION = "docs/decisions/all-sky-projection.md";
    private static final String BUILT = "docs/studies/whole-sky/measurements.md";
    private static final String PLATFORM = "docs/studies/whole-sky/platform.md";
    private static final String STUDY = "docs/studies/all-sky/measurements.md";
    private static final String ENGLISH_WORDS = "src/resources/interface-language/en.properties";
    private static final String NORWEGIAN_WORDS = "src/resources/interface-language/nb-NO.properties";

    /** Each cited number, verbatim in both languages, and the report it is read from. */
    private static final List<String[]> CITATIONS = List.of(
            new String[] {"59.91 N, 10.75 E", BUILT},
            new String[] {"2026-12-15", BUILT},
            new String[] {"21:00", BUILT},
            new String[] {"2027-04-25", BUILT},
            new String[] {"2029-01-03", BUILT},
            new String[] {"V 5.0", BUILT},
            new String[] {"V 8.0", BUILT},
            // The evening's nine bodies: longitude from the central
            // meridian, ecliptic latitude, altitude - each row as the
            // report prints it.
            new String[] {"| +153.2 | +2.9 | -40.8", BUILT},
            new String[] {"| +91.4 | +2.7 | +0.7", BUILT},
            new String[] {"| +81.7 | +0.9 | +6.9", BUILT},
            new String[] {"| -2.5 | -0.1 | +50.0", BUILT},
            new String[] {"| -57.4 | -2.5 | +22.7", BUILT},
            new String[] {"| -63.7 | -1.4 | +18.9", BUILT},
            new String[] {"| -87.7 | +0.8 | +2.3", BUILT},
            new String[] {"| -161.4 | +0.0 | -46.7", BUILT},
            new String[] {"| -170.8 | -0.2 | -49.5", BUILT},
            new String[] {"39.82", BUILT},
            new String[] {"65.0°", BUILT},
            new String[] {"0.070%", BUILT},
            new String[] {"+178.2", BUILT},
            new String[] {"9.1", STUDY},
            new String[] {"34.4", STUDY},
            new String[] {"80.9", STUDY},
            new String[] {"32.2", STUDY},
            new String[] {"35.6", STUDY},
            new String[] {"12.0", STUDY},
            new String[] {"16.8", STUDY},
            new String[] {"158", STUDY},
            new String[] {"23.4", DECISION});

    /**
     * Each cited phrase: the report's words, then the English and the
     * Norwegian article's own rendering of them - a number with the
     * language's own unit word or separator, a count in the language's own.
     */
    private static final List<String[]> PHRASES = List.of(
            new String[] {"5-6 of the nine bodies", "**5-6 of the nine bodies**",
                    "**5-6 av de ni legemene**", DECISION},
            new String[] {"9 of 9 every time", "**9 of 9** every time", "**9 av 9** hver gang", DECISION},
            new String[] {"six of the eight bodies other than the Sun",
                    "**six of the eight bodies other than the Sun**",
                    "**seks av de åtte legemene utenom Solen**", BUILT},
            new String[] {"exactly half the map's area", "**exactly half the map's area**",
                    "**nøyaktig halvparten av kartets areal**", DECISION},
            new String[] {"within ±5 degrees", "**within ±5 degrees**", "**innenfor ±5 grader**",
                    DECISION},
            new String[] {"37.7-68.6 degrees", "**37.7-68.6 degrees**", "**37.7-68.6 grader**",
                    DECISION},
            new String[] {"1,440 samples", "**1,440 samples**", "**1 440 punkter**", DECISION},
            new String[] {"| 0 | 0 | 88 | none |", "**88** constellations", "**88** stjernebildene",
                    BUILT},
            new String[] {"1.8 degrees in the spread case", "**1.8°** from the seam",
                    "**1.8°** fra sømmen", DECISION},
            new String[] {"no gap among the nine bodies wider than 64.2 degrees",
                    "wider than **64.2°**", "bredere enn **64.2°**", BUILT},
            new String[] {"the seven planets within 138.7 degrees of ecliptic longitude",
                    "within **138.7°** of ecliptic longitude",
                    "innenfor **138.7°** i ekliptisk lengde", BUILT},
            new String[] {"`evening-en-white` | 7 | mercury, sun |", "**seven of the nine names**",
                    "**sju av de ni navnene**", PLATFORM},
            new String[] {"`evening-en-white-subdue-off` | 9 | - |", "**all nine are named**",
                    "**alle ni er navngitt**", PLATFORM},
            new String[] {"`gathering-en-white` | 4 | moon, neptune, saturn, uranus, venus |",
                    "**four of the nine names**", "**fire av de ni navnene**", PLATFORM},
            new String[] {"0.000000 degrees from Saturn's position",
                    "**0.000000°** from Saturn's position", "**0.000000°** fra Saturns posisjon",
                    BUILT},
            new String[] {"field after Open here: 60 degrees", "the ordinary **60°** chart",
                    "det vanlige kartet på **60°**", BUILT},
            new String[] {"up to 36 times", "**up to 36 times**", "**opptil 36 ganger**", DECISION},
            new String[] {"up to 175 degrees of longitude", "**175 degrees** of longitude",
                    "**175 grader** i lengde", DECISION},
            new String[] {"3.1 degrees from the seam", "**3.1 degrees** from the seam",
                    "**3.1 grader** fra sømmen", DECISION});

    /**
     * What the page itself says, quoted in each language as the as-built
     * report records that language's page: the English words, the
     * Norwegian words, and the report both stand in.
     */
    private static final List<String[]> SAID = List.of(
            new String[] {
                "Whole sky · Hammer · ecliptic north up · ecliptic longitude increases left at centre",
                "Hele himmelen · Hammer · ekliptisk nord opp · ekliptisk lengde øker mot venstre i midten",
                BUILT},
            new String[] {"Central meridian: zenith's ecliptic longitude 65.0° · Stars to V 5.0",
                    "Sentralmeridian: senits ekliptiske lengde 65.0° · Stjerner til V 5.0", BUILT},
            new String[] {"outside the elliptical outline is paper, not sky",
                    "utenfor det elliptiske omrisset er det papir, ikke himmel", BUILT},
            new String[] {"a cartographic symbol, not Neptune's apparent diameter",
                    "et kartografisk symbol, ikke Neptuns tilsynelatende diameter", BUILT},
            new String[] {"too close to tell apart at this field, not drawn",
                    "for nær til å skjelnes i dette feltet, ikke tegnet", BUILT},
            new String[] {"(below the horizon)", "(under horisonten)", BUILT});

    /**
     * The interface's own words, each as its language file holds it:
     * a control is called what the atlas calls it, and a refusal is
     * quoted, not paraphrased.
     */
    private static final List<String[]> WORDS = List.of(
            new String[] {"Whole sky", "Hele himmelen"},
            new String[] {"Open here", "Åpne her"},
            new String[] {"Subdue below horizon", "Demp under horisonten"},
            new String[] {"Reset view", "Tilbakestill visningen"},
            new String[] {"On this page", "På denne siden"},
            new String[] {"Place and Time", "Sted og tid"},
            new String[] {"Emphasis", "Fremheving"},
            new String[] {"Equatorial grid", "Ekvatorialt rutenett"},
            new String[] {"zoom never enters or leaves this view",
                    "zoom går aldri inn i eller ut av denne visningen"},
            new String[] {"a list of the entire sky is not the regional table this promises",
                    "en liste over hele himmelen er ikke den regionale tabellen dette lover"});

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    }

    @Test
    void everyFigureIsTheEvidencePageByteForByte() throws Exception {
        String english = read(ENGLISH);
        String norwegian = read(NORWEGIAN);
        for (Map.Entry<String, String> figure : FIGURES.entrySet()) {
            String name = figure.getKey() + ".png";
            Path copy = IMAGES.resolve(name);
            String source = STUDY_DIR + figure.getValue() + ".png";
            assertTrue(Files.exists(copy), name + " is shipped with the article");
            assertArrayEquals(Files.readAllBytes(Path.of(source)), Files.readAllBytes(copy),
                    name + " is " + source + " byte for byte");
            assertTrue(english.contains("images/" + name), name + " is shown in English");
            assertTrue(norwegian.contains("images/" + name), name + " is shown in Norwegian");
        }
        assertEquals(FIGURES.size(), count(english, "!["), "English shows only held figures");
        assertEquals(FIGURES.size(), count(norwegian, "!["), "Norwegian shows only held figures");
    }

    @Test
    void everyCitedNumberStandsInTheReportItIsCitedFrom() throws IOException {
        String english = read(ENGLISH);
        String norwegian = read(NORWEGIAN);
        for (String[] citation : CITATIONS) {
            assertTrue(read(Path.of(citation[1])).contains(citation[0]),
                    "'" + citation[0] + "' stands in " + citation[1]);
            assertTrue(english.contains(citation[0]), "'" + citation[0] + "' is quoted in English");
            assertTrue(norwegian.contains(citation[0]), "'" + citation[0] + "' is quoted in Norwegian");
        }
        for (String[] phrase : PHRASES) {
            assertTrue(read(Path.of(phrase[3])).contains(phrase[0]),
                    "'" + phrase[0] + "' stands in " + phrase[3]);
            assertTrue(english.contains(phrase[1]), "'" + phrase[1] + "' is said in English");
            assertTrue(norwegian.contains(phrase[2]), "'" + phrase[2] + "' is said in Norwegian");
        }
    }

    @Test
    void whatThePageSaysIsQuotedAsTheAtlasSaysIt() throws IOException {
        String english = read(ENGLISH);
        String norwegian = read(NORWEGIAN);
        for (String[] said : SAID) {
            String report = read(Path.of(said[2]));
            assertTrue(report.contains(said[0]), "'" + said[0] + "' stands in " + said[2]);
            assertTrue(report.contains(said[1]), "'" + said[1] + "' stands in " + said[2]);
            assertTrue(english.contains(said[0]), "'" + said[0] + "' is quoted in English");
            assertTrue(norwegian.contains(said[1]), "'" + said[1] + "' is quoted in Norwegian");
        }
        String englishWords = read(Path.of(ENGLISH_WORDS));
        String norwegianWords = read(Path.of(NORWEGIAN_WORDS));
        for (String[] word : WORDS) {
            assertTrue(englishWords.contains(word[0]), "'" + word[0] + "' is the atlas's English");
            assertTrue(norwegianWords.contains(word[1]), "'" + word[1] + "' is the atlas's Norwegian");
            assertTrue(english.contains(word[0]), "'" + word[0] + "' is used in English");
            assertTrue(norwegian.contains(word[1]), "'" + word[1] + "' is used in Norwegian");
        }
    }

    /**
     * The same thing at the same precision (#705): every figure written
     * in digits in one language is written in the other, as often. A
     * grouped thousand counts alike in both (1,440 and 1 440).
     */
    @Test
    void bothLanguagesCarryTheSameNumbers() throws IOException {
        assertEquals(numbers(read(ENGLISH)), numbers(read(NORWEGIAN)),
                "the digits of the English and the Norwegian text");
        assertEquals(count(read(ENGLISH), " ## "), count(read(NORWEGIAN), " ## "),
                "the two texts have the same sections");
    }

    @Test
    void bothLanguagesAreListedWhereAReaderFindsThem() throws IOException {
        String index = read(Path.of("docs/articles/README.md"));
        assertTrue(index.contains("the-whole-sky-at-one-time.md")
                && index.contains("hele-himmelen-pa-ett-tidspunkt.md"), "the articles index lists both");
        String build = read(Path.of("scripts/build-articles.py"));
        assertTrue(build.contains("the-whole-sky-at-one-time.md")
                && build.contains("hele-himmelen-pa-ett-tidspunkt.md"), "the Pages build publishes both");
    }

    /**
     * Destined for 6.5, not yet released: the text names the release and
     * frames it as the one that follows 6.4, never as published; and it
     * stays inside #705's scope guard - no meteor showers, no paths over
     * time offered, no Milky Way claimed.
     */
    @Test
    void theArticleDescribesWhatIsDestinedForSixFiveWithoutClaimingItReleased() throws IOException {
        String english = read(ENGLISH).toLowerCase(Locale.ROOT);
        String norwegian = read(NORWEGIAN).toLowerCase(Locale.ROOT);
        assertTrue(english.contains("6.5") && english.contains("the release that follows 6.4"));
        assertTrue(norwegian.contains("6.5") && norwegian.contains("utgaven som følger etter 6.4"));
        for (String text : List.of(english, norwegian)) {
            for (String claim : List.of("has been released", "was released", "already released",
                    "er utgitt", "ble utgitt", "allerede utgitt", "naked eye", "det blotte øye",
                    "meteor")) {
                assertFalse(text.contains(claim), "the text does not say '" + claim + "'");
            }
        }
        assertTrue(english.contains("no milky way") && norwegian.contains("ingen melkevei"),
                "the Milky Way is named only as absent");
    }

    private static int count(String text, String part) {
        int found = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + part.length())) {
            found++;
        }
        return found;
    }

    private static List<String> numbers(String text) {
        List<String> found = new ArrayList<>();
        Matcher digits = Pattern.compile("\\d+(?:\\.\\d+)?").matcher(text);
        while (digits.find()) {
            found.add(digits.group());
        }
        Collections.sort(found);
        return found;
    }
}
