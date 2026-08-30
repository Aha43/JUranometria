# Sprint 14 Codex review — Zoom where you point

Reviewed 2026-08-30 at `eec763d`, before the proposed 0.14.0 release.

## Findings

### P1 — Exercise a real coverage refusal in the closing journey

Issue #126 explicitly requires the journey to exercise both a coverage refusal
and a sequence bound, and says that removing the coverage guard must make the
journey fail. `DirectZoomJourneyTest` proves the 36-degree sequence bound at
lines 118–127, but it never reaches a state where the shared coverage predicate
rejects a candidate. The production controller could stop consulting
`Atlas.assembler()::fits` and this sprint-closing journey would still pass.

Add a production-path leg at a coverage edge that dispatches the relevant real
wheel event, proves it is consumed, and proves state and scene identity remain
unchanged (therefore no assembly/query). Keep the existing bound leg: the two
refusal reasons are separate contracts. If the complete all-sky pack makes a
natural coverage edge impossible in this particular window, use the smallest
explicitly fenced controller/assembler fixture that retains the real
`ChartComponent` and `ZoomInteraction`, and state that limitation honestly in
the handover.

### P2 — Make the reverse-round-trip assertion real

The journey prose says the four-step reverse returns the original view within
the reviewed tolerance, but the test records `beforeReverse` only after reaching
36 degrees and never compares it with anything. The final assertion

```java
pointerDrift(anchor, px, py) >= 0.0 || beforeReverse != null
```

is unconditionally true for the constructed values and exists only to consume
that variable. The anchor-drift assertion is valuable, but it is not the stated
centre round-trip check. Capture the centre immediately before the outward
wheel sequence, assert the centre after the reverse burst returns within the
reviewed angular tolerance, and remove the tautological final assertion.

## What was verified

- GitHub's required `test` and GitGuardian checks are green at the PR head.
- Headless local JDK 21 run: 295 tests discovered, 290 successful, five
  display-dependent tests aborted by assumption, and no failures.
- The closing changes are confined to the journey, documentation, menu-mask
  visibility needed by that journey, and the chart's accessible description.
- `git diff --check` is clean before adding this review.
- The individual Sprint 14 decision, controller transition, wheel wiring, and
  shortcut PRs received independent review; their follow-up findings were
  resolved before this closing pass.

The display-dependent journey could not execute in this review environment.
Its author reports a successful 295/295 display run. The requested changes make
that run prove both refusal categories and the claimed round trip.

## Assessment

Sprint 14 is architecturally restrained and the interaction model is strong.
Pointer zoom reuses the exact projection and pan geometry, keeps one discrete
field sequence, refuses ambiguous polar solutions instead of choosing a
surprising branch, and preserves the atlas's synchronous query-to-pixels
contract. Refreshing letterbox geometry inside multi-notch bursts was an
especially worthwhile review correction.

The recorded residual risks are fair for 0.14.0. Silent polar refusal and
device-mediated trackpad momentum deserve observation in normal use, but they
do not justify animation, synthetic inertia, or more controls now.

## Recommendation

Close the two acceptance-test gaps, then merge PR #130, close milestone 14, and
cut 0.14.0. No production behavior or visual-policy change is requested. The
proposed Sprint 15 coordinate grid remains the natural next feature, followed
by the planned 1.0 stabilization sprint.
