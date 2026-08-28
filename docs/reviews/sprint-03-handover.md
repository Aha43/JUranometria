# Sprint 3 review handover

Prepared 2026-08-29 for the pre-release Codex review of Sprint 3,
"Local catalogue foundation" (milestone 3, issues #23–#26 plus the
open-source issue #28, PRs #27, #29, #30, #31, and the PR closing
#26). Written before that final PR merges. After review the plan is:
merge, close the milestone, release 0.3.0.

Read the Sprint 1 and 2 handovers first; this document covers only
what Sprint 3 changed.

## What Sprint 3 delivered

The atlas now runs on real, reproducible catalogue data end to end:

1. **#23 (decision)** — `docs/decisions/catalogue-sources.md`: Tycho-2
   (CDS I/259, CC BY-NC 3.0 IGO) for stars with Johnson V derived by
   the ReadMe's own BT/VT transformation; OpenNGC v20260501
   (CC-BY-SA-4.0) for NGC/IC objects; a 10° cone around M31 to
   V ≤ 10.0; a pinned, checksummed, deterministic import contract;
   tiling/binary deferred with reasons.
2. **#28 (licensing)** — MIT `LICENSE` (unaltered text) for code and
   docs; `LICENSING.md` describing every bundled-data exception,
   including that the packaged application is redistributable
   non-commercially only while it bundles the Tycho-derived resource.
3. **#24 (import)** — `juranometria.tool`: an offline plain-Java
   importer (`make import-catalogue`) with pinned SHA-256 checks over
   all 24 raw inputs, explicit normalization rules with provenance
   counts, byte-identical determinism, and generated NOTICE files.
   Produced `src/resources/catalog/m31/`: 3,204 stars, 47 galaxies.
   The issue-level Codex review found two P2s (a duplicate TYC id
   from a resolved Hipparcos double; the unpinned license input),
   fixed with a main-catalogue-wins component policy plus whole-file
   uniqueness guards, and a pinned license checksum.
4. **#25 (loading)** — `BundledCatalogue` behind the unchanged
   `Catalogue` interface; fixture removed; renderer labels only
   Messier-priority objects by their Messier alias now that dozens of
   galaxies exist.
5. **#26 (seam)** — `SceneAssembler` at the UI boundary: every view
   state or window-size change derives a bounded cone query whose
   radius is the exact gnomonic corner distance at the current aspect
   ratio plus a 1.5° object-extent margin (M31's 88.9′ semi-major is
   the largest), assembles a complete immutable `ChartScene`, and
   hands it to the component; painting renders the stored scene and
   never queries. This fixed a latent Sprint 2 bug: the old fixed 6°
   load could silently clip eligible objects in tall windows.

Tests grew from 70 to 97. The committed reference image changed once
(#25, real data — reviewed in that PR) and is byte-identical through
#26 by design: the corner-safe query only adds objects outside the
default frame.

## Worth extra scrutiny

1. **The resize/repaint contract** — `ChartComponent` assembles on
   `componentResized` and `setViewState`, and `paintComponent` skips a
   frame when the stored scene's geometry mismatches the component
   (a resize event is already queued). Check there is no path to a
   permanently blank chart (e.g., an event sequence where the resize
   event never arrives after a mismatch) and that the EDT-only
   assumption holds.
2. **Corner-radius mathematics** — `SceneAssembler.queryRadiusDegrees`
   uses tan/atan on the gnomonic plane. Verify the formula and the
   claim that aspect ratio cannot outrun it; also judge the fixed
   1.5° margin versus a per-object extent check.
3. **Query cost per interaction** — every zoom step, magnitude step,
   and resize filters 3,204 + 47 rows on the EDT. Measured cost is
   microseconds, and the issue forbade premature caching; confirm
   nothing (e.g., continuous resize streams) makes this user-visible.
4. **Data-edge honesty** — a very tall window at 8° needs ~8.9° + 1.5°
   margin, slightly beyond the 10° data region: the chart simply has
   no data past 10° and margins go empty. Judge whether that edge
   deserves a visible statement (title block or documentation) or is
   acceptable silence for this sprint.
5. **Duplicate-id policy** — main-catalogue-wins discards a resolved
   companion's separate photometry (one case in the region). Sanity-
   check the policy's astronomy and its provenance wording.
6. **The importer's trust boundary** — checksums pin the inputs, but
   the OpenNGC tag and CDS files live on external infrastructure.
   Confirm the failure modes (moved files, changed tags) fail the
   import rather than degrade it.

## Sprint review answers

- **Is normal chart use fully local?** Yes. The application performs
  no network access at any point; every query runs against immutable
  in-memory lists parsed once from bundled resources. The importer is
  the only network-touching code and is never run by the application.
- **Can every bundled row be traced?** Yes: pinned source URLs and
  versions, SHA-256 of all 24 raw inputs verified before transform,
  every normalization counted in the generated `PROVENANCE.md`, and
  re-running the import reproduces the files byte-identically.
- **Are magnitude notation and limits honest?** Yes: the stars carry
  Johnson V derived by the source's own published transformation
  (edge cases counted); the UI's "Stars to V 8.0" is true against a
  catalogue complete far deeper; the V ≤ 10.0 import depth leaves
  room for fainter steps without re-import.
- **Did real data reveal a need for tiling or binary formats?** No.
  3,204 rows parse in milliseconds and filter in microseconds; the
  regional scale produces no measurable pressure. Deferral stands,
  now backed by observed behaviour rather than estimation. The
  per-region output directory and the cone-query seam are the shapes
  a tiled future reuses.
- **Is the seam ready for Search and recenter?** Yes, by
  construction: `SceneAssembler` takes a centre today as a
  constructor argument; recentring is "make the centre part of the
  view state and pass it through" — one seam, no renderer or painting
  changes. Search resolves a name to a centre and does the same. The
  10° data region gives ±2° of honest recentring to test against
  before any wider import.

## Process expectations

As before: findings become issues or PR comments; user-visible work
needs a changelog entry; renderer-affecting changes require the
regenerated reference image to be inspected. The reference image was
reviewed at #25 (real data) and is intentionally unchanged by #26 —
flag any diff you can produce from it as a finding.
