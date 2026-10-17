# JUranometria 5.3.0 — the road closed: Sprint 50 handover

*Issue #546, closing the road to 5.3.0 (#527). Written on main
`32234b73`, the verified landing of the sprint's last pull request,
before any `VERSION`, tag or publication decision. Part I of the road
(Mercury computed and tabled, and the two emphasis changes, #525–#531)
is handed over in `docs/decisions/mercury-handover.md`; this document
covers Part II - Sprint 50, milestone "Mercury on the chart" - and the
whole road's release boundary.*

## 1. What a reader gets in 5.3.0

As the unreleased changelog states them, newest first:

- **The article "The phases of Venus"** (#545), in English and
  Norwegian (*Venus' faser*), on released 5.2 behaviour - published
  through Pages on merge, not a release.
- **Closer fields for Mercury, Venus and Jupiter** (#544, closing #481):
  zoom in goes past 1° to 0.5° and 0.25° when one of the three, shown
  on the chart, lies on the page the step would show - by the toolbar,
  the Controller, the View menu, the shortcuts or the wheel; the page
  is called a closer field; panning keeps it, and a page the planet has
  left says why it offers nothing closer; zoom out returns to the
  ladder; never saved.
- **Place and Time opens again after a refused entry** (#551) - a 5.2.0
  defect found at the #544 checkpoint.
- **The closer fields prepared** (#543): the field printed to its own
  precision on every surface, finer grid steps below 1°, the catalogue
  asked for the closer page itself.
- **Mercury on the chart** (#542): View ▸ Solar System ▸ Mercury on the
  chart, between the Sun and Venus; the phased 6 px symbol on the
  ordinary ladder, over the Sun's disc in a transit; Show on chart and
  Centre on chart.
- **The Sun's centre dot capped** (#541) at a symbol's size - the one
  change to released ink, ruled.
- Part I: **Mercury, computed** (#529) and **in the Solar System
  tables** (#530); **Solar System labels as one emphasis class** (#526)
  and **Emphasis in the Controller** (#525); the handover (#531).

Nothing else the atlas draws has changed beyond what section 6 lists.

## 2. Every Part II ruling, as recorded

Each ruling is quoted in full in the record named; this is the index.

| ruling | where it stands | in short |
|---|---|---|
| #539, rulings 1-7 (2026-10-15) | `docs/decisions/mercury-cartography.md`, *The ruling* | rule M for Mercury; no Mercury conjunction exception, the Sun's dot capped near 3 px; Venus's interaction inherited, *Mercury on the chart* between Sun and Venus (C / K); #481 in 5.3.0 with visible phase on screen as its acceptance; closer fields 0.5° and 0.25° behind four foundations; Mercury and Venus first; six PRs in order |
| merges sequential (2026-10-15) | #540, #547 | each PR requalified against the exact main it lands on |
| #551 separate (2026-10-17) | #544, #551 | the Place and Time defect fixed and landed first, as its own issue and PR; #544 updated onto the repaired main and requalified |
| Jupiter joins the closer fields (2026-10-17) | `mercury-cartography.md`, *#544, as built*; #544 | the owner and the planner amended ruling 6 at the `602aaef4` checkpoint: Mercury, Venus and Jupiter; the Sun and the Moon never; Saturn after its own cartography study; every message in both languages |
| the closer fields' product finding (2026-10-17) | `mercury-cartography.md`; #544 | limited benefit on a laptop, clearly worthwhile on an external chart monitor; behaviour never depends on monitor size; the moons' shadows on Jupiter's disc future work only |
| #545 scope (2026-10-17) | #545 | the article describes Venus as released in 5.2, without the closer fields, keeping the Jupiter-article precedent |

## 3. The owner's packaged verdicts of the sprint

- **#539** - the seven proposals approved with the rulings above;
  production authorised in the stated sequence.
- **#541** - the cap accepted; merged after #540 on renewed
  qualification.
- **#542** (`77b06741`): *"Looks good"*; the 2019 transit checked at
  greatest transit; and at 1° at the time of testing, Mercury "very
  small but still" a clearly phased planet, "not super usable" - a true phase
  on an enlarged symbol, recorded on #542 and in *#542, as built*.
- **#543** - accepted without a packaged candidate (nothing visible):
  *the separation is right*.
- **#544** - three candidates. `b05b9c19`: the owner met the Place and
  Time defect while testing it (#551). `602aaef4`, after #551:
  *"looks ok now"*, Mercury's transit convincing, the captured error
  output empty. `bdb2d026`, with Jupiter: *no problems found; closer
  Jupiter views are worthwhile, especially for events such as the
  December triple transit; useful rather than essential; Jovian
  shadows remain future work.*
- **#545** - the texts accepted: *clear, accurate and well balanced*,
  the Norwegian reading naturally; the scope kept to 5.2.

## 4. Every stopped, failed, superseded or incomplete run of Sprint 50

Every record is kept under `~/juranometria-runs/runs/`, unchanged.

| run | what happened | how it was continued |
|---|---|---|
| `gates-j539-b2fff5b5` | all OK - the proposal head | superseded by the ruled head `ea140377` |
| `gates-j541-28c4f9c7` | all OK against main `156e6c21` | `SUPERSEDED.md`: #540 landed first, so #547 was updated and requalified at `75f52685` |
| `gates-j542-77b06741` | gate 1 failed on one test: `ExportCompanionTest`, refused by `ExportSheetDialogSheetMain`'s capture (333 → 326 px, the known kind) | `INCOMPLETE.md` with all seven precedent conditions; gates 2-7 once (`-from2`); CI's display run executed the refused test successfully |
| `gates-j544-b05b9c19` | all OK; its checkpoint found #551 | superseded after #551 landed |
| `gates-j544-602aaef4` | all OK; accepted | superseded by the owner's Jupiter amendment; never merged |
| `gates-j551-6d777fe3` | all OK; the new regression test aborted in gate 1 on the explained focus family | proven both ways in targeted runs (fails unfixed, passes fixed); CI's display run executed it successfully |

Two operator notes. The pointer-parking helper the gates use lived in
the session's scratchpad, which is volatile, and vanished between
#544 and #545; it was rebuilt under `~/juranometria-runs/tools/park`.
A packaged candidate started from Finder or `open` discards its error
output; since #544 every candidate is launched with its standard error
captured to a file under `runs/`, and #544's last two candidates'
logs stayed empty.

## 5. Routes, evidence movement and performance

### 5.1 The route every PR took

Each PR ran CI once after all seven local gates once; each merge was
pinned to its exact head and landed under the #494 rule,
**structurally verified** - second parent the accepted head, first
parent the qualified base, tree equal to the qualified trial merge's -
with the suites skipped and `app-image` not dispatched.

| PR | issue | head → merge | route | landing run | Pages |
|---|---|---|---|---|---|
| #540 | #539 | `ea140377` → `b91a8aa8` | wide | 38771214441 | 38771214342 |
| #547 | #541 | `75f52685` → `8c9940dd` | wide | 38791318819 | - |
| #548 | #542 | `77b06741` → `ec6784f9` | wide | 38811076049 | 38811076053 |
| #549 | #543 | `7541bb07` → `bae17857` | wide | 38853445649 | 38853445704 |
| #552 | #551 | `6d777fe3` → `a87c12f5` | interaction | 38940170667 | - |
| #550 | #544 | `bdb2d026` → `77140e07` | wide | 38989517654 | - |
| #555 | #545 | `6620d291` → `32234b73` | wide | 38999737943 | 38999737893, both articles verified live |

### 5.2 What evidence moved

| merge | `docs/studies` | whole change |
|---|---|---|
| `b91a8aa8` (#539) | 39 files: `mercury-cartography/` created | 44 files, +1 790 |
| `8c9940dd` (#541) | 14 files: the cap's pages (section 6), four Mercury study pages, provenance | 19 files, +100 |
| `ec6784f9` (#542) | 21 files: `mercury-on-the-chart/` created, the menu sheets with the new item, provenance, the test-evidence measurements | 50 files, +1 297 |
| `bae17857` (#543) | 31 files: `closer-fields/` created; #539's sub-1° pages through the production title and grid | 53 files, +1 300 |
| `a87c12f5` (#551) | 1 file: the test-evidence measurements | 5 files, +101 |
| `77140e07` (#544) | 32 files: the closer-fields study with the offer and Jupiter, #539's closer pages again, the menu strings, provenance, the test-evidence measurements | 60 files, +1 464 |
| `32234b73` (#545) | 0; `docs/articles` 13 files (two texts, nine copied figures, the index) and the Pages build | 15 files, +668 |

The language ledger stands at **149 entries**, under its bound of 150;
#544's view-report line was written so that it re-keyed none of them.
The test-evidence measurements are requoted at **465 reads of live
chart state against 1048 explicit hand-offs**.

### 5.3 Performance, measured

Local gates on this Mac, from the records' start and verdict times: a
full seven-gate run **52:50–53:41** a head (`ea140377` 53:41,
`75f52685` 52:58, `7541bb07` 52:58, `6d777fe3` 53:01, `bdb2d026`
52:57, `6620d291` 52:53); gate 1 alone 11–12 min; gates 2-7 from gate
2 41:37. CI's test workflow on a PR: **45–51 min** on the wide route
(`ea140377` 47, `75f52685` 48, `77b06741` 46, `7541bb07` 45,
`602aaef4` 51, `bdb2d026` 46, `6620d291` 47) and 15 min on the
interaction route (`6d777fe3`). The budget's order is unchanged from
Part I.

## 6. Existing-body invariance through the whole sprint

- **The packs.** No pack changed in Sprint 50: the planets pack is
  version 2 from #529 (Part I), the Solar System and both Jovian
  kernels byte-identical (`PlanetsPackTest` on every run).
- **The one ruled change to released ink** (#541, ruling 2): the Sun's
  centre dot, now at most 3 px in radius. Seven released-body pages
  moved for it and only for it - `sun-on-the-chart/equinox-3-nb`,
  `sun-on-the-chart/september-equinox-6`,
  `moon-on-the-chart/june-new-with-sun-12`,
  `venus-on-the-chart/transit-2012-1`, `-6`, `-8` and
  `venus-on-the-chart/superior-conjunction-2026-3` - and, among the
  unreleased road's own evidence, `solar-system-labels/transit-2-paper`
  and four `mercury-cartography` pages. No Jupiter or Jovian page
  moved.
- **The ladder.** No page at 1° or wider changed for the closer fields:
  their words, grid steps and title note apply below the floor only,
  and the evidence contracts reproduced every ladder page on every head.
- **Mercury is off until chosen**, so no existing page carries it.

## 7. What 5.3.0 contains, and what it explicitly does not

**Contains:** Mercury computed, tabled and on the chart; Solar System
labels as an emphasis class and the Controller's Emphasis group; the
Sun's dot capped; the closer fields 0.5° and 0.25° for Mercury, Venus
and Jupiter, with visible phase on screen at 0.25°; the Place and Time
reopening fix; the Venus article (already live on Pages).

**Does not:** Saturn, Mars or any later planet on the chart or in the
closer fields; the moons' shadows on Jupiter's disc; anything naming
the Sun on a 0.25° page that lies wholly inside its disc during a
transit (#543's finding, left open by the owner); emphasis kept
through zoom (#537, open); closer-field behaviour that depends on the
monitor.

## 8. The release rehearsal, and the owner's separate decisions

As for 5.2.0, the rehearsal is what the seven gates and the CI
workflows already do on the handover head, plus the live check, and it
is recorded rather than claimed: the **integrated evidence flush** is
gate 3 (`make evidence-contracts`) and gate 4 (`evidence-contracts-ci`)
on this head, every study of the road regenerated on the integrated
main; the **packaged release journey on every platform image** is the
`app-image` workflow's four images (macOS arm64 and x64, Linux,
Windows) and their cross-architecture smoke test in run 38993794049,
which qualified #555's trial merge `38f96dbf` - whose tree is main
`32234b73`'s, the code this document describes - each image running
the packaged acceptance with *mercury table OK*, *solar system labels
OK*, *emphasis controls OK*, *mercury on the chart OK* and *closer
fields OK* among its journeys. This head changes only this document,
so CI classifies it as documentation and skips the images and its own
evidence job; locally, gate 5 runs the packaged acceptance on this Mac
on this head. The **dist archive built twice and verified** is gate 6; the
**articles reachable** is the live Pages check made on `32234b73` for
#545 (both pages, their languages, a figure byte for byte against the
repository). The results go on #546 and under
`~/juranometria-runs/runs/release-5.3.0/`.

Then, as 5.2.0 was done (`runs/release-5.2.0/README.md`): no `VERSION`
change, tag or publication in this issue. Each of the following is a
separate owner decision, asked for in turn and never inferred from the
one before: the `VERSION` bump with its changelog date and the
version-fed regenerations; the release pull request; its merge under
#494; the release workflow's manual-dispatch rehearsal (publishes
nothing); the local annotated tag; the tag push, which publishes
(`release.yml` has no gate); the public verification; the close-out
of #527.

## 9. Still open after 5.3.0

#537 (emphasis kept through zoom and pan), #497 (bookmarks), #259
(interactive furniture from Place and Time), #282 (macOS quarantine,
unsigned builds), #417 (the Earth's shadow, optional), #362
(photographers owning their Swing environment - the capture-refusal
family, met again on #542), the 0.25° transit page that does not name
the Sun, Saturn's and Mars's own cartography studies, the moons'
shadows on Jupiter's disc, and the backlog as listed on the
repository.

### 9.1 Immediate successor: Sprint 51

Milestone "Sprint 51 — Make the chart state explicit", created after
this document's first head:

- [#556](https://github.com/Aha43/JUranometria/issues/556): rule the
  frozen UTC chart instant and update relationship.
- [#259](https://github.com/Aha43/JUranometria/issues/259): implement
  the accepted Place and Time chart furniture.
- [#537](https://github.com/Aha43/JUranometria/issues/537): preserve
  emphasis through zoom and pan.
- [#557](https://github.com/Aha43/JUranometria/issues/557): study the
  optional dense star-identity mode.
- [#558](https://github.com/Aha43/JUranometria/issues/558): implement
  it.
- [#559](https://github.com/Aha43/JUranometria/issues/559): hand the
  detail sprint to the next planet milestone.

Sprint 51 starts only after 5.3.0 closes, has no release of its own,
and excludes local civil time, bookmarks and NGC/IC/M annotation work.

### 9.2 The longer road

- [#553](https://github.com/Aha43/JUranometria/issues/553): time-range
  paths, for the first planet milestone after Mercury.
- [#554](https://github.com/Aha43/JUranometria/issues/554): the
  Sun–Earth–planet configuration diagram, around the 6.0 wrap-up.
