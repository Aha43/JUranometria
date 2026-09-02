package juranometria.page;

import java.util.Optional;

import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;

/**
 * The bridge between the marked set's lead and the chart's existing
 * singular selection (Sprint 24, issue #215).
 *
 * <p>A reader marks several objects and reads the facts of one of
 * them - the lead. The chart already has exactly one way to say
 * "this object is the one being read about": {@link SelectionModel}.
 * Teaching it about sets would put multi-selection into a model
 * every other surface depends on being singular, so the lead is
 * <em>fed</em> to it instead, and the set keeps its own seam.
 *
 * <p>The direction is one way. Marks lead the selection; the
 * selection does not silently mark. A reader who clicks the chart
 * has selected something, not marked it, and the difference is the
 * point of working marks being ephemeral.
 */
public final class LeadSelection {

    private LeadSelection() {
    }

    /**
     * Keeps {@code selection} showing whatever the marks lead with,
     * for as long as the returned handle is not run.
     *
     * <p>The lead's identity has to become a selectable object, and
     * only the page knows where an identity is and what kind of
     * thing it is - so the page is asked, and an identity the page
     * no longer holds selects nothing rather than a guess.
     */
    public static Runnable connect(WorkingMarksModel marks,
                                   SelectionModel selection,
                                   java.util.function.Supplier<PageContents> page) {
        if (marks == null || selection == null || page == null) {
            throw new IllegalArgumentException(
                    "a bridge needs marks, a selection and a page");
        }
        return marks.onChange(change -> {
            if (change.lead() == null) {
                selection.clear();
                return;
            }
            Optional<Selection.Object> object =
                    selectable(page.get(), change.lead());
            if (object.isPresent()) {
                selection.select(object.get());
            } else {
                selection.clear();
            }
        });
    }

    /** The lead, as the chart's own idea of a selected object. */
    public static Optional<Selection.Object> selectable(PageContents page,
                                                        String identity) {
        if (page == null || identity == null) {
            return Optional.empty();
        }
        return page.find(identity).map(entry -> new Selection.Object(
                entry instanceof PageEntry.DeepSky
                        ? Selection.Object.Kind.DEEP_SKY
                        : Selection.Object.Kind.STAR,
                entry.identity(), entry.position()));
    }
}
