package juranometria.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Whether gestures accumulate (Sprint 27, issues #258/#260):
 * session interaction state, deliberately not membership — the
 * #258 gate's "small observable mode holder beside the model" — so
 * the chart, the table and the visible Accumulate control read one
 * switch. The platform's additive modifier works regardless of it.
 *
 * <p>Session-only like the selection itself: never persisted, off
 * at every start.
 */
public final class SelectionMode {

    private final List<Consumer<Boolean>> listeners = new ArrayList<>();
    private boolean accumulate;

    /** Whether gestures currently accumulate. */
    public boolean accumulate() {
        return accumulate;
    }

    /** Turns accumulation on or off; listeners hear real changes. */
    public void accumulate(boolean on) {
        if (this.accumulate == on) {
            return;
        }
        this.accumulate = on;
        for (Consumer<Boolean> listener : List.copyOf(listeners)) {
            listener.accept(on);
        }
    }

    /**
     * Subscribes a consumer, tells it the current state, and
     * returns the handle that releases it.
     */
    public Runnable onChange(Consumer<Boolean> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
        listener.accept(accumulate);
        return () -> listeners.remove(listener);
    }
}
