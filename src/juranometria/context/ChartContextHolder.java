package juranometria.context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import juranometria.sky.Observer;

/**
 * The one owner of the chart's time and place (#612, rulings P6 and
 * P7): a plain object holding the current {@link ChartContext} behind
 * an atomic reference, so a rendering or export thread reads one
 * complete immutable snapshot, never half a change.
 *
 * <p>Installation and notification happen on the event thread, and
 * nowhere else: the observer and the instant change together, as one
 * reference swap, and every listener is told of the whole change on the
 * thread that made it. While a paint or an export holds the context
 * ({@link #hold}), an installation is refused rather than letting the
 * page mix two contexts: one paint reads one snapshot throughout.
 *
 * <p>Place and Time is the only editor; a table's row action and
 * <em>Update to now</em> reach the same {@link #install} and so redraw
 * everything together.
 */
public final class ChartContextHolder {

    /** What a listener is told: the context replaced and the one installed. */
    public record Change(ChartContext previous, ChartContext current) {
        public Change {
            if (previous == null || current == null) {
                throw new IllegalArgumentException("a change has a before and an after");
            }
        }
    }

    /** A registration that ends when cancelled; cancelling twice is once. */
    public interface Subscription {
        void cancel();
    }

    private final AtomicReference<ChartContext> current;
    private final List<Consumer<Change>> listeners = new ArrayList<>();
    private int holds;

    /** Starts at revision zero with the observer the boundary provides. */
    public ChartContextHolder(Observer initial) {
        this.current = new AtomicReference<>(new ChartContext(initial, 0));
    }

    /** The current snapshot, from any thread. */
    public ChartContext current() {
        return current.get();
    }

    /** The current observer, from any thread - the shape every consumer was built over. */
    public Observer observer() {
        return current.get().observer();
    }

    /**
     * Installs a new observer and instant together, as the next
     * revision, and tells every listener on the event thread.
     *
     * @throws IllegalStateException off the event thread, or while a
     *     paint or export holds the context
     */
    public void install(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("the chart is always somewhere, at some instant");
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "chart time and place are installed on the event thread");
        }
        if (holds > 0) {
            throw new IllegalStateException(
                    "chart time and place cannot change while a paint or export reads them");
        }
        ChartContext previous = current.get();
        ChartContext next = new ChartContext(observer, previous.revision() + 1);
        current.set(next);
        Change change = new Change(previous, next);
        for (Consumer<Change> listener : List.copyOf(listeners)) {
            listener.accept(change);
        }
    }

    /**
     * Runs a paint or an export over one snapshot: installations are
     * refused until it returns. Event thread only, like the installs it
     * excludes; nested holds are counted.
     */
    public <T> T hold(java.util.function.Supplier<T> work) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("the context is held on the event thread");
        }
        holds++;
        try {
            return work.get();
        } finally {
            holds--;
        }
    }

    /** Whether a paint or export holds the context now. */
    public boolean held() {
        return holds > 0;
    }

    /** Follows every installation; returns the registration to cancel. */
    public Subscription onChange(Consumer<Change> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("a subscription is somebody listening");
        }
        listeners.add(listener);
        return new Subscription() {
            private boolean live = true;

            @Override
            public void cancel() {
                if (live) {
                    live = false;
                    listeners.remove(listener);
                }
            }
        };
    }

    /** How many subscriptions are live, so "reopening adds no listener" is a claim a test can make. */
    public int subscribers() {
        return listeners.size();
    }
}
