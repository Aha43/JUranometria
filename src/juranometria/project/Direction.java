package juranometria.project;

/**
 * A direction, in the frame of a chart centre.
 *
 * <p>How far it lies <strong>along</strong> the centre - the cosine of
 * the angle between them - and how far <strong>east</strong> and
 * <strong>north</strong>. Every azimuthal projection is made of these
 * three numbers and differs only in what it does with them, so a
 * projection that computed its own would be a projection that could
 * disagree with the others about where a position is.
 *
 * <p>The transverse part, {@code hypot(east, north)}, is the sine of
 * that angle, measured rather than derived from the cosine. That
 * matters: inverting a cosine is ill conditioned at both ends of its
 * range, and a study that did it discarded a disc of sky three
 * milliarcseconds across around the point opposite the centre
 * (docs/decisions/overview-projection.md).
 */
public record Direction(double along, double east, double north) {

    /** How far off the centre this direction lies, as a sine. */
    public double transverse() {
        return Math.hypot(east, north);
    }

    /** Whether this direction is the centre itself or opposite it. */
    public boolean onTheAxis() {
        return transverse() == 0.0;
    }
}
