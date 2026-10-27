# #633 Uranus authority comparison

Skyfield on the pinned kernels against the kept JPL Horizons responses (Horizons: its own satellite solution for Uranus and the moons, DE441 for the Earth). The Earth and the Sun are DE440's (`de440s.bsp`). Observers at sea level on the WGS84 ellipsoid. Civil time: UT1 before 1972, UTC from 1972; the record's expiry, 2027-06-28, read from `Leap_Second.dat`, bounds the exact era.

Three Uranus models: **barycentre**, DE440's Uranus barycentre alone (0 -> 7); **centre**, the barycentre plus `ura184_part-3`'s 7 -> 799; **centre on DE442**, `ura184_part-3`'s own barycentre (DE442) plus its 7 -> 799. Every row 1900-2100 lies inside the kernel's span (JD 2305450.5 to 2597626.5).

Constants read from `pck00011.tpc`: Uranus's radii 25559, 25559, 24973 km; pole RA 257.311 +0.0 T, Dec -15.175 +0.0 T, with 0 periodic terms in RA and 0 in Dec; the moons' radii Miranda [240.4, 234.2, 232.9], Ariel [581.1, 577.9, 577.7], Umbriel [584.7, 584.7, 584.7], Titania [788.9, 788.9, 788.9], Oberon [761.4, 761.4, 761.4] km; the Sun's radius 695700 km.

## Uranus, barycentre model: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6390 rows) | after (7967 rows) | worst row |
|---|---:|---:|---:|---:|---|
| apparent, Sun deflection, beyond 1° from the Sun″ | +0.5973 | +0.1485 | +0.0827 | +0.4998 | matrix-7d-uranus-geocentric 1900-05-21 00:00 |
| apparent, Sun deflection″ | +2.3815 | +0.1485 | +2.6098 | +0.4998 | matrix-7d-uranus-oslo 1986-12-15 00:00 |
| apparent, no deflection″ | +1.7017 | +0.5787 | +0.9823 | +1.7447 | matrix-7d-uranus-geocentric 2065-11-23 00:00 |
| astrometric″ | +0.3529 | +0.1010 | +0.0588 | +0.3779 | matrix-7d-uranus-geocentric 2061-05-02 00:00 |
| diameter″ | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-uranus-oslo 2042-03-31 00:00 |
| distance km | +1137.6002 | +998.7771 | +1034.5059 | +1320.5473 | matrix-7d-uranus-geocentric 2040-04-16 00:00 |
| horizontal″ | +2.6723 | +2.0856 | +2.3276 | +408.6698 | named-uranus-alert 2100-12-31 23:59 |
| illuminated % | -0.0001 | -0.0001 | -0.0001 | -0.0001 | matrix-7d-uranus-oslo 2048-06-01 00:00 |
| illuminated, Sun light time and aberration % | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-uranus-geocentric 2054-01-26 00:00 |
| phase angle vs phi″ | +5.0562 | -5.0918 | -5.0585 | +5.1040 | matrix-7d-uranus-geocentric 2050-03-28 00:00 |
| phase angle, Sun light time and aberration, vs phi″ | -0.1965 | -0.1849 | -0.1830 | +0.1985 | matrix-7d-uranus-oslo 2060-05-03 00:00 |
| phase angle, Sun light time and aberration″ | +21.4569 | -20.6628 | +21.3025 | +21.3705 | matrix-7d-uranus-oslo 1902-12-15 00:00 |
| phase angle″ | +25.6990 | +25.5135 | -25.6152 | +25.7676 | matrix-7d-uranus-oslo 2037-01-12 00:00 |
| pole Dec, secular only″ | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-uranus-oslo 1900-01-01 00:00 |
| pole Dec, with periodic terms″ | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-uranus-oslo 1900-01-01 00:00 |
| pole RA, secular only″ | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-uranus-oslo 1900-01-01 00:00 |
| pole RA, with periodic terms″ | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-uranus-oslo 1900-01-01 00:00 |
| pole angle, secular only″ | +14.5731 | +0.2037 | +16.7893 | +2.9874 | matrix-7d-uranus-oslo 1986-12-15 00:00 |
| pole angle, with periodic terms″ | +14.5731 | +0.2037 | +16.7893 | +2.9874 | matrix-7d-uranus-oslo 1986-12-15 00:00 |
| sub-observer latitude, centric vs Horizons″ | +4784.0430 | +4231.2534 | -4783.7329 | +4784.0801 | matrix-7d-uranus-geocentric 2059-03-10 00:00 |
| sub-observer latitude, detic vs Horizons″ | +0.3657 | -0.1042 | -0.0485 | +0.3718 | matrix-7d-uranus-geocentric 2058-04-22 00:00 |

## Uranus, centre model: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6390 rows) | after (7967 rows) | worst row |
|---|---:|---:|---:|---:|---|
| apparent, Sun deflection, beyond 1° from the Sun″ | +0.5947 | +0.1486 | +0.0826 | +0.4993 | matrix-7d-uranus-geocentric 1900-05-21 00:00 |
| apparent, Sun deflection″ | +2.3809 | +0.1486 | +2.6096 | +0.4993 | matrix-7d-uranus-oslo 1986-12-15 00:00 |
| apparent, no deflection″ | +1.7019 | +0.5773 | +0.9849 | +1.7460 | matrix-7d-uranus-geocentric 2065-11-23 00:00 |
| astrometric″ | +0.3518 | +0.1006 | +0.0564 | +0.3763 | matrix-7d-uranus-geocentric 2061-05-02 00:00 |
| diameter″ | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-uranus-oslo 2038-03-08 00:00 |
| distance km | +1113.8607 | +965.0825 | +1027.1195 | +1300.6695 | matrix-7d-uranus-geocentric 2039-04-18 00:00 |
| horizontal″ | +2.6718 | +2.0856 | +2.3290 | +408.6685 | named-uranus-alert 2100-12-31 23:59 |
| illuminated % | -0.0001 | -0.0001 | -0.0001 | -0.0001 | matrix-7d-uranus-oslo 2048-06-01 00:00 |
| illuminated, Sun light time and aberration % | +0.0000 | +0.0000 | +0.0000 | +0.0000 | matrix-7d-uranus-geocentric 2054-01-26 00:00 |
| phase angle vs phi″ | +5.0562 | -5.0918 | -5.0584 | +5.1040 | matrix-7d-uranus-geocentric 2050-03-28 00:00 |
| phase angle, Sun light time and aberration, vs phi″ | -0.1964 | -0.1850 | -0.1832 | +0.1985 | matrix-7d-uranus-oslo 2060-05-03 00:00 |
| phase angle, Sun light time and aberration″ | +21.4567 | -20.6627 | +21.3026 | +21.3705 | matrix-7d-uranus-oslo 1902-12-15 00:00 |
| phase angle″ | +25.6991 | +25.5135 | -25.6152 | +25.7675 | matrix-7d-uranus-oslo 2037-01-12 00:00 |

## Uranus, centre on DE442 model: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (6475 rows) | 1962-1971 (1049 rows) | 1972-2027-06 (6390 rows) | after (7967 rows) | worst row |
|---|---:|---:|---:|---:|---|
| apparent, Sun deflection, beyond 1° from the Sun″ | +0.2452 | +0.0536 | +0.0525 | +0.1918 | matrix-7d-uranus-geocentric 1900-04-23 00:00 |
| apparent, Sun deflection″ | +2.1415 | +0.0536 | +2.6096 | +0.1918 | matrix-7d-uranus-oslo 1986-12-15 00:00 |
| apparent, no deflection″ | +1.5845 | +0.5558 | +0.9621 | +1.4477 | matrix-7d-uranus-oslo 1940-05-13 00:00 |
| astrometric″ | +0.0001 | +0.0018 | +0.0000 | +0.0009 | matrix-7d-uranus-geocentric 1965-09-06 00:00 |
| diameter″ | -0.0000 | -0.0000 | -0.0000 | +0.0000 | matrix-7d-uranus-geocentric 1963-12-09 00:00 |
| distance km | -0.9365 | +17.3032 | +0.0620 | -10.3587 | matrix-7d-uranus-geocentric 1965-12-06 00:00 |
| horizontal″ | +2.4324 | +2.0998 | +2.2738 | +408.9421 | named-uranus-alert 2100-12-31 23:59 |
| illuminated % | -0.0001 | -0.0001 | -0.0001 | -0.0001 | matrix-7d-uranus-oslo 2048-06-01 00:00 |
| illuminated, Sun light time and aberration % | +0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-uranus-oslo 2100-02-22 00:00 |
| phase angle vs phi″ | +5.0570 | +5.0906 | -5.0574 | -5.0916 | matrix-7d-uranus-geocentric 2049-12-13 00:00 |
| phase angle, Sun light time and aberration, vs phi″ | +0.1800 | +0.1809 | -0.1800 | +0.1800 | matrix-7d-uranus-geocentric 1966-03-07 00:00 |
| phase angle, Sun light time and aberration″ | +21.4725 | -20.6676 | +21.3023 | +21.3533 | matrix-7d-uranus-oslo 1902-12-15 00:00 |
| phase angle″ | +25.7129 | +25.5189 | -25.6123 | +25.7591 | matrix-7d-uranus-oslo 2037-01-12 00:00 |

## Miranda: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (3240 rows) | 1962-1971 (527 rows) | 1972-2027-06 (4639 rows) | after (7676 rows) | worst row |
|---|---:|---:|---:|---:|---|
| X apparent, beyond 1° from the Sun″ | +0.0005 | -0.0006 | -0.0005 | -0.0005 | matrix-7d-miranda-geocentric 1963-11-04 00:00 |
| X apparent″ | +0.0122 | -0.0006 | -0.0356 | -0.0169 | matrix-7d-miranda-geocentric 1986-12-15 00:00 |
| Y apparent, beyond 1° from the Sun″ | -0.0005 | +0.0008 | -0.0005 | -0.0005 | matrix-7d-miranda-geocentric 1965-03-01 00:00 |
| Y apparent″ | -0.0202 | +0.0008 | -0.0152 | +0.0062 | matrix-7d-miranda-geocentric 1945-06-04 00:00 |
| apparent, beyond 1° from the Sun″ | +0.5947 | +0.1486 | +0.0826 | +0.4993 | matrix-7d-miranda-geocentric 1900-05-21 00:00 |
| apparent″ | +2.3908 | +0.1486 | +2.6369 | +1.7287 | matrix-7d-miranda-geocentric 1986-12-15 00:00 |
| astrometric″ | +0.3518 | +0.1006 | +0.0569 | +0.3763 | matrix-7d-miranda-geocentric 2061-05-02 00:00 |
| diameter, largest radius″ | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-miranda-geocentric 2042-04-14 00:00 |
| distance km | +1113.7920 | +967.9944 | +1027.1121 | +1300.6701 | matrix-7d-miranda-oslo 2039-04-16 00:00 |
| horizontal″ | +1.2905 | +1.0893 | +2.3320 | +408.6661 | named-miranda-alert 2100-12-31 23:59 |
| position angle, beyond 1° from the Sun° | -0.0022 | +0.0273 | +0.0011 | +0.0059 | matrix-7d-miranda-geocentric 1965-10-18 00:00 |
| position angle, beyond 5″ of Uranus° | -0.1176 | +0.0020 | -0.1199 | -0.1145 | matrix-7d-miranda-geocentric 1986-12-15 00:00 |
| position angle, within 5″ of Uranus° | -0.0022 | +0.0273 | -0.0008 | +0.0059 | matrix-7d-miranda-geocentric 1965-10-18 00:00 |
| position angle° | -0.1176 | +0.0273 | -0.1199 | -0.1145 | matrix-7d-miranda-geocentric 1986-12-15 00:00 |
| separation, beyond 1° from the Sun″ | +0.0010 | +0.0010 | -0.0010 | +0.0011 | matrix-7d-miranda-oslo 2062-11-11 00:00 |
| separation″ | +0.0102 | +0.0015 | -0.0065 | +0.0098 | matrix-7d-miranda-geocentric 1940-05-13 00:00 |

## Ariel: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (3240 rows) | 1962-1971 (527 rows) | 1972-2027-06 (4639 rows) | after (7676 rows) | worst row |
|---|---:|---:|---:|---:|---|
| X apparent, beyond 1° from the Sun″ | -0.0005 | -0.0006 | -0.0005 | -0.0005 | matrix-7d-ariel-geocentric 1964-09-21 00:00 |
| X apparent″ | -0.0178 | -0.0006 | +0.0499 | -0.0068 | matrix-7d-ariel-geocentric 1986-12-15 00:00 |
| Y apparent, beyond 1° from the Sun″ | +0.0005 | +0.0007 | -0.0005 | -0.0005 | matrix-7d-ariel-geocentric 1966-04-25 00:00 |
| Y apparent″ | +0.0289 | +0.0007 | +0.0225 | -0.0255 | matrix-7d-ariel-geocentric 1945-06-04 00:00 |
| apparent, beyond 1° from the Sun″ | +0.5946 | +0.1486 | +0.0826 | +0.4993 | matrix-7d-ariel-geocentric 1900-05-21 00:00 |
| apparent″ | +2.3664 | +0.1486 | +2.5735 | +1.7210 | matrix-7d-ariel-geocentric 1986-12-15 00:00 |
| astrometric″ | +0.3518 | +0.1006 | +0.0569 | +0.3764 | matrix-7d-ariel-geocentric 2061-05-02 00:00 |
| diameter, largest radius″ | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-ariel-oslo 2040-02-11 00:00 |
| distance km | +1113.7954 | +967.0001 | +1027.1707 | +1300.6989 | matrix-7d-ariel-geocentric 2039-04-18 00:00 |
| horizontal″ | +1.2905 | +1.0893 | +2.3320 | +408.6679 | named-ariel-alert 2100-12-31 23:59 |
| position angle, beyond 1° from the Sun° | -0.0016 | +0.0247 | -0.0014 | -0.0048 | matrix-7d-ariel-geocentric 1966-04-25 00:00 |
| position angle, beyond 5″ of Uranus° | -0.1160 | +0.0022 | -0.2069 | +0.1173 | matrix-7d-ariel-geocentric 1986-12-15 00:00 |
| position angle, within 5″ of Uranus° | -0.0016 | +0.0247 | -0.0006 | -0.0048 | matrix-7d-ariel-geocentric 1966-04-25 00:00 |
| position angle° | -0.1160 | +0.0247 | -0.2069 | +0.1173 | matrix-7d-ariel-geocentric 1986-12-15 00:00 |
| separation, beyond 1° from the Sun″ | +0.0014 | +0.0015 | +0.0015 | +0.0015 | matrix-7d-ariel-oslo 2062-11-11 00:00 |
| separation″ | -0.0173 | +0.0015 | -0.0077 | -0.0160 | matrix-7d-ariel-geocentric 1940-05-13 00:00 |

## Umbriel: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (3240 rows) | 1962-1971 (527 rows) | 1972-2027-06 (4639 rows) | after (7676 rows) | worst row |
|---|---:|---:|---:|---:|---|
| X apparent, beyond 1° from the Sun″ | +0.0005 | -0.0006 | -0.0005 | -0.0005 | matrix-7d-umbriel-geocentric 1962-11-26 00:00 |
| X apparent″ | +0.0390 | -0.0006 | +0.0711 | -0.0094 | matrix-7d-umbriel-geocentric 1986-12-15 00:00 |
| Y apparent, beyond 1° from the Sun″ | +0.0005 | +0.0007 | +0.0005 | -0.0005 | matrix-7d-umbriel-geocentric 1965-10-04 00:00 |
| Y apparent″ | +0.0284 | +0.0007 | -0.0244 | +0.0368 | matrix-7d-umbriel-oslo 2031-06-14 00:00 |
| apparent, beyond 1° from the Sun″ | +0.5947 | +0.1486 | +0.0826 | +0.4993 | matrix-7d-umbriel-geocentric 1900-05-21 00:00 |
| apparent″ | +2.4226 | +0.1486 | +2.5587 | +1.7517 | matrix-7d-umbriel-geocentric 1986-12-15 00:00 |
| astrometric″ | +0.3518 | +0.1006 | +0.0569 | +0.3763 | matrix-7d-umbriel-geocentric 2061-05-02 00:00 |
| diameter, largest radius″ | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-umbriel-oslo 2040-04-14 00:00 |
| distance km | +1114.0456 | +966.9654 | +1027.1558 | +1300.6931 | matrix-7d-umbriel-geocentric 2039-04-18 00:00 |
| horizontal″ | +1.2905 | +1.0893 | +2.3321 | +408.6635 | named-umbriel-alert 2100-12-31 23:59 |
| position angle, beyond 1° from the Sun° | +0.0020 | +0.0147 | +0.0017 | +0.0052 | matrix-7d-umbriel-geocentric 1966-05-30 00:00 |
| position angle, beyond 5″ of Uranus° | +0.1351 | +0.0016 | -0.2223 | +0.0671 | matrix-7d-umbriel-geocentric 1986-12-15 00:00 |
| position angle, within 5″ of Uranus° | +0.0020 | +0.0147 | +0.0006 | +0.0052 | matrix-7d-umbriel-geocentric 1966-05-30 00:00 |
| position angle° | +0.1351 | +0.0147 | -0.2223 | +0.0671 | matrix-7d-umbriel-geocentric 1986-12-15 00:00 |
| separation, beyond 1° from the Sun″ | -0.0020 | +0.0020 | +0.0020 | +0.0021 | matrix-7d-umbriel-geocentric 2052-12-23 00:00 |
| separation″ | -0.0225 | +0.0020 | +0.0097 | -0.0204 | matrix-7d-umbriel-geocentric 1940-05-13 00:00 |

## Titania: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (3240 rows) | 1962-1971 (527 rows) | 1972-2027-06 (4639 rows) | after (7676 rows) | worst row |
|---|---:|---:|---:|---:|---|
| X apparent, beyond 1° from the Sun″ | -0.0005 | +0.0005 | -0.0005 | -0.0005 | matrix-7d-titania-geocentric 1914-07-27 00:00 |
| X apparent″ | -0.0743 | +0.0005 | +1.0293 | -0.0589 | matrix-7d-titania-geocentric 1981-11-23 00:00 |
| Y apparent, beyond 1° from the Sun″ | -0.0005 | +0.0006 | +0.0005 | +0.0005 | matrix-7d-titania-geocentric 1965-11-29 00:00 |
| Y apparent″ | -0.0151 | +0.0006 | -1.3243 | -0.0053 | matrix-7d-titania-geocentric 1981-11-23 00:00 |
| apparent, beyond 1° from the Sun″ | +0.5947 | +0.1486 | +0.0826 | +0.4993 | matrix-7d-titania-geocentric 1900-05-21 00:00 |
| apparent″ | +2.3057 | +0.1486 | +2.7323 | +1.6875 | matrix-7d-titania-geocentric 1986-12-15 00:00 |
| astrometric″ | +0.3518 | +0.1006 | +0.0569 | +0.3763 | matrix-7d-titania-geocentric 2061-05-02 00:00 |
| diameter, largest radius″ | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-titania-oslo 2037-01-24 00:00 |
| distance km | +1113.8435 | +966.7437 | +1027.0884 | +1300.6828 | matrix-7d-titania-geocentric 2039-04-18 00:00 |
| horizontal″ | +1.2906 | +1.0893 | +2.3322 | +408.6636 | named-titania-alert 2100-12-31 23:59 |
| position angle, beyond 1° from the Sun° | +0.0027 | +0.0045 | +0.0026 | +0.0026 | matrix-7d-titania-geocentric 1966-05-09 00:00 |
| position angle, beyond 5″ of Uranus° | +0.0330 | +0.0015 | -0.3017 | -0.0892 | matrix-7d-titania-geocentric 1981-11-23 00:00 |
| position angle, within 5″ of Uranus° | +0.0019 | +0.0045 | +0.0006 | -0.0021 | matrix-7d-titania-geocentric 1966-05-09 00:00 |
| position angle° | +0.0330 | +0.0045 | -0.3017 | -0.0892 | matrix-7d-titania-geocentric 1981-11-23 00:00 |
| separation, beyond 1° from the Sun″ | +0.0033 | +0.0033 | +0.0033 | +0.0034 | matrix-7d-titania-oslo 2056-01-15 00:00 |
| separation″ | +0.0430 | +0.0048 | -1.6196 | -0.0319 | matrix-7d-titania-geocentric 1981-11-23 00:00 |

## Oberon: worst absolute difference from Horizons, by era

| quantity | 1900-1961 (3240 rows) | 1962-1971 (527 rows) | 1972-2027-06 (4639 rows) | after (7676 rows) | worst row |
|---|---:|---:|---:|---:|---|
| X apparent, beyond 1° from the Sun″ | -0.0006 | -0.0005 | -0.0005 | -0.0006 | matrix-7d-oberon-oslo 2054-05-23 00:00 |
| X apparent″ | +0.0174 | -0.0005 | -0.0719 | -0.0756 | matrix-7d-oberon-oslo 2031-06-14 00:00 |
| Y apparent, beyond 1° from the Sun″ | +0.0005 | -0.0006 | +0.0005 | -0.0005 | matrix-7d-oberon-geocentric 1966-04-11 00:00 |
| Y apparent″ | +0.1041 | -0.0006 | +0.1549 | -0.0173 | matrix-7d-oberon-geocentric 1986-12-15 00:00 |
| apparent, beyond 1° from the Sun″ | +0.5947 | +0.1486 | +0.0826 | +0.4993 | matrix-7d-oberon-geocentric 1900-05-21 00:00 |
| apparent″ | +2.4098 | +0.1486 | +2.4686 | +1.6664 | matrix-7d-oberon-geocentric 1986-12-15 00:00 |
| astrometric″ | +0.3518 | +0.1006 | +0.0569 | +0.3763 | matrix-7d-oberon-geocentric 2061-04-25 00:00 |
| diameter, largest radius″ | -0.0000 | -0.0000 | -0.0000 | -0.0000 | matrix-7d-oberon-oslo 2036-04-19 00:00 |
| distance km | +1114.2829 | +965.3130 | +1027.1251 | +1300.7066 | matrix-7d-oberon-geocentric 2039-04-11 00:00 |
| horizontal″ | +1.2905 | +1.0893 | +2.3321 | +408.6680 | named-oberon-alert 2100-12-31 23:59 |
| position angle, beyond 1° from the Sun° | +0.0033 | +0.0062 | +0.0032 | +0.0034 | matrix-7d-oberon-geocentric 1966-04-18 00:00 |
| position angle, beyond 5″ of Uranus° | +0.0795 | +0.0015 | +0.2262 | -0.0664 | matrix-7d-oberon-geocentric 1986-12-15 00:00 |
| position angle, within 5″ of Uranus° | +0.0024 | +0.0062 | +0.0011 | +0.0032 | matrix-7d-oberon-geocentric 1966-04-18 00:00 |
| position angle° | +0.0795 | +0.0062 | +0.2262 | -0.0664 | matrix-7d-oberon-geocentric 1986-12-15 00:00 |
| separation, beyond 1° from the Sun″ | +0.0044 | +0.0044 | +0.0044 | -0.0045 | matrix-7d-oberon-oslo 2062-11-11 00:00 |
| separation″ | -0.0315 | +0.0044 | -0.0253 | +0.0382 | matrix-7d-oberon-oslo 2067-12-03 00:00 |

## Visibility states against Horizons' codes

Ours on Horizons' definition: limb to limb on Uranus's equatorial sphere and each moon's largest PCK radius, light time considered; the shadow at the moon's emission epoch, in Uranus's equatorial-sphere umbra. Horizons' codes: `t` transiting, `O` occulted, `p`/`u` partial/total umbral eclipse, `P`/`U` occulted while partially/totally eclipsed, `*` clear.

| moon | agreeing | disagreeing | Horizons' codes |
|---|---:|---:|---|
| miranda | 16,082 | 0 | `*` 15838, `O` 8, `P` 4, `U` 97, `p` 4, `t` 122, `u` 9 |
| ariel | 16,082 | 0 | `*` 15965, `O` 8, `P` 2, `U` 50, `p` 2, `t` 49, `u` 6 |
| umbriel | 16,082 | 0 | `*` 16022, `O` 5, `U` 22, `t` 27, `u` 6 |
| titania | 16,082 | 0 | `*` 16052, `O` 2, `P` 1, `U` 8, `p` 1, `t` 13, `u` 5 |
| oberon | 16,082 | 0 | `*` 16072, `t` 10 |

## The planetary ephemeris, and the centre offset

| quantity | worst |
|---|---:|
| barycentre km | 4864.7 |
| earth km | 0.494644 |
| geocentric direction″ | 0.376358 |
| geocentric distance km | 1300.73 |
| ura184 barycentre (DE442) vs DE440 km | 4864.7 |
| ura184 barycentre (DE442) vs de442s km | 0 |
| centre vs barycentre, geocentric direction, 1900-2100 every 6 h″ | 0.00352964 |

## The named events against Espenak's almanacs

The almanac gives each event to the hour (GMT). The models' instants are the geocentric apparent ecliptic longitude difference of date reaching 180° or 0°, to the second.

| event | almanac (UT) | barycentre model | minus almanac | centre on DE442 | minus almanac |
|---|---|---|---:|---|---:|
| conjunction 2003 | 2003-02-17 21:00 | 2003-02-17 21:37:33 | +37.6 min | 2003-02-17 21:37:32 | +37.5 min |
| opposition 2003 | 2003-08-24 10:00 | 2003-08-24 10:02:19 | +2.3 min | 2003-08-24 10:02:18 | +2.3 min |
| conjunction 2012 | 2012-03-24 18:00 | 2012-03-24 18:20:05 | +20.1 min | 2012-03-24 18:20:04 | +20.1 min |
| opposition 2012 | 2012-09-29 07:00 | 2012-09-29 07:14:48 | +14.8 min | 2012-09-29 07:14:47 | +14.8 min |
| conjunction 2018 | 2018-04-18 15:00 | 2018-04-18 14:00:09 | -59.8 min | 2018-04-18 14:00:09 | -59.8 min |
| opposition 2018 | 2018-10-24 01:00 | 2018-10-24 00:46:44 | -13.3 min | 2018-10-24 00:46:44 | -13.3 min |
| conjunction 2026 | 2026-05-22 16:00 | 2026-05-22 14:26:16 | -93.7 min | 2026-05-22 14:26:17 | -93.7 min |
| opposition 2026 | 2026-11-25 23:00 | 2026-11-25 22:41:17 | -18.7 min | 2026-11-25 22:41:19 | -18.7 min |
| conjunction 2027 | 2027-05-27 08:00 | 2027-05-27 06:11:44 | -108.3 min | 2027-05-27 06:11:46 | -108.2 min |
| opposition 2027 | 2027-11-30 10:00 | 2027-11-30 09:21:53 | -38.1 min | 2027-11-30 09:21:55 | -38.1 min |
| conjunction 2035 | 2035-07-02 23:00 | 2035-07-02 19:30:08 | -209.9 min | 2035-07-02 19:30:11 | -209.8 min |
| conjunction 2100 | 2100-04-11 10:00 | 2100-04-11 10:16:32 | +16.5 min | 2100-04-11 10:16:39 | +16.7 min |
| opposition 2100 | 2100-10-16 23:00 | 2100-10-16 23:16:47 | +16.8 min | 2100-10-16 23:16:54 | +16.9 min |
