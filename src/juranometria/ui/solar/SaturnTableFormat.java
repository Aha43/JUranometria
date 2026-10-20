package juranometria.ui.solar;

import java.util.Locale;

/**
 * The rounding of the Saturn table (issue #576, the mock-ups ruled on
 * #574), in {@code Locale.ROOT}, beside {@link JovianTableFormat} for
 * what Saturn's table shares with Jupiter's - a tenth of a degree, an
 * astronomical unit to three decimals, arcseconds to a tenth, Saturn
 * radii to a hundredth. Words - a face, a side, a state, a moon's name -
 * are a language's and are not here.
 */
public final class SaturnTableFormat {

    private SaturnTableFormat() {
    }

    /** {@code 7.43°}: a ring opening's size to a hundredth of a degree, its sign the face's. */
    public static String opening(double degrees) {
        return String.format(Locale.ROOT, "%.2f°", Math.abs(degrees));
    }

    /**
     * Whether an opening reads as edge-on: exactly when its size, shown to
     * a hundredth of a degree, is zero - no face can be named from it.
     */
    public static boolean edgeOn(double degrees) {
        return opening(degrees).equals("0.00°");
    }

    /** {@code 44.7″ × 5.8″}: the rings' apparent major and minor axes. */
    public static String ringSize(double majorArcseconds, double minorArcseconds) {
        return JovianTableFormat.arcseconds(majorArcseconds) + " × "
                + JovianTableFormat.arcseconds(minorArcseconds);
    }
}
