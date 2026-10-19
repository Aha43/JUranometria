# #574 Saturn authority comparison

Skyfield on `sat441.bsp` (Saturn, 699, and Mimas-Iapetus, 601-608, about the Saturn system barycentre; its own barycentre and Earth are DE440's) against the kept JPL Horizons responses (Horizons: `sat441l` for every Saturn-system body, DE441 for the Earth). Observers at sea level on the WGS84 ellipsoid. Civil time: UT1 before 1972, UTC from 1972; the record's expiry, 2027-06-28, read from `Leap_Second.dat`, bounds the exact era. No gravitational deflection, as the atlas computes.

Constants read from `pck00011.tpc`: Saturn's radii 60268, 60268, 54364 km; pole RA 40.589 -0.036 T, Dec 83.537 -0.004 T (T in Julian centuries TDB from J2000, at the light-time-corrected epoch); the Sun's radius 695700 km; each moon's three radii. Ring radii from the retained PDS table.

## Saturn: worst absolute difference by era

| era | rows | astrometric″ | apparent″ | horizontal″ | distance km | diameter″ | illuminated % | phase angle″ | phase angle vs phi″ | phase, Sun light time″ | phase, Sun light time, vs phi″ | illuminated, Sun light time % | phase, Sun aberrated″ | phase, Sun aberrated, vs phi″ | pole RA″ | pole Dec″ | pole angle″ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | 6475 | +0.0002 | +0.6118 | +1.0086 | -0.9184 | -0.0000 | +0.0002 | -28.0501 | -7.1910 | -28.0465 | -7.1916 | +0.0002 | +21.3212 | -0.1799 | -0.0180 | -0.0180 | -0.2629 |
| 1962-1971 | 1049 | +0.0035 | +0.5323 | +2.0079 | +17.1646 | -0.0000 | +0.0002 | +26.9880 | -7.1170 | +26.9943 | -7.1235 | +0.0002 | +20.8847 | +0.1802 | +0.0180 | -0.0180 | -0.1827 |
| 1972-2027-06 | 6546 | +0.0000 | +0.7067 | +2.7741 | +0.0537 | -0.0000 | +0.0002 | -27.8460 | +7.1889 | -27.8420 | +7.1809 | +0.0002 | +21.2386 | -0.1800 | +0.0180 | -0.0180 | +0.1874 |
| after | 7677 | +0.0017 | +1.2081 | +405.5233 | +10.2882 | -0.0000 | +0.0002 | -27.9673 | -7.1839 | -27.9614 | -7.1860 | +0.0002 | +21.5376 | +0.1800 | -0.0180 | -0.0180 | -0.2708 |

## Saturn: the ring opening, three readings against Horizons' sub-observer and sub-solar latitudes

| era | B centric″ | B detic″ | B detic, apparent line″ | B′ centric″ | B′ detic″ | B′ detic, Sun light time″ | B′ detic, Sun aberrated″ |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | +18192.321 | -0.002 | +11.972 | -18086.831 | +3.716 | +3.711 | +0.002 |
| 1962-1971 | +17466.504 | +0.003 | +11.969 | +17408.456 | -3.648 | -3.649 | +0.002 |
| 1972-2027-06 | +18195.918 | -0.002 | +11.976 | -18089.142 | +3.716 | +3.711 | +0.002 |
| after | +18194.265 | +0.002 | +11.986 | -18091.349 | +3.717 | +3.712 | +0.002 |

*centric* is the planetocentric latitude of the observer (or the Sun) over the PCK pole: the ring plane's true opening angle. *detic* is the same direction's planetodetic latitude on the PCK spheroid. *apparent line* uses the aberrated line of sight. *Sun light time* places the Sun where Saturn sees it, its light time to Saturn iterated; *Sun aberrated* adds the aberration of Saturn's own barycentric motion. The Sun's ring-plane crossings below use the aberrated reading.

## The rings: openings, crossings and apparent ellipses

**dense-2026-saturn-oslo** (365 rows, 2026-01-01 to 2026-12-31):
- widest opening to the observer: B = -9.155° planetocentric at 2026-07-21 (Horizons' ObsSub-LAT -11.203°)
- ring-plane crossings of the Earth: Horizons 0, ours 0
- ring-plane crossings of the Sun: Horizons 0, ours 0
- nearest approaches (Horizons' range rate through zero): 1, first 2026-10-05, last 2026-10-05
- nearest row 2026-10-05: A Ring outer edge 44.72″ by 5.78″; C Ring inner edge 24.35″ across; the globe 19.70″ (Horizons)
- farthest row 2026-03-25: A Ring outer edge 35.96″ by 3.14″; C Ring inner edge 19.58″ across; the globe 15.84″ (Horizons)

**matrix-7d-saturn-oslo** (10488 rows, 1900-01-01 to 2100-12-27):
- widest opening to the observer: B = -26.995° planetocentric at 2003-04-07 (Horizons' ObsSub-LAT -32.049°)
- ring-plane crossings of the Earth: Horizons 30, ours 30
  - Horizons 1907-04-12 23:28, ours 1907-04-12 23:28 (+0.00 h)
  - Horizons 1907-10-04 00:33, ours 1907-10-04 00:33 (-0.00 h)
  - Horizons 1908-01-07 18:00, ours 1908-01-07 18:00 (-0.00 h)
  - Horizons 1920-11-08 07:10, ours 1920-11-08 07:10 (-0.00 h)
  - Horizons 1921-02-22 03:55, ours 1921-02-22 03:55 (+0.00 h)
  - Horizons 1921-08-04 02:12, ours 1921-08-04 02:12 (+0.00 h)
  - Horizons 1937-02-21 00:41, ours 1937-02-21 00:41 (+0.00 h)
  - Horizons 1950-09-14 11:33, ours 1950-09-14 11:33 (+0.00 h)
  - Horizons 1966-04-02 14:27, ours 1966-04-02 14:27 (+0.00 h)
  - Horizons 1966-10-28 13:45, ours 1966-10-28 13:45 (+0.00 h)
  - Horizons 1966-12-18 08:25, ours 1966-12-18 08:25 (+0.00 h)
  - Horizons 1979-10-27 08:14, ours 1979-10-27 08:14 (+0.00 h)
  - Horizons 1980-03-12 09:32, ours 1980-03-12 09:32 (-0.00 h)
  - Horizons 1980-07-23 07:04, ours 1980-07-23 07:04 (-0.00 h)
  - Horizons 1995-05-22 06:55, ours 1995-05-22 06:55 (-0.00 h)
  - Horizons 1995-08-10 16:56, ours 1995-08-10 16:56 (+0.00 h)
  - Horizons 1996-02-12 00:10, ours 1996-02-12 00:10 (-0.00 h)
  - Horizons 2009-09-04 13:27, ours 2009-09-04 13:27 (+0.00 h)
  - Horizons 2025-03-23 17:54, ours 2025-03-23 17:54 (-0.00 h)
  - Horizons 2038-10-15 17:26, ours 2038-10-15 17:26 (-0.00 h)
  - Horizons 2039-04-01 19:59, ours 2039-04-01 19:59 (+0.00 h)
  - Horizons 2039-07-09 15:32, ours 2039-07-09 15:32 (-0.00 h)
  - Horizons 2054-05-06 04:34, ours 2054-05-06 04:34 (+0.00 h)
  - Horizons 2054-08-31 07:47, ours 2054-08-31 07:47 (-0.00 h)
  - Horizons 2055-02-01 08:52, ours 2055-02-01 08:52 (+0.00 h)
  - Horizons 2068-08-25 05:04, ours 2068-08-25 05:04 (-0.00 h)
  - Horizons 2084-03-14 08:17, ours 2084-03-14 08:17 (-0.00 h)
  - Horizons 2097-10-05 04:48, ours 2097-10-05 04:48 (+0.00 h)
  - Horizons 2098-04-26 14:24, ours 2098-04-26 14:24 (-0.00 h)
  - Horizons 2098-06-18 10:44, ours 2098-06-18 10:44 (+0.00 h)
- ring-plane crossings of the Sun: Horizons 14, ours 14
  - Horizons 1907-07-27 03:13, ours 1907-07-27 03:13 (+0.00 h)
  - Horizons 1921-04-12 01:37, ours 1921-04-12 01:37 (+0.00 h)
  - Horizons 1936-12-29 11:46, ours 1936-12-29 11:46 (+0.00 h)
  - Horizons 1950-09-21 20:29, ours 1950-09-21 20:29 (+0.00 h)
  - Horizons 1966-06-16 11:17, ours 1966-06-16 11:17 (+0.00 h)
  - Horizons 1980-03-03 17:23, ours 1980-03-03 17:23 (-0.00 h)
  - Horizons 1995-11-19 15:42, ours 1995-11-19 15:42 (+0.00 h)
  - Horizons 2009-08-11 03:30, ours 2009-08-11 03:30 (-0.00 h)
  - Horizons 2025-05-06 15:34, ours 2025-05-06 15:34 (+0.00 h)
  - Horizons 2039-01-22 21:05, ours 2039-01-22 21:05 (-0.00 h)
  - Horizons 2054-10-10 08:30, ours 2054-10-10 08:30 (-0.00 h)
  - Horizons 2068-06-29 10:07, ours 2068-06-29 10:07 (-0.00 h)
  - Horizons 2084-03-27 11:51, ours 2084-03-27 11:51 (+0.00 h)
  - Horizons 2097-12-13 09:14, ours 2097-12-13 09:14 (+0.00 h)
- nearest approaches (Horizons' range rate through zero): 194, first 1900-06-25, last 2100-04-19
- nearest row 1914-12-21: A Ring outer edge 46.97″ by 20.89″; C Ring inner edge 25.58″ across; the globe 20.70″ (Horizons)
- farthest row 1900-12-31: A Ring outer edge 34.13″ by 14.83″; C Ring inner edge 18.59″ across; the globe 15.04″ (Horizons)

**ring-plane-window-2024-2025-saturn-oslo** (366 rows, 2024-09-01 to 2025-09-01):
- widest opening to the observer: B = +5.230° planetocentric at 2024-11-13 (Horizons' ObsSub-LAT +6.418°)
- ring-plane crossings of the Earth: Horizons 1, ours 1
  - Horizons 2025-03-23 17:51, ours 2025-03-23 17:51 (-0.00 h)
- ring-plane crossings of the Sun: Horizons 1, ours 1
  - Horizons 2025-05-06 15:35, ours 2025-05-06 15:35 (-0.00 h)
- nearest row 2025-09-01: A Ring outer edge 43.81″ by 1.95″; C Ring inner edge 23.86″ across; the globe 19.30″ (Horizons)
- farthest row 2025-03-12: A Ring outer edge 35.57″ by 0.42″; C Ring inner edge 19.37″ across; the globe 15.68″ (Horizons)

**matrix-7d-saturn-geocentric** (10488 rows, 1900-01-01 to 2100-12-27):
- widest opening to the observer: B = -26.995° planetocentric at 2003-04-07 (Horizons' ObsSub-LAT -32.049°)
- ring-plane crossings of the Earth: Horizons 30, ours 30
  - Horizons 1907-04-12 23:23, ours 1907-04-12 23:23 (-0.00 h)
  - Horizons 1907-10-04 00:44, ours 1907-10-04 00:44 (+0.00 h)
  - Horizons 1908-01-07 17:52, ours 1908-01-07 17:52 (+0.00 h)
  - Horizons 1920-11-08 07:19, ours 1920-11-08 07:19 (-0.00 h)
  - Horizons 1921-02-22 03:46, ours 1921-02-22 03:46 (+0.00 h)
  - Horizons 1921-08-04 02:18, ours 1921-08-04 02:18 (+0.00 h)
  - Horizons 1937-02-21 00:36, ours 1937-02-21 00:36 (+0.00 h)
  - Horizons 1950-09-14 11:39, ours 1950-09-14 11:39 (+0.00 h)
  - Horizons 1966-04-02 14:22, ours 1966-04-02 14:22 (+0.00 h)
  - Horizons 1966-10-28 14:03, ours 1966-10-28 14:03 (+0.00 h)
  - Horizons 1966-12-18 08:07, ours 1966-12-18 08:07 (+0.00 h)
  - Horizons 1979-10-27 08:20, ours 1979-10-27 08:20 (+0.00 h)
  - Horizons 1980-03-12 09:23, ours 1980-03-12 09:23 (+0.00 h)
  - Horizons 1980-07-23 07:11, ours 1980-07-23 07:11 (+0.00 h)
  - Horizons 1995-05-22 06:44, ours 1995-05-22 06:44 (+0.00 h)
  - Horizons 1995-08-10 17:08, ours 1995-08-10 17:08 (-0.00 h)
  - Horizons 1996-02-12 00:04, ours 1996-02-12 00:04 (-0.00 h)
  - Horizons 2009-09-04 13:32, ours 2009-09-04 13:32 (+0.00 h)
  - Horizons 2025-03-23 17:49, ours 2025-03-23 17:49 (+0.00 h)
  - Horizons 2038-10-15 17:32, ours 2038-10-15 17:32 (-0.00 h)
  - Horizons 2039-04-01 19:49, ours 2039-04-01 19:49 (-0.00 h)
  - Horizons 2039-07-09 15:41, ours 2039-07-09 15:41 (-0.00 h)
  - Horizons 2054-05-06 04:27, ours 2054-05-06 04:27 (+0.00 h)
  - Horizons 2054-08-31 07:57, ours 2054-08-31 07:57 (+0.00 h)
  - Horizons 2055-02-01 08:46, ours 2055-02-01 08:46 (-0.00 h)
  - Horizons 2068-08-25 05:10, ours 2068-08-25 05:10 (+0.00 h)
  - Horizons 2084-03-14 08:12, ours 2084-03-14 08:12 (+0.00 h)
  - Horizons 2097-10-05 04:54, ours 2097-10-05 04:54 (+0.00 h)
  - Horizons 2098-04-26 14:09, ours 2098-04-26 14:09 (+0.00 h)
  - Horizons 2098-06-18 10:58, ours 2098-06-18 10:58 (-0.00 h)
- ring-plane crossings of the Sun: Horizons 14, ours 14
  - Horizons 1907-07-27 03:13, ours 1907-07-27 03:13 (+0.00 h)
  - Horizons 1921-04-12 01:37, ours 1921-04-12 01:37 (+0.00 h)
  - Horizons 1936-12-29 11:46, ours 1936-12-29 11:46 (+0.00 h)
  - Horizons 1950-09-21 20:29, ours 1950-09-21 20:29 (+0.00 h)
  - Horizons 1966-06-16 11:17, ours 1966-06-16 11:17 (+0.00 h)
  - Horizons 1980-03-03 17:23, ours 1980-03-03 17:23 (-0.00 h)
  - Horizons 1995-11-19 15:42, ours 1995-11-19 15:42 (+0.00 h)
  - Horizons 2009-08-11 03:30, ours 2009-08-11 03:30 (-0.00 h)
  - Horizons 2025-05-06 15:34, ours 2025-05-06 15:34 (+0.00 h)
  - Horizons 2039-01-22 21:05, ours 2039-01-22 21:05 (-0.00 h)
  - Horizons 2054-10-10 08:30, ours 2054-10-10 08:30 (-0.00 h)
  - Horizons 2068-06-29 10:07, ours 2068-06-29 10:07 (-0.00 h)
  - Horizons 2084-03-27 11:51, ours 2084-03-27 11:51 (+0.00 h)
  - Horizons 2097-12-13 09:14, ours 2097-12-13 09:14 (+0.00 h)
- nearest approaches (Horizons' range rate through zero): 194, first 1900-06-25, last 2100-04-19
- nearest row 1914-12-21: A Ring outer edge 46.97″ by 20.89″; C Ring inner edge 25.58″ across; the globe 20.70″ (Horizons)
- farthest row 1900-12-31: A Ring outer edge 34.13″ by 14.83″; C Ring inner edge 18.59″ across; the globe 15.04″ (Horizons)

## Mimas: worst absolute difference by era

| era | rows | astrometric″ | apparent″ | horizontal″ | distance km | X apparent″ | Y apparent″ | X astrometric″ | Y astrometric″ | position angle° | separation″ | diameter, largest radius″ | diameter, mean radius″ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | 6475 | +0.0002 | +0.6106 | +1.0086 | -1.2836 | +0.0034 | -0.0034 | -0.1305 | +0.2947 | +0.0168 | -0.0029 | +0.0000 | -0.0032 |
| 1962-1971 | 1049 | +0.0042 | +0.5345 | +2.0078 | +24.5522 | +0.0025 | +0.0020 | +0.0362 | -0.0826 | +0.1465 | -0.0030 | +0.0000 | -0.0032 |
| 1972-2027-06 | 6180 | +0.0000 | +0.7001 | +2.7740 | +0.0538 | +0.0055 | +0.0052 | -0.0392 | +0.0850 | -0.0196 | +0.0034 | +0.0000 | -0.0032 |
| after | 7677 | +0.0017 | +1.2229 | +405.5228 | +10.2882 | -0.0154 | -0.0049 | -0.1435 | +0.3052 | -0.0427 | +0.0120 | -0.0000 | -0.0032 |

## Enceladus: worst absolute difference by era

| era | rows | astrometric″ | apparent″ | horizontal″ | distance km | X apparent″ | Y apparent″ | X astrometric″ | Y astrometric″ | position angle° | separation″ | diameter, largest radius″ | diameter, mean radius″ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | 6475 | +0.0002 | +0.6151 | +1.0086 | -1.1920 | +0.0031 | -0.0071 | +0.1683 | +0.3704 | -0.0093 | -0.0061 | -0.0000 | -0.0015 |
| 1962-1971 | 1049 | +0.0039 | +0.5372 | +2.0079 | +24.2357 | -0.0049 | -0.0023 | -0.0436 | +0.1074 | -0.0243 | +0.0046 | -0.0000 | -0.0015 |
| 1972-2027-06 | 6180 | +0.0000 | +0.7162 | +2.7740 | +0.0535 | -0.0092 | -0.0044 | -0.0494 | -0.1081 | +0.0130 | +0.0095 | -0.0000 | -0.0015 |
| after | 7677 | +0.0017 | +1.1960 | +405.5249 | +10.2881 | +0.0106 | -0.0138 | +0.1737 | -0.3878 | +0.0556 | -0.0042 | +0.0000 | -0.0015 |

## Tethys: worst absolute difference by era

| era | rows | astrometric″ | apparent″ | horizontal″ | distance km | X apparent″ | Y apparent″ | X astrometric″ | Y astrometric″ | position angle° | separation″ | diameter, largest radius″ | diameter, mean radius″ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | 6475 | +0.0002 | +0.6133 | +1.0086 | -1.2017 | +0.0045 | +0.0026 | +0.2037 | -0.4614 | +0.0178 | -0.0046 | -0.0000 | -0.0025 |
| 1962-1971 | 1049 | +0.0042 | +0.5366 | +2.0080 | +24.6399 | -0.0048 | +0.0026 | -0.0535 | -0.1325 | +0.0262 | -0.0046 | -0.0000 | -0.0025 |
| 1972-2027-06 | 6180 | +0.0000 | +0.7196 | +2.7740 | +0.0538 | -0.0117 | -0.0063 | +0.0628 | +0.1332 | +0.0132 | +0.0119 | -0.0000 | -0.0025 |
| after | 7677 | +0.0017 | +1.1735 | +405.5245 | +10.2881 | +0.0337 | -0.0047 | +0.2227 | -0.4797 | +0.0116 | +0.0339 | -0.0000 | -0.0025 |

## Dione: worst absolute difference by era

| era | rows | astrometric″ | apparent″ | horizontal″ | distance km | X apparent″ | Y apparent″ | X astrometric″ | Y astrometric″ | position angle° | separation″ | diameter, largest radius″ | diameter, mean radius″ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | 6475 | +0.0002 | +0.6147 | +1.0086 | -1.1743 | +0.0062 | +0.0114 | +0.2663 | -0.5870 | -0.0168 | +0.0070 | +0.0000 | -0.0007 |
| 1962-1971 | 1049 | +0.0037 | +0.5272 | +2.0079 | +22.4260 | +0.0058 | -0.0019 | -0.0685 | +0.1702 | +0.0195 | -0.0059 | +0.0000 | -0.0007 |
| 1972-2027-06 | 6180 | +0.0000 | +0.6911 | +2.7737 | +0.0536 | +0.0134 | +0.0103 | +0.0779 | -0.1725 | -0.0175 | +0.0115 | +0.0000 | -0.0007 |
| after | 7677 | +0.0017 | +1.2383 | +405.5251 | +10.2883 | -0.0294 | +0.0245 | +0.2787 | +0.6153 | +0.0541 | +0.0123 | +0.0000 | -0.0007 |

## Rhea: worst absolute difference by era

| era | rows | astrometric″ | apparent″ | horizontal″ | distance km | X apparent″ | Y apparent″ | X astrometric″ | Y astrometric″ | position angle° | separation″ | diameter, largest radius″ | diameter, mean radius″ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | 6475 | +0.0002 | +0.6296 | +1.0086 | -1.0959 | -0.0050 | -0.0214 | +0.3737 | +0.8225 | -0.0156 | -0.0082 | +0.0000 | -0.0005 |
| 1962-1971 | 1049 | +0.0039 | +0.5412 | +2.0079 | -21.8126 | -0.0097 | -0.0033 | -0.0938 | -0.2334 | +0.0219 | -0.0079 | -0.0000 | -0.0005 |
| 1972-2027-06 | 6180 | +0.0000 | +0.6848 | +2.7735 | +0.0536 | +0.0196 | +0.0128 | +0.1088 | -0.2384 | -0.0171 | +0.0173 | -0.0000 | -0.0005 |
| after | 7677 | +0.0017 | +1.2832 | +405.5247 | +10.2885 | -0.0747 | +0.0204 | +0.3883 | -0.8674 | +0.0186 | +0.0718 | +0.0000 | -0.0005 |

## Titan: worst absolute difference by era

| era | rows | astrometric″ | apparent″ | horizontal″ | distance km | X apparent″ | Y apparent″ | X astrometric″ | Y astrometric″ | position angle° | separation″ | diameter, largest radius″ | diameter, mean radius″ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | 6475 | +0.0002 | +0.6065 | +1.0087 | -1.0698 | +0.0158 | +0.0471 | +0.8779 | -1.9064 | -0.0260 | -0.0193 | +0.0000 | -0.0001 |
| 1962-1971 | 1049 | +0.0037 | +0.5139 | +2.0077 | +20.4912 | +0.0196 | +0.0094 | -0.2230 | -0.5494 | -0.0135 | -0.0192 | -0.0000 | -0.0001 |
| 1972-2027-06 | 6180 | +0.0000 | +0.6844 | +2.7755 | +0.0536 | -0.0155 | +0.0305 | -0.2510 | -0.5576 | -0.0256 | +0.0221 | +0.0000 | -0.0001 |
| after | 7677 | +0.0017 | +1.0615 | +405.5311 | +10.2873 | +0.1452 | +0.0258 | -0.8841 | +2.0312 | +0.0182 | +0.1443 | -0.0000 | -0.0001 |

## Hyperion: worst absolute difference by era

| era | rows | astrometric″ | apparent″ | horizontal″ | distance km | X apparent″ | Y apparent″ | X astrometric″ | Y astrometric″ | position angle° | separation″ | diameter, largest radius″ | diameter, mean radius″ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | 6475 | +0.0002 | +0.5894 | +1.0085 | -1.0056 | +0.0230 | +0.0198 | -1.1182 | -2.4213 | +0.0148 | +0.0297 | +0.0000 | -0.0143 |
| 1962-1971 | 1049 | +0.0036 | +0.5563 | +2.0082 | -18.9939 | -0.0259 | -0.0151 | +0.2607 | -0.6718 | -0.0130 | +0.0231 | -0.0000 | -0.0142 |
| 1972-2027-06 | 6180 | +0.0000 | +0.6546 | +2.7737 | +0.0537 | +0.0483 | +0.0239 | +0.3067 | +0.7099 | -0.0170 | +0.0480 | -0.0000 | -0.0142 |
| after | 7677 | +0.0017 | +1.4228 | +405.5267 | +10.2871 | -0.2125 | +0.0524 | -1.2310 | +2.5250 | +0.0158 | +0.2078 | +0.0000 | -0.0142 |

## Iapetus: worst absolute difference by era

| era | rows | astrometric″ | apparent″ | horizontal″ | distance km | X apparent″ | Y apparent″ | X astrometric″ | Y astrometric″ | position angle° | separation″ | diameter, largest radius″ | diameter, mean radius″ |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-1961 | 6475 | +0.0002 | +0.5788 | +1.0085 | -1.0119 | +0.0318 | +0.1077 | +1.8039 | -5.6656 | -0.0378 | -0.0562 | +0.0000 | -0.0039 |
| 1962-1971 | 1049 | +0.0034 | +0.5005 | +2.0073 | +18.7049 | +0.0403 | -0.0361 | +0.5796 | -1.6154 | +0.0300 | -0.0559 | -0.0000 | -0.0038 |
| 1972-2027-06 | 6180 | +0.0000 | +0.6940 | +2.7724 | +0.0537 | -0.0378 | -0.0629 | +0.5616 | -1.6093 | +0.0364 | -0.0565 | -0.0000 | -0.0039 |
| after | 7677 | +0.0017 | +0.8709 | +405.4974 | +10.2889 | +0.3344 | -0.0731 | -1.8147 | +5.8475 | +0.0398 | +0.3219 | +0.0000 | -0.0039 |

## Visibility states against Horizons' codes

Ours on Horizons' definition: limb to limb on Saturn's equatorial sphere, each moon's largest PCK radius, light time considered; the shadow at the moon's emission epoch, Saturn's equatorial-sphere umbra. *Figure differs* counts rows where the apparent oblate figure (the PCK pole and polar radius) gives a different state. Horizons' codes take no account of the rings.

| moon | agreeing | disagreeing | figure differs | Horizons' codes |
|---|---:|---:|---:|---|
| mimas | 21381 | 0 | 159 | * 19362, O 121, P 6, U 811, p 4, t 972, u 105 |
| enceladus | 21381 | 0 | 93 | * 20221, O 83, P 6, U 451, p 6, t 544, u 70 |
| tethys | 21381 | 0 | 59 | * 20573, O 68, P 7, U 293, p 4, t 366, u 70 |
| dione | 21381 | 0 | 32 | * 20899, O 48, P 6, U 165, p 2, t 214, u 47 |
| rhea | 21381 | 0 | 24 | * 21129, O 41, P 2, U 50, t 108, u 51 |
| titan | 21381 | 0 | 4 | * 21325, O 14, U 8, t 24, u 10 |
| hyperion | 21381 | 0 | 4 | * 21359, O 4, t 12, u 6 |
| iapetus | 21381 | 0 | 4 | * 21369, O 4, p 2, t 6 |

## Observational relevance, from the weekly Oslo matrix 1900-2100

*Behind the rings* counts rows where the sight line to a moon meets the ring plane between the observer and the moon, between 74,491 and 136,770 km (the C Ring's inner and the A Ring's outer boundary); *before the rings*, rows where the line meets them beyond the moon, so that the moon is seen against them. The rings are taken as a plane with no thickness and no gaps. This is geometry only: no Horizons code or published authority checks it.

| moon | rows | separation from Saturn″ (min-max) | diameter″ (min-max) | behind the rings | before the rings | Horizons' codes |
|---|---:|---|---|---:|---:|---|
| mimas | 10488 | 0.3-32.2 | 0.0519-0.0714 | 66 | 73 | * 9516, O 59, P 3, U 390, p 2, t 468, u 50 |
| enceladus | 10488 | 0.0-40.9 | 0.0640-0.0881 | 0 | 0 | * 9935, O 38, P 3, U 215, p 3, t 260, u 34 |
| tethys | 10488 | 0.6-50.4 | 0.1344-0.1848 | 13 | 13 | * 10108, O 30, P 3, U 137, p 2, t 173, u 35 |
| dione | 10488 | 0.5-64.6 | 0.1406-0.1934 | 0 | 0 | * 10261, O 23, P 3, U 77, p 1, t 101, u 22 |
| rhea | 10488 | 0.0-90.4 | 0.1909-0.2627 | 1 | 1 | * 10367, O 19, P 1, U 24, t 52, u 25 |
| titan | 10488 | 1.8-213.0 | 0.6424-0.8849 | 0 | 0 | * 10460, O 7, U 4, t 12, u 5 |
| hyperion | 10488 | 2.3-283.2 | 0.0449-0.0619 | 0 | 0 | * 10477, O 2, t 6, u 3 |
| iapetus | 10488 | 7.9-625.5 | 0.1857-0.2567 | 0 | 3 | * 10482, O 2, p 1, t 3 |

## The planetary ephemeris, separated from sat441

Weekly over 1900-2100 (TDB): DE442s (`de442s.bsp`) against the released DE440 (`de440s.bsp`), each carrying Saturn's centre by sat441's own 6 -> 699; then sat441's barycentre and Earth against de440s's; then the size of the centre offset itself and the direction it moves Saturn by, seen from the Earth.

| quantity | 1900-1949 | 1950-1999 | 2000-2049 | 2050-2100 |
|---|---:|---:|---:|---:|
| barycentre km | 0.8646 | 0.4475 | 0.8725 | 1.497 |
| earth km | 0.3661 | 0.2501 | 0.2058 | 0.4946 |
| direction″ | 0.0001602 | 7.619e-05 | 0.0001071 | 0.0001953 |
| distance km | 0.3831 | 0.1209 | 0.1744 | 0.6579 |
| sat441 barycentre vs de440s km | 0 | 0 | 0 | 0 |
| sat441 earth vs de440s km | 0 | 0 | 0 | 0 |
| centre offset km | 311.2 | 310.8 | 311.8 | 311.5 |
| centre vs barycentre direction″ | 0.05117 | 0.05185 | 0.05183 | 0.05211 |

## Titan, hourly at Oslo, 2026-08-01 to 2026-08-17: greatest elongations

- Horizons 2026-08-01 18:00 189.79″; ours 189.81″ (+0.017″)
- Horizons 2026-08-09 20:00 187.62″; ours 187.64″ (+0.016″)

## Where the worst rows are

- dione, 1900-1961, X apparent″: +0.00619 at matrix-7d-dione-geocentric 1918-08-12 00:00
- dione, 1900-1961, X astrometric″: +0.26635 at matrix-7d-dione-geocentric 1900-07-02 00:00
- dione, 1900-1961, Y apparent″: +0.01136 at matrix-7d-dione-oslo 1931-01-05 00:00
- dione, 1900-1961, Y astrometric″: -0.58702 at matrix-7d-dione-geocentric 1900-06-11 00:00
- dione, 1900-1961, apparent″: +0.61467 at matrix-7d-dione-oslo 1932-01-18 00:00
- dione, 1900-1961, astrometric″: +0.00019 at named-dione-alert 1900-01-01 00:00
- dione, 1900-1961, diameter, largest radius″: +0.00000 at matrix-7d-dione-oslo 1918-04-29 00:00
- dione, 1900-1961, diameter, mean radius″: -0.00068 at matrix-7d-dione-oslo 1914-12-21 00:00
- dione, 1900-1961, distance km: -1.17426 at matrix-7d-dione-oslo 1900-10-01 00:00
- dione, 1900-1961, horizontal″: +1.00858 at named-dione-alert 1900-01-01 00:00
- dione, 1900-1961, position angle°: -0.01680 at matrix-7d-dione-oslo 1931-01-05 00:00
- dione, 1900-1961, separation″: +0.00696 at matrix-7d-dione-geocentric 1931-01-05 00:00
- dione, 1962-1971, X apparent″: +0.00581 at matrix-7d-dione-geocentric 1962-01-22 00:00
- dione, 1962-1971, X astrometric″: -0.06851 at matrix-7d-dione-oslo 1971-10-25 00:00
- dione, 1962-1971, Y apparent″: -0.00191 at matrix-7d-dione-oslo 1963-02-04 00:00
- dione, 1962-1971, Y astrometric″: +0.17016 at matrix-7d-dione-geocentric 1962-09-03 00:00
- dione, 1962-1971, apparent″: +0.52722 at matrix-7d-dione-oslo 1962-01-22 00:00
- dione, 1962-1971, astrometric″: +0.00373 at matrix-7d-dione-oslo 1965-03-29 00:00
- dione, 1962-1971, diameter, largest radius″: +0.00000 at matrix-7d-dione-oslo 1965-11-15 00:00
- dione, 1962-1971, diameter, mean radius″: -0.00067 at matrix-7d-dione-oslo 1971-11-22 00:00
- dione, 1962-1971, distance km: +22.42602 at matrix-7d-dione-oslo 1965-07-12 00:00
- dione, 1962-1971, horizontal″: +2.00792 at matrix-7d-dione-oslo 1971-12-27 00:00
- dione, 1962-1971, position angle°: +0.01950 at matrix-7d-dione-geocentric 1967-02-13 00:00
- dione, 1962-1971, separation″: -0.00586 at matrix-7d-dione-oslo 1970-01-19 00:00
- dione, 1972-2027-06, X apparent″: +0.01341 at matrix-7d-dione-geocentric 2020-01-13 00:00
- dione, 1972-2027-06, X astrometric″: +0.07795 at matrix-7d-dione-oslo 1972-10-30 00:00
- dione, 1972-2027-06, Y apparent″: +0.01025 at matrix-7d-dione-oslo 2020-01-13 00:00
- dione, 1972-2027-06, Y astrometric″: -0.17248 at matrix-7d-dione-geocentric 1972-12-04 00:00
- dione, 1972-2027-06, apparent″: +0.69114 at matrix-7d-dione-geocentric 2020-01-13 00:00
- dione, 1972-2027-06, astrometric″: +0.00001 at named-dione-cape-town 2026-10-19 12:00
- dione, 1972-2027-06, diameter, largest radius″: +0.00000 at matrix-7d-dione-oslo 2003-01-27 00:00
- dione, 1972-2027-06, diameter, mean radius″: -0.00068 at matrix-7d-dione-oslo 1973-12-24 00:00
- dione, 1972-2027-06, distance km: +0.05361 at named-dione-quito 2026-10-19 12:00
- dione, 1972-2027-06, horizontal″: +2.77372 at matrix-7d-dione-oslo 2027-04-05 00:00
- dione, 1972-2027-06, position angle°: -0.01746 at matrix-7d-dione-geocentric 2020-01-13 00:00
- dione, 1972-2027-06, separation″: +0.01153 at matrix-7d-dione-geocentric 2020-01-13 00:00
- dione, after, X apparent″: -0.02937 at matrix-7d-dione-oslo 2034-07-17 00:00
- dione, after, X astrometric″: +0.27866 at matrix-7d-dione-oslo 2092-01-28 00:00
- dione, after, Y apparent″: +0.02454 at matrix-7d-dione-geocentric 2034-07-17 00:00
- dione, after, Y astrometric″: +0.61530 at matrix-7d-dione-oslo 2092-01-07 00:00
- dione, after, apparent″: +1.23835 at matrix-7d-dione-oslo 2034-07-17 00:00
- dione, after, astrometric″: +0.00168 at named-dione-quito 2100-12-31 23:59
- dione, after, diameter, largest radius″: +0.00000 at matrix-7d-dione-geocentric 2048-09-14 00:00
- dione, after, diameter, mean radius″: -0.00068 at matrix-7d-dione-oslo 2032-12-27 00:00
- dione, after, distance km: +10.28828 at named-dione-cape-town 2100-12-31 23:59
- dione, after, horizontal″: +405.52512 at matrix-7d-dione-oslo 2100-08-23 00:00
- dione, after, position angle°: +0.05411 at matrix-7d-dione-oslo 2034-07-17 00:00
- dione, after, separation″: +0.01230 at matrix-7d-dione-oslo 2034-07-17 00:00
- enceladus, 1900-1961, X apparent″: +0.00305 at matrix-7d-enceladus-oslo 1931-01-05 00:00
- enceladus, 1900-1961, X astrometric″: +0.16828 at matrix-7d-enceladus-oslo 1900-06-04 00:00
- enceladus, 1900-1961, Y apparent″: -0.00707 at matrix-7d-enceladus-geocentric 1931-01-05 00:00
- enceladus, 1900-1961, Y astrometric″: +0.37035 at matrix-7d-enceladus-geocentric 1900-07-23 00:00
- enceladus, 1900-1961, apparent″: +0.61506 at matrix-7d-enceladus-geocentric 1931-01-05 00:00
- enceladus, 1900-1961, astrometric″: +0.00019 at named-enceladus-quito 1900-01-01 00:00
- enceladus, 1900-1961, diameter, largest radius″: -0.00000 at matrix-7d-enceladus-oslo 1932-12-12 00:00
- enceladus, 1900-1961, diameter, mean radius″: -0.00155 at matrix-7d-enceladus-geocentric 1914-12-21 00:00
- enceladus, 1900-1961, distance km: -1.19195 at matrix-7d-enceladus-oslo 1902-10-13 00:00
- enceladus, 1900-1961, horizontal″: +1.00859 at named-enceladus-alert 1900-01-01 00:00
- enceladus, 1900-1961, position angle°: -0.00925 at matrix-7d-enceladus-geocentric 1931-01-05 00:00
- enceladus, 1900-1961, separation″: -0.00605 at matrix-7d-enceladus-oslo 1931-01-05 00:00
- enceladus, 1962-1971, X apparent″: -0.00493 at matrix-7d-enceladus-geocentric 1962-01-22 00:00
- enceladus, 1962-1971, X astrometric″: -0.04358 at matrix-7d-enceladus-geocentric 1971-11-01 00:00
- enceladus, 1962-1971, Y apparent″: -0.00234 at matrix-7d-enceladus-oslo 1963-02-04 00:00
- enceladus, 1962-1971, Y astrometric″: +0.10744 at matrix-7d-enceladus-geocentric 1962-08-27 00:00
- enceladus, 1962-1971, apparent″: +0.53725 at matrix-7d-enceladus-oslo 1962-01-22 00:00
- enceladus, 1962-1971, astrometric″: +0.00387 at matrix-7d-enceladus-geocentric 1965-04-05 00:00
- enceladus, 1962-1971, diameter, largest radius″: -0.00000 at matrix-7d-enceladus-oslo 1970-06-29 00:00
- enceladus, 1962-1971, diameter, mean radius″: -0.00153 at matrix-7d-enceladus-oslo 1971-11-29 00:00
- enceladus, 1962-1971, distance km: +24.23575 at matrix-7d-enceladus-oslo 1965-07-05 00:00
- enceladus, 1962-1971, horizontal″: +2.00788 at matrix-7d-enceladus-oslo 1971-12-27 00:00
- enceladus, 1962-1971, position angle°: -0.02428 at matrix-7d-enceladus-geocentric 1965-08-16 00:00
- enceladus, 1962-1971, separation″: +0.00460 at matrix-7d-enceladus-geocentric 1962-01-22 00:00
- enceladus, 1972-2027-06, X apparent″: -0.00916 at matrix-7d-enceladus-geocentric 2020-01-13 00:00
- enceladus, 1972-2027-06, X astrometric″: -0.04941 at matrix-7d-enceladus-geocentric 1973-12-17 00:00
- enceladus, 1972-2027-06, Y apparent″: -0.00442 at matrix-7d-enceladus-oslo 1988-12-26 00:00
- enceladus, 1972-2027-06, Y astrometric″: -0.10811 at matrix-7d-enceladus-geocentric 1972-12-11 00:00
- enceladus, 1972-2027-06, apparent″: +0.71619 at matrix-7d-enceladus-geocentric 2020-01-13 00:00
- enceladus, 1972-2027-06, astrometric″: +0.00001 at matrix-7d-enceladus-oslo 2027-03-01 00:00
- enceladus, 1972-2027-06, diameter, largest radius″: -0.00000 at matrix-7d-enceladus-geocentric 2024-12-16 00:00
- enceladus, 1972-2027-06, diameter, mean radius″: -0.00154 at matrix-7d-enceladus-geocentric 1973-12-24 00:00
- enceladus, 1972-2027-06, distance km: +0.05355 at named-enceladus-quito 2026-10-19 12:00
- enceladus, 1972-2027-06, horizontal″: +2.77402 at matrix-7d-enceladus-oslo 2027-04-05 00:00
- enceladus, 1972-2027-06, position angle°: +0.01297 at matrix-7d-enceladus-oslo 1974-07-01 00:00
- enceladus, 1972-2027-06, separation″: +0.00952 at matrix-7d-enceladus-oslo 2020-01-13 00:00
- enceladus, after, X apparent″: +0.01060 at matrix-7d-enceladus-oslo 2034-07-17 00:00
- enceladus, after, X astrometric″: +0.17372 at matrix-7d-enceladus-oslo 2092-02-11 00:00
- enceladus, after, Y apparent″: -0.01382 at matrix-7d-enceladus-oslo 2034-07-17 00:00
- enceladus, after, Y astrometric″: -0.38784 at matrix-7d-enceladus-oslo 2092-01-28 00:00
- enceladus, after, apparent″: +1.19595 at matrix-7d-enceladus-oslo 2034-07-17 00:00
- enceladus, after, astrometric″: +0.00168 at named-enceladus-quito 2100-12-31 23:59
- enceladus, after, diameter, largest radius″: +0.00000 at matrix-7d-enceladus-oslo 2065-08-24 00:00
- enceladus, after, diameter, mean radius″: -0.00155 at matrix-7d-enceladus-geocentric 2091-12-31 00:00
- enceladus, after, distance km: +10.28808 at named-enceladus-cape-town 2100-12-31 23:59
- enceladus, after, horizontal″: +405.52493 at matrix-7d-enceladus-oslo 2100-08-23 00:00
- enceladus, after, position angle°: +0.05561 at matrix-7d-enceladus-oslo 2034-07-17 00:00
- enceladus, after, separation″: -0.00421 at matrix-7d-enceladus-geocentric 2078-01-03 00:00
- hyperion, 1900-1961, X apparent″: +0.02298 at matrix-7d-hyperion-geocentric 1931-01-05 00:00
- hyperion, 1900-1961, X astrometric″: -1.11815 at matrix-7d-hyperion-geocentric 1900-06-25 00:00
- hyperion, 1900-1961, Y apparent″: +0.01981 at matrix-7d-hyperion-geocentric 1931-01-05 00:00
- hyperion, 1900-1961, Y astrometric″: -2.42129 at matrix-7d-hyperion-geocentric 1901-07-29 00:00
- hyperion, 1900-1961, apparent″: +0.58935 at matrix-7d-hyperion-oslo 1932-01-18 00:00
- hyperion, 1900-1961, astrometric″: +0.00016 at matrix-7d-hyperion-oslo 1900-01-08 00:00
- hyperion, 1900-1961, diameter, largest radius″: +0.00000 at matrix-7d-hyperion-oslo 1920-09-20 00:00
- hyperion, 1900-1961, diameter, mean radius″: -0.01425 at matrix-7d-hyperion-geocentric 1914-12-28 00:00
- hyperion, 1900-1961, distance km: -1.00555 at matrix-7d-hyperion-oslo 1901-09-30 00:00
- hyperion, 1900-1961, horizontal″: +1.00852 at named-hyperion-alert 1900-01-01 00:00
- hyperion, 1900-1961, position angle°: +0.01479 at matrix-7d-hyperion-oslo 1944-01-17 00:00
- hyperion, 1900-1961, separation″: +0.02971 at matrix-7d-hyperion-geocentric 1931-01-05 00:00
- hyperion, 1962-1971, X apparent″: -0.02591 at matrix-7d-hyperion-geocentric 1962-01-22 00:00
- hyperion, 1962-1971, X astrometric″: +0.26067 at matrix-7d-hyperion-geocentric 1971-10-11 00:00
- hyperion, 1962-1971, Y apparent″: -0.01510 at matrix-7d-hyperion-oslo 1963-02-04 00:00
- hyperion, 1962-1971, Y astrometric″: -0.67176 at matrix-7d-hyperion-geocentric 1962-09-10 00:00
- hyperion, 1962-1971, apparent″: +0.55629 at matrix-7d-hyperion-oslo 1962-01-22 00:00
- hyperion, 1962-1971, astrometric″: +0.00357 at matrix-7d-hyperion-oslo 1965-03-01 00:00
- hyperion, 1962-1971, diameter, largest radius″: -0.00000 at matrix-7d-hyperion-oslo 1969-12-01 00:00
- hyperion, 1962-1971, diameter, mean radius″: -0.01416 at matrix-7d-hyperion-geocentric 1971-11-22 00:00
- hyperion, 1962-1971, distance km: -18.99395 at matrix-7d-hyperion-oslo 1965-11-22 00:00
- hyperion, 1962-1971, horizontal″: +2.00817 at matrix-7d-hyperion-oslo 1971-12-27 00:00
- hyperion, 1962-1971, position angle°: -0.01301 at matrix-7d-hyperion-geocentric 1971-10-18 00:00
- hyperion, 1962-1971, separation″: +0.02308 at matrix-7d-hyperion-oslo 1970-08-17 00:00
- hyperion, 1972-2027-06, X apparent″: +0.04832 at matrix-7d-hyperion-oslo 2020-01-13 00:00
- hyperion, 1972-2027-06, X astrometric″: +0.30673 at matrix-7d-hyperion-oslo 1972-11-20 00:00
- hyperion, 1972-2027-06, Y apparent″: +0.02394 at matrix-7d-hyperion-geocentric 1988-12-26 00:00
- hyperion, 1972-2027-06, Y astrometric″: +0.70985 at matrix-7d-hyperion-geocentric 1972-11-27 00:00
- hyperion, 1972-2027-06, apparent″: +0.65456 at matrix-7d-hyperion-geocentric 2020-01-13 00:00
- hyperion, 1972-2027-06, astrometric″: +0.00001 at named-hyperion-cape-town 2026-10-19 12:00
- hyperion, 1972-2027-06, diameter, largest radius″: -0.00000 at matrix-7d-hyperion-geocentric 2026-08-17 00:00
- hyperion, 1972-2027-06, diameter, mean radius″: -0.01425 at matrix-7d-hyperion-geocentric 1973-12-31 00:00
- hyperion, 1972-2027-06, distance km: +0.05366 at named-hyperion-quito 2026-10-19 12:00
- hyperion, 1972-2027-06, horizontal″: +2.77375 at matrix-7d-hyperion-oslo 2027-04-05 00:00
- hyperion, 1972-2027-06, position angle°: -0.01701 at matrix-7d-hyperion-geocentric 1988-12-26 00:00
- hyperion, 1972-2027-06, separation″: +0.04803 at matrix-7d-hyperion-geocentric 2020-01-13 00:00
- hyperion, after, X apparent″: -0.21252 at matrix-7d-hyperion-oslo 2034-07-17 00:00
- hyperion, after, X astrometric″: -1.23098 at matrix-7d-hyperion-oslo 2092-01-07 00:00
- hyperion, after, Y apparent″: +0.05243 at matrix-7d-hyperion-geocentric 2034-07-17 00:00
- hyperion, after, Y astrometric″: +2.52502 at matrix-7d-hyperion-oslo 2093-01-19 00:00
- hyperion, after, apparent″: +1.42283 at matrix-7d-hyperion-oslo 2034-07-17 00:00
- hyperion, after, astrometric″: +0.00168 at named-hyperion-quito 2100-12-31 23:59
- hyperion, after, diameter, largest radius″: +0.00000 at matrix-7d-hyperion-geocentric 2045-07-03 00:00
- hyperion, after, diameter, mean radius″: -0.01425 at matrix-7d-hyperion-geocentric 2090-12-18 00:00
- hyperion, after, distance km: +10.28707 at named-hyperion-cape-town 2100-12-31 23:59
- hyperion, after, horizontal″: +405.52673 at matrix-7d-hyperion-oslo 2100-08-23 00:00
- hyperion, after, position angle°: +0.01580 at matrix-7d-hyperion-oslo 2093-01-19 00:00
- hyperion, after, separation″: +0.20784 at matrix-7d-hyperion-oslo 2034-07-17 00:00
- iapetus, 1900-1961, X apparent″: +0.03176 at matrix-7d-iapetus-oslo 1905-02-13 00:00
- iapetus, 1900-1961, X astrometric″: +1.80391 at matrix-7d-iapetus-oslo 1913-12-29 00:00
- iapetus, 1900-1961, Y apparent″: +0.10774 at matrix-7d-iapetus-oslo 1931-01-05 00:00
- iapetus, 1900-1961, Y astrometric″: -5.66557 at matrix-7d-iapetus-oslo 1900-07-02 00:00
- iapetus, 1900-1961, apparent″: +0.57876 at matrix-7d-iapetus-oslo 1932-01-18 00:00
- iapetus, 1900-1961, astrometric″: +0.00016 at matrix-7d-iapetus-oslo 1900-12-31 00:00
- iapetus, 1900-1961, diameter, largest radius″: +0.00000 at matrix-7d-iapetus-oslo 1961-12-11 00:00
- iapetus, 1900-1961, diameter, mean radius″: -0.00385 at matrix-7d-iapetus-geocentric 1916-01-03 00:00
- iapetus, 1900-1961, distance km: -1.01195 at matrix-7d-iapetus-oslo 1900-09-24 00:00
- iapetus, 1900-1961, horizontal″: +1.00851 at named-iapetus-alert 1900-01-01 00:00
- iapetus, 1900-1961, position angle°: -0.03778 at matrix-7d-iapetus-oslo 1931-01-05 00:00
- iapetus, 1900-1961, separation″: -0.05622 at matrix-7d-iapetus-oslo 1944-02-28 00:00
- iapetus, 1962-1971, X apparent″: +0.04033 at matrix-7d-iapetus-oslo 1963-02-04 00:00
- iapetus, 1962-1971, X astrometric″: +0.57956 at matrix-7d-iapetus-oslo 1971-10-04 00:00
- iapetus, 1962-1971, Y apparent″: -0.03611 at matrix-7d-iapetus-geocentric 1963-02-04 00:00
- iapetus, 1962-1971, Y astrometric″: -1.61536 at matrix-7d-iapetus-geocentric 1962-08-27 00:00
- iapetus, 1962-1971, apparent″: +0.50049 at matrix-7d-iapetus-oslo 1962-01-22 00:00
- iapetus, 1962-1971, astrometric″: +0.00344 at matrix-7d-iapetus-oslo 1965-03-01 00:00
- iapetus, 1962-1971, diameter, largest radius″: -0.00000 at matrix-7d-iapetus-oslo 1963-06-03 00:00
- iapetus, 1962-1971, diameter, mean radius″: -0.00381 at matrix-7d-iapetus-oslo 1971-11-29 00:00
- iapetus, 1962-1971, distance km: +18.70494 at matrix-7d-iapetus-geocentric 1965-05-24 00:00
- iapetus, 1962-1971, horizontal″: +2.00732 at matrix-7d-iapetus-oslo 1971-12-27 00:00
- iapetus, 1962-1971, position angle°: +0.02998 at matrix-7d-iapetus-oslo 1971-12-13 00:00
- iapetus, 1962-1971, separation″: -0.05589 at matrix-7d-iapetus-oslo 1971-01-25 00:00
- iapetus, 1972-2027-06, X apparent″: -0.03780 at matrix-7d-iapetus-oslo 1990-01-08 00:00
- iapetus, 1972-2027-06, X astrometric″: +0.56156 at matrix-7d-iapetus-oslo 1972-11-06 00:00
- iapetus, 1972-2027-06, Y apparent″: -0.06286 at matrix-7d-iapetus-oslo 2021-01-25 00:00
- iapetus, 1972-2027-06, Y astrometric″: -1.60929 at matrix-7d-iapetus-geocentric 1973-12-03 00:00
- iapetus, 1972-2027-06, apparent″: +0.69401 at matrix-7d-iapetus-geocentric 2020-01-13 00:00
- iapetus, 1972-2027-06, astrometric″: +0.00001 at named-iapetus-cape-town 2026-10-19 12:00
- iapetus, 1972-2027-06, diameter, largest radius″: -0.00000 at matrix-7d-iapetus-geocentric 1997-07-07 00:00
- iapetus, 1972-2027-06, diameter, mean radius″: -0.00386 at matrix-7d-iapetus-geocentric 1973-12-24 00:00
- iapetus, 1972-2027-06, distance km: +0.05365 at named-iapetus-quito 2026-10-19 12:00
- iapetus, 1972-2027-06, horizontal″: +2.77241 at matrix-7d-iapetus-oslo 2027-04-05 00:00
- iapetus, 1972-2027-06, position angle°: +0.03636 at matrix-7d-iapetus-oslo 1973-12-03 00:00
- iapetus, 1972-2027-06, separation″: -0.05646 at matrix-7d-iapetus-oslo 1975-03-24 00:00
- iapetus, after, X apparent″: +0.33441 at matrix-7d-iapetus-oslo 2034-07-17 00:00
- iapetus, after, X astrometric″: -1.81471 at matrix-7d-iapetus-oslo 2089-11-28 00:00
- iapetus, after, Y apparent″: -0.07313 at matrix-7d-iapetus-geocentric 2034-07-17 00:00
- iapetus, after, Y astrometric″: +5.84752 at matrix-7d-iapetus-geocentric 2092-01-28 00:00
- iapetus, after, apparent″: +0.87086 at matrix-7d-iapetus-oslo 2034-07-17 00:00
- iapetus, after, astrometric″: +0.00168 at named-iapetus-quito 2100-12-31 23:59
- iapetus, after, diameter, largest radius″: +0.00000 at matrix-7d-iapetus-oslo 2038-10-11 00:00
- iapetus, after, diameter, mean radius″: -0.00385 at matrix-7d-iapetus-geocentric 2031-12-15 00:00
- iapetus, after, distance km: +10.28891 at named-iapetus-cape-town 2100-12-31 23:59
- iapetus, after, horizontal″: +405.49744 at matrix-7d-iapetus-oslo 2100-08-30 00:00
- iapetus, after, position angle°: +0.03978 at matrix-7d-iapetus-geocentric 2034-07-17 00:00
- iapetus, after, separation″: +0.32194 at matrix-7d-iapetus-oslo 2034-07-17 00:00
- mimas, 1900-1961, X apparent″: +0.00344 at matrix-7d-mimas-oslo 1931-01-05 00:00
- mimas, 1900-1961, X astrometric″: -0.13045 at matrix-7d-mimas-geocentric 1900-07-16 00:00
- mimas, 1900-1961, Y apparent″: -0.00336 at matrix-7d-mimas-geocentric 1915-06-28 00:00
- mimas, 1900-1961, Y astrometric″: +0.29468 at matrix-7d-mimas-geocentric 1900-06-18 00:00
- mimas, 1900-1961, apparent″: +0.61058 at matrix-7d-mimas-geocentric 1931-01-05 00:00
- mimas, 1900-1961, astrometric″: +0.00021 at named-mimas-alert 1900-01-01 00:00
- mimas, 1900-1961, diameter, largest radius″: +0.00000 at matrix-7d-mimas-oslo 1924-08-25 00:00
- mimas, 1900-1961, diameter, mean radius″: -0.00324 at matrix-7d-mimas-geocentric 1914-12-21 00:00
- mimas, 1900-1961, distance km: -1.28362 at matrix-7d-mimas-oslo 1900-09-17 00:00
- mimas, 1900-1961, horizontal″: +1.00856 at named-mimas-alert 1900-01-01 00:00
- mimas, 1900-1961, position angle°: +0.01677 at matrix-7d-mimas-geocentric 1931-01-05 00:00
- mimas, 1900-1961, separation″: -0.00293 at matrix-7d-mimas-geocentric 1912-01-22 00:00
- mimas, 1962-1971, X apparent″: +0.00253 at matrix-7d-mimas-oslo 1963-02-04 00:00
- mimas, 1962-1971, X astrometric″: +0.03617 at matrix-7d-mimas-geocentric 1971-11-01 00:00
- mimas, 1962-1971, Y apparent″: +0.00199 at matrix-7d-mimas-geocentric 1962-01-22 00:00
- mimas, 1962-1971, Y astrometric″: -0.08260 at matrix-7d-mimas-oslo 1962-09-10 00:00
- mimas, 1962-1971, apparent″: +0.53450 at matrix-7d-mimas-oslo 1962-01-22 00:00
- mimas, 1962-1971, astrometric″: +0.00418 at matrix-7d-mimas-oslo 1965-03-22 00:00
- mimas, 1962-1971, diameter, largest radius″: +0.00000 at matrix-7d-mimas-geocentric 1967-10-23 00:00
- mimas, 1962-1971, diameter, mean radius″: -0.00322 at matrix-7d-mimas-oslo 1971-11-29 00:00
- mimas, 1962-1971, distance km: +24.55223 at matrix-7d-mimas-geocentric 1965-06-14 00:00
- mimas, 1962-1971, horizontal″: +2.00784 at matrix-7d-mimas-oslo 1971-12-27 00:00
- mimas, 1962-1971, position angle°: +0.14651 at matrix-7d-mimas-geocentric 1966-09-12 00:00
- mimas, 1962-1971, separation″: -0.00295 at matrix-7d-mimas-oslo 1969-01-06 00:00
- mimas, 1972-2027-06, X apparent″: +0.00553 at matrix-7d-mimas-geocentric 2020-01-13 00:00
- mimas, 1972-2027-06, X astrometric″: -0.03923 at matrix-7d-mimas-geocentric 1972-11-20 00:00
- mimas, 1972-2027-06, Y apparent″: +0.00523 at matrix-7d-mimas-oslo 2020-01-13 00:00
- mimas, 1972-2027-06, Y astrometric″: +0.08501 at matrix-7d-mimas-geocentric 1973-12-31 00:00
- mimas, 1972-2027-06, apparent″: +0.70013 at matrix-7d-mimas-geocentric 2020-01-13 00:00
- mimas, 1972-2027-06, astrometric″: +0.00001 at named-mimas-cape-town 2026-10-19 12:00
- mimas, 1972-2027-06, diameter, largest radius″: +0.00000 at matrix-7d-mimas-geocentric 2008-11-17 00:00
- mimas, 1972-2027-06, diameter, mean radius″: -0.00324 at matrix-7d-mimas-geocentric 1973-12-24 00:00
- mimas, 1972-2027-06, distance km: +0.05377 at named-mimas-quito 2026-10-19 12:00
- mimas, 1972-2027-06, horizontal″: +2.77396 at matrix-7d-mimas-oslo 2027-04-05 00:00
- mimas, 1972-2027-06, position angle°: -0.01965 at matrix-7d-mimas-geocentric 2020-01-13 00:00
- mimas, 1972-2027-06, separation″: +0.00336 at matrix-7d-mimas-oslo 2020-01-13 00:00
- mimas, after, X apparent″: -0.01545 at matrix-7d-mimas-geocentric 2034-07-17 00:00
- mimas, after, X astrometric″: -0.14345 at matrix-7d-mimas-oslo 2092-01-28 00:00
- mimas, after, Y apparent″: -0.00492 at matrix-7d-mimas-oslo 2034-07-17 00:00
- mimas, after, Y astrometric″: +0.30518 at matrix-7d-mimas-oslo 2092-01-07 00:00
- mimas, after, apparent″: +1.22287 at matrix-7d-mimas-oslo 2034-07-17 00:00
- mimas, after, astrometric″: +0.00168 at named-mimas-quito 2100-12-31 23:59
- mimas, after, diameter, largest radius″: -0.00000 at matrix-7d-mimas-geocentric 2096-07-02 00:00
- mimas, after, diameter, mean radius″: -0.00324 at matrix-7d-mimas-geocentric 2091-12-24 00:00
- mimas, after, distance km: +10.28822 at named-mimas-cape-town 2100-12-31 23:59
- mimas, after, horizontal″: +405.52283 at matrix-7d-mimas-oslo 2100-08-23 00:00
- mimas, after, position angle°: -0.04268 at matrix-7d-mimas-oslo 2034-07-17 00:00
- mimas, after, separation″: +0.01202 at matrix-7d-mimas-geocentric 2034-07-17 00:00
- rhea, 1900-1961, X apparent″: -0.00505 at matrix-7d-rhea-oslo 1905-02-13 00:00
- rhea, 1900-1961, X astrometric″: +0.37365 at matrix-7d-rhea-geocentric 1900-07-09 00:00
- rhea, 1900-1961, Y apparent″: -0.02142 at matrix-7d-rhea-oslo 1931-01-05 00:00
- rhea, 1900-1961, Y astrometric″: +0.82251 at matrix-7d-rhea-geocentric 1900-06-11 00:00
- rhea, 1900-1961, apparent″: +0.62964 at matrix-7d-rhea-geocentric 1931-01-05 00:00
- rhea, 1900-1961, astrometric″: +0.00018 at matrix-7d-rhea-oslo 1900-12-03 00:00
- rhea, 1900-1961, diameter, largest radius″: +0.00000 at matrix-7d-rhea-geocentric 1921-09-26 00:00
- rhea, 1900-1961, diameter, mean radius″: -0.00052 at matrix-7d-rhea-geocentric 1914-12-21 00:00
- rhea, 1900-1961, distance km: -1.09592 at matrix-7d-rhea-oslo 1901-10-14 00:00
- rhea, 1900-1961, horizontal″: +1.00856 at named-rhea-alert 1900-01-01 00:00
- rhea, 1900-1961, position angle°: -0.01555 at matrix-7d-rhea-oslo 1931-01-05 00:00
- rhea, 1900-1961, separation″: -0.00822 at matrix-7d-rhea-oslo 1914-02-23 00:00
- rhea, 1962-1971, X apparent″: -0.00971 at matrix-7d-rhea-geocentric 1962-01-22 00:00
- rhea, 1962-1971, X astrometric″: -0.09377 at matrix-7d-rhea-geocentric 1971-11-15 00:00
- rhea, 1962-1971, Y apparent″: -0.00329 at matrix-7d-rhea-oslo 1963-02-04 00:00
- rhea, 1962-1971, Y astrometric″: -0.23342 at matrix-7d-rhea-oslo 1962-07-30 00:00
- rhea, 1962-1971, apparent″: +0.54124 at matrix-7d-rhea-oslo 1962-01-22 00:00
- rhea, 1962-1971, astrometric″: +0.00394 at matrix-7d-rhea-oslo 1965-03-15 00:00
- rhea, 1962-1971, diameter, largest radius″: -0.00000 at matrix-7d-rhea-geocentric 1965-05-24 00:00
- rhea, 1962-1971, diameter, mean radius″: -0.00051 at matrix-7d-rhea-geocentric 1971-11-22 00:00
- rhea, 1962-1971, distance km: -21.81264 at matrix-7d-rhea-oslo 1965-11-22 00:00
- rhea, 1962-1971, horizontal″: +2.00793 at matrix-7d-rhea-oslo 1971-12-27 00:00
- rhea, 1962-1971, position angle°: +0.02187 at matrix-7d-rhea-oslo 1966-12-26 00:00
- rhea, 1962-1971, separation″: -0.00794 at matrix-7d-rhea-oslo 1970-01-05 00:00
- rhea, 1972-2027-06, X apparent″: +0.01963 at matrix-7d-rhea-oslo 2020-01-13 00:00
- rhea, 1972-2027-06, X astrometric″: +0.10878 at matrix-7d-rhea-oslo 1972-11-13 00:00
- rhea, 1972-2027-06, Y apparent″: +0.01279 at matrix-7d-rhea-oslo 2020-01-13 00:00
- rhea, 1972-2027-06, Y astrometric″: -0.23838 at matrix-7d-rhea-geocentric 1972-12-18 00:00
- rhea, 1972-2027-06, apparent″: +0.68475 at matrix-7d-rhea-geocentric 2020-01-13 00:00
- rhea, 1972-2027-06, astrometric″: +0.00001 at named-rhea-cape-town 2026-10-19 12:00
- rhea, 1972-2027-06, diameter, largest radius″: -0.00000 at matrix-7d-rhea-oslo 1991-01-14 00:00
- rhea, 1972-2027-06, diameter, mean radius″: -0.00052 at matrix-7d-rhea-geocentric 1973-12-24 00:00
- rhea, 1972-2027-06, distance km: +0.05360 at named-rhea-quito 2026-10-19 12:00
- rhea, 1972-2027-06, horizontal″: +2.77353 at matrix-7d-rhea-oslo 2027-04-05 00:00
- rhea, 1972-2027-06, position angle°: -0.01706 at matrix-7d-rhea-geocentric 2020-01-13 00:00
- rhea, 1972-2027-06, separation″: +0.01726 at matrix-7d-rhea-oslo 2020-01-13 00:00
- rhea, after, X apparent″: -0.07473 at matrix-7d-rhea-oslo 2034-07-17 00:00
- rhea, after, X astrometric″: +0.38828 at matrix-7d-rhea-oslo 2092-01-21 00:00
- rhea, after, Y apparent″: +0.02041 at matrix-7d-rhea-geocentric 2034-07-17 00:00
- rhea, after, Y astrometric″: -0.86737 at matrix-7d-rhea-oslo 2091-12-24 00:00
- rhea, after, apparent″: +1.28315 at matrix-7d-rhea-oslo 2034-07-17 00:00
- rhea, after, astrometric″: +0.00168 at named-rhea-quito 2100-12-31 23:59
- rhea, after, diameter, largest radius″: +0.00000 at matrix-7d-rhea-geocentric 2027-09-06 00:00
- rhea, after, diameter, mean radius″: -0.00052 at matrix-7d-rhea-geocentric 2091-12-31 00:00
- rhea, after, distance km: +10.28850 at named-rhea-cape-town 2100-12-31 23:59
- rhea, after, horizontal″: +405.52467 at matrix-7d-rhea-oslo 2100-08-23 00:00
- rhea, after, position angle°: +0.01857 at matrix-7d-rhea-geocentric 2034-07-17 00:00
- rhea, after, separation″: +0.07179 at matrix-7d-rhea-oslo 2034-07-17 00:00
- saturn, 1900-1961, B centric″: +18192.32099 at matrix-7d-saturn-geocentric 1944-04-17 00:00
- saturn, 1900-1961, B detic, apparent line″: +11.97154 at matrix-7d-saturn-oslo 1907-03-04 00:00
- saturn, 1900-1961, B detic″: -0.00184 at matrix-7d-saturn-oslo 1918-08-19 00:00
- saturn, 1900-1961, B′ centric″: -18086.83059 at matrix-7d-saturn-oslo 1958-07-07 00:00
- saturn, 1900-1961, B′ detic, Sun aberrated″: +0.00182 at matrix-7d-saturn-oslo 1908-10-12 00:00
- saturn, 1900-1961, B′ detic, Sun light time″: +3.71103 at matrix-7d-saturn-geocentric 1950-07-10 00:00
- saturn, 1900-1961, B′ detic″: +3.71564 at matrix-7d-saturn-oslo 1921-01-31 00:00
- saturn, 1900-1961, apparent″: +0.61181 at matrix-7d-saturn-geocentric 1931-01-05 00:00
- saturn, 1900-1961, astrometric″: +0.00015 at matrix-7d-saturn-geocentric 1900-01-01 00:00
- saturn, 1900-1961, diameter″: -0.00001 at matrix-7d-saturn-geocentric 1917-09-24 00:00
- saturn, 1900-1961, distance km: -0.91844 at matrix-7d-saturn-oslo 1900-09-24 00:00
- saturn, 1900-1961, horizontal″: +1.00860 at named-saturn-alert 1900-01-01 00:00
- saturn, 1900-1961, illuminated %: +0.00019 at matrix-7d-saturn-oslo 1914-09-28 00:00
- saturn, 1900-1961, illuminated, Sun light time %: +0.00019 at matrix-7d-saturn-oslo 1914-09-28 00:00
- saturn, 1900-1961, phase angle vs phi″: -7.19096 at matrix-7d-saturn-geocentric 1915-07-19 00:00
- saturn, 1900-1961, phase angle″: -28.05009 at matrix-7d-saturn-oslo 1917-01-15 00:00
- saturn, 1900-1961, phase, Sun aberrated, vs phi″: -0.17994 at matrix-7d-saturn-geocentric 1928-11-19 00:00
- saturn, 1900-1961, phase, Sun aberrated″: +21.32122 at matrix-7d-saturn-oslo 1932-01-18 00:00
- saturn, 1900-1961, phase, Sun light time, vs phi″: -7.19164 at matrix-7d-saturn-geocentric 1915-07-19 00:00
- saturn, 1900-1961, phase, Sun light time″: -28.04653 at matrix-7d-saturn-oslo 1917-01-15 00:00
- saturn, 1900-1961, pole Dec″: -0.01799 at matrix-7d-saturn-geocentric 1914-05-18 00:00
- saturn, 1900-1961, pole RA″: -0.01800 at matrix-7d-saturn-oslo 1935-07-08 00:00
- saturn, 1900-1961, pole angle″: -0.26288 at matrix-7d-saturn-geocentric 1900-04-23 00:00
- saturn, 1962-1971, B centric″: +17466.50420 at matrix-7d-saturn-geocentric 1971-09-06 00:00
- saturn, 1962-1971, B detic, apparent line″: +11.96865 at matrix-7d-saturn-oslo 1966-03-07 00:00
- saturn, 1962-1971, B detic″: +0.00322 at matrix-7d-saturn-oslo 1965-03-22 00:00
- saturn, 1962-1971, B′ centric″: +17408.45601 at matrix-7d-saturn-geocentric 1971-12-27 00:00
- saturn, 1962-1971, B′ detic, Sun aberrated″: +0.00226 at matrix-7d-saturn-geocentric 1965-08-02 00:00
- saturn, 1962-1971, B′ detic, Sun light time″: -3.64919 at matrix-7d-saturn-oslo 1966-10-03 00:00
- saturn, 1962-1971, B′ detic″: -3.64816 at matrix-7d-saturn-oslo 1966-10-03 00:00
- saturn, 1962-1971, apparent″: +0.53233 at matrix-7d-saturn-oslo 1962-01-22 00:00
- saturn, 1962-1971, astrometric″: +0.00347 at matrix-7d-saturn-oslo 1965-03-01 00:00
- saturn, 1962-1971, diameter″: -0.00001 at matrix-7d-saturn-geocentric 1967-05-15 00:00
- saturn, 1962-1971, distance km: +17.16456 at matrix-7d-saturn-oslo 1965-07-05 00:00
- saturn, 1962-1971, horizontal″: +2.00794 at matrix-7d-saturn-oslo 1971-12-27 00:00
- saturn, 1962-1971, illuminated %: +0.00019 at matrix-7d-saturn-oslo 1971-08-23 00:00
- saturn, 1962-1971, illuminated, Sun light time %: +0.00019 at matrix-7d-saturn-oslo 1971-08-23 00:00
- saturn, 1962-1971, phase angle vs phi″: -7.11699 at matrix-7d-saturn-geocentric 1971-10-04 00:00
- saturn, 1962-1971, phase angle″: +26.98801 at matrix-7d-saturn-oslo 1971-12-06 00:00
- saturn, 1962-1971, phase, Sun aberrated, vs phi″: +0.18021 at matrix-7d-saturn-geocentric 1970-09-07 00:00
- saturn, 1962-1971, phase, Sun aberrated″: +20.88470 at matrix-7d-saturn-geocentric 1962-01-29 00:00
- saturn, 1962-1971, phase, Sun light time, vs phi″: -7.12349 at matrix-7d-saturn-geocentric 1971-10-04 00:00
- saturn, 1962-1971, phase, Sun light time″: +26.99426 at matrix-7d-saturn-oslo 1971-12-06 00:00
- saturn, 1962-1971, pole Dec″: -0.01799 at matrix-7d-saturn-geocentric 1970-05-18 00:00
- saturn, 1962-1971, pole RA″: +0.01800 at matrix-7d-saturn-oslo 1963-07-08 00:00
- saturn, 1962-1971, pole angle″: -0.18275 at matrix-7d-saturn-geocentric 1963-02-04 00:00
- saturn, 1972-2027-06, B centric″: +18195.91781 at matrix-7d-saturn-geocentric 2003-04-07 00:00
- saturn, 1972-2027-06, B detic, apparent line″: +11.97645 at matrix-7d-saturn-oslo 2009-03-02 00:00
- saturn, 1972-2027-06, B detic″: -0.00180 at matrix-7d-saturn-geocentric 1983-06-20 00:00
- saturn, 1972-2027-06, B′ centric″: -18089.14234 at matrix-7d-saturn-oslo 2017-05-29 00:00
- saturn, 1972-2027-06, B′ detic, Sun aberrated″: +0.00180 at matrix-7d-saturn-geocentric 1998-11-23 00:00
- saturn, 1972-2027-06, B′ detic, Sun light time″: +3.71146 at matrix-7d-saturn-oslo 2009-05-18 00:00
- saturn, 1972-2027-06, B′ detic″: +3.71572 at matrix-7d-saturn-oslo 1979-12-10 00:00
- saturn, 1972-2027-06, apparent″: +0.70672 at matrix-7d-saturn-geocentric 2020-01-13 00:00
- saturn, 1972-2027-06, astrometric″: +0.00001 at named-saturn-cape-town 2026-10-19 12:00
- saturn, 1972-2027-06, diameter″: -0.00000 at matrix-7d-saturn-oslo 2000-07-17 00:00
- saturn, 1972-2027-06, distance km: +0.05367 at named-saturn-quito 2026-10-19 12:00
- saturn, 1972-2027-06, horizontal″: +2.77414 at matrix-7d-saturn-oslo 2027-04-05 00:00
- saturn, 1972-2027-06, illuminated %: +0.00019 at matrix-7d-saturn-oslo 2002-09-16 00:00
- saturn, 1972-2027-06, illuminated, Sun light time %: +0.00019 at matrix-7d-saturn-oslo 2002-09-16 00:00
- saturn, 1972-2027-06, phase angle vs phi″: +7.18890 at matrix-7d-saturn-geocentric 2003-01-20 00:00
- saturn, 1972-2027-06, phase angle″: -27.84596 at matrix-7d-saturn-geocentric 2005-01-10 00:00
- saturn, 1972-2027-06, phase, Sun aberrated, vs phi″: -0.17995 at matrix-7d-saturn-geocentric 2023-02-06 00:00
- saturn, 1972-2027-06, phase, Sun aberrated″: +21.23864 at matrix-7d-saturn-oslo 1991-01-21 00:00
- saturn, 1972-2027-06, phase, Sun light time, vs phi″: +7.18089 at matrix-7d-saturn-geocentric 2003-01-20 00:00
- saturn, 1972-2027-06, phase, Sun light time″: -27.84196 at matrix-7d-saturn-geocentric 2005-01-10 00:00
- saturn, 1972-2027-06, pole Dec″: -0.01799 at matrix-7d-saturn-geocentric 2026-05-18 00:00
- saturn, 1972-2027-06, pole RA″: +0.01800 at ring-plane-window-2024-2025-saturn-oslo 2024-11-06 00:00
- saturn, 1972-2027-06, pole angle″: +0.18744 at matrix-7d-saturn-geocentric 1990-01-08 00:00
- saturn, after, B centric″: +18194.26491 at matrix-7d-saturn-geocentric 2062-04-03 00:00
- saturn, after, B detic, apparent line″: +11.98612 at matrix-7d-saturn-oslo 2068-03-05 00:00
- saturn, after, B detic″: +0.00208 at matrix-7d-saturn-oslo 2091-10-08 00:00
- saturn, after, B′ centric″: -18091.34870 at matrix-7d-saturn-oslo 2076-04-13 00:00
- saturn, after, B′ detic, Sun aberrated″: +0.00180 at matrix-7d-saturn-oslo 2039-01-24 00:00
- saturn, after, B′ detic, Sun light time″: +3.71223 at matrix-7d-saturn-geocentric 2097-09-23 00:00
- saturn, after, B′ detic″: +3.71680 at matrix-7d-saturn-geocentric 2038-10-25 00:00
- saturn, after, apparent″: +1.20812 at matrix-7d-saturn-oslo 2034-07-17 00:00
- saturn, after, astrometric″: +0.00168 at named-saturn-quito 2100-12-31 23:59
- saturn, after, diameter″: -0.00001 at matrix-7d-saturn-oslo 2099-02-23 00:00
- saturn, after, distance km: +10.28817 at named-saturn-cape-town 2100-12-31 23:59
- saturn, after, horizontal″: +405.52335 at matrix-7d-saturn-oslo 2100-08-23 00:00
- saturn, after, illuminated %: +0.00019 at matrix-7d-saturn-oslo 2032-10-04 00:00
- saturn, after, illuminated, Sun light time %: +0.00019 at matrix-7d-saturn-geocentric 2091-10-01 00:00
- saturn, after, phase angle vs phi″: -7.18391 at matrix-7d-saturn-oslo 2062-10-09 00:00
- saturn, after, phase angle″: -27.96725 at matrix-7d-saturn-oslo 2064-01-14 00:00
- saturn, after, phase, Sun aberrated, vs phi″: +0.17997 at matrix-7d-saturn-geocentric 2039-02-07 00:00
- saturn, after, phase, Sun aberrated″: +21.53765 at matrix-7d-saturn-oslo 2079-01-16 00:00
- saturn, after, phase, Sun light time, vs phi″: -7.18603 at matrix-7d-saturn-geocentric 2091-08-27 00:00
- saturn, after, phase, Sun light time″: -27.96141 at matrix-7d-saturn-oslo 2064-01-14 00:00
- saturn, after, pole Dec″: -0.01799 at matrix-7d-saturn-geocentric 2082-05-18 00:00
- saturn, after, pole RA″: -0.01800 at matrix-7d-saturn-oslo 2075-07-08 00:00
- saturn, after, pole angle″: -0.27075 at matrix-7d-saturn-geocentric 2092-05-12 00:00
- tethys, 1900-1961, X apparent″: +0.00447 at matrix-7d-tethys-geocentric 1931-01-05 00:00
- tethys, 1900-1961, X astrometric″: +0.20373 at matrix-7d-tethys-geocentric 1900-06-18 00:00
- tethys, 1900-1961, Y apparent″: +0.00259 at matrix-7d-tethys-oslo 1932-01-18 00:00
- tethys, 1900-1961, Y astrometric″: -0.46136 at matrix-7d-tethys-oslo 1900-06-11 00:00
- tethys, 1900-1961, apparent″: +0.61330 at matrix-7d-tethys-oslo 1932-01-18 00:00
- tethys, 1900-1961, astrometric″: +0.00019 at matrix-7d-tethys-oslo 1901-01-07 00:00
- tethys, 1900-1961, diameter, largest radius″: -0.00000 at matrix-7d-tethys-oslo 1938-02-07 00:00
- tethys, 1900-1961, diameter, mean radius″: -0.00254 at matrix-7d-tethys-oslo 1914-12-21 00:00
- tethys, 1900-1961, distance km: -1.20165 at matrix-7d-tethys-oslo 1900-10-08 00:00
- tethys, 1900-1961, horizontal″: +1.00862 at named-tethys-alert 1900-01-01 00:00
- tethys, 1900-1961, position angle°: +0.01784 at matrix-7d-tethys-geocentric 1931-01-05 00:00
- tethys, 1900-1961, separation″: -0.00459 at matrix-7d-tethys-oslo 1914-03-02 00:00
- tethys, 1962-1971, X apparent″: -0.00484 at matrix-7d-tethys-geocentric 1962-01-22 00:00
- tethys, 1962-1971, X astrometric″: -0.05347 at matrix-7d-tethys-oslo 1971-10-25 00:00
- tethys, 1962-1971, Y apparent″: +0.00265 at matrix-7d-tethys-oslo 1963-02-04 00:00
- tethys, 1962-1971, Y astrometric″: -0.13246 at matrix-7d-tethys-geocentric 1962-08-06 00:00
- tethys, 1962-1971, apparent″: +0.53661 at matrix-7d-tethys-oslo 1962-01-22 00:00
- tethys, 1962-1971, astrometric″: +0.00419 at matrix-7d-tethys-oslo 1965-03-15 00:00
- tethys, 1962-1971, diameter, largest radius″: -0.00000 at matrix-7d-tethys-geocentric 1968-07-29 00:00
- tethys, 1962-1971, diameter, mean radius″: -0.00252 at matrix-7d-tethys-oslo 1971-11-29 00:00
- tethys, 1962-1971, distance km: +24.63986 at matrix-7d-tethys-oslo 1965-07-05 00:00
- tethys, 1962-1971, horizontal″: +2.00797 at matrix-7d-tethys-oslo 1971-12-27 00:00
- tethys, 1962-1971, position angle°: +0.02623 at matrix-7d-tethys-geocentric 1966-08-22 00:00
- tethys, 1962-1971, separation″: -0.00457 at matrix-7d-tethys-oslo 1970-01-12 00:00
- tethys, 1972-2027-06, X apparent″: -0.01168 at matrix-7d-tethys-geocentric 2020-01-13 00:00
- tethys, 1972-2027-06, X astrometric″: +0.06282 at matrix-7d-tethys-oslo 1972-10-30 00:00
- tethys, 1972-2027-06, Y apparent″: -0.00633 at matrix-7d-tethys-oslo 2020-01-13 00:00
- tethys, 1972-2027-06, Y astrometric″: +0.13325 at matrix-7d-tethys-geocentric 1972-12-11 00:00
- tethys, 1972-2027-06, apparent″: +0.71958 at matrix-7d-tethys-geocentric 2020-01-13 00:00
- tethys, 1972-2027-06, astrometric″: +0.00001 at named-tethys-cape-town 2026-10-19 12:00
- tethys, 1972-2027-06, diameter, largest radius″: -0.00000 at matrix-7d-tethys-geocentric 2017-10-30 00:00
- tethys, 1972-2027-06, diameter, mean radius″: -0.00254 at matrix-7d-tethys-oslo 1973-12-24 00:00
- tethys, 1972-2027-06, distance km: +0.05378 at named-tethys-quito 2026-10-19 12:00
- tethys, 1972-2027-06, horizontal″: +2.77398 at matrix-7d-tethys-oslo 2027-04-05 00:00
- tethys, 1972-2027-06, position angle°: +0.01317 at matrix-7d-tethys-oslo 1974-07-01 00:00
- tethys, 1972-2027-06, separation″: +0.01190 at matrix-7d-tethys-geocentric 2020-01-13 00:00
- tethys, after, X apparent″: +0.03365 at matrix-7d-tethys-geocentric 2034-07-17 00:00
- tethys, after, X astrometric″: +0.22269 at matrix-7d-tethys-oslo 2092-01-21 00:00
- tethys, after, Y apparent″: -0.00465 at matrix-7d-tethys-oslo 2093-07-20 00:00
- tethys, after, Y astrometric″: -0.47972 at matrix-7d-tethys-oslo 2091-12-10 00:00
- tethys, after, apparent″: +1.17350 at matrix-7d-tethys-oslo 2034-07-17 00:00
- tethys, after, astrometric″: +0.00168 at named-tethys-quito 2100-12-31 23:59
- tethys, after, diameter, largest radius″: -0.00000 at matrix-7d-tethys-geocentric 2030-01-28 00:00
- tethys, after, diameter, mean radius″: -0.00254 at matrix-7d-tethys-oslo 2032-12-27 00:00
- tethys, after, distance km: +10.28815 at named-tethys-cape-town 2100-12-31 23:59
- tethys, after, horizontal″: +405.52448 at matrix-7d-tethys-oslo 2100-08-23 00:00
- tethys, after, position angle°: +0.01158 at matrix-7d-tethys-geocentric 2078-01-03 00:00
- tethys, after, separation″: +0.03386 at matrix-7d-tethys-oslo 2034-07-17 00:00
- titan, 1900-1961, X apparent″: +0.01581 at matrix-7d-titan-oslo 1918-08-12 00:00
- titan, 1900-1961, X astrometric″: +0.87791 at matrix-7d-titan-oslo 1900-07-02 00:00
- titan, 1900-1961, Y apparent″: +0.04712 at matrix-7d-titan-geocentric 1931-01-05 00:00
- titan, 1900-1961, Y astrometric″: -1.90641 at matrix-7d-titan-geocentric 1900-07-23 00:00
- titan, 1900-1961, apparent″: +0.60655 at matrix-7d-titan-oslo 1932-01-18 00:00
- titan, 1900-1961, astrometric″: +0.00017 at named-titan-quito 1900-01-01 00:00
- titan, 1900-1961, diameter, largest radius″: +0.00000 at matrix-7d-titan-oslo 1954-09-13 00:00
- titan, 1900-1961, diameter, mean radius″: -0.00012 at matrix-7d-titan-geocentric 1914-12-21 00:00
- titan, 1900-1961, distance km: -1.06975 at matrix-7d-titan-oslo 1900-09-24 00:00
- titan, 1900-1961, horizontal″: +1.00870 at named-titan-alert 1900-01-01 00:00
- titan, 1900-1961, position angle°: -0.02597 at matrix-7d-titan-geocentric 1931-01-05 00:00
- titan, 1900-1961, separation″: -0.01934 at matrix-7d-titan-oslo 1912-01-29 00:00
- titan, 1962-1971, X apparent″: +0.01957 at matrix-7d-titan-geocentric 1962-01-22 00:00
- titan, 1962-1971, X astrometric″: -0.22305 at matrix-7d-titan-geocentric 1971-11-01 00:00
- titan, 1962-1971, Y apparent″: +0.00941 at matrix-7d-titan-geocentric 1963-02-04 00:00
- titan, 1962-1971, Y astrometric″: -0.54939 at matrix-7d-titan-geocentric 1962-09-24 00:00
- titan, 1962-1971, apparent″: +0.51395 at matrix-7d-titan-oslo 1962-01-22 00:00
- titan, 1962-1971, astrometric″: +0.00372 at matrix-7d-titan-oslo 1965-03-08 00:00
- titan, 1962-1971, diameter, largest radius″: -0.00000 at matrix-7d-titan-geocentric 1962-05-21 00:00
- titan, 1962-1971, diameter, mean radius″: -0.00012 at matrix-7d-titan-geocentric 1971-11-29 00:00
- titan, 1962-1971, distance km: +20.49121 at matrix-7d-titan-oslo 1965-07-05 00:00
- titan, 1962-1971, horizontal″: +2.00773 at matrix-7d-titan-oslo 1971-12-27 00:00
- titan, 1962-1971, position angle°: -0.01345 at matrix-7d-titan-geocentric 1962-01-22 00:00
- titan, 1962-1971, separation″: -0.01922 at matrix-7d-titan-oslo 1970-01-19 00:00
- titan, 1972-2027-06, X apparent″: -0.01550 at matrix-7d-titan-geocentric 1988-12-26 00:00
- titan, 1972-2027-06, X astrometric″: -0.25104 at matrix-7d-titan-oslo 1972-12-04 00:00
- titan, 1972-2027-06, Y apparent″: +0.03052 at matrix-7d-titan-geocentric 2020-01-13 00:00
- titan, 1972-2027-06, Y astrometric″: -0.55763 at matrix-7d-titan-geocentric 1972-12-25 00:00
- titan, 1972-2027-06, apparent″: +0.68442 at matrix-7d-titan-geocentric 2020-01-13 00:00
- titan, 1972-2027-06, astrometric″: +0.00001 at matrix-7d-titan-oslo 2027-03-15 00:00
- titan, 1972-2027-06, diameter, largest radius″: +0.00000 at matrix-7d-titan-oslo 2002-09-09 00:00
- titan, 1972-2027-06, diameter, mean radius″: -0.00012 at matrix-7d-titan-geocentric 1973-12-17 00:00
- titan, 1972-2027-06, distance km: +0.05364 at named-titan-quito 2026-10-19 12:00
- titan, 1972-2027-06, horizontal″: +2.77548 at matrix-7d-titan-oslo 2027-04-05 00:00
- titan, 1972-2027-06, position angle°: -0.02558 at matrix-7d-titan-geocentric 2020-01-13 00:00
- titan, 1972-2027-06, separation″: +0.02212 at matrix-7d-titan-oslo 1974-07-01 00:00
- titan, after, X apparent″: +0.14517 at matrix-7d-titan-oslo 2034-07-17 00:00
- titan, after, X astrometric″: -0.88411 at matrix-7d-titan-oslo 2092-02-18 00:00
- titan, after, Y apparent″: +0.02576 at matrix-7d-titan-oslo 2093-07-20 00:00
- titan, after, Y astrometric″: +2.03117 at matrix-7d-titan-oslo 2092-01-14 00:00
- titan, after, apparent″: +1.06154 at matrix-7d-titan-oslo 2034-07-17 00:00
- titan, after, astrometric″: +0.00168 at named-titan-quito 2100-12-31 23:59
- titan, after, diameter, largest radius″: -0.00000 at matrix-7d-titan-oslo 2091-07-09 00:00
- titan, after, diameter, mean radius″: -0.00012 at matrix-7d-titan-oslo 2090-12-11 00:00
- titan, after, distance km: +10.28729 at named-titan-cape-town 2100-12-31 23:59
- titan, after, horizontal″: +405.53106 at matrix-7d-titan-oslo 2100-08-23 00:00
- titan, after, position angle°: +0.01824 at matrix-7d-titan-geocentric 2034-07-17 00:00
- titan, after, separation″: +0.14426 at matrix-7d-titan-oslo 2034-07-17 00:00
