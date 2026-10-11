package juranometria.solar;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.VenusService.LimbConditioning;
import juranometria.solar.VenusService.Phase;
import juranometria.solar.VenusService.VenusObservation;
import juranometria.solar.spk.Vector3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Venus service's shape (issue #501): the interval it answers and
 * refuses by name, the phase words and their precedence, the bright
 * limb's conditioning, the range, the geocentric intermediate, and the
 * one formula it shares with Jupiter's service held equal to it.
 */
class VenusServiceTest {

    private static VenusService service;
    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-08-15T06:00:00Z"));

    @BeforeAll
    static void load() {
        service = VenusService.load();
    }

    @Test
    void theIntervalIsThePacksAndOutsideItIsRefusedByName() {
        assertEquals(java.time.LocalDate.of(1900, 1, 1), service.firstDay());
        assertEquals(java.time.LocalDate.of(2100, 12, 31), service.lastDay());
        assertNotNull(service.observe(OSLO.at(Instant.parse("1900-01-01T00:00:00Z"))));
        assertNotNull(service.observe(OSLO.at(Instant.parse("2100-12-31T23:59:59Z"))));
        for (String outside : new String[] {"1899-12-31T23:59:59Z", "2101-01-01T00:00:00Z"}) {
            IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                    () -> service.observe(OSLO.at(Instant.parse(outside))));
            assertTrue(refused.getMessage().contains("Venus is computed for civil dates from"
                    + " 1900-01-01 to 2100-12-31"), refused.getMessage());
            assertThrows(IllegalArgumentException.class,
                    () -> service.observeGeocentric(Instant.parse(outside)));
        }
    }

    @Test
    void theGeocentricIntermediateHasNoHorizonAndTheObserversHasOne() {
        assertNull(service.observeGeocentric(OSLO.instant()).horizontal());
        assertNotNull(service.observe(OSLO).horizontal());
    }

    @Test
    void theSameAnswerForTheSameObserverAlways() {
        assertEquals(service.observe(OSLO), service.observe(OSLO));
        assertEquals(service.observe(OSLO), VenusService.load().observe(OSLO));
    }

    @Test
    void theDeflectionIsJupitersFormulaExactly() {
        JovianSystemService jovian = JovianSystemService.load();
        double et = service.timeScales().tt(OSLO.instant()).secondsPastJ2000();
        Vector3 observer = new Vector3(1.2e8, -8.1e7, -3.5e7);
        for (Vector3 range : List.of(new Vector3(-1.4e8, 2.0e7, 1.1e7),
                new Vector3(3.0e7, 9.0e7, -2.0e7), new Vector3(1.0e8, 1.0e8, 4.0e7))) {
            assertEquals(jovian.deflectedBySun(range, observer, et),
                    service.deflectedBySun(range, observer, et),
                    "one formula, two services");
        }
    }

    @Test
    void thePhaseWordsAreTheMoonsBandsWithTheGeometricStatesFirst() {
        double far = 1.0e6; // arcseconds: nowhere near the Sun's disc
        assertEquals(Phase.NEAR_INFERIOR_CONJUNCTION, VenusService.phase(0.0199, 170.0, far, 1000.0));
        assertEquals(Phase.CRESCENT, VenusService.phase(0.02, 160.0, far, 1000.0));
        assertEquals(Phase.CRESCENT, VenusService.phase(0.4799, 100.0, far, 1000.0));
        assertEquals(Phase.NEAR_HALF_LIT, VenusService.phase(0.48, 92.0, far, 1000.0));
        assertEquals(Phase.NEAR_HALF_LIT, VenusService.phase(0.5199, 88.0, far, 1000.0));
        assertEquals(Phase.GIBBOUS, VenusService.phase(0.52, 87.0, far, 1000.0));
        assertEquals(Phase.GIBBOUS, VenusService.phase(0.9799, 16.0, far, 1000.0));
        assertEquals(Phase.NEAR_FULL, VenusService.phase(0.98, 15.0, far, 1000.0));
        assertEquals(Phase.IN_FRONT_OF_SUN, VenusService.phase(0.0, 179.0, 500.0, 1000.0),
                "within the limbs and between us and the Sun: a transit");
        assertEquals(Phase.BEHIND_SUN, VenusService.phase(1.0, 1.0, 500.0, 1000.0),
                "within the limbs and beyond the Sun: hidden");
    }

    @Test
    void the2012TransitIsInFrontOfTheSunAndItsLimbIsNotWellDefined() {
        VenusObservation o = service.observe(OSLO.at(Instant.parse("2012-06-06T01:00:00Z")));
        assertEquals(Phase.IN_FRONT_OF_SUN, o.phase());
        assertTrue(o.elongationDegrees() * 3600.0 < o.sunAngularRadiusArcseconds(),
                "Venus's centre within the Sun's disc");
        assertEquals(LimbConditioning.NEAR_INFERIOR_CONJUNCTION_OR_FULL, o.brightLimbConditioning());
    }

    @Test
    void theGreatestEasternElongationOf2026IsHalfLitInTheEveningSky() {
        VenusObservation o = service.observe(OSLO);
        assertEquals(Phase.NEAR_HALF_LIT, o.phase());
        assertEquals(SolarSystemService.Side.EAST_OF_SUN, o.side());
        assertEquals(LimbConditioning.WELL_DEFINED, o.brightLimbConditioning());
        assertEquals(SolarSystemService.CompassPoint.WNW, o.brightLimbCompassPoint(),
                "the lit side faces the Sun, below the western horizon");
    }

    @Test
    void aRangeIsTheInstantAnswerAtEverySample() {
        TimeRange range = new TimeRange(Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-28T00:00:00Z"), Duration.ofDays(1));
        List<VenusService.Row> rows = service.observe(OSLO, range);
        assertEquals(9, rows.size());
        for (VenusService.Row row : rows) {
            assertEquals(service.observe(OSLO.at(row.sample().instant())), row.observation());
        }
    }
}
