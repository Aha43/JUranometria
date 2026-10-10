# Sprint 45 handover — Jupiter on the chart

Five issues, seven pull-request heads, three packaged owner
checkpoints and two owner merge decisions put Jupiter and its four
Galilean moons on the chart. The sprint's issues
ran in order:

- **#483, a shared Centre on chart.** One action for the Sun and the
  Moon, from the Controller and from each table. It also prepared
  Jupiter's seam without a button.
- **#482, a measured cartography study.** The owner ruled nine
  questions and rejected three of its proposals. The study was then
  amended twice before it merged.
- **#484, Jupiter drawn.** It added one module, one remembered switch,
  the 6 px symbol beneath the stars, the true disc above them, and
  Centre on chart for Jupiter.
- **#485, the four moons.** Each moon is decided on its own, in state
  vocabulary A.
- **#486, the evidence once.** It added a production study, an A4
  sheet at a measured scale, screen and export reconciled pixel for
  pixel, the Controller photographs, and a gallery room, published on
  the owner's approval.

Eleven gate runs were recorded. Four of them stopped or were recorded
incomplete, and one CI run failed. Each is listed below as it
happened, and none was rerun into green.

Written against `main` at `7746098b` (PR #492), after its post-merge
checks (`test` 38051270940, `app-image` 38051275612, `pages`
38051271052) and the live verification of the gallery room. There is
no VERSION change, no tag and no release.

## What shipped

| issue | pull request | merged | what |
|---|---|---|---|
| #483 | #488 | `9b7506a3` | `CentreOnChart` for the Sun and the Moon in both hosts; Jupiter's seam prepared; `ChartViewState.normalMinimumFieldDegrees()` |
| #482 | #489 | `3b557415` | the measured cartography study, its frame contract (`JovianChartFrameTest`), the label invariant (`JovianLabelInvariantTest`), 20 mock-ups, and the decision record with the owner's rulings |
| #484 | #490 | `77b1139b` | `JovianModule`, `OverlayContribution.OblateBody`, the switch *Jupiter and moons on the chart*, Jupiter's symbol and true disc, Centre on chart for Jupiter |
| #485 | #491 | `cd56d46a` | `OverlayContribution.Satellite`, `SatelliteMarks`, vocabulary A, the moons' names and spoken states |
| #486 | #492 | `7746098b` | `JupiterOnTheChartStudyMain`, the A4 Jovian sheet, the packaged sheet journey, the Controller photographs, and the gallery room *Jupiter and its moons* |

## Owner rulings and checkpoint findings

### #483 — Centre on chart

The packaged checkpoint was accepted on `bb53e57b` with "Works well,
tested." The owner recorded alongside it:

- a range centres on its first instant;
- opening the Sun and Moon dialogs keeps computing on open;
- chart titles based on coordinates would be preferable (not built);
- Jupiter gets no button until its visible module lands. It landed
  in #484.

### #482 — the cartography rulings

The owner ruled on the first proposal (`b55bc2b9`):

1. **One switch**, *Jupiter and moons on the chart*, off by default and
   remembered.
2. **Jupiter's mark is 6 px or its true size**, whichever is larger.
   The minimum is called *a cartographic symbol, not Jupiter's
   apparent diameter*.
3. **The true figure and pole**, through a continuous transition and
   the J2000-derived pole. The flattening is not claimed to be visible
   at 1°.
4. **Moons are 3 px symbols, each decided on its own.**
   - At the normal minimum field, every moon not behind Jupiter is
     drawn, and overlap is allowed.
   - Above it, a moon is drawn only where its own mark is
     distinguishable.
   - A collision suppresses only the lower-priority mark.
5. **State vocabulary A.**
   - Clear: a filled dot.
   - In front: a dot with a ground ring.
   - Behind: omitted.
   - In shadow: a hollow ring.
   - No ghost.
6. **Labels use adjacent candidates and the existing collision
   machinery.** A crowded label is refused on its own. There is no
   fixed right-hand rule, and one crowded pair never suppresses every
   label.
7. **Horizon, page edge, stars, clicks and export** follow the Sun's
   and the Moon's rules. The symbolic mark never erases a real star.
8. **Centre on chart** goes through #483's seam.
9. **No geometry, label or evidence code assumes 1° is permanent.**

**Rejected from the proposal:**

- drawing all the moons only when the whole system is separable;
- vocabulary B, with a dashed ghost and grey shadow;
- right-only labels.

**Two later rulings:**

- **The label counts became a platform record.** CI on `aa142b99`
  showed the label counts depend on the platform's fonts. Production
  placement uses real font metrics, so the counts moved to a
  per-machine record, and the invariant became a test.
- **The capture-refusal precedent was clarified.** See "Stopped,
  failed and incomplete runs".

The amended study was accepted and merged on `9f2dd5ed`.

### #484 — Jupiter

The packaged checkpoint was accepted on `b677d122`. The verdict, as
recorded: "Jupiter shows fine; I liked how it kept its size so it
remained visible when zooming out. Language okay. No moons expected at
this point."

- **Ruling 7, as read:** only the true disc hides stars, never the 6 px
  symbol. The reading was accepted.
- **Norwegian approved:** *Jupiter og månene på kartet*, *et kartografisk
  symbol, ikke Jupiters tilsynelatende diameter*, *Sentrer kartet på
  Jupiter*.

### #485 — the moons

The packaged checkpoint was accepted on `c754b23c`. The Norwegian was
approved: *foran Jupiter*, *i Jupiters skygge*, the moons' symbol
phrases, and the names *Io, Europa, Ganymedes, Callisto*.

### #486 — the evidence and the gallery

Publication was approved at the gallery's shared Oslo moment, which
keeps the one-moment tour. The triple transit stays in the study and
on the exported sheet, where its special time is explicit.

### Carried forward, not built

- **The discoverability of *Update from Place and Time*.** This
  finding was deferred in Sprint 44, for the Sun, the Moon and Jupiter
  together. It is unchanged.
- **Coordinate-based chart titles.** The owner noted this at #483.

## The frame contract

- **Positions.** Jupiter and each moon are drawn at the service's
  astrometric J2000 place, through the page's ordinary projection.
  The table's apparent X and Y are never reused: they differ from the
  J2000 offsets by up to 6.0″.
- **The pole.** Jupiter's pole is derived from the PCK pole vector at
  Jupiter's J2000 place, in `JovianSystemService.poleAngleJ2000Degrees`.
  It is turned onto the page through `PageBasis`, the page's own north
  and east at Jupiter.
  - It agrees with the table's of-date angle, carried back by the
    atlas's own rotation, to 0.003° over the century.
  - The two frames' angles themselves differ by up to 0.58°.
  - `JovianModuleTest` holds production equal to the study's
    derivation.
- **Evidence.** The production study measured every drawn centre at
  0.0000 px from the service's own projection.

## Marks, transitions and states

- **Jupiter's symbol.** While the true disc is under 6 px, the mark is
  a 6 px cartographic symbol. That holds at every field above about
  1° on the 900 px page.
  - It is painted in the reference layer, beneath every catalogue mark,
    so it never erases a star.
  - It hides nothing, so a click passes through it.
  - It is spoken as *(a cartographic symbol, not Jupiter's apparent
    diameter)*.
- **The true disc.** From 6 px across the mark is the true disc: 7.6
  to 12.5 px at 1°.
  - It is painted in the bodies layer, opaque, like the Sun and the
    Moon, and a click on it is the empty sky there.
  - Its outline turns from a circle at 6 px to the true axis ratio at
    12 px, with the minor axis along the pole.
- **The moons.** Each is a 3 px symbol, never its apparent diameter,
  which is 0.67 to 1.84″ and below a pixel on every page.
  `SatelliteMarks` decides each on its own:
  - **Behind:** omitted.
  - **At the normal minimum field:** drawn at the exact place, overlap
    allowed.
  - **In front, above that field:** drawn only over a drawn disc at
    least 12 px wide.
  - **Clear or shadowed, above that field:** drawn only 6 px clear of
    Jupiter's edge and of every higher-precedence moon.
  - **Precedence:** in front, then clear, then shadowed; then Ganymede,
    Callisto, Io and Europa.
- **Vocabulary A**, held on both palettes by `JovianMoonsInkTest`:
  - a filled dot means clear of Jupiter;
  - a dot ringed in the page's ground, over the disc, means in front;
  - a hollow ring means wholly or partly in shadow; the table keeps the
    difference;
  - nothing means behind.
- **Layers.** A moon mark clear of the opaque disc is painted beneath
  the stars. A mark in front of the disc, or touching it, is painted
  with the disc, over it. No moon mark consumes a click.
- **Names.** Jupiter's and the moons' names go through the bodies'
  adjacent-box machinery, which refuses rather than overlaps.
  - The moons are named after the marks, in precedence order, each
    refused on its own.
  - The catalogue's `LabelPlacement` falls back to "under duress"
    placement. The Jovian names do not use it.
- **Spoken.** Each moon is spoken with its state and as a symbol, for
  example "Io, in front of Jupiter (a cartographic symbol, not Io's
  apparent diameter)".
- **Years.** Jupiter is drawn from 1900, and the moons from 2000.
  Outside 1900–2100 nothing is drawn, and the table keeps its refusal.

## What resolves at each scale

These figures are on the 900 × 700 page and are recorded in
`docs/studies/jupiter-on-the-chart/measurements.md`.

- **The triple transit**, 2026-12-11 22:45 UTC at Oslo. From 36° to 2°,
  Jupiter is the symbol and the three moons in front are "in front
  unresolved". Ganymede is drawn from 8°. At 1° all four are drawn,
  three over the true disc.
- **The normal spread**, 2026-03-06 06:00 UTC. Callisto is drawn from
  12°, Ganymede from 6°, Europa from 2°, and Io at 1°.
- **The A4 sheet.** The 42° two-body sheet does not resolve the moons.
  Measured on the sheet's own page at the triple transit, the widest
  field at which every moon not behind Jupiter is drawn is **1°**.
  There, Jupiter's disc is 3.1 mm and each moon's mark 1.1 mm.

## The CI route, as it ran

| PR | head | route | CI run | result | duration |
|---|---|---|---|---|---|
| #488 | `bb53e57b` | wide, 16 paths | 37855071565 | green | 45 min |
| #489 | `b55bc2b9` | wide, 27 paths | 37902650603 | green | 46 min |
| #489 | `aa142b99` | wide, 29 paths | 37927356756 | **evidence failed** | 46 min |
| #489 | `9f2dd5ed` | wide, 31 paths | 37945881527 | green | 45 min |
| #490 | `b677d122` | wide, 35 paths | 37968538132 | green | 46 min |
| #491 | `c754b23c` | wide, 18 paths | 38022257787 | green | 45 min |
| #492 | `9b9621c8` | wide, 44 paths | 38047496940 | green | 49 min |

- **Every display job executed every test** it found, with 0 failed,
  0 aborted and 0 skipped. That included each test a local gate had
  recorded as unexecuted.
- **Post-merge:** `test` and one dispatched `app-image` passed on every
  merge head, as did `pages` where it ran:

  | merge | test | app-image | pages |
  |---|---|---|---|
  | `9b7506a3` | 37892429288 | 37892430971 | |
  | `3b557415` | 37952161672 | 37952170679 | 37952161387 |
  | `77b1139b` | 38011912611 | 38011913534 | |
  | `cd56d46a` | 38040419638 | 38040420383 | |
  | `7746098b` | 38051270940 | 38051275612 | 38051271052 |

- **The gallery room was verified live.**
  - The index, both slide pages and the Sun-and-Moon horizon page are
    byte-identical to a local build of the merge.
  - Both images are byte-identical to the committed files.
  - Each title, caption, alt text and ink line appears as written.
  - Every link answers 200.

## Stopped, failed and incomplete runs

The records are kept under `~/juranometria-runs/runs/`. None of these
runs was rerun into green.

| head | run | what happened | resolution |
|---|---|---|---|
| *(uncommitted, #483)* | pre-flight suite | ratchets and wrong premises: the `doClick` ratchet, the display corpus, a Preferences touch; a test assuming the Sun/Moon dialog computes nothing on open; a real-app hunt through search on a hidden toolbar. The Export dialog's 332→326 capture refusal appeared in a pre-flight only | fixed before `bb53e57b` |
| `aa142b99` (#482) | gate 1 | **incomplete** under the capture-refusal precedent: `PlaceAndTimeSheetMain` refused 420→326 | not rerun |
| `aa142b99` (#482) | gate 2 | **stopped**: the same generator refused twice at 420→324, a width not seen before | the owner ruled it the known kind of transition and recorded gate 2 incomplete too; gates 3–7 green once |
| `aa142b99` (#482) | CI 37927356756 | **evidence failed**: the study report's label table did not reproduce on Linux (236 refusals at 1° against macOS's 225), because label boxes use the platform's font metrics | the owner ruled a platform record; the failed run was kept and not retried; `9f2dd5ed` |
| `9f2dd5ed` (#482) | gate 1 | **incomplete** under the precedent: `ExportSheetDialogSheetMain` refused 332→326. The kept failure folder also held a stale `PlaceAndTimeSheetMain/` from `aa142b99`, which the record names | gates 2–7 green once; CI display executed the test |
| *(uncommitted, #484)* | pre-flight suites | one compile gap (`PageTextTest`'s page words), then six recorded expectations: the module-boundary shapes, the production store doors (13→14), the test-evidence counts, the companion journey, and the language ledger at 153 against its cap | namespaced dotted identifiers kept the ledger at 149; fixed before `b677d122` |
| `c14e7d86` (#485) | gate 5 | **stopped**: the #484 packaged journey still required Jupiter alone on the 8° page and its spoken text to end with Jupiter. With the moons, a distinguishable Ganymede is drawn after it, as ruled | the journey requires Jupiter first; `c754b23c`; full requalification from gate 1, following the #473 precedent for a stale gate expectation |

Every gate run that passed is recorded beside these: `bb53e57b`,
`b55bc2b9`, `aa142b99-from3`, `9f2dd5ed-from2`, `b677d122`, `c754b23c`
and `9b9621c8`.

**The capture-refusal precedent was clarified on #482** (2026-10-09).
"A known fail-closed geometry transition" means the known kind of
transition: the application-sized window replacing the declared
content before proof. It is not an allowlist of exact widths. Exact
rollback dimensions are diagnostic evidence, not the identity of the
transition. A refusal in a later gate on the same head is recorded
incomplete in the same way. Restarting from gate 1 is refused, because
it would only seek a luckier capture. The repository's record is
amended in this pull request
(`docs/decisions/chart-options-companion.md`).

The refusals recur on this machine: Place and Time at 420→326 and
420→324, and the Export dialog at 332→326. They appeared on branches
that touched no capture code. Each was executed successfully by CI.
The cause is not investigated.

## Evidence moved

The canonical evidence contract reported 0 breaches on every qualified
head. The known 17 inspection images drifted and were restored each
time.

| merge | what moved |
|---|---|
| `9b7506a3` (#483) | the test-evidence report, with the hand-off count requoted to 941; no image |
| `3b557415` (#482) | the cartography study: report, platform record, 20 mock-ups. Provenance 475 → **495** |
| `77b1139b` (#484) | the test-evidence report and its decision requoted: 74 files touching process-wide state, 457 reads, 954 hand-offs; no image |
| `cd56d46a` (#485) | nothing under `docs/studies` or `docs/gallery` |
| `7746098b` (#486) | see below |

The #486 merge moved:

- 14 production pages, the report and the platform record;
- `sheet-a4-jovian.png` and `.pdf`;
- two gallery slides and their pages, the gallery manifest and index,
  and the Sun-and-Moon horizon page's next-slide link;
- two new Controller photographs and two re-promoted ones
  (`companion-*-6-solar-system-open`), now showing Jupiter's group, and
  `companion-strings.md`;
- provenance from 495 to **515**, with the two re-promoted photographs
  re-dated;
- the hand-off count, requoted to 956.

**What did not move:**

- No released chart page, sheet or gallery image outside the new room.
- `docs/reference/` is unchanged.
- No core-chart renderer behaviour: the Jovian ink appears only with
  the switch on.
- The language ledger stays at **149**, under its cap of 150.

## What later planets may rely on

- **`OverlayContribution.OblateBody`.** A body with true equatorial and
  polar diameters, a pole angle in the chart's frame, and a minimum
  mark. The page decides when the minimum is a symbol, painting it
  beneath the stars, and when it is the body, opaque.
- **`OverlayContribution.Satellite`.** A body belonging to another, with
  its relation, shadow and precedence. The page decides each satellite
  on its own at its own scale (`SatelliteMarks`), and publishes the
  decision (`ReferenceInk.satelliteDecisions`).
- **Identities are namespaced and dotted**, as in `jovian.system` and
  `jovian.jupiter`. A moon can never be mistaken for the Moon, and the
  language scanner routes them as identifiers. The ledger has one entry
  of headroom.
- **The pole frame.** Derive the pole from the body's own constants at
  its J2000 place, and turn it onto the page through `PageBasis`. Never
  rotate an of-date table angle by an assumed correction.
- **Names.** Use the bodies' refusing name boxes, never a duress
  fallback.
- **Evidence.** Facts that depend on the platform's fonts belong in a
  platform record. Packaged journeys restate constants from study
  tools, pinned by a test, because the packaged image does not reach
  into the tools.
- **The service.** Jupiter is computed for 1900–2100 and the moons for
  2000–2100, as Sprint 44 handed over.

## #481, future architecture, not delivered

A contextual field below 1° is **not** exposed. `ChartViewState`
refuses unsupported fields. Everything this sprint built reads
`ChartViewState.normalMinimumFieldDegrees()` rather than naming 1°,
as ruling 9 requires:

- the satellite decision's floor;
- the sheet's measured field;
- Centre on chart;
- the tests.

The mark minimum, the transition and the distinguishability distances
are in pixels, and the positions are in J2000. A finer rung would
therefore change none of them; it would only add a rung.

## The road to 6.0

- **VERSION stays 5.0.0.** A 6.0.0 needs its own release ruling and
  release notes covering Sprints 44 and 45.
- **Not yet decided:**
  - the discoverability of *Update from Place and Time*;
  - coordinate-based chart titles;
  - how an instant after the exact civil-time interval is marked on the
    chart (the table already marks it; the chart does not);
  - whether the recurring capture refusals on this machine are
    investigated before the release's gates.
- **Excluded this sprint and still excluded:** satellite shadow spots
  on Jupiter, surface features, event search and other planets.
