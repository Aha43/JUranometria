# Sprint 21 handover — Read the deep sky

The chart has drawn five different deep-sky symbols since Sprint 6 and
has never told a reader what any of them means. Sprint 21 (issues
#184–#186, milestone 21) names them, hands each one to the reader as
a switch, and puts the explanation in the same place as the control.
This handover is the coder's account for the review.

## What Sprint 21 delivered

- **The gate** (#184):
  [`docs/decisions/deep-sky-vocabulary.md`](../decisions/deep-sky-vocabulary.md),
  measured over the whole pack with `make deep-sky-study`
  ([the study](../studies/deep-sky-vocabulary/measurements.md)).
- **The surface** (#185): five independent family flags behind one
  composition seam, and Chart Options rebuilt as four tabs whose
  Deep sky tab is legend and control at once.
- **The close** (#186): the production-control journey, the packaged
  evidence, the documentation, and this handover.

## The decision, and the measurements behind it

**A family is exactly one drawn symbol.** That is the whole idea, and
it is what made the proposal checkable rather than a matter of taste:
a family is the set of catalogue types the chart already draws
identically, so a reader who learns the mark has learned the family.

| symbol | family | types | rows |
|---|---|---|---:|
| oriented ellipse | Galaxies | `G`, `GPair`, `GTrpl`, `GGroup` | 10,791 |
| dotted circle | Open clusters | `OCl` | 663 |
| crossed circle | Globular clusters | `GCl` | 208 |
| outlined box | Nebulae | `Neb`, `EmN`, `RfN`, `DrkN`, `HII`, `SNR`, `Cl+N` | 303 |
| small crossed circle | Planetary nebulae | `PN` | 130 |

12,095 drawable + 1,276 deliberately undrawn = **13,371**, the whole
bundled pack. Exact counts, not bands: `DeepSkyVocabularyTest` pins
all nineteen per-type counts and the five family totals, so a pack
that lost a thousand galaxies fails rather than rounds.

**One surprise, said out loud rather than filed:** `Cl+N`, a cluster
with nebulosity, draws as a *nebula* and not as a cluster. A reader
hiding nebulae also hides 67 objects they might look for under
clusters, so the family's own description says so.

## What the measuring changed

Three things the gate found by measuring rather than by inspection,
each of which changed the design:

- **A shared painter is not a shared vocabulary.** The first legend
  handed the production painter one axis and no angle, which
  degenerates the chart's oriented ellipse into a plain circle — and
  taught a mark the chart never draws, beside a sentence promising
  catalogued size and orientation. Exemplars now live in
  `ChartRenderer.legendShapeFor` at the pack's own **median axis
  ratios** (galaxies 0.644, nebulae 0.778, open clusters 0.933,
  rounded to a twentieth), re-measured against the catalogue by a
  test. The nearest pair improved from 34% to **51%** of unshared
  ink, and is now the two families that genuinely are both circles.
- **The chart's ink is invisible on a dark dialog.** Painted straight
  onto the dark theme's panel it scores **1.85:1**; on the chart's own
  white, **5.74:1**. So each row carries a scrap of the chart's paper.
  The chip is not decoration.
- **One contrast rule, applied to every state.** Every mark in the
  legend clears 3:1 against its ground *as rendered*. That rejected
  fading a switched-off family's symbol (1.97:1 and 1.56:1), and it
  also condemned the chart's own **nebula box at 2.96:1** — which the
  gate costed and assigned, and #185 paid: grey 150 → **132**
  (3.74:1). Not the 148 that clears the floor by one part in a
  hundred. `LegendContrastTest` now holds the chart to it and fails on
  either 150 or 148.

## The composition seam

`ChartRenderer.permitted(scene, dso, options)` is the one place the
filter lives, in front of the option-free `RegionalDetailPolicy`,
which goes on answering what the chart *would* draw without ever
being told what the reader asked for. Every deep-sky pass asks it:
the marks, the labels, and the space star labels yield to. So a
family switched off leaves no mark, no label, and no reservation —
and hit testing cannot name what the page did not draw.

Two decisions are now **published rather than re-derived**:
`labelledDeepSky` and `drawnDeepSky` return the sets the passes
iterate. That mattered: the first version of the `labelled ⊆ drawn`
test mirrored the rules instead of asking the renderer, and was
wrong about objects clipped at the page edge. The test now compares
the renderer's own answers.

## The dialog earns its tabs

Eleven checkboxes in one column was already long; sixteen is a list.
Four tabs by subject — Deep sky, Stars, Constellations, Chart — with
the live-preview transaction, the single instance, and **every
existing mnemonic** unchanged. The families took `G`, `O`, `C`, `U`,
`P`.

Measuring the keyboard route turned up something already true:
`Constellation figures` and `Flamsteed numbers` have both answered to
Alt-F since Sprint 17, and in one panel that made one of them
unreachable by its own letter. The tabs separate them. Recorded and
pinned rather than left to be discovered.

**The dialog's height is the screen's**, not a constant: the usable
bounds less window decoration, floored at 320 px, with the tab
scrolling inside the cap. A scroll bar answers a tab that is too
tall; nothing answers an OK button under a taskbar.

## The study photographs production

At the gate the dialog pictures were a mock-up. Now that the dialog
exists, the study builds `ChartOptionsDialog.content` — because a
study that keeps its own copy of the surface is a second
implementation free to drift. It caught the drift immediately: **the
mock-up's Chart tab had the magnitude key switched on, and the
release ships it off.**

Each picture also states the screen it assumes rather than reading
this machine's, so the report reproduces byte-for-byte instead of
moving when a dock hides itself.

## Corrections from review

Every finding across four rounds, and what each cost:

- **P1, the galaxy exemplar** — above. The fix moved representative
  geometry into a production seam, which is where the study and the
  dialog now both read it.
- **P1, the fixed 780 px height cap** — measured on one machine and
  enshrined. Now derived from the screen, with a regression that
  drives the production sizing path against a stood-in 768 px display
  at ordinary and enlarged text.
- **P2, the contrast rule applied only to the rejected design** —
  resolved by one rule, and by paying for it (the nebula ink).
- **P2, the gate's contract living only in a merged PR** — the
  requirements now sit in issue #185's own body.
- **P2, a test that deleted a `defaultFont` override instead of
  restoring it**, then the follow-up that an override *equal to the
  theme's font* is still a choice. Both fixed in one shared
  `SwingSession`, which detects an override by presence rather than
  value.
- **P2, the contrast rule documented but unenforced** — now a test.
- **CI, not review:** the scrolling tab layout adds arrow buttons that
  arrive **unnamed**, and on Linux they appear at a width where macOS
  fits the titles. `AccessibleSurfaceTest` failed exactly as it
  should. They are named from `BasicArrowButton.getDirection()` by a
  container listener, since the layout adds and removes them.

One correction to my own reporting: I attributed a single focus
failure to machine load. Mutating the restoration to leak a theme
reproduced a focus failure in an unrelated toolbar test, so the
reviewer's order-dependence hypothesis had evidence behind it and my
guess did not.

## Verification

- **485 tests** on a display; headless, the display-dependent
  journeys abort visibly rather than disappear.
- **The M31 reference is byte-identical.** The released default page
  draws no nebula box, so even the palette change left it untouched —
  which the gate predicted before making it.
- The six chart-furniture study images that *do* draw nebulae were
  regenerated with the ink change, and nothing else moved.
- `make deep-sky-study` reproduces its report and all sixteen images
  byte-for-byte.
- **The journey drives the real controls throughout**: the View menu,
  the tab strip's own Control-Page Down route, the real checkboxes,
  the search field, pointer events on the chart, the Inspector, and
  the toolbar's Reset view. Its premises are established before the
  outcomes they support, and two mutations — ignoring the family
  filter, and dropping the target exemption — both fail it.
- **Packaged acceptance**, inside the native image:
  `deep-sky families OK (Galaxies 16→0, Open clusters 16→0, Globular
  clusters 13→0, Nebulae 33→0, Planetary nebulae 12→0, each leaving
  the others untouched; a hidden family changes the drawn page; the
  named target still drawn with its own family hidden; symbol-less
  NGC 6335 found, centred and titled with no invented mark)`.
- The unchanged four-platform native-image and portable-distribution
  matrix, light and dark launches, and the no-system-Java path run in
  CI on every push.

## Residual risks

- **The control hides the taxonomy it groups.** A reader who wants
  only reflection nebulae, or only galaxy groups, cannot say so: the
  dialog offers five families, not nineteen types. The source type is
  intact on every object and the Inspector still names it, but the
  *control* is coarser than the catalogue. That is the trade the gate
  made deliberately, and it is the one most likely to be revisited.
- **Symbol comprehension is tested structurally, not with readers.**
  The measurements say the marks are geometrically distinct and clear
  a contrast floor; nobody has watched a reader look at the Deep sky
  tab and then find a globular cluster on the page. "Distinguishable"
  is not "understood".
- **Selection still has no keyboard walk across the page.** A reader
  without a pointer reaches an object by searching for it, as since
  Sprint 19. Family filtering does not make that worse, but this
  sprint was a chance to fix it and did not.
- **`Cl+N` is filed under Nebulae** — stated in the family's
  description, and still a surprise to a reader who does not read it.
- **The legend's tilt is a fiction a reader could over-read.** A
  galaxy in the legend leans at 35°; a galaxy on the page leans the
  way the catalogue recorded.
- **The nebula ink changed.** It is measured, argued and byte-checked
  against the reference, but it is a change to the released chart's
  palette, and readers of earlier study images will see the
  difference.

## Recommendation

Hold for the owner. Sprint 21 keeps every promise of the 1.0
contract: no key changes meaning, an upgrading store gains the five
families switched on so the chart a reader left is the chart they
return to, and the released default page is byte-identical.

It adds reader-visible features (five family controls, a tabbed
dialog) and changes one palette value on pages that draw nebulae, so
it is a **minor** release when the owner decides to cut one — but
that decision, the merge, the milestone and the tag are the owner's,
not this pull request's.
