package juranometria.ui.solar;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartModuleHost;

/**
 * The Sun on the chart, as a session (Sprint 37, issue #415): the one
 * seam that attaches the Solar System module, switches the Sun, and
 * restores the reader's last choice - the shape the ecliptic's
 * session has, so the two switches behave alike.
 */
public final class SunChartSession {

    private SunChartSession() {
    }

    /**
     * Attaches the module. The observer is Place and Time's, read
     * through the supplier at every paint and never copied; the pack
     * loads on first use; the horizon's state is the meridian
     * module's.
     */
    public static SolarSystemModule begin(ChartModuleHost modules,
                                          Supplier<Observer> observer,
                                          Supplier<SolarSystemService> service,
                                          BooleanSupplier horizonDrawn) {
        if (modules == null) {
            throw new IllegalArgumentException("a session attaches to a host");
        }
        return modules.attach(new SolarSystemModule(observer, service,
                horizonDrawn));
    }

    /** The switch: flips the Sun and remembers the choice. */
    public static Runnable toggle(SolarSystemModule module, SunChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "the switch needs a module and a store");
        }
        return () -> {
            module.sunShowing(!module.sunShowing());
            store.save(module.sunShowing());
        };
    }

    /** Restores the last choice into the module and its menu item together. */
    public static void restore(SolarSystemModule module, SunChartStore store,
                               javax.swing.JCheckBoxMenuItem item) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "a session restores a module from a store");
        }
        if (item == null) {
            throw new IllegalArgumentException("a loaded Sun switch has a"
                    + " control: without one, a remembered choice could draw"
                    + " the Sun with no way for a reader to turn it off");
        }
        module.sunShowing(store.shownOrDefault());
        // The item follows the module from here on (#458): a Controller
        // box or a journey that switches the Sun is shown by the tick.
        module.onSunChange(item::setSelected);
    }

    /**
     * The Sun's chart switch as a control sees it (#458, ruled on
     * #457): the module's own state, asked through the toggle that
     * remembers the choice, followed from the module.
     */
    public static BodyOnChart switchOf(SolarSystemModule module, SunChartStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException("the switch needs a module and a store");
        }
        return new BodyOnChart() {
            @Override
            public boolean showing() {
                return module.sunShowing();
            }

            @Override
            public void show(boolean shown) {
                if (module.sunShowing() != shown) {
                    module.sunShowing(shown);
                    store.save(shown);
                }
            }

            @Override
            public void onChange(java.util.function.Consumer<Boolean> listener) {
                module.onSunChange(listener);
            }
        };
    }
}
