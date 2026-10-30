package juranometria.ui.solar;

import juranometria.solarchart.SolarSystemModule;

/**
 * Neptune's switch (issue #659): the same Solar System
 * module {@link SunChartSession} attached, a second switch on it, and
 * the reader's last choice restored - the Sun's shape, so the two
 * switches behave alike and neither knows about the other.
 */
public final class NeptuneChartSession {

    private NeptuneChartSession() {
    }

    /** The switch: flips Neptune and remembers the choice. */
    public static Runnable toggle(SolarSystemModule module, NeptuneChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "the switch needs a module and a store");
        }
        return () -> {
            module.neptuneShowing(!module.neptuneShowing());
            store.save(module.neptuneShowing());
        };
    }

    /** Restores the last choice into the module and its menu item together. */
    public static void restore(SolarSystemModule module, NeptuneChartStore store,
                               javax.swing.JCheckBoxMenuItem item) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "a session restores a module from a store");
        }
        if (item == null) {
            throw new IllegalArgumentException("a loaded Neptune switch has a"
                    + " control: without one, a remembered choice could draw"
                    + " Neptune with no way for a reader to turn it off");
        }
        module.neptuneShowing(store.shownOrDefault());
        // The item follows the module from here on (#458).
        module.onNeptuneChange(item::setSelected);
    }

    /** Neptune's chart switch as a control sees it (#458, ruled on #457). */
    public static BodyOnChart switchOf(SolarSystemModule module, NeptuneChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("the switch needs a module and a store");
        }
        return new BodyOnChart() {
            @Override
            public boolean showing() {
                return module.neptuneShowing();
            }

            @Override
            public void show(boolean shown) {
                if (module.neptuneShowing() != shown) {
                    module.neptuneShowing(shown);
                    store.save(shown);
                }
            }

            @Override
            public void onChange(java.util.function.Consumer<Boolean> listener) {
                module.onNeptuneChange(listener);
            }
        };
    }
}
