package juranometria.solar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.sky.SkyFrame;
import juranometria.solar.time.TimeScales;
import juranometria.tool.SeasonalEventsFixture;
import juranometria.tool.SeasonalEventsFixture.Event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The named seasonal instants are the published ones, wherever they
 * appear (issue #399, owner checkpoint).
 *
 * <p>The first table labelled 02:24 UTC as the June solstice and
 * 22:05 UTC on the 22nd as the September equinox; the published
 * values are 08:24 and 00:05 on the 23rd. The positions were right
 * for the instants supplied and the names attached to them were not.
 * So the instants now come from one cited fixture, and this test
 * holds every place that names an event to it - the Horizons
 * requests, the named-case list and the generated table - without
 * computing a single solar position. Under the old values every one
 * of those assertions fails.
 *
 * <p>A second test does use the calculation, deliberately and
 * separately: at the published instants the Sun's apparent longitude
 * must be at its cardinal value, which checks the fixture and the
 * service against each other.
 */
class SeasonalEventsTest {

    private static final Path HORIZONS = Path.of("docs/studies/solar-system/horizons");
    private static final Path TABLE = Path.of("docs/studies/solar-system/measurements.md");

    @Test
    void theFixtureIsTheImcceTableQuotedWithItsSource() throws IOException {
        String text = Files.readString(SeasonalEventsFixture.FIXTURE,
                StandardCharsets.UTF_8);
        for (String required : List.of("imcce.fr", "Rocher",
                "Equinoxe_printemps_1583_2999.pdf", "Solstice_ete_1583_2999.pdf",
                "Equinoxe_automne_1583_2999.pdf", "Solstice_hiver_1583_2999.pdf",
                "sha256", "retrieved")) {
            assertTrue(text.contains(required), "the fixture cites " + required);
        }
        Map<String, Event> events = SeasonalEventsFixture.read();
        assertEquals(SeasonalEventsFixture.EVENTS, new ArrayList<>(events.keySet()));
        assertEquals(Instant.parse("2026-06-21T08:24:26Z"),
                events.get("june-solstice").instant(),
                "the June solstice as the IMCCE publishes it, to the second");
        assertEquals(Instant.parse("2026-09-23T00:05:08Z"),
                events.get("september-equinox").instant());
        for (Event e : events.values()) {
            assertTrue(e.asPublished().contains("2026") && e.asPublished().contains("UT"),
                    e.name() + " quotes the published row: " + e.asPublished());
        }
    }

    @Test
    void everyHorizonsNamedRequestAskedForTheFixtureInstants() throws IOException {
        Map<String, Event> events = SeasonalEventsFixture.read();
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> files = Files.list(HORIZONS)) {
            for (Path file : files.filter(p -> p.getFileName().toString()
                    .startsWith("named-")).sorted().toList()) {
                String url = Files.readAllLines(file, StandardCharsets.UTF_8)
                        .stream().filter(l -> l.startsWith("# request-url: "))
                        .findFirst().orElseThrow();
                String decoded = java.net.URLDecoder.decode(url,
                        StandardCharsets.UTF_8);
                for (Event e : events.values()) {
                    String stamp = e.instant().toString()
                            .replace("T", " ").replace("Z", "");
                    if (!decoded.contains("'" + stamp + "'")) {
                        wrong.add(file.getFileName() + " was not asked for "
                                + e.name() + " at " + stamp);
                    }
                }
                checked++;
            }
        }
        assertEquals(5, checked, "five observers' named responses");
        assertEquals(List.of(), wrong,
                "each named response was fetched for the fixture's instants");
    }

    @Test
    void theNamedCaseListAndTheTableCarryTheFixtureInstants() throws IOException {
        Map<String, Event> events = SeasonalEventsFixture.read();
        String cases = Files.readString(HORIZONS.resolve("NAMED-CASES.txt"),
                StandardCharsets.UTF_8);
        String table = Files.readString(TABLE, StandardCharsets.UTF_8);
        List<String> wrong = new ArrayList<>();
        for (Event e : events.values()) {
            String stamp = e.instant().toString().replace("T", " ").replace("Z", "");
            if (!cases.contains(stamp + "  " + e.name() + "-2026-imcce")) {
                wrong.add("NAMED-CASES.txt lacks " + e.name() + " at " + stamp);
            }
            // The table shows the instant to the minute, rounded.
            String minute = e.instant().plusSeconds(30).toString()
                    .substring(0, 16).replace("T", " ");
            String label = switch (e.name()) {
                case "march-equinox" -> "March equinox 2026 (IMCCE)";
                case "june-solstice" -> "June solstice 2026 (IMCCE)";
                case "september-equinox" -> "September equinox 2026 (IMCCE)";
                default -> "December solstice 2026 (IMCCE)";
            };
            if (!table.contains("| " + label + " | " + minute + " |")) {
                wrong.add("the table lacks the row \"" + label + " | " + minute + "\"");
            }
        }
        assertEquals(List.of(), wrong);
        assertTrue(!table.contains("2026-06-21 02:24") && !table.contains("2026-09-22 22:05"),
                "the wrong instants are gone from the table");
        assertTrue(table.contains("Near local solar midnight, midsummer (23:00 UTC)")
                && !table.contains("| Local midnight"),
                "23:00 UTC is near, not at, Oslo's solar midnight, and says so");
    }

    /**
     * The one check that uses the calculation: at the published
     * instants the Sun's apparent geocentric longitude of date is at
     * 0°, 90°, 180° or 270°. The IMCCE's seconds and this service's
     * 0.14″ agreement with Horizons put the tolerance at 1″ - twenty
     * seconds of the Sun's motion; the old June instant misses by six
     * hours, which is 900″.
     */
    @Test
    void atThePublishedInstantsTheSunIsAtTheCardinalLongitude() throws IOException {
        SolarSystemService service = SolarSystemService.load();
        Map<String, Event> events = SeasonalEventsFixture.read();
        double[] cardinal = {0.0, 90.0, 180.0, 270.0};
        int i = 0;
        for (Event e : events.values()) {
            TimeScales.Epoch epoch = service.timeScales().tt(e.instant());
            SkyPosition apparent = service.geocentricApparentOfDate(epoch.jdTt());
            double centuries = SkyFrame.centuries(epoch.jdTt());
            double obliquity = Math.toRadians(SkyFrame.meanObliquityDegrees(centuries)
                    + SkyFrame.nutationDegrees(centuries)[1]);
            double ra = Math.toRadians(apparent.raDegrees());
            double dec = Math.toRadians(apparent.decDegrees());
            double longitude = Math.toDegrees(Math.atan2(
                    Math.sin(ra) * Math.cos(obliquity) + Math.tan(dec) * Math.sin(obliquity),
                    Math.cos(ra)));
            double off = ((longitude - cardinal[i] + 540.0) % 360.0 - 180.0) * 3600.0;
            assertTrue(Math.abs(off) <= 1.0, e.name() + ": apparent longitude of"
                    + " date is " + cardinal[i] + "° within 1″ at the published"
                    + " instant; off by " + off + "″");
            i++;
        }
    }
}
