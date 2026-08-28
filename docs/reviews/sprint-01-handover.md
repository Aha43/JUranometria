# Sprint 1 review handover

Prepared 2026-08-28 for the pre-release Codex review of Sprint 1,
"The first convincing chart". At the time of writing, PR #12 (issue #6)
is open and everything else is merged to `main`. After review the plan
is: merge, close the milestone, release 0.1.0.

## What this project is

JUranometria is a quiet, monochrome Java/Swing star atlas — white paper,
black ink, north up, **east left** — deliberately old-style: plain source
folders, `Makefile`, direct `javac`/`java`, downloaded JARs. No Maven,
Gradle, dependency injection, or UI frameworks, and no Jackson yet —
these are hard project rules, not omissions. Read `AGENTS.md` first,
then the documents under `docs/`.

## Where things landed

Sprint 1 (milestone 1, issues #1–#6, PRs #7–#12) renders a deterministic
8-degree M31 chart from bundled data.

Packages, one concern each:

- `juranometria.app` — `JUranometriaMain` (EDT, macOS screen menu bar
  properties set before AWT loads, `--dark` flag), `UiTheme` (FlatLaf
  setup), `AppInfo` (reads the `/VERSION` resource), `M31Chart` (fixed
  scene assembly, loads data up front), `ChartImageMain`
  (`make chart-image` writes the deterministic reference PNG).
- `juranometria.chart` — immutable records validated at construction:
  `SkyPosition` (ICRS decimal degrees, RA `[0, 360)`, dec `[-90, 90]`),
  `ChartViewport`, `SkyRegion` (haversine cone query), `Star`,
  `DeepSkyObject`, `DsoType` (GALAXY only, per scope), `StarSizePolicy`
  (magnitude-to-radius, clamped minimum and maximum), `ChartScene`
  (the complete render input: viewport, objects, title, limiting
  magnitude).
- `juranometria.project` — `GnomonicProjection` (sky to tangent plane;
  positions 90 degrees or more from centre return `Optional.empty()`),
  `ViewportMapping` (plane to pixels, east left and north up, field
  width spans the pixel width), `PlanePoint`, `PixelPoint`. No
  Swing/Java2D imports anywhere here — that independence is an
  architectural invariant.
- `juranometria.catalog` — `Catalogue` interface (bounded region
  queries; contract: painting never parses files or touches the
  network), `FixtureCatalogue` (parses the bundled CSV once at load).
- `juranometria.render` — `ChartRenderer`: consumes a `ChartScene` and
  a `Graphics2D`, owns its explicit palette. Draw order: paper, galaxy
  ellipses, stars, labels, title block, frame.
- `juranometria.ui` — `ChartComponent`: holds pre-loaded data, builds a
  scene from its current size in `paintComponent`, delegates to the
  renderer.
- `src/resources/catalog/` — CSV fixtures (104 stars to V < 8.0 within
  5.5 degrees, three galaxies) plus `PROVENANCE.md` (SIMBAD TAP
  queries, retrieval date, transformations, acknowledgment).

Build and run: `pwsh scripts/download-libs.ps1` (pins FlatLaf 3.4.1,
FlatLaf Extras, JSVG 1.7.2, JUnit console 1.10.2), `make test`
(44 tests), `make run`, `make chart-image`. Java 21 minimum, compiled
with `--release 21`.

## Worth extra scrutiny

1. **Sign conventions in projection and rotation** — the most
   consequential mathematics. East-left means east is negative pixel x;
   a galaxy position angle (east of north) becomes
   `rotate(-toRadians(pa))` in `ChartRenderer.drawGalaxy`, with the
   ellipse's major axis drawn vertical before rotation. Derived by hand
   and verified visually plus one PA=90 test; a second independent
   derivation would be valuable.
2. **`ViewportMapping` scale** — `widthPx / (2 * tan(fieldWidth / 2))`,
   with the same scale vertically. Check the assumption holds for
   non-square aspect ratios, and the 180-degree rejection boundary.
3. **`SkyRegion.contains`** — haversine across the RA 0/360 wrap.
   Check pole behaviour.
4. **Determinism claims versus text rendering** — `renderToImage`
   includes font glyphs, so images are deterministic per machine but
   not across platforms. Tests deliberately use ink-presence counts and
   geometry rather than image hashes. Judge whether that is the right
   line, or whether text should move behind a testable seam before the
   planned visual regression images arrive.
5. **`FixtureCatalogue` parsing** — plain `String.split(",", -1)`, no
   quoting support; malformed lines throw `IllegalStateException`.
   Fine for a fixture; flag anything that would rot badly when the
   Stage 2 import tool (see `docs/catalogues.md`) arrives.
6. **Fixture-coupled tests** — `FixtureCatalogueTest` asserts exact
   counts (104 stars, 12 within 2 degrees) and ordered identifier
   lists. Intentional, so data changes stay conscious decisions, but
   judge whether the coupling is too tight.
7. **`ChartComponent.getAccessibleContext`** — a launch crash was found
   here (a plain `JComponent` subclass returns a null context); fixed
   with an anonymous `AccessibleJComponent` (CANVAS role) plus a
   regression test. Check the fix's correctness.
8. **`StarSizePolicy` clamp semantics** — stars fainter than the limit
   return the minimum radius rather than being culled; culling is
   currently implicit in the data cut. Is that honest enough, or should
   the renderer skip them explicitly?
9. **Title block geometry** — computed from `FontMetrics` at fixed
   margins; check overflow behaviour at small viewport sizes (the
   component allows 1x1).
10. **Label placement** — deliberately naive (right of the symbol's
    horizontal extent). Confirm it is cleanly replaceable when
    collision avoidance arrives; the simplicity itself is by design.
11. **SIMBAD provenance** — `src/resources/catalog/PROVENANCE.md`:
    check the terms and acknowledgment statement reads as sufficient
    for redistributing this small extract.

## Known accepted trade-offs (not findings)

Full recompile on every `make` invocation (fine at this size); no
coordinate grid, pan, zoom, search, or export; a single DSO symbol
type; no wide-field projection — all explicitly deferred by the sprint
scope. The magnitude 6–8 star-size steps are subtle and already noted
as a Sprint 2 tuning candidate in PR #12.

## Process expectations

Findings should become GitHub issues (or PR #12 review comments where
they belong to that diff) rather than direct pushes — the project works
strictly issue, branch, pull request. User-visible changes need a
`CHANGELOG.md` entry under `Unreleased`. Renderer changes need the
reference image (`docs/reference/m31-stars.png`, regenerated with
`make chart-image`) inspected, not only tests.
