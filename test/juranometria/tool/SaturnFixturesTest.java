package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #574 study's fixtures carry what they say they carry (Sprint 52):
 * every kept JPL Horizons response under
 * {@code docs/studies/saturn-system/horizons/} still hashes to the
 * digest its header records and names its request; the two retained
 * ring pages hash to the digests their source record states, and the
 * extracted ring table names the page it came from; the IMCCE rows
 * quoted for the event comparison are attributed, carry the terms and
 * name the whole files by the digests the source record pins. Study
 * evidence only: nothing at build, test or run time reaches Horizons,
 * PDS, NSSDCA or IMCCE.
 */
class SaturnFixturesTest {

    private static final Path STUDY = Path.of("docs/studies/saturn-system");
    private static final Path HORIZONS = STUDY.resolve("horizons");
    private static final Path RINGS = STUDY.resolve("rings");

    @Test
    void eachHorizonsResponseStillHashesToTheDigestItRecords() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        int checked = 0;
        try (Stream<Path> files = Files.list(HORIZONS)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .sorted().toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (!text.startsWith("# JPL Horizons response")) {
                    continue;
                }
                String recorded = text.lines()
                        .filter(l -> l.startsWith("# body-sha256: "))
                        .map(l -> l.substring("# body-sha256: ".length()))
                        .findFirst().orElseThrow();
                String bodyText = text.substring(text.indexOf("API VERSION"));
                String actual = HexFormat.of().formatHex(
                        sha.digest(bodyText.getBytes(StandardCharsets.UTF_8)));
                assertEquals(recorded, actual, file + " carries the response it was given,"
                        + " byte for byte");
                assertTrue(text.contains("# request-url: https://ssd.jpl.nasa.gov/")
                        && text.contains("# requested-utc: 2026-10-19T"),
                        file + " records its request and when it was made");
                assertTrue(text.contains("$$SOE") && text.contains("$$EOE"),
                        file + " is a complete response");
                assertTrue(text.contains("{source: sat441l}"),
                        file + " is answered from sat441l, the study's satellite family");
                checked++;
            }
        }
        assertEquals(74, checked, "seventy-four responses kept whole: the named instants for five"
                + " observers and nine bodies, the daily 2026 year and the 7-day 1900-2100 matrices for"
                + " nine bodies at Oslo, Saturn across the 2024-2025 ring-plane window, Titan hourly"
                + " over one orbit, and the 30-day magnitudes for nine bodies");
    }

    @Test
    void theNamedInstantsAreBoundariesAndReferencesOnly() throws Exception {
        List<String> named = Files.readAllLines(HORIZONS.resolve("NAMED-CASES.txt"), StandardCharsets.UTF_8);
        assertEquals(List.of("1900-01-01 00:00:00  boundary-start",
                "1962-01-01 00:00:00  eop-record-begins",
                "1972-01-01 00:00:00  first-leap-second",
                "2000-01-01 12:00:00  j2000",
                "2017-01-01 00:00:00  last-leap-second",
                "2026-06-21 10:00:00  oslo-sample-row",
                "2026-10-19 12:00:00  today",
                "2100-12-31 23:59:59  boundary-end"), named,
                "no event instant is typed among the named cases; events come from IMCCE");
    }

    @Test
    void theRingPagesAreTheOnesTheSourceRecordPins() throws Exception {
        String sources = Files.readString(RINGS.resolve("SOURCES.md"), StandardCharsets.UTF_8);
        for (String page : List.of("saturn_tables.html", "satringfact.html")) {
            String digest = sha256(RINGS.resolve(page));
            assertTrue(sources.contains("`" + page + "`") && sources.contains("`" + digest + "`"),
                    page + " hashes to the digest rings/SOURCES.md states");
        }
        String table = Files.readString(RINGS.resolve("pds-rings-table.tsv"), StandardCharsets.UTF_8);
        assertTrue(table.lines().findFirst().orElseThrow()
                        .contains(sha256(RINGS.resolve("saturn_tables.html"))),
                "the extracted table names the retained PDS page by its digest");
        assertTrue(table.contains("Feature\tInner Boundary (km)\tOuter Boundary (km)"),
                "the table keeps the page's own column heads");
    }

    @Test
    void theQuotedImcceRowsAreAttributedAndNameTheirFiles() throws Exception {
        String quoted = Files.readString(STUDY.resolve("imcce-quoted-rows.txt"), StandardCharsets.UTF_8);
        String sources = Files.readString(STUDY.resolve("SOURCES.md"), StandardCharsets.UTF_8);
        assertTrue(quoted.contains("IMCCE") && quoted.contains("TASS 1.6") && quoted.contains("W. Thuillot"),
                "the quoted rows name IMCCE, the theory and the author");
        assertTrue(quoted.contains("can be used as an example to illustrate documents if the source is"
                + " clearly identified"), "the quoted rows carry IMCCE's terms");
        int rows = 0;
        for (String line : quoted.lines().toList()) {
            if (line.startsWith("# imcce-phensaturne-")) {
                String digest = line.substring(line.indexOf("sha256 ") + 7).strip();
                assertTrue(sources.contains("`" + digest + "`"),
                        line + " names a whole file by the digest SOURCES.md pins");
            } else if (!line.startsWith("#")) {
                assertTrue(line.matches("imcce-phensaturne-phenE-S[1-8]\\.dat:\\d+\t.*"),
                        line + " gives its file and line");
                rows++;
            }
        }
        assertEquals(24, rows, "the minimum quoted: the worst event of each satellite and kind");
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}
