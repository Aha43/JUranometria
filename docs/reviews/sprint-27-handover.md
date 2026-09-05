# Sprint 27 handover — Sirius: the atlas, used

**Issues #252, #253, #257, #258, #260, #261, #262.** Written for the
final sprint review. Nothing here is merged, tagged or released on
the strength of it.

## What Sprint 27 delivered

The sprint the atlas went public and grew its first cross-page
reader state. The gallery decided its own curation and editorial
voice before a page existed, then went live under a least-privilege
Pages deployment; the On-this-page table's chart-status column
earned its width; and the working selection arrived in three
gated steps — semantics decided, the session model built, and the
four reader surfaces rewired onto the one truth, with the Sprint 24
page-scoped adapter retiring with its last consumer.

| # | what it was | PR |
|---|---|---|
| 252 | the gallery gate: curation, editorial voice, evidence classes for public pages | [#263](https://github.com/Aha43/JUranometria/pull/263) |
| 253 | the gallery live: GitHub Pages, main-only deploy, least privilege | [#264](https://github.com/Aha43/JUranometria/pull/264) |
| 257 | the compact Chart column: five decided words, meaning-ordered sort | [#265](https://github.com/Aha43/JUranometria/pull/265) |
| 258 | the working-selection gate: gesture table, captured transactions, ink rule | [#266](https://github.com/Aha43/JUranometria/pull/266) |
| 260 | the session model: `WorkingSelection`, no pruning, no persistence | [#267](https://github.com/Aha43/JUranometria/pull/267) |
| 261 | the surfaces: chart, table, search and Inspector as four views of one set | [#268](https://github.com/Aha43/JUranometria/pull/268) |
| 262 | this close: the reader's walk, the evidence runs, this handover | — |

## The required journey, walked as a reader

**The public gallery, live.** Every page of
`https://aha43.github.io/JUranometria/` was fetched from the
public host: the index and all nine slide pages answer 200, every
slide the index names resolves, and no page references a remote
asset — one stylesheet and one script, both served from the site
itself. The one script is the reviewed arrow-key navigation whose
own comment states its contract ("Everything works with it
absent"): navigation is plain `rel="prev"/"next"` anchors, so the
whole site reads and navigates with JavaScript off. All ten pages
carry the `width=device-width` viewport declaration for the mobile
layout the #252 gate reviewed. The downloads link points at the
immutable `releases/tag/v1.7.0` — the current release — and the
repository link sits beside it, exactly as the #252 re-review
required. Both module rooms (On This Page, Place and Time) were
read through their direct URLs.

**The shipped application, driven by hand.** The packaged arm64
image was built from the merged head (`PACKAGED ACCEPTANCE OK`
inside the build) and launched; the machine's real `juranometria`
preference node was exported before the session and restored after
it. Every gesture below was a real pointer click or keystroke on
the running application, with the state read from screenshots and
from the store at the time:

- **An ordinary click inside M 31's ellipse** offered its two
  candidates (M 32 current), drew the ring on M 32, and the
  Inspector's **Working set · 1 object** appeared: ◉ NGC 221 with
  its remove control and Clear selection — the reviewed section,
  live. **Cycling the chooser to M 31** retargeted member and lead
  in one transition: the working set read ◉ NGC 224, the ring
  became the M 31-sized ring — the honest consequence the #258
  gate weighed — and the candidate list stayed open.
- **Accumulate on** (the visible toolbar control), the same
  crowded point clicked again: the captured toggle added NGC 221
  against the pre-click snapshot — **Working set · 2 objects** in
  joining order, both rings on the page, the lead on the
  toggled-in member.
- **Search carried the set across pages**: "M 42" recentred to
  Orion and added NGC 1976 (leading, ringed), with NGC 224 and
  NGC 221 labelled *off this page* in words; "Betelgeuse" made it
  four members over three pages, the star ringed and every
  deep-sky member off-page.
- **Choosing the off-page NGC 221 in the working set** made it
  lead: the facts panel answered honestly — its coordinates and
  "Not on this page any more." — while membership stood at four
  and the drawn member kept its one ring.
- **Remove-one, both ways**: the non-lead NGC 1976 left alone;
  removing the lead NGC 221 passed the lead to the last-marked
  remaining member (the Betelgeuse star), and the facts followed.
- **The palette changed under a live set**: View → Chart Options,
  Chart tab, Black sky, OK — the page went black, the ring turned
  to the black-sky selection ink, and **membership did not move**.
- **Restart**: quit and relaunch came back wearing the reader's
  black sky with **no working selection** — nothing selected, no
  Working set section — the persisted choice surviving beside the
  session state that never persists. The store read
  `chart.palette = black-sky` between the sessions and was then
  restored to the reader's own values.
- **The compact table, read and sorted**: On this page at the
  ordinary Inspector width listed 12 rows in four columns with no
  horizontal scrolling — "not recorded" and "No mark" whole — the
  counted line beneath ("and 77 further stars, none of them
  named"), and a real click on the Chart header sorted by meaning:
  every Shown first in stable default order, No mark last.

**The integrated closing journey** (the closing review's
requirement) runs the filed steps the manual walk had left to
component evidence, in one committed display journey —
`theClosingJourneyWalksEveryFiledStepThroughRealControls` in
`WorkingSelectionSurfacesJourneyTest` — through real reader
routes, failing if any transition is removed:

- an ordinary chart click and an Accumulate table click — the
  table reached through the Inspector's own mode chooser, because
  a card the chooser has not raised is not on screen — build a set
  holding a **drawn and an undrawn member**; the treatments are
  proven **on the component's own painted pixels** against the
  untouched page: the drawn member's ring on its own
  circumference with no cross arm at its centre, the undrawn
  member's cross on both arms with the diagonal a ring would
  cross clean — once each — with the registry and projection
  checks kept beneath as diagnostics;
- the Chart column is **sorted and then dragged to the front at
  its real header, through the shared `ReaderInput` routes** —
  the click, and a new premise-proving drag whose endpoints must
  both lie inside the visible header — and holds its measured
  width by model identity wherever it sits; under **enlarged
  application text** (20 pt, on the shared guard) the width rule
  holds again after a real resize — whole words, never cut — with
  membership, order and lead fixed throughout;
- search **carries the undrawn member off-page** as a member, not
  as ink (no contribution of either kind remains), the working
  set labelling both absentees in words;
- under that same live set, the window carries the **production
  `AppMenuBar`**: Chart Options opens from its real View-menu
  item (the recorded menu-item convention) and its real checkbox
  hides and restores an ordinary family — the member's **ring
  leaves the painted page and the cross arrives, then back**, the
  set never moving; **Black sky** is chosen and OK persists it;
  and the theme is changed **both directions through the real
  File-menu Settings item**, the dialog's own appearance controls
  and OK, on the shared guard — membership, order and lead
  identical after every one of these presentation changes;
- **Clear selection** through the real control empties every
  surface, and a **second session** built from the same store
  begins with a clean working selection while the persisted black
  sky is in force.

Beneath it, the narrower committed evidence stands unchanged —
table toggles and both range transactions, the ambiguous
transactions, the mutation checks
(`WorkingSelectionTableGestureTest`,
`WorkingSelectionChartGestureTest`, the retold Sprint 24
journeys) — all green in the runs below.

## The required evidence, run and counted

**Complete suite from a clean checkout** (depth-1 clone into a
fresh directory, dependencies fetched by
`scripts/download-libs.sh`, Apple-silicon Mac, Darwin 25.5.0,
Homebrew OpenJDK 21, display present so the display corpus runs
rather than aborts): **140 containers found / 140 successful /
0 failed / 0 aborted; 824 tests found / 824 successful / 0 failed
/ 0 aborted / 0 skipped**; 46.9 s wall, cold, the integrated
closing journey included.
The required xvfb display job runs the same corpus in CI and is
green on the merged head (112 s on the approved head; 87–115 s
over the sprint's last four runs).

**Evidence contracts in the environments the contract names.**
From the bare clean checkout, `make evidence-contracts` **fails
loudly by design**: `VERIFICATION INCOMPLETE` naming the
constellation and star-identity families' gitignored inputs and
their exact fetch commands. With the pinned inputs supplied it
runs green in 15.5 s: 108 reproduced, 77 reproduced via generator
build output, 8 pinned residue, 17 inspection images regenerated
identical, 17 unchanged, 3 session photographs, 1 fixture and 2
files checksum-verified, 13 captured-evidence screenshots
digest-verified, 1 widget-measured report held to substance —
`EVIDENCE CONTRACTS OK`.

**Native images and packaged acceptance.** The arm64 image builds
green locally (9.7 s warm) with `PACKAGED ACCEPTANCE OK`,
including the on-this-page step migrated to the session model: 56
entries, the mark on the object the page does not draw, 2 692
pixels of selection ink laid down and **cleared to the byte**, and
a second session beginning empty while wearing the reader's stored
choice. CI built all four platform images green on the approved
head (75–117 s per cell) with the same acceptance inside each.

**Distribution verification** ran green on all three `verify`
platforms (39–42 s each).

**Release rehearsal** in its named environment: a manual `release`
workflow run
([33937219168](https://github.com/Aha43/JUranometria/actions/runs/33937219168))
— build, jar, all four images, cross-architecture smoke, all three
portable verifies, stage, agreement and rehearsal jobs green,
**publish skipped** exactly as the rehearsal contract promises a
manual run cannot publish.

**Pages build, links and accessibility**: the deployed site's
checks above, plus the committed `GalleryTest` corpus — byte
identity between repo pages and the deployed artifact's images,
curation rules, UI-labelling, no-remote-assets, link resolution —
green in every run.

## Timings, before and after

Before = the Sprint 26 close's recorded numbers (same conditions
stated there). After = this close (CI averaged over the sprint's
last four `test`-workflow runs and the approved head's
`app-image`/`dist` runs; local n=1 clean cold plus warm samples).

| check | before (1.7.0) | after (this close) | reading |
|---|---|---|---|
| CI `test` | 84 s avg, worst 98 (n=4) | 88 s avg, worst 92 (n=4) | 783 → 824 tests |
| CI `display` | 98 s avg, worst 117 (n=4) | 102 s avg, worst 115 (n=4) | three gesture/journey classes added |
| CI `image` cells | 82–139 s | 75–117 s (n=1 each) | flat within variance |
| CI `dist` verify | 43–46 s each | 39–42 s each | flat |
| local clean checkout, cold | 45.2 s / 783 | 47.8 s / 824 | ≈58 ms per test, unchanged |

No causation is claimed from these samples; runner variance spans
the gaps. The suite grew by forty-one tests — most of them real-window
gesture evidence — at essentially flat cost.

## The ordinary chart is byte-identical

No accepted Sprint 27 feature changes the core page. The released
M 31 reference comparison, every ink count and study baseline ran
green all sprint with no re-baselining; `WorkingCrossTest` holds
the unmarked page byte-identical with the module attached and the
selection empty; the packaged acceptance clears its selection ink
"to the byte"; and the module-absent path is the same chart it has
been since #216, proven by the same committed comparisons.

## Every review correction, by round

**#252 (PR #263, two rounds + the owner's editorial round).** The
narrow responsive captures included the maintainer's browser
chrome and unrelated personal workspace UI → cropped/recaptured
with digests updated and a structural corner-guard regression.
The gallery's release link pointed at a mutable page → the
immutable `releases/tag/v1.7.0`. The owner's own round set the
editorial voice — *describe what JUranometria lets the reader
see; never imply that other charts, tools or readers failed* —
recorded in the gallery decision as human editorial judgment, not
a banned-word test. A gate-era study image (m42-36deg) was caught
compositing geography over furniture and replaced by a
production-pass source, with `PRODUCTION_PASS_SOURCES` pinned.

**#253 (PR #264, one round).** The editorial rule claimed to
govern all documentation but lived only in the gallery decision →
generalised into `docs/development.md`'s documentation-review
guidance (describe; no unsupported superlatives or implied
criticism; acknowledge sources; a human judgment). After review,
the temporary `feature/gallery-pages` trigger and deployment
policy 59131153 were removed, leaving `main` the sole deployer.

**#257 (PR #265, one round, two P1).** Column widths were
reapplied by view position, so a reader's drag scrambled them →
widths keyed by model identity, held through resize and page
change. The Chart column's full question was attached to the whole
header → a shared wrapper header renderer gives it to exactly the
Chart header cell, travelling with the column and cleared for
every other header.

**#258 (PR #266, one round, two P1).** The additive ambiguous
click was undefined when candidates were already members → the
captured transaction: one toggle against the pre-click snapshot,
defined for every absent/present combination, cycling never
accumulating. The additive range defined growth but not
contraction → anchored recomputation, snapshot ∪ current range at
every extension and retraction, both joining the mutation checks.

**#260 (PR #267, three rounds).** Page changes still pruned the
new session model through the adapter's `replaceWith` → the
adapter became a page-scoped *view* that narrows what is shown and
never writes membership. Bulk routes accepted blank identities →
member validation in the model's own `Change`. Per-listener model
subscriptions let nested `pruneTo` broadcasts reach listeners in
different orders → one lifetime subscription and one serialized
view queue, with two-listener nested regressions both ways.
Off-scope model changes published duplicate identical views →
equality suppression at the shared queue boundary, with off-page
silence and visible-fallback-lead cases proven.

**#261 (PR #268, two rounds + a post-approval round).** The
Inspector mistook stars in the scene's query margin for objects on
the paper — and its new test asserted the defect → the inventory
(which lists every on-paper star, named or not) became the one
page boundary, with both directions premise-asserted and the ink
promise pinned: a selected unnamed star the limit hides gets its
cross, so the PR's claimed no-ink boundary was corrected rather
than accepted. Sorting failed to end the captured range
transaction → a row-sorter listener closes it without editing,
with a real-header regression whose stale-snapshot mutation fails
on both membership and joining order. Post-approval: the
display-classed table gesture tests still clicked a table in no
window, their local visible-rect assertion proving nothing → the
fixture shows a real window and every row and header click goes
through the shared `ReaderInput` route, which gained the
point-and-modifiers click; omitting the window fails on "is on
screen, in a window a reader can see", verified by mutation.

## Found beyond the filed work

- **The Sprint 24 journey still asserted navigation pruning** —
  and had only stayed green because the desktop was refusing
  window focus, aborting the display corpus. Retold to the decided
  semantics (membership travels; no transition; no off-page ink)
  and run green with the corpus executing.
- **A stale gesture note could have turned sorting into an edit**:
  a table press that changes no selection leaves its note for the
  sorter's restore event to consume as a reader gesture — found in
  self-review before Codex saw it, closed with a maintenance
  guard and a committed mutation check.
- **CI's headless toolkit refused the platform-modifier question**
  (19 failures on the first push): the mask now lives once in
  `SelectInteraction.toggleModifierMask()` with a headless
  fallback, and the table-gesture tests joined the display corpus
  because Swing's own table UI asks the toolkit during a press.
- **The search field keeps its text after a successful search**:
  during the walk, typing a second query without selecting-all
  first produced "M 42Betelgeuse" and an honest no-match. A reader
  repeating searches must clear the field by hand. Recorded as an
  observation, not filed.
- **The lead bridge learned positions from the answering model**
  so a star the catalogue does not name can still be answered when
  it comes to lead by removal — an edge found while hardening,
  closed before review.

## Public Pages provenance

The site is generated by `GalleryMain` from
`docs/gallery/manifest.json` — one source of truth — with images
byte-identical to the repository's reviewed evidence
(`GalleryTest` compares them on every run). Deployment is the
permanent least-privilege path: `.github/workflows/pages.yml`
triggers on `main` only, holds only `pages: write` and
`id-token: write`, and `main` is the sole branch the Pages
environment accepts (temporary policy 59131153 deleted after
#253's review). Chart imagery is production output under the
project's own licensing posture; the bundled Tycho-2 derivation
keeps its CC BY-NC 3.0 IGO consequences to the packaged
application, and the site links the immutable v1.7.0 release for
downloads rather than hosting any.

## Residual risks, stated

- **The search field's kept text** (above): a second search
  requires a manual select-all. Minor, reader-visible, unfiled.
- **The M 31-sized member ring** is the accepted consequence of
  reusing the one ring vocabulary, weighed at the #258 gate and
  seen live in this walk; the Inspector names the lead, so the
  large ring is identification, not emphasis.
- **The display corpus needs a desktop that grants focus**: on
  this machine overnight, every focus-dependent journey aborts
  with its stated premise until the desktop wakes. The aborts are
  honest and CI's xvfb job is unaffected; recorded so a future
  red-versus-abort reading starts from the right place.
- **Standing items carried forward**, unchanged: the portable
  JAR's Dock tile (a `java.awt.Taskbar` follow-up, recorded, not
  filed); the icns's absent plain 16/32 variants; the Milky Way
  parked unlicensed with no dark-ground treatment;
  the deep-sky-vocabulary report as the one byte-claim exception;
  captured-evidence digests pinning bytes, not truth.

## Version recommendation

**1.8.0.** Three reader-facing capabilities arrived: the public
gallery site, the compact meaning-sorted chart-status column, and
the cross-page working selection with its visible Accumulate
control and the Inspector's Working set section. Nothing was
removed and nothing broke compatibility — a minor version by the
project's semantic-versioning contract. The stores upgrade
silently: the working selection persists nothing by decision, so
a 1.7.0 reader keeps every choice they had and gains only what
this sprint added. No `VERSION` change, milestone close, tag or
release before the final review and the owner's explicit
instruction.
