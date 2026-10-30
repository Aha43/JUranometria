# JUranometria 6.0.0 — the outer planets closed: handover to release

*Issue #661, closing the road to Neptune and its moons (#653) and, with
it, the stretch that began at 5.5.0. Written on main `5bc01c2b`, the
verified landing of the road's last pull request, before any `VERSION`,
tag or publication decision; the number 6.0.0 is this document's working
name for the next release, the owner's to confirm. Since 5.5.0
(`6958ce75`, published 2026-10-27) two roads have landed: the Uranus
road, #633-#640, handed over in `docs/reviews/uranus-road-handover.md`
and given no release of its own by the owner's decision of 2026-10-29;
and the Neptune road, #654-#660 with the owner-added #670, whose
numerical half is `docs/decisions/neptune-handover.md` (#657) and whose
chart contract is `docs/decisions/neptune-cartography.md` (#658). Three
reader articles joined in the same stretch: Mars (#639), Uranus (#660)
and Neptune (#670). This document covers all of it, with the release
boundary: the integrated evidence flush, the packaged journey on every
platform image, and the heap measurement deferred since #640.*

## 1. What a reader gets in 6.0.0

As the unreleased changelog states them, newest first:

- **The article "Neptune on the page"** (#670), in English and
  Norwegian (*Neptun på siden*), on the behaviour this release
  carries, written to say so without claiming the release - published
  through Pages on merge.
- **The article "Uranus on the page"** (#660), in English and Norwegian
  (*Uranus på siden*), on Uranus as released for 6.0.
- **Triton and Nereid on the chart** (#659 part 2, ruled on #658): with
  *Neptune on the chart* on, both moons from 2000 to 2100 as 3 px
  symbols at their own J2000 places, **offered only at the 1° field and
  the closer fields** and there drawn only where each stands clear of
  Neptune's mark and of the other moon - so a moon ten magnitudes
  fainter than any star the atlas draws never stands alone on a wide
  page; a moon behind, in transit or too close to tell apart not drawn
  and spoken; never selectable.
- **Neptune on the chart** (#659 part 1): *Neptune on the chart* in
  View ▸ Solar System after *Uranus on the chart*, *Show on chart* and
  *Centre on chart* in the Neptune group and dialog; the 6 px symbol on
  every field, full and round, no pole and no rings; selectable as an
  occurrence; the closer fields offered on Neptune; covered behind the
  Sun (2003) and drawn beside it (2026); the Solar System menu's spoken
  description made whole, naming every body the menu holds.
- **Neptune's cartography measured** (#658) and **Neptune handed to
  cartography** (#657): the study with its eleven rulings, and the
  settled numerical contract in one place.
- **Neptune, Triton and Nereid in the Solar System tables** (#656), the
  moons computed for 2000-2100 and Neptune shown alone before them.
- **Neptune, computed offline** (#655): a seventh pack, Neptune System,
  with DE442's Neptune barycentre over 1900-2100 and NEP098's Triton and
  Nereid over 2000-2100; the pole with its periodic term; and the system
  contract (#654).
- **The article "Mars on the page"** (#639), in English and Norwegian
  (*Mars på siden*), on released 5.5 behaviour.
- **Uranus's moons on the chart** (#638 part 2) and **Uranus on the
  chart** (#638 part 1, ruled on #637): the 6 px symbol on every field,
  the rings table-only, the five moons drawn only where they stand
  clear, on every field.
- **Uranus's cartography measured** (#637), **Uranus handed to
  cartography** (#636), **Uranus and its five moons in the tables**
  (#635), **Uranus, computed offline** (#634) and **the system
  contract** (#633): a sixth pack, Uranus System, with DE442's Uranus
  barycentre and URA182.21's five moons.

**Recorded by this handover.** The Unreleased changelog had no entry
for #658: its pull request changed no `CHANGELOG.md`. It now has its
entry, in date order, written from its decision record; no other entry
changed.

Not a reader's change: the evidence contracts' heap is 2 GiB since
#635, and what the process retains at that limit is measured in
section 8 for the first time.

## 2. Every ruling of the stretch, as recorded

| ruling | where it stands | in short |
|---|---|---|
| #633, U-1 to U-13 (2026-10-27) | `uranus-system.md`, *The ruling* | DE442's barycentre for Uranus (U-1a), the barycentre for the centre, the IAU pole with no periodic terms, no phase words, all five moons 2000-2100 (U-6a), Jupiter's six states, no ring rows (U-8), Saturn's and Mars's targets |
| #634's distance target (2026-10-28) | `uranus-system.md`, *#634* | "keep the barycentre and amend the exact-era distance target to 50 km" - the centre-versus-barycentre consequence, measured late and corrected |
| #635's acceptance and letters (2026-10-28) | `uranus-system.md`, *#635* | accepted untested; *Uranus...* lettered N and U; the evidence contracts' heap raised to 2 GiB after gate 4 ran out of 1 GiB |
| #636's two corrections (2026-10-28) | `uranus-handover.md` | the published 5.5.0 baseline; the registration and the heap measurement deferred and not yet filed |
| #637, R-1 to R-10 (2026-10-28) | `uranus-cartography.md`, *The ruling* | the 6 px mark always; unphased, round, no colour; no pole; rings table-only; Mars's clearance rule for the moons; vocabulary A; the closer fields; Uranus selectable, its moons not; Uranus first, then the moons |
| the gate plan (2026-10-28) | standing rule | before any gate run, a stated plan of which gates and why, approved with the checkpoint; gates 1 and 7 never skipped; doubt means all seven |
| no Uranus release (2026-10-29) | #632, #640 | the Neptune road next, 6.0.0 after Neptune, 7.0.0 for improvement sprints, Pluto later |
| #639's texts (2026-10-29) | #639 | "Accept both texts" |
| #654, N-1 to N-14 (2026-10-29) | `neptune-system.md`, *The ruling* | "Rule N-1 to N-14 as proposed": DE442's barycentre for Neptune (N-1a), the barycentre for the centre with the exact-era distance target at **100 km** and the builder refusing above **80 km** (N-2a), the IAU pole **with its periodic term** (N-4), no phase words (N-5), Triton and Nereid 2000-2100 with the inner moons excluded on measurement (N-6a), Jupiter's six states (N-7), **no ring rows, ink or authority** (N-8), the moon names unchanged in Norwegian (N-9), the position angle from the offsets (N-10) |
| the pre-flight (2026-10-29) | standing rule | after two stopped runs on one head: raw-source parity, exact replays of gates 6 and 7 and a parity audit against the copied landing before every gate request - "Ok go on" |
| #655's acceptance (2026-10-29) | #655 | "Accept #655, app closed, run all seven gates" |
| #656's verdict (2026-10-29) | `neptune-handover.md`, §1.8 | "got no reason to question Neptune tables, looks good"; the empty-until-Compute dialog noted for the 7.0.0 road |
| #657's acceptance (2026-10-30) | #657 | the handover accepted as written, gates 1 and 7 approved with 2-6 recorded as skipped |
| #658, R-1 to R-11 with R-6a (2026-10-30) | `neptune-cartography.md`, *The ruling* | "Rule R-1 through R-11 as proposed, choosing R-6a for Nereid: Neptune's moons are offered only at 1° and the closer fields, and each mark is still subject independently to the inherited clearance rule. Nereid remains a charted moon rather than table-only... Draw every moon at its own J2000 chart position" |
| #659 part 1 and part 2 (2026-10-30) | `neptune-cartography.md`, *as built* | "Accept part 1, app closed, run all seven gates"; "Accept part 2, app closed, run all seven gates" |
| #660's texts (2026-10-30) | #660 | "Accept both texts, run gates 1 and 7 with the site build" |
| the road amended (2026-10-30) | #653, #670 | the Neptune article added to the Neptune road before #661, "modelled on the Uranus article and held to Neptune's qualified reports and figures", describing 6.0.0 behaviour without claiming the release |
| #670's texts (2026-10-30) | #670 | "Accept both texts, run gates 1 and 7 with the site build" |

## 3. The owner's packaged verdicts

- **#635** - candidate `8a1f40e0`: *"Don't got chance to test. Chance is
  ok, always been. Leave letter. Go on."*
- **#637** - *"I agree with the recommendations. Rule R-1 through R-10
  exactly as proposed"*.
- **#638 part 1** - candidate `5ddbf42e`, *"Nice it is spot on the
  ecliptic NOW"*, then *"Accept part 1, run the gates and CI"*; **part
  2** - candidate `4a97a521`: *"Accept part 2, run the gates and CI"*.
- **#639** - *"Accept both texts, run the gates and CI"*.
- **#655** - *"Accept #655, app closed, run all seven gates"*.
- **#656** - candidate `6a5f3037`, opened three times in the packaged
  application: *"got no reason to question Neptune tables, looks good,
  great work again"*; the note that a planet dialog opens with no data
  until Compute, *"Pure stuff for gui improvements"*.
- **#659 part 1** - candidate `3fcccc4e`: *"Accept part 1, app closed,
  run all seven gates"*; **part 2** - candidate `778742bd`: *"Accept
  part 2, app closed, run all seven gates, app closed"*.
- **#660** and **#670** - *"Accept both texts, run gates 1 and 7 with
  the site build"*, each after reading both texts.

## 4. Packs, licensing and accuracy

**Two new packs, five unchanged.** Uranus System (`387,072` +
`38,055,936` bytes, DE442's Uranus barycentre 1900-2100 and URA182.21's
five moons 2000-2100) and Neptune System (`416,768` + `50,302,976`
bytes, DE442's Neptune barycentre 1900-2100 and NEP098's Triton and
Nereid 2000-2100). The Solar System, Jovian, planets (version 3),
Saturn System and Mars System packs are byte-identical to 5.5.0's:
each new builder refuses to write unless every released kernel carries
its released digest, and `UranusSystemPackTest` and
`NeptuneSystemPackTest` hold them. Each new kernel carries NAIF's
required comment area for a modified kernel; no ephemeris byte differs
from its source. Together the two packs add **89.2 MB** to every image.

**Licensing.** Two notices under NAIF's rules for modified kernels
(`NOTICE-uranus-system.md`, `NOTICE-neptune-system.md`), carried in
About in both languages, in `LICENSING.md` and the summary, in the
image's inventory - whose computed count now reads **16** - and in the
Makefile's `dist` list and `verify-dist.sh`. Horizons responses are
committed under its public terms (59 requests for Uranus, 39 for
Neptune; the geocentric matrices retained outside Git); Espenak's
pages stay outside Git with only the quoted rows committed. Raw kernels
live in the main checkout's ignored `imports/raw/`.

**Accuracy, where the clock correction is exact (1972 to 2027-06-28).**
Uranus's place agrees with Horizons to 0.0033″ and its distance to
43.529 km under the 50 km target; its moons' offsets to 0.0034″, their
states on 5,577 of 5,579 rows for Miranda and Ariel and 5,579 of 5,579
for Titania and Oberon. Neptune's place agrees to 0.0035″ and its
distance to 93.426 km under the 100 km target; its moons' offsets to
0.0038″ and position angles to 0.0070°, states 5,578 of 5,579 for
Triton and 5,579 of 5,579 for Nereid. After the record's expiry the
places may differ by 0.3194″ (Uranus) and 0.2078″ (Neptune) by 2100 -
the clock, not the planet. The packaged acceptance matches its Horizons
row within 0.0008″ (Uranus) and 0.0032″ (Neptune). Both accuracy
reports (`docs/studies/uranus-system/service-accuracy.md`,
`docs/studies/neptune-system/service-accuracy.md`) are reproduced by
the evidence contract since #655, closing the registration gap #640
named.

## 5. Every stopped, failed, superseded or incomplete run

Every record is kept under `~/juranometria-runs/runs/`, unchanged;
nothing was rerun into green.

| run | what happened | how it was continued |
|---|---|---|
| `gates-j633-6d5526c9` | the proposal head; gates 1 and 2 OK, **stopped during gate 3 on the owner's word** (`ABORTED.md`) | the gate-economy rule; the ruled head `2bda2543` all OK |
| the Uranus raw kernel | removing the #633 worktree deleted `ura184_part-3.bsp`, kept in its `imports/raw` | re-fetched with approval into the main checkout; checksums matched |
| `gates-j635-c936de1b` | **stopped at gate 4**: the portable contract ran out of its 1 GiB heap inside the label study (`GATE4-FAILED.md`) | not retried; the owner chose 2 GiB; `02038686` all OK |
| `gates-j638-b1eedee3-run1-pointer` | **stopped at gate 1**: the pointer rested over the toolbar photographs while the owner tried the candidate | the owner ordered a rerun with the candidate quit and the mouse still |
| `gates-j638-b1eedee3` | gate 1 **INCOMPLETE**: the export dialog's capture refused 332 → 326, the known kind | all seven precedent conditions in `INCOMPLETE.md`; gates 2-7 once; CI's display run 2 486/2 486 |
| `gates-j654-b9530be6` | **stopped at gate 1**: the committed test-evidence inventory lacked the study's 37 fixtures and 5 reports (`GATE1-STOPPED.md`) | regenerated; `30a2d120` all OK |
| `gates-j655-092dff97-run1` | gates 1-2 OK, **stopped at gate 3**: the worktree held only four of the main checkout's raw source directories (`GATE3-STOPPED.md`) | an environment omission; the rest copied in; rerun from gate 1 on the owner's word |
| `gates-j655-092dff97-run2` | gates 1-5 OK, **stopped at gate 6**: the Makefile's `dist` list lacked `NOTICE-neptune-system.md` (`GATE6-STOPPED.md`) | one Makefile line; `0321568f` all OK; the pre-flight instituted |
| `gates-j657-8207efd0-plan` | gate 1 **INCOMPLETE**: `PlaceAndTimeSheetMain` refused 420 → 324, the known kind; the pointer had not been parked for a planned run (`INCOMPLETE.md`) | all seven precedent conditions; gate 7 once; CI's display run executed the refused test with 0 failed |
| `gates-j659-6b4a80bd-stopped-gate3` | gates 1-2 OK, **stopped at gate 3**: `control-explanations/measurements.md` still quoted the Solar System menu's old spoken description (`GATE3-STOPPED.md`) | regenerated; `cbc3fa71` all OK |
| development runs of #634, #635 and #638 part 1 | the language ledger at 160, stale About sheets and test-evidence measurements, the distance cell against its #633 target; the hand-off count 1105 → 1106; the preference-door pin | each fixed before gating (`uranus-road-handover.md`, §4) |
| development runs of #655, #659 and #670 | the About sheets, licensing inventory and companion order; the hand-off count 1106 → 1107 and the door pin "twenty"; `UranusArticleTest`'s and `NeptuneArticleTest`'s citations pointed at the report that holds them | each fixed before gating |
| `landing-671.md` | the merge-identity check was first run with a short `--accepted-head` and before the landing runs had finished, and said "not proven" | rerun with the full head after the runs: proven 1-4; the record keeps a note of the first |
| the #661 heap measurement | the first invocation built the measured command from `make -n` before the classes were built and captured the compiler line | the classes built, the command re-read (section 8) |

Gate 1's aborts were 12 on every Neptune head and 12, 28 or 49 on
Uranus's; every record's list of unexplained aborts is empty - each is
the desktop-focus family.

Lessons the next road inherits:

- **raw downloads outlive worktrees**: fetch into the main checkout's
  `imports/raw/<system>/` and copy **all of it** into a worktree;
- **a study measures every observable a ruling touches** - Uranus's
  barycentre ruling was measured in arcseconds and corrected in
  kilometres; Neptune's was measured in both before it was ruled;
- **a new committed study file moves the test-evidence inventory**;
  regenerate `make test-evidence-study` before committing;
- **a changed interface text is followed by a search of the committed
  reports for the old words**, not only the photographers that carry it;
- **the pre-flight before every gate request**: raw parity, exact
  replays of gates 6 and 7, the parity audit against the copied landing
  - it found the one missing Makefile line on `092dff97`, and every
  later head passed on its first run;
- **park the pointer before every run**, planned runs included, and
  quit the candidate;
- **a merge-identity check waits for the landing runs** and takes the
  full accepted head.

## 6. Routes, evidence movement and performance

### 6.1 The route every PR took

Each PR ran CI once after its local gates; each merge was pinned to its
exact head and landed under #494, structurally verified on all four
points.

| PR | issue | head → merge | PR `test` run | landing `test` run |
|---|---|---|---|---|
| #644 | #633 | `2bda2543` → `cfd5e0b1` | 40579969801 | 40592825059 |
| #645 | #634 | `4ca9ebb0` → `6e194e16` | 40614822836 | 40623535677 |
| #646 | #635 | `02038686` → `6e45f155` | 40667748467 | 40680850352 |
| #647 | #636 | `1498791a` → `1308d309` | 40700819521 | 40727036580 |
| #648 | #637 | `83e163ef` → `c5051add` | 40750195183 | 40758814558 |
| #649 | #638 part 1 | `b1eedee3` → `8488f87f` | 40784622038 | 40819826592 |
| #650 | #638 part 2 | `be8b5589` → `782ac644` | 40852765403 | 40858994317 |
| #651 | #639 | `8a430154` → `f2905f61` | 40870104597 | 40878637580; Pages green |
| #652 | #640 | `b694a110` → `ed01b714` | narrow route | green |
| #662 | #654 | `30a2d120` → `69a50e13` | 40907075085 | 40918106186 |
| #663 | #655 | `0321568f` → `3f2b81c1` | 40959104500 | 40965989432 |
| #664 | #656 | `6a5f3037` → `9013663e` | 40975186803 | 40979360056 |
| #665 | #657 | `8207efd0` → `6030c857` | 40981501735 | 40984112682 |
| #666 | #658 | `d967b4d8` → `cd74c196` | 40992016902 | 41027225726 |
| #667 | #659 part 1 | `cbc3fa71` → `52c359b4` | 41051363769 | 41058593534 |
| #668 | #659 part 2 | `a6fc49d7` → `a74e5f23` | 41070224342 | 41080289640 |
| #669 | #660 | `5d6097a7` → `42a28573` | 41111230043 | 41116642201; Pages green |
| #671 | #670 | `f7547b7b` → `5bc01c2b` | 41121997432 | 41142720894; Pages green |

Every landed PR's display job ran the whole suite with 0 failed, 0
aborted and 0 skipped. The suite grew from **2 432** tests at 5.5.0 to
**2 565** at `5bc01c2b`; as gate 1 counted it head by head: 2 432,
2 465, 2 479, 2 479, 2 482, 2 486, 2 492, 2 496 (Uranus), 2 496, 2 528,
2 542, 2 542, 2 545, 2 550, 2 557, 2 561 and 2 565 (Neptune).

### 6.2 What evidence moved

From `6958ce75` to `5bc01c2b` the promoted artifacts in
`docs/studies/PROVENANCE.md` went from **848 to 916**. Under
`docs/studies`, **199 files** moved - 177 added and 22 modified, none
deleted; the whole change is 359 files, +154 358 / −119 lines. By
directory: `uranus-system` 60 (new), `neptune-system` 43 (new),
`uranus-cartography` 21 (new), `neptune-cartography` 20 (new),
`neptune-on-the-chart` 18 (new), `uranus-on-the-chart` 15 (new),
`interface-language` 17 (the About, companion and menu sheets and
strings), `closer-fields` 2 (the wording now naming Uranus and
Neptune), `control-explanations` 1 (the Solar System menu's whole
spoken description), and one file each of `test-evidence` and the
provenance record. Six decision records are new (`uranus-system`,
`uranus-handover`, `uranus-cartography`, `neptune-system`,
`neptune-handover`, `neptune-cartography`), one amended
(`test-evidence`, 1105 → 1107 hand-offs), one review record is new
(`uranus-road-handover`), and six article texts with 34 figures joined
`docs/articles` (Mars 10, Uranus 11, Neptune 13).

**No released chart output moved.** Uranus's and Neptune's ink and
their moons are off until chosen; every ladder, sheet, regional,
overview, globe and printable page reproduced on every head under
gates 3 and 4 where they ran, and on this head under the integrated
flush (section 8).

The language ledger stands at **159** under its cap of 160; the
test-evidence measurements are quoted at **1107 explicit hand-offs**;
the suite is **2 565 tests**; the packaged licensing inventory is **16
resources**, counted from its own list.

### 6.3 Performance, measured

Local gates on this Mac, from each record's start to its verdict. A
full seven-gate run: **74.9 and 75.5 min** at the 1 GiB evidence heap
(`2bda2543`, `4ca9ebb0`) and **54.9-56.5 min** at 2 GiB (`02038686`
55.1, `1498791a` 54.9, `83e163ef` 55.6, `be8b5589` 55.0, `30a2d120`
55.1, `0321568f` 55.5, `6a5f3037` 55.7, `d967b4d8` 56.5, `cbc3fa71`
55.3, `a6fc49d7` 55.6); gate 4 alone fell from 38.6-38.8 min to
19.7-20.5 under the 2 GiB heap, gate 3 holds at 18.4-18.6 and gate 1
at 11.0-12.4. The planned runs under the gate-plan rule - gates 1 and 7
with or without the site build, for a head that changes prose, copied
figures and a test - took **11.9-12.8 min** (`8a430154` 11.9,
`b694a110` 11.9, `8207efd0` 12.8, `5d6097a7` 12.0, `f7547b7b` 12.0):
a fifth of a full run, five times in the stretch. CI's `test`
workflow on a PR, from creation to completion: **22-79 min**
(`2bda2543` 79, `4ca9ebb0` 55, `02038686` 51, `1498791a` 48,
`83e163ef` 44, `b1eedee3` 48, `be8b5589` 42, `8a430154` 49,
`30a2d120` 49, `0321568f` 49, `6a5f3037` 37, `8207efd0` 22,
`d967b4d8` 48, `cbc3fa71` 48, `a6fc49d7` 48, `5d6097a7` 33,
`f7547b7b` 48).

## 7. Existing-body invariance through the whole stretch

- **The packs and services.** Section 4: five released packs
  byte-identical, two new. `UranusService`, `UranusSystemPack`,
  `NeptuneService` and `NeptuneSystemPack` are new; no released service
  file changed. The Sun, Moon, Mercury, Venus, Mars, Jupiter and Saturn
  reference tests ran unchanged on every head, and Uranus's on every
  Neptune head.
- **The source outside the new planets.** Of the 59 source files
  changed, those not named for Uranus or Neptune are hosting and
  distribution: `AboutDialog`, `AppMenuBar`, `JUranometriaMain`,
  `StartupStores`, `PackagedAcceptanceMain`, `SolarSystemModule`,
  `SolarSystemSection`, `CentreOnChart`, `ChartComponent`,
  `ChartControls` and `ChartViewController` (the closer fields offered
  on the new bodies), `CompanionSheetMain`, `MenuSheetMain`,
  `CloserFieldsStudyMain`, `EvidenceContractMain`, the Makefile and
  the build, dist and articles scripts - and one rendering file,
  **`ReferenceInk`**, which gained `FLOOR_ONLY_MOONS` for R-6a: a round
  system whose primary is Neptune is not formed on a page wider than
  the normal minimum field. Mars's and Uranus's moons are unchanged by
  it, held by their ink tests and by gates 3 and 4 on every head.
- **Released chart output.** None moved (section 6.2). The interface
  sheets moved for their words and rows alone: the About notices, the
  collapsed Uranus and Neptune groups, the menu items *Uranus...*,
  *Uranus on the chart*, *Neptune...* and *Neptune on the chart*, the
  closer-field wording, and the Solar System menu's spoken description
  made whole.

## 8. The release rehearsal, the heap, and the owner's separate decisions

As for 5.4.0 and 5.5.0, the rehearsal is what the seven gates and the
CI workflows already do, recorded rather than claimed, on this
handover's head.

- **The integrated evidence flush** is gate 3 (`make
  evidence-contracts`) and gate 4 (`evidence-contracts-ci`) on this
  head: every study of the stretch - Uranus's and Neptune's system,
  cartography and on-the-chart studies, both accuracy reports, the
  interface sheets and the test-evidence measurements - regenerated on
  the integrated main and held to its committed bytes.
- **The packaged release journey on every platform image** is the
  `app-image` workflow's four images and their cross-architecture smoke
  test on this handover's trial merge, whose code is main
  `5bc01c2b`'s, each running the packaged acceptance with *uranus table
  OK*, *uranus on the chart OK*, *uranus moons on the chart OK*,
  *uranus system pack OK*, *neptune table OK*, *neptune on the chart
  OK*, *neptune moons on the chart OK* and *neptune system pack OK*
  among its journeys; locally, gate 5 runs the same acceptance on this
  Mac on this head with the inventory at 16.
- **The dist archive built twice and verified** is gate 6.
- **The articles reachable** is the Pages deployment of `5bc01c2b`:
  all six planet article pages answer 200.
- **What the evidence-contract process retains at 2 GiB**, deferred
  from #636 and #640, measured once on main `5bc01c2b`'s code in a
  separate worktree, with the exact `make` command of each route run
  under `-Xlog:gc*` and nothing else changed (record
  `~/juranometria-runs/runs/heap-661-5bc01c2b/`): the canonical route
  (`make evidence-contracts`) finished in **18.7 min** with **3,748**
  collections and **830** concurrent marking cycles, no Full GC, under
  **3.9 s** of total pause (the longest 22 ms); the heap was committed
  to its **2,048 MB** cap from 3.7 min on, and at the peak the process
  kept between **1,089 MB** (the least the heap fell to within the
  busiest marking cycle) and **1,541 MB** (the most left after any
  collection) live. The portable route (`evidence-contracts-ci`)
  finished in **20.0 min** with **5,156** collections and **1,205**
  cycles, no Full GC, **4.8 s** of pause (the longest 25 ms); committed
  to **2,048 MB** from 5.3 min on, and at the peak between **1,195 MB**
  and **1,658 MB** live. Reading: both routes run against the cap for
  most of their length, marking concurrently about once a second, and
  retain 1.1-1.7 GiB at their peak - which is why 1 GiB ran out in #635
  and why 2 GiB holds, with 400-500 MB of headroom. Whether a larger
  heap would shorten the run is not measured; the heap stays at 2 GiB.

The results go on #661 and under `~/juranometria-runs/runs/`.

Then, as 5.5.0 was done: no `VERSION` change, tag or publication in
this issue. Each of the following is a separate owner decision, asked
for in turn and never inferred from the one before:

1. the version number and the `VERSION` bump with its changelog date
   and every version-fed regeneration - found by the version strings in
   the committed artifacts, never by copying 5.5.0's list;
2. the release pull request, and its merge under #494;
3. the release workflow's manual-dispatch rehearsal, which publishes
   nothing;
4. the local annotated tag;
5. the tag push, which publishes - `release.yml` has no gate;
6. the public verification, and the close-out of #653 and #661.

## 9. What 6.0.0 contains, and what it explicitly does not

**Contains:** Uranus and Neptune computed offline from DE442's
barycentres, tabled and on the chart as 6 px symbols; Uranus's five
major moons and Neptune's Triton and Nereid, 2000-2100, drawn where they
stand clear - Neptune's only at 1° and closer; the closer fields for
both; the Mars, Uranus and Neptune articles (live on Pages); the whole
Solar System menu spoken.

**Does not:** Pluto or any wider outer Solar System body; the moons
before 2000 (the 1900-2100 excerpts were measured and not ruled in);
Uranus's or Neptune's inner and irregular moons; a magnitude for any
body; rings in ink (Uranus's in the table, Neptune's not held at all);
a phased, coloured or textured outer planet, or its pole on the chart;
shadows of any moon on its planet; event search, paths over a range
(#553) or the configuration diagram (#554); the article library in the
application (#598); gate plans from declared change impact (#613); the
verifier's recognition of dispatched runs (#594); a repair of the
capture-refusal family (#362) or of #589.

## 10. Still open after 6.0.0, and the road to 7.0.0

#613, #617, #618, #598, #594, #589, #571, #566, #564, #563, #554, #553,
#497, #417, #387, #373, #370, #362, #356, #355, #351, #339, #327, #326,
#324, #295, #293, #282, and the Milky Way sprint #189-#192 with PR
#199, blocked on licensing.

**The road to 7.0.0** is planned around general GUI, chart, catalogue
and workflow improvements, not a planet. The Neptune article, which was
to open it, was brought forward into the Neptune road by the owner's
amendment of 2026-10-30 and is published; the road therefore begins
with the improvements. Filed and ready, among others: the owner's note
from #656 that a planet dialog opens with no data until Compute - the
"opening computes nothing" rule showing through; #613, planning a gate
run from the declared change impact, which the five planned runs of
this stretch (section 6.3) argue for; #617, measured angles and
telescope fields; #618, how JPL ephemerides become an offline atlas -
the cross-planet article deferred since Saturn, now with seven packs to
tell of; #553, paths over a time range; #554, the configuration
diagram; #598, the article library in the application. Pluto and the
wider outer Solar System wait for a later second Solar System sprint.
