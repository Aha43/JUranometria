# The observer's N, E, S and W on the horizon

Issue #359, decided 2026-09-23. This record explains a regeneration
that looks enormous and is bounded: 212 evidence artifacts change,
and a three-variant measurement attributes almost all of it to one
resource-level wording correction, while the semantic feature itself
produces exactly the additional primitives expected.

## What the feature is

The Place and Time family's meridian module contributes four cardinal
marks - the observer's N, E, S and W on the mathematical horizon -
whenever the horizon is shown. Each is a `DirectionMark` carrying
identity and exact sky position and **no words**: the drawn letter
and the full spoken name are the page's language
(`PageWords.directionLetter` / `directionSpoken`; N/E/S/W in
English, N/Ø/S/V in Norwegian Bokmål), resolved by type in
`ReferenceInk`, whose `directionPlacements` is the one decision
paint inks and accessibility reads. The chart's accessible
description speaks each rendered direction's full localized name and
stops when the horizon goes off.

Placement is subordinate reference ink under the measured policy:
a cardinal letter yields to all text, furniture, grid notation and
deep-sky glyphs, may cross anonymous star dots (measured at 2-26 px
of interference on real pages, readable; the stricter alternative
left wide pages with no letters at all), takes one of four adjacent
boxes or the whole landmark is omitted, and the mark itself never
moves. An accepted cardinal outranks the layer's generic line names,
which relocate through their existing candidates - and only when a
cardinal is actually accepted. On the 180-degree globe the horizon is
the limb: the mark sits at its exact limb point as an inward-pointing
half-diamond (the limb law of #331 halves it honestly) with its
letter inside the mapped sky.

The page's own orientation wording was corrected with it, in both
languages, so the two easts cannot be confused: titles say
*Celestial north up · celestial east left* (*Himmelsk nord opp ·
himmelsk øst til venstre*), and the spoken page says it as a
sentence.

The teaching demonstration is selected, not authored: a bounded sweep
of 48 explicit instants at Oslo (2026-03-20, every 30 minutes) found
5 qualifying pairs, and the first - 05:00Z - is the frozen example:
matched horizon-centred 180-degree pages where E and W are visibly
attached and exchange sides while celestial orientation stands still.
Permanent tests pin that explicit instant; the sweep remains
discovery evidence.

## The blast radius, measured and attributed

Three build variants factor the attribution: clean `main`; main's
code with only the feature's language resources (the wording alone);
and the full feature. All 46 derived docs-writing generators, the six
build writers mapped to every promoted destination, 32 stdout
reporters captured explicitly, 20 platform records captured headless;
a failed or empty generator refused, never read as unaffected;
nothing inferred from an absent file.

The frozen measured-effect manifest is **212 paths**
(sha256 `7e2834d834b58679dd74c27e73f5cbfa5fe987d348ec3cac51aa368eaa235cab`):

- **Cardinal marks: 2 paths.** `globe-export/platform.md` - the
  exported globe's SVG gains exactly two text elements, none anchored
  outside the limb - and `globe-modules/platform.md`, two ink
  counts grown by the marks' own ink. The feature's whole
  artifact-level footprint.
- **Wording: 210 paths.** The caption rides every title-block page
  and the sheet exports' embedded text, across twenty studies.
  The wording-only variant reproduces all of it without a line of
  feature code.
- **A cardinal relocating a line name: zero artifacts.** The
  precedence is proven mechanically
  (`lineNamesYieldOnlyToAcceptedCardinals`); no committed page
  exercises it, and no evidence was manufactured to fill the bucket.

Excluded from the manifest with its own follow-up: the
export-dialog capture pair, whose committed screenshot and recorded
size come from captures of different dialog widths and whose width
flips between desktop sessions - capture instability of the #364
family, not feature effect.

Re-measured in the repair that followed this issue's incomplete
checkpoint: the #364 holdout applies to the unstable **image
capture**, not to truthful text. `export-strings.md` regenerates
byte-identically across fresh processes; the pre-#359 build
reproduces its committed bytes exactly, so it was never stale; and
its only #359 delta is the two quoted byte sizes of the written
`orion.svg`, which an SVG diff pins to one changed text element -
the facts line carrying this issue's approved celestial wording.
(The committed screenshot itself reproduced byte-identically at
332 px in the same sessions; the "326" beside it is the English
sheet's own packed width, not an inconsistency.) The deterministic
companion is therefore promoted with the repair, and the holdout
keeps only the capture-session instability it was written for.

## Promotion: 195 paths, not 212

Physical promotion was derived mechanically as the manifest minus the
**18 `ecliptic/candidate-*.png`** (sha256 of the promotion set
`9dcd1da2cd695311841b878a1d310d7f35d1d669843f2aa7428320cd28bce3fa`). Those 18 are genuinely touched by the wording - the
measurement stands - but their generated pixels also carry
independently owned, unreviewed #324 drift, and promoting them here
would make this issue the accidental carrier of evidence that
belongs elsewhere. #324 owns their baseline; regenerated after it
lands, they receive the caption naturally. A promotion exclusion,
not a manifest revision.

The 194 became 195 with one **source-effect companion**:
`test-evidence/measurements.md`, whose whole delta is one line -
explicit event-thread hand-offs 786 to 792, this issue's own new
tests. It sat outside the manifest because the experiment factored
the classpath, and this report reads the test sources, identical
across all three variants. The observation model's limitation is
recorded here rather than papered over.

Provenance: **178 rows newly dated**, and the naive one-row-per-PNG
arithmetic is wrong twice for structural reasons - the six
chart-sheet SVG/PDF sheets are renderer-drawn and carry rows; the
two inspection-class images and the fourteen Markdown companions
carry none by design. 172 rowed PNGs + 6 sheets = 178; every
promoted path accounts.

Before promotion, the wide-page baseline correction (the separate
commit ahead of #360's feature) was integrated first, and the 25
pages it corrects were reconciled: 25/25 the baseline equals the
experiment's control regeneration, 25/25 the fresh feature output
equals the experiment's, so this issue's effect on them is exactly
the attributed wording portion. The platform companions were
promoted from their headless captures, the contract's own
environment.

Canonical and portable evidence contracts passed on the promoted
tree, first attempt.
