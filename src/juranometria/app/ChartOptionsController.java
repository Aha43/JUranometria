package juranometria.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import juranometria.render.ChartOptions;

/**
 * The production chart-options state and its transitions, kept out of
 * Swing (issue #104): holds the current immutable {@link ChartOptions}
 * and notifies listeners on every change (the repaint path, and every
 * presentation of the options). Options are presentation state: no
 * navigation, no scene, no queries.
 *
 * <p><strong>Shared immediate</strong> (ruled on #442, #443): every
 * accepted change is the chart at once and is saved at once, whoever
 * makes it - a box in the dialog or the companion, a letter on the
 * chart keyboard, Restore Defaults. There is no preview to take back
 * and no snapshot: the dialog's old Cancel wrote the value it opened
 * with over every change made since, including the keyboard's, and a
 * presentation that can only ever write the current value with one
 * field changed cannot do that.
 */
public final class ChartOptionsController {

    private final ChartOptionsStore store;
    private final List<Consumer<ChartOptions>> listeners = new ArrayList<>();

    /** A registration that ends when cancelled; cancelling twice is once. */
    public interface Subscription {
        void cancel();
    }
    private ChartOptions options;

    /** Starts from the persisted options (defaults when none exist). */
    public ChartOptionsController(ChartOptionsStore store) {
        if (store == null) {
            throw new IllegalArgumentException("options store is required");
        }
        this.store = store;
        this.options = store.load();
    }

    public ChartOptions options() {
        return options;
    }

    /**
     * Registers a listener and immediately hands it the current options;
     * the returned subscription ends it, so a presentation that closes
     * lets go (#443).
     */
    public Subscription onChange(Consumer<ChartOptions> listener) {
        if (listener == null) {
            throw new IllegalArgumentException(
                    "a subscription is somebody listening");
        }
        listeners.add(listener);
        listener.accept(options);
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

    /** How many subscriptions are live, so "no duplicate listeners" can be held. */
    public int subscribers() {
        return listeners.size();
    }

    /** The live-preview transition: one notification per real change. */
    public void apply(ChartOptions next) {
        if (next == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        if (next.equals(options)) {
            return;
        }
        options = next;
        for (Consumer<ChartOptions> listener : List.copyOf(listeners)) {
            listener.accept(next);
        }
    }

    /**
     * One accepted change: the chart at once, and saved at once. What
     * every box, letter and Restore Defaults does (#443).
     */
    public void accept(ChartOptions next) {
        apply(next);
        confirm();
    }

    /**
     * Restore Defaults: the released chart, accepted and saved - asked
     * for first by the presentation that offers it, because nothing
     * takes it back.
     */
    public void restoreDefaults() {
        accept(ChartOptions.DEFAULTS);
    }

    /** Persists the current options for future launches. */
    public void confirm() {
        store.save(options);
    }
}
