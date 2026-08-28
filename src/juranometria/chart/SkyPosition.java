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
}
