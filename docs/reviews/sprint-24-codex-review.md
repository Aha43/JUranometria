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

## Follow-up — `bc462a6`

**The packaged-rendering P1 is resolved.** The image acceptance now attaches
the real module through `ChartModuleHost`, collects the module-owned geometry,
paints the real `ChartComponent`, measures new pixels near the production
projection, clears back to byte equality, and detaches. Removing the module or
its interaction painter can no longer be replaced by a locally predicted list
of IDs.

The display journey now reaches the table's selection listener with mouse
events and correctly limits its candidates to rows the table actually lists.
One P1 control-path gap remains, plus one smaller restart claim.

### P1 — Dispatched row clicks are not proved reachable by a pointer

`clickRow` obtains `table.getCellRect(viewRow, 0, true)` and dispatches an event
at that coordinate, but it neither scrolls the cell into the viewport nor
asserts that the cell intersects `table.getVisibleRect()`. A `JTable` is taller
than its viewport; Swing will accept a synthetic event at an off-screen row
coordinate even though no reader can put a pointer there. After sorting, the
three chosen undrawn rows may be anywhere in the model. This can therefore
reintroduce the same defect the correction just found: marking an object the
reader cannot actually reach, now because its row is clipped rather than
because it was never a row.

Scroll each target through the table's real scrolling path, wait for layout,
and require its cell to be wholly visible before dispatching the click. Make a
mutation that removes the scroll fail on that premise.

The other two requested control paths also remain programmatic: sorting still
uses `setSortKeys`, and changing the lead is another plain mouse click rather
than keyboard movement/extension. Click the real header and use the focused
table's real keyboard selection gesture for the lead/extension leg, asserting
the table is the focus owner first. The journey already has the necessary
display and `FocusedWindow` machinery; using it closes the difference between
an action installed in Swing and an input a reader can deliver.

### P2 — Packaged “restart begins empty” still has no restart

The packaged method clears the current model, scans preference **key names**
for the substring `mark`, then detaches. It no longer creates even the fresh
`WorkingMarksModel` the earlier version did, let alone a second host/module
session. The output nevertheless says “nothing persisted,” and the handover
describes a restart beginning empty.

End the first host, construct a fresh chart host and module against the same
application preferences, and require its working set, lead, overlay
contributions, and table selection to begin empty. Establish first that the
old session contained a mark. A test that merely assumes persistence would use
a key containing `mark` is not a session-boundary check.

After these two corrections, PR #223 may proceed to final review. Keep the
milestone and 1.5.0 held.

## Follow-up — `3b45d9a`

The real header and keyboard-extension paths are now present, and the journey
scrolls before its pointer selections. The packaged run also constructs a
second host and module. Two vacuity gaps remain in the new premises.

### P1 — The visibility premise does not cover the dispatched click point

`clickRow` asserts `table.getVisibleRect().intersects(cell)`, then dispatches
at the centre of `cell`. Intersection proves only that some part of the cell is
visible. A clipped sliver can satisfy it while the centre `(x, y)` remains
outside the viewport, leaving the synthetic event deliverable where a reader's
pointer cannot reach.

After scrolling, compute the exact click point and require
`visibleRect.contains(x, y)` before dispatch. Prefer requiring the cell's
vertical span to be contained as well, since a row fits in the viewport; do
not require a wide first column to fit horizontally if the chosen click point
itself is reachable. Mutation-check the distinction with a viewport/cell
arrangement that intersects but does not contain the click point.

### P2 — The second session compares the preference store to itself

The packaged acceptance assigns `kept = ChartOptionsStore.user().load()`,
constructs a fresh `ChartComponent`, but never applies the loaded options to
that component or otherwise gives the store to the second session. It then
calls `ChartOptionsStore.user().load()` again and compares that result with
`kept`. Barring an external concurrent write, this is true regardless of
whether restart wiring reads or applies any option.

Establish a known non-default option through the production store/controller
path in the first session. In the second session, construct/load options as
the application does, apply them to the restarted chart, and require both the
controller/chart option and the inventory's resulting visibility to reflect
the persisted choice. Restore the user's original value in cleanup. A mutation
that leaves the restarted chart at `ChartOptions.DEFAULTS` must fail while its
working marks, lead, overlays, and table selection still begin empty.

PR #223, milestone 24, and 1.5.0 remain held for these contained corrections.

## Follow-up — `3c5f5c3`

**The pointer premise is resolved.** The journey now checks the exact point it
will dispatch against the table's visible rectangle. The second packaged
session also loads and applies a persisted family choice, begins with empty
working state, and proves the inventory reports the option's effect without
changing page membership.

One P1 release-safety defect remains.

### P1 — A failing packaged acceptance leaves the reader's real settings changed

`onThisPageJourney` saves Galaxies-off to `ChartOptionsStore.user()`, performs
all second-session assertions, detaches, and only then restores `before`.
Restoration is ordinary success-path code. If any `require`, render, catalogue
operation, or detach throws after the save, the packaged verifier exits while
leaving the actual `juranometria` preference node changed. This acceptance is
also run manually against downloaded releases, so the affected store is not
only an isolated CI account.

Wrap the entire mutation interval in `try/finally`: capture the original
options before writing, establish and assert a genuinely distinctive stored
choice, build/use/detach the second session inside the guarded body, and
restore plus flush in `finally`. Keep module detachment in cleanup as well, so
an assertion cannot leak either resource. Add a failure-path test or injectable
probe that throws after the changed option is applied and requires the original
store to be restored. A success-only final assertion is not sufficient for the
case cleanup exists to handle.

After this correction, PR #223 may proceed to final approval. Keep milestone
24 and 1.5.0 held until then.

## Follow-up — `596ce87`

The failure-path test correctly proves that an exception thrown by the body is
preserved and that restoration still runs. Both module hosts now detach from
their guarded bodies. The hazardous preference boundary remains outside the
guard, however.

### P1 — The temporary write can fail before the restoration `finally` exists

`withTemporaryOptions` executes `store.save(temporary)` and the first
`flush()` before entering its `try`. A preferences backend can write some keys
and then throw, or accept the writes and fail while flushing them. In either
case control never enters the `try`, the restoration `finally` is never
installed, and the reader is left with the partial or complete temporary
choice. This is the same release-safety defect one step earlier than the body
failure now tested.

Enter `try` before the first mutating operation, then save, flush, and run the
body inside it; restore and flush from `finally`. Add an injected store/flush
failure that occurs after mutation and prove the original options are restored
while the original failure remains observable (with any restoration failure
handled deliberately rather than silently replacing it).

The helper also accepts an arbitrary `ChartOptionsStore` but flushes the
hard-coded production node. Its tests use
`juranometria-acceptance-restore-test`, so they never flush the node they
modify. Put flushing behind the same injected persistence boundary (or pass
the matching flush operation explicitly) and make the fresh-store test read
after that actual node has been flushed.

Once the mutation itself is inside the protected interval and the correct
store is flushed, the sprint may receive final approval. PR #223, milestone 24,
and 1.5.0 remain held.

### P2 — The new mode control is clipped in the running application

Dogfooding on 3 September found the Inspector's new **On this page** toggle
rendered as **“On this pa”** on an ordinary application start. It has also been
seen unclipped, so the surface currently depends on the window/font/layout
state it happens to receive. This is the control that names and opens Sprint
24's feature; shipping it truncated would contradict the gate's decision that
the words carrying meaning remain whole.

Reproduce the actual Inspector composition at its 240 px floor, preferred
width, both themes, the supported platform font stacks, and enlarged text.
Measure the toggle's rendered text and insets against its allocated bounds,
not merely `isVisible()` or a positive width. Choose a responsive arrangement
that keeps **Selected** and **On this page** whole—allowing the chooser to stack
or otherwise reflow if they genuinely cannot fit—without reducing the chart's
400 px floor or hiding either mode. Add a regression that fails on the observed
“On this pa” allocation and visually inspect the real panel after correction.

Treat this as part of #217 and the 1.5.0 release gate, since it was found on
the feature being handed over rather than after release.

## Follow-up — `1926bc6`

**The preference-safety P1 is resolved.** The guarded interval now begins
before the first write, the store being mutated owns its flush operation, and
failure-path tests cover a write refusal, a flush failure after mutation, and
a failed restoration without replacing the acceptance failure it accompanies.
The same seam also removes the older equatorial-grid acceptance hazard.

The clipped **On this page** control reported during dogfooding remains open;
this commit does not change its layout. PR #223 and 1.5.0 remain held only for
that visible surface correction and its cross-font/width evidence.

## Follow-up — `6674063`

**The visible clipping defect is resolved.** The chooser now uses the full
Inspector width, gives both names equal room while they fit, and stacks them
when they do not. The regression measures the text in each button's actual
font across 240/320/420 px and 11–24 pt, and separately pins the alignment and
responsive shape that caused the observed “O”.

One P2 test-isolation correction remains.

### P2 — The new font sweep bypasses the shared Swing-state guard

`InspectorModeChooserTest` captures `UIManager.getFont("defaultFont")`, writes
several overrides, then restores by putting that resolved font back. Sprint
21 established `SwingSession.restoring` precisely because `UIManager.get`
cannot distinguish an explicit override from the active look-and-feel's own
font. Putting the resolved LAF value back can invent an override that did not
exist and pin it across the next theme, making unrelated display tests depend
on execution order.

Run both font-mutating tests inside `SwingSession.restoring`, as the existing
dialog, accessibility, toolbar, and contrast tests do. Keep the three
failure-mode proofs in `SwingSessionTest` load-bearing; do not recreate a
smaller local restoration scheme. Confirm the chooser test leaves both the
inherited look and feel and the presence/absence of a font override exactly as
it found them, including when an assertion throws.

After that contained test cleanup, all Sprint 24 findings are resolved and PR
#223 may merge for the recommended 1.5.0 release.

## Final follow-up — `7673508`

**Approved.** Both Sprint 24 font/theme sweeps now run through the shared
`SwingSession.restoring` guard. They preserve the inherited look and feel and
the presence or absence of an explicit font override, including exceptional
exit, rather than installing a resolved look-and-feel font as a choice nobody
made. The table theme sweep also no longer assumes the suite began in light
mode.

Issue #224 correctly records the nine older unguarded tests as follow-up work;
they predate this sprint and need not widen the release PR.

All gate, foundation, module, interaction, packaged-acceptance, preference
safety, responsive-layout, and test-isolation findings are resolved. PR #223
may merge, milestone 24 may close, and the reviewed work may release as 1.5.0.
