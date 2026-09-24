# Temporary cartographic emphasis: the seam and the frozen palette

Issue #361. The atlas's canonical chart stays the authored
default; while reading, a person may let exactly one semantic
structure rise from the page, and the atlas settles back. This
record freezes the ink decisions of phase 1 and the interaction and
export rules phase 2 settled on top of them.

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

## The transient control (phase 2)

One compact **Emphasis** button in the chart toolbar, before the
search field, opening one radio menu: Normal and the six structures
in the order above. The menu is built fresh each time it opens, so
it reads the chart rather than remembering it.

- **Transient, never persisted.** The emphasized structure lives on
  the chart component beside the options, survives panning and
  zooming, and dies with the session. No preference key exists;
  the persisted chart options carry no trace of it.
- **Availability and clearing.** A target is choosable only while
  its layer is switched on and drawn at the current field, or some
  module is contributing geometry the chart maps to it; unavailable
  targets arrive disabled. The moment an emphasized structure stops
  being available - its layer switched off, the detail policy
  narrowing past it, its module hiding its lines or detaching - the
  emphasis settles immediately. No invisible latent mode survives,
  and an unavailable ask settles rather than being held.
- **Choosing Normal, or the active target again, settles the page**
  - one named rule, held by contract.
- **Selection independence, both directions.** Selecting an object
  never changes emphasis; emphasizing never changes the selection
  or the working set.
- No keyboard shortcut and no press-and-hold in this phase, by
  ruling: the visible control establishes the interaction first.

## The explicit emphasized export (phase 2)

Ordinary export is the canonical chart whatever the screen shows -
byte for byte, by the same code path, held by contract. While
something is emphasized, the export dialog offers one explicit
choice, **Include current emphasis**, always starting unchecked and
absent entirely otherwise; nothing about it is remembered between
exports. When the reader checks it:

- one recording is made and SVG, PDF and PNG all write it, so the
  three formats cannot disagree about what was emphasized;
- each format records the semantic target as the stable token of
  the structure (`emphasis:equatorial-grid` and its kin) - SVG in a
  metadata element, PDF as a Keywords entry, PNG as an Emphasis
  text entry. A token, not prose, so the record survives any
  interface language, and absent entirely from an ordinary sheet;
- the sheet stays white paper, as every sheet is, drawn with the
  paper accents.

Cancel, ordinary export, and clearing emphasis all leave the
canonical export bytes untouched.

## Observed limitation

A structure appears only where its geometry crosses the page - at
the study's Oslo instant the meridian misses the Orion pages and
shows on Sagittarius. The control therefore disables unavailable
targets rather than emphasizing an absent line; availability is
read from the layers and the contributed geometry, not from
whether the page's frame happens to intersect the structure.

## Multiple emphasis (follow-up)

The reader may raise any number of available structures at once.
This supersedes the single-target wording above wherever the two
differ; everything else stands.

- **The set.** The transient state is a set of structures with one
  deterministic order, the enum's own, still never persisted. The
  menu shows independent checkmarks; choosing a structure toggles
  only that structure; **Normal** clears the whole set. When a
  structure becomes unavailable, only that member leaves; pan, zoom,
  selection, language and unrelated options keep the rest.
- **No new rendering rule.** No dimming, no colour blending, no new
  draw precedence: each layer keeps its frozen accent and its
  existing painter order, and crossings behave as they do
  canonically. Held by three contracts on the ruled crossing pages
  (grid with figures, grid with ecliptic, meridian with horizon),
  both grounds: away from crossings each structure reproduces its
  singleton pixels exactly; a combination changes nothing outside
  the singletons' shared stroke and antialiasing envelope; at
  crossings the later-painted structure keeps its frozen accent on
  top. The exact union of singleton masks is deliberately not
  demanded at crossings, where draw order and antialiasing may
  legitimately hide or composite part of the lower stroke.
- **The export.** A checked export carries every active token in
  enum order joined with `+` (`meridian+horizon`), the same string in
  SVG, PDF and PNG. A single target is the bare released token.
- **Colour is supplementary.** Multiple emphasis does not promise
  that every active structure can be identified by hue alone. Under
  the common dichromacies some co-raised accents sit close together,
  most of all when all six are raised (measured in
  `docs/studies/structure-emphasis/measurements.md`). Geometry, dash
  pattern, position and the menu's checked identities remain part
  of the reading system. Accepted by the owner as a documented
  limitation, not a defect.

### The migration proof, and what holds after it

On the capture Mac (macOS aarch64, Java 21.0.11), all 24 released
singleton pages (orion at 42 degrees and sagittarius at 120 degrees,
both grounds, all six structures) and the one-target export in all
three formats were byte-identical before and after the set-shaped
refactor, against hashes captured on `main` 3176d93 before any code
changed. That proves this migration, once, on that machine.

It is not a permanent contract: one machine's rasterization cannot
be another machine's contract, and the first CI run showed exactly
that. What holds permanently is equivalence on whatever runtime
runs the suite. The released one-target route (membership decided by
identity with its one target, metadata from that target's own token)
and the one-member set route (membership by set containment,
metadata from the ordered join) are drawn side by side in the same
JVM and must agree to the byte: every singleton page with the
module structures contributing, and every singleton export in SVG,
PDF and PNG. The two routes are shaped independently, and each
contract was proved able to fail by breaking one route at a time.
