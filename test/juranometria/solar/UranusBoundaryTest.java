package juranometria.solar;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The intervals (#633, U-6a): Uranus 1900 to 2100 and its five moons 2000
 * to 2100, the civil days inclusive; the refusals name the years; Uranus
 * answers on either side of the moons' years.
 */
class UranusBoundaryTest {

    private static final UranusService SERVICE = UranusService.load();
    private static final Observer OSLO = new Observer(59.91, 10.75, Instant.parse("2026-10-27T12:00:00Z"));

    @Test
    void theMoonsAnswerFrom2000To2100Inclusive() {
        assertFalse(SERVICE.moonsAnswerAt(Instant.parse("1999-12-31T23:59:59Z")));
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observeMoons(OSLO.at(Instant.parse("1999-12-31T23:59:59Z"))));
        assertTrue(refused.getMessage().contains("2000-01-01 to 2100-12-31"), refused.getMessage());
        for (String when : new String[] {"2000-01-01T00:00:00Z", "2100-12-31T23:59:59Z"}) {
            assertTrue(SERVICE.moonsAnswerAt(Instant.parse(when)), when);
            assertEquals(5, SERVICE.observeMoons(OSLO.at(Instant.parse(when))).moons().size(), when);
        }
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observeMoons(OSLO.at(Instant.parse("2101-01-01T00:00:00Z"))));
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observeMoonsGeocentric(Instant.parse("1999-06-01T00:00:00Z")));
    }

    @Test
    void anEpochOutsideTheMoonsKernelIsRefusedBeforeItIsRead() {
        double before = (2451544.5 - 1.0 - 2451545.0) * 86400.0;
        assertThrows(IllegalArgumentException.class, () -> SERVICE.configurationAtEt(before));
        assertEquals(5, SERVICE.configurationAtEt((2451544.5 + 1.0 - 2451545.0) * 86400.0).moons().size());
    }

    @Test
    void uranusItselfAnswersOnBothSidesOfTheMoonsYears() {
        for (String when : new String[] {"1900-01-01T00:00:00Z", "1999-12-31T23:59:59Z",
                "2100-12-31T23:59:59Z"}) {
            assertTrue(SERVICE.observe(OSLO.at(Instant.parse(when))).distanceKm() > 0.0, when);
        }
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observe(OSLO.at(Instant.parse("2101-01-01T00:00:00Z"))));
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observe(OSLO.at(Instant.parse("1899-12-31T23:59:59Z"))));
        assertEquals(LocalDate.of(1900, 1, 1), SERVICE.firstDay());
        assertEquals(LocalDate.of(2100, 12, 31), SERVICE.lastDay());
        assertEquals(LocalDate.of(2000, 1, 1), SERVICE.moonsFirstDay());
        assertEquals(LocalDate.of(2100, 12, 31), SERVICE.moonsLastDay());
    }
}
