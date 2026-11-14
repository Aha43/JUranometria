package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.meteor.MeteorShower;
import juranometria.meteor.MeteorShowerPack;
import juranometria.meteor.MeteorShowers;
import juranometria.sky.Ecliptic;
import juranometria.sky.SkyFrame;
import juranometria.solar.SolarSystemService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The accuracy claim of R-10 held against the bundled pack (#716): at
 * every printed row the Global Meteor Network's table reaches, the
 * production evaluation lies within 4 degrees of the independent video
 * radiant, within 2 degrees on a shower's average, and within 1.7
 * degrees at the maximum - the figures of section 7 of
 * {@code docs/studies/meteor-showers/measurements.md}.
 *
 * <p>The independent table is CC BY 4.0 and is not shipped. By the
 * owner's ruling of 2026-11-14 the smallest extract that holds the claim
 * is committed with the tests - the table's own lines nearest each printed
 * row and each maximum, with their source, digest, licence and attribution
 * ({@code test/resources/meteor-showers/gmn-radiant-samples.tsv}, written
 * by the importer) - so the claim is held everywhere, CI included. Where
 * the raw table is present the extract is also checked against it line by
 * line. The Quadrantids' clock time is the one instant the calendar
 * prints, and section 4 of the study measured it.
 */
class MeteorShowerAccuracyTest {

    private static final Path FIXTURE =
            Path.of("test/resources/meteor-showers/gmn-radiant-samples.tsv");

    /** One line of the extract: the table's solar longitude and the radiant it gives. */
    private record Sample(double solarLongitude, SkyPosition at) {
    }

    private static Map<String, List<Sample>> fixture() throws Exception {
        Map<String, List<Sample>> byCode = new LinkedHashMap<>();
        for (String line : Files.readAllLines(FIXTURE, StandardCharsets.US_ASCII)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String[] f = line.trim().split("\\s+");
            double sol = Double.parseDouble(f[0]);
            // Sun-centred ecliptic longitude plus the Sun's is the radiant's (the table's heading).
            byCode.computeIfAbsent(f[6], k -> new ArrayList<>()).add(new Sample(sol,
                    Ecliptic.toEquatorial(SkyFrame.normalise(Double.parseDouble(f[1]) + sol),
                            Double.parseDouble(f[2]))));
        }
        return byCode;
    }

    /** The sample at a solar longitude, or null when the extract has none within 0.11 degrees. */
    private static Sample sampleAt(Map<String, List<Sample>> fixture, String code,
                                   double solarLongitude) {
        Sample best = null;
        for (Sample g : fixture.getOrDefault(code, List.of())) {
            if (best == null || Math.abs(MeteorStudyData.signed(g.solarLongitude() - solarLongitude))
                    < Math.abs(MeteorStudyData.signed(best.solarLongitude() - solarLongitude))) {
                best = g;
            }
        }
        return best != null && Math.abs(MeteorStudyData.signed(
                best.solarLongitude() - solarLongitude)) <= 0.11 ? best : null;
    }

    @Test
    void theProductionRadiantAgreesWithTheIndependentVideoNetwork() throws Exception {
        Map<String, List<Sample>> fixture = fixture();
        MeteorShowerPack pack = MeteorShowerPack.load();
        int met = 0;
        int atMaximum = 0;
        for (MeteorShower shower : pack.showers()) {
            double sum = 0.0;
            int rows = 0;
            for (MeteorShower.Row row : shower.rows()) {
                Sample sample = sampleAt(fixture, shower.code(), row.solarLongitude());
                if (sample == null) {
                    continue;
                }
                double apart = MeteorShowers.radiantAt(shower, row.solarLongitude()).at()
                        .separationDegrees(sample.at());
                assertTrue(apart <= 4.05, shower.code() + " at " + row.solarLongitude() + ": "
                        + apart);
                sum += apart;
                rows++;
            }
            if (rows > 0) {
                assertTrue(sum / rows <= 2.0, shower.code() + " mean " + sum / rows);
            }
            met += rows;
            Sample peak = sampleAt(fixture, shower.code(), shower.maximumLongitude());
            if (peak != null) {
                double apart = MeteorShowers.radiantAt(shower, shower.maximumLongitude()).at()
                        .separationDegrees(peak.at());
                assertTrue(apart <= 1.75, shower.code() + " at maximum: " + apart);
                atMaximum++;
            }
        }
        assertTrue(met >= 20, "the premise: the independent table reaches the rows - " + met);
        assertEquals(9, atMaximum, "and every shower's maximum");
    }

    @Test
    void theExtractNamesItsSourceItsDigestItsLicenceAndItsAttribution() throws Exception {
        String text = Files.readString(FIXTURE, StandardCharsets.US_ASCII);
        for (String required : List.of(
                "gmn_shower_table_20230518.txt",
                "SHA-256 of the source file: " + MeteorStudyData.SHA256.get(
                        "gmn_shower_table_20230518.txt"),
                "CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/",
                "Global Meteor Network, https://globalmeteornetwork.org/",
                "Vida et al. 2020, MNRAS 491, 2688-2705", "Vida et al. 2021, MNRAS 506, 5046-5074",
                "lines selected, nothing else")) {
            assertTrue(text.contains(required), "the extract states: " + required);
        }
        assertEquals(9, fixture().size(), "the nine showers and no other");
    }

    @Test
    void everyLineOfTheExtractIsTheSourcesOwn() throws Exception {
        Path raw = MeteorStudyData.RAW.resolve("gmn_shower_table_20230518.txt");
        Assumptions.assumeTrue(Files.isRegularFile(raw),
                "the raw Global Meteor Network table is not on this machine");
        PinnedInputs.verifyChecksum(raw, MeteorStudyData.SHA256.get(
                "gmn_shower_table_20230518.txt"));
        Set<String> source = new HashSet<>(Files.readAllLines(raw, StandardCharsets.US_ASCII));
        for (String line : Files.readAllLines(FIXTURE, StandardCharsets.US_ASCII)) {
            if (!line.startsWith("#")) {
                assertTrue(source.contains(line), "not a line of the source: " + line);
            }
        }
    }

    @Test
    void theOneClockTimeTheCalendarPrintsIsWhereTheSunReachesTheListedLongitude()
            throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(MeteorStudyData.RAW),
                "the raw meteor-shower sources are not on this machine");
        SolarSystemService solar = SolarSystemService.load();
        MeteorShowers showers = new MeteorShowers(MeteorShowerPack.load(),
                solar::solarLongitudeJ2000Degrees);
        // "Shortly after 03h UT" on 4 January 2027 (IMO 2027 calendar, p. 3; study section 4).
        java.time.Instant found = showers.nominalMaximumAtOrAfter(
                showers.shower("QUA").orElseThrow(),
                java.time.Instant.parse("2027-01-01T00:00:00Z")).orElseThrow();
        assertTrue(found.isAfter(java.time.Instant.parse("2027-01-04T03:00:00Z"))
                && found.isBefore(java.time.Instant.parse("2027-01-04T04:00:00Z")),
                found.toString());
    }
}
