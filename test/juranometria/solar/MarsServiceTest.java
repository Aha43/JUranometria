package juranometria.solar;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.spk.Vector3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Mars service's own invariants (issue #602, as ruled on #601): one
 * deflection formula with Jupiter's; the pole with its periodic terms, a
 * pole 1.5° from the secular model's; the tilt planetocentric; the
 * polar diameter at the tilt; the moons in their ruled order; and a
 * missing pack refusing only this service.
 */
class MarsServiceTest {

    private static final MarsService SERVICE = MarsService.load();
    private static final Observer OSLO = new Observer(59.91, 10.75, Instant.parse("2026-10-25T12:00:00Z"));

    @Test
    void theDeflectionIsJupitersFormulaExactly() {
        JovianSystemService jovian = JovianSystemService.load();
        double et = SERVICE.timeScales().tt(OSLO.instant()).secondsPastJ2000();
        Vector3 observer = new Vector3(1.2e8, -8.1e7, -3.5e7);
        for (Vector3 range : List.of(new Vector3(-1.4e8, 2.0e7, 1.1e7),
                new Vector3(3.0e7, 9.0e7, -2.0e7), new Vector3(1.0e9, 1.0e9, 4.0e8))) {
            assertEquals(jovian.deflectedBySun(range, observer, et), SERVICE.deflectedBySun(range, observer, et),
                    "one formula, every planet's service");
        }
    }

    @Test
    void thePolesPeriodicTermsAreApplied() {
        // docs/decisions/mars-system.md, "The pole needs its periodic terms":
        // without them the pole stands about 1.5° from Horizons'.
        double jd = 2461339.0;
        Vector3 full = SERVICE.pole(jd, MarsService.Rival.NONE);
        Vector3 secular = SERVICE.pole(jd, MarsService.Rival.SECULAR_POLE);
        double degrees = Math.toDegrees(Math.acos(full.dot(secular)));
        assertTrue(degrees > 1.4 && degrees < 1.7, "the periodic terms move the pole " + degrees + "°");
    }

    @Test
    void theTiltIsPlanetocentricAndThePolarDiameterIsTakenAtIt() {
        MarsService.MarsObservation o = SERVICE.observe(OSLO);
        MarsService.MarsObservation detic = SERVICE.observeRival(OSLO, MarsService.Rival.PLANETODETIC_TILT);
        assertTrue(Math.abs(detic.tiltDegrees()) > Math.abs(o.tiltDegrees()),
                "the planetodetic reading lies farther from the equator than the planetocentric");
        double re = 3396.19;
        double rp = 3376.20;
        double phi = Math.toRadians(o.tiltDegrees());
        double b = Math.sqrt(rp * rp * Math.cos(phi) * Math.cos(phi) + re * re * Math.sin(phi) * Math.sin(phi));
        assertEquals(2.0 * Math.toDegrees(Math.asin(b / o.distanceKm())) * 3600.0,
                o.polarDiameterArcseconds(), 1e-9);
        assertTrue(o.polarDiameterArcseconds() < o.equatorialDiameterArcseconds());
        assertEquals(o.tiltDegrees() >= 0.0, o.northPoleTowardObserver());
    }

    @Test
    void theMoonsComeInTheRuledOrderAndAnswerWithMars() {
        MarsService.Configuration c = SERVICE.observeMoons(OSLO);
        assertEquals(List.of(MarsService.Moon.PHOBOS, MarsService.Moon.DEIMOS),
                c.moons().stream().map(MarsService.MoonPlace::moon).toList());
        assertEquals(SERVICE.observe(OSLO).astrometricJ2000(), c.mars().astrometricJ2000(),
                "the configuration's Mars is the Mars the planet's card shows");
        for (MarsService.MoonPlace p : c.moons()) {
            assertTrue(p.separationArcseconds() < 90.0, p.moon() + " lies near Mars");
            assertTrue(p.angularDiameterArcseconds() < 0.1, p.moon() + " is never a disc");
        }
    }

    @Test
    void aMissingMoonsPackRefusesTheService() {
        assertThrows(RuntimeException.class, () -> new MarsService(SolarSystemPack.load(), PlanetsPack.load(),
                MarsSystemPack.load(name -> null)));
        assertThrows(IllegalArgumentException.class, () -> new MarsService(SolarSystemPack.load(),
                PlanetsPack.load(), null));
    }
}
