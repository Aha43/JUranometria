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
  3°. At 1° every one is drawn, and production's adjacent candidates
  place 96 % of their labels. No configuration loses every moon label.

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

## Evidence

- **The study:** `src/juranometria/tool/JovianCartographyStudyMain.java`
  (`make jovian-cartography-study`), with
  `src/juranometria/tool/JovianChartGeometry.java` holding the frame
  contract's arithmetic.
- **The contract test:** `test/juranometria/tool/JovianChartFrameTest.java`.
- **The report and mock-ups:** `docs/studies/jovian-cartography/`. The
  report is a registered deterministic report. The 20 mock-ups are
  registered image generators under the renderer-drawn contract, with
  their provenance rows, as #414's were.
