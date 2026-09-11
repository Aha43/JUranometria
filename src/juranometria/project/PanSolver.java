package juranometria.project;

import java.util.Optional;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;

/**
 * The grab-to-pan geometry of docs/decisions/pan-navigation.md: the
 * exact inverse of the chart's gnomonic page - pixel to sky - and the
 * exact grab solver - given that the sky position grabbed at press
 * time must sit under the moved pointer, find the new chart centre.
 * Both are closed-form in the projection's own frame (xi east, eta
 * north; east left, north up on the page), never a degrees-per-pixel
 * approximation. Proven and measured by the Sprint 8 design gate
 * (make pan-study); production home per that decision.
 */
public final class PanSolver {

    /**
     * A classified solver outcome: {@code centre} is present unless the
     * request would carry the chart centre past a celestial pole, in
     * which case {@code pastPole} is true - the only legitimate empty.
     * {@code constrained} marks a polar follow whose horizontal
     * component was clamped to the feasibility boundary. Any empty
     * without past-pole evidence is a solver invariant violation and
     * throws rather than passing as a quiet no-op (PR #76 follow-up).
     *
     * {@code ambiguous} reports that a SECOND verified centre, more
     * than {@link #AMBIGUITY_SEPARATION_DEGREES} from the returned
     * one, also solves the request exactly - a near-polar page whose
     * grabbed sky lies beyond the pole. The returned centre is still
     * the one nearest the previous centre: for a drag's small
     * increments that continuity tie-break is the right answer and
     * panning keeps it (docs/decisions/pan-navigation.md); a
     * pointer-anchored zoom step is a large jump for which the
     * tie-break can silently switch branches, so the zoom decision
     * REFUSES ambiguous transitions outright
     * (docs/decisions/pointer-zoom.md, PR #127 review).
     */
    public record PanSolution(Optional<SkyPosition> centre,
                              boolean constrained, boolean pastPole,
                              boolean ambiguous) {
    }

    /**
     * Two verified centres further apart than this are a genuine
     * branch ambiguity, not the declination equation's double root
     * resolving to numerical twins (those agree to ~1e-8 degrees).
     */
    public static final double AMBIGUITY_SEPARATION_DEGREES = 1e-3;

    /**
     * Solver acceptance: a candidate centre must reproject the grabbed
     * position onto the requested plane point this closely (plane
     * units; at the widest released page one plane unit is ~1385 px,
     * so this is ~0.0014 px). The floor is set by drags along a plane
     * axis, where the declination equation has a double root and
     * floating point resolves it to about 1e-8 plane units - a
     * tolerance below that rejects exact solutions.
     */
    static final double PLANE_TOLERANCE = 1e-6;

    private PanSolver() {
    }

    /** The tangent-plane point under a pixel; the mapping's inverse. */
    public static PlanePoint planeFromPixel(ChartViewport viewport,
                                            PixelPoint pixel) {
        // The viewport's own scale, not a second copy of the rule
        // that computes it - this held the same inlined tangent
        // ViewportMapping did.
        // Navigation arithmetic on a viewport with nothing drawn
        // yet, so the projection is named rather than carried: the
        // viewport says which kind draws its field, and that is the
        // one derivation the atlas makes (#301).
        double pixelsPerPlaneUnit = new ViewportMapping(viewport,
                Projections.of(viewport.projection(),
                        viewport.centre())).pixelsPerPlaneUnit();
        return new PlanePoint(
                (viewport.widthPx() / 2.0 - pixel.x()) / pixelsPerPlaneUnit,
                (viewport.heightPx() / 2.0 - pixel.y()) / pixelsPerPlaneUnit);
    }

    /**
     * The exact inverse gnomonic projection: the sky position whose
     * image is the given tangent-plane point, for a chart centred at
     * {@code centre}. Every finite plane point has a pre-image.
     */
    /**
     * Which position a point on the plane came from, under the
     * projection the chart is drawn by.
     *
     * <p>The kind is asked for rather than assumed, and the old form
     * that took only a centre is gone. It read the plane back through
     * a tangent plane whatever page the reader was looking at, so an
     * overview chart scaled its pointer one way and inverted it
     * another - hit testing, pan press and pointer zoom all silently
     * on the wrong sky. A review found it. There is nothing left that
     * can make that mistake by omission.
     */
    public static SkyPosition skyFromPlane(ChartProjection kind,
                                           SkyPosition centre,
                                           PlanePoint plane) {
        return Projections.of(kind, centre).unproject(plane).orElseThrow();
    }

    /** The same, for a caller that has the page it is pointing at. */
    public static SkyPosition skyFromPlane(ChartViewport viewport,
                                           PlanePoint plane) {
        return skyFromPlane(viewport.projection(), viewport.centre(), plane);
    }


    /**
     * The grab invariant, solved exactly: find the chart centre for
     * which {@code grabbed} projects to the tangent-plane point
     * {@code target}. In the centre's orthonormal frame (c, e east,
     * n north) the invariant reads s = (c + xi e + eta n) / N with
     * N = sqrt(1 + xi^2 + eta^2), which separates into closed-form
     * equations for the centre's RA (from s. e, which depends on RA
     * alone) and then its declination (from s . c). Up to four
     * algebraic candidates arise; each is verified by full
     * reprojection and the valid candidate nearest the previous
     * centre is returned, keeping a continuous drag continuous.
     *
     * When the requested plane point is infeasible for this grabbed
     * position - a north-up chart pins a near-polar point close to the
     * page's vertical axis, the feasible set being
     * |xi| <= cot|dec_s| * sqrt(1 + eta^2) - the horizontal component
     * is clamped to the feasibility boundary and solved exactly there:
     * the sky follows the hand as far as the chart's geometry allows,
     * tracking the vertical component fully (PR #76 review, P1).
     * Returns empty only when even the clamped target has no centre
     * inside the valid declination range - panning past the pole -
     * which is an explicit hold, never NaN state.
     */
    public static PanSolution solveCentre(ChartProjection kind,
                                          SkyPosition grabbed,
                                          PlanePoint target,
                                          SkyPosition previousCentre) {
        double raS = Math.toRadians(grabbed.raDegrees());
        double decS = Math.toRadians(grabbed.decDegrees());
        double sx = Math.cos(decS) * Math.cos(raS);
        double sy = Math.cos(decS) * Math.sin(raS);
        double sz = Math.sin(decS);
        double cosDecS = Math.cos(decS);

        // What the page asks for, in the sky rather than on the
        // paper. Everything below this line is spherical geometry
        // and knows no projection at all.
        //
        // This is the whole generalisation, and it is smaller than
        // the refusal it replaced. The equations were the tangent
        // plane's only because two quantities were written in its
        // units: 1/N is the cosine of the angle from the centre, and
        // xi/N is that offset's eastward part. Every azimuthal
        // projection has both - it places a direction at a plane
        // radius that depends on the angle alone, and along the true
        // bearing - so asking the projection for the angle at a
        // plane radius says them in any of them. For the tangent
        // plane the numbers below are exactly 1/N and xi/N again,
        // which is why nothing about panning a 42-degree page
        // changed.
        Offset asked = offsetOf(kind, target);
        double along = asked.along();
        double east = asked.east();
        double north = asked.north();

        // Feasibility: |s . e| <= cos(decS), because e depends on the
        // centre's right ascension alone and cannot reach further
        // east of a near-polar grab than that. An infeasible request
        // has its horizontal component clamped to the boundary - with
        // a margin keeping the declination equation strictly solvable
        // - and is solved exactly there, so the sky follows the hand
        // as far as the chart's geometry allows and tracks the
        // vertical component in full (PR #76 review).
        //
        // The vertical that is tracked in full is the *page's*, so
        // the clamp holds eta and moves xi, exactly as it always did.
        // Clamping the offset's northward part instead looks like the
        // same thing and is not: the along part then takes up the
        // difference, which moves the page point the reader is
        // dragging vertically as well. It cost a test to find out,
        // and the test was right.
        double margin = 1.0 - 1e-9;
        double bound = margin * cosDecS;
        boolean constrained = Math.abs(sz) > 1e-15
                && Math.abs(east) > bound;
        PlanePoint request = target;
        if (constrained) {
            request = new PlanePoint(
                    feasibleAcross(kind, target, bound),
                    target.etaNorth());
            Offset held = offsetOf(kind, request);
            along = held.along();
            east = held.east();
            north = held.north();
        }

        // s . e = east with e = (-sin a, cos a, 0):
        // cos(decS) * sin(raS - a) = east.
        double sinOffset = east / cosDecS;
        // After the feasibility clamp above this cannot exceed 1 beyond
        // floating-point rounding (an offset's eastward part is at
        // most one off the polar path); anything larger is a solver
        // invariant violation, never
        // a quiet no-op. The clamp below absorbs pure rounding at the
        // boundary, where a grab on the horizontal axis lands at exactly
        // 1; every surviving candidate is verified by full reprojection.
        if (Math.abs(sinOffset) > 1.0 + 1e-9) {
            throw new IllegalStateException(String.format(
                    java.util.Locale.ROOT,
                    "pan solver invariant violated: infeasible RA equation"
                            + " after clamping (%.12f) for grab %s",
                    sinOffset, grabbed));
        }
        double offset = Math.asin(Math.clamp(sinOffset, -1.0, 1.0));
        double[] alphaCandidates = {raS - offset, raS - (Math.PI - offset)};

        // What every candidate is verified against: the page point
        // actually being solved for, which is the one asked for
        // unless the clamp moved it.
        PlanePoint solved = request;

        SkyPosition best = null;
        double bestSeparation = Double.MAX_VALUE;
        boolean sawOutOfRangeDeclination = false;
        java.util.List<SkyPosition> verified = new java.util.ArrayList<>(4);
        for (double alpha : alphaCandidates) {
            // s . c = along: p cos d + sz sin d = along with
            // p = sx cos a + sy sin a.
            double p = sx * Math.cos(alpha) + sy * Math.sin(alpha);
            double amplitude = Math.hypot(p, sz);
            if (amplitude < 1e-15
                    || Math.abs(along) > amplitude * (1.0 + 1e-9)) {
                continue;
            }
            double phase = Math.atan2(sz, p);
            double acos = Math.acos(Math.clamp(along / amplitude,
                    -1.0, 1.0));
            for (double delta : new double[] {phase + acos, phase - acos}) {
                if (Math.abs(delta) >= Math.PI / 2.0) {
                    // An algebraic root beyond a pole: the only source
                    // of a legitimate empty result.
                    sawOutOfRangeDeclination = true;
                    continue;
                }
                SkyPosition candidate = new SkyPosition(
                        (Math.toDegrees(alpha) % 360.0 + 360.0) % 360.0,
                        Math.toDegrees(delta));
                if (!reprojects(kind, candidate, grabbed, solved)) {
                    continue;
                }
                verified.add(candidate);

                double separation =
                        candidate.separationDegrees(previousCentre);
                if (separation < bestSeparation) {
                    bestSeparation = separation;
                    best = candidate;
                }
            }
        }
        if (best == null) {
            if (!sawOutOfRangeDeclination) {
                throw new IllegalStateException(String.format(
                        java.util.Locale.ROOT,
                        "pan solver invariant violated: no centre for grab"
                                + " %s at plane (%.9f, %.9f) and no past-pole"
                                + " evidence", grabbed, solved.xiEast(),
                        solved.etaNorth()));
            }
            return new PanSolution(Optional.empty(), constrained, true,
                    false);
        }
        boolean ambiguous = false;
        for (SkyPosition candidate : verified) {
            if (candidate.separationDegrees(best)
                    > AMBIGUITY_SEPARATION_DEGREES) {
                ambiguous = true;
                break;
            }
        }
        return new PanSolution(Optional.of(best), constrained, false,
                ambiguous);
    }

    /** Full-projection verification of a candidate centre. */
    private static boolean reprojects(ChartProjection kind,
                                      SkyPosition centre, SkyPosition grabbed,
                                      PlanePoint target) {
        var projection = Projections.of(kind, centre);
        var plane = projection.project(grabbed);
        return plane.isPresent()
                && Math.abs(plane.get().xiEast() - target.xiEast())
                        <= PLANE_TOLERANCE
                && Math.abs(plane.get().etaNorth() - target.etaNorth())
                        <= PLANE_TOLERANCE;
    }

    /**
     * A direction from the chart's centre, in the centre's own frame:
     * how far along the line of sight, how far east, how far north.
     *
     * <p>A unit vector, and the one thing a page and the sky can both
     * say. The plane point is the projection's account of it and the
     * spherical equations are the sky's, so this is where the two
     * meet - and it is why the solver below knows no projection.
     */
    private record Offset(double along, double east, double north) {
    }

    /**
     * The radial law, which belongs to the projection and not to
     * where it is pointed.
     *
     * <p>An azimuthal projection places a direction at a plane radius
     * that depends on the angle from the centre alone, along the true
     * bearing. Neither half of that mentions the centre, so any
     * centre answers - and asking one is how this stays a question
     * for the projection rather than a formula copied out of it.
     */
    private static Projection radially(ChartProjection kind) {
        return Projections.of(kind, new SkyPosition(0.0, 0.0));
    }

    /** Which direction from the centre a page point is asking for. */
    private static Offset offsetOf(ChartProjection kind, PlanePoint target) {
        double radius = Math.hypot(target.xiEast(), target.etaNorth());
        if (radius == 0.0) {
            return new Offset(1.0, 0.0, 0.0);
        }
        double angle = Math.toRadians(
                radially(kind).angleAtPlaneRadius(radius));
        double across = Math.sin(angle);
        return new Offset(Math.cos(angle),
                across * target.xiEast() / radius,
                across * target.etaNorth() / radius);
    }

    /**
     * The furthest across the page this row can ask for: the
     * horizontal component whose offset is exactly at the feasibility
     * bound, at the vertical the reader is holding.
     *
     * <p>Found by bisection, and that is a smaller admission than it
     * looks. The bound is a statement about the sky - how far east of
     * a near-polar grab a centre can put its own east vector - and
     * where it falls on the paper is whatever the projection's radial
     * law says. For the tangent plane the answer is
     * {@code cot|dec| * sqrt(1 + eta^2)}, which is the closed form
     * this replaced and which it reproduces to the last bit; for the
     * next projection it would be different algebra for the same
     * sentence, which is exactly the copying the projection boundary
     * exists to stop.
     *
     * <p>What is solved afterwards is closed form, and every
     * candidate is verified by full reprojection against the clamped
     * point. This only decides where the boundary is.
     *
     * <p>The eastward part rises with the horizontal component over
     * every page the atlas draws - it turns over only past two plane
     * units under the overview's projection, where the widest rung
     * reaches 1.16 - so the interval below brackets the boundary.
     */
    private static double feasibleAcross(ChartProjection kind,
                                         PlanePoint target, double bound) {
        double wanted = Math.abs(target.xiEast());
        double low = 0.0;
        double high = wanted;
        for (int step = 0; step < 100; step++) {
            double middle = 0.5 * (low + high);
            double east = Math.abs(offsetOf(kind, new PlanePoint(middle,
                    target.etaNorth())).east());
            if (east > bound) {
                high = middle;
            } else {
                low = middle;
            }
        }
        return Math.copySign(low, target.xiEast());
    }
}
