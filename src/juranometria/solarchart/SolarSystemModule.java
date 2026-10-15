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
    /**
     * Venus's identity on the page (#514): the service's own name for the
     * body, lower-cased - derived, so the language ledger gains no row
     * for a word that is a key and not a word (as #501's pack keys).
     */
    public static final String VENUS = Body.VENUS.name().toLowerCase(java.util.Locale.ROOT);
    /**
     * Rule M (ruling 1 on #512): Venus is drawn as the larger of its true
     * solid-body disc and a mark of this size, phased either way; a
     * cartographic symbol, never a claim about apparent size.
     */
    public static final double VENUS_MINIMUM_MARK_PX = 6.0;
    /** The page's state keys for Venus meeting the Sun's disc (ruling 3 on #512). */
    public static final String VENUS_IN_FRONT = "venus.inFront";
    public static final String VENUS_BEHIND = "venus.behind";
    /** Mercury's identity on the page (#542), derived as Venus's is. */
    public static final String MERCURY = Body.MERCURY.name().toLowerCase(java.util.Locale.ROOT);
    /** Rule M for Mercury (ruling 1 on #539): Venus's mark, the same size. */
    public static final double MERCURY_MINIMUM_MARK_PX = VENUS_MINIMUM_MARK_PX;
    /** The page's state keys for Mercury meeting the Sun's disc (ruling 2 on #539). */
    public static final String MERCURY_IN_FRONT = MERCURY + VENUS_IN_FRONT.substring(VENUS.length());
    public static final String MERCURY_BEHIND = MERCURY + VENUS_BEHIND.substring(VENUS.length());

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

    private boolean venusShowing;
    private final java.util.List<java.util.function.Consumer<Boolean>> venusListeners =
            new java.util.ArrayList<>();
    private boolean mercuryShowing;
    private final java.util.List<java.util.function.Consumer<Boolean>> mercuryListeners =
            new java.util.ArrayList<>();

    public boolean mercuryShowing() {
        return mercuryShowing;
    }

    /** Mercury on the chart (#542): its own switch, independent of every other body's. */
    public void mercuryShowing(boolean showing) {
        boolean changed = this.mercuryShowing != showing;
        this.mercuryShowing = showing;
        if (services != null) {
            services.redraw();
        }
        if (changed) {
            for (java.util.function.Consumer<Boolean> listener
                    : java.util.List.copyOf(mercuryListeners)) {
                listener.accept(showing);
            }
        }
    }

    public void onMercuryChange(java.util.function.Consumer<Boolean> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("a listener is required");
        }
        mercuryListeners.add(listener);
        listener.accept(mercuryShowing);
    }

    public boolean venusShowing() {
        return venusShowing;
    }

    /** Venus on the chart (#514): its own switch, independent of the Sun's and the Moon's. */
    public void venusShowing(boolean showing) {
        boolean changed = this.venusShowing != showing;
        this.venusShowing = showing;
        if (services != null) {
            services.redraw();
        }
        if (changed) {
            for (java.util.function.Consumer<Boolean> listener
                    : java.util.List.copyOf(venusListeners)) {
                listener.accept(showing);
            }
        }
    }

    public void onVenusChange(java.util.function.Consumer<Boolean> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("a listener is required");
        }
        venusListeners.add(listener);
        listener.accept(venusShowing);
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
        if (!sunShowing && !moonShowing && !venusShowing && !mercuryShowing) {
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
                    sun.distanceKm(), InkRole.BODY,
                    OverlayContribution.Moment.of(now), true));
        }
        if (mercuryShowing) {
            // Mercury (#542, the rulings on #539): Venus's form exactly - the
            // solid body's disc from the equatorial radius, phased as the
            // Mercury table states it, over the same 6 px mark (rule M),
            // selectable as an occurrence, its state spoken when its disc
            // meets the Sun's. Outside its years the chart draws nothing.
            try {
                juranometria.solar.MercuryService.MercuryObservation mercury =
                        (juranometria.solar.MercuryService.MercuryObservation)
                                service.get().observe(Body.MERCURY, now);
                String state = switch (mercury.phase()) {
                    case IN_FRONT_OF_SUN -> MERCURY_IN_FRONT;
                    case BEHIND_SUN -> MERCURY_BEHIND;
                    default -> null;
                };
                offered.add(new OverlayContribution.Body(MERCURY, "Mercury",
                        mercury.astrometricJ2000(), mercury.solidBodyDiameterArcseconds(),
                        new OverlayContribution.Lit(mercury.illuminatedFraction(),
                                mercury.brightLimbAngleDegrees(), mercury.phaseAngleDegrees()),
                        horizon && mercury.horizontal().altitudeDegrees() < 0.0,
                        mercury.distanceKm(), InkRole.BODY,
                        OverlayContribution.Moment.of(now), true, MERCURY_MINIMUM_MARK_PX,
                        state));
            } catch (IllegalArgumentException outsideTheYears) {
                // Nothing for this instant.
            }
        }
        if (venusShowing) {
            // Venus (#514, the rulings on #512): the solid body's disc,
            // phased exactly as the Venus table states it - k, χ of date
            // and i - over the 6 px minimum mark, selectable as an
            // occurrence (#513), its geometric state spoken when its disc
            // meets the Sun's. Outside its years the table says why; the
            // chart draws nothing it has no numbers for.
            try {
                juranometria.solar.VenusService.VenusObservation venus =
                        (juranometria.solar.VenusService.VenusObservation)
                                service.get().observe(Body.VENUS, now);
                String state = switch (venus.phase()) {
                    case IN_FRONT_OF_SUN -> VENUS_IN_FRONT;
                    case BEHIND_SUN -> VENUS_BEHIND;
                    default -> null;
                };
                offered.add(new OverlayContribution.Body(VENUS, "Venus",
                        venus.astrometricJ2000(), venus.solidBodyDiameterArcseconds(),
                        new OverlayContribution.Lit(venus.illuminatedFraction(),
                                venus.brightLimbAngleDegrees(), venus.phaseAngleDegrees()),
                        horizon && venus.horizontal().altitudeDegrees() < 0.0,
                        venus.distanceKm(), InkRole.BODY,
                        OverlayContribution.Moment.of(now), true, VENUS_MINIMUM_MARK_PX,
                        state));
            } catch (IllegalArgumentException outsideTheYears) {
                // Nothing for this instant.
            }
        }
        if (moonShowing) {
            MoonObservation moon = (MoonObservation) service.get().observe(Body.MOON, now);
            offered.add(new OverlayContribution.Body(MOON, "Moon",
                    moon.astrometricJ2000(), moon.angularDiameterArcseconds(),
                    new OverlayContribution.Lit(moon.illuminatedFraction(),
                            moon.brightLimbAngleDegrees(), moon.phaseAngleDegrees()),
                    horizon && moon.horizontal().altitudeDegrees() < 0.0,
                    moon.distanceKm(), InkRole.BODY,
                    OverlayContribution.Moment.of(now), true));
        }
        return List.copyOf(offered);
    }
}
