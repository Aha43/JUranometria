# Planets pack provenance

Generated resource - do not edit; regenerate with:

```sh
scripts/download-solar-system-sources.sh
scripts/download-jovian-sources.sh
make import-planets
```

Pack `planets` version 2, audited 2026-10-13.

## Sources

| input | URL | retrieved | SHA-256 |
|---|---|---|---|
| `de440s.bsp` (NASA/JPL NAIF, DE440 short kernel; the released Solar System pack's source) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp | 2026-09-28 | `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` |
| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc | 2026-10-07 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |

## Extraction

Tool: `juranometria.tool.PlanetsPackMain` (this repository), using `juranometria.solar.spk.SpkExcerpt`. One file per planet; each keeps the planet's de440s segments, SPK Type 2, over 1900-01-01 to 2101-01-01 TDB with a 31-day margin at each end; whole records kept from the first covering the start to the last covering the end, coefficients unchanged.

`juranometria-de440-venus-1900-2100.bsp` (Venus), coverage of the written segments (seconds past J2000, TDB):

- 0 -> 2: -3159518400.0 to 3191227200.0
- 2 -> 299: -4734072000.0 to 4735368000.0

`juranometria-de440-mercury-1900-2100.bsp` (Mercury), coverage of the written segments (seconds past J2000, TDB):

- 0 -> 1: -3158827200.0 to 3190536000.0
- 1 -> 199: -4734072000.0 to 4735368000.0

## Output

| file | bytes | SHA-256 |
|---|---|---|
| `juranometria-de440-venus-1900-2100.bsp` | 1242112 | `8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12` |
| `juranometria-de440-mercury-1900-2100.bsp` | 3299328 | `78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d` |

## Validation

- juranometria-de440-venus-1900-2100.bsp: 86370 states over the coverage at 1.7-day spacing, every segment: position and velocity identical to the source, worst difference 0.0.
- juranometria-de440-mercury-1900-2100.bsp: 86370 states over the coverage at 1.7-day spacing, every segment: position and velocity identical to the source, worst difference 0.0.
- Venus about its barycentre (2 -> 299): 43185 states, position and velocity identically zero: the planet is its barycentre.
- Mercury about its barycentre (1 -> 199): 43185 states, position and velocity identically zero: the planet is its barycentre.
- The released Solar System kernel is untouched: sha256 `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580`, verified before writing.
- The released Jovian kernel `juranometria-jup365-galilean-2000-2100.bsp` is untouched: sha256 `6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6`, verified before writing.
- The released Jovian kernel `juranometria-jup365-jupiter-1900-2100.bsp` is untouched: sha256 `efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92`, verified before writing.
- A released planet's file is never rewritten: the builder refuses to write a pack in which one would change.

## Planetary constants, copied from `pck00011.tpc`

| keyword | value, verbatim |
|---|---|
| `BODY299_RADII` | `6051.8,6051.8,6051.8` |
| `BODY199_RADII` | `2440.53,2440.53,2438.26` |
| `BODY10_RADII` | `695700.,695700.,695700.` |

## Applicable rules

NAIF: unmodified kernels may be redistributed; a modified kernel must be renamed and its attribution replaced with the modifier's, which this pack does in each kernel's comment area and in `NOTICE-planets.md`. SPICE data are "Technology and Software Publicly Available"; no fees or licensing are required.
