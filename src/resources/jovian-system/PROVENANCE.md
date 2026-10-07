# Jovian System pack provenance

Generated resource - do not edit; regenerate with:

```sh
scripts/download-jovian-sources.sh
scripts/download-solar-system-sources.sh
make import-jovian-system
```

Pack `jovian-system` version 1, audited 2026-10-08.

## Sources

| input | URL | retrieved | SHA-256 |
|---|---|---|---|
| `jup365.bsp` (NASA/JPL NAIF, JUP365 with DE440) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/jup365.bsp | 2026-10-07 | `dbf016c01ba4d022154838000cf3f06962cf958ddc503a366f7fe8f81495c5cb` |
| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc | 2026-10-07 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |
| `de440s.bsp` (the released Solar System pack's source; the barycentre identity proof) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp | 2026-09-28 | `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` |

## Extraction

Tool: `juranometria.tool.JovianPackMain` (this repository), using `juranometria.solar.spk.SpkExcerpt`, which keeps every segment of a pick with records in the interval (JUP365 keeps two per body, meeting at JD 2450464.5 TDB, 1997-01-16). `juranometria-jup365-jupiter-1900-2100.bsp`: 0 -> 5 (Jupiter barycentre), 5 -> 599 (Jupiter), 1900-01-01 to 2101-01-01 TDB. `juranometria-jup365-galilean-2000-2100.bsp`: 5 -> 501 (Io), 5 -> 502 (Europa), 5 -> 503 (Ganymede), 5 -> 504 (Callisto), 2000-01-01 to 2101-01-01 TDB. All SPK Type 2; a 31-day margin at each end; whole records kept from the first covering the start to the last covering the end, coefficients unchanged.

Coverage of the written segments (seconds past J2000, TDB):

- juranometria-jup365-jupiter-1900-2100.bsp: 0 -> 5: -3160900800.0 to 3192609600.0
- juranometria-jup365-jupiter-1900-2100.bsp: 5 -> 599: -3158460000.0 to -93355200.0
- juranometria-jup365-jupiter-1900-2100.bsp: 5 -> 599: -93355200.0 to 3190060800.0
- juranometria-jup365-galilean-2000-2100.bsp: 5 -> 501: -2764800.0 to 3189963600.0
- juranometria-jup365-galilean-2000-2100.bsp: 5 -> 502: -2764800.0 to 3189963600.0
- juranometria-jup365-galilean-2000-2100.bsp: 5 -> 503: -2764800.0 to 3189963600.0
- juranometria-jup365-galilean-2000-2100.bsp: 5 -> 504: -2764800.0 to 3190060800.0

## Output

| file | bytes | SHA-256 |
|---|---|---|
| `juranometria-jup365-jupiter-1900-2100.bsp` | 13550592 | `efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92` |
| `juranometria-jup365-galilean-2000-2100.bsp` | 39294976 | `6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6` |

## Validation

- Jupiter's kernel: 86370 states over the coverage at 1.7-day spacing, all two bodies: position and velocity identical to the source, worst difference 0.0.
- The moons' kernel: 86800 states over the coverage at 1.7-day spacing, all four bodies: position and velocity identical to the source, worst difference 0.0.
- The barycentre: 43222 states of jup365.bsp's 0 -> 5 over 1900-2100 with the margin, at 1.7-day spacing, identical to the released de440s.bsp's; the pack's barycentre is DE440's, the one the Sun and the Moon stand on.
- The released Solar System kernel is untouched: sha256 `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580`, verified before writing.

## Planetary constants, copied from `pck00011.tpc`

| keyword | value, verbatim |
|---|---|
| `BODY599_RADII` | `71492,71492,66854` |
| `BODY599_POLE_RA` | `268.056595,-0.006499,0.` |
| `BODY599_POLE_DEC` | `64.495303,0.002413,0.` |
| `BODY10_RADII` | `695700.,695700.,695700.` |
| `BODY501_RADII` | `1829.4,1819.4,1815.7` |
| `BODY502_RADII` | `1562.6,1560.3,1559.5` |
| `BODY503_RADII` | `2631.2,2631.2,2631.2` |
| `BODY504_RADII` | `2410.3,2410.3,2410.3` |

## Applicable rules

NAIF: unmodified kernels may be redistributed; a modified kernel must be renamed and its attribution replaced with the modifier's, which this pack does in each kernel's comment area and in `NOTICE-jovian-system.md`. SPICE data are "Technology and Software Publicly Available"; no fees or licensing are required. Nothing of IMCCE's is included.
