# Sprint 1 Codex review

Reviewed 2026-08-28 before merging PR #12 and releasing `0.1.0`.

Review scope: Sprint 1, "The first convincing chart", from foundation commit
`9cf8fdc` through PR #12 head `5932488`. The review used
`sprint-01-handover.md` as its entry point, then inspected the implementation,
tests, fixture provenance, reference image, and GitHub state independently.

## Result

The implementation is structurally strong and the first chart succeeds as a
product proof. Package boundaries are clear, the mathematical core is isolated
from Swing and Java2D, catalogue loading stays outside painting, and the chart
keeps its explicit white-paper palette under both application themes.

No high-severity architectural, projection, or catalogue defects were found.
Three P2 findings should be resolved before PR #12 is merged and `0.1.0` is
released.

## Findings

### P2 — apply the scene limiting magnitude

**Location:** `src/juranometria/render/ChartRenderer.java`, star-rendering loop.

`ChartScene.limitingMagnitude` currently affects only the title block. The
renderer draws every supplied star, while `StarSizePolicy.radiusFor` gives a
star at or beyond its own fixed limit the minimum radius. A scene that states
"Stars to V 8.0" can therefore display a fainter star if a deeper catalogue
query supplies one.

The Sprint 1 fixture is already cut below V 8, so the present reference chart
does not expose the mismatch. It becomes a correctness defect as soon as the
catalogue depth or a magnitude control changes.

**Required change:** exclude stars fainter than the scene limit during scene
assembly or rendering, and protect the boundary with a focused test. Keep
`StarSizePolicy` responsible for mark sizing rather than implicit filtering.

**Tracking:** GitHub issue
[#13](https://github.com/Aha43/JUranometria/issues/13), added to the Sprint 1
milestone.

### P2 — make chart notation independent of the machine locale

**Location:** `src/juranometria/render/ChartRenderer.java`, title and coordinate
formatting.

The title block uses `String.format` with the JVM default locale. Rendering the
same scene under Norwegian and US locales produced different PNG files; the
Norwegian rendering used decimal commas where the US rendering used decimal
points. The renderer's claimed deterministic output therefore depends on
global machine state.

**Required change:** use an explicit stable locale, such as `Locale.ROOT`, for
formal chart notation. Add a regression test that changes the default locale
around formatting or rendering.

**Tracking:** recorded in the Codex review on PR
[#12](https://github.com/Aha43/JUranometria/pull/12).

### P2 — keep the title block inside supported viewport sizes

**Location:** `src/juranometria/render/ChartRenderer.java`, `drawTitleBlock`.

The title box always uses its complete measured width at a fixed x position and
a fixed three-line height. The chart component is freely resizable and the
viewport model accepts very small dimensions. A narrow window clips the title
on the right; a short window makes `boxY` negative and moves part or all of the
block above the visible chart.

**Required change:** either establish and enforce a meaningful minimum chart
size or define adaptive/omitted title behaviour below a documented threshold.
Protect the chosen behaviour with a focused small-viewport test.

**Tracking:** recorded in the Codex review on PR
[#12](https://github.com/Aha43/JUranometria/pull/12).

## Areas checked without findings

- Gnomonic projection equations and the 90-degree rejection boundary.
- North-up, east-left pixel mapping and equal horizontal/vertical scale.
- Galaxy position-angle conversion from east-of-north coordinates to the
  east-left Java2D chart.
- Angular cone containment across the right-ascension wrap and at the poles.
- Separation of projection, catalogue, rendering, Swing, and application
  assembly concerns.
- Fixture parsing for its deliberately restricted, non-quoted CSV format.
- Fixture-coupled tests as conscious change detectors.
- `ChartComponent` accessible-context implementation and regression test.
- Current label-placement simplicity and its replaceability.
- SIMBAD units, position-angle convention, bundled provenance, and requested
  acknowledgment.
- Reference chart appearance in `docs/reference/m31-stars.png`.

## Verification performed

- `make test`: **44 tests passed** when run with normal macOS Java2D and font
  access. The sandboxed JVM aborted while initializing Java2D; rerunning outside
  that restricted graphics environment passed cleanly.
- Rendered the same scene with Norwegian and US JVM locales and compared their
  PNG hashes, confirming the locale-dependent output.
- Inspected the committed M31 reference image at its original resolution.
- Reviewed the full Sprint 1 change range and the narrower PR #12 diff.
- Checked PR #12 status and posted the two PR-specific findings there.

## Release recommendation

Resolve the three P2 findings, regenerate and inspect the reference chart if
renderer output changes, rerun the complete test suite, and request a short
follow-up review. PR #12 can then merge, the Sprint 1 milestone can close, and
release `0.1.0` can be cut.

