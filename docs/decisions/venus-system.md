# Knowing where Venus is and how it is lit

**Sprint 47, issue #500.** The numerical contract for Venus, proposed
from measurement before any production data or code exists. **Ruled
by the owner (2026-10-11)** - see [The ruling](#the-ruling) at the
end, which governs where it differs from the proposal; the pack was
returned once for a third layout, measured in *The pack* below. No
production Venus service, table, selection or rendering exists. It
inherits the Sun's, the Moon's and Jupiter's contracts
(`sun-computation.md`, `moon-computation.md`, `jovian-system.md`)
wherever it does not say otherwise, and says so where it inherits a
ruling rather than proposing afresh. The study is `docs/studies/venus/`:
`make venus-system-study` for the table proposal, diagrams and the
cartography handoff (`measurements.md`), `make venus-pack-study` for
the excerpt (`pack-measurements.md`),
`scripts/venus-authority-comparison.py` for the error matrix
(`authority-comparison.md`); the numbers quoted here are those
reports'.

> Venus must answer where it is in the chart's J2000 frame and in the
> observer's sky, how far away and how large it is, how much of it is
> lit, what phase it has and which way the bright side faces - and why
> its size and its lit fraction change in opposite-looking ways through
> an apparition. (#499)

## The question

Before the road to 5.2.0 draws Venus, the atlas must answer those
questions offline, permanently, to a measured accuracy, in both
languages. This record proposes the authority and its terms, the pack
and its interval, the time and frame model with its measured costs,
the meaning of every observable, the phase vocabulary, the accuracy
targets, the named fixtures, the reader's table and the cartography
handoff. It builds nothing.

## The authority, inventoried from the official sources (2026-10-11)

| what | source | retrieved | digest (SHA-256) |
|---|---|---|---|
| Venus, the Sun, the Earth: **DE440**, the released pack's own source; Venus is segments 0 → 2 and 2 → 299 | `https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp`, pinned by `scripts/download-solar-system-sources.sh` | 2026-09-28 | `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` |
| Venus's and the Sun's radii: IAU WGCCRE as NAIF's `pck00011.tpc` states them (`BODY299_RADII` 6051.8, `BODY10_RADII` 695700) | `https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc`, pinned by the Jovian pack | 2026-10-07 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |
| the comparison service: **JPL Horizons** (DE441; Venus target radii 6051.8 km) | `https://ssd.jpl.nasa.gov/api/horizons.api`, 51 responses kept whole (`docs/studies/venus/horizons-kept.md`) | 2026-10-11 | per response |
| named-case timing, independently published: **Espenak, Sky Event Almanacs** (AstroPixels), 2012, 2026 and 2100 GMT | `https://astropixels.com/almanac/almanac{01,21,81}/almanac{2012,2026,2100}gmt.html`, nine rows quoted in `venus-events.txt` | 2026-10-11 | `251a85e4…b307`, `4236c51d…ce17`, `1178bd6d…ecd6` |
| the newest planetary ephemeris, **DE442** (2025), for comparison only (owner's approval B) | `https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de442s.bsp` (32 701 440 bytes, MD5 equal to NAIF's `aa_checksums.txt`), kept outside Git | 2026-10-11 | `54d97562a5b094d298b1b8eafa5a2e17e3e010ce85e1a366d07f003ad159323c` |

**DE440 stays.** Daily over 1900–2100, DE442s moves Venus's
geocentric direction by at most **0.0061″** and its distance by
**0.023 km** from DE440; the barycentric Venus, Earth and Sun each by
about half a kilometre. NAIF's DE442 notes give no Venus improvement.
No measurement shows a Venus benefit or a deficiency DE442 corrects,
so mixing authorities buys nothing; DE442s remains a study download,
never bundled, never a build dependency.

**Terms.** NAIF's rules, re-read on 2026-10-11, are those the released
notices quote: redistribution of unmodified kernels permitted; a
modified kernel renamed, its attribution replaced with the modifier's;
SPICE "Technology and Software Publicly Available", no fees or
licensing. Venus needs **no new source and no new terms**: both
segments come from the released pack's pinned `de440s.bsp`, the
radius from the pinned PCK. The Espenak rows are quoted with
attribution, URL, retrieval date and digest, never bundled and never a
runtime or build dependency. The drafted notice, provenance and
manifest are `docs/studies/venus/notice-draft.md`.

## The pack, measured

Cut with the released excerpt writer and proved against the source
(`pack-measurements.md`):

- **Venus is 1.18 MB for the whole released interval.** Segment 0 → 2
  over 1900–2100 with the released 31-day margins is 1 180 672 bytes
  (1 109 956 gzipped); 2 → 299 is identically zero - Venus has no
  moon - proved at 43 222 epochs, and costs nothing beside it. Every
  state on a 1.7-day grid is bit-identical to `de440s.bsp`.
- **Layout A (proposed): a `venus` pack beside the released ones.**
  One kernel, `juranometria-de440-venus-1900-2100.bsp`, 1 180 672
  bytes (1 110 054 gzipped). The Solar System pack's kernel keeps its
  digest `63fbf570…`; the Jovian pack's five files keep theirs (listed
  in the report). Sun, Moon, Earth and Jovian answers are then
  invariant by byte identity: their Earth and Sun come from the
  released kernel, which is not rewritten.
- **Layout B: the Solar System kernel regenerated (v3).** Six
  segments, 16 024 576 bytes - 1 114 112 more than today. The four
  released segments give the released states bit for bit (172 892
  states), but the released file's name, bytes and digest change, and
  the Sun and the Moon would be re-released to add a planet.
- **The interval: 1900–2100**, the Sun's, the Moon's and Jupiter's,
  with the same margins and refusals. Unlike the Galilean moons there
  is nothing to save by shortening it.

The study first proposed **A** - the same reasoning the owner ruled
for Jupiter on #472, for 66 KB more than B. The owner returned it once
(ruling 2): the road now includes every planet, so a Venus-named pack
should not be frozen before a third layout is measured.

**Layout C, measured over the whole road** (`pack-measurements.md`,
section *Layout C*): one logical `planets` pack, gaining Mercury,
Mars, Saturn, Uranus, Neptune and Pluto across later 5.x releases in
that order, Jupiter staying the Jovian pack's. Every kernel carries the
released builder's comment area, so each size is a shipping size:

| planet | de440s segments | kernel | gzip -9 |
|---|---|---:|---:|
| Venus | 0 → 2, 2 → 299 (zero) | 1 241 088 | 1 129 895 |
| Mercury | 0 → 1, 1 → 199 (zero) | 3 298 304 | 3 112 272 |
| Mars | 0 → 4 | 708 608 | 630 770 |
| Saturn | 0 → 6 | 487 424 | 412 106 |
| Uranus | 0 → 7 | 433 152 | 356 328 |
| Neptune | 0 → 8 | 433 152 | 356 545 |
| Pluto | 0 → 9 | 433 152 | 356 632 |

- **The final size**: about **7.0 MB** for all seven (6.35 MB
  gzipped) with a kernel per planet; 6.65 MB as one kernel - the
  repeated comment area is the 0.39 MB difference. Mercury is the
  largest because its fast orbit needs short records.
- **Not measured: the outer planets' own centres.** de440s carries
  every barycentre but a planet's centre only for Mercury and Venus
  (1 → 199 and 2 → 299, identically zero, proved). Mars, Saturn,
  Uranus, Neptune and Pluto (499 … 999 about their barycentres) are in
  NAIF's satellite kernels (`aa_summaries.txt`, retrieved 2026-10-11),
  which are not downloaded; each planet's own study measures and asks
  for them, and the layout takes them as further segments in that
  planet's file.
- **Bit identity on regeneration**: every planet's kernel, cut twice,
  is byte-identical, and bit-identical to de440s on 43 223-86 446
  states. As one growing kernel regenerated at each release, the
  earlier planets' states stay bit-identical at every step (up to
  345 784 states) - **but the file's bytes and digest change at every
  release**.
- **Filename and manifest stability**: with a kernel per planet, a
  released planet's file keeps its name, bytes and digest for ever;
  the manifest gains a planet's keys (`ephemeris.<planet>.kernel`, its
  coverage and checksum) and a new pack version, and its earlier lines
  stand. With one growing kernel, the kernel's digest - and every test
  pinning it - moves with each planet added.
- **The cost of re-releasing**: a kernel per planet changes one new
  file and the three text files (10 KB, measured on the Jovian pack)
  per release, **7.0 MB of kernels committed over the whole road**;
  one growing kernel re-commits the whole kernel each time, **35.2
  MB**.
- **Separate packs per planet (D)** keep the same kernel files and add
  three text files per planet: 21 text files and 0.2 MB more, seven
  manifests, notices and provenances to keep in step, and no gain in
  identity over one logical pack whose members never change.

**The study's recommendation, returned for the pack ruling**: Layout
C as **one logical `planets` pack with a kernel file per planet** -
the released Solar System and Jovian packs byte-identical, every
released planet's file byte-identical for ever, the manifest and notice
growing by addition. The measurement supports the owner's stated
preference for C; within C it favours a kernel per planet over one
growing kernel. The drafted notice and manifest
are `docs/studies/venus/notice-draft.md`.

## Time, and the observer: inherited, and what they cost Venus

Inherited whole: `TimeScales` from the pinned leap-second record with
Espenak–Meeus ΔT outside it; `Observer` on the WGS 84 ellipsoid at sea
level with UT1 = UTC; the same *est.* marks after the exact interval.
The comparison reads every civil instant through the atlas's own
`TimeScales` (`juranometria.tool.TimeScaleTableMain`), so what it
measures is the accepted model's cost, not a library's:

| era | astrometric | distance | horizontal |
|---|---|---|---|
| 1900–1961 | 0.061″ | 16.0 km | 1.05″ |
| 1962–1971 | 0.020″ | 2.6 km | 2.44″ |
| **1972 → 2027-06-28, the exact interval** | **0.003″** | **0.3 km** | 11.68″ |
| after it | 7.1″ | 1 858 km | 6.6″ (not asserted) |

- Venus moves at most **0.0526″ per second of time** (at superior
  conjunction, when the two orbital motions add) and its distance
  changes at most **14.09 km/s**; the Earth turns 15.04″ per second of
  UT1.
- **Before 1972** the accepted ΔT model departs from the record by
  about a second (0.061″ and 16 km at those rates) - what the Moon
  paid in the same seconds.
- **Through the exact interval** UT1 = UTC is the whole horizontal
  difference (11.68″, as the Sun's 11.2″); the J2000 place agrees to
  0.003″.
- **After it** the atlas's ΔT (205.1 s at 2100-12-31) and Horizons'
  part by about 135 s - the atlas's is 108.4 s from Skyfield's, which
  is about 27 s from Horizons' - as the Sun's record measured (133 s):
  **7.1″ on the J2000 place and 1 858 km on the distance**, both the
  model's cost at Venus's own rates, not implementation error. The
  table's time note would say *below 8″ through 2100*.

## Frames and meanings, proposed

Everything the table states is **topocentric** for the observer at sea
level - Venus's parallax reaches **32.87″** (Quito, 2085-12-16), so a
geocentric place would miss every exact-era target - except where a
fixture is named geocentric.

- **Chart position (J2000), apparent of date, horizontal, distance** -
  exactly as the Sun's, the Moon's and Jupiter's: topocentric
  astrometric ICRS/J2000 with light-time and **no aberration**;
  light-time corrected, **deflected by the Sun** (as ruled for Jupiter
  on #472) and aberrated, precessed and nutated to the true equator
  and equinox of date; **airless** altitude and azimuth; observer to
  the light-time-corrected centre of Venus (299, which is the Venus
  barycentre 2 exactly), in AU and kilometres.
- **The Sun's deflection belongs in Venus's apparent place.** Without
  it the apparent place is up to 0.37″ from Horizons beyond 1° of the
  Sun (0.21″ through the exact interval, Skyfield's own time scale);
  with it **0.055″** through the exact interval, 0.067″ in 1962–1971
  and 0.26″ before (the stand-in's own 1900s residual, as Jupiter's).
  Within 1° it is measured and printed, never asserted: at the
  2008-06-09 superior conjunction Venus was 0.064° from the Sun's
  centre, behind its disc, and the formula and Horizons part by 2.95″
  for a Venus no one can see.
- **Apparent diameter** - 2·asin(6051.8 km / d), the PCK's solid-body
  radius, which is Horizons' `Ang-diam` to 0.0000″ on every row. See
  *The radius*, below, for the cloud tops.
- **Elongation ψ** - at the observer, unsigned, between the apparent
  Sun and the apparent (deflected) Venus: Horizons' S-O-T to 0.18″
  from 1962 and 0.22″ before, beyond 1° of the Sun.
- **Side** - east or west of the Sun in **apparent right ascension of
  date**, Venus minus Sun wrapped to (−180°, 180°], as the Moon's
  ruled side: east the evening sky (Horizons' /T), west the morning
  (/L). Measured, not assumed: Horizons' flag agrees with right
  ascension on **11 611 of 11 611** rows and with ecliptic longitude on
  11 477 - the 134 disagreements lie around conjunctions, where the two
  definitions cross on different days. *The conjunction a reader is
  told of is not the side's change*: Espenak's 2026 inferior
  conjunction is 2026-10-24 03:44 in ecliptic longitude, while Venus
  crosses the Sun's right ascension on 2026-10-22 13:13 - for a day and
  a half the table says W before the named conjunction. The table
  states the side; it names no conjunction (no event search in this
  road).
- **Phase angle i** - at Venus, **Horizons' S-T-O, defined as the
  Moon's ruled one** (M2): the Sun where it was one Sun–Venus
  light-time before Venus's light left, its direction aberrated by
  Venus's barycentric velocity; Venus as the observer sees it,
  aberrated by the observer's, reversed. Beyond 1° of the Sun it
  reproduces S-T-O to **0.18″** through the exact interval, 0.19″ in
  1962–1971 and 0.25″ before (11.2″ after the interval, the ΔT model's
  cost). With neither aberration it is 10.5″ off S-T-O and
  24.4″ off Horizons' other angle, *phi* (the Sun's aberration only,
  which the study reproduces to 0.28″).
- **Illuminated fraction** k = (1 + cos i) / 2: Horizons' Illu% to
  **0.0036 percentage points** - Horizons computes Illu% from *phi*,
  which the study reproduces to 0.00002 points, but the Moon's ruling
  keeps one angle for the phase and the fraction, and so does this.
- **Bright limb χ** - Meeus 48.5 from the topocentric apparent-of-date
  Sun and Venus, from celestial north through east, [0°, 360°), as the
  Moon's; held to Meeus 48.5 applied to Horizons' own apparent Sun and
  Venus: **0.01″** through the exact interval and 0.12″ before, clear
  of k < 2 % and k ≥ 98 %; 1.04″ near them. From J2000 inputs it turns
  by up to **0.62°** (1900-05-07), so the input frame is part of the
  definition. The later J2000 drawing rotates this χ into the chart's
  north basis and does not recompute it (the Moon's ruling).
  *Not well-defined* below 2 % and from 98 %: the same thresholds as
  the phase words.

## The radius, measured (the owner's question)

| case | distance | solid body, 6051.8 km | mean cloud tops, +74 km | optical radius, +79 km |
|---|---|---|---|---|
| nearest, 2061-12-26 | 39 605 904 km | 63.035″ | 63.805″ | 63.860″ |
| farthest, 1952-06-23 | 259 693 849 km | 9.613″ | 9.731″ | 9.739″ |

- **Physical meaning.** 6051.8 ± 1.0 km is the solid body: the
  Magellan cartographic mean radius, the IAU WGCCRE value in
  `pck00011.tpc`, and Horizons' radius. What a telescope sees is the
  cloud deck: the mean cloud-top altitude is **74 ± 1 km**
  (Venus Express/VIRTIS; Ignatiev et al. 2009, *Journal of Geophysical
  Research*), and the optical radius measured at 4 500 Å during the
  2012 transit is **6 131 ± 13 km, 79 ± 13 km above the solid body**
  (Reale et al. 2015, *Nature Communications* 6:7563,
  doi:10.1038/ncomms8563). No IAU or NAIF constant names a cloud-top
  radius; the published values are measurements with their own
  wavelength and uncertainty.
- **The angular difference** is **1.22 %** of the diameter at every
  distance: **0.77″ at the nearest, 0.12″ at the farthest**.
- **The visible consequence** is below a pixel everywhere the atlas
  draws: at the 1° field it is **0.19 px** at the largest, on the A4
  sheet at 300 dpi 0.7 px; at any wider field less. The table's
  diameter, rounded to 0.1″, changes by up to 0.8″ - a reader comparing
  the table with an occultation or transit measurement would see it.
- **Proposed:** the table states the solid-body diameter, named as
  such in its explanation, because it is the authority's constant and
  Horizons' value and the comparison holds it exactly; the cloud-top
  difference is stated once in the explanation rather than drawn. The
  choice is the owner's; nothing here depends on terminology.

## Accuracy, measured

Against JPL Horizons (DE441 there; an independent implementation in
the same authority family), row by row: five observers - Oslo 59.9° N,
Quito 0.2° S, Cape Town 33.9° S, Alert 82.5° N, the Chatham Islands
176.5° W across the date line - plus geocentric; at the named instants
and the four representative appearances; every 7 days 1900–2100 at
Oslo and geocentrically, every 30 days at the other four; hourly
within three days of the three inferior conjunctions and the superior
conjunction in the named set, six-hourly within ten days of each
greatest elongation, with the Sun on the same grids; 31 891 Venus rows
(`authority-comparison.md`). The study stands in for the service with
Skyfield reading `de440s.bsp` under the atlas's time model and the
Sun's deflection:

| era | rows | astrometric | apparent, > 1° from the Sun | horizontal | distance | diameter | elongation, > 1° | phase angle (S-T-O), > 1° | illuminated | χ, clear of new and full |
|---|---:|---|---|---|---|---|---|---|---|---|
| 1900–1961 | 9 496 | 0.061″ | 0.26″ | 1.05″ | 16.0 km | 0.0000″ | 0.22″ | 0.25″ | 0.0036 pt | 0.12″ |
| 1962–1971 | 1 538 | 0.020″ | 0.067″ | 2.44″ | 2.6 km | 0.0000″ | 0.18″ | 0.19″ | 0.0035 pt | 0.02″ |
| **1972 → 2027-06-28** | 9 268 | **0.003″** | **0.055″** | 11.68″ | **0.3 km** | 0.0000″ | 0.18″ | 0.18″ | 0.0036 pt | 0.01″ |
| after | 11 589 | 7.1″ | 6.9″ | 6.6″ (not asserted) | 1 858 km | 0.0011″ | 8.0″ | 11.2″ | 0.0047 pt | 14.3″ |

**Proposed targets** (through the exact interval as the tests should
hold the service; before and after it, widened only by the time
model's measured cost, as the Sun's, the Moon's and Jupiter's were;
every one returns to the owner before it is widened):

| quantity | 1972 → 2027-06-28 | 1962–1971 | 1900–1961 | after |
|---|---|---|---|---|
| astrometric J2000 | ≤ 0.05″ | ≤ 0.1″ | ≤ 0.2″ | ≤ 8″ |
| apparent of date, elongation > 1°, the Sun's deflection | ≤ 0.2″ | ≤ 0.2″ | ≤ 0.5″ | ≤ 8″ |
| apparent, elongation ≤ 1° | measured, printed, not asserted | | | |
| horizontal | ≤ 20″ (the Sun's) | ≤ 20″ | ≤ 20″ | not asserted |
| distance | ≤ 2 km | ≤ 10 km | ≤ 30 km | ≤ 2 500 km |
| diameter | ≤ 0.001″ | ≤ 0.001″ | ≤ 0.001″ | ≤ 0.01″ |
| elongation, > 1° | ≤ 0.5″ | ≤ 0.5″ | ≤ 0.5″ | ≤ 10″ |
| phase angle (S-T-O), elongation > 1° | ≤ 1″ | ≤ 1″ | ≤ 1″ | ≤ 15″ |
| illuminated fraction | ≤ 0.01 point | ≤ 0.01 | ≤ 0.01 | ≤ 0.01 |
| χ, k between 2 % and 98 % | ≤ 1″ | ≤ 1″ | ≤ 1″ | ≤ 20″ |
| side (E/W) against Horizons' /T and /L | every row | every row | every row | every row |

No measured value exceeds a proposed target. The targets hold the
measured maxima with margin; the after-interval column bounds the
ΔT model's cost at Venus's own rates (0.0526″/s and 14.09 km/s for
about 135 s), not implementation error.

## The named fixtures, proposed

- **Espenak's rows** (`venus-events.txt`, nine rows, three almanacs).
  *Conjunctions* (apparent geocentric ecliptic longitude of date): all
  four DE440 instants fall **inside the listed hour**, 9–44 minutes
  after it - the rows read as the hour begun; read as rounded, two
  (+36, +44 min) exceed Espenak's stated ±30 minutes. Proposed
  assertion: the instant lies in the listed hour. *Greatest
  elongations*: the values agree to Espenak's printed 0.1° (46.041°,
  45.803°, 45.892°, 46.480°, 45.725°); the instants are 27–81 minutes
  from the listed hour, where the elongation is within **0.04″** of its
  maximum - an instant no model fixes to the hour. Proposed: the value
  to 0.1°, the instant within ±2 hours, the flatness named.
- **Horizons' named instants** at the five observers and
  geocentrically: the interval's start and end, the record's
  boundaries (1962, 1972, 2017), J2000.0, the exact interval's end
  2027-06-28, each Espenak event; the 2012 transit (2012-06-06 01:00,
  Venus 0.15° from the Sun's centre, in front of its disc, k
  0.0003 %); and four representative appearances of the 2026–27
  apparition (gibbous evening 2026-06-15, crescent evening 2026-09-21,
  crescent morning 2026-11-30, gibbous morning 2027-03-08), each the
  weekly Oslo row nearest a quarter or three quarters lit.
- **The fixture boundary, as ruled** (ruling 6): committed - the
  named instants and the four appearances for **all five observers and
  geocentrically** (Venus and Sun), so production tests hold parallax,
  polar, southern, horizon and date-line behaviour; the hourly 2026
  inferior conjunction, the fortnightly 2026–27 apparition (each with
  the Sun) and the 7-day 1900–2100 matrix, at Oslo: **29 files,
  3.44 MB**, indexed with their digests in `horizons/README.md` by
  `scripts/venus-fixture-index.py`. Retained outside Git with URLs
  and digests (`horizons-kept.md`): the other observers' 30-day
  matrices, the geocentric matrices and the other dense windows.

## The reader's table, proposed

`docs/studies/venus/controls-venus-*.png`, in both languages and the
dark appearance, from the kept Horizons rows (`measurements.md`):

- **Venus joins the Sun's and the Moon's table family**
  (`SolarTableSession`, the 1 000-row cap, the same refusals read from
  the released strings, Update from Place and Time, nothing calculated
  on opening or expanding, nothing persisted). Its Controller section
  sits **between the Sun and the Moon** - Solar System sections are
  ordered outward from the Sun (#499).
- **The instant card**: right ascension and declination (J2000),
  altitude, azimuth, distance in AU and million km, apparent diameter,
  elongation with E or W and the sky it means, phase angle, illuminated
  fraction, the phase word and the lit side (χ of date with the
  nearest of sixteen points, or *not well-defined*).
- **The range**: one row per instant with the elongation, the
  illuminated fraction, the phase word, the diameter, the distance,
  the altitude and the lit side; the mock-up's 2026–27 apparition at
  14-day steps shows the disc growing from 13″ to 60″ while the lit
  fraction falls from 79 % to 1 % and the side changes.
- **No surface is implied**: the diagrams draw a flat lit shape on a
  dark disc, and the words speak only of light and size.

**The phase words** (`measurements.md`), on the Moon's ruled half-open
bands so the two bodies change words at the same fractions:

| k | English | Norsk |
|---|---|---|
| < 2 % | near inferior conjunction | nær nedre konjunksjon |
| 2–48 % | crescent | sigd |
| 48–52 % | near half lit | nær halvt belyst |
| 52–98 % | gibbous | mer enn halvt belyst |
| ≥ 98 % | near full | nær full |
| centre within the Sun's radius plus Venus's, i > 90° | in front of the Sun | foran Solen |
| the same, i < 90° | behind the Sun | bak Solen |

The two geometric states take precedence; Norwegian uses *Solen*,
*Ø*/*V* and *Elongasjon fra Solen* as the released Moon table does.

## The cartography handoff

Measured on the atlas's own field ladder (read from `ChartViewState`)
through the production projection, and on the sheets; no Venus is
drawn on any page (`measurements.md`, section *The cartography
handoff*):

- **Diameter**: 9.61″ (1952-06-23) to 63.03″ (2061-12-26) at Oslo over
  1900–2100. On the 900 × 700 page **2.40–15.76 px at 1°**, 1.20–7.88 px
  at 2°, under a pixel from 12° wide; on the A4 sheet's chart area
  0.73–4.76 mm at 1° (56 px at 300 dpi), on Letter 0.68–4.45 mm.
- **The phase at 1°.** Counting a week readable when the disc is at
  least 6 px and its lit and dark parts each at least 1 px (the
  study's proposed criterion, for the ruling), the phase reads on the
  screen page in **19.5 %** of the weeks at 1° and in **none at 2° or
  wider**; on A4 at 300 dpi in 63.4 % at 1°, 47.0 % at 2°, 3.9 % at 6°.
  The largest half-lit disc is 27.7″, 6.9 px at 1°. Near inferior
  conjunction the disc is largest (15 px) and the crescent under a
  tenth of a pixel wide; at superior conjunction it is full and 2.4 px.
- **Phase geometry for the J2000 chart**: the lit shape is the bright
  half-disc with the terminator's half-ellipse of semi-axis |1 − 2k|·r,
  turned to χ - computed of date, rotated into the J2000 chart's local
  north at Venus's projected place, not recomputed from J2000 inputs
  (which would differ by up to 0.62°).
- **#481's role**: a sub-1° field is what would make Venus's phase
  readable on screen beyond a fifth of the weeks. 5.2.0 does not need
  it to draw Venus honestly - a true-size disc or a minimum mark with
  the phase stated in the table - but it does need it to *show* the
  phase on screen at most dates. The study recommends deciding that
  in Part II with #481's own measurement, not here.
- **Identity and time**: Venus's identity on the page is `venus`, as
  the Sun's `sun` and the Moon's `moon`; a selected Venus is an
  occurrence - the identity with the chart instant and observer it
  was computed for - because its place belongs to that instant. The
  chart core owns no clock: the instant comes from Place and Time,
  and a selection that outlives an instant change re-resolves from
  the page or says which instant it holds.

## For the rulings

1. **Authority**: DE440 from the released `de440s.bsp`; the PCK radii;
   Horizons for comparison; Espenak for named timing; DE442 not
   adopted (0.006″).
2. **Pack and interval**: layout A, a `venus` pack of one 1.18 MB
   kernel, 1900–2100; the released Solar System and Jovian packs
   untouched.
3. **Meanings**: as proposed above - topocentric; deflected apparent
   place; the side by right ascension; S-T-O and k as the Moon's; χ of
   date; the solid-body diameter.
4. **The radius**: the solid body, with the cloud-top difference
   stated, or the cloud tops - the owner's ruling, on the measurement
   above.
5. **Phase vocabulary**: the words and bands above, both languages.
6. **Fixtures**: the committed set and the outside-Git set as listed;
   the Espenak assertions as proposed.
7. **Targets**: the table above.
8. **Cartography handoff**: the readability criterion, #481's role,
   the identity/time contract.

## The ruling

The owner's rulings (2026-10-11), recorded as given:

1. **Authority accepted.** Keep DE440. DE442 changes Venus by only
   0.006″ and offers no measured Venus benefit; it stays comparison
   evidence only.
2. **Pack returned once with a third layout.** Do not yet freeze a
   Venus-named pack. Measure Layout C - one new logical `planets` pack,
   initially containing Venus, designed to gain Mercury, Mars, Saturn,
   Uranus, Neptune and Pluto across later 5.x releases - against the
   final estimated size with all planned planets, the incremental size
   per planet, whether existing planetary states remain bit-identical
   when the pack is regenerated, filename and manifest stability, the
   cost of repeatedly re-releasing the growing pack, and separate
   per-planet packs against one growing planetary pack. The released
   Solar System and Jovian packs stay byte-identical either way. The
   owner's current preference is Layout C; measurement settles it.
   *Measured in [The pack](#the-pack-measured); returned for the
   ruling.*
3. **Interval and meanings accepted.** 1900–2100; topocentric
   quantities; the Sun's deflection for the apparent place; east and
   west by right ascension of date; the Moon's S-T-O phase definition
   and its illuminated-fraction definition; χ apparent of date, rotated
   later into the J2000 page. The side changing before Espenak's
   ecliptic-longitude conjunction is honest: the table states east or
   west and does not claim to announce a conjunction.
4. **Radius: the solid body.** 6051.8 km, the result called the
   *solid-body apparent diameter* in the explanation, which states that
   the visible cloud deck is about 1.22 % larger, wavelength-dependent,
   and at most 0.77″ larger in the measured interval. The uncertain
   cloud radius is never drawn.
5. **Phase vocabulary accepted**: the proposed bands and both
   languages, the geometric *in front of the Sun* and *behind the Sun*
   states taking precedence.
6. **Fixtures extended before freezing.** The Oslo matrices and dense
   windows as proposed, and also the named-instant and
   representative-appearance responses for all five observers and
   geocentrically, so production tests hold parallax, polar, southern,
   horizon and date-line behaviour. The other observers' long matrices
   and the exploratory dense windows stay outside Git with their URLs
   and digests. The Espenak assertions accepted: a conjunction falls
   within the listed hour; a greatest elongation agrees to 0.1° and
   occurs within ±2 hours; the record says why the maximum is too flat
   for a sharper time assertion (within 0.04″ of its maximum the
   instant is 27–81 minutes from the listed hour).
7. **Accuracy targets accepted.** They separate the exact civil-time
   interval from the measured ΔT cost afterward; none is widened later
   without returning.
8. **Cartography handoff accepted.** The 1-pixel lit/dark and
   6-pixel-disc readability criterion stays a study measure, not a
   rendering rule. #481 is decided during Venus cartography: Venus can
   be drawn honestly without closer zoom, but closer zoom is needed to
   make its phase readable at most dates. A selected Venus is an
   occurrence with shared chart context - stable identity plus instant
   and observer; the chart core still owns no clock.


## #501, as built

**The pack, as ruled (layout C).** `src/resources/planets/`: one logical
`planets` pack, version 1 - `juranometria-de440-venus-1900-2100.bsp`
(1 242 112 bytes; segments 0 → 2 and 2 → 299 from the released pack's
pinned `de440s.bsp`, every state on a 1.7-day grid identical to the
source, 2 → 299 identically zero), `NOTICE-planets.md`, `PROVENANCE.md`
and `manifest.properties`, built by `make import-planets`
(`PlanetsPackMain`). The manifest lists its planets under
`pack.planets` and keys each planet's facts `ephemeris.<planet>.*`; a
later planet adds its file and its keys and raises the pack version.
The builder refuses to write a pack in which a released planet's file
would change, and proves the Solar System kernel (`63fbf570…`) and both
Jovian kernels (`efddb718…`, `6ad781a4…`) untouched before writing;
rebuilt twice, the pack is byte-identical. The drafted notice's key
`planets` became `pack.planets` so the language scanner reads it as an
identifier - the ledger gains no row (it holds 149 of its 150).

**The service.** `VenusService` reads the Solar System pack's Sun, Earth
and time scales and the planets pack's Venus, and answers the ruled
meanings: astrometric J2000; apparent of date deflected by the Sun
(the one-body formula Jupiter's service applies, held equal to it by
`VenusServiceTest`) and aberrated; airless horizontal; distance; the
solid-body apparent diameter; the Moon's S-T-O and k; elongation and
the side of the Sun by apparent right ascension of date; the phase word
with the geometric states first; χ of date with its conditioning. The
Sun, the Moon and Jupiter are untouched: `SolarSystemService` and
`JovianSystemService` are unchanged, and their packs byte-identical
(`PlanetsPackTest`).

**Measured, against Horizons, every committed fixture** (10 771 rows;
`docs/studies/venus/service-accuracy.md`, `make venus-accuracy-report`,
held by `VenusReferenceVectorTest`):

| quantity | 1900–1961 | 1962–1971 | 1972 → 2027-06-28 | after |
|---|---|---|---|---|
| astrometric | 0.061″ | 0.020″ | **0.001″** | 7.118″ |
| apparent, elongation > 1° | 0.134″ | 0.086″ | **0.129″** | 7.231″ |
| horizontal | 0.906″ | 1.977″ | 11.680″ | 6.607″ (not asserted) |
| distance | 15.9 km | 2.6 km | **0.2 km** | 1 857.9 km |
| diameter | 0.0000″ | 0.0000″ | 0.0000″ | 0.0011″ |
| elongation, > 1° | 0.214″ | 0.181″ | 0.180″ | 7.977″ |
| phase angle (S-T-O), > 1° | 0.251″ | 0.186″ | 0.180″ | 11.181″ |
| illuminated, points | 0.0035 | 0.0035 | 0.0035 | 0.0047 |
| χ, k in [2 %, 98 %) | 0.039″ | 0.000″ | 0.053″ | 3.900″ |

**Every value is inside its ruled target; no target is amended.** The astrometric
differences are measured by the atlas's own haversine: the first CI run
(#505) found the arc cosine of a dot product quantised at machine
precision for angles of a few milliarcseconds, rounding to 0.003″ on
one machine and 0.004″ on another; the study's 0.003″ for this era was
that quantisation too. Measured stably the exact era agrees to 0.001″. The
one number above the study's stand-in is the exact interval's apparent
place, 0.129″ against 0.055″: the service precesses and nutates with
the atlas's own IAU 1976/1980 models, where the stand-in used IAU 2000A
- the Sun's released contract measured 0.14″ for the same cause - and
it stays within the ruled 0.2″. The side of the Sun agrees with
Horizons' /T and /L on all 10 771 rows; Venus's parallax reaches
32.15″; every phase word occurs. Espenak's nine rows hold under the
ruled assertions (`VenusNamedEventsTest`): the four conjunctions 8–44
minutes into the listed hour; the five greatest elongations at the
printed tenth and side, 29–83 minutes from the listed hour, where the
elongation is within 0.040″ of its maximum. The mutation rivals
(`VenusMutationTest`) - a geocentric place, the civil instant read as
TT, no deflection near the Sun, no aberration, the geometric phase
angle, a J2000 χ, a side by ecliptic longitude, the cloud-top radius -
each fail their target.

**The first Venus table** is `docs/studies/venus/table-measurements.md`
(`make venus-table-study`): the five observers, the date-line one
included, at Espenak's nine events, the four representative
appearances, a chosen day and both ends of the interval; Oslo every 14
days through the 2026–27 apparition and hourly through the 2012
transit - in the Moon table's columns and rounding, the phase words as
ruled, in English until #502 carries them in the language files.

**Packaged.** `NOTICE-planets.md` joins the licensing inventory (12
resources), the archive (`licenses/NOTICE-planets.md`), `LICENSING.md`
and the About dialog's notices in both languages; the packaged
acceptance prints *planets pack OK*, checking Venus at Oslo against a
Horizons row the image restates (held to the committed row by
`PackagedVenusSampleTest`), the 2026 greatest eastern elongation near
half lit in the evening sky, and the 2012 transit in front of the Sun.
No dialog, Controller section, selection or chart mark exists yet.
