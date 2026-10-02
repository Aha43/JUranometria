# Sprint 38 handover — Keep the field where I put it

The first sprint on the road to 5.0. Three issues, one merged pull request
carrying four commits, one owner ruling on the CI shape, one ruling on a
stopped gate, and a packaged owner checkpoint that returned no finding.

Written against `main` at `4c39bf4` (PR #430), after its post-merge checks.
No chart ink, no astronomy, no VERSION change, no tag, no release.

## The friction

The chart was right; using it was not easy. The owner's starting point for 5.0
was that the chart is good but cumbersome to control. The first concrete case
was accidental zoom: a careless wheel notch or two-finger trackpad scroll over
the chart threw away a field the reader had chosen on purpose.

## The lock

A toolbar toggle, **Lock zoom** / **Lås zoom**, sits right after the two zoom
buttons.

- **Locked:** a wheel or trackpad event over the chart is consumed and does
  nothing, and any banked remainder is dropped, so unlocking never releases a
  step made while locked.
- **Unaffected:** everything deliberate still changes the field — the toolbar's
  buttons, View's items and their keys, search, recentring. Panning, selection
  and the inspector are unaffected too.
- **One state:** one `ZoomLock` is what the wheel asks and what the toggle
  shows, so they cannot disagree.
- **Placement:** installed around the chart's existing wheel handler in
  `JUranometriaMain`; `ChartComponent` is unchanged.

## Every zoom route, and how it is governed

| route | where | while locked |
|---|---|---|
| mouse wheel over the chart | `ZoomInteraction` | does nothing; remainder dropped |
| trackpad two-finger scroll | the same handler: AWT delivers it as precise wheel events | does nothing |
| trackpad pinch / magnify | no route: the atlas listens to no gesture or magnification event | — |
| toolbar Zoom In / Zoom Out | `AtlasToolbar` → `ChartViewController.zoomIn/zoomOut` | zooms |
| View → Zoom In / Zoom Out and their accelerators, with the `=`, `+`, keypad `+`/`-` variants | `AppMenuBar`, `installZoomShortcuts` | zooms |
| search: a found object that does not fit is shown at the widest field that fits | `SearchNavigation` → `recenter(position, field, …)` | changes the field |
| centre on a selection; a module's recentre request | `recenter(position)` | keeps the field |
| panning | `PanInteraction` | unchanged; never changes the field |

`ZoomInteraction` is the only wheel listener the atlas installs. The scroll
panes' own wheel handling scrolls tables, not the chart. So the lock governs
the one continuous pointer route to the field.

## Proof

- **Behaviour:** `ZoomLockTest` covers wheel notches and fine trackpad
  rotations locked and unlocked, the dropped remainder, deliberate zoom while
  locked, panning while locked, the toggle and the lock as one state, and the
  preference.
- **Persistence:** the choice is a preference, `zoomLocked`, unlocked when
  never chosen or unreadable. It is restored before the first wheel event and
  saved on every change. `ZoomLockRestartJourneyTest` starts the real
  `JUranometriaMain.start` twice on a scratch node, and the packaged journey
  round-trips the preference through each bundled runtime's own backend.
- **Localization and accessibility:** the label, accessible name, hover and
  explanation are written in English and Norwegian; the toggle is focusable;
  its explanation is reachable like every other control's.
  - The control-explanation study now counts 105 operable controls across 13
    surfaces, 66 hovered.
  - The toolbar photographs in both languages were re-recorded and reviewed.
- **Packaged:** `PackagedAcceptanceMain` prints `zoom lock OK` after locking
  through the packaged toolbar, proving the wheel and trackpad do nothing,
  Zoom In still zooms, and the unlocked wheel zooms again. It printed in both
  acceptance runs on all four images (macOS arm64 and x64, Linux x64, Windows
  x64), and in local gate 5.

## The owner checkpoint

The macOS arm64 image from PR #430's first run (`8e4e1cf`) was reviewed on the
owner's own profile, which starts unlocked. The journey covered:

- accidental wheel and trackpad zoom, unlocked and locked;
- toolbar zoom and explicit field selection while locked;
- pan, selection, inspector and search;
- persistence through restart;
- both languages and the control's accessibility;
- whether the control is clear without crowding the toolbar.

Verdict: **"works great."** No finding.

## The interaction CI route (#427 → #428)

#427 measured why every interface change was wide (about 45 minutes, most of
it the evidence job) and the owner ruled I1–I4 with six conditions; the record
is `docs/decisions/interaction-ci.md`.

- **Routes:** three now — narrow < interaction < wide.
- **Boundary:** derived, never listed. `EvidenceGenerators` splits the
  registered generators into 33 chart producers, 22 reproduced and 14
  photographers (`InterfacePhotographers`). `ChangeBoundary` reaches two
  closures through the compiled class graph: at `8e4e1cf`, 410 classes the
  chart producers reach and 517 the interface reaches.
  - A path in both is wide; anything unresolved is wide.
  - Language keys are judged one by one against the merge base.
  - `JUranometriaMain` is wide by name (I1).
- **Jobs:**
  - `test` and `display` run on every route;
  - `interaction-evidence` runs on interaction only (`make
    evidence-contracts-interaction`, 22 generators twice, 63 s locally, then
    an unchanged tree);
  - `evidence` runs on wide only;
  - `app-image` and `dist` run on interaction and wide (I3).
  - `main`, tags, releases and manual dispatch stay wide.
- **This sprint ran wide.** `make classify` on PR #430 said **wide**, as the
  ruling anticipated. On the feature's own account two paths caused it:
  `JUranometriaMain`, and the two ledger rows in
  `docs/studies/sky-language/manual-review.tsv`, a directory the chart
  producer `SkyLanguagePairMain` names. The owner ruled the branch stays wide
  and the ledger's conservative classification stands; neither rule is to be
  weakened to get a cheaper route. Every other feature path classified as
  interaction, including all eight new language entries (four keys in each
  language).
  - In CI, `interaction-evidence` was skipped and `evidence` ran.
  - **The interaction job has therefore not yet run in CI on a real pull
    request.** Its wiring is held by `RenderingRouteWorkflowTest`, and it
    proves itself on the first later change that fits its boundary.

## Chart output did not move

Between `99f6077` (4.0.0) and `4c39bf4`:

- nothing under `docs/reference/` or `docs/gallery/` changed;
- no PNG outside `docs/studies/interface-language/` changed;
- no source under `render/`, `sheet/`, `chart/`, `project/` or `module/`
  changed, and neither did `ChartComponent` or any ink class;
- `PROVENANCE.md` changed in exactly 8 rows, all of them the reviewed toolbar
  photographs, whose digests changed. No other evidence was promoted.

In local gate 4 (the portable contract), those 8 photographs moved from
legacy-baseline (held as committed) to reproduced: 136 → 128 and 268 → 276
against the 4.0.0 rehearsal. CI's full `evidence` job passed on `8e4e1cf`.

## Stopped and failed runs

1. **Development suite, first full run** on the feature tree: 3 failed.
   - The evidence contract's interaction mode now reaches `RenderingClosure`.
   - The moved photographer registry names the directory it audits.
   - A test pins a language key's line numbers, which the new keys moved.

   Corrected in `85e74da` and `8e4e1cf`. The second run passed 1809 with 0
   failed; the 22 aborts were the local keyboard-focus family.
2. **Gate 1 on `8e4e1cf`: stopped, not rerun.**
   - Tally: 1831 found and started, 1797 passed, 1 failed, 0 skipped, 33
     aborted (focus family).
   - The failure was a safe capture refusal in
     `SheetCaptureSizingTest.aLaggingContentCatchesUpToTheDeclaration`. The
     branch does not touch the capture machinery, and the test passed in both
     development runs.
   - Owner ruling, the #416 precedent: unexecuted local coverage, not passed,
     on condition that CI's first `display` run executes it.
   - Met: run 36938797870 passed it, 1831 of 1831 tests started and passed,
     with 0 failed, 0 aborted and 0 skipped.

   Gates 2–7 then passed once.

No CI run failed. Each check on PR #430 ran once.

## Post-merge, on `4c39bf4`

`4c39bf4` is a merge commit whose second parent is `8e4e1cf`. Each workflow
below ran once and succeeded:

- `test` on push (run 36949021764): `test`, `classify` (wide) and `evidence`
  passed; `interaction-evidence` was skipped, as it should be on `main`.
  `display` found, started and passed 1831 of 1831 tests, with 0 failed,
  0 aborted and 0 skipped.
- `app-image`, dispatched on `main` (run 36949027732): all four images and the
  cross-architecture smoke check passed, and each image printed
  `zoom lock OK` in both acceptance runs.
- `pages` on push (run 36949021316).

## For the companion-window roadmap

- **The ledger rule.** Most new controls add identifier rows to
  `manual-review.tsv`, so under today's accepted rule they will run wide.
  Whether the ledger belongs elsewhere, or whether `SkyLanguagePairMain`'s
  reach can be shown not to include it, is an open question for the owner.
  It is not something to change for a cheaper route.
- **`JUranometriaMain` stays wide.** Wiring a companion window into the
  application will be wide by ruling. Features whose wiring lives in their
  own surfaces are the ones the interaction route can carry.
- **The first interaction-only pull request** is also the interaction job's
  first real CI run. Watch its timings against the 17–18 minutes the decision
  record expects.
- **The ruled limits stand.** #427's limits — no companion window or panel
  architecture, no chart rendering changes — were held this sprint; the
  companion redesign starts from here.
