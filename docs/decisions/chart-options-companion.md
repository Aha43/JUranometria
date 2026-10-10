# Chart Options in the Controls companion

Sprint 40, issue #442. **Ruled by the owner (2026-10-03)** — see
[The ruling](#the-ruling) at the end, which governs where it differs
from the proposal. #443 builds it in two pull requests; the first
makes the existing dialog shared immediate. The study is `docs/studies/chart-options-companion/`
(`make chart-options-companion-study`).

> A persistent control panel tells the truth about the chart now. It must
> not conceal a private draft that can later overwrite changes made
> elsewhere.

## What exists today

Measured on `main` at `bb98009`.

- **The value.** `ChartOptions`, an immutable record of 16 switches and
  a palette (17 controls). It stores the reader's **remembered** choices.
  The **effective** drawing is computed when it is read: a deep-sky
  family or deep-sky labels draw only while deep-sky objects are on, and
  constellation names only while figures are on. A child's choice
  survives its master going off and on.
- **The authority.** `ChartOptionsController` holds the current value.
  `apply(next)` notifies listeners on a real change, and `confirm()`
  saves. Its `onChange` has **no way to unsubscribe**. It reaches the
  chart only through `TargetRetirement`, which also retires a searched
  target whose family a change hides.
- **The writers.**
  - The dialog's boxes preview through `apply`.
  - OK runs `confirm`.
  - Cancel, Escape and the close box run `revertTo(snapshot)`.
  - Restore Defaults previews the defaults.
  - The chart keyboard (⌘/Ctrl-K then a letter, 17 letters) runs
    `apply`, then `confirm` at once, from a stored value.
  - Startup loads the store.

  Nothing else in production writes options. Home touches the view only.
- **The store.** It saves on `confirm()` only, and the shutdown path
  flushes it. An unreadable value falls back to the released default.
- **The dialog.** A modeless, single-instance `JDialog`. Its four tabs
  hold the 17 controls: Deep sky 7, Stars 3, Constellations 3, Chart 4.
  Below them sit Restore Defaults, Cancel and OK. It takes a snapshot
  once, when it is built, and **never listens to the controller**.

### Reproduced, not inferred

The real dialog, opened through its own `open`, and the chart keyboard
driven through its own `ChartSwitches` route, on a scratch store,
`bb98009`:

```
cancel:            after keyboard E grid=false stored=false dialogBox=true | after Cancel grid=true stored=false
stale-click:       after keyboard E grid=false stored=false dialogBox=true | after clicking Title block grid=true title=false | after OK stored grid=true
ok-after-keyboard: after keyboard E grid=false stored=false dialogBox=true | after OK grid=false stored=false
reopen-snapshot:   after keyboard E grid=false stored=false dialogBox=true | keyboard T then close box: grid=true title=true stored grid=false title=false
```

So the open dialog does go stale and does roll back:

1. **Cancel restores the opening snapshot over the keyboard's change.**
   The chart then shows the grid while the store says it is off, until
   something saves again.
2. **Any later click in the dialog re-applies all 17 of its stale
   boxes.** The keyboard's change is undone, and OK then saves the
   undone value.
3. **OK alone keeps a keyboard change.** OK confirms the current value
   and never writes the boxes.
4. **Several keyboard changes are all reverted by the close box**, and
   the chart and the store disagree on each of them.

This is the Place and Time defect class from #433, worse: Cancel writes
a whole stale value, not one field. Under the freeze it is not fixed
alone. The semantics below remove it by construction.

## The semantics, compared

| | **1. Shared immediate** (recommended; the owner's hypothesis) | 2. Immediate companion, transactional dialog | 3. Explicit companion transaction |
|---|---|---|---|
| a box in the companion | applies and saves | applies and saves | changes a draft only |
| a box in the old dialog | applies and saves | previews; OK saves, Cancel takes back | — |
| Restore Defaults | an accepted change, saved | companion: saved; dialog: a preview until OK | a draft until Apply |
| the chart keyboard | as today, applies and saves; every presentation shows it | the same | the chart changes; the draft does not |
| what the boxes show | always the chart | companion: the chart; dialog: the chart plus its own pending edits | the draft, which may not be the chart |
| can a change be lost? | **no** | only if Cancel reverts by snapshot. Safe only by undoing the dialog's **own** edits, field by field, and only where nothing changed that field since — a rule a reader cannot see | yes: Apply of a draft built before a keyboard change writes over it, unless Apply merges field by field, and then the draft was not what the reader saw |
| what a reader must learn | nothing: a box is the chart | two models in two windows over one chart | Apply and Revert, and that the chart and the panel can disagree |

**Why 1.** It is the only one where the panel is always the chart, and
it already is the chart keyboard's model and the View menu switches'
model. Alternative 2 has to give Cancel a meaning ("take back only what
this dialog changed, unless something else changed it since") that no
reader can predict from the screen. Alternative 3 breaks the principle
outright: a panel left open all day would show a draft, not the chart.

**Its consequences, stated:**
- **The old dialog loses OK and Cancel.** It closes with **Close**, the
  close box or Escape, and closing takes nothing back
  (`controls-co-companion-*-7-dialog-shared-immediate.png`).
- **Restore Defaults becomes immediate.** It changes up to 17 choices
  at once and is saved at once; there is no Cancel to take it back.
  The chart shows the result at once, and every switch can be set back
  by its box or its letter. Whether Restore Defaults should ask first
  is a question for the owner, below.
- **Every change is saved when it is made**, as the keyboard already
  does. One gesture means the same thing wherever it is made.

## The layout, measured

At the companion's default width (360 px), on this machine. The full
table, both languages and both appearances, is in the study's
`platform.md`.

| | English | Norwegian |
|---|---:|---:|
| Deep sky's controls | 423 | 437 |
| Stars / Constellations / Chart | 91 / 91 / 112 | 91 / 91 / 112 |
| Chart Options as four tabs | 569 | 583 |
| as four groups, all open | 912 | 926 |
| Place and Time | 293 | 293 |
| both sections, groups all open | 1205 | 1219 |
| narrowest untruncated companion, either layout | 338 | 336 |

The companion's 360 px default holds Chart Options untruncated in both
languages, so no wider window is needed.

Both sections fully open (about 1210 px) fit only a 1440-tall screen.
On 800, 900 and 1080 they scroll. What fits depends on what is open:

| arrangement | height with Place and Time | 900-tall screen (847 usable) | 1080 (1027) |
|---|---:|---|---|
| groups all open | 1205 | scrolls | scrolls |
| tabs | 862 | scrolls | fits |
| one open at a time, Deep sky open | 911 | scrolls | fits |
| groups, Deep sky collapsed, the rest open | 782 | fits | fits |

Keyboard stops through Chart Options (from the study's report):

- **Tabs:** 8 to reach Deep sky's last control. Another subject is a
  separate gesture inside a scrolling window.
- **Groups, all open:** 21 stops.
- **Groups, one open:** 11 stops with Deep sky open.

**Recommended: independently collapsible subject groups.**
- **Why groups.** They are the companion's own model: a section heading
  that collapses. Each subject's heading is a keyboard stop that says
  whether it is open, and a reader can keep open exactly the subjects
  they use.
- **Why not tabs.** Tabs inside a scrolling side window are a second
  navigation inside the first. They show one subject at a time and
  still cost 569 px.
- **Why not one-open-at-a-time.** It saves height, but forbids a
  reader from keeping two subjects in view. It also turns opening one
  subject into closing another, which is a side effect.

**Default for a newly introduced Chart Options section:**
- **Expanded,** with Stars, Constellations and Chart open and **Deep
  sky collapsed**.
- **Why.** Deep sky is 58 % of Chart Options' height. Collapsed, the
  whole companion fits a 900-tall screen beside Place and Time.
- **Remembered.** Each subject is remembered as the reader leaves it,
  under its own id (`chartoptions.deepsky` and so on, through
  `CompanionStore.collapsed`).

**The old dialog keeps its four tabs.** Only its footer changes, to
Restore Defaults and Close.

## Architecture invariants #443 holds

1. **One current value:** `ChartOptionsController`'s.
2. **Every presentation observes every change:** the companion's
   panel, the dialog and the keyboard's palette all follow the
   controller.
3. **No whole stale value is ever written:**
   - a box writes the controller's **current** value with its one field
     changed, as the keyboard already does, never all 17 of its own
     boxes;
   - Restore Defaults writes the defaults;
   - nothing writes a snapshot.
4. **Dependencies:** an off master disables its children in every
   presentation and hides them on the chart, and the children's
   remembered choices survive.
5. **Persistence:** every accepted change is saved the moment it is
   made, by every presentation and the keyboard alike.
6. **No duplicate listeners:** the controller gains a `Subscription`
   (unsubscribe), as `MeridianModule` did. Reopening the dialog or
   hiding the companion adds no listener, and disposing releases.
7. **Startup:** with nothing remembered, the application still draws
   exactly the released chart (the defaults are unchanged).
8. **No chart evidence moves** because another presentation exists.

## CI route, measured

With the classifier, on `bb98009`:

| path #443 touches | route | why |
|---|---|---|
| `render/ChartOptions.java` | wide | the renderer reads it — not touched |
| `app/ChartOptionsController.java` (the `Subscription`) | **wide** | a chart producer reaches it |
| `app/ChartOptionsDialog.java` (shared presentation) | **wide** | the same |
| `app/JUranometriaMain.java` (the second section) | **wide** | by ruling (#427, I1) |
| `ui/companion/*`, `tool/CompanionSheetMain.java` | interaction | |
| `InterfacePhotographers.java` | wide | **not touched**: extending the companion's photographer needs no registry change |

The controller and dialog are wide through
`DeepSkyOcclusionStudyMain → DeepSkyVocabularyStudyMain →
DeepSkyVocabularyMockupMain → ChartOptionsDialog`. **This is a true
dependency, not only a conservative boundary.** `DeepSkyVocabularyStudyMain`
owns renderer-drawn pictures (`symbols-*.png`), and its run photographs
the dialog and records its access letters in a report only the wide
route regenerates. So #443 is **wide**, and no boundary change is
proposed.

## Evidence #443 moves

- **Interface:**
  - the Chart Options photographs (`chartoptions-*`: the footer
    becomes Restore Defaults and Close);
  - the companion photographs (`companion-*`), gaining the Chart
    Options section, with its subjects open and collapsed;
  - the control-explanation audit, which loses Cancel and OK and gains
    Close and four subject headings;
  - the test-evidence report;
  - ledger rows for new identifiers;
  - the `deep-sky-vocabulary` inspection images (drift restored, as
    their class allows).
- **Must stay byte-identical:**
  - every renderer-drawn chart picture and chart provenance row;
  - the gallery and reference images;
  - `deep-sky-vocabulary/measurements.md`, unless the dialog's access
    letters change — they need not.
- **A packaged journey,** `chart options OK`:
  - the panel and the dialog follow a keyboard change;
  - a box writes one field;
  - Restore Defaults is saved;
  - closing takes nothing back;
  - the subscriptions are released.

## Implementation split

**Option A — one pull request (wide).**

**Option B — two, each wide** (recommended):
1. **Shared immediate semantics in the existing dialog.** The
   controller gains its `Subscription`, the dialog subscribes and
   writes one field at a time, and the footer becomes Restore Defaults
   and Close. This removes the reproduced defects on its own, and is
   reviewable apart from the companion.
2. **The Chart Options section in the companion.** A shared panel built
   from the dialog's subject columns, collapsible groups, the wiring
   and the evidence. The owner's packaged checkpoint is on this one,
   before it merges.

## The ruling

1. **Shared immediate.**
   - Every accepted change updates the chart and persists at once.
   - The companion, the old dialog, the keyboard and the View switches
     all show the same current value.
   - Each control changes only its own field in the controller's
     latest value.
   - The old dialog loses OK and Cancel and gains **Close**; the close
     box and Escape close without reverting anything.
2. **Four independently collapsible subject groups** in the companion:
   Deep sky, Stars, Constellations, Chart.
   - Each group remembers its own collapse, and more than one may stay
     open; it is not an accordion.
   - The old dialog keeps its tabs. Both hosts share the controls and
     the semantics, not the layout.
3. **First appearance:**
   - the outer Chart Options section is expanded;
   - Deep sky is collapsed;
   - Stars, Constellations and Chart are open.

   After that, every collapse is remembered.
4. **Restore Defaults is immediate after an explicit confirmation**:
   "Restore every chart option to the atlas defaults?", with Cancel as
   the safe default. Confirmed, it is one authoritative change, saved at
   once, and both presentations update together.
5. **Two pull requests, both wide:**
   1. The controller's notification and the shared-immediate old
      dialog. This independently removes the reproduced defects.
   2. The companion section, grouping, evidence and journeys, stopping
      at the packaged owner checkpoint before merge.

   The real wide dependency is not rearranged to seek the interaction
   route.

## A conditional precedent for capture refusals

Recorded by the owner on #442 (2026-10-03). It is not a blanket
exemption for `SheetCaptureSizingTest`.

A safe capture refusal in gate 1 may continue to gates 2–7, the push
and CI without another ruling **only when all of these hold**:

1. The full refusal trace and its retained evidence exist.
2. The refusal is a known fail-closed geometry transition.
3. No generated artifact disagrees with committed bytes.
4. No unrelated test fails.
5. The change cannot affect capture sizing, window lifecycle,
   generator behaviour or the refused surface.
6. The local record calls the test unexecuted, never passed.
7. The first CI `display` run executes that exact test successfully,
   with zero failures, aborts or skips.

If any condition is unclear, stop for a ruling.

**Clarified by the owner**, recorded here in the Sprint 45 handover
(#487):

- **Any gate, not only gate 1** (#443, 2026-10-03). The precedent
  attaches to the kind of failure. A safe capture refusal in a later
  gate qualifies too, for example in `InterfaceEvidenceGateTest`'s run.
  Only the remaining gates then continue, and the refused gate is never
  rerun.
- **"Generator behaviour" in condition 5** means the generator involved
  in the refusal, or shared machinery able to affect it. A change to a
  different photographer still qualifies.
- **The kind of transition, not its widths** (#482 on `aa142b99`,
  2026-10-09). Gate 1 refused `PlaceAndTimeSheetMain` at 420→326 and
  gate 2 refused it again at 420→324. Condition 2's "known fail-closed
  geometry transition" means the known **kind** of transition: the
  application-sized window replacing the declared content before proof.
  It is not an allowlist of exact rollback widths. **Exact rollback
  dimensions are diagnostic evidence, not the identity of the known
  transition.**
  - A second refusal in a later gate on the same head is recorded
    incomplete in the same way, with none of the refused tests counted
    as passed locally.
  - Restarting from gate 1 is refused, because it would only seek a
    luckier capture and would erase the first run's record.
  - CI's first `display` run must then execute every refused test with
    zero failures, aborts or skips. On `aa142b99` it did.
