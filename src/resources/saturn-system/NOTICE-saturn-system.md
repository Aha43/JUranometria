# Notice: Saturn System ephemeris pack

This pack carries two **JUranometria-modified excerpts of the JPL
satellite ephemeris SAT441**, so the atlas can compute where Saturn,
its rings and seven of its moons are without any network access at
build, test or run time. It stands beside the Solar System, Jovian
and planets packs, which it does not change.

## One pack, two kernel files

The two kernels are one logical, versioned pack: one manifest, one
validation boundary, one notice and one provenance. They are two
files because their intervals differ, as ruled on #574: Saturn
answers over 1900 to 2100, the years the Sun and the Moon answer
for, while Mimas, Enceladus, Tethys, Dione, Rhea, Titan and Iapetus
answer over 2000 to 2100. Outside the moons' years Saturn still
answers and the moon configuration refuses, naming its interval.

## The ephemeris excerpts

`juranometria-sat441-saturn-1900-2100.bsp` and
`juranometria-sat441-moons-2000-2100.bsp` were produced by
JUranometria from the official kernel `sat441.bsp` distributed by
NASA/JPL's Navigation and Ancillary Information Facility (NAIF):
SAT441 for Saturn and its satellites about the Saturn barycentre,
merged by NAIF with DE440's Saturn barycentre about the
solar-system barycentre. The first keeps Saturn and the barycentre
over the years 1900 to 2100, the second the seven moons over 2000
to 2100, every coefficient unchanged. They are **modified kernels**
under NAIF's rules and are named, annotated and attributed as such;
they are not original JPL files and JPL did not produce them.

NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
by NAIF is permitted as long as they have not been modified." "If a
kernel distributed by NAIF has been modified in any way, any
embedded or otherwise allied attribution of the original kernel
producer must be replaced with the name and institution of whomever
has made the last modification." "The file name must also be
changed to help avoid confusion." "No fees or licensing are
required."

The satellite ephemeris is SAT441 by R. A. Jacobson (JPL Solar
System Dynamics), distributed by NAIF as `sat441.bsp`. The
barycentre is DE440's, the work of R. S. Park, W. M. Folkner,
J. G. Williams and D. H. Boggs, *The JPL Planetary and Lunar
Ephemerides DE440 and DE441*, The Astronomical Journal 161:105
(2021), doi:10.3847/1538-3881/abd414. JUranometria acknowledges
NASA/JPL NAIF, JPL Solar System Dynamics and the ephemeris authors
as the source of these data.

## The planetary constants and the rings

The manifest carries, copied verbatim, the radii of Saturn, the Sun
and the seven moons and Saturn's pole from NAIF's `pck00011.tpc`
(the IAU Working Group on Cartographic Coordinates and Rotational
Elements, 2015 report, as NAIF states it). The PCK file itself is
not redistributed; its digest is recorded.

The four ring radii the atlas draws - the C Ring's inner boundary,
the B Ring's outer boundary, the Cassini Division's outer boundary
and the A Ring's outer boundary - are quoted from the table "Rings
of Saturn" of the Planetary Data System's Ring-Moon Systems Node,
Vital Statistics for Saturn's Rings and Inner Satellites
(https://pds-rings.seti.org/saturn/saturn_tables.html), with the
footnoted sources that table cites. The page itself is not
redistributed with the application.

## Not included

Nothing of the IMCCE's Saturnian-phenomena predictions, software or
files is in this pack or anywhere in the application; the atlas has
no dependency on IMCCE, JPL Horizons or the PDS at build, test or
run time. The study that chose this pack quoted a few of IMCCE's
published event rows as test evidence, with attribution, in the
repository's study record only.

## Provenance

`PROVENANCE.md` beside this notice records the source URLs,
retrieval date, upstream digests, the exact extraction and its
validation, the output digests and the coverage. The manifest pins
every file's SHA-256, verified before use.
