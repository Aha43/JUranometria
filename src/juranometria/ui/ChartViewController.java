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
