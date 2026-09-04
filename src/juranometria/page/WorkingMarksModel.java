package juranometria.page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The reader's working marks: ephemeral questions asked of the
 * current page, never annotations saved onto the sky (Sprint 24,
 * issue #215).
 *
 * <p>Zero or more stable catalogue identities in the order they were
 * marked, plus <strong>one lead</strong> - the item whose facts a
 * panel shows. The lead feeds the existing singular
 * {@code SelectionModel} without turning it into a multi-selection.
 *
 * <p>No Swing, no AWT, no preferences. A future module subscribes
 * here without the chart learning what a table is, and nothing
 * survives the session: there are no observing lists, no notes, no
 * import and no export.
 *
 * <p>Delivery follows {@code SelectionModel}: consumers are told a
 * whole state and never a partial one, and a listener that marks
 * something while being told about a mark does not interleave two
 * stories - its change is queued and delivered in turn, in one order
 * for every consumer.
 */
/**
 * The page surfaces' view of the working selection (Sprint 24,
 * issue #215; narrowed to a one-way adapter by Sprint 27, issue
 * #260).
 *
 * <p>The membership truth now lives in
 * {@link juranometria.chart.WorkingSelection} — one session-level
 * model, never pruned by navigation, never persisted. This class
 * keeps the Sprint 24 API its consumers were reviewed against and
 * holds <strong>no state of its own</strong>: every write forwards
 * to the one model, every read comes from it, and every
 * notification is the model's own, re-addressed. Two models that
 * could disagree are one model with two names.
 *
 * <p>{@link #pruneTo} survives here, not on the model: it is the
 * compatibility seam that preserves today's page-bound surface
 * behaviour — computed against the model's state and applied as
 * one whole replace — until the reader surfaces move to the
 * cross-page semantics. <strong>Retirement path:</strong> the
 * surfaces issue (#261) moves the table, module and journeys onto
 * {@code WorkingSelection} and the decided gestures; when its last
 * consumer moves, this adapter and its prune go with it.
 */
public final class WorkingMarksModel {

    /**
     * What consumers are told - the Sprint 24 shape, unchanged:
     * always the whole state.
     */
    public record Change(List<String> marks, String lead) {

        public Change {
            marks = List.copyOf(marks);
            if (new java.util.HashSet<>(marks).size() != marks.size()) {
                throw new IllegalArgumentException(
                        "a marked set holds each identity once: "
                                + marks);
            }
            if (lead != null && !marks.contains(lead)) {
                throw new IllegalArgumentException(
                        "the lead is always one of the marks: " + lead
                                + " is not among " + marks);
            }
            if (lead == null && !marks.isEmpty()) {
                throw new IllegalArgumentException(
                        "marks without a lead: " + marks);
            }
        }

        /** Nothing is marked. */
        public boolean isEmpty() {
            return marks.isEmpty();
        }
    }

    private final juranometria.chart.WorkingSelection model;

    /** An adapter over its own private model - tests and fixtures. */
    public WorkingMarksModel() {
        this(new juranometria.chart.WorkingSelection());
    }

    /** The production shape: one model, this view of it. */
    public WorkingMarksModel(juranometria.chart.WorkingSelection model) {
        if (model == null) {
            throw new IllegalArgumentException(
                    "the adapter is a view of the one model");
        }
        this.model = model;
    }

    /** The one model this adapter re-addresses. */
    public juranometria.chart.WorkingSelection model() {
        return model;
    }

    /** The marked identities, in the order they were marked. */
    public List<String> marks() {
        return model.members();
    }

    /** The lead identity, or null when nothing is marked. */
    public String lead() {
        return model.lead();
    }

    public boolean isMarked(String identity) {
        return model.isMember(identity);
    }

    /**
     * Subscribes a consumer to the one model's changes, re-addressed
     * in the Sprint 24 shape, and returns the releasing handle.
     */
    public Runnable onChange(Consumer<Change> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        return model.onChange(change -> listener.accept(
                new Change(change.members(), change.lead())));
    }

    /** Marks an identity and makes it the lead. */
    public void mark(String identity) {
        model.add(identity);
    }

    /** Unmarks an identity, doing nothing if it was not marked. */
    public void unmark(String identity) {
        model.remove(identity);
    }

    /** Makes an already-marked identity the lead. */
    public void lead(String identity) {
        model.lead(identity);
    }

    /** Replaces the whole marked set in one transition. */
    public void replaceWith(List<String> identities, String lead) {
        model.replaceWith(identities, lead);
    }

    /** Nothing is marked. */
    public void clear() {
        model.clear();
    }

    /**
     * Drops every mark the new page does not hold, as one change -
     * the page-bound behaviour today's surfaces were reviewed
     * against, computed here and applied as one whole replace on
     * the model. The compatibility seam, with its retirement path
     * in the class comment: the model itself has no prune, and the
     * cross-page semantics arrive with the surfaces issue.
     */
    public void pruneTo(PageContents page) {
        if (page == null) {
            throw new IllegalArgumentException("a page to prune to");
        }
        List<String> current = model.members();
        if (current.isEmpty()) {
            return;
        }
        List<String> survivors = new ArrayList<>();
        for (String identity : current) {
            if (page.holds(identity)) {
                survivors.add(identity);
            }
        }
        if (survivors.size() == current.size()) {
            return;                // nothing left the page
        }
        model.replaceWith(survivors,
                survivors.contains(model.lead()) ? model.lead()
                        : survivors.isEmpty() ? null
                                : survivors.get(survivors.size() - 1));
    }
}
