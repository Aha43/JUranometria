# Sprint 8 handover — Pan across the local sky

Written 2026-08-29 by the coder for the independent pre-release review
of 0.8.0, following the established handover pattern. The sprint's
issues: #72 (pan geometry and interaction decision), #73 (controller
navigation), #74 (mouse interaction), #75 (this finish).

## What Sprint 8 delivered

- **Exact grab-to-pan**: press on the paper and the sky position under
  the pointer follows the hand, solved closed-form against the real
  gnomonic projection (`juranometria.project.PanSolver`) — never
  degrees-per-pixel. Every event of a gesture solves against the
  **press-time grab**, so a gesture returning to its press pixel
  restores the centre bit-exactly; measured closure over the
  acceptance grid is 9.6e-5 px worst.
- **Classified outcomes, honest at the poles**: the solver returns
  exact, constrained-follow, or past-pole-hold — an unexplained empty
  throws. A north-up chart pins a near-polar grab close to the page's
  vertical axis; the constrained follow tracks the vertical component
  exactly while the horizontal clamps to the feasibility boundary,
  saturated events refuse as identical-centre no-ops (no assembly),
  and only a genuine past-the-pole pull holds. Depending on the exact
  grab and direction, a deep polar pull legitimately pivots the view
  around the pole to the far meridian — the exact invariant solution,
  chosen continuously.
- **One atomic controller transition** (`ChartViewController.pan`):
  a single anonymous recenter per accepted event — one notification,
  one scene assembly; field width and limiting magnitude untouched;
  the first accepted pan after a named search clears label and
  identity together into honest coordinate titles; refusals notify
  nobody.
- **A restrained physical interaction** (`PanInteraction`): primary
  button, paper only (letterbox chrome is inert), the decided 4 px
  threshold, open/closed-hand cursors drawn in the atlas's own ink,
  gestures that keep solving beyond the page and window, release
  anywhere ending the gesture with no stuck state, and an accessible
  description documenting the interaction. No scrollbars, sliders,
  modes, inertia, or history.

## The complete journey (acceptance path, exercised)

`ExplorationJourneyTest` drives the real application objects and real
controls end to end: search `m 42` through the real `SearchField`,
zoom until Orion's geography names the region, grab the paper with
real mouse events and pan along the constellation (the grabbed sky
released under the moved pointer within a thousandth of a pixel),
verify the atomic target departure and the honest coordinate title,
zoom and change magnitude around the panned centre, then click the
real toolbar's Reset button — restoring the exact released M31
8°/V 8.0 default with its no-geography state.

## Representative verification

- **Fields and geometry**: real-event component tests at 8°, 18°, and
  36°; RA 0 crossed continuously westward; free diagonal polar drags
  tracking exactly; constrained diagonal polar follows with the
  vertical component exact; solver-classified saturation and
  past-pole holds with no extra assemblies and no stuck cursor;
  near-corner and edge-crossing drags in the solver's acceptance
  grid; paper drags inside a letterboxed window pan while letterbox
  presses stay inert.
- **Themes**: both chromes launched and inspected; the chart's ink is
  structurally theme-independent (the Sprint 7 byte-identical test),
  and the cursors are OS-level, identical in both.
- **No committed images**: panning changes no rendering rule, so the
  existing committed pages remain the visual evidence; the drag feel
  itself was verified by hand in review of #74.

## Measurements (the pan pipeline, decomposed)

| Operation (warm) | median | p95 | max |
|---|---:|---:|---:|
| Solve-only geometry per pointer event | 0.3 µs | 2.0 µs | 0.28 ms |
| Accepted pan transition (solve + state + notify, no render) | 0.4 µs | 1.6 µs | 10 ms (one JIT spike in 10,000) |
| Full query-to-pixels per drag event, 8° | 1.3 ms | 2.3 ms | 4.6 ms |
| Full query-to-pixels per drag event, 18° | 2.6 ms | 4.1 ms | 8.8 ms |
| Full query-to-pixels per drag event, 36° | 4.0 ms | 4.6 ms | 6.2 ms |

**Method**: every row reproduces from `make pan-study`. The
solve-only and transition rows are the study's committed
microbenchmarks - 10,000 samples each over a wandering 36° drag with
distinct targets, sample count and operation boundaries printed with
the results (the transition row is solve + state update + one
no-render listener notification); the query-to-pixels rows are the
study's 120-event drag burst through the real seam, one warm run. Every row sits far inside the 16.7 ms frame budget,
confirming the gate's synchronous-EDT decision; saturated and held
events assemble nothing, so no queue of obsolete assemblies can form
(AWT's native drag coalescing remains the backstop). Packaged jar:
**1.42 MiB** (+0.01 over 0.7.0 — the pan code); startup, heap, and
geography numbers are unchanged from the Sprint 7 handover.

## Worth extra scrutiny

1. **The polar boundary centre**: a hard sideways pull on the most
   extreme polar grab carries the centre essentially to the pole in
   one constrained event — the exact boundary solve, but a large
   visual step. Review judged the richer polar behaviour (saturate or
   pivot, by grab and direction) an asset; a gentler easing along the
   boundary would be a feel refinement, not a correctness fix.
2. **Cross-gesture holonomy** is inherent: separate out-and-back
   gestures do not close (north-up spherical geometry, measured and
   documented in the decision); within-gesture reversal is bit-exact.
3. **The hand cursors are programmatic glyphs** (atlas ink, white
   halo) with predefined-cursor fallback; no artwork pipeline exists,
   deliberately.

## Sprint review answers

- **Does grab-to-pan feel like moving paper, including at 36° and the
  distorted corners?** Yes — because it is not a feel, it is the
  invariant: the grabbed position stays under the pointer exactly, so
  corner distortion changes what the page shows, never how the grab
  tracks. Verified to 1e-3 px through real events at every released
  field and by hand in the #74 review ("the smoothness comes from
  anchoring every event to the original grabbed sky position").
- **Is the invariant numerically honest at RA wrap and the poles?**
  Yes: wrap crossings are continuous (no longitude jump, tested),
  polar behaviour is solver-classified (constrained follow with the
  vertical exact, saturation as an identical-centre solution,
  past-pole holds carrying algebraic evidence), and the alternate
  pivot around the pole is the exact solution, not an artifact.
- **Does the first real movement clear target authority at the right
  moment?** Yes — at the 4 px threshold, atomically (label and
  identity together, the state's own invariant), never on clicks or
  jitter (query-counted tests), and the title turns coordinate-honest
  in the same single notification.
- **Are the edge cases uneventful and recoverable?** Yes: letterbox
  presses and secondary buttons are inert; gestures continue beyond
  the page and window; release anywhere ends the gesture and restores
  the hover cursor; held events keep the gesture live and it resumes
  when the pointer returns. **Cancellation is a defined path, not an
  assumption**: a gesture whose release the chart can never receive -
  the chart hidden or removed mid-drag, or its window deactivated -
  ends through the same gesture-ending method, restoring the default
  cursor; tested while a closed-hand drag was live, including that
  stray post-cancellation drags change nothing and the open hand
  returns on the next hover.
- **Do the measurements support the synchronous EDT design?** Beyond
  doubt: the geometry costs tenths of microseconds, the whole
  transition under two microseconds, and the complete query-to-pixels
  path 1.3–4.0 ms median (max 8.8 ms) against a 16.7 ms frame —
  with refused events assembling nothing, so no obsolete-assembly
  queue can form.
- **Is Home trustworthy?** Exactly: from a searched, zoomed,
  magnitude-changed, panned view, the real Reset button restores
  `ChartViewState.DEFAULT` — M31, 8°, V 8.0, released title, no
  identity, no geography — asserted through the real toolbar in the
  journey test.
- **Is star naming still the highest-value next sprint?** Yes, more
  than ever — with one addition promoted by direct exploration.
  Panning turns the atlas into a place you wander, and the bright
  stars you wander past are anonymous dots; star names plus
  common-alias search (the standing "Betelgeuse"/"lmc" gap) remains
  the clear next step. Direct exploration also made **wheel zoom /
  zoom about the pointer** feel like the most natural companion gap —
  the hand now moves the chart but still leaves it to reach the
  toolbar to change scale; worth ranking immediately after naming.
  Keyboard panning matters for access and should ride along when
  navigation is next touched. Nothing exposed a more fundamental
  need.

## Process expectations

The established pattern: this handover accompanies the open sprint PR;
the independent Codex review lands as
`docs/reviews/sprint-08-codex-review.md`; findings are fixed on the PR
with regression tests; both documents are committed with the fixes;
then merge, close milestone 8, and cut 0.8.0.
