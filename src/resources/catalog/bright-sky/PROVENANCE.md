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
- Deep-sky objects: every OpenNGC object with a position,
  including those without photometry or dimensions; the type
  column carries the OpenNGC token. Unknown values stay
  explicitly empty - the pack preserves facts and never
  invents dimensions, angles, or magnitudes. V and B
  magnitudes are stored in their own columns.

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
| Deep-sky objects written | 13371 |
| - type * | 546 |
| - type ** | 244 |
| - type *Ass | 64 |
| - type Cl+N | 67 |
| - type DrkN | 2 |
| - type EmN | 8 |
| - type G | 10521 |
| - type GCl | 208 |
| - type GGroup | 13 |
| - type GPair | 231 |
| - type GTrpl | 26 |
| - type HII | 83 |
| - type Neb | 94 |
| - type Nova | 3 |
| - type OCl | 663 |
| - type Other | 419 |
| - type PN | 130 |
| - type RfN | 38 |
| - type SNR | 11 |
| Dup/NonEx entries skipped | 662 |
| - dropped for missing position | 0 |
| - without any magnitude (kept, fields empty) | 1827 |
| - without V but with B (kept, vmag empty) | 7276 |
| - without dimensions (kept, fields empty) | 1300 |
| - without a position angle (kept, field empty) | 2596 |

Stars are ordered by (vmag, id) within each tile; deep-sky
objects by id. Identical pinned inputs reproduce every file
byte-identically.

## Relation to the M31 regional resource

The Sprint 3 regional resource retired when the application
switched to this pack (Sprint 5, issue #43); this pack is
the single source of bundled catalogue data, and the M31
reference chart reproduces from it byte-identically.
