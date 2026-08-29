# Sprint 4 review handover

Prepared 2026-08-29 for the pre-release Codex review of Sprint 4,
"Search and recenter" (milestone 4, issues #34–#36, PRs #37, #38, and
the PR closing #36). Written before that final PR merges. After
review the plan is: merge, close the milestone, release 0.4.0.

Read the earlier handovers first; this document covers only what
Sprint 4 changed.

## What Sprint 4 delivered

The atlas navigates: a person can find any bundled object or
coordinate and move the chart there, with coverage honesty enforced
at every layer.

1. **#34** — the celestial centre joined `ChartViewState` (default
   M31); `ChartViewController` gained `recenter(centre)` and
   `recenter(centre, field)`; `SceneAssembler` distinguishes the
   mutable chart centre from the fixed data centre and extends the
   coverage rule to offsets (offset + corner radius + 1.5° margin ≤
   the 10° cone), with `fits` and `widestFittingFieldDegrees`
   answering coverage questions without querying or painting, and
   letterboxing tightening as the centre moves off the data centre.
   `SkyPosition.separationDegrees` moved the haversine into the
   domain.
2. **#35** — `juranometria.search`: `LocalSearch` over immutable
   domain lists (source-independent), forgiving normalization
   (case/whitespace-insensitive, Messier ≡ M), decimal and
   sexagesimal coordinate parsing, exact > prefix > partial ranking
   with label-then-identity ties, deduplicated and bounded to eight
   immutable results, empty list for anything unhelpful. The shared
   `SkyFormat` was extracted so search results and the title block
   speak one notation.
3. **#36** — the toolbar `SearchField`: Enter resolves through
   `LocalSearch` (the toolbar parses nothing itself); a single result
   applies immediately, multiple open a keyboard-navigable popup
   (arrows/Enter/Escape native), messages ("No local match", "Beyond
   local catalogue coverage") appear as quiet disabled popup items.
   Selection recentres once through the controller under the sprint's
   stated policy: keep the field when it fits, else the widest
   complete step (visible in the synchronized readout), else the
   chart stays put. Reset restores the complete default and clears
   the search.

Tests grew from 108 to 122. The default reference image is unchanged
through the entire sprint.

## Worth extra scrutiny

1. **The apply policy's single notification** — `recenter(centre,
   field)` chains `recenteredAt().withFieldWidth()` inside one
   controller update. Confirm no path emits two notifications or two
   scene assemblies per selection.
2. **Popup lifecycle** — the field guards `popup.show` behind
   `isShowing()` so headless tests exercise wiring without a screen.
   Check the real-app lifecycle: stale popups on rapid re-search,
   Escape/focus behaviour, popup left open across a reset.
3. **Coordinate-form ambiguity** — two bare numbers ("3 4") are read
   as decimal degrees RA/Dec. Judge whether that surprises anyone
   typing something else, and whether the documented forms in the
   README suffice.
4. **The NO_FIT message wording** — "Beyond local catalogue coverage"
   is deliberately number-free. Judge whether it should name the 10°
   cone or point at documentation.
5. **Search index lifetime** — `M31Chart` builds `LocalSearch` once
   over the full coverage cone at first use, alongside the assembler's
   catalogue. Confirm the two cannot drift (both come from one
   `BundledCatalogue.load()`), and that EDT-only use holds.
6. **Result labels** — list items read "M 31 · NGC 224" when label
   and identity differ. Check the popup stays legible with the
   longest bundled common names.

## Sprint review answers

- **Does search feel like part of a quiet atlas?** Yes: one
  placeholder text field in the toolbar, no button, no dropdown
  chrome until asked; results are a small text list; failures are a
  single quiet line. Nothing blinks or autocompletes while typing.
- **Are normalization and ranking useful without surprises?** The
  forgiving forms all land on the expected object, and the one
  debatable rule is deliberate and tested: in a prefix tie, ordering
  by label puts "M 31" before "NGC 206" — the famous name wins.
- **Does every selected result produce a complete-coverage chart?**
  Yes, by layered construction: the field policy consults `fits`
  before moving; the assembler independently refuses any view whose
  query would leave the cone; letterboxing bounds the page; and the
  bundled rows are test-guarded to lie inside the declared coverage.
- **Is mutable-centre state ready for a pan sprint?** Yes. Pan is
  `recenter(current centre + small offset)` under exactly the same
  coverage rule; nothing else changes. The open question for that
  sprint is edge behaviour — clamping at the coverage boundary versus
  refusing steps — not machinery.
- **What is conspicuously missing from search?** Star names. The
  catalogue's stars carry only TYC identifiers, so "Mirach", "nu
  And", and Bayer/Flamsteed designations find nothing — the most
  visible data gap in daily use. A future catalogue sprint should
  consider a small name cross-reference (e.g. IAU named stars plus
  Bayer/Flamsteed from a redistributable source). Constellation names
  and boundaries are the other absence the product vision already
  anticipates.

## Process expectations

As before: findings become issues or PR comments; the reference image
is intentionally unchanged this sprint — flag any diff you can
produce from it as a finding.
