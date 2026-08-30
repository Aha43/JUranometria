package juranometria.tool;

import java.util.List;
import java.util.Locale;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;

/**
 * The Sprint 14 pointer-zoom study (issue #123): measures, over every
 * adjacent field-width transition and its reverse, whether the
 * existing exact seams - {@link PanSolver#planeFromPixel},
 * {@link PanSolver#skyFromPlane}, and the pan centre solver - keep
 * the sky beneath the pointer fixed as the discrete field changes.
 * Sweeps centre, edge, and corner pointers over equatorial, RA-wrap,
 * high-declination, near-polar, and southern pages; reports pointer
 * drift after each step and after the round trip, classifies
 * constrained and past-pole outcomes explicitly, and times realistic
 * wheel bursts through query-to-pixels against the 16.7 ms frame
 * budget. Run via "make zoom-study".
 */
public final class ZoomStudyMain {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    private ZoomStudyMain() {
    }

    /** One solved pointer-anchored step; the geometry under decision. */
    record Step(PanSolver.PanSolution solution, SkyPosition anchor,
                PlanePoint target) {
    }

    /**
     * The candidate transition: recover the sky under the pointer at
     * the current state, ask where that pixel lands on the new
     * field's tangent plane, and solve the centre that puts the
     * anchor there - entirely through the existing pan seams.
     */
    static Step solve(SkyPosition centre, double fieldDegrees,
                      double newFieldDegrees, PixelPoint pointer) {
        ChartViewport current = new ChartViewport(
                centre, fieldDegrees, WIDTH, HEIGHT);
        SkyPosition anchor = PanSolver.skyFromPlane(centre,
                PanSolver.planeFromPixel(current, pointer));
        ChartViewport zoomed = new ChartViewport(
                centre, newFieldDegrees, WIDTH, HEIGHT);
        PlanePoint target = PanSolver.planeFromPixel(zoomed, pointer);
        return new Step(PanSolver.solveCentre(anchor, target, centre),
                anchor, target);
    }

    /** The decision's stated reversal tolerance (degrees). */
    static final double REVERSAL_TOLERANCE_DEGREES = 1e-4;

    /**
     * The decided acceptance contract (PR #127 follow-up): a
     * pointer-zoom step is accepted only when it is EXACT - not
     * constrained (a measured 220-px anchor miss is not
     * pointer-centred zoom), not past-pole, not ambiguous - AND its
     * reverse at the same pointer is preflighted to be equally exact
     * and to restore the origin within tolerance, so an accepted
     * step can never enter a view the opposite wheel movement
     * refuses. One extra 2-microsecond solve buys the symmetry.
     * Empty means refused: chart unchanged, centre zoom available.
     */
    static java.util.Optional<SkyPosition> acceptStep(
            SkyPosition centre, double fieldDegrees,
            double newFieldDegrees, PixelPoint pointer) {
        Step out = solve(centre, fieldDegrees, newFieldDegrees, pointer);
        if (out.solution().centre().isEmpty()
                || out.solution().constrained()
                || out.solution().ambiguous()) {
            return java.util.Optional.empty();
        }
        SkyPosition mid = out.solution().centre().get();
        Step back = solve(mid, newFieldDegrees, fieldDegrees, pointer);
        if (back.solution().centre().isEmpty()
                || back.solution().constrained()
                || back.solution().ambiguous()
                || back.solution().centre().get().separationDegrees(centre)
                        > REVERSAL_TOLERANCE_DEGREES) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(mid);
    }

    /** The step geometry on a letterboxed paper of the given height. */
    static Step solveOn(SkyPosition centre, double fieldDegrees,
                        double newFieldDegrees, PixelPoint pointer,
                        int heightPx) {
        ChartViewport current = new ChartViewport(
                centre, fieldDegrees, WIDTH, heightPx);
        SkyPosition anchor = PanSolver.skyFromPlane(centre,
                PanSolver.planeFromPixel(current, pointer));
        ChartViewport zoomed = new ChartViewport(
                centre, newFieldDegrees, WIDTH, heightPx);
        PlanePoint target = PanSolver.planeFromPixel(zoomed, pointer);
        return new Step(PanSolver.solveCentre(anchor, target, centre),
                anchor, target);
    }

    static double pointerDriftOn(Step step, double newFieldDegrees,
                                 PixelPoint pointer, int heightPx) {
        SkyPosition newCentre = step.solution().centre().orElseThrow();
        ChartViewport viewport = new ChartViewport(
                newCentre, newFieldDegrees, WIDTH, heightPx);
        PixelPoint landed = new ViewportMapping(viewport).toPixel(
                new GnomonicProjection(newCentre)
                        .project(step.anchor()).orElseThrow());
        return Math.hypot(landed.x() - pointer.x(),
                landed.y() - pointer.y());
    }

    /** Whether a centre places the anchor at the plane point exactly. */
    static boolean solvesExactly(SkyPosition centre, SkyPosition anchor,
                                 PlanePoint target) {
        var plane = new GnomonicProjection(centre).project(anchor);
        return plane.isPresent()
                && Math.abs(plane.get().xiEast() - target.xiEast()) <= 1e-6
                && Math.abs(plane.get().etaNorth() - target.etaNorth()) <= 1e-6;
    }

    /** The pointer's pixel error after an accepted step. */
    static double pointerDriftPx(Step step, double newFieldDegrees,
                                 PixelPoint pointer) {
        SkyPosition newCentre = step.solution().centre().orElseThrow();
        ChartViewport viewport = new ChartViewport(
                newCentre, newFieldDegrees, WIDTH, HEIGHT);
        PixelPoint landed = new ViewportMapping(viewport).toPixel(
                new GnomonicProjection(newCentre)
                        .project(step.anchor()).orElseThrow());
        return Math.hypot(landed.x() - pointer.x(),
                landed.y() - pointer.y());
    }

    public static void main(String[] args) {
        record Page(String name, double ra, double dec) {
        }
        List<Page> pages = List.of(
                new Page("m31-equatorialish", 10.684708, 41.268750),
                new Page("orion-equator", 83.818667, -5.389667),
                new Page("ra-wrap", 0.3, 45.0),
                new Page("dec-60", 37.946619, 60.0),
                new Page("dec-85", 37.946619, 85.0),
                new Page("near-pole", 37.946619, 89.9),
                new Page("crux-south", 186.649563, -63.099093),
                new Page("deep-south", 0.0, -85.0));
        List<PixelPoint> pointers = List.of(
                new PixelPoint(WIDTH / 2.0, HEIGHT / 2.0),
                new PixelPoint(1.0, HEIGHT / 2.0),
                new PixelPoint(WIDTH - 1.0, HEIGHT / 2.0),
                new PixelPoint(WIDTH / 2.0, 1.0),
                new PixelPoint(WIDTH / 2.0, HEIGHT - 1.0),
                new PixelPoint(1.0, 1.0),
                new PixelPoint(WIDTH - 1.0, 1.0),
                new PixelPoint(1.0, HEIGHT - 1.0),
                new PixelPoint(WIDTH - 1.0, HEIGHT - 1.0));
        List<Double> fields = ChartViewState.fieldWidthSteps();

        int accepted = 0;
        int refusedConstrained = 0;
        int refusedPastPole = 0;
        int refusedAmbiguous = 0;
        int refusedPreflight = 0;
        double worstStepDrift = 0.0;
        double worstConstrainedShortfallPx = 0.0;
        double constrainedShortfallSumPx = 0.0;
        double worstRoundTripCentreError = 0.0;
        double worstRoundTripDrift = 0.0;
        String worstCase = "";
        long solveNanos = 0;
        int solves = 0;

        for (Page page : pages) {
            SkyPosition centre = new SkyPosition(page.ra(), page.dec());
            for (int i = 0; i + 1 < fields.size(); i++) {
                for (double[] pair : new double[][] {
                        {fields.get(i), fields.get(i + 1)},
                        {fields.get(i + 1), fields.get(i)}}) {
                    for (PixelPoint pointer : pointers) {
                        long t0 = System.nanoTime();
                        Step out = solve(centre, pair[0], pair[1], pointer);
                        solveNanos += System.nanoTime() - t0;
                        solves++;
                        // The acceptance contract, applied exactly as
                        // production will: only exact, preflight-
                        // reversible steps pass; every refusal is
                        // attributed to its cause.
                        if (out.solution().pastPole()) {
                            refusedPastPole++;
                            continue;
                        }
                        if (out.solution().ambiguous()) {
                            refusedAmbiguous++;
                            continue;
                        }
                        if (out.solution().constrained()) {
                            refusedConstrained++;
                            // Refused BECAUSE the shortfall is visible:
                            // measured to justify the refusal, not to
                            // excuse an acceptance.
                            double shortfall = pointerDriftPx(out,
                                    pair[1], pointer);
                            constrainedShortfallSumPx += shortfall;
                            if (shortfall > worstConstrainedShortfallPx) {
                                worstConstrainedShortfallPx = shortfall;
                            }
                            continue;
                        }
                        SkyPosition mid = out.solution().centre().orElseThrow();
                        Step back = solve(mid, pair[1], pair[0], pointer);
                        boolean reverseExact = back.solution().centre()
                                .isPresent()
                                && !back.solution().constrained()
                                && !back.solution().ambiguous();
                        double centreError = reverseExact
                                ? back.solution().centre().get()
                                        .separationDegrees(centre)
                                : Double.NaN;
                        if (!reverseExact
                                || centreError > REVERSAL_TOLERANCE_DEGREES) {
                            // The preflight: an accepted step must be
                            // immediately reversible at the same
                            // pointer, so the wheel can never enter a
                            // view it cannot leave the same way.
                            refusedPreflight++;
                            continue;
                        }
                        if (acceptStep(centre, pair[0], pair[1], pointer)
                                .isEmpty()) {
                            throw new IllegalStateException(
                                    "acceptance contract disagrees with"
                                            + " the study's own scoring");
                        }
                        if (acceptStep(mid, pair[1], pair[0], pointer)
                                .isEmpty()) {
                            throw new IllegalStateException(
                                    "an accepted step's reverse failed the"
                                            + " acceptance contract - the"
                                            + " symmetry claim would be"
                                            + " false");
                        }
                        accepted++;
                        double drift = pointerDriftPx(out, pair[1], pointer);
                        if (drift > worstStepDrift) {
                            worstStepDrift = drift;
                        }
                        double backDrift = pointerDriftPx(back, pair[0],
                                pointer);
                        if (centreError > worstRoundTripCentreError) {
                            worstRoundTripCentreError = centreError;
                            worstCase = String.format(Locale.ROOT,
                                    "%s %.0f->%.0f deg at (%.0f,%.0f)",
                                    page.name(), pair[0], pair[1],
                                    pointer.x(), pointer.y());
                        }
                        if (backDrift > worstRoundTripDrift) {
                            worstRoundTripDrift = backDrift;
                        }
                    }
                }
            }
        }

        System.out.printf(Locale.ROOT,
                "%d pointer-anchored steps solved over %d pages, every"
                        + " adjacent transition and its reverse, %d pointer"
                        + " positions:%n", solves, pages.size(),
                pointers.size());
        System.out.printf(Locale.ROOT,
                "  ACCEPTED (exact, preflight-reversible) %d; refused:"
                        + " constrained %d, past-pole %d, ambiguous %d,"
                        + " preflight (reverse not exact) %d - no other"
                        + " empties%n",
                accepted, refusedConstrained, refusedPastPole,
                refusedAmbiguous, refusedPreflight);
        System.out.printf(Locale.ROOT,
                "  constrained anchor shortfall, the reason for its"
                        + " refusal: worst %.1f px, mean %.1f px%n",
                worstConstrainedShortfallPx,
                refusedConstrained == 0 ? 0.0
                        : constrainedShortfallSumPx / refusedConstrained);
        System.out.printf(Locale.ROOT,
                "  worst pointer drift after an exact step: %.2e px%n",
                worstStepDrift);
        System.out.printf(Locale.ROOT,
                "  reversal guarantee, by construction over every accepted"
                        + " step: worst centre error %.2e deg, worst"
                        + " pointer drift %.2e px (%s)%n",
                worstRoundTripCentreError, worstRoundTripDrift, worstCase);
        System.out.printf(Locale.ROOT,
                "  mean pure-geometry solve: %.1f us%n",
                solveNanos / 1e3 / solves);

        // Letterbox: a window taller than the projection-sanity page
        // cap letterboxes; the paper is the viewport, the chrome bands
        // anchor no sky. Pointers in chrome are refused by decision
        // (and the event left unconsumed); pointers on the paper of a
        // letterboxed window solve exactly like any page.
        SkyPosition lbCentre = new SkyPosition(37.946619, 85.0);
        int paperHeight = Atlas.assembler().maxPageHeightPx(
                lbCentre, 36.0, WIDTH);
        int windowHeight = paperHeight + 400;
        int chromeRefused = 0;
        int paperExact = 0;
        double worstLetterboxDrift = 0.0;
        for (double y : new double[] {50, windowHeight - 50,
                windowHeight / 2.0, 210, windowHeight - 210}) {
            double paperTop = (windowHeight - paperHeight) / 2.0;
            if (y < paperTop || y >= paperTop + paperHeight) {
                chromeRefused++;
                continue;
            }
            PixelPoint onPaper = new PixelPoint(300.0, y - paperTop);
            Step step = solveOn(lbCentre, 36.0, 24.0, onPaper,
                    paperHeight);
            if (step.solution().centre().isPresent()
                    && !step.solution().constrained()
                    && !step.solution().ambiguous()) {
                paperExact++;
                worstLetterboxDrift = Math.max(worstLetterboxDrift,
                        pointerDriftOn(step, 24.0, onPaper, paperHeight));
            }
        }
        System.out.printf(Locale.ROOT,
                "letterbox (window %dx%d, paper height %d): %d chrome"
                        + " pointers refused by decision, %d paper pointers"
                        + " solved exactly (worst drift %.2e px)%n",
                WIDTH, windowHeight, paperHeight, chromeRefused, paperExact,
                worstLetterboxDrift);

        // Realistic burst: five notches outward from the searched-star
        // page, each notch a full accepted transition through
        // query-to-pixels - state, assembly, render.
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        ChartViewState state = new ChartViewState(
                new SkyPosition(83.818667, -5.389667), 6.0, 8.0, null, null);
        // Warm the tiles and the renderer once.
        renderer.renderToImage(Atlas.assembler().assemble(state, WIDTH, HEIGHT));
        System.out.println("burst: five outward notches, pointer at"
                + " (300,200), full query-to-pixels per notch:");
        PixelPoint pointer = new PixelPoint(300.0, 200.0);
        for (int notch = 0; notch < 5 && state.canZoomOut(); notch++) {
            long t0 = System.nanoTime();
            double next = state.zoomOut().fieldWidthDegrees();
            Step out = solve(state.centre(), state.fieldWidthDegrees(),
                    next, pointer);
            SkyPosition centre = out.solution().centre().orElseThrow();
            long t1 = System.nanoTime();
            state = state.recenteredAt(centre).withFieldWidth(next);
            ChartScene scene = Atlas.assembler().assemble(state, WIDTH, HEIGHT);
            long t2 = System.nanoTime();
            renderer.renderToImage(scene);
            long t3 = System.nanoTime();
            System.out.printf(Locale.ROOT,
                    "  -> %2.0f deg: solve %6.3f ms, assemble %6.2f ms,"
                            + " render %6.2f ms, total %6.2f ms (budget"
                            + " 16.7 ms)%n",
                    next, (t1 - t0) / 1e6, (t2 - t1) / 1e6, (t3 - t2) / 1e6,
                    (t3 - t0) / 1e6);
        }
    }
}
