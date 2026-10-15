# Drawing Mercury, and the closer fields

**Sprint 50, issue #539.** The cartography contract for Mercury and the
object-dependent closer fields of #481, proposed from measurement before
any production Mercury layer or closer zoom exists, for the owner's
rulings. The numbers are the study's
(`docs/studies/mercury-cartography/measurements.md`, its platform record
beside it, `make mercury-cartography-study`), computed through the
production projections, the production renderer and the production
bodies layer, with Mercury offered to that layer by the study as a body
built from the accepted Mercury service (#529) exactly as the Solar
System module builds Venus's. The closer fields are real pages - the same
renderer over the same catalogue, through viewports the ladder does not
offer - and the ladder is untouched. It inherits the Sun's and the
Moon's cartography (`solar-cartography.md`, C1-C6), Jupiter's
(`jovian-cartography.md`) and Venus's (`venus-cartography.md`, rule M)
wherever it does not say otherwise.

## What was measured

**Size.** Mercury's solid-body disc is 4.64″ to 12.26″ at Oslo over
1900-2100, on the 900 × 700 page **1.16-3.06 px at 1°**, under the
smallest star mark at its smallest from 2° wide and always from 3°; it
reaches the 6 px mark only at 0.51° (largest) to 0.19° (smallest) -
below the 1° floor in every week. On the A4 sheet's chart area at 300 dpi
it is 4.1-10.9 px at 1°. (The #512 Venus report used the whole sheet's
width for its paper pixels and so overstates them by 9 %: a slip in that
report, not in anything drawn.)

**The phase, by the study criterion** (a disc of at least 6 px, its lit
and dark parts each at least 1 px - a study measure, not a rendering
rule), the share of the century's weeks that read:

| field | Mercury, screen | Venus, screen |
|---:|---:|---:|
| 1° | 0.0 % | 19.5 % |
| 0.5° (candidate) | 0.0 % | 52.9 % |
| 0.25° (candidate) | 41.9 % | 65.3 % |
| 0.1° (candidate) | 68.4 % | 77.9 % |

Mercury's near-half-lit weeks all read at 0.25°, three quarters of its
crescent weeks and a third of its gibbous ones; near full and near
inferior conjunction never. In the 2026 apparitions its phase reads on
no day at 1° or 0.5°, and on 76 evening and 74 morning days at 0.25°.

**Rule T against rule M, painted.** Rule T draws Mercury at 1.79 px on
the 1° page at the 2026 greatest elongation and 0.22 px at 8° - fainter
than the faintest star, no phase at any ordinary field. Rule M draws
Venus's phased 6 px mark at every ordinary field, beneath the stars, not
opaque, spoken as *a cartographic symbol, not Mercury's apparent
diameter*; the phase is carried by the mark's lit region, never by a
size the disc does not have.

**On paper**, the mark is a fixed part of the page: 6 units of the
recording's 770 for 1°, so it prints **2.1 mm (25 px)** on A4 at 300 dpi,
where Mercury's true disc would print 7.1 px. The sheet agrees with the
screen by construction - the same rule in the same units - and is honest
as a symbol, but a 300 dpi sheet could show the true disc's phase in
30.3 % of the weeks at 1° (the figure #528 measured).

**At the conjunctions.** A transit paints Mercury's dark mark over the
Sun's ground-filled ring (#514's rule for a symbol in front of an opaque
disc); behind the Sun the mark is laid beneath the stars and the Sun's
disc covers it; the spoken description appends *in front of the Sun* or
*behind the Sun*. The 1937 grazing transit is *near inferior conjunction*
from Oslo, its centre +5.3″ outside the limb sum by parallax (the
geocentric margin held under ruling 6's amendment is +0.017″).
**One finding the earlier bodies never met:** the production Sun's
centre dot has a sixth of the Sun's radius - about 40 px at 1° - and in
**5 of the 28 transits** (1924, 1927, 1970, 1973, 2019) Mercury's mark
stands wholly on it at greatest transit, its dark tone a faint grey on
the dot's black. Below 1° the dot grows with the disc: at 0.25° the
Sun's ring is off the page and the page is a black disc 320 px across.

**The frame.** The lit region is the page's own (`litRegion`, C3), χ of
date turned through the page's basis at Mercury; near a page's edge the
basis turns by up to 0.055° at 0.25°, 0.22° at 1°, 1.74° at 8°.
`MercuryCartographyTest` holds the round trip on the 1°, 0.5° and 0.25°
pages: Mercury's J2000 place to the page and back within a
microarcsecond, the bodies layer centred on its projected pixel, rule M's
switch at the true disc's size, the lit area k of the disc's.

**Labels.** Of the 70 named instants at 1°, rule M, the Sun on, the name
was placed at 44 and refused at 26 - the 25 transits whose mark stands
inside the Sun's ink and the 2026 superior conjunction (platform record);
the 1937, 1993 and 1999 transits near the limb kept a box. A crowded
Sagittarius page at 3° placed it.

**The closer pages themselves**, beyond the body:

- the title block and spoken description print one decimal - the 0.25°
  page says *Field 0.3°*;
- the grid's finest steps (1m, 15′) were built for the ladder: at 0.25°
  one step spans the page, so at most one line each way;
- a 0.25° page near the ecliptic often holds no catalogue star at V 8.0;
- the Sun's centre dot fills a closer page at a transit (above);
- the study reused the 1° page's catalogue query; production would query
  its own field.

## The proposal, for the rulings

1. **Rule M for Mercury**, unchanged from Venus's: the larger of the true
   disc and the phased 6 px mark, a symbol while the mark stands in -
   which, on the ordinary ladder, is always. On paper the mark stays in
   page units so screen and sheet agree; printing the true disc on
   high-resolution paper would be a separate ruling.
2. **Conjunctions: no Mercury rule; one Sun amendment.** Painting order
   handles transit and occultation, and the table's state is spoken, as
   for Venus. The Sun's centre dot is a symbol, not a feature of the
   disc: **cap it at a symbol's size** (proposed: its present rule up to a
   3 px radius) so a central transit reads on every field and a closer
   page is not a black disc. That moves released Sun ink at the fields
   where the dot is larger than 3 px - the Sun's on-the-chart evidence
   and its journeys - and is the one released-output change this proposal
   asks for. The alternative is to leave the dot and accept the five
   faint transits and the black closer page.
3. **Labels, C4-C6, selection, Centre on chart: inherited.** Names
   refused inside the Sun's ink stay refused; below the horizon quieted;
   a selected Mercury is an occurrence (#513). *Mercury on the chart*
   joins View > Solar System between Sun on the chart and Venus on the
   chart (proposed access letter **C**, Norwegian **K**, both free in that
   submenu), and the Controller's Mercury group gains Show on chart and
   Centre on chart, as Venus's did on #514. Words, proposed:
   `page.body.mercury` = *Mercury* / *Merkur*, its symbol note, and the
   states `mercury.inFront` / `mercury.behind`. The Solar System labels
   class (#526) includes Mercury by the bodies layer's own classification;
   #537 does not touch it - the class's availability is a contributed
   body, which zoom never removes.
4. **#481 in 5.3.0: yes.** Without a closer field Mercury's phase is
   never visible on screen - not in one week of the century at 1° or
   0.5° - and the road is *Mercury and closer planetary views*. Venus
   gains more than any other body at 0.5° (19.5 % to 52.9 % of its weeks).
5. **The closer fields and their contract**, proposed:
   - **Fields:** two rungs below the floor, **0.5° and 0.25°**. Not 0.1°:
     Mercury gains a quarter more weeks there, but the grid, the empty
     page and the Sun's dot all worsen, and the phases it adds are the
     slivers the criterion barely admits.
   - **A general chart capability with an object-dependent invitation**,
     not a Mercury view: the page stays the chart, with its grid, title,
     export and selection; no magnifier panel.
   - **Eligibility:** the closer step is offered when a supported body
     stands on the page the step would show - around the pointer for
     pointer-centred zoom, around the centre for the button and the
     keyboard. Being in view and staying in view is the rule; neither
     selection nor Centre on chart is required (Centre on chart still
     lands at the ordinary 1°, from which the step is one press away).
   - **Lifecycle:** panning never changes the field. If the qualifying
     body leaves a closer page, the page keeps its field, the zoom-in
     control is unavailable, and the title block says why (*closer field:
     no Mercury or Venus on this page*); zooming out returns to the ladder.
     A selection change does nothing to the field. Home goes to the
     opening page as always.
   - **Controls and keys:** the same zoom-in and zoom-out buttons, in the
     toolbar and the Controller, and the same shortcuts; the zoom-in
     control's explanation names the closer step and what makes it
     available. The zoom lock locks the wheel as it does now.
   - **Titles, report and export:** the field printed to its own
     precision (0.25°, 0.5°) and the page called a closer field; the view
     report names it; export allowed, the sheet saying the same.
   - **Persistence:** none. A closer field is never saved; a restored
     view opens at the nearest ladder rung, 1°, because the body has
     moved since.
   - **Before the UI exposes it:** the field's formatting, finer grid
     steps (15s, 2′), the catalogue queried at the page's own field, the
     Sun's dot (ruling 2), and the projection, labels, export, keyboard,
     persistence and evidence proven at both rungs - #481's own list.
6. **Which bodies inherit:** each supported body declares its useful
   closer field from its own measurement. Now: **Mercury and Venus**, both
   rungs. Later, by their own studies: Jupiter (its oblate disc and moon
   transits, measured on #482 to need closer than 1°), Mars (its gibbous phase and
   changing size), Saturn (its rings). Not the Sun or the Moon - their discs
   already fill the ordinary pages.
7. **The production split**, proposed:
   1. *the Sun's centre dot* (if ruling 2 caps it): one small Sun ink
      change with its evidence;
   2. *Mercury on the chart* on the ordinary ladder: rule M, words, the
      module's switch, menu and Controller, selection, Centre on chart,
      evidence and a packaged checkpoint;
   3. *closer-field foundations*, no UI: field formatting, grid steps,
      the catalogue query, title, report and export at 0.5° and 0.25°,
      evidence pages, the eligibility seam;
   4. *closer fields exposed*: eligibility, controls, keys, lifecycle,
      packaged checkpoint;
   5. the Venus article; 6. 5.3.0's preparation and release.

## Evidence this study moves, and what must not

`docs/studies/mercury-cartography/` is new: the report, its platform
record and 36 study pages, registered with the evidence contract (a
deterministic report, a platform record, the pages held by the image
contract). `MercuryCartographyTest` is new. No production code, chart,
table-result or export bytes move; no language key; no Mercury layer; no
change to the field ladder; no download.

## The ruling

The owner's rulings (2026-10-15), approving all seven recommendations,
recorded as given:

1. **Size: rule M**, as for Venus. Printed marks retain their size in
   page units.
2. **Conjunctions:** no Mercury-specific exception; the table's state is
   represented. The Sun's centre dot is capped at approximately a 3 px
   radius - the sole change to existing released ink.
3. **Interaction:** Venus's labels, selection and Centre on chart
   inherited. *Mercury on the chart* between Sun and Venus, C in English
   and K in Norwegian; Show on chart and Centre on chart in the
   Controller's Mercury group.
4. **#481 is in 5.3.0.** Visible phase on screen is an acceptance
   requirement; merely drawing Mercury does not satisfy it.
5. **Closer fields 0.5° and 0.25°.** Each step offered when Mercury or
   Venus lies within the page that step would display. Panning preserves
   the field; Home keeps its current behaviour. Titles show sufficient
   precision, export remains available, and closer fields are never
   persisted. Exposure is blocked until all four foundations are
   complete: field formatting, finer grid steps, catalogue queries at the
   actual closer field, and the Sun-dot cap.
6. **Inheritance:** Mercury and Venus only for now; Jupiter, Mars and
   Saturn each require their own study; the Sun and the Moon excluded.
7. **Production split:** the six PRs in the stated order, each
   independently reviewable, with #481 placed explicitly in the sequence:

   | step | issue | |
   |---|---|---|
   | 1 | #541 | Cap the Sun's centre dot |
   | 2 | #542 | Draw Mercury on the chart |
   | 3 | #543 | Closer-field foundations, no UI (part of #481) |
   | 4 | #544 | Offer the closer fields (closes #481) |
   | 5 | #545 | The Venus article |
   | 6 | #546 | Prepare and publish 5.3.0 |

This approves the design and sequence; each merge and the release remain
separate checkpoints.
