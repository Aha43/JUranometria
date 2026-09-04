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
