# Sprint 26 handover — The maintenance sprint that made its evidence executable

**Issues #241, #224, #243, #242, #245, #246, #244.** Written for the
final sprint review. Nothing here is merged, tagged or released on
the strength of it.

## What Sprint 26 delivered

A maintenance sprint with a gate at its head: measure the test and
CI evidence before touching it, so the later issues implement what
the measurements demand rather than what their descriptions assumed.
The core is invisible to a reader and load-bearing for everyone
else: every process-wide disturbance in the suite now rides one
shared guard, every journey presses what a reader presses, and every
generated artifact is held to an executable contract. Two
reader-facing repairs rode with it: the Mac application identity
verified from the public downloads, and the chart's first second
ground — **Black sky** — derived by measurement.

| # | what it was | PR |
|---|---|---|
| 241 | the gate: friction measured, the scanner built, five guards proven on broken fixtures | [#247](https://github.com/Aha43/JUranometria/pull/247) |
| 224 | shared-state consolidation onto `SwingSession`, suppression rule included | [#248](https://github.com/Aha43/JUranometria/pull/248) |
| 243 | display discipline: `ReaderInput`, premises first, one raw-key dispatcher, pinned | [#249](https://github.com/Aha43/JUranometria/pull/249) |
| 242 | executable evidence contracts: regenerate, compare, restore, fail loudly | [#250](https://github.com/Aha43/JUranometria/pull/250) |
| 245 | the Andromeda mark as the Mac identity, from the published 1.6.0 downloads | [#251](https://github.com/Aha43/JUranometria/pull/251) |
| 246 | Black sky: the second intentional chart palette | [#254](https://github.com/Aha43/JUranometria/pull/254) |
| 244 | this close: the evidence runs, the documentation, this handover | — |

## The required evidence, run and counted

**Complete suite from a clean checkout** (depth-1 clone into a fresh
directory, dependencies fetched by `scripts/download-libs.sh`,
Apple-silicon Mac, Darwin 25.5.0, Homebrew OpenJDK 21, display
present so the display corpus runs rather than aborts):
**132 containers found / 132 started / 132 successful / 0 failed /
0 aborted / 0 skipped; 783 tests found / 783 started / 783
successful / 0 failed / 0 aborted / 0 skipped**; 45.2 s cold. The
required xvfb display job runs the same corpus in CI and is green
on the merged head (98 s average over the sprint's last four runs).

**Evidence contracts in the environments the contract names.** From
the bare clean checkout, `make evidence-contracts` **fails loudly
by design**: `VERIFICATION INCOMPLETE` naming the star-identity
family's gitignored inputs and the two exact fetch commands — the
behaviour #242's review demanded instead of a silent skip. With the
pinned inputs supplied it runs green in 15.4 s: 102 reproduced, 77
reproduced via generator build output, 8 pinned residue, 12
inspection images regenerated identical, 17 unchanged, 3 session
photographs, 1 fixture and 2 files checksum-verified, 7
captured-evidence screenshots digest-verified, 1 widget-measured
report held to substance — `EVIDENCE CONTRACTS OK`.

**Test-order and shared-state protections** are exercised by the
suite itself: the gate proves each guard on deliberately broken
fixtures, `SwingSessionTest` runs the guard family including the
suppression rule and cross-process preference witnesses, and the
scanner's standing counts (39 touchers, 18 shared-guarded, 20
deliberate local fixtures, 0 unprotected; exactly one raw-key
dispatcher) are pinned in `TestEvidenceGateTest` and quoted in the
decision document, which the gate holds to the scanner's output.

**Native images and packaged acceptance.** The arm64 image builds
green locally (8.6 s warm) with the #245 icon chain — regenerated
from the reviewed geometry, byte-compared, and now
reference-checked in `Info.plist` — and `PACKAGED ACCEPTANCE OK`
including the new black-sky step (`mask agreement 0 px short of
exact on 630000, ground black, place untouched`). CI built all
four platform images green on the merged head (82–139 s per cell),
with the same acceptance inside each.

**Distribution verification** ran green on all three `verify`
platforms (43–46 s each, two runs sampled).

**Release rehearsal** in its named environment: a manual `release`
workflow run
([33888048526](https://github.com/Aha43/JUranometria/actions/runs/33888048526))
— build, jar, all four images, cross-architecture smoke, all three
portable verifies, stage, agreement and rehearsal jobs green,
**publish skipped** exactly as the rehearsal contract promises a
manual run cannot publish.

## Timings, before and after

Before = the #241 gate's measured table (recorded conditions:
GitHub-hosted runners, averages over stated samples; local numbers
one warm run). After = this close (CI averaged over the sprint's
last four `test` runs and two `app-image`/`dist` runs; local n=2
warm plus the clean cold run above).

| check | before | after | reading |
|---|---|---|---|
| CI `test` | 64 s avg, worst 78 (n=8) | 84 s avg, worst 98 (n=4) | the suite grew 738 → 783 tests, several of them full-page pixel accountings |
| CI `display` | 86 s avg, worst 98 (n=8) | 98 s avg, worst 117 (n=4) | one journey added (black sky) |
| CI `image` cells | 64–108 s | 82–139 s (n=2) | packaged acceptance gained the black-sky render-and-mask comparison |
| CI `dist` verify | ~43 s each | 43–46 s each (n=2) | flat |
| local `make test`, warm | 37.8 s / 738 tests (n=1) | 44.3–44.4 s / 783 (n=2) | ≈51 → ≈57 ms per test |
| local clean checkout, cold | — | 45.2 s | first measurement |

No causation is claimed from these samples; runner variance alone
spans much of each gap, and the suite is measurably bigger and
doing measurably more per test. The honest summary is the gate's
own finding restated: **elapsed time was never the friction and has
grown only with the work performed.** What the sprint actually
bought is structural and now mechanical — zero unprotected shared
state, premises before events, contracts that regenerate and
compare instead of trusting commits, and incomplete verification
that fails loudly with its remedy named.

## Reader-facing additions, demonstrated

**The Mac identity (#245)** was reproduced from the **published
1.6.0 downloads**, not a working-tree build: the digest-pinned
captures under `docs/studies/mac-identity/` show the Andromeda mark
on every `.app` surface (Finder pre-launch, Dock, switcher, Desktop
alias, `/Applications`) and Java's figure on the portable
`java -jar` route — recorded narrowly as a property of a bundle-less
launch, with the `.app` named plainly in the README as the branded
route. The unreferenced-ICNS packaging check was mutation-rehearsed
both ways and the candidate image's Dock tile committed as the
after-evidence.

**Black sky (#246)** is demonstrated three ways. Through its public
controls: `BlackSkyJourneyTest` drives the real View menu, the real
Chart tab, premise-proven clicks, Cancel, OK, Restore Defaults,
restart, and both theme directions, reading the component's own
painted pixels — green locally and in the CI display job. Inside
every native image: the packaged acceptance's black-sky step. And
live in the packaged application on this machine, by the keyboard
route end to end: the built arm64 image was launched, **View →
Chart Options** opened from the real menu bar, the Chart tab
reached with Ctrl+Page Down, **Black sky** toggled with Tab and
Space, OK confirmed the same way; the chart previewed live behind
the modeless dialog, the store showed `white-paper` until OK and
`black-sky` after it, and a full quit and relaunch of the packaged
application came back wearing the reader's black sky inside the
machine's dark chrome — each state read from the store and from
screenshots at the time. The machine's real preference node was
exported before the demonstration and restored byte-identically
after it.

**The ordinary chart is byte-identical.** `ChartOptions.DEFAULTS`
carries white paper; the entire existing suite — the released M31
reference comparison, every ink count, every study baseline — ran
green through the palette conversion with no re-baselining, and
the evidence contracts reproduce all renderer-drawn artifacts
byte-for-byte on this machine.

## Documentation

`docs/development.md` now carries the final evidence taxonomy (six
classes and their contracts) and the display-test discipline as a
"Tests and evidence" section beside the definition of done;
`docs/decisions/test-evidence.md` remains the deciding document and
the gate holds its quoted counts to the scanner.
`docs/application-appearance.md`, `docs/chart-conventions.md` and
the README record the black-sky choice where their standing claims
needed refining.

## Every review correction, by round

**#241 (PR #247, four rounds).** State inventory scanned only
`test/` → the evidence executables under `src` are scanned too. An
unrelated `finally`/`AfterEach` vouched for any touch → protection
is paired per state (capture, restoring write, place to run).
Study PNGs all classed as non-deterministic and the SOFA oracle as
artwork → the class taxonomy, with renderer byte contracts kept.
The scanner counted its own marker definitions → instruments
excluded by pinned name. The real-preferences guard caught only the
bare node literal → production doors; a remembered door list missed
`AppShutdown.real()` → the door set is **derived** from the
production sources and pinned. The `.c` generator unreachable by
the docs walk → both fixture halves inventoried. `test-evidence-study`
not phony → fixed.

**#224 (PR #248, three rounds).** The exit probe removed its node
then flushed the removed node, with no parent-process assertion →
parent captured before removal, `parent.flush()`, cross-process
witness. Failure wrappers replaced the primary failure → the
project's suppression rule in `guarded`. `scratchPreferences`
tested only same-JVM visibility → cross-process proof. Exempting
every `userRoot()` source without `.node(` also exempted root
writes → the read-only witness is pinned by exact name.

**#243 (PR #249, three rounds).** Keyboard journeys without a
proven focus owner, maskable by per-file premise counts → focus
insisted at the gesture. `chooseTab` could press a clipped header →
point-in-visible-rect premise. Window focus was not target focus
for tab traversal → `shortcutOn`; the raw-dispatcher set pinned to
exactly `ReaderInput`. Public premise-free `press()` was a bypass →
private, all callers through proven routes.

**#242 (PR #250, five rounds).** Clean-checkout dependence on
gitignored inputs → input gates with exact fetch commands, and
incomplete verification fails the run. No outer restoration; files
created during generation escaped inventory → the outer
`finally`-shaped path and newcomer breaches. "Visually inspected"
claimed by an eyeless command → honest verdict vocabulary. Ninety
renderer images weakened to unchecked baselines → compared against
their generators' build output, per-directory after a name
collision mis-paired two studies. The residue list unenforced →
the eight-file bidirectional pin. Generation-and-restoration double
failure lost the primary → suppression. Manual rehearsals →
committed regression tests, including thrown-generator-through-the-
real-finally, cleanup-only failure, all pin branches, and the real
gates pinned with their fetch strings.

**#245 (PR #251, one round).** Documentation overstated Duke as
macOS's treatment of "any bare JAR" while recording a runtime API
that could brand it → the claim narrowed to the measurement; a
route-matrix cross-reference named by number → named by route.

**#246 (PR #254, one round).** Module reference ink and working
crosses were never exercised through the real `ChartComponent`
wiring — a call site pinned to white paper would have survived,
with crosses invisible on black → `BlackSkyModuleInkTest` renders
the real component with the real registry and the real
`MeridianModule` and reasons over the diff pixels; both call-site
mutants rehearsed independently, each killed by exactly its own
test.

## Found beyond the filed work

- Sprint 1's smoke test had leaked FlatLaf into every later test
  for years — found the day the corrected gate first ran.
- Eighty-eight study pages (73 + 15) had quietly fallen behind the
  atlas — pre-grid, pre-Bayer renderings whose studies' own rule is
  that chosen pages are production output — re-baselined.
- The committed light-theme dialog photograph carried the 344 px
  stale-class stripe from #228's window; the dialog study
  photographed off the EDT and its widths were order-dependent —
  it photographs inside `invokeAndWait` now, photographs
  regenerated whole.
- The icns ships without plain 16/32 (or 128@2x/256@2x) variants;
  every inspected surface renders correctly — recorded as an
  observation on `ApplicationIconMain`, not changed.
- The black-sky study's first pixel accounting caught honest
  antialiasing a naive bulk floor misread (531 pixels of grey 1
  along M31's ellipse rim), and the centre-of-page ground probe
  was wrong twice for the same reason — the default page centres
  on M31's own pale wash — before both probes learned to ask for
  the commonest pixel.
- The verifier caught its own author once: a dropped hex character
  in a fixture pin.

## Residual risks, stated

- **The portable JAR wears Java's Dock icon** — an inherent
  property of a bundle-less launch, recorded in the decision and
  the README. `java.awt.Taskbar.setIconImage` could brand the
  running process's tile; that follow-up is recorded but not filed.
- **Two flush-drop mutants are macOS-equivalent**: this platform
  persists a preference-node removal without an explicit flush, so
  those mutants survive here by platform behaviour; the
  cross-process witnesses are the load-bearing proofs, recorded in
  the #224 review trail.
- **The deep-sky-vocabulary report** remains the one byte-claim
  exception (widget-measured geometry; held to existence and
  substance) — pinned as `WIDGET_MEASURED_REPORT`.
- **Captured evidence pins bytes, not truth**: a digest proves a
  screenshot has not changed since review, not what it shows; the
  class depends on capture-time review, which is where it happened.
- **One transient mixed frame** was observed while photographing
  the live preview in the packaged demonstration: a screen capture
  taken immediately after the toggle showed the page already black
  with the furniture still white-paper for that frame; the next
  capture and every offscreen render are coherent, and the
  renderer draws each frame from one palette in one pass, so this
  reads as window-composite timing during the capture, not product
  logic. Recorded because it was seen, not because it reproduced.
- **The Milky Way remains parked unlicensed**; its dark-ground
  treatment is explicitly deferred and `ChartPalette` carries no
  colour for it by decision.

## Version recommendation

**1.7.0.** Black sky is a new reader-facing capability (a persisted
chart choice with its own palette), the Mac identity work
strengthened packaging checks and documentation, and nothing
removed or broke compatibility: a minor version by the project's
semantic-versioning contract. The stores upgrade silently — a
1.6.0 reader keeps white paper and every choice they had. No
`VERSION` change, milestone close, tag or release before the final
review and the owner's explicit instruction.
