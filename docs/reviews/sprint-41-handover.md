# Sprint 41 handover — Clear the chart, keep the controls

Three issues and three merged pull requests. The sprint measured the
chart toolbar before touching it, reproduced on a real window what
hiding it changes, and ruled a contract; then it gave the chart's
controls a second host in the Controls companion, and then let the
toolbar hide while the window remembers itself. The packaged owner
checkpoint was accepted on two Macs, one of them the observatory
arrangement the sprint was for. Three gate runs stopped and two CI
runs failed; each is recorded below as it happened, and none was
rerun into green. The interaction route still has not run on a real
pull request.

Written against `main` at `16dbc8e4` (PR #454), after its post-merge
checks. No VERSION change, no tag and no release.

## What shipped

| issue | pull request | merged | what |
|---|---|---|---|
| #449 | #452 | `72207c2` | the measured study, the real-window reproduction and the ruled decision record (`docs/decisions/clean-chart-controls.md`) |
| #450 | #453 | `ef5c5b7` | PR 1: `ChartControls` and `ChartActions`; the compact *Chart controls* section; the Emphasis keyboard repair |
| #450 | #454 | `16dbc8e4` | PR 2: View ▸ Chart Toolbar and ⇧⌘T; the remembered toolbar; the remembered window |

**Owner verdict on the packaged candidate** (`ae927816`, both Macs,
the old Intel Mac with the external monitor): **accepted** —
"Works very well. On the old Mac hooked up to the monitor: laptop
screen for the dialog, monitor a pure chart." No additional finding.

## The observatory sheet

> Hiding chart chrome does not disable the instrument.

The use the sprint was for: the chart window maximised on the
external monitor with its toolbar hidden — a map sheet on the
observatory table — and the Controls companion on the laptop display
holding every chart action a reader needs. The owner's verdict is that
this is what it now does.

## What moved into Controls, and what deliberately did not

The toolbar was inventoried control by control on #449, read off the
bar `AtlasChrome` composes (thirteen controls), and each was classed:
an **action**, a **state** control, a **readout**, or application
**furniture**.

**Into Controls, as one compact section** (ruled: one section, not
four; introduced open):

- row 1 — Zoom in, Zoom out, Lock zoom · Fewer stars, More stars · Home
  (the bar calls it Reset view);
- row 2 — Inspector · Accumulate · Emphasis;
- a search field as wide as the section;
- the field-and-magnitude readout, as a quiet line (ruled in: field
  and magnitude belong with navigation).

**Deliberately not:** the **version** (Help ▸ About has it; it is the
first thing the bar itself gives up when squeezed) and **Exit** (the
close box and the platform's Quit remain; a door beside controls a
reader works with on another display invites the one mistake nothing
undoes). The bar keeps both.

The section is first in the companion — the one a reader reaches for
while the chart is a clean sheet on another display — and measured
147 px; with all three sections open the companion scrolls at the
900 px cap, and with Chart Options collapsed (470 px) it does not.
Collapse is the reader's, remembered per section, as #443 ruled.

## Shared actions and state, and what stays local

`ChartControls` builds the ten controls and the readout over the
authorities, once per host; the toolbar and the companion section are
its two hosts, as the dialog and the companion are `ChartOptionsControls`'.
Neither copies the other.

| state | authority | both hosts |
|---|---|---|
| field, magnitude, their availability, the readout | `ChartViewController.onChange` | enable their own buttons, write their own readout, say the same words at the end of the ladder |
| Lock zoom | `ZoomLock` | ask `lock`, show `onChange` |
| Inspector | `InspectorToggle` | ask `toggle`, show showing/available and the three explanations |
| Accumulate | `SelectionMode` | ask `accumulate`, show `onChange` |
| Emphasis | `ChartComponent` | open the one menu `ChartActions` builds, reading the chart as it opens |
| Home | `ChartActions.home()` | reset the view and clear **every** registered search field |

`ChartActions` holds the two actions that are more than one call —
Home and the Emphasis menu — extracted once from the bar (ruled), so
the toolbar, the menu, the shortcut and the companion cannot diverge.

**Search** (ruled): each host has its own `SearchField`, composed
through `AtlasChrome.companionControls(chart)` in the session's
language and wired to the same selection services. **The query text
and the result list are local** to the field typed into — using one
field never overwrites unfinished text in the other — while choosing
a result moves the shared chart and joins the shared working
selection. Home from either host clears the target and both fields.

What else stays local without misleading anyone: hover text, which
button has focus.

## The toolbar's visibility

- **View ▸ Chart Toolbar**, a checked item beside Controls, with
  **⇧⌘T on macOS and Ctrl+Shift+T elsewhere** (ruled: the macOS
  convention ⌥⌘T conflicts with GNOME's terminal key). A fifth
  `Shortcuts` id, so the item and every tooltip that names it agree.
- **Default visible; remembered** — `ChartChromeStore`
  (`chartToolbarShown`), read before the window packs, so a hidden bar
  never flashes; anything unreadable is "never chosen".
- **Layout only.** Reproduced on #449 on a real window — packed,
  maximised and in macOS full screen — and held by the journey: the
  window keeps its size, the chart gains the bar's 29 px (this
  machine), and centre, field, magnitude, target, projection,
  emphasis, lock, Accumulate, search text, the companion and its
  bounds are unchanged. Every control keeps its subscription, so the
  bar returns showing every change made meanwhile.
- **Focus.** A control on a bar about to hide hands the keyboard to
  the chart. The tick follows the bar however it was hidden.
- **Recovery.** The menu bar is never hidden; View ▸ Chart Toolbar and
  View ▸ Controls are always one gesture away, and the keystroke works
  while the chart window has focus.

## The window remembers itself

Ruled into #450 because otherwise the two-display arrangement had to
be rebuilt at every launch.

- `ChartWindowStore` keeps the **last ordinary bounds** and the
  **maximised flag apart**: the ordinary bounds survive being
  maximised and are returned to on leaving it.
- `ChartWindowPlacement` applies the companion's reachability rule
  (#434): a remembered place is used only if its title strip is on a
  screen that exists now, fitted inside it; otherwise the window opens
  packed and centred as it always has.
- **Native macOS full screen is never remembered** (ruled): a
  normal-state window that fills its screen below the menu bar — the
  shape measured on #449 (0,33 1512 × 949 on a 1512 × 982 screen with
  a 33 px bar). A window the reader sizes to exactly that shape with
  the dock hidden reads the same way; that is the rule's stated limit.
- `StartupStores` grows to eleven.

## Maximised, full screen, two displays, a display that goes

**Measured on #449** (one display): maximised is `extendedState` 6
with the owned companion above the chart; macOS full screen is a Space
— `extendedState` stays 0, the frame takes the screen, and **the owned
companion is not on that Space** (Java reports it showing; the screen
shows only the chart). Recorded as a known limitation, as ruled; window
ownership is not weakened and no always-on-top trick was added.

**Proved automatically:** the placement arithmetic over a laptop plus
a 2560 × 1440 monitor, including the monitor disconnected and
reconnected (the #449 study and `ChartWindowPlacementTest`); the
restart with the bar hidden and the window moved, and a remembered
place on a gone display opening on screen (`ToolbarVisibilityJourneyTest`,
executed by CI's display job).

**Proved by hand, deliberately:** the maximised restart. CI's virtual
display has no window manager, and the display job rightly refuses an
aborted test; a probe on this Mac (three starts, logged on PR #454)
showed maximised remembered across a quit with the ordinary bounds kept
and returned to, and the owner's journey judged it on the real monitor.

**The supported arrangement** is a maximised clean chart on the
external monitor with Controls on the laptop. There is no full-screen
command.

## Accessibility and the companion with three panels

- Every control in both hosts is reachable by keyboard, **Emphasis
  included**: the bar attached it after re-asserting its buttons'
  focusability and the look and feel took it away — the #449
  inventory read `focusable = no` for it alone. Repaired in #450 PR 1
  (ruled), and held by walking both windows' own focus traversal.
- The section's heading is a focusable toggle saying the subject and
  whether it is open; the readout is a label, spoken as text, never a
  stop.
- Three panels: Chart controls (147 px) first, Place and Time, Chart
  Options; the companion's 900 px cap means all three open scroll on a
  900-tall screen, and the owner's verdict is that Controls is not
  crowded.

## The CI route, as it ran

Every pull request ran **wide**, for a reason the classifier stated.
None was rearranged to obtain a cheaper route.

| pull request | why wide | `test` wall clock |
|---|---|---|
| #452 | the `Makefile` and the evidence contract's registries, where the study is registered | 45 min 29 s |
| #453 | `JUranometriaMain` alone (#427, I1) — `AtlasToolbar`, `SearchField`, `AtlasChrome`, `ChartControls`, `ChartActions` and the photographers all interaction | 44 min 38 s |
| #454 | `JUranometriaMain`; `AppMenuBar` and `Shortcuts`, reached by `FurnitureStudyMain → PlatformEvidence → ChartKeys`; the `menu.toolbar.*` keys those resolve | 28 min 35 s |

**The interaction route has still not run on a real pull request.**
#453 came closest: one named path. Two classifier facts learned this
sprint are worth keeping:

- A string literal in a chart-reached class that is a *prefix* of
  interface keys makes the classifier read every such key as the
  chart's: `Shortcuts.TOOLBAR = "toolbar"` turned `toolbar.*` wide.
  The id is spelled `chartToolbar`.
- `ChartSection`-like ids and preference keys arrive as ledger rows,
  judged by their source's closure; the `zoomLock` row moved with the
  name it reviews from `AtlasToolbar` to `ChartControls`.

Every post-merge `test` and `app-image` run was green on its merge
head. `pages` ran once, for #452's `Makefile` change.

## Stopped, failed and incomplete runs

None of these is rewritten as green.

1. **#452, gate 3 on `9c5283d`:** `VERIFICATION INCOMPLETE` for the
   constellation and star-identity families — the fresh worktree had
   no `imports/raw/` (gitignored downloads). Copied from the local
   repository, not downloaded; the first log kept; gates 3–7 ran once.
2. **#453, gate 3 on `da5760b`:** the #449 study's deterministic report
   still recorded Emphasis as unfocusable, which that pull request
   repaired. The generator's note now says when it answered otherwise;
   regenerated; all seven gates ran again from gate 1 on `5acb533`.
3. **#454, CI on `6c4cff1c`:** the display job — the first to execute
   the toolbar journey, which aborts locally for want of window focus
   — failed at the maximised assertion (`0`): a virtual display has no
   window manager. Every claim before it had passed.
4. **#454, gate 1 on `ddd5c780`:** the journey split in two; the
   maximised test stopped because the peer reported the window's move
   after the window was maximised, so the store held the packed
   bounds. Kept; not rerun.
5. **#454, CI on `4e4a72c2`:** the maximised test, now waiting for the
   move, *aborted* on its stated premise, and the display job refuses
   an abort. The maximised restart is no longer an automated display
   test (above). All seven gates and CI then ran once on `ae927816`.
6. **Two gate scripts refused to start** on a HEAD-guard mismatch —
   git abbreviates some heads to eight characters. Nothing ran.

## Evidence moved

Between `a4b427a` (Sprint 40's close) and `16dbc8e4`: 61 files under
`docs/`.

- **The #449 study:** `docs/studies/clean-chart-controls/` — 31
  `controls-clean-chart-*` mock-ups and schematics, its report and its
  platform record.
- **Interface photographs:** the companion's ten (the third section)
  and `companion-strings.md`; the menu's eight (Chart Toolbar) and
  `menu-strings.md`. **The toolbar's eight reproduced byte for byte**
  on #453, which is how the extraction was proved.
- **Reports:** the control-explanation audit (117 controls, 15
  surfaces); the toggle-shortcut study (11 strokes); the test-evidence
  measurements and the decision's requotes (55 display files, 923
  hand-offs; doClick files 39 and postActionEvent 4, unchanged).
- **Ledger:** `zoomLock` moved to `ui/ChartControls.java`; three rows
  added (`toolbarItem`, `chartToolbarShown`, `chartToolbar`).
- **Provenance:** 20 interface rows re-dated.

**No released chart byte moved.**
- Nothing changed under `docs/reference/` or `docs/gallery/`.
- No chart-reached source changed (renderer, projection, sheet, chart,
  `ChartComponent`, modules, inks).
- Every wide head re-drew the chart evidence through both contracts
  and CI's `evidence` job, and it reproduced.

## Dogfooding

The owner accepted on two Macs with no finding, and recorded two
wishes for a later sprint, filed as #455:

- the companion titled **JUranometria Controller**, since on the
  laptop screen it reads as an application of its own;
- Controls **not reopening at launch** — it tends to cover the chart
  and is one gesture away. This changes the #433 ruling and needs the
  owner's word before it is built.

Standing, recorded rather than changed: the chart keyboard's prefix
works only while the chart window has focus; the collapsed companion
keeps its 120 px minimum height.

## What a later panel may reuse, and what is unsettled

**Reusable as built:**
- **A controls class with hosts** (`ChartControls`, `ChartOptionsControls`):
  the host owns layout; the class owns controls, writes, following and
  words.
- **Shared actions** (`ChartActions`) for anything that is more than
  one call.
- **A remembered window** — `ChartWindowStore` + `ChartWindowPlacement`
  — and the pattern of keeping ordinary bounds apart from a state flag.
- **The evidence pattern:** extract first, prove the old host
  byte-identical; one photographer per surface; a packaged journey per
  claim; a local probe plus the owner's journey for what a virtual
  display cannot witness.

**Deliberately unsettled:**
- #455's two wishes.
- Whether the old dialogs retire now that three panels live in the
  companion.
- Whether the chart keyboard should work while the companion has
  focus.
- Whether the full-screen limitation deserves a word in the
  application (it is in the record only).
- Whether the interaction route's boundary needs revisiting: one
  named path (`JUranometriaMain`) alone made #453 wide.
