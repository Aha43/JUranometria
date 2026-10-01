# #406 Moon authority comparison, measured 2026-09-29 14:02Z

## Skyfield (DE440) vs Horizons (DE441), worst absolute difference by era

| era | rows | astrometric | apparent | horizontal | distance | diameter | illuminated | phase angle | elongation |
|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 16180 | 0.027″ | 0.232″ | 1.03″ | +0.0111 km | -0.0005″ | -0.0051 % | -42.60″ | -0.20″ |
| 1962-1971 | 2615 | 0.413″ | 0.367″ | 2.86″ | -0.0965 km | +0.0008″ | -0.0051 % | +42.79″ | +0.54″ |
| 1972-2027-06 | 14890 | 0.051″ | 0.098″ | 3.15″ | -0.0869 km | +0.0009″ | +0.0051 % | -42.63″ | -0.22″ |
| after | 19185 | 6.947″ | 6.920″ | 413.03″ | -12.5180 km | +0.0682″ | +0.0065 % | +45.34″ | +6.85″ |

Where the worst rows are:
- 1900-1961, apparent: 0.23179 at alert 1900-01-15 00:00
- 1900-1961, astrometric: 0.02697 at alert 1900-01-01 00:00
- 1900-1961, diameter arcsec: -0.00054 at alert 1923-12-31 00:00
- 1900-1961, distance km: 0.01115 at alert 1906-12-10 00:00
- 1900-1961, elongation deg: -0.00006 at chatham 1900-11-05 00:00
- 1900-1961, horizontal: 1.02705 at alert 1960-03-14 00:00
- 1900-1961, illuminated %: -0.00506 at oslo 1938-01-10 00:00
- 1900-1961, phase angle deg: -0.01183 at oslo 1946-12-09 00:00
- 1962-1971, apparent: 0.36666 at quito 1965-09-20 00:00
- 1962-1971, astrometric: 0.41272 at quito 1965-09-20 00:00
- 1962-1971, diameter arcsec: 0.00077 at chatham 1963-04-29 00:00
- 1962-1971, distance km: -0.09650 at quito 1964-03-30 00:00
- 1962-1971, elongation deg: 0.00015 at alert 1965-09-20 00:00
- 1962-1971, horizontal: 2.85784 at cape-town 1964-03-30 00:00
- 1962-1971, illuminated %: -0.00513 at quito 1965-01-11 00:00
- 1962-1971, phase angle deg: 0.01189 at cape-town 1963-12-30 00:00
- 1972-2027-06, apparent: 0.09773 at quito 2027-05-31 00:00
- 1972-2027-06, astrometric: 0.05050 at quito 2027-02-15 00:00
- 1972-2027-06, diameter arcsec: 0.00086 at quito 2027-02-22 00:00
- 1972-2027-06, distance km: -0.08689 at quito 2027-02-22 00:00
- 1972-2027-06, elongation deg: -0.00006 at quito 2026-12-21 00:00
- 1972-2027-06, horizontal: 3.14647 at cape-town 2027-04-19 00:00
- 1972-2027-06, illuminated %: 0.00506 at cape-town 2007-12-31 00:00
- 1972-2027-06, phase angle deg: -0.01184 at cape-town 1999-02-01 00:00
- after, apparent: 6.91979 at quito 2100-12-27 00:00
- after, astrometric: 6.94734 at quito 2100-10-11 00:00
- after, diameter arcsec: 0.06825 at quito 2100-09-06 00:00
- after, distance km: -12.51801 at quito 2100-09-06 00:00
- after, elongation deg: 0.00190 at quito 2100-09-13 00:00
- after, horizontal: 413.02534 at chatham 2100-09-06 00:00
- after, illuminated %: 0.00653 at quito 2100-01-04 00:00
- after, phase angle deg: 0.01259 at chatham 2097-10-21 00:00

Libration (Horizons sub-observer point) extremes by era, and the parallax applied (topocentric − geocentric):
- 1900-1961: sub-lon max |360.00°| at alert 1947-12-01 00:00; sub-lat max |7.74°| at oslo 1908-06-22 00:00; parallax max 1.024° at quito 1933-12-18 00:00
- 1962-1971: sub-lon max |359.99°| at cape-town 1968-05-13 00:00; sub-lat max |7.74°| at oslo 1965-06-21 00:00; parallax max 1.022° at quito 1967-03-27 00:00
- 1972-2027-06: sub-lon max |360.00°| at oslo 1990-10-22 00:00; sub-lat max |7.75°| at oslo 2000-07-24 00:00; parallax max 1.024° at quito 1979-01-29 00:00
- after: sub-lon max |360.00°| at quito 2028-12-11 00:00; sub-lat max |7.74°| at oslo 2059-06-02 00:00; parallax max 1.024° at quito 2036-01-14 00:00

Bright-limb position angle χ (Meeus 48.5) from Skyfield apparent positions vs from Horizons' own apparent Sun and Moon, Oslo weekly, 10488 rows: worst -98.14″ at 2097-10-21 00:00 illu=99.97921

## Meeus 47.a and 48.a (1992 April 12.0 TD, geocentric; published, non-JPL)

| quantity | Meeus | Skyfield/DE440 | difference |
|---|---|---|---|
| apparent λ | 133.162655° | 133.166724° | +14.6″ |
| apparent β | −3.229126° | -3.229190° | -0.2″ |
| distance Δ | 368409.7 km | 368439.4 km | +29.7 km |
| apparent α | 134.688470° | 134.687915° | -2.0″ |
| apparent δ | +13.768368° | 13.768449° | +0.3″ |
| phase angle i (48.a) | 69.0756° | 69.0840° | +30.1″ |
| illuminated fraction k (48.a) | 0.6786 | 0.6785 | -0.0001 |
| bright-limb PA χ (48.a) | 285.0° | 285.04° | +0.04° |
| Horizons at the same TT instant: apparent α, δ; k; i | 134.687900°, 13.768449°; 67.8547%; 69.0761° | vs Skyfield 0.05″; -0.0047%; +28.3″ | |

2026 daily series at Oslo (Horizons): nearest 2026-12-24 at 351483 km, diameter 2039.2″; farthest 2026-12-11 at 411684 km, diameter 1741.0″ (topocentric; daily sampling).
