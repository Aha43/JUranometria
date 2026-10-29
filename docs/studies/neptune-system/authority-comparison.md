# #654 Neptune authority comparison

Skyfield on the pinned kernels against the kept JPL Horizons responses (Horizons: its nep098_merged for Neptune and the moons, DE441 for the Earth). The Earth and the Sun are DE440's (`de440s.bsp`). Observers at sea level on the WGS84 ellipsoid. Civil time: UT1 before 1972, UTC from 1972; the record's expiry, 2027-06-28, read from `Leap_Second.dat`, bounds the exact era.

Four Neptune models: **barycentre**, DE440's Neptune barycentre alone (0 -> 8); **centre**, the barycentre plus `nep098_part-1`'s 8 -> 899; **barycentre on DE442**, `nep098_part-1`'s own barycentre (DE442) alone; **centre on DE442**, `nep098_part-1`'s own barycentre (DE442) plus its 8 -> 899. Every row 1900-2100 lies inside the kernel's span (JD 2378496.5 to 2524592.5).

Constants read from `pck00011.tpc`: Neptune's radii 24764, 24764, 24341 km; pole RA 299.36 +0.0 T, Dec 43.46 +0.0 T, with 1 periodic terms in RA and 1 in Dec; the moons' radii Triton [1352.6, 1352.6, 1352.6], Nereid [170.0, 170.0, 170.0] km; the Sun's radius 695700 km.

## Neptune, barycentre model: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6390 rows) | after (8112 rows) | worst row |
|---|---:|---:|---:|---:|---|
| apparent, Sun deflection, beyond 1° from the Sun″ | +0.2030 | +0.0582 | +0.0639 | +0.0987 | matrix-7d-neptune-oslo 1900-05-21 00:00 |
| apparent, Sun deflection″ | +0.2030 | +0.0582 | +0.0639 | +2.0232 | matrix-7d-neptune-geocentric 2087-08-11 00:00 |
| apparent, no deflection″ | +0.4873 | +0.2594 | +1.6069 | +1.4340 | matrix-7d-neptune-oslo 2006-02-06 00:00 |
| astrometric″ | +0.0517 | +0.0092 | +0.0125 | +0.1072 | opposition-2100-neptune-oslo-hourly 2100-03-05 01:00 |
| diameter″ | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-neptune-oslo 2072-02-22 00:00 |
| distance km | -585.5338 | -121.6766 | -363.4256 | -593.6515 | matrix-7d-neptune-oslo 2063-03-26 00:00 |
| horizontal″ | +0.9304 | +1.9915 | +2.4903 | +411.5706 | named-neptune-alert 2100-12-31 23:59 |
| illuminated % | -0.0000 | -0.0000 | +0.0000 | +0.0000 | matrix-7d-neptune-oslo 2045-08-07 00:00 |
| illuminated, Sun light time and aberration % | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-neptune-oslo 2062-04-10 00:00 |
| phase angle vs phi″ | -3.9415 | -3.8900 | -3.9471 | -3.9577 | matrix-7d-neptune-geocentric 2047-07-01 00:00 |
| phase angle, Sun light time and aberration, vs phi″ | +0.1816 | +0.1800 | +0.1805 | -0.1830 | matrix-7d-neptune-geocentric 2100-03-15 00:00 |
| phase angle, Sun light time and aberration″ | -21.0048 | +20.5079 | +21.2100 | -21.0882 | matrix-7d-neptune-oslo 2005-02-07 00:00 |
| phase angle″ | -24.7455 | +23.5460 | +24.1690 | -24.8337 | matrix-7d-neptune-oslo 2085-01-29 00:00 |
| pole Dec, secular only″ | +1701.8280 | +1757.5920 | +1836.0000 | +1794.3480 | matrix-7d-neptune-oslo 2003-08-18 00:00 |
| pole Dec, with periodic terms″ | -0.0180 | -0.0180 | -0.0180 | +0.0180 | matrix-7d-neptune-geocentric 1945-11-12 00:00 |
| pole RA, secular only″ | +2050.7040 | +945.2520 | +728.3520 | -1949.7240 | matrix-7d-neptune-oslo 1900-01-01 00:00 |
| pole RA, with periodic terms″ | +0.0180 | +0.0180 | -0.0180 | +0.0180 | matrix-7d-neptune-oslo 2076-02-10 00:00 |
| pole angle, secular only″ | -2025.2170 | -1438.6276 | +1704.2214 | +1837.0989 | matrix-7d-neptune-geocentric 1931-11-09 00:00 |
| pole angle, with periodic terms″ | +0.2225 | -0.1797 | +0.1835 | -1.4033 | matrix-7d-neptune-oslo 2085-08-06 00:00 |
| sub-observer latitude, centric vs Horizons″ | -2983.1472 | +1099.9827 | +3000.3743 | -2999.6317 | matrix-7d-neptune-geocentric 2004-05-03 00:00 |
| sub-observer latitude, detic vs Horizons″ | -0.0220 | -0.0062 | +0.0071 | -0.0317 | named-neptune-chatham 2100-12-31 23:59 |

## Neptune, centre model: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6390 rows) | after (8112 rows) | worst row |
|---|---:|---:|---:|---:|---|
| apparent, Sun deflection, beyond 1° from the Sun″ | +0.2001 | +0.0572 | +0.0626 | +0.0958 | matrix-7d-neptune-oslo 1900-05-07 00:00 |
| apparent, Sun deflection″ | +0.2001 | +0.0572 | +0.0626 | +2.0246 | matrix-7d-neptune-geocentric 2087-08-11 00:00 |
| apparent, no deflection″ | +0.4869 | +0.2609 | +1.6051 | +1.4354 | matrix-7d-neptune-oslo 2006-02-06 00:00 |
| astrometric″ | +0.0483 | +0.0065 | +0.0099 | +0.1039 | opposition-2100-neptune-oslo-hourly 2100-03-04 00:00 |
| diameter″ | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-neptune-geocentric 2061-01-03 00:00 |
| distance km | -529.6365 | -55.9255 | -300.0198 | -526.7196 | matrix-7d-neptune-geocentric 1900-09-24 00:00 |
| horizontal″ | +0.9278 | +1.9896 | +2.4915 | +411.5715 | named-neptune-alert 2100-12-31 23:59 |
| illuminated % | -0.0000 | -0.0000 | +0.0000 | +0.0000 | matrix-7d-neptune-oslo 2045-08-07 00:00 |
| illuminated, Sun light time and aberration % | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-neptune-oslo 2062-04-10 00:00 |
| phase angle vs phi″ | -3.9415 | -3.8900 | -3.9471 | -3.9578 | matrix-7d-neptune-geocentric 2047-07-01 00:00 |
| phase angle, Sun light time and aberration, vs phi″ | +0.1810 | +0.1804 | -0.1800 | -0.1825 | matrix-7d-neptune-geocentric 2100-03-15 00:00 |
| phase angle, Sun light time and aberration″ | -21.0043 | +20.5074 | +21.2094 | -21.0889 | matrix-7d-neptune-oslo 2005-02-07 00:00 |
| phase angle″ | -24.7454 | +23.5460 | +24.1691 | -24.8337 | matrix-7d-neptune-oslo 2085-01-29 00:00 |

## Neptune, barycentre on DE442 model: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6390 rows) | after (8112 rows) | worst row |
|---|---:|---:|---:|---:|---|
| apparent, Sun deflection, beyond 1° from the Sun″ | +0.2486 | +0.0525 | +0.0549 | +0.1949 | matrix-7d-neptune-oslo 1900-04-16 00:00 |
| apparent, Sun deflection″ | +0.2486 | +0.0525 | +0.0549 | +2.0801 | matrix-7d-neptune-geocentric 2087-08-11 00:00 |
| apparent, no deflection″ | +0.4981 | +0.2595 | +1.6062 | +1.4237 | matrix-7d-neptune-oslo 2006-02-06 00:00 |
| astrometric″ | +0.0035 | +0.0040 | +0.0035 | +0.0038 | matrix-7d-neptune-geocentric 1965-05-03 00:00 |
| diameter″ | -0.0000 | +0.0000 | +0.0000 | -0.0000 | matrix-7d-neptune-geocentric 1962-07-02 00:00 |
| distance km | +74.2321 | +89.4793 | +66.8124 | -74.5090 | matrix-7d-neptune-oslo 1965-03-01 00:00 |
| horizontal″ | +0.9787 | +1.9956 | +2.4994 | +411.4756 | named-neptune-alert 2100-12-31 23:59 |
| illuminated % | -0.0000 | -0.0000 | +0.0000 | +0.0000 | matrix-7d-neptune-oslo 2045-08-07 00:00 |
| illuminated, Sun light time and aberration % | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-neptune-oslo 1919-10-27 00:00 |
| phase angle vs phi″ | -3.9428 | -3.8899 | -3.9474 | -3.9575 | matrix-7d-neptune-geocentric 2047-07-01 00:00 |
| phase angle, Sun light time and aberration, vs phi″ | +0.1805 | +0.1798 | +0.1805 | -0.1802 | matrix-7d-neptune-geocentric 1907-12-30 00:00 |
| phase angle, Sun light time and aberration″ | -21.0042 | +20.5081 | +21.2100 | -21.0912 | matrix-7d-neptune-oslo 2005-02-07 00:00 |
| phase angle″ | -24.7449 | +23.5462 | +24.1690 | -24.8366 | matrix-7d-neptune-oslo 2085-01-29 00:00 |

## Neptune, centre on DE442 model: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6390 rows) | after (8112 rows) | worst row |
|---|---:|---:|---:|---:|---|
| apparent, Sun deflection, beyond 1° from the Sun″ | +0.2458 | +0.0516 | +0.0539 | +0.1921 | matrix-7d-neptune-oslo 1900-04-23 00:00 |
| apparent, Sun deflection″ | +0.2458 | +0.0516 | +0.0539 | +2.0816 | matrix-7d-neptune-geocentric 2087-08-11 00:00 |
| apparent, no deflection″ | +0.4994 | +0.2610 | +1.6044 | +1.4252 | matrix-7d-neptune-oslo 2006-02-06 00:00 |
| astrometric″ | +0.0001 | +0.0010 | +0.0000 | +0.0005 | matrix-7d-neptune-geocentric 1965-11-01 00:00 |
| diameter″ | -0.0000 | -0.0000 | +0.0000 | +0.0000 | matrix-7d-neptune-geocentric 1962-03-12 00:00 |
| distance km | +0.9322 | +19.2598 | -0.1894 | -8.6120 | matrix-7d-neptune-oslo 1965-03-01 00:00 |
| horizontal″ | +0.9760 | +1.9937 | +2.5007 | +411.4765 | named-neptune-alert 2100-12-31 23:59 |
| illuminated % | -0.0000 | -0.0000 | +0.0000 | +0.0000 | matrix-7d-neptune-oslo 2045-08-07 00:00 |
| illuminated, Sun light time and aberration % | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-neptune-oslo 2030-04-29 00:00 |
| phase angle vs phi″ | -3.9428 | -3.8899 | -3.9474 | -3.9576 | matrix-7d-neptune-geocentric 2047-07-01 00:00 |
| phase angle, Sun light time and aberration, vs phi″ | -0.1800 | +0.1802 | -0.1800 | +0.1800 | matrix-7d-neptune-geocentric 1963-09-16 00:00 |
| phase angle, Sun light time and aberration″ | -21.0037 | +20.5075 | +21.2094 | -21.0918 | matrix-7d-neptune-oslo 2005-02-07 00:00 |
| phase angle″ | -24.7448 | +23.5462 | +24.1691 | -24.8367 | matrix-7d-neptune-oslo 2085-01-29 00:00 |

## Triton: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6099 rows) | after (7677 rows) | worst row |
|---|---:|---:|---:|---:|---|
| X apparent, beyond 1° from the Sun″ | +0.0005 | +0.0006 | -0.0005 | +0.0005 | matrix-7d-triton-oslo 1965-01-18 00:00 |
| X apparent″ | +0.0005 | +0.0006 | -0.0005 | +0.0268 | matrix-7d-triton-geocentric 2087-08-11 00:00 |
| Y apparent, beyond 1° from the Sun″ | +0.0005 | +0.0006 | -0.0005 | -0.0005 | matrix-7d-triton-geocentric 1964-01-06 00:00 |
| Y apparent″ | +0.0005 | +0.0006 | -0.0005 | +0.0281 | matrix-7d-triton-geocentric 2087-08-11 00:00 |
| apparent, beyond 1° from the Sun″ | +0.2001 | +0.0572 | +0.0626 | +0.0958 | matrix-7d-triton-oslo 1900-05-07 00:00 |
| apparent″ | +0.2001 | +0.0572 | +0.0626 | +2.0392 | matrix-7d-triton-geocentric 2087-08-11 00:00 |
| astrometric″ | +0.0483 | +0.0065 | +0.0099 | +0.1039 | matrix-7d-triton-oslo 2100-03-01 00:00 |
| diameter, largest radius″ | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-triton-geocentric 1903-10-12 00:00 |
| distance km | -529.6791 | -57.1156 | -300.0323 | -526.7937 | matrix-7d-triton-geocentric 1900-09-17 00:00 |
| horizontal″ | +0.9278 | +1.9896 | +2.4915 | +411.5690 | named-triton-alert 2100-12-31 23:59 |
| position angle, beyond 1° from the Sun° | +0.0014 | -0.0021 | -0.0015 | -0.0015 | matrix-7d-triton-geocentric 1965-10-25 00:00 |
| position angle, beyond 5″ of Neptune° | +0.0014 | -0.0019 | -0.0015 | -0.1028 | matrix-7d-triton-geocentric 2087-08-11 00:00 |
| position angle, within 5″ of Neptune° | -0.0006 | -0.0021 | +0.0000 | -0.0007 | matrix-7d-triton-geocentric 1965-10-25 00:00 |
| position angle° | +0.0014 | -0.0021 | -0.0015 | -0.1028 | matrix-7d-triton-geocentric 2087-08-11 00:00 |
| separation, beyond 1° from the Sun″ | +0.0017 | +0.0017 | -0.0017 | +0.0019 | matrix-7d-triton-geocentric 2075-07-15 00:00 |
| separation″ | +0.0017 | +0.0017 | +0.0037 | -0.0124 | matrix-7d-triton-geocentric 2089-08-15 00:00 |

## Nereid: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6099 rows) | after (7677 rows) | worst row |
|---|---:|---:|---:|---:|---|
| X apparent, beyond 1° from the Sun″ | +0.0006 | -0.0005 | +0.0007 | -0.0007 | matrix-7d-nereid-oslo 2007-11-12 00:00 |
| X apparent″ | +0.0006 | -0.0005 | +0.0007 | +1.6224 | matrix-7d-nereid-geocentric 2085-08-06 00:00 |
| Y apparent, beyond 1° from the Sun″ | +0.0006 | +0.0005 | +0.0005 | -0.0006 | matrix-7d-nereid-oslo 1912-12-09 00:00 |
| Y apparent″ | +0.0006 | +0.0005 | +0.0005 | -1.8228 | matrix-7d-nereid-geocentric 2087-08-11 00:00 |
| apparent, beyond 1° from the Sun″ | +0.2002 | +0.0572 | +0.0626 | +0.0959 | matrix-7d-nereid-oslo 1900-05-07 00:00 |
| apparent″ | +0.2002 | +0.0572 | +0.0626 | +0.0959 | matrix-7d-nereid-oslo 1900-05-07 00:00 |
| astrometric″ | +0.0482 | +0.0065 | +0.0099 | +0.1040 | matrix-7d-nereid-oslo 2100-03-01 00:00 |
| diameter, largest radius″ | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-nereid-oslo 1900-04-16 00:00 |
| distance km | -530.1756 | -56.3340 | -300.0722 | -526.2673 | matrix-7d-nereid-geocentric 1900-09-24 00:00 |
| horizontal″ | +0.9278 | +1.9896 | +2.4914 | +411.5715 | named-nereid-alert 2100-12-31 23:59 |
| position angle, beyond 1° from the Sun° | -0.0191 | +0.0117 | -0.0187 | -0.0175 | matrix-7d-nereid-geocentric 1913-12-15 00:00 |
| position angle, beyond 5″ of Neptune° | -0.0191 | +0.0117 | -0.0187 | -0.2221 | matrix-7d-nereid-geocentric 2087-08-11 00:00 |
| position angle, within 5″ of Neptune° | +0.0007 | +0.0000 | +0.0004 | -0.0013 | matrix-7d-nereid-geocentric 2083-02-15 00:00 |
| position angle° | -0.0191 | +0.0117 | -0.0187 | -0.2221 | matrix-7d-nereid-geocentric 2087-08-11 00:00 |
| separation, beyond 1° from the Sun″ | +0.0437 | -0.0298 | +0.0412 | -0.0430 | matrix-7d-nereid-oslo 1930-11-24 00:00 |
| separation″ | +0.0437 | -0.0298 | -0.1762 | -1.2691 | matrix-7d-nereid-geocentric 2085-08-06 00:00 |

## Visibility states against Horizons' codes

Ours on Horizons' definition: limb to limb on Neptune's equatorial sphere and each moon's largest PCK radius, light time considered; the shadow at the moon's emission epoch, in Neptune's equatorial-sphere umbra. Horizons' codes: `t` transiting, `O` occulted, `p`/`u` partial/total umbral eclipse, `P`/`U` occulted while partially/totally eclipsed, `*` clear.

| moon | agreeing | disagreeing | Horizons' codes |
|---|---:|---:|---|
| triton | 21,300 | 0 | `*` 21246, `P` 4, `U` 22, `t` 28 |
| nereid | 21,300 | 0 | `*` 21300 |

## The planetary ephemeris, and the centre offset

| quantity | worst |
|---|---:|
| barycentre km | 2222.01 |
| earth km | 0.494644 |
| geocentric direction″ | 0.103654 |
| geocentric distance km | 530.572 |
| nep098 barycentre (DE442) vs DE440 km | 2222.01 |
| nep098 barycentre (DE442) vs de442s km | 0 |
| centre vs barycentre, geocentric direction, 1900-2100 every 6 h″ | 0.00354913 |

## The named events against Espenak's almanacs

The almanac gives each event to the hour (GMT). The models' instants are the geocentric apparent ecliptic longitude difference of date reaching 180° or 0°, to the second.

| event | almanac (UT) | barycentre model | minus almanac | centre on DE442 | minus almanac |
|---|---|---|---:|---|---:|
| conjunction 2003 | 2003-01-30 23:00 | 2003-01-30 23:34:24 | +34.4 min | 2003-01-30 23:34:24 | +34.4 min |
| opposition 2003 | 2003-08-04 12:00 | 2003-08-04 13:54:23 | +114.4 min | 2003-08-04 13:54:23 | +114.4 min |
| conjunction 2012 | 2012-02-19 20:00 | 2012-02-19 20:40:41 | +40.7 min | 2012-02-19 20:40:41 | +40.7 min |
| opposition 2012 | 2012-08-24 11:00 | 2012-08-24 12:31:38 | +91.6 min | 2012-08-24 12:31:38 | +91.6 min |
| conjunction 2018 | 2018-03-04 14:00 | 2018-03-04 13:53:53 | -6.1 min | 2018-03-04 13:53:53 | -6.1 min |
| opposition 2018 | 2018-09-07 17:00 | 2018-09-07 18:26:42 | +86.7 min | 2018-09-07 18:26:41 | +86.7 min |
| conjunction 2026 | 2026-03-22 10:00 | 2026-03-22 11:18:36 | +78.6 min | 2026-03-22 11:18:36 | +78.6 min |
| opposition 2026 | 2026-09-26 00:00 | 2026-09-26 01:36:21 | +96.4 min | 2026-09-26 01:36:21 | +96.4 min |
| conjunction 2027 | 2027-03-24 22:00 | 2027-03-24 23:12:41 | +72.7 min | 2027-03-24 23:12:41 | +72.7 min |
| opposition 2027 | 2027-09-28 13:00 | 2027-09-28 14:18:50 | +78.8 min | 2027-09-28 14:18:50 | +78.8 min |
| conjunction 2035 | 2035-04-12 02:00 | 2035-04-12 02:40:00 | +40.0 min | 2035-04-12 02:40:00 | +40.0 min |
| opposition 2035 | 2035-10-16 17:00 | 2035-10-16 17:57:38 | +57.6 min | 2035-10-16 17:57:38 | +57.6 min |
| opposition 2100 | 2100-03-06 12:00 | 2100-03-06 11:44:42 | -15.3 min | 2100-03-06 11:44:40 | -15.3 min |
| conjunction 2100 | 2100-09-09 15:00 | 2100-09-09 15:34:41 | +34.7 min | 2100-09-09 15:34:38 | +34.6 min |
