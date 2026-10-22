# Mercury, never far from the Sun

*What the atlas draws of Mercury, why the drawing so often has to be a
symbol, what the closer fields add, and what the atlas does not claim*

## The planet you have to catch

Mercury is the planet most people have never knowingly seen. It stays
close to the Sun - in 2026 its greatest elongations run from **18.1°**
to **27.8°** - so it is only ever low in the dusk or the dawn, and it
hurries: a whole swing from one side of the Sun to the other takes
weeks, not months. Through a telescope it shows phases like Venus's,
on a disc a fifth of the size. JUranometria 5.3 draws Mercury with its
phase at its true place for your spot on Earth and your instant, on
the same page as the fixed stars; it adds a Mercury table beside the
Venus table; and it opens two fields narrower than 1°, the *closer
fields*, at which Mercury's disc can at last be seen on the screen.
This article is about how those marks and pages are to be read.

The atlas's own account of the decisions is in
`docs/decisions/mercury-system.md` and
`docs/decisions/mercury-cartography.md`; this article explains them, it
does not replace them.

## Where the numbers come from

Nothing is fetched. Mercury is computed from NASA/JPL's **DE440**
ephemeris, the same source the atlas's Sun, Moon, Venus and Jupiter
use, cut into its own file in the bundled planets pack:
**3 299 328 bytes**, covering **1900–2100**, every coefficient as JPL
published it. It sits beside Venus's file, which is byte-identical to
the one 5.2 shipped: a released planet's file is never rewritten.

Mercury's size is its solid body: the IAU figure is triaxial,
**2440.53 km** along two axes and **2438.26 km** along the third, and
the atlas draws the **equatorial 2440.53 km**, never a mean radius.
There is no cloud deck to argue about; what a telescope shows of
Mercury is rock, and the atlas draws that rock as a figure, not a
picture.

## Held to an authority

The numbers are checked, not trusted. The atlas's Mercury was compared
with **JPL Horizons** on **11 207** rows - five observers (Oslo, Quito,
Cape Town, Alert and the Chatham Islands across the date line) and the
geocentric case, at the named events and every week across the
interval. In the years where the clock correction is known exactly the
place among the stars agrees to **0.000″** and the distance to
**0.2 km**; the apparent place of date agrees to **0.129″**, which is
the atlas's older precession and nutation models, not the planet. The
side of the Sun agrees on every row, and the parallax applied - the
shift between the Earth's centre and your spot on it - reaches
**15.88″**.

After **2027-06-28** the difference between atomic time and the
Earth's rotation is a forecast, and the table marks those instants
*est.*: Mercury moves fast, so the clock's uncertainty costs more here
than for Venus - up to **11.858″** by 2100 - and that is the clock,
not the planet.

The named events are Fred Espenak's almanac rows and the NASA/GSFC
transit catalogue: conjunctions fall **−16 to +37** minutes from the
listed hour, greatest elongations match the printed angle to the
tenth of a degree, and the greatest-transit instants of all **28**
transits of 1900–2100 fall **−0.5 to +0.7** minutes from the
catalogue's minute, the separation of the two centres agreeing to
**0.1″**.

One of those transits is honest about a boundary. On **1937-05-11**
Mercury grazed the Sun's limb; the catalogue counts it as a transit,
but by the atlas's own radii the centre of Mercury is **+0.017″**
outside the limb at greatest transit, so the atlas says *near inferior
conjunction* rather than *in front of the Sun*. The margin is recorded
by name, and any second such case stops the build.

## The phase in words

The Mercury table describes the phase the way the Venus table does,
word for word. When Mercury's disc meets the Sun's the table says so -
*in front of the Sun* or *behind the Sun* - and otherwise it uses the
five bands on the lit fraction: *near inferior conjunction* below
**2 %**, *crescent*, *near half lit*, *gibbous*, and *near full* from
**98 %**. Beside the band stands the lit side, the position angle of
the bright limb and the nearest of sixteen compass points, and near
inferior conjunction and near full the table says *not well-defined*
rather than inventing a direction.

Mercury's apparent diameter runs from **4.64″** when it is farthest,
beyond the Sun, to **12.26″** when it is nearest - a fifth of Venus's
range, and small enough that it matters for everything that follows.

## The named evening

The fixture the atlas measures itself against is the 2026 greatest
eastern elongation, which Espenak lists at **2026-02-19 18:00** UTC.
At Oslo the table says Mercury is **49.7 %** lit, *near half lit*,
**18.1°** east of the Sun in the evening sky, its lit side at
**243°**, west-southwest - towards the Sun, as it must be. Its disc is
**7.2″** across, it is **0.937962** AU away, about **140.317** million
kilometres, and it stands **1.80°** above the airless horizon: Mercury
at its best in a Norwegian February is a planet in the last of the
daylight, a hand's breadth above the ground.

![The Mercury table at 2026-02-19 18:00 UTC for Oslo: one row giving
right ascension 23h 17m 02.0s, declination −3° 20′ 04″, altitude
1.80°, distance 0.937962 AU, apparent diameter 7.2 arcseconds, 49.7 %
illuminated, near half lit, 18.1° E, lit side 243°
west-southwest.](images/mercury-table-en.png)

*The Mercury table at the named instant: one row, every column the
service's, the time note below it.*

## From the numbers to the chart

The chart draws Mercury where the numbers put it, through the switch
*Mercury on the chart* in View ▸ Solar System and in the Controller's
Mercury group - off until you choose it, then remembered. The rule is
Venus's, unchanged: **Mercury is drawn at its true size where that can
be seen, and as a phased symbol where it cannot.**

For Mercury that second clause is nearly the whole story. On the
atlas's ordinary pages, at every field down to 1°, Mercury's disc is
between **1.16–3.06 px** across - smaller than the smallest star's
dot - so the atlas draws the **6 px** cartographic symbol, says so in
the spoken description, *a cartographic symbol, not Mercury's apparent
diameter*, and paints it beneath the stars, so that it never hides
one. The symbol is phased: its lit part is the planet's lit part,
turned through the page's own north and east, so the half that faces
the Sun on the sky faces the Sun on the page.

![An 8-degree chart in Norwegian centred near Mercury on 19 February
2026 at 18:00 UTC: a small half-lit mark labelled Merkur among a few
stars, the grid and the title block in
Norwegian.](images/mercury-elongation-8-nb.png)

*The named instant at 8°, the page in Norwegian: the 6 px symbol,
half lit, its lit side to the west, towards the Sun.*

At 1°, where Venus's disc at the same kind of instant just takes over
from the mark, Mercury's is still about **1.8 px**: the mark stands
in, and the phase you see is in the mark's lit region, never in a
size the disc does not have.

![A 1-degree chart centred on Mercury on 19 February 2026 at 18:00
UTC: a small half-lit mark labelled Mercury near the middle of an
almost empty page, one star at the lower right, the grid labelled in
minutes of arc.](images/mercury-elongation-1.png)

*The same instant at 1°: still the symbol - the true disc is under
two pixels here.*

On the black sky the same mark is drawn in the black sky's star ink,
its dark side a little above the sky's ground, so the phase still
reads.

![A 3-degree chart on the black sky at the named instant: the half-lit
Mercury mark among white stars on black.](images/mercury-elongation-3-dark.png)

*The named instant at 3° on the black sky.*

## The closer fields

Measured over every week of 1900–2100, Mercury's phase reads on the
screen in **no week at any field** of the atlas's ordinary ladder; on
an A4 sheet at 300 dpi it reads in **30.3 %** of the weeks at 1°. The
true disc reaches the 6 px mark only at **0.51°** when Mercury is
nearest and at **0.19°** when it is farthest. So 5.3 opens two fields
below the 1° floor, **0.5°** and **0.25°**, and calls them *closer
fields*. Zoom in offers the next of them - from the toolbar, the
Controller, the View menu, the shortcuts or the wheel - when Mercury,
Venus or Jupiter, shown on the chart, is on the page that step would
show; the page says it is a closer field; panning keeps the field;
zoom out returns to the ordinary ladder; Home is unchanged; and a
closer field is never saved.

At the named instant the 0.5° page still draws the symbol.

![A 0.5-degree chart centred on Mercury on 19 February 2026 at 18:00
UTC: the half-lit mark labelled Mercury alone on a page whose title
block reads Closer field
0.5°.](images/mercury-closer-0.5.png)

*The named instant at 0.5°: the symbol still, the title block saying
*Closer field*.*

At 0.25° the true disc is **7.2 px**, over the mark, and takes over:
opaque, the Moon's phased disc in miniature, drawn as itself.

![A 0.25-degree chart centred on Mercury on 19 February 2026 at 18:00
UTC: a small half-lit disc labelled Mercury alone on the page, the
grid labelled every two minutes of
arc.](images/mercury-closer-0.25.png)

*The named instant at 0.25°: the true disc, half lit, its lit side
towards the Sun.*

That is the whole reason the closer fields exist: not to magnify the
stars, which stay the same catalogue to the same limit, but to let a
planet whose disc is a few arcseconds across be drawn as a disc at all.

## Reading a conjunction

There is no special drawing for a conjunction; the order of painting
handles it. The nearer body is painted last.

**A transit.** On **2019-11-11** at **15:20** UTC Mercury was crossing
the face of the Sun - the greatest transit, to the catalogue's minute.
At 1° the Sun is a ring around a ground-filled disc **484.4 px**
across; Mercury is a symbol in front of an opaque disc, so instead of
being laid beneath the stars it is painted over the Sun, its dark side
reading on the Sun's ground beside the Sun's small centre dot, and the
page says *in front of the Sun* beside the name.

![A 1-degree chart on 11 November 2019 at 15:20 UTC: a large thick
ring, the Sun, filling most of the page, with Mercury's small dark mark
near the middle and the Sun's centre dot just below
it.](images/mercury-transit-2019-1.png)

*The 2019 transit at 1°: the symbol over the Sun's disc, the name
refused inside the Sun's ink.*

![A 6-degree chart in Norwegian on 11 November 2019 at 15:20 UTC: the
Sun as a ring labelled Solen with Mercury's small mark inside it near
the centre.](images/mercury-transit-2019-6-nb.png)

*The same transit at 6°, in Norwegian: the mark over the Sun's ring.*

At 0.25° the Sun's disc is **1937.8 px** across - the whole page lies
inside it, its ring off the page - and Mercury's true disc, **10.0 px**,
is drawn over the Sun's ground beside the centre dot, dark, because
its lit side faces away from us.

![A 0.25-degree chart on 11 November 2019 at 15:20 UTC: a plain white
page with one dark grey disc, Mercury, near the middle and a smaller
black dot, the Sun's centre, below and to the right of
it.](images/mercury-transit-2019-0.25.png)

*The 2019 transit at 0.25°: Mercury's true disc on the Sun's ground.
Nothing on the page itself says this is the Sun; the table does.*

The next transit the atlas's interval holds is **2032-11-13 08:54**
UTC, marked *est.* because it lies beyond the exactly known clock.

**Behind the Sun** the same order covers Mercury: on **2026-05-14** at
**14:00** UTC the mark is laid beneath the stars and the Sun's disc is
painted after it, and the page says *behind the Sun*.

**An inferior conjunction.** On **2026-03-07** at **11:00** UTC
Mercury passes between us and the Sun, **3.6°** to its west. The table
says **0.7 %** lit, *near inferior conjunction*. The mark is drawn dark
with its limb: the lit sliver is under a pixel, and the atlas does not
invent one to make the phase look nicer.

![A 1-degree chart on 7 March 2026 at 11:00 UTC: a small dark mark
labelled Mercury in the middle of a nearly empty
page.](images/mercury-inferior-conjunction-2026-1.png)

*Inferior conjunction at 1°: the dark mark with its limb.*

## A whole apparition

The atlas holds the drawn phase to the table's. From the 2026 superior
conjunction on **2026-01-21**, every **3 days** through the inferior
conjunction and **50 days** beyond, a 1° page is drawn by the
production chart and the mark cut out of it. Each mark's pixels are
counted: how much of it is drawn lit, and which way the lit side
faces, read back through the page's north and east. A 6 px mark holds
**some two dozen** pixels, so the share is coarse; what is held is
that a crescent is drawn less than half lit and a gibbous mark more,
and that the lit side faces **within 45°** of the table's direction.
It does for **19 of the 19** steps where the table gives a direction.

![Three rows of small marks cut from 1-degree pages every 3 days
through the first half of 2026, each with the letter M of its name
beside it: nearly full marks first, then gibbous, then crescents
growing dark, then crescents reopening on the other side and filling
again.](images/mercury-apparition-2026.png)

*The 2026 apparition, one mark every 3 days, left to right. The names
are cut off with the tiles; the pages they come from are in the
evidence.*

## What the atlas does not claim

- **No surface.** The disc is the solid body, drawn as a figure, not a
  picture: no markings, no albedo, no colour.
- **The symbol is not the size.** On every ordinary page Mercury is the
  6 px mark, which says where Mercury is and which side is lit and
  nothing about how big Mercury looks; the spoken description says so.
- **Seeing it on the page is not seeing it in the sky.** Mercury's
  altitude at the named instant is stated airless; refraction near the
  horizon is not applied, and the atlas says nothing about whether
  Mercury is visible to the eye.
- **No invented sliver.** Near inferior conjunction and near full the
  drawing is what the geometry gives.
- **No events.** The atlas samples instants; it does not search for
  transits or elongations or predict their times. The named instants
  are fixtures for measurement, not a catalogue.
- **Three planets.** Mercury, Venus and Jupiter are the planets in
  release 5.3.

*Technical companions: `docs/decisions/mercury-system.md` (the numbers
and their contract), `docs/decisions/mercury-cartography.md` (the mark,
the closer fields and their rulings), `docs/decisions/mercury-handover.md`
(the settled numerical contract in one place),
`docs/studies/mercury/table-measurements.md`,
`docs/studies/mercury-cartography/measurements.md`,
`docs/studies/mercury-on-the-chart/measurements.md` and
`docs/studies/closer-fields/measurements.md` (the measurements this
article quotes).*
