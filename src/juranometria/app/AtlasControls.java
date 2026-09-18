package juranometria.app;

import juranometria.chart.SelectionMode;
import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartViewController;
import juranometria.ui.InspectorToggle;
import juranometria.ui.SceneAssembler;
import juranometria.ui.SearchField;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.SkyLanguageSession;
import juranometria.search.LocalSearch;

/**
 * Where the session's language reaches the reader's controls
 * (Sprint 33, issue #350).
 *
 * <p>This seam exists because of a defect it now makes impossible.
 * The toolbar was externalised, translated into Norwegian and proved
 * by eight passing contracts - and a reader who chose Norwegian still
 * saw an English toolbar, because the application called a
 * constructor that quietly defaulted to English. Every contract
 * passed a language explicitly, so not one of them could see it.
 *
 * <p>A component test asks <em>can this surface speak Norwegian?</em>
 * That question was answered correctly all along. The question nobody
 * was asking is <em>does the application hand it Norwegian?</em> -
 * and that one has an address only if the composition has a name. So
 * it has one, and exactly one thing lives here: the derivation of the
 * interface language from the session, and its delivery to both
 * surfaces that take it.
 *
 * <p>Deliberately narrow. This is not a home for the frame, the
 * chart, the modules or the menu bar; making all of
 * {@code JUranometriaMain} testable is a different and much larger
 * decision, and not one this defect justifies.
 */
public final class AtlasControls {

    private final SearchField searchField;
    private final AtlasToolbar toolbar;

    private AtlasControls(SearchField searchField, AtlasToolbar toolbar) {
        this.searchField = searchField;
        this.toolbar = toolbar;
    }

    /**
     * Builds the search field and the toolbar in the session's
     * interface language.
     *
     * <p>The language is read from the session once and handed to
     * both. Neither is given the chance to ask for a default, and
     * neither asks the platform locale: what a reader chose is the
     * only answer either of them gets.
     */
    public static AtlasControls of(SkyLanguageSession language,
                                   ChartViewController controller,
                                   LocalSearch search,
                                   SceneAssembler assembler,
                                   InspectorToggle inspector,
                                   String versionText,
                                   Runnable requestExit,
                                   SelectionMode selectionMode) {
        if (language == null) {
            throw new IllegalArgumentException(
                    "the controls speak the session's language");
        }
        InterfaceText said =
                InterfaceText.forLanguage(language.interfaceLanguage());
        SearchField field = new SearchField(search, assembler, controller,
                said);
        AtlasToolbar bar = new AtlasToolbar(controller, field, inspector,
                versionText, requestExit, selectionMode, said);
        return new AtlasControls(field, bar);
    }

    /** The field a reader types into. */
    public SearchField searchField() {
        return searchField;
    }

    /** The bar the field sits on. */
    public AtlasToolbar toolbar() {
        return toolbar;
    }
}
