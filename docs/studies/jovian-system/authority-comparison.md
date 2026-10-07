# #472 Jovian authority comparison, measured 2026-10-07 14:13Z

Skyfield on `jup365.bsp` (JUP365 with DE440) against the kept JPL Horizons responses under `the study area (the committed subset is `horizons/`; the 7-day matrices are retained locally)` (Horizons: jup365 merged, DE441). Observers at sea level.

## Jupiter: worst absolute difference by era

| era | rows | astrometric | apparent | horizontal | distance | diameter (asin) | diameter (atan) | phase angle | illuminated |
|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 6475 | 0.004″ | 1.099″ | 1.10″ | -0.8 km | +0.0000″ | -0.0000″ | -30.09″ | +0.0005 % |
| 1962-1971 | 1049 | 0.008″ | 0.528″ | 1.92″ | -18.0 km | +0.0000″ | -0.0000″ | -29.94″ | +0.0005 % |
| 1972-2027-06 | 7792 | 0.003″ | 1.041″ | 2.84″ | -0.1 km | +0.0000″ | -0.0000″ | -30.09″ | +0.0005 % |
| after | 7677 | 0.003″ | 1.303″ | 405.97″ | +9.8 km | -0.0000″ | -0.0000″ | +30.06″ | +0.0005 % |

## Io: worst absolute difference by era

| era | rows | astrometric | apparent | horizontal | distance | X (apparent basis) | Y (apparent basis) | X (astrometric basis) | Y (astrometric basis) | position angle | separation |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 6475 | 0.004″ | 1.071″ | 1.10″ | -1.3 km | -0.065″ | +0.047″ | +0.317″ | +1.339″ | -0.0281° | -0.074″ |
| 1962-1971 | 1049 | 0.009″ | 0.517″ | 1.92″ | +26.4 km | +0.007″ | -0.016″ | +0.109″ | +0.480″ | +0.0394° | -0.013″ |
| 1972-2027-06 | 7792 | 0.004″ | 1.004″ | 2.84″ | -0.1 km | +0.030″ | -0.039″ | +0.081″ | +0.389″ | -0.0310° | +0.027″ |
| after | 7677 | 0.004″ | 1.356″ | 405.96″ | +9.8 km | -0.044″ | +0.052″ | -0.277″ | +1.332″ | +0.0625° | +0.045″ |

## Europa: worst absolute difference by era

| era | rows | astrometric | apparent | horizontal | distance | X (apparent basis) | Y (apparent basis) | X (astrometric basis) | Y (astrometric basis) | position angle | separation |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 6475 | 0.004″ | 1.134″ | 1.12″ | -1.3 km | +0.112″ | -0.056″ | +0.480″ | +2.204″ | +0.0212° | -0.121″ |
| 1962-1971 | 1049 | 0.008″ | 0.513″ | 1.92″ | +26.5 km | -0.012″ | -0.023″ | -0.167″ | +0.713″ | -0.0519° | -0.020″ |
| 1972-2027-06 | 7792 | 0.004″ | 1.029″ | 2.84″ | -0.1 km | +0.037″ | +0.041″ | +0.132″ | -0.600″ | -0.0196° | +0.037″ |
| after | 7677 | 0.004″ | 1.346″ | 405.95″ | +9.8 km | +0.089″ | +0.077″ | -0.442″ | -2.155″ | +0.0664° | +0.091″ |

## Ganymede: worst absolute difference by era

| era | rows | astrometric | apparent | horizontal | distance | X (apparent basis) | Y (apparent basis) | X (astrometric basis) | Y (astrometric basis) | position angle | separation |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 6475 | 0.003″ | 1.025″ | 1.12″ | -1.1 km | -0.139″ | +0.122″ | +0.807″ | -3.226″ | -0.0236° | -0.163″ |
| 1962-1971 | 1049 | 0.008″ | 0.510″ | 1.92″ | +25.2 km | +0.019″ | -0.028″ | +0.261″ | +1.109″ | -0.0194° | -0.031″ |
| 1972-2027-06 | 7792 | 0.004″ | 1.255″ | 2.84″ | -0.1 km | -0.165″ | -0.178″ | +0.209″ | +0.902″ | -0.0619° | +0.137″ |
| after | 7677 | 0.004″ | 1.476″ | 405.97″ | +9.8 km | +0.118″ | +0.196″ | -0.693″ | +3.308″ | +0.0797° | +0.120″ |

## Callisto: worst absolute difference by era

| era | rows | astrometric | apparent | horizontal | distance | X (apparent basis) | Y (apparent basis) | X (astrometric basis) | Y (astrometric basis) | position angle | separation |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 6475 | 0.003″ | 1.010″ | 1.12″ | -1.1 km | -0.210″ | +0.237″ | -1.382″ | -6.102″ | +0.0366° | -0.255″ |
| 1962-1971 | 1049 | 0.008″ | 0.541″ | 1.92″ | +23.4 km | +0.042″ | -0.033″ | +0.494″ | +2.206″ | -0.0360° | -0.055″ |
| 1972-2027-06 | 7792 | 0.003″ | 1.414″ | 2.84″ | -0.1 km | -0.263″ | -0.345″ | +0.379″ | +1.721″ | -0.0801° | +0.203″ |
| after | 7677 | 0.004″ | 1.367″ | 405.98″ | +9.8 km | +0.114″ | -0.161″ | -1.253″ | +5.922″ | +0.0548° | +0.117″ |

## Visibility states against Horizons' codes (limb-to-limb, equatorial radii)

| moon | rows agreeing | rows disagreeing |
|---|---|---|
| io | 22993 | 0 |
| europa | 22992 | 1 |
| ganymede | 22993 | 0 |
| callisto | 22993 | 0 |

Disagreements, europa (1):
- oslo 2026-12-06 17:00: ours * (clear, lit, depth +660838 km, sep 20.37") vs Horizons O (sep 20.3635)

## The named evening, 11 December 2026: every state transition, minute series

**Io, geocentric:**
- 18:00 Horizons -> *
- 18:00 ours     -> *
- 22:32 Horizons -> t
- 22:32 ours     -> t
- 00:53 Horizons -> *
- 00:53 ours     -> *

**Europa, geocentric:**
- 18:00 Horizons -> *
- 18:00 ours     -> *
- 21:52 Horizons -> t
- 21:52 ours     -> t
- 00:50 Horizons -> *
- 00:50 ours     -> *

**Ganymede, geocentric:**
- 18:00 Horizons -> *
- 18:00 ours     -> *

**Callisto, geocentric:**
- 18:00 Horizons -> *
- 18:00 ours     -> *
- 18:08 Horizons -> t
- 18:08 ours     -> t
- 22:59 Horizons -> *
- 22:59 ours     -> *

**Io, oslo:**
- 18:00 Horizons -> *
- 18:00 ours     -> *
- 22:32 Horizons -> t
- 22:32 ours     -> t
- 00:53 Horizons -> *
- 00:53 ours     -> *

**Europa, oslo:**
- 18:00 Horizons -> *
- 18:00 ours     -> *
- 21:52 Horizons -> t
- 21:52 ours     -> t
- 00:50 Horizons -> *
- 00:50 ours     -> *

**Ganymede, oslo:**
- 18:00 Horizons -> *
- 18:00 ours     -> *

**Callisto, oslo:**
- 18:00 Horizons -> *
- 18:00 ours     -> *
- 18:08 Horizons -> t
- 18:08 ours     -> t
- 22:59 Horizons -> *
- 22:59 ours     -> *

Where the worst rows are:
- callisto, 1900-1961, X apparent basis: -0.21006 at oslo 1919-07-21 00:00
- callisto, 1900-1961, X astrometric basis: -1.38236 at oslo 1902-08-18 00:00
- callisto, 1900-1961, Y apparent basis: 0.23710 at geocentric 1919-07-21 00:00
- callisto, 1900-1961, Y astrometric basis: -6.10181 at oslo 1901-08-05 00:00
- callisto, 1900-1961, apparent: 1.00996 at oslo 1906-06-11 00:00
- callisto, 1900-1961, astrometric: 0.00307 at geocentric 1900-06-18 00:00
- callisto, 1900-1961, distance km: -1.07110 at oslo 1900-09-03 00:00
- callisto, 1900-1961, horizontal: 1.12446 at oslo 1906-06-11 00:00
- callisto, 1900-1961, position angle deg: 0.03659 at geocentric 1913-06-23 00:00
- callisto, 1900-1961, separation arcsec: -0.25509 at geocentric 1919-07-21 00:00
- callisto, 1962-1971, X apparent basis: 0.04193 at geocentric 1970-11-09 00:00
- callisto, 1962-1971, X astrometric basis: 0.49437 at oslo 1964-10-12 00:00
- callisto, 1962-1971, Y apparent basis: -0.03302 at oslo 1970-11-09 00:00
- callisto, 1962-1971, Y astrometric basis: 2.20618 at oslo 1965-12-13 00:00
- callisto, 1962-1971, apparent: 0.54064 at oslo 1965-05-31 00:00
- callisto, 1962-1971, astrometric: 0.00753 at geocentric 1965-04-26 00:00
- callisto, 1962-1971, distance km: 23.35629 at oslo 1965-09-13 00:00
- callisto, 1962-1971, horizontal: 1.91871 at oslo 1971-12-27 00:00
- callisto, 1962-1971, position angle deg: -0.03601 at oslo 1965-12-13 00:00
- callisto, 1962-1971, separation arcsec: -0.05515 at oslo 1963-12-23 00:00
- callisto, 1972-2027-06, X apparent basis: -0.26255 at geocentric 1978-07-10 00:00
- callisto, 1972-2027-06, X astrometric basis: 0.37913 at oslo 1973-08-27 00:00
- callisto, 1972-2027-06, Y apparent basis: -0.34457 at oslo 1978-07-10 00:00
- callisto, 1972-2027-06, Y astrometric basis: 1.72056 at geocentric 1972-06-26 00:00
- callisto, 1972-2027-06, apparent: 1.41391 at geocentric 1978-07-10 00:00
- callisto, 1972-2027-06, astrometric: 0.00307 at oslo 2026-12-01 05:00
- callisto, 1972-2027-06, distance km: -0.07026 at cape-town 2026-12-11 22:30
- callisto, 1972-2027-06, horizontal: 2.83829 at cape-town 2026-12-11 22:55
- callisto, 1972-2027-06, position angle deg: -0.08014 at geocentric 1978-07-10 00:00
- callisto, 1972-2027-06, separation arcsec: 0.20253 at geocentric 1978-07-10 00:00
- callisto, after, X apparent basis: 0.11395 at oslo 2049-07-05 00:00
- callisto, after, X astrometric basis: -1.25305 at geocentric 2098-01-20 00:00
- callisto, after, Y apparent basis: -0.16093 at geocentric 2066-12-13 00:00
- callisto, after, Y astrometric basis: 5.92176 at oslo 2097-01-07 00:00
- callisto, after, apparent: 1.36727 at oslo 2056-01-10 00:00
- callisto, after, astrometric: 0.00435 at geocentric 2032-08-16 00:00
- callisto, after, distance km: 9.77814 at cape-town 2100-12-31 23:59
- callisto, after, horizontal: 405.97942 at oslo 2100-07-26 00:00
- callisto, after, position angle deg: 0.05478 at geocentric 2056-01-10 00:00
- callisto, after, separation arcsec: 0.11672 at geocentric 2049-07-05 00:00
- europa, 1900-1961, X apparent basis: 0.11191 at geocentric 1919-07-21 00:00
- europa, 1900-1961, X astrometric basis: 0.48034 at oslo 1902-07-14 00:00
- europa, 1900-1961, Y apparent basis: -0.05607 at geocentric 1919-07-21 00:00
- europa, 1900-1961, Y astrometric basis: 2.20403 at geocentric 1901-08-05 00:00
- europa, 1900-1961, apparent: 1.13433 at geocentric 1919-07-21 00:00
- europa, 1900-1961, astrometric: 0.00435 at geocentric 1912-11-25 00:00
- europa, 1900-1961, distance km: -1.27218 at oslo 1900-08-27 00:00
- europa, 1900-1961, horizontal: 1.12371 at oslo 1919-07-21 00:00
- europa, 1900-1961, position angle deg: 0.02120 at oslo 1906-06-11 00:00
- europa, 1900-1961, separation arcsec: -0.12134 at geocentric 1919-07-21 00:00
- europa, 1962-1971, X apparent basis: -0.01225 at geocentric 1970-11-09 00:00
- europa, 1962-1971, X astrometric basis: -0.16736 at oslo 1964-11-09 00:00
- europa, 1962-1971, Y apparent basis: -0.02295 at geocentric 1965-05-31 00:00
- europa, 1962-1971, Y astrometric basis: 0.71322 at oslo 1966-02-07 00:00
- europa, 1962-1971, apparent: 0.51343 at oslo 1965-05-31 00:00
- europa, 1962-1971, astrometric: 0.00813 at geocentric 1965-05-03 00:00
- europa, 1962-1971, distance km: 26.49370 at oslo 1965-09-27 00:00
- europa, 1962-1971, horizontal: 1.91898 at oslo 1971-12-27 00:00
- europa, 1962-1971, position angle deg: -0.05189 at oslo 1966-11-14 00:00
- europa, 1962-1971, separation arcsec: -0.01960 at oslo 1963-12-09 00:00
- europa, 1972-2027-06, X apparent basis: 0.03697 at oslo 2007-12-24 00:00
- europa, 1972-2027-06, X astrometric basis: 0.13208 at geocentric 1973-09-03 00:00
- europa, 1972-2027-06, Y apparent basis: 0.04105 at oslo 2026-07-29 00:00
- europa, 1972-2027-06, Y astrometric basis: -0.59951 at geocentric 1972-05-29 00:00
- europa, 1972-2027-06, apparent: 1.02863 at geocentric 1978-07-10 00:00
- europa, 1972-2027-06, astrometric: 0.00435 at geocentric 1972-08-21 00:00
- europa, 1972-2027-06, distance km: -0.07027 at cape-town 2026-12-11 22:30
- europa, 1972-2027-06, horizontal: 2.83833 at cape-town 2026-12-11 22:55
- europa, 1972-2027-06, position angle deg: -0.01960 at oslo 1997-01-20 00:00
- europa, 1972-2027-06, separation arcsec: 0.03669 at geocentric 2007-12-24 00:00
- europa, after, X apparent basis: 0.08942 at geocentric 2049-07-05 00:00
- europa, after, X astrometric basis: -0.44170 at geocentric 2092-09-01 00:00
- europa, after, Y apparent basis: 0.07695 at oslo 2096-06-18 00:00
- europa, after, Y astrometric basis: -2.15463 at geocentric 2097-01-21 00:00
- europa, after, apparent: 1.34571 at oslo 2056-01-10 00:00
- europa, after, astrometric: 0.00435 at geocentric 2036-11-03 00:00
- europa, after, distance km: 9.77944 at cape-town 2100-12-31 23:59
- europa, after, horizontal: 405.95263 at oslo 2100-07-26 00:00
- europa, after, position angle deg: 0.06638 at geocentric 2056-01-10 00:00
- europa, after, separation arcsec: 0.09125 at geocentric 2049-07-05 00:00
- ganymede, 1900-1961, X apparent basis: -0.13929 at geocentric 1919-07-21 00:00
- ganymede, 1900-1961, X astrometric basis: 0.80717 at oslo 1902-08-04 00:00
- ganymede, 1900-1961, Y apparent basis: 0.12205 at geocentric 1919-07-21 00:00
- ganymede, 1900-1961, Y astrometric basis: -3.22565 at oslo 1907-02-11 00:00
- ganymede, 1900-1961, apparent: 1.02493 at geocentric 1919-07-21 00:00
- ganymede, 1900-1961, astrometric: 0.00307 at geocentric 1900-02-05 00:00
- ganymede, 1900-1961, distance km: -1.13862 at oslo 1901-10-07 00:00
- ganymede, 1900-1961, horizontal: 1.12004 at oslo 1906-06-11 00:00
- ganymede, 1900-1961, position angle deg: -0.02360 at oslo 1906-06-11 00:00
- ganymede, 1900-1961, separation arcsec: -0.16292 at geocentric 1919-07-21 00:00
- ganymede, 1962-1971, X apparent basis: 0.01946 at geocentric 1968-09-09 00:00
- ganymede, 1962-1971, X astrometric basis: 0.26115 at geocentric 1964-11-23 00:00
- ganymede, 1962-1971, Y apparent basis: -0.02788 at oslo 1965-05-31 00:00
- ganymede, 1962-1971, Y astrometric basis: 1.10890 at oslo 1966-02-14 00:00
- ganymede, 1962-1971, apparent: 0.50960 at oslo 1965-05-31 00:00
- ganymede, 1962-1971, astrometric: 0.00813 at oslo 1964-04-27 00:00
- ganymede, 1962-1971, distance km: 25.22223 at oslo 1965-09-13 00:00
- ganymede, 1962-1971, horizontal: 1.91912 at oslo 1971-12-27 00:00
- ganymede, 1962-1971, position angle deg: -0.01939 at oslo 1965-05-31 00:00
- ganymede, 1962-1971, separation arcsec: -0.03120 at oslo 1962-11-19 00:00
- ganymede, 1972-2027-06, X apparent basis: -0.16509 at geocentric 1978-07-10 00:00
- ganymede, 1972-2027-06, X astrometric basis: 0.20949 at oslo 1973-08-06 00:00
- ganymede, 1972-2027-06, Y apparent basis: -0.17800 at oslo 1978-07-10 00:00
- ganymede, 1972-2027-06, Y astrometric basis: 0.90246 at oslo 2026-02-03 00:00
- ganymede, 1972-2027-06, apparent: 1.25540 at geocentric 1978-07-10 00:00
- ganymede, 1972-2027-06, astrometric: 0.00435 at geocentric 2002-06-03 00:00
- ganymede, 1972-2027-06, distance km: -0.07020 at cape-town 2026-12-11 22:30
- ganymede, 1972-2027-06, horizontal: 2.83796 at cape-town 2026-12-11 22:55
- ganymede, 1972-2027-06, position angle deg: -0.06195 at oslo 1978-07-10 00:00
- ganymede, 1972-2027-06, separation arcsec: 0.13711 at geocentric 1978-07-10 00:00
- ganymede, after, X apparent basis: 0.11829 at oslo 2037-06-29 00:00
- ganymede, after, X astrometric basis: -0.69261 at oslo 2098-03-10 00:00
- ganymede, after, Y apparent basis: 0.19573 at oslo 2056-01-10 00:00
- ganymede, after, Y astrometric basis: 3.30786 at geocentric 2096-12-17 00:00
- ganymede, after, apparent: 1.47569 at oslo 2056-01-10 00:00
- ganymede, after, astrometric: 0.00435 at geocentric 2055-12-06 00:00
- ganymede, after, distance km: 9.77650 at cape-town 2100-12-31 23:59
- ganymede, after, horizontal: 405.97184 at oslo 2100-07-26 00:00
- ganymede, after, position angle deg: 0.07975 at oslo 2056-01-10 00:00
- ganymede, after, separation arcsec: 0.11991 at oslo 2037-06-29 00:00
- io, 1900-1961, X apparent basis: -0.06469 at oslo 1919-07-21 00:00
- io, 1900-1961, X astrometric basis: 0.31651 at geocentric 1902-09-08 00:00
- io, 1900-1961, Y apparent basis: 0.04688 at geocentric 1919-07-21 00:00
- io, 1900-1961, Y astrometric basis: 1.33940 at geocentric 1901-08-12 00:00
- io, 1900-1961, apparent: 1.07076 at geocentric 1919-07-21 00:00
- io, 1900-1961, astrometric: 0.00435 at geocentric 1929-10-14 00:00
- io, 1900-1961, distance km: -1.25255 at oslo 1900-07-30 00:00
- io, 1900-1961, horizontal: 1.09820 at oslo 1906-06-11 00:00
- io, 1900-1961, position angle deg: -0.02813 at oslo 1906-06-11 00:00
- io, 1900-1961, separation arcsec: -0.07370 at geocentric 1919-07-21 00:00
- io, 1962-1971, X apparent basis: 0.00691 at geocentric 1966-07-04 00:00
- io, 1962-1971, X astrometric basis: 0.10928 at oslo 1964-10-19 00:00
- io, 1962-1971, Y apparent basis: -0.01633 at geocentric 1965-05-31 00:00
- io, 1962-1971, Y astrometric basis: 0.48025 at oslo 1965-11-08 00:00
- io, 1962-1971, apparent: 0.51730 at oslo 1965-05-31 00:00
- io, 1962-1971, astrometric: 0.00869 at geocentric 1965-04-26 00:00
- io, 1962-1971, distance km: 26.40381 at oslo 1965-09-06 00:00
- io, 1962-1971, horizontal: 1.91903 at oslo 1971-12-27 00:00
- io, 1962-1971, position angle deg: 0.03937 at geocentric 1968-04-29 00:00
- io, 1962-1971, separation arcsec: -0.01265 at oslo 1963-12-23 00:00
- io, 1972-2027-06, X apparent basis: 0.02969 at geocentric 1978-07-10 00:00
- io, 1972-2027-06, X astrometric basis: 0.08129 at geocentric 1973-07-23 00:00
- io, 1972-2027-06, Y apparent basis: -0.03854 at geocentric 1997-01-20 00:00
- io, 1972-2027-06, Y astrometric basis: 0.38861 at geocentric 1972-06-26 00:00
- io, 1972-2027-06, apparent: 1.00423 at geocentric 1978-07-10 00:00
- io, 1972-2027-06, astrometric: 0.00435 at geocentric 2006-10-23 00:00
- io, 1972-2027-06, distance km: -0.07028 at cape-town 2026-12-11 22:30
- io, 1972-2027-06, horizontal: 2.83835 at cape-town 2026-12-11 22:55
- io, 1972-2027-06, position angle deg: -0.03098 at oslo 1978-07-10 00:00
- io, 1972-2027-06, separation arcsec: 0.02662 at geocentric 1978-07-10 00:00
- io, after, X apparent basis: -0.04440 at geocentric 2049-07-05 00:00
- io, after, X astrometric basis: -0.27736 at oslo 2092-09-08 00:00
- io, after, Y apparent basis: 0.05214 at geocentric 2056-01-10 00:00
- io, after, Y astrometric basis: 1.33214 at oslo 2095-11-14 00:00
- io, after, apparent: 1.35587 at oslo 2056-01-10 00:00
- io, after, astrometric: 0.00435 at geocentric 2038-02-15 00:00
- io, after, distance km: 9.77759 at cape-town 2100-12-31 23:59
- io, after, horizontal: 405.95804 at oslo 2100-07-26 00:00
- io, after, position angle deg: 0.06251 at geocentric 2056-01-10 00:00
- io, after, separation arcsec: 0.04524 at geocentric 2049-07-05 00:00
- jupiter, 1900-1961, apparent: 1.09931 at geocentric 1919-07-21 00:00
- jupiter, 1900-1961, astrometric: 0.00435 at geocentric 1936-09-14 00:00
- jupiter, 1900-1961, diameter asin: 0.00001 at geocentric 1902-09-22 00:00
- jupiter, 1900-1961, diameter atan: -0.00001 at geocentric 1905-10-23 00:00
- jupiter, 1900-1961, distance km: -0.84085 at oslo 1900-08-27 00:00
- jupiter, 1900-1961, horizontal: 1.10438 at oslo 1906-06-11 00:00
- jupiter, 1900-1961, illuminated %: 0.00047 at oslo 1904-07-25 00:00
- jupiter, 1900-1961, phase angle deg: -0.00836 at oslo 1918-12-30 00:00
- jupiter, 1962-1971, apparent: 0.52814 at oslo 1965-05-31 00:00
- jupiter, 1962-1971, astrometric: 0.00753 at geocentric 1965-05-03 00:00
- jupiter, 1962-1971, diameter asin: 0.00001 at oslo 1965-01-11 00:00
- jupiter, 1962-1971, diameter atan: -0.00001 at oslo 1963-07-29 00:00
- jupiter, 1962-1971, distance km: -17.95246 at oslo 1965-03-01 00:00
- jupiter, 1962-1971, horizontal: 1.91885 at oslo 1971-12-27 00:00
- jupiter, 1962-1971, illuminated %: 0.00047 at oslo 1963-07-08 00:00
- jupiter, 1962-1971, phase angle deg: -0.00832 at oslo 1965-12-13 00:00
- jupiter, 1972-2027-06, apparent: 1.04060 at geocentric 1978-07-10 00:00
- jupiter, 1972-2027-06, astrometric: 0.00307 at oslo 2026-12-01 04:00
- jupiter, 1972-2027-06, diameter asin: 0.00001 at oslo 2026-12-11 22:36
- jupiter, 1972-2027-06, diameter atan: -0.00001 at geocentric 1989-12-11 00:00
- jupiter, 1972-2027-06, distance km: -0.07027 at cape-town 2026-12-11 22:30
- jupiter, 1972-2027-06, horizontal: 2.83833 at cape-town 2026-12-11 22:55
- jupiter, 1972-2027-06, illuminated %: 0.00047 at oslo 2011-08-01 00:00
- jupiter, 1972-2027-06, phase angle deg: -0.00836 at oslo 1989-12-25 00:00
- jupiter, after, apparent: 1.30298 at oslo 2056-01-10 00:00
- jupiter, after, astrometric: 0.00307 at geocentric 2027-09-13 00:00
- jupiter, after, diameter asin: -0.00001 at oslo 2099-01-12 00:00
- jupiter, after, diameter atan: -0.00001 at oslo 2096-11-19 00:00
- jupiter, after, distance km: 9.77822 at cape-town 2100-12-31 23:59
- jupiter, after, horizontal: 405.96512 at oslo 2100-07-26 00:00
- jupiter, after, illuminated %: 0.00047 at oslo 2070-07-21 00:00
- jupiter, after, phase angle deg: 0.00835 at oslo 2085-01-01 00:00
