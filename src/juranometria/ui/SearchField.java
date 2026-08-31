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

    public SearchField(LocalSearch search, SceneAssembler assembler,
                       ChartViewController controller) {
        super(14);
        this.search = search;
        this.assembler = assembler;
        this.controller = controller;
        putClientProperty("JTextField.placeholderText", "Search");
        setToolTipText("Find an object or coordinates, e.g. M 31, NGC 224,"
                + " TYC 2801-2090-1, or 0:42:44 +41:16:09");
        getAccessibleContext().setAccessibleName("Search the atlas");
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
     * The reviewed relationship between search and selection (issue
     * #170): finding an object by name selects it, so the inspector
     * can describe what the reader just looked up. This is also the
     * keyboard-only route into the inspector - there is no cursor
     * that walks from star to star, so a reader without a pointer
     * arrives by searching.
     *
     * <p>Coordinates select nothing: the reader asked for a place,
     * not for an object, and no object was found there.
     */
    private void establishSelection(SearchResult result) {
        if (selection == null) {
            return;
        }
        switch (result.kind()) {
            case STAR -> selection.select(new Selection.Object(
                    Selection.Object.Kind.STAR, result.identity(),
                    result.position()));
            case DEEP_SKY_OBJECT -> selection.select(new Selection.Object(
                    Selection.Object.Kind.DEEP_SKY, result.identity(),
                    result.position()));
            default -> selection.clear();
        }
    }

    /** Recentres under the coverage policy; the chart never moves on NO_FIT. */
    Outcome apply(SearchResult result) {
        // A named object titles the chart; coordinates leave it anonymous
        // so the title falls back to the position itself.
        boolean coordinates = result.kind() == SearchResult.Kind.COORDINATES;
        String targetLabel = coordinates ? null : result.regionTitle();
        String targetIdentity = coordinates ? null : result.identity();
        double currentField = controller.state().fieldWidthDegrees();
        if (assembler.fits(result.position(), currentField)) {
            controller.recenter(result.position(), targetLabel, targetIdentity);
            establishSelection(result);
            return Outcome.RECENTERED;
        }
        OptionalDouble widest = assembler.widestFittingFieldDegrees(result.position());
        if (widest.isPresent()) {
            controller.recenter(result.position(), widest.getAsDouble(),
                    targetLabel, targetIdentity);
            establishSelection(result);
            return Outcome.RECENTERED_NARROWER;
        }
        return Outcome.NO_FIT;
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
