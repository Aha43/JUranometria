# From the celestial sphere to a page

*How JUranometria chooses which truths a sky chart should preserve*

Stand outside on a clear night and the sky seems to have the simplest shape
imaginable: a dome. Turn around and the other half completes an imaginary
sphere, with the observer at its centre. Astronomers call this useful fiction
the **celestial sphere**. A star's right ascension and declination locate a
direction on that sphere much as longitude and latitude locate a place on
Earth.

A chart has a harder job. Paper is flat. The sky is not.

There is no way to flatten a sphere without changing something. Directions,
shapes, areas, distances and straightness cannot all survive together. Every
sky atlas therefore makes a choice, whether or not it tells the reader. The
choice is called a **map projection**, but in an astronomical chart it is more
than a formula. It determines where every star lands, how constellation
figures bend, what happens at the edge, how dragging feels, and whether an
exported sheet still means what its title says.

JUranometria uses three projections because it asks three different kinds of
question. A telescope field, a wide overview and a celestial globe do not need
the same truth preserved.

![Orion and the surrounding sky shown in gnomonic, stereographic and
orthographic projections](images/three-projections.png)

*Figure 1. One centre and one nominal 90-degree field, drawn three ways by
JUranometria’s projection study. The gnomonic page stretches the outer sky to keep great circles
straight. The stereographic page lets scale grow while preserving local
angles. The orthographic page compresses the same outer sky as it approaches
the limb. Even the slightly different star counts printed by the three panels
are informative: equal rectangles and equal nominal fields do not admit
exactly the same directions near their corners.*

The differences are not decorative. Put the same star 60 degrees from the
centre and give all three projections the same scale at the centre. Its radius
on the mathematical plane is about 1.73 in the gnomonic projection, 1.15 in
the stereographic projection and 0.87 in the orthographic projection. At 80
degrees the three answers are about 5.67, 1.68 and 0.98. One runs outward
toward infinity, one grows steadily, and one approaches the fixed limb of a
globe. Those three behaviours are the three kinds of page in miniature.

## The small patch: put the telescope where the chart says

For an ordinary detailed chart, JUranometria uses the **gnomonic projection**.
Its construction is easiest to understand as a physical model. Imagine an
opaque celestial sphere with a point light at its centre and a pinhole at every
star. Rest the sphere against a flat sheet, tangent at the chart centre, and
turn on the light. Each ray that passes through a star hole leaves a mark on
the sheet.

The central ray meets the sheet at the point of tangency. A nearby ray travels
a little farther before it arrives. Move the star hole farther around the
sphere and the mark runs outward ever faster. At ninety degrees the ray is
parallel to the sheet and never reaches it at any finite distance. A hole on
the far hemisphere does not belong to this projection at all.

![A point light at the centre of a celestial sphere projects star holes onto a
tangent plane, with marks spreading rapidly away from the centre and the
ninety-degree ray never meeting the plane](images/gnomonic-light-and-plane.svg)

*Figure 2. A cross-section of the gnomonic construction. Equal angular steps
across the sphere occupy increasingly unequal distances on paper. The centre
is gentle; the horizon at 90 degrees is infinitely far away.*

The construction has a remarkable property: every great circle becomes a
straight line. A great circle is the largest possible circle on a sphere. The
celestial equator, the ecliptic, a horizon and a local meridian are all great
circles. On a gnomonic chart they can be drawn as straight lines, even though
they curve around the sky.

That is also why this projection belongs in a practical observing atlas. A
straight course across the sphere remains straight on the page, and a compact
telescope field lands where the chart says it will.

The price rises rapidly away from the centre. If an object is an angular
distance *t* from the centre, its distance on the tangent plane is proportional
to `tan(t)`. Near the centre, tangent and angle are nearly equal. Farther out,
the scale runs away. At ninety degrees the tangent plane has no finite answer
at all.

So the detailed chart is not merely cropped at an arbitrary width. Its useful
domain is part of its meaning. JUranometria uses the gnomonic projection for
fields where its straight great circles and accurate pointing are valuable,
and stops before edge distortion turns familiar sky patterns into unfamiliar
ones.

## The wide view: preserve the shapes we recognise

A wide chart asks a different question. Its purpose is not usually to place a
telescope within a small field. It is to let a reader recognise the large
pattern of the sky.

For that, JUranometria uses a **stereographic projection**. It can show almost
the whole sphere: only the point exactly opposite the centre is missing. More
important for an overview, it is **conformal**. Small shapes keep their local
angles and therefore keep their shape, although their scale changes across the
page.

That distinction matters in a star chart. We recognise Orion, the Plough and
Cassiopeia by shape. If the stars in an asterism are stretched differently in
different directions, the chart has not merely changed their size; it has
changed the pattern the reader was trying to recognise.

Stereographic projection pays in scale instead. Objects near the edge grow
relative to objects near the centre. That is a better bargain for a wide
finding chart: the constellation still looks like itself, while the varying
scale is something the grid and field statement can disclose.

JUranometria did not choose this from a list of attractive formulas. It drew
real pages of the real sky and compared candidate projections at the centre,
edge and corners. Gnomonic preserved great-circle straightness but distorted
shape too quickly. Orthographic looked unmistakably spherical but compressed
the outer sky too hard for an overview chart. Stereographic preserved the
recognisable patterns best.

Orion makes a useful test because most readers know its proportions before
they know its coordinates. Near the centre, all three projections can make the
belt and the four bright corner stars look plausible. Move the same figure
toward the edge and their promises separate. Gnomonic stretches it outward;
orthographic presses it toward the limb; stereographic enlarges it but keeps
the small angles between its lines. The familiar figure becomes a measuring
instrument for the projection.

## The hemisphere: show that the sky has an edge

The third view is not a wider rectangular chart. It is a **celestial globe**:
one hemisphere seen from very far away. JUranometria draws it with an
**orthographic projection**.

Its construction is the familiar view of a globe from space. A star's offsets
on the page are simply the east and north components of its direction. The
visible hemisphere ends at a circular limb. Beyond that circle is not empty
sky; it is no sky at all, because those directions are on the far side of the
sphere.

Scale collapses toward the limb and shapes are squashed radially. Here that is
not a defect to hide. It is the visual cue that tells the reader this page is a
sphere seen edge-on. The outer tenth of the globe's radius contains far more
sky than the inner tenth. A constellation approaching the limb looks
foreshortened for the same reason a continent does on a photograph of Earth.

This requires a different kind of clipping. A rectangular chart is bounded by
paper. A globe is bounded first by the sky's circular limb, then by paper.
Without that distinction, stars might stop correctly while grid lines and
constellation boundaries continued into the corners, drawing beyond the edge
of the world.

## One star's journey

The three projections differ, but the route through JUranometria is the same.
Consider one catalogue position.

First, the chart chooses a **centre**: the direction at the middle of the
page. Around that centre the projection builds a local three-dimensional
frame:

- **along** points from the observer toward the centre;
- **east** is tangent to the sky toward increasing right ascension;
- **north** is tangent toward increasing declination.

The star's direction is expressed in those three components. The projection
then answers where that direction lands on its plane:

| projection | plane radius for angular distance *t* | what it preserves |
|---|---:|---|
| gnomonic | `tan(t)` | great circles are straight |
| stereographic | `2 tan(t/2)` | local shape and angles |
| orthographic | `sin(t)` | the appearance and limb of a sphere |

The result is still not a pixel. It is a point on a mathematical plane. A
viewport mapping gives that plane a scale and places it on the page. Half the
declared horizontal field occupies half the page width. North maps upward.
East maps leftward, the traditional printed-chart convention: the chart is a
view of the celestial sphere rather than a terrestrial landscape in front of
the observer.

That last sentence is easy to misunderstand near the north celestial pole.
On the page, “east left” describes the chart's coordinate handedness. At a
particular place on a wide view, the direction toward the local eastern
horizon may point right, upward or diagonally. The mathematical-horizon module can
state that local fact with its N/E/S/W marks without changing the title's global convention.

### East left—while an E marker points right

This apparent contradiction is easiest to see by comparing a printed atlas
with a planetarium program while looking toward Polaris. The atlas may state
“north up, east left”, yet the planetarium can place an **E** toward the right
side of the view. Both can be correct.

The atlas is stating the handedness of its celestial coordinates. Hold
declination fixed and increase right ascension: on the printed page, the stars
move left. That convention belongs to the map as a whole.

The planetarium's **E** usually identifies a direction on the observer's local
horizon. Face north and geographical east is to your right. Face south and it
is to your left. Look near the zenith and the direction may run diagonally
across the display. It depends on where the observer is facing, not merely on
which way right ascension increases.

Putting both statements on one chart would therefore add information rather
than correct an error. The title can retain the global “east left” convention,
while optional horizon marks show where the local eastern and western
directions lie in this particular view. The small moment of puzzlement is useful: it
reveals that a sky chart relates two coordinate systems—the sphere used by
astronomers and the horizon used by the person standing beneath it.

Only after projection and mapping does the renderer receive a pixel position.
It can then draw the star, provided the point lies inside both the projection's
visible domain and the page's clip.

```text
catalogue direction
       │
       ▼
local frame about the page centre
       │
       ▼
projection: sky direction → plane point
       │
       ▼
viewport mapping: plane point → page pixel
       │
       ▼
visible-region and paper clipping
       │
       ▼
renderer → screen, SVG, PDF or PNG
```

## A line is not just many points

Projecting a star is simple because a star is a point. The grid, ecliptic,
horizon, meridian and constellation boundaries are curves on the sphere.

A tempting implementation is to sample hundreds of points, project each one,
and join the dots. That works until it does not: a curve may cross a projection
boundary between samples, develop visible corners, or need thousands of
points at one scale and only a handful at another.

JUranometria instead asks the projection what a great circle becomes
**analytically**. Depending on the projection and orientation, the result is a
line, circle or ellipse. Internally it is first represented as a general conic
equation. That form remains well behaved even where a circle's radius would
grow toward infinity and the visible curve becomes a line.

The projection does not decide how to draw the conic. That decision belongs
to the page. The same astronomical great circle may be visibly curved across
a hemisphere and indistinguishable from a straight line across a small field.
The page chooses the simplest drawable form whose departure from the true
curve is below a measured allowance on that paper.

This is a useful architectural boundary because it matches the science. The
module contributing an ecliptic or meridian knows which great circle it means.
It does not know whether that circle will become a line or an ellipse. The
projection knows the mathematical image. The page knows whether the difference
is visible.

![The same great circle drawn as a line, circle and ellipse by three
projections](images/great-circle-three-projections.svg)

*Figure 3. The astronomical object is one great circle. Its projected form is
a line, circle or ellipse; the page then draws only the part inside its honest
domain.*

## Why a projection is not a module

*Technical sidebar: the distinction that keeps new astronomical layers from
rewriting the chart's geometry.*

JUranometria's modules add knowledge to a chart. An ecliptic module contributes
the ecliptic and its seasonal points. A future asterism module may contribute
Karlsvogna without pretending it is one of the 88 IAU constellations. A solar
system module can contribute moving bodies.

A projection does something categorically different.

Remove a module and the same page remains, with less on it. Change the
projection and every mark moves. Visibility, scale, curvature, inverse lookup,
navigation and export all change together.

That yields one of JUranometria's central architectural rules:

> A module says what belongs on the sky. A projection says how the sky becomes
> a page.

The code follows the rule. Projection implementations know nothing about
Swing, preferences, files, network access, modules or catalogues. Consumers
ask a common projection interface for positions, inverse positions, scale,
domain, visible radius and great-circle form. Only one factory turns a
projection kind into an implementation.

The page carries its scene and its projection together. This prevents a subtle
but serious failure: placing stars with one projection while scaling, naming
or exporting the page as another. JUranometria has made that mistake during
development. The hemisphere's stars were placed orthographically on a plane
scaled stereographically, so the globe occupied roughly half the size it was
meant to. Pairing scene and projection makes disagreement a state the rest of
the application cannot represent.

## Interaction has to use the same geometry

A chart is not finished when it looks right. JUranometria is interactive, so
the inverse journey matters too.

When a reader drags the chart, clicks a point or zooms under the pointer, the
application must ask which sky direction lies beneath a page coordinate. That
is the projection's inverse operation. If drawing uses one geometry while
navigation assumes another, the sky slips under the pointer or the selected
object is not the one that was clicked.

Zooming is especially revealing. The reader expects the direction beneath the
pointer to stay beneath the pointer while the field changes. Solving the new
centre requires projecting and unprojecting in the projection that owns the
new field. It cannot be repaired later in the renderer because the view state
itself would already name the wrong centre.

Screen and export therefore consume the same assembled page. An exported
sheet is not a second drawing with a similar set of choices; it is another
recording of the geometry the reader was looking at.

This is where an interactive atlas differs most from a pile of prepared
charts. The page may change its centre, width and projection while the reader
uses it, but each change has to remain one coherent geometrical statement.
The star under the pointer, the field named in the title, the curve drawn on
screen and the curve written to PDF must all belong to that same statement.

## The edge cases are the model

Projection code is full of apparently tiny boundary questions. Does exactly
ninety degrees belong to the domain? Is a pole written with a different right
ascension a different point? Is the stereographic antipode merely very far
away, or absent?

These are not numerical housekeeping. They state what the geometry means.

For a gnomonic page, ninety degrees is infinitely far away and is not on the
plane. For an orthographic globe, ninety degrees is the limb and is the most
important boundary the projection draws. The same comparison cannot answer
both. Likewise, right ascension has no meaning at a celestial pole. Equivalent
coordinate spellings must produce the same direction before trigonometric
rounding gets a chance to distinguish them.

JUranometria's projection work repeatedly found that a mathematically correct
formula can still encode the wrong boundary when evaluated with finite
numbers. The durable fixes came from stating the geometric fact first, then
making the implementation represent it directly.

![A rectangular chart whose sky continues beyond its paper beside a globe
whose visible sky ends at a circular limb](images/paper-edge-and-sky-limb.png)

*Figure 4. A rectangular overview is cut off by paper, while an orthographic
globe ends at the limb of its visible hemisphere. Quiet paper outside the
disc is not empty sky.*

## A page is a declared compromise

No projection wins. Each tells the truth needed by a particular page:

- the **gnomonic chart** tells the truth about straight great-circle paths and
  compact pointing fields;
- the **stereographic overview** tells the truth about recognisable local
  shapes across a wide region;
- the **orthographic globe** tells the truth that the visible sky is a
  hemisphere with a limb.

The architecture matters because it keeps those compromises explicit. A
field width selects a projection by a single rule. A projection states its
domain and scale. A drawn page carries the projection that actually placed
its marks. Modules contribute sky geometry without learning how it bends on
paper. Navigation and export reuse the same geometry.

That is how a sphere becomes a page without the program pretending nothing
was lost. The chart cannot preserve every truth at once. It can preserve the
right truth for what the reader is trying to do—and say clearly which truth it
chose.


---

**Under the chart:** [Read the technical companion](../how/projections.md), with
the source and decision records behind the geometry. See also [Language without
translating the sky](../how/language.md), the first article in *How
JUranometria works*.
