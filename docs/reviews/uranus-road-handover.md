# The Uranus road closed: handover into the Neptune road

*Issue #640, closing the road to Uranus and its five major moons (#632).
Written on main `f2905f61`, the verified landing of the road's last pull
request. Since 5.5.0 (`6958ce75`, published 2026-10-27) one stretch has
landed: the Uranus road, #633-#639, whose numerical half is handed over
in `docs/decisions/uranus-handover.md` (#636) and whose chart contract is
`docs/decisions/uranus-cartography.md` (#637).*

**No release at this boundary.** By the owner's decision of 2026-10-29,
recorded on #632 and #640, the Uranus road gets no release of its own:
the work passes straight into a Neptune road, and **6.0.0 is released
when Neptune is done**, completing the classical planets through Neptune.
This document therefore makes no `VERSION` change, no tag and no release
rehearsal; section 8 says where those now stand.

## 1. What a reader will get from this road

As the unreleased changelog states them, newest first:

- **The article "Mars on the page"** (#639), in English and Norwegian
  (*Mars på siden*), on released 5.5 behaviour - published through Pages
  on merge, not a release.
- **Uranus's moons on the chart** (#638 part 2, ruled on #637): with
  *Uranus on the chart* on, Miranda, Ariel, Umbriel, Titania and Oberon
  from 2000 to 2100 as 3 px symbols at their own places, each drawn only
  where its mark stands clear of Uranus's and of the moons already
  drawn, on every field; a moon behind, in transit or too close to tell
  apart not drawn and spoken; never selectable.
- **Uranus on the chart** (#638 part 1): *Uranus on the chart* in View ▸
  Solar System after *Saturn on the chart*, and *Show on chart* with
  *Centre on chart* in the Uranus group and dialog; the 6 px symbol on
  every field, full and round, no pole and no rings; selectable as an
  occurrence; the closer fields offered on Uranus; covered behind the
  Sun, with the state spoken.
- **Uranus's cartography measured** (#637) and **Uranus handed to
  cartography** (#636): the study with its ten rulings, and the settled
  numerical contract in one place.
- **Uranus and its five moons in the Solar System tables** (#635), the
  moons computed for 2000-2100 and Uranus shown alone before them.
- **Uranus, computed offline** (#634): a sixth pack, Uranus System, with
  DE442's Uranus barycentre over 1900-2100 and URA182.21's five moons
  over 2000-2100; and the system contract (#633).

**Recorded by this handover.** The Unreleased changelog had no entry for
#633, #636 or #637: their pull requests changed no `CHANGELOG.md`. Each
now has its entry, in date order, written from its decision record; and
#638's part 2 entry now stands above part 1, newest first, as Mars's do.
No other entry changed.

Not a reader's change: the evidence contracts' heap rose from 1 GiB to
2 GiB (#635; section 4).

## 2. Every ruling of the stretch, as recorded

| ruling | where it stands | in short |
|---|---|---|
| #633, U-1 to U-13 (2026-10-27) | `uranus-system.md`, *The ruling* | "Rule U-1 to U-13 as proposed": DE442's barycentre for Uranus (U-1a), the barycentre for the centre (U-2), the PCK figure, the IAU pole with no periodic terms, the fraction and phase angle without phase words, all five moons 2000-2100 (U-6a), Jupiter's six states on the flattened figure, no ring rows (U-8), Saturn's card without rings, Saturn's and Mars's targets, the fixtures, the stated event limitation, Mars's production split |
| #634's distance target and six confirmations (2026-10-28) | `uranus-system.md`, *#634, measured and as built* | "Choose (a): keep the barycentre and amend the exact-era distance target to 50 km" - a correction of #633's missed centre-versus-barycentre consequence, not a relaxation; the near-Sun and near-Uranus print rules, the post-2027 states printed, events not asserted, one pack, the computed licensing count |
| #635's acceptance and letters (2026-10-28) | `uranus-system.md`, *#635* | accepted untested; *Uranus...* lettered N (English) and U (Norwegian), "No letter for mars" kept |
| #635's evidence heap (2026-10-28) | the Makefile; `uranus-system.md` | the three evidence-contract routes at 2 GiB |
| #636's two corrections (2026-10-28) | `uranus-handover.md` | the published 5.5.0 baseline; the registration and the heap measurement deferred and not yet filed |
| #637, R-1 to R-10 (2026-10-28) | `uranus-cartography.md`, *The ruling* | the 6 px mark always; unphased, round, no colour; no pole; **rings table-only**; **Mars's clearance rule for the moons**; vocabulary A; **closer fields at 0.5° and 0.25°**; Uranus selectable, its moons not; screen and paper as every body; Uranus first, then the moons |
| the gate plan (2026-10-28) | standing rule | before any gate run, a stated plan of which gates and why, approved with the checkpoint; gates 1 and 7 never skipped; doubt means all seven |
| no Uranus release (2026-10-29) | #632, #640 | the Neptune road next, 6.0.0 after Neptune, 7.0.0 for improvement sprints, Pluto later |

## 3. The owner's packaged verdicts

- **#635** - candidate `8a1f40e0`: *"Don't got chance to test. Chance is
  ok, always been. Leave letter. Go on."*, then *"Great. No letter for
  mars"*.
- **#637** - the study and its mock-ups: *"I agree with the
  recommendations. Rule R-1 through R-10 exactly as proposed"*.
- **#638 part 1** - candidate `5ddbf42e`, tried in the packaged
  application (*"Nice it is spot on the ecliptic NOW"*): *"Accept part 1,
  run the gates and CI"*.
- **#638 part 2** - candidate `4a97a521`: *"Accept part 2, run the gates
  and CI"*.
- **#639** - *"Accept both texts, run the gates and CI"*, after reading
  the built pages.

## 4. Every stopped, failed, superseded or incomplete run

Every record is kept under `~/juranometria-runs/runs/`, unchanged;
nothing was rerun into green.

| run | what happened | how it was continued |
|---|---|---|
| `gates-j633-6d5526c9` | the proposal head; gates 1 and 2 OK, **stopped during gate 3 on the owner's word** (`ABORTED.md`) | the gate-economy rule; the ruled head `2bda2543` all OK |
| the raw kernel | removing the #633 worktree deleted `ura184_part-3.bsp`, kept in its `imports/raw` | re-fetched with approval into the main checkout; checksums matched |
| `gates-j635-c936de1b` | **stopped at gate 4**: the portable contract ran out of its 1 GiB heap inside the label study (`GATE4-FAILED.md`) | not retried; the owner chose 2 GiB; `02038686` all OK |
| `gates-j638-b1eedee3-run1-pointer` | **stopped at gate 1**: the pointer rested over the toolbar photographs while the owner tried the candidate (`GATE1-STOPPED.md`) | the owner ordered a rerun with the candidate quit and the mouse still |
| `gates-j638-b1eedee3` | gate 1 **INCOMPLETE**: the export dialog's capture refused 332 → 326, the known kind | all seven precedent conditions in `INCOMPLETE.md`, the 14 artifacts of the refused run byte-identical; gates 2-7 once (`gates-from2.sh`), gate 2 running the test clean; CI's display run 2 486/2 486 |
| development runs of #634 | the language ledger at 160 (one new literal), the About sheets and the test-evidence measurements stale, the distance cell failing its #633 target | the literal reworded (159), the evidence regenerated, the distance ruled |
| development runs of #635 | the test-evidence hand-off count (1105 → 1106) and its measurements | regenerated and requoted |
| a development run of #638 part 1 | the preference-door pin: *Uranus on the chart*'s store the nineteenth entry point | the pin moved by name |

Gate 1's aborts were 12, 28 or 49; every record's list of unexplained
aborts is empty - each is the desktop-focus family.

Lessons the next road inherits:

- **raw downloads outlive worktrees**: fetch into the main checkout's
  `imports/raw/<system>/` and copy into a worktree;
- **a study measures every observable a ruling touches**, not only the
  angle - the barycentre ruling was measured in arcseconds, never in
  kilometres;
- **quit the candidate and keep the mouse still before any gate run**;
- **the evidence contracts' heap is 2 GiB**, what the one process
  retains still unmeasured;
- **a study or report joins the evidence contract when it lands** - the
  Uranus accuracy report did not (section 9);
- **a menu or Controller item added in outward order moves every
  journey that pins the order, and the menu photographs** - which had
  missed *Uranus...* from #635 until #638.

## 5. Routes, evidence movement and performance

### 5.1 The route every PR took

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

Every landed PR's display job ran the whole suite with 0 failed, 0
aborted and 0 skipped: 2 432, 2 465, 2 479, 2 479, 2 482, 2 486, 2 492
and 2 496 tests.

### 5.2 What evidence moved

From `6958ce75` to `f2905f61` the promoted artifacts in
`docs/studies/PROVENANCE.md` went from **848 to 881**. Under
`docs/studies`, **117 files** moved - 96 added and 21 modified, none
deleted; the whole change is 208 files, +77 740 / −103 lines. By
directory: `uranus-system` 60 (new), `uranus-cartography` 21 (new),
`interface-language` 17 (the About, companion and menu sheets),
`uranus-on-the-chart` 15 (new), `closer-fields` 2 (the wording now naming
Uranus), and one file each of `test-evidence` and the provenance record.
Three decision records are new (`uranus-system`, `uranus-handover`,
`uranus-cartography`), one amended (`test-evidence`), and the two Mars
article texts with ten figures joined `docs/articles`.

**No released chart output moved.** Uranus's ink and its moons are off
until chosen; every ladder, sheet, regional, overview, globe and
printable page reproduced on every head under gates 3 and 4 where they
ran.

The language ledger stands at **159** under its cap of 160; the
test-evidence measurements are quoted at **1106 explicit hand-offs**;
the suite is **2 496 tests**; the packaged licensing inventory is
**15 resources**, now counted from its own list.

### 5.3 Performance, measured

Local gates on this Mac, from each record's start to its verdict: a full
seven-gate run **74.9 and 75.5 min** at the 1 GiB evidence heap
(`2bda2543`, `4ca9ebb0`), and **54.9-55.6 min** at 2 GiB (`02038686`
55.1, `1498791a` 54.9, `83e163ef` 55.6, `be8b5589` 55.0); gate 4 alone
fell from 38.6-38.8 min to 19.7. Gate 1 alone 11.0-12.4 min. The first
gate plan that trimmed a run - gates 1 and 7 and the Pages build for the
Mars article (`8a430154`) - took **11.9 min**. CI's `test` workflow on a
PR, from creation to completion: **42-79 min** (`2bda2543` 79, `4ca9ebb0`
55, `02038686` 51, `1498791a` 48, `83e163ef` 44, `b1eedee3` 48,
`be8b5589` 42, `8a430154` 49). The Uranus System pack's 38.4 MB are in
every image.

## 6. Existing-body invariance through the whole road

- **The packs.** Uranus System is new; the Solar System, Jovian,
  planets (version 3), Saturn System and Mars System packs are
  byte-identical to 5.5.0's - the Uranus builder refuses to write unless
  every released kernel carries its released digest, and
  `UranusSystemPackTest` holds them.
- **The services.** `UranusService` and `UranusSystemPack` are new; no
  released service file changed. The Sun, Moon, Mercury, Venus, Mars,
  Jupiter and Saturn reference tests ran unchanged on every head.
- **The interface sheets** moved for their words and rows alone: the
  About notice, the collapsed Uranus group in the companion, the menu
  items *Uranus...* and *Uranus on the chart*, and the closer-field
  wording.

## 7. What the road contains, and what it explicitly does not

**Contains:** Uranus computed from DE442's barycentre, tabled and on the
chart as the 6 px symbol with its five moons; the closer fields for
Uranus; the Mars article (live on Pages).

**Does not:** a Uranus release (section 8); the Uranus article (on the
one-planet-later cadence, in the Neptune road); Uranus's rings on the
chart (table-only, R-4); a phased or coloured Uranus; Uranus's pole on
the chart; the irregular moons, or the five moons before 2000; event
search, paths over a range (#553) or the configuration diagram (#554);
Neptune or Pluto.

## 8. No release here: where the release work now stands

The owner's decision of 2026-10-29 moves the release boundary to the end
of the Neptune road. Accordingly:

- **No `VERSION` change, tag or release rehearsal** happens in #640.
  `VERSION` stays **5.5.0**.
- **The integrated evidence flush and the packaged release journey on
  every platform image** move to the Neptune road's 6.0.0 handover,
  where they cover Mars's article, Uranus and Neptune together since
  5.5.0. Until then each head's gates and CI do the same work per PR,
  as recorded above.
- **The release decisions** - the version number and bump, the release
  pull request, the dispatch rehearsal, the local tag, the tag push that
  publishes, and the public verification - stay separate owner
  decisions, asked for in turn at the 6.0.0 boundary, never inferred.

## 9. What the Neptune road inherits

### 9.1 Deferred and not yet filed

- **Register `docs/studies/uranus-system/service-accuracy.md` in the
  evidence contract**, as Mars's accuracy report is;
  `UranusReferenceVectorTest` holds its numbers meanwhile.
- **Measure what the evidence-contract process retains** at 2 GiB.

### 9.2 The Neptune road

Filed when this handover lands, shaped like #632, its issues in order:

1. **the system contract**: authority and kernels, interval, figure and
   pole, which moons (Triton certainly; Nereid and the others decided
   on measurement), rings as an open question, the accuracy targets;
2. **the offline service and data pack**, released packs invariant;
3. **the tables**, with a packaged checkpoint;
4. **the numerical handover**;
5. **the cartography study**;
6. **Neptune and its ruled moons on the chart**, the planet then the
   moons;
7. **the Uranus reader article**, on the one-planet-later cadence;
8. **the integrated 6.0.0 handover and release boundary**, with the
   evidence flush and the packaged release journey of section 8.

**The Neptune road includes the Uranus article; the Neptune article
opens the post-6.0 road toward 7.0.0**, both on the one-planet-later
cadence. After 6.0.0, the road to 7.0.0 is planned around general GUI, chart, catalogue
and workflow improvements; candidates already filed include #613, #617,
#618, #553 and the GUI refinements set aside during the Mars road. Pluto
and the wider outer Solar System wait for a later second Solar System
sprint.

### 9.3 Still open

#613, #618, #617, #598, #594, #589, #571, #566, #564, #563, #554, #553,
#497, #417, #387, #373, #370, #362, #356, #355, #351, #339, #327, #326,
#324, #295, #293, #282, and the Milky Way sprint #189-#192 with PR #199,
blocked on licensing.
