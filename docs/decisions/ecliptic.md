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

Both candidates are **mean**: the mean ecliptic and equinox of J2000,
and the mean ecliptic and equinox of date. No nutation is applied to
either, because nutation belongs to neither.

**Both are read from SOFA, not computed by the atlas.** An earlier
draft of this gate built the of-date pole and equinox in mean
coordinates and passed them to `SkyFrame.toJ2000`, whose input is a
*true* direction and which therefore strips a nutation that was never
applied — adding a rotation belonging to neither candidate, and
measuring the comparison with production's own transformation instead
of the independent authority (PR #276 review). SOFA's `iauEcm06` now
supplies each date's ecliptic frame directly in ICRS.

| date | ε(t) | circle: pole offset | equinox: offset from J2000 equinox |
|---|---:|---:|---:|
| 1900-01-01 | 23.45229° | 0.78′ | 1.397° |
| 2000-01-01 | 23.43928° | 0.00′ | 0.000° |
| 2026-03-20 | 23.43587° | 0.21′ | 0.366° |
| 2050-07-04 | 23.43271° | 0.40′ | 0.706° |
| 2100-01-01 | 23.42627° | 0.78′ | 1.397° |

At J2000 both offsets are exactly zero — the of-date candidate *is*
the J2000 candidate there — which is the check that the two are being
read in one frame.

**The dates are UTC; SOFA's `iauEcm06` and `iauObl06` take TT.** No
conversion is applied, and the price is measured rather than left to
look like it was not there: advancing every date by the present
TT−UTC of 69.184 s moves these directions by at most **0.000110″** —
about 540 times below the 0.06″ the implementation is held to, and
far below a pixel at any field (PR #276 round 2). The generator
computes this and the fixture records it.

The **plane barely moves**: the of-date pole stays under an arcminute
from the J2000 pole across two centuries. The **equinox slides**
~50″/yr *along* the circle, past a degree by 2100.

### What that is worth on the paper

Measured through the atlas's own `GnomonicProjection` and
`ViewportMapping` at the positions concerned, on the 900×700 page the
study pages use. An earlier draft used a flat `900 / field`, which is
neither the gnomonic scale nor the mapping production applies
(PR #276 review).

**Each is measured on the page where it is worst, and they are not
the same page.** The two circles *cross* at the equinoxes, so an
equinox page is the one place the lines coincide; they are furthest
apart a quarter turn away, at the solstices.

| date | circle, June solstice page: 8° | 18° | 36° | March equinox mark: 8° | 18° | 36° |
|---|---:|---:|---:|---:|---:|---:|
| 1900-01-01 | 1.5 px | 0.6 px | 0.3 px | 157 px | 69 px | 34 px |
| 2000-01-01 | 0.0 px | 0.0 px | 0.0 px | 0 px | 0 px | 0 px |
| 2026-03-20 | 0.4 px | 0.2 px | 0.1 px | 41 px | 18 px | 9 px |
| 2050-07-04 | 0.7 px | 0.3 px | 0.2 px | 79 px | 35 px | 17 px |
| 2100-01-01 | 1.5 px | 0.6 px | 0.3 px | 157 px | 69 px | 34 px |

> **The circle is not identical**, and an earlier draft overstated it
> as "under a pixel at every field" (PR #276 review). At its worst
> page and narrowest field the two circles are about a pixel and a
> half apart — small, but ink rather than nothing. The **equinox mark
> is two orders of magnitude worse**: 157 px.

That *ratio* is the decision, not either number alone. A reader could
not tell the two lines apart and could not miss the two marks being
apart. So the fixed atlas anchors **both** the circle and its named
points to J2000, and the oracle check below is made at dates far from
J2000 — where the line alone would never expose the error.

## The cardinal landmarks

**Four fixed points, named in the interface.** Issue #271 chose
named marks over bare intersections or unlabelled ticks: the value
of the ecliptic to a reader is largely in *where the equinoxes and
solstices sit on the sky*, and a mark with no name teaches nothing.

| landmark | RA | Dec | off-circle |
|---|---:|---:|---:|
| March equinox | 0.000° | +0.0000° | 1.4e-14° |
| June solstice | 90.000° | +23.4393° | 1.4e-14° |
| September equinox | 180.000° | +0.0000° | 1.4e-14° |
| December solstice | 270.000° | −23.4393° | 1.4e-14° |

**Named by month, not by season.** "Summer solstice" reverses for a
southern reader, and "vernal" carries the same northern assumption,
while this geometry has no observer at all (PR #276 review). The
month names are true everywhere, and the atlas has no business
telling a reader in Melbourne that June is their summer.

Each lies on the circle to machine precision. The **March equinox
falls at exactly RA 0h, Dec 0°** — right ascension is measured from
it, so on a J2000 chart the ecliptic crosses the equator at the RA
origin by definition. The rendered equinox page shows exactly this:
the circle crossing the equator grid line at the labelled mark.

**These are geometry, not the Sun.** The June solstice is the
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
| the crossing at the RA origin | equinox, 24° | circle meets equator exactly at the March-equinox mark |
| the northern extremum | solstice, 24° | the June-solstice mark at the top of the arc |
| the whole arc | wide, 36° | one straight-mapped chord across Pisces–Cetus–Aquarius |
| a dense Milky Way field | dense, 18° | the circle threads Sagittarius past the winter-solstice mark, legible amid catalogue ink |
| named-mark collision | narrow, 8° | the equinox ring and label clear at the tightest field |
| a high-declination page | polar, 8° | the ecliptic is far away and simply absent — no invented chord |
| both grounds | equinox/wide black | palette-owned ink, the same geometry on black |

**But both existing treatments are wrong for it, and that is the
gate's substantive finding.** The eight pages draw the ecliptic in the
existing `Reference.LINE` and `Point` treatments, and they show why
neither can stay:

- the **line** is solid grid-label grey, which is *exactly* the
  meridian's stroke. `candidate-line-solid.png` puts both on one
  page: two identical greys crossing at right angles, told apart
  only by their labels. On the black ground it is worse, because
  everything quiet collapses toward the same pale grey.
- the **marks** are the zenith's: `ReferenceInk` draws a contributed
  point as a small open ring *with an upward tick*, and its own
  comment says the tick is there "because a place overhead has a
  direction". An equinox is not a place overhead. Sharing a Java
  type does not make that cartographic meaning generic
  (PR #276 review).

### The candidates, and what was chosen

An earlier draft proved the existing line wrong and then left the
replacement to #273. That was refused, rightly: the gate is where
the line vocabulary is decided, and #273 should implement a reviewed
contract rather than reopen the question while changing production.
So the candidates were drawn — on the page where the two lines
actually cross, in a dense Sagittarius field, in both grounds
(`make ecliptic-study`, `candidate-line-*.png`).

**They are drawn in production's own reference layer.** A first
version of the study painted them *after* a completed chart, which
put the candidate above every star, symbol and label — so the pages
answered the opposite of the question they were being used to settle:
in production a catalogued mark covers the reference line, and there
the line covered the mark (PR #276 round 2). The study now drives
`ChartRenderer.render` and fills its `ReferenceLayer`, the same
boundary `ChartComponent` hands to `ReferenceInk` — above the grid
and the geography, below every mark and label — with the production
meridian inked in that layer beside the candidate. The verdicts
below are from the corrected pages.

| candidate | verdict |
|---|---|
| solid (the existing `LINE`) | **rejected** — identical to the meridian |
| dashed 6-on 4-off (the existing `BOUNDARY`) | **rejected** — it is the horizon's, and means "the boundary of what can be seen" |
| long dash 12-on 6-off | distinguishable, but reads as a plain dashed line — easily confused with the horizon at a glance |
| fine dotted 2-on 4-off | **rejected** — the chart already draws constellation *boundaries* fine-dotted; on the rendered page the two are the same line |
| **dash-dot 12-4-2-4** | **chosen** |

> **Dash-dot**, one pixel, in the same grey. It is the cartographic
> convention for a *datum* line — a construction of the frame rather
> than a feature of the terrain — and it is unmistakable beside all
> three lines it must not be confused with: the meridian's solid, the
> horizon's even dash, and the constellation boundaries' fine dots.
> Verified on both grounds.

And for the marks (`candidate-mark-*.png`):

| candidate | verdict |
|---|---|
| ring with upward tick | **rejected** — the zenith's own symbol |
| plain ring | **rejected** — a ring alone is the lead-selection ring, and reads as an object |
| **open diamond** | **chosen** |

> **A small open diamond.** No other mark on the chart is a diamond:
> stars are filled discs, deep-sky objects are ellipses, dotted and
> crossed circles, boxes and spoked squares, working marks are a
> gapped cross, the zenith is a ring and tick. It reads as a marked
> *position on a line* rather than a place or an object.

**Enlarged application text does not reach the chart.** The two pages
rendered at 12 pt and 18 pt application text are **byte-identical**,
because chart faces are the renderer's own fixed fonts — the same
property the black-sky gate established. Enlarged text matters for
the control, and it is shown there.

## The module seam needs two additions

An earlier draft promised "exactly one seam addition" while its own
study demonstrated a second missing distinction. There are **two**,
and the gate names both rather than discovering the second during
implementation:

> **1. One more `Reference` kind, for a permanent circle of the
> celestial sphere.** Today `Reference` is `LINE` or `BOUNDARY`. The
> ecliptic is neither the meridian's sightline nor the horizon's
> visibility edge: it is a circle true for every observer and every
> date. That is a statement about what the geometry *is* — its
> relation to the frame — exactly as `LINE` and `BOUNDARY` are, and
> not about what it looks like. The chart inks it dash-dot.
>
> **2. A kind on `Point`, distinguishing a place from a landmark.**
> `PLACE` is a direction an observer stands under or faces, and has
> an up — the zenith, unchanged, ring and tick. `LANDMARK` is a
> distinguished position *on* a reference line, with no orientation —
> the chart inks it as an open diamond.

Both identifiers must stay **domain-neutral**: no value named
`ECLIPTIC`, `EQUINOX` or `SOLSTICE`, because a module drawing the
galactic equator and marking the galactic centre would want exactly
these two kinds. The chart still owns every stroke; it is told what
the geometry is, never what to draw.

Everything else is already present:

- the geometry is the existing **`GreatCircle`** and **`Point`**, in
  the existing **`REFERENCE_LINE`** ink role — no new geometry type;
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
way to ink a circle and one more way to ink a point.

## The reader surface

> **A checkbox item named `Ecliptic` on the View menu**, directly
> below the Inspector's — which is already exactly this: a
> `JCheckBoxMenuItem` whose tick tells the reader whether the thing
> is showing.

An earlier draft said only "its own module control", which names an
owner rather than a place a reader can reach, and left #274 to make a
product decision (PR #276 review). The home is now chosen and drawn
(`controls-view-menu*.png`).

Why the menu:

- **The ecliptic has no settings.** No observer, no instant, nothing
  to type — the geometry is the same for everyone, forever. A dialog
  would be a window built around a single checkbox. This is the whole
  difference from place-and-time, which needed a dialog because it
  had a latitude, a longitude and an instant to hold.
- **A menu cannot be truncated by a narrow window.** A popup is
  sized by its own content, not by the window it hangs from. That is
  a structural property, and it is precisely what disqualified the
  Inspector for place-and-time at its 240 px floor: the Inspector is
  a panel *inside* the window and inherits its width, and a menu is
  not. No measurement of the window can make a menu item narrower.

  *The study's pixel numbers are not the View popup's.* Its images
  are arrangement mock-ups — production item classes, with the
  neighbours' real accelerators, but laid out in a `JPanel` rather
  than by the menu UI, which owns its own check-icon and accelerator
  columns, insets and separator metrics; a popup paints nothing until
  it is shown (PR #276 round 2). The images show the arrangement and
  the tick states; the no-truncation claim rests on the structural
  reason above, not on them. **#274 owes a real-menu test**: both
  themes, enlarged text, nothing clipped.
- **Its state is visible.** A tick shows whether the ecliptic is on
  without the reader having to look at the chart and guess.
- **Chart Options is for the chart's own drawing** — palette, grid,
  magnitude, labels — settings that exist whether or not any module
  is loaded. Folding an optional module's switch into a permanent
  dialog would tie the two together.
- **A separate View surface** was rejected as more chrome than one
  toggle earns, at every text size.

Shown at ordinary and enlarged text in both application themes:
`controls-view-menu.png`, `controls-view-menu-enlarged.png`,
`controls-view-menu-dark.png`, `controls-view-menu-dark-enlarged.png`
— all four with the ecliptic **shown**, a reader-selected state.
`controls-view-menu-default.png` shows the **released default**,
which is not that.

### The released default: hidden

> **A reader who has never asked for the ecliptic does not get it.**
> On a fresh install the item is present and unticked, and the chart
> is the chart it has always been.

An earlier draft specified how an existing value survives the
module's absence but never said what a fresh store means, which left
#274 unable to implement restart semantics (PR #276 round 2). It is
decided here, and the atlas has already decided it once: the meridian
module is attached at startup and immediately told
`showing(false, false, false)`, so a reader who opens the atlas for
the first time sees no reference lines at all.

The reasons are the same ones, and one more that matters for a
removable module:

- **the default page is the quiet chart.** The ecliptic is a thing a
  reader turns on to ask a question; the document says so, and a
  default that turned it on would contradict it.
- **installing a module must not redraw everyone's chart.** With the
  module absent there is no ecliptic. If the module defaulted to
  *shown*, merely adding it would change the default page for every
  reader who never asked — the module would be making a decision
  about the atlas rather than offering one to the reader.
- **off is recoverable in one click; on is a surprise.** A reader who
  wants it finds a ticked box where they expect it. A reader who does
  not want it never had to discover why a new line appeared.

### Persistence, including when the module is absent

**The on/off choice is remembered; nothing else is.** Whether a
reader wants the ecliptic shown is a stable display preference, like
the palette or the grid — unlike place-and-time's *instant*, a frozen
snapshot that would come back stale. The ecliptic has no date and no
observer, so there is nothing else to store.

**Three states, and they stay distinct:**

| store | means | drawn |
|---|---|---|
| key absent | the reader has never chosen | no — the released default |
| explicit `false` | the reader chose to hide it | no |
| explicit `true` | the reader chose to show it | yes |

The absent key and the explicit `false` are **not collapsed**, even
though both render the same today: a reader's stated choice must
outlive a change of default, and it is what lets a removal and a
return preserve intent rather than silently resetting it. So the
preference is read as an optional value, never as a boolean with a
default baked into the read.

The module is removable, so the gate states what happens when it is
not there:

- **the menu item exists only while the module is loaded.** No
  module, no control — a tick for something that cannot be drawn
  would be a promise the atlas cannot keep;
- **the remembered value survives the module's absence and is
  honoured when it returns.** It is a reader's stated preference,
  not module state, so a build without the module leaves it
  untouched rather than clearing it;
- **an absent module reads nothing and writes nothing.** The
  preference is only consulted when a module is present to act on
  it, so an absent module cannot silently rewrite a reader's choice
  to a default.

#274 owns the control and these semantics.

## Rejected

- **The ecliptic of date, expressed in J2000.** Rejected by
  measurement: at its worst page and narrowest field its circle is
  about 1.5 px from the mean ecliptic of J2000 — nothing a reader
  would read as a different line — while its equinox mark lands up
  to 157 px away. It buys no visible fidelity and moves the one
  thing a fixed atlas must hold still.
- **A zodiac band or ±8° belt.** Rejected as out of scope and as
  unsafe wording: no fixed belt "contains every Solar System body"
  — Pluto alone reaches ~17° of ecliptic latitude. A band would also
  be ornament the sprint explicitly excludes, and it would compete
  with catalogue ink for no cartographic gain.
- **A full ecliptic-longitude grid.** Rejected: it would make the
  chart learn ecliptic coordinates, the one thing the seam exists to
  prevent, and it is named out of scope.
- **Unlabelled ticks or a bare intersection** instead of named
  marks. Rejected: where the equinoxes and solstices sit on the sky
  is the reader's reason to look, and an unnamed mark withholds
  exactly that.
- **Seasonal landmark names** (vernal/summer/autumnal/winter).
  Rejected: they reverse for half the world, and this geometry has
  no observer to have a season.
- **Reusing the existing `BOUNDARY` dash** for the ecliptic.
  Rejected: it is distinguishable from the meridian, but it means
  "the boundary of what can be seen", which the ecliptic is not —
  and it is the horizon's, so the two would collide the moment both
  modules were on.
- **A new geometry type.** Rejected as unnecessary: `GreatCircle`
  and `Point` already carry the ecliptic; the two additions are
  kinds on the geometry that exists, not new geometry.

## The contracts the implementation inherits

- **#272 — geometry.** Compute the mean ecliptic of J2000 from the
  atlas's own obliquity: the pole at (270°, 90°−ε₀) and the four
  landmarks at λ = 0, 90, 180, 270 (β = 0), named **March equinox,
  June solstice, September equinox, December solstice**. Hold it to
  the SOFA oracle at **0.06″**, the tolerance
  `EclipticReferenceVectorTest` already fixes. No C in `src`; no
  runtime or build dependency on SOFA.
- **#273 — module and ink.** A removable module contributing one
  `GreatCircle` and four `Point` marks in `REFERENCE_LINE`. Add the
  **two** domain-neutral kinds named above, and ink them as chosen
  here: the circle **dash-dot 12-4-2-4**, one pixel, in the figure
  grey; a landmark as a **small open diamond**. Both are inked in the
  renderer's existing `ReferenceLayer` — above grid and geography,
  below every mark and label — which is where the gate's candidate
  pages drew them. The chart gains no astronomy, and no enum value
  names the ecliptic.
- **#274 — controls.** A `JCheckBoxMenuItem` named **Ecliptic** on
  the View menu, below the Inspector's, present only while the module
  is loaded. **Released default: hidden.** The choice is remembered
  across sessions as a display preference and read as an optional
  value, so a missing key, an explicit `false` and an absent module
  stay distinguishable; an absent module neither reads nor writes it.
  Owes a **real-menu test** — a shown popup, both themes, enlarged
  text, nothing clipped — since the gate's control images are
  arrangement mock-ups.
- **#275 — journey and handover.** A reader-level journey that turns
  the ecliptic on from the View menu, reads a named landmark, and
  turns it off to a clean chart; the Sprint 28 handover.

## Structural evidence

This gate changes **no production rendering**. Everything added is a
study tool, a checked-in oracle, and a test:

- `src/juranometria/tool/EclipticStudyMain.java`,
  `EclipticInkStudyMain.java`, `EclipticCandidateStudyMain.java` and
  `EclipticControlStudyMain.java` — study mains, run by `make
  ecliptic-study`;
- `docs/studies/ecliptic/` — the measurements, the SOFA oracle, the
  eight production-ink pages, the ink candidates and the control
  mock-ups;
- `scripts/ecliptic-vectors.c` — the oracle generator, out of the
  build;
- `test/juranometria/sky/EclipticReferenceVectorTest.java` — the
  model held to the oracle.

No `ChartRenderer`, `ReferenceInk`, `OverlayContribution`, or module
source is touched. The chart pages — the eight production-ink ones
and the eighteen candidates — are classified `renderer-drawn` by the
evidence contracts, alongside the gallery's; the four control
mock-ups are `widget-rendered-inspection`, as every control mock-up
before them.

Two pins moved, both by the route the gates define rather than around
it:

- **the widget-photographer list gains two entries.** The evidence
  gate pins every study main that sets a process-wide font, "so the
  next one arrives by decision, not a habit". The control mock-up is
  one by construction; the candidate study is one because it sets the
  font *to prove enlarged text does not reach the chart* — the two
  pages it renders that way are byte-identical, which is the
  evidence.
- **the test-evidence report is regenerated**, as it is whenever
  study output is added.

The oracle's provenance header is now **emitted by the generator**
rather than added to the file by hand. Regenerating it had silently
dropped the licence terms and the source digest, which
`EclipticReferenceVectorTest` caught; a fixture whose terms can be
lost by re-running the command that makes it is not reproducible.

There is **no new runtime network and no new catalogue dependency**,
and the module boundary is **not weakened** — the seam gains nothing
until #273, which the boundary tests will hold. The production
ecliptic does not exist yet, and will not until this gate is
approved and merged.
