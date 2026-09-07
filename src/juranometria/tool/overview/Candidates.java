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

            @Override
            public Optional<PlaneCurve> greatCircle(SkyPosition pole) {
                return gnomonicCircle(inFrame(centre, pole));
            }
        };
    }

    /** r = 2 tan(theta/2): conformal, loses only the antipode. */
    static StudyProjection stereographic(SkyPosition centre) {
        return azimuthal(centre, "stereographic",
                angle -> 2.0 * Math.tan(angle / 2.0),
                radius -> 2.0 * Math.atan(radius / 2.0),
                179.999, Candidates::stereographicCircle);
    }

    /** r = sin(theta): the globe's own outline, one hemisphere. */
    static StudyProjection orthographic(SkyPosition centre) {
        return azimuthal(centre, "orthographic",
                Math::sin,
                radius -> radius >= 1.0 ? Double.NaN : Math.asin(radius),
                90.0, Candidates::orthographicCircle);
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
                                             double limitDegrees,
                                             CircleForm circleForm) {
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

            @Override
            public Optional<PlaneCurve> greatCircle(SkyPosition pole) {
                return circleForm.of(inFrame(centre, pole));
            }
        };
    }

    /** A projection's own answer, from the pole in its own frame. */
    private interface CircleForm {
        Optional<PlaneCurve> of(double[] pole);
    }

    /**
     * The pole, in the projection's own frame: how far along the
     * centre direction, the east direction, and the north direction.
     *
     * <p>These are the same three dot products {@code project} takes
     * of a position, which is why a great circle's form falls out of
     * the projection's arithmetic rather than out of a fit.
     */
    static double[] inFrame(SkyPosition centre, SkyPosition pole) {
        double centreRa = Math.toRadians(centre.raDegrees());
        double centreDec = Math.toRadians(centre.decDegrees());
        double poleDec = Math.toRadians(pole.decDegrees());
        double offset = Math.toRadians(pole.raDegrees()) - centreRa;
        double sinCentreDec = Math.sin(centreDec);
        double cosCentreDec = Math.cos(centreDec);
        double sinPoleDec = Math.sin(poleDec);
        double cosPoleDec = Math.cos(poleDec);
        return new double[] {
                sinCentreDec * sinPoleDec
                        + cosCentreDec * cosPoleDec * Math.cos(offset),
                cosPoleDec * Math.sin(offset),
                cosCentreDec * sinPoleDec
                        - sinCentreDec * cosPoleDec * Math.cos(offset)};
    }

    /**
     * Every gnomonic great circle is a straight line.
     *
     * <p>A point at angle {@code t} and position angle {@code f}
     * lands at {@code (tan t sin f, tan t cos f)}, so
     * {@code sin t / r = cos t}; putting that into
     * {@code pole . point = 0} gives
     * {@code cos t (a + b xi + c eta) = 0}, and the cosine is
     * positive everywhere the projection reaches.
     */
    private static Optional<PlaneCurve> gnomonicCircle(double[] pole) {
        if (Math.hypot(pole[1], pole[2]) < 1.0e-12) {
            // The circle lies entirely at ninety degrees from the
            // centre, where this projection is infinitely far away.
            return Optional.empty();
        }
        return Optional.of(
                PlaneCurve.Straight.of(pole[0], pole[1], pole[2]));
    }

    /**
     * A stereographic great circle is a circle, or a line when it
     * passes through the centre.
     *
     * <p>With {@code r = 2 tan(t/2)} the same substitution gives
     * {@code a (1 - (xi^2 + eta^2)/4) + b xi + c eta = 0}: a circle
     * of centre {@code (2b/a, 2c/a)} and radius {@code 2/|a|}, and a
     * line through the origin when {@code a} is zero - which is
     * exactly when the pole is ninety degrees from the centre, so
     * the circle runs through the centre of the page.
     */
    private static Optional<PlaneCurve> stereographicCircle(double[] pole) {
        if (Math.abs(pole[0]) < 1.0e-12) {
            return Optional.of(
                    PlaneCurve.Straight.of(0.0, pole[1], pole[2]));
        }
        return Optional.of(new PlaneCurve.Circular(
                2.0 * pole[1] / pole[0], 2.0 * pole[2] / pole[0],
                2.0 / Math.abs(pole[0])));
    }

    /**
     * An orthographic great circle is an ellipse centred on the page
     * centre, one radius wide and {@code |a|} deep.
     *
     * <p>With {@code r = sin t} the substitution gives
     * {@code (a^2 + b^2) xi^2 + 2bc xi eta + (a^2 + c^2) eta^2 = a^2},
     * whose axes are {@code |a|} along the direction of the pole's
     * own tangential part and exactly 1 - the limb - across it. When
     * the pole is the centre the two are equal and the ellipse is
     * the limb itself, which is the right answer: the great circle
     * square to the line of sight is the edge of the globe.
     */
    private static Optional<PlaneCurve> orthographicCircle(double[] pole) {
        if (Math.abs(pole[0]) < 1.0e-12) {
            // The ellipse has collapsed: with the pole square to the
            // centre the conic reads (b xi + c eta)^2 = 0, which is a
            // line through the centre and not a very thin ellipse.
            // Left as an ellipse it is a shape with no interior, and
            // the affine frame that clips it cannot be inverted.
            return Optional.of(
                    PlaneCurve.Straight.of(0.0, pole[1], pole[2]));
        }
        double along = Math.abs(pole[0]);
        if (Math.abs(along - 1.0) < 1.0e-12) {
            // Equal axes. The pole is the centre, so this circle is
            // the limb itself - and a circle is the simpler of two
            // true names for it. Each projection returns the
            // simplest form that is exact, so that one curve has one
            // name and a count of forms means something.
            return Optional.of(new PlaneCurve.Circular(0.0, 0.0, 1.0));
        }
        double tilt = Math.hypot(pole[1], pole[2]) < 1.0e-12
                ? 0.0 : Math.atan2(pole[2], pole[1]);
        return Optional.of(new PlaneCurve.Elliptical(0.0, 0.0,
                along, 1.0, tilt));
    }
}
