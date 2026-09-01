package juranometria.ui;

import java.util.OptionalDouble;

import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.search.SearchResult;

/**
 * What the atlas does with a search result: the whole policy, with no
 * Swing in it (Sprint 21, issue #186).
 *
 * <p>It used to live inside {@link SearchField}, which meant the only
 * way to exercise it was to type into a text field on a display. The
 * packaged acceptance runs headless, and reconstructing the expected
 * navigation there proved nothing about the path a reader takes -
 * it would have passed just as well if choosing a result had
 * recentred on the wrong place, lost the target identity, or titled
 * the page by the wrong name (sprint review, P1).
 *
 * <p>So the decision lives here and the field calls it. There is one
 * policy, and both the reader's keystroke and the packaged evidence
 * go through it.
 */
public final class SearchNavigation {

    private SearchNavigation() {
    }

    /** What happened to the chart. */
    public enum Outcome {
        /** Recentred at the current field width. */
        RECENTERED,
        /** Recentred, narrowed to the widest field the pack covers. */
        RECENTERED_NARROWER,
        /** The pack does not cover this position at any field. */
        NO_FIT
    }

    /**
     * Recentres under the coverage policy; the chart never moves on
     * {@link Outcome#NO_FIT}.
     *
     * <p>A named object titles the chart and becomes its target;
     * coordinates leave it anonymous, so the title falls back to the
     * position itself. A found object is also selected, which is the
     * keyboard-only route into the Inspector - there is no cursor
     * that walks from star to star. {@code selection} may be null
     * where nothing is listening.
     */
    public static Outcome apply(SearchResult result,
                                SceneAssembler assembler,
                                ChartViewController controller,
                                SelectionModel selection) {
        boolean coordinates = result.kind() == SearchResult.Kind.COORDINATES;
        String targetLabel = coordinates ? null : result.regionTitle();
        String targetIdentity = coordinates ? null : result.identity();
        double currentField = controller.state().fieldWidthDegrees();
        if (assembler.fits(result.position(), currentField)) {
            controller.recenter(result.position(), targetLabel,
                    targetIdentity);
            establishSelection(result, selection);
            return Outcome.RECENTERED;
        }
        OptionalDouble widest =
                assembler.widestFittingFieldDegrees(result.position());
        if (widest.isPresent()) {
            controller.recenter(result.position(), widest.getAsDouble(),
                    targetLabel, targetIdentity);
            establishSelection(result, selection);
            return Outcome.RECENTERED_NARROWER;
        }
        return Outcome.NO_FIT;
    }

    /**
     * The reviewed relationship between search and selection (issue
     * #170): finding an object by name selects it, so the inspector
     * can describe what the reader just looked up.
     *
     * <p>Coordinates select nothing: the reader asked for a place,
     * not for an object, and no object was found there.
     */
    private static void establishSelection(SearchResult result,
                                           SelectionModel selection) {
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
}
