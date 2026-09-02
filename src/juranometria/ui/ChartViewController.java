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
                        grabbed, target, state.centre());
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
                                     boolean zoomIn) {
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
                solveExactReversible(state.centre(),
                        state.fieldWidthDegrees(),
                        centred.fieldWidthDegrees(), pointer);
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
    private static java.util.Optional<juranometria.chart.SkyPosition>
            solveExactReversible(juranometria.chart.SkyPosition centre,
                                 double fieldDegrees, double newFieldDegrees,
                                 juranometria.project.PlanePoint pointer) {
        juranometria.chart.SkyPosition anchor =
                juranometria.project.PanSolver.skyFromPlane(centre, pointer);
        double scale = Math.tan(Math.toRadians(newFieldDegrees) / 2.0)
                / Math.tan(Math.toRadians(fieldDegrees) / 2.0);
        juranometria.project.PlanePoint target =
                new juranometria.project.PlanePoint(
                        pointer.xiEast() * scale, pointer.etaNorth() * scale);
        var out = juranometria.project.PanSolver.solveCentre(
                anchor, target, centre);
        if (out.centre().isEmpty() || out.constrained() || out.ambiguous()) {
            return java.util.Optional.empty();
        }
        juranometria.chart.SkyPosition mid = out.centre().get();
        juranometria.chart.SkyPosition anchorAgain =
                juranometria.project.PanSolver.skyFromPlane(mid, target);
        var back = juranometria.project.PanSolver.solveCentre(
                anchorAgain, pointer, mid);
        if (back.centre().isEmpty() || back.constrained()
                || back.ambiguous()
                || back.centre().get().separationDegrees(centre)
                        > POINTER_ZOOM_REVERSAL_TOLERANCE_DEGREES) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(mid);
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
