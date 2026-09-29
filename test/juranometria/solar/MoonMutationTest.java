package juranometria.solar;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.MoonReferenceVectorTest.Row;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.time.TimeScales;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mutation proof for the Moon (issue #406, M9): the mistakes the
 * contract guards against would be caught by the same oracle.
 *
 * <p>Each case is a rival computation - the service asked the wrong
 * way, or one of its steps left out - held to Horizons on the named
 * Oslo instant, 2026-06-21 10:00 UTC. A rival that still passed would
 * mean the reference test could not tell right from wrong; each must
 * fail its target by a margin, so the targets are known to bite.
 */
class MoonMutationTest {

    private static SolarSystemService service;
    private static Row row;

    @BeforeAll
    static void load() throws IOException {
        service = MoonReferenceVectorTest.service();
        row = MoonReferenceVectorTest.rows("named-oslo").stream()
                .filter(r -> r.when().equals(Instant.parse("2026-06-21T10:00:00Z")))
                .findFirst().orElseThrow();
    }

    private static MoonObservation right() {
        return MoonReferenceVectorTest.observe(row);
    }

    @Test
    void aGeocentricPlaceFailsTheAstrometricTargetByAWholeDegree() {
        // The Moon's parallax reaches a degree; the station is not optional.
        TimeScales.Epoch epoch = service.timeScales().tt(row.when());
        SkyPosition geocentric = service.geocentricMoon(epoch.jdTt()).astrometricJ2000();
        double error = MoonReferenceVectorTest.arcsec(geocentric, row.ra(), row.dec());
        assertTrue(error > 1800.0, "a geocentric Moon misses Horizons'"
                + " topocentric one by over half a degree at this hour: " + error + "″");
    }

    @Test
    void readingTheCivilInstantAsTerrestrialTimeFailsTheAstrometricTarget() {
        double ttMinusUtc = 69.184; // what the rival would forget to add
        MoonObservation rival = (MoonObservation) service.observe(Body.MOON,
                MoonReferenceVectorTest.observerAt(row)
                        .at(row.when().minusSeconds((long) ttMinusUtc)));
        double error = MoonReferenceVectorTest.arcsec(rival.astrometricJ2000(),
                row.ra(), row.dec());
        assertTrue(error > 30.0, "69 s of time moves the Moon by about"
                + " 38 arcseconds, far past the 0.1 arcsecond target: " + error);
    }

    @Test
    void leavingOutAberrationFailsTheApparentTarget() throws IOException {
        // On the sample morning the Moon lies within 5° of the anti-apex
        // of the Earth's motion, where aberration nearly vanishes (2″);
        // the first leap-second instant has it near full strength.
        Row leap = MoonReferenceVectorTest.rows("named-oslo").stream()
                .filter(r -> r.when().equals(Instant.parse("1972-01-01T00:00:00Z")))
                .findFirst().orElseThrow();
        MoonObservation o = MoonReferenceVectorTest.observe(leap);
        TimeScales.Epoch epoch = service.timeScales().tt(leap.when());
        SkyPosition unaberrated = SkyFrame.toOfDate(o.astrometricJ2000(),
                epoch.jdTt());
        double error = MoonReferenceVectorTest.arcsec(unaberrated,
                leap.raApp(), leap.decApp());
        assertTrue(error > 15.0 && error < 25.0, "aberration is about 20"
                + " arcseconds, twenty times the 1 arcsecond target: " + error);
    }

    @Test
    void aGeometricPhaseAngleFailsHorizonsDefinitionByTensOfArcseconds() throws IOException {
        // The contract's phase angle is Horizons' S-T-O, which carries the
        // Moon's own aberration on the Sun it sees and the observer's on
        // the down-leg. Horizons' geometric angle, "phi", is published
        // beside it; the two differ by up to 21″ per aberration, and a
        // rival that computed the geometric angle would miss S-T-O by
        // more than the 5″ target at most instants.
        List<Row> rows;
        try {
            rows = MoonReferenceVectorTest.rows("matrix-7d-oslo");
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        int beyondTarget = 0;
        double worst = 0.0;
        for (Row r : rows) {
            MoonObservation o = MoonReferenceVectorTest.observe(r);
            double sto = (o.phaseAngleDegrees() - r.phaseAngle()) * 3600.0;
            if (r.when().atOffset(java.time.ZoneOffset.UTC).getYear() < 2027) {
                assertTrue(Math.abs(sto) <= 5.0, "S-T-O at " + r.when() + ": " + sto);
            }
        }
        // The rival is measured through Horizons' own two columns: phi
        // against S-T-O, row by row.
        for (String line : MoonReferenceVectorTest.lines(
                MoonReferenceVectorTest.HORIZONS.resolve("matrix-7d-oslo.txt"))) {
            String[] c = line.split(",");
            double difference = Math.abs(Double.parseDouble(c[20].strip())
                    - Double.parseDouble(c[17].strip())) * 3600.0;
            worst = Math.max(worst, difference);
            if (difference > 5.0) {
                beyondTarget++;
            }
        }
        assertTrue(worst > 20.0 && worst < 45.0, "the geometric angle misses"
                + " S-T-O by up to the two aberrations: " + worst + "″");
        assertTrue(beyondTarget > rows.size() / 2, "and by more than the target"
                + " at most instants: " + beyondTarget + " of " + rows.size());
    }

    @Test
    void theBrightLimbAngleFromJ2000InputsFailsItsTarget() {
        // χ is defined from the apparent places of date (#406, M2); the
        // astrometric J2000 places give an angle up to 0.6° different
        // across the matrix, and here by more than the 0.1° target.
        List<Row> rows;
        try {
            rows = MoonReferenceVectorTest.rows("matrix-7d-oslo");
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        double worst = 0.0;
        for (Row r : rows) {
            MoonObservation o = MoonReferenceVectorTest.observe(r);
            if (o.brightLimbConditioning() != SolarSystemService.LimbConditioning.WELL_DEFINED) {
                continue;
            }
            SolarSystemService.SunObservation sun = (SolarSystemService.SunObservation)
                    service.observe(Body.SUN, MoonReferenceVectorTest.observerAt(r));
            double rival = SolarSystemService.brightLimbAngle(
                    sun.astrometricJ2000(), o.astrometricJ2000());
            worst = Math.max(worst, Math.abs(MoonReferenceVectorTest.wrapped(
                    rival - o.brightLimbAngleDegrees())));
        }
        assertTrue(worst > 0.1 && worst < 1.0, "J2000 inputs turn χ by up to "
                + worst + "°, past the 0.1° target");
    }

    @Test
    void anObserversSideIsNotTheMoonsTrend() throws IOException {
        // The waxing/waning classification is geocentric by construction;
        // the side of the Sun in one observer's sky is not, and near
        // conjunction and opposition the two differ. A rival that named
        // the phase from the side would call some rows wrong.
        int differed = 0;
        int rows = 0;
        for (String site : List.of("quito", "alert")) {
            for (Row r : MoonReferenceVectorTest.rows("matrix-7d-" + site)) {
                MoonObservation o = MoonReferenceVectorTest.observe(r);
                boolean sideSaysWaxing = o.side() == SolarSystemService.Side.EAST_OF_SUN;
                boolean waxing = o.trend() == SolarSystemService.Trend.WAXING;
                if (sideSaysWaxing != waxing) {
                    differed++;
                    assertTrue(o.elongationDegrees() < 12.0
                            || o.elongationDegrees() > 168.0, "only near"
                            + " conjunction or opposition: " + r.when() + " ψ "
                            + o.elongationDegrees());
                }
                rows++;
            }
        }
        assertTrue(differed > 0 && differed < rows / 20, "the rival would"
                + " misname " + differed + " of " + rows + " rows");
    }

    @Test
    void theSunMovesLittleAndTheMoonMuchAcrossAnObservation() {
        MoonObservation o = right();
        assertEquals(TimeScales.Confidence.EXACT, o.timeConfidence());
        assertTrue(o.distanceKm() > 356_000.0 && o.distanceKm() < 407_000.0,
                "a lunar distance: " + o.distanceKm());
        assertTrue(o.angularDiameterArcseconds() > 1740.0
                && o.angularDiameterArcseconds() < 2040.0,
                "between the year's apogee and perigee sizes");
    }
}
