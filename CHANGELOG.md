# Changelog

All notable changes to JUranometria will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Releases are published by pushing an annotated `vX.Y.Z` tag. The
  tag, `VERSION`, and the changelog section must agree before
  anything is built; the five artifacts are produced by the same
  workflows that gate every pull request, verified as a set, and
  published once with their checksums.

### Fixed

- A damaged or incomplete download now says so. Previously a
  corrupt bundled file threw before the window existed, printing a
  stack trace to a console a packaged reader does not have and
  leaving the application running with nothing on screen; it now
  names the file that failed to verify, gives the remedy (download
  again, check the published SHA-256, unpack fresh), and exits.

## [0.17.0] - 2026-08-31

Sprint 17 — Letter the stars. The Bayer and Flamsteed identities
that Sprint 13 bundled and made searchable become restrained chart
notation, so a constellation page now names the pattern it draws.
Reviewed by Codex at the design gate and at every implementation
step, including the closing journey.

### Added

- The stars are lettered: Bayer designations now reach the wide
  constellation pages in conventional notation - the letter with
  its component digits raised (π³, α¹) - and travel beside a
  traditional name where a star has both ("Betelgeuse α",
  "Gacrux γ"). Greek letters draw at every field; the post-omega
  Latin letters wait for the regional charts, where Flamsteed
  numbers keep their released limit.
- Three independent Chart Options controls replace the single star
  label toggle: **Star names**, **Bayer letters**, and **Flamsteed
  numbers**. A preference store from an earlier release carries its
  single choice into all three, and from the first confirmation
  each layer answers to its own setting.

## [0.16.0] - 2026-08-30

Sprint 16, in progress — the distribution work of the reviewed 1.0
contract, released early so the atlas can be installed and used
while the remaining stabilization issues close. Reviewed by Codex
at the distribution gate, the contract amendment, and each
implementation step.

### Added

- **Self-contained application images: users no longer install
  Java.** Each platform download - macOS Apple silicon, macOS
  Intel, Windows x86-64, Linux x86-64 - carries its own pinned
  Temurin 21 runtime, trimmed by `jlink` to the six modules the
  atlas actually uses (about 80 MB unpacked, 25 MB compressed).
  Built natively on each platform, never cross-packaged, and
  verified there: launch with no system Java at all, light and
  `--dark`, from paths containing spaces, with the About surface
  and a real preference change-and-reload exercised through the
  bundled runtime, the complete licensing inventory and the
  runtime's own legal notices asserted mechanically, and the images
  byte-reproducible across repeated builds - the two macOS
  architectures even render identical charts to each other. The
  applications are unsigned; the packaged README explains
  Gatekeeper and SmartScreen plainly.
- The unpack-and-run release archive: `make dist` deterministically
  builds `JUranometria-X.Y.Z.zip` - the application JAR, its pinned
  libraries, POSIX and Windows launch helpers with readable
  missing/old-Java diagnostics, and every licence and notice
  including the runtime libraries' - verified for exact contents and
  a packaged headless smoke render, then exercised on macOS, Linux,
  and Windows CI with real GUI launches from paths containing
  spaces.

### Fixed

- The build selects its own JDK instead of trusting whichever one
  leads the shell `PATH`: `make` resolves an explicit `JAVA_HOME`
  first, then a local Homebrew `openjdk@21`, then the `PATH` tools, so
  a shell whose `PATH` still leads with an older release builds and
  runs the atlas with no manual environment setup. A toolchain older
  than the recorded JDK 21 minimum stops the build with a message
  naming the required and detected versions instead of compiler
  errors.

## [0.15.0] - 2026-08-30

Sprint 15 — Read the coordinates. Reviewed by Codex at the design
gate, at each implementation step, and at the sprint close; the
review trail lives in `docs/reviews/`.

### Added

- The charts read their coordinates: a quiet equatorial (ICRS/J2000)
  grid - projection-correct meridians and parallels drawn beneath
  everything, with right-ascension labels along the bottom edge and
  signed declination labels along the left, adapting its intervals
  to every field width and converging cleanly at the poles. One new
  Chart Options control, "Equatorial coordinate grid" (default on,
  Content group), gates it repaint-only; the M31 reference
  deliberately gains the reviewed graticule.

## [0.14.0] - 2026-08-30

Sprint 14 — Zoom where you point. Reviewed by Codex at the design
gate, at each implementation step, and at the sprint close; the
review trail lives in `docs/reviews/`.

### Added

- Zoom where you point: the mouse wheel steps the field width with
  the sky beneath the pointer held exactly beneath the pointer -
  projection-correct through the pan solver's closed-form geometry,
  never a screen-space approximation. One notch is one step;
  trackpad rotations accumulate to whole steps; each accepted step
  is exact and preflight-reversible (a step the chart cannot anchor
  or reverse - near a celestial pole - honestly refuses rather than
  drift), and a pointer step that moves the centre clears the
  searched target like a pan. Platform zoom shortcuts (Cmd/Ctrl
  with +, -, =, and the keypad forms) and View-menu Zoom In/Out
  zoom about the centre, keeping the target, even while Search has
  focus - unmodified typing is never intercepted.

## [0.13.0] - 2026-08-30

Sprint 13 — Name the stars. Reviewed by Codex at the design gate, at
each implementation step, and at the sprint close; the review trail
lives in `docs/reviews/`.

### Added

- The charts name their stars: a restrained, scale-sensitive label
  pass renders traditional names, Bayer letters, and Flamsteed
  numbers per the reviewed policy - names V ≤ 2.5 on the widest
  pages, names and Bayer V ≤ 3.0 from 12-18°, all three forms on
  regional pages (Flamsteed to V 5.0) - brightest first,
  deterministic, yielding to deep-sky labels and the title block
  (prefer omission). A searched star always keeps its best identity
  label, exempt from thresholds and collisions, with no new symbol.
  One new Chart Options control, "Star names and identifiers"
  (default on), gates the pass; the M31 reference deliberately gains
  its one accepted label (35 And).
- Stars are findable by identity, fully offline: traditional names by
  prefix ("betel", "Polaris"), Bayer designations as the Greek letter
  or its spelled-out name plus the constellation ("α Ori",
  "alpha orionis" - IAU abbreviation and genitive both accepted, with
  and without a component digit), and Flamsteed numbers the same way
  ("58 Ori"). A bare letter or number lists its candidates rather
  than guessing, and every star result shows its full identity
  ("Betelgeuse · α Ori · V 0.6"). Star records carry a structured
  identity loaded from the checksummed pack; existing object,
  coordinate, and TYC searches are unchanged.
- Bundled star-identity pack (`star-identities.csv`, 4,805 rows):
  traditional star names, Bayer designations, Flamsteed numbers, and
  constellation memberships from the pinned d3-celestial
  `starnames.json` (BSD-3-Clause), joined to the bright-sky pack by
  Hipparcos number through the raw Tycho-2 catalogue - the reviewed
  decision's join, reproduced by `make import-star-identities` with
  every exception category counted and a checksummed manifest. The
  application does not read the new fields yet.

## [0.12.0] - 2026-08-30

Sprint 12 — Let the reader choose the chart. Reviewed by Codex before
release; the review trail lives in `docs/reviews/`.

### Added

- The reader chooses the chart: View > Chart Options... offers five
  content and label toggles - deep-sky objects, deep-sky labels,
  constellation figures, boundaries, and names - previewing live on
  the chart as they change, confirmed with OK, abandoned safely with
  Cancel or Escape, and remembered across restarts. Restore Defaults
  brings back the released chart exactly. Every scale rule stays
  automatic inside an enabled layer, and a searched target with an
  established symbol is always drawn and labelled whatever the
  toggles - the chart never titles itself by an object it hides.
  Labels follow their symbols and names follow their figures; Home
  still resets navigation only.

## [0.11.0] - 2026-08-30

Sprint 11 — Give the application a public face. Reviewed by Codex
before release; the review trail lives in `docs/reviews/`.

### Added

- A restrained menu bar: Help > About JUranometria shows the packaged
  version, the product description, and the licensing of the code and
  every bundled resource - with the complete notices readable offline -
  and File > Settings... holds a persistent Light/Dark
  appearance choice, applied live on OK and remembered across
  restarts. The --dark launch flag remains a session-only override
  that never rewrites the saved setting; the chart page keeps its own
  white paper and dark ink in both appearances.

## [0.10.0] - 2026-08-30

Sprint 10 — Refresh the foundations. A maintenance release; reviewed
by Codex before release, the trail lives in `docs/reviews/`.

### Changed

- Dependencies refreshed to current releases: FlatLaf and FlatLaf
  Extras 3.4.1 -> 3.7.2 and JSVG 1.7.2 -> 2.1.0 (moved together -
  FlatLaf Extras 3.7.2 requires JSVG 2), and the JUnit console runner
  1.10.2 -> 6.1.3. The application's chrome, icons, themes, and the
  chart itself are unchanged; the M31 reference remains
  byte-identical.
- The PowerShell bootstrap script is retired: the supported
  contributor environments are macOS and Linux (Windows via WSL), and
  scripts/lib-versions.env with scripts/download-libs.sh is the one
  bootstrap authority.

## [0.9.0] - 2026-08-30

Sprint 9 — Make a clean checkout dependable. A maintenance release;
reviewed by Codex before release, the trail lives in `docs/reviews/`.

### Added

- A POSIX dependency bootstrap: `scripts/download-libs.sh` fetches the
  pinned dependencies on macOS and Linux without PowerShell, sharing
  one authoritative version-and-checksum file
  (`scripts/lib-versions.env`) with the PowerShell script and the
  Makefile. Both scripts verify every existing and downloaded jar
  against its pinned SHA-256 (repairing partial or corrupt files,
  downloading atomically), and a build without the dependencies stops
  with a one-line instruction naming the script instead of compiler
  errors.
- A required GitHub Actions check: every pull request runs the full
  test suite on JDK 21 with the same command a contributor runs
  locally, and branch protection on `main` requires it before any
  merge.

### Fixed

- The application grants FlatLaf explicit native access (launcher flag
  on `make run`, manifest attribute in the packaged jar), so JDK 24+
  launches carry no restricted-native-access warning and keep native
  window integration under the stricter future default. The packaged
  jar's manifest also carries the runtime `Class-Path`, so
  `java -jar build/app/JUranometria.jar` now actually launches.
- The README's status, document list, and the development guide's
  sprint-record convention describe the repository as it exists after
  eight releases; a dead `.gitignore` section is gone.

## [0.8.0] - 2026-08-30

Sprint 8 — Pan across the local sky. Reviewed by Codex before release;
the review trail lives in `docs/reviews/`.

### Added

- The chart is now directly draggable: press on the paper and the sky
  position under the pointer follows the hand exactly - anchored to
  the press-time grab for the whole gesture, solved with the real
  projection (never degrees-per-pixel), smooth across RA 0 and honest
  near the poles, where the sky follows as far as a north-up chart
  allows. A real drag departs a searched target into an honest
  coordinate title; clicks and jitter change nothing; field width and
  magnitude ride along untouched; Reset view still returns exactly
  home. An open hand marks the draggable paper, a closed hand the
  live grab.

## [0.7.0] - 2026-08-29

Sprint 7 — Give the wider sky its geography. Reviewed by Codex before
release; the review trail lives in `docs/reviews/`.

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
