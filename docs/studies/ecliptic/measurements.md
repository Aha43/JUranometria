# Which ecliptic belongs on a fixed J2000 chart

Measured by `make ecliptic-study`. Reproduced byte-for-byte by the evidence contracts. Pure astronomy: no fonts, rendering, clock, or locale in these numbers.

## The frame

The chart is ICRS/J2000, so the reference circle is the **mean ecliptic of J2000**: a permanent great circle, not a line that drifts with the date.

- J2000 mean obliquity ε₀ = **23.4392911°** (84381.448″)
- Ecliptic north pole: RA 270.0000°, Dec +66.56071° — exactly ε₀ from the celestial pole (measured 23.43929°)

## The cardinal landmarks

Fixed points, named in the interface (issue #271 chose named marks). Each lies on the circle to machine precision.

**Named by month, not by season.** "Summer solstice" reverses for a southern reader, and "vernal" carries the same northern assumption, while this geometry has no observer at all (PR #276 review). The month names are true everywhere.

| landmark | RA | Dec | off-circle |
|---|---:|---:|---:|
| March equinox | 0.000° | +0.0000° | 1.4e-14° |
| June solstice | 90.000° | +23.4393° | 1.4e-14° |
| September equinox | 180.000° | +0.0000° | 1.4e-14° |
| December solstice | 270.000° | -23.4393° | 1.4e-14° |

The March equinox falls at exactly RA 0h, Dec 0° — right ascension is measured from it, so on a J2000 chart the ecliptic crosses the equator at the RA origin by definition.

## Two motions of very different size

The two candidates are the **mean ecliptic and equinox of J2000** and the **mean ecliptic and equinox of date**. Both are *mean*: no nutation is applied to either, because nutation belongs to neither candidate.

**Both come from SOFA, not from the atlas.** An earlier draft built the of-date pole and equinox in mean coordinates and passed them to `SkyFrame.toJ2000`, whose input is a *true* direction and which therefore removes a nutation that was never there (PR #276 review). That added a rotation belonging to neither candidate, and measured the comparison with production's own transformation rather than with the independent authority. Both candidates are now read from `docs/studies/ecliptic/reference-vectors.txt`, where SOFA's `iauEcm06` supplies each date's ecliptic frame directly in ICRS.

They differ in two ways, and the sizes are what settle the decision:

- the **circle** (the plane) moves very little — the of-date ecliptic pole stays under an arcminute from the J2000 pole across two centuries;
- the **equinox** slides ~50″/yr *along* the circle, drifting well over a degree.

| date | ε(t) | circle: pole offset | equinox: offset from J2000 equinox |
|---|---:|---:|---:|
| 1900-01-01 | 23.45229° | 0.78′ | 1.397° |
| 2000-01-01 | 23.43928° | 0.00′ | 0.000° |
| 2026-03-20 | 23.43587° | 0.21′ | 0.366° |
| 2050-07-04 | 23.43271° | 0.40′ | 0.706° |
| 2100-01-01 | 23.42627° | 0.78′ | 1.397° |

At J2000 itself both offsets are exactly zero — the of-date candidate *is* the J2000 candidate there — which is the check that the two are being read in one frame.

**The dates are UTC, and SOFA's `iauEcm06` and `iauObl06` take TT.** No conversion is applied. What follows is a **sensitivity experiment**, not the conversion: no single offset is the true one for all five dates, because UTC did not exist in 1900 and the leap seconds after today have not been decided (PR #276 round 3).

| date shifted by | worst displacement | against the 0.06″ tolerance |
|---|---:|---:|
| 69.184 s — today's TT−UTC | 0.000110″ | 544× smaller |
| 300 s — an allowance over 1900–2100 | 0.000478″ | 125× smaller |

The second row is an **allowance, not a bound**: ΔT was near zero around 1900, is about 69 s now, and published projections for 2100 are of order 200 s with wide uncertainty. 300 s is chosen to sit above all of that; no maximum beyond it is claimed, and the atlas ships no ΔT table. Even so the displacement stays two orders of magnitude below the tolerance, so **no time-scale choice available here can affect the decision**.

So a naive of-date *line* would look nearly right, and only the **equinox landmark** betrays a wrong frame. That is why a fixed atlas anchors both the circle and its landmarks to J2000, and why the oracle check is made at dates far from J2000 rather than on the line alone.

## Agreement with the SOFA oracle

`docs/studies/ecliptic/reference-vectors.txt` carries the ICRS/J2000 equatorial direction of eight ecliptic longitudes, computed by IAU SOFA (iauObl80, iauEcm06). The atlas takes no dependency on SOFA; these are an authority's numbers, checked in.

- obliquity: model 23.439291111° vs SOFA 23.439291111° — agree to 1.1e-10°
- ecliptic longitudes: worst separation **0.0403″** over eight directions

The residual is the ICRS-vs-mean-J2000 frame bias plus the IAU 2006-vs-1980 obliquity difference: below a twentieth of an arcsecond, and far below chart scale. The implementation's tolerance is derived from this measured residual, not copied from production.

## On the paper

Measured through the atlas's own `GnomonicProjection` and `ViewportMapping` at the positions concerned, on the 900x700 page every rendered study page uses. An earlier draft used a flat `900 / field`, which is neither the gnomonic scale nor the mapping production applies (PR #276 review).

**Each is measured on the page where it is worst**, which is not the same page. The two circles *cross* at the equinoxes, so an equinox page is the one place the lines coincide and would flatter the of-date candidate; they are furthest apart a quarter turn away, at the solstices. The equinox marks, of course, are compared on the equinox page.

| date | circle, June solstice page: 8° |  18° |  36° | March equinox mark: 8° |  18° |  36° |
|---|---:|---:|---:|---:|---:|---:|
| 1900-01-01 | 1.5 px | 0.6 px | 0.3 px | 157 px | 69 px | 34 px |
| 2000-01-01 | 0.0 px | 0.0 px | 0.0 px | 0 px | 0 px | 0 px |
| 2026-03-20 | 0.4 px | 0.2 px | 0.1 px | 41 px | 18 px | 9 px |
| 2050-07-04 | 0.7 px | 0.3 px | 0.2 px | 79 px | 35 px | 17 px |
| 2100-01-01 | 1.5 px | 0.6 px | 0.3 px | 157 px | 69 px | 34 px |

For contrast, on the **equinox** page — where the circles cross — the 2100 circle offset is 0.1 px at the 8° field, effectively nothing.

**The circle is not identical, and the earlier draft overstated it as "under a pixel at every field"** (PR #276 review). Even at its worst page and narrowest field the two circles are only a pixel or so apart — small, but visible ink rather than nothing. The equinox mark is two orders of magnitude worse.

That ratio, not either number alone, is the decision: a reader could not tell the two *lines* apart, and could not miss the two *marks* being apart. A chart that fixed its circle to J2000 and its landmarks to the date would look right and read wrong.
