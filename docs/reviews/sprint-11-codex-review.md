# Sprint 11 Codex review — Give the application a public face

Reviewed 2026-08-30 at `be34406`, before the proposed 0.11.0 release.

## Findings

### P1 — Keep `--dark` authoritative through the Settings path

`JUranometriaMain` applies `darkOverride` only when constructing the initial
`dark[0]`. Settings then receives that effective value as if it were the saved
choice, and its OK path always applies and persists the selected radio button.
Consequently, a launch with `--dark` over a saved Light preference can rewrite
the store to Dark merely by opening Settings and pressing OK. Selecting Light
and pressing OK also turns that supposedly overridden session light. Both
behaviours contradict the documented and accepted contract that `--dark` wins
for the session without rewriting the saved choice.

Carry the override through the application appearance/session policy rather
than collapsing it into the initial boolean. While an override is active, the
dialog must distinguish the saved preference from the effective appearance.
It may save an explicitly chosen preference for the next ordinary launch, but
the current session must remain Dark; merely confirming the dialog must not
convert the command-line override into a stored Dark preference. Exercise this
through the production Settings-confirmation path, not only through
`AppearanceStore.sessionDark()` in isolation.

### P2 — Test licence assignments, not just the presence of licence words

`AboutDialogTest.theSummaryAgreesWithTheRepositoryLicensingDocument()` checks
that each licence identifier occurs somewhere in `LICENSING.md` and that both
documents mention a non-commercial consequence. It never checks which resource
each licence belongs to. The packaged summary could accidentally say that
OpenNGC is MIT and Tabler is CC BY-SA while the test continued to pass. That is
weaker than the handover's claim that the two surfaces are held in agreement,
and this is precisely the content where a misleading green test would matter.

Assert the resource/licence relationships in both documents—code/MIT,
Tycho-2/CC BY-NC plus its practical consequence, OpenNGC/CC BY-SA,
constellation geography/BSD-3-Clause, and Tabler/MIT—or derive both surfaces
from a small structured source if that remains simple. Keep the different prose
appropriate to its audience; the test needs semantic pairings, not a full text
comparison.

## What was verified

- GitHub's required `test` check is green on JDK 21/Linux.
- Headless local run: 217 tests discovered, 216 successful, and the one
  display-dependent journey explicitly aborted by assumption as documented.
- The headless-safe About, menu, preferences, and Settings tests pass.
- The packaged JAR contains its `VERSION`, compact licensing summary, and the
  existing notices/licence resources used by the full view.
- `make chart-image` reproduces `docs/reference/m31-stars.png`
  byte-for-byte.
- The packaged build carries the new menu/dialog classes and resolves the
  existing adjacent runtime libraries.
- No chart, catalogue, projection, or rendering production class changed.
- `git diff --check` found one trailing blank line at the end of
  `LICENSING.md`; remove it with the follow-up.

## Assessment

The public surface is otherwise well judged. About presents the practical
non-commercial consequence in its compact offline view instead of hiding it in
legal text, and the detailed view reuses notices already shipped in the JAR.
The menu stays restrained, with no premature Chart Options placeholder. The
preference boundary is small, JDK-only, injectable, and independent of chart
state.

The File-menu compromise is also reasonable for this cross-platform stage.
Native macOS About and Preferences handlers can remain a later refinement;
duplicating the macOS-provided application menu would have been worse.

## Recommendation

Fix the session-override contract and strengthen the licensing agreement test,
then merge PR #101, close milestone 11, and cut 0.11.0. No change to the visual
design or licensing prose is otherwise requested.
