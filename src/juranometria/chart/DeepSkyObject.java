package juranometria.chart;

import java.util.List;

/**
 * An immutable deep-sky object with the minimum fields from
 * docs/catalogues.md: identifiers, type, position, apparent dimensions,
 * position angle, brightness, and a label priority (1 is most important).
 *
 * The position angle is measured in degrees east of north in [0, 180).
 * A NaN magnitude means the source records no photometry for the object;
 * dimensions and angle are always concrete because the renderer needs
 * them, with the loader supplying documented display minimums where the
 * source records none.
 */
public record DeepSkyObject(String id, List<String> aliases, DsoType type,
                            SkyPosition position, double majorAxisArcmin,
                            double minorAxisArcmin, double positionAngleDegrees,
                            double magnitude, int labelPriority) {

    public DeepSkyObject {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("DSO id must not be blank");
        }
        if (type == null || position == null || aliases == null) {
            throw new IllegalArgumentException("DSO type, position, and aliases are required");
        }
        aliases = List.copyOf(aliases);
        if (!(majorAxisArcmin > 0.0) || !(minorAxisArcmin > 0.0)
                || minorAxisArcmin > majorAxisArcmin) {
            throw new IllegalArgumentException("DSO axes must be positive with minor <= major: "
                    + majorAxisArcmin + " x " + minorAxisArcmin);
        }
        if (!(positionAngleDegrees >= 0.0 && positionAngleDegrees < 180.0)) {
            throw new IllegalArgumentException(
                    "position angle must be in [0, 180) degrees: " + positionAngleDegrees);
        }
        if (!Double.isFinite(magnitude) && !Double.isNaN(magnitude)) {
            throw new IllegalArgumentException(
                    "DSO magnitude must be finite or NaN for unknown: " + magnitude);
        }
        if (labelPriority < 1) {
            throw new IllegalArgumentException("label priority must be at least 1: " + labelPriority);
        }
    }
}
