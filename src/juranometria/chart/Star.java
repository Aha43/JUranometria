package juranometria.chart;

/**
 * An immutable catalogue star: an identifier, an ICRS position, and a
 * brightness value. The fixture supplies V magnitudes; the field is a
 * generic brightness, not a photometric claim.
 */
public record Star(String id, SkyPosition position, double magnitude) {

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
