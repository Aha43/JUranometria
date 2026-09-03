package juranometria.meridian;

import java.util.ArrayList;
import java.util.List;

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
 * <h2>Nothing ticks</h2>
 *
 * <p>The observer carries a frozen instant. There is no clock here,
 * no timer and no animation: the chart is drawn for one moment and
 * stays there until a reader says otherwise.
 */
public final class MeridianModule implements ChartModule {

    /** The name this module's ink is owned under. */
    public static final String ID = "meridian";

    private Observer observer;
    private boolean meridianShowing = true;
    private boolean horizonShowing = true;
    private boolean zenithShowing = true;

    private ChartServices services;
    private Runnable withdraw;
    private long skyComputations;

    public MeridianModule(Observer observer) {
        observer(observer);
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
    }

    @Override
    public void detach() {
        if (withdraw != null) {
            withdraw.run();
        }
        withdraw = null;
        services = null;
    }

    // ---- what the module owns ---------------------------------------

    public Observer observer() {
        return observer;
    }

    /**
     * Somewhere else, or some other moment. The page does not move.
     */
    public void observer(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException(
                    "the module is always somewhere, at some moment");
        }
        this.observer = observer;
        redraw();
    }

    /** Which of the three a reader is being shown. */
    public void showing(boolean meridian, boolean horizon, boolean zenith) {
        this.meridianShowing = meridian;
        this.horizonShowing = horizon;
        this.zenithShowing = zenith;
        redraw();
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
                new LocalSky(observer).zenith(), null,
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
        LocalSky sky = new LocalSky(observer);
        List<OverlayContribution> offered = new ArrayList<>();
        if (meridianShowing) {
            offered.add(new OverlayContribution.GreatCircle(
                    "meridian", "Meridian", sky.meridian().pole(),
                    OverlayContribution.Reference.LINE,
                    InkRole.REFERENCE_LINE));
        }
        if (horizonShowing) {
            // A boundary of what can be seen, and named
            // "mathematical" nowhere but in the documentation: the
            // word a reader is shown is the word on the page, and
            // the atlas does not claim to know about hills or air.
            offered.add(new OverlayContribution.GreatCircle(
                    "horizon", "Horizon", sky.horizon().pole(),
                    OverlayContribution.Reference.BOUNDARY,
                    InkRole.REFERENCE_LINE));
        }
        if (zenithShowing) {
            offered.add(new OverlayContribution.Point(
                    "zenith", "Zenith", sky.zenith(),
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
