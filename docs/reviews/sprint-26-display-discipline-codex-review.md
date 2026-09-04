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
