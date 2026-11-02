# Bookmarks: saved views, optionally enriched

**Issue #497, Sprint 56 — Bookmark the sky (provisionally 6.2.0).** The
owner ruled the product semantics on 2026-11-02 (#497, comment
6580989942); the owner's earlier clarification of 2026-10-10 on dynamic
objects stands beside it. This record is the study that ruling asked
for: it measures what the rulings need from the atlas as it is at
`6223cdab` (v6.1.0), proposes the exact schema, persistence, interface
and implementation split, and names the few choices still open. It stopped
for the owner's checkpoint before any production code; **the owner ruled
C-1 to C-8 on 2026-11-02 (section 16)**, amending C-2 and adopting the
alternative in C-3, and the sections below are brought to that ruling.

The measurements are `docs/studies/bookmarks/measurements.md`, written by
`make bookmarks-study` (`juranometria.tool.BookmarkStudyMain`), with the
sketches beside it and the example library in
`docs/studies/bookmarks/examples/library.json`. The proposed document
format is executable study code, `juranometria.tool.BookmarkDocument`,
held by `BookmarkDocumentTest`. No application class reads or writes a
bookmark.

## 1. Vocabulary

| term | meaning |
|---|---|
| **bookmark** | a saved geometric view, optionally enriched with subjects, a primary, a place, a chart time and a note |
| **view** | the centre (ICRS/J2000 right ascension and declination, degrees) and the field width, one of the atlas's steps |
| **subject** | a kind (`star`, `deep-sky`, `body`) and the atlas's own identity for it; never display text |
| **primary** | at most one subject, held by identity, selected when the bookmark opens if it resolves |
| **place** | latitude and east longitude, as the chart context holds them |
| **chart time** | one UTC instant and its purpose, `event` or `observation` |
| **folder** | a user folder; the permanent root is *Bookmarks* / *Bokmerker* and is never stored as text |
| **library** | the whole tree, in user order |
| **record needing attention** | a stored record this version cannot use: kept exactly, reported, never opened or rewritten |

## 2. What a view is, measured

From the atlas's own classes (measurements, section 1):

- `ChartViewState` holds a centre, a field width, a magnitude limit, an
  optional target label and identity, and a projection that must agree
  with the field. **There is no orientation state**: every page is drawn
  celestial north up, east left (`ViewportMapping`), and the words "Celestial
  north up · celestial east left" are fixed text. **The projection is the
  field's** (`ChartProjection.forField`: orthographic at 180°,
  stereographic above 42°, gnomonic otherwise).
- So the minimum saved view is **centre and field**. Projection and
  orientation are restored by restoring the field; storing them would be
  storing a second copy that could disagree. Should a later atlas offer a
  rotated or a chosen projection, version 2 adds the field it needs.
- **The magnitude limit is the reader's** (ruling 2) and is not stored.
  Opening through the controller's own `recenter(centre, field)` keeps it
  and lowers it only where the new field's default is brighter - the
  atlas's existing rule for every field change, measured in section 1.
- **The target label and identity are not stored.** They are derived when
  a deep-sky primary opens (section 4).
- **The page's vertical extent is the window's**, not the view's: the
  field is the horizontal extent. A subject near the top or bottom of the
  saved page can be off a shorter window's page (measured: at 8° a
  900 × 500 window shows ±2.22° vertically against ±3.11° at 900 × 700).
  This is why a primary is resolved by identity, not by what the page
  happens to show (section 6).
- **Closer fields** (0.5°, 0.25°) are accepted by `recenter` whether or
  not a body offers them; the offer is asked only by the closer-field step.
  Proposed: a bookmark opens at its closer field only where the restored
  page offers it, otherwise at 1°, and the details say so.

## 3. The version-1 document

One UTF-8 JSON document, written canonically (two-space indentation, keys
in a fixed order, one value per line, LF, one trailing LF, non-ASCII
written as itself). Section 4 of the measurements shows a complete
bookmark; the full schema:

```text
document   = { "format": "juranometria-bookmarks", "version": 1,
               "writtenBy": <application version>, "root": [ node* ] }
node       = folder | bookmark            (anything else is kept, see §8)
folder     = { "type": "folder", "id", "title", "children": [ node* ] }
bookmark   = { "type": "bookmark", "id", "title",
               "view": { "ra", "dec", "field" },
               "subjects"?: [ subject+ ],             user order
               "primary"?: subject,                   one of the subjects
               "time"?: { "instant": "...Z", "purpose": "event" | "observation" },
               "place"?: { "latitude", "eastLongitude" },
               "note"?: text }
subject    = { "kind": "star" | "deep-sky" | "body", "id": <atlas identity> }
```

- **Identities.** `id`s of folders and bookmarks are random UUIDs, stable
  for the record's life (an edit, a move or a later API refers to them).
  Subject identities are the catalogue's (`TYC 1800-2202-1`, `NGC 598`)
  and the modules' (`moon`, `jovian.io`), language-independent; titles
  and notes are the user's own text in any language.
- **The primary is stored by identity**, so it never follows list
  position (ruling 4). Subjects are unique within a bookmark.
- **One instant plus a purpose suffices.** The instant is stored in UTC
  (`2026-11-24T11:54:00Z`); a later local civil input is converted at the
  dialog and the zone is not stored, because the chart has one time and it
  is an instant. A time without `Z` is a record needing attention, never
  guessed.
- **Numbers keep their exact text** on reading, so a document read and
  written again is unchanged byte for byte (measured on all eight required
  examples, section 4).
- **No defaults are written**: an absent `subjects`, `primary`, `time`,
  `place` or `note` means none.
- **Choice C-1 (format).** The application bundles no JSON today: the only
  reader is the tool-only `MiniJson`, kept out of the application by an
  architecture note since Sprint 1 ("no general-purpose JSON dependency").
  Recommended: JSON, with the application's own small strict reader and
  canonical writer (the study's `BookmarkDocument` is 879 lines including
  the domain and its validation), and still no dependency. Why JSON: it is the most
  inspectable format a reader can open, it is the natural shape of a tree
  with ordered lists, and any later import, API or database speaks it.
  The alternatives measured against the rulings: `java.util.Properties`
  cannot hold an ordered tree without an invented key scheme; the JDK's XML
  is present (`java.desktop` requires `java.xml` transitively) but heavier to read and
  to keep canonical.

## 4. Opening a bookmark: the restoration rules

Measured on the atlas's own controller, chart-context holder and working
selection (measurements, section 6), all eight combinations of stored
place, stored time and primary:

| stored | what changes | what stays the reader's |
|---|---|---|
| (always) | centre and field, through `recenter`; projection and orientation follow the field | the magnitude limit (lowered only by the field's own rule), every Chart Option, grid, label, emphasis and layer |
| place | latitude and east longitude | the instant |
| time | the chart time, to the exact stored instant | the place |
| place and time | both, in **one** installation of the chart context (one redraw, one revision) | — |
| primary | the selection becomes the primary, if it resolves | — |
| no primary | nothing: the selection is not touched | the selection |

- Place and time go through the existing route every other surface uses
  (`MeridianModule.observer(…)` → `ChartContextHolder.install`), built from
  the current observer so that an absent part is left exactly as it was.
- **C-2 (place), as ruled.** Opening a bookmark with a place installs it
  as the chart's current place and does **not** write it to `PlaceStore`:
  the reader's persisted startup place changes only by an explicit Place
  and Time edit. The chart still has one current place; the stored default
  for a later session is not a second chart answer. (A later *Remember this
  place* action could be offered; opening a bookmark never does it.) The
  details panel states the place before opening.
- **Making subjects visible** (ruling 2: "may be made visible"):
  - a Solar System body's switch is turned on if it is off, and remembered,
    exactly as *Centre on chart* already does (`CentreOnChart.centre`) - a
    planet's moons are drawn with their planet;
  - a deep-sky primary opens as the page's named target (`recenter` with its
    label and identity), which the renderer already draws whatever the
    family switches say;
  - **a star primary receives the same single-target privilege (C-3, as
    ruled)**: that one star is drawn as the page's target even when it is
    fainter than the reader's limit, without raising the limit or revealing
    any other faint star - a rendering change of its own, built with the
    opening in stage 3;
  - a non-primary deep-sky object hidden by the current Chart Options, and a
    non-primary star fainter than the reader's limit, are **not** forced:
    that would be restoring appearance (ruling 2). The details list them as
    "hidden by your current Chart Options" or "fainter than your limit".
- **The primary resolves by identity**, against the catalogue and the
  module bodies, not against the page inventory: `PageContents.find` only
  sees what is on the page, and a shorter window can leave a primary just
  off it (section 2). A moon is not selectable today, so a moon can be a
  subject but not a primary in version 1.
- **A dynamic primary is an occurrence at the chart instant** (owner,
  2026-10-10): opening the Moon-and-Venus bookmark installs its event time
  and then selects the Moon at that instant, through the same route a
  click uses.

## 5. Capturing a bookmark: time and place are explicit

Ruling 6 requires an explicit choice; the owner's note of 2026-10-10 says a
bookmark captured from a dynamic selection carries its event time and must
not silently drop it. The add dialog reconciles the two (sketches 2 and 3):

- **Include chart time** is a visible checkbox with the instant written
  beside it, and a purpose (event or observation).
- **Choice C-4 (the default).** Recommended: the box is ticked, as *event
  time*, exactly when a subject is a Solar System body, with one sentence
  saying why ("The Moon is shown at this instant; without it, it opens at
  the chart's time then."); unticked otherwise. Nothing is captured that
  the dialog does not show, and the reader can untick it.
- **Include place** is always unticked by default, with the place written
  beside it.
- **Observation time** is chosen deliberately; "Now" may be offered beside
  it as a button that fills the field, never as a default.
- The subjects are the working selection at the moment of adding, in its
  order, with its lead as the proposed primary - the user reorders, removes
  and chooses the primary (or *None*). The working selection itself stays
  unpersisted, as its decision requires; the bookmark copies identities.

## 6. Stable and unresolved subjects

| case | what the stored identity does | what opening does |
|---|---|---|
| a star or deep-sky object in the catalogue | resolves | selected if primary, otherwise may be made visible |
| a Messier name | the catalogue's own identity is stored (`M 33` is `NGC 598`) | one object, never two |
| a body or moon | the module's identity | the body's switch on; a moon via its planet |
| an identity the catalogue no longer has (a later Tycho-2 cut, a renumbering) | kept exactly as stored | the view opens; the subject is reported "not found in this catalogue - kept as stored"; nothing is substituted |
| an unresolved primary | kept | the view opens; the selection is not touched; the report names the identity |
| a renamed object | identities are designations, not names; a renamed proper name changes only what is shown | resolves |
| a body this version does not know (a later atlas adds Pluto) | kept | reported as above |

## 7. Persistence and the service boundary

**Where.** None of these folders exists for JUranometria today: everything
else lives in the one Preferences node `juranometria`, and the atlas writes
no other user file.

| system | document |
|---|---|
| macOS | `~/Library/Application Support/JUranometria/bookmarks.json` |
| Windows | `%APPDATA%\JUranometria\bookmarks.json` |
| Linux and others | `$XDG_DATA_HOME/juranometria/bookmarks.json`, else `~/.local/share/juranometria/` |

**Choice C-5**: these locations, and whether a system property may name
another folder (useful for tests and a later portable launch; not
offered in the interface).

**The write** (measured, section 7): the whole document to
`bookmarks.json.part` beside it, forced to disk; if the current document
reads as usable, it is copied over `bookmarks.json.bak` (the last known
good library); then the temporary file is moved onto `bookmarks.json`
atomically, refusing rather than copying where the file system cannot
move atomically - the export sheet's own rule. Every write is the whole
library: the document is small (the eight examples are 5 223 bytes).

**The start** reads `bookmarks.json`; a stray `.part` is deleted unread; a
damaged document is never overwritten - it is kept and, if a backup reads
as usable, the backup is offered and restored only on the reader's yes
(sketch 7); a newer version is never written and the library opens
read-only with a notice. While the library is read-only, *Add Bookmark…*
and editing are disabled, with the reason as their explanation.

**The service** (ruling 7). Every consumer - the add dialog, the
Bookmarks tab, the opener, export and restore - uses one
persistence-independent `BookmarkLibrary`:

```text
BookmarkLibrary
  state()           : Available(tree, attention) | ReadOnly(reason) | Unavailable(reason, offer)
  add(parent, bookmark, position)          -> id
  update(id, bookmark)                     edit: title, view, subjects, primary, time, place, note
  addFolder(parent, title, position)       -> id
  rename(folderId, title)
  move(id, newParent, position)            order and membership only; content untouched
  removeBookmark(id)
  removeFolder(id, KEEP_CONTENTS | DELETE_CONTENTS)
  exportTo(destination)                    the canonical document, records needing attention included
  inspect(source)       -> RestoreSummary  validates, counts, never touches the library
  restoreFrom(source)                      keeps the current library, then replaces
  recoverFromBackup()                      only after the reader's yes
  onChange(listener)
```

The local document is one implementation (`DocumentBookmarkLibrary`); a
test implementation holds the library in memory. Nothing outside the
service sees a path, a JSON value or a file.

## 8. Export, backup and restore

- **Export** writes the library's canonical text - every record, including
  those needing attention - to a file the reader chooses, through the
  export sheet's placement and its replace question.
- **Restore** reads a chosen file; refuses before touching anything if it is
  malformed, foreign or newer, with the reason; otherwise shows the summary
  (bookmarks, folders, records needing attention, the writing version;
  sketch 6); on the reader's yes, keeps the current library as
  `bookmarks-before-restore.json` beside the document and replaces it.
- **Exact round trip, measured** (section 8): export then restore leaves the
  document byte-identical to the export; restoring the kept file undoes the
  restore byte for byte.
- No merge-import in this road (ruling 7).

## 9. Failure, migration and partial recovery

Measured, section 5 and section 7:

| case | outcome |
|---|---|
| interrupted write | the previous library is the document or the backup; nothing is lost |
| malformed main document | kept unchanged; the backup offered; library unavailable until the reader decides |
| a byte-order mark or another program's JSON | treated as unreadable or foreign; never overwritten |
| future schema | never reinterpreted, never written; read-only notice: a newer JUranometria is needed |
| unknown record type, unknown field, bad view, local-time instant, duplicate id, primary not among subjects | the record is kept exactly, marked *needs attention* with its reasons; every other record stays usable |
| deleted folder | its contents move to the parent unless the reader explicitly chooses to delete them (ruling 5) |
| unknown subject identity | kept; reported at opening; never substituted |

A record needing attention is shown in the tree with a symbol and words
(never colour alone); its details list the reasons and offer *Export…*
before any edit, and nothing opens it.

## 10. Privacy

Everything is local: one file in the user's own data folder, read and
written only by the application, with no network access (as for every
other part of the atlas). What a later API could expose, without adding
one now: the library's tree, titles, notes, subjects, views, times and
places. Notes and places are personal; any later exposure would be a
ruling of its own.

## 11. The interface

**Menu** (sketch 1): a top-level **Bookmarks** / **Bokmerker** between
Solar System and Help, mnemonic B in both languages: **Add Bookmark…** /
**Legg til bokmerke…** with Command-D / Ctrl-D, and **Show Bookmarks** /
**Vis bokmerker**. **Choice C-6**: a key for Show Bookmarks -
recommended Shift-Command-B / Shift-Ctrl-B (unbound today; Command-B is left
free). The menu never mirrors the tree (ruling 6).

**Toolbar**: one icon-only button for Add Bookmark…, after Place and Time,
with the tooltip naming the key and the explanation naming the menu item,
as the #675 buttons do. **Choice C-7**: the icon - recommended Tabler
`bookmark-plus` (outline, v3.46.0, MIT); fetching it needs your approval of
the download (one SVG into `src/resources/icons`, recorded in
`ICONS.md`; its name and size are confirmed against v3.46.0 at the
download, and if it is absent there I stop and report rather than
substitute). If it does not read clearly at toolbar size in both themes, the
button keeps the words.

**Add Bookmark…** (sketches 2, 3): a modal dialog like Export's - title
(prefilled from the primary's name, or the page's title), folder, the
view line (always saved), the subjects in order with Move up, Move down and
Remove, the primary (a choice including *None*), the two explicit
checkboxes with their values written out, the note, Cancel and Add. Enter
adds; Escape cancels.

**The Bookmarks tab** (sketches 4, 5): a permanent third Controller tab.
The tree (root *Bookmarks*, folders, bookmarks, a ◷ beside a timed one,
⚠ and words on a record needing attention), the details of the selected
item (selecting never opens), and Open, Edit…, Delete…, New Folder… and a
Library menu (Export Bookmarks…, Restore from File…). Keyboard: arrows
walk the tree, Enter opens, Delete asks, F2 renames a folder;
drag-and-drop and Move up/Move down reorder; the tab is reached with the
Controller's existing Ctrl-PageUp/PageDown. *Show Bookmarks* opens the
Controller if it is closed and selects this tab.

**Edit…** changes everything Add can, plus *Use the current view*; moving
between folders changes no content (ruling 5).

**Accessibility**: every control has an accessible name and one
explanation decision (`Explain.control` / `selfExplanatory`), the hover
never equal to the spoken words; the tree's rows speak title, kind and the
time or attention state; the attention state is never colour alone.

**Words**: proposed in both languages in the measurements' table; all new
keys appended at the end of both language files (the pinned line of
`InterfaceTextTest` is untouched).

## 12. Integration map and conflict points

| meets | conflict point | proposal |
|---|---|---|
| `ChartViewController` | no `setState`; `recenter(…field…)` clamps the limit | open through `recenter`, as measured |
| closer-field offer | `recenter` accepts 0.5° / 0.25° unasked | ask the offer; else 1°, stated |
| `ChartContextHolder` | installs only on the event thread, refused while held | one install of the combined observer |
| `PlaceStore` | install does not save the place | as ruled (C-2): opening never saves it |
| `WorkingSelection` / `LeadSelection` | the lead resolves through the page inventory only | resolve the primary by identity first |
| Solar System switches | turning one on is remembered | as *Centre on chart* (C-3) |
| renderer, star target | stars have no target privilege today (`ChartRenderer` draws a star only within the limit) | the star primary's single-target privilege (C-3) |
| `SearchNavigation` target privilege | dropped by any pan | the deep-sky primary's visibility lasts until the reader moves the page, as a search's does |
| Controller tabs | `companion.tabs.explain` says "Two tabs" | rewritten for three |
| language ledger | 166 of a bound of 170 | about seven new identifiers: raise the bound with the reason, or derive names from classes |
| `TestEvidenceGateTest` | a new user-data door and a new display test | its pins move with the reasons |
| evidence | menu, toolbar, companion, control-explanation and toggle-shortcut sheets; new photographers for the dialog and the tab | regenerated once per stage, all at the final head |

## 13. The implementation, in reviewable stages

Proposed, as #675 was done: **one integrated implementation issue in four
stages, one PR**, each stage with focused development checks and an
unqualified packaged candidate where it is visible; then one final
qualification (the full suite and the evidence contracts as development
checks first, the stated gate plan, CI once), the pinned merge, and a
6.2.0 handover issue.

1. **The library service and the document** - the application's JSON
   reader and writer, `BookmarkLibrary`, the document implementation with
   its write, backup, start and recovery, the in-memory implementation;
   tests for every row of sections 5, 7 and 8. No interface. (Development
   checks only; nothing to hold in the hand.)
2. **Adding** - the menu, Command-D, the toolbar button (icon after C-7),
   the add dialog with the explicit choices; bookmarks are stored but not
   yet listed. Candidate.
3. **The Bookmarks tab and opening** - the tree, details, the restoration
   rules of section 4 with the visibility rules and the unresolved report,
   Edit, move, reorder, folders and their deletion choice. Candidate.
4. **Export, restore and recovery** - the Library menu, the restore summary
   and undo, the damaged-document offer, the read-only modes; the new
   photographers and every moved sheet. Candidate, then the final
   qualification.

Then **#N+1 — the 6.2.0 handover**, as #676 was. **Choice C-8**: this
shape, or separate issues and PRs per stage.

**The heap, measured for this road** (the owner's limit of 2026-11-01:
the portable route had peaked at 1 915 MB under its 2 GiB cap at 6.1.0).
On this study's tree, both routes under `-Xlog:gc*` with #676's exact
commands (record `~/juranometria-runs/runs/heap-497-study/`): the
canonical route 18.5 min, no Full GC, at most **1 517 MB** live after a
collection; the portable route 19.9 min, no Full GC, at most **1 830 MB**
- about 218 MB under the cap, a little more room than at 6.1.0, the peak
after collection being a noisy figure. The study's 14 sketches do not
narrow the margin. Stages 2-4 add photographers and move sheets; the heap
is measured again on stage 4's head, before its evidence lands, and
raising the cap is put to the owner explicitly if the margin has shrunk.

## 14. Which gates can inspect the ruled study head

What this head adds: two tool classes (study code, packaged in the jar as
every tool class is), one test, a registered study (report, platform
record, 14 images, one example document), a Makefile target, this record.
No application class changes.

| gate | inspects on this head | proposed |
|---|---|---|
| 1, full suite | the new test; `TestEvidenceGateTest`'s inventory counts; the language ledger over the new tool sources | run |
| 2, interface evidence | the display-owned photographers; none changes, and the study's sketches are not photographers | skip, with that reason |
| 3, canonical evidence contract | regenerates the new study twice and holds it to its committed bytes, with every other study | run |
| 4, portable evidence contract | the same on the CI route | run |
| 5, packaged acceptance | the jar gains two tool classes; the image must still pass | run |
| 6, portable archive | the archive's contents with the larger jar | run |
| 7, provenance and a clean tree | the 15 new promoted artifacts | run |

So the causal plan is **1, 3, 4, 5, 6 and 7, with 2 recorded as skipped**;
by the standing rule that doubt means all seven, running gate 2 as well
costs about four minutes (3.8 min on `438d0cc4`'s record). The owner chooses. Gates are run only on the
head the owner rules, not on this proposal head (the gate-economy rule).

## 15. Choices for the owner

- **C-1** the document format: JSON with the application's own small
  reader and writer (recommended).
- **C-2** a stored place is remembered as the reader's place when opened
  (recommended).
- **C-3** visibility: bodies switched on and remembered, a deep-sky primary
  as the page's target, other hidden subjects reported and not forced
  (recommended); or extend the target privilege to a star primary.
- **C-4** *Include chart time* ticked as event time exactly when a subject
  is a Solar System body, stated in the dialog (recommended).
- **C-5** the document's folder per system; a property for another folder
  (recommended, not in the interface).
- **C-6** Shift-Command-B / Shift-Ctrl-B for Show Bookmarks (recommended).
- **C-7** the Tabler `bookmark-plus` icon, and approval to download it.
- **C-8** one integrated implementation issue in four stages, one PR, then
  a handover issue (recommended).
- And the gate plan for the ruled study head (section 14).

## 16. The ruling

The owner's ruling of 2026-11-02 on #497, recorded as given:

> - C-1: accept JSON with the application's own small strict reader and canonical writer, with no new dependency.
> - C-2: amend the recommendation. Opening a bookmark with a stored place applies that place to the current chart context but does not write it to `PlaceStore` or change the reader's persisted startup place. Only an explicit Place and Time edit changes that lasting preference. The chart still has one current place; the stored default for a later session is not a second chart answer.
> - C-3: accept the proposed visibility rules, with the star-primary alternative adopted. A star primary receives the same single-target visibility privilege as a deep-sky primary, without raising the general magnitude limit or revealing other faint stars. Non-primary subjects hidden by current settings are reported and not forced. Solar System switches behave as proposed.
> - C-4: accept the visible, pre-ticked Include chart time box as event time exactly when at least one Solar System subject is included, with the explanatory sentence and the ability to untick it. Place remains unticked by default.
> - C-5: accept the normal per-system user-data locations and a hidden system-property override, with no interface setting.
> - C-6: accept Shift-Command-B / Shift-Ctrl-B for Show Bookmarks.
> - C-7: accept Tabler `bookmark-plus`. The one SVG download from the already used Tabler v3.46.0 source is approved within the stated size, destination and MIT terms. Stop rather than substitute if the named icon is absent.
> - C-8: accept one integrated implementation issue and PR in the proposed four stages, with focused development checks and the visible unqualified candidates at stages 2, 3 and 4, followed by one final qualification and a separate 6.2.0 handover.

The owner's reasons, in brief: a bookmark's place is a temporary context,
not the reader's lasting default; and a primary subject should be
visible - a valid bookmark of a faint double star must not open centred and
selected with its main subject invisible.

For this study head: gates 1 and 3-7 once, gate 2 skipped for the stated
causal reason (no interface photograph changed; the registered sketches
are checked by gates 3 and 4); CI once; then the pinned merge of the
study. No production implementation before that landing. The icon is
fetched in stage 2, not here.
