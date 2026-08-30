package juranometria.chart;

/**
 * An immutable catalogue star: an identifier, an ICRS position, a
 * brightness value, and - when the bundled identity pack knows it -
 * a structured {@link StarIdentity}. The fixture supplies V
 * magnitudes; the field is a generic brightness, not a photometric
 * claim. A null identity is honestly unknown.
 */
public record Star(String id, SkyPosition position, double magnitude,
                   StarIdentity identity) {

    /** A star the identity pack does not know. */
    public Star(String id, SkyPosition position, double magnitude) {
        this(id, position, magnitude, null);
    }

    public Star {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("star id must not be blank");
        }
        if (position == null) {
            throw new IllegalArgumentException("star position must not be null");
        }
        if (!Double.isFinite(magnitude)) {
            throw new IllegalArgumentException("star magnitude must be finite: " + magnitude);
        }
    }
}
