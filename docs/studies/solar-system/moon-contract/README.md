# The Moon contract study (issue #406)

Measured on 2026-09-29 before any production Moon code. Not evidence the
contract regenerates: a record of what the #406 checkpoint rested on.

- `comparison-moon.md` - Skyfield 1.55 on the official `de440s.bsp` against
  JPL Horizons (DE441), 52 870 rows, plus Meeus 47.a/48.a and the
  bright-limb angle checked from the oracle's own inputs; produced by
  `scripts/moon-authority-comparison.py` from the responses
  `scripts/horizons-moon-fetch.py` keeps whole (committed with #407).
- `figures/` - the two definitions diagrams (SVG source, PNG rendered with
  the bundled JSVG): phase angle and elongation; the bright-limb position
  angle on the chart and in the local sky.
- `PackStudy.java.txt` - the one-pack regeneration study: the Moon segment
  added to the modified DE440 excerpt, footprint measured, every Sun,
  barycentre and Earth state proved bit-identical, the Moon's states proved
  identical to the official kernel, and the Sun's observations through the
  service proved record-equal.
