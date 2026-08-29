# Sprint 9 handover — Make a clean checkout dependable

Written 2026-08-30 by the coder for the independent pre-release review
of 0.9.0, following the established handover pattern. The sprint's
issues, in the executed order: #87 (misleading `.gitignore` rules),
#80 (POSIX bootstrap and readable failures), #81 (required
pull-request test workflow), #82 (deliberate native access on modern
JDKs), #86 (truthful README and development documentation), #89 (this
finish). An intentionally mundane sprint, driven by the problems a
second, fresh Mac actually exposed.

## What Sprint 9 delivered

- **One authoritative dependency declaration**:
  `scripts/lib-versions.env` is sourced by the new POSIX
  `scripts/download-libs.sh`, parsed by `scripts/download-libs.ps1`,
  and `include`d by the `Makefile` — three consumers, zero
  opportunity to drift. The scripts fetch the same four pinned jars
  to the same locations and skip files that exist.
- **Readable failure instead of compiler noise**: `classes` and
  `test` depend on `check-libs`, which stops with a one-line
  instruction naming both download scripts when jars are missing —
  replacing the eight `package ... does not exist` errors the fresh
  Mac produced.
- **The merge rule is now enforced by a machine**: the `test` GitHub
  Actions workflow runs `scripts/download-libs.sh` + `make test` on
  JDK 21 for every pull request to `main` and every push to `main`,
  with the jars cached by the hash of the version file.
  `docs/development.md` names the check beside its merge rule — and
  after the review (which found the private-plan gap) the owner made
  the repository public and branch protection now **requires** the
  `test` check on `main`, administrators included: the rule is
  technical, not cultural.
- **Native access is deliberate, not a warning**: the `run` target
  passes `--enable-native-access=ALL-UNNAMED` and the packaged jar
  carries `Enable-Native-Access: ALL-UNNAMED` in its manifest
  (honoured by `java -jar`, including under
  `--illegal-native-access=deny`). The reason — FlatLaf's native
  window integration meeting JEP 472 — is written in
  `docs/development.md` under "Native access".
- **The repository describes itself truthfully**: the README status
  reflects eight releases of working instrument; the sprint-record
  convention (Sprint 1's Markdown document, then milestones plus the
  `docs/reviews/` handover/review pairs) is stated once and
  `docs/development.md` agrees; the dead Maven/Gradle `.gitignore`
  section — including a negation whose trailing comment git read as
  part of the pattern — is gone, with every needed ignore verified
  still held by the kept rules.

## The clean-checkout journey, executed literally

On this machine, a genuinely fresh clone of the sprint branch (no
`lib/`), following only the README:

```
git clone <repo> && cd <repo>
make test               -> "Missing dependency: lib/flatlaf-3.4.1.jar"
                           (+3 more) and the one-line instruction
scripts/download-libs.sh -> downloads the four pinned jars
scripts/download-libs.sh -> "Already exists" x4, downloads nothing
make test               -> 206/206 tests pass
make chart-image        -> byte-identical to docs/reference/m31-stars.png
git status              -> clean
```

The PowerShell path was executed against the same shared version file
into a separate directory: **all four jars byte-identical** to the
shell script's set.

After the review, the bootstrap gained **pinned SHA-256 verification**
(the hashes live beside the versions in `lib-versions.env`): both
scripts verify existing files (a corrupt or zero-byte jar is reported
and re-downloaded), download to a temporary name, verify, and only
then move into place - exercised with deliberately corrupted and
truncated jars on both the shell and PowerShell paths, and with a
deliberately wrong pin (rejected with an actionable message, exit 1,
temporary file removed). The packaged jar also gained its manifest
`Class-Path`, and `java -jar build/app/JUranometria.jar` was launched
and observed running - the native-access attribute is now attached to
a launch mode that works.

## CI verification

The workflow's first run — on a clean `ubuntu-latest` runner, no
PowerShell, cold cache — bootstrapped with the shell script and passed
the complete suite: **the entire test suite, Swing component tests and
rendering assertions included, is green headless on Linux**, which
also quietly proves the tests never depended on macOS fonts or a
display. The run surfaced one deprecation (setup-java v4), fixed to
v5 in the same sprint. The check runs on this very PR.

## JDK evidence

- **JDK 21 (this machine, the recorded minimum)**: the
  `--enable-native-access` flag is accepted, `make run` launches
  cleanly, and the manifest attribute is present in the built jar
  (inspected).
- **JDK 24+**: this machine has only JDK 21, so the modern-JDK
  behaviour rests on issue #82's own measurements (JDK 26.0.1,
  FlatLaf 3.4.1): the flag silences the four-line warning; the
  manifest attribute keeps `java -jar` silent and fully integrated
  even under `--illegal-native-access=deny`. The implemented changes
  are exactly the measured remedies. An independent check on the
  reviewer's newer-JDK machine would complete the evidence.

## Worth extra scrutiny

1. **The JDK 24+ acceptance is second-hand on this machine** (above)
   — the one criterion not re-verified locally.
2. **CI runs the suite headless on Linux**; if a future test compares
   text-bearing pixels against committed bytes (only the M31
   reference does today, and it passed), platform fonts could bite.
   Nothing currently depends on them.
3. **`include scripts/lib-versions.env` in the Makefile** means a
   syntax error in that file breaks make with a make-flavoured
   message; the file is three assignments and a comment, and both
   scripts would fail loudly on the same edit.

## Sprint review answers

- **Clone to green tests using only the README?** Yes — executed
  literally on a fresh clone: the guard message, the one bootstrap
  command, `make test` green, reference byte-identical, tree clean.
- **Can the two bootstrap paths drift?** No: one version file, three
  consumers, and the sprint verified the two scripts produce
  byte-identical jar sets from it. Drift would now require editing a
  single file inconsistently with itself.
- **Does local testing correspond to the required check?** They are
  the same command (`make test`) after the same bootstrap
  (`scripts/download-libs.sh`) on the same JDK line (21); a local
  green is a CI green unless the platform itself differs, and the
  first Linux run proved the suite platform-clean.
- **Is the native-access permission narrow and explained?** It is
  scoped to what the launch actually needs (`ALL-UNNAMED` — the
  application is deliberately unmodularised), applied in exactly the
  two launch paths, and documented with its JEP and measurements so
  it cannot be mistaken for noise. JDK 21 verified here; JDK 26
  evidence from the issue.
- **Did the maintenance preserve the simple organization?** Yes:
  plain Make and plain shell, one workflow file of six steps, no
  dependency manager, no new tooling; the diff is dominated by
  documentation and one small script.
- **Are README, sprint record, and instructions truthful now?** Yes —
  status matches the changelog, every link resolves, the convention
  is stated once and followed everywhere it is referenced.
- **Which deferred group next?** The dependency refresh (#83–#85)
  first: it now has exactly the safety net it was deferred to wait
  for (one pin file to edit, CI to catch regressions, byte-level
  bootstrap parity to verify). The release-delivery work (#88) should
  still wait for its artifact/notice decision. Neither blocks
  returning to product work (star names) if that is preferred — the
  refresh is a small sprint whenever it is wanted.

## Process expectations

The established pattern: this handover accompanies the open sprint PR
(#91, carrying the whole sprint by the owner's explicit exception for
this chore sprint); the independent Codex review lands as
`docs/reviews/sprint-09-codex-review.md`; findings are fixed on the
PR with regression tests; both documents are committed with the
fixes; then merge, close milestone 9, and cut 0.9.0.
