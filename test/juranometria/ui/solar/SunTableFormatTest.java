package juranometria.ui.solar;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The frozen rounding, spelled the same for the reader and the study
 * (issue #400).
 */
class SunTableFormatTest {

    @Test
    void theOsloSampleRowSpellsAsTheContractShowedIt() {
        SkyPosition sun = new SkyPosition(89.670160928, 23.433999183);
        assertEquals("05h 58m 40.8s", SunTableFormat.hms(sun));
        assertEquals("+23° 26′ 02″", SunTableFormat.dms(sun));
        assertEquals("150.56°", SunTableFormat.degrees(150.559493971));
        assertEquals("51.01°", SunTableFormat.altitude(51.005649944, "(x)"));
        assertEquals("1.016165", SunTableFormat.astronomicalUnits(1.01616469255810));
        assertEquals("152.016", SunTableFormat.millionKilometres(152016074.29));
        assertEquals("31′ 27.9″", SunTableFormat.minutesSeconds(1887.944));
        assertEquals("2026-06-21 10:00",
                SunTableFormat.minute(Instant.parse("2026-06-21T10:00:00Z")));
    }

    @Test
    void secondsAndMinutesCarryInsteadOfShowingSixty() {
        // 23h 59m 59.97s rounds to 0.0s of the next minute, hour and day.
        SkyPosition seam = new SkyPosition(359.9998750, 0.0);
        assertEquals("00h 00m 00.0s", SunTableFormat.hms(seam));
        // 59.6″ rounds to a whole minute.
        assertEquals("+0° 01′ 00″",
                SunTableFormat.dms(new SkyPosition(0.0, 0.0165556)));
        assertEquals("32′ 00.0″", SunTableFormat.minutesSeconds(1919.97));
    }

    @Test
    void theSignIsAlwaysShownAndTheMinusIsTheRealOne() {
        assertEquals("−23° 26′ 12″",
                SunTableFormat.dms(new SkyPosition(269.0, -23.436667)));
        assertEquals("+0° 08′ 44″",
                SunTableFormat.dms(new SkyPosition(179.0, 0.145556)));
        assertEquals("−28.76° (below the horizon)",
                SunTableFormat.altitude(-28.7612, "(below the horizon)"),
                "a negative altitude keeps its number and appends the status");
        assertEquals("0.00°", SunTableFormat.altitude(0.0, "(x)"),
                "zero is on the horizon, not below it");
        assertEquals("−0.18°", SunTableFormat.degrees(-0.18));
    }

    @Test
    void theMinuteIsRoundedNotTruncated() {
        assertEquals("2026-06-21 08:24",
                SunTableFormat.minute(Instant.parse("2026-06-21T08:24:26Z")));
        assertEquals("2026-06-21 08:25",
                SunTableFormat.minute(Instant.parse("2026-06-21T08:24:30Z")));
        assertEquals("2026-09-23 00:05",
                SunTableFormat.minute(Instant.parse("2026-09-23T00:05:08Z")));
        assertEquals("2101-01-01 00:00",
                SunTableFormat.minute(Instant.parse("2100-12-31T23:59:59Z")),
                "the last civil second rounds into the next minute, as"
                        + " the study's table shows it");
    }
}
