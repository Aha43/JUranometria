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
            public double visiblePlaneRadius() {
                return Double.POSITIVE_INFINITY;
            }

            @Override
            public Optional<PlaneConic> greatCircle(SkyPosition pole) {
                return gnomonicCircle(inFrame(centre, pole));
            }
        };
    }

    /** r = 2 tan(theta/2): conformal, loses only the antipode. */
    static StudyProjection stereographic(SkyPosition centre) {
        return azimuthal(centre, "stereographic",
                angle -> 2.0 * Math.tan(angle / 2.0),
                radius -> 2.0 * Math.atan(radius / 2.0),
                // Everything but the antipode, which is the
                // projection's own statement about itself. An earlier
                // draft wrote 179.999 here, which quietly refused a
                // finite region of sky - a thousandth of a degree
                // across the whole sky is not a rounding guard, it is
                // a different projection from the documented one.
                // The antipode is excluded where it actually fails,
                // by its radius not being finite, and nowhere else.
                180.0, Double.POSITIVE_INFINITY,
                Candidates::stereographicCircle);
    }

    /** r = sin(theta): the globe's own outline, one hemisphere. */
    static StudyProjection orthographic(SkyPosition centre) {
        return azimuthal(centre, "orthographic",
                Math::sin,
                // A radius of exactly one is the limb, and the limb
                // is on the globe: asin(1) is ninety degrees. Written
                // as ">=" this refused the one circle the projection
                // draws best, and disagreed with project(), which
                // places a point at ninety degrees quite happily.
                radius -> radius > 1.0 ? Double.NaN : Math.asin(radius),
                // A globe has an edge, and it is at a plane radius
                // of one: a point ninety degrees from the centre
                // lands at sin(90) and there is nothing beyond it.
                90.0, 1.0, Candidates::orthographicCircle);
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
                                             double visibleRadius,
                                             CircleForm circleForm) {
        double centreRa = Math.toRadians(centre.raDegrees());
        double centreDec = Math.toRadians(centre.decDegrees());
        double sinCentreDec = Math.sin(centreDec);
        double cosCentreDec = Math.cos(centreDec);
        // Compared in radians, because the limb is exactly where the
        // comparison happens: a point ninety degrees out gives an
        // angle of pi/2 to the last bit, and converting that to
        // degrees and back can land a hair above 90.0 and refuse the
        // one circle a globe draws best.
        double limitRadians = Math.toRadians(limitDegrees);

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

                // The east and north parts of the direction from the
                // centre. Their length is the sine of the angular
                // distance, computed here rather than derived from
                // its cosine, and that is the whole reason the angle
                // below is found the way it is.
                double east = cosDec * Math.sin(raOffset);
                double north = cosCentreDec * sinDec
                        - sinCentreDec * cosDec * Math.cos(raOffset);
                double length = Math.hypot(east, north);
                double cosDistance = sinCentreDec * sinDec
                        + cosCentreDec * cosDec * Math.cos(raOffset);

                // From both parts, never from the cosine alone.
                // Inverting a cosine is ill conditioned at both ends
                // of its range - near zero and near a half turn a
                // double's cosine has already lost the small
                // difference the angle is made of - so acos there is
                // accurate to about 1.5e-08 radians whatever it is
                // given, and rounds a whole disc of sky three
                // milliarcseconds across onto the antipode itself,
                // where it was then refused as unplaceable. A review
                // found that region. atan2 of the two parts is
                // accurate at both ends, because the small part is
                // measured rather than reconstructed.
                double distance = Math.atan2(length, cosDistance);
                if (distance > limitRadians) {
                    return Optional.empty();
                }
                if (length == 0.0) {
                    // On the axis: the centre itself, or the one
                    // point opposite it that no azimuthal projection
                    // places. An exact test for an exact condition,
                    // where the old threshold quietly coalesced
                    // everything within a fifth of a microarcsecond
                    // of the centre.
                    return cosDistance > 0.0
                            ? Optional.of(new PlanePoint(0.0, 0.0))
                            : Optional.empty();
                }
                double radius = radiusOf.applyAsDouble(distance);
                if (!Double.isFinite(radius)) {
                    return Optional.empty();
                }
                return Optional.of(new PlanePoint(radius * east / length,
                        radius * north / length));
            }

            @Override
            public Optional<SkyPosition> unproject(PlanePoint point) {
                double radius = Math.hypot(point.xiEast(), point.etaNorth());
                if (radius == 0.0) {
                    // The only radius with no direction to it. Every
                    // other one divides safely below, however small.
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
            public double visiblePlaneRadius() {
                return visibleRadius;
            }

            @Override
            public Optional<PlaneConic> greatCircle(SkyPosition pole) {
                return circleForm.of(inFrame(centre, pole));
            }
        };
    }

    /**
     * How thin an orthographic ellipse may be before it is a line.
     *
     * <p>{@code a} is a direction cosine and the ellipse's thin
     * radius is {@code |a|} in plane units, so the substituted line
     * lies at most {@code |a| x (page units per plane unit)} from
     * the true curve. The atlas's largest scale is its narrowest
     * field, one degree, at 5.16e4 page units per plane unit - so at
     * this threshold the substitution costs at most
     * <strong>5.2e-04 page units</strong> on the worst page it can
     * ever be asked for, and less on every other. That is the
     * derivation; {@link SubstitutionReport} is the measurement.
     */
    private static final double FLAT = 1.0e-8;

    /**
     * The projections substitute nothing else.
     *
     * <p>Each returns the exact curve, and the only special cases
     * are exact ones: a stereographic circle of unbounded radius
     * <em>is</em> a line when the pole is exactly square to the
     * centre, and an orthographic ellipse <em>is</em> a circle when
     * its radii are exactly equal. Both are equalities, not
     * tolerances.
     *
     * <p>An earlier draft substituted the simpler form <em>near</em>
     * those cases, on a bare epsilon of 1e-12, and a review was
     * right to refuse it. Measuring what it cost showed something
     * worse than an unjustified threshold: it was in the wrong
     * place. Between a pole component of 1e-3 and 1e-12 the exact
     * circle has a radius so large that asking where it crosses a
     * page loses every digit that matters - the measured error
     * peaked at <strong>8e+08 page units</strong> - so the whole
     * band the epsilon was protecting was the band it left
     * unprotected.
     *
     * <p>The decision cannot be made here in any case. Whether a
     * circle is distinguishable from a line is a question about a
     * page: the same curve is one or the other depending on how much
     * of it a page shows and at what scale. So it is made in
     * {@link StudyMapping#onPage}, in page units, where the page is
     * known - and it is a statement about the error it allows rather
     * than about the size of a number.
     */
    /** A projection's own answer, from the pole in its own frame. */
    private interface CircleForm {
        Optional<PlaneConic> of(double[] pole);
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
    private static Optional<PlaneConic> gnomonicCircle(double[] pole) {
        if (pole[1] == 0.0 && pole[2] == 0.0) {
            // The circle lies entirely at ninety degrees from the
            // centre, where this projection is infinitely far away.
            // An exact test, because it is an exact condition: the
            // line has no direction at all, not merely a distant one.
            return Optional.empty();
        }
        return Optional.of(PlaneConic.line(pole[0], pole[1], pole[2]));
    }

    /**
     * A stereographic great circle, as one conic for every case.
     *
     * <p>With {@code r = 2 tan(t/2)} the substitution gives
     * {@code a (1 - (xi^2 + eta^2)/4) + b xi + c eta = 0}, which is
     * written out below without ever dividing by {@code a}. Divided
     * through it would be a circle of centre {@code (2b/a, 2c/a)}
     * and radius {@code 2/|a|}, and that division is the whole
     * trouble: it is a circle for most poles, a line for one, and a
     * pile of overflow either side of it. Left undivided the same
     * six numbers say all three, and the passage from circle to line
     * is the coefficient of {@code x^2} passing through zero.
     */
    private static Optional<PlaneConic> stereographicCircle(double[] pole) {
        return Optional.of(new PlaneConic(-pole[0] / 4.0, 0.0,
                -pole[0] / 4.0, pole[1], pole[2], pole[0]));
    }

    /**
     * An orthographic great circle is an ellipse about the centre.
     *
     * <p>With {@code r = sin t} the substitution gives
     * {@code (a^2 + b^2) xi^2 + 2bc xi eta + (a^2 + c^2) eta^2 = a^2},
     * whose axes are {@code |a|} along the direction of the pole's
     * own tangential part and exactly 1 - the limb - across it. When
     * the pole is the centre the two are equal and the ellipse is
     * the limb itself, which is the right answer: the great circle
     * square to the line of sight is the edge of the globe.
     */
    private static Optional<PlaneConic> orthographicCircle(double[] pole) {
        double a = pole[0];
        double b = pole[1];
        double c = pole[2];
        if (Math.abs(a) < FLAT) {
            // The conic factors: with the pole square to the centre
            // it reads (b xi + c eta)^2 = 0, a line drawn twice
            // rather than an ellipse of no width. A great circle
            // through the centre of an orthographic page crosses it
            // straight, and that is the answer wanted.
            //
            // This one case cannot be left to the page, and the
            // reason is worth stating. Everywhere else a projection
            // hands over a conic and the page decides what to draw;
            // here the conic is degenerate, its quadratic part is a
            // perfect square, and the arithmetic that would find a
            // centre and two radii divides by a determinant that is
            // zero in exact arithmetic and rounding noise in a
            // double. There is nothing for the page to measure.
            return Optional.of(PlaneConic.line(0.0, b, c));
        }
        return Optional.of(new PlaneConic(a * a + b * b, 2.0 * b * c,
                a * a + c * c, 0.0, 0.0, -a * a));
    }
}
