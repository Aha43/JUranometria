# Orienting a fixed chart to a place and an instant

Measured by `make place-and-time-study`. Every angle is turned into pixels through the atlas's own gnomonic projection and viewport mapping, on pages its own assembler builds, so an error is stated in what a reader would see rather than only in arcseconds.

## The frame problem

The catalogue and the chart are ICRS/J2000. A meridian, a zenith and a horizon belong to the observer's own date: they are defined against the true equator and equinox of that instant. Drawing them on a J2000 page is therefore a change of frame, and the only question is whether the atlas performs it or pretends it is unnecessary.

A page is 900 px wide, so a degree of sky is worth this much of it:

| field | px per degree | one pixel is |
|---:|---:|---:|
| 36° | 24.2 | 2.48' |
| 24° | 37.0 | 1.62' |
| 18° | 49.6 | 1.21' |
| 12° | 74.7 | 48.18" |
| 8° | 112.3 | 32.05" |
| 6° | 149.9 | 24.02" |
| 4° | 224.9 | 16.01" |
| 3° | 299.9 | 12.00" |
| 2° | 450.0 | 8.00" |
| 1° | 900.0 | 4.00" |

## What the shortcut costs

The shortcut is to compute the zenith's right ascension from sidereal time and plot it on the J2000 page unchanged. It is wrong by the whole of precession since 2000, and it gets worse every year the atlas is used.

| instant | shortcut | precession only | worst pixels at 36° | worst pixels at 1° |
|---|---:|---:|---:|---:|
| J2000 itself | 8.00" | 8.00" | 0 px | 2 px |
| March equinox 2026 | 21.17' | 9.46" | 9 px | 318 px |
| June solstice 2026 | 21.36' | 8.31" | 9 px | 320 px |
| December solstice 2026 | 21.63' | 7.75" | 9 px | 324 px |
| 2050 | 39.34' | 8.63" | 16 px | 590 px |
| 1975 | 20.11' | 7.92" | 8 px | 302 px |

**The shortcut is rejected.** At the atlas's widest field it puts the zenith a third of a degree from where it belongs today, and at the narrowest it is off the page. **Precession alone** leaves nutation as the residual - small, but several pixels at the narrowest field, and free to avoid. The atlas carries **precession and nutation**.

## What the atlas does not know about time

Three simplifications, each a decision with a price rather than an oversight. The atlas ships no time-scale data and makes no network call, so each price is paid deliberately.

| simplification | worst error | at 36° | at 1° |
|---|---:|---:|---:|
| UTC stands in for UT1 (up to 0.9 s apart) | 13.54" | 0.09 px | 3.38 px |
| UTC stands in for TT in the precession arguments | 0.0002" | 0.00 px | 0.00 px |
| the nutation series stops at twenty terms, against the published full-series value for 1987 April 10 | 0.0033" | 0.00 px | 0.00 px |
| polar motion, not modelled | 0.50" | 0.00 px | 0.12 px |
| diurnal aberration, not modelled | 0.32" | 0.00 px | 0.08 px |
| IAU 1976 precession against the IAU 2006 form, over two centuries | 0.28" | 0.00 px | 0.07 px |

Refraction is not modelled and is not in the table: it is a property of air rather than of the sky, which is why the horizon here is named **mathematical** rather than corrected.

**What the nutation row is, and is not.** It is the residual of this twenty-term series against the published full-series value at one date. An earlier draft also claimed a worst case for *any* date, by adding the amplitudes of the omitted terms; that was not a bound (review). It ignored the terms' time-dependent coefficients, and it added quantities in longitude as though they were angles on the sky, which they are not - a nutation in longitude moves a direction by less than itself, by a factor that depends on where the direction lies relative to the ecliptic. A rigorous any-date bound needs the whole 106-term table, which this atlas does not ship, so **no such bound is claimed here**. What can be said is the sensitivity: at ten times the measured residual the tail would still be 0.03", four hundred times smaller than not knowing UT1.

**The accuracy contract.** Adding every term above - 13.54" for UT1, 0.0033" for the nutation residual, 0.28" for the choice of precession model, 0.50" for polar motion and 0.32" for diurnal aberration - the atlas places the zenith, meridian and horizon within **14.64"** of the direction a real observer would measure. That is 0.10 px at the widest field and 3.7 px at the narrowest, and it is dominated by not knowing UT1: every other term together is worth 1.11".

## The three geometries

| geometry | what it is | where it comes from |
|---|---|---|
| **zenith** | the point overhead | right ascension = apparent sidereal time + east longitude, declination = latitude, of date |
| **meridian** | the great circle through both celestial poles and the zenith | hour angle 0 and 12ʰ, drawn as one closed curve |
| **horizon** | every direction ninety degrees from the zenith | the great circle whose pole is the zenith |

The **anti-meridian** and the **nadir** get no vocabulary of their own. The meridian is drawn as one closed circle, so a page holding the far half shows it without a reader being taught a second name; the nadir is the zenith's opposite and is never marked, because a mark for the point beneath a reader's feet is a mark for something they cannot look at.

Sample zeniths, in the chart's own frame:

| place | instant | zenith (J2000) | of date | apart |
|---|---|---|---|---:|
| Oslo | March equinox 2026 | 46.624°, +59.811° | 47.141°, +59.913° | 16.74' |
| Oslo | 2050 | 337.613°, +59.651° | 338.080°, +59.913° | 21.14' |
| Sydney | March equinox 2026 | 187.251°, -33.723° | 187.598°, -33.869° | 19.38' |
| Sydney | 2050 | 118.056°, -33.733° | 118.537°, -33.869° | 25.33' |
| the north pole | March equinox 2026 | 0.822°, +89.853° | 36.389°, +90.000° | 8.80' |
| the north pole | 2050 | 359.296°, +89.717° | 327.328°, +90.000° | 16.96' |

## What the frame difference looks like

On a polar page the difference stops being a number in a table. The meridian passes through the celestial pole **of date**, and that is not the pole the chart is drawn around - so the line misses the chart's own pole by a measurable distance.

(An earlier draft of this study said the meridian *turns* there. It does not: a gnomonic projection maps every great circle to a straight line, and the kink in the first rendering was the study's own drawing breaking the curve wherever two samples landed far apart. The line is straight; it simply does not go where a reader might assume.)

| instant | pole of date, in J2000 | from the chart's pole | at 36° | at 4° | meridian misses the chart's pole by |
|---|---|---:|---:|---:|---:|
| J2000 itself | 226.211°, +89.998° | 8.00" | 0 px | 0 px | 7.89" |
| March equinox 2026 | 0.822°, +89.853° | 8.80' | 4 px | 33 px | 6.33' |
| June solstice 2026 | 0.657°, +89.852° | 8.89' | 4 px | 33 px | 6.32' |
| December solstice 2026 | 0.532°, +89.849° | 9.07' | 4 px | 34 px | 5.96' |
| 2050 | 359.296°, +89.717° | 16.96' | 7 px | 64 px | 6.32' |
| 1975 | 180.898°, +89.867° | 7.97' | 3 px | 30 px | 7.82' |

A reader who opens a polar page sees the meridian pass beside the chart's pole rather than through it. That is not an error to be hidden: it is the J2000 chart and the observer's own sky, drawn honestly on one page, and it is the clearest argument in this study for carrying the frames properly rather than pretending one is the other.

## How much of it is on a page

A meridian and a horizon are great circles; a page is a few degrees of sky. What a reader actually sees is a short arc, or nothing at all, and the module must be honest about the difference.

Measured on pages centred on the released default (M31) and on the zenith itself, for an observer in Oslo at the March 2026 equinox:

| page | field | meridian on paper | horizon on paper | zenith on paper |
|---|---:|---:|---:|---|
| M31 | 36° | 0.0% | 0.0% | no |
| M31 | 8° | 0.0% | 0.0% | no |
| M31 | 1° | 0.0% | 0.0% | no |
| the zenith | 36° | 7.9% | 0.0% | yes |
| the zenith | 8° | 1.7% | 0.0% | yes |
| the zenith | 1° | 0.2% | 0.0% | yes |

So on most pages a reader sees an arc of each, or none: the module must draw what crosses the paper and say nothing where nothing crosses it, rather than promising a line that is not there.

## How finely a curve must be drawn

A great circle is not a straight line on a gnomonic page - except when it is, which is the one thing that makes this cheap: a gnomonic projection maps every great circle to a straight line. The meridian and the horizon are great circles, so two projected points would be enough if the frame rotation did not intervene, and the rotation is rigid, so it still is.

Measured rather than asserted: the furthest a projected sample falls from the chord through its neighbours, at each field.

| field | meridian | horizon |
|---:|---:|---:|
| 36° | 0.0000 px | 0.0000 px |
| 8° | 0.0000 px | 0.0000 px |
| 1° | 0.0000 px | 0.0000 px |

Straight to a thousandth of a pixel, which is the projection's own property and not a tolerance: **#227 may draw each geometry as a small number of segments**, and needs no subdivision of the kind the deep-sky extents required in Sprint 24.

