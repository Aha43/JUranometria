# Saturn's rings on the page

*What the atlas draws of Saturn, its rings and seven of its moons, how
the rings turn over the years, how to read the figure and the marks, and
what the atlas does not claim*

## A planet that changes its shape

Every other planet in the atlas is a disc, lit more or less. Saturn is a
figure: a flattened globe inside two flat rings that are far wider than
the planet itself, and the rings do not hold still. Over Saturn's long
year the Earth sees them from above and from below in turn - wide open
to one face, then closing to a line, then opening to the other.
JUranometria 5.4 computes Saturn and seven of its moons for your spot on
Earth and your instant, gives them a Saturn table beside Jupiter's, and
draws the figure and the moons at their true places on the same page as
the fixed stars. This article is about how that figure and those marks
are to be read.

The atlas's own account of the decisions is in
`docs/decisions/saturn-system.md` and
`docs/decisions/saturn-cartography.md`; this article explains them, it
does not replace them.

## Where the numbers come from

Nothing is fetched. Saturn's centre and the moons come from NAIF's
**sat441** ephemeris, JPL's Saturn satellite solution, cut into a Saturn
pack beside the atlas's other packs: Saturn itself in **16,516,096
bytes** covering **1900-2100**, the moons in **62,829,568 bytes**
covering **2000-2100**, **79,345,664 bytes** in all. No released file
changed for it: the Sun's, the Moon's, Mercury's, Venus's and Jupiter's
files are byte for byte the ones earlier releases shipped.

Saturn's size is the IAU's: radii of **60268, 60268 and 54364 km** - the
two equatorial axes equal, the polar one a tenth shorter - and the table
gives its apparent width both across the equator and from pole to pole,
as you see it at that instant. The rings are not a picture either. They
are two flat annuli with edges read from the PDS Ring-Moon Systems
Node's *Rings of Saturn* table: from **74,491 km** to **117,570 km**,
the C Ring's inner edge to the B Ring's outer edge, and from **122,050
km** to **136,770 km**, the outer edge of the Cassini Division to the
outer edge of the A Ring. Between them lies the Cassini Division itself.
Nothing else of the ring system is drawn.

The seven moons are the ruled ones: **Mimas, Enceladus, Tethys, Dione,
Rhea, Titan and Iapetus**. Hyperion, the faintest and the most
irregular, is left out of this release.

## Held to an authority

The numbers are checked, not trusted. The atlas's Saturn was compared
with **JPL Horizons** on its committed responses - **74 Horizons
responses** - across five observers and the geocentric case, the named
events and the weeks of the whole interval. Where the clock correction
is known exactly, from 1972 until **2027-06-28**, Saturn's place among
the stars agrees to **0.0000″** and its distance to **0.22 km**; the
moons' offsets from Saturn agree to **0.0006″**, and their states -
clear, in front, behind, in the shadow - on **1,819 of 1,819** rows for
each moon. After **2027-06-28** the difference between atomic time and
the Earth's rotation is a forecast, and Saturn's place may differ by up
to **0.6840″** by 2100: that is the clock, not the planet.

Saturn's crossings of its own ring plane are where the honesty of the
numbers matters most, because they are where the rings vanish. The Earth
crossed the ring plane on **2025-03-23 17:51** UTC and the Sun on
**2025-05-06 15:35**, seen from Oslo, both agreeing with Horizons to the
minute; all **44** crossings of the interval, **30 Earth, 14 Sun**,
agree within a minute through the exactly known years, drifting to **2
minutes by 2098**. But the atlas says plainly what that agreement is
worth: Horizons answers from the same sat441 family, and no independent
published authority checks the crossings. They are validated against
Horizons only.

## The opening of the rings

The table's most important new word is the ring opening, **B**: how far
the rings are tilted open towards you, in degrees, and which face you
see. It is the true angle between your line of sight and the ring plane,
*planetocentric*, measured over Saturn's pole. Horizons prints a
different latitude, the planetodetic one, a latitude on Saturn's
flattened globe; the two differ by up to **18,196″**, five degrees, and
the atlas never shows that number as the opening. The widest opening of
the interval is **−26.995°** on **2003-04-07**, where Horizons'
planetodetic figure would say **−32.049°**.

![A plot of the ring opening to the Earth at Oslo, weekly from 1900 to
2100: a solid curve swinging between about plus and minus 27 degrees
seven times, a dashed curve swinging further, to about 32 degrees, and
red dots on the zero line where the curve
crosses.](images/saturn-ring-opening-1900-2100.png)

*The ring opening to the Earth, every week of 1900-2100: the atlas's
planetocentric B (solid) and Horizons' planetodetic latitude (dashed).
Each red dot is a week in which the opening changed sign - a crossing of
the ring plane by the Earth.*

There is a second opening, **B′**, to the Sun: which face the Sun
lights. The atlas takes it where Saturn sees the Sun, the light delayed
and aberrated by Saturn's own motion; without that correction the Sun's
crossings would fall **1.31-1.33 h** early. When B and B′ have opposite
signs you are looking at the face the Sun is not lighting - *the unlit
face* - and the table says so. In the weekly Oslo rows of 1900-2100 that
happens in **248 of the 10,488**.

## The named instant

The fixture the atlas measures itself against is the 2026 row nearest
the Earth, **2026-10-05 00:00** UTC, at Oslo. Saturn stands at **00h 44m
16.0s**, **+1° 48′ 29″**, **31.18°** above the airless horizon,
**8.434258 AU** away. The globe is **19.7″ / 17.8″** across, equator and
poles, **100.0 %** lit as an outer planet nearly always is, its north
pole at **3.2°**. The rings open **7.43° (south face)** to us and
**7.71° (south face)** to the Sun - the face we see is the lit one - and
their outer edge is **44.7″ × 5.8″** on the sky.

![The Saturn card at 2026-10-05 00:00 UTC for Oslo: right ascension 00h
44m 16.0s, declination +01° 48′ 29″, altitude 31.2°, azimuth 195.3°,
distance 8.434 AU, apparent diameter 19.7″ by 17.8″, 100.0 %
illuminated, north pole 3.2°, rings opening 7.43° south face to the
observer and 7.71° south face to the Sun, rings 44.7″ by
5.8″.](images/saturn-card-en.png)

*The Saturn card at the named instant: every line the service's.*

## From the numbers to the chart

The chart draws Saturn where the numbers put it, through the switch
*Saturn on the chart* in View ▸ Solar System, after Jupiter's, and in
the Controller's Saturn group - off until you choose it, then
remembered. The rule is Jupiter's, applied to the whole figure: **Saturn
is drawn as its true globe and rings where the rings are at least 6 px
across, and as a 6 px cartographic symbol where they are not.** It is a
switch, not a blend: a circle cannot grow into a ringed figure.

Across the century the rings' outer edge spans **34.13″** to **46.97″**
and the globe **15.04″** to **20.70″**, so on the atlas's 900-pixel
pages the true figure takes over only at the narrowest fields. At the
named instant every field from 36° down to 2° draws the 6 px symbol: it
says where Saturn is and nothing about its size, it is painted beneath
the stars so that it never hides one, and the spoken description says *a
cartographic symbol, not Saturn's apparent diameter*.

![An 8-degree chart centred on Saturn on 5 October 2026 at 00:00 UTC: a
small round mark labelled Saturn near the middle, a small dot labelled
Iapetus just to its right, stars as dots and galaxies as small grey
ellipses scattered over the page, the grid.](images/saturn-card-8.png)

*The named instant at 8°: the 6 px symbol, and Iapetus, far enough out
to stand clear of it.*

At 1° the true figure takes over, **12.2 × 4.5** px of ink: still small,
but a ringed figure, opaque, and spoken simply as *Saturn*.

![A 1-degree chart on 5 October 2026 at 00:00 UTC: a tiny flattened
ringed figure labelled Saturn with a cluster of small dots and names
around it - Enceladus, Dione, Titan, Rhea, Mimas - a larger grey ellipse
above and to the right, and Iapetus at the lower
right.](images/saturn-card-1.png)

*The named instant at 1°: the true figure, the moons around it. The grey
ellipse is a galaxy from the catalogue, as on the 8° page, not a body.*

## The closer fields

Saturn is one of the bodies for which zoom in offers the *closer
fields*, **0.5°** and **0.25°**, below the ordinary 1° floor. They do
not magnify the stars; they exist so that a figure a few tens of
arcseconds across can be drawn as itself. At the named instant the
figure is **22.3 × 8.9** px at 0.5° and **44.6 × 17.8** px at 0.25°, and
there the rings read as rings: the half nearer to us drawn over the
globe, the far half behind it, the Cassini Division cut between them
only where it is at least a pixel across.

![A 0.25-degree chart on the black sky at 5 October 2026: Saturn's globe
inside its narrowly opened rings, white on black, labelled Saturn, with
small white dots and names - Mimas, Enceladus, Dione, Rhea, Titan -
around it.](images/saturn-card-0.25-dark.png)

*The named instant at 0.25° on the black sky. Tethys is behind Saturn
and is not drawn; Iapetus is off the page.*

## The rings over the years

The same rules draw the rings at every opening. Wide open to the north
face, on **2076-10-10 00:00** UTC, B is **+26.97°**; wide open to the
south face, on **2003-04-05 00:00**, it is **−26.99°**. Which half of
the rings lies in front of the globe changes with the face: the near
half lies away from the projected north pole when the north face is seen
and towards it when the south face is.

![A 0.25-degree chart on 10 October 2076: Saturn's globe inside widely
opened rings, labelled Saturn, the seven moons as small dots with their
names around it.](images/saturn-open-north-0.25.png)

*The rings widest to the north face, 2076, at 0.25°.*

![A 0.25-degree chart on the black sky on 5 April 2003, in Norwegian:
the globe inside widely opened rings, white on black, labelled Saturn,
with small dots and names around
it.](images/saturn-open-south-0.25-dark.png)

*The rings widest to the south face, 2003, at 0.25° on the black sky,
the page in Norwegian.*

**Edge-on.** When the ring plane passes through the Earth the rings are
only a thin line: on the atlas's pages, a ring minor axis under 2 px is
drawn as a **1 px** line through the globe, which at 1° is the case in
**30.4 %** of the century's weeks. At the Earth's crossing of
**2025-03-23 17:51** UTC the opening is **+0.00°** and the table says
*edge-on*.

![A 0.25-degree chart in Norwegian on 23 March 2025 at 17:51 UTC: a
small circle, the globe, with a thin straight line through it, the rings
seen edge-on, labelled Saturn, the moons as dots close along the
line.](images/saturn-edge-on-0.25.png)

*The Earth's ring-plane crossing, 2025, at 0.25°: the rings a line.*

**The unlit face.** Between the Earth's crossing and the Sun's, six
weeks later, the Earth and the Sun stood on opposite sides of the ring
plane. On **2025-05-06 00:00** UTC, hours before the Sun's crossing, the
opening to us is **−2.19°**, the south face, and to the Sun **+0.01°**,
the north: we see the face the Sun is not lighting. The page draws those
rings in a dimmed ink, so the page says what the table says.

![A 0.25-degree chart on 6 May 2025: a small globe with a faint, nearly
flat ring line through it, labelled Saturn, the moons as dots close
by.](images/saturn-unlit-0.25.png)

*The unlit face seen, 2025, at 0.25°: the rings dimmed.*

## The moons

Each of the seven moons is a **3 px** symbol at its own place among the
stars, and each is decided on its own, so one crowded pair never
suppresses the rest. A moon behind Saturn is not drawn. At 1° and the
closer fields every other moon is drawn at its exact place, its mark
overlapping the figure's or another's where the real configuration is
that close; at wider fields a moon is drawn only where its mark stands
clear of the figure and of the moons already drawn. At the named instant
that is **0 of 7** moons at 36°, **1 of 7** at 8° - Iapetus - and **6 of
7** at 1°, Tethys being behind Saturn. Every moon is far smaller than a
pixel on every field - Titan, the largest, is **0.8849″** at most - so
its mark is always a symbol, and the spoken description says so.

The states read as they do for Jupiter's moons: clear of Saturn, a
filled dot; in front of Saturn, a dot ringed in the page's ground,
painted over the figure - at wider fields than 1° only where the globe
itself is at least **12 px** wide; in Saturn's shadow, wholly or partly,
a hollow ring; behind, not drawn. The inner moons live close: Mimas is
never farther than **32.2″** from Saturn's centre, where the rings reach
out to **23.5″**.

![A 0.25-degree chart in Norwegian on 1 July 2026 at 03:00 UTC: Saturn's
globe and rings with a small ringed dot on the globe, Mimas in front of
it, and the other moons as dots with their names
around.](images/saturn-in-front-0.25.png)

*Mimas in front of Saturn, **2026-07-01 03:00** UTC, at 0.25°; the page
says *foran Saturn*.*

![A 0.25-degree chart on the black sky on 1 July 2026 at 14:00 UTC:
Saturn's globe and rings, white on black, with a hollow ring just below
the figure, Enceladus in Saturn's shadow, and other moons as
dots.](images/saturn-in-shadow-0.25-dark.png)

*Enceladus in Saturn's shadow, **2026-07-01 14:00** UTC, at 0.25° on the
black sky: a hollow ring, spoken *in Saturn's shadow*.*

Every drawn moon's name is tried in the same boxes around its mark that
every body's name uses, and refused on its own where none is clear, so
near the figure some names are often missing: the table gives them.

## What the atlas does not claim

- **No surface.** The globe and the rings are a figure, not a picture:
  no bands, no colour, no texture, no ring shading beyond the two flat
  tones the figure is drawn in.
- **No shadows.** The globe's shadow on the rings, the rings' shadow
  on the globe and a moon's shadow on Saturn are geometry this release
  does not compute, and none is drawn.
- **No ring-obscuration state.** A moon whose sight line crosses the
  rings is drawn by the order of painting only; neither Horizons nor
  any other authority the atlas uses accounts for the rings, so the
  atlas never says a moon is hidden by them.
- **The symbol is not the size.** On most pages Saturn is the 6 px
  mark, which says where Saturn is and nothing about how big it looks;
  the moons are always symbols.
- **Seeing it on the page is not seeing it in the sky.** Altitudes are
  airless, and the atlas says nothing about whether a ring or a moon is
  visible to you; no magnitude is printed for any body.
- **No events.** The atlas samples instants; it does not search for
  crossings, elongations or transits of the moons, or predict their
  times. The named instants are fixtures for measurement, not a
  catalogue.
- **Four planets.** Mercury, Venus, Jupiter and Saturn are the planets
  in release 5.4.

*Technical companions: `docs/decisions/saturn-system.md` (the numbers
and their contract), `docs/decisions/saturn-cartography.md` (the figure,
the rings, the moons and their rulings),
`docs/decisions/saturn-handover.md` (the settled numerical contract in
one place), `docs/studies/saturn-system/table-measurements.md` and
`docs/studies/saturn-on-the-chart/measurements.md` (the measurements
this article quotes).*