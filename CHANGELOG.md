# Changelog

All notable changes to JUranometria will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Regional charts now teach constellation geography: at 12 degrees and
  wider, traditional line figures draw in quiet grey with constellation
  names (Latin, by the atlas convention) placed on each figure's
  visible ink; at 18 degrees and wider, the official IAU boundaries
  join as faint dotted lines - reconstructed true arcs that curve
  smoothly around the poles and across RA 0. Searching M42 and zooming
  out now ends with the whole of Orion named around the still-centred
  nebula. Geography sits under stars, symbols, labels, and the title
  block; the released 1-8 degree pages keep exactly their shipped ink,
  and the M31 reference chart is unchanged.
- The application now bundles reproducible constellation geography:
  the 88 IAU constellation identities (Latin names, genitives,
  abbreviations), traditional line-figure segments, and the official
  IAU boundaries reconstructed along their constant-coordinate B1875
  arcs to within one arcminute - generated offline from pinned,
  checksum-verified sources (d3-celestial, BSD-3-Clause; provenance
  and notices bundled) via make import-constellations. Nothing draws
  yet; the rendering arrives with the constellation layer.

## [0.6.0] - 2026-08-29

Sprint 6 — Reveal the wider sky. Reviewed by Codex before release; the
review trail lives in `docs/reviews/`.

### Added

- Regional zoom: the field width now extends through 12, 18, 24, and 36
  degrees, so zooming out from a searched object progressively reveals
  its celestial neighbourhood - Orion whole around M42, Cassiopeia and
  the Perseus Milky Way around M31 - while the searched target's exact
  position, title, and identity remain the centre through every step.
  Reset still restores exactly M31 at 8 degrees and V 8.0.
- Regional detail policy: at fields wider than 18 degrees, deep-sky
  symbols draw only at their true projected size - the practical-minimum
  clamp no longer inflates hundreds of sub-pixel objects into speckle
  (M13's 36-degree page drops from 256 drawn symbols to 2). Messier
  objects are always drawn, clamped when necessary, and the searched
  target is always drawn and labelled when its type has a chart symbol;
  labels otherwise attach only to Messier objects shown at true size,
  which dissolves the M31/M32/M110 label pile naturally. The 18-degree
  and narrower pages, and the user's star magnitude limit, are untouched.

## [0.5.0] - 2026-08-29

Sprint 5 — Build the local sky. Reviewed by Codex before release; the
review trail lives in `docs/reviews/`.

### Added

- Every deep-sky type now draws its chart symbol from the chart
  conventions: open clusters as dotted circles, globular clusters as
  circles with a central cross, nebulae of every kind as restrained
  light-grey outlined boxes, planetary nebulae as small crossed circles,
  and galaxy pairs and groups as oriented ellipses - so M42, the
  Pleiades, M13, and M57 appear on their charts with their Messier
  labels. Stellar-type NGC entries and associations remain searchable
  but undrawn. The M31 reference chart gains two honest marks (a small
  globular symbol and the NGC 317 pair) and was regenerated and
  visually reviewed.

### Fixed

- The chart title follows the current view instead of always claiming
  the M31 region: a search target titles its chart (for example
  "M 42 · Great Orion Nebula region"), a coordinate recenter titles by
  its position, zooming keeps the target, and reset restores the exact
  M31 default. Search messages now state what is true of the whole
  bundled catalogue.
- Large objects can no longer be silently omitted from a chart: the
  query margin now comes from the pack manifest's declared maximum
  object semi-extent (5.39 degrees, the Large Magellanic Cloud)
  instead of a constant sized for M31, so the LMC, the Hyades, and
  other giants appear whenever their symbols reach into the visible
  frame.

### Added

- The local sky: the application now loads, queries, and searches the
  bundled bright all-sky catalogue pack - the complete sky to stars of
  V 8.0 with 13,371 deep-sky objects - so offline searches such as M42,
  the Pleiades, or 47 Tuc recenter onto complete local charts anywhere,
  including across the RA wrap and near the poles. Scene queries read
  only the tiles a view needs, each verified against its manifest
  checksum at first read, and a missing, corrupt, or incompatible pack
  fails with a clear diagnostic instead of a sparse sky. Non-galaxy
  deep-sky types are searchable and recentre the chart but await their
  chart symbols. The Sprint 3 M31 regional resource retired; the M31
  reference chart reproduces from the pack byte-identically.

## [0.4.0] - 2026-08-29

Sprint 4 — Search and recenter. Reviewed by Codex before release; the
review trail lives in `docs/reviews/`.

### Added

- Search and recenter: a compact toolbar search resolves bundled object
  names, identifiers, and coordinates entirely offline and recentres the
  atlas. The current field width is preserved when the bundled coverage
  allows; otherwise the chart steps to the widest field that remains
  complete, and a result beyond local coverage leaves the chart unchanged
  with a concise message. Reset restores the M31 default and clears the
  search. Windows taller than the honest page letterbox the atlas page on
  the application surface.

## [0.3.0] - 2026-08-29

Sprint 3 — Local catalogue foundation. Reviewed by Codex before release;
the review trail lives in `docs/reviews/`.

### Changed

- Chart scenes are now assembled by querying the local catalogue from the
  current view state and window geometry: zooming, magnitude changes, and
  window resizes issue a fresh bounded query whose radius always covers
  the visible chart corners (including tall or wide windows), while plain
  repaints reuse the assembled scene without touching the catalogue.
- The chart now draws from the bundled regional catalogue generated from
  Tycho-2 and OpenNGC (3204 stars to V 10.0 and 47 galaxies within 10
  degrees of M31) instead of the hand-curated SIMBAD fixture. M31's
  ellipse takes its OpenNGC dimensions (177.8 by 69.7 arcminutes), M110
  now shows its true oriented shape, Messier objects are labelled by
  their Messier names, and fainter catalogue galaxies appear as
  unlabelled symbols.

## [0.2.0] - 2026-08-28

Sprint 2 — Navigate the chart. Reviewed by Codex before release; the
review trail lives in `docs/reviews/`.

### Added

- The first interactive controls: a compact atlas toolbar with zoom in,
  zoom out, and reset view, stepping the field width through 8, 6, 4, 3,
  2, and 1 degrees centred on M31, with a live field-width readout,
  bundled Tabler icons, and controls that disable at the fixture bounds.
- A limiting-magnitude control: fewer/more stars step the stellar limit
  between V 4.0 and V 8.0 in whole magnitudes with immediate chart
  updates; the toolbar readout, title block, and rendered stars always
  agree, and reset restores the complete 8°/V 8.0 default view.

## [0.1.0] - 2026-08-28

Sprint 1 — The first convincing chart. Reviewed by Codex before release;
the review trail lives in `docs/reviews/`.

### Added

- Initial product, chart, architecture, catalogue, development, and Sprint 1
  specifications.
- Application appearance specification based on FlatLaf and bundled Tabler SVG
  icons.
- Executable project foundation: Make-based build targeting Java 21, pinned
  dependency download script (FlatLaf, FlatLaf Extras, JSVG, JUnit console
  runner), and a minimal FlatLaf application window launched on the EDT.
- The first rendered chart: the application window now shows a fixed
  8-degree M31-region star chart on white paper with a restrained frame,
  drawn from the bundled fixture, with a tunable magnitude-to-radius star
  scale and a `make chart-image` target that writes a deterministic
  reference image.
- The complete Sprint 1 chart: M31, M32, and M110 drawn as oriented
  ellipses from their catalogued dimensions and position angles, restrained
  labels, and a formal title block stating target, centre, ICRS J2000,
  field width, limiting magnitude, and orientation. A `--dark` flag runs
  the dark application theme; the chart stays white paper in both.
