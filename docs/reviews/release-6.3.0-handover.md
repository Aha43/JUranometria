# JUranometria 6.3.0 — guide the chart and explain the selection: handover to release

*Issue #693, closing road #689 (Sprint 57). Written on the integrated
head of `feature/691-pointer-navigation` (`4ce00f94` plus this
handover), before any `VERSION`, tag or publication decision; 6.3.0 is
this document's working name for the next release, the owner's to
confirm. Since 6.2.0 (`651f5347`, published 2026-11-03) one pull request
has landed - #694, the measured study with its rulings - and the two
owner-checkpointed stages #691 and #692 stand on one integrated branch
awaiting a single qualification. No catalogue, service, planet, pack or
released chart page changes. The road's promise: **guide the chart
precisely, then keep the result understandable on the page.***

## 1. What a reader gets in 6.3.0

As the unreleased changelog states them:

- **Pointer navigation** (#691). Controls → Chart → **Pointer
  navigation** offers Free, Horizontal only, Vertical only and Locked. A
  constrained drag keeps the grabbed sky on the screen row, or column,
  where the press was - a statement about the screen, never a line in the
  sky; Locked lets no drag move the chart while a click still selects.
  Holding **Shift** at the press of a free drag keeps that one drag to the
  direction of its first movement. Every launch starts Free; nothing is
  stored; Lock zoom is unchanged. A constrained or locked drag is said
  with the page and written in the view report.
- **The selected-object box** (#692). Click a star, a deep-sky object, the
  Moon or a planet and a small box in the upper left says what it is -
  its name and designation, its magnitude or its chart time, its position,
  and whether the page dims it below the horizon - while the ordinary
  selection ring, now drawn around a selected body too, marks it. Only
  the last selected object is described; the box leaves the page with
  the object and returns with it; hiding the toolbar hides it; it travels
  to every exported sheet with its ring. *Chart Options → Chart →
  Selected object box*, on by default.

## 2. Every ruling of the road

| ruling | where it stands | in short |
|---|---|---|
| the road (2026-11-04) | #689 | one compact interaction-and-feedback road: constrained pointer dragging, and the last selected object explained on the chart; a measured study first; one final qualification of the integrated head |
| C-1 to C-16 (2026-11-04) | `docs/decisions/guide-and-explain.md` §11; #690 | "Rule C-1 through C-16 as recommended, run gates 1 and 3-7": the four modes; Shift read at the press, its axis decided once; every projection, mouse and trackpad press-drag, no keyboard panning added; Free at every launch, never stored; a Controller section after Chart controls, a spoken sentence and a report line when not Free, no toolbar, menu or shortcut; the words as drafted; *Selected object* / *Valgt objekt*, on by default in a store of its own; the working selection's lead while it is on the page; bounded facts; a ring for a selected body; this box upper left first (amending ruling 3 on #556); clean chart hides it (amending the clean-chart ruling); the lead's mark travels to paper; one sealed fact type shared with the Inspector; the Inspector's English type line and bare Bayer letter left for later; #691 then #692 with unqualified candidates and one final qualification |
| the portability correction (2026-11-05) | #694 | "Accept the portability fix on `341a325b`"; gates by the impact of the exact diff - 3 and 4 recorded from the local runs on the clean exact head, 7 run, 1, 2, 5 and 6 skipped with reasons |
| the study's landing (2026-11-05) | #694; `landing-694.md` | "Merge #694 pinned to 341a325b" |
| #691 accepted (2026-11-05) | #691 | "Works great" |
| #692 accepted (2026-11-05) | #692 | "works very well"; the question of a chart-keyboard letter for the box was not taken up, so it has none, as ruled |

## 3. The owner's checkpoints and verdicts

- **The study** - `a9ac52e7` stopped for C-1 to C-16 with the pan
  measurements, the furniture pages and the heap; ruled the same day,
  recorded at `674786cd`.
- **The portability correction** - CI found the study's portable report
  stating two facts that depend on the desktop (section 5); corrected at
  `341a325b`; the owner accepted it and ruled its gates by impact.
- **#691, pointer navigation** - candidate `ed52eb8e` opened on the
  owner's screen: *"Works great"*.
- **#692, the selected-object box, screen and export** - candidate
  `4ce00f94`: *"works very well"*.

## 4. The contracts as built

### 4.1 Pointer navigation

- `ui.PointerDrag` holds the session's mode and the two screen rules: the
  target pixel for a mode, and the dominant axis of the first movement of
  at least 4 px (production's click threshold), a tie going across.
- `PanInteraction` reads the mode **at the press**: a mode chosen, or
  Shift pressed or released, mid-drag changes nothing until the next press.
  A constrained drag asks the unchanged exact grab solver for the pointer's
  column on the press row, or its row on the press column; its pole rules
  apply as before. Locked starts no grab and offers no open hand; the
  press stays the click's.
- `ui.PointerNavigationPanel` is the Controller's section directly after
  Chart controls, open the first time: four named, explained radio choices
  and one sentence. The section's open or closed state is the
  Controller's; the choice is never stored.
- The chart's spoken description gains one sentence while the drag is
  constrained or locked (`PageWords.pointerDragNote`); the view report a
  `pointer-drag:` line after `furniture:`. Free adds neither, so every
  released page description and report reads as before.

### 4.2 The selected-object box

- `ui.selection.SelectedObjectFacts` is sealed over a star, a deep-sky
  object and a body, built only from the page inventory entry of the lead
  and the body placement the page painted - no second position, time or
  astronomy calculation. It also holds the coordinate, magnitude and
  Messier formatting the Inspector now calls, moved unchanged.
- **The facts**: a star - its best name (name, Bayer letter with
  constellation, Flamsteed number with constellation, catalogue id), its
  other designation and catalogue id, *V* magnitude, position; a deep-sky
  object - its Messier name or id, its id and family in the singular, its
  magnitude with its band, position; a body - its page name, *for … UTC*,
  position, and *(below the horizon)* when the page dims it. A heading and
  at most three lines, a fourth only for a dimmed body.
- `ui.selection.SelectedObjectBox` is a chart module offering one box
  through the furniture seam exactly while its switch is on, the toolbar is
  shown and the working selection's lead has an entry in the current page's
  inventory - drawn, or crossed because an option, the limit or the field
  keeps it from being drawn. No lead, no box: an untouched chart is the
  released page.
- **Placement**: `Furniture` and `ChartFurniture` may ask for the upper
  left first; such a box is placed after every box in the ruled order and
  tries the upper left, then the upper right while the magnitude key does
  not hold it, then the lower right; a box no corner can hold is refused,
  as all furniture is.
- **The mark**: a selected body gets the ring the catalogue's marks get,
  just clear of its drawn disc or symbol, on screen and on paper.
- **The setting**: `SelectedObjectStore` (key `selectedObjectBox`, on by
  default) and `SelectedObjectSwitch`, driven by the *Selected object box*
  in Chart Options → Chart (mnemonic J, after the title block) in the
  dialog and in the Controller. The box has no chart-keyboard letter, as
  ruled; every other Chart Options box has one.
- **Clean chart**: hiding the toolbar hides the box; showing it restores
  it, the selection and the setting untouched.
- **Paper**: when the box is on the page, the lead's mark travels to SVG,
  PDF and PNG with it, whatever the working-selection export choice; the
  rest of the set travels only when asked.
- **Speech**: the page's description gains *Selected: …*; the box has no
  control and takes no focus.

### 4.3 What the seam permits and refuses

The box accepts only a `SelectedObjectFacts` value; no module can add a
line or pass free text, and `Furniture` still carries meaning and text
only - the one named preference `upperLeftFirst` is the sole exception the
furniture test admits. Not promised by this seam, and not started:
measurement readouts, meteor radiants, projection notes, user notes, a
general information box.

## 5. Every stopped, failed, superseded or incomplete run

Every record is under `~/juranometria-runs/runs/`, unchanged; nothing was
rerun into green.

| run | what happened | how it was continued |
|---|---|---|
| the study's contracts under the heap log (`heap-690-study`) | both routes breached on a Pointer navigation sketch: drawn twice in a clean process it reproduced, but an earlier generator in the contract's process left its default font behind | the study clears the default font first, as the bookmark study does; proven against a deliberately polluted font; both routes rerun clean (`j690-devcheck`) |
| the study's full suite (`j690-devcheck`) | one failure, the export dialog photographer's capture refusal (332 → 326 px), the #362 family; 49 desktop-focus aborts | not rerun; the study touches no capture code |
| gates 1 and 3-7 on `674786cd` (`gates-j690-674786cd-plan`) | all OK; **CI's evidence job then failed** (run 41922738171): the portable report stated a grab residual's last digits (0 here, about 1e-4 px on Linux) and the corner each box stands in, which the fonts decide | the report gives the residual to 0.01 px and leaves corners to `platform.md` (`341a325b`); preserved as `CI-FAILED.md` |
| gates on `341a325b` (`gates-j690-341a325b-plan`) | by the owner's impact ruling: 3 and 4 recorded from the local runs on the clean exact head, 7 run OK, 1, 2, 5 and 6 skipped with reasons | landed as `4b5bd874` |
| #691's first full suite (`j691-devcheck/suite-1`) | five pins on the changed surfaces: the Chart tab's section order (two tests), one ledger row, the test-evidence counts (two) | each fixed before the candidate |
| #692's first full suite (`suite-3`) | six pins: three ledger rows, the reader's-store entry points (20 → 21), furniture's carried types, the test-evidence counts, the display-owned evidence the changed photographers moved | each fixed |
| #692's long run (`suite-4`, `gate2-3`, `heap-692-stage`) | both contracts breached on the #690 study's Venus page: production now rings a selected body, so the study's body pages moved; the full suite and the interface gate each met one capture refusal (Place and Time and export dialog photographers), with 49 focus aborts | the study's body pages regenerated and its sketch caption corrected; contracts rerun clean (`contracts-692`); the refusals not rerun |

Lessons this road adds:

- **a study's portable report states only what every platform agrees
  on**: no furniture corner, no floating-point residual finer than the
  page can show - CI's Linux runner is the second platform;
- **a production change can move a study's evidence**: when production
  starts drawing what a study once sketched, the study is regenerated and
  its words corrected in the same change;
- **gate plans for an amended head follow the exact diff's impact**
  (the owner's ruling of 2026-11-05).

## 6. Routes, evidence movement and performance

### 6.1 Routes

| PR / head | local gates | CI `test` workflow | outcome |
|---|---|---|---|
| #694 `674786cd` | 1, 3-7 OK (52.9 min) | 41922738171: `evidence` failed | corrected |
| #694 `341a325b` | 3, 4 recorded; 7 OK | 41930057488: green, 50.5 min | merged 2026-11-05 00:17 UTC → `4b5bd874`, proven 1-4 (wide route) |
| integrated #691 + #692 | the final qualification (section 8) | - | - |

The suite stood at **2 641** tests at 6.2.0, **2 644** with the study,
**2 658** with #691 and **2 672** with #692.

### 6.2 Evidence movement

From `651f5347` to `4ce00f94`: **96 files, +4 495 / −91 lines** - the
study 28 files under `docs/studies/guide-and-explain` (26 images, the
portable report and the platform record), the implementation 68 files.
Under `docs/studies`, beyond the new study: `interface-language` 20
files (the Controller's 16 photographs and its strings in both languages,
the Chart Options Chart-tab photographs and strings), and one each of
`control-explanations`, `language-ledger`, `test-evidence` and the
provenance record, whose promoted artifacts went from **950 to 976**.
Six source files are new (`PointerDrag`, `PointerNavigationPanel` and the
four `ui.selection` classes) and twenty-two changed; six test classes are
new. Both language files gain the `pointer.*`, `page.pointer.*`,
`chartoptions.selectedObject.*` and `selected.*` keys, appended at the
end, the pinned lines untouched.

**No released chart page moved**: every ladder, sheet, regional,
overview, globe and printable page reproduced under both evidence
contracts on the integrated tree. A selected body's ring and the box
appear only with a selection, which no committed chart page has.

The test-evidence measurements are quoted at **1 181 explicit hand-offs**
and **82 files** touching process-wide state (38 guarded, 43 restoring
locally); the language ledger stays under its bound of 190; the packaged
licensing inventory is **16 resources**.

### 6.3 Performance

The study's planned gates: **52.9 min**; CI's `test` workflow on the
study heads: 48.6 and 50.5 min. The road from the owner's instruction
(2026-11-04) to two accepted stages (2026-11-05 afternoon) took about a
day and a half of calendar time.

## 7. What did not change

- **Chart contracts.** No released chart page moved. The new ink - a
  selected body's ring, and the box - exists only while something is
  selected.
- **Free dragging.** A free drag is the released drag exactly: the same
  solver, threshold, cursors, pole rules and cancellation; the existing pan
  tests ran unchanged.
- **Persistence.** One new key, `selectedObjectBox`, in a store of its own;
  no chart option, migration or existing key moved. The pointer mode is
  never stored. Lock zoom keeps its key and semantics.
- **Keyboard.** No shortcut was added or moved; the Selected object box has
  a mnemonic and no chart-keyboard letter.
- **Accessibility.** Every new control is named and explained in both
  languages; the page says a constrained drag and the selected object.
- **Export.** No export surface changed; the box travels through the
  existing furniture path, the lead's mark through the existing working
  ink.
- **The Inspector.** Its words are unchanged; its coordinate, magnitude and
  Messier formatting now come from the shared fact source, byte for byte.

## 8. The heap, the final qualification, and the owner's separate decisions

- **The heap**, measured on #692's tree before its evidence landed
  (`heap-692-stage`, #676's commands): canonical **18.7 min**, peak
  **1 646 MB** live, no full collection; portable **20.5 min**, peak
  **1 617 MB**, one short full collection, as on 6.2.0's main. The cap
  stays at 2 GiB as ruled for 6.2.0; measured again before the next
  evidence-heavy road.
- **The final qualification** of the integrated head, for the owner's
  approval: **all seven gates** - the head changes application classes,
  interface photographs, the packaged journeys and the distribution's
  contents - with the inventory at 16 and the eight markers, then the PR
  (closing #691, #692 and #693), CI once and the pinned merge.

Then, as for 6.2.0, each of the following is a separate owner decision:

1. the version number and the `VERSION` bump with its changelog date and
   every version-fed regeneration;
2. the release pull request and its merge under #494;
3. the release rehearsal, which publishes nothing;
4. the local annotated tag;
5. the tag push, which publishes;
6. the public verification and the close-out of road #689 and the
   Sprint 57 milestone.

## 9. What 6.3.0 contains, and what it explicitly does not

**Contains:** pointer navigation with its four modes and the Shift
modifier; the selected-object box with its setting, its placement, its
clean-chart and paper rules; a ring for a selected body.

**Does not:** keyboard panning; a shortcut for either feature; a
chart-keyboard letter for the box; a constellation or planetary-moon
selection; size, phase or altitude in the box; a general information box;
the Inspector's English-only type line and bare Bayer letter (C-15, left
for later); and none of the backlog below.

## 10. The backlog this road hands on, without starting it

- **Projections**: a chart-time all-sky projection (#686) and its
  overview (#687) may reuse the pointer modes - they are screen rules and
  already apply to every projection the drag reaches.
- **Measurement tools and meteor showers**: may offer furniture through
  the existing seam; the selected-object box's sealed facts are not a
  general text box and do not carry them.
- **Portable bookmarks** (#688): untouched by this road.
- Still open as before: #618, #617, #613, #598, #594, #589, #571, #566,
  #564, #554, #553, #417, #387, #373, #370, #362, #356, #355, #351, #339,
  #327, #326, #324, #295, #293, #282, and the Milky Way sprint #189-#192
  with PR #199, blocked on licensing.
