# JUranometria 5.2.0 — the road closed: Sprint 48 handover

*Issue #517, closing the road to 5.2.0 (#499). Written on main
`9da8f017`, the verified landing of the sprint's last pull request,
before any `VERSION`, tag or publication decision. Part I of the road
(Venus computed and tabled, #499–#503, #507, #508) is handed over in
`docs/decisions/venus-handover.md`; this document covers Part II -
Sprint 48, milestone "Venus on the chart" - and the whole road's
release boundary.*

## 1. What a reader gets in 5.2.0

As the unreleased changelog states them, newest first:

- **The article "Jupiter's little sky"** (#516), in English and
  Norwegian, on released 5.1 behaviour - published through Pages on
  merge, not a release.
- **Altitude ticks on the meridian** (#515): Place and Time's fourth
  switch, every 10° above the mathematical horizon on both halves, the
  figure beside each tick where the page has room.
- **Venus on the chart** (#514): View ▸ Solar System ▸ Venus on the
  chart, Show on chart and Centre on chart; the true phased disc, or
  the phased 6 px mark while the disc is smaller; a transit as Venus's
  dark disc inside the Sun's ring; Venus selectable as an occurrence.
- **The Sun, the Moon and Jupiter can be selected** (#513): a click on a
  drawn body is an occurrence - its identity at the instant and place
  drawn - that follows the page's moment and says when it is no longer
  present.
- **The Controller has two tabs** (#507) and **menu help no longer shows
  HTML source** (#508) - Part I's two checkpoint repairs.
- **Venus, computed offline** (#499–#501) and **the Venus table** (#502)
  - Part I.

Nothing else the atlas draws has changed: the Sun's, the Moon's and
Jupiter's pages are byte for byte as 5.1.0 drew them (section 6).

## 2. Every Part II ruling, as recorded

Each ruling is quoted in full in the record named; this is the index.

| ruling | where recorded | what was ruled |
|---|---|---|
| Venus's size and phase on the page | `venus-cartography.md`, *The ruling* (1, 2) | rule M: the larger of true scale and a phased 6 px mark, the mark cartographic, not a size claim; the Moon's phase geometry, χ of date into the page's basis, no invented sliver |
| the conjunction drawings | `venus-cartography.md` (3) | no special rule: painting order handles transit and occultation; the geometric state is spoken |
| C4–C6 inherited | `venus-cartography.md` (4) | Venus in `SolarSystemModule`; Show and Centre on chart with the layer |
| #481's decision | `venus-cartography.md` (5); #481 | **stays open**; Venus ships on the ordinary field ladder; the study's closer-zoom measurement attached to #481 for the later design |
| phase readability | `venus-cartography.md` (6) | a study measurement, never a production threshold |
| the occurrence contract and re-resolution | `celestial-occurrence.md`, *The ruling* (1) | the built behaviour: a selected occurrence follows the page's moment while the body is drawn; gone, the last resolved occurrence is kept and the Inspector says so |
| which bodies select | `celestial-occurrence.md` (2) | the Sun, the Moon, Jupiter and Venus; the four Galilean moons unselectable, for an explicit later ruling |
| the meridian ticks' step and density | `meridian-ticks.md`, *The ruling* | every 10°, a figure at every tick where placement has room, nothing below the horizon; off at startup, shared by both hosts, with the meridian, raised with its emphasis, `altitude-ticks on/off` |
| a symbol in front of an opaque disc | `venus-cartography.md`, *as built* | painted in the bodies layer over the farther disc (the #514 checkpoint finding); a name refused inside the Sun's ink at ≤ 4° is C4's, accepted |
| the article's text | #516 | accepted after one correction round (section 4) |

## 3. The owner's packaged verdicts of the sprint

Recorded in the owner's words on the issues, in order:

- **#514, on the candidate from `ac278af3`** (2026-10-12): *"Venus
  calculations look correct. The transit across the Sun is especially
  convincing. The earlier zoom-related problem appears resolved.
  Refusing Venus's label inside the Sun's ink at fields of 4° and
  narrower is accepted."* Two findings on the earlier candidates
  became the two fixes of that issue: Venus's symbol hidden under the
  Sun's disc at 4°–12° (`13753481`), and the chart ceasing to repaint
  past 4° (`ac278af3`, section 4).
- **#515, on the candidate from `4ec9bfb1`** (2026-10-13): the zoom
  fixed, the ticks accepted as proposed - *"Ten degrees makes the
  meridian a practical altitude scale, and labelling every tick
  preserves the simple 'read the nearest mark' behaviour."*
- **#516, on the texts at `2affc1c4` and `20ceaf6a`**: *"The articles
  are strong and worth publishing"*, with one factual finding -
  Ganymede's side - corrected together with two of the same class;
  then *"Accepted ... the new signed-offset contract now covers the
  whole sequence rather than only the cases we happened to notice."*

## 4. Every stopped, failed or incomplete run of Sprint 48

All records are kept under `~/juranometria-runs/runs/` on the owner's
machine, each under the head it ran on, and quoted on its issue.
Nothing was retried into green: every head that stopped was abandoned
for a new head qualified from gate 1, or continued only under a
recorded ruling. Part I's rows are in `venus-handover.md` §3.

| head | issue | what happened | disposition |
|---|---|---|---|
| `33f772c9` | #513 | STOPPED at gate 1: `ViewReport` pulled into the rendering closure by the packaged acceptance's call; `InterfaceTextTest`'s pinned `export.replace.question` lines moved by keys inserted above them | both real; fixed in `24a96bbd` |
| `24a96bbd` | #513 | all seven gates OK; **CI `test` FAILED**: `Toolkit.getMenuShortcutKeyMaskEx()` throws `HeadlessException` on the runner | a defect of the journey, not of the change; `SelectInteraction.toggleModifierMask()` in `39b66ed6` (gates and CI green); the ruled head `c5de398d` green |
| `669377fe` | #514 | gate 1 **INCOMPLETE** under the capture-refusal precedent: `ExportSheetDialogSheetMain` 332 → 326 - the third head with a 326 refusal (#501, #508, #514); the suite's own twice-test passed, so condition 3 held; gates 3–7 OK; CI green, `display` executed the refused test clean | continued under the precedent; the refusal family flagged to the owner as a standing investigation |
| `13753481` | #514 | all seven gates OK, CI green; the **candidate froze** at the owner's checkpoint past 4° at the transit | not a gate's finding: a `ClassCastException` in paint - `symbolOverADisc` asked a great circle for its distance; found by capturing the candidate's stderr; fixed in `ac278af3` with a test that offers a circle *ahead of* the module, as the application does |
| `ac278af3` | #514 | gates 1–4 OK; **STOPPED at gate 5 by a wrong tool argument** (the journey grep "Venus on the chart" against the log's `venus on the chart OK`) | an operator error; gates 3–7 run once more as a separate record, all OK; both records kept |
| `72bdc445` | #515 | STOPPED at gate 1: two pins the fourth switch moved - the control-explanations audit (136 → 137 operable controls) and nine access letters, not eight | regenerated and re-pinned in `e95cb772` |
| `e95cb772` | #515 | gate 1 OK; **STOPPED at gate 2**: `toolbar-en-2-results.png` differed - the search popup's row under the pointer drawn selected | the pointer, not code; the owner authorised one gate 2 with the pointer parked, which reproduced every artifact twice (both records kept); gates 3–7 then **STOPPED at gate 3**: the companion-window report lists the panel's controls (8 → 9) and three platform records grew; **CI failed** on the ink test (a tick under Linux's wider title block) and on the ticks study's font-dependent pixel counts |
| `4cef7972` | #515 | gates 1–4 OK; **ABORTED by the operator during gate 5** - a source edit in the gating worktree, so the build could not be attributed to the head; CI's ubuntu image failed the packaged journey on the same title-block band | stopped rather than finished into a misattributed verdict; `ABORTED.md` in the record; fixed in `4ec9bfb1` (all seven gates, both workflows green) |
| `2affc1c4` | #516 | all seven gates OK, both workflows green; the **owner's reading** found Ganymede "to the east" (it is west) | two more captions of the same class corrected; `JupiterArticleTest` now ties every side word to the signed Horizons offset; `20ceaf6a` green |

Lessons now in the standing rules: **park the pointer before gate 2**
(a popup list selects the row under the mouse); **never touch a
worktree while its gates run**; **a new control in Place and Time
moves five studies and one journey's letter count** - regenerate
before gating; **a study's font-dependent counts belong in its
platform record**, never in the portable report; **a headless probe
that offers only bodies cannot meet a fault the ecliptic exposes** -
offer a circle first; the gate tool's journey greps are
case-sensitive.

## 5. Routes, evidence movement and performance

### 5.1 The route every PR took

Each PR ran the wide route once after all seven local gates once; each
merge was pinned to its exact head and landed under the #494 rule,
**structurally verified** - second parent the accepted head, first
parent the qualified base, tree equal to the qualified trial merge's -
with the suites skipped and `app-image` not dispatched.

| PR | issue | head → merge | landing run | notes |
|---|---|---|---|---|
| #518 | #512 | `1f00c77a` → `b19f84ae` | 38198085306 | study; ruled head after the proposal head `98399f0e` |
| #519 | #513 | `c5de398d` → `81aab95e` | 38234517108 | ruled head after `39b66ed6` |
| #520 | #514 | `ac278af3` → `15a4283a` | 38298508593 | third head; two checkpoint fixes |
| #521 | #515 | `4ec9bfb1` → `3444c26b` | 38321506970 | fourth head; pins and platform |
| #522 | #516 | `20ceaf6a` → `9da8f017` | 38356506373 | Pages 38356506287 deployed and verified live |

### 5.2 What evidence moved

| merge | `docs/studies` | whole change |
|---|---|---|
| `b19f84ae` (#512) | 17 files: `venus-cartography/` created | 22 files, +1 388 |
| `81aab95e` (#513) | 6 files: the Inspector sheets' occurrence state, provenance, the test-evidence measurements | 37 files, +1 412 |
| `15a4283a` (#514) | 23 files: `venus-on-the-chart/` created, the menu sheets with the Venus item, provenance, the test-evidence measurements | 57 files, +1 912 |
| `3444c26b` (#515) | 71 files: `meridian-ticks/` created; the Place and Time, companion, clean-chart, chart-options-companion, solar-system-controls and control-explanations photographs for the fourth switch; provenance | 95 files, +1 601 |
| `9da8f017` (#516) | 0; `docs/articles` 14 files (two texts, eleven copied figures, the index) | 17 files, +768 |

No Sun, Moon or Jupiter chart page moved in any of them (section 6);
the only released-body imagery that moved is the Controller's Solar
System photographs, retaken for the fourth Place and Time switch they
show. The language ledger stands at **149 unresolved literals of its
150** (every new identifier - `venus`, the chart stores' keys, the
menu item, the tick identities - derived from an existing name rather
than spelled); the test-evidence measurements are requoted at **460
reads of live chart state against 1000 explicit hand-offs**.

### 5.3 Performance, measured

Local gates on this Mac, from the records' start and verdict times: a
full seven-gate run **52–53 min** a head (`98399f0e` 52:37, `c5de398d`
51:58, `13753481` 51:59, `4ec9bfb1` 52:30, `20ceaf6a` 51:44); gate 1
alone 11–12 min (a stop at gate 1 costs 11–12 min); gate 2 ≈ 3 min
(the authorised gate 2 took 3:12); gates 3–7 from gate 3 ≈ 38 min. CI
on a PR: the test workflow **28–49 min** (`1f00c77a` 33, `c5de398d` 28,
`ac278af3` 43, `4ec9bfb1` 46, `20ceaf6a` 49), the `evidence` job its
longest part at 37–45 min where photographs were retaken; the landing
on main under a minute. The budget's order is unchanged from Part I.

## 6. Existing-body invariance through the whole sprint

- **The Sun, the Moon and Jupiter.** Their packs are byte-identical
  (`PlanetsPackTest`, `SunInvarianceTest` on every run); no page under
  `docs/studies/sun-on-the-chart`, `moon-on-the-chart`,
  `jupiter-on-the-chart`, `jovian-*` or `solar-system` moved in any of
  the five merges - gate 2 and gate 3 compared them on every head, and
  the meridian-ticks study holds the three bodies' placements and every
  pixel inside their discs identical with the ticks on and off.
- **Venus's numbers from Part I.** `docs/studies/venus/service-accuracy.md`
  and `src/resources/planets` are untouched since `f5663336` (#501):
  Part II drew Venus from the same service, and `VenusReferenceVectorTest`
  and `VenusNamedEventsTest` held it on every head.
- **The occurrence model** changed how a click on a body is answered,
  not what is drawn: `RenderingClosureTest` holds the view report and
  the selection outside the rendering closure, and C4 and C6 keep
  their force for what a disc covers (`SunOnTheChartJourneyTest`,
  `VenusInkTest`).

## 7. What 5.2.0 contains, and what it explicitly does not

**Contains:** everything in section 1, on the Sun-and-Moon pack of
5.0.0, the Jovian pack of 5.1.0 and the planets pack of Part I, all
byte-identical to their first release.

**Does not contain, by the road's own boundary (#499):** no Mercury,
Saturn or later planet; no bookmarks (UI or storage - #497 stays the
design question); no server work; no local civil-time model and no
named time zones (#259 and its later work stay separate); no closer
zoom than 1° (#481 open, with Venus's measurement attached); no event
search, animation or live tracking; no cloud-top option for Venus (the
uncertain radius is explained, never drawn); no satellite shadows on
Jupiter; no selectable Galilean moons.

**What the next road inherits:** the later planets' barycentre/centre
and satellite-kernel questions, asked in each planet's own authority
study with sizes and purpose; #481's object-dependent fields with
Venus's and Jupiter's measurements; the Galilean moons' selection
ruling; a general redesign of *Update from Place and Time* (deferred in
`solar-system-controls.md`); the capture-refusal family at 326 px
(three heads over two sprints), to be investigated on its own; and the
standing rules of section 4.

## 8. The release rehearsal, and the owner's separate decisions

The rehearsal is what the seven gates and the two CI workflows already
do on the handover head, plus the live check, and it is recorded
rather than claimed: the **integrated evidence flush** is gate 3
(`make evidence-contracts`) and gate 4 (`evidence-contracts-ci`) on
this head; the **packaged release journey on every platform image** is
the `app-image` workflow's four images, each running the packaged
acceptance with the licensing inventory complete at 12 resources; the
**dist archive built twice and verified** with `verify-dist` is gate 6;
the **gallery and the articles reachable** is the live Pages check made
on `9da8f017` for #516 (both pages, their languages, twenty figure
requests, the corrected wording) and repeated after this document
lands. The results go on #517 and under
`~/juranometria-runs/runs/release-5.2.0/`.

Then, as 5.1.0 was done (`runs/release-5.1.0/README.md`): no `VERSION`
change, tag or publication in this issue. Each of the following is a
separate owner decision, asked for in turn and never inferred from the
one before: the `VERSION` bump with its changelog date and the
version-fed regenerations; the release pull request; its merge under
#494; the release workflow's manual-dispatch rehearsal (publishes
nothing; five archives and `SHA256SUMS.txt`); the local annotated tag;
the tag push, which publishes (`release.yml` has no gate); the public
verification; the close-out.

## 9. Still open after 5.2.0

#481 (fields below 1°, two measurements attached), #497 (bookmarks),
#259 (interactive furniture from Place and Time), #282 (macOS
quarantine, unsigned builds), #417 (the Earth's shadow, optional), #362
(photographers owning their Swing environment - the capture-refusal
family's likely home), and the backlog as listed on the repository.
