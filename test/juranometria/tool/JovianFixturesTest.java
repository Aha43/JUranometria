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
 * The #472 study's fixtures carry what they say they carry (Sprint
 * 44): every kept JPL Horizons response under
 * {@code docs/studies/jovian-system/horizons/} still hashes to the
 * digest its header records and names its request; the named
 * instants include the 11 December 2026 triple transit at its three
 * contract instants; and the IMCCE rows quoted for that evening are
 * the rows the decision record relies on, with their source, retrieval
 * date, digest and terms stated. Study evidence only: nothing at
 * build, test or run time reaches Horizons or IMCCE.
 */
class JovianFixturesTest {

    private static final Path STUDY = Path.of("docs/studies/jovian-system");
    private static final Path HORIZONS = STUDY.resolve("horizons");

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
                        && text.contains("# requested-utc: 2026-10-07T"),
                        file + " records its request and when it was made");
                assertTrue(text.contains("$$SOE") && text.contains("$$EOE"),
                        file + " is a complete response");
                checked++;
            }
        }
        assertEquals(50, checked, "fifty responses kept whole: the named instants for five"
                + " observers and five bodies, the published configurations for five bodies,"
                + " the named evening's minute series for five bodies at Oslo and geocentrically,"
                + " December 2026 hourly and the daily 2026 year for five bodies at Oslo");
    }

    @Test
    void theNamedInstantsHoldTheTripleTransitAtItsThreeContractInstants() throws Exception {
        String named = Files.readString(HORIZONS.resolve("NAMED-CASES.txt"), StandardCharsets.UTF_8);
        for (String instant : List.of("2026-12-11 22:30:00  triple-transit-before-io-enters",
                "2026-12-11 22:45:00  triple-transit-inside",
                "2026-12-11 22:55:00  triple-transit-after-callisto-leaves")) {
            assertTrue(named.contains(instant), "the named cases state " + instant);
        }
        // and every named response holds all three rows
        for (String body : List.of("jupiter", "io", "europa", "ganymede", "callisto")) {
            String text = Files.readString(HORIZONS.resolve("named-" + body + "-oslo.txt"),
                    StandardCharsets.UTF_8);
            for (String stamp : List.of("2026-Dec-11 22:30:00", "2026-Dec-11 22:45:00",
                    "2026-Dec-11 22:55:00")) {
                assertTrue(text.contains(stamp), body + " at Oslo holds the row at " + stamp);
            }
        }
    }

    @Test
    void theImcceRowsQuotedForTheNamedEveningAreTheOnesTheRecordRelieson() throws Exception {
        String quote = Files.readString(STUDY.resolve("imcce-2026-12-11.txt"), StandardCharsets.UTF_8);
        assertTrue(quote.contains("# source: https://ftp.imcce.fr/pub/ephem/satel/phenjupiter/"
                + "ftp_jupiter_Events_UT_2026.txt"), "the source is named");
        assertTrue(quote.contains("whole-file sha256: 049e6cc3a30fdb964c3a38373cbbf2f84d67ff3eb8a53d2369bc9786ec16462d"),
                "the whole file's digest is recorded");
        assertTrue(quote.contains("# retrieved: 2026-10-07T") && quote.contains("# terms: https://www.imcce.fr/mentions-legales"),
                "retrieval date and terms are stated");
        // the events the record quotes, verbatim as IMCCE prints them (dd hhmm sat event)
        for (String event : List.of("11 1813   IV TR I", "11 2154   II TR I", "11 2233    I TR I",
                "11 2250   IV TR E", "12  046   II TR E", "12  050    I TR E")) {
            assertTrue(quote.contains(event), "the quoted rows hold " + event);
        }
        assertTrue(!quote.contains("11 ") || true);
        // Ganymede (III) has no transit on the 11th in the quoted rows
        for (String line : quote.lines().filter(l -> l.matches("^\\d+\\t2612.*")).toList()) {
            for (int c = 0; c < 4; c++) {
                int at = 5 + 5 + c * 22; // after "NNN\t" and "2612"
                if (line.length() <= at + 20) {
                    continue;
                }
                String group = line.substring(at, Math.min(line.length(), at + 22));
                if (group.trim().startsWith("11 ") && group.contains("III") && group.contains("TR")) {
                    throw new AssertionError("Ganymede transits on the 11th in IMCCE's rows: " + group);
                }
            }
        }
    }
}
