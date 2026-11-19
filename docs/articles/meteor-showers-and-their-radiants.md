# Meteor showers and their radiants

*What a radiant is and why it moves, why the atlas needs a chart time to
place one, why its height, the Moon and the dark decide a shower night,
what a quoted rate and a nominal maximum do and do not say, how to use the
panel and the chart, and what the atlas does not claim*

## A point the paths point back to

A meteor is the streak of light seen when a grain of dust, most often
no bigger than a grain of sand, burns up as it meets the air. The grain
itself is a meteoroid; the meteor is what we see of its end. A meteor
shower is what happens when the Earth passes through a stream of such
grains, all travelling along nearly the same orbit round the Sun.
Because they share an orbit, they meet the Earth travelling side by
side, in parallel.

Parallel lines seen from the inside do not look parallel. Railway tracks
appear to meet at a point on the horizon; snowflakes in the headlights
appear to burst from a point ahead of the car. The meteors of one shower
do the same. Each streak can appear anywhere in the sky, but draw every
one of them backwards and the lines cross in one small area. That area is
the **radiant**, and the shower is named after the constellation it lies
in: the Perseids after Perseus, the Geminids after Gemini.

The radiant is therefore a direction, not a place where something
happens. It is the direction the stream comes from, as seen from a moving
Earth. Nothing is to be seen at the radiant itself; a meteor that appears
there is coming straight at you, and is a short streak or a point.

JUranometria 6.6, the release that follows 6.5, can draw that direction
on the chart and say what the atlas knows about it at the chart's time
and place. This article is about how to read both. It describes the
atlas as it is built for 6.6; it is not that release's notes.

The atlas's own account of the decisions is in
`docs/decisions/meteor-showers.md`; this article explains it, it does
not replace it.

## Nine showers, one at a time

The atlas carries nine annual showers: those that stand both in the
International Meteor Organization's working list and in the IAU Meteor
Data Center's list of established showers, are seen at night, and are
listed with a rate of ten or more.

| shower | active, as the calendar dates it | solar longitude of the maximum | listed rate (ZHR) |
|---|---|---|---|
| Quadrantids | Dec 28 - Jan 12 | 283.15 | 80+ |
| Lyrids | Apr 14 - Apr 30 | 32.32 | 18 |
| Eta Aquariids | Apr 19 - May 28 | 45.5 | 50 |
| Southern Delta Aquariids | Jul 12 - Aug 23 | 128 | 25 |
| Perseids | Jul 17 - Aug 24 | 140.0 | 110+ |
| Orionids | Oct 02 - Nov 07 | 208 | 20 |
| Leonids | Nov 06 - Nov 30 | 235.27 | 15+ |
| Geminids | Dec 04 - Dec 20 | 262.2 | 150 |
| Ursids | Dec 17 - Dec 26 | 270.7 | 10 |

The values are those of one named edition, the IMO Meteor Shower
Calendar 2027, and every view of them in the atlas says so. Source:
International Meteor Organization (IMO) – www.imo.net. Positions between
the printed rows, instants and everything said for a place are
JUranometria's own calculations, and JUranometria is not affiliated with
or endorsed by the IMO.

The showers live in one panel, *Meteor showers*, opened from the Solar
System menu and shown in the Controller's Solar System tab. Its selector
lists the nine in the order of the year, and marks those active at the
chart time. You choose one, or none. The chosen shower's radiant is then
drawn on the chart, as a small mark of six short rays about an empty
centre, with the shower's name beside it.

[![An ordinary chart of Perseus, Cassiopeia and Camelopardalis on white
paper, with a small mark of six short rays at the centre, north of the
figure of Perseus, labelled
Perseids.](images/meteor-perseids-night-en-white.png)](images/meteor-perseids-night-en-white.png)

*The Perseid radiant from Oslo (**59.91 N, 10.75 E**) on **2027-08-13**
at **00:00** UTC. The mark is a direction, drawn at a fixed size; it
carries no rate and no strength. Every figure in this article opens at
its full size when you select it.*

[![The same page on a black ground with its words in Norwegian: the
six-rayed mark at the centre, labelled
Perseidene.](images/meteor-perseids-night-nb-black.png)](images/meteor-perseids-night-nb-black.png)

*The same page on the black sky, in Norwegian. The geometry is
identical; only the ground and the words change.*

Only one shower is drawn at a time, and only while it is active. On a
December evening the Perseids can stay selected, but the panel says *No
radiant is drawn or described*, and the chart shows none.

## Why the chart needs a time

A shower returns every year because the Earth returns to the place where
its orbit crosses the stream. That place is a position on the Earth's
orbit, and the calendar is only an approximate way of naming it: a year
is about a quarter of a day longer than 365 days, and the leap day puts
the difference back every fourth year. Meteor astronomers therefore use
the **solar longitude**, the Sun's ecliptic longitude as seen from the
Earth, which says exactly where on its orbit the Earth is.

The atlas asks every question about a shower of the solar longitude at
the chart time, never of a date. The Perseids' maximum is listed at solar
longitude 140.0. The Sun reaches it at these instants:

| year | the Sun reaches solar longitude 140.0 (UTC) |
|---|---|
| 2026 | 2026-08-13 02:00 |
| 2027 | 2027-08-13 08:11 |
| 2028 | 2028-08-12 14:28 |

About six hours later each year, and a day earlier after the leap day.
For **5 of the nine** showers the maximum falls on a different calendar
date in 2027 than in 2026, although nothing about the showers changed.

The same holds for the activity period. The calendar's dates are turned
once into an interval of solar longitude, and a shower is active when
the chart time lies inside it. The panel gives both: *Activity period:
Jul 17 to Aug 24 in the calendar, solar longitude 113.9° to 151.2°.* It
adds that the ends are the calendar's convention, known to about a day.
A stream has no edge; activity fades in and fades out.

The solar longitude can also be seen on the chart. With the ecliptic
shown, *Solar longitude marker* in Chart Options puts two small carets
on the ecliptic at the Sun's ecliptic longitude at the chart time, with
the number beside them: the same number the panel states. *Longitude
scale* graduates the line, so an activity period can be read along it
as a stretch of the ecliptic and the marker seen entering and leaving
it as the chart time is changed.
[The whole sky at one time](the-whole-sky-at-one-time.md) explains both.

So a radiant without a time is not a position. The atlas never states
one: the panel, the Inspector and the spoken description always give the
chart time with it.

## The radiant moves

The radiant is not fixed among the stars. The direction the meteors seem
to come from is the stream's own motion combined with the Earth's, and
the Earth's direction of travel turns by about a degree a day as it goes
round the Sun. The radiant drifts with it, eastwards, night by night.

[![The same field of Perseus and Cassiopeia: the six-rayed mark labelled
Perseids now stands far to the right, below Cassiopeia and above
Andromeda.](images/meteor-perseids-early-en-white.png)](images/meteor-perseids-early-en-white.png)

*The same field on **2027-07-20**, early in the Perseids' activity. The
radiant is still well west of Perseus.*

[![The same field again: the mark labelled Perseids stands a little to
the left of its place on the first
figure.](images/meteor-perseids-late-en-white.png)](images/meteor-perseids-late-en-white.png)

*And on **2027-08-20**, a week after the maximum.*

Between the first of these pages and the night of the maximum the
Perseid radiant moves **22.1** degrees across the sky; in the week that
follows, another **4.5**. Over a whole activity period the nine radiants
travel between **4.0 and 37.7 degrees**. A radiant looked up for the
night of the maximum can be most of a constellation away from where it
stands two weeks before.

The atlas follows the IMO's radiant table, which prints a position every
five days. Between two printed rows it moves the radiant along the great
circle that joins them; beyond the last row, while the shower is still
active, it carries the motion on and says that it has. The coordinates
are stated to a whole degree, as the source prints them. A radiant is
not a sharper thing than that: held against an independent network of
video cameras, the atlas's positions for these nine lie at worst **4.0°**
from the measured radiant, and that network's own scatter is of the same
size.

## How high it stands

The radiant's height above your horizon decides how much of a shower you
can see at all.

If the radiant is below the horizon, the stream is arriving from under
your feet, and the Earth is in the way. The Eta Aquariids come from a
radiant near the celestial equator in Aquarius, which from a northern
country rises shortly before the Sun.

[![An ordinary chart of Aquarius and Pegasus with a dashed line, the
horizon, across the upper right corner; almost everything, drawn in
grey, lies below it, among it the six-rayed mark labelled Eta Aquariids
(below the horizon).](images/meteor-eta-aquariids-below-horizon-en-white.png)](images/meteor-eta-aquariids-below-horizon-en-white.png)

*The Eta Aquariid radiant from Oslo on **2027-05-06** at **23:00** UTC,
**14.0** degrees below the mathematical horizon. With Subdue below
horizon on, the mark and its name are drawn quietly, like everything
else below the dashed line.*

A radiant below the horizon is drawn, selectable and spoken like any
other mark, and says so in its name: *Eta Aquariids (below the horizon)*.

Above the horizon, higher is better, for a reason that is pure geometry.
A stream arriving from overhead falls straight onto the layer of air
above you. One arriving at a slant is spread over more of it, as slanting
sunlight is spread over more ground. All else being equal, what reaches
you goes with the sine of the radiant's altitude. From Oslo the Perseid
radiant's highest point in the dark on the night of the 2027 maximum is
**+60.3** degrees, and the sine of that is **0.87**. At 30 degrees the
figure would be a half. That is a geometric factor and nothing more; it
is not a rate for your night.

Height depends on where you are. The panel's night summary gives the
radiant's highest altitude while the Sun is more than 12 degrees down:

| shower, night of the 2027 nominal maximum | from Oslo | from Cape Town |
|---|---|---|
| Eta Aquariids | -4.1 | +46.3 |
| Southern Delta Aquariids | no such dark | +71.3 |
| Perseids | +60.3 | -1.7 |
| Geminids | +63.0 | +23.1 |

From Oslo the Eta Aquariid radiant never clears the horizon while the
sky is dark; from Cape Town (**33.93 S, 18.42 E**) it is the Perseids'
that does not. The same list of showers is a different year of meteors
at another latitude.

## The maximum may not be yours

Each shower is listed with the solar longitude at which its activity is
normally greatest. The atlas calls the instant the Sun reaches it the
**nominal maximum**, and the panel shows it no more finely than the
listing warrants: a longitude printed to a hundredth of a degree gives a
time *known to about ten minutes*, one printed to a tenth *known to an
hour or two*, and one printed to a whole degree only a date, *known to
about a day*.

The button *Set chart time to nominal maximum* puts the chart there, and
what it finds is often a surprise. The maximum is one instant for the
whole Earth, and at your place that instant may be in the middle of the
day:

| shower | nominal maximum 2027 (UTC) | radiant altitude from Oslo | Sun altitude from Oslo |
|---|---|---|---|
| Eta Aquariids | 2027-05-06 15:11 | -16.6 | +29.4 |
| Perseids | 2027-08-13 08:11 | +66.8 | +33.1 |
| Geminids | 2027-12-14 19:53 | +32.5 | -39.9 |

From Oslo, **3 of the nine** nominal maxima of 2027 fall with the Sun
above the horizon and **2** with the radiant below it. The Perseids'
falls on a mid-morning in August. That does not spoil the shower: a
major shower's activity is broad, and the nights on either side of the
maximum are the ones to plan for. It does mean that the instant alone
tells you little. Set the chart time to the maximum, then move it to
your own night and read what the panel says there.

While a shower is active the button offers that activity period's
maximum, also after it has passed, and the panel then says *has passed*.
While it is not active the button offers the next one.

The word *nominal* is chosen with care. These are average values from a
list. In some years a shower does something else: the Earth meets a
dense filament of dust laid down at one particular return of the parent
comet, and for an hour the rate is many times the usual. Such events are
**predictions**, made for a named year by named researchers, and the
IMO's calendar discusses them in its text. The atlas carries none of
them. It says *nominal* of every value, names the edition they come
from, and keeps the word *predicted* for something it does not yet
offer.

## A rate for an ideal sky

The number most often quoted for a shower is its **zenithal hourly
rate**, the ZHR: 150 for the Geminids, 110 for the Perseids. It is the
number least like what you will count.

The ZHR is a calculated figure: the number of shower meteors one ideal
observer would see in an hour under a perfectly clear sky showing stars
to magnitude +6.5, with the radiant overhead. Every one of those
conditions is usually unmet. The radiant is not overhead, and all else
being equal the rate falls with the sine of its altitude, as an earlier
section describes.
The sky is seldom that dark; under a sky that shows fewer stars it
shows fewer meteors too, and it loses most where a shower is rich in
faint ones. The **population index** printed beside the rate says how
rich that is: the higher the index, the larger the share of faint
meteors, and the more a bright sky costs. And one observer sees only
part of the sky.

The atlas therefore prints the ZHR as its source does and says at once
what it is: *the rate for an ideal dark sky with the radiant overhead,
not what an observer will count*. It computes no local rate. An honest
one needs the darkness of your sky on that night, which the atlas does
not know.

## The Moon and the dark

Two things decide how dark a shower night is: where the Sun is below the
horizon, and where the Moon is.

For the Sun the panel gives three durations, sampled through the night
around the chart time: how long the Sun stands more than 6, 12 and 18
degrees below the horizon. The last is astronomical darkness: 18
degrees is the conventional end of astronomical twilight. At high
latitude the true answer
is often *none*. On the night of the 2027 Perseid maximum, from Oslo:
*Sun more than 6° below the horizon: 6.4 h; more than 12°: 3.8 h; more
than 18°: none.* In December the Geminids have **12.6 h** of astronomical
darkness from the same place.

The Moon is a matter of luck, and it changes from year to year. The
panel states its illuminated fraction, its phase, its altitude and its
distance from the radiant. At the Geminids' nominal maximum, from Oslo:

| year | the Moon is lit | degrees from the radiant |
|---|---|---|
| 2026 | 25% | 146 |
| 2027 | 98% | 15 |
| 2028 | 5% | 128 |

The shower's listed values are identical in all three years. The nights
are not.

[![An ordinary chart of Gemini: the six-rayed mark labelled Geminids
just above Castor, and the Moon, a small open circle, lower right of it
on the line of the figure, with the dash-dot ecliptic running below
both.](images/meteor-geminids-evening-en-white.png)](images/meteor-geminids-evening-en-white.png)

*The Geminid radiant from Oslo on **2027-12-14** at **20:00** UTC, with
a nearly full Moon in the same field. The chart shows where the Moon
is; it draws no moonlight.*

The panel states these facts and stops. It never calls a night good or
bad: *Nothing here judges the night or forecasts the weather.*

## The radiant on the whole sky

The radiant is drawn on every kind of page, the whole sky among them.
[The whole sky at one time](the-whole-sky-at-one-time.md) explains that
map; on it a radiant is one more point, placed by its position and on
its own side of the horizon.

[![The whole sky as a wide ellipse with the planets along the straight
middle line and the horizon as a dashed curve; a small six-rayed mark
labelled Geminids stands a little above the middle line, left of centre,
with the Moon just below it.](images/meteor-geminids-whole-sky-en-white.png)](images/meteor-geminids-whole-sky-en-white.png)

*The same chart time on the whole sky. The radiant, the Moon and the
horizon are on one page: what is up, what is down, and what stands
between the radiant and the dark.*

This is the page for the question a planner asks first: at this time,
from here, is the radiant up, and where is the Moon? *Centre on radiant*
in the panel then opens the ordinary chart around it.

## Where the streams come from

The dust of a stream was shed by a parent body, and it travels on along
that body's orbit. For eight of the nine showers the atlas names the
parent, as the IAU Meteor Data Center states it. Most are comets, which
lose dust each time they pass the Sun: 109P/Swift-Tuttle for the
Perseids, 55P/Tempel–Tuttle for the Leonids, 8P/Tuttle for the Ursids,
C/1861 G1 (Thatcher) for the Lyrids. One comet, 1P/Halley, is the parent
of two showers, the Eta Aquariids and the Orionids, because the Earth
crosses its orbit twice a year. The Geminids' parent is listed as an
asteroid, (3200) Phaethon, and the Quadrantids' as the body 2003 EH1.
For the Southern Delta Aquariids the atlas states no parent.

These are identifications made by astronomers from orbits, and the atlas
repeats them as its source gives them. It computes no stream and no
comet.

## Compatible with a shower, never proven to belong

You see a meteor, and its path points back to the radiant. Is it a
Geminid?

Probably, on the night of the Geminids. But direction alone cannot say
so. Meteors that belong to no shower arrive all night from every
direction, and some of them line up with any point you care to name. The
study measured how often, using the IMO's own rules for how wide a
radiant to allow: a meteor travelling in a direction unrelated to the
radiant still lines up with it **15.6%** of the time when it appears 15
degrees from the radiant, and **7.3%** of the time at 50 degrees. Roughly
one stray in ten points at the radiant by accident.

That is why observers who classify meteors use more than the direction:
the meteor's angular speed and the length of its path must also fit the
shower's speed and its distance from the radiant.

The atlas plots no meteors, and its vocabulary is fixed all the same. A
direction that lines up with a radiant is **compatible with** that
shower. Nothing in the atlas says that a meteor *belongs to* or *is
from* one.

## Using the panel and the chart

- **Open the panel** with *Meteor showers...* in the Solar System menu,
  or find the *Meteor showers* group in the Controller's Solar System
  tab. Both are the same panel.
- **Choose a shower.** Choosing does not move the chart or change the
  chart time. The panel now describes the shower at the chart's time and
  place.
- **Give the chart a time.** A shower is described only at a chart time,
  so Place and Time must be on. *Set chart time to nominal maximum* is a
  starting point; then set your own evening.
- **Read the facts**: whether the shower is active, where the radiant is
  and how high, the Sun, the Moon, and the night around the chart time.
- **Find it on the chart** with *Centre on radiant*, which keeps the
  field and the time. Select the mark and the Inspector calls it what it
  is: *Meteor shower radiant: the point the shower's meteors appear to
  come from, at the chart time stated.*
- **Do not look only at the radiant.** The longest streaks appear well
  away from it. The mark tells you which way they will be travelling,
  and how high the source stands.

A shower at a chart time can be kept as a bookmark, with its time and
place, and opening it selects the shower again.

Aloud, the page says of the mark what it is: *Perseids (the radiant of a
meteor shower)*.

## What the atlas does not claim

- **It is not a forecast.** The panel states where things are at the
  chart time. It predicts no rates, no outbursts and no weather, and it
  never calls a night good or bad.
- **The values are nominal.** They are one edition's list of how these
  showers usually behave. A given year can differ, and the atlas carries
  no year-specific prediction.
- **The ZHR is not your count.** It is printed as the source prints it,
  for an ideal sky with the radiant overhead. No local rate is computed.
- **A radiant is known to a degree or so.** Positions are stated to a
  whole degree, in J2000, as the source prints them; the mark's size
  means nothing.
- **Nine showers, no more.** Weaker showers, daytime showers and the
  sporadic background are not in the atlas.
- **The horizon is the mathematical one.** It has no air and no
  landscape. A radiant just above it is, in practice, not yet up.
- **The Moon is drawn, moonlight is not.** The chart shows where the
  Moon is and the panel how much of it is lit; neither shows how bright
  the sky will be.
- **No meteor is plotted, and none is assigned.** A path that lines up
  with a radiant is compatible with the shower, not proven to belong to
  it.
- **The list describes the streams as observed now.** The Sun is
  computed for 1900 to 2100, so the arithmetic works across those years,
  but streams change, and a chart time far from the edition says only
  where its values would place the radiant.

*Technical companions: `docs/decisions/meteor-showers.md` (the study,
the rulings and the feature as built),
`docs/studies/meteor-radiants/measurements.md` and its `platform.md`
(the pages this article shows and the facts it cites), and
`docs/studies/meteor-showers/measurements.md` (the measurements the
rulings were made from).*
