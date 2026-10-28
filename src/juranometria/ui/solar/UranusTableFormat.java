package juranometria.ui.solar;

import java.util.Locale;

/**
 * The rounding of the Uranus table (issue #635, the card ruled on #633,
 * U-9), in {@code Locale.ROOT}, beside {@link JovianTableFormat} for what
 * Uranus's table shares with Jupiter's, Saturn's and Mars's. Words - a
 * pole, a side, a state, a moon's name - are a language's and are not here.
 */
public final class UranusTableFormat {

    private UranusTableFormat() {
    }

    /** {@code 19.4°}: the tilt's size to a tenth of a degree; its sign names the pole (Mars's). */
    public static String tilt(double degrees) {
        return String.format(Locale.ROOT, "%.1f°", Math.abs(degrees));
    }

    /** {@code 2.83°}: the phase angle, Sun-Uranus-observer, to a hundredth of a degree (U-5). */
    public static String phaseAngle(double degrees) {
        return String.format(Locale.ROOT, "%.2f°", degrees);
    }
}
