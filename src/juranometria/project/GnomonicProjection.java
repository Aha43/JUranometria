package juranometria.project;

import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Gnomonic (tangent-plane) projection of ICRS positions onto a chart
 * plane centred on a sky position.
 *
 * <p>The atlas's own projection, and the right one for the fields it
 * serves: it puts every great circle on a straight line, and every
 * telescope field where the telescope will find it. It pays for that
 * at the edges, and pays fast - at a 90-degree page corner it draws a
 * circle as an ellipse half as wide again as it is tall - which is
 * why Sprint 30 chose a second projection for wider views rather than
 * a wider field for this one (docs/decisions/overview-projection.md).
 *
 * <p>Positions 90 degrees or more from the centre have no
 * tangent-plane image and project to {@link Optional#empty()}.
 * Positions inside that hemisphere always project, even when they
 * fall outside a viewport; clipping is not a projection concern.
 */
public final class GnomonicProjection extends AzimuthalProjection {

    public GnomonicProjection(SkyPosition centre) {
        super(centre);
    }

    @Override
    public String name() {
        return "gnomonic";
    }

    /**
     * Projects a sky position onto the tangent plane.
     *
     * <p>Divided by the cosine directly, and never by way of the
     * angle. It is both better conditioned and the arithmetic the
     * released atlas has always done: every page from one degree to
     * forty-two is drawn by these two divisions and must stay drawn
     * by them.
     */
    @Override
    public Optional<PlanePoint> project(SkyPosition position) {
        Direction direction = frame().directionTo(position);
        if (direction.along() <= 0.0) {
            // The far hemisphere and its boundary, where the tangent
            // plane is infinitely far away. An exact condition, so it
            // is asked exactly.
            return Optional.empty();
        }
        return Optional.of(new PlanePoint(
                direction.east() / direction.along(),
                direction.north() / direction.along()));
    }

    @Override
    double angleAtRadius(double planeRadius) {
        return Math.atan(planeRadius);
    }

    @Override
    public double planeRadius(double angleDegrees) {
        return Math.tan(Math.toRadians(angleDegrees));
    }

    @Override
    public double limitDegrees() {
        return 90.0;
    }

    /**
     * Sixty degrees: a tangent plane degrades far from its centre,
     * and this is where the atlas has always stopped a page.
     *
     * <p>At that corner a degree is four times the size it is at the
     * centre, and a circle is drawn twice as wide as it is tall. The
     * number is the one the scene assembler has carried since the
     * beginning, unchanged and now stated where it belongs.
     */
    @Override
    public double usefulCornerDegrees() {
        return 60.0;
    }

    /**
     * Every gnomonic great circle is a straight line.
     *
     * <p>A point at angle {@code t} and position angle {@code f}
     * lands at {@code (tan t sin f, tan t cos f)}, so putting
     * {@code sin t / r = cos t} into {@code pole . point = 0} leaves
     * {@code cos t (a + b xi + c eta) = 0}, and the cosine is
     * positive everywhere this projection reaches.
     */
    @Override
    public Optional<PlaneConic> greatCircle(SkyPosition pole) {
        Direction direction = frame().directionTo(pole);
        if (direction.onTheAxis()) {
            // The circle ninety degrees from the centre, lying where
            // the tangent plane is infinitely far away. There is no
            // curve - not a line very far off.
            return Optional.empty();
        }
        return Optional.of(PlaneConic.line(direction.along(),
                direction.east(), direction.north()));
    }
}
