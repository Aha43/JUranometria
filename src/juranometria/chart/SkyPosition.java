package juranometria.chart;

/**
 * An ICRS/J2000 sky position.
 *
 * The domain model stores angles in decimal degrees: right ascension in
 * [0, 360) and declination in [-90, 90]. Values outside these ranges,
 * including NaN, are rejected at construction.
 */
public record SkyPosition(double raDegrees, double decDegrees) {

    public SkyPosition {
        if (!(raDegrees >= 0.0 && raDegrees < 360.0)) {
            throw new IllegalArgumentException(
                    "right ascension must be in [0, 360) degrees: " + raDegrees);
        }
        if (!(decDegrees >= -90.0 && decDegrees <= 90.0)) {
            throw new IllegalArgumentException(
                    "declination must be in [-90, 90] degrees: " + decDegrees);
        }
    }

    /** Great-circle separation to another position, in degrees. */
    public double separationDegrees(SkyPosition other) {
        double dec1 = Math.toRadians(decDegrees);
        double dec2 = Math.toRadians(other.decDegrees);
        double halfDeltaDec = (dec2 - dec1) / 2.0;
        double halfDeltaRa = Math.toRadians(other.raDegrees - raDegrees) / 2.0;

        // Haversine; robust across the RA 0/360 wrap.
        double h = Math.sin(halfDeltaDec) * Math.sin(halfDeltaDec)
                + Math.cos(dec1) * Math.cos(dec2)
                * Math.sin(halfDeltaRa) * Math.sin(halfDeltaRa);
        return Math.toDegrees(2.0 * Math.asin(Math.min(1.0, Math.sqrt(h))));
    }
}
