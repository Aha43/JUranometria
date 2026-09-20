package juranometria.app;

import juranometria.ui.ecliptic.EclipticStore;
import juranometria.ui.language.SkyLanguageStore;
import juranometria.ui.placeandtime.PlaceStore;

/**
 * Every preference the application reads at startup, named in one
 * place (Sprint 33, issue #350).
 *
 * <p>Startup read five preference nodes by calling {@code user()} five
 * times, scattered through {@code start}. That is correct for the
 * shipping application and closed it to proof: a test cannot execute
 * the real composition without writing to the reader's own
 * preferences, which the evidence gate forbids and which would be
 * wrong even if it did not.
 *
 * <p>So the five are gathered and handed in. {@link #user()} is the
 * one place that reaches for the reader's nodes, and {@code main} is
 * the one caller of it. A journey that wants to prove the shipping
 * route supplies its own five and gets the real
 * {@code JUranometriaMain.start} - the same lines, in the same order,
 * with the same wiring.
 *
 * <p><strong>All five, or none.</strong> Injecting four and letting
 * the fifth fall through to {@code user()} would leave a journey that
 * reads a real reader preference and reports on a session it did not
 * fully determine. A missing store is refused here rather than
 * defaulted, so that cannot happen quietly.
 *
 * <p>This is a bundle, not a service. It holds no policy, decides
 * nothing, and exists so the argument list of one package-private
 * method stays readable.
 */
record StartupStores(AppearanceStore appearance,
                     ChartOptionsStore chartOptions,
                     SkyLanguageStore language,
                     PlaceStore place,
                     EclipticStore ecliptic) {

    StartupStores {
        require(appearance, "appearance");
        require(chartOptions, "chart options");
        require(language, "language");
        require(place, "place and time");
        require(ecliptic, "ecliptic");
    }

    /**
     * The reader's own preferences.
     *
     * <p>The only place all five {@code user()} nodes are opened, and
     * called from {@code main} alone.
     */
    static StartupStores user() {
        return new StartupStores(AppearanceStore.user(),
                ChartOptionsStore.user(), SkyLanguageStore.user(),
                PlaceStore.user(), EclipticStore.user());
    }

    private static void require(Object store, String what) {
        if (store == null) {
            throw new IllegalArgumentException("startup needs a " + what
                    + " store: a session that reads four preferences and"
                    + " lets the fifth find its own is not a session"
                    + " anything can account for");
        }
    }
}
