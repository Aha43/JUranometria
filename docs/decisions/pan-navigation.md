# Decision: projection-correct grab-to-pan geometry and interaction

Decided 2026-08-29 for Sprint 8, "Pan across the local sky" (issue
#72), from an exact solver implemented and measured against the real
gnomonic projection and page geometry (`make pan-study`;
`PanGeometry` and `PanGeometryTest` are the worked evidence). The
user-visible promise: press on the paper, move the pointer, and the
same place in the sky follows the hand — physically, at every released
field, near the poles, and across RA 0.

## Coordinate conventions (unchanged, now with an exact inverse)

- Forward: `GnomonicProjection` maps ICRS J2000 to tangent-plane
  (ξ east, η north); `ViewportMapping` maps plane to pixels with east
  left, north up: `px = cx − ξ·ppu`, `py = cy − η·ppu`,
  `ppu = width / (2 tan(field/2))`.
- Inverse, added by this gate: pixel → plane by inverting the linear
  mapping; plane → sky by the exact inverse gnomonic
  (`c = atan ρ`; `dec = asin(cos c sin δ₀ + η sin c cos δ₀ / ρ)`;
  `ra = α₀ + atan2(ξ sin c, ρ cos δ₀ cos c − η sin δ₀ sin c)`).
  Round-trips verify to 1e-12 plane units across ordinary, wrap, and
  polar centres.

## The grab invariant and the centre solver

**Invariant: the sky position under the pointer at press time remains
under the pointer throughout the gesture.** Every drag event solves,
exactly: find centre `c` such that the grabbed position `s` projects
to the pointer's plane point `(ξ, η)`.

In the centre's orthonormal frame `(c, ê east, n̂ north)` the
invariant reads `s = (c + ξ ê + η n̂)/N`, `N = √(1+ξ²+η²)`, which
separates closed-form: `s·ê = ξ/N` depends on the centre's RA alone
(`cos δ_s · sin(ra_s − α) = ξ/N`, two α families), then `s·c = 1/N`
gives the declination (`p cos δ + sin δ_s' … `, two roots each). Up to
four algebraic candidates arise; **each is verified by full forward
reprojection** and the valid candidate nearest the previous centre is
chosen, keeping a continuous drag continuous. Never a degrees-per-pixel
approximation anywhere.

## Numerical tolerance and failure behaviour

- Candidate acceptance: reprojection within **1e-6 plane units**
  (≈ 0.0014 px at the widest page). The floor is principled: drags
  along a plane axis make the declination equation a double root,
  which floating point resolves to ~1e-8 plane units — a tighter
  tolerance rejects exact solutions (found and fixed by the study).
- Measured closure over the acceptance grid (8°/18°/36° × ordinary,
  RA-wrap, both polar centres × straight, diagonal, near-corner, and
  edge-crossing drags): worst **9.6e-5 px**, typically 1e-10 px.
- **Failure is explicit, classified, and never silent**: the solver
  returns a classified outcome (exact, constrained follow, or
  past-pole hold). An infeasible horizontal request follows to the
  feasibility boundary (below); only a past-the-pole request holds
  the previous centre, and that hold carries the solver's own
  evidence (an algebraic declination root beyond the pole). **An
  empty result without past-pole evidence is a solver invariant
  violation and throws** - other failure modes cannot hide behind a
  quiet no-op. Nothing NaN or out-of-range ever reaches
  `ChartViewState`.

## Gesture semantics: the press-time grab is the reference

Every event of a gesture solves against the **same press-time grabbed
position**, never incrementally against the previous event. Measured
consequence: a gesture that wanders through 50 random waypoints and
returns its pointer to the press pixel restores the centre **bit
exactly (0 px)**. Cross-gesture out-and-back (release, regrab at the
new centre, drag back) does not close — by geometry, not error: 50
such loops accumulate the genuine spherical holonomy of north-up
panning (measured, e.g. ~2.2 kpx at the wrap/36° case). Incremental
per-event grabbing was rejected precisely because it would leak that
holonomy *into* a single gesture as apparent drift.

## Polar behaviour: constrained follow, derived and measured

A north-up gnomonic chart pins the celestial pole to the page's
vertical axis (`ξ_pole = 0` for every centre); a grab at declination
δ_s can only occupy plane points with `|ξ| ≤ cot|δ_s|·√(1+η²)`.
The decided contract (revised for the PR #76 review's P1):

- **The sky follows the hand as far as the geometry allows.** When
  the pointer leaves the grab's feasible set, the solver clamps the
  horizontal component onto the feasibility boundary and solves
  exactly there — the vertical component keeps tracking exactly, the
  horizontal follow stops at the boundary. The study reports each
  constrained event with its shortfall (e.g. a 200 px horizontal pull
  on the near-polar grab follows all but 117–182 px depending on
  field). Polar pages never freeze under the hand.
- **Panning past the pole holds**: when even the clamped target has no
  centre inside the valid declination range — the drag would carry
  the centre across the pole — the event holds the previous centre
  explicitly. The drag has pulled the sky as far as it goes.
- Grabbing **any less extreme point** of a polar page pans freely
  (worked test). No special polar mode exists; both behaviours fall
  out of the same solver.

## Interaction semantics for #73/#74

- **Drag threshold: 4 device pixels** from the press point separates
  click/jitter from a real drag; below it, no state changes.
- **Crossing the threshold clears the searched target** — label and
  identity together (the atomic rule) — and every subsequent title is
  the honest coordinate title. Field width and limiting magnitude are
  untouched by panning.
- **Home/Reset is unchanged**: the exact released M31 8°/V 8.0
  default. No navigation history, no "return to last target".
- Press in the letterboxed surround starts no gesture; once a gesture
  is live, pointer positions outside the paper (and outside the
  window) keep solving — every finite plane point has a pre-image, so
  a drag crossing a page edge simply continues.
- Cursor expectation for #74: open hand over the paper, closed hand
  while a gesture is live, default cursor over the letterbox.

## Event handling: direct and synchronous, with evidence

A simulated gesture of 120 consecutive events, the pointer advancing
5 px per event, each paying the full solve + assemble + render cost
through the real seam, warm:

| Field | median | p95 | max | 60 Hz budget |
|---:|---:|---:|---:|---:|
| 8° | 1.3 ms | 3.0 ms | 4.5 ms | 16.7 ms |
| 18° | 2.5 ms | 5.0 ms | 5.8 ms | 16.7 ms |
| 36° | 3.8 ms | 4.6 ms | 5.9 ms | 16.7 ms |

Every percentile sits far inside one frame, so **each MOUSE_DRAGGED
event is handled synchronously on the EDT: solve, assemble once
through the established scene seam, repaint**. No custom coalescing
machinery is built; AWT already coalesces consecutive MOUSE_DRAGGED
events when the EDT is busy, which is the natural backstop if a slow
machine ever needs one. Repaint performs no data queries, as always.

## Rejected alternatives

- **Linear RA/Dec-per-pixel offsets** — wrong everywhere it matters:
  RA degrees shrink with cos δ (unusable near the poles), and the
  gnomonic scale grows sec²θ off-centre (17% at the 36° corner), so
  the grabbed point would slide out from under the hand.
- **Scrollbars/sliders** — the sky is a sphere; it has no edges for a
  scrollbar to represent, and RA 0 would become a false seam.
- **Inertia/kinetic scrolling** — motion the hand did not make, on an
  instrument whose promise is physical control. Rejected outright.
- **Navigation history / return-to-target** — explicitly out of this
  sprint's scope; panning away from a search is an honest, titled
  departure.
- **Incremental per-event grabbing** — accumulates holonomy inside a
  gesture (measured above); the press-time reference is strictly
  better and free.
- **Asynchronous/coalesced event pipeline** — unjustified by
  measurement (max 6.9 ms against a 16.7 ms frame).

## Consequences

- #73 moves `PanGeometry` into `juranometria.project` as production
  API (inverse projection + grab solver) and gives `ChartViewState` /
  `ChartViewController` the pan transition with exactly the semantics
  above; #74 attaches the mouse listeners, threshold, and cursors and
  nothing more; `make pan-study` stays as the reproducibility path
  for every number here.
- The M31 8° reference and every released behaviour are untouched by
  this gate (design and tests only).
