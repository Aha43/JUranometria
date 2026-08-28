# M31 fixture provenance

Hand-curated test fixture for designing the Sprint 1 M31 chart. It is not
the beginning of a maintained catalogue.

## Source

All values were retrieved from the SIMBAD database, operated at CDS,
Strasbourg, France, on 2026-08-28, via the SIMBAD TAP service
(`https://simbad.cds.unistra.fr/simbad/sim-tap`).

Queries used:

- Field stars: stars with V < 8.0 within 5.5 degrees of the M31 centre,
  which covers the corners of the intended 8-degree-wide chart
  (`SELECT b.main_id, b.ra, b.dec, f.V FROM basic b JOIN allfluxes f
  ON b.oid = f.oidref WHERE CONTAINS(POINT('ICRS', b.ra, b.dec),
  CIRCLE('ICRS', 10.6847, 41.269, 5.5)) = 1 AND f.V < 8.0
  AND b.otype = '*..'`).
- Galaxies: `basic` rows joined with `allfluxes` for the identifiers
  M 31, M 32, and M 110, including `galdim_majaxis`, `galdim_minaxis`,
  and `galdim_angle`.

## Terms and acknowledgment

SIMBAD data may be used freely with acknowledgment. This fixture is a small
extract for testing and chart design.

> This research has made use of the SIMBAD database, operated at CDS,
> Strasbourg, France.

## Transformations

- Coordinates are ICRS decimal degrees rounded to six decimals; V magnitudes
  rounded to two decimals; galaxy dimensions (arcminutes) rounded to two
  decimals.
- Star identifiers are SIMBAD `main_id` values with the leading `*` or `V*`
  marker, abbreviation dots, and internal padding removed (for example
  `* nu. And` becomes `nu And`, `V* V428 And` becomes `V428 And`).
- Rows are ordered brightest first; identifier uniqueness was verified
  during generation.
- Galaxy position angles were normalized from SIMBAD's values (-20 for M32,
  -15 for M110) into [0, 180) by adding 180 degrees.
- Magnitudes are V magnitudes from SIMBAD `allfluxes.V`.
