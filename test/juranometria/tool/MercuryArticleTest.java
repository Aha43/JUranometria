package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Mercury article (issue #580) is held to released 5.3.0: every
 * figure is a released evidence page byte for byte, every cited number
 * stands in the report it is cited from, both languages are listed
 * where a reader finds them, and the text describes 5.3 and nothing a
 * later release adds.
 */
class MercuryArticleTest {

    private static final Path ENGLISH = Path.of("docs/articles/mercury-never-far-from-the-sun.md");
    private static final Path NORWEGIAN = Path.of("docs/articles/merkur-aldri-langt-fra-solen.md");
    private static final Path IMAGES = Path.of("docs/articles/images");

    /** Each figure and the evidence page it is a byte-for-byte copy of, unchanged since 5.3.0. */
    private static final List<String[]> FIGURES = List.of(
            new String[] {"mercury-elongation-8-nb.png", "docs/studies/mercury-on-the-chart/elongation-8-nb.png"},
            new String[] {"mercury-elongation-1.png", "docs/studies/mercury-on-the-chart/elongation-1.png"},
            new String[] {"mercury-elongation-3-dark.png", "docs/studies/mercury-on-the-chart/elongation-3-dark.png"},
            new String[] {"mercury-closer-0.5.png", "docs/studies/closer-fields/mercury-elongation-0.5.png"},
            new String[] {"mercury-closer-0.25.png", "docs/studies/closer-fields/mercury-elongation-0.25.png"},
            new String[] {"mercury-transit-2019-1.png", "docs/studies/mercury-on-the-chart/transit-2019-1.png"},
            new String[] {"mercury-transit-2019-6-nb.png", "docs/studies/mercury-on-the-chart/transit-2019-6-nb.png"},
            new String[] {"mercury-transit-2019-0.25.png", "docs/studies/closer-fields/transit-2019-0.25.png"},
            new String[] {"mercury-inferior-conjunction-2026-1.png",
                    "docs/studies/mercury-on-the-chart/inferior-conjunction-2026-1.png"},
            new String[] {"mercury-apparition-2026.png", "docs/studies/mercury-on-the-chart/apparition-2026.png"});

    /**
     * The two table sheets are the released ones: the Mercury table
     * sheet moved after 5.3.0 (#259's chart-instant line, #537's
     * agreement line), so the article's copies are held to the bytes at
     * tag {@code v5.3.0} by their SHA-256, read from
     * {@code git show v5.3.0:docs/studies/interface-language/<sheet>}.
     */
    private static final List<String[]> RELEASED = List.of(
            new String[] {"mercury-table-en.png",
                    "8e60aea50ac45ead7d0944eb4514ad0024e4ad3ec144f8b0f852fbe33f19d2f3"},
            new String[] {"mercury-table-nb-NO.png",
                    "f68ae6e759a5c08eb761049fe572a0bbc8ab9e43b803a5f20eaeedc5cf2ce410"});

    private static final String HANDOVER = "docs/decisions/mercury-handover.md";
    private static final String TABLE = "docs/studies/mercury/table-measurements.md";
    private static final String CARTOGRAPHY = "docs/studies/mercury-cartography/measurements.md";
    private static final String CHART = "docs/studies/mercury-on-the-chart/measurements.md";
    private static final String CLOSER = "docs/studies/closer-fields/measurements.md";

    /** Each cited number, verbatim in both languages, and the report it is read from. */
    private static final List<String[]> CITATIONS = List.of(
            new String[] {"18.1°", TABLE},
            new String[] {"27.8°", TABLE},
            new String[] {"3 299 328 bytes", HANDOVER},
            new String[] {"1900–2100", HANDOVER},
            new String[] {"2440.53 km", HANDOVER},
            new String[] {"2438.26 km", HANDOVER},
            new String[] {"11 207", HANDOVER},
            new String[] {"0.000″", HANDOVER},
            new String[] {"0.2 km", HANDOVER},
            new String[] {"0.129″", HANDOVER},
            new String[] {"15.88″", HANDOVER},
            new String[] {"2027-06-28", HANDOVER},
            new String[] {"11.858″", HANDOVER},
            new String[] {"0.1″", HANDOVER},
            new String[] {"1937-05-11", HANDOVER},
            new String[] {"+0.017″", HANDOVER},
            new String[] {"2 %", HANDOVER},
            new String[] {"98 %", HANDOVER},
            new String[] {"4.64″", HANDOVER},
            new String[] {"12.26″", HANDOVER},
            new String[] {"2026-02-19 18:00", TABLE},
            new String[] {"49.7 %", TABLE},
            new String[] {"243°", TABLE},
            new String[] {"7.2″", TABLE},
            new String[] {"0.937962", TABLE},
            new String[] {"140.317", TABLE},
            new String[] {"1.80°", TABLE},
            new String[] {"23h 17m 02.0s", TABLE},
            new String[] {"−3° 20′ 04″", TABLE},
            new String[] {"1.16–3.06 px", HANDOVER},
            new String[] {"6 px", CHART},
            new String[] {"1.8 px", CHART},
            new String[] {"30.3 %", HANDOVER},
            new String[] {"0.51°", CARTOGRAPHY},
            new String[] {"0.19°", CARTOGRAPHY},
            new String[] {"0.5°", CLOSER},
            new String[] {"0.25°", CLOSER},
            new String[] {"7.2 px", CLOSER},
            new String[] {"2019-11-11", CHART},
            new String[] {"15:20", CHART},
            new String[] {"484.4 px", CHART},
            new String[] {"1937.8 px", CLOSER},
            new String[] {"10.0 px", CLOSER},
            new String[] {"2032-11-13 08:54", TABLE},
            new String[] {"2026-05-14", CHART},
            new String[] {"14:00", CHART},
            new String[] {"2026-03-07", CHART},
            new String[] {"11:00", CHART},
            new String[] {"3.6°", CHART},
            new String[] {"0.7 %", CHART},
            new String[] {"2026-01-21", CHART});

    /**
     * Each cited phrase: the report's words, then the English and the
     * Norwegian article's own rendering of them.
     */
    private static final List<String[]> PHRASES = List.of(
            new String[] {"−16 to +37 min", "**−16 to +37** minutes", "**−16 til +37**\nminutter", HANDOVER},
            new String[] {"28 greatest transits", "greatest-transit instants of all **28**\ntransits",
                    "tidspunktene for største\npassasje for alle **28** passasjene", HANDOVER},
            new String[] {"+0.7 min", "**−0.5 to +0.7** minutes", "**−0.5 til\n+0.7** minutter", HANDOVER},
            new String[] {"no week at any field", "**no week at any field**", "**ingen uke ved noe felt**", HANDOVER},
            new String[] {"every 3 days", "every **3 days**", "hver **tredje dag**", CHART},
            new String[] {"50 days beyond", "**50 days** beyond", "**50 dager** videre", CHART},
            new String[] {"some two dozen pixels", "**some two dozen** pixels", "**omtrent to dusin** piksler", CHART},
            new String[] {"within 45°", "**within 45°**", "**innenfor\n45°**", CHART},
            new String[] {"19 of the 19", "**19 of the 19**", "**alle 19**", CHART});

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void everyFigureIsTheReleasedEvidencePageByteForByte() throws Exception {
        for (String[] figure : FIGURES) {
            Path copy = IMAGES.resolve(figure[0]);
            assertTrue(Files.exists(copy), figure[0] + " is shipped with the article");
            assertArrayEquals(Files.readAllBytes(Path.of(figure[1])), Files.readAllBytes(copy),
                    figure[0] + " is " + figure[1] + " byte for byte");
        }
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        for (String[] figure : RELEASED) {
            Path copy = IMAGES.resolve(figure[0]);
            assertTrue(Files.exists(copy), figure[0] + " is shipped with the article");
            assertEquals(figure[1], HexFormat.of().formatHex(sha256.digest(Files.readAllBytes(copy))),
                    figure[0] + " is the sheet released in 5.3.0, byte for byte");
        }
        String english = read(ENGLISH);
        String norwegian = read(NORWEGIAN);
        for (String[] figure : FIGURES) {
            assertTrue(english.contains("images/" + figure[0]), figure[0] + " is shown in English");
            assertTrue(norwegian.contains("images/" + figure[0]), figure[0] + " is shown in Norwegian");
        }
        assertTrue(english.contains("images/mercury-table-en.png")
                && norwegian.contains("images/mercury-table-nb-NO.png"),
                "each language shows its own table sheet");
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
        assertTrue(index.contains("mercury-never-far-from-the-sun.md")
                && index.contains("merkur-aldri-langt-fra-solen.md"), "the articles index lists both");
        String build = read(Path.of("scripts/build-articles.py"));
        assertTrue(build.contains("mercury-never-far-from-the-sun.md")
                && build.contains("merkur-aldri-langt-fra-solen.md"), "the Pages build publishes both");
    }

    /**
     * Released 5.3, as the Venus article described released 5.2: the
     * planet a later release adds is not this article's to describe,
     * and nothing in it claims surface detail or naked-eye ease.
     */
    @Test
    void theArticleDescribesReleasedFiveThreeAndNothingElse() throws IOException {
        for (Path article : List.of(ENGLISH, NORWEGIAN)) {
            String text = read(article).toLowerCase(Locale.ROOT);
            assertFalse(text.contains("saturn"), article + " says nothing of Saturn");
            assertTrue(text.contains("5.3"), article + " names the release it describes");
            assertFalse(text.contains("naked eye") || text.contains("det blotte øye"),
                    article + " promises no naked-eye ease");
        }
        assertFalse(read(ENGLISH).toLowerCase(Locale.ROOT).contains("mars"),
                "the English article says nothing of Mars (in Norwegian, mars is March)");
    }
}
