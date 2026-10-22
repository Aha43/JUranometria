package juranometria.saturnchart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.SaturnObservation;

/**
 * Saturn on the chart (Sprint 53, issue #579), through the module the
 * owner ruled on #578: one module and one remembered switch, <em>Saturn
 * on the chart</em>, off by default, shaped like Jupiter's
 * ({@code JovianModule}, #484): Saturn's figure - its globe and its rings
 * - and, under the same switch, the seven ruled moons, each a 3 px symbol
 * decided on its own by the page against the drawn figure (ruling 3 on
 * #578), with its state, named but never selectable (ruling 5).
 *
 * <p>Saturn is offered as an {@link OverlayContribution.RingedBody} at
 * its astrometric J2000 place, with its true equatorial and polar
 * diameters (the polar one at the ring opening, the contract's), its
 * pole derived in the chart's own frame
 * ({@link SaturnSystemService#poleAngleJ2000Degrees}), the planetocentric
 * ring opening B and whether the unlit face is seen, the ruled ring
 * radii, and Jupiter's ruled {@value #MINIMUM_MARK_PX} px minimum mark:
 * the page draws the larger of that and the figure, measured by the
 * rings' major axis, and says when it is a symbol.
 *
 * <p>Like the Sun, the Moon and Jupiter (C5 of #414): one instant per
 * page, the observer's, read when the page asks and never kept. Outside
 * the numbers' years (1900-2100) nothing is offered; the table keeps
 * its refusal.
 */
public final class SaturnModule implements ChartModule {

    /** The module's registry id; the system's bodies are named under {@code saturn}. */
    public static final String ID = "saturn.system";
    /**
     * Saturn's identity on the page (ruling 5 on #578): the page's
     * language names it from {@code page.body.saturn}; its moons will be
     * {@code saturn.mimas} and so on.
     */
    public static final String SATURN = "saturn";
    /** Ruling 1 on #578: Saturn's mark is Jupiter's 6 px minimum or its true figure, whichever is larger. */
    public static final double MINIMUM_MARK_PX = juranometria.jovianchart.JovianModule.MINIMUM_MARK_PX;
    /** Ruling 3 on #578: each moon is Jupiter's 3 px symbol, never its apparent diameter. */
    public static final double MOON_MARK_PX = juranometria.jovianchart.JovianModule.MOON_MARK_PX;
    /**
     * The order a collision between moons of the same state is settled in
     * (ruling 3 on #578, the #578 study's section B): the larger moon
     * keeps its mark.
     */
    static final List<SaturnSystemService.Moon> PRECEDENCE = List.of(
            SaturnSystemService.Moon.TITAN, SaturnSystemService.Moon.RHEA,
            SaturnSystemService.Moon.IAPETUS, SaturnSystemService.Moon.DIONE,
            SaturnSystemService.Moon.TETHYS, SaturnSystemService.Moon.ENCELADUS,
            SaturnSystemService.Moon.MIMAS);

    /** A moon's identity on the page, {@code saturn.mimas} and so on (ruling 5 on #578). */
    public static String identityOf(SaturnSystemService.Moon moon) {
        return SATURN + "." + moon.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The table's moon names, identical in both languages (ruling 4 on #578). */
    public static String nameOf(SaturnSystemService.Moon moon) {
        return switch (moon) {
            case MIMAS -> "Mimas";
            case ENCELADUS -> "Enceladus";
            case TETHYS -> "Tethys";
            case DIONE -> "Dione";
            case RHEA -> "Rhea";
            case TITAN -> "Titan";
            case IAPETUS -> "Iapetus";
        };
    }

    private final Supplier<Observer> observer;
    private final Supplier<SaturnSystemService> service;
    private final BooleanSupplier horizonDrawn;
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();
    private boolean showing;
    private ChartServices services;
    private Runnable withdraw;
    private long contributions;

    public SaturnModule(Supplier<Observer> observer,
                        Supplier<SaturnSystemService> service,
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
        return "Saturn";
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

    /** Whether Saturn is drawn. */
    public boolean showing() {
        return showing;
    }

    /**
     * Shows or hides Saturn; the chart redraws, and every control
     * following the module hears of a real change (#458's single
     * visibility authority).
     */
    public void showing(boolean showing) {
        boolean changed = this.showing != showing;
        this.showing = showing;
        if (services != null) {
            services.redraw();
        }
        if (changed) {
            for (Consumer<Boolean> listener : List.copyOf(listeners)) {
                listener.accept(showing);
            }
        }
    }

    /** Follows the switch from now on, told its current state at once. */
    public void onChange(Consumer<Boolean> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("a listener is required");
        }
        listeners.add(listener);
        listener.accept(showing);
    }

    /** How many times the page has asked for this module's geometry. */
    public long timesAsked() {
        return contributions;
    }

    /** What the page draws now: nothing while hidden, outside the years, or with no observer. */
    public List<OverlayContribution> contributedGeometry() {
        contributions++;
        if (!showing) {
            return List.of();
        }
        Observer now = observer.get();
        if (now == null) {
            return List.of();
        }
        SaturnSystemService saturnSystem = service.get();
        SaturnObservation saturn;
        try {
            saturn = saturnSystem.observeSaturn(now);
        } catch (IllegalArgumentException outsideTheYears) {
            // The table says why; the chart draws nothing it has no
            // numbers for (1900-2100 inclusive).
            return List.of();
        }
        boolean below = horizonDrawn.getAsBoolean()
                && saturn.horizontal().altitudeDegrees() < 0.0;
        List<OverlayContribution> offered = new ArrayList<>();
        offered.add(new OverlayContribution.RingedBody(SATURN, "Saturn",
                saturn.astrometricJ2000(), saturn.equatorialDiameterArcseconds(),
                saturn.polarDiameterArcseconds(), saturnSystem.poleAngleJ2000Degrees(saturn),
                saturn.ringOpeningDegrees(), saturn.unlitFaceSeen(), saturn.ringRadiiKm(),
                MINIMUM_MARK_PX, below, saturn.distanceKm(), InkRole.BODY,
                OverlayContribution.Moment.of(now), true));
        // The seven ruled moons (ruling 3 on #578), where they have numbers
        // (2000-2100): each at its own astrometric J2000 place with its
        // disc and shadow states, for the page to decide moon by moon
        // against the drawn figure. From 1900 to 1999 Saturn is drawn
        // alone; the table says why.
        SaturnSystemService.Configuration moons;
        try {
            moons = saturnSystem.observeMoons(now);
        } catch (IllegalArgumentException outsideTheMoonsYears) {
            return List.copyOf(offered);
        }
        for (SaturnSystemService.MoonPlace moon : moons.moons()) {
            OverlayContribution.Satellite.Relation relation = switch (moon.discRelation()) {
                case CLEAR -> OverlayContribution.Satellite.Relation.CLEAR;
                case IN_FRONT -> OverlayContribution.Satellite.Relation.IN_FRONT;
                case BEHIND -> OverlayContribution.Satellite.Relation.BEHIND;
            };
            offered.add(new OverlayContribution.Satellite(identityOf(moon.moon()),
                    nameOf(moon.moon()), moon.astrometricJ2000(), SATURN, relation,
                    moon.shadowRelation() != SaturnSystemService.ShadowRelation.SUNLIT,
                    moon.shadowRelation() == SaturnSystemService.ShadowRelation.PARTLY_IN_SHADOW,
                    PRECEDENCE.indexOf(moon.moon()), MOON_MARK_PX, below, InkRole.BODY,
                    // Ruling 5 on #578: the moons are not selectable.
                    OverlayContribution.Moment.of(now), false));
        }
        return List.copyOf(offered);
    }
}
