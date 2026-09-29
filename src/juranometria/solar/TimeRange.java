package juranometria.solar;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A reusable request for a body's quantities over time: inclusive
 * start and end, and a fixed elapsed-time step on the UTC timeline
 * (issue #398, R7).
 *
 * <p>Samples fall at {@code start + k·step} while that is not after
 * {@code end}. If the end is not on that grid it is appended as the
 * last sample and marked, so a reader asking for "until noon" gets
 * noon and can see it was not a grid point. The step is elapsed
 * time - minutes, hours, days of 86 400 seconds - so a daily range
 * does not drift across a civil daylight-saving change; the atlas's
 * instants are UTC and so is this.
 *
 * <p>A range that would produce more than {@link #MAX_ROWS} rows is
 * refused, never truncated: a table missing its last rows is a table
 * that lies about its end. A range that runs backwards is refused,
 * and so is a step that cannot advance the start instant.
 *
 * <p>Nothing here knows which body or which quantities: the same
 * range serves the Sun now and the Moon later, and it encodes no
 * path, animation or planet-loop assumption.
 */
public record TimeRange(Instant start, Instant end, Duration step) {

    /** The most rows one range may ask for. */
    public static final int MAX_ROWS = 1000;

    public TimeRange {
        if (start == null || end == null || step == null) {
            throw new IllegalArgumentException("a range names its start,"
                    + " its end and its step");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("the range runs backwards:"
                    + " end " + end + " is before start " + start);
        }
        if (step.isNegative() || step.isZero()) {
            throw new IllegalArgumentException("the step must be positive;"
                    + " it is " + step);
        }
        if (!start.plus(step).isAfter(start)) {
            throw new IllegalArgumentException("the step " + step
                    + " does not advance " + start);
        }
        long rows = rowsFor(start, end, step);
        if (rows > MAX_ROWS) {
            throw new IllegalArgumentException("the range asks for " + rows
                    + " rows; at most " + MAX_ROWS + " are answered, and"
                    + " a range is never cut short silently");
        }
    }

    /** One instant of the range, and whether it is the appended end. */
    public record Sample(Instant instant, boolean appendedEnd) {
    }

    /** The instants this range asks for, in order. */
    public List<Sample> samples() {
        List<Sample> samples = new ArrayList<>();
        Instant at = start;
        while (!at.isAfter(end)) {
            samples.add(new Sample(at, false));
            at = at.plus(step);
        }
        Instant last = samples.get(samples.size() - 1).instant();
        if (!last.equals(end)) {
            samples.add(new Sample(end, true));
        }
        return Collections.unmodifiableList(samples);
    }

    /** How many rows the range answers, the appended end included. */
    public int rows() {
        return (int) rowsFor(start, end, step);
    }

    private static long rowsFor(Instant start, Instant end, Duration step) {
        long span = Duration.between(start, end).toNanos();
        long stepNanos = step.toNanos();
        long gridRows = span / stepNanos + 1;
        boolean aligned = span % stepNanos == 0;
        return gridRows + (aligned ? 0 : 1);
    }
}
