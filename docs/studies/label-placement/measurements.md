# How labels share an atlas page

What this file is: the measurement behind issue #310, Sprint 31's cartography and
architecture gate. It inventories what the atlas does with text today, measures
where that goes wrong, and compares the candidate policies for #313 to build
against. It changes no production behaviour and no released page.

Recorded on: `Mac OS X 26.5.2/aarch64/Homebrew 21.0.11`, and that matters here. Every
number below is a count of pixels, so it is reproducible on a machine rather than
across machines, exactly like the atlas's other renderer-drawn evidence: font
rasterisation differs, and the same collision that shares 68 pixels here shares
27 on a Linux runner. What does not differ is which pairs collide and which do
not, which is what this document is for.

## What counts as a collision here

Two rules, and both of them cost this study a rewrite.

**A box is not ink.** "Nunki σ" reserves a box 45 pixels wide of which the letters
are about half, and a star's mark is a disc in a square that is empty at the
corners. Counting overlapping rectangles reports collisions a reader cannot see.

**Ink at a place is not that thing's ink.** A figure line, a boundary, a grid line
and a star all cross the same page, and any of them will answer for another if
all that is asked is whether something is there.

So every collision below is measured by taking the participants away, and by taking
them away *separately*:

```
one   = page without other  -  page without both
other = page without one    -  page without both
collision = one ∩ other
```

Each piece is measured with the other absent, which matters twice. Ink drawn
later hides ink drawn earlier, so measuring both on the finished page would make
their visible sets disjoint by construction and find nothing at all. And ink drawn
over ink of its own colour changes no pixel, so measuring the later one on the
finished page quietly loses the pixels where a dark glyph lands on a dark line -
which on a crowded page is most of the collision.

An earlier draft of this study did both, and its own paint-order check caught it: a
boundary appeared to be covering nine pixels of a label that was covering five of
it, which is impossible for ink underneath. The same check found a second one -
withholding a star to measure its disc was taking that star's *name* with it, so
a name across another star's mark and a name across another star's name were the
same reading. Both are the gate's own rule turned on the gate: an oracle that cannot
distinguish two causes will credit the wrong one.

Both participants are named by construction: each because taking it away changed
those pixels while the other was not there to. Rectangles are still used, but only
to **nominate** pairs worth painting; nothing reaches a table below that a nomination
alone decided.

## The pages

| page | centre | field | limit | extent | why |
|---|---|---:|---:|---|---|
| `home` | 10.7°, +41.3° | 8° | V 8.0 | 900x700 | the released Home page, and the control for every claim about not making it worse |
| `orion-08` | 83.0°, +0.0° | 8° | V 8.0 | 900x700 | a detail page: every label form the policy allows |
| `orion-18` | 83.0°, +0.0° | 18° | V 8.0 | 900x700 | the field where Latin Bayer letters stop |
| `orion-36` | 83.0°, +0.0° | 36° | V 8.0 | 900x700 | the widest detail page |
| `orion-42` | 83.0°, +0.0° | 42° | V 8.0 | 900x700 | the sheet page, gnomonic's last rung |
| `orion-60` | 83.0°, +0.0° | 60° | V 5.0 | 900x700 | the first overview rung |
| `orion-90` | 83.0°, +0.0° | 90° | V 4.0 | 900x700 | the winter sky, where the owner saw the defect |
| `orion-120` | 83.0°, +0.0° | 120° | V 4.0 | 900x700 | the widest page the atlas draws |
| `sagittarius-90` | 266.0°, -28.0° | 90° | V 4.0 | 900x700 | the Milky Way, densest labelling in the sky |
| `sagittarius-120` | 266.0°, -28.0° | 120° | V 4.0 | 900x700 | the named fixture's page: Nunki against Namalsadirah |
| `cygnus-90` | 310.0°, +40.0° | 90° | V 4.0 | 900x700 | the summer triangle and the Milky Way's north |
| `crux-90` | 187.0°, -60.0° | 90° | V 4.0 | 900x700 | the southern sky, small figures close together |
| `pole-120` | 0.0°, +90.0° | 120° | V 4.0 | 900x700 | the pole, where the graticule converges |
| `seam-120` | 0.0°, +0.0° | 120° | V 4.0 | 900x700 | the right-ascension seam |
| `orion-120-small` | 83.0°, +0.0° | 120° | V 4.0 | 700x500 | a reduced window: the same sky in less room |
| `orion-42-a4` | 83.0°, +0.0° | 42° | V 8.0 | A4 770x523 | the A4 sheet's own chart extent |
| `orion-42-letter` | 83.0°, +0.0° | 42° | V 8.0 | Letter 720x540 | the US Letter sheet's own chart extent |
| `orion-90-black` | 83.0°, +0.0° | 90° | V 4.0 | 900x700 | black sky: the other ground the atlas ships |
| `sagittarius-90-key` | 266.0°, -28.0° | 90° | V 4.0 | 900x700 | with the magnitude key, the second piece of furniture |
| `sagittarius-90-ecliptic` | 266.0°, -28.0° | 90° | V 4.0 | 900x700 | the ecliptic module's line, its name and its landmarks |
| `orion-90-observer` | 83.0°, +0.0° | 90° | V 4.0 | 900x700 | the meridian, the mathematical horizon and the zenith |
| `nunki-searched` | 283.8°, -26.3° | 90° | V 4.0 | 900x700 | a searched target: the one label that is guaranteed |
| `orion-18-selected` | 83.0°, +0.0° | 18° | V 8.0 | 900x700 | a working selection: three members wearing their rings |

Every page is assembled by the production assembler and painted by the production
renderer, through the same reference layer the chart component hands it. The
A4 and Letter extents are the chart areas the sheet writer actually draws into.

The atlas's chart text does **not** follow the application's text-size setting:
the renderer's label face is a fixed 11-point sans and the constellation-name
face a fixed 12-point sans, so enlarging application text changes the interface
around the chart and not the chart. What does change placement is the room:
`orion-120-small` is the same sky in a 700×500 window.

## The existing truth, family by family

| family | anchor | eligibility | may move | omission | clipping | draw order | option |
|---|---|---|---|---|---|---:|---|
| searched target | the target's own mark | exempt from every magnitude threshold | no | never omitted; drawn first and reserves its box | page rectangle | 8 | none: a target is always named |
| star name, letter, number | east of the star's disc at its magnitude radius | `StarLabelPolicy` limits by field band | no | omitted when its box meets an accepted box | refused off-page | 8 | Star names, Bayer letters, Flamsteed numbers |
| deep-sky label | east of the symbol's own half-extent | `RegionalDetailPolicy` | no | never: it is placed before star labels and they yield to it | none | 9 | Deep-sky labels |
| constellation name | centroid of the constellation's **visible figure ink** | a figure that leaves ink | no | never omitted | clipped at the page edge until #313 | 4 | Constellation names, and figures |
| equatorial grid notation | the page edge the line leaves by | spacing policy | no | omitted when it meets the title block or key | kept inside the paper | 1 | Equatorial grid |
| meridian, horizon, ecliptic names | the upper end of the curve's own run | the module's | down the edge, past names already written | omitted when there is no room left below | paper rectangle | 5 | the module's own control |
| title block, magnitude key | the page's own corners | the reader's switch | no | omitted when the page is too small | none | 10, 11 | Title block, Magnitude key |

Draw order is the renderer's own sequence, and it decides who covers whom.

### What each family actually avoids

Not read off the code: counted. A pair that collides on these pages is a pair
that is not being avoided, and a pair that never collides is either avoided or
never near. The distinction is in the footnote under the table.

| text family | grid | boundary | figure | const. name | reference | dso symbol | star disc | star label | dso label | ring | title | key |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| const. name | 150 | 52 | 17 | — | 2 | 30 | 228 | 56 | 35 | · | 20 | · |
| star label | 179 | 140 | 305 | 56 | 6 | 59 | 252 | — | · | · | · | · |
| dso label | 97 | 40 | 61 | 35 | 15 | 26 | 51 | · | — | · | 6 | · |

A dot is no collision anywhere in the corpus. Star labels and deep-sky labels
never meet because the star-label pass yields to the deep-sky boxes before it
places anything; star labels never meet each other for the same reason; and
neither meets the title block or the key, which they both reserve. Everything
else is a collision nobody is preventing.

Two families cannot be separated by this study and should be separable by
#313. The equatorial grid draws its lines and its edge notation under one reader
switch, and the reference layer its curves and their names under another, so
neither can be withheld apart from the other and this document reports each as
one participant. The star-label pass has published its decisions since #154; the
other text families have not.

## What the atlas draws today

| page | text drawn | collisions | pixels | worst single | order check |
|---|---:|---:|---:|---:|---:|
| `home` | 3 | 2 | 146 | 136 | 1.00 |
| `orion-08` | 12 | 8 | 88 | 16 | 1.07 |
| `orion-18` | 19 | 23 | 261 | 33 | 1.00 |
| `orion-36` | 17 | 32 | 348 | 51 | 1.00 |
| `orion-42` | 24 | 69 | 685 | 47 | 1.07 |
| `orion-60` | 39 | 51 | 913 | 87 | 1.00 |
| `orion-90` | 96 | 63 | 1371 | 172 | 1.00 |
| `orion-120` | 174 | 169 | 3438 | 198 | 1.00 |
| `sagittarius-90` | 88 | 84 | 1091 | 70 | 1.00 |
| `sagittarius-120` | 106 | 147 | 2516 | 110 | 1.14 |
| `cygnus-90` | 54 | 69 | 1315 | 91 | 0.92 |
| `crux-90` | 74 | 74 | 1319 | 90 | 0.89 |
| `pole-120` | 95 | 114 | 2545 | 109 | 1.00 |
| `seam-120` | 101 | 91 | 1442 | 94 | 1.00 |
| `orion-120-small` | 195 | 263 | 3414 | 185 | 1.06 |
| `orion-42-a4` | 21 | 63 | 610 | 47 | 1.04 |
| `orion-42-letter` | 24 | 84 | 821 | 48 | 1.00 |
| `orion-90-black` | 96 | 63 | 1365 | 172 | 1.00 |
| `sagittarius-90-key` | 87 | 84 | 1091 | 70 | 1.00 |
| `sagittarius-90-ecliptic` | 88 | 102 | 1701 | 70 | 1.06 |
| `orion-90-observer` | 96 | 68 | 1440 | 172 | 1.00 |
| `nunki-searched` | 83 | 81 | 1100 | 71 | 1.00 |
| `orion-18-selected` | 19 | 23 | 261 | 33 | 1.00 |
| **all 23 pages** | | **1827** | **29281** | | **1.14** |

The collision measure does not depend on who is on top: each participant's ink is
measured with the other absent, and the collision is the intersection. Which of the
two a reader sees is the renderer's own drawing sequence, and the last column checks
that sequence rather than trusting it. Of the shared pixels, each participant is
asked how many it is still holding on the finished page - how many its removal
would change - and the one drawn later should hold at least as many. The column
is the worst ratio of earlier-held to later-held over the page's family pairs.
Anything above one would mean this study reports its collisions the wrong way
round.

Where it sits a little above one, the two families share a tone. A constellation
name and a figure line are drawn in the *same* grey, so a name laid on that line
changes no pixel and the measurement credits the line with holding what the name
is covering. The atlas's text inks, for reference: star and deep-sky labels
`#222222`, constellation names and figure lines `#787878`, star marks `#000000`,
grid and reference notation lighter still.

### By pair

| what covers what | how often | pixels |
|---|---:|---:|
| star label over figure | 305 | 6420 |
| star label over star disc | 252 | 3381 |
| star disc over const. name | 228 | 3621 |
| star label over grid | 179 | 2816 |
| const. name over grid | 150 | 3385 |
| star label over boundary | 140 | 1445 |
| dso label over grid | 97 | 1290 |
| dso label over figure | 61 | 442 |
| star label over dso symbol | 59 | 778 |
| star label over const. name | 56 | 2040 |
| const. name over boundary | 52 | 654 |
| dso label over star disc | 51 | 212 |
| dso label over boundary | 40 | 208 |
| dso label over const. name | 35 | 239 |
| dso symbol over const. name | 30 | 371 |
| dso label over dso symbol | 26 | 330 |
| title over const. name | 20 | 559 |
| const. name over figure | 17 | 171 |
| dso label over reference | 15 | 526 |
| star label over reference | 6 | 83 |
| title over dso label | 6 | 240 |
| reference over const. name | 2 | 70 |

## The two defects the owner saw

These are the gate's first priority, and they are counted apart from everything
else rather than folded into a total that a hairline crossing could dominate.

| page | const. name ↔ star label | star label over an unrelated disc | its own disc |
|---|---:|---:|---:|
| `home` | 0 | 1 | excluded |
| `orion-08` | 0 | 2 | excluded |
| `orion-18` | 0 | 7 | excluded |
| `orion-36` | 0 | 13 | excluded |
| `orion-42` | 0 | 27 | excluded |
| `orion-60` | 4 | 9 | excluded |
| `orion-90` | 4 | 7 | excluded |
| `orion-120` | 5 | 18 | excluded |
| `sagittarius-90` | 2 | 5 | excluded |
| `sagittarius-120` | 9 | 14 | excluded |
| `cygnus-90` | 5 | 2 | excluded |
| `crux-90` | 2 | 7 | excluded |
| `pole-120` | 3 | 11 | excluded |
| `seam-120` | 2 | 3 | excluded |
| `orion-120-small` | 5 | 23 | excluded |
| `orion-42-a4` | 0 | 31 | excluded |
| `orion-42-letter` | 0 | 37 | excluded |
| `orion-90-black` | 4 | 7 | excluded |
| `sagittarius-90-key` | 2 | 5 | excluded |
| `sagittarius-90-ecliptic` | 2 | 5 | excluded |
| `orion-90-observer` | 4 | 7 | excluded |
| `nunki-searched` | 3 | 4 | excluded |
| `orion-18-selected` | 0 | 7 | excluded |
| **all pages** | **56** | **252** | |

The last column says what is deliberately not counted: a star's own disc, which its
name is anchored beside by decision. Counting it would bury the defect the owner
reported - a name across somebody *else's* star - under one entry per label on
the page.

A constellation name is also covered by star **discs**, which is the same defect
with the layers the other way up: the name is drawn during the geography pass
and every mark on the page is drawn after it.

## The named fixture: Nunki against Namalsadirah

| star | catalogue | chart identity | V | drawn radius |
|---|---|---|---:|---:|
| Nunki | `TYC 6868-1829-1` | σ Sgr · Flamsteed 34 | 2.06 | 4.38 px |
| Namalsadirah | `TYC 6867-2428-1` | φ Sgr · Flamsteed 27 | 3.13 | 3.94 px |

On the production `sagittarius-120` page, the four pieces the fixture asks to be
kept apart, each one measured by withholding it and painting the page again:

| piece | withheld by | ink | meets Nunki's label |
|---|---|---:|---|
| Nunki's label glyphs | its star's identity set aside, its mark kept | 284 px | — |
| Nunki's own disc | its star removed from the scene | 76 px | no |
| Namalsadirah's disc | its star removed from the scene | 56 px | **yes, 44 px** at 354,345 |
| Namalsadirah's label glyphs | its star's identity set aside | 0 px | no |

The label is anchored beside its own disc, which is why that row is not a defect.
The row that is, is Namalsadirah's: a magnitude 3.1 mark, drawn at nearly four
pixels of radius, with a smaller star's name written across it. The two are
separated here by two different surgeries on the same scene, so neither can be
answering for the other.

The shared area is larger than Namalsadirah's own visible ink, and that is the
defect stated arithmetically: what the mark has left on the finished page is what
the name did not cover.

**The fourth participant is missing, and its absence is the same defect.**
Namalsadirah qualifies for a Bayer letter at this field - V 3.13 against a limit of
V 3.5, and the policy returns "φ" for it - but the page does not draw one. The star-
label pass takes stars brightest first, accepts "Nunki σ", and then refuses φ
because its box meets the accepted one. So the atlas silently declines to name a
star in order to protect a name it then draws across that same star's mark. The
omission is not recorded anywhere; it is the pass returning early.

The fixture reproduces on the page a reader reaches, not only on one contrived
centre. Nunki's label box meets Namalsadirah's drawn disc at:

| centre | field | 900×700 | 1100×800 |
|---|---:|---|---|
| Sagittarius | 42° | **meets** | clear |
| Sagittarius | 60° | **meets** | **meets** |
| Sagittarius | 90° | **meets** | **meets** |
| Sagittarius | 120° | **meets** | **meets** |
| Nunki itself | 42° | clear | clear |
| Nunki itself | 60° | **meets** | clear |
| Nunki itself | 90° | **meets** | **meets** |
| Nunki itself | 120° | **meets** | **meets** |
| the Milky Way | 42° | clear | clear |
| the Milky Way | 60° | **meets** | **meets** |
| the Milky Way | 90° | **meets** | **meets** |
| the Milky Way | 120° | **meets** | **meets** |

## The candidate policies

Three variants of one deterministic greedy pass, differing in exactly the
questions the gate has to answer. Each takes the labels in a fixed priority, gives
each a fixed ordered list of candidate positions around its own anchor - east
first, which is where every label sits today - and takes the first that no
accepted box, no drawn mark, no furniture and no page edge refuses. A label with
no free candidate is omitted rather than drawn over something.

Their pages are painted the same way the released page is: production's chart
with the text families switched off through the reader's own options, and the
candidate's text written on top in the renderer's own fonts and inks. They are
then put through the same collision oracle, so nothing below is a candidate
marking its own work.

| page | policy | collisions | of the two defects | lost | gained | moved | worst move | candidates tried |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| `home` | the atlas today | 2 | 1 | — | — | — | — | — |
| | greedy, star labels first | 0 | 0 | 1 | 0 | 1 | 22 px | 19 |
| | greedy, constellation names first | 0 | 0 | 1 | 0 | 1 | 22 px | 19 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 1 | 0 | 1 | 22 px | 19 |
| | greedy, star labels first, keeping what it cannot place | 1 | 0 | 0 | 1 | 1 | 22 px | 19 |
| | greedy, star labels first, least bad when nothing is free | 1 | 0 | 0 | 1 | 1 | 22 px | 19 |
| `orion-36` | the atlas today | 32 | 19 | — | — | — | — | — |
| | greedy, star labels first | 8 | 0 | 9 | 0 | 5 | 47 px | 148 |
| | greedy, constellation names first | 8 | 0 | 9 | 0 | 5 | 47 px | 148 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 12 | 0 | 4 | 52 px | 163 |
| | greedy, star labels first, keeping what it cannot place | 36 | 18 | 0 | 1 | 5 | 47 px | 148 |
| | greedy, star labels first, least bad when nothing is free | 25 | 7 | 0 | 1 | 5 | 47 px | 148 |
| `orion-90` | the atlas today | 63 | 19 | — | — | — | — | — |
| | greedy, star labels first | 40 | 0 | 40 | 5 | 26 | 87 px | 545 |
| | greedy, constellation names first | 39 | 0 | 36 | 7 | 27 | 87 px | 543 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 60 | 3 | 30 | 75 px | 776 |
| | greedy, star labels first, keeping what it cannot place | 201 | 4 | 0 | 11 | 27 | 87 px | 563 |
| | greedy, star labels first, least bad when nothing is free | 58 | 2 | 0 | 11 | 25 | 87 px | 568 |
| `orion-120` | the atlas today | 169 | 37 | — | — | — | — | — |
| | greedy, star labels first | 88 | 0 | 78 | 8 | 57 | 88 px | 1008 |
| | greedy, constellation names first | 86 | 0 | 79 | 32 | 57 | 88 px | 983 |
| | greedy, star labels first, avoiding every line | 2 | 0 | 118 | 3 | 48 | 89 px | 1448 |
| | greedy, star labels first, keeping what it cannot place | 117 | 10 | 0 | 15 | 58 | 88 px | 1023 |
| | greedy, star labels first, least bad when nothing is free | 472 | 5 | 0 | 15 | 48 | 88 px | 1060 |
| `sagittarius-120` | the atlas today | 147 | 48 | — | — | — | — | — |
| | greedy, star labels first | 82 | 0 | 14 | 14 | 40 | 116 px | 442 |
| | greedy, constellation names first | 84 | 0 | 10 | 14 | 42 | 116 px | 416 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 49 | 10 | 54 | 71 px | 906 |
| | greedy, star labels first, keeping what it cannot place | 159 | 28 | 0 | 18 | 41 | 116 px | 453 |
| | greedy, star labels first, least bad when nothing is free | 128 | 7 | 0 | 18 | 41 | 116 px | 452 |
| `crux-90` | the atlas today | 74 | 17 | — | — | — | — | — |
| | greedy, star labels first | 75 | 0 | 5 | 6 | 25 | 92 px | 216 |
| | greedy, constellation names first | 75 | 0 | 4 | 6 | 25 | 92 px | 210 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 32 | 5 | 33 | 73 px | 587 |
| | greedy, star labels first, keeping what it cannot place | 87 | 4 | 0 | 6 | 25 | 92 px | 216 |
| | greedy, star labels first, least bad when nothing is free | 86 | 3 | 0 | 6 | 25 | 92 px | 216 |

"Of the two defects" counts only the owner's two: a constellation name and a
star label sharing pixels either way up, and a star's name across another
star's disc. The wider count includes every hairline a label crosses.

**Lost** is the measurement that decides this. It is not "labels the policy
refused" - the atlas refuses labels today too, silently, and Namalsadirah's
letter is one of them. It is the text the released page draws that the candidate
page does not, counted from the two painted pages. **Gained** is the other
direction: text the candidate draws that the atlas does not.

The wider collision count goes **up** for the last two policies on the widest pages,
and that is worth looking at rather than hiding: a label moved off a star's mark has
to go somewhere, and on a 120-degree page what is everywhere else is the graticule
and the figures. `orion-120` trades 37 of the owner's defects for 2, and pays for it
with label-over-hairline crossings. That is the right trade and the gate should say
so explicitly: a name across a star hides a fact, a name across a grid line hides a
grid line, and the two are not commensurable. A policy that chased the total would
choose the third, which reaches nearly zero by refusing to draw a third of the
text.

The first three policies buy their repair with omissions, and on the widest pages
the price is a third of the text a reader has today. The last two pay nothing: a
label with no free candidate is still drawn, so no page loses anything it has.
They differ in where it goes - back to its own anchor, which is where the released
page puts it, or to whichever of its eight candidates covers the least ink.

### May a constellation name move, and how far?

A name that leaves its own figure is naming the wrong part of the sky, which is
worse than the collision it was avoiding. The chosen policy therefore refuses any
candidate outside the region its constellation owns - the **convex hull of that
figure's visible ink**, not its bounding box: the box of Eridanus, which wanders
half the sky, contains most of Orion.

What that rule costs and what it buys, measured by running the same policy with
it and without it:

| page | | names placed | moved | worst move | off their own figure | centre outside it | collisions |
|---|---|---:|---:|---:|---:|---:|---:|
| `home` | owned | 0 | 0 | 0 px | 0 | 0 | 1 |
|  | free | 0 | 0 | 0 px | 0 | 0 | 1 |
| `orion-36` | owned | 5 | 0 | 0 px | 0 | 2 | 25 |
|  | free | 5 | 0 | 0 px | 2 | 2 | 25 |
| `orion-90` | owned | 18 | 6 | 82 px | 0 | 5 | 58 |
|  | free | 18 | 10 | 82 px | 5 | 8 | 53 |
| `orion-120` | owned | 27 | 15 | 74 px | 0 | 10 | 472 |
|  | free | 27 | 20 | 91 px | 6 | 10 | 465 |
| `sagittarius-120` | owned | 30 | 14 | 70 px | 0 | 16 | 128 |
|  | free | 30 | 19 | 92 px | 8 | 16 | 111 |
| `crux-90` | owned | 27 | 12 | 50 px | 0 | 13 | 86 |
|  | free | 27 | 17 | 50 px | 5 | 15 | 86 |

The rule the policy enforces is that a name's box **overlaps** the region its own
figure owns. Overlap rather than "its centre is inside", because a figure can be
smaller than its own name: Crater on a 90-degree page leaves six pixels by five of
visible ink and CRATER is fifty pixels wide, so the strict reading would refuse
every candidate it has and teach nothing. The stricter statistic is reported
beside it so the difference is visible rather than argued about.

Without the rule, names leave their own figures on every crowded page - eight of
thirty at Sagittarius, seven of twenty-seven at 120 degrees. With it, none does
anywhere, and the collision count is a little higher. That is the whole trade: the
rule costs a name the occasional candidate and buys the guarantee that a name is
written across the thing it names.

## A word cut short is another word

The atlas draws a label whose box runs off the paper and lets the page cut it.
The rule for constellation names says so in as many words - *honest position over
pretty placement* - and it treats clipping as a matter of tidiness.

It is not. Of the constellations the bundled pack draws, these become a **different
constellation** when the page cuts their name:

```
LEO MINOR              cut short reads   LEO
SAGITTARIUS            cut short reads   SAGITTA
TRIANGULUM AUSTRALE    cut short reads   TRIANGULUM
```

And of the 12635 deep-sky labels these pages carry, **35057 truncations are another
object's own label** - every `IC 1203` cut to `IC 1`, every `NGC 2024` cut to
`NGC 202`. A clipped label is not an untidy page. It is a page that names the
wrong thing, at the edge, where a reader matching a chart against the sky is
most likely to be working.

How often the atlas does it today, counted over the corpus - every family, because
the decision is every family's:

| page | star names | deep-sky labels | constellation names |
|---|---:|---:|---:|
| `home` | 0 | 1 | 0 |
| `orion-08` | 1 | 0 | 0 |
| `orion-18` | 0 | 10 | 0 |
| `orion-36` | 0 | 1 | 1 |
| `orion-42` | 1 | 1 | 1 |
| `orion-60` | 0 | 2 | 1 |
| `orion-90` | 0 | 2 | 3 |
| `orion-120` | 1 | 1 | 2 |
| `sagittarius-90` | 2 | 0 | 1 |
| `sagittarius-120` | 1 | 0 | 0 |
| `cygnus-90` | 1 | 1 | 1 |
| `crux-90` | 2 | 0 | 5 |
| `pole-120` | 2 | 7 | 2 |
| `seam-120` | 2 | 3 | 3 |
| `orion-120-small` | 3 | 1 | 2 |
| `orion-42-a4` | 0 | 1 | 2 |
| `orion-42-letter` | 0 | 1 | 2 |
| `orion-90-black` | 0 | 2 | 3 |
| `sagittarius-90-key` | 2 | 0 | 1 |
| `sagittarius-90-ecliptic` | 2 | 0 | 1 |
| `orion-90-observer` | 0 | 2 | 3 |
| `nunki-searched` | 1 | 0 | 1 |
| `orion-18-selected` | 0 | 10 | 0 |
| **all 23 pages** | **21** | **46** | **35** |

So the decision is that no text is clipped by the page, in any family: a label
that cannot be drawn whole is not drawn, and the placement records which candidates
the paper refused. The mark is still there, unnamed - which is what the page does to
every star below its limit, without apology.

**That table is not the cost of the change.** It counts what the atlas clips today,
which is what the decision is about; what a reader would lose is a different
question, because a label whose usual place runs off the paper has seven other
places to try before it is given up on. The two are counted apart, and the
second is in the next section.

## The seam against this study

Issue #313 builds the placement seam production will use. It is a second
implementation of the decision this document settles, written against the same
words and sharing no code with the greedy pass above - so the two can be asked
the same question, which is worth more than asking either of them twice.

| page | labels both place | same candidate | same box | placed under duress | omitted by the seam |
|---|---:|---:|---:|---:|---:|
| `home` | 4 | 4 | 1 | 2 | 0 |
| `orion-36` | 16 | 16 | 10 | 6 | 2 |
| `orion-90` | 64 | 59 | 45 | 8 | 2 |
| `orion-120` | 98 | 87 | 67 | 12 | 3 |
| `sagittarius-120` | 118 | 107 | 86 | 14 | 1 |
| `crux-90` | 78 | 75 | 53 | 4 | 2 |

They do not agree everywhere, and the places they part are worth more than the
places they meet. Two causes, and neither is the placement rule:

**The candidates are built twice.** Both build eight boxes around an anchor from
the same sentence, and a deep-sky label's reach differs between them by the gap
itself - three pixels. That is why "same candidate" is high and "same box" is
lower: they choose the same position and draw it a few pixels apart.

**And a difference cascades.** Placement is sequential: a label three pixels from
where the other pass put it changes what every later label finds free. One
disagreement early on a crowded page is worth several late ones.

### What a reader would actually lose

The cost of the clipping decision, counted as the thing it is: text the atlas
draws today that the seam would not draw at all. Not the same as the clipping
count above - a label whose usual place runs off the paper has seven other places
to try - and not the same as the seam's omissions either, which include text
this study's own pass never asked for.

| page | drawn today | not drawn by the seam | which are |
|---|---:|---:|---|
| `home` | 5 | 0 | — |
| `orion-36` | 18 | 2 | 1 dso label, 1 const. name |
| `orion-90` | 66 | 2 | 2 const. name |
| `orion-120` | 101 | 3 | 1 dso label, 2 const. name |
| `sagittarius-120` | 119 | 1 | 1 const. name |
| `crux-90` | 80 | 2 | 2 const. name |

Every one of those is at the paper's edge or outside its own figure's region:
those are the two refusals a fallback may not spend, and the only two that can
omit anything. #314 applies the rule and the released pages change by these
counts.

**And the seam omits where this pass does not.** The paper's edge is not a cost the
fallback may spend, so a label whose every candidate leaves the page is not drawn
at all. This study's pass draws those clipped, which is what the atlas does today;
the section above counts them, and the decision says why the seam is right to
refuse.

What this does establish is the part worth establishing: on the same page, from
the same published geometry, two implementations written from one document and
sharing no code choose the same candidate for the great majority of a page's text.
The candidate arithmetic is #314's to make one of, when the families migrate to the
seam and this pass retires.

## Stability under a small navigation change

A label that jumps to the other side of its star when the reader nudges the page
is worse than one that sits still in a slightly worse place. Each page below is
placed, then placed again one pan step east (a twentieth of the field) and one
rung deeper on the zoom ladder, and the labels that changed candidate are
counted.

| page | labels | changed after a pan | after a zoom |
|---|---:|---:|---:|
| `orion-90` | 107 | 25 | 19 |
| `sagittarius-120` | 124 | 15 | 20 |
| `home` | 4 | 0 | 0 |

Counted as: a label that both pages draw, whose offset from its own anchor changed
sign in x or in y - it moved to the other side of the thing it names.

## Screen and paper

The sheet writers replay one recording of one production render (#285, #298), so
SVG, PDF and PNG carry whatever the renderer decided and no placement question
is answered twice. What differs between screen and paper is the extent, and the
extent changes placement: `orion-42` at 900×700, at the A4 chart area and at
the Letter chart area are three different pages of the same sky, measured
above.

## The pages themselves

- `sagittarius-120-today.png`
- `sagittarius-120-candidate.png`
- `orion-90-today.png`
- `orion-90-candidate.png`
- `home-today.png`
- `home-candidate.png`

The two `sagittarius-120` pages are the fixture's own: the released page with
Nunki's name across Namalsadirah, and the same page with the candidate policy
placing it. `home` is the control - the page a reader opens the atlas on, which
must not get worse.


## What this cost to measure

23 pages and 17567 painted renders for the census alone. Every collision in this
document is one of those renders differenced against another, which is what a
collision nobody can dispute costs.

No wall-clock time is recorded here: a millisecond is a fact about a machine
rather than about the atlas, and this file has to reproduce itself byte for byte.
The placement policies' cost is reported above as candidates examined, and their
measured runtime is in `docs/decisions/label-placement.md`, which says which machine
it was taken on.
