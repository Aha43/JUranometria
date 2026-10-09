# Jupiter and the Galilean moons on the page: measurements and mockups

Sprint 45, issue #482. What a cartography rule would mean on the atlas's own pages, measured through the production projections and the accepted Jovian service (#473), with candidate marks drawn by this study over pages the production composition painted, as the Sun-Moon study did (#414). Nothing here is production rendering; every image is a mockup and says so in its corner. Regenerate with `make jovian-cartography-study`.

## A. True scale on every page

Over 2000-2100 (5270 weekly geocentric instants from the service): Jupiter's equatorial diameter is 30.5-49.9″ and its polar diameter 28.6-46.6″; Io 0.78-1.28″, Europa 0.67-1.09″, Ganymede 1.12-1.84″, Callisto 1.03-1.69″; the system's largest extent is a moon 655″ (10.9′) from Jupiter's centre (CALLISTO 2057-09-08). The atlas's smallest star mark is 1.4 px across and its largest 10.0 px (the production star policy).

Pixels on the 900 × 700 page at its centre (each field's own projection); millimetres on the A4 sheet's chart area (272 mm wide, 3208 px at 300 dpi) and the Letter sheet's (254 mm).

| field | Jupiter equator px (min-max) | polar px (max) | equator − polar px (max) | largest moon px | system extent px | Jupiter on A4 mm (max) | extent on A4 mm | Jupiter on Letter mm (max) |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 180° | 0.05-0.08 | 0.07 | 0.005 | 0.0028 | 1.0 | 0.021 | 0.27 | 0.020 |
| 120° | 0.06-0.09 | 0.09 | 0.006 | 0.0035 | 1.2 | 0.031 | 0.41 | 0.029 |
| 90° | 0.08-0.13 | 0.12 | 0.009 | 0.0048 | 1.7 | 0.042 | 0.55 | 0.039 |
| 60° | 0.12-0.20 | 0.19 | 0.013 | 0.0075 | 2.7 | 0.063 | 0.82 | 0.059 |
| 42° | 0.17-0.28 | 0.27 | 0.018 | 0.0104 | 3.7 | 0.090 | 1.18 | 0.084 |
| 36° | 0.21-0.33 | 0.31 | 0.022 | 0.0123 | 4.4 | 0.105 | 1.37 | 0.098 |
| 24° | 0.31-0.51 | 0.48 | 0.033 | 0.0189 | 6.7 | 0.157 | 2.06 | 0.147 |
| 18° | 0.42-0.69 | 0.64 | 0.045 | 0.0253 | 9.0 | 0.209 | 2.75 | 0.195 |
| 12° | 0.63-1.04 | 0.97 | 0.067 | 0.0381 | 13.6 | 0.314 | 4.12 | 0.293 |
| 8° | 0.95-1.56 | 1.45 | 0.101 | 0.0573 | 20.4 | 0.470 | 6.18 | 0.440 |
| 6° | 1.27-2.08 | 1.94 | 0.135 | 0.0765 | 27.3 | 0.627 | 8.24 | 0.586 |
| 4° | 1.91-3.12 | 2.91 | 0.202 | 0.1148 | 40.9 | 0.941 | 12.35 | 0.880 |
| 3° | 2.55-4.15 | 3.89 | 0.270 | 0.1531 | 54.6 | 1.254 | 16.47 | 1.173 |
| 2° | 3.82-6.23 | 5.83 | 0.404 | 0.2297 | 81.9 | 1.881 | 24.71 | 1.759 |
| 1° | 7.64-12.47 | 11.66 | 0.809 | 0.4594 | 163.7 | 3.762 | 49.41 | 3.519 |

## B. Where things become resolvable or separable

- **Jupiter's disc reaches the atlas's smallest star mark** (1.4 px) at 8.9° at its largest and 5.5° at its smallest; it reaches the largest star mark (10.0 px) at 1.25° and 0.76°.
- **Jupiter's disc is a shape, not a dot** (3 px across) from 4.2° at its largest, 2.5° at its smallest; it reaches the proposed 6 px minimum mark at 2.08° and 1.27°.
- **Jupiter's oblateness is honestly resolved** - the equatorial and polar diameters a whole pixel apart - only at 0.81° on the 900 px page at its largest (0.81 px apart at 1°): **below the atlas's smallest field**. At 1° the true disc is 7.6-12.5 px across, a disc whose oblate outline is within a pixel of a circle. On the A4 sheet at 300 dpi it is resolved from 2.9°. Drawing the outline oblate is truthful at any size; seeing it needs a field below 1° (#481) or paper.
- **The moons' true discs are below a pixel on every page** (Ganymede, the largest, 0.46 px at 1°): any moon mark is a symbol. Proposed symbol 3 px; two symbols are separable 6 px apart (centre to centre).

Separability over 2026 (every 6 hours at Oslo, 1 460 configurations): the share in which every pair of moons, and every moon and Jupiter's mark, are at least the given page distance apart, by field. *Marks* uses the symbol rule above against Jupiter's drawn radius; *labels* uses a nominal label line (14 px) between every pair. A moon behind Jupiter is left out (it is not drawn under any candidate).

| field | marks separable | labels have room | median closest pair (px) |
|---:|---:|---:|---:|
| 12° | 0.0 % | 0.0 % | -0.2 |
| 8° | 0.1 % | 0.0 % | 0.3 |
| 6° | 0.3 % | 0.0 % | 0.6 |
| 4° | 4.2 % | 0.1 % | 1.4 |
| 3° | 10.5 % | 0.3 % | 2.1 |
| 2° | 27.3 % | 2.9 % | 3.5 |
| 1° | 55.7 % | 21.0 % | 6.9 |

## C. The frame contract

The chart is J2000 (ICRS); the table's X/Y and pole angle are apparent and of date. The study does **not** rotate the table's values by an assumed correction. It proposes, and `JovianChartFrameTest` holds:

- **Positions**: each body's own astrometric J2000 place from the service - Jupiter's and each moon's - through the page's ordinary projection, as every star's and the Sun's and the Moon's.
- **Jupiter's pole for the ellipse**: the PCK's pole vector is stated in the ICRS, the chart's frame, so its direction on the sky at Jupiter's J2000 place is derived there, from J2000 north through east, and turned onto the page by the page's projected north and east at that place (the Moon's phase contract, C3). Measured over 2000-2100: the derived angle agrees with the table's of-date pole angle carried back by the atlas's own of-date-to-J2000 rotation to **0.0029°**; the two frames' angles themselves differ by up to 0.58° - which is why the table's angle is never drawn as it stands.
- **The table's offsets are not the page's**: a moon's J2000 offset from Jupiter differs from the table's apparent X/Y by up to **6.00″** over the century - the frames' rotation at Callisto's distance - so a page draws each moon at its own J2000 place, never at Jupiter's place plus X/Y.
- **Round trip and neighbours**: every body's J2000 place projected and unprojected returns to itself; a moon's page offset from Jupiter equals its J2000 offset turned by the page's tangents, across the sky (the poles, the equator, RA 0/24h) and through Jupiter's JUP365 segment boundary at 1997-01-16 (Jupiter only: the moons answer from 2000).
- **Agreement**: the page position of each body is the service's J2000 position, not a second computation; the test compares them directly.

## D. Mockups on the atlas's own pages

Each page is painted by the production composition (`ChartComponent` over the bundled catalogue, with the meridian module where the page has a horizon). Over it this study draws, from the service's J2000 places for the instant and observer stated: **Jupiter** as a disc at its true equatorial diameter or the proposed 6 px minimum mark, whichever is larger, its outline turning from a circle to the true oblate ellipse as the true disc grows past the minimum, the minor axis along the pole derived in the chart's frame; **each moon** as a 3 px symbol at its own J2000 place, in the state vocabulary of candidate A (section E) unless the caption says otherwise; labels to the right, refused when they would overlap. Where the true system is a few pixels across, an inset magnifies it - a **study magnification, not a field the atlas offers** - so the states can be judged; the page itself is unmagnified.

### The triple transit, 2026-12-11 22:45 UTC at Oslo, 36° field

![](field-36.png)

Jupiter's true disc is 0.3 px here; the minimum mark stands for it and the moons share its few pixels. Drawn: Jupiter at (450, 350), true 0.3 × 0.3 px, drawn 6.0 × 6.0 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The triple transit, 2026-12-11 22:45 UTC at Oslo, 8° field

![](field-8.png)

Jupiter's true disc is 1.3 px here; the minimum mark stands for it and the moons share its few pixels. Drawn: Jupiter at (450, 350), true 1.3 × 1.2 px, drawn 6.0 × 6.0 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The triple transit, 2026-12-11 22:45 UTC at Oslo, 3° field

![](field-3.png)

Jupiter's true disc is 3.4 px here; the moons are symbols; the inset shows the arrangement. Drawn: Jupiter at (450, 350), true 3.4 × 3.2 px, drawn 6.0 × 6.0 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The triple transit, 2026-12-11 22:45 UTC at Oslo, 1° field

![](field-1.png)

Jupiter's true disc is 10.1 px here; the moons are symbols; the inset shows the arrangement. Drawn: Jupiter at (450, 350), true 10.1 × 9.5 px, drawn 10.1 × 9.7 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The 11 December 2026 sequence at 22:30 UTC, Oslo, 1° field

![](triple-2230.png)

States: Io clear of jupiter; Europa in front of jupiter; Ganymede clear of jupiter; Callisto in front of jupiter; Drawn: Jupiter at (450, 350), true 10.1 × 9.5 px, drawn 10.1 × 9.7 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The 11 December 2026 sequence at 22:45 UTC, Oslo, 1° field

![](triple-2245.png)

States: Io in front of jupiter; Europa in front of jupiter; Ganymede clear of jupiter; Callisto in front of jupiter; Drawn: Jupiter at (450, 350), true 10.1 × 9.5 px, drawn 10.1 × 9.7 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The 11 December 2026 sequence at 22:55 UTC, Oslo, 1° field

![](triple-2255.png)

States: Io in front of jupiter; Europa in front of jupiter; Ganymede clear of jupiter; Callisto in front of jupiter; Drawn: Jupiter at (450, 350), true 10.1 × 9.5 px, drawn 10.1 × 9.7 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The 11 December 2026 sequence at 23:00 UTC, Oslo, 1° field

![](triple-2300.png)

States: Io in front of jupiter; Europa in front of jupiter; Ganymede clear of jupiter; Callisto clear of jupiter; Drawn: Jupiter at (450, 350), true 10.1 × 9.5 px, drawn 10.1 × 9.7 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### Io behind Jupiter (Horizons O at this hour), 2026-12-02 07:00 UTC, 1° field

![](behind-1.png)

States: Io behind jupiter; Europa clear of jupiter; Ganymede clear of jupiter; Callisto clear of jupiter; Drawn: Jupiter at (450, 350), true 9.8 × 9.2 px, drawn 9.8 × 9.4 px, pole 22.1° (J2000); Io behind, not drawn; Europa at (492, 333) clear; Ganymede at (410, 366) clear; Callisto at (501, 331) clear;

### Io wholly in Jupiter's shadow (Horizons u), 2026-12-02 04:00 UTC, 1° field

![](shadow-full-1.png)

States: Io in jupiters shadow; Europa clear of jupiter; Ganymede clear of jupiter; Callisto clear of jupiter; Drawn: Jupiter at (450, 350), true 9.8 × 9.2 px, drawn 9.8 × 9.4 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### Io partly in Jupiter's shadow (Horizons p), 2026-12-21 15:00 UTC, 1° field

![](shadow-partial-1.png)

States: Io partly in jupiters shadow; Europa clear of jupiter; Ganymede clear of jupiter; Callisto clear of jupiter; Drawn: Jupiter at (450, 350), true 10.4 × 9.7 px, drawn 10.4 × 9.9 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The state vocabularies, A above B

![](states-vocabulary.png)

One cell per state, each a real configuration at Oslo (Horizons' visibility code at that hour named), Jupiter's true oblate outline and pole and the moon at its true J2000 offset, at a study magnification of 2 px per arcsecond. A: behind not drawn; in front ringed in paper over the disc; in shadow a hollow ring. B: behind a dashed ghost; in shadow grey. Drawn: clear of Jupiter: Io, Horizons * at 2026-12-11 22:30 UTC, offset 19.9″ east, -7.2″ north; in front of Jupiter: Europa, Horizons t at 2026-12-11 22:45 UTC, offset 8.7″ east, -0.4″ north; behind Jupiter: Io, Horizons O at 2026-12-02 07:00 UTC, offset 13.0″ east, -6.0″ north; in Jupiter's shadow: Io, Horizons u at 2026-12-02 04:00 UTC, offset -33.8″ east, 13.0″ north; partly in shadow: Io, Horizons p at 2026-12-21 15:00 UTC, offset -36.4″ east, 13.9″ north;

### Jupiter just below the drawn horizon, 2026-12-01 12:20 UTC, Oslo, 8°

![](horizon-drawn-8.png)

Jupiter at -1.2° altitude, drawn in the ground's dimmed ink with its status, as the Sun's and the Moon's ruling (C4) has it. Drawn: Jupiter at (510, 468), true 1.2 × 1.1 px, drawn 6.0 × 6.0 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The same instant with the horizon hidden

![](horizon-hidden-8.png)

With no horizon drawn the celestial chart draws Jupiter normally (C4). Drawn: Jupiter at (510, 468), true 1.2 × 1.1 px, drawn 6.0 × 6.0 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The most crowded 2026 configuration with all four moons clear, 3° field

![](labels-crowded-3.png)

Moon labels refused on the page where they would overlap; the inset shows the moons. Drawn: Jupiter at (450, 350), true 2.7 × 2.5 px, drawn 6.0 × 6.0 px, pole 12.1° (J2000); moons not separable at this field, not drawn;

### Jupiter beside Regulus's label, 2027-01-15 22:00 UTC, 8° field

![](labels-regulus-8.png)

Jupiter 5.0° from Regulus: its label takes a free adjacent place or is refused; it never overwrites the star's. Drawn: Jupiter at (702, 225), true 1.4 × 1.3 px, drawn 6.0 × 6.0 px, pole 21.8° (J2000); moons not separable at this field, not drawn;

### The system near the page's left edge, 3° field

![](page-edge-3.png)

Bodies off the page are not drawn, as stars are not (C4). Drawn: Jupiter at (30, 348), true 3.4 × 3.2 px, drawn 6.0 × 6.0 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

### The A4 sheet's chart area at 150 dpi (1754 × 1090 px), 8° field

![](printable-a4-8.png)

Jupiter's true disc is 0.38 mm on the sheet: the minimum mark stands for it. Drawn: Jupiter at (877, 545), true 2.5 × 2.3 px, drawn 6.0 × 6.0 px, pole 22.1° (J2000); moons not separable at this field, not drawn;

## E. What the measurements say, and what is proposed for ruling

The issue's eight product questions, each with the measured reason and the proposal; nothing is built until the owner rules.

1. **One remembered switch, `Jupiter and moons on the chart`, off by default** - the Sun's and the Moon's model: one switch for the body, the Controller's box and View's item following it, remembered like theirs.
2. **Jupiter at ordinary fields: a cartographic mark plus its label.** Unlike the Sun and the Moon (C1: true scale, always), Jupiter's true disc is 0.95-1.56 px at 8°, below the atlas's smallest star mark (1.4 px) at every field above 8.9°: true scale alone would lose it. Proposed: a disc of 6 px or its true size, whichever is larger, in its own neutral ink, with its label - a symbol, stated as one in the legend and the inspector, never offered as its size.
3. **Jupiter when resolvable: the true oblate disc**, its minor axis along the pole derived in the chart's frame, no bands, Great Red Spot or rotation. The transition is continuous and measured: the mark is a circle at its 6 px minimum and becomes the true ellipse, axis ratio 0.935, as the true disc grows to twice that; at 1° the true disc is 7.6-12.5 px, so the outline is near a circle until a finer field (#481) or paper. The pole is drawn in no mark - only the outline shows it.
4. **Moons: symbols, never apparent diameters.** Their true discs are below a pixel on every page (Ganymede 0.46 px at 1°). Proposed: a 3 px dot, stated as a symbol; drawn only where the system is separable (section B) - at wider fields the moons are not drawn and Jupiter's mark alone stands for the system.
5. **Hidden, eclipsed and transiting moons** - two vocabularies are drawn (section D). **Proposed: candidate A** - clear: a filled dot; in front of Jupiter: a filled dot ringed in paper so it reads over the disc; behind Jupiter: not drawn, its label refused, the table saying where it is; in Jupiter's shadow (wholly or partly): a hollow ring, there but dark. B (behind as a dashed ghost, shadow as grey) is the alternative; it draws something the reader cannot see. No satellite shadow spots on Jupiter: the service does not compute them.
6. **Moon labels**: drawn only where section B gives them room (a label line between every pair), to the right of the mark, refused rather than overlapping; Jupiter's label as the Sun's and the Moon's (C4): adjacent candidates, refused rather than overwriting a star's.
7. **Horizon, page edge, stars, clicking, export** - as the Sun's and the Moon's ruling (C4, C6): below a drawn horizon, dimmed with its status; with the horizon hidden, drawn normally; off the page, nothing; the marks do not hide stars (Jupiter's mark is a symbol, not an opaque disc of its true size, until it is resolvable); clicking Jupiter may consume the hit but never exposes a hidden star; exported sheets draw what the page draws, at the sheet's scale.
8. **Centre on chart for Jupiter** - #483's accepted action, its seam prepared: compute the typed draft, enable the layer, centre on Jupiter's J2000 place, choose 1° (the chart's normal minimum; the target may ask for a finer field once #481 offers one), bring the chart forward, leave the Controller or dialog open. The button lands with the visible module (#484).

**Future close zoom (#481)**: the projection, the sizing rule and the labels above are field-independent - the mark's minimum, the transition and the separability thresholds are in pixels, the positions in J2000 - so a field below 1° needs no change to them. No sub-1° field is exposed or implemented here.
