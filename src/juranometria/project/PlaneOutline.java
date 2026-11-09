package juranometria.project;

/**
 * Where a projection's sky ends on its own plane (#687, architecture A
 * of docs/decisions/all-sky-projection.md).
 *
 * <p>Until the whole-sky view the atlas had two answers, and one
 * number carried both: {@link Projection#visiblePlaneRadius} is
 * infinite for a chart plane that goes on for ever and the radius of
 * the limb for a hemisphere. A whole-sky map has a third shape - an
 * ellipse twice as wide as it is high - which no radius can say, and a
 * disc of its half-width would clip the top and bottom of the sky
 * away. So the shape is stated as a shape, and the radius keeps its
 * older meaning of how far ink may reach.
 *
 * <p>Plane units, centred on the page's plane origin, {@code xi} east
 * and {@code eta} north as {@link PlanePoint} has them. Nothing here is
 * pixels and nothing here draws: a page turns it into a clip.
 */
public sealed interface PlaneOutline {

    /** Whether the sky stops somewhere on the plane. */
    boolean bounded();

    /** Whether a plane point is sky (on the edge counts). */
    boolean contains(double xi, double eta);

    /**
     * The widest half-extent of the sky across the plane, or infinite:
     * the number {@link Projection#visiblePlaneRadius} has always
     * answered, read from the shape.
     */
    double halfWidth();

    /** The tallest half-extent, or infinite; a disc's is its radius. */
    double halfHeight();

    /** A chart plane: sky to the edge of any paper. */
    record Unbounded() implements PlaneOutline {

        @Override
        public boolean bounded() {
            return false;
        }

        @Override
        public boolean contains(double xi, double eta) {
            return true;
        }

        @Override
        public double halfWidth() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double halfHeight() {
            return Double.POSITIVE_INFINITY;
        }
    }

    /** A hemisphere's limb: the one shape the atlas had until #687. */
    record Disc(double radius) implements PlaneOutline {

        public Disc {
            if (!Double.isFinite(radius) || radius <= 0.0) {
                throw new IllegalArgumentException(
                        "a disc has a finite positive radius: " + radius);
            }
        }

        @Override
        public boolean bounded() {
            return true;
        }

        @Override
        public boolean contains(double xi, double eta) {
            return Math.hypot(xi, eta) <= radius;
        }

        @Override
        public double halfWidth() {
            return radius;
        }

        @Override
        public double halfHeight() {
            return radius;
        }
    }

    /**
     * An ellipse about the plane origin: the whole sky under the
     * Hammer projection, whose outline is {@code x^2/8 + y^2/2 = 1}
     * (half-width {@code 2 sqrt 2}, half-height {@code sqrt 2}).
     */
    record Ellipse(double halfWidth, double halfHeight)
            implements PlaneOutline {

        public Ellipse {
            if (!Double.isFinite(halfWidth) || !Double.isFinite(halfHeight)
                    || halfWidth <= 0.0 || halfHeight <= 0.0) {
                throw new IllegalArgumentException(
                        "an ellipse has two finite positive half-extents: "
                                + halfWidth + " by " + halfHeight);
            }
        }

        @Override
        public boolean bounded() {
            return true;
        }

        @Override
        public boolean contains(double xi, double eta) {
            double x = xi / halfWidth;
            double y = eta / halfHeight;
            return x * x + y * y <= 1.0;
        }
    }

    /**
     * The outline a radius alone describes: what every projection
     * answered before a shape could be stated, and still the default.
     */
    static PlaneOutline of(double visiblePlaneRadius) {
        return Double.isFinite(visiblePlaneRadius)
                ? new Disc(visiblePlaneRadius) : new Unbounded();
    }
}
