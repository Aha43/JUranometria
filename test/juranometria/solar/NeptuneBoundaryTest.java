package juranometria.solar;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;

import static juranometria.solar.SharedServices.neptune;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The intervals (#654, N-6a): Neptune 1900 to 2100 and Triton and Nereid 2000
 * to 2100, the civil days inclusive; the refusals name the years; Neptune
 * answers on either side of the moons' years.
 */
class NeptuneBoundaryTest {

    private static final Observer OSLO = new Observer(59.91, 10.75, Instant.parse("2026-10-29T12:00:00Z"));

    @Test
    void theMoonsAnswerFrom2000To2100Inclusive() {
        assertFalse(neptune().moonsAnswerAt(Instant.parse("1999-12-31T23:59:59Z")));
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> neptune().observeMoons(OSLO.at(Instant.parse("1999-12-31T23:59:59Z"))));
        assertTrue(refused.getMessage().contains("2000-01-01 to 2100-12-31"), refused.getMessage());
        for (String when : new String[] {"2000-01-01T00:00:00Z", "2100-12-31T23:59:59Z"}) {
            assertTrue(neptune().moonsAnswerAt(Instant.parse(when)), when);
            assertEquals(2, neptune().observeMoons(OSLO.at(Instant.parse(when))).moons().size(), when);
        }
        assertThrows(IllegalArgumentException.class,
                () -> neptune().observeMoons(OSLO.at(Instant.parse("2101-01-01T00:00:00Z"))));
        assertThrows(IllegalArgumentException.class,
                () -> neptune().observeMoonsGeocentric(Instant.parse("1999-06-01T00:00:00Z")));
    }

    @Test
    void anEpochOutsideTheMoonsKernelIsRefusedBeforeItIsRead() {
        double before = (2451544.5 - 1.0 - 2451545.0) * 86400.0;
        assertThrows(IllegalArgumentException.class, () -> neptune().configurationAtEt(before));
        assertEquals(2, neptune().configurationAtEt((2451544.5 + 1.0 - 2451545.0) * 86400.0).moons().size());
    }

    @Test
    void neptuneItselfAnswersOnBothSidesOfTheMoonsYears() {
        for (String when : new String[] {"1900-01-01T00:00:00Z", "1999-12-31T23:59:59Z",
                "2100-12-31T23:59:59Z"}) {
            assertTrue(neptune().observe(OSLO.at(Instant.parse(when))).distanceKm() > 0.0, when);
        }
        assertThrows(IllegalArgumentException.class,
                () -> neptune().observe(OSLO.at(Instant.parse("2101-01-01T00:00:00Z"))));
        assertThrows(IllegalArgumentException.class,
                () -> neptune().observe(OSLO.at(Instant.parse("1899-12-31T23:59:59Z"))));
        assertEquals(LocalDate.of(1900, 1, 1), neptune().firstDay());
        assertEquals(LocalDate.of(2100, 12, 31), neptune().lastDay());
        assertEquals(LocalDate.of(2000, 1, 1), neptune().moonsFirstDay());
        assertEquals(LocalDate.of(2100, 12, 31), neptune().moonsLastDay());
    }
}
