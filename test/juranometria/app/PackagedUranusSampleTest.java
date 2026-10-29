package juranometria.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The packaged Uranus journey (#634) restates two Horizons rows so the
 * native image can check itself without the study's files. The
 * restatements are held here to the committed rows they cite.
 */
class PackagedUranusSampleTest {

    @Test
    void theUranusSampleIsTheCommittedHorizonsRow() throws Exception {
        String row = Files.readString(Path.of("docs/studies/uranus-system/horizons/named-uranus-oslo.txt"),
                StandardCharsets.UTF_8).lines().filter(l -> l.startsWith(" 2026-Oct-27 12:00:00"))
                .findFirst().orElseThrow();
        String[] f = row.split(",");
        assertEquals(java.time.Instant.parse("2026-10-27T12:00:00Z"), PackagedAcceptanceMain.URANUS_SAMPLE_INSTANT);
        assertEquals(Double.parseDouble(f[3].strip()), PackagedAcceptanceMain.URANUS_SAMPLE_RA);
        assertEquals(Double.parseDouble(f[4].strip()), PackagedAcceptanceMain.URANUS_SAMPLE_DEC);
    }

    @Test
    void theMoonsSampleIsEspenaksOppositionHour() throws Exception {
        String row = Files.readString(Path.of("docs/studies/uranus-system/uranus-events.txt"),
                StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("2026 Nov 25 23 Uranus at Opposition"))
                .findFirst().orElseThrow();
        assertEquals(java.time.Instant.parse("2026-11-25T23:00:00Z"),
                PackagedAcceptanceMain.URANUS_MOONS_SAMPLE_INSTANT, row);
    }

    @Test
    void theMirandaSampleIsARowHorizonsCodesBehindAndEclipsed() throws Exception {
        String row = Files.readString(Path.of(
                        "docs/studies/uranus-system/horizons/matrix-7d-miranda-oslo.txt"),
                StandardCharsets.UTF_8).lines().filter(l -> l.startsWith(" 2005-Mar-19 00:00:00"))
                .findFirst().orElseThrow();
        assertEquals(java.time.Instant.parse("2005-03-19T00:00:00Z"), PackagedAcceptanceMain.MIRANDA_ECLIPSE_INSTANT);
        assertEquals("U", row.split(",")[13].strip(), "occulted and in total umbral eclipse");
    }
}
