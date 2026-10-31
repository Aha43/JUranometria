package juranometria.meridian;

import java.util.ArrayList;
import java.util.List;

import juranometria.context.ChartContextHolder;
import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.InkRole;
import juranometria.module.NavigationRequest;
import juranometria.module.OverlayContribution;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

/**
 * A removable way to read the sky: where the reader is, and when
 * (Sprint 25, issue #227).
 *
 * <p>The module owns the observer and contributes three geometries
 * in the chart's existing {@code REFERENCE_LINE} role - the
 * meridian, the mathematical horizon and the zenith. It never
 * receives a {@code Graphics2D}, a pixel, a renderer or the
 * catalogue, and the chart it contributes to knows nothing of
 * observers, clocks, longitude or sidereal time.
 *
 * <p>Remove it and the atlas is the atlas: the ordinary chart is the
 * released page, byte for byte, and no observer or time work is done
 * at all.
 *
 * <h2>The chart stays where the reader put it</h2>
 *
 * <p>Changing the place or the instant <strong>redraws the lines and
 * leaves the page where it is</strong>. The one thing that moves the
 * chart is {@link #centreOnZenith()}, which is a reader asking, once
 * - never a side effect of setting a latitude or a clock. A page
 * that moved while a reader was reading it could not be read.
 *
 * <h2>Every change is announced, here</h2>
 *
 * <p>The module is the one authority for the place, the instant and
 * the three lines, so it is the one place that says when they change
 * (#433, ruled; #434). Every presentation that shows them - the Place
 * and Time dialog today, the companion next - subscribes through
 * {@link #onChange} and is told after every mutation, whoever made
 * it. A presentation that only read the module when it was built went
 * stale: with the dialog open, the chart keyboard turned the meridian
 * on, the dialog's box still showed it off, and the next click in the
 * dialog turned it back off. A notification raised by the authority
 * cannot be bypassed by a writer that forgets to raise it.
 *
 * <h2>Nothing ticks</h2>
 *
 * <p>The observer carries a frozen instant. There is no clock here,
 * no timer and no animation: the chart is drawn for one moment and
 * stays there until a reader says otherwise.
 */
public final class MeridianModule implements ChartModule {

    /** The name this module's ink is owned under. */
    public static final String ID = "meridian";

    /** The chart's one time and place (#612): read here, owned by the holder. */
    private final ChartContextHolder context;
    private boolean meridianShowing = true;
    private boolean horizonShowing = true;
    private boolean zenithShowing = true;
    private boolean ticksShowing = false;
    /** Subdue below horizon (#615): on by default (ruling P-D), independent of the line (P-I). */
    private boolean subduing = true;
    private Ticks ticks = Ticks.PROPOSED;

    private final List<Runnable> listeners = new ArrayList<>();

    private ChartServices services;
    private Runnable withdraw;
    private long skyComputations;

    /** The furniture this module offers the chart, once its presentation says what. */
    private java.util.function.Supplier<List<juranometria.module.Furniture>> furniture;
    private java.util.function.Consumer<String> furnitureActions;
    private Runnable withdrawFurniture;

    /** Over its own holder: a module begun with an observer owns the context. */
    public MeridianModule(Observer observer) {
        this(new ChartContextHolder(observer));
    }

    /**
     * Over the application's holder (#612): the module reads the chart's
     * time and place there, redraws and announces on every installation,
     * and no longer owns an observer of its own.
     */
    public MeridianModule(ChartContextHolder context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "the module is always somewhere, at some moment");
        }
        this.context = context;
        context.onChange(change -> {
            redraw();
            changed();
        });
    }

    /** The holder of the chart's one time and place. */
    public ChartContextHolder context() {
        return context;
    }

    @Override
    public String name() {
        return "Meridian and horizon";
    }

    @Override
    public void attach(ChartServices services) {
        if (services == null) {
            throw new IllegalArgumentException(
                    "a module is attached to a chart's services");
        }
        if (this.services != null) {
            throw new IllegalStateException(
                    "this module is already attached; attaching twice"
                            + " leaves the first contribution with"
                            + " nothing to withdraw it");
        }
        this.services = services;
        // Pulled, not pushed: the chart asks when it paints, so
        // changing the place or the instant needs no announcement
        // and no guess about when the page is next drawn.
        this.withdraw = services.contribute(ID, this::contributedGeometry);
        furnishIfOffered();
    }

    /**
     * The box this module asks the chart to carry, and the handler its
     * action is handed back to (Sprint 51, issue #259, the rulings on
     * #556).
     *
     * <p>The words are the presentation's - the module carries no
     * language - so the Place and Time presentation states them here;
     * the lifecycle stays the module's. The box is offered while the
     * module is attached and withdrawn with it, so detaching the module
     * removes its box and its action and leaves the page as it was.
     * Offered once: a second offer would be a presentation that has
     * lost track of the first.
     */
    public void offerFurniture(
            java.util.function.Supplier<List<juranometria.module.Furniture>> box,
            java.util.function.Consumer<String> actions) {
        if (box == null || actions == null) {
            throw new IllegalArgumentException(
                    "furniture is offered with the handler for its action");
        }
        if (this.furniture != null) {
            throw new IllegalStateException(
                    "this module already offers its furniture");
        }
        this.furniture = box;
        this.furnitureActions = actions;
        furnishIfOffered();
    }

    private void furnishIfOffered() {
        if (services != null && furniture != null && withdrawFurniture == null) {
            withdrawFurniture = services.furnish(ID, furniture, furnitureActions);
        }
    }

    /**
     * Whether this module is attached to a chart right now - read
     * from its own services, the one place that knows (#372). The
     * host's list is not asked: a module detached directly stays in
     * it.
     */
    public boolean attached() {
        return services != null;
    }

    @Override
    public void detach() {
        if (withdrawFurniture != null) {
            withdrawFurniture.run();
        }
        withdrawFurniture = null;
        if (withdraw != null) {
            withdraw.run();
        }
        withdraw = null;
        services = null;
    }

    // ---- what the module owns ---------------------------------------

    public Observer observer() {
        return context.observer();
    }

    /**
     * Somewhere else, or some other moment. The page does not move.
     * Installed through the holder, on the event thread (#612): the
     * redraw and the announcement follow from the installation.
     */
    public void observer(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException(
                    "the module is always somewhere, at some moment");
        }
        context.install(observer);
    }

    /** Which of the three a reader is being shown. */
    public void showing(boolean meridian, boolean horizon, boolean zenith) {
        this.meridianShowing = meridian;
        this.horizonShowing = horizon;
        this.zenithShowing = zenith;
        redraw();
        changed();
    }

    /** A registration that ends when cancelled; cancelling twice is once. */
    public interface Subscription {
        void cancel();
    }

    /**
     * Told after every change to the observer or the lines, by
     * whichever presentation or key made it. The listener runs on the
     * thread that made the change - the event thread, as for every
     * Swing caller - and must not itself change the module.
     */
    public Subscription onChange(Runnable listener) {
        if (listener == null) {
            throw new IllegalArgumentException(
                    "a subscription is somebody listening");
        }
        listeners.add(listener);
        return new Subscription() {
            private boolean live = true;

            @Override
            public void cancel() {
                if (live) {
                    live = false;
                    listeners.remove(listener);
                }
            }
        };
    }

    /**
     * How many subscriptions are live - so that "reopening adds no
     * listener" is a claim a test can make (#433, invariant 4).
     */
    public int subscribers() {
        return listeners.size();
    }

    private void changed() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }

    public boolean meridianShowing() {
        return meridianShowing;
    }

    public boolean horizonShowing() {
        return horizonShowing;
    }

    public boolean zenithShowing() {
        return zenithShowing;
    }

    /**
     * The meridian's graduation: a tick every {@code stepDegrees} of
     * altitude above the mathematical horizon, on both halves, with
     * the figure written beside every {@code labelEveryDegrees}
     * (issue #515). Ninety is the zenith and is never a tick; zero is
     * the horizon and is never one either.
     *
     * <p>{@link #PROPOSED} is the study's proposal, held here as the
     * one declared value the chart, the study and the record all
     * read; the owner's ruling on the steps and the density replaces
     * it in one place.
     */
    public record Ticks(double stepDegrees, double labelEveryDegrees) {

        /** Every ten degrees, each one lettered: the proposal. */
        public static final Ticks PROPOSED = new Ticks(10.0, 10.0);

        public Ticks {
            if (!(stepDegrees > 0.0 && stepDegrees < 90.0)
                    || Math.abs(90.0 / stepDegrees
                            - Math.rint(90.0 / stepDegrees)) > 1e-9) {
                throw new IllegalArgumentException(
                        "the step divides the quarter turn: "
                                + stepDegrees);
            }
            if (!(labelEveryDegrees >= stepDegrees)
                    || Math.abs(labelEveryDegrees / stepDegrees
                            - Math.rint(labelEveryDegrees / stepDegrees))
                            > 1e-9) {
                throw new IllegalArgumentException(
                        "a figure stands at a tick: " + labelEveryDegrees
                                + " is no multiple of " + stepDegrees);
            }
        }

        /** The altitudes ticked, ascending, the zenith excluded. */
        public List<Double> altitudes() {
            List<Double> all = new ArrayList<>();
            for (int k = 1; k * stepDegrees < 90.0 - 1e-9; k++) {
                all.add(k * stepDegrees);
            }
            return List.copyOf(all);
        }

        /** Whether the figure is written at this altitude. */
        public boolean labelled(double altitudeDegrees) {
            double n = altitudeDegrees / labelEveryDegrees;
            return Math.abs(n - Math.rint(n)) < 1e-9;
        }
    }

    /** Whether the meridian carries its altitude ticks. */
    public boolean ticksShowing() {
        return ticksShowing;
    }

    /**
     * Graduate the meridian, or stop. Drawn with the meridian: a
     * reader who hides the line hides its ticks with it, and showing
     * ticks on a hidden line draws nothing until the line is shown.
     */
    public void ticksShowing(boolean showing) {
        this.ticksShowing = showing;
        redraw();
        changed();
    }

    public Ticks ticks() {
        return ticks;
    }

    /** Whether celestial ink below the mathematical horizon is subdued (#615). */
    public boolean subduing() {
        return subduing;
    }

    /**
     * Subdue below horizon, or stop (#615). Independent of the horizon
     * line (ruling P-I): either may be on without the other. Redraws
     * and tells the hosts, as every switch here does.
     */
    public void subduing(boolean on) {
        this.subduing = on;
        redraw();
        changed();
    }

    /** The graduation to draw - the study's way of comparing densities. */
    public void ticks(Ticks ticks) {
        if (ticks == null) {
            throw new IllegalArgumentException("a graduation, or the proposal");
        }
        this.ticks = ticks;
        redraw();
        changed();
    }

    /**
     * How many times this module has worked out the observer's sky.
     *
     * <p>Here so that "a module showing nothing does no observer or
     * time work" is a claim a test can make rather than a sentence in
     * a comment. A module with all three lines hidden answers with
     * nothing before it computes anything, and this number does not
     * move.
     */
    public long timesTheSkyWasComputed() {
        return skyComputations;
    }

    /**
     * Centre the chart on the point overhead, because a reader asked.
     *
     * <p>The only movement this module ever causes, and it is a
     * request rather than a move: whether the chart can go there at
     * this field is the chart's decision, not the module's.
     */
    public void centreOnZenith() {
        if (services == null) {
            throw new IllegalStateException(
                    "an unattached module has no chart to ask");
        }
        skyComputations++;
        services.request(new NavigationRequest(
                new LocalSky(observer()).zenith(), null,
                "Centre on zenith"));
    }

    // ---- what the chart is offered -----------------------------------

    /**
     * The geometry, in sky coordinates, for the chart to ink.
     *
     * <p>Pure: it draws nothing, moves nothing and reads no
     * catalogue. A module showing nothing offers nothing, and returns
     * before any of the observer's sky is worked out.
     */
    public List<OverlayContribution> contributedGeometry() {
        if (!meridianShowing && !horizonShowing && !zenithShowing) {
            return List.of();
        }
        skyComputations++;
        Observer observer = observer();
        LocalSky sky = new LocalSky(observer);
        List<OverlayContribution> offered = new ArrayList<>();
        if (meridianShowing) {
            offered.add(new OverlayContribution.GreatCircle(
                    "meridian", "Meridian", sky.meridian().pole(),
                    OverlayContribution.Reference.LINE,
                    InkRole.REFERENCE_LINE));
            // The graduation rides the meridian (#515): the altitude
            // of each tick is the module's, the point is the sky's
            // own, and the words are the page's. The geographic
            // poles, where the meridian has no north and no south
            // half, carry no ticks - refused by the sky and caught
            // here, as a module that threw while the chart painted
            // would stop the page.
            if (ticksShowing
                    && Math.abs(observer.latitudeDegrees()) < 90.0 - 1e-9) {
                // One contextual readout (#563, ruled on #675 stage 5):
                // offered with the ticks; the page draws it only while no
                // tick of the line is on the page, at the visible point
                // nearest its centre, rounded to the degree. Refused with
                // the ticks at the geographic poles.
                offered.add(new OverlayContribution.AltitudeReadout(
                        ID + "/readout", ID, sky.meridian().pole(), sky.zenith(),
                        InkRole.REFERENCE_LINE));
                for (double altitude : ticks.altitudes()) {
                    for (boolean north : new boolean[] {true, false}) {
                        // Its own identity, unique in the module: the
                        // line, the half and the altitude.
                        String half = (north ? juranometria.chart.Cardinal.NORTH
                                : juranometria.chart.Cardinal.SOUTH).name()
                                .toLowerCase(java.util.Locale.ROOT);
                        offered.add(new OverlayContribution.Tick(
                                ID + "/" + half + "/" + altitude, ID,
                                sky.onMeridian(altitude, north),
                                sky.meridian().pole(), altitude,
                                ticks.labelled(altitude),
                                InkRole.REFERENCE_LINE));
                    }
                }
            }
        }
        if (horizonShowing) {
            // "Mathematical horizon", because the gate says the name
            // belongs in the interface and not only in the
            // documentation (review). An earlier version shortened it
            // to "Horizon" on the page, which is precisely the quiet
            // claim the longer name exists to refuse: a reader's real
            // horizon has hills and air in it, and this line is where
            // the sky meets a perfectly flat, perfectly transparent
            // Earth.
            offered.add(new OverlayContribution.GreatCircle(
                    "horizon", "Mathematical horizon",
                    sky.horizon().pole(),
                    OverlayContribution.Reference.BOUNDARY,
                    InkRole.REFERENCE_LINE));
            // The observer's N, E, S and W ride the horizon option
            // (issue #359): the horizon is what they are directions
            // ON, so they exist exactly while it does - off with the
            // switch, gone with the detach, like everything above.
            // No words here. The marks carry identity and position;
            // the page's own language letters them.
            for (juranometria.chart.Cardinal direction
                    : juranometria.chart.Cardinal.values()) {
                offered.add(new OverlayContribution.DirectionMark(
                        direction, sky.cardinal(direction)));
            }
        }
        if (zenithShowing) {
            offered.add(new OverlayContribution.Point(
                    "zenith", "Zenith", sky.zenith(),
                    // A place a reader stands under: it has an up,
                    // and the chart draws the tick for it.
                    OverlayContribution.Mark.PLACE,
                    InkRole.REFERENCE_LINE));
        }
        return List.copyOf(offered);
    }

    /**
     * Asks the chart to draw again, without asking it to move or to
     * reassemble anything.
     */
    private void redraw() {
        if (services != null) {
            services.redraw();
        }
    }
}
