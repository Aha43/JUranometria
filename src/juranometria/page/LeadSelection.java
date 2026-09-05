package juranometria.page;

import java.util.Optional;

import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.WorkingSelection;

/**
 * The bridge between the working selection's lead and the chart's
 * existing singular selection (Sprint 24, issue #215; retargeted to
 * the session model by issue #261).
 *
 * <p>A reader selects several objects and reads the facts of one of
 * them - the lead. The chart already has exactly one way to say
 * "this object is the one being read about": {@link SelectionModel}.
 * Teaching it about sets would put multi-selection into a model
 * every other surface depends on being singular, so the lead is
 * <em>fed</em> to it instead (the #258 decision: production wiring
 * drives the answering model from the working selection's lead).
 *
 * <p>The direction is one way, with one deliberate silence: a lead
 * the answering model is <strong>already answering</strong> is left
 * alone. A gesture that offered candidates has said more than the
 * lead's identity - the chooser is open - and re-asserting the same
 * lead would collapse a choice the reader is in the middle of
 * making. The rule is the queue-boundary no-op rule of #260 again:
 * a transition nobody could observe is not delivered. A lead the
 * model is <em>not</em> answering is fed to it, which is also what
 * ends a finished click's transaction: the candidate list collapses
 * to the new lead, so a stale chooser cannot reopen it.
 */
public final class LeadSelection {

    private LeadSelection() {
    }

    /**
     * Keeps {@code selection} answering whatever the working
     * selection leads with, for as long as the returned handle is
     * not run.
     *
     * <p>The lead's identity has to become a selectable object. The
     * page is asked first - it knows where an identity is and what
     * kind of thing it is. A lead the page no longer holds is
     * resolved from what the bridge learned when the page did hold
     * it: every member joins the set through a gesture on the page
     * that shows it, so its position was known at that moment, and a
     * position is a catalogue fact rather than a second membership
     * truth. Only an identity the bridge has never resolved selects
     * nothing rather than a guess.
     */
    public static Runnable connect(WorkingSelection working,
                                   SelectionModel selection,
                                   java.util.function.Supplier<PageContents> page) {
        if (working == null || selection == null || page == null) {
            throw new IllegalArgumentException(
                    "a bridge needs the working selection, a selection"
                            + " and a page");
        }
        // Session-only resolution memory: identity to the selectable
        // object the page reported, learned whenever a member is on
        // the page. Derived presentation data, never membership.
        java.util.Map<String, Selection.Object> known =
                new java.util.HashMap<>();
        return working.onChange(change -> {
            PageContents contents = page.get();
            for (String member : change.members()) {
                selectable(contents, member)
                        .ifPresent(object -> known.put(member, object));
            }
            String lead = change.lead();
            if (lead == null) {
                if (!(selection.selection() instanceof Selection.None)) {
                    selection.clear();
                }
                return;
            }
            if (selection.selection() instanceof Selection.Object current
                    && current.catalogueId().equals(lead)) {
                // Already the answer - and possibly a richer one,
                // with the click's candidates still offered.
                return;
            }
            Selection.Object object = selectable(contents, lead)
                    .orElseGet(() -> known.get(lead));
            if (object != null) {
                selection.select(object);
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
