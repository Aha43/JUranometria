# Saturn study sources

Issue #574. The raw inputs of the Saturn system study, retrieved under the
owner's approval of 2026-10-19 for measurement only. They are kept whole and
unmodified, gitignored and outside Git, under `imports/raw/saturn-system/`.
Each file is pinned in `scripts/download-saturn-sources.sh` and verified
against its pin before every use. No excerpt is built or committed until the
#574 checkpoint is ruled.

## Retrieved

Retrieved at 2026-10-19T01:52:24Z.

| file | URL | bytes | SHA-256 |
|---|---|---:|---|
| `sat441.bsp` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/sat441.bsp | 661592064 | `d7e444a9ba7a52b8f448ff0747030789524d2f0c1212e4e2bd7f5fc3c96444d5` |
| `sat441.cmt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/sat441.cmt | 67276 | `a45d31f6039854593395b539a3fb61df8f83ab13fb621c3ce5d710ed35629882` |
| `sat441.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/sat441.mrg | 406 | `9e6c4d2d7d72437c73efffd631265909973cf44b1d1717a9f1ca79604fa5adb2` |

NAIF's listing gave the files as 631M, 66K and 406 bytes, last modified
2022-01-29.

## Contents, read from the kernel

Every segment is SPK Type 2 in frame 1 (J2000). All cover 1749-12-30 to
2250-01-06 TDB.

- **Saturn system barycentre:** 0 → 6, one segment.
- **Saturn:** 6 → 699, two segments.
- **The eight candidate moons:** 6 → 601–608 (Mimas, Enceladus, Tethys, Dione,
  Rhea, Titan, Hyperion, Iapetus), two segments each.
- **Other moons, also present:** 6 → 609 (Phoebe), 612 (Helene), 613
  (Telesto), 614 (Calypso), 632 (Methone) and 634 (Polydeuces).
- **Context segments:** the Sun (0 → 10), the Earth–Moon barycentre (0 → 3)
  and the Earth (3 → 399).

This matches NAIF's `aa_summaries.txt`. Among NAIF's Saturn kernels, only
`sat441` carries both Saturn's centre and the eight major moons; `sat455`,
`sat456`, `sat457`, `sat459` and `sat480` carry Saturn but none of 601–608.

## Already held, from earlier studies

- **`de440s.bsp`:** pinned in `scripts/download-solar-system-sources.sh`.
  Holds Saturn's system barycentre (0 → 6) and no Saturn centre.
- **`pck00011.tpc`:** pinned in `scripts/download-jovian-sources.sh`.
  - Saturn: `BODY699_RADII = ( 60268 60268 54364 )`, plus its pole and prime
    meridian.
  - The radii and poles of bodies 601–609.
  - No ring constants.

## Terms

NAIF's rules, quoted from https://naif.jpl.nasa.gov/naif/rules.html on
2026-10-19, identical to the Jovian pack's precedent:

- "Redistribution of SPICE kernels distributed by NAIF is permitted as long
  as they have not been modified."
- "If a kernel distributed by NAIF has been modified in any way, any embedded
  or otherwise allied attribution of the original kernel producer must be
  replaced with the name and institution of whomever has made the last
  modification."
- "The file name must also be changed to help avoid confusion."
- "No fees or licensing are required."

## DE442s, for comparison only

The owner approved this on 2026-10-19, for the #574 comparison only. It is
not the Saturn authority and not a production dependency without a later
ruling. It answers one question: does the newest compact planetary
ephemeris materially move Saturn's system barycentre or the Earth from the
released DE440 foundation? The files are kept whole and unmodified,
gitignored, under `imports/raw/saturn-system/`. Each is pinned in
`scripts/download-saturn-sources.sh` and verified before every use.

| file | URL | retrieved (UTC) | bytes | SHA-256 |
|---|---|---|---:|---|
| `de442s.bsp` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de442s.bsp | 2026-10-19T08:35:14Z | 32701440 | `54d97562a5b094d298b1b8eafa5a2e17e3e010ce85e1a366d07f003ad159323c` |
| `de442s.mrg` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de442s.mrg | 2026-10-19T08:35:31Z | 260 | `7823b2fff1f737a252993459470602f259656f4a922ebf3ab5490d16c4474c7a` |
| `planets-aa_checksums.txt` | https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/aa_checksums.txt | 2026-10-19T08:35:32Z | 559 | `3c618bed7d51a48ee059269097eeeab1f69f3b2641ec2aeb96132757418c845b` |

- **Checksum.** NAIF's checksum record lists `de442s.bsp` at MD5
  `cc49327e06088124c0e39d8dde9f0b58`, and the retrieved file's MD5 matches.
  The record gives no checksum for the `.mrg`. Its own response headers give
  `Last-Modified: Thu, 06 Feb 2025 17:44:14 GMT`, and those of the kernel and
  `.mrg` give `Thu, 06 Feb 2025 17:36:42 GMT`.
- **Size.** Each byte count equals the `Content-Length` NAIF served, and the
  kernel's matches the 32,701,440 bytes asked about before approval.
- **Coverage.** This was read from the kernel. There are 14 segments, all
  Type 2 in frame 1 (J2000), each covering JD 2396752.5 to 2506352.5: 0 → 1
  to 10, 3 → 301, 3 → 399, 1 → 199 and 2 → 299. These are the same bodies
  and the same span as the released `de440s.bsp`, in a different segment
  order. The `.mrg` gives the span as 1849 DEC 26 to 2150 JAN 22 TDB, cut
  from `de442.bsp`.
- **Provenance.** NAIF's `de442_tech-comments.txt` was read, not retained.
  It says DE442 was integrated on 13 May 2024 and is "essentially an update
  of the planetary ephemeris DE440". It was built mainly to update the
  Uranus barycentre, and incorporates Uranus occultation data, further Mars
  orbiter ranging and four more years of Juno ranging.
- **Terms.** These are NAIF's rules above. Both quoted sentences were still
  on https://naif.jpl.nasa.gov/naif/rules.html on 2026-10-19.

## Not downloaded

DE441 was not approved; the owner said "Do not download DE441". It stays
unapproved unless the measurements raise a question that only it can answer.
The Horizons fixtures and the ring authority were approved separately. Their
records are `horizons/README.md` and `rings/SOURCES.md`.
