# Chart Options in the Controls companion

Sprint 40, issue #442. **Proposed — awaiting the owner's ruling.** No
production Chart Options companion exists; this record is what #443
would build. The study is `docs/studies/chart-options-companion/`
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

## Questions for the owner

1. Semantics: **1, shared immediate**?
2. Layout: **independently collapsible groups**, with the old dialog
   keeping its tabs?
3. Default: Chart Options expanded, with **Deep sky collapsed** and the
   rest open?
4. Restore Defaults: immediate as proposed, or immediate **after a
   confirmation** ("Reset all chart choices?")?
5. Split: **A** or **B**?
