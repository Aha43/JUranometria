# Uranus on the page

*What the atlas draws of Uranus and its five major moons, why Uranus is
always a symbol and never a disc, why its rings stay in the table, how to
read its tilt and its moons, and what the atlas does not claim*

## A planet that never grows

Uranus is far: nineteen times the Earth's distance from the Sun, and it
hardly changes. Over 1900-2100 the atlas finds it between **3.34-4.08″**
across, a disc no page of the atlas can resolve. JUranometria 6.0
computes Uranus and its five major moons - Miranda, Ariel, Umbriel,
Titania and Oberon - for your spot on Earth and your instant, gives them
a Uranus table beside the other planets', and draws them on the same page
as the fixed stars. This article is about how to read what it draws, and
why so much of Uranus is given in numbers rather than ink.

The atlas's own account of the decisions is in
`docs/decisions/uranus-system.md` and
`docs/decisions/uranus-cartography.md`; this article explains them, it
does not replace them.

## Where the numbers come from

Nothing is fetched. Uranus's place comes from **DE442**, JPL's planetary
ephemeris, as NAIF merged it into its Uranus satellite kernel: the Uranus
barycentre, the centre of mass of Uranus and its moons, over
**1900–2100**, in **387,072 bytes**. Uranus is the first planet in the
atlas whose place does not come from DE440. The reason is measured, not
assumed: compared with JPL Horizons, DE442's barycentre agrees with
Horizons' Uranus to **0.0018″**, while DE440's misses by **0.3779″**;
the two ephemerides put the barycentre up to **4,864.7 km** apart. The
Sun and the Earth stay DE440's, whose DE442 positions differ by at most
**0.49 km**.

The barycentre stands for the planet. Uranus's centre is never more than
**45.136 km** from it, which is **0.0035″** seen from the Earth, far below
anything a page could show. But a distance is a distance: Horizons
measures to the centre, and the atlas's distance to the barycentre is
therefore allowed to differ from Horizons' by **50 km**, a target the
owner amended once the consequence was measured. The study had measured
the angle and not the kilometres; the lesson was carried to the next
planet.

The moons come from NAIF's **URA182.21** satellite solution, in the same
Uranus System pack, **38,055,936 bytes**. They are computed for
**2000–2100**; outside those years Uranus is shown alone.

No released file changed for Uranus: the Sun's, the Moon's, Mercury's,
Venus's, Mars's, Jupiter's and Saturn's files are byte for byte the ones
earlier releases shipped.

## Held to an authority

The numbers are checked, not trusted. The atlas's Uranus was compared
with **JPL Horizons** on **59 Horizons requests**, across several
observers and the weeks of the whole interval. Where the clock correction
is known exactly, from 1972 until **2027-06-28**, Uranus's place among
the stars agrees to **0.0033″** and its distance to **43.529 km**; the
moons' offsets from Uranus agree to **0.0034″**, their position angles to
**0.0051°**, and their states - clear, in front, behind, in the shadow -
on **5,577 of 5,579** rows for Miranda and Ariel and **5,579 of 5,579**
for Titania and Oberon. Every disagreement lies after **2027-06-28**,
where the difference between atomic time and the Earth's rotation is a
forecast; there Uranus's place may differ by up to **0.3194″** by 2100.
That is the clock, not the planet. The packaged application checks itself
against one Horizons row at every build and matches it within
**0.0008″**.

## Full, and tilted on its side

Uranus is an outer planet far from the Sun, so it is never anything but
full: the atlas finds it never less than **99.92 %** lit, at a phase
angle of at most **3.1825°**. The table gives the illuminated fraction
and the phase angle and uses no phase words at all; there is nothing to
name.

What Uranus has instead is its tilt. Its axis lies almost in its orbit,
so over a Uranian year the Earth looks now at a pole, now at the equator.
Over 1900–2100 the sub-observer latitude runs from -82.6° to **82.6°**:
twice in two centuries the Earth looks almost straight down a pole. The
tilt the table gives is *planetocentric*, measured over the pole the IAU
defines; Horizons prints the planetodetic latitude on Uranus's flattened
globe, which differs from it by thousands of arcseconds, and the two
agree within **0.37″** once converted. The atlas never shows Horizons'
number as the tilt. The pole's own position is given as an angle on the
sky, from celestial north through east.

## The named instant

The row the atlas keeps as its "today" is **2026-10-27 12:00** UTC at
Oslo. Uranus is **18.5757 AU** away, **3.794″** across, **99.983 %** lit
at a phase angle of **1.499°**, its north pole at position angle
**289.79°** and tipped towards us, and it stands **8.05°** below the
airless horizon. All five moons are clear of Uranus.

## From the numbers to the chart

The chart draws Uranus where the numbers put it, through the switch
*Uranus on the chart* in View ▸ Solar System and in the Controller's
Uranus group - off until you choose it, then remembered. The rule is the
one Mars follows: **Uranus is drawn as the larger of its true disc and a
6 px cartographic symbol.** For Uranus that is always the symbol. Its
largest disc would reach the 6 px mark only at a field of **0.170°**,
narrower than any field the atlas offers, the closer ones included: on
every page the mark stands for Uranus, drawn full and round in the page's
ink, with no colour, and spoken as *a cartographic symbol, not Uranus's
apparent diameter*. On a 6 px mark the dark part of the disc would be
**0.005 px** and the flattening at most **0.093 px**, neither of them
visible, so the mark is unphased and round by rule.

At the opposition of **2026-11-25**, when Uranus is **3.82″** across and
tipped **73.8°** towards us, the true disc is **a tenth of a pixel** at
8°: the page draws the mark, calls it a symbol, and paints it beneath the
stars so that it never hides one.

![An 8-degree chart centred on Uranus on 25 November 2026, the page's
words in Norwegian: a small round mark labelled Uranus near the middle,
stars as dots of different sizes, the grid.](images/uranus-opposition-2026-8-nb.png)

*The 2026 opposition at 8°, in Norwegian: the 6 px symbol.*

![A 3-degree chart on the black sky: a small white round mark labelled
Uranus in the middle, white stars around it, the grid in
grey.](images/uranus-opposition-2026-3-dark.png)

*The same at 3° on the black sky: the mark in the sky's star ink, no
colour.*

When Uranus passes behind the Sun, the Sun's disc is painted over it. At
Espenak's conjunction hour of **2026-05-22** Uranus stands **0.2°** from
the Sun's centre, inside its disc: the mark is covered and the page says
*behind the Sun*.

![A 1-degree chart: the Sun as a large white disc with a heavy black
outline filling the upper middle of the page, labelled Sun; Uranus hidden
beneath it; a small dot, Oberon, beside the lower edge of the
disc.](images/uranus-conjunction-2026-1.png)

*Behind the Sun, 2026, at 1°: the mark is covered, and the page says so.*

## The rings stay in the table

Uranus has rings, nine narrow classical ones from the Six ring to
Epsilon, the outermost, at **51,149 km** from the planet's centre. The
atlas knows them and does not draw them. The Epsilon ring spans
**6.69-8.16″** on the sky: on the ordinary ladder of fields it lies
inside the 6 px mark, at 0.5° it is still **4.08 px** across, and only at
0.25° does it reach beyond the mark, by **1.08 px** on each side. The
rings are wider than the mark only at a field narrower than **0.34°**,
and every ring's width is far under a pixel on every field. Drawn true
they would be a hairline just outside the mark on one field, and the
atlas never enlarges what it draws. So the rings are table-only: their
opening to us is the tilt the table already gives.

## The closer fields

Uranus is one of the bodies for which zoom in offers the *closer
fields*, **0.5°** and **0.25°**, below the ordinary 1° floor. They gain
Uranus nothing - the planet is still the mark - but they are the only
fields on which the inner moons stand apart from it.

![A 0.25-degree chart on the black sky on 25 November 2026: Uranus as a
small white round mark in the middle, labelled Uranus, with five small
dots around it labelled Oberon, Titania, Umbriel, Ariel and
Miranda.](images/uranus-opposition-2026-0.25-dark.png)

*The 2026 opposition at 0.25° on the black sky: the mark, still 6 px,
and all five moons clear of it. Inside the mark's outline every one of
its **12 of 12** pixels is lit: the mark is drawn full.*

## The five moons

Each moon is a **3 px** symbol at its own place among the stars, decided
on its own. Like Mars's moons and unlike Jupiter's, they have no "floor":
**a moon of Uranus is drawn only where its mark stands clear of Uranus's
drawn mark and of the moons already drawn, on every field.** From 2000 to
2100, weekly, Miranda stands **0.1133″–10.3213″** from Uranus's centre
and Oberon **0.964″–46.4638″**; no moon is ever **0.13″** across. On the
ordinary fields they stand on Uranus's own mark: at 1° Titania is clear
in **30.8 %** of weeks and Oberon in **67.0 %**, the three inner moons
never; at 0.5° Umbriel joins them in **66.9 %** of weeks; Miranda stands
clear only at 0.25°, in **64.2 %** of weeks. Where a moon is not drawn,
the page names it and says *too close to tell apart at this field, not
drawn*.

On **2052-03-23** Titania and Oberon stand at their widest. At 1° Titania
is drawn and the rest are spoken; at 0.25° four of the five stand apart;
at 8° none clears the mark and each is spoken.

![A 1-degree chart on 23 March 2052: Uranus as a small round mark
labelled Uranus, with one small dot just below it labelled
Titania.](images/uranus-moons-widest-2052-1.png)

*Titania and Oberon at their widest, 2052, at 1°: Titania clear and
drawn, the others spoken.*

![A 0.25-degree chart on 23 March 2052, the page's words in Norwegian:
Uranus as a round mark in the middle, with four small dots around it
labelled Oberon, Titania, Umbriel and Ariel.](images/uranus-moons-widest-2052-0.25-nb.png)

*The same at 0.25°, in Norwegian: most of the system stands apart.*

![An 8-degree chart on 23 March 2052: Uranus as a small round mark
labelled Uranus among stars, no moon drawn.](images/uranus-moons-widest-2052-8.png)

*The same at 8°: every moon on Uranus's mark, none drawn, each spoken.*

The states read as they do for Jupiter's, Saturn's and Mars's moons, but
Uranus's mark changes what can be shown. A moon in front of its planet is
drawn as a ringed dot only on a disc at least **12 px** wide, and Uranus's
mark is 6: a transit is never drawn. On **2005-05-28** Miranda crosses
Uranus; the page says *too close to tell apart at this field, not drawn*,
which is the truth of a 6 px mark. A moon behind Uranus is never drawn;
on **2005-03-19** Miranda is behind Uranus in its shadow, and the page
says *behind Uranus, not drawn*. A moon in Uranus's shadow would be a
hollow ring where it stands clear - but an eclipsed moon stands beside
the limb, inside the shadow's width, and so is almost never clear of the
mark: on **2006-02-04** Ariel is in Uranus's shadow and is spoken, not
drawn.

![A 0.25-degree chart on 19 March 2005: Uranus as a round mark labelled
Uranus, with Oberon, Titania and Ariel as small dots around it; Miranda
is not drawn.](images/uranus-miranda-behind-2005-0.25.png)

*Miranda behind Uranus, 2005, at 0.25°: not drawn, and spoken.*

![A 0.25-degree chart on the black sky on 28 May 2005: Uranus as a white
round mark labelled Uranus, with Oberon, Titania and Umbriel as small
dots; Miranda, in transit, is not drawn.](images/uranus-miranda-in-front-2005-0.25-dark.png)

*Miranda in front of Uranus, 2005, at 0.25°: the mark cannot hold a
transit, so the moon is spoken, not drawn.*

![A 0.25-degree chart on 4 February 2006, the page's words in Norwegian:
Uranus as a round mark with Oberon, Titania, Umbriel and Miranda as small
dots around it; Ariel, in the shadow beside the limb, is not
drawn.](images/uranus-ariel-shadow-2006-0.25-nb.png)

*Ariel in Uranus's shadow, 2006, at 0.25°, in Norwegian: beside the limb,
not clear of the mark, spoken.*

Before the moons' years the page draws Uranus alone, as the table shows
it, and says nothing of moons that have no numbers: on **1999-12-31** at
0.25° there is Uranus and nothing else.

![A 0.25-degree chart on 31 December 1999: Uranus as a small round mark
labelled Uranus and nothing near it.](images/uranus-before-the-moons-1999-0.25.png)

*Before the moons' years, 1999, at 0.25°: Uranus alone.*

## What the atlas does not claim

- **No surface, no colour, no rings in ink.** Uranus is drawn in the
  page's ink and ground, full and round, with no markings, no colour, no
  texture and no ring. Nothing the atlas computes measures a colour, and
  the rings are given in the table.
- **The symbol is not the size.** Uranus is always the 6 px mark, which
  says where Uranus is and nothing about how big it looks; the moons are
  always symbols.
- **Seeing it on the page is not seeing it in the sky.** Altitudes are
  airless, and the atlas says nothing about whether Uranus or its moons
  can be seen from where you stand; no magnitude is printed for any
  body. The moons are faint and close to a planet far brighter than
  they are.
- **Computed geometry, not observation.** A moon in front, behind or in
  the shadow is the geometry at the instant you chose. A moon's shadow
  on Uranus is not computed, and none is drawn.
- **No events.** The atlas samples instants; it does not search for
  oppositions, conjunctions, transits or eclipses, or predict their
  times. Espenak's published oppositions and conjunctions are a coarse
  cross-check, found between three and a half hours before and
  **+37 minutes** after his listed hour - his almanac's own precision
  for Uranus, not a claim about the atlas.
- **The moons' years.** The five moons are computed for 2000 to 2100
  only; Uranus itself for 1900-2100.

*Technical companions: `docs/decisions/uranus-system.md` (the numbers and
their contract), `docs/decisions/uranus-cartography.md` (the mark, the
moons, the rings and their rulings), `docs/decisions/uranus-handover.md`
(the settled numerical contract in one place),
`docs/studies/uranus-cartography/measurements.md` and
`docs/studies/uranus-on-the-chart/measurements.md` (the measurements this
article quotes).*
