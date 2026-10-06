# Sprint 42 handover — Bring the Solar System into the Controller

Four issues, one repair and five merged pull requests. The sprint
gave the companion its own name and let it start closed; measured the
Sun and Moon tables in a 360 px column before moving them; ruled a
contract; built the shared controls without wiring and then wired
them, with View's four Solar System entries grouped into a submenu.
On the way, the interaction CI route ran on a real pull request for
the first time and failed in its own last step; the defect was
repaired on `main` in a change of its own, and the route then ran
green on the same pull request. The packaged owner checkpoint was
accepted. Four gate runs stopped and one CI run failed; each is
recorded below as it happened, and none was rerun into green.

Written against `main` at `189849da` (PR #465), after its post-merge
checks (`test` 37492046401, `app-image` 37492048364). No VERSION
change, no tag and no release.

## What shipped

| issue | pull request | merged | what |
|---|---|---|---|
| #455 | #460 | `d134f9fb` | the window titled **JUranometria Controller**; it starts closed at every launch |
| #457 | #461 | `a62393c9` | the measured study and the ruled decision record (`docs/decisions/solar-system-controls.md`) |
| #458 | #462 | `bd78261f` | PR 1: `SolarTableSession`, `SolarTableControls`, the dialog as first host and the Controller's `SolarSystemSection` as second, without wiring |
| #463 | #464 | `a808a6da` | the interaction route's repair: judged platform records put back after a green run |
| #458 | #465 | `189849da` | PR 2: the wiring, the module as visibility authority, View ▸ Solar System, the packaged journey |

**Owner verdict on the packaged candidate** (`5718ab47`, the
combined head of PR 2 with `main` merged in): **accepted** — "Looks
good, approved." No additional finding.

## The Controller's name, and why it starts closed

Two rulings the owner recorded on accepting Sprint 41, after using
the companion beside a clean chart on an external monitor, amended
`docs/decisions/companion-window.md` (#455):

- **JUranometria Controller / JUranometria Kontroller.** On the laptop
  screen it reads visually as an application of its own, so its title
  says what it is. View's item stays *Controls / Kontroller*, by the
  owner's word.
- **It starts closed at every launch.** Open, it tends to cover the
  chart a reader came for, and View is one gesture away. Whether it
  was open is no longer remembered: `CompanionStore` lost `visible()`,
  `companion.visible` is neither written nor read, and a stale key in
  an existing profile is ignored. Where it was, how large and which
  sections were collapsed are still remembered, and View opens it
  where it was left.

Proved by three real starts on one preference node
(`CompanionStartupJourneyTest`), and by the packaged companion
journey, which requires the key absent.

## The Solar System hierarchy

> The Controller is the coherent home for the Solar System tools; the
> dialogs remain, and the two never drift apart.

The study (#457) measured the dialogs' controls lifted into a 360 px
column, both languages, light and dark: a Sun group with the instant
row as a **card** of heading-and-value lines is 409 px tall, as a
table 318; a Moon group 460 / 318; both groups open as cards 899; the
Controller with Solar System collapsed / Sun open / both open
959 / 1398 / 1828 (this machine's pixels). The owner ruled eleven
points, recorded verbatim in the decision; the ones that shape the
hierarchy:

- **Collapse (rulings 1 and 8).** Solar System is introduced
  collapsed. On its first expansion Sun is open and Moon collapsed.
  The three collapse choices are remembered apart
  (`solarsystem`, `solarsystem.sun`, `solarsystem.moon`), and
  **collapse state is all that is persisted** — no drafts, table
  scroll, focus, selection or popup state.
- **Contents (ruling 2).** Every Sun and Moon control except Close,
  plus each body's **Show on chart** box. An instant is shown as a
  card with every column, one line each; a range as the complete
  table scrolling sideways, up to eight rows tall and then scrolling
  down inside the group.
- **Order (ruling 9).** Chart controls, Place and Time, Solar System,
  Chart Options — so a newly introduced Solar System heading is not
  hidden below Chart Options on a 900 px laptop display.
- **Keyboard (ruling 10).** No access letters inside the Controller's
  groups, since they collide; the dialogs keep theirs. Tab reaches the
  chosen view (its sibling by the arrow keys, as in every Swing button
  group), the fields, Compute, the box and Update; every field is
  labelled and every control has an accessible name.

## Shared controls, and the boundary with the legacy dialogs

One class per concern, built once per host, the pattern Sprints 39–41
established:

- **`SolarTableSession`**, one per body, holds the applied query and
  its result — rows, outcome, the observer read — with the dialog's
  refusals (unparseable instant, backwards range, too many rows, an
  edge outside the ephemeris) moved here as data, so both hosts refuse
  the same thing. **Nothing computes unless asked** (`computations()`
  counts); building, opening, subscribing and expanding ask the
  service nothing (ruling 4).
- **`SolarTableControls`**, one per host: the observer note, the two
  views, From/To/step, Compute, the status line, the result, and —
  where the host offers a switch — the body's Show on chart box over
  `BodyOnChart`. **Drafts are local to each host** (ruling 3): a
  query applied in either host is shown by both through one
  subscription, and a result arriving never writes into the other
  host's unfinished text.
- **The dialog is the first host.** `SolarTableDialog.Content` lays the
  same controls out exactly as before, with access letters, and keeps
  its front-pull behaviour: opening it or bringing it to the front
  performs its Update, and that result is then what the Controller
  shows (ruling 4). The twenty Sun and Moon table photographs
  reproduced byte for byte on PR 1, which is how the extraction was
  proved before the second host was built.
- **`SolarSystemSection`** is the Controller's host: two groups, each
  a `CompanionSection` remembered apart, the narrow arrangement from
  the study, no access letters.

The packaged journey `solar system controls OK` holds the boundary in
every native image: one session per body followed by two hosts; a
draft applied in either shown by both with the other's unfinished
text untouched; the groups compute nothing when built; the keys the
stores always used survive.

## Visibility, the submenu, and tables kept apart from toggles

**`SolarSystemModule` is the single visibility authority (ruling
5A).** It now tells every listener when a body's visibility really
changes, and tells a new listener the current state at once; the
Sun's listeners never hear the Moon's. One switch per body
(`SunChartSession.switchOf`, `MoonChartSession.switchOf`) stands
behind View's item, the Controller's box and the store, so no control
surface bypasses another, and toggling a body computes no table.

**View ▸ Solar System** (ruling 7), in this order: Sun on the chart,
Moon on the chart, a separator, Sun…, Moon…; its own label, access
letter (Y), spoken name and description in both languages. Opening a
table does not toggle a body and toggling a body does not open a
table — held by the real-application journey
(`SolarSystemCompanionJourneyTest`) and by the owner's step 7.

## The CI route, as it ran

Every pull request predicted its route from its real dependencies
before it was written, and none was rearranged to obtain a cheaper
one.

| pull request | predicted / actual | why | `test` workflow wall clock |
|---|---|---|---|
| #460 (#455) | wide / wide | `JUranometriaMain`; `CompanionStore` through `ChartOptionsControls`; the `menu.companion.explain` keys | 1 h 15 min across three attempts (below); the jobs that ran: test 12 min, display 18 min, evidence 45 min |
| #461 (#457) | wide / wide | the `Makefile` and the contract's registries, as every study's | 44 min 55 s |
| #462 (#458 PR 1), `6ed202c2` | interaction / **interaction** | every path reached only by the interface — **the route's first real run**; the job failed in its last step (below) | 11 min 41 s to the failure |
| #464 (#463) | wide / wide | `test.yml` (the gates themselves) and `EvidenceContractMain` | 45 min 10 s |
| #462, `b9263d6f` | interaction / **interaction** | the same 32 paths, with `main`'s repair merged in — **the route's first green real run**: `interaction-evidence` 3 min 55 s | 17 min 33 s |
| #465 (#458 PR 2) | wide / wide | `JUranometriaMain`, `AppMenuBar` (`FurnitureStudyMain → PlatformEvidence → ChartKeys`), `SolarSystemModule` (`SunOnTheChartStudyMain`), the `menu.solarsystem.*` keys | 37 min 11 s |

**The interaction route is now established in real use.** Its first
run passed the contract and then failed `Nothing moved`: the eighteen
platform records the contract judges as *reproduces here, not held
across machines* had been rewritten by the Linux runner and left in
the tree, which the wide route's job never checks. The owner refused
an exclusion of `platform.md` from the step — a blind spot where an
unrelated change to a record could pass unnoticed — and ruled
restoration: a green interaction run puts back exactly the records it
judged, to their pre-run bytes; a breached run restores nothing, so
what it wrote stays readable; nothing else is touched
(`concludeUnderRestoration`, `restorePlatformRecords`,
`InteractionRestorationTest`). The repair landed on `main` on its own
(#464), `main` was merged into #462 without rewriting its commits,
the gates ran again from gate 1, and the same thirty-two paths took
the same route and came out green: "18 judged platform records, 18
put back". The route's workflow took 17 min 33 s against 37–45 min
for a wide one, `display` the critical path, as #428 expected.

Every post-merge `test` and `app-image` run was green on its merge
head (`d134f9fb`, `a62393c9`, `a808a6da`, `bd78261f`, `189849da`).
`pages` ran once, for #461's `Makefile` change.

## Stopped, failed and incomplete runs

None of these is rewritten as green.

1. **#455, gate 1 on `c7282716`:** the interface evidence gate —
   `menu-strings.md` no longer matched its photographer, because the
   Controls item's explanation changed and the menu sheet had not
   been re-recorded. Kept; not rerun.
2. **#455, gate 5 on `c5011775`:** the packaged clean-chart journey —
   the Chart Toolbar item's explanation still said the controls remain
   "in the Controls window", and the journey requires the window named
   by its title. Kept; all seven gates then ran once on `0d3ebf1d`.
3. **#455, CI on `0d3ebf1d`, three attempts:** GitHub's hosted Ubuntu
   runners never acquired the `display`, `classify` and dist jobs on
   the first attempt, nor the linux-x64 `app-image` job on the second
   ("not acquired by Runner of type hosted"); the platform cancelled
   them after fifteen minutes. The `test` job passed on attempt 1, and
   every job that ran, passed. The owner ruled this infrastructure
   non-execution, not a first-run failure: re-triggered once per job
   and recorded.
4. **#458 PR 1, gate 1 on `fbbd5b45`:** the committed test-evidence
   report no longer reproduced from its main — regenerated before the
   last edits to the new tests. Regenerated; all seven gates ran once
   on `6ed202c2`.
5. **#462, CI on `6ed202c2`:** the interaction route's first real run
   failed in `Nothing moved` after a green contract (above). Not
   retried; it remains in the record. The green run on `b9263d6f` is
   the proof.
6. **#459, gate 1 on the handover's own tree:** the interface evidence
   gate stopped in `ExportSheetDialogSheetMain` with a capture refusal
   of the #376 kind — the Norwegian PNG/A4 state packed at 332 px and
   a stale 326 px layout from the previous state arrived between the
   pack and its proof. The thirteen photographs made before it are
   byte-identical with what is committed; nothing else failed; the
   change is two Markdown files. Continued under the owner's
   seven-condition precedent (2026-10-03): recorded locally as
   unexecuted, never passed, never rerun; CI's `display` job is the
   proof that the test executes green.
7. Two trivia of the trail, recorded for honesty: a merge attempt that
   read its message from stdin and did nothing, and a gate script
   derived over itself by a wrong head variable, re-derived from its
   template. Nothing ran in either.

## Evidence moved

Between `8c61c739` (Sprint 41's close) and `189849da`: 58 files under
`docs/`.

- **The #457 study:** `docs/studies/solar-system-controls/` — 26
  `controls-solar-*` arrangements (dialogs, groups as card and table,
  both open in light and dark, the Controller at 900 px collapsed and
  with Sun open, the View menu today and as proposed), its report and
  its platform record.
- **Interface photographs:** the companion's twelve (a sixth state
  opening Solar System) and `companion-strings.md`; the menu's eight,
  now painting View's submenu as a column of its own, and
  `menu-strings.md`. The Sun and Moon table photographs (twenty) and
  the toggle-shortcut study did not move.
- **Reports:** the control-explanation audit (136 controls, 16
  surfaces — the Solar System groups a surface of their own); the
  test-evidence measurements and the decision's requotes (56 display
  files, 19 focus and 33 reachability premises, 937 hand-offs; the
  back-door ratchets unchanged — no new doClick or postActionEvent).
- **Ledger:** two rows (`solarsystem`, `solarSystemMenu`).
- **Provenance:** 21 interface rows re-dated.
- **Decisions:** `solar-system-controls.md` (new), `companion-window.md`
  (amended on #455), `interaction-ci.md` (#463), `test-evidence.md`
  (requoted).

**No released chart, export or sheet byte moved.**
- Nothing changed under `docs/reference/` or `docs/gallery/`, and no
  renderer-drawn picture, sheet or export moved.
- No chart-reached source changed except the three the ruling named
  wide (`JUranometriaMain`, `AppMenuBar`, `SolarSystemModule`), none
  of them drawing; `SolarTable` and `SolarTableWords`, which the Moon
  table study reaches, were not touched.
- Every wide head re-drew the chart evidence through both contracts
  and CI's `evidence` job, and it reproduced; the interaction head
  held every chart picture as committed with its provenance.

## What the architecture leaves ready for later planets

- **`SolarTable`** is the per-body description — columns, words,
  prefix — and `SolarTableSession`, `SolarTableControls`,
  `SolarTableDialog` and `BodyOnChart` are written over it, not over
  the Sun or the Moon. A planet is a `SolarTable`, a session, a
  `BodyOnChart` switch and a group in `SolarSystemSection`; the
  dialog, the card, the sideways table and the packaged journey come
  with it.
- **The module as authority with notification** is the seam a new
  body's chart switch plugs into; the switch pattern
  (`switchOf(module, store)`) is one static method per body.
- **A submenu the walkers understand:** `MenuSheetMain`,
  `ToggleShortcutStudyMain`, `MenuMnemonicTest` and `AppMenuBar.named`
  all recurse, so a new entry in View ▸ Solar System is photographed,
  audited and checked for colliding letters without new plumbing.
- **The route repair** means interaction-only work on these controls
  (a new table column, a new group arrangement) can take the cheaper
  route and prove the tree unchanged honestly.

## Deliberately unsettled

- **The legacy dialogs and the menu's shape.** The dialogs stay, by
  ruling; whether they retire, and whether View ▸ Solar System should
  eventually hold only the tables (the chart switches living in the
  Controller alone), is a later sprint's question, raised only after
  dogfooding.
- **Access letters in the Controller's groups.** Ruled out because
  they collide; a scheme that does not collide (per-group letters, or
  letters only for the open group) was not studied.
- **The Moon's eleven columns at 360 px.** The card and the sideways
  table are the ruled answer; a narrower Moon table was not designed.
- **Whether the chart keyboard should work while the Controller has
  focus**, carried from Sprint 41.
- **Whether the interaction route's boundary deserves revisiting:**
  `JUranometriaMain` alone still makes any wiring pull request wide,
  by ruling (#427, I1).
