# Bright-sky pack provenance

Generated resources - do not edit by hand. Regenerate with:

```sh
scripts/download-catalogue-sources.sh
make import-allsky
```

The pack implements docs/decisions/all-sky-tiling.md over the
sources decided in docs/decisions/catalogue-sources.md. Raw
inputs were audited 2026-08-29; the import tool verifies their SHA-256
checksums (pinned in `PinnedInputs`) before transforming
anything, and `manifest.properties` carries the SHA-256 of
every generated tile file.

## Sources

- Stars: The Tycho-2 Catalogue (Hog E. et al., 2000, A&A 355,
  L27), CDS I/259, main catalogue and supplement-1. License
  CC BY-NC 3.0 IGO - see `NOTICE-tycho2.md`.
- Deep-sky objects: OpenNGC release v20260501 (Mattia Verga).
  License CC-BY-SA-4.0 - see `NOTICE-openngc.md` and
  `LICENSE-CC-BY-SA-4.0.txt`.

## Coverage and rules

- Coverage: the complete sky, partitioned into 72 populated
  radec-grid-30 tiles; every object lives in exactly one home
  tile chosen by its centre position.
- Stars: Johnson V <= 8.0, derived per the I/259 ReadMe as
  V = VT - 0.090 * (BT - VT); VT (or Hp in supplement-1) used
  unchanged when BT is absent. Supplement-1 positions are at
  epoch J1991.25 without proper-motion propagation. Identifier
  collisions follow the main-catalogue-wins component policy.
- Deep-sky objects: every usable OpenNGC type; the type column
  carries the OpenNGC token. Objects without any V or B
  magnitude are dropped (the chart model requires one);
  missing dimensions receive a nominal arcminute, counted
  below.

## Row counts and normalizations

| Fact | Count |
|---|---|
| Stars written | 45630 |
| - from the main catalogue | 45470 |
| - from supplement-1 | 160 |
| - using the observed (fallback) position | 466 |
| - V taken from VT alone (no BT) | 1 |
| - V taken from an Hp magnitude | 40 |
| - supplement components skipped for an existing TYC id | 0 |
| Records dropped for missing VT (whole sky) | 25 |
| Deep-sky objects written | 11544 |
| - type * | 63 |
| - type ** | 15 |
| - type *Ass | 15 |
| - type Cl+N | 41 |
| - type EmN | 4 |
| - type G | 10459 |
| - type GCl | 197 |
| - type GPair | 25 |
| - type GTrpl | 3 |
| - type HII | 40 |
| - type Neb | 38 |
| - type Nova | 3 |
| - type OCl | 487 |
| - type Other | 6 |
| - type PN | 129 |
| - type RfN | 13 |
| - type SNR | 6 |
| Dup/NonEx entries skipped | 662 |
| - dropped for missing position | 0 |
| - dropped for missing V and B | 1827 |
| - major axis absent, nominal 1.0 arcmin | 88 |
| - minor axis absent, set to major | 663 |
| - position angle absent, recorded as 0.0 | 884 |
| - V magnitude taken from B | 7276 |

Stars are ordered by (vmag, id) within each tile; deep-sky
objects by id. Identical pinned inputs reproduce every file
byte-identically.

## Relation to the M31 regional resource

`src/resources/catalog/m31/` remains the application's live
data source until the loader switches to this pack (Sprint 5,
issue #43), at which point the regional resource retires.
Until then the two coexist deliberately: this pack is not yet
read by the application.
