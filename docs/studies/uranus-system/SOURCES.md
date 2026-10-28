# Uranus study sources

Issue #633. The raw inputs of the Uranus system study, retrieved under the owner's approvals of 2026-10-27 ("approve requests 1 and 2", "approve requests 3 and 4") for measurement only. They are kept whole and unmodified, gitignored and outside Git. Each is verified against its pin before every use. No excerpt is committed or bundled until the #633 checkpoint is ruled.

**Correction (#636, 2026-10-28).** `imports/raw/uranus-system/` below was the #633 worktree's, and removing that worktree deleted it. On 2026-10-27 `ura184_part-3.bsp`, `.cmt` and `.mrg` were re-fetched, approved, into the main checkout's ignored `imports/raw/uranus-system/` (`RETRIEVED.txt`), the kernel's MD5 and SHA-256 matching the pins below. Request 1's other descriptive files were not re-fetched; no tool reads them. See `docs/decisions/uranus-handover.md`, §1.1.

## Request 1: NAIF's descriptive files, retrieved 2026-10-27T18:43:54Z

Under `imports/raw/uranus-system/`, 478,499 bytes in all, each equal to the `Content-Length` NAIF served and asked about before approval. They chose the kernel; none is read by any study tool.

| file | URL | bytes | SHA-256 |
|---|---|---:|---|
| `satellites-aa_summaries.txt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/aa_summaries.txt | 44153 | `a5356f7a76073b5ff7ae5b98af6cdbf43230771bf2e2a2ccf32cb47515b8a1ad` |
| `satellites-aa_checksums.txt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/aa_checksums.txt | 1847 | `bcf1491e062427558d1a00692d8e78656082debbb1d96e034d1e5810597cfdba` |
| `ura184_part-1.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura184_part-1.cmt | 64242 | `fcbac3437bf1868edf3a8b82c5f5854306189d64896d5a264d0896cd0c0fde5c` |
| `ura184_part-2.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura184_part-2.cmt | 64247 | `f4affb6041c58958c5e84ca89aff0318f209cd32cb7edc3831b5340b6a2e83e2` |
| `ura184_part-3.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura184_part-3.cmt | 64350 | `c9f4f0ae15b759ea6f5dcc652922629d23b4baa7d10615e0db2766a167d6323f` |
| `ura184_part-1.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura184_part-1.mrg | 478 | `121c005ae589857346ebdff037ce7670ebf08c10330184a09995c4e3a5812d96` |
| `ura184_part-2.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura184_part-2.mrg | 480 | `3c1fbe5d687d7baeb5bbade2b7ff5ff360a48b2b9ed2476a737c1639e5d6c33d` |
| `ura184_part-3.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura184_part-3.mrg | 520 | `19e86c20aad79697930152c1e7345c9af0e8f834c4bd111a85c6f19396b322f5` |
| `ura111xl-799.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura111xl-799.cmt | 7570 | `6d4d803197ffe9990cf92ecac696c937f1527bb86c1c236def946887782f99d4` |
| `ura111xl-799.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura111xl-799.mrg | 317 | `7dfdabd9b854d4ae19884b4e86d8bea9634dbf7358e7781688ceb21a564b9a0d` |
| `ura116xl.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura116xl.cmt | 4400 | `02d14d0e9aa0f4d73ed7f441e8abb8af21f4cb82f1a92177aa6ffb6ce2450b76` |
| `old-ura111l.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/a_old_versions/ura111l.cmt | 106315 | `8a092a1bd0efdff4992962f02fe1789be7752e9bae00608968256adf86fb08d7` |
| `old-ura111l.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/a_old_versions/ura111l.mrg | 760 | `c4d146fa6f11c91a112690f39555fec6943dd09420cfbba9dcd480e9a7899308` |
| `old-ura182.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/a_old_versions/ura182.cmt | 41595 | `692e6676cb68a4c87e46dcdb145ffbfac7b6ab465a994ee732b70a47b19af40d` |
| `old-ura182.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/a_old_versions/ura182.mrg | 466 | `6048ca048296752203f37fa2deca0d30cb201dc8041aaec8435c61cfa6068c57` |
| `old-ura117.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/a_old_versions/ura117.cmt | 76208 | `4bb4920c58456185ec9c9cf034d04d18e268c750a8934c43b2b84731a1cb8a8a` |
| `old-ura117.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/a_old_versions/ura117.mrg | 551 | `fe95007307d4e041c57bc927a4faab9b2e4895d85fd2c28ceaf7d14c9348ecd6` |

## Request 3: the kernel, retrieved 2026-10-27T18:47:44Z to 18:48:26Z

Under `imports/raw/uranus-system/`, pinned in `scripts/download-uranus-sources.sh` (with its comment and merge files above).

| file | URL | bytes | MD5 | SHA-256 |
|---|---|---:|---|---|
| `ura184_part-3.bsp` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/ura184_part-3.bsp | 386885632 | `cf24471fb9037543cdbe168bc0fd4c2e` | `273cd4ccc470d1098562cd4e94fa48fc2b4c6b1c2896308e473c949b87937c53` |

- **Checksum.** NAIF's record (`satellites-aa_checksums.txt`) lists `ura184_part-3.bsp` at MD5 `cf24471fb9037543cdbe168bc0fd4c2e`, and the retrieved file's MD5 matches.
- **Contents, read from the kernel** (`contract/pack-measurements.md`). Every segment is SPK Type 2 in frame 1 (J2000). Uranus (7 → 799) and Miranda, Ariel, Umbriel, Titania and Oberon (7 → 705, 701-704) are `URA182.21`, each in two segments meeting at JD 2446278.5 (1985-08-01) and together spanning JD 2305450.5 to 2597626.5 (1600-01-04 to 2399-12-17 TDB); the Uranus barycentre (0 → 7) is `DE-0442LE-0442` over the same span; nine irregular moons and S/2023 U 1 are `URA117`.
- **The comment file** names the satellite solution `URA182.21`, fit to data, on DE442.

## Request 2: the ring pages, retrieved 2026-10-27

Under `~/juranometria-runs/uranus-raw/`, outside Git. Neither server sent a size; each is under the 1 MB bound asked about.

| file | URL | bytes | SHA-256 |
|---|---|---:|---|
| `pds-rings.seti.org_uranus_uranus_tables.html` | https://pds-rings.seti.org/uranus/uranus_tables.html | 17789 | `5ff82c5742e00732113bf0be6a7f0129caff1c3c9949f6128f1e78ec81e21146` |
| `nssdc.gsfc.nasa.gov_planetary_factsheet_uranringfact.html` | https://nssdc.gsfc.nasa.gov/planetary/factsheet/uranringfact.html | 4338 | `dbbe304c6cd6927fbb8ac94b8451ec67f2be56e6e3762bfb5c4c213e2488093e` |

## Request 4: the Horizons fixtures, retrieved 2026-10-27

By `scripts/horizons-uranus-fetch.py`, inside the approved bounds: **59 requests** (bound 60), **11,349,249 bytes committed** under `horizons/` (bound 15 MB), **15,528,721 bytes retained** outside Git under `~/juranometria-runs/uranus-horizons/` (bound 30 MB). Each file's header records the full request URL, the retrieval UTC, and the body's SHA-256 and byte count; the run log is `~/juranometria-runs/runs/uranus-horizons-fetch-2026-10-27.log`.

## Already held

- **`de440s.bsp`** and **`pck00011.tpc`**, the released packs' sources, pinned in `scripts/download-solar-system-sources.sh` and `scripts/download-jovian-sources.sh`.
- **`de442s.bsp`**, from the Venus study (`~/juranometria-runs/venus-raw/`, sha256 `54d97562…`), for comparison only.
- **Espenak's almanacs** for 2003, 2012, 2018, 2026, 2027, 2035 and 2100, from the Venus and Mars studies; the Uranus rows are quoted in `uranus-events.txt` with each page's digest.
- **The study's Python environment**, `~/juranometria-runs/venv` (Skyfield 1.55, NumPy 2.5.3), from the earlier studies.

## Terms

- **NAIF**, as for every earlier pack (https://naif.jpl.nasa.gov/naif/rules.html): redistribution of unmodified kernels is permitted; a modified kernel must name its last modifier and institution and change its file name; no fees or licensing.
- **JPL Horizons**: public responses, committed whole with their requests, as for Saturn and Mars.
- **The ring pages** state no terms; any value used is quoted with its source and footnotes, as Saturn's were.
- **Espenak's almanacs**: only the quoted rows are committed, with attribution.
