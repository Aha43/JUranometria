package juranometria.page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.RegionalDetailPolicy;

/**
 * What is on this page (Sprint 24, issue #215).
 *
 * <p>A service the chart offers: it knows nothing of tables,
 * sidebars or crosses, and a module that displays what it reports
 * can be removed without weakening it.
 *
 * <h2>No catalogue query</h2>
 *
 * <p>The scene the chart already assembled holds every star and
 * deep-sky object in the queried region <em>unfiltered</em> - the
 * renderer applies the magnitude limit as it draws. An inventory is
 * therefore a projection sweep over a list the page is already
 * holding: arithmetic, no I/O. Painting never builds one.
 *
 * <h2>Independent of the filters, honest about them</h2>
 *
 * <p>Everything on the paper is reported, whether or not the page
 * draws it - and each entry carries production's own answer for why.
 * A reader who cannot find M110 is owed "hidden by a chart option",
 * not silence. The answers come from {@code symbolForType},
 * {@code permitted}, {@link RegionalDetailPolicy} and the scene's
 * limiting magnitude; none of those rules is reimplemented here.
 */
public final class PageInventory {

    private PageInventory() {
    }

    /**
     * The contents of this page under these options.
     *
     * <p>Rebuilt when the page changes - centre, field, size,
     * magnitude limit, catalogue content, or a chart option that
     * changes visibility - and at no other time.
     */
    public static PageContents of(ChartScene scene, ChartOptions options) {
        if (scene == null || options == null) {
            throw new IllegalArgumentException(
                    "an inventory is of a scene under options");
        }
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        RegionalDetailPolicy policy =
                new RegionalDetailPolicy(scene, mapping.pixelsPerPlaneUnit());
        SkyPosition centre = scene.viewport().centre();

        List<DeepSkyObject> deepSky = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (PageExtent.onPage(scene, dso)) {
                deepSky.add(dso);
            }
        }
        deepSky.sort(defaultOrder(centre));

        List<Star> stars = new ArrayList<>();
        for (Star star : scene.stars()) {
            if (onPaper(projection, mapping, scene, star.position())) {
                stars.add(star);
            }
        }
        stars.sort(starOrder(centre));

        // Deep-sky first, then stars. A reader asking what is here is
        // hunting objects; the stars are the landmarks they steer by.
        // Within each kind the order is total - down to catalogue
        // identity - so the same page always reports identically
        // however the catalogue arrives.
        List<PageEntry> entries = new ArrayList<>();
        for (DeepSkyObject dso : deepSky) {
            entries.add(new PageEntry.DeepSky(dso,
                    visibilityOf(scene, dso, options, policy),
                    centre.separationDegrees(dso.position())));
        }
        for (Star star : stars) {
            entries.add(new PageEntry.StarEntry(star, visibilityOf(scene, star),
                    centre.separationDegrees(star.position())));
        }
        return new PageContents(entries);
    }

    /**
     * The decided order within the deep-sky objects: a Messier
     * number first, then recorded brightness, then distance from the
     * centre, then catalogue identity - which is what makes it
     * total.
     */
    public static Comparator<DeepSkyObject> defaultOrder(SkyPosition centre) {
        return Comparator
                .comparingInt((DeepSkyObject dso) ->
                        messierOf(dso) == null ? 1 : 0)
                .thenComparingInt(dso -> messierOf(dso) == null
                        ? Integer.MAX_VALUE : messierOf(dso))
                .thenComparing(byRecordedBrightness())
                .thenComparingDouble(dso ->
                        centre.separationDegrees(dso.position()))
                .thenComparing(DeepSkyObject::id);
    }

    /**
     * The decided order within the stars: named ones first, then
     * brightness, distance and identity. The named ones are what a
     * reader steers by; the rest are counted rather than read.
     */
    public static Comparator<Star> starOrder(SkyPosition centre) {
        return Comparator
                .comparingInt((Star star) -> named(star) ? 0 : 1)
                .thenComparingDouble(Star::magnitude)
                .thenComparingDouble(star ->
                        centre.separationDegrees(star.position()))
                .thenComparing(Star::id);
    }

    /**
     * Brightest first, with objects whose magnitude the source never
     * recorded last - they are not bright, they are unrecorded, and
     * sorting them as though they were zero would say otherwise.
     */
    private static Comparator<DeepSkyObject> byRecordedBrightness() {
        return Comparator
                .comparingInt((DeepSkyObject dso) ->
                        Double.isNaN(dso.magnitude()) ? 1 : 0)
                .thenComparingDouble(dso -> Double.isNaN(dso.magnitude())
                        ? 0.0 : dso.magnitude());
    }

    /** The Messier number an object is also known by, or null. */
    public static Integer messierOf(DeepSkyObject dso) {
        for (String alias : dso.aliases()) {
            String trimmed = alias.trim();
            if (trimmed.startsWith("M")) {
                String digits = trimmed.replaceAll("[^0-9]", "");
                if (!digits.isEmpty() && trimmed.replaceAll("[0-9 ]", "")
                        .equalsIgnoreCase("M")) {
                    return Integer.valueOf(digits);
                }
            }
        }
        return null;
    }

    private static boolean named(Star star) {
        juranometria.chart.StarIdentity identity = star.identity();
        return identity != null && (identity.name() != null
                || identity.bayer() != null
                || identity.flamsteed() != null);
    }

    /**
     * Why this object can or cannot be seen, in the order the
     * renderer itself asks: a type with no symbol is never drawn
     * whatever the options say, a hidden family is never drawn
     * however large it is, and the detail policy decides last.
     */
    private static PageVisibility visibilityOf(ChartScene scene,
                                               DeepSkyObject dso,
                                               ChartOptions options,
                                               RegionalDetailPolicy policy) {
        if (ChartRenderer.symbolForType(dso.type())
                == ChartRenderer.Symbol.NONE) {
            return PageVisibility.NO_SYMBOL;
        }
        if (!ChartRenderer.permitted(scene, dso, options)) {
            return PageVisibility.FAMILY_HIDDEN;
        }
        if (!policy.drawn(dso)) {
            return PageVisibility.TOO_SMALL;
        }
        return PageVisibility.DRAWN;
    }

    /** A star is drawn when the page's magnitude limit admits it. */
    private static PageVisibility visibilityOf(ChartScene scene, Star star) {
        return star.magnitude() > scene.limitingMagnitude()
                ? PageVisibility.BELOW_LIMIT : PageVisibility.DRAWN;
    }

    /**
     * Whether a position lands on the paper. A star has no recorded
     * extent, so its centre is the whole question.
     */
    private static boolean onPaper(GnomonicProjection projection,
                                   ViewportMapping mapping, ChartScene scene,
                                   SkyPosition position) {
        PixelPoint pixel = projection.project(position)
                .map(mapping::toPixel).orElse(null);
        return pixel != null && ChartRenderer.paperOf(scene)
                .contains(pixel.x(), pixel.y());
    }
}
