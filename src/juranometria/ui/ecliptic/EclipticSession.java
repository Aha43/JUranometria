package juranometria.ui.ecliptic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import juranometria.ecliptic.EclipticModule;
import juranometria.ui.ChartModuleHost;

/**
 * The ecliptic's session (Sprint 28, issue #274): begun by attaching
 * the module, switched through one {@link Switch} that remembers the
 * choice in the store as it is made, and restored from that store at
 * startup.
 *
 * <p>Since #675 the reader's control is the Ecliptic box in Chart
 * Options' Chart checklist - in the dialog and in the Controller - and
 * no longer a menu item. {@code EclipticStore} and this session stay
 * the single persisted authority (ruling 2 on #674): the boxes drive
 * the switch and follow it, so a change made by any route - a box, the
 * chart keyboard's letter - is shown by every box at once, and nothing
 * is written down twice.
 */
public final class EclipticSession {

    private EclipticSession() {
    }

    /**
     * The one switch for the ecliptic: what it shows, how it is
     * toggled, and who follows it. Every route to the ecliptic runs
     * through one of these, which is why the boxes agree.
     */
    public static final class Switch {
        private final EclipticModule module;
        private final EclipticStore store;
        private final List<Consumer<Boolean>> followers =
                new CopyOnWriteArrayList<>();

        private Switch(EclipticModule module, EclipticStore store) {
            this.module = module;
            this.store = store;
        }

        /** Whether the ecliptic is showing now. */
        public boolean showing() {
            return module.showing();
        }

        /** Flips the ecliptic, writes the choice down, and tells every follower. */
        public void toggle() {
            module.showing(!module.showing());
            store.save(module.showing());
            tell();
        }

        /**
         * Adds a control that shows the switch's state; it is told the
         * state at once and on every change. Returns what cancels it.
         */
        public Runnable follow(Consumer<Boolean> follower) {
            if (follower == null) {
                throw new IllegalArgumentException("a follower is a control");
            }
            followers.add(follower);
            follower.accept(module.showing());
            return () -> followers.remove(follower);
        }

        /** How many controls follow this switch. */
        public int followers() {
            return followers.size();
        }

        /**
         * Sets the module from the remembered choice - the released
         * default, hidden, for a reader who never chose - and tells
         * every follower. Refuses when nothing follows: a remembered
         * choice could then draw the ecliptic with no way for a reader
         * to turn it off (#274's rule, carried over from the menu item).
         */
        public void restore() {
            if (followers.isEmpty()) {
                throw new IllegalStateException(
                        "a loaded ecliptic module has a control: without"
                                + " one, a remembered choice could draw"
                                + " the ecliptic with no way for a reader"
                                + " to turn it off");
            }
            module.showing(store.shownOrDefault());
            tell();
        }

        private void tell() {
            boolean showing = module.showing();
            for (Consumer<Boolean> follower : followers) {
                follower.accept(showing);
            }
        }
    }

    /** The switch for an attached module and its store. */
    public static Switch of(EclipticModule module, EclipticStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "the switch needs a module and a store");
        }
        return new Switch(module, store);
    }

    /**
     * The toggle alone, as an action, for a route that needs nothing
     * else of the switch. It follows nobody: a caller with controls to
     * keep in step uses {@link #of} and hands every route the same switch.
     */
    public static Runnable toggle(EclipticModule module,
                                  EclipticStore store) {
        return of(module, store)::toggle;
    }

    public static EclipticModule begin(ChartModuleHost modules) {
        return modules.attach(new EclipticModule());
    }
}
