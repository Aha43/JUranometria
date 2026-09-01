# Decision: the title block and the stellar-magnitude key

**Sprint 20, issue #179.** Status: proposed for review. This gate
adds one small piece of chart furniture and names another, measured
over eight real pages (`make furniture-study` →
[`docs/studies/chart-furniture/`](../studies/chart-furniture/)).

A reader can tell that one dot is larger than another. What they
cannot tell is *how much brighter* that means — the chart draws
magnitude as a circle, and nothing on the page says so. This gate
decides how to say it without turning a quiet chart into a diagram
of itself.

## The key's samples: three, and why not more

Circles are drawn at exactly the radius the star pass would use for
that magnitude, through the same `StarSizePolicy`. The ladder is
what decides how many samples a key can honestly carry:

| pair | difference in drawn diameter |
|---|---:|
| V 0 → V 1 | **0.40 px** |
| V 3 → V 4 | 0.86 px |
| V 5 → V 6 | 1.01 px |
| V 7 → V 8 | 1.43 px |

**A key stepping by one magnitude would show circles a reader cannot
tell apart** — under a pixel and a half at every step — and would
imply a precision the drawing does not have. So the key shows
**three samples: the top of the scale, its middle, and the limit
itself**, always whole magnitudes:

| limiting magnitude | samples | drawn diameters | smallest difference |
|---:|---|---|---:|
| V 4 | V 0, V 2, V 4 | 10.00, 8.81, 7.13 px | 1.19 px |
| V 5 | V 0, V 3, V 5 | 10.00, 8.00, 6.21 px | 1.79 px |
| V 6 | V 0, V 3, V 6 | 10.00, 8.00, 5.20 px | 2.00 px |
| V 7 | V 0, V 4, V 7 | 10.00, 7.13, 4.06 px | 2.87 px |
| V 8 | V 0, V 4, V 8 | 10.00, 7.13, 2.63 px | 2.87 px |

The **limit is always one of the samples**, so the key names the
faintest star the page actually draws. At V 4 the three circles
differ by only 1.19 px, and that is the honest answer rather than a
defect: a page limited to the bright stars really does draw dots of
nearly equal size, and a key that exaggerated the difference would
be describing a different chart.

The heading reads **"Stars, visual magnitude"** and each row reads
`V 0`, `V 4`, `V 8`. Nothing claims that area or diameter is linear
in magnitude, because it is not — the mapping is the renderer's, and
the key shows its results rather than explaining its formula.

## Where it goes: the upper right

By elimination, then by inspection. The **title block** owns the
lower left; **right-ascension labels** run along the bottom;
**declination labels** run down the left. The upper right is the one
corner of the page carrying no furniture of its own, and the two
blocks balance across the diagonal
([the default page](../studies/chart-furniture/m31-08-with-key.png)).

Geometry follows the title block exactly, so there is one rule for
furniture rather than two: the same margin, the same padding, paper
fill with a frame rule, and **the same refusal** — a page too small
to hold the box inside its margins omits it rather than clipping it.

**Precedence**, deterministic and documented:

1. the graticule draws beneath everything, as it always has;
2. stars and symbols draw over it;
3. **furniture draws last and opaque** — the title block, then the
   key — so neither is ever half-covered by chart ink;
4. **labels yield to the furniture that will actually draw**: the
   key's box joins the title block in the occupied set the star-label
   pass honours, and in the reservation the grid's edge labels
   honour, so no label is placed where furniture will cover it.
   Furniture the reader has switched off reserves nothing — switching
   the title block off gives back the grid notation it was
   suppressing, which an earlier implementation did not.

## Default: offered, and initially hidden

**The key is off in the released default; the title block stays on.**

This is measured, not inherited. The key's box is 160×72 px — 1.83%
of a 900×700 page — and on the pages a reader explores it covers
real ink:

| page | chart ink covered | star and symbol ink |
|---|---:|---:|
| M31, 8° (the released default) | 288 px | **0 px** |
| quiet sky, 8° | 405 px | 0 px |
| Sagittarius, 8° | 400 px | 0 px |
| Polaris, 18° | 445 px | 33 px |
| Crux, 18° | 487 px | 118 px |
| **Orion, 36°** | 635 px | **436 px** |

The second column is **derived from the layers**, not from how dark a
pixel is: it measures the key's box on a page rendered with stars and
deep-sky symbols alone. An earlier version counted every pixel darker
than a grey threshold and called the result star ink, which also
counted labels, figures and constellation names — and, by missing the
paler edges of antialiased dots, *understated* the real figure (290 px
on Orion against the 436 px the layers actually draw).

On the wide and crowded pages the key blanks a patch of sky
containing stars — visible in
[Orion with the key](../studies/chart-furniture/orion-36-with-key.png)
against [Orion without it](../studies/chart-furniture/orion-36-without-key.png).
A reader who wants the scale explained turns it on and accepts that
cost knowingly; a reader who does not should never pay it. The
atlas's habit is that the page shows the sky and the furniture
earns its place.

Two consequences follow, and both are deliberate:

- **the released default page is unchanged**, so no reference or
  study image churns for this sprint;
- the key is **discoverable only through Chart Options**, which is
  the same way every other layer is discovered.

The title block keeps its released behaviour — on — and becomes
switchable for the first time, for the reader who wants the bare
page.

## What the lower-left panel is called

**Title block**, everywhere a reader can see: the Chart Options
label, its accessible name, the README, and this decision. It is the
cartographic term for exactly this object — the panel stating what
the sheet shows, its centre, its frame, its scale and its
orientation — and the code has called it `titleBlock` since Sprint 1.
"Chart title" was considered and rejected: the panel is not a title,
it is five statements about the page, and calling it a title would
mislead a reader looking for the thing that names their target.

## Where the controls live

In the existing **Content** group, with no third group and no tab.
The dialog carries two labelled groups today; two more checkboxes
make eleven in one compact column. A "Furniture" group would be a
heading over two lines, which costs a reader more attention than it
saves. If a third piece of furniture ever arrives, that is the
sprint that earns the group.

The two are **independent options**: either, both, or neither. Both
are **repaint-only** — no catalogue query, no scene assembly, no
navigation, target, or selection change — like every other option
since Sprint 12.

## What this gate rejects

- **A key stepping by one magnitude.** Measured at 0.4–1.4 px per
  step: circles nobody can distinguish.
- **A key that starts at the brightest star on the page.** It would
  change as the reader panned, which is a key that describes the
  page rather than the scale.
- **The lower right.** Right-ascension labels already run along the
  bottom edge.
- **Drawing the key by default.** It covers up to 436 px of star and
  symbol ink on a wide page, and the reader has not asked for it.
- **A third options group.** Two checkboxes do not earn a heading.

## Consequences

- `ChartRenderer` owns `magnitudeKeySamples`, `magnitudeKeyBounds`
  and `drawMagnitudeKey`; the study renders the production key
  rather than a copy of it, and a test fails if the two could
  diverge.
- #180 wires both as independent, persisted, migrated options with
  the label-precedence above, and puts the Inspector on the toolbar.
- #181 walks the journey and closes the sprint.
- **No reference image changes in this sprint**, because the default
  page does not change.
