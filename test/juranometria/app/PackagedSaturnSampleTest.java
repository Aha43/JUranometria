package juranometria.app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import juranometria.tool.SaturnTableStudyMain;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The packaged Saturn journey (#575) restates one Horizons row - Oslo at
 * the named sample row - and the card's instant, so the native image can
 * check itself without the study's files. The restatements are held here
 * to the committed fixtures they cite, so they cannot drift from them.
 */
class PackagedSaturnSampleTest {

    @Test
    void theSampleIsTheCommittedHorizonsRow() throws Exception {
        String text = Files.readString(Path.of(
                "docs/studies/saturn-system/horizons/named-saturn-oslo.txt"), StandardCharsets.UTF_8);
        String row = text.lines().filter(l -> l.startsWith(" 2026-Jun-21 10:00:00"))
                .findFirst().orElseThrow();
        String[] f = row.split(",");
        assertEquals(java.time.Instant.parse("2026-06-21T10:00:00Z"),
                PackagedAcceptanceMain.SATURN_SAMPLE_INSTANT);
        assertEquals(Double.parseDouble(f[3].strip()), PackagedAcceptanceMain.SATURN_SAMPLE_RA);
        assertEquals(Double.parseDouble(f[4].strip()), PackagedAcceptanceMain.SATURN_SAMPLE_DEC);
    }

    @Test
    void theCardsInstantIsTheDailyRowNearestTheEarth() throws Exception {
        assertEquals(SaturnTableStudyMain.cardInstant(), PackagedAcceptanceMain.SATURN_CARD_INSTANT);
    }
}
