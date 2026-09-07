package juranometria.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import juranometria.chart.ChartScene;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.geo.GeoSegment;

/**
 * The stars a constellation figure is built from (Sprint 30, issue
 * #307).
 *
 * <p>A magnitude limit answers how crowded a page should be. A
 * constellation figure answers which stars define its shape. The
 * first must not cut holes in the second - and it did: the overview's
 * brighter default limits removed stars that figure segments are
 * drawn to, leaving the lines and losing the node. On a 120-degree
 * page at V 4.0, <strong>76 of 190</strong> figure endpoints on the
 * paper had no star; at 90 degrees, 43 of 121. Found by looking at
 * the finished page, which is the only thing that could have found
 * it: the density the gate measured was right, and nothing about it
 * says which stars a figure needs.
 *
 * <p>So a star that a visible figure is drawn to is kept, however
 * faint, and nothing else is. It keeps its own size, because its size
 * is a statement about its brightness and it is no brighter for being
 * structural; and it keeps its silence, because a label is a
 * different promise from a node.
 *
 * <p>This is display policy, not projection and not module ink. The
 * figures are the atlas's own geography and the exception belongs
 * beside the rule it excepts.
 */
public final class FigureAnchors {

    /**
     * How near a figure's endpoint a star must be to <em>be</em> that
     * endpoint, in degrees.
     *
     * <p>Measured rather than chosen, because the figures are stored
     * as coordinates and carry no catalogue identity. Over every
     * figure endpoint the bundled pack can show - 335 of them - the
     * nearest star is either the endpoint's own, within a thousandth
     * of a degree, or nothing near at all:
     *
     * <pre>
     * half of them within  0.00005 degrees
     * nine tenths within   0.0007 degrees
     * the next nearest at  0.89 degrees
     * </pre>
     *
     * <p>Three orders of magnitude of empty space separate the two
     * populations, and this sits in the middle of it. The far ones
     * are endpoints whose star is outside the queried sky; they match
     * nothing and are meant to.
     */
    public static final double SAME_STAR_DEGREES = 0.01;

    /** Declination bands, so an endpoint asks about its own sky. */
    private static final double BAND_DEGREES = 2.0 * SAME_STAR_DEGREES;

    private static final FigureAnchors NONE =
            new FigureAnchors(java.util.Set.of());

    private final java.util.Set<String> anchors;

    private FigureAnchors(java.util.Set<String> anchors) {
        this.anchors = anchors;
    }

    /**
     * The anchors of this page, or none when the reader is not
     * looking at constellation figures.
     *
     * <p>The exception disappears with the thing it excepts: with
     * figures switched off there is no shape to complete, and a star
     * kept for one would be an ordinary star below the limit, which
     * is exactly what the density policy is for.
     */
    public static FigureAnchors of(ChartScene scene, ChartOptions options,
                                   GeographyDetailPolicy policy) {
        if (scene == null || options == null || policy == null) {
            throw new IllegalArgumentException(
                    "a page, its options and its detail policy are"
                            + " required");
        }
        if (!options.constellationFigures() || !policy.figuresDrawn()) {
            return NONE;
        }
        Map<Long, List<Star>> byBand = new HashMap<>();
        for (Star star : scene.stars()) {
            byBand.computeIfAbsent(bandOf(star.position().decDegrees()),
                    key -> new ArrayList<>()).add(star);
        }
        java.util.Set<String> anchors = new java.util.HashSet<>();
        for (GeoSegment segment : scene.geography().figureSegments()) {
            for (SkyPosition endpoint
                    : List.of(segment.from(), segment.to())) {
                Star star = nearest(byBand, endpoint);
                if (star != null) {
                    anchors.add(star.id());
                }
            }
        }
        return anchors.isEmpty() ? NONE
                : new FigureAnchors(java.util.Set.copyOf(anchors));
    }

    /**
     * The one star an endpoint is: the nearest inside the tolerance,
     * and only it.
     *
     * <p>Thirty of the pack's 314 matchable endpoints have a second
     * star within a hundredth of a degree - a close companion, or a
     * catalogue entry beside the one the figure means. Keeping
     * everything inside the tolerance kept those too, and admitted a
     * magnitude 7.8 star to a page limited at V 4.0, which is exactly
     * the "unrelated star below the limit" this must not do. An
     * endpoint is one star.
     *
     * <p>And where two are equally near - the components of a double,
     * which this pack records at the same position - it is the
     * <strong>brighter</strong>. Andromeda's figure ends on such a
     * pair, V 4.3 and V 7.8 at a separation of zero, and taking
     * whichever the scan reached last kept the faint one: a node
     * three magnitudes dimmer than the star the figure is named for,
     * and a fainter star admitted for nothing. A figure means the
     * star a reader sees.
     */
    private static Star nearest(Map<Long, List<Star>> byBand,
                                SkyPosition endpoint) {
        Star best = null;
        double closest = SAME_STAR_DEGREES;
        long low = bandOf(endpoint.decDegrees() - SAME_STAR_DEGREES);
        long high = bandOf(endpoint.decDegrees() + SAME_STAR_DEGREES);
        for (long band = low; band <= high; band++) {
            for (Star star : byBand.getOrDefault(band, List.of())) {
                double apart =
                        star.position().separationDegrees(endpoint);
                if (apart > closest) {
                    continue;
                }
                if (apart < closest || best == null
                        || star.magnitude() < best.magnitude()) {
                    closest = apart;
                    best = star;
                }
            }
        }
        return best;
    }

    /** Whether a visible figure is drawn to this star. */
    public boolean holds(Star star) {
        return star != null && anchors.contains(star.id());
    }

    /** Whether this page keeps any star its limit would have hidden. */
    public boolean any() {
        return !anchors.isEmpty();
    }

    private static long bandOf(double declinationDegrees) {
        return (long) Math.floor(declinationDegrees / BAND_DEGREES);
    }
}
