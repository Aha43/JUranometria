package juranometria.solar;

import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.solar.MercuryService.MercuryObservation;
import juranometria.solar.MercuryService.Rival;
import juranometria.tool.MercuryComparison;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mutation proof for Mercury (issue #529): the mistakes the contract
 * guards against would be caught by the same oracle. Each case is a
 * rival - the service asked the wrong way, or a step left out - held
 * to Horizons' own row at a named Oslo instant; each must fail the
 * target ruled on #528 by a margin, so the targets are known to bite.
 * One step is measured rather than claimed: the Sun's deflection,
 * which no committed Mercury row brings to the 0.2″ target (below).
 */
class MercuryMutationTest {

    private static final double LAT = 59.91;
    private static final double LON = 10.75;
    private static MercuryService service;

    @BeforeAll
    static void load() {
        service = MercuryService.load();
    }

    /** Horizons' Oslo row at an instant, from the committed fixtures. */
    private static String[] row(String stem, String instant) throws IOException {
        for (String[] f : MercuryComparison.rows(stem)) {
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
        String[] f = row("dense-2026-inferior-conjunction-1-oslo-199", "2026-Mar-07 11:00");
        MercuryObservation geo = service.observeGeocentric(Instant.parse("2026-03-07T11:00:00Z"));
        double error = sep(geo.astrometricJ2000(), f[3], f[4]);
        assertTrue(error > 10.0, "Mercury's parallax is a dozen arcseconds here; a geocentric"
                + " place misses the 0.05″ target by far: " + error + "″");
    }

    @Test
    void readingTheCivilInstantAsTerrestrialTimeFailsTheAstrometricTarget() throws IOException {
        String[] f = row("named-oslo-199", "2026-Jan-21 16:00");
        // The rival forgets TT - UTC, 69.184 s in 2026: it computes the instant that much early.
        MercuryObservation rival = service.observe(oslo("2026-01-21T16:00:00Z").at(
                Instant.parse("2026-01-21T16:00:00Z").minusMillis(69_184)));
        double error = sep(rival.astrometricJ2000(), f[3], f[4]);
        assertTrue(error > 2.0, "at superior conjunction Mercury moves 0.07″ a second; 69 s"
                + " is far past the 0.05″ target: " + error + "″");
    }

    @Test
    void theSunsDeflectionIsAppliedAndMovesThePlaceTowardHorizons() throws IOException {
        // Measured, not claimed: Mercury's committed rows never come within
        // 1.6° of the Sun's centre outside a transit (named-oslo-199, the
        // superior conjunctions), where the one-body deflection is a few
        // hundredths of an arcsecond - so the rival without it survives the
        // ruled 0.2″ target on every committed row, unlike Venus's at 0.7°.
        // The step is ruled (#528, ruling 3) and applied; this holds that it
        // is, and that it brings the place closer to Horizons' own.
        String[] f = row("named-oslo-199", "2012-Feb-07 09:00");
        assertTrue(Double.parseDouble(f[17]) > 2.0 && Double.parseDouble(f[11]) > 99.0,
                "2.07° from the Sun, beyond it");
        MercuryObservation right = service.observe(oslo("2012-02-07T09:00:00Z"));
        MercuryObservation rival = service.observeRival(oslo("2012-02-07T09:00:00Z"),
                Rival.NO_DEFLECTION);
        double rightError = sep(right.apparentOfDate(), f[5], f[6]);
        double rivalError = sep(rival.apparentOfDate(), f[5], f[6]);
        assertTrue(rightError <= 0.2 && rightError < rivalError && rivalError - rightError > 0.02,
                "the deflection is applied and improves the agreement: " + rightError
                        + "″ with it, " + rivalError + "″ without");
        assertTrue(rivalError <= 0.2, "stated, not claimed: the rival survives the 0.2″ target"
                + " on this, the closest committed row outside a transit: " + rivalError + "″");
    }

    @Test
    void leavingOutAberrationFailsTheApparentTarget() throws IOException {
        String[] f = row("named-oslo-199", "1972-Jan-01 00:00");
        MercuryObservation rival = service.observeRival(oslo("1972-01-01T00:00:00Z"),
                Rival.NO_ABERRATION);
        double error = sep(rival.apparentOfDate(), f[5], f[6]);
        assertTrue(error > 5.0, "aberration is about 20″: " + error + "″");
    }

    @Test
    void theGeometricPhaseAngleFailsTheStoTarget() throws IOException {
        String[] f = row("named-oslo-199", "1972-Jan-01 00:00");
        MercuryObservation rival = service.observeRival(oslo("1972-01-01T00:00:00Z"),
                Rival.GEOMETRIC_PHASE_ANGLE);
        double error = Math.abs(rival.phaseAngleDegrees() - Double.parseDouble(f[19])) * 3600.0;
        assertTrue(error > 2.0, "S-T-O carries two aberrations, Mercury's at up to 49 km/s;"
                + " they part by a dozen arcseconds here: " + error + "″ against the 1″ target");
    }

    @Test
    void aJ2000BrightLimbFailsTheChiTarget() throws IOException {
        String[] m = row("named-oslo-199", "2100-Dec-31 23:59");
        String[] s = row("named-oslo-10", "2100-Dec-31 23:59");
        double ofDate = MercuryComparison.brightLimb(Double.parseDouble(s[5]),
                Double.parseDouble(s[6]), Double.parseDouble(m[5]), Double.parseDouble(m[6]));
        double j2000 = MercuryComparison.brightLimb(Double.parseDouble(s[3]),
                Double.parseDouble(s[4]), Double.parseDouble(m[3]), Double.parseDouble(m[4]));
        double error = Math.abs(((j2000 - ofDate + 540.0) % 360.0) - 180.0) * 3600.0;
        assertTrue(error > 60.0, "a century of precession turns χ: " + error
                + "″ against the 1″ target - the input frame is part of the definition");
    }

    @Test
    void theSideByEclipticLongitudeDisagreesWithHorizonsBeforeTheConjunction() throws IOException {
        // 2026-03-06 20:00: Horizons has Mercury west of the Sun (/L) by right
        // ascension, fifteen hours before the conjunction in ecliptic
        // longitude - Mercury's latitude is large at this one.
        String[] f = row("dense-2026-inferior-conjunction-1-oslo-199", "2026-Mar-06 20:00");
        assertTrue(f[18].contains("L"));
        MercuryObservation o = service.observe(oslo("2026-03-06T20:00:00Z"));
        assertTrue(o.side() == SolarSystemService.Side.WEST_OF_SUN, "the ruled side agrees");
        double et = service.timeScales().tt(o.instant()).secondsPastJ2000();
        assertTrue(service.geocentricLongitudeFromSunAtEt(et) > 0.0,
                "by ecliptic longitude Mercury is still east: the rival would disagree");
    }

    @Test
    void theMeanRadiusFailsTheDiameterTarget() throws IOException {
        // #528, ruling 4: the equatorial 2440.53 km, the mean never
        // substituted - docs/studies/mercury/authority-comparison.md, "The
        // radius", measures the mean at 2439.4 km.
        String[] f = row("named-oslo-199", "2026-Mar-07 11:00");
        MercuryObservation o = service.observe(oslo("2026-03-07T11:00:00Z"));
        double mean = 2.0 * Math.toDegrees(Math.asin(2439.4 / o.distanceKm())) * 3600.0;
        assertTrue(Math.abs(mean - Double.parseDouble(f[12])) > 0.001,
                "the mean radius is 0.05 % smaller: past the 0.001″ target near inferior"
                        + " conjunction, where the disc is widest");
        assertTrue(Math.abs(o.solidBodyDiameterArcseconds() - Double.parseDouble(f[12])) < 0.001);
    }
}
