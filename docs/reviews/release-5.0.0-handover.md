# Release handover — JUranometria 5.0.0, the usability programme

Five sprints, 38 to 42, between 4.0.0 (`99f6077`) and the closing
head of Sprint 42 (`ba7c68cd`). The chart was right at 4.0; using it
was not easy. This release changes nothing about what the atlas draws
and everything about how it is operated: the chart may stay a clean
sheet on the main display while every control a reader needs sits in
the **JUranometria Controller** on another, and no stored chart
choice is invalidated. The owner has used it in that arrangement and
ruled the boundary reached: "I am happy releasing 5.0.0."

This document draws the five sprint handovers together
(`sprint-38-handover.md` to `sprint-42-handover.md`, each the
authority for its own sprint) and says what the release is, what it
deliberately is not, and how it was proved. It is written against
`main` at `ba7c68cd`, after Sprint 42's post-merge checks. It carries
no VERSION change; release preparation follows the owner's
acceptance of the packaged candidate (#467).

## The friction each sprint removed

| sprint | friction | what removed it |
|---|---|---|
| 38 — Keep the field where I put it | a careless wheel notch or two-finger scroll over the chart threw away a field chosen on purpose | **Lock zoom** on the toolbar: locked, the wheel and trackpad do nothing and bank nothing; every deliberate route still zooms; the choice is remembered (#428) |
| 39 — Keep the controls at hand | Place and Time lived in a dialog that was opened, used, closed, and went stale when left open | one modeless **companion window** beside the chart, holding Place and Time as a second host of the same panel; the module is the one authority and notifies (#433, #434) |
| 40 — Keep chart choices at hand | the Chart Options dialog's OK/Cancel transaction rolled the chart keyboard's changes back and re-applied stale boxes | **shared immediate**: every accepted change is the chart at once and saved at once; Chart Options in both hosts; Restore Defaults asks first (#442, #443) |
| 41 — Clear the chart, keep the controls | the toolbar occupied the chart; Emphasis could not be reached by keyboard; a two-display arrangement lost its windows | the chart's controls in the companion; a hideable, remembered **toolbar** (⇧⌘T); the chart window remembers its bounds and maximised state and validates them against the displays that exist (#449, #450) |
| 42 — Bring the Solar System into the Controller | the companion read as an unnamed extra window that covered the chart at launch; the Sun and Moon tools were four scattered View items and two dialogs | **JUranometria Controller**, starting closed; the Sun and Moon tables and switches as a section of it, one session per body shared with the dialogs; **View ▸ Solar System** (#455, #457, #458) |

Alongside, Sprint 38 gave CI a mechanically selected **interaction
route** for interface work no chart producer reaches (#427, #428),
Sprint 39 settled the language ledger's place in it (#432), and
Sprint 42 repaired the route after its first real run (#463).

## What a reader gets

**The Controller.** View ▸ Controls (R) opens a window titled
*JUranometria Controller* / *JUranometria Kontroller*, owned by the
chart window, modeless, above it, minimising with it. It holds four
sections, in this order: **Chart controls**, **Place and Time**,
**Solar System**, **Chart Options**. Each section, and each group
inside Solar System and Chart Options, collapses and remembers its
collapse. The window remembers where it was and how large; it is
never narrower than its sections ask (360 px by default) and never
taller than 900 px or the screen; taller content scrolls.

**It starts closed at every launch.** Only the chart opens; View, or
the item's letter, brings the Controller back where it was left.
Whether it was open is not remembered (`companion.visible` is neither
written nor read; a stale key is ignored).

**Chart controls**, one compact section: Zoom in, Zoom out, Lock
zoom; Fewer stars, More stars; Home; Inspector, Accumulate, Emphasis;
a search field as wide as the section; the field-and-magnitude
readout. Search text is local to each host and its effect shared;
Home clears every search field; an unfinished draft in one host is
never overwritten by the other.

**Place and Time**, the dialog's panel as a second host over the same
module, place store and clock: a change in either host, or by the
chart keyboard, shows in both at once; refused input is explained in
both hosts and the module never sees it.

**Solar System**, introduced collapsed; on its first expansion Sun is
open and Moon collapsed. Each group holds every control of its dialog
but Close, plus the body's **Show on chart** box: the observer note,
the two views, From and To, the step and Compute, the status line,
the result — an instant as a card of every column, a range as the
complete table scrolling sideways — and Update from Place and Time.
Opening the Controller or expanding anything computes nothing.
Drafts are local to each host; a query applied in either host is the
one shared result both show; a result arriving never overwrites
unfinished text.

**Chart Options**, the seventeen boxes in four groups (Deep sky
introduced collapsed), each a one-field write onto the current value,
and Restore Defaults with its confirmation. Closing a host takes
nothing back, because there is nothing to take back.

**The chart window.** View ▸ Chart Toolbar (⇧⌘T / Ctrl+Shift+T) hides
and shows the toolbar; the choice is remembered, and so are the
window's ordinary bounds and whether it was maximised, kept apart so
that leaving the maximised state restores the ordinary bounds. A
remembered rectangle is used only on a display that exists now.

**View ▸ Solar System**: Sun on the chart, Moon on the chart, a
separator, Sun…, Moon…. The chart switches, the Controller's boxes
and the stores all follow `SolarSystemModule`, the one authority,
through one switch per body; opening a table never toggles a body
and toggling a body never opens a table.

**Lock zoom**, on the toolbar and in the Controller: locked, a wheel
or trackpad event over the chart does nothing and any banked
remainder is dropped; the toolbar's buttons, View's items and their
keys, search and recentring still change the field. The choice is a
preference, unlocked when never chosen.

Every new control has a label, hover and explanation in English and
Norwegian, an accessible name, and a place in the Tab order; the
control-explanation audit counts 136 operable controls across 16
surfaces, every one accounted for.

## Shared state and actions, and the boundary with the legacy hosts

The pattern the five sprints established, and that every panel now
follows:

- **One authority that notifies.** `MeridianModule` (observer and
  lines), `ChartOptionsController` (the seventeen options),
  `SolarSystemModule` (whether the Sun and Moon are drawn),
  `ZoomLock`. Each announces a change after it has applied it,
  whoever made it; no presentation keeps a copy to apply later.
- **One controls class, any number of hosts.** `PlaceAndTimePanel`,
  `ChartOptionsControls`, `ChartControls`, `SolarTableControls`: the
  class owns controls, writes, following and words; the host owns
  layout. Each host follows through one subscription, released when
  its window is disposed.
- **Shared actions** (`ChartActions`) for anything more than one
  call, so a toolbar button and a Controller button do the same thing
  by calling the same thing.
- **What stays local:** search text, an unfinished Sun or Moon draft,
  a section's or group's collapse, the window's bounds — presentation,
  never state. Collapse lives in the companion's own store and never
  touches the chart's options.

**The legacy dialogs remain**, behaviour-compatible and ruled to stay
until dogfooding says otherwise: Place and Time…, Chart Options…
(now Restore Defaults and Close, no OK or Cancel), Sun… and Moon…
(keeping their access letters and their front-pull Update, whose
result the Controller then shows). Each dialog's photographs
reproduced byte for byte when its controls were extracted, which is
how every extraction was proved before its second host was built.

## Persistence and startup

| what | key or store | remembered |
|---|---|---|
| zoom lock | `zoomLocked` | yes; unlocked when never chosen |
| Controller bounds | `CompanionStore` | yes, used only on a display that exists now |
| Controller open | — | **no** (ruled on #455); starts closed |
| section and group collapse | `companion.collapsed.*` | yes, each apart, with the host's default for one never chosen |
| toolbar shown | `ChartChromeStore` | yes |
| chart-window bounds and maximised | `ChartWindowStore` | yes, kept apart; validated against the displays; native full screen not remembered |
| Sun and Moon on the chart | `sunOnChartShown`, `moonOnChartShown` | yes — the keys the menu items always used; the menu restructure did not move them |
| chart options | `ChartOptionsStore` | yes, saved on every accepted change |
| Sun and Moon drafts, table scroll, focus, selection, popups | — | **no** (ruled on #457) |

Startup opens the chart window where it was, maximised if it was,
and the toolbar as it was; the Controller stays closed. Shutdown
disposes the Controller before the modules detach, so every
subscription is released first.

## Keyboard, focus, accessibility and two displays

- **Tab reaches everything** in the Controller: section headings are
  focusable toggles whose accessible names say the subject and whether
  it is open; the fields are labelled; the chosen view of a button
  group is reached and its sibling by the arrow keys. Emphasis, which
  the toolbar's Tab order skipped at 4.0, is reached in both hosts
  (#449).
- **Access letters** are kept in the dialogs and in View; none are
  assigned inside the Controller's groups, where they would collide.
- **The chart keyboard** (⌘/Ctrl-K, then a letter) works while the
  chart window has focus and is inert while the Controller has it.
  Recorded in Sprint 39, unchanged since.
- **Two displays.** The supported observatory arrangement is the chart
  window maximised on the external monitor with its toolbar hidden,
  and the Controller on the laptop display. The window remembers that
  arrangement and recovers from a display that is gone: ordinary
  bounds on a display that no longer exists fall back to the primary
  display; maximised stays maximised. Native macOS full screen is a
  Space of its own that the owned Controller does not follow, and is
  not remembered — a recorded limitation, not a supported mode.
- **Both languages** throughout; the interface language applies at the
  next start, as for every window.

## The CI routes, as they ran

Three routes since Sprint 38 — **narrow** (nothing the application or
its evidence reads), **interaction** (interface work no chart producer
reaches: the reports and photographs reproduced, every chart picture
held as committed with its provenance, `app-image` and `dist`), and
**wide** (the whole evidence contract). The boundary is derived from
the compiled class graph, never listed; `JUranometriaMain` is wide by
ruling; anything unresolved is wide. Every pull request predicted its
route from its real dependencies and none was rearranged for a cheaper
one.

| sprint | pull requests | routes | `test` wall clock |
|---|---|---|---|
| 38 | #430 | wide (`JUranometriaMain`; two ledger rows in a chart producer's directory) | about 45 min |
| 39 | #436–#440 | all wide (the guard itself; registries; `MeridianModule`; the photographer registry; `AppMenuBar` and `JUranometriaMain`) | 28–44 min |
| 40 | #445–#447 | all wide (registries; `ChartOptionsController` reached by a chart study — a true dependency; `JUranometriaMain`) | 45 min each |
| 41 | #452–#454 | all wide (registries; `JUranometriaMain`; `AppMenuBar` and `Shortcuts` through `ChartKeys`) | 29–45 min |
| 42 | #460, #461, #464, #465 | wide (`JUranometriaMain`; registries; the contract and the workflow; `AppMenuBar`, `SolarSystemModule`, the `menu.solarsystem.*` keys) | 37–45 min |
| 42 | **#462** | **interaction** — the route's first real run, twice | 11 min 41 s to its failure; **17 min 33 s** green |

**The interaction route's first real run failed, in the route.** On
`6ed202c2` the contract passed and the job's last step, *Nothing
moved*, found eighteen `platform.md` records rewritten by the Linux
runner — records the contract itself classes as *reproduces here, not
held across machines*, and left written. The owner refused to exclude
them from the step (a blind spot where an unrelated change to a record
could pass) and ruled restoration (#463): a green interaction run puts
back exactly the records it judged, to their pre-run bytes, so a
record already changed when the run began stays changed for the diff
to name; a breached run restores nothing, so what it wrote stays
readable. The repair landed on `main` in a pull request of its own
(#464), `main` was merged into #462 without rewriting its commits, the
gates ran again from gate 1, and the same thirty-two paths took the
same route green: "18 judged platform records, 18 put back". The
failed run stays in the record.

What the route still makes wide, by ruling or by true dependency:
`JUranometriaMain` (so every wiring pull request), `AppMenuBar` (a
chart study reaches it through `ChartKeys`), the modules a chart study
constructs, the registries and the guard itself. Two classifier facts
worth keeping: a string literal in a chart-reached class that is a
*prefix* of interface keys makes every such key wide (`"toolbar"`
became `chartToolbar`); and a moved file is judged at both its paths.

Every post-merge `test` and `app-image` run on every merge head of the
five sprints was green.

## Every stopped, failed, incomplete and non-executed run

None of these is rewritten as green. Each sprint handover records its
own in full; this is the complete list, by kind.

**Product failures found by a gate, fixed, and proved afresh from
gate 1:**
- Sprint 38, the first full suite on the feature tree: three failures
  (the contract's interaction mode, the moved registry, a pinned line
  number); fixed, then 1809 passed.
- Sprint 40, #446 gate 1 on `011952f`: nine failures — eight journeys
  still pressing the removed OK and Cancel, missed because the full
  suite had not been run before the gates; fixed in `4915977`, then
  the suite, all seven gates and CI.
- Sprint 41, #453 gate 3 on `da5760b`: the #449 study's report still
  recorded Emphasis unfocusable, which that pull request repaired;
  regenerated, gates again from gate 1 on `5acb533`.
- Sprint 42, #455 gate 1 on `c7282716` (`menu-strings.md` not
  re-recorded) and gate 5 on `c5011775` (an explanation naming the
  window by its old name); #458 gate 1 on `fbbd5b45` (a stale
  test-evidence report). Each kept; each followed by all seven gates
  on the next head.

**Safe capture refusals, continued under the owner's seven-condition
precedent (2026-10-03), never rerun, each proved executed by CI's
first `display` run:**
- Sprint 38, gate 1 on `8e4e1cf` (`SheetCaptureSizingTest`); CI
  1831/1831.
- Sprint 40, #445 gate 1 on `ce824c5` (the 420→371 transition); CI
  1865/1865 — the run that produced the precedent. #447 gate 2 on
  `7b3bcdf` (the Place and Time photographer, 420→326); CI 1872/1872.
- Sprint 42, #459 gate 1 on the handover tree (the export dialog's
  Norwegian state, 332→326); CI 1907/1907.

**CI runs that failed on a product claim, and what followed:**
- Sprint 41, #454 on `6c4cff1c`: the display job failed the maximised
  assertion — a virtual display has no window manager; and on
  `4e4a72c2` the rewritten test *aborted* on its premise, which the
  display job refuses. The maximised restart is a local probe and the
  owner's journey, not an automated display test. Gates and CI then
  ran once on `ae927816`.
- Sprint 42, #462 on `6ed202c2`: the interaction route's own last step
  (above). Not retried; repaired on `main`; green on `b9263d6f`.

**Infrastructure non-execution, recorded and re-triggered once per
job:**
- Sprint 42, #460 on `0d3ebf1d`: GitHub's hosted Ubuntu runners never
  acquired the `display`, `classify` and dist jobs on the first attempt
  nor the linux-x64 `app-image` job on the second; the platform
  cancelled them after fifteen minutes. Every job that ran, passed.
  Ruled infrastructure non-execution, not a first-run failure.

**Stopped for a reason outside the product:**
- Sprint 41, #452 gate 3 on `9c5283d`: `VERIFICATION INCOMPLETE` for
  two families because a fresh worktree had no `imports/raw/`; copied
  locally, gates 3–7 once. Two gate scripts refused to start on an
  eight-character head abbreviation; nothing ran.
- Sprint 42: a merge command that read its message from stdin and did
  nothing; a gate script derived over itself and re-derived.

**Development stops caught before any gate** (Sprints 39 and 40): a
classifier that failed open on a moved file's old path (`--no-renames`,
held by a test); a study measuring a clipped viewport as untruncated;
24 inspection images drifted by a shared font override; a photographer
refusing a collapsed state; the companion opening one title bar short;
a journey waiting 348 s on an unanswered question. Each is in its
sprint's handover.

## Evidence moved, and what did not

Between `99f6077` (4.0.0) and `ba7c68cd`: 177 files under `docs/`.

- **Five studies** promoted, each with its report and platform record:
  `companion-window/` (20 mock-ups), `chart-options-companion/` (18),
  `clean-chart-controls/` (30), `solar-system-controls/` (26), and the
  deep-sky-vocabulary study's 15 dialog inspection images regenerated
  when Cancel and OK left the dialog.
- **Interface photographs** (`docs/studies/interface-language/`, 40
  files): the toolbar's eight (Lock zoom); the companion's twelve
  (four sections, six states); the menu's eight (Controls, Chart
  Toolbar, the Solar System submenu as a column of its own); Chart
  Options' eight (Restore Defaults and Close); and their strings
  reports. The Place and Time dialog's twelve and the Sun and Moon
  tables' twenty did not move — the proof of every extraction.
- **Reports:** the control-explanation audit (105 controls on 13
  surfaces at 4.0.0; 136 on 16 now); the toggle-shortcut study; the
  test-evidence measurements and the decision's requotes (56 display
  files, 937 explicit hand-offs; the doClick and postActionEvent
  ratchets never raised without a stated reason).
- **Ledger:** moved whole to `docs/studies/language-ledger/` (#432);
  rows added for every new control and key.
- **Provenance:** 37 rows, every one an interface photograph or a
  widget study.
- **Decisions:** five new (`interaction-ci`, `companion-window`,
  `chart-options-companion`, `clean-chart-controls`,
  `solar-system-controls`), `test-evidence` requoted, and
  `docs/development.md` for the routes.

**No released chart, export or sheet byte moved.**
- Nothing changed under `docs/reference/` or `docs/gallery/`.
- No PNG, SVG or PDF changed outside the interface and widget-study
  directories named above.
- The only chart-reached sources that changed are `MeridianModule`
  and `SolarSystemModule`, each gaining notifications and no ink; no
  renderer, projection, sheet, chart, module-ink or `ChartComponent`
  source changed.
- Every wide head re-drew the chart evidence through both contracts
  and CI's `evidence` job, and it reproduced; the interaction head held
  every chart picture as committed with its provenance.

Release preparation will move only what `VERSION` feeds — the gallery
manifest and pages, the chart sheets' embedded producer, the four
compact About images, the About and page-language companions, and
their provenance rows — and will show the exact pixels that moved.

## Accepted limitations and deferrals

Stated here and in the changelog; none blocks 5.0.0:

- the legacy Place and Time, Chart Options, Sun and Moon dialogs
  remain available; retirement waits for dogfooding;
- the Controller deliberately starts closed at every launch;
- native macOS full screen does not carry the owned Controller onto
  the chart's Space; the supported observatory arrangement is a
  maximised clean chart on the external monitor with the Controller
  on the laptop;
- chart-window shortcuts stay scoped to chart focus while the
  Controller has focus;
- no access letters inside Controller groups, because they collide;
  ordinary keyboard traversal is supported;
- the collapsed Controller keeps its 120 px minimum height;
- layout refinements noticed during dogfooding are deferred to a later
  polishing sprint, unless the final journey finds a functional or
  accessibility defect;
- the interaction route's boundary still makes `JUranometriaMain`
  wiring wide;
- macOS downloads remain unsigned and not notarised (#282), and no
  page has been read on paper (#293).

**The polishing boundary.** Everything the owner noticed about layout
while dogfooding — spacing, the collapsed height, how sections share
room — is a later sprint's; what this release holds is that every
control works, is reachable, is explained in both languages, and
agrees with its other host.
