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
   past-pole, or **ambiguous** (the solver now reports when a second
   verified centre, more than 1e-3° from the returned one, also
   solves the request exactly — the gate's one production-code
   addition, changing no existing behavior). An empty without
   past-pole evidence still throws: failure diagnostics, never
   fallback drift.

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

- **1,162 exact, 52 constrained, 35 past-pole, 47 ambiguous — no
  other empties.**
- **Worst pointer drift after an exact step: 5.4e-4 px** (the
  solver's plane tolerance expressed in pixels; subpixel by three
  orders of magnitude).
- **The reversal guarantee, universal over accepted steps**: one
  accepted step and its accepted reverse restore the centre within
  **5.8e-5 degrees** and the pointer within **5.4e-4 px** — stated
  tolerance **1e-4 degrees, 1e-2 px**. Fifty reversals were refused
  as ambiguous (see below) — each counted, none silently wrong.
- **Constrained shortfall, measured**: where north-up geometry
  clamps the anchor (52 steps on dec ≥ 85 pages), the sky follows
  the pointer as far as the feasibility boundary allows and the
  visible shortfall is **112 px mean, 220 px worst** on a 900-px
  page — the same honest polar behavior panning shows, measured
  rather than merely counted.
- **Letterbox, exercised**: a window taller than the projection-
  sanity cap (900×5112 over a 4,712-px paper) refuses its chrome
  pointers by decision and solves paper pointers exactly
  (worst drift 2.4e-12 px) — the paper is the viewport; chrome
  anchors no sky.
- **Pure geometry: 2.2 µs per solve.** A realistic five-notch
  outward burst (6° → 36°, all five transitions executed) through
  the full pipeline — solve, accepted state transition, scene
  assembly, render — costs **4.9–10.4 ms per notch** against the
  16.7 ms frame budget: every notch a fully applied, fully rendered
  step with no coalescing, no animation, and no background
  machinery.

## Behavior at the limits

- **RA wrap**: the solver works in the projection's own frame; wrap
  pages are in the sweep and behave identically to equatorial ones.
- **High declination (constrained, 52 classified)**: a north-up page
  cannot put arbitrary sky at an arbitrary pixel near a pole. The
  clamped solve follows the pointer as far as the feasibility
  boundary |ξ| ≤ cot|δ|·√(1+η²) allows — the same physics, the same
  classification, and the same honest behavior as panning there.
- **Past-pole (35 classified)**: no centre can honour the anchor.
  **The step is refused: chart unchanged, nobody notified** — the
  pan hold's rule. Centre-preserving zoom (toolbar, keyboard)
  remains available at the same pointer; the wheel resumes the
  moment the pointer anchors feasible sky.
- **Ambiguous (47 classified): the step is refused.** On a
  near-polar page whose pointer anchors sky beyond the pole, the
  centre equation has two exact roots up to 28.3° apart. A drag's
  small increments make the pan solver's nearest-centre tie-break
  the right continuity rule there — and panning keeps it unchanged —
  but a zoom step is a large jump on which that tie-break can
  silently switch branches, so **zoom refuses the transition
  outright** (PR #127 review): chart unchanged, wheel inert at that
  pointer, centre zoom available. The refusal can be one-sided — an
  accepted step's reverse may itself be the ambiguous one (50
  measured), in which case the wheel simply will not reverse at that
  exact pointer; any other pointer, or centre zoom, still moves.
  With ambiguity refused, the reversal guarantee above holds
  universally over accepted steps.
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
an anchored gesture with no anchor is a refusal, not a guess. The
study exercises this on a genuinely letterboxed window (900×5112,
paper 4,712 px): chrome pointers refuse, paper pointers solve to
2.4e-12 px, because the paper — not the window — is the viewport.

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
- The gate's only production change is the solver's new `ambiguous`
  report on `PanSolution` — additive, exercised by pan unchanged
  (drags keep their reviewed continuity tie-break) — so no renderer,
  catalogue, or projection behavior changes and the M31 reference
  stays byte-identical through this gate.
