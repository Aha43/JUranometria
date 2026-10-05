# The companion window, around Place and Time

Sprint 39, issue #433. **Ruled by the owner (2026-10-02)** — see
[The ruling](#the-ruling) at the end, which governs where it differs
from the proposal. No production companion exists; this record is
what #434 builds.
The study is `docs/studies/companion-window/`
(`make companion-window-study`).

> The chart remains the main experience. Controls stay available beside it
> without becoming a second chart or an alternate sky viewer.

## What exists today

Measured on `main` at `7c6d72d`.

- **The authority.** `MeridianModule` owns the one observer (place
  and frozen instant) and the three line choices — meridian,
  mathematical horizon, zenith. It has **no change notification**:
  `observer(Observer)` and `showing(m, h, z)` set and redraw, and
  every reader pulls.
- **The dialog.** `PlaceAndTimeDialog` is a modeless, single-instance
  `JDialog` owned by the chart window, opened only from
  View ▸ Place and Time…. It has no Apply/OK/Cancel: a field applies
  when committed (Enter or focus loss) and a box when clicked. A
  refused value is silently put back. The dialog is disposed on close
  and built anew on the next open, at a computed size (a 420-pixel
  floor), centred on the chart window. Nothing is remembered.
- **The writers.** Two presentations write the line choices: the
  dialog's boxes and the chart keyboard (⌘/Ctrl-K then R or H, through
  `ChartKeyboardSession`). Readers — the Sun and Moon tables, the
  bodies on the chart, Copy View Report — pull when they draw or gain
  focus.
- **Persistence.** `PlaceStore` keeps the place (`place.latitude`,
  `place.eastLongitude`); the instant and the line choices are
  per-session by design (the session starts with every line off).
- **Language and appearance.** The interface language is fixed for a
  session (Settings says a restart applies it). Appearance changes
  reach every open window through `FlatLaf.updateUI()`.
- **No window anywhere remembers its bounds**, and nothing recovers a
  window from a display that has gone.

### A defect this study found

**The dialog's boxes go stale, and a stale box undoes the keyboard.**
The dialog reads the module once, when it is built. With it open, the
chart keyboard turns the meridian on; the dialog's Meridian box still
shows it off; ticking Zenith in the dialog then sends all three boxes
and turns the meridian back off. Reproduced against the production
content on `7c6d72d`:

```
after keyboard: meridian=true dialog box=false
after zenith click: meridian=false zenith=true
```

It ships in 4.0.0. Under the UI freeze it is not fixed alone: it is
exactly what the synchronization below has to make impossible, and
#434's first contract would hold it.

## Three choices, compared

### 1. Where the change notification lives

| | **A. At the authority** (recommended) | B. In a UI-side controller |
|---|---|---|
| shape | `MeridianModule.onChange(listener)` returns a `Subscription`; `observer(…)`, `showing(…)` notify after redraw | a new `PlaceAndTimeControl` wraps the module; every writer goes through it and it notifies |
| a writer that bypasses it | cannot exist: every mutation is the module's | possible — the chart keyboard writes the module today; a guard test would have to forbid direct writes |
| the authority | unchanged owner, gains a notification | a second object that looks like an owner |
| route | **wide** (`MeridianModule` is reached by chart producers) | interaction |

A is the natural owner (#427, condition 6): the module already is the
one authority, and a notification that every mutation raises cannot be
bypassed. B is cheaper in CI only, and the ruling forbids choosing
it for that.

### 2. What the companion hosts

| | **A. One shared panel** (recommended) | B. A companion-only panel |
|---|---|---|
| shape | the dialog's content becomes `PlaceAndTimePanel`; the dialog and the companion each hold one | the companion builds its own controls |
| one set of actions | by construction | by discipline: two implementations to keep equal |
| compatibility proof | the dialog's twelve committed photographs stay byte-identical | none |

### 3. What the companion is

| | **A. An owned, modeless `JDialog`** (recommended) | B. An independent `JFrame` | C. A side panel in the chart window |
|---|---|---|---|
| beside the chart | stays above its owner; minimises with it | can fall behind the chart window | always |
| on a small screen | stands over the trailing edge (drawn) | hidden behind | shrinks the chart |
| own Dock / taskbar entry | no | yes | — |
| | | | rejected: the owner ruled a window, and at the Inspector's 240-pixel floor these controls were measured to truncate (`docs/decisions/place-and-time.md`, "Where the controls live") |

## The proposal

- **One instance.** Built on first open, then hidden and shown —
  never rebuilt — so its listeners are attached exactly once. Closed
  (the close box or Escape) it hides and returns focus to the chart
  window. Disposed at application shutdown with every window, after
  its subscriptions are released on the shutdown path.
- **The host.** A vertical list of collapsible sections; this sprint
  holds one, Place and Time. A section heading is a toggle whose
  accessible name says the section and whether it is expanded. The
  content sits in a scroll pane that follows the window's width and
  never scrolls sideways (the study's first run measured a clipped
  160-pixel companion as untruncated without that).
- **How a panel gets state.** `PlaceAndTimePanel(module, store, clock,
  said)` — the authority, the place store, a clock read only by Now,
  and the session's words. It knows nothing of the window that holds
  it; it subscribes to the module and releases the subscription when
  its holder is disposed.
- **Synchronisation.** Both presentations subscribe. On a change each
  refreshes its boxes and every field **except one holding an
  uncommitted edit**, which keeps the reader's typing until it commits
  or is refused.
- **Refused input.** As now, the field is put back — and a line under
  the section says why, in the section's language, as the field's
  accessible description so a screen reader hears it. No dialog, no
  beep. Shared, so the old dialog gains it too (additive; drawn in
  the study as `controls-companion-*-refused-360.png`).
- **Remembered.** Position, size, whether it was open, and each
  section's collapse, in a `CompanionStore` beside the others. On
  startup a remembered rectangle is used only if at least its heading
  row lies on a screen that exists; otherwise the companion goes beside
  the chart window's trailing edge if there is room, and over that
  edge if not (drawn at four screen sizes).
- **Width.** Default 360 pixels. Minimum is the content's own
  preferred width plus the shell's insets, read at runtime — measured
  here at 342 (English) and 340 (Norwegian) on one machine; another
  desktop draws words at other widths, which is why it is read and not
  written down.
- **Language.** Built in the session's interface language, like every
  window; a language change applies at the next start, which Settings
  already says.
- **Appearance.** Follows `FlatLaf.updateUI()`. The panel re-wraps its
  frozen note when its UI is updated (today the note is wrapped once,
  at build time).
- **Focus and keys.** Opening focuses the first field of the first
  expanded section; Tab follows component order; Escape hides. The
  chart keyboard's ⌘/Ctrl-K is bound to the chart window and stays
  inert while the companion has focus — stated, not changed.
- **The access point.** One View-menu item, **after Place and Time…**,
  a checkbox that shows and hides the companion and reflects whether
  it is open. No accelerator in this sprint. The menu is otherwise
  unchanged, and Place and Time… still opens the dialog.

## Invariants #434 holds

1. One observer and one frozen instant, owned by `MeridianModule`;
   no presentation holds a copy it applies later.
2. One set of line choices, owned by the same module.
3. A mutation through the dialog, the companion or the chart keyboard
   is observable through every other presentation before the next
   event is processed — including the defect above.
4. Reopening the companion or the dialog adds no listener: the
   module's subscriber count returns to its baseline after every
   close of the dialog, and the companion holds exactly one.
5. Hiding or closing the companion changes nothing on the chart.
6. The companion owns no painting and no astronomy.
7. The dialog keeps its behaviour: its committed photographs stay
   byte-identical (with the refusal line the one deliberate addition,
   which changes a photograph only of a refused state).
8. No chart ink and no chart evidence moves because a second
   presentation exists.

## CI route

Measured with the classifier on `7c6d72d`:

| path | route |
|---|---|
| `meridian/MeridianModule.java` (notification) | **wide** — chart producers reach it |
| `ui/placeandtime/*` (panel, dialog, store) | interaction |
| a new `ui/companion/*` (window, host, store) | interaction, once the application reaches it |
| `app/AppMenuBar.java` (the access point) | **wide** — a chart study reaches it |
| `app/JUranometriaMain.java` (wiring) | **wide** by ruling (#427, I1) |
| `app/StartupStores.java`, `AppShutdown.java` | interaction |
| new interface keys, read only by interface code | interaction |
| new ledger rows on interface sources | interaction (#432) |
| `docs/studies/interface-language/*` (photographs) | interaction |

The whole of #434 is therefore **wide**. Split in delivery order, its
middle is interaction: the panel and the window can land, photographed
and journeyed, before the menu and startup wiring reach them.

## Evidence #434 needs

- A photographer for the companion (`CompanionSheetMain`, interface
  photographs in both languages and appearances, collapsed and
  refused states) registered in `InterfacePhotographers`.
- The dialog's existing photographs, unchanged.
- The control-explanation and test-evidence reports regenerated; new
  ledger rows for new identifiers.
- A packaged journey, `companion OK`: open, change the place in the
  companion and see it in the dialog and the module; toggle a line by
  the chart keyboard and see it in both; close and reopen without a
  new listener; remembered bounds across a restart on a scratch node.
- Nothing chart-drawn.

## Implementation split

**Option 1 — one pull request (wide).** Everything in #434 together.

**Option 2 — three, in order** (recommended):

1. `MeridianModule` change notification with `Subscription`, and the
   dialog subscribing — which fixes the defect above. **Wide.**
2. `PlaceAndTimePanel`, the companion window, its host and store, and
   their photographer and journeys — reached by the photographer and
   the tests, not yet by the menu. **Interaction**: the route's first
   real pull request.
3. The View-menu item, startup restore and shutdown release.
   **Wide.**

Option 2 costs two wide runs instead of one and moves no code; it
lands the authority change on its own, reviewable, and gives the
interaction route an honest first run. Option 1 is one review and one
wide run.

## The ruling

The owner's answers to the five questions put at the checkpoint:

1. **Architecture: A, A, A.**
   - Notifications belong in `MeridianModule`. It is the authority, so
     every mutation notifies from there; a UI-side controller would
     only conceal bypasses.
   - One shared `PlaceAndTimePanel`, hosted by both the dialog and the
     companion, with the same presentation and actions.
   - One owned, modeless `JDialog`: a single instance, hidden on close
     and never rebuilt. It stays above its chart owner, not above
     unrelated applications.

   The stale-checkbox defect is a real 4.0 defect. Fixing it through
   the authoritative notification seam is part of the architecture,
   not unrelated scope.
2. **The three-PR split (option 2),** each merged and post-merge
   checked before the next begins:
   1. The module notification and the current dialog subscribing.
      Wide.
   2. The shared panel, the companion, its store, evidence and
      journeys, without application wiring. Interaction.
   3. View access, startup restoration and shutdown. Wide.

   Code is not rearranged merely to preserve those classifications.
   The owner's packaged checkpoint is on PR 3's candidate, before
   PR 3 merges.
3. **Title: Controls / Kontroller** for the first version. It is broad
   enough for later panels and quiet beside the chart. The Norwegian
   word may be reconsidered after using the packaged window.
4. **Visibility is restored at startup.** If the companion was visible
   at a clean quit it reopens; if it was hidden it stays hidden. In
   addition:
   - it is restored only after the chart window has valid screen
     geometry;
   - it is recovered onto an available display;
   - it does not take the chart's intended initial keyboard focus;
   - its visibility is remembered independently of the accepted
     Place and Time state.
5. **The refusal line appears in both presentations.** It belongs to
   the shared panel, so the dialog and the companion explain the same
   refused input. Silent put-back was poor behaviour, and explaining
   only in the companion would make the two presentations
   semantically different. This is an architecture-required
   correction, allowed under the freeze.

No other panel, menu redesign, version work or new functionality
enters the sprint.

## Amended on #455 (Sprint 42)

Two rulings the owner recorded on accepting Sprint 41's packaged
candidate, after using the companion beside a clean chart on an
external monitor:

3. **Title: JUranometria Controller / JUranometria Kontroller.** On
   the laptop screen it reads visually as an application of its own,
   so its title says what it is. View's item stays *Controls /
   Kontroller*.
4. **It starts closed at every launch.** Open, it tends to cover the
   chart a reader came for, and View is one gesture away. Whether it
   was open is no longer remembered (`companion.visible` is neither
   written nor read; a stale key is ignored); where it was, how large
   and which sections were collapsed still are, and it opens where it
   was left.
