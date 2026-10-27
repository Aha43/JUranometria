package juranometria.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The packaged Mars journeys (#602, #606) restate two Horizons rows and one
 * of the Mars-on-the-chart study's instants so the native image can check
 * itself without the study's files. The restatements are held here to the
 * committed rows and report they cite.
 */
class PackagedMarsSampleTest {

    @Test
    void theMarsSampleIsTheCommittedHorizonsRow() throws Exception {
        String row = Files.readString(Path.of("docs/studies/mars-system/horizons/named-mars-oslo.txt"),
                StandardCharsets.UTF_8).lines().filter(l -> l.startsWith(" 2026-Oct-25 12:00:00"))
                .findFirst().orElseThrow();
        String[] f = row.split(",");
        assertEquals(java.time.Instant.parse("2026-10-25T12:00:00Z"), PackagedAcceptanceMain.MARS_SAMPLE_INSTANT);
        assertEquals(Double.parseDouble(f[3].strip()), PackagedAcceptanceMain.MARS_SAMPLE_RA);
        assertEquals(Double.parseDouble(f[4].strip()), PackagedAcceptanceMain.MARS_SAMPLE_DEC);
    }

    @Test
    void thePhobosSampleIsARowHorizonsCodesBehindAndEclipsed() throws Exception {
        String row = Files.readString(Path.of(
                        "docs/studies/mars-system/horizons/opposition-2018-phobos-geocentric-10m.txt"),
                StandardCharsets.UTF_8).lines().filter(l -> l.startsWith(" 2018-Jul-26 12:30:00"))
                .findFirst().orElseThrow();
        assertEquals(java.time.Instant.parse("2018-07-26T12:30:00Z"), PackagedAcceptanceMain.PHOBOS_ECLIPSE_INSTANT);
        assertEquals("U", row.split(",")[13].strip(), "occulted and in total umbral eclipse");
    }

    @Test
    void theMoonsSampleIsTheStudysDeimosPage() throws Exception {
        String heading = Files.readString(Path.of("docs/studies/mars-on-the-chart/measurements.md"),
                StandardCharsets.UTF_8).lines()
                .filter(l -> l.startsWith("### Deimos near its greatest elongation, "))
                .findFirst().orElseThrow();
        assertEquals(java.time.Instant.parse(heading.substring(heading.indexOf(", ") + 2,
                        heading.indexOf(" (the page's field"))),
                PackagedAcceptanceMain.MARS_MOONS_SAMPLE_INSTANT);
    }
}
