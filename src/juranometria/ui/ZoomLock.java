package juranometria.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Whether the pointer may change the field (Sprint 38, issue #428):
 * the one state behind the toolbar's zoom lock, the wheel and trackpad
 * handler, and the remembered choice.
 *
 * <p>While locked, continuous pointer input - a mouse wheel, a
 * trackpad's scroll - does not zoom; everything deliberate still does:
 * the toolbar's zoom buttons, View's zoom items and their keys, and a
 * search that chooses a field. Unlocked is the default and the
 * released behaviour. Event-thread state, like the rest of the chart.
 */
public final class ZoomLock {

    private boolean locked;
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();

    /** Whether the pointer is kept from zooming. */
    public boolean locked() {
        return locked;
    }

    /** Locks or unlocks; every listener hears a change once. */
    public void lock(boolean locked) {
        if (this.locked == locked) {
            return;
        }
        this.locked = locked;
        for (Consumer<Boolean> listener : List.copyOf(listeners)) {
            listener.accept(locked);
        }
    }

    /** Hears every change of the lock. */
    public void onChange(Consumer<Boolean> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("a listener is required");
        }
        listeners.add(listener);
    }
}
