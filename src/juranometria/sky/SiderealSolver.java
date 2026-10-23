package juranometria.sky;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import juranometria.chart.SkyPosition;

/**
 * Instants from local apparent sidereal time (Sprint 54, issue #612
 * absorbing #569; rulings P3 and P4 in
 * {@code docs/decisions/chart-context.md}).
 *
 * <p>Two questions, both answered by inverting what the Place and Time
 * box already computes - {@link LocalSky#localSiderealTimeDegrees()},
 * apparent sidereal time from {@link SkyFrame#gastDegrees} with the
 * observer's east longitude - and nothing else:
 * <ul>
 * <li>{@link #whenLocalSiderealTimeIs}: the UTC instant on a given UTC
 *     date at which the local apparent sidereal time has a given value,
 *     the occurrence nearest a stated instant when the date holds two
 *     (ruling P3: two occurrences of one sidereal time are about
 *     23 h 56 min apart, so a UTC date holds a second one only when the
 *     first falls in its opening minutes).</li>
 * <li>{@link #nextUpperCulmination}: the next upper culmination of a
 *     body at or after an instant - local apparent sidereal time equal
 *     to the body's apparent right ascension of date - iterated for a
 *     moving body to one second within a declared bound, and refused
 *     past it (ruling P4). The caller supplies the body's apparent place
 *     of date at any instant, topocentric where parallax applies; this
 *     class supplies no ephemeris.</li>
 * </ul>
 *
 * <p>No sidereal rate is typed here: the rate of apparent sidereal
 * time is read from {@link SkyFrame#gastDegrees} itself, over one UTC
 * day around the instant in question, so the only constants are the
 * circle and the UTC day. Precision is one second of time, the
 * instant field's own.
 */
public final class SiderealSolver {

    /** How many rounds a moving body's culmination is iterated before the solver refuses. */
    public static final int ROUNDS = 12;

    /** The convergence target: one second of time. */
    public static final double CONVERGED_SECONDS = 1.0;

    private static final double SECONDS_PER_DAY = 86400.0;

    private SiderealSolver() {
    }

    /** A culmination solved: the instant, the body's altitude then, and the rounds it took. */
    public record Culmination(Instant instant, double altitudeDegrees, int rounds) {

        /** Whether the body culminates below the mathematical horizon. */
        public boolean belowHorizon() {
            return altitudeDegrees < 0.0;
        }
    }

    /** Local apparent sidereal time at an instant for an east longitude, in degrees. */
    public static double localSiderealTimeDegrees(Instant at, double eastLongitudeDegrees) {
        return SkyFrame.normalise(SkyFrame.gastDegrees(SkyFrame.julianDate(at))
                + eastLongitudeDegrees);
    }

    /**
     * The rate of apparent sidereal time in degrees per UTC second, read
     * from the frame over the UTC day starting at this instant.
     */
    static double rateDegreesPerSecond(Instant around) {
        double jd = SkyFrame.julianDate(around);
        double advance = SkyFrame.normalise(SkyFrame.gastDegrees(jd + 1.0)
                - SkyFrame.gastDegrees(jd));
        return (360.0 + advance) / SECONDS_PER_DAY;
    }

    /** A difference of angles folded into (-180, 180]. */
    private static double signed(double degrees) {
        double folded = SkyFrame.normalise(degrees);
        return folded > 180.0 ? folded - 360.0 : folded;
    }

    /**
     * The first instant at or after {@code from} at which the local
     * apparent sidereal time is {@code targetDegrees}, to a millisecond.
     */
    static Instant nextLocalSiderealTime(Instant from, double targetDegrees,
                                         double eastLongitudeDegrees) {
        double rate = rateDegreesPerSecond(from);
        double ahead = SkyFrame.normalise(targetDegrees
                - localSiderealTimeDegrees(from, eastLongitudeDegrees));
        Instant at = plusSeconds(from, ahead / rate);
        for (int round = 0; round < 8; round++) {
            double correction = signed(targetDegrees
                    - localSiderealTimeDegrees(at, eastLongitudeDegrees)) / rate;
            at = plusSeconds(at, correction);
            if (Math.abs(correction) < 1.0e-3) {
                break;
            }
        }
        return at;
    }

    private static Instant plusSeconds(Instant at, double seconds) {
        return at.plusNanos(Math.round(seconds * 1.0e9));
    }

    /**
     * The UTC instant on {@code utcDate} at which the local apparent
     * sidereal time is {@code targetDegrees} - whole seconds, so that
     * the instant field can show it - choosing, when the date holds two
     * such instants, the one nearest {@code nearestTo} (ruling P3). The
     * date is never left: the result lies within it.
     *
     * <p>The instant is the first whole second at or after the exact
     * solution, so a box that truncates sidereal time to the minute reads
     * the minute asked for back.
     */
    public static Instant whenLocalSiderealTimeIs(double targetDegrees, LocalDate utcDate,
                                                  double eastLongitudeDegrees,
                                                  Instant nearestTo) {
        if (utcDate == null || nearestTo == null) {
            throw new IllegalArgumentException("a UTC date and the instant to stand nearest");
        }
        Instant dayStart = utcDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);
        List<Instant> occurrences = new ArrayList<>();
        Instant at = nextLocalSiderealTime(dayStart, targetDegrees, eastLongitudeDegrees);
        while (at.isBefore(dayEnd)) {
            occurrences.add(at);
            // The next occurrence: one sidereal day on, re-solved.
            at = nextLocalSiderealTime(at.plusSeconds(1), targetDegrees,
                    eastLongitudeDegrees);
        }
        if (occurrences.isEmpty()) {
            // Cannot happen - a sidereal day is shorter than a UTC day -
            // but a solver states its own impossibility rather than
            // indexing an empty list.
            throw new IllegalStateException("no occurrence of the sidereal time on " + utcDate);
        }
        Instant nearest = occurrences.get(0);
        for (Instant candidate : occurrences) {
            if (Math.abs(ChronoUnit.MILLIS.between(nearestTo, candidate))
                    < Math.abs(ChronoUnit.MILLIS.between(nearestTo, nearest))) {
                nearest = candidate;
            }
        }
        return wholeSecondAtOrAfter(nearest);
    }

    /** The first whole second at or after an instant. */
    static Instant wholeSecondAtOrAfter(Instant exact) {
        Instant truncated = exact.truncatedTo(ChronoUnit.SECONDS);
        return truncated.equals(exact) ? truncated : truncated.plusSeconds(1);
    }

    /**
     * The next upper culmination of a body at or after the observer's
     * instant (ruling P4): the instant at which the local apparent
     * sidereal time equals the body's apparent right ascension of date,
     * the body's place re-read at each candidate until the instant moves
     * by less than {@link #CONVERGED_SECONDS}, within {@link #ROUNDS};
     * null when it does not settle - the caller refuses with its message
     * and installs nothing. The altitude is the geometric altitude on the
     * meridian, {@code 90° - |latitude - declination of date|}, with no
     * refraction, so that <em>on the meridian</em> never reads as
     * <em>observable</em>.
     *
     * @param from           the observer: the place, and the instant to search from
     * @param apparentOfDate the body's apparent place of date at an instant - topocentric
     *                       where parallax applies, as the services compute it
     */
    public static Culmination nextUpperCulmination(Observer from,
                                                   Function<Instant, SkyPosition> apparentOfDate) {
        if (from == null || apparentOfDate == null) {
            throw new IllegalArgumentException("an observer and the body's apparent place of date");
        }
        double longitude = from.eastLongitudeDegrees();
        Instant start = from.instant();
        SkyPosition place = apparentOfDate.apply(start);
        Instant at = nextLocalSiderealTime(start, place.raDegrees(), longitude);
        for (int round = 1; round <= ROUNDS; round++) {
            place = apparentOfDate.apply(at);
            Instant next = nextLocalSiderealTime(start, place.raDegrees(), longitude);
            double moved = Math.abs(ChronoUnit.MILLIS.between(at, next)) / 1000.0;
            at = next;
            if (moved < CONVERGED_SECONDS) {
                double altitude = 90.0 - Math.abs(from.latitudeDegrees() - place.decDegrees());
                return new Culmination(wholeSecondAtOrAfter(at), altitude, round);
            }
        }
        return null;
    }
}
