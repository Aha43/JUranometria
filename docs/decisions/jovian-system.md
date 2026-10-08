# Knowing where Jupiter and its four large moons are

**Sprint 44, issue #472.** The numerical contract for Jupiter and Io,
Europa, Ganymede and Callisto, proposed from measurement before any
production data or code exists. **Ruled by the owner (2026-10-07)** -
see [The ruling](#the-ruling) at the end, which governs where it
differs from the proposal. It inherits
the Sun's and the Moon's contracts (`sun-computation.md`,
`moon-computation.md`) wherever it does not say otherwise. The study
is `docs/studies/jovian-system/` (`make jovian-system-study` for the
proposal's mock-ups and diagrams, `make jovian-pack-study` for the
excerpt, `scripts/jovian-authority-comparison.py` for the error
matrix); the numbers quoted here are that study's.

> Jupiter is a little sky. The atlas first proves where Jupiter, Io,
> Europa, Ganymede and Callisto are and whether each moon is actually
> visible; only then may the chart draw the configuration among the
> surrounding stars. (#471)

## The question

Before the road to 6.0 draws anything Jovian, the atlas must answer
five questions a reader asks at a chosen instant - where Jupiter is,
how large it appears, where each Galilean moon lies relative to it,
whether a moon is clear, crossing in front, hidden behind or in
Jupiter's shadow, and what configuration to expect - offline,
permanently, to a measured accuracy, in both languages. This record
settles the authority and its terms, the pack, the frames, the
observables and their signs, the visibility states and their
precedence, the accuracy and its targets, the reader's table, the
named fixtures and the implementation split; it builds nothing.

## The authority, re-verified from the official sources (2026-10-07)

| what | source | retrieved | digest (SHA-256) |
|---|---|---|---|
| the Galilean satellites: **JUP365**, R. A. Jacobson (2021), "The Orbits of the Regular Jovian Satellites and the Orientation of the Pole of Jupiter" (personal communication to Horizons/NAIF) | `https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/jup365.bsp` (1 136 581 632 bytes) | 2026-10-07 | `dbf016c01ba4d022154838000cf3f06962cf958ddc503a366f7fe8f81495c5cb` |
| its comment file, which names the merge (JUP365 with DE440's Jupiter barycentre, Sun, Earth–Moon barycentre and Earth), the pole, the GM values and the 16 m interpolation error | `.../jup365.cmt` (69 594 bytes) | 2026-10-07 | `3c851a4b5d1223e155320e7f63558ebcc81908c8f68fb1c7b43985a69663e08a` |
| the Jupiter barycentre about the solar-system barycentre: **DE440**, the released pack's own source | `.../spk/planets/de440s.bsp`, already pinned by `scripts/download-solar-system-sources.sh` | 2026-09-29 | `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` |
| Jupiter's and the moons' radii and Jupiter's pole: **IAU WGCCRE 2015** as NAIF's `pck00011.tpc` states it | `https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc` (131 226 bytes) | 2026-10-07 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |
| the independent comparison inside the authority family: **JPL Horizons** (jup365 merged, DE441, its own time scales and Earth orientation) | `https://ssd.jpl.nasa.gov/api/horizons.api`, 61 responses kept whole with URL, UTC and digest | 2026-10-07 | per file, `horizons/README.md` |
| a published catalogue outside the JPL family: **IMCCE**, *Phenomena of the Galilean satellites of Jupiter*, from Lainey (2021)'s theory, in TT and rounded to the minute in UT | `https://ftp.imcce.fr/pub/ephem/satel/phenjupiter/` (`ftp_jupiter_Events_UT_2026.txt`, 73 843 bytes; `phenE.2024/2025/2026`; `README`) | 2026-10-07 | `049e6cc3a30fdb964c3a38373cbbf2f84d67ff3eb8a53d2369bc9786ec16462d` (UT 2026) and the others in the study |

JPL's Solar System Dynamics group lists JUP365 as the current ephemeris
for Io, Europa, Ganymede and Callisto (`ssd.jpl.nasa.gov/sats/ephem/`,
read 2026-10-07); NAIF's later `jup347`–`jup349` kernels carry the
irregular satellites, not the Galilean ones. JUP365 is **SPK Type 2**
throughout - the one segment type the atlas's reader and excerpt
writer already handle - with segments 5 → 501, 502, 503, 504, 505,
514, 515, 516, 599 over 1600-01-09 to 2200-01-09 (TDB), and DE440's
0 → 5, 0 → 10, 0 → 3, 3 → 399 beside them; its stated Chebyshev
interpolation error is 16 m, and its frame is J2000 (ICRF).

**Terms.** NAIF's rules (`naif.jpl.nasa.gov/naif/rules.html`, read
2026-10-07): redistribution of unmodified kernels is permitted; a
modified kernel must carry the name and institution of whoever made
the last modification in place of the original attribution and a
changed file name - exactly the practice the released pack follows,
with its MODIFIED-by comment, its own name and its notice. The pack
notice, provenance and manifest for the Jovian excerpt follow the
same builder, and NAIF's PCK asks the same of a modified
`pck00011.tpc` (the atlas copies values, not the file). **IMCCE's**
legal notice (`imcce.fr/mentions-legales`, read 2026-10-07) says its
web and FTP data "may be used as examples to illustrate documents if
the source is clearly identified" and reserves commercial or
professional use of its interactive software for authorisation. This
record therefore proposes to quote **only the event rows the fixtures
need** (the 11 December 2026 case and the named cases) with URL,
retrieval date, digest and attribution, never the catalogue, and
never anything of IMCCE's inside the packaged application. That
reading is a decision item for the owner (below).

## The pack, measured

Cut with the released excerpt writer and sized from the kernel's own
segment directories (`docs/studies/jovian-system/contract/pack-measurements.md`,
`make jovian-pack-study`; the inputs are gitignored downloads, so the
record is kept, not regenerated by the contract):

- **JUP365 keeps two segments per body**, split at JD 2450464.5 TDB
  (1997-01-16). The released reader (`SpkKernel.segment`) and writer
  (`SpkExcerpt.of`) take the first segment of a body; both must select
  by epoch in #473 - a required change, found by measurement, not a
  design choice. *Repaired by #473's first change; see the last
  section.*
- **The excerpt is large.** Io and Europa are fitted at 1.125-day
  intervals with degree-15 polynomials (50 words a record), Ganymede at
  1.125 days with 32 words, Callisto and Jupiter at 2.25 days. Over the
  released interval, with its 31-day margin:

| interval | Io | Europa | Ganymede | Callisto | Jupiter | one kernel | gzip (measured ratio 0.858) |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1900–2100 | 26.1 MB | 26.1 MB | 16.7 MB | 9.1 MB | 13.1 MB | **91.2 MB** | 78.3 MB |
| 1950–2100 | 19.6 | 19.6 | 12.6 | 6.9 | 9.8 | 68.5 MB | 58.8 MB |
| 2000–2100 | 13.1 | 13.1 | 8.4 | 4.6 | 6.6 | 45.9 MB | 39.4 MB |
| 2000–2050 | 6.5 | 6.5 | 4.2 | 2.3 | 3.3 | 22.7 MB | 19.5 MB |
| 2020–2080 | 7.8 | 7.8 | 5.0 | 2.7 | 3.9 | 27.3 MB | 23.4 MB |

  The released pack is 14.9 MB (14.2 MB gzipped); the five self-contained
  downloads are 44–49 MB each. A Galilean excerpt over the whole released
  interval would be six times the pack and nearly double every download.
  The Jupiter barycentre from de440s (0 → 5, 1900–2100) is 482 304 bytes;
  Jupiter alone (599 about 5) 13.1 MB over the interval.
- **Proved bit-identical**: the first half (1899-12-01 to 1997-01-16,
  five segments, 44 025 856 bytes) on 104 345 states; the barycentre
  excerpt on 43 223; the released kernel read back against de440s on
  172 892 - the Sun, Earth–Moon barycentre, Earth and Moon states are
  the released file's own bytes (sha256 `63fbf570…`).
- **Two layouts.** *A*: a second pack beside the released one - the
  released kernel untouched, same digest, plus a Jovian kernel (the
  barycentre and the Galilean segments; 91.7 MB at 1900–2100) with its
  own manifest, notice and provenance; Sun and Moon invariance is then
  byte identity of the released file. *B*: one regenerated Solar System
  pack v3 of fifteen segments (106.6 MB at 1900–2100); the four released
  segments are rewritten from the same coefficients, proved identical by
  the builder and `SunInvarianceTest` as v2 was, but the file's bytes
  change. The study proposes **A**, because it keeps the released Sun
  and Moon bytes literally unchanged and lets the Jovian interval be
  ruled on its own.
- **Refusals**: outside the Jovian interval the service refuses with
  the Sun's words and Jupiter's name; a missing or corrupt Jovian pack
  refuses Jupiter alone and leaves the Sun and the Moon as released
  (held by the pack's manifest digests, as `SolarSystemPackTest` holds
  the released pack).

## Time, and the observer

Inherited whole from the Sun and the Moon: `TimeScales` from the
pinned leap-second record with Espenak–Meeus ΔT outside it, the same
civil interval 1900-01-01 to 2100-12-31 with the same 31-day margin
and the same refusals at both boundaries, `Observer` on the WGS 84
ellipsoid at sea level with UT1 = UTC, the same *est.* confidence
marks after the exact interval. JUP365 covers 1600–2200, so the
excerpt is cut to the released interval and refuses outside it like
the Sun's segments do.

## Frames, with their meanings frozen

- **Jupiter's chart position, apparent of date, horizontal, distance** -
  exactly as the Sun's and the Moon's: topocentric astrometric
  ICRS/J2000 with light-time and no aberration; aberrated and
  precessed–nutated to the true equator and equinox of date; airless
  altitude and azimuth; observer to the light-time-corrected centre of
  Jupiter (599, not the barycentre), in kilometres and AU.
- **Apparent diameter, equatorial and polar** - 2·asin(71 492 km / d)
  and 2·asin(66 854 km / d), the IAU 2015 radii as `pck00011.tpc`
  states them (flattening 0.06487). Horizons' `Ang-diam` is the
  equatorial width; the study reproduces it to 0.0000″ on every row (`contract/authority-comparison.md`).
- **The apparent figure** - the oblate spheroid of those radii, seen
  with the pole direction JUP365 and the PCK state (RA 268.056595°
  − 0.006499°/century, Dec 64.495303° + 0.002413°/century, J2000),
  giving the pole position angle from north through east and the
  sub-observer latitude; the projected polar semi-axis is
  b′ = sqrt(R_p² cos²B + R_e² sin²B) for sub-observer latitude B.
  This is what the disc test uses (below) and what later cartography
  draws; the first table shows the pole angle and the two diameters.
- **A moon's plane-of-sky offset** - from the apparent places of date
  of the moon and of Jupiter, each light-time corrected and aberrated
  for the observer: **X = Δα·cos δ_Jupiter, Y = Δδ, in arcseconds,
  X positive east (right ascension increasing), Y positive north**,
  Horizons' own definition, which the study reproduces to 0.35″
  without the Sun's deflection and to 0.001″ with it (Accuracy,
  below). The side is *east* for X > 0 and *west* for X < 0; the words
  "east"/"west" and "north"/"south" are shown beside the magnitudes,
  never a bare sign. Separation is the angle between the centres; in
  Jupiter radii it is that angle over the apparent equatorial radius.
- **Observer-line depth** - the moon's distance from the observer
  minus Jupiter's, both light-time corrected: negative is nearer
  (*in front*), positive farther (*behind*). Depth is never inferred
  from X and Y.
- **Illumination and shadow** - at the moon's emission instant, the
  moon relative to the Sun–Jupiter axis: the umbra's radius at the
  moon's distance along the axis is R_e − d·(R_☉ − R_e)/D with
  R_☉ = 695 700 km (the PCK) and D the Sun–Jupiter distance; the moon
  is *in shadow* when its whole disc lies inside the umbra, *partly in
  shadow* when the umbra crosses its disc, *sunlit* otherwise. The
  penumbra is not a state (Horizons flags none either).
- **Light time, both legs** - as the Moon's contract has it: the
  down-leg takes each body where it was one light-time before the
  civil instant; the up-leg takes the Sun where it was one Sun–Jupiter
  light-time before the moon's emission, for the shadow and the phase.

## The visibility states, and their precedence

Two independent facts, both carried in the type: the **disc
relation** - *clear*, *in front* (the moon's disc overlaps Jupiter's
apparent figure and the depth is negative), *behind* (overlaps, depth
positive) - and the **shadow relation** - *sunlit*, *partly in shadow*,
*in shadow*. The reader's one-line state is composed in this
precedence:

1. *behind Jupiter, in its shadow* / *behind Jupiter* - a moon behind
   Jupiter cannot be seen whatever its illumination, and the shadow is
   stated after the fact that hides it;
2. *in front of Jupiter* - a transiting moon is sunlit by geometry
   (in front of the disc it is on the Sun's side of Jupiter);
3. *in Jupiter's shadow* / *partly in Jupiter's shadow* - clear of the
   disc but dark: not visible, and the table says why;
4. *clear of Jupiter* - visible.

The disc test is **limb-to-limb against the apparent figure**: the
moon's own apparent radius (the PCK's equatorial value) counts, so a
moon touching the limb is already *in front* or *behind*, which is
what an observer sees at ingress; the centre-crossing instant IMCCE
publishes lies inside that interval by the moon's own crossing time
(measured below). Horizons uses the same limb-to-limb rule with
equatorial radii and no oblateness; the atlas uses the oblate figure,
and the study measures what that changes: at most one minute at any
ingress or egress of the named evening (Accuracy, below).

## Accuracy, measured

Against JPL Horizons (jup365 merged with DE441; an independent
implementation in the same authority family, with its own time
scales and Earth orientation), row by row, with Skyfield reading
`jup365.bsp` (JUP365 with DE440) - the study's stand-in for the
service, with the atlas's conventions: no gravitational deflection,
UT1 = UTC, sea level - for the five observers at the twelve named
instants, every 7 days across 1900–2100 at Oslo and geocentrically,
daily through 2026, hourly through December 2026 and minute by minute
on the named evening (`contract/authority-comparison.md`; 22 993 rows per
body):

| body | era | rows | astrometric | apparent of date | horizontal | distance | X | Y | separation | position angle |
|---|---|---|---|---|---|---|---|---|---|---|
| Jupiter | 1900 → 2027-06-28 | 15 316 | **0.008″** | 1.10″ | 2.8″ | 18 km (1962–71), else 0.8 km | — | — | — | — |
| Jupiter | after | 7 677 | 0.003″ | 1.30″ | 406″ (not asserted) | 9.8 km | — | — | — | — |
| Io | exact interval | 15 316 | 0.009″ | 1.07″ | 2.8″ | 26 km (1962–71), else 1.3 km | 0.07″ | 0.05″ | 0.07″ | 0.04° |
| Europa | exact interval | 15 316 | 0.008″ | 1.13″ | 2.8″ | 27 km / 1.3 km | 0.11″ | 0.06″ | 0.12″ | 0.05° |
| Ganymede | exact interval | 15 316 | 0.008″ | 1.26″ | 2.8″ | 25 km / 1.1 km | 0.17″ | 0.18″ | 0.16″ | 0.06° |
| Callisto | exact interval | 15 316 | 0.008″ | 1.41″ | 2.8″ | 23 km / 1.1 km | 0.26″ | 0.35″ | 0.26″ | 0.08° |
| the moons | after | 7 677 | 0.004″ | ≤ 1.48″ | 406″ (not asserted) | 9.8 km | ≤ 0.12″ | ≤ 0.20″ | ≤ 0.12″ | 0.08° |

What the numbers are:

- **Astrometric 0.003–0.009″**: the two readers agree on JUP365 and on
  the Earth; the DE440/DE441 Jupiter-barycentre separation is below
  the Moon's 0.2″ because Jupiter is thirteen times farther.
- **Apparent of date ≈ 1″, worst at conjunctions** (1919-07-21,
  1978-07-10, 2056-01-10): the study's stand-in applies no
  gravitational deflection, as the Sun's and Moon's contracts do not.
  With the Sun's deflection applied (`contract/deflection-*.md`) the
  worst apparent difference from 1962 on falls from 1.04″ to
  **0.054″** on Jupiter's Oslo matrix and Io's X/Y to 0.001″; banded by
  elongation over both Jupiter matrices, the difference beyond 1° from
  the Sun is ≤ 0.57″ without deflection and ≤ 0.25″ with it, while
  within 1° (Jupiter behind or beside the Sun, unobservable) it stays
  1.3–1.4″ either way, where Horizons' treatment of a ray grazing the
  Sun differs from the plain formula. The 0.25″ residual with
  deflection lies in 1900–1901 at every elongation (0.054″ from 1962):
  a time-scale or frame difference of the stand-in's own, not isolated
  here and to be measured again with the service, which the Sun's
  contract already holds to 0.14″ in that era with the atlas's frames.
  **Deflection by the Sun belongs in Jupiter's apparent place**; the
  Moon could omit it (0.4 mas), Jupiter cannot (1″ at half a degree,
  0.05″ at five).
- **X and Y are Horizons' apparent-of-date differential coordinates**:
  computed from the apparent places they agree to ≤ 0.35″, from the
  astrometric places they are off by up to 6″ (Callisto, 1901) - the
  basis is settled by measurement, not by reading. With the Sun's deflection applied the X/Y agreement on Io's Oslo
  matrix is 0.001″ through the exact interval (`contract/deflection-io-oslo.md`):
  what remains of the 0.07–0.35″ above is the apparent-place
  difference, not the differential coordinates.
- **Distance**: 0.8–1.3 km through the exact interval; 18–27 km in
  1962–1971 where Horizons reads those civil instants with its own
  ΔT, as the Moon measured (the Moon paid 0.06 km, Jupiter at 13 km/s
  of range rate pays seconds × 13 km); 9.8 km after the interval.
- **Diameter**: 2·asin(71 492 km / d) reproduces Horizons' equatorial
  `Ang-diam` to 0.0000″ on every row (2·atan is indistinguishable at
  that precision); the contract states asin, as the Sun and Moon do.
- **Illuminated fraction**: 0.0005 points from the geometric phase
  angle; Horizons' S-T-O carries the two aberrations the Moon's
  contract learned (30″ here, 0.0005 points of k) - the service will
  compute S-T-O as the Moon does, and the first table shows k only.
- **Horizontal**: 1.1–2.8″ through the exact interval (UT1 = UTC and
  the ΔT model, as for the Sun); 406″ after it, where Skyfield's and
  Horizons' ΔT predictions diverge by 27 s - measured and never
  asserted, as the Sun's and Moon's contracts have it.
- **Visibility states**: on the equatorial-sphere, limb-to-limb
  definition Horizons states, the study's geometry agrees with
  Horizons' code on **91 971 of 91 972 rows** - the one disagreement
  a grazing Europa occultation (2026-12-06 17:00 at Oslo, separation
  20.3635″ against a limb sum of 20.3640″ - corrected on #473 from the
  study's 20.37″ against 20.69″: Horizons' own row gives Jupiter's
  diameter 39.85758″ and the separation 20.3635″, the service the same,
  so the margin is half a milliarcsecond, where the two
  implementations' light-time treatments differ) -
  and on the named evening every ingress and egress minute of every
  moon, geocentric and at Oslo, is Horizons' minute.
  With the **oblate apparent figure and the PCK pole** in place of
  Horizons' equatorial sphere (`contract/december-2026-transitions-oblate.md`),
  the named evening's transitions move by at most one minute: Europa's
  ingress 21:53 against 21:52, Callisto's egress 22:58 against 22:59,
  Io's unchanged at 22:32 and 00:53, at Oslo and geocentrically alike.
  The figure is the contract's; the sphere is the comparison's
  definition, and a one-minute difference at this event is the measured
  cost of saying which limb a moon touches.

**Proposed targets** (through the exact interval, as the tests
should hold the service; after it, measured and widened only by the
time-scale divergence, as the Sun's and the Moon's were):

| quantity | through the exact interval | after it (measured, widened by the time scales only) |
|---|---|---|
| Jupiter and each moon, astrometric J2000 | ≤ 0.05″ | ≤ 0.1″ |
| apparent of date, Sun's elongation > 1°, with the Sun's deflection | ≤ 0.2″ from 1962, ≤ 0.3″ before | ≤ 0.5″ |
| apparent of date, elongation ≤ 1° (Jupiter unobservable) | measured, printed, not asserted | — |
| horizontal | ≤ 5″ | not asserted |
| distance | ≤ 2 km (1972 →), ≤ 30 km (1962–71), ≤ 2 km (1900–61) | ≤ 25 km |
| equatorial and polar diameter | ≤ 0.001″ | ≤ 0.001″ |
| X, Y, separation (apparent basis) | ≤ 0.05″ | ≤ 0.2″ |
| position angle | ≤ 0.05° | ≤ 0.1° |
| illuminated fraction | ≤ 0.01 point | ≤ 0.03 point |
| visibility state against Horizons' code, equatorial-sphere definition | **an exact named allowlist**: the grazing Europa occultation at Oslo, 2026-12-06 17:00 (separation 20.3635″ against a limb sum of 20.3640″, as corrected on #473; the two implementations' light-time treatments differ at the graze) - any other disagreement is a new finding and stops the run | measured output, not asserted (amended on #473: the two ΔT predictions name different instants) |
| ingress and egress minutes on the named evening | Horizons' minute ± 1 (the figure's cost), IMCCE's centre-crossing minute inside the limb-to-limb interval; the production oblate-limb states compared separately from Horizons' spherical definition, every deliberate boundary difference named, never counted as an unexplained mismatch | — |

Each number bounds a measured, explained cause and sits beside the
maximum measured on 2026-10-07; the targets would be returned to the
owner before being widened, as the Moon's were.

## The named fixtures

**The 11 December 2026 triple transit** (the club member's report of
"11 December 22:35–22:53" was in Norwegian local time as written; the
published interval is UTC, and from Norway the event is 23:35–23:53
CET): Io, Europa and Callisto on Jupiter's disc at once, Ganymede
clear - one of seventeen such triple transits in the century, as the
published descriptions the owner cited put it. Three authorities,
three definitions:

| source, definition | Callisto on the disc | Europa | Io | three at once | Ganymede |
|---|---|---|---|---|---|
| **JPL Horizons** (jup365 merged, DE441), limb-to-limb with equatorial radii, minute series, geocentric and Oslo alike | 18:08–22:58 | 21:52–00:49 | 22:32–00:52 | **22:32–22:58** | clear all evening (303″ west) |
| **IMCCE** (Lainey 2021), the centre's crossing, TT rounded to the minute in UT (TT−UT = 69 s) | 18:13–22:50 | 21:54–00:46 | 22:33–00:50 | **22:33–22:50** | no event on the 11th |
| **Project Pluto** (`projectpluto.com/jevent.htm`, Guide 9.1, UT, read 2026-10-07) | 18:13–22:53 | 21:54–00:47 | 22:33–00:50 | 22:33–22:53 | — |
| **Naked Eye Planets** (Pierpaolo Ricci, quoted; read 2026-10-07) | | | | "between 2235 UT and 2253 UT", "one of only 17 occasions during the 21st century" | |

The three published intervals differ by what an ingress *is* - the
limb's first contact (Horizons: Io 22:32) or the centre's crossing
(IMCCE: 22:33) - and by the figure the limb is taken from; the
measured span between the definitions is a minute at Io's ingress and
up to nine minutes at Callisto's egress (22:50 centre, 22:53 Guide,
22:58 limb-to-limb equatorial), which is why the contract names its
definition rather than a time. The study's own geometry on JUP365 is
held to both (below).

The named contract instants, as ruled, are **22:30 UTC** (before Io's
limb ingress), **22:45** (inside the common triple transit), **22:55**
(the definition boundary: IMCCE's centre has left Jupiter while
Callisto's disc still overlaps it under the limb-to-limb contract -
kept as a boundary case, not as "after Callisto leaves") and **23:00**
(the unambiguous post-Callisto case under the production oblate limb:
Horizons' own code is clear there too, separation 21.20″ against a
limb sum of 20.93″); Ganymede is clear at all four, 303″ west. Each is
held at the five observers and geocentrically. The 2024–2025
configurations Sky & Telescope (Joe Rao, 2024-12-05) and
Astronomy.com (2025-12-01) described are kept as additional named
cases with their Horizons rows (`published-*-geocentric.txt`); they
are not the club member's report, which was of an event still to come.

## The reader's table, proposed

The mock-ups are `docs/studies/jovian-system/controls-jovian-*.png`,
in both languages, light and dark, drawn from the named fixture's
values. Proposed:

- **A Jupiter summary card** for the instant (image 1): right
  ascension and declination (J2000), altitude and azimuth, distance
  (AU), apparent diameter equator / poles, illuminated fraction, the
  north pole's position angle - the card form the Solar System
  section already uses for an instant.
- **The four moons** (image 2): one row per moon in the fixed order
  Io, Europa, Ganymede, Callisto - side; east–west offset with its
  letter; north–south offset with its letter; separation in
  arcseconds; separation in Jupiter radii; the state in words. The
  table is 604 px wide at the proposed column widths and scrolls
  sideways in the Controller like the Moon's.
- **A range** (images 3 and 4): the **timestamp-grouped** form - one
  heading per sampled instant with its four rows beneath - is
  proposed over the flat form, because a configuration is read as a
  group and a flat table interleaves four moons' rows; the step and
  the 1 000-row cap (250 instants) are the Sun's. It is a sampled
  table, not an event search: the words say "every … from … to …"
  and nothing finds an ingress for the reader.
- **Refusals and boundaries** (image 5): the Sun's and the Moon's,
  with Jupiter named.
- **In the Controller** (image 6): a Jupiter group under Solar
  System, collapsed on introduction, with the card, the table and
  Update from Place and Time; no Show on chart box until something
  Jovian is drawn (#474).

The words are listed in the study's report (`measurements.md`, "The
words, proposed") in both languages; *Ganymedes* is the Norwegian
form, *foran Jupiter* / *bak Jupiter* / *i Jupiters skygge* / *klar av
Jupiter* the states.

## Architecture, and the CI route

Reused as built: `Observer`, `TimeScales`, `TimeRange` and the
refusals; `SpkKernel` and `SpkExcerpt` (Type 2 only, which is all
JUP365 needs); the Sun's light-time and aberration chain and the
Moon's two-leg geometry; `SolarTable` for the table's columns and
words, `SolarTableSession` and `SolarTableControls` for one applied
query shown by two hosts; `SolarSystemSection` for a third group.

What becomes generic: `SolarSystemService.Body` gains JUPITER and the
observation type becomes a `JovianObservation` carrying the Jupiter
quantities and four `MoonConfiguration` records (offsets, separation,
depth, disc and shadow relations, the composed state); the table seam
gains a two-level row (an instant's card plus a group of four rows).
What stays Jovian-specific: the apparent figure and its pole, the
shadow geometry, the state vocabulary, the second kernel.

**Predicted routes**, by the classifier's closures as they stand
(`SolarSystemService`, `SolarSystemPack` and the pack resources are
reached by the chart-producing `SunOnTheChartStudyMain` and
`MoonOnTheChartStudyMain` through `SolarSystemModule`):

| pull request | touches | route |
|---|---|---|
| #473, the pack and the computation | `solar/*` (service, pack, a second kernel resource), scripts, fixtures, tests | **wide** - the service and pack are chart-reached; a data-pack change is never declared narrow by intention |
| #474, the table | `ui/solar/*`, `SolarSystemSection`, `JUranometriaMain` (the group), keys, photographers | **wide** - `JUranometriaMain` by ruling; everything else interaction |
| this study | `Makefile`, the contract's registries, the generator, fixtures | **wide** - the registries, as every study's |

## For the rulings

1. **Authority**: JUP365 (with DE440's barycentre from the released
   source) for the five bodies; Horizons as the in-family comparison;
   IMCCE as the independent catalogue for named events.
2. **Terms**: a modified, renamed JUP365 excerpt under NAIF's rules,
   with the same notice pattern as the released pack; **IMCCE rows
   quoted, the catalogue not redistributed** - the owner's call on
   whether quoting event rows with attribution is within "illustrate
   documents, source identified".
3. **Pack layout and interval**: layout A (a second, Jovian pack
   beside the untouched released one), and the Jovian interval - the
   whole released 1900–2100 at 91 MB (78 MB gzipped), or a narrower
   era: 2000–2100 at 46 MB, 2020–2080 at 27 MB, 2000–2050 at 23 MB.
   Jupiter alone could keep 1900–2100 (13.6 MB with its barycentre)
   while the moons keep a narrower era; the table refuses the moons
   outside it and says so. The study recommends **Jupiter 1900–2100,
   the four moons 2000–2100** (about 59 MB, 51 MB gzipped) if a
   doubled download is acceptable, else 2020–2080 for the moons.
4. **Frames and signs** as frozen above; Horizons' X/Y definition and
   sign convention adopted verbatim.
5. **Apparent figure**: the oblate IAU 2015 spheroid with JUP365's pole
   for the disc test and the later drawing; Horizons' equatorial
   sphere only as the comparison's definition.
6. **Visibility states**: the two relations and the precedence above;
   limb-to-limb ingress and egress; the penumbra not a state.
7. **Accuracy targets** as tabled under *Accuracy, measured*, and
   the Sun's gravitational deflection in Jupiter's apparent place (a
   new element the Sun's and Moon's contracts could omit; for Jupiter
   it is 1″ at half a degree from the Sun and 0.05″ at five).
8. **The table**: the card, the four fixed-order rows, the
   timestamp-grouped range, the refusals; the words as proposed.
9. **The named fixture**: the 11 December 2026 triple transit at its
   three instants, with the 2024–2025 configurations as additional
   cases.
10. **Fixtures committed**: of the 28.1 MB Horizons corpus (61 files,
    kept whole under `~/juranometria-runs/jovian-study/horizons/` with
    its README), the proposal was the subset every claim about the
    named fixture reads - the named instants for five observers and
    five bodies (25 files), the published 2024–2025 configurations (5),
    the 11 December 2026 minute series at Oslo and geocentrically (10),
    December 2026 hourly at Oslo (5) and the daily 2026 year at Oslo
    (5) - with the ten 7-day 1900–2100 matrices retained locally. **As
    ruled, the five Oslo era matrices join them: 55 files, 15.6 MB**,
    each re-hashed by `JovianFixturesTest`; the five geocentric
    matrices stay outside the repository, their URLs and digests in
    the durable study record. IMCCE: the quoted rows file only
    (`imcce-2026-12-11.txt`), never the catalogue.
11. **Implementation split**: #473 pack and computation (wide), then
    #474 table (wide), as #471 orders them.

## The ruling

The owner's rulings (2026-10-07), recorded as given:

1. **Authority approved.** JUP365 for Jupiter and the four Galilean
   moons, with DE440's Jupiter barycentre from the released source;
   Horizons for the dense implementation comparison; IMCCE for
   independently published named-event rows.
2. **Terms approved with the proposed boundary.** A modified, renamed
   JUP365 excerpt under NAIF's rules, with JUranometria's modifier
   identity, notice, manifest and provenance. Only the few IMCCE event
   rows the fixtures require are quoted, with exact attribution, URL,
   retrieval date and source digest; the IMCCE catalogue, software or
   any IMCCE file is never redistributed inside the application, and
   the application has no runtime dependency on IMCCE.
3. **Pack layout A and the recommended interval.** The released
   Sun/Moon pack stays byte-identical and untouched; a second Jovian
   pack is added. Jupiter **1900–2100**; Io, Europa, Ganymede and
   Callisto **2000–2100**. Outside the moon interval Jupiter remains
   answerable while the four-moon configuration refuses explicitly and
   names its supported interval. The cost - about 59 MB installed,
   51 MB compressed - is accepted.
4. **Epoch-aware SPK selection is mandatory.** #473 repairs both
   `SpkKernel` and `SpkExcerpt` to select the segment covering the
   requested epoch, holds the 1997 split with tests on both sides and
   at the boundary, and leaves no "first segment for body" behaviour.
5. **Frames and signs approved.** Horizons' apparent-of-date X/Y
   definition verbatim: X positive toward increasing right ascension,
   described to readers as east; Y positive north; words beside
   magnitudes rather than bare signs; front or behind from
   observer-line depth, never from X/Y.
6. **Apparent figure approved.** The oblate IAU 2015 Jupiter spheroid
   with the JUP365/PCK pole for limb tests and later cartography;
   Horizons' equatorial sphere only as the comparison's definition.
7. **Visibility model approved.** The disc relation and the shadow
   relation as independent typed facts; the proposed composed-state
   precedence; limb-to-limb contact including the moon's own radius;
   the penumbra not a reader state in this release; no visibility
   inferred from a two-dimensional offset.
8. **Accuracy targets approved, with one tightening.** The Sun's
   gravitational deflection is added to Jupiter's apparent position,
   the astrometric J2000 place untouched; elongations at or below 1°
   stay measured and printed, never asserted as observable accuracy;
   every other target stands and returns for a ruling before widening.
   The generic "one disagreement in 10 000" allowance is replaced by
   an **exact named allowlist** pinning the grazing Europa case at
   2026-12-06 17:00 and its cause; any further spherical-reference
   disagreement is a new finding and stops the run. The production
   oblate-limb states are compared separately from Horizons' spherical
   definition, and deliberate boundary differences are named, never
   counted as unexplained mismatches.
9. **Table approved.** The Jupiter summary card; Io, Europa, Ganymede
   and Callisto in fixed order; full offsets, separation, Jupiter radii
   and the state in words; timestamp-grouped sampled ranges capped at
   250 instants / 1 000 moon rows - sampling, not event search; Jupiter
   enters the Controller collapsed, with no Show on chart choice until
   cartography exists; the English and Norwegian wording as proposed.
10. **Triple-transit fixture approved with corrected contract
    instants.** 22:30 UTC before Io's limb ingress; 22:45 inside the
    common triple transit; **22:55 is not "after Callisto leaves" under
    the limb-to-limb contract** and is kept only as the
    definition-boundary case (IMCCE's centre has left while the disc
    still overlaps); **23:00 UTC** added as the unambiguous
    post-Callisto case under the production oblate limb; Ganymede clear
    throughout; the 2024–2025 configurations retained as secondary
    fixtures, never as the club report.
11. **Fixture boundary and implementation split approved.** The 50-file
    named/dense-2026 subset and the five Oslo 7-day era matrices
    (about 12.3 MB) are committed, so the principal long-era error
    table is held by a test; the five geocentric era matrices stay
    outside Git, retained with exact URLs and digests in the durable
    study record; only the quoted IMCCE rows are committed. Then #473
    computation and pack, then #474 table, both wide as predicted.

## #473, the first change: epoch-aware selection, as built

Ruling 4, built before any Jovian resource or service behaviour, on
`main` at `c7e136fd`:

- **The reader** (`SpkKernel`) selects by centre, target *and* epoch:
  `segments(center, target)` lists a pair's segments in file order,
  `segment(center, target, et)` returns the one covering the epoch and,
  where several cover it, the one later in the file - the precedence
  NAIF's own readers give (SPK Required Reading, the search order of
  segments). `segment(center, target)` remains for a pair with exactly
  one segment and refuses a pair with several ("select one by epoch").
  A gap between a pair's segments, or an epoch beyond them, is refused
  naming the pair and every interval it covers; a pair the kernel lacks
  is refused by name. Nothing takes "the first segment for a body".
- **The writer** (`SpkExcerpt`) keeps, for each pick, every source
  segment with records inside the interval, each cut to the interval,
  in file order - so a reader of the excerpt chooses as it would in the
  source - and refuses an interval the pair's segments do not cover
  together, naming the first uncovered stretch.
- **The released pack is unchanged.** The bundled kernel (sha256
  `63fbf570…`), excerpted by the new writer over its own coverage with
  its own name and comment, is reproduced byte for byte in the suite
  (`SpkExcerptTest`), and `make import-solar-system` from the pinned
  `de440s.bsp` rebuilt every file of the pack identical to the committed
  one. `SunInvarianceTest` and `SpkKernelTest` hold as before.
- **The fixture** `docs/studies/jovian-system/spk/jup365-boundary-1997.bsp`
  (31 744 bytes, a modified excerpt of JUP365 with JUranometria named
  as modifier) carries both of Io's and Jupiter's segments twenty days
  each side of the split and the barycentre's one; its builder
  (`SpkBoundaryFixtureMain`, `make spk-boundary-fixture`) proved 171
  states identical to the source and rebuilds it byte-identically. The
  independent reference `spk-boundary-reference.txt` (jplephem on the
  *unmodified* source, 227 rows, every covering segment's state) holds
  the reader on both sides of the split and at it: the later segment
  is selected there, the two fits meet within 5.1 × 10⁻¹⁰ km, and every
  row reproduces within the metre bound (worst 9.3 × 10⁻⁶ km, which is
  jplephem's offset arithmetic on the source, as the fixture's README
  explains). Gaps, overlaps with disagreeing segments and missing
  epochs are proved on kernels assembled from the fixture
  (`SpkAssembly`, test-only).
- **Route:** wide, by the classifier - the reader and writer are
  chart-reached through the Solar System service.

## #473, the second change: the pack and the computation, as built

Built on `main` at `f49dc2ff` (the first change merged), measured on
2026-10-08 and returned before any widening; the owner's amendments
of 2026-10-08, recorded as given, and the record as built:

**The pack** (`src/resources/jovian-system/`, `JovianPackMain`,
`make import-jovian-system`): one logical, versioned pack of two
kernel files behind one manifest and one validation boundary -
`juranometria-jup365-jupiter-1900-2100.bsp` (0 → 5 and both of
Jupiter's 5 → 599 segments, 13 550 592 bytes) and
`juranometria-jup365-galilean-2000-2100.bsp` (5 → 501 … 504, the
later segments, 39 294 976 bytes) - with the released 31-day margins.
Two files because the ruled intervals differ (Jupiter 1900–2100, the
moons 2000–2100; the Galilean excerpt over 1900–2100 would be 91 MB);
the notice says so. The builder proves every excerpt identical to its
source (86 370 and 86 800 states), the barycentre identical to the
released `de440s.bsp` (43 222 states: NAIF merged DE440's own
segment), and the released Solar System kernel untouched (sha256
`63fbf570…`) before writing; the PCK's radii and pole are copied
verbatim into the manifest, the PCK itself not redistributed.

**The service** (`JovianSystemService`): Jupiter as the Sun and the
Moon are computed, plus the Sun's gravitational deflection in the
apparent place only (the NOVAS one-body formula as Skyfield 1.55
writes it, the Sun where it was when the ray passed closest); the
equatorial and polar diameters by asin; the pole position angle from
true-of-date north and the sub-observer latitude; the phase angle and
illuminated fraction as the Moon's. Each moon: X and Y as ruled,
separation on the same apparent basis (the targets table's "apparent
basis"; from the light-time-corrected directions the difference from
Horizons was 0.062″ at Jupiter's conjunction, from the apparent places
0.0004″), the position angle from X and Y, depth, the disc relation on
the oblate figure, the sphere's beside it for the comparison, the
shadow relation at the moon's emission instant with the up-leg light
time, the composed state. Pure ephemeris-time entry for the geometry
tests.

**Measured against Horizons** (`docs/studies/jovian-system/service-accuracy.md`,
`JovianReferenceVectorTest`), through the exact civil-time interval:
astrometric 0.011″; apparent with deflection 0.134″ (0.095″ before
1962); X, Y and separation 0.0007″; position angle 0.036°; diameters
0.0000″; illuminated 0.00001; pole angle 0.002°; distance 0.22 km from
1972, 4.95 km in 1962–71; the state on the sphere's definition agreeing
on every row but the allowlisted graze; the named evening's minutes
Horizons' on the sphere, within one minute on the figure (Europa's
ingress 21:53, Callisto's ingress 18:09 and egress 22:57); IMCCE's centre-crossing
minutes inside the limb-to-limb intervals. **Exceeded and amended by
ruling**, each the atlas's own frozen civil-time model: horizontal
11.85″ (Place and Time's UT1 = UTC beside leap seconds; the released
Moon shows 11.58″ on the same 1994 date) → **≤ 20″** as the Sun's and
the Moon's; distance in 1900–1961 32.6 km (the ΔT model about 1.2 s
from the record at 28 km/s of range rate) → **≤ 40 km**; after the
exact interval, where the two ΔT predictions name instants up to 133 s
apart: astrometric 1.7″ and apparent 1.8″ → **≤ 3″**, X, Y and
separation 0.71″ → **≤ 1″**, distance 5 522 km → **≤ 7 000 km**, the
states **measured output, not assertions** (17 further rows from 2041
on), the position angle **printed and classified by conditioning** -
ill-conditioned where the authoritative X/Y residual could turn it by
more than 0.05° at the row's separation, no cutoff - with the 36.6°
residual at a 0.5″ separation not a physical disagreement. The pole
position angle, a quantity without a ruled target, **≤ 0.01°**
(measured 0.002°). The flat position angle from X and Y against
Horizons' spherical one differs by second order in the separation
(0.036° at ten arcminutes), inside the ruled 0.05°, and the definition
stands.

**Presentation** (ruling 8): quantities after the exact civil-time
interval inherit the Sun's and the Moon's estimate qualification
(`timeConfidence` on every observation); the state labels there are
geometry at an estimated instant, never a prediction with exact event
timing - a rule for #474's table.

**A correction found by the geometry test.** The study's oblate disc
test (and the first port of it) took the moon's lateral offset in
kilometres and compared it with Jupiter's limb at Jupiter's distance;
a moon nearer or farther than Jupiter subtends that offset at its own
distance, so the offset was misplaced by the ratio of the two
distances - up to 0.02″ at a graze, enough to put a transiting moon
on the figure where the sphere had it clear. The test is now made in
angles throughout. The study's oblate minutes
(`contract/december-2026-transitions-oblate.md`) carried the shortcut;
the service's, above, do not.

**Full-range geometry** (`JovianGeometryTest`): every four hours over
2000–2100 at fixed ephemeris time, in front is nearer and sunlit,
behind and shadowed are farther, every overlap lies within the limb
sums, the figure only ever frees a limb the sphere holds and only at a
graze, every composed state occurs for Io, Europa and Ganymede; and
Jupiter's figure and phase stay within what the geometry allows over
1900–2100. Boundaries (`JovianBoundaryTest`): the intervals inclusive
of their ends, Jupiter answering where the moons refuse, a missing
pack refusing the service and leaving the Sun and the Moon as
released.

## #474, the table, as built

The table ruled in 9 above, built on `main` at `193c72fe` over the
accepted service (`JovianTableSession`, `JovianTableControls`,
`JovianMoonsModel`, `JovianTableFormat`, `JovianTableDialog`):

- **One applied query and result, two hosts.** The Controller's
  Solar System section gains a Jupiter group after the Moon's,
  introduced collapsed; View > Solar System gains *Jupiter...* after
  *Moon...*, the dialog host kept for dogfooding as the Sun's and the
  Moon's are. Both follow one session; each keeps its own draft, and a
  result applied in either never writes into the other's fields. No
  Show on chart box: nothing Jovian is drawn.
- **Nothing computes from building, opening or expanding** (#474,
  required). This differs from the Sun's and the Moon's dialogs, which
  compute when they open and come to the front (ruled on #400): the
  Jupiter dialog shows what the session holds and computes only on
  Compute, a view, Enter or Update from Place and Time. The 53 MB pack
  is read on the first computation, never at startup.
- **One instant**: the Jupiter card - right ascension and declination
  (J2000), altitude, azimuth, distance (AU), apparent diameter equator
  / poles, illuminated fraction, the north pole's position angle - and
  the four moons in the fixed order Io, Europa, Ganymede, Callisto:
  side, east–west and north–south offsets with their letters,
  separation in arcseconds and in Jupiter radii, the state in words,
  the rounding of the ruled mock-ups. Every value is the service's
  result, rounded; the table recomputes nothing.
- **A range**, timestamp-grouped as ruled: **each group is one
  Jupiter result and its four moons** - a heading naming the sampled
  instant, two lines of Jupiter's summary with the card's eight values
  labelled (right ascension, declination, altitude, azimuth; distance,
  diameters, illuminated fraction, the pole's angle), then the four
  moon rows - every line spanning or filling one table a keyboard
  walks, whose spoken name says how it is grouped; at most 250
  instants (1 000 moon rows), refused with the count beyond. It
  samples; nothing searches for an ingress. *The first build grouped
  only the moons under each instant; the owner's packaged checkpoint
  found that a range then answered how the moons stand around Jupiter,
  not where Jupiter is, against the ruled contract - corrected before
  the checkpoint was accepted, and held by a contract test.*
- **Refusals**, with the years named: no observer; Jupiter outside
  1900–2100; a single instant before 2000 shows Jupiter's card and
  says the configuration is refused for 2000–2100; a range leaving the
  moons' years is refused whole; a range running backwards or an
  unreadable instant as the Sun's and the Moon's.
- **After the exact civil-time interval** (ruling 8 of #473): the
  instant and each group heading carry *est.*, and the status says the
  moons' states there are geometry at an estimated instant, not
  predicted times of events; the time note says so too.
- **Words**: English and Norwegian, the study's proposed wording as
  ruled (*Ganymedes*, *foran Jupiter*, *bak Jupiter*, *i Jupiters
  skygge*, *klar av Jupiter*), the letters e/w/n/s and ø/v/n/s; the
  remaining Norwegian sentences are new and await review.
- **Proof**: `JovianTableTest` - zero computations and no pack read
  from building and expanding; one result in two hosts with drafts
  untouched; every cell the service's answer; the triple transit in
  both languages; every range group one Jupiter result and its four
  moons, Jupiter's eight values spelled as the card spells them and
  its position moving across seven weekly instants, in both languages
  (Jupiter is near its stationary point in December 2026 - about
  0.05 s of right ascension an hour, measured - so hourly positions
  repeat at the table's rounding); **every December 2026 hour at Oslo reads Horizons'
  state in words, in both languages, but the allowlisted graze**, every
  one of Horizons' seven codes occurring; the refusals; a range grouped
  and capped at exactly 250 instants; the estimate mark.
  `SolarSystemCompanionJourneyTest` in the real application: Jupiter
  after the Moon, collapsed, opened computing nothing, reached by the
  Controller's own focus traversal, Compute showing the card and the
  four moons; View > Solar System ending in *Jupiter...*. The packaged
  acceptance runs the same journey in every native image
  (`jupiter table OK`).
- **Evidence**: no chart, export, sheet or gallery byte moves; the
  Controller's photographers build the section without Jupiter, as
  before.
