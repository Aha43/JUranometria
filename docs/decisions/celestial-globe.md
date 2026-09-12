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

Which families are visible at 180 degrees; how many names are useful; whether boundaries stay on
by default, assessed with #326 in mind; navigation and pointing near
the limb; module geometry; projection identity in export; and paper
policy, with #293 remaining the authority for any claim that needs
printing.
