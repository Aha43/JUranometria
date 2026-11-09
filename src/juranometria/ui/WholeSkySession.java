package juranometria.ui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import juranometria.chart.SkyPosition;
import juranometria.project.HammerProjection;
import juranometria.sky.Ecliptic;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

/**
 * The one switch for the whole sky (#687, ruled R-13 on #686): the
 * toolbar's toggle, the Controller's, the Chart menu's item and a
 * whole-sky bookmark all run this, and every one of them shows what the
 * controller holds.
 *
 * <p>What it knows that the controller does not is the observer. The
 * whole sky's central meridian is the ecliptic longitude of the
 * observer's zenith (R-4, observer-aligned: the zenith lies on the
 * central meridian, not at the map's centre), so entering asks the
 * chart's one observer where the zenith is, and a change of chart time
 * or place while the whole sky is shown moves the central meridian with
 * it. Nothing here is persisted: the whole sky is never restored at
 * startup (R-10).
 */
public final class WholeSkySession {

    private final ChartViewController controller;
    private final Supplier<Observer> observer;

    /**
     * @param observer the chart's observer now, or null while there is
     *     none - then the whole sky cannot be entered
     */
    public WholeSkySession(ChartViewController controller,
                           Supplier<Observer> observer) {
        if (controller == null || observer == null) {
            throw new IllegalArgumentException(
                    "the whole sky needs the controller and the observer");
        }
        this.controller = controller;
        this.observer = observer;
    }

    /**
     * The centre a whole-sky page is built about for this observer: the
     * point of the ecliptic at the zenith's ecliptic longitude.
     */
    public static SkyPosition centreFor(Observer observer) {
        SkyPosition zenith = new LocalSky(observer).zenith();
        return HammerProjection.centreFor(
                Ecliptic.toEcliptic(zenith).longitudeDegrees());
    }

    /** Whether the chart shows the whole sky. */
    public boolean on() {
        return controller.wholeSky();
    }

    /** Shows the whole sky at the chart's time and place; nothing without an observer. */
    public void enter() {
        Observer now = observer.get();
        if (now == null) {
            return;
        }
        controller.enterWholeSky(centreFor(now));
    }

    /** Returns to the ordinary chart exactly as it was left (R-8). */
    public void leave() {
        controller.leaveWholeSky();
    }

    public void set(boolean on) {
        if (on) {
            enter();
        } else {
            leave();
        }
    }

    public void toggle() {
        set(!on());
    }

    /**
     * Chart time or place changed: the central meridian follows the
     * zenith (R-4). Nothing on an ordinary page.
     */
    public void observerChanged() {
        if (!on()) {
            return;
        }
        Observer now = observer.get();
        if (now != null) {
            controller.followObserver(centreFor(now));
        }
    }

    /** Told, with the current answer at once, whenever the whole sky is shown or left. */
    public void onChange(Consumer<Boolean> listener) {
        controller.onChange(state -> listener.accept(state.wholeSky()));
    }
}
