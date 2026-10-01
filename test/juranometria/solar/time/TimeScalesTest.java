package juranometria.solar.time;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The time-scale reading the contract froze (issue #398, R-b): exact
 * between 1972 and the pinned record's expiry, estimated either side
 * from the published expressions, and never a clock.
 */
class TimeScalesTest {

    /** The IERS file's shape, with its own expiry line. */
    private static final String IERS = """
            #  Value of TAI-UTC in second valid beetween the initial value until
            #  the epoch given on the next line. The last line reads that NO
            #  leap second was introduced since the corresponding date
            #  Updated through IERS Bulletin C69
            #
            #  File expires on 28 June 2027
            #
            #    MJD        Date    TAI-UTC (s)
            #           day month year
            #    ---    --------------   ------
            #
                41317.0    1  1 1972       10
                41499.0    1  7 1972       11
                41683.0    1  1 1973       12
                42048.0    1  1 1974       13
                42413.0    1  1 1975       14
                42778.0    1  1 1976       15
                43144.0    1  1 1977       16
                43509.0    1  1 1978       17
                43874.0    1  1 1979       18
                44239.0    1  1 1980       19
                44786.0    1  7 1981       20
                45151.0    1  7 1982       21
                45516.0    1  7 1983       22
                46247.0    1  7 1985       23
                47161.0    1  1 1988       24
                47892.0    1  1 1990       25
                48257.0    1  1 1991       26
                48804.0    1  7 1992       27
                49169.0    1  7 1993       28
                49534.0    1  7 1994       29
                50083.0    1  1 1996       30
                50630.0    1  7 1997       31
                51179.0    1  1 1999       32
                53736.0    1  1 2006       33
                54832.0    1  1 2009       34
                56109.0    1  7 2012       35
                57204.0    1  7 2015       36
                57754.0    1  1 2017       37
            """;

    private static TimeScales scales() throws IOException {
        return new TimeScales(LeapSeconds.parse(IERS));
    }

    @Test
    void theRecordIsReadAsPublishedExpiryIncluded() throws IOException {
        LeapSeconds record = LeapSeconds.parse(IERS);
        assertEquals(LocalDate.of(1972, 1, 1), record.first());
        assertEquals(LocalDate.of(2027, 6, 28), record.expires());
        assertEquals(10, record.taiMinusUtc(LocalDate.of(1972, 6, 30)));
        assertEquals(11, record.taiMinusUtc(LocalDate.of(1972, 7, 1)));
        assertEquals(37, record.taiMinusUtc(LocalDate.of(2027, 6, 27)));
        assertThrows(IllegalArgumentException.class,
                () -> record.taiMinusUtc(LocalDate.of(2027, 6, 28)),
                "on the expiry the record vouches for nothing");
        assertThrows(IllegalArgumentException.class,
                () -> record.taiMinusUtc(LocalDate.of(1971, 12, 31)));
    }

    @Test
    void aRecordWithoutAnExpiryOrOutOfStepIsRefused() {
        assertThrows(IOException.class, () -> LeapSeconds.parse(
                "    41317.0    1  1 1972       10\n"),
                "no expiry, no validity");
        assertThrows(IOException.class, () -> LeapSeconds.parse(
                "#  File expires on 28 June 2027\n"
                        + "    41317.0    1  1 1972       10\n"
                        + "    41499.0    1  7 1972       12\n"),
                "a skipped second is a corrupt record");
    }

    @Test
    void betweenUtcAndTheExpiryTheReadingIsExact() throws IOException {
        TimeScales.Epoch e = scales().tt(Instant.parse("2026-06-21T10:00:00Z"));
        assertEquals(TimeScales.Confidence.EXACT, e.confidence());
        assertEquals(37 + 32.184, e.ttMinusCivilSeconds(), 1e-12);
        assertEquals(2461212.5 + (10 * 3600 + 69.184) / 86400.0, e.jdTt(), 1e-9);
        TimeScales.Epoch first = scales().tt(Instant.parse("1972-01-01T00:00:00Z"));
        assertEquals(TimeScales.Confidence.EXACT, first.confidence());
        assertEquals(42.184, first.ttMinusCivilSeconds(), 1e-12);
    }

    @Test
    void beforeUtcTheCivilInstantIsUt1PlusDeltaT() throws IOException {
        TimeScales.Epoch e = scales().tt(Instant.parse("1900-01-01T00:00:00Z"));
        assertEquals(TimeScales.Confidence.ESTIMATED_BEFORE_RECORD, e.confidence());
        assertEquals(-2.79, e.ttMinusCivilSeconds(), 1e-9,
                "Espenak–Meeus at 1900.0");
        TimeScales.Epoch last = scales().tt(Instant.parse("1971-12-31T23:59:59Z"));
        assertEquals(TimeScales.Confidence.ESTIMATED_BEFORE_RECORD, last.confidence());
        assertEquals(42.25, last.ttMinusCivilSeconds(), 0.01,
                "and joins the exact reading within a tenth of a second");
    }

    @Test
    void afterTheExpiryTheReadingIsEstimatedAndSaysSo() throws IOException {
        TimeScales.Epoch e = scales().tt(Instant.parse("2027-06-28T00:00:00Z"));
        assertEquals(TimeScales.Confidence.ESTIMATED_AFTER_RECORD, e.confidence());
        assertEquals(DeltaT.espenakMeeus(TimeScales.decimalYear(
                Instant.parse("2027-06-28T00:00:00Z"))),
                e.ttMinusCivilSeconds(), 1e-9);
        assertTrue(e.ttMinusCivilSeconds() > 75 && e.ttMinusCivilSeconds() < 77,
                "the published model runs a few seconds ahead of the"
                        + " record in the late 2020s; that is the estimate's"
                        + " honest error and it is not hidden: "
                        + e.ttMinusCivilSeconds());
    }

    @Test
    void theExpressionsAreThePublishedOnes() {
        assertEquals(-2.79, DeltaT.espenakMeeus(1900.0), 1e-12);
        assertEquals(21.20, DeltaT.espenakMeeus(1920.0), 1e-12);
        assertEquals(29.07, DeltaT.espenakMeeus(1950.0), 1e-12);
        assertEquals(45.45, DeltaT.espenakMeeus(1975.0), 1e-12);
        assertEquals(63.86, DeltaT.espenakMeeus(2000.0), 1e-12);
        assertEquals(62.92 + 0.32217 * 26 + 0.005589 * 676,
                DeltaT.espenakMeeus(2026.0), 1e-9);
        assertEquals(93.0, DeltaT.espenakMeeus(2050.0), 1e-9);
        assertEquals(202.74, DeltaT.espenakMeeus(2100.0), 1e-9);
        assertThrows(IllegalArgumentException.class,
                () -> DeltaT.espenakMeeus(1899.99));
    }

    @Test
    void theBoundariesAreReadFromTheRecordNotWritten() throws IOException {
        TimeScales scales = scales();
        assertEquals(LocalDate.of(1972, 1, 1), scales.exactFrom());
        assertEquals(LocalDate.of(2027, 6, 28), scales.exactUntil(),
                "a later record moves this date; nothing else does");
    }

    @Test
    void theSameInstantAlwaysGivesTheSameEpoch() throws IOException {
        Instant civil = Instant.parse("1999-12-31T23:59:59.5Z");
        assertEquals(scales().tt(civil), scales().tt(civil));
        assertEquals(TimeScales.julianDate(Instant.parse("2000-01-01T12:00:00Z")),
                2451545.0, 1e-12, "J2000.0 by the plain count");
    }
}
