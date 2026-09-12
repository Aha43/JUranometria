# How a reader uses an orthographic celestial globe

Sprint 32, issue #301. **In progress**: this records the decisions the
gate has taken so far, and is not yet the finished contract. Density,
families, label counts, boundaries, navigation and export are still
open.

Sprint 30 settled the geometry in
[the overview-projection decision](overview-projection.md) —
orthographic, one visible hemisphere, a real limb, exact
project/unproject, elliptical great circles, short-side sizing — and
this does not reopen any of it. What it decides is how that truthful
and strongly foreshortened globe becomes a page a reader can use.

## The globe is a bounded object placed on paper

Every other page in the atlas is sized by one rule: **half the field
across half the page width**. A globe cannot be, and this is the first
decision the gate takes.

A hemisphere is a disc. Sized by the width it runs off the top and
bottom of a landscape page at every field. Sized to fill the short
side exactly it touches the paper on two sides, and — measured on the
candidates, not predicted — its limb is **flattened against the page
edges**, so it stops being a circle, which is the one thing a globe's
outline has to be. It also leaves nowhere for the title block but on
top of the sky.

So:

> **A globe page is a centred circular disc at 90% of the page's short
> side.**

- **Fixed cartography, not a reader's preference.** The atlas has no
  other page-geometry setting, and the gate refuses a new preference
  unless the study shows the existing model cannot tell the truth
  without one. It can.
- **The scale does not depend on the furniture.** A globe that changed
  size when the magnitude key was switched on would be a globe whose
  scale meant nothing.
- **The border is where the furniture lives** — the generous side
  gutters of a landscape page, the space above and below on a portrait
  one — rather than over the celestial sphere.
- **The page corners remain paper.** They are never an extension of
  the sky.

Compared at 100%, 94%, 90% and 86% of the short side, across three
containers and three furniture states, on the crowded hemisphere
because a border that survives the worst page is a border that works
(`make globe-frame-study`). 100% is not "no margin" but a clipped
globe; 94% is the first size that closes the circle; 90% leaves a
quiet border of 5% on each side of the limiting dimension — about
10.5 mm on A4 — without making the sphere feel small.

**This rule belongs to the orthographic globe alone.** Gnomonic and
stereographic pages are rectangular charts whose field fills the map
frame. The implementation reaches only projections that have a limb,
so the two chart projections take the branch they always took and draw
what they always drew.

## An extended object describes a sky footprint

Found by looking at a candidate page: the Large Magellanic Cloud drawn
as a grey ellipse half on the sky and half on the paper, hanging over
the limb.

The atlas turns an object's angular axes into page units at the **page
centre's** rate:

```java
arcminToPx(arcmin, pixelsPerPlaneUnit)
        = radians(arcmin / 60.0) * pixelsPerPlaneUnit
```

That is right at the centre of any azimuthal projection, where the
scale is one plane unit per radian, and near enough anywhere on a page
no wider than 120 degrees. A globe's radial scale is `cos(theta)`, and
at the cloud's 82.2 degrees that is **0.135** — so it is drawn about
seven times too wide across the radius.

Measured on the crowded hemisphere, in plane units where the limb is
exactly 1:

| | furthest point of the outline |
|---|---|
| the cloud's projected footprint | **0.9991** — inside the limb |
| its axes scaled at the page centre | **1.0842** — 8% past the limb, on paper |

**The cloud is not crossing the limb.** Its centre is 82.2 degrees
out, its half-extent is 5.38 degrees, so its outermost sky reaches
87.5 degrees with two and a half to spare. It is only *drawn* across
it.

> **The centre-scale conversion is forbidden for the orthographic
> globe.** An extended object's angular outline, axes and position
> angle are transformed through the page's own projection: the outline
> is walked in the sky and each point projected.

The Large Magellanic Cloud is the required fixture, because a local
uniform scale is visibly and substantially wrong there. A local
Jacobian approximation may be proposed for small objects and compared
against the projected outline; **for an object as large as the cloud
the projected outline is the authority**, and scaling its axes at the
centre is not an acceptable approximation of it.

This also produces the visual cue that says the page is a sphere: near
the limb a circular object is radially squashed, because that is what
a circle on a sphere seen edge-on looks like.

## The limb clips, and does not erase

An object whose centre is visible may still have part of its footprint
on the far side, and an object 89 degrees out with a 4-degree extent
genuinely straddles the boundary. Measured, that fixture keeps **239
of 360** outline points on the visible hemisphere: the visible part is
real sky, and a rule that dropped the object would be throwing it
away.

| what the footprint does | what the page does |
|---|---|
| lies entirely on the hidden hemisphere | no ink, no label, no hit, no inventory entry |
| intersects the visible hemisphere | draw only that portion, clipped at the limb, and the object **is** in the page inventory |
| lies entirely on the visible hemisphere | drawn as its projected footprint |

- **Hit testing operates only on visible ink.** What a reader cannot
  see, they cannot point at.
- **No label floats outside the disc.** A label outside the limb is a
  label for something the page did not draw — which is how the gate
  found this family of faults: a lone Greek letter hovering in an
  empty corner, belonging to a star 95 degrees away on the far side of
  the globe.
- **An object whose centre is hidden needs a truthful visible label
  anchor, or it is not labelled.** A name pinned to a place the page
  does not show is not a name the page can keep.

Implementation belongs to **#331**, and any reusable projected-footprint
geometry the seam needs, to **#329**. The fixtures are pinned in
`GlobeFootprintTest`: the cloud, which fails if centre-scale sizing
returns, and the straddler, which fails if clipping quietly becomes
dropping.

## How much sky a hemisphere can carry

The gate's first cartographic decision, and the one the others depend
on: how many names are useful, and whether boundaries help, are
different questions at V 4.0 than at V 6.0.

**The difficulty is not density but its distribution.** A rectangular
chart is much the same everywhere, so one ink figure describes it. A
globe's scale collapses toward the limb, and the arithmetic of that is
severe:

| band of the disc | of the paper | of the sky |
|---|---|---|
| middle, inside half the radius | 25.0% | 13.4% |
| **limb band, the outer tenth** | **19.0%** | **43.6%** |

The outer tenth of the radius carries nearly half the hemisphere in a
fifth of the page. Nothing is drawn differently there; there is simply
more sky in it.

Measured with the atlas's own definition of ink — a pixel that is not
the ground — over those bands, on four hemispheres, twice each: the
page as the atlas draws it, and the same page carrying marks alone
(`make globe-density-study`).

| V | stars | middle, as drawn | limb, as drawn | middle, marks | limb, marks |
|---|---|---|---|---|---|
| 4.0 | ~300 | 10–21% | 32–40% | 1–6% | 7–10% |
| 5.0 | ~900 | 11–23% | 36–44% | 3–9% | 15–18% |
| 6.0 | ~2 800 | 15–28% | 46–54% | 7–15% | 30–36% |
| 7.0 | ~8 700 | 22–38% | 62–71% | 15–26% | 52–61% |
| 8.0 | ~25 000 | 33–51% | 78–86% | 27–44% | 73–82% |

Three things the measurement says that looking could not:

**The limb carries about three times the mark-ink of the middle, at
every magnitude.** That ratio is the projection, not the sky, and it
does not improve with a brighter limit — it is the shape of the
problem rather than a quantity to tune away.

**Below V 6.0 most of the ink is furniture, not stars.** At V 4.0 a
hemisphere carries three hundred stars and is already a fifth inked;
the marks account for one to six points of that, and the grid,
boundaries, figures and names for the rest. **Choosing a brighter
limiting magnitude barely quietens a globe** — which is why the
boundary question (#326) is not a separate tidy-up but part of this
decision.

**A sparse centre does not give a sparse page.** The emptiest
hemisphere in the corpus has the *lowest* middle ink and the *highest*
limb ink at every limit — 0.9% against 8.5% of marks at V 4.0 —
because centring on empty sky puts the Milky Way near the limb, where
the page can least afford it.

### The default is V 5.0

At V 6.0 the limb band is half the paper in ink as drawn, and about a
third in marks alone: the outer ring reads as a dark rim rather than
as sky. At V 7.0 and V 8.0 it is solid.

V 5.0 keeps the limb band under half as drawn and under a fifth in
marks, and the disc stays a sky rather than a texture: figures remain
followable across it, and names still have somewhere to sit.

V 4.0 is quieter by only a few points, because at that limit the page
is nearly all furniture, and it costs the reader most of the naked-eye
sky — which is the thing a globe is for.

**This is the globe's own default and not a change to the ladder.**
The magnitude steps stay `{4.0, 5.0, 6.0, 7.0, 8.0}`, the reader can
choose any of them at 180 degrees as at any other field, and no other
rung's default moves.

## What each layer costs, and which stay on

The density measurement found that a globe cannot be quietened by
drawing fewer stars, because below V 6.0 most of its ink is furniture.
So each layer was measured by **taking it away from the finished
page** — what a reader gains by switching it off is what removing it
recovers. A layer drawn alone cannot answer that: the grid over empty
paper inks every pixel it touches, while the grid over a crowded limb
inks only the pixels that were not already dark
(`make globe-furniture-study`).

At V 5.0, across four hemispheres:

| layer | costs, over the disc | costs, in the limb band |
|---|---|---|
| constellation figures | 4.7–6.1 points | **6.3–8.3 points** |
| the coordinate grid | 3.3–3.7 | 5.0–5.9 |
| constellation boundaries | 3.1–3.6 | 4.4–5.5 |
| constellation names | 2.0–2.7 | 1.8–4.0 |
| star names and letters | 1.8–2.4 | 1.8–3.3 |
| **all five together** | | **20.1–26.5** |
| *the marks themselves, for comparison* | | *15.0–18.3* |

**The furniture costs half again what the sky it decorates costs.**
And the ranking does not follow value: the most expensive layer is the
one a globe can least do without, and the cheapest layers are the ones
that were suspected first.

The policy at 180 degrees:

- **Figures stay on.** The most expensive layer on the page, and the
  one that earns it: without them a hemisphere is a field of dots, and
  recognising the sky is what a globe is for.
- **The grid stays on.** Also expensive, and it does two things
  nothing else does — it orients the reader, and it is what makes the
  disc read as a sphere rather than as a circular crop.
- **Boundaries are off by default.** The clearest case: real ink for
  little a reader at this scale is reading. Dotted administrative
  lines are a detail chart's tool.
- **Constellation names, star names and letters stay on.** Under four
  points each, and they are how a reader learns the sky. This reverses
  the expectation the study began with — the text was never the
  problem.

**This is the 180-degree policy alone.** No existing page changes:
boundaries stay on where they already are, and a reader who turns them
on globally gets them on the globe too.

**When boundaries are drawn, #326 should make them subdued rather
than pitch dark.** That remains worth doing for narrower charts and
for the reader who asks for them here.

## The grid fades toward the limb

The grid stays on, and it is the second most expensive layer on the
page. Whether its *detail* is still doing anything out there was asked
as **gaps rather than ink**, because ink cannot tell a few fat lines
from a hundred hairlines (`make globe-grid-study`):

| radius | sky from centre | lines crossed | median clear gap |
|---|---|---|---|
| 0.50 | 30° | 18–23 | 37–41 px |
| 0.80 | 53° | 44–46 | 24–31 px |
| **0.95** | **72°** | **69–76** | **15–20 px** |
| 0.99 | 82° | 125–160 | **3–8 px** |

Out to **r = 0.95** the grid is a grid. By r = 0.99 it crosses a
hundred and sixty times around one circuit with three pixels between
lines, which is a texture, and it is much of why the limb reads as a
dark rim.

**Stopping it there was refused**: the band beyond 0.95 holds 31 per
cent of the hemisphere, so stopping would abandon a third of the sky
to no orientation at all — the one thing the grid was kept on for.
**Thinning it was refused**: a line that begins or ends according to
where it happens to fall has stopped meaning anything geometric.

> **Full contrast through r = 0.95, then a linear fade to 25% of
> contrast at the limb. The orthographic globe only.**

Strength is a share of the ink's *contrast against the ground* rather
than of its value, because that is what "fainter" means to an eye and
to a press.

Chosen by eye from candidates at 35, 25 and 15 per cent
(`make globe-grid-fade-study`): 35 still forms a mesh at the crowded
limb, 15 all but disappears exactly where orientation is hardest, and
25 quiets the rim while the lines stay traceable into it. The
hierarchy is what improves — objects lead, the grid supports, and the
limb stops looking reinforced by a second outline.

**What the fade does not do**, measured so that nobody expects
otherwise: the grid's own weight in the limb band falls from 11.2% to
7.9%, while the whole page's weight moves only from 15.50% to 15.31%.
The stars and figures are the rest of what is heavy there, and they do
not fade. Fading the grid makes the grid legible; it does not make the
limb light.

### A measure that could not see the question

Recorded because it nearly went into the decision. The first fade
numbers showed ink of 22.1% against 22.0% across every candidate, as
though fading changed nothing. The gate's ink measure asks whether a
pixel is the ground — the right question for *is anything drawn here*
and the wrong one for *how heavily*, since a faded line is still not
the paper. **Weight**, the mean distance of a band's pixels from the
ground, is the measure this question needed, and the one the table
above reports.

## Which families a hemisphere shows

All five stay enabled. The decision took two corrections to reach, and
both are recorded because each was a model error caught before it
became policy.

### The globe is a symbol map of where things are

Measured from projected footprints against the atlas's own practical
minimum of 6 px (`make globe-family-study`), almost nothing on a
hemisphere is drawn as its own shape. Median footprints run **0.1 to
1.7 px** against that 6 px threshold, at the centre as well as the
limb. So the reader is not being shown what objects look like; they
are being shown **where objects are**, by family glyph.

Both spans are **recorded**, not the major alone: near the limb it is
the radial direction that collapses, and two globulars measure 3.7 px
major against 1.5 px minor, which the major alone calls nearly
resolved.

**Whether the minor span should bear on resolution is an open question
for #331, not something this gate settles.** The classification here
uses the atlas's existing major-axis rule and adds nothing to it. A
review proposed also requiring the footprint to cover the production
glyph's inked area — true to the concern, but the ink is
production-derived while the *comparison* would have been invented in
a study, and equal ink area does not establish that a two-dimensional
form is resolved. An evidence repair is no place to make a new
cartographic rule.

**The classification is confirmed in pixels, on objects drawn by
themselves.** A rule computed from footprints is a prediction until
somebody looks at the ink, so one object of each family in each band
is rendered alone on the page and the ink it leaves is measured:

| family | band | footprint | drawn today | inked box |
|---|---|---|---|---|
| galaxies | limb | 0.7 × 0.3 px | 6.0 px | 8 × 6 |
| open clusters | centre | 2.6 × 2.6 px | 6.0 px | 8 × 8 |
| open clusters | limb | 0.4 × 0.1 px | 6.0 px | 8 × 7 |
| globular clusters | centre | 3.3 × 3.2 px | 6.0 px | 8 × 8 |
| globular clusters | limb | 1.9 × 0.8 px | 6.0 px | 8 × 8 |
| nebulae | centre | 4.6 × 4.3 px | 6.0 px | 6 × 8 |
| nebulae | limb | 4.9 × 1.3 px | 7.1 px | 3 × 9 |

Each is an object the geometry calls a **minimum symbol**, and each
inks about the minimum glyph — which is what the classification says
it should. **Three of the ten rows have no object to look at**, and
the report says so rather than omitting them: on a regional page the
clamp reaches only Messier priority and the searched target, so the
28 centre galaxies and the 59 planetary nebulae below the practical
minimum are not drawn at all and have no pixels to confirm anything
with.

*Two repairs stand behind that table (review of PR #336).* It could
choose an object production leaves undrawn and then report whatever
ink was in the window as its, and it cropped from a whole-family
rendering, so a neighbour's ink could be counted. It now requires that
the atlas draws the object, and draws that object by itself. The
"stands alone" filter it used to lean on is gone: rendering one object
cannot admit a neighbour anyway, and the filter was refusing eight
rows of ten.

The categories are named **resolved extent** and **minimum symbol**
rather than anything that assumes the conclusion, because a minimum
glyph is not nothing. Compared pixel by pixel at 6 px, the five family
glyphs differ from one another by **33 to 50 pixels** — the closest
pair being open clusters against nebulae, a dotted circle against a
box. Family identity survives at the size everything is actually
drawn.

### Eligibility

> An object appears when its **truthfully projected footprint
> resolves**, or it is a **Messier landmark**, or it is the
> **searched target**. A priority object whose real footprint is
> unresolved is drawn at the existing minimum family glyph. There is
> no globe-specific promotion for any family.

### What the footprint correction actually does

It removes **false size, not objects**. A hemisphere draws about
seventy deep-sky symbols, and they are overwhelmingly Messier — 18 of
20 galaxies, 28 of 28 globulars — so both rules keep them:

| family | drawn today | under the corrected rule | withdrawn |
|---|---|---|---|
| galaxies | 20 | 20 | 0 |
| open clusters | 9 | 9 | 0 |
| globular clusters | 28 | 28 | 0 |
| nebulae | 14 | 13 | **1** |
| planetary nebulae | 2 | 2 | 0 |

What changes is how large a few of them are drawn. A large galaxy near
the limb is currently sized at the rate it would have near the middle
of the page — tens of pixels — when its true footprint is a tenth of
one.

**How much ink that costs was claimed at 73% of today's at the limb,
and that claim is withdrawn.** It came from a study that modelled
every mark as a filled disc of its major diameter, which ignores the
minor span and is wrong about every family: an open cluster is a
dotted ring around nothing, a nebula an empty box, a planetary a small
circle with spokes.

The replacement was wrong too, and differently, which the review of
PR #336 caught: it compared **ink against a silhouette**. The
corrected side used the area the projected footprint *encloses*, so a
ring, a box, a cross or a pair of spokes became a filled ellipse again
the moment it resolved; and the today side took its minor axis from
the foreshortened globe span, where production scales **both**
catalogue axes at the page centre's rate. Two different geometries in
one ratio.

And it was measuring **one population for two rules**. A single loop
entered through "drawn today" charged the corrected side for the
nebula the corrected rule withdraws, and could never have charged it
for an object the corrected rule newly admits. The unresolved
landmarks were also given a *square* 6 × 6 glyph, where the atlas's
own clamp raises the major to the practical minimum and enlarges the
minor by the same factor, so the object keeps its shape.

Both sides are now the ink a production symbol leaves, asked of
`ChartRenderer.symbolInk` over the axes each rule gives it — today's
from `ChartRenderer.symbolAxesPx`, which is published rather than
restated, because restating it is how the wrong minor axis got in —
each summed over **its own** population, and each population checked
against the counts in the table above:

| band | symbol ink today | corrected |
|---|---|---|
| centre | 717 px | 723 px (**101%**) |
| limb | 5 493 px | 1 287 px (**23%**) |
| whole disc | 6 489 px | 2 276 px (**35%**) |

The study now **fails** rather than prints if the two sums cover a
different population from the one it has just tabulated. Charging the
corrected side through "drawn today" — the defect itself — trips it:
*the ink sums cover 31 today and 31 corrected, but the table above
counts 31 and 30*.

**The correction removes over three quarters of the deep-sky ink at
the limb**,
and the centre confirms the measurement rather than the story: there
the two rules agree to within 1%, because at the page centre the
centre-scale conversion is the right one. The limb is where a symbol
is drawn tens of pixels across for a footprint a tenth of one, and
that is exactly where the ink falls away.

That is a stronger result than the 73% it replaces, and it was hidden
by mismatches running in both directions at once — a silhouette and a
withdrawn object over-count the corrected side, a foreshortened minor
axis under-counts today's, and a square minimum glyph over-counts
every landmark the corrected rule draws small.

**None of this alters the family decision.** The population is
retained overwhelmingly through Messier priority — 18 of 20 galaxies,
28 of 28 globulars — which both rules keep, so the objects a reader
sees are the same either way. What changes is how much ink those same
objects cost, and the corrected rule costs less.

### Two model errors this study made first

Recorded because policy was nearly written on them.

**A magnitude filter the atlas does not have.** The first counts
filtered objects at V 5.0 and reported 134 galaxies and 106 open
clusters on a hemisphere. `RegionalDetailPolicy.drawn` has no
magnitude test: above 18 degrees a page is regional, and an object
appears if it draws at true size, or is Messier priority, or is the
target. The real figure is about seventy symbols, and the counts, the
collisions and the ink had all been computed over the wrong
population.

**A prediction that the correction would empty the limb.** It followed
from the first error: the nineteen limb galaxies looked like objects
admitted by the centre-scale mistake, and they are Messier landmarks
that both rules keep. Measuring the corrected population rather than
reasoning about it is what caught it.

## Names: no globe budget, a truthful page instead

There is **no separate name budget for the globe**. Family eligibility
and the shared placement priority are unchanged. What changes is that
the placement seam receives the globe's **circular page region as its
hard boundary**: text may neither cross nor leave the limb, and a
constellation name with no position inside both its visible figure
region and the globe is truthfully omitted.

This is not a private globe placement rule. The policy is untouched —
same candidate order, same obstacles, same collisions. It is the
existing policy told where the page ends, instead of assuming every
page is a rectangle.

### Why a budget looked unnecessary, and why that was not evidence

Measured first with the paper's edge as the boundary
(`make globe-name-study`), a hemisphere looked flawless: every label
placed, none omitted, 150 of 150 star names attributable at first
choice. It was flawless because the policy had the whole white margin
to work in.

| | on the disc | crossing the limb | outside it |
|---|---|---|---|
| star names | 129 | 15 | **6** |
| constellation names | 42 | 16 | 0 |

Six star names sat **entirely off the celestial sphere**, and each
passed every attribution test while breaking the rule that nothing
floats outside the disc. **The zero in the omitted column and the
labels in the margin were the same fact.**

### What the truthful page costs

Given the limb as its boundary, the same policy on the same page:

| | outcome |
|---|---|
| star names | **150 of 150** attributable, all inside the disc, none crossing |
| deep-sky names | unchanged |
| constellation names | 52 placed inside the disc, **6 omitted** |
| labels that moved | **54** |
| labels unchanged | 149 |

**No star name is lost**, and at the limb their median candidate rank
*improves* from 1 to 0 — excluding the margin did not push them into
worse positions, it stopped them wandering off the sky.

The six losses are constellation names, and the refusal cause says
which boundary refused them: **`OWNERSHIP`, not `PAGE_EDGE`**. They
cannot find a position inside their own figure's region that is also
on the sphere, because those figures are cut by the limb and most of
their region is not on this page. A name for a figure the page barely
shows is a name worth omitting.

So the budget question answers itself: attribution was already good
and stays good, and the only cost of telling the truth is six names
for constellations that are mostly elsewhere.

### A measure that was mine rather than the atlas's

The first run also reported 34 "doubtful" constellation names by the
nearest-rival test. That result was withdrawn rather than recorded: a
constellation's owned shape is the convex hull of a sprawling figure,
neighbouring hulls interpenetrate, so a name is routinely zero from
its own hull and zero from a neighbour's. It measured overlapping
hulls, not any confusion a reader could have. Constellation names are
now judged by whether they sit on the figure they name, which is what
the policy's `OWNERSHIP` refusal already guarantees.

## Pointing: the footprint decides, and nothing else

Sprint 30 refused the orthographic projection for the chart ladder
partly because **it cannot be pointed at**. Measured through the
production projection and mapping, radially and tangentially apart,
at sixteen azimuths and four sub-pixel phases per radius
(`make globe-pointing-study`) — because a single sample is a
fortunate pixel, and the first run of this study reported round-trip
errors that rose and fell with no pattern for exactly that reason:

| radius | sky out | 1 px radial | 1 px tangential | pixel spread | samples with no sky |
|---|---|---|---|---|---|
| 0.00 | 0° | 8.49′ | 8.49′ | 12.00′ | 0/64 |
| 0.50 | 30° | 9.82′ | 8.49′ | 13.62′ | 0/64 |
| 0.80 | 53° | 14.23′ | 8.49′ | 19.10′ | 0/64 |
| 0.90 | 64° | 19.71′ | 8.49′ | 26.00′ | 0/64 |
| 0.95 | 72° | 27.87′ | 8.49′ | 36.25′ | 0/64 |
| 0.99 | 82° | **69.53′** | 8.49′ | 84.43′ | 0/64 |
| 0.999 | 87° | **no sky** | 8.49′ | no sky | **64/64** |
| 1.00 | 90° | no sky | no sky | no sky | 64/64 |

**Tangential pointing is 8.49′ per pixel everywhere** — median and
worst identical, at every azimuth and phase. A reader points *along*
the limb exactly as well as at the centre. Only the radial direction
degrades, and it degrades smoothly: there is no measured point at
which it stops working and before which it works.

**Failure, when it comes, is abrupt and geometric.** At r = 0.99 every
sample has sky; at r = 0.999 not one does. The footprint either
contains dependable sky or it does not.

### The rule

> - **Object clicks** use visible ink and catalogue identity, and are
>   unaffected by any of this: clicking a drawn mark names the object
>   outright, however coarse the inversion is there.
> - **Empty-sky pointing and recentring** are allowed only when the
>   pointer's **entire logical pixel footprint lies inside the globe**.
> - If any part of that footprint has no inverse, the gesture is
>   **refused**.
> - **No additional radial cutoff.**

The growing radial uncertainty is real, but it is smooth, reversible
and visibly explained by the projection — the reader can see the sky
compressing. Recentring reverses everywhere it is defined: the old
centre is recoverable to 2–4′ inside r = 0.9 and 16–32′ near the limb,
with no branch switching and no jumps, and is lost only at the limb
itself. A separate threshold would forbid pointing that works.

### The round trip had to go through a real pixel

Recorded because the first version measured nothing a reader could
experience. Sky to plane to pixel and back in double precision only
proves that the same arithmetic reverses itself, which Sprint 30 had
already shown to 3.3e-13 degrees. Snapped to the whole pixel a mouse
event actually delivers, the same loop reaches **9 223 arcseconds —
2.6 degrees** — near the limb. The rounding is the measurement.

### Dragging to pan is not settled

**It could not be measured, and that is the finding.**
`PanSolver.solveCentre` takes a `ChartProjection` *kind*, so there is
no way to ask it about a globe at all: the solver is coupled to the
enum rather than to the projection being navigated — the same shape of
fault `DrawnPage` corrected elsewhere in this gate.

**#330 must accept the page or projection being navigated before drag
behaviour can be measured at all.** Nothing here settles it, and it
must not be inferred from the other two gestures: solver continuity,
whether the grabbed sky stays under the pointer, and whether a drag
can switch branches are questions about the solver's tie-break, which
was written for pages whose scale does not collapse.

## Modules, clipping, and what a globe exports

### The clipping rule

> **Every piece of sky-derived ink is clipped to the bounded page
> region before painting. Furniture is outside that clip.**

One sentence rather than a list, so that no family is forgotten:
constellation figures and boundaries, the grid, the modules, working
marks, star marks and deep-sky symbols are all sky-derived. **Text is
not clipped** — it is governed by placement inside the disc, because
clipping a word would make a false name.

For modules specifically, **#331 must make this true of every
contribution, not only curves**:

- curve geometry may reach the limb, but no painted stroke may extend
  beyond it except the unavoidable stroke-edge tolerance;
- point marks such as the zenith draw only when their sky position is
  visible;
- a module name is drawn only with a truthful anchor and its whole
  label inside the disc, and is otherwise omitted;
- module ink stays below sky text and does not influence label
  placement, preserving the existing module contract.

### What the modules do today: known debt

Observed, not inferred (`make globe-module-study`):

| centred on | module ink inside | beyond the limb | furthest | names |
|---|---|---|---|---|
| zenith overhead | 7 682 px | 2 506 px | **1.55×** | Meridian — inside the disc |
| southern horizon | 7 152 px | 948 px | 1.19× | Mathematical horizon — **outside the disc** |
| ecliptic high | 10 514 px | 1 389 px | 1.55× | none |
| Sagittarius | 10 868 px | 1 450 px | 1.30× | none |

Module curves reach **half again the disc's radius**, and a module
name is drawn wholly on the paper. The first run of this study used
one centre, found no names at all, and would have let the name rule
pass vacuously — the other centres were chosen so each module had
somewhere to put a name.

What does hold is the module contract: `textObstacles` is built from
the scene and options and is never offered a module's contribution, so
module ink cannot move a sky label.

### Where a globe's outside-limb ink comes from

Attributed by removing one layer at a time from the page
(`make globe-export-study`). **These are marginal costs and do not
sum**: the ink overlaps, so removing two layers recovers less than the
two rows together.

| removed | recovers |
|---|---|
| constellation figures | **4 563 px** |
| constellation names | **4 417 px** |
| star names | 2 923 px |
| deep-sky symbols | 684 px |
| the grid | 90 px |
| **star marks** | **0 px** |

Star marks never leave the limb — they cannot project past ninety
degrees. The overrun is lines and text, which is why the clipping rule
had to be broadened past "curves".

### Export: identity and fidelity settled, geometry not

**Projection identity survives all three writers**, with and without
modules: SVG, PDF and PNG each carry "orthographic".

**The PNG writer is faithful**, measured rather than assumed. The
recording was rasterised at the writer's own 3208×2180 — the chart's
own rectangle cut from the sheet, since the file is the whole page and
the chart is inset by the margin — and the outside-limb masks compared
pixel for pixel, **one layer at a time**:

| layer alone on the page | in both | page only | file only | furthest | beyond √5 |
|---|---|---|---|---|---|
| constellation figures | 74 013 | 3 042 | 286 | 1.0 px | **0** |
| the grid | 73 416 | 3 049 | 314 | 1.0 px | **0** |
| deep-sky symbols | 83 516 | 2 943 | 319 | 1.0 px | **0** |
| star marks | 72 354 | 2 881 | 268 | 1.0 px | **0** |
| *star names* | *112 846* | *5 047* | *2 428* | *1.4 px* | *0* |
| *constellation names* | *134 787* | *6 100* | *3 337* | *2.2 px* | *0* |

**The two italicised rows are this machine's answer only**, and a
second machine is why. Running the contract on the CI runner, the
star-names layer produced lone pixels **4.0 px** apart at radius 1.118
of the disc, and the neighbourhood shows what they are: a vertical
glyph stem two pixels wide in the page and four pixels wide in the
file, sitting five columns to the left. The same stroke, hinted
differently under the two paths' transforms — the page drawn straight
to a raster, the file replayed by the writer at 300 dpi. That is font
rasterisation, which #315 already classifies as the desktop's answer
rather than the atlas's.

So the claim is split, and narrowed to what it can carry: **the writer
keeps the page's geometry for vector ink on every machine**, pinned to
bytes at √5; for **text ink** the comparison is held to reproducing
within one environment and lives in the platform record. The
demonstration that the oracle can fail uses the constellation-figures
layer, so it stays portable.

A first version of this check compared the whole page at once, asking
of each differing pixel only whether the other rendering had *any* ink
within the bound. **That oracle was withdrawn**: beyond a globe's limb
the figures, the grid and the names lie across one another, so a shape
the writer had moved could be excused by an unrelated glyph that
happened to be near. Drawing one layer alone keeps the ink in the
comparison owned by the shape being compared.

The bound is **√5 ≈ 2.24 px**, and it comes from the mechanism rather
than from this page: two rasterisers may place a filled edge up to one
device pixel apart *in each axis*, and a span may gain or lose a pixel
at each end, so a solid block's corner can lie √(2²+1²) from the
other's nearest ink and no further.

The single pixel that reached that bound was chased down rather than
called an edge effect: it sits on a **glyph stem in a constellation
name beyond the limb**, which both renderings draw, the file's block
beginning a row higher and ending a column wider. That it fell in
constellation-name text — the largest source above — is the reason
chasing it mattered, and the CI runner later showed why: the same
mechanism, on a desktop that hints text differently, moves a stem four
pixels rather than two.

**And the oracle is shown to fail.** A check that cannot fail proves
nothing, so a 25×25 patch is struck out of the page rendering at
radius 1.40 of the disc — beyond the limb, clear of the sheet border —
removing **125 px** of constellation-figure ink. The comparison then
reports **105 px beyond the bound**, the furthest 8 px from any ink in
the file. A deleted outside-limb shape is not survivable.

So a globe's exported geometry is wrong **before** anything is
written. The writers preserve a page that is already drawing sky
outside the sphere.

**PDF geometry is unverified**, deliberately. `PdfSheetWriter.path`
can confirm a known shape is present but cannot enumerate marks, so
circular containment cannot be answered without a parser this gate has
no business building. Its projection identity *is* verified.

### The three study doors, and who removes each

The gate could not see a production globe without them, and each names
its remover in the code as well as here:

| door | why it exists | removed by |
|---|---|---|
| `SceneAssembler.assembleForStudy` | a `ChartViewState` refuses a 180-degree field | **#329** |
| `GlobeProjection` (and `ViewportMapping`'s frame override) | production has no orthographic projection | **#329** |
| `ChartSheet.recordForStudy` | the sheet path takes a `ChartViewState` too | **#331** |

`ChartRenderer.drawing(page, …)` goes with the first two: **#329**.

**`DrawnPage` is not on this list.** It is production architecture,
not a door — the investigation found thirteen implicit copies of that
boundary already in the atlas, and `OneProjectionPerPageTest` keeps it
at one.

### Where this gate's evidence lives, and what checks it

The eleven studies above write documents under `docs/studies/`, and
`EvidenceContractMain` runs every one of them. Until the review of
PR #336 it ran none: the gate cited numbers that no green contract had
ever looked at, so a study could have fallen behind the atlas without
anything saying so.

Each study writes **two** documents, because it measures two kinds of
thing and a report mixing them can be held to neither (#315):

| | pinned to bytes | held to reproducing here |
|---|---|---|
| what | objects counted, footprints projected, spans in degrees, a projection named after a writer has had it, every pass-or-fail verdict | every count of ink, every font-measured box, every file size |
| where | `measurements.md` | `platform.md` |

So "no ink beyond the limb" is in the portable half and fails anywhere
it stops being true, while the 99 625 pixels that *are* beyond it are
this machine's number and are held to reproducing on it.

| study | `docs/studies/` | platform record |
|---|---|---|
| `make globe-study` | `globe-hemispheres/` | — |
| `make globe-frame-study` | `globe-frame/` | — |
| `make globe-density-study` | `globe-density/` | yes |
| `make globe-furniture-study` | `globe-furniture/` | yes |
| `make globe-grid-study` | `globe-grid/` | yes |
| `make globe-grid-fade-study` | `globe-grid-fade/` | yes |
| `make globe-family-study` | `globe-families/` | yes |
| `make globe-name-study` | `globe-names/` | yes |
| `make globe-pointing-study` | `globe-pointing/` | — |
| `make globe-module-study` | `globe-modules/` | yes |
| `make globe-export-study` | `globe-export/` | yes |

**Publishing them found a defect the gate had been living with.** The
frame study asks what fraction of the page a disc should fill, and the
only way to ask is a JVM-wide property. Run on its own it put the
property back by exiting; run in one process with the other ten —
which is how the contract runs them — it left the last fraction it
tried, **86%**, set for everything after it. The pointing study then
reported a pixel at r = 0.95 as **83.8°** out from the centre instead
of 71.8°, and every study between them measured a disc four percent
too small. Nothing in the gate could have noticed: each study was run
in its own JVM, by hand, and agreed with itself — which is also why
**no figure recorded above is wrong**: every one of them was measured
by a study running alone, at the atlas's own 90%. The property is now
restored in a `finally`, and `GlobeStudyIsolationTest` holds both that
it is restored *and* that a globe assembled afterwards has the disc it
would have had — the second because a property put back by luck would
satisfy the first.

The three without a platform record measure no ink at all: the
hemispheres count what the sky puts on a page, the frame is the disc's
geometry against the paper's, and the pointing study is projection
arithmetic. The hemisphere study also lost its wall-clock column when
it was published — a timing cannot be pinned to bytes, and the label
study settled that (#310).

## What this gate does not claim

Stated here rather than only in passing, because a limit mentioned
once inside a paragraph reads as a pass to anyone scanning:

- **PDF geometry is unverified.** Its projection identity is verified;
  whether its marks lie inside the limb is not, and this gate
  deliberately did not build a PDF parser to find out.
- **Paper readability is not claimed.** Nothing here has been printed.
  How a globe reads on paper — at what size the limb crowding becomes
  unreadable in ink, whether the faded grid survives a press — belongs
  to **#293**, which remains the authority for any claim that needs
  physical printing. This gate settles geometry and exported truth
  only.
- **Drag-to-pan is not settled**, and was not measurable: see above.
- **Writer fidelity for text ink is established per environment, not
  across machines.** Vector ink — figures, grid, deep-sky symbols,
  star marks — is held to √5 on every machine that runs the contract.
  Text is not, because two rasterisation paths hint a glyph stem to
  different pixels, and the CI runner moves one four pixels where this
  machine moves it two. What that leaves unproven is narrow: whether a
  *letter* survives the writer unmoved is a claim this gate makes only
  of the desktop it was measured on.

## A page is drawn by one projection

Not a cartographic decision, but the gate found it and it had to be
settled before any of the evidence above could be trusted.

A viewport named a projection *kind*, the kind was a function of the
field, and the field was a rung on the ladder — so **thirteen** places
asked the viewport again whenever they needed a projection: the grid,
the labels, the modules, the working marks, the page extent, the
inventory, the plane-to-pixel mapping, the renderer's three drawing
passes, the title block, the accessible description, the exported
sheet. Thirteen copies of one derivation, every one correct, and
nothing kept them agreeing because until a globe existed they could
not disagree.

Given something to disagree about, they did: the first hemisphere had
its stars at orthographic radii on a plane scaled stereographically,
the disc at half the page it should have filled, and every label
placed by a projection that had not drawn a single star on it.

`DrawnPage` carries a scene and the projection that draws it as one
value; `OneProjectionPerPageTest` holds that only its factory may
derive one, **and** that the passes which draw a page are handed one
rather than building their own — the second rule because the first
was satisfied by a fix that put the split straight back.

## Still open

Dragging to pan, which #330 must make measurable first; paper
readability, which remains #293's; and how a globe's page and limb reach the
placement seam and the clip in production, which is #331's.

The shared placement policy stood: it needed no change, only the
page's true shape.
