package juranometria.ui.solar;

import java.util.function.Consumer;

/**
 * One body's "on the chart" choice, as a control sees it (Sprint 42,
 * issue #458, ruled on #457): whether the body is shown, a way to ask
 * for a change, and a way to be told what happened. The authority
 * behind it is the Solar System module, which notifies every control
 * from there (ruling 5A), so a menu item, a Controller box and the
 * chart cannot disagree; a test stands a fake in its place.
 */
public interface BodyOnChart {

    /** Whether the body is shown on the chart now. */
    boolean showing();

    /** Asks for the body to be shown or hidden; the answer comes back through the listener. */
    void show(boolean shown);

    /** Told the current state at once, and every change after. */
    void onChange(Consumer<Boolean> listener);
}
