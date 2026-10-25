# Mars study sources

Issue #601. The raw inputs of the Mars system study, retrieved under the owner's approval of 2026-10-25 ("approve 1–4") for measurement only. They are kept whole and unmodified, gitignored and outside Git. Each is verified against its pin before every use. No excerpt is committed or bundled until the #601 checkpoint is ruled.

## NAIF, retrieved 2026-10-25T16:52:00Z

Under `imports/raw/mars-system/`, pinned in `scripts/download-mars-sources.sh`. Each byte count equals the `Content-Length` NAIF served, and the figure asked about before approval.

| file | URL | bytes | SHA-256 |
|---|---|---:|---|
| `mar099s.bsp` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/mar099s.bsp | 67594240 | `997dc93ba640e476da7a494d2237dcdeb145e528db37be8ccee588c615e4e1ff` |
| `mar099s.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/mar099s.cmt | 72577 | `d39cf30469cf969992587ca8c92fef739033a6aa1c3d02f89323ed0b76ec16ea` |
| `mar099s.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/mar099s.mrg | 410 | `cf68f8d0bb349dc85db6da141f62af2ad426b22847f919ded1668ddf6d18f591` |
| `satellites-aa_summaries.txt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/aa_summaries.txt | 44153 | `a5356f7a76073b5ff7ae5b98af6cdbf43230771bf2e2a2ccf32cb47515b8a1ad` |
| `satellites-aa_checksums.txt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/aa_checksums.txt | 1847 | `bcf1491e062427558d1a00692d8e78656082debbb1d96e034d1e5810597cfdba` |

- **Checksum.** NAIF's record lists `mar099s.bsp` at MD5 `fd7302dfbaa0c63ce85b1e98923ee6a1`, and the retrieved file's MD5 matches.
- **Contents, read from the kernel** (`contract/pack-measurements.md`). Every segment is SPK Type 2 in frame 1 (J2000), JD 2449718.5 to 2469807.5, which is 1995-01-01 to 2050-01-01 TDB:
  - Phobos (4 → 401), Deimos (4 → 402) and Mars (4 → 499), `MAR099.01`;
  - the Earth (3 → 399), the Sun (0 → 10), the Mars barycentre (0 → 4) and the Earth–Moon barycentre (0 → 3), `DE-0440LE-0440`.
- **The merge file** names the source `mar099.bsp` and DE440's bodies 4, 10, 3 and 399, from 1995 JAN 01 to 2050 JAN 01 TDB.
- **NAIF's summary** gives `mar099.bsp` the same bodies over 1600 JAN 01 to 2600 JAN 02.

## The released packs' own sources, re-fetched 2026-10-25T16:51:45Z

Under their existing pins, unchanged, because the raw files were no longer on this machine (`inventory.md`).

| file | pinned in | bytes | SHA-256 |
|---|---|---:|---|
| `imports/raw/solar-system/de440s.bsp` | `scripts/download-solar-system-sources.sh` | 32726016 | `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` |
| `imports/raw/jovian-system/pck00011.tpc` | `scripts/download-jovian-sources.sh` | 131226 | `3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1` |

Only these two files were fetched. The scripts' other pins (`jup365`, the leap-second file) were not re-fetched.

## Already held

- **`de442s.bsp`**, from the Venus study (`~/juranometria-runs/venus-raw/`, sha256 `54d97562…`), for comparison only.
- **Espenak's almanacs** for 2012, 2026 and 2100, from the Venus study (`docs/studies/venus/venus-events.txt`).

## Espenak's almanacs, retrieved 2026-10-25T16:53:53Z

Under `~/juranometria-runs/mars-raw/`, outside Git. Only the quoted rows in `mars-events.txt` are committed.

| file | URL | bytes | SHA-256 |
|---|---|---:|---|
| `almanac2003gmt.html` | https://astropixels.com/almanac/almanac01/almanac2003gmt.html | 36381 | `50811f3be0f29b6de42cd4f3ad2bdbc3c296b9ced7c3807d8e08cff8c5fd4cc3` |
| `almanac2018gmt.html` | https://astropixels.com/almanac/almanac01/almanac2018gmt.html | 35949 | `8f0d441a2b224cef1af9ce0544274d3f97d1d758a1a3525ccab50a95f2b830be` |
| `almanac2027gmt.html` | https://astropixels.com/almanac/almanac21/almanac2027gmt.html | 36654 | `af77389f672434ed752163a351533ce68ba31b51a030663795e76ed8388f9540` |
| `almanac2035gmt.html` | https://astropixels.com/almanac/almanac21/almanac2035gmt.html | 35946 | `0af51c5ed77b28875654ebd91f97c9ee086857ebca3eaabb847545fb3f397fdd` |

## Terms

NAIF's rules, quoted from https://naif.jpl.nasa.gov/naif/rules.html on 2026-10-25, unchanged since the Saturn study:
- "Redistribution of SPICE kernels distributed by NAIF is permitted as long as they have not been modified."
- A kernel modified in any way must carry the name and institution of whoever modified it last.
- "The file name must also be changed to help avoid confusion."
- "No fees or licensing are required."
