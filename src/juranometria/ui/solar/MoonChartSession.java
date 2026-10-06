package juranometria.ui.solar;

import juranometria.solarchart.SolarSystemModule;

/**
 * The Moon's switch (Sprint 37, issue #416): the same Solar System
 * module {@link SunChartSession} attached, a second switch on it, and
 * the reader's last choice restored - the Sun's shape, so the two
 * switches behave alike and neither knows about the other.
 */
public final class MoonChartSession {

    private MoonChartSession() {
    }

    /** The switch: flips the Moon and remembers the choice. */
    public static Runnable toggle(SolarSystemModule module, MoonChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "the switch needs a module and a store");
        }
        return () -> {
            module.moonShowing(!module.moonShowing());
            store.save(module.moonShowing());
        };
    }

    /** Restores the last choice into the module and its menu item together. */
    public static void restore(SolarSystemModule module, MoonChartStore store,
                               javax.swing.JCheckBoxMenuItem item) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "a session restores a module from a store");
        }
        if (item == null) {
            throw new IllegalArgumentException("a loaded Moon switch has a"
                    + " control: without one, a remembered choice could draw"
                    + " the Moon with no way for a reader to turn it off");
        }
        module.moonShowing(store.shownOrDefault());
        // The item follows the module from here on (#458).
        module.onMoonChange(item::setSelected);
    }

    /** The Moon's chart switch as a control sees it (#458, ruled on #457). */
    public static BodyOnChart switchOf(SolarSystemModule module, MoonChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("the switch needs a module and a store");
        }
        return new BodyOnChart() {
            @Override
            public boolean showing() {
                return module.moonShowing();
            }

            @Override
            public void show(boolean shown) {
                if (module.moonShowing() != shown) {
                    module.moonShowing(shown);
                    store.save(shown);
                }
            }

            @Override
            public void onChange(java.util.function.Consumer<Boolean> listener) {
                module.onMoonChange(listener);
            }
        };
    }
}
