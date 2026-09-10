# How labels share an atlas page

What this file is: the measurement behind issue #310, Sprint 31's cartography and
architecture gate. It inventories what the atlas does with text, measures where
that goes wrong, and compares the candidate policies the gate chose between.

**It has been measured twice.** The gate measured an atlas in which each family
placed its own labels and avoided what it happened to avoid. Issue #314 moved
the three families onto one decision, and this file now measures that atlas -
with the earlier numbers kept beside the new ones wherever they are the point,
from two records taken before the change: `census-before.tsv` and `text-before.tsv`.
What the migration did to the atlas, page by page and label by label, is its own
section below.

> **Platform observation.** The measurements below are this machine's:
> ink counts, font extents and encoded sizes depend on the fonts and the
> rasteriser in front of them, and another machine measures differently
> without anything being wrong. Held to reproducing here, never to matching
> another machine's recording. What each label is, where the policy allows it to go, which candidate it took and why a refusal was refused does not depend on a font, and is asserted by this study's gate.
>
> Recorded on: `Mac OS X 26.5.2/aarch64/Homebrew 21.0.11`

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

## The truth this began from, family by family

How the families worked before #314 - each with its own answer to every
question, which is what the gate was called to settle. The `may move` and
`omission` columns are the ones #314 changed: every family may now move, and
omission is one rule for all of them.

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
| const. name | 159 | 73 | 200 | — | 2 | 6 | 62 | 9 | · | · | 6 | · |
| star label | 223 | 161 | 242 | 9 | 6 | 15 | 32 | — | · | · | 22 | · |
| dso label | 94 | 17 | 6 | · | 15 | 5 | 7 | · | — | · | · | · |

A dot is no collision anywhere in the corpus. Since #314 one decision places
all three families, so no two pieces of text share a box: they never meet each
other, and they do not meet a mark or a symbol either unless nothing was free.
What is left in the table is what the decision permits - text across a line -
and the fallback, which is what a label does when every one of its positions is
refused.

**The title-block column is that fallback, and it is worth reading twice.** The
paper's edge and a figure's own region are costs the fallback may not spend;
furniture is. So a label with nowhere free can end up under a block that is
opaque and drawn last, where the reader does not see it - which is exactly what
the old star-label pass did with it, by refusing to draw it at all. Same page for
the reader, and it is counted here rather than left out.

Two families still cannot be separated by this study, though #313 published
their decisions. The equatorial grid draws its lines and its edge notation under
one reader switch, and the reference layer its curves and their names under another,
so neither can be *withheld* apart from the other and this document reports each as
one participant. Placement can tell them apart: grid notation is an obstacle to
the sky's text, and the reference layer's names are not (docs/decisions/place-and-time.md).

## What the atlas draws today

Two numbers where the atlas has one, because Sprint 31 changed it: **before** is
the same measurement of the same page taken from the atlas as 1.11.0 shipped
it, when each family placed its own labels (`census-before.tsv` beside this file).

*Text drawn* is text a reader can see: a piece whose removal changes a pixel.
A label behind the title block is not in it, and neither is one whose glyphs fall
entirely on ink of their own colour. The count of text the page *places* is in
the migration section, and the two are different questions.

| page | text drawn | before | collisions | before | pixels | before | worst single | order check |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `home` | 2 | 3 | 0 | 2 | 0 | 146 | 0 | 0.00 |
| `orion-08` | 12 | 12 | 6 | 8 | 55 | 88 | 13 | 1.00 |
| `orion-18` | 15 | 19 | 10 | 23 | 117 | 261 | 20 | 1.00 |
| `orion-36` | 17 | 17 | 23 | 32 | 235 | 348 | 25 | 0.98 |
| `orion-42` | 24 | 24 | 31 | 69 | 323 | 685 | 29 | 1.20 |
| `orion-60` | 41 | 39 | 32 | 51 | 467 | 913 | 55 | 0.96 |
| `orion-90` | 100 | 96 | 54 | 63 | 830 | 1371 | 62 | 1.00 |
| `orion-120` | 175 | 174 | 129 | 169 | 2229 | 3438 | 70 | 1.00 |
| `sagittarius-90` | 97 | 88 | 85 | 84 | 1005 | 1091 | 101 | 0.94 |
| `sagittarius-120` | 122 | 106 | 126 | 147 | 2070 | 2516 | 97 | 1.06 |
| `cygnus-90` | 57 | 54 | 55 | 69 | 685 | 1315 | 73 | 1.00 |
| `crux-90` | 78 | 74 | 79 | 74 | 1082 | 1319 | 61 | 1.00 |
| `pole-120` | 119 | 95 | 88 | 114 | 1232 | 2545 | 93 | 1.00 |
| `seam-120` | 145 | 101 | 77 | 91 | 1320 | 1442 | 94 | 0.95 |
| `orion-120-small` | 197 | 195 | 105 | 263 | 1761 | 3414 | 65 | 1.12 |
| `orion-42-a4` | 21 | 21 | 26 | 63 | 251 | 610 | 29 | 1.00 |
| `orion-42-letter` | 21 | 24 | 37 | 84 | 293 | 821 | 19 | 1.00 |
| `orion-90-black` | 100 | 96 | 54 | 63 | 830 | 1365 | 62 | 1.00 |
| `sagittarius-90-key` | 96 | 87 | 85 | 84 | 1005 | 1091 | 101 | 0.94 |
| `sagittarius-90-ecliptic` | 97 | 88 | 103 | 102 | 1615 | 1701 | 101 | 1.06 |
| `orion-90-observer` | 100 | 96 | 59 | 68 | 892 | 1440 | 62 | 1.06 |
| `nunki-searched` | 94 | 83 | 88 | 81 | 1173 | 1100 | 68 | 0.94 |
| `orion-18-selected` | 15 | 19 | 10 | 23 | 117 | 261 | 20 | 1.00 |
| **all 23 pages** | | | **1362** | **1827** | **19587** | **29281** | | **1.20** |

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
| star label over figure | 242 | 2727 |
| star label over grid | 223 | 3196 |
| const. name over figure | 200 | 3626 |
| star label over boundary | 161 | 1646 |
| const. name over grid | 159 | 3227 |
| dso label over grid | 94 | 1133 |
| const. name over boundary | 73 | 1054 |
| star disc over const. name | 62 | 641 |
| star label over star disc | 32 | 264 |
| title over star label | 22 | 773 |
| dso label over boundary | 17 | 121 |
| dso label over reference | 15 | 526 |
| star label over dso symbol | 15 | 125 |
| star label over const. name | 9 | 118 |
| dso label over star disc | 7 | 51 |
| dso label over figure | 6 | 22 |
| dso symbol over const. name | 6 | 72 |
| star label over reference | 6 | 61 |
| title over const. name | 6 | 62 |
| dso label over dso symbol | 5 | 57 |
| reference over const. name | 2 | 85 |

## The two defects the owner saw

These are the gate's first priority, and they are counted apart from everything
else rather than folded into a total that a hairline crossing could dominate.

| page | const. name ↔ star label | before | star label over an unrelated disc | before | its own disc |
|---|---:|---:|---:|---:|---:|
| `home` | 0 | 0 | 0 | 1 | excluded |
| `orion-08` | 0 | 0 | 0 | 2 | excluded |
| `orion-18` | 0 | 0 | 0 | 7 | excluded |
| `orion-36` | 0 | 0 | 5 | 13 | excluded |
| `orion-42` | 0 | 0 | 4 | 27 | excluded |
| `orion-60` | 1 | 4 | 0 | 9 | excluded |
| `orion-90` | 0 | 4 | 0 | 7 | excluded |
| `orion-120` | 1 | 5 | 0 | 18 | excluded |
| `sagittarius-90` | 1 | 2 | 0 | 5 | excluded |
| `sagittarius-120` | 2 | 9 | 0 | 14 | excluded |
| `cygnus-90` | 0 | 5 | 0 | 2 | excluded |
| `crux-90` | 0 | 2 | 0 | 7 | excluded |
| `pole-120` | 0 | 3 | 0 | 11 | excluded |
| `seam-120` | 0 | 2 | 0 | 3 | excluded |
| `orion-120-small` | 0 | 5 | 2 | 23 | excluded |
| `orion-42-a4` | 0 | 0 | 8 | 31 | excluded |
| `orion-42-letter` | 0 | 0 | 12 | 37 | excluded |
| `orion-90-black` | 0 | 4 | 0 | 7 | excluded |
| `sagittarius-90-key` | 1 | 2 | 0 | 5 | excluded |
| `sagittarius-90-ecliptic` | 1 | 2 | 0 | 5 | excluded |
| `orion-90-observer` | 0 | 4 | 0 | 7 | excluded |
| `nunki-searched` | 2 | 3 | 1 | 4 | excluded |
| `orion-18-selected` | 0 | 0 | 0 | 7 | excluded |
| **all pages** | **9** | **56** | **32** | **252** | |

The last column says what is deliberately not counted: a star's own disc, which its
name is anchored beside by decision. Counting it would bury the defect the owner
reported - a name across somebody *else's* star - under one entry per label on
the page.

A constellation name is also covered by star **discs**, which is the same defect
with the layers the other way up: the name is drawn during the geography pass
and every mark on the page is drawn after it. Over the corpus that pair went from
228 to 62 when the families migrated, and what is left of it is the
fallback: a name whose figure leaves it nowhere free takes the least bad of its own
candidates rather than leaving the constellation unnamed.

## The named fixture: Nunki against Namalsadirah

| star | catalogue | chart identity | V | drawn radius |
|---|---|---|---:|---:|
| Nunki | `TYC 6868-1829-1` | σ Sgr · Flamsteed 34 | 2.06 | 4.38 px |
| Namalsadirah | `TYC 6867-2428-1` | φ Sgr · Flamsteed 27 | 3.13 | 3.94 px |

On the production `sagittarius-120` page, the four pieces the fixture asks to be
kept apart, each one measured by withholding it and painting the page again:

| piece | withheld by | ink | meets Nunki's label |
|---|---|---:|---|
| Nunki's label glyphs | its star's identity set aside, its mark kept | 222 px | — |
| Nunki's own disc | its star removed from the scene | 76 px | no |
| Namalsadirah's disc | its star removed from the scene | 61 px | no |
| Namalsadirah's label glyphs | its star's identity set aside | 52 px | no |

Both rows read *no* now, and one of them is the whole of Sprint 31. Before the
families migrated, Namalsadirah's row read **yes, 44 px**: a magnitude 3.1 mark,
drawn at nearly four pixels of radius, with a smaller star's name written across
it - more of the mark covered than the mark had left visible. The two are
separated here by two different surgeries on the same scene, so neither can be
answering for the other, and the same measurement that found the defect is the
one reporting it gone.

**The fourth participant was missing, and its absence was the same defect.**
Namalsadirah qualifies for a Bayer letter at this field - V 3.13 against a limit of
V 3.5, and the policy returns "φ" for it - but the page did not draw one. The old star-
label pass took stars brightest first, accepted "Nunki σ", and then refused φ
because its box met the accepted one: the atlas silently declined to name a star
in order to protect a name it then drew across that same star's mark. Its row in
the table above has ink in it now, which is the letter being drawn.

The fixture reproduced on the page a reader reaches, not only on one contrived
centre - which is why the repair is asked for at every one of them. Nunki's label
box against Namalsadirah's drawn disc:

| centre | field | 900×700 | 1100×800 |
|---|---:|---|---|
| Sagittarius | 42° | clear | clear |
| Sagittarius | 60° | clear | clear |
| Sagittarius | 90° | clear | clear |
| Sagittarius | 120° | clear | clear |
| Nunki itself | 42° | clear | clear |
| Nunki itself | 60° | clear | clear |
| Nunki itself | 90° | clear | clear |
| Nunki itself | 120° | clear | clear |
| the Milky Way | 42° | clear | clear |
| the Milky Way | 60° | clear | clear |
| the Milky Way | 90° | clear | clear |
| the Milky Way | 120° | clear | clear |

## The candidate policies

The gate's own comparison, kept because it is what the decision was taken from -
and because since #314 the row marked *the atlas itself* is the chosen policy in
production, so the table reads as one implementation against another.

Three variants of one deterministic greedy pass, differing in exactly the
questions the gate has to answer. Each takes the labels in a fixed priority, gives
each a fixed ordered list of candidate positions around its own anchor - east
first, which is where every label sat before Sprint 31 - and takes the first that no
accepted box, no drawn mark, no furniture and no page edge refuses. A label with
no free candidate is omitted rather than drawn over something.

Their pages are painted the same way the released page is: production's chart
with the text families switched off through the reader's own options, and the
candidate's text written on top in the renderer's own fonts and inks. They are
then put through the same collision oracle, so nothing below is a candidate
marking its own work.

| page | policy | collisions | of the two defects | lost | gained | moved | worst move | candidates tried |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| `home` | the atlas itself | 0 | 0 | — | — | — | — | — |
| | greedy, star labels first | 0 | 0 | 0 | 0 | 1 | 22 px | 11 |
| | greedy, constellation names first | 0 | 0 | 0 | 0 | 1 | 22 px | 11 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 0 | 0 | 1 | 22 px | 11 |
| | greedy, star labels first, keeping what it cannot place | 1 | 0 | 0 | 1 | 1 | 22 px | 11 |
| | greedy, star labels first, least bad when nothing is free | 1 | 0 | 0 | 1 | 1 | 22 px | 11 |
| `orion-36` | the atlas itself | 23 | 9 | — | — | — | — | — |
| | greedy, star labels first | 9 | 0 | 7 | 0 | 7 | 47 px | 126 |
| | greedy, constellation names first | 9 | 0 | 7 | 0 | 7 | 47 px | 126 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 13 | 0 | 3 | 33 px | 177 |
| | greedy, star labels first, keeping what it cannot place | 30 | 14 | 0 | 0 | 7 | 47 px | 126 |
| | greedy, star labels first, least bad when nothing is free | 22 | 6 | 0 | 0 | 7 | 47 px | 126 |
| `orion-90` | the atlas itself | 54 | 0 | — | — | — | — | — |
| | greedy, star labels first | 45 | 0 | 39 | 0 | 28 | 87 px | 522 |
| | greedy, constellation names first | 46 | 0 | 34 | 2 | 32 | 87 px | 512 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 63 | 0 | 29 | 90 px | 781 |
| | greedy, star labels first, keeping what it cannot place | 195 | 3 | 0 | 3 | 29 | 87 px | 540 |
| | greedy, star labels first, least bad when nothing is free | 62 | 1 | 0 | 2 | 28 | 87 px | 540 |
| `orion-120` | the atlas itself | 129 | 1 | — | — | — | — | — |
| | greedy, star labels first | 84 | 0 | 73 | 0 | 55 | 88 px | 967 |
| | greedy, constellation names first | 84 | 0 | 75 | 24 | 52 | 88 px | 943 |
| | greedy, star labels first, avoiding every line | 2 | 0 | 114 | 0 | 51 | 89 px | 1385 |
| | greedy, star labels first, keeping what it cannot place | 117 | 10 | 0 | 5 | 56 | 88 px | 982 |
| | greedy, star labels first, least bad when nothing is free | 429 | 2 | 0 | 5 | 48 | 88 px | 1011 |
| `sagittarius-120` | the atlas itself | 126 | 12 | — | — | — | — | — |
| | greedy, star labels first | 83 | 0 | 14 | 0 | 41 | 116 px | 424 |
| | greedy, constellation names first | 86 | 0 | 11 | 0 | 46 | 90 px | 421 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 53 | 0 | 57 | 71 px | 893 |
| | greedy, star labels first, keeping what it cannot place | 149 | 21 | 0 | 2 | 42 | 116 px | 435 |
| | greedy, star labels first, least bad when nothing is free | 133 | 13 | 0 | 2 | 42 | 116 px | 434 |
| `crux-90` | the atlas itself | 79 | 1 | — | — | — | — | — |
| | greedy, star labels first | 71 | 0 | 4 | 0 | 24 | 92 px | 251 |
| | greedy, constellation names first | 70 | 0 | 3 | 0 | 26 | 92 px | 230 |
| | greedy, star labels first, avoiding every line | 0 | 0 | 30 | 0 | 34 | 73 px | 570 |
| | greedy, star labels first, keeping what it cannot place | 83 | 2 | 0 | 2 | 24 | 92 px | 251 |
| | greedy, star labels first, least bad when nothing is free | 83 | 1 | 0 | 2 | 24 | 92 px | 251 |

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
| `orion-36` | owned | 5 | 2 | 18 px | 0 | 1 | 22 |
|  | free | 5 | 2 | 18 px | 1 | 2 | 26 |
| `orion-90` | owned | 18 | 8 | 82 px | 0 | 8 | 62 |
|  | free | 18 | 12 | 82 px | 5 | 9 | 57 |
| `orion-120` | owned | 27 | 14 | 82 px | 0 | 12 | 429 |
|  | free | 27 | 16 | 82 px | 6 | 13 | 426 |
| `sagittarius-120` | owned | 30 | 15 | 73 px | 0 | 15 | 133 |
|  | free | 30 | 18 | 92 px | 10 | 17 | 116 |
| `crux-90` | owned | 27 | 11 | 57 px | 0 | 16 | 83 |
|  | free | 27 | 15 | 62 px | 6 | 17 | 81 |

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

| page | star names | deep-sky labels | constellation names | as drawn now |
|---|---:|---:|---:|---:|
| `orion-08` | 1 | 2 | 0 | 0 |
| `orion-36` | 0 | 1 | 1 | 0 |
| `orion-42` | 1 | 1 | 1 | 0 |
| `orion-60` | 0 | 2 | 1 | 0 |
| `orion-90` | 0 | 0 | 3 | 0 |
| `orion-120` | 1 | 1 | 2 | 0 |
| `sagittarius-90` | 2 | 0 | 1 | 0 |
| `sagittarius-120` | 1 | 0 | 2 | 0 |
| `cygnus-90` | 1 | 1 | 1 | 0 |
| `crux-90` | 2 | 0 | 5 | 0 |
| `pole-120` | 3 | 3 | 2 | 0 |
| `seam-120` | 2 | 0 | 4 | 0 |
| `orion-120-small` | 5 | 0 | 2 | 0 |
| `orion-42-a4` | 0 | 1 | 2 | 0 |
| `orion-42-letter` | 0 | 1 | 2 | 0 |
| `orion-90-black` | 0 | 0 | 3 | 0 |
| `sagittarius-90-key` | 2 | 0 | 1 | 0 |
| `sagittarius-90-ecliptic` | 2 | 0 | 1 | 0 |
| `orion-90-observer` | 0 | 0 | 3 | 0 |
| `nunki-searched` | 1 | 0 | 2 | 0 |
| **all 23 pages** | **24** | **13** | **39** | **0** |

The first three columns are the labels whose **usual place** - the first of their
stated candidates, where each family drew before Sprint 31 - runs off the
paper. The last is how many the atlas draws clipped, which is the decision: **no
text is clipped by the page, in any family**. A label that cannot be drawn whole
is drawn somewhere else, and one with nowhere else is not drawn at all, with the
placement recording which candidates the paper refused. The mark is still there,
unnamed - which is what the page does to every star below its limit, without
apology.

**The first three columns are not the cost of the change.** They count what would
be clipped, which is what the decision is about; what a reader actually loses is
a different question, because a label whose usual place runs off the paper has
seven other places to try before it is given up on. The two are counted apart,
and the second is measured against the atlas as it was, further down.

## What the migration changed

Every page of the corpus, against the same page drawn by the atlas as 1.11.0
shipped it - each family placing its own labels, avoiding what it happened to
avoid. That page's text was recorded label by label before the change (`text-before.tsv`
beside this file) and is read back here, so a label that moved is a move rather than
a loss and a gain.

| page | drawn before | drawn now | kept its place | moved | worst move | no longer drawn | newly drawn |
|---|---:|---:|---:|---:|---:|---:|---:|
| `home` | 4 | 5 | 2 | 2 | 48 px | 0 | 1 |
| `orion-08` | 12 | 11 | 6 | 4 | 25 px | 2 | 1 |
| `orion-18` | 16 | 16 | 8 | 8 | 86 px | 0 | 0 |
| `orion-36` | 18 | 16 | 4 | 12 | 66 px | 2 | 0 |
| `orion-42` | 22 | 22 | 5 | 16 | 65 px | 1 | 1 |
| `orion-60` | 33 | 35 | 22 | 9 | 83 px | 2 | 4 |
| `orion-90` | 56 | 64 | 31 | 23 | 84 px | 2 | 10 |
| `orion-120` | 87 | 98 | 45 | 39 | 83 px | 3 | 14 |
| `sagittarius-90` | 73 | 81 | 50 | 22 | 100 px | 1 | 9 |
| `sagittarius-120` | 101 | 118 | 59 | 41 | 113 px | 1 | 18 |
| `cygnus-90` | 52 | 54 | 32 | 19 | 80 px | 1 | 3 |
| `crux-90` | 75 | 78 | 47 | 26 | 89 px | 2 | 5 |
| `pole-120` | 86 | 91 | 50 | 33 | 76 px | 3 | 8 |
| `seam-120` | 66 | 68 | 41 | 24 | 53 px | 1 | 3 |
| `orion-120-small` | 72 | 89 | 28 | 41 | 83 px | 3 | 20 |
| `orion-42-a4` | 18 | 18 | 3 | 14 | 85 px | 1 | 1 |
| `orion-42-letter` | 19 | 18 | 2 | 15 | 85 px | 2 | 1 |
| `orion-90-black` | 56 | 64 | 31 | 23 | 84 px | 2 | 10 |
| `sagittarius-90-key` | 72 | 81 | 49 | 22 | 100 px | 1 | 10 |
| `sagittarius-90-ecliptic` | 73 | 81 | 50 | 22 | 100 px | 1 | 9 |
| `orion-90-observer` | 56 | 64 | 31 | 23 | 84 px | 2 | 10 |
| `nunki-searched` | 69 | 79 | 43 | 26 | 100 px | 0 | 10 |
| `orion-18-selected` | 16 | 16 | 8 | 8 | 86 px | 0 | 0 |
| **all pages** | **1152** | **1267** | **647** | **472** | **113 px** | **33** | **148** |

The atlas draws **115 more** pieces of text than it did, and the two directions
are different things. What it stopped drawing is the clipping decision being
paid: a label whose every candidate leaves the paper is not drawn. What it
started drawing is the collision policy being repaid: a star's name that used to
be dropped because its one box was taken now has seven other places to try.

The text the atlas no longer draws, in full, because a list is the only honest
form for it:

```
orion-08: DEEP_SKY_LABEL:NGC 1976
orion-08: DEEP_SKY_LABEL:NGC 1982
orion-36: DEEP_SKY_LABEL:NGC 2168
orion-36: CONSTELLATION_NAME:Gem
orion-42: DEEP_SKY_LABEL:NGC 2168
orion-60: DEEP_SKY_LABEL:Mel022
orion-60: DEEP_SKY_LABEL:NGC 2548
orion-90: CONSTELLATION_NAME:Cae
orion-90: CONSTELLATION_NAME:Pyx
orion-120: DEEP_SKY_LABEL:NGC 224
orion-120: CONSTELLATION_NAME:And
orion-120: CONSTELLATION_NAME:LMi
sagittarius-90: CONSTELLATION_NAME:Cir
sagittarius-120: CONSTELLATION_NAME:Tuc
cygnus-90: DEEP_SKY_LABEL:NGC 598
crux-90: CONSTELLATION_NAME:Crt
crux-90: CONSTELLATION_NAME:Crv
pole-120: DEEP_SKY_LABEL:NGC 598
pole-120: CONSTELLATION_NAME:CVn
pole-120: CONSTELLATION_NAME:Tri
seam-120: CONSTELLATION_NAME:Sge
orion-120-small: CONSTELLATION_NAME:Car
orion-120-small: CONSTELLATION_NAME:Hor
orion-120-small: CONSTELLATION_NAME:Lyn
orion-42-a4: DEEP_SKY_LABEL:NGC 2168
orion-42-letter: DEEP_SKY_LABEL:NGC 2168
orion-42-letter: CONSTELLATION_NAME:Tau
orion-90-black: CONSTELLATION_NAME:Cae
orion-90-black: CONSTELLATION_NAME:Pyx
sagittarius-90-key: CONSTELLATION_NAME:Cir
sagittarius-90-ecliptic: CONSTELLATION_NAME:Cir
orion-90-observer: CONSTELLATION_NAME:Cae
orion-90-observer: CONSTELLATION_NAME:Pyx
```

## The seam against this study

Issue #313 builds the placement seam production will use. It is a second
implementation of the decision this document settles, written against the same
words and sharing no code with the greedy pass above - so the two can be asked
the same question, which is worth more than asking either of them twice.

| page | labels both place | same candidate | same box | placed under duress | omitted by the seam |
|---|---:|---:|---:|---:|---:|
| `home` | 3 | 3 | 1 | 2 | 0 |
| `orion-36` | 16 | 16 | 10 | 6 | 2 |
| `orion-90` | 64 | 61 | 45 | 8 | 2 |
| `orion-120` | 97 | 88 | 67 | 12 | 3 |
| `sagittarius-120` | 118 | 113 | 86 | 14 | 1 |
| `crux-90` | 78 | 76 | 53 | 4 | 2 |

They do not agree everywhere, and the places they part are worth more than the
places they meet. Two causes, and neither is the placement rule:

**The candidates are built twice.** Both build eight boxes around an anchor from
the same sentence, and a deep-sky label's reach differs between them by the gap
itself - three pixels. That is why "same candidate" is high and "same box" is
lower: they choose the same position and draw it a few pixels apart.

**And a difference cascades.** Placement is sequential: a label three pixels from
where the other pass put it changes what every later label finds free. One
disagreement early on a crowded page is worth several late ones.

What this establishes is the part worth establishing: on the same page, from the
same published geometry, two implementations written from one document and
sharing no code choose the same candidate for the great majority of a page's text.

## Stability under a small navigation change

A label that jumps to the other side of its star when the reader nudges the page
is worse than one that sits still in a slightly worse place. The gate gave that a
budget - **no more than 15% of a page's labels displaced by a one-step pan** - and
left the zoom rung without one, because a zoom changes which stars are on the page
at all.

Every page is placed by the atlas, then placed again one pan step east (a
twentieth of the field). Displaced means a label both pages draw whose offset
from its own anchor changed sign in x or in y: it crossed to the other side of
the thing it names, which is the jump a reader notices.

| page | labels | displaced by a pan | share | budget | after a zoom |
|---|---:|---:|---:|---|---:|
| `home` | 5 | 1 | 20% | **over** | 1 |
| `orion-08` | 11 | 2 | 18% | **over** | 1 |
| `orion-18` | 15 | 1 | 7% | met | 2 |
| `orion-36` | 16 | 2 | 13% | met | 8 |
| `orion-42` | 20 | 5 | 25% | **over** | 8 |
| `orion-60` | 35 | 3 | 9% | met | 15 |
| `orion-90` | 63 | 8 | 13% | met | 16 |
| `orion-120` | 96 | 14 | 15% | met | 29 |
| `sagittarius-90` | 79 | 5 | 6% | met | 15 |
| `sagittarius-120` | 116 | 20 | 17% | **over** | 23 |
| `cygnus-90` | 52 | 5 | 10% | met | 8 |
| `crux-90` | 76 | 12 | 16% | **over** | 19 |
| `pole-120` | 90 | 19 | 21% | **over** | 12 |
| `seam-120` | 63 | 12 | 19% | **over** | 8 |
| `orion-120-small` | 86 | 15 | 17% | **over** | 23 |
| `orion-42-a4` | 18 | 3 | 17% | **over** | 7 |
| `orion-42-letter` | 18 | 5 | 28% | **over** | 4 |
| `orion-90-black` | 63 | 8 | 13% | met | 16 |
| `sagittarius-90-key` | 79 | 6 | 8% | met | 16 |
| `sagittarius-90-ecliptic` | 79 | 5 | 6% | met | 15 |
| `orion-90-observer` | 63 | 8 | 13% | met | 16 |
| `nunki-searched` | 75 | 12 | 16% | **over** | 13 |
| `orion-18-selected` | 15 | 1 | 7% | met | 2 |
| **all pages** | **1233** | **172** | **14%** | **met** | |

Over the corpus the budget is met. On eleven pages it is not, and the two facts
are not in tension: a page of eighteen labels moves five of them and reads as 28%,
while the pages carrying most of the atlas's text sit between 6% and 21%.

**What moves them.** For every displaced label, what refused - on the panned page -
the position it held on the first one:

| what refused the position it had | labels |
|---|---:|
| a star's mark or a symbol | 45 |
| another label, itself displaced or newly there | 22 |
| nothing - it took an earlier candidate the pan had freed | 65 |
| the paper's edge | 20 |
| the title block or the key | 20 |
| **all** | **172** |

The largest group is not a collision at all: those labels moved to an **earlier**
candidate that the pan had freed - a label going back to the side it prefers, as
soon as it can. The next is the cascade: a label yields to a label that has itself
moved. Neither can be removed from a first-free pass that is not allowed to
remember where a label was, and the decision is not allowed to remember: a page
that depended on how the reader arrived at it would export differently for two
readers looking at the same sky.

**Two geometry-only remedies were measured and neither is one.** Ignoring contact
below a pixel of shared ink - the smallest mark the atlas can lay down, so the bound
comes from the ink rather than from the number it would produce - removes exactly
one displacement of the twenty at `sagittarius-120`: the refusals that move labels
there share 6, 31, 34, 126, 154 and 168 square pixels, and one shares 0.30.

And the eight positions can be tried in a different order. The stated one is east,
west, then the diagonals, which sends a refused label straight across the thing
it names; trying the neighbours first - east, north-east, south-east, north,
south, north-west, south-west, west - keeps it on the same side where it can. Both
orders, placed by the same pass and counted by the same rule as the table above:

| candidate order | displaced by a pan | of |
|---|---:|---:|
| the gate's: east, west, then the diagonals | 172 | 1233 |
| neighbours first | 179 | 1233 |

Better on 3 pages, worse on 10, and no improvement over the corpus. The stated
order stands.

So the budget is amended rather than met, with the measurement above as its
grounds (docs/decisions/label-placement.md, *Stability, amended*).

Before the migration this table would have been all zeroes and would have said
nothing: every family had exactly one position, so nothing could be displaced by
anything. What happened instead - and did - was that a label disappeared when its
one box was taken.

## What placing a page costs

The gate budgeted the **work**, not the clock: the obstacle comparisons an
indexed pass makes against the product of labels and ink, which is the same number
on every machine. The budget is a tenth.

| page | labels | obstacles | every against every | comparisons made | per label | share | budget |
|---|---:|---:|---:|---:|---:|---:|---|
| `home` | 5 | 56 | 280 | 65 | 13 | 23.2% | **over** |
| `orion-36` | 18 | 1566 | 28188 | 1561 | 86 | 5.5% | met |
| `orion-90` | 66 | 174 | 11484 | 503 | 7 | 4.4% | met |
| `orion-120` | 101 | 249 | 25149 | 823 | 8 | 3.3% | met |
| `sagittarius-120` | 119 | 271 | 32249 | 977 | 8 | 3.0% | met |
| `crux-90` | 80 | 166 | 13280 | 228 | 2 | 1.7% | met |

The index is a uniform grid of 64-pixel cells: a candidate box asks the cells it
covers and nothing else, so a label at the top of the page never hears about a
symbol at the bottom of it.

The share is met where the budget was meant to bite - the wide pages, where the
work could run away - and missed on two pages where the ratio stops meaning much.
`home` has five labels and fifty-six obstacles, so there is nothing for an index to
save; `orion-36` has eighteen labels against a dense star field of sixteen hundred
marks, and its labels sit in the crowded middle of it. The number that governs
whether a page can be placed at all is the last-but-two column, and no page in
the corpus asks more than a few hundred questions per label.

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

23 pages and 16841 painted renders for the census alone. Every collision in this
document is one of those renders differenced against another, which is what a
collision nobody can dispute costs.

No wall-clock time is recorded here: a millisecond is a fact about a machine
rather than about the atlas, and this file has to reproduce itself byte for byte.
The placement policies' cost is reported above as candidates examined, and their
measured runtime is in `docs/decisions/label-placement.md`, which says which machine
it was taken on.
