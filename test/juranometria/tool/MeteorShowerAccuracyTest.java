package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * by the importer) - so the claim is held everywhere, CI included.
 *
 * <p>Nothing here reads a raw source or is skipped for want of one
 * (owner's ruling, 2026-11-14, after two such assumptions aborted on
 * CI's display run): fidelity to the raw table and to the calendar's
 * printed clock time is the importer's to refuse on
 * ({@code make import-meteor-showers}); these tests hold the committed
 * pack, the committed extract, its attribution, the calculation and the
 * expected clock result.
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
    void theOneClockTimeTheCalendarPrintsIsWhereTheSunReachesTheListedLongitude()
            throws Exception {
        SolarSystemService solar = SolarSystemService.load();
        MeteorShowers showers = new MeteorShowers(MeteorShowerPack.load(),
                solar::solarLongitudeJ2000Degrees);
        // "Maximum: January 4, 03h25m UT", "shortly after 03h UT" (IMO 2027 calendar, pp. 3-4;
        // study section 4). The importer reads those words in the pinned text; here the
        // committed pack and the production Sun give the expected clock result.
        Instant found = showers.nominalMaximumAtOrAfter(
                showers.shower(MeteorShowerPackMain.CLOCK_TIME_SHOWER).orElseThrow(),
                Instant.parse("2027-01-01T00:00:00Z")).orElseThrow();
        assertEquals(Instant.parse("2027-01-04T03:25:00Z"), MeteorShowerPackMain.CLOCK_TIME);
        assertTrue(found.isAfter(Instant.parse("2027-01-04T03:00:00Z")), found.toString());
        long off = Math.abs(found.getEpochSecond()
                - MeteorShowerPackMain.CLOCK_TIME.getEpochSecond());
        assertTrue(off <= MeteorShowerPackMain.CLOCK_TIME_TOLERANCE_SECONDS,
                found + " is " + off + " s from the printed time");
    }

    @Test
    void everyDataLineOfTheExtractIsATableLineOfOneOfTheNineShowers() throws Exception {
        Set<String> codes = new HashSet<>();
        for (MeteorShower shower : MeteorShowerPack.load().showers()) {
            codes.add(shower.code());
        }
        int lines = 0;
        for (String line : Files.readAllLines(FIXTURE, StandardCharsets.US_ASCII)) {
            if (line.startsWith("#")) {
                continue;
            }
            String[] f = line.trim().split("\\s+");
            assertEquals(7, f.length, line);
            assertTrue(codes.contains(f[6]), line);
            lines++;
        }
        assertEquals(44, lines, "the extract the importer derived and verified");
    }
}
