package juranometria.meteorchart;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import juranometria.meteor.MeteorShower;
import juranometria.meteor.MeteorShowers;
import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.OverlayContribution;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

/**
 * The selected meteor shower's radiant on the chart (#716, R-12 of
 * {@code docs/decisions/meteor-showers.md}).
 *
 * <p>One shower is selected, or none, and the selection survives any
 * chart time. The radiant is offered when, and only when, the chart
 * time lies inside the shower's activity, at its position for that
 * chart time, with that moment - never a radiant without the time that
 * defines it. Geometry is pulled at paint time from Place and Time's
 * observer, so the module holds no clock and no copy of the instant.
 */
public final class MeteorShowerModule implements ChartModule {

    /** The module's name in the overlay registry. */
    public static final String ID = "meteor-showers";

    private final Supplier<Observer> observer;
    private final Supplier<MeteorShowers> service;
    private final BooleanSupplier horizonDrawn;
    private final List<Consumer<String>> listeners = new ArrayList<>();
    private ChartServices services;
    private Runnable withdraw = () -> { };
    private String selected;

    /**
     * @param observer Place and Time's observer now, or null while there is none
     * @param service the shower service, read when a radiant is first asked for
     * @param horizonDrawn whether the observer's horizon is drawn and subdues
     */
    public MeteorShowerModule(Supplier<Observer> observer, Supplier<MeteorShowers> service,
                              BooleanSupplier horizonDrawn) {
        if (observer == null || service == null || horizonDrawn == null) {
            throw new IllegalArgumentException("an observer, a service and the horizon");
        }
        this.observer = observer;
        this.service = service;
        this.horizonDrawn = horizonDrawn;
    }

    @Override
    public String name() {
        return ID;
    }

    @Override
    public void attach(ChartServices services) {
        this.services = services;
        withdraw = services.contribute(ID, this::contributedGeometry);
    }

    @Override
    public void detach() {
        withdraw.run();
        withdraw = () -> { };
        services = null;
    }

    /** The selected shower's IAU code, or null for none. */
    public String selected() {
        return selected;
    }

    /** Selects a shower by its IAU code, or none with null; redraws and tells listeners. */
    public void select(String code) {
        if (code != null && service.get().shower(code).isEmpty()) {
            throw new IllegalArgumentException("not a shower the atlas carries: " + code);
        }
        if (java.util.Objects.equals(code, selected)) {
            return;
        }
        selected = code;
        if (services != null) {
            services.redraw();
        }
        for (Consumer<String> listener : List.copyOf(listeners)) {
            listener.accept(code);
        }
    }

    /** Told the new code, or null, whenever the selected shower changes. */
    public Runnable onChange(Consumer<String> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** The radiant for the chart's place and time, or nothing. */
    public List<OverlayContribution> contributedGeometry() {
        return radiant().<List<OverlayContribution>>map(List::of).orElse(List.of());
    }

    /** The selected shower's radiant at the chart time, while the shower is active then. */
    public Optional<OverlayContribution.Radiant> radiant() {
        Observer now = observer.get();
        if (selected == null || now == null) {
            return Optional.empty();
        }
        try {
            MeteorShowers showers = service.get();
            MeteorShower shower = showers.shower(selected).orElseThrow();
            double longitude = showers.solarLongitude(now.instant());
            if (!MeteorShowers.active(shower, longitude)) {
                return Optional.empty();
            }
            MeteorShowers.Radiant at = MeteorShowers.radiantAt(shower, longitude);
            boolean below = horizonDrawn.getAsBoolean()
                    && new LocalSky(now).altitudeDegrees(at.at()) < 0.0;
            return Optional.of(new OverlayContribution.Radiant(shower.identity(),
                    shower.iauName(), at.at(), below, OverlayContribution.Moment.of(now),
                    true));
        } catch (IllegalArgumentException outsideTheYears) {
            // The Sun is not computed for this instant: nothing to draw.
            return Optional.empty();
        }
    }
}
