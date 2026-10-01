# Which way the light falls

*Draft for the JUranometria articles, Sprint 37. The atlas has said how
much of the Moon is lit since Sprint 36; now it draws it. This article
is about the drawing - what a phase is, which way it faces, and what a
chart can and cannot show of it.*

## Two numbers and a direction

Half of the Moon is always lit, the half that faces the Sun. What
changes through the month is how much of that half faces you. The
angle that measures it sits at the Moon, between the direction to the
Sun and the direction to you: the **phase angle**, *i*. When it is
zero the lit half faces you squarely and the Moon is full; at ninety
degrees you see half of the lit half, a quarter Moon; at a hundred and
eighty you see none of it and the Moon is new. The share of the disc
you see lit follows from it exactly - the **illuminated fraction**,
*k* = (1 + cos *i*) / 2 - which is the percentage the Moon table
prints.

![The Sun, the Moon and an observer on the Earth, with the phase angle i
marked at the Moon between the directions to the Sun and to the observer,
and the elongation psi marked at the observer between the directions to
the Sun and to the Moon.](images/moon-phase-geometry.png)

*The two angles the contract froze before anything was drawn. The phase
angle is the Moon's; the elongation - how far the Moon stands from the
Sun in your sky - is yours.*

The fraction does not say where on the disc the light is. A Moon 46 %
lit could have its bright edge on any side. That needs a direction:
the **bright limb's position angle**, *χ*, the direction of the middle
of the lit edge, measured on the sky from north through east. The Moon
table gives it as a number and a compass word - on the morning of
21 June 2026 at Oslo, 45.6 % lit, waxing, lit side 294°,
west-northwest.

![The Moon table at Oslo, 21 June 2026 10:00 UTC: one row, 45.6 % lit,
waxing crescent, 84.8 degrees east of the Sun, lit side 294 degrees
west-northwest.](images/moon-table-instant.png)

## Where north is

"From north through east" sounds simple until you ask which north. On
a star chart, north is the direction of the celestial pole, and the
chart is drawn with it up and east to the left - the sky seen from
inside, as you look up at it. In your own sky, *up* is the zenith,
the point over your head, and the celestial pole is somewhere else -
high in the north from Oslo, below the horizon from Cape Town. The
angle between the two norths at the Moon is the **parallactic angle**,
and it changes as the sky turns.

![Left: the Moon on a chart with north up and east left, the bright
limb's position angle chi marked from north through east. Right: the
same Moon in the local sky with the zenith up, turned by the
parallactic angle q.](images/moon-bright-limb-angle.png)

*The same Moon twice: as a chart draws it, and as you see it. The chart
uses χ as it is; your eye sees it turned by q.*

So the same Moon can be "lit on the right" on the chart and "lit
below" in the sky, and both are true. The atlas draws the chart's
version, because it draws charts; the table's lit side is the chart's
too.

## Drawing it

On the page, north is up only at the centre of a chart centred on the
celestial equator. Elsewhere the meridians lean, and a direction
measured from celestial north has to be turned into the page's own
directions *at the Moon*, not at the page's centre. JUranometria does
that literally: it projects the Moon's position and two points a
second of arc away from it - one towards the pole, one towards
increasing right ascension - and takes the two page directions they
make. The lit side faces cos *χ* along the first plus sin *χ* along
the second.

Those two directions are not always at right angles. At the corner of
a 36° page centred at +45° of declination they meet at 88.3°, because
a tangent projection stretches the sky unevenly away from its centre.
So the atlas keeps both, and checks its own work by turning every
drawn direction back into an angle and comparing it with the χ it was
given - exactly, to a billionth of a degree, at the page's centre,
at its corners and near the pole. The angle is never worked out again
from the Moon's chart position; the table's χ is the one drawn.

![The first-quarter Moon of 21 June 2026 on a 3-degree chart page: a
disc half dark and half white, the white half towards the right and
slightly upward, labelled Moon.](images/moon-first-quarter-oslo-3.png)

*The first quarter of 21 June 2026, 21:55 UTC, from Oslo, at its true
size on a 3° page. The table says 50.1 % lit, lit side 293°,
west-northwest; on a north-up, east-left page that is to the right
and a little up.*

The disc is drawn at its true angular size - never enlarged, at any
field. Its dark side is inked, grey on paper, so the stars it covers
are seen to be covered rather than shining through it; clicking it
never selects a star it hides. The terminator, the line between day
and night on the Moon, is half of an ellipse whose width is |cos *i*|
of the disc's: straight at the quarters, bulging towards the dark side
when the Moon is gibbous and towards the light when it is a crescent.
The lit area that results is *k* of the disc, exactly.

## A month on one row

The strongest check is not one Moon but a month of them. Here is the
lunation of June 2026, from the new Moon of 15 June, one Moon every
twenty-four hours, each cut from its own production page.

![Thirty small Moons in three rows: dark, then thin crescents lit on the
right growing to a half, then gibbous, then full in the middle row, then
waning gibbous lit on the left shrinking back to dark crescents.](images/moon-lunation-june-2026.png)

*June 2026 from Oslo, daily from the new Moon, left to right, ten to a
row. The disc changes size too: the Moon is nearer at some points of
its orbit than others.*

Each of those Moons was then drawn again on a 1° page, where it is
nearly five hundred pixels across, and measured from its own pixels:
how much of it is lit, and which way the lit part faces. On every one
of the twenty-four days on which the table states a lit side, the
drawn Moon faces the table's compass point and shows its lit share to
within two percentage points; on the six days near new and full, where
the table says the lit side is not well defined, the lit share agrees
too.

## From the south

The chart is the sky's, not the observer's. Seen from Cape Town at the
same first-quarter instant, the table gives a Moon 49.3 % lit - the
Moon is close enough that a different place on the Earth sees it a
little differently - and the same lit side, 293°, west-northwest. On
the chart it is drawn the same way round.

![The first-quarter Moon from Cape Town on a 3-degree page, lit on the
right as from Oslo.](images/moon-first-quarter-cape-town-3.png)

What differs is the sky, not the chart: a reader in the south looks at
the Moon with the sky turned over against a northern reader's, the
celestial north pole below their horizon, and sees the same disc lit
on the left. That is
the parallactic angle again, and it is why the atlas states the lit
side on the chart's terms rather than as "left" or "right".

## What the sky shows and the chart does not

When the owner first checked the drawn Moon against the real one, the atlas said 78.3 % lit and Stellarium 78 %; the drawing looked
a little fuller than that. Measured from the screenshot, it was not:
the dark crescent was the width the fraction requires. The difference
is in the Moon. Near the terminator the Sun is low in the lunar sky,
the ground there is lit at a grazing angle, and it looks dim; the eye
counts that band as dark. The chart draws the lit area the fraction
describes in one even tone. It does not draw a surface, so it does not
draw that dimming either.

Scale matters as well. The young crescent of the gallery's moment -
20 March 2026, 21:33 UTC, Oslo, 4.3 % lit, 23.8° from the Sun - is
clear on a 3° page.

![A large grey disc labelled Moon with a thin white crescent along its
right-hand edge, on a 3-degree chart page in Pisces.](images/moon-young-crescent-3.png)

On a printed sheet of 42°, the same crescent is at most 0.15 mm wide -
narrower than the line that draws the disc's edge - and the Moon reads
as an almost dark disc. That is not an error to be corrected by
drawing the crescent larger; it is what true scale means at that
field, and the atlas says so beside the sheet rather than enlarging
anything.

## What it does not do

It does not draw the Moon's surface, its libration - the slow nodding
that lets us see a little more than half of it over a month - or the
faint earthshine on the dark side. It does not draw eclipses or the
Earth's shadow. It draws one instant: the one Place and Time holds,
with no track across the page. And it draws only the Sun and the Moon;
the planets come with questions of their own.

*Technical companion: `docs/decisions/solar-cartography.md`, the
ruled contract and its implementation; `docs/decisions/moon-computation.md`
for the numbers.*
