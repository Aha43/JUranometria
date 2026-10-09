package juranometria.jovianchart;

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
import juranometria.solar.JovianSystemService;
import juranometria.solar.JovianSystemService.JupiterObservation;

/**
 * Jupiter on the chart (issue #484), through the module the owner ruled
 * on #482: one module and one remembered switch, <em>Jupiter and moons
 * on the chart</em>, off by default. This issue draws Jupiter alone; the
 * four Galilean moons (#485) extend this module and this switch, each
 * moon decided on its own, so one crowded pair never suppresses the
 * whole system.
 *
 * <p>Jupiter is offered as an {@link OverlayContribution.OblateBody} at
 * its astrometric J2000 place, with its true equatorial and polar
 * diameters, its pole derived in the chart's own frame
 * ({@link JovianSystemService#poleAngleJ2000Degrees}), and the ruled
 * {@value #MINIMUM_MARK_PX} px minimum mark: the page draws the larger
 * of that and the true disc, and says when it is a symbol.
 *
 * <p>Like the Sun and the Moon (C5 of #414): one instant per page, the
 * observer's, read when the page asks and never kept. Outside the
 * numbers' years (1900-2100) nothing is offered; the table keeps its
 * refusal. From 1900 to 1999 Jupiter is drawn and the moons, which have
 * no numbers there, will not be.
 */
public final class JovianModule implements ChartModule {

    /** The module's registry id; the system's bodies are named under {@code jovian.}. */
    public static final String ID = "jovian.system";
    /**
     * Jupiter's identity on the page, namespaced so the four Galilean
     * moons (#485) can never be mistaken for the Moon: the page's
     * language names it from {@code page.body.jovian.jupiter}.
     */
    public static final String JUPITER = "jovian.jupiter";
    /** Ruling 2 on #482: Jupiter's mark is this or its true size, whichever is larger. */
    public static final double MINIMUM_MARK_PX = 6.0;

    private final Supplier<Observer> observer;
    private final Supplier<JovianSystemService> service;
    private final BooleanSupplier horizonDrawn;
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();
    private boolean showing;
    private ChartServices services;
    private Runnable withdraw;
    private long contributions;

    public JovianModule(Supplier<Observer> observer,
                        Supplier<JovianSystemService> service,
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
        return "Jupiter and moons";
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

    /** Whether Jupiter (and, with #485, its moons) is drawn. */
    public boolean showing() {
        return showing;
    }

    /**
     * Shows or hides the system; the chart redraws, and every control
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
        JovianSystemService jovian = service.get();
        JupiterObservation jupiter;
        try {
            jupiter = jovian.observeJupiter(now);
        } catch (IllegalArgumentException outsideTheYears) {
            // The table says why; the chart draws nothing it has no
            // numbers for (#472: 1900-2100 inclusive).
            return List.of();
        }
        boolean below = horizonDrawn.getAsBoolean()
                && jupiter.horizontal().altitudeDegrees() < 0.0;
        return List.of(new OverlayContribution.OblateBody(JUPITER, "Jupiter",
                jupiter.astrometricJ2000(), jupiter.equatorialDiameterArcseconds(),
                jupiter.polarDiameterArcseconds(), jovian.poleAngleJ2000Degrees(jupiter),
                MINIMUM_MARK_PX, below, jupiter.distanceKm(), InkRole.BODY));
    }
}
