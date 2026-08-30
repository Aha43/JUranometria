# Sprint 15 Codex review — Read the coordinates

Reviewed 2026-08-30 at `4734596`, before the proposed 0.15.0 release.

## Finding

### P2 — Drive the installed keyboard zoom shortcut in the closing journey

Issue #135 explicitly requires zoom by wheel and keyboard through production
paths. `CoordinateGridJourneyTest` installs the production shortcuts at lines
85–86, but every scale transition is made either with a wheel event or by
calling `ChartViewController.zoomIn()` / `zoomOut()` directly. A missing or
incorrect `installZoomShortcuts` call would therefore leave this sprint-closing
journey green. Its class documentation also says keyboard zoom is part of the
journey, so the evidence currently overstates what it proves.

Drive at least one installed zoom action through the frame's real root-pane
binding and assert both the resulting field and the corresponding grid interval.
Keep the wheel round trip: the two input paths are separate user contracts. No
production behavior or new feature is requested.

## What was verified

- GitHub's required `test` and GitGuardian checks are green at the PR head.
- Headless local JDK 21 run: 309 tests discovered, 303 successful, six
  display-dependent tests aborted by assumption, and no failures.
- `make chart-image` reproduces the deliberately updated M31 reference
  byte-for-byte.
- Headless regeneration reproduces all 15 coordinate-grid study pages and all
  nine regenerated star-identity pages byte-for-byte.
- The regenerated M31 and wide Orion pages were inspected; the graticule is
  quiet, legible, and correctly subordinate to chart content.
- `git diff --check` is clean before adding this review.

The display-dependent closing journey could not execute in this headless review
run. Its author reports a successful 309/309 display run.

## Assessment

Sprint 15 has landed as a coherent piece of cartography. The grid is derived
from the projection, stays below the astronomical content, handles the RA wrap
and polar cap explicitly, and enters the established repaint-only options
contract without contaminating navigation or catalogue assembly. Default-on is
the right choice: coordinates now explain every page without demanding setup,
while the old appearance remains one clear option away.

The handover is candid about the visual ripple and residual label compromises.
The regenerated Sprint 13 pages are the correct consequence of treating those
images as production output rather than frozen historical screenshots.

## Recommendation

Close the keyboard-path evidence gap, then merge PR #141, close milestone 15,
and cut 0.15.0. Sprint 16 should remain the planned stabilization and 1.0 release
sprint; nothing in this review calls for another feature sprint first.
