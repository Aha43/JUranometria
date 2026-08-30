# Sprint 10 handover — Refresh the foundations

Written 2026-08-30 by the coder for the independent pre-release review
of 0.10.0, following the established handover pattern. The sprint's
issues, in the executed order: #93 (retire the partial PowerShell
bootstrap), #83 (FlatLaf 3.7.2), #84 (JSVG decision), #85 (JUnit
decision), #94 (this finish). Like Sprint 9, a deliberately mundane
sprint, run on one PR by the owner's exception.

## Outcomes, decision by decision

| Dependency | Was | Now | Decision evidence |
|---|---|---|---|
| FlatLaf + Extras | 3.4.1 (2024-03) | **3.7.2** | changelog reviewed for chrome-facing changes; both themes launched and inspected - icons, toolbar, search field, chrome unchanged in character |
| JSVG | 1.7.2 | **2.1.0** | **not a free choice**: FlatLaf Extras 3.7.2 declares JSVG 2.1.0 as its runtime dependency, and the intermediate state (3.7.2 + JSVG 1.7.2) was tested and fails with `NoClassDefFoundError: com/github/weisj/jsvg/view/FloatSize`. The two moved in one commit |
| JUnit console | 1.10.2 | **6.1.3** | zero test-source changes; the same `ConsoleLauncher execute --scan-class-path` invocation finds and passes all 206 tests with no deprecation warnings on JDK 21 |
| PowerShell bootstrap | `download-libs.ps1` | **retired** | it never provided a complete native-Windows path (make and POSIX scripts remained required) and duplicated download/checksum logic. Supported environments stated plainly: macOS and Linux, Windows via WSL |

All version and checksum declarations live solely in
`scripts/lib-versions.env`, consumed by `scripts/download-libs.sh`
and the `Makefile`.

## The clean-checkout journey, re-run on the refreshed set

A fresh clone on JDK 21 (the recorded minimum, the only JDK on this
machine), following only the README: the guard names the four missing
jars and the one bootstrap command; `scripts/download-libs.sh`
fetches the refreshed set (SHA-256 verified, atomic); a second run
downloads nothing; `make test` passes **206/206**; `make chart-image`
is **byte-identical** to the committed M31 reference; `make app`
builds and **`java -jar build/app/JUranometria.jar` launches and
runs** with its adjacent `lib/`; the tree is clean afterwards. The
corrupt-recovery path was exercised against the new pins (a truncated
FlatLaf jar reported and repaired). The required GitHub `test` check
runs the same journey on a clean Linux runner and gates this PR.

## Visual verification

Both themes launched on FlatLaf 3.7.2 + JSVG 2.1.0 and inspected:
light and dark chrome as before, the Tabler toolbar icons rendered
through JSVG 2 with the colour filter recolouring them per theme, and
the chart page the atlas's own white paper and dark ink in both. The
FlatLaf changelog notes reviewed for this range: the JSVG migration
(3.2), retuned dark-theme text contrast (3.6), and macOS 14.4+
rounded-popup handling (3.5-3.7.2) - none changed what the atlas
shows.

## Residual risks, stated honestly

1. **FlatLaf 3.6 retuned dark-theme text contrast**; the app's chrome
   text is standard FlatLaf and looked right in review, but pixel-level
   chrome comparison is not part of the test suite (the chart's own
   ink is, and is byte-identical).
2. **JUnit 6 is a major line**: today it runs the suite identically;
   future JUnit-6-only behaviours (stricter discovery, changed
   defaults) would surface in CI first, which is the designed net.
3. **JDK 24+ native-access evidence** still rests on the issue #82
   measurements (this machine runs JDK 21); the manifest and flag are
   unchanged by this sprint.

## Sprint review answers

- **Is one authority intact after the refresh?** Yes -
  `lib-versions.env` was the only file whose versions changed; the
  scripts, Makefile filenames, CI cache key, and guard messages all
  followed automatically.
- **Did the safety net built in Sprint 9 actually help?** Decisively:
  the checksum bootstrap made the swaps atomic and verifiable, the
  byte-identical M31 reference proved the renderer untouched, the
  deterministic suite caught the FlatLaf/JSVG coupling immediately
  (four icon tests failed on the exact missing class), and the
  required check re-proved the whole journey on Linux.
- **Was anything upgraded blindly?** No - each move carries its
  evidence: a changelog review and theme inspection (FlatLaf), a
  tested incompatibility making the choice (JSVG), and an unchanged
  206-test run with no warnings (JUnit).
- **Does the simple organization survive?** Untouched: plain Make,
  one env file, one shell script - minus one script that implied
  support the project could not deliver.
- **What next?** Return to product work: **star names,
  Bayer-Flamsteed identifiers, and common-alias search** (the
  standing recommendation of the Sprint 8 review and handover), with
  wheel zoom about the pointer after. The remaining maintenance item,
  #88 (release artifact), still awaits the owner's decision on the
  downloadable artifact and its non-commercial data notice.

## Process expectations

The established pattern: this handover accompanies the open sprint
PR; the independent Codex review lands as
`docs/reviews/sprint-10-codex-review.md`; findings are fixed on the
PR; both documents are committed with the fixes; then merge, close
milestone 10, and cut 0.10.0.
