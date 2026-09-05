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
     * Attaches the module and restores the reader's choice.
     *
     * <p>The store is read exactly once, here. Nothing else in the
     * session consults it, so there is one place where a remembered
     * choice becomes a drawn chart.
     */
    public static EclipticModule begin(ChartModuleHost modules,
                                       EclipticStore store) {
        if (store == null) {
            throw new IllegalArgumentException(
                    "a session restores from a store");
        }
        EclipticModule module = modules.attach(new EclipticModule());
        module.showing(store.shownOrDefault());
        return module;
    }
}
