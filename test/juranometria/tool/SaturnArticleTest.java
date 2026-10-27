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
 * The Saturn article (issue #607) is held to released 5.4.0, as the
 * Mercury article is to 5.3.0: every figure is an evidence page byte for
 * byte - each unchanged since tag {@code v5.4.0} when the article was
 * written - every cited number stands in the report it is cited from,
 * both languages are listed where a reader finds them, and the text
 * describes 5.4 and nothing a later release adds. Text is compared with
 * its whitespace collapsed, so a line break in the prose or the report
 * never hides a citation.
 */
class SaturnArticleTest {

    private static final Path ENGLISH = Path.of("docs/articles/saturns-rings-on-the-page.md");
    private static final Path NORWEGIAN = Path.of("docs/articles/saturns-ringer-pa-siden.md");
    private static final Path IMAGES = Path.of("docs/articles/images");

    /** Each figure and the evidence page it is a byte-for-byte copy of. */
    private static final List<String[]> FIGURES = List.of(
            new String[] {"saturn-ring-opening-1900-2100.png",
                    "docs/studies/saturn-system/controls-saturn-5-ring-opening.png"},
            new String[] {"saturn-card-8.png", "docs/studies/saturn-on-the-chart/card-8.png"},
            new String[] {"saturn-card-1.png", "docs/studies/saturn-on-the-chart/card-1.png"},
            new String[] {"saturn-card-0.25-dark.png", "docs/studies/saturn-on-the-chart/card-0.25-dark.png"},
            new String[] {"saturn-open-north-0.25.png", "docs/studies/saturn-on-the-chart/open-north-0.25.png"},
            new String[] {"saturn-open-south-0.25-dark.png",
                    "docs/studies/saturn-on-the-chart/open-south-0.25-dark.png"},
            new String[] {"saturn-edge-on-0.25.png", "docs/studies/saturn-on-the-chart/edge-on-0.25.png"},
            new String[] {"saturn-unlit-0.25.png", "docs/studies/saturn-on-the-chart/unlit-0.25.png"},
            new String[] {"saturn-in-front-0.25.png", "docs/studies/saturn-on-the-chart/in-front-0.25.png"},
            new String[] {"saturn-in-shadow-0.25-dark.png",
                    "docs/studies/saturn-on-the-chart/in-shadow-0.25-dark.png"});

    /** Each language's own table card, and the sheet it is a copy of. */
    private static final String[] CARD_EN = {"saturn-card-en.png",
            "docs/studies/saturn-system/controls-saturn-en-1-saturn-card.png"};
    private static final String[] CARD_NB = {"saturn-card-nb-NO.png",
            "docs/studies/saturn-system/controls-saturn-nb-NO-1-saturn-card.png"};

    private static final String HANDOVER = "docs/decisions/saturn-handover.md";
    private static final String CARTOGRAPHY = "docs/decisions/saturn-cartography.md";
    private static final String TABLE = "docs/studies/saturn-system/table-measurements.md";
    private static final String CHART = "docs/studies/saturn-on-the-chart/measurements.md";

    /** Each cited number, verbatim in both languages, and the report it is read from. */
    private static final List<String[]> CITATIONS = List.of(
            new String[] {"1900-2100", HANDOVER},
            new String[] {"2000-2100", HANDOVER},
            new String[] {"Hyperion", HANDOVER},
            new String[] {"2027-06-28", HANDOVER},
            new String[] {"0.0000″", HANDOVER},
            new String[] {"0.22 km", HANDOVER},
            new String[] {"0.0006″", HANDOVER},
            new String[] {"0.6840″", HANDOVER},
            new String[] {"2025-03-23 17:51", HANDOVER},
            new String[] {"2025-05-06 15:35", HANDOVER},
            new String[] {"−26.995°", HANDOVER},
            new String[] {"2003-04-07", HANDOVER},
            new String[] {"−32.049°", HANDOVER},
            new String[] {"34.13″", HANDOVER},
            new String[] {"46.97″", HANDOVER},
            new String[] {"15.04″", HANDOVER},
            new String[] {"20.70″", HANDOVER},
            new String[] {"0.8849″", HANDOVER},
            new String[] {"32.2″", HANDOVER},
            new String[] {"23.5″", HANDOVER},
            new String[] {"2026-10-05 00:00", TABLE},
            new String[] {"00h 44m 16.0s", TABLE},
            new String[] {"+1° 48′ 29″", TABLE},
            new String[] {"31.18°", TABLE},
            new String[] {"8.434258", TABLE},
            new String[] {"19.7″ / 17.8″", TABLE},
            new String[] {"100.0 %", TABLE},
            new String[] {"3.2°", TABLE},
            new String[] {"7.43°", TABLE},
            new String[] {"7.71°", TABLE},
            new String[] {"44.7″ × 5.8″", TABLE},
            new String[] {"6 px", CARTOGRAPHY},
            new String[] {"12 px", CARTOGRAPHY},
            new String[] {"1 px", CARTOGRAPHY},
            new String[] {"30.4 %", CARTOGRAPHY},
            new String[] {"3 px", CHART},
            new String[] {"0.5°", CHART},
            new String[] {"0.25°", CHART},
            new String[] {"12.2 × 4.5", CHART},
            new String[] {"22.3 × 8.9", CHART},
            new String[] {"44.6 × 17.8", CHART},
            new String[] {"2076-10-10 00:00", CHART},
            new String[] {"26.97°", CHART},
            new String[] {"2003-04-05 00:00", CHART},
            new String[] {"26.99°", CHART},
            new String[] {"+0.00°", CHART},
            new String[] {"2025-05-06 00:00", CHART},
            new String[] {"2.19°", CHART},
            new String[] {"0.01°", CHART},
            new String[] {"2026-07-01 03:00", CHART},
            new String[] {"2026-07-01 14:00", CHART});

    /**
     * Each cited phrase: the report's words, then the English and the
     * Norwegian article's own rendering of them - a grouped number with
     * the language's own separator, a count in the language's own words.
     */
    private static final List<String[]> PHRASES = List.of(
            new String[] {"16,516,096", "**16,516,096 bytes**", "**16 516 096 bytes**", HANDOVER},
            new String[] {"62,829,568", "**62,829,568 bytes**", "**62 829 568 bytes**", HANDOVER},
            new String[] {"79,345,664", "**79,345,664 bytes**", "**79 345 664 bytes**", HANDOVER},
            new String[] {"60268, 60268 and 54364 km", "**60268, 60268 and 54364 km**",
                    "**60268, 60268 og 54364 km**", HANDOVER},
            new String[] {"74,491 km", "**74,491 km**", "**74 491 km**", HANDOVER},
            new String[] {"117,570 km", "**117,570 km**", "**117 570 km**", HANDOVER},
            new String[] {"122,050 km", "**122,050 km**", "**122 050 km**", HANDOVER},
            new String[] {"136,770 km", "**136,770 km**", "**136 770 km**", HANDOVER},
            new String[] {"74 Horizons responses", "**74 Horizons responses**", "**74 Horizons-svar**",
                    HANDOVER},
            new String[] {"1,819 of 1,819", "**1,819 of 1,819**", "**1 819 av 1 819**", HANDOVER},
            new String[] {"all 44 crossings", "all **44** crossings", "alle **44** passasjene", HANDOVER},
            new String[] {"30 Earth, 14 Sun", "**30 Earth, 14 Sun**", "**30 for jorden og 14 for Solen**",
                    HANDOVER},
            new String[] {"2 minutes by 2098", "**2 minutes by 2098**", "**2 minutter innen 2098**", HANDOVER},
            new String[] {"18,196″", "**18,196″**", "**18 196″**", HANDOVER},
            new String[] {"1.31-1.33 h", "**1.31-1.33 h**", "**1.31-1.33 timer**", HANDOVER},
            new String[] {"248 of the 10,488", "**248 of the 10,488**", "**248 av de 10 488**", HANDOVER},
            new String[] {"Moons: 0 of 7 drawn", "**0 of 7** moons at 36°", "**0 av 7** måner ved 36°", CHART},
            new String[] {"Moons: 1 of 7 drawn", "**1 of 7** at 8°", "**1 av 7** ved 8°", CHART},
            new String[] {"Moons: 6 of 7 drawn", "**6 of 7** at 1°", "**6 av 7** ved 1°", CHART});

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    }

    @Test
    void everyFigureIsTheEvidencePageByteForByte() throws Exception {
        String english = read(ENGLISH);
        String norwegian = read(NORWEGIAN);
        for (String[] figure : FIGURES) {
            Path copy = IMAGES.resolve(figure[0]);
            assertTrue(Files.exists(copy), figure[0] + " is shipped with the article");
            assertArrayEquals(Files.readAllBytes(Path.of(figure[1])), Files.readAllBytes(copy),
                    figure[0] + " is " + figure[1] + " byte for byte");
            assertTrue(english.contains("images/" + figure[0]), figure[0] + " is shown in English");
            assertTrue(norwegian.contains("images/" + figure[0]), figure[0] + " is shown in Norwegian");
        }
        for (String[] card : List.of(CARD_EN, CARD_NB)) {
            assertArrayEquals(Files.readAllBytes(Path.of(card[1])),
                    Files.readAllBytes(IMAGES.resolve(card[0])), card[0] + " is " + card[1] + " byte for byte");
        }
        assertTrue(english.contains("images/" + CARD_EN[0]) && norwegian.contains("images/" + CARD_NB[0]),
                "each language shows its own table card");
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
        assertTrue(index.contains("saturns-rings-on-the-page.md")
                && index.contains("saturns-ringer-pa-siden.md"), "the articles index lists both");
        String build = read(Path.of("scripts/build-articles.py"));
        assertTrue(build.contains("saturns-rings-on-the-page.md")
                && build.contains("saturns-ringer-pa-siden.md"), "the Pages build publishes both");
    }

    /**
     * Released 5.4, as the Mercury article described released 5.3: the
     * planet a later release adds is not this article's to describe, and
     * nothing in it claims surface detail or naked-eye ease.
     */
    @Test
    void theArticleDescribesReleasedFiveFourAndNothingElse() throws IOException {
        for (Path article : List.of(ENGLISH, NORWEGIAN)) {
            String text = read(article).toLowerCase(Locale.ROOT);
            assertTrue(text.contains("5.4"), article + " names the release it describes");
            assertFalse(text.contains("naked eye") || text.contains("det blotte øye"),
                    article + " promises no naked-eye ease");
            assertFalse(text.contains("phobos") || text.contains("deimos"),
                    article + " says nothing of the moons a later release draws");
        }
        assertFalse(read(ENGLISH).toLowerCase(Locale.ROOT).contains("mars"),
                "the English article says nothing of Mars (in Norwegian, mars is March)");
    }
}
