package juranometria.ui;

import java.awt.Dimension;
import java.util.List;
import java.util.OptionalDouble;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import juranometria.search.LocalSearch;
import juranometria.chart.Selection;
import juranometria.search.SearchResult;

/**
 * The atlas search field: resolves object names, identifiers, and
 * coordinates through {@link LocalSearch} (no parsing of its own) and
 * recentres through the shared controller under the coverage policy:
 * keep the current field width when the complete view fits; otherwise
 * take the widest supported step that fits; otherwise leave the chart
 * unchanged and say so concisely.
 *
 * Enter resolves; a single or exact result applies immediately; multiple
 * results open a small keyboard-navigable list (arrows and Enter select,
 * Escape dismisses and returns focus to the field).
 */
public final class SearchField extends JTextField {

    /** Optional: the shared selection, when the application has one. */
    private juranometria.chart.SelectionModel selection;

    /** What a search interaction did, for tests and popup decisions. */
    enum Outcome {
        EMPTY, NO_MATCH, RECENTERED, RECENTERED_NARROWER, NO_FIT, CHOICES
    }

    /**
     * A famous name such as M42 finds nothing because only the M31 region
     * is bundled, and nonsense finds nothing because it names nothing; the
     * catalogue cannot tell those apart, so the message states the one
     * fact true of both — no match in the bundled data — without implying
     * the query names a real object.
     */
    private static final String NO_MATCH = "search.nomatch";
    /**
     * Unreachable under an all-sky pack; kept for regional packs.
     *
     * <p>{@code NO_FIT} is returned only when no field width fits a
     * result's position, which cannot happen when every position fits
     * at some rung. A regional catalogue can still produce it, so it
     * is held by a constructed fixture in both languages rather than
     * deleted for being quiet (#350).
     */
    private static final String NO_FIT = "search.nofit";

    private final juranometria.ui.language.InterfaceText said;
    private final LocalSearch search;
    private final SceneAssembler assembler;
    private final ChartViewController controller;
    private JPopupMenu popup;

    /** Told what the reader found, when anything is listening. */
    public void setSelectionModel(juranometria.chart.SelectionModel model) {
        this.selection = model;
    }

    /** Optional: the working selection and its mode (issue #261). */
    private juranometria.chart.WorkingSelection working;
    private juranometria.chart.SelectionMode mode;

    /**
     * Tells search where the working selection lives, so a found
     * object joins the set - replacing it ordinarily, added to it
     * when gestures accumulate.
     */
    public void setWorkingSelection(
            juranometria.chart.WorkingSelection working,
            juranometria.chart.SelectionMode mode) {
        this.working = working;
        this.mode = mode;
    }

    /**
     * English, for harnesses inside this package.
     *
     * <p><strong>Package-private on purpose</strong> (#350). A public
     * constructor that quietly chose English is how the toolbar came
     * to be externalised, translated, proved by eight passing
     * contracts, and still shown in English to a reader who had asked
     * for Norwegian: the application called the defaulting form and
     * every test passed a language explicitly, so nothing could see
     * it. Production and application journeys now cannot reach this;
     * the compiler says so, which a grep could not.
     */
    SearchField(LocalSearch search, SceneAssembler assembler,
                ChartViewController controller) {
        this(search, assembler, controller,
                juranometria.ui.language.InterfaceText.forLanguage("en"));
    }

    /**
     * The field in a language a caller states (issue #350).
     *
     * <p>A separate language surface from the toolbar it sits on: its
     * grammar is its own, and the four examples below are canonical
     * designations and a canonical coordinate pair rather than words.
     */
    public SearchField(LocalSearch search, SceneAssembler assembler,
                       ChartViewController controller,
                       juranometria.ui.language.InterfaceText said) {
        super(14);
        if (said == null) {
            throw new IllegalArgumentException(
                    "the field has to say its words in some language");
        }
        this.search = search;
        this.assembler = assembler;
        this.controller = controller;
        this.said = said;
        putClientProperty("JTextField.placeholderText",
                said.say("search.placeholder"));
        getAccessibleContext().setAccessibleName(said.say("search.a11y"));
        // A field whose expected input is not obvious: the examples
        // are the explanation, and they belong where a reader who is
        // about to type can see them. The spoken form gives the same
        // four shapes without leaning on a placeholder nobody hears.
        //
        // The examples go in as arguments. They are designations and
        // a coordinate pair, not words, and a translator handed a
        // sentence with them already written into it would own a copy
        // of the catalogue's spelling (#350).
        Explain.control(this,
                said.say("search.hover", EXAMPLES[0], EXAMPLES[1],
                        EXAMPLES[2], EXAMPLES[3]),
                said.say("search.explain"));
        setMaximumSize(new Dimension(220, Integer.MAX_VALUE));
        addActionListener(event -> handle(getText()));
    }

    /**
     * The four shapes a reader may type, as the catalogue spells them.
     *
     * <p>Canonical: a Messier number, an NGC number, a Tycho identity
     * and a right ascension with a declination. No language changes
     * any of them, so they live here and are handed to whichever
     * sentence needs them.
     */
    private static final String[] EXAMPLES = {
            "M 31", "NGC 224", "TYC 2801-2090-1", "0:42:44 +41:16:09"};

    /** Clears the query text and any open result list. */
    public void clearSearch() {
        setText("");
        hidePopup();
    }

    /** Resolves a query and reacts; returns the outcome for tests. */
    Outcome handle(String query) {
        hidePopup();
        if (query == null || query.isBlank()) {
            return Outcome.EMPTY;
        }
        List<SearchResult> results = search.search(query);
        if (results.isEmpty()) {
            showMessage(NO_MATCH);
            return Outcome.NO_MATCH;
        }
        if (results.size() == 1) {
            Outcome outcome = apply(results.get(0));
            if (outcome == Outcome.NO_FIT) {
                showMessage(NO_FIT);
            }
            return outcome;
        }
        showPopup(resultsPopup(results));
        return Outcome.CHOICES;
    }

    /**
     * Recentres under the coverage policy; the chart never moves on
     * NO_FIT.
     *
     * <p>The policy itself lives in {@link SearchNavigation}, with no
     * Swing around it, so the packaged acceptance can take the same
     * path a reader's Enter takes rather than reconstructing where
     * the chart ought to have gone (Sprint 21 review, P1).
     */
    Outcome apply(SearchResult result) {
        return switch (SearchNavigation.apply(result, assembler,
                controller, selection, working, mode)) {
            case RECENTERED -> Outcome.RECENTERED;
            case RECENTERED_NARROWER -> Outcome.RECENTERED_NARROWER;
            case NO_FIT -> Outcome.NO_FIT;
        };
    }

    /** A keyboard-navigable list; every item runs the same apply policy. */
    JPopupMenu resultsPopup(List<SearchResult> results) {
        JPopupMenu menu = new JPopupMenu();
        for (SearchResult result : results) {
            JMenuItem item = new JMenuItem(itemText(result));
            item.addActionListener(event -> {
                if (apply(result) == Outcome.NO_FIT) {
                    showMessage(NO_FIT);
                }
            });
            // The item's own words are the object's name and its
            // catalogue identity, which is the whole meaning; a
            // tooltip over a list a reader is walking with the arrow
            // keys would be in the way of the list.
            Explain.selfExplanatory(item,
                    said.say("search.result.explain", result.label()));
            menu.add(item);
        }
        return menu;
    }

    private static String itemText(SearchResult result) {
        return result.label().equals(result.identity())
                ? result.label()
                : result.label() + " · " + result.identity();
    }

    /**
     * A failure, said whole.
     *
     * <p>The explanation used to be built as {@code "Nothing to
     * choose: " + message.toLowerCase(ROOT) + ". Try another name, or
     * coordinates."} - a whole sentence lowercased by an English rule
     * and spliced into another sentence as a clause. German would
     * capitalise the noun and Norwegian would not phrase it that way
     * at all. Each message now owns a label and an explanation, and
     * neither is derived from the other (#350).
     */
    private void showMessage(String stem) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem(said.say(stem + ".label"));
        item.setEnabled(false);
        // Disabled, and the one place in the atlas where that is the
        // whole message rather than a control gone quiet - so it says
        // what to do next instead of why it cannot be pressed. Unlike
        // the toolbar's version label, this really is unavailable:
        // there is nothing to choose.
        Explain.selfExplanatory(item, said.say(stem + ".explain"));
        menu.add(item);
        showPopup(menu);
    }

    private void showPopup(JPopupMenu menu) {
        hidePopup();
        popup = menu;
        // Headless tests exercise the wiring without a screen.
        if (isShowing()) {
            menu.show(this, 0, getHeight());
        }
    }

    private void hidePopup() {
        if (popup != null) {
            popup.setVisible(false);
            popup = null;
        }
    }
}
