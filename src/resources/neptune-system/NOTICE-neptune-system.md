# Notice: Neptune System ephemeris pack

This pack carries two **JUranometria-modified excerpts of NAIF's
Neptune satellite kernel `nep098_part-1.bsp`**, so the atlas can
compute where Neptune, Triton and Nereid are without any network
access at build, test or run time. It stands beside the Solar
System, Jovian, planets, Saturn System, Mars System and Uranus
System packs, which it does not change.

## The years

Neptune answers from 1900 to 2100. Triton and Nereid answer from
2000 to 2100, as ruled on #654; before 2000 Neptune still answers
and the moons refuse, naming their years.

## The ephemeris excerpts

`juranometria-de442-neptune-1900-2100.bsp` keeps the Neptune
barycentre about the solar-system barycentre from DE442, as NAIF
merged it into `nep098_part-1.bsp`; the atlas takes that
barycentre as the planet, Neptune's centre lying within 80 km of
it. `juranometria-nep098-moons-2000-2100.bsp` keeps Triton and
Nereid about the Neptune barycentre from the satellite solution
NEP098. Both were produced by JUranometria from the official
kernel distributed by NASA/JPL's Navigation and Ancillary
Information Facility (NAIF), every coefficient unchanged. They are
**modified kernels** under NAIF's rules and are named, annotated
and attributed as such; they are not original JPL files and JPL
did not produce them.

NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
by NAIF is permitted as long as they have not been modified." "If a
kernel distributed by NAIF has been modified in any way, any
embedded or otherwise allied attribution of the original kernel
producer must be replaced with the name and institution of whomever
has made the last modification." "The file name must also be
changed to help avoid confusion." "No fees or licensing are
required."

The satellite ephemeris is NEP098, from JPL Solar System
Dynamics, and the planetary ephemeris is DE442, from JPL,
distributed together by NAIF as `nep098_part-1.bsp`. JUranometria
acknowledges NASA/JPL NAIF, JPL Solar System Dynamics and the
ephemeris authors as the source of these data.

## The planetary constants

The manifest carries, copied verbatim, Neptune's radii, its pole
with the periodic term and its angles, and Triton's and Nereid's
radii from NAIF's `pck00011.tpc` (the IAU Working
Group on Cartographic Coordinates and Rotational Elements, 2015
report, as NAIF states it). The PCK file itself is not
redistributed; its digest is recorded.

## Provenance

`PROVENANCE.md` beside this notice records the source URLs,
retrieval date, upstream digests, the exact extraction and its
validation, the output digests and the coverage. The manifest pins
every file's SHA-256, verified before use.
