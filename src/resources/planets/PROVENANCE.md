# Planets pack provenance

Generated resource - do not edit; regenerate with:

```sh
scripts/download-solar-system-sources.sh
scripts/download-jovian-sources.sh
make import-planets
```

Pack `planets` version 3, audited 2026-10-25.

## Sources

| input | URL | retrieved | SHA-256 |
|---|---|---|---|
| `de440s.bsp` (NASA/JPL NAIF, DE440 short kernel; the released Solar System pack's source) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp | 2026-09-28 | `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` |
| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc | 2026-10-07 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |
| `mar099s.bsp` (NASA/JPL NAIF, Mars satellite ephemeris; read to measure Mars's centre about its barycentre, not redistributed by this pack) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/mar099s.bsp | 2026-10-25 | `997dc93ba640e476da7a494d2237dcdeb145e528db37be8ccee588c615e4e1ff` |

## Extraction

Tool: `juranometria.tool.PlanetsPackMain` (this repository), using `juranometria.solar.spk.SpkExcerpt`. One file per planet; each keeps the planet's de440s segments, SPK Type 2, over 1900-01-01 to 2101-01-01 TDB with a 31-day margin at each end; whole records kept from the first covering the start to the last covering the end, coefficients unchanged.

`juranometria-de440-venus-1900-2100.bsp` (Venus), coverage of the written segments (seconds past J2000, TDB):

- 0 -> 2: -3159518400.0 to 3191227200.0
- 2 -> 299: -4734072000.0 to 4735368000.0

`juranometria-de440-mercury-1900-2100.bsp` (Mercury), coverage of the written segments (seconds past J2000, TDB):

- 0 -> 1: -3158827200.0 to 3190536000.0
- 1 -> 199: -4734072000.0 to 4735368000.0

`juranometria-de440-mars-1900-2100.bsp` (Mars), coverage of the written segments (seconds past J2000, TDB):

- 0 -> 4: -3160900800.0 to 3192609600.0

## Output

| file | bytes | SHA-256 |
|---|---|---|
| `juranometria-de440-venus-1900-2100.bsp` | 1242112 | `8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12` |
| `juranometria-de440-mercury-1900-2100.bsp` | 3299328 | `78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d` |
| `juranometria-de440-mars-1900-2100.bsp` | 709632 | `c59b91b0188dc540906916c067238c8d311fd718f65b909825720e54e22e7ffb` |

## Validation

- juranometria-de440-venus-1900-2100.bsp: 86370 states over the coverage at 1.7-day spacing, every segment: position and velocity identical to the source, worst difference 0.0.
- juranometria-de440-mercury-1900-2100.bsp: 86370 states over the coverage at 1.7-day spacing, every segment: position and velocity identical to the source, worst difference 0.0.
- juranometria-de440-mars-1900-2100.bsp: 43185 states over the coverage at 1.7-day spacing, every segment: position and velocity identical to the source, worst difference 0.0.
- Venus about its barycentre (2 -> 299): 43185 states, position and velocity identically zero: the planet is its barycentre.
- Mercury about its barycentre (1 -> 199): 43185 states, position and velocity identically zero: the planet is its barycentre.
- Mars about its barycentre (`mar099s.bsp`, 4 -> 499): 482137 hourly states over mar099s's span, the centre at most 0.210 m from the barycentre (limit 0.25 m): the planet is drawn from its barycentre, as ruled (#601, M-1).
- The released Solar System kernel is untouched: sha256 `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580`, verified before writing.
- The released Jovian kernel `juranometria-jup365-galilean-2000-2100.bsp` is untouched: sha256 `6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6`, verified before writing.
- The released Jovian kernel `juranometria-jup365-jupiter-1900-2100.bsp` is untouched: sha256 `efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92`, verified before writing.
- The released Saturn System kernel `juranometria-sat441-moons-2000-2100.bsp` is untouched: sha256 `8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b`, verified before writing.
- The released Saturn System kernel `juranometria-sat441-saturn-1900-2100.bsp` is untouched: sha256 `28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d`, verified before writing.
- A released planet's file is never rewritten: the builder refuses to write a pack in which one would change.

## Planetary constants, copied from `pck00011.tpc`

| keyword | value, verbatim |
|---|---|
| `BODY299_RADII` | `6051.8,6051.8,6051.8` |
| `BODY199_RADII` | `2440.53,2440.53,2438.26` |
| `BODY499_RADII` | `3396.19,3396.19,3376.20` |
| `BODY499_POLE_RA` | `317.269202,-0.10927547,0.` |
| `BODY499_POLE_DEC` | `54.432516,-0.05827105,0.` |
| `BODY499_NUT_PREC_RA` | `0,0,0,0,0,0,0,0,0,0,0.000068,0.000238,0.000052,0.000009,0.419057` |
| `BODY499_NUT_PREC_DEC` | `0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0.000051,0.000141,0.000031,0.000005,1.591274` |
| `BODY4_NUT_PREC_ANGLES` | `190.72646643,15917.10818695,0,21.46892470,31834.27934054,0,332.86082793,19139.89694742,0,394.93256437,38280.79631835,0,189.63271560,41215158.18420050,12.711923222,121.46893664,660.22803474,0,231.05028581,660.99123540,0,251.37314025,1320.50145245,0,217.98635955,38279.96125550,0,196.19729402,19139.83628608,0,198.991226,19139.4819985,0,226.292679,38280.8511281,0,249.663391,57420.7251593,0,266.183510,76560.6367950,0,79.398797,0.5042615,0,122.433576,19139.9407476,0,43.058401,38280.8753272,0,57.663379,57420.7517205,0,79.476401,76560.6495004,0,166.325722,0.5042615,0,129.071773,19140.0328244,0,36.352167,38281.0473591,0,56.668646,57420.9295360,0,67.364003,76560.2552215,0,104.792680,95700.4387578,0,95.391654,0.5042615,0` |
| `BODY4_MAX_PHASE_DEGREE` | `2` |
| `BODY10_RADII` | `695700.,695700.,695700.` |

## Applicable rules

NAIF: unmodified kernels may be redistributed; a modified kernel must be renamed and its attribution replaced with the modifier's, which this pack does in each kernel's comment area and in `NOTICE-planets.md`. SPICE data are "Technology and Software Publicly Available"; no fees or licensing are required.
