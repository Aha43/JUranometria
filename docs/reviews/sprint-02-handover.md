# Sprint 2 review handover

Prepared 2026-08-28 for the pre-release Codex review of Sprint 2,
"Navigate the chart" (milestone 2, issues #15–#17, PRs #18, #19, and
the PR closing #17). Written before that final PR merges, so the
review can cover it together with `main`. After review the plan is:
merge, close the milestone, release 0.2.0.

Read `docs/reviews/sprint-01-handover.md` first for the project
introduction and Sprint 1 layout; this document covers only what
Sprint 2 changed.

## What Sprint 2 delivered

The fixed Sprint 1 page became the first interactive chart: a compact
FlatLaf toolbar with zoom in/out, fewer/more stars, and reset view,
all centred on M31, all honest about the bundled fixture's limits.
Pan, search, mouse-centred zoom, persistence, layers, and new
catalogue data were deliberately excluded.

New and changed pieces:

- `juranometria.chart.ChartViewState` — immutable view state holding
  field width and limiting magnitude as **explicit step sequences**
  (field 8, 6, 4, 3, 2, 1 degrees; limit V 4.0–8.0 in whole
  magnitudes), the Sprint 2 bounds in one place. Transitions clamp to
  `this` at bounds; `can*` queries feed UI enablement; off-sequence
  values are rejected at construction. `DEFAULT` is exactly the
  Sprint 1 chart.
- `juranometria.ui.ChartViewController` — Swing-free holder that
  applies transitions and notifies listeners after real changes only
  (bound no-ops are silent); registration immediately replays the
  current state.
- `juranometria.ui.AtlasToolbar` — non-floatable toolbar: zoom pair,
  separator, minus/plus magnitude pair, separator, reset, glue, and a
  right-aligned readout ("Field 8° · Stars to V 8.0"). Icon-only
  buttons with tooltips and accessible names.
- `juranometria.ui.ChartComponent` — now adopts view states via
  `setViewState` and rebuilds its scene from the current state on
  every paint, so the title block, toolbar readout, and rendered
  content agree **by construction** rather than by value-copying.
- `juranometria.app.UiTheme` — installs a FlatLaf SVG color filter
  recoloring monochrome icon strokes per theme (dark grey on light,
  light grey on dark). The filter lives in the Swing icon pipeline
  only; chart ink is untouched.
- Icons: `zoom-in`, `zoom-out`, `zoom-reset`, `minus`, `plus` from
  Tabler Icons pinned at release v3.46.0 via
  `scripts/download-icons.sh`; provenance in
  `src/resources/icons/ICONS.md`.
- `M31Chart`'s field/limit constants collapsed into
  `ChartViewState.DEFAULT` (single source of truth); `ChartImageMain`
  renders from it, and the committed reference image stayed
  **byte-identical** through the whole sprint.
- README gained a "Using the atlas" section; changelog updated.

Tests grew from 47 to 67: exhaustive state-sequence walks including
reversibility and bounds, controller notification semantics, headless
toolbar seam tests (enablement + readout synchronization, no pixel
positions), and renderer culling at an intermediate V 6.0 limit.

## Worth extra scrutiny

1. **Synchronization claims** — "toolbar, title block, and content
   always agree" rests on every consumer reading the same
   `ChartViewState` through the controller. Check nothing caches a
   stale field width or limit (in particular `ChartComponent` rebuilds
   the scene per paint — confirm no path paints without the current
   state).
2. **Listener/EDT discipline** — `ChartViewController` is
   synchronous and unsynchronized by design; all mutations happen on
   the EDT via button actions. Confirm nothing calls it off-EDT, and
   judge whether that contract deserves an assert or doc note.
3. **The magnitude control's honesty at the top bound** — "More
   stars" is disabled at V 8.0 because the fixture holds nothing
   fainter. Judge whether the disabled control communicates "no more
   data" clearly enough, or whether that promise needs a tooltip
   nuance.
4. **Step-sequence design** — field steps (8, 6, 4, 3, 2, 1) and
   whole-magnitude limits are a product decision encoded in
   `ChartViewState`. Sanity-check the sequences against
   `docs/chart-conventions.md`'s detail-by-scale table.
5. **Icon color filter scope** — `FlatSVGIcon.ColorFilter.getInstance()`
   is a process-global filter mapping pure black. Confirm this cannot
   leak into future chart-adjacent SVG use, and that repeated
   `UiTheme.apply` calls (filter re-add) stay harmless.
6. **Keystroke-free verification** — live click-through of the
   toolbar could not be scripted on this machine (macOS accessibility
   permission); the wiring is covered by the seam tests and the
   zoom/magnitude combinations were inspected through the
   deterministic renderer (the same path `ChartComponent` paints).
   Judge whether that evidence is sufficient or a manual click-through
   should be part of the release checklist.

## Known accepted trade-offs (not findings)

Discrete steps rather than continuous zoom (deliberate honesty about
fixture data); no keyboard shortcuts beyond standard focus traversal
(Tab + Space work; accelerators are a future nicety); the readout is
plain text rather than a control; `make` still rebuilds everything.

## Sprint review answers

- **Does the toolbar frame the atlas without competing with it?**
  Yes. One thin strip, five icon buttons, one readout; the page keeps
  the entire remaining window. In dark theme the chrome recedes
  further while the page stays paper-white.
- **Does zooming reveal meaningful structure with the current
  fixture?** Yes, more than expected: at 4° and below, M32 and M110
  outgrow the minimum-symbol clamp and show their true oriented
  ellipses, and the field thins naturally toward the core.
- **Is the magnitude control understandable without clutter?**
  The minus/plus pair beside "Stars to V N.N" reads as one
  instrument; the tooltips carry the astronomy ("brighter/fainter
  magnitude limit") so the buttons don't have to.
- **Are all limits honest about the available data?** Yes, in three
  layers: the state cannot leave its sequences, controls disable at
  the bounds, and the renderer independently culls to the scene
  limit — so no combination can claim data the fixture lacks.
- **What did Sprint 2 teach us for the Local catalogue foundation
  sprint?** Three things. First, scene assembly is now the seam that
  matters: `ChartComponent` holds pre-loaded lists and the state only
  filters at render time, so a real catalogue should slot in behind
  a query-on-state-change boundary rather than a load-once list.
  Second, the limiting magnitude is already honest end-to-end, so a
  deeper catalogue only has to extend `MAGNITUDE_LIMIT_STEPS` and the
  data — the UI and renderer need no change. Third, the discrete
  step model proved pleasant to use and test; the catalogue sprint
  should keep it and tie magnitude depth to available data per field
  width instead of inventing continuous zoom.

## Process expectations

Same as Sprint 1: findings become GitHub issues or PR review comments,
changelog under `Unreleased` for user-visible work, renderer changes
require the regenerated reference image to be inspected. The reference
image was intentionally untouched this sprint — flag any diff you can
produce from it as a finding.
