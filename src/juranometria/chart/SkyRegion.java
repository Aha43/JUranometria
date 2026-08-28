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
        double dec1 = Math.toRadians(centre.decDegrees());
        double dec2 = Math.toRadians(position.decDegrees());
        double halfDeltaDec = (dec2 - dec1) / 2.0;
        double halfDeltaRa =
                Math.toRadians(position.raDegrees() - centre.raDegrees()) / 2.0;

        // Haversine great-circle separation; robust across the RA 0/360 wrap.
        double h = Math.sin(halfDeltaDec) * Math.sin(halfDeltaDec)
                + Math.cos(dec1) * Math.cos(dec2)
                * Math.sin(halfDeltaRa) * Math.sin(halfDeltaRa);
        double separationDegrees = Math.toDegrees(2.0 * Math.asin(Math.min(1.0, Math.sqrt(h))));
        return separationDegrees <= radiusDegrees;
    }
}
