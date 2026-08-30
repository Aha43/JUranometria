# Notice: star-identity data

The traditional star names, Bayer designations, Flamsteed
numbers, and constellation memberships in this pack are
derived from **d3-celestial** by Olaf Frohn,
https://github.com/ofrohn/d3-celestial, pinned at commit
`7e720a3de062059d4c5400a379146a601d9010e0`
(`starnames.json`), and redistributed under the
**BSD-3-Clause** licence (full text in
`LICENSE-BSD-3-Clause.txt` beside this notice).

Provenance, per docs/decisions/star-identity.md:

- The proper names are **traditional star names** as
  compiled by the source - largely coinciding with the IAU
  WGSN approved set, but this pack does not claim per-name
  IAU certification. Bayer letters and Flamsteed numbers
  are historical designation systems, not IAU standards.
- Upstream compilation sources recorded by d3-celestial:
  the HD-DM-GC-HR-HIP-Bayer-Flamsteed Cross Index (Kostjuk
  2002, VizieR IV/27A), the FK5/common-name cross index
  (IV/22), the GCVS, and the IAU constellation chart data.
- Identities attach to the bright-sky pack's stars by
  Hipparcos number through the raw Tycho-2 catalogue (main
  files and supplement); a multi-component system's
  identity attaches to its brightest packed component
  only. Stars without an entry simply have no identity.
