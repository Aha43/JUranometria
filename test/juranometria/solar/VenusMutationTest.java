package juranometria.solar;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.solar.VenusService.Rival;
import juranometria.solar.VenusService.VenusObservation;
import juranometria.tool.VenusComparison;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mutation proof for Venus (issue #501): the mistakes the contract
 * guards against would be caught by the same oracle. Each case is a
 * rival - the service asked the wrong way, or a step left out - held
 * to Horizons' own row at a named Oslo instant; each must fail the
 * target ruled on #500 by a margin, so the targets are known to bite.
 */
class VenusMutationTest {

    private static final double LAT = 59.91;
    private static final double LON = 10.75;
    private static VenusService service;

    @BeforeAll
    static void load() {
        service = VenusService.load();
    }

    /** Horizons' Oslo row at an instant, from the committed fixtures. */
    private static String[] row(String stem, String instant) throws IOException {
        for (String[] f : VenusComparison.rows(stem)) {
            if (f[0].startsWith(instant)) {
                return f;
            }
        }
        throw new IllegalStateException(stem + " has no row at " + instant);
    }

    private static double sep(SkyPosition p, String ra, String dec) {
        return p.separationDegrees(new SkyPosition(Double.parseDouble(ra),
                Double.parseDouble(dec))) * 3600.0;
    }

    private static Observer oslo(String instant) {
        return new Observer(LAT, LON, Instant.parse(instant));
    }

    @Test
    void aGeocentricPlaceFailsTheAstrometricTargetNearInferiorConjunction() throws IOException {
        String[] f = row("dense-2026-inferior-conjunction-oslo-299", "2026-Oct-25 07:00");
        VenusObservation geo = service.observeGeocentric(Instant.parse("2026-10-25T07:00:00Z"));
        double error = sep(geo.astrometricJ2000(), f[3], f[4]);
        assertTrue(error > 20.0, "Venus's parallax is half an arcminute here; a geocentric"
                + " place misses the 0.05″ target by far: " + error + "″");
    }

    @Test
    void readingTheCivilInstantAsTerrestrialTimeFailsTheAstrometricTarget() throws IOException {
        String[] f = row("named-oslo-299", "2026-Jan-06 16:00");
        // The rival forgets TT - UTC, 69.184 s in 2026: it computes the instant that much early.
        VenusObservation rival = service.observe(oslo("2026-01-06T16:00:00Z").at(
                Instant.parse("2026-01-06T16:00:00Z").minusMillis(69_184)));
        double error = sep(rival.astrometricJ2000(), f[3], f[4]);
        assertTrue(error > 2.0, "at superior conjunction Venus moves 0.05″ a second; 69 s"
                + " is far past the 0.05″ target: " + error + "″");
    }

    @Test
    void leavingOutTheSunsDeflectionFailsTheApparentTargetNearTheSun() throws IOException {
        String[] f = row("named-oslo-299", "2026-Jan-06 16:00");
        assertTrue(Double.parseDouble(f[17]) > 0.5, "0.7° from the Sun, outside its disc");
        VenusObservation right = service.observe(oslo("2026-01-06T16:00:00Z"));
        VenusObservation rival = service.observeRival(oslo("2026-01-06T16:00:00Z"),
                Rival.NO_DEFLECTION);
        double rightError = sep(right.apparentOfDate(), f[5], f[6]);
        double rivalError = sep(rival.apparentOfDate(), f[5], f[6]);
        assertTrue(rightError <= 0.2 && rivalError > 0.2,
                "against the ruled 0.2″ the deflection is the difference near the Sun: "
                        + rightError + "″ with it, " + rivalError + "″ without");
    }

    @Test
    void leavingOutAberrationFailsTheApparentTarget() throws IOException {
        String[] f = row("named-oslo-299", "2026-Aug-15 06:00");
        VenusObservation rival = service.observeRival(oslo("2026-08-15T06:00:00Z"),
                Rival.NO_ABERRATION);
        double error = sep(rival.apparentOfDate(), f[5], f[6]);
        assertTrue(error > 5.0, "aberration is about 20″: " + error + "″");
    }

    @Test
    void theGeometricPhaseAngleFailsTheStoTarget() throws IOException {
        String[] f = row("named-oslo-299", "2026-Aug-15 06:00");
        VenusObservation rival = service.observeRival(oslo("2026-08-15T06:00:00Z"),
                Rival.GEOMETRIC_PHASE_ANGLE);
        double error = Math.abs(rival.phaseAngleDegrees() - Double.parseDouble(f[19])) * 3600.0;
        assertTrue(error > 2.0, "S-T-O carries two aberrations, up to 24″ between them: "
                + error + "″ against the 1″ target");
    }

    @Test
    void aJ2000BrightLimbFailsTheChiTarget() throws IOException {
        String[] v = row("named-oslo-299", "2100-Mar-02 06:00");
        String[] s = row("named-oslo-10", "2100-Mar-02 06:00");
        double ofDate = VenusComparison.brightLimb(Double.parseDouble(s[5]),
                Double.parseDouble(s[6]), Double.parseDouble(v[5]), Double.parseDouble(v[6]));
        double j2000 = VenusComparison.brightLimb(Double.parseDouble(s[3]),
                Double.parseDouble(s[4]), Double.parseDouble(v[3]), Double.parseDouble(v[4]));
        double error = Math.abs(((j2000 - ofDate + 540.0) % 360.0) - 180.0) * 3600.0;
        assertTrue(error > 60.0, "a century of precession turns χ: " + error
                + "″ against the 1″ target - the input frame is part of the definition");
    }

    @Test
    void theSideByEclipticLongitudeDisagreesWithHorizonsBeforeTheConjunction() throws IOException {
        // 2026-10-23 03:00: Horizons has Venus west of the Sun (/L) by right
        // ascension, a day before the conjunction in ecliptic longitude.
        String[] f = row("dense-2026-inferior-conjunction-oslo-299", "2026-Oct-23 03:00");
        assertTrue(f[18].contains("L"));
        VenusObservation o = service.observe(oslo("2026-10-23T03:00:00Z"));
        assertTrue(o.side() == SolarSystemService.Side.WEST_OF_SUN, "the ruled side agrees");
        double et = service.timeScales().tt(o.instant()).secondsPastJ2000();
        assertTrue(service.geocentricLongitudeFromSunAtEt(et) > 0.0,
                "by ecliptic longitude Venus is still east: the rival would disagree");
    }

    @Test
    void theCloudTopRadiusFailsTheDiameterTarget() throws IOException {
        String[] f = row("named-oslo-299", "2026-Oct-24 03:00");
        VenusObservation o = service.observe(oslo("2026-10-24T03:00:00Z"));
        double cloud = 2.0 * Math.toDegrees(Math.asin((6051.8 + 74.0) / o.distanceKm())) * 3600.0;
        assertTrue(Math.abs(cloud - Double.parseDouble(f[12])) > 0.5,
                "the cloud deck is 1.22 % wider: far past the 0.001″ target");
        assertTrue(Math.abs(o.solidBodyDiameterArcseconds() - Double.parseDouble(f[12])) < 0.001);
    }
}
