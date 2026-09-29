# Knowing where the Moon is

**Sprint 36, issues #406 and #407.** The science and delivery contract
for the atlas's second Solar System computation, as the owner ruled it
on #406 (M1–M9) after the measured checkpoint, and as
`juranometria.solar` implements it beside the Sun's contract
(`sun-computation.md`), which it inherits wherever it does not say
otherwise. Measured by `make moon-study` (the first table) and held by
`MoonReferenceVectorTest` against 52 870 rows of JPL Horizons; the
numbers quoted here are those tests' and the #406 study's
(`docs/studies/solar-system/moon-contract/`).

## The question

The road to 4.0 (#397) gives the Moon the same numerical treatment the
Sun received before any of it is drawn: an authority, frames with
exact meanings, a phase vocabulary that means one thing, an accuracy
measured rather than asserted, and a table a reader can use - offline,
permanently.

## The authority: the same pack, regenerated once with the Moon (M4)

The Solar System pack is **v2**: the JUranometria-modified DE440
excerpt now keeps four SPK Type 2 segments - the Sun and the Earth–Moon
barycentre from the solar-system barycentre, the Earth and **the Moon**
from the Earth–Moon barycentre - over 1900–2100 with the same 31-day
margin, every coefficient the source's. The kernel is renamed
`juranometria-de440-sun-emb-earth-moon-1900-2100.bsp`, its internal
name and comment area say what it now carries, and the notice,
provenance and manifest are rebuilt from the same digest-pinned
`de440s.bsp` by the same builder, which proves 172 740 states at
1.7-day spacing identical to the source before writing.

| | bytes |
|---|---|
| pack v1, Sun + EMB + Earth | 8 884 224 |
| **pack v2, with the Moon** | **14 910 464** |

The #406 checkpoint measured 14 849 024 bytes for the same segments with
a one-record comment area; the released pack keeps the source kernel's
full comment area as v1 does, 60 records more. Nothing else differs.

**The Sun is unchanged, bit for bit.** Before regeneration,
`SolarSystemInvarianceMain` wrote the raw states of the three released
segments at 4 001 epochs each and the Sun's every quantity for the five
reference observers at the thirteen named instants, at full precision,
from pack v1 (`docs/studies/solar-system/sun-invariance.txt`, never
regenerated). `SunInvarianceTest` holds pack v2 to every number
exactly. The jplephem reference states (`spk-reference.txt`) were
regenerated on the v2 kernel: every v1 row is reproduced unchanged and
36 Moon rows are added.

## Frames, with their meanings frozen (M1, M2)

- **Chart position, apparent of date, horizontal, distance** - exactly
  as the Sun's: topocentric astrometric ICRS/J2000 with light-time and
  no aberration; aberrated and precessed–nutated to the true equator
  and equinox of date; airless altitude and azimuth for the geodetic
  place at sea level with UT1 = UTC; observer to the
  light-time-corrected centre, in kilometres.
- **Apparent diameter** - 2·asin(1 737.4 km / distance): the IAU mean
  lunar radius, Horizons' own convention (its angular diameter
  back-derives to it exactly). A spherical mean-radius convention for
  the table and a later untextured disc; not a topographic limb.
- **Parallax is not optional.** The topocentric–geocentric difference
  reaches 1.024°; a geocentric Moon is two diameters wrong.
- **Phase angle i** - at the Moon, Horizons' S-T-O. Measured, not
  assumed: Horizons publishes two phase angles, the geometric "phi"
  and S-T-O, which differ by up to ±21″; S-T-O is the angle between
  the *apparent* Sun as the Moon sees it - light-time corrected and
  aberrated by the Moon's own barycentric motion - and the down-leg to
  the observer *as the observer sees it*, the aberrated direction
  reversed. With both aberrations the service reproduces S-T-O to
  0.33″ through the exact interval; with neither it is 44″ off, with
  one 22″. The checkpoint's guess that the 43″ was an epoch difference
  was wrong; the measurement says what it is.
- **Illuminated fraction** k = (1 + cos i) / 2, from that angle:
  reproduces Horizons' Illu% to 0.003 percentage points. It is what
  each observer sees: parallax turns the phase angle by up to a degree,
  which near a quarter is under a percentage point of k.
- **Elongation ψ** - at the observer, unsigned, between the apparent
  (aberrated) Sun and Moon: Horizons' S-O-T to 0.35″.
- **Side** - which side of the Sun the Moon is on in *this observer's*
  sky, by apparent right ascension of date, Moon minus Sun wrapped to
  (−180°, 180°]: positive is **east of the Sun** (the evening sky,
  trailing the Sun; Horizons' /T), negative **west** (morning, leading;
  /L). Agrees with Horizons on 52 868 of 52 870 rows; the two
  disagreements lie within 0.003° of opposition in right ascension,
  where the definition has no side.
- **Waxing or waning** - a global classification, kept apart from the
  side as ruled: the geocentric, light-time-corrected, un-aberrated
  elongation in ecliptic longitude, Moon minus Sun **on the atlas's
  J2000 ecliptic**, wrapped to [0°, 360°); **waxing below 180°, waning
  from 180°**. The same for every observer at the same instant (held
  across 41 952 observer pairs); the observer-specific side differs
  from it at 87 of them, all within 12° of conjunction or opposition,
  which is exactly the flip the ruling forbids the phase word to make.
- **Bright-limb position angle χ** - Meeus 48.5 from the topocentric
  apparent-of-date Sun and Moon, from celestial north through east,
  [0°, 360°). Held to Meeus 48.5 applied to Horizons' own apparent Sun
  and Moon on Oslo's grid: 0.027° worst over 8 648 rows clear of new
  and full. From J2000 inputs instead it turns by up to 0.6°, which is
  why the input frame is part of the definition. The later fixed-J2000
  drawing rotates this χ into the chart's north basis and does not
  recompute it; that transformation is held by its own contract when
  rendering begins, not here.
- **Conditioning.** χ is carried always and stated as *not usefully
  defined* when the phase is near new or near full (below, M5/M6): the
  same two thresholds decide both, so the word and the angle can never
  disagree.

## Phase words and bands (M5)

From k and the trend, half-open bands: k < 2 % **near new Moon**
(*nær nymåne*); 2–48 % waxing **waxing crescent** (*tiltagende
månesigd*); 48–52 % waxing **near first quarter** (*nær første
kvarter*); 52–98 % waxing **waxing gibbous** (*tiltagende måne, mer enn
halv*); k ≥ 98 % **near full Moon** (*nær fullmåne*); 52–98 % waning
**waning gibbous** (*avtagende måne, mer enn halv*); 48–52 % waning
**near last quarter** (*nær siste kvarter*); 2–48 % waning **waning
crescent** (*avtagende månesigd*). Norwegian uses *tiltagende* and
*avtagende*, never *minkende*; *i ny / i ne* is prose, not a label. A
band is a visual category, not an event: an exact new Moon or quarter
belongs to the cited named-event fixtures, never inferred from k.

## The lit side (M6)

The table shows the angle and the nearest of sixteen compass points,
`285° (west-northwest)` / `285° (vest-nordvest)`, never "left" or
"right", which change with the page and the local vertical; the
explanation states *position angle of the bright limb's midpoint, from
celestial north through east*. Near new and full it shows a localized
*not well-defined near new/full Moon* instead. The English words are
in the study; the Norwegian ones enter with the reader's table (#408).

## Libration (M3): none in 4.0.0

Measured over the matrix, the sub-observer point ranges ±7.75° in
latitude and about ±8° in longitude. It affects no quantity above and
has no reader-facing meaning without a rendered surface; it is deferred
until a textured or feature-bearing surface is proposed. A plain
illuminated disc needs none.

## Time, and the observer

Inherited whole from the Sun (`TimeScales`, `TimeRange`, `Observer`,
the confidence vocabulary): the same civil interval, the same exact
boundary from the pinned record, the same *est.* marks, the same range
with the same refusals (M7). Two inherited conventions cost the Moon
more than they cost the Sun, because the Moon moves 0.55″ a second and
its parallax is a degree:

- **UT1 = UTC** through the station's position: up to 0.9 s of Earth
  rotation moves an equatorial observer 420 m, which is 0.24″ of the
  Moon's direction. Measured: 0.20″ at Quito in the exact interval,
  0.03″ at Alert (cos φ = 0.13).
- **The Espenak–Meeus ΔT before 1972**, against the record: about 1.3 s
  in the 1900s, which is 0.74″ of direction; 0.23″ in 1962–1971.
- **Sea level.** Place and Time carries no height. The first Moon fetch,
  made with the Sun's site heights, measured Quito's 2.85 km as 1.65″
  of parallax, 2.86 km of distance and 0.016″ of diameter; the Moon
  fixtures were refetched at sea level, where the contract's observer
  stands, and the cost of a real height is recorded here rather than
  modelled.

## Accuracy, measured

Against JPL Horizons (DE441; an independent implementation in the same
authority family) for the five observers at sea level, every 7 days
across 1900–2100 - a 30-day step would alias the 29.5-day lunation -
the named instants and a daily 2026 series at Oslo, 52 870 rows:

| era | rows | astrometric | apparent | horizontal | distance | diameter | illuminated | elongation | phase angle |
|---|---|---|---|---|---|---|---|---|---|
| 1900–1961 | 16 180 | 0.74″ | 0.80″ | 1.4″ | 0.10 km | 0.0009″ | 0.003 % | 0.84″ | 0.85″ |
| 1962–1971 | 2 615 | 0.23″ | 0.17″ | 2.5″ | 0.06 km | 0.0007″ | 0.003 % | 0.38″ | 0.38″ |
| **1972 → 2027-06-28** | 14 890 | **0.20″** | **0.24″** | **12.2″** | **0.34 km** | **0.0022″** | **0.003 %** | **0.35″** | **0.33″** |
| after 2027-06-28 | 19 185 | 86.7″ | 86.8″ | 85.8″ (not asserted) | 11.9 km | 0.061″ | 0.019 % | 79″ | 79″ |

**The targets, as ruled (M9), and what the measurement says.** Met:
apparent ≤ 1″, horizontal ≤ 20″, distance ≤ 1 km, diameter ≤ 0.01″,
illuminated ≤ 0.01 point, elongation ≤ 1″, phase angle ≤ 5″ through
the exact interval; astrometric ≤ 0.5″ in 1962–1971; χ ≤ 0.1°; after
the interval, direction within the 90″ budget (86.7″, the DE440/DE441
lunar divergence of 6.9″ plus the ΔT models' 133 s at 0.55″/s) and
distance within the 25 km budget (11.9 km). **Exceeded, and returned to
the owner with their causes rather than widened** (checkpoint on #407):
astrometric 0.74″ in 1900–1961 against ≤ 0.1″ (the accepted ΔT model
against the record, 0.55″ per second); astrometric 0.20″ from 1972
against ≤ 0.1″ (UT1 = UTC through the station, bounded at 0.24″);
diameter 0.061″ and illuminated fraction 0.019 points after the
interval against 0.01 (the 11.9 km of DE441 divergence at 0.005″ per
km; the 133 s of ΔT at 0.5″ per second on the phase angle). None is an
implementation residual: each scales with cos φ, with the era's ΔT, or
with the families' own divergence, and each was decomposed before it
was named.

**A published case outside the JPL family** (`MoonPublishedCaseTest`):
Meeus 47.a and 48.a, 1992 April 12.0 TD, geocentric. Apparent α, δ
within the book's truncated series (measured 2″), distance within
50 km (30 km), phase angle within the two aberrations S-T-O carries
and the book does not, k to the book's four decimals, χ to its tenth of
a degree. Horizons at the same TT instant agrees with the service to
the exact-era targets.

**The mutations the oracle catches** (`MoonMutationTest`): a
geocentric place (over half a degree), the civil instant read as TT
(38″), aberration left out (20″ at the first leap second; on the
sample morning the Moon sits 5° from the anti-apex and aberration
nearly vanishes, which the test records), the geometric phase angle
against S-T-O (up to 42″, past 5″ at most instants), χ from J2000
inputs (past 0.1°), and an observer's side taken for the trend (wrong
rows within 12° of conjunction or opposition).

**The named events** (`MoonNamedEventsTest`): Espenak's 2026 catalogues
at minute precision (M8) - the June lunation, the late-January perigee
and apogee the checkpoint quoted, and the year's nearest perigee and
farthest apogee - from `docs/studies/solar-system/moon-events-2026.txt`
with URLs, retrieval date and digests. At those instants the atlas's
own answer is what the name says: near new at conjunction in longitude
within 0.02°, the quarters half lit within 0.2 points at 90° and 270°,
near full at 180°, the perigees and apogees the extreme against the
neighbouring hours. The catalogues' distances sit tens of kilometres
from DE440's (+16 km, −40 km): the book's perigee series against the
numerical ephemeris, which is why they name and do not judge.

## The first table

Instant (UTC); right ascension and declination (J2000); altitude (no
refraction) and azimuth (from north through east); distance to the
kilometre, `384 400 km`; apparent diameter to 0.1″; illuminated to
0.1 %, `67.9 %`; phase, in words; elongation from the Sun to 0.1° with
the side's letter, `84.8° E` (Norwegian `Ø`/`V`); lit side, `285°
(west-northwest)` or *not well-defined*. Ecliptic longitude is not
shown: the Moon sits up to 5° off the ecliptic and the number adds
nothing a reader uses. `docs/studies/solar-system/moon-measurements.md`
is the table as numbers, four observers at the named events and chosen
instants, a daily range through one lunation and an hourly night.

## Fixtures

`docs/studies/solar-system/horizons-moon/` - thirteen Horizons
responses kept whole with request URL, time and body digest, fetched by
`scripts/horizons-moon-fetch.py`: five named, five 7-day matrices
(3.0 MB each; the 7-day step is what keeps the lunation from
aliasing), the daily year, the geocentric published case and the Sun on
Oslo's grid for χ. `MoonReferenceVectorTest` re-hashes each. The
comparison that preceded the ruling is `scripts/moon-authority-comparison.py`
with the study directory's own README.

## What the implementation issues inherit

- `juranometria.solar` stays removable and toolkit-free
  (`SolarSystemBoundaryTest`); Place and Time learns nothing of the
  Moon (`PlaceAndTimeGateTest`); the chart core and renderer refer to
  nothing under it.
- `SolarSystemService.observe(Body.MOON, observer)` answers a
  `MoonObservation` with units, frames, the side, the trend, the
  phase, χ and its conditioning in its types; the range is the Sun's.
- The pack loads through its manifest and refuses corruption
  (`SolarSystemPackTest`, four segments, bodies `3,10,301,399`,
  `pack.version=2`); the packaged image computes the Moon for Oslo's
  sample morning within 0.3″ of Horizons' sea-level answer and finds
  it a waxing crescent (`PackagedAcceptanceMain`).
- The issue #410 wording repair rides with this pack, as ruled: the
  Sun table's longitude explanation now says *measured from the fixed
  J2000 equinox … near, not exactly at* in both languages, held by
  `SunTableWordingTest`, with the decision prose and the interface
  evidence re-recorded.
