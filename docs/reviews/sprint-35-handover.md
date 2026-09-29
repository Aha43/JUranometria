# Sprint 35 handover — Know where the Sun is

Four issues, three merged pull requests, two owner checkpoints that
each stopped a merge, and a gate that will outlive the sprint. This
is what the sprint established, the contracts the Moon and the planets
inherit, what the owner found that no check could, what stayed
rendering-neutral and what did not, the totals, and the questions the
visual Sun begins with.

Written against `main` at `edfbed5` (PR #404), after its post-merge
checks. Sprint 35 establishes trustworthy Sun data and a reusable
numerical surface; it does not claim that JUranometria shows the Sun
on its chart. No version changes, nothing is tagged or released.

## Why the sprint existed

The road to 4.0 (#397) makes the Sun and the Moon trustworthy numbers
before it makes them chart objects. Numbers need an authority, a time
scale, frames with exact meanings, an accuracy that is measured rather
than asserted, and a table a reader can use - all offline, because the
atlas works without a network, permanently. And calculation work should
not pay an hour of rendering qualification to prove what it cannot
change, so the sprint began by building the guard that decides.

## What shipped

**#398 — the contract and the rendering-neutral gate (PR #402).** A
permanent, body-agnostic classifier: `RenderingClosure` computes, from
the compiled tree, every source file reachable from the programs whose
output CI compares; `ChangeRoute` judges each changed path against that
closure and a named list of inputs the regime consumes without
compiling; one reusable `classify` workflow gates the evidence job, the
native images and the archive on `wide`. Mutation-proved before any
ephemeris code. Recorded in `docs/decisions/rendering-neutral-gate.md`.
The same issue froze the science contract after two evidence
checkpoints (authority comparison, then measured route, time scales,
errors and table), and amended Place and Time's guard from a filename
ban to an ownership rule.

**#399 — the pack, the service and the first table (PR #403).**
`src/resources/solar-system/`: a JUranometria-modified excerpt of JPL
DE440 (Sun, Earth–Moon barycentre and Earth, 1900–2100 with a 31-day
margin, 8 884 224 bytes, every coefficient the source's) and the IERS
leap-second file unmodified, with manifest checksums, a notice, a
generated provenance record and a builder that proves the excerpt
against the official kernel and rebuilds byte-for-byte.
`juranometria.solar`: an SPK Type 2 reader held to an independent
reader at 14 mm; the time scales; the body-agnostic `TimeRange`; and
`SolarSystemService`, which answers the contract's quantities for Place
and Time's observer and instant and reads no clock. Held row by row to
12 670 rows of JPL Horizons. The first table as numbers,
`docs/studies/solar-system/measurements.md`, registered with the
evidence contract.

**#400 — the reader's table (PR #404).** `View → Sun…`: a modeless,
singular dialog over the service, reading the meridian module's
observer on demand, with the single-instant and range views, stated
refusals, marks for an appended end and an estimated clock correction,
every control named and lettered, every heading saying its unit and
frame, in English and Norwegian. Photographed by its own generator;
the menu and control-explanation evidence re-recorded; driven inside
the packaged image in both languages.

## The contract, as frozen

**Authority.** JPL DE440 (Park, Folkner, Williams and Boggs 2021), as
a modified SPICE kernel under NAIF's rules: renamed, JUranometria named
as the modifier in the comment area, JPL's own comment area retained
as source attribution, no fee or licence. The IERS leap-second record,
public data, bundled unmodified. Both under
`src/resources/solar-system/` with source URLs, retrieval date,
upstream and output digests, extraction recipe and validation.

**Interval.** Civil dates 1900-01-01 to 2100-12-31 inclusive, refused
outside; the kernel's coverage carries a 31-day margin, which was
measured to be necessary.

**Time.** Civil instants before 1972 are UT1, with TT from the
unmodified Espenak–Meeus expressions; from 1972 to the pinned IERS
file's expiry (2027-06-28 in pack v1) the reading is exact; after it,
UT1 ≈ UTC and TT − UTC is estimated from the same expressions; TDB ≈
TT; UT1 = UTC for Earth rotation. Every answer says which era it is in.
The expiry is a data-validity boundary; a later pinned file moves it
through the pack's provenance, and the surfaces generate the date from
the pack.

**Frames.** Chart position: topocentric astrometric ICRS/J2000,
light-time corrected, no aberration, no deflection. Apparent of date:
computed and tested, not shown. Horizontal: topocentric apparent,
airless, azimuth from north through east. Distance: observer to the
light-time-corrected centre. Apparent diameter from the IAU 2015
nominal radius, 695 700 km.

**Accuracy, measured against Horizons (12 670 rows, five observers).**
Through the exact interval: astrometric 0.05″, apparent 0.14″,
horizontal 11.2″, distance 2.2 km, diameter 0.0005″, inside the ruled
targets of 0.1″ / 1″ / 20″ / 10 km / 0.01″. After it, the ΔT
conventions' 133 s at 2100 dominate: astrometric 5.8″, apparent 5.9″,
distance 68 km, held at the owner-accepted envelopes of ≤ 6″ and
≤ 100 km with the measured maxima beside them; horizontal after the
interval is printed, not asserted; these are model/convention
comparison envelopes, not measured physical error in DE440. Meeus 25.a
within its stated 0.01°. Five mutation rivals each fail their target.

## What the Moon and the planets inherit

- `SolarSystemService.observe(Body, Observer)` and
  `observe(Body, Observer, TimeRange)`: a body enum with one member
  today, an `Observation` sealed interface, rows over samples. A new
  body adds its observation type and columns; the shape stays.
- `TimeRange`: inclusive ends, a positive fixed elapsed-time step on
  the UTC timeline, an off-grid end appended and marked, more than
  1 000 rows refused rather than truncated, a backwards range and a
  step that cannot advance refused; `rowsOf` for a surface that
  refuses with the number stated.
- `TimeScales` and its `Confidence`, `LeapSeconds` with its expiry,
  `DeltaT`.
- The pack: regenerated as one pack when the Moon's segment is added
  (option 3b, ≈ 14.9 MB), never a second kernel beside it; the
  filename and manifest name the bodies and the version.
- `SunTableModel`'s marks and refusals, `SunTableFormat`'s rounding,
  the language files' `suntable.decimal`.

## The table's words

| | English | Norsk bokmål |
|---|---|---|
| menu | Sun… | Solen… |
| title | The Sun | Solen |
| views | This instant / Over a range | Dette tidspunktet / Over et tidsrom |
| range | From (UTC) / To (UTC) / Every / Compute | Fra (UTC) / Til (UTC) / Hvert / Beregn |
| steps | 1 hour, 6 hours, 1 day, 7 days, 30 days | 1 time, 6 timer, 1 døgn, 7 døgn, 30 døgn |
| buttons | Update from Place and Time / Close | Oppdater fra Sted og tid / Lukk |
| columns | Instant (UTC); Right ascension (J2000); Declination (J2000); Ecliptic longitude (J2000); Altitude (no refraction); Azimuth (from north through east); Distance; Apparent diameter | Tidspunkt (UTC); Rektascensjon (J2000); Deklinasjon (J2000); Ekliptisk lengde (J2000); Høyde (uten refraksjon); Asimut (fra nord via øst); Avstand; Tilsynelatende diameter |
| marks | est. · † · (below the horizon) | est. · † · (under horisonten) |
| decimal | 51.01°, 1.016165 AU (152.016 mill. km), 31′ 27.9″ | 51,01°, 1,016165 AU (152,016 mill. km), 31′ 27,9″ |
| refusals | backwards; too many rows; unreadable instant; outside 1900–2100 | baklengs; for mange rader; uleselig tidspunkt; utenfor 1900–2100 |

The complete set is the `suntable.*` and `menu.sun.*` families in both
language files (80 keys each) and the photographed companion
`docs/studies/interface-language/suntable-strings.md`. The Norwegian
strings are original translations reviewed by the owner in the #400
journey; the decimal comma was the owner's ruling, applied to what is
shown and never to what is parsed.

## What owner testing found that no check could

**The seasonal instants were wrong (#399).** The first table named the
June solstice at 02:24 UTC and the September equinox at 22:05 UTC on
the 22nd - instants supplied from memory, hours wrong, under the right
names. The computed positions were correct for the instants given.
Repaired as ruled: one cited fixture (the IMCCE's tables, to the
second, with the documents' URLs and digests), everything that names
an event reading it, a contract red first against the old files, the
five named Horizons responses refetched, the accounting rerun on the
same 12 670 rows (the maxima moved only where the corrected row was
itself the worst), and a fresh qualification from gate 1. The green
CI run on the rejected candidate stays in the record as green but
rejected. "Local midnight" became "Near local solar midnight,
midsummer (23:00 UTC)".

**The Norwegian decimal (#400).** The first candidate kept the point in
Norwegian on the interface files' standing rule that a value must
parse; the owner ruled the comma for the reader-facing table, with
calculations, fixtures, manifests and typed values locale-independent.
The separator became a value in each language file, the five Norwegian
photographs were re-recorded, and the corrected candidate was
re-qualified from gate 1 before the six-step journey.

**Accepted at the checkpoints:** the table's columns and presentation;
the future envelopes; the pack's terms; copy/export deferred until
dogfooding shows what form is useful.

## Rendering-neutral, and what was skipped

The gate is built, mutation-proved, and answered `wide` for every
pull request this sprint: #402 changes the workflows and the guard
itself; #403 introduces the pack (wide by ruling, because it changes
packaging) and adds fixtures under `docs/studies`; #404 moves committed
interface evidence - the menu photographs, a new photographer's
companion, the control-explanation study, the language files. So
**no expensive job was skipped this sprint**: every PR ran the
evidence contract, the four native images and the archive on CI. That
is the honest reading of the rule: a narrow route exists for
calculation-only changes, and no calculation-only change happened this
sprint - the Moon's computation will be the first such candidate. The
pull request carrying this handover, prose alone, is the first change
to take the narrow route, and its CI run is the record of what that
skips. Pushes to `main` classify wide by event, as designed.

What the gate did prove is the other half: the chart's renderer, its
options, the projection, the sky model and Place and Time refer to
nothing under `juranometria.solar` (`SolarSystemBoundaryTest`,
`PlaceAndTimeGateTest`), no chart rendering moved, and the 417
provenance rows changed only for the interface photographs that were
re-recorded (About, menu, Sun table).

## Totals

| head | local suite | classified refusals | CI display (first run) |
|---|---|---|---|
| #402 `d565965` | 1670 found and started, 1632 passed, 0 failed, 38 focus-family aborts | none | 1670/1670, 0 failed, 0 aborted, 0 skipped |
| #403 `e717a8a` (rejected candidate) | 1717, 1674 passed, 43 focus aborts | none | 1717/1717 clean - green but rejected at the checkpoint |
| #403 `bdbf61f` | 1721, 1697 passed, 2 refusals, 22 focus aborts | Export photographer's packed geometry replaced before proof; sizing contract's window rolled back after restatement | 1721/1721 clean |
| #404 `3efa757` | 1735, 1692 passed, 43 focus aborts | none | 1735/1735 clean |
| #404 `2ff1c92` | 1735, 1712 passed, 1 refusal, 22 focus aborts | sizing contract's window rolled back after restatement | 1735/1735 clean |

Every classified refusal was accepted by the owner under the #390 rule
as unexecuted local coverage, with CI's zero-abort display run as the
executed coverage; each gate-1 record is preserved and none was rerun.
Stopped gate runs kept as stopped: `bb918f6` (five ledger occurrences
unreviewed), `3ad24cb` (four About provenance rows not re-dated),
`bdbf61f` and `2ff1c92` (gate 1, the refusals above). Post-merge on
`053d9c4`, `a3dabdc` and `edfbed5`: test, display, evidence and the
dispatched packaged acceptance green on each exact merge head.

## Deferred, by ruling

- **The constellation column.** Two independent implementations
  disagree on 5 of 12 670 rows, all at boundary-crossing instants; the
  atlas has no point-to-constellation lookup. Needs its own authority,
  a B1875 transformation and an exact crossing rule; reconsider after
  dogfooding shows whether the reader misses it.
- **Rise, set, transit and twilight** - event searches with a horizon
  and refraction policy of their own.
- **Copy/export from the table** - until dogfooding shows what form is
  useful.
- **Apparent-of-date RA/Dec, hour angle, equation of time** - computed
  and tested, not shown.
- Place and Time's silent restoration of invalid input (pre-existing);
  the Sun table states its refusals.

## Questions the visual Sun begins with

Not answered here, by ruling; the next sprint begins with visual
studies on real pages:

- **Angular-size scaling:** the disc is 31–32′; at what field widths is
  it drawn at scale, and at what scale below?
- **Minimum visible size:** the smallest mark that is honestly a Sun
  rather than a star.
- **Label:** whether, where, in which language, under which collision
  policy.
- **Occlusion:** what the disc covers - stars, grid, horizon - and
  whether daylight is represented at all.
- **Selection:** whether the Sun is selectable, what the Inspector
  says, what the view report carries.
- **Emphasis:** whether the Sun joins the raised structures or is its
  own.
- **Export:** the disc and any label in SVG, PDF and PNG, ordinary and
  emphasized.
- **Range depiction:** whether a range becomes a track on the page,
  and if so how - the table already answers the numbers.

The revised road, as the owner set it after #400: the Moon's
computation and table next, before any combined Sun–Moon cartography.

## The owner journey

Two journeys, both on packaged candidates. The #399 table review found
the seasonal instants and stopped the merge; the corrected candidate
was accepted. The #400 six-step journey - Oslo now; Quito or Cape Town
through *Update from Place and Time*; the IMCCE June solstice and
September equinox; today; a multi-day range with an off-grid final
instant; both languages for punctuation, units, the estimated-time
explanation, a negative altitude and the appended row - was driven on
the corrected candidate built from `2ff1c92` and accepted, including
the language review, with the answer that the table explains where
the Sun is, how large it looks, and how those values change without
supplied memory.

## Closing checkpoint

Pending the owner's acceptance of this handover. No VERSION change,
tag or release.
