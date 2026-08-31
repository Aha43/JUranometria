# Sprint 17 handover — Letter the stars

Sprint 17 (issues #153–#156, milestone 17) turned the Bayer and
Flamsteed identities that Sprint 13 bundled and made searchable into
restrained chart notation: the constellation pages now name the
pattern they draw. This handover is the coder's account for the
independent review before 0.17.0.

Sprint 16 remains paused by the owner's decision with three issues
open (#145 audit, #88 release automation, #146 packaged journey);
0.16.0 shipped its distribution work early so the atlas is
installable while that finishes.

## What Sprint 17 delivered

- **The decision** (#153, PR #157): `docs/decisions/bayer-notation.md`
  — measured over the released pack, not imported by instinct.
  1,967 Bayer designations (1,519 Greek, 448 post-omega Latin, 389
  with component digits), 2,649 Flamsteed numbers, 539 names of
  which 462 are also lettered, and 68 bare letters of which 67 are
  shared across constellations (`α` spans 84). From that: the
  letter alone with its component digits raised; names and letters
  together where both qualify; **no constellation qualifier**
  (letters draw where figures and boundaries already resolve them,
  measured at 0–4 ambiguities per page); **Greek everywhere, Latin
  from 8° in**; and Bayer letters reaching the 24–36° pages at
  V ≤ 3.5 — the headline. 22 study pages, including the rejected
  `qualified` and `everything` alternatives.
- **The rendering** (#154, PR #160): `StarLabelPolicy` carries the
  notation and thresholds; `ChartRenderer.starLabelPlacements`
  extracts the pass's *decision* as the seam production draws from
  and studies count, so no second collision implementation exists.
  The M31 reference changed deliberately by **81 pixels in one
  14×10 box**: `35` became `ν`, the same star in better notation.
- **The controls** (#155, PR #161): three independent Chart Options
  — Star names, Bayer letters, Flamsteed numbers — with the policy
  kept option-free by reporting which *forms qualify* while the
  renderer composes what the reader *permits*; persistence with the
  decided precedence, so a pre-split store carries its single
  choice into all three layers and each layer then answers to its
  own key forever.
- **The journey** (#156): `LetteredStarJourneyTest` walks the real
  paths — reading Andromeda (`Alpheratz α`, `Mirach β`,
  `Almach γ¹`), Orion (`Betelgeuse α` … `Saiph κ`, `π³`), and Ursa
  Major (`Dubhe α`, `Merak β`, `Phecda γ`, `Alioth ε`) **by their
  notation without searching**; walking a band boundary (σ Ori
  absent at 36°, present at 8°); proving search, title, catalogue
  identity, and drawn notation agree about Dubhe; separating names
  from identifiers through the real dialog (repaint-only, the
  searched star keeping `Dubhe α`); dragging across a boundary
  region; a restart honouring exactly the confirmed layer; and
  Restore Defaults + Home ending on the reviewed default page.

## Findings and corrections, recorded honestly

- **The census counted stars, not constellations** (gate review):
  corrected to distinct constellations per letter, and now
  *executed* by a test so a pack change fails the gate rather than
  leaving stale prose.
- **The study protected grid labels that production overpaints**
  (gate review): the coordinate-grid decision makes grid ink
  subordinate, so the study was quietly improving on production;
  mirroring it measured one more letter on the Pleiades page.
- **Counting by policy overstated every page** (#154): the first
  implementation counted what the policy *selected*, not what the
  renderer *drew*. Rather than re-implement the collision loop, the
  placement decision became the shared seam — after which the
  study's production pages reproduced the reviewed numbers exactly.
- **Flamsteed numbers keep their released limit** because raising
  it put a bare `32` beside `M 32` — numbers read as Messier
  numbers. That measurement is also why the controls are three, not
  one.
- **A workflow defect surfaced en route** (#158, PR #159): the
  app-image comparison aborted while counting differences, because
  `diff` exits 1 under `set -e`. Fixed with a script that separates
  identical, recorded-difference, and genuine error, proven over
  doctored directories.

## Ripples

The Sprint 13 star-identity pages and the Sprint 15 coordinate-grid
pages are production output by their own decisions, so the new
notation changes them: **474 pixels on the widest Orion page**, all
of it identifier ink (letters appearing, `35`→`ν`-style notation
changes). All 18 pages regenerated and committed; their decisions'
measured numbers are unaffected, since those were measured against
their own policies at the time.

## Verification

- 324 tests, 0 failures on a display; headless CI aborts the seven
  display-dependent tests visibly by assumption; the required
  `test` check was green on every sprint PR.
- Clean bootstrap re-run this session (`lib/` deleted, full suite
  green); `make chart-image` reproduces the deliberately updated
  M31 reference; every committed study page in all three families
  regenerates byte-for-byte.
- Native application image rebuilt and verified: launches with **no
  system Java**, light and `--dark`; packaged About and
  preference-change acceptance green; module inventory, runtime
  legal notices, and licensing inventory asserted; portable archive
  verified with its packaged smoke render.
- The four-platform native matrix and three-platform portable
  verification were green on PRs #160 and #161.

## Residual risks, stated honestly

- **Unqualified letters rely on context.** Where figures and
  boundaries draw, they resolve; on the ≤ 8° pages no geography
  draws, and although measured ambiguity there is 0, a reader
  panning a narrow field across a boundary sees letters without
  their constellation. Qualification remains a reversible decision.
- **Latin Bayer letters still look unusual** at 8° (`c`, `d`, `e`
  beside numerals). They are correct designations; the hold-back
  keeps them off the constellation pages, which is where they
  looked worst.
- **Pairs lengthen labels**, so a name+letter box collides slightly
  more readily than a name alone; measured rejections stayed at 0–1
  per studied page, but a denser future pack would feel it first.
- **The M31 reference now depends on a Bayer threshold**: any
  future change to the letter limits touches it again. That is
  visible by design rather than hidden.

## Sprint review answers

- **Can a reader read the sky by its notation?** Yes — Orion,
  Andromeda, Ursa Major, and Crux all state their Bayer sequences
  on the constellation pages, and the journey proves it without
  search.
- **Did notation cost restraint?** No: wide pages carry the anchors
  plus a handful of letters (Orion 36°: 7 pairs, 4 letters, zero
  rejections), and the rejected `everything` page stays committed
  as the contrast.
- **Did it cost honesty?** No: notation comes from structured
  catalogue values, a degenerate designation is omitted rather than
  drawn broken, ambiguity is measured rather than assumed away, and
  every count the decision quotes is now executed by a test.
- **Did it cost architecture?** No: one policy (still option-free),
  one shared placement seam that removed a duplicate rather than
  adding one, three options in the established contract.
- **What next?** **Sprint 16's remaining 1.0 work** — the audit
  (#145), release automation (#88), and the packaged 1.0 journey
  (#146). Nothing in this sprint changed the 1.0 contract; the
  identifier controls and notation simply become part of what that
  audit checks.

## Process expectations

The established pattern: this handover accompanies the open sprint
PR; the independent Codex review lands as
`docs/reviews/sprint-17-codex-review.md`; findings are fixed on the
PR; both documents are committed with the fixes; then merge, close
milestone 17, and cut 0.17.0.
