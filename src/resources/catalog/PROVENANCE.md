# M31 fixture provenance

Hand-curated test fixture for designing the Sprint 1 M31 chart. It is not
the beginning of a maintained catalogue.

## Source

All values were retrieved from the SIMBAD database, operated at CDS,
Strasbourg, France, on 2026-08-28, via the SIMBAD TAP service
(`https://simbad.cds.unistra.fr/simbad/sim-tap`).

Queries used:

- Field stars: objects with V < 6.0 within 8 degrees of the M31 centre
  (`SELECT b.main_id, b.ra, b.dec, f.V FROM basic b LEFT JOIN allfluxes f
  ON b.oid = f.oidref WHERE CONTAINS(POINT('ICRS', b.ra, b.dec),
  CIRCLE('ICRS', 10.6847, 41.269, 8)) = 1 AND f.V < 6.0`).
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
- Star identifiers are SIMBAD `main_id` values with the leading `*` marker
  and internal padding removed (for example `* bet And` becomes `bet And`,
  `mu.` becomes `mu`).
- The components `phi And A` and `phi And B` were dropped in favour of the
  combined entry `phi And`.
- Galaxy position angles were normalized from SIMBAD's values (-20 for M32,
  -15 for M110) into [0, 180) by adding 180 degrees.
- Magnitudes are V magnitudes from SIMBAD `allfluxes.V`.
