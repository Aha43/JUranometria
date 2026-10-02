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
 * <p>So they are gathered and handed in - eight since the Sun and the
 * Moon joined the chart (#415, #416) and zoom could be locked (#428).
 * {@link #user()} is the one place that reaches for the reader's
 * nodes, and {@code main} is the one caller of it. A journey that wants to prove the shipping
 * route supplies its own and gets the real
 * {@code JUranometriaMain.start} - the same lines, in the same order,
 * with the same wiring.
 *
 * <p><strong>All of them, or none.</strong> Injecting all but one and
 * letting the last fall through to {@code user()} would leave a journey that
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
                     EclipticStore ecliptic,
                     juranometria.ui.solar.SunChartStore sunChart,
                     juranometria.ui.solar.MoonChartStore moonChart,
                     juranometria.ui.ZoomLockStore zoomLock) {

    StartupStores {
        require(appearance, "appearance");
        require(chartOptions, "chart options");
        require(language, "language");
        require(place, "place and time");
        require(ecliptic, "ecliptic");
        require(sunChart, "sun on the chart");
        require(moonChart, "moon on the chart");
        require(zoomLock, "zoom lock");
    }

    /**
     * The reader's own preferences.
     *
     * <p>The only place the reader's {@code user()} nodes are opened, and
     * called from {@code main} alone.
     */
    static StartupStores user() {
        return new StartupStores(AppearanceStore.user(),
                ChartOptionsStore.user(), SkyLanguageStore.user(),
                PlaceStore.user(), EclipticStore.user(),
                juranometria.ui.solar.SunChartStore.user(),
                juranometria.ui.solar.MoonChartStore.user(),
                juranometria.ui.ZoomLockStore.user());
    }

    private static void require(Object store, String what) {
        if (store == null) {
            throw new IllegalArgumentException("startup needs a " + what
                    + " store: a session that is handed all but one of its"
                    + " preferences and lets the last find its own is not a"
                    + " session"
                    + " anything can account for");
        }
    }
}
