# Decision: pointer-centred zoom and the platform zoom shortcuts

**Sprint 14, issue #123.** Status: proposed for review. This gate
changes no product behavior; the M31 reference is byte-identical.

## The invariant

Zooming with the wheel keeps **the sky beneath the pointer beneath
the pointer** as the field width steps through the existing discrete
sequence (36° … 1°). Scale changes; the place the reader is looking
at does not move.

## Geometry: the pan seams already express it exactly

A pointer-anchored step is three existing operations and no new
solver:

1. **Recover the anchor**: the pointer's tangent-plane point via
   `PanSolver.planeFromPixel` at the current viewport, then its sky
   position via `PanSolver.skyFromPlane` (the exact inverse
   gnomonic) about the current centre.
2. **State the requirement**: the same pixel at the new field width
   is a new tangent-plane point (`planeFromPixel` with the new
   field's scale — same window, different pixels-per-plane-unit).
3. **Solve the centre**: `PanSolver.solveCentre(anchor, target,
   currentCentre)` — the Sprint 8 closed-form grab solver, verified
   by full reprojection, classifying every outcome as exact,
   constrained (polar clamp at the north-up feasibility boundary),
   or past-pole. An empty without past-pole evidence still throws:
   failure diagnostics, never fallback drift.

The accepted transition is **one atomic `ChartViewState` change** —
recenter and field width together — through the existing controller,
exactly as pan applies its recenters.

A dedicated zoom solver was considered and rejected: it would
re-derive the identical equations the pan decision already proved,
measured, and reviewed, and split one geometric truth across two
implementations.

## Measured evidence (`make zoom-study`)

1,296 pointer-anchored steps — every adjacent field transition and
its reverse, nine pointer positions (centre, edge midpoints,
corners), eight pages (equatorial, mid-northern, RA-wrap, dec 60/85,
dec 89.9, southern, dec −85):

- **1,197 exact, 64 constrained, 35 past-pole — no other empties.**
- **Worst pointer drift after an exact step: 5.4e-4 px** (the
  solver's plane tolerance expressed in pixels; subpixel by three
  orders of magnitude).
- **Ordinary round trip** (one step and its reverse, both exact):
  centre restored within **9.8e-7 degrees**, pointer within
  **3.8e-4 px**. This is the reversal guarantee and its stated
  tolerance: **1e-6 degrees, 1e-2 px**.
- **75 round trips took a second exact branch**: on near-polar pages
  whose pointer anchors sky beyond the pole, the reverse step's
  declination equation has two exact roots; the drag-continuity
  tie-break picks the branch nearer the mid centre, which need not
  be the original (worst centre difference 28.3°, the original
  centre verified still exact in every case). This is the pan
  decision's alternate-meridian physics, inherent to north-up
  geometry at the pole — documented, classified, and detected by
  verification in the study, never averaged into the guarantee.
- **Pure geometry: 2.0 µs per solve.** A realistic five-notch
  outward burst through the full pipeline — solve, accepted state
  transition, scene assembly, render — costs **5.5–9.9 ms per
  notch** against the 16.7 ms frame budget: every notch can be a
  fully applied, fully rendered step with no coalescing, no
  animation, and no background machinery.

## Behavior at the limits

- **RA wrap**: the solver works in the projection's own frame; wrap
  pages are in the sweep and behave identically to equatorial ones.
- **High declination (constrained, 64 classified)**: a north-up page
  cannot put arbitrary sky at an arbitrary pixel near a pole. The
  clamped solve follows the pointer as far as the feasibility
  boundary |ξ| ≤ cot|δ|·√(1+η²) allows — the same physics, the same
  classification, and the same honest behavior as panning there.
- **Past-pole (35 classified)**: no centre can honour the anchor.
  **The step is refused: chart unchanged, nobody notified** — the
  pan hold's rule. Centre-preserving zoom (toolbar, keyboard)
  remains available at the same pointer; the wheel resumes the
  moment the pointer anchors feasible sky.
- **Sequence ends**: at 36° zooming out and 1° zooming in the wheel
  does nothing (the discrete sequence is the reviewed scale
  contract; no overshoot, no easing).
- **Coverage**: the candidate state must pass the same coverage
  predicate every navigation passes (`SceneAssembler.fits`); a
  refused candidate is a complete no-op. Under the bundled all-sky
  pack nothing is ever refused; the contract exists for regional
  futures.

## Wheel and trackpad grammar

- **Direction**: rotating toward the reader (positive rotation)
  zooms **out**; away zooms **in** — the platform's map-and-document
  convention.
- **Discrete steps**: one wheel notch = one field step. High-
  resolution trackpads report fractional rotations:
  `getPreciseWheelRotation` values **accumulate**, a step fires each
  time the magnitude reaches 1.0 (consuming 1.0, keeping the
  remainder), and a direction reversal discards the opposing
  remainder. No momentum, no easing, no animation.
- **Bursts**: each accepted step applies fully (measured above at a
  third to a half of the frame budget); a burst of notches is a
  sequence of honest pages, never a skipped or interpolated one.
- **Consumption**: wheel events over the chart paper are consumed —
  including at sequence ends — so the chart owns its wheel; events
  over letterbox chrome are left alone.

## The pointer in letterbox chrome

A pointer outside the paper page (the component's existing
`isOnPaper` boundary) anchors no sky: **the wheel does nothing
there**, and the event is not consumed. No fallback to centre zoom —
an anchored gesture with no anchor is a refusal, not a guess.

## Target and title honesty

**The target survives exactly when the centre survives.**

- Toolbar and keyboard zoom are centre-preserving: they keep the
  searched target and its title, exactly as released.
- A pointer-anchored step that moves the centre is an **anonymous
  recenter** — the atomic pan rule extends unchanged: target
  identity and title clear together on the first accepted step, and
  the chart titles honestly by its coordinates.
- A pointer-anchored step whose solved centre is the current centre
  (the pointer sits on the page centre) is a pure field change and
  keeps the target — the same transition the toolbar performs.

## Keyboard and platform shortcuts

- The **View menu** gains **Zoom In** and **Zoom Out** items wired
  to the exact toolbar actions (centre-preserving, target kept, same
  enablement at sequence ends).
- **Accelerators** use the platform menu mask
  (`Toolkit.getMenuShortcutKeyMaskEx()`: ⌘ on macOS, Ctrl
  elsewhere): Zoom In on **mask+'='** (the key that carries '+'
  unshifted-less keyboards type), **mask+Shift+'='** (explicit
  '+'), and **mask+keypad-add**; Zoom Out on **mask+'-'** and
  **mask+keypad-subtract**.
- **Keyboard zoom is centre-preserving** — it has no pointer anchor,
  and anchoring it at the last known pointer position would teleport
  the chart on an invisible dependency (rejected).
- **While Search has focus**, the accelerators still work — they all
  carry the menu mask, so unmodified `+`, `-`, and `=` remain
  ordinary text that reaches the field untouched. Nothing intercepts
  plain keystrokes.

## Rejected alternatives

- **Continuous field widths**: every scale policy (regional detail,
  geography, star labels) is decided against the discrete sequence;
  continuous zoom would re-open all of them for a cosmetic gain.
- **Animated/momentum zoom**: restraint; each notch is already a
  complete honest page inside the frame budget.
- **A dedicated zoom solver**: duplicates the pan closed-form.
- **Centre-zoom fallback for letterbox or past-pole wheel events**:
  a pointer gesture that silently changes meaning is worse than one
  that visibly does nothing.
- **Anchor-preserving keyboard zoom**: see above.

## Consequences

- #124 implements the accepted transition as one atomic controller
  operation (`ChartViewController`, built on the pan seams) with the
  study's classifications as its contract; #125 wires the wheel
  (accumulator, consumption, letterbox refusal) and the platform
  shortcuts; #126 walks the journey and hands over.
- The geometry tests lock the invariant, the reversal tolerance, the
  constrained and past-pole classifications, and the verified
  second-branch physics, so the production implementation has its
  acceptance written before it exists.
- No renderer, catalogue, or projection change; the M31 reference
  stays byte-identical through this gate.
