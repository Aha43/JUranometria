# Sprint 5 review handover

Prepared 2026-08-29 for the pre-release Codex review of Sprint 5,
"Build the local sky" (milestone 5, issues #41–#43, PRs #44, #45, and
the PR closing #43). Written before that final PR merges. After
review the plan is: merge, close the milestone, release 0.5.0.

Read the earlier handovers first; this document covers only what
Sprint 5 changed.

## What Sprint 5 delivered

The atlas covers the whole sky, offline:

1. **#41** — the tiled-catalogue contract, decided by measurement
   (V ≤ 8.0 built-in depth, `radec-grid-30`, plain-properties
   manifest with per-file checksums, home-tile uniqueness plus padded
   conservative selection). The sprint review corrected the tile
   selector to a provably complete boundary-sampling rule after a
   south-pole counterexample.
2. **#42** — the generated `bright-sky` pack: 45,630 stars and 13,371
   DSOs of every OpenNGC type in 72 tiles (~2.5 MiB; jar ~1.2 MiB).
   The reviews reshaped the data principle: unknown values stay
   explicitly empty (separate vmag/bmag columns), positioned objects
   without photometry ship, cross-reference aliases split correctly,
   generation is clean-room with an ownership guard.
3. **#43** — `TiledCatalogue` behind the unchanged `Catalogue`
   interface: manifest-validated at load, per-tile SHA-256 verified at
   first read, only intersecting tiles read per query, cached
   thereafter, immutable deterministic results, duplicates impossible.
   `SceneAssembler.allSky` removes the regional coverage restriction
   (page height bounded only by a 60° projection-sanity corner cap);
   the M31-specific coverage constant is gone from navigation, with
   the manifest as the source of truth. Search spans the whole pack.
   The Sprint 3 m31 resource, its loader, and its importer retired.
   The loader maps the pack's explicit unknowns into the chart model
   (NaN magnitude allowed as honestly unknown; nominal display
   minimums for absent dimensions, documented as display decisions).

## Measurements (the numbers the sprint asked for)

| What | Measured |
|---|---|
| Manifest parse + first M31 scene | 33 ms |
| Complete pack load (59,001 objects, for search) | 67 ms |
| First whole-sky search ("M42", includes wiring warm-up) | 60 ms |
| Warm scene query (Orion, cached tiles) | 0.23 ms |
| Heap with full pack + search index resident | 27 MiB |
| Packaged jar with the whole sky | 1.18 MiB |

## Worth extra scrutiny

1. **The drawn-types decision.** The renderer draws and labels only
   `DsoType.GALAXY`; all other types load, search, and recentre. This
   keeps the M31 reference chart byte-identical (NGC 206 and the
   NGC 317 pair sit inside the default frame and would otherwise
   appear) and defers the chart-conventions symbols to issue #46. The
   consequence: an M42 chart is a complete, correct Orion star field
   with no nebula symbol at its centre. Judge whether that is
   acceptable for 0.5.0 or whether #46 must precede the release.
2. **The stale title block.** Recentred charts still read
   "M31 · Andromeda Galaxy region" over, say, Orion coordinates — a
   Sprint 4 limitation now far more visible. The centre coordinates
   beneath it are always correct. Candidate quick follow-up
   (title-follows-search-target), deliberately not smuggled into this
   sprint.
3. **Integrity-check timing.** Tile checksums verify at first read,
   not all at startup (startup stays 33 ms; full verification happens
   naturally as tiles load). A corrupt tile therefore surfaces on
   first navigation into it. Judge whether lazy verification is
   acceptable or a startup sweep is worth its cost.
4. **The loader's display mappings.** Absent dimensions become a
   nominal 1.0′, absent minor mirrors major, absent angle becomes 0 —
   documented as display decisions in `TiledCatalogue`. NaN magnitude
   is now a legal chart-model value (renderer never reads DSO
   magnitude). Check no code path assumes finite DSO magnitude.
5. **All-sky assembler mode.** `fits` is constant-true and the page
   cap comes from the 60° projection-sanity corner limit. Check the
   regional mode (kept for future partial packs) cannot be
   accidentally wired, and that the projection cap is generous but
   sound.
6. **Search memory residency.** Search construction loads all 72
   tiles (67 ms, 27 MiB heap total). Fine today; a deeper pack would
   revisit this (the manifest's row totals give the early warning).

## Sprint review answers

- **Honestly usable across the whole declared coverage, offline?**
  Yes. Any name or coordinate under the declared all-sky coverage
  recentres onto a complete V ≤ 8.0 chart; the application performs no
  network access; integrity failures name the resource instead of
  presenting sparse sky.
- **Does a scene query read only what it needs?** Yes, verified by
  test: the default M31 view reads exactly one tile; an RA-wrap field
  reads its two; a polar cone reads the top band; tiles cache after
  first read and repaint touches nothing.
- **Are version, depth, integrity, licensing machine-checkable?**
  Yes: `manifest.properties` carries format version, coverage, depth,
  tiling scheme, source versions, licences, and per-file SHA-256s;
  the loader validates the version and scheme and verifies checksums;
  the packaging tests guard notices.
- **What do size and latency say about deeper data?** Everything is
  cheap at this depth: 2.5 MiB, 67 ms, 27 MiB heap. The measured
  V ≤ 10 CSV (14.1 MiB, ~360k rows) would multiply those by roughly
  five to seven — still plausibly fine, but that is the point where a
  startup-loading search index and lazy verification start to matter,
  and optional packs (rather than built-in depth) remain the right
  shape because the UI cannot display fainter than V 8 today.
- **Which data absence is now most conspicuous?** Ranked: (1) the
  missing symbols for non-galaxy types — M42 recentres perfectly and
  then isn't drawn (#46); (2) star names — "Betelgeuse" still finds
  nothing while its field is one search away; (3) constellation
  geography, which the whole-sky atlas now visibly lacks the moment
  one leaves a familiar field; (4) deeper stars, least urgent, since
  the UI's own limit is the boundary.

## Process expectations

As before. The M31 reference image is intentionally byte-identical
through the entire sprint — reproduced from the new pack rather than
the retired resource, which is the migration's proof. Flag any diff
you can produce from it as a finding.
