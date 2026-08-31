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
                            double magnitude, int labelPriority,
                            Recorded recorded) {

    /**
     * What the source actually recorded, beside the display values
     * above (Sprint 19, issue #169).
     *
     * <p>The renderer needs concrete dimensions for every object, so
     * the loader supplies documented minimums where the catalogue is
     * silent. That substitution used to be the only truth the
     * application kept, and it is not a truth: measured over the
     * bundled pack, <strong>19.4%</strong> of rows record no position
     * angle and <strong>68.1%</strong> record no V magnitude, of
     * which most carry only a B magnitude. Anything reporting those
     * substituted values as catalogue facts would state a size nobody
     * measured, a position angle of exactly zero for a fifth of the
     * sky, and a blue magnitude labelled visual for the majority of
     * it.
     *
     * <p>A null field here means the source recorded nothing. The
     * display values above are never null and never change.
     */
    public record Recorded(Double majorAxisArcmin, Double minorAxisArcmin,
                           Double positionAngleDegrees, Band band) {

        /** Which photometric band {@code magnitude} belongs to. */
        public enum Band {
            /** A visual magnitude, as recorded. */
            VISUAL,
            /** A blue magnitude, standing in where no V was recorded. */
            BLUE,
            /** No photometry at all; the magnitude is NaN. */
            NONE
        }

        /** A source that recorded nothing at all about an object. */
        public static final Recorded NOTHING =
                new Recorded(null, null, null, Band.NONE);

        public Recorded {
            if (band == null) {
                throw new IllegalArgumentException("band is required");
            }
        }

        /** Whether the source measured this object's extent. */
        public boolean hasSize() {
            return majorAxisArcmin != null;
        }

        /** Whether the source measured an orientation. */
        public boolean hasPositionAngle() {
            return positionAngleDegrees != null;
        }
    }

    /**
     * An object stated only by its display values - fixtures and
     * studies that care about geometry, never the loader.
     *
     * <p>Its dimensions are taken as given rather than as recorded,
     * and a magnitude supplied here is read as a visual magnitude,
     * which is the convention every fixture in this repository has
     * always used. The loader states all of it explicitly.
     */
    public DeepSkyObject(String id, List<String> aliases, DsoType type,
                         SkyPosition position, double majorAxisArcmin,
                         double minorAxisArcmin, double positionAngleDegrees,
                         double magnitude, int labelPriority) {
        this(id, aliases, type, position, majorAxisArcmin, minorAxisArcmin,
                positionAngleDegrees, magnitude, labelPriority,
                new Recorded(null, null, null,
                        Double.isNaN(magnitude) ? Recorded.Band.NONE
                                : Recorded.Band.VISUAL));
    }

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
        if (recorded == null) {
            throw new IllegalArgumentException("recorded facts are required;"
                    + " use Recorded.NOTHING when the source is silent");
        }
        if (Double.isNaN(magnitude)
                != (recorded.band() == Recorded.Band.NONE)) {
            throw new IllegalArgumentException(
                    "a magnitude and its band must agree: " + magnitude
                            + " with " + recorded.band());
        }
    }
}
