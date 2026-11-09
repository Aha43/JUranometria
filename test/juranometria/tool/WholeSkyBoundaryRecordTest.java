package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The decision record's account of the ecliptic poles and high
 * latitudes (#709, docs/decisions/all-sky-projection.md section 9) is
 * held to the as-built report it reads from (section 12 of
 * docs/studies/whole-sky/measurements.md): every number the record
 * quotes stands in the report's own row, so the record cannot drift
 * from a regenerated measurement. Whitespace is collapsed, so a line
 * break never hides a citation.
 */
class WholeSkyBoundaryRecordTest {

    private static final Path RECORD = Path.of("docs/decisions/all-sky-projection.md");
    private static final Path REPORT = Path.of("docs/studies/whole-sky/measurements.md");

    /** The report's row or sentence, then the record's words for it. */
    private static final List<String[]> CITED = List.of(
            new String[] {"declination of date: 66.5588 at this instant",
                    "declination of date (66.5588 at the measured instant)"},
            new String[] {"| 59.91 | 6.65 degrees | goes all the way round | 1.09 |",
                    "at Oslo its fastest turn is 1.09 degrees per minute"},
            new String[] {"| 66 | 0.56 degrees | goes all the way round | 10.5 | 11 min |",
                    "At latitude 66 the fastest turn is 10.5 degrees per minute and a quarter"
                            + " turn takes 11 minutes"},
            new String[] {"| 66.5 | 0.06 degrees | goes all the way round | 97.4 | 71 s |",
                    "at 66.5, 97.4 degrees per minute and 71 seconds"},
            new String[] {"| 67 | 0.44 degrees | swings 158 degrees and back, about longitude 90 |",
                    "158 degrees at latitude 67"},
            new String[] {"| 70 | 3.44 degrees | swings 119 degrees and back, about longitude 90 |",
                    "119 at 70"},
            new String[] {"| 80 | 13.44 degrees | swings 52 degrees and back, about longitude 90 |",
                    "52 at 80"},
            new String[] {"| -67 | 0.44 degrees | swings 158 degrees and back, about longitude 270 |",
                    "about ecliptic longitude 90 in the north and 270 in the south"});

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    }

    @Test
    void everyNumberTheRecordQuotesStandsInTheReport() throws IOException {
        String record = read(RECORD);
        String report = read(REPORT);
        for (String[] cited : CITED) {
            assertTrue(report.contains(cited[0]), "the report says: " + cited[0]);
            assertTrue(record.contains(cited[1]), "the record says: " + cited[1]);
        }
    }

    /** No boundary page failed to paint, and none failed a check. */
    @Test
    void everyBoundaryPagePaintsAndHolds() throws IOException {
        String report = Files.readString(REPORT, StandardCharsets.UTF_8);
        int rows = 0;
        for (String line : report.split("\n")) {
            if (line.startsWith("| north | ") || line.startsWith("| south | ")) {
                rows++;
                assertTrue(line.contains("| yes | "), "painted: " + line);
                assertTrue(!line.contains("| no |") && !line.contains("NO:"),
                        "no check failed: " + line);
                assertTrue(line.contains("| 9 |"), "nine bodies: " + line);
            }
        }
        assertTrue(rows == 24, "twelve places at each pole: " + rows);
    }
}
