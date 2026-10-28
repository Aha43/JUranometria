package juranometria.ui.solar;

import juranometria.solarchart.SolarSystemModule;

/**
 * Uranus's switch (issue #638): the same Solar System
 * module {@link SunChartSession} attached, a second switch on it, and
 * the reader's last choice restored - the Sun's shape, so the two
 * switches behave alike and neither knows about the other.
 */
public final class UranusChartSession {

    private UranusChartSession() {
    }

    /** The switch: flips Uranus and remembers the choice. */
    public static Runnable toggle(SolarSystemModule module, UranusChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "the switch needs a module and a store");
        }
        return () -> {
            module.uranusShowing(!module.uranusShowing());
            store.save(module.uranusShowing());
        };
    }

    /** Restores the last choice into the module and its menu item together. */
    public static void restore(SolarSystemModule module, UranusChartStore store,
                               javax.swing.JCheckBoxMenuItem item) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "a session restores a module from a store");
        }
        if (item == null) {
            throw new IllegalArgumentException("a loaded Uranus switch has a"
                    + " control: without one, a remembered choice could draw"
                    + " Uranus with no way for a reader to turn it off");
        }
        module.uranusShowing(store.shownOrDefault());
        // The item follows the module from here on (#458).
        module.onUranusChange(item::setSelected);
    }

    /** Uranus's chart switch as a control sees it (#458, ruled on #457). */
    public static BodyOnChart switchOf(SolarSystemModule module, UranusChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("the switch needs a module and a store");
        }
        return new BodyOnChart() {
            @Override
            public boolean showing() {
                return module.uranusShowing();
            }

            @Override
            public void show(boolean shown) {
                if (module.uranusShowing() != shown) {
                    module.uranusShowing(shown);
                    store.save(shown);
                }
            }

            @Override
            public void onChange(java.util.function.Consumer<Boolean> listener) {
                module.onUranusChange(listener);
            }
        };
    }
}
