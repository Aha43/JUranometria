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
 * The moons' interval (#601, M-7a with the amendment): Phobos and Deimos
 * from 2000-01-01 up to, but not including, 2050-01-01 00:00 TDB, the
 * civil instant converted with the atlas's own time scales before the
 * comparison; the refusal names 2000 to 2049; Mars itself answers on
 * either side, over 1900-2100.
 */
class MarsBoundaryTest {

    private static final MarsService SERVICE = MarsService.load();
    private static final Observer OSLO = new Observer(59.91, 10.75, Instant.parse("2026-10-25T12:00:00Z"));

    /** The civil instant, to the millisecond, whose TT reading is the exclusive end. */
    private static Instant civilEnd() {
        double end = SERVICE.moonsEndJdTdb();
        long lo = Instant.parse("2049-12-31T23:50:00Z").toEpochMilli();
        long hi = Instant.parse("2050-01-01T00:00:00Z").toEpochMilli();
        assertTrue(SERVICE.timeScales().tt(Instant.ofEpochMilli(lo)).jdTt() < end);
        assertTrue(SERVICE.timeScales().tt(Instant.ofEpochMilli(hi)).jdTt() >= end);
        while (hi - lo > 1) {
            long mid = (lo + hi) / 2;
            if (SERVICE.timeScales().tt(Instant.ofEpochMilli(mid)).jdTt() < end) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return Instant.ofEpochMilli(hi);
    }

    @Test
    void theEndIsExclusiveOnTheTdbReadingOfTheCivilInstant() {
        Instant end = civilEnd();
        assertTrue(end.isBefore(Instant.parse("2050-01-01T00:00:00Z")),
                "the last civil seconds of 2049 already lie past the TDB boundary: " + end);
        Instant justBefore = end.minusSeconds(1);
        assertTrue(SERVICE.moonsAnswerAt(justBefore));
        assertEquals(2, SERVICE.observeMoons(OSLO.at(justBefore)).moons().size());
        assertFalse(SERVICE.moonsAnswerAt(end));
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observeMoons(OSLO.at(end)));
        assertTrue(refused.getMessage().contains("2000 to 2049"), refused.getMessage());
        assertThrows(IllegalArgumentException.class, () -> SERVICE.observeMoonsGeocentric(end));
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observeMoons(OSLO.at(Instant.parse("2050-06-01T00:00:00Z"))));
    }

    @Test
    void theKernelsFinalInstantIsNeverEvaluated() {
        double endEt = (SERVICE.moonsEndJdTdb() - 2451545.0) * 86400.0;
        assertThrows(IllegalArgumentException.class, () -> SERVICE.configurationAtEt(endEt),
                "the exclusive end itself is refused before any kernel is read");
        assertEquals(2, SERVICE.configurationAtEt(endEt - 1.0).moons().size());
    }

    @Test
    void theStartIsTheFirstCivilDayOf2000() {
        assertFalse(SERVICE.moonsAnswerAt(Instant.parse("1999-12-31T23:59:59Z")));
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observeMoons(OSLO.at(Instant.parse("1999-12-31T23:59:59Z"))));
        assertTrue(SERVICE.moonsAnswerAt(Instant.parse("2000-01-01T00:00:00Z")));
        assertEquals(2, SERVICE.observeMoons(OSLO.at(Instant.parse("2000-01-01T00:00:00Z"))).moons().size());
    }

    @Test
    void marsItselfAnswersOnBothSidesOfTheMoonsYears() {
        for (String when : new String[] {"1900-01-01T00:00:00Z", "1999-12-31T23:59:59Z",
                "2050-06-01T00:00:00Z", "2100-12-31T23:59:59Z"}) {
            assertTrue(SERVICE.observe(OSLO.at(Instant.parse(when))).distanceKm() > 0.0, when);
        }
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observe(OSLO.at(Instant.parse("2101-01-01T00:00:00Z"))));
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observe(OSLO.at(Instant.parse("1899-12-31T23:59:59Z"))));
        assertEquals(MarsService.MARS_FIRST_DAY, SERVICE.firstDay());
        assertEquals(MarsService.MARS_LAST_DAY, SERVICE.lastDay());
        assertEquals(LocalDate.of(2000, 1, 1), MarsService.MOONS_FIRST_DAY);
        assertEquals(MarsSystemPack.load().moonsLastYear(), MarsService.MOONS_LAST_YEAR);
        assertEquals(MarsSystemPack.load().moonsFirstDay(), MarsService.MOONS_FIRST_DAY);
    }
}
