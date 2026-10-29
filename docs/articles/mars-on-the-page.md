# Mars on the page

*What the atlas draws of Mars, Phobos and Deimos, why Mars is usually a
symbol and sometimes a disc, how to read its phase and its two small
moons, and what the atlas does not claim*

## A planet that changes size

Mars is the planet whose size changes most. Near the Sun's side of the
sky it is a small far disc; at a close opposition, when the Earth
overtakes it, it is seven times wider. Over 1900-2100 the atlas finds it
between **3.50″** and **25.10″** across. JUranometria 5.5 computes Mars
and its two moons, Phobos and Deimos, for your spot on Earth and your
instant, gives them a Mars table beside the other planets', and draws
them on the same page as the fixed stars. This article is about how to
read what it draws.

The atlas's own account of the decisions is in
`docs/decisions/mars-system.md` and `docs/decisions/mars-cartography.md`;
this article explains them, it does not replace them.

## Where the numbers come from

Nothing is fetched. Mars's place comes from **DE440**, JPL's planetary
ephemeris, already the source of the atlas's Sun, Moon, Mercury and
Venus: the Mars barycentre, the centre of mass of Mars and its moons,
over **1900–2100**, in **709,632 bytes**. The barycentre stands for the
planet because Mars's centre is never more than **0.210 m** from it.

Phobos and Deimos come from NAIF's **MAR099** satellite ephemeris, in a
Mars System pack of **43,951,104 bytes**. It covers 2000 up to, **not
including**, the first instant of 2050, so the table and the chart give
the moons for **2000 to 2049**; outside those years Mars is shown alone.
The longer version of the same ephemeris, **1.2 GB**, was not taken in.

No released file changed for Mars: the Sun's, the Moon's, Mercury's,
Venus's, Jupiter's and Saturn's files are byte for byte the ones earlier
releases shipped.

## Held to an authority

The numbers are checked, not trusted. The atlas's Mars was compared with
**JPL Horizons** on **36 Horizons responses**, across several observers
and the weeks of the whole interval. Where the clock correction is known
exactly, from 1972 until **2027-06-28**, Mars's place among the stars
agrees to **0.0004″** and its distance to **0.179 km**; the moons'
offsets from Mars agree to **0.0006″**, and their states - clear, in
front, behind, in the shadow - on **3,777 of 3,780** rows for Phobos and
**3,780 of 3,780** for Deimos. The three Phobos rows that differ all lie
after **2027-06-28**, where the difference between atomic time and the
Earth's rotation is a forecast; there Mars's place may differ by up to
**4.0278″** by 2100. That is the clock, not the planet.

One correction matters more for Mars than for the outer planets: the
bending of light passing the Sun. Without it the apparent place misses
Horizons by **0.346″**; with it, by **0.130″**.

## Phase and tilt

Mars is an outer planet, so it is never a crescent: the atlas finds it
never less than **83.86 %** lit. The table names its phase in the same
words it uses for Venus and Mercury, and for Mars only three ever occur:
*gibbous*, *near full* and *behind the Sun*. It also gives the lit side -
the direction of the bright limb - where that is well defined, and says
*not well defined* near full, where there is no edge to point at.

The tilt is how far Mars's north or south pole is tipped towards you,
measured over the pole the IAU defines, *planetocentric*. Horizons
prints a different latitude, the planetodetic one on Mars's flattened
globe; converted, the two agree within **0.013″**, and the atlas never
shows Horizons' number as the tilt.

## The named instant

The row the atlas keeps as its "today" is **2026-10-25 12:00** UTC at
Oslo. Mars stands at **09h 11m 17.0s**, **+17° 45′ 47″**, **16.98°**
above the airless horizon, **1.484474 AU** away. It is **6.3″ / 6.3″**
across, equator and poles, **89.6 %** lit and *gibbous*, **76.8° W** of
the Sun in the morning sky, its lit side at **108° (ESE)**, its north
pole at **0.2°**, and tilted **19.37° (north pole toward us)**.

## From the numbers to the chart

The chart draws Mars where the numbers put it, through the switch *Mars
on the chart* in View ▸ Solar System and in the Controller's Mars group -
off until you choose it, then remembered. The rule is the one Venus and
Mercury follow: **Mars is drawn as the larger of its true disc and a
6 px cartographic symbol**, and either is phased as the table says.

On most pages that is the symbol. At the close opposition of
**2003-08-28**, when Mars was **25.1″** across, the true disc was under a
pixel at 8°: the page draws the 6 px mark, says *a cartographic symbol,
not Mars's apparent diameter*, and paints it beneath the stars so that
it never hides one.

![An 8-degree chart centred on Mars on 28 August 2003, the page's words
in Norwegian: a small round mark labelled Mars near the middle, stars as
dots of different sizes, the grid.](images/mars-opposition-2003-8-nb.png)

*The 2003 opposition at 8°, in Norwegian: the 6 px symbol.*

Mars's largest disc reaches the 6 px mark only at about **1.05°**, so on
the ordinary ladder of fields the true disc takes over only at 1°, and
only near the closest oppositions: in 2003 it is **6.3 px** there, a
real disc, no longer a symbol, and spoken simply as *Mars*.

![A 1-degree chart centred on Mars on 28 August 2003: a small round disc
labelled Mars in the middle of an otherwise empty grid, no star bright
enough nearby.](images/mars-opposition-2003-1.png)

*The 2003 opposition at 1°: the true disc, a little over 6 px.*

When Mars passes behind the Sun, the Sun's disc is painted over it. In
**1912-11-05**, the hour the service finds Mars deepest behind the Sun's
disc that year, the mark is covered and the page says *behind the Sun*.

![A 1-degree chart: the Sun as a large white disc with a heavy black
outline and a dot at its centre, labelled Sun, filling the lower middle
of the page; Mars hidden beneath it; a single star at the upper
left.](images/mars-behind-the-sun-1912-1.png)

*Behind the Sun, 1912, at 1°: the mark is covered, and the page says so.*

## The closer fields

Mars is one of the bodies for which zoom in offers the *closer fields*,
**0.5°** and **0.25°**, below the ordinary 1° floor. They do not magnify
the stars; they exist so that a disc a few arcseconds across can be drawn
as itself. At the 2003 opposition Mars is **12.6 px** at 0.5° and
**25.1 px** at 0.25°.

![A 0.25-degree chart on the black sky on 28 August 2003: Mars as a
white disc in the middle, labelled Mars, with two small white dots near
it labelled Phobos and Deimos.](images/mars-opposition-2003-0.25-dark.png)

*The 2003 opposition at 0.25° on the black sky: the true disc some 25 px
across, and both moons.*

The phase is real but slight. On the day of 2018 when the dark part of
the disc was widest on the sky, **2018-10-06**, Mars was **87.9 %** lit
and **15.1″** across: at 0.25° the dark side is a narrow band on the limb
away from the Sun. On a 6 px symbol at Mars's slimmest the dark part is
**0.97 px**, under a pixel.

![A 0.25-degree chart on 6 October 2018: Mars as a small round disc
labelled Mars, its dark side only a sliver along one edge, with Phobos as
a dot to its left and Deimos as a dot below it to the
right.](images/mars-gibbous-2018-0.25.png)

*Mars at its widest dark side of 2018, at 0.25°: even then the dark side
is only a couple of pixels along the limb away from the Sun.*

## Phobos and Deimos

Each moon is a **3 px** symbol at its own place among the stars, decided
on its own. Unlike Jupiter's moons, they have no "floor": **a moon of
Mars is drawn only where its mark stands clear of Mars's drawn mark and
of the other moon, on every field.** That is because they live close.
From 2000 to 2049, weekly, Phobos stands **0.26″–34.22″** from Mars's
centre and Deimos **0.47″–84.79″**, reaching **35.02″** and **86.77″** at
the 2003 opposition; neither is ever **0.1″** across. On the ordinary
fields both usually stand on Mars's own mark, and the page leaves them
out and says why.

On **2018-07-27**, near Deimos's greatest elongation, Deimos is
**70.7″** from Mars's centre. At 0.25° both moons stand clear and are
drawn; at 8° both stand on Mars's mark, neither is drawn, and the page
says *too close to tell apart at this field, not drawn*.

![A 0.25-degree chart on 27 July 2018: Mars as a disc labelled Mars,
Phobos as a small dot just to its left and Deimos as a small dot farther
left, each named.](images/mars-deimos-wide-2018-0.25.png)

*Deimos near its widest, 2018, at 0.25°: both moons clear and drawn.*

![An 8-degree chart on 27 July 2018: Mars as a small round mark labelled
Mars among stars, no moon drawn.](images/mars-deimos-wide-2018-8.png)

*The same instant at 8°: both moons on Mars's mark, not drawn, and
spoken.*

The states read as they do for Jupiter's and Saturn's moons. A moon in
front of Mars is a dot ringed in the page's ground, painted over the
disc - only where the drawn disc is at least **12 px** wide, so that the
ring can be seen. On **2001-06-30** Phobos crosses a **20.5″** disc,
**6.9″** from its centre, and at 0.25° the page shows and says it: *in
front of Mars*.

![A 0.25-degree chart on 30 June 2001: Mars as a disc labelled Mars with
a small ringed dot on it, Phobos, and Deimos as a dot farther off to the
upper right.](images/mars-phobos-in-front-2001-0.25.png)

*Phobos in front of Mars, 2001, at 0.25°: the dot ringed in the ground,
painted over the disc.*

A moon in Mars's shadow is a hollow ring. On **2018-05-01**, before that
year's opposition, the shadow fell beside the disc and both moons stood
in it, clear of Mars: two hollow rings, each spoken *in Mars's shadow*.
A moon behind Mars is never drawn; the page names it and says *behind
Mars, not drawn*.

![A 0.25-degree chart on 1 May 2018: Mars as a disc labelled Mars, with
two small hollow rings just to its right, Phobos and Deimos, in Mars's
shadow.](images/mars-phobos-shadow-2018-0.25.png)

*Both moons in Mars's shadow, 2018, at 0.25°: hollow rings.*

After the moons' years the page draws Mars alone, as the table shows it,
and says nothing of moons that have no numbers: on **2051-03-01** at
0.25° there is Mars and nothing else.

![A 0.25-degree chart on 1 March 2051: Mars as a small round mark
labelled Mars and nothing near it.](images/mars-after-the-moons-2051-0.25.png)

*After the moons' years, 2051, at 0.25°: Mars alone.*

## What the atlas does not claim

- **No surface, no colour.** Mars is drawn in the page's ink and ground,
  phased, with no markings, no polar caps, no colour and no texture.
  Nothing the atlas computes measures a colour.
- **The symbol is not the size.** On most pages Mars is the 6 px mark,
  which says where Mars is and nothing about how big it looks; the moons
  are always symbols.
- **Seeing it on the page is not seeing it in the sky.** Altitudes are
  airless, and the atlas says nothing about whether Mars or its moons
  can be seen from where you stand; no magnitude is printed for any
  body. Phobos and Deimos are very faint and very close to a bright
  planet.
- **Computed geometry, not observation.** A moon in front, behind or in
  the shadow is the geometry at the instant you chose. A moon's shadow
  on Mars is not computed, and none is drawn.
- **No events.** The atlas samples instants; it does not search for
  oppositions, closest approaches, transits or eclipses, or predict
  their times. Espenak's published oppositions and conjunctions are a
  coarse cross-check, found within **three hours** of his listed hour -
  his almanac's own precision for Mars, not a claim about the atlas.
- **The moons' years.** Phobos and Deimos are computed for 2000 to 2049
  only; Mars itself for 1900-2100.
- **Five planets.** Mercury, Venus, Mars, Jupiter and Saturn are the
  planets in release 5.5.

*Technical companions: `docs/decisions/mars-system.md` (the numbers and
their contract), `docs/decisions/mars-cartography.md` (the mark, the
moons and their rulings), `docs/decisions/mars-handover.md` (the settled
numerical contract in one place),
`docs/studies/mars-system/table-measurements.md` and
`docs/studies/mars-on-the-chart/measurements.md` (the measurements this
article quotes).*
