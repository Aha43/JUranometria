package juranometria.ui.solar;

import juranometria.jovianchart.JovianModule;

/**
 * The remembered switch for Jupiter and its moons on the chart (issue
 * #484), shaped like the Moon's ({@link MoonChartSession}): the View
 * menu's item toggles it, the Controller's and the dialog's boxes show
 * it, and every one of them follows the module, the single visibility
 * authority (#458).
 */
public final class JovianChartSession {

    private JovianChartSession() {
    }

    /** The menu item's action: flip the switch and remember it. */
    public static Runnable toggle(JovianModule module, JovianChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("the switch needs a module and a store");
        }
        return () -> {
            module.showing(!module.showing());
            store.save(module.showing());
        };
    }

    /** At startup: the remembered choice, and the menu item following the module from here on. */
    public static void restore(JovianModule module, JovianChartStore store,
                               javax.swing.JCheckBoxMenuItem item) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("a session restores a module from a store");
        }
        if (item == null) {
            throw new IllegalArgumentException("a loaded Jupiter switch has a"
                    + " control: without one, a remembered choice could draw"
                    + " Jupiter with no way for a reader to turn it off");
        }
        module.showing(store.shownOrDefault());
        module.onChange(item::setSelected);
    }

    /** The switch as a body layer for the Controller's box and Centre on chart. */
    public static BodyOnChart switchOf(JovianModule module, JovianChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("the switch needs a module and a store");
        }
        return new BodyOnChart() {
            @Override
            public boolean showing() {
                return module.showing();
            }

            @Override
            public void show(boolean shown) {
                if (module.showing() != shown) {
                    module.showing(shown);
                    store.save(shown);
                }
            }

            @Override
            public void onChange(java.util.function.Consumer<Boolean> listener) {
                module.onChange(listener);
            }
        };
    }
}
