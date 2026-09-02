# Codex review: Sprint 24 — Discover what is on this page

**PR #223 · issue #217 · reviewed at `9a3cf00`**

## Decision

Changes requested. The table selection guard and chart-options notification
fix two real integration defects, and the handover is unusually complete. The
two acceptance journeys still bypass the feature at the points where they
claim to prove it.

## P1 — Packaged acceptance invents “one cross” without running the module or painter

`PackagedAcceptanceMain.onThisPageJourney` builds a `PageInventory` and a
`WorkingMarksModel`, then repeats `OnThisPageModule.crosses()` locally by
filtering marked entries whose visibility is not `DRAWN`. It appends their IDs
to a `List<String>` and calls that result “exactly one cross.” No
`OnThisPageModule` is attached, no `OverlayContribution` is collected, no
`ChartComponent` is painted, and no cross pixel is observed.

This duplicated rule can pass if the packaged module contributes nothing, if
the chart ignores `InkRole.INTERACTION`, or if `WorkingCrossInk` paints at the
wrong position. The log line and handover therefore report native-image
evidence the packaged run does not contain.

Exercise the actual headless production chain inside every image: create the
component and host, attach `OnThisPageModule`, mark a drawn and an undrawn
inventory entry, collect the module-owned contribution, and paint the chart.
Require no extra ink for the drawn entry and cross ink at the undrawn entry's
production-projected position. Clear through the production model and require
the contribution and its pixels to disappear, with the unmarked page equal to
the ordinary renderer. Then detach and prove the module's contribution is
withdrawn. Mutations that empty `OnThisPageModule.crosses()` or skip
`WorkingCrossInk.paint()` must fail packaged acceptance.

Keep the non-persistence check, but do not describe a locally reconstructed ID
list as rendered ink.

## P1 — The closing journey bypasses the table for its central marking behavior

The journey says it is driven through “the table's rows [and] the keyboard,”
but its main multi-object leg calls
`marks().replaceWith(marked, invisible.get(0))` directly, and the next leg
changes the lead with `marks().lead(...)`. Sorting is installed with
`setSortKeys` rather than the real column header. Those calls begin after the
UI behavior the sprint exists to provide and therefore cannot catch a broken
selection listener, view/model conversion, Shift extension, lead propagation,
or header wiring. This is especially significant because the implementation
report says this journey found a selection-rebuild defect in exactly that
boundary.

Drive the central arc as a reader does:

- click the real magnitude header to sort;
- focus the table and use real row-selection gestures to mark several
  invisible entries plus a drawn entry;
- use real keyboard movement/extension to change the lead; and
- assert one model transition per gesture, the same marked identities after
  sorting and page rebuild, crosses only for the undrawn members, and singular
  selection following the actual lead.

It is fine for lower-level tests to call the model directly. The sprint-closing
journey cannot make that substitution while claiming the production control
path. Add a mutation that disconnects the table's selection listener and make
the journey fail on its own premise.

## Accepted direction

- `ChartComponent` now distinguishes scene changes from option changes, so the
  inventory can refresh visibility without pretending the scene was rebuilt.
- Guarding model replacement as a following operation correctly prevents
  programmatic row rebuilds from being interpreted as reader unmarking.
- Numeric column widths are derived from the active table font, and horizontal
  scrolling preserves complete state/magnitude words where four columns do
  not fit.
- The handover accurately records the module boundary, refusal contract,
  transient working state, residual layout cost, and the independent #220
  flake.

Do not merge PR #223, close milestone 24, or cut 1.5.0 until both acceptance
paths exercise the production feature they describe.
