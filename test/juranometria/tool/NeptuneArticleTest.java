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
 * The Neptune article (issue #670) is held to Neptune as built on main
 * for the release that follows 5.5, as the Uranus article is: every figure
 * is an evidence page byte for byte, every cited number stands in the
 * report it is cited from, both languages are listed where a reader finds
 * them, and the text describes what is destined for 6.0 without claiming
 * that 6.0 has been released. Text is compared with its whitespace
 * collapsed, so a line break in the prose or the report never hides a
 * citation.
 */
class NeptuneArticleTest {

    private static final Path ENGLISH = Path.of("docs/articles/neptune-on-the-page.md");
    private static final Path NORWEGIAN = Path.of("docs/articles/neptun-pa-siden.md");
    private static final Path IMAGES = Path.of("docs/articles/images");
    private static final String CHART_DIR = "docs/studies/neptune-on-the-chart/";

    /** Each figure and the evidence page it is a byte-for-byte copy of. */
    private static final List<String> FIGURES = List.of("opposition-2026-8-nb", "opposition-2026-3-dark",
            "opposition-2026-1", "opposition-2026-0.25-dark", "conjunction-2026-3", "conjunction-2003-1",
            "triton-widest-2045-1", "triton-widest-2045-0.25-nb", "nereid-widest-2018-2",
            "nereid-nearest-2008-0.25", "triton-behind-2044-0.25", "triton-in-front-2043-0.25-dark",
            "before-the-moons-1999-0.25");

    private static final String HANDOVER = "docs/decisions/neptune-handover.md";
    private static final String CARTOGRAPHY = "docs/decisions/neptune-cartography.md";
    private static final String CARTOGRAPHY_REPORT = "docs/studies/neptune-cartography/measurements.md";
    private static final String CHART = "docs/studies/neptune-on-the-chart/measurements.md";
    private static final String ACCURACY = "docs/studies/neptune-system/service-accuracy.md";

    /** Each cited number, verbatim in both languages, and the report it is read from. */
    private static final List<String[]> CITATIONS = List.of(
            new String[] {"2.18-2.37″", CARTOGRAPHY_REPORT},
            new String[] {"DE442", HANDOVER},
            new String[] {"1900–2100", HANDOVER},
            new String[] {"0.0040″", HANDOVER},
            new String[] {"0.1072″", HANDOVER},
            new String[] {"0.49 km", HANDOVER},
            new String[] {"74.190 km", HANDOVER},
            new String[] {"0.0036″", HANDOVER},
            new String[] {"89.48 km", HANDOVER},
            new String[] {"100 km", HANDOVER},
            new String[] {"80 km", HANDOVER},
            new String[] {"NEP098", HANDOVER},
            new String[] {"2000–2100", HANDOVER},
            new String[] {"0.70 sin N", HANDOVER},
            new String[] {"0.018″", HANDOVER},
            new String[] {"2027-06-28", HANDOVER},
            new String[] {"0.0035″", HANDOVER},
            new String[] {"93.426 km", HANDOVER},
            new String[] {"0.0038″", HANDOVER},
            new String[] {"0.0070°", HANDOVER},
            new String[] {"0.2078″", HANDOVER},
            new String[] {"0.0032″", HANDOVER},
            new String[] {"99.971 %", HANDOVER},
            new String[] {"1.9529°", HANDOVER},
            new String[] {"29.2178°", HANDOVER},
            new String[] {"0.0520″", HANDOVER},
            new String[] {"2026-10-29 12:00", HANDOVER},
            new String[] {"29.0485 AU", HANDOVER},
            new String[] {"2.351″", HANDOVER},
            new String[] {"99.991 %", HANDOVER},
            new String[] {"1.0636°", HANDOVER},
            new String[] {"316.498°", HANDOVER},
            new String[] {"20.77°", HANDOVER},
            new String[] {"0.5348″–16.9617″", HANDOVER},
            new String[] {"2.1413″–457.7006″", HANDOVER},
            new String[] {"13.43–13.632", HANDOVER},
            new String[] {"18.669–18.875", HANDOVER},
            new String[] {"2.317″", CARTOGRAPHY},
            new String[] {"6 px", CARTOGRAPHY},
            new String[] {"3 px", CARTOGRAPHY},
            new String[] {"12 px", CARTOGRAPHY},
            new String[] {"0.099°", CARTOGRAPHY},
            new String[] {"0.002 px", CARTOGRAPHY},
            new String[] {"0.040 px", CARTOGRAPHY},
            new String[] {"32.8 %", CARTOGRAPHY},
            new String[] {"91.3 %", CARTOGRAPHY},
            new String[] {"95.6 %", CARTOGRAPHY},
            new String[] {"18.3 %", CARTOGRAPHY},
            new String[] {"86.9 %", CARTOGRAPHY},
            new String[] {"V 8", CARTOGRAPHY},
            new String[] {"0.5°", CHART},
            new String[] {"0.25°", CHART},
            new String[] {"2026-09-26", CHART},
            new String[] {"2.36″", CHART},
            new String[] {"18.9°", CHART},
            new String[] {"1.3°", CHART},
            new String[] {"2045-11-04", CHART},
            new String[] {"2018-08-25", CHART},
            new String[] {"2008-03-22", CHART},
            new String[] {"2044-09-26", CHART},
            new String[] {"2043-09-07", CHART},
            new String[] {"1999-12-31", CHART},
            new String[] {"2026-03-22", ACCURACY},
            new String[] {"2003-01-30", ACCURACY});

    /**
     * Each cited phrase: the report's words, then the English and the
     * Norwegian article's own rendering of them - a grouped number with
     * the language's own separator, a count or a word in the language's own.
     */
    private static final List<String[]> PHRASES = List.of(
            new String[] {"416,768", "**416,768 bytes**", "**416 768 bytes**", HANDOVER},
            new String[] {"50,302,976", "**50,302,976 bytes**", "**50 302 976 bytes**", HANDOVER},
            new String[] {"2,222.155 km", "**2,222.155 km**", "**2 222.155 km**", HANDOVER},
            new String[] {"39 requests", "**39 Horizons requests**", "**39 Horizons-forespørsler**", HANDOVER},
            new String[] {"2,050.7″", "**2,050.7″**", "**2 050.7″**", HANDOVER},
            new String[] {"5,578 of 5,579", "**5,578 of 5,579**", "**5 578 av 5 579**", HANDOVER},
            new String[] {"5,579 of 5,579", "**5,579 of 5,579**", "**5 579 av 5 579**", HANDOVER},
            new String[] {"+114 minutes", "**+114 minutes**", "**+114 minutter**", HANDOVER},
            new String[] {"south pole is towards the observer", "south pole towards us", "sørpolen mot oss",
                    HANDOVER},
            new String[] {"5.4 magnitudes fainter", "**5.4 magnitudes** fainter", "**5.4 magnituder** svakere",
                    CARTOGRAPHY},
            new String[] {"10.7", "**10.7 magnitudes** fainter", "**10.7 magnituder** svakere", CARTOGRAPHY},
            new String[] {"a tenth of a pixel", "**a tenth of a pixel**", "**en tidel av en piksel**", CHART},
            new String[] {"12 of 12", "**12 of 12**", "**12 av 12**", CHART},
            new String[] {"too close to tell apart at this field, not drawn",
                    "*too close to tell apart at this field, not drawn*",
                    "*for nær til å skjelnes i dette feltet, ikke tegnet*", CHART},
            new String[] {"behind Neptune, not drawn", "*behind Neptune, not drawn*", "*bak Neptun, ikke tegnet*",
                    CHART});

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    }

    @Test
    void everyFigureIsTheEvidencePageByteForByte() throws Exception {
        String english = read(ENGLISH);
        String norwegian = read(NORWEGIAN);
        for (String figure : FIGURES) {
            String name = "neptune-" + figure + ".png";
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
        assertTrue(index.contains("neptune-on-the-page.md") && index.contains("neptun-pa-siden.md"),
                "the articles index lists both");
        String build = read(Path.of("scripts/build-articles.py"));
        assertTrue(build.contains("neptune-on-the-page.md") && build.contains("neptun-pa-siden.md"),
                "the Pages build publishes both");
    }

    /**
     * Destined for 6.0, not yet released: the text names the release and
     * frames it as the one that follows 5.5, never as published; it names
     * no body beyond Neptune, and claims no surface detail or naked-eye ease.
     */
    @Test
    void theArticleDescribesWhatIsDestinedForSixWithoutClaimingItReleased() throws IOException {
        String english = read(ENGLISH).toLowerCase(Locale.ROOT);
        String norwegian = read(NORWEGIAN).toLowerCase(Locale.ROOT);
        assertTrue(english.contains("6.0") && english.contains("the release that follows 5.5"));
        assertTrue(norwegian.contains("6.0") && norwegian.contains("utgaven som følger etter 5.5"));
        for (String text : List.of(english, norwegian)) {
            for (String claim : List.of("has been released", "was released", "already released",
                    "er utgitt", "ble utgitt", "allerede utgitt", "naked eye", "det blotte øye", "pluto")) {
                assertFalse(text.contains(claim), "the text does not say '" + claim + "'");
            }
        }
    }
}
