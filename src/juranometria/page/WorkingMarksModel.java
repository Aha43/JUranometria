package juranometria.page;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
public final class WorkingMarksModel {

    /**
     * What consumers are told. Always the whole state: the marks in
     * order and which of them leads, never a delta a listener has to
     * reconstruct.
     */
    public record Change(List<String> marks, String lead) {

        public Change {
            marks = List.copyOf(marks);
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

    private final List<Consumer<Change>> listeners = new ArrayList<>();

    /**
     * Transitions awaiting delivery, each a whole state.
     *
     * <p>Masking the accessors was not enough (review). The model
     * moved the moment a mutator was called, so a listener that
     * marked something while being told about a mark was computing
     * its own change against a state nobody could see - and the
     * fields disagreed with the event in flight underneath the mask.
     *
     * <p>So this follows {@code SelectionModel} exactly: a
     * transition is queued <strong>whole</strong> and applied
     * immediately before its own event is delivered. The fields
     * always describe the change being delivered, because they are
     * set from it.
     */
    private final Deque<Change> pending = new ArrayDeque<>();
    private boolean delivering;

    /** The state that has been delivered. */
    private List<String> marks = List.of();
    private String lead;

    /** The marked identities, in the order they were marked. */
    public List<String> marks() {
        return marks;
    }

    /** The lead identity, or null when nothing is marked. */
    public String lead() {
        return lead;
    }

    public boolean isMarked(String identity) {
        return marks.contains(identity);
    }

    /**
     * Subscribes a consumer and returns the handle that unsubscribes
     * it. The consumer is told the current state immediately, so it
     * never has to ask separately what it missed - and during a
     * delivery that is the state being delivered, because the fields
     * are it.
     */
    public Runnable onChange(Consumer<Change> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
        listener.accept(new Change(marks, lead));
        return () -> listeners.remove(listener);
    }

    /**
     * Marks an identity and makes it the lead - what marking one
     * more object means: the newest question is the one being asked.
     */
    public void mark(String identity) {
        requireIdentity(identity);
        Change base = intended();
        if (base.marks().contains(identity)
                && identity.equals(base.lead())) {
            return;                // nothing a consumer could observe
        }
        List<String> next = new ArrayList<>(base.marks());
        if (!next.contains(identity)) {
            next.add(identity);
        }
        queue(new Change(next, identity));
    }

    /** Unmarks an identity, doing nothing if it was not marked. */
    public void unmark(String identity) {
        requireIdentity(identity);
        Change base = intended();
        if (!base.marks().contains(identity)) {
            return;
        }
        List<String> next = new ArrayList<>(base.marks());
        next.remove(identity);
        queue(new Change(next, identity.equals(base.lead())
                ? lastOf(next) : base.lead()));
    }

    /** Makes an already-marked identity the lead. */
    public void lead(String identity) {
        requireIdentity(identity);
        Change base = intended();
        if (!base.marks().contains(identity)) {
            throw new IllegalArgumentException(
                    "only a marked identity can lead: " + identity);
        }
        if (identity.equals(base.lead())) {
            return;
        }
        queue(new Change(base.marks(), identity));
    }

    /** Nothing is marked. */
    public void clear() {
        if (intended().isEmpty()) {
            return;
        }
        queue(new Change(List.of(), null));
    }

    /**
     * Drops every mark the new page does not hold, as
     * <strong>one</strong> change.
     *
     * <p>A view change must not publish a cloud of transient states
     * in which some marks have gone and others have not, and a
     * consumer must never see a lead that has already left the page.
     * Searching elsewhere does not carry a cloud of crosses to
     * another region.
     */
    public void pruneTo(PageContents page) {
        if (page == null) {
            throw new IllegalArgumentException("a page to prune to");
        }
        Change base = intended();
        if (base.isEmpty()) {
            return;
        }
        List<String> survivors = new ArrayList<>();
        for (String identity : base.marks()) {
            if (page.holds(identity)) {
                survivors.add(identity);
            }
        }
        if (survivors.size() == base.marks().size()) {
            return;                // nothing left the page
        }
        queue(new Change(survivors,
                survivors.contains(base.lead()) ? base.lead()
                        : lastOf(survivors)));
    }

    // ----------------------------------------------------------------

    /**
     * The state a new change is built on: the last one queued, or
     * the delivered one when nothing is waiting.
     *
     * <p>Not the delivered state while a queue exists. Two reentrant
     * changes built on the same base would each describe a whole
     * state, and the second would silently undo the first - a mark
     * made during a delivery would vanish when its event arrived.
     * Every transition is built on the one before it, so the queue
     * is a history rather than a set of competing guesses.
     */
    private Change intended() {
        return pending.isEmpty() ? new Change(marks, lead)
                : pending.peekLast();
    }

    /**
     * Queues a whole transition and drains the queue, applying each
     * state immediately before delivering its own event.
     */
    private void queue(Change change) {
        pending.addLast(change);
        if (delivering) {
            return;
        }
        delivering = true;
        try {
            Change next;
            while ((next = pending.pollFirst()) != null) {
                this.marks = next.marks();
                this.lead = next.lead();
                // A copy, so a listener that unsubscribes while being
                // told cannot disturb the notification in progress.
                for (Consumer<Change> listener : List.copyOf(listeners)) {
                    listener.accept(next);
                }
            }
        } finally {
            delivering = false;
            pending.clear();
        }
    }

    private static String lastOf(List<String> identities) {
        return identities.isEmpty() ? null
                : identities.get(identities.size() - 1);
    }

    private static void requireIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            throw new IllegalArgumentException(
                    "a mark is a catalogue identity");
        }
    }
}
