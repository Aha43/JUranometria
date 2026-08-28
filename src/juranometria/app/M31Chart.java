package juranometria.app;

import java.util.List;

import juranometria.catalog.BundledCatalogue;
import juranometria.catalog.Catalogue;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;

/**
 * Assembles the fixed M31 chart from the bundled regional catalogue.
 * The catalogue is loaded once, at first use during application setup,
 * never during painting.
 */
public final class M31Chart {

    public static final SkyPosition CENTRE = new SkyPosition(10.684708, 41.268750);
    public static final String TITLE = "M31 · Andromeda Galaxy region";

    /** Covers the corners of an 8-degree-wide chart at common aspect ratios. */
    private static final double QUERY_RADIUS_DEGREES = 6.0;

    private M31Chart() {
    }

    private static final class Holder {
        static final Catalogue CATALOGUE = BundledCatalogue.load();
    }

    /** The M31-region stars from the bundled catalogue. */
    public static List<Star> loadStars() {
        return Holder.CATALOGUE.starsIn(new SkyRegion(CENTRE, QUERY_RADIUS_DEGREES));
    }

    /** The M31-region deep-sky objects from the bundled catalogue. */
    public static List<DeepSkyObject> loadDeepSkyObjects() {
        return Holder.CATALOGUE.deepSkyObjectsIn(new SkyRegion(CENTRE, QUERY_RADIUS_DEGREES));
    }
}
