package juranometria.chart;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * A field width as the atlas prints it (#543, part of #481).
 *
 * <p>The ladder's fields are whole degrees and keep the forms they have
 * always printed in - one decimal in the title block and the spoken
 * description ({@code 8.0}), none in the toolbar, the export's
 * description and its file name ({@code 8}) - so no released page,
 * sheet or sentence moves. A closer field prints to its own precision
 * in every one of them: {@code 0.5} and {@code 0.25}, never the
 * {@code 0.3} or {@code 0} that the ladder's formats would round it to
 * (docs/decisions/mercury-cartography.md, ruling 5: "titles show
 * sufficient precision").
 *
 * <p>Always {@link Locale#ROOT}, as the formats it replaces were: the
 * number is the same in every interface language.
 */
public final class FieldWidthText {

    private FieldWidthText() {
    }

    /** The field with no trailing zeros: {@code 8}, {@code 0.5}, {@code 0.25}. */
    public static String plain(double fieldWidthDegrees) {
        return BigDecimal.valueOf(fieldWidthDegrees).stripTrailingZeros()
                .toPlainString();
    }

    /**
     * The title block's and the spoken description's form: one decimal
     * on the ladder ({@code 8.0}, {@code 1.0}), the closer field's own
     * precision below it.
     */
    public static String title(double fieldWidthDegrees) {
        return closer(fieldWidthDegrees)
                ? plain(fieldWidthDegrees)
                : String.format(Locale.ROOT, "%.1f", fieldWidthDegrees);
    }

    /**
     * The toolbar's, the export description's and the file name's form:
     * whole degrees on the ladder, the closer field's own precision
     * below it.
     */
    public static String whole(double fieldWidthDegrees) {
        return closer(fieldWidthDegrees)
                ? plain(fieldWidthDegrees)
                : String.format(Locale.ROOT, "%.0f", fieldWidthDegrees);
    }

    private static boolean closer(double fieldWidthDegrees) {
        return fieldWidthDegrees < ChartViewState.normalMinimumFieldDegrees();
    }
}
