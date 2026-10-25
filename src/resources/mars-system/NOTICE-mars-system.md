# Notice: Mars System ephemeris pack

This pack carries one **JUranometria-modified excerpt of the JPL
satellite ephemeris MAR099**, so the atlas can compute where
Phobos and Deimos are without any network access at build, test
or run time. It stands beside the Solar System, Jovian, Saturn
System and planets packs, which it does not change. Mars itself
is in the planets pack.

## The moons' years

Phobos and Deimos answer from 2000 up to, but not including, the
first instant of 2050 (TDB), the end of NAIF's short Mars kernel,
as ruled on #601: the atlas speaks of them as 2000 to 2049. Mars
answers over 1900 to 2100 from the planets pack; outside the
moons' years Mars still answers and the moons refuse, naming
their years.

## The ephemeris excerpt

`juranometria-mar099-moons-2000-2050.bsp` was produced by
JUranometria from the official kernel `mar099s.bsp` distributed
by NASA/JPL's Navigation and Ancillary Information Facility
(NAIF): MAR099 for Phobos and Deimos about the Mars barycentre,
merged by NAIF with DE440. It keeps the two moons over the years
2000 to 2050, every coefficient unchanged. It is a **modified
kernel** under NAIF's rules and is named, annotated and attributed
as such; it is not an original JPL file and JPL did not produce
it.

NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
by NAIF is permitted as long as they have not been modified." "If a
kernel distributed by NAIF has been modified in any way, any
embedded or otherwise allied attribution of the original kernel
producer must be replaced with the name and institution of whomever
has made the last modification." "The file name must also be
changed to help avoid confusion." "No fees or licensing are
required."

The satellite ephemeris is MAR099, from JPL Solar System Dynamics,
distributed by NAIF as `mar099s.bsp`. The barycentre it is stated
about is DE440's, the work of R. S. Park, W. M. Folkner,
J. G. Williams and D. H. Boggs, *The JPL Planetary and Lunar
Ephemerides DE440 and DE441*, The Astronomical Journal 161:105
(2021), doi:10.3847/1538-3881/abd414. JUranometria acknowledges
NASA/JPL NAIF, JPL Solar System Dynamics and the ephemeris authors
as the source of these data.

## The planetary constants

The manifest carries, copied verbatim, the radii of Phobos and
Deimos from NAIF's `pck00011.tpc` (the IAU Working Group on
Cartographic Coordinates and Rotational Elements, 2015 report, as
NAIF states it). The PCK file itself is not redistributed; its
digest is recorded.

## Provenance

`PROVENANCE.md` beside this notice records the source URLs,
retrieval date, upstream digests, the exact extraction and its
validation, the output digest and the coverage. The manifest pins
every file's SHA-256, verified before use.
