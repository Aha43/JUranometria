# Decision: the chart-options contract and first controls

Decided 2026-08-30 for Sprint 12, "Let the reader choose the chart"
(issue #103), from an inventory of the render pipeline's policy seams
and rendered evidence over the real catalogue and geography
(`make chart-options-study`; representative pages committed under
`docs/studies/chart-options/`).

## Inventory: where every candidate decision lives today

| Ink | Decided by | Seam |
|---|---|---|
| Stars | scene magnitude cull in `ChartRenderer` (toolbar-driven) | renderer star pass |
| Deep-sky symbols | `RegionalDetailPolicy.drawn/clampAllowed` | renderer DSO pass |
| Deep-sky labels | `RegionalDetailPolicy.labelled` | renderer label pass |
| Constellation figures | `GeographyDetailPolicy.figuresDrawn` (≥12°) | renderer geography pass |
| Constellation boundaries | `GeographyDetailPolicy.boundariesDrawn` (≥18°) | renderer geography pass |
| Constellation names | `GeographyDetailPolicy.namesDrawn` + visible-figure-ink centroids | renderer geography pass |

**The single seam that should consume options is the renderer's two
policy objects.** Everything a candidate option affects is already
decided there; nothing lives in assembly or painting order that an
option would need to reach.

## The first-release options and defaults

Five options, all defaulting **on** — the released defaults are
exactly the current chart at every field, anchored by the study's
first render being byte-identical to the released M31 reference:

| Option | Default | Governs |
|---|---|---|
| Deep-sky objects | on | the DSO symbol pass |
| Deep-sky labels | on | the DSO label pass |
| Constellation figures | on | the figure pass |
| Constellation boundaries | on | the boundary pass |
| Constellation names | on | the name pass; **effective only while figures are on** (below) |

**Rejected as options, with reasons:**

- **Stars** — the atlas's substance; a starless page is not a chart of
  this product, and the toolbar's magnitude limit already gives the
  reader honest control of stellar depth. Rejected outright.
- **Per-family DSO splits** (nebulae/galaxies/clusters separately) —
  over-granular for a first release; the single deep-sky toggle
  matches how the studied pages read. Open for later evidence.
- **Names without figures** — rendered
  (`m42-36-figures-off-names-on.png`): the naming policy anchors on
  visible figure ink, so without figures there is nothing honest to
  anchor to, and detached names floating over stars would break the
  #65 principle that names explain what is on the page. Therefore
  **Names depends on Figures**: the control stays visible but
  disabled (its state remembered) while figures are off. The genuine
  alternative — boundary-region-centroid naming for a boundary-only
  chart style — is a real future feature, not a toggle side-effect.

## Enabled means permission; the scale policy stays automatic

An enabled option is **permission to draw where policy allows**;
disabled means **never draw**. Nothing the policies decide becomes a
user responsibility: magnitude limits, the regional true-size rule,
Messier clamping, and every geography scale threshold continue to
apply inside an enabled layer exactly as released. Options add one
gate in front of each policy question, nothing else.

## The searched target keeps its guarantee

The existing honesty rule — the chart never titles itself by a
symbol-capable target it does not show — **extends across the
toggles**: a searched target with an established symbol is always
drawn and always labelled, even when the deep-sky symbol or label
layer is disabled. Evidence: `m57-36-dsos-off-target-kept.png` — the
reader hides the deep-sky crowd and keeps exactly their target, M57's
clamped ring alone between Lyra's stars. This is the same exemption
the regional detail policy already applies at wide fields, now
uniform across user choice; symbol-less types still recenter and
title only, unchanged.

## Model, persistence, Home, and Restore Defaults

- **`ChartOptions` is an immutable record of the five booleans**,
  presentation state consumed by the renderer's policy seam. It lives
  in neither `ChartViewState` (it is not navigation) nor `ChartScene`
  (it is not sky data) — so **every toggle is repaint-only**: the
  study re-rendered the same assembled 36° scene with a layer emptied
  in 3–4 ms, no reassembly, no catalogue or geography query. Nothing
  in the first set justifies reassembly.
- **Options persist across launches** through the established
  JDK-preferences boundary (the appearance store's node; unknown or
  corrupt values mean the default **on**, never a launch failure).
- **Home resets navigation only** — centre, field, magnitude, target —
  and never touches chart options; the two kinds of state stay in
  their separate stores by construction.
- **Restore Defaults** (a button in the dialog) sets all five options
  on — the released chart — as a live preview like any other change,
  confirmed or abandoned by the same OK/Cancel.

## Dialog behaviour

- **Live preview with OK/Cancel**: toggling a checkbox repaints the
  chart immediately (justified by the repaint-only measurement — the
  reader is choosing ink and must see it). **OK** keeps the previewed
  state and persists it; **Cancel, window close, and Escape revert to
  the options captured when the dialog opened** and persist nothing.
  The dialog is modeless (the established convention), owned and
  centred on the atlas window, so the chart stays visible beside it.
- **No tabs.** The five real controls form two labelled groups —
  **Content** (Deep-sky objects, Constellation figures, Constellation
  boundaries) and **Labels** (Deep-sky labels, Constellation names) —
  which fit one compact panel; tabs are not earned by five
  checkboxes, and no empty future tab exists.
- Keyboard order runs Content top-to-bottom, then Labels, then
  Restore Defaults, Cancel, OK; every control carries an accessible
  name; the packed dialog is its own minimum size; FlatLaf styles it
  in both themes exactly as the About and Settings dialogs.
- **Menu: a new View menu between File and Help, holding Chart
  Options…** — the chart-domain home the menu bar lacked. No toolbar
  button; zoom, magnitude, search, and Home stay where they are.

## Render/query contract (for #104)

- `ChartComponent` holds the current `ChartOptions` and passes them to
  the renderer; changing options triggers repaint of the existing
  scene, never `assembleScene`.
- `RegionalDetailPolicy` and `GeographyDetailPolicy` each gain the
  option gate in front of their existing answers; the target
  exemption is decided inside `RegionalDetailPolicy` where the
  identity already lives.
- Assembly continues to query geography by scale policy alone, so a
  layer toggled back on is already in the scene — the price is the
  already-measured ~2 ms of geography in wide-field assembly, paid
  today regardless.

## Test matrix (chosen, not exhaustive)

The state space is 2⁵ = 32; the matrix that carries the meaning:
defaults byte-identical to the released charts; each single option
off; all off; symbols-off and labels-off each with a searched target
(the exemption); the Names-requires-Figures dependency; persistence
round-trip with corrupt-value fallback; Home leaving options alone;
live preview applying without queries and Cancel reverting. Blind
combination snapshots are rejected — the options are independent
gates at one seam, and the pairwise evidence above is where the
interactions genuinely live.

## Worked visual evidence (committed)

| Page | Shows |
|---|---|
| `m42-36-defaults.png` | every layer, the released look |
| `m42-36-dsos-off.png` | Orion's figure and names over a pure star field — the "learn the constellations" chart the toggle exists for |
| `m42-36-geography-off.png` | the pre-Sprint-7 regional chart, recoverable by choice |
| `m42-36-figures-off-names-on.png` | why Names depends on Figures: no ink, no honest anchors, no names |
| `m57-36-defaults.png` / `m57-36-dsos-off-target-kept.png` | the target exemption across a toggle |

## Consequences

- #104 implements `ChartOptions`, the policy gates, the persistence
  keys, and the target exemption exactly as above; #105 implements
  the View menu and the live-preview dialog exactly as above and
  nothing more; the study tool (`make chart-options-study`) stays as
  the reproducibility path.
- The released M31 reference and all committed pages remain
  byte-identical until a reader chooses otherwise — and Restore
  Defaults always brings the released chart back.
