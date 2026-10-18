package juranometria.ui.placeandtime;

import java.time.Instant;
import java.util.function.Supplier;

import juranometria.meridian.MeridianModule;

/**
 * <em>Update to now</em>: the one action that reads the clock (Sprint
 * 51, issue #259, rulings 4 and 7 on #556).
 *
 * <p>One instance for the running atlas, and every route reaches it -
 * the Place and Time panel's button, the chart's Place and Time box by
 * pointer, keyboard and assistive technology, the chart keyboard's W
 * and the Controller's Solar System tab. Each activation reads the
 * clock <strong>exactly once</strong> and replaces Place and Time's
 * frozen chart instant with what it read; the place, the page and every
 * panel's table are left as they were. Nothing follows the clock
 * afterwards: there is no timer here and no other read.
 */
public final class UpdateToNow implements Runnable {

    private final MeridianModule module;
    private final Supplier<Instant> clock;

    /**
     * @param clock read once per activation and at no other moment -
     *     passed in, because a clock this owned could not be counted or
     *     held still by a test
     */
    public UpdateToNow(MeridianModule module, Supplier<Instant> clock) {
        if (module == null || clock == null) {
            throw new IllegalArgumentException(
                    "updating to now replaces a module's instant from a clock");
        }
        this.module = module;
        this.clock = clock;
    }

    /** Reads the clock once and freezes the chart at what it read. */
    @Override
    public void run() {
        module.observer(module.observer().at(clock.get()));
    }
}
