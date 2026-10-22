# JUranometria 5.4.0 — the road closed: Sprints 51–53 handover

*Issue #581, closing the road to 5.4.0 (#573). Written on main
`582f6725`, the verified landing of the road's last pull request,
before any `VERSION`, tag or publication decision. Since 5.3.0
(`f19395b0`, 2026-10-17) three sprints have landed: Sprint 51, *Make the
chart state explicit*, handed over in
`docs/reviews/sprint-51-handover.md`; Sprint 52, *Know ringed Saturn* -
Part I of this road - handed over in `docs/decisions/saturn-handover.md`;
and Sprint 53, *Saturn and its moons on the chart*, which this document
covers, with the whole road's release boundary.*

## 1. What a reader gets in 5.4.0

As the unreleased changelog states them, newest first:

- **The article "Mercury, never far from the Sun"** (#580), in English
  and Norwegian (*Merkur, aldri langt fra Solen*), on released 5.3
  behaviour - published through Pages on merge, not a release.
- **Saturn on the chart** (#579, ruled on #578): *Saturn on the chart*
  in View ▸ Solar System after Jupiter's and in the Controller's Saturn
  group with Centre on chart; the globe and rings at their true size
  once the rings are 6 px across (1° and the closer fields), otherwise
  the 6 px symbol; the rings' near half over the globe, a line when
  edge-on, the Cassini gap only where it is a pixel across, the unlit
  face dimmed; Saturn joins the closer fields; the seven ruled moons as
  3 px symbols decided one by one against the drawn figure, with their
  states, their names attempted one by one, never selectable.
- **Saturn's cartography measured** (#578): the study and the eight
  rulings in `docs/decisions/saturn-cartography.md`.
- Part I: **Saturn handed to cartography** (#577); **Saturn, its rings
  and seven moons in the Solar System tables** (#576); **Saturn,
  computed offline** (#575) - a fourth pack, two SAT441 excerpts, the
  IAU 2015 constants and the PDS ring radii.
- Sprint 51: **all star identities where they fit** (#558, ruled on
  #557); **emphasis survives zooming and panning** (#537); **the chart
  says when it is drawn for** (#259, ruled on #556) - the Place and Time
  box, *Update to now*, the tables' agreement line.

Not a reader's change, but in the tree: `docs/development.md` now
states how a change is qualified - the seven gates, the capture-refusal
precedent and the prose route (#599).

## 2. Every Sprint 53 ruling, as recorded

| ruling | where it stands | in short |
|---|---|---|
| #578, rulings 1-8 (2026-10-21) | `saturn-cartography.md`, *The ruling* | the larger of the true figure and the 6 px mark by the rings' major axis, a discrete switch; the two annuli with the near half over the globe, outline and edge-on thresholds, the dimmed unlit face, the conditional Cassini gap, no shadows; the moons under Jupiter's separability rule with the drawn figure as the primary's extent, labels attempted for every drawn moon and refusals accepted; vocabulary A; `saturn` and `saturn.<moon>`, the moons unselectable, Centre on chart at 1°; Saturn eligible for 0.5° and 0.25°; the inherited contracts; two wide-route PRs each with a packaged checkpoint |
| #578's landing (2026-10-21) | #593, #594 | the macOS runner cancelled `dist` on `c4f6e209`; the owner ruled one manual dispatch (run 39633008012, green) as the product verdict and the landing **unproved on point 4** of #494; #594 filed for the verifier to recognise a dispatched run on an identical tree |
| #579 PR 1's checkpoint finding (2026-10-21) | #579; `saturn-cartography.md`, *as built*, ruling 2 | two dark dots at twelve o'clock on the open rings: the Cassini gap cut all round after a one-place decision; *fix it now in PR 1, then requalify* - the cut confined to the arcs where the gap is a pixel across |
| #580's review (2026-10-22) | #580 | Codex's four wording requests endorsed by the owner: the greatest-transit instants of all 28 transits; *det som et teleskop viser*, *en håndsbredde*, *hver tredje dag* |
| the prose route (2026-10-22) | `docs/development.md`, *Qualifying a change*; #599 | a prose-only correction to an already qualified article takes the full suite, the article site build, a clean-tree check and CI once, decided by the diff against an allowlist that excludes images and the site builder; everything else the seven gates; #597 not redone |
| #589 (2026-10-20) | #589 | the Place and Time preferences exception on CI's display stays record-only |

Sprint 52's rulings (#574's ten, the #575 checkpoint's ten, #584's
scope, the #590 and #588 repairs) are indexed in
`saturn-handover.md`; Sprint 51's (#556's ten, #537's option 1, #557's
six, #571) in `sprint-51-handover.md`.

## 3. The owner's packaged verdicts of the sprint

- **#578** - the study's mock-ups seen in the conversation: *"Looks
  good!"*; all eight proposals accepted as the rulings above.
- **#579, the figure** - first candidate `73a69e65`: *"Looked at
  saturn at current time, yday first time for loooong time looked at
  saturn in a telescope, look right. 0.25 rings fine. Well done."*,
  then the two dots at twelve o'clock; second candidate `eb69beb7`,
  with the gap confined: accepted and merged.
- **#579, the moons** - candidate `d9be8b52` (the application
  identical to `ad3ffe69`'s, a study-only commit between): *the seven
  moons, their states, symbols, suppression and independent labels look
  right*; merged.
- **#580** - both texts accepted after the four review changes; merged.
- Sprint 52's **#576** (`09d4537b`) and Sprint 51's **#259**, **#537**
  and **#558** verdicts are quoted in their handovers.

## 4. Every stopped, failed, superseded or incomplete run of Sprint 53

Every record is kept under `~/juranometria-runs/runs/`, unchanged;
nothing was rerun into green.

| run | what happened | how it was continued |
|---|---|---|
| `gates-j578-11f8bce3` | all OK - the proposal head | superseded by the ruled head `c4f6e209` (all OK); its `dist` on the PR cancelled by the macOS runner, one owner-ruled dispatch green, landed unproved on point 4 (section 2) |
| `pre-check-579-4b125d43` (development run) | five pin tests of the change (a ledger row, the closer wording in both languages, the preference doors, the module vocabulary) | fixed on `76f8a63c` |
| `pre-check-579-76f8a63c` (development run) | `ExportSheetDialogSheetMain` refused its capture (333×223 → 326×206), the #362 export-dialog kind | kept with a note; the gates started from gate 1 |
| `gates-j579-76f8a63c` | gates 1-6 OK; **stopped at gate 7**: `closer-fields/platform.md` rewritten `headless false → true` - `make closer-fields-study` ran without the headless flag (#323's kind) | the record and the target fixed on `73a69e65` (all OK, CI green) |
| `gates-j579-73a69e65` | all OK, CI green; the checkpoint found the Cassini-gap dots | superseded by `eb69beb7` |
| `gates-j579-eb69beb7` | gate 1 INCOMPLETE: the export-dialog refusal in two generator runs (`ExportCompanionTest`, `InterfaceEvidenceGateTest.everyRegisteredGeneratorProducesWhatItClaims`) | all seven conditions in `INCOMPLETE.md`; gates 2-7 once (`-from2`); CI's display run executed both tests clean; merged `3c0f82d4` |
| `pre-check-579m-a1067a97` (development run) | two pins: three ledger rows for the state-key fragments, the hand-off count 1 079 | fixed on `ad3ffe69` |
| `gates-j579m-ad3ffe69` | all OK; **CI's evidence job failed** (run 39726682360): `saturn-on-the-chart/measurements.md` did not reproduce on Linux - the moons' label outcomes, a matter of fonts, written into the deterministic report | not a flake, not retried; moved to `platform.md` on `d9be8b52` (all OK, CI green); merged `a7633eb4` |
| `gates-j580-d818df80` | all OK, CI green - the article's first head | superseded by the review head `1cbbfd50` |
| `gates-j580-1cbbfd50` | gate 1 OK; gate 2 INCOMPLETE: the export-dialog refusal inside the twice test | the seven conditions, condition 3 by gate 1 on the same head; gates 3-7 once (`-from3`); CI's display run executed the test clean; merged `3c237d6e` |
| `gates-jgp-1ac58afe` | all OK, CI green (narrow) | merged `582f6725` |

Gate 1's aborted count ranged from 12 to 49 across the qualified heads;
every record's list of unexplained aborts is empty - each abort is the
desktop-focus family. The export-dialog refusal (#362) was met on three
gated heads and two development runs of the sprint, and the prose route
exists partly so that it is not met for changes it cannot fault.

Lessons the next milestone inherits, now in the standing rules:

- a study report keeps what fonts decide - which names were placed, how
  many - in `platform.md`; the deterministic report keeps only what is
  decided geometrically (CI's Linux evidence job is the proof the local
  gate 4 cannot give);
- a study's `make` target runs headless, as the contract runs it, or
  its platform record lies about the route that made it;
- a rule phrased *only where* is evaluated around the figure, not once;
- the `interface-gate-failures` directory a gate chain copies can be a
  preceding development run's leftover: date it before reading it as a
  gate result;
- a prose-only correction takes the prose route, decided by the diff.

## 5. Routes, evidence movement and performance

### 5.1 The route every PR took

Each PR ran CI once after all seven local gates once (or, for a refusal
under the precedent, the remaining gates once); each merge was pinned
to its exact head and landed under #494, structurally verified, with
the suites skipped - #593 excepted, as section 2 records.

| PR | issue | head → merge | PR `test` run | landing `test` run |
|---|---|---|---|---|
| #593 | #578 | `c4f6e209` → `ebc3c6c5` | 39625705649 (`dist` cancelled; dispatch 39633008012) | 39634342974, the full suites run |
| #595 | #579 PR 1 | `eb69beb7` → `3c0f82d4` | 39683921635 | 39693033069 |
| #596 | #579 PR 2 | `d9be8b52` → `a7633eb4` | 39736175504 | 39742108298 |
| #597 | #580 | `1cbbfd50` → `3c237d6e` | 39766843200 | 39772813424; Pages 39772813302 |
| #609 | #599 | `1ac58afe` → `582f6725` | 39783817961 (narrow) | 39787984373 |

Every landed PR's display job ran the whole suite with 0 failed, 0
aborted and 0 skipped (2 295, 2 314, 2 323, 2 327 and 2 327 tests).
Sprint 52's six PRs (#582–#592) and Sprint 51's five (#562–#572) are
tabled in their handovers; the 5.3.0 release PR #561 and the 5.3.0
handover #560 complete the chain from `f19395b0`.

### 5.2 What evidence moved

From `f19395b0` to `582f6725` the promoted artifacts in
`docs/studies/PROVENANCE.md` went from **648 to 774**. Under
`docs/studies`, **387 files** moved - 241 added, 146 modified - of the
1 159 at the tag; the whole change is 555 files, +185 207 / −633 lines.
By directory: `saturn-system` 103 (new), `interface-language` 83,
`saturn-cartography` 38 (new), `saturn-on-the-chart` 26 (new),
`star-identity-density` 25 (new), `solar-system-controls` 24,
`chart-instant` 19 (new), `chart-instant-furniture` 16 (new),
`companion-window` 15, `all-star-identities` 14 (new),
`chart-options-companion` 10, `clean-chart-controls` 7,
`closer-fields` 2 (the measurements' and one page's wording, now
naming Saturn among the closer bodies), and one file each of
`toggle-shortcuts`, `test-evidence`, `language-ledger`,
`control-explanations` and the provenance record. Four decision
records are new (`chart-instant`, `saturn-system`,
`saturn-cartography`, `saturn-handover`, `star-identity-density`),
two amended (`structure-emphasis`, `test-evidence`), and the two
Mercury article texts with twelve figures joined `docs/articles`.

The language ledger stands at **159 reviewed rows** under its cap of
160; the test-evidence measurements are requoted at **473 reads of live
chart state against 1 079 explicit hand-offs**; the suite is **2 327
tests**; the packaged licensing inventory is **13 resources**.

### 5.3 Performance, measured

Local gates on this Mac, from the records' start and verdict times: a
full seven-gate run **55:10–58:25** a head in Sprint 53 (`11f8bce3`
55:10, `c4f6e209` 55:13, `73a69e65` 57:04, `ad3ffe69` 57:29,
`d9be8b52` 57:31, `d818df80` 57:11, `1ac58afe` 58:25), up from
52:46–54:44 in Sprints 51–52 as the Saturn pack, its tests and the
Saturn studies joined; gate 1 alone 10:35–13:24; gates 2-7 from gate 2
45:17, from gate 3 42:33. CI's `test` workflow on a PR: **34–54 min**
on the wide route (`73a69e65` 54, `eb69beb7` 34, `d9be8b52` 35,
`d818df80` 45, `1cbbfd50` 36), 14–21 min on the narrow route
(`51a08ea2` 14, `1ac58afe` 21); `app-image` 7 min and `dist` 3 min on
`d9be8b52`. The budget's order is unchanged; the Saturn pack's 79 MB
is in every image.

## 6. Existing-body invariance through the whole road

- **The packs.** The Saturn pack is new (two kernels, 79 345 664
  bytes); the Solar System, planets and Jovian packs are byte-identical
  to 5.3.0's, held on every head (`PlanetsPackTest`, the Jovian and
  Solar System pack tests) and the Sun, Moon, Mercury, Venus and
  Jupiter services' answers unchanged (section 1.7 of the Saturn
  handover).
- **Released chart output.** From `f19395b0` to `582f6725` **no page
  of the Sun, the Moon, Mercury, Venus or Jupiter on the chart moved**,
  nor any chart-sheet, regional, overview, globe, meridian-tick,
  solar-system-label or printable study page: the only released-family
  files that moved are the two closer-field files whose words name
  Saturn, and the evidence contracts reproduced every ladder page on
  every head. Saturn's ink and the moons are off until chosen; the
  Place and Time box appears only while a time-dependent layer is on;
  the density switch is off by default; the agreement line is in the
  tables' sheets only.
- **The interface sheets** moved for their words and rows alone:
  Sprint 51's box and agreement line, Sprint 52's About notice and
  Saturn group, Sprint 53's View item, Controller switch and the widened
  explanations.

## 7. What 5.4.0 contains, and what it explicitly does not

**Contains:** Saturn computed, tabled and on the chart with its rings
and seven moons; the closer fields for Saturn; the chart's Place and
Time box and *Update to now*; emphasis through navigation; all star
identities where they fit; the Mercury article (live on Pages); the
qualification routes in the development guide.

**Does not:** Mars or any later planet (the Mars road #600–#608 is
filed and not begun); the Saturn article (#607, on the one-planet-later
cadence); the article library in the application (#598); ring, globe or
satellite shadows; Hyperion; time-range paths (#553); the configuration
diagram (#554); the verifier's recognition of dispatched runs (#594);
#571's absolute magnitude limit; a repair of the #362 capture family or
of #589.

## 8. The release rehearsal, and the owner's separate decisions

As for 5.3.0, the rehearsal is what the seven gates and the CI
workflows already do on the handover head, plus the live check, and it
is recorded rather than claimed: the **integrated evidence flush** is
gate 3 (`make evidence-contracts`) and gate 4 (`evidence-contracts-ci`)
on this head, every study of the three sprints regenerated on the
integrated main; the **packaged release journey on every platform
image** is the `app-image` workflow's four images (macOS arm64 and x64,
Linux, Windows) and their cross-architecture smoke test in run 39766843251,
which qualified #597's trial merge - whose tree is main `3c237d6e`'s,
the code this document describes, `#609` adding only the development
guide - each image running the packaged acceptance with *saturn table
OK*, *saturn on the chart OK* and *saturn moons on the chart OK* among
its journeys (run 39736175501 did the same for #596's trial merge, with `dist`
39736175674). This head changes only this document, so CI classifies it as
documentation and skips the images and its own evidence job; locally,
gate 5 runs the packaged acceptance on this Mac on this head. The
**dist archive built twice and verified** is gate 6; the **articles
reachable** is the live Pages check made on `3c237d6e` for #580 (both
Mercury pages HTTP 200). The results go on #581 and under
`~/juranometria-runs/runs/release-5.4.0/`.

Then, as 5.3.0 was done (`runs/release-5.3.0/README.md`): no
`VERSION` change, tag or publication in this issue. Each of the
following is a separate owner decision, asked for in turn and never
inferred from the one before: the `VERSION` bump with its changelog
date and the version-fed regenerations (the chart sheets, the Jovian
and Saturn A4 sheets, the closer-field and Mercury printable sheets,
the gallery manifest and pages, the About sheets, the page-language
companion, provenance); the release pull request; its merge under
#494; the release workflow's manual-dispatch rehearsal (publishes
nothing); the local annotated tag; the tag push, which publishes
(`release.yml` has no gate); the public verification; the close-out
of #573 and #581.

## 9. Still open after 5.4.0

#594 (the verifier and dispatched runs), #589 (the Place and Time
preferences node on CI's display, record-only), #362 (the capture-refusal
family, met on three gated heads this sprint), #571 (the absolute
magnitude limit for figure anchors), #598 (the article library in the
application), #607 (the Saturn article), #553 (time-range paths), #554
(the configuration diagram), #563, #564, #566, #569, #497, #417, #387,
#373, #370, #356, #355, #351, #339, #327, #326, #324, #295, #293, #282,
and the Milky Way sprint #189–#192 with PR #199, blocked on licensing.

### 9.1 Immediate successor: the Mars road

[#600](https://github.com/Aha43/JUranometria/issues/600), *Road: Mars,
Phobos and Deimos*, with its eight issues in order - #601 the system
contract, #602 the offline service, #603 the tables, #604 the numerical
handover, #605 the cartography study, #606 Mars and its moons on the
chart, #607 the Saturn article, #608 the handover to release
preparation. It begins only on the owner's instruction after 5.4.0
closes; no Mars work has begun.

### 9.2 The longer road

- [#553](https://github.com/Aha43/JUranometria/issues/553): time-range
  paths, for the first planet milestone that needs them.
- [#554](https://github.com/Aha43/JUranometria/issues/554): the
  Sun–Earth–planet configuration diagram, around the 6.0 wrap-up.
- [#598](https://github.com/Aha43/JUranometria/issues/598): the article
  library from the application and its subject panels.
