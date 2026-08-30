package juranometria.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import juranometria.render.ChartOptions;

/**
 * The production chart-options state and its transitions, kept out of
 * Swing (issue #104): holds the current immutable {@link ChartOptions},
 * notifies listeners on every change (the repaint path), and owns the
 * dialog protocol the decision specifies - live preview via
 * {@link #apply}, {@link #restoreDefaults} as an ordinary previewed
 * transition, {@link #revertTo} for Cancel/Escape back to an
 * open-time snapshot, and {@link #confirm} persisting the current
 * value through the injected store. Options are presentation state:
 * no navigation, no scene, no queries.
 */
public final class ChartOptionsController {

    private final ChartOptionsStore store;
    private final List<Consumer<ChartOptions>> listeners = new ArrayList<>();
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

    /** Registers a listener and immediately hands it the current options. */
    public void onChange(Consumer<ChartOptions> listener) {
        listeners.add(listener);
        listener.accept(options);
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
        for (Consumer<ChartOptions> listener : listeners) {
            listener.accept(next);
        }
    }

    /** Restore Defaults: the released chart, as an ordinary preview. */
    public void restoreDefaults() {
        apply(ChartOptions.DEFAULTS);
    }

    /** Cancel/Escape: back to the snapshot captured at dialog open. */
    public void revertTo(ChartOptions snapshot) {
        apply(snapshot);
    }

    /** OK: persists the current, previewed options for future launches. */
    public void confirm() {
        store.save(options);
    }
}
