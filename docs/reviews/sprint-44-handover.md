# Sprint 44 handover — Know Jupiter's little sky

Three issues, four merged pull requests and two owner checkpoints
handed over the numerical Jovian system. The sprint's three issues
ran in order:

- **#472, a contract study.** It chose the authorities and the pack,
  measured the error matrix against JPL Horizons and IMCCE, and
  identified the 11 December 2026 triple transit as the named
  fixture. The owner ruled on all eleven of its decisions.
- **#473, the pack and the computation**, in two changes. The first
  made the SPK reader and writer select a segment by epoch, which
  JUP365's split at 1997 requires. The second built a second ephemeris
  pack and the Jovian service. Its first measurement exceeded four
  ruled targets, each through the atlas's own frozen civil-time
  model. It was returned before any widening, and the owner amended
  the targets.
- **#474, the table.** It shows Jupiter and the four Galilean moons
  for an instant and a sampled range, in the Controller and in a
  dialog host. The owner's packaged checkpoint found a real gap: a
  range omitted Jupiter's own summary. The gap was fixed and accepted.

Six gate runs stopped or were recorded incomplete. Each is listed
below as it happened, and none was rerun into green. Every CI run
passed on its first execution.

Written against `main` at `02c46ccd` (PR #479), after its post-merge
checks (`test` 37812532925, `app-image` 37812550947). No VERSION
change, no tag and no release. **Closing this sprint does not mean
6.0.0 is ready.** It hands trustworthy numbers to the later Jovian
cartography milestone.

## What shipped

| issue | pull request | merged | what |
|---|---|---|---|
| #472 | #476 | `c7e136fd` | the study and the ruled contract (`docs/decisions/jovian-system.md`); 55 Horizons fixtures, the quoted IMCCE rows, mock-ups, the pack measurement |
| #473 | #477 | `f49dc2ff` | change 1: `SpkKernel` and `SpkExcerpt` select a segment by centre, target and epoch; the JUP365 split fixture |
| #473 | #478 | `193c72fe` | change 2: the Jovian pack, `JovianSystemService`, the comparison engine and its report |
| #474 | #479 | `02c46ccd` | the Jupiter table: a Controller group, a dialog host and *View ▸ Solar System ▸ Jupiter...* |

## Owner findings

1. **The range omitted Jupiter's own summary** (#474, packaged
   checkpoint on `6b86b838`). *Over a range* grouped only the four
   moons under each instant. The range therefore answered how the
   moons stand around Jupiter, not where Jupiter is, against the
   ruled contract of a Jupiter summary plus four moon rows per
   instant. **Fixed on `b35e7920` and accepted.** Each range group is
   now one Jupiter result and its four moons:
   - a heading naming the instant;
   - two spanning lines carrying the card's eight values, labelled and
     spelled by the same formatter as the card;
   - the four moon rows.

   The range heading and the table's spoken name say how it is
   grouped. A contract test holds it:
   `everyRangeGroupIsOneJupiterResultAndItsFourMoonsInBothLanguages`
   checks a seven-week range in both languages. It steps by weeks
   because Jupiter is near its stationary point in December 2026,
   measured at about 0.05 s of right ascension an hour, so hourly
   positions repeat at the table's rounding.
2. **Update from Place and Time was easy to miss again** (#474,
   packaged checkpoint). The same question applies to the Sun's and
   the Moon's groups. **Deferred to a later review sprint**, for all
   three bodies together. #474 does not change it.

The first checkpoint was accepted on `193c72fe`'s numbers. The owner
accepted the numerical layer and closed #473 with its totals. The
second checkpoint was accepted on `b35e7920` ("Fixed :-)").

## Authority, licensing and the pack

**Authorities** (ruling 1 of #472):

- **JUP365** (R. A. Jacobson, JPL Solar System Dynamics, 2021) for
  Jupiter and the four Galilean moons. NAIF distributes it as
  `jup365.bsp`, merged with DE440's Jupiter barycentre.
- **JPL Horizons** for the dense comparison.
- **IMCCE** for independently published event rows.

**Terms** (ruling 2):

- The pack is a modified, renamed JUP365 excerpt under NAIF's rules,
  with JUranometria named as modifier in each kernel's comment area,
  a notice, a manifest and a provenance record.
- The IAU 2015 radii and pole are copied verbatim from NAIF's
  `pck00011.tpc` into the manifest. The PCK file itself is not
  redistributed.
- Nothing of IMCCE's is in the application. Only six event rows are
  quoted, as test evidence in `docs/studies/jovian-system/`, with
  attribution, URL, retrieval date and digest.

**The pack** is `src/resources/jovian-system/`, built by
`JovianPackMain` (`make import-jovian-system`). It is one logical,
versioned pack (`pack.version=1`) behind one manifest and one
validation boundary.

| file | content | bytes | sha256 |
|---|---|---:|---|
| `juranometria-jup365-jupiter-1900-2100.bsp` | 0 → 5 (DE440's barycentre); 5 → 599, both JUP365 segments | 13 550 592 | `efddb718…cae92` |
| `juranometria-jup365-galilean-2000-2100.bsp` | 5 → 501, 502, 503, 504, the later JUP365 segments | 39 294 976 | `6ad781a4…99ef6` |
| `NOTICE-jovian-system.md`, `PROVENANCE.md`, `manifest.properties` | | | in the manifest |

- **Why two files.** The ruled intervals differ: Jupiter covers
  1900–2100, the moons 2000–2100 (ruling 3). A Galilean excerpt over
  the whole of 1900–2100 would be 91 MB.
- **Source digests:**
  - `jup365.bsp`: `dbf016c0…c5cb`
  - `pck00011.tpc`: `3dff7b1d…8bb1`
  - `de440s.bsp`, for the barycentre proof: `c1c7feea…49f2`
- **What the builder proves before writing:**
  - each excerpt is identical to its source, on 86 370 and 86 800 states;
  - the barycentre is identical to the released `de440s.bsp`, on
    43 222 states, because NAIF merged DE440's own segment;
  - the released Solar System kernel is untouched.

  A rebuild is byte-identical.
- **Where the notice ships.** The Jovian notice is in the JAR, in
  the archive's `licenses/`, in `packaging/LICENSING.md`, in the
  packaged licensing inventory (now 11 resources), and in the About
  notices view. The About view gained an English and a Norwegian
  heading, and both licensing summaries gained a paragraph. The
  owner approved the Norwegian wording.
- **Cost:** about 90 ms to load, 79 MB of heap held, one
  configuration about 4 ms, a 250-instant range 31–36 ms. Measured on
  this Mac, macOS 27.0.1, three runs. The pack is read on the first
  computation, never at startup.

## Sun and Moon invariance

The released Sun/Moon pack is **byte-identical** to the one 5.0.0
shipped (`63fbf570…5580`). Five things hold it:

- `SpkExcerptTest` reproduces the released kernel byte for byte
  through the epoch-aware writer.
- `make import-solar-system`, run from the pinned source, rebuilt
  every file of that pack identically during #473.
- The Jovian builder verifies the released kernel's digest before
  writing.
- `JovianPackTest` holds the digest in two places.
- `SunInvarianceTest` and `SpkKernelTest` are unchanged and green on
  every head.

The Jovian service reads the Solar System pack's time scales, so one
leap-second record governs the whole sky. A missing or corrupt Jovian
pack refuses the Jovian service alone; `JovianBoundaryTest` proves it.

## Frames, time, light time and the observer

These are frozen in `docs/decisions/jovian-system.md` and built as
`JovianSystemService`.

- **Jupiter, like the Sun and the Moon.** Jupiter is placed at its
  centre (599), with its light time, seen from a sea-level geodetic
  station on the rotating Earth. Place and Time's UT1 = UTC applies.
  The one addition, ruling 8, is the Sun's gravitational deflection:
  it enters the **apparent place of date only**. The chart's
  astrometric J2000 place is untouched. The formula is the NOVAS
  one-body one as Skyfield 1.55 writes it.
- **The moons.** X = Δα·cos δ_Jupiter and Y = Δδ, both from the
  apparent places of date. These are Horizons' apparent differential
  coordinates, verbatim (ruling 5): X is positive toward increasing
  right ascension, east; Y is positive north. The separation is
  measured on the same apparent basis. The position angle is the
  direction of X and Y. Depth is the moon's light-time distance less
  Jupiter's: negative is nearer.
- **Shadow.** It is computed at the moon's emission instant, with the
  up-leg light time to the Sun.

## The apparent figure and the visibility states

- **The figure** (ruling 6). It is the oblate IAU 2015 spheroid, with
  equatorial radius 71 492 km and polar radius 66 854 km. The pole
  comes from the PCK terms: RA 268.056595° − 0.006499°/century, Dec
  64.495303° + 0.002413°/century. The projected polar semi-axis is
  b′ = sqrt(R_p² cos² B + R_e² sin² B) at sub-observer latitude B.
  The disc test is made **in angles throughout**: each body's extent
  is taken at its own distance. Diameters are 2·asin(R/d).
- **The disc relation, limb to limb.** The moon's own radius counts.
  - *clear*: the moon's disc does not overlap Jupiter's.
  - *in front*: the discs overlap and the moon is nearer.
  - *behind*: the discs overlap and the moon is farther.

  Front and behind come from depth, never from X and Y.
- **The shadow relation.** It is judged against the umbra
  R_e − along·(R_☉ − R_e)/D: *sunlit*, *partly in shadow* or *in
  shadow*. The penumbra is not a state.
- **The composed state, in precedence** (ruling 7):
  1. behind Jupiter, in its shadow (the moon behind Jupiter while
     partly or wholly in shadow);
  2. behind Jupiter;
  3. in front of Jupiter;
  4. in Jupiter's shadow;
  5. partly in Jupiter's shadow;
  6. clear of Jupiter.

  Only *clear of Jupiter* and *in front of Jupiter* can be seen.
- **The comparison's definition.** Horizons' equatorial sphere is
  computed beside the reader's figure, for the comparison only.

## Measured accuracy, final totals

The figures come from the report `docs/studies/jovian-system/service-accuracy.md`,
which the contract regenerates. `JovianReferenceVectorTest` holds them
to the targets ruled on #472 and amended on #473, row by row against
the committed Horizons fixtures. Worst cases:

| quantity | through the exact civil-time interval (to 2027-06-28) | after it |
|---|---|---|
| astrometric | 0.011″ (Jupiter 1900–61), 0.0001″ from 1972 — ≤ 0.05″ | 1.69″ — ≤ 3″ *(amended)* |
| apparent, more than 1° from the Sun, with the Sun's deflection | 0.134″, 0.095″ before 1962 — ≤ 0.2″ / ≤ 0.3″ | 1.80″ — ≤ 3″ *(amended)* |
| apparent within 1° of the Sun | measured, not asserted | measured, not asserted |
| X / Y / separation | 0.0007″ / 0.0006″ / 0.0004″ — ≤ 0.05″ | 0.71″ — ≤ 1″ *(amended)* |
| position angle | 0.036° — ≤ 0.05° | printed and classed by conditioning; 0.049° on well-conditioned rows |
| Jupiter's pole angle | 0.002° — ≤ 0.01° *(amended, a new quantity)* | 0.002° |
| horizontal | 11.85″ — ≤ 20″ *(amended)* | not asserted |
| distance | 32.6 km (1900–61) — ≤ 40 *(amended)*; 4.95 km (1962–71) — ≤ 30; 0.22 km from 1972 — ≤ 2 | 5 522 km — ≤ 7 000 *(amended)* |
| diameters, illuminated fraction | 0.0000″, 0.00001 | 0.0002″ |

**What each amendment bounds.** Each is a measured cause in the
atlas's own frozen civil-time model, never an implementation error:

- **Horizontal:** Place and Time's UT1 = UTC beside leap seconds. The
  released Moon shows 11.58″ on the same 1994 date.
- **Distance in 1900–61:** the ΔT model sits about 1.2 s from the
  record, at 28 km/s of range rate.
- **After the exact interval:** the two ΔT predictions name instants
  up to 133 s apart.

**Visibility states:**

- **Through the exact interval**, 13 743 of 13 744 moon rows agree
  with Horizons' code. The one exception is the single allowlisted
  graze: Europa at Oslo, 2026-12-06 17:00, with separation 20.3635″
  against a limb sum of 20.3640″, a margin of half a milliarcsecond
  (ruling 8; the corrected margin, ruling 6 of #473).
- **After the interval** the states are measured output: 17 of 15 364
  rows disagree, all from 2041 on.
- **The oblate figure against the sphere** differs on 24 of 29 108
  rows. Each is named, and each is a graze.

**The named evening, 11 December 2026.** On the sphere, every ingress
and egress minute is Horizons' own minute. On the figure:

| moon | in front of Jupiter (UTC) |
|---|---|
| Io | 22:32–00:52 |
| Europa | 21:53–00:49 |
| Callisto | 18:09–22:57 |
| Ganymede | clear, about 303″ west |

IMCCE's centre-crossing minutes lie inside every limb-to-limb
interval. The four contract instants read as ruled at five observers
and geocentrically: 22:30, 22:45, 22:55 (the definition boundary)
and 23:00.

**Two corrections made before qualification**, both recorded in the
decision record:

- **The disc test.** The study's oblate test, and the first port of
  it, took a moon's lateral offset at Jupiter's distance rather than
  its own. The error was up to 0.02″ at a graze, and it moved
  Callisto's ingress to 18:09. The fixed-ephemeris-time geometry test
  found it.
- **The Europa graze.** The study's margin was 0.32″; Horizons' own
  row gives 0.5 mas.

## The table, its session and the Controller

These are the semantics #474 built:

- **One shared result, two hosts.** `JovianTableSession` holds one
  applied query and result, shared by the Controller's Jupiter group
  and the Jupiter dialog. Each host keeps its own draft. A result
  applied in one host never writes into the other's fields.
- **Where Jupiter sits.** The Jupiter group follows the Moon's under
  Solar System, introduced collapsed. *View ▸ Solar System* ends in
  *Jupiter...*. There is **no Show on chart box**, because nothing
  Jovian is drawn.
- **Nothing computes from building, opening or expanding.** This
  differs from the Sun's and the Moon's dialogs, which compute when
  they open and when they come to the front (ruled on #400). Jupiter
  computes only on Compute, a view change, Enter, or Update from
  Place and Time.
- **One instant.** The Jupiter card shows eight values. Below it,
  Io, Europa, Ganymede and Callisto appear in fixed order, each with
  its side, its east–west and north–south offsets with their letters,
  its separation in arcseconds and in Jupiter radii, and its state in
  words.
- **A range.** Each sampled instant is one group: Jupiter's summary,
  then its four moons. At most 250 instants (1 000 moon rows) are
  shown. The table samples instants and never searches for an
  ingress.
- **Refusals** name their years:
  - Jupiter outside 1900–2100 is refused.
  - An instant before 2000 shows Jupiter's card, and the
    configuration is refused for 2000–2100.
  - A range that leaves the moons' years is refused whole.
- **Estimated instants.** After the exact interval, instants are
  marked *est.*, and the states there are not offered as predicted
  event times (ruling 8 of #473).
- **Every value is the service's, rounded.** `JovianTableTest`
  proves it, together with zero computations and no pack read from
  building, both hosts sharing one result, and every December 2026
  hour at Oslo reading Horizons' state in words in both languages,
  except the allowlisted graze. `SolarSystemCompanionJourneyTest`
  proves the same in the real application. The packaged acceptance
  runs the journey inside every image (`jupiter table OK`).

## The CI route, as it ran

| PR | head | predicted | actual | CI run | duration |
|---|---|---|---|---|---|
| #476 | `06a5a0e9` | wide (registries) | wide | 37644235901 | 47 min |
| #476 | `b897bfbc` | wide | wide | 37659829714 | 45 min |
| #477 | `7559cf3c` | wide (`SpkKernel` is chart-reached) | wide | 37685981183 | 46 min |
| #478 | `a8308c76` | wide (service and pack) | wide | 37752352546 | 37 min |
| #479 | `6b86b838` | wide (UI and `JUranometriaMain`) | wide | 37779246167 | 45 min |
| #479 | `b35e7920` | wide | wide | 37804197331 | 45 min |

- **Every CI run was green on its first execution.**
- **Every display job executed every test**, with 0 failed, 0 aborted
  and 0 skipped. That included each test a local gate had recorded as
  unexecuted.
- **Post-merge:** `test`, `pages` (where it was triggered) and one
  dispatched `app-image` passed on every merge head: `c7e136fd`,
  `f49dc2ff`, `193c72fe` and `02c46ccd`.
- **No hosted job failed to be acquired.** There was no
  infrastructure non-execution.

## Stopped, failed and incomplete runs

The records are kept under `~/juranometria-runs/runs/`. None of these
runs was rerun into green.

| head | run | what happened | resolution |
|---|---|---|---|
| `fc9c305d` (#472) | gate 1 | **stopped**: the test-evidence report no longer reproduced from its generator | regenerated as `06a5a0e9` |
| `06a5a0e9` (#472) | gate 1 | **incomplete** under the capture-refusal precedent: `PlaceAndTimeSheetMain` refused 420→326, recorded unexecuted with all seven conditions met | gates 2–7 green once; CI display executed the test; the owner's rulings followed, then a full requalification on `b897bfbc` |
| `c225c4f2` (#477) | gate 1 | **stopped**: the test-evidence report again, and the scan filed a `.bsp` fixture as renderer-drawn | a kernel fixture is classed as byte-exact data; `7559cf3c` |
| *(uncommitted, #478)* | pre-flight suite | two product findings: the ephemeris-ownership guard and the language ledger | fixed before `ffd64a03` |
| `ffd64a03` (#478) | gate 1 | **incomplete** under the precedent: Place and Time 420→326, and `SheetCaptureSizingTest` rolling back to 300; all seven conditions met | gates 2–4 green once |
| `ffd64a03` (#478) | gate 5 | **stopped** on the external gate script's stale inventory count of 10; the packaged product itself passed with 11 | on the owner's word, gates 5–7 run once |
| `ffd64a03` (#478) | gate 6 | **stopped** on a real defect: `scripts/verify-dist.sh` listed the Jovian notice out of `LC_ALL=C` order | one-line fix in `a8308c76`; full requalification from gate 1 |
| *(uncommitted, #479)* | pre-flight suite | four findings: the `doClick` ratchet, the test-evidence report with the decision quoting its hand-off count (937 → 938), and the submenu order | fixed before `6b86b838` |
| `6b86b838` (#479) | owner checkpoint | **not accepted**: the range omitted Jupiter's summary | fixed in `b35e7920`; full requalification; accepted |

Every gate run that passed is recorded beside these:
`06a5a0e9-from2`, `b897bfbc`, `7559cf3c`, `a8308c76`, `6b86b838` and
`b35e7920`.

## Evidence moved

**No released chart, export, sheet or gallery byte moved.** The
canonical evidence contract reported 0 breaches on every qualified
head. The known 17 inspection images drifted and were restored each
time, as on every run since macOS 27.0.1.

What moved, by pull request:

- **#476** added the Jovian study: its report, a platform record,
  17 mock-ups, 55 Horizons fixtures, the IMCCE quote and the kept
  measurements.
- **#477** added the JUP365 split fixture
  (`docs/studies/jovian-system/spk/`) and regenerated the
  test-evidence report.
- **#478:**
  - added the Jovian pack and the registered accuracy report
    (`service-accuracy.md`);
  - extended `about-strings.md` and re-promoted **four About images**
    (`about-{en,nb-NO}-compact-{light,dark}`), moving **four provenance
    rows**. The only change is the scrollbar thumb, 166 pixels at
    x 541–546, because the licensing summary grew by one paragraph;
  - added three reviewed language-ledger rows, keeping the ledger at
    149 under its cap of 150;
  - corrected the study's record of the Europa graze and of the
    oblate minutes.
- **#479** regenerated the test-evidence report and requoted the
  decision record's hand-off count. No image moved: the Controller's
  photographers build the Solar System section without the Jupiter
  group.

## What cartography may rely on

The later Jovian cartography milestone may treat the following as
contracts. Each is held by a test.

- **The chart position.** `JupiterObservation.astrometricJ2000()` and
  each `MoonPlace.astrometricJ2000()`: topocentric ICRS/J2000 with
  light time, no aberration and no deflection. This is the frame the
  chart is drawn in, the same as the Sun's and the Moon's.
- **The apparent quantities.** `apparentOfDate()`, X and Y, the
  separation and the position angle are of date and on the apparent
  basis. They are reader quantities, not chart coordinates.
- **Size and figure.** Equatorial and polar diameters, the pole's
  position angle from true-of-date north through east, and the
  sub-observer latitude B. The PCK constants come through
  `JovianPack.constants()`.
- **Depth and states.** Depth; the disc relation on the figure; the
  shadow relation; the composed state and its precedence.
  `sphericalDiscRelation` is for comparison only.
- **Time and intervals.** `timeConfidence()` on every observation.
  Jupiter covers 1900–2100 and the moons 2000–2100, and the service
  refuses by name outside those years.
- **Cost:** about 90 ms to load, about 4 ms a configuration, 79 MB
  held.

## What cartography must still measure

- **The frame for drawing the figure.** The pole angle and the
  apparent offsets are of date. The chart is J2000, so drawing
  Jupiter's figure or the moons' arrangement on it needs the rotation
  between of-date north and J2000 north at Jupiter's place, measured
  and bounded. The study found the moons' offsets 6″ apart between the
  astrometric and apparent bases (Callisto, 1901). Cartography must
  rule which basis the chart's marks follow, and say so.
- **Scale.** Jupiter is 30–50″ across, and the moons are at most
  about 10′ away. At which fields is the disc wider than a pixel,
  and are the moons separable from Jupiter at all? True-scale against
  symbolic marks, as the Sun's and the Moon's cartography measured.
- **What a state looks like.** A moon in front of Jupiter, behind it
  (hidden), or in its shadow (dark). Whether and how a shadow
  transit is drawn; the issues excluded shadow drawing here.
- **Labels.** Four moon names within arcminutes of a bright planet,
  against the star labels.
- **Interaction.** Whether a Jupiter mark follows the Sun and Moon's
  Show on chart model: one switch, the Controller's box and the View
  item. Whether the Update from Place and Time question deferred above
  is settled first.
- **After 2027.** How an estimated instant's mark is distinguished on
  the chart, as the table distinguishes it.

## Deliberately unsettled

- **The discoverability of Update from Place and Time.** This is the
  owner's deferred finding, for the Sun, the Moon and Jupiter
  together.
- **The new Norwegian sentences of #474.** These are the
  explanations, refusals, status lines, time note and menu entry,
  beyond the study's ruled words. They await the owner's review.
- **The Controller photographers.** They build the Solar System
  section without the Jupiter group, so the committed photographs do
  not show it yet.
- **VERSION stays 5.0.0.** A 6.0.0 needs the cartography milestone
  and its own release ruling.
