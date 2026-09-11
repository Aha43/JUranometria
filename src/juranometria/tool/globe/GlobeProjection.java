package juranometria.tool.globe;

import java.util.Optional;

import juranometria.chart.SkyPosition;
import juranometria.project.PlaneConic;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.tool.overview.Candidates;
import juranometria.tool.overview.StudyProjection;

/**
 * The orthographic hemisphere, as a projection the production
 * assembler and renderer can draw by (Sprint 32, issue #301).
 *
 * <p><strong>Study only, and temporary: issue #329 owns its removal</strong>,
 * and is where this geometry becomes production with the exactness
 * proofs a production projection carries.
 *
 * <p><strong>It adds no geometry.</strong> Every formula here is the
 * one Sprint 30 reviewed and measured: this delegates to
 * {@link Candidates#orthographic(SkyPosition)}, the same candidate the
 * overview study compared against the other two and whose round trip
 * it measured to 3.3e-13 degrees. Restating {@code r = sin(theta)} in
 * a second place would be inviting the two to disagree later, and a
 * gate that measured its own arithmetic rather than the reviewed
 * arithmetic would be measuring the wrong thing.
 *
 * <p>What it adds is the two answers the production interface asks for
 * and the study interface does not:
 *
 * <ul>
 *   <li>{@link #angleAtPlaneRadius(double)} - the inverse of the
 *       radius rule, {@code asin(r)}, which the reviewed candidate
 *       already computes internally for {@code unproject} but does not
 *       expose. Beyond the limb it is not a large angle, it is no
 *       angle: a radius past one is not a place.</li>
 *   <li>{@link #usefulCornerDegrees()} - where the page stops being
 *       worth reading. For a globe that is its own limb, because past
 *       ninety degrees there is no sky rather than degraded sky. The
 *       tangent plane answers 60 and the conformal one 120 for the
 *       opposite reason: they can draw further and it stops being
 *       worth it. <strong>How coarse the globe becomes on the way to
 *       that limb is a decision this gate owes, not a number
 *       settled here</strong> - Sprint 30 measured a page unit at a
 *       90-degree corner covering twelve arcminutes against four at
 *       the centre, and said a globe is a thing to look at rather
 *       than a thing to point at.</li>
 * </ul>
 */
public final class GlobeProjection implements Projection {

    private final StudyProjection reviewed;

    public GlobeProjection(SkyPosition centre) {
        this.reviewed = Candidates.orthographic(centre);
    }

    @Override
    public String name() {
        return reviewed.name();
    }

    @Override
    public SkyPosition centre() {
        return reviewed.centre();
    }

    @Override
    public Optional<PlanePoint> project(SkyPosition position) {
        return reviewed.project(position);
    }

    @Override
    public Optional<SkyPosition> unproject(PlanePoint point) {
        return reviewed.unproject(point);
    }

    @Override
    public double planeRadius(double angleDegrees) {
        return reviewed.planeRadius(angleDegrees);
    }

    @Override
    public double angleAtPlaneRadius(double planeRadius) {
        // The inverse of r = sin(theta), and the limb is on the globe:
        // asin(1) is ninety degrees exactly. Written as ">=" this
        // would refuse the one circle the projection draws best, which
        // is the mistake the reviewed candidate records having made.
        return planeRadius > 1.0 ? Double.NaN
                : Math.toDegrees(Math.asin(planeRadius));
    }

    @Override
    public double limitDegrees() {
        return reviewed.limitDegrees();
    }

    @Override
    public double usefulCornerDegrees() {
        return reviewed.limitDegrees();
    }

    @Override
    public double visiblePlaneRadius() {
        return reviewed.visiblePlaneRadius();
    }

    @Override
    public Optional<PlaneConic> greatCircle(SkyPosition pole) {
        // A copy, not a conversion. The overview study and production
        // each have a six-coefficient conic of the same shape - the
        // study's came first, in Sprint 30 - so the ellipse a globe
        // draws a great circle as arrives here exactly as the
        // reviewed candidate computed it, with the coefficients
        // carried across in order and nothing recomputed.
        return reviewed.greatCircle(pole).map(conic -> new PlaneConic(
                conic.a(), conic.b(), conic.c(),
                conic.d(), conic.e(), conic.f()));
    }
}
