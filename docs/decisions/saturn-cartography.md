# Drawing Saturn, its rings and the seven moons

**Sprint 53, issue #578.** This is the cartography contract for Saturn, its
rings and the seven ruled moons, proposed from measurement before any
production Saturn ink exists. It is for the owner's rulings; nothing here
is built. It inherits the Sun's and the Moon's cartography
(`solar-cartography.md`, C1-C6), Jupiter's (`jovian-cartography.md`,
rulings 1-9, and the symbol layering rule of #514), Mercury's and the
closer fields' (`mercury-cartography.md`), and the numerical contract
ruled on #574 and amended on #575 (`saturn-system.md`), as the handover
`saturn-handover.md` section 5 hands them over.

The study is `docs/studies/saturn-cartography/`: `make
saturn-cartography-study` builds the portable report `measurements.md`,
this machine's `platform.md` and 36 mock-up pages, from the accepted
Saturn service and the production projections and renderer. Every
number quoted here is that report's, or the numerical contract's where
named; none is typed from memory.

## What was measured

**Size.** Over 1900-2100 (10,488 weekly geocentric instants) Saturn's
equatorial diameter is 15.0-20.7″ and its polar diameter at the ring
opening 13.8-19.1″; the A Ring's outer edge spans 34.1-47.0″ across its
major axis and at most 21.0″ across its minor axis, at the widest
openings (B = +26.97° on 2076-10-12 and −26.99° on 2003-04-07). The gap
between the globe's limb and the C Ring's inner edge is 1.77-2.44″; the
Cassini Division is at most 0.77″ wide. Over 2000-2100 the largest moon
disc is Titan's 0.88″; the system reaches 625″ (10.4′, Iapetus,
2062-12-23), Titan 215″. On the 900 × 700 page:

| field | globe px | rings major px | rings minor px (max) | globe-ring gap px | Cassini px | Titan px | extent px |
|---:|---|---|---:|---:|---:|---:|---:|
| 8° | 0.47-0.65 | 1.06-1.47 | 0.65 | 0.08 | 0.02 | 0.028 | 19 |
| 3° | 1.25-1.72 | 2.84-3.91 | 1.75 | 0.20 | 0.06 | 0.074 | 52 |
| 2° | 1.88-2.59 | 4.27-5.87 | 2.62 | 0.31 | 0.10 | 0.111 | 78 |
| 1° | 3.76-5.17 | 8.53-11.74 | 5.24 | 0.61 | 0.19 | 0.221 | 156 |
| 0.5° (closer) | 7.52-10.35 | 17.07-23.48 | 10.48 | 1.22 | 0.38 | 0.442 | 312 |
| 0.25° (closer) | 15.04-20.70 | 34.13-46.97 | 20.96 | 2.44 | 0.77 | 0.885 | 625 |

**What those sizes mean** (section B of the report):

- **The rings are the figure's size, and the figure is a symbol on
  every ladder field but one.** The rings' major axis reaches Jupiter's
  6 px minimum mark at 1.96° (largest) and 1.42° (smallest): on the
  ordinary ladder the true rings are wider than the mark only at 1°,
  where they are 8.5-11.7 px across. The globe alone reaches 6 px only
  at 0.86°-0.63°: below the floor in every week.
- **The globe and the rings merge at 1°.** The gap between the limb and
  the C Ring's inner edge is a whole pixel only from 0.61°-0.44°: at 1° it
  is 0.44-0.61 px, at 0.5° 0.89-1.22 px, at 0.25° 1.77-2.44 px. At 1° the
  figure is one ringed shape; the globe stands apart from the rings on the
  closer fields only.
- **The Cassini Division is never a drawn gap on the ladder**: 0.19 px at
  1°, 0.38 px at 0.5°, 0.77 px at 0.25°. The two ruled annuli read as one
  band wherever the gap is under a pixel.
- **Edge-on is common.** In 3,191 of the 10,488 weeks (30.4 %) the rings'
  minor axis is under 2 px at 1° - too thin for a filled band with a
  hole - and in 248 weeks (2.4 %) the unlit face is seen.
- **Saturn's oblateness** is at most 0.48 px at 1°: resolved on the
  closer fields only. The rings, not the globe, carry the figure's shape.
- **Every moon mark is a symbol.** Titan's disc is 0.22 px at 1° and
  0.88 px at 0.25°.
- **The inner moons keep to the rings' line.** At 1° the A Ring's
  semi-major axis is 4.3-5.9 px while Mimas's and Enceladus's separations
  reach at most 8.0 and 10.2 px (the contract's weekly maxima of 32.2″
  and 40.9″): their marks stand within a few pixels of the figure's edge
  along its major axis, and inside the ring ellipse when the rings are
  open.

**Each moon decided on its own**, Jupiter's rule 4 with the drawn figure
(the ring ellipse where the rings are drawn true, the 6 px mark
otherwise) as the primary's extent, over 2026 every 6 hours at Oslo
(1,460 configurations, 10,220 moon places): the share of moon places
not behind Saturn that are drawn is 7.8 % at 12°, 10.6 % at 8°, 24.5 %
at 3°, 32.5 % at 2° and 100 % at 1° and below; a configuration has some
moon drawn in 52.3 % of cases at 12°, 71.6 % at 8°, 99.5 % at 3° and
always from 2°. A moon in front is unresolved (3.9 %) on every ladder
field, since the drawn globe is under 12 px there; it resolves on the
closer fields. A drawn moon's mark lies over the ring ellipse, outside
the globe, in 2.0 % of places at 1°, 1.4 % at 0.5° and 1.2 % at 0.25° -
geometry the page shows by painting order, never a state.

**Labels.** Through the production placement with its eight adjacent
candidates, refused alone when no candidate is clean. No configuration
at any field loses every moon label (enforced each run). **A finding
Jupiter's four moons never met:** on this machine (`platform.md`) at 1°
6,145 moon labels are placed and 3,697 refused - seven names crowd a
figure a dozen pixels wide - and on the closer fields a third are still
refused. Every mark is drawn; the inner moons' names often have no clean
box.

**The frame** (section C, held by `SaturnCartographyTest`): each body at
its own astrometric J2000 place through the page's projection; the pole
derived in the chart's frame (`SaturnSystemService
.poleAngleJ2000Degrees`) agrees with the table's of-date angle carried
back by the atlas's rotation to 0.0007°, while the two frames' angles
differ by up to 0.55°; the ring ellipse's semi-axes on the page are the
contract's asin(R/Δ) and asin(R/Δ)·|sin B| at the page's scale, the
major axis across the pole; the near half of the rings lies away from the
projected pole when B > 0 and toward it when B < 0; a moon's J2000 offset
differs from the table's X/Y by up to 5.95″, so no page draws Saturn's
place plus X/Y.

**The mock-ups** (section D, 36 pages over production pages, English and
Norwegian, white paper and black sky): the card's instant from 36° to
0.25°; the rings widest north and south, edge-on at the 2025 Earth
crossing, and the unlit face between the 2025 crossings, at 1° and
0.25°; state vocabulary A with the figure; Titan at greatest elongation;
Iapetus at its farthest; the most crowded seven-moon configuration; a
moon in front, behind, in shadow and partly in shadow, at 0.25° and 1°;
below and above a drawn horizon; the 2018 Sagittarius opposition at 8°
and 3°; the 2026 conjunction with the production Sun; a page edge; the
A4 sheet at 8° and 1°.

## The proposal, for the rulings (#578's eight)

1. **Saturn's mark: the true figure, or Jupiter's 6 px minimum.** The
   figure - globe and rings - at true scale, or a 6 px circle standing
   for the whole figure, whichever is larger, measured by the rings'
   major axis. On the ordinary ladder the true figure is drawn only at
   1° (8.5-11.7 px) and on the closer fields; from 2° up the symbol
   stands in. The transition is a switch, not Jupiter's blend: a circle
   cannot grow into a ringed figure. Evidence and accessible text call the
   symbol *a cartographic symbol, not Saturn's apparent diameter*. While
   the symbol stands in it is painted in the reference layer beneath the
   stars and consumes no click, as Jupiter's; the true figure is painted
   in the bodies layer, opaque, as Jupiter's true disc.
2. **The rings.** The two ruled annuli, flat, their semi-axes asin(R/Δ)
   and asin(R/Δ)·|sin B| at the page's scale, the major axis along the
   pole's page direction turned 90°; filled, the far half painted behind
   the globe and the near half over it by the contract's −sin B cos B
   rule; a hairline outline on the A Ring's outer edge only where the
   figure is at least 20 px wide and the rings' minor axis at least 6 px.
   Where the minor axis is under 2 px the rings are a 1 px line through
   the globe - edge-on, 30.4 % of the century's weeks at 1°. When the
   unlit face is seen the rings take a dimmed ink, so the page says what
   the table says. The Cassini gap is drawn only where it is a pixel (the
   closest page). No shadows, as #574's ruling 4 keeps them out of 5.4.0.
3. **The moons.** Seven 3 px symbols, Jupiter's rule 4 with the drawn
   figure as the primary's extent: behind Saturn omitted; at the normal
   minimum field and the closer fields every other moon drawn at its
   exact J2000 place, overlap allowed; above it only where its mark is
   6 px clear of the figure's drawn edge and of every higher-priority
   moon (in front, clear, shadowed; then Titan, Rhea, Iapetus, Dione,
   Tethys, Enceladus, Mimas); a moon in front only over a globe at least
   12 px wide, the closer fields. A collision suppresses only the
   lower-priority mark. Labels through the adjacent candidates, refused
   alone, never under duress - and often refused at 1°, the finding
   above: **the owner may prefer a rule that names only the moons whose
   marks stand clear of the figure**, or accept the refusals as Jupiter's
   rule gives them.
4. **Vocabulary on the chart.** Jupiter's vocabulary A for the six
   states: clear a filled dot; in front a filled dot ringed in the page's
   ground over the globe; behind not drawn; wholly or partly in shadow a
   hollow ring (the table keeps the distinction). No ring-obscuration
   state, as #574's ruling 5: a clear moon whose sight line crosses the
   rings is painted over them whether nearer or farther, and spoken as
   clear. No satellite shadow spots, no ring shadow, no globe shadow.
   Spoken: *Saturn (a cartographic symbol, not Saturn's apparent
   diameter)* while the symbol stands in, *Saturn* when the figure is
   true; each moon with the table's state words (`saturntable.state.*`)
   and the symbol note. Words proposed: `page.body.saturn` = *Saturn*
   in both languages with its symbol note; `page.body.saturn.<moon>`
   the table's moon names, identical in both languages.
5. **Selection and Centre on chart.** Saturn's identity on the page is
   `saturn`; a selected Saturn is an occurrence (#513), as Jupiter's. The
   moons are `saturn.mimas` … `saturn.iapetus`, symbols that consume no
   click, as the Galilean moons; proposed not selectable. Centre on chart
   lands at 1°, as for every body. *Saturn on the chart* joins View >
   Solar System after Jupiter on the chart (access letter to be chosen
   from the free ones in that submenu); the Controller's Saturn group
   gains Show on chart and Centre on chart, as Jupiter's did.
6. **The closer fields: Saturn eligible.** Saturn joins Mercury, Venus
   and Jupiter for 0.5° and 0.25°: at 0.25° the rings are 34.1-47.0 px,
   the globe separates from them (the gap 1.77-2.44 px), the inner moons
   stand clear of the ring ellipse and a moon in front is resolved. The
   closer pages are painted in section D; the ladder itself is untouched.
7. **Screen, paper, accessibility and export.** Below a drawn horizon the
   figure and its moons take the ground's dimmed ink with the status; off
   the page nothing is drawn; the symbol never erases a star and a symbol
   in front of an opaque disc paints over it (#514); export draws through
   the same module in page units, so the A4 sheet agrees with the screen
   (the printable pages); the Solar System labels class (#526) raises
   Saturn's and the moons' names.
8. **The production split, for #579.** In the ruled order, each a
   wide-route PR with its on-the-chart evidence: (a) the figure - globe
   and rings, the switch, Centre on chart, the words, closer-field
   eligibility - with a packaged owner checkpoint; (b) the seven moons,
   their marks, states and labels, with the packaged checkpoint #579
   names. Then #580 and #581.

## Evidence this study moves, and what must not

This study adds `docs/studies/saturn-cartography/` (the report, this
machine's platform record and the 36 pages), registers its generator in
the report, platform and image registries, adds `SaturnCartographyTest`,
and requotes the test-evidence study. **No released chart output moves:**
the study draws its candidate figure over pages the production renderer
painted, in a study-only step, and offers nothing to the production
bodies layer but the Sun on one page. The pack, the service, the table
and every earlier body's evidence are unchanged.

## The ruling

The owner's rulings of 2026-10-21 on the eight proposals, recorded as
given:

1. **Saturn's mark.** The proposed rule is accepted: draw the larger of
   the true globe-and-rings figure and the 6 px cartographic minimum,
   measured by the rings' major axis. It is a discrete switch, never a
   blended scale. The symbol lies beneath the stars; the true figure is
   opaque.
2. **The rings.** The proposed two-annulus treatment is accepted: the
   near-half ordering, the outline thresholds, the edge-on 1 px line, the
   dimmed unlit face, the conditional Cassini gap, and no shadows.
3. **The moons.** The seven moons are drawn as 3 px symbols under
   Jupiter's separability rule, with Saturn's drawn globe-and-rings figure
   as the primary's extent. **Labels are attempted for every drawn moon,
   and individual refusals accepted.** A label is not precluded merely
   because the moon's mark lies over or close to the figure; those are
   often the most interesting configurations. A refused label must not
   block another label.
4. **Vocabulary.** Vocabulary A, the proposed spoken and page wording,
   and no unverified ring-obscuration state are accepted.
5. **Selection and controls.** The `saturn` and `saturn.<moon>`
   identities are accepted, with the moons unselectable. Centre on chart
   uses 1°. Saturn is placed after Jupiter, in outward-from-the-Sun
   order.
6. **The closer fields.** Saturn is eligible for 0.5° and 0.25°.
7. **The inherited contracts.** Horizon, page edge, star precedence,
   export sizing and Solar System label emphasis are accepted as
   proposed.
8. **The implementation split.** Two wide-route PRs for #579:
   - Saturn's globe and rings, including closer-field eligibility;
   - the seven moons and their states, symbols, labels and suppression.

   Each PR stops at its own packaged checkpoint before merging.

**Where the ruling departs from the proposal.** Nowhere in substance;
on proposal 3 the owner chose the first of the two offered label
policies - every drawn moon's label attempted, refusals accepted one by
one - and declined any rule that withholds a name near the figure.

No production Saturn drawing begins before this study's landing.

## Saturn on the chart, as built (#579, the figure)

The first of #579's two pull requests under the rulings above: Saturn's
globe and rings, the switch, Centre on chart, the words and the
closer-field eligibility. The moons follow in the second.

- **One module, one switch.** `juranometria.saturnchart.SaturnModule`
  offers Saturn while *Saturn on the chart* is on. The switch is off by
  default and remembered (`SaturnChartStore`, `saturn.onChartShown`).
  View > Solar System carries it directly after *Jupiter and moons on the
  chart* (access letter A in both languages), and the Controller's Saturn
  group carries the same switch as a box with Centre on chart beside it,
  as Jupiter's group does; the Saturn dialog gains Centre on chart too.
  All of them follow the module, the single visibility authority of
  #458. Place and Time's box on the chart counts the switch among the
  layers that bring it.
- **The contribution.** Saturn is an `OverlayContribution.RingedBody`:
  the service's astrometric J2000 place, its equatorial diameter and the
  polar diameter at the ring opening, the pole derived in the chart's
  frame (`SaturnSystemService.poleAngleJ2000Degrees`), the planetocentric
  opening B and whether the unlit face is seen, the ruled four ring edges,
  and Jupiter's 6 px minimum. `SaturnModuleTest` holds each equal to the
  service's.
- **Ruling 1: the switch.** `ReferenceInk.ringedOn` measures the figure
  by the rings' major axis at the page's scale. Under 6 px the mark is a
  6 px circle, a cartographic symbol painted in the reference layer
  beneath the catalogue's marks, consuming no click and spoken as *Saturn
  (a cartographic symbol, not Saturn's apparent diameter)*. From 6 px the
  true figure is painted in the bodies layer, opaque, spoken as *Saturn*;
  a click on it is Saturn's occurrence (#513). On the production pages
  the figure is the symbol on every ladder field above 1° and the true
  figure at 1° (12.2 × 4.5 px at the card instant), 0.5° (22.3 × 8.9 px)
  and 0.25° (44.6 × 17.8 px) - `docs/studies/saturn-on-the-chart`.
- **Ruling 2: the rings.** The two ruled annuli at asin(R/Δ) and
  asin(R/Δ)·|sin B|, the Cassini gap cut between them only on the arcs
  about the ansae where it is a pixel across - its width at the ansae
  foreshortened toward the minor axis by sin B - and one band elsewhere;
  the major axis across the pole turned by the page's own north and east
  (`PageBasis`); the far half painted behind the globe and the near half
  over it, the near half away from the projected pole when B > 0 and
  toward it when B < 0; a minor axis under 2 px drawn as a 1 px line over
  the globe; the A Ring's outer edge outlined only where the figure is at
  least 20 px wide and the minor axis at least 6 px; the ring ink two
  tones between the star ink and the ground, both quieter when the unlit
  face is seen, all dimmed below a drawn horizon. No shadows.
  `SaturnInkTest` holds each rule on the page.
  *The checkpoint finding (2026-10-21):* at the first packaged checkpoint
  the owner saw two dark dots at twelve o'clock on the open rings of
  2034-01-08 at 0.25°. The gap had been decided once, by its width at the
  ansae, and then cut all the way round, so toward the minor axis - where
  it is only sin B as wide - a sub-pixel sliver was painted as a row of
  dots on the far half. The cut is now confined to where the gap is a
  pixel across; the study's 900 px pages never reach a pixel and did not
  move; `SaturnInkTest` holds the gap open at the ansae and closed on the
  minor axis at 1.9 px by 0.95 px, and open all round at 3.8 px by 1.9 px.
- **Ruling 5: identity and words.** `saturn`, named from
  `page.body.saturn` with its symbol note in both languages; Centre on
  chart lands at the chart's normal minimum field, through #483's seam
  (`CentreOnChart.Body.SATURN`).
- **Ruling 6: the closer fields.** Saturn joins the bodies named to
  `ChartComponent.setCloserFieldBodies`; the zoom-in explanations and the
  title block's note now name *Mercury, Venus, Jupiter or Saturn*.
  `CloserFieldJourneyTest` zooms to 0.25° on Saturn at the card instant.
- **Ruling 7: inherited.** Below a drawn horizon the figure takes the
  ground's dimmed ink with the status; off the page nothing is drawn; the
  symbol never erases a star; export draws through the same module in
  page units (the study's A4 sheet). The Solar System labels class
  raises Saturn's name with the other bodies', through the bodies layer's
  own classification.
- **Packaged acceptance** runs the journey in every image, printing
  *saturn on the chart OK*.
- **Not in this pull request:** the seven moons, their marks, states,
  labels and suppression (the second pull request).
