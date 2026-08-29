# Decision: the regional zoom range and scale policy

Decided 2026-08-29 for Sprint 6, "Reveal the wider sky" (issue #54),
from rendered evidence and measurements over the real catalogue,
assembler mathematics, projection, and renderer, with symbol sizes
measured at the renderer's exact viewport scale (`make regional-study`;
representative charts committed under `docs/studies/regional-zoom/`).
Candidate fields 12°, 18°, 24°, and 36° were studied over M31, M42,
M45, M13, and the Polaris field at the released 900×700 aspect and
V 8.0.

## Measurements (per chart at 900×700, V ≤ 8.0)

| Target | Field | Stars drawn | DSOs drawn today | Under the regional policy | Priority-1 labels | Tiles | Query+render |
|---|---:|---:|---:|---:|---:|---:|---:|
| M31 | 12° | 139 | 11 | (unchanged) | 3 | 3 | ~11 ms |
| M31 | 18° | 322 | 29 | (unchanged) | 3 | 5 | ~10 ms |
| M31 | 24° | 571 | 143 | **4** | 2 | 8 | ~11 ms |
| M31 | 36° | 1,252 | 303 | **6** | 3 | 9 | ~10 ms |
| M42 | 36° | 1,510 | 241 | **10** | 2 | 8 | ~9 ms |
| M13 | 36° | 880 | 256 | **2** | 2 (1 at exact scale) | 9 | ~5 ms |
| Polaris | 36° | 1,047 | 145 | **1** | 0 | 12 | ~4 ms |

Gnomonic radial linear scale (centre = 1.000): horizontal edge /
corner — 12°: 1.011/1.018 · 18°: 1.025/1.040 · 24°: 1.045/1.073 ·
36°: 1.106/**1.169**.

Performance is a non-issue at every candidate: warm query + assembly +
render stays around 10 ms, and the widest polar view touches 12 tiles.

## Decisions

### Field-width sequence: extend to 12°, 18°, 24°, 36°

The supported sequence becomes **36, 24, 18, 12, 8, 6, 4, 3, 2, 1**
degrees. Zooming out from a searched target progressively reveals its
neighbourhood; at 36° the pages genuinely deliver the promise — the
M31 study chart shows Cassiopeia and the Perseus Milky Way emerging
around Andromeda, with M33 and M76 in place.

**Maximum 36°, with the distortion stated and accepted:** the radial
scale at the extreme corner is 1.169 — a 17% stretch confined to the
corners, at which the studied pages remain visually uniform and
recognizably the same atlas. The horizontal edge sits at 1.106.

**Projection-change threshold:** a corner radial scale of **1.25**
(reached near a 44° field at this aspect) is the declared boundary
beyond which the gnomonic chart may not honestly go; wider "sky
quarter" views require a different projection (the alternative the
architecture deferred since Sprint 1). 36° stays comfortably inside.

### Star depth: the user's limit is preserved at every scale

The user's limiting magnitude carries unchanged through every zoom
step, and reset still restores exactly M31, 8°, V 8.0. Evidence: V 8.0
at 36° draws 900–1,500 stars on the page — the density of a good
naked-eye atlas, with Milky Way structure visibly emerging, and
nowhere approaching clutter. Rejected: a scale-dependent effective
limit (it would break the by-construction agreement between toolbar
readout, title block, and rendered stars for no measured benefit) and
clamp-or-suggest schemes (added interaction complexity the evidence
does not demand).

### The regional detail policy for DSOs (fields wider than 18°)

At 12° and 18° the released behaviour needs no change: the pages stay
readable (29 drawn symbols at M31/18° read as light seasoning).

At 24° and 36° the practical minimum-symbol clamp becomes the clutter
engine: it inflates hundreds of sub-pixel faint galaxies into
6-pixel speckle (303 drawn at M31/36°), and co-located Messier labels
collide (the M31/M32/M110 stack). Therefore, **at fields wider than
18°**:

- a DSO symbol draws only at its **true projected size**, when that
  reaches the practical minimum — no clamp inflation (true size is
  computed with the renderer's exact viewport scale; the study's
  original linear approximation sat 3.3% off at 36° and was corrected
  after the PR #58 review);
- **priority-1 (Messier) objects are always drawn**, clamped when
  necessary;
- **a searched target whose type has an established chart symbol is
  always drawn and always labelled**, clamped when necessary,
  regardless of its priority or size — the chart may never title
  itself by a *drawable* object it does not show. Types that
  deliberately have no symbol (stellar records, associations, novae,
  `OTHER`) still recenter and title honestly, exactly as at the
  released fields; the guarantee does not force symbols into
  existence (PR #58 follow-up). The target's **stable catalogue
  identity** (not merely its display label) travels in the view
  state to the scene and the rendering policy; #55/#56 thread it
  beside the existing target label (PR #58 review finding 1);
- **labels attach only to objects drawn at true size, plus the
  target** — which dissolves the measured label collisions naturally
  (M32 and M110 fall silent at 36° while M31, M33, and M42 keep
  their names; at exact scale even M92's 14′ symbol drops its label
  at 36° — precisely why the target exemption is required).

Measured effect: M31/36° goes from 303 drawn symbols to 6; M13/36°
from 256 to 2; the pages match the chart conventions' own
detail-by-scale intent ("20–30°: constellation-scale relationships
and notable DSOs"). Rejected: applying the policy from 12° (the 18°
page is already good and would be over-thinned to 3 symbols), and
removing the clamp without the priority-1 exemption (sub-pixel
objects become 1-pixel noise indistinguishable from faint stars).

### Nebulae and symbols: no further change

The lighter-grey nebula boxes remain right through 36° (the Orion
study pages), and no symbol redesign is justified by the evidence.

## Consequences

- #55 implements the extended sequence with target preservation;
  #56 implements exactly the regional detail policy above and nothing
  more; the released ≤ 18° pages and the M31 reference image remain
  untouched until then and byte-identical at 8°.
- The study tool (`make regional-study`) stays in the repository as
  the reproducibility path for these numbers and charts.
