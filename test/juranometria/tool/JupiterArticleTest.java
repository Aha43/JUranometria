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
 * The article "Jupiter's little sky" (Sprint 48, issue #516), held to
 * what it quotes: every figure is a released evidence page copied byte
 * for byte from the generator that holds it, every cited number stands
 * in the report it is cited from, both languages are listed where the
 * reader finds them, and the article says nothing of what 5.1 did not
 * release.
 */
class JupiterArticleTest {

    private static final Path ENGLISH = Path.of("docs/articles/jupiters-little-sky.md");
    private static final Path NORWEGIAN = Path.of("docs/articles/jupiters-lille-himmel.md");
    private static final Path IMAGES = Path.of("docs/articles/images");

    /** Each figure, and the evidence page it is a copy of. */
    private static final List<String[]> FIGURES = List.of(
            new String[] {"jupiter-triple-transit-1.png", "docs/studies/jupiter-on-the-chart/triple-1.png"},
            new String[] {"jupiter-sequence-2230.png", "docs/studies/jupiter-on-the-chart/sequence-2230.png"},
            new String[] {"jupiter-sequence-2300.png", "docs/studies/jupiter-on-the-chart/sequence-2300.png"},
            new String[] {"jupiter-io-shadow-1.png", "docs/studies/jupiter-on-the-chart/io-shadow-1.png"},
            new String[] {"jupiter-io-behind-1.png", "docs/studies/jupiter-on-the-chart/io-behind-1.png"},
            new String[] {"jupiter-spread-3-dark.png", "docs/studies/jupiter-on-the-chart/spread-3-dark.png"},
            new String[] {"jupiter-triple-8.png", "docs/studies/jupiter-on-the-chart/triple-8.png"},
            new String[] {"jupiter-horizon-drawn-8.png", "docs/studies/jupiter-on-the-chart/horizon-drawn-8.png"},
            new String[] {"jupiter-card-en.png", "docs/studies/jovian-system/controls-jovian-en-1-jupiter-card.png"},
            new String[] {"jupiter-moons-table-en.png", "docs/studies/jovian-system/controls-jovian-en-2-moons-table.png"},
            new String[] {"jupiter-moons-table-nb-NO.png", "docs/studies/jovian-system/controls-jovian-nb-NO-2-moons-table.png"});

    /** Each cited number, verbatim, and the report it is read from. */
    private static final List<String[]> CITATIONS = List.of(
            new String[] {"1900–2100", "docs/decisions/jovian-system.md"},
            new String[] {"2000–2100", "docs/decisions/jovian-system.md"},
            new String[] {"71 492 km", "docs/decisions/jovian-system.md"},
            new String[] {"59 MB", "docs/decisions/jovian-system.md"},
            new String[] {"51 MB", "docs/decisions/jovian-system.md"},
            new String[] {"22 993", "docs/decisions/jovian-system.md"},
            new String[] {"91 971", "docs/decisions/jovian-system.md"},
            new String[] {"91 972", "docs/decisions/jovian-system.md"},
            new String[] {"2026-12-06 17:00", "docs/decisions/jovian-system.md"},
            new String[] {"20.3635″", "docs/decisions/jovian-system.md"},
            new String[] {"20.3640″", "docs/decisions/jovian-system.md"},
            new String[] {"21:53", "docs/decisions/jovian-system.md"},
            new String[] {"21:52", "docs/decisions/jovian-system.md"},
            new String[] {"22:58", "docs/decisions/jovian-system.md"},
            new String[] {"22:59", "docs/decisions/jovian-system.md"},
            new String[] {"22:32", "docs/decisions/jovian-system.md"},
            new String[] {"00:53", "docs/decisions/jovian-system.md"},
            new String[] {"0.0001\"", "docs/studies/jovian-system/service-accuracy.md"},
            new String[] {"0.1337\"", "docs/studies/jovian-system/service-accuracy.md"},
            new String[] {"0.0006\"", "docs/studies/jovian-system/service-accuracy.md"},
            new String[] {"1.3567\"", "docs/studies/jovian-system/service-accuracy.md"},
            new String[] {"3739.16 km", "docs/studies/jovian-system/service-accuracy.md"},
            new String[] {"18.051°", "docs/studies/jovian-system/measurements.md"},
            new String[] {"94.580°", "docs/studies/jovian-system/measurements.md"},
            new String[] {"4.868899", "docs/studies/jovian-system/measurements.md"},
            new String[] {"40.491″", "docs/studies/jovian-system/measurements.md"},
            new String[] {"99.270 %", "docs/studies/jovian-system/measurements.md"},
            new String[] {"+15.831", "docs/studies/jovian-system/measurements.md"},
            new String[] {"-5.602", "docs/studies/jovian-system/measurements.md"},
            new String[] {"0.829", "docs/studies/jovian-system/measurements.md"},
            new String[] {"+8.722", "docs/studies/jovian-system/measurements.md"},
            new String[] {"-0.442", "docs/studies/jovian-system/measurements.md"},
            new String[] {"0.431", "docs/studies/jovian-system/measurements.md"},
            new String[] {"-15.480", "docs/studies/jovian-system/measurements.md"},
            new String[] {"+11.269", "docs/studies/jovian-system/measurements.md"},
            new String[] {"0.946", "docs/studies/jovian-system/measurements.md"},
            new String[] {"302.990″", "docs/studies/jovian-system/measurements.md"},
            new String[] {"14.966", "docs/studies/jovian-system/measurements.md"},
            new String[] {"10.1 px", "docs/studies/jupiter-on-the-chart/measurements.md"},
            new String[] {"6 px", "docs/studies/jupiter-on-the-chart/measurements.md"},
            new String[] {"3 px", "docs/studies/jupiter-on-the-chart/measurements.md"},
            new String[] {"2026-12-02 04:00 UTC", "docs/studies/jupiter-on-the-chart/measurements.md"},
            new String[] {"2026-12-02 07:00 UTC", "docs/studies/jupiter-on-the-chart/measurements.md"},
            new String[] {"2026-03-06 06:00 UTC", "docs/studies/jupiter-on-the-chart/measurements.md"},
            new String[] {"2026-12-01 12:20 UTC", "docs/studies/jupiter-on-the-chart/measurements.md"},
            new String[] {"1.2°", "docs/studies/jupiter-on-the-chart/measurements.md"});

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /** The article writes the report's figure; ″ and " are both the arcsecond. */
    private static boolean quotes(String article, String number) {
        return article.contains(number) || article.contains(number.replace("\"", "″"));
    }

    @Test
    void everyFigureIsTheReleasedEvidencePageByteForByte() throws IOException {
        for (String[] figure : FIGURES) {
            Path copy = IMAGES.resolve(figure[0]);
            Path source = Path.of(figure[1]);
            assertTrue(Files.exists(copy), figure[0] + " is in the article's images");
            assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(copy),
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
            String report = read(Path.of(citation[1]));
            assertTrue(quotes(report, citation[0]) || report.contains(citation[0].replace("–", "-")),
                    "'" + citation[0] + "' stands in " + citation[1]);
            assertTrue(quotes(english, citation[0]), "'" + citation[0] + "' is quoted in English");
            assertTrue(quotes(norwegian, citation[0]), "'" + citation[0] + "' is quoted in Norwegian");
        }
    }

    /**
     * The named evening's sides, read from Horizons' signed offsets in
     * the contract study (X positive east): every clause of either
     * article that names a moon and a side names the side the sign
     * gives, so the prose cannot disagree with the table (the owner's
     * reading of 2affc1c4 found Ganymede "to the east").
     */
    @Test
    void everyMoonsSideInTheProseIsTheSignOfItsHorizonsOffset() throws IOException {
        java.util.Map<String, Boolean> eastOf = new java.util.LinkedHashMap<>();
        for (String line : read(Path.of("docs/studies/jovian-system/measurements.md")).split("\n")) {
            if (!line.startsWith("| 2026-Dec-11 ")) {
                continue;
            }
            String[] cells = line.split("\\|");
            if (cells.length < 5 || !cells[2].trim().matches("io|europa|ganymede|callisto")) {
                continue;
            }
            String moon = cells[2].trim();
            boolean east = cells[3].trim().startsWith("+");
            Boolean before = eastOf.put(moon, east);
            assertTrue(before == null || before == east,
                    moon + " keeps one side through the named evening");
        }
        assertTrue(eastOf.size() == 4, "all four moons read from the named evening: " + eastOf);
        java.util.Map<String, String> norwegian = java.util.Map.of("io", "Io", "europa", "Europa",
                "ganymede", "Ganymedes", "callisto", "Callisto");
        java.util.Map<String, String> english = java.util.Map.of("io", "Io", "europa", "Europa",
                "ganymede", "Ganymede", "callisto", "Callisto");
        sidesAgree(read(ENGLISH), english, eastOf,
                "\\b(east|eastern)\\b", "\\b(west|western)\\b", "English");
        sidesAgree(read(NORWEGIAN), norwegian, eastOf,
                "\\b(øst|østlige|østsiden)\\b", "\\b(vest|vestlige|vestsiden)\\b", "Norwegian");
    }

    /**
     * A side word belongs to the moon last named before it in its
     * clause - "Io east ... Ganymede west" names two moons and two
     * sides, each the right one.
     */
    private static void sidesAgree(String article, java.util.Map<String, String> names,
                                   java.util.Map<String, Boolean> eastOf,
                                   String eastWords, String westWords, String language) {
        StringBuilder alternatives = new StringBuilder();
        for (String name : names.values()) {
            alternatives.append("\\b").append(name).append("\\b|");
        }
        java.util.regex.Pattern token = java.util.regex.Pattern.compile(
                alternatives + eastWords + "|" + westWords);
        java.util.regex.Pattern east = java.util.regex.Pattern.compile(eastWords);
        int sidesChecked = 0;
        for (String clause : article.replace("\n", " ").split("[.;:]")) {
            String current = null;
            java.util.regex.Matcher m = token.matcher(clause);
            while (m.find()) {
                String word = m.group();
                String moon = null;
                for (var name : names.entrySet()) {
                    if (name.getValue().equals(word)) {
                        moon = name.getKey();
                    }
                }
                if (moon != null) {
                    current = moon;
                    continue;
                }
                if (current == null) {
                    continue;
                }
                boolean saysEast = east.matcher(word).matches();
                sidesChecked++;
                assertTrue(saysEast == eastOf.get(current), language + ": '" + clause.trim()
                        + "' - " + names.get(current) + " is "
                        + (eastOf.get(current) ? "east" : "west")
                        + " of Jupiter on the named evening, not '" + word + "'");
            }
        }
        assertTrue(sidesChecked >= 4, language + ": the sides are stated: " + sidesChecked);
    }

    @Test
    void bothLanguagesAreListedWhereAReaderFindsThem() throws IOException {
        String index = read(Path.of("docs/articles/README.md"));
        assertTrue(index.contains("jupiters-little-sky.md") && index.contains("jupiters-lille-himmel.md"),
                "the articles index lists both");
        String build = read(Path.of("scripts/build-articles.py"));
        assertTrue(build.contains("jupiters-little-sky.md") && build.contains("jupiters-lille-himmel.md"),
                "the Pages build publishes both");
        assertTrue(build.contains("\"nb-NO\""), "the Norwegian page declares its language");
    }

    @Test
    void theArticleDescribesReleasedFiveOneAndNothingElse() throws IOException {
        for (Path article : List.of(ENGLISH, NORWEGIAN)) {
            String text = read(article).toLowerCase(Locale.ROOT);
            assertFalse(text.contains("venus"), article + " says nothing of Venus");
            assertTrue(text.contains("5.1"), article + " names the release it describes");
        }
    }
}
