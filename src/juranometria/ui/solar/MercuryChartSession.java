package juranometria.ui.solar;

import juranometria.solarchart.SolarSystemModule;

/**
 * Mercury's switch (Sprint 50, issue #542): the same Solar System
 * module {@link SunChartSession} attached, a second switch on it, and
 * the reader's last choice restored - the Sun's shape, so the two
 * switches behave alike and neither knows about the other.
 */
public final class MercuryChartSession {

    private MercuryChartSession() {
    }

    /** The switch: flips Mercury and remembers the choice. */
    public static Runnable toggle(SolarSystemModule module, MercuryChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "the switch needs a module and a store");
        }
        return () -> {
            module.mercuryShowing(!module.mercuryShowing());
            store.save(module.mercuryShowing());
        };
    }

    /** Restores the last choice into the module and its menu item together. */
    public static void restore(SolarSystemModule module, MercuryChartStore store,
                               javax.swing.JCheckBoxMenuItem item) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "a session restores a module from a store");
        }
        if (item == null) {
            throw new IllegalArgumentException("a loaded Mercury switch has a"
                    + " control: without one, a remembered choice could draw"
                    + " Mercury with no way for a reader to turn it off");
        }
        module.mercuryShowing(store.shownOrDefault());
        // The item follows the module from here on (#458).
        module.onMercuryChange(item::setSelected);
    }

    /** Mercury's chart switch as a control sees it (#458, ruled on #457). */
    public static BodyOnChart switchOf(SolarSystemModule module, MercuryChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("the switch needs a module and a store");
        }
        return new BodyOnChart() {
            @Override
            public boolean showing() {
                return module.mercuryShowing();
            }

            @Override
            public void show(boolean shown) {
                if (module.mercuryShowing() != shown) {
                    module.mercuryShowing(shown);
                    store.save(shown);
                }
            }

            @Override
            public void onChange(java.util.function.Consumer<Boolean> listener) {
                module.onMercuryChange(listener);
            }
        };
    }
}
