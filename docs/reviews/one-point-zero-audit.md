# The 1.0 audit — the application against its contract

**Sprint 18, issue #145.** Audited: the released **0.17.0** tree and
its five distributed artifacts, against
[the 1.0 contract](../decisions/one-point-zero-contract.md) including
its #150 amendment. This is the record #146 cites instead of
repeating the audit from memory.

The rule the audit held itself to: **fix only a demonstrated
contradiction of the contract**, record everything else as post-1.0.
Six contradictions were found and fixed; one robustness defect was
found by experiment and fixed; the rest of the contract was
confirmed by evidence, and that evidence is now executed by tests
rather than asserted in prose. The suite went from 324 to 334.

## Blockers found and fixed

### 1. The contract had outgrown its own numbers

It said "the seven `chart.*` option keys" and "the seven toggles".
The application ships **nine** — the count was written before the
coordinate grid (Sprint 15) and the star-identifier split (Sprint
17). Its preferences section also spoke only of 0.15, and never
stated the identifier migration precedence that Sprint 17 decided.
Corrected in place, marked as #145 corrections, with the precedence
written out exactly and the default-view sentence naming all three
identifier layers.

### 2. README claimed the reader needs Java

"Running the released application … a Java runtime of version 21 or
later, and nothing else" directly contradicts the amendment that
made the four platform images self-contained. The download section
likewise described only the portable ZIP (`./juranometria`,
`java -jar`), so a reader following the README would never learn
that an application image exists. Replaced with the five-artifact
table, the real launchers, `SHA256SUMS.txt`, and the unsigned
Gatekeeper/SmartScreen reality the contract requires be stated
plainly.

### 3. README understated the atlas

"Non-galaxy deep-sky types are searchable and recentre the chart but
await their chart symbols" has been false since the symbol language
landed: open clusters, globular clusters, nebulae of every kind, and
planetary nebulae all draw. Only stellar entries, associations, and
novae stay undrawn, deliberately. Corrected to what
`ChartRenderer.symbolFor` actually does.

### 4. README's status was eight releases stale

"Nine releases in (v0.1.0 through v0.9.0)" on the eve of 1.0.
Rewritten for the 0.17.0 reality: eighteen releases, four
self-contained applications plus the portable archive, notation,
boundaries, and grid.

### 5. The licensing map never named the bundled Java runtime

The contract requires that "the distribution licensing map names
it", and the image `README.txt` does say Temurin is GPLv2 with the
Classpath Exception — but `LICENSING.md`, the document README calls
"the complete picture", did not mention a runtime at all, though
four of the five artifacts ship one. Added as its own section,
including why the Classpath Exception is what keeps the MIT code
MIT, and that the portable archive contains no runtime. The About
surface is deliberately left alone: the contract's division of
licensing surfaces puts runtime licences with the runtime, not in
About.

### 6. `architecture.md` still deferred the Java baseline

It listed "the minimum supported Java release" among decisions
deliberately deferred, which the contract settled as Java 21. Struck
through with a pointer to the contract; the genuinely open items
(export libraries, network client, wide-field projection) are left
as they are, because they are still open.

### 7. A damaged download failed invisibly — the one defect

Not a documentation matter, and the most serious finding.
**Demonstrated**, not theorised: with one byte appended to a bundled
star tile, 0.17.0 threw `ExceptionInInitializerError` on the event
dispatch thread, printed a stack trace to a console a packaged
reader does not have, and **left the process running with no window
at all** — on macOS, an icon in the dock that never opens anything,
with nothing to indicate what was wrong.

Fixed with `StartupFailure`: launch is wrapped, the failure is
reported in the loader's own words with the remedy, and the process
exits non-zero instead of lingering invisibly. What a reader with a
damaged download now sees:

```
JUranometria could not start.

catalogue tile tiles/r10-d1/stars.csv does not match its manifest
checksum
  expected 9e503ae25a479194fd40747dbbf62dca00338d92eae623dbcbb4ee42f74aa294
  actual   a22c6ed8849d46579e047affde87508fe8578c54f0761141e36bfd3526d049b1

The application verifies its bundled star catalogue, constellation
geography, and star identities against their own checksummed
manifests as it loads, so this usually means the downloaded files
are damaged or incomplete.

Download the release again, check it against the published SHA-256
checksum, and unpack it fresh into an empty folder.
```

The same surface covers the one preference failure the store cannot
absorb — a preferences node that has been removed underneath a
running application — which is now a sentence rather than a silent
death.

## Confirmed by evidence, and now executed by tests

| Contract promise | How it was checked | Result |
|---|---|---|
| Preferences from older releases load unchanged | `PreferenceUpgradeTest` builds the **complete** key set each of 0.13/0.14, 0.15, and 0.17 actually wrote | every choice survives; absent keys take their documented defaults |
| No stored value can fail a launch | every key damaged a different real way (`FALSE`, `0`, empty, whitespace, `null`, `yes`, trailing space, foreign value) | all nine load as released defaults |
| Every control carries an accessible name | `AccessibleSurfaceTest` walks the toolbar, search, all three dialogs, and the menu bar | no gaps; the only unnamed components anywhere are FlatLaf's internal scroll-bar buttons, which are the look and feel's, skipped by focus traversal |
| Dialogs are owned and close on Escape | asserted for all three dialogs | held (Settings reaches it through the shared helper) |
| "Makes no network requests at any time" | `OfflinePromiseTest` scans every compiled class for the constant-pool references a connection requires | zero, across 300+ classes — offline by construction, not by policy |
| Both themes, chart theme-independent | existing appearance tests and the packaged acceptance in light and `--dark` | held |
| The five artifact shapes | the four native cells, `smoke-cross-architecture`, and three portable `verify` cells, all green on the release commit | held |
| The packaged application is the published one | the **actual downloaded** 0.17.0 macOS arm64 archive, run with `PATH=/nonexistent` | About reports 0.17.0 with 26,048 characters of notices, a real preference change-and-reload succeeds, and its smoke render is **byte-identical** to the committed reference |
| About, `LICENSING.md`, `packaging/LICENSING.md` agree | the existing pairing test, still green after the runtime section was added | held |
| Version agreement | About shows the packaged `VERSION`, never a second copy | held |

Robustness checks that found nothing, recorded so they are not
repeated blindly: the window has **no minimum size**, so eleven
degenerate geometries (down to 1×1, plus 300×1 and 1×300) were
rendered and panned — none throws, none corrupts the view. The
absence of a minimum size is therefore not a defect, and setting one
is a taste question, not a 1.0 blocker.

## Open-issue disposition

Only three issues are open, all of them this sprint's own:

| Issue | Disposition |
|---|---|
| #145 | this audit |
| #88 | 1.0 blocker, next; it also owns replacing the release-automation deferral still standing in `docs/development.md` |
| #146 | 1.0 blocker, last; cites this record |

No blocker was discovered that needed an issue of its own — each was
narrow enough to fix and prove here.

## Post-1.0, recorded rather than done

- **The released JAR carries the developer study and import tools**
  (41 `juranometria/tool/…` entries). Harmless — none of them
  touches the network, and they are inert without the gitignored
  raw imports — but they are not part of what a reader runs.
- **Single-candidate label placement**: a rejected label could try
  the alternate side before giving up. Recorded across several
  sprints; still a refinement, not a defect.
- **Installers, signing, notarization, app stores, update
  services** — deferred by the contract itself, unchanged.
- **A minimum window size**, if a future review decides the
  1×1-pixel atlas is untidy rather than merely harmless.

These belong to the dogfooding sprint that follows 1.0, judged on
use rather than on prediction.

## Residual risks

- **The audit is only as good as the contract.** Six of the seven
  findings were the documents drifting behind the product, which is
  the failure mode this sprint exists to catch; a promise nobody
  wrote down cannot be audited, and the contract remains the only
  place 1.0 is defined.
- **The invisible-launch defect was found by experiment, not by
  reading.** Its class — a failure whose only symptom is that
  nothing happens — is exactly what tests do not notice. The new
  surface covers the launch path; a failure *after* the window
  appears still reaches only the console.
- **Accessibility is verified structurally, not with a screen
  reader.** Every control names itself and every dialog is operable
  and dismissible by keyboard; how VoiceOver or Narrator actually
  announces the chart canvas is unmeasured, and honest to say so.
- **Intel Mac evidence remains a recorded run**, per the contract,
  not a machine CI can re-run on demand beyond the
  `macos-15-intel` cell.
