# Sprint 7 handover — Give the wider sky its geography

Written 2026-08-29 by the coder for the independent pre-release review
of 0.7.0, following the established handover pattern. The sprint's
issues: #63 (sources and chart contract), #64 (reproducible bundled
geography), #65 (scale-sensitive rendering), #66 (this finish).

## What Sprint 7 delivered

- **Constellation geography with a verified licence chain**: all three
  layers from d3-celestial (BSD-3-Clause, pinned commit, SHA-256
  everywhere), with the provenance recorded and shipped — Delporte's
  1930 IAU boundaries via the Davenhall & Leggett corner lineage; the
  figure convention of the IAU/S&T CC BY 4.0 charts; IAU Latin names.
  Sources with unverifiable redistribution terms (iau.org data files,
  VizieR VI/49, Stellarium's GPL lines) were rejected at the design
  gate, with reasons recorded.
- **Boundaries reconstructed, never bent**: edges sampled along their
  constant-RA/Dec B1875 arcs (IAU-1976 precession) at import, proven
  within **0.0654′** of the true boundary against the decision's 1′
  tolerance; straight J2000 corner chords — off by up to 2.18° — were
  measured and rejected. The generator refuses shapes that fit
  neither constant coordinate, and the loader refuses foreign
  coordinate frames and failed checksums.
- **Scale-sensitive rendering through one policy object**
  (`GeographyDetailPolicy`, enforced at assembly *and* renderer):
  figures and Latin names from 12°, dotted boundaries from 18°,
  nothing at the released 1–8° fields — the M31 reference remains
  byte-identical. Geography draws beneath stars, symbols, labels, and
  the title block; names sit at the centroid of each figure's visible
  ink, sorted deterministically.
- **No toolbar layer control**, as decided: the scale policy alone
  achieves the journey; reset semantics are untouched, and the
  decision records the control as rejected-for-now, not forever.

## The complete journey (acceptance path, exercised)

From a fresh launch: searching `m 42` recentres on NGC 1976; zooming
out, Orion's figure and the name ORION arrive exactly at 12°,
boundaries exactly at 18°, and at 36° the whole of Orion stands named
around the still-centred, still-titled M42 with MONOCEROS, ERIDANUS,
LEPUS, and CANIS MAJOR at the edges — asserted end to end by
`theM42JourneyGainsOrionExactlyAtThePolicyThresholds` through the real
Atlas, including that names attach only to constellations with figure
ink present and that reset restores the exact M31 8°/V 8.0 default
with its decided geography default (none). There is no layer
visibility to change — the documented simpler result.

## Representative sky verification (both themes)

Eight product pages committed under
`docs/studies/constellation-rendering/` (regenerate with
`make regional-study`, which renders the real assembled scenes; the
Sprint 6 pages under `docs/studies/regional-detail/` are regenerated
from the same corrected path), all visually reviewed:

| Page | What was inspected |
|---|---|
| `m42-12/18/36deg.png` | ORION arrives at 12° over the Sword; boundaries at 18°; the whole constellation at 36° with named neighbours |
| `m31-36deg.png` | Andromeda's chain through M31's ellipse, ANDROMEDA named beneath; Perseus, Triangulum with M33, the Great Square of Pegasus |
| `m45-36deg.png` | TAURUS on the Hyades beside a labelled M45; Perseus, Auriga, Aries |
| `rawrap-36deg.png` | Sculptor named across RA 0 with no seam; Piscis Austrinus figure |
| `polar-36deg.png` | the reconstructed +88° arc circling Polaris; URSA MINOR, Draco, Cepheus, Camelopardalis |
| `lmc-36deg.png` | DORADO, VOLANS, PICTOR, HYDRUS around the LMC's true ellipse; deep-southern boundaries curving correctly |

Themes: both chromes were launched and inspected, and a test proves
the point structurally — a 36° geography page renders byte-identically
under FlatLight and FlatDark, because the chart owns its palette.

## Measurements (compared with Sprint 6)

| Measure | Sprint 6 | Sprint 7 |
|---|---:|---:|
| Catalogue manifest + first M31 scene | 35–42 ms | 22–24 ms (of which first scene 13–15) |
| Geography load + checksum verify (new) | — | 36–43 ms |
| Total to first chart | 35–42 ms | **59–67 ms** |
| Complete pack load (for search) | 107–135 ms | 93–118 ms |
| First whole-sky search | 69–78 ms | 60–80 ms |
| Warm `SceneAssembler.assemble` end to end, 36° | 0.5–0.8 ms | **2.1–3.0 ms** (geography queries included: 59 figure + 562 boundary pieces + 7 names at M42) |
| Warm render, 36° | 2–5 ms | **4–20 ms** (geography drawing included) |
| Heap with everything resident | 23 MiB | 25 MiB |
| Packaged jar | 1.19 MiB | **1.41 MiB** |

**Method**: startup rows from a fresh-JVM harness, two runs, as in
Sprints 5–6; the warm rows discard the first (JIT/font-warm-up) render
of ~250 ms and report the following runs; the study tool's `qry`,
`scn`, `rnd`, and `e2e` columns (one warm run per row,
`make regional-study`) are the reproducible per-page path, with `rnd`
timing the real assembled page, geography included. The honest
statement: **geography costs ~40 ms once at startup (checksummed
load) and ~2 ms per widest-page assembly; warm renders are generally
single-digit milliseconds with observed outliers to 20 ms — so warm
query-to-pixels is generally under 10 ms, bounded by ~23 ms in the
worst observed warm case. Comfortable for synchronous assembly; not a
hard real-time bound, and not claimed as one.**

## Worth extra scrutiny

1. **Serpens' split parts share one centroid**: if both parts of
   Serpens ever leave figure ink on one page, the single name sits at
   the combined centroid — potentially over Ophiuchus. At 900×700 and
   36° the parts have not co-occurred in review; a per-part naming
   refinement is deferred with the collision work.
2. **Name/name collisions are unhandled by design** (initial policy:
   deterministic placement, title block always wins, honest edge
   clipping). Counts stayed ≤ 15 names per page in review.
3. **The Sprint 6 study pages changed**: `make regional-study` now
   renders the real assembled scene, so `docs/studies/regional-detail/`
   gained the geography layers; the pre-geography originals live in
   git history. The Sprint 6 decision's DSO-policy counts are
   unaffected.
4. **First render after launch costs ~250 ms** (JIT and font warm-up,
   measured in the harness; not user-visible as jank since it is the
   first paint). Unchanged in kind from earlier sprints, now stated.

## Sprint review answers

- **Does geography materially improve understanding of where a
  searched object sits?** Yes — decisively. The 36° M42 page went
  from a nameless star field with symbols to a page where Orion is
  legible at a glance and M42's place in the Sword is obvious; the
  same for Andromeda's chain through M31 and Dorado around the LMC.
  The review of #65 called the layer's restraint its strongest
  quality: geography explains the sky while stars and DSOs keep
  visual authority.
- **Are boundaries, figures, and names rendered without false
  authority?** Yes, and the distinction is enforced in writing and in
  packaging: the decision, README, chart conventions, and the bundled
  NOTICE all state that figures follow the IAU/S&T chart convention
  and are not an IAU standard (a packaging test asserts the NOTICE
  wording), while boundaries and names are the IAU's own.
- **Where do the layers help, and where would they clutter?** Figures
  and names earn their ink from 12° (6 segments already orient) and
  boundaries from 18°; below that the title already names the region
  and boundary ink would repeat it. At 36° the counts stay modest
  (≤ 62 figure segments, ≤ 15 names). The measured clutter risks —
  polar boundary density and the Magellanic name cluster — were
  reviewed and stayed legible.
- **Are wrap, polar, and crossing cases geometrically complete?**
  Yes, by construction and by test: segments subdivide along the sky
  with a real line-versus-page intersection per piece (the
  both-endpoints-off-page regression), boundaries are true
  reconstructed arcs (the +88° circle around Polaris), and Sculptor's
  border crosses RA 0 without a seam on the committed page.
- **Is the source, frame, licensing, and build clear to a future
  maintainer?** The decision records sources, pinned commit, SHA-256,
  licences, and rejected alternatives; the pack manifest records
  frame, counts, method, tolerance, and per-file checksums, and the
  loader enforces frame and checksums; `make import-constellations`
  regenerates byte-identically from `scripts/
  download-constellation-sources.sh`. LICENSING.md carries the layer.
- **Do measurements support synchronous scene assembly?** Comfortably:
  +2 ms assembly and generally single-digit-millisecond rendering at
  the widest field (observed warm outliers to 20 ms), heap +2 MiB,
  jar +0.22 MiB. Nothing motivates asynchrony, an index, or tiling
  for 13,929 segments.
- **Is star naming/Bayer–Flamsteed the highest-value next sprint?**
  Yes. The wide pages now name the constellations but the bright
  stars anchoring the figures are anonymous — Betelgeuse, Rigel, and
  Polaris are prominent dots at the corners of named figures, and
  search still cannot find them (the known `Betelgeuse`/`lmc` alias
  gap). Star names plus common-alias search would complete the
  orientation story the geography began. No more fundamental
  cartographic need surfaced: distortion, honesty, and performance
  all measured comfortable; the deferred list (collision engine,
  grids, pan, artwork) remains deliberate.

## Process expectations

The established pattern: this handover accompanies the open sprint PR;
the independent Codex review lands as
`docs/reviews/sprint-07-codex-review.md`; findings are fixed on the PR
with regression tests; both documents are committed with the fixes;
then merge, close milestone 7, and cut 0.7.0.
