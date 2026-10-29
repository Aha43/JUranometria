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
 * The Mars article (issue #639) is held to released 5.5.0, as the Saturn
 * article is to 5.4.0: every figure is an evidence page byte for byte -
 * each unchanged since tag {@code v5.5.0} when the article was written -
 * every cited number stands in the report it is cited from, both
 * languages are listed where a reader finds them, and the text describes
 * 5.5 and nothing a later release adds. Text is compared with its
 * whitespace collapsed, so a line break in the prose or the report never
 * hides a citation.
 */
class MarsArticleTest {

    private static final Path ENGLISH = Path.of("docs/articles/mars-on-the-page.md");
    private static final Path NORWEGIAN = Path.of("docs/articles/mars-pa-siden.md");
    private static final Path IMAGES = Path.of("docs/articles/images");
    private static final String CHART_DIR = "docs/studies/mars-on-the-chart/";

    /** Each figure and the evidence page it is a byte-for-byte copy of. */
    private static final List<String> FIGURES = List.of("opposition-2003-8-nb", "opposition-2003-1",
            "behind-the-sun-1912-1", "opposition-2003-0.25-dark", "gibbous-2018-0.25",
            "deimos-wide-2018-0.25", "deimos-wide-2018-8", "phobos-in-front-2001-0.25",
            "phobos-shadow-2018-0.25", "after-the-moons-2051-0.25");

    private static final String HANDOVER = "docs/decisions/mars-handover.md";
    private static final String CARTOGRAPHY = "docs/decisions/mars-cartography.md";
    private static final String TABLE = "docs/studies/mars-system/table-measurements.md";
    private static final String CHART = "docs/studies/mars-on-the-chart/measurements.md";

    /** Each cited number, verbatim in both languages, and the report it is read from. */
    private static final List<String[]> CITATIONS = List.of(
            new String[] {"3.50″", HANDOVER},
            new String[] {"25.10″", HANDOVER},
            new String[] {"1900–2100", HANDOVER},
            new String[] {"0.210 m", HANDOVER},
            new String[] {"1.2 GB", HANDOVER},
            new String[] {"2027-06-28", HANDOVER},
            new String[] {"0.0004″", HANDOVER},
            new String[] {"0.179 km", HANDOVER},
            new String[] {"0.0006″", HANDOVER},
            new String[] {"4.0278″", HANDOVER},
            new String[] {"0.346″", HANDOVER},
            new String[] {"0.130″", HANDOVER},
            new String[] {"83.86 %", HANDOVER},
            new String[] {"0.013″", HANDOVER},
            new String[] {"0.26″–34.22″", HANDOVER},
            new String[] {"0.47″–84.79″", HANDOVER},
            new String[] {"35.02″", HANDOVER},
            new String[] {"86.77″", HANDOVER},
            new String[] {"0.1″", HANDOVER},
            new String[] {"2026-10-25 12:00", TABLE},
            new String[] {"09h 11m 17.0s", TABLE},
            new String[] {"+17° 45′ 47″", TABLE},
            new String[] {"16.98°", TABLE},
            new String[] {"1.484474 AU", TABLE},
            new String[] {"6.3″ / 6.3″", TABLE},
            new String[] {"89.6 %", TABLE},
            new String[] {"76.8°", TABLE},
            new String[] {"108°", TABLE},
            new String[] {"0.2°", TABLE},
            new String[] {"19.37°", TABLE},
            new String[] {"6 px", CARTOGRAPHY},
            new String[] {"3 px", CARTOGRAPHY},
            new String[] {"12 px", CARTOGRAPHY},
            new String[] {"1.05°", CARTOGRAPHY},
            new String[] {"0.97 px", CARTOGRAPHY},
            new String[] {"2003-08-28", CHART},
            new String[] {"25.1″", CHART},
            new String[] {"6.3 px", CHART},
            new String[] {"12.6 px", CHART},
            new String[] {"25.1 px", CHART},
            new String[] {"0.5°", CHART},
            new String[] {"0.25°", CHART},
            new String[] {"1912-11-05", CHART},
            new String[] {"2018-10-06", CHART},
            new String[] {"87.9 %", CHART},
            new String[] {"15.1″", CHART},
            new String[] {"2018-07-27", CHART},
            new String[] {"70.7″", CHART},
            new String[] {"2001-06-30", CHART},
            new String[] {"20.5″", CHART},
            new String[] {"6.9″", CHART},
            new String[] {"2018-05-01", CHART},
            new String[] {"2051-03-01", CHART});

    /**
     * Each cited phrase: the report's words, then the English and the
     * Norwegian article's own rendering of them - a grouped number with
     * the language's own separator, a count or a word in the language's own.
     */
    private static final List<String[]> PHRASES = List.of(
            new String[] {"709,632", "**709,632 bytes**", "**709 632 bytes**", HANDOVER},
            new String[] {"43,951,104", "**43,951,104 bytes**", "**43 951 104 bytes**", HANDOVER},
            new String[] {"36 Horizons responses", "**36 Horizons responses**", "**36 Horizons-svar**",
                    HANDOVER},
            new String[] {"3,777 of 3,780", "**3,777 of 3,780**", "**3 777 av 3 780**", HANDOVER},
            new String[] {"3,780 of 3,780", "**3,780 of 3,780**", "**3 780 av 3 780**", HANDOVER},
            new String[] {"not including", "**not including**", "**men ikke med**", HANDOVER},
            new String[] {"2000 to 2049", "**2000 to 2049**", "**2000 til 2049**", HANDOVER},
            new String[] {"three hours", "**three hours**", "**tre timer**", HANDOVER},
            new String[] {"gibbous", "*gibbous*", "*mer enn halvt belyst*", TABLE},
            new String[] {"north pole toward us", "(north pole toward us)", "(nordpolen mot oss)", TABLE});

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    }

    @Test
    void everyFigureIsTheEvidencePageByteForByte() throws Exception {
        String english = read(ENGLISH);
        String norwegian = read(NORWEGIAN);
        for (String figure : FIGURES) {
            String name = "mars-" + figure + ".png";
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
        assertTrue(index.contains("mars-on-the-page.md") && index.contains("mars-pa-siden.md"),
                "the articles index lists both");
        String build = read(Path.of("scripts/build-articles.py"));
        assertTrue(build.contains("mars-on-the-page.md") && build.contains("mars-pa-siden.md"),
                "the Pages build publishes both");
    }

    /**
     * Released 5.5, as the Saturn article described released 5.4: the
     * planet a later release adds is not this article's to describe, and
     * nothing in it claims surface detail or naked-eye ease.
     */
    @Test
    void theArticleDescribesReleasedFiveFiveAndNothingElse() throws IOException {
        for (Path article : List.of(ENGLISH, NORWEGIAN)) {
            String text = read(article).toLowerCase(Locale.ROOT);
            assertTrue(text.contains("5.5"), article + " names the release it describes");
            assertFalse(text.contains("naked eye") || text.contains("det blotte øye"),
                    article + " promises no naked-eye ease");
            for (String later : List.of("uranus", "miranda", "ariel", "umbriel", "titania", "oberon")) {
                assertFalse(text.contains(later), article + " says nothing of " + later
                        + ", which a later release draws");
            }
        }
    }
}
