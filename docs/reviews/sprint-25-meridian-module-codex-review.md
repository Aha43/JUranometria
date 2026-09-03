# Sprint 25 meridian-module review

Reviewed PR #236 at `7f83bc1`, implementing issue #227.

## Decision

Changes requested. The main architecture is good: the module owns place and
time, contributes sky geometry, and can be removed without changing the chart;
the chart owns projection, layering, strokes, marks, and labels. A generic
line-versus-boundary meaning and a paint-only invalidation are both justified
additions to the seam. Neither gives the module a graphics context or teaches
the chart what a meridian is.

Three issues remain, followed by one documentation requirement.

## Findings

### P1 — The reader is shown “Horizon,” contrary to the reviewed decision

The gate explicitly says the line is named **mathematical horizon in the
interface and not only in documentation**. `MeridianModule` contributes it as:

```java
new OverlayContribution.GreatCircle("horizon", "Horizon", ...)
```

Its comment then deliberately says “mathematical” appears nowhere except the
documentation. That reverses the gate rather than implementing it. The reason
for the chosen name still applies: this line knows no terrain, atmosphere, or
refraction, and calling it simply “Horizon” makes a stronger promise than the
atlas can keep.

Use **Mathematical horizon** for the contribution's accessible name and drawn
label. Pin that exact reader-facing vocabulary in the module test and packaged
journey; a nonblank-name assertion cannot protect the decision.

### P1 — Packaged acceptance does not exercise the claimed off-page horizon

The packaged observer was chosen so the horizon misses the released page, and
the prose says this silence is exercised. The checks enable meridian, horizon,
and zenith together and require only that their combined image differs by more
than 100 pixels. They then check the three contribution identities. An
incorrect horizon painted across the page would increase the total and pass
every assertion.

Exercise the geometries independently through the real attached module and
chart painter:

- horizon only must leave this chosen page byte-identical;
- meridian only must put down visible ink; and
- zenith only must put its ring at the model's projected position.

Establish each premise so a missing reference painter or a module returning
nothing cannot satisfy the test. Mutation-check the horizon case by giving it
a pole that crosses the page or by making the painter draw a substitute line;
the packaged assertion must fail on its own claim.

### P1 — Paint-only redraw is proved against the test double, not production wiring

`MeridianModuleTest` shows that `module.observer(...)` invokes
`TestChartServices.redraw()` and that this fake service does not increment its
fake inventory counter. It does not prove what the real
`ChartModuleHost.redraw()` does. The packaged run checks only unchanged view
state and absence of navigation requests. A mutation from `chart.repaint()` to
`rebuild()` in the production host can reassemble inventory/query the catalogue
while leaving all those checks green.

Drive a place, instant, and visibility change through a module attached to a
real `ChartModuleHost` and counting `ChartComponent`/assembler seam. Require:

- the same scene instance and view state;
- no additional assembly or catalogue/inventory build;
- changed overlay geometry; and
- a repaint request or rendered-pixel change.

The test should fail if `ChartModuleHost.redraw()` calls its `rebuild()` path.
This is the production guarantee that makes adding `redraw()` a safe generic
service rather than a hidden route to chart work.

### P2 — Amend the gate for the two justified seam additions

The gate says the seam needs “exactly one new thing,” the great-circle
geometry. Implementation correctly discovered two requirements the gate did
not name:

- a domain-neutral semantic distinction between a line across the sky and a
  boundary, allowing the chart to choose solid versus dashed without knowing
  what a horizon is; and
- a paint-only invalidation request for pulled geometry whose module-owned
  state changes.

Record both amendments in `docs/decisions/place-and-time.md`, including why
the rejected alternatives—appearance instructions from modules, domain names
inside the chart, pushing geometry, or rebuilding the scene—would violate the
architecture. The decision should describe the seam that now exists rather
than preserve an “exactly one” claim already known to be false.

Keep the semantic name generic (`Reference`, `LINE`, `BOUNDARY`) and keep
`redraw()` limited to paint invalidation. Do not expose stroke, dash, colour,
renderer, or repaint scheduling details to modules.

## Accepted work to preserve

- Great circles are contributed by pole and clipped analytically.
- Reference ink is above geography/grid and below all catalogue marks.
- A module-free or quiet chart is byte-identical to the released chart.
- Place, instant, and switches do not move the page; only the explicit centre
  request uses `NavigationRequest`.
- Geometry is pulled and owned, deterministic across attachment order for this
  ink layer, and withdrawn on detach.
- Disabled geometry performs no local-sky calculation.
- No observer, clock, sidereal time, pixel, renderer, or catalogue crosses the
  module boundary.

## PR #235 / issue #220

PR #235 is approved as a diagnostic-only change. It records both moments on
the EDT, changes no behavior, and its output distinguishes scene reassembly,
scene-content loss, renderer omission, and changed placement. Merge it
independently before waiting for the next natural #220 failure.

The next repair must follow the reported state. Metal remains correlation, not
cause, until a failure with the expanded diagnostic identifies which set or
decision changed.

## Follow-up review at `7444848`

The naming, isolated packaged-horizon evidence, and gate amendment close their
findings. In particular, the horizon is now required to change exactly zero
pixels on its own, so overlap or antialiasing in the combined render cannot
hide stray horizon ink.

### P1 — The production redraw test proves only the negative half

`ModuleRedrawTest.redrawRebuildsNothingAndTellsNobodyThePageChanged` proves
that production `redraw()` does not replace the scene or inventory and does not
announce a page change. Its control proves those observations can detect a
rebuild. This closes the most dangerous half of the original finding.

But the test invokes `services.redraw()` directly and observes no positive
effect. Replacing `ChartModuleHost.redraw()` with a no-op leaves every assertion
in the new test green. The packaged acceptance paints explicitly after each
module change, so it also does not prove that the running UI schedules a paint.
The module's fake-service test only proves that the module asks; the production
test only proves that the host does not rebuild. Nothing yet proves that the
request crosses the real seam and causes fresh module geometry to appear.

Drive an observer, instant, or visibility change through a `MeridianModule`
attached to the real host and prove the positive effect without calling paint
or `redraw()` from the test: either observe the repaint request itself, or use a
display-backed test that waits for the changed reference pixels to appear.
Keep the identity and announcement assertions, since they prove the repaint is
paint-only. Mutation-check both halves independently: `redraw()` changed to
`rebuild()` must fail the negative evidence, and `redraw()` changed to a no-op
must fail the positive evidence.

PR #236 remains held on this one production-path gap. PR #235 is merged and is
not part of this finding.

## Final review at `0c95f1c`

Approved. The remaining P1 is closed.

The production-path test now drives an observer change through an attached
`MeridianModule`, clears setup repaint noise before that transition, and
observes that Swing is asked to repaint the actual chart component. At the same
time, scene and inventory identity and page-change silence retain the negative
half of the contract. The global repaint manager is restored in `finally`.

The two relevant mutations are independent: replacing production `redraw()`
with `rebuild()` fails the identity evidence, while replacing it with a no-op
fails the repaint evidence. Together they establish that module invalidation
causes paint, and only paint.

All findings on PR #236 are closed. The meridian module foundation is ready to
merge; #228 may begin after that merge.
