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
    private static final String NO_MATCH_MESSAGE =
            "No match in the bundled catalogue";
    /** Unreachable under an all-sky pack; kept for regional packs. */
    private static final String NO_FIT_MESSAGE =
            "Found, but beyond this pack's coverage";

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

    public SearchField(LocalSearch search, SceneAssembler assembler,
                       ChartViewController controller) {
        super(14);
        this.search = search;
        this.assembler = assembler;
        this.controller = controller;
        putClientProperty("JTextField.placeholderText", "Search");
        getAccessibleContext().setAccessibleName("Search the atlas");
        // A field whose expected input is not obvious: the examples
        // are the explanation, and they belong where a reader who is
        // about to type can see them. The spoken form gives the same
        // four shapes without leaning on a placeholder nobody hears.
        Explain.control(this,
                "Find an object or coordinates, e.g. M 31, NGC 224,"
                        + " TYC 2801-2090-1, or 0:42:44 +41:16:09",
                "Type a Messier or NGC number, a star's catalogue"
                        + " identity, or a right ascension and"
                        + " declination, then press Enter; the chart"
                        + " goes there and marks it");
        setMaximumSize(new Dimension(220, Integer.MAX_VALUE));
        addActionListener(event -> handle(getText()));
    }

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
            showMessage(NO_MATCH_MESSAGE);
            return Outcome.NO_MATCH;
        }
        if (results.size() == 1) {
            Outcome outcome = apply(results.get(0));
            if (outcome == Outcome.NO_FIT) {
                showMessage(NO_FIT_MESSAGE);
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
                    showMessage(NO_FIT_MESSAGE);
                }
            });
            // The item's own words are the object's name and its
            // catalogue identity, which is the whole meaning; a
            // tooltip over a list a reader is walking with the arrow
            // keys would be in the way of the list.
            Explain.selfExplanatory(item,
                    "Goes to " + result.label() + " and marks it");
            menu.add(item);
        }
        return menu;
    }

    private static String itemText(SearchResult result) {
        return result.label().equals(result.identity())
                ? result.label()
                : result.label() + " · " + result.identity();
    }

    private void showMessage(String message) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem(message);
        item.setEnabled(false);
        // Disabled, and the one place in the atlas where that is the
        // whole message rather than a control gone quiet - so it says
        // what to do next instead of why it cannot be pressed.
        Explain.selfExplanatory(item,
                "Nothing to choose: " + message.toLowerCase(
                        java.util.Locale.ROOT)
                        + ". Try another name, or coordinates.");
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
