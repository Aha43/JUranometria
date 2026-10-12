# Drawing Venus

**Sprint 48, issue #512.** The cartography contract for Venus, proposed
from measurement before any production Venus ink exists, for the
owner's rulings. The numbers are the study's
(`docs/studies/venus-cartography/measurements.md`,
`make venus-cartography-study`), computed through the production
projections and the accepted Venus service (#501, `venus-system.md`)
over the bundled packs, at the committed named instants (Espenak's rows
and the Horizons appearance fixture); the mock-ups are study ink over
pages the production composition painted, Venus's lit shape the page's
own (`ReferenceInk.litRegion`, the Moon's ruled geometry C3). Nothing
here is production rendering: that is #514, through the occurrence seam
of #513. It inherits the Sun's and the Moon's cartography
(`solar-cartography.md`, C1–C6) and Jupiter's (`jovian-cartography.md`)
wherever it does not say otherwise.

## The question

The numbers are accepted and handed over (`venus-handover.md`). Drawing
Venus raises what they do not answer:

- how large Venus is on a page, and whether a page may enlarge it;
- how its phase is drawn, and when a phase can be read at all;
- what the page shows at the conjunctions, when Venus crosses the
  Sun's disc or stands behind it;
- whether 5.2.0 needs a field below the atlas's 1° floor (#481) to
  show the phase, and what such a field would have to prove first;
- what a reader who cannot see the page is told.

## What was measured

**Size.** Over 1900–2100, every 7 days at Oslo, Venus's solid-body
diameter is 9.61″ (1952-06-23, farthest) to 63.03″ (2061-12-26,
nearest). On the 900 × 700 page, through each field's own projection:

| field | Venus (px) | under the smallest star mark (1.4 px) | on A4 at 300 dpi (px) |
|---:|---|---|---|
| 36° | 0.06–0.42 | always | 0.3–1.7 |
| 8° | 0.30–1.97 | at its smallest | 1.2–7.7 |
| 3° | 0.80–5.25 | at its smallest | 3.1–20.5 |
| 1° | 2.40–15.76 | never | 9.4–61.4 |

The true disc reaches the smallest star mark at 11.3° (largest) and
1.7° (smallest), a shape of 3 px at 5.3° and 0.8°, Jupiter's 6 px mark
at 2.63° and 0.40°. At 1° it is a shape in every week, and under the
6 px mark in 75.5 % of them. Venus is therefore the second body, after
Jupiter, to vanish at true scale on most pages; the Sun and the Moon
never did (C1 of #414).

**The phase.** By the handover's readability criterion - a disc of at
least 6 px whose lit and dark parts along the bright limb's axis are
each at least 1 px; a study measure, not a rendering rule (#500,
ruling 8) - the share of the century's weeks whose phase reads:

| field | screen page | A4 at 300 dpi | Letter at 300 dpi |
|---:|---:|---:|---:|
| 3° | 0.0 % | 29.6 % | 27.0 % |
| 2° | 0.0 % | 52.4 % | 49.0 % |
| 1° | 19.5 % | 64.9 % | 63.9 % |
| 0.5° (not offered) | 52.9 % | | |
| 0.25° (not offered) | 65.3 % | | |
| 0.1° (not offered) | 77.9 % | | |

By phase word at 1° on screen: crescent weeks read in 86.2 %, near
half lit in 57.7 %, gibbous in 0.0 %, near full and near inferior
conjunction never - a full disc has no dark pixel to read, a sliver no
lit one; those are the criterion's construction, not a defect of the
page. In the 2026–27 apparition the phase reads on the 1° screen page on
56 evening days (2026-08-15 to 2026-10-09) and 60 morning days
(2026-11-08 to 2027-01-06); at 0.25° on 186 and 239 days.

**The conjunctions**, Oslo, 1°, at the published hours:

| event | elongation | Venus px | the Sun px | nearer | the page shows |
|---|---:|---:|---:|---|---|
| 2012 transit | 0.15° | 14.45 | 472.6 | Venus | its dark disc inside the Sun's ring |
| 2026 superior | 0.71° | 2.44 | 487.7 | the Sun | a full 2.4 px disc beside the ring: the published conjunction is in ecliptic longitude, and the disc clears the Sun's |
| 2026 inferior | 6.52° | 15.28 | 482.1 | Venus | a dark disc with its limb; the lit sliver 0.09 px |
| 2100 inferior | 3.72° | 14.55 | 474.9 | Venus | the same; the sliver 0.03 px |

The Sun is a ring with its centre dot, its disc filled with the ground
(C2); the page paints the farther body first (C4). A transit and an
occultation therefore need no rule of their own: painting order and
distance give the dark disc inside the ring, and nothing at all when
Venus is inside the ring and farther. In the weekly rows *behind the
Sun* occurs three times in the century at Oslo.

**The frame.** The lit region is the page's own: the half disc towards
the bright limb with the terminator's half-ellipse of semi-axis
r·|cos i|, turned to χ of date through the page's north and east at
Venus (`PageBasis`), never recomputed from J2000 inputs (C3; #500,
ruling 3). With Venus near the page's edge the basis turns from the
page's top by up to 0.24° on the 1° page and 1.90° on the 8° page -
the Moon's behaviour, measured again for Venus.

**#481.** For each week the widest field at which its phase reads:
half the weeks need 0.54° or closer, three quarters 0.13°, 90 %
0.02°. Measured through the projection alone: the atlas's ladder
refuses any field below 1°, and no page was painted at one; the strip
`closer-fields.png` shows the 2026 elongation and the November crescent
at the scale 0.5°, 0.25° and 0.1° would give. Jupiter's record (#482)
measured its oblateness resolved only below 1° on screen and ruled no
close zoom then; Mercury and Saturn have no service and are each
planet's own study's.

**Mock-ups.** Fourteen pages under both candidate rules
(`elongation-true-*`, `elongation-mark-*`, `appearance-*`, the three
conjunctions, the A4 sheet at 150 dpi) in both palettes and both
languages, each with the rule and the drawn pixels in its caption and
MOCKUP in its corner; the inset a study magnification, not a field the
atlas offers.

## The proposal, for the rulings

1. **Size: rule M.** Venus is drawn as the larger of its true
   solid-body disc and a 6 px minimum mark - Jupiter's rule (#482,
   ruling 2) - **phased either way** by the page's own lit region, so
   a reader at 8° sees a phased symbol where rule T (true scale
   always, the Sun's and the Moon's C1) would draw a dot the size of a
   faint star. When the mark stands for the disc the spoken name says
   so, as Jupiter's does (*a cartographic symbol, not Venus's apparent
   diameter*). The study draws both rules; rule T is the honest
   alternative and loses only the phase above 2–3°.
2. **The phase geometry is the Moon's**, C3, with χ of date rotated
   into the page's basis at Venus; the dark side's ink and the limb as
   the Moon's. No sliver is invented: near inferior conjunction the
   disc is drawn dark with its limb (the lit part under a tenth of a
   pixel at 1°), near full it reads as full. The cloud-top radius is
   never drawn (#500, ruling 4).
3. **The conjunctions need no rule**: a transit is Venus's dark disc
   inside the Sun's ring, an occultation nothing, by painting order
   and distance; the spoken description at each is for the ruling
   (proposed: the phase word the table says - *in front of the Sun*,
   *behind the Sun* - appended to the name as *below the horizon* is).
4. **Below the horizon, off the page, collisions, time, module:** C4,
   C5 and C6 inherited unchanged; the Solar System module offers
   Venus beside the Sun and the Moon, as Jupiter's offers Jupiter;
   Centre on chart arrives with the layer (#483's pattern, through
   #514).
5. **#481, for the owner:** Venus can be drawn honestly at the
   ordinary 1° floor under either rule; closer zoom is what makes its
   phase readable at most dates - half the weeks at about 0.5°, two
   thirds at 0.25°. The study recommends that **5.2.0 draw Venus at the
   ordinary ladder alone** and that #481 stay open with this
   measurement attached: a closer field is a navigation and evidence
   contract of its own (the fields, the qualifying-object rule, the
   way back to the ordinary floor; projection, labels, export,
   keyboard zoom, persistence and evidence proven below 1° first), and
   nothing in this sprint depends on it. The owner may rule the other
   way; then #481's study is the next step before #514.
6. **The readability criterion** stays a study measure.

## Evidence this study moves, and what must not

`docs/studies/venus-cartography/` is new (the report and fourteen
mock-ups, registered with the evidence contract as a deterministic
report with its images held beside it); `ReferenceInk.litRegion` is made
public so the study draws with the page's geometry rather than a copy
- a visibility change, no behaviour. No chart, table-result or export
bytes move; no production Venus ink, selection or Controller change; no
language key; no download.

## The ruling

The owner's rulings (2026-10-12), accepting the six proposals as
written, recorded as given:

1. **Rule M.** Venus is drawn as the larger of its true scale and a
   phased 6 px mark. Venus must remain findable like Jupiter, but its
   phase must remain honest. The mark represents Venus
   cartographically, not a claim about apparent size.
2. **The Moon's phase geometry**: χ of date rotated into the page's
   basis. No sliver is invented near inferior conjunction or near
   full.
3. **No special conjunction drawing rule.** Painting order handles
   transit and occultation. The table's geometric state is appended to
   the spoken description when applicable.
4. **C4–C6 inherited.** Venus is added to `SolarSystemModule`; Show on
   chart and Centre on chart arrive with the production layer in #514.
5. **#481 stays open.** Venus ships on the ordinary field ladder in
   5.2.0. This study's measurement is attached to #481 for the later
   object-dependent closer-zoom design.
6. **Phase readability stays a study measurement**, not a production
   threshold or suppression rule.

Nothing in the proposal was amended; the study's report and mock-ups
stand as measured. #513 (the shared occurrence identity and selection
context) and #514 (Venus on the chart) build on these rulings.

## Venus on the chart, as built (#514)

- **The seam.** `OverlayContribution.Body` gains a minimum mark (0 for
  the Sun and the Moon, 6 px for Venus - `SolarSystemModule
  .VENUS_MINIMUM_MARK_PX`, ruling 1) and a state key (ruling 3).
  `SolarSystemModule` offers Venus as a third switch beside the Sun's
  and the Moon's: the solid body's disc, `Lit(k, χ, i)` exactly as the
  Venus table states them, its moment, selectable (#513, ruling 2),
  and `venus.inFront` / `venus.behind` when its disc meets the Sun's;
  outside Venus's years it offers nothing, as Jupiter's module does.
- **Rule M on the page.** `ReferenceInk.bodyOn` places a round body's
  mark as the larger of its true disc and its minimum; while the mark
  stands in, the body is a *symbol* - painted by the reference layer
  beneath the stars (`paintSymbols`, with Jupiter's), phased by the
  page's own lit region exactly as the true disc would be, not opaque
  (a click through it reaches a star unless Venus is pointed at),
  spoken with `page.body.venus.symbol`; once the true disc reaches the
  mark it is the body itself, opaque, the Moon's phased disc. The
  symbol's ink is an obstacle for the page's words, as Jupiter's is.
  **A symbol in front of an opaque disc** - Venus in transit at 4° to
  12° (the owner's checkpoint finding) - is painted in the bodies
  layer over that disc, in painting order, not beneath the stars
  where the disc would cover it (`ReferenceInk.symbolOverADisc`); its
  name takes the first clean box beside the mark, clear of the Sun's
  ink, or is refused where none is, as the Moon's at a new Moon (C4).
- **No conjunction rule.** The transit is Venus's dark disc inside the
  Sun's ring by painting order (the nearer last); a Venus inside the
  ring and farther is covered by the Sun's ground-filled disc; the
  spoken description appends *in front of the Sun* / *behind the Sun*
  (`page.body.state.venus.*`, both languages).
- **The switch.** `View ▸ Solar System ▸ Venus on the chart`, directly
  after *Sun on the chart* (the ruled order), remembered as the Sun's
  and the Moon's are (`VenusChartStore`, preference `venusOnChartShown`;
  `VenusChartSession`), hidden by default; the Controller's Venus group
  and the Venus dialog gain *Show on chart* and *Centre on chart* over
  the same layer and the shared action (`CentreOnChart.Body.VENUS`).
- **Selection.** Venus is a selectable occurrence through the #513
  seam: a click on the disc, or on the symbol, selects Venus at the
  instant and observer drawn, nothing behind an opaque disc offered.
- **Paper.** The same layer through `SheetInk.bodies`, so a sheet
  carries Venus as the screen showed it.
- **Proof.** `SolarSystemModuleTest` (k, χ, i, the mark, the moment,
  the state, nothing outside the years); `VenusInkTest` (the symbol at
  8°, phased and pointable, the true disc at 1°, the transit over the
  Sun's ring and the spoken state); `VenusOnTheChartJourneyTest` (the
  real component: drawn where the service puts it, spoken as a symbol
  at 8° and as Venus at 1°, a click selecting its occurrence, the
  switch remembered and restored with its item in both languages, the
  submenu's order); the Controller journey's Venus group with both
  controls; the packaged image's `venusOnTheChartJourney`.
- **Evidence.** `docs/studies/venus-on-the-chart/` - Venus's production
  pages at the cited instants (the 2026 elongation at 8° in Norwegian,
  at 3° on both palettes and at 1°; the 2012 transit over the Sun's
  ring; the 2026 inferior and superior conjunctions) and the 2026-27
  apparition as a row of phases measured against the table; the menu
  sheets retaken with the new item; provenance re-recorded.
- **Not done, by ruling.** No field below 1° (#481 stays open); no
  atmospheric detail; the Galilean moons' selection unchanged.
