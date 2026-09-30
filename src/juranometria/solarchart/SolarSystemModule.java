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

    private final Supplier<Observer> observer;
    private final Supplier<SolarSystemService> service;
    private final BooleanSupplier horizonDrawn;
    private boolean sunShowing;
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

    /** Shows or hides the Sun; the chart redraws. */
    public void sunShowing(boolean showing) {
        this.sunShowing = showing;
        if (services != null) {
            services.redraw();
        }
    }

    /** How many times the chart asked, for tests of pull-not-push. */
    public long timesAsked() {
        return contributions;
    }

    /**
     * What the page is offered now: the Sun at Place and Time's
     * observer and instant, if the Sun is shown and an observer is
     * attached; nothing otherwise. Pulled by the chart when it paints.
     */
    public List<OverlayContribution> contributedGeometry() {
        contributions++;
        if (!sunShowing) {
            return List.of();
        }
        Observer now = observer.get();
        if (now == null) {
            return List.of();
        }
        List<OverlayContribution> offered = new ArrayList<>();
        SunObservation sun = (SunObservation) service.get().observe(Body.SUN, now);
        offered.add(new OverlayContribution.Body(SUN, "Sun",
                sun.astrometricJ2000(), sun.angularDiameterArcseconds(), null,
                horizonDrawn.getAsBoolean() && sun.horizontal().altitudeDegrees() < 0.0,
                InkRole.BODY));
        return List.copyOf(offered);
    }
}
