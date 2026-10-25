package juranometria.ui.coordinates;

import java.math.BigDecimal;

import juranometria.chart.SkyPosition;

/**
 * J2000 equatorial coordinates as a reader types them, component by
 * component (issue #623, ruling C-1 in
 * {@code docs/decisions/centre-on-coordinates.md}).
 *
 * <p>Strict, because a coordinate repaired behind a reader's back is a
 * different place: hours, minutes, degrees and arcminutes are whole
 * numbers, seconds may carry decimals; minutes and seconds lie below
 * 60; right ascension is 0h to 24h and 24h is accepted only as
 * 24 00 00, which normalises to 0h; declination is at most 90° and
 * exactly ±90° only with zero minutes and seconds. An empty minutes or
 * seconds field counts as zero; an empty hours or degrees field is
 * refused. Nothing here infers an epoch: the frame is J2000, stated
 * by the presentation that asks.
 */
public final class CoordinateEntry {

    /** Which component a refusal is about. */
    public enum Refusal {
        /** A field that is not a number of the kind it asks for. */
        NUMBER,
        /** Right ascension outside 0h-24h, or a component at or over 60. */
        RIGHT_ASCENSION,
        /** Declination over 90°, or a component at or over 60, or ±90° with more. */
        DECLINATION
    }

    /** What an entry answered: a position in normalised form, or why not. */
    public record Result(SkyPosition position, Refusal refusal, String offending,
                         String hours, String minutes, String seconds,
                         boolean south, String degrees, String arcminutes,
                         String arcseconds) {

        /** Whether the entry was accepted. */
        public boolean accepted() {
            return position != null;
        }
    }

    private CoordinateEntry() {
    }

    /**
     * Reads the six components and the declination's sign.
     *
     * @param south true for a negative declination
     */
    public static Result read(String hours, String minutes, String seconds,
                              boolean south, String degrees, String arcminutes,
                              String arcseconds) {
        Integer h = whole(hours, false);
        if (h == null) {
            return refused(Refusal.NUMBER, hours);
        }
        Integer m = whole(minutes, true);
        if (m == null) {
            return refused(Refusal.NUMBER, minutes);
        }
        BigDecimal s = decimal(seconds);
        if (s == null) {
            return refused(Refusal.NUMBER, seconds);
        }
        Integer d = whole(degrees, false);
        if (d == null) {
            return refused(Refusal.NUMBER, degrees);
        }
        Integer am = whole(arcminutes, true);
        if (am == null) {
            return refused(Refusal.NUMBER, arcminutes);
        }
        BigDecimal as = decimal(arcseconds);
        if (as == null) {
            return refused(Refusal.NUMBER, arcseconds);
        }
        BigDecimal sixty = BigDecimal.valueOf(60);
        if (h > 24 || m >= 60 || s.compareTo(sixty) >= 0
                || (h == 24 && (m != 0 || s.signum() != 0))) {
            return refused(Refusal.RIGHT_ASCENSION, null);
        }
        if (d > 90 || am >= 60 || as.compareTo(sixty) >= 0
                || (d == 90 && (am != 0 || as.signum() != 0))) {
            return refused(Refusal.DECLINATION, null);
        }
        int hoursShown = h == 24 ? 0 : h;
        double ra = 15.0 * (hoursShown + m / 60.0 + s.doubleValue() / 3600.0);
        double dec = d + am / 60.0 + as.doubleValue() / 3600.0;
        boolean negative = south && (d != 0 || am != 0 || as.signum() != 0);
        return new Result(new SkyPosition(ra, negative ? -dec : dec), null, null,
                Integer.toString(hoursShown), twoDigits(m), shown(s), negative,
                Integer.toString(d), twoDigits(am), shown(as));
    }

    private static Result refused(Refusal why, String offending) {
        return new Result(null, why, offending == null ? "" : offending.strip(),
                null, null, null, false, null, null, null);
    }

    /** A whole number of digits only; empty is zero when allowed, else refused. */
    private static Integer whole(String text, boolean emptyIsZero) {
        String typed = text == null ? "" : text.strip();
        if (typed.isEmpty()) {
            return emptyIsZero ? 0 : null;
        }
        if (!typed.chars().allMatch(Character::isDigit) || typed.length() > 3) {
            return null;
        }
        return Integer.parseInt(typed);
    }

    /** Digits with at most one decimal point (a comma reads as one); empty is zero. */
    private static BigDecimal decimal(String text) {
        String typed = text == null ? "" : text.strip().replace(',', '.');
        if (typed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int points = 0;
        for (char c : typed.toCharArray()) {
            if (c == '.') {
                points++;
            } else if (!Character.isDigit(c)) {
                return null;
            }
        }
        if (points > 1 || typed.equals(".") || typed.length() > 12) {
            return null;
        }
        return new BigDecimal(typed);
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    /** Seconds as typed, normalised: two whole digits, the decimals the reader gave. */
    private static String shown(BigDecimal seconds) {
        String plain = seconds.toPlainString();
        int point = plain.indexOf('.');
        String whole = point < 0 ? plain : plain.substring(0, point);
        String fraction = point < 0 ? "" : plain.substring(point);
        return (whole.length() < 2 ? "0" + whole : whole) + fraction;
    }
}
