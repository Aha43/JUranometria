# Mars System pack provenance

Generated resource - do not edit; regenerate with:

```sh
scripts/download-mars-sources.sh
scripts/download-jovian-sources.sh
scripts/download-solar-system-sources.sh
make import-mars-system
```

Pack `mars-system` version 1, audited 2026-10-25.

## Sources

| input | URL | retrieved | SHA-256 |
|---|---|---|---|
| `mar099s.bsp` (NASA/JPL NAIF, MAR099 with DE440) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/mar099s.bsp | 2026-10-25 | `997dc93ba640e476da7a494d2237dcdeb145e528db37be8ccee588c615e4e1ff` |
| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc | 2026-10-07 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |
| `de440s.bsp` (the released Solar System pack's source; the barycentre identity proof) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp | 2026-09-28 | `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` |

## Extraction

Tool: `juranometria.tool.MarsSystemPackMain` (this repository), using `juranometria.solar.spk.SpkExcerpt`. `juranometria-mar099-moons-2000-2050.bsp`: 4 -> 401 (Phobos), 4 -> 402 (Deimos), SPK Type 2, from 2000-01-01 TDB with a 31-day margin before it, to the source's own end, 1577880000.0 s past J2000 TDB (2050-01-01 00:00 TDB), the moons' exclusive boundary (#601, M-7a with the amendment). Whole records kept, coefficients unchanged.

Coverage of the written segments (seconds past J2000, TDB):

- 4 -> 401: -2721600.0 to 1577880000.0
- 4 -> 402: -2721600.0 to 1577880000.0

## Output

| file | bytes | SHA-256 |
|---|---|---|
| `juranometria-mar099-moons-2000-2050.bsp` | 43951104 | `cd84a9bfd574ebaeec63893d30735129de030e7e361af6c0f465f634629bc208` |

## Validation

- The moons' kernel: 21524 states at 1.7-day spacing from the start up to, not including, the exclusive end, both moons: position and velocity identical to the source, worst difference 0.0.
- The barycentre: 10762 states of mar099s.bsp's 0 -> 4 over the moons' interval, at 1.7-day spacing, identical to the released de440s.bsp's; the moons stand on the barycentre the planets pack carries.
- The released Solar System kernel is untouched: sha256 `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580`, verified before writing.
- `/resources/jovian-system/juranometria-jup365-galilean-2000-2100.bsp` is untouched: sha256 `6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6`, verified before writing.
- `/resources/jovian-system/juranometria-jup365-jupiter-1900-2100.bsp` is untouched: sha256 `efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92`, verified before writing.
- `/resources/planets/juranometria-de440-mercury-1900-2100.bsp` is untouched: sha256 `78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d`, verified before writing.
- `/resources/planets/juranometria-de440-venus-1900-2100.bsp` is untouched: sha256 `8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12`, verified before writing.
- `/resources/saturn-system/juranometria-sat441-moons-2000-2100.bsp` is untouched: sha256 `8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b`, verified before writing.
- `/resources/saturn-system/juranometria-sat441-saturn-1900-2100.bsp` is untouched: sha256 `28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d`, verified before writing.

## Planetary constants, copied from `pck00011.tpc`

| keyword | value, verbatim |
|---|---|
| `BODY401_RADII` | `13.0,11.4,9.1` |
| `BODY402_RADII` | `7.8,6.0,5.1` |

## Applicable rules

NAIF: unmodified kernels may be redistributed; a modified kernel must be renamed and its attribution replaced with the modifier's, which this pack does in the kernel's comment area and in `NOTICE-mars-system.md`. No fees or licensing are required.
