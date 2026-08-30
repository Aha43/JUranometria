# Decision: the equatorial coordinate grid

**Sprint 15, issue #132.** Status: proposed for review. This gate
changes no product behavior; the M31 reference is byte-identical.

## What the grid is

A quiet ICRS/J2000 equatorial graticule: **constant-right-ascension
meridians and constant-declination parallels**, projected through the
chart's real gnomonic viewport and mapping — the same frame every
star, object, and boundary already lives in. No other coordinate
system (alt-azimuth, ecliptic, galactic) and no epoch controls: the
chart's frame is stated in its title block, and the grid draws that
frame, nothing else.

## Projection-correct curves, measured

Curves are **sampled along the sky and drawn as clipped chord
pieces** — the geography pass's proven approach, never a straight
screen-space chord between distant points. The sample step is
field/180 (about 5 px per chord at the released page width);
each chord's midpoint is checked against the true projected curve
midpoint, and the measured worst error across every study page is
**0.004 px** (tall letterboxed 36° page; ordinary pages measure
≤ 0.001 px). Stated tolerance: **0.05 px** — an order of magnitude of
headroom, locked by test. In the gnomonic projection meridians are
exactly straight (great circles) and parallels are conics; both
emerge correctly from the one sampling rule with no special cases.

Candidate curves and sample ranges come from the **page's own sky
bounds**, measured through the exact inverse projection along the
page border (with the full RA circle when a celestial pole lies on
the page) — so the pass touches only visible sky. Mean computation:
**2.0 ms per page** — 3–6k subdivision samples on ordinary pages,
9.6k on the polar page, and **24.8k on the tall letterboxed page**
(whose paper is nearly seven ordinary pages of sky) — comfortably
synchronous repaint in every case, and by construction a
pure function of the viewport: **no catalogue or geography query can
occur** (the seam sees only `ChartViewport`).

## Intervals: one adaptive rule, no tables

From the pleasant steps — RA: 1m, 2m, 5m, 10m, 20m, 30m, 1h, 2h, 3h,
6h of time; Dec: 15′, 30′, 1°, 2°, 5°, 10°, 15° — each axis independently
chooses **the smallest step whose on-page spacing at the page centre
is at least 110 px**, RA spacing scaled by cos(centre declination).
One rule gives every behavior the gate asked for:

- RA and Dec may differ (they usually do; the measured table below).
- Adjacent field widths cannot jump densities: spacing stays within
  [110, ~275) px by construction.
- High-declination pages widen the RA step by the same rule as
  everything else; at the pole the step reaches its 6h cap and **four
  meridians radiate through concentric parallels** — the classical
  polar chart, with no seams and no runaway density (measured:
  4 meridians, 9 parallels on the dec 89.9 page).

Measured on the committed pages (`make grid-study`,
docs/studies/coordinate-grid/):

| page | field | RA step | Dec step | meridians × parallels | labels (suppressed) | worst err px |
|---|---:|---|---|---|---|---|
| m31-08 | 8° | 10m | 1° | 4 × 6 | 9 (1) | 0.000 |
| orion-36 | 36° | 20m | 5° | 7 × 5 | 10 (2) | 0.000 |
| orion-12 | 12° | 10m | 2° | 4 × 5 | 7 (1) | 0.000 |
| orion-03 | 3° | 2m | 30′ | 6 × 5 | 8 (3) | 0.000 |
| m42-01 | 1° | 1m | 15′ | 4 × 3 | 5 (2) | 0.000 |
| ra-wrap-24 | 24° | 20m | 5° | 8 × 5 | 7 (2) | 0.001 |
| polar-36 | 36° | 6h | 5° | 4 × 9 | 2 (0) | 0.001 |
| dec60-18 | 18° | 20m | 5° | 8 × 3 | 7 (2) | 0.001 |
| crux-18 | 18° | 20m | 5° | 10 × 3 | 9 (3) | 0.000 |
| pleiades-08 | 8° | 5m | 1° | 8 × 7 | 10 (3) | 0.000 |
| minwin-08 | 8°, 500×400 | 10m | 2° | 4 × 5 | 5 (3) | 0.000 |
| letterbox-36 | 36°, 900×4712 | 6h | 5° | 4 × 23 | 9 (0) | 0.004 |

## Labels

- **RA along the bottom edge** where each meridian meets it, in grid
  notation: whole hours bare ("6h"), otherwise hours and whole
  minutes ("5h 40m") — deliberately distinct from the title block's
  decimal-minute formal notation, because a grid label names a line,
  not a position. **RA 0h wraps as "0h"**, and east-left ordering is
  locked by test (greater RA lands further left).
- **Dec along the left edge**, signed: "+41°", "−5°"; arcminutes only
  when the interval is finer than a degree ("+41° 30′").
- One label per curve; a curve that never meets its labelling edge
  stays unlabelled — honest omission over invented placement. Labels
  belong to the **paper**, not the window: on letterboxed and
  minimum-size pages they sit at the paper's edges (measured pages
  included).
- **One exact bounds calculation governs everything** (PR #137
  review): the label's box comes from the real font metrics — never
  a guessed width — and that single calculation decides placement,
  **paper containment** (a label whose box would clip the paper edge
  after ordinary panning is suppressed, not clipped), title
  suppression, and drawing.
- **The title block wins**: a grid label whose exact box intersects
  the shared production `ChartRenderer.titleBlockBounds` is
  suppressed (measured: 0–3 per page, counted with the containment
  suppressions in the study report).

## Visual hierarchy

Grid ink is the quietest on the chart: 1-px lines at (216,216,216) —
lighter than boundary ink (190) — with labels at (150,150,150) in a
10 pt face. The grid draws **beneath everything**: paper < grid <
geography < stars < star labels < deep-sky labels < title block. The
standing rule that grid ink is subordinate is kept literally: every
other pass paints over it, and the grid never joins any collision
set except its own labels yielding to the title block.

The study emulates under-drawing exactly by compositing the finished
chart over the grid (paper pixels let the grid through); this is
identical to production under-drawing except at antialiased ink
fringes crossing a grid line. #133 regenerates the study pages
through the real pass — the Sprint 13 parity pattern.

## Scale behavior and the default

The rendered evidence (the absent / lines-only / labelled triple on
the M31 page, and the full sweep 1°–36°) shows the grid earning its
ink at every released field: wide pages read as charts with
graticule, narrow pages gain the scale cue that a bare star field
lacks, and the polar page becomes the classical polar chart. The
labelled variant is recommended over lines-only: unlabelled lines
ask the reader to guess what they show.

**Recommendation: the grid is a Chart Option, default ON.** The
atlas's purpose is reading the sky, and coordinates are part of
reading it; the ink is demonstrably quiet. This **deliberately
changes the M31 reference** when #133 lands — the same explicit
owner decision shape as the Sprint 13 label change, called out here
so the gate review can accept or reverse it (default OFF keeps every
released pixel and costs only the discoverability of the toggle).

## The option and the seam

- One new Chart Options control: **"Coordinate grid"**, Content
  group, no dependency, persisted as `chart.coordinateGrid`,
  repaint-only under the established contract (live preview, OK
  persists, Cancel reverts, Restore Defaults includes it, Home
  untouched).
- The production seam (#133): a pure `render`-package pass computed
  from `ChartViewport` alone — the study's `GridStudyMain` geometry
  (bounds, spec, sampling, labelling) is the reference
  implementation, moved rather than mirrored. It cannot query
  catalogue or geography **by type**: nothing else is reachable from
  its inputs.

## Rejected alternatives

- **Border rulers/ticks instead of lines**: out of scope by the
  issue, and rejected on merit — ticks demand eye travel to the
  edges; the atlas's finder-chart use wants in-field reference.
- **A fixed interval table per field width**: duplicates what the
  one spacing rule derives, and breaks on high-declination pages
  where the table would need a second dimension.
- **Screen-space straight chords between grid intersections**:
  measurably wrong on parallels (the whole reason for the 0.05 px
  tolerance and its test).
- **Labels on all four edges**: doubles the ink for no new
  information at the released page sizes.
- **Default OFF** (recorded as the reversible alternative): keeps
  the released pixels but hides the sprint's purpose behind a
  dialog; the recommendation stands ON with the deliberate,
  reviewed reference update.

## Consequences

- #133 implements the pass in the renderer beneath geography, ports
  the study geometry as the production seam, regenerates the study
  pages through it, and (if the default stands) updates the M31
  reference deliberately with visual review; #134 adds the Chart
  Options control with the full persistence contract; #135 walks the
  journey and hands over.
- The gate tests lock east-left ordering, RA-wrap continuity, signed
  declination and 0h notation, polar convergence bounds, honest
  clipping, deterministic output, the interval spacing rule, and the
  0.05 px tolerance — production acceptance written before
  production exists.
- This gate changes no product behavior; the M31 reference is
  byte-identical.
