package juranometria.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * One switch for the Inspector, however a reader reaches it (Sprint
 * 20, issue #180).
 *
 * <p>The toolbar button and the View menu item are two controls over
 * <strong>one state</strong>, and the window may close the panel
 * without either being touched. This is the seam that keeps all three
 * telling the same story: controls ask it to flip and are told what
 * actually happened, rather than each keeping its own idea of whether
 * the panel is there.
 *
 * <p>It deliberately knows nothing about panels, windows, or widths.
 * The application binds it to the inspector it owns; a control binds
 * to it and is told two things - whether the panel is
 * <em>showing</em>, and whether it <em>could</em> show at the current
 * width. A control that cannot be honoured says so rather than
 * claiming a panel that is not there.
 */
public final class InspectorToggle {

    /** What a control is told: what is, and what is possible. */
    public record State(boolean showing, boolean available) {
    }

    private final List<Consumer<State>> listeners = new ArrayList<>();
    private Runnable request = () -> { };
    private BooleanSupplier available = () -> true;
    private boolean showing;

    /**
     * Binds the switch to the thing it switches: what to run when a
     * reader asks for a flip, and how to ask whether the panel could
     * be shown at all right now.
     */
    public void bind(Runnable requestToggle, BooleanSupplier canShow) {
        this.request = requestToggle == null ? () -> { } : requestToggle;
        this.available = canShow == null ? () -> true : canShow;
    }

    /** A reader asked, from whichever control they reached for. */
    public void toggle() {
        request.run();
    }

    /** What actually happened; the panel reports this, not a control. */
    public void report(boolean nowShowing) {
        this.showing = nowShowing;
        State state = state();
        for (Consumer<State> listener : List.copyOf(listeners)) {
            listener.accept(state);
        }
    }

    public boolean isShowing() {
        return showing;
    }

    public State state() {
        return new State(showing, available.getAsBoolean());
    }

    /**
     * Subscribes a control, telling it the current state at once so
     * it never starts out lying about the panel.
     */
    public void onChange(Consumer<State> listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        listener.accept(state());
    }
}
