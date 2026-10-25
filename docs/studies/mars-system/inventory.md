# Mars: the read-only inventory (#601, first action)

*Issue #601 on the Mars road #600, from clean main `eed9ccce`
(2026-10-25). Nothing was downloaded. NAIF's and astropixels.com's
directory listings were read, and sizes were taken from `HEAD`
responses (`Content-Length`, `Last-Modified`) only. Every number below
is one of those responses, or is read from a file committed in the
repository or retained on this machine. None is typed from memory.*

## What is in hand

| what | where | state |
|---|---|---|
| **The released planets pack** (Venus 2 → 299, Mercury 1 → 199, DE440, 1900–2100) | `src/resources/planets/` | carries **no Mars segment**. Its manifest pins `de440s.bsp` (sha256 `c1c7feea…`) and `pck00011.tpc` (sha256 `3dff7b1d…`), and copies only `BODY10`, `BODY199` and `BODY299` radii. |
| **The released Solar System pack** (Sun, EMB, Earth, Moon) | `src/resources/solar-system/` | no Mars segment |
| **DE442s**, comparison only (owner's ruling B on #500) | `~/juranometria-runs/venus-raw/de442s.bsp`, 32,701,440 bytes, sha256 `54d97562…` | holds the Mars barycentre (0 → 4), not Mars's centre (499) |
| **Espenak's almanacs** for 2012, 2026 and 2100 | `~/juranometria-runs/venus-raw/`, digests in `docs/studies/venus/venus-events.txt` | carry Mars rows (below) |
| the Saturn, Jovian, Venus and Mercury study tooling | `scripts/`, `juranometria.tool.*` | the shapes to mirror: SPK reader/excerpt writer, Horizons fetch, comparison, pack and contract study mains |

## What is no longer on this machine

The raw sources of the released packs were not restored with the
catalogue imports after #619. These files are absent from
`imports/raw/`: `solar-system/de440s.bsp`, `jovian-system/pck00011.tpc`
and `saturn-system/sat441.bsp`. Their pins are intact in
`scripts/download-solar-system-sources.sh` and
`scripts/download-jovian-sources.sh`. NAIF still serves both
at the pinned sizes:

| file | Content-Length | Last-Modified |
|---|---:|---|
| `spk/planets/de440s.bsp` | 32,726,016 | Mon, 21 Dec 2020 19:39:26 GMT |
| `pck/pck00011.tpc` | 131,226 | Tue, 27 Dec 2022 16:28:50 GMT |

Without them the study cannot measure the Mars barycentre from the
released authority, nor read Mars's, Phobos's and Deimos's radii and
poles from the released PCK.

## The Mars kernels NAIF serves

NAIF's `generic_kernels/spk/satellites/` holds one Mars family,
`mar099`, in two sizes:

| file | Content-Length | Last-Modified |
|---|---:|---|
| `mar099s.bsp` | 67,594,240 | Tue, 03 Jun 2025 00:01:57 GMT |
| `mar099s.cmt` | 72,577 | Tue, 03 Jun 2025 00:01:56 GMT |
| `mar099s.mrg` | 410 | Tue, 03 Jun 2025 00:01:56 GMT |
| `mar099.bsp` | 1,227,574,272 | Tue, 03 Jun 2025 00:02:08 GMT |
| `mar099.cmt` | 72,575 | Tue, 03 Jun 2025 00:01:57 GMT |
| `mar099.mrg` | 409 | Tue, 03 Jun 2025 00:01:57 GMT |
| `aa_summaries.txt` | 44,153 | Sun, 25 Oct 2026 00:12:50 GMT |
| `aa_checksums.txt` | 1,847 | (listing: 2026-10-24 17:27) |

Not yet known, and only readable from the files themselves:
- each kernel's segments, bodies (0 → 4, 4 → 499, 4 → 401, 4 → 402),
  type, frame and coverage;
- whether `mar099s` spans the released interval 1900–2100;
- whether `mar099`'s own Mars barycentre equals DE440's, as sat441's
  equals de440s's.

These facts decide whether the 1.2 GB kernel is needed at all. The
study asks for the small kernel and the summaries first.

## Named Mars cases already published in the retained almanacs

Read from Espenak's rows (GMT, to the hour; occultations to the
minute). The month is carried from the almanac's own month column.
The study tool re-reads them before any is used.

| year | rows |
|---|---|
| 2012 | aphelion Feb 15 21 (1.66598 AU); opposition Mar 03 19; Moon occults Mars Sep 19 20:36 |
| 2026 | conjunction with the Sun Jan 09 10; perihelion Mar 26 07 (1.38126 AU); Moon occults Mars Oct 05 05:30 and Nov 02 14:23 |
| 2100 | Moon occults Mars Jul 09 23:44; aphelion Jul 12 19 (1.66621 AU); Moon occults Mars Aug 07 15:15; conjunction with the Sun Oct 11 07 |

The retained years hold one opposition, and it is not a
favourable one. Four more almanacs from the same
publisher are available (sizes from `HEAD`):

| file | Content-Length |
|---|---:|
| `almanac01/almanac2003gmt.html` | 36,381 |
| `almanac01/almanac2018gmt.html` | 35,949 |
| `almanac21/almanac2027gmt.html` | 36,654 |
| `almanac21/almanac2035gmt.html` | 35,946 |

These years are candidates for the opposition cases: the close
oppositions the study expects in 2003, 2018 and 2035, and the next
opposition in 2027. The almanac rows and Horizons will confirm or
correct that expectation, not memory.

## Terms

**NAIF.** Rechecked on https://naif.jpl.nasa.gov/naif/rules.html on
2026-10-25. The rules are unchanged:
- "Redistribution of SPICE kernels distributed by NAIF is permitted as
  long as they have not been modified."
- A modified kernel must carry the modifier's attribution and a new file name.
- "No fees or licensing are required."

The released excerpts follow these rules.

**Horizons.** The public JPL SSD service, on the same terms as every
Horizons fixture already in the repository.

**astropixels.com (Espenak).** The same terms as the retained almanacs:
the rows are quoted with attribution, and the pages are kept outside Git.

## The download request

Every download needs the owner's prior approval. Requests 1–4 are
needed for the study to start measuring. Request 5 is conditional and
is not asked for now.

1. **Re-fetch of the two pinned sources.**
   - Source: NAIF, at the pinned URLs.
   - Purpose: the Mars barycentre (0 → 4) from the released DE440, and
     the IAU 2015 radii, pole and rotation of 499, 401 and 402.
   - Size: 32,726,016 + 131,226 bytes.
   - Destination: `imports/raw/solar-system/` and
     `imports/raw/jovian-system/`, gitignored, each verified against
     its existing SHA-256 pin.
   - Terms: NAIF's.
2. **`mar099s.bsp`, `.cmt` and `.mrg`, with the satellites directory's
   `aa_summaries.txt` and `aa_checksums.txt`.**
   - Source: NAIF `generic_kernels/spk/satellites/`.
   - Purpose: Mars's centre (4 → 499) and Phobos and Deimos (401, 402);
     their coverage, segmentation, record intervals and excerpt cost.
   - Size: 67,594,240 + 72,577 + 410 + 44,153 + 1,847 = 67,713,227 bytes.
   - Destination: `imports/raw/mars-system/`, gitignored and outside
     Git, pinned in a new `scripts/download-mars-sources.sh`.
   - Terms: NAIF's. Measurement only until ruled.
3. **JPL Horizons, Mars (499), Phobos (401), Deimos (402) and the Sun (10).**
   - Purpose: the in-family comparison, following the Venus and Saturn
     pattern.
     - named instants at the five established observers and
       geocentrically;
     - 7-day matrices over 1900–2100 for Mars, at Oslo and geocentric;
     - the moons' apparent X/Y and states over the interval
       `mar099s` covers;
     - hourly windows around the named oppositions, conjunctions and
       occultations;
     - dense windows for close moon configurations.
   - Size: an estimate, not a measurement, of about 40 requests. About
     10 MB would be committed under
     `docs/studies/mars-system/horizons/`. The geocentric matrices,
     about 6 MB, would be retained outside Git under
     `~/juranometria-runs/mars-horizons/`, as Venus and Saturn did. The
     real figures will be reported after retrieval.
   - Terms: Horizons'.
4. **Espenak's almanacs for 2003, 2018, 2027 and 2035.**
   - Source: astropixels.com, at the paths above.
   - Purpose: published times for the named oppositions and
     conjunctions.
   - Size: 36,381 + 35,949 + 36,654 + 35,946 = 144,930 bytes.
   - Destination: `~/juranometria-runs/mars-raw/`, outside Git. Only
     the quoted rows and digests would be committed.
5. **Conditional, not asked for now: `mar099.bsp`, 1,227,574,272 bytes.**
   This would be asked for only if `mar099s` does not cover the interval
   the study proposes, or if its Mars or moons need checking against the
   long kernel. It would be asked for separately, with the reason
   measured.
