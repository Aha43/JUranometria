package juranometria.ui.solar;

import java.util.Locale;

import juranometria.solar.SolarSystemService.CompassPoint;

/**
 * The frozen rounding of the Moon table's own columns (issue #406,
 * M5 and M6; issue #407), in {@code Locale.ROOT}, beside
 * {@link SunTableFormat} for the columns the two tables share.
 *
 * <p>Distance to the kilometre with a space between thousands;
 * illuminated fraction to a tenth of a percent; elongation to a
 * tenth of a degree with the side's letter; the bright limb's position
 * angle to the degree. Words - the phase, the compass point, "not well-defined" -
 * are a language's and are not here: the study spells them in English
 * and the reader's table takes them from the language files.
 */
public final class MoonTableFormat {

    private MoonTableFormat() {
    }

    /** {@code 384 400 km}: to the kilometre, thousands spaced. */
    public static String kilometres(double km) {
        long rounded = Math.round(km);
        String digits = Long.toString(Math.abs(rounded));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                out.append(' ');
            }
            out.append(digits.charAt(i));
        }
        return (rounded < 0 ? "−" : "") + out + " km";
    }

    /** {@code 67.9 %}: a fraction in [0, 1] as a percentage to 0.1. */
    public static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.1f %%", fraction * 100.0);
    }

    /**
     * {@code 84.8° E}: the elongation to a tenth of a degree, with the
     * side's letter as the language spells it - E or W, Ø or V.
     */
    public static String elongation(double degrees, String sideLetter) {
        return String.format(Locale.ROOT, "%.1f° %s", degrees, sideLetter);
    }

    /** {@code 285°}: a position angle to the degree, 360 shown as 0. */
    public static String positionAngle(double degrees) {
        long rounded = Math.round(degrees) % 360;
        return rounded + "°";
    }

    /** The compass point's letters, as they are read in every language. */
    public static String letters(CompassPoint point) {
        return point.name();
    }
}
