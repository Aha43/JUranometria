# Sprint 21 Codex review

**Reviewed:** PR #193 at `e02db41720a24991001c37a230a193470a25b3ad`  
**Scope:** Sprint 21, issues #184–#186  
**Verdict:** changes requested — **resolved and approved** at
`741c2d6` (see the follow-up at the end)

Sprint 21's product design is strong. The gate derived five reader-facing families from the production symbols and exact catalogue counts; the implementation keeps filtering at one renderer composition seam; source types remain intact for the Inspector; old stores migrate to the chart they already had; the tabbed dialog teaches the same marks the chart draws; and the target exemption preserves the standing rule that a named, symbol-capable target remains visible. The gate and implementation reviews also converted several assumptions into durable evidence: production exemplar geometry, screen-relative sizing, as-rendered contrast, exact Swing-state restoration, and accessible tab-strip controls.

The closing journey uses substantially more of the real UI than its predecessors: View menu, tab traversal, checkboxes, search field, pointer selection, Inspector, zoom toolbar, Restore Defaults, and Home. Its family-filter and target-exemption mutations are useful and the four-platform packaged matrix is green. The released M31 page remains byte-identical.

Three acceptance claims still need stronger production-path evidence.

## Findings

### P1 — The label leg can pass without proving the label contract

At `DeepSkyFamilyJourneyTest` lines 298–323, switching labels off asserts only that the resulting list has at most one element. It never proves that ordinary deep-sky labels existed immediately before the action, that at least one ordinary label disappeared, or that the searched target's label remained. An empty label list satisfies the whole first half. After labels are switched back on and Nebulae is hidden, the loop asserting `labelled ⊆ drawn` also passes when `labelledIds()` is empty.

This is exactly the vacuity rule the issue asks the closing journey to avoid. Establish the preconditions from the renderer's production label decisions: require a symbol-capable searched target and at least one ordinary labelled object on the page. Drive the real labels checkbox off, then require the target label to remain and every ordinary label to disappear. Turn labels back on and require an ordinary label to return. Hide its family and require that specific ordinary label and symbol to disappear while the target guarantee remains. Mutation-check removal of both ordinary filtering and the target-label exemption.

### P1 — Packaged symbol-less search does not drive the search result into navigation

The packaged addition calls `Atlas.search().search(symbolless.id())`, but then ignores the returned `SearchResult` and manually creates a `ChartViewState` and `ChartScene` from the already-held object's position and identity. Consequently the evidence would still pass if selecting a packaged search result failed to recenter, used the wrong title, or lost identity between search and navigation. The diagnostic says the object was “found, centred and titled,” but only “found” is exercised through search.

Use the production result-to-navigation path the application uses: select the returned result through the relevant controller/search seam, then assemble from the resulting navigation state. Assert both RA and declination, stable target identity, and the honest region title before proving the renderer invents no mark. If packaged acceptance intentionally remains headless, it may invoke the same non-Swing production method used by `SearchField`; it must not reconstruct the expected state independently.

### P2 — The journey's “restart” is another direct store read

At lines 367–384, the journey confirms the dialog and then calls `ChartOptionsStore.forNode(store).load()` directly. That proves the bytes round-trip—a contract already covered by store tests—but not the issue's requested restarted-session behavior. No new `ChartOptionsController`, chart binding, or dialog is created, so a constructor that ignored the store could regress while this journey still reported that restart worked.

Create a fresh `ChartOptionsController` over a fresh store instance for the same node, bind it to a new or deliberately replaced chart-options consumer, and assert its initial rendered/visible family state through that consumer. The old controller must not supply the answer. The test need not relaunch a JVM, but it must cross the production session boundary it claims.

## Verification reviewed

- All 12 CI checks are green, including four native images, cross-architecture smoke, and three portable-distribution verification jobs.
- 485 tests are reported on a display; display-dependent journeys abort visibly in headless runs.
- The M31 reference is byte-identical.
- The deep-sky study reproduces byte-for-byte.
- Gate and implementation review findings through PRs #187 and #188 are closed.

## Release recommendation

The proposed minor release is appropriate once these closing-evidence gaps are fixed. Sprint 21 adds reader-visible controls and a documented palette adjustment without changing a 1.0 promise or the released default page. Do not merge PR #193, close milestone 21, or tag the release before the follow-up review.

## Follow-up — approved

**Reviewed:** PR #193 at `741c2d6`  
**Verdict:** approved

All three closing findings are properly resolved, all 12 checks are
green, 485 tests pass, and the reference remains byte-identical. PR
#193 may merge, milestone 21 may close, and a minor release is
appropriate.

What each finding cost, for the record:

- **The label leg** now establishes its premises before acting — a
  symbol-capable target and ordinary names actually on the page —
  then requires every ordinary name to go and the target's to stay,
  requires them all back, and hides the target's own family so one
  action proves both halves. A label pass ignoring the family gate,
  and a dropped target-label guarantee, each fail it.
- **The packaged search** drives its returned `SearchResult` through
  the production policy, which moved out of `SearchField` into
  `SearchNavigation` so a headless run can take the same path a
  reader's Enter takes. Losing the target identity, or recentring on
  the wrong place, each fail the packaged run.
- **The restart** crosses a real session boundary: a fresh
  controller over a fresh store instance, feeding a fresh chart
  component, with the answer asked of that component's own page.
  Following that thread also showed the journey had been measuring
  the controller's options rather than the chart's, so a consumer
  that dropped the family flags would have gone unnoticed; it is
  caught now.
