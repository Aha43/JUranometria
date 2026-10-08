# Notice: Jovian System ephemeris pack

This pack carries two **JUranometria-modified excerpts of the JPL
satellite ephemeris JUP365**, so the atlas can compute where
Jupiter and its four Galilean moons are without any network
access at build, test or run time. It stands beside the Solar
System pack, which it does not change: the Sun and the Moon are
computed from that pack alone, as released.

## One pack, two kernel files

The two kernels are one logical, versioned pack: one manifest,
one validation boundary, one notice and one provenance. They are
two files because their intervals differ, as ruled on #472:
Jupiter answers over 1900 to 2100, the years the Sun and the Moon
answer for, while the four moons answer over 2000 to 2100 - the
Galilean excerpt over the whole 1900-2100 would be 91 MB, and the
ruled interval keeps it at 39 MB. Outside the moons' years Jupiter
still answers and the moon configuration refuses, naming its
interval.

## The ephemeris excerpts

`juranometria-jup365-jupiter-1900-2100.bsp` and
`juranometria-jup365-galilean-2000-2100.bsp` were produced by
JUranometria from the official kernel `jup365.bsp` distributed by
NASA/JPL's Navigation and Ancillary Information Facility (NAIF):
JUP365 for Io, Europa, Ganymede, Callisto and Jupiter about the
Jupiter barycentre, merged by NAIF with DE440's Jupiter barycentre
about the solar-system barycentre. The first keeps Jupiter and
the barycentre over the years 1900 to 2100, the second the four
moons over 2000 to 2100, every coefficient unchanged. They are
**modified kernels** under NAIF's rules and are named, annotated
and attributed as such; they are not original JPL files and JPL
did not produce them.

NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
by NAIF is permitted as long as they have not been modified." "If
a kernel distributed by NAIF has been modified in any way, any
embedded or otherwise allied attribution of the original kernel
producer must be replaced with the name and institution of
whomever has made the last modification." "The file name must
also be changed to help avoid confusion." SPICE data carry no
fees or licensing ("Technology and Software Publicly Available").

The satellite ephemeris is JUP365 by R. A. Jacobson (JPL Solar
System Dynamics, 2021), distributed by NAIF as `jup365.bsp`
("JUP365 Satellite Ephemeris with Jupiter barycenter (5), Sun
(10), Earth Moon barycenter (3), and Earth (399) from DE440",
NAIF, 2021-03-14). The barycentre is DE440's, the work of
R. S. Park, W. M. Folkner, J. G. Williams and D. H. Boggs, *The
JPL Planetary and Lunar Ephemerides DE440 and DE441*, The
Astronomical Journal 161:105 (2021), doi:10.3847/1538-3881/abd414.
JUranometria acknowledges NASA/JPL NAIF, JPL Solar System Dynamics
and the ephemeris authors as the source of these data.

## The planetary constants

The manifest carries, copied verbatim, the radii of Jupiter, the
Sun and the four moons and Jupiter's pole from NAIF's
`pck00011.tpc` (the IAU Working Group on Cartographic Coordinates
and Rotational Elements, 2015 report, as NAIF states it). The PCK
file itself is not redistributed; its digest is recorded.

## Not included

Nothing of the IMCCE's Galilean-phenomena catalogue, software or
files is in this pack or anywhere in the application; the atlas
has no dependency on IMCCE at build, test or run time. The study
that chose this pack quoted a few of IMCCE's published event rows
as test evidence, with attribution, in the repository's study
record only.

## Provenance

`PROVENANCE.md` beside this notice records the source URLs,
retrieval date, upstream digests, the exact extraction and its
validation, the output digests and the coverage. The manifest
pins every file's SHA-256, verified before use.
