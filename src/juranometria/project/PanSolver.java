package juranometria.project;

import java.util.Optional;

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
        double half = Math.toRadians(viewport.fieldWidthDegrees()) / 2.0;
        double pixelsPerPlaneUnit =
                viewport.widthPx() / (2.0 * Math.tan(half));
        return new PlanePoint(
                (viewport.widthPx() / 2.0 - pixel.x()) / pixelsPerPlaneUnit,
                (viewport.heightPx() / 2.0 - pixel.y()) / pixelsPerPlaneUnit);
    }

    /**
     * The exact inverse gnomonic projection: the sky position whose
     * image is the given tangent-plane point, for a chart centred at
     * {@code centre}. Every finite plane point has a pre-image.
     */
    public static SkyPosition skyFromPlane(SkyPosition centre, PlanePoint plane) {
        double xi = plane.xiEast();
        double eta = plane.etaNorth();
        double rho = Math.hypot(xi, eta);
        double alpha0 = Math.toRadians(centre.raDegrees());
        double delta0 = Math.toRadians(centre.decDegrees());
        if (rho == 0.0) {
            return centre;
        }
        double c = Math.atan(rho);
        double sinC = Math.sin(c);
        double cosC = Math.cos(c);
        double dec = Math.asin(cosC * Math.sin(delta0)
                + eta * sinC * Math.cos(delta0) / rho);
        double ra = alpha0 + Math.atan2(xi * sinC,
                rho * Math.cos(delta0) * cosC - eta * Math.sin(delta0) * sinC);
        return new SkyPosition((Math.toDegrees(ra) % 360.0 + 360.0) % 360.0,
                Math.toDegrees(dec));
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
    public static PanSolution solveCentre(SkyPosition grabbed,
                                          PlanePoint target,
                                          SkyPosition previousCentre) {
        double xi = target.xiEast();
        double eta = target.etaNorth();

        double raS = Math.toRadians(grabbed.raDegrees());
        double decS = Math.toRadians(grabbed.decDegrees());
        double sx = Math.cos(decS) * Math.cos(raS);
        double sy = Math.cos(decS) * Math.sin(raS);
        double sz = Math.sin(decS);
        double cosDecS = Math.cos(decS);

        // Feasibility: |s . e| <= cos(decS) with e depending on RA alone
        // requires |xi| <= cot|decS| * sqrt(1 + eta^2). Clamp an
        // infeasible request onto the boundary (with a margin keeping
        // the declination equation strictly solvable) and solve there.
        double margin = 1.0 - 1e-9;
        double bound = margin * (cosDecS / Math.abs(sz))
                * Math.sqrt(1.0 + eta * eta);
        boolean constrained = Math.abs(sz) > 1e-15 && Math.abs(xi) > bound;
        if (constrained) {
            xi = Math.copySign(bound, xi);
        }
        double n = Math.sqrt(1.0 + xi * xi + eta * eta);

        // s . e = xi/N with e = (-sin a, cos a, 0):
        // cos(decS) * sin(raS - a) = xi/N.
        double sinOffset = xi / (n * cosDecS);
        // After the feasibility clamp above this cannot exceed 1 beyond
        // floating-point rounding (|xi|/N < 1 always holds off the polar
        // path); anything larger is a solver invariant violation, never
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

        SkyPosition best = null;
        double bestSeparation = Double.MAX_VALUE;
        boolean sawOutOfRangeDeclination = false;
        java.util.List<SkyPosition> verified = new java.util.ArrayList<>(4);
        for (double alpha : alphaCandidates) {
            // s . c = 1/N: p cos d + sz sin d = 1/N with
            // p = sx cos a + sy sin a.
            double p = sx * Math.cos(alpha) + sy * Math.sin(alpha);
            double amplitude = Math.hypot(p, sz);
            if (amplitude < 1e-15
                    || Math.abs(1.0 / n) > amplitude * (1.0 + 1e-9)) {
                continue;
            }
            double phase = Math.atan2(sz, p);
            double acos = Math.acos(Math.clamp((1.0 / n) / amplitude,
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
                if (!reprojects(candidate, grabbed, xi, eta)) {
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
                                + " evidence", grabbed, xi, eta));
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
    private static boolean reprojects(SkyPosition centre, SkyPosition grabbed,
                                      double xi, double eta) {
        var projection = new GnomonicProjection(centre);
        var plane = projection.project(grabbed);
        return plane.isPresent()
                && Math.abs(plane.get().xiEast() - xi) <= PLANE_TOLERANCE
                && Math.abs(plane.get().etaNorth() - eta) <= PLANE_TOLERANCE;
    }
}
