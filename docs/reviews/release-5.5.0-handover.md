# JUranometria 5.5.0 — the Mars road closed: handover to release

*Issue #608, closing the road to Mars, Phobos and Deimos (#600). Written
on main `d4493390`, the verified landing of the road's last pull
request, before any `VERSION`, tag or publication decision; the number
5.5.0 is this document's working name for the next release, the
owner's to confirm. Since 5.4.0 (`708d1228`, published 2026-10-23) two
stretches have landed: the between-planets work of Sprint 54 - one
chart time and place (#612, absorbing #569), the tracked-link repair
(#619), Subdue below horizon (#615) and Centre on coordinates (#623) -
and the Mars road, #601-#607, whose numerical half is handed over in
`docs/decisions/mars-handover.md` (#604). This document covers both,
with the release boundary.*

## 1. What a reader gets in 5.5.0

As the unreleased changelog states them, newest first:

- **The article "Saturn's rings on the page"** (#607), in English and
  Norwegian (*Saturns ringer på siden*), on released 5.4 behaviour -
  published through Pages on merge, not a release.
- **Phobos and Deimos on the chart** (#606 part 2, ruled on #605): with
  *Mars on the chart* on, both moons from 2000 to 2049 as 3 px symbols
  at their own places, each drawn only where its mark stands clear of
  Mars and of the other moon, on every field - no floor; in front only
  over a drawn disc of at least 12 px, in the shadow a hollow ring; a
  moon behind Mars or too close to tell apart not drawn and spoken in
  the page's description; never selectable.
- **Mars on the chart** (#606 part 1): *Mars on the chart* in View ▸
  Solar System after *Moon on the chart*, and *Show on chart* with
  *Centre on chart* in the Mars group and dialog; rule M phased in the
  page's ink with no colour; selectable as an occurrence; the closer
  fields offered on Mars; covered behind the Sun, with the state
  spoken; nothing outside 1900-2100.
- **Mars's cartography measured** (#605) and **Mars handed to
  cartography** (#604): the study with its eight rulings, and the
  settled numerical contract in one place.
- **Mars, Phobos and Deimos in the Solar System tables** (#603), the
  moons computed for 2000-2049 and Mars shown alone outside them.
- **Mars, computed offline** (#602): the planets pack's third kernel
  (DE440's Mars barycentre, 1900-2100) and a fifth pack, Mars System,
  Phobos and Deimos from MAR099; and the system contract (#601).
- **Centre on coordinates** (#623): View ▸ *Centre on coordinates...*,
  a modeless window for a typed J2000 position, the requested-position
  cross, and Search's words corrected to say J2000.
- **Subdue below horizon** (#615): on by default in Place and Time, the
  sky below the mathematical horizon at 40 % of its contrast, on screen
  and paper, independent of the horizon line.
- **One chart, one time and place** (#612, absorbing #569): one shared
  chart context read once per paint and export; stale table results
  dimmed under *Compute again*, never recomputed behind the reader's
  back; *At chart time* and *Over a time range*, a range always starting
  at the chart time; *Set chart time to this instant* on a row; a
  *Local sidereal time* field; *Set chart time to its next meridian
  transit* in the Inspector.

**Recorded by this handover.** The Unreleased changelog had no entry for
#615, #623, #601, #604 or #605: their pull requests changed no
`CHANGELOG.md`. Each now has its entry, in date order, written from its
decision record; no other entry changed.

Not a reader's change: the tracked `lib` and `imports` links committed
by #612 were removed (#619), and the worktree rule that caused them is
in the standing rules.

## 2. Every ruling of the stretch, as recorded

| ruling | where it stands | in short |
|---|---|---|
| #612, P1-P8 (2026-10-23) | `chart-context.md`, *The rulings* | stale results readable but dimmed, *Update from Place and Time* removed; ranges seeded from chart time; sidereal entry on the chart time's UTC date, the nearer of two; next upper culmination iterated to one second within a declared bound, refused past it; *Chart time (UTC)*; installation on the event thread, one snapshot per paint or export; place persistence unchanged; the Inspector's occurrence line kept |
| #612, P2 amended twice (2026-10-24) | `chart-context.md`, ruling 2 | first two views only; then **a range always starts at the chart time** - a span of 1 to 365 days and a step, storing no instant, so every body's range shares one start |
| #615, P-C to P-P (2026-10-24) | `below-horizon.md`, *The rulings* | 40 % of each ink's distance from the ground; on by default; independent of the line; a label by its anchor; curves split at the horizon; emphasis reduced alike; selection and working marks never subdued; screen and paper one rule. The words (P-W) ruled on the issue |
| #623, C-1 to C-4 (2026-10-25) | `centre-on-coordinates.md`, *The rulings* | the modeless dialog in View with its shortcut; Search's coordinate contract with the selection cleared; the requested-position cross, never selectable, inspected or exported; Search's words corrected |
| #601, M-1 to M-12 with M-7a and the amendment (2026-10-25) | `mars-system.md`, *The ruling* | DE440 for Mars, MAR099 for the moons to an exclusive TDB end at 2050-01-01; the figure, the frames, the observables and the words |
| #602's amendments with the wording notes (2026-10-25) | `mars-system.md`, *#602, measured* | targets amended where Skyfield was the wrong reference and where the time model is the cause; Espenak's coarse Mars hours allowed three hours |
| #603, the mnemonic (2026-10-26) | `mars-system.md` | "Mars… has no menu mnemonic in either language; preserve all existing mnemonics. Revisit only if the Solar System menu is reorganized later." |
| #605, R-1 to R-8 (2026-10-26) | `mars-cartography.md`, *The ruling* | "Agree": rule M phased; no colour; the moons only where clear, no Jupiter floor; vocabulary A; Mars selectable, the moons not; the closer fields; screen and paper as every body; Mars first, then the moons |
| #606 part 2's wording and transit case (2026-10-27) | #606; `mars-cartography.md`, *as built, part 2* | the spoken sentences for an undrawn moon kept; the June 2001 transit as the evidence case; the pointer-hover failure recorded as environmental |
| #607's texts (2026-10-27) | #607 | both texts accepted as written |

## 3. The owner's packaged verdicts

- **#612** - accepted on candidate `ade8a817` after two amendments of
  P2 on earlier candidates; landed on the owner's word.
- **#615** - the implementation landed on the owner's word after its
  packaged candidate, on which the evidence was then regenerated
  (`536c2d39`, *on the accepted candidate*).
- **#623** - the four rulings accepted as proposed; landed on the
  owner's word.
- **#603** - *"accept the candidate, Mars shown outside moon years as
  proposed, really good decision it seem to me"*.
- **#605** - the study's mock-ups: *"Agree"*, then *"Good work,
  merge."*
- **#606 part 1** - candidate `525d59b8`: *"accept the candidate, run
  the gates and CI (great work)"*.
- **#606 part 2** - candidate `bfabd85d`, through the owner's review of
  five visual points (Mars clean at ordinary fields; the moons separate
  at close fields; the in-front dot crossing Mars; hollow shadow marks
  and absent behind-Mars marks; Mars alone outside 2000-2049), then
  *"Cool all look great, good work!"* and *"merge"*.
- **#607** - *"accept both texts, run the gates and CI"*.

## 4. Every stopped, failed, superseded or incomplete run

Every record is kept under `~/juranometria-runs/runs/`, unchanged;
nothing was rerun into green.

| run | what happened | how it was continued |
|---|---|---|
| `gates-j612-93c9073e`, `-30fe421a` | all OK - the study and its ruled head | merged `ddec6382` |
| `gates-j612i-2182399a` | all OK locally; **CI failed** (run 40095380393): the stale note's wrapped HTML on CI's narrow Linux host did not contain the asserted sentence | not retried; the check reads the spoken name on `f869b213` (all OK, CI green) |
| `gates-j612i-5f06172e` | **stopped at gate 1**: the language ledger (new occurrences unreviewed) and the Place and Time access-letter premise (ten letters, not nine) | superseded by the accepted candidate's heads above |
| `gates-j619-edce5f98-run1` | **stopped at gate 3**: the constellation and star-identity families unverified - their gitignored raw sources had gone, replaced by #612's tracked `imports` link | the raw sources restored with `scripts/download-*-sources.sh` |
| `gates-j619-edce5f98-run2` | **aborted by the operator** during gate 1, doomed to the same stop (`ABORTED.md`) | run 3 all OK; merged `bed281cf` |
| `gates-j615-74a981c3` | **stopped at gate 1**: the test-evidence report did not reproduce | regenerated on `80ea8958` (all OK) |
| `gates-j615-80ea8958` | all OK; **CI's evidence job failed** (run 40131975033): `below-horizon/measurements.md` did not reproduce on Linux | not retried; fixed on `58a5c1cc` (all OK, CI green); merged `ad1c1b78` |
| `gates-j615i-536c2d39` | **stopped at gate 1**: the Jupiter article's horizon figure no longer the evidence page; the decision's quoted count; a Place and Time capture refusal | the article figures recopied and the counts requoted on `26ba46f1`, gated from gate 1 (all OK); merged `8858f40c` |
| `gates-j602-00afefc3` | **stopped at gate 6**: the archive lacked `NOTICE-mars-system.md` | added to the `dist` copy on `efcc72a0` (all OK) |
| `gates-j603-7f214aba` | all OK; **CI's Linux image failed** (run 40306977207): packaged acceptance read a wrapped status line | the journey reads the accessible name on `21cb1802` (all OK, CI green) |
| `gates-j605-7057e9ae` | **stopped at gate 4**: out of memory under the portable contract's 1 GiB heap - the study held static service state | the study releases its statics in `finally` on `686534dc`, the heap unchanged (all OK) |
| development runs of #606 part 1 | three closer-field wording pins; the 14 extra focus aborts of a desktop that would not give focus | the pins updated before gating; gate 1 on `525d59b8` 12 aborts |
| a development run of #606 part 2 | a stray pointer over *Zoom Out* gave five toolbar sheets a hover highlight | environmental: with the pointer parked the interface gate passed on the same tree; no evidence changed |
| `gates-j606m-bfabd85d` | gate 1 INCOMPLETE: `PlaceAndTimeSheetMain` refused 420 → 324, the #362 kind | all seven precedent conditions in `INCOMPLETE.md`, the 150 artifacts of the refused run byte-identical; gates 2-7 once (`rest.sh`), gate 2 running the test clean; CI's display run 2428/2428; merged `10268d2b` |

Gate 1's aborts were 12 on most heads and 49 on three; every record's
list of unexplained aborts is empty - each is the desktop-focus family.

Lessons the next road inherits:

- never link `lib` or `imports` into a worktree, nor `git add -A` over
  such links (#619);
- a check of words on a narrow host reads the accessible name, never the
  wrapped label (#612, #603);
- a study released into the one-JVM portable contract releases its
  static state;
- a new notice joins the Makefile's `dist` copy as well as the verifier
  (#602, as #575 before it);
- park the pointer before any run that photographs the interface.

## 5. Routes, evidence movement and performance

### 5.1 The route every PR took

Each PR ran CI once after all seven local gates once (or, under the
precedent, the remaining gates once); each merge was pinned to its
exact head and landed under #494, structurally verified on all four
points.

| PR | issue | head → merge | PR `test` run | landing `test` run |
|---|---|---|---|---|
| #614 | #612 study | `30fe421a` → `ddec6382` | 40023210047 | 40025606425 |
| #616 | #612 | `f869b213` → `d9aca4fa` | 40100247920 | 40106805126 |
| #620 | #619 | `edce5f98` → `bed281cf` | 40110931513 | 40119338970 |
| #621 | #615 study | `58a5c1cc` → `ad1c1b78` | 40141674494 | 40145907727 |
| #622 | #615 | `26ba46f1` → `8858f40c` | 40157663451 | 40160338811 |
| #624 | #623 | `d0433c52` → `eed9ccce` | 40206688028 | 40211675439 |
| #625 | #601 | `9a1b5bad` → `ef9e3656` | 40225797934 | 40229718758 |
| #626 | #602 | `efcc72a0` → `0d9854bc` | 40245982170 | 40281283483 |
| #627 | #603 | `21cb1802` → `9470b0a2` | 40314722795 | 40323547862 |
| #628 | #604 | `c25b7730` → `ab99bc5e` | 40333651820 | 40341004141 |
| #629 | #605 | `686534dc` → `b82cfc41` | 40363063626 | 40371888319 |
| #630 | #606 part 1 | `525d59b8` → `50fe7e75` | 40395653470 | 40405129949 |
| #631 | #606 part 2 | `bfabd85d` → `10268d2b` | 40439819742 | 40453720227 |
| #641 | #607 | `495afcb7` → `d4493390` | 40475495147 | 40488913061; Pages green |

Every landed PR's display job ran the whole suite with 0 failed, 0
aborted and 0 skipped: 2 327, 2 354, 2 357, 2 357, 2 364, 2 370, 2 370,
2 403, 2 415, 2 415, 2 418, 2 422, 2 428 and 2 432 tests.

### 5.2 What evidence moved

From `708d1228` to `d4493390` the promoted artifacts in
`docs/studies/PROVENANCE.md` went from **774 to 848**. Under
`docs/studies`, **315 files** moved - 148 added, 143 modified, 24
deleted - of the 1 400 at the tag; the whole change is 491 files,
+53 782 / −3 174 lines. By directory: `interface-language` 128 (the
tables' sheets reshaped by #612, 24 of their variants removed with the
views they showed; the menu, companion and About sheets for Mars),
`mars-system` 45 (new), `below-horizon` 34 (new), `solar-system-controls`
24, `mars-cartography` 19 (new), `mars-on-the-chart` 18 (new),
`companion-window` 16, `place-and-time` 7, `clean-chart-controls` 5,
`solar-system-labels` 4, `chart-options-companion` 3, `closer-fields` 2
(the wording now naming Mars), `control-explanations` 2, and one file
each of `test-evidence`, the provenance record, and the horizon pages
#615 subdued: `sun-on-the-chart`, `moon-on-the-chart`,
`jupiter-on-the-chart`, `saturn-on-the-chart`, `mercury-cartography`
and `gallery`. Six decision records are new (`chart-context`,
`below-horizon`, `centre-on-coordinates`, `mars-system`,
`mars-handover`, `mars-cartography`), two amended (`chart-instant`,
`test-evidence`), `docs/development.md` amended, and the two Saturn
article texts with twelve figures joined `docs/articles`.

The language ledger stands at **159** under its cap of 160; the
test-evidence measurements are quoted at **478 reads of live chart
state against 1105 explicit hand-offs**; the suite is **2 432 tests**;
the packaged licensing inventory is **13 resources**.

### 5.3 Performance, measured

Local gates on this Mac, from each record's start to its verdict: a full
seven-gate run **57:11-61:33** a head (`93c9073e` 57:11, `30fe421a`
59:13, `2182399a` 57:57, `f869b213` 59:25, `edce5f98` 58:23, `80ea8958`
58:24, `58a5c1cc` 59:10, `26ba46f1` 59:07, `d0433c52` 58:52, `9a1b5bad`
59:01, `efcc72a0` 59:31, `7f214aba` 59:39, `21cb1802` 60:58,
`c25b7730` 59:40, `686534dc` 60:28, `525d59b8` 61:22, `bfabd85d`
61:33 with gates 2-7 under the precedent, `495afcb7` 61:11), up from
55:10-58:25 in Sprint 53 as Mars's two packs, its tests and its studies
joined; gate 1 alone 10:40-12:31. CI's `test` workflow on a PR, from
creation to completion: **37-60 min** on the wide route (`f869b213` 51,
`58a5c1cc` 54, `26ba46f1` 40, `d0433c52` 57, `9a1b5bad` 47, `efcc72a0`
56, `21cb1802` 54, `686534dc` 42, `525d59b8` 60, `bfabd85d` 37,
`495afcb7` 60) and 13-20 min where the change allowed a narrower one
(`30fe421a` 14, `edce5f98` 20, `c25b7730` 13 - the narrow route,
`evidence` skipped). The Mars System pack's
43,951,104 bytes are in every image.

## 6. Existing-body invariance through the whole road

- **The packs.** The planets pack is version 3, adding Mars's kernel;
  Venus's and Mercury's files in it are unchanged. Mars System is new.
  The Solar System, Jovian and Saturn System packs are byte-identical to
  5.4.0's: both Mars builders refuse to write unless every released
  kernel carries its released digest, and `PlanetsPackTest` and
  `MarsSystemPackTest` hold them on every head (`mars-handover.md`,
  section 1.7). No released service changed; the Sun, Moon, Mercury,
  Venus, Jupiter and Saturn reference tests ran unchanged on every head.
- **Released chart output.** Two rulings moved released pages, and only
  they: #615 subdued what lies below a drawn horizon - the six horizon
  pages named in section 5.2 and the wide label pages - and the
  closer-field wording now names Mars. Every other ladder, sheet,
  regional, overview, globe and printable page reproduced on every head
  under gates 3 and 4. Mars's ink and its moons are off until chosen.
- **The interface sheets** moved for their words and rows alone: #612's
  table views, the stale line and the new fields; #615's switch; #623's
  dialog; the Mars table, group, notice and menu items; and the
  menu photographs, which now also show *Saturn...* and *Saturn on the
  chart*, missing from them since 5.4.0.

## 7. What 5.5.0 contains, and what it explicitly does not

**Contains:** one chart time and place; Subdue below horizon; Centre on
coordinates; Mars computed, tabled and on the chart with Phobos and
Deimos; the closer fields for Mars; the Saturn article (live on Pages).

**Does not:** the Mars article (#639, on the one-planet-later cadence,
in the Uranus road); Uranus or any later planet (the Uranus road
#632-#640 is filed and not begun); surface markings, albedo or colour
for Mars; shadows of the moons on Mars; event search, paths over a range
(#553) or the configuration diagram (#554); the article library in the
application (#598); the verifier's recognition of dispatched runs
(#594); #571's absolute magnitude limit; a repair of the #362 capture
family or of #589.

## 8. The release rehearsal, and the owner's separate decisions

As for 5.4.0, the rehearsal is what the seven gates and the CI
workflows already do, recorded rather than claimed. The **integrated
evidence flush** is gate 3 (`make evidence-contracts`) and gate 4
(`evidence-contracts-ci`) on this head, every study of the stretch
regenerated on the integrated main. The **packaged release journey on
every platform image** is the `app-image` workflow's four images and
their cross-architecture smoke test on #641's trial merge, whose code
is main `d4493390`'s, each running the packaged acceptance with *mars
table OK*, *mars on the chart OK* and *mars moons on the chart OK* among
its journeys; locally, gate 5 runs the same acceptance on this Mac on
this head. The **dist archive built twice and verified** is gate 6.
The **articles reachable** is the Pages deployment of `d4493390`. The
results go on #608 and under `~/juranometria-runs/runs/release-5.5.0/`.

Then, as 5.4.0 was done: no `VERSION` change, tag or publication in
this issue. Each of the following is a separate owner decision, asked
for in turn and never inferred from the one before:

1. the version number and the `VERSION` bump with its changelog date
   and every version-fed regeneration - found by the version strings in
   the committed artifacts, never by copying 5.4.0's list (the chart
   sheets, the Jovian, Saturn and closer-field A4 sheets, the
   chart-instant furniture sheet, the Mercury printable sheet, the
   gallery, the About sheets, the page-language companion, provenance);
2. the release pull request, and its merge under #494;
3. the release workflow's manual-dispatch rehearsal, which publishes
   nothing;
4. the local annotated tag;
5. the tag push, which publishes - `release.yml` has no gate;
6. the public verification, and the close-out of #600 and #608.

## 9. Still open after 5.5.0

#613 (gate plans from change impact), #594, #589, #571, #566, #564,
#563, #553, #554, #598, #497, #417, #387, #373, #370, #362, #356, #355,
#351, #339, #327, #326, #324, #295, #293, #282, #617 (angles and
telescope fields), #618 (the ephemerides article), and the Milky Way
sprint #189-#192 with PR #199, blocked on licensing.

### 9.1 The next between-planets sprint

The road asks for a between-planets stretch before the next planet, as
Sprint 54 came between Saturn and Mars. Its content is the owner's to
choose. Filed and ready, among others: #613, planning a gate run from
the declared change impact, which would shorten the hour each head now
takes; #617, measured angles and telescope fields on the chart; #618,
how JPL ephemerides become an offline atlas, the cross-planet article
the Saturn handover deferred to the 6.0 wrap-up; #553, paths over a
time range, the first feature the one-chart-time ruling was made for.

### 9.2 Next planet road: Uranus

[#632](https://github.com/Aha43/JUranometria/issues/632), *Road: Uranus
and its five major moons*, is filed with its eight issues in order -
#633 the system contract, #634 the offline service, #635 the tables,
#636 the numerical handover, #637 the cartography study, #638 Uranus
and its moons on the chart, #639 the Mars article, #640 the handover to
release. It begins only on the owner's instruction after 5.5.0 and any
chosen between-planets work close; no Uranus work has begun.
