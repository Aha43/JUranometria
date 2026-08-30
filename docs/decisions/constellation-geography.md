# Decision: constellation-geography sources and chart contract

Decided 2026-08-29 for Sprint 7, "Give the wider sky its geography"
(issue #63), from source research with verified licensing and from
rendered evidence over the real catalogue, projection, and page
geometry (`make constellation-study`; representative pages committed
under `docs/studies/constellation-geography/`). Candidate layers were
studied at 8°, 12°, 18°, 24°, and 36° around M42, M31, M45, an
RA-wrap field, and a polar field.

## Selected sources

All three layers come from one coherent, redistributable source:
**d3-celestial** by Olaf Frohn (https://github.com/ofrohn/d3-celestial),
licensed **BSD-3-Clause** for the whole repository including its data
files, pinned at commit `7e720a3de062059d4c5400a379146a601d9010e0`
and fetched reproducibly by `scripts/download-constellation-sources.sh`
into gitignored `imports/raw/constellations/` (SHA-256 recorded at
import, as for the catalogue pack):

| File | Layer | Content |
|---|---|---|
| `constellations.lines.json` | line figures | 743 great-circle segments over 89 features (88 constellations; Serpens in two parts), GeoJSON, J2000 |
| `constellations.bounds.json` | boundaries | 1,570 corner-to-corner edges (the Delporte corner set), GeoJSON, J2000 |
| `constellations.json` | names | id, Latin name, genitive, IAU abbreviation, prominence rank 1–3, label anchor |

SHA-256 of the pinned files, verified by the import tool of #64:

```
294f66bef5d5cf50b1e17f16d2efa1d97a15131612c68dd935adef6e7373e13c  constellations.lines.json
f2e2687af6b20b24567879f838c21874d412efcc93ecc1966be07e78431cc196  constellations.bounds.json
ab4ae692027cbc042c0d6791a84456a65eb7c55656107fd00c58ff6e55d4d8b2  constellations.json
```

**Provenance chain, recorded for the notices:** the boundaries
digitize Delporte's IAU-adopted 1930 delimitation (via the corner data
lineage of Davenhall & Leggett 1989, VizieR VI/49); the line figures
follow the convention drawn on the IAU/Sky & Telescope constellation
charts (published under CC BY 4.0) with Frohn's modifications; names
and abbreviations are the IAU's standard Latin names. Attribution in
the bundled NOTICE: Olaf Frohn (BSD-3-Clause), with the IAU/S&T chart
convention and the Delporte/VI/49 lineage credited.

## Boundaries versus figures: the astronomical distinction

The IAU standardizes **boundaries** (Delporte 1930, adopted 1928/1930)
and **names/abbreviations**. It does **not** standardize line figures:
stick figures are editorial conventions that differ between atlases.
JUranometria draws the figure convention of the IAU/S&T charts as
modified by Frohn, and its documentation must never call the figures
an IAU standard. Boundaries are dotted and quiet; figures are thin
grey lines; both live under the star ink.

## Coordinate contract and precession

- Source coordinates are **J2000**, matching the chart's ICRS J2000
  frame directly (the frames agree to well under an arcsecond, far
  inside chart tolerance). GeoJSON longitudes −180°..180° map to RA
  0°..360° at import. **No runtime precession exists anywhere.**
- **Boundary edges are constant-RA or constant-Dec arcs in the B1875
  frame**, not straight lines between their J2000 corners. Measured
  over all 1,570 corner pairs (IAU-1976 precession, B1875.0 =
  JD 2405889.25855): straight J2000 chords deviate from the true edge
  by up to **2.18° (131′)** — Chamaeleon's 10.6°-long constant-Dec
  edge near the south pole — with 313 edges off by more than 3′ and
  96 by more than 15′. The polar study page shows chords visibly
  cutting the Ursa Minor/Cepheus staircase.
- Therefore the boundary is **reconstructed, never chord-drawn**:
  precess each corner J2000 → B1875 (IAU-1976 angles; 1,562 of 1,570
  pairs verify as constant-RA or constant-Dec within 0.02°), walk the
  constant coordinate in steps of at most 1°, precess samples back to
  J2000, and store the sampled polyline. The reconstruction is
  implemented and measured **in the reproducible study tool**
  (`BoundaryReconstruction`, run by `make constellation-study`): 778
  constant-RA and 786 constant-Dec edges yield 13,186 polyline pieces
  whose worst deviation from the true edges is **0.065′ — inside the
  1′ tolerance** the decision requires, proven again by unit test on
  the worst polar case. The study pages render these reconstructed
  boundaries, not the rejected chords.
- **The 8 non-aligned corner pairs are classified**: they are the four
  shared edges of the Ursa Minor/Cepheus border (each appearing in
  both rings) where the source itself replaced the true constant-Dec
  +88.0000° B1875 arc (RA 345° → 120°) with great-circle chord
  samples — the chain's endpoints sit exactly on +88° while its
  interior vertices dip to +87.55°. The reconstruction detects such
  chains, requires their endpoints to share declination, and restores
  the true arc (visible on the polar study page as the smooth circle
  around Polaris). Any shape fitting neither constant coordinate is a
  hard error with a diagnostic — boundaries are never silently bent —
  and both behaviours are unit-tested.

## Model and query contract

- Immutable records in a new `juranometria.geo` (name final in #64):
  `Constellation` (id, Latin name, genitive, abbreviation, rank),
  figure segments and boundary polylines as lists of `SkyPosition`.
- Bundled as a compact plain-text resource in the pack style
  (CSV rows, manifest with SHA-256, NOTICE files, packaging test
  asserting the BSD licence text and attributions travel with the
  jar) — one resource, not tiled: the whole geography is ~15k sampled
  vertices, far below the catalogue pack.
- Bounded queries by `SkyRegion`, like the catalogue: segments whose
  endpoints or interpolated path intersect the padded region. The
  study proved the geometry cases: 8 figure segments legitimately
  cross RA 0 (e.g. Andromeda 9.22° → 354.53°) and draw seamlessly on
  the RA-wrap pages; the highest boundary corner (Cepheus, dec
  +88.70°) draws through the pole-adjacent projection; and segments
  are drawn subdivided at 0.5° steps along the sky with a **real
  line-versus-page intersection test per piece**, so a segment whose
  endpoints both lie off-page still contributes exactly its visible
  crossing (regression-tested with a corner-clipping segment whose
  endpoints and subdivision samples all project off-page).

## Scale policy (from the rendered comparisons)

| Layer | Draws at | Evidence |
|---|---|---|
| Line figures | **12° and wider** | 34 segments at M42/36° teach Orion whole; 6 at 12° already orient; at 8° the released pages stay untouched |
| Names | **12° and wider** | 1–6 names per page, always on visible figure ink |
| Boundaries | **18° and wider** | dotted reconstructed boundaries earn their ink where neighbouring constellations enter (32–832 pieces on the 18° pages); below 18° they mostly repeat the title's message |

- **The released 1–8° pages keep exactly their shipped ink** — the
  M31 8° reference remains byte-identical. The close fields are for
  observing detail and already carry the region's name in the title;
  the geography layers exist for regional orientation, which begins
  at the regional steps. (The studied 8° overlays were quiet too —
  the Andromeda chain beside M31 genuinely orients — so extending the
  layer closer stays open as a future, separately justified change.)
- Rejected: forcing all three layers at every scale (boundary ink at
  8° repeats the title), and rank-filtering names at 900×700 (counts
  never exceeded 6; rank is bundled anyway for smaller windows later).

## Name placement and deterministic overlap policy

A constellation is named when its figure leaves ink on the page, with
the label centred at the **centroid of the figure's visible, sampled
ink** — deterministic, language-neutral (Latin names, the atlas
convention; translations deliberately stay out of the data model),
and self-adjusting so the name always sits on the visible part of its
constellation. The rejected alternative — the source's fixed label
anchors, drawn only when the anchor is on-page — loses ORION on
**every** M42-centred field (its anchor at dec +13 lies off all pages
centred at M42's dec −5.4°): measured 0 names across the whole M42
journey versus 1–5 under the selected policy.

Overlap rules, initial and deterministic: the title block draws after
the geography and therefore always wins; names may clip at page edges
(honest position over pretty placement); name-versus-name collision
avoidance is deferred with the existing label-collision work. Figure
lines pass under star ink and DSO symbols by draw order.

## Layer control

**No toolbar layer control this sprint.** The scale policy alone
achieves the acceptance journey, and reset semantics stay untouched.
Rejected for now rather than forever: a single restrained toggle
remains the natural extension if close-field geography proves wanted;
a general layer framework is explicitly not justified by one layer.

## Rejected sources

- **Stellarium "modern" skyculture lines** — GPL-2.0 data whose MIT
  re-licensing rests on a maintainer-thread comment by its author
  (Stellarium discussion #790); the licence chain is informal where
  d3-celestial's BSD-3 is explicit. Rejected on licensing hygiene.
- **Sky & Telescope's own figure set (`modern_st`)** — the convention
  is proprietary to S&T and not freely redistributable.
- **iau.org boundary/data text files** — the IAU pages state CC BY 4.0
  for the chart images but carry no explicit licence for the data
  files themselves; per this sprint's rule, unverifiable
  redistribution terms disqualify a bundled source.
- **VizieR VI/49 `bound_20.dat`** (the pre-interpolated J2000
  boundary) — authoritative and it would remove our interpolation
  step, but its ReadMe carries no licence statement. Its corner-set
  lineage still reaches us through d3-celestial's BSD-3 distribution,
  and #64's own interpolation reproduces the fidelity with a proven
  tolerance.

## The acceptance journey, made concrete for #64/#65

From a fresh launch: search `m 42`, zoom out. At 12° the Sword and
Belt carry Orion's figure and the name ORION appears on its visible
ink; at 18° the figure's shoulders and legs frame the page; at 36°
the whole of Orion stands named around the searched, still-centred
M42, with MONOCEROS, ERIDANUS, LEPUS, and CANIS MAJOR entering at the
edges and dotted boundaries separating them. The study pages
`m42-08/18/36deg.png` are the reference renderings; #65's tests
assert figure ink, the ORION label, and untouched 8° pages against
this policy.

## Recorded source erratum

Found while wiring genitive search (#114, Codex review PR #119): the
source's `gen` field for **Crux** repeats the nominative ("Crux");
the Latin genitive is **Crucis**. The pack generator records this as
an explicit erratum - the corrected value is carried, declared in
the manifest (`erratum.genitive.Cru`) and the notice, and the
generator refuses to run if the source stops matching the recorded
wrong value, so the erratum can never silently outlive its cause.

## Consequences

- #64 productionizes the study's proven reconstruction
  (`PrecessionB1875` + `BoundaryReconstruction`) into the import
  pipeline with the same 1′ tolerance proof, and builds the bundled
  resource with manifest/notices and the bounded query; #65
  implements exactly the rendering and scale policy above and nothing
  more; the study tool (`make constellation-study`) stays as the
  reproducibility path for every number and page in this decision.
- LICENSING.md gains the BSD-3-Clause d3-celestial layer beside the
  existing Tycho-2 (CC BY-NC 3.0 IGO) and OpenNGC (CC-BY-SA-4.0)
  entries; the packaged application's licence position is unchanged
  (BSD-3 is more permissive than both).
