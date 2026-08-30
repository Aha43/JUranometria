# Sprint 14 handover — Zoom where you point

Sprint 14 (issues #123–#126, milestone 14) made scale navigation
direct: the wheel zooms where the reader points, holding the sky
beneath the pointer to the pixel, and the platform's zoom shortcuts
work everywhere the window is active. This handover is the coder's
account for the independent review before 0.14.0.

## What Sprint 14 delivered

- **The decision** (#123, PR #127): `docs/decisions/pointer-zoom.md`
  — the invariant expressed entirely through the pan seams (no new
  solver; a dedicated one was rejected as a duplicate of the
  reviewed closed-form), and the **acceptance contract** the review
  sharpened: a pointer step is accepted only when it is *exact and
  preflight-reversible at the same pointer* — constrained,
  past-pole, ambiguous, and non-restoring-reverse outcomes all
  refuse. `make zoom-study` measured 1,296 steps: 1,112 accepted,
  every refusal attributed, anchor drift 5.4e-4 px, reversal by
  construction within 5.8e-5°, 2 µs per solve, bursts at 4.9–10.4 ms
  per fully-rendered notch. The one production addition:
  `PanSolution.ambiguous`, reporting a second verified exact centre
  (panning keeps its reviewed continuity tie-break unchanged).
- **The transition** (#124, PR #128): `ChartViewController.zoomAt` —
  one atomic recenter-plus-field change, pixel-free like `pan`, with
  the classified `PointerZoomOutcome` (accepted / at-bound /
  infeasible-pointer / coverage-refused); refusals change nothing
  and notify nobody, per category. Target honesty as decided: a step
  that moves the centre is an anonymous recenter; the exact page
  centre degenerates to the toolbar's centre-preserving transition
  and keeps the target.
- **The wiring** (#125, PR #129): `ZoomInteraction` — one notch one
  step, trackpad rotations accumulating with the remainder kept and
  direction reversals discarding it, paper events consumed (bound
  and refused notches included), letterbox chrome left alone, and
  the burst loop re-reading the page geometry per step (the review's
  stale-geometry finding). View-menu Zoom In/Out plus
  `installZoomShortcuts`: every practical plus/minus form on the
  platform menu mask through shared guarded actions — no OS-name
  conditionals, unmodified typing never intercepted.
- **The journey** (#126): `DirectZoomJourneyTest` on a real shown
  window — search Betelgeuse, wheel out four fields with the sky
  beneath an off-centre pointer held to the pixel, hit the bound
  (consumed, `assertSame` scene — no assembly), reverse the burst,
  zoom by masked keys dispatched through the Search field itself
  (and prove unmodified `=` is just text), agree with the toolbar,
  pan, filter magnitudes, wheel again, and come home to the exact
  released default by the real toolbar control.

## Findings made along the way, recorded honestly

- **The polar branch ambiguity** (gate review): near-polar pointers
  can admit two exact centres up to 28.3° apart. Decided as refusal,
  not documentation — and the preflight makes acceptance symmetric,
  verified over all 1,112 accepted steps.
- **Constrained steps refuse** (gate follow-up): the polar clamp
  right for a drag would miss a wheel pointer by a measured 112 px
  mean / 220 px worst — measured to justify the refusal.
- **Stranding is unreachable** (PR #129): a pointer close enough to
  a window edge to be letterboxed by a re-capped paper anchors sky
  at plane offsets whose reverse has a second exact root, so the
  contract refuses the first step before stranding could occur. The
  gate's 700-px pages capped |η| at ~0.25 and never saw this; tall
  letterboxed windows are where large plane offsets live. The
  in-loop paper check stays as documented defence in depth.

## Verification

- 295 tests, 0 failures on a display; headless CI aborts the five
  display-dependent tests visibly by assumption (four acceptance
  journeys and the dialog single-instance test); required `test`
  check green on every sprint PR.
- Clean bootstrap re-run this session (`lib/` deleted, full suite
  green); `make chart-image` byte-identical to the M31 reference —
  **untouched this entire sprint** — and the committed star-identity
  studies reproduce exactly.
- Packaged `make jar` + `java -jar`, light and `--dark`, verified.
- Performance: gate-measured 4.9–10.4 ms per fully-rendered notch
  against the 16.7 ms budget; each wheel step is synchronous through
  the controller, so no stale assembly queue can form.

## Residual risks, stated honestly

- **Refusals are silent by design** (the pan hold's rule). A reader
  wheeling at a refused polar pointer sees nothing move and gets no
  message; the toolbar and keyboard always work. If reviews find
  this too quiet, a status-line hint would be the contained answer.
- **Large plane offsets refuse often**: on very tall letterboxed
  windows, pointers in the outer band anchor sky the contract
  refuses (ambiguous reverse). Geometrically honest, but a reader
  with an unusual window shape may find the wheel selective near the
  page's far edges.
- **Trackpad feel is device-mediated**: macOS delivers momentum
  events after the fingers lift; each still accumulates by the
  decided rule (no synthesized inertia), so a hard flick can step
  further than a cautious reader expected. The discrete sequence
  bounds the damage at 9 steps.
- **The View menu now depends on the navigation controller** — the
  first menu wiring that touches chart state. It goes through the
  same controller API as the toolbar, kept deliberately thin.

## Sprint review answers

- **Does zoom feel direct?** Yes — the sky beneath the pointer stays
  beneath the pointer to ~5e-4 px, every notch a complete honest
  page well inside the frame budget, and the wheel, keyboard,
  toolbar, and menu agree on one controller contract.
- **Did directness cost honesty?** No: every refusal is classified
  and inert (never drift, never a silent branch switch), the target
  clears exactly when the centre moves, and reversibility is part of
  the acceptance, not a statistic.
- **Did directness cost architecture?** No: one controller
  transition on the existing pan seams, one wheel listener, one
  shortcut installer; the solver gained a report, not a behavior;
  policies, renderer, catalogue, and toolbar are untouched.
- **Was restraint kept?** No animation, no momentum, no continuous
  zoom, no new toolbar controls; the menu gained exactly the two
  conventional items.
- **What next?** **Sprint 15: the equatorial coordinate grid** — the
  atlas now names its stars and moves like a physical chart; the
  last recorded convention gap is the quiet RA/Dec graticule the
  chart-conventions document has planned from the start, and the
  Chart Options dialog is its natural toggle home. **Sprint 16
  remains the planned 1.0 stabilization/release sprint** — planned,
  not opportunistically begun. No evidence this sprint surfaced
  anything more urgent.

## Process expectations

The established pattern: this handover accompanies the open sprint
PR; the independent Codex review lands as
`docs/reviews/sprint-14-codex-review.md`; findings are fixed on the
PR; both documents are committed with the fixes; then merge, close
milestone 14, and cut 0.14.0.
