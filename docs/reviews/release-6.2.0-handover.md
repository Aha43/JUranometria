# JUranometria 6.2.0 — bookmark the sky: handover to release

*Issue #683, closing the Sprint 56 road *Bookmark the sky* (#497). Written
on main `ae7260ce`, the verified landing of the road's integrated pull
request, before any `VERSION`, tag or publication decision; the number
6.2.0 is this document's working name for the next release, the owner's
to confirm. Since 6.1.0 (`6223cdab`, published 2026-11-01) two pull
requests have landed: #680, the measured study with its rulings, and
#682, the integrated implementation (#681) in four owner-checkpointed
stages. No catalogue, service, planet, pack or chart page changed. This
document records what the road ruled and built, every checkpoint and
verdict, every stopped run and its disposition, what moved and how long
it took, what did not move, and the release boundary with the heap
measurement.*

## 1. What a reader gets in 6.2.0

As the unreleased changelog states it:

- **Bookmarks** (#497, #681). A bookmark is a saved view, optionally
  enriched: the chart's centre and field always, and, by the reader's
  choice, the objects on it in the reader's order with one main object,
  the chart time (as an event's time or an observation's), the place,
  and a note. **Bookmarks ▸ Add Bookmark…** (Command-D / Ctrl-D, and an
  icon-only toolbar button after Place and Time) proposes them from the
  page and the selection; the chart time is pre-ticked only when a
  Solar System object is included, and the dialog says why. **Bookmarks ▸
  Show Bookmarks** (Shift-Command-B / Shift-Ctrl-B) opens the
  Controller's new **Bookmarks** tab: a tree of folders the reader
  makes, details without moving the chart, Open, Edit…, Delete…, New
  Folder…, Move up and Move down. Opening restores the view, applies a
  stored place and time to the current chart only (the remembered
  Place and Time is never changed), turns on a Solar System object's
  switch, draws the main object - a faint star included - as the page's
  one target and selects it, and says what became of each object; the
  reader's magnitude limit and Chart Options are untouched. **Library ▸
  Export Bookmarks…** writes the whole library to a file; **Library ▸
  Restore from File…** checks a file, says what it holds, and replaces
  the library only on the reader's yes, keeping the previous one beside
  it. An unreadable bookmark file is never overwritten: the tab offers
  its backup or a file to restore from; a file from a newer JUranometria
  is never written. In both languages (*Bokmerker*).

Not a reader's change: the evidence-contract heap at 2 GiB is measured
again in section 8; it passes, the cap stays, and for the first time
the portable route ran one short Full GC.

## 2. Every ruling of the road, as recorded

| ruling | where it stands | in short |
|---|---|---|
| the road (2026-11-02) | chat; milestone *Sprint 56 — Bookmark the sky* | verify the repository at rest at v6.1.0; the milestone with #497 attached; a measured study first, stopping at its checkpoint; no production, candidate, `VERSION` or release work before that checkpoint is accepted |
| rulings 1-8 on #497 (2026-11-02) | #497, comment of 15:26 UTC | the bookmark model (a saved view, optionally enriched); view and layers (no incidental chart layers restored); selection (one optional primary, never a substitute); subject order (user-controlled, primary stored apart); folders (one permanent root, safe deletion); the surface (a top-level menu, Command-D, an icon button, a permanent Controls tab, details without opening); persistence (one atomic versioned UTF-8 document with a last-known-good backup, export and validated reversible restore, a persistence-independent service); failure and partial recovery (fail-safe, never silent) |
| C-1 to C-8 (2026-11-02) | `docs/decisions/bookmarks.md` §16; #497 | **C-1** JSON with the application's own strict reader and canonical writer, no dependency; **C-2 amended**: opening a bookmark's place changes the current chart context only and never writes `PlaceStore`; **C-3 with the star-primary alternative**: a star primary receives the deep-sky primary's single-target privilege without raising the limit or revealing other faint stars, non-primary hidden subjects reported, not forced; **C-4** *Include chart time* pre-ticked as event time exactly when a Solar System subject is included; **C-5** the per-system data folders and a hidden system property; **C-6** Shift-Command-B / Shift-Ctrl-B for Show Bookmarks; **C-7** Tabler `bookmark-plus` and the one download, "stop rather than substitute if absent"; **C-8** one integrated issue and PR in four stages, then this separate handover |
| the study's gate plan (2026-11-02) | #497; `gates-j497-79e5b865-plan` | gates 1 and 3-7 once on the ruled study head, gate 2 skipped (no interface photograph changed; the sketches are checked by gates 3 and 4), CI once, the pinned merge |
| the study's landing (2026-11-02) | #680; `landing-680.md` | "Merge PR #680 pinned to `79e5b865…` under the merge-identity rule"; #497 kept open for the implementation |
| the implementation directive (2026-11-02) | #681 | clean up the study worktree; the integrated issue with four stages; "Use focused development checks only. Do not run the seven gates or CI during the stages"; stage 1 without a candidate; stage 2's download exactly as approved; no stage 3 before the stage-2 candidate is tried and accepted |
| the stage-3 review corrections (2026-11-03) | #681, stage 4 checkpoint | three findings applied before stage 4: atomic edit-plus-move and rename-plus-move with failure-seam tests; *Use the current selection* in Edit; the folder deletion question counting the whole nested scope |
| the heap limit (2026-11-01, carried) | 6.1.0 handover §8 | measure both routes again before a road that materially grows evidence; measured on the study head, on stage 4's head and on this handover's main (section 8) |
| the Norwegian words (2026-11-03) | #681 | the owner read the Norwegian half of `bookmarks-strings.md` in full: "Looks good" |
| the final gate plan (2026-11-03) | chat | "Accept stage 4, run all seven gates"; then the PR, CI once and a stop for the pinned merge |
| the landing (2026-11-03) | #682; `landing-682.md` | "Merge #682 pinned to 25dd7119" |
| the handover (2026-11-03) | #683 | the content accepted "subject to one amendment before qualification": the five stale README routes corrected on this head - *Chart → Inspector*, *Chart → Place and Time…*, *Solar System → Sun…*, the Chart menu carrying Zoom In and Zoom Out, *Chart → Chart Options…* - rather than deferred into the version bump; then all seven gates once, the application closed and the pointer parked, a gate-1 capture refusal of the #362 family handled by the established precedent and never rerun into green, any other failure stopping the run; the PR, CI once and a stop for the pinned merge |
| the heap (2026-11-03) | #683; section 8 | "Keep the evidence heap at 2 GiB for 6.2.0. Record the portable route's single 25 ms full collection and the reduced headroom, but do not raise the cap merely because one fragmentation collection occurred. Measure it again before the next evidence-heavy road." |
| the #681 close-out (2026-11-03) | #683 | the fully merged #681 branch closed out: containment and cleanliness verified, every record kept, then its worktree and local and remote branches removed |

## 3. The owner's checkpoints and verdicts

- **The study** - `4511347b` stopped for rulings on C-1 to C-8 with the
  measured architecture, eight example bookmarks round-tripping byte for
  byte, the restoration matrix, the write walked through every
  interruption point, eleven failure cases, and the heap. Ruled the same
  day (section 2), recorded at `79e5b865`, landed as `0b61af47`.
- **Stages 1 and 2, storage and adding** - candidate `eca2f514`, opened
  on the owner's own bookmark folder. While trying it the owner asked
  whether the time could later be edited, to take an observation's
  exact time from a Seestar S50 download; stage 3 made the chart time an
  editable UTC field in Add and Edit. The verdict: *"Stage 2 is accepted.
  I successfully created a bookmark, and the Add Bookmark dialog looked
  and behaved right."*
- **Stage 3, the tab, opening, editing and folders** - candidate
  `a5be8df9`, a copy of the owner's real bookmark document kept aside
  first. The verdict: *"Stage 3 is accepted"*, with three review
  findings to apply before stage 4 (section 2), applied at `74dab599`;
  the visible changes - one Edit checkbox, one sentence - were carried in
  the stage-4 candidate rather than a separate one.
- **Stage 4, export, restore and recovery** - candidate `71bc3840`, the
  owner's real document kept aside again; one question asked: the new
  Norwegian words had not been reviewed by a Norwegian reader. The
  verdicts: *"Looks good"* for the Norwegian, recorded at `25dd7119`;
  then *"Accept stage 4, run all seven gates"*.

## 4. What was built

### 4.1 The surface: menu, shortcuts, toolbar, tab

**Menu bar:** File · Chart · Solar System · **Bookmarks** · Help. The
Bookmarks menu (mnemonic B; *Bokmerker*) holds **Add Bookmark…**
(Command-D / Ctrl-D; *Legg til bokmerke…*) and **Show Bookmarks**
(Shift-Command-B / Shift-Ctrl-B; *Vis bokmerker*). The menu does not
mirror the folder tree (ruling 6).

**Toolbar and Controller strip:** an icon-only **Add Bookmark** button
after Place and Time, with tooltip, accessible name and explanation.

**The one icon decision:** Tabler Icons v3.46.0 outline `bookmark-plus`,
MIT, fetched by `scripts/download-icons.sh` under the C-7 approval on
2026-11-02T20:21:19Z - 469 bytes, sha256
`71fea9dc55178bd5d454288552fb3f31b87aa38c86d21b8094ee209c8d9dfa68`,
recorded in `src/resources/icons/ICONS.md` (retrieval record
`~/juranometria-runs/runs/j681-icon/RETRIEVAL.md`). No candidate was
compared and none rejected; nothing was substituted.

**The Controller:** a permanent third tab, *Bookmarks* / *Bokmerker*,
after Chart and Solar System; Show Bookmarks opens the Controller if
needed and selects it.

### 4.2 The document, its folder, and its safety rules

`juranometria.bookmark`, behind the persistence-independent
`BookmarkLibrary` service; nothing outside it sees a path, a JSON value
or a file.

- **The format** is the study's version 1 (`bookmarks.md` §3): one UTF-8
  JSON document, `"format": "juranometria-bookmarks"`, `"version": 1`,
  written canonically; read by the application's own strict parser,
  which keeps numbers' exact text and key order and refuses a
  byte-order mark. The study's eight examples round-trip byte for byte
  through the production code.
- **The folder** (C-5): `~/Library/Application Support/JUranometria/`
  on macOS, `%APPDATA%\JUranometria\` on Windows, the XDG data folder
  elsewhere; the system property `juranometria.bookmarks.dir` names
  another, not offered in the interface. Nothing is written until the
  first bookmark.
- **The write**: the whole library to `bookmarks.json.part`, forced to
  disk; a usable current document copied to `bookmarks.json.bak`; an
  atomic move onto `bookmarks.json`, refused rather than copied. An
  interrupted write leaves the previous library and adopts nothing; a
  stray `.part` is deleted unread at the next start.
- **Opening the library never fails the application's start.** A
  damaged document is kept unchanged and the library is *unreadable*,
  with the backup offered when it reads as usable; recovering keeps the
  damaged file as `bookmarks-unreadable-<stamp>.json`. A document from a
  newer version is never written and every change is refused.
- **Edit-plus-move and rename-plus-move** are each one next library, kept
  by one write (the stage-3 correction).

As built, the study's record names differ in two places, recorded in
`bookmarks.md`'s new *As built* section: the library's states are
*available*, *newer* and *unreadable with a backup offer* (the study
sketched *available*, *read-only* and *unavailable*), and the file kept
by a restore is stamped, `bookmarks-before-restore-<stamp>.json`, so a
second restore never overwrites the first one's keeping.

### 4.3 Adding

The dialog proposes from the page: a title (the main object's name, else
the page's title or centre); the folder; the view, always saved; the
selected objects in the selection's order, with Move up, Move down and
Remove; the main object, the selection's lead, or None; **Include chart
time** with its UTC instant editable (`uuuu-MM-dd HH:mm[:ss]`) and its
purpose, pre-ticked as event time exactly when a Solar System object is
included, with the sentence saying why (C-4); **Include place**, never
pre-ticked; and the note. A subject's kind is read from its identity: a
Tycho-2 designation is a star, a module identity (`moon`, `jovian.io`)
a body, anything else deep sky. Enter adds, Escape cancels. Edit uses the
same dialog, with *Use the current view instead* and *Use the current
selection instead*.

### 4.4 The tab and opening

The tab shows the tree under its root, a ◷ beside a bookmark with a
stored time and a ⚠ with words beside a record this version cannot use;
choosing shows the details without moving the chart; opening is
explicit (Open, Enter, or a double click). Deleting a folder asks to keep
what it holds in the parent, to delete everything, or to cancel, and the
question counts the bookmarks, folders and records needing attention
through every folder inside.

**Opening** (`BookmarkOpener`, measured on the atlas's real controller,
chart, module host, page inventory, chart context and working selection):

1. a stored place and time are installed together, in one context
   installation; an absent part is left as it was; `PlaceStore` is never
   written (C-2);
2. a Solar System subject's switch is turned on, a moon's by its
   planet's;
3. the view recentres with the main object as the target; a closer field
   opens only where it is offered, otherwise 1°;
4. the page is painted, and the main object is selected through the
   page's own inventory; with no main object the selection is unchanged;
5. each subject is reported: drawn, hidden by a chart option, fainter
   than the limit, outside the page, not drawn at this time, or not in
   the catalogue - kept as saved, never substituted.

**The star primary's privilege (C-3)** is the one rendering change of
the road: a star named as the page's target is drawn, labelled and
counted visible even when fainter than the reader's limit, in
`ChartRenderer`, `LabelGeometry` and `PageInventory`, as a deep-sky
target already was. No other star is revealed and the limit is not
raised. No committed chart page names a faint star as its target, and
none moved (section 6.2).

### 4.5 Export, restore and recovery

- **Export Bookmarks…** writes the library's canonical document - every
  record, including those needing attention - to a chosen file, asking
  before replacing one.
- **Restore from File…** refuses a malformed, foreign or newer file with
  its reason and changes nothing; otherwise states what the file holds
  and replaces the library only on the reader's yes, keeping the current
  one beside it. Restoring that kept file undoes the restore, byte for
  byte. Refused over a newer library; allowed over an unreadable one.
- **An unreadable library** offers *Restore Backup…*, showing the
  backup's date and contents first, and *Restore from File…*.
- Counts read properly in both languages, through choice patterns
  ("1 bookmark in 1 folder", "1 bokmerke").

## 5. Every stopped, failed, superseded or incomplete run

Every record is kept under `~/juranometria-runs/runs/`, unchanged; nothing
was rerun into green. **No gate run of this road stopped, and no CI run
failed**: the study's planned run and the implementation's seven gates
each qualified on the first attempt, and both pull requests' CI was green
on its first run.

| run | what happened | how it was continued |
|---|---|---|
| the study's evidence contracts under `-Xlog:gc*` (`heap-497-study`, 2026-11-02) | both routes **breached** on all fourteen study sketches, which did not reproduce across the contract's two runs in one process: the sketches were built before the application theme was first applied | the theme applied before each sketch is built and the default font restored; both routes rerun clean (`j497-devcheck/evidence-contracts-2.log`, `-ci-2.log`); the heap figures of the breached runs kept as valid measurements, the work being the same and full length (`j497-devcheck/README.md`) |
| this handover's first heap invocation (`heap-683-ae7260ce`, 2026-11-03) | stopped at once: the build step named a make target that does not exist (`compile`); nothing was measured | rerun with the right target (`classes`) into the same record |
| this handover's pre-flight on `1969cc6d` (`j683-preflight`, 2026-11-03) | the full suite **failed one test**: the Place and Time sheet's capture refused, its window settling at 326 px where 420 was established - the #362 capture-refusal family, the same kind of transition as #657's - with 49 desktop-focus aborts and none unexplained; the gate 6 and 7 replays exact and clean | not rerun; the refusal's record kept with the pre-flight; the owner's gate plan for the amended head states how gate 1 treats that family |
| development checks during the stages | each found by a focused or full-suite check before any gate request: the tool's study test shadowing the production document test of the same name in the test runner (renamed `BookmarkDocumentFormatTest`); duplicate test ids from a per-open counter (one shared counter); the language ledger's bound (raised once for the road, 170 → 190, with its reason); new `doClick` calls (replaced by package-visible methods); the control-explanation test and four photographers learning the new menu; the companion's tab pins from two tabs to three; the test-evidence hand-off count re-quoted (1 132, 1 153, 1 156, 1 161); "1 bookmarks" (choice patterns); the Library button clipped at 360 px (moved to its own row); a would-be 25th unpaired look-and-feel photographer (the bookmark sheet restores the look and feel) | each fixed before the stage's candidate or the gate request |

Gate 1's aborts were 12 on both runs; every record's list of unexplained
aborts is empty - each is the desktop-focus family.

Lessons this road confirms rather than adds: the pre-flight of the 6.1.0
road (the full suite and both evidence routes as development checks
before any gate request) was followed at every stage and is why no gate
run stopped; a study's sketches are built after the theme is applied,
as every interface photographer's are.

## 6. Routes, evidence movement and performance

### 6.1 The routes the pull requests took

| PR | head | local gates | CI `test` workflow | merged | landing |
|---|---|---|---|---|---|
| #680, the study | `79e5b865` | 1 and 3-7 OK by plan, 2 skipped (18:40 → 19:33) | 41501779121: green, 41.9 min | 2026-11-02 19:44 UTC → `0b61af47` | test 41512691379, pages 41512691156, both success |
| #682, the implementation | `25dd7119` | all seven OK (08:06 → 09:02) | 41600354856: green, 48.4 min | 2026-11-03 11:01 UTC → `ae7260ce` | test 41624078273 success |

Both landings were structurally verified on all four points under #494:
second parent the accepted head; first parent the qualified base
(`6223cdab`, then `0b61af47`); tree the qualified trial merge's
(`98baa8c5` = `9e9e71c2`'s, then `43231533` = `35128432`'s); the test,
app-image and dist runs green on that trial merge, on the wide route
(`landing-680.md`, `landing-682.md`).

The suite stood at **2 585** tests at 6.1.0, **2 590** on the study head
and **2 641** on the implementation's.

### 6.2 What evidence moved

From `6223cdab` to `ae7260ce` the change is **123 files, +9 916 / −87
lines** - the study 25 files (+3 471), the implementation 100 (+6 449).
Under `docs/studies`, **66 files** moved, none deleted: `bookmarks` 17
(new: the study's measurements, sketches and examples), `interface-
language` 44 (9 new - the bookmark sheet's photographs and its strings -
and 35 regenerated menu, toolbar and companion sheets in both
languages), `control-explanations` 2, and one file each of
`test-evidence`, `language-ledger` and the provenance record, whose
promoted artifacts went from **927 to 950**. Thirty-three source files
are new (`juranometria.bookmark`, `juranometria.ui.bookmark`, the
study's two tool classes, the bookmark photographer, the icon) and
twenty-four changed, among them both language files with **194 keys
each** appended at the end, the pinned lines untouched. Eight test
classes are new.

**No released chart output moved.** Every ladder, sheet, regional,
overview, globe and printable page reproduced under gates 3 and 4 on
`79e5b865` and `25dd7119`.

The language ledger's bound stands at 190; the test-evidence
measurements are quoted at **1 161 explicit hand-offs** and **950
renderer-drawn artifacts**, with 36 evidence executables touching
process-wide state and still 24 unpaired; the packaged licensing
inventory is **16 resources**.

### 6.3 Performance, measured

Local gates on this Mac: the implementation's seven gates **56.0 min**,
in line with 6.1.0's 56.4-56.5; the study's planned run of six gates
**52.9 min**. CI's `test` workflow: 41.9 and 48.4 min. The whole
road, from the owner's opening instruction on 2026-11-02 to the second
landing (2026-11-03 11:01 UTC), took about one day of calendar time.

## 7. What did not change

- **Chart contracts.** No released chart page moved (section 6.2). The
  one rendering change is the star target's privilege, reached only
  when a bookmark names a faint star as its main object.
- **Persistence.** No Preferences key was added, changed or migrated:
  the bookmark document is a new file in the user's data folder, outside
  the `juranometria` node, and opening a bookmark never writes
  `PlaceStore` or any chart option. The existing stores and their
  journeys ran unchanged.
- **Keyboard.** Every existing shortcut and mnemonic is where it was;
  two are new (Command-D, Shift-Command-B) and the Bookmarks menu takes
  B; the control-explanation test reads every shortcut in the table.
- **Accessibility.** Every new control has an accessible name and an
  explanation in both languages; the toolbar button is icon-only with a
  tooltip; record states are told with a symbol and words, never colour
  alone.
- **Export.** No chart export surface changed; the bookmark export is
  new and its own.

## 8. The release rehearsal, the heap, and the owner's separate decisions

As for 6.1.0, the rehearsal is what the gates and the CI workflows
already do, recorded rather than claimed, on this handover's head.

- **The integrated evidence flush** is gate 3 and gate 4 on this head:
  the bookmark sheet and the regenerated menu, toolbar and companion
  sheets in both languages, the study's sketches, the test-evidence
  measurements, regenerated on the integrated main and held to their
  committed bytes.
- **The packaged release journey on every platform image** is the
  `app-image` workflow's four images on this handover's trial merge;
  locally, gate 5 runs the packaged acceptance with the inventory at 16.
- **The dist archive built twice and verified** is gate 6.
- **What the evidence-contract process retains at 2 GiB**, measured on
  main `ae7260ce` in the handover's worktree with #676's exact commands
  under `-Xlog:gc*` (record `~/juranometria-runs/runs/heap-683-ae7260ce/`):
the canonical route (`make evidence-contracts`) finished in **18.6 min**
  with **4,752** collections and **1,180** concurrent marking cycles, no
  Full GC, **4.6 s** of total pause (the longest 22 ms), and kept at most
  **1,526 MB** live after any collection. The portable route
  (`evidence-contracts-ci`) finished in **20.1 min** with **5,042**
  collections and **1,161** cycles, **4.9 s** of pause (the longest
  26 ms), and kept at most **1,548 MB** live after a collection. Both
  committed to the 2,048 MB cap, and both reported *EVIDENCE CONTRACTS
  OK*. **For the first time in these measurements, the portable route
  ran one Full GC**: at 657 s a humongous allocation found no contiguous
  free regions, a mixed collection could not make room, and G1 compacted
  the heap - 1,460 MB to 930 MB in 25.5 ms. It was fragmentation of
  the large-object regions (842 of them in use just before), not
  exhaustion: live data after the compaction was 930 MB.

  The road's four measurements of the portable route's peak live data:

  | head | portable peak live | Full GC |
  |---|---|---|
  | 6.1.0's main `4af96509` | 1,915 MB | 0 |
  | the study tree (2026-11-02) | 1,830 MB | 0 |
  | stage 4's tree `71bc3840` | 1,626 MB | 0 |
  | this main `ae7260ce` | 1,548 MB | 1 |

  Reading: the peak falls as the evidence grows, so it is a sampled
  figure that depends on when collections land, not a fixed property
  of the tree; the bookmark evidence did not narrow the margin by any
  measure. The one Full GC is a different signal: the large rendered
  images are now numerous enough that G1 can run out of contiguous
  regions under a 2 GiB cap. It cost 25 ms and changed no output.
  **The owner's ruling** (2026-11-03): the heap stays at 2 GiB for
  6.2.0 - the measured peak is below the previous portable peak, and a
  short collection caused by fragmentation is not by itself an
  out-of-memory warning. The single 25 ms full collection and the
  reduced headroom are recorded here as the limit this release carries
  forward, beside 6.1.0's; both routes are measured again before the
  next evidence-heavy road.

Then, as 6.1.0 was done: no `VERSION` change, tag or publication in this
issue. Each of the following is a separate owner decision, asked for in
turn and never inferred from the one before:

1. the version number and the `VERSION` bump with its changelog date and
   every version-fed regeneration;
2. the release pull request, and its merge under #494;
3. the release workflow's manual-dispatch rehearsal, which publishes
   nothing;
4. the local annotated tag;
5. the tag push, which publishes - `release.yml` has no gate;
6. the public verification, and the close-out of the Sprint 56 milestone
   and #683.

## 9. What 6.2.0 contains, and what it explicitly does not

**Contains:** bookmarks - the Bookmarks menu, Add Bookmark… and Show
Bookmarks with their shortcuts, the toolbar button, the Bookmarks tab
with folders, opening by the ruled restoration rules, editing, export,
validated reversible restore and recovery; the star target privilege.

**Does not:** merge-import, cloud, server, API, sync, a session planner
or analytics; chart appearance restored with a bookmark (ruling 2);
a planetary moon as a main object (none is selectable yet); a *Now*
button for an observation's time; meteors, telescope fields (#617), the
article library in the application (#598), paths over a time range
(#553), the configuration diagram (#554), Pluto or any astronomical
feature; a change to any catalogue, service, pack, chart page,
preference key or existing shortcut.

## 10. Still open after 6.2.0

The README's *Using the atlas* section gave five pre-6.1 routes under
**View** (Inspector, Place and Time, Sun, Zoom, Chart Options), which
6.1.0 had moved to the Chart and Solar System menus; found by this
handover and, on the owner's amendment, corrected on its head, so the
version bump carries no unrelated documentation repair.

Open issues: #613, #617, #618, #598, #594, #589, #571, #566, #564,
#554, #553, #417, #387, #373, #370, #362, #356, #355, #351, #339, #327,
#326, #324, #295, #293, #282, and the Milky Way sprint #189-#192 with PR
#199, blocked on licensing. #497 and #681 closed with #682. The owner's
7.0.0 outline remains a planning document; its later topics are ruled
separately, each on its own road.
