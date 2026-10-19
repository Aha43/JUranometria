# Saturn System pack provenance

Generated resource - do not edit; regenerate with:

```sh
scripts/download-saturn-sources.sh
scripts/download-jovian-sources.sh
scripts/download-solar-system-sources.sh
make import-saturn-system
```

Pack `saturn-system` version 1, audited 2026-10-19.

## Sources

| input | URL | retrieved | SHA-256 |
|---|---|---|---|
| `sat441.bsp` (NASA/JPL NAIF, SAT441 with DE440) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/sat441.bsp | 2026-10-19 | `d7e444a9ba7a52b8f448ff0747030789524d2f0c1212e4e2bd7f5fc3c96444d5` |
| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc | 2026-10-07 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |
| `saturn_tables.html` (PDS Ring-Moon Systems Node, "Rings of Saturn"; values quoted, page not redistributed) | https://pds-rings.seti.org/saturn/saturn_tables.html | 2026-10-19 | `2dea8fcda149c94324353ccfb47e3df27f5dfef00ba8d62ab738a917378fc64c` |
| `de440s.bsp` (the released Solar System pack's source; the barycentre identity proof) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp | 2026-09-28 | `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` |

## Extraction

Tool: `juranometria.tool.SaturnPackMain` (this repository), using `juranometria.solar.spk.SpkExcerpt`, which keeps every segment of a pick with records in the interval (SAT441 keeps two per body, meeting at JD 2456928.5 TDB, 2014-09-28). `juranometria-sat441-saturn-1900-2100.bsp`: 0 -> 6 (Saturn barycentre), 6 -> 699 (Saturn), 1900-01-01 to 2101-01-01 TDB. `juranometria-sat441-moons-2000-2100.bsp`: 6 -> 601 (Mimas), 602 (Enceladus), 603 (Tethys), 604 (Dione), 605 (Rhea), 606 (Titan), 608 (Iapetus), 2000-01-01 to 2101-01-01 TDB; Hyperion (607) excluded as ruled. All SPK Type 2; a 31-day margin at each end; whole records kept from the first covering the start to the last covering the end, coefficients unchanged.

Coverage of the written segments (seconds past J2000, TDB):

- juranometria-sat441-saturn-1900-2100.bsp: 0 -> 6: -3160900800.0 to 3192609600.0
- juranometria-sat441-saturn-1900-2100.bsp: 6 -> 699: -3158481600.0 to 465134400.0
- juranometria-sat441-saturn-1900-2100.bsp: 6 -> 699: 465134400.0 to 3189974400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 601: -2721600.0 to 465134400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 601: 465134400.0 to 3189974400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 602: -2721600.0 to 465134400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 602: 465134400.0 to 3189974400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 603: -2721600.0 to 465134400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 603: 465134400.0 to 3189974400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 604: -2721600.0 to 465134400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 604: 465134400.0 to 3189974400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 605: -2786400.0 to 465134400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 605: 465134400.0 to 3190039200.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 606: -2721600.0 to 465134400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 606: 465134400.0 to 3190104000.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 608: -2980800.0 to 465134400.0
- juranometria-sat441-moons-2000-2100.bsp: 6 -> 608: 465134400.0 to 3190622400.0

## Output

| file | bytes | SHA-256 |
|---|---|---|
| `juranometria-sat441-saturn-1900-2100.bsp` | 16516096 | `28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d` |
| `juranometria-sat441-moons-2000-2100.bsp` | 62829568 | `8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b` |

## Validation

- Saturn's kernel: 86370 states over the coverage at 1.7-day spacing, all two bodies: position and velocity identical to the source, worst difference 0.0.
- The moons' kernel: 151900 states over the coverage at 1.7-day spacing, all seven bodies: position and velocity identical to the source, worst difference 0.0.
- The barycentre: 43222 states of sat441.bsp's 0 -> 6 over 1900-2100 with the margin, at 1.7-day spacing, identical to the released de440s.bsp's; the pack's barycentre is DE440's, the one the Sun and the Moon stand on.
- The released Solar System kernel is untouched: sha256 `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580`, verified before writing.
- `/resources/jovian-system/juranometria-jup365-galilean-2000-2100.bsp` is untouched: sha256 `6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6`, verified before writing.
- `/resources/jovian-system/juranometria-jup365-jupiter-1900-2100.bsp` is untouched: sha256 `efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92`, verified before writing.
- `/resources/planets/juranometria-de440-mercury-1900-2100.bsp` is untouched: sha256 `78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d`, verified before writing.
- `/resources/planets/juranometria-de440-venus-1900-2100.bsp` is untouched: sha256 `8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12`, verified before writing.

## Planetary constants, copied from `pck00011.tpc`

| keyword | value, verbatim |
|---|---|
| `BODY699_RADII` | `60268,60268,54364` |
| `BODY699_POLE_RA` | `40.589,-0.036,0.` |
| `BODY699_POLE_DEC` | `83.537,-0.004,0.` |
| `BODY10_RADII` | `695700.,695700.,695700.` |
| `BODY601_RADII` | `207.8,196.7,190.6` |
| `BODY602_RADII` | `256.6,251.4,248.3` |
| `BODY603_RADII` | `538.4,528.3,526.3` |
| `BODY604_RADII` | `563.4,561.3,559.6` |
| `BODY605_RADII` | `765.0,763.1,762.4` |
| `BODY606_RADII` | `2575.15,2574.78,2574.47` |
| `BODY608_RADII` | `745.7,745.7,712.1` |

The PCK assigns no BODY699_NUT_PREC_RA or BODY699_NUT_PREC_DEC: Saturn's pole is the linear model above, verified by the builder.

## Ring radii, quoted from the PDS table

| manifest key | PDS feature and boundary | km |
|---|---|---:|
| `rings.c-ring.inner.km` | C Ring, inner | 74491 |
| `rings.b-ring.outer.km` | B Ring, outer | 117570 |
| `rings.cassini-division.outer.km` | Cassini Division, outer | 122050 |
| `rings.a-ring.outer.km` | A Ring, outer | 136770 |

Read from `docs/studies/saturn-system/rings/pds-rings-table.tsv`, the study's row-by-row extraction of the retained page, which names the page by the digest above; the B Ring's outer boundary is the B Ring row's, as ruled on #574.

## Applicable rules

NAIF: unmodified kernels may be redistributed; a modified kernel must be renamed and its attribution replaced with the modifier's, which this pack does in each kernel's comment area and in `NOTICE-saturn-system.md`. No fees or licensing are required. The PDS page states no terms; its values are quoted with their source, the page not redistributed. Nothing of IMCCE's is included.
