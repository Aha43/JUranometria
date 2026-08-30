# Sprint 13 handover — Name the stars

Sprint 13 (issues #112–#116, milestone 13) gave the atlas's stars
their identities: a reviewed decision, a reproducible identity pack,
offline search by name and designation, and a restrained scale-
sensitive label pass — closing the gap the last four sprint
handovers named first ("the stars themselves remain anonymous").
This handover is the coder's account for the independent review
before 0.13.0.

## What Sprint 13 delivered

- **The decision** (#112, PR #117): `docs/decisions/star-identity.md`
  — source (d3-celestial `starnames.json`, BSD-3-Clause, the same
  pinned commit as the constellation geography), the HIP-through-
  raw-Tycho-2 join with every exception category measured, the
  five-column data model, the search grammar, the scale table, the
  layer order and collision rules, the searched-star guarantee, and
  the accepted single-label M31 reference change. The study renders
  through shared production geometry and verified pinned inputs.
- **The pack** (#113, PR #118): `make import-star-identities`
  generates `src/resources/catalog/star-identities/` — 4,805 rows
  (539 traditional names, 1,967 Bayer, 2,649 Flamsteed), refusing to
  write on any drift from the reviewed counts, cross-checking every
  constellation membership, verifying the bright-sky tiles in both
  directions before trusting a component-selecting magnitude, and
  byte-identical across runs. Checksummed manifest, notices, About
  inventory, licensing surfaces.
- **Load and search** (#114, PR #119): `StarIdentity` (immutable,
  structured, never a display string) attaches to `Star` at tile
  load through the verifying `StarIdentities` loader; `LocalSearch`
  implements the decided grammar — names by prefix, Bayer as Greek
  or spelled letter plus constellation (abbreviation and genitive),
  Flamsteed as number plus constellation, superscript component
  digits folded — with ambiguity listed, never silently resolved,
  and every star display line carrying the full identity.
- **The labels** (#115, PR #120): one deterministic pass, brightest
  first with the stable TYC tie-break, yielding to deep-sky labels
  and the title block (prefer omission); the searched star's best
  identity always drawn, exempt from thresholds and collisions, no
  new symbol; one new Chart Options control ("Star names and
  identifiers", default on, repaint-only); the M31 reference
  deliberately updated with its one accepted label.
- **The journey** (#116): `NamedStarJourneyTest` walks the real
  production paths — search field, results popup, View menu, dialog,
  controllers, persistence — from the released M31 page through
  Betelgeuse ("betel"), Rigel ("beta orionis"), Polaris (picked from
  the listed "1 umi" choices), Acrux ("alpha crucis"), and the
  exemption star 35 Cru, and back to the exact released default.

## Corrections made during review, recorded honestly

- **The Crux genitive erratum** (PR #119): the source's `gen` field
  for Crux repeats the nominative. The constellation pack now
  corrects it as a recorded erratum — declared in the manifest and
  notice, guarded so the correction fails loudly if the source
  drifts — because canonical forms ("Alpha Crucis") are part of the
  search contract. Found by the reviewer; fixed at the generated-
  data boundary, not in the search layer.
- **The wide-field name limit rose from V 2.0 to V 2.5** (PR #120):
  the owner's own reading of the atlas exposed that the 2.0–2.5
  band holds 43 asterism-anchor names (Alpheratz, Mirach, Dubhe,
  Mizar…), so figures sat anonymous at 36° and names popped across
  zoom. Recorded as an adjustment in the decision; only `orion-36`
  changed among the studies.
- **Source anomalies carried as unknowns, never invented**: nine
  designation-less variable stars carry NSV catalogue fragments in
  the source's constellation field (counted in the pack manifest);
  274 joined rows carry no packed identity fields at all and load
  as rows without an identity.

## Verification

- 268 tests, 0 failures on a display; headless CI discovers 268,
  passes 264, and aborts the four display-dependent tests visibly by
  assumption (the three acceptance journeys and the dialog
  single-instance test); the required GitHub `test` check is green
  on every sprint PR.
- Clean bootstrap re-verified this session: `lib/` deleted,
  `scripts/download-libs.sh`, full suite green.
- `make chart-image` byte-identical to the deliberately updated M31
  reference; `make import-star-identities` and
  `make star-identity-study` regenerate byte-identically over the
  committed pack and pages (the chosen study pages are now the
  production renderer's own output).
- Packaged `make jar` + `java -jar` launches, light and `--dark`,
  verified on macOS this session.
- Budget: identities load 27 ms; atlas + search build 157 ms; total
  heap ~25 MiB (unchanged in order); warm 36° render 3.3 ms; warm
  identity queries ~4 ms.

## Residual risks, stated honestly

- **Label placement is single-candidate**: right of the dot at
  baseline height; collision means omission, never an alternate
  side. Tight pairs can leave both unlabelled where a smarter
  placement would fit one (recorded in the decision as the known
  refinement, with label-vs-dot touching accepted).
- **Genitive search accepts what the geography carries**. One
  erratum (Crux) is corrected and guarded; an undiscovered wrong
  genitive elsewhere would make one canonical form miss until
  recorded the same way.
- **Search display lines and popup ordering** rank by label text
  within buckets, so a rename in the source could reorder ambiguous
  lists across a pack refresh — deterministic per build, not across
  source updates.
- **The identity pack is non-positional**: it trusts the bright-sky
  pack's TYC identifiers. A future deeper star pack must re-run the
  join (the generator refuses drifted counts, so this cannot happen
  silently).

## Sprint review answers

- **Can a reader find and name the stars?** Yes — by name prefix,
  Bayer, Flamsteed, and canonical genitive forms, fully offline;
  the chart answers at the decided scales, and search, title, label,
  and catalogue provenance agree on the same TYC star.
- **Did naming cost restraint?** No: wide pages carry only the
  asterism anchors, collision rejection keeps dense fields quiet,
  and the everything-page remains committed as the rejected
  alternative.
- **Did naming cost honesty?** No: unknowns stay unknown at every
  layer, source errors are recorded errata or counted anomalies,
  ambiguous queries list rather than guess, and a suppressed label
  is a collision outcome the tests prove deterministic.
- **Did naming cost architecture?** No: identities are assembled
  data (option-free by type), the label pass is one renderer seam
  gated by one option, and the policy classes stay option-free.
- **What next?** **Sprint 14: pointer-centred wheel zoom** — the
  recorded route, four handovers standing; navigation is the last
  place the atlas feels less than physical, and no evidence this
  sprint surfaced anything more urgent. **Sprint 15 remains the
  planned 1.0 stabilization/release sprint** — planned, not
  opportunistically begun.

## Process expectations

The established pattern: this handover accompanies the open sprint
PR; the independent Codex review lands as
`docs/reviews/sprint-13-codex-review.md`; findings are fixed on the
PR; both documents are committed with the fixes; then merge, close
milestone 13, and cut 0.13.0.
