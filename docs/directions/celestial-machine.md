# Direction: the celestial machine

## Purpose

Let the fixed chart reveal the geometry that connects celestial coordinates,
Earth's orientation, and Solar System motion. The result should feel like an
astrolabe laid over an atlas: quiet, inspectable, and still when the reader is
not acting.

Sprint 25 placed the first stone. A removable module draws the reader's
meridian, zenith, and mathematical horizon for one explicit place and frozen
instant. It also established the frame rule: dated geometry is transformed
onto the J2000 chart rather than quietly treating coordinates of date as
J2000.

## Likely path

1. **Ecliptic reference.** Decide whether the chart should carry the mean
   ecliptic of J2000, the ecliptic of date, or both with unambiguous names. A
   J2000 ecliptic is the likely stable road for the fixed chart, but that is a
   gate question rather than a decision made here.
2. **Astronomical zodiac belt.** Study optional boundaries at stated ecliptic
   latitude. They are small circles, not great circles, and require their own
   spherical projection and clipping evidence.
3. **Solar System positions.** Plot selected bodies at an explicit instant,
   transformed honestly into the chart's frame.
4. **Dated tracks.** Draw bounded, reader-requested paths with sparse dates,
   including retrograde loops, without animation or automatic following.

The ecliptic comes before planets because it is useful cartography by itself
and gives later motion its intelligible road.

## What the ecliptic gate must settle

- Mean J2000 ecliptic versus ecliptic of date, including the visible cost of
  choosing the wrong frame.
- An authoritative obliquity/model and oracle; never a typed `23.44` constant
  presented as precision.
- The central ecliptic as a great circle contributed by its pole and clipped
  analytically, not a one-degree polyline.
- Whether an optional ±8° belt helps reading. Its boundaries are small circles
  and cannot reuse great-circle clipping.
- Whether longitude ticks earn the ink: none, sparse major ticks, and dense
  ticks should be compared on real pages.
- Vocabulary. Equal 30° zodiac signs are not the unequal astronomical
  constellations, and the ecliptic also crosses Ophiuchus. No glyph or label
  should imply otherwise.
- Pages at both equinox crossings, both solstitial extrema, RA 0, the poles,
  narrow fields, and places where a belt contains the whole page.

An ±8° belt may describe a traditional visual zodiac; it must not promise that
every body a future Solar System module could plot is guaranteed to remain in
it.

## What the Solar System gate must settle

- Calculation code versus a bundled ephemeris: accuracy, supported dates,
  provenance, licensing, size, offline operation, and reproducibility.
- A primary external oracle for every supported body and the frame/time scales
  used in comparison.
- Which bodies belong in the first release and how the Sun and Moon differ
  from planets.
- Static position, track interval, sampling/error bound, date marks, label
  collisions, RA-seam crossings, polar passages, and paths leaving and
  re-entering a page.
- How a module consumes the selected instant and selection events without the
  chart learning ephemerides or orbital vocabulary.

Mars's retrograde loop is the defining visual case. Mercury near the Sun, the
Moon's rapid motion, an outer planet's slow track, and an RA-seam crossing are
necessary counter-cases.

## A smaller future module: meteor showers

Meteor-shower radiants offer a compact use of the same celestial machinery.
The reader chooses one established shower and a date; the module centres the
chart on the shower's radiant, marks it with restrained reference ink, and
states the date. Nothing needs to animate, predict an observing session, or
fill the chart with every active shower. A later Moon module could add useful
context without becoming a dependency of the shower module.

The apparent simplicity should remain explicit about the astronomy. A gate
must choose an authoritative source and verify its redistribution terms, then
settle the radiant's coordinate frame and epoch, the shower's activity and
peak dates, radiant drift through the activity interval, and whether the source
defines timing by calendar date or solar longitude. “Centre on the radiant” is
a reader-requested navigation action; merely enabling the module must not move
the page.

The first useful acceptance case is deliberately small: select one familiar
annual shower, choose a date within its recorded interval, arrive at the
source-derived radiant on the fixed chart, and see the shower name and stated
date. Removing the module leaves the ordinary chart unchanged.

## Guardrails

- No ticking clock, animation, device orientation, or automatic recentering.
- No network dependency or silently expiring astronomical data.
- No invented accuracy and no frame conversion hidden behind a generic label.
- No default track so large or dense that it becomes the chart.
- The reader requests every movement and time interval.
- Removing the module leaves the ordinary chart byte-identical.
