# Knowing where Saturn, its rings and its moons are

**Sprint 52, issue #574.** This is the numerical contract for Saturn, its ring geometry and its major moons. It is proposed from measurement before any production data or code exists, and awaits the owner's rulings. It inherits the Sun's, the Moon's and Jupiter's contracts (`sun-computation.md`, `moon-computation.md`, `jovian-system.md`) wherever it does not say otherwise.

The study is `docs/studies/saturn-system/`:
- `make saturn-system-study` builds the proposal's mock-ups and diagrams, from the committed fixtures.
- `make saturn-pack-study` builds the excerpt; its report is kept as `contract/pack-measurements.md`.
- `scripts/saturn-authority-comparison.py` builds the comparison against Horizons and DE442s (`authority-comparison.md`).
- `scripts/saturn-imcce-comparison.py` builds the event times against IMCCE (`imcce-comparison.md`).

Every number quoted here is one of those reports'. None is typed from memory.

> Saturn is the coolest planet. The atlas first proves where Saturn, its rings and its moons are, and which moon is actually visible. Only then may the chart draw them. (#573)

## The question

Before the road to 5.4.0 draws anything of Saturn, the atlas must answer a reader's questions at a chosen instant, offline, permanently, to a measured accuracy and in both languages:
- where Saturn is, and how large its globe and rings appear;
- how far the rings are open, and to which face, toward the observer and toward the Sun;
- where each ruled moon lies relative to Saturn;
- whether each moon is clear, in front, behind or in Saturn's shadow.

This record settles:
- the authority and its terms;
- the pack and the interval;
- the time model;
- the frames and quantities;
- the ring model;
- the moon set;
- the states and the vocabulary;
- the fixtures and the targets;
- the production split.

It builds nothing.

## The authority, inventoried from the official sources (2026-10-19)

Each source was approved by the owner before retrieval. Each is kept whole with its URL, retrieval time, byte count and SHA-256, in `SOURCES.md`, `rings/SOURCES.md` and `horizons/README.md`.

- **NAIF's `sat441.bsp`** (661,592,064 bytes, kept outside Git). It is the only NAIF Saturn kernel that carries both Saturn's centre (6 → 699) and the eight major moons (6 → 601–608). `sat455`, `sat456`, `sat457`, `sat459` and `sat480` carry Saturn but none of 601–608. Every segment is SPK Type 2 in J2000 and covers 1749-12-30 to 2250-01-06. Each body has two segments, split at JD 2456928.5, which the epoch-aware reader and writer of #473 already handle. sat441's own barycentre and Earth are de440s's, to the bit. Measured weekly over 1900–2100 (`authority-comparison.md`), both differ by 0 km.
- **IAU 2015 constants, `pck00011.tpc`** (already held from the Jovian study):
  - Saturn's radii are 60268, 60268 and 54364 km.
  - The pole is RA 40.589 − 0.036 T, Dec 83.537 − 0.004 T, with no nutation terms.
  - Each moon has three radii.
  - The PCK has no ring constants.
- **The ring radii.** The primary source is the PDS Ring-Moon Systems Node's *Rings of Saturn* table, retained whole (44,980 bytes) and extracted row by row into `rings/pds-rings-table.tsv`. The NSSDCA *Saturnian Rings Fact Sheet* is kept as a secondary cross-check only. French et al. 2017 (*Absolute radius scale and Saturn's pole direction*) is cited through the table's footnote 10 only; no paper was downloaded.
- **JPL Horizons, the in-family comparison.** It ran to 83 requests:
  - 74 responses are committed (25,676,145 bytes).
  - The nine geocentric matrices (22,861,854 bytes) are retained outside Git.

  Horizons answers every Saturn-system body from `sat441l` and the Earth from DE441. Agreement with it therefore tests the pipeline, not the satellite ephemeris.
- **DE442s, the newest planetary ephemeris, for comparison only** (32,701,440 bytes, outside Git). NAIF's published MD5 matches. Its coverage equals de440s's. DE441 was not downloaded, as ruled.
- **IMCCE's phenomena of the Saturnian satellites, the independent authority** (1,834,910 bytes, outside Git).
  - These are W. Thuillot's predictions from TASS 1.6 (Vienne & Duriez 1995): eclipses, occultations, transits and shadow transits for 2006–2013, in TT, 32,099 events in all.
  - Only 24 rows are committed, in `imcce-quoted-rows.txt`, with attribution, terms and the source files' digests.

**Terms.**
- **NAIF.** Redistribution is allowed if the kernel is unmodified. A modified kernel must carry the modifier's name and institution and be renamed. No fees or licensing are required. This is the same as the released packs.
- **The PDS and NSSDCA pages** state no terms.
- **IMCCE.** Data "can be used as an example to illustrate documents if the source is clearly identified", and reproduction otherwise needs authorisation. The files are therefore never committed, bundled or republished.

## The pack, measured

From `contract/pack-measurements.md`:
- **The full excerpt.** Saturn and every candidate moon over 1900–2100 (the released interval with its 31-day margin) cut with the released `SpkExcerpt` gives 148,756,480 bytes in 20 segments. It is bit-identical to the source over 432,230 states.
- **The barycentre.** Saturn's barycentre from de440s is 427,008 bytes.
- **Record intervals.** They set the cost per body:
  - Mimas and Enceladus: 0.75 days;
  - Tethys, Dione and Saturn: 1.5 days;
  - Rhea: 2.25 days;
  - Titan and Hyperion: 3 days;
  - Iapetus: 9 days.

Bytes per body:

| interval | Saturn | Mimas | Enceladus | Tethys | Dione | Rhea | Titan | Hyperion | Iapetus |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900–2100 | 16,067,144 | 39,187,664 | 32,133,896 | 18,418,424 | 14,891,504 | 9,927,792 | 8,033,768 | 6,858,104 | 2,286,264 |
| 2000–2100 | 8,080,344 | 19,708,064 | 16,160,624 | 9,262,824 | 7,489,104 | 4,992,960 | 4,040,368 | 3,449,104 | 1,150,024 |
| 2020–2080 | 4,805,888 | 11,721,232 | 9,611,416 | 5,509,184 | 4,454,240 | 2,969,504 | 2,402,960 | 2,051,312 | 683,792 |

Mimas and Enceladus alone are 71 MB of the 1900–2100 moons. Sums of the table's per-body bytes for the candidate packs:

| candidate | bytes |
|---|---:|
| Saturn and all eight moons, 1900–2100 | 147,804,560 |
| Saturn and the five proposed moons, 1900–2100 | 69,624,896 |
| Saturn 1900–2100, the five proposed moons 2000–2100 | 43,002,424 |
| Saturn 1900–2100, the five proposed moons 2020–2080 | 32,086,824 |

The released packs today are the Sun/Earth/Moon, Mercury and Venus packs and the two Jovian packs. Layout A, as Jupiter's, adds a Saturn pack beside them and leaves every released pack byte-identical.

## Time, and the observer: inherited

These are unchanged from the released contracts:
- civil time is UT1 before 1972 and UTC from 1972 to the leap-second record's expiry;
- the eras are 1900–1961, 1962–1971, 1972–2027-06 and after (the expiry, 2027-06-28, is read from `Leap_Second.dat`);
- the observer stands at sea level on WGS84.

The cost to Saturn is the Jovian one:
- **Distance** differs by up to 17.2 km against Horizons in 1962–1971, where the two clocks differ.
- **Horizontal coordinates** after the record's expiry differ by up to 405.5″, from the unpredictable UT1. The J2000 place is untouched, so the released time note applies verbatim.

## Frames and meanings, proposed

**Saturn**, as Jupiter's quantities are ruled:
- **Astrometric J2000 direction:** topocentric, light-time corrected.
- **Apparent direction of date:** aberrated, and deflected by the Sun as Jupiter's ruling 8 adds.
- **Altitude and azimuth:** airless.
- **Distance:** to the light-time-corrected centre.
- **Equatorial diameter:** 2·asin(R_e/Δ).
- **Illuminated fraction:** (1 + cos i)/2.
- **North pole's position angle:** of date, from the PCK pole.

The polar diameter differs from Jupiter's. It is taken at the planetocentric ring opening B, 2·asin(b′/Δ) with b′ = √(R_p² cos² B + R_e² sin² B), because Saturn's flattening makes B matter.

**The ring opening is planetocentric, and Horizons' latitudes are not.** This is the study's main finding (`authority-comparison.md`).
- **Horizons' `ObsSub-LAT` is planetodetic.** Our planetocentric B converted to planetodetic on the PCK spheroid matches it within 0.003″ in every era. The planetocentric value differs from it by up to 18,196″, about 5°.
- **The widest opening.** Over 1900–2100 it is −26.995° planetocentric on 2003-04-07, where Horizons prints −32.049°.
- **The proposal.** B, the angle between the line of sight and the ring plane, is the planetocentric latitude over the PCK pole. Horizons' planetodetic value is never shown as the opening.

**The opening to the Sun, B′,** is taken where Saturn sees the Sun: the Sun light-time corrected and aberrated by Saturn's own barycentric motion.
- **The match.** That is Horizons' convention: `SunSub-LAT` agrees within 0.002″ once both are applied.
- **Without the aberration** the difference is 3.7″ and the Sun's ring-plane crossings fall 1.31–1.33 h early.
- **With it**, all 14 Sun crossings of 1900–2100 fall at Horizons' minute. All 30 Earth crossings do too.
- **The unlit face.** When B and B′ differ in sign, the observer sees the unlit face of the rings. This happens in 248 of the 10,488 weekly Oslo rows (`measurements.md`).

**The rings on the sky.**
- **Major axis.** It lies at the pole angle + 90°. Each edge's apparent semi-axes are asin(R/Δ) and asin(R/Δ)·|sin B|.
- **The near half.** The half in front of the globe lies away from the projected north pole when B > 0, and toward it when B < 0. A ring point in the observer's direction projects at −sin B cos B along the projected pole. At the card's instant (B = −7.43°) the near half crosses the globe north of its centre, as `controls-saturn-4-ring-geometry.png` draws.

**The moons**, as Jupiter's ruling 5 states:
- Horizons' apparent-of-date X/Y verbatim: X positive toward increasing right ascension, east to the reader, and Y north;
- the separation, the position angle and the separation in Saturn radii;
- front or behind from the line-of-sight depth, never from X/Y.

## The ring model, proposed

Drawn as a plane with no thickness, centred on Saturn's centre, normal to the PCK pole. Two annuli, with the boundaries quoted from the PDS table as they stand:
- **Inner annulus:** from the C Ring's inner boundary (74,491 km) to the B Ring's outer boundary.
- **Outer annulus:** from the Cassini Division's outer boundary (122,050 km) to the A Ring's outer boundary (136,770 km).

**The definitions.** The table gives an inner and an outer boundary for every feature (`rings/SOURCES.md`).
- **Gaps and divisions keep both edges.** None is reduced to one radius.
- **What a boundary is, is not stated.** The page gives no epoch, pole or reference plane.
- **The eccentric features.** For the Maxwell and Herschel ringlets and the F Ring a boundary is a nominal radius, not an instantaneous edge.

**Open for the ruling: the B Ring's outer edge.**
- **The disagreement.** The PDS table disagrees with itself here: 117,570 km in the B Ring row, but 117,500 km at Region B5, the Cassini Division and the Huygens Gap. B5's comment calls the edge "highly variable".
- **The proposal.** Take the B Ring row's own value.
- **Why it doesn't matter on the page.** The 70 km between them is 0.012″ at Saturn's nearest. At the closest field, 15′, the rings span 34.13–46.97 px (`measurements.md`), so the difference is about 0.02 px.

**Not drawn**:
- the D, E, F and G rings and the faint rings;
- the Encke and Keeler gaps, which are sub-pixel at every field;
- ring thickness;
- the globe's shadow on the rings and the rings' shadow on the globe. These are left to the cartography handoff (#577), which may propose them from this geometry.

**The pole.** The ring plane's pole is the PCK's IAU 2015 pole. It agrees with Horizons' `N.Pole-RA`/`N.Pole-DC` within 0.018″, which is Horizons' printed rounding. French et al. 2017's pole is not quoted, since the retained page does not state it.

## The visibility states, proposed

These are Jupiter's ruling 7 with Saturn's name. The states, in order of precedence:
- behind Saturn, in its shadow;
- behind Saturn;
- in front of Saturn;
- in Saturn's shadow;
- partly in Saturn's shadow;
- clear of Saturn.

**The figure.**
- **Contact is limb-to-limb**, including the moon's own radius.
- **Production uses the oblate figure:** the PCK pole and polar radius for the disc test, and the shadow cone on the same figure.
- **Horizons' equatorial sphere is used only as the comparison's definition.**

On that definition every state agrees with Horizons' codes, all 21,381 rows for each of the eight moons (`authority-comparison.md`). The oblate figure gives a different state in a few rows:

| moon | rows where the oblate figure differs |
|---|---:|
| Mimas | 159 |
| Enceladus | 93 |
| Tethys | 59 |
| Dione | 32 |
| Rhea | 24 |
| Titan, Hyperion, Iapetus | 4 each |

These are deliberate boundary differences, named, not unexplained mismatches.

**The oblate figure is IMCCE's** (`imcce-comparison.md`). Over the 21,572 eclipses, occultations and transits it lists, 95% of sat441's event times fall within 111.3 s of IMCCE's on the oblate figure, against 966.3 s on the sphere.

**No ring-obscuration state** is proposed for this release.
- **Measurable, not checked.** The geometry is measurable. In the weekly Oslo matrix, Tethys's sight line crosses the rings in 13 rows behind them and 13 before, Rhea's in 1 and 1, and Iapetus's in 3 before (`authority-comparison.md`).
- **No authority covers it.** Horizons' codes take no account of the rings, and IMCCE's events none either. The issue admits vocabulary "only where independently verifiable", so this stays geometry for the cartography handoff, not a reader's state.

## Accuracy, measured

These are against Horizons, which uses the same satellite family. All figures are the worst over every era, from `authority-comparison.md`.

**Saturn**
- **Astrometric:** 0.0035″.
- **Apparent:** 1.2081″ without deflection. Jupiter's ruled deflection is expected to close most of this, to be measured in #575.
- **Horizontal:** 2.7741″ to 2027-06; 405.5″ after it (UT1).
- **Distance:** 17.2 km (1962–1971).
- **Diameter:** 0.0000″.
- **Illuminated:** 0.0002%.
- **Pole angle:** 0.2708″.
- **Pole RA and Dec:** 0.018″ (rounding).
- **B and B′:** 0.003″ and 0.002″ as above.

**The five proposed moons**
- **Astrometric:** at most 0.0042″.
- **Apparent X/Y:** at most 0.3344″ (Iapetus X, after 2027-06). In 1972–2027-06: 0.0629″ (Iapetus Y).
- **Separation:** at most 0.3219″ (Iapetus, after).
- **Position angle:** at most 0.0541° (Dione, after 2027-06).
- **Diameter:** Horizons' `Ang-diam` is 2·asin(R_max/Δ) on the PCK's largest radius, matched to 0.0000″.
- **Titan's greatest elongations** of August 2026 (hourly at Oslo): within 0.017″.

**The planetary ephemeris, separated from sat441.** Weekly 1900–2100, DE442s against DE440:
- **Saturn's barycentre** moves by at most 1.497 km, and the Earth by at most 0.4946 km.
- **Saturn's direction** from the Earth moves by at most 0.0001953″, and its distance by at most 0.6579 km.

sat441's own Saturn-centre offset is about 311 km (311.8 km at most). It moves Saturn's direction by up to 0.05211″, so a barycentre-only Saturn would be measurably wrong, and the excerpt must keep 6 → 699. The newest planetary ephemeris changes nothing a reader could see.

**sat441 against an independent theory** (IMCCE, TASS 1.6, 2006–2013, oblate figure). Event-time differences, 95th percentile, by kind (`imcce-comparison.md`):

| moon | eclipse | occultation | transit |
|---|---:|---:|---:|
| Tethys | 21.5 s | 42.9 s | 51.0 s |
| Dione | 73.2 s | 41.1 s | 64.3 s |
| Rhea | 122.3 s | 51.2 s | 49.4 s |
| Titan | 325.3 s | 132.3 s | 197.2 s |
| Iapetus | 1355.8 s | 1361.3 s | 1089.0 s |
| Mimas | 115.9 s | 111.3 s | 125.6 s |
| Enceladus | 43.5 s | 33.5 s | 37.8 s |
| Hyperion | 2660.9 s | 3365.5 s | 2234.9 s |

- **Iapetus:** its events are few (3, 5 and 6) and slow.
- **Mimas:** sat441's events fall about a minute before IMCCE's throughout, with a median of −58.0 s for eclipses and −59.8 s for transits.
- **Hyperion:** its events differ by up to 15,873 km, measured across the limb. Some are grazing: IMCCE gives semi-durations of 46 and 65 s.

These differences are between two theories. They are recorded, and are not a pipeline target.

## The moon set, proposed

Separations, codes and brightness are from the weekly Oslo matrix and Horizons' 30-day geocentric magnitudes (airless, no extinction). The table is in `measurements.md`; its pack bytes are from above.

**Proposed: Tethys, Dione, Rhea, Titan and Iapetus.**

| moon | APmag | separation from Saturn | pack 1900–2100 | IMCCE 95% | verdict |
|---|---|---|---:|---|---|
| Titan | 8.02–8.95, the brightest | 1.8–213.0″ | 8.0 MB | 132–325 s | **in** |
| Rhea | 9.40–10.33 | up to 90.4″ | 9.9 MB | ≤ 122 s | **in** |
| Tethys | 9.90–10.83 | up to 50.4″ | 18.4 MB | ≤ 51 s | **in** |
| Dione | 10.10–11.03 | up to 64.6″ | 14.9 MB | ≤ 73 s | **in** |
| Iapetus | 10.80–11.74 | up to 625.5″ | 2.3 MB | ≤ 1361 s | **in** |
| Enceladus | 11.40–12.33 | at most 40.9″ | 32.1 MB | ≤ 44 s | owner's option |
| Mimas | 12.60–13.53 | at most 32.2″ | 39.2 MB | ≤ 126 s | owner's option |
| Hyperion | 13.93–14.86, the faintest | — | 6.9 MB | ≤ 3366 s | **out** |

**The five.**
- **Titan, Rhea, Tethys and Dione** sit clear of the rings for most of their orbits.
- **Iapetus** is the farthest and the cheapest. Horizons' magnitude model spans only 0.94 mag for it. Whether it models Iapetus's two-toned hemispheres is not stated in its notes, so the table should not print a magnitude for it without a further authority.
- **Size.** Together they are 43.0 MB with Saturn 1900–2100 and the moons 2000–2100.

**Enceladus and Mimas: the owner's option.** Each is accurate.
- **Enceladus** is never farther from Saturn than 40.9″, so it stays close to the rings, whose A Ring outer edge spans 34.13–46.97″ across. It would add 16.2 MB (2000–2100).
- **Mimas** is never farther than 32.2″, and its sat441 events fall about a minute before IMCCE's. It would add 19.7 MB (2000–2100).

**Hyperion: proposed out.**
- **Brightness.** It is the faintest of the eight.
- **Shape.** Its PCK figure is the most irregular of the eight (the three radii differ most), so a sphere-based disc state is least honest for it.
- **Agreement.** IMCCE's and sat441's positions for it disagree by thousands of km.

## Brightness

**Not shown in the first table.** Neither Jupiter's table nor the proposed Saturn table prints magnitudes.

**Horizons' Saturn magnitude** includes the rings only below 6.5° phase and below 27° effective ring inclination, and is the globe alone otherwise. A Saturn magnitude would therefore need its own model and authority, which is a later question. The magnitudes here only rank the moons.

## The named fixtures, proposed

- **The card's instant: 2026-10-05 00:00 UTC at Oslo.** This is the daily 2026 row nearest the Earth (8.434258 AU). Horizons' range rate changes sign between 4 and 5 October. Here B = −7.4277° (south face), the pole angle is 3.2326°, and Tethys is behind Saturn in its shadow. That gives one non-clear state at the named instant.
- **The 2025 ring-plane crossing.** The Earth crossed on 2025-03-23 (17:49 geocentric, 17:51–17:54 at Oslo by sampling) and the Sun on 2025-05-06 (15:34–15:35). These are read from Horizons' daily window and weekly matrices. ***Limitation, plainly stated:*** these crossings, and the oppositions (Horizons' range rate through zero), **are validated only against Horizons**. Horizons answers from the same `sat441l` family and DE441. No independent published authority confirms them at this checkpoint, as the owner accepted.
- **Titan's greatest elongations, August 2026.** Hourly at Oslo: 2026-08-01 18:00 and 2026-08-09 20:00.
- **IMCCE's quoted events.** The worst of each moon and kind, 2006–2013, for the event comparison.
- **The boundaries and reference instants** in `NAMED-CASES.txt`. No event instant is typed there.

## The reader's table, proposed

The mock-ups are `controls-saturn-*.png`, in both languages, light and dark.

**The Saturn card**, 11 lines:
- right ascension and declination (J2000);
- altitude and azimuth;
- distance;
- apparent diameter, equator and poles;
- illuminated fraction;
- the north pole's position angle;
- the rings' opening to the observer, with its face (north, south, or edge-on);
- the rings' opening to the Sun, with "the unlit face is seen" when the faces differ;
- the rings' apparent size.

**The moons**, the proposed five outward from Saturn, with Jupiter's columns:
- moon and side;
- east–west and north–south;
- separation and Saturn radii;
- state.

**Ranges** are timestamp-grouped and sampled, with Jupiter's cap. In the Controller, Saturn enters last, after Jupiter, collapsed, with no Show on chart choice until cartography exists.

**The words** are tabled in `measurements.md`.
- Where Jupiter's released keys fit (the card's position lines, the columns and the e/w/n/s letters), they are reused verbatim.
- One released key cannot be: the Norwegian `jupitertable.column.separation` reads "Avstand fra Jupiter", so Saturn's proposes "Avstand fra Saturn".

## The cartography handoff

From `measurements.md`:
- **Size on the sky.** Over 1900–2100 the A Ring's outer edge spans 34.13–46.97″ across, and the globe 15.04–20.70″.
- **On the 900 × 700 page,** the rings first reach a pixel at 8° (1.06–1.47 px). At 1° they span 8.53–11.74 px. At the closer fields they span 17.07–23.48 px (30′) and 34.13–46.97 px (15′).

The globe stays under Rule M's 6 pixels on every ladder field (5.17 px at most at 1°), so Saturn is a symbol there. The rings' shape is legible only at the closer fields. The near/far rule and the ring ellipses above are what #577 hands to the drawing.

## Fixtures, proposed

**Committed:**
- the 74 Horizons responses (25,676,145 bytes): named instants for five observers and nine bodies; the daily 2026 year, the weekly 1900–2100 matrices at Oslo, the 2024–2025 ring-plane window, Titan hourly, and the 30-day magnitudes;
- the two ring pages, their extracted table and their record (80 KB);
- IMCCE's 24 quoted rows.

`SaturnFixturesTest` re-hashes each file. The nine geocentric matrices stay outside the repository; their URLs and digests are in `horizons/README.md`.

## Architecture, and the CI route

**Reused as built:**
- `Observer`, `TimeScales`, `TimeRange` and the refusals;
- the epoch-aware `SpkKernel` and `SpkExcerpt`;
- the Jovian service's light-time, aberration, deflection and two-relation state machinery;
- the Solar System table seam and `SolarSystemSection`.

**What is Saturn-specific:**
- the ring geometry (B, B′, the face, the ellipses, the near half);
- the oblate figure's larger flattening;
- the state vocabulary's name;
- the third kernel.

**Predicted routes:**

| pull request | touches | route |
|---|---|---|
| #575, the service and the ruled data | `solar/*`, a Saturn pack resource, scripts, fixtures, tests | **wide**: a data-pack change is never narrow by intention |
| #576, the tables | `ui/solar/*`, `SolarSystemSection`, `JUranometriaMain`, keys, photographers | **wide** |
| #577, the cartography handoff | study and decision record | **wide**: the registries |
| this study | `Makefile`, the contract's registries, the generator, fixtures | **wide** |

## For the rulings

1. **Authority.**
   - sat441 for Saturn's centre and the ruled moons, with DE440's Saturn barycentre from the released source.
   - Horizons as the in-family comparison.
   - IMCCE (TASS 1.6) as the independent event catalogue.
   - The PDS Rings Node table as the ring-radius authority, with NSSDCA as a cross-check only.
   - DE442s as measurement only: it changes nothing visible, by up to 0.0002″.
2. **Terms.**
   - A modified, renamed sat441 excerpt under NAIF's rules, with the released notice pattern.
   - IMCCE: rows quoted only; never bundled, never republished.
   - PDS: values quoted with the table's footnotes.
3. **Pack layout and interval.** Layout A: a Saturn pack beside the untouched released packs. Recommended: **Saturn 1900–2100 and the five moons 2000–2100**, 43,002,424 bytes. The moons refuse outside their interval and name it. Alternatives:
   - 69,624,896 bytes with the five moons over 1900–2100;
   - 32,086,824 bytes with the moons over 2020–2080.
4. **The moon set.** **Tethys, Dione, Rhea, Titan and Iapetus.** Enceladus (+16.2 MB) and Mimas (+19.7 MB) are the owner's option. Hyperion is out. The reasons are tabled above.
5. **Frames and meanings** as frozen above:
   - B planetocentric, never Horizons' planetodetic latitude;
   - B′ with the Sun light-timed and aberrated by Saturn's motion;
   - the unlit-face flag;
   - X/Y and depth as Jupiter's ruling 5.
6. **The ring model.**
   - Two annuli from the PDS boundaries, planar, normal to the PCK pole.
   - The B Ring's outer edge at the B Ring row's 117,570 km.
   - The near-half rule.
   - No minor rings, gaps, thickness or shadows in this release.
7. **The visibility states.**
   - Jupiter's six, with Saturn's name.
   - The oblate figure in production, as IMCCE uses it.
   - Horizons' sphere only for the comparison.
   - **No ring-obscuration state**, as it is not independently verifiable.
8. **Vocabulary** as proposed in both languages, including the new Norwegian separation heading.
9. **Fixtures** as committed. The geocentric matrices stay retained outside Git.
10. **Accuracy targets.** These are the measured worsts, rounded up, against Horizons on the sphere's definition:

    | quantity | target |
    |---|---|
    | Saturn astrometric | 0.01″ |
    | Saturn distance | 20 km |
    | B and B′ | 0.01″ |
    | pole angle | 0.5″ |
    | moon X/Y and separation | 0.5″ |
    | moon position angle | 0.1° |
    | states | exact, with an empty allowlist |

    Saturn's apparent place is to be measured with deflection in #575 and ruled there. IMCCE agreement is recorded, not gated.
11. **Named fixtures.**
    - The 2026-10-05 card.
    - The 2025 crossings and the oppositions, **validated only against Horizons**, as stated.
    - Titan's August 2026 elongations.
    - IMCCE's quoted events.
12. **Implementation split.**
    - #575: the service and the ruled data (wide).
    - #576: the tables (wide).
    - #577: the cartography handoff.
    - Then Sprint 53's #578–#581.
