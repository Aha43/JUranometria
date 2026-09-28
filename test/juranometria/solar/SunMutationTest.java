package juranometria.solar;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solar.SunReferenceVectorTest.Row;
import juranometria.solar.time.TimeScales;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mutation proof: the mistakes the contract guards against would be
 * caught by the same oracle (issue #399).
 *
 * <p>Each case is a rival computation - the service asked the wrong
 * way, or one of its steps left out - held to Horizons on the named
 * instants. A rival that still passed would mean the reference test
 * could not tell right from wrong; each must fail its target by a
 * margin, so the targets are known to bite.
 */
class SunMutationTest {

    private static SolarSystemService service;
    private static List<Row> named;

    @BeforeAll
    static void load() throws IOException {
        service = SolarSystemService.load();
        named = SunReferenceVectorTest.rows("named-oslo");
    }

    private static Row exactEra(int index) {
        // 2026-06-21 10:00 UTC, the sample row: exact time, high Sun.
        return named.stream().filter(r -> r.when().equals(
                Instant.parse("2026-06-21T10:00:00Z"))).findFirst().orElseThrow();
    }

    @Test
    void readingTheCivilInstantAsTerrestrialTimeFailsTheAstrometricTarget() {
        Row row = exactEra(0);
        double ttMinusUtc = 69.184; // what the rival would forget to add
        SunObservation rival = (SunObservation) service.observe(Body.SUN,
                SunReferenceVectorTest.observerAt(row)
                        .at(row.when().minusSeconds((long) ttMinusUtc)));
        double error = SunReferenceVectorTest.arcsec(rival.astrometricJ2000(),
                row.ra(), row.dec());
        assertTrue(error > 2.0, "a minute of time moves the Sun by about"
                + " 2.5 arcseconds, far past the 0.1 arcsecond target: "
                + error);
    }

    @Test
    void skippingPrecessionAndNutationFailsTheApparentTarget() {
        Row row = exactEra(0);
        SunObservation o = (SunObservation) service.observe(Body.SUN,
                SunReferenceVectorTest.observerAt(row));
        double error = SunReferenceVectorTest.arcsec(o.astrometricJ2000(),
                row.raApp(), row.decApp());
        assertTrue(error > 1000.0, "J2000 handed off as of-date is a third"
                + " of a degree wrong in 2026: " + error + "″");
    }

    @Test
    void leavingOutAberrationFailsTheApparentTarget() {
        Row row = exactEra(0);
        SunObservation o = (SunObservation) service.observe(Body.SUN,
                SunReferenceVectorTest.observerAt(row));
        TimeScales.Epoch epoch = service.timeScales().tt(row.when());
        SkyPosition unaberrated = SkyFrame.toOfDate(o.astrometricJ2000(),
                epoch.jdTt());
        double error = SunReferenceVectorTest.arcsec(unaberrated,
                row.raApp(), row.decApp());
        assertTrue(error > 15.0 && error < 25.0, "annual aberration is about"
                + " 20 arcseconds, twenty times the 1 arcsecond target: "
                + error);
    }

    @Test
    void aWestPositiveLongitudeFailsTheHorizontalTarget() {
        Row row = exactEra(0);
        Observer right = SunReferenceVectorTest.observerAt(row);
        SunObservation rival = (SunObservation) service.observe(Body.SUN,
                new Observer(right.latitudeDegrees(),
                        -right.eastLongitudeDegrees(), right.instant()));
        double error = SunReferenceVectorTest.horizontalArcsec(
                rival.horizontal(), row.az(), row.el());
        assertTrue(error > 3600.0, "Oslo mirrored to 10.75° W is over an"
                + " hour of hour angle wrong: " + error + "″");
    }

    @Test
    void aGeocentricPlaceFailsTheAstrometricTarget() {
        // The Sun's parallax is up to 8.8″; a rival that ignored the
        // station would be topocentric only in name.
        Row row = exactEra(0);
        TimeScales.Epoch epoch = service.timeScales().tt(row.when());
        SkyPosition geocentric = service.geocentricApparentOfDate(epoch.jdTt());
        SunObservation o = (SunObservation) service.observe(Body.SUN,
                SunReferenceVectorTest.observerAt(row));
        double parallax = geocentric.separationDegrees(o.apparentOfDate()) * 3600.0;
        assertTrue(parallax > 3.0 && parallax < 9.0, "the station moves the"
                + " apparent Sun by several arcseconds at this hour: " + parallax);
        double error = SunReferenceVectorTest.arcsec(geocentric,
                row.raApp(), row.decApp());
        assertTrue(error > 1.0, "and the geocentric answer misses Horizons'"
                + " topocentric one by more than the apparent target: " + error);
    }
}
