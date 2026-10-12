# Jupiter's little sky

*What the atlas draws of Jupiter and its four large moons, how to read
the marks, and what it does not claim*

## A planet that carries its own sky

Point a small telescope at Jupiter and you see a sky in miniature: a
flattened disc with four points of light strung out beside it, Io,
Europa, Ganymede and Callisto, in an arrangement that is different
every night and different again by the end of the same night. Galileo
drew them in 1610 as asterisks beside a circle. JUranometria 5.1 draws
them too - at their true places for your spot on Earth and your
instant, on the same page as the fixed stars - and this article is
about how those marks are to be read.

The atlas's own account of the decisions is in
`docs/decisions/jovian-system.md` and `docs/decisions/jovian-cartography.md`;
this article explains them, it does not replace them.

## Where the numbers come from

Nothing is fetched. The atlas carries a second bundled pack beside the
Sun and Moon pack: two modified excerpts of NASA/JPL's **JUP365**
ephemeris, cut to the years the atlas answers for and renamed and
re-attributed under NAIF's rules for modified kernels, with every
coefficient exactly as JPL published it. Jupiter is covered for
**1900–2100**, the four moons for **2000–2100**. Jupiter's size
and the direction of its pole are the IAU 2015 values, copied verbatim
from NAIF's planetary constants; the equatorial radius the apparent
diameter is computed from is **71 492 km**. The pack costs about **59 MB
installed, 51 MB compressed** - the price the owner accepted for a
planet that answers offline.

From those excerpts the atlas computes, for your place and instant,
the same four answers it computes for the Sun and the Moon: where
Jupiter is among the stars (its astrometric place, in the chart's
fixed ICRS/J2000 frame), where it is in your sky (altitude and
azimuth, without refraction), how far and how large (distance; the
equatorial and the polar diameter, because Jupiter is visibly
flattened), and for which instant. It adds three that only a planet
with moons needs: how much of the disc is lit, which way the north
pole points on the page, and where each moon stands beside the planet.

A moon's place is given in the terms JPL Horizons uses: how far east
or west and north or south of Jupiter's centre, in seconds of arc; the
separation; the same distance in Jupiter radii; and a one-line
*state*. The state is built from two facts the atlas keeps apart. Does
the moon's disc overlap Jupiter's apparent figure, and if so is it on
the near side (*in front*) or the far side (*behind*)? And is it lit by
the Sun, or wholly or partly inside Jupiter's shadow? A moon behind
Jupiter cannot be seen whatever its lighting, so that is said first; a
moon in front is sunlit by geometry; a moon clear of the disc but in
the shadow is dark, and the table says why. The overlap test is
limb-to-limb against the flattened figure, counting the moon's own
radius, so a moon touching the limb is already *in front* - which is
what an observer sees at ingress.

## Held to an authority

Every quantity was compared, row by row, with JPL Horizons - an
independent implementation in the same ephemeris family - for five
observers, every seven days across two centuries, daily through 2026,
hourly through December 2026 and minute by minute on one named
evening: **22 993 rows per body**. Through the years where civil time
is known exactly, Jupiter's astrometric place agrees to
**0.0001″** and its apparent place to **0.1337″**; the moons' offsets
from Jupiter agree to **0.0006″**. The visibility states, judged on the
definition Horizons publishes, agree on **91 971 of 91 972 rows** - the
one disagreement a grazing occultation of Europa at Oslo on
**2026-12-06 17:00** UTC, where the separation is **20.3635″** against a
limb sum of **20.3640″**: half a thousandth of an arcsecond, inside the
difference between the two implementations' treatment of light time.
That case is named in the contract rather than allowed for in general;
a second one would stop the build.

Two honest limits were found by measuring, not assumed. Jupiter's
apparent place needs the Sun's gravitational bending of its light,
which the Sun and the Moon could ignore; without it the atlas would be
an arcsecond off when Jupiter stands half a degree from the Sun, so it
is applied. And after the pinned leap-second record ends, in mid-2027,
the atlas can only estimate the difference between civil time and the
ephemeris's time, and Jupiter's apparent place is then good to
**1.3567″**, its distance to **3739.16 km** - the release notes bound
it at two arcseconds, the table marks every such row *est.*, and
nothing in that era is offered as a predicted event time.

## The named evening

On **11 December 2026**, as seen from Oslo, three of the four moons
cross Jupiter's disc at once. The atlas's named fixture is four
instants of that evening, every number read from Horizons' own rows.
At **22:45 UTC** Jupiter stands **18.051°** above the horizon at azimuth
**94.580°**, **4.868899** AU away, **40.491″** across the equator,
**99.270 %** lit. Io is **+15.831″** east and **-5.602″** south of the
centre, **0.829** radii out - in front; Europa **+8.722″** east,
**-0.442″** south, **0.431** radii - in front; Callisto **-15.480″** west,
**+11.269″** north, **0.946** radii - in front; Ganymede **302.990″** away,
**14.966** radii - clear.

![The Jupiter card for 11 December 2026, 22:45 UTC: right ascension 09h
56m 39.3s, declination +13 degrees 25 minutes 46 seconds, altitude
18.1 degrees, azimuth 94.6 degrees, distance 4.869 AU, apparent
diameter 40.5 by 37.9 arcseconds, illuminated 99.3 percent, north pole
position angle 22.2 degrees.](images/jupiter-card-en.png)

![The four moons at that instant: Io east, 15.8 arcseconds east and
5.6 south, 0.83 radii, in front of Jupiter; Europa east, 8.7 east and
0.4 south, 0.43 radii, in front; Ganymede west, 280.3 west and 115.1
north, 14.97 radii, clear; Callisto west, 15.5 west and 11.3 north,
0.95 radii, in front.](images/jupiter-moons-table-en.png)

*The Jupiter card and the four moons, as the table shows them for the
triple transit. Where the atlas writes "east" beside a magnitude,
Horizons writes a sign; the two agree.*

Because the atlas tests limb against limb on the flattened figure
while Horizons tests against an equatorial sphere, the minutes at
which a moon enters or leaves the disc can differ by one: on this
evening Europa's ingress falls at **21:53 against 21:52**, Callisto's
egress at **22:58 against 22:59**, and Io's two crossings, at **22:32
and 00:53**, are the same minute in both. That one minute is the
measured cost of saying which limb a moon touches, and it is stated
rather than hidden.

## From the numbers to the chart

The chart draws Jupiter where the numbers put it, through the same
switch, *Jupiter and moons on the chart*, in the View menu and in the
Controller's Jupiter group - off until you choose it, then remembered.
Two rules decide what you see.

**Jupiter is drawn at its true size where that can be seen, and as a
symbol where it cannot.** At the fields a reader usually works at,
Jupiter's forty arcseconds are smaller than the faintest star's dot,
so the atlas draws a **6 px cartographic symbol**, says so in the
spoken description - *a cartographic symbol, not Jupiter's apparent
diameter* - and paints it beneath the stars, so that it never hides
one. At the 1° field the true disc has grown to **10.1 px** and takes
over: opaque, with its outline turned towards the planet's flattened
figure, the short axis along the pole. The flattening is drawn there
but not claimed to be visible; at that scale the two axes are under a
pixel apart.

![A 1-degree chart centred on Jupiter on 11 December 2026 at 22:45 UTC:
a small disc labelled Jupiter with three dots on it, labelled Europa,
Callisto and Io, and a single dot to the upper right labelled
Ganymede.](images/jupiter-triple-transit-1.png)

*The triple transit at 1°: Jupiter's true disc with Io, Europa and
Callisto in front of it, each a dot ringed in the paper's colour, and
Ganymede clear of it to the east.*

**Each moon is a 3 px mark, and the mark says its state.** A moon's
own disc is smaller than a pixel, so every moon is a symbol, and the
vocabulary is four marks: *clear* of Jupiter, a filled dot in star
ink; *in front*, a filled dot ringed in the page's ground, painted
over the disc; *in Jupiter's shadow*, wholly or partly, a hollow ring;
*behind*, nothing - no ghost, no dashed outline. The table keeps the
finer distinctions in words; the chart draws what can be seen.

![A 1-degree chart in Norwegian on 2 December 2026 at 04:00 UTC:
Jupiter's disc with a small hollow ring just to its right labelled Io,
and filled dots labelled Europa, Callisto and Ganymedes further
out.](images/jupiter-io-shadow-1.png)

*Io wholly in Jupiter's shadow, 2026-12-02 04:00 UTC - a hollow ring
beside the disc - on the Norwegian page, where the moon is spoken
as "Io, i Jupiters skygge".*

![A 1-degree chart on 2 December 2026 at 07:00 UTC: Jupiter's disc with
only three moons drawn - Europa, Callisto and Ganymede - and no mark
for Io.](images/jupiter-io-behind-1.png)

*Three hours later, 2026-12-02 07:00 UTC, Io is behind Jupiter and is
simply not drawn.*

At 1° every moon that is not behind Jupiter is drawn, even where they
crowd, because the atlas's one named evening would otherwise lose its
whole point. At wider fields a moon is drawn only when its mark can be
told apart from Jupiter's and from the others', and each moon is
decided on its own, so one crowded pair never takes the rest with it.
Names go around the marks in the chart's usual way, and a name that
has no clean place is left off on its own, never pushed over something
else.

![A 3-degree chart on the black sky on 6 March 2026 at 06:00 UTC:
Jupiter as a small filled symbol with two dots to its right labelled
Ganymede and Callisto, and no marks for Io and Europa.](images/jupiter-spread-3-dark.png)

*The same four moons at 3° on the black sky, 2026-03-06 06:00 UTC:
Jupiter is a symbol now,
and only Ganymede and Callisto stand far enough from it to be drawn;
Io and Europa are not, and the chart does not pretend otherwise.*

![An 8-degree chart on the black sky on 11 December 2026 at 22:45 UTC:
Jupiter as a small symbol labelled Jupiter, with one dot beside it
labelled Ganymede.](images/jupiter-triple-8.png)

*The triple transit at 8°: at this scale the three transiting moons
cannot be shown, and only Ganymede, far to the east, is.*

The same two rules hold below a drawn horizon, where Jupiter is
dimmed and says so, and on paper, where an exported sheet draws
Jupiter and its moons exactly as the screen does - the same places,
states and names.

![An 8-degree chart on 1 December 2026 at 12:20 UTC with a dashed
mathematical horizon; just below it a pale symbol labelled Jupiter
(below the horizon) with two dots beside it.](images/jupiter-horizon-drawn-8.png)

*Jupiter 1.2° below the mathematical horizon at Oslo, 2026-12-01 12:20 UTC:
drawn pale, and named as below it.*

## Reading the sequence

![A 1-degree chart on 11 December 2026 at 22:30 UTC: Jupiter's disc
with Europa and Callisto on it and Io a filled dot just clear of the
western limb; Ganymede to the upper right.](images/jupiter-sequence-2230.png)

*22:30 UTC: Io, a filled dot, has not yet touched the limb; Europa and
Callisto are already in front.*

![The same chart on the black sky at 23:00 UTC: Io and Europa on the
disc, Callisto a filled dot just clear of its eastern side.](images/jupiter-sequence-2300.png)

*23:00 UTC: Callisto has left the disc and is a plain dot again; Io and
Europa remain in front.*

Read the three pages together and the marks tell the evening: a filled
dot becomes a ringed dot as a moon enters, and a ringed dot becomes a
filled dot as it leaves. Nothing moves on its own - the atlas has no
clock and no animation - but set the instant and the page answers for
it.

## What the atlas does not claim

- **No surface.** There are no cloud belts, no Great Red Spot and no
  rotation; the disc is a figure, not a picture.
- **No shadows on Jupiter.** A moon's shadow falling on the disc is not
  computed and so not drawn, though the table says when a moon is
  itself in Jupiter's shadow.
- **No events.** The atlas samples instants; it does not search for
  transits, eclipses or occultations, and does not predict their
  times. The one named evening is a fixture for measurement, not a
  catalogue.
- **No moons before 2000.** From 1900 to 1999 Jupiter is drawn and
  tabled; the moons are refused with their years named.
- **No closer zoom than 1°**, and no claim that the flattening is
  visible there.
- **One planet.** Jupiter is the only planet in release 5.1.

*Technical companions: `docs/decisions/jovian-system.md` (the numbers
and their contract), `docs/decisions/jovian-cartography.md` (the marks
and their rulings), `docs/studies/jovian-system/service-accuracy.md`
and `docs/studies/jupiter-on-the-chart/measurements.md` (the
measurements this article quotes).*
