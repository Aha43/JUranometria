package juranometria.ui.solar;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import juranometria.chart.SkyPosition;

/**
 * The frozen rounding of the Sun table (issue #398, R-d; issue #400),
 * in {@code Locale.ROOT}, for the reader's table and the study alike.
 *
 * <p>Right ascension to a tenth of a second of time; declination to a
 * second of arc with the sign always shown; degrees to a hundredth;
 * distance to six decimals of an astronomical unit and thousandths of
 * a million kilometres; apparent diameter to a tenth of an arcsecond;
 * the instant to the minute, rounded. Numbers are notation, formatted
 * here and handed to a language's sentence already spelled, as every
 * number in the interface is; the minus is U+2212.
 *
 * <p>Promoted from the study generator so that the table a reader sees
 * and the table the evidence contract holds are the same bytes for
 * the same values.
 */
public final class SunTableFormat {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);

    private static final String MINUS = "−";

    private SunTableFormat() {
    }

    /** {@code 05h 58m 40.8s}: hours, minutes and seconds to 0.1 s. */
    public static String hms(SkyPosition p) {
        double hours = p.raDegrees() / 15.0;
        int h = (int) hours;
        double m = (hours - h) * 60.0;
        int mm = (int) m;
        double s = (m - mm) * 60.0;
        s = Math.round(s * 10.0) / 10.0;
        if (s >= 60.0) {
            s -= 60.0;
            mm++;
        }
        if (mm >= 60) {
            mm -= 60;
            h = (h + 1) % 24;
        }
        return String.format(Locale.ROOT, "%02dh %02dm %04.1fs", h, mm, s);
    }

    /** {@code +23° 26′ 02″}: degrees, minutes and seconds, sign always shown. */
    public static String dms(SkyPosition p) {
        double dec = p.decDegrees();
        String sign = dec < 0 ? MINUS : "+";
        double a = Math.abs(dec);
        int d = (int) a;
        double m = (a - d) * 60.0;
        int mm = (int) m;
        long s = Math.round((m - mm) * 60.0);
        if (s >= 60) {
            s -= 60;
            mm++;
        }
        if (mm >= 60) {
            mm -= 60;
            d++;
        }
        return String.format(Locale.ROOT, "%s%d° %02d′ %02d″",
                sign, d, mm, s);
    }

    /** {@code 150.56°}: a bearing or a longitude to a hundredth. */
    public static String degrees(double degrees) {
        return String.format(Locale.ROOT, "%.2f°", degrees)
                .replace("-", MINUS);
    }

    /**
     * {@code −12.34° (below the horizon)}: the number is kept whatever
     * its sign, and the status - a language's own words - is appended
     * when the Sun is below the horizon.
     */
    public static String altitude(double degrees, String belowTheHorizon) {
        return altitude(degrees(degrees), degrees, belowTheHorizon);
    }

    /** The same, over a number already spelled for the reader. */
    public static String altitude(String spelled, double degrees,
                                  String belowTheHorizon) {
        return degrees < 0 ? spelled + " " + belowTheHorizon : spelled;
    }

    /** {@code 1.016165}: astronomical units to six decimals. */
    public static String astronomicalUnits(double au) {
        return String.format(Locale.ROOT, "%.6f", au);
    }

    /** {@code 152.016}: millions of kilometres to three decimals. */
    public static String millionKilometres(double km) {
        return String.format(Locale.ROOT, "%.3f", km / 1e6);
    }

    /** {@code 31′ 27.9″}: arcminutes and arcseconds to 0.1″. */
    public static String minutesSeconds(double arcseconds) {
        int m = (int) (arcseconds / 60.0);
        double s = arcseconds - m * 60.0;
        s = Math.round(s * 10.0) / 10.0;
        if (s >= 60.0) {
            s -= 60.0;
            m++;
        }
        return String.format(Locale.ROOT, "%d′ %04.1f″", m, s);
    }

    /**
     * A {@code Locale.ROOT} number respelled with a language's decimal
     * separator - {@code 51.01°} to {@code 51,01°} - for what a reader
     * sees (issue #400, owner ruling). Never applied to what is parsed
     * back: the range fields, the fixtures and the study keep the point.
     */
    public static String decimal(String rootNumber, String separator) {
        return ".".equals(separator) ? rootNumber
                : rootNumber.replace(".", separator);
    }

    /** {@code 2026-06-21 08:24}: UTC, rounded to the minute. */
    public static String minute(Instant instant) {
        return MINUTE.format(instant.plusSeconds(30)
                .truncatedTo(ChronoUnit.MINUTES));
    }
}
