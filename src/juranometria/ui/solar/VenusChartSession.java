package juranometria.ui.solar;

import juranometria.solarchart.SolarSystemModule;

/**
 * Venus's switch (Sprint 48, issue #514): the same Solar System
 * module {@link SunChartSession} attached, a second switch on it, and
 * the reader's last choice restored - the Sun's shape, so the two
 * switches behave alike and neither knows about the other.
 */
public final class VenusChartSession {

    private VenusChartSession() {
    }

    /** The switch: flips Venus and remembers the choice. */
    public static Runnable toggle(SolarSystemModule module, VenusChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "the switch needs a module and a store");
        }
        return () -> {
            module.venusShowing(!module.venusShowing());
            store.save(module.venusShowing());
        };
    }

    /** Restores the last choice into the module and its menu item together. */
    public static void restore(SolarSystemModule module, VenusChartStore store,
                               javax.swing.JCheckBoxMenuItem item) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "a session restores a module from a store");
        }
        if (item == null) {
            throw new IllegalArgumentException("a loaded Venus switch has a"
                    + " control: without one, a remembered choice could draw"
                    + " Venus with no way for a reader to turn it off");
        }
        module.venusShowing(store.shownOrDefault());
        // The item follows the module from here on (#458).
        module.onVenusChange(item::setSelected);
    }

    /** Venus's chart switch as a control sees it (#458, ruled on #457). */
    public static BodyOnChart switchOf(SolarSystemModule module, VenusChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("the switch needs a module and a store");
        }
        return new BodyOnChart() {
            @Override
            public boolean showing() {
                return module.venusShowing();
            }

            @Override
            public void show(boolean shown) {
                if (module.venusShowing() != shown) {
                    module.venusShowing(shown);
                    store.save(shown);
                }
            }

            @Override
            public void onChange(java.util.function.Consumer<Boolean> listener) {
                module.onVenusChange(listener);
            }
        };
    }
}
