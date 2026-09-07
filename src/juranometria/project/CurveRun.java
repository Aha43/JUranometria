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

        public Segment {
            if (start == null || end == null) {
                throw new IllegalArgumentException(
                        "a straight run has both of its ends");
            }
            if (!Double.isFinite(start.x()) || !Double.isFinite(start.y())
                    || !Double.isFinite(end.x())
                    || !Double.isFinite(end.y())) {
                throw new IllegalArgumentException(
                        "a run ends at pixels: " + start + " to " + end);
            }
        }

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

        /** A whole turn: the span of a run that closes on itself. */
        public static final double WHOLE_TURN = 2.0 * Math.PI;

        public Arc {
            if (!Double.isFinite(centreX) || !Double.isFinite(centreY)
                    || !Double.isFinite(tiltRadians)
                    || !Double.isFinite(radiusAlong)
                    || !Double.isFinite(radiusAcross)
                    || radiusAlong <= 0.0 || radiusAcross <= 0.0) {
                throw new IllegalArgumentException(
                        "an arc turns about a centre at two radii: "
                                + centreX + "," + centreY + " "
                                + radiusAlong + " by " + radiusAcross);
            }
            // A span of nothing is not a short run, it is no run: the
            // clipping never produces one, and a renderer handed one
            // draws an invisible mark where a reference line should
            // be. More than a whole turn is a run drawn twice.
            if (!Double.isFinite(startRadians)
                    || !Double.isFinite(spanRadians)
                    || spanRadians <= 0.0 || spanRadians > WHOLE_TURN) {
                throw new IllegalArgumentException(
                        "an arc runs from an angle through a span within"
                                + " one turn: " + startRadians + " through "
                                + spanRadians);
            }
            // The ends are what a label hangs on, and the rule that
            // hangs it reads one of them. One end present and the
            // other absent is a run no rule can name, and a closed
            // run with ends is one that would be named at a join it
            // does not have.
            if ((start == null) != (end == null)) {
                throw new IllegalArgumentException(
                        "an arc has both ends or neither: " + start
                                + " to " + end);
            }
            if (start == null && spanRadians != WHOLE_TURN) {
                throw new IllegalArgumentException(
                        "a run with no ends is one that closes, and this"
                                + " spans " + spanRadians);
            }
        }

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
