package juranometria.solar;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService.Configuration;
import juranometria.solar.SaturnSystemService.DiscRelation;
import juranometria.solar.SaturnSystemService.Moon;
import juranometria.solar.SaturnSystemService.MoonPlace;
import juranometria.solar.SaturnSystemService.SaturnObservation;
import juranometria.solar.SaturnSystemService.ShadowRelation;
import juranometria.solar.SaturnSystemService.VisibilityState;
import juranometria.solar.spk.Vector3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Saturn service's own invariants (issue #575), under the contract
 * ruled on #574: one deflection formula with Jupiter's; the ring
 * geometry's definitions - B and B′ planetocentric, the unlit face
 * exactly when they differ in sign, the ellipse's minor axis the major
 * times |sin B|, the polar diameter the projected figure's at B; the
 * states composed in Jupiter's precedence; the order of the seven
 * moons; and SAT441's two segments meeting without a seam at
 * 2014-09-28.
 */
class SaturnServiceTest {

    private static final Observer OSLO = new Observer(59.91, 10.75, Instant.parse("2026-10-05T00:00:00Z"));
    private static SaturnSystemService service;

    @BeforeAll
    static void load() {
        service = SaturnSystemService.load();
    }

    @Test
    void theGeocentricIntermediateHasNoHorizonAndTheObserversHasOne() {
        assertNull(service.observeSaturnGeocentric(OSLO.instant()).horizontal());
        assertNotNull(service.observeSaturn(OSLO).horizontal());
    }

    @Test
    void theSameAnswerForTheSameObserverAlways() {
        assertEquals(service.observeSaturn(OSLO), service.observeSaturn(OSLO));
        assertEquals(service.observeMoons(OSLO), SaturnSystemService.load().observeMoons(OSLO));
    }

    @Test
    void theDeflectionIsJupitersFormulaExactly() {
        JovianSystemService jovian = JovianSystemService.load();
        double et = service.timeScales().tt(OSLO.instant()).secondsPastJ2000();
        Vector3 observer = new Vector3(1.2e8, -8.1e7, -3.5e7);
        for (Vector3 range : List.of(new Vector3(-1.4e8, 2.0e7, 1.1e7),
                new Vector3(3.0e7, 9.0e7, -2.0e7), new Vector3(1.0e9, 1.0e9, 4.0e8))) {
            assertEquals(jovian.deflectedBySun(range, observer, et), service.deflectedBySun(range, observer, et),
                    "one formula, three services");
        }
    }

    @Test
    void theRingGeometryIsTheRulingsDefinitions() {
        SaturnPack.Constants k = service.pack().constants();
        for (String when : List.of("2003-04-07T00:00:00Z", "2025-03-23T12:00:00Z", "2025-04-10T00:00:00Z",
                "2026-10-05T00:00:00Z", "2032-06-01T00:00:00Z")) {
            SaturnObservation o = service.observeSaturn(OSLO.at(Instant.parse(when)));
            // The unlit face, exactly when B and B′ differ in sign.
            assertEquals(Math.signum(o.ringOpeningDegrees()) * Math.signum(o.sunRingOpeningDegrees()) < 0.0,
                    o.unlitFaceSeen(), when);
            // An edge's minor axis is its major times |sin B|, and its major asin(R/Δ).
            double outer = k.ringOuterRadiusKm();
            assertEquals(Math.toDegrees(Math.asin(outer / o.distanceKm())) * 3600.0,
                    o.ringSemiMajorArcseconds(outer), 1e-12);
            assertEquals(o.ringSemiMajorArcseconds(outer) * Math.abs(Math.sin(Math.toRadians(o.ringOpeningDegrees()))),
                    o.ringSemiMinorArcseconds(outer), 1e-12);
            // The polar diameter is the projected figure's at B: between the
            // polar radius's (B = 0) and the equatorial's (B = 90°).
            double polarAtZero = 2.0 * Math.toDegrees(Math.asin(k.saturnPolarRadiusKm() / o.distanceKm())) * 3600.0;
            assertTrue(o.polarDiameterArcseconds() >= polarAtZero - 1e-9
                    && o.polarDiameterArcseconds() <= o.equatorialDiameterArcseconds() + 1e-9, when);
            assertEquals(SkyFrameNormalise.of(o.poleAngleDegrees() + 90.0), o.ringMajorAxisPositionAngleDegrees(), 1e-12);
            assertEquals(List.of(k.ringInnerRadiusKm(), k.ringBOuterRadiusKm(), k.ringCassiniOuterRadiusKm(),
                    k.ringOuterRadiusKm()), o.ringRadiiKm());
        }
        // The widest opening #574 measured, 2003-04-07: about 27 degrees
        // planetocentric, the south face - never Horizons' planetodetic 32.
        double b = service.observeSaturn(OSLO.at(Instant.parse("2003-04-07T00:00:00Z"))).ringOpeningDegrees();
        assertTrue(b < -26.9 && b > -27.1, "the true geometric opening, not the planetodetic latitude: " + b);
    }

    @Test
    void theStatesAreComposedInJupitersPrecedence() {
        assertEquals(VisibilityState.BEHIND_SATURN_IN_ITS_SHADOW,
                VisibilityState.of(DiscRelation.BEHIND, ShadowRelation.IN_SHADOW));
        assertEquals(VisibilityState.BEHIND_SATURN_IN_ITS_SHADOW,
                VisibilityState.of(DiscRelation.BEHIND, ShadowRelation.PARTLY_IN_SHADOW));
        assertEquals(VisibilityState.BEHIND_SATURN, VisibilityState.of(DiscRelation.BEHIND, ShadowRelation.SUNLIT));
        assertEquals(VisibilityState.IN_FRONT_OF_SATURN,
                VisibilityState.of(DiscRelation.IN_FRONT, ShadowRelation.IN_SHADOW));
        assertEquals(VisibilityState.IN_SATURNS_SHADOW, VisibilityState.of(DiscRelation.CLEAR, ShadowRelation.IN_SHADOW));
        assertEquals(VisibilityState.PARTLY_IN_SATURNS_SHADOW,
                VisibilityState.of(DiscRelation.CLEAR, ShadowRelation.PARTLY_IN_SHADOW));
        assertEquals(VisibilityState.CLEAR_OF_SATURN, VisibilityState.of(DiscRelation.CLEAR, ShadowRelation.SUNLIT));
    }

    @Test
    void theSevenMoonsComeOutwardFromSaturnAndTheirStatesAreTheirRelations() {
        Configuration c = service.observeMoons(OSLO);
        assertEquals(List.of(Moon.MIMAS, Moon.ENCELADUS, Moon.TETHYS, Moon.DIONE, Moon.RHEA, Moon.TITAN,
                Moon.IAPETUS), c.moons().stream().map(MoonPlace::moon).toList());
        for (MoonPlace p : c.moons()) {
            assertEquals(VisibilityState.of(p.discRelation(), p.shadowRelation()), p.state());
            // X and Y are flat offsets; the separation is the true angle,
            // the same to second order (0.006″ at Iapetus's 578″)
            assertEquals(Math.hypot(p.xArcseconds(), p.yArcseconds()), p.separationArcseconds(), 0.01,
                    p.moon() + ": the separation is X and Y's, to second order");
            if (p.discRelation() == DiscRelation.IN_FRONT) {
                assertTrue(p.depthKm() < 0.0, p.moon() + " in front is nearer than Saturn");
            }
            if (p.discRelation() == DiscRelation.BEHIND) {
                assertTrue(p.depthKm() > 0.0, p.moon() + " behind is farther than Saturn");
            }
        }
        assertEquals(VisibilityState.BEHIND_SATURN_IN_ITS_SHADOW, c.moon(Moon.TETHYS).state(),
                "the card's instant: Tethys behind Saturn in its shadow");
    }

    @Test
    void sat441sTwoSegmentsMeetWithoutASeam() {
        double split = SaturnPackTest.SPLIT_ET;
        Configuration before = service.configurationAtEt(split - 0.001);
        Configuration after = service.configurationAtEt(split + 0.001);
        for (Moon moon : Moon.values()) {
            double moved = before.moon(moon).separationArcseconds() - after.moon(moon).separationArcseconds();
            // two milliseconds of real motion move Mimas by a few
            // millionths of an arcsecond; a seam would show in kilometres
            assertTrue(Math.abs(moved) < 1e-3, moon + " across the split: " + moved + "″");
        }
        assertTrue(Math.abs(before.saturn().ringOpeningDegrees() - after.saturn().ringOpeningDegrees()) < 1e-9);
    }

    /** SkyFrame.normalise, named for the test. */
    private static final class SkyFrameNormalise {
        static double of(double degrees) {
            return juranometria.sky.SkyFrame.normalise(degrees);
        }
    }
}
