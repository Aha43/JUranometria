# Which ecliptic belongs on a fixed J2000 chart

Measured by `make ecliptic-study`. Reproduced byte-for-byte by the evidence contracts. Pure astronomy: no fonts, rendering, clock, or locale in these numbers.

## The frame

The chart is ICRS/J2000, so the reference circle is the **mean ecliptic of J2000**: a permanent great circle, not a line that drifts with the date.

- J2000 mean obliquity ε₀ = **23.4392911°** (84381.448″)
- Ecliptic north pole: RA 270.0000°, Dec +66.56071° — exactly ε₀ from the celestial pole (measured 23.43929°)

## The cardinal landmarks

Fixed points, named in the interface (issue #271 chose named marks). Each lies on the circle to machine precision.

| landmark | RA | Dec | off-circle |
|---|---:|---:|---:|
| Vernal equinox | 0.000° | +0.0000° | 1.4e-14° |
| Summer solstice | 90.000° | +23.4393° | 1.4e-14° |
| Autumnal equinox | 180.000° | +0.0000° | 1.4e-14° |
| Winter solstice | 270.000° | -23.4393° | 1.4e-14° |

The vernal equinox falls at exactly RA 0h, Dec 0° — right ascension is measured from it, so on a J2000 chart the ecliptic crosses the equator at the RA origin by definition.

## Two motions of very different size

An of-date ecliptic and the fixed J2000 ecliptic differ in two ways, and the sizes are what settle the decision:

- the **circle** (the plane) barely moves — the of-date ecliptic pole, transformed into J2000, sits a fraction of an arcminute from the J2000 pole;
- the **equinox** slides ~50″/yr *along* the circle — the of-date vernal equinox, correctly transformed into J2000, drifts degrees from (0h, 0°).

| date | ε(t) | circle: pole shift | equinox: drift from (0h,0°) |
|---|---:|---:|---:|
| 1900-01-01 | 23.45229° | 0.82′ | 1.392° |
| 2000-01-01 | 23.43929° | 0.10′ | 0.004° |
| 2026-03-20 | 23.43588° | 0.36′ | 0.368° |
| 2050-07-04 | 23.43272° | 0.28′ | 0.709° |
| 2100-01-01 | 23.42629° | 0.93′ | 1.398° |

So a naive of-date *line* would look almost right — under an arcminute — and only the **equinox landmark** betrays a wrong frame. That is why a fixed atlas anchors both the circle and its landmarks to J2000, and why the gate insists on landmark checks at dates far from J2000 rather than the line alone.

## Agreement with the SOFA oracle

`docs/studies/ecliptic/reference-vectors.txt` carries the ICRS/J2000 equatorial direction of eight ecliptic longitudes, computed by IAU SOFA (iauObl80, iauEcm06). The atlas takes no dependency on SOFA; these are an authority's numbers, checked in.

- obliquity: model 23.439291111° vs SOFA 23.439291111° — agree to 1.1e-10°
- ecliptic longitudes: worst separation **0.0403″** over eight directions

The residual is the ICRS-vs-mean-J2000 frame bias plus the IAU 2006-vs-1980 obliquity difference: below a twentieth of an arcsecond, and far below chart scale. The implementation's tolerance is derived from this measured residual, not copied from production.

## At chart scale

A page is 900 px wide, so a degree spans this many pixels:

| field | px per degree | 1.4° equinox drift | 0.9′ circle shift |
|---:|---:|---:|---:|
| 8° | 112.5 | 157 px | 1.7 px |
| 18° | 50.0 | 70 px | 0.8 px |
| 36° | 25.0 | 35 px | 0.4 px |

The equinox drift is a chart-scale distance a reader would see; the circle shift is a fraction of a pixel. The line can be called timeless; its equinox cannot.
