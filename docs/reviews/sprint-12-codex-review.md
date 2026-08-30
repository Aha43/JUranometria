# Sprint 12 Codex review — Let the reader choose the chart

Reviewed 2026-08-30 at `ffb2117`, before the proposed 0.12.0 release.

## Findings

### P2 — Exercise a label choice in the production journey

Issue #106 asks the end-to-end journey to exercise representative content and
label choices through the real menu and dialog. `ChartOptionsJourneyTest`
currently clicks only **Deep-sky objects**. That makes Deep-sky labels
ineffective through the dependency, but it never operates a label control and
therefore cannot catch a production wiring error in either label checkbox.
The lower-level dialog and renderer tests are good, but they do not close this
explicit production-path acceptance criterion.

Add one representative label-only interaction through the open dialog—for
example turn off Constellation names while leaving figures enabled—and assert
the option reaches the real chart without changing navigation or assembling a
new scene. It may be restored with the other defaults before OK; the journey
does not need to grow into an exhaustive combination test.

### P2 — Prove scene identity directly instead of comparing identity hashes

The journey's `queryCount()` does not count queries: it returns
`System.identityHashCode(chart.scene())`, then compares two integers. Different
scene objects are permitted to share that value, so the assertion is not the
claimed proof that no reassembly—and therefore no query—occurred. The helper's
name also makes the evidence look stronger than it is when reading a failure.

Capture the `ChartScene` before changing options and use `assertSame` against
`chart.scene()` afterward. Keep the existing counting-catalogue component test
as the direct query-budget proof; together those checks establish the real
Atlas path and the actual catalogue-call contract without pretending the
production Atlas exposes a counter.

## What was verified

- GitHub's required `test` check and GitGuardian check are green at the PR
  head.
- Headless local run on JDK 21: 231 tests discovered, 228 successful, and the
  three display-dependent dialog journeys explicitly aborted by assumption;
  no test failed.
- `make chart-image` reproduces `docs/reference/m31-stars.png` byte-for-byte.
- The existing rendering matrix covers defaults, individual layer gates,
  dependencies, target exemptions, and exact geography scale thresholds.
- The real dialog test covers single-instance behavior and the production
  Escape, OK, Cancel, and window-close paths across reopen cycles.
- The changelog already carries the complete Sprint 12 user-visible change;
  README, chart conventions, and application appearance agree with it.
- `git diff --check` is clean, and the working tree remained clean after
  verification.

The ordinary local macOS test invocation aborted in native Swing startup
before JUnit produced a result. Re-running explicitly headless produced the
clean result above, while the required Linux check exercised the repository's
normal CI command successfully. This looks environmental rather than a PR
failure, but the display-dependent journey therefore rests on CI/author visual
verification in this review pass.

## Assessment

The architecture has held its shape. Options remain presentation state,
compose at the renderer's pass structure, and never leak into scene assembly
or navigation. The target exemption preserves the atlas's strongest honesty
rule, while the dependencies prevent label-only artifacts. Five controls in
two compact groups was the right call; tabs would have made this small surface
feel heavier than its content.

The persistence protocol is especially tidy: preview, cancel, confirmation,
and released defaults all live outside Swing and have direct tests. The
handover's residual-risk account is proportionate and does not use exhaustive
combination testing to disguise a simple composition model.

## Recommendation

Strengthen the production journey with one real label choice and a direct
scene-identity assertion, then merge PR #110, close milestone 12, and cut
0.12.0. No production-code or visual-design change is requested.
