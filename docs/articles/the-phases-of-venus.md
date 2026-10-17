# The phases of Venus

*What the atlas draws of Venus, why a planet needs a phase on the
chart, how to read the mark, and what the atlas does not claim*

## A planet that changes shape

Watch Venus through a small telescope over a few months and it will
not keep still in shape: a small round gibbous disc beside the Sun,
then a half, then a large and thinning crescent as it swings towards
us, then nothing as it passes between us and the Sun, then the same
sequence backwards in the morning sky. Galileo saw it in 1610, and it
was one of the observations that settled where the Sun is in the
Solar System: in Ptolemy's arrangement, with Venus always between us
and the Sun, it could never show a full phase. JUranometria 5.2 draws Venus with its phase - at its true place
for your spot on Earth and your instant, on the same page as the fixed
stars - and this article is about how that mark is to be read.

The atlas's own account of the decisions is in
`docs/decisions/venus-system.md` and `docs/decisions/venus-cartography.md`;
this article explains them, it does not replace them.

## Where the numbers come from

Nothing is fetched. Venus is computed from NASA/JPL's **DE440**
ephemeris, the same one the atlas's Sun and Moon already used, taken
from the released Solar System pack's own pinned source and cut into a
new bundled *planets* pack: one file for Venus, **1 242 112 bytes**,
covering **1900–2100**, every coefficient exactly as JPL published it.
A later planet adds its own file to the same pack; a released planet's
file is never rewritten.

Venus's size is its solid body: the IAU radius of **6051.8 km**, as
NAIF's planetary constants state it. You will not see that body.
What a telescope shows is the top of a permanent cloud deck, **74 km**
higher, which would draw Venus **1.22 %** wider - at most **0.771″**,
too small a difference to see on the page. The atlas states the difference in the table's explanation and never draws
the cloud radius: the solid body is the quantity the authorities agree
on.

## Held to an authority

The numbers are checked, not trusted. The atlas's Venus was compared
with **JPL Horizons** on **10 771** rows - five observers (Oslo,
Quito, Cape Town, Alert and the Chatham Islands across the date line)
and the geocentric case, at the nine named events and every week or
month across the interval. In the years where the clock correction is known
exactly, the place among the stars agrees to **0.001″** and the
distance to **0.2 km**. The apparent place of date agrees to
**0.129″**; that last figure is not error in Venus but the atlas's
older precession and nutation models, the same cause the Sun's
released contract measured.

After **2027-06-28** the difference between atomic time and the
Earth's rotation is a forecast, and the table marks those instants
*est.*: the cost there reaches **7.118″** by 2100, which is the clock,
not the planet.

The named events are Fred Espenak's almanac rows: the four
conjunctions fall **8–44** minutes into the hour the almanac lists,
and the five greatest elongations match the printed angle to the
tenth of a degree and the side of the Sun.

## The phase in words

The table describes the phase the way an observer would, in words
first and numbers after. When Venus's disc meets the Sun's the table
says so - *in front of the Sun* or *behind the Sun* - and otherwise it
uses five bands on the lit fraction: *near inferior conjunction* below
**2 %**, *crescent*, *near half lit*, *gibbous*, and *near full* from
**98 %**. Beside the band stands the lit side: the position angle of
the bright limb and the nearest of sixteen compass points. Near
inferior conjunction and near full the direction is not well defined,
and the table says *not well-defined* rather than inventing one.

Venus's apparent diameter changes more than any other planet's in
the atlas: from **9.61″** when it is farthest, on the far side of the
Sun, to **63.03″** when it is nearest. The large crescent and the
small full disc are the same planet at the two ends of its orbit
around the Sun.

## The named evening

The fixture the atlas measures itself against is the 2026 greatest
eastern elongation, which Espenak lists at **2026-08-15 06:00** UTC.
At Oslo the table says Venus is **48.7 %** lit, *near half lit*,
**45.9° E** of the Sun in the evening sky, its lit side at **295°**,
west-northwest - towards the Sun, as it must be. Its disc is
**24.3″** across and it is **0.686211** AU away, about **102.656**
million kilometres.

![The Venus table at 2026-08-15 06:00 UTC for Oslo: one row giving
right ascension 12h 27m 00.7s, declination −4° 26′ 50″, below the
horizon, distance 0.686211 AU, apparent diameter 24.3 arcseconds,
48.7 % illuminated, near half lit, 45.9° E, lit side 295°
west-northwest.](images/venus-table-en.png)

*The Venus table at the named instant: one row, every column the
service's, the time note below it.*

## From the numbers to the chart

The chart draws Venus where the numbers put it, through the switch
*Venus on the chart* in View ▸ Solar System and in the Controller's
Venus group - off until you choose it, then remembered. One rule
decides what you see, and it is the same rule that draws Jupiter.

**Venus is drawn at its true size where that can be seen, and as a
phased symbol where it cannot.** At the fields a reader usually works
at, Venus's few tens of arcseconds are smaller than a star's dot, so
the atlas draws a **6 px** cartographic symbol, says so in the spoken
description - *a cartographic symbol, not Venus's apparent diameter* -
and paints it beneath the stars, so that it never hides one. The
symbol is phased: the lit part of the mark is the lit part of the
planet, turned through the page's own north and east,
so the half that faces the Sun on the sky faces the Sun on the page.

![An 8-degree chart in Norwegian, centred near Venus on 15 August 2026
at 06:00 UTC: a small half-lit mark labelled Venus among scattered
stars, with the grid and the title block in
Norwegian.](images/venus-elongation-8-nb.png)

*The named instant at 8°, the page in Norwegian: the 6 px symbol,
half lit, its lit side to the west, towards the Sun.*

At the named instant the true disc at the 1° field is **6.1 px**, just
over the mark, and takes over:
opaque, the Moon's phased disc in miniature, no longer a symbol.

![A 1-degree chart centred on Venus on 15 August 2026 at 06:00 UTC: a
small half-lit disc labelled Venus near the middle of the page, two
faint stars, the grid labelled in minutes of
arc.](images/venus-elongation-1.png)

*The same instant at 1°: the true disc, just over the mark, half lit
and drawn as itself.*

On the black sky the same mark is drawn in the black sky's star ink,
its dark side a little above the sky's ground, so the phase still
reads.

![A 3-degree chart on the black sky at the named instant: the half-lit
Venus mark among white stars on black.](images/venus-elongation-3-dark.png)

*The named instant at 3° on the black sky.*

## Reading a conjunction

There is no special drawing for a conjunction; the order of painting
handles it. The nearer body is painted last.

**A transit.** On 2012-06-06 at **01:00** UTC Venus was crossing the
face of the Sun. At 1° the Sun is a ring around a ground-filled disc
and Venus's true disc, **14.5 px**, is painted over it - dark, because
its lit side faces away from us - and the page says *in front of the
Sun* beside the name.

![A 1-degree chart on 6 June 2012 at 01:00 UTC: a large thick ring,
the Sun, filling most of the lower page, with a small grey disc inside
it near the top, Venus, and the Sun's centre dot below
it.](images/venus-transit-2012-1.png)

*The 2012 transit at 1°: Venus's dark disc inside the Sun's ring.*

At wider fields Venus is a symbol again, and a symbol is normally
painted beneath the stars - where the Sun's disc would cover it. So a
symbol in front of an opaque disc is painted over that disc instead,
in painting order, and its name takes the first clean place beside it.

![An 8-degree chart on the black sky in Norwegian on 6 June 2012 at
01:00 UTC: the Sun as a white ring labelled Solen with Venus's small
mark inside it near the rim, labelled Venus
above.](images/venus-transit-2012-8.png)

*The same transit at 8°, on the black sky and in Norwegian: the mark
over the Sun's ring, its name above.*

**Behind the Sun** the same order covers Venus: the Sun's disc is
painted after it, and the page says *behind the Sun*.

**An inferior conjunction.** On **2026-10-24** at **03:00** UTC Venus
passes between us and the Sun. The table says
**0.6 %** lit, *near inferior conjunction*. The disc is the largest of
the apparition, **15.3 px** at 1°, and it is drawn dark with its
limb: the lit sliver is narrower than a tenth of a pixel, and the
atlas does not invent one to make the phase look nicer.

![A 1-degree chart on 24 October 2026 at 03:00 UTC: a dark grey disc
labelled Venus in the middle of a nearly empty
page.](images/venus-inferior-conjunction-2026-1.png)

*Inferior conjunction at 1°: the largest disc of the apparition, and
almost all of it unlit.*

## A whole apparition

The atlas holds the drawn phase to the table's. From the 2026 superior
conjunction on **2026-01-06**, every **14 days** through the inferior
conjunction and on into 2027, a 1° page is drawn by the production
chart and the mark cut out of it. Each mark's pixels are counted: how
much of it is drawn lit, and which way the lit side faces, read back
through the page's north and east. A 6 px mark holds **some two
dozen** pixels, so the share is coarse; what is held is that the drawn half is
the table's half and that the lit side faces **within 45°** of the
table's direction. It does for **18 of the 18** steps where the table
gives a direction.

![Three rows of small marks cut from 1-degree pages every 14 days
through 2026 and 2027: small full discs at first, then half-lit
marks, then larger crescents growing dark, then thin crescents
reopening on the other side.](images/venus-apparition-2026-27.png)

*The 2026–27 apparition, one mark every 14 days, left to right. The
names are cut off with the tiles; the pages they come from are in the
evidence.*

## What the atlas does not claim

- **No clouds and no surface.** The disc is the solid body, drawn as a
  figure, not a picture; the cloud tops are stated, never drawn.
- **The symbol is not the size.** Where Venus is the 6 px mark, the
  mark says where Venus is and which side is lit; it says nothing
  about how big Venus looks, and the spoken description says so.
- **No invented sliver.** Near inferior conjunction and near full the
  drawing is what the geometry gives, even when that is a dark disc or
  a plain one.
- **No events.** The atlas samples instants; it does not search for
  transits or elongations or predict their times. The named instants
  are fixtures for measurement, not a catalogue.
- **No atmosphere on Earth either.** Altitudes are airless; refraction
  near the horizon is not applied.
- **Two planets.** Venus and Jupiter are the planets in release 5.2.

*Technical companions: `docs/decisions/venus-system.md` (the numbers
and their contract), `docs/decisions/venus-cartography.md` (the mark
and its rulings), `docs/decisions/venus-handover.md` (the settled
contract in one place), `docs/studies/venus/table-measurements.md`,
`docs/studies/venus/measurements.md` and
`docs/studies/venus-on-the-chart/measurements.md` (the measurements
this article quotes).*
