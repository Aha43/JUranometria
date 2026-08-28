# M31-region catalogue provenance

Generated resources - do not edit by hand. Regenerate with:

```sh
scripts/download-catalogue-sources.sh
make import-catalogue
```

The sources, licensing, magnitude semantics, and import contract
are decided in `docs/decisions/catalogue-sources.md`. Raw inputs
were audited 2026-08-29; the import tool verifies their SHA-256
checksums (pinned in `CatalogueImportMain`) before transforming
anything.

## Sources

- Stars: The Tycho-2 Catalogue (Hog E. et al., 2000, A&A 355,
  L27), CDS I/259, main catalogue and supplement-1, from
  `https://cdsarc.cds.unistra.fr/ftp/I/259/`. License
  CC BY-NC 3.0 IGO - see `NOTICE-tycho2.md`.
- Deep-sky objects: OpenNGC release v20260501 (Mattia Verga),
  `NGC.csv` and `addendum.csv`, from
  `https://github.com/mattiaverga/OpenNGC`. License
  CC-BY-SA-4.0 - see `NOTICE-openngc.md` and
  `LICENSE-CC-BY-SA-4.0.txt`.

## Coverage and limits

- Region: cone of radius 10.0 degrees around the M31 centre
  (10.684708, +41.268750 ICRS).
- Stars: Johnson V <= 10.0, derived per the I/259 ReadMe as
  V = VT - 0.090 * (BT - VT); VT (or Hp in supplement-1) used
  unchanged when BT is absent. Supplement-1 positions are at
  their catalogue epoch J1991.25 without proper-motion
  propagation (out of scope; sub-arcsecond at chart scales).
- Deep-sky objects: every OpenNGC galaxy (type G) in the
  region; other types wait for their chart symbols.

## Row counts and normalizations

| Fact | Count |
|---|---|
| Stars written | 3205 |
| - from the main catalogue | 3188 |
| - from supplement-1 | 17 |
| - using the observed (fallback) position | 13 |
| - V taken from VT alone (no BT) | 0 |
| - V taken from an Hp magnitude | 1 |
| Records dropped for missing VT (whole sky) | 25 |
| Galaxies written | 47 |
| In-region objects of other types, skipped | 10 |
| Dup/NonEx entries skipped (whole sky) | 662 |
| - position angle absent, recorded as 0.0 | 0 |
| - minor axis absent, set to major | 0 |
| - V magnitude taken from B | 31 |
| - galaxies dropped for missing V and B | 1 |

Stars are ordered by (vmag, id); deep-sky objects by id.
Identical pinned inputs reproduce these files byte-identically.
