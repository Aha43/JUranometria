package juranometria.tool.overview;

import java.util.Optional;

import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PanSolver;
import juranometria.project.PlanePoint;

/**
 * The three candidates, as functions (Sprint 30, issue #296).
 *
 * <p>All three are azimuthal: they differ only in how far from the
 * centre a given angular distance lands, which is one function each.
 * Everything else - the rotation of the sky onto the page, the
 * direction convention, the inverse - is common, and the gate found
 * that worth noticing: the interface is small because the family is.
 *
 * <p>The gnomonic candidate <strong>delegates to production</strong>
 * rather than restating it, so the control in every measurement is
 * the released projection and not a copy of it that might have
 * drifted.
 */
final class Candidates {

    private Candidates() {
    }

    /** The released projection, borrowed as the control. */
    /** The three by name, so a report can loop over them. */
    static StudyProjection named(String name, SkyPosition centre) {
        return switch (name) {
            case "gnomonic" -> gnomonic(centre);
            case "stereographic" -> stereographic(centre);
            case "orthographic" -> orthographic(centre);
            default -> throw new IllegalArgumentException(
                    "no candidate is called " + name);
        };
    }

    static StudyProjection gnomonic(SkyPosition centre) {
        GnomonicProjection production = new GnomonicProjection(centre);
        return new StudyProjection() {

            @Override
            public String name() {
                return "gnomonic";
            }

            @Override
            public SkyPosition centre() {
                return centre;
            }

            @Override
            public Optional<PlanePoint> project(SkyPosition position) {
                return production.project(position);
            }

            @Override
            public Optional<SkyPosition> unproject(PlanePoint point) {
                return Optional.of(PanSolver.skyFromPlane(centre, point));
            }

            @Override
            public double planeRadius(double angleDegrees) {
                return Math.tan(Math.toRadians(angleDegrees));
            }

            @Override
            public double limitDegrees() {
                return 90.0;
            }
        };
    }

    /** r = 2 tan(theta/2): conformal, loses only the antipode. */
    static StudyProjection stereographic(SkyPosition centre) {
        return azimuthal(centre, "stereographic",
                angle -> 2.0 * Math.tan(angle / 2.0),
                radius -> 2.0 * Math.atan(radius / 2.0),
                179.999);
    }

    /** r = sin(theta): the globe's own outline, one hemisphere. */
    static StudyProjection orthographic(SkyPosition centre) {
        return azimuthal(centre, "orthographic",
                Math::sin,
                radius -> radius >= 1.0 ? Double.NaN : Math.asin(radius),
                90.0);
    }

    /**
     * An azimuthal projection from its radius function and that
     * function's inverse, both in radians.
     *
     * <p>The direction from the centre is the same for all three -
     * the position angle of the target from the centre, east to the
     * left as the atlas draws it - so only the radius differs. The
     * gnomonic case above is written out separately only because it
     * delegates to production.
     */
    private static StudyProjection azimuthal(SkyPosition centre,
                                             String name,
                                             java.util.function.DoubleUnaryOperator radiusOf,
                                             java.util.function.DoubleUnaryOperator angleOf,
                                             double limitDegrees) {
        double centreRa = Math.toRadians(centre.raDegrees());
        double centreDec = Math.toRadians(centre.decDegrees());
        double sinCentreDec = Math.sin(centreDec);
        double cosCentreDec = Math.cos(centreDec);

        return new StudyProjection() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public SkyPosition centre() {
                return centre;
            }

            @Override
            public Optional<PlanePoint> project(SkyPosition position) {
                double raOffset =
                        Math.toRadians(position.raDegrees()) - centreRa;
                double dec = Math.toRadians(position.decDegrees());
                double sinDec = Math.sin(dec);
                double cosDec = Math.cos(dec);

                double cosDistance = sinCentreDec * sinDec
                        + cosCentreDec * cosDec * Math.cos(raOffset);
                double distance = Math.acos(Math.clamp(cosDistance, -1.0, 1.0));
                if (Math.toDegrees(distance) > limitDegrees) {
                    return Optional.empty();
                }
                if (distance < 1e-12) {
                    return Optional.of(new PlanePoint(0.0, 0.0));
                }

                // The same east/north directions the gnomonic
                // projection uses, scaled to this projection's radius
                // instead of the tangent plane's.
                double east = cosDec * Math.sin(raOffset);
                double north = cosCentreDec * sinDec
                        - sinCentreDec * cosDec * Math.cos(raOffset);
                double length = Math.hypot(east, north);
                double radius = radiusOf.applyAsDouble(distance);
                return Optional.of(new PlanePoint(radius * east / length,
                        radius * north / length));
            }

            @Override
            public Optional<SkyPosition> unproject(PlanePoint point) {
                double radius = Math.hypot(point.xiEast(), point.etaNorth());
                if (radius < 1e-15) {
                    return Optional.of(centre);
                }
                double distance = angleOf.applyAsDouble(radius);
                if (Double.isNaN(distance)) {
                    return Optional.empty();
                }
                double sinDistance = Math.sin(distance);
                double cosDistance = Math.cos(distance);
                double east = point.xiEast() / radius;
                double north = point.etaNorth() / radius;

                double sinDec = cosDistance * sinCentreDec
                        + sinDistance * north * cosCentreDec;
                double dec = Math.asin(Math.clamp(sinDec, -1.0, 1.0));
                double ra = centreRa + Math.atan2(sinDistance * east,
                        cosDistance * cosCentreDec
                                - sinDistance * north * sinCentreDec);
                return Optional.of(new SkyPosition(
                        (Math.toDegrees(ra) % 360.0 + 360.0) % 360.0,
                        Math.toDegrees(dec)));
            }

            @Override
            public double planeRadius(double angleDegrees) {
                return radiusOf.applyAsDouble(Math.toRadians(angleDegrees));
            }

            @Override
            public double limitDegrees() {
                return limitDegrees;
            }
        };
    }
}
