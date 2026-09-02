# What is on this page

Measured by `make on-this-page-study`. Every position, projection and visibility answer comes from production: the projection and viewport mapping the renderer places marks with, the `permitted` rule the family switches obey, the detail policy, and the scene the application's own assembler builds.

## What "on this page" means

**An object is on the page when its recorded position projects onto the paper.** Nothing else: not whether its symbol is drawn, its family switched on, its magnitude inside the limit, or the atlas has a symbol for its type at all.

The paper is the viewport the renderer clips to - `1, 1, width-2, height-2` - which is the same rectangle `drawnMarks` tests against, so a table and the drawing cannot come to disagree about where the page ends. Letterbox chrome is not paper: it is not in the viewport at all.

An object behind the projection's horizon has no place on the page and is not on it. A gnomonic page shows less than a hemisphere, and the projection says so by refusing the point rather than by returning a distant one.

## How much is on a page

| page | why | field | deep-sky | stars | total |
|---|---|---:|---:|---:|---:|
| m31 | the released default | 1° | 3 | 0 | 3 |
| m31 | the released default | 8° | 8 | 48 | 56 |
| m31 | the released default | 18° | 33 | 319 | 352 |
| m31 | the released default | 36° | 352 | 1247 | 1599 |
| orion | bright, familiar, equatorial | 1° | 1 | 3 | 4 |
| orion | bright, familiar, equatorial | 8° | 16 | 144 | 160 |
| orion | bright, familiar, equatorial | 18° | 57 | 474 | 531 |
| orion | bright, familiar, equatorial | 36° | 245 | 1552 | 1797 |
| virgo | the densest galaxies | 1° | 13 | 0 | 13 |
| virgo | the densest galaxies | 8° | 389 | 26 | 415 |
| virgo | the densest galaxies | 18° | 780 | 154 | 934 |
| virgo | the densest galaxies | 36° | 1837 | 647 | 2484 |
| lmc | the Large Magellanic Cloud | 1° | 17 | 0 | 17 |
| lmc | the Large Magellanic Cloud | 8° | 275 | 37 | 312 |
| lmc | the Large Magellanic Cloud | 18° | 374 | 239 | 613 |
| lmc | the Large Magellanic Cloud | 36° | 533 | 1011 | 1544 |
| ra-zero | the seam | 1° | 0 | 1 | 1 |
| ra-zero | the seam | 8° | 17 | 41 | 58 |
| ra-zero | the seam | 18° | 103 | 201 | 304 |
| ra-zero | the seam | 36° | 524 | 848 | 1372 |
| polar | near the pole | 1° | 0 | 2 | 2 |
| polar | near the pole | 8° | 1 | 68 | 69 |
| polar | near the pole | 18° | 10 | 276 | 286 |
| polar | near the pole | 36° | 82 | 1112 | 1194 |

The worst page here carries **1552 stars** and **1837 deep-sky objects**. That is the number a table has to survive, and it decides whether one undifferentiated list is honest.

## Centres are not enough

An object is on the page when its **recorded extent** reaches the paper, not merely its centre. M 31 is 178 arcminutes long: a page can be filled by its disc while its centre sits outside the paper, and a centre-only rule would report an empty page in front of a visible galaxy.

| page | field | centres only | with recorded extent | missed |
|---|---:|---:|---:|---:|
| m31 | 1° | 1 | 3 | **2** |
| m31 | 18° | 30 | 33 | **3** |
| m31 | 36° | 351 | 352 | **1** |
| orion | 18° | 55 | 57 | **2** |
| orion | 36° | 244 | 245 | **1** |
| virgo | 18° | 779 | 780 | **1** |
| virgo | 36° | 1834 | 1837 | **3** |
| lmc | 8° | 274 | 275 | **1** |
| lmc | 36° | 532 | 533 | **1** |

**15 objects** across these pages would have been left out of a table that asked only about centres - among them M 32 and M 110 on a 1° view of M 31, which is the closest look the atlas offers at the page it opens on.

The extent used is the **catalogue's** size, never the drawn symbol's. That keeps the inventory a fact about the sky: it does not move when a family is switched off, when the detail policy refuses a symbol, or when the practical-minimum clamp enlarges a tiny one for legibility.

## Present, and why it cannot be seen

Every state is production's own answer. `permitted` is the rule the family switches obey, the detail policy is the one the renderer asks, and the magnitude limit is the scene's. Nothing here infers a state from a missing pixel.

| page | field | drawn | hidden by an option | fainter than the limit | no symbol | too small at this field |
|---|---:|---:|---:|---:|---:|---:|
| m31 | 8° | 55 | 0 | 0 | 1 | 0 |
| m31 | 36° | 1253 | 0 | 0 | 49 | 297 |
| orion | 8° | 158 | 0 | 0 | 2 | 0 |
| orion | 36° | 1564 | 0 | 0 | 26 | 207 |
| virgo | 8° | 369 | 0 | 0 | 46 | 0 |
| virgo | 36° | 669 | 0 | 0 | 289 | 1526 |
| lmc | 8° | 293 | 0 | 0 | 19 | 0 |
| lmc | 36° | 1017 | 0 | 0 | 26 | 501 |
| ra-zero | 8° | 55 | 0 | 0 | 3 | 0 |
| ra-zero | 36° | 848 | 0 | 0 | 52 | 472 |
| polar | 8° | 69 | 0 | 0 | 0 | 0 |
| polar | 36° | 1113 | 0 | 0 | 4 | 77 |

Switching **Galaxies** off on the released page moves **7** objects from *drawn* to *hidden by a chart option* - 55 to 48 - without changing what the page contains. That is the distinction the table exists to make: presence is a fact about the sky, and visibility is a fact about the reader's choices.

## The stars, and whether they belong in it

A page carries far more stars than deep-sky objects, and nearly all of them are a catalogue number and a brightness. A row reading *TYC 2801-2090-1, 4.5* answers no question a reader had. So the question is not whether stars are on the page - they are, and the table must not pretend otherwise - but which of them a reader could have come looking for.

| page | field | stars | with a name, Bayer letter or Flamsteed number | anonymous |
|---|---:|---:|---:|---:|
| m31 | 8° | 48 | 4 | 44 |
| m31 | 36° | 1247 | 94 | 1153 |
| orion | 8° | 144 | 11 | 133 |
| orion | 36° | 1552 | 115 | 1437 |
| virgo | 8° | 26 | 5 | 21 |
| virgo | 36° | 647 | 89 | 558 |
| lmc | 8° | 37 | 5 | 32 |
| lmc | 36° | 1011 | 89 | 922 |
| ra-zero | 8° | 41 | 3 | 38 |
| ra-zero | 36° | 848 | 103 | 745 |
| polar | 8° | 68 | 2 | 66 |
| polar | 36° | 1112 | 58 | 1054 |

**The magnitude limit only bites when a reader asks it to.** The bundled pack is bright sky, so at the released limit of V 8 nothing on any page above is past it. Set Orion's 18° page to V 4 and **463 of its 474 stars** become present-but-unplotted - which is precisely the state a reader cannot discover by looking at the paper.

## The order a reader meets them

Proposed default: **a Messier number first, then recorded brightness, then angular distance from the centre, then catalogue identity.** Identity last makes it total, so the same page always lists in the same order however the catalogue arrives.

The released page, in that order:

| # | object | Messier | magnitude | from centre |
|---:|---|---|---:|---:|
| 1 | NGC 224 | M 31 | 3.4 | 0.00° |
| 2 | NGC 221 | M 32 | 8.1 | 0.40° |
| 3 | NGC 205 | M 110 | 8.2 | 0.61° |
| 4 | NGC 317A | — | 13.6 | 3.74° |
| 5 | NGC 317B | — | 13.9 | 3.73° |
| 6 | IC 1550 | — | 15.0 | 4.67° |
| 7 | NGC 206 | — | not recorded | 0.67° |
| 8 | NGC 317 | — | not recorded | 3.73° |

**8** deep-sky rows in all on that page.

## Working it without a pointer

The platform's own bindings, resolved from the table's input map and fired - the same actions Swing runs for those keystrokes - on the released page's real rows. A picture of a sidebar cannot answer this; running it can.

| keystroke | Swing's action | rows selected | lead |
|---|---|---|---|
| `DOWN` | `selectNextRow` | M 31 | M 31 |
| `DOWN` | `selectNextRow` | M 32 | M 32 |
| `shift DOWN` | `selectNextRowExtendSelection` | M 32, M 110 | M 110 |
| `shift DOWN` | `selectNextRowExtendSelection` | M 32, M 110, NGC 317A | NGC 317A |
| `ctrl pressed A` | *no binding* | — | — |
| `meta A` | `selectAll` | M 31, M 32, M 110, NGC 317A, … (13 rows) | NGC 317A |
| `HOME` | `selectFirstColumn` | NGC 317A | NGC 317A |
| `ctrl pressed HOME` | *no binding* | — | — |

**Walking and extending are free.** Down moves the lead one row; shift-Down builds a marked set out of consecutive rows. Those are the two gestures the surface is mostly made of, and the platform already has them.

**Select-all is there, under the platform's own modifier and not the other one.** `meta A` runs `selectAll` and takes all 13 rows; `ctrl A` is bound to nothing. Which modifier that is belongs to the look and feel, and the module has no business choosing it.

**And returning to the top is not bound.** `HOME` moves to the first *column*, and `ctrl HOME` is bound to nothing at all - so a reader pressing Home to get back to M 31 stays where they are. That is a real gap, found by running the bindings rather than by assuming them, and it is the sort of thing a picture of a sidebar could never have shown.

So the decision is narrow and stated: **the module adds no key bindings of its own.** Where the platform binds a gesture the module uses it and does not care which modifier the look and feel chose; where it binds nothing - getting back to the top - #216 offers an explicit control beside **Clear marks** rather than inventing a keystroke. A module that taught the table new keys would be a module assistive technology has to be taught too.

What a reader needs beyond the platform's own is decided rather than invented: **Enter** takes the lead row into the Selected facts, and **Centre here** is an explicit action rather than a side effect of moving through rows. Selecting a row never moves the chart, so a reader can walk the whole page without losing their place - the promise point-and-identify has made since Sprint 19.

## What it costs to know

The scene the chart already assembled holds **every** star and deep-sky object in the queried region, unfiltered - the renderer applies the magnitude limit as it draws. So an inventory needs no catalogue query of its own: it is a projection sweep over a list the page is already holding.

Across the six pages at 8°, that is **3914 objects** to project - arithmetic, no catalogue, no allocation beyond the answer.

Timings are deliberately absent from this report. They differ between machines and between two runs on one machine, and a study whose output moves cannot be reproduced - the same rule the Milky Way gate settled. #215 sets the interaction budget and enforces it on the supported CI path.

