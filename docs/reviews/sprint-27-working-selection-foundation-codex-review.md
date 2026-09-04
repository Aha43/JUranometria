# Codex review: Sprint 27 working-selection foundation

Reviewed PR #267 at `270393c` against issue #260 and the approved #258 gate.
`WorkingSelection` retains the established whole-state queue: nested changes
build on the intended state, each event agrees with the fields visible during
its delivery, and bulk replacement publishes once. `WorkingMarksModel` holds
no fields that can become a second membership truth, and the new service and
mode remain free of surface, storage, network and planner dependencies.

## [P1] A page change still prunes the session-level model

The issue explicitly requires “No page change prunes the set,” but the running
foundation does exactly that. `OnThisPageModule.pageChanged()` still calls
`services.workingMarks().pruneTo(page)`, and the compatibility adapter
implements that operation by calling `model.replaceWith(survivors, ...)` on
the new `WorkingSelection`. Because both service names deliberately address
one state, navigating away removes members from `workingSelection()` itself.
The reflection test proves only that the new class does not spell a pruning
method; it does not prove that a page cannot mutate the state through the
adapter. There is correspondingly no committed page-change test satisfying
the issue's evidence item.

Close the behavioral route in this foundation and add a production-wiring
regression: select members, drive a real page change through the host/module,
and require the same `WorkingSelection.Change` afterward with no publication.
Do not preserve old page-bound behavior by mutating the new truth under an old
method name. If keeping the current surface fully coherent makes that unsafe
as a separate increment, merge the necessary migration slice from #261 here
or revise the PR boundary explicitly; the approved model cannot temporarily
mean the opposite of its defining cross-page invariant.

## [P1] Bulk replacement accepts identities every other route rejects

`add`, `remove`, `toggle`, and `lead` call `requireIdentity`, but the public
`Change` constructor and `replaceWith` validate only uniqueness and the
lead/member relationship. `replaceWith(List.of(" "), " ")` therefore creates
a non-empty working selection whose sole “catalogue identity” is blank. That
state can also be constructed directly as a public `Change`, even though no
catalogue or surface can resolve it.

Validate every member identity in the canonical `Change` constructor so all
construction and bulk/captured-transaction paths share the invariant. Extend
the existing `duplicatesAndBlankIdentitiesAreImpossible` test to exercise a
blank member through both `replaceWith` and direct `Change` construction; its
current blank assertion covers only `add` and leaves the actual hole open.

## Re-review: [P1] Serialize model and scope changes before notifying the view

The two original findings are closed at `f885e5c`: navigation now changes only
the adapter's presentation scope, and canonical `Change` construction rejects
blank members through both bulk routes. The scoped view, however, breaks the
delivery discipline it is preserving when more than one consumer listens.

`onChange` creates a separate `model.onChange` subscription for every view
listener. If the first listener calls `pruneTo` while a model change is being
delivered, `pruneTo` immediately broadcasts the scoped result to **all** view
listeners. Control then returns to the model's original listener loop, whose
second adapter subscription computes that same model event under the new
scope. The first consumer hears `[before scope, after scope]`; the second hears
`[after scope, after scope]`. It misses the state the first consumer saw and
receives a duplicate, contrary to the standing guarantee that every consumer
hears one order of whole states. The existing nested-prune test has only one
listener, so it cannot expose this.

Subscribe the adapter to the model once and serialize both model-derived view
changes and scope changes through one view-level delivery queue before walking
a copied listener list. Suppress consecutive identical views as ordinary
no-ops. Add two listeners, make the first change scope during delivery, and
require both to hear the same exact sequence once each while `marks()` and
`lead()` agree with the event in flight. Also exercise a nested model write
from a scope notification, so neither direction can interleave the other.

## Second re-review: [P1] Suppress model changes invisible through the scope

The single subscription and serialized queue at `0bdbdfb` close the
interleaving defect in both directions. However, only `pruneTo` compares its
projected result with the last intended view. The model subscription calls
`queue(viewOf(change))` unconditionally. When the view is scoped to a page
holding M31, adding or removing an off-page M42 therefore publishes the same
`[M31]` view again even though no mark or lead visible through this adapter
changed. That contradicts both the inherited “nothing a consumer could
observe” rule and the previous review's explicit requirement to suppress
consecutive identical views.

Put duplicate suppression at the common queue boundary, comparing against the
last pending state or the delivered state, so it covers model and scope
origins alike. Add a scoped two-listener regression that adds, changes the lead
to, and removes off-page members while the projected marks and lead remain
unchanged; neither listener should hear an event. Include a control where the
underlying transition changes the visible fallback lead, which must still
publish, so the suppression cannot hide an observable view change.

## Approval

Approved at `2b232f5`. Duplicate suppression now sits at the shared queue
boundary and compares with the last intended view, whether that is the state
already delivered or the tail waiting to be delivered. Model and scope changes
therefore share one no-op rule without weakening serialized, reentrant
delivery. The regression exercises off-page membership and lead changes that
must stay silent, then proves that a visible arrival and fallback-lead change
still publish. The original page-scope, identity, and multi-listener findings
remain closed. No findings remain for #260.
