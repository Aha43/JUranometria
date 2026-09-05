package juranometria.ecliptic;

import java.util.ArrayList;
import java.util.List;

import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.sky.Ecliptic;

/**
 * A removable way to read the sky: the one circle that explains why
 * the others move (Sprint 28, issue #273).
 *
 * <p>The module owns whether the ecliptic is shown and contributes
 * the fixed mean ecliptic of J2000 in the chart's existing
 * {@code REFERENCE_LINE} role - the circle, and the four cardinal
 * landmarks along it. It never receives a {@code Graphics2D}, a
 * pixel, a palette colour, a font metric, a renderer or the
 * catalogue, and the chart it contributes to knows nothing of
 * ecliptic longitude, obliquity, seasons, the zodiac, the Sun or any
 * planet.
 *
 * <p>Remove it and the atlas is the atlas: the ordinary chart is the
 * released page, byte for byte, and no ecliptic work is done at all.
 *
 * <h2>Nothing here has a date</h2>
 *
 * <p>Unlike the meridian, this module has no observer and no
 * instant. The gate chose the mean ecliptic of J2000 - a permanent
 * circle - over the ecliptic of date, and {@link Ecliptic} offers no
 * way to ask for anything else. So there is nothing to freeze,
 * nothing to tick, and no reason for the page to change under a
 * reader.
 *
 * <h2>Hidden until asked for</h2>
 *
 * <p>A fresh module shows nothing. The gate decided the released
 * default: a reader who has never asked for the ecliptic does not
 * get it, and installing a module must not redraw everyone's chart
 * (docs/decisions/ecliptic.md). Remembering a reader's choice across
 * sessions is issue #274's, not this module's - there is no
 * preference store here.
 */
public final class EclipticModule implements ChartModule {

    /** The name this module's ink is owned under. */
    public static final String ID = "ecliptic";

    /**
     * Hidden until a reader asks. The gate's released default, and
     * the reason installing the module changes no page.
     */
    private boolean showing;

    private ChartServices services;
    private Runnable withdraw;
    private long geometryBuilds;

    @Override
    public String name() {
        return "Ecliptic";
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
        // switching the ecliptic on needs no announcement and no
        // guess about when the page is next drawn.
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

    /** Whether the reader is being shown the ecliptic. */
    public boolean showing() {
        return showing;
    }

    /**
     * Show the ecliptic, or stop showing it.
     *
     * <p>Paint-only: the page does not move, the scene is not
     * reassembled, the catalogue is not asked anything, and no page
     * change is announced. The chart draws what it already has, with
     * this module now offering different geometry.
     */
    public void showing(boolean showing) {
        this.showing = showing;
        if (services != null) {
            services.redraw();
        }
    }

    /**
     * How many times this module has built its geometry.
     *
     * <p>Here so that "a module showing nothing does no ecliptic
     * work" is a claim a test can make rather than a sentence in a
     * comment. A hidden module answers with nothing before it builds
     * anything, and this number does not move.
     */
    public long timesTheGeometryWasBuilt() {
        return geometryBuilds;
    }

    // ---- what the chart is offered -----------------------------------

    /**
     * The geometry, in sky coordinates, for the chart to ink.
     *
     * <p>Pure: it draws nothing, moves nothing, reads no catalogue
     * and consults no clock. A hidden module offers nothing and
     * returns before any of the ecliptic is worked out.
     *
     * <p>The circle says it is {@code PERMANENT} and the landmarks
     * say they are {@code LANDMARK}s. Both are statements about what
     * the geometry <em>is</em>; the chart owns every stroke, and this
     * module could not draw one if it wanted to.
     */
    public List<OverlayContribution> contributedGeometry() {
        if (!showing) {
            return List.of();
        }
        geometryBuilds++;
        List<OverlayContribution> offered = new ArrayList<>();
        offered.add(new OverlayContribution.GreatCircle(ID, "Ecliptic",
                Ecliptic.POLE, OverlayContribution.Reference.PERMANENT,
                InkRole.REFERENCE_LINE));
        for (Ecliptic.Landmark landmark : Ecliptic.landmarks()) {
            // The landmark's own identity and its own reader-facing
            // name: the chart draws the word the model chose, and no
            // second list of names exists to drift from it.
            offered.add(new OverlayContribution.Point(
                    landmark.identity(), landmark.name(), landmark.at(),
                    OverlayContribution.Mark.LANDMARK,
                    InkRole.REFERENCE_LINE));
        }
        return List.copyOf(offered);
    }
}
