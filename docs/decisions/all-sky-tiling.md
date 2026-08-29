# Decision: the tiled all-sky catalogue contract

Decided 2026-08-29 for Sprint 5, "Build the local sky" (issue #41).
This document settles the partition, the pack layout, the manifest,
the query contract, and the built-in magnitude depth that issues
#42–#43 implement — from measurements of the already pinned inputs,
not estimates.

## Measurements

All-sky star extracts from the pinned Tycho-2 inputs (I/259 main +
supplement-1), applying exactly the importer's rules from the Sprint 3
contract (mean-else-observed position, V = VT − 0.090(BT − VT), VT or
Hp fallbacks, drop on missing VT), sized in the existing 4-column CSV
row format:

| Depth | Rows | CSV size |
|---:|---:|---:|
| V ≤ 6.5 | 8,805 | 0.35 MiB |
| V ≤ 7.0 | 15,449 | 0.61 MiB |
| **V ≤ 8.0** | **45,630** | **1.80 MiB** |
| V ≤ 9.0 | 130,720 | 5.16 MiB |
| V ≤ 10.0 | 357,144 | 14.11 MiB |

The complete pinned OpenNGC dataset (release v20260501): 14,033 rows,
of which 11,544 are usable in the existing 10-column DSO row format
after the established drops (652 Dup, 10 NonEx-class, 0 without
positions, 1,827 without any V or B magnitude) — **0.59 MiB** of CSV.

Method: the measurement script applies the importer's parse rules to
the checksummed raw files in `imports/raw/`; the #42 generator will
reproduce these numbers as its row counts, which is the reproducible
check.

## Decisions

### Built-in depth: one complete bright pack at V ≤ 8.0

The UI exposes magnitude limits only through V 8.0, and the complete
bright sky at that depth is **1.80 MiB of stars plus 0.59 MiB of
DSOs — about 2.4 MiB with manifest and notices**. That ships as one
complete built-in pack. There is no core-plus-optional split and no
package manager: V ≤ 9 (5.2 MiB) and V ≤ 10 (14.1 MiB) are recorded
here as *possible future optional packs* whose cost is only justified
when the UI grows fainter magnitude steps; nothing in the contract
below prevents adding them as additional packs with their own
manifests.

Consequence for the current M31 resource: its V ≤ 10 regional depth
retires with it (#43). The visible chart loses nothing — the renderer
culls at the scene limit and the UI cannot exceed V 8.0.

### Partition: a fixed 30° RA/Dec grid (`radec-grid-30`)

Twelve 30° right-ascension columns by six 30° declination bands — 72
tiles named `r00-d0` (RA [0°, 30°), Dec [−90°, −60°)) through
`r11-d5`, band low edge inclusive, north pole clamped into `d5`.
At V ≤ 8.0 this averages ~630 stars per tile.

**Worked examples** (each mirrored by a test in `SkyTilingTest`):

- *Ordinary region:* the default M31 query (centre 10.68°, +41.27°,
  radius ≈ 7°) touches exactly `r00-d4`.
- *RA 0°:* a 5° cone at RA 0.5° selects `r00-d4` and its wrap
  neighbour `r11-d4`.
- *Tile boundary:* a 3° cone at Dec +29° selects `r00-d3` and
  `r00-d4`.
- *Pole:* a 2° cone at Dec +89° selects all twelve `d5` tiles and
  nothing below them.

**Rejected alternatives.** *HEALPix* — equal-area and elegant, but it
brings either a dependency or non-trivial hand mathematics, and at
45k rows nothing needs equal area. *A single un-tiled file* — honestly
viable at 1.8 MiB and rejected only as a contract, not for size: the
partition is what lets deeper packs (14 MiB at V ≤ 10, more if Gaia
ever arrives) ship in the same shape, and its cost today is 72 small
files instead of one. *Variable RA divisions per band* (fewer columns
near the poles) — avoids skinny polar tiles but complicates identity
for no measured benefit; polar tiles are merely small.

### Pack layout: plain files, plain Java

```text
src/resources/catalog/bright-sky/
  manifest.properties
  NOTICE-tycho2.md
  NOTICE-openngc.md
  LICENSE-CC-BY-SA-4.0.txt
  PROVENANCE.md
  tiles/r00-d0/stars.csv        (only tiles with rows exist)
  tiles/r00-d0/dsos.csv
  ...
```

Rows use the existing CSV formats unchanged (stars: id, ra, dec,
vmag; DSOs: id, aliases, type, ra, dec, major, minor, pa, vmag,
label priority) — so the M31 chart data at V ≤ 8.0 is reproduced with
identifiers, aliases, magnitude semantics, DSO dimensions, and
provenance intact. The DSO `type` column now carries every usable
OpenNGC type (Cl+N, OCl, Neb, PN, …), not only `G`: search must find
M42 (NGC 1976, type Cl+N, V 4.0, 90′×60′) even though the renderer
only has a galaxy symbol today; how unrendered types appear on the
chart is a #43 decision. No database, no service, no binary format —
at these sizes (largest tile well under 200 KiB) the measurements
demonstrate no need.

**Determinism:** within a tile, stars order by (vmag, id) and DSOs by
id, exactly as today; all numbers format with `Locale.ROOT` at the
established precisions; LF endings; identical pinned inputs must
reproduce every file byte-identically (the #42 generator asserts it).

### Manifest: `manifest.properties`

A plain `java.util.Properties` file — machine-readable with zero new
dependencies. Required keys, validated by `PackManifest.parse`:
`format.version` (=1), `pack.name`, `coverage.type`,
`stars.limit.vmag`, `tiling.scheme` (=`radec-grid-30`),
`sources.tycho2.catalogue`, `sources.openngc.release`,
`license.stars`, `license.dsos`. Additionally one
`checksum.tiles/<tile>/<file>` SHA-256 entry per data file, plus free
provenance keys. An unsupported version or scheme fails loading with
a clear diagnostic — failure honesty at the boundary rather than a
silently wrong sky.

### Query contract

1. A cone query (the scene assembler's region, already padded by the
   1.5° object-extent margin) selects every tile whose bounds'
   clamped nearest point lies within the radius plus a 0.5°
   `SELECTION_PADDING_DEGREES`.
2. Selection is deliberately conservative: the padding covers the
   clamped-nearest-point approximation on the sphere, so a tile can
   be over-selected (cost: reading one small extra file) but never
   missed; a completeness test sweeps positions inside a
   boundary-straddling region and asserts every home tile was
   selected.
3. Rows from selected tiles are then filtered by the true query
   region — correctness never depends on tile-selection precision,
   only completeness does.
4. **No duplicates by construction:** every object lives in exactly
   one home tile (by centre position, `SkyTiling.tileId`). An object
   whose symbol crosses a tile edge stays visible because the query
   cone already carries the object-extent margin, which reaches into
   the neighbouring tile and selects it.

### Migration from the M31 resource (#43)

A `TiledCatalogue` implements the existing `Catalogue` interface over
the pack: parse the manifest at load, verify per-file checksums
lazily or at load (a mismatch is a clear failure, not a sparse sky),
read and cache selected tiles on query. `BundledCatalogue` and
`src/resources/catalog/m31/` retire when #43 switches the wiring;
`SceneAssembler`'s coverage cone widens from the 10° region to
all-sky (the coverage rule machinery remains — it becomes trivially
satisfied until partial packs return). The M31 reference chart must
remain visually identical at V ≤ 8.0; the reference image is the
guard.

## Consequences

- The 1,827 OpenNGC objects without any magnitude stay out of the
  pack (the chart model requires a magnitude); the count is recorded
  in generated provenance. A future search-only name listing could
  recover them.
- The Tycho-2 CC BY-NC 3.0 IGO terms now cover the all-sky star
  layer; the packaged application remains redistributable
  non-commercially only (`LICENSING.md` continues to apply, with
  paths updated in #43).
- Deeper packs and Gaia remain future decisions; this contract gives
  them a shape without building their machinery.
