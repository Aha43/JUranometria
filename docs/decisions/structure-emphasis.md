# Temporary cartographic emphasis: the seam and the frozen palette

Issue #361, phase 1. The atlas's canonical chart stays the authored
default; while reading, a person may let exactly one semantic
structure rise from the page, and the atlas settles back. This
record freezes the ink decisions that phase 2 (the transient control
and the explicit emphasized export) builds on.

## The boundary the discovery proved

Emphasis alters ink only. It must never reuse visibility toggles or
any path that changes anchors, names, geometry or membership: the
discovery mock showed that switching constellation figures off also
removes their #307-anchored stars and relocates constellation names
- a reflow, which is exactly what a momentary reading aid must not
cause. The seam therefore threads one optional target through the
painters and changes nothing else; the canonical page with no
target is byte-identical to the page before the seam existed, held
by contract.

## The seam

- `ChartStructure` (chart-owned): MERIDIAN, ECLIPTIC,
  EQUATORIAL_GRID, HORIZON, CONSTELLATION_BOUNDARIES,
  CONSTELLATION_FIGURES. Core painters name the constants directly.
  Modules keep contributing typed geometry under their own string
  identities and never see chart types; the chart maps the
  identities it knows centrally - meridian and zenith to MERIDIAN;
  horizon and the four cardinal landmarks to HORIZON; ecliptic and
  its four seasonal landmarks to ECLIPTIC. An unknown identity
  keeps canonical ink under every selection.
- `StructureStyle` is the one style resolver: palette, structure,
  selected-or-not, and the canonical ink and stroke in; colour and
  stroke out, together. Painters do not invent accents. Unselected
  returns exactly what came in, so every non-target layer is
  canonical by construction.
- Grid emphasis includes the coordinate notation with the curves.
  Figure emphasis colours only the figure strokes - anchored stars
  and constellation names keep canonical ink, held by contract
  against the renderer's published figure shapes.
- The target is transient presentation context beside
  `ChartOptions`, never inside it and never persisted.

## The emphasized style

- **Stroke: canonical + 0.6 px**, with the dash pattern kept, so
  each structure's solid/dashed/dash-dot identity survives and the
  distinction survives monochrome output through stroke and
  luminance rather than hue.
- **No dimming of competing ink.** Emphasis is one mechanism in
  this phase; if it proves insufficient without quieting the rest
  of the page, that returns as evidence before a second mechanism
  is added.

## The frozen palette

Measured on the full matrix - Sagittarius and Orion at 8, 42 and
120 degrees plus the 180-degree globe, both grounds, with the
meridian/horizon and ecliptic/grid crossings on the pages, in
colour, luminance-only monochrome, and the three common
dichromacies by the Vienot/Brettel linear-RGB matrices. Every
accent clears 4.5:1 WCAG against its ground (weakest 5.16:1, the
paper grid) and separates from the canonical ink it replaces under
its most limiting simulation.

| structure | white paper | black sky |
|---|---|---|
| meridian | `#a42e2e` | `#e8907e` |
| ecliptic | `#82610f` | `#cfa84e` |
| equatorial grid | `#566e96` | `#7a98c7` |
| horizon | `#4a6c52` | `#8bb092` |
| constellation boundaries | `#6d4c8d` | `#a384c7` |
| constellation figures | `#944f30` | `#cc7447` |

Two provisional candidates were measured and rejected:

- paper ecliptic `#9e761c` - luminance nearly equal to the
  canonical grey (monochrome separation 1.6), which would have left
  monochrome output depending on the stroke gain alone; darkened to
  `#82610f` (7.1, and ground contrast 4.15:1 to 5.73:1);
- black-sky meridian `#db7066` - collapsing to 5.5 from its
  canonical grey under protanopia; lightened and warmed to
  `#e8907e` (13.7, comparable to the accepted lower range).

Accepted as borderline, deliberately: the paper horizon (monochrome
7.8) and the black-sky figures under protanopia (10.9) - the hue
stays readable, the luminance separation is adequate, and the
stroke gain carries the distinction where colour weakens. The
horizon is the most restrained of the six on purpose.

The study harness (`EmphasisStudyMain`) regenerates the whole
matrix and the measurement tables into `build/emphasis-study`;
study output only, promoted nowhere.

## Observed limitation

A structure appears only where its geometry crosses the page - at
the study's Oslo instant the meridian misses the Orion pages and
shows on Sagittarius. Phase 2's control must disable unavailable
targets rather than emphasize an absent line.
