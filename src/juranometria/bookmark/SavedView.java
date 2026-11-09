package juranometria.bookmark;

import java.util.Set;

/**
 * The geometric view a bookmark saves: the centre (ICRS/J2000 degrees)
 * and the field width, one of the atlas's steps. The projection and the
 * orientation are not stored: the atlas derives the projection from the
 * field and draws celestial north up, east left on every page
 * (docs/decisions/bookmarks.md, section 2).
 *
 * <p>Or the whole sky (#687, ruled R-11 on #686, option B-2): a view
 * with no centre and no field, because both are derived - the central
 * meridian from the observer's zenith at the bookmark's chart time and
 * place, which such a bookmark therefore keeps. Written as the one
 * optional member {@code "mode": "whole-sky"} in the version-1 schema,
 * so an older atlas keeps the record exactly as a record needing
 * attention and every ordinary bookmark is byte for byte what it was.
 */
public record SavedView(double raDegrees, double decDegrees, double fieldWidthDegrees,
                        boolean wholeSky) {

    /** The field steps the atlas draws: ChartViewState's ladder and its closer fields. */
    public static final Set<Double> FIELD_STEPS = Set.of(180.0, 120.0, 90.0, 60.0, 42.0,
            36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0, 0.5, 0.25);

    /** The stored token of a whole-sky view's mode. */
    public static final String WHOLE_SKY_MODE = "whole-sky";

    /** An ordinary view: a centre and a field. */
    public SavedView(double raDegrees, double decDegrees, double fieldWidthDegrees) {
        this(raDegrees, decDegrees, fieldWidthDegrees, false);
    }

    public SavedView {
        if (wholeSky) {
            // No centre, no field: held at one canonical spelling so two
            // whole-sky views are equal, and never written.
            if (raDegrees != 0.0 || decDegrees != 0.0
                    || fieldWidthDegrees != juranometria.chart.ChartProjection.WHOLE_SKY_DEGREES) {
                throw new IllegalArgumentException("a whole-sky view has no centre or field");
            }
        } else {
            if (!(raDegrees >= 0 && raDegrees < 360) || !(decDegrees >= -90 && decDegrees <= 90)) {
                throw new IllegalArgumentException("a centre on the sky: " + raDegrees + ", " + decDegrees);
            }
            if (!FIELD_STEPS.contains(fieldWidthDegrees)) {
                throw new IllegalArgumentException("a field the atlas draws: " + fieldWidthDegrees);
            }
        }
    }

    /** The whole sky at the bookmark's time and place. */
    public static SavedView wholeSkyView() {
        return new SavedView(0.0, 0.0, juranometria.chart.ChartProjection.WHOLE_SKY_DEGREES, true);
    }
}
