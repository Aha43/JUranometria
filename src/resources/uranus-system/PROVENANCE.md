# Uranus System pack provenance

Generated resource - do not edit; regenerate with:

```sh
scripts/download-uranus-sources.sh
scripts/download-jovian-sources.sh
make import-uranus-system
```

Pack `uranus-system` version 1, audited 2026-10-27.

## Sources

| input | URL | retrieved | SHA-256 |
|---|---|---|---|
| `ura184_part-3.bsp` (NASA/JPL NAIF, URA182.21 with DE442) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura184_part-3.bsp | 2026-10-27 | `273cd4ccc470d1098562cd4e94fa48fc2b4c6b1c2896308e473c949b87937c53` |
| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc | 2026-10-07 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |

## Extraction

Tool: `juranometria.tool.UranusSystemPackMain` (this repository), using `juranometria.solar.spk.SpkExcerpt`.

- `juranometria-de442-uranus-1900-2100.bsp`: 0 -> 7, SPK Type 2, from 1900-01-01 to 2101-01-01 TDB with 31-day margins (#633, U-1a, U-2).
- `juranometria-ura182-moons-2000-2100.bsp`: 7 -> 705 (Miranda), 701 (Ariel), 702 (Umbriel), 703 (Titania), 704 (Oberon), SPK Type 2, from 2000-01-01 to 2101-01-01 TDB with 31-day margins (#633, U-6a).

Whole records kept, coefficients unchanged. Coverage of the written segments (seconds past J2000, TDB):

- 0 -> 7: -3160900800.0 to 3192609600.0 (DE-0442LE-0442)
- 7 -> 705: -2721600.0 to 3189931200.0 (URA182.21)
- 7 -> 701: -2721600.0 to 3189974400.0 (URA182.21)
- 7 -> 702: -2721600.0 to 3189974400.0 (URA182.21)
- 7 -> 703: -2808000.0 to 3190017600.0 (URA182.21)
- 7 -> 704: -2808000.0 to 3190017600.0 (URA182.21)

## Output

| file | bytes | SHA-256 |
|---|---|---|
| `juranometria-de442-uranus-1900-2100.bsp` | 387072 | `21964f569b7fe33b2a5648090215942e86542af8a5718f93fa5987259af25c9e` |
| `juranometria-ura182-moons-2000-2100.bsp` | 38055936 | `6f8ff87802472a0c30f4c1779d6b1dec004922ce5573795200b518a6ebc19180` |

## Validation

- Uranus's kernel: 0 -> 7: 43223 states at 1.7-day spacing and the last instant, position and velocity as the source's, worst difference 0.00 km.
- The moons' kernel: 7 -> 705, 7 -> 701, 7 -> 702, 7 -> 703, 7 -> 704: 108690 states at 1.7-day spacing and the last instant, position and velocity as the source's, worst difference 6.17e-11 km.
- The barycentre's source: 1 segment(s) of 0 -> 7, named DE-0442LE-0442: DE442's, as ruled (U-1a).
- Uranus about its barycentre: 1761937 hourly states over 1900-2100, the centre at most 45.136 km from the barycentre (limit 50.0 km): the planet is drawn from its barycentre, as ruled (U-2).
- The pole: `pck00011.tpc` assigns Uranus no periodic terms (BODY799_NUT_PREC_RA, BODY799_NUT_PREC_DEC absent), as the ruled model has none (U-4).
- The released Solar System kernel is untouched: sha256 `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580`, verified before writing.
- `/resources/jovian-system/juranometria-jup365-galilean-2000-2100.bsp` is untouched: sha256 `6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6`, verified before writing.
- `/resources/jovian-system/juranometria-jup365-jupiter-1900-2100.bsp` is untouched: sha256 `efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92`, verified before writing.
- `/resources/mars-system/juranometria-mar099-moons-2000-2050.bsp` is untouched: sha256 `cd84a9bfd574ebaeec63893d30735129de030e7e361af6c0f465f634629bc208`, verified before writing.
- `/resources/planets/juranometria-de440-mars-1900-2100.bsp` is untouched: sha256 `c59b91b0188dc540906916c067238c8d311fd718f65b909825720e54e22e7ffb`, verified before writing.
- `/resources/planets/juranometria-de440-mercury-1900-2100.bsp` is untouched: sha256 `78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d`, verified before writing.
- `/resources/planets/juranometria-de440-venus-1900-2100.bsp` is untouched: sha256 `8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12`, verified before writing.
- `/resources/saturn-system/juranometria-sat441-moons-2000-2100.bsp` is untouched: sha256 `8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b`, verified before writing.
- `/resources/saturn-system/juranometria-sat441-saturn-1900-2100.bsp` is untouched: sha256 `28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d`, verified before writing.

## Planetary constants, copied from `pck00011.tpc`

| keyword | value, verbatim |
|---|---|
| `BODY799_RADII` | `25559,25559,24973` |
| `BODY799_POLE_RA` | `257.311,0.,0.` |
| `BODY799_POLE_DEC` | `-15.175,0.,0.` |
| `BODY705_RADII` | `240.4,234.2,232.9` |
| `BODY701_RADII` | `581.1,577.9,577.7` |
| `BODY702_RADII` | `584.7,584.7,584.7` |
| `BODY703_RADII` | `788.9,788.9,788.9` |
| `BODY704_RADII` | `761.4,761.4,761.4` |
| `BODY10_RADII` | `695700.,695700.,695700.` |

## Applicable rules

NAIF: unmodified kernels may be redistributed; a modified kernel must be renamed and its attribution replaced with the modifier's, which this pack does in each kernel's comment area and in `NOTICE-uranus-system.md`. No fees or licensing are required.
