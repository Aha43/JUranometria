# Sprint 37 handover — Draw the Sun and the Moon

Six issues, four merged pull requests, a ruled cartography contract,
two packaged owner checkpoints of which one returned a finding and one
an observation that measurement settled, a correction to a shipped
omission found by the work that followed it, and one shared evidence
flush that published a new room of the public gallery. This is what
the sprint established, what the owner found and ruled, what stopped
and how each stop was classified, the totals, the evidence movement,
what stays excluded, and the questions the planets open with.

Written against `main` at `07e0fb0` (PR #423), after its post-merge
checks. Sprint 37 draws the Sun and the Moon on the chart, on paper
and in the gallery; it does not claim 4.0.0. No version changes,
nothing is tagged or released unless the owner rules that 4.0.0 is
reached.

## Why the sprint existed

Sprint 36 left both bodies held to an authority as numbers and in the
reader's tables, and closed with ten cartographic questions. The road
to 4.0 (#397) answers them on real pages: first a ruled contract from
measurement and mockups, then the Sun and the Moon each behind its own
packaged checkpoint, then one shared, expensive evidence integration
after both were accepted. The Earth's shadow (#417) was created as an
optional experiment, eligible after the Moon and not blocking 4.0.

## What shipped

**#414 — the cartography contract (study only; PR #420, merged as
`18ffeb8`).** True angular diameters measured through every field the
atlas draws (no page puts a true-scale disc below the minimum visible
ink), how north turns across a page (−16.7° at the 36° page's corner
at +45°, −72.2° at +75°; the projected north and east 88.3° apart
there), and ten mockups over production pages. The owner ruled
**C1–C6**: true scale always, never enlarged; the Sun a true-scale ring
with its centre dot and the Moon a true-scale phased disc, in neutral
chart ink; phase drawn in the projected north/east basis at the Moon
with a round-trip contract, never re-derived from J2000; off the page
nothing and no edge hint; below a shown horizon dimmed with a localized
status, drawn normally with the horizon hidden; the Moon's disc opaque
and hidden stars out of hit-testing; labels on deterministic adjacent
candidates, refusable, never over the equinox landmark nor fusing two
meanings; one instant per page; one Solar System module, no planet
framework, no emphasis, no inspector selection in 4.0. Study:
`docs/studies/solar-cartography/`; record:
`docs/decisions/solar-cartography.md`.

**#415 — the Sun on the chart (PR #421, merged as `dfab45b`).**
`OverlayContribution.Body` and `InkRole.BODY`; one
`SolarSystemModule` reading Place and Time's observer when the page
paints; a renderer hook painting bodies after every catalogue mark
and label and before the furniture; the disc filled with the ground,
ring r/12 and dot r/6 in star ink, dimmed below a drawn horizon; the
name placed on the cardinal letters' tiers from the disc's edge or
refused; a click inside a disc is the empty sky there and offers
nothing behind it; `View → Sun on the chart`, off by default,
remembered (`SunChartStore`), independent of Place and Time. After the
first packaged review (below) the disc became an obstacle for every
word the reference layer writes - landmark words, line names and
cardinal letters - so a word keeps its released box unless a disc
touches it and otherwise takes the first clean box around its own
mark (`SunOverTheEquinoxWordTest`).

**#416 — the Moon on the chart (PR #422, merged as `bbc0ba8`).** The
same module offers the Moon with `Lit(k, χ, i)` exactly as the Moon
table states them. `PageBasis` holds the page's own north and east at
a position; the lit side faces cos χ·n̂ + sin χ·ê, and the round trip
is the exact inverse (the ruled atan2(d·ê, d·n̂) where the basis is
square, exact where a tangent page skews it). The disc: the dark side
inked 30 % of the way from the page's darker ink to its lighter, the
lit region (half disc ± a half-ellipse of axis ratio |cos i|, area k)
in the lighter, the limb in star ink. Bodies carry their distance and
are painted farthest first, so the Moon covers the Sun; names are
placed after every disc, nearest first, clear of every disc's ink, and
spaced from the ink's outer edge (at 1° the Sun's ring had reached past
the old gap). `View → Moon on the chart`, its own switch, remembered
(`MoonChartStore`). The lunation measured from its own pixels: on all
24 days of June 2026 where the table states a lit side, the drawn disc
faces the table's compass point and shows its k within 2 percentage
points.

**#417 — the Earth's shadow.** Parked, as created: optional,
non-blocking, and active only on the owner's word after mockups.

**#418 — the shared evidence integration (PR #423, merged as
`07e0fb0`).** The export correction (below); the public gallery's new
room, *The Sun and the Moon*, at its one moment (Oslo, 2026-03-20
21:33 UTC): the 4.3 % crescent on a 3° page with no horizon, and a 36°
page holding both bodies and Oslo's horizon, both dimmed and saying
so - published by `pages` and verified live, images byte-identical to
the committed files; `sheet-a4-solar-system` (SVG, PDF, 300 dpi PNG) at
42° with the ecliptic, the horizon off; the packaged
`solarSystemSheetJourney`, which exports that sheet through the
application's own path and holds the PNG pixel for pixel to an
inspected recording and that to the screen.

## What the owner found and ruled

- **The equinox word under the Sun (#415).** The first packaged
  candidate drew the Sun spot on, at the right size, but at the 2026
  September equinox on a 6° page the disc hid part of the landmark's
  word; switching the Sun off restored it. The committed 24° March
  page already showed the same fault, which the checkpoint had
  described as honest occlusion; the ruling said otherwise. Repaired
  as ruled - the disc an obstacle, never permission - and the second
  candidate accepted: *"Wow. It's perfect."*
- **The switches.** `View → Sun on the chart` ruled correct as designed
  (off by default, remembered, independent of Place and Time); the
  Moon's own switch and the 30 % dark side accepted inside the ruling.
- **The Moon looked fuller than 78 % (#416).** Checked against the sky
  - JUranometria 78.3 %, Stellarium 78 % - the drawn Moon first looked
  slightly fuller. Measured from the owner's 8° screenshot the dark
  crescent is 0.41 of the radius against the 0.43 that k requires: the
  drawing is right, and the sky's Moon looks less lit because the band
  near the terminator is sunlit at a grazing angle and looks dim. At
  36° a separate, real effect remains: the outline ring covers part of
  a crescent under 3 px wide. Recorded on #418 as a non-blocking
  observation for dogfooding, not changed.
- **The 42° sheet's crescent (#418).** At 42° the 4.3 % crescent is at
  most 0.15 mm, narrower than the outline's inner half (0.18 mm), so it
  is unresolved: ruled the scale's honest limit - nothing enlarged, no
  paper-only limb rule, no second sheet. The report says so; the 3°
  gallery page carries the crescent.
- **Placed labels are a platform fact (#418).** Ruled systematic: every
  sheet's placed-label count leaves the deterministic report for the
  per-machine platform record, beside its bytes.

## Corrections to shipped work

- **Exported sheets carried no body, from #415 to #418.** #415 wrote
  `SheetInk.bodies` and said a sheet carries the Sun the screen
  showed, but the application's export passed only the reference
  layer and `ChartSheet` had no bodies layer. The #415 review accepted
  "exported-sheet agreement" against a sheet that could not show it;
  the omission was found by #418's sheet work, not by any check.
  `ChartSheet` and `ExportSheet` now carry the layer (released sheets
  byte-identical); `ExportSheetSessionTest` fails without the wiring;
  the packaged journey holds an exported file to the screen.
- **Two comment blocks** in the language files (a stray English line
  from #408 and the Sun's words under the Moon table's heading from
  #415), tidied in #416.

## What stopped, and what was classified

- **#414, `cbcb07f`**: gate 3 stopped - the cartography study wrote its
  report to a file rather than stdout, against the evidence contract's
  rule for deterministic reports. Corrected by `db26afc` (stdout, a
  Makefile redirect, the study's projections through `DrawnPage`),
  which passed all seven gates once.
- **#415, `4cf63fa`**: a gate launch was stopped by hand within
  seconds because a focused test was still red; recorded
  (`ABORTED-LAUNCH.md`), not counted.
- **#415, `ca71dcd`**: gate 1 incomplete on one
  `ExportSheetDialogSheetMain` capture refusal; classified by owner
  ruling as unexecuted local coverage on condition that CI's first
  `display` run executed the test cleanly, which it did; gates 2–7 ran
  once.
- **#415, `d4f9e3f`**: a development run of the suite was started
  while the superseded review application was still open beside it;
  stopped, the review application quit, rerun alone. Not a gate.
- **#416, `28efec4`**: gate 1 failed one test,
  `SheetCaptureSizingTest.contentTheWindowLaysOutAtItsPackedWidthIsLaidOutAtTheDeclaration`
  - the known native-window layout transition (declared 420 px, stale
  packed 371 px), in code #416 did not touch. Classified by owner
  ruling as unexecuted local coverage on the same condition; CI's first
  `display` run executed it cleanly (1810/1810); gates 2–7 ran once.
- **#418, `ccf2bcd`**: all seven local gates OK; CI's `evidence` job
  failed - the chart-sheet report did not reproduce on Linux (15
  labels placed on macOS, 14 there, every shape the same). Kept in the
  record; corrected by `58e06b2` as ruled; the seven gates restarted
  once and CI ran once, all green.
- **Route**: every pull request wide (rendering, resources, studies,
  interface evidence), as the classifier chose.

## Totals

| head | local suite (gate 1) | local gates | CI |
|---|---|---|---|
| `cbcb07f` (#414) | 1776/1776, 0 failed, 43 aborted | stopped at gate 3 | not run |
| `db26afc` (#414) | 1776/1776, 0 failed, 43 aborted | 7/7 OK | all green |
| `ca71dcd` (#415) | 1796/1796, 1 failed (capture refusal), 43 aborted | 1 incomplete, classified; 2–7 OK | all green; `display` 1796/1796 |
| `d4f9e3f` (#415) | 1797/1797, 0 failed, 38 aborted | 7/7 OK | all green |
| `28efec4` (#416) | 1810/1810, 1 failed (layout transition), 43 aborted | 1 incomplete, classified; 2–7 OK | all green; `display` 1810/1810 |
| `ccf2bcd` (#418) | 1812/1812, 0 failed, 22 aborted | 7/7 OK | `evidence` **failed** (label count); recorded |
| `58e06b2` (#418) | 1812/1812, 0 failed, 22 aborted | 7/7 OK | all green |
| `18ffeb8` (merge of #420) | — | — | post-merge `test` and `app-image`: successful |
| `dfab45b` (merge of #421) | — | — | post-merge `test` and `app-image`: successful |
| `bbc0ba8` (merge of #422) | — | — | post-merge `test` and `app-image`: successful |
| `07e0fb0` (merge of #423) | — | — | post-merge `test`, `app-image` and `pages`: successful |

Every abort is in the keyboard-focus and display family the gate
script names; none is unexplained. Every post-merge run passed on its
first attempt.

Evidence movement, sprint start (`bcf132d`) to close (`07e0fb0`):
renderer-drawn artefacts **431 → 463** (the cartography mockups, the
Sun's and the Moon's production pages and lunation strip, two gallery
slides, three sheet files), deterministic reports **74 → 77**,
provenance **431 → 463** rows. Re-recorded along the way: the menu
photographs and companion (twice, one item each), the control
explanations, the test-evidence study and its quoted counts with each
pull request, the ledger (seven rows), the chart-sheet report and its
platform record.

## What the seams now guarantee for the planets

- **`OverlayContribution.Body`** is everything a page needs from a
  body: identity, position, true diameter, how it is lit (or null),
  whether a drawn horizon hides it, and its distance. A planet is a
  `Body` from the same module, painted by the same three passes -
  placed, painted farthest first, named nearest first - and gets
  opacity, hit concealment, dimming, label refusal, the obstacle rule
  for every reference word and the export for free.
- **`PageBasis`** turns any position angle into a page direction and
  back exactly; a planet's phase (Venus, Mercury) or a ring's tilt
  needs no new rotation code.
- **`SheetInk.bodies`** and the bodies layer of `ChartSheet` mean a
  sheet carries whatever the screen draws, held by a packaged journey
  that compares the exported file with the screen.
- **The module is one switch per body**, remembered per body; the menu
  bar has kept every earlier arity through each addition.

## Exclusions, explicit

No libration, surface or earthshine; no eclipses or the Earth's shadow
(#417, parked); no rise, set, transit or twilight; no tracks, ranges
or animation on the page; no Sun or Moon selection in the inspector;
no emphasis participation; no edge hints for an off-page body; no
daylight; no planets.

## The planets: the questions they open with

1. **Which planets, and why those.** The five naked-eye planets, or
   all eight; Pluto and the dwarf planets out or in.
2. **The pack.** Each planet's barycentre segment from DE440 for
   1900–2100, the cost in bytes, and whether one regeneration carries
   them all, with every earlier body's invariance held to a fixture
   written before it.
3. **True scale at a point's size.** How large each planet's true
   disc is at each field the atlas draws, measured from the pack as
   #414 measured the Sun and the Moon - and where it falls below the
   minimum visible ink, which C1's "never enlarged" never had to face
   for two half-degree bodies: what a planet's mark is when its disc
   cannot be seen, and how it differs from a star of the same
   brightness.
4. **Magnitude.** A planet's brightness changes with its distance and
   phase; whether the mark's size follows the star-size policy from a
   computed magnitude, and against which authority that magnitude is
   held.
5. **Phase for the inner planets.** Venus and Mercury show phases; at
   true scale they are invisible on the page, so whether phase appears
   only in the table.
6. **Labels in a crowded ecliptic.** Several bodies, the Moon and the
   ecliptic's landmarks within a few degrees: the label precedence
   between bodies, and whether the obstacle rule generalises.
7. **The table.** One `SolarTable` per planet or one table for all;
   which columns a planet needs (elongation, phase, magnitude, the
   constellation the atlas cannot yet name).
8. **Satellites and rings.** Out of scope, or the first thing a reader
   at a telescope asks for.

## The article programme

On a branch for the owner's review, `docs/sprint-37-articles`, built on
the parked `docs/first-articles`: *Which way the light falls*, the
lunar phase and orientation article the Sprint 36 notes recommended
writing once a drawn Moon could sit beside the bright-limb figure,
with production Moon imagery only - the accepted pages, the gallery's
crescent and the lunation strip, never the contract-era mockups; and
the accepted *Where the Sun is*, which gains its figures from the
accepted Sun pages and an updated closing section now that the atlas
draws the Sun, without becoming a Moon article.

## Structural evidence

- `docs/decisions/solar-cartography.md` - the C1–C6 ruling and the
  Sun's, the Moon's and the integration's implementation, with every
  owner finding and correction.
- `docs/studies/solar-cartography/` (the contract study and mockups),
  `docs/studies/sun-on-the-chart/`, `docs/studies/moon-on-the-chart/`
  (with the lunation measured against the table),
  `docs/studies/chart-sheet/` (with `sheet-a4-solar-system` and the
  platform record of placed labels), `docs/studies/gallery/` and
  `docs/gallery/` (the published room).
- Issues #414–#418 and pull requests #420–#423 carry the
  checkpoints, rulings, gate records and stopped runs verbatim.
