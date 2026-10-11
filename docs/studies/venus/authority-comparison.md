# #500 Venus authority comparison, measured 2026-10-11 02:02Z

Skyfield on `de440s.bsp` (DE440) against the kept JPL Horizons responses (Horizons: DE441, Venus radius 6051.8 km). Observers at sea level. The proposed contract reads each civil instant with the atlas's own `TimeScales` (TT from the leap-second record, Espenak-Meeus ΔT outside it) and UT1 = the civil instant; the two Skyfield passes read it with Skyfield's (UT1 before 1972, UTC from it, its own ΔT), which isolates the time-scale model's cost.

## The proposed contract against Horizons: the atlas's time scale, deflection by the Sun applied

| era | rows | astrometric | apparent | apparent, elongation > 1° | horizontal | distance | diameter | elongation | S-T-O, both aberrations (the Moon's) | its k vs Illu% | phi, the Sun's aberration only | its k vs Illu% | neither aberration vs S-T-O | vs phi | its k vs Illu% |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 9496 | 0.061″ | 0.257″ | 0.257″ | 1.05″ | -16.0 km | +0.0000″ | +0.22″ | -0.25″ | +0.00355 % | +0.28″ | +0.00002 % | +10.51″ | -24.44″ | +0.0059 % |
| 1962-1971 | 1538 | 0.020″ | 0.067″ | 0.067″ | 2.44″ | -2.6 km | -0.0000″ | +0.18″ | +0.19″ | +0.00354 % | -0.19″ | +0.00001 % | +10.40″ | +24.44″ | +0.0059 % |
| 1972-2027-06 | 9268 | 0.003″ | 2.949″ | 0.055″ | 11.68″ | -0.3 km | -0.0000″ | +3.12″ | -2.89″ | +0.00355 % | +0.18″ | -0.00001 % | +10.53″ | +24.44″ | +0.0059 % |
| after | 11589 | 7.118″ | 6.937″ | 6.937″ | 6.55″ | +1858.0 km | -0.0011″ | +7.98″ | +11.22″ | +0.00471 % | +11.30″ | -0.00149 % | +15.98″ | +34.82″ | -0.0068 % |

Parallax applied (topocentric minus geocentric astrometric place, every observer row): up to 32.87″ at quito 2085-12-16 00:00 (matrix-30d-quito).

Beyond 1° from the Sun, by era: elongation 1900-1961 +0.22″, 1962-1971 +0.18″, 1972-2027-06 -0.18″, after +7.98″; S-T-O 1900-1961 -0.25″, 1962-1971 +0.19″, 1972-2027-06 -0.18″, after +11.22″.

Apparent place by Venus's elongation from the Sun (worst, every era):
- elongation ≤ 1°: 6.575″ at quito 2097-12-13 00:00 (matrix-30d-quito); from 1962 6.575″ at quito 2097-12-13 00:00 (matrix-30d-quito)
- elongation 1–2°: 6.619″ at geocentric 2099-07-27 00:00 (matrix-7d-geocentric-299); from 1962 6.619″ at geocentric 2099-07-27 00:00 (matrix-7d-geocentric-299)
- elongation 2–5°: 6.629″ at quito 2099-08-05 00:00 (matrix-30d-quito); from 1962 6.629″ at quito 2099-08-05 00:00 (matrix-30d-quito)
- elongation > 5°: 6.937″ at chatham 2100-12-31 23:59 (named-chatham-299); from 1962 6.937″ at chatham 2100-12-31 23:59 (named-chatham-299)

Where the worst rows are:
- 1900-1961, S-T-O as the Moon: -0.25369 at chatham 1906-11-26 00:00 (matrix-30d-chatham)
- 1900-1961, S-T-O beyond 1°: -0.25369 at chatham 1906-11-26 00:00 (matrix-30d-chatham)
- 1900-1961, apparent: +0.25712 at geocentric 1900-07-02 00:00 (matrix-7d-geocentric-299)
- 1900-1961, apparent beyond 1°: +0.25712 at geocentric 1900-07-02 00:00 (matrix-7d-geocentric-299)
- 1900-1961, astrometric: +0.06116 at geocentric 1905-12-18 00:00 (matrix-7d-geocentric-299)
- 1900-1961, diameter: +0.00001 at chatham 1906-12-26 00:00 (matrix-30d-chatham)
- 1900-1961, distance km: -15.95831 at cape-town 1905-07-04 00:00 (matrix-30d-cape-town)
- 1900-1961, elongation: +0.22332 at geocentric 1908-06-22 00:00 (matrix-7d-geocentric-299)
- 1900-1961, elongation beyond 1°: +0.22332 at geocentric 1908-06-22 00:00 (matrix-7d-geocentric-299)
- 1900-1961, horizontal: +1.05209 at alert 1956-02-07 00:00 (matrix-30d-alert)
- 1900-1961, illuminated: +0.00588 at oslo 1927-11-21 00:00 (matrix-7d-oslo)
- 1900-1961, illuminated as Horizons: +0.00002 at quito 1905-04-05 00:00 (matrix-30d-quito)
- 1900-1961, illuminated from S-T-O: +0.00357 at quito 1915-03-14 00:00 (matrix-30d-quito)
- 1900-1961, illuminated from the Moon's S-T-O: +0.00355 at quito 1947-02-24 00:00 (matrix-30d-quito)
- 1900-1961, phase S-T-O: +10.51193 at quito 1932-04-13 00:00 (matrix-30d-quito)
- 1900-1961, phase phi: -24.43844 at oslo 1959-11-30 00:00 (matrix-7d-oslo)
- 1900-1961, phi as Horizons: +0.27852 at geocentric 1906-12-03 00:00 (matrix-7d-geocentric-299)
- 1962-1971, S-T-O as the Moon: +0.18532 at oslo 1967-09-18 00:00 (matrix-7d-oslo)
- 1962-1971, S-T-O beyond 1°: +0.18532 at oslo 1967-09-18 00:00 (matrix-7d-oslo)
- 1962-1971, apparent: +0.06663 at geocentric 1962-01-15 00:00 (matrix-7d-geocentric-299)
- 1962-1971, apparent beyond 1°: +0.06663 at geocentric 1962-01-15 00:00 (matrix-7d-geocentric-299)
- 1962-1971, astrometric: +0.01968 at geocentric 1962-01-15 00:00 (matrix-7d-geocentric-299)
- 1962-1971, diameter: -0.00001 at quito 1964-07-24 00:00 (matrix-30d-quito)
- 1962-1971, distance km: -2.60343 at geocentric 1964-03-30 00:00 (matrix-7d-geocentric-299)
- 1962-1971, elongation: +0.18312 at geocentric 1967-08-21 00:00 (matrix-7d-geocentric-299)
- 1962-1971, elongation beyond 1°: +0.18312 at geocentric 1967-08-21 00:00 (matrix-7d-geocentric-299)
- 1962-1971, horizontal: +2.43815 at alert 1967-11-06 00:00 (matrix-30d-alert)
- 1962-1971, illuminated: +0.00587 at geocentric 1971-01-18 00:00 (matrix-7d-geocentric-299)
- 1962-1971, illuminated as Horizons: +0.00001 at geocentric 1964-08-03 00:00 (matrix-7d-geocentric-299)
- 1962-1971, illuminated from S-T-O: +0.00354 at quito 1971-02-18 00:00 (matrix-30d-quito)
- 1962-1971, illuminated from the Moon's S-T-O: +0.00354 at quito 1966-03-16 00:00 (matrix-30d-quito)
- 1962-1971, phase S-T-O: +10.39558 at quito 1964-04-25 00:00 (matrix-30d-quito)
- 1962-1971, phase phi: +24.43807 at quito 1962-06-05 00:00 (matrix-30d-quito)
- 1962-1971, phi as Horizons: -0.19018 at quito 1964-06-24 00:00 (matrix-30d-quito)
- 1972-2027-06, S-T-O as the Moon: -2.89431 at oslo 2008-06-09 00:00 (matrix-7d-oslo)
- 1972-2027-06, S-T-O beyond 1°: -0.18026 at oslo 2012-04-30 00:00 (matrix-7d-oslo)
- 1972-2027-06, apparent: +2.94923 at geocentric 2008-06-09 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, apparent beyond 1°: +0.05472 at geocentric 2027-05-03 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, astrometric: +0.00307 at oslo 2026-08-24 21:00 (apparition-2026-oslo-299)
- 1972-2027-06, diameter: -0.00001 at quito 2004-06-24 00:00 (matrix-30d-quito)
- 1972-2027-06, distance km: -0.31683 at quito 1983-08-14 00:00 (matrix-30d-quito)
- 1972-2027-06, elongation: +3.11558 at geocentric 2008-06-09 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, elongation beyond 1°: -0.18011 at oslo 2012-06-04 14:00 (dense-2012-inferior-conjunction-oslo-299)
- 1972-2027-06, horizontal: +11.68058 at oslo 1994-07-11 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated: +0.00588 at oslo 2019-01-07 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated as Horizons: -0.00001 at oslo 2012-03-26 13:00 (dense-2012-greatest-elongation-east-oslo-299)
- 1972-2027-06, illuminated from S-T-O: +0.00358 at quito 2006-03-16 00:00 (matrix-30d-quito)
- 1972-2027-06, illuminated from the Moon's S-T-O: +0.00355 at quito 1998-03-28 00:00 (matrix-30d-quito)
- 1972-2027-06, phase S-T-O: +10.53099 at chatham 1996-04-07 00:00 (matrix-30d-chatham)
- 1972-2027-06, phase phi: +24.44281 at oslo 1973-06-11 00:00 (matrix-7d-oslo)
- 1972-2027-06, phi as Horizons: +0.17998 at oslo 2018-12-17 00:00 (matrix-7d-oslo)
- after, S-T-O as the Moon: +11.22043 at geocentric 2100-05-03 00:00 (matrix-7d-geocentric-299)
- after, S-T-O beyond 1°: +11.22043 at geocentric 2100-05-03 00:00 (matrix-7d-geocentric-299)
- after, apparent: +6.93666 at chatham 2100-12-31 23:59 (named-chatham-299)
- after, apparent beyond 1°: +6.93666 at chatham 2100-12-31 23:59 (named-chatham-299)
- after, astrometric: +7.11795 at chatham 2100-12-31 23:59 (named-chatham-299)
- after, diameter: -0.00114 at alert 2100-06-01 00:00 (matrix-30d-alert)
- after, distance km: +1857.97296 at oslo 2100-07-20 04:00 (dense-2100-greatest-elongation-west-oslo-299)
- after, elongation: +7.97730 at oslo 2093-12-21 00:00 (matrix-7d-oslo)
- after, elongation beyond 1°: +7.97730 at oslo 2093-12-21 00:00 (matrix-7d-oslo)
- after, horizontal: +6.55297 at alert 2100-12-31 23:59 (named-alert-299)
- after, illuminated: -0.00679 at geocentric 2100-03-22 00:00 (matrix-7d-geocentric-299)
- after, illuminated as Horizons: -0.00149 at oslo 2100-04-19 00:00 (matrix-7d-oslo)
- after, illuminated from S-T-O: +0.00358 at quito 2038-03-28 00:00 (matrix-30d-quito)
- after, illuminated from the Moon's S-T-O: +0.00471 at oslo 2100-06-14 00:00 (matrix-7d-oslo)
- after, phase S-T-O: +15.98169 at chatham 2100-05-02 00:00 (matrix-30d-chatham)
- after, phase phi: +34.81818 at geocentric 2093-12-14 00:00 (matrix-7d-geocentric-299)
- after, phi as Horizons: +11.30286 at geocentric 2100-05-03 00:00 (matrix-7d-geocentric-299)

## Skyfield against Horizons: Skyfield's time scale, deflection by the Sun not applied

| era | rows | astrometric | apparent | apparent, elongation > 1° | horizontal | distance | diameter | elongation | S-T-O, both aberrations (the Moon's) | its k vs Illu% | phi, the Sun's aberration only | its k vs Illu% | neither aberration vs S-T-O | vs phi | its k vs Illu% |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 9496 | 0.004″ | 0.532″ | 0.371″ | 1.19″ | -0.4 km | -0.0000″ | -0.60″ | +0.55″ | +0.00355 % | +0.18″ | -0.00001 % | +10.52″ | -24.44″ | +0.0059 % |
| 1962-1971 | 1538 | 0.035″ | 0.229″ | 0.186″ | 2.43″ | +8.3 km | -0.0000″ | -0.36″ | +0.39″ | +0.00354 % | -0.21″ | +0.00001 % | +10.37″ | +24.43″ | +0.0059 % |
| 1972-2027-06 | 9268 | 0.003″ | 0.456″ | 0.213″ | 3.42″ | +0.1 km | -0.0000″ | -0.53″ | +0.47″ | +0.00355 % | +0.18″ | +0.00000 % | +10.53″ | +24.44″ | +0.0059 % |
| after | 11589 | 0.047″ | 0.638″ | 0.294″ | 410.98″ | -12.2 km | +0.0000″ | -0.71″ | +0.67″ | +0.00355 % | +0.21″ | -0.00001 % | +10.50″ | +24.44″ | +0.0059 % |

Parallax applied (topocentric minus geocentric astrometric place, every observer row): up to 32.87″ at quito 2085-12-16 00:00 (matrix-30d-quito).

Beyond 1° from the Sun, by era: elongation 1900-1961 -0.34″, 1962-1971 -0.36″, 1972-2027-06 -0.37″, after -0.34″; S-T-O 1900-1961 +0.34″, 1962-1971 +0.33″, 1972-2027-06 +0.36″, after +0.35″.

Apparent place by Venus's elongation from the Sun (worst, every era):
- elongation ≤ 1°: 0.638″ at oslo 2097-12-16 00:00 (matrix-7d-oslo); from 1962 0.638″ at oslo 2097-12-16 00:00 (matrix-7d-oslo)
- elongation 1–2°: 0.371″ at geocentric 1902-11-24 00:00 (matrix-7d-geocentric-299); from 1962 0.294″ at oslo 2096-05-21 00:00 (matrix-7d-oslo)
- elongation 2–5°: 0.307″ at geocentric 1901-04-22 00:00 (matrix-7d-geocentric-299); from 1962 0.277″ at oslo 2097-12-23 00:00 (matrix-7d-oslo)
- elongation > 5°: 0.271″ at geocentric 1901-04-08 00:00 (matrix-7d-geocentric-299); from 1962 0.228″ at oslo 2099-08-10 00:00 (matrix-7d-oslo)

Where the worst rows are:
- 1900-1961, S-T-O as the Moon: +0.55291 at oslo 1926-11-22 00:00 (matrix-7d-oslo)
- 1900-1961, S-T-O beyond 1°: +0.33628 at geocentric 1941-04-21 00:00 (matrix-7d-geocentric-299)
- 1900-1961, apparent: +0.53221 at cape-town 1918-11-23 00:00 (matrix-30d-cape-town)
- 1900-1961, apparent beyond 1°: +0.37109 at geocentric 1902-11-24 00:00 (matrix-7d-geocentric-299)
- 1900-1961, astrometric: +0.00435 at quito 1957-12-28 00:00 (matrix-30d-quito)
- 1900-1961, diameter: -0.00001 at geocentric 1900-05-14 00:00 (matrix-7d-geocentric-299)
- 1900-1961, distance km: -0.42031 at geocentric 1900-09-17 00:00 (matrix-7d-geocentric-299)
- 1900-1961, elongation: -0.60120 at geocentric 1918-11-25 00:00 (matrix-7d-geocentric-299)
- 1900-1961, elongation beyond 1°: -0.33520 at geocentric 1954-02-01 00:00 (matrix-7d-geocentric-299)
- 1900-1961, horizontal: +1.19105 at alert 1918-11-23 00:00 (matrix-30d-alert)
- 1900-1961, illuminated: +0.00588 at oslo 1903-11-30 00:00 (matrix-7d-oslo)
- 1900-1961, illuminated as Horizons: -0.00001 at geocentric 1903-11-09 00:00 (matrix-7d-geocentric-299)
- 1900-1961, illuminated from S-T-O: +0.00357 at quito 1915-03-14 00:00 (matrix-30d-quito)
- 1900-1961, illuminated from the Moon's S-T-O: +0.00355 at quito 1947-02-24 00:00 (matrix-30d-quito)
- 1900-1961, phase S-T-O: +10.52155 at quito 1932-04-13 00:00 (matrix-30d-quito)
- 1900-1961, phase phi: -24.43795 at oslo 1959-11-30 00:00 (matrix-7d-oslo)
- 1900-1961, phi as Horizons: +0.18055 at geocentric 1906-12-03 00:00 (matrix-7d-geocentric-299)
- 1962-1971, S-T-O as the Moon: +0.38794 at alert 1966-11-11 00:00 (matrix-30d-alert)
- 1962-1971, S-T-O beyond 1°: +0.33366 at alert 1970-01-24 00:00 (matrix-30d-alert)
- 1962-1971, apparent: +0.22949 at geocentric 1968-06-17 00:00 (matrix-7d-geocentric-299)
- 1962-1971, apparent beyond 1°: +0.18648 at alert 1970-01-24 00:00 (matrix-30d-alert)
- 1962-1971, astrometric: +0.03545 at geocentric 1965-03-01 00:00 (matrix-7d-geocentric-299)
- 1962-1971, diameter: -0.00001 at alert 1964-05-25 00:00 (matrix-30d-alert)
- 1962-1971, distance km: +8.29351 at geocentric 1965-10-04 00:00 (matrix-7d-geocentric-299)
- 1962-1971, elongation: -0.36320 at quito 1970-01-24 00:00 (matrix-30d-quito)
- 1962-1971, elongation beyond 1°: -0.36320 at quito 1970-01-24 00:00 (matrix-30d-quito)
- 1962-1971, horizontal: +2.42667 at alert 1967-11-06 00:00 (matrix-30d-alert)
- 1962-1971, illuminated: +0.00587 at geocentric 1971-01-18 00:00 (matrix-7d-geocentric-299)
- 1962-1971, illuminated as Horizons: +0.00001 at cape-town 1964-05-25 00:00 (matrix-30d-cape-town)
- 1962-1971, illuminated from S-T-O: +0.00354 at quito 1971-02-18 00:00 (matrix-30d-quito)
- 1962-1971, illuminated from the Moon's S-T-O: +0.00354 at quito 1971-02-18 00:00 (matrix-30d-quito)
- 1962-1971, phase S-T-O: +10.37338 at quito 1964-04-25 00:00 (matrix-30d-quito)
- 1962-1971, phase phi: +24.43019 at quito 1962-06-05 00:00 (matrix-30d-quito)
- 1962-1971, phi as Horizons: -0.21452 at oslo 1962-11-05 00:00 (matrix-7d-oslo)
- 1972-2027-06, S-T-O as the Moon: +0.47178 at geocentric 2024-06-03 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, S-T-O beyond 1°: +0.36336 at oslo 2026-01-09 07:00 (dense-2026-superior-conjunction-oslo-299)
- 1972-2027-06, apparent: +0.45567 at oslo 2024-06-03 00:00 (matrix-7d-oslo)
- 1972-2027-06, apparent beyond 1°: +0.21332 at geocentric 1976-06-14 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, astrometric: +0.00307 at oslo 2026-06-29 21:00 (apparition-2026-oslo-299)
- 1972-2027-06, diameter: -0.00001 at oslo 2026-10-05 21:00 (apparition-2026-oslo-299)
- 1972-2027-06, distance km: +0.08748 at quito 2027-06-24 00:00 (matrix-30d-quito)
- 1972-2027-06, elongation: -0.52619 at geocentric 2024-06-03 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, elongation beyond 1°: -0.37063 at oslo 2026-01-09 09:00 (dense-2026-superior-conjunction-oslo-299)
- 1972-2027-06, horizontal: +3.41741 at cape-town 2027-04-25 00:00 (matrix-30d-cape-town)
- 1972-2027-06, illuminated: +0.00588 at oslo 2019-01-07 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated as Horizons: +0.00000 at geocentric 2003-11-24 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, illuminated from S-T-O: +0.00358 at quito 2006-03-16 00:00 (matrix-30d-quito)
- 1972-2027-06, illuminated from the Moon's S-T-O: +0.00355 at quito 1998-03-28 00:00 (matrix-30d-quito)
- 1972-2027-06, phase S-T-O: +10.53115 at chatham 1996-04-07 00:00 (matrix-30d-chatham)
- 1972-2027-06, phase phi: +24.44276 at oslo 1973-06-11 00:00 (matrix-7d-oslo)
- 1972-2027-06, phi as Horizons: +0.17998 at chatham 1990-10-06 00:00 (matrix-30d-chatham)
- after, S-T-O as the Moon: +0.66991 at chatham 2097-12-13 00:00 (matrix-30d-chatham)
- after, S-T-O beyond 1°: +0.34649 at geocentric 2067-08-01 00:00 (matrix-7d-geocentric-299)
- after, apparent: +0.63752 at oslo 2097-12-16 00:00 (matrix-7d-oslo)
- after, apparent beyond 1°: +0.29407 at oslo 2096-05-21 00:00 (matrix-7d-oslo)
- after, astrometric: +0.04742 at cape-town 2100-05-12 12:00 (named-cape-town-299)
- after, diameter: +0.00002 at quito 2100-05-02 00:00 (matrix-30d-quito)
- after, distance km: -12.24566 at quito 2099-09-04 00:00 (matrix-30d-quito)
- after, elongation: -0.70891 at quito 2097-12-13 00:00 (matrix-30d-quito)
- after, elongation beyond 1°: -0.34116 at quito 2049-12-25 00:00 (matrix-30d-quito)
- after, horizontal: +410.98236 at alert 2100-10-29 00:00 (matrix-30d-alert)
- after, illuminated: +0.00588 at oslo 2035-01-01 00:00 (matrix-7d-oslo)
- after, illuminated as Horizons: -0.00001 at cape-town 2092-04-13 00:00 (matrix-30d-cape-town)
- after, illuminated from S-T-O: +0.00358 at quito 2038-03-28 00:00 (matrix-30d-quito)
- after, illuminated from the Moon's S-T-O: +0.00355 at quito 2070-03-10 00:00 (matrix-30d-quito)
- after, phase S-T-O: +10.50481 at chatham 2079-05-22 00:00 (matrix-30d-chatham)
- after, phase phi: +24.43626 at cape-town 2093-06-07 00:00 (matrix-30d-cape-town)
- after, phi as Horizons: +0.20574 at cape-town 2087-07-09 00:00 (matrix-30d-cape-town)

## Skyfield against Horizons: Skyfield's time scale, deflection by the Sun applied

| era | rows | astrometric | apparent | apparent, elongation > 1° | horizontal | distance | diameter | elongation | S-T-O, both aberrations (the Moon's) | its k vs Illu% | phi, the Sun's aberration only | its k vs Illu% | neither aberration vs S-T-O | vs phi | its k vs Illu% |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 9496 | 0.004″ | 0.243″ | 0.243″ | 1.02″ | -0.4 km | -0.0000″ | +0.18″ | +0.18″ | +0.00355 % | +0.18″ | -0.00001 % | +10.52″ | -24.44″ | +0.0059 % |
| 1962-1971 | 1538 | 0.035″ | 0.065″ | 0.065″ | 2.43″ | +8.3 km | -0.0000″ | +0.21″ | -0.22″ | +0.00354 % | -0.21″ | +0.00001 % | +10.37″ | +24.43″ | +0.0059 % |
| 1972-2027-06 | 9268 | 0.003″ | 2.949″ | 0.055″ | 3.42″ | +0.1 km | -0.0000″ | +3.12″ | -2.89″ | +0.00355 % | +0.18″ | +0.00000 % | +10.53″ | +24.44″ | +0.0059 % |
| after | 11589 | 0.047″ | 0.223″ | 0.223″ | 410.98″ | -12.2 km | +0.0000″ | +0.20″ | -0.21″ | +0.00355 % | +0.21″ | -0.00001 % | +10.50″ | +24.44″ | +0.0059 % |

Parallax applied (topocentric minus geocentric astrometric place, every observer row): up to 32.87″ at quito 2085-12-16 00:00 (matrix-30d-quito).

Beyond 1° from the Sun, by era: elongation 1900-1961 +0.18″, 1962-1971 +0.21″, 1972-2027-06 +0.18″, after +0.20″; S-T-O 1900-1961 +0.18″, 1962-1971 -0.22″, 1972-2027-06 -0.18″, after -0.21″.

Apparent place by Venus's elongation from the Sun (worst, every era):
- elongation ≤ 1°: 2.949″ at geocentric 2008-06-09 00:00 (matrix-7d-geocentric-299); from 1962 2.949″ at geocentric 2008-06-09 00:00 (matrix-7d-geocentric-299)
- elongation 1–2°: 0.241″ at quito 1901-04-26 00:00 (matrix-30d-quito); from 1962 0.191″ at oslo 2099-07-27 00:00 (matrix-7d-oslo)
- elongation 2–5°: 0.241″ at geocentric 1901-04-15 00:00 (matrix-7d-geocentric-299); from 1962 0.220″ at chatham 2100-05-12 12:00 (named-chatham-299)
- elongation > 5°: 0.243″ at geocentric 1900-01-22 00:00 (matrix-7d-geocentric-299); from 1962 0.223″ at cape-town 2100-05-02 00:00 (matrix-30d-cape-town)

Where the worst rows are:
- 1900-1961, S-T-O as the Moon: +0.18070 at geocentric 1921-05-02 00:00 (matrix-7d-geocentric-299)
- 1900-1961, S-T-O beyond 1°: +0.18070 at geocentric 1921-05-02 00:00 (matrix-7d-geocentric-299)
- 1900-1961, apparent: +0.24345 at geocentric 1900-01-22 00:00 (matrix-7d-geocentric-299)
- 1900-1961, apparent beyond 1°: +0.24345 at geocentric 1900-01-22 00:00 (matrix-7d-geocentric-299)
- 1900-1961, astrometric: +0.00435 at quito 1957-12-28 00:00 (matrix-30d-quito)
- 1900-1961, diameter: -0.00001 at geocentric 1900-05-14 00:00 (matrix-7d-geocentric-299)
- 1900-1961, distance km: -0.42031 at geocentric 1900-09-17 00:00 (matrix-7d-geocentric-299)
- 1900-1961, elongation: +0.18055 at quito 1908-12-15 00:00 (matrix-30d-quito)
- 1900-1961, elongation beyond 1°: +0.18055 at quito 1908-12-15 00:00 (matrix-30d-quito)
- 1900-1961, horizontal: +1.02438 at alert 1902-02-20 00:00 (matrix-30d-alert)
- 1900-1961, illuminated: +0.00588 at oslo 1903-11-30 00:00 (matrix-7d-oslo)
- 1900-1961, illuminated as Horizons: -0.00001 at geocentric 1903-11-09 00:00 (matrix-7d-geocentric-299)
- 1900-1961, illuminated from S-T-O: +0.00357 at quito 1915-03-14 00:00 (matrix-30d-quito)
- 1900-1961, illuminated from the Moon's S-T-O: +0.00355 at quito 1947-02-24 00:00 (matrix-30d-quito)
- 1900-1961, phase S-T-O: +10.52155 at quito 1932-04-13 00:00 (matrix-30d-quito)
- 1900-1961, phase phi: -24.43795 at oslo 1959-11-30 00:00 (matrix-7d-oslo)
- 1900-1961, phi as Horizons: +0.18055 at geocentric 1906-12-03 00:00 (matrix-7d-geocentric-299)
- 1962-1971, S-T-O as the Moon: -0.22136 at oslo 1964-06-15 00:00 (matrix-7d-oslo)
- 1962-1971, S-T-O beyond 1°: -0.22136 at oslo 1964-06-15 00:00 (matrix-7d-oslo)
- 1962-1971, apparent: +0.06491 at oslo 1966-01-24 00:00 (matrix-7d-oslo)
- 1962-1971, apparent beyond 1°: +0.06491 at oslo 1966-01-24 00:00 (matrix-7d-oslo)
- 1962-1971, astrometric: +0.03545 at geocentric 1965-03-01 00:00 (matrix-7d-geocentric-299)
- 1962-1971, diameter: -0.00001 at alert 1964-05-25 00:00 (matrix-30d-alert)
- 1962-1971, distance km: +8.29351 at geocentric 1965-10-04 00:00 (matrix-7d-geocentric-299)
- 1962-1971, elongation: +0.21440 at oslo 1964-06-15 00:00 (matrix-7d-oslo)
- 1962-1971, elongation beyond 1°: +0.21440 at oslo 1964-06-15 00:00 (matrix-7d-oslo)
- 1962-1971, horizontal: +2.42524 at alert 1967-11-06 00:00 (matrix-30d-alert)
- 1962-1971, illuminated: +0.00587 at geocentric 1971-01-18 00:00 (matrix-7d-geocentric-299)
- 1962-1971, illuminated as Horizons: +0.00001 at cape-town 1964-05-25 00:00 (matrix-30d-cape-town)
- 1962-1971, illuminated from S-T-O: +0.00354 at quito 1971-02-18 00:00 (matrix-30d-quito)
- 1962-1971, illuminated from the Moon's S-T-O: +0.00354 at quito 1971-02-18 00:00 (matrix-30d-quito)
- 1962-1971, phase S-T-O: +10.37338 at quito 1964-04-25 00:00 (matrix-30d-quito)
- 1962-1971, phase phi: +24.43019 at quito 1962-06-05 00:00 (matrix-30d-quito)
- 1962-1971, phi as Horizons: -0.21452 at oslo 1962-11-05 00:00 (matrix-7d-oslo)
- 1972-2027-06, S-T-O as the Moon: -2.89437 at oslo 2008-06-09 00:00 (matrix-7d-oslo)
- 1972-2027-06, S-T-O beyond 1°: -0.18011 at oslo 1995-07-24 00:00 (matrix-7d-oslo)
- 1972-2027-06, apparent: +2.94923 at geocentric 2008-06-09 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, apparent beyond 1°: +0.05472 at geocentric 2027-05-03 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, astrometric: +0.00307 at oslo 2026-06-29 21:00 (apparition-2026-oslo-299)
- 1972-2027-06, diameter: -0.00001 at oslo 2026-10-05 21:00 (apparition-2026-oslo-299)
- 1972-2027-06, distance km: +0.08748 at quito 2027-06-24 00:00 (matrix-30d-quito)
- 1972-2027-06, elongation: +3.11558 at geocentric 2008-06-09 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, elongation beyond 1°: +0.18005 at oslo 2011-05-02 00:00 (matrix-7d-oslo)
- 1972-2027-06, horizontal: +3.42256 at cape-town 2027-04-25 00:00 (matrix-30d-cape-town)
- 1972-2027-06, illuminated: +0.00588 at oslo 2019-01-07 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated as Horizons: +0.00000 at geocentric 2003-11-24 00:00 (matrix-7d-geocentric-299)
- 1972-2027-06, illuminated from S-T-O: +0.00358 at quito 2006-03-16 00:00 (matrix-30d-quito)
- 1972-2027-06, illuminated from the Moon's S-T-O: +0.00355 at quito 1998-03-28 00:00 (matrix-30d-quito)
- 1972-2027-06, phase S-T-O: +10.53115 at chatham 1996-04-07 00:00 (matrix-30d-chatham)
- 1972-2027-06, phase phi: +24.44276 at oslo 1973-06-11 00:00 (matrix-7d-oslo)
- 1972-2027-06, phi as Horizons: +0.17998 at chatham 1990-10-06 00:00 (matrix-30d-chatham)
- after, S-T-O as the Moon: -0.20550 at oslo 2100-05-14 22:00 (dense-2100-inferior-conjunction-oslo-299)
- after, S-T-O beyond 1°: -0.20550 at oslo 2100-05-14 22:00 (dense-2100-inferior-conjunction-oslo-299)
- after, apparent: +0.22334 at cape-town 2100-05-02 00:00 (matrix-30d-cape-town)
- after, apparent beyond 1°: +0.22334 at cape-town 2100-05-02 00:00 (matrix-30d-cape-town)
- after, astrometric: +0.04742 at cape-town 2100-05-12 12:00 (named-cape-town-299)
- after, diameter: +0.00002 at quito 2100-05-02 00:00 (matrix-30d-quito)
- after, distance km: -12.24566 at quito 2099-09-04 00:00 (matrix-30d-quito)
- after, elongation: +0.20266 at chatham 2093-12-04 00:00 (matrix-30d-chatham)
- after, elongation beyond 1°: +0.20266 at chatham 2093-12-04 00:00 (matrix-30d-chatham)
- after, horizontal: +410.97738 at alert 2100-10-29 00:00 (matrix-30d-alert)
- after, illuminated: +0.00588 at oslo 2035-01-01 00:00 (matrix-7d-oslo)
- after, illuminated as Horizons: -0.00001 at cape-town 2092-04-13 00:00 (matrix-30d-cape-town)
- after, illuminated from S-T-O: +0.00358 at quito 2038-03-28 00:00 (matrix-30d-quito)
- after, illuminated from the Moon's S-T-O: +0.00355 at quito 2070-03-10 00:00 (matrix-30d-quito)
- after, phase S-T-O: +10.50481 at chatham 2079-05-22 00:00 (matrix-30d-chatham)
- after, phase phi: +24.43626 at cape-town 2093-06-07 00:00 (matrix-30d-cape-town)
- after, phi as Horizons: +0.20574 at cape-town 2087-07-09 00:00 (matrix-30d-cape-town)

## Bright-limb position angle χ (Meeus 48.5)

The proposed contract's χ (the atlas's time scale, Venus deflected by the Sun) against χ from Horizons' own Sun and Venus, every row where both were fetched (11611 rows), worst by era:

| era | of date, clear of k < 2 % and k ≥ 98 % | of date, near those | J2000, clear | J2000, near |
|---|---|---|---|---|
| 1900-1961 | +0.12″ | -0.85″ | -0.10″ | -0.91″ |
| 1962-1971 | -0.02″ | +0.06″ | -0.02″ | +0.06″ |
| 1972-2027-06 | +0.01″ | -1.04″ | +0.00″ | +0.24″ |
| after | -14.29″ | -141.37″ | -14.33″ | -141.29″ |

Where the worst rows are:
- date, 1900-1961, clear of k < 2 % and k ≥ 98 %: +0.12″ at geocentric 1905-04-17 00:00 illu=4.50413% elong=17.3389°
- date, 1900-1961, k < 2 % or k ≥ 98 %: -0.85″ at geocentric 1908-07-06 00:00 illu=0.23242% elong=3.9526°
- date, 1962-1971, clear of k < 2 % and k ≥ 98 %: -0.02″ at geocentric 1967-09-04 00:00 illu=2.01383% elong=11.6941°
- date, 1962-1971, k < 2 % or k ≥ 98 %: +0.06″ at geocentric 1963-09-02 00:00 illu=99.96082% elong=1.6188°
- date, 1972-2027-06, clear of k < 2 % and k ≥ 98 %: +0.01″ at geocentric 2027-05-10 00:00 illu=90.39521% elong=25.1145°
- date, 1972-2027-06, k < 2 % or k ≥ 98 %: -1.04″ at geocentric 2008-06-09 00:00 illu=99.99993% elong=0.0638°
- date, after, clear of k < 2 % and k ≥ 98 %: -14.29″ at geocentric 2097-03-11 00:00 illu=4.24414% elong=16.9946°
- date, after, k < 2 % or k ≥ 98 %: -141.37″ at oslo 2100-05-12 20:00 illu=0.20111% elong=3.6834°
- j2000, 1900-1961, clear of k < 2 % and k ≥ 98 %: -0.10″ at geocentric 1903-09-28 00:00 illu=4.38525% elong=17.2610°
- j2000, 1900-1961, k < 2 % or k ≥ 98 %: -0.91″ at geocentric 1908-07-06 00:00 illu=0.23242% elong=3.9526°
- j2000, 1962-1971, clear of k < 2 % and k ≥ 98 %: -0.02″ at geocentric 1967-09-04 00:00 illu=2.01383% elong=11.6941°
- j2000, 1962-1971, k < 2 % or k ≥ 98 %: +0.06″ at geocentric 1963-09-02 00:00 illu=99.96082% elong=1.6188°
- j2000, 1972-2027-06, clear of k < 2 % and k ≥ 98 %: +0.00″ at quito 2012-03-27 07:00 illu=51.14488% elong=46.0387°
- j2000, 1972-2027-06, k < 2 % or k ≥ 98 %: +0.24″ at cape-town 2012-06-06 01:00 illu=0.00038% elong=0.1589°
- j2000, after, clear of k < 2 % and k ≥ 98 %: -14.33″ at geocentric 2097-03-11 00:00 illu=4.24414% elong=16.9946°
- j2000, after, k < 2 % or k ≥ 98 %: -141.29″ at oslo 2100-05-12 20:00 illu=0.20111% elong=3.6834°

Horizons' /T (trailing: east of the Sun, evening) and /L (leading: west, morning) flag, against two definitions of east, same rows:
- apparent right ascension of date, Venus minus Sun: agrees on 11611, disagrees on 0
- apparent ecliptic longitude of date, Venus minus Sun: agrees on 11477, disagrees on 134

The frame matters: χ in ICRF/J2000 minus χ of date, Horizons' own positions, largest by era:
- 1900-1961: +0.622° at geocentric 1900-05-07 00:00 illu=45.92643% elong=45.3586°
- 1962-1971: +0.234° at geocentric 1962-05-21 00:00 illu=87.49410% elong=28.0119°
- 1972-2027-06: +0.174° at geocentric 1972-05-08 00:00 illu=30.64848% elong=41.2383°
- after: +0.611° at geocentric 2099-11-23 00:00 illu=85.20938% elong=31.5650°

## DE440 against DE442s (daily, 1900-01-01 to 2100-12-31, TT)

| quantity | worst | where |
|---|---|---|
| Venus barycentric position | +0.5120 km | 2095.65 |
| Earth barycentric position | +0.4946 km | 2095.72 |
| Sun barycentric position | +0.4839 km | 2095.34 |
| geocentric direction of Venus | +0.0061 ″ | 1900.09 |
| geocentric distance of Venus | -0.0229 km | 2100.17 |
| geocentric direction, 1900–1962 | 0.0061 ″ | |
| geocentric direction, 1962–2027.49 | 0.0061 ″ | |
| geocentric direction, 2027.49–2101 | 0.0061 ″ | |

## The named events: Espenak (AstroPixels) against DE440

Conjunction: Venus and the Sun at one apparent ecliptic longitude of date; greatest elongation: the maximum of the geocentric apparent Sun-Venus angle. Espenak states ±30 minutes for both.

| case | published row (UT, to the hour) | DE440 instant (UT) | difference | DE440 greatest elongation, or the conjunction in RA | its excess over the listed hour's |
|---|---|---|---|---|---|
| 2012-greatest-elongation-east | Mar 27 07 Venus at Greatest Elong: 46.0°E | 2012-03-27 07:44:19 | +44 min | 46.041° | 0.01″ |
| 2012-inferior-conjunction | Jun 06 01 Venus at Inferior Conjunction | 2012-06-06 01:08:58 | +9 min | in right ascension 2012-06-06 00:53 | |
| 2012-greatest-elongation-west | Aug 15 10 Venus at Greatest Elong: 45.8°W | 2012-08-15 09:06:35 | -53 min | 45.803° | 0.02″ |
| 2026-superior-conjunction | Jan 06 16 Venus at Superior Conjunction | 2026-01-06 16:36:00 | +36 min | in right ascension 2026-01-06 08:12 | |
| 2026-greatest-elongation-east | Aug 15 06 Venus at Greatest Elong: 45.9°E | 2026-08-15 06:31:33 | +32 min | 45.892° | 0.01″ |
| 2026-inferior-conjunction | Oct 24 03 Venus at Inferior Conjunction | 2026-10-24 03:44:06 | +44 min | in right ascension 2026-10-22 13:13 | |
| 2100-greatest-elongation-east | Mar 02 06 Venus at Greatest Elong: 46.5°E | 2100-03-02 04:38:43 | -81 min | 46.480° | 0.04″ |
| 2100-inferior-conjunction | May 12 12 Venus at Inferior Conjunction | 2100-05-12 12:22:59 | +23 min | in right ascension 2100-05-11 20:36 | |
| 2100-greatest-elongation-west | Jul 21 16 Venus at Greatest Elong: 45.7°W | 2100-07-21 15:32:42 | -27 min | 45.725° | 0.00″ |

## What a second of time costs

- Venus's fastest astrometric motion on the kept grids: 0.0526″ per second of time (3.16″ a minute) at 2026-01-03 23:00 (dense-2026-superior-conjunction-oslo-299); its fastest range rate 14.09 km/s.
- The Earth turns 15.04″ per second of UT1: a time error moves the horizontal place by up to that.
- So one second of civil-time error costs Venus's equatorial place at most 0.053″ and its distance at most 14.1 km, and its horizontal place up to 15″.
- ΔT at 2027-06-28 00:00:00: the atlas's 76.0 s (ESTIMATED_AFTER_RECORD), Skyfield's 69.1 s; apart +6.9 s.
- ΔT at 2050-01-01 00:00:00: the atlas's 93.0 s (ESTIMATED_AFTER_RECORD), Skyfield's 71.4 s; apart +21.6 s.
- ΔT at 2100-12-31 23:59:59: the atlas's 205.1 s (ESTIMATED_AFTER_RECORD), Skyfield's 96.7 s; apart +108.4 s.
- Horizons' own ΔT is not printed; the Skyfield passes' 411″ horizontal says it and Skyfield's part by about 27 s by 2100, and the proposed contract's after-era astrometric and distance differences above are the atlas's ΔT parting from Horizons' at the rates above - the cost of the accepted model, measured.

## The radius: solid body against cloud top

| case | distance | solid 6051.8 km | cloud top +74 km | optical +79 km (4500 Å) | cloud − solid | Horizons Ang-diam |
|---|---|---|---|---|---|---|
| nearest (geocentric, 7-day), 2061-12-26 | 39,605,904 km | 63.035″ | 63.805″ | 63.860″ | +0.771″ (1.22 %) | 63.035″ |
| farthest (geocentric, 7-day), 1952-06-23 | 259,693,849 km | 9.613″ | 9.731″ | 9.739″ | +0.118″ (1.22 %) | 9.613″ |
