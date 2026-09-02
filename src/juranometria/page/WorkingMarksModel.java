package juranometria.page;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    /** Changes awaiting delivery, so every consumer sees one order. */
    private final Deque<Change> pending = new ArrayDeque<>();
    private boolean delivering;

    private final Set<String> marks = new LinkedHashSet<>();
    private String lead;

    /** The marked identities, in the order they were marked. */
    public List<String> marks() {
        return List.copyOf(marks);
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
     * never has to ask separately what it missed.
     */
    public Runnable onChange(Consumer<Change> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
        listener.accept(state());
        return () -> listeners.remove(listener);
    }

    /**
     * Marks an identity and makes it the lead - what marking one
     * more object means: the newest question is the one being asked.
     */
    public void mark(String identity) {
        requireIdentity(identity);
        if (marks.contains(identity) && identity.equals(lead)) {
            return;                // nothing a consumer could observe
        }
        marks.add(identity);
        lead = identity;
        publish();
    }

    /** Unmarks an identity, doing nothing if it was not marked. */
    public void unmark(String identity) {
        requireIdentity(identity);
        if (!marks.remove(identity)) {
            return;
        }
        if (identity.equals(lead)) {
            lead = lastOf(marks);
        }
        publish();
    }

    /** Makes an already-marked identity the lead. */
    public void lead(String identity) {
        requireIdentity(identity);
        if (!marks.contains(identity)) {
            throw new IllegalArgumentException(
                    "only a marked identity can lead: " + identity);
        }
        if (identity.equals(lead)) {
            return;
        }
        lead = identity;
        publish();
    }

    /** Nothing is marked. */
    public void clear() {
        if (marks.isEmpty()) {
            return;
        }
        marks.clear();
        lead = null;
        publish();
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
        if (marks.isEmpty()) {
            return;
        }
        List<String> survivors = new ArrayList<>();
        for (String identity : marks) {
            if (page.holds(identity)) {
                survivors.add(identity);
            }
        }
        if (survivors.size() == marks.size()) {
            return;                // nothing left the page
        }
        marks.clear();
        marks.addAll(survivors);
        if (lead != null && !marks.contains(lead)) {
            lead = lastOf(marks);
        }
        publish();
    }

    // ----------------------------------------------------------------

    private Change state() {
        return new Change(List.copyOf(marks), lead);
    }

    /**
     * Queues this state and drains the queue.
     *
     * <p>The state is captured now rather than at delivery, so a
     * listener that changes the marks while being told about them
     * cannot rewrite the change already in flight: consumers see
     * every state, in the order it happened.
     */
    private void publish() {
        pending.addLast(state());
        if (delivering) {
            return;
        }
        delivering = true;
        try {
            while (!pending.isEmpty()) {
                Change change = pending.removeFirst();
                for (Consumer<Change> listener : List.copyOf(listeners)) {
                    listener.accept(change);
                }
            }
        } finally {
            delivering = false;
            pending.clear();
        }
    }

    private static String lastOf(Set<String> identities) {
        String last = null;
        for (String identity : identities) {
            last = identity;
        }
        return last;
    }

    private static void requireIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            throw new IllegalArgumentException(
                    "a mark is a catalogue identity");
        }
    }
}
