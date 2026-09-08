# Constellation figures and the overview's magnitude limit

What this file is: the measurement behind issue #307, which was found by
looking at a page rather than by any test. The overview's default magnitude
limits - measured, and right about density - were removing stars that constellation
figure segments are drawn to. The lines stayed and the figure lost a node.

A magnitude limit answers how crowded a page should be. A constellation
figure answers which stars define its shape. The first must not cut holes in
the second.

Every page below is drawn by the production renderer through the production
assembler, at the field's own default limit, 900 x 700 on white paper.

Recorded on: `Mac OS X 26.5.2/aarch64/Homebrew 21.0.11`

## The defect, and the repair

Figure endpoints that land on the paper, and how many of them had no star.

| centre | field | limit | endpoints on page | without a node | under the title block |
|---|---:|---:|---:|---:|---:|
| Orion | 42° | V 8.0 | 41 | 0 | 2 |
| Orion | 60° | V 5.0 | 72 | 0 | 0 |
| Orion | 90° | V 4.0 | 120 | 0 | 6 |
| Orion | 120° | V 4.0 | 189 | 0 | 4 |
| Sagittarius | 42° | V 8.0 | 45 | 0 | 4 |
| Sagittarius | 60° | V 5.0 | 81 | 0 | 1 |
| Sagittarius | 90° | V 4.0 | 133 | 0 | 2 |
| Sagittarius | 120° | V 4.0 | 206 | 0 | 6 |
| M31 | 42° | V 8.0 | 27 | 0 | 1 |
| M31 | 60° | V 5.0 | 64 | 0 | 3 |
| M31 | 90° | V 4.0 | 118 | 0 | 4 |
| M31 | 120° | V 4.0 | 179 | 0 | 3 |

With the exception removed and the page otherwise unchanged, the same column
reads:

```
               42°     60°     90°    120°
Orion            0       0      42      74
Sagittarius      0       4      52      80
M31              0       1      58      84
```

The 42-degree sheet page is clean without it, which is why this was a matter
for the overview: at V 8.0 every figure star is admitted anyway. Sixty degrees
is nearly so and not quite - Orion's page is whole at V 5.0, Sagittarius's is
four endpoints short and M31's one - which is worth knowing, because the rung
that looked safe was safe at one centre and not at the others.

A node here is not ink at the endpoint: it is ink that goes away when the
stars are withheld from the page and nothing else is changed. A figure's line
ends at its endpoint, so ink alone would let the line answer for the star it
was drawn to. The lines, the grid, the boundaries and the furniture are laid
down identically in both paintings; a pixel that changes is a star.

The last column is the chart's own furniture, not a missing star. The title
block is painted over the sky, and a node beneath it is covered like anything
else there - it happens on the released 42-degree page too, where no star is
held back for a figure at all. Each one is counted there only if it falls
inside the block's own bounds and its node reappears when the furniture is
switched off: painted, and then painted over.

## What it costs

| centre | field | stars at the limit | kept below it | faintest kept | its radius |
|---|---:|---:|---:|---:|---:|
| Orion | 42° | 2108 | 0 | — | — |
| Orion | 60° | 161 | 0 | — | — |
| Orion | 90° | 98 | 43 | V 4.9 | 3.16 px |
| Orion | 120° | 139 | 76 | V 6.5 | 2.30 px |
| Sagittarius | 42° | 1863 | 0 | — | — |
| Sagittarius | 60° | 162 | 4 | V 5.6 | 2.82 px |
| Sagittarius | 90° | 95 | 55 | V 5.6 | 2.82 px |
| Sagittarius | 120° | 148 | 83 | V 5.6 | 2.82 px |
| M31 | 42° | 1760 | 0 | — | — |
| M31 | 60° | 125 | 1 | V 5.9 | 2.66 px |
| M31 | 90° | 73 | 60 | V 5.9 | 2.66 px |
| M31 | 120° | 110 | 86 | V 6.5 | 2.30 px |

## The size floor, which is the one the atlas already has

An anchor is kept, not promoted: it is drawn at the size its own magnitude
asks for, because its size is a statement about its brightness and it is no
brighter for being structural. No new floor was added, and this is why.

| | magnitude | radius |
|---|---:|---:|
| faintest star kept for a figure | V 6.5 | 2.30 px |
| faintest star on the released Home page | V 8.0 | 1.32 px |

The atlas has drawn the smaller of those two on every page it has shipped
since 1.0. An anchor is a larger mark than that, so the existing policy is the
measured floor and a new one would only misstate a star's brightness.

## What it must not do

**Nothing else below the limit.** An endpoint is one star - the nearest inside
the matching tolerance, and only it. Thirty of the pack's matchable endpoints
have a second star that close, a companion or a neighbouring catalogue entry;
keeping everything inside the tolerance admitted a magnitude 7.8 star to a page
limited at V 4.0, which is the very thing this must not do.

**No label.** A node and a name are different promises, and the page's own
label thresholds are brighter than its limiting magnitude at every overview
rung, so an anchor cannot qualify for one at all:

| field | page limit | faintest star the policy would label |
|---:|---:|---:|
| 60° | V 5.0 | V 3.5 |
| 90° | V 4.0 | V 3.5 |
| 120° | V 4.0 | V 3.5 |

**Nothing when the figures are off.** The exception exists to complete a shape;
with no shape drawn there is nothing to complete.

## How an endpoint finds its star

The figures are stored as coordinates and carry no catalogue identity, so a
star is matched to an endpoint by position - and the distance that means
"the same star" is measured rather than chosen. Over every figure endpoint the
bundled pack can show:

| of 335 endpoints | nearest star within |
|---|---:|
| 50% of them | 0.000048° |
| 90% of them | 0.000705° |
| 95% of them | 2.277698° |
| matched at all | 314 |
| the nearest that is not | 0.89° |

The two populations are separated by three orders of magnitude of empty
space, and 0.01 degrees sits in the middle of it. The far ones are endpoints
whose star is outside the queried sky; they match nothing, and are meant to.
