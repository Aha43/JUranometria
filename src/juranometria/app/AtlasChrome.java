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
 * <p><strong>What it owns.</strong> The session-derived words, and
 * the composition of the application's chrome: the search field, the
 * toolbar and the menu bar. Those are the three reader-facing
 * surfaces that take a language, and one place deciding which
 * language they get is the whole point.
 *
 * <p><strong>What it does not.</strong> The frame, the chart, the
 * modules, the domain actions or any lifecycle. The menu's callbacks
 * are handed in and never learned: this seam does not know what
 * Settings opens or what Export writes, only which words the bar says
 * while offering them. Making all of {@code JUranometriaMain}
 * testable is a different and much larger decision, and not one this
 * defect justifies.
 *
 * <p>It was called {@code AtlasControls} and said it was "not a home
 * for the menu bar" - and then the menu was added to it, because the
 * session's language has to reach the menu the same way it reaches
 * the toolbar. The name and the contract were renamed to match what
 * it does rather than the sentence being quietly deleted (#350).
 */
public final class AtlasChrome {

    private final SearchField searchField;
    private final AtlasToolbar toolbar;

    private final InterfaceText said;

    private AtlasChrome(SearchField searchField, AtlasToolbar toolbar,
                          InterfaceText said) {
        this.searchField = searchField;
        this.toolbar = toolbar;
        this.said = said;
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
    public static AtlasChrome of(SkyLanguageSession language,
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
        return new AtlasChrome(field, bar, said);
    }

    /** The field a reader types into. */
    public SearchField searchField() {
        return searchField;
    }

    /** The bar the field sits on. */
    public AtlasToolbar toolbar() {
        return toolbar;
    }

    /**
     * The menu bar, in the same language as everything else.
     *
     * <p>Here for the same reason the toolbar is: the menu is a
     * reader-facing surface the application composes, and "does the
     * application hand it the session's language?" needs an address.
     * The handlers stay the application's - this seam does not learn
     * what Settings or Export do, only which words the bar says.
     */
    public javax.swing.JMenuBar menuBar(ChartViewController navigation,
                                        Runnable openSettings,
                                        Runnable openChartOptions,
                                        Runnable openAbout,
                                        Runnable toggleInspector,
                                        Runnable openPlaceAndTime,
                                        Runnable toggleEcliptic,
                                        Runnable exportSheet) {
        return AppMenuBar.create(navigation, openSettings, openChartOptions,
                openAbout, toggleInspector, openPlaceAndTime, toggleEcliptic,
                exportSheet, said);
    }
}
