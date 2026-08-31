package juranometria.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The one place the current selection lives (Sprint 19, issue #169).
 *
 * <p>UI-independent by construction: no Swing, no chart component, no
 * knowledge of the inspector. Consumers subscribe and are told what
 * the reader is now asking about. The inspector is the first such
 * consumer, not the owner of this state, and the sprint's journey
 * subscribes a second one to prove that the seam is real.
 *
 * <p>That is the whole of the "future module" provision the gate
 * approved: <strong>a listener list</strong>. No lifecycle, no
 * discovery, no plugin API. If a second consumer ever needs more,
 * that is the sprint which finds out.
 *
 * <p>Ambiguity lives here too. When a click reaches several marks,
 * the model holds every candidate in the reviewed order with one of
 * them current, so a consumer can offer the choice rather than
 * pretending the atlas resolved it.
 */
public final class SelectionModel {

    /** What consumers are told: never a partial state. */
    public record Change(Selection selection, List<Selection.Object> candidates,
                         int currentIndex) {

        public Change {
            candidates = List.copyOf(candidates);
            // A state nobody can misread (review): the index either
            // points at the current selection or says there is no
            // candidate list at all.
            if (candidates.isEmpty()) {
                if (currentIndex != -1) {
                    throw new IllegalArgumentException(
                            "no candidates, so no current index: "
                                    + currentIndex);
                }
            } else {
                if (currentIndex < 0 || currentIndex >= candidates.size()) {
                    throw new IllegalArgumentException(
                            "current index " + currentIndex + " is outside "
                                    + candidates.size() + " candidates");
                }
                if (!candidates.get(currentIndex).equals(selection)) {
                    throw new IllegalArgumentException(
                            "the current candidate must be the selection: "
                                    + selection + " vs "
                                    + candidates.get(currentIndex));
                }
            }
        }

        /** Whether the reader is being offered a choice. */
        public boolean isAmbiguous() {
            return candidates.size() > 1;
        }
    }

    private final List<Consumer<Change>> listeners = new ArrayList<>();
    /** Changes awaiting delivery, so every consumer sees one order. */
    private final java.util.Deque<Change> pending = new java.util.ArrayDeque<>();
    private boolean delivering;
    private Selection selection = Selection.NOTHING;
    private List<Selection.Object> candidates = List.of();
    private int currentIndex = -1;

    public Selection selection() {
        return selection;
    }

    /** Every eligible candidate for the last click, in reviewed order. */
    public List<Selection.Object> candidates() {
        return candidates;
    }

    /** Which candidate is current, or -1 when there is none. */
    public int currentIndex() {
        return currentIndex;
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
        listener.accept(change());
        return () -> listeners.remove(listener);
    }

    /** Nothing is selected. */
    public void clear() {
        set(Selection.NOTHING, List.of(), -1);
    }

    /** A point of sky with nothing catalogued within reach. */
    public void selectEmptySky(SkyPosition position) {
        set(new Selection.EmptySky(position), List.of(), -1);
    }

    /**
     * One object, unambiguously - a single-candidate click, or a
     * search result the reader chose by name.
     */
    public void select(Selection.Object object) {
        set(object, List.of(object), 0);
    }

    /**
     * Several candidates in the reviewed order, the first current.
     * The reader is being offered a choice; the atlas has not made
     * one for them.
     */
    public void selectAmong(List<Selection.Object> ordered) {
        if (ordered == null || ordered.isEmpty()) {
            throw new IllegalArgumentException(
                    "use clear() or selectEmptySky() when nothing was hit");
        }
        set(ordered.get(0), ordered, 0);
    }

    /**
     * Moves to another of the candidates the last click produced -
     * what the inspector's list does. It changes the answer, never
     * the page.
     */
    public void chooseCandidate(int index) {
        if (index < 0 || index >= candidates.size()) {
            throw new IndexOutOfBoundsException(
                    "no such candidate: " + index + " of "
                            + candidates.size());
        }
        set(candidates.get(index), candidates, index);
    }

    private void set(Selection next, List<Selection.Object> nextCandidates,
                     int nextIndex) {
        this.selection = next;
        this.candidates = List.copyOf(nextCandidates);
        this.currentIndex = nextIndex;
        pending.add(change());
        if (delivering) {
            // A consumer changed the selection while being told about
            // one. Queue it: the change in flight finishes reaching
            // everybody first, and this one follows (review, P1).
            //
            // Delivering it immediately would nest, and the outer
            // loop would then carry on handing the OLDER change to
            // the consumers it had not reached yet - which was
            // reproducible: a model resting at B while an observer's
            // last word was A, leaving it permanently stale.
            return;
        }
        delivering = true;
        try {
            Change change;
            while ((change = pending.poll()) != null) {
                // A copy, so a listener that unsubscribes while being
                // told cannot disturb the notification in progress.
                for (Consumer<Change> listener : List.copyOf(listeners)) {
                    listener.accept(change);
                }
            }
        } finally {
            delivering = false;
            pending.clear();
        }
    }

    private Change change() {
        return new Change(selection, candidates, currentIndex);
    }
}
