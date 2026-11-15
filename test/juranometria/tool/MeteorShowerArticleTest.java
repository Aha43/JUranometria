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
 * The meteor-shower article (issue #717) is held to the feature as ruled
 * on #715 and built by #716 for the release that follows 6.5: every
 * figure is an evidence page byte for byte, every cited number stands in
 * the report it is cited from, every word of the panel, the page or the
 * interface it quotes stands where the atlas says it, the two languages
 * carry the same numbers, both are listed where a reader finds them, and
 * the text carries the source's credit without claiming an affiliation
 * or a release. Text is compared with its whitespace collapsed, so a
 * line break in the prose or the report never hides a citation.
 *
 * <p>Nothing here reads a raw source: the reports cited are committed.
 */
class MeteorShowerArticleTest {

    private static final Path ENGLISH =
            Path.of("docs/articles/meteor-showers-and-their-radiants.md");
    private static final Path NORWEGIAN =
            Path.of("docs/articles/meteorsvermer-og-radiantene-deres.md");
    private static final Path IMAGES = Path.of("docs/articles/images");
    private static final String STUDY_DIR = "docs/studies/meteor-radiants/";

    /** Each figure, and the evidence page it is a byte-for-byte copy of. */
    private static final Map<String, String> FIGURES = Map.of(
            "meteor-perseids-night-en-white", "perseids-night-en-white",
            "meteor-perseids-night-nb-black", "perseids-night-nb-black",
            "meteor-perseids-early-en-white", "perseids-early-en-white",
            "meteor-perseids-late-en-white", "perseids-late-en-white",
            "meteor-eta-aquariids-below-horizon-en-white", "eta-aquariids-below-horizon-en-white",
            "meteor-geminids-evening-en-white", "geminids-evening-en-white",
            "meteor-geminids-whole-sky-en-white", "geminids-whole-sky-en-white");

    private static final String DECISION = "docs/decisions/meteor-showers.md";
    private static final String BUILT = "docs/studies/meteor-radiants/measurements.md";
    private static final String STUDY = "docs/studies/meteor-showers/measurements.md";
    private static final String SOURCES = "docs/studies/meteor-showers/sources.md";
    private static final String ENGLISH_WORDS = "src/resources/interface-language/en.properties";
    private static final String NORWEGIAN_WORDS = "src/resources/interface-language/nb-NO.properties";

    /** Each cited number, verbatim in both languages, and the report it is read from. */
    private static final List<String[]> CITATIONS = List.of(
            new String[] {"59.91 N, 10.75 E", BUILT},
            new String[] {"33.93 S, 18.42 E", BUILT},
            new String[] {"2027-08-13", BUILT},
            new String[] {"00:00", BUILT},
            new String[] {"2027-07-20", BUILT},
            new String[] {"2027-08-20", BUILT},
            new String[] {"2027-05-06", BUILT},
            new String[] {"23:00", BUILT},
            new String[] {"2027-12-14", BUILT},
            new String[] {"20:00", BUILT},
            // The nine showers: the calendar's activity, then the listed
            // solar longitude of the maximum beside the listed rate.
            new String[] {"Dec 28 - Jan 12", BUILT}, new String[] {"283.15 | 80+", BUILT},
            new String[] {"Apr 14 - Apr 30", BUILT}, new String[] {"32.32 | 18", BUILT},
            new String[] {"Apr 19 - May 28", BUILT}, new String[] {"45.5 | 50", BUILT},
            new String[] {"Jul 12 - Aug 23", BUILT}, new String[] {"128 | 25", BUILT},
            new String[] {"Jul 17 - Aug 24", BUILT}, new String[] {"140.0 | 110+", BUILT},
            new String[] {"Oct 02 - Nov 07", BUILT}, new String[] {"208 | 20", BUILT},
            new String[] {"Nov 06 - Nov 30", BUILT}, new String[] {"235.27 | 15+", BUILT},
            new String[] {"Dec 04 - Dec 20", BUILT}, new String[] {"262.2 | 150", BUILT},
            new String[] {"Dec 17 - Dec 26", BUILT}, new String[] {"270.7 | 10", BUILT},
            // The Perseids' nominal maximum in three consecutive years.
            new String[] {"2026-08-13 02:00", BUILT},
            new String[] {"2027-08-13 08:11", BUILT},
            new String[] {"2028-08-12 14:28", BUILT},
            // The radiant's highest in the dark, from two places.
            new String[] {"-4.1", BUILT}, new String[] {"+46.3", BUILT},
            new String[] {"+71.3", BUILT}, new String[] {"+60.3", BUILT},
            new String[] {"-1.7", BUILT}, new String[] {"+63.0", BUILT},
            new String[] {"+23.1", BUILT},
            // At the nominal maximum from Oslo: instant, radiant, Sun.
            new String[] {"| 2027-05-06 15:11 | -16.6 | +29.4 |", BUILT},
            new String[] {"| 2027-08-13 08:11 | +66.8 | +33.1 |", BUILT},
            new String[] {"| 2027-12-14 19:53 | +32.5 | -39.9 |", BUILT},
            // The parent bodies, as the pack states them.
            new String[] {"109P/Swift-Tuttle", BUILT},
            new String[] {"55P/Tempel\u2013Tuttle", BUILT},
            new String[] {"8P/Tuttle", BUILT},
            new String[] {"C/1861 G1 (Thatcher)", BUILT},
            new String[] {"1P/Halley", BUILT},
            new String[] {"(3200) Phaethon", BUILT},
            new String[] {"2003 EH1", BUILT},
            // The calendar's own definition of the ZHR, and the chance
            // that an unrelated meteor lines up with a radiant.
            new String[] {"+6.5", SOURCES},
            new String[] {"15.6%", STUDY},
            new String[] {"7.3%", STUDY});

    /**
     * Each cited phrase: the report's words, then the English and the
     * Norwegian article's own rendering of them - a number with the
     * language's own unit word, a count in the language's own.
     */
    private static final List<String[]> PHRASES = List.of(
            new String[] {"For 5 of the nine the nominal maximum falls on a different UTC"
                    + " calendar date", "**5 of the nine**", "**5 av de ni**", BUILT},
            new String[] {"on the sky 22.1 and 4.5 degrees", "**22.1** degrees",
                    "**22.1** grader", BUILT},
            new String[] {"on the sky 22.1 and 4.5 degrees", "another **4.5**", "**4.5** til",
                    BUILT},
            new String[] {"between 4.0 and 37.7 degrees", "between **4.0 and 37.7 degrees**",
                    "mellom **4.0 og 37.7 grader**", BUILT},
            new String[] {"at worst 4.0°", "at worst **4.0°**", "i verste fall **4.0°**",
                    DECISION},
            new String[] {"| 45.82 | 338.6 -0.2 | between rows | -14.0 |",
                    "**14.0** degrees below the mathematical horizon",
                    "**14.0** grader under den matematiske horisonten", BUILT},
            new String[] {"| +60.3 | 01:12 | 0.87 |", "the sine of that is **0.87**",
                    "sinus til det er **0.87**", BUILT},
            new String[] {"| no such dark |", "| no such dark |", "| ikke noe slikt mørke |",
                    BUILT},
            new String[] {"3 of the nine nominal maxima of 2027 fall with the Sun above the"
                    + " horizon and 2 with the radiant below it", "**3 of the nine**",
                    "**3 av de ni**", BUILT},
            new String[] {"| 16.1 h | 14.3 h | 12.6 h |", "**12.6 h** of astronomical darkness",
                    "**12.6 timer** astronomisk mørke", BUILT},
            new String[] {"25%, +12.4, 146", "| 2026 | 25% | 146 |", "| 2026 | 25% | 146 |",
                    BUILT},
            new String[] {"98%, +32.6, 15", "| 2027 | 98% | 15 |", "| 2027 | 98% | 15 |", BUILT},
            new String[] {"5%, -30.8, 128", "| 2028 | 5% | 128 |", "| 2028 | 5% | 128 |", BUILT},
            new String[] {"| 128 | 25 | 2.5 | 41 | 9 | - |",
                    "For the Southern Delta Aquariids the atlas states no parent",
                    "For Sørlige delta-akvarider oppgir atlaset ikke noe opphav", BUILT},
            new String[] {"the radiant in whole degrees at dates five days apart",
                    "prints a position every five days", "trykker en posisjon hver femte dag",
                    STUDY});

    /**
     * What the panel and the page themselves say, quoted in each language
     * as the as-built report records it: the English words, the Norwegian
     * words, and the report both stand in.
     */
    private static final List<String[]> SAID = List.of(
            new String[] {"No radiant is drawn or described",
                    "Ingen radiant tegnes eller beskrives", BUILT},
            new String[] {"Activity period: Jul 17 to Aug 24 in the calendar, solar longitude"
                    + " 113.9° to 151.2°.",
                    "Aktivitetsperiode: Jul 17 til Aug 24 i kalenderen, sollengde 113.9° til"
                    + " 151.2°.", BUILT},
            new String[] {"Sun more than 6° below the horizon: 6.4 h; more than 12°: 3.8 h; more"
                    + " than 18°: none.",
                    "Solen mer enn 6° under horisonten: 6.4 t; mer enn 12°: 3.8 t; mer enn 18°:"
                    + " ingen.", BUILT},
            new String[] {"Eta Aquariids (below the horizon)", "Eta-akvaridene (under horisonten)",
                    BUILT},
            new String[] {"Perseids (the radiant of a meteor shower)",
                    "Perseidene (radianten til en meteorsverm)", BUILT});

    /**
     * The interface's own words, each as its language file holds it: a
     * control is called what the atlas calls it, and what the panel says
     * is quoted, not paraphrased.
     */
    private static final List<String[]> WORDS = List.of(
            new String[] {"Meteor showers...", "Meteorsvermer..."},
            new String[] {"Set chart time to nominal maximum",
                    "Sett karttiden til nominelt maksimum"},
            new String[] {"Centre on radiant", "Sentrer på radianten"},
            new String[] {"Place and Time", "Sted og tid"},
            new String[] {"Subdue below horizon", "Demp under horisonten"},
            new String[] {"known to about ten minutes", "kjent til omtrent ti minutter"},
            new String[] {"known to an hour or two", "kjent til en time eller to"},
            new String[] {"known to about a day", "kjent til omtrent et døgn"},
            new String[] {"has passed", "er passert"},
            new String[] {"the rate for an ideal dark sky with the radiant overhead, not what an"
                    + " observer will count",
                    "raten for en ideell mørk himmel med radianten i senit, ikke det en"
                    + " observatør vil telle"},
            new String[] {"Nothing here judges the night or forecasts the weather.",
                    "Ingenting her vurderer natten eller varsler været."},
            new String[] {"Meteor shower radiant: the point the shower's meteors appear to come"
                    + " from, at the chart time stated.",
                    "Radiant for meteorsverm: punktet svermens meteorer ser ut til å komme fra,"
                    + " ved karttiden som er oppgitt."});

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
     * The same thing at the same precision (#717): every figure written
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
        assertTrue(index.contains("meteor-showers-and-their-radiants.md")
                && index.contains("meteorsvermer-og-radiantene-deres.md"),
                "the articles index lists both");
        String build = read(Path.of("scripts/build-articles.py"));
        assertTrue(build.contains("meteor-showers-and-their-radiants.md")
                && build.contains("meteorsvermer-og-radiantene-deres.md"),
                "the Pages build publishes both");
    }

    /**
     * Destined for 6.6, not yet released: the text names the release and
     * frames it as the one that follows 6.5, never as published. It
     * carries the IMO's credit exactly and disclaims affiliation, as the
     * owner's ruling of 2026-11-14 requires of every surface that shows
     * these values.
     */
    @Test
    void theArticleDescribesWhatIsDestinedForSixSixAndCreditsItsSource() throws IOException {
        String english = read(ENGLISH);
        String norwegian = read(NORWEGIAN);
        assertTrue(english.contains("6.6") && english.contains("the release that follows 6.5"));
        assertTrue(norwegian.contains("6.6") && norwegian.contains("utgaven som følger etter 6.5"));
        for (String text : List.of(english.toLowerCase(Locale.ROOT),
                norwegian.toLowerCase(Locale.ROOT))) {
            for (String claim : List.of("has been released", "was released", "already released",
                    "er utgitt", "ble utgitt", "allerede utgitt")) {
                assertFalse(text.contains(claim), "the text does not say '" + claim + "'");
            }
        }
        String credit = "Source: International Meteor Organization (IMO) \u2013 www.imo.net";
        assertTrue(read(Path.of(BUILT)).contains(credit), "the credit is the pack's own");
        assertTrue(english.contains(credit) && norwegian.contains(credit),
                "both texts carry the credit exactly");
        assertTrue(english.contains("not affiliated with or endorsed by the IMO")
                && norwegian.contains("ikke tilknyttet eller anbefalt av IMO"),
                "and claim no affiliation");
        assertTrue(english.contains("IMO Meteor Shower Calendar 2027")
                && norwegian.contains("IMO Meteor Shower Calendar 2027"), "the edition is named");
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
