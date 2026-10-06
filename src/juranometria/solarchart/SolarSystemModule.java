package juranometria.solarchart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.SolarSystemService.SunObservation;

/**
 * The Sun and the Moon on the chart (Sprint 37, issue #415 for the
 * Sun; #416 for the Moon), as one chart module under the #414 ruling.
 *
 * <p>The module owns nothing about the sky: Place and Time owns the
 * observer and the civil instant, and the module reads them through
 * the supplier it is handed - the meridian module's, in the
 * application - when the chart asks it to contribute, never earlier
 * and never from a clock of its own. The pack is read through its own
 * supplier, lazily, so a session that never shows a body never loads
 * the ephemeris. What the module contributes is a
 * {@link OverlayContribution.Body} per body shown: the topocentric
 * astrometric J2000 position the tables show, the true angular
 * diameter, and whether the body stands below the observer's
 * mathematical horizon <em>while that horizon is drawn</em> - the
 * page dims it only then, as ruled (C4), and draws it normally when
 * the horizon is hidden.
 *
 * <p>Not a planet framework (C6): two bodies, one switch each, one
 * seam. No selection, no emphasis, no events, no tracks.
 */
public final class SolarSystemModule implements ChartModule {

    public static final String ID = "solar-system";
    public static final String SUN = "sun";
    public static final String MOON = "moon";

    private final Supplier<Observer> observer;
    private final Supplier<SolarSystemService> service;
    private final BooleanSupplier horizonDrawn;
    private boolean sunShowing;
    private boolean moonShowing;
    private ChartServices services;
    private Runnable withdraw;
    private long contributions;

    /**
     * @param observer Place and Time's observer, or null when it is
     *                 not attached; read at every contribution
     * @param service the Solar System service, loaded on first use
     * @param horizonDrawn whether the mathematical horizon is drawn
     *                     on the page at the moment of contribution
     */
    public SolarSystemModule(Supplier<Observer> observer,
                             Supplier<SolarSystemService> service,
                             BooleanSupplier horizonDrawn) {
        if (observer == null || service == null || horizonDrawn == null) {
            throw new IllegalArgumentException("the module reads an observer,"
                    + " a service and whether the horizon is drawn");
        }
        this.observer = observer;
        this.service = service;
        this.horizonDrawn = horizonDrawn;
    }

    @Override
    public String name() {
        return "Sun and Moon";
    }

    @Override
    public void attach(ChartServices services) {
        if (services == null) {
            throw new IllegalArgumentException(
                    "a module is attached to a chart's services");
        }
        if (this.services != null) {
            throw new IllegalStateException("this module is already attached;"
                    + " attaching twice leaves the first contribution with"
                    + " nothing to withdraw it");
        }
        this.services = services;
        this.withdraw = services.contribute(ID, this::contributedGeometry);
    }

    public boolean attached() {
        return services != null;
    }

    @Override
    public void detach() {
        if (withdraw != null) {
            withdraw.run();
        }
        withdraw = null;
        services = null;
    }

    /** Whether the Sun is drawn. */
    public boolean sunShowing() {
        return sunShowing;
    }

    /** Shows or hides the Sun; the chart redraws, and every listener hears a change. */
    public void sunShowing(boolean showing) {
        boolean changed = this.sunShowing != showing;
        this.sunShowing = showing;
        if (services != null) {
            services.redraw();
        }
        if (changed) {
            for (java.util.function.Consumer<Boolean> listener
                    : java.util.List.copyOf(sunListeners)) {
                listener.accept(showing);
            }
        }
    }

    /** Whether the Moon is drawn. */
    public boolean moonShowing() {
        return moonShowing;
    }

    /** Shows or hides the Moon; the chart redraws, and every listener hears a change. */
    public void moonShowing(boolean showing) {
        boolean changed = this.moonShowing != showing;
        this.moonShowing = showing;
        if (services != null) {
            services.redraw();
        }
        if (changed) {
            for (java.util.function.Consumer<Boolean> listener
                    : java.util.List.copyOf(moonListeners)) {
                listener.accept(showing);
            }
        }
    }

    private final java.util.List<java.util.function.Consumer<Boolean>> sunListeners =
            new java.util.ArrayList<>();
    private final java.util.List<java.util.function.Consumer<Boolean>> moonListeners =
            new java.util.ArrayList<>();

    /**
     * Hears every change of whether the Sun is drawn (Sprint 42, issue
     * #458, ruled on #457): the module is the one authority for the
     * choice, so a menu item, a Controller box and the chart follow it
     * from here and no control surface can bypass another. Told the
     * current state at once.
     */
    public void onSunChange(java.util.function.Consumer<Boolean> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("a listener is required");
        }
        sunListeners.add(listener);
        listener.accept(sunShowing);
    }

    /** Hears every change of whether the Moon is drawn; told the current state at once. */
    public void onMoonChange(java.util.function.Consumer<Boolean> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("a listener is required");
        }
        moonListeners.add(listener);
        listener.accept(moonShowing);
    }

    /** How many times the chart asked, for tests of pull-not-push. */
    public long timesAsked() {
        return contributions;
    }

    /**
     * What the page is offered now: the Sun and the Moon, each if it
     * is shown, at Place and Time's observer and instant; nothing when
     * no observer is attached. Pulled by the chart when it paints.
     *
     * <p>The Moon carries how it is lit exactly as the Moon table
     * states it (#416): the illuminated fraction k, the bright limb's
     * position angle χ from celestial north through east, and the phase
     * angle i. The page turns χ into its own directions at the Moon;
     * nothing here recomputes it.
     */
    public List<OverlayContribution> contributedGeometry() {
        contributions++;
        if (!sunShowing && !moonShowing) {
            return List.of();
        }
        Observer now = observer.get();
        if (now == null) {
            return List.of();
        }
        boolean horizon = horizonDrawn.getAsBoolean();
        List<OverlayContribution> offered = new ArrayList<>();
        if (sunShowing) {
            SunObservation sun = (SunObservation) service.get().observe(Body.SUN, now);
            offered.add(new OverlayContribution.Body(SUN, "Sun",
                    sun.astrometricJ2000(), sun.angularDiameterArcseconds(), null,
                    horizon && sun.horizontal().altitudeDegrees() < 0.0,
                    sun.distanceKm(), InkRole.BODY));
        }
        if (moonShowing) {
            MoonObservation moon = (MoonObservation) service.get().observe(Body.MOON, now);
            offered.add(new OverlayContribution.Body(MOON, "Moon",
                    moon.astrometricJ2000(), moon.angularDiameterArcseconds(),
                    new OverlayContribution.Lit(moon.illuminatedFraction(),
                            moon.brightLimbAngleDegrees(), moon.phaseAngleDegrees()),
                    horizon && moon.horizontal().altitudeDegrees() < 0.0,
                    moon.distanceKm(), InkRole.BODY));
        }
        return List.copyOf(offered);
    }
}
