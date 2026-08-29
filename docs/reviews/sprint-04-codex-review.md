# Sprint 4 Codex review

Reviewed 2026-08-29 from `v0.3.0` through PR #39 at `addd58b`.

## Result

Sprint 4 delivers the intended find-and-go journey with a restrained toolbar,
a useful source-independent local index, immutable centre state, and one-step
recentring. Search selection itself observes the bundled coverage contract and
the default chart remains deterministic.

Three findings should be resolved before the 0.4.0 release.

## Findings

### P1 — Keep zoom transitions inside coverage after recentering

The search field chooses a fitting field when it recentres, but subsequent
toolbar zoom transitions use only `ChartViewState.canZoomOut()` and never
consult `SceneAssembler.fits`. This creates a normal path into an invalid view.

For example, a result 5° north of the data centre is correctly opened at 6°:
`5 + 3 + 1.5 = 9.5°`. The Zoom out button remains enabled and moves the same
centre to 8°, which does not fit: `5 + 4 + 1.5 = 10.5°`. At that state
`maxPageHeightPx` returns zero; `ChartComponent` then attempts to assemble a
zero-height viewport and fails on the EDT. The controller is left holding the
invalid state, so later resize/repaint activity can continue failing.

Coverage must govern every navigation transition, not only search selection.
Put the validity rule at a shared navigation boundary, refuse invalid updates
before notification, and disable Zoom out when the next field step does not
fit around the current centre. Add a real-button EDT test following the exact
sequence: select an offset result, observe the fitting field, attempt/inspect
Zoom out, and confirm no invalid state or second scene is produced.

### P2 — Do not report a zero-height boundary as fitting

`SceneAssembler.fits` uses `<=` for the horizontal coverage equation. At exact
equality it returns true even though no vertical extent remains. With an 8°
field, a centre exactly 4.5° from the M31 data centre satisfies
`4.5 + 4 + 1.5 = 10°`; `fits` returns true while `maxPageHeightPx` returns zero.
A coordinate search can construct this position directly and the apply policy
therefore selects it as a valid view before component assembly fails.

Define fitting in terms of a positive drawable page, or make the boundary
strict with a deliberate numerical tolerance/minimum page height. Tests should
cover exact equality and positions immediately on each side of it. The same
predicate should be used by search, toolbar transitions, and scene assembly so
they cannot disagree.

### P2 — Validate sexagesimal minutes and seconds

`LocalSearch.sexagesimal` checks that three components exist, but it does not
require minutes and seconds to lie in `[0, 60)`. Invalid coordinates are
silently normalized into different valid positions. For example,
`1:-30:00 +41:00:00` is accepted as RA 7.5°, and `0:99:00 +41:00:00` is also
accepted instead of returning no result. Negative subcomponents and values of
60 or more must be rejected for both RA and declination; the hour/degree limits
already applied to the total should remain. Add focused boundary tests for
59:59.9, 60:00, and negative minute/second components.

## Verification

- `make test` passed: 122/122.
- `make chart-image` is byte-identical to
  `docs/reference/m31-stars.png` (SHA-256
  `021a0e1e1cafbe95ce789d102d5ff7fe6c78911bb49c3f648a3e687068aa68aa`).
- `git diff --check` passed.
- Search selection emits one controller notification for fitting and
  coverage-narrowed results.
- Local search and scene assembly are built once over the same loaded
  `BundledCatalogue`; normal operation remains local.
- Exact/prefix/partial ordering, alias deduplication, result bounds, reset,
  and real Swing search actions were inspected.
- Independent probes reproduced the invalid zoom state, the zero-height exact
  boundary, and acceptance of malformed sexagesimal input.

## Review notes

The documented interpretation of two bare numbers as decimal RA/Dec is
reasonable. The no-match and no-fit wording now states only what the regional
catalogue actually knows. The popup remains compact; the current bundled
labels do not justify additional truncation machinery.
