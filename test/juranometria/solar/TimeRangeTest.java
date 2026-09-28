package juranometria.solar;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The six range contracts the owner named (issue #398, R7): one row,
 * an exactly aligned end, an appended end, the row cap, an inverted
 * range, and a step that cannot advance.
 */
class TimeRangeTest {

    private static final Instant T0 = Instant.parse("2026-06-21T00:00:00Z");

    @Test
    void aRangeOfOneInstantIsOneRow() {
        TimeRange range = new TimeRange(T0, T0, Duration.ofHours(1));
        assertEquals(List.of(new TimeRange.Sample(T0, false)),
                range.samples());
        assertEquals(1, range.rows());
    }

    @Test
    void anEndOnTheGridIsTheLastSampleAndNotAppended() {
        TimeRange range = new TimeRange(T0, T0.plus(Duration.ofHours(3)),
                Duration.ofHours(1));
        List<TimeRange.Sample> samples = range.samples();
        assertEquals(4, samples.size());
        assertEquals(T0.plus(Duration.ofHours(3)), samples.get(3).instant());
        assertFalse(samples.get(3).appendedEnd(),
                "an end that is a grid point is an ordinary sample");
        assertEquals(4, range.rows());
    }

    @Test
    void anEndOffTheGridIsAppendedAndMarked() {
        Instant end = T0.plus(Duration.ofMinutes(150));
        TimeRange range = new TimeRange(T0, end, Duration.ofHours(1));
        List<TimeRange.Sample> samples = range.samples();
        assertEquals(4, samples.size(), "00:00, 01:00, 02:00 and then 02:30");
        assertEquals(end, samples.get(3).instant());
        assertTrue(samples.get(3).appendedEnd(),
                "the reader asked for 02:30 and gets it, marked as off-grid");
        assertFalse(samples.get(2).appendedEnd());
        assertEquals(4, range.rows());
    }

    @Test
    void moreThanTheCapIsRefusedNotTruncated() {
        // 1000 rows exactly is answered; 1001 is refused.
        Instant end = T0.plus(Duration.ofHours(999));
        assertEquals(1000, new TimeRange(T0, end, Duration.ofHours(1)).rows());
        IllegalArgumentException refused = assertThrows(
                IllegalArgumentException.class,
                () -> new TimeRange(T0, end.plus(Duration.ofHours(1)),
                        Duration.ofHours(1)));
        assertTrue(refused.getMessage().contains("1001 rows"),
                refused.getMessage());
        // An appended end counts as a row too.
        assertThrows(IllegalArgumentException.class,
                () -> new TimeRange(T0, end.plus(Duration.ofMinutes(1)),
                        Duration.ofHours(1)),
                "999 grid steps plus an appended end is 1001 rows");
    }

    @Test
    void aRangeThatRunsBackwardsIsRefused() {
        IllegalArgumentException refused = assertThrows(
                IllegalArgumentException.class,
                () -> new TimeRange(T0, T0.minusSeconds(1), Duration.ofHours(1)));
        assertTrue(refused.getMessage().contains("backwards"));
    }

    @Test
    void aStepThatCannotAdvanceIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new TimeRange(T0, T0.plusSeconds(60), Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new TimeRange(T0, T0.plusSeconds(60),
                        Duration.ofSeconds(-1)));
        // A step below the instant's resolution advances nothing.
        assertThrows(IllegalArgumentException.class,
                () -> new TimeRange(T0, T0.plusSeconds(60),
                        Duration.ofNanos(0)));
    }

    @Test
    void theStepIsElapsedTimeNotCivilDays() {
        // Across a European daylight-saving change a "day" step stays
        // 86 400 s: the instants are UTC and so is the grid.
        Instant before = Instant.parse("2026-03-28T12:00:00Z");
        TimeRange range = new TimeRange(before,
                before.plus(Duration.ofDays(2)), Duration.ofDays(1));
        assertEquals(List.of(before, before.plusSeconds(86_400),
                before.plusSeconds(172_800)),
                range.samples().stream().map(TimeRange.Sample::instant).toList());
    }
}
