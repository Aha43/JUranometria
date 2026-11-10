# JUranometria 6.5.0 — see the whole sky at chart time: handover to release

*Issue #704, closing road #703 (Sprint 59). Written on the integrated
head of `docs/704-release-6.5.0-handover` (main `d0dc9fc9` plus the
packaged whole-sky journey and this handover), before any `VERSION`, tag
or publication decision; 6.5.0 is this document's working name for the
next release, the owner's to confirm. Since 6.4.0 (`3c6ad6c2`, published
2026-11-07) four landings stand on main: the ruled study #686
(`c6c7b102`), the whole-sky view #687 (`6023465a`), the polar-boundary
correction #709 (`7d273bbb`) and the bilingual article #705
(`d0dc9fc9`). No catalogue, service, planet or pack changes. The road's
promise, from #703: **one honest view of the whole sky at chart time,
chosen explicitly and never entered by zooming.***

## 1. What a reader gets in 6.5.0

As the unreleased changelog states them, oldest first:

- **The whole sky at one chart time** (#687, ruled on #686). A *Whole
  sky* / *Hele himmelen* toggle on the toolbar, in the Controller and in
  the Chart menu shows the entire sky at the Place and Time instant as
  one equal-area map: Hammer projection, ecliptic north up, ecliptic
  longitude increasing left at centre, the central meridian at the
  zenith's ecliptic longitude, every Solar System body switched on above
  or below the horizon. The page states what it is - never a centre as a
  place or a radial field - on the chart, aloud, in the exported sheet
  and in the view report. Zoom and pan are unavailable in it and say so;
  *Open here* returns to the ordinary chart centred on the chosen object
  or position at the field left; Reset view returns to the first page;
  On this page is unavailable in it and points to Open here. A whole-sky
  view can be bookmarked with its chart time and place; existing
  bookmarks are unchanged; nothing about the mode is remembered between
  sessions. The graticule is numbered sparsely at the outline and along
  the central meridian. Every existing page is drawn exactly as before.
- **A horizon whose east or west point is the equinox** (#709). A chart
  with the mathematical horizon shown could fail to paint when a cardinal
  point fell within a few hundred-trillionths of a degree west of right
  ascension 0h, an error in every release since 3.0.0 that about a
  picosecond of chart time would reach. Found by measuring the whole-sky
  page with the zenith at an ecliptic pole. The same wrap in a deep-sky
  outline is guarded too. Nothing else about any page changes.
- **The whole sky at one time: the reader article** (#705). *The whole
  sky at one time* / *Hele himmelen på ett tidspunkt*, seven figures that
  are the as-built study's pages byte for byte, every number held to the
  record it is read from, in both languages; live on Pages.

**The reader purpose, distinct from the two existing wide views.** The
stereographic overview (the 60°, 90° and 120° rungs) and the 180°
orthographic globe each show *a part* of the sphere about a centre, with the ordinary
chart's radial claims: a centre as a place, a field width, a single
scale. The whole sky shows *all of it* at once - the bodies above and
below the horizon in one frame, with the ecliptic across the middle so
the planets' relationship to it and to the horizon reads at a glance -
and is entered by one explicit switch, never by zooming out (#703's
boundary: projection is a mode, not another rung). It is the view to
*account for* every enabled body at the chart time and then *choose
one*; the ordinary chart is where the detail is. The overview and globe
are untouched (section 8).

## 2. Every ruling of the road

| ruling | where it stands | in short |
|---|---|---|
| the road (owner, 2026-11-07/08) | #703; #686, #687 bodies | study with real production ink and stop before production code; build only the admitted route to an explicitly unqualified packaged candidate; article from the accepted build; one integrated qualification under a declared gate-impact plan; projection is a mode, ordinary zoom never changes projection, no projection chooser, no meteors, time paths, new catalogue data or second chart; #697 separate; VERSION, rehearsal, tag, publication, verification and cleanup separate owner decisions |
| study amendments (owner, 2026-11-08) | #686 | control = equirectangular; measure four central meridians (chart centre, observer's meridian, anti-Sun, fixed convention) plus the zenith; the architecture question is a comparison of the existing `Projection` interface against a page renderer of its own, modules the single source of ink; bookmarks in the state inventory and the rulings; stop unqualified, no gates; one study door, `Projections.drawnForStudy` |
| R-1 to R-14 and architecture A (owner, 2026-11-08) | `docs/decisions/all-sky-projection.md` §7 | admitted; Hammer; ecliptic horizontal; R-4 the central meridian is the zenith's ecliptic longitude, *observer-aligned*, the zenith on the central meridian and never at the map's centre; the seam rule, the sampled-curve route, the true horizon with Subdue; R-8 Open here into the last ordinary field; R-9 zoom and pan refused with an explanation, Home the ordinary Home; R-10 never restored at startup; R-11 bookmarks as B-2, chart time and place required, schema 1 kept, every existing bookmark byte for byte; R-12 no radial claims, the ruled title clause *ecliptic longitude increases left at centre*; R-13 one toggle; R-14 On this page unavailable in the mode, pointing to Open here; architecture A: extend the one `Projection` interface, existing azimuthal pages byte-identical |
| R-15 grid notation (owner, 2026-11-08, first hands-on) | §7 R-15 | "add the outline-aware grid notation as proposed"; body names at the seam stand as built |
| second hands-on (owner, 2026-11-08) | §7 R-15, §8 Words | the seam-label rule and R-15 accepted as built: "the numbered graticule now feels deliberately quiet and complete"; the central-meridian wording ended in fragments in both languages; corrected to whole sentences before the final verdict |
| #687 accepted (owner, 2026-11-08) | #687 | "I accept the corrected #687 packaged candidate at 3fabc4e8." |
| gate plan for `7e349fd5` (owner, 2026-11-09) | #687 | all seven once on exactly `7e349fd5`: "accepted application bytes unchanged; preserve the stopped 3fabc4e8 pre-flight and its lesson" |
| CI-failure corrections and #708 (owner, 2026-11-09) | PR #707; #708 | corrections A (the `%.1e` residual column stated as yes/no within 1e-12) and B (`WholeSkyPageTest` releases its services) approved; the pre-existing static holders and the suite-heap decision go to #708, not this road; amended-head gate plan 1, 3, 4, 7 once; the failed CI run preserved |
| the capture-refusal precedent (owner, 2026-11-09 and 2026-11-10) | #687, #709 | confirmed satisfied for gate 1 on `db907424` and gate 2 on `44325dea`, condition 7 met by CI's display job each time; the refused test stays not passed locally |
| #705 first ruling (owner, 2026-11-09) | #705 | "Keep the grid figure"; both titles stand; the rejected candidates stay a comparison in words and numbers; textual corrections to follow; "File and diagnose the zenith-near-an-ecliptic-pole edge as a Sprint 59 issue before article qualification; do not assume a fallback. If the product needs correction, land that first and rebase the article." |
| #709 ruling (owner, 2026-11-09) | §9 | accept the `LocalSky.positionOf` correction; keep the `SkyFootprint.at` guard (the same invalid-360 defect, proven at its own boundary, matching `PageExtent`); **R-4 stands unchanged, no fallback and no hysteresis near the ecliptic poles**; the high-latitude consequence stated in the record and the article, in the owner's words; all seven gates on `28a42a17`, CI once, stop for the pinned merge |
| #705 corrections and verdict (owner, 2026-11-10) | #705; commit `66a48bc8` | six bilingual corrections (whole-sky figures; the Sun exactly on the seam; the Mollweide comparison; a projection centre but no movable chart centre, scale or field; *ligger senit*; what Open here is for) and the high-latitude paragraph verbatim; then "one of the best articles made so far"; gate plan 1, 3, 4, 5, 6, 7 and the site build approved, gate 2 skipped |
| the merges (owner, 2026-11-08, -09, -10, -10) | PRs #706, #707, #710, #711 | each merged pinned to its accepted head under the merge-identity rule, on the owner's instruction |
| #704 (owner, 2026-11-10) | this document | the integrated handover with the packaged journey (enter, account, select, Open here, a whole-sky bookmark), the live article, existing-projection invariance; #708 recorded and its margin measured, not fixed here unless a required qualification exposes a new failure; stop with the handover and a proposed gate-impact plan; no VERSION, rehearsal, tag, publication or road close |

## 3. The owner's checkpoints and verdicts

- **#686, the ruled study** - `cc59b2b2`, twenty candidate pages in
  three forms at three chart times, no production change beyond the one
  study door: ruled R-1 to R-14 with architecture A the same day; the
  door removed by #687.
- **#687, the unnumbered candidate** - `34a53673`, the packaged
  application in the owner's hands: the grid notation ruled (R-15), the
  seam names left as built.
- **#687, the numbered candidate** - `7987474a`: *"the numbered
  graticule now feels deliberately quiet and complete"*; one wording
  defect found, the trailing *, the zenith's* / *, senits*.
- **#687, the corrected candidate** - `3fabc4e8`: *"I accept the
  corrected #687 packaged candidate at 3fabc4e8."* The accepted
  application bytes are those of every later head of that branch
  (`7e349fd5` test-only; `db907424` a study column and a test release).
- **#709, the boundary** - the owner asked for the observation to be
  filed and diagnosed before the article qualified; ruled on the
  diagnosis as section 2 records.
- **#705, the text** - `68474915` read with the corrections given
  afterwards; `66a48bc8` with them and the high-latitude paragraph:
  *"One of the best articles made so far."*

## 4. The contracts as built

The contract is `docs/decisions/all-sky-projection.md`: §7 the ruling,
§8 as built, §9 the poles. In short, with what holds each clause:

### 4.1 The study architecture (#686, held as history)

- **Real production ink through one door.** `Projections.drawnForStudy`
  let the study draw every production pass - stars, figures, boundaries,
  labels, the Solar System modules, the meridian's horizon - through
  three whole-sphere forms (Hammer, Mollweide, the equirectangular
  control) at three chart times the modules themselves found: the Oslo
  evening of 2026-12-15 21:00 UTC (six of eight bodies up in darkness),
  the gathering of 2027-04-25 (seven planets within 138.7° of ecliptic
  longitude) and the spread of 2029-01-03 (no gap wider than 64.2°).
- **What it measured** (`docs/studies/all-sky/measurements.md`, held as
  committed with its superseded status at its head): Tissot deformation
  over the sphere (Hammer mean 35.6°, Mollweide 32.2°, equirectangular
  16.8° but area scale 0.637 to 36.5); handedness and the orientation
  sentence; what no existing page can do; every enabled body accounted
  for; four central-meridian candidates in two orientations (the zenith's
  ecliptic longitude puts the zenith 0.0° from the central meridian; the
  chart centre left it 37° to 39° off); the winding horizon; what each
  existing pass does at the seam; the journey into an ordinary chart.
- **The two architectures compared** (§3): extending the one
  `Projection` interface against a page renderer of its own. Ruled A.

### 4.2 The whole sky as built (#687)

- **Architecture A, exactly.** `Projection` gained `outline()`,
  `mayJoin(from, to)`, `greatCirclesAreConics()` and `wholeSphere()`,
  each with the azimuthal answer as its default, so no existing
  projection changed a byte. `GreatCirclePage.clip` takes a sampled route
  (1 440 samples, broken by the join rule, refined to the seam by
  bisection, `CurveRun.Polyline`) only for a projection without conics;
  the #298 seam's exact forms stand everywhere else. `PageRegion` carries
  an elliptical limb; the three joining passes ask the join rule; a split
  figure's name anchors on the part holding more ink; `BelowHorizon`
  reads the sampled horizon's chords. The modules were not touched.
- **The kind and the mode.** `ChartProjection.HAMMER` draws
  `WHOLE_SKY_DEGREES` (360) and nothing else; that width is not a rung.
  `ChartViewController` keeps the ordinary view while the whole sky is
  shown, restores it exactly on leaving, treats every recentre from the
  whole sky as Open here into that view's field (R-8), refuses pan and
  pointer zoom (R-9), and moves the central meridian when the observer
  changes (R-4). `WholeSkySession` is the one switch (R-13); nothing
  stores it (R-10).
- **Words (R-12).** Title *Whole sky · Hammer · ecliptic north up ·
  ecliptic longitude increases left at centre* / *Hele himmelen · Hammer
  · ekliptisk nord opp · ekliptisk lengde øker mot venstre i midten*;
  facts *Central meridian: zenith's ecliptic longitude 65.0° · Stars to
  V 5.0*; aloud and in the sheet *Central meridian at the zenith's
  ecliptic longitude, 65.0 degrees.*; the view report's
  `field-degrees: whole-sky`, `view`, `orientation`, `central-meridian`
  and `central-longitude-degrees` lines; the scene's language-free title
  `λ 65.0°`. No ordinary *Centre … ICRS J2000* or *Field* claim anywhere.
- **Refusals said.** Zoom in and out explain themselves; the Inspector's
  Centre here is Open here; On this page shows its refusal sentence
  (R-14); the meridian's altitude readout is not placed.
- **Grid notation (R-15).** RA figures where a meridian meets the
  outline, placed outward along the outline's normal; Dec figures above
  and right of a parallel's crossing of the central meridian; each
  refusing independently; never across the seam; a meridian through an
  ecliptic pole not named there.
- **Bookmarks (R-11, B-2).** `SavedView` has a whole-sky form written as
  `"view": {"mode": "whole-sky"}` and nothing else; such a record needs
  its chart time and place or is kept as needing attention; the add
  dialog ticks and holds both; opening enters the whole sky after the
  stored time and place are the chart's; every ordinary bookmark is
  written byte for byte as before.
- **Measured on the accepted build** (`docs/studies/whole-sky/measurements.md`):
  nine of nine bodies on every page; the central longitude the zenith's
  to four decimals (evening 64.9659, gathering 175.3130, spread
  104.3135), the zenith 37.67° to 68.63° from the map's centre; every
  reference circle one run with ends on the outline within 1e-12; no
  figure or grid piece across the page and no name stranded; Subdue
  disagreeing with the true horizon on 0.040 to 0.141 per cent of the
  sky; Open here landing 0.000000° from Saturn at the 60° field left; the
  monitor pages carrying 10 RA and 10 Dec figures.

### 4.3 The polar-boundary correction (#709)

- **One correction, not the whole sky's.** With the zenith at an
  ecliptic pole the horizon is the ecliptic and its east or west point is
  the equinox at 0h. `LocalSky.positionOf` wrapped a negative right
  ascension by adding a turn; within 2.84e-14° below zero the sum is
  exactly 360.0, `SkyPosition` refused it, the meridian module's cardinal
  points threw and the page did not paint - #359's code, since 3.0.0,
  reachable on an ordinary chart. It wraps with `SkyFrame.normalise` now,
  held at the function by `LocalSkyTest`; `SkyFootprint.at` carried the
  same wrap and has the same guard (`SkyFootprintTest`), kept by the
  owner's ruling.
- **The boundary proof, preserved.** `juranometria.tool.WholeSkyBoundary`
  is shared by the as-built report's section 12 and
  `WholeSkyPageTest.thePageHoldsWithTheZenithAtAndAroundAnEclipticPole`
  (24 rows: at each pole and 1e-12, 1e-9, 0.001, 0.1 and 1 degree from
  it): every page paints, the zenith is on the central meridian, all nine
  bodies are on it, the reference circles are finite, Subdue is true;
  within about 1e-12° of a pole the central longitude is decided by
  rounding and any choice is a correct map. `WholeSkyBoundaryRecordTest`
  holds the report's rows to the record's words.
- **R-4 at high latitude, measured and ruled.** The critical latitude is
  the J2000 ecliptic pole's declination of date, 66.5588 at the measured
  instant. Equatorward the central meridian goes all the way round,
  unevenly (Oslo's fastest turn 1.09° per minute; at 66.5 a quarter turn
  in 71 s); poleward it swings and returns (158° at 67, 119° at 70, 52°
  at 80) about ecliptic longitude 90 in the north and 270 in the south.
  **The owner ruled R-4 unchanged, no fallback and no hysteresis**: every
  page is correct; what the latitude changes is how the map moves with
  chart time; stated in the record (§9) and in the article in the owner's
  words.

### 4.4 The article (#705)

Seven figures, each the as-built study's page byte for byte, one new
(`evening-en-white-grid.png`, the evening page with the equatorial grid
raised by Emphasis, drawn by `WholeSkyStudyMain` and recorded in the
report's pages section). `WholeSkyArticleTest` reads both articles and
holds every figure and phrase to the record it comes from, the latitude
row for 66.5° and 71 s included (six tests). Live after the
landing: https://aha43.github.io/JUranometria/articles/the-whole-sky-at-one-time.html
and `hele-himmelen-pa-ett-tidspunkt.html`, built by the pages run
42813917024 and answering 200.

## 5. Both languages, accessibility, screen, speech, keyboard, report, export and paper

- **Languages.** `page.wholeSky.{subject,title,facts,spoken,sheet.description}`
  and `page.projection.hammer` in both files; the toggle, the menu item,
  the Controller's control, the zoom and pan refusals, the On this page
  refusal, the bookmark dialog's words in both. The language ledger grows
  from 180 to 185 unresolved identifiers (the whole-sky identifiers:
  `wholeSkyItem`, `whole-sky`, `hammer`, `ecliptic`, `wholeSky`), under
  its bound of 190. The control explanations count 174 operable controls
  on 21 surfaces, one more dynamic and one fewer hovered than at 6.4.0.
- **Speech.** The page speaks its own sentence (section 4.2) and names
  every body with *below the horizon* where it is; the ordinary sentence
  is never spoken on it (`WholeSkyWordsTest`).
- **Keyboard.** The toggle is an ordinary control reached like every
  other; no shortcut was added or moved. Keyboard focus of the toggle is
  listed as not exercised by the study.
- **Report.** `field-degrees: whole-sky`, `projection: hammer`,
  `view: whole-sky`, `orientation: ecliptic-horizontal`,
  `central-meridian: zenith-ecliptic-longitude`,
  `central-longitude-degrees`, never `field-degrees: 360`.
- **Export and paper.** The sheet's metadata, description and file name
  branch on `wholeSphere()`; the exported sheet drawn end to end is
  listed as not exercised by the study (its words are held).
- **Screen.** Both palettes, monitor and laptop sizes, in the study's
  pages; the Milky Way has no production ink (#246) and is not drawn.

## 6. Every stopped, failed, superseded or incomplete run

Every record is under `~/juranometria-runs/runs/`, unchanged; nothing was
rerun into green.

| run | what happened | how it was continued |
|---|---|---|
| #686's first pre-flight on `278ec37d` (`preflight-j686-278ec37d`) | the canonical contract breached 23 times: the study's platform record named no machine and carried timings; 22 pages had no provenance row; the tree was dirty afterwards | fixed in `4650db66` (`PlatformEvidence.preface`, the pages registered); pre-flights on `4650db66` and `844e5dff` clean; seven gates OK on `844e5dff` |
| #686's dev-check suite (`j686-devcheck`) | 0 failed, 49 focus aborts, no verdict recorded | the pre-flights and gates carry the verdicts |
| #687's pre-flight on the accepted `3fabc4e8` (`preflight-j687-3fabc4e8`) | 1 of 2 718 failed: `WholeSkyWordsTest` still searched for the superseded spoken phrase; contracts, dist and provenance OK | test-only correction `7e349fd5` (+16 −9, the whole wording pinned in both languages); its pre-flight 0 failed; the accepted application bytes unchanged, as the owner's gate approval states |
| **CI once on `7e349fd5`, run 42538687161** (`ci-j687-7e349fd5-FAILED`), after all seven local gates were OK | the display job ran out of Java heap at the suite's end (`SaturnOnTheChartJourneyTest.load`, a 63 MB kernel) while the headless job passed; the evidence job breached the contract: the §5 residual column printed 1.1e-16 on macOS arm64 and 2.2e-16 on Linux x64 | **not retried**; measured (`heap-j687-7e349fd5`: 33 full collections under a 4 GB cap, 4.0 GB live; `WholeSkyPageTest`'s static services +388 MB); corrections A and B as `db907424`; the older holders filed as #708 |
| `heap-j687-7e349fd5` | the README's first figures were parsed from `gc,start` lines as well as pauses ("66 full GCs", "~617 MB") | corrected in place with `gcsum.py` (33 collections); the corrected table stands in `heap-j687-db907424` |
| **gate 1 on `db907424`** (`gates-j687-db907424-plan`) | **INCOMPLETE**: `InterfaceEvidenceGateTest.everyCommittedArtifactHasARegisteredGenerator` refused by `PlaceAndTimeSheetMain`'s capture, 420 → 326 px, the known fail-closed kind; 2 705 passed, 0 other failures | not rerun; the seven precedent conditions recorded; gates 3, 4, 7 continued OK; condition 7 met by CI's display job (run 42584790350, 2 718 of 2 718, the refused test ✔); the owner confirmed the precedent satisfied; gates 2, 5, 6 held from `7e349fd5` by the approved plan - a reason later corrected (section 6, lessons), the gap covered by CI's dist and app-image jobs and by `make app-image` on landed main |
| **gate 2 on `44325dea`** (`gates-j709-44325dea-plan`) | **INCOMPLETE**: `everyCommittedArtifactIsWhatItsGeneratorProducesTwice` refused by the same capture, 420 → 326 px; gate 1 OK with the interface gate fully green inside it (26 focus aborts, the usual family) | not rerun; conditions recorded (3 met by gate 1 of the same run); gates 3 to 7 continued OK (`gates-j709-44325dea-from3`); condition 7 met by CI's display job (run 42721023106, 2 723 of 2 723, the refused test ✔ in 93.5 s); the owner confirmed |
| #705's checkpoint `68474915` | the owner's ruling said "apply the textual corrections above", but no list reached the branch; #709 was to land first | the corrections arrived with the #709 ruling and were applied after the rebase (`66a48bc8`); the article's two citations of 66.5° and 71 s wait for the landed report |
| #705's rebase onto `7d273bbb` (`j705-rebase-*.log`) | only the CHANGELOG conflicted (both entries kept, #709's first); the study and the two inventories auto-merged with two stale headline counts (1 022) | the study regenerated byte-identical; provenance and test-evidence regenerated to 1 023 |
| superseded: the #686 study door and generator | `Projections.drawnForStudy` and `AllSkyStudyMain` removed by #687; the study's 24 files held as history with their status at the head | the record of the comparison that ruled the view |
| superseded: the agent's proposed R-4 (centre on the observer's sky, LST in the equatorial orientation) | replaced by the owner's R-4 at the ruling | §7's correction note |
| superseded: the agent's longer high-latitude paragraph for the article | replaced by the owner's text verbatim | `66a48bc8` |
| #704's dev checks (`j704-devcheck-app-image-1.log`) | the first packaged run of the whole-sky journey failed its own pin: the ordinary title reads *Field 60.0°*, not *Field 60°*; everything before it passed | the pin corrected; the second run `whole sky OK`, `PACKAGED ACCEPTANCE OK` |
| #704's suite heap measurement (`heap-704-d0dc9fc9`) | `make classes` ran in the worktree 20 minutes into the measurement, to compile the journey - a lapse of the never-touch-a-running-worktree rule, recorded in the README | the tally equals gate 1 on `66a48bc8` (2 729 / 2 717 / 0 / 12), the suite never loads the one class that changed; the measurement stands with the caveat |

Lessons this road adds:

- **a platform record must not carry timings or heap** (the #686
  pre-flight's 23 breaches): a platform page records what the machine
  drew, not how long it took;
- **a `%.1e` residual column is not portable**: 1.1e-16 on macOS arm64,
  2.2e-16 on Linux x64; state the claim as yes/no within a tolerance;
- **a static service in a test class lives for the rest of the suite**:
  `WholeSkyPageTest` retained 388 MB; release in `@AfterAll`; the six
  older holders are #708's;
- **the old-phrase sweep must search phrases, not ingredients**
  (`WholeSkyWordsTest` on `3fabc4e8`);
- **a change under `src/`, tools included, moves gates 5 and 6**: the
  packaged jar holds the study tools (570 `juranometria/tool/` entries in
  the 6.4.0 image);
- **a rebase that touches generated counts needs regeneration**: the
  #705 rebase auto-merged two study pages' rows but left the headline
  counts stale.

## 7. Routes, evidence movement and performance

### 7.1 Routes

| head | local gates | CI | outcome |
|---|---|---|---|
| `cc59b2b2` (#686 study) | none (unqualified checkpoint) | none | ruled |
| `844e5dff` (#686 ruled, registered) | all seven once, `gates-j686-844e5dff`, 49 min | PR #706 once, 17 of 17 jobs green (test 42484714594, dist 42484714569, app-image 42484714554; evidence 42 min 54 s) | merged pinned → `c6c7b102` (2026-11-08); post-merge test 42488762212, pages 42488762329 |
| `34a53673`, `7987474a`, `3fabc4e8` (#687 candidates) | none (unqualified candidates) | none | hands-on; `3fabc4e8` accepted |
| `7e349fd5` (#687, test-only on the accepted bytes) | all seven once, `gates-j687-7e349fd5`, 49 min | PR #707 once: **failed** (test 42538687161; dist 42538687194 and app-image 42538687152 green) | corrected, not retried |
| `db907424` (#687 amended) | 1 INCOMPLETE, 3, 4, 7 once (`gates-j687-db907424-plan`, 11.5 + 32 min); 2, 5, 6 held from `7e349fd5` | once, all green (test 42584790350: display 2 718 of 2 718; dist 42584790331; app-image 42584790490) | merged pinned → `6023465a` (2026-11-09); post-merge test 42595577682, pages 42595577742 |
| `28a42a17` (#709 correction and proof) | pre-flight only | none | ruled; one prose-only commit followed |
| `44325dea` (#709 with the ruling) | 1 OK, 2 INCOMPLETE, 3 to 7 once (`gates-j709-44325dea-plan`, `-from3`; 16 + 34 min) | once, all green (test 42721023106: display 2 723 of 2 723; dist 42721022993; app-image 42721022991) | merged pinned → `7d273bbb` (2026-11-10); landing test 42772136023; pages not triggered |
| `68474915` (#705 text) | pre-flight on `60a9e681` | none | read; corrections followed |
| `66a48bc8` (#705 rebased and corrected) | 1, 3, 4, 5, 6, 7 and the site once (`gates-j705-66a48bc8-plan`, 46 min); 2 skipped, the interface gate green inside gate 1 | once, all green (test 42806197872: display 2 729 of 2 729; dist 42806197748; app-image 42806198300) | merged pinned → `d0dc9fc9` (2026-11-10); landing test 42813916916, pages 42813917024 |
| the integrated head with the journey and this handover | the final qualification (section 9) | once | - |

Every merge was recorded before it from `refs/pull/N/merge` and proven
after it by `scripts/verify-merge-identity.sh` (`landing-706`, `-707`,
`-710`, `-711` and their judges): the second parent the accepted head,
the first parent the qualified base, the tree the trial merge's, the
three PR runs qualifying that trial merge. The main checkout was
fast-forwarded to each landing with the exact tree.

The suite stood at **2 672** tests at 6.4.0 and **2 729** at `d0dc9fc9`:
#686 added 13 (2 685), #687 33 (2 718), #709 5 (2 723), #705 6 (2 729).

### 7.2 Evidence movement

From `3c6ad6c2` to `d0dc9fc9`: **124 files, +7 808 / −126 lines** - #686
42 files, #687 94 (+5 018 / −2 396, the study door and generator
removed), #709 15, #705 18. Under `docs/studies`: `all-sky` (24 files,
held as history), `whole-sky` (14 files, the as-built report, nine pages,
the journey pair, the pole page and the raised-grid page),
`control-explanations`, `language-ledger`, `test-evidence` (explicit
hand-offs 1 183 → 1 185) and the provenance record, whose promoted
artifacts went from **989 to 1 023** (34 new, every one under the two
whole-sky studies). Source: 47 files under `src/` (section 8 lists them by
what they are), 17 test files (9 new). Decision records:
`all-sky-projection.md` (new), `overview-projection.md` (the sampled
route amendment), `coordinate-grid.md` (the R-15 labels),
`bookmarks.md` (the whole-sky form), `test-evidence.md`. Articles: two
texts and seven figures. The packaged licensing inventory stays at
**16 resources**.

**The unaffected-chart invariance.** No study outside `all-sky` and
`whole-sky` moved a page: every ladder page, the overview and globe
studies, every planet study, the sheets and the gallery reproduced
byte-identical under both contracts on every gated head (gate 7 on
`844e5dff`, `7e349fd5`, `db907424`, `44325dea` and `66a48bc8`: 0 newly
dated apart from the whole-sky pages each head added). Section 8 says
why.

### 7.3 Performance

**Timings.** The seven-gate runs took 49 min on `844e5dff` and
`7e349fd5` (suite 11.5 to 11.8 min, interface gate 3.8, canonical
contract 15.2 to 15.3, portable 16.9 to 17.2, app image 51 s, dist 19 s,
provenance 3 s); the planned runs 44 min on `db907424` (gate 1 plus
3, 4, 7), 50 min on `44325dea` (all seven in two records) and 46 min on
`66a48bc8` (six gates and the site, canonical 15.8, portable 17.5). CI's
test run took 26 min to fail on `7e349fd5` and 34 to 36 min green on the
later heads; the evidence job 42 min 54 s on `844e5dff`. The #686 study
drew its twenty candidate pages in 8 s at 0.95 GB; the as-built study's
pages and bookmark round trip run inside the canonical contract.

**The suite heap, measured for #708** (`heap-704-d0dc9fc9`, the #708
command: `make test` under `-Xmx4g` with GC logging, on `d0dc9fc9`):

| head | run | full GCs under the 4 GB cap | lowest after-young per minute (MB) | last 15 s after-young |
|---|---|---|---|---|
| `7e349fd5` (before #687's release step) | 748 s | 33, from 735.7 s; live after full GC 3 712 to 4 035 | 1: 486 · 2: 1 140 · 3: 2 085 · 4: 2 506 · 7: 2 998 · 8: 3 262 · 12: 3 741 | 3 741 to 4 094 |
| `db907424` (#687 as landed) | 697 s | 0 | 1: 475 · 2: 1 289 · 3: 1 948 · 4: 2 156 · 7: 3 077 · 11: 3 462 | 3 462 to 3 800 |
| **`d0dc9fc9` (after #709 and #705)** | 698 s | **1, at 693.2 s; live after it 3 616** | 1: 486 · 2: 1 007 · 3: 1 953 · 4: 2 182 · 7: 3 047 · 11: 3 410 | 3 410 to 3 767 |

**The present margin:** the live set still grows through the run, to a
bound of 3 410 MB in the last minute, and one full collection under the
4 GB cap found **3 616 MB live, 480 MB under the cap of a 16 GB CI
runner** (2.4 GB under the 6 GB default heap of the owner's 24 GB Mac).
CI's display job on `66a48bc8` ran 2 729 of 2 729 clean under that cap;
the margin is present but thin, and one more class-level static pack
would likely bring the cap-bound collections of `7e349fd5` back. #704
changes nothing here; the decision is #708's (section 9).

**The evidence heap** (`heap-704-contracts`, #676's commands, 2 GiB
cap, on the integrated tree): canonical **15.6 min**, peak **1 749 MB** live; portable **17.7 min**, peak **1 753 MB**; no full collection on either, the heap committed to its 2 GiB cap. The portable peak is 17 MB below 6.4.0's 1 770 MB and 162 MB below 6.1.0's 1 915 MB. The cap stays at 2 GiB, to be
measured again before the next evidence-heavy road.

The road from the owner's filing (2026-11-07 23:14 UTC) to the fourth
landing (2026-11-10 12:13 UTC) took two days and thirteen hours.

## 8. What did not change

- **Existing projections.** Stereographic, orthographic and gnomonic
  answer the four new `Projection` statements with the azimuthal
  defaults; `ProjectionStrategyTest`, `ProjectionCarriedThroughTest`,
  `CelestialGlobeRungTest` and `OneProjectionPerPageTest` hold the
  existing routes, and the evidence contracts prove every existing page
  byte-identical (section 7.2). Ordinary zoom never changes projection;
  the 180° rung is still the orthographic globe; no projection chooser
  exists.
- **Catalogue, services, packs and planets.** Untouched.
- **Modules.** The meridian, ecliptic, Solar System, Jovian and Saturn
  modules were not edited; they contribute to the whole sky through the
  same seam as to every page. On this page refuses the mode (R-14) and
  the meridian's altitude readout is not placed; everything else is
  carried.
- **Persistence.** No key was added for the mode (R-10); bookmark schema
  1 is kept, with one optional member written only for whole-sky
  bookmarks; every existing bookmark is byte for byte what it was.
- **Keyboard.** No shortcut added or moved.
- **The source files the road touched outside the new classes**, for the
  record: `app/{AppMenuBar,InspectorPanel,JUranometriaMain,ViewReport}`,
  `bookmark/{BookmarkDocument,SavedView}`,
  `chart/{ChartProjection,ChartViewState}`,
  `project/{CurveRun,DrawnPage,GreatCirclePage,PageRegion,PageWords,PlaneCurve,Projection,Projections,SkyFootprint,ViewportMapping}`,
  `render/{BelowHorizon,ChartRenderer,EquatorialGrid}`,
  `sheet/{SheetFileName,SheetMetadata}`, `sky/LocalSky`,
  `tool/EvidenceContractMain`,
  `ui/{AtlasToolbar,ChartControls,ChartViewController,PanInteraction,ReferenceInk,SceneAssembler,ZoomInteraction}`,
  `ui/bookmark/{AddBookmarkDialog,BookmarkCapture,BookmarkOpener,BookmarksPanel}`,
  `ui/language/PageText`, `ui/onthispage/{OnThisPageModule,OnThisPageTable}`,
  the two language files; new: `project/{HammerProjection,PlaneOutline}`,
  `ui/WholeSkySession`, `tool/{WholeSkyBoundary,WholeSkyComposition,WholeSkyStudyMain}`.

## 9. The packaged journey, the final qualification, and the owner's separate decisions

- **The packaged journey.** `PackagedAcceptanceMain` gains
  `wholeSkyJourney`, marker **`whole sky OK`**: the page as the
  application composes it - a production `ChartComponent` with the
  meridian module (horizon, zenith, Subdue), the ecliptic and every
  Solar System module attached as `JUranometriaMain` attaches them,
  driven by a real `ChartViewController` through `WholeSkySession` - at
  the Oslo evening of the as-built study. A fresh controller starts
  ordinary (R-10); the whole sky is entered from a 60° page through the
  one switch; the nine enabled bodies are drawn and each is spoken, the
  Sun *below the horizon*; the ruled title and sentence carry no radial
  claim. Saturn is selected a pixel off its mark and Open here
  (`recenter`) lands on the ordinary 60° chart centred there with Saturn
  on it and *Field 60.0°* in its title. A whole-sky bookmark is captured
  through `BookmarkCapture` (time and place kept whatever the boxes
  say), written as the one `mode` member in schema 1, read back byte for
  byte, and opened through the real `BookmarkOpener` after the chart's
  time and place were moved to another night in another place: the
  stored ones become the chart's, the whole sky is entered at the same
  central meridian with all nine bodies, and leaving restores the 60°
  page. Passed in the app image built on this tree
  (`j704-devcheck-app-image-2.log`: *9 of 9 enabled bodies drawn and
  spoken, 3 below the horizon, … central meridian λ 65.0°*, `PACKAGED
  ACCEPTANCE OK`, inventory 16, image 6.4.0), after one corrected pin
  (section 6). The journey imports no tool class: it composes the page
  as the application does, so what it proves is the application's route.
- **The regenerated evidence.** Both evidence contracts ran once on the
  integrated tree under the stated 2 GiB (`heap-704-contracts`):
  `EVIDENCE CONTRACTS OK` on both, canonical 15.6 min and portable 17.7 min, every registered generator reproducing its committed artifacts. No study, interface or portable artifact moved: the
  journey is application code, the handover is a document.
- **Existing-projection invariance, once more.** The same two contract
  runs reproduce every overview, globe and ladder page byte-identical on
  this tree, as every gated head of the road did (section 7.2).
- **The final qualification** of the integrated head, for the owner's
  approval - the gate-impact plan, by the diff from `d0dc9fc9`:
  `src/juranometria/app/PackagedAcceptanceMain.java` (an application
  class, in the jar and in the rendering closure), `CHANGELOG.md` (the
  *Unqualified candidate* tag of the #687 entry removed, since this is
  the qualification) and this document. **All seven gates once** - gate
  1 the full suite (the closure and change-route tests read the packaged
  main); gate 2 because the changed class is in the rendering closure,
  even though no photographer or artifact changes; gates 3 and 4 because
  the contract's interaction mode reads the closure; gates 5 and 6
  because the jar's bytes change and the new journey must run inside the
  image, with the inventory at 16 and the markers extended by
  **`whole sky OK`**; gate 7 provenance unchanged, tree unchanged - plus
  the site build once (the changelog feeds it). Then the PR (closing
  #704, keeping road #703 open), CI once and the pinned merge.
- **#708, recorded, not fixed.** The six static holders stand as filed;
  the present margin is section 7.3's. No required qualification of this
  road exposed a new heap failure after `db907424` (CI's display job
  green on `db907424`, `44325dea` and `66a48bc8`), so nothing of #708 is
  done inside #704, as instructed.

Then, as for 6.4.0, each of the following is a separate owner decision:

1. the version number and the `VERSION` bump with its changelog date and
   every version-fed regeneration;
2. the release pull request and its merge under #494;
3. the release rehearsal, which publishes nothing;
4. the local annotated tag;
5. the tag push, which publishes;
6. the public verification and the close-out of road #703 and the
   Sprint 59 milestone.

## 10. What 6.5.0 contains, and what it explicitly does not

**Contains:** the whole sky at one chart time as section 4.2 states it,
with its words in both languages, its bookmark form, its grid notation,
its refusals and its journey back to an ordinary chart; the
polar-boundary correction to the horizon's cardinal points; the bilingual
article.

**Does not:** a projection chooser or a second whole-sphere form; the
whole sky as a zoom rung or restored at startup; a fallback or
hysteresis for the central meridian near the ecliptic poles; On this
page or the altitude readout in the mode; a Milky Way; meteor showers,
Solar System time paths, a synchronized second chart or new catalogue
data; any repair or claim about #697; the six static holders of #708;
and none of the backlog below.

## 11. The backlog this road hands on, without starting it

- **Meteor showers** (#564): a radiant is a point on the sphere at a date;
  the whole sky is the one page that shows every radiant at once above
  and below the horizon. It would enter through its own module and the
  ordinary seam, drawn by the existing passes under the join rule like
  every other mark; nothing in this road anticipates it.
- **Solar System time paths**: a path is a sampled curve on the sphere;
  the sampled-curve route (`CurveRun.Polyline`, the join rule, seam
  refinement) is the drawing it would use, through its own contract.
  Nothing here draws one.
- **#697** (east-left after a globe-to-overview journey): untouched and
  unclaimed. The whole sky states its own orientation (R-12) and never
  repeats the ordinary east-left claim; #697 waits for its captured
  evidence.
- **#708** (release the suite's static planet packs and state the test
  heap): open, with the margin measured in section 7.3; the owner's
  choice between releasing the six holders and stating `-Xmx`.
- **Presentation improvements the owner noticed on another Mac**
  (2026-11-09, nothing wrong as such) are for future sprints and are not
  filed.
- Still open as before: #688, #618, #617, #613, #598, #594, #589, #566,
  #554, #553, #417, #387, #373, #370, #362, #356, #355, #351, #339, #327,
  #326, #324, #295, #293, #282, and the Milky Way sprint #189-#192 with
  PR #199, blocked on licensing.
