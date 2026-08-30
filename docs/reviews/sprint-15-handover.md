# Sprint 15 handover — Read the coordinates

Sprint 15 (issues #132–#135, milestone 15) gave the atlas its
equatorial graticule: a quiet, projection-correct ICRS/J2000 grid
that explains position and scale at every released field, on by
default, one Chart Options toggle away. This handover is the coder's
account for the independent review before 0.15.0 — the last feature
sprint before the planned 1.0 stabilization.

## What Sprint 15 delivered

- **The decision** (#132, PR #137): `docs/decisions/coordinate-grid.md`
  — constant-RA meridians and constant-Dec parallels sampled along
  the sky and clipped per piece (worst chord error 0.004 px against
  the true projected midpoint; stated tolerance 0.05 px); ONE
  adaptive interval rule per axis (smallest pleasant step spacing
  ≥ 110 px at the page centre, RA scaled by cos dec, capping at 6h
  so the pole draws the classical four-meridian chart); grid-notation
  edge labels under one exact bounds calculation (the review's
  correction — placement, paper containment, title suppression, and
  drawing from the same font-metrics box); the quietest ink beneath
  everything; **default ON, owner-approved**, with the deliberate
  M31 reference change called out at the gate.
- **The renderer** (#133, PR #139): `render.EquatorialGrid` IS the
  gate's geometry moved to production (the study delegates
  entirely); the pass draws first, beneath geography; a pure
  function of the viewport with no catalogue or geography query
  reachable by type; 0.74 ms mean per page. The M31 reference
  deliberately updated; the twelve study pages regenerated as
  production output; the gate's absent/lines-only/labelled triple
  kept as decision evidence.
- **The option** (#134, PR #140): `ChartOptions.equatorialGrid`,
  seventh boolean, default on; `chart.equatorialGrid` persisted with
  the only-literal-"false"-disables rule so pre-grid stores migrate
  silently; "Equatorial coordinate grid" as the fourth Content
  checkbox with an ICRS/J2000 accessible description; the full
  protocol proven for grid-off end to end (store round trip,
  restart, Cancel, Home independence, Restore Defaults — the
  review's requested evidence).
- **The journey** (#135): `CoordinateGridJourneyTest` on a real
  shown window — the default page draws its graticule; real drags
  carry a Pegasus page across 0h; wheel bursts step the RA interval
  wider and back at the reviewed thresholds; the polar page caps at
  6h; the southern sky composes (Acrux picked from the honest
  results popup beside Gacrux); the real dialog hides the grid
  repaint-only (same scene, same navigation, target kept), Cancel
  restores, OK persists, Home leaves the choice alone, a restart
  honours it, and Restore Defaults ends on the exact decided
  default page.

## Ripples, recorded honestly

- **The Sprint 13 star-identity study pages now carry the grid**:
  they are production output by their own reviewed decision, so the
  default-on graticule appears in them; all nine regenerated and
  committed. The Sprint 13 *decision* pages were measured gridless
  and their numbers are unaffected (the grid joins no collision
  set).
- **Two renderer tests were adjusted, not weakened**: the Messier
  label-silence and tiny-viewport title-omission assertions now
  count *content ink* (darker than the grid's quiet band), keeping
  their meaning under an always-on graticule.
- **Interval ladder gained a 15′ rung**: the gate's own density test
  caught the 1° page's Dec spacing breaking the pleasant band — the
  kind of finding the gate exists to force before production.

## Verification

- 309 tests, 0 failures on a display; headless CI aborts the six
  display-dependent tests visibly by assumption (five acceptance
  journeys and the dialog single-instance test); required `test`
  check green on every sprint PR.
- Clean bootstrap re-run this session (`lib/` deleted, full suite
  green); `make chart-image` byte-identical to the deliberately
  updated M31 reference; `make grid-study` and
  `make star-identity-study` reproduce every committed page
  byte-for-byte.
- Packaged `make jar` + `java -jar`, light and `--dark`, verified.
- Performance: grid computation 0.74 ms mean per page through
  production; repaint-only toggling proven at the scene instance;
  no catalogue/geography query reachable from the grid seam by
  type.

## Residual risks, stated honestly

- **Grid labels can be overpainted**: star and deep-sky labels are
  deliberately unaware of grid labels (grid ink is subordinate), so
  a dense page can paint content over a coordinate label. Accepted
  by the decision; the study pages show it stays legible in
  practice.
- **The default-on change touches every visual consumer**: any
  external screenshot or downstream comparison made against ≤0.14.0
  pages will differ by the graticule. The toggle restores the old
  look in one click.
- **Suppression is per-edge-crossing**: a curve that misses its
  labelling edge stays unlabelled (honest omission), so unusual
  aspect ratios can show sparse labelling. The letterbox and
  minimum-window study pages bound how sparse.
- **AA fringes over grid lines** differ from the gate's composited
  pages by design; the committed pages are production output, so
  this is history, not a live risk.

## Sprint review answers

- **Can the reader read the coordinates?** Yes — at every released
  field, across the wrap and at both poles, in formal notation, on
  by default and one honest toggle away.
- **Did the grid cost quiet?** No: the quietest ink on the chart,
  beneath everything, proven at the pixels (a starless page's every
  inked pixel outside frame and title stays ≥ 148 grey) and by the
  committed comparison triple.
- **Did the grid cost architecture?** No: one pure viewport-only
  seam, one renderer pass, one option in the established contract;
  policies, catalogue, geography, and navigation untouched.
- **Was restraint kept?** One rule for intervals instead of tables,
  one label per curve, no rulers, no styling controls, no toolbar
  ink.
- **What next?** **Sprint 16: the planned 1.0 stabilization and
  release sprint** — the recorded route, now due: the atlas draws,
  names, navigates, and coordinates its sky; nothing this sprint
  surfaced is more urgent than finishing. Deferred maintenance
  (#88 release automation, #97 Actions runtime) belongs in that
  sprint's sweep.

## Process expectations

The established pattern: this handover accompanies the open sprint
PR; the independent Codex review lands as
`docs/reviews/sprint-15-codex-review.md`; findings are fixed on the
PR; both documents are committed with the fixes; then merge, close
milestone 15, and cut 0.15.0.
