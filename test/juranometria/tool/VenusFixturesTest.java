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
 * The #500 study's fixtures carry what they say they carry (Sprint 47):
 * every committed JPL Horizons response under
 * {@code docs/studies/venus/horizons/} still hashes to the digest its
 * header records and names its request; the ruled set is complete -
 * the named instants and representative appearances for five observers
 * and geocentrically, Venus and the Sun - and indexed in its README;
 * and Espenak's rows are quoted with their attribution, sources and
 * digests. Study evidence only: nothing at build, test or run time
 * reaches Horizons or AstroPixels.
 */
class VenusFixturesTest {

    private static final Path STUDY = Path.of("docs/studies/venus");
    private static final Path HORIZONS = STUDY.resolve("horizons");

    @Test
    void eachHorizonsResponseStillHashesToTheDigestItRecords() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        String readme = Files.readString(HORIZONS.resolve("README.md"), StandardCharsets.UTF_8);
        int checked = 0;
        try (Stream<Path> files = Files.list(HORIZONS)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .sorted().toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                String recorded = text.lines()
                        .filter(l -> l.startsWith("# body-sha256: "))
                        .map(l -> l.substring("# body-sha256: ".length()))
                        .findFirst().orElseThrow();
                String body = text.substring(text.indexOf("API VERSION"));
                assertEquals(recorded, HexFormat.of().formatHex(
                                sha.digest(body.getBytes(StandardCharsets.UTF_8))),
                        file + " carries the response it was given, byte for byte");
                assertTrue(text.startsWith("# JPL Horizons response, kept whole (#500)")
                        && text.contains("# request-url: https://ssd.jpl.nasa.gov/")
                        && text.contains("# requested-utc: 2026-10-11T"),
                        file + " records its request and when it was made");
                assertTrue(text.contains("$$SOE") && text.contains("$$EOE"),
                        file + " is a complete response");
                assertTrue(readme.contains("`" + file.getFileName() + "`")
                        && readme.contains(recorded), file + " is indexed with its digest");
                checked++;
            }
        }
        assertEquals(29, checked, "twenty-nine responses, as ruled on #500: the named instants"
                + " and representative appearances for five observers and geocentrically, Venus"
                + " and the Sun (24), the hourly 2026 inferior conjunction and the fortnightly"
                + " 2026-27 apparition at Oslo, Venus and the Sun (4), the 7-day 1900-2100 matrix"
                + " at Oslo (1)");
    }

    @Test
    void theRuledSetHoldsEveryObserverAndTheEarthsCentre() {
        for (String site : List.of("oslo", "quito", "cape-town", "alert", "chatham", "geocentric")) {
            for (String kind : List.of("named", "appearances")) {
                for (String body : List.of("299", "10")) {
                    assertTrue(Files.exists(HORIZONS.resolve(kind + "-" + site + "-" + body + ".txt")),
                            kind + " at " + site + " for " + body);
                }
            }
        }
    }

    @Test
    void espenaksRowsAreQuotedWithTheirAttributionSourcesAndDigests() throws Exception {
        String events = Files.readString(STUDY.resolve("venus-events.txt"), StandardCharsets.UTF_8);
        assertTrue(events.contains("Sky Event Almanacs Courtesy of Fred Espenak, www.AstroPixels.com"));
        for (String digest : List.of(
                "251a85e49504212df9391af58daec07b72f4fee86c77fe22b443e44c1018b307",
                "4236c51d23297c270a42a3cb7a17c82881c1e5020de06b890594a1f40d80ce17",
                "1178bd6d049593e5a3f40a94e6dbdf4210637587b09a25c3972b4d79b81aecd6")) {
            assertTrue(events.contains(digest), "the almanac page's digest " + digest);
        }
        assertTrue(events.contains("Not a runtime or build input"));
        assertEquals(9, events.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).count(),
                "nine rows quoted, no more");
    }
}
