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
                CATALOGUE.starsIn(wholeSky()), CATALOGUE.deepSkyObjectsIn(wholeSky()),
                GEOGRAPHY.constellations());

        private static SceneAssembler assembler(TiledCatalogue catalogue) {
            if (!"all-sky".equals(catalogue.manifest().coverage())) {
                throw new IllegalStateException("the bundled pack declares coverage "
                        + catalogue.manifest().coverage()
                        + "; regional packs need a data centre this wiring does not define");
            }
            return SceneAssembler.allSky(catalogue,
                    catalogue.manifest().maxObjectSemiExtentDegrees(), GEOGRAPHY);
        }

        static final juranometria.ui.language.SkyLanguageChoice.Available
                LANGUAGES = languages();
        static final juranometria.geo.SkyNames NAMES =
                juranometria.geo.SkyNames.discover();

        private static SkyRegion wholeSky() {
            return new SkyRegion(DEFAULT_CENTRE, 180.0);
        }

        /**
         * The one place the two language registries meet.
         *
         * <p>Each answers its own domain from its own resources:
         * which languages the controls can speak, and which languages
         * the sky can be named in. Neither imports the other, and
         * this is the only code that holds both - so a chart pack
         * cannot become an interface language anywhere, because
         * nowhere else is in a position to confuse them.
         */
        private static juranometria.ui.language.SkyLanguageChoice.Available
                languages() {
            return new juranometria.ui.language.SkyLanguageChoice.Available(
                    juranometria.ui.language.InterfaceLanguages
                            .discover().tagSet(),
                    java.util.Set.copyOf(juranometria.geo.SkyNames
                            .discover().chartLanguages()));
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

    /**
     * The same assembler, naming the sky in a language a caller
     * states.
     *
     * <p>The language is a parameter, never something read here. This
     * class holds no session state and touches no preference node:
     * what the reader is currently showing is
     * {@code SkyLanguageSession}'s to know, and it hands the resolved
     * answer in. An assembler that fetched the language itself would
     * make every page depend on when it was assembled rather than on
     * what it was asked for.
     */
    public static SceneAssembler assemblerNamedIn(String chartLanguage) {
        return Holder.ASSEMBLER.namedIn(chartLanguage, Holder.NAMES);
    }

    /**
     * What this installation can offer the reader, in both settings.
     *
     * <p>Assembled from the two registries rather than from either:
     * interface languages from the descriptors that ship, chart
     * languages from the packs that ship. A value a build cannot
     * offer is what {@code SkyLanguageStore} resolves a stored
     * choice against, so this is what decides whether a remembered
     * language is honoured or fallen back from.
     */
    public static juranometria.ui.language.SkyLanguageChoice.Available
            languages() {
        return Holder.LANGUAGES;
    }
}
