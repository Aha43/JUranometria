# Orienting the chart to a place and a time

**Sprint 25, issue #225.** The gate for #226–#229. Measured by
`make place-and-time-study`; the numbers quoted here are that
study's, and its report and images reproduce byte for byte.

## The question

A reader can find M31 on this chart. They cannot find out whether it
is above their horizon tonight, or which way to face. The atlas
knows where everything is and nothing about where the reader is.

> **The chart stays a chart.** It gains three lines that belong to a
> place and an instant — the meridian, the mathematical horizon and
> the zenith — and gains no clock, no observer and no planetarium.

## The observer

**Latitude, east-positive longitude, and an explicit instant in
UTC.** Typed by the reader, and nothing else: no device location, no
place-name service, no time-zone database, no network.

- **The place is remembered.** A reader's latitude and longitude
  change rarely and are tedious to type; they belong with the
  reader's other stored choices.
- **The instant is not.** It is a frozen snapshot of a moment the
  reader chose, and a remembered one would come back stale and
  wrong-looking with no way to tell why.
- **"Now" means the moment the reader presses it**, read once from
  the system clock and frozen from then on. It is a button, not a
  state: nothing ticks, and pressing it again is how a reader moves
  the sky forward.

East-positive longitude is stated in the field label rather than
assumed, because the sign convention is the single easiest thing to
get wrong here and a chart drawn for the wrong hemisphere looks
entirely plausible.

## The frame problem, and the accuracy contract

The catalogue and the chart are **ICRS/J2000**. A meridian, a zenith
and a horizon are defined against the **true equator and equinox of
date**. Putting the second on the first is a change of frame, and
the atlas performs it rather than pretending it is unnecessary.

**The shortcut is rejected by measurement.** Computing right
ascension from sidereal time and plotting it on the J2000 page
unchanged is wrong by the whole of precession since 2000:

| instant | error | at the 36° field | at the 1° field |
|---|---:|---:|---:|
| J2000 itself | 8.00″ | 0 px | 2 px |
| March 2026 | 21.17′ | 9 px | 318 px |
| 2050 | 39.34′ | 16 px | 590 px |

**Precession alone** leaves nutation as the residual — about 9″, or
2 px at the narrowest field. Free to avoid, so it is avoided: the
atlas carries **precession (IAU 1976) and nutation (IAU 1980,
twenty terms)**.

Three simplifications remain, each priced rather than hidden:

| simplification | worst error | at 36° | at 1° |
|---|---:|---:|---:|
| UTC stands in for UT1 (bounded at 0.9 s by agreement) | **13.54″** | 0.09 px | 3.38 px |
| UTC stands in for TT in the precession arguments | 0.00″ | 0.00 px | 0.00 px |
| the nutation series stops at twenty terms — against the published full-series value | 0.0009″ | 0.00 px | 0.00 px |
| the same, bounded for any date by the series' own ordering | 0.40″ | 0.00 px | 0.10 px |
| IAU 1976 precession against the IAU 2006 form | 0.28″ | 0.00 px | 0.07 px |

Polar motion (< 0.5″), diurnal aberration (< 0.3″) and refraction
are not modelled at all.

**The nutation tail is bounded rather than guessed at.** An earlier
draft compared twenty terms against four and called the difference
the truncation cost. That measures what terms 5–20 contribute and
says nothing about the terms actually omitted (review). Two things
are measured instead: this series against the **published
full-series value** for Meeus's worked example, which is 0.0009″;
and a worst case from the series' own ordering — the twentieth
term's amplitude is 0.0046″, no later term exceeds it, and 86 of
them in phase at maximum would be 0.40″, which they cannot be.

> **The accuracy contract, derived rather than asserted.** Adding
> those terms at their worst — 13.54″ for UT1, 0.40″ for the
> nutation tail, 0.28″ for the choice of precession model — the
> zenith, meridian and horizon are placed within **14.22″** of the
> observer's own frame: 0.10 px at the widest field, 3.6 px at the
> narrowest. Every term other than UT1 is worth 0.68″ together, so
> the bound is what not knowing UT1 costs, and nothing else matters
> until that changes.

## The three geometries

| geometry | what it is |
|---|---|
| **zenith** | the point overhead: RA = apparent sidereal time + east longitude, Dec = latitude, of date |
| **meridian** | the great circle through both celestial poles and the zenith |
| **mathematical horizon** | every direction ninety degrees from the zenith |

**The horizon is named mathematical, in the interface and not only
here.** It is where the sky meets a perfectly flat, perfectly
transparent Earth. A reader's real horizon has hills and air in it,
and a line that quietly claimed to be that would be the atlas
promising something it cannot know.

**The anti-meridian and the nadir get no vocabulary.** The meridian
is one closed circle, so a page holding the far half shows it
without teaching a second name; the nadir is never marked, because a
mark for the point under a reader's feet is a mark for something
they cannot look at.

## What it looks like on a page

**A gnomonic projection maps every great circle to a straight line**
— measured at 0.0000 px of deviation across every field.

That does *not* make a polyline sufficient, which an earlier draft
of this gate claimed (review). `OverlayContribution.Path` is a list
of positions: subdivision by another name — the study was passing it
720 — and no list of vertices can say where the **infinite** circle
crosses the paper when every vertex it was given lies outside it. A
one-degree page with a circle sampled at eight points has no vertex
anywhere near the paper, and a polyline through them draws nothing.

**So the seam gains one generic geometry: a great circle, given by
its pole.** A pole is a direction; the chart learns no astronomy
from being handed one. Because the projection is gnomonic, the chart
can then be exact: the projected circle is fixed by any two of its
points that project at all, and the resulting line is clipped to the
paper analytically. Nothing about what a reader sees is decided by
sampling.

The cases that decide it are measured, not argued — every one of
them a case a polyline answers wrongly:

| case | answered |
|---|---|
| every supplied vertex off the paper | the crossing is found |
| a page lying wholly between two sparse samples | the crossing is found |
| a corner clip a few dozen pixels long | exact, and short |
| the half of the circle the projection refuses | clipped from the visible half |
| polar pages, both poles, and the RA seam | no different |
| a circle that misses the paper | silence |

Each returned endpoint is checked against the sky rather than
against the arithmetic that produced it: the pixel is turned back
into a direction through the chart's own inverse, and its angle from
the pole must be ninety degrees.

Ink, chosen by drawing it over real pages in both themes:

| geometry | ink |
|---|---|
| meridian | solid, 1 px, mid grey — the weight the chart already uses for constellation figures, in a line that is unmistakably straight |
| horizon | **dashed** 6-on 4-off, same weight and grey: a boundary of visibility rather than a thing in the sky |
| zenith | a small open ring with an upward tick — a *place*, not an object, and deliberately not the cross Sprint 24 uses for working marks |

- **Labels are the line's own name**, drawn once where the line
  leaves the paper, in the grid-label grey. They take **no part in
  the star-label collision policy**: reference ink is furniture, and
  a meridian that displaced a star's name would be the observer
  editing the sky.
- **Layering: above the grid and the constellation lines, below
  every catalogued mark and label.** A reference line must never
  hide an object; it exists to be read *across* the chart.
- **Off the page is silence.** On most pages none of the three
  crosses the paper at all — on the released M31 page at the equinox,
  none of them does — and the module draws nothing rather than
  promising a line that is not there.

### What a polar page shows

The meridian passes through the celestial pole **of date**, which in
2026 is **8.80′** from the pole the chart is drawn around, and by 2050
will be 17′. So on a polar page the line passes *beside* the chart's
pole — measured at 6.3′ from it — rather than through it.

That is not a defect to be hidden. It is the J2000 chart and the
observer's own sky drawn honestly on one page, and it is the
clearest argument in this gate for carrying the frames properly.

*(An earlier draft of the study said the meridian "turns" at the
pole. It does not — a great circle is straight here. The kink in the
first rendering was the study's own drawing breaking the line
wherever two samples landed far apart, which at a 4° field was every
segment. #227 inherits that lesson: decide what is on the paper from
the geometry, never from a threshold in pixels.)*

## Calm interaction

- **A frozen snapshot.** The chart is drawn for one instant and
  stays there. No ticking clock, no animation, no automatic
  movement.
- **Two deliberate actions, and no others.** **Now** re-freezes on
  the current moment. **Center on zenith** moves the chart, once,
  because the reader asked — never as a side effect of setting a
  place or a time.
- Changing latitude, longitude or the instant **redraws the lines
  and leaves the page where it is**.

## Where the controls live

**A dialog, opened from the View menu, as Chart Options is.** A
place and an instant are settings, not readings — and the Inspector
is a reading surface with two modes already.

Measured rather than argued: at the Inspector's 240 px floor the
same controls truncate the instant to `2026-03-20 2` and the action
to `Center on…`, while the dialog holds all of it at 18 pt. A third
Inspector mode would also push the mode chooser to three stacked
rows at that width, spending the panel's height on chrome before a
reader has read anything.

## The module seam needs one addition

The seam from #215 carries almost all of this. It needs exactly one
new thing, and the gate names it rather than pretending it does not:

- the module contributes a **great circle** — the one new,
  domain-neutral geometry — for the meridian and the horizon, and
  **`Point`** for the zenith, both in the existing
  **`REFERENCE_LINE`** ink role;
- the chart learns **how to ink that role** — and nothing else. It
  gains no observer, no clock, no longitude, no sidereal time, no
  meridian and no horizon;
- the drawn label is the contribution's existing
  `accessibleName`, so a reader who cannot see the line is told the
  same word the line is drawn with, and no new field is needed;
- **`Center on zenith`** is an ordinary `NavigationRequest`, which
  already carries a reason.

Two things the chart must gain, both rules about geometry and ink
rather than about astronomy:

1. **A great-circle contribution**, clipped analytically as above.
   It is given a pole and a role. It knows nothing of meridians,
   horizons, observers or time, and a future module drawing a
   galactic equator would use the same type.
2. **A reference ink for a contributed point** — the zenith ring —
   because today the chart inks points only in the interaction role,
   as crosses.

## Rejected

- **Plotting sidereal time straight onto the J2000 page.** Rejected
  by measurement: 21′ today, 39′ by 2050, and off the page entirely
  at the narrowest field.
- **Precession without nutation.** Rejected because the residual is
  9″ — visible at the narrowest field — and the twenty-term series
  that removes it costs nothing.
- **Shipping UT1−UTC or a leap-second table.** Rejected: it is data
  with its own provenance and expiry, and the atlas would have to
  refresh it or go stale silently. The price of refusing it is 13.5″,
  and it is stated.
- **A ticking clock.** Rejected: the atlas is a chart, and a page
  that moves while a reader looks at it cannot be read.
- **A third Inspector mode.** Rejected by mock-up at 240 px.
- **Naming the anti-meridian and the nadir.** Rejected: more
  vocabulary than the drawing needs.
- **Contributing the lines as polylines.** Rejected by the cases
  above: a polyline cannot answer a page that lies between its own
  vertices, and specifying a sampling rule would reintroduce every
  problem Sprint 24 spent eight review rounds removing — for
  geometry that does not need sampling at all.
- **A real (terrain or refracted) horizon.** Rejected: the atlas
  knows nothing about air or hills, and a line implying otherwise
  would be a promise it cannot keep.

## What the implementation issues inherit

**#226** — the observer record, GMST/GAST, the precession and
nutation rotations, and the three geometries, all UI-independent;
the 14.22″ contract above as an executable statement; and the oracle
described below.

### What the oracle rests on, and what it does not

The gate review asked for authoritative end-to-end vectors, ideally
from IAU SOFA. **Those could not be obtained here: this work is
done offline, and the atlas takes no network dependency**, so
transcribing SOFA output would mean typing numbers from memory —
which is exactly the way to put an invented constant into an
astronomical model. What stands in their place, and what each
actually proves:

| check | what it would catch |
|---|---|
| Meeus worked examples 7.b, 12.a, 12.b, 22.a | a wrong Julian date, sidereal time or nutation |
| sidereal time by the IAU 2000 Earth rotation angle | a wrong classical polynomial (they agree to 0.28″ and deliberately not to zero) |
| **the pole of date stays exactly one obliquity from the ecliptic pole, at every date** | a wrong matrix order, a transpose, or a sign in the combined rotation |
| **the pole moves at 50.29″·sin ε per year, and keeps going the same way** | a precession applied backwards or at the wrong rate |
| **nutation moves the true pole by between 0.5″ and 10″** | nutation omitted, doubled, or applied in the wrong frame |
| **the equation of the equinoxes equals Δψ cos ε** | GAST and the nutation series disagreeing |
| **every angle between directions survives the transformation** | anything that is not a rotation at all |

The third and fourth are the end-to-end anchors the review asked
for: they constrain the *combined* transformation against known
physics rather than against its own components. **#226 should still
commit SOFA-derived vectors** when a primary source is at hand; this
gate states plainly that it has not done so.

**#227** — the removable module and the chart's reference-line ink:
the vocabulary above, straight-line geometry with no subdivision,
silence where nothing crosses the paper, labels outside the
collision policy, and layering above the grid and below every mark.

**#228** — the dialog: three fields, three visibility switches, and
exactly two actions; east-positive stated; the place remembered and
the instant not.

**#229** — the journey, the packaged evidence, and the handover.
