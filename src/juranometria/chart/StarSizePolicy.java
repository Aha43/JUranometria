package juranometria.chart;

/**
 * The replaceable magnitude-to-radius policy from docs/chart-conventions.md:
 *
 * <pre>radius = minimum + scale * (limitMagnitude - magnitude) ^ exponent</pre>
 *
 * clamped to a maximum so bright stars do not dominate the page. The right
 * relationship is a cartographic judgement, not a photometric claim; tune
 * the parameters freely.
 */
public record StarSizePolicy(double minimumRadiusPx, double maximumRadiusPx,
                             double scalePx, double exponent, double limitMagnitude) {

    /**
     * Starting values for an 8-degree finder chart around 900 px wide with
     * the fixture's V < 8.0 stellar depth.
     */
    public static final StarSizePolicy DEFAULT =
            new StarSizePolicy(0.7, 5.0, 1.0, 0.7, 8.5);

    public StarSizePolicy {
        if (!(minimumRadiusPx > 0.0) || !(maximumRadiusPx >= minimumRadiusPx)) {
            throw new IllegalArgumentException("radii must satisfy 0 < minimum <= maximum: "
                    + minimumRadiusPx + ", " + maximumRadiusPx);
        }
        if (!(scalePx > 0.0)) {
            throw new IllegalArgumentException("scale must be positive: " + scalePx);
        }
        if (!(exponent > 0.0 && exponent <= 1.0)) {
            throw new IllegalArgumentException("exponent must be in (0, 1]: " + exponent);
        }
        if (!Double.isFinite(limitMagnitude)) {
            throw new IllegalArgumentException("limit magnitude must be finite: " + limitMagnitude);
        }
    }

    /** Mark radius in pixels for a star of the given magnitude. */
    public double radiusFor(double magnitude) {
        if (!Double.isFinite(magnitude)) {
            throw new IllegalArgumentException("magnitude must be finite: " + magnitude);
        }
        double brightness = Math.max(0.0, limitMagnitude - magnitude);
        double radius = minimumRadiusPx + scalePx * Math.pow(brightness, exponent);
        return Math.min(maximumRadiusPx, radius);
    }
}
