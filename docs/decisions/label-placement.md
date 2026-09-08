# How labels share an atlas page

Sprint 31, issue #310. The cartography and architecture gate for
`#313` (the placement seam), `#314` (production placement) and `#315`
(the closing journey).

Measured in
[the label-placement study](../studies/label-placement/measurements.md),
which changed no production behaviour and no released page.

## The reader-facing rule

> **Every piece of text on a page belongs to something, and says so by
> being next to it.** A label may sit at one of eight stated positions
> around the thing it names. It moves only to avoid covering another
> piece of text, a star's mark, a deep-sky symbol, the page's
> furniture or the paper's edge. It never moves so far that it names
> something else, and it is never silently dropped.

Two things that rule deliberately does not promise. A label may cross
a **line** — a figure, a boundary, a graticule meridian, a reference
curve — because on a 120° page the lines are most of the paper and a
policy that avoided them would have to stop drawing text. And a label
may still, rarely, sit on a mark: when none of its eight positions is
free it takes the one that covers the least, because a name that is
drawn awkwardly is worth more to a reader than a name that is not
drawn at all.

## What was wrong

The owner found it by looking at a finished 120° page: a constellation
name with star names written through it, and a small star name laid
across an unrelated bright star's disc. Both reproduce, and both are
in the study's tables with their participants named from the render.

Measured over 23 pages of the corpus, **1,827 attributed collisions**,
of which **56** are a constellation name and a star label sharing
pixels and **252** are a star's name across another star's mark. The
named fixture, Nunki against Namalsadirah on the Sagittarius overview,
is worse than it first looks:

- Nunki's name is drawn across 68 pixels of Namalsadirah's disc — more
  ink than the mark has left visible;
- and Namalsadirah's own Bayer letter, which qualifies at this field,
  is **not drawn at all**. The star-label pass takes stars brightest
  first, accepts "Nunki σ", and then refuses φ because its box meets
  the accepted one.

So the atlas declines to name a star in order to protect a name that
it then draws across that same star's mark. That is not a placement
bug in one family; it is the absence of a policy.

The families were developed separately and avoid different things:

| family | avoids |
|---|---|
| star labels | the title block, the magnitude key, deep-sky label boxes, other star labels |
| deep-sky labels | nothing |
| constellation names | nothing |
| grid notation | the title block and the magnitude key |
| reference names | other reference names |

Nothing avoids a star's mark, a symbol, a figure line or a boundary,
and nothing but star labels avoids anything drawn by another family.

## The decision

### One shared placement service, several ordered policies

Not one algorithm for everything, and not five unrelated ones. `#313`
builds **one placement service** that owns:

- the candidate vocabulary (below);
- the obstacle set and the geometry of every kind of obstacle;
- the accepted-box register, so that a later family sees an earlier
  family's decisions;
- the refusal and fallback semantics.

Each text family keeps its own **eligibility** rule — which stars
qualify at which field is `StarLabelPolicy`'s business and stays
there — and hands the service an anchor, a string, a priority and a
candidate list. Eligibility and placement are different questions and
this sprint does not merge them.

### Priority

In this order, and the order is part of the contract:

1. **the searched target's label** — guaranteed, never moved, never
   refused, exactly as today;
2. **star labels**, brightest first, then by catalogue id;
3. **deep-sky labels**, by the catalogue's own label priority;
4. **constellation names**.

Constellation names go last because they have the most freedom: a name
belongs to a whole figure and has a page-sized region to sit in, where
a star's name has one star. Placing them first was measured, and buys
nothing: the same count of the owner's defects on every page of the
candidate set, one more released label lost at `orion-120`, and seven
more collisions at `sagittarius-120`.

Only one label is **guaranteed**: the searched target's. Everything
else may move, and nothing else may be promised a position.

### The candidate vocabulary

Two lists, both fixed and both ordered. They are stated apart because
they are not the same length: an earlier draft of this document said
"eight positions, and no others" while the policy it described gave
constellation names seventeen.

**A star label or a deep-sky label: eight positions**, around the mark
it names, in this order:

```
east, west, north-east, north-west, south-east, south-west, north, south
```

East first because that is where every label sits today, so an
uncrowded page keeps the placement it has and no released page moves
for nothing. Offsets are the anchor's own reach — a star's magnitude
radius, a symbol's half-extent — plus three pixels, which is the gap
the renderer already uses.

**A constellation name: seventeen positions.** Its anchor is the
centroid of its visible figure ink, as today; its candidates are that
point, then the same eight directions at 20 pixels, then those eight
again at 40. A name has no mark to sit beside and a whole figure to
sit in, so it is given more room — and the room is bounded by the
region below rather than by the ring.

Ties are impossible in either list: it is ordered and the first free
candidate wins. Nothing here searches, samples, iterates to
convergence, or depends on hash order, floating-point summation order
or the platform.

### What a label must avoid

| must avoid | must not have to avoid |
|---|---|
| any accepted label of any family | figure lines |
| any drawn star's mark, at its own magnitude radius | constellation boundaries |
| any drawn deep-sky symbol, at its own outline | the equatorial graticule |
| the title block and the magnitude key, when drawn | reference curves |
| the paper's edge, by two pixels | its own anchor's mark |

The right-hand column is a decision and not an omission. Measured: a
policy that also refuses every line reaches nearly zero collisions and
pays for it by refusing to draw **a third of the text** on the widest
pages — 116 of the labels `orion-120` draws today. A name across a
star hides a fact; a name across a hairline hides a hairline; the two
are not commensurable and a policy that minimised their sum would
choose the wrong one.

"Its own anchor's mark" is likewise deliberate: a star's name is
drawn beside its own disc by decision, and a policy that treated that
as a collision would move every label on the page.

### Nothing is silently dropped

A label with no free candidate is **still drawn**, at whichever of its
candidates covers the least ink — least covered area first, and only
among equals does crossing fewer lines decide. Lexicographic, not
weighted, because a weight between "covers a mark" and "crosses a
line" would be a tuning constant nobody could defend.

"Least ink" is the area shared with each obstacle's **own drawn
outline**, not with its bounding box. A disc in its square is a fifth
empty at the corners and a galaxy's ellipse far more, and this gate's
whole argument is that a box is not ink: a fallback that ranked
candidates by boxes would be ranking them by the very thing the census
refuses to count. An earlier draft did exactly that.

The ownership rule below is not a cost the fallback may spend. A
candidate outside its constellation's own region is refused there too,
whatever it would have covered.

This is the load-bearing choice of the whole decision. The three
policies that omit instead reach zero of the owner's defects and lose
between 1 and 116 labels a released page currently draws. The
least-bad fallback loses **none**, on every page measured, and still
takes the observed defects from 48 to 7 on the fixture's page, from
37 to 6 at `orion-120`, and from 19 to 1 at `orion-90`.

Where a label ends up at a candidate that covers something, the
service records it. `#314` publishes that record; a page whose text is
placed under duress should be able to say so.

### Constellation names may move, but not off their own figure

A name may take any of its candidates **whose box overlaps the region
its own figure owns**. A name outside its own figure is naming a
different part of the sky, which is worse than the collision it was
avoiding.

The region is the **convex hull of that figure's visible ink**, and
every word of that is load-bearing.

*Hull*, not bounding box: the box of Eridanus, which wanders half the
sky, contains most of Orion, so a name allowed anywhere in that box
could be written over the wrong constellation and still pass. An
earlier draft of this document said "visible figure ink" while the
study measured an axis-aligned extent, which is exactly that mistake.

*Visible ink*, by the renderer's own definition: the midpoints of the
drawn pieces its half-degree subdivision cuts each segment into,
counted where the piece **crosses** the paper. Not the segments'
endpoints — Pyxis on the 90-degree Orion page is drawn with no
endpoint and no sampled point inside the page at all, and an
endpoint-based rule left it unnamed on a page the atlas names it on.

*Overlap*, not "its centre is inside": a figure can be smaller than
its own name. Crater at 90° leaves six pixels by five of visible ink
and CRATER is fifty pixels wide, so the strict reading would refuse
every candidate it has. The stricter statistic is measured and
reported beside the rule so the difference stays visible.

The rule is enforced by the policy this decision is taken from, and
its cost is measured by running that same policy without it: names
leave their own figures on every crowded page — eight of thirty at
`sagittarius-120`, six of twenty-seven at `orion-120` — against none
anywhere with the rule, for a few more collisions.

A name that cannot find an owned candidate stays at the centroid,
where it is today.

### Grid and reference notation stay edge-owned furniture

They keep their present rules: grid notation is refused where the
title block or the key will draw, reference names walk down the paper
edge past names already written. They do not join general placement,
because both are anchored to the page's edge rather than to a place in
the sky, and because both already yield to the only things that can
displace them.

They must, however, become **separable**. The study could not withhold
grid labels apart from grid lines, or reference names apart from
reference curves, because production offers one switch for each pair —
so both are reported as one participant. `#313` publishes the grid's
label decisions and the reference layer's name decisions the way star
labels have been published since #154, which makes them measurable and
lets general placement see them as obstacles.

### The exact geometry

| what | geometry the service uses |
|---|---|
| a label | its box: `metrics.stringWidth` + 4 by `metrics.getHeight()`, origin at the drawn string's left minus 2 and its baseline minus ascent |
| a star's mark | the filled disc at `StarSizePolicy.radiusFor(magnitude)` |
| a deep-sky symbol | the drawn outline, not its rotated bounding box |
| the page | the chart's own clip, inset two pixels |
| the title block, the magnitude key | their published bounds **plus one pixel right and bottom** |

That last row is a defect this gate found and `#313` must fix at the
source. `titleBlockBounds` returns a rectangle of width *w*, and
`drawTitleBlock` calls `drawRect(x, y, w, h)`, which paints a border
at `x + w` and `y + h` — one pixel outside the box everything else is
told about. Two constellation names on the corpus are clipped by it.

Text geometry stays **boxes, not glyphs**. Per-glyph outlines would
buy a few pixels of tolerance and cost determinism across platforms
and font versions, which a printed atlas cannot pay.

### Print, export and paint order

Unchanged, and it already works: the sheet writers replay one
recording of one production render, so SVG, PDF and PNG carry whatever
the renderer decided and no placement question is answered twice.

The extent is a real input and screen and paper use the **same
policy** with different extents, not different budgets. Measured:
`orion-42` at 900×700, at the A4 chart area and at the Letter chart
area are three different placements of one sky, with 69, 63 and 84
collisions respectively.

Paint order is unchanged:

```
grid → boundaries → figures → constellation names → reference layer
     → deep-sky symbols → star marks → star labels → deep-sky labels
     → selection rings → title block → magnitude key
```

with one consequence worth stating: a constellation name is drawn
**before** every mark on the page, so 198 of the corpus's collisions
are a star's disc drawn over a name rather than the other way round.
Moving names later would not repair that; placing them where marks are
not, will.

### Budgets

| | budget | measured today |
|---|---|---|
| placement, 900×700 overview page | **≤ 60 ms** | 45 ms at `orion-120`, 19 ms at `orion-90`, 9 ms at `sagittarius-120` |
| placement, released detail page | **≤ 20 ms** | 5 ms at `home`, 13 ms at `orion-36` |
| labels displaced on a one-step pan | **≤ 15%** | 3 of 84 at `orion-90`, 14 of 124 at `sagittarius-120` |
| labels displaced on a one-rung zoom | no budget | 20 of 84, 20 of 124 |

A zoom changes which stars are on the page at all, so its placements
are not required to resemble the previous rung's. A pan is a nudge and
should look like one.

## Contracts for the issues that follow

**#313, the seam.** One `LabelPlacement` service in `juranometria.render`,
option-free and deterministic, taking anchors and candidate lists and
returning placements with the reason for each. It publishes decisions
for **every** text family, the way `starLabelPlacements` publishes the
star pass's since #154 — including grid notation and reference names.
It changes no reader-visible geometry: with the current families,
priorities and obstacle set reduced to what each family avoids today,
it must reproduce every released page byte for byte. That is the
acceptance test, and the released-page digests are the oracle.

**#314, production placement.** Turns the obstacle set on, family by
family, with the released-page digests changing exactly where this
document says they should and nowhere else. The Nunki fixture is the
named regression: on the Sagittarius overview, Nunki's label must not
share a pixel with Namalsadirah's disc, and Namalsadirah must get its
own letter back.

**#315, the closing journey.** A reader's route through a page whose
labels are placed, on both grounds and both extents, with the study
re-run and its defect counts at their stated floors.

## Rejected

**Page-level optimisation.** Not needed and not affordable to justify.
Greedy with a least-bad fallback takes the observed defects to
single figures with nothing lost; the remaining collisions are
label-over-line, which no amount of optimisation should be spent
removing. A page-level pass would also be harder to make stable under
a pan, which is a property this atlas needs more than it needs the
last few pixels.

**Leader lines.** A line from a displaced label to its star is a fifth
kind of ink on a page that already has four, and the measurement says
it is not needed: the worst displacement in the chosen policy is
116 px on a 900-pixel page, at one label of the whole corpus, and every label is still the nearest text
to its own anchor.

**Per-glyph collision geometry.** Explicitly out of scope in the
issue, and rightly: it is platform- and font-dependent, and a printed
atlas is a promise that the same page comes out the same way.

**Moving constellation names off their own figures.** Measured and
rejected above.

**Making the grid and reference layers join general placement.**
Rejected for now, but the separability they need is required work.

**A general annotation framework.** Out of scope in the issue. The
atlas has five text families and a decided priority between them; a
framework would be a way of not deciding.
