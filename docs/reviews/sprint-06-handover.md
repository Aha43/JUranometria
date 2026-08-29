# Sprint 6 handover — Reveal the wider sky

Written 2026-08-29 by the coder for the independent pre-release review
of 0.6.0, following the pattern of the earlier sprint handovers. The
sprint's issues: #54 (regional zoom decision), #55 (centre-preserving
zoom into regional fields), #56 (regional detail policy), #57 (this
finish).

## What Sprint 6 delivered

- **The field-width sequence extends to 36, 24, 18, 12, 8, 6, 4, 3,
  2, 1 degrees**, decided by measurement over the real catalogue and
  renderer (`docs/decisions/regional-zoom.md`, `make regional-study`).
  Zooming out from a searched object progressively reveals its
  neighbourhood; reset still restores exactly M31, 8°, V 8.0.
- **The searched target's stable catalogue identity rides the view
  state** beside its display label — atomic by construction (both
  present for catalogue targets, both absent for coordinate views;
  half-states are rejected at the record) — through every zoom and
  magnitude transition into `ChartScene`, where the rendering policy
  consumes it.
- **A regional detail policy in one testable object**
  (`juranometria.render.RegionalDetailPolicy`): at fields wider than
  18°, DSO symbols draw only at their true projected size (exact
  viewport scale, no practical-minimum clamp inflation); Messier
  objects always draw, clamped when necessary, but clamp-inflated
  symbols fall silent; the searched target is always drawn and
  labelled when its type has an established chart symbol; symbol-less
  types are never forced into existence. 18° and narrower keep the
  released behaviour exactly; the practical-minimum constant and the
  Messier label rule live in the policy as the single source of truth.
- **The user's magnitude limit is preserved at every scale** — the
  toolbar readout, title block, and drawn stars still agree by
  construction, verified across the full journey by test.

## The complete journey (acceptance path, exercised)

From a fresh launch: searching `m 42` recentres on NGC 1976 with
title "M 42 · Great Orion Nebula region"; zooming out steps 12° → 18°
→ 24° → 36° with the centre, title, and identity identical at every
step (asserted by `aSearchedTargetSurvivesTheWholeRegionalZoomJourney`);
the magnitude limit can be stepped at regional fields and carries
through further zooming with the target; every step reverses; reset
restores the exact default state. The same journey was rendered
through the real catalogue by the measurement harness and inspected
visually.

## Representative sky verification (both themes)

All pages rendered at 900×700, V 8.0, 36° — the widest field — and
inspected visually. Committed under `docs/studies/regional-detail/`
(regenerate with `make regional-study`; the pre-policy study set from
the #54 decision remains untouched under `docs/studies/regional-zoom/`
for comparison):

| Page | Class | What was inspected |
|---|---|---|
| `m42-36deg.png` | equatorial | whole Orion; Belt/Sword nebulae at true size; M78's clamped box correctly silent; M42/M43 keep names |
| `m45-36deg.png` | ecliptic north | Pleiades labelled; Hyades and California Nebula at true size, unlabelled non-Messier |
| `m13-36deg.png` | northern | 256 symbols → 2: M13 labelled, M92 clamped and silent |
| `polar-36deg.png` | high declination | clean star field, one true-size cluster; 12 tiles converge at the pole |
| `lmc-36deg.png` | far southern | the LMC's true 10.8° ellipse (the object the manifest extent margin exists for) with its true-size inner nebulae; SMC entering the corner; 508 in-frame symbols → 6 |
| `rawrap-36deg.png` | RA wrap | centre 23h57.8m; the star field crosses RA 0 with no seam |

The study pages carry no searched target, so sub-minimum objects
honestly vanish there (NGC 7793's 10′ ellipse in the rawrap page).
The searched view of the same sky was rendered through the real
search path and keeps the target clamped and labelled — including the
hardest case, M57's 1.4′ ring at the exact centre of a 36° Lyra/Cygnus
Milky Way page.

Both application themes were launched and inspected: light and dark
chrome recolor the toolbar and icons only; the chart paper stays the
chart's own white-and-ink palette in both, and the toolbar readout
matches the title block. (Screenshots were reviewed, not committed —
`make run`, and `java -cp ... juranometria.app.JUranometriaMain
--dark` for the dark chrome.)

## Measurements (compared with Sprint 5)

| Measure | Sprint 5 | Sprint 6 |
|---|---:|---:|
| Manifest parse + first M31 scene | 33 ms | 35–42 ms |
| Complete pack load (59,001 objects, for search) | 67 ms | 107–135 ms |
| First whole-sky search | 60 ms | 69–78 ms |
| Warm `SceneAssembler.assemble` end to end (query + scene construction), 8° | — | 0.5 ms |
| Warm `SceneAssembler.assemble` end to end, 36° (3,439 stars, 446 DSOs) | — | 0.5–0.8 ms |
| Warm catalogue query alone, 36° | — | 0.4–4.4 ms |
| Warm render, 36° | — | 2–5 ms |
| Heap with full pack + search resident | 27 MiB | 23 MiB |
| Packaged jar | 1.18 MiB | 1.19 MiB |

**Method** (Codex review, Sprint 6 finding 1): the regional rows come
from `make regional-study`, which since this sprint times the real
`SceneAssembler.assemble` end to end (`e2e` column) alongside its own
catalogue-query (`qry`), scene-construction (`scn`), and render
(`rnd`) components — one warm run per row, ranges reported over three
tool runs across the M42, M31, and LMC pages. The end-to-end figure
is faster than some standalone `qry` values because it runs last in
each row, fully warm — the honest statement is: **with tiles cached,
query plus scene construction stays under a millisecond; a
cache-cold regional query costs single-digit milliseconds**. The
startup rows come from a fresh-JVM harness as in Sprint 5; those two
rows are noisier than Sprint 5's single-run numbers (ranges over
three runs), no loader code changed this sprint, and the totals
remain imperceptible at startup.

## Worth extra scrutiny

1. **Target-exemption identity matching** is exact string equality on
   the catalogue id (`ChartScene.targetIdentity` vs `DeepSkyObject.id`).
   Search always produces ids from the same pack, so they agree today;
   nothing enforces that coupling across future packs.
2. **Co-located Messier labels still collide slightly** — M42/M43 at
   36° overlap by a few pixels. The policy dissolved the measured
   M31/M32/M110 pile; general collision avoidance stays deliberately
   deferred.
3. **Search alias gaps**: `lmc` finds nothing (the pack knows "Large
   Magellanic Cloud" and "Nubecula Major"); named stars ("Betelgeuse")
   still find nothing. Both are catalogue-content gaps, not search
   bugs, and are Sprint 7 candidates.
4. **The study tool's policy columns run without a searched target**,
   so they measure the policy's floor, not the target exemption; the
   exemption is covered by renderer and policy tests instead.
5. **`RegionalStudyMain` gained two targets** (lmc, rawrap) so every
   committed page reproduces from `make regional-study`; the decision
   table's original five targets and their numbers are unchanged.

## Sprint review answers

- **Does progressive zoom-out teach where a searched object sits?**
  Yes — the M42 journey ends with the whole of Orion, Belt to
  Betelgeuse to Rigel, on one page with the winter Milky Way emerging;
  M31's 36° page shows Cassiopeia and the Perseus arm around
  Andromeda with M33 in place. The intermediate steps (12°, 18°, 24°)
  read as a continuous approach rather than discrete scenes.
- **Where does gnomonic distortion become noticeable, and is 36°
  comfortable?** The corner radial scale is 1.169 at 36° (edge 1.106)
  — measurable, visible only by comparing corner star spacing
  deliberately, and confined to the extreme corners. The pages remain
  recognizably the same atlas. The declared honesty boundary (corner
  scale 1.25, ≈44° at this aspect) leaves 36° comfortably inside;
  wider views await a different projection, as deferred since Sprint 1.
- **Is the magnitude policy understandable and honest across scales?**
  Yes, because there is nothing to understand: the user's limit is the
  limit, at every field, stated identically by toolbar and title block
  and enforced by the renderer's cull. V 8.0 at 36° draws 900–1,500
  stars — good naked-eye-atlas density, nowhere near clutter — so no
  scale-dependent limit was needed, and none was added.
- **Which symbols or labels become too busy first, and what was
  deferred?** Without the policy, sub-pixel galaxy speckle (303 drawn
  at M31/36°) and co-located Messier labels were the clutter engines;
  the policy removes both. What remains busiest is the pair of
  co-located true-size labels (M42/M43); full label collision
  avoidance, panning, constellation geography, and the NGC 206-style
  objects-inside-objects judgement remain deliberately deferred.
- **Do the measurements support the tiled pack and synchronous
  assembly at regional scale?** Comfortably: the real
  `SceneAssembler.assemble` completes in 0.5–0.8 ms warm at the
  widest field (cache-cold queries single-digit milliseconds, the
  17-tile LMC worst case included), rendering in 2–5 ms, heap flat
  at 23 MiB. The whole warm query-to-pixels path is under 6 ms.
  Nothing in the regional range motivates asynchrony or a different
  tiling.
- **Is constellation geography now the highest-value next layer?**
  Yes, and regional use sharpened the case: the 36° pages are
  constellation-scale by the chart conventions' own table, and the
  star patterns are plainly visible but nameless. Constellation lines
  and names (plus the named-star and common-alias search gap, which
  the same reference data would largely close) would let the wide
  pages teach the sky rather than merely show it. No more fundamental
  need surfaced — performance, honesty, and distortion all measured
  comfortable.

## Process expectations

The established pattern: this handover accompanies the open sprint PR;
the independent Codex review lands as
`docs/reviews/sprint-06-codex-review.md`; findings are fixed on the PR
with regression tests; both documents are committed with the fixes;
then merge, close milestone 6, and cut 0.6.0.
