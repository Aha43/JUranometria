package juranometria.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import juranometria.chart.ChartViewState;

/**
 * Holds the current chart-view state and applies its transitions,
 * notifying listeners after every change. Swing-free, so the wiring
 * between controls and state stays testable below the UI; transitions at
 * their bounds are no-ops in the state model and notify nobody.
 */
public final class ChartViewController {

    private ChartViewState state = ChartViewState.DEFAULT;
    private final List<Consumer<ChartViewState>> listeners = new ArrayList<>();

    public ChartViewState state() {
        return state;
    }

    /** Registers a listener and immediately hands it the current state. */
    public void onChange(Consumer<ChartViewState> listener) {
        listeners.add(listener);
        listener.accept(state);
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

    public void reset() {
        update(state.reset());
    }

    private void update(ChartViewState next) {
        if (next.equals(state)) {
            return;
        }
        state = next;
        for (Consumer<ChartViewState> listener : listeners) {
            listener.accept(next);
        }
    }
}
