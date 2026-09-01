# Decision: which deep-sky symbol goes behind which

**Sprint 23, issue #201.** Measured with
`make deep-sky-occlusion-study` →
[`docs/studies/deep-sky-occlusion/`](../studies/deep-sky-occlusion/).

## The defect

The atlas's founding page names three galaxies and showed two.

M 31, M 32 and M 110 are all loaded, all accepted by
`RegionalDetailPolicy`, all published by `drawnMarks`, and all
labelled. What went wrong was the order they were painted in. The
bundled rows reach the default page as **NGC 205, NGC 221, NGC 224**
— storage order — and the galaxy symbol fills its interior opaquely.
So M 31's disc, 178 arcminutes of it, was painted last and covered
M 32's 7.7-arcminute ellipse completely, and part of M 110's.

Labels draw in a later pass, so **M 32's label survived its symbol**.
A reader met the name, looked for the third ellipse, and could not
find it — on the page the application opens on.

Catalogue order is storage order. It is not cartography, and it was
never meant to be.

## The rule

**The larger painted footprint goes behind. Identity breaks ties.**

One rule, applied to every deep-sky mark, at the renderer's existing
placement seam: `drawnMarks` sorts the symbols it publishes, and the
renderer paints the list it is given. There is no second geometry and
no second order.

### Why the *painted* footprint

The measure is the area a symbol's own ink encloses **at the size the
page actually draws it**, computed from the drawn axes.

- **Not the catalogue's major axis.** The practical-minimum clamp
  enlarges a tiny object so it stays visible, and two objects must be
  compared at the sizes the page gives them, not the sizes the
  catalogue records. Sub-arcminute pairs that look nested in the sky
  are drawn the same size and nest in neither direction.
- **Not the bounding box.** A box grows as an ellipse turns — a
  40′×4′ galaxy at 45° bounds a larger square than the same galaxy
  upright, though it covers exactly as much paper. Ordering by one
  would let a companion surface or submerge as its neighbour rotates.
  Area from the axes is the same whichever way an object lies. This
  was verified rather than assumed: a regression drives two galaxies
  whose boxes and areas disagree, and it is the only test that the
  bounding-box mutant fails.

This is the same reasoning, and the same seam, that already defines a
symbol's `reach`.

### Why identity breaks ties

So the order cannot depend on tile, CSV, map or collection iteration.
Reversing the scene's list leaves the published order identical and
the rendered page byte-for-byte the same — asserted, with the reversal
itself asserted first so the test cannot pass vacuously.

### What the rule does not do

It does not move a catalogued position, invent a leader line, make a
fill transparent, resize a symbol, or teach hit testing about ink the
reader cannot see. It changes one thing: the order.

## What it changes

Measured over the bundled all-sky pack at drawn sizes, on 18 pages —
six regions at 2°, 8° and 36°:

| | fully covered symbols |
|---|---:|
| storage order | **60** |
| the stacking rule | **0** |

A symbol counts as fully covered when a filled disc painted after it
contains the whole of its outline: exact geometry over the published
placements, not a pixel sample.

The pack holds **11 galaxy discs of ten arcminutes or more that
contain other catalogued objects**, 396 objects between them. The
Magellanic Clouds dominate and explain themselves — the atlas draws
the Large Cloud as one filled ellipse nearly eleven degrees across,
and the hundreds of clusters and nebulae catalogued *within* it all
fall inside that disc. Under storage order each of them depended on
where its row happened to sit.

**Only the galaxy ellipse fills.** Every other symbol in the
vocabulary is an outline and crosses nothing out, so a galaxy is the
only mark that can bury another. The rule is still defined for all of
them, because one rule that orders every mark is easier to reason
about — and to test — than a rule with a family exception in it.

### The default page, after

| galaxy | painted | inside M 31's disc | leaves ink |
|---|---:|---|---:|
| M 31 / NGC 224 | 1st | — | 34,750 px |
| M 110 / NGC 205 | 2nd | partly | 566 px |
| M 32 / NGC 221 | 3rd | **entirely** | 220 px |

Surviving ink is measured, not argued: the page is rendered again
with that one object removed, and the pixels that change are the ones
it contributes. M 32 sits on M 31's fill, so what identifies it is its
own grey-132 outline — which is exactly how the chart distinguishes a
galaxy anywhere else.

## Unavoidable cases

Where two outlines coincide closely enough that no order can show
both, no order is invented to pretend otherwise. **None occur in the
measured set**: the stacking rule leaves zero fully covered symbols
across all 18 pages. Should one appear, the tie-break decides it
deterministically and the smaller mark is the one drawn on top, which
is the useful half of an unusable pair.

## The released reference

`docs/reference/m31-stars.png` changes by **201 pixels of 630,000
(0.032%)**, confined to x 445–505, y 296–402 — the region holding
M 31's two companions. Those pixels are M 32's outline appearing and
M 110's covered portion returning.

The old bytes are released, and they encode the defect. They are not
preserved for that reason.

## Pointing

Hit testing already ordered its answer by *ink before nearness, then
distance, then the tighter mark, then identity* — never by paint
order — so it needed no change and got none. Standing on M 32 answers
with M 32 rather than the disc it sits on, and each of the three is
reachable through a real pointer interaction. That is asserted, not
assumed.
