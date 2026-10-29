package juranometria.solar;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.spk.Vector3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Neptune service's own invariants (issue #655, as ruled on #654): one
 * deflection formula with Jupiter's; the pole the IAU's with its periodic
 * term over N; the tilt planetocentric; the polar diameter at the tilt; the
 * moons in their ruled order; the behind-the-Sun reading; and a missing pack
 * refusing only this service.
 */
class NeptuneServiceTest {

    private static final NeptuneService SERVICE = NeptuneService.load();
    private static final Observer OSLO = new Observer(59.91, 10.75, Instant.parse("2026-10-29T12:00:00Z"));

    @Test
    void theDeflectionIsJupitersFormulaExactly() {
        JovianSystemService jovian = JovianSystemService.load();
        double et = SERVICE.timeScales().tt(OSLO.instant()).secondsPastJ2000();
        Vector3 observer = new Vector3(1.2e8, -8.1e7, -3.5e7);
        for (Vector3 range : List.of(new Vector3(-1.4e8, 2.0e7, 1.1e7),
                new Vector3(3.0e7, 9.0e7, -2.0e7), new Vector3(1.0e9, 2.5e9, 4.0e8))) {
            assertEquals(jovian.deflectedBySun(range, observer, et), SERVICE.deflectedBySun(range, observer, et),
                    "one formula, every planet's service");
        }
    }

    @Test
    void thePoleIsTheIauModelWithItsPeriodicTerm() {
        NeptuneSystemPack pack = NeptuneSystemPack.load();
        assertEquals("299.36,0.,0.", pack.manifest().get("pck.BODY899_POLE_RA"), "verbatim (N-4)");
        assertEquals("43.46,0.,0.", pack.manifest().get("pck.BODY899_POLE_DEC"), "verbatim");
        for (double jd : new double[] {2415020.5, 2461343.0, 2488434.5}) {
            double t = (jd - 2451545.0) / 36525.0;
            double n = Math.toRadians(357.85 + 52.316 * t);
            Vector3 p = SERVICE.pole(jd);
            assertEquals(299.36 + 0.70 * Math.sin(n), Math.toDegrees(Math.atan2(p.y(), p.x())) + 360.0, 1e-9,
                    "RA with 0.70 sin N at JD " + jd);
            assertEquals(43.46 - 0.51 * Math.cos(n), Math.toDegrees(Math.asin(p.z())), 1e-9,
                    "Dec with -0.51 cos N at JD " + jd);
        }
    }

    @Test
    void theTiltIsPlanetocentricAndThePolarDiameterIsTakenAtIt() {
        NeptuneService.NeptuneObservation o = SERVICE.observe(OSLO);
        NeptuneService.NeptuneObservation detic = SERVICE.observeRival(OSLO, NeptuneService.Rival.PLANETODETIC_TILT);
        assertTrue(Math.abs(detic.tiltDegrees()) > Math.abs(o.tiltDegrees()),
                "the planetodetic reading lies farther from the equator than the planetocentric");
        double re = 24764.0;
        double rp = 24341.0;
        double phi = Math.toRadians(o.tiltDegrees());
        double b = Math.sqrt(rp * rp * Math.cos(phi) * Math.cos(phi) + re * re * Math.sin(phi) * Math.sin(phi));
        assertEquals(2.0 * Math.toDegrees(Math.asin(b / o.distanceKm())) * 3600.0,
                o.polarDiameterArcseconds(), 1e-9);
        assertTrue(o.polarDiameterArcseconds() < o.equatorialDiameterArcseconds());
        assertEquals(o.tiltDegrees() >= 0.0, o.northPoleTowardObserver());
        assertTrue(o.illuminatedFraction() > 0.9997, "Neptune is never less than 99.971 % lit");
    }

    @Test
    void theMoonsComeInTheRuledOrderAndAnswerWithNeptune() {
        NeptuneService.Configuration c = SERVICE.observeMoons(OSLO);
        assertEquals(List.of(NeptuneService.Moon.TRITON, NeptuneService.Moon.NEREID),
                c.moons().stream().map(NeptuneService.MoonPlace::moon).toList());
        assertEquals(SERVICE.observe(OSLO).astrometricJ2000(), c.neptune().astrometricJ2000(),
                "the configuration's Neptune is the Neptune the planet's card shows");
        // #654's measured extremes: Triton within 16.97″, Nereid within 457.71″.
        assertTrue(c.moon(NeptuneService.Moon.TRITON).separationArcseconds() < 17.0, "Triton lies near Neptune");
        assertTrue(c.moon(NeptuneService.Moon.NEREID).separationArcseconds() < 460.0, "Nereid within 7.7′");
        for (NeptuneService.MoonPlace p : c.moons()) {
            assertTrue(p.angularDiameterArcseconds() < 0.13, p.moon() + " is never a disc");
            assertEquals(Math.toDegrees(Math.atan2(p.xArcseconds(), p.yArcseconds())),
                    p.positionAngleDegrees() > 180.0 ? p.positionAngleDegrees() - 360.0 : p.positionAngleDegrees(),
                    1e-9, p.moon() + "'s position angle is derived from its offsets (N-10)");
        }
    }

    @Test
    void behindTheSunIsTheDiscsMeetingTheSuns() {
        // The 2026 conjunction, Espenak's hour (neptune-events.txt): Neptune
        // passes the Sun a little over a degree from its centre, outside its
        // 0.27° disc, so the reading must be false both there and in October.
        NeptuneService.NeptuneObservation conjunction =
                SERVICE.observeGeocentric(Instant.parse("2026-03-22T10:00:00Z"));
        assertTrue(conjunction.elongationDegrees() < 2.0, "near the Sun at the conjunction");
        assertFalse(conjunction.behindSun(), "never behind the Sun in 2026");
        assertEquals(conjunction.elongationDegrees() * 3600.0 < conjunction.sunAngularRadiusArcseconds()
                + conjunction.equatorialDiameterArcseconds() / 2.0, conjunction.behindSun());
        assertFalse(SERVICE.observe(OSLO).behindSun(), "far from the Sun in October");
    }

    @Test
    void aMissingPackRefusesTheService() {
        assertThrows(RuntimeException.class, () -> new NeptuneService(SolarSystemPack.load(),
                NeptuneSystemPack.load(name -> null)));
        assertThrows(IllegalArgumentException.class, () -> new NeptuneService(SolarSystemPack.load(), null));
    }
}
