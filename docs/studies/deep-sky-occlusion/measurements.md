# Overlapping deep-sky symbols

Measured by `make deep-sky-occlusion-study` over the bundled all-sky pack and the released default page. Every placement, order and pixel comes from production: `ChartRenderer.drawnMarks` publishes what is painted and in what order, and an object's surviving ink is measured by rendering the same page again without it.

## What the pack contains

Asking the sky rather than any one page, **1667** of the 13371 bundled rows' centres fall inside a galaxy's disc, and in **1165** of those the disc is the larger of the two. Only the galaxy ellipse paints an opaque interior - every other symbol in the vocabulary is an outline and crosses nothing out - so a galaxy is the only mark that can bury another.

**That figure is an upper bound, not the answer.** 501 of the 1165 are pairs of sub-arcminute galaxies inside a disc under two arcminutes - NED components of one catalogued system, a few tenths of an arcminute apart. At any field width a reader actually uses, the practical-minimum clamp enlarges both to the same drawn size, so neither contains the other on the page however the sky is arranged. Sky geometry cannot answer this; only drawn geometry can, which is what the next section measures.

What survives that reasoning is a smaller and much more interesting population: a genuinely large galaxy holding catalogued objects of its own. Grouped by the disc doing the covering:

| the disc | its size | objects inside it |
|---|---:|---:|
| ESO056-115 | 646' | 318 |
| NGC 292 | 300' | 39 |
| NGC 598 | 62' | 15 |
| NGC 5457 | 24' | 10 |
| NGC 4559 | 11' | 7 |
| NGC 224 | 178' | 2 |
| NGC 1097 | 11' | 1 |
| NGC 2403 | 20' | 1 |
| NGC 4472 | 10' | 1 |
| NGC 5194 | 14' | 1 |
| NGC 6822 | 17' | 1 |

**11** discs, holding **396** objects between them. The Magellanic Clouds dominate the list and explain themselves: the atlas draws the Large Cloud as one filled ellipse nearly eleven degrees across, and the hundreds of clusters and nebulae the catalogue records *within* it all fall inside that disc. Under storage order every one of them is at the mercy of where its row happens to sit.

M 31 with M 32 is the same defect on a page every reader opens.

## What each order does, at drawn sizes

A mark is **fully covered** when some filled disc painted after it contains the whole of its outline: exact geometry over the outlines `drawnMarks` publishes, at the sizes the page really draws, clamp included. Storage order is the order the scene's own list arrives in - what the renderer used before this issue.

| page | field | symbols | fully covered, storage order | fully covered, stacking rule |
|---|---:|---:|---:|---:|
| m31-default | 2° | 3 | 1 | 0 |
| m31-default | 8° | 7 | 1 | 0 |
| m31-default | 36° | 6 | 1 | 0 |
| lmc | 2° | 43 | 0 | 0 |
| lmc | 8° | 258 | 0 | 0 |
| lmc | 36° | 6 | 0 | 0 |
| smc | 2° | 25 | 17 | 0 |
| smc | 8° | 52 | 23 | 0 |
| smc | 36° | 3 | 0 | 0 |
| m33 | 2° | 10 | 8 | 0 |
| m33 | 8° | 65 | 8 | 0 |
| m33 | 36° | 6 | 1 | 0 |
| virgo | 2° | 38 | 0 | 0 |
| virgo | 8° | 345 | 0 | 0 |
| virgo | 36° | 22 | 0 | 0 |
| m51 | 2° | 12 | 0 | 0 |
| m51 | 8° | 21 | 0 | 0 |
| m51 | 36° | 7 | 0 | 0 |

Across these 18 pages, storage order fully buried **60** symbols that the reader was nevertheless told about. The stacking rule leaves **0**.

## The released default page

M 31 at a 8-degree field, stars to V 8.0 - the page the application opens on.

### The order the renderer paints in

Published by `drawnMarks`, which is the list the renderer paints from - so this is the drawing order itself, not a description of it.

| painted | object | drawn axes (px) | symbol ink (px) |
|---:|---|---:|---:|
| 1 | NGC 224 | 283 x 219 | 33834 |
| 2 | NGC 205 | 30 x 18 | 325 |
| 3 | NGC 221 | 14 x 9 | 42 |
| 4 | IC 1550 | 6 x 6 | 28 |
| 5 | NGC 317 | 6 x 6 | 12 |
| 6 | NGC 317A | 6 x 5 | 12 |
| 7 | NGC 317B | 6 x 3 | 13 |

**Symbol ink** is measured, not assumed: the page is rendered again with that one object removed, and the pixels that change inside its own outline are the ones its symbol contributes. Labels are switched off and the searched target cleared first, because under the defect M 32's label went on drawing while its ellipse was gone entirely - counting the label would have reported ink for a mark no reader could see. Zero means exactly that: a label with nothing under it.

### Andromeda's three galaxies

| galaxy | painted | inside M 31's disc | symbol ink (px) |
|---|---:|---|---:|
| NGC 224 | 1 | - | 33834 |
| NGC 221 | 3 | **entirely** | 42 |
| NGC 205 | 2 | partly | 325 |

M 31's disc **entirely contains** M 32's ellipse: every point of the smaller mark lies within the larger one, and the larger one is filled. Painting M 31 second - which storage order did - therefore leaves M 32 no ink at all, whatever the page. The stacking rule paints it first instead, and M 32 keeps its outline.


Images in [`docs/studies/deep-sky-occlusion/`](.).
