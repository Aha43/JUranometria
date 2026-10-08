package juranometria.ui.solar;

import java.util.Locale;

/**
 * The rounding of the Jupiter table (issue #474, the mock-ups ruled on
 * #472), in {@code Locale.ROOT}, beside {@link SunTableFormat} for what
 * the tables share. Words - a side, a state, a moon's name - are a
 * language's and are not here.
 */
public final class JovianTableFormat {

    private JovianTableFormat() {
    }

    /** {@code 18.1°}: a height, a bearing or a position angle to a tenth. */
    public static String tenthDegree(double degrees) {
        return String.format(Locale.ROOT, "%.1f°", degrees).replace('-', '\u2212');
    }

    /** {@code 4.869 AU}: astronomical units to three decimals. */
    public static String astronomicalUnits(double au) {
        return String.format(Locale.ROOT, "%.3f AU", au);
    }

    /** {@code 16.8″}: arcseconds to a tenth. */
    public static String arcseconds(double arcseconds) {
        return String.format(Locale.ROOT, "%.1f″", arcseconds);
    }

    /** {@code 40.5″ / 37.9″}: the equatorial and polar diameters. */
    public static String diameters(double equatorial, double polar) {
        return arcseconds(equatorial) + " / " + arcseconds(polar);
    }

    /** {@code 15.8″ e}: an offset's magnitude with its side's letter. */
    public static String offset(double arcseconds, String letter) {
        return arcseconds(Math.abs(arcseconds)) + " " + letter;
    }

    /** {@code 0.83}: Jupiter radii to a hundredth. */
    public static String radii(double radii) {
        return String.format(Locale.ROOT, "%.2f", radii);
    }
}
