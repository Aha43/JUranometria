# Neptune System pack provenance

Generated resource - do not edit; regenerate with:

```sh
scripts/download-neptune-sources.sh
scripts/download-jovian-sources.sh
make import-neptune-system
```

Pack `neptune-system` version 1, audited 2026-10-29.

## Sources

| input | URL | retrieved | SHA-256 |
|---|---|---|---|
| `nep098_part-1.bsp` (NASA/JPL NAIF, NEP098 with DE442) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/nep098_part-1.bsp | 2026-10-29 | `5327f47abba438d993237ff676ab03b728778a965bb238f86de883c0ad7790b8` |
| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc | 2026-10-07 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |

## Extraction

Tool: `juranometria.tool.NeptuneSystemPackMain` (this repository), using `juranometria.solar.spk.SpkExcerpt`.

- `juranometria-de442-neptune-1900-2100.bsp`: 0 -> 8, SPK Type 2, from 1900-01-01 to 2101-01-01 TDB with 31-day margins (#654, N-1a, N-2).
- `juranometria-nep098-moons-2000-2100.bsp`: 8 -> 801 (Triton), 802 (Nereid), SPK Type 2, from 2000-01-01 to 2101-01-01 TDB with 31-day margins (#654, N-6a).

Whole records kept, coefficients unchanged. Coverage of the written segments (seconds past J2000, TDB):

- 0 -> 8: -3160900800.0 to 3192609600.0 (DE-0442LE-0442)
- 8 -> 801: -2721600.0 to 3189931200.0 (NEP098)
- 8 -> 802: -2721600.0 to 3189931200.0 (NEP098)

## Output

| file | bytes | SHA-256 |
|---|---|---|
| `juranometria-de442-neptune-1900-2100.bsp` | 416768 | `12a8da7d0006bb5babf8477f441c5cdc84db9deff0e811369a1d1f4a75b2bca8` |
| `juranometria-nep098-moons-2000-2100.bsp` | 50302976 | `e78119d977a4593ed81e5850ad5dede3ce6026a86add05d4dfc2c4faf0f90576` |

## Validation

- Neptune's kernel: 0 -> 8: 43223 states at 1.7-day spacing and the last instant, position and velocity as the source's, worst difference 0.00 km.
- The moons' kernel: 8 -> 801, 8 -> 802: 43476 states at 1.7-day spacing and the last instant, position and velocity as the source's, worst difference 9.31e-10 km.
- The barycentre's source: 1 segment(s) of 0 -> 8, named DE-0442LE-0442: DE442's, as ruled (N-1a).
- Neptune about its barycentre: 1761937 hourly states over 1900-2100, the centre at most 74.190 km from the barycentre (limit 80.0 km): the planet is drawn from its barycentre, as ruled (N-2).
- The pole's periodic term: RA +0.70 sin N and Dec -0.51 cos N over N = 357.85° + 52.316° T, every other coefficient zero, as ruled (N-4).
- The released Solar System kernel is untouched: sha256 `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580`, verified before writing.
- `/resources/jovian-system/juranometria-jup365-galilean-2000-2100.bsp` is untouched: sha256 `6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6`, verified before writing.
- `/resources/jovian-system/juranometria-jup365-jupiter-1900-2100.bsp` is untouched: sha256 `efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92`, verified before writing.
- `/resources/mars-system/juranometria-mar099-moons-2000-2050.bsp` is untouched: sha256 `cd84a9bfd574ebaeec63893d30735129de030e7e361af6c0f465f634629bc208`, verified before writing.
- `/resources/planets/juranometria-de440-mars-1900-2100.bsp` is untouched: sha256 `c59b91b0188dc540906916c067238c8d311fd718f65b909825720e54e22e7ffb`, verified before writing.
- `/resources/planets/juranometria-de440-mercury-1900-2100.bsp` is untouched: sha256 `78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d`, verified before writing.
- `/resources/planets/juranometria-de440-venus-1900-2100.bsp` is untouched: sha256 `8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12`, verified before writing.
- `/resources/saturn-system/juranometria-sat441-moons-2000-2100.bsp` is untouched: sha256 `8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b`, verified before writing.
- `/resources/saturn-system/juranometria-sat441-saturn-1900-2100.bsp` is untouched: sha256 `28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d`, verified before writing.
- `/resources/uranus-system/juranometria-de442-uranus-1900-2100.bsp` is untouched: sha256 `21964f569b7fe33b2a5648090215942e86542af8a5718f93fa5987259af25c9e`, verified before writing.
- `/resources/uranus-system/juranometria-ura182-moons-2000-2100.bsp` is untouched: sha256 `6f8ff87802472a0c30f4c1779d6b1dec004922ce5573795200b518a6ebc19180`, verified before writing.

## Planetary constants, copied from `pck00011.tpc`

| keyword | value, verbatim |
|---|---|
| `BODY899_RADII` | `24764,24764,24341` |
| `BODY899_POLE_RA` | `299.36,0.,0.` |
| `BODY899_POLE_DEC` | `43.46,0.,0.` |
| `BODY899_NUT_PREC_RA` | `0.70,0.,0.,0.,0.,0.,0.,0.` |
| `BODY899_NUT_PREC_DEC` | `-0.51,0.,0.,0.,0.,0.,0.,0.` |
| `BODY8_NUT_PREC_ANGLES` | `357.85,52.316,323.92,62606.6,220.51,55064.2,354.27,46564.5,75.31,26109.4,35.36,14325.4,142.61,2824.6,177.85,52.316,647.840,125213.200,355.700,104.632,533.550,156.948,711.400,209.264,889.250,261.580,1067.100,313.896,1244.950,366.212,1422.800,418.528,1600.650,470.844` |
| `BODY801_RADII` | `1352.6,1352.6,1352.6` |
| `BODY802_RADII` | `170,170,170` |
| `BODY10_RADII` | `695700.,695700.,695700.` |

## Applicable rules

NAIF: unmodified kernels may be redistributed; a modified kernel must be renamed and its attribution replaced with the modifier's, which this pack does in each kernel's comment area and in `NOTICE-neptune-system.md`. No fees or licensing are required.
