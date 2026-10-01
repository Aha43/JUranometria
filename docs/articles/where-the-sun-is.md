# Where the Sun is

*How JUranometria knows where the Sun is, and how it holds that answer
to an authority*

## A question with four answers

Ask an atlas where the Sun is and you are asking four questions at
once, and they have different answers.

*Where is it among the stars?* A star chart is drawn in one fixed
frame - the fixed ICRS/J2000 chart frame - and every star on the page
sits where the catalogue put it. To put the Sun on that page, you need
its direction in that same frame, from where you stand, at the moment
you ask, with the light's own travel time taken into account.
Astronomers call that the **astrometric** position. It is the one answer that can be
drawn among the fixed stars without lying to them.

*Where is it in my sky?* That is a different frame: your horizon,
north, the point over your head. It depends on where you are on Earth
and on how far the Earth has turned. Here the answer is the
**apparent** direction - the astrometric one carried into the Earth's
orientation at that moment (its rotation, and the slow precession and
nutation of its axis) and nudged by the aberration of light, which
comes from the Earth's own motion - given as an altitude above the
mathematical horizon and an azimuth from north through east. No refraction: the atlas says so on the heading, because
your real horizon has air in it and the atlas does not pretend to know
about the air.

*How far, and how large?* Distance from you to the Sun's centre, and
the angular diameter that follows from it and the Sun's radius. The
Sun looks about half a degree across, a little more in January than in
July, because the Earth's orbit is not a circle.

*When?* Every one of those answers moves. The Sun drifts a degree a day
along the ecliptic and wheels fifteen degrees an hour across your sky.
So the atlas answers for an instant, or for a range of instants with a
step, and the range is the same idea for the Sun and for the Moon.

## Where the numbers come from

Nothing is fetched. The atlas carries a small excerpt of JPL's
planetary ephemeris **DE440** - the Sun, the Earth–Moon barycentre,
the Earth and the Moon, for the years 1900 to 2100 - as a SPICE kernel
modified by JUranometria under NAIF's rules for such kernels, renamed
and re-attributed, with every coefficient exactly as JPL published it.
The excerpt was proved against the official kernel before it was
written: 172 740 states, identical. Beside it travels the
IERS leap-second record, unmodified, so the atlas knows exactly how
civil time and the ephemeris's time differ - until the date the record
itself says its knowledge ends.

That last point matters more than it looks. Civil time is UTC; the
ephemeris runs on a uniform dynamical time; the two differ by a
step that grows with every leap second - TAI − UTC stands at
37 seconds today - plus a constant. Between
1972 and the pinned record's expiry the atlas knows the difference
exactly. Before 1972 there was no UTC as we know it, and the atlas
reads a civil instant as Earth-rotation time and adds a published
correction. After the record's expiry it estimates, from the same
published expressions, and the table marks every such row *est.* The
honest cost is small - by 2100 the estimate could be off by about two
minutes, which moves the Sun on the page by under six seconds of arc -
but it is stated rather than hidden.

## Held to an authority

A number that cannot be checked is a guess with decimals. Every
quantity the atlas computes was compared, row by row, with JPL
Horizons - an independent implementation on the same family of
ephemerides - for five observers from Alert in the Arctic to the
Chatham Islands, every thirty days across two centuries and daily
through 2026: 12 670 rows. Through the years where time is known
exactly, the chart position agrees to five hundredths of an
arcsecond, altitude and azimuth to eleven, distance to two kilometres
and the Sun's diameter to a two-thousandth of an arcsecond. A textbook
case from outside JPL - Meeus's worked example for 1992 - lands within
the precision the book claims for it.

The comparison also caught something about the atlas's own habits. The
first table named the June solstice and the September equinox at
instants written down from memory, six hours wrong. The positions were
right for the instants given; the names attached to them were not. The
owner's review found it, and the repair was not a corrected number but
a rule: every named instant in the atlas now comes from one cited
fixture - the IMCCE's tables of the equinoxes and solstices, quoted to
the second - and a contract fails the build if any table, request or
test drifts from it.

## What the table says

`View → Sun…` opens a table beside Place and Time, which owns the place
and the instant; the table reads them and keeps nothing of its own.
One row for that instant, or a row for every step of a range - and a
range that runs backwards, asks for more than a thousand rows, cannot
be read or leaves the two centuries is refused with its reason on the
status line, not guessed at. The eight columns are the instant in UTC;
right ascension and declination in the chart's own frame; ecliptic
longitude, measured from the fixed J2000 equinox - so the equinoxes and
solstices of 2026 fall near, not exactly at, 0°, 90°, 180° and 270°,
because the equinox itself has moved a third of a degree since 2000;
altitude and azimuth, without refraction; distance in astronomical
units and millions of kilometres; and the apparent diameter in minutes
and seconds of arc. A negative altitude keeps its number and says it is
below the horizon. An end that is not on the range's grid is added as
the last row and marked. In Norwegian the words are Norwegian and the
decimal is a comma; the digits, the frames and the units are the same,
because they are notation, not language.

![The Sun table at Oslo, 21 June 2026 10:00 UTC: right ascension 5h 58m
40.8s, declination +23 degrees 26 minutes 2 seconds, ecliptic longitude
89.70 degrees, altitude 51.01 degrees, azimuth 150.56 degrees.](images/sun-table-instant.png)

*The June solstice morning at Oslo: the Sun near the top of the
ecliptic, its longitude 89.70°, a little short of 90° because the
equinox has moved since 2000.*

## From the numbers to the chart

The atlas draws the Sun - and every decision about the drawing was
made from pictures on real pages, not from the numbers: the disc at its true angular size and never enlarged, a ring
with its centre dot, opaque over what it covers, named beside it or
not at all, dimmed and saying so below a drawn horizon. The numbers
above are what put it there.

![A 24-degree chart of Pisces with the ecliptic crossing it; near right
ascension 0h a small ring with a centre dot labelled Sun sits on the
ecliptic, just beside a small diamond labelled March equinox.](images/sun-equinox-24.png)

*The IMCCE's March equinox instant of 2026, 20 March 14:45:53 UTC: the
Sun on the ecliptic near right ascension 0h, at its true size on a 24°
page, near - not on - the fixed J2000 equinox mark, because the
equinox of date has moved since 2000. Each has its own word.*

![A 36-degree chart with a dashed line labelled Mathematical horizon and,
below it, a small pale ring labelled Sun (below the horizon).](images/sun-below-horizon-36.png)

*Oslo on the evening of the same day, 18:30 UTC, with the horizon
drawn: the Sun 8.1° below it, dimmed and saying so.*

What the table still does not do: it does not say which constellation
the Sun is in: two independent implementations disagreed on five of
the 12 670 rows, every one at an instant when the Sun was crossing a
boundary, and the atlas has no boundary lookup yet that would settle
such a moment truthfully. And it does not compute sunrise or sunset -
those are events, with a horizon and an atmosphere of their own, and
the atlas will decide them on their own terms.

*Technical companion: `docs/decisions/sun-computation.md`, the frozen
contract with its measurements.*
