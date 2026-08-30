# Sprint 10 Codex review — Refresh the foundations

Reviewed 2026-08-30 at `0a46474`, before the proposed 0.10.0 release.

## Findings

### P2 — Finish retiring the second-bootstrap claims

The PowerShell implementation is gone, but two current build comments still
describe a pair of scripts. `Makefile` says the pin set is “shared with both
download scripts,” and `scripts/lib-versions.env` says “both bootstrap scripts
verify” the hashes. These comments now contradict Sprint 10's central
maintenance result and leave the next contributor wondering whether a second
supported path was accidentally deleted or merely moved.

Make both comments singular and describe the real division of responsibility:
the shell bootstrap verifies the hashes, while the Makefile consumes the
versions for filenames and the packaged manifest. Historical handovers and the
0.9.0 changelog entry should remain untouched because they accurately record
the state of those releases.

### P2 — Bring the README status through the released 0.9.0

The README still opens its status section with “Eight releases in (v0.1.0
through v0.8.0),” although 0.9.0 is already recorded and released and this PR
is preparing 0.10.0. That makes the public landing page look one release behind
immediately after the sprint whose purpose was repository upkeep.

Update it to the current released state before 0.10.0, or remove the brittle
count/range and describe the working instrument without a manually advancing
release total. The latter is preferable because the changelog already owns the
exact release history.

## What was verified

- GitHub's required `test` check is green on JDK 21/Linux.
- Every downloaded JAR matches the SHA-256 in
  `scripts/lib-versions.env`.
- Headless local test run on JDK 21: 206/206 tests across 37 containers pass.
- JUnit 6.1.3 uses the existing console invocation and discovers the unchanged
  suite.
- `make chart-image` reproduces `docs/reference/m31-stars.png`
  byte-for-byte.
- `make app` packages FlatLaf 3.7.2, FlatLaf Extras 3.7.2, and JSVG 2.1.0;
  the generated manifest names those exact adjacent libraries.
- A headless packaged launch reaches Swing window creation with no
  missing-class or linkage failure.
- The runtime dependency JARs retain their embedded licence files.
- No PowerShell file or current invocation of `download-libs.ps1` remains.
- `git diff --check` is clean.

## Assessment

The strongest result is the FlatLaf/JSVG decision. Testing the intermediate
combination proved that JSVG 2 was required by the chosen FlatLaf Extras
release rather than treating a major-version bump as routine housekeeping.
The implementation then kept that coupled change atomic. JUnit 6 is similarly
well bounded: test-only, compatible with the Java 21 floor, and verified with
the same source set and discovery command.

Removing the isolated PowerShell downloader also improves the old-style Java
organization. The repository now states one honest contributor path instead
of maintaining one fragment of an otherwise unsupported native-Windows
workflow.

## Recommendation

Fix the two small documentation inconsistencies, then merge PR #95, close
milestone 10, and cut 0.10.0. No dependency or application-code changes are
requested.

## Follow-up — findings resolved

Reviewed the fixes at `47e3883`. The Makefile and pin-file comments now
describe the single bootstrap script, and the README status advances through
the released 0.9.0. The required `test` check is green, GitGuardian is green,
and the complete diff remains free of whitespace errors.

**Final recommendation: approve and merge.**
