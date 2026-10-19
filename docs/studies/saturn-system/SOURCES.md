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

## Still to be asked, separately

- the JPL Horizons fixtures for the comparison matrix
- a published ring authority, since PCK holds no ring constants
- DE441, if the newest-ephemeris comparison needs it
