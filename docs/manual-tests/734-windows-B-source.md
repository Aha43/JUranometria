# Hands-on test — #734 subject B: clean source clone on Windows

**Status:** DRAFT  
**Tester:**  
**Date:**  
**Issue:** [#734 — Establish a clean Windows installation and rendering baseline](https://github.com/Aha43/JUranometria/issues/734)  
**Subject:** a fresh clone of `Aha43/JUranometria` at tag `v7.0.0` (`122c3187`)  
**Documented route:** `README.md`, *Requirements* and *Build and run (contributors)*: JDK 21+, GNU Make and `curl`; "on Windows, use WSL" — a native Windows development workflow is not promised.  
**Platform:** as recorded in [subject A](734-windows-A-reader.md), section 1  

This subject is kept separate from the reader installation. Follow only what the README says; when it is not enough, stop following and write down exactly what was missing and what you did instead. Every workaround is a finding, not a failure of the test. The earlier demonstration with an IDE's bundled JDK is context, not the route to assume.

## 1. Build environment

| | |
|---|---|
| WSL version and distribution (`wsl --version`, `wsl -l -v`), or "native" | |
| Git (`git --version`) | |
| Java (`java -version`) and where it came from | |
| Make (`make --version`) | |
| `curl --version` (first line) | |
| IDE-provided runtime used, if any | |

## 2. Clone and bootstrap

- [ ] `git clone https://github.com/Aha43/JUranometria.git` into a fresh folder, then `git checkout v7.0.0`.
- [ ] `scripts/download-libs.sh` completes and fills `lib/`.
- [ ] Nothing beyond the README's three requirements had to be installed. (If something did, list each item below.)

**Missing prerequisites and workarounds, in order met:**


## 3. Build and test

- [ ] `make test` compiles and runs. Record the tally lines (found, successful, failed, aborted) exactly as printed; run it once and do not rerun to improve it.
- [ ] `make run` builds and opens the application window.

**Tally and observation:**


## 4. The built application

- [ ] The window opens with a chart and the Controller.
- [ ] One quick look at each: Place and Time, Export Sheet, Whole sky. Note only differences from the published application of subject A.

**Observation:**


## 5. Native Windows, if attempted (optional)

Only if you choose to try it; the README does not promise it.

- [ ] Attempted from (Git Bash / PowerShell / other):
- [ ] How far it got, and the first thing that stopped it:

**Observation:**


## Not tested in this record

List here anything above that was skipped, and why.

## Verdict

Replace `DRAFT` below and at the top with exactly one of `PASS`, `FAIL` or `INCOMPLETE`.

**DRAFT**

**Overall observation:**

