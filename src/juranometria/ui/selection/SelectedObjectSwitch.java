package juranometria.ui.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The selected-object box's one switch (#692, C-7 on #690): read by the
 * box, driven by the Chart Options box in the dialog and in the
 * Controller, and remembered in its own store on every change.
 */
public final class SelectedObjectSwitch {

    private final SelectedObjectStore store;
    private boolean showing;
    private final List<Consumer<Boolean>> followers = new ArrayList<>();

    public SelectedObjectSwitch(SelectedObjectStore store) {
        if (store == null) {
            throw new IllegalArgumentException("a switch remembers itself somewhere");
        }
        this.store = store;
        this.showing = store.shownOrDefault();
    }

    /** Whether the box may be drawn. */
    public boolean showing() {
        return showing;
    }

    /** Turns the box on or off and remembers it. */
    public void show(boolean shown) {
        if (shown == showing) {
            return;
        }
        showing = shown;
        store.save(shown);
        for (Consumer<Boolean> follower : List.copyOf(followers)) {
            follower.accept(shown);
        }
    }

    /** The other way. */
    public void toggle() {
        show(!showing);
    }

    /**
     * Calls {@code follower} with the state now and on every change; the
     * returned action stops it.
     */
    public Runnable follow(Consumer<Boolean> follower) {
        if (follower == null) {
            throw new IllegalArgumentException("a follower is a control");
        }
        followers.add(follower);
        follower.accept(showing);
        return () -> followers.remove(follower);
    }
}
