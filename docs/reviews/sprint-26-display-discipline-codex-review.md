# Sprint 26 display-discipline review

Reviewed PR #249 at `7e6b57a`.

The migration has already paid for itself: it exposed controls that were
never visible, tabs that were never chosen, and a field that belonged to no
window. The shared pointer and text-entry routes are a useful improvement.
The remaining findings are both in the boundary the PR says it has made
structural.

## P1 — Keyboard routes can still dispatch to components that do not own focus

`ReaderInput` states that a keyboard target must own focus, but its public
`press(...)` helper dispatches key events without checking that premise
(`test/juranometria/ui/ReaderInput.java:143-154`). Two existing journeys also
remain outside the helper:

- `DeepSkyFamilyJourneyTest.pressOn(...)` calls
  `requestFocusInWindow()` and dispatches Ctrl-PageUp/Down immediately
  (`test/juranometria/ui/DeepSkyFamilyJourneyTest.java:727-735`). As #209
  established, that request may be silently refused. Direct dispatch still
  activates the binding, so the test can pass when a reader's keyboard cannot
  reach the tab strip.
- `PlaceAndTimeDialogLifecycleTest` sends Escape directly to the root pane
  (`test/juranometria/ui/placeandtime/PlaceAndTimeDialogLifecycleTest.java:188-196`).
  A real Escape starts at the actual focus owner and reaches the root-pane
  binding through Swing's keyboard dispatch. Sending the event to the root
  pane proves the binding can be invoked, not that the reader can invoke it.

The gate does not catch either survivor. It has a broken-pointer fixture but
no corresponding broken-keyboard fixture, and it counts premises per file.
`DeepSkyFamilyJourneyTest` therefore receives credit for the focus-proven
search field elsewhere in the file while its tab-strip keystrokes remain
unguarded. This is the same weakness as counting one honest assertion on
behalf of another gesture.

Make the keyboard route state its focus premise at the operation that sends
the key. One reasonable shape is for `ReaderInput.press(...)` to insist on
the containing window and the supplied component before dispatch; Escape
should be sent from the actual focus owner. Then make the structural guard
reject a direct keyboard dispatch outside the exact named mechanism tests or
the shared helper. Mutation proof should remove/refuse the focus acquisition
for each of these two journeys and fail each on its own premise under the
display job.

## P2 — Tab selection does not prove that its click point is reachable

`ReaderInput.chooseTab(...)` proves the tabbed pane is showing and that the
header has nonzero width, then dispatches at the header midpoint
(`test/juranometria/ui/ReaderInput.java:118-138`). It never checks that this
point lies in the tab strip's visible rectangle. A clipped header, including
one outside a scrolling tab viewport, can therefore be selected by a
dispatched event even though a reader cannot click it.

This contradicts the helper's own promise that every pointer target proves
the clicked point is reachable. Apply the same point-in-visible-rectangle
premise used by `click(...)`, and exercise an actually clipped tab header so
removing the premise fails for reachability rather than being carried by the
subsequent selected-index assertion.

## Verdict

Changes requested. The production application is untouched and the migrated
pointer/text routes are stronger, but the PR does not yet establish the one
keyboard discipline or the complete pointer discipline it records in the
decision.

## Follow-up at `846245c`

The tab-header reachability finding is closed. `chooseTab(...)` now tests the
actual header midpoint against the tab strip's visible rectangle before it
dispatches the click.

### P1 remains — focused-window and focused-component keys were conflated

The four raw dispatchers moved into `ReaderInput.shortcut(...)`, but that
method establishes only that the containing window is focused. That is the
right premise for the application shortcuts explicitly registered with
`WHEN_IN_FOCUSED_WINDOW`, including zoom and the dialog's Escape binding. It
does not establish the premise for `DeepSkyFamilyJourneyTest`'s tab traversal.
Ctrl-PageUp/Down is the tabbed pane's own keyboard route: its binding belongs
to the tabbed pane's focus/ancestor input map. A reader can invoke it only
when the tabbed pane is the keyboard target (or focus lies in the applicable
ancestry). Directly dispatching the event to an unfocused tabbed pane still
runs it, so the journey can remain green with broken keyboard reachability.

The new structural check also retains the masking shape it says it removes.
For a file containing raw key dispatch, any occurrence of
`ReaderInput.shortcut(...)`, `ReaderInput.typeAndEnter(...)`, or a focus helper
anywhere in that file excuses the dispatch. A future raw dispatch can therefore
borrow an unrelated honest gesture in the same file exactly as before.
At this head the actual raw-dispatcher set is just `ReaderInput.java`, so pin
that exact set instead of inferring local honesty from another substring.

Separate the two contracts in the shared vocabulary: one operation for an
application shortcut that requires a focused window, and one for a component
key that requires the supplied component to be the actual focus owner.
`DeepSkyFamilyJourneyTest` needs the latter; the zoom/interval/Escape routes
need the former. Mutation proof should make focus acquisition for the tab
strip fail while the window remains focused and require the journey to stop
on that premise. The structural test should fail if a raw `KEY_PRESSED`
dispatch is added to any file other than the one exact shared helper.

Verdict remains changes requested on this P1.
