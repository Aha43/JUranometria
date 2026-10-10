# Release handover — JUranometria 5.1.0, Jupiter

5.1.0 is the Jupiter release. It adds no feature beyond what Sprints
44 and 45 built and the owner accepted. Jupiter and its four Galilean
moons are:

- computed offline;
- explained in tables;
- centred on from the numerical surfaces;
- drawn honestly on the chart;
- exported to paper;
- presented in the public gallery.

Sprint 46 adds no product change. It settled how a merge is qualified
(#494), and it carries this release (#495).

The full records are the two sprint handovers,
`docs/reviews/sprint-44-handover.md` and
`docs/reviews/sprint-45-handover.md`. This document names what the
release rests on and where each fact is held. It does not repeat
their tables.

Written against `main` at `7d74b321` (PR #496, #494's merge). VERSION
is still 5.0.0 here. It changes only after the owner accepts the
packaged candidate.

## What a reader gets

- **Jupiter and its four moons, computed offline.** A second bundled
  ephemeris pack covers Jupiter from 1900 to 2100 and Io, Europa,
  Ganymede and Callisto from 2000 to 2100.
- **The Jupiter table**, in the Controller's Solar System section and
  in *View ▸ Solar System ▸ Jupiter...*, in English and Norwegian.
  - **For an instant:** Jupiter's position, height, distance, apparent
    diameter at the equator and the poles, illuminated fraction and
    the north pole's angle.
  - **For each moon:** its side of Jupiter, east–west and north–south
    offsets, separation in arcseconds and in Jupiter radii, and
    whether it is clear of Jupiter, in front of it, behind it or in
    its shadow.
  - **Over a range:** each sampled instant is one group, Jupiter's full
    summary and then its four moons.
- **Centre on chart** for the Sun, the Moon and Jupiter, from the
  Controller and from each table. It computes what is typed, turns the
  body's chart switch on, centres the chart at 1° and brings it
  forward.
- **Jupiter on the chart.** *Jupiter and moons on the chart* is off by
  default and remembered. It is in *View ▸ Solar System* and in the
  Jupiter group.
  - At ordinary fields Jupiter is a 6-pixel cartographic symbol beneath
    the stars, and the chart says so.
  - From 6 pixels across, at the 1° field, it is its true disc, turned
    towards its flattened figure.
- **The four moons**, as 3-pixel symbols at their own places:
  - a filled dot when clear of Jupiter;
  - a ringed dot over the disc when in front of it;
  - a hollow ring in its shadow;
  - nothing when behind it.

  At 1° every moon not behind Jupiter is drawn; at wider fields only
  those whose marks can be told apart.
- **On paper.** Exported sheets draw Jupiter and its moons exactly as
  the screen does. The A4 Jovian sheet is measured at 1°.
- **In the gallery.** A room, *Jupiter and its moons*, at the
  gallery's shared moment.

## Authorities, the pack, its intervals and licensing

- **Authorities** (#472):
  - JPL's **JUP365** (R. A. Jacobson, 2021) for Jupiter and the moons;
  - **JPL Horizons** for the dense comparison;
  - **IMCCE** for independently published event rows, quoted as test
    evidence only.
- **The pack.** `src/resources/jovian-system/` is one versioned pack
  behind one manifest, built by `make import-jovian-system`. It holds
  two renamed JUP365 excerpts:

  | file | bytes | covers |
  |---|---:|---|
  | `juranometria-jup365-jupiter-1900-2100.bsp` | 13 550 592 | Jupiter, 1900–2100 |
  | `juranometria-jup365-galilean-2000-2100.bsp` | 39 294 976 | the moons, 2000–2100 |

  Each excerpt is proved identical to its source before it is written,
  and a rebuild is byte-identical. The IAU 2015 radii and pole are
  copied verbatim into the manifest from NAIF's `pck00011.tpc`, which
  is not redistributed.
- **Licensing.** The pack is a modified, renamed excerpt under NAIF's
  rules, with JUranometria named as modifier in each kernel. Its
  notice, `NOTICE-jovian-system.md`, ships in five places:
  - in the JAR;
  - in the archive's `licenses/`;
  - in `packaging/LICENSING.md`;
  - in the packaged licensing inventory, which now has 11 resources;
  - in the About notices view, in English and Norwegian.

  The owner approved the Norwegian wording in Sprint 44.
- **Offline.** The pack is read on the first computation, never at
  startup. Nothing is downloaded at runtime.
- **The Sun/Moon pack is byte-identical** to the one 5.0.0 shipped,
  held by five checks (Sprint 44 handover, "Sun and Moon
  invariance").

## The measured error model and its honest limits

These are held row by row by `JovianReferenceVectorTest` against the
committed Horizons fixtures. The full table is in the Sprint 44
handover, "Measured accuracy, final totals".

| quantity | through the exact civil-time interval (to 2027-06-28) | after it |
|---|---|---|
| astrometric | 0.011″ worst (1900–61), 0.0001″ from 1972 | 1.69″ worst |
| apparent | 0.134″ worst | 1.80″ worst |
| the moons' X, Y and separation | under 0.001″ | 0.71″ worst |
| Jupiter's pole angle | 0.002° | 0.002° |

- **The amended targets** each bound a measured cause in the atlas's
  frozen civil-time model, never an implementation error:
  - Place and Time's UT1 = UTC gives horizontal coordinates within 20″;
  - the ΔT model gives distance within 40 km in 1900–61;
  - after the exact interval, the two ΔT predictions name instants up
    to 133 s apart.
- **Visibility states.** Through the exact interval, 13 743 of 13 744
  moon rows agree with Horizons. The one exception is a single named
  graze: Europa at Oslo, 2026-12-06 17:00, half a milliarcsecond
  inside the limb sum. After the interval, states are measured
  output, never predicted event times. The table marks those instants
  *est.*, and the chart does not distinguish them; that is recorded
  as open, below.
- **The named verification case.** The triple transit of 11 December
  2026 at Oslo:
  - Io in front of Jupiter 22:32–00:52;
  - Europa 21:53–00:49;
  - Callisto 18:09–22:57;
  - Ganymede clear.

  Every minute is Horizons' own on the sphere. It is a documented
  verification case, not an event-search feature.

## Frames, and the chart/table boundary

- **The chart** draws each body at the service's **astrometric J2000**
  place, through the page's ordinary projection, like the Sun and the
  Moon.
- **The table** shows the **apparent, of-date** quantities: X, Y,
  separation, position angle and the pole angle. These are reader
  quantities, never chart coordinates. The table's X and Y differ
  from the J2000 offsets by up to 6.0″, so the chart never reuses
  them.
- **Jupiter's pole on the chart** is derived from the PCK pole vector
  at Jupiter's J2000 place and turned onto the page through its own
  north and east. It agrees with the table's of-date angle, carried
  back by the atlas's rotation, to 0.003°. The two frames' angles
  differ by up to 0.58°.

## Cartographic marks, scale, states and labels

These are the owner's rulings on #482, in
`docs/decisions/jovian-cartography.md`.

- **Jupiter.** Its mark is 6 px or its true size, whichever is
  larger.
  - **While the minimum stands in:** the mark is *a cartographic
    symbol, not Jupiter's apparent diameter*. It is painted beneath
    the stars, hides nothing, and a click passes through it.
  - **From 6 px:** it is the true disc, opaque. Its outline turns from
    a circle to the true axis ratio by 12 px, with the minor axis
    along the pole.
  - **At 1°** the flattening is drawn but not claimed visible.
- **The moons.** Each is a 3 px symbol, decided on its own:
  - behind Jupiter, omitted;
  - at the chart's normal minimum field, drawn at its exact place,
    overlap allowed;
  - above it, drawn only where its own mark is distinguishable.

  A collision suppresses only the lower-precedence mark.
- **Vocabulary A,** on both palettes:

  | state | mark |
  |---|---|
  | clear | filled dot |
  | in front | dot ringed in the page's ground, over the disc |
  | wholly or partly in shadow | hollow ring |
  | behind | nothing |

- **What resolves at each scale** on the 900 px page:
  - at the triple transit, the three moons in front resolve only at 1°;
  - at an ordinary spread, Callisto from 12°, Ganymede from 6°, Europa
    from 2° and Io at 1°;
  - on A4, the widest field that resolves every moon at the triple
    transit is 1°.

  These are measured in `docs/studies/jupiter-on-the-chart/`.
- **Labels.** Names go through the bodies' adjacent boxes and are
  refused one at a time when none is clean. They never overlap, and
  never use the catalogue's "under duress" fallback. Each moon is
  spoken with its state and as a symbol.

## The two owner findings repaired at the Jupiter table checkpoint (#474)

1. **The range omitted Jupiter's own summary.** It was fixed on
   `b35e7920` and accepted. Each range group is now Jupiter's result
   and its four moons, held in both languages.
2. **Update from Place and Time was easy to miss.** This was deferred
   to a review sprint, for the Sun, the Moon and Jupiter together.

## Every stopped, failed and incomplete run

None was rewritten as green. The full tables, with heads and
resolutions, are in the sprint handovers. In summary:

- **Sprint 44**, in its handover:
  - *Gate 1 stopped twice* on stale test-evidence reports (`fc9c305d`,
    `c225c4f2`).
  - *Gate 1 was recorded incomplete twice* under the capture-refusal
    precedent (`06a5a0e9`, `ffd64a03`).
  - *Gate 5 stopped* on a stale gate-script count, and *gate 6* on a
    real `verify-dist.sh` ordering defect (both `ffd64a03`).
  - *The #474 checkpoint was not accepted* until the range was fixed
    (`6b86b838`).
- **Sprint 45**, in its handover:
  - *Gates 1 and 2 were recorded incomplete* (Place and Time
    420→326/324) on `aa142b99`.
  - *CI on `aa142b99` failed:* the evidence report's label table
    depended on fonts and became a platform record.
  - *Gate 1 was recorded incomplete* (Export dialog 332→326) on
    `9f2dd5ed`.
  - *Gate 5 stopped* on a stale packaged expectation (`c14e7d86`).
- **Sprint 46, #494:**
  - *A pre-flight suite* met the Export dialog's 332→326 refusal. It
    did not recur in gate 1.
  - *The first CI run on `4af73326`* (38069537922) was **invalid for
    the wide route**. `evidence` was skipped behind the skipped
    `landing` job. It was not retried, and was fixed on `b66eeb2a`
    with a full requalification. The record is
    `runs/ci-j494-4af73326-INVALID.md`.

Every CI display job executed every test it found, with 0 failed,
0 aborted and 0 skipped. That includes each test a local gate had
recorded as unexecuted. No hosted job failed to be acquired.

## Evidence and provenance, and what stayed byte-identical

- **Provenance grew** from 475 promoted artifacts, at the start of
  Sprint 45, to **515**:
  - Sprint 44 added the Jovian study and its fixtures;
  - #482 added 20 mock-ups;
  - #486 added the production pages, the A4 sheet, two gallery slides
    and two Controller photographs, and re-dated two.
- **Byte-identical:**
  - every released chart page, sheet and gallery image outside the new
    room;
  - `docs/reference/`;
  - the Sun/Moon pack.

  The canonical evidence contract reported 0 breaches on every
  qualified head. The known 17 inspection images were restored each
  time.
- **The language ledger** stays at 149, under its cap of 150.
- **The gallery room was published and verified live:** pages and
  images byte-identical to the build and to the committed files, every
  link answering 200.

## How merges were qualified

Through Sprint 45, every merge was followed by a push-triggered `test`
and one dispatched `app-image`.

Since #494 (`docs/decisions/post-merge-qualification.md`), a merge
proved to be the exact trial merge its pull request already qualified
is verified structurally by `main`'s `landing` check, not rerun. The
proof requires the accepted head, the base, the trial-merge tree, the
parents and the route's required jobs. Anything unproved runs the full
route. Main-only deployments and every release check still run.

The first application was #494's own landing, `7d74b321`. It was
verified, and its suites were skipped in 24 seconds.

## Accepted boundaries

- No cloud belts, Great Red Spot, surface rotation, satellite surface
  detail or satellite shadow spots.
- No event search and no notifications.
- No zoom below 1°. #481 remains future architecture; everything
  built reads the chart's minimum field rather than naming 1°.
- The discoverability of *Update from Place and Time* is deferred to a
  review sprint.
- No other planet.
- VERSION becomes **5.1.0**, not 6.0.0. The road to 6.0 continues
  planet by planet.

## What later planets reuse

These are recorded in the Sprint 45 handover, "What later planets may
rely on":

- the oblate-body and satellite contributions, with page-scale
  decisions published read-only;
- the minimum-mark rule: a symbol beneath the stars, and the true disc
  above them;
- namespaced dotted identities;
- the pole derived from the body's own constants at its J2000 place;
- refusing name boxes;
- platform records for font-dependent facts;
- packaged journeys that restate study constants under a pinning test.

## Still open after 5.1.0

- the discoverability of *Update from Place and Time*;
- coordinate-based chart titles (the owner's note at #483);
- how the chart marks an instant after the exact civil-time interval;
- the capture refusals that recur on this Mac and have not been
  investigated.
