package juranometria.ui.solar;

import juranometria.solarchart.SolarSystemModule;

/**
 * Mars's switch (issue #606): the same Solar System
 * module {@link SunChartSession} attached, a second switch on it, and
 * the reader's last choice restored - the Sun's shape, so the two
 * switches behave alike and neither knows about the other.
 */
public final class MarsChartSession {

    private MarsChartSession() {
    }

    /** The switch: flips Mars and remembers the choice. */
    public static Runnable toggle(SolarSystemModule module, MarsChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "the switch needs a module and a store");
        }
        return () -> {
            module.marsShowing(!module.marsShowing());
            store.save(module.marsShowing());
        };
    }

    /** Restores the last choice into the module and its menu item together. */
    public static void restore(SolarSystemModule module, MarsChartStore store,
                               javax.swing.JCheckBoxMenuItem item) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "a session restores a module from a store");
        }
        if (item == null) {
            throw new IllegalArgumentException("a loaded Mars switch has a"
                    + " control: without one, a remembered choice could draw"
                    + " Mars with no way for a reader to turn it off");
        }
        module.marsShowing(store.shownOrDefault());
        // The item follows the module from here on (#458).
        module.onMarsChange(item::setSelected);
    }

    /** Mars's chart switch as a control sees it (#458, ruled on #457). */
    public static BodyOnChart switchOf(SolarSystemModule module, MarsChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("the switch needs a module and a store");
        }
        return new BodyOnChart() {
            @Override
            public boolean showing() {
                return module.marsShowing();
            }

            @Override
            public void show(boolean shown) {
                if (module.marsShowing() != shown) {
                    module.marsShowing(shown);
                    store.save(shown);
                }
            }

            @Override
            public void onChange(java.util.function.Consumer<Boolean> listener) {
                module.onMarsChange(listener);
            }
        };
    }
}
