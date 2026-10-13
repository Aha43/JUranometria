# Mercury pack study (#528): the excerpt, measured

source de440s.bsp: 32,726,016 bytes, sha256 c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2

Mercury in the source (type, coverage in JD TDB):

| segment | centre | target | type | from | to |
|---|---|---|---|---|---|
| DE-0440LE-0440 | 0 | 1 | 2 | 2396752.5 | 2506352.5 |
| DE-0440LE-0440 | 1 | 199 | 2 | 2396752.5 | 2506352.5 |

## The Mercury segments, cut alone over the released interval

| segment | bytes | gzip -9 | sha256 |
|---|---:|---:|---|
| 0 -> 1 | 3,237,888 | 3,092,259 | `ec67ba7110d93cdc2ad3b3ba9c99711b0e7cb5f20d3f04d18a58e1ce5e942360` |
| 1 -> 199 | 5,120 | 255 | `665982a15cd0e493ea62f79053830fc8701205f88ed7868e5d89598209e887a5` |

- 1 -> 199 is identically zero: 43185 states, position and velocity identically zero: the planet is its barycentre

## The kernel the builder would add (planets pack version 2)

- `juranometria-de440-mercury-1900-2100.bsp`: 3,299,328 bytes (3,112,736 gzip -9), sha256 `78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d`, written to `build/mercury-pack-study`
- juranometria-de440-mercury-1900-2100.bsp: 86370 states over the coverage at 1.7-day spacing, every segment: position and velocity identical to the source, worst difference 0.0
- the builder's own comment, interval, margins and grid; the internal name and the comment's version line say V2.

## The radii, read from the pinned PCK as the builder reads them

pck00011.tpc: 131,226 bytes, sha256 3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1

- `BODY199_RADII` = 2440.53,2440.53,2438.26
- `BODY299_RADII` = 6051.8,6051.8,6051.8
- `BODY10_RADII` = 695700.,695700.,695700.

Mercury's figure is triaxial in the PCK (equatorial 2440.53, 2440.53, polar 2438.26 km); Venus's and the Sun's are spheres. Which radius the apparent diameter is computed from is a question for the ruling; the authority comparison measures each against Horizons.

## The released packs, untouched

The study writes nothing under `src/resources`. The planets pack as committed:

| file | bytes | sha256 |
|---|---:|---|
| `NOTICE-planets.md` | 3,049 | `4c1866b14decce7f1379f17009efd4f6fd1449a26671390f24e5dc89f8d8a84c` |
| `PROVENANCE.md` | 3,004 | `1fac72abaaf6fa037412c10a3aa8f74bf6d26b84a6d69fc3b1f91c7670d022b6` |
| `juranometria-de440-venus-1900-2100.bsp` | 1,242,112 | `8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12` |
| `manifest.properties` | 1,274 | `c8cd97e88073eabffa554d1a32ddbaaef618c1c8c953656da6f173657cf783e4` |

The pack is 1,249,439 bytes; with Mercury's kernel it would be 4,548,767 (its notice, provenance and manifest growing by their Mercury lines). The released Solar System kernel's digest is `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580` and the Jovian kernels' are held by the builder (`PlanetsPackMain.RELEASED_*`), which refuses to write if any has moved; the Venus file above is the one the next version keeps byte for byte.
- `juranometria-jup365-galilean-2000-2100.bsp`: `6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6`
- `juranometria-jup365-jupiter-1900-2100.bsp`: `efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92`
