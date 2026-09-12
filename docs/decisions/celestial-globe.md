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

Density and the default limiting magnitude; which families are visible
at 180 degrees; how many names are useful; whether boundaries stay on
by default, assessed with #326 in mind; navigation and pointing near
the limb; module geometry; projection identity in export; and paper
policy, with #293 remaining the authority for any claim that needs
printing.
