# The interaction CI route

Sprint 38, issues #427 (measured and ruled) and #428 (implemented).
Extends [the rendering-neutral gate](rendering-neutral-gate.md) of #398
from two routes to three.

## Why

The road to 5.0 is interface work, and every interface change was
wide. Measured on `main` at `99f6077`:

| job | narrow | wide |
|---|---:|---:|
| `test` (unit suite) | 11 min | 7–11 min |
| `display` (whole suite on a virtual display, 0 failed / aborted / skipped) | 17 min | 13–17 min |
| `evidence` (every generator reproduced) | — | **36–45 min** |
| `app-image` (4 native images, packaged acceptance in each) | — | 3–5 min each, in parallel |
| `dist` (portable archive, 3 OSes) | — | about 2 min |
| **wall clock** | **about 17 min** | **44–46 min** |

One chart study, `LabelStudyMain`, took 2 209 s of the evidence job;
the interface-only generators take seconds. The planned zoom lock
(#428) was wide on 9 of its 12 paths, and none of them could reach
chart ink: widget photographs and UI reports under `docs/studies/`,
interface words under `src/resources/`, and a toolbar reached only by
its own photographer.

## The ruling (#427)

- **I1.** `JUranometriaMain` stays wide: it composes modules into the
  chart, and a mistake there can change chart content in ways a finite
  packaged journey may miss.
- **I2.** Language files are judged key by key, failing closed: a key
  is interaction only when its consumers are mechanically confined to
  the interface. Shared, dynamically resolved or unknown keys are
  wide; no list of safe keys.
- **I3.** `dist` runs on the interaction route.
- **I4.** The route is called `interaction`.
- Both closures are derived from the registered generators and the
  compiled code, never from a path list; a path in both is wide;
  anything unresolved is wide; the interaction route never regenerates
  the chart corpus; interface generators come from their registry;
  `main`, tags, releases and manual qualification stay wide; feature
  code is not moved to obtain a cheaper route.

## The boundary

**Generators.** `EvidenceGenerators` reads the evidence contract's
registries (reports, platform records, image generators, build
writers and the directories they promote into), the
`docs/studies/...` paths each generator's own class names, and what
is committed there. A generator whose directories hold any committed
renderer-drawn picture - outside `docs/studies/interface-language/`,
whose photographs are interface evidence by registry - is a **chart
producer**; one whose outputs cannot be resolved is a chart producer
too. Every other evidence generator owns only reports, platform
records, fixtures or inspection imagery and is **reproduced** on every
interaction run. At `99f6077`: 33 chart producers, 22 reproduced, 14
photographers (`InterfacePhotographers`, the registry the interface
gate reads).

**Closures.** `ChangeBoundary` reaches through one class graph
(`RenderingClosure`, from the constant pools) twice:

- the **chart** closure, from every chart producer, the smoke render
  (`ChartImageMain`) and the two resource-index writers;
- the **interaction** closure, from the photographers, the reproduced
  generators and the packaged application's two entry points.

**Paths.**

| path | route |
|---|---|
| `docs/reference/`, `docs/gallery/`, `packaging/`, `scripts/`, workflows, `Makefile`, `VERSION`, `LICENSING.md`, resources other than interface words, `JUranometriaMain`, the guard and the evidence contract and provenance recorder | **wide**, by name |
| `PackagedAcceptanceMain` | **interaction**: the route runs it on every platform |
| a source the chart closure reaches (whether or not the interface does) | **wide** |
| a source only the interaction closure reaches | **interaction** |
| a source nothing reaches; tests; prose | **narrow** |
| a removed source | **wide** |
| `docs/studies/...`, a committed input (fixture, ledger) | by which closure's code names its path or its directory: chart → **wide**, interface only → **interaction**, only the suite → **interaction**; named by nothing → by owner |
| `docs/studies/...`, generated evidence | by the owning generator: chart producer → **wide**, reproduced or photographer → **interaction**, none → **wide** |
| `docs/studies/PROVENANCE.md` | row by row against the merge base: every changed row an interaction-owned artifact's → **interaction**, else **wide** |
| `docs/studies/language-ledger/manual-review.tsv` | row by row against the merge base (#432, below) |
| `src/resources/interface-language/*.properties` | key by key against the merge base (below) |

**Keys.** A key is read by a closure when a string constant of one of
its classes is the key, a prefix ending in a dot, a stem the key
continues with a dot, or a concatenation recipe (the compiler writes
`"menu." + name + ".label"` as `menu.\u0001.label`) whose literal
pieces are key-shaped. A recipe that begins with a spliced value -
`"\u0001.label"` - names a key only when the spliced part could itself
come from a plain constant of the same closure. A key the chart can
resolve is **wide**; one only the interface can resolve is
**interaction**; one nothing names is **wide**. A key-only or
comment-only edit is interaction; without the merge base, every key
reads as new.

**The language ledger (#432).** The review ledger sits in a directory
of its own, so no study generator reads it merely by naming a folder
it shares. Its readers are judged first: if chart code reads it, every
change is **wide**. After that, each row that changed is judged by the
source its `file` column names, both before and after the change:

- **interaction** when only the interface reaches that source;
- **wide** when a chart producer reaches it, including a source both
  closures reach;
- **wide** when the source is missing or neither closure reaches it.

Anything that is not a well-formed row change is **wide**: a malformed
line, a duplicated identity, an edit to the preamble or header, a
carriage return, a reorder with nothing else changed, or a missing
base. The directory-reader rule stays as conservative as it was for
every other committed input.

**Moves.** The change is listed with `git diff --name-only
--no-renames`, so a moved file is judged at both of its paths. Until
#432, git's rename detection named only the new path, and the old one
was never judged - a gap since #398's two-route gate.

**Events.** Only a pull request can be less than wide. A push to
`main`, a tag and a manual dispatch are wide by event.

## The job matrix

| job | narrow | interaction | wide |
|---|:-:|:-:|:-:|
| `test` | ✓ | ✓ | ✓ |
| `display` (0 failed / aborted / skipped; includes the interface evidence gate) | ✓ | ✓ | ✓ |
| `interaction-evidence`: `make evidence-contracts-interaction`, then `git diff --exit-code` | — | ✓ | — |
| `evidence`: `make evidence-contracts-ci` | — | — | ✓ |
| `app-image` (4 native images + packaged acceptance + smoke) | — | ✓ | ✓ |
| `dist` (portable archive, 3 OSes) | — | ✓ | ✓ |

The interaction contract (`EvidenceContractMain interaction`) is the
portable contract over the reproduced generators only. No chart
producer runs; every chart picture is held as committed - compared
byte for byte with what is committed, with its provenance account -
and the job then requires the working tree unchanged.

**The route's first real run (#462) and its repair (#463).** A
platform record reproduces on the runner, as the contract asks, but
with the runner's bytes: the contract passed and the tree-unchanged
step then named eighteen rewritten records. Ruled: restoration, not
exclusion - excluding platform records from the step would have let an
unrelated change to one pass unnoticed. A *green* interaction run puts
back exactly the records it judged, to their **pre-run** bytes, so a
record already changed when the run began stays changed for the diff
to name; a breached run restores no record, so what it wrote stays
readable for diagnosis; nothing but judged platform records is
touched. `InteractionRestorationTest` holds all four; the step itself
is unchanged. Locally the contract took
63 s (22 generators, twice each) against about 40 minutes for the
full contract on CI. Expected wall clock for the route: about
17–18 minutes, `display` remaining the critical path.

## What changed for existing work

- Application code that no chart producer reaches - the inspector's
  report, the tables' dialogs - was narrow under #398 and is
  interaction now, because it ships in the packaged application and
  the route's packaged acceptance is what proves it.
- Interface photographs and UI reports were wide by sitting under
  `docs/studies/`; they are interaction now.
- **A finding the route surfaced, settled by #432:** the
  language-review ledger used to lie in
  `docs/studies/sky-language/`, a directory the chart producer
  `SkyLanguagePairMain` names. That producer names the folder only to
  write its pair pages, but the constant pool cannot tell a write from
  a read, so every new control's ledger rows were wide. #432 moved the
  ledger to `docs/studies/language-ledger/` and judges it row by row.
  The alternative was measured and declined: treating a class that
  only writes into a folder as a non-reader is not provable from the
  constant pool. It would also have dropped the IAU identity data
  beside the language study, `iau-constellations.tsv`, which only a
  test reads, from wide to interaction.

## Proof

`ChangeRouteTest` (narrow, interaction, shared, unknown, language-key,
provenance-row, ledger-row and entry-point cases against a stated
boundary, and the real tree against the ruling's examples, including
#430's two zoom-lock rows), `RenderingClosureTest`,
`RenderingRouteWorkflowTest` (the three gates in the workflows).
