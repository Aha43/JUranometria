# Putting the ecliptic on the fixed sky

**Sprint 28, issue #271.** The gate for #272–#275. Measured by
`make ecliptic-study`; the numbers quoted here are that study's, and
its report and images reproduce byte for byte.

## The question

The atlas draws one reference circle already — the meridian, which
belongs to a place and a moment. The ecliptic is the opposite kind
of line: it belongs to no observer and no date, and it is the one
circle on the sky that explains why the others move. This gate
decides which ecliptic a fixed J2000 chart should carry, and how to
draw it quietly.

> **The chart stays a chart.** It gains one permanent circle and
> four named points, and it gains no Sun, no Moon, no planet, no
> ephemeris, no season, no zodiac sign, and no ecliptic-longitude
> grid. The chart never learns the word *ecliptic*.

## The frame

The catalogue and the chart are **ICRS/J2000**. There are two circles
one could call "the ecliptic," and they are not the same line:

- the **mean ecliptic of J2000** — a permanent great circle, fixed
  in the chart's own frame;
- the **ecliptic of date**, transformed into J2000 — the plane of
  the Earth's orbit at the moment a reader happens to be looking.

**The chart carries the mean ecliptic of J2000**, because that is
the only one coherent with a fixed atlas. Its defining constant is
the atlas's own obliquity:

- J2000 mean obliquity **ε₀ = 23.4392911°** (84381.448″);
- ecliptic north pole at **RA 270.0000°, Dec +66.56071°** — exactly
  ε₀ from the celestial pole, measured at 23.43929°.

The pole is a direction; nothing about handing the chart a direction
teaches it astronomy. The circle is every point ninety degrees from
that pole.

### Why not the ecliptic of date

The two circles differ in two ways, and the **sizes of those two
differences are the whole of the argument**:

| date | ε(t) | circle: pole shift | equinox: drift from (0h,0°) |
|---|---:|---:|---:|
| 1900-01-01 | 23.45229° | 0.82′ | 1.392° |
| 2000-01-01 | 23.43929° | 0.10′ | 0.004° |
| 2026-03-20 | 23.43588° | 0.36′ | 0.368° |
| 2050-07-04 | 23.43272° | 0.28′ | 0.709° |
| 2100-01-01 | 23.42629° | 0.93′ | 1.398° |

The **plane barely moves**: the of-date pole, transformed into
J2000, stays under an arcminute from the J2000 pole across two
centuries. The **equinox slides** ~50″/yr *along* the circle, so the
of-date vernal equinox drifts more than a degree from (0h, 0°) by
2100.

At chart scale that asymmetry is the decision:

| field | px per degree | 1.4° equinox drift | 0.9′ circle shift |
|---:|---:|---:|---:|
| 8° | 112.5 | 157 px | 1.7 px |
| 18° | 50.0 | 70 px | 0.8 px |
| 36° | 25.0 | 35 px | 0.4 px |

> A naive of-date **line** would look almost right — the circle it
> traces is under a pixel from the true one at every field. What
> gives a wrong frame away is not the line but the **landmark**: an
> equinox mark placed from the wrong epoch lands 157 px off at the
> narrowest field.

So the fixed atlas anchors **both** the circle and its named points
to J2000, and the oracle check below is made at dates far from J2000
— where the line alone would never expose the error.

## The cardinal landmarks

**Four fixed points, named in the interface.** Issue #271 chose
named marks over bare intersections or unlabelled ticks: the value
of the ecliptic to a reader is largely in *where the seasons sit on
the sky*, and a mark with no name teaches nothing.

| landmark | RA | Dec | off-circle |
|---|---:|---:|---:|
| Vernal equinox | 0.000° | +0.0000° | 1.4e-14° |
| Summer solstice | 90.000° | +23.4393° | 1.4e-14° |
| Autumnal equinox | 180.000° | +0.0000° | 1.4e-14° |
| Winter solstice | 270.000° | −23.4393° | 1.4e-14° |

Each lies on the circle to machine precision. The **vernal equinox
falls at exactly RA 0h, Dec 0°** — right ascension is measured from
it, so on a J2000 chart the ecliptic crosses the equator at the RA
origin by definition. The rendered equinox page shows exactly this:
the circle crossing the equator grid line at the labelled mark.

**These are geometry, not the Sun.** The summer solstice is the
point on the sky the Sun *would* occupy at that longitude; it is
fixed in J2000 whether or not the Sun is anywhere near it, and the
atlas states no date, no Sun position, and no season. The
distinction is the whole reason the gate is allowed under the
sprint's own out-of-scope list.

## Agreement with an authority

Invariants constrain this geometry but do not identify it: a sign,
phase, transpose, or epoch error coherent across the obliquity and
the frame rotation satisfies "the points lie on the circle" and
still puts the whole ecliptic in the wrong place. So the model is
held to vectors computed by **IAU SOFA**, release 2023-10-11,
checked in as `docs/studies/ecliptic/reference-vectors.txt` with
their provenance and the program that produced them
(`scripts/ecliptic-vectors.c`, calling `iauObl80` and `iauEcm06`).
Nothing is fetched, compiled, or called: the atlas takes no
dependency on SOFA, at run time or at build time.

> **The obliquity agrees with SOFA to 1.1e-10°**, and the eight
> ecliptic directions to a **worst separation of 0.0403″** — below
> a twentieth of an arcsecond, a fraction of a pixel at the
> narrowest field.

The residual is the ICRS-vs-mean-J2000 frame bias plus the IAU
2006-vs-1980 obliquity difference. `EclipticReferenceVectorTest`
holds the model to these vectors at **0.06″** — half again the
measured residual, room for another machine's last digits and not
for a regression — and separately asserts the ecliptic pole is ε₀
from the celestial pole. The tolerance is derived from the measured
number, not copied from production.

## What it looks like on a page

The chart vocabulary was decided by looking at production-rendered
pages — the real `ChartComponent`, the real overlay registry, the
real reference-ink painter — across both grounds, dense and sparse
fields, the RA wrap, the poles, and narrow and wide fields.

**The geometry needs nothing new.** The ecliptic is drawn by the
same `GreatCircle(pole)` the meridian gate added in #227, clipped
analytically to the paper. Every page confirms it:

| case | page | what it shows |
|---|---|---|
| the crossing at the RA origin | equinox, 24° | circle meets equator exactly at the vernal-equinox mark |
| the northern extremum | solstice, 24° | the summer-solstice mark at the top of the arc |
| the whole arc | wide, 36° | one straight-mapped chord across Pisces–Cetus–Aquarius |
| a dense Milky Way field | dense, 18° | the circle threads Sagittarius past the winter-solstice mark, legible amid catalogue ink |
| named-mark collision | narrow, 8° | the equinox ring and label clear at the tightest field |
| a high-declination page | polar, 8° | the ecliptic is far away and simply absent — no invented chord |
| both grounds | equinox/wide black | palette-owned ink, the same geometry on black |

**But the ink is not sufficient, and this is the gate's one finding.**
The study draws the ecliptic in the existing `Reference.LINE`
treatment — solid, grid-label grey — which is *exactly* the
meridian's. On a page where a reader has both the meridian module
and the ecliptic module active, the two circles are **indistinguishable**.
On the black ground the confusion is worse, because everything
quiet collapses toward the same pale grey.

## The module seam needs one addition

The seam from #215, as amended by #227, carries almost all of this.
It needs **exactly one new thing**, and the gate names it:

> **One more `Reference` kind, visually distinct from the
> meridian's.** Today `Reference` is `LINE` or `BOUNDARY`, both
> inked in grid-label grey (solid and dashed). The ecliptic is
> neither the meridian's sightline nor the horizon's visibility
> edge: it is a **permanent circle of the celestial sphere**, true
> for every observer and every date. That is a statement about what
> the geometry *is* — its relation to the frame — not about what it
> looks like, exactly as `LINE` and `BOUNDARY` are. The chart still
> owns every stroke.

What #273 must settle, by drawing it over real pages as #227 settled
the meridian's stroke:

- the exact stroke that makes the ecliptic unmistakable beside the
  meridian in **both** grounds and at **enlarged** text — a distinct
  dash rhythm or weight within the monochrome palette, never a
  colour;
- the enum identifier, which must stay **domain-neutral**: the chart
  must not gain a value named `ECLIPTIC`, because a future module
  drawing the galactic equator would want the same kind.

Everything else is already present:

- the four landmarks are contributed as **`Point`** in the existing
  **`REFERENCE_LINE`** ink role — the same role the zenith uses;
- the drawn label is the contribution's existing `accessibleName`,
  so a reader who cannot see a mark is told the same word it is
  drawn with, and no new field is needed;
- labels take **no part in the star-label collision policy**:
  reference ink is furniture, and an ecliptic that displaced a
  star's name would be the observer editing the sky;
- **layering: above the grid and constellation lines, below every
  catalogued mark and label** — a reference circle must never hide
  an object.

The chart learns no ecliptic longitude, no obliquity, no seasons, no
zodiac, no Sun, no planets, and no ephemerides. It learns one more
way to ink a circle it is handed.

## The reader surface

**Its own module control**, not Chart Options and not a separate
View surface.

- the ecliptic is a **removable module**, like the meridian — a
  thing a reader turns on to answer a question and off to get a
  quiet chart back. Its switch belongs *with the module*, so that
  removing the module removes its control and leaves no orphaned
  setting behind (this is the module-boundary contract #273 and #274
  inherit);
- **Chart Options is for the chart's own drawing** — palette, grid,
  magnitude, labels — settings that exist whether or not any module
  is loaded. Folding a module's switch in there would tie a
  permanent dialog to an optional feature;
- a **separate View surface** was compared and rejected as more
  chrome than one toggle earns, at ordinary, narrow, and enlarged
  text alike.

**Persistence: the on/off choice is remembered; nothing else is.**
Unlike place-and-time's *instant* — a frozen snapshot that would
come back stale — whether a reader wants the ecliptic shown is a
stable display preference, like the palette or the grid. It is
remembered across sessions. The ecliptic itself has no date and no
observer, so there is nothing else to store. #274 owns the control
and this persistence semantic.

## Rejected

- **The ecliptic of date, transformed into J2000.** Rejected by
  measurement: its circle is under a pixel from the mean ecliptic,
  so it buys nothing a reader can see, while its equinox drifts up
  to 157 px — a landmark placed in the wrong frame, on a chart whose
  whole point is a fixed frame.
- **A zodiac band or ±8° belt.** Rejected as out of scope and as
  unsafe wording: no fixed belt "contains every Solar System body"
  — Pluto alone reaches ~17° of ecliptic latitude. A band would also
  be ornament the sprint explicitly excludes, and it would compete
  with catalogue ink for no cartographic gain.
- **A full ecliptic-longitude grid.** Rejected: it would make the
  chart learn ecliptic coordinates, the one thing the seam exists to
  prevent, and it is named out of scope.
- **Unlabelled ticks or a bare intersection** instead of named
  marks. Rejected: the seasons' places on the sky are the reader's
  reason to look, and an unnamed mark withholds exactly that.
- **A new geometry type.** Rejected as unnecessary: `GreatCircle`
  and `Point` already carry the ecliptic; the only addition is one
  `Reference` kind.

## The contracts the implementation inherits

- **#272 — geometry.** Compute the mean ecliptic of J2000 from the
  atlas's own obliquity: the pole at (270°, 90°−ε₀) and the four
  landmarks at λ = 0, 90, 180, 270 (β = 0). Hold it to the SOFA
  oracle at **0.06″**, the tolerance `EclipticReferenceVectorTest`
  already fixes. No C in `src`; no runtime or build dependency on
  SOFA.
- **#273 — module and ink.** A removable module contributing one
  `GreatCircle` and four `Point` marks in `REFERENCE_LINE`. Add
  **one** domain-neutral `Reference` kind and decide its exact
  chart-owned stroke by drawing it over production pages in both
  grounds and at enlarged text, distinct from the meridian's.
  Layering and label rules as above. The chart gains no astronomy.
- **#274 — controls.** The module's own on/off control, its shown
  state remembered across sessions as a display preference. Removing
  the module removes the control.
- **#275 — journey and handover.** A reader-level journey that turns
  the ecliptic on, reads a named landmark, and turns it off to a
  clean chart; the Sprint 28 handover.

## Structural evidence

This gate changes **no production rendering**. Everything added is a
study tool, a checked-in oracle, and a test:

- `src/juranometria/tool/EclipticStudyMain.java` and
  `EclipticInkStudyMain.java` — study mains, run by `make
  ecliptic-study`;
- `docs/studies/ecliptic/` — the measurements, the SOFA oracle, and
  eight rendered pages;
- `scripts/ecliptic-vectors.c` — the oracle generator, out of the
  build;
- `test/juranometria/sky/EclipticReferenceVectorTest.java` — the
  model held to the oracle.

No `ChartRenderer`, `ReferenceInk`, `OverlayContribution`, or module
source is touched; the eight study pages are classified
`renderer-drawn` by the evidence contracts, alongside the gallery's.
There is **no new runtime network and no new catalogue dependency**,
and the module boundary is **not weakened** — the seam gains nothing
until #273, which the boundary tests will hold. The production
ecliptic does not exist yet, and will not until this gate is
approved and merged.
