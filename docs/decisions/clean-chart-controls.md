# A clean chart, with its controls kept at hand

Sprint 41, issue #449. **Ruled by the owner (2026-10-05)** — see
[The ruling](#the-ruling) at the end, which governs where it differs
from the proposal. The study is `docs/studies/clean-chart-controls/`
(`make clean-chart-study`); the real-window reproduction is quoted
below. #450 builds it in two pull requests; #451 hands it over.

> Hiding chart chrome does not disable the instrument.

> The chart remains the observing surface. Controls may live beside it,
> but the companion does not become another chart.

## What exists today

Measured on `main` at `a4b427a`.

**The bar.** `AtlasToolbar` is composed once, through `AtlasChrome`,
and sits north of the chart in the window's `BorderLayout`. The study's
inventory reads thirteen controls off the composed bar, in order:

| control | class | authority | elsewhere | remembered |
|---|---|---|---|---|
| Zoom in, Zoom out | action | `ChartViewController` | View, ⌘/Ctrl = and − | no |
| Lock zoom | state | `ZoomLock` | nowhere | `zoomLocked` |
| Fewer stars, More stars | action | `ChartViewController` | nowhere | no |
| Reset view | action | `ChartViewController.reset`, then the field's `clearSearch` | nowhere | no |
| Inspector | state | `InspectorToggle` (showing, available) | View, ⌘/Ctrl I | no |
| Accumulate | state | `SelectionMode` | the platform's modifier | no |
| Emphasis | state | `ChartComponent` (`toggleEmphasis`, `emphasizedSet`, `emphasisAvailable`, `onEmphasisChange`) | nowhere | no |
| Search | action with a local query | `LocalSearch`, `SearchNavigation`, the selection | nowhere | no |
| Field · Stars to V | readout | `ChartViewController` | the chart's title block | — |
| version | furniture | `AppInfo` | Help ▸ About | — |
| Exit | furniture | `AppShutdown` | the close box, the platform's Quit | — |

Every authority already notifies: `ChartViewController.onChange` (replays
the current state), `ZoomLock.onChange`, `InspectorToggle.onChange`
(replays), `SelectionMode.onChange` (replays, returns a remover) and
`ChartComponent.onEmphasisChange`. None but `SelectionMode` can be
released; the companion is built once and disposed at quit, so nothing
here needs releasing in this sprint.

**Two things live only in the bar.** Reset's clearing of the search
field, and the Emphasis menu, which `AtlasToolbar.emphasisMenu` builds
from the chart each time it opens. Search's query text and its result
list are the field's own; its outcome - navigation, selection - is
shared through the controller and the selection model.

**Responsive hiding.** The bar yields status text and never controls:
the version first, then the readout, each hidden whole. The Inspector
yields below a 640 px window. Nothing about the bar's own visibility
exists: no View item, no key, no preference.

**The window.** Packed to the chart's 900 × 700 and centred at every
start. Its size, position and maximised state are **not remembered**.

**Evidence.** `ToolbarSheetMain` photographs the bar in four states and
two languages; `MenuSheetMain` the menus; `CompanionSheetMain` the
companion with its two sections; the control-explanation audit walks a
real bar. No generator photographs the whole chart window.

### Reproduced, not inferred

The real application, started through `JUranometriaMain.start` over a
scratch node on this Mac (one display, 1512 × 982, menu bar 33 px):
the lock, Accumulate, the Inspector, two emphases, a search for M 31
and the companion were set, and the bar was hidden with `setVisible`
and a revalidate. Every line the bar's hiding changed, and nothing
else:

```
toolbar hidden:   chart.bounds 0,0 580x729 (was 0,29 580x700)   inspector.bounds 580,0 320x729 (was 580,29 320x700)
toolbar shown:    chart.bounds 0,29 580x700                     inspector.bounds 580,29 320x700
unchanged across both: frame.bounds 306,75 900x785 · view.centre 10.684792,41.269056 · field 8.0 · magnitude 8.0 ·
  target "M 31 · Andromeda Galaxy region" · projection GNOMONIC · emphasis [EQUATORIAL_GRID] · zoomLock true ·
  accumulate true · search.text "M 31" · inspectorItem true · companion.showing true · companion.bounds 846,75 360x814
```

The same, maximised (`extendedState` 6, frame 0,33 1512 × 868): the
chart went from 1192 × 783 to 1192 × 812 and back; every other line
unchanged. The same in macOS full screen (requested through
`com.apple.eawt`, `extendedState` **stays 0**, frame 0,33 1512 × 949):
896 → 925 and back; every other line unchanged. Three runs, identical.

**Hiding the bar is layout only.** The chart gains the bar's height -
29 px on this machine - and nothing it draws or remembers moves. The
headless arithmetic in the study agrees (900 × 700 → 900 × 729).

Two findings beyond the question asked:

1. **The Emphasis button cannot be reached by keyboard.** It is attached
   after the bar re-asserts its buttons' focusability, and the look and
   feel takes it away again. The inventory reads `focusable = no` for it
   alone. A defect in the shipped bar, found by reading the composed
   surface rather than the code.
2. **In macOS full screen the owned companion is not on the chart's
   Space.** Java reports it `showing` with its old bounds; the screen
   capture shows only the chart. Maximised, the companion stays above
   the chart as designed. Full screen is a different thing from
   maximised, and the application has never promised anything about it.

## Which controls belong in Controls

Three arrangements were drawn with the production controls lifted out of
a composed bar, at 360 px, in both languages and both appearances,
beneath the two sections the companion already holds.

| arrangement | alone | with both sections | with Chart Options collapsed | narrowest | keyboard stops |
|---|---:|---:|---:|---:|---:|
| 1. the toolbar, wrapped | 149 | 931 | 472 | 200 | 12 |
| 2. one **Chart controls** section, three rows and a readout | 147 | 929 | 470 | 240 (nb 232) | 11 |
| 3. Find, Navigate, Stars, Work with the chart | 259 | 1041 | 582 | 204 | 14 |

(Heights in px on this machine; stops count headings and controls.)

**Proposed: arrangement 2.** One section, titled *Chart controls* /
*Kartkontroller* (new keys), holding:

- row 1: Zoom in, Zoom out, Lock zoom · Fewer stars, More stars · Reset;
- row 2: Inspector · Accumulate · Emphasis;
- row 3: a search field, as wide as the section;
- a quiet readout line, *Field 8° · Stars to V 8.0*, in the version's
  subdued style.

Why: it is the bar's grammar, so a reader who knows the bar knows the
section; it is the shortest of the three; four headings for ten controls
(arrangement 3) cost three stops and 112 px for no grouping a reader
asked for; the wrapped bar (arrangement 1) breaks mid-group wherever the
width happens to fall.

**Decided, not assumed:**

- **The readout: yes.** It is chart state the section's own buttons
  change, and the title block that also says it is an option a reader
  may switch off. It is a label, not a stop.
- **The version: no.** Help ▸ About has it; it is the first thing the bar
  itself gives up when squeezed.
- **Exit: no.** The close box and the platform's Quit remain; a door
  beside the controls a reader works with on another display invites
  the one mistake nothing can undo. The bar keeps its own.
- **Introduced open**, like Place and Time; collapse remembered under
  `companion.collapsed.chartcontrols`. With all three sections open the
  companion scrolls on a 900-tall screen (929 px); with Chart Options
  collapsed it does not (470 px). That is the reader's choice, as #443
  ruled collapse to be.

## The shared-state contract

Two live presentations - the bar and the section - follow the same
authorities and never each other.

| state | authority | each presentation | shared or local |
|---|---|---|---|
| field, magnitude, their availability, the readout | `ChartViewController.onChange` | enables its own four buttons, writes its own readout and explanations | shared |
| Lock zoom | `ZoomLock` | asks `lock`, shows `onChange` | shared, remembered |
| Inspector | `InspectorToggle` | asks `toggle`, shows `onChange` (selected = showing, enabled = available, the three explanations) | shared |
| Accumulate | `SelectionMode` | asks `accumulate`, shows `onChange` | shared, session only |
| Emphasis | `ChartComponent` | opens the **one** menu builder, extracted from the bar (`EmphasisMenu`), which reads the chart when it opens | shared, transient |
| search | `LocalSearch`, `SearchNavigation`, the selection | its own `SearchField` over the same index, assembler, controller and selection | the **query text and result list are local** to the field typed into; the outcome is shared |
| Reset / Home | `ChartViewController.reset`, then clearing | one shared `home()` that resets the view and clears **every** search field | shared |

So two small seams are extracted from `AtlasToolbar`, not duplicated:
the Emphasis menu builder, and Home. A `ChartControls` class builds the
section's buttons over the authorities, in the pattern of
`ChartOptionsControls`; the bar keeps its own buttons and its own
responsive rule, unchanged, so its photographs stay byte-identical.

**What may stay local without misleading anyone:** the text a reader
typed, the result list beneath the field they typed into, the hover
text on each button, and which button has focus. A query typed into the
companion is not echoed into the bar: it is the reader's draft, not the
chart's state, and `Home` clears both.

## The toolbar's visibility

- **View ▸ Chart Toolbar** (*Kartverktøylinje*), a checked item beside
  Controls and Inspector, with a shortcut registered in `Shortcuts` so
  the tooltip can name it. Proposed: **⇧⌘T / Ctrl+Shift+T.** The macOS
  convention is ⌥⌘T (Finder, Safari: Hide Toolbar), but Ctrl+Alt+T opens
  a terminal on GNOME before any application sees it, and a shortcut a
  tooltip names must work where it is named. The owner may prefer the
  convention; the record states the trade.
- **Default visible; the choice remembered** - `chartToolbarShown` in a
  tenth startup store, `ChartChromeStore`, read before the frame packs
  so a hidden bar never flashes. The item's tick follows the bar's
  `componentShown`/`componentHidden`, as Controls' does.
- **Hiding the bar touches nothing else.** The companion's visibility,
  placement and sections are its own store's; the Inspector's state is
  the toggle's. A hidden bar keeps every subscription, so showing it
  again shows every change made meanwhile (reproduced above: state set,
  hidden, shown, identical).
- **Recovery.** The menu bar is never hidden, so View ▸ Chart Toolbar and
  View ▸ Controls are always one gesture away, and the shortcut works
  while the chart window has focus. With both the bar and the companion
  hidden the chart is still the chart: wheel, keys, View's zoom items and
  the chart keyboard all work.
- **Startup, restart, language.** Restored with the other stores;
  surviving a clean quit like the companion's open state; the item's
  words from the session language like every other item. The bar's
  responsive rule is unchanged; a bar shown again at a narrow width
  yields its status text as before.
- **Focus and accessibility.** If a bar control has focus when the bar
  hides, focus goes to the chart (`requestFocusInWindow`), never
  nowhere. The item explains what it hides and where the controls
  remain. Screen-reader order in the section: heading, row 1, row 2,
  the field; the readout is spoken as text.
- **No chart-state mutation.** A test holds the probe's invariants: view
  state, emphasis, lock, Accumulate, Inspector, search text and the
  companion's bounds equal across hide and show, packed and maximised.

## Maximised, full screen, two displays, and a monitor that goes away

**Proved here, on one display.** Maximised is `extendedState` 6 with the
owned companion above the chart. macOS full screen is a Space:
`extendedState` stays 0, the frame takes the screen, and the owned
companion is not shown on that Space. Hiding and showing the bar in
either changes the chart's height by 29 px and nothing else.

**Arithmetic the suite already holds**, run over a laptop display and a
2560 × 1440 monitor to its right (the study's placement image):

| case | the companion |
|---|---|
| chart maximised on the monitor, companion remembered on the laptop | stays on the laptop, where it was |
| the same, companion remembered beside the chart on the monitor | stays on the monitor |
| the monitor disconnected, companion last seen on it | falls back over the chart's trailing edge on the laptop |
| the monitor reconnected, companion last seen on the laptop | stays on the laptop |

Placement runs once per session; mid-session, the desktop moves windows
off a vanished display itself, and the companion remembers wherever it
lands.

**Proposed contract.**

- The clean-chart journey is a **maximised** chart window on the
  monitor, Controls on the laptop. The application makes no promise
  about macOS full screen and adds no full-screen command (out of
  scope); the record states what was measured so nobody is surprised
  by a companion left on the desktop Space.
- **One scope question for the owner.** The chart window's bounds and
  maximised state are not remembered, so every session begins with a
  packed window centred on the main display, to be dragged to the
  monitor and maximised again. Remembering them - the same
  on-a-screen-that-exists check `CompanionPlacement` makes - is window
  chrome, not chart behaviour, and is what makes the two-display journey
  repeatable. Recommended for #450; the owner rules whether it is
  inside "control presentation only".

**Needs the owner's packaged two-display journey**, because no
automated run here has two displays: the companion staying on the
laptop while the chart is maximised on the monitor; moving each window
between displays; unplugging and replugging the monitor with both open;
and, if the owner cares to see it, full screen with "Displays have
separate Spaces" on.

## CI route, measured

With the classifier on `a4b427a`, over the paths #450 would touch:

| path | route | why |
|---|---|---|
| `app/JUranometriaMain.java` | **wide** | by ruling (#427, I1) |
| `app/AppMenuBar.java` | **wide** | `FurnitureStudyMain → PlatformEvidence → ChartKeys → AppMenuBar` |
| `ui/Shortcuts.java` | **wide** | `FurnitureStudyMain → PlatformEvidence → Shortcuts` |
| `ui/AtlasToolbar.java`, `ui/SearchField.java`, `app/AtlasChrome.java`, `app/StartupStores.java`, `ui/companion/*`, the photographers, `PackagedAcceptanceMain` | interaction | reached only by the interface |
| new `toolbar.*`, `menu.toolbar.*`, `chartcontrols.*` keys; ledger rows | interaction | interface-only keys, on interface sources |

`PlatformEvidence` is the chart producers' platform record, and it
spells the chart keyboard's prefix through `ChartKeys` and `Shortcuts`.
That is a true dependency: a chart record that names a keystroke must
spell it as the application does. **#450 is wide**, and no boundary is
rearranged to make it otherwise. This study's own pull request is wide
for the `Makefile` and the contract's registries, as #442's was.

## Evidence #450 moves, and what must not

- **Moves:** the companion photographs (`companion-*`), gaining the
  third section; the menu photographs (`menu-*`), gaining the View
  item; the control-explanation audit; the test-evidence report; ledger
  rows (`chartToolbarShown`, `toolbarItem`,
  `companion.collapsed.chartcontrols`, `heading.chartcontrols`).
- **Must stay byte-identical:** the toolbar photographs (`toolbar-*`),
  because the bar is preserved; every renderer-drawn chart, reference,
  gallery and sheet byte and every chart provenance row; the Chart
  Options and Place and Time photographs.
- **A packaged journey,** `clean chart OK`: the bar hidden through the
  View item and shown through the shortcut, view state and every switch
  equal before and after; a change made in the section while the bar is
  hidden shown by the bar when it returns; Home from the section
  clearing the bar's field; the choice round-tripping a restart; the
  Emphasis menu from the section reading the chart.

## Implementation split

**Two pull requests, both wide** (recommended):

1. **The Chart controls section.** The two seams extracted
   (`EmphasisMenu`, Home), `ChartControls` over the authorities, the
   section in the companion, its photographs and tests. The bar's
   photographs proved byte-identical first, as #443 did.
2. **The bar's visibility.** The store, the View item, the shortcut,
   restore at startup, the focus rule, the invariance test, the journey,
   and - if ruled in - the chart window's remembered bounds. **The
   owner's packaged two-display checkpoint is on this one, before it
   merges.**

**One pull request** would be fewer gates but would put the owner's
checkpoint behind the whole change.

## For the ruling

1. Arrangement 2, one *Chart controls* section, three rows and a
   readout; introduced open.
2. The readout in; the version and Exit out.
3. Search: a second field, local query, shared outcome; Home clears
   every field.
4. The shortcut: ⇧⌘T / Ctrl+Shift+T, or the macOS convention ⌥⌘T with
   the GNOME conflict accepted.
5. Whether #450 remembers the chart window's bounds and maximised
   state.
6. Whether #450 fixes the Emphasis button's focusability while it
   touches the bar's neighbours (a defect, not a feature), or a
   separate issue does.
7. The two-pull-request split, with the two-display checkpoint on the
   second.

## The ruling

The owner's rulings on the seven points, recorded as given:

1. **One compact *Chart controls* section, introduced open.** The two
   compact rows, the search field and the quiet readout form one
   coherent instrument; four sections make the companion unnecessarily
   tall and fragmented.
2. **Keep the quiet readout**: field size and stellar magnitude limit
   belong with navigation. **Exclude the version and Exit**; they are
   application furniture, not chart controls.
3. **Search text stays local; its effect is shared.** Typing and the
   results popup belong to the field being used. Choosing a result moves
   the shared chart. Using one field must not overwrite unfinished text
   in the other. Home/reset clears the shared target and every search
   field. **The Home and Emphasis actions are extracted once**, so the
   toolbar, the menu, the shortcut and the companion cannot diverge.
4. **Shift+⌘T on macOS and Ctrl+Shift+T elsewhere** for toolbar
   visibility; the alternative with a known desktop conflict is avoided.
   The menu item shows the current state, toolbar visibility is
   remembered, and the default remains visible.
5. **Chart-window placement is in #450**, because otherwise the intended
   two-display setup must be rebuilt every launch: remember the last
   ordinary bounds and maximised state; keep the last normal bounds even
   while maximised; validate them against the displays present and
   recover safely if a monitor disappears; **do not remember native
   macOS full-screen state.** The supported observatory arrangement is a
   maximised clean chart on the external monitor with Controls on the
   laptop. Native macOS full screen is recorded as a known limitation;
   window ownership is not weakened and no always-on-top trick is added
   to work around Spaces.
6. **Emphasis keyboard focus is fixed in this sprint.** It is directly
   exposed by duplicating the chart controls and belongs to their
   accessibility contract; keyboard access is proved in both the
   existing toolbar and the new companion controls.
7. **Two wide pull requests.** PR 1: the shared chart-control actions,
   the compact companion section, the search semantics, the quiet
   readout and the Emphasis accessibility repair - no toolbar hiding
   yet. PR 2: toolbar visibility, the menu item, the shortcut, the
   remembered toolbar state, safe chart-window placement and
   maximisation, and the packaged two-display owner checkpoint.

**The final packaged journey tests the principle itself:** the chart can
become a clean sheet on the large monitor while every useful toolbar
operation remains available in Controls on the laptop; the chart must
remain fully usable with its toolbar hidden; and disconnecting the
external monitor must recover both windows safely.
