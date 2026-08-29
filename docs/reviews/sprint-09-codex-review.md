# Sprint 9 Codex review — Make a clean checkout dependable

Reviewed 2026-08-30 against PR #91 at `cf60b1d`, covering issues
#87, #80, #81, #82, #86, and #89 from the 0.8.0 release.

## Result

Sprint 9 is well chosen and mostly successful. The second-Mac friction has
been converted into a short, understandable contributor path: one shared
pin file, a native shell bootstrap, an actionable missing-library guard,
automatic Linux/JDK 21 testing, and documentation that describes the
working atlas rather than its original prototype stage. The deliberately
plain Java and Make organization survives intact.

The clean happy path and complete application suite pass. Review found two
P1 contract mismatches and one P2 bootstrap-hardening issue before 0.9.0.
They are maintenance defects, not product or cartographic regressions.

## Findings

### P1 — The documented `java -jar` launch cannot load its dependencies

Issue #82 and `docs/development.md` say the packaged JAR's
`Enable-Native-Access: ALL-UNNAMED` attribute supports a plain `java -jar`
launch, including under `--illegal-native-access=deny`. The built manifest
does contain that attribute, but it contains no `Class-Path`. The FlatLaf,
FlatLaf Extras, and JSVG JARs are copied to `build/app/lib/`, where `java
-jar` does not discover them automatically.

The reviewed command

```
JAVA_TOOL_OPTIONS=-Djava.awt.headless=true java -jar build/app/JUranometria.jar
```

fails on the EDT with `NoClassDefFoundError:
com/formdev/flatlaf/FlatLightLaf`. The manifest permission is therefore
attached to a launch mode that cannot start the packaged application.

Add the three adjacent runtime JARs to the manifest `Class-Path` (using the
shared version variables), or provide a packaged launcher that applies the
native-access flag and class path together. Verify the actual packaged
launch rather than only inspecting the manifest. Keep `make run` as the
separate `-cp` path.

### P1 — The workflow is automatic, but it is not an enforced required check

The workflow runs and is green, which is valuable. The PR, changelog,
handover, workflow comment, and development guide go further: they call it
a “required check” and say the merge rule is “enforced by a machine.” GitHub
reports that branch protection and repository rulesets are unavailable for
this private repository on its current plan (HTTP 403: make the repository
public or upgrade to GitHub Pro). There is consequently no rule preventing
a merge when `test` is absent, pending, or failed.

Do not represent a cultural merge rule as technical enforcement. Either
enable a plan/repository setting that can require the `test` status, or
change the repository wording consistently to “automatic PR check” and say
that the owner still enforces the green-before-merge rule manually. The
latter keeps Sprint 9 useful without turning an account-plan choice into a
code requirement.

### P2 — Do not let an interrupted download become an idempotently cached bad JAR

Both bootstrap scripts treat any existing target path as complete. The
POSIX script downloads directly to the final filename with `curl -o`; an
interrupted transfer can leave a partial file, after which every rerun says
“Already exists.” A zero-byte or otherwise corrupt JAR also passes
`check-libs`, whose predicate is only `-f`, and the contributor falls back
to opaque ZIP/compiler errors. The same conceptual risk exists in the
PowerShell path and in a restored dependency cache.

Download to a temporary file and move it into place only after success.
Also validate existing/downloaded files, preferably against SHA-256 values
kept with the authoritative pins (or at least as readable JAR archives), so
idempotence means “the correct pinned artifact is present.” Exercise a
pre-existing corrupt/partial file and show that bootstrap repairs or rejects
it with an actionable message.

## What was verified

- The GitHub `test` workflow is green on PR #91 on JDK 21/Linux.
- `make test` in headless mode: 206/206 tests pass locally.
- `make chart-image`: the released M31 reference remains byte-identical.
- `make app` builds the expected application directory and the native-access
  manifest attribute is present.
- Direct `java -jar` launch fails with the missing FlatLaf class described
  above.
- GitHub's branch-protection and ruleset APIs both report the current private
  repository/plan cannot enable those enforcement features.
- `git diff --check` was clean before adding this review document.

## Maintenance assessment

The strongest part of the sprint is the shared version declaration. It
keeps the shell script, PowerShell script, Makefile filenames, and CI cache
key aligned without introducing Maven or Gradle. The first clean Ubuntu run
passing Swing and rendering tests is also useful new evidence: the suite is
not accidentally tied to the development Mac.

The documentation correction is appropriately restrained. It updates the
status and sprint record without rewriting the product vision, and the
`.gitignore` cleanup removes misleading policy rather than reorganizing a
working file for style.

## Recommendation after fixes

Resolve the packaged-launch and enforcement wording mismatches, harden the
bootstrap against poisoned partial files, then merge PR #91, close milestone
9, and cut 0.9.0. The dependency refresh in #83–#85 is now a sensible small
follow-up maintenance sprint. It can also wait until after a product sprint;
the new automatic check and shared pins already provide the intended safety
net.

## Follow-up — 2026-08-30

Reviewed the fixes at `99b223f`. All three findings are resolved:

- The application manifest now carries the version-derived runtime
  `Class-Path`. A packaged launch resolves FlatLaf from the adjacent `lib/`
  directory; the headless verification reached Swing window creation without
  a missing-class failure, and the normal GUI launch was also exercised.
- The repository is public and `main` branch protection requires the `test`
  check, including for administrators. Force pushes and branch deletion remain
  disabled.
- Both bootstrap scripts now verify pinned SHA-256 hashes, replace corrupt or
  partial files, download through temporary files, and refuse an incorrect
  pin. The shared variable parser also accepts digits in checksum variable
  names.

Follow-up verification: required GitHub checks green, 206/206 tests pass, the
M31 reference image is byte-identical, and the diff passes whitespace checks.

**Final recommendation: approve and merge.**
