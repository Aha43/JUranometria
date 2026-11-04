# Guide the chart and explain the selection

*Issue #690, the measured study of road #689 toward 6.3.0. Written on
main `651f5347` (v6.2.0) in `study/690-guide-and-explain`. It changes no
production behaviour: it inventories what the atlas does today, measures
the proposed pointer modes through the production pan solver, draws the
proposed box on production pages and on paper, and stops for the owner's
rulings. The measurements are `docs/studies/guide-and-explain/
measurements.md` (portable) and `platform.md` (this desktop's fonts);
regenerate with `make guide-and-explain-study`.*

The road's promise: **guide the chart precisely, then keep the result
understandable on the page.** Two features serve it. A reader can
constrain a pointer drag deliberately. The chart itself can say what the
last selected object is, in a small printable box, without the Inspector.

## 1. What the atlas does today

### 1.1 Pointer dragging

- **One drag path for every page.** `PanInteraction` turns a left-button
  press on sky into a grab; once the pointer has moved at least 4 px
  (`DRAG_THRESHOLD_PX`, shared with selection and furniture clicks) every
  drag event asks `ChartViewController.pan` to put the grabbed sky under
  the pointer, solved exactly by `PanSolver.solveCentre`. The ordinary
  gnomonic pages (1°-42°), the stereographic overview (60°-120°) and the
  orthographic globe (180°) all pan through it; the projection is a
  property of the field, and there is no projection menu (measurements
  §3).
- **Near the poles** the solver holds an infeasible horizontal component
  at its feasibility boundary and refuses a pan past a pole rather than
  wrapping it. Nothing in this road changes that.
- **Modifiers.** The drag ignores every modifier: a Command/Ctrl-drag
  pans. On a click, the menu modifier adds to the selection. Shift, Alt
  (Option), the secondary button and wheel modifiers have no meaning on
  the chart (measurements §2).
- **The wheel and two-finger trackpad scroll zoom; they never pan**
  (`ZoomInteraction`). There are no gesture or magnification listeners.
- **There is no keyboard panning at all.** The chart keyboard (Menu+K,
  then a letter) toggles layers and runs one command; it moves nothing.
  So "pointer dragging, not keyboard panning" leaves nothing to reconcile.
- **Lock zoom** (`ZoomLock`, persisted as `zoomLocked`) stops the wheel
  and trackpad only; the zoom buttons, the menu, the keys and search still
  zoom, and dragging still pans (`ZoomLockTest.lockedPanningStillPans`).
  Its one toggle stands on the toolbar and in the Controller's Chart
  controls strip, in no menu.

### 1.2 Selection and what the atlas says about it

- **What is selected.** `SelectionModel` answers one selection for the
  Inspector: a star, a deep-sky object, empty sky, or a Solar System
  occurrence (Sun, Moon, Mercury … Neptune; planetary moons are drawn but
  not selectable). There is no constellation selection. The
  `WorkingSelection` is the ordered set Accumulate builds, with one
  **lead**: the newest member added, or the last remaining member when the
  lead is removed. `LeadSelection` feeds the lead to the model. **"The last
  selected object" is the working selection's lead.**
- **What is said about it.** The Inspector builds its facts inside
  `InspectorPanel`: a star's best name, Bayer letter, Flamsteed number,
  catalogue id, V magnitude and J2000 coordinates; a deep-sky object's
  Messier name or id, aliases, type, magnitude with its band, size and
  coordinates; a body's name, the instant and observer it was drawn for,
  and coordinates. Three gaps matter here: the deep-sky **type line is
  English only** (`readableType`, "type not classified", "H II region");
  the star's Bayer and Flamsteed lines carry **no constellation** ("α", not
  "α Ori"); and the fact builders are package-private, so nothing outside
  the Inspector can reuse them.
- **The mark.** A selected star or deep-sky object the page draws gets the
  ring (`drawSelectionHighlight`); one the page holds but does not draw
  (hidden by an option, too faint, too small) gets the working cross. **A
  selected body gets no mark at all**: the ring matches catalogue marks
  only, and a body is always "drawn", so it never gets a cross. Off the
  page there is no ink. Selection ink takes no part in label placement, so
  a refused label never refuses a mark.
- **The chart's spoken description does not mention the selection.**
- **Chart time.** A paint holds the one chart context; the bodies are
  recomputed from it on each pull, and the page's `drawnBodies` are what
  the inventory and the Inspector read. There is no stored per-paint body
  snapshot, but everything a box needs is already in the page inventory
  and the drawn placements of the same paint.

### 1.3 Chart furniture, clean chart and paper

- **Furniture** is typed text a module offers through
  `ChartServices.furnish`: an identity, a heading, lines, at most one
  action, a spoken sentence and a sheet sentence; no colour, rectangle or
  Graphics. The renderer sizes, places, inks, reserves and speaks it.
  Lines never wrap; a box no corner can hold is refused. Place and Time's
  box is the only module furniture today.
- **Corners** follow ruling 3 on #556: the lower right, then the upper
  right while the magnitude key does not hold it, then the upper left; the
  title block keeps the lower left. Labels and grid figures yield to a box.
- **Clean chart** hides the toolbar and nothing else
  (`docs/decisions/clean-chart-controls.md`: it "touches nothing else").
  The title block and Place and Time's box stay on a clean chart.
- **Paper.** Module furniture travels to SVG, PDF and PNG on its own
  (`paperFurniture`, no control on paper). The selection's marks travel
  only when the export asks for the working selection, which is off by
  default.

## 2. Pointer navigation, measured

The four modes and the modifier are prototyped in
`tool/PointerDragSketch` and run through the production solver along two
recorded gestures - one mostly across, one mostly up, each with a ±12 px
hand wobble - on five pages (measurements §1). A constraint changes only
**which pixel the grabbed sky is asked to stand under**: the pointer's
column on the press row (horizontal only), its row on the press column
(vertical only), or none (locked). What the measurements settle:

- **The constraint is exact on the screen.** The grabbed sky ends exactly
  on the asked-for pixel on every page and in every mode (0 px off
  target), and the hand's wobble disappears.
- **It is a screen statement, not a sky statement.** During a
  horizontal-only drag, a star 200 px above the press moves up or down the
  page by -0.1 px on the 8° page at M 42, 0.2 px at +60°, 13.0 px at +85°,
  2.6 px on the 60° overview and 55.2 px on the globe. The page's north-up
  grid is not parallel to the screen's rows away from the centre row, so a
  horizontal-only drag keeps a screen row, never a parallel of
  declination, the equator or the ecliptic. The words must say so.
- **Nothing new happens at the poles.** No waypoint of any mode met the
  solver's pole hold or refusal on these gestures; where it does, the
  production rule applies unchanged under every mode.
- **Locked never pans** and leaves the centre exactly where it was.
- **The modifier** decides its axis once, from the larger component of
  the first movement of at least 4 px, a tie going across; the mostly-across
  gesture became horizontal and the mostly-up gesture vertical on every
  page, and each then matched the corresponding mode exactly.

## 3. The selected-object box, drawn

The box is offered to production pages through the chart's own furniture
seam by a study-only module (measurements §5). The corner, ink, label
yielding, speech and paper are production's; the words are the study's.

- **Every family the selection model admits** is drawn: a named star
  (Betelgeuse), a star known only by its catalogue id, a deep-sky object
  (M 42), the Moon and Venus at chart time, and a body below the drawn
  horizon (the Moon at the cited instant, with the parenthesised state the
  page already uses). There is no constellation selection, so no
  constellation box.
- **A refused label** never suppresses the box: on the Pleiades' 2° page
  the page refuses M 45's own label and the box still names it. (Star
  labels are almost never refused on the tried pages; deep-sky labels
  often are.)
- **Off the page and back.** With M 42 selected and the page panned to RA
  120°, the box and the ring are gone; panned back, both return.
- **Placement under today's rule** puts the box in the lower right on
  ordinary pages, the upper right on a 520 px window (the lower right meets
  the title block), and the upper left only when the magnitude key holds
  the upper right. On the 600 × 1900 letterboxed page the English box
  stands in the lower right and the Norwegian in the upper right: the
  Norwegian page's lower right does not clear its other furniture.
- **Paper.** Exported to A4 with the working selection, the sheet carries
  the box and the ring; the SVG holds the heading as text. Exported as
  today's default (no working selection), the box travels but the ring
  does not - 1 745 pixels apart, all of them the mark.
- **Two sketches** show what production cannot do yet: the box preferring
  the upper left, and a selection ring around a selected body.

## 4. Proposal: pointer navigation

- **Modes.** Free, Horizontal only, Vertical only and Locked, mutually
  exclusive. Free is today's drag exactly. Horizontal and vertical name the
  screen. Locked lets no drag pan; a click still selects, the wheel and the
  zoom controls still zoom, and over sky the cursor is the ordinary arrow
  rather than the open hand.
- **Modifier.** Shift, held at the press of a free drag, keeps that one
  drag to its dominant axis, decided once as measured. It is read at the
  press only, so pressing or releasing Shift mid-drag changes nothing and
  nothing is left behind; in the constrained modes and in Locked it does
  nothing. A Shift-click is still a click.
- **Scope.** Every page and projection - the ordinary chart, the overview
  and the globe - because they share the one drag path; mouse and trackpad
  press-and-drag alike. The wheel and two-finger scroll keep zooming and
  are untouched. Keyboard panning does not exist and is not added.
- **Startup and persistence.** Free at every launch, never stored. Lock
  zoom keeps its key, default and semantics. The section's open or closed
  state is remembered by the Controller as every section's is.
- **Surface.** A collapsible **Pointer navigation** section in Controls →
  Chart, after Chart controls, open the first time: four radio choices and
  one sentence (sketched in both languages and themes). No toolbar button
  and no menu item.
- **Reporting.** A constrained or locked mode is visible in its section,
  added to the chart's spoken instructions, and written in the view report
  as a `pointer-drag` line (`horizontal`, `vertical`, `locked`); Free adds
  nothing, so the released page and report are unchanged. The proposed
  sentences, the Norwegian for the owner's review:

  | mode | English | Norwegian |
  |---|---|---|
  | horizontal | Dragging keeps to horizontal movement on the screen. | Når du drar, holder bevegelsen seg vannrett på skjermen. |
  | vertical | Dragging keeps to vertical movement on the screen. | Når du drar, holder bevegelsen seg loddrett på skjermen. |
  | locked | Dragging does not move the chart. | Å dra flytter ikke kartet. |
- **Shortcuts.** None in 6.3.0. The chart keyboard's free letters are A, Q,
  V, X and Z, and no global shortcut is needed for a choice made rarely;
  the modifier serves the quick case.

## 5. Proposal: the selected-object box

- **Name.** *Selected object* / *Valgt objekt*: the Chart Options box
  "Selected object box" in the Chart tab beside the title block and the
  magnitude key, and in the Controller's Chart Options section. On by
  default, remembered like the ecliptic's switch in a store of its own
  (`selectedObjectBox`), so `ChartOptions` and every sheet's canonical
  options stay unchanged.
- **Subject.** The working selection's lead - the last selected object.
  The accumulated set stays the Inspector's.
- **When it shows.** Exactly while the lead is on the current page: drawn,
  or marked with the working cross because an option, the limit or the
  field keeps it from being drawn. Off the page, no box; back on the page,
  the box returns. Empty sky never enters the working selection, so it
  never gets a box: a plain click on empty sky clears the selection and
  the box with it. An *additive* click on empty sky (Accumulate, or the
  menu modifier) leaves the working selection and its lead as they were,
  so the box keeps describing the lead while the Inspector answers
  *Empty sky*; the proposal keeps that, the box following the lead. An
  untouched chart has no selection, so it is the released page.
- **Facts.** At most a heading and three lines, plus one state line for a
  body; no second calculation - every fact is read from the page inventory
  and the drawn placements of the same paint:
  - a star: its best name (name, Bayer letter with constellation,
    Flamsteed number with constellation, catalogue id); its other
    designation and catalogue id; V magnitude; J2000 coordinates;
  - a deep-sky object: its Messier name or id; id and its symbol family in
    the singular (*nebula*, *tåke*); magnitude with its band; coordinates;
  - a body: its page name; *for 2026-08-15 06:00 UTC*; coordinates; and
    *(below the horizon)* when the page dims it.
  No size, phase, distance or altitude in version one.
- **Mark.** The ordinary ring or cross, as today. **New: a selected body
  gets the ring** in the selection ink around its drawn disc or symbol, so
  box and mark always appear together.
- **Placement.** This box prefers the **upper left**, then the upper right
  while the key does not hold it, then the lower right; every other box
  keeps ruling 3's order and is placed first. Lines never wrap; a box no
  corner can hold is refused, as all furniture is. This amends ruling 3 on
  #556 for this one box.
- **Clean chart** hides the box with the toolbar and restores it with the
  toolbar, the selection and the setting untouched - the first page
  furniture the clean-chart switch reaches, amending its "touches nothing
  else".
- **Paper.** While the box is on the page, the lead's mark travels to the
  sheet with it, whatever the working-selection export choice says; the
  rest of the working selection still travels only when asked.
- **Speech.** The page's description gains the box's sentence
  (*Selected: Betelgeuse, α Ori, …*) through the furniture path, as Place
  and Time's does. The box has no control, so it takes no focus.
- **The seam.** One typed value, `SelectedObjectFacts`, sealed over star,
  deep-sky object and body, built by one shared fact source that the
  Inspector also uses. The box accepts no free text, and no module can add
  a line. Not promised by this seam: measurement readouts, meteor
  radiants, projection notes, user notes, or a general information box.
- **Words.** New keys for the compact magnitude (*V {0}*, *B {0}*), the
  five families in the singular, *for {0} UTC* and *Selected: {0}*; every
  other word is the Inspector's or the page's. The Inspector's English-only
  type line and its bare Bayer letter are recorded, not fixed, here.

## 6. Integration map and conflicts

| area | touched by | conflict |
|---|---|---|
| `PanInteraction`, `ChartViewController.pan` | #691 | none; the target pixel is the only change |
| Controller Chart tab, `CompanionTabs` | #691 | the new section after Chart controls |
| `PageText.chartInstructions`, `ViewReport` | #691 | new sentence and report line only when not Free |
| `InspectorPanel` fact builders | #692 | extracted into the shared fact source; the Inspector's output must stay byte-identical |
| `ChartRenderer.furnitureLayout`, `ChartFurniture.Corner` | #692 | a per-box corner preference; ruling 3's order for every other box |
| `ChartRenderer.drawSelectionHighlight`, `WorkingCrossInk` | #692 | the body ring |
| `ChartChromeStore`, clean chart | #692 | the box follows the toolbar's visibility |
| `ExportSheetSession`, `SheetInk.working` | #692 | the lead's mark travels with the box |
| `ChartOptionsControls`, a new store | #692 | one box beside the title block and key |
| interface-language files | #691, #692 | keys appended at the end of both files |

## 7. Choices for the owner

- **C-1 Modes.** Free, Horizontal only, Vertical only and Locked, as in
  §4. Recommended.
- **C-2 Modifier.** Shift, read at the press of a free drag, the axis
  decided once from the first 4 px. Recommended. Alternative: Alt
  (Option), or no modifier.
- **C-3 Scope.** Every projection and field, mouse and trackpad
  press-drag; wheel and scroll untouched; no keyboard panning added.
  Recommended.
- **C-4 Startup.** Free at every launch, not stored; Lock zoom unchanged.
  Recommended.
- **C-5 Surface and reporting.** The Controller section after Chart
  controls; a spoken sentence and a view-report line when not Free; no
  toolbar button, menu item or shortcut. Recommended.
- **C-6 Pointer words** in both languages as listed in measurements §4
  and the reporting table of §4 here, the Norwegian for the owner's
  review. Recommended as drafted.
- **C-7 Box name, setting and store.** *Selected object* / *Valgt
  objekt*, on by default, its own store, in Chart Options → Chart and the
  Controller. Recommended.
- **C-8 Subject and showing.** The working selection's lead, shown exactly
  while it is on the page; an additive click on empty sky leaves the box
  describing the lead. Recommended. Alternative: hide the box whenever the
  Inspector answers empty sky.
- **C-9 Facts.** The bounded per-family lines of §5, no size or phase in
  version one. Recommended. Alternative: add a deep-sky object's size, or
  the Moon's illuminated fraction from the painted disc.
- **C-10 Body mark.** A selection ring for a selected body. Recommended.
- **C-11 Placement.** Upper left first for this box only, amending ruling
  3 on #556. Recommended.
- **C-12 Clean chart.** Hides the box, amending the clean-chart ruling.
  Recommended, as the road's starting rule. Alternative: keep clean chart
  toolbar-only and hide the box only by its own switch.
- **C-13 Paper.** The lead's mark travels with the box. Recommended.
- **C-14 The seam.** One sealed `SelectedObjectFacts` built by a fact
  source shared with the Inspector; no free text. Recommended.
- **C-15 Inspector gaps.** Leave the English-only type line and the bare
  Bayer letter for a later issue. Recommended. Alternative: fix both in
  #692, since the shared fact source touches them.
- **C-16 Stages and gates.** #691 then #692, each with focused development
  checks and an unqualified packaged candidate; one final seven-gate
  qualification of the integrated head in #693. For this study head:
  gates 1 and 3-7 once, gate 2 skipped because no interface photographer
  changed (the study's sketches are study images, checked by gates 3 and
  4), then CI once and the pinned merge - as for the bookmark study.
  Recommended.

## 8. The heap

Measured on this study tree before its evidence lands, as the owner's
limit of 2026-11-01 requires, with #676's exact commands under
`-Xlog:gc*` (record `~/juranometria-runs/runs/heap-690-study/`). The
heap figures stand even though both routes in that run reported a breach
on one sketch (§9): the work done was the same and ran its full length.

| route | time | peak live after a collection | full collections |
|---|---|---|---|
| canonical | 19.0 min | 1 590 MB | 0 |
| portable | 20.4 min | 1 616 MB | 0 |

6.2.0's main measured 1 526 and 1 548 MB, with one 25 ms fragmentation
collection on the portable route. The study adds 26 small images and does
not narrow the margin. The cap stays at 2 GiB as ruled; it is measured
again before #692's evidence lands.

## 9. What the development checks found

- **One sketch did not reproduce inside the contract's shared process.**
  Both routes breached on a Pointer navigation sketch: drawn twice in a
  clean process the four sketches are identical, but an earlier generator
  in the contract's process can leave its own default font behind. The
  study now clears the default font before drawing, as the bookmark study
  does; with the default font deliberately polluted first, two runs in one
  process reproduce the recorded bytes. Both routes were then run again
  (§10).
- **The full suite** found one failure, the export dialog photographer's
  capture refusal (content 332 px wide settling at 326 px): the #362
  capture-refusal family, in a photographer this study does not touch, with
  49 desktop-focus aborts and none unexplained. It was not rerun.

## 10. Development checks on the study head

- `PointerDragSketchTest`: 3 of 3.
- The study drawn twice in one process, the default font polluted before
  the first run: identical, and identical to the recorded provenance.
- Both evidence-contract routes, run again after the fix: *EVIDENCE
  CONTRACTS OK*, no breach, the study's images unchanged
  (`~/juranometria-runs/runs/j690-devcheck/`).
- The full suite on the tree before the fix: 2 594 of 2 644 passed, the one
  failure and the 49 aborts as described in §9.

No gate and no CI has run on this head.

## 11. The ruling

The owner's ruling of 2026-11-04 on #690, recorded as given: "Rule C-1
through C-16 as recommended, run gates 1 and 3-7".

So, as §4, §5 and §7 recommend:

- **Pointer navigation:**
  - The modes are Free, Horizontal only, Vertical only and Locked (C-1).
  - Shift, read at the press of a free drag, keeps that one drag to its dominant axis, decided once (C-2).
  - The modes apply to every projection, to mouse and trackpad press-drag alike. The wheel is untouched and no keyboard panning is added (C-3).
  - Free at every launch, not stored, and Lock zoom unchanged (C-4).
  - A Controller section after Chart controls, plus a spoken sentence and a view-report line when the mode is not Free. No toolbar button, menu item or shortcut (C-5).
  - The words in both languages are as drafted, the Norwegian included (C-6).
- **The selected-object box:**
  - It is called *Selected object* / *Valgt objekt*, is on by default, and has its own store. It appears in Chart Options → Chart and in the Controller (C-7).
  - It describes the working selection's lead while that lead is on the page. An additive click on empty sky keeps the box on the lead (C-8).
  - The facts are bounded, with no size or phase in version one (C-9).
  - A selected body gets a selection ring (C-10).
  - This box prefers the upper left, amending ruling 3 on #556 for this box only (C-11).
  - Clean chart hides the box, amending its "touches nothing else" ruling (C-12).
  - The lead's mark travels to paper with the box (C-13).
  - It is built on one sealed `SelectedObjectFacts`, shared with the Inspector, with no free text (C-14).
- **What is left for later:**
  - The Inspector's English-only type line and bare Bayer letter are left for a later issue (C-15).
- **The stages:**
  - #691, then #692, each with unqualified candidates, and one final seven-gate qualification in #693 (C-16).
- **For this study head:**
  - Gates 1 and 3-7 once, with gate 2 skipped for the stated causal reason.
  - Then CI once and the pinned merge of the study.
  - No production implementation before that landing.
