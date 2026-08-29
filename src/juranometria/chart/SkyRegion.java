package juranometria.chart;

/**
 * A circular sky region for bounded catalogue queries: a centre and an
 * angular radius in degrees.
 */
public record SkyRegion(SkyPosition centre, double radiusDegrees) {

    public SkyRegion {
        if (centre == null) {
            throw new IllegalArgumentException("region centre must not be null");
        }
        if (!(radiusDegrees > 0.0 && radiusDegrees <= 180.0)) {
            throw new IllegalArgumentException(
                    "region radius must be in (0, 180] degrees: " + radiusDegrees);
        }
    }

    /** True when the position lies within the region's angular radius. */
    public boolean contains(SkyPosition position) {
        return centre.separationDegrees(position) <= radiusDegrees;
    }
}
