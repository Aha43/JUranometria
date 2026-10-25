# Knowing where Mars, Phobos and Deimos are

**Issue #601, the first issue of the Mars road #600.** This is the numerical contract for Mars and its two moons. It is proposed from measurement, before any production data or code exists. **Not yet ruled.** The owner's ruling, when given, governs wherever it differs from this proposal. The record inherits the Sun's, the Moon's, Jupiter's and Saturn's contracts (`sun-computation.md`, `moon-computation.md`, `jovian-system.md`, `saturn-system.md`) wherever it does not say otherwise.

The study is `docs/studies/mars-system/`:
- `inventory.md` is the read-only inventory made before any download.
- `make mars-pack-study` cuts and proves every candidate kernel. Its report is kept as `contract/pack-measurements.md`.
- `scripts/mars-authority-comparison.py` compares the pinned kernels with Horizons and DE442s (`authority-comparison.md`).
- `scripts/mars-horizons-measurements.py` reads the observer's quantities from the committed Horizons responses (`measurements.md`).

Every number quoted here comes from one of those reports, or from a pinned file named beside it. None is typed from memory.

## The question

Before the road draws anything of Mars, the atlas must answer a reader's questions at a chosen instant, offline, permanently, to a measured accuracy, and in both languages:
- where Mars is, and how large and how fully lit its disc appears;
- how the changing distance alters that disc through the opposition cycle;
- where Phobos and Deimos lie relative to Mars;
- whether each moon is clear, in front, behind or in Mars's shadow.

This record proposes:
- the authority and its terms;
- the pack and the interval;
- the time model;
- the frames and quantities;
- the moon set;
- the states and the vocabulary;
- the fixtures and the targets;
- the production split.

It builds nothing.

## The authority, inventoried from the official sources (2026-10-25)

Each source was approved by the owner before retrieval ("approve 1–4", 2026-10-25). Each is kept whole, with its URL, retrieval time, byte count and SHA-256, in `SOURCES.md` and `horizons/README.md`.

- **NAIF's `mar099s.bsp`** (67,594,240 bytes, kept outside Git; MD5 equal to NAIF's checksum record).
  - It is the short member of NAIF's only Mars family, `mar099`, fitted by JPL in May 2025 on DE440.
  - It carries Mars (4 → 499), Phobos (4 → 401) and Deimos (4 → 402) about the Mars barycentre. It also carries DE440's Mars barycentre, Sun, Earth–Moon barycentre and Earth.
  - Every segment is SPK Type 2 in J2000 and covers **1995-01-01 to 2050-01-01 TDB** only.
  - The long member, `mar099.bsp` (1,227,574,272 bytes), covers 1600–2600. It was not downloaded.
  - The kernel's comments state a Chebyshev interpolation error of 2 m. They state no positional uncertainty for either moon.
- **The released DE440, `de440s.bsp`**, re-fetched under its existing pin. It holds the Mars barycentre (0 → 4) over 1849–2150, but no Mars centre.
- **IAU 2015 constants, `pck00011.tpc`**, re-fetched under its existing pin.
  - Mars's radii are 3396.19, 3396.19 and 3376.20 km. The PCK says the polar value is the mean of separate north and south polar radii.
  - The pole is RA 317.269202 − 0.10927547 T, Dec 54.432516 − 0.05827105 T. Unlike Saturn's, it carries **five periodic terms in RA and five in Dec**, over angles shared with Phobos and Deimos.
  - Phobos's radii are 13.0, 11.4 and 9.1 km; Deimos's are 7.8, 6.0 and 5.1 km.
- **JPL Horizons, the in-family comparison**: 39 requests.
  - 36 responses are committed (6,123,846 bytes).
  - The three geocentric matrices (4,660,673 bytes) are retained outside Git.
  - Horizons answers Mars, Phobos and Deimos from `mar099` and the Earth from DE441. Agreement therefore tests the pipeline, not the satellite ephemeris.
- **DE442s**, for comparison only. It is already held from the Venus study (pin `54d97562…`).
- **Espenak's almanacs**, the published event times. 2012, 2026 and 2100 were held from the Venus study, and 2003, 2018, 2027 and 2035 were added. They are kept outside Git, and only quoted rows are committed.

**Terms.**
- **NAIF.** Rechecked on 2026-10-25. Redistribution is allowed if the kernel is unmodified. A modified kernel must carry the modifier's name and institution and be renamed. No fees or licensing are required. These are the released packs' terms.
- **Horizons.** The public JPL SSD service, as for every Horizons fixture already committed.
- **Espenak's almanacs.** Rows quoted with attribution; the pages are never committed.

## The main finding: the planet needs no satellite kernel

From `contract/pack-measurements.md`:
- **The centre offset is negligible.** Mars's centre never lies more than **0.210 m** from the Mars barycentre, evaluated hourly over all 482,137 epochs of `mar099s`'s span.
- **The same seen from Earth.** The geocentric direction of the centre differs from the barycentre's by at most **7.5 × 10⁻⁷″** (`authority-comparison.md`).
- **One barycentre, not two.** `mar099s`'s own Mars barycentre is **bit-identical** to the released `de440s.bsp`'s, over 11,818 states.

From `authority-comparison.md`:
- **The barycentre model against Horizons.** DE440's Mars barycentre alone, with no satellite kernel, agrees with Horizons' Mars centre over 1900–2100. The astrometric direction differs by at most 0.0010″ before 1962, 0.0194″ in 1962–1971, 0.0001″ in 1972 to 2027-06, and 0.0208″ after.
- **No benefit from the centre segment.** Inside `mar099s`'s span the centre model's astrometric differences are the same to the printed digit: 0.0001″, and 0.0012″ after 2027-06.

So Mars can be what Venus and Mercury are: a DE440 excerpt over the released interval. The 1995–2050 limit of `mar099s` then constrains only the moons.

**DE442s moves nothing that matters.** Weekly over 1900–2100:
- the Mars barycentre moves at most 4.6 km;
- the geocentric direction at most 0.0141″;
- the geocentric distance at most 2.9 km.

As for Venus and Mercury, DE440 stays.

## The pack, measured

Each kernel is cut with the released `SpkExcerpt` and proved against its source on a 1.7-day grid (`contract/pack-measurements.md`).

| kernel | bodies | bytes | gzip -9 |
|---|---|---:|---:|
| Mars, 1900–2100 (from `de440s`) | 0 → 4 | 648,192 | 610,833 |
| Phobos and Deimos, 2000–2050 | 4 → 401, 402 | 43,910,144 | 37,752,323 |
| Phobos, 2000–2050 | 4 → 401 | 29,275,136 | 26,111,117 |
| Deimos, 2000–2050 | 4 → 402 | 14,640,128 | 11,641,595 |
| Phobos and Deimos, 2020–2050 | 4 → 401, 402 | 26,378,240 | 22,674,537 |
| Phobos and Deimos, 2030–2050 | 4 → 401, 402 | 17,610,752 | 15,137,264 |
| *for comparison:* Mars centre with both moons, 2000–2050 | 4 → 499, 401, 402 | 59,130,880 | 51,829,286 |

**Proofs.**
- **On the grid,** every excerpt is bit-identical to its source.
- **At the last instant.** Where the moons' interval ends at `mar099s`'s own final instant, 2050-01-01 TDB, source and excerpt differ by at most **6.81 × 10⁻¹² km** at that single instant. The two select neighbouring records at the boundary. The report prints that instant separately from the grid, and never folds it into the grid's proof.
- **The 2050 margin.** The released 31-day margin cannot be kept after 2050, because the kernel ends there.

**The moons to 2100 would need `mar099.bsp`.**
- **Estimate.** At `mar099s`'s measured record rates (Phobos 0.25-day, Deimos 0.5-day records), both moons over 2000–2100 would be about **88.7 MB**. This is an uncut estimate, labelled as such under Saturn's rule; only a cut figure is a record.
- **The cost of measuring.** Cutting it needs the 1.2 GB download. That download is asked for only if the owner rules for it (proposal M-7c).

**The layout.**
- **The planet.** Mars joins the released `planets` pack as a third DE440 kernel, beside Venus and Mercury. This is Layout A: one kernel per planet, leaving every released kernel byte-identical.
- **The moons.** If any ships, the moons form their own pack, as the Jovian and Saturn moons do.

## Time, and the observer: inherited

These are unchanged from the released contracts:
- civil time is UT1 before 1972, and UTC from 1972 to the leap-second record's expiry (2027-06-28, read from `Leap_Second.dat`);
- the observer stands at sea level on WGS84.

The measured cost to Mars matches Jupiter's and Saturn's:
- **Distance** differs from Horizons by up to 9.93 km in 1962–1971, where the two clocks differ.
- **Horizontal coordinates** after the expiry differ by up to 409.16″, from the unpredictable UT1. The J2000 place is untouched.

## Frames and meanings, proposed

**Mars**, as Jupiter's and Saturn's quantities are ruled:
- **Astrometric J2000 direction:** topocentric, light-time corrected.
- **Apparent direction of date:** aberrated, and deflected by the Sun (Jupiter's ruling 8).
- **Altitude and azimuth:** airless.
- **Distance:** to the light-time-corrected barycentre, which is the centre within 0.210 m.
- **Equatorial diameter:** 2·asin(R_e/Δ). It agrees with Horizons to the printed 0.0001″ in every era.
- **Polar diameter:** taken at the planetocentric sub-observer latitude φ, as 2·asin(b′/Δ) with b′ = √(R_p² cos² φ + R_e² sin² φ), as for Saturn.
- **Phase angle and illuminated fraction:** with the Sun where Mars sees it, light-time corrected and aberrated by Mars's own motion. This is Saturn's convention.
  - The illuminated fraction (1 + cos i)/2 then agrees with Horizons' `Illu%` to the printed 0.0001 % in every era.
  - The phase angle agrees with Horizons' `phi` (quantity 43) within 0.19″, its printed rounding.
- **Elongation, and the lit side** (χ of date, with the nearest of sixteen points or *not well-defined*): as Venus's.
- **North pole's position angle:** of date, from the PCK pole **with its periodic terms**.

**The pole needs its periodic terms.**
- **Without them**, the pole is 5,578.6″ off in declination and 1,486.4″ in right ascension, about 1.5°, and the position angle 5,930.8″ off.
- **With them**, the pole agrees with Horizons within 0.018″, and the position angle within 0.28″.
- **The proposal.** Production models the IAU 2015 pole in full, and a builder check refuses a PCK whose terms it does not model, as Saturn's builder refuses unexpected terms.

**The tilt toward the observer is planetocentric.** Horizons' `ObsSub-LAT` is planetodetic.
- **Converted, it matches.** Our planetocentric value converted on the PCK spheroid agrees with Horizons within 0.013″.
- **Unconverted, it does not.** The planetocentric value itself differs by up to 969.6″.
- **The proposal.** As Saturn's ruling 3 does for the rings, the reader is shown the planetocentric tilt, and Horizons' planetodetic figure is never shown as it.

**Horizons' S-T-O is not the phase angle we compute.** For Mars, `S-T-O` stands about 21″ from `phi` in every era, while our phase angle sits within 0.19″ of `phi`. The illuminated fraction agrees exactly either way. The proposal holds the phase angle to `phi`, and records that `S-T-O` reads differently for Mars.

**The moons** follow Jupiter's ruling 5 and Saturn's: X positive toward increasing right ascension, east to the reader, and Y north, apparent of date relative to Mars.

## What Mars looks like, measured

From `measurements.md`, weekly 1900–2100 from Oslo:

| quantity | least | greatest |
|---|---:|---:|
| equatorial diameter | 3.50″ (1987-08-24) | 25.10″ (2003-08-25) |
| distance | 0.3731 AU (2003-08-25) | 2.6762 AU (1987-08-24) |
| phase angle | 0.15° (2038-11-01) | 47.38° (2005-07-11) |
| illuminated | 83.86 % (2005-07-11) | 99.9998 % |
| tilt (planetodetic, Horizons) | −26.52° | +26.43° |
| magnitude, geocentric, 30-day | −2.881 (1924-08-23) | +1.869 (2100-07-31) |

Mars is never less than 83.86 % lit. Its phase words therefore reduce to two of the Moon's and Venus's bands: *gibbous* and *near full*. The two geometric states, in front of or behind the Sun, follow Venus's definition.

**The named oppositions and conjunctions.** Each window runs hourly from Oslo, three days either side of the almanac's hour. The table gives the row at that hour:

| almanac event | distance (AU) | diameter | phase angle |
|---|---:|---:|---:|
| opposition 2003-08-28 16h | 0.372850 | 25.118″ | 4.8428° |
| opposition 2012-03-03 19h | 0.674074 | 13.894″ | 2.4848° |
| opposition 2018-07-27 06h | 0.386156 | 24.253″ | 4.6952° |
| opposition 2027-02-19 15h | 0.677939 | 13.814″ | 2.6455° |
| opposition 2035-09-15 17h | 0.381637 | 24.540″ | 4.1032° |
| conjunction 2026-01-09 10h | 2.403476 | 3.897″ | 0.6504° |
| conjunction 2100-10-11 07h | 2.625307 | 3.567″ | 0.3763° |

**Opposition and closest approach are different instants for Mars.**
- **Inside the window.** The window's closest row falls before or after the opposition: 2003-08-27 01:00 and 2012-03-05 22:00 for those oppositions, and 2027-02-20 00:00 for 2027's.
- **Outside the window.** For 2018 and 2035 the closest row is the window's edge, so the true closest approach lies more than three days from opposition.

The service study will match each opposition to the almanac's hour. The reader must be told which of the two events a value belongs to.

## The moons, measured

| | Phobos | Deimos |
|---|---:|---:|
| distance from Mars's centre | 9,232.0–9,517.9 km | 23,448.0–23,466.9 km |
| separation from Mars's centre, weekly 1995–2049 | 0.26″ to 34.22″ | 0.47″ to 84.79″ |
| greatest separation, 2003 opposition | 35.02″ | 86.77″ |
| greatest separation, 2027 (aphelic) opposition | 19.35″ | 47.71″ |
| angular diameter | 0.013″ to 0.096″ | 0.008″ to 0.058″ |
| magnitude, geocentric, 30-day | 10.40 to 15.04 | 11.49 to 16.13 |

**Observational value.**
- **Large-aperture targets near opposition only.** Both moons are faint points close to a planet of magnitude −2.9 at best. At favourable oppositions Deimos stands up to 87″ from Mars and Phobos up to 35″, inside Mars's glare.
- **Never a disc.** Neither moon is ever larger than 0.1″. Both are always points on the chart.
- **Eclipses and transits are real configurations.** At the 2018 opposition Phobos was totally eclipsed while occulted (`U`) in 24 of 289 ten-minute rows, and transiting (`t`) in 29.

**Accuracy against Horizons** (inside `mar099s`, 1995–2050):
- X, Y and separation within 0.0024″ for Phobos and 0.0095″ for Deimos.
- Position angle within 0.0060° when more than 20″ from Mars. Closer than 20″ the angle is ill-conditioned: there it differs by up to 0.056° for Phobos and 0.054° for Deimos.
- The apparent place within 0.0536″ beyond 1° from the Sun. Within 1° it reaches 1.06″, at the November 2023 conjunction, as Jupiter's and Saturn's grow there.

## The visibility states, proposed

These are Jupiter's six states, with Mars's name:
- behind Mars, in its shadow;
- behind Mars;
- in front of Mars;
- in Mars's shadow;
- partly in Mars's shadow;
- clear of Mars.

**The figure.**
- **Contact is limb-to-limb**, including the moon's largest radius.
- **Production uses the oblate figure**, with the PCK pole and polar radius, as Saturn's does.
- **Horizons' equatorial sphere is the comparison's definition only.**

**Agreement with Horizons, on Horizons' definition.**
- **Deimos** agrees in all 6,909 rows.
- **Phobos** agrees in 6,905 of 6,909 rows. The four exceptions are two instants, each in the Oslo and the geocentric matrix. In both, ours reads *partly in shadow* (`p`): on 2028-12-10 Horizons reads clear (`*`), and on 2034-01-08 it reads totally eclipsed (`u`).
- **What the exceptions are.** Each is a boundary of Mars's umbra crossed within Phobos's own 13 km radius. Horizons' sphere has no atmosphere, and neither does ours.
- **The proposal.** Like Saturn's oblate-figure rows, these boundary rows are named as such, not counted as failures.
- **No shadow transits.** A moon's shadow on Mars is not proposed: the road excludes shadow extensions.

## Accuracy targets, proposed

These are the measured worst values of `authority-comparison.md`, rounded up, in the eras the inherited time model defines. Each is a target the service study must meet against the same fixtures. Unlike Jupiter's and Saturn's, Mars's measured differences stay small after 2027-06: the worst are 0.0208″ astrometric, 0.1943″ apparent beyond 1° from the Sun, and 9.10 km in distance. The proposal does not widen them to Jupiter's figures.

| quantity | 1900–1961 | 1962–1971 | 1972 to 2027-06 | after 2027-06 |
|---|---:|---:|---:|---:|
| Mars astrometric | 0.01″ | 0.05″ | 0.01″ | 0.05″ |
| Mars apparent, beyond 1° from the Sun | 0.3″ | 0.1″ | 0.1″ | 0.5″ |
| Mars distance | 1 km | 20 km | 1 km | 20 km |
| equatorial diameter | 0.001″ | 0.001″ | 0.001″ | 0.01″ |
| illuminated fraction | 0.001 % | 0.001 % | 0.001 % | 0.01 % |
| phase angle, against `phi` | 0.5″ | 0.5″ | 0.5″ | 10″ |
| pole position angle | 0.5″ | 0.5″ | 0.5″ | 0.5″ |
| moons X, Y, separation, beyond 1° from the Sun | – | – | 0.05″ | 0.5″ |
| moons position angle, beyond 20″ of Mars | – | – | 0.01° | 0.01° |
| moons position angle, within 20″ | – | – | printed, not asserted | printed, not asserted |
| states on Horizons' sphere | – | – | exact, or a named boundary row | printed, not asserted |
| horizontal | 20″, inherited | 20″ | 20″ | printed, not asserted |

## The reader's table, proposed

**The Mars card**, in Venus's and Saturn's family:
- right ascension and declination (J2000);
- altitude and azimuth;
- distance;
- apparent diameter, equator and poles;
- elongation with E or W;
- phase angle and illuminated fraction, with the phase word and the lit side;
- the north pole's position angle;
- the tilt toward the observer, planetocentric, naming the pole shown.

**The moons**, if ruled in, use Jupiter's columns:
- moon and side;
- east–west and north–south;
- separation, and separation in Mars radii;
- state.

**Ranges** are timestamp-grouped and sampled, with the released cap.

**Ordering.** In menus and the Controller, Mars sits between the Moon and Jupiter, outward from the Sun (#600). It has no Show on chart choice until cartography exists.

**The words.** The released Jupiter and Saturn keys are reused where they fit. Where a key names its planet, a Mars key is proposed beside it, as Saturn's separation column was.

**Opposition versus closest approach.** The card shows the instant's values only. Neither event is computed in Part I: the road excludes event search.

## Fixtures, proposed

- **Committed, bounded:**
  - the 36 Horizons responses (6,123,846 bytes);
  - the almanac rows as quoted, with digests, in a `mars-events.txt` like Venus's.
- **Never committed:**
  - the kernels;
  - the three retained geocentric matrices;
  - the almanac pages.

## The production split, proposed

- **#602:**
  - the Mars kernel in the `planets` pack;
  - if ruled in, the moons' pack;
  - the service, with the released packs and every released answer proved byte-identical;
  - the accuracy report, against the targets above.
- **#603:** the Controller and dialog tables, as a packaged owner checkpoint.
- **#604:** the numerical handover to cartography.

No production chart ink is in Part I.

## For the rulings

- **M-1, the authority for the planet.** DE440's Mars barycentre (0 → 4) from the released `de440s.bsp`, over 1900–2100, as a third kernel in the `planets` pack: 648,192 bytes, 610,833 gzipped. No satellite kernel for the planet: the centre offset is at most 0.210 m. DE440 stays, as for Venus and Mercury.
- **M-2, the figure.** The PCK radii, equatorial 3396.19 km and mean polar 3376.20 km. The polar diameter is taken at the planetocentric tilt.
- **M-3, the pole.** The full IAU 2015 model, with its periodic terms; the builder refuses terms it does not model. The tilt is shown planetocentric, never Horizons' planetodetic value.
- **M-4, the phase.** Saturn's convention, with the Sun light-time corrected and aberrated where Mars sees it, held to Horizons' `phi`. Phase words as the Moon's and Venus's bands. The lit side as Venus's.
- **M-5, the apparent place.** Aberration and the Sun's deflection, as Jupiter's ruling 8, measured within 1° of the Sun, never asserted there.
- **M-6, time and observer.** Inherited unchanged, with its measured costs.
- **M-7, the moons.** Three choices:
  - **M-7a (recommended): both moons, 2000–2050, in their own pack: 43,910,144 bytes, 37,752,323 gzipped.** The table refuses a moon instant after 2050-01-01 by name, as Jupiter's and Saturn's moons refuse before 2000.
  - **M-7b: Deimos only**, 2000–2050: 14,640,128 bytes. It is the easier, farther and cheaper moon, but Phobos holds the eclipses and transits.
  - **M-7c: both moons to 2100**, which first needs approval to download `mar099.bsp` (1,227,574,272 bytes) for measurement. The uncut estimate is about 88.7 MB.
  - **Or none**, which leaves the planet alone.
- **M-8, the states.** Jupiter's six, on the oblate figure, with Horizons' sphere as the comparison. The two Phobos umbra-boundary instants are named rows. No shadow transits.
- **M-9, the vocabulary.** The card and the moon columns above, in both languages, reusing released keys where they fit.
- **M-10, the targets.** The table above.
- **M-11, the fixtures.** Committed and retained as above.
- **M-12, the published-event limitation.** State plainly that oppositions and conjunctions are checked against Espenak's almanacs, to the hour, and moon states against Horizons only.
