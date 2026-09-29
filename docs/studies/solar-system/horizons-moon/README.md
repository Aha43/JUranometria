# JPL Horizons responses for the Moon (issues #406, #407)

Thirteen responses kept whole, exactly as returned, each headed by
its request URL with every query parameter, the UTC of the request
and the SHA-256 of the whole body; `MoonReferenceVectorTest` re-hashes
them. Fetched once by `scripts/horizons-moon-fetch.py`. Study evidence
only: nothing at build, test or run time depends on them beyond the
tests reading them as fixtures, and the atlas never reaches Horizons.

These replaced a first set fetched on 2026-09-29 with the Sun
fixtures' site heights (Quito at 2.85 km). The contract's observer
stands on the WGS 84 ellipsoid at sea level - Place and Time carries
no height - and for the Moon that height showed as 1.65 arcseconds of
parallax, 2.86 km of distance and 0.016 arcseconds of diameter, so
the requests were remade at altitude 0 the same day. The first set is
not kept; its measurements are recorded in
`docs/decisions/moon-computation.md`.

- `named-*.txt` - the thirteen named instants, five observers
- `matrix-7d-*.txt` - every 7 days across 1900-2100, five observers
- `dense-2026-oslo.txt` - daily through 2026 at Oslo
- `sun-matrix-7d-oslo.txt` - the Sun on Oslo's 7-day grid, for the
  bright-limb angle held to Horizons' own inputs
- `geocentric-meeus-47a-tt.txt` - 1992-04-12 00:00 TT, geocentric,
  the Meeus 47.a/48.a case
- `NAMED-CASES.txt` - the named instants and their labels
