# The Sun and Moon tables in the JUranometria Controller

Sprint 42, issue #457. **Ruled by the owner (2026-10-06)** — see
[The ruling](#the-ruling) at the end, which governs where it differs
from the proposal. The study is `docs/studies/solar-system-controls/`
(`make solar-system-controls-study`). #458 builds it in two pull
requests; #459 hands it over.

> The Controller is the coherent home for the Solar System tools; the
> dialogs remain, and the two never drift apart.

## What exists today

Measured on `main` at `d134f9fb`.

**Two dialogs, one shell.** View ▸ Sun… and View ▸ Moon… open
`SolarTableDialog`, one modeless instance per body, disposed on close
and rebuilt on reopening. Each holds a `Content` panel: the observer
note, two views (*This instant*, *Over a range*), the range's start,
end and step, *Compute*, a status line, the table (Sun 8 columns at a
760 px viewport; Moon 11 at 1465 px), a time note, *Update from Place
and Time* and *Close*. The content computes when it is built, when the
dialog comes to the front, and on *Compute*, *Update* or Enter in a
range field — synchronously, on the event thread. **Nothing is
persisted** (ruled on #400), and **nothing is pushed**: the observer is
pulled from Place and Time on demand, and a dialog left open shows a
stale table until it is asked (reproduced in the study).

**Two switches, no seam.** View ▸ Sun on the chart and Moon on the
chart flip flags on `SolarSystemModule` through `SunChartSession` and
`MoonChartSession`, remembered as `sunOnChartShown` and
`moonOnChartShown`. The module notifies nobody; the menu tick is the
item's own. Toggling opens no table; opening a table toggles nothing.

**The menu** is flat: Chart Options…, Place and Time…, Controls, Chart
Toolbar, Sun…, Moon…, Inspector, Ecliptic, Sun on the chart, Moon on
the chart, —, Zoom In, Zoom Out. No submenu exists anywhere, and
`MenuMnemonicTest` checks each menu's direct items only.

**The Controller** holds Chart controls, Place and Time and Chart
Options: 959 px with a fourth section collapsed — it already scrolls
on a 900-tall screen.

## The layout, measured

At 360 px, both languages, light and dark, with the production
content's controls lifted into each arrangement:

| arrangement | height |
|---|---:|
| a Sun group, the instant row as a **card** of heading-and-value lines | 409 |
| a Sun group, the instant row as the **table**, scrolling sideways | 318 |
| a Moon group, card / table | 460 / 318 |
| Solar System with both groups open, cards | 899 |
| the Controller: Solar System collapsed / Sun open / both open | 959 / 1398 / 1828 |
| a table of 1 / 4 / 8 / 25 / 169 rows (header 25 + 20 per row) | 45 / 105 / 185 / 525 / 3405 |

(This machine's pixels; `platform.md`.)

**Proposed presentation of a group:** the observer note wrapped; the
two views on one row; *From* and *To* on rows of their own, the fields
as wide as the group; *Every* with its step and *Compute*; the status
line; the result; the body's *Show on chart* box; *Update from Place
and Time*. No *Close* — a group is not a window. The result is the
**card** in the instant view (every column, one line each; nothing the
dialog shows is lost) and the **table scrolling sideways** in the range
view, as tall as its rows up to a cap of eight, then scrolling down
inside the group. The dialogs keep their layout.

## The contract, proposed

Answers to the eight questions, for the owner to rule on.

1. **Introduced:** *Solar System* **collapsed**; inside it, **Sun open,
   Moon collapsed** — the #443 pattern. All three remember their own
   collapse under `companion.collapsed.solarsystem`, `….sun`, `….moon`.
   Open, both groups add 899 px to a Controller that already scrolls;
   a reader who wants both open leaves them so.
2. **In the Controller at laptop width:** every control of the dialog
   except *Close*, plus the body's *Show on chart* box; every column,
   as the card or the table. Nothing the legacy dialog offers is lost.
3. **Query inputs are local drafts; the applied query and its result
   are shared.** Each host keeps its own *This instant / Over a range*,
   *From*, *To* and *Every* as a draft, seeded from the last applied
   query when the host is built. *Compute* and *Update* apply the
   host's draft to one **`SolarTableSession` per body** — the
   authority for the applied query, its rows and its status — and
   every host shows the session through one subscription. So the
   dialog and the Controller agree after a calculation in either (a
   proof #458 requires), while typing in one never overwrites typing in
   the other. Nothing of it is persisted (ruled on #400).
4. **Opening or expanding reveals the last result and calculates
   nothing** — the Controller, its section, its groups. The dialog
   keeps its ruled behaviour (#400): coming to the front is an
   *Update*, which — being an Update — both hosts then show. If the
   owner prefers the dialog, too, to reveal rather than recompute on
   coming to the front, that is a one-line change and a changed
   ruling.
5. **Synchronisation.** *Update from Place and Time* re-reads the
   observer and recomputes the applied query — in the session, shown
   by both hosts. The current instant is the observer's. A range
   computation applies the host's draft. Chart visibility is **one
   shared switch per body**, followed by the menu tick, the group's box
   and the chart. Two places for its notification:
   - **A (recommended):** `SolarSystemModule` gains an `onChange`,
     because it is the authority and a mutation that bypassed a UI
     switch would otherwise leave a tick stale (#433's reasoning). The
     module is chart-reached, so the pull request carrying it is
     **wide**.
   - **B:** a `BodyOnChart` switch in `ui/solar` that the session
     drives the module from; interaction route, but a direct
     `module.sunShowing(…)` would bypass it.
6. **Yes:** each group carries its own *Show on chart*, sharing the
   body's switch. Sun and Moon stay independent.
7. **View ▸ Solar System** contains, in order: Sun on the chart, Moon
   on the chart, a separator, Sun…, Moon…; the four flat items go. The
   saved `sunOnChartShown` / `moonOnChartShown` keys are untouched and
   survive the move. `MenuMnemonicTest` recurses into the submenu.
8. **Persisted:** the three collapses (presentation, the companion's
   store). **Not persisted:** drafts, applied queries, results, scroll
   and focus positions (ruled on #400: a stored range is a clock in
   disguise).

**Access letters.** The study measured every dialog letter against the
Controller: all but *V/I* collide with Chart Options or Place and Time
controls already in that window, and Sun's letters are Moon's. The
Controller's groups therefore carry **no access letters** of their own;
the dialogs keep theirs, each in its own window.

## Architecture

- **`SolarTableControls`** (one class, built per body like
  `SolarTable`): the draft controls, the result views, the status, the
  *Show on chart* box, the subscription to the session and the shared
  switch. The dialog's `Content` becomes its first host (the dialog's
  layout, byte-identical photographs); the Controller's group its
  second — the `ChartControls` / `ChartOptionsControls` pattern.
- **`SolarTableSession`** per body: the applied query, its rows and
  status; `apply(query)`, `update()` (re-read the observer), `onChange`
  with a `Subscription`. The computation stays `SolarSystemService`'s
  and stays synchronous and on demand.
- **The visibility switch** per 5A or 5B.
- No change to any astronomical quantity, column, format, mark,
  export or sheet.

## Invariants #458 holds

- Collapsing the section or a group changes no astronomical or chart
  state and tells no session anything.
- Opening the Controller, the section or a group computes nothing.
- The dialogs remain, calculate as before, and agree with the
  Controller after a calculation in either host.
- Chart visibility is one choice per body; opening a table never
  toggles it; toggling never opens or computes a table.
- Sun and Moon are independent: a Sun range computes no Moon rows.
- Keyboard traversal reaches every control and every submenu item in
  both languages; no access letter collides within the Controller.
- `sunOnChartShown` and `moonOnChartShown` survive the menu move.
- No ephemeris, table value, chart geometry, export, sheet or released
  chart byte changes; the Sun and Moon table photographs reproduce byte
  for byte unless the ruled layout changes them (it does not).

## CI route, measured

With the classifier on `d134f9fb`, over the paths #458 would touch:

| path | route | why |
|---|---|---|
| `ui/solar/SolarTableDialog.java`, `SolarTableModel.java`, the sessions and stores, the photographers, `PackagedAcceptanceMain`, `AtlasChrome`, `StartupStores`, `ui/companion/*` | interaction | reached only by the interface |
| `app/JUranometriaMain.java` | **wide** | by ruling (#427, I1) |
| `app/AppMenuBar.java` | **wide** | `FurnitureStudyMain → PlatformEvidence → ChartKeys → AppMenuBar` |
| `solarchart/SolarSystemModule.java` | **wide** | `SunOnTheChartStudyMain` constructs it (5A) |
| `ui/solar/SolarTable.java`, `SolarTableWords.java` | **wide** | `MoonTableStudyMain` reaches them — **not touched** |
| `solartable.*`, `suntable.*`, `moontable.*` keys | wide | chart producers resolve their stems — **not touched**; new keys are `menu.*` and `solarsystem.*`, interface-only |

This study's own pull request is wide for the `Makefile` and the
contract's registries, as every study's has been.

## Implementation split

**Two pull requests** (recommended):

1. **The shared controls, without application wiring** — interaction
   route, if the boundary holds as measured: `SolarTableSession`,
   `SolarTableControls` for both bodies, the dialog as first host
   (photographs byte-identical, proved before the second host is
   built), the Controller group as second host, the Solar System
   section built and photographed through `CompanionSheetMain`, the
   tests. No `JUranometriaMain`, no `AppMenuBar`, no module change.
   **This would be the interaction route's first real run.**
2. **The wiring and the menu** — wide: the section in
   `JUranometriaMain`, View ▸ Solar System, the visibility seam (5A or
   5B), the packaged journeys, and the owner's packaged checkpoint
   before it merges.

The issue's three-step sequence (Sun, Moon, hierarchy) is folded into
two because one class serves both bodies, and splitting Sun from Moon
would prove the same seam twice.

## Evidence #458 moves, and what must not

- **Moves:** the companion photographs (a fourth section) and strings;
  the menu photographs (the submenu); the control-explanation audit;
  the toggle-shortcut study (the menu's bindings); the test-evidence
  report; ledger rows for new ids and keys; provenance.
- **Must stay byte-identical:** the Sun and Moon table photographs
  (`suntable-*`, `moontable-*`); `solar-system/measurements.md` and
  `moon-measurements.md`; every renderer-drawn Sun or Moon picture;
  every chart, reference, gallery and sheet byte.
- **Packaged journeys:** `solar system controls OK` — one session per
  body followed by two hosts; a draft applied in either shown by both;
  opening computes nothing; the switch shared by the menu, the box and
  the chart; the keys surviving; the dialogs unchanged.

## For the rulings

The eight contract answers above, and:

9. 5A (the module notifies; wide) or 5B (a switch beside it;
   interaction).
10. The two-pull-request split, with PR 1 attempting the interaction
    route and PR 2 carrying the checkpoint.

## The ruling

The owner's rulings, recorded as given:

1. **Collapse defaults.** Solar System is introduced collapsed. On its
   first expansion, Sun is open and Moon is collapsed. All three
   collapse choices are remembered independently.
2. **Controller contents.** Every useful Sun/Moon control except
   Close, plus each body's Show on chart switch. Every result column:
   a single instant as a key/value card; a range as the complete,
   horizontally scrolling table.
3. **State semantics.** Editor drafts remain local to each host.
   Applying a query creates one shared applied query and result per
   body through `SolarTableSession`; both hosts follow that result.
   Receiving a shared result must not overwrite unfinished local
   draft text.
4. **Calculation.** Opening the Controller or expanding Solar System,
   Sun or Moon never calculates. The legacy dialogs' front-pull
   behaviour is preserved for compatibility: opening a dialog performs
   its current Update, and that applied result then becomes visible in
   the Controller.
5. **Visibility authority: 5A.** `SolarSystemModule` remains the single
   authority and gains notification. This is legitimately wide and
   cannot be bypassed by another control surface.
6. **Chart switches.** Each body group contains its own synchronised
   Show on chart choice.
7. **Menu.** View → Solar System, in this order: Sun on the chart,
   Moon on the chart, separator, Sun…, Moon….
8. **Persistence.** Collapse state only. No drafts, table scroll,
   focus, selection or popup state.
9. **Controller ordering:** Chart controls, Place and Time, Solar
   System, Chart Options. A newly introduced Solar System header is
   not hidden below Chart Options on a 900 px laptop display.
10. **Keyboard.** No access-letter mnemonics inside the Controller
    groups, since they collide; preserved in the legacy dialogs.
    Complete Tab traversal, label association, accessible names and
    submenu keyboard navigation are required.
11. **Implementation split: two pull requests.** First, the shared
    Sun/Moon control components, sessions and contracts without
    production wiring, the classifier deciding whether it becomes the
    first real interaction-route pull request. Second, the Controller
    wiring, the authoritative visibility notification, the grouped
    View submenu and the packaged owner checkpoint; wide.
