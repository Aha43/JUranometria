package juranometria.tool;

import java.nio.file.Files;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.meteor.MeteorShower;
import juranometria.meteor.MeteorShowerPack;
import juranometria.meteor.MeteorShowers;
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
 * <p>The independent table is a raw source the repository does not
 * carry ({@code imports/raw/meteor-showers}, CC BY 4.0, never shipped),
 * so this runs where the raw sources are and is skipped elsewhere. The
 * Quadrantids' clock time is the one instant the calendar prints, and
 * section 4 of the study measured it.
 */
class MeteorShowerAccuracyTest {

    @Test
    void theProductionRadiantAgreesWithTheIndependentVideoNetwork() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(MeteorStudyData.RAW),
                "the raw meteor-shower sources are not on this machine");
        SolarSystemService solar = SolarSystemService.load();
        MeteorStudyData study = MeteorStudyData.load(solar);
        MeteorShowerPack pack = MeteorShowerPack.load();
        int met = 0;
        int atMaximum = 0;
        for (MeteorShower shower : pack.showers()) {
            double sum = 0.0;
            int rows = 0;
            for (MeteorShower.Row row : shower.rows()) {
                MeteorStudyData.GmnSample sample = study.gmnAt(shower.code(), row.solarLongitude());
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
            MeteorStudyData.GmnSample peak = study.gmnAt(shower.code(),
                    shower.maximumLongitude());
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
