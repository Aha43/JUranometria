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
