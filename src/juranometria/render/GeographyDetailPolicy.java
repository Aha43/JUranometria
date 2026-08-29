package juranometria.render;

/**
 * The constellation-geography scale policy of
 * docs/decisions/constellation-geography.md, in one testable place:
 * line figures and names draw at 12 degrees and wider, boundaries at
 * 18 degrees and wider, and nothing draws at the released 1-8 degree
 * fields - those pages keep exactly their shipped ink, and the M31
 * reference stays byte-identical. Consulted by scene assembly (which
 * skips the queries entirely below the thresholds) and again by the
 * renderer, so even a hand-built scene cannot draw geography at a
 * field the decision keeps clean.
 */
public final class GeographyDetailPolicy {

    /** The narrowest field at which figures and names draw. */
    public static final double NARROWEST_FIGURE_FIELD_DEGREES = 12.0;

    /** The narrowest field at which boundaries draw. */
    public static final double NARROWEST_BOUNDARY_FIELD_DEGREES = 18.0;

    private final double fieldWidthDegrees;

    public GeographyDetailPolicy(double fieldWidthDegrees) {
        if (!(fieldWidthDegrees > 0.0) || !Double.isFinite(fieldWidthDegrees)) {
            throw new IllegalArgumentException(
                    "field width must be positive and finite: "
                            + fieldWidthDegrees);
        }
        this.fieldWidthDegrees = fieldWidthDegrees;
    }

    public boolean figuresDrawn() {
        return fieldWidthDegrees >= NARROWEST_FIGURE_FIELD_DEGREES;
    }

    public boolean namesDrawn() {
        return fieldWidthDegrees >= NARROWEST_FIGURE_FIELD_DEGREES;
    }

    public boolean boundariesDrawn() {
        return fieldWidthDegrees >= NARROWEST_BOUNDARY_FIELD_DEGREES;
    }
}
