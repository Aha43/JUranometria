package juranometria.catalog;

import java.util.List;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;

/**
 * A bounded spatial view of catalogue data. Implementations hold their data
 * in memory once loaded; queries never read files or the network, so
 * painting can depend on query results without doing either.
 */
public interface Catalogue {

    /** Stars inside the region. */
    List<Star> starsIn(SkyRegion region);

    /** Deep-sky objects inside the region. */
    List<DeepSkyObject> deepSkyObjectsIn(SkyRegion region);
}
