package juranometria.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The packaged Neptune journey (#655) restates two Horizons rows so the
 * native image can check itself without the study's files. The
 * restatements are held here to the committed rows they cite.
 */
class PackagedNeptuneSampleTest {

    @Test
    void theNeptuneSampleIsTheCommittedHorizonsRow() throws Exception {
        String row = Files.readString(Path.of("docs/studies/neptune-system/horizons/named-neptune-oslo.txt"),
                StandardCharsets.UTF_8).lines().filter(l -> l.startsWith(" 2026-Oct-29 12:00:00"))
                .findFirst().orElseThrow();
        String[] f = row.split(",");
        assertEquals(java.time.Instant.parse("2026-10-29T12:00:00Z"), PackagedAcceptanceMain.NEPTUNE_SAMPLE_INSTANT);
        assertEquals(Double.parseDouble(f[3].strip()), PackagedAcceptanceMain.NEPTUNE_SAMPLE_RA);
        assertEquals(Double.parseDouble(f[4].strip()), PackagedAcceptanceMain.NEPTUNE_SAMPLE_DEC);
    }

    @Test
    void theTritonSampleIsARowHorizonsCodesBehindAndEclipsed() throws Exception {
        String row = Files.readString(Path.of(
                        "docs/studies/neptune-system/horizons/matrix-7d-triton-oslo.txt"),
                StandardCharsets.UTF_8).lines().filter(l -> l.startsWith(" 2044-Sep-26 00:00:00"))
                .findFirst().orElseThrow();
        assertEquals(java.time.Instant.parse("2044-09-26T00:00:00Z"), PackagedAcceptanceMain.TRITON_ECLIPSE_INSTANT);
        assertEquals("U", row.split(",")[13].strip(), "occulted and in total umbral eclipse");
    }
}
