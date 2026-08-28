package juranometria.app;

import java.util.List;

import juranometria.catalog.FixtureCatalogue;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;

/**
 * Assembles the fixed Sprint 1 chart: the M31 region from the bundled
 * fixture. Loading happens up front, never during painting.
 */
public final class M31Chart {

    public static final SkyPosition CENTRE = new SkyPosition(10.684708, 41.268750);
    public static final double FIELD_WIDTH_DEGREES = 8.0;
    public static final String TITLE = "M31 · Andromeda Galaxy region";

    /** The fixture's stellar depth; stated in the title block. */
    public static final double LIMITING_MAGNITUDE = 8.0;

    /** Covers the corners of an 8-degree-wide chart at common aspect ratios. */
    private static final double QUERY_RADIUS_DEGREES = 6.0;

    private M31Chart() {
    }

    /** Loads the fixture stars around the M31 centre. */
    public static List<Star> loadStars() {
        return FixtureCatalogue.loadBundled()
                .starsIn(new SkyRegion(CENTRE, QUERY_RADIUS_DEGREES));
    }

    /** Loads the fixture deep-sky objects around the M31 centre. */
    public static List<DeepSkyObject> loadDeepSkyObjects() {
        return FixtureCatalogue.loadBundled()
                .deepSkyObjectsIn(new SkyRegion(CENTRE, QUERY_RADIUS_DEGREES));
    }
}
