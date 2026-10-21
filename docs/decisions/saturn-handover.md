# Saturn, numerically: the handover to cartography

**Sprint 52, issue #577**, closing Part I of the road to 5.4.0 (#573).
This record hands the settled numerical Saturn contract to the
cartography and release milestone (Part II, Sprint 53: #578-#581). It
records; it builds nothing. Where it quotes a number, the number is a
report's, a fixture's or a run record's, named beside it; where it
quotes a ruling, the ruling is the owner's, dated. The contract itself
is `saturn-system.md` (the study, the rulings of #574, the #575
checkpoint rulings, #575 and #576 as built); this document says what
stands, what was measured, what stopped, what moved, and what the next
milestone must decide.

**Two baselines, kept apart:**

- **The Saturn feature work** (#574-#576) was complete at main
  `58ffa6d7` (2026-10-20). Every Saturn number, file and table in
  section 1 is as of that main.
- **The infrastructure repair chain** this handover needed before it
  could qualify (section 2.2): #587's own CI stop, then #588 and #590,
  four capture sizing tests repaired in the test file only. The chain
  ends at main `224058e0` (2026-10-21), this handover's baseline. It
  changed no production code, no Saturn result and no evidence.

No `VERSION` change (5.3.0), no tag, no release.

## 1. What is settled

### 1.1 Authority, pack and licensing (#574, rulings 1-2; #575)

- **Authority.** NAIF's `sat441.bsp` for Saturn's centre (6 → 699) and
  the ruled moons (6 → 601-606, 608), with Saturn's barycentre (0 → 6)
  sat441's own, bit-identical to the released `de440s.bsp`'s; IAU 2015
  constants from `pck00011.tpc` (radii 60268, 60268 and 54364 km; the
  pole with no nutation terms); the PDS Ring-Moon Systems Node's *Rings
  of Saturn* table for the ring radii, NSSDCA as a cross-check only.
  JPL Horizons is the in-family comparison (it answers from `sat441l`
  and DE441); IMCCE's TASS 1.6 phenomena (2006-2013) the independent
  event catalogue; DE442s measurement only - it moves Saturn's direction
  by at most 0.0001953″ (`authority-comparison.md`).
- **Pack.** A Saturn pack beside the untouched released packs, in
  Jupiter's layout (a planet kernel and a separate moons kernel), from
  `make import-saturn-system`, rebuilt byte-identically
  (`saturn-system.md`, *The pack, as built*):

  | file | bytes | gzip -9 | SHA-256 |
  |---|---:|---:|---|
  | `juranometria-sat441-saturn-1900-2100.bsp` | 16,516,096 | 14,846,477 | `28438fff…` |
  | `juranometria-sat441-moons-2000-2100.bsp` | 62,829,568 | 56,502,957 | `8844bf08…` |
  | the two kernels | 79,345,664 | 71,349,434 | |

  The 43,008 bytes above ruling 2's cut figure (79,302,656) are the two
  comment areas - NAIF's required modification notice and sat441's own
  comments kept as attribution - accepted on the #575 checkpoint; no
  ephemeris byte differs from the source. The manifest carries the PCK
  values and the four ruled ring radii verbatim.
- **The superseded figure.** The checkpoint's 43,002,424 bytes was a
  sum of per-body records, not a pack; the cut figures are the record
  and per-body sums stay labelled as uncut estimates (#574, recorded
  with the ruling).
- **Licensing.** NAIF's rules for modified kernels, stated in
  `NOTICE-saturn-system.md`; the licensing inventory now has 13
  resources, and the archive, `LICENSING.md` and the About notices in
  both languages carry it. IMCCE's files are never committed, bundled
  or republished: 24 rows are quoted with attribution
  (`imcce-quoted-rows.txt`). The PDS and NSSDCA pages state no terms;
  their values are quoted with the table's footnotes.
- **Fixtures.** 74 Horizons responses committed (25,676,145 bytes), the
  nine geocentric matrices retained outside Git with their digests in
  `horizons/README.md`; the two ring pages, their extraction and record;
  `SaturnFixturesTest` re-hashes each file.

### 1.2 Interval, time and frames

Saturn and its barycentre 1900-2100; the moons 2000-2100, refused
outside by name (ruling 1). The time model is inherited unchanged: UT1
before 1972, UTC to the leap-second record's expiry on 2027-06-28 (read
from `Leap_Second.dat`), the ΔT model before and after; observers at
sea level on WGS84. The chart position is topocentric astrometric
ICRS/J2000, light-time corrected; apparent places of date are aberrated
and deflected by the Sun, as Jupiter's.

### 1.3 Every observable and its meaning (ruled)

- **Saturn:** distance to the light-time-corrected centre; the
  equatorial diameter 2·asin(R_e/Δ); **the polar diameter at the ring
  opening B**, 2·asin(b′/Δ) with b′ = √(R_p² cos² B + R_e² sin² B);
  illuminated fraction (1 + cos i)/2; the north pole's position angle of
  date.
- **The ring opening B is planetocentric** - the angle between the line
  of sight and the ring plane over the PCK pole - and Horizons'
  planetodetic `ObsSub-LAT` is never shown as the opening (ruling 3). The
  two differ by up to 18,196″; the widest opening of 1900-2100 is
  −26.995° (2003-04-07) where Horizons prints −32.049°.
- **The opening to the Sun B′** is taken where Saturn sees the Sun:
  light-timed and aberrated by Saturn's own motion. Without that
  aberration the Sun's ring-plane crossings fall 1.31-1.33 h early.
- **The unlit face** is seen when B and B′ differ in sign: 248 of the
  10,488 weekly Oslo rows of 1900-2100 (`measurements.md`).
- **The rings on the sky:** two flat annuli normal to the PCK pole -
  74,491 km to 117,570 km (the C Ring's inner edge to the B Ring row's
  outer edge) and 122,050 km to 136,770 km (the Cassini Division's outer
  edge to the A Ring's outer edge) (ruling 4). Each edge's apparent
  semi-axes are asin(R/Δ) and asin(R/Δ)·|sin B|, the major axis at the
  pole angle + 90°. **The near half**, in front of the globe, lies away
  from the projected north pole when B > 0 and toward it when B < 0: a
  ring point toward the observer projects at −sin B cos B along the
  projected pole. No minor rings, other gaps, thickness or shadows.
- **The moons:** Horizons' apparent-of-date X/Y (X toward increasing
  right ascension, Y north), separation, position angle and separation
  in Saturn radii; front or behind from the line-of-sight depth, never
  from X/Y. Their states are Jupiter's six with Saturn's name - behind
  Saturn in its shadow, behind Saturn, in front of Saturn, in Saturn's
  shadow, partly in Saturn's shadow, clear of Saturn - decided
  limb-to-limb on **Saturn's flattened figure** and shadow cone (ruling
  5). Horizons' sphere is the comparison's definition only.
- **The words** in English and Norwegian as ruled (ruling 6), including
  *edge-on*, the unlit face, *Avstand fra Saturn* and *Saturnradier*.

### 1.4 Moon inclusions and exclusions (ruling 2, 2026-10-19)

**In:** Mimas, Enceladus, Tethys, Dione, Rhea, Titan and Iapetus. The
study proposed five; the owner added Mimas and Enceladus - "Enceladus is
too important to omit from a 'major moons' feature", Mimas "is iconic
and completes the classical inner-moon sequence". **Out of 5.4.0:**
Hyperion - the faintest (APmag 13.93-14.86), the most irregular PCK
figure, and thousands of km between IMCCE's and sat441's positions.

From the weekly Oslo matrix (`measurements.md`), what Part II draws:

| moon | separation ″ (min-max) | diameter ″ (max) | APmag | rows not clear of Saturn |
|---|---|---:|---|---:|
| Mimas | 0.3-32.2 | 0.0714 | 12.60-13.53 | 972 of 10,488 |
| Enceladus | 0.0-40.9 | 0.0881 | 11.40-12.33 | 553 |
| Tethys | 0.6-50.4 | 0.1848 | 9.90-10.83 | 380 |
| Dione | 0.5-64.6 | 0.1934 | 10.10-11.03 | 227 |
| Rhea | 0.0-90.4 | 0.2627 | 9.40-10.33 | 121 |
| Titan | 1.8-213.0 | 0.8849 | 8.02-8.95 | 28 |
| Iapetus | 7.9-625.5 | 0.2567 | 10.80-11.74 | 6 |

No magnitude is printed for any body; Horizons' Saturn magnitude
includes the rings only in part of the geometry, and its Iapetus model
does not state whether it covers the two-toned hemispheres.

### 1.5 Measured errors, by era (`service-accuracy.md`, held by `SaturnReferenceVectorTest`)

Every value inside its ruled target - #574's ruling 10, amended on the
#575 checkpoint (2026-10-19) where the time model is the cause:

| quantity | target | 1900-1961 | 1962-1971 | 1972 → 2027-06-28 | after |
|---|---|---:|---:|---:|---:|
| Saturn astrometric | 0.01″; 3″ after | 0.0059″ | 0.0018″ | 0.0000″ | 0.6840″ |
| Saturn apparent, > 1° from the Sun | 0.3″ before 1962, 0.2″; 3″ after | 0.0881″ | 0.0837″ | 0.1364″ | 0.8085″ |
| Saturn distance | 40 km before 1962, 20 km; 7,000 km after | 33.43 km | 5.98 km | 0.22 km | 3,877.47 km |
| B, on Horizons' reading | 0.01″; 1″ after | 0.0046″ | 0.0022″ | 0.0018″ | 0.3831″ |
| B′, on Horizons' reading | 0.01″; 1″ after | 0.0026″ | 0.0019″ | 0.0018″ | 0.1031″ |
| moons X, Y, separation | 0.5″ | - | - | 0.0006″ | 0.3031″ (Mimas X) |
| moons apparent | 0.2″; 3″ after | - | - | 0.1329″ | 1.0153″ |
| moons position angle | 0.1°; 0.2° after, well-conditioned rows | - | - | 0.0361° (Iapetus) | 0.1990° (Mimas) |
| states on Horizons' sphere | exact; measured output after | - | - | 1,819 of 1,819 rows, each moon | 31 rows differ, printed |

The pole angle meets its 0.5″ in every era; the horizontal place is at
most 11.98″ through the exact interval against the inherited 20″. After
the expiry, ill-conditioned near-centre rows print their position angle
without asserting it (Mimas 1,901 of 3,841 rows, Iapetus none). On the
flattened figure the states differ from the sphere's on 159 (Mimas), 93
(Enceladus), 59 (Tethys), 32 (Dione), 24 (Rhea) and 4 (Titan, Iapetus)
weekly rows: deliberate boundary differences, named. IMCCE uses the
flattened figure: 95 % of sat441's event times fall within 111.3 s of
IMCCE's on it, against 966.3 s on the sphere (`imcce-comparison.md`;
recorded, not gated).

### 1.6 Named cases, rivals and honest limits

- **The card's instant, 2026-10-05 00:00 UTC at Oslo** (the 2026 row
  nearest the Earth, 8.434258 AU): the rings open 7.43° to the south
  face, which is lit; Tethys is behind Saturn in its shadow; the other
  six moons are clear.
- **The 2025 ring-plane crossings**: the Earth's at 2025-03-23 17:51 and
  the Sun's at 2025-05-06 15:35 (Oslo) agree with Horizons to the
  minute; all 44 crossings of 1900-2100 (30 Earth, 14 Sun) within a
  minute through the exact interval, drifting to 2 minutes by 2098.
  ***Ruling 8, stated plainly:* ring-plane crossings and oppositions are
  validated only against Horizons**, which answers from the same sat441
  family and DE441; no independent published authority checks them.
- **Titan's greatest elongations**, 2026-08-01 18:00 and 2026-08-09
  20:00 (hourly at Oslo), within 0.001″ of Horizons.
- **IMCCE's 21 quoted events** for the seven ruled moons, each found by
  the reader's relations within three hours of IMCCE's contact. These
  mix two theories with two definitions of contact; #574's
  centre-to-centre comparison is the like-for-like one.
- **Rivals** (`SaturnMutationTest`): Saturn's barycentre for its centre,
  the planetodetic latitude for the opening, an unaberrated Sun for B′
  and Iapetus's offsets from the J2000 places each miss their target.
- **No ring-obscuration state** (ruling 5): the geometry is measurable -
  Tethys's sight line crosses the rings in 13 weekly rows behind them
  and 13 before - but neither Horizons nor IMCCE accounts for the rings,
  so it is not a reader's state.

### 1.7 Existing-body invariance

The builder refuses to write unless the released Solar System, Jovian
and planets kernels carry their released digests, and `SaturnPackTest`
holds all five. No released service changed: the Saturn service is new
code beside them, its deflection held equal to Jupiter's
(`SaturnServiceTest`). The released Sun, Moon, Mercury, Venus and
Jupiter reference tests ran unchanged on every head, and the Sun's,
Moon's, Mercury's, Venus's and Jupiter's tables and sheets stand.

### 1.8 The Saturn table and its verdict (#576)

The Controller's Saturn group after Jupiter's, introduced collapsed,
and View > Solar System > *Saturn...* after *Jupiter...*; one shared
session with host-local drafts; building, opening, expanding or
restoring computes nothing, and the 79 MB pack is read on the first
computation. The eleven-line card (with the ring opening to the
observer and to the Sun, each with its face, *edge-on* exactly when the
shown value is 0.00°, the unlit face named, and the rings' apparent
size), then the seven moons outward from Saturn in Jupiter's columns. A
range is grouped by instant: three summary lines of Saturn (position,
figure, rings) and its seven moons, at most 142 instants within the
1,000-row cap; before 2000 Saturn answers and the moons are refused
with their years. No Show on chart, no Centre on chart. Packaged
checkpoint on candidate `09d4537b`: **accepted by the owner
(2026-10-20).**

### 1.9 Where the contract lives

`saturn-system.md` (study, the rulings, the #575 checkpoint rulings,
#575 and #576 as built); `docs/studies/saturn-system/` (`SOURCES.md`,
`rings/`, `horizons/`, `authority-comparison.md`,
`imcce-comparison.md`, `measurements.md`, `contract/`,
`service-accuracy.md`, `table-measurements.md`, the mock-ups).

## 2. Late native resizes: #584, and the repair chain before this handover (under #362)

### 2.1 #584, during #576

Gate 1 on #576's `553e1c0b` stopped on
`SheetCaptureSizingTest.invalidOrUnrealizableDeclarationsAreRefused`: an
expected refusal did not happen. The owner ruled (2026-10-19) that the
capture-refusal precedent does **not** extend to such a failure, and
that it be diagnosed and landed separately before #576 continued. The
cause, in the test and not in production: on macOS a native window
answers an earlier resize request later than Java reads it back,
carrying the whole frame with no Java call, and can land inside a later
resize and revert it. The test's narrow window clamped only
`setSize(Dimension)` while the policy asked through `setSize(int,int)`
(`7a81a0f2`); two siblings were late answers to their setup's `pack()`
and now create their window at its packed frame (`41765d1c`). No wait,
retry, assumption or validation change; 30 of 30 runs of the class
afterwards. Landed as `56fbb819` (PR #585).

**Observation, not acted on:** the recurring export-dialog capture
refusal (`ExportSheetDialogSheetMain`, established 333x223 then proved
at 326x206) has the same signature. It may share the cause; #362 owns
it.

### 2.2 The repair chain before this handover could qualify (#587, #588, #590)

This is infrastructure, not Saturn work. Each repair changed
`test/juranometria/tool/SheetCaptureSizingTest.java` only; in every
traced case the production capture was right.

1. **#587's CI stopped** (run 39407972656, this handover's first head
   `2bebef0b`, after all seven local gates passed):
   `aGeometryThePolicyDidNotEstablishIsRefused` - "Expected
   IllegalStateException ... nothing was thrown", 2,288 found, 1
   failed. An expected refusal that did not happen is outside the
   capture-refusal precedent (the owner's #584 ruling). **The run was
   never retried and is kept as evidence**; the handover head was
   abandoned, and #587 stayed stopped and unmerged until the chain
   below landed.
2. **#588 reproduced it on CI's own display.** The failure was on
   ubuntu-latest under bare `xvfb-run` (no window manager), not macOS.
   - **One diagnostic run** (39470749157, a throwaway branch,
     deleted): an instrumented harness of exactly that test, 2 of 500
     runs alone not refused, 0 of 500 after the suite.
   - **The cause:** the setup packed the window narrow, so the
     capture's establishing pack had to ask for 326 → 333x223. The
     native window's late answer to that request landed after the
     test's stale `setSize(326x206)` and put the window back. The
     layout settled at what was established, and the capture correctly
     refused nothing. This was not the proposed hypothesis, a late
     answer to the setup's own request.
   - **The repair:** the window is created at its wide packed frame,
     and the stale size is derived without resizing it. The refusal
     and its 333x223/326x206 assertion are unchanged.
   - **One validation run of the fixed head** (39476504548): all 1,000
     iterations refused. The establishing pack's 326 → 333 request and
     the native revert, present 1,000 and 9 times in the original
     record, appeared 0 times.
   - **Landed as `224058e0`** (PR #592, CI 39515898947: display 2,288
     of 2,288), after #590 as ruled.
3. **#590, four sibling tests**, found while stress-testing #588's fix
   on macOS. Each was traced independently in its failing context
   (instrumented scratch copies, never committed; diagnosis only).
   - `aLaggingContentCatchesUpToTheDeclaration`,
     `contentTheWindowLaysOutAtItsPackedWidthIsLaidOutAtTheDeclaration`
     and `applicationSizedPaintsTheDeclaredGeometry`: the native
     window's late answer to the setup's pack took the window back to
     the packed width inside the restatement, and the capture refused
     an established declaration. Each is now created at its packed
     frame.
   - `aRollbackAfterTheRestatementRefuses` had two causes: the same
     setup answer, and the late answer to the policy's own
     `setSize(420)`, which undid the simulated rollback. It is
     redesigned: created at the floor, its policy applies the floor
     without packing, and its rollback to 326 is the only resize the
     native window is asked for. It still proves that a rollback after
     the restatement is refused, naming 326. The owner accepted the
     window creation's two 420x295 events, both before the rollback.
   - **Qualified uninstrumented:** each test 40 of 40 alone, the whole
     class 0 failing runs in 500 fresh JVMs. **Landed as `a564d720`**
     (PR #591, CI 39498107479: display 2,288 of 2,288).
4. **#589, record only.** A Place and Time field saving to a removed
   preferences node on the event thread, seen in CI display logs
   (including a passing run). Its presence in a passing run does not
   establish that it is harmless; no action was taken, pending its own
   ruling.

## 3. Every stopped, superseded or incomplete run

All records are kept under `~/juranometria-runs/runs/` on the owner's
machine, each under the head it ran on, and quoted on its issue.
Nothing was retried into green: every head that stopped was abandoned
for a new head qualified from gate 1.

| head | issue | what happened | disposition |
|---|---|---|---|
| `c8a0baba` | #574 | all seven gates OK, CI green (run 39238451135) - the proposal head | superseded by the ruled head `80c96ad1` after the cut pack layout and the eight rulings (all seven OK, CI once) |
| `402b0631` | #575 | the measured, not-qualified checkpoint head; no gate run | the owner's ten checkpoint rulings, held on `4b9b32b2` |
| `4b9b32b2` | #575 | gates 1-5 OK; STOPPED at gate 6: the archive lacked `NOTICE-saturn-system.md` - `verify-dist.sh` expected it, the Makefile's `dist` copy had not gained it | added on `e69dcaa0` (all seven OK, CI once) |
| `553e1c0b` | #576 | STOPPED at gate 1: `SheetCaptureSizingTest`, an expected refusal that did not happen (2,288 found, 1 failed, 12 aborted) | not covered by the precedent (owner's ruling); repaired in #584; #576 rebased onto it as `09d4537b` (all seven OK, CI once) |
| `2bebef0b` | #577 | all seven gates OK; **CI STOPPED** (run 39407972656): `aGeometryThePolicyDidNotEstablishIsRefused`, an expected refusal that did not happen | never retried, kept as evidence; repaired in #588 after #590 (section 2.2); this handover rebased onto `224058e0` as a new head, qualified from gate 1 |
| `929dc3cd` | #588 | the diagnostic run on CI's display (39470749157), a throwaway branch: 2 of 500 not refused | the cause traced; the branch deleted after capture |
| `28cd87b2` | #588 | the fixed head's validation run (39476504548): 1,000 of 1,000 refused | validation of a new head, not a retry; the branch deleted after capture |
| `41765d1c` | #584 | gate 1 INCOMPLETE: `ExportSheetDialogSheetMain` refused 333x223 → 326x206, a production capture refusing fail-closed | all seven precedent conditions held and written in `INCOMPLETE.md`; the test recorded unexecuted, never passed; CI's display run 39327105644 executed it (2,275 successful, 0 failed, 0 aborted, 0 skipped); gates 2-7 OK via `gates-from2.sh` |

Development runs before the gates, kept beside them, found what the
qualified heads then carried: three SaturnPack identifiers unreviewed by
the language ledger (`pre-check-575-suite.log`, three rows added); the
companion journey's outward order and the test-evidence count
(`pre-check-576-suite-1.log`). The same development runs also met
capture-family failures in two export generators
(`ExportSheetDialogSheetMain`, `ExportCompanionTest`), which are
development findings, not gate results; the export-dialog refusal
recurred in #584's gate 1 and was handled under the precedent (above).

**A development pre-check refusal, recorded under #362.** The full-suite
pre-check before #588's gates (head `94074711`, a development run, not a
gate) met `PlaceAndTimeSheetMain`'s capture refusing correctly: the
established 420x284 layout was replaced by 326x284 before proof, and the
capture failed closed. Gate 1 on the same head did not meet it. The
evidence is kept in
`~/juranometria-runs/runs/pre-check-588-94074711-refusal-PlaceAndTimeSheetMain/`.

Gate 1's aborted count ranged from 12 to 49 across the qualified heads,
and every gate record's list of unexplained aborts is empty: each abort
is the explained desktop-focus family.

Lessons the next milestone inherits:

- a new notice must join the Makefile's `dist` copy as well as the
  verifier's contract (#575's stop; #473 changed the same line);
- every new identifier on an application surface needs its ledger row
  before the suite;
- a menu or Controller group added in the outward order moves every
  journey that pins that order, and the companion sheets;
- an expected refusal that does not happen is never an incomplete
  capture;
- a test that sizes a real window must not leave an earlier native
  resize in flight when its scenario begins: create the window at the
  frame it needs, and never assume the native answer has arrived
  (#584, #588, #590);
- a failure seen on one platform is diagnosed on that platform, and
  instrumentation changes rates - diagnose with it, qualify without it.

## 4. Routes, evidence movement and performance

### 4.1 The route every PR took

The classifier predicted the **wide** route for #574, #575 and #576, and
each ran it once per head after all seven local gates: `test`,
`display`, `evidence`, `classify`, with `app-image` and `dist`. #584,
one test file, ran the **narrow** route (`evidence` skipped). Each merge
was pinned to its exact head and landed under #494, **structurally
verified** - second parent the accepted head, first parent the
qualified base, tree equal to the qualified trial merge's - so each
landing's `test` run took under a minute. Pages deployed where its paths
moved (`765cc55d`, `2d3186e9`).

| PR | issue | head → merge | PR `test` run | landing `test` run |
|---|---|---|---|---|
| #582 | #574 | `80c96ad1` → `765cc55d` | 39262483454 | 39273199585 |
| #583 | #575 | `e69dcaa0` → `2d3186e9` | 39294880240 | 39303316921 |
| #585 | #584 | `41765d1c` → `56fbb819` | 39327105644 | 39336359829 |
| #586 | #576 | `09d4537b` → `58ffa6d7` | 39342330940 | 39367534970 |
| #591 | #590 | `e46c1347` → `a564d720` | 39498107479 | 39508327402 |
| #592 | #588 | `94074711` → `224058e0` | 39515898947 | 39541741327 |

Every landed PR's display job ran the whole suite with 0 failed, 0
aborted and 0 skipped (2,240, 2,275, 2,275, 2,288, 2,288 and 2,288
tests). #591 and #592, one test file each, ran the narrow route.

### 4.2 What evidence moved

| merge | `docs/studies` | whole change |
|---|---|---|
| `765cc55d` (#574) | 102 files: `docs/studies/saturn-system/` created (101), the test-evidence measurements | 116 files, +143,420 |
| `2d3186e9` (#575) | 10 files: `service-accuracy.md`, `table-measurements.md`, the About sheets and strings (the new notice), the language ledger (three rows), provenance, the test-evidence measurements | 43 files, +4,949 |
| `56fbb819` (#584) | none: one test file | 1 file, +69 |
| `58ffa6d7` (#576) | 7 files: the companion sheets and strings with the Saturn group, provenance, the test-evidence measurements | 24 files, +2,402 |
| `a564d720` (#590) | none: one test file | 1 file, +59 |
| `224058e0` (#588) | none: one test file | 1 file, +17 |

**No released chart output moved.** Since Sprint 51's close
(`cbf9849d`) no chart, sheet, on-the-chart, printable, regional,
overview, globe or closer-field evidence moved: the directories that
moved are `saturn-system` (new), `interface-language` (the About and
companion sheets only), `language-ledger`, provenance and the
test-evidence measurements. The test-evidence hand-off count is requoted
at 1,061 (from 1,060, for `SaturnTableTest`).

### 4.3 Performance, measured

Local gates on this Mac, from the qualified heads' records: gate 1
(2,240-2,288 tests) 11-12 min, gate 2 3-4 min, gate 3 18-19 min, gate 4
19-20 min, gates 5-7 about a minute - 53-54 min a head. CI on a wide
PR: `test` 10-14 min, `display` 14-20 min, `evidence` 31-48 min, the
whole run 32-48 min; `app-image` 7-11 min; `dist` 3-4 min. The narrow
#584 run took 20 min. The Saturn pack adds 79 MB to every image;
neither budget changed order.

## 5. What Part II must decide, and what it inherits

### 5.1 Size on the page (`measurements.md`, *The cartography handoff*)

Over 1900-2100 the A Ring's outer edge spans 34.13″ (farthest,
11.0502 AU) to 46.97″ (nearest, 8.0304 AU), the globe's equator 15.04″
to 20.70″. On the 900 × 700 page:

| field | rings' major axis px | globe px |
|---:|---|---|
| 12° | 0.71-0.97 | 0.31-0.43 |
| 8° | 1.06-1.47 | 0.47-0.65 |
| 4° | 2.13-2.93 | 0.94-1.29 |
| 2° | 4.27-5.87 | 1.88-2.59 |
| 1° | 8.53-11.74 | 3.76-5.17 |
| 0.5° | 17.07-23.48 | 7.52-10.35 |
| 0.25° | 34.13-46.97 | 15.04-20.70 |

(the wider ladder fields are in the report). The rings first reach a
pixel at 8°; the globe stays under rule M's 6 px on every ladder field;
the rings' shape is legible only at the closer fields. Part II measures
every field and paper size (#578).

### 5.2 Questions Part II must answer (#578's eight, with the contract's specifics)

1. **True scale or a minimum mark.** Jupiter's rule - 6 px or the true
   size, whichever is larger, a cartographic symbol while the minimum
   stands in (#482 ruling 2), and Venus's rule M for phase - is the
   precedent. For Saturn the question is the system's: whether the
   minimum carries the rings (a symbol of globe and rings), at which
   field the true rings take over, and how the flattened globe's polar
   diameter at B (1.3) is drawn.
2. **The rings.** Two annuli (1.3), their line weight and fill on white
   paper and black sky; **the near half over the globe and the far half
   behind it**, by the −sin B cos B rule; the near-edge-on rings
   (*edge-on* at 0.00°, the 2025 crossings); whether and how the unlit
   face is shown. The globe's shadow on the rings and the rings' shadow
   on the globe are geometry this contract does not compute; Part II may
   propose them, or leave them out as ruling 4 does for 5.4.0.
3. **The moons.** Seven marks, each decided on its own, as Jupiter's
   ruling 4 and `SatelliteMarks`; but the inner moons live inside the
   ring ellipse - Mimas never farther than 32.2″, Enceladus 40.9″,
   against an A Ring half-span of up to 23.5″ - so separability from
   the rings, not only from the globe, decides them. Precedence among
   seven (Jupiter's: in front, clear, shadowed, then by name), and
   labels through the bodies' adjacent boxes, refused alone, never under
   duress. Every moon is sub-pixel at every field (Titan's 0.8849″ at
   most): its mark is a symbol everywhere.
4. **Vocabulary on the chart.** Jupiter's vocabulary A for the six
   states; **no ring-obscuration state** (ruling 5) - a moon whose sight
   line crosses the rings is drawn by the painting order only, never
   named as hidden by them; no satellite shadow spots.
5. **Selection and Centre on chart.** Saturn's identity on the page, the
   moons' (Jupiter's are `jovian.io` and so on), whether any moon is
   selectable, and the occurrence model (#513). The table refuses Centre
   on chart until the layer exists.
6. **The closer fields.** Saturn is not yet eligible for 0.5° and 0.25°:
   the #481 contract named Mercury, Venus and Jupiter, and "Saturn waits
   for its own cartography study" (`mercury-cartography.md`). At 0.25°
   the rings are 34.13-46.97 px and the globe 15.04-20.70 px.
7. **Screen, paper, accessibility and export.** The spoken description
   (a symbol named as one, the states in words, the rings' opening and
   face), export through the same module, Solar System label emphasis
   (#526) over Saturn's and the moons' names, the horizon, the page edge,
   and "a symbol never erases a star" (#482 ruling 7, and the symbol
   layering rule of #514: a symbol in front of an opaque disc is painted
   over it in the bodies layer).
8. **The years.** 1900-1999: Saturn without moons, as Jupiter's.
   Outside 1900-2100: nothing, with the table's refusal.
9. **The production split** (#578 ruling 8), then #579's packaged
   checkpoint.

### 5.3 The Mercury article and the 5.4.0 boundary (#573, Part II)

Part II publishes the bilingual Mercury reader article (#580),
following the one-planet-later cadence, then hands Saturn to 5.4.0
release preparation (#581). 5.4.0 requires accepted Saturn numbers
(done), rings and ruled moons on the chart, the Mercury article, one
integrated evidence flush and a packaged release journey. Completing
Part I changes no `VERSION`; tag and publication remain separate owner
decisions.

### 5.4 Explicit exclusions (#573, unchanged)

No invented surface detail, albedo map, false photographic texture or
unsupported colour; no general event search, animation, live tracking,
rise/set service or future Jupiter-shadow work; no Mars, Uranus,
Neptune or Pluto; no bookmarks, local server, AI integration or local
civil-time work. Hyperion stays out of 5.4.0. IMCCE material is never
bundled.

### 5.5 Deferred by name

- The export-dialog refusal's possible late-resize cause (section 2.1),
  under #362, and the Place and Time pre-check refusal recorded there.
- The class's remaining `packedAround` users, which have not failed in
  any stress run, are reported candidates only:
  `packingAnApplicationSizedWindowProducesTheWidthNobodySees`,
  `theDeclarationIsFrozenAndRestatedExactlyOnce`,
  `invalidOrUnrealizableDeclarationsAreRefused`.
- #589, record only, pending its own ruling.
- The cross-planet article on ephemerides and provenance waits for the
  6.0 wrap-up.
- A Saturn magnitude needs its own model and authority.
- Open and untouched by Sprint 52: #571, #553, #554, #497, #282, #417.
