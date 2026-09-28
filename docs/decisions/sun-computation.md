# Knowing where the Sun is

**Sprint 35, issues #398 and #399.** The science and delivery contract
for the atlas's first Solar System computation, as the owner ruled it
on #398 after two evidence checkpoints, and as `juranometria.solar`
implements it. Measured by `make sun-study` (the first table) and held
by `SunReferenceVectorTest` against 12 670 rows of JPL Horizons; the
numbers quoted here are those tests' and the #398 study's.

## The question

The road to 4.0 (#397) makes the Sun and Moon trustworthy numbers
before it makes them chart objects. Numbers need an authority, a time
scale, frames with exact meanings, an accuracy that is measured rather
than asserted, and a table a reader can use - and all of it offline,
because the atlas promises to work without a network, permanently.

## The authority: JPL DE440, as a modified kernel

**JPL's Development Ephemeris DE440** (Park, Folkner, Williams and
Boggs 2021) is the authority, chosen because one file gives the Sun,
the Moon and later the planets one coherent foundation. The atlas
bundles a **JUranometria-modified excerpt** of NAIF's official
`de440s.bsp`: three SPK Type 2 segments - the Sun and the Earth–Moon
barycentre relative to the solar-system barycentre, and the Earth
relative to the Earth–Moon barycentre - over 1900–2100 with a
31-day margin at each end. Every Chebyshev coefficient is the
source's, unchanged; only whole records outside the interval were
removed, and the builder proves it before writing: 129 555 states at
1.7-day spacing, position and velocity identical to the source, worst
difference 0.0.

| | bytes |
|---|---|
| `de440s.bsp`, unmodified, 1849–2150, 14 segments (not bundled) | 32 726 016 |
| **the bundled excerpt, Sun + EMB + Earth, 1900–2100** | **8 884 224** |
| the same with the Moon added, for the Moon sprint | ≈ 14.9 MB, one pack regenerated |

NAIF's rules make an unmodified kernel freely redistributable and a
modified one a different thing that must be renamed and re-attributed:
*"If a kernel distributed by NAIF has been modified in any way, any
embedded or otherwise allied attribution of the original kernel
producer must be replaced with the name and institution of whomever
has made the last modification."* The pack does exactly that. The
kernel's own name is `juranometria-de440-sun-emb-earth-1900-2100.bsp`;
its comment area names JUranometria as the modifier, states what was
kept and from where, and then carries the source kernel's own comment
area as source attribution; `NOTICE-solar-system.md` says the same in
prose with the DE440 authors acknowledged; `PROVENANCE.md` records the
source URLs, retrieval date, upstream SHA-256, the exact extraction
tool and interval, the output SHA-256, the validation and the
applicable rules; and `manifest.properties` pins every file's digest,
verified before use. SPICE data carry no fees or licensing
("Technology and Software Publicly Available").

The pack is rebuilt from the digest-pinned official kernel by
`make import-solar-system` after `scripts/download-solar-system-sources.sh`;
rebuilding it writes the same bytes. Nothing at build, test or run
time reaches the network.

### The margin is not optional

A civil instant of 1900-01-01 00:00 lies seconds *before* 1900-01-01
in TDB once ΔT is applied. A kernel cut to the day refuses the first
row; the margin was found by measurement, not caution.

## Time

The atlas's civil instant is UTC, as Place and Time keeps it. What it
means, and how exactly TT follows, is era-dependent, and every answer
says which era it is in (`TimeScales.Confidence`).

| civil dates | the instant is read as | TT | measured cost |
|---|---|---|---|
| 1900-01-01 → 1971-12-31 | **UT1** | UT1 + ΔT, Espenak–Meeus polynomials, unmodified | model − record ≤ 1.09 s (1900–20), 0.42 s (1920–41), 0.68 s (1941–61), 0.10 s (1961–72): ≤ 0.045″ of RA |
| 1972-01-01 → the pinned IERS file's expiry (2027-06-28 in pack v1) | UTC | **exact**: UTC + (TAI−UTC) + 32.184 s | 0 |
| after the expiry | UTC, with UT1 ≈ UTC | **estimated**: TT − UTC ≈ ΔT, the same expressions | E&M runs 6.31 s ahead of the record in 2026 (+0.26″); at 2100 it gives 202.7 s where Horizons holds 69.2 s: 133 s, which is 5.5″ of RA and up to 67 km of distance |

- The reference records were the Stephenson–Morrison–Hohenkerk Table
  S15 (2016, 2020 addendum) to 1961 and IERS EOP C01 from 1962.
- **A measured trap:** reading a pre-1972 civil instant as UTC with a
  leap offset - which one widely used library does by default - costs
  44 s: 1.9″ in RA and 656″ in azimuth at 1900. The contract reads it
  as UT1.
- The pinned file's expiry is a data-validity boundary, not a
  prediction about leap seconds. A later file, pinned and rebuilt
  into the pack, moves the exact boundary; nothing else does, and the
  displayed boundary date is generated from the pack, never written
  as a literal.
- TDB is taken as TT (under 2 ms). UT1 is taken as UTC for Earth
  rotation, Place and Time's standing rule, whose cost is bounded at
  13.5″ on horizontal coordinates and was measured at 11.2″ worst
  across the exact interval. After the present UTC regime changes,
  the future UT1 − UTC is unknown and no numeric horizontal accuracy
  is asserted; Horizons itself holds UT1 = UTC beyond its predictions,
  so the two agree on that convention, and a published extrapolation
  that does not differs from it by 27 s of rotation at 2100.

## Frames, with their meanings frozen

- **Chart position — topocentric astrometric ICRS/J2000.** The
  direction from the observer's position at the instant to the Sun's
  position at the instant less the light-time, iterated; no
  aberration, no deflection (the Sun deflects its own centre by
  nothing). The ephemeris frame is the ICRF, taken as the atlas's
  J2000 sky. This is Horizons' quantity 1 and the position to draw
  among catalogue stars, which are likewise un-aberrated.
- **Apparent of date — computed, tested, not shown.** Astrometric plus
  annual and diurnal aberration from the observer's barycentric
  velocity, then the atlas's own IAU 1976 precession and IAU 1980
  nutation forward to the true equator and equinox of date
  (`SkyFrame.toOfDate`, the exact reverse of the rotation Place and
  Time already ships). Horizons' quantity 2.
- **Horizontal — apparent, airless, topocentric.** Altitude and azimuth
  of the apparent direction, azimuth from north through east, for the
  observer's geodetic place at sea level (WGS 84), through the same
  sidereal time and Earth orientation Place and Time uses. Horizons'
  quantity 4. The drawn horizon is astrometric geometry, so the Sun's
  drawn altitude and the table's differ by the aberration, at most
  20.5″, below the table's 0.01° rounding.
- **Distance** - observer to the light-time-corrected centre.
  **Apparent diameter** - 2·asin(R / distance) with R the IAU 2015
  nominal solar radius, 695 700 km, which is Horizons' convention.
- The Sun's parallax reaches 8.8″, so the station vector is applied:
  a geocentric answer would miss the exact-era target by itself.

## Accuracy, measured

Against JPL Horizons (DE441 there; an independent implementation in
the same authority family, with its own time scales and Earth
orientation), row by row, for five observers - Oslo 59.9° N; Quito
0.2° S; Cape Town 33.9° S; Alert 82.5° N; the Chatham Islands
176.5° W - at the named instants, every 30 days across 1900–2100 and
daily through 2026:

| era | rows | astrometric | apparent of date | horizontal | distance | diameter |
|---|---|---|---|---|---|---|
| **1900 → 2027-06-28, the exact interval** | 8 190 | **0.05″** | **0.14″** | **11.2″** | **2.2 km** | **0.0005″** |
| after 2027-06-28 | 4 480 | 5.8″ | 5.9″ | 5.6″ (not asserted) | 68 km | 0.0013″ |

**The targets the tests hold.** Through the exact interval: astrometric
≤ 0.1″, apparent ≤ 1″, horizontal ≤ 20″, distance ≤ 10 km, diameter
≤ 0.01″ - the ruling's, met with margin. After it, the time model
dominates: the two ΔT conventions' 133 s at 2100 move the Sun 0.041″
per second and change its distance by Earth's radial speed of up to
0.5 km/s. The ruling stated ≤ 5″ and ≤ 15 km there "unless #399's
production implementation measures a larger bound"; it did, and the
tests hold **≤ 6″ and ≤ 100 km**, with the measured 5.8″ and 68 km
recorded beside them. Diameter stays ≤ 0.01″. Horizontal coordinates
after the interval are measured and printed, never asserted. Every
worst case is required to be greater than zero: two implementations,
not one compared with itself.

**A published case outside the JPL family.** Meeus, *Astronomical
Algorithms*, example 25.a (1992 October 13.0 TD, geocentric apparent,
stated accuracy 0.01°): the service lands within that precision -
about 10″ from the book's low-precision answer - and Horizons at the
same TT instant lands within 0.05″ of the service.

**The mutations the oracle catches** (`SunMutationTest`): the civil
instant read as TT (2.8″), precession and nutation skipped (a third
of a degree), aberration left out (20″), a west-positive longitude
(over an hour of hour angle), and a geocentric place (several
arcseconds). Each fails its target by a margin, so the targets are
known to bite.

**The DE440/DE441 separation** - about 12 km and 0.014″ by 2100 - is
not an error in DE440; it is the observed disagreement between two
members of the authority family and serves as a cross-check envelope.

## The first table

Instant (UTC); right ascension and declination (J2000); ecliptic
longitude (J2000, the atlas's own ecliptic); altitude (no refraction)
and azimuth (from north through east); distance; apparent diameter.
Rounding: RA to 0.1 s, Dec to 1″, longitude and altitude and azimuth
to 0.01°, distance to 6 decimals of an AU followed by thousandths of a
million km, diameter to 0.1″. A negative altitude keeps its number and
appends the status: `−12.34° (below the horizon)`. The Norwegian
terms, accepted: Tidspunkt (UTC); Rektascensjon (J2000); Deklinasjon
(J2000); Ekliptisk lengde (J2000); Høyde (uten refraksjon); Asimut (fra
nord via øst); Avstand; Tilsynelatende diameter; `−12,34° (under
horisonten)`.

One line under the table, whose boundary date is generated from the
pinned record: *Dates before 1972 and after the pinned time table use
an estimated clock correction. After 2026, uncertainty in local horizon
coordinates cannot yet be stated precisely. The effect on the J2000
chart position remains below 6″ through 2100 under the adopted model.*
(The ruling's wording said 5″; the measured bound is the one printed.)
Norwegian: *Datoer før 1972 og etter den fastlåste tidstabellen bruker
en beregnet klokkekorreksjon. Etter 2026 kan usikkerheten i lokale
horisontkoordinater ennå ikke angis presist. Virkningen på
J2000-posisjonen på kartet holder seg under 6″ fram til 2100 med den
valgte modellen.* Both are polished and ledger-checked in #400.

`docs/studies/solar-system/measurements.md` is the table as numbers:
four observers at the 2026 solstices and equinoxes, a chosen "today",
a midsummer midnight and both ends of the interval; a daily range with
an appended, marked end; an hourly range through a summer night.

## The range

Inclusive start and end; a positive fixed elapsed-time step on the UTC
timeline, so a daily range does not drift across a civil
daylight-saving change; an end off the grid appended and marked; more
than 1 000 rows refused, never truncated; a backwards range and a step
that cannot advance refused. The request and result are body-agnostic
and encode no path, animation or planet-loop assumption.

## Deferred, by ruling

- **The constellation column.** Two independent implementations
  disagree on 5 of 12 670 rows, every one at a boundary-crossing
  instant, and the atlas has no point-to-constellation lookup; its
  boundaries are polylines. Building one needs its own authority, a
  B1875 transformation and an exact crossing rule. It is not built as
  incidental work, and the first table ships without it.
- **Rise, set, transit and twilight** are event searches with a
  horizon and refraction policy of their own, not positions at an
  instant; a later observing-planning decision.
- **Apparent-of-date RA/Dec, hour angle and the equation of time** are
  computed and tested but not shown, so the first table has one
  coordinate vocabulary.

## What the implementation issues inherit

- `juranometria.solar` is removable: it refers to no toolkit, screen,
  renderer, application, preference store, network, file system or
  clock (`SolarSystemBoundaryTest`), and the chart core, renderer,
  projection, catalogue, geography, search, sky model, meridian and
  ecliptic modules and the Place and Time dialog refer to nothing under
  it. Place and Time remains the owner of the observer and the civil
  instant and learns nothing of how an ephemeris works; time-scale and
  ephemeris data may live only in the Solar System pack
  (`PlaceAndTimeGateTest`).
- The service consumes `sky.Observer` and answers `SunObservation`
  with units and frames in its types; it never reads the system clock.
- The pack loads through its manifest's checksums and refuses
  corruption (`SolarSystemPackTest`); the reader reproduces an
  independent reader's states to rounding and refuses epochs outside
  coverage (`SpkKernelTest`); the packaged image loads the pack and
  computes the Oslo sample within 0.1″ of Horizons
  (`PackagedAcceptanceMain`).
- Introducing the pack was a wide change by ruling, because it changes
  packaging. Calculation-only changes after it may take the narrow
  route when the rendering-neutral gate proves the boundary.

## Structural evidence

- `docs/studies/solar-system/horizons/` - twelve Horizons responses
  kept whole, each with its request URL, time and body digest, fetched
  once by `scripts/horizons-sun-fetch.py`; `SunReferenceVectorTest`
  re-hashes each body against the digest it records.
- `docs/studies/solar-system/spk-reference.txt` - 114 kernel-level
  states from jplephem, by `scripts/spk-reference.py`.
- `scripts/sun-authority-comparison.py` - the #398 comparison of
  Skyfield on the DE440 subset against Horizons, Meeus and the ΔT
  record, whose results are quoted above; it needs a throwaway
  Python environment and the downloads it names, none of which the
  atlas depends on.
