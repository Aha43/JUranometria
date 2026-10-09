# Drawing Jupiter and the Galilean moons

**Sprint 45, issue #482.** The proposed cartography contract for Jupiter
and its four Galilean moons, from the measured study
(`docs/studies/jovian-cartography/`). This record is a **proposal
awaiting the owner's rulings**. No production rendering exists, and none
begins before the rulings.

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
- **The moons are rarely separable.** Over 2026, a 3 px symbol for each
  moon is clear of every other and of Jupiter's mark in 56 % of
  configurations at 1°, 11 % at 3°, and 0.1 % at 8°. A label line
  between every pair fits in 21 % at 1°.

**The frame.** Measured over the century:

- **The pole.** Jupiter's pole derived in the chart's own frame — the
  PCK pole vector is stated in the ICRS — agrees with the table's
  of-date pole angle, carried back by the atlas's own rotation, to
  0.003°. The two frames' angles themselves differ by up to 0.58°.
- **The offsets.** A moon's J2000 offset from Jupiter differs from the
  table's X/Y by up to 6.0″.

So neither the table's angle nor its offsets can be drawn as they stand.

## The frame contract, proposed and held

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

## For the rulings

The issue's eight product questions. Each proposal is drawn in the study's
mock-ups over production pages, in both palettes and both languages.

1. **One remembered switch, *Jupiter and moons on the chart*, off by
   default.** It follows the Sun's and the Moon's model.
2. **Jupiter at ordinary fields gets a cartographic mark and its label.**
   The mark is a disc of 6 px or its true size, whichever is larger,
   stated as a symbol. This departs from C1 because true scale loses
   Jupiter.
3. **Jupiter once resolvable is drawn as its true oblate disc**, with no
   bands, Great Red Spot or rotation.
   - The minor axis lies along the pole derived in the chart's frame.
   - The transition is continuous: a circle at the 6 px minimum, reaching
     the true axis ratio as the true disc grows to 12 px.
4. **The moons are symbols, never apparent diameters.** Each is a 3 px
   dot, drawn only where the system is separable. Elsewhere Jupiter's
   mark alone stands for the system.
5. **The moons' states, candidate A.**
   - Clear: a filled dot.
   - In front of Jupiter: a filled dot ringed in paper over the disc.
   - Behind Jupiter: not drawn, and its label refused.
   - Wholly or partly in shadow: a hollow ring.

   The alternative, B, draws a dashed ghost behind and grey in shadow.
   Satellite shadow spots on Jupiter are not drawn, because the service
   does not compute them.
6. **Moon labels** are drawn only where they have room. Each goes to the
   right of its mark and is refused rather than overlapping. Jupiter's
   label follows C4: adjacent candidates, never overwriting a star's.
7. **Horizon, page edge, stars, clicking and export follow C4 and C6.**
   - Below a drawn horizon, Jupiter is dimmed with its status; with the
     horizon hidden, it is drawn normally.
   - Off the page, nothing is drawn.
   - No star is hidden while Jupiter is a symbol.
   - A click may consume the hit but never exposes a hidden star.
   - An exported sheet draws what the page draws.
8. ***Centre on chart* for Jupiter** uses #483's accepted action through
   its prepared seam. Its button lands with the visible module (#484).

**A smaller field later (#481).** The mark's minimum, the transition and
the separability thresholds are all measured in pixels, and the
positions in J2000. A field below 1° therefore needs none of them
changed. None is exposed here.

## Evidence

- **The study:** `src/juranometria/tool/JovianCartographyStudyMain.java`
  (`make jovian-cartography-study`), with
  `src/juranometria/tool/JovianChartGeometry.java` holding the frame
  contract's arithmetic.
- **The contract test:** `test/juranometria/tool/JovianChartFrameTest.java`.
- **The report and mock-ups:** `docs/studies/jovian-cartography/`. The
  report is a registered deterministic report. The 18 mock-ups are
  registered image generators under the renderer-drawn contract, with
  their provenance rows, as #414's were.
