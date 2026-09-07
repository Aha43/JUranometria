package juranometria.project;

import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * What the atlas's projections have in common (issue #297).
 *
 * <p>Both are azimuthal: they keep the direction from the centre and
 * differ only in how far out they put a thing that far away. So the
 * direction is worked out once, here, and each projection says what
 * it does with it.
 *
 * <p>The inverse is shared for the same reason and in the same shape,
 * differing only in the angle a plane radius stands for.
 */
abstract class AzimuthalProjection implements Projection {

    private final SkyPosition centre;
    private final CentreFrame frame;

    AzimuthalProjection(SkyPosition centre) {
        this.centre = centre;
        this.frame = CentreFrame.about(centre);
    }

    @Override
    public final SkyPosition centre() {
        return centre;
    }

    final CentreFrame frame() {
        return frame;
    }

    /** The angle a point this far out on the plane came from, in
     *  radians, or NaN where the plane has no sky under it. */
    abstract double angleAtRadius(double planeRadius);

    @Override
    public final double angleAtPlaneRadius(double planeRadius) {
        return Math.toDegrees(angleAtRadius(planeRadius));
    }

    @Override
    public final Optional<SkyPosition> unproject(PlanePoint point) {
        double xi = point.xiEast();
        double eta = point.etaNorth();
        double rho = Math.hypot(xi, eta);
        if (rho == 0.0) {
            return Optional.of(centre);
        }
        double c = angleAtRadius(rho);
        if (Double.isNaN(c)) {
            return Optional.empty();
        }
        double sinC = Math.sin(c);
        double cosC = Math.cos(c);
        double sinCentreDec = frame.sinCentreDec();
        double cosCentreDec = frame.cosCentreDec();
        double dec = Math.asin(cosC * sinCentreDec
                + eta * sinC * cosCentreDec / rho);
        double ra = frame.centreRaRadians() + Math.atan2(xi * sinC,
                rho * cosCentreDec * cosC - eta * sinCentreDec * sinC);
        return Optional.of(new SkyPosition(
                (Math.toDegrees(ra) % 360.0 + 360.0) % 360.0,
                Math.toDegrees(dec)));
    }

    /**
     * A chart plane has no edge to the sky: everything the projection
     * can show, it shows somewhere on it.
     */
    @Override
    public double visiblePlaneRadius() {
        return Double.POSITIVE_INFINITY;
    }
}
