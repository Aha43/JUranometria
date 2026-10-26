# Notice: planets ephemeris pack

This pack carries **JUranometria-modified excerpts of the JPL
planetary ephemeris DE440**, one file per planet, so the atlas can
compute where the planets are, how large they look and how much
of each is lit, without any network access at build, test or run
time. It stands beside the Solar System pack and the Jovian System
pack, which it does not change: the Sun, the Earth and the Moon
are computed from the Solar System pack alone, as released, and
each planet's place is measured from them.

## One pack, one file per planet

The planets are one logical, versioned pack: one manifest, one
validation boundary, one notice and one provenance. Each planet
has a kernel file of its own, as ruled on #500: a later release
adds a planet's file and its lines in the manifest, and a planet's
file, once released, is never rewritten - its name and digest
stand.

## The ephemeris excerpts

`juranometria-de440-venus-1900-2100.bsp` was produced by
JUranometria from the official kernel `de440s.bsp` distributed by
NASA/JPL's Navigation and Ancillary Information Facility (NAIF).
It keeps two of that kernel's segments - the Venus barycentre
relative to the solar-system barycentre, and Venus relative to
its barycentre, which is identically zero because Venus has no
moon - over the years 1900 to 2100, with every coefficient
unchanged. It is a **modified kernel** under NAIF's rules and is
named, annotated and attributed as such; it is not an original JPL
file and JPL did not produce it.

`juranometria-de440-mercury-1900-2100.bsp` was produced the same
way from the same kernel, in pack version 2: it keeps the Mercury
barycentre relative to the solar-system barycentre, and Mercury
relative to its barycentre, identically zero because Mercury has
no moon, over the same years with every coefficient unchanged. It
too is a modified kernel, named, annotated and attributed as such.
Venus's file was not rewritten when Mercury's was added.

`juranometria-de440-mars-1900-2100.bsp` was produced the same way
from the same kernel, in pack version 3: it keeps one segment, the
Mars barycentre relative to the solar-system barycentre, over the
same years with every coefficient unchanged. That kernel carries
no segment for Mars's centre. NAIF's Mars satellite ephemeris
`mar099s.bsp` puts the centre within 0.210 m of the barycentre, so
the atlas takes the barycentre as the planet, as ruled on #601;
the builder measures that bound again before it writes. It too is
a modified kernel, named, annotated and attributed as such.
Neither Venus's nor Mercury's file was rewritten.

NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
by NAIF is permitted as long as they have not been modified." "If
a kernel distributed by NAIF has been modified in any way, any
embedded or otherwise allied attribution of the original kernel
producer must be replaced with the name and institution of
whomever has made the last modification." "The file name must
also be changed to help avoid confusion." SPICE data carry no
fees or licensing ("Technology and Software Publicly Available").

The ephemeris is the work of R. S. Park, W. M. Folkner,
J. G. Williams and D. H. Boggs, *The JPL Planetary and Lunar
Ephemerides DE440 and DE441*, The Astronomical Journal 161:105
(2021), doi:10.3847/1538-3881/abd414. JUranometria acknowledges
NASA/JPL NAIF and the DE440 authors as the source of these data.

## The planetary constants

The manifest carries, copied verbatim from NAIF's `pck00011.tpc`
(the IAU Working Group on Cartographic Coordinates and Rotational
Elements, 2015 report, as NAIF states it), each planet's radii and
the Sun's. Venus's radius, 6051.8 km, is its solid body; the
visible cloud deck lies higher, and the atlas states, never draws,
that difference. Mercury's radii are its triaxial figure,
2440.53 km, 2440.53 km and 2438.26 km; the atlas uses the
equatorial 2440.53 km, as JPL Horizons does, and never substitutes
the mean. Mars's radii are 3396.19 km at the equator and 3376.20 km
at the poles, the mean of the north and south polar radii, as the
PCK states; the atlas also copies Mars's pole and rotation model
in full, with its periodic terms. The PCK file itself is
not redistributed; its digest is recorded.

## Provenance

`PROVENANCE.md` beside this notice records, for each planet's
file, the source URL, retrieval date, upstream digest, the exact
extraction and its validation, the output digest and the coverage.
The manifest pins every file's SHA-256, verified before use.
