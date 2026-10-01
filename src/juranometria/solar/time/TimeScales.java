package juranometria.solar.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * From a civil instant to Terrestrial Time, the way the Solar System
 * contract reads it (issue #398, R-b).
 *
 * <p>The atlas's civil instant is a {@code java.time.Instant}: seconds
 * on the UTC timeline, as Place and Time keeps it. What that instant
 * means, and how exactly TT follows from it, depends on when it is:
 *
 * <ul>
 *   <li><b>Before 1972-01-01</b>, UTC as now defined did not exist; the
 *   civil instant is read as UT1, and TT = UT1 + ΔT with the
 *   Espenak–Meeus expressions. {@code ESTIMATED_BEFORE_RECORD}.</li>
 *   <li><b>From 1972-01-01 until the pinned IERS file's expiry</b>, the
 *   reading is exact: TT = UTC + (TAI − UTC) + 32.184 s.
 *   {@code EXACT}.</li>
 *   <li><b>From the expiry on</b>, the record vouches for nothing. Under
 *   the atlas's standing approximation UT1 ≈ UTC, TT − UTC ≈ ΔT from
 *   the same published expressions, and the answer says so.
 *   {@code ESTIMATED_AFTER_RECORD}.</li>
 * </ul>
 *
 * <p>TDB is taken as TT: the difference is under 2 ms, three orders
 * below anything this product states. UT1 is taken as UTC for Earth
 * rotation, which is Place and Time's own rule; this class does not
 * touch it.
 *
 * <p>Nothing here reads a clock. An instant comes in, an epoch goes
 * out, and the same instant always gives the same epoch.
 */
public final class TimeScales {

    /** TT − TAI, exactly, by definition. */
    public static final double TT_MINUS_TAI = 32.184;

    /** J2000.0 as a TT Julian date. */
    public static final double J2000_JD = 2451545.0;

    /** How much the answer can be trusted, era by era. */
    public enum Confidence {
        /** UTC → TAI → TT from the pinned leap-second record. */
        EXACT,
        /** Civil instant read as UT1; ΔT from a published model. */
        ESTIMATED_BEFORE_RECORD,
        /** Beyond the record's expiry; ΔT from a published model. */
        ESTIMATED_AFTER_RECORD
    }

    /**
     * A TT epoch and how it was obtained.
     *
     * @param jdTt       Julian date on the TT scale
     * @param confidence which reading produced it
     * @param ttMinusCivilSeconds TT minus the civil instant, in seconds
     */
    public record Epoch(double jdTt, Confidence confidence,
                        double ttMinusCivilSeconds) {

        /** Seconds past J2000.0 on the TT (≈ TDB) scale, as SPK wants. */
        public double secondsPastJ2000() {
            return (jdTt - J2000_JD) * 86400.0;
        }
    }

    private static final LocalDate UTC_BEGINS = LocalDate.of(1972, 1, 1);

    private final LeapSeconds leapSeconds;

    public TimeScales(LeapSeconds leapSeconds) {
        if (!leapSeconds.first().equals(UTC_BEGINS)) {
            throw new IllegalArgumentException("the leap-second record is"
                    + " expected to begin with UTC itself, 1972-01-01; this"
                    + " one begins " + leapSeconds.first());
        }
        this.leapSeconds = leapSeconds;
    }

    /** The first date the reading is exact: 1972-01-01. */
    public LocalDate exactFrom() {
        return UTC_BEGINS;
    }

    /** The first date the reading is no longer exact: the record's expiry. */
    public LocalDate exactUntil() {
        return leapSeconds.expires();
    }

    /** The TT epoch of a civil instant, and how it was obtained. */
    public Epoch tt(Instant civil) {
        LocalDate date = civil.atOffset(ZoneOffset.UTC).toLocalDate();
        double jdCivil = julianDate(civil);
        if (date.isBefore(UTC_BEGINS)) {
            double deltaT = DeltaT.espenakMeeus(decimalYear(civil));
            return new Epoch(jdCivil + deltaT / 86400.0,
                    Confidence.ESTIMATED_BEFORE_RECORD, deltaT);
        }
        if (date.isBefore(leapSeconds.expires())) {
            double offset = leapSeconds.taiMinusUtc(date) + TT_MINUS_TAI;
            return new Epoch(jdCivil + offset / 86400.0, Confidence.EXACT,
                    offset);
        }
        double deltaT = DeltaT.espenakMeeus(decimalYear(civil));
        return new Epoch(jdCivil + deltaT / 86400.0,
                Confidence.ESTIMATED_AFTER_RECORD, deltaT);
    }

    /**
     * The Julian date of a civil instant read as a plain count of days
     * on its own scale: {@code SkyFrame.julianDate}'s arithmetic, kept
     * here so this package depends on no sky class for it.
     */
    public static double julianDate(Instant civil) {
        return civil.getEpochSecond() / 86400.0
                + civil.getNano() / 86400e9 + 2440587.5;
    }

    /** Decimal year, the argument the ΔT expressions take. */
    static double decimalYear(Instant civil) {
        LocalDate date = civil.atOffset(ZoneOffset.UTC).toLocalDate();
        LocalDate start = LocalDate.of(date.getYear(), 1, 1);
        LocalDate next = start.plusYears(1);
        double secondsIntoYear = ChronoUnit.SECONDS.between(
                start.atStartOfDay(ZoneOffset.UTC).toInstant(), civil);
        double secondsInYear = ChronoUnit.SECONDS.between(
                start.atStartOfDay(ZoneOffset.UTC).toInstant(),
                next.atStartOfDay(ZoneOffset.UTC).toInstant());
        return date.getYear() + secondsIntoYear / secondsInYear;
    }
}
