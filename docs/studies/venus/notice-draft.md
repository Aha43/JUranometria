# Venus pack: the notice, provenance and manifest, drafted (#500)

A draft for the owner's pack ruling, not a released file. The numbers are
`pack-measurements.md`'s (`make venus-pack-study`). Nothing below is in
`src/resources/` until #501 builds it on the ruled layout. The owner returned
the pack once (ruling 2) for Layout C, measured in `pack-measurements.md`.

## The terms, re-verified (2026-10-11)

Venus needs **no new source**. Both segments it needs are in the released
Solar System pack's own pinned source, `de440s.bsp` (NASA/JPL NAIF, sha256
`c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2`,
`scripts/download-solar-system-sources.sh`): 0 → 2, the Venus barycentre
about the solar-system barycentre, and 2 → 299, Venus about its barycentre,
which is identically zero (Venus has no moon; proved at 43 222 epochs). The
radius is the PCK's, `pck00011.tpc` (already pinned by the Jovian pack):
`BODY299_RADII = ( 6051.8 6051.8 6051.8 )`, and the Sun's
`BODY10_RADII = ( 695700. 695700. 695700. )` for the in-front-of and
behind-the-Sun states.

NAIF's rules, re-read on <https://naif.jpl.nasa.gov/naif/rules.html> on
2026-10-11 and unchanged from the released notice's quotations:
"Redistribution of SPICE kernels distributed by NAIF is permitted as long as
they have not been modified." "If a kernel distributed by NAIF has been
modified in any way, any embedded or otherwise allied attribution of the
original kernel producer must be replaced with the name and institution of
whomever has made the last modification." "The file name must also be
changed to help avoid confusion." SPICE is "Technology and Software Publicly
Available"; no fees or licensing are required.

DE442 (2025) is not proposed: over 1900-2100 it moves Venus's geocentric
direction by at most 0.006″ from DE440 (`authority-comparison.md`), so mixing
authorities buys nothing. `de442s.bsp` stays a study download only.

The AstroPixels rows quoted in `venus-events.txt` are named-case timing only:
quoted with attribution, URL, retrieval date and digest, never bundled,
never a build or run-time dependency.

## Layout C (recommended, returned for the pack ruling): one `planets` pack, a kernel per planet

The released Solar System and Jovian packs stay byte-identical (their digests
are in `pack-measurements.md`). One new logical pack, `planets`, version 1,
first holding Venus, gaining one kernel file per planet across later 5.x
releases (Mercury, Mars, Saturn, Uranus, Neptune, Pluto), each released
file never rewritten:

- `juranometria-de440-venus-1900-2100.bsp` - 1 241 088 bytes with the
  builder's comment area (1 129 895 gzipped), segments 0 → 2 and 2 → 299,
  1899-12-01 to 2101-02-01 TDB, every coefficient the source's.
- `NOTICE-planets.md`, `PROVENANCE.md`, `manifest.properties`, grown by
  addition at each release.

### `NOTICE-planets.md`, drafted

> # Notice: planets ephemeris pack
>
> This pack carries **JUranometria-modified excerpts of the JPL planetary
> ephemeris DE440**, one file per planet, so the atlas can compute where the
> planets are, how large they look and how much of each is lit, without any
> network access at build, test or run time. It stands beside the Solar
> System pack and the Jovian System pack, which it does not change: the Sun,
> the Earth and the Moon are computed from the Solar System pack alone, as
> released, and each planet's place is measured from them. A planet's file,
> once released, is never rewritten; a later release adds files.
>
> ## The ephemeris excerpts
>
> `juranometria-de440-venus-1900-2100.bsp` was produced by JUranometria from
> the official kernel `de440s.bsp` distributed by NASA/JPL's Navigation and
> Ancillary Information Facility (NAIF). It keeps two of that kernel's
> segments - the Venus barycentre relative to the solar-system barycentre,
> and Venus relative to its barycentre - over the years 1900 to 2100, with
> every coefficient unchanged. It is a **modified kernel** under NAIF's rules
> and is named, annotated and attributed as such; it is not an original JPL
> file and JPL did not produce it.
>
> NAIF's rules, quoted: "Redistribution of SPICE kernels distributed by NAIF
> is permitted as long as they have not been modified." "If a kernel
> distributed by NAIF has been modified in any way, any embedded or
> otherwise allied attribution of the original kernel producer must be
> replaced with the name and institution of whomever has made the last
> modification." "The file name must also be changed to help avoid
> confusion." SPICE data carry no fees or licensing ("Technology and
> Software Publicly Available").
>
> The ephemeris is the work of R. S. Park, W. M. Folkner, J. G. Williams and
> D. H. Boggs, *The JPL Planetary and Lunar Ephemerides DE440 and DE441*,
> The Astronomical Journal 161:105 (2021), doi:10.3847/1538-3881/abd414.
> Venus's radius, 6051.8 km, is the IAU WGCCRE value as NAIF's
> `pck00011.tpc` states it: the solid body, not the cloud tops.
> JUranometria acknowledges NASA/JPL NAIF and the DE440 authors as the
> source of these data.
>
> ## Provenance
>
> `PROVENANCE.md` beside this notice records, for each planet's file, the
> source URL, retrieval date, upstream digest, the exact extraction and its
> validation, the output digest and the coverage. The manifest pins every
> file's SHA-256, verified before use.

### `PROVENANCE.md`, the facts it will carry for Venus

- Source: `de440s.bsp`, <https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp>,
  retrieved 2026-09-28 (the released pack's pinned download), sha256 `c1c7feea…49f2`.
- Radius: `pck00011.tpc`, <https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc>,
  retrieved 2026-10-07 (the Jovian pack's pinned download), sha256 `3dff7b1d…bb1`.
- Extraction: the planets pack builder (#501), using `SpkExcerpt`; segments
  0 → 2 and 2 → 299, SPK Type 2; 1900-01-01 to 2101-01-01 TDB with the
  released 31-day margin; whole records, coefficients unchanged.
- Validation: every state on a 1.7-day grid identical to the source (the
  study: 86 446 states, worst difference 0), and 2 → 299 identically zero.
- Output: the kernel's bytes and sha256; the coverage of each segment.

### `manifest.properties`, the keys (per-planet keys stand once released)

```properties
format.version=1
pack.name=planets
pack.version=1
ephemeris=DE440
planets=venus
ephemeris.venus.bodies=2,299
ephemeris.venus.kernel=juranometria-de440-venus-1900-2100.bsp
ephemeris.venus.coverage.start.jd=2415020.5
ephemeris.venus.coverage.end.jd=2488434.5
ephemeris.venus.segments=2
pck.BODY299_RADII=6051.8,6051.8,6051.8
pck.BODY10_RADII=695700.,695700.,695700.
source.de440s.url=https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp
source.de440s.sha256=c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2
source.pck.url=https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc
source.pck.sha256=3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1
terms=NAIF rules for modified kernels
checksum.<file>=<sha256>  (one per file)
```

A later release adds `planets=venus,mercury`, the `ephemeris.mercury.*` keys,
its radius and its checksum, and raises `pack.version`; no Venus line changes.

## Layout A (first proposed, superseded): a Venus-named pack

The same Venus kernel in a pack named `venus`. Returned by the owner (#500,
ruling 2) because the road now includes every planet: a pack per planet is
Layout D in `pack-measurements.md` - the same files with three text files
per planet.

## Layout B (not proposed): the Solar System pack regenerated as v3

Six segments in one kernel, `juranometria-de440-sun-emb-earth-moon-venus-1900-2100.bsp`,
16 024 576 bytes: 1 114 112 more than today's 14 910 464, slightly smaller
than A because one header is shared. The four released segments give the
released states bit for bit (172 892 states), but the released file, its
name, its digest and `SunInvarianceTest`'s byte identity all change, and the
Sun and Moon would be re-released to add a planet. C keeps the released
bytes literally unchanged; B does not.

## What the downloads gain

Venus adds about 1.1 MB compressed to every archive under any layout; all
seven planets' barycentre kernels about 6.4 MB (`pack-measurements.md`,
before the outer planets' centres are measured). The five released archives
are 44-49 MB each.
