# Knowing where Mercury is and how it is lit

**Sprint 49, issue #528.** The numerical contract for Mercury, proposed
from measurement before any production data or code exists. **Ruled
by the owner (2026-10-14)** - see [The ruling](#the-ruling) at the end,
which accepts the eight proposals as written. No production Mercury
service, table, selection or rendering exists. It
inherits the Sun's, the Moon's, Jupiter's and Venus's contracts
(`sun-computation.md`, `moon-computation.md`, `jovian-system.md`,
`venus-system.md`) wherever it does not say otherwise, and says so
where it inherits a ruling rather than proposing afresh. The study is
`docs/studies/mercury/`: `inventory.md` (the read-only first action),
`make mercury-system-study` for the table proposal, diagrams and the
cartography handoff (`measurements.md`), `make mercury-pack-study` for
the excerpt (`pack-measurements.md`),
`scripts/mercury-authority-comparison.py` for the error matrix
(`authority-comparison.md`); the numbers quoted here are those
reports'. The contract study and its images are registered evidence
that CI reproduces; the pack report and the authority comparison read
the pinned raw downloads the runner does not hold, so they are
reproduced locally and held as committed, as Venus's are.

> Mercury must eventually answer where it is in the chart's J2000
> frame and in the observer's sky; how far away and how large it
> appears; how much is illuminated, what phase it has, and which way
> the bright side faces; how elongation, apparent size and phase
> change together near the Sun; what can honestly be drawn at the
> ordinary 1° floor; whether Mercury supplies the concrete case for
> object-dependent fields below 1° in #481. (#527)

## The question

Before the road to 5.3.0 tables or draws Mercury, the atlas must
answer those questions offline, permanently, to a measured accuracy,
in both languages. This record proposes the authority and its terms,
the pack and its interval, the time and frame model with its measured
costs, the meaning of every observable, the phase vocabulary, the
accuracy targets, the named fixtures, the reader's table and the
cartography handoff. It builds nothing.

## The authority, inventoried from the official sources (2026-10-13)

Read-only first (`inventory.md`); nothing was downloaded before the
owner's permission, and nothing beyond the two permitted downloads
after it.

| what | source | state |
|---|---|---|
| Mercury: **DE440**, the released packs' own source; segments **0 → 1** (the Mercury barycentre about the solar-system barycentre) and **1 → 199** (Mercury about its barycentre, identically zero: no moon), `DE-0440LE-0440`, type 2 | `imports/raw/solar-system/de440s.bsp`, pinned, sha256 `c1c7feea…` = the planets pack's `source.de440s.sha256` | no new source |
| Mercury's radii: IAU WGCCRE 2015 as NAIF's `pck00011.tpc` states them, `BODY199_RADII = (2440.53, 2440.53, 2438.26)` - a triaxial figure where Venus's and the Sun's are spheres | `imports/raw/jovian-system/pck00011.tpc`, pinned, sha256 `3dff7b1d…` = the pack's `source.pck.sha256` | no new source |
| the comparison service: **JPL Horizons** (DE441; Mercury target radius 2440.53 km) | `https://ssd.jpl.nasa.gov/api/horizons.api`, 56 responses kept whole (`horizons-kept.md`), 55 committed (`horizons/README.md`) | permitted 2026-10-13 |
| named-case timing: **Espenak, Sky Event Almanacs** (AstroPixels), 2012, 2026 and 2100 GMT - retained for Venus, and carrying Mercury's rows | `~/juranometria-runs/venus-raw/`, digests in `mercury-events.txt` | no new download |
| the transits: **Espenak, *Seven Century Catalog of Mercury Transits: 1601 CE to 2300 CE*, NASA/GSFC** | `https://eclipse.gsfc.nasa.gov/transit/catalog/MercuryCatalog.html`, 26 830 B, sha256 `e29ff3e1…`, retrieved 2026-10-13T20:30:23Z, retained whole outside Git; the 28 rows of 1907–2098 quoted with the acknowledgment the page asks for | permitted 2026-10-13 |
| the newest planetary ephemeris, **DE442s**, for comparison only | `~/juranometria-runs/venus-raw/de442s.bsp`, retained for Venus (owner's approval B on #500) | no new download |

**DE440 stays.** Daily over 1900–2100 (73 353 epochs), DE442s moves
Mercury's geometric geocentric direction by at most **0.000049″** and
its distance by at most **0.023 km**; the
barycentric Mercury, Earth and Sun each by about half a kilometre
(`authority-comparison.md`, *DE440 against DE442s*). The Venus
study's 0.0061″ for the same comparison was the arc cosine of a dot
product quantised at machine precision - the #505 finding again, now
measured with a stable angle - so both planets' DE442 figures are a
hundred times smaller than that record says; nothing in either shows
a benefit. DE442s remains a study download, never bundled, never a
build dependency.

**Terms.** NAIF's rules, re-read on 2026-10-13, are those the released
notices quote: redistribution of unmodified kernels permitted; a
modified kernel renamed, its attribution replaced with the modifier's;
no fees or licensing. Mercury needs **no new source and no new
terms**: both segments come from the pinned `de440s.bsp`, the radii
from the pinned PCK. Espenak's almanac rows are quoted as for Venus;
the transit catalogue's own terms - *"Permission is freely granted to
reproduce this data when accompanied by an acknowledgment: Transit
Predictions by Fred Espenak, NASA/GSFC"* - are met by the
acknowledgment in `mercury-events.txt`; neither is bundled, neither is
a runtime or build input. The manifest, notice and provenance changes
are a `mercury` entry beside the `venus` one, as the builder's
`Planet` record already shapes them (`pack-measurements.md`).

## The pack, measured

Cut with the released builder's own helpers - its interval, margins,
internal name and comment - into the study directory, never into the
pack (`pack-measurements.md`):

- **Mercury is 3.30 MB for the whole released interval**:
  `juranometria-de440-mercury-1900-2100.bsp`, **3 299 328 bytes**
  (3 112 736 gzipped), every state on the builder's 1.7-day grid
  bit-identical to `de440s.bsp` (86 370 states), 1 → 199 identically
  zero at 43 185 epochs - 2.7 × Venus's 1.24 MB, because Mercury's orbit
  needs denser Chebyshev granules. Segment 0 → 1 alone is 3 237 888
  bytes; 1 → 199 alone 5 120.
- **Layout C as ruled on #500**: one logical `planets` pack, a kernel
  file per planet, the version raised. The pack would go from
  1 249 439 to **4 548 767 bytes** (plus the Mercury lines of its
  notice, provenance and manifest); the Venus kernel
  (`8b0760b4…`), the Solar System kernel (`63fbf570…`) and the two
  Jovian kernels (`efddb718…`, `6ad781a4…`) stay byte for byte, which
  the builder proves before writing and refuses otherwise.
- The manifest gains `ephemeris.mercury.*` (bodies `1,199`, segments
  2, the same coverage) and `pck.BODY199_RADII=2440.53,2440.53,2438.26`.

## Time, and the observer: inherited, and what they cost Mercury

Inherited whole: `TimeScales` from the pinned leap-second record with
Espenak–Meeus ΔT outside it; `Observer` on the WGS 84 ellipsoid at sea
level with UT1 = UTC; the same *est.* marks after the exact interval.
The comparison reads every civil instant through the atlas's own
`TimeScales` (`juranometria.tool.TimeScaleTableMain`), so what it
measures is the accepted model's cost, not a library's:

| era | rows | astrometric | distance | horizontal |
|---|---|---|---|---|
| 1900–1961 | 6 530 | 0.106″ | 56.3 km | 1.04″ |
| 1962–1971 | 1 056 | 0.025″ | 15.1 km | 1.98″ |
| **1972 → 2027-06-28, the exact interval** | 6 293 | **0.003″** | **0.2 km** | 11.47″ |
| after it | 7 816 | 11.9″ | 6 495 km | 10.8″ (not asserted) |

- Mercury moves at most **0.0915″ per second of time** (5.49″ a
  minute, 2064-06-02) and its distance changes at most **49.15 km/s**:
  1.7 × and 3.5 × Venus's rates. The Earth turns 15.04″ per second of
  UT1.
- **Before 1972** the accepted ΔT model departs from the record by
  about a second, which at those rates is 0.106″ and 56 km - the same
  second Venus and the Moon paid, at Mercury's speed.
- **Through the exact interval** UT1 = UTC is the whole horizontal
  difference (11.47″, as the Sun's 11.2″); the J2000 place agrees to
  0.003″.
- **After it** the atlas's ΔT (205.1 s at 2100-12-31) and Horizons'
  part by about 135 s - the atlas's is 108.4 s from Skyfield's, which
  is about 27 s from Horizons' - and at Mercury's rates that is **11.9″
  on the J2000 place and 6 495 km on the distance**: the model's cost,
  not implementation error, and larger than Venus's 7.1″ and 1 858 km
  because Mercury is faster. The table's time note would say *below
  12″ through 2100*.

## Frames and meanings, proposed

Everything the table states is **topocentric** for the observer at sea
level - Mercury's parallax reaches **15.88″** (Chatham, 2100-06-10) -
except where a fixture is named geocentric. The meanings are Venus's,
ruled on #500, applied to Mercury:

- **Chart position (J2000), apparent of date, horizontal, distance** -
  topocentric astrometric ICRS/J2000 with light-time and no
  aberration; light-time corrected, deflected by the Sun and
  aberrated, precessed and nutated to the true equator and equinox of
  date; airless altitude and azimuth; observer to the light-time
  corrected centre of Mercury (199, which is the barycentre 1
  exactly), in AU and kilometres.
- **The Sun's deflection belongs in Mercury's apparent place**, more
  than in Venus's: Mercury never stands more than 28° from the Sun.
  Beyond 1° of the Sun the deflected apparent place agrees with
  Horizons to **0.054″** through the exact interval, 0.072″ in
  1962–1971 and 0.28″ before; without deflection 0.19″ through the
  exact interval. Within 1° - superior and inferior conjunction, the
  transits - it is measured and printed, never asserted: 0.72″ through
  the exact interval, where the one-body formula and Horizons part for
  a Mercury no one can see.
- **Apparent diameter** - 2·asin(2440.53 km / d), the PCK's equatorial
  radius, which is Horizons' `Ang-diam` to 0.0000″ on every row. See
  *The radius*, below.
- **Elongation ψ** - at the observer, unsigned, between the apparent
  Sun and the apparent (deflected) Mercury: Horizons' S-O-T to 0.18″
  through the exact interval, 0.19″ in 1962–1971, 0.25″ before, beyond
  1° of the Sun. Mercury's greatest elongations run **18°–28°**.
- **Side** - east or west of the Sun in apparent right ascension of
  date, Mercury minus Sun wrapped to (−180°, 180°]: Horizons' /T and /L
  agree with right ascension on **719 of 719** rows and with ecliptic
  longitude on 639 - the 80 disagreements lie around conjunctions, as
  Venus's did. The table states the side; it names no conjunction.
- **Phase angle i** - Horizons' S-T-O, defined as the Moon's ruled one
  (M2): the Sun where it was one Sun–Mercury light-time before
  Mercury's light left, aberrated by Mercury's barycentric velocity;
  Mercury as the observer sees it, aberrated by the observer's,
  reversed. Beyond 1° it reproduces S-T-O to **0.18″** through the
  exact interval, 0.21″ in 1962–1971, 0.43″ before (38.6″ after the
  interval, the ΔT model's cost). With neither aberration it is 21.2″
  off S-T-O and 40.8″ off *phi* - the aberrations matter twice as much
  as for Venus.
- **Illuminated fraction** k = (1 + cos i) / 2: Horizons' Illu% to
  **0.0048 percentage points** through the exact interval.
- **Bright limb χ** - Meeus 48.5 from the topocentric apparent-of-date
  Sun and Mercury, from north through east; held to Meeus 48.5 on
  Horizons' own Sun and Mercury: **0.00″** through the exact interval,
  0.02″ in 1962–1971 and 0.05″ before, clear of k < 2 % and k ≥ 98 %.
  From J2000 inputs it turns by up to **0.60°** (1900-01-01), so the
  input frame is part of the definition, as for Venus. *Not
  well-defined* below 2 % and from 98 %.

## The radius, measured

| case | distance | equatorial 2440.53 km | mean 2439.4 km | polar 2438.26 km | mean − equatorial | Horizons Ang-diam |
|---|---|---|---|---|---|---|
| nearest, 1936-06-01 | 82 147 074 km | 12.256″ | 12.250″ | 12.245″ | −0.006″ (−0.05 %) | 12.256″ |
| farthest, 2034-12-04 | 217 141 880 km | 4.637″ | 4.634″ | 4.632″ | −0.002″ (−0.05 %) | 4.637″ |

Mercury has no atmosphere and no cloud deck: there is no second
convention a reader could see. The PCK's figure is triaxial; Horizons
uses the equatorial 2440.53 km and reproduces to 0.0000″. The IAU 2015
mean radius, 1.13 km smaller, would narrow the diameter by 0.05 % -
0.006″ at the largest, 0.001 px at the 1° field.
**Proposed:** the equatorial radius, 2440.53 km, named as the PCK's and
Horizons', with the figure's three axes stated once in the explanation.

## Accuracy, measured

Against JPL Horizons, row by row, with the study's stand-in for the
service (Skyfield reading `de440s.bsp` under the atlas's time model
and the Sun's deflection): the five observers at the 75 named instants
(the boundaries, the 2012, 2026 and 2100 conjunctions, elongations and
occultations, the 28 transits) and the four appearances, geocentrically
too; every 7 days 1900–2100 at Oslo and geocentrically; hourly within
three days of the 2026-03-07 inferior conjunction and six-hourly within
ten days of the 2026-04-03 greatest western elongation, with the Sun
on the same grids; every 3 days through the 2026 apparitions - 21 695
Mercury rows (`authority-comparison.md`):

| era | rows | astrometric | apparent, > 1° from the Sun | horizontal | distance | diameter | elongation, > 1° | phase angle (S-T-O), > 1° | illuminated | χ, clear of new and full |
|---|---:|---|---|---|---|---|---|---|---|---|
| 1900–1961 | 6 530 | 0.106″ | 0.28″ | 1.04″ | 56.3 km | 0.0000″ | 0.25″ | 0.43″ | 0.0048 pt | 0.05″ |
| 1962–1971 | 1 056 | 0.025″ | 0.072″ | 1.98″ | 15.1 km | 0.0000″ | 0.19″ | 0.21″ | 0.0048 pt | 0.02″ |
| **1972 → 2027-06-28** | 6 293 | **0.003″** | **0.054″** | 11.47″ | **0.2 km** | 0.0000″ | 0.18″ | 0.18″ | 0.0048 pt | 0.00″ |
| after | 7 816 | 11.9″ | 11.6″ | 10.8″ (not asserted) | 6 495 km | 0.0004″ | 12.0″ | 38.6″ | 0.0119 pt | 5.2″ |

The matrix is thinner than Venus's on purpose: the four other
observers' 30-day matrices and the other dense windows were not
fetched (the retained budget), so Quito, Cape Town, Alert and Chatham
are held at the 75 named instants and four appearances only.

**Proposed targets** (through the exact interval as the tests should
hold the service; before and after it, widened only by the time
model's measured cost; every one returns to the owner before it is
widened):

| quantity | 1972 → 2027-06-28 | 1962–1971 | 1900–1961 | after |
|---|---|---|---|---|
| astrometric J2000 | ≤ 0.05″ | ≤ 0.1″ | ≤ 0.2″ | ≤ 15″ |
| apparent of date, elongation > 1°, the Sun's deflection | ≤ 0.2″ | ≤ 0.2″ | ≤ 0.5″ | ≤ 15″ |
| apparent, elongation ≤ 1° | measured, printed, not asserted | | | |
| horizontal | ≤ 20″ (the Sun's) | ≤ 20″ | ≤ 20″ | not asserted |
| distance | ≤ 2 km | ≤ 30 km | ≤ 100 km | ≤ 8 000 km |
| diameter | ≤ 0.001″ | ≤ 0.001″ | ≤ 0.001″ | ≤ 0.01″ |
| elongation, > 1° | ≤ 0.5″ | ≤ 0.5″ | ≤ 0.5″ | ≤ 15″ |
| phase angle (S-T-O), elongation > 1° | ≤ 1″ | ≤ 1″ | ≤ 1″ | ≤ 45″ |
| illuminated fraction | ≤ 0.01 point | ≤ 0.01 | ≤ 0.01 | ≤ 0.02 |
| χ, k between 2 % and 98 % | ≤ 1″ | ≤ 1″ | ≤ 1″ | ≤ 20″ |
| side (E/W) against Horizons' /T and /L | every row | every row | every row | every row |

No measured value exceeds a proposed target. The after-interval column
bounds the ΔT model's cost at Mercury's own rates (0.0915″/s and
49.15 km/s for about 135 s), not implementation error; the 1900–1961
distance and after-interval columns are wider than Venus's for the same
reason.

## The named fixtures, proposed

- **Espenak's almanac rows** (`mercury-events.txt`, 38 rows, three
  almanacs: twelve conjunctions and greatest elongations a year, two
  lunar occultations). *Conjunctions* (apparent geocentric ecliptic
  longitude of date): the DE440 instants fall **−16 to +39 minutes**
  from the listed hour - on both sides of it, so Espenak's Mercury
  rows read as rounded to the nearest hour, not as the hour begun (the
  Venus rows all fell inside the hour begun), and one 2100 row is
  beyond the ±30 minutes Espenak states. Proposed assertion: the
  instant within ±1 hour of the listed one (the atlas's instant is the
  ecliptic-longitude conjunction's; the hour is Espenak's rounding). *Greatest elongations*: the
  values agree to Espenak's printed 0.1° on every row (18.210° for
  18.2°, 27.493° for 27.5°, 27.819° for 27.8°, …); the instants are
  −26 to +35 minutes from the listed hour, where the elongation is
  within **0.13″** of its maximum. Proposed: the value to 0.1° and its side,
  the instant within ±2 hours, the flatness named. *The occultations*
  (2012-12-12 00:44, 2026-02-18 23:03): named instants only, a
  cross-check of the Moon's and Mercury's apparent places together;
  they anchor no assertion in this contract.
- **Espenak's transit rows** (28, 1907–2098, NASA/GSFC): DE440's
  greatest transit - the least geocentric apparent separation of the
  centres - falls **−0.8 to +2.7 minutes** from the listed one, the
  later ones later, which is the ΔT model parting after 2027; the
  minimum separation agrees **to the published 0.1″ on all 28**;
  contacts I and IV from the apparent radii agree to within about a
  minute through the exact interval and three after it. Proposed: the
  greatest transit within ±5 minutes, the minimum separation to 0.2″;
  *in front of the Sun* asserted at every listed greatest transit for
  the geocentric observer. (From Oslo the grazing transit of
  1937-05-11, minimum separation 955.5″, is *near inferior conjunction*
  rather than *in front*: parallax lifts the centre outside the limb
  sum. The table states what the observer sees.)
- **Horizons' named instants** at the five observers and
  geocentrically: the interval's start and end, the record's
  boundaries (1962, 1972, 2017), J2000.0, the exact interval's end
  2027-06-28, every almanac event and every transit; and the four
  representative appearances of 2026 - gibbous evening 2026-02-16,
  crescent evening 02-23, crescent morning 03-23, gibbous morning
  04-20 - each the weekly Oslo row nearest a quarter or three quarters
  lit on its side of the 2026-03-07 inferior conjunction.
- **The fixture boundary, as fetched under the owner's permission**:
  committed (`horizons/`, 55 files, **3.89 MB**, indexed with their
  digests in `horizons/README.md` by `scripts/mercury-fixture-index.py`)
  - the three named lists and the appearances for all five observers
  and geocentrically (Mercury and Sun), the hourly 2026-03-07 window
  and the six-hourly 2026-04-03 window at Oslo, the 2026 apparitions
  every 3 days at Oslo (Mercury and Sun), the 7-day 1900–2100 matrix at
  Oslo. Retained outside Git with URL and digest (`horizons-kept.md`):
  the geocentric 7-day matrix (2.83 MB). **The committed set is
  0.39 MB over the 3.5 MB the owner approved** (three lists where Venus
  had one); the owner is asked on #528 whether to keep it whole or
  retain the four non-Oslo observers' transit lists.

## The reader's table, proposed

`docs/studies/mercury/controls-mercury-*.png`, in both languages and
the dark appearance, from the kept Horizons rows (`measurements.md`):

- **Mercury joins the Sun's, the Moon's and Venus's table family**
  (`SolarTableSession`, the 1 000-row cap, the same refusals read from
  the released strings, Update from Place and Time, nothing calculated
  on opening or expanding, nothing persisted). Its Controller section
  sits **between the Sun and Venus** - Solar System sections are
  ordered outward from the Sun (#527).
- **The instant card**, the eleven columns Venus has: right ascension
  and declination (J2000), altitude, azimuth, distance in AU and
  million km, apparent diameter, elongation with E or W and the sky it
  means, phase angle, illuminated fraction, the phase word and the lit
  side (χ of date with the nearest of sixteen points, or *not
  well-defined*). The mock-up's card is the 2026-02-19 greatest
  eastern elongation: 18.1° E, evening sky, 7.2″, 49.7 % lit, *near
  half lit*, lit side 243° (west-southwest).
- **The range**: the mock-up's 2026 evening and morning apparitions at
  3-day steps - Mercury's apparitions last weeks where Venus's last
  months - show the disc growing from 4.8″ to 10.9″ while the lit
  fraction falls from 99 % to 1 % and the side changes.
- **No surface is implied**: the diagrams draw a flat lit shape on a
  dark disc, and the words speak only of light and size.

**The phase words** (`measurements.md`), on the Moon's and Venus's
ruled half-open bands, so the three bodies change words at the same
fractions:

| k | English | Norsk |
|---|---|---|
| < 2 % | near inferior conjunction | nær nedre konjunksjon |
| 2–48 % | crescent | sigd |
| 48–52 % | near half lit | nær halvt belyst |
| 52–98 % | gibbous | mer enn halvt belyst |
| ≥ 98 % | near full | nær full |
| centre within the Sun's radius plus Mercury's, i > 90° | in front of the Sun | foran Solen |
| the same, i < 90° | behind the Sun | bak Solen |

The two geometric states take precedence; Norwegian uses *Merkur*,
*Solen*, *Ø*/*V* and *Elongasjon fra Solen* as the released tables do.
Every word occurs in the named set, *in front of the Sun* at 27 of the
28 transits from Oslo.

## The cartography handoff

Measured on the atlas's own field ladder (read from `ChartViewState`)
through the production projection, and on the sheets; no Mercury is
drawn on any page (`measurements.md`, *The cartography handoff*):

- **Diameter**: 4.64″ (2034-12-04) to 12.26″ (1936-06-01) at Oslo over
  1900–2100 - a fifth of Venus's range. On the 900 × 700 page
  **1.16–3.06 px at 1°**, 0.58–1.53 px at 2°, under a pixel from 4°
  wide; on the A4 sheet's chart area 0.35–0.93 mm at 1° (10.9 px at
  300 dpi), on Letter 0.33–0.87 mm.
- **The phase at 1°.** On the study's proposed criterion (a disc at
  least 6 px, its lit and dark parts each at least 1 px) the phase
  reads on the screen page in **none of the weeks at any field** - the
  disc never reaches 6 px - and on A4 at 300 dpi in **30.3 %** of the
  weeks at 1°, none at 2°. Every one of the 75 named appearances is
  unreadable on screen; at the 2026-02-19 elongation the half-lit disc
  is 1.79 px.
- **So Mercury is the #481 case, measured.** Venus could be drawn
  honestly at 1° and read in a fifth of the weeks; Mercury can be
  drawn honestly - a true-size disc of 1–3 px, or rule M's phased 6 px
  mark with the phase in the table - but its phase can never be read
  on screen at the ordinary ladder. A field four times narrower would
  scale the measured 1° figures four times - 4.6–12.2 px, the 2026-02-19
  half-lit disc 7.2 px - which the criterion would call readable; that
  is arithmetic on the measurement, not a page drawn. The study
  recommends deciding #481 in Part II from this measurement and
  Venus's together, not here.
- **Phase geometry for the J2000 chart**: as Venus's - the lit shape is
  the bright half-disc with the terminator's half-ellipse of semi-axis
  |1 − 2k|·r, turned to χ computed of date and rotated into the J2000
  chart's local north at Mercury's projected place.
- **Identity and time**: Mercury's identity on the page is `mercury`;
  a selected Mercury is an occurrence, as the Sun, the Moon, Jupiter
  and Venus are since #513.

## For the rulings

1. **Authority**: DE440 from the pinned `de440s.bsp`; the PCK radii;
   Horizons for comparison; Espenak's almanacs and transit catalogue
   for named timing, each under its own terms; DE442 not adopted.
2. **Pack and interval**: layout C as ruled, the `mercury` kernel of
   3.30 MB beside the `venus` one in the `planets` pack at version 2,
   1900–2100; every released file byte-identical.
3. **Meanings**: as proposed - topocentric; deflected apparent place;
   the side by right ascension; S-T-O and k as the Moon's; χ of date;
   the equatorial diameter.
4. **The radius**: the equatorial 2440.53 km, the PCK's and Horizons',
   the triaxial figure stated once - or the IAU mean 2439.4 km, 0.05 %
   smaller, which Horizons does not use.
5. **Phase vocabulary**: the words and bands above, both languages,
   *Merkur* in Norwegian.
6. **Fixtures**: the committed and retained sets as fetched, with the
   committed-size question; the Espenak assertions as proposed -
   conjunctions within ±1 hour, elongations to 0.1° within ±2 hours,
   transits within ±5 minutes and 0.2″, *in front of the Sun* at every
   geocentric greatest transit.
7. **Targets**: the table above.
8. **Cartography handoff**: the readability criterion, Mercury as the
   measured #481 case, the identity/time contract; no production
   Mercury ink before Part II.

## The ruling

The owner's rulings (2026-10-14, on #528 at 8abe1295), accepting all
eight proposals, recorded as given:

1. **Authority**: DE440 from the already pinned source; PCK constants;
   Horizons for comparison; Espenak's almanacs and NASA/GSFC transit
   catalogue for named timing. DE442 remains comparison-only.
2. **Pack**: layout C, planets-pack version 2, one 1900–2100 Mercury
   kernel beside Venus's, with every released file byte-identical.
3. **Meanings**: the proposed topocentric quantities, apparent-place
   corrections, right-ascension side, Moon-family phase geometry, and χ
   of date.
4. **Radius**: the 2440.53 km equatorial radius used by the PCK and
   Horizons. The triaxial figure stated once; the mean radius not
   substituted.
5. **Vocabulary**: the shared phase bands and both languages, with
   *Merkur* in Norwegian.
6. **Fixtures**: the complete 3.89 MB committed set kept - the 0.39 MB
   overage approved; retaining all observer transit lists is worth more
   than trimming 0.14 MB while still exceeding the original estimate -
   and the geocentric matrix outside Git. The proposed assertions
   accepted: conjunction within ±1 hour; elongation value and side to
   0.1°, instant within ±2 hours; greatest transit within ±5 minutes and
   minimum separation within 0.2″; geocentric Mercury in front of the
   Sun at every listed greatest transit. *Amended on #529 (2026-10-14)
   for one named definition-boundary case:* the atlas's ruled PCK and
   Horizons radii and its exact limb-overlap definition stand. At the
   geocentric greatest transit of 1937-05-11 Mercury's centre is
   0.017″ outside the atlas's limb sum, so the atlas correctly says
   *near inferior conjunction*, not *in front of the Sun*; the transit
   catalogue reaches overlap because it uses a larger solar radius,
   and that difference is stated explicitly - the Sun is not enlarged
   and no tolerance is added merely to reproduce the catalogue's
   category. *In front of the Sun* is required at the other 27 of the
   28 greatest-transit rows; the 1937 row is held by name to its
   measured signed margin and state; any second exception stops the
   build and returns for a ruling.
7. **Targets**: the complete proposed table, without widening.
8. **Cartography handoff**: the readability measure and Mercury as the
   concrete #481 case; closer zoom kept for Part II; no production
   Mercury ink now.

Nothing in the proposal was amended when ruled; ruling 6 carries the
one amendment above, made on #529 when the service measured the 1937
case. #529 (the pack addition and `MercuryService`), #530 (the table)
and #531 (the handoff to cartography) build on these rulings.

## #529, as built

**The pack, as ruled (layout C, version 2).** `src/resources/planets/`
now carries `juranometria-de440-mercury-1900-2100.bsp` beside Venus's
file: 3 299 328 bytes, sha256 `78736932…` - byte for byte the file the
#528 study cut (`pack-measurements.md`) - with segments 0 → 1 and
1 → 199 from the released pack's pinned `de440s.bsp`, every state on a
1.7-day grid identical to the source, 1 → 199 identically zero.
Venus's file is untouched: 1 242 112 bytes, sha256 `8b0760b4…`, the
5.2.0 file (`PlanetsPackTest`). The builder (`PlanetsPackMain`) now
gives each planet the pack version it joined in - Venus 1, Mercury 2 -
and writes that version into the planet's own file's internal name and
comment, so raising the pack's version never rewrites a released
planet's file; the manifest says `pack.version=2`,
`pack.planets=venus,mercury`, `ephemeris.mercury.*`,
`ephemeris.<planet>.added.in.version`, `pck.BODY199_RADII =
2440.53,2440.53,2438.26` verbatim; the notice names Mercury's file,
its version and the triaxial figure once, with the equatorial radius
the atlas uses; the provenance proves both planets about their
barycentres and the released Solar System and Jovian kernels
untouched. The audit date is the inventory's, 2026-10-13.

**The service.** `MercuryService` is `VenusService`'s form over
Mercury's bodies 1 and 199 and the pack's equatorial radius: the ruled
meanings (ruling 3) - astrometric J2000; apparent of date deflected
by the Sun (the one-body formula Jupiter's and Venus's services apply,
held equal to both by `MercuryServiceTest`) and aberrated; airless
horizontal; distance; the apparent diameter from 2440.53 km; the
Moon's S-T-O and k; elongation and the side of the Sun by apparent
right ascension of date; the phase word with the geometric states
first, the bands Venus's name for name over a grid (ruling 5); χ of
date with its conditioning. As with Venus on #501, the service stands
alone in this issue: `SolarSystemService.Body` gains no entry and no
sealed permit changes, so no switch in the interface is touched -
Venus joined the dispatch with its table on #502, and Mercury joins
with its own on #530. The Sun, the Moon, Jupiter and Venus answer as
before: their services are unchanged, their packs byte-identical, and
Venus's answers held equal before and after Mercury's service is
loaded (`MercuryServiceTest`, `PlanetsPackTest`).

**Measured, against Horizons, every committed fixture** (11 207 rows;
`docs/studies/mercury/service-accuracy.md`,
`make mercury-accuracy-report`, held by `MercuryReferenceVectorTest`):

| quantity | 1900–1961 | 1962–1971 | 1972 → 2027-06-28 | after |
|---|---|---|---|---|
| astrometric | 0.106″ | 0.025″ | **0.000″** | 11.858″ |
| apparent, elongation > 1° | 0.177″ | 0.083″ | **0.129″** | 11.938″ |
| horizontal | 1.024″ | 1.992″ | 11.476″ | 10.886″ (not asserted) |
| distance | 56.2 km | 15.0 km | **0.2 km** | 6 495.0 km |
| diameter | 0.0000″ | 0.0000″ | 0.0000″ | 0.0004″ |
| elongation, > 1° | 0.241″ | 0.187″ | 0.180″ | 11.930″ |
| phase angle (S-T-O), > 1° | 0.419″ | 0.198″ | 0.181″ | 38.326″ |
| illuminated, points | 0.0048 | 0.0048 | 0.0048 | 0.0119 |
| χ, k in [2 %, 98 %) | 0.057″ | 0.011″ | 0.051″ | 5.334″ |

**Every value is inside its ruled target (ruling 7); no target is
amended.** The exact interval's apparent place is 0.129″ against the
stand-in's 0.054″ for the cause Venus's record names - the atlas's IAU
1976/1980 precession and nutation against the stand-in's IAU 2000A -
and stays within the ruled 0.2″; the exact astrometric place rounds to
0.000″ at three decimals and is not zero (the engine compares two
implementations). The side of the Sun agrees with Horizons' /T and /L
on all 11 207 rows; Mercury's parallax reaches 15.88″; every phase
word occurs. Espenak's rows hold under the ruled assertions
(`MercuryNamedEventsTest`): the eighteen conjunctions −16 to +37
minutes from the listed hour, on both sides of it; the eighteen
greatest elongations at the printed tenth and side, −26 to +34 minutes
from the listed hour, where the elongation is within 0.126″ of its
maximum; the two occultations are named instants only. The 28
greatest transits fall −0.5 to +0.7 minutes from the catalogue's
minute with the minimum separation agreeing to 0.1″ on every row.

**The grazing transit of 1937-05-11, ruled.** Ruling 6 asserted
geocentric Mercury *in front of the Sun* at every listed greatest
transit; the service reads it at 27 of the 28. At the 1937 greatest
transit the centres are 955.546″ apart and the limb sum - the released
Sun's 695 700 km and Mercury's 2440.53 km - is 955.529″: the centre is
outside it by a signed margin of +0.017″, so the ruled word (centre
within the Sun's radius plus Mercury's) is *near inferior conjunction*
there. The catalogue lists first and fourth contacts (08:53, 09:06)
and no second or third because it uses a larger solar radius: a
grazing contact decided by the solar radius, not by the ephemeris (the
separation agrees with the published 955.5″). The owner amended ruling
6 for this one named definition-boundary case (above): the atlas's
radii and its exact overlap definition stand, the Sun is not enlarged
and no tolerance is added to reproduce the catalogue's category; the
other 27 rows are required *in front*; the 1937 row is held by name to
its measured signed margin and state; a second exception stops the
build. `MercuryNamedEventsTest` holds exactly that: the margin at the
greatest transit, printed per row in `service-accuracy.md`, is +0.017″
at 1937 within ±0.005″ and negative at every other row. The study had already noted the Oslo
row reading *near inferior conjunction* for the same geometry.

**The mutation rivals** (`MercuryMutationTest`) - a geocentric place
(a dozen arcseconds out near inferior conjunction), the civil instant
read as TT (several arcseconds at superior conjunction), no aberration
(about 20″), the geometric phase angle (12″ at Mercury's 49 km/s), a
J2000 χ (a century of precession), a side by ecliptic longitude (west
by right ascension fifteen hours before the longitude conjunction),
the mean radius (0.05 % past the 0.001″ target) - each fail their
target. One is measured, not claimed: the Sun's deflection. Mercury's
committed rows never come within 1.6° of the Sun's centre outside a
transit, where the one-body deflection is a few hundredths of an
arcsecond, so the rival without it survives the ruled 0.2″ target on
every committed row - unlike Venus's at 0.7°. The step is ruled and
applied; the test holds that it is applied and brings the place
closer to Horizons' own (0.065″ with it, 0.089″ without, at the 2012
superior conjunction).

**The first Mercury table** is `docs/studies/mercury/table-measurements.md`
(`make mercury-table-study`): the five observers, the date-line one
included, at Espenak's 38 almanac rows and the 28 greatest transits,
the four representative appearances, a chosen day and both ends of the
interval; Oslo every 3 days through the 2026 evening and morning
apparitions and hourly through the 2019 transit - in the Moon table's
columns and rounding, the phase words as ruled, in English until #530
carries them in the language files. From Cape Town the 1937 row reads
*in front of the Sun* and from Oslo *near inferior conjunction*: the
table states what each observer sees.

**Packaged.** The licensing inventory is unchanged at 12 resources -
Mercury's file joins the pack under the notice it already has - and
the archive, `LICENSING.md` and the About dialog's notices are
unchanged; the notice's text now names Mercury. The packaged
acceptance's *planets pack OK* line checks Mercury at Oslo against a
Horizons row the image restates (held to the committed row by
`PackagedMercurySampleTest`), the 2026 greatest eastern elongation
near half lit in the evening sky, and the 2019 transit in front of the
Sun, beside Venus's checks. No dialog, Controller section, selection
or chart mark exists.

## #530, the table, as built

Mercury joins the Sun's, the Moon's and Venus's table family, as ruled:
one more `SolarTable` (`SolarTable.mercury()`, stem `mercurytable`,
prefix `mercury`) over the same shell, session, refusals and range cap.
The service seam is the one Venus took on #502: Mercury is added to the
Solar System service - `Body.MERCURY`, answered by `MercuryService`
over the planets pack, which `SolarSystemService` builds the first time
Mercury is asked for, so the Sun's and the Moon's answers never read
the planets pack and a missing one refuses Mercury alone, as it refuses
Venus. `MercuryObservation` joins the sealed `Observation` family, and
every switch over it names Mercury; the one Centre on chart switch
refuses Mercury by name, since nothing of Mercury is drawn.

- **Columns**: Venus's eleven - instant; right ascension and declination
  (J2000); altitude; azimuth; distance in AU and million km; the solid
  body's apparent diameter, its explanation naming the equatorial
  2440.53 km, the triaxial figure once, and the mean never substituted
  (ruling 4); illuminated; phase, in the ruled words with the geometric
  states first, its explanation saying the bands are Venus's (ruling
  5); elongation with E or W by right ascension, announcing no
  conjunction (ruling 3); lit side, χ of date with the nearest of
  sixteen points, *not well-defined (near inferior conjunction)* below
  2 % and *(near full)* from 98 %.
- **Hosts**: View > Solar System > Mercury... between Sun... and
  Venus...; the Controller's Mercury group between the Sun's and
  Venus's, introduced collapsed - the order outward from the Sun (#527).
  One shared session, each host's draft its own; building, opening,
  expanding, choosing the Solar System tab or restoring a window
  computes nothing; Update from Place and Time and Compute apply. No
  Show on chart and no Centre on chart: no Mercury is drawn, and the
  menu's "on the chart" items are unchanged.
- **Words**: `mercurytable.*`, `menu.mercury.*` and
  `solarsystem.mercury` in both languages - *Merkur*, *Solen*, *Ø*/*V*,
  *Elongasjon fra Solen* as the released tables say them; the phase
  words are Venus's, word for word, held equal by `MercuryTableTest`.
  The access letter is R in both languages (`MenuMnemonicTest`). The
  time note states the measured after-2027 cost: below 15″ through
  2100 (the ruled after-interval target, the service measured at
  11.9″).
- **Evidence**: `MercuryTableTest` (the named cases in both languages
  - the 2026 greatest eastern elongation, the 2019 transit, the 2026
  superior conjunction - the range through the 2026 inferior
  conjunction with the side changing once and its refusals, the
  date-line observer, two hosts over one session, no chart layer); the
  Controller journey, the menu order and the menu help in both
  languages; the packaged journey *mercury table OK*; the Mercury table
  photographed in both languages (`mercurytable-*`, a registered
  photographer, Venus's twin), and the menu and Controller sheets
  retaken with Mercury in them. The Sun's, the Moon's, Venus's and
  Jupiter's tables are unchanged: their tests and sheets stand.

## Handed over (#531)

Part I of the road to 5.3.0 closed on 2026-10-15 at main `0ae4afa0`.
The handover to cartography - what is settled, what was measured and
why, every stopped or incomplete run, what moved, and what Part II
must decide - is `mercury-handover.md`. This record stays the contract.
