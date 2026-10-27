# Uranus: the read-only inventory (#633, first action)

*Issue #633 on the Uranus road #632, from clean main `6958ce75`
(2026-10-27, released as 5.5.0). Nothing was downloaded. NAIF's
directory listings were read at 2026-10-27T18:39:55Z, and sizes were
taken from `HEAD` responses (`Content-Length`, `Last-Modified`) only.
Every number below is one of those responses, or is read from a file
committed in the repository or retained on this machine. None is typed
from memory.*

## What is in hand

| what | where | state |
|---|---|---|
| **`de440s.bsp`**, the released planetary authority | `imports/raw/solar-system/`, sha256 `c1c7feea…` (pinned in `scripts/download-solar-system-sources.sh`) | holds the Uranus **barycentre** (0 → 7); whether it holds Uranus's centre (799) is to be read from the file |
| **`pck00011.tpc`**, the IAU 2015 constants | `imports/raw/jovian-system/`, sha256 `3dff7b1d…` | Uranus's radii and pole, and the five moons', to be read from the file |
| **`de442s.bsp`**, comparison only (ruling B on #500) | `~/juranometria-runs/venus-raw/` | holds the barycentres, not 799 |
| **Espenak's almanacs** for 2003, 2012, 2018, 2026, 2027, 2035 and 2100 | `~/juranometria-runs/mars-raw/`, `venus-raw/`, digests in `docs/studies/mars-system/mars-events.txt` and `docs/studies/venus/venus-events.txt` | each carries a *Uranus in Conjunction with Sun* row, and all but 2035 a *Uranus at Opposition* row: the named events need no new page |
| the Saturn, Jovian and Mars study tooling | `scripts/`, `juranometria.tool.*` | the shapes to mirror: SPK reader and excerpt writer, Horizons fetch, comparison, pack and contract study mains |

No Uranus satellite kernel, Horizons response or ring authority is on
this machine.

## The Uranus kernels NAIF serves

`generic_kernels/spk/satellites/`:

| file | Content-Length | Last-Modified |
|---|---:|---|
| `ura111xl-701.bsp` | 2,118,418,432 | Sun, 10 Apr 2022 20:29:15 GMT |
| `ura111xl-702.bsp` | 868,558,848 | Sun, 10 Apr 2022 20:29:23 GMT |
| `ura111xl-703.bsp` | 434,285,568 | Sun, 10 Apr 2022 20:29:27 GMT |
| `ura111xl-704.bsp` | 434,285,568 | Sun, 10 Apr 2022 20:29:31 GMT |
| `ura111xl-705.bsp` | 2,118,418,432 | Sun, 10 Apr 2022 20:29:51 GMT |
| `ura111xl-799.bsp` | 1,059,215,360 | Sun, 10 Apr 2022 20:30:00 GMT |
| `ura116xl.bsp` | 691,533,824 | Sun, 10 Apr 2022 20:30:10 GMT |
| `ura184_part-1.bsp` | 2,062,529,536 | Fri, 26 Sep 2025 13:42:39 GMT |
| `ura184_part-2.bsp` | 2,062,529,536 | Fri, 26 Sep 2025 13:42:57 GMT |
| `ura184_part-3.bsp` | 386,885,632 | Fri, 26 Sep 2025 13:43:01 GMT |

`generic_kernels/spk/satellites/a_old_versions/`, the smaller cuts:

| file | Content-Length | Last-Modified |
|---|---:|---|
| `ura111.bsp` | 169,928,704 | Thu, 09 Jan 2014 01:29:40 GMT |
| `ura111l.bsp` | 230,649,856 | Sun, 10 Apr 2022 20:28:55 GMT |
| `ura116.bsp` | (listing: 97M) | Wed, 06 Oct 2021 |
| `ura117.bsp` | (listing: 82M) | Thu, 29 Feb 2024 |
| `ura182.bsp` | (listing: 714M) | Thu, 06 Feb 2025 |

The `ura111xl-NNN` names suggest one file per body (701 Ariel, 702
Umbriel, 703 Titania, 704 Oberon, 705 Miranda, 799 Uranus), but that
is a name, not a measurement. **Not yet known, and only readable from
NAIF's own summary and the comment files:** which kernel carries
Uranus's centre and all five major moons; each one's theory, interval
and segment type; whether `ura184` supersedes `ura111` for the major
moons or covers other bodies; and which family a bounded excerpt over
the atlas's years could be cut from. The pack cost the issue asks for
can be measured only from a real cut file.

## Rings

The PDS Ring-Moon Systems Node serves
`https://pds-rings.seti.org/uranus/uranus_tables.html` (HTML,
Last-Modified Mon, 14 Mar 2022 22:17:18 GMT, no Content-Length), and
NSSDCA `https://nssdc.gsfc.nasa.gov/planetary/factsheet/uranringfact.html`
(no Content-Length). Whether either gives authoritative radii for the
principal rings, the ε ring above all, is to be read from the pages.

## What the first approval would allow

Only descriptive files, to choose the kernel before any kernel is
asked for: NAIF's satellite summary and checksums, the `.cmt` and
`.mrg` files of `ura184`, `ura111xl` (799), `ura116xl`, `ura111l`,
`ura182` and `ura117` - 478,499 bytes in all, by `HEAD` - and the two
ring pages. The kernel, the Horizons fixtures and anything to be
committed are separate requests after them.

## After requests 1 and 2 (approved 2026-10-27, "approve requests 1 and 2")

Retrieved at the sizes asked about (478,499 bytes from NAIF; the ring
pages 17,789 and 4,338 bytes), digests in `imports/raw/uranus-system/RETRIEVED.txt`
and `~/juranometria-runs/uranus-raw/RETRIEVED.txt`. What they settle,
read from the files:

- **`ura184_part-3.bsp`** (NAIF, 2025-09-25) carries Uranus (7 → 799)
  and **all five major moons** (7 → 701-705), with nine irregular moons
  and S/2023 U 1, over **1600 JAN 04 - 2399 DEC 17 TDB**; the satellites
  from URA182.21 (fit to data, DE442), the Uranus barycentre, Sun and
  Earth from DE442. NAIF's checksum list gives it MD5
  `cf24471fb9037543cdbe168bc0fd4c2e`.
- **Parts 1 and 2** hold only the small inner moons (706-715, 725-727).
- **`ura111xl-*`** cover 12001 BC - 17000 AD (URA111, DE441), hence
  their size; **`ura111l.bsp`** (old versions) is URA111 for 701-705 and
  799 over 1600-2399 with DE440's planets: the superseded solution.
- **The rings.** Both pages give the ε ring's dimensions (PDS: semimajor
  axis, modes; NSSDCA: radius, width), with sources; read in the study.
