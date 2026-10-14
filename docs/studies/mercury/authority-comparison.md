# #528 Mercury authority comparison, measured 2026-10-13 20:49Z

Skyfield on `de440s.bsp` (DE440) against the kept JPL Horizons responses (Horizons: DE441, Mercury radius 2440.53 km). Observers at sea level. The proposed contract reads each civil instant with the atlas's own `TimeScales` (TT from the leap-second record, Espenak-Meeus ΔT outside it) and UT1 = the civil instant; the two Skyfield passes read it with Skyfield's (UT1 before 1972, UTC from it, its own ΔT), which isolates the time-scale model's cost.

## The proposed contract against Horizons: the atlas's time scale, deflection by the Sun applied

| era | rows | astrometric | apparent | apparent, elongation > 1° | horizontal | distance | diameter | elongation | S-T-O, both aberrations (the Moon's) | its k vs Illu% | phi, the Sun's aberration only | its k vs Illu% | neither aberration vs S-T-O | vs phi | its k vs Illu% |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 6530 | 0.106″ | 0.279″ | 0.279″ | 1.04″ | -56.3 km | -0.0000″ | -0.25″ | +0.43″ | -0.00481 % | -0.49″ | -0.00006 % | -21.17″ | -40.80″ | +0.0098 % |
| 1962-1971 | 1056 | 0.025″ | 0.072″ | 0.072″ | 1.98″ | -15.1 km | -0.0000″ | +0.19″ | +0.21″ | -0.00481 % | -0.22″ | -0.00002 % | -21.14″ | -40.64″ | +0.0098 % |
| 1972-2027-06 | 6293 | 0.003″ | 0.721″ | 0.054″ | 11.47″ | -0.2 km | -0.0000″ | +0.85″ | -0.79″ | -0.00481 % | +0.18″ | +0.00001 % | -21.20″ | +40.73″ | +0.0098 % |
| after | 7816 | 11.858″ | 11.681″ | 11.637″ | 10.82″ | +6495.0 km | +0.0004″ | -12.98″ | +40.19″ | -0.01187 % | +40.08″ | +0.00727 % | +59.19″ | +79.35″ | -0.0167 % |

Parallax applied (topocentric minus geocentric astrometric place, every observer row): up to 15.88″ at chatham 2100-06-10 05:00 (named-chatham-199).

Beyond 1° from the Sun, by era: elongation 1900-1961 -0.25″, 1962-1971 +0.19″, 1972-2027-06 -0.18″, after -11.99″; S-T-O 1900-1961 +0.43″, 1962-1971 +0.21″, 1972-2027-06 +0.18″, after +38.61″.

Apparent place by Mercury's elongation from the Sun (worst, every era):
- elongation ≤ 1°: 11.681″ at oslo 2099-05-04 00:00 (matrix-7d-oslo); from 1962 11.681″ at oslo 2099-05-04 00:00 (matrix-7d-oslo)
- elongation 1–2°: 11.637″ at oslo 2098-05-19 00:00 (matrix-7d-oslo); from 1962 11.637″ at oslo 2098-05-19 00:00 (matrix-7d-oslo)
- elongation 2–5°: 11.508″ at oslo 2097-06-03 00:00 (matrix-7d-oslo); from 1962 11.508″ at oslo 2097-06-03 00:00 (matrix-7d-oslo)
- elongation > 5°: 11.613″ at geocentric 2100-07-26 00:00 (matrix-7d-geocentric-199); from 1962 11.613″ at geocentric 2100-07-26 00:00 (matrix-7d-geocentric-199)

Where the worst rows are:
- 1900-1961, S-T-O as the Moon: +0.42816 at geocentric 1907-11-18 00:00 (matrix-7d-geocentric-199)
- 1900-1961, S-T-O beyond 1°: +0.42816 at geocentric 1907-11-18 00:00 (matrix-7d-geocentric-199)
- 1900-1961, apparent: +0.27921 at geocentric 1905-01-02 00:00 (matrix-7d-geocentric-199)
- 1900-1961, apparent beyond 1°: +0.27921 at geocentric 1905-01-02 00:00 (matrix-7d-geocentric-199)
- 1900-1961, astrometric: +0.10576 at geocentric 1906-06-11 00:00 (matrix-7d-geocentric-199)
- 1900-1961, diameter: -0.00001 at geocentric 1904-05-02 00:00 (matrix-7d-geocentric-199)
- 1900-1961, distance km: -56.27434 at geocentric 1905-09-18 00:00 (matrix-7d-geocentric-199)
- 1900-1961, elongation: -0.24838 at geocentric 1907-11-18 00:00 (matrix-7d-geocentric-199)
- 1900-1961, elongation beyond 1°: -0.24838 at geocentric 1907-11-18 00:00 (matrix-7d-geocentric-199)
- 1900-1961, horizontal: +1.03945 at alert 1907-11-14 12:07 (transits-alert-199)
- 1900-1961, illuminated: +0.00978 at oslo 1950-10-02 00:00 (matrix-7d-oslo)
- 1900-1961, illuminated as Horizons: -0.00006 at geocentric 1905-09-18 00:00 (matrix-7d-geocentric-199)
- 1900-1961, illuminated from S-T-O: -0.00483 at oslo 1943-01-11 00:00 (matrix-7d-oslo)
- 1900-1961, illuminated from the Moon's S-T-O: -0.00481 at oslo 1923-01-15 00:00 (matrix-7d-oslo)
- 1900-1961, phase S-T-O: -21.16555 at geocentric 1924-09-29 00:00 (matrix-7d-geocentric-199)
- 1900-1961, phase phi: -40.79544 at geocentric 1955-10-24 00:00 (matrix-7d-geocentric-199)
- 1900-1961, phi as Horizons: -0.49012 at geocentric 1906-11-26 00:00 (matrix-7d-geocentric-199)
- 1962-1971, S-T-O as the Moon: +0.20819 at geocentric 1963-09-16 00:00 (matrix-7d-geocentric-199)
- 1962-1971, S-T-O beyond 1°: +0.20819 at geocentric 1963-09-16 00:00 (matrix-7d-geocentric-199)
- 1962-1971, apparent: +0.07182 at geocentric 1962-01-08 00:00 (matrix-7d-geocentric-199)
- 1962-1971, apparent beyond 1°: +0.07182 at geocentric 1962-01-08 00:00 (matrix-7d-geocentric-199)
- 1962-1971, astrometric: +0.02516 at oslo 1962-01-08 00:00 (matrix-7d-oslo)
- 1962-1971, diameter: -0.00001 at geocentric 1963-06-03 00:00 (matrix-7d-geocentric-199)
- 1962-1971, distance km: -15.05795 at oslo 1962-01-15 00:00 (matrix-7d-oslo)
- 1962-1971, elongation: +0.18711 at oslo 1962-01-01 00:00 (matrix-7d-oslo)
- 1962-1971, elongation beyond 1°: +0.18711 at oslo 1962-01-01 00:00 (matrix-7d-oslo)
- 1962-1971, horizontal: +1.98476 at oslo 1971-12-27 00:00 (matrix-7d-oslo)
- 1962-1971, illuminated: +0.00979 at oslo 1970-09-28 00:00 (matrix-7d-oslo)
- 1962-1971, illuminated as Horizons: -0.00002 at oslo 1962-01-15 00:00 (matrix-7d-oslo)
- 1962-1971, illuminated from S-T-O: -0.00481 at geocentric 1963-01-07 00:00 (matrix-7d-geocentric-199)
- 1962-1971, illuminated from the Moon's S-T-O: -0.00481 at oslo 1963-01-07 00:00 (matrix-7d-oslo)
- 1962-1971, phase S-T-O: -21.13564 at oslo 1964-09-21 00:00 (matrix-7d-oslo)
- 1962-1971, phase phi: -40.64153 at oslo 1966-11-21 00:00 (matrix-7d-oslo)
- 1962-1971, phi as Horizons: -0.22115 at oslo 1971-12-20 00:00 (matrix-7d-oslo)
- 1972-2027-06, S-T-O as the Moon: -0.79151 at oslo 2026-05-14 14:00 (named-oslo-199)
- 1972-2027-06, S-T-O beyond 1°: +0.18037 at oslo 1973-06-11 00:00 (matrix-7d-oslo)
- 1972-2027-06, apparent: +0.72070 at cape-town 2026-05-14 14:00 (named-cape-town-199)
- 1972-2027-06, apparent beyond 1°: +0.05438 at oslo 2027-04-19 00:00 (matrix-7d-oslo)
- 1972-2027-06, astrometric: +0.00307 at oslo 2026-02-15 17:00 (apparition-2026-oslo-199)
- 1972-2027-06, diameter: -0.00000 at geocentric 2026-07-06 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, distance km: -0.24668 at quito 2017-01-01 00:00 (named-quito-199)
- 1972-2027-06, elongation: +0.84586 at geocentric 2026-05-14 14:00 (named-geocentric-199)
- 1972-2027-06, elongation beyond 1°: -0.17996 at geocentric 1979-01-08 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, horizontal: +11.47395 at oslo 1994-07-04 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated: +0.00979 at geocentric 2009-10-05 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, illuminated as Horizons: +0.00001 at geocentric 1973-10-29 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, illuminated from S-T-O: -0.00485 at oslo 2002-01-14 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated from the Moon's S-T-O: -0.00481 at oslo 2002-01-14 00:00 (matrix-7d-oslo)
- 1972-2027-06, phase S-T-O: -21.20246 at geocentric 1989-10-09 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, phase phi: +40.73030 at oslo 1981-05-04 00:00 (matrix-7d-oslo)
- 1972-2027-06, phi as Horizons: +0.18040 at oslo 1977-04-18 00:00 (matrix-7d-oslo)
- after, S-T-O as the Moon: +40.19269 at oslo 2098-11-10 00:00 (matrix-7d-oslo)
- after, S-T-O beyond 1°: +38.60679 at geocentric 2097-11-25 00:00 (matrix-7d-geocentric-199)
- after, apparent: +11.68091 at oslo 2099-05-04 00:00 (matrix-7d-oslo)
- after, apparent beyond 1°: +11.63734 at oslo 2098-05-19 00:00 (matrix-7d-oslo)
- after, astrometric: +11.85809 at geocentric 2099-05-04 00:00 (matrix-7d-geocentric-199)
- after, diameter: +0.00041 at geocentric 2099-02-16 00:00 (matrix-7d-geocentric-199)
- after, distance km: +6494.95349 at alert 2100-10-24 20:00 (elongations-alert-199)
- after, elongation: -12.98400 at geocentric 2098-11-10 00:00 (matrix-7d-geocentric-199)
- after, elongation beyond 1°: -11.98678 at geocentric 2097-11-25 00:00 (matrix-7d-geocentric-199)
- after, horizontal: +10.81529 at alert 2100-04-18 17:00 (named-alert-199)
- after, illuminated: -0.01667 at geocentric 2100-01-25 00:00 (matrix-7d-geocentric-199)
- after, illuminated as Horizons: +0.00727 at oslo 2100-10-18 00:00 (matrix-7d-oslo)
- after, illuminated from S-T-O: -0.00484 at oslo 2041-01-21 00:00 (matrix-7d-oslo)
- after, illuminated from the Moon's S-T-O: -0.01187 at oslo 2100-01-25 00:00 (matrix-7d-oslo)
- after, phase S-T-O: +59.19284 at oslo 2098-11-10 00:00 (matrix-7d-oslo)
- after, phase phi: +79.35284 at oslo 2098-11-10 00:00 (matrix-7d-oslo)
- after, phi as Horizons: +40.07770 at oslo 2098-11-10 00:00 (matrix-7d-oslo)

## Skyfield against Horizons: Skyfield's time scale, deflection by the Sun not applied

| era | rows | astrometric | apparent | apparent, elongation > 1° | horizontal | distance | diameter | elongation | S-T-O, both aberrations (the Moon's) | its k vs Illu% | phi, the Sun's aberration only | its k vs Illu% | neither aberration vs S-T-O | vs phi | its k vs Illu% |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 6530 | 0.004″ | 0.440″ | 0.316″ | 0.99″ | -1.5 km | +0.0000″ | -0.43″ | +0.37″ | -0.00481 % | -0.19″ | -0.00001 % | -21.17″ | +40.70″ | +0.0098 % |
| 1962-1971 | 1056 | 0.054″ | 0.289″ | 0.142″ | 1.98″ | -32.7 km | +0.0000″ | -0.46″ | +0.42″ | -0.00479 % | +0.34″ | -0.00003 % | -21.02″ | +40.54″ | +0.0098 % |
| 1972-2027-06 | 6293 | 0.004″ | 0.357″ | 0.191″ | 2.73″ | +0.1 km | -0.0000″ | -0.46″ | +0.47″ | -0.00481 % | -0.18″ | +0.00001 % | -21.20″ | +40.73″ | +0.0098 % |
| after | 7816 | 0.031″ | 0.429″ | 0.269″ | 409.67″ | -11.6 km | -0.0000″ | -0.50″ | +0.50″ | -0.00482 % | -0.19″ | -0.00001 % | -21.20″ | +40.73″ | +0.0098 % |

Parallax applied (topocentric minus geocentric astrometric place, every observer row): up to 15.89″ at chatham 2100-06-10 05:00 (named-chatham-199).

Beyond 1° from the Sun, by era: elongation 1900-1961 -0.30″, 1962-1971 -0.27″, 1972-2027-06 -0.33″, after -0.28″; S-T-O 1900-1961 +0.27″, 1962-1971 -0.34″, 1972-2027-06 +0.30″, after +0.29″.

Apparent place by Mercury's elongation from the Sun (worst, every era):
- elongation ≤ 1°: 0.440″ at geocentric 1904-10-31 00:00 (matrix-7d-geocentric-199); from 1962 0.429″ at oslo 2028-11-20 00:00 (matrix-7d-oslo)
- elongation 1–2°: 0.316″ at oslo 1902-04-28 00:00 (matrix-7d-oslo); from 1962 0.269″ at oslo 2093-12-07 00:00 (matrix-7d-oslo)
- elongation 2–5°: 0.291″ at oslo 1901-05-13 00:00 (matrix-7d-oslo); from 1962 0.242″ at oslo 2095-10-31 00:00 (matrix-7d-oslo)
- elongation > 5°: 0.263″ at oslo 1901-01-14 00:00 (matrix-7d-oslo); from 1962 0.216″ at oslo 2098-09-08 00:00 (matrix-7d-oslo)

Where the worst rows are:
- 1900-1961, S-T-O as the Moon: +0.36769 at oslo 1924-10-27 00:00 (matrix-7d-oslo)
- 1900-1961, S-T-O beyond 1°: +0.27206 at oslo 1943-04-05 00:00 (matrix-7d-oslo)
- 1900-1961, apparent: +0.43973 at geocentric 1904-10-31 00:00 (matrix-7d-geocentric-199)
- 1900-1961, apparent beyond 1°: +0.31579 at oslo 1902-04-28 00:00 (matrix-7d-oslo)
- 1900-1961, astrometric: +0.00435 at geocentric 1900-06-04 00:00 (matrix-7d-geocentric-199)
- 1900-1961, diameter: +0.00001 at oslo 1902-10-20 00:00 (matrix-7d-oslo)
- 1900-1961, distance km: -1.45313 at geocentric 1900-08-20 00:00 (matrix-7d-geocentric-199)
- 1900-1961, elongation: -0.43298 at geocentric 1904-10-31 00:00 (matrix-7d-geocentric-199)
- 1900-1961, elongation beyond 1°: -0.29944 at oslo 1948-12-13 00:00 (matrix-7d-oslo)
- 1900-1961, horizontal: +0.99178 at alert 1914-11-07 12:03 (transits-alert-199)
- 1900-1961, illuminated: +0.00977 at oslo 1950-10-02 00:00 (matrix-7d-oslo)
- 1900-1961, illuminated as Horizons: -0.00001 at geocentric 1905-09-11 00:00 (matrix-7d-geocentric-199)
- 1900-1961, illuminated from S-T-O: -0.00483 at oslo 1943-01-11 00:00 (matrix-7d-oslo)
- 1900-1961, illuminated from the Moon's S-T-O: -0.00481 at oslo 1923-01-15 00:00 (matrix-7d-oslo)
- 1900-1961, phase S-T-O: -21.16543 at geocentric 1924-09-29 00:00 (matrix-7d-geocentric-199)
- 1900-1961, phase phi: +40.70352 at geocentric 1936-04-20 00:00 (matrix-7d-geocentric-199)
- 1900-1961, phi as Horizons: -0.18593 at geocentric 1900-11-19 00:00 (matrix-7d-geocentric-199)
- 1962-1971, S-T-O as the Moon: +0.41607 at oslo 1969-11-17 00:00 (matrix-7d-oslo)
- 1962-1971, S-T-O beyond 1°: -0.33955 at geocentric 1965-11-29 00:00 (matrix-7d-geocentric-199)
- 1962-1971, apparent: +0.28875 at oslo 1969-11-17 00:00 (matrix-7d-oslo)
- 1962-1971, apparent beyond 1°: +0.14218 at geocentric 1970-10-26 00:00 (matrix-7d-geocentric-199)
- 1962-1971, astrometric: +0.05438 at geocentric 1965-03-01 00:00 (matrix-7d-geocentric-199)
- 1962-1971, diameter: +0.00001 at oslo 1967-07-17 00:00 (matrix-7d-oslo)
- 1962-1971, distance km: -32.73056 at geocentric 1965-09-06 00:00 (matrix-7d-geocentric-199)
- 1962-1971, elongation: -0.45564 at oslo 1969-11-17 00:00 (matrix-7d-oslo)
- 1962-1971, elongation beyond 1°: -0.26675 at geocentric 1963-04-01 00:00 (matrix-7d-geocentric-199)
- 1962-1971, horizontal: +1.98465 at oslo 1971-12-27 00:00 (matrix-7d-oslo)
- 1962-1971, illuminated: +0.00977 at oslo 1970-09-28 00:00 (matrix-7d-oslo)
- 1962-1971, illuminated as Horizons: -0.00003 at oslo 1965-09-06 00:00 (matrix-7d-oslo)
- 1962-1971, illuminated from S-T-O: -0.00481 at geocentric 1963-01-07 00:00 (matrix-7d-geocentric-199)
- 1962-1971, illuminated from the Moon's S-T-O: -0.00479 at oslo 1963-01-07 00:00 (matrix-7d-oslo)
- 1962-1971, phase S-T-O: -21.01974 at geocentric 1970-09-28 00:00 (matrix-7d-geocentric-199)
- 1962-1971, phase phi: +40.54402 at geocentric 1970-04-06 00:00 (matrix-7d-geocentric-199)
- 1962-1971, phi as Horizons: +0.34252 at oslo 1965-12-06 00:00 (matrix-7d-oslo)
- 1972-2027-06, S-T-O as the Moon: +0.46644 at geocentric 1976-11-08 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, S-T-O beyond 1°: +0.30082 at geocentric 2014-12-08 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, apparent: +0.35669 at oslo 1976-11-08 00:00 (matrix-7d-oslo)
- 1972-2027-06, apparent beyond 1°: +0.19126 at geocentric 2008-11-24 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, astrometric: +0.00435 at geocentric 1985-06-24 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, diameter: -0.00000 at geocentric 2026-07-06 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, distance km: +0.06921 at quito 2026-11-20 23:00 (elongations-quito-199)
- 1972-2027-06, elongation: -0.46054 at geocentric 1976-11-08 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, elongation beyond 1°: -0.32652 at geocentric 2014-12-08 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, horizontal: +2.73466 at oslo 2027-04-12 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated: +0.00979 at geocentric 2009-10-05 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, illuminated as Horizons: +0.00001 at geocentric 1973-10-29 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, illuminated from S-T-O: -0.00485 at oslo 2002-01-14 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated from the Moon's S-T-O: -0.00481 at oslo 2002-01-14 00:00 (matrix-7d-oslo)
- 1972-2027-06, phase S-T-O: -21.20246 at geocentric 1989-10-09 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, phase phi: +40.73042 at oslo 1981-05-04 00:00 (matrix-7d-oslo)
- 1972-2027-06, phi as Horizons: -0.18034 at geocentric 1998-12-21 00:00 (matrix-7d-geocentric-199)
- after, S-T-O as the Moon: +0.50394 at geocentric 2028-11-20 00:00 (matrix-7d-geocentric-199)
- after, S-T-O beyond 1°: +0.29397 at geocentric 2034-12-04 00:00 (matrix-7d-geocentric-199)
- after, apparent: +0.42875 at oslo 2028-11-20 00:00 (matrix-7d-oslo)
- after, apparent beyond 1°: +0.26914 at oslo 2093-12-07 00:00 (matrix-7d-oslo)
- after, astrometric: +0.03134 at quito 2100-06-10 05:00 (named-quito-199)
- after, diameter: -0.00001 at cape-town 2100-06-10 05:00 (named-cape-town-199)
- after, distance km: -11.64485 at quito 2100-12-31 23:59 (named-quito-199)
- after, elongation: -0.49842 at oslo 2042-11-03 00:00 (matrix-7d-oslo)
- after, elongation beyond 1°: -0.27917 at oslo 2076-10-12 00:00 (matrix-7d-oslo)
- after, horizontal: +409.67088 at alert 2100-10-24 20:00 (elongations-alert-199)
- after, illuminated: +0.00980 at geocentric 2088-10-04 00:00 (matrix-7d-geocentric-199)
- after, illuminated as Horizons: -0.00001 at oslo 2093-05-18 00:00 (matrix-7d-oslo)
- after, illuminated from S-T-O: -0.00484 at oslo 2041-01-21 00:00 (matrix-7d-oslo)
- after, illuminated from the Moon's S-T-O: -0.00482 at oslo 2081-01-13 00:00 (matrix-7d-oslo)
- after, phase S-T-O: -21.19940 at geocentric 2088-10-04 00:00 (matrix-7d-geocentric-199)
- after, phase phi: +40.72885 at geocentric 2060-05-03 00:00 (matrix-7d-geocentric-199)
- after, phi as Horizons: -0.18692 at oslo 2099-10-26 00:00 (matrix-7d-oslo)

## Skyfield against Horizons: Skyfield's time scale, deflection by the Sun applied

| era | rows | astrometric | apparent | apparent, elongation > 1° | horizontal | distance | diameter | elongation | S-T-O, both aberrations (the Moon's) | its k vs Illu% | phi, the Sun's aberration only | its k vs Illu% | neither aberration vs S-T-O | vs phi | its k vs Illu% |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1900-1961 | 6530 | 0.004″ | 0.245″ | 0.245″ | 0.99″ | -1.5 km | +0.0000″ | -0.18″ | +0.18″ | -0.00481 % | -0.19″ | -0.00001 % | -21.17″ | +40.70″ | +0.0098 % |
| 1962-1971 | 1056 | 0.054″ | 0.077″ | 0.077″ | 1.99″ | -32.7 km | +0.0000″ | -0.21″ | -0.34″ | -0.00478 % | +0.34″ | -0.00003 % | -21.02″ | +40.54″ | +0.0098 % |
| 1972-2027-06 | 6293 | 0.004″ | 0.721″ | 0.054″ | 2.74″ | +0.1 km | -0.0000″ | +0.85″ | -0.79″ | -0.00481 % | -0.18″ | +0.00001 % | -21.20″ | +40.73″ | +0.0098 % |
| after | 7816 | 0.031″ | 2.818″ | 0.212″ | 409.67″ | -11.6 km | -0.0000″ | +2.97″ | -2.89″ | -0.00482 % | -0.19″ | -0.00001 % | -21.20″ | +40.73″ | +0.0098 % |

Parallax applied (topocentric minus geocentric astrometric place, every observer row): up to 15.89″ at chatham 2100-06-10 05:00 (named-chatham-199).

Beyond 1° from the Sun, by era: elongation 1900-1961 -0.18″, 1962-1971 -0.21″, 1972-2027-06 -0.18″, after +0.18″; S-T-O 1900-1961 +0.18″, 1962-1971 -0.34″, 1972-2027-06 +0.18″, after +0.19″.

Apparent place by Mercury's elongation from the Sun (worst, every era):
- elongation ≤ 1°: 2.818″ at geocentric 2035-11-12 00:00 (matrix-7d-geocentric-199); from 1962 2.818″ at geocentric 2035-11-12 00:00 (matrix-7d-geocentric-199)
- elongation 1–2°: 0.234″ at oslo 1902-04-28 00:00 (matrix-7d-oslo); from 1962 0.212″ at quito 2100-10-09 07:00 (named-quito-199)
- elongation 2–5°: 0.245″ at geocentric 1900-03-26 00:00 (matrix-7d-geocentric-199); from 1962 0.211″ at quito 2100-06-10 05:00 (named-quito-199)
- elongation > 5°: 0.245″ at geocentric 1900-04-16 00:00 (matrix-7d-geocentric-199); from 1962 0.202″ at quito 2100-07-04 06:00 (elongations-quito-199)

Where the worst rows are:
- 1900-1961, S-T-O as the Moon: +0.18464 at oslo 1900-08-06 00:00 (matrix-7d-oslo)
- 1900-1961, S-T-O beyond 1°: +0.18464 at oslo 1900-08-06 00:00 (matrix-7d-oslo)
- 1900-1961, apparent: +0.24512 at geocentric 1900-04-16 00:00 (matrix-7d-geocentric-199)
- 1900-1961, apparent beyond 1°: +0.24512 at geocentric 1900-04-16 00:00 (matrix-7d-geocentric-199)
- 1900-1961, astrometric: +0.00435 at geocentric 1900-06-04 00:00 (matrix-7d-geocentric-199)
- 1900-1961, diameter: +0.00001 at oslo 1902-10-20 00:00 (matrix-7d-oslo)
- 1900-1961, distance km: -1.45313 at geocentric 1900-08-20 00:00 (matrix-7d-geocentric-199)
- 1900-1961, elongation: -0.18070 at oslo 1904-09-26 00:00 (matrix-7d-oslo)
- 1900-1961, elongation beyond 1°: -0.18070 at oslo 1904-09-26 00:00 (matrix-7d-oslo)
- 1900-1961, horizontal: +0.99177 at alert 1914-11-07 12:03 (transits-alert-199)
- 1900-1961, illuminated: +0.00977 at oslo 1950-10-02 00:00 (matrix-7d-oslo)
- 1900-1961, illuminated as Horizons: -0.00001 at geocentric 1905-09-11 00:00 (matrix-7d-geocentric-199)
- 1900-1961, illuminated from S-T-O: -0.00483 at oslo 1943-01-11 00:00 (matrix-7d-oslo)
- 1900-1961, illuminated from the Moon's S-T-O: -0.00481 at oslo 1923-01-15 00:00 (matrix-7d-oslo)
- 1900-1961, phase S-T-O: -21.16543 at geocentric 1924-09-29 00:00 (matrix-7d-geocentric-199)
- 1900-1961, phase phi: +40.70352 at geocentric 1936-04-20 00:00 (matrix-7d-geocentric-199)
- 1900-1961, phi as Horizons: -0.18593 at geocentric 1900-11-19 00:00 (matrix-7d-geocentric-199)
- 1962-1971, S-T-O as the Moon: -0.34033 at geocentric 1965-11-29 00:00 (matrix-7d-geocentric-199)
- 1962-1971, S-T-O beyond 1°: -0.34033 at geocentric 1965-11-29 00:00 (matrix-7d-geocentric-199)
- 1962-1971, apparent: +0.07739 at geocentric 1964-12-21 00:00 (matrix-7d-geocentric-199)
- 1962-1971, apparent beyond 1°: +0.07739 at geocentric 1964-12-21 00:00 (matrix-7d-geocentric-199)
- 1962-1971, astrometric: +0.05438 at geocentric 1965-03-01 00:00 (matrix-7d-geocentric-199)
- 1962-1971, diameter: +0.00001 at oslo 1967-07-17 00:00 (matrix-7d-oslo)
- 1962-1971, distance km: -32.73056 at geocentric 1965-09-06 00:00 (matrix-7d-geocentric-199)
- 1962-1971, elongation: -0.21013 at geocentric 1965-04-19 00:00 (matrix-7d-geocentric-199)
- 1962-1971, elongation beyond 1°: -0.21013 at geocentric 1965-04-19 00:00 (matrix-7d-geocentric-199)
- 1962-1971, horizontal: +1.98724 at oslo 1971-12-27 00:00 (matrix-7d-oslo)
- 1962-1971, illuminated: +0.00977 at oslo 1970-09-28 00:00 (matrix-7d-oslo)
- 1962-1971, illuminated as Horizons: -0.00003 at oslo 1965-09-06 00:00 (matrix-7d-oslo)
- 1962-1971, illuminated from S-T-O: -0.00481 at geocentric 1963-01-07 00:00 (matrix-7d-geocentric-199)
- 1962-1971, illuminated from the Moon's S-T-O: -0.00478 at oslo 1963-01-07 00:00 (matrix-7d-oslo)
- 1962-1971, phase S-T-O: -21.01974 at geocentric 1970-09-28 00:00 (matrix-7d-geocentric-199)
- 1962-1971, phase phi: +40.54402 at geocentric 1970-04-06 00:00 (matrix-7d-geocentric-199)
- 1962-1971, phi as Horizons: +0.34252 at oslo 1965-12-06 00:00 (matrix-7d-oslo)
- 1972-2027-06, S-T-O as the Moon: -0.79151 at oslo 2026-05-14 14:00 (named-oslo-199)
- 1972-2027-06, S-T-O beyond 1°: +0.18040 at oslo 2021-05-17 00:00 (matrix-7d-oslo)
- 1972-2027-06, apparent: +0.72070 at cape-town 2026-05-14 14:00 (named-cape-town-199)
- 1972-2027-06, apparent beyond 1°: +0.05429 at geocentric 2027-04-19 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, astrometric: +0.00435 at geocentric 1985-06-24 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, diameter: -0.00000 at geocentric 2026-07-06 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, distance km: +0.06921 at quito 2026-11-20 23:00 (elongations-quito-199)
- 1972-2027-06, elongation: +0.84586 at geocentric 2026-05-14 14:00 (named-geocentric-199)
- 1972-2027-06, elongation beyond 1°: -0.17997 at geocentric 1979-01-08 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, horizontal: +2.74047 at oslo 2027-04-12 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated: +0.00979 at geocentric 2009-10-05 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, illuminated as Horizons: +0.00001 at geocentric 1973-10-29 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, illuminated from S-T-O: -0.00485 at oslo 2002-01-14 00:00 (matrix-7d-oslo)
- 1972-2027-06, illuminated from the Moon's S-T-O: -0.00481 at oslo 2002-01-14 00:00 (matrix-7d-oslo)
- 1972-2027-06, phase S-T-O: -21.20246 at geocentric 1989-10-09 00:00 (matrix-7d-geocentric-199)
- 1972-2027-06, phase phi: +40.73042 at oslo 1981-05-04 00:00 (matrix-7d-oslo)
- 1972-2027-06, phi as Horizons: -0.18034 at geocentric 1998-12-21 00:00 (matrix-7d-geocentric-199)
- after, S-T-O as the Moon: -2.89104 at oslo 2035-11-12 00:00 (matrix-7d-oslo)
- after, S-T-O beyond 1°: +0.19419 at oslo 2100-06-07 00:00 (matrix-7d-oslo)
- after, apparent: +2.81844 at geocentric 2035-11-12 00:00 (matrix-7d-geocentric-199)
- after, apparent beyond 1°: +0.21156 at quito 2100-10-09 07:00 (named-quito-199)
- after, astrometric: +0.03134 at quito 2100-06-10 05:00 (named-quito-199)
- after, diameter: -0.00001 at cape-town 2100-06-10 05:00 (named-cape-town-199)
- after, distance km: -11.64485 at quito 2100-12-31 23:59 (named-quito-199)
- after, elongation: +2.96581 at geocentric 2035-11-12 00:00 (matrix-7d-geocentric-199)
- after, elongation beyond 1°: +0.18283 at chatham 2100-07-04 06:00 (elongations-chatham-199)
- after, horizontal: +409.66798 at alert 2100-10-24 20:00 (elongations-alert-199)
- after, illuminated: +0.00980 at geocentric 2088-10-04 00:00 (matrix-7d-geocentric-199)
- after, illuminated as Horizons: -0.00001 at oslo 2093-05-18 00:00 (matrix-7d-oslo)
- after, illuminated from S-T-O: -0.00484 at oslo 2041-01-21 00:00 (matrix-7d-oslo)
- after, illuminated from the Moon's S-T-O: -0.00482 at oslo 2081-01-13 00:00 (matrix-7d-oslo)
- after, phase S-T-O: -21.19940 at geocentric 2088-10-04 00:00 (matrix-7d-geocentric-199)
- after, phase phi: +40.72885 at geocentric 2060-05-03 00:00 (matrix-7d-geocentric-199)
- after, phi as Horizons: -0.18692 at oslo 2099-10-26 00:00 (matrix-7d-oslo)

## Bright-limb position angle χ (Meeus 48.5)

The proposed contract's χ (the atlas's time scale, Mercury deflected by the Sun) against χ from Horizons' own Sun and Mercury, every row where both were fetched (719 rows), worst by era:

| era | of date, clear of k < 2 % and k ≥ 98 % | of date, near those | J2000, clear | J2000, near |
|---|---|---|---|---|
| 1900-1961 | -0.05″ | +28.09″ | +0.03″ | +28.13″ |
| 1962-1971 | -0.02″ | +3.81″ | -0.02″ | +3.81″ |
| 1972-2027-06 | +0.00″ | +0.28″ | -0.00″ | +0.29″ |
| after | -5.19″ | +12451.95″ | -5.34″ | +12451.84″ |

Where the worst rows are:
- date, 1900-1961, clear of k < 2 % and k ≥ 98 %: -0.05″ at chatham 1900-01-01 00:00 illu=76.76480% elong=21.1849°
- date, 1900-1961, k < 2 % or k ≥ 98 %: +28.09″ at oslo 1907-11-14 12:07 illu=0.00336% elong=0.2097°
- date, 1962-1971, clear of k < 2 % and k ≥ 98 %: -0.02″ at quito 1962-01-01 00:00 illu=96.25000% elong=9.3724°
- date, 1962-1971, k < 2 % or k ≥ 98 %: +3.81″ at chatham 1970-05-09 08:16 illu=0.00004% elong=0.0303°
- date, 1972-2027-06, clear of k < 2 % and k ≥ 98 %: +0.00″ at oslo 2026-03-10 03:00 illu=2.05379% elong=6.3100°
- date, 1972-2027-06, k < 2 % or k ≥ 98 %: +0.28″ at cape-town 1973-11-10 10:32 illu=0.00000% elong=0.0070°
- date, after, clear of k < 2 % and k ≥ 98 %: -5.19″ at cape-town 2100-12-31 23:59 illu=77.26313% elong=18.0192°
- date, after, k < 2 % or k ≥ 98 %: +12451.95″ at cape-town 2098-11-10 07:16 illu=0.00027% elong=0.0592°
- j2000, 1900-1961, clear of k < 2 % and k ≥ 98 %: +0.03″ at oslo 1900-01-01 00:00 illu=76.76367% elong=21.1836°
- j2000, 1900-1961, k < 2 % or k ≥ 98 %: +28.13″ at oslo 1907-11-14 12:07 illu=0.00336% elong=0.2097°
- j2000, 1962-1971, clear of k < 2 % and k ≥ 98 %: -0.02″ at cape-town 1962-01-01 00:00 illu=96.25061% elong=9.3715°
- j2000, 1962-1971, k < 2 % or k ≥ 98 %: +3.81″ at chatham 1970-05-09 08:16 illu=0.00004% elong=0.0303°
- j2000, 1972-2027-06, clear of k < 2 % and k ≥ 98 %: -0.00″ at quito 2017-01-01 00:00 illu=4.89149% elong=8.0712°
- j2000, 1972-2027-06, k < 2 % or k ≥ 98 %: +0.29″ at cape-town 1973-11-10 10:32 illu=0.00000% elong=0.0070°
- j2000, after, clear of k < 2 % and k ≥ 98 %: -5.34″ at cape-town 2100-12-31 23:59 illu=77.26313% elong=18.0192°
- j2000, after, k < 2 % or k ≥ 98 %: +12451.84″ at cape-town 2098-11-10 07:16 illu=0.00027% elong=0.0592°

Horizons' /T (trailing: east of the Sun, evening) and /L (leading: west, morning) flag, against two definitions of east, same rows:
- apparent right ascension of date, Mercury minus Sun: agrees on 719, disagrees on 0
- apparent ecliptic longitude of date, Mercury minus Sun: agrees on 639, disagrees on 80

The frame matters: χ in ICRF/J2000 minus χ of date, Horizons' own positions, largest by era:
- 1900-1961: -0.589° at oslo 1900-01-01 00:00 illu=76.76367% elong=21.1836°
- 1962-1971: -0.220° at cape-town 1962-01-01 00:00 illu=96.25061% elong=9.3715°
- 1972-2027-06: -0.163° at quito 1972-01-01 00:00 illu=62.28487% elong=22.6195°
- after: -0.595° at quito 2100-05-16 16:00 illu=36.79342% elong=21.8572°

## DE440 against DE442s (daily, 1900-01-01 to 2100-12-31, TT)

| quantity | worst | where |
|---|---|---|
| Mercury barycentric position | +0.504063 km | 2095.45 |
| Earth barycentric position | +0.494645 km | 2095.72 |
| Sun barycentric position | +0.483898 km | 2095.34 |
| geocentric direction of Mercury | +0.000049 ″ | 1929.44 |
| geocentric distance of Mercury | +0.022924 km | 2099.22 |
| geocentric direction, 1900–1962 | 0.000049 ″ | |
| geocentric direction, 1962–2027.49 | 0.000027 ″ | |
| geocentric direction, 2027.49–2101 | 0.000045 ″ | |

## The named events: Espenak (AstroPixels) against DE440

### The transits: Espenak (NASA/GSFC) against DE440

Greatest transit: the instant of least geocentric apparent separation of the centres, with that separation beside Espenak's published minimum; contacts I and IV from the apparent radii (the Sun 695 700 km, Mercury 2440.53 km) beside the published ones. Espenak's times are to the minute.

| transit | published I | II | greatest | III | IV | min. sep. | DE440 greatest | difference | DE440 min. sep. | DE440 I | DE440 IV |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1907-transit | 10:24 | 10:26 | 12:07 | 13:47 | 13:50 | 758.6″ | 1907-11-14 12:06:09 | -0.8 min | 758.6″ | 10:23:09 | 13:49:10 |
| 1914-transit | 09:57 | 09:59 | 12:03 | 14:07 | 14:09 | 630.7″ | 1914-11-07 12:02:40 | -0.3 min | 630.7″ | 09:56:37 | 14:08:46 |
| 1924-transit | 21:44 | 21:47 | 01:41 | 05:35 | 05:38 | 84.6″ | 1924-05-08 01:40:39 | -0.3 min | 84.6″ | 21:43:21 | 05:37:47 |
| 1927-transit | 03:02 | 03:04 | 05:46 | 08:27 | 08:29 | 128.7″ | 1927-11-10 05:45:14 | -0.8 min | 128.7″ | 03:01:45 | 08:28:48 |
| 1937-transit | 08:53 | - | 08:59 | - | 09:06 | 955.5″ | 1937-05-11 08:58:58 | -0.0 min | 955.5″ | - | - |
| 1940-transit | 20:49 | 20:51 | 23:21 | 01:52 | 01:53 | 368.5″ | 1940-11-11 23:20:49 | -0.2 min | 368.5″ | 20:48:36 | 01:53:06 |
| 1953-transit | 15:37 | 15:41 | 16:54 | 18:07 | 18:11 | 861.8″ | 1953-11-14 16:53:34 | -0.4 min | 861.8″ | 15:36:56 | 18:10:13 |
| 1957-transit | 23:59 | 00:09 | 01:14 | 02:20 | 02:30 | 907.3″ | 1957-05-06 01:14:03 | +0.1 min | 907.3″ | 23:58:58 | 02:29:06 |
| 1960-transit | 14:34 | 14:36 | 16:53 | 19:10 | 19:12 | 527.9″ | 1960-11-07 16:52:44 | -0.3 min | 527.9″ | 14:33:45 | 19:11:47 |
| 1970-transit | 04:19 | 04:22 | 08:16 | 12:10 | 12:13 | 114.1″ | 1970-05-09 08:16:08 | +0.1 min | 114.1″ | 04:19:23 | 12:12:43 |
| 1973-transit | 07:47 | 07:49 | 10:32 | 13:16 | 13:17 | 26.4″ | 1973-11-10 10:32:14 | +0.2 min | 26.4″ | 07:47:26 | 13:17:07 |
| 1986-transit | 01:43 | 01:45 | 04:07 | 06:29 | 06:31 | 470.5″ | 1986-11-13 04:07:01 | +0.0 min | 470.5″ | 01:43:06 | 06:31:00 |
| 1993-transit | 03:06 | 03:12 | 03:57 | 04:41 | 04:47 | 926.7″ | 1993-11-06 03:56:31 | -0.5 min | 926.7″ | 03:06:04 | 04:46:58 |
| 1999-transit | 21:15 | 21:30 | 21:41 | 21:52 | 22:07 | 963.0″ | 1999-11-15 21:40:53 | -0.1 min | 963.0″ | 21:15:26 | 22:06:20 |
| 2003-transit | 05:13 | 05:17 | 07:52 | 10:27 | 10:32 | 708.3″ | 2003-05-07 07:52:24 | +0.4 min | 708.3″ | 05:13:05 | 10:31:38 |
| 2006-transit | 19:12 | 19:14 | 21:41 | 00:08 | 00:10 | 422.9″ | 2006-11-08 21:41:04 | +0.1 min | 422.9″ | 19:12:08 | 00:10:04 |
| 2016-transit | 11:12 | 11:15 | 14:57 | 18:39 | 18:42 | 318.5″ | 2016-05-09 14:57:25 | +0.4 min | 318.5″ | 11:12:23 | 18:42:18 |
| 2019-transit | 12:35 | 12:37 | 15:20 | 18:02 | 18:04 | 75.9″ | 2019-11-11 15:19:48 | -0.2 min | 75.9″ | 12:35:31 | 18:04:10 |
| 2032-transit | 06:41 | 06:43 | 08:54 | 11:05 | 11:07 | 572.1″ | 2032-11-13 08:54:13 | +0.2 min | 572.1″ | 06:41:13 | 11:07:16 |
| 2039-transit | 07:17 | 07:21 | 08:46 | 10:12 | 10:15 | 822.3″ | 2039-11-07 08:46:54 | +0.9 min | 822.3″ | 07:18:16 | 10:15:35 |
| 2049-transit | 11:03 | 11:07 | 14:24 | 17:41 | 17:44 | 511.8″ | 2049-05-07 14:24:34 | +0.6 min | 511.8″ | 11:04:07 | 17:44:53 |
| 2052-transit | 23:53 | 23:55 | 02:29 | 05:04 | 05:06 | 318.7″ | 2052-11-09 02:30:22 | +1.4 min | 318.7″ | 23:54:12 | 05:06:36 |
| 2062-transit | 18:16 | 18:20 | 21:36 | 00:53 | 00:57 | 520.5″ | 2062-05-10 21:37:44 | +1.7 min | 520.5″ | 18:17:22 | 00:57:59 |
| 2065-transit | 17:24 | 17:26 | 20:06 | 22:46 | 22:48 | 180.7″ | 2065-11-11 20:07:11 | +1.2 min | 180.7″ | 17:25:20 | 22:49:07 |
| 2078-transit | 11:42 | 11:44 | 13:41 | 15:37 | 15:39 | 674.3″ | 2078-11-14 13:42:26 | +1.4 min | 674.3″ | 11:43:53 | 15:41:02 |
| 2085-transit | 11:42 | 11:45 | 13:34 | 15:24 | 15:26 | 718.5″ | 2085-11-07 13:36:12 | +2.2 min | 718.5″ | 11:44:26 | 15:28:02 |
| 2095-transit | 17:20 | 17:24 | 21:05 | 00:47 | 00:50 | 309.8″ | 2095-05-08 21:07:30 | +2.5 min | 309.8″ | 17:22:48 | 00:52:04 |
| 2098-transit | 04:35 | 04:37 | 07:16 | 09:56 | 09:57 | 214.7″ | 2098-11-10 07:18:43 | +2.7 min | 214.7″ | 04:37:36 | 09:59:55 |

Conjunction: Mercury and the Sun at one apparent ecliptic longitude of date; greatest elongation: the maximum of the geocentric apparent Sun-Mercury angle. Espenak states ±30 minutes for both.

| case | published row (UT, to the hour) | DE440 instant (UT) | difference | DE440 greatest elongation, or the conjunction in RA | its excess over the listed hour's |
|---|---|---|---|---|---|
| 2012-superior-conjunction-1 | Feb 07 09 Mercury at Superior Conjunction | 2012-02-07 09:02:29 | +2 min | in right ascension 2012-02-06 11:36 | |
| 2012-greatest-elongation-east-1 | Mar 05 09 Mercury at Greatest Elong: 18.2°E | 2012-03-05 09:34:45 | +35 min | 18.210° | 0.13″ |
| 2012-inferior-conjunction-1 | Mar 21 19 Mercury at Inferior Conjunction | 2012-03-21 19:20:32 | +21 min | in right ascension 2012-03-21 00:40 | |
| 2012-greatest-elongation-west-1 | Apr 18 17 Mercury at Greatest Elong: 27.5°W | 2012-04-18 17:22:50 | +23 min | 27.493° | 0.02″ |
| 2012-superior-conjunction-2 | May 27 11 Mercury at Superior Conjunction | 2012-05-27 11:19:19 | +19 min | in right ascension 2012-05-27 13:03 | |
| 2012-greatest-elongation-east-2 | Jul 01 02 Mercury at Greatest Elong: 25.7°E | 2012-07-01 01:57:02 | -3 min | 25.743° | 0.00″ |
| 2012-inferior-conjunction-2 | Jul 28 20 Mercury at Inferior Conjunction | 2012-07-28 19:57:14 | -3 min | in right ascension 2012-07-28 02:06 | |
| 2012-greatest-elongation-west-2 | Aug 16 12 Mercury at Greatest Elong: 18.7°W | 2012-08-16 12:03:58 | +4 min | 18.692° | 0.00″ |
| 2012-superior-conjunction-3 | Sep 10 13 Mercury at Superior Conjunction | 2012-09-10 12:43:47 | -16 min | in right ascension 2012-09-09 18:10 | |
| 2012-greatest-elongation-east-3 | Oct 26 22 Mercury at Greatest Elong: 24.1°E | 2012-10-26 22:11:31 | +12 min | 24.081° | 0.01″ |
| 2012-inferior-conjunction-3 | Nov 17 16 Mercury at Inferior Conjunction | 2012-11-17 15:46:37 | -13 min | in right ascension 2012-11-17 16:49 | |
| 2012-greatest-elongation-west-3 | Dec 04 23 Mercury at Greatest Elong: 20.6°W | 2012-12-04 22:48:27 | -12 min | 20.551° | 0.01″ |
| 2026-superior-conjunction-1 | Jan 21 16 Mercury at Superior Conjunction | 2026-01-21 15:48:53 | -11 min | in right ascension 2026-01-20 23:16 | |
| 2026-greatest-elongation-east-1 | Feb 19 18 Mercury at Greatest Elong: 18.1°E | 2026-02-19 17:41:08 | -19 min | 18.123° | 0.04″ |
| 2026-inferior-conjunction-1 | Mar 07 11 Mercury at Inferior Conjunction | 2026-03-07 11:01:43 | +2 min | in right ascension 2026-03-06 16:27 | |
| 2026-greatest-elongation-west-1 | Apr 03 23 Mercury at Greatest Elong: 27.8°W | 2026-04-03 22:33:34 | -26 min | 27.819° | 0.03″ |
| 2026-superior-conjunction-2 | May 14 14 Mercury at Superior Conjunction | 2026-05-14 14:24:22 | +24 min | in right ascension 2026-05-14 15:11 | |
| 2026-greatest-elongation-east-2 | Jun 15 20 Mercury at Greatest Elong: 24.5°E | 2026-06-15 19:59:50 | -0 min | 24.517° | 0.00″ |
| 2026-inferior-conjunction-2 | Jul 13 01 Mercury at Inferior Conjunction | 2026-07-13 01:25:45 | +26 min | in right ascension 2026-07-12 14:34 | |
| 2026-greatest-elongation-west-2 | Aug 02 08 Mercury at Greatest Elong: 19.5°W | 2026-08-02 08:07:28 | +7 min | 19.468° | 0.00″ |
| 2026-superior-conjunction-3 | Aug 27 17 Mercury at Superior Conjunction | 2026-08-27 17:03:47 | +4 min | in right ascension 2026-08-27 00:40 | |
| 2026-greatest-elongation-east-3 | Oct 12 10 Mercury at Greatest Elong: 25.2°E | 2026-10-12 10:03:18 | +3 min | 25.161° | 0.00″ |
| 2026-inferior-conjunction-3 | Nov 04 14 Mercury at Inferior Conjunction | 2026-11-04 14:24:18 | +24 min | in right ascension 2026-11-04 13:05 | |
| 2026-greatest-elongation-west-3 | Nov 20 23 Mercury at Greatest Elong: 19.6°W | 2026-11-20 23:31:17 | +31 min | 19.622° | 0.07″ |
| 2100-greatest-elongation-east-1 | Jan 23 16 Mercury at Greatest Elong: 18.6°E | 2100-01-23 15:48:09 | -12 min | 18.621° | 0.01″ |
| 2100-inferior-conjunction-1 | Feb 08 04 Mercury at Inferior Conjunction | 2100-02-08 04:20:02 | +20 min | in right ascension 2100-02-07 15:42 | |
| 2100-greatest-elongation-west-1 | Mar 05 23 Mercury at Greatest Elong: 27.2°W | 2100-03-05 22:36:00 | -24 min | 27.181° | 0.02″ |
| 2100-superior-conjunction-1 | Apr 18 17 Mercury at Superior Conjunction | 2100-04-18 17:12:37 | +13 min | in right ascension 2100-04-18 11:56 | |
| 2100-greatest-elongation-east-2 | May 16 16 Mercury at Greatest Elong: 21.9°E | 2100-05-16 15:45:41 | -14 min | 21.856° | 0.01″ |
| 2100-inferior-conjunction-2 | Jun 10 05 Mercury at Inferior Conjunction | 2100-06-10 05:35:54 | +36 min | in right ascension 2100-06-10 09:19 | |
| 2100-greatest-elongation-west-2 | Jul 04 06 Mercury at Greatest Elong: 21.7°W | 2100-07-04 05:48:22 | -12 min | 21.719° | 0.01″ |
| 2100-superior-conjunction-2 | Aug 01 06 Mercury at Superior Conjunction | 2100-08-01 05:46:40 | -13 min | in right ascension 2100-07-31 20:16 | |
| 2100-greatest-elongation-east-3 | Sep 13 16 Mercury at Greatest Elong: 26.8°E | 2100-09-13 15:57:17 | -3 min | 26.841° | 0.00″ |
| 2100-inferior-conjunction-3 | Oct 09 07 Mercury at Inferior Conjunction | 2100-10-09 07:03:22 | +3 min | in right ascension 2100-10-08 21:15 | |
| 2100-greatest-elongation-west-3 | Oct 24 20 Mercury at Greatest Elong: 18.3°W | 2100-10-24 19:49:45 | -10 min | 18.340° | 0.01″ |
| 2100-superior-conjunction-3 | Nov 28 07 Mercury at Superior Conjunction | 2100-11-28 07:39:17 | +39 min | in right ascension 2100-11-28 12:35 | |

## What a second of time costs

- Mercury's fastest astrometric motion on the kept grids: 0.0915″ per second of time (5.49″ a minute) at 2064-06-02 00:00 (matrix-7d-geocentric-199); its fastest range rate 49.15 km/s.
- The Earth turns 15.04″ per second of UT1: a time error moves the horizontal place by up to that.
- So one second of civil-time error costs Mercury's equatorial place at most 0.092″ and its distance at most 49.2 km, and its horizontal place up to 15″.
- ΔT at 2027-06-28 00:00:00: the atlas's 76.0 s (ESTIMATED_AFTER_RECORD), Skyfield's 69.1 s; apart +6.9 s.
- ΔT at 2050-01-01 00:00:00: the atlas's 93.0 s (ESTIMATED_AFTER_RECORD), Skyfield's 71.4 s; apart +21.6 s.
- ΔT at 2100-12-31 23:59:59: the atlas's 205.1 s (ESTIMATED_AFTER_RECORD), Skyfield's 96.7 s; apart +108.4 s.
- Horizons' own ΔT is not printed; the Skyfield passes' 411″ horizontal says it and Skyfield's part by about 27 s by 2100, and the proposed contract's after-era astrometric and distance differences above are the atlas's ΔT parting from Horizons' at the rates above - the cost of the accepted model, measured.

## The radius: the PCK's equatorial figure against the IAU mean and the polar radius

| case | distance | equatorial 2440.53 km | mean 2439.4 km | polar 2438.26 km | mean − equatorial | Horizons Ang-diam |
|---|---|---|---|---|---|---|
| nearest (geocentric, 7-day), 1936-06-01 | 82,147,074 km | 12.256″ | 12.250″ | 12.245″ | -0.006″ (-0.05 %) | 12.256″ |
| farthest (geocentric, 7-day), 2034-12-04 | 217,141,880 km | 4.637″ | 4.634″ | 4.632″ | -0.002″ (-0.05 %) | 4.637″ |
