package juranometria.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The packaged Mercury journey (#529) restates one Horizons row - Oslo
 * at the 2026 greatest eastern elongation - so the native image can
 * check itself without the study's files. The restatement is held here
 * to the committed row it cites, so it cannot drift from it.
 */
class PackagedMercurySampleTest {

    @Test
    void theSampleIsTheCommittedHorizonsRow() throws Exception {
        String text = Files.readString(
                Path.of("docs/studies/mercury/horizons/elongations-oslo-199.txt"),
                StandardCharsets.UTF_8);
        String row = text.lines().filter(l -> l.startsWith(" 2026-Feb-19 18:00:00"))
                .findFirst().orElseThrow();
        String[] f = row.split(",");
        assertEquals(java.time.Instant.parse("2026-02-19T18:00:00Z"),
                PackagedAcceptanceMain.MERCURY_SAMPLE_INSTANT);
        assertEquals(Double.parseDouble(f[3].strip()), PackagedAcceptanceMain.MERCURY_SAMPLE_RA);
        assertEquals(Double.parseDouble(f[4].strip()), PackagedAcceptanceMain.MERCURY_SAMPLE_DEC);
    }
}
