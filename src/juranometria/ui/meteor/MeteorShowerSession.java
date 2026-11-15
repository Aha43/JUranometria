package juranometria.ui.meteor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import juranometria.chart.SkyPosition;
import juranometria.context.ChartContextHolder;
import juranometria.meteor.MeteorShower;
import juranometria.meteor.MeteorShowers;
import juranometria.meteor.ShowerNight;
import juranometria.meteorchart.MeteorShowerModule;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.SolarSystemService.SunObservation;

/**
 * The one meteor-shower state every host shows (#716): which shower is
 * selected - the chart module's own selection, not a copy - and what
 * the atlas can say of it at the chart's place and time.
 *
 * <p>It owns no clock (R-17). Every fact is computed for Place and
 * Time's observer when asked, and the one action that changes time
 * installs an instant through the chart context, as every other
 * chart-time action does.
 */
public final class MeteorShowerSession {

    /**
     * What is said of the selected shower at the chart's place and time.
     *
     * @param radiant the radiant at the chart time; described only while active
     * @param moonToRadiantDegrees the Moon's distance from the radiant
     * @param maximum the nominal maximum offered at the chart time: this
     *        activity period's while the shower is active, also when it has
     *        passed, and otherwise the next
     * @param night the night around the chart time, while the shower is active
     */
    public record Facts(MeteorShower shower, Observer chart, double solarLongitude,
                        boolean active, MeteorShowers.Radiant radiant,
                        double radiantAltitudeDegrees, double sunAltitudeDegrees,
                        MoonObservation moon, double moonToRadiantDegrees,
                        Optional<Instant> maximum, Optional<ShowerNight> night) {
    }

    private final Supplier<Observer> observer;
    private final Supplier<MeteorShowers> showers;
    private final Supplier<SolarSystemService> solar;
    private final MeteorShowerModule module;
    private final List<Runnable> listeners = new ArrayList<>();
    private ChartContextHolder installer;
    private Consumer<SkyPosition> centring;

    public MeteorShowerSession(Supplier<Observer> observer, Supplier<MeteorShowers> showers,
                               Supplier<SolarSystemService> solar, MeteorShowerModule module) {
        if (observer == null || showers == null || solar == null || module == null) {
            throw new IllegalArgumentException("an observer, the services and the module");
        }
        this.observer = observer;
        this.showers = showers;
        this.solar = solar;
        this.module = module;
        module.onChange(code -> changed());
    }

    /** The one chart context a chart-time action installs through. */
    public void installThrough(ChartContextHolder context) {
        installer = context;
    }

    /** Where Centre on radiant sends a J2000 place. */
    public void centreThrough(Consumer<SkyPosition> centring) {
        this.centring = centring;
    }

    /** Told whenever the selection or the chart's place and time changes. */
    public Runnable onChange(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** Tells every host to read again; for a host told of a change the session cannot see. */
    public void changed() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }

    /** The shower service. */
    public MeteorShowers showers() {
        return showers.get();
    }

    /** The chart's observer now, or null while Place and Time holds none. */
    public Observer chart() {
        return observer.get();
    }

    /** The selected shower's code, or null. */
    public String selected() {
        return module.selected();
    }

    /** Selects a shower, or none with null. Never moves the chart or the time. */
    public void select(String code) {
        module.select(code);
    }

    /** The showers active at the chart time; empty without one or outside the Sun's years. */
    public List<MeteorShower> activeNow() {
        Observer now = observer.get();
        if (now == null) {
            return List.of();
        }
        try {
            return showers.get().activeAt(now.instant());
        } catch (IllegalArgumentException outsideTheYears) {
            return List.of();
        }
    }

    /**
     * The facts for the selected shower at the chart's place and time;
     * empty with no shower selected, no chart observer, or a chart time
     * the Sun is not computed for.
     */
    public Optional<Facts> facts() {
        Observer now = observer.get();
        String code = module.selected();
        if (now == null || code == null) {
            return Optional.empty();
        }
        try {
            MeteorShowers service = showers.get();
            MeteorShower shower = service.shower(code).orElseThrow();
            double longitude = service.solarLongitude(now.instant());
            boolean active = MeteorShowers.active(shower, longitude);
            MeteorShowers.Radiant radiant = MeteorShowers.radiantAt(shower, longitude);
            SolarSystemService sky = solar.get();
            SunObservation sun = (SunObservation) sky.observe(Body.SUN, now);
            MoonObservation moon = (MoonObservation) sky.observe(Body.MOON, now);
            Optional<ShowerNight> night = Optional.empty();
            if (active) {
                night = night(service, shower, sky, now);
            }
            return Optional.of(new Facts(shower, now, longitude, active, radiant,
                    new LocalSky(now).altitudeDegrees(radiant.at()),
                    sun.horizontal().altitudeDegrees(), moon,
                    moon.astrometricJ2000().separationDegrees(radiant.at()),
                    service.nominalMaximumFor(shower, now.instant()), night));
        } catch (IllegalArgumentException outsideTheYears) {
            return Optional.empty();
        }
    }

    private static Optional<ShowerNight> night(MeteorShowers service, MeteorShower shower,
                                               SolarSystemService sky, Observer now) {
        try {
            return Optional.of(ShowerNight.around(now.instant(), now.eastLongitudeDegrees(),
                    at -> ((SunObservation) sky.observe(Body.SUN, now.at(at)))
                            .horizontal().altitudeDegrees(),
                    at -> {
                        double longitude = service.solarLongitude(at);
                        return MeteorShowers.active(shower, longitude)
                                ? Optional.of(new LocalSky(now.at(at)).altitudeDegrees(
                                        MeteorShowers.radiantAt(shower, longitude).at()))
                                : Optional.empty();
                    }));
        } catch (IllegalArgumentException nightLeavesTheYears) {
            return Optional.empty();
        }
    }

    /** Whether the chart time can be set from here now. */
    public boolean canSetChartTime() {
        return installer != null && observer.get() != null;
    }

    /**
     * Sets the chart time to the selected shower's nominal maximum - this
     * activity period's while the shower is active at the chart time, also
     * when it has passed, and otherwise the next - through the one chart
     * context; the place is kept. Returns the instant installed, or empty when there
     * was nothing to install.
     */
    public Optional<Instant> setChartTimeToMaximum() {
        Optional<Instant> next = facts().flatMap(Facts::maximum);
        if (next.isPresent() && canSetChartTime()) {
            installer.install(installer.observer().at(next.get()));
        }
        return canSetChartTime() ? next : Optional.empty();
    }

    /** Whether the radiant can be centred now: a shower active at the chart time. */
    public boolean canCentre() {
        return centring != null && facts().map(Facts::active).orElse(false);
    }

    /** Centres the chart on the radiant at the chart time; nothing while inactive. */
    public void centreOnRadiant() {
        if (canCentre()) {
            centring.accept(facts().orElseThrow().radiant().at());
        }
    }
}
