# #574 Saturnian satellite events: sat441 against IMCCE (TASS 1.6)

IMCCE's predictions of the phenomena of the Saturnian satellites, 2006-2013, by W. Thuillot from TASS 1.6 (Vienne & Duriez 1995), Institut de mécanique céleste et de calcul des éphémérides, Paris Observatory: retrieved 2026-10-19, kept whole outside Git (digests in `SOURCES.md`). Against them: Skyfield on the pinned `sat441.bsp`, geocentric, each crossing of the satellite's centre over Saturn's limb (occultations, transits) or the edge of Saturn's umbra (eclipses), found within 90 minutes of IMCCE's TT instant on a 1-minute grid and interpolated. Two figures of Saturn: the equatorial sphere and the oblate figure (PCK pole, radii 60268 and 54364 km). Differences are ours minus IMCCE's, in seconds. Shadow transits are not compared. IMCCE rounds to the second.

32099 events read; 307 give 60 in the seconds field, a carried rounding, read as the next minute.

## Differences by satellite, kind and figure

*km* converts each time difference to a distance at Saturn: the difference times the rate at which our crossing function changes there (km/s, across the limb or the umbra's edge). It compares a fast and a slow, grazing event on one scale.

| satellite | kind | figure | events | matched | median s | 95% abs s | max abs s | median abs km | 95% abs km | max abs km |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| mimas | eclipse | sphere | 2722 | 2722 | -63.9 | 781.8 | 1686.7 | 1733 | 5571 | 9699 |
| mimas | eclipse | oblate | 2722 | 2720 | -58.0 | 115.9 | 192.6 | 461 | 1288 | 1580 |
| mimas | occultation | sphere | 2546 | 2546 | -50.9 | 784.6 | 1657.5 | 1484 | 5715 | 9513 |
| mimas | occultation | oblate | 2546 | 2546 | -53.8 | 111.3 | 205.8 | 541 | 1188 | 1613 |
| mimas | transit | sphere | 4984 | 4984 | -63.8 | 650.0 | 1751.2 | 1585 | 5076 | 9706 |
| mimas | transit | oblate | 4984 | 4984 | -59.8 | 125.6 | 192.9 | 655 | 1479 | 1781 |
| enceladus | eclipse | sphere | 1480 | 1480 | -3.7 | 1124.5 | 1924.6 | 1718 | 6517 | 9992 |
| enceladus | eclipse | oblate | 1480 | 1477 | -3.3 | 43.5 | 356.5 | 62 | 192 | 213 |
| enceladus | occultation | sphere | 1412 | 1412 | -5.4 | 1140.9 | 1817.2 | 1865 | 6637 | 9461 |
| enceladus | occultation | oblate | 1412 | 1412 | -2.5 | 33.5 | 114.1 | 64 | 195 | 229 |
| enceladus | transit | sphere | 2605 | 2605 | -2.0 | 1057.2 | 1823.3 | 1808 | 6353 | 9538 |
| enceladus | transit | oblate | 2605 | 2605 | -3.8 | 37.8 | 185.2 | 112 | 291 | 432 |
| tethys | eclipse | sphere | 767 | 767 | -11.5 | 927.8 | 2047.4 | 1052 | 5537 | 9672 |
| tethys | eclipse | oblate | 767 | 765 | -8.0 | 21.5 | 112.3 | 71 | 147 | 216 |
| tethys | occultation | sphere | 820 | 820 | -18.1 | 879.9 | 1786.0 | 1849 | 5551 | 8777 |
| tethys | occultation | oblate | 820 | 820 | -9.1 | 42.9 | 158.9 | 141 | 237 | 287 |
| tethys | transit | sphere | 1582 | 1582 | +2.0 | 847.1 | 2109.9 | 1597 | 5367 | 10264 |
| tethys | transit | oblate | 1582 | 1582 | -1.1 | 51.0 | 290.0 | 167 | 372 | 475 |
| dione | eclipse | sphere | 497 | 497 | +7.6 | 1362.3 | 2019.9 | 2273 | 6490 | 8673 |
| dione | eclipse | oblate | 497 | 497 | +3.8 | 73.2 | 207.9 | 80 | 338 | 380 |
| dione | occultation | sphere | 475 | 475 | +0.5 | 1397.4 | 2313.3 | 1745 | 6794 | 9840 |
| dione | occultation | oblate | 475 | 475 | +18.1 | 41.1 | 94.0 | 126 | 227 | 274 |
| dione | transit | sphere | 794 | 794 | -0.5 | 1269.8 | 2132.0 | 1360 | 6397 | 9188 |
| dione | transit | oblate | 794 | 794 | -1.1 | 64.3 | 117.3 | 69 | 340 | 430 |
| rhea | eclipse | sphere | 183 | 183 | -10.2 | 1490.6 | 2490.4 | 1228 | 6252 | 8881 |
| rhea | eclipse | oblate | 183 | 181 | +17.8 | 122.3 | 282.4 | 247 | 412 | 475 |
| rhea | occultation | sphere | 175 | 175 | +16.6 | 851.1 | 1882.4 | 1472 | 4630 | 7535 |
| rhea | occultation | oblate | 175 | 175 | +18.0 | 51.2 | 102.6 | 124 | 260 | 318 |
| rhea | transit | sphere | 340 | 340 | -3.9 | 829.7 | 2587.5 | 1425 | 4505 | 9662 |
| rhea | transit | oblate | 340 | 340 | -4.2 | 49.4 | 213.0 | 96 | 340 | 487 |
| titan | eclipse | sphere | 33 | 33 | +23.1 | 1619.2 | 1816.3 | 1282 | 4721 | 5164 |
| titan | eclipse | oblate | 33 | 33 | -31.6 | 325.3 | 464.4 | 500 | 1040 | 1056 |
| titan | occultation | sphere | 33 | 33 | +68.5 | 1726.2 | 2355.1 | 2429 | 5442 | 6451 |
| titan | occultation | oblate | 33 | 33 | +84.9 | 132.3 | 152.7 | 371 | 505 | 508 |
| titan | transit | sphere | 44 | 44 | +121.7 | 1990.0 | 2960.5 | 2559 | 6287 | 8047 |
| titan | transit | oblate | 44 | 44 | +114.6 | 197.2 | 232.3 | 486 | 992 | 1048 |
| hyperion | eclipse | sphere | 23 | 23 | -1121.9 | 3084.4 | 3334.9 | 8349 | 12938 | 13514 |
| hyperion | eclipse | oblate | 23 | 23 | -1341.9 | 2660.9 | 3261.5 | 7182 | 12643 | 13411 |
| hyperion | occultation | sphere | 23 | 23 | -468.5 | 3287.9 | 5305.9 | 6281 | 16024 | 17647 |
| hyperion | occultation | oblate | 23 | 23 | +29.1 | 3365.5 | 3810.9 | 3100 | 15321 | 15873 |
| hyperion | transit | sphere | 20 | 19 | -652.4 | 2571.3 | 3080.1 | 3172 | 10136 | 10275 |
| hyperion | transit | oblate | 20 | 20 | -594.7 | 2234.9 | 2735.2 | 1969 | 10130 | 10273 |
| iapetus | eclipse | sphere | 3 | 3 | +881.0 | 960.0 | 968.8 | 2481 | 2848 | 2889 |
| iapetus | eclipse | oblate | 3 | 3 | +481.0 | 1355.8 | 1453.0 | 1412 | 3900 | 4177 |
| iapetus | occultation | sphere | 5 | 5 | +246.9 | 1948.6 | 2091.7 | 2072 | 4852 | 5096 |
| iapetus | occultation | oblate | 5 | 5 | +1182.0 | 1361.3 | 1367.2 | 2637 | 3170 | 3229 |
| iapetus | transit | sphere | 6 | 6 | +475.8 | 2846.9 | 3347.8 | 2165 | 4999 | 5877 |
| iapetus | transit | oblate | 6 | 6 | +560.7 | 1089.0 | 1231.0 | 1494 | 2142 | 2146 |

- equatorial sphere: 21571 of 21572 events matched, 95% within 966.3 s
- oblate figure: 21563 of 21572 events matched, 95% within 111.3 s
- the better fit: the oblate figure

## Unmatched events on the better figure

- mimas, eclipse: 2 (first 2013-01-14 11:10:43 TT, imcce-phensaturne-phenE-S1.dat line 15264)
- enceladus, eclipse: 3 (first 2007-03-15 17:36:01 TT, imcce-phensaturne-phenE-S2.dat line 297)
- tethys, eclipse: 2 (first 2011-10-17 21:23:38 TT, imcce-phensaturne-phenE-S3.dat line 4706)
- rhea, eclipse: 2 (first 2008-07-05 08:09:56 TT, imcce-phensaturne-phenE-S5.dat line 3)

## The worst event of each satellite and kind, on the better figure

Quoted in `imcce-quoted-rows.txt`, verbatim, with their file and line.

- mimas EC.R 2013-01-13 12:44:17 TT (imcce-phensaturne-phenE-S1.dat line 15261): ours -192.6 s
- mimas OC.R 2013-07-13 11:17:26 TT (imcce-phensaturne-phenE-S1.dat line 15290): ours +205.8 s
- mimas TR.I 2012-12-12 22:52:31 TT (imcce-phensaturne-phenE-S1.dat line 15133): ours -192.9 s
- enceladus EC.D 2007-03-17 02:27:41 TT (imcce-phensaturne-phenE-S2.dat line 299): ours +356.5 s
- enceladus OC.R 2012-05-03 18:49:16 TT (imcce-phensaturne-phenE-S2.dat line 7847): ours +114.1 s
- enceladus TR.E 2011-11-13 19:51:16 TT (imcce-phensaturne-phenE-S2.dat line 7565): ours +185.2 s
- tethys EC.R 2011-10-16 00:16:20 TT (imcce-phensaturne-phenE-S3.dat line 4703): ours -112.3 s
- tethys OC.R 2007-09-09 18:07:01 TT (imcce-phensaturne-phenE-S3.dat line 6): ours +158.9 s
- tethys TR.I 2007-09-08 19:12:01 TT (imcce-phensaturne-phenE-S3.dat line 3): ours -290.0 s
- dione EC.D 2011-02-13 05:56:06 TT (imcce-phensaturne-phenE-S4.dat line 2405): ours +207.9 s
- dione OC.R 2008-02-19 17:16:39 TT (imcce-phensaturne-phenE-S4.dat line 196): ours +94.0 s
- dione TR.E 2007-10-12 17:09:29 TT (imcce-phensaturne-phenE-S4.dat line 6): ours +117.3 s
- rhea EC.D 2008-07-09 20:27:06 TT (imcce-phensaturne-phenE-S5.dat line 7): ours +282.4 s
- rhea OC.R 2010-09-17 18:41:48 TT (imcce-phensaturne-phenE-S5.dat line 1048): ours +102.6 s
- rhea TR.I 2010-09-20 00:22:56 TT (imcce-phensaturne-phenE-S5.dat line 1049): ours -213.0 s
- titan EC.D 2010-01-01 21:31:23 TT (imcce-phensaturne-phenE-S6.dat line 129): ours +464.4 s
- titan OC.R 2009-03-04 14:50:11 TT (imcce-phensaturne-phenE-S6.dat line 40): ours +152.7 s
- titan TR.E 2009-03-12 11:34:11 TT (imcce-phensaturne-phenE-S6.dat line 43): ours +232.3 s
- hyperion EC.R 2009-03-23 08:00:02 TT (imcce-phensaturne-phenE-S7.dat line 32): ours -3261.5 s
- hyperion OC.D 2010-06-12 11:22:54 TT (imcce-phensaturne-phenE-S7.dat line 93): ours -3810.9 s
- hyperion TR.I 2010-06-02 00:02:37 TT (imcce-phensaturne-phenE-S7.dat line 91): ours -2735.2 s
- iapetus EC.D 2007-05-05 02:03:13 TT (imcce-phensaturne-phenE-S8.dat line 15): ours +1453.0 s
- iapetus OC.D 2007-02-14 02:12:35 TT (imcce-phensaturne-phenE-S8.dat line 11): ours +1367.2 s
- iapetus TR.I 2007-06-14 01:37:13 TT (imcce-phensaturne-phenE-S8.dat line 17): ours +1231.0 s
