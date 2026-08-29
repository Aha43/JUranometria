package juranometria.chart;

import java.util.Locale;

/** Formal chart notation for sky coordinates, locale-independent. */
public final class SkyFormat {

    private SkyFormat() {
    }

    /** Right ascension as hours and decimal minutes, e.g. {@code 0h 42.7m}. */
    public static String formatRa(double raDegrees) {
        double hours = raDegrees / 15.0;
        int wholeHours = (int) hours;
        double minutes = (hours - wholeHours) * 60.0;
        return String.format(Locale.ROOT, "%dh %04.1fm", wholeHours, minutes);
    }

    /** Declination as signed degrees and arcminutes, e.g. {@code +41° 16′}. */
    public static String formatDec(double decDegrees) {
        String sign = decDegrees < 0 ? "−" : "+";
        double absolute = Math.abs(decDegrees);
        int wholeDegrees = (int) absolute;
        int arcminutes = (int) Math.round((absolute - wholeDegrees) * 60.0);
        if (arcminutes == 60) {
            wholeDegrees++;
            arcminutes = 0;
        }
        return String.format(Locale.ROOT, "%s%d° %02d′", sign, wholeDegrees, arcminutes);
    }
}
