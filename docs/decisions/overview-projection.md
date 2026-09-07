# The first overview projection

Sprint 30, issue #296. The gate that decides what belongs beyond the
atlas's honest 42-degree boundary, and what the chart core has to grow
to carry it.

Everything here was decided from `docs/studies/overview-projection/`:
49 real pages of the real sky and the measurements beside them, all
produced by `make overview-study`. **No production behaviour changed
in this issue.** The atlas draws exactly what it drew before.

## The rule this gate settles

> **A module says what belongs on the sky. A projection says how the
> sky becomes a page.**

Recorded in `docs/architecture.md`. It answers a question that would
otherwise have been answered by accident: a projection is not a
chart module, and must not be built as one.

A module contributes domain knowledge as sky geometry — a pole, a
position, a path — and does not know how any of it lands on paper.
Detach it and the page is the same page with less on it. A projection
decides the placement, visibility, curvature, inverse lookup,
navigation and export of *everything* contributed, the modules'
geometry and the catalogue's alike. Change it and every mark moves.
One is an addition to a chart; the other is what a chart is.

The seam already has the right shape, and the study is the evidence:
a module names a great circle by its pole and says what kind of line
it is, and the same pole becomes a straight line, a circular arc or
an ellipse depending on nothing the module knows. Not one line of
`juranometria.module` would change to draw these pages.

## The decisions

### The first overview projection is **stereographic**

Because shape survives it and nothing else does.

| at a 90-degree page corner | scale | shape |
|---|---:|---:|
| gnomonic | +160% | **+61%** |
| stereographic | +28% | **+0.0%** |
| orthographic | −56% | **+125%** |

The stereographic projection's shape column is zero at every field
and every corner, to the precision of the arithmetic. That is what
conformal means and it is the whole argument: a reader matches a
*shape* against the sky. The Plough drawn 60 per cent out of round in
the corner of a page is not a wider view of the Plough, it is a
different asterism. Scale is the price, and it is mild — at 120
degrees a corner degree is half as big again as a centre one, where
the gnomonic projection's is nearly six times.

It also has the domain. The gnomonic projection cannot reach 90
degrees from its centre at any price: a point at 89.9 degrees is 573
plane units out when a whole 42-degree page is 0.77 wide. The
stereographic projection's only singular point is the antipode of the
centre, and a whole hemisphere fits inside a radius of 2.

**The gnomonic projection is not deprecated and does not change.**
It is exactly right for the fields it serves, where it puts every
great circle on a straight line and every telescope field where the
telescope will find it. This is a second projection, not a
replacement.

### Its field is **60, 90 and 120 degrees**, continuing the ladder — provisionally

Not a range, not a new control: three more rungs on the field ladder
the atlas already has, after 42.

**The rungs and the magnitudes below are provisional targets for
#299, not settled numbers.** A review was right that this gate cannot
settle them: the study's pages carry no star labels and use production's
stroke policy nowhere, so they cannot establish how dense a *finished*
overview page reads. What the ink measure below is, honestly, is a
comparative index between projections and between fields on pages
drawn the same way — not an absolute readability threshold.

Two things about it do survive that limitation, and they are the two
ends of the range:

- **Labels can only add ink.** So the 180-degree refusal holds however
  the renderer draws: a page already a third ink before a single name
  is placed does not become readable when the names arrive.
- **60 degrees sits beside the released control.** 5.2% at magnitude
  5.0 against the released page's 5.0% is a like-for-like comparison
  on pages drawn by the same painter, which is what that measure can
  support.

It is 90 and 120, and every magnitude number, that #299 owes a
renderer-drawn confirmation or a revision. **#299 must publish the
same ink measure over real rendered pages with labels, and either
adopt these rungs and defaults or say what it changed them to.**

The upper limit is not the projection's — the stereographic
projection would draw 180 degrees, and the study did. It is the
sky's. Measured as the fraction of the middle half of a page that
carries ink, against the released 42-degree page as the control:

| field | at magnitude 6 | at the magnitude nearest the control |
|---:|---:|---|
| 42° (released control) | 5.0% | — |
| 60° | 7.2% | 5.2% at mag 5.0 |
| 90° | 12.2% | 6.1% at mag 4.0 |
| 120° | 18.5% | 9.1% at mag 4.0 |
| 180° | 32.3% | 15.3% at mag 4.0 |

A 180-degree page is a third of the paper under ink at the default
magnitude, and still a sixth of it with everything fainter than
fourth magnitude removed. It is drawable and it is not readable, so
it is not offered. The floor those last two rows are sitting on is
not stars: it is constellation figures, boundaries and grid, which
magnitude does not thin. At 120 degrees the furniture alone approaches
twice the released page's entire ink.

**Provisional contract for #299:** the default limiting magnitude
follows the field — about 5.0 at 60 degrees and 4.0 at 90 and 120 —
so that an overview arrives readable rather than arriving at the
default and needing rescue. These are starting points to be measured
against, not constants to be copied in. The reader's existing
magnitude control is unchanged and still wins.

### The projection interface

```java
public interface Projection {
    String name();
    SkyPosition centre();
    Optional<PlanePoint> project(SkyPosition position);
    Optional<SkyPosition> unproject(PlanePoint point);
    double planeRadius(double angleDegrees);
    double limitDegrees();
    Optional<PlaneCurve> greatCircle(SkyPosition pole);
}
```

Seven methods, and each earned its place by something in the study
being impossible without it.

The last one was absent from this gate's first proposal, and a review
was right that its absence was the whole problem. A projection that
only maps points cannot tell #298 what a great circle became. The
caller would have to project a few hundred points and fit a form to
them — which is **sampling**, the compromise this issue refuses —
then ask which form came back, which is a **type check**, and then
one day meet a projection needing a form nobody had written, which is
**widening the vocabulary later**.

A projection knows the answer analytically. Write the pole in the
projection's own frame, as its components `a`, `b`, `c` along the
centre, east and north directions — the same three dot products
`project` already takes — and the condition that a point lies on the
circle becomes an equation in page coordinates:

| projection | substituting its own radius | leaves |
|---|---|---|
| gnomonic | `r = tan t` | `a + b·ξ + c·η = 0`, a line |
| stereographic | `r = 2 tan(t/2)` | a circle of centre `(2b/a, 2c/a)` and radius `2/|a|`; a line when `a` is zero |
| orthographic | `r = sin t` | `(a²+b²)ξ² + 2bc·ξη + (a²+c²)η² = a²`, an ellipse of radii `|a|` and 1 |

Each projection returns the **simplest form that is exact**, so one
curve has one name: a stereographic circle of infinite radius is a
line, and an orthographic ellipse with equal axes is a circle.

`PlaneCurve` is used polymorphically — the caller draws `shape()` and
clips with `clipTo(rectangle)` and never asks which of the three it
holds. Because all three forms exist now, #301 adds none.

`project` and `unproject` are `Optional` because a projection has a
domain: the orthographic hemisphere and the gnomonic 90-degree limit
both return empty, and a page draws nothing rather than drawing a
guess. The existing `GnomonicProjection.project` already returns
`Optional`; `unproject` is today a static method on `PanSolver` and
becomes the projection's own.

`planeRadius(angle)` is the one that is easy to leave out and cannot
be. It is how far from the centre a point that far from the centre
lands, and it is what a mapping needs to size a page —
`width / (2 · planeRadius(field/2))`. Production writes that rule as
`width / (2 tan(field/2))`, which is this method's gnomonic body
inlined into `ViewportMapping`, and it is also why that class refuses
a field of 180 degrees or more: not because a page cannot hold one,
but because `tan` cannot.

`limitDegrees` is how far the projection reaches at all — 90 for
gnomonic and orthographic, 180 for stereographic — and it is what
lets a page ask whether a field is possible before trying to draw it.

The study's `StudyProjection` is this interface, and all three
candidates implement it. The gnomonic one delegates to production's
own `GnomonicProjection` and `PanSolver`, and the study measured that
it agrees with production to **0.000e+00** — the same plane point,
not a near one. The stereographic candidate agrees to 1.137e-13 with
the one the Sprint 29 gate wrote from the projection's closed form —
a different derivation reaching the same numbers. All three
round-trip sky → plane → sky to within 3.3e-13 degrees.

### The projected-curve vocabulary

Three words, all exact, no sampling:

| word | what takes it |
|---|---|
| **straight** | every gnomonic great circle; any circle through the page centre under the other two |
| **circular** | every other stereographic great circle |
| **elliptical** | every orthographic great circle, and nothing else |

Over the 108 page-and-circle combinations measured, every one is
drawn by one of these three — 66 straight, 25 circular, 17 elliptical
— stated by the projection and never fitted.

The fit is kept as the **check**. Two independent routes to the same
curve, one from the projection's algebra and one from several hundred
points it actually projected, agree on the form in **108 of 108**
cases, and the drawn curve passes through those points to within
3.1e-08 page units against an acceptance threshold of 1e-3.

Two things this vocabulary can say that production's cannot, both
found by clipping real pages rather than by reasoning about them:

**A curve can cross a page more than once.** A circle and a rectangle
meet in up to **eight** points — two per edge — which is up to four
separate runs of curve on the paper. Twenty-one of the 108
combinations leave the page and come back, and four of them come back
in four runs. Production's
`GreatCirclePage.clip` returns `Optional<Arc>`, which can only answer
"once" or "not at all", so it would draw one piece and silently drop
the rest. **The replacement returns a list of runs.**

**A curve can close.** A great circle wholly inside the paper has no
ends, and `ReferenceInk`'s rule of naming a line where it leaves the
page has nothing to hang on. No page at or below 180 degrees closes —
the pole-centred stereographic equator first closes at a field of
208.5 degrees — but at 180 degrees it is exactly tangent to the left
and right edges, which is one rounding error away, and that rounding
error is what made the study's own first clipper report the same
circle in three pieces on one side of a page and one on the other.
**A run therefore has optional ends, and a closed run is named
elsewhere or not at all.**

Clipping needs no sampling and no new geometry. A straight run is
Liang–Barsky. A circular run is the crossings of a circle with four
edges, sorted, with the pieces between them joined up. An elliptical
run is the same arithmetic after one affine change of variables, in
the frame where the ellipse is a unit circle.

### The orthographic globe stays issue #301

Three measured reasons, and none of them is that it was hard.

**It is the worst of the three at every field**, at both costs at
once: +125% shape and −56% scale at a 90-degree corner. It should
not be called undistorted. It preserves the visual logic of a globe —
which is a real thing to teach, and why #301 exists — while
foreshortening everything toward the limb and showing one hemisphere.

**It cannot be pointed at.** A page unit at the corner of a 90-degree
orthographic page covers twelve arcminutes of sky against four
arcminutes at the same page's centre, and it goes on coarsening all
the way to the limb, where it stops meaning anything at all. The two chart projections
magnify their corners instead, so pointing gets no harder as the page
widens. A globe is a thing to look at, not a thing to point at.

**It is sized by a different rule.** Every page in the atlas shows
half its field across half its width. A hemisphere is a disc, and on
a landscape page a disc sized by the width runs off the top and
bottom at *every* field; the whole disc only fits if the scale is set
by the page's short side, leaving 200 units of a 900-wide page empty
on either side.

What the study bought by taking it seriously anyway is the third
word. The elliptical run is written, measured and drawn on 17 of the
study's 108 combinations, so #301 is an **addition** rather
than a redesign of it — one record and one clipping rule, with
nothing that already existed changed to admit it. That is what the
milestone meant by studying the globe from the beginning so the seam
could not accidentally fit stereographic alone.

### Navigation: another zoom step, not a new mode

The overview is the field ladder continued. Zoom, pan and recentre
keep working because they are the same operations:

- **recentre** is `unproject` of the clicked point, which every
  candidate provides and all three round-trip to 3.3e-13 degrees;
- **pan** is recentre with a dragged point;
- **transition back to a detailed page** is a narrower rung, and the
  projection changes with it — silently, because the centre and the
  field carry across and nothing a reader chose is lost.

Which projection draws which rung is a property of the field, not a
setting. **There is no projection menu**, now or later. A reader who
wanted "the stereographic view" would be a reader who had been told
about a problem they do not have.

### Grid, labels, symbols and identity

**The grid stays sampled.** A meridian is a great circle and a
parallel is not, so no vocabulary of exact forms covers a graticule;
production already samples both at `field/180` and records its worst
chord error, and this gate found no reason to change that answer. The
exact vocabulary is for the modules' reference circles, which is
exactly where production is exact today.

**Reference labels need a collision rule.** Production has never
needed one: at 42 degrees no page carries more than two reference
lines. An overview page carries four, and they leave the paper near
the same corner — the study's first pages wrote "the celestial
equator" and "a meridian" through each other, and it stacks them now.
The star-label collision policy is unchanged and unaffected.

**Symbols do not scale with the field and do not rotate.** A star's
size means its magnitude and a deep-sky symbol's size means its
extent; neither is a statement about the projection, and a symbol
turned to follow the local north would be telling a reader something
about the paper while looking like something about the sky.

**A page says which projection drew it** — in the title block, in the
accessible description, and in the exported sheet's metadata. Two
sheets of the same centre and field drawn by different projections
are different documents, and a sheet that did not say which one it
was would be a sheet that could not be checked.

## Contracts for the rest of Sprint 30

- **#297 — the projection seam.** Introduce the interface above with
  gnomonic as its only implementation, and move the 25 in-place
  constructions across 7 files behind it. Production pages must come
  out byte for byte identical; the study already measures the
  gnomonic study projection against production at 0.000e+00.
  `ViewportMapping`'s scale becomes `width / (2 · planeRadius(field/2))`
  and its 180-degree refusal moves to the projection.
  `SceneAssembler.queryRadiusDegrees` must ask the projection for its
  page corner rather than computing a gnomonic one.
  Measured over Orion, a 120-degree stereographic page reaches 72.4
  degrees where the gnomonic rule fetches 65.5, and **2,402 catalogue
  objects** fall in the ring between. They would not be drawn faintly
  or badly. They would be absent, in the corners, where nothing looks
  wrong.

- **#298 — the curve vocabulary.** Replace `GreatCirclePage.clip`'s
  `Optional<Arc>` with the three exact forms and a list of runs with
  optional ends. The forms come from `Projection.greatCircle(pole)`
  in closed form — no sampling, and no caller asking which form it
  got. `ReferenceInk` draws each run's own shape and names the curve
  at an end when it has one.

- **#299 — the overview page.** Stereographic at 60, 90 and 120
  degrees as three more rungs, with the field-linked default
  magnitude above. Recentre, pan and the transition back to a
  detailed page, all through `unproject`. **It also owes the
  confirmation this gate could not give**: the same ink measure over
  real renderer-drawn pages, with labels and the renderer's own
  stroke policy, either adopting the rungs and defaults above or
  stating what it changed them to and why.

- **#300 — identity.** The projection's name in the title block, the
  accessible description and the sheet metadata.

- **#301 — the globe.** Unchanged, and now known to be an addition
  rather than a redesign.

## What this gate did not settle

The study's pages were drawn by a study painter, not by
`ChartRenderer`, because the renderer cannot be asked for another
projection — which is the finding, not a shortcut. They are evidence
about **geometry**: what the sky looks like under each projection,
what a curve becomes, what a page asks for. They are not evidence
about the finished chart's ink weights or label density at an
overview field, because the study draws no star labels and does not
use the renderer's stroke policy. Those belong to #299.

Nor has any of this been read at a telescope, or on paper. Issue
#293 still owns the paper.
