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
 * <p>{@link #pruneTo} survives here, not on the model — and it
 * narrows only this <strong>view</strong> (review): the model is
 * never mutated by navigation, which is its defining cross-page
 * invariant, while this adapter keeps presenting the page-bound
 * picture today's surfaces were reviewed against - members the
 * current page does not hold are outside the view, and the view's
 * lead falls back to the last-marked visible member. The scope is
 * presentation state, never a second membership truth: no member
 * exists here that the model does not hold.
 * <strong>Retirement path:</strong> the surfaces issue (#261)
 * moves the table, module and journeys onto
 * {@code WorkingSelection} and the decided cross-page gestures;
 * when its last consumer moves, this adapter and its view scope go
 * with it.
 */
public final class WorkingMarksModel {

    /**
     * What consumers are told - the Sprint 24 shape, unchanged:
     * always the whole state.
     */
    public record Change(List<String> marks, String lead) {

        public Change {
            marks = List.copyOf(marks);
            for (String mark : marks) {
                if (mark == null || mark.isBlank()) {
                    throw new IllegalArgumentException(
                            "a mark is a catalogue identity: "
                                    + marks);
                }
            }
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

    /**
     * The page this view is scoped to, or null for the whole set -
     * presentation scope, never membership: a member outside the
     * scope is hidden here and untouched in the model.
     */
    private PageContents scope;

    /**
     * One model subscription and one serialized view queue
     * (review): every view listener hears the same states in the
     * same order, whether a transition arrived from the model or
     * from a scope change, and a nested {@code pruneTo} during a
     * delivery is queued in turn rather than broadcast midway -
     * the models' own discipline, applied to the view.
     */
    private final List<Consumer<Change>> viewListeners =
            new ArrayList<>();
    private final java.util.Deque<Change> pending =
            new java.util.ArrayDeque<>();
    private boolean delivering;
    /** The view state that has been delivered. */
    private Change delivered = new Change(List.of(), null);

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
        // The one model subscription, for the adapter's lifetime:
        // every model transition is re-addressed through the scope
        // and joins the same serialized queue as scope changes.
        model.onChange(change -> queue(viewOf(change)));
    }

    /** The one model this adapter re-addresses. */
    public juranometria.chart.WorkingSelection model() {
        return model;
    }

    /** The marked identities the view shows, in marked order. */
    public List<String> marks() {
        return delivered.marks();
    }

    /** The view's lead identity, or null when the view is empty. */
    public String lead() {
        return delivered.lead();
    }

    public boolean isMarked(String identity) {
        return delivered.marks().contains(identity);
    }

    /**
     * Subscribes a consumer to this view of the one model - told
     * the current view immediately (during a delivery, the state
     * being delivered), again for every model transition, and for
     * every view-scope change - and returns the releasing handle.
     */
    public Runnable onChange(Consumer<Change> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        viewListeners.add(listener);
        listener.accept(delivered);
        return () -> viewListeners.remove(listener);
    }

    private Change viewOf(
            juranometria.chart.WorkingSelection.Change change) {
        if (scope == null) {
            return new Change(change.members(), change.lead());
        }
        List<String> visible = new ArrayList<>();
        for (String identity : change.members()) {
            if (scope.holds(identity)) {
                visible.add(identity);
            }
        }
        String lead = visible.contains(change.lead()) ? change.lead()
                : visible.isEmpty() ? null
                        : visible.get(visible.size() - 1);
        return new Change(visible, lead);
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
     * Scopes this view to a page, as one change - the page-bound
     * picture today's surfaces were reviewed against. The model is
     * <strong>never</strong> touched (review): navigation narrows
     * what this view shows, and every member stays a member of the
     * one session set underneath.
     */
    public void pruneTo(PageContents page) {
        if (page == null) {
            throw new IllegalArgumentException("a page to prune to");
        }
        this.scope = page;
        queue(viewOf(new juranometria.chart.WorkingSelection
                .Change(model.members(), model.lead())));
    }

    /**
     * Queues a whole view state and drains, applying each before
     * delivering its own event - so a listener that prunes or
     * writes while being told enqueues in turn, and every listener
     * hears one order.
     */
    private void queue(Change change) {
        // Suppression lives at the shared boundary (review): a
        // model change entirely outside the scope maps to the view
        // state already delivered - or already queued - and is
        // nothing a consumer could observe, whichever door it came
        // through. A change that moves anything visible, the
        // fallback lead included, differs and publishes.
        Change base = pending.isEmpty() ? delivered
                : pending.peekLast();
        if (base.equals(change)) {
            return;
        }
        pending.addLast(change);
        if (delivering) {
            return;
        }
        delivering = true;
        try {
            Change next;
            while ((next = pending.pollFirst()) != null) {
                this.delivered = next;
                for (Consumer<Change> listener
                        : List.copyOf(viewListeners)) {
                    listener.accept(next);
                }
            }
        } finally {
            delivering = false;
            pending.clear();
        }
    }
}
