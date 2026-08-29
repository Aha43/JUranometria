# Changelog

All notable changes to JUranometria will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
