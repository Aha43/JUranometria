package juranometria.ui.solar;

import java.util.Locale;

/**
 * The rounding of the Neptune table (issue #656, the card ruled on #654,
 * N-9), in {@code Locale.ROOT}, beside {@link JovianTableFormat} for what
 * Neptune's table shares with Jupiter's, Saturn's and Mars's. Words - a
 * pole, a side, a state, a moon's name - are a language's and are not here.
 */
public final class NeptuneTableFormat {

    private NeptuneTableFormat() {
    }

    /** {@code 19.4°}: the tilt's size to a tenth of a degree; its sign names the pole (Mars's). */
    public static String tilt(double degrees) {
        return String.format(Locale.ROOT, "%.1f°", Math.abs(degrees));
    }

    /** {@code 2.83°}: the phase angle, Sun-Neptune-observer, to a hundredth of a degree (N-5). */
    public static String phaseAngle(double degrees) {
        return String.format(Locale.ROOT, "%.2f°", degrees);
    }
}
