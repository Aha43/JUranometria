package juranometria.project;

import java.util.Optional;

/**
 * One visible run of a projected curve, and what it takes to draw it
 * (issue #298).
 *
 * <p>Numbers, not a {@code Shape}: this package draws nothing and is
 * held to that by a scan of its compiled classes. What turns a run
 * into ink is the chart's, which is also the older rule - a module
 * says where, the projection says where that lands, and the chart
 * decides what it looks like.
 *
 * <p>A run is a straight segment or an arc of an ellipse, and a
 * circle is the arc whose two radii are equal. The
 * <strong>ends</strong> are what a label hangs on, and they are
 * absent when the run closes: a great circle wholly inside the paper
 * has none, and the rule that names a line where it leaves the page
 * has nothing to hold.
 */
public sealed interface CurveRun {

    /** Where the run begins, or empty when it closes on itself. */
    Optional<PixelPoint> from();

    /** Where it ends, or empty for the same reason. */
    Optional<PixelPoint> to();

    /** Whether this run has no ends at all. */
    default boolean closed() {
        return from().isEmpty();
    }

    /** A straight run: two points and the line between them. */
    record Segment(PixelPoint start, PixelPoint end) implements CurveRun {

        @Override
        public Optional<PixelPoint> from() {
            return Optional.of(start);
        }

        @Override
        public Optional<PixelPoint> to() {
            return Optional.of(end);
        }
    }

    /**
     * An arc of an ellipse, in page units, measured from the
     * positive x axis towards positive y - the page's own sense,
     * which runs clockwise on a screen because y runs down.
     *
     * <p>{@code radiusAlong} lies along the tilt and
     * {@code radiusAcross} square to it; equal radii are a circle,
     * and a span of a whole turn is a closed curve with no ends.
     */
    record Arc(double centreX, double centreY, double radiusAlong,
               double radiusAcross, double tiltRadians,
               double startRadians, double spanRadians,
               PixelPoint start, PixelPoint end) implements CurveRun {

        @Override
        public Optional<PixelPoint> from() {
            return Optional.ofNullable(start);
        }

        @Override
        public Optional<PixelPoint> to() {
            return Optional.ofNullable(end);
        }

        /** Whether the two radii are the same: a circular arc. */
        public boolean circular() {
            return radiusAlong == radiusAcross;
        }
    }
}
