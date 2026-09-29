# Solar System pack provenance

Generated resource - do not edit; regenerate with:

```sh
scripts/download-solar-system-sources.sh
make import-solar-system
```

Pack `solar-system` version 2, audited 2026-09-29.

## Sources

| input | URL | retrieved | SHA-256 |
|---|---|---|---|
| `de440s.bsp` (NASA/JPL NAIF, DE440 short kernel) | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp | 2026-09-28 | `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` |
| `Leap_Second.dat` (IERS Earth Orientation Centre) | https://hpiers.obspm.fr/iers/bul/bulc/Leap_Second.dat | 2026-09-28 | `6cb6f5d4b819f2e568e25db4b0b26d89dedf031fdffb18bc94d40f4e94e268d7` |

## Extraction

Tool: `juranometria.tool.SolarSystemPackMain` (this repository), using `juranometria.solar.spk.SpkExcerpt`. Segments kept, in order: 0 -> 3 (Earth-Moon barycentre), 0 -> 10 (Sun), 3 -> 399 (Earth), 3 -> 301 (Moon); all SPK Type 2. Interval: 1900-01-01 to 2101-01-01 TDB, with a 31-day margin at each end; whole records are kept from the first covering the start to the last covering the end, coefficients unchanged.

Coverage of the written segments (seconds past J2000, TDB):

- 0 -> 3: -3159518400.0 to 3191227200.0
- 0 -> 10: -3159518400.0 to 3191227200.0
- 3 -> 399: -3158481600.0 to 3190190400.0
- 3 -> 301: -3158481600.0 to 3190190400.0

## Output

| file | bytes | SHA-256 |
|---|---|---|
| `juranometria-de440-sun-emb-earth-moon-1900-2100.bsp` | 14910464 | `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580` |

## Validation

172740 states over the coverage at 1.7-day spacing, all four segments: position and velocity identical to the source, worst difference 0.0.

## Applicable rules

NAIF: unmodified kernels may be redistributed; a modified kernel must be renamed and its attribution replaced with the modifier's, which this pack does in the kernel's comment area and in `NOTICE-solar-system.md`. SPICE data are "Technology and Software Publicly Available"; no fees or licensing are required. The IERS file is public bulletin data, bundled unmodified.
