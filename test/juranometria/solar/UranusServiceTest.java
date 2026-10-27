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
 * The Uranus service's own invariants (issue #634, as ruled on #633): one
 * deflection formula with Jupiter's; the pole the IAU's with no periodic
 * terms; the tilt planetocentric; the polar diameter at the tilt; the moons
 * in their ruled order; the behind-the-Sun reading; and a missing pack
 * refusing only this service.
 */
class UranusServiceTest {

    private static final UranusService SERVICE = UranusService.load();
    private static final Observer OSLO = new Observer(59.91, 10.75, Instant.parse("2026-10-27T12:00:00Z"));

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
    void thePoleIsTheIauModelWithNoPeriodicTerms() {
        UranusSystemPack pack = UranusSystemPack.load();
        assertEquals("257.311,0.,0.", pack.manifest().get("pck.BODY799_POLE_RA"), "verbatim (U-4)");
        assertEquals("-15.175,0.,0.", pack.manifest().get("pck.BODY799_POLE_DEC"), "verbatim");
        assertFalse(pack.manifest().containsKey("pck.BODY799_NUT_PREC_RA"), "no periodic terms");
        Vector3 p = SERVICE.pole(2461339.0);
        assertEquals(257.311, Math.toDegrees(Math.atan2(p.y(), p.x())) + 360.0, 1e-9);
        assertEquals(-15.175, Math.toDegrees(Math.asin(p.z())), 1e-9);
    }

    @Test
    void theTiltIsPlanetocentricAndThePolarDiameterIsTakenAtIt() {
        UranusService.UranusObservation o = SERVICE.observe(OSLO);
        UranusService.UranusObservation detic = SERVICE.observeRival(OSLO, UranusService.Rival.PLANETODETIC_TILT);
        assertTrue(Math.abs(detic.tiltDegrees()) > Math.abs(o.tiltDegrees()),
                "the planetodetic reading lies farther from the equator than the planetocentric");
        double re = 25559.0;
        double rp = 24973.0;
        double phi = Math.toRadians(o.tiltDegrees());
        double b = Math.sqrt(rp * rp * Math.cos(phi) * Math.cos(phi) + re * re * Math.sin(phi) * Math.sin(phi));
        assertEquals(2.0 * Math.toDegrees(Math.asin(b / o.distanceKm())) * 3600.0,
                o.polarDiameterArcseconds(), 1e-9);
        assertTrue(o.polarDiameterArcseconds() < o.equatorialDiameterArcseconds());
        assertEquals(o.tiltDegrees() >= 0.0, o.northPoleTowardObserver());
        assertTrue(o.illuminatedFraction() > 0.999, "Uranus is never less than 99.92 % lit");
    }

    @Test
    void theMoonsComeInTheRuledOrderAndAnswerWithUranus() {
        UranusService.Configuration c = SERVICE.observeMoons(OSLO);
        assertEquals(List.of(UranusService.Moon.MIRANDA, UranusService.Moon.ARIEL, UranusService.Moon.UMBRIEL,
                        UranusService.Moon.TITANIA, UranusService.Moon.OBERON),
                c.moons().stream().map(UranusService.MoonPlace::moon).toList());
        assertEquals(SERVICE.observe(OSLO).astrometricJ2000(), c.uranus().astrometricJ2000(),
                "the configuration's Uranus is the Uranus the planet's card shows");
        for (UranusService.MoonPlace p : c.moons()) {
            assertTrue(p.separationArcseconds() < 50.0, p.moon() + " lies near Uranus");
            assertTrue(p.angularDiameterArcseconds() < 0.13, p.moon() + " is never a disc");
        }
    }

    @Test
    void behindTheSunIsTheDiscsMeetingTheSuns() {
        // The 2026 conjunction, Espenak's hour (uranus-events.txt): about 0.4°
        // from the Sun's centre, outside its 0.26° disc; a day of motion moves
        // it 1° along, so the reading must be false both there and at quadrature.
        UranusService.UranusObservation conjunction =
                SERVICE.observeGeocentric(Instant.parse("2026-05-22T16:00:00Z"));
        assertTrue(conjunction.elongationDegrees() < 1.0, "near the Sun at the conjunction");
        assertEquals(conjunction.elongationDegrees() * 3600.0 < conjunction.sunAngularRadiusArcseconds()
                + conjunction.equatorialDiameterArcseconds() / 2.0, conjunction.behindSun());
        assertFalse(SERVICE.observe(OSLO).behindSun(), "far from the Sun in October");
    }

    @Test
    void aMissingPackRefusesTheService() {
        assertThrows(RuntimeException.class, () -> new UranusService(SolarSystemPack.load(),
                UranusSystemPack.load(name -> null)));
        assertThrows(IllegalArgumentException.class, () -> new UranusService(SolarSystemPack.load(), null));
    }
}
