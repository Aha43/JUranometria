package juranometria.meteor;

import java.util.List;

import juranometria.chart.SkyPosition;

/**
 * One meteor shower as the bundled pack states it (#716): its identity
 * from the IAU Meteor Data Center, and every number about its activity
 * and radiant from one named edition of the International Meteor
 * Organization's Meteor Shower Calendar.
 *
 * <p>Printed values are kept as printed - the solar longitude of the
 * maximum with the decimals the calendar gives it, the rate with its
 * {@code +} - because how finely a value is printed is part of what is
 * known (R-7 of {@code docs/decisions/meteor-showers.md}). The two
 * activity longitudes and each radiant row's longitude are not the
 * calendar's: the importer computed them from the calendar's dates with
 * the atlas's own Sun, and the pack says so.
 *
 * @param code the IAU three-letter code
 * @param iauNumber the IAU numeral code
 * @param iauName the IAU's name for the shower
 * @param parentBody the parent body as the IAU list states it, or empty
 * @param activityStart the calendar's first date of activity, as printed
 * @param activityEnd the calendar's last date of activity, as printed
 * @param startLongitude the Sun's J2000 longitude at 00:00 UTC of the first date
 * @param endLongitude the Sun's J2000 longitude at 24:00 UTC of the last date
 * @param maximumDate the calendar's date of maximum in the edition's year, as printed
 * @param maximumLongitudeText the solar longitude of the maximum, as printed
 * @param radiantAtMaximum the working list's radiant, J2000 assumed
 * @param vInfinity the atmospheric entry speed, km/s
 * @param populationIndex the population index, as printed
 * @param zhr the zenithal hourly rate, as printed
 * @param rows the radiant table's rows in the order the Earth meets them
 */
public record MeteorShower(String code, int iauNumber, String iauName, String parentBody,
                           String activityStart, String activityEnd, double startLongitude,
                           double endLongitude, String maximumDate, String maximumLongitudeText,
                           SkyPosition radiantAtMaximum, double vInfinity,
                           String populationIndex, String zhr, List<Row> rows) {

    /** The stem every shower identity starts with. */
    public static final String IDENTITY_STEM = "meteor-shower.";

    /** One printed row of the radiant table: a date and a whole-degree position. */
    public record Row(int month, int day, double solarLongitude, SkyPosition at) {
    }

    /** How finely the calendar prints the maximum's solar longitude (R-7). */
    public enum MaximumPrecision {
        /** A whole degree: about a day either way. */
        DATE,
        /** A tenth of a degree: an hour or two. */
        HOURS,
        /** A hundredth of a degree: about ten minutes. */
        TEN_MINUTES
    }

    public MeteorShower {
        if (code == null || code.length() != 3
                || !code.chars().allMatch(c -> c >= 'A' && c <= 'Z')) {
            throw new IllegalArgumentException("a shower has an IAU three-letter code: " + code);
        }
        if (iauName == null || iauName.isBlank() || radiantAtMaximum == null
                || maximumLongitudeText == null || zhr == null || populationIndex == null
                || activityStart == null || activityEnd == null || maximumDate == null) {
            throw new IllegalArgumentException("a shower states every listed value: " + code);
        }
        parentBody = parentBody == null ? "" : parentBody;
        rows = List.copyOf(rows);
        Double.parseDouble(maximumLongitudeText);
    }

    /** The chart identity: {@code meteor-shower.} and the IAU code (R-15). */
    public String identity() {
        return IDENTITY_STEM + code;
    }

    /** The solar longitude of the nominal maximum, degrees. */
    public double maximumLongitude() {
        return Double.parseDouble(maximumLongitudeText);
    }

    /** How finely the maximum is known, from the decimals the calendar prints. */
    public MaximumPrecision maximumPrecision() {
        int dot = maximumLongitudeText.indexOf('.');
        int decimals = dot < 0 ? 0 : maximumLongitudeText.length() - dot - 1;
        return decimals >= 2 ? MaximumPrecision.TEN_MINUTES
                : decimals == 1 ? MaximumPrecision.HOURS : MaximumPrecision.DATE;
    }

    /** The shower's code, if an identity is a shower's; null otherwise. */
    public static String codeOf(String identity) {
        return identity != null && identity.startsWith(IDENTITY_STEM)
                ? identity.substring(IDENTITY_STEM.length()) : null;
    }
}
