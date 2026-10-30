# Neptune on the page

*What the atlas draws of Neptune, Triton and Nereid, why Neptune is
always a symbol and never a disc, why its moons appear only on the
narrowest pages, how to read its pole and its moons, and what the atlas
does not claim*

## The farthest planet

Neptune is the farthest planet the atlas computes: thirty times the
Earth's distance from the Sun, and a disc that hardly changes. Over
1900-2100 the atlas finds it between **2.18-2.37″** across, smaller than
Uranus and far below anything a page of the atlas can resolve.
JUranometria 6.0, the release that follows 5.5, computes Neptune and its
two ruled moons - Triton and Nereid - for your spot on Earth and your
instant, gives them a Neptune table beside the other planets', and draws
them on the same page as the fixed stars. This article is about how to
read what it draws, and why so much of Neptune is given in numbers rather
than ink. It describes the atlas as it is built for 6.0; it is not that
release's notes.

The atlas's own account of the decisions is in
`docs/decisions/neptune-system.md` and
`docs/decisions/neptune-cartography.md`; this article explains them, it
does not replace them.

## Where the numbers come from

Nothing is fetched. Neptune's place comes from **DE442**, JPL's planetary
ephemeris, as NAIF merged it into its Neptune satellite kernel: the
Neptune barycentre, the centre of mass of Neptune and its moons, over
**1900–2100**, in **416,768 bytes**. Neptune is the second planet in the
atlas, after Uranus, whose place does not come from DE440, and the reason
is measured, not inherited: compared with JPL Horizons, DE442's
barycentre agrees with Horizons' Neptune to **0.0040″**, while DE440's
misses by **0.1072″**; the two ephemerides put the barycentre up to
**2,222.155 km** apart. The Sun and the Earth stay DE440's, whose DE442
positions differ by at most **0.49 km**.

The barycentre stands for the planet. Neptune's centre is never more than
**74.190 km** from it, which is **0.0036″** seen from the Earth, far below
anything a page could show. But Horizons measures a distance to the
centre, and the offset is most of the atlas's distance error against it:
up to **89.48 km** in the study. This time the kilometres were measured
before anything was built - the lesson the Uranus road had learned late -
and the owner ruled the distance target at **100 km**, with the pack's
builder refusing to write at all if the offset ever exceeded **80 km**.

The moons come from NAIF's **NEP098** satellite solution, in the same
Neptune System pack, **50,302,976 bytes**. They are computed for
**2000–2100**; outside those years Neptune is shown alone.

No released file changed for Neptune: the Sun's, the Moon's, Mercury's,
Venus's, Mars's, Jupiter's, Saturn's and Uranus's files are byte for byte
the ones earlier releases shipped.

## Held to an authority

The numbers are checked, not trusted. The atlas's Neptune was compared
with **JPL Horizons** on **39 Horizons requests**, across several
observers and the weeks of the whole interval. Where the clock correction
is known exactly, from 1972 until **2027-06-28**, Neptune's place among
the stars agrees to **0.0035″** and its distance to **93.426 km**, inside
the ruled 100 km; the moons' offsets from Neptune agree to **0.0038″**,
their position angles to **0.0070°**, and their states - clear, in front,
behind, in the shadow - on **5,578 of 5,579** rows for Triton and
**5,579 of 5,579** for Nereid. The one disagreement lies after
**2027-06-28**, where the difference between atomic time and the Earth's
rotation is a forecast; there Neptune's place may differ by up to
**0.2078″** by 2100. That is the clock, not the planet. The packaged
application checks itself against one Horizons row at every build and
matches it within **0.0032″**.

## Full, with a pole that nods

Neptune is an outer planet far from the Sun, so it is never anything but
full: the atlas finds it never less than **99.971 %** lit, at a phase
angle of at most **1.9529°**. The table gives the illuminated fraction
and the phase angle and uses no phase words at all; there is nothing to
name.

Neptune's tilt is a modest one beside Uranus's: over 1900–2100 the
sub-observer latitude runs from -**29.2178°** to +29.2066°, so the Earth
looks at Neptune from a little above or a little below its equator, never
down a pole. What is new is that the pole itself moves. Triton's orbit
makes Neptune's axis trace a small circle over about seven centuries, and
the IAU gives the pole with a periodic term: a right ascension of
299.36° + **0.70 sin N**, where N is an angle that grows a little every
year. Without that term the atlas's pole would miss Horizons' by up to
**2,050.7″**; with it, by **0.018″**. The pack's builder pins the term and
refuses a constants file that lacks it. As for Uranus, the tilt the table
gives is *planetocentric*; Horizons prints the planetodetic latitude, and
the two agree within **0.0520″** once converted. The pole's own position
is given as an angle on the sky, from celestial north through east.

## The named instant

The row the atlas keeps as its "today" is **2026-10-29 12:00** UTC at
Oslo. Neptune is **29.0485 AU** away, **2.351″** across, **99.991 %** lit
at a phase angle of **1.0636°**, its north pole at position angle
**316.498°** and its south pole towards us, and it stands **20.77°**
below the airless horizon. Both moons are clear of Neptune.

## From the numbers to the chart

The chart draws Neptune where the numbers put it, through the switch
*Neptune on the chart* in View ▸ Solar System and in the Controller's
Neptune group - off until you choose it, then remembered. The rule is the
one Mars and Uranus follow: **Neptune is drawn as the larger of its true
disc and a 6 px cartographic symbol.** For Neptune that is always the
symbol. Its largest disc would reach the **6 px** mark only at a field of
**0.099°**, narrower than any field the atlas offers, the closer ones
included: on every page the mark stands for Neptune, drawn full and round
in the page's ink, with no colour, and spoken as *a cartographic symbol,
not Neptune's apparent diameter*. On a 6 px mark the dark part of the
disc would be **0.002 px** and the flattening at most **0.040 px**,
neither of them visible, so the mark is unphased and round by rule. The
pole is not drawn either; the table gives it in numbers, and the spoken
description says the tilt in words.

At the opposition of **2026-09-26**, when Neptune is **2.36″** across and
tipped **18.9°** away from us, the true disc is **a tenth of a pixel** at
8°: the page draws the mark, calls it a symbol, and paints it beneath the
stars so that it never hides one.

![An 8-degree chart centred on Neptune on 26 September 2026, the page's
words in Norwegian: a small round mark labelled Neptun in the middle,
stars as dots of different sizes, the grid.](images/neptune-opposition-2026-8-nb.png)

*The 2026 opposition at 8°, in Norwegian: the 6 px symbol, and no moon
on a page this wide.*

![A 3-degree chart on the black sky: a small white round mark labelled
Neptune in the middle, white stars around it, the grid in
grey.](images/neptune-opposition-2026-3-dark.png)

*The same at 3° on the black sky: the mark in the sky's star ink, no
colour.*

Neptune's conjunctions with the Sun are of two kinds on the page. At
Espenak's conjunction hour of **2026-03-22** the conjunction is in
ecliptic longitude only: Neptune stands **1.3°** from the Sun's centre,
beside the Sun's disc, and the page draws the mark with no Sun relation
stated. At the hour of **2003-01-30** Neptune's disc meets the Sun's: the
Sun's disc is painted over the mark and the page says *behind the Sun*.

![A 3-degree chart on 22 March 2026: the Sun as a large white disc with a
heavy black outline at the top edge of the page, labelled Sun, and
Neptune as a small round mark in the middle, a degree and more
below it.](images/neptune-conjunction-2026-3.png)

*The 2026 conjunction at 3°: beside the Sun, not behind it.*

![A 1-degree chart on 30 January 2003: the Sun as a large white disc with
a heavy black outline filling the middle of the page, labelled Sun;
Neptune hidden beneath it.](images/neptune-conjunction-2003-1.png)

*Behind the Sun, 2003, at 1°: the mark is covered, and the page says so.*

## No rings, anywhere

Neptune has rings, faint and partly in arcs. The atlas holds no numbers
for them: no ring authority was requested for Neptune, so the table has
no ring rows and the page no ring ink. That is the difference from
Uranus, whose rings the atlas knows and keeps in the table. Nothing is
drawn for which the atlas has no numbers.

## The closer fields

Neptune is one of the bodies for which zoom in offers the *closer
fields*, **0.5°** and **0.25°**, below the ordinary 1° floor. They gain
Neptune nothing - the planet is still the mark - but they are the only
fields on which Triton stands apart from it.

![A 0.25-degree chart on the black sky on 26 September 2026: Neptune as a
small white round mark in the middle, labelled Neptune, with Triton as a
small dot just below it and Nereid as a small dot far to the upper
left.](images/neptune-opposition-2026-0.25-dark.png)

*The 2026 opposition at 0.25° on the black sky: the mark, still 6 px,
Triton just clear of it, Nereid far out. Inside the mark's outline every
one of its **12 of 12** pixels is lit: the mark is drawn full.*

## Two moons, and a new rule

Each moon is a **3 px** symbol at its own place among the stars, decided
on its own. Like Mars's and Uranus's moons they have no "floor": a moon
of Neptune is drawn only where its mark stands clear of Neptune's drawn
mark and of the moon already drawn. From 2000 to 2100, Triton stands
**0.5348″–16.9617″** from Neptune's centre and Nereid
**2.1413″–457.7006″**: Triton is always close, Nereid ranges from inside
the mark to nearly eight arcminutes out, on the most eccentric orbit of
any moon the atlas draws. Triton never clears the mark at 1° or wider;
at 0.5° it stands clear in **32.8 %** of weeks and at 0.25° in
**91.3 %**. Nereid is the opposite of every moon so far: by the clearance
rule alone it would stand clear of Neptune's mark on **18.3 %** of weeks
even at 12°, on **86.9 %** at 2°, and on **95.6 %** at 1°.

That raised a question no earlier planet had asked. Horizons gives the
moons' magnitudes as **13.43–13.632** for Triton and **18.669–18.875**
for Nereid, and the atlas draws no star fainter than **V 8**: at its
brightest Triton is **5.4 magnitudes** fainter than the faintest star any
page draws, and Nereid **10.7 magnitudes** fainter. Drawn on an 8° or 12°
page, Nereid would be a lone dot among stars ten magnitudes brighter,
which a reader may take for a star. So the owner ruled: **Neptune's moons
are offered only at the 1° field and the closer fields**, where the
planet's system is the page's subject, and there each mark is still
subject on its own to the clearance rule. On a wider page no moon is
drawn and none is spoken; Nereid is a charted moon, never a lone
magnitude-18.7 dot. Where a moon is offered but not drawn, the page names
it and says *too close to tell apart at this field, not drawn*. Every
moon is drawn at its own J2000 place on the chart, never at Neptune's
place plus the table's offset: for Nereid the two can differ by
**2.317″**, more than two pixels at 0.25°.

![A 1-degree chart on 26 September 2026: Neptune as a small round mark
labelled Neptune, with one small dot to its upper left labelled
Nereid.](images/neptune-opposition-2026-1.png)

*The 2026 opposition at 1°: Nereid clear and drawn, Triton on the mark
and spoken.*

On **2045-11-04** Triton stands at its widest. Even then it is on
Neptune's mark at 1°, spoken and not drawn; at 0.25° it stands clear.

![A 1-degree chart on 4 November 2045: Neptune as a small round mark
labelled Neptune, with one small dot just to its right labelled
Nereid; Triton is not drawn.](images/neptune-triton-widest-2045-1.png)

*Triton at its widest, 2045, at 1°: still on the mark, spoken; Nereid
where it stands.*

![A 0.25-degree chart on 4 November 2045, the page's words in Norwegian:
Neptune as a round mark labelled Neptun, with Triton as a small dot above
and to the left of it and Nereid as a small dot to the
right.](images/neptune-triton-widest-2045-0.25-nb.png)

*The same at 0.25°, in Norwegian: Triton clear of the mark.*

On **2018-08-25** Nereid stands at its widest, and the rule shows: at 2°
it would stand clear, and it is not offered, so the page has Neptune
alone and says nothing of moons. On **2008-03-22** Nereid is at its
nearest, on the mark even at 0.25°, spoken and not drawn.

![A 2-degree chart on 25 August 2018: Neptune as a small round mark
labelled Neptune among a few stars, no moon
drawn.](images/neptune-nereid-widest-2018-2.png)

*Nereid at its widest, 2018, at 2°: the moons are not offered on a page
this wide, and none is drawn or spoken.*

![A 0.25-degree chart on 22 March 2008: Neptune as a small round mark
labelled Neptune, with Triton as a small dot to its right; Nereid is not
drawn.](images/neptune-nereid-nearest-2008-0.25.png)

*Nereid at its nearest, 2008, at 0.25°: on the mark, spoken.*

The states read as they do for Jupiter's, Saturn's, Mars's and Uranus's
moons, but Neptune's mark changes what can be shown. A moon in front of
its planet is drawn as a ringed dot only on a disc at least **12 px**
wide, and Neptune's mark is 6: a transit is never drawn. On
**2043-09-07** Triton crosses Neptune; the page says *too close to tell
apart at this field, not drawn*, which is the truth of a 6 px mark. A
moon behind Neptune is never drawn; on **2044-09-26** Triton is behind
Neptune in its shadow, and the page says *behind Neptune, not drawn*.
Triton's transits and eclipses come only in the seasons around Neptune's
equinoxes, the first of them in the 2040s; Nereid, in every row the atlas
compared, is never in front, behind or in the shadow.

![A 0.25-degree chart on 26 September 2044: Neptune as a round mark
labelled Neptune, with Nereid as a small dot to its upper left; Triton is
not drawn.](images/neptune-triton-behind-2044-0.25.png)

*Triton behind Neptune, 2044, at 0.25°: not drawn, and spoken.*

![A 0.25-degree chart on the black sky on 7 September 2043, the page's
words in Norwegian: Neptune as a white round mark labelled Neptun, with
Nereid as a small dot far to the upper left; Triton, in transit, is not
drawn.](images/neptune-triton-in-front-2043-0.25-dark.png)

*Triton in front of Neptune, 2043, at 0.25°, in Norwegian: the mark
cannot hold a transit, so the moon is spoken, not drawn.*

Before the moons' years the page draws Neptune alone, as the table shows
it, and says nothing of moons that have no numbers: on **1999-12-31** at
0.25° there is Neptune and nothing else.

![A 0.25-degree chart on 31 December 1999: Neptune as a small round mark
labelled Neptune and nothing near it.](images/neptune-before-the-moons-1999-0.25.png)

*Before the moons' years, 1999, at 0.25°: Neptune alone.*

## What the atlas does not claim

- **No surface, no colour, no rings.** Neptune is drawn in the page's
  ink and ground, full and round, with no markings, no colour, no
  texture and no ring. Nothing the atlas computes measures a colour, and
  the atlas holds no number for a ring.
- **The symbol is not the size.** Neptune is always the 6 px mark, which
  says where Neptune is and nothing about how big it looks; the moons are
  always symbols.
- **Seeing it on the page is not seeing it in the sky.** Altitudes are
  airless, and the atlas says nothing about whether Neptune or its moons
  can be seen from where you stand; no magnitude is printed for any
  body. Nereid is the faintest body the atlas has ever drawn, and that is
  why it is drawn only where Neptune is the page's subject.
- **Computed geometry, not observation.** A moon in front, behind or in
  the shadow is the geometry at the instant you chose. A moon's shadow
  on Neptune is not computed, and none is drawn.
- **No events.** The atlas samples instants; it does not search for
  oppositions, conjunctions, transits or eclipses, or predict their
  times. Espenak's published oppositions and conjunctions are a coarse
  cross-check, found between seventeen minutes before and
  **+114 minutes** after his listed hour - his almanac's own precision
  for Neptune, not a claim about the atlas.
- **The moons' years.** Triton and Nereid are computed for 2000 to 2100
  only; Neptune itself for 1900-2100. Neptune's six inner moons and its
  outer irregular moons are not computed at all.

*Technical companions: `docs/decisions/neptune-system.md` (the numbers
and their contract), `docs/decisions/neptune-cartography.md` (the mark,
the moons and their rulings), `docs/decisions/neptune-handover.md` (the
settled numerical contract in one place),
`docs/studies/neptune-cartography/measurements.md` and
`docs/studies/neptune-on-the-chart/measurements.md` (the measurements
this article quotes).*
