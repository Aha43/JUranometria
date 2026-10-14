package juranometria.solar;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import juranometria.sky.Observer;
import juranometria.solar.MercuryService.LimbConditioning;
import juranometria.solar.MercuryService.MercuryObservation;
import juranometria.solar.MercuryService.Phase;
import juranometria.solar.spk.Vector3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Mercury service's shape (issue #529): the interval it answers and
 * refuses by name, the phase words and their precedence - the bands
 * shared with Venus, as ruled on #528 (ruling 5) - the bright limb's
 * conditioning, the range, the geocentric intermediate, and the one
 * formula it shares with Jupiter's and Venus's services held equal to
 * them.
 */
class MercuryServiceTest {

    private static MercuryService service;
    /** Oslo at the 2026 greatest eastern elongation (Espenak, mercury-events.txt). */
    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-02-19T18:00:00Z"));

    @BeforeAll
    static void load() {
        service = MercuryService.load();
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
            assertTrue(refused.getMessage().contains("Mercury is computed for civil dates from"
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
        assertEquals(service.observe(OSLO), MercuryService.load().observe(OSLO));
    }

    @Test
    void theDeflectionIsJupitersAndVenussFormulaExactly() {
        JovianSystemService jovian = JovianSystemService.load();
        VenusService venus = VenusService.load();
        double et = service.timeScales().tt(OSLO.instant()).secondsPastJ2000();
        Vector3 observer = new Vector3(1.2e8, -8.1e7, -3.5e7);
        for (Vector3 range : List.of(new Vector3(-1.4e8, 2.0e7, 1.1e7),
                new Vector3(3.0e7, 9.0e7, -2.0e7), new Vector3(1.0e8, 1.0e8, 4.0e7))) {
            assertEquals(jovian.deflectedBySun(range, observer, et),
                    service.deflectedBySun(range, observer, et),
                    "one formula, three services");
            assertEquals(venus.deflectedBySun(range, observer, et),
                    service.deflectedBySun(range, observer, et));
        }
    }

    @Test
    void thePhaseBandsAreVenussExactlyWithTheGeometricStatesFirst() {
        // #528, ruling 5: the shared phase bands. Over a grid of the
        // fraction, the phase angle and the limb geometry, Mercury's word
        // is Venus's word, name for name.
        for (double k = 0.0; k <= 1.0; k += 0.005) {
            for (double angle : new double[] {1.0, 60.0, 89.0, 91.0, 120.0, 179.0}) {
                for (double separation : new double[] {500.0, 1.0e6}) {
                    assertEquals(VenusService.phase(k, angle, separation, 1000.0).name(),
                            MercuryService.phase(k, angle, separation, 1000.0).name(),
                            "k " + k + ", i " + angle + ", separation " + separation);
                }
            }
        }
        double far = 1.0e6; // arcseconds: nowhere near the Sun's disc
        assertEquals(Phase.NEAR_INFERIOR_CONJUNCTION, MercuryService.phase(0.0199, 170.0, far, 1000.0));
        assertEquals(Phase.CRESCENT, MercuryService.phase(0.02, 160.0, far, 1000.0));
        assertEquals(Phase.NEAR_HALF_LIT, MercuryService.phase(0.48, 92.0, far, 1000.0));
        assertEquals(Phase.GIBBOUS, MercuryService.phase(0.52, 87.0, far, 1000.0));
        assertEquals(Phase.NEAR_FULL, MercuryService.phase(0.98, 15.0, far, 1000.0));
        assertEquals(Phase.IN_FRONT_OF_SUN, MercuryService.phase(0.0, 179.0, 500.0, 1000.0),
                "within the limbs and between us and the Sun: a transit");
        assertEquals(Phase.BEHIND_SUN, MercuryService.phase(1.0, 1.0, 500.0, 1000.0),
                "within the limbs and beyond the Sun: hidden");
    }

    @Test
    void the2019TransitIsInFrontOfTheSunAndItsLimbIsNotWellDefined() {
        // NASA/GSFC: greatest transit 2019-11-11 15:20 UT (mercury-events.txt).
        MercuryObservation o = service.observe(OSLO.at(Instant.parse("2019-11-11T15:20:00Z")));
        assertEquals(Phase.IN_FRONT_OF_SUN, o.phase());
        assertTrue(o.elongationDegrees() * 3600.0 < o.sunAngularRadiusArcseconds(),
                "Mercury's centre within the Sun's disc");
        assertEquals(LimbConditioning.NEAR_INFERIOR_CONJUNCTION_OR_FULL, o.brightLimbConditioning());
    }

    @Test
    void theGreatestEasternElongationOf2026IsHalfLitInTheEveningSky() {
        MercuryObservation o = service.observe(OSLO);
        assertEquals(Phase.NEAR_HALF_LIT, o.phase());
        assertEquals(SolarSystemService.Side.EAST_OF_SUN, o.side());
        assertEquals(LimbConditioning.WELL_DEFINED, o.brightLimbConditioning());
        assertEquals(SolarSystemService.CompassPoint.WSW, o.brightLimbCompassPoint(),
                "the lit side faces the Sun, below the western horizon");
    }

    @Test
    void aRangeIsTheInstantAnswerAtEverySample() {
        TimeRange range = new TimeRange(Instant.parse("2026-03-04T00:00:00Z"),
                Instant.parse("2026-03-10T00:00:00Z"), Duration.ofDays(1));
        List<MercuryService.Row> rows = service.observe(OSLO, range);
        assertEquals(7, rows.size());
        for (MercuryService.Row row : rows) {
            assertEquals(service.observe(OSLO.at(row.sample().instant())), row.observation());
        }
    }

    @Test
    void venusAnswersAsBeforeBesideIt() {
        // Invariance (#529): Venus's service reads its own file of the
        // same pack; loading Mercury's changes nothing it says.
        Observer venusOslo = new Observer(59.91, 10.75, Instant.parse("2026-08-15T06:00:00Z"));
        VenusService.VenusObservation before = VenusService.load().observe(venusOslo);
        service.observe(OSLO);
        assertEquals(before, VenusService.load().observe(venusOslo));
        assertEquals(VenusService.Phase.NEAR_HALF_LIT, before.phase());
    }
}
