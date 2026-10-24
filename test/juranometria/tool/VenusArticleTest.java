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
 * The article "The phases of Venus" (Sprint 50, issue #545), held to
 * what it quotes as "Jupiter's little sky" is: every figure is a
 * released evidence page copied byte for byte, every cited number
 * stands in the report it is cited from and in both languages, every
 * cited phrase stands in its report and in each language's own words,
 * both languages are listed where the reader finds them, and the
 * article describes Venus as release 5.2 drew it and nothing else.
 */
class VenusArticleTest {

    private static final Path ENGLISH = Path.of("docs/articles/the-phases-of-venus.md");
    private static final Path NORWEGIAN = Path.of("docs/articles/venus-faser.md");
    private static final Path IMAGES = Path.of("docs/articles/images");

    /** Each figure, and the evidence page it is a copy of. */
    private static final List<String[]> FIGURES = List.of(
            new String[] {"venus-table-en.png", "docs/studies/interface-language/venustable-en-1-instant.png"},
            new String[] {"venus-table-nb-NO.png", "docs/studies/interface-language/venustable-nb-NO-7-instant.png"},
            new String[] {"venus-elongation-8-nb.png", "docs/studies/venus-on-the-chart/elongation-8-nb.png"},
            new String[] {"venus-elongation-1.png", "docs/studies/venus-on-the-chart/elongation-1.png"},
            new String[] {"venus-elongation-3-dark.png", "docs/studies/venus-on-the-chart/elongation-3-dark.png"},
            new String[] {"venus-transit-2012-1.png", "docs/studies/venus-on-the-chart/transit-2012-1.png"},
            new String[] {"venus-transit-2012-8.png", "docs/studies/venus-on-the-chart/transit-2012-8.png"},
            new String[] {"venus-inferior-conjunction-2026-1.png",
                    "docs/studies/venus-on-the-chart/inferior-conjunction-2026-1.png"},
            new String[] {"venus-apparition-2026-27.png", "docs/studies/venus-on-the-chart/apparition-2026-27.png"});

    private static final String HANDOVER = "docs/decisions/venus-handover.md";
    private static final String STUDY = "docs/studies/venus/measurements.md";
    private static final String TABLE = "docs/studies/venus/table-measurements.md";
    private static final String CHART = "docs/studies/venus-on-the-chart/measurements.md";

    /** Each cited number, verbatim in both languages, and the report it is read from. */
    private static final List<String[]> CITATIONS = List.of(
            new String[] {"1 242 112 bytes", HANDOVER},
            new String[] {"1900–2100", HANDOVER},
            new String[] {"6051.8 km", HANDOVER},
            new String[] {"74 km", STUDY},
            new String[] {"1.22 %", HANDOVER},
            new String[] {"0.771″", STUDY},
            new String[] {"10 771", HANDOVER},
            new String[] {"0.001″", HANDOVER},
            new String[] {"0.2 km", HANDOVER},
            new String[] {"0.129″", HANDOVER},
            new String[] {"2027-06-28", HANDOVER},
            new String[] {"7.118″", HANDOVER},
            new String[] {"8–44", HANDOVER},
            new String[] {"2 %", HANDOVER},
            new String[] {"98 %", HANDOVER},
            new String[] {"9.61″", STUDY},
            new String[] {"63.03″", STUDY},
            new String[] {"2026-08-15 06:00", TABLE},
            new String[] {"48.7 %", TABLE},
            new String[] {"45.9°", TABLE},
            new String[] {"295°", TABLE},
            new String[] {"24.3″", TABLE},
            new String[] {"0.686211", TABLE},
            new String[] {"102.656", TABLE},
            new String[] {"12h 27m 00.7s", TABLE},
            new String[] {"−4° 26′ 50″", TABLE},
            new String[] {"6 px", CHART},
            new String[] {"6.1 px", CHART},
            new String[] {"2012-06-06", CHART},
            new String[] {"01:00", CHART},
            new String[] {"14.5 px", CHART},
            new String[] {"2026-10-24", CHART},
            new String[] {"03:00", CHART},
            new String[] {"0.6 %", CHART},
            new String[] {"15.3 px", CHART},
            new String[] {"2026-01-06", CHART});

    /**
     * Each cited phrase: the report's words, then the English and the
     * Norwegian article's own rendering of them.
     */
    private static final List<String[]> PHRASES = List.of(
            new String[] {"every 14 days", "every **14 days**", "hver **14. dag**", CHART},
            new String[] {"some two dozen pixels", "**some two\ndozen** pixels", "**omtrent to dusin** piksler", CHART},
            new String[] {"within 45°", "**within 45°**", "**innenfor 45°**", CHART},
            new String[] {"18 of the 18", "**18 of the 18**", "**alle 18**", CHART});

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void everyFigureIsTheReleasedEvidencePageByteForByte() throws IOException {
        for (String[] figure : FIGURES) {
            Path copy = IMAGES.resolve(figure[0]);
            assertTrue(Files.exists(copy), figure[0] + " is in the article's images");
            assertArrayEquals(Files.readAllBytes(Path.of(figure[1])), Files.readAllBytes(copy),
                    figure[0] + " is " + figure[1] + " byte for byte - regenerate the"
                            + " source and copy it, never edit the copy");
            // A page photographed in one language illustrates that
            // language's article; every chart page illustrates both.
            boolean english = figure[0].endsWith("-en.png");
            boolean norwegian = figure[0].endsWith("-nb-NO.png");
            assertTrue(norwegian || read(ENGLISH).contains("images/" + figure[0]),
                    figure[0] + " is shown in English");
            assertTrue(english || read(NORWEGIAN).contains("images/" + figure[0]),
                    figure[0] + " is shown in Norwegian");
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
        assertTrue(index.contains("the-phases-of-venus.md") && index.contains("venus-faser.md"),
                "the articles index lists both");
        String build = read(Path.of("scripts/build-articles.py"));
        assertTrue(build.contains("the-phases-of-venus.md") && build.contains("venus-faser.md"),
                "the Pages build publishes both");
    }

    /**
     * Released 5.2, as the Jupiter article described released 5.1: the
     * planet a later release adds and the fields it opens are not this
     * article's to describe.
     */
    @Test
    void theArticleDescribesReleasedFiveTwoAndNothingElse() throws IOException {
        for (Path article : List.of(ENGLISH, NORWEGIAN)) {
            String text = read(article).toLowerCase(Locale.ROOT);
            assertFalse(text.contains("mercury") || text.contains("merkur"),
                    article + " says nothing of Mercury");
            assertFalse(text.contains("closer field") || text.contains("nærfelt")
                            || text.contains("0.25°") || text.contains("0.5°"),
                    article + " says nothing of the closer fields");
            assertTrue(text.contains("5.2"), article + " names the release it describes");
        }
    }
}
