# Neptune study sources

Issue #654. The raw inputs of the Neptune system study were retrieved under the owner's approvals of 2026-10-29 ("approve request 1" through "approve request 4"), for measurement only. NAIF's files sit unmodified in the **main checkout's** ignored `imports/raw/neptune-system/` (`RETRIEVED.txt`), never in a worktree's (the #636 lesson). Each file is verified against its pin before every use. No excerpt is committed or bundled until the #654 checkpoint is ruled.

## Request 1: NAIF's satellite summary and checksums, retrieved 2026-10-29T13:51:54Z

| file | URL | bytes | SHA-256 |
|---|---|---:|---|
| `satellites-aa_summaries.txt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/aa_summaries.txt | 44153 | `a5356f7a76073b5ff7ae5b98af6cdbf43230771bf2e2a2ccf32cb47515b8a1ad` |
| `satellites-aa_checksums.txt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/aa_checksums.txt | 1847 | `bcf1491e062427558d1a00692d8e78656082debbb1d96e034d1e5810597cfdba` |

These are the same bytes as the Uranus study's request 1, because NAIF has not changed either file since 2026-10-27.

## Request 2: the comment and merge files, retrieved 2026-10-29T13:54:58Z

Each file's size equals the `Content-Length` that NAIF served, which the request stated before approval. URLs follow `https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/<file>`.

| file | bytes | SHA-256 |
|---|---:|---|
| `nep097.cmt` | 68241 | `2e8623de72f9d2ca66b85592af92dd2c8c314664239e795b87a39433166fb63f` |
| `nep097.mrg` | 480 | `c32e09c6457896e8986f8f04654d54d6bca0445a2d094b35a4b56a5ce1f2d81c` |
| `nep098_part-1.cmt` | 42361 | `3f89f88f0a247d4dc945501f512a4e6d5ed5c7288cd89f03822301befc7ade2f` |
| `nep098_part-1.mrg` | 632 | `ecfa849333cfeebb9ea81fef80d66b4a250a1bb56d7e8cd24ceab43d3c01d55c` |
| `nep098_part-2.cmt` | 42357 | `f9be7ab11ea6dd31ccdb310a98321b55c422b4e574f178987a47cb0ef85316a7` |
| `nep098_part-2.mrg` | 632 | `288f7235c0ba9dfef40e7831fd554da394ca9e6350819ad414878f7c951197ed` |
| `nep098_part-3.cmt` | 42356 | `228ccb63793a7133bace28f74af16829f72845bf135158e62aa92e9a94639690` |
| `nep098_part-3.mrg` | 632 | `596dc313abdd9b040960a2393f0abebcf12274662985a1917570bbc690ed5776` |
| `nep105.cmt` | 70729 | `c7459745cf3c5ead45efb5ecf419cadff76bc521486adaad1be6012967b28df6` |
| `nep105.mrg` | 523 | `514435791916119386fef19efc9d1d7ca4f14bddc5c5ac72d4cb1dff98f6614b` |

## Request 3: the Horizons fixtures, retrieved 2026-10-29

`scripts/horizons-neptune-fetch.py` stayed inside the approved bounds:

- **39 requests** (bound 45);
- **10,360,375 bytes committed** under `horizons/` (bound 12 MB);
- **8,196,593 bytes retained** outside Git under `~/juranometria-runs/neptune-horizons/` (bound 15 MB).

Each file's header records the full request URL, the retrieval time in UTC, and the body's SHA-256 and byte count. The run log is `~/juranometria-runs/runs/neptune-horizons-fetch-2026-10-29.log`. Every header names `{source: nep098_merged}` for Neptune and its moons.

## Request 4: the kernel, retrieved 2026-10-29T14:05:29Z

The kernel is pinned in `scripts/download-neptune-sources.sh`, together with its comment file and merge file above.

| file | URL | bytes | MD5 | SHA-256 |
|---|---|---:|---|---|
| `nep098_part-1.bsp` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/nep098_part-1.bsp | 821838848 | `e83c4bf7d362822d35fd6159f4f3a319` | `5327f47abba438d993237ff676ab03b728778a965bb238f86de883c0ad7790b8` |

- **Checksum.** NAIF's record (`satellites-aa_checksums.txt`) lists the file at that MD5, and the retrieved file matches it.
- **Contents, read from the kernel** (`contract/pack-measurements.md`). Every segment is SPK Type 2 in frame 1 (J2000).
  - Neptune (8 → 899), Triton (801), Nereid (802) and Naiad (803) are `NEP098`. Each body comes in two segments that meet at JD 2447830.5 (1989-11-01), and together they span JD 2378496.5 to 2524592.5 (1800-2199 TDB).
  - The Neptune barycentre (0 → 8) is `DE-0442LE-0442`.
- **The comment file** describes NEP098 as integrated orbits fit to Earth-based data, with the barycentre, the Sun and the Earth from DE442.

## Already held

- **`de440s.bsp`** and **`pck00011.tpc`**, the released packs' sources, pinned in `scripts/download-solar-system-sources.sh` and `scripts/download-jovian-sources.sh`.
- **`de442s.bsp`**, from the Venus study (`~/juranometria-runs/venus-raw/`), used for comparison only.
- **Espenak's almanacs** for 2003, 2012, 2018, 2026, 2027, 2035 and 2100, from the Venus and Mars studies. The Neptune rows are quoted in `neptune-events.txt`, with each page's digest.
- **The study's Python environment**, `~/juranometria-runs/venv`, from the earlier studies.

## Not requested

- `nep097.bsp` (105,262,080 bytes) carries NEP097's Triton on DE440. `nep105.bsp` (210,456,576 bytes) carries NEP105's Nereid on DE440. Both are superseded for this purpose by nep098, which carries both moons on DE442 and matches what Horizons serves.
- `nep098_part-2.bsp` and `nep098_part-3.bsp` (1,791,917,056 bytes each) carry the inner moons. Part 2 holds Thalassa, Despina and Galatea; part 3 holds Larissa, Proteus and Hippocamp.
- No ring authority was requested.

## Terms

- **NAIF**, as for every earlier pack (https://naif.jpl.nasa.gov/naif/rules.html): unmodified kernels may be redistributed. A modified kernel must name its last modifier and institution and must change its file name. There are no fees and no licensing.
- **JPL Horizons**: public responses, committed whole together with their requests.
- **Espenak's almanacs**: only the quoted rows are committed, with attribution.
