package juranometria.app;

import juranometria.catalog.TiledCatalogue;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.search.LocalSearch;
import juranometria.ui.SceneAssembler;

/**
 * The application's data wiring: the bundled tiled catalogue behind an
 * assembler whose coverage comes from the pack manifest, and the local
 * search built over the complete pack. Everything loads at first use
 * during application setup, never during painting; the default chart
 * remains the M31 region.
 */
public final class Atlas {

    public static final SkyPosition DEFAULT_CENTRE = new SkyPosition(10.684708, 41.268750);

    private Atlas() {
    }

    private static final class Holder {
        static final TiledCatalogue CATALOGUE = TiledCatalogue.load();
        static final juranometria.geo.ConstellationGeography GEOGRAPHY =
                juranometria.geo.ConstellationGeography.load();
        static final SceneAssembler ASSEMBLER = assembler(CATALOGUE);
        static final LocalSearch SEARCH = new LocalSearch(
                CATALOGUE.starsIn(wholeSky()), CATALOGUE.deepSkyObjectsIn(wholeSky()));

        private static SceneAssembler assembler(TiledCatalogue catalogue) {
            if (!"all-sky".equals(catalogue.manifest().coverage())) {
                throw new IllegalStateException("the bundled pack declares coverage "
                        + catalogue.manifest().coverage()
                        + "; regional packs need a data centre this wiring does not define");
            }
            return SceneAssembler.allSky(catalogue,
                    catalogue.manifest().maxObjectSemiExtentDegrees(), GEOGRAPHY);
        }

        private static SkyRegion wholeSky() {
            return new SkyRegion(DEFAULT_CENTRE, 180.0);
        }
    }

    /** The application's scene assembler over the bundled pack. */
    public static SceneAssembler assembler() {
        return Holder.ASSEMBLER;
    }

    /** The application's local search over the same bundled pack. */
    public static LocalSearch search() {
        return Holder.SEARCH;
    }
}
