package juranometria.ui.ecliptic;

import juranometria.ecliptic.EclipticModule;
import juranometria.ui.ChartModuleHost;

/**
 * How a session starts the ecliptic module (Sprint 28, issue #274).
 *
 * <p>The one seam that owns session-start policy, so the decision is
 * stated in a place a reader of the code can find rather than hidden
 * in a constructor - the same shape as
 * {@code PlaceAndTimeSession}.
 *
 * <p>The module is attached and then told what the reader last
 * chose. A reader who has never chosen gets the released default,
 * which is <strong>hidden</strong>: installing a removable module
 * must not change the page for someone who never asked for it.
 */
public final class EclipticSession {

    private EclipticSession() {
    }

    /**
     * What the Ecliptic menu item does.
     *
     * <p>Here rather than inline in the application, so the chain a
     * reader actually sets off - item to module to remembered choice
     * - is one named thing that a test can drive (PR #279 review).
     * Rehearsing the three effects separately proves each of them
     * and none of the wiring between them.
     *
     * <p>Two effects and no others: the module shows or stops
     * showing, and the reader's choice is remembered. The page does
     * not move, the selection does not change, no clock is read, and
     * no chart option is touched.
     */
    public static Runnable toggle(EclipticModule module,
                                  EclipticStore store) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "the switch needs a module and a store");
        }
        return () -> {
            module.showing(!module.showing());
            store.save(module.showing());
        };
    }

    /**
     * Attaches the module, showing nothing.
     *
     * <p>Attaching alone must change no page, so the module arrives
     * hidden and stays hidden until {@link #restore} says otherwise.
     */
    public static EclipticModule begin(ChartModuleHost modules) {
        return modules.attach(new EclipticModule());
    }

    /**
     * Turns a remembered choice into both a drawn chart and a shown
     * tick - in one place.
     *
     * <p>The store is read exactly once, here, and both things that
     * have to agree about it are set from that one read. An earlier
     * version restored the module in one statement and the tick in
     * another, so a test could only rehearse the pairing rather than
     * drive it, and deleting either statement left the other looking
     * right (PR #279 re-review).
     *
     * @param item the Ecliptic menu item, or null in an atlas that
     *     has no such item - which is what a build without the
     *     module shows
     */
    public static void restore(EclipticModule module,
                               EclipticStore store,
                               javax.swing.JCheckBoxMenuItem item) {
        if (module == null || store == null) {
            throw new IllegalArgumentException(
                    "a session restores a module from a store");
        }
        module.showing(store.shownOrDefault());
        if (item != null) {
            item.setSelected(module.showing());
        }
    }
}
