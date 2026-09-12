package juranometria.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import juranometria.chart.ChartViewState;

/**
 * Holds the current chart-view state and applies its transitions,
 * notifying listeners after every change. Swing-free, so the wiring
 * between controls and state stays testable below the UI.
 *
 * The controller is the navigation boundary where coverage governs every
 * transition: a proposed state failing the validity predicate is refused
 * before any notification, exactly like a bounds no-op, and the
 * {@code can*} queries consult the same predicate so controls disable
 * instead of offering a transition that would be refused.
 */
public final class ChartViewController {

    private final Predicate<ChartViewState> isViewValid;
    private ChartViewState state = ChartViewState.DEFAULT;
    private final List<Consumer<ChartViewState>> listeners = new ArrayList<>();

    /** A controller that accepts every state; for tests without coverage. */
    public ChartViewController() {
        this(anyState -> true);
    }

    /** @param isViewValid the shared coverage predicate, e.g. assembler::fits */
    public ChartViewController(Predicate<ChartViewState> isViewValid) {
        if (isViewValid == null) {
            throw new IllegalArgumentException("validity predicate must not be null");
        }
        this.isViewValid = isViewValid;
    }

    public ChartViewState state() {
        return state;
    }

    /** Registers a listener and immediately hands it the current state. */
    public void onChange(Consumer<ChartViewState> listener) {
        listeners.add(listener);
        listener.accept(state);
    }

    public boolean canZoomIn() {
        return state.canZoomIn() && isViewValid.test(state.zoomIn());
    }

    public boolean canZoomOut() {
        return state.canZoomOut() && isViewValid.test(state.zoomOut());
    }

    public boolean canDecreaseMagnitudeLimit() {
        return state.canDecreaseMagnitudeLimit()
                && isViewValid.test(state.decreaseMagnitudeLimit());
    }

    public boolean canIncreaseMagnitudeLimit() {
        return state.canIncreaseMagnitudeLimit()
                && isViewValid.test(state.increaseMagnitudeLimit());
    }

    public void zoomIn() {
        update(state.zoomIn());
    }

    public void zoomOut() {
        update(state.zoomOut());
    }

    public void decreaseMagnitudeLimit() {
        update(state.decreaseMagnitudeLimit());
    }

    public void increaseMagnitudeLimit() {
        update(state.increaseMagnitudeLimit());
    }

    /** Moves the chart centre anonymously; the chart titles by position. */
    public void recenter(juranometria.chart.SkyPosition centre) {
        update(state.recenteredAt(centre));
    }

    /** Recentres on a named target whose label titles the chart. */
    public void recenter(juranometria.chart.SkyPosition centre, String targetLabel,
                         String targetIdentity) {
        update(state.recenteredAt(centre, targetLabel, targetIdentity));
    }

    /** Anonymous recenter with a field change in one notification. */
    public void recenter(juranometria.chart.SkyPosition centre, double fieldWidthDegrees) {
        update(state.recenteredAt(centre).withFieldWidth(fieldWidthDegrees));
    }

    /** Named-target recenter with a field change in one notification. */
    public void recenter(juranometria.chart.SkyPosition centre, double fieldWidthDegrees,
                         String targetLabel, String targetIdentity) {
        update(state.recenteredAt(centre, targetLabel, targetIdentity)
                .withFieldWidth(fieldWidthDegrees));
    }

    /**
     * One atomic pan transition per docs/decisions/pan-navigation.md:
     * solves the new centre for which {@code grabbed} - the sky
     * position under the pointer at press time - sits at the pointer's
     * current tangent-plane point, and applies it as a single
     * anonymous recenter. Field width and limiting magnitude are
     * untouched; the searched target's label and identity clear
     * together on the first accepted pan (the atomic rule), so the
     * chart titles honestly by its coordinates; already-anonymous
     * views stay anonymous. A past-pole hold or a centre the coverage
     * predicate refuses changes nothing and notifies nobody.
     *
     * The caller (the UI issue) is responsible for the drag threshold:
     * pointer jitter below it must never reach this transition.
     *
     * @return true when the pan was accepted and applied
     */
    /**
     * Retires the searched target where it stands (Sprint 23, issue
     * #196): the label and the identity clear together, the centre,
     * field width and limiting magnitude are untouched, and the chart
     * titles honestly by its coordinates.
     *
     * <p>The same atomic rule panning already uses, for the same
     * reason. A reader who switches off the family their target
     * belongs to has asked for it to go; leaving it drawn because it
     * is the target answers a question the reader did not ask, and
     * leaves a galaxy on a chart whose galaxies are switched off with
     * nothing on the surface to explain it. Leaving the page is what
     * this shares with panning - not losing the place reached.
     *
     * @return true when a target was retired; false when there was
     *         none, and nobody is notified
     */
    public boolean retireTarget() {
        if (state.targetIdentity() == null) {
            return false;
        }
        update(state.recenteredAt(state.centre()));
        return true;
    }

    public boolean pan(juranometria.chart.SkyPosition grabbed,
                       juranometria.project.PlanePoint target) {
        juranometria.project.PanSolver.PanSolution solution =
                juranometria.project.PanSolver.solveCentre(
                        state.projection(), grabbed, target,
                        state.centre());
        if (solution.centre().isEmpty()) {
            return false;
        }
        ChartViewState panned = state.recenteredAt(solution.centre().get());
        if (panned.equals(state) || !isViewValid.test(panned)) {
            return false;
        }
        update(panned);
        return true;
    }

    /**
     * A classified pointer-zoom outcome, sufficient for the
     * interaction layer and tests without message parsing: the step
     * was applied; the field sequence has no next step; the pointer's
     * geometry refuses the acceptance contract (constrained,
     * past-pole, ambiguous, or a preflighted reverse that is not
     * exact); or the candidate state failed the coverage predicate.
     */
    public enum PointerZoomOutcome {
        ACCEPTED, AT_BOUND, INFEASIBLE_POINTER, REFUSED_COVERAGE
    }

    /**
     * The reviewed reversal tolerance for the preflight
     * (docs/decisions/pointer-zoom.md): the reverse of an accepted
     * step must restore the origin this closely.
     */
    static final double POINTER_ZOOM_REVERSAL_TOLERANCE_DEGREES = 1e-4;

    /**
     * One atomic pointer-anchored zoom step per
     * docs/decisions/pointer-zoom.md: the sky beneath the pointer
     * stays beneath the pointer as the field takes one discrete step.
     * The caller supplies the pointer's tangent-plane point at the
     * CURRENT field (the paper is the viewport; letterbox chrome
     * never reaches this transition), exactly as pan does.
     *
     * The acceptance contract: the forward solve must be exact - not
     * constrained, not past-pole, not ambiguous - and the reverse at
     * the same pointer is preflighted to be equally exact and to
     * restore this centre within the stated tolerance, so an
     * accepted step can never enter a view the opposite movement
     * refuses. Anything else changes nothing and notifies nobody.
     *
     * Target honesty: the target survives exactly when the centre
     * survives - a step that moves the centre is an anonymous
     * recenter under the atomic pan rule; a pointer on the exact
     * page centre degenerates to the toolbar's centre-preserving
     * transition and keeps the target. Applied as one state change,
     * one notification.
     */
    public PointerZoomOutcome zoomAt(juranometria.project.PlanePoint pointer,
                                     boolean zoomIn, int widthPx,
                                     int heightPx) {
        if (zoomIn ? !state.canZoomIn() : !state.canZoomOut()) {
            return PointerZoomOutcome.AT_BOUND;
        }
        ChartViewState centred = zoomIn ? state.zoomIn() : state.zoomOut();
        if (pointer.xiEast() == 0.0 && pointer.etaNorth() == 0.0) {
            // The exact page centre: the anchor IS the centre and the
            // solve degenerates to the toolbar's centre-preserving
            // transition (keeping the target), free of the radian
            // round-trip's last-bit noise.
            if (!isViewValid.test(centred)) {
                return PointerZoomOutcome.REFUSED_COVERAGE;
            }
            update(centred);
            return PointerZoomOutcome.ACCEPTED;
        }
        java.util.Optional<juranometria.chart.SkyPosition> solved =
                solveExactReversible(state.projection(),
                        centred.projection(), state.centre(),
                        state.fieldWidthDegrees(),
                        centred.fieldWidthDegrees(), pointer,
                        widthPx, heightPx);
        if (solved.isEmpty()) {
            return PointerZoomOutcome.INFEASIBLE_POINTER;
        }
        ChartViewState candidate = solved.get().equals(state.centre())
                ? centred
                : state.recenteredAt(solved.get())
                        .withFieldWidth(centred.fieldWidthDegrees());
        if (!isViewValid.test(candidate)) {
            return PointerZoomOutcome.REFUSED_COVERAGE;
        }
        update(candidate);
        return PointerZoomOutcome.ACCEPTED;
    }

    /**
     * The gate's acceptance geometry (the reference implementation
     * measured by make zoom-study): recover the anchor, scale the
     * pointer's plane point to the new field (the plane offset of a
     * fixed pixel scales by tan(f'/2)/tan(f/2)), solve the centre
     * exactly, and preflight the exact reverse.
     */
    /**
     * How the plane stretches between two fields.
     *
     * <p>Asked of the projection. Written as a ratio of tangents this
     * was the tangent plane's answer given for every projection, in
     * the one place a reader notices most: this ratio is exactly what
     * keeps the star under the pointer while the field changes.
     *
     * <p>Package-private so that it can be checked the other way
     * round - against the scales two viewports actually draw at,
     * which is a different route to the same number and does not
     * agree with a wrong one.
     */
    static double zoomScale(juranometria.project.Projection projection,
                            double fieldDegrees, double newFieldDegrees,
                            int widthPx, int heightPx) {
        return zoomScale(projection, projection, fieldDegrees,
                newFieldDegrees, widthPx, heightPx);
    }

    /**
     * The same, between two pages that are not drawn by the same
     * projection.
     *
     * <p>Which is one step of the ladder: 42 degrees is the tangent
     * plane's widest and 60 is the overview's narrowest, so a reader
     * zooming out of the sheet page crosses it. Each half of the
     * ratio belongs to the page it describes - how far out the old
     * page put that pixel, and how far out the new page puts it -
     * and using one projection for both is a step that lands the sky
     * up to twenty pixels from the pointer that asked for it.
     */
    static double zoomScale(juranometria.project.Projection from,
                            juranometria.project.Projection to,
                            double fieldDegrees, double newFieldDegrees,
                            int widthPx, int heightPx) {
        // Asked of the two pages rather than of the two fields. What
        // this ratio has to be is "the plane point of the same
        // pixel", and a pixel becomes a plane point through the
        // page's own scale - which was half the field across half the
        // width for every page the atlas drew, so the two were the
        // same number and the shorter one was written.
        //
        // A globe is scaled by its disc instead: ninety per cent of
        // the page's short side, whatever field it names
        // (docs/decisions/celestial-globe.md). Written as a ratio of
        // fields, a step onto the globe moved the sky out from under
        // the pointer that asked for it - not because the solve was
        // wrong but because the target was the wrong plane point
        // (#329).
        return scaleOf(from, fieldDegrees, widthPx, heightPx)
                / scaleOf(to, newFieldDegrees, widthPx, heightPx);
    }

    /** How many pixels one plane unit is on a page of this shape. */
    private static double scaleOf(juranometria.project.Projection projection,
                                  double fieldDegrees, int widthPx,
                                  int heightPx) {
        return new juranometria.project.ViewportMapping(
                new juranometria.chart.ChartViewport(projection.centre(),
                        fieldDegrees, widthPx, heightPx,
                        juranometria.chart.ChartProjection.forField(
                                fieldDegrees)),
                projection).pixelsPerPlaneUnit();
    }

    /**
     * Package-private, so that a test can ask it about a projection
     * no reader can reach yet.
     *
     * <p>Nothing offers the overview until issue #299, so the pointer
     * zoom cannot be driven with one through the controller's own
     * surface - and the scale this works out is exactly where a
     * review found the tangent plane's answer given for every
     * projection. A promise that cannot be checked is a promise
     * waiting to be broken, so it is checked here directly.
     */
    static java.util.Optional<juranometria.chart.SkyPosition>
            solveExactReversible(juranometria.chart.ChartProjection fromKind,
                                 juranometria.chart.ChartProjection toKind,
                                 juranometria.chart.SkyPosition centre,
                                 double fieldDegrees, double newFieldDegrees,
                                 juranometria.project.PlanePoint pointer,
                                 int widthPx, int heightPx) {
        // Two pages, each read by its own projection. The pointer is
        // a plane point on the page being left; the target is the
        // same pixel on the page being entered, which is a different
        // plane point whenever the two are drawn differently. Reading
        // both through one of them is the fault this whole package
        // exists to prevent, and it survived here until the ladder
        // had a rung where the projection changes.
        juranometria.project.Projection from =
                juranometria.project.Projections.of(fromKind, centre);
        juranometria.project.Projection to =
                juranometria.project.Projections.of(toKind, centre);
        // A pointer on paper anchors nothing, which is the same
        // refusal as a pointer whose solve is ambiguous - the wheel
        // visibly does nothing rather than inventing a star to zoom
        // towards (#301).
        java.util.Optional<juranometria.chart.SkyPosition> under =
                juranometria.project.PanSolver.skyAt(fromKind, centre,
                        pointer);
        if (under.isEmpty()) {
            return java.util.Optional.empty();
        }
        juranometria.chart.SkyPosition anchor = under.get();
        double scale = zoomScale(from, to, fieldDegrees, newFieldDegrees,
                widthPx, heightPx);
        juranometria.project.PlanePoint target =
                new juranometria.project.PlanePoint(
                        pointer.xiEast() * scale, pointer.etaNorth() * scale);
        // The same question of the page being entered, and before
        // the solve rather than after it: a pointer that is sky on
        // this page can scale to a plane point that is paper on the
        // next, and the solver asked about paper answers with a NaN
        // centre rather than with a refusal. Refusing here is what
        // stops a step onto a globe becoming a page centred nowhere.
        if (juranometria.project.PanSolver.skyAt(toKind, centre, target)
                .isEmpty()) {
            return java.util.Optional.empty();
        }
        var out = juranometria.project.PanSolver.solveCentre(
                toKind, anchor, target, centre);
        if (out.centre().isEmpty() || out.constrained()
                || switchedBranch(out, centre, anchor)) {
            return java.util.Optional.empty();
        }
        juranometria.chart.SkyPosition mid = out.centre().get();
        java.util.Optional<juranometria.chart.SkyPosition> againAt =
                juranometria.project.PanSolver.skyAt(toKind, mid, target);
        if (againAt.isEmpty()) {
            return java.util.Optional.empty();
        }
        juranometria.chart.SkyPosition anchorAgain = againAt.get();
        var back = juranometria.project.PanSolver.solveCentre(
                fromKind, anchorAgain, pointer, mid);
        if (back.centre().isEmpty() || back.constrained()
                || switchedBranch(back, mid, anchorAgain)
                || back.centre().get().separationDegrees(centre)
                        > POINTER_ZOOM_REVERSAL_TOLERANCE_DEGREES) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(mid);
    }

    /**
     * Whether a two-branch solve returned a centre the step cannot
     * have reached: a jump rather than a zoom.
     *
     * <p>The centre equation has two exact roots on a page whose
     * pointer anchors sky far from the centre, and the solver returns
     * the one nearest the previous centre. For a drag's small
     * increments that continuity tie-break is right and panning keeps
     * it; a zoom step is a large jump on which it could silently
     * switch branches, and the reviewed rule was to refuse every
     * ambiguous step outright (docs/decisions/pointer-zoom.md).
     *
     * <p>That rule was measured on pages up to 36 degrees wide, where
     * the only ambiguous pointers were near-polar ones anchoring sky
     * beyond the pole. At the overview's fields it fires on ordinary
     * pointers - a corner of a 120-degree page anchors sky 72 degrees
     * from the centre, and the second root is on the far side of the
     * sky - so keeping it would have quietly stopped the wheel
     * working on exactly the pages #299 adds, while the gate promised
     * that navigation keeps working because it is the same operation.
     *
     * <p><strong>What this enforces, exactly:</strong> the accepted
     * centre is no further from the previous one than the anchor is.
     * That is a bound on the <em>length</em> of the step and nothing
     * more - it is a rule against teleporting, not a rule about
     * direction.
     *
     * <p>It is stated that narrowly on purpose, because a review
     * found the wider claim it first carried - that the centre moves
     * "towards the anchor and never past it" - to be either unenforced
     * or unenforceable here. The direction is not available to
     * discriminate: every candidate the solver returns has already
     * been verified by full reprojection, so <em>every</em> one of
     * them puts the anchor at exactly the offset the target asks for.
     * A wrong root is wrong about where the page ends up, not about
     * where the anchor lands. What separates it from the right one is
     * how far the centre had to travel, which is what is measured.
     *
     * <p>The bound is not a tolerance: the anchor's own offset is the
     * distance the centre would move if the pointer ended at the
     * page's middle, so it is the largest honest step this gesture
     * has. It refuses the near-polar case the reviewed rule was
     * written for, where the two roots straddle the pole and the far
     * one lies beyond it.
     */
    private static boolean switchedBranch(
            juranometria.project.PanSolver.PanSolution solved,
            juranometria.chart.SkyPosition from,
            juranometria.chart.SkyPosition anchor) {
        if (!solved.ambiguous() || solved.centre().isEmpty()) {
            return false;
        }
        return solved.centre().get().separationDegrees(from)
                > anchor.separationDegrees(from)
                        + juranometria.project.PanSolver
                                .AMBIGUITY_SEPARATION_DEGREES;
    }

    public void reset() {
        update(state.reset());
    }

    private void update(ChartViewState next) {
        if (next.equals(state) || !isViewValid.test(next)) {
            return;
        }
        state = next;
        for (Consumer<ChartViewState> listener : listeners) {
            listener.accept(next);
        }
    }
}
