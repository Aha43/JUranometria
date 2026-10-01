# Notice: Solar System ephemeris pack

This pack carries a **JUranometria-modified excerpt of the JPL
planetary ephemeris DE440** and the **IERS leap-second file**,
so the atlas can compute where the Sun and the Moon are without
any network access at build, test or run time.

## The ephemeris excerpt

`juranometria-de440-sun-emb-earth-moon-1900-2100.bsp` was produced
by JUranometria from the official kernel `de440s.bsp` distributed
by NASA/JPL's Navigation and Ancillary Information Facility (NAIF).
It keeps four of that kernel's segments - the Sun and the
Earth-Moon barycentre relative to the solar-system barycentre,
and the Earth and the Moon relative to the Earth-Moon barycentre -
over the years 1900 to 2100, with every coefficient unchanged. It is a
**modified kernel** under NAIF's rules and is named, annotated
and attributed as such; it is not an original JPL file and JPL
did not produce it.

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

## The leap-second file

`Leap_Second.dat` is the IERS Earth Orientation Centre's
(Observatoire de Paris) published record of TAI - UTC, bundled
**unmodified**, including the validity date it states for itself.
Its expiry is the boundary of the atlas's exact time reading, not
a prediction about leap seconds; a later file moves that boundary
through this pack's provenance.

## Provenance

`PROVENANCE.md` beside this notice records the source URLs,
retrieval date, upstream digests, the exact extraction and its
validation, the output digest and the coverage. The manifest
pins every file's SHA-256, verified before use.
