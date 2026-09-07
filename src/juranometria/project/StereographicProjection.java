package juranometria.project;

import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Stereographic projection: the atlas's overview (Sprint 30).
 *
 * <p>Chosen by measurement, not by taste. Its <em>shape</em>
 * distortion is zero at every field and every corner - that is what
 * conformal means, and it is the whole argument, because a reader
 * matches a shape against the sky. At a 90-degree page corner the
 * gnomonic projection draws a circle as an ellipse half as wide again
 * as it is tall and the orthographic one more than twice; this draws
 * it round. It pays in scale instead, and mildly
 * (docs/decisions/overview-projection.md).
 *
 * <p>It also has the domain. The gnomonic projection cannot reach 90
 * degrees from its centre at any price; this reaches everything but
 * the single point opposite the centre, and a whole hemisphere fits
 * inside a plane radius of two.
 */
public final class StereographicProjection extends AzimuthalProjection {

    public StereographicProjection(SkyPosition centre) {
        super(centre);
    }

    @Override
    public String name() {
        return "stereographic";
    }

    /**
     * Projects a sky position onto the plane.
     *
     * <p>{@code 2 / (1 + along)} rather than {@code 2 tan(t/2)}
     * divided by {@code sin t}: the same number, without ever
     * forming the angle, and well conditioned everywhere except the
     * one point where the projection genuinely has no answer.
     */
    @Override
    public Optional<PlanePoint> project(SkyPosition position) {
        Direction direction = frame().directionTo(position);
        if (direction.onTheAxis()) {
            // On the axis: the centre itself, or the one point
            // opposite it that no azimuthal projection places.
            return direction.along() > 0.0
                    ? Optional.of(new PlanePoint(0.0, 0.0))
                    : Optional.empty();
        }
        double scale = 2.0 / (1.0 + direction.along());
        if (!Double.isFinite(scale)) {
            return Optional.empty();
        }
        return Optional.of(new PlanePoint(scale * direction.east(),
                scale * direction.north()));
    }

    @Override
    double angleAtRadius(double planeRadius) {
        return 2.0 * Math.atan(planeRadius / 2.0);
    }

    @Override
    public double planeRadius(double angleDegrees) {
        return 2.0 * Math.tan(Math.toRadians(angleDegrees) / 2.0);
    }

    /**
     * Everything but the antipode, which is the projection's own
     * statement about itself rather than a number near it.
     */
    @Override
    public double limitDegrees() {
        return 180.0;
    }

    /**
     * A stereographic great circle, as one conic for every case.
     *
     * <p>Substituting {@code r = 2 tan(t/2)} into
     * {@code pole . point = 0} gives
     * {@code a (1 - (xi^2 + eta^2)/4) + b xi + c eta = 0}, written
     * out here without ever dividing by {@code a}. Divided through it
     * would be a circle of centre {@code (2b/a, 2c/a)} and radius
     * {@code 2/|a|}, and that division is the whole trouble: a circle
     * for most poles, a line for one, and a pile of overflow either
     * side of it. Left undivided the same six numbers say both.
     */
    @Override
    public Optional<PlaneConic> greatCircle(SkyPosition pole) {
        Direction direction = frame().directionTo(pole);
        return Optional.of(new PlaneConic(
                -direction.along() / 4.0, 0.0, -direction.along() / 4.0,
                direction.east(), direction.north(), direction.along()));
    }
}
