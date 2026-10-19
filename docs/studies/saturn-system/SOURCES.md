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

## IMCCE's Saturnian satellite phenomena, for comparison only

The owner approved this on 2026-10-19. These are IMCCE's predictions of the eclipses, occultations, transits and shadow transits of Saturn's first eight satellites. They were computed by W. Thuillot from the TASS 1.6 theory (Vienne & Duriez 1995), independent of JPL's sat441. They are used only to compare event times for #574.

The nine files are kept whole and unmodified, gitignored, under `imports/raw/saturn-system/`. Each is pinned in `scripts/download-saturn-sources.sh` and verified before every use. They are never committed, bundled in the application or republished. Only the minimum quoted rows are committed, with attribution and the source file's digest. Each was fetched with `curl -fsS -D <file>.headers`, and the response headers are kept beside each file.

| file | URL | retrieved (UTC) | bytes | SHA-256 |
|---|---|---|---:|---|
| `imcce-phensaturne-README.txt` | https://ftp.imcce.fr/pub/ephem/satel/phensaturne/README.txt | 2026-10-19T09:06:16Z | 4803 | `9fba712de344e3dd76b205d089d903fc464a9331fb7c4aa6c11073589630c637` |
| `imcce-phensaturne-phenE-S1.dat` | https://ftp.imcce.fr/pub/ephem/satel/phensaturne/phenE-S1.dat | 2026-10-19T09:06:17Z | 878200 | `fa462f76bce97e89441da5ea58b992ad5c2e6d1d0db62ba3cf56905cf8937145` |
| `imcce-phensaturne-phenE-S2.dat` | https://ftp.imcce.fr/pub/ephem/satel/phensaturne/phenE-S2.dat | 2026-10-19T09:06:17Z | 462442 | `7fc95bdfc246f5cee825397604e39c01d2b1d8a13d244ef458b61cfef765626c` |
| `imcce-phensaturne-phenE-S3.dat` | https://ftp.imcce.fr/pub/ephem/satel/phensaturne/phenE-S3.dat | 2026-10-19T09:06:18Z | 268243 | `ffe39d0bbace5edb145409c0e1df4213689a839e02a0ca19a1e15261d0f1ce7b` |
| `imcce-phensaturne-phenE-S4.dat` | https://ftp.imcce.fr/pub/ephem/satel/phensaturne/phenE-S4.dat | 2026-10-19T09:06:18Z | 146206 | `0a8431191a27be84acd8d85a7e58046edfe57a89a75faa5750ae543c05c1c302` |
| `imcce-phensaturne-phenE-S5.dat` | https://ftp.imcce.fr/pub/ephem/satel/phensaturne/phenE-S5.dat | 2026-10-19T09:06:18Z | 59794 | `30a04db8a9a75b5a8ccf851b0003559ee89f8891c95cf5c0c9327e6243cb7661` |
| `imcce-phensaturne-phenE-S6.dat` | https://ftp.imcce.fr/pub/ephem/satel/phensaturne/phenE-S6.dat | 2026-10-19T09:06:18Z | 8722 | `bb1fafd2855dcc343c3a0bcc6b5211c3a0887eb3c7e1155df7c91e86178d04c8` |
| `imcce-phensaturne-phenE-S7.dat` | https://ftp.imcce.fr/pub/ephem/satel/phensaturne/phenE-S7.dat | 2026-10-19T09:06:19Z | 5302 | `0961de801d13c8c33b467906311134c47b3ec21cb9ee01a81d2a448d598aaf82` |
| `imcce-phensaturne-phenE-S8.dat` | https://ftp.imcce.fr/pub/ephem/satel/phensaturne/phenE-S8.dat | 2026-10-19T09:06:19Z | 1198 | `59e88532bfb959c65cbc70695f61fff3c247912a715dd5994426af0d192e81b2` |

- **Size.** The total is 1,834,910 bytes, the figure stated before approval. Each file equals the `Content-Length` served. The server gives `Last-Modified` dates of 4 October 2008 for the data files and 10 October 2008 for the README.
- **Contents.** The README was compared byte for byte with the one read before approval, and is identical. It says the predictions are "in Terrestrial Time" and cover "the season 2006-2013". They are "based on the TASS 1.6 ephemerides (Vienne & Duriez, 1995)", and "Each event is computed for the satellite centre". The years in the data files run from 2006 to 2013. Satellite 8, Iapetus, has events only in 2006–2007.
- **Events.** The data files list 32,099 events in all: 15,406 for Mimas, 8,112 Enceladus, 4,705 Tethys, 2,564 Dione, 1,048 Rhea, 152 Titan, 92 Hyperion and 20 Iapetus. Each is given as EC.D/EC.R, OC.D/OC.R, TR.I/TR.E or SH.I/SH.E, as the mid-event time in TT.
- **Attribution.** IMCCE (Institut de mécanique céleste et de calcul des éphémérides), Paris Observatory. The predictions are by W. Thuillot, after Thuillot (1983, A&A 127, 63-67), from TASS 1.6 (Vienne & Duriez 1995, A&A 297, 588-605).
- **Terms.** Quoted from https://www.imcce.fr/mentions-legales, read on 2026-10-19: "The data contained in the Web and FTP servers of the IMCCE can be used as an example to illustrate documents if the source is clearly identified." The same page says "Requests for the reproduction of data or publications from the site of the IMCCE are made using the form provided for this purpose", so the files are not reproduced. The README and the directory listing both say "THE USE OF THESE DATA IS SUBJECTED TO CONDITIONS", pointing to IMCCE's copyright page.

## Not downloaded

DE441 was not approved; the owner said "Do not download DE441". It stays
unapproved unless the measurements raise a question that only it can answer.
The Horizons fixtures (and their magnitude run), the ring authority and the IMCCE predictions were approved separately. Their
records are `horizons/README.md`, `rings/SOURCES.md` and the section above.
