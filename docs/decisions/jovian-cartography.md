# Drawing Jupiter and the Galilean moons

**Sprint 45, issue #482.** The cartography contract for Jupiter and its
four Galilean moons, from the measured study
(`docs/studies/jovian-cartography/`), as the owner ruled it on #482. The
study's first proposal was amended to these rulings before it merged. No
production rendering exists here: that is #484.

## The question

The numbers are accepted (`jovian-system.md`, #472–#475), and so is the
shared *Centre on chart* journey (#483). Drawing Jupiter and its moons
raises questions the numbers do not answer:

- how large Jupiter is on a page;
- what stands for a moon whose disc is far below a pixel;
- which frame the marks are drawn in, when the table's offsets and pole
  angle are apparent and of date while the chart is J2000;
- how the moons' states are shown;
- when moon labels have room;
- how a smaller field later (#481) would change any of this.

## What was measured

The measurements are in `measurements.md`, section A to C. They come
from the accepted service and the production projections.

**Size.** Over 2000–2100, Jupiter's equatorial diameter is 30.5–49.9″
and its polar diameter 28.6–46.6″. Each moon's is 0.67–1.84″. The system
reaches 655″, Callisto in 2057. On the 900 × 700 page:

| field | Jupiter (px) | largest moon (px) | system extent (px) | Jupiter on A4 (mm) |
|---:|---|---:|---:|---:|
| 36° | 0.21–0.33 | 0.012 | 4.4 | 0.11 |
| 8° | 0.95–1.56 | 0.057 | 20.4 | 0.47 |
| 3° | 2.55–4.15 | 0.153 | 54.6 | 1.25 |
| 1° | 7.64–12.47 | 0.459 | 163.7 | 3.76 |

The atlas's smallest star mark is 1.4 px across and its largest 10 px,
both from the production star policy.

**What those sizes mean:**

- **Jupiter vanishes at true scale on most pages.** It is smaller than
  the smallest star mark at every field above 5.5–8.9°. The Sun and the
  Moon were never in that position (C1 of #414).
- **The oblate figure is never visible on screen.** Its two axes are
  only 0.81 px apart at 1°, so its oblateness cannot be honestly
  resolved at any field the atlas offers. It needs a field below 1°
  (#481), or paper, where A4 at 300 dpi resolves it from 2.9°.
- **A moon's mark is always a symbol.** Every moon's true disc is
  below a pixel at every field.
- **The whole system is rarely separable, but a moon often is.** Over
  2026 the first proposal's rule, all moons only when every mark is
  separable, drew the moons in 56 % of configurations at 1°, 11 % at 3°
  and 0.1 % at 8°. Decided one moon at a time (section B of the
  report), 22 % of the visible moon places are drawn at 8° and 60 % at
  3°. At 1° every one is drawn.
- **Label counts belong to the machine, the invariant does not.**
  Production placement sizes each label with the desktop's real font
  metrics, so how many moon labels are placed, moved or refused differs
  between machines. CI's Linux run on `aa142b99` refused 236 at 1°
  where macOS refused 225, and neither is wrong. Those counts are in the
  study's platform record, not its portable report. On every platform,
  no configuration loses every moon label. The study enforces this
  each time it runs, and `JovianLabelInvariantTest` holds it.

**The frame.** Measured over the century:

- **The pole.** Jupiter's pole derived in the chart's own frame — the
  PCK pole vector is stated in the ICRS — agrees with the table's
  of-date pole angle, carried back by the atlas's own rotation, to
  0.003°. The two frames' angles themselves differ by up to 0.58°.
- **The offsets.** A moon's J2000 offset from Jupiter differs from the
  table's X/Y by up to 6.0″.

So neither the table's angle nor its offsets can be drawn as they stand.

## The frame contract, as ruled and held

`JovianChartFrameTest` holds this contract:

- **Positions.** Each body is drawn at its own astrometric J2000 place
  from the service, through the page's ordinary projection.
- **The pole.** Jupiter's pole is derived from the PCK vector at the
  body's J2000 place, then turned onto the page by the page's projected
  north and east there — the Moon's phase contract, C3.
- **No assumed rotation.** The table's values are never rotated by an
  assumed correction.
- **What the test proves:**
  - every place round-trips through the projection;
  - a moon's page offset is its J2000 offset turned by the page's
    tangents, at the poles, the equator and RA 0/24h;
  - Jupiter moves under 0.01 px per second at 1° across its JUP365
    segment boundary at 1997-01-16.

## The rulings

The owner ruled on the issue's product questions on #482. Each is drawn
in the study's mock-ups over production pages, in both palettes and both
languages.

1. **One switch, *Jupiter and moons on the chart*, off by default and
   remembered.** It follows the Sun's and the Moon's model.
2. **Jupiter's mark is 6 px or its true size, whichever is larger.**
   True scale alone would lose Jupiter at most fields, so this departs
   from C1. Evidence and accessible text call the minimum *a
   cartographic symbol, not Jupiter's apparent diameter*.
3. **The true figure and pole are drawn**, through the continuous
   transition and the J2000-derived pole.
   - The mark is a circle at the 6 px minimum and reaches the true axis
     ratio as the true disc grows to 12 px.
   - The minor axis lies along the pole derived in the chart's frame.
   - No bands, Great Red Spot or rotation.
   - **The flattening is not claimed to be visibly resolved at the
     current 1° floor.** There the two axes are under a pixel apart.
4. **The moons are 3 px symbolic marks, each decided on its own.**
   - At the normal minimum field, 1° today, every moon that can be drawn
     is drawn at its exact J2000 position, and overlap is allowed.
   - A moon in front of Jupiter overlaps it on purpose.
   - A moon behind Jupiter is omitted.
   - Clear or shadowed moons outside the disc are decided independently.
     A collision suppresses only the lower-priority mark or label.
   - Above the normal minimum, a moon is drawn only when its own mark is
     distinguishable.
   - Labels are decided separately from marks.
   - The 22:45 triple transit of 11 December 2026 keeps its three
     in-front moon marks at 1°.
5. **State vocabulary A.**
   - Clear: a filled dot.
   - In front of Jupiter: a filled dot with a paper or background ring.
   - Behind Jupiter: omitted.
   - Wholly or partly in shadow: a hollow ring, shared by both. The
     table keeps the difference.
   - No ghost. Satellite shadow spots on Jupiter are not drawn, because
     the service does not compute them.
6. **Labels use adjacent candidates and the existing collision
   machinery.** A label is refused when it has no room, where production
   would otherwise place it under duress. One crowded pair never
   suppresses every moon label.
7. **Horizon, page edge, stars, clicks and export follow C4 and C6.**
   - Below a drawn horizon, Jupiter is dimmed with its status. With the
     horizon hidden, it is drawn normally.
   - Off the page, nothing is drawn.
   - The symbolic mark must not erase a real star.
   - Its hit area must not expose a hidden star.
   - Export draws through the same module.
8. ***Centre on chart* for Jupiter** uses #483's accepted action through
   its prepared seam. Its button lands with the visible module (#484).
9. **Future close zoom (#481).** No geometry, label or evidence code may
   assume that 1° is permanent. The study reads the chart's normal
   minimum field rather than naming 1°.

**Rejected from the first proposal:**

- drawing all moons only when the whole system is separable, which hid
  the triple transit behind a lone Jupiter mark at 1°;
- vocabulary B, which drew a dashed ghost behind Jupiter and grey in
  shadow;
- a fixed rule placing every moon label to the right of its mark.

**What the amended study demonstrates:**

- the 22:45 triple transit at 1°, with the three in-front moon marks
  present on the page;
- independent suppression at 3°, where one colliding moon is not drawn
  and the two clear moons beyond it are;
- labels on adjacent candidates around Jupiter, and a crowded label
  refused alone;
- state vocabulary A on white paper and on black sky.

## Jupiter on the chart, as built (#484)

- **One module, one switch.** `juranometria.jovianchart.JovianModule`
  offers Jupiter while *Jupiter and moons on the chart* is on. The
  switch is off by default and remembered (`JovianChartStore`). View >
  Solar System carries it directly after Moon on the chart, and the
  Controller's Jupiter group carries the same switch as a box. All of
  them follow the module, the single visibility authority of #458.
- **The contribution.** Jupiter is an `OverlayContribution.OblateBody`.
  It carries the service's astrometric J2000 place, the true equatorial
  and polar diameters, the 6 px minimum, and the pole's position angle
  from `JovianSystemService.poleAngleJ2000Degrees`. That angle is the
  study's frame contract moved into production, and
  `JovianModuleTest` holds it equal to the study's derivation over the
  century.
- **Ruling 7: the symbol never erases a star.** While the true disc is
  under the 6 px minimum, the mark is painted in the reference layer,
  which lies beneath every catalogue mark. So a star over the symbol
  stays on top. The symbol hides nothing, so a click through it reaches
  what is under it. Once the true disc reaches the minimum, it is
  painted in the bodies layer like the Sun and the Moon: opaque, and a
  click on it is the empty sky there. On the 900 px screen page that
  happens at the 1° field.
- **Ruling 3: the figure.** The outline is a circle at the minimum and
  reaches the true axis ratio as the true disc grows to 12 px. Its minor
  axis is turned onto the page through `PageBasis`, the page's own north
  and east at Jupiter. At 1° the flattening is drawn but not claimed to
  be visible.
- **Ruling 2: the accessible text.** A symbol is spoken as "Jupiter (a
  cartographic symbol, not Jupiter's apparent diameter)", or in
  Norwegian "Jupiter (et kartografisk symbol, ikke Jupiters
  tilsynelatende diameter)". A true disc is spoken as "Jupiter".
- **Ruling 6: the name.** Jupiter's name goes through the bodies'
  existing collision machinery. It takes the first clean adjacent box
  and is refused on its own when none is clean. That machinery never
  falls back to an overlap. The catalogue's `LabelPlacement` does fall
  back, placing a crowded label "under duress" over the least ink, and
  #485's moon labels must not use that fallback.
- **The years.** Outside 1900–2100 the module offers nothing and the
  table keeps its refusal. From 1900 to 1999 Jupiter is drawn. The
  moons are not, because they have no numbers there.
- **Export** draws both layers from the same contributions, so a sheet
  carries what the screen carries.
- **Carried to #485.** One crowded pair never suppresses the whole
  system: each moon is decided on its own, as section B of the study
  measures. A crowded moon label is refused on its own, never placed
  under duress. No moon rendering begins until Jupiter passes its
  packaged checkpoint.

## The Galilean moons on the chart, as built (#485)

- **Offered by the same module and switch.** For 2000–2100,
  `JovianModule` offers each moon as an `OverlayContribution.Satellite`
  of Jupiter. Each carries the service's own astrometric J2000 place,
  never the table's of-date X/Y. It also carries its disc relation,
  whether it is wholly or partly in shadow, and the 3 px mark. The
  identities are `jovian.io`, `jovian.europa`, `jovian.ganymede` and
  `jovian.callisto`, so a moon can never be mistaken for the Moon.
- **Decided by the page, moon by moon (ruling 4).**
  `juranometria.ui.SatelliteMarks` is the study's rule in production:
  - behind is omitted;
  - at the chart's normal minimum field, read and never assumed, every
    other moon is drawn, and overlap is allowed;
  - above it, a moon in front is drawn only over a disc at least 12 px
    wide;
  - a clear or shadowed moon is drawn only when its mark stands 6 px
    clear of Jupiter's drawn edge and of every higher-precedence moon.
    A collision suppresses only the lower-precedence mark.
  - Precedence is in front, then clear, then shadowed, then Ganymede,
    Callisto, Io and Europa.
  - The 22:45 triple transit keeps its three in-front marks at 1°.
- **Vocabulary A (ruling 5).**
  - clear: a filled dot in star ink;
  - in front: a filled dot ringed in the page's ground, painted over
    Jupiter's disc;
  - wholly or partly in shadow: a hollow ring;
  - behind: nothing, and no ghost.
  `JovianMoonsInkTest` holds this on both palettes.
- **Layers (ruling 7).** A moon's mark is a symbol. A mark clear of
  Jupiter's opaque disc is painted beneath the catalogue's marks, so it
  never erases a star. A mark in front of the disc, or touching it, is
  painted with the disc, over it. No mark consumes a click.
- **Labels, decided after the marks (ruling 6).** Each moon is named in
  precedence order through the bodies' adjacent boxes, around its own
  mark. It is refused on its own when no box is clean, and never
  overlaps. One crowded pair never takes every name with it.
- **Spoken.** Each drawn moon is spoken with its state and as a symbol,
  for example "Io, in front of Jupiter (a cartographic symbol, not Io's
  apparent diameter)". In Norwegian the states are *foran Jupiter* and
  *i Jupiters skygge*, and the names follow the table (*Ganymedes*).
- **Not drawn:** satellite shadow spots on Jupiter. Nor are moons in
  1900–1999, which have no numbers.

## The evidence, integrated once (#486)

- **Production pages.** `JupiterOnTheChartStudyMain`
  (`make jupiter-on-the-chart-study`, `docs/studies/jupiter-on-the-chart/`)
  draws the merged module through the real `ChartComponent`. It covers
  the journeys #485 names:
  - the triple transit at 36°, 8°, 3° and 1°;
  - the 11 December 2026 sequence at 22:30, 22:55 and 23:00;
  - a normal spread at 1° and 3°;
  - Io behind Jupiter, wholly in shadow and partly in shadow;
  - Jupiter below a drawn horizon and with the horizon hidden.

  Both palettes and both languages are covered. The portable report
  holds each body's place against the service, every moon's page
  decision and state, and the spoken text. Every drawn centre is
  0.0000 px from the service's own projection.
- **What does not resolve, recorded and never enlarged.** At each field
  from 36° to 1°, at the triple transit and at the spread, the report
  states why each undrawn moon is not drawn. The reasons are: in front
  of a disc too small to hold its mark, at Jupiter's edge, or colliding.
  The page's decision is published read-only
  (`ReferenceInk.satelliteDecisions`) so the evidence states it rather
  than infers it.
- **The sheet, at a measured scale.** The 42° two-body sheet does not
  resolve the moons. The study measures each supported field on the A4
  sheet's own page at the triple transit and takes the widest at which
  every moon not behind Jupiter is drawn. That is the normal minimum
  field, 1°. `sheet-a4-jovian.png` (300 dpi) and `.pdf` are written
  there.
  - **Pixel for pixel:** the packaged journey *jovian sheet OK* holds
    the application's own export equal to the inspected recording.
  - **Screen and paper:** they draw the same Jupiter disc and figure,
    the same four moons with the same states, and the same names at the
    same places.
  - `ExportSheetSessionTest` holds that the sheet names all five bodies
    only when the switch is on.
  - The journey restates the moment and field, and
    `PackagedJovianSheetTest` holds them to the study's.
- **Platform records.** Where each name went depends on the desktop's
  fonts, so it is the study's `platform.md`, as for the cartography
  study.
- **Interface.** The Controller photographs (`CompanionSheetMain`) now
  show Jupiter's group, as the application holds it. A new state,
  `companion-*-7-jupiter-open`, shows the Jovian module's Show on chart
  box open, with nothing computed.
- **Gallery, awaiting publication approval.** A room *Jupiter and its
  moons* at the gallery's own moment (Oslo, 2026-03-20 21:33 UTC), when
  Jupiter stands 41° up in Gemini with all four moons clear:
  - `jupiter-and-moons`: 1°, the true disc and four moon symbols;
  - `jupiter-in-gemini`: 8°, the 6 px symbol with only Ganymede and
    Callisto told apart.

  Both are drawn by `GalleryPageMain` through the production module.
  Merging publishes them.

## Evidence

- **The study:** `src/juranometria/tool/JovianCartographyStudyMain.java`
  (`make jovian-cartography-study`), with
  `src/juranometria/tool/JovianChartGeometry.java` holding the frame
  contract's arithmetic.
- **The contract tests:** `test/juranometria/tool/JovianChartFrameTest.java`
  for the frame, and `test/juranometria/tool/JovianLabelInvariantTest.java`
  for ruling 6's invariant on whichever platform runs it.
- **The report and mock-ups:** `docs/studies/jovian-cartography/`. The
  report is a registered deterministic report, holding the geometry,
  the per-moon decisions, the candidates and the rules. Its
  `platform.md` is a registered platform record, holding the label
  counts, the label font and each mock-up's label outcomes on the
  machine that generated it. The 20 mock-ups are
  registered image generators under the renderer-drawn contract, with
  their provenance rows, as #414's were.
