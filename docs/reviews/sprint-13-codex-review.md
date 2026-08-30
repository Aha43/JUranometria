# Sprint 13 Codex review — Name the stars

Reviewed 2026-08-30 at `072f00d`, before the proposed 0.13.0 release.

## Findings

### P2 — Close the production-control and visible-label gaps in the journey

Issue #116 asks the acceptance journey to pan through real production controls,
use Home, and show that the selected star's identity is present on the chart.
`NamedStarJourneyTest` builds the real `ChartComponent` and `AtlasToolbar`, but
then bypasses both interaction surfaces: the purported real pan calls
`navigation.pan(...)` directly, and the final Home action calls
`navigation::reset` rather than the toolbar button. A missing mouse listener or
Home action listener would therefore still pass this sprint-closing journey.

The 35 Cru section has a similar last-mile gap. It proves that target identity
survives zoom and the option toggle, then states in an assertion message that
the renderer keeps the guaranteed label; it does not inspect rendered output.
The synthetic renderer test is good unit evidence, but the acceptance journey's
headline case is the real V 5.49 catalogue star, deliberately beyond every
ordinary label threshold.

Drive a real mouse drag through `ChartComponent` (as the established pan tests
do), activate Home through the real toolbar button, and add a rendering
assertion that distinguishes the guaranteed **35** label on the actual assembled
scene even while ordinary star labels are off. Keep the assertions that the
drag clears target/title atomically, the same scene survives the option repaint,
and Home restores the exact released state.

### P2 — Correct the headless verification count

The handover says that three display-requiring journeys abort by assumption on
headless CI. This PR adds `NamedStarJourneyTest`, making the observed count four:
268 tests discovered, 264 successful, 4 assumption-aborted, and 0 failed. The PR
body repeats the same three-test statement. Update the committed handover to say
four; the GitHub summary can be corrected alongside it. The author's reported
268/268 display run may remain as a separate macOS result.

## What was verified

- GitHub's required `test` and GitGuardian checks are green at the PR head.
- Headless local JDK 21 run: 268 tests discovered, 264 successful, four
  display-dependent tests aborted by assumption, and no failures.
- `make chart-image` reproduces the deliberately updated
  `docs/reference/m31-stars.png` byte-for-byte.
- Search results, titles, target identities, and structured scene identities
  agree for the representative stars covered by the journey.
- Representative wraparound, polar, dense, and southern scenes exercise the
  real renderer and repeat byte-identically.
- The final V 2.5 wide-field rule and the recorded Crux/Crucis erratum are
  present in the merged Sprint 13 implementation and documentation.
- `git diff --check` is clean before adding this review.

The display-dependent journey could not execute in this headless review
environment. Its author reports a successful real-display run; the requested
changes make that run prove the production connections its prose already
claims.

## Assessment

Sprint 13 has landed as a coherent feature rather than a decorative label pass.
The identity pack preserves structured facts, search stays fully local and
refuses silent ambiguity, the renderer uses restrained scale rules and stable
collision ordering, and target identity threads through navigation without
becoming presentation state. Correcting Crucis at the generated-data boundary
and moving the wide-field cutoff to V 2.5 were both good review outcomes.

The single-candidate placement risk is accurately recorded and acceptable for
0.13.0. Prefer-omission is already a sound baseline, and the data now exists to
judge alternate placement later from real pages rather than speculation.

## Recommendation

Strengthen the sprint journey at its three last-mile assertions and correct the
headless count, then merge PR #121, close milestone 13, and cut 0.13.0. No
production-code or visual-policy change is requested.
