package juranometria.bookmark;

import java.util.Set;

/**
 * The geometric view a bookmark saves: the centre (ICRS/J2000 degrees)
 * and the field width, one of the atlas's steps. The projection and the
 * orientation are not stored: the atlas derives the projection from the
 * field and draws celestial north up, east left on every page
 * (docs/decisions/bookmarks.md, section 2).
 */
public record SavedView(double raDegrees, double decDegrees, double fieldWidthDegrees) {

    /** The field steps the atlas draws: ChartViewState's ladder and its closer fields. */
    public static final Set<Double> FIELD_STEPS = Set.of(180.0, 120.0, 90.0, 60.0, 42.0,
            36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0, 0.5, 0.25);

    public SavedView {
        if (!(raDegrees >= 0 && raDegrees < 360) || !(decDegrees >= -90 && decDegrees <= 90)) {
            throw new IllegalArgumentException("a centre on the sky: " + raDegrees + ", " + decDegrees);
        }
        if (!FIELD_STEPS.contains(fieldWidthDegrees)) {
            throw new IllegalArgumentException("a field the atlas draws: " + fieldWidthDegrees);
        }
    }
}
