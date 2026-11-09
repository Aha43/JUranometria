# The whole sky at one time

*Why one page of the atlas shows the entire sky at the chart's time and
place, why it is an ellipse with the ecliptic across its middle, how to
read its horizon, its seam and its grid, how to go from it into an
ordinary chart, and what it does not claim*

## Where is everything, right now?

An ordinary page of the atlas answers a local question: what lies around
this star, this planet, this patch of sky. Some questions are not local.
Where are the Sun, the Moon and every planet at this moment? Which of
them are up, and which are under my feet? Is the planet I want anywhere
near the one I am looking at?

No ordinary page can answer that, and the reason was measured before
anything was built. At three chart times the atlas searched out for
itself, the best-placed 120° overview holds **5-6 of the nine bodies**
and the best hemisphere of the celestial globe seven; all nine fit on a
globe only when the planets happen to gather on one side of the sky.

JUranometria 6.5, the release that follows 6.4, adds a page that holds
**9 of 9** every time. One switch, *Whole sky*, on the toolbar, in the
Controller and in the Chart menu, replaces the chart with the entire sky
at the Place and Time instant, as one finite map. This article is about
how to read that map, why it has the shape it has, and how to get from
it back into an ordinary chart. It describes the atlas as it is built
for 6.5; it is not that release's notes.

The atlas's own account of the decisions is in
`docs/decisions/all-sky-projection.md`; this article explains it, it does
not replace it.

## One evening, nine bodies

The page below is Oslo (**59.91 N, 10.75 E**) on **2026-12-15** at
**21:00** UTC, an evening the atlas found by searching a year of nights
for the one with the most to see: **six of the eight bodies other than
the Sun** are above the horizon in darkness.

[![The whole sky as a wide ellipse on white paper: stars as dots with
constellation figures and names, a straight dash-dot line across the
middle with the planets along it, a dashed curve that swings down around
the middle of the map, and everything outside that curve drawn in
grey.](images/whole-sky-evening-en-white.png)](images/whole-sky-evening-en-white.png)

*The whole sky from Oslo, 2026-12-15 at 21:00 UTC. The straight line
across the middle is the ecliptic; the dashed curve is the horizon, and
what lies outside it, drawn quietly, is below it. Every figure in this
article opens at its full size when you select it.*

Everything the ordinary chart draws is here, drawn by the same ink:
stars to **V 5.0**, the constellation figures and their names, the
deep-sky symbols, the grid, the ecliptic, the horizon with its cardinal
points and the zenith. Outside the ellipse there is nothing. The page
says so aloud: *outside the elliptical outline is paper, not sky*.

The Solar System bodies on the page are the ones you have switched on,
no more and no fewer. Every figure in this article has all nine on.

Read the middle line from left to right and every body is accounted for.
The first column is the body's ecliptic longitude from the map's central
meridian, positive to the left; the second its ecliptic latitude, its
height above or below the middle line; the third its altitude over
Oslo's horizon. All are in degrees.

| body | from the central meridian | ecliptic latitude | altitude | |
|---|---|---|---|---|
| Venus | +153.2 | +2.9 | -40.8 | below the horizon |
| Mars | +91.4 | +2.7 | +0.7 | |
| Jupiter | +81.7 | +0.9 | +6.9 | |
| Uranus | -2.5 | -0.1 | +50.0 | |
| Saturn | -57.4 | -2.5 | +22.7 | |
| Neptune | -63.7 | -1.4 | +18.9 | |
| Moon | -87.7 | +0.8 | +2.3 | |
| Sun | -161.4 | +0.0 | -46.7 | below the horizon |
| Mercury | -170.8 | -0.2 | -49.5 | below the horizon |

Uranus is almost on the central meridian, high in the sky. Mars and
Jupiter are low in the east, on the left; the Moon is low in the west,
on the right. The Sun and Mercury are far round to the right, and Venus
far round to the left, all three deep below the horizon.

## No flat map keeps everything

A sphere cannot be flattened without giving something up, and the
atlas's first article, [From the celestial sphere to a
page](from-celestial-sphere-to-page.md), is about the three bargains its
ordinary pages make. None of those three can show the whole sky. The
detailed chart's projection runs to infinity a quarter of the way round;
the overview's leaves out the point opposite its centre and swells
without limit towards it; the globe shows one hemisphere and no more.

The whole sky is drawn in a fourth projection, **Hammer**. Its
construction is short. Take an equal-area map of one hemisphere, a disc.
Halve every longitude, so that the whole sphere is squeezed into that
disc; then stretch the disc to twice its width. The squeeze halves every
area and the stretch doubles it again, so the result keeps areas exactly,
and the whole sky lands inside an ellipse twice as wide as it is high.
The two poles of the map are points, at the top and the bottom. The left
and right edges are one and the same half-circle of the sky, cut open:
the seam.

What the map keeps is **area**. A constellation covers the same share of
the page wherever it falls, the stars are as crowded on the page as they
are in the sky, and the part of the sky below the horizon is **exactly
half the map's area**, on every page.

What it gives up is **shape**, more and more with distance from the
centre. The measure used here is the most by which the map can change an
angle at a place. Along the middle line it is nothing at the centre,
**9.1°** a quarter of the way round, and **34.4°** at 170° from the
central meridian. Off the middle line it grows faster: at 45° of
latitude and 170° of longitude it is **80.9°**. Near the centre a
constellation looks like itself; near the ends of the seam it is sheared
almost beyond recognition. That is the price of seeing everything at
once, and it is why the page offers a way back into an ordinary chart
rather than a way to zoom.

## Why the ecliptic lies flat

A whole-sky map has to choose what its middle line is. The atlas chose
the **ecliptic**, the Sun's yearly path, because that is where the
bodies are: on every measured page the Sun, the Moon and the planets lie
**within ±5 degrees** of it. On this map the ecliptic is one straight
line through the gentlest part of the projection, and "follow the
ecliptic to find every body" is something the eye can do.

The cost is that the map's "up" is not the familiar one. Up is
**ecliptic north**, the pole of the ecliptic in Draco, at the top of the
ellipse. Celestial north, the pole near Polaris, is somewhere else:
even at the centre of the map it leans from page-up by as much as
**23.4°**, and towards the edges local celestial north turns as far as
**158°** from it. So the page never says *celestial north up*. Its title
states what is true:

> Whole sky · Hammer · ecliptic north up · ecliptic longitude increases left at centre

The last clause is the map's handedness. Like every page of the atlas it
shows the sky from inside the sphere, so ecliptic longitude, the
direction the Sun travels through the year, increases to the left. It is
said to hold *at centre* because that is where it was measured to hold;
near the edges of an ellipse "left" is no longer a direction on the sky.

## What the grid tells you

The grid on this page is still the equatorial one, right ascension and
declination, drawn on a map built around the ecliptic. That mismatch is
the most instructive thing on the page. Raise it with *Emphasis* ▸
*Equatorial grid* and the projection shows itself.

[![The evening's whole-sky map with the equatorial grid drawn in strong
blue lines: the meridians fan out from a point beside Polaris in the
upper middle, the circles of high declination are closed loops around
that point, and the lower circles run across the ellipse as long
waves.](images/whole-sky-evening-en-white-grid.png)](images/whole-sky-evening-en-white-grid.png)

*The same evening with the equatorial grid raised by Emphasis. Nothing
else about the page has changed; the grid shows what the projection
does.*

The meridians of right ascension do not meet at the top of the map. They
gather at the celestial pole beside Polaris, well below the top and
usually to one side of the central meridian. The circles of high
declination close around that point; those nearer the celestial equator
run across the page as long waves, and the equator itself crosses the
straight ecliptic at the two equinoxes. Near the centre the cells of the
grid are close to the shapes they have on the sky; towards the seam they
lean and lengthen. But because the map keeps areas, every cell between
the same two circles of declination covers the same area of paper,
however it is bent. Shape is what the grid loses on its way to the edge,
never size.

The grid is numbered sparsely, and only where a figure fits whole. Right
ascension is written on the paper outside the ellipse where a meridian
meets the outline, which happens only at the seam, so each hour that
reaches it appears at both edges. Declination is written inside the sky
where a circle of declination crosses the central meridian.

## Where you stand

The stars sit on this map by their ecliptic coordinates, which no
observer changes. What your place and time decide, besides where the
Sun, the Moon and the planets are, is which longitude is in the middle
and where the horizon runs.

**The central meridian follows your zenith.** The longitude drawn down
the middle of the map is the ecliptic longitude of the point straight
above you, and the second line of the title says which it is: *Central
meridian: zenith's ecliptic longitude 65.0° · Stars to V 5.0*. Change
the time or the place and the whole sky slides sideways under a new
central meridian. The zenith is on that meridian, but it is not the
centre of the map: it keeps its own ecliptic latitude, and from Oslo
that puts it well above the middle line: **39.82°** from the centre on
this evening, and **37.7-68.6 degrees** from it across the three
measured pages. The map is aligned with the observer; it is not a view
of the sky with the zenith in the middle.

**The horizon is one long curve.** With the mathematical horizon shown
in Place and Time, the page draws it dashed, with its four cardinal
points. On the sky it is a great circle; on this map it is a deep bowl
or a long wave that runs from one edge to the other, and the sky above
it is the half that contains the zenith. With *Subdue below horizon* on,
everything on the other side is drawn at reduced contrast, each star,
name and body by its own position, and each line split where it crosses
the horizon. On the evening page the quieted region differs from the
true horizon on **0.070%** of the sky.

[![The same whole-sky map on a black ground with the page's words in
Norwegian: white stars and figures inside the dashed horizon's bowl, grey
ones outside it, the planets along the straight middle
line.](images/whole-sky-evening-nb-black.png)](images/whole-sky-evening-nb-black.png)

*The same evening on the black sky, in Norwegian. The geometry is
identical; only the ground and the words change.*

A body below the horizon says so in its name: *Venus (below the
horizon)*. The longer name needs more room. On the machine that drew
these figures the evening page places **seven of the nine names**, and
Mercury's and the Sun's, close to the right-hand edge, are refused; the
marks are drawn, can be selected and are spoken. With Subdue off,
nothing is quieted and **all nine are named**.

[![The same whole-sky map with nothing drawn in grey: every star, figure
and name at full contrast on both sides of the dashed horizon, and
Mercury and the Sun named at the right-hand
edge.](images/whole-sky-evening-en-white-subdue-off.png)](images/whole-sky-evening-en-white-subdue-off.png)

*The same evening with Subdue below horizon off: the horizon is still
drawn, and nothing is quieted.*

## The seam

The left and right edges of the ellipse are one half-circle of the sky:
the meridian of ecliptic longitude opposite the central one, from pole
to pole. It has to be cut somewhere, and what happens there is ruled:

- **No ink crosses it.** A line that reaches the seam stops at the
  outline and continues from the other edge. Nothing is hidden to make
  the break; a star or a body at the seam is drawn, on its own side.
- **A great circle is cut once.** On every measured page the ecliptic,
  the celestial equator, the horizon and the meridian are each one
  unbroken run from edge to edge. They are not the exact curves of the
  ordinary pages: on this one projection each is drawn through **1,440
  samples**, and its two ends are placed on the outline itself.
- **A split constellation is named once.** A figure the seam divides is
  drawn in two parts, one at each edge, and its name stands on the part
  that holds more of its lines. On every measured page each of the
  **88** constellations has its name anchored on its own figure, and
  none is stranded between two halves.
- **No name straddles it.** A body within a name's width of the seam
  keeps its mark, selectable and spoken, and its name is refused.

Because the map follows the observer, the seam always meets the ecliptic
at its lowest point below the horizon. Around local midnight the Sun is
near it. On **2029-01-03** at midnight UTC, a date found by searching
for the moment when the nine bodies are spread most evenly round the
sky, the Sun stands **+178.2°** from the central meridian, **1.8°**
from the seam.

[![A whole-sky map in which the horizon's bowl holds the upper middle
of the ellipse: the planets are spread along the whole straight middle
line, with Mars and the Moon left of centre, Uranus and Saturn right of
it, and the Sun a small grey mark at the far left end of that
line.](images/whole-sky-spread-en-white.png)](images/whole-sky-spread-en-white.png)

*2029-01-03, 00:00 UTC, Oslo: no gap among the nine bodies wider than
**64.2°**. The Sun is the small grey mark at the far left end of the
middle line, drawn on its own side of the seam, its name refused.*

## When the planets gather

The opposite case is **2027-04-25**, when the seven planets lie within
**138.7°** of ecliptic longitude. From Oslo at midnight UTC only Jupiter
and Mars are up; the Sun, the Moon and the other five planets are below
the horizon, most of them crowded towards the right-hand edge.

[![A whole-sky map in which the horizon runs as a long wave across the
ellipse with the upper part of the sky above it: Mars and Jupiter stand
right of centre on the middle line, and the Sun, Mercury and several
other marks are crowded in the grey region at the far
right.](images/whole-sky-gathering-en-white.png)](images/whole-sky-gathering-en-white.png)

*2027-04-25, 00:00 UTC, Oslo: the planets gathered. All nine marks are
on the page; four names found room.*

This page shows the map's limit as plainly as its use. All nine bodies
are drawn, but where six of them stand close together and each name
carries *(below the horizon)*, there is not room to write them all: on
the machine that drew the figure **four of the nine names** are placed.
The marks remain, each can be selected, and the spoken description
lists all nine with their horizon state. The names are what Open here
is for.

At this scale every planet is a **cartographic symbol**, said as such
aloud - *a cartographic symbol, not Neptune's apparent diameter* - and
no moon is drawn: the page says of each moon it would offer, *too close
to tell apart at this field, not drawn*.

## From the whole sky into a chart

The whole sky is a place to choose from, not a place to stay. Select a
body, a star or an empty position, as on any page, and the Inspector's
button reads *Open here*. It leaves the whole sky for the ordinary chart
centred on what you chose, at the field you were using before you
entered.

[![An ordinary rectangular chart 60 degrees wide: Saturn and Neptune as
small labelled marks near the middle, the March equinox marked on the
sloping dash-dot ecliptic, the figures of Pisces and Cetus, and the Moon
at the right-hand edge.](images/whole-sky-journey-open-here.png)](images/whole-sky-journey-open-here.png)

*Open here on Saturn, from the evening page: the ordinary **60°** chart,
celestial north up again, with Neptune and the Moon in the same field.*

In the measured journey Saturn is selected a pixel off its mark and the
chart opens centred **0.000000°** from Saturn's position, at the 60°
field left behind, in that field's own projection. The whole sky has
done its work: the question "where is Saturn, and what is near it?" was
answered on one page, and the answer is now an ordinary chart you can
zoom, pan and read as any other.

Turning *Whole sky* off without choosing anything returns the ordinary
chart exactly as you left it.

## What zoom, pan and Reset view do here

The whole sky has no field to widen or narrow and no centre to move, so
the controls that change those are refused rather than given a new
meaning:

- **Zoom** is unavailable, and says why: *zoom never enters or leaves
  this view*. You do not arrive at the whole sky by zooming out, and you
  do not leave it by zooming in.
- **Dragging** does not pan. The central meridian belongs to the
  observer, not to the pointer.
- **Reset view** leaves the whole sky for the atlas's first page, as it
  does from anywhere.
- **On this page** is unavailable, because *a list of the entire sky is
  not the regional table this promises*. Open here first, then On this
  page in the chart that opens.
- **The meridian's altitude readout** is not placed; a whole-sky map is
  not read along the meridian.

Nothing about the mode is remembered when the atlas closes: it always
starts on the ordinary chart. A whole-sky view can be kept as a
**bookmark**, and such a bookmark always carries its chart time and
place, because the page is nothing without them. Existing bookmarks are
unchanged, and an older version of the atlas keeps a whole-sky bookmark
exactly as it is, marked as needing attention, without opening or
rewriting it.

## The maps not chosen

The choice of Hammer, of the ecliptic and of the zenith's meridian was
made from drawn pages and measurements, and the alternatives lost for
reasons worth knowing.

**Mollweide**, the other equal-area ellipse, is the closest rival and is
better on average: a mean deformation of **32.2°** over the whole sky
against Hammer's **35.6°**, with straight circles of latitude. But it
carries a constant **12.0°** all along its middle line, where Hammer has
none at the centre. The middle line and the centre are exactly where
this map puts the planets and the observer's sky.

**The plain rectangle**, longitude across and latitude up, has the
lowest mean deformation of all, **16.8°**. It pays for that in area,
which it inflates **up to 36 times** towards its top and bottom edges,
where each pole becomes a line as wide as the map. For a reader in Oslo
that smears the circumpolar sky, the part seen every clear night, across
the whole top of the page.

**Two hemispheres side by side** would trade the seam for a cut along a
whole great circle, the rims of both discs, so the ecliptic and every
figure passing from one disc to the other would be cut twice.

**Celestial north up** was the alternative to the ecliptic. The grid
would be the familiar one and north exactly up at the centre, but the
planets would wander up and down a curve instead of lying along a
straight line through the best part of the map.

**Other central meridians** were drawn too. One fixed for all time
leaves the observer's zenith as much as **175 degrees** of longitude
from the middle of the map. One that follows the previous chart's centre
depends on something unrelated to the question, and put Jupiter **3.1
degrees** from the seam. One opposite the Sun, the tempting choice for a
night sky, cuts the Sun in two at the seam every time.

## What the view does not claim

- **It is not the sky as you see it.** The zenith is not the centre and
  up is not overhead; the horizon is a curve drawn on a map of the
  ecliptic, not the edge of a view. For the sky as it stands around a
  direction, open an ordinary chart there.
- **Shapes hold only near the centre.** Areas are true everywhere;
  figures far from the centre are sheared, and no scale, field width or
  centre is stated because the map has no single one.
- **On the page is not in the sky.** The horizon is the mathematical
  one, with no air and no landscape, and the atlas prints no magnitude
  for a planet. A body above the horizon may stand in daylight or too
  low to be seen.
- **Symbols are not sizes, and names are not guaranteed.** Every planet
  is a symbol here and no moon is drawn; a name is written only where it
  fits whole, and beside the seam or in a crowd it often does not.
- **Stars to V 5.0, no fainter.** The page takes the brighter of your
  own limit and **V 5.0**; a limit of **V 8.0** on the ordinary chart
  becomes V 5.0 here and returns when you leave.
- **One instant.** The page shows where everything is at the chart's
  time. It draws no paths, predicts no risings, settings or
  conjunctions, and searches for no events.
- **No Milky Way.** The atlas draws none on any page, and so none here.

*Technical companions: `docs/decisions/all-sky-projection.md` (the
study, the ruling and the view as built),
`docs/studies/whole-sky/measurements.md` and its `platform.md` (the
measurements and the figures this article shows), and
`docs/studies/all-sky/measurements.md` (the candidates that were drawn
and not chosen, kept as history).*
