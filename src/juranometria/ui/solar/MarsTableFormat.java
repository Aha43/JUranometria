package juranometria.ui.solar;

import java.util.Locale;

/**
 * The rounding of the Mars table (issue #603, the card ruled on #601,
 * M-9), in {@code Locale.ROOT}, beside {@link JovianTableFormat} for what
 * Mars's table shares with Jupiter's and Saturn's. Words - a pole, a side,
 * a state, a moon's name - are a language's and are not here.
 */
public final class MarsTableFormat {

    private MarsTableFormat() {
    }

    /** {@code 19.4°}: the tilt's size to a tenth of a degree; its sign names the pole. */
    public static String tilt(double degrees) {
        return String.format(Locale.ROOT, "%.1f°", Math.abs(degrees));
    }
}
