# Mars, numerically: the handover to cartography

**Issue #604, closing Part I of the Mars road (#600).** This record hands the settled numerical Mars contract to Part II: #605, the cartography study; #606, Mars on the chart; #607, the Saturn article; and #608, the release boundary. It records; it builds nothing.

Every number it quotes comes from a report, a fixture or a run record named beside it. Every ruling it quotes is the owner's, dated. The contract itself is `mars-system.md`: the study, the #601 rulings, the #602 checkpoint ruling, and #602 and #603 as built. This document says what stands, what was measured, what stopped, what moved, and what Part II must decide.

**Baseline.** The Mars feature work (#601–#603) was complete at main `9470b0a2` (2026-10-26). There was no infrastructure repair chain. No `VERSION` change (5.4.0), no tag, no release.

## 1. What is settled

### 1.1 Authority, pack and licensing (#601: M-1, M-7a with the amendment, M-11; #602)

**The planet: DE440's Mars barycentre (0 → 4).**
- **Source.** It comes from the released `de440s.bsp`, over 1900–2100, as the planets pack's third kernel.
- **Why the barycentre.** NAIF's `mar099s.bsp` puts Mars's centre within **0.210 m** of the barycentre. The planets builder re-measures this from the pinned kernel before it writes, and refuses above 0.25 m.
- **Against Horizons.** DE440's barycentre alone agrees with Horizons' Mars centre to 0.0208″ astrometric over 1900–2100 (`authority-comparison.md`).
- **DE442s.** It moves the geocentric direction by at most 0.0141″. It was a measurement only.

**The moons: NAIF's `mar099s.bsp`.**
- **Bodies and years.** Phobos (4 → 401) and Deimos (4 → 402), from 2000 up to, **not including**, 2050-01-01 00:00 TDB. That is the kernel's own end, an exclusive boundary.
- **The boundary check.** A civil instant is converted to TDB with the atlas's time scales before the comparison. The reader is told 2000 to 2049, and the kernel's final instant is never evaluated.
- **The barycentre they stand on.** `mar099s`'s own Mars barycentre is bit-identical to the released DE440's.
- **The long kernel.** `mar099.bsp` (1.2 GB, 1600–2600) was neither downloaded nor ruled in.

**Constants: IAU 2015, from `pck00011.tpc`.**
- **Mars.** Radii 3396.19, 3396.19 and 3376.20 km, the polar radius being the mean of the north and south.
- **The pole in full,** with its five periodic terms in right ascension and five in declination over the shared Mars-system angles. Without those terms the pole stands about 1.5° from Horizons'.
- **The moons.** Phobos 13.0, 11.4 and 9.1 km; Deimos 7.8, 6.0 and 5.1 km.

**The packs, built by `make import-planets` and `make import-mars-system`.** Each rebuilds byte-identically (`mars-system.md`, *#602, measured*):

| file | bytes | SHA-256 |
|---|---:|---|
| `planets/juranometria-de440-mars-1900-2100.bsp` (0 → 4) | 709,632 | `c59b91b0…` |
| `mars-system/juranometria-mar099-moons-2000-2050.bsp` (4 → 401, 402) | 43,951,104 | `cd84a9bf…` |

- **Larger than the cut, by attribution only.** Each file is larger than the #601 cut (648,192 and 43,910,144 bytes) by its comment area: NAIF's required modification notice and the source's own comments, kept as attribution. No ephemeris byte differs from the source.
- **The planets pack is version 3.** Venus's and Mercury's files are unchanged.

**Licensing.**
- **The rules.** NAIF's rules for modified kernels, stated in the planets notice and the new `NOTICE-mars-system.md`.
- **Where it is carried.** The About dialog (both languages), `LICENSING.md`, the licensing summary, the app-image list and the portable archive.
- **Horizons and Espenak.** Horizons responses are committed under its public terms. Espenak's almanac pages are kept outside Git, and only the quoted rows are committed (`mars-events.txt`).

**Fixtures.**
- 36 Horizons responses committed (6,123,846 bytes).
- Three geocentric matrices retained outside Git (4,660,673 bytes), with their digests in `horizons/README.md`.
- Seven almanacs' Mars rows, quoted with their pages' digests.

### 1.2 Interval, time and frames (M-6)

**Intervals.** Mars answers 1900–2100. Phobos and Deimos answer from 2000 to the exclusive TDB boundary above.

**The time model,** inherited unchanged:
- UT1 before 1972, and UTC to the leap-second record's expiry on 2027-06-28 (`Leap_Second.dat`);
- the ΔT model before and after;
- observers at sea level on WGS84.

**Frames.**
- **The chart position** is topocentric astrometric ICRS/J2000, light-time corrected.
- **Apparent places of date** are aberrated and deflected by the Sun, with Jupiter's formula (M-5). For Mars that deflection matters: without it the apparent place misses Horizons by 0.346″, against 0.130″ with it (`MarsMutationTest`).

### 1.3 Every observable and its meaning (M-2 to M-4, M-8)

**Mars:**
- **distance** to the light-time-corrected barycentre;
- **equatorial diameter**, 2·asin(R_e/Δ);
- **polar diameter at the planetocentric tilt φ**, 2·asin(b′/Δ) with b′ = √(R_p² cos² φ + R_e² sin² φ);
- **phase angle**, with the Sun where Mars sees it (light-timed, aberrated by Mars's motion) against the light-time-corrected down-leg. It is held to Horizons' `phi`, not `S-T-O`;
- **illuminated fraction**, (1 + cos i)/2;
- **phase words**, Venus's and Mercury's bands with the geometric states first. Mars is never less than 83.86 % lit, so only *gibbous*, *near full* and *behind the Sun* occur;
- **elongation** with E or W by right ascension;
- **lit side**, χ of date with the nearest of sixteen points;
- **the north pole's position angle**, of date.

**The tilt is planetocentric.** It is the observer's latitude over the PCK pole along the line of sight. Horizons' `ObsSub-LAT` is planetodetic: it agrees with ours converted to planetodetic within 0.013″, and is never shown as the tilt.

**The moons.**
- **Offsets.** Horizons' apparent-of-date X/Y (X toward increasing right ascension, Y north), separation, position angle, and separation in Mars radii.
- **Front or behind** comes from the line-of-sight depth, never from X/Y.
- **States.** Jupiter's six with Mars's name: *behind Mars, in its shadow*; *behind Mars*; *in front of Mars*; *in Mars's shadow*; *partly in Mars's shadow*; *clear of Mars*. They are decided limb to limb on **Mars's flattened figure** and its umbra. Horizons' equatorial sphere is the comparison's definition only.
- **No shadow transits.** A moon's shadow on Mars is not computed.

**The words.** English and Norwegian, as built in #603: *Avstand fra Mars*, *Marsradier*, *Helning mot deg*, *(nordpolen mot deg)*.

### 1.4 Moon inclusions and exclusions (M-7a, 2026-10-25)

**In:** Phobos and Deimos, 2000–2049. The recommendation the owner pasted with the ruling gave the reason: Phobos "is too interesting to omit: its rapid motion, eclipses and transits are a large part of what makes the Mars system worth including."

**Not ruled in:**
- **Deimos alone** (M-7b).
- **Both moons to 2100** (M-7c), which needed the 1.2 GB kernel.
- **No moons.**

From the committed Horizons responses (`measurements.md`), the moons Part II draws:

| moon | separation from Mars's centre, weekly 1995–2049 | greatest at the 2003 opposition | angular diameter | magnitude, geocentric |
|---|---|---:|---|---|
| Phobos | 0.26″–34.22″ | 35.02″ | 0.013″–0.096″ | 10.40–15.04 |
| Deimos | 0.47″–84.79″ | 86.77″ | 0.008″–0.058″ | 11.49–16.13 |

Neither moon is ever 0.1″ across, so both are points at every field. No magnitude is shown for any body: the road has no magnitude authority for them.

### 1.5 Measured errors, by era (`service-accuracy.md`, held by `MarsReferenceVectorTest`)

Every value is inside its target in force: M-10 as amended on the #602 checkpoint (2026-10-25, "Rule the #602 amendments as proposed, with the wording notes").
- **Why the amendments.** They corrected study targets derived through Skyfield's clock and of-date machinery.
- **The of-date bound.** The 1972–2027 apparent-place bound is the accepted accuracy of the atlas's existing of-date model, the same Jupiter and Saturn were ruled on.
- **The former targets stay in the test.** It fails if any amended cell comes inside its former #601 target without the record being updated.

| quantity | target in force | 1900–1961 | 1962–1971 | 1972 → 2027-06-28 | after |
|---|---|---:|---:|---:|---:|
| Mars astrometric | 0.05 / 0.05 / 0.01 / 5″ | 0.0372″ | 0.0120″ | 0.0004″ | 4.0278″ |
| Mars apparent, > 1° from the Sun | 0.3 / 0.1 / 0.2 / 5″ | 0.1155″ | 0.0847″ | 0.1348″ | 4.1389″ |
| Mars distance | 40 / 20 / 1 / 7,000 km | 19.951 km | 2.624 km | 0.179 km | 2,281.646 km |
| equatorial diameter | 0.001 / 0.01″ | 0.00001″ | 0.00001″ | 0.00001″ | 0.00029″ |
| illuminated | 0.001 / 0.01 points | 0.00001 | 0.00001 | 0.00001 | 0.00039 |
| phase angle, against `phi` | 0.5 / 10″ | 0.2064″ | 0.1801″ | 0.1800″ | 4.9490″ |
| pole position angle | 0.5 / 3″ | 0.223″ | 0.207″ | 0.222″ | 2.670″ |
| moons X, Y, separation, > 1° from the Sun | 0.05 / 0.5″ | – | – | 0.0006″ | 0.1169″ |
| moons position angle, > 20″ from Mars | 0.01°; printed after | – | – | 0.0060° | 0.1584° (printed) |
| horizontal | 20″; printed after | 0.8970″ | 2.1111″ | 11.6773″ | 3.2200″ (printed) |

**Moon states on Horizons' sphere.** These agree on every row through 2027: Phobos 3,777 of 3,780 in all, Deimos 3,780 of 3,780. The three Phobos disagreements all fall after the record's expiry, where states are printed and not asserted.

### 1.6 Named cases, rivals and honest limits

**The study's "today", 2026-10-25 12:00 UTC at Oslo.**
- **Mars** is gibbous (89.6 %) in the morning sky, its north pole tipped towards the observer.
- **Both moons** are clear of Mars.
- **The match.** Mars agrees with Horizons' astrometric row to within 0.01″, which the packaged acceptance checks.

**Phobos's states, named from Horizons' Oslo rows.** It is behind Mars on 2005-02-20, behind it in its shadow on 2005-02-27, in its shadow on 2005-03-06, and in front of it on 2005-08-07 (`MarsTableTest`). From the Earth's centre on 2018-07-26 12:30 it is behind Mars in its shadow, which the packaged acceptance checks. At the 2018 opposition, Phobos passes behind Mars into its shadow and later in front of it on one orbit (`table-measurements.md`).

**Espenak's oppositions and conjunctions** (M-12, as amended).
- **Through the service.** Each is found within three hours of its listed hour: from 4 to 158 minutes away.
- **What the three hours means.** It is **the almanac's own precision for Mars**, never a claim of computed event-search accuracy.
- **Why the almanac is coarse.** Near opposition the longitude difference from the Sun moves only 1.23° to 1.41° a day. The four rows beyond the former hour are named in `MarsComparison`.
- **Opposition is not closest approach.** For 2018 and 2035 the closest approach lies more than three days from opposition.

**Rivals** (`MarsMutationTest`). Each of these misses its target:
- no aberration;
- no deflection;
- the secular pole alone;
- the aberrated down-leg for the phase;
- an unaberrated Sun;
- the planetodetic tilt;
- the moons' offsets from J2000 places.

**The boundary.** At 23:58 UTC on 2049-12-31 the moons answer. At 23:59:30 they are refused, because that civil instant already lies past 2050-01-01 00:00 TDB (`MarsBoundaryTest`).

### 1.7 Existing-body invariance

- **The builders prove the released kernels.** Both builders refuse to write unless every released kernel carries its released digest: the Solar System, Jovian, Saturn System, Venus and Mercury kernels. `PlanetsPackTest` and `MarsSystemPackTest` hold them.
- **No released service changed.** `MarsService` is new code beside them, its deflection held equal to Jupiter's (`MarsServiceTest`).
- **The loader change is compatible.** The planets loader reads a planet's figure body from a new manifest key. Venus and Mercury load as before.
- **The released tests ran unchanged.** The Sun, Moon, Mercury, Venus, Jupiter and Saturn reference tests ran unchanged on every head.

### 1.8 The Mars table and its verdict (#603)

**Placement and behaviour.**
- **Where.** The Controller's Mars group sits between the Moon's and Jupiter's, introduced collapsed. View > Solar System > *Mars...* sits between *Moon...* and *Jupiter...*.
- **One session.** It is shared, with host-local drafts.
- **Nothing computes uninvited.** Building, opening, expanding or restoring computes nothing, and the 44 MB moons' pack is read on the first computation.

**What it shows.**
- **The card,** twelve lines: position, height, distance, the diameter at the equator and the poles, illuminated fraction and phase, elongation, lit side, pole position angle, and tilt.
- **The moons:** Phobos and Deimos in Jupiter's and Saturn's columns.
- **Ranges** are grouped by instant, with three Mars summary lines each, at most 500 instants.
- **No chart surface.** No Show on chart and no Centre on chart.

**Rulings (2026-10-26).**
- **Mars outside the moons' years is always shown.** It is shown alone at an instant. A range keeps Mars at every instant, with a line where the moons are not computed and a count in the status.
- **No access letter.** *Mars…* has no menu mnemonic in either language, and every existing letter is kept: "Revisit only if the Solar System menu is reorganized later."
- **Accepted.** The packaged candidate `1020fdc7` was **accepted by the owner (2026-10-26)**.

### 1.9 Where the contract lives

- **`mars-system.md`:** the study, the rulings, the #602 checkpoint ruling, and #602 and #603 as built.
- **`docs/studies/mars-system/`:**
  - the sources: `inventory.md`, `SOURCES.md` and `mars-events.txt`;
  - the fixtures: `horizons/`;
  - the measurements: `authority-comparison.md`, `measurements.md` and `contract/pack-measurements.md`;
  - the service: `service-accuracy.md` and `table-measurements.md`.

## 2. Every stopped, superseded or incomplete run

All records are kept under `~/juranometria-runs/runs/` on the owner's machine, each under the head it ran on. Nothing was retried into green: every head that stopped was abandoned, and a new head was qualified from gate 1.

| head | issue | what happened | disposition |
|---|---|---|---|
| `bb4c2b0d` | #601 | the read-only inventory and download request; no gate run | the approved sources fetched; the study measured and proposed at `a6d4d10a` |
| `a6d4d10a` | #601 | the proposal head; development suite: 1 failure, the test-evidence report not reproducing (the study's files counted) | report regenerated; ruled head `9a1b5bad`: all seven gates OK, CI once |
| `be01a1b2` | #602 | the measured, not-qualified checkpoint; development suite before it: the language ledger at 164 (five new literals), the About premises (11 → 12 documents), the About evidence | literals routed (159), premises and evidence regenerated; checkpoint ruled |
| `00afefc3` | #602 | gates 1–5 OK; **STOPPED at gate 6**: the portable archive lacked `NOTICE-mars-system.md` - `verify-dist.sh` expected it, the Makefile's `dist` copy had not gained it | added on `efcc72a0`: all seven gates OK, CI once |
| `1020fdc7` | #603 | the packaged candidate; development suite: 1 failure, the test-evidence decision's hand-off count (1104 → 1105) | requoted before the candidate; accepted 2026-10-26 |
| `7f214aba` | #603 | all seven gates OK; **CI STOPPED** (run 40306977207): the Linux image's packaged acceptance read the Controller's *wrapped* status line, broken across `<br>` by that platform's fonts | never retried; the check reads the status's spoken, unwrapped name on `21cb1802`: all seven gates OK, CI once |

**Lessons the next milestone inherits:**
- **This is the second time a new notice missed the Makefile's `dist` copy** (Saturn's #575 was the first). The list is still a hand edit that only `verify-dist.sh`, at gate 6, catches.
- **A packaged check reads the spoken name, never the shown text, of anything a narrow host wraps.**
- **New identifiers on an application surface are routed, not reviewed.** The ledger stands at 159 of fewer than 160.
- **A study's targets must come from the shipped calculation's own time and of-date models,** not a library's. The #602 amendments corrected exactly that.
- **A menu or Controller group added in outward order moves every journey that pins that order, and the companion sheets.**

**Gate 1's aborted count** was 12 or 49 across the qualified heads. Every gate record's list of unexplained aborts is empty.

## 3. Routes, evidence movement and performance

### 3.1 The route every PR took

Each PR ran the wide route once per head, after all seven local gates: `test`, `display`, `evidence`, `classify`, `app-image` and `dist`. Each merge was pinned to its exact head and landed under #494, structurally verified 4/4.

| PR | issue | head → merge | PR run | display (found / failed / aborted) | landing `test` run |
|---|---|---|---|---|---|
| #625 | #601 | `9a1b5bad` → `ef9e3656` | 40225797934 | 2,370 / 0 / 0 | 40229718758 |
| #626 | #602 | `efcc72a0` → `0d9854bc` | 40245982170 | 2,403 / 0 / 0 | 40281283483 |
| #627 | #603 | `21cb1802` → `9470b0a2` | 40314722795 | 2,415 / 0 / 0 | 40323547862 |

### 3.2 What evidence moved

| merge | `docs/studies` | whole change |
|---|---|---|
| `ef9e3656` (#601) | 44 files: `docs/studies/mars-system/` created (43), the test-evidence measurements | 51 files, +33,578 |
| `0d9854bc` (#602) | 7 files: `service-accuracy.md` and `table-measurements.md`, the About sheets and strings (the new notice), provenance, the test-evidence measurements | 46 files, +4,096 |
| `9470b0a2` (#603) | 7 files: the companion sheets and strings with the Mars group, provenance, the test-evidence measurements | 24 files, +2,416 |

**No released chart output moved.** Only these directories moved: `mars-system` (new), `interface-language` (the About and companion sheets only), provenance and the test-evidence measurements. No chart, sheet, on-the-chart, printable, regional, overview, globe or closer-field evidence moved. The test-evidence hand-off count stands at 1,105.

### 3.3 Performance, measured

**Local gates on this Mac,** from the gate records' own file times:

| gate | time |
|---|---|
| 1 (2,370–2,415 tests) | 11.5–12.5 min |
| 2 | about 3.6 min |
| 3 | about 19 min |
| 4 | 23.5–24 min |
| 5–7 | about a minute |
| **a head, in all** | **59–60 min** |

**Install size.** The Mars System pack adds 44 MB to every image, and the planets pack 0.7 MB.

## 4. What Part II must decide, and what it inherits

### 4.1 Size on the page

Computed from `measurements.md`: Mars's equatorial diameter over 1900–2100 (3.50″ to 25.10″, weekly from Oslo) and the moons' greatest separations at the 2003 opposition. On the 900 × 700 page, a field of F degrees across its width gives 900 / (F · 3600) px per arcsecond:

| field | Mars's diameter, px | Phobos, greatest separation, px | Deimos, greatest separation, px |
|---:|---|---:|---:|
| 12° | 0.07–0.52 | 0.73 | 1.81 |
| 8° | 0.11–0.78 | 1.09 | 2.71 |
| 4° | 0.22–1.57 | 2.19 | 5.42 |
| 2° | 0.44–3.14 | 4.38 | 10.85 |
| 1° | 0.87–6.27 | 8.75 | 21.69 |
| 0.5° | 1.75–12.55 | 17.51 | 43.39 |
| 0.25° | 3.50–25.10 | 35.02 | 86.77 |

**What the table shows.** A 6 px mark reaches 3 px from Mars's centre.
- **Under rule M's 6 px.** Mars is under 6 px at every field down to 2°. It exceeds 6 px only at 1° at the most favourable oppositions, and at the closer fields.
- **Inside Mars's minimum mark.** Even at its greatest separation, Phobos lies within 3 px of Mars's centre at 4° and wider. It reaches 4.38 px at 2°.
- **Clear of it.** Deimos clears 3 px from 4° near a favourable opposition (5.42 px), but not at 8° (2.71 px).

Part II measures every field and paper size on real production pages (#605).

### 4.2 Questions Part II must answer (#605)

1. **True scale or a minimum mark.** Rule M is the precedent: Jupiter's 6 px or the true size, whichever is larger, a symbol while the minimum stands in, and Venus's phased symbol. For Mars, the questions are whether its mark is phased, and at which field the true disc takes over. Mars is always at least 83.86 % lit, so its phase is a slim gibbous at most. The polar diameter at the tilt differs from the equatorial by at most about 0.6 %, below a pixel until the closer fields.
2. **Colour and surface.** The road forbids invented surface markings, albedo maps, false textures and unsupported colour. Part II must say whether Mars gets any colour at all, and on what authority. The table offers none.
3. **The moons.** There are two marks, each decided on its own, as `SatelliteMarks` decides Jupiter's and Saturn's. From 4° outward Phobos never leaves Mars's minimum mark, so separability from the planet's mark, not only from its true disc, decides whether Phobos is drawable there at all. The decisions: precedence, labels through the bodies' adjacent boxes, refused alone, never under duress.
4. **Vocabulary on the chart.** Jupiter's vocabulary A for the six states. There are no satellite shadow spots: a moon's shadow on Mars is not computed.
5. **The years.** Mars is drawn over 1900–2100. The moons are drawn only from 2000 to the exclusive TDB boundary, refused by name elsewhere, as the table does. Mars is never hidden because the moons are not computed.
6. **Selection and Centre on chart.** Mars's identity on the page, and the moons' if any is selectable, through the occurrence model (#513). The table's Centre on chart waits for the layer.
7. **The closer fields.** Mars is not yet eligible for 0.5° and 0.25°. The #481 contract named Mercury, Venus and Jupiter, and Saturn and Mars each wait for their own cartography study.
8. **Screen, paper, accessibility and export.** These cover:
   - the spoken description: a symbol named as one, the phase and tilt in words, the states;
   - export through the same module;
   - Solar System label emphasis (#526);
   - the horizon and the page edge;
   - "a symbol never erases a star";
   - the symbol-layering rule of #514.
9. **The production split.** Planet and moons in separately reviewable PRs if the study supports it, then #606's packaged checkpoints.

### 4.3 The Saturn article and the release boundary (#600, Part II)

Part II publishes the bilingual Saturn reader article (#607), keeping the one-planet-later cadence. The Mars article waits for the following planet road. Part II then hands Mars to release preparation (#608).

The release requires all of these:
- accepted Mars numbers, which are done;
- the ruled moons and Mars on the chart;
- the Saturn article;
- one integrated evidence flush;
- a packaged release journey.

Completing Part I changes no `VERSION`. Tag and publication remain separate owner decisions.

### 4.4 Explicit exclusions (#600, unchanged)

- No invented surface markings, albedo map, false photographic texture or unsupported colour.
- No general event search, animation, live tracking or rise/set service, and no Jovian or Saturnian shadow extension.
- No Uranus, Neptune or Pluto.
- Time-range paths, the Sun–Earth–planet diagram, meteor showers, bookmarks and article-library access remain their own roads.
- Every external download requires prior approval.

### 4.5 Deferred by name

- **Phobos and Deimos after 2049** need `mar099.bsp` (1.2 GB), which was not ruled in.
- **A Mars magnitude** needs its own model and authority.
- **The GUI refinements** the owner set aside on 2026-10-25 come after Mars.
- **The menu letter for *Mars…*** is revisited only if the Solar System menu is reorganized.
