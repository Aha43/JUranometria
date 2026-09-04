package juranometria.chart;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * The reader's working selection: one ordered set of catalogue
 * identities with one lead, session-level and session-only
 * (Sprint 27, issue #260, semantics decided by the #258 gate in
 * docs/decisions/working-selection.md).
 *
 * <p>Grown from {@code WorkingMarksModel}'s reviewed semantics and
 * promoted out of page scope: <strong>no page-pruning operation
 * exists here</strong> — navigation never mutates the set — and no
 * persistence route exists or may be added; the set begins empty
 * every session. It is a working set for an evening, not an
 * observing list, planner, note layer, or saved collection.
 *
 * <p>No Swing, no AWT, no renderer, no preferences, no files, no
 * network — held by a bytecode boundary test, not a comment.
 *
 * <p>Delivery is the project's standing discipline: consumers are
 * told a whole state, never a partial one; the fields visible
 * during a notification are the state being delivered; a nested
 * transition builds on the last queued state and is delivered in
 * turn, in one order for every consumer; and one logical gesture
 * publishes once.
 */
public final class WorkingSelection {

    /**
     * What consumers are told. Always the whole state: the members
     * in first-membership order and which of them leads.
     */
    public record Change(List<String> members, String lead) {

        public Change {
            members = List.copyOf(members);
            // An ordered set, so it says so: a duplicate would give
            // a table two rows for one object and let a reader
            // remove something that stayed a member.
            if (new java.util.HashSet<>(members).size()
                    != members.size()) {
                throw new IllegalArgumentException(
                        "a working selection holds each identity"
                                + " once: " + members);
            }
            if (lead != null && !members.contains(lead)) {
                throw new IllegalArgumentException(
                        "the lead is always a member: " + lead
                                + " is not among " + members);
            }
            if (lead == null && !members.isEmpty()) {
                throw new IllegalArgumentException(
                        "members without a lead: " + members);
            }
        }

        /** Nothing is selected. */
        public boolean isEmpty() {
            return members.isEmpty();
        }
    }

    private final List<Consumer<Change>> listeners = new ArrayList<>();

    /**
     * Transitions awaiting delivery, each a whole state, each built
     * on the one before it — the queue is a history, never a set of
     * competing guesses (the WorkingMarksModel review's rule, kept
     * verbatim).
     */
    private final Deque<Change> pending = new ArrayDeque<>();
    private boolean delivering;

    /** The state that has been delivered. */
    private List<String> members = List.of();
    private String lead;

    /** The members, in the order they first joined. */
    public List<String> members() {
        return members;
    }

    /** The lead identity, or null when the set is empty. */
    public String lead() {
        return lead;
    }

    public boolean isMember(String identity) {
        return members.contains(identity);
    }

    /**
     * Subscribes a consumer and returns the handle that releases
     * it. The consumer is told the current state immediately —
     * during a delivery, the state being delivered, because the
     * fields are it.
     */
    public Runnable onChange(Consumer<Change> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
        listener.accept(new Change(members, lead));
        return () -> listeners.remove(listener);
    }

    /**
     * Adds an identity and makes it the lead — the newest question
     * is the one being asked. Re-adding a member does not move it
     * in the order; it only takes the lead.
     */
    public void add(String identity) {
        requireIdentity(identity);
        Change base = intended();
        if (base.members().contains(identity)
                && identity.equals(base.lead())) {
            return;                // nothing a consumer could observe
        }
        List<String> next = new ArrayList<>(base.members());
        if (!next.contains(identity)) {
            next.add(identity);
        }
        queue(new Change(next, identity));
    }

    /**
     * Removes one member, keeping the rest. Removing the lead
     * passes the lead to the <strong>last-marked remaining
     * member</strong> — the standing rule the gate restated and
     * kept. Removing a non-member does nothing.
     */
    public void remove(String identity) {
        requireIdentity(identity);
        Change base = intended();
        if (!base.members().contains(identity)) {
            return;
        }
        List<String> next = new ArrayList<>(base.members());
        next.remove(identity);
        queue(new Change(next, identity.equals(base.lead())
                ? lastOf(next) : base.lead()));
    }

    /**
     * The additive gesture's one verb (#258): absent — added, and
     * it leads; present — removed, the lead by the removal rule.
     */
    public void toggle(String identity) {
        requireIdentity(identity);
        if (intended().members().contains(identity)) {
            remove(identity);
        } else {
            add(identity);
        }
    }

    /**
     * Makes an already-selected member the lead. Changing the lead
     * never removes anything.
     */
    public void lead(String identity) {
        requireIdentity(identity);
        Change base = intended();
        if (!base.members().contains(identity)) {
            throw new IllegalArgumentException(
                    "only a member can lead: " + identity);
        }
        if (identity.equals(base.lead())) {
            return;
        }
        queue(new Change(base.members(), identity));
    }

    /**
     * Replaces the whole selection in <strong>one</strong>
     * transition — what a replacing click, a range recomputation,
     * or a captured transaction's step means: one gesture, one
     * publish, never a run of intermediate states nobody chose.
     */
    public void replaceWith(List<String> identities, String lead) {
        List<String> next = List.copyOf(identities);
        Change base = intended();
        if (base.members().equals(next)
                && java.util.Objects.equals(base.lead(), lead)) {
            return;
        }
        queue(new Change(next, lead));
    }

    /** Empties the whole set, explicitly. */
    public void clear() {
        if (intended().isEmpty()) {
            return;
        }
        queue(new Change(List.of(), null));
    }

    // ----------------------------------------------------------------

    /**
     * The state a new change is built on: the last one queued, or
     * the delivered one when nothing is waiting — so nested
     * transitions build on each other rather than silently undoing
     * the one before.
     */
    private Change intended() {
        return pending.isEmpty() ? new Change(members, lead)
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
                this.members = next.members();
                this.lead = next.lead();
                // A copy, so a listener that releases itself while
                // being told cannot disturb the notification.
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
                    "a selection member is a catalogue identity");
        }
    }
}
