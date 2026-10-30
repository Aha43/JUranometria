package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Uranus article (issue #660) is held to Uranus as released on main
 * for 6.0, as the Mars article is to 5.5: every figure is an evidence page
 * byte for byte, every cited number stands in the report it is cited
 * from, both languages are listed where a reader finds them, and the text
 * describes Uranus and nothing the planet after it adds. Text is compared
 * with its whitespace collapsed, so a line break in the prose or the
 * report never hides a citation.
 */
class UranusArticleTest {

    private static final Path ENGLISH = Path.of("docs/articles/uranus-on-the-page.md");
    private static final Path NORWEGIAN = Path.of("docs/articles/uranus-pa-siden.md");
    private static final Path IMAGES = Path.of("docs/articles/images");
    private static final String CHART_DIR = "docs/studies/uranus-on-the-chart/";

    /** Each figure and the evidence page it is a byte-for-byte copy of. */
    private static final List<String> FIGURES = List.of("opposition-2026-8-nb", "opposition-2026-3-dark",
            "conjunction-2026-1", "opposition-2026-0.25-dark", "moons-widest-2052-1",
            "moons-widest-2052-0.25-nb", "moons-widest-2052-8", "miranda-behind-2005-0.25",
            "miranda-in-front-2005-0.25-dark", "ariel-shadow-2006-0.25-nb", "before-the-moons-1999-0.25");

    private static final String HANDOVER = "docs/decisions/uranus-handover.md";
    private static final String CARTOGRAPHY = "docs/decisions/uranus-cartography.md";
    private static final String CARTOGRAPHY_REPORT = "docs/studies/uranus-cartography/measurements.md";
    private static final String CHART = "docs/studies/uranus-on-the-chart/measurements.md";
    private static final String ACCURACY = "docs/studies/uranus-system/service-accuracy.md";

    /** Each cited number, verbatim in both languages, and the report it is read from. */
    private static final List<String[]> CITATIONS = List.of(
            new String[] {"3.34-4.08″", CARTOGRAPHY_REPORT},
            new String[] {"DE442", HANDOVER},
            new String[] {"1900–2100", HANDOVER},
            new String[] {"0.0018″", HANDOVER},
            new String[] {"0.3779″", HANDOVER},
            new String[] {"0.49 km", HANDOVER},
            new String[] {"45.136 km", HANDOVER},
            new String[] {"0.0035″", HANDOVER},
            new String[] {"50 km", HANDOVER},
            new String[] {"URA182.21", HANDOVER},
            new String[] {"2000–2100", HANDOVER},
            new String[] {"2027-06-28", HANDOVER},
            new String[] {"0.0033″", HANDOVER},
            new String[] {"43.529 km", HANDOVER},
            new String[] {"0.0034″", HANDOVER},
            new String[] {"0.0051°", HANDOVER},
            new String[] {"0.3194″", HANDOVER},
            new String[] {"0.0008″", HANDOVER},
            new String[] {"99.92 %", HANDOVER},
            new String[] {"3.1825°", HANDOVER},
            new String[] {"82.6°", HANDOVER},
            new String[] {"0.37″", HANDOVER},
            new String[] {"2026-10-27 12:00", HANDOVER},
            new String[] {"18.5757 AU", HANDOVER},
            new String[] {"3.794″", HANDOVER},
            new String[] {"99.983 %", HANDOVER},
            new String[] {"1.499°", HANDOVER},
            new String[] {"289.79°", HANDOVER},
            new String[] {"8.05°", HANDOVER},
            new String[] {"0.1133″–10.3213″", HANDOVER},
            new String[] {"0.964″–46.4638″", HANDOVER},
            new String[] {"0.13″", HANDOVER},
            new String[] {"+37 minutes", HANDOVER},
            new String[] {"6 px", CARTOGRAPHY},
            new String[] {"3 px", CARTOGRAPHY},
            new String[] {"12 px", CARTOGRAPHY},
            new String[] {"0.170°", CARTOGRAPHY},
            new String[] {"0.005 px", CARTOGRAPHY},
            new String[] {"0.093 px", CARTOGRAPHY},
            new String[] {"30.8 %", CARTOGRAPHY},
            new String[] {"67.0 %", CARTOGRAPHY},
            new String[] {"66.9 %", CARTOGRAPHY},
            new String[] {"64.2 %", CARTOGRAPHY},
            new String[] {"4.08 px", CARTOGRAPHY},
            new String[] {"1.08 px", CARTOGRAPHY},
            new String[] {"6.69-8.16″", CARTOGRAPHY_REPORT},
            new String[] {"0.34°", CARTOGRAPHY_REPORT},
            new String[] {"0.5°", CHART},
            new String[] {"0.25°", CHART},
            new String[] {"2026-11-25", CHART},
            new String[] {"3.82″", CHART},
            new String[] {"73.8°", CHART},
            new String[] {"2026-05-22", ACCURACY},
            new String[] {"0.2°", CHART},
            new String[] {"2052-03-23", CHART},
            new String[] {"2005-05-28", CHART},
            new String[] {"2005-03-19", CHART},
            new String[] {"2006-02-04", CHART},
            new String[] {"1999-12-31", CHART});

    /**
     * Each cited phrase: the report's words, then the English and the
     * Norwegian article's own rendering of them - a grouped number with
     * the language's own separator, a count or a word in the language's own.
     */
    private static final List<String[]> PHRASES = List.of(
            new String[] {"387,072", "**387,072 bytes**", "**387 072 bytes**", HANDOVER},
            new String[] {"38,055,936", "**38,055,936 bytes**", "**38 055 936 bytes**", HANDOVER},
            new String[] {"4,864.7 km", "**4,864.7 km**", "**4 864.7 km**", HANDOVER},
            new String[] {"59 Horizons requests", "**59 Horizons requests**", "**59 Horizons-forespørsler**",
                    HANDOVER},
            new String[] {"5,577 of 5,579", "**5,577 of 5,579**", "**5 577 av 5 579**", HANDOVER},
            new String[] {"5,579 of 5,579", "**5,579 of 5,579**", "**5 579 av 5 579**", HANDOVER},
            new String[] {"tipped towards the observer", "tipped towards us", "vendt mot oss", HANDOVER},
            new String[] {"51149 km", "**51,149 km**", "**51 149 km**", CARTOGRAPHY_REPORT},
            new String[] {"a tenth of a pixel", "**a tenth of a pixel**", "**en tidel av en piksel**", CHART},
            new String[] {"12 of 12", "**12 of 12**", "**12 av 12**", CHART},
            new String[] {"too close to tell apart at this field, not drawn",
                    "*too close to tell apart at this field, not drawn*",
                    "*for nær til å skjelnes i dette feltet, ikke tegnet*", CHART},
            new String[] {"behind Uranus, not drawn", "*behind Uranus, not drawn*", "*bak Uranus, ikke tegnet*",
                    CHART});

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    }

    @Test
    void everyFigureIsTheEvidencePageByteForByte() throws Exception {
        String english = read(ENGLISH);
        String norwegian = read(NORWEGIAN);
        for (String figure : FIGURES) {
            String name = "uranus-" + figure + ".png";
            Path copy = IMAGES.resolve(name);
            assertTrue(Files.exists(copy), name + " is shipped with the article");
            assertArrayEquals(Files.readAllBytes(Path.of(CHART_DIR + figure + ".png")),
                    Files.readAllBytes(copy), name + " is " + CHART_DIR + figure + ".png byte for byte");
            assertTrue(english.contains("images/" + name), name + " is shown in English");
            assertTrue(norwegian.contains("images/" + name), name + " is shown in Norwegian");
        }
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
    void bothLanguagesAreListedWhereAReaderFindsThem() throws IOException {
        String index = read(Path.of("docs/articles/README.md"));
        assertTrue(index.contains("uranus-on-the-page.md") && index.contains("uranus-pa-siden.md"),
                "the articles index lists both");
        String build = read(Path.of("scripts/build-articles.py"));
        assertTrue(build.contains("uranus-on-the-page.md") && build.contains("uranus-pa-siden.md"),
                "the Pages build publishes both");
    }

    /**
     * Uranus as released for 6.0, as the Mars article described released
     * 5.5: the planet after Uranus is not this article's to describe (its
     * own article opens the next road), the rings are drawn by no release,
     * and nothing in it claims surface detail or naked-eye ease.
     */
    @Test
    void theArticleDescribesUranusForSixAndNothingElse() throws IOException {
        for (Path article : List.of(ENGLISH, NORWEGIAN)) {
            String text = read(article).toLowerCase(Locale.ROOT);
            assertTrue(text.contains("6.0"), article + " names the release it describes");
            assertFalse(text.contains("naked eye") || text.contains("det blotte øye"),
                    article + " promises no naked-eye ease");
            for (String later : List.of("neptun", "triton", "nereid")) {
                assertFalse(text.contains(later), article + " says nothing of " + later
                        + ", which the next article describes");
            }
        }
    }
}
