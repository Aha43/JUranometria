# Uranus, numerically: the handover to cartography

**Issue #636, closing Part I of the Uranus road (#632).** This record hands the settled numerical Uranus contract to Part II: #637, the cartography study; #638, Uranus and its ruled moons on the chart; #639, the Mars reader article; and #640, the road's handover and release boundary. It records; it builds nothing.

Every number it quotes comes from a report, a fixture or a run record named beside it. Every ruling it quotes is the owner's, dated. The contract itself is `uranus-system.md`: the study, the #633 rulings, the #634 distance amendment, and #634 and #635 as built. This document says what stands, what was measured, what stopped, what moved, and what Part II must decide.

**Baseline.** The Uranus feature work (#633–#635) was complete at main `6e45f155` (2026-10-28). There was no infrastructure repair chain; one gate stop on #635 changed the evidence contracts' heap (§2). No `VERSION` change (5.5.0), no tag, no release.

## 1. What is settled

### 1.1 Authority, pack and licensing (#633: U-1a, U-2, U-6a, U-11; #634)

**The planet: DE442's Uranus barycentre (0 → 7), from `ura184_part-3.bsp`.**
- **Why DE442.** Horizons answers Uranus as DE442 does. Against 21,881 Horizons rows, 1900–2100, DE442's barycentre with the centre offset agrees to **0.0018″** astrometric; the released DE440's misses by **0.3779″** (`authority-comparison.md`). DE440 and DE442 place the barycentre up to 4,864.7 km apart (`contract/pack-measurements.md`).
- **The Sun and the Earth stay DE440's.** Their DE440 and DE442 positions differ by at most 0.49 km. This is the first planet whose place does not come from DE440 (U-1a, ruled 2026-10-27).
- **The barycentre stands for the centre (U-2).** Uranus's centre never lies more than **45.136 km** from it over 1900–2100, at most 0.0035″ seen from the Earth. The builder re-measures the offset hourly over 1900–2100 from the pinned kernel and refuses above 50 km. The 7,352,320-byte centre excerpt is not shipped.

**The moons: URA182.21, from the same kernel (U-6a).**
- **Bodies and years.** Miranda (7 → 705), Ariel (701), Umbriel (702), Titania (703) and Oberon (704), the civil days **2000-01-01 to 2100-12-31 inclusive**. The kernel runs to 2399, so unlike Mars there is no exclusive TDB edge: the boundary is the civil day, and the excerpt keeps the released packs' 31-day margins.
- **Before 2000** Uranus answers and the moons refuse, naming their years.

**Constants: IAU 2015, from `pck00011.tpc`.**
- **Uranus.** Radii 25559, 25559 and 24973 km.
- **The pole.** RA 257.311°, Dec −15.175°, a quadratic in T **with no periodic terms**. The builder refuses if the PCK ever carries Uranus's periodic pole terms. Horizons' pole agrees to 0.0000″.
- **The moons.** Each moon's PCK radii; the first is its radius in the states.

**The pack, built by `make import-uranus-system`.** One `uranus-system` pack holds both kernels, as Saturn's does; the planets pack stays at version 3. It rebuilds byte-identically (`uranus-system.md`, *#634, measured and as built*):

| file | bytes | SHA-256 |
|---|---:|---|
| `uranus-system/juranometria-de442-uranus-1900-2100.bsp` (0 → 7) | 387,072 | `21964f56…` |
| `uranus-system/juranometria-ura182-moons-2000-2100.bsp` (7 → 705, 701–704) | 38,055,936 | `6f8ff878…` |

- **Larger than the cut, by attribution only.** Each is 14,336 bytes over the #633 cut (372,736 and 38,041,600 bytes): the comment area NAIF's rules for a modified kernel require. No ephemeris byte differs from the source.
- **The builder's refusals.** It writes nothing unless `ura184_part-3.bsp`'s SHA-256 (`273cd4cc…`), the PCK pin and the `DE-0442` segment name match, every released kernel carries its released digest (§1.7), the centre offset is under 50 km and the PCK has no periodic Uranus pole terms.

**Licensing.**
- **The rules.** NAIF's rules for modified kernels, stated in `NOTICE-uranus-system.md`: the excerpts are called modified kernels, renamed, attributed to JUranometria, and never presented as JPL's own.
- **Where it is carried.** The About dialog (both languages: *Uranus System ephemeris*, *Uranussystemefemeride*), `LICENSING.md`, the licensing summary, the app image's list and the portable archive.
- **The inventory count.** `build-app-image.sh` printed 13 licensing resources from #602 to #634 while its list held 14. It now counts its own list and prints 15; `LicensingInventoryCountTest` refuses a typed count and a list that leaves out a notice About shows (the owner's ruling 6 on the #634 checkpoint).
- **Horizons and Espenak.** Horizons responses are committed under its public terms; Espenak's almanac pages are kept outside Git, and only the quoted rows are committed (`uranus-events.txt`).

**Fixtures** (`SOURCES.md`, request 4).
- 59 Horizons requests: 53 responses committed under `horizons/` (11,349,249 bytes), and the six geocentric matrices retained outside Git under `~/juranometria-runs/uranus-horizons/` (15,528,721 bytes).
- Seven almanacs' Uranus rows, quoted with their pages' digests.

**The raw kernel, corrected here.** `SOURCES.md` records request 1's descriptive files and request 3's kernel under `imports/raw/uranus-system/`. That was the #633 worktree's copy, and removing the worktree deleted it. On 2026-10-27, approved with conditions, `scripts/download-uranus-sources.sh` re-fetched **`ura184_part-3.bsp`, `.cmt` and `.mrg`** into the main checkout's ignored `imports/raw/uranus-system/` (`RETRIEVED.txt`: 22:25:35Z to 22:26:07Z). The kernel's MD5 matched NAIF's and its SHA-256 the pin. Request 1's other descriptive files (the summaries, checksums and older comment files) were not re-fetched: no tool reads them. The ring pages (request 2) are where `SOURCES.md` says, under `~/juranometria-runs/uranus-raw/`.

### 1.2 Interval, time and frames (inherited)

**Intervals.** Uranus answers 1900–2100. The five moons answer 2000–2100, as above.

**The time model,** inherited unchanged from Saturn and Mars:
- UT1 before 1972, and UTC to the leap-second record's expiry on 2027-06-28;
- the ΔT model before and after;
- observers at sea level on WGS84.

**Frames.**
- **The chart position** is topocentric astrometric ICRS/J2000, light-time corrected.
- **Apparent places of date** are aberrated and deflected by the Sun, with Jupiter's formula. Leaving the deflection out more than doubles the apparent-place error and takes it over 0.2″ (`UranusMutationTest`).

### 1.3 Every observable and its meaning (U-3 to U-5, U-7, U-9)

**Uranus:**
- **distance** to the light-time-corrected barycentre;
- **equatorial diameter**, 2·asin(R_e/Δ);
- **polar diameter at the planetocentric tilt φ**, as Saturn's and Mars's;
- **phase angle**, held to Horizons' `phi`, not `S-T-O` (the aberrated down-leg and an unaberrated Sun each fail it, `UranusMutationTest`);
- **illuminated fraction**, (1 + cos i)/2. Uranus is never less than **99.92 %** lit (`measurements.md`: at least 99.9229 %, phase angle at most 3.1825°), so there are **no phase words** (U-5);
- **the north pole's position angle**, of date.

**The tilt is planetocentric (U-4).** Horizons' `ObsSub-LAT` is planetodetic: it differs from our planetocentric tilt by up to 4,784″, and from ours converted to planetodetic by under 0.37″. It is never shown as the tilt. Over 1900–2100 the sub-observer latitude runs from −82.6° to +82.6° (`measurements.md`): twice in two centuries the Earth looks almost straight down a pole.

**No elongation and no lit side.** U-9 rules Saturn's card, which has neither; the owner was offered each as one line at the #635 checkpoint and left the card as ruled.

**The moons.**
- **Offsets.** Horizons' apparent-of-date X/Y (X toward increasing right ascension, Y north), separation, position angle, and separation in Uranus radii.
- **Front or behind** comes from the line-of-sight depth, never from X/Y.
- **States (U-7).** Jupiter's six with Uranus's name: *behind Uranus, in its shadow*; *behind Uranus*; *in front of Uranus*; *in Uranus's shadow*; *partly in Uranus's shadow*; *clear of Uranus*. They are decided limb to limb on **Uranus's flattened figure** and its umbra. Horizons' equatorial sphere is the comparison's definition only.
- **Where the two differ.** On 2004-05-29 at Oslo Horizons codes Miranda O, behind its sphere; at 1.0035 radii it clears the flattened limb, and the table says *clear of Uranus* (`UranusTableTest`). The report counts such rows as *figure differs*: Miranda 4, Ariel 1, Umbriel 1, Titania 0, Oberon 0 (`service-accuracy.md`).
- **No shadow transits.** A moon's shadow on Uranus is not computed.

**The rings (U-8).** No ring rows in the table: their opening to us is the tilt, already shown. No shadows of or on the rings.

**The words.** English and Norwegian, as built in #635: *Avstand fra Uranus*, *Uranusradier*, *Fasevinkel*, *Helning mot deg*, *(nordpolen mot deg)*. The moon names are the same in both languages (U-9).

### 1.4 Moon inclusions and exclusions (U-6a, 2026-10-27)

**In:** all five classical moons, 2000–2100, decided on measured cost and value; the road (#632) forbids substituting a smaller set merely to save bytes.

**Not ruled in:**
- **The five, 1900–2100** (U-6b): 75,637,760 bytes.
- **The four without Miranda** (U-6c): 25,034,752 bytes. Miranda is the faintest and the most expensive per moon, but it holds most of the transits (122 transit rows alone).
- **The irregular moons** (`ura116xl`) and S/2023 U 1.

From the committed Horizons responses (`measurements.md`, Oslo, weekly 2000–2100), the moons Part II draws:

| moon | separation from Uranus's centre | angular diameter | magnitude |
|---|---|---|---|
| Miranda | 0.1133″–10.3213″ | 0.031428″–0.03834″ | 16.1–16.736 |
| Ariel | 0.2451″–15.1772″ | 0.075967″–0.092679″ | 13.95–14.586 |
| Umbriel | 0.3549″–21.2074″ | 0.076439″–0.093251″ | 14.6–15.236 |
| Titania | 1.1036″–34.7306″ | 0.103129″–0.125832″ | 13.519–14.156 |
| Oberon | 0.964″–46.4638″ | 0.099524″–0.121445″ | 13.729–14.366 |

No moon is ever 0.13″ across, so all five are points at every field. No magnitude is shown for any body: the road has no magnitude authority for them.

### 1.5 Measured errors, by era (`service-accuracy.md`, held by `UranusReferenceVectorTest`)

Every value is inside its target in force: U-10 (Saturn's and Mars's targets), with **the exact-era distance amended to 50 km** on the #634 checkpoint (2026-10-28, "Choose (a): keep the barycentre and amend the exact-era distance target to 50 km").
- **Why the amendment.** Horizons measures to Uranus's centre, and U-2 ships the barycentre, up to 45.136 km away. The #633 study measured U-2's angular effect and not its distance. The owner's ruling records that the amendment **corrects that missed consequence; it is not a relaxation caused by a faulty service.** The 7,000 km after the exact interval stands, and so does the builder's 50 km refusal.

| quantity | target in force | 1900–1961 | 1962–1971 | 1972 → 2027-06-28 | after |
|---|---|---:|---:|---:|---:|
| Uranus astrometric | 0.05 / 0.05 / 0.01 / 3″ | 0.0048″ | 0.0034″ | 0.0033″ | 0.3194″ |
| Uranus apparent, > 1° from the Sun | 0.3 / 0.1 / 0.2 / 3″ | 0.0902″ | 0.0837″ | 0.1307″ | 0.4507″ |
| Uranus distance | 50 / 50 / 50 / 7,000 km | 47.658 km | 42.964 km | 43.529 km | 4,000.031 km |
| equatorial diameter | 0.001 / 0.01″ | 0.00000″ | 0.00000″ | 0.00000″ | 0.00001″ |
| illuminated | 0.001 / 0.01 points | 0.00001 | 0.00001 | 0.00001 | 0.00001 |
| phase angle, against `phi` | 0.5 / 10″ | 0.1814″ | 0.1797″ | 0.1801″ | 0.4541″ |
| pole position angle, > 1° from the Sun | 0.5 / 3″ | 0.224″ | 0.192″ | 0.220″ | 1.422″ |
| moons X, Y, separation, > 1° from the Sun | 0.05 / 0.5″ | – | – | 0.0034″ | 0.0639″ |
| moons position angle, > 20″ from Uranus | 0.01°; printed after | – | – | 0.0051° | 0.0940° (printed) |
| horizontal, Uranus | 20″; printed after | 2.4166″ | 2.1012″ | 12.3085″ | 1.8644″ (printed) |
| horizontal, moons | 20″; printed after | – | – | 9.2767″ | 1.9055″ (printed) |

**Measured, printed, never asserted** (each inherited from Saturn's or Mars's rulings, and confirmed on the #634 checkpoint):
- the pole position angle **within 1° of the Sun**: 12.938″ and 16.779″ at conjunctions;
- the moons' position angle **within 20″ of Uranus** (Mars's threshold): 0.1421° exact, 9.0408° after;
- the moons' apparent place **within 1° of the Sun**: 0.1235″ exact, 1.9404″ after. At the 1981-11-23 conjunction, before the moons' years, Titania's separation is 1.62″ off with the light passing beside the Sun (`authority-comparison.md`);
- the planetodetic tilt against `ObsSub-LAT`.

**Moon states on Horizons' sphere.** These agree on every row through 2027. In the moons' years: Miranda 5,577 of 5,579, Ariel 5,577 of 5,579, Umbriel 5,578 of 5,579, Titania and Oberon 5,579 of 5,579. All five disagreements fall after the record's expiry (2048–2094), where the instant itself is estimated: printed, not asserted (Saturn's ruling, inherited).

### 1.6 Named cases, rivals and honest limits

**The study's "today", 2026-10-27 12:00 UTC at Oslo** (Horizons' row in `named-uranus-oslo.txt`).
- **Uranus** is 18.5757 AU away, 3.794″ across, 99.983 % lit at a phase angle of 1.499°, its north pole at position angle 289.79° and tipped towards the observer, 8.05° below the horizon.
- **All five moons** are clear of Uranus (Horizons codes `*`).
- **The match.** The packaged acceptance holds Uranus to Horizons' astrometric row within 0.01″; the image measured 0.0008″ (gate 5 at `02038686`).

**The states, named from Horizons' Oslo rows** (`UranusTableTest`): Umbriel behind Uranus on 2009-12-05 (O); Miranda behind it in its shadow on 2005-03-19 (U), which the packaged acceptance also checks; Miranda in front of it on 2005-05-28 (t); Ariel in its shadow on 2006-02-04 (u).

**Espenak's oppositions and conjunctions** (U-12).
- **Through the service.** Each is found from −210 to +37 minutes of the listed hour (`service-accuracy.md`): oppositions −38 to +14 minutes, conjunctions −210 to +37.
- **What that means.** It is **the almanac's own precision for Uranus**, a coarse cross-check, printed and never asserted. Oppositions and conjunctions are validated against Horizons; moon states are checked against Horizons only.

**Rivals** (`UranusMutationTest`). Each of these misses its target: no aberration; no deflection; the aberrated down-leg for the phase; an unaberrated Sun; the planetodetic tilt; the moons' offsets from J2000 places. There is no secular-pole-alone rival: Uranus's pole has no periodic terms.

**The boundary** (`UranusBoundaryTest`). The moons answer from 2000-01-01 to 2100-12-31 inclusive; an epoch outside their kernel is refused before it is read; Uranus answers on both sides.

### 1.7 Existing-body invariance

- **The builder proves the released kernels.** It refuses to write unless every released kernel carries its released digest: the Solar System, Jovian, planets (Venus, Mercury, Mars), Saturn System and Mars System kernels. `UranusSystemPackTest` holds them.
- **No released service changed.** Between `cfd5e0b1` and `6e45f155` the only files under `src/juranometria/solar` are new (`UranusService`, `UranusSystemPack`); nothing under `render`, `solarchart`, `chart` or `sky` changed.
- **The released tests ran unchanged.** The Sun, Moon, Mercury, Venus, Mars, Jupiter and Saturn reference tests ran unchanged on every head. The tests that did change are distribution and host journeys: the bundled notices, the About text, the Place and Time gate, the licensing count and the Solar System companion's order.

### 1.8 The Uranus table and its verdict (#635)

**Placement and behaviour.**
- **Where.** View > Solar System > *Uranus...* directly after *Saturn...*, lettered **N** in English and **U** in Norwegian. The Controller's Uranus group follows Saturn's, introduced collapsed.
- **One session,** shared, with host-local drafts.
- **Nothing computes uninvited.** Building, opening, expanding or restoring computes nothing and reads no pack: the years the table names are `UranusService`'s constants, which the service holds the pack's manifest to when it loads. The 38 MB pack is read on the first computation.

**What it shows.**
- **The card,** ten lines: position, height, distance, the diameter at the equator and the poles, illuminated fraction, phase angle, pole position angle and tilt.
- **The moons:** Miranda, Ariel, Umbriel, Titania and Oberon in Jupiter's columns.
- **Ranges** are grouped by instant, with three Uranus summary lines each, at most **200 instants** (five moon rows each, within the 1,000-row cap).
- **Outside the moons' years** Uranus is always shown, as Mars's table rules (2026-10-26): alone at an instant before 2000, and at every instant of a range, with a line where the moons are not computed and a count in the status.
- **No chart surface.** No Show on chart and no Centre on chart; *Set chart time to this instant* is offered, as in every table.

**Rulings (2026-10-28).**
- **Accepted untested.** "Don't got chance to test. Chance is ok, always been. Leave letter. Go on." The candidate `8a1f40e0` was accepted without the owner's packaged journey.
- **The letters.** "Great. No letter for mars": *Mars...* keeps no letter (#603); *Uranus...* keeps N and U.

### 1.9 Where the contract lives

- **`uranus-system.md`:** the study, the rulings, the #634 distance amendment and confirmations, and #634 and #635 as built.
- **`docs/studies/uranus-system/`:**
  - the sources: `inventory.md`, `SOURCES.md` (with the correction in §1.1) and `uranus-events.txt`;
  - the fixtures: `horizons/`;
  - the measurements: `authority-comparison.md`, `measurements.md` and `contract/pack-measurements.md`;
  - the service: `service-accuracy.md`. There is no table-measurements report: Mars's #603 had one, #635 did not add one.

## 2. Every stopped, superseded or incomplete run

All records are kept under `~/juranometria-runs/runs/` on the owner's machine, each under the head it ran on. Nothing was retried into green: every head that stopped was abandoned, and a new head was qualified from gate 1.

| head | issue | what happened | disposition |
|---|---|---|---|
| `6d5526c9` | #633 | the proposal head; gates 1 and 2 OK, **stopped during gate 3 on the owner's word**: "Skip them, post the checkpoint for rulings" (`ABORTED.md`) | the gate-economy rule: a head awaiting rulings is not qualified. Ruled head `2bda2543`: all seven gates OK, CI once |
| – | #634 | removing the #633 worktree deleted the raw `ura184_part-3.bsp`, kept in its `imports/raw` | re-fetch approved with conditions; checksums matched (§1.1). Raw downloads now live in the main checkout |
| `27fe0ed5` | #634 | the checkpoint head, not gated; development suite before it: the language ledger at 160 (one new literal), the About sheets and the test-evidence measurements stale, and the distance cell failing its #633 target | literal reworded (159), evidence regenerated, distance ruled (a); `4ca9ebb0`: all seven gates OK, CI once |
| `8a1f40e0` | #635 | the packaged candidate, not gated; development suite: the test-evidence hand-off count (1105 → 1106) and its measurements | regenerated and requoted; accepted 2026-10-28 |
| `c936de1b` | #635 | gates 1–3 OK; **STOPPED at gate 4**: the portable evidence contract ran out of its 1 GiB heap inside the label study (`GATE4-FAILED.md`). The label study alone under 1 GiB peaks at 372 MB live and reproduces its files; #635 touches no chart or label code | never retried; the owner chose 2 GiB for the three contract routes; `02038686`: all seven gates OK, CI once |

**Lessons the next milestone inherits:**
- **Raw downloads outlive worktrees.** Fetch into the main checkout's ignored `imports/raw/<system>/`, copy into a worktree, never the reverse.
- **A study must measure every observable a ruling touches, not only the angle.** The barycentre-for-centre ruling was measured in arcseconds and never in kilometres; the distance target met it only in #634.
- **The evidence contracts' heap is now 2 GiB,** and the record says what it retains is not yet measured. At 1 GiB, gate 4 took 38.6–38.8 minutes on the #633 and #634 heads; at 2 GiB it took 19.7 (§3.3).
- **The Uranus accuracy report is not registered in the evidence contract,** while Mars's is. A stale or hand-edited `service-accuracy.md` would not be caught there; `UranusReferenceVectorTest` still holds its numbers to the targets. Filed as a separate task, not done here.
- **A new identifier on an application surface is routed, not reviewed.** The ledger stands at 159 of fewer than 160; one more unrouted literal fails the suite.
- **A menu or Controller group added in outward order moves every journey that pins that order, and the companion sheets** (Mars's lesson, met again).

**Gate 1's aborted count** was 28, 12 and 49 across the qualified heads. Every gate record's list of unexplained aborts is empty.

## 3. Routes, evidence movement and performance

### 3.1 The route every PR took

Each PR ran the wide route once per head, after all seven local gates: `test`, `display`, `evidence`, `classify`, `app-image` and `dist`. Each merge was pinned to its exact head and landed under #494, structurally verified 4/4.

| PR | issue | head → merge | PR `test` run | display (found / failed / aborted) | landing `test` run |
|---|---|---|---|---|---|
| #644 | #633 | `2bda2543` → `cfd5e0b1` | 40579969801 | 2,432 / 0 / 0 | 40592825059 |
| #645 | #634 | `4ca9ebb0` → `6e194e16` | 40614822836 | 2,465 / 0 / 0 | 40623535677 |
| #646 | #635 | `02038686` → `6e45f155` | 40667748467 | 2,479 / 0 / 0 | 40680850352 |

### 3.2 What evidence moved

| merge | `docs/studies` | whole change |
|---|---|---|
| `cfd5e0b1` (#633) | 60 files: `docs/studies/uranus-system/` created (59), the test-evidence measurements | 67 files, +67,750 |
| `6e194e16` (#634) | 7 files: `service-accuracy.md`, the About sheets and strings (the new notice), provenance, the test-evidence measurements | 37 files, +3,277 |
| `6e45f155` (#635) | 7 files: the companion sheets and strings with the Uranus group, provenance, the test-evidence measurements | 26 files, +2,498 |

**No released chart output moved.** Only these directories moved: `uranus-system` (new), `interface-language` (the About and companion sheets only), provenance and the test-evidence measurements. No chart, sheet, on-the-chart, printable, regional, overview, globe or closer-field evidence moved. The test-evidence hand-off count stands at 1,106.

### 3.3 Performance, measured

**Local gates on this Mac,** from the gate records' own file times:

| gate | `2bda2543` | `4ca9ebb0` | `02038686` |
|---|---:|---:|---:|
| 1 (2,432–2,479 tests) | 12.1 min | 11.8 min | 12.4 min |
| 2 | 3.6 min | 3.7 min | 3.6 min |
| 3 | 19.4 min | 20.1 min | 18.3 min |
| 4 (1 GiB, 1 GiB, 2 GiB) | 38.6 min | 38.8 min | 19.7 min |
| 5–7 | 1.1 min | 1.1 min | 1.2 min |
| **a head, in all** | **74.9 min** | **75.5 min** | **55.1 min** |

Gate 4 took 23.5–24 minutes on the Mars road's heads (`mars-handover.md`). It rose to nearly 39 minutes at 1 GiB on this road and fell to under 20 at 2 GiB; the timing is measured, the cause not.

**Install size.** The Uranus System pack adds 38.4 MB to every image (387,072 + 38,055,936 bytes).

## 4. What Part II must decide, and what it inherits

### 4.1 Size on the page

Computed from `measurements.md`: Uranus's equatorial diameter over 1900–2100 (3.34151″ to 4.07631″, weekly from Oslo), the moons' greatest separations over 2000–2100, and the Epsilon ring, about 8″ across at the nearest (`uranus-system.md`). On the 900 × 700 page, a field of F degrees across its width gives 900 / (F · 3600) px per arcsecond:

| field | Uranus's diameter, px | Miranda | Ariel | Umbriel | Titania | Oberon | Epsilon ring, px |
|---:|---|---:|---:|---:|---:|---:|---:|
| 12° | 0.07–0.08 | 0.22 | 0.32 | 0.44 | 0.72 | 0.97 | 0.17 |
| 8° | 0.10–0.13 | 0.32 | 0.47 | 0.66 | 1.09 | 1.45 | 0.25 |
| 4° | 0.21–0.25 | 0.65 | 0.95 | 1.33 | 2.17 | 2.90 | 0.50 |
| 2° | 0.42–0.51 | 1.29 | 1.90 | 2.65 | 4.34 | 5.81 | 1.00 |
| 1° | 0.84–1.02 | 2.58 | 3.79 | 5.30 | 8.68 | 11.62 | 2.00 |
| 0.5° | 1.67–2.04 | 5.16 | 7.59 | 10.60 | 17.37 | 23.23 | 4.00 |
| 0.25° | 3.34–4.08 | 10.32 | 15.18 | 21.21 | 34.73 | 46.46 | 8.00 |

The moons' columns are each moon's greatest separation from Uranus's centre, in pixels.

**What the table shows.** A 6 px mark reaches 3 px from Uranus's centre.
- **Uranus is a symbol at every field.** Its true disc never reaches 6 px, not even at 0.25° (4.08 px at most). Under rule M the minimum mark always stands in.
- **Inside Uranus's minimum mark.** At 4° and wider no moon ever leaves 3 px of Uranus's centre. At 2° only Titania and Oberon can, near their greatest separations; at 1° Ariel and Umbriel join them; Miranda clears 3 px only at 0.5° and closer.
- **The closer fields decide Miranda.** Uranus is not eligible for 0.5° or 0.25° (§4.2, 7). Without them Miranda is never separable from Uranus's mark.
- **The rings** are a pixel across at 2° and 8 px only at 0.25°; the Epsilon ring's width is far below a pixel at every field.

Part II measures every field and paper size on real production pages (#637).

### 4.2 Questions Part II must answer (#637)

1. **The mark.** Rule M is the precedent: the 6 px symbol or the true size, whichever is larger. For Uranus the symbol stands at every field, so the question is what that symbol shows. Uranus is always at least 99.92 % lit: is its mark ever phased, or always full? Is its flattening (2.3 % between the radii) ever visible? Not below about 4 px.
2. **The pole.** Uranus's pole stands at RA 257.311°, Dec −15.175°, and Horizons' sub-observer latitude reaches ±82.6° over 1900–2100. Part II decides whether the mark shows the pole or the tilt at all; the table already gives both in numbers.
3. **Colour and surface.** The road forbids invented surface markings, false textures and unsupported colour. Part II must say whether Uranus gets any colour at all, and on what authority. The table offers none.
4. **The moons.** There are five marks, each decided on its own, as `SatelliteMarks` decides Jupiter's and Saturn's, with separability from the planet's mark, not only from its true disc, deciding drawability (§4.1). The decisions: precedence, labels through the bodies' adjacent boxes, refused alone, never under duress.
5. **The rings (#637).** Whether an Epsilon outline is drawn at any field, measured on real pages; it is a pixel at 2°. No ring shadows.
6. **Vocabulary on the chart.** Jupiter's vocabulary A for the six states. There are no satellite shadow spots: a moon's shadow on Uranus is not computed.
7. **The years.** Uranus is drawn over 1900–2100. The moons are drawn only over 2000–2100, refused by name elsewhere, as the table does. Uranus is never hidden because the moons are not computed.
8. **Selection and Centre on chart.** Uranus's identity on the page, and the moons' if any is selectable, through the occurrence model (#513). The table's Show on chart and Centre on chart wait for the layer.
9. **The closer fields.** Uranus is not yet eligible for 0.5° and 0.25°. The #481 contract named Mercury, Venus and Jupiter; each later planet waits for its own cartography study. For Uranus, eligibility decides whether Miranda is ever drawn apart from the planet.
10. **Screen, paper, accessibility and export.** These cover:
    - the spoken description: a symbol named as one, the tilt in words, the states;
    - export through the same module;
    - Solar System label emphasis (#526);
    - the horizon and the page edge;
    - "a symbol never erases a star";
    - the symbol-layering rule of #514.
11. **The production split.** Planet and moons in separately reviewable PRs if the study supports it, then #638's packaged checkpoints.

### 4.3 The Mars article and the release boundary (#632, Part II)

Part II publishes the bilingual Mars reader article (#639), keeping the one-planet-later cadence. The Uranus article waits for the following planet road. Part II then prepares the road's handover and release boundary (#640).

The release requires all of these:
- accepted Uranus numbers, which are done;
- the ruled moons and Uranus on the chart;
- the Mars article;
- one integrated evidence flush;
- a packaged release journey.

Completing Part I changes no `VERSION`. Tag and publication remain separate owner decisions.

### 4.4 Explicit exclusions (#632, unchanged)

- No invented surface markings, albedo map, false photographic texture or unsupported colour.
- No general event search, animation, live tracking or rise/set service, and no shadow transits.
- No Neptune or Pluto.
- No magnitude for any body without its own authority.
- Every external download requires prior approval.

### 4.5 Deferred by name

- **The moons before 2000** need the five-moon excerpt over 1900–2100 (U-6b, 75,637,760 bytes), which was not ruled in.
- **The irregular moons** (`ura116xl`) were not studied for inclusion.
- **A Uranus magnitude** needs its own model and authority.
- **Registering `service-accuracy.md` in the evidence contract** (§2).
- **Measuring what the evidence contract's process retains** (§2, §3.3).
