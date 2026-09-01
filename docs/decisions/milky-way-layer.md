# Decision: how the atlas shows the Milky Way

**Sprint 22, issue #189.** Status: **open on licensing**. The
cartography is decided and measured; whether any of it may be built
depends on an answer that has not arrived. Measured with
`make milky-way-study` →
[`docs/studies/milky-way/`](../studies/milky-way/).

Production packs, options, stores, scenes and rendering are untouched
by this issue.

## What the layer would mean

**Five source-defined levels rendered as three chart washes.**

That sentence is the whole claim, and it is deliberately smaller than
the one this gate started with. The proposed wording was *"integrated
light of unresolved stars"*, which the data does not support: the
source is a **hand-contributed outline catalogue**, not a photometric
measurement. In the interface the layer is called simply **Milky
Way**; the provenance explains what kind of thing it is.

No level may be described as a brightness in any unit. The source
orders its levels and says nothing more, so neither does the atlas.

## The source, and why the gate is still open

**Candidate: `mw.json` from d3-celestial**, pinned commit
`7e720a3de062059d4c5400a379146a601d9010e0`, 534,254 bytes, SHA-256
`aee221a7a0e879418e685de00c3e68fbdfac5667c0a8aab74929ef9cf4aab4fb`.

The chain, as far as it can be established:

- d3-celestial's readme lists `mw.json` among the project's own data
  files and describes it as *"Milky Way outlines in 5 brightness
  steps"*, crediting the **Milky Way Outline Catalog by Jose R.
  Vieira**.
- d3-celestial is released under **BSD-3-Clause**. The licence names
  Olaf Frohn and speaks of "software".
- The catalogue's own page (`skymap.com/milkyway_cat.htm`) is gone.
  The Internet Archive's copy shows a **user-contributed catalogue,
  submitted 16 March 2000**, whose instructions were in a
  `ReadMe.txt` inside the download. That archive does not appear to be
  preserved, so **what Vieira granted cannot be read**.

So the honest description is: **conditionally acceptable as the
d3-celestial distributed outline dataset.** The repository's explicit
treatment of `mw.json` as a project data file is stronger than
finding an unexplained file in a BSD repository — and it is still not
documentation of the upstream grant. This decision does not claim
that Vieira released the catalogue under BSD, because no evidence
says so.

**The question is with the maintainer**:
[d3-celestial#160](https://github.com/ofrohn/d3-celestial/issues/160),
opened 2026-09-01, asking whether BSD-3 covers `data/mw.json`,
whether redistribution and transformation are permitted, what terms
allowed the Vieira catalogue to be redistributed, and whether the
original `ReadMe.txt` survives. An answer that the repository is BSD
would not close the gap.

**If the chain cannot be established, the layer is parked.** That is
a successful gate, not a failed sprint: the alternative is to weaken
a licensing standard this project has already used to reject better
data.

### Rejected sources

- **Mellinger, Milky Way Panorama 2.0** — a calibrated visible-light
  panorama with a published construction (PASP, 2009), and the
  scientifically strongest candidate by some distance. Its
  distribution page states only *"Copyright © 2000–2018 Axel
  Mellinger"*: **no redistribution licence**. Rejected on the same
  hygiene rule that rejected the IAU data files and VizieR's
  `bound_20.dat` in Sprint 7. A paper describing how data was made is
  not permission to redistribute it.
- **KStars `milkyway.dat`** — 135 contours, equatorial, explicit
  licence: **GPL-2.0-or-later**. Rejected on compatibility, not
  hygiene: bundling copyleft data into an MIT-licensed application
  imposes terms this project does not carry.
- **NASA SVS Deep Star Maps 2020** — the best licence position of all
  (public domain, credit requested), and the wrong thing: a rendered
  image of 7 billion catalogued stars. Turning it into outlines means
  thresholding a colour-mapped visualisation, which is inventing
  boundaries, and it depicts star flux rather than the naked-eye
  Milky Way. Rejected on meaning.
- **Photographic textures, decorative gradients, invented feathering,
  a single hand-drawn band** — rejected outright. The atlas draws
  from data or it does not draw.

## What the data is

| level | rings | filled | holes | points |
|---|---:|---:|---:|---:|
| `ol1` | 10 | 7 | 3 | 10,126 |
| `ol2` | 113 | 88 | 25 | 12,472 |
| `ol3` | 46 | 40 | 6 | 5,651 |
| `ol4` | 27 | 27 | 0 | 1,843 |
| `ol5` | 6 | 6 | 0 | 584 |
| **all** | **202** | | | **30,676** |

Equatorial, longitude −180°…180° converted to right ascension,
declination −74.9°…+66.9°. Every ring closed.

**Three rules the data forces, each measured rather than assumed:**

1. **Filling is by containment depth, never by winding.** The source
   mixes directions — level 2 alone carries 103 clockwise rings and
   10 counter-clockwise — so the GeoJSON convention that outer rings
   run counter-clockwise does not hold. Measured containment depth
   never exceeds **1**: this is regions and holes, never islands
   inside holes.
2. **There is no frame in which nothing wraps.** Four rings cross the
   source's own ±180 meridian (eight steps); three cross RA 0 once
   converted to the atlas's frame. An importer must unwrap
   deliberately. The study's unwrapping leaves **202 of 202 rings
   continuous**.
3. **The levels are not strictly nested.** Sampling the sky at half a
   degree (231,120 samples), **1,300 samples lie in a higher level
   while outside level 1** — about 0.56% of covered sky *by that
   sample*, which is a sampled proportion and not a spherical-area
   computation. Named examples, exact: **RA 59.500°, dec −38.000°**
   and **RA 239.500°, dec −38.000°**, each inside level 2 and inside
   no other level.

## The projection problem, and the oracle

**The obvious implementation is wrong, and the study measured how
wrong.** Projecting each outline point and closing whatever survives
produces broken fills: a gnomonic page shows only the hemisphere in
front of it, so a ring reaching past that horizon arrives in pieces,
each piece closes on its own, and the even-odd rule then counts
crossings the sky does not have. At the **south galactic pole** — the
emptiest sky there is — that layer filled **100% of the page**.

So the study asks the sky instead: every pixel is turned back into a
sky position through the atlas's own inverse
(`ChartHitTest.skyAt`, what grab-to-pan uses) and tested against the
outlines. No horizon, no seam, no parity to lose. The poles came back
0.0% at every field, and every other page became astronomically
sensible.

**That sampler is the correctness oracle, not the implementation.**

Its cost is an **observation, not reproducible evidence**, and is kept
out of the study's report for that reason - a study whose output moves
between runs cannot be reproduced, and a faster machine must never
make one fail. Observed while writing this decision:

> 36 pages sampled in **116-382 ms each** on Mac OS X aarch64,
> OpenJDK 64-Bit Server VM 21.0.11.

That is hundreds of milliseconds for one page, against an interactive
budget of a repaint. **Per-pixel sampling is therefore rejected for
interactive rendering** - the conclusion needs only the order of
magnitude, which no plausible machine changes. #191 enforces an
actual interaction budget, on the supported CI path, against its own
implementation.

## The palette, and why three washes

Near-white washes are compared by **lightness (CIE L\*)**, not by
contrast ratio: at this end of the scale every ratio sits near 1:1
and separates nothing.

**The chart's own palette sets the budget.** Paper to `GALAXY_FILL`
— the palest mark the chart already draws — is **ΔL\* 8.00**, and
that is all the room a background wash may occupy.

| ladder | first step from paper | step to step | clear of galaxy fill |
|---|---:|---:|---:|
| five levels, visibly spaced | 1.73 | 1.74 | **−0.70** |
| five levels, squeezed above the galaxy fill | 2.42 | 1.05 | +1.40 |
| five levels, quietest possible | 1.38 | 1.04 | +2.45 |
| **three washes, greys 249 / 244 / 239** | **2.07** | **1.74** | **+2.45** |

The first attempt ends **darker than a galaxy**, which would sink a
galaxy into its own background. Five levels squeezed above the galaxy
fill step by about 1 L\*, which is where two washes stop being two.

**Three washes fit; five do not.** The source's five levels merge
**1–2 / 3 / 4–5** in rendering only.

Over the darkest wash every mark keeps its separation — grid lines
8.11 L\*, boundaries 17.47 L\*, text 81.22 L\*. The thinnest is the
**galaxy fill at 2.45 L\***, whose grey-132 outline carries the
symbol regardless. That is the tightest part of the palette and a
**named regression case**.

Measured on the rendered page rather than in theory: a Sagittarius
page contains **exactly four colours** — `#ffffff`, `#f9f9f9`,
`#f4f4f4`, `#efefef`. No translucency, no compositing, no antialiased
intermediates, so the L\* numbers above describe real pixels.

**The layer is theme-independent, verified**: light and dark produce
the same bytes, because the chart owns its palette.

## What #190 and #191 must carry

**The semantic rule**: the wash is chosen by the **highest source
level containing the point**.

**Acceptable implementations**: any method proven equivalent to that
rule. **Painting the five independent regions in order, opaquely, so
later levels overwrite earlier ones, is equivalent** - including
where the levels are not nested, because each region is painted for
itself and the last opaque paint wins. Ordering is not the hazard;
the four assumptions below are.

**Forbidden assumptions**, each of which would break on the
non-nested places measured above:

- strict nesting between levels;
- deciding the wash by *counting* how many levels contain a point;
- clipping higher levels to level 1;
- cumulative alpha, or any translucency whose result was not measured
  as composited pixels.

**Also required:**

- **Keep all five levels in the imported data.** The merge to three
  happens in rendering, so the transformation stays reversible.
- **Clip spherical regions to the visible page before closing or
  filling them**, and compare the result against the oracle on: both
  poles, the RA seam, fully enclosed pages, pages with holes, and
  partially covered pages — including the case ordinary polygon
  clipping misses, where **no outline edge enters the page but the
  whole page lies inside a region** (`cygnus 8°` and `ra-zero 8°` are
  exactly that: 100% covered).
- **Compare coverage numbers, not only screenshots.**
- **Measure the final rendered pixel colours**, including anything
  Java2D compositing or antialiasing does to them.
- **Benchmark during pan and zoom** against the existing synchronous
  interaction budget.

## Still to decide before #190 begins

- **Default on or off**, judged from representative Milky Way pages —
  whether the layer adds geographic understanding without becoming
  the page's subject. The released M31 default page carries **0 layer
  pixels of 630,000**, which is useful regression evidence and does
  not decide the question.
- Field-width policy, draw order, the Chart Options location and
  mnemonic, and whether toggling can remain repaint-only.

These wait on the licensing answer, because deciding them costs work
that a parked layer would not need.
