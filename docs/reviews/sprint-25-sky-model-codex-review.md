# Sprint 25 sky-model review

Reviewed PR #234 at `16bfdd0`, implementing issue #226 after the approved
place-and-time gate.

## Decision

Changes requested. The package split is largely right: observer and local-sky
astronomy belong outside the chart, and great-circle clipping belongs with the
projection. The SOFA fixture now exercises the production `SkyFrame` API, the
longitude and apparent-sidereal-time mutations have meaningful regressions,
and the chart remains unchanged when the model is absent.

Three public-boundary defects remain.

## Findings

### P1 — The public model can still be asked for truncated answers

The PR says there is no fidelity argument and tests that no public method takes
an enum or contains `shortcut`, `fidelity`, or `approx` in its name. But
`SkyFrame` exposes:

```java
public static double[] nutationDegrees(double centuries, int terms)
```

A caller can request four terms, one term, or zero terms. This is exactly the
lesser-answer control the contract says production cannot expose; it merely uses
an integer rather than an enum, so the structural test misses it. The study's
need to price truncation has leaked into the public model.

Keep only the full chosen nutation result public. Put the term-counted form in
the study, or make it private/package-private only if production tests have a
real reason to exercise it. Strengthen the public-surface test so restoring this
overload fails on the capability it exposes, rather than by method naming.

Also reject a term count outside the available range wherever the study helper
lives; silently treating a negative count as zero is not a meaningful model.

### P1 — Great-circle clipping still requires caller-supplied sampling

`GreatCircle` correctly carries a pole. `GreatCirclePage.clip`, however, does
not accept that circle or its pole. It accepts `List<SkyPosition> points`, and
the production-shaped test calls it with `new GreatCircle(pole).around(180)`.
The method then samples those supplied samples for two projectable points.

That is not the pole-based clipping approved by the gate. It leaves a future
module or renderer responsible for choosing 180, and a malformed or sparse list
can still make the same great circle disappear or produce an unstable line.
The test showing eight vertices miss the page is not load-bearing because the
actual call quietly substitutes a different 180-vertex list.

Make the projection API accept the geometric identity—the pole, or a generic
great-circle value—and derive its projected line from that value and the
gnomonic projection basis. No caller should supply points or a sampling count.
Then mutation-check the named one-degree case by replacing the pole-based path
with a genuinely sampled one; the current helper in the test must not conceal
that mutation by always supplying 180 points.

### P1 — The new projection class violates the stated AWT-free boundary

The PR reports “No Swing, AWT, preferences, renderer, OS or network,” but
`juranometria.project.GreatCirclePage` imports and exposes
`java.awt.geom.Rectangle2D`. The structural tests search `SkyFrame` for data and
network access and search the chart core for sky imports, but none verifies the
claimed AWT-free model/projection boundary.

Use a small project-owned page rectangle/value, existing viewport geometry, or
four numeric bounds. Keep Java2D conversion at the renderer/UI edge. Add an
architecture test over both `juranometria.sky` and the new projection surface
that rejects Swing, AWT, preferences, renderer, OS, and network imports. The
test should fail against the current `Rectangle2D` signature before the fix.

This is more than cosmetic: #227 will make the module seam consume this API.
Accepting an AWT rectangle here would establish the graphics dependency at the
first reusable module boundary after the gate explicitly rejected handing
graphics concepts to modules.

## Accepted work to preserve

- `Observer` requires an explicit instant and names east-positive longitude.
- `LocalSky` returns J2000 geometry and never hides chart content.
- Apparent sidereal time is distinguished from mean sidereal time by a test
  that fails when the distinction is removed.
- The complete frame rotation is held to the checked-in SOFA oracle.
- No clock, host time zone, locale, preference, file, network, or bundled data
  determines the answer.
- The chart draws its ordinary page unchanged with this model unused.

## Separate issue #220

Add the proposed limiting magnitude, scene star count, scene identity, and
chosen-star-present fields to the diagnostic. The latest failure rules out an
options mismatch and confirms that the queried pixel held no mark at click
time. A changed limiting magnitude or scene contents is now the useful next
distinction; Metal remains correlation only.

