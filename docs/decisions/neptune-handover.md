# Neptune, numerically: the handover to cartography

**Issue #657, closing Part I of the Neptune road (#653).** This record hands the settled numerical Neptune contract to Part II:
- #658, the cartography study;
- #659, Neptune and its ruled moons on the chart;
- #660, the Uranus reader article;
- #661, the integrated 6.0.0 handover and release boundary.

It records; it builds nothing.

Every number it quotes comes from a report, a fixture or a run record named beside it. Every ruling it quotes is the owner's, dated. The contract itself is `neptune-system.md`: the study, the #654 rulings, and #655 and #656 as built. This document says what stands, what was measured, what stopped, what moved, and what Part II must decide.

**Baseline.** The Neptune feature work (#654–#656) was complete at main `9013663e` (2026-10-30). It began from the Uranus road's end, main `ed01b714`, under the published 5.5.0 (`v5.5.0` → `6958ce75`). There was no infrastructure repair chain. Three gate stops were the head's or the worktree's own (§2). Neptune Part I made no VERSION change, tag or release.

## 1. What is settled

### 1.1 Authority, pack and licensing (#654: N-1a, N-2a, N-6a, N-11; #655)

**The planet: DE442's Neptune barycentre (0 → 8), from `nep098_part-1.bsp`.**
- **Why DE442.** Horizons answers Neptune from `nep098_merged`, on DE442. Against Horizons over 1900–2100 (`authority-comparison.md`):
  - DE442's barycentre alone agrees to **0.0040″** astrometric;
  - DE442's barycentre with the centre added agrees to 0.0010″;
  - the released DE440's barycentre misses by **0.1072″**.

  DE440 and DE442 place the barycentre up to 2,222.155 km apart (`contract/pack-measurements.md`).
- **The Sun and the Earth stay DE440's.** Their DE440 and DE442 positions differ by at most 0.49 km. Neptune is the second planet, after Uranus, whose place does not come from DE440 (N-1a, ruled 2026-10-29).
- **The barycentre stands for the centre (N-2a), measured in both units this time.**
  - **The offset.** Neptune's centre never lies more than **74.190 km** from the barycentre over 1900–2100. That is 0.0036″ seen from the Earth, but it is most of the barycentre's distance error against Horizons: up to 89.48 km in the study.
  - **The ruling.** The owner ruled the exact-era distance target at **100 km**, with the builder refusing an offset above **80 km**. The 10,291,200-byte centre excerpt is not shipped.
  - **The display.** The table shows distance to 1e-6 AU (149.6 km), so the offset is under half its last digit.
  - **Why this matters.** This is the Uranus road's lesson applied in the study: there, the distance consequence surfaced only in #634.

**The moons: NEP098, from the same kernel (N-6a).**
- **Bodies and years.** Triton (8 → 801) and Nereid (8 → 802), the civil days **2000-01-01 to 2100-12-31 inclusive**. The kernel runs to 2199, so the boundary is the civil day, and the excerpt keeps the released packs' 31-day margins.
- **Before 2000** Neptune answers and the moons refuse, naming their years.

**Constants: IAU 2015, from `pck00011.tpc`.**
- **Neptune.** Radii 24764, 24764 and 24341 km.
- **The pole carries a periodic term, unlike Uranus's (N-4).** It is RA 299.36° + 0.70 sin N and Dec 43.46° − 0.51 cos N, with N = 357.85° + 52.316° T.
  - Without the term the pole is off from Horizons' by up to 2,050.7″; with it, 0.018″.
  - The builder pins the coefficients and the angle and refuses a PCK that differs.
  - The mutation rival without the term fails the pole's position angle (`NeptuneMutationTest`).
- **The moons.** Triton 1352.6 km, Nereid 170 km. The first PCK radius is each moon's radius in the states.

**The pack, built by `make import-neptune-system`.** One `neptune-system` pack holds both kernels, as Uranus's does. Two builds are byte-identical (`neptune-system.md`, *#655, measured and as built*):

| file | bytes | SHA-256 |
|---|---:|---|
| `neptune-system/juranometria-de442-neptune-1900-2100.bsp` (0 → 8) | 416,768 | `12a8da7d…` |
| `neptune-system/juranometria-nep098-moons-2000-2100.bsp` (8 → 801, 802) | 50,302,976 | `e78119d9…` |

- **Larger than the cut, by attribution only.** Each kernel is 44,032 bytes over the #654 cut (372,736 and 50,258,944 bytes). That is the comment area NAIF's rules for a modified kernel require, larger than Uranus's 14,336 because nep098's own comment area, kept as attribution, is longer. No ephemeris byte differs from the source.
- **The builder's refusals.** It writes nothing unless all of these hold:
  - `nep098_part-1.bsp`'s SHA-256 (`5327f47a…`), the PCK pin and the `DE-0442` segment name match;
  - every released kernel carries its released digest, Uranus's included (§1.7);
  - the centre offset is under 80 km;
  - the periodic pole term is the ruled one.

**Licensing.**
- **The rules.** NAIF's rules for modified kernels, stated in `NOTICE-neptune-system.md`.
- **Where it is carried:**
  - About, in both languages (*Neptune System ephemeris*, *Neptunsystemefemeride*);
  - `LICENSING.md` and the licensing summary;
  - the app image's list, whose computed count now reads **16**;
  - the Makefile's `dist` list and `verify-dist.sh`.
- **Horizons and Espenak.** Horizons responses are committed under its public terms. Espenak's pages stay outside Git; only the quoted rows are committed (`neptune-events.txt`).

**Fixtures and raw sources** (`SOURCES.md`).
- **Horizons.** 39 requests: 36 responses committed under `horizons/` (10,360,375 bytes), and the three geocentric matrices retained outside Git under `~/juranometria-runs/neptune-horizons/` (8,196,593 bytes).
- **Espenak.** Seven almanacs' Neptune rows, quoted with their pages' digests.
- **The raw files** live in the main checkout's ignored `imports/raw/neptune-system/`, never in a worktree's (the #636 lesson). Removing the #654, #655 and #656 worktrees deleted no raw file.

### 1.2 Interval, time and frames (inherited)

**Intervals.** Neptune answers 1900–2100. Triton and Nereid answer 2000–2100, as above.

**The time model** is inherited unchanged from Saturn, Mars and Uranus:
- UT1 before 1972;
- UTC to the leap-second record's expiry on 2027-06-28;
- the ΔT model before and after;
- observers at sea level on WGS84.

**Frames.**
- **The chart position** is topocentric astrometric ICRS/J2000, light-time corrected.
- **Apparent places of date** are aberrated and deflected by the Sun, with Jupiter's formula. The deflection and aberration rivals fail (`NeptuneMutationTest`).

### 1.3 Every observable and its meaning (N-3 to N-5, N-7, N-9, N-10)

**Neptune:**
- **distance** to the light-time-corrected barycentre;
- **equatorial diameter**;
- **polar diameter at the planetocentric tilt**;
- **phase angle**, held to Horizons' `phi`;
- **illuminated fraction.** Neptune is never less than **99.971 %** lit (`measurements.md`: phase angle at most 1.9529°), so there are **no phase words**;
- **the north pole's position angle**, of date, from the pole with its periodic term.

**The tilt is planetocentric (N-4).** Horizons' `ObsSub-LAT` is planetodetic. It differs from our planetocentric tilt by up to 3,000.4″, and from ours converted to planetodetic by at most 0.0520″ (`service-accuracy.md`). Over 1900–2100 the sub-observer latitude runs from −29.2178° to +29.2066° (`measurements.md`).

**The moons.**
- **Offsets.** Horizons' apparent-of-date X/Y, separation, separation in Neptune radii, and **the position angle, derived from the offsets (N-10)**. Horizons' own `SatPANG` disagrees with its own X/Y by up to 0.0191° for Nereid beyond 20″ (`neptune-system.md`). The service is held to the X/Y-derived angle, and `SatPANG` is printed, never asserted.
- **States (N-7).** Jupiter's six, with Neptune's name, decided on Neptune's flattened figure and its umbra. Horizons' equatorial sphere is the comparison only.
- **Where figure and sphere differ: they never do.** On no committed row does the flattened figure decide differently from the sphere ("figure differs" is 0 for both moons, `service-accuracy.md`).
- **No shadow transits.**

**The rings (N-8).** No ring rows, no ring ink, no ring authority requested.

**The words.** English and Norwegian, as built in #656:
- *Avstand fra Neptun*, *Neptunradier*, *klar av Neptun*, *bak Neptun, i skyggen*;
- *(nordpolen mot deg)* and *(sørpolen mot deg)*;
- the moon names are the same in both languages (N-9).

### 1.4 Moon inclusions and exclusions (N-6a, 2026-10-29)

**In:** Triton and Nereid, 2000–2100, decided on measured cost and value. The road (#653) forbids a smaller set merely to save bytes.

**Not ruled in:**
- **Triton and Nereid, 1900–2100** (N-6c): 99,932,160 bytes.
- **Triton alone** (N-6b): 29,565,952 bytes for 2000–2100.
- **The six inner moons**, excluded on measurement, not bytes alone (`measurements.md`):
  - Proteus, the brightest, is magnitude 20.27 and never more than 5.6241″ from Neptune's centre;
  - Larissa, Galatea and Despina are 21.97 to 22.789;
  - Horizons gives no magnitude for Naiad or Thalassa;
  - Naiad alone would cost 147,812,352 bytes for 2000–2100. The others were not measured; their kernels total 3.58 GB.
- **The irregular moons** (`nep104`).

From the committed Horizons responses (`horizons/matrix-7d-*-oslo.txt` and `magnitudes-30d-*-geocentric.txt`, over the moons' years 2000–2100), the moons Part II draws:

| moon | separation from Neptune's centre | angular diameter | magnitude |
|---|---|---|---|
| Triton | 0.5348″–16.9617″ | 0.119475″–0.129437″ | 13.43–13.632 |
| Nereid | 2.1413″–457.7006″ | 0.015007″–0.016273″ | 18.669–18.875 |

Both moons are points at every field. **Nereid is the faintest body the atlas has ever ruled in**, 2.6 magnitudes fainter than Miranda. Its separation reaches 7.6′ on a strongly eccentric orbit (1,343,903 to 9,684,532 km from Neptune, `contract/pack-measurements.md`). No magnitude is shown for any body.

### 1.5 Measured errors, by era (`service-accuracy.md`, held by `NeptuneReferenceVectorTest`)

Every value is inside its target in force: N-10, which is Uranus's targets with N-2a's distance.

| quantity | target in force | 1900–1961 | 1962–1971 | 1972 → 2027-06-28 | after |
|---|---|---:|---:|---:|---:|
| Neptune astrometric | 0.05 / 0.05 / 0.01 / 3″ | 0.0050″ | 0.0035″ | 0.0035″ | 0.2078″ |
| Neptune apparent, > 1° from the Sun | 0.3 / 0.1 / 0.2 / 3″ | 0.0892″ | 0.0861″ | 0.1307″ | 0.3324″ |
| Neptune distance | 100 / 100 / 100 / 7,000 km | 93.426 km | 79.211 km | 66.760 km | 4,071.648 km |
| equatorial diameter | 0.001 / 0.01″ | 0.00000″ | 0.00000″ | 0.00000″ | 0.00000″ |
| illuminated | 0.001 / 0.01 points | 0.00001 | 0.00001 | 0.00001 | 0.00001 |
| phase angle, against `phi` | 0.5 / 10″ | 0.1803″ | 0.1794″ | 0.1805″ | 0.3563″ |
| pole position angle, > 1° from the Sun | 0.5 / 3″ | 0.210″ | 0.199″ | 0.208″ | 0.338″ |
| moons X, Y, separation, > 1° from the Sun | 0.05 / 0.5″ | – | – | 0.0038″ | 0.0259″ |
| moons position angle, > 20″, against Horizons' X/Y | 0.01°; printed after | – | – | 0.0070° | 0.2044° (printed) |
| horizontal, Neptune | 20″; printed after | 0.9396″ | 2.0017″ | 11.6957″ | 2.4843″ (printed) |
| horizontal, moons | 20″; printed after | – | – | 9.2576″ | 2.5127″ (printed) |

**Measured, printed, never asserted:**
- the pole position angle **within 1° of the Sun**: 0.159″ exact, 1.232″ after;
- the moons' position angle **within 20″ of Neptune**: 0.0421° exact, 0.2826° after;
- **against `SatPANG`** beyond 20″: 0.0190° exact, which is Horizons' own disagreement with its X/Y (§1.3);
- the moons' apparent place **within 1° of the Sun**: 0.1011″ exact, 1.8281″ after;
- the planetodetic tilt against `ObsSub-LAT`.

**Moon states on Horizons' sphere.** Triton agrees on 5,578 of 5,579 rows in the moons' years, Nereid on 5,579 of 5,579. The one disagreement, Triton on 2049-04-19 (ours clear, Horizons in transit), falls after the record's expiry, where the instant itself is estimated: printed, not asserted (Saturn's ruling, inherited).

### 1.6 Named cases, rivals and honest limits

**The study's "today", 2026-10-29 12:00 UTC at Oslo** (Horizons' row in `named-neptune-oslo.txt`).
- **Neptune** is 29.0485 AU away, 2.351″ across, 99.991 % lit at a phase angle (`phi`) of 1.0636°. Its north pole is at position angle 316.498°, and Horizons' sub-observer latitude is −19.82°: **the south pole is towards the observer**. Neptune is 20.77° below the horizon.
- **Triton and Nereid** are both clear of Neptune (Horizons codes `*`).
- **The match.** The packaged acceptance holds Neptune to Horizons' astrometric row within 0.01″; the image measured **0.0032″** (gate 5 at `0321568f` and `6a5f3037`).

**The states, named from Horizons' Oslo rows** (`NeptuneTableTest`): Triton in front of Neptune on 2043-09-07 and 2045-05-22 (`t`), and behind it in its shadow on 2044-09-26 and 2045-08-21 (`U`). The packaged acceptance checks 2044-09-26.
- **Seasonal.** Triton transits and is eclipsed only in seasons around Neptune's equinoxes. Inside the moons' years the first fall in 2043–2049, **after the exact interval**, so no named state lies in the exact era.
- **Nereid never transits, is never occulted and is never eclipsed** in any committed row.
- **Codes the moons' years never offer.** Horizons' codes `O` (occulted) and `u` (in shadow, clear of the disc) appear for no ruled moon in those years.

**Espenak's oppositions and conjunctions** (N-12). Each is found from −17 to +114 minutes of the listed hour (`service-accuracy.md`). That is **the almanac's own precision for Neptune**: printed, never asserted. Oppositions and conjunctions are validated against Horizons; moon states are checked against Horizons only.

**Rivals** (`NeptuneMutationTest`). Each of these misses its target:
- no aberration;
- no deflection;
- the aberrated down-leg for the phase;
- an unaberrated Sun;
- the planetodetic tilt;
- the moons' offsets from J2000 places;
- **the pole without its periodic term**, new for Neptune.

**The boundary** (`NeptuneBoundaryTest`). The moons answer 2000-01-01 to 2100-12-31 inclusive, an epoch outside their kernel is refused before it is read, and Neptune answers on both sides.

### 1.7 Existing-body invariance

- **The builder proves the released kernels.** It refuses to write unless every released kernel carries its released digest: the Solar System, Jovian, planets (Venus, Mercury, Mars), Saturn System, Mars System and **Uranus System** kernels. `NeptuneSystemPackTest` holds them.
- **No released service changed.** Between `ed01b714` and `9013663e` the source files that changed outside the new Neptune classes are distribution and hosting only:
  - `AboutDialog`, `AppMenuBar`, `JUranometriaMain` and `PackagedAcceptanceMain`;
  - `SolarSystemSection`;
  - `CompanionSheetMain` and `MenuSheetMain`;
  - `EvidenceContractMain`.

  Nothing under `render`, `solarchart`, `chart` or `sky` changed, and no released service class.
- **The released tests ran unchanged.** The Sun, Moon, Mercury, Venus, Mars, Jupiter, Saturn and Uranus reference tests ran unchanged on every head. The tests that changed are distribution and host journeys: the bundled notices, the About text, the Place and Time gate, the licensing count, and the Solar System companion's order.
- **Uranus's accuracy report** is now reproduced by the evidence contract, beside Neptune's (#655, deferred from #640). It reproduced byte for byte.

### 1.8 The Neptune table and its verdict (#656)

**Placement and behaviour.**
- **Where.** View ▸ Solar System ▸ *Neptune...* directly after *Uranus...*. The Controller's Neptune group follows Uranus's, introduced collapsed.
- **No access letter.** Every letter of "Neptune" and "Neptun" is already claimed on the menu, so Mars's ruling on #603 applies.
- **One shared session,** with host-local drafts.
- **Nothing computes uninvited.** Building, opening, expanding or restoring computes nothing and reads no pack. The 50 MB pack is read on the first computation.

**What it shows.**
- **The card,** Uranus's ten lines.
- **The moons:** Triton and Nereid in Jupiter's columns.
- **Ranges** are grouped by instant, at most **500 instants**: two moon rows each, the row cap divided by the moon count, as every table's is.
- **Outside the moons' years** Neptune is always shown, with the moons' years named.
- **No chart surface:** no Show on chart and no Centre on chart.

**Evidence.** The Controller photographs and, from the start this time, the **menu photographs** show the new group and item. Uranus's had missed the menu until #638.

**Verdict (2026-10-29).** The owner tried the packaged candidate: "got no reason to question Neptune tables, looks good". The owner noted a GUI idea for the 7.0.0 road: a planet dialog opens empty until Compute, which is the "opening computes nothing" rule showing through.

### 1.9 Where the contract lives

- **`neptune-system.md`:** the study, the rulings N-1 to N-14, and #655 and #656 as built.
- **`docs/studies/neptune-system/`:**
  - the sources: `inventory.md`, `SOURCES.md` and `neptune-events.txt`;
  - the fixtures: `horizons/`;
  - the measurements: `authority-comparison.md`, `measurements.md` and `contract/pack-measurements.md`;
  - the service: `service-accuracy.md`, now under the evidence contract.

## 2. Every stopped, superseded or incomplete run

All records are kept under `~/juranometria-runs/runs/` on the owner's machine, each under the head it ran on. Nothing was retried into green. Two heads stopped on defects of their own (`b9530be6`, and `092dff97` on its second run) and were superseded by a fixed head qualified from gate 1. `092dff97`'s first run stopped on its worktree's missing inputs; that head was rerun, unchanged, from gate 1 on the owner's word.

| head | issue | what happened | disposition |
|---|---|---|---|
| `b7a01105` | #654 | the proposal head, not gated (gate economy) | ruled; the ruling recorded at `b9530be6` |
| `b9530be6` | #654 | **STOPPED at gate 1**: `theFastReportsReproduceByteForByteFromTheirMains`. The committed test-evidence inventory lacked the study's 37 fixtures and 5 reports (`GATE1-STOPPED.md`) | regenerated; `30a2d120`: all seven gates OK, CI once |
| `092dff97` | #655 | run 1: gates 1–2 OK, **STOPPED at gate 3**, "verification incomplete". j655 held only four of the main checkout's raw source directories (`GATE3-STOPPED.md`) | an environment omission; the rest cloned in; rerun on the owner's word |
| `092dff97` | #655 | run 2: gates 1–5 OK, **STOPPED at gate 6**. The Makefile's `dist` list lacked `NOTICE-neptune-system.md`, which `verify-dist.sh` expects (`GATE6-STOPPED.md`) | one Makefile line; `0321568f`: all seven gates OK, CI once |
| `6a5f3037` | #656 | the packaged candidate, accepted; **all seven gates OK on the first run** | CI once |

**Lessons the next milestone inherits:**
- **A new committed study file moves the test-evidence inventory.** Regenerate `make test-evidence-study` before committing a study.
- **A worktree needs the whole of `imports/raw`, copied** (`cp -cR`), not the planet's directories alone. Gate 3 verifies the constellation and star-identity families from their raw sources.
- **The pre-flight (owner's concern, 2026-10-29: "Gates expensive (but critical)").** Before every gate request:
  - raw-source parity with main;
  - exact replays of gates 6 and 7;
  - for a road that copies an earlier landing, **a file and line parity audit** against it.

  Run against `092dff97`, the audit flags exactly the missing Makefile line. With it, #656 passed all seven on its first run.
- **Measure every observable a ruling touches**, applied: the barycentre's distance was measured in #654, ruled in N-2a, and met in #655.
- **The ledger** stands at 159 of fewer than 160, unchanged across the road.

**Gate 1's aborted count** was 12 on every qualified head. Every gate record's list of unexplained aborts is empty.

## 3. Routes, evidence movement and performance

### 3.1 The route every PR took

Each PR ran the wide route once per head, after all seven local gates: `test` (with `display`, `evidence` and `classify`), `app-image` and `dist`. Each merge was pinned to its exact head and landed under #494, structurally verified 4/4.

| PR | issue | head → merge | PR `test` run | display (found / failed / aborted) | landing `test` run |
|---|---|---|---|---|---|
| #662 | #654 | `30a2d120` → `69a50e13` | 40907075085 | 2,496 / 0 / 0 | 40918106186 |
| #663 | #655 | `0321568f` → `3f2b81c1` | 40959104500 | 2,528 / 0 / 0 | 40965989432 |
| #664 | #656 | `6a5f3037` → `9013663e` | 40975186803 | 2,542 / 0 / 0 | 40979360056 |

### 3.2 What evidence moved

| merge | `docs/studies` | whole change |
|---|---|---|
| `69a50e13` (#654) | 43 files: `docs/studies/neptune-system/` created (42), the test-evidence measurements | 51 files, +65,272 |
| `3f2b81c1` (#655) | 6 files: `service-accuracy.md`; the About sheets and strings (the new notice: the scrollbar thumb and the notice count); provenance; the test-evidence measurements | 38 files, +3,340 |
| `9013663e` (#656) | 16 files: the companion sheets and strings with the Neptune group; **the eight menu photographs and `menu-strings.md`** with *Neptune...*; provenance; the test-evidence measurements | 34 files, +2,492 |

**No released chart output moved.** Only these directories moved:
- `neptune-system`, which is new;
- `interface-language`: the About, companion and menu sheets only;
- provenance and the test-evidence measurements.

No chart, sheet, on-the-chart, printable, regional, overview, globe or closer-field evidence moved. The test-evidence hand-off count stands at 1,107.

### 3.3 Performance, measured

**Local gates on this Mac,** from the gate records' own file times:

| gate | `30a2d120` | `0321568f` | `6a5f3037` |
|---|---:|---:|---:|
| 1 (2,496–2,542 tests) | 11.7 min | 11.7 min | 11.8 min |
| 2 | 3.6 min | 3.7 min | 3.6 min |
| 3 | 18.4 min | 18.6 min | 18.4 min |
| 4 (2 GiB) | 20.1 min | 20.2 min | 20.5 min |
| 5–7 | 1.2 min | 1.2 min | 1.3 min |
| **a head, in all** | **55.1 min** | **55.5 min** | **55.7 min** |

Uranus's #635 head took 55.1 minutes at the same 2 GiB. A head's cost held steady across Neptune Part I, though the two stopped runs on `092dff97` and the stopped run on `b9530be6` each cost part of it again. What the evidence-contract process retains at 2 GiB is still not measured; #661 measures it.

**Install size.** The Neptune System pack adds 50.7 MB to every image (416,768 + 50,302,976 bytes). With Uranus's 38.4 MB, the outer planets add 89.2 MB since 5.5.0.

## 4. What Part II must decide, and what it inherits

### 4.1 Size on the page

Computed from `measurements.md` and §1.4:
- Neptune's equatorial diameter over 1900–2100 is 2.18006″ to 2.36997″ (weekly, from Oslo);
- the moons' greatest separations over 2000–2100 are Triton 16.9617″ and Nereid 457.7006″.

On the 900 × 700 page, a field of F degrees across its width gives 900 / (F · 3600) px per arcsecond:

| field | Neptune's diameter, px | Triton, greatest | Nereid, greatest |
|---:|---|---:|---:|
| 12° | 0.05 | 0.35 | 9.5 |
| 8° | 0.07 | 0.53 | 14.3 |
| 4° | 0.14–0.15 | 1.06 | 28.6 |
| 2° | 0.27–0.30 | 2.12 | 57.2 |
| 1° | 0.55–0.59 | 4.24 | 114.4 |
| 0.5° | 1.09–1.18 | 8.48 | 228.9 |
| 0.25° | 2.18–2.37 | 16.96 | 457.7 |

**What the table shows.** A 6 px mark reaches 3 px from Neptune's centre.
- **Neptune is a symbol at every field.** Its true disc never exceeds 2.37 px, even at 0.25°. Under rule M the 6 px mark always stands in, as for Uranus.
- **Triton** never leaves 3 px of Neptune's centre at 2° and wider. At 1° it can, near its greatest separation (4.24 px). At 0.5° and 0.25° it often does. **The closer fields decide Triton**, as they decided Miranda.
- **Nereid** is the opposite case. At its greatest separation it stands 9.5 px out even at 12°, and at 0.25° 457.7 px out, half the page's width. At its least separation (2.1413″) it is inside Neptune's mark at every field, 2.14 px from the centre even at 0.25°. Whether a magnitude-18.7 moon is drawn on a star chart at all, and at which fields, is a question the atlas has not met before.
- **No rings** to draw (N-8).

Part II measures every field and paper size on real production pages (#658).

### 4.2 Questions Part II must answer (#658)

1. **The mark.** Rule M stands in at every field. Neptune is always at least 99.971 % lit: is its mark ever phased, or always full? Its flattening (1.7 % between the radii) is never visible at 2.37 px.
2. **The pole.** The pole carries a periodic term, and the sub-observer latitude reaches ±29.2°. Uranus's ruling drew no pole. Does Neptune's mark show anything of it?
3. **Colour and surface.** The road forbids invented markings, false textures and unsupported colour. Uranus's ruling was "unphased, round, no colour". Does Neptune follow it?
4. **The moons.**
   - Two marks, each decided on its own, with **separability from the planet's mark** deciding drawability (§4.1).
   - Does Nereid, at magnitude 18.7 and up to 7.6′ out, get a mark like Triton's, or is it table-only on the chart? The road forbids silently dropping a ruled moon, so any omission is a ruling.
   - Labels through the bodies' adjacent boxes: refused alone, never under duress.
5. **Vocabulary on the chart.** Jupiter's vocabulary A for the states. There are no satellite shadow spots.
6. **The years.** Neptune is drawn over 1900–2100, the moons only over 2000–2100, refused by name elsewhere. Neptune is never hidden because the moons are not computed.
7. **Selection and Centre on chart.** Neptune's identity on the page, and the moons' if any is selectable, through the occurrence model (#513). The table's Show on chart and Centre on chart wait for the layer.
8. **The closer fields.** Neptune is not yet eligible for 0.5° and 0.25°. For Triton, eligibility decides whether it is ever drawn apart from Neptune's mark below 1°.
9. **Screen, paper, accessibility and export:**
   - the spoken description: a symbol named as one, the tilt in words, the states;
   - export through the same module;
   - Solar System label emphasis (#526);
   - the horizon and the page edge;
   - "a symbol never erases a star";
   - the symbol-layering rule of #514.
10. **The Solar System menu's spoken description.** It still reads "The Sun, Mercury, Venus, the Moon, Jupiter and Saturn…" and has missed Mars and Uranus since their roads. Raised at the #656 checkpoint; #659 is where it can be made whole.
11. **The production split.** Planet and moons in separately reviewable PRs if the study supports it, then #659's packaged checkpoints.

### 4.3 The Uranus article and the 6.0.0 boundary (#653, Part II)

Part II publishes the bilingual **Uranus** reader article (#660), keeping the one-planet-later cadence. **Neptune's article opens the post-6.0 road toward 7.0.0.** Part II then prepares the integrated 6.0.0 handover and release boundary (#661), covering everything since 5.5.0: the Mars article, Uranus and Neptune.

6.0.0 requires all of these:
- accepted Neptune numbers, which are done;
- the ruled moons and Neptune on the chart;
- the Uranus article;
- one integrated evidence flush across the whole stretch;
- a packaged release journey on every platform image.

Completing Part I changes no `VERSION`. Tag and publication remain separate owner decisions.

### 4.4 Explicit exclusions (#653, unchanged)

- No invented cloud bands, albedo map, false photographic texture, unsupported colour or ring geometry.
- No general event search, animation, live tracking or rise/set service, and no shadow transits.
- **No Pluto** and no wider outer Solar System: these belong to a later second Solar System sprint.
- No magnitude for any body without its own authority.
- Every external download requires prior approval.

### 4.5 Deferred by name

- **The moons before 2000** need the 1900–2100 excerpt (N-6c, 99,932,160 bytes), which was not ruled in.
- **The inner moons**, excluded on measurement; their kernels (3.58 GB) were never fetched.
- **A Neptune magnitude** needs its own model and authority.
- **Measuring what the evidence contract's process retains** at 2 GiB: #661.
- **The Solar System menu's spoken description** (§4.2, 10): #659.
- **The owner's GUI note** on empty planet dialogs: the 7.0.0 improvement road.
