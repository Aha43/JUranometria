# The rendering-neutral gate

Sprint 35, issue #398. Which changes may skip the rendering regime, how
that is decided, and what it can never decide.

## The question

The Solar System road (#397) begins with calculation: where the Sun
is, then where the Moon is, as numbers, before either is drawn. That
work cannot change a rendered page, and it should not pay an hour of
CI — the evidence contract, four native images, the portable archive —
to prove what it cannot change. But "cannot change a rendered page" is
not something a pull request may say about itself. It has to be shown,
every time, by something that does not take the author's word for it.

## The rule, as ruled

A change takes the **narrow** route — the unit suite and the display
suite only — when a guard proves that it touches no renderer, chart
contribution, rendering or evidence generator, committed generated
image or provenance row. Anything else, or any doubt, is **wide**, and
the whole regime applies. Pushes to `main`, tags and manual dispatches
are wide by event, whatever they changed.

This is permanent and body-agnostic: the Moon and later the planets use
it unchanged. It is not a shortcut to be reverted after a sprint.

## How it is decided

`juranometria.tool.ChangeClassifierMain` runs in CI (the reusable
`classify` workflow, called by `test`, `app-image` and `dist`) and
locally (`make classify BASE=origin/main`). Git lists every path
changed since the merge base with the target branch, untracked files
included, and hands the list to the classifier — which starts no
process itself, as `OfflinePromiseTest` requires of every class that
ships. The route of the change is wide if any path is.

A path is wide for one of two reasons.

**The mechanical half: the rendering closure.** `RenderingClosure`
reads the compiled classes of the head and follows every reference
from a fixed set of roots — the programs whose output CI compares:

- `EvidenceContractMain`, whose registries name every study generator,
  and `EvidenceProvenanceMain`;
- `ChartImageMain`, the smoke render the image workflow compares;
- every interface photographer, `juranometria.tool.*SheetMain`;
- the two index writers `make classes` runs, which write into the
  build everything downstream reads.

Every `juranometria` class reached, transitively, is mapped through its
`SourceFile` attribute to the source file that compiled it. A change to
any of those files is wide, and the finding names the chain from the
root. The scan reads every constant-pool string, so a generator the
registry names only as a dotted string handed to `Class.forName` is
followed like a typed reference. That over-approximates in the safe
direction. On the tree at #398 the closure is 568 of 634 classes, in
266 source files; what lies outside is the application's entry point,
the text-only features such as the view report, and the guard itself.

**The named half: inputs the regime consumes without compiling.** These
are wide by path, whatever the closure says:

- `docs/studies/**` — the evidence tree the contract regenerates and
  holds to its bytes, provenance rows and pinned fixtures included;
- `docs/reference/**` and `docs/gallery/**` — committed images and
  pages;
- `src/resources/**` — bundled resources, read by renderers and
  generators and shipped in every package. A resource is wide whether
  or not a renderer reads it, because which resources a class reads is
  not something a class file states, and a rule that guessed would one
  day guess wrong;
- `packaging/**`, `scripts/**`, `Makefile`, `VERSION`, `LICENSING.md`
  — packaging and build inputs;
- `.github/workflows/**` — the gates themselves;
- `src/juranometria/app/JUranometriaMain.java` and
  `PackagedAcceptanceMain.java` — the packaged application and its
  qualification;
- the guard's own three files.

A removed source file is wide too: what depended on it at the base
cannot be read from the build at the head.

Everything else is narrow: tests, decision records and other prose,
and source outside the closure — which is where a calculation service
nothing rendering-shaped refers to lives.

## What the answer means in a run

- `narrow`: the `evidence` job, the native `image` matrix and the
  `dist` build and verify are skipped, and the classifier's step
  summary says so, path by path, with the reason each is narrow. The
  `test` and `display` jobs run regardless; they never consult the
  route.
- `wide`: everything runs, as before #398.
- A classifier that cannot compute the boundary — no build, no merge
  base — fails its job, which skips every job that needs it and turns
  the run red. Silence is never read as narrow.

## What it is proved by

- `RenderingClosureTest`: every renderer, ink, module and sky class is
  reached; every generator the contract registers, every interface
  photographer and every tool the build runs is a root or reached from
  one, read from their own source; every chain starts at a root and
  each link is a reference the class file carries; the closure is
  smaller than the tree; an incomplete build refuses.
- `ChangeRouteTest`: mutation proof. One path of each kind the ruling
  named — a renderer, a generator, a committed image, a provenance
  row, a packaging input, a workflow, the entry points, the guard —
  forces wide and names its reason; a test, a decision record and a
  calculation class outside the closure come back narrow; a mixed
  change is wide.
- `RenderingRouteWorkflowTest`: the three pull-request workflows gate
  exactly their expensive jobs on the route, the required suites never
  read it, and the release workflow does not gate on it at all.

## What it cannot decide, said plainly

- **Generators the contract does not register** are outside the
  closure unless something registered reaches them. Their committed
  output under `docs/studies` is wide by path when it moves, but a
  change to the generator alone is narrow. That is issue #324's gap,
  not a new one, and this gate does not paper over it.
- **The first data pack is wide by ruling.** Introducing the Solar
  System pack under `src/resources/solar-system/` changes packaging, and
  the owner ruled it wide even where it changes no renderer. The named
  half makes that mechanical. Later calculation-only changes may be
  narrow.
- **A route is a classification, not a judgement.** The classifier's
  exit status is 0 for either answer. What it refuses is to answer at
  all when it cannot compute the boundary.

## Ownership, amended with it (R1)

`PlaceAndTimeGateTest` used to forbid any bundled resource named leap,
ut1, ephemeris, iers or horizon, anywhere. Place and Time still brings
no data. The rule now proves ownership: such resources may live only
under `src/resources/solar-system/`, and the sky model, the meridian
module and the Place and Time dialog refer to nothing under
`juranometria.solar`. The service consumes Place and Time's observer
and civil instant; Place and Time learns nothing of how an ephemeris
works.
