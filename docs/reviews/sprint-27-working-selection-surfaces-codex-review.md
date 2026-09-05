# Codex review: Sprint 27 working-selection surfaces

Reviewed PR #268 at `e08d705` against issue #261 and the approved #258 gate.
The four surfaces now address one session model, navigation no longer prunes
membership, the chart's ambiguous chooser uses the decided captured toggle,
and the ordinary ring/cross split is preserved. Two interaction findings
remain.

## [P1] The Inspector calls stars in the query margin “on this page”

`InspectorPanel.rebuildWorkingSet()` falls back from `PageContents.find()` to
`sceneHoldsStar()` for a star the inventory did not find. A scene deliberately
contains a query margin outside the paper, so presence in `scene.stars()` is
not evidence that the star is on this page. `PageContents` already contains an
entry for every on-paper star, named or unnamed; only `namedStars()` filters
what the table prints. The fallback is therefore unnecessary and changes an
accurate off-page answer into an inaccurate on-page one.

The new `aStarTheCatalogueDoesNotNameIsNotCalledOffThePage` test actually
chooses the first scene star for which `inventory.find(star.id())` is empty —
that is, a star outside the paper — and then requires the Inspector not to say
“off this page.” Its premise and assertion encode the defect. This becomes
reader-visible after an unnamed star is selected on one page and navigation
moves it into the next scene's query margin.

Use the inventory's `find`/`holds` result as the single page boundary for every
member, as the module architecture requires. Replace the current test with two
premise-asserted cases: an unnamed star whose `PageEntry` is on the paper must
not be called off-page, while an unnamed star present only in the scene's query
margin must be. Also pin the related ink promise: lowering the magnitude limit
under an on-paper unnamed selected star gives it the working cross, because
the inventory does carry that entry. The PR description's stated no-ink
boundary rests on the same mistaken “unnamed means absent from inventory”
assumption and should be corrected rather than accepted as a residual risk.

## [P1] Sorting does not end the captured range transaction

The gate states that a range transaction ends when a non-range gesture
arrives. `OnThisPageTable` clears `rangeSnapshotMembers` for a non-shift row
gesture, an external model transition, and a page replacement, but not when
the reader sorts through the column header. Sorting changes the view order and
is a distinct, non-range gesture. The next Shift gesture nevertheless reuses
the membership snapshot captured before the sort and treats a range in the
new order as a continuation of the old one. It can consequently remove rows
added by the completed pre-sort range, even though an additive gesture must
preserve every pre-existing member.

End the range transaction when the table's sort keys change, without editing
membership. Add a real-header regression: build an additive range, sort via
the header, then begin a Shift range in the sorted view. The post-sort gesture
must snapshot the membership that existed after the first range, retain all
of it, and add its current range once. Include a mutation that leaves the old
snapshot alive; it should fail on the member the stale recomputation drops.

## Approval

Approved at `84a77df`. The inventory is now the Inspector's sole page
boundary, with separate premises and answers for an unnamed star on the paper
and one held only in the scene's query margin. The additional ink regression
confirms that an on-paper unnamed member remains in the inventory when the
magnitude limit hides it and receives exactly its working cross, so the
incorrect residual limitation is removed rather than documented.

Sorting now ends the range transaction through the row sorter's own change
notification and edits no membership. The header-driven regression completes
an additive range, sorts, and proves that the next Shift range snapshots the
post-sort working set, retaining its members and joining order. Both original
findings are closed; no findings remain for #261.

## Post-approval review: [P1] The display table tests still click an unreachable component

The shared modifier fallback at `9380294` correctly keeps headless chart
gestures from asking a headless toolkit for a platform menu modifier. Moving
`WorkingSelectionTableGestureTest` to the display corpus is also justified by
Swing's own table UI asking that question. But the fixture still deliberately
puts the table in no window. Its new assertion checks only that the point is
inside `table.getVisibleRect()`; a sized component that is not showing has a
local visible rectangle too. Every dispatched press therefore remains a click
no reader could make, now wearing a display assumption and a reachability pin.
This is the precise premise gap #243's shared `ReaderInput.click` route was
created to close.

Show the fixture in a real window, wait for layout, and route its pointer
gestures — rows and column header — through `ReaderInput.click`, which proves
showing, size, and the actual point inside the visible rectangle before
dispatch. Dispose the window in the fixture's existing close path. Keep the
headless assumption because the table UI genuinely requires a toolkit, and
require the full display run to execute the class with zero aborts. A mutation
that omits showing the window must fail on the shared premise, so the new
display classification cannot become ceremonial.

## Final approval

Approved at `afe2697`. The shared pointer route now accepts an explicit point
and modifiers while proving the control is showing, sized, and that this exact
point lies inside its visible rectangle before dispatch. The gesture fixture
places the production table in a real shown window, sends every row and header
click through that route, and disposes the window in its close path. Removing
the window's `setVisible` fails on the shared showing premise, so the display
classification and reachability evidence are load-bearing. The headless
toolkit limitation remains stated honestly, while the display run executes all
nine gesture tests. No findings remain for #261.
