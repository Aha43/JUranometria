# The Moon, computed: the first table

Sprint 36, issue #407. Every row below comes from the bundled DE440 excerpt and the pinned IERS leap-second file, offline, by `juranometria.solar.SolarSystemService`; the columns, units and rounding are the contract ruled in #406. Chart position is topocentric astrometric ICRS/J2000; altitude and azimuth are apparent and airless, azimuth from north through east; distance is observer to the light-time-corrected centre; the apparent diameter uses the IAU mean lunar radius of 1 737.4 km; the illuminated fraction follows the phase angle at the Moon; the phase word is the visual category from that fraction and the global waxing/waning sequence; the elongation is the unsigned angle from the apparent Sun in this observer's sky, with E when the Moon is east of the Sun there (the evening sky) and W when west; the lit side is the position angle of the bright limb's midpoint from celestial north through east, with the nearest of sixteen compass points, and is not well-defined near new and full Moon.

Time is exact from 1972-01-01 until 2027-06-28 (the pinned record's own validity); earlier and later instants use an estimated clock correction and are marked *est.* in the Instant column. The named events are Fred Espenak's published instants at minute precision, read from `docs/studies/solar-system/moon-events-2026.txt`; the instant shown is rounded to the minute.

## Oslo (59.91° N, 10.75° E)

| Case | Instant (UTC) | Right ascension (J2000) | Declination (J2000) | Altitude (no refraction) | Azimuth (from north through east) | Distance | Apparent diameter | Illuminated | Phase | Elongation from the Sun | Lit side |
|---|---|---|---|---|---|---|---|---|---|---|---|
| New Moon, June 2026 (Espenak) | 2026-06-15 02:54 | 05h 33m 21.8s | +27° 02′ 20″ | 7.50° | 46.54° | 356 345 km | 33′ 31.3″ | 0.1 % | near new Moon | 3.8° E | not well-defined (near new Moon) |
| First quarter, June 2026 (Espenak) | 2026-06-21 21:55 | 11h 54m 55.4s | −3° 07′ 20″ | 6.74° | 251.42° | 387 650 km | 30′ 48.9″ | 50.1 % | near first quarter | 89.9° E | 293° (west-northwest) |
| Full Moon, June 2026 (Espenak) | 2026-06-29 23:57 | 18h 35m 01.9s | −28° 08′ 49″ | 1.66° | 187.84° | 405 002 km | 29′ 29.7″ | 99.8 % | near full Moon | 175.0° W | not well-defined (near full Moon) |
| Last quarter, July 2026 (Espenak) | 2026-07-07 19:29 | 00h 51m 53.4s | +8° 31′ 00″ | −15.87° (below the horizon) | 36.60° | 380 272 km | 31′ 24.8″ | 50.2 % | near last quarter | 90.1° W | 67° (east-northeast) |
| Nearest perigee of 2026 (Espenak) | 2026-06-14 23:18 | 05h 21m 13.8s | +26° 52′ 41″ | −3.16° (below the horizon) | 2.50° | 357 508 km | 33′ 24.8″ | 0.1 % | near new Moon | 4.3° W | not well-defined (near new Moon) |
| Farthest apogee of 2026 (Espenak) | 2026-12-11 06:46 | 19h 00m 58.0s | −26° 37′ 07″ | −24.44° (below the horizon) | 101.30° | 409 024 km | 29′ 12.3″ | 4.7 % | waxing crescent | 25.1° E | 272° (west) |
| A chosen "today" | 2026-09-29 12:00 | 02h 31m 44.7s | +19° 14′ 08″ | −9.19° (below the horizon) | 341.58° | 372 280 km | 32′ 05.2″ | 90.5 % | waning gibbous | 144.1° W | 66° (east-northeast) |
| Near local solar midnight, midsummer (23:00 UTC) | 2026-06-21 23:00 | 11h 56m 52.0s | −3° 23′ 07″ | −1.23° (below the horizon) | 265.07° | 388 788 km | 30′ 43.5″ | 50.5 % | near first quarter | 90.4° E | 293° (west-northwest) |
| First civil instant | 1900-01-01 00:00 *est.* | 18h 17m 11.1s | −22° 53′ 26″ | −50.88° (below the horizon) | 27.06° | 373 309 km | 31′ 59.9″ | 0.4 % | near new Moon | 7.6° W | not well-defined (near new Moon) |
| Last civil instant | 2101-01-01 00:00 *est.* | 19h 39m 23.1s | −25° 04′ 39″ | −54.75° (below the horizon) | 351.92° | 372 735 km | 32′ 02.9″ | 1.5 % | near new Moon | 13.9° E | not well-defined (near new Moon) |

## Quito (0.18° S, 78.50° W)

| Case | Instant (UTC) | Right ascension (J2000) | Declination (J2000) | Altitude (no refraction) | Azimuth (from north through east) | Distance | Apparent diameter | Illuminated | Phase | Elongation from the Sun | Lit side |
|---|---|---|---|---|---|---|---|---|---|---|---|
| New Moon, June 2026 (Espenak) | 2026-06-15 02:54 | 05h 28m 52.2s | +27° 34′ 15″ | −47.25° (below the horizon) | 312.76° | 361 875 km | 33′ 00.6″ | 0.1 % | near new Moon | 4.4° W | not well-defined (near new Moon) |
| First quarter, June 2026 (Espenak) | 2026-06-21 21:55 | 11h 57m 57.8s | −2° 20′ 22″ | 70.40° | 96.92° | 382 440 km | 31′ 14.1″ | 50.4 % | near first quarter | 90.3° E | 293° (west-northwest) |
| Full Moon, June 2026 (Espenak) | 2026-06-29 23:57 | 18h 39m 22.3s | −27° 18′ 58″ | 7.69° | 117.53° | 404 351 km | 29′ 32.5″ | 99.9 % | near full Moon | 175.7° W | not well-defined (near full Moon) |
| Last quarter, July 2026 (Espenak) | 2026-07-07 19:29 | 00h 47m 37.9s | +9° 18′ 20″ | −36.72° (below the horizon) | 281.68° | 382 347 km | 31′ 14.6″ | 50.8 % | near last quarter | 90.7° W | 67° (east-northeast) |
| Nearest perigee of 2026 (Espenak) | 2026-06-14 23:18 | 05h 16m 29.4s | +27° 51′ 43″ | −4.27° (below the horizon) | 297.96° | 357 613 km | 33′ 24.2″ | 0.3 % | near new Moon | 5.7° W | not well-defined (near new Moon) |
| Farthest apogee of 2026 (Espenak) | 2026-12-11 06:46 | 18h 58m 49.5s | −25° 32′ 46″ | −64.23° (below the horizon) | 184.39° | 412 171 km | 28′ 58.9″ | 4.5 % | waxing crescent | 24.5° E | 270° (west) |
| A chosen "today" | 2026-09-29 12:00 | 02h 28m 26.3s | +20° 17′ 58″ | 16.51° | 291.40° | 369 437 km | 32′ 20.1″ | 90.7 % | waning gibbous | 144.3° W | 64° (east-northeast) |
| Near local solar midnight, midsummer (23:00 UTC) | 2026-06-21 23:00 | 11h 58m 58.8s | −2° 37′ 01″ | 85.70° | 127.03° | 382 344 km | 31′ 14.6″ | 50.7 % | near first quarter | 90.6° E | 293° (west-northwest) |
| First civil instant | 1900-01-01 00:00 *est.* | 18h 12m 27.1s | −22° 10′ 15″ | −18.47° (below the horizon) | 246.47° | 370 355 km | 32′ 15.3″ | 0.6 % | near new Moon | 8.8° W | not well-defined (near new Moon) |
| Last civil instant | 2101-01-01 00:00 *est.* | 19h 35m 14.5s | −24° 31′ 29″ | 3.11° | 245.68° | 367 145 km | 32′ 32.2″ | 1.3 % | near new Moon | 12.9° E | not well-defined (near new Moon) |

## Cape Town (33.93° S, 18.42° E)

| Case | Instant (UTC) | Right ascension (J2000) | Declination (J2000) | Altitude (no refraction) | Azimuth (from north through east) | Distance | Apparent diameter | Illuminated | Phase | Elongation from the Sun | Lit side |
|---|---|---|---|---|---|---|---|---|---|---|---|
| New Moon, June 2026 (Espenak) | 2026-06-15 02:54 | 05h 34m 50.3s | +28° 16′ 14″ | −38.05° (below the horizon) | 78.54° | 361 108 km | 33′ 04.8″ | 0.2 % | near new Moon | 5.0° E | not well-defined (near new Moon) |
| First quarter, June 2026 (Espenak) | 2026-06-21 21:55 | 11h 53m 38.5s | −1° 47′ 15″ | 10.59° | 274.84° | 387 231 km | 30′ 50.9″ | 49.3 % | near first quarter | 89.1° E | 293° (west-northwest) |
| Full Moon, June 2026 (Espenak) | 2026-06-29 23:57 | 18h 34m 23.5s | −27° 07′ 55″ | 74.09° | 290.88° | 399 121 km | 29′ 55.8″ | 99.9 % | near full Moon | 176.1° W | not well-defined (near full Moon) |
| Last quarter, July 2026 (Espenak) | 2026-07-07 19:29 | 00h 52m 57.4s | +9° 49′ 30″ | −44.08° (below the horizon) | 111.16° | 382 977 km | 31′ 11.5″ | 49.5 % | near last quarter | 89.3° W | 67° (east-northeast) |
| Nearest perigee of 2026 (Espenak) | 2026-06-14 23:18 | 05h 21m 47.5s | +28° 00′ 20″ | −79.37° (below the horizon) | 120.93° | 363 458 km | 32′ 52.0″ | 0.2 % | near new Moon | 5.2° W | not well-defined (near new Moon) |
| Farthest apogee of 2026 (Espenak) | 2026-12-11 06:46 | 19h 02m 17.2s | −25° 30′ 42″ | 16.95° | 109.69° | 404 540 km | 29′ 31.7″ | 4.8 % | waxing crescent | 25.3° E | 270° (west) |
| A chosen "today" | 2026-09-29 12:00 | 02h 31m 43.8s | +20° 25′ 16″ | −73.11° (below the horizon) | 220.53° | 377 396 km | 31′ 39.1″ | 90.3 % | waning gibbous | 143.6° W | 65° (east-northeast) |
| Near local solar midnight, midsummer (23:00 UTC) | 2026-06-21 23:00 | 11h 55m 38.2s | −2° 03′ 04″ | −2.35° (below the horizon) | 265.76° | 388 916 km | 30′ 42.9″ | 49.8 % | near first quarter | 89.6° E | 293° (west-northwest) |
| First civil instant | 1900-01-01 00:00 *est.* | 18h 17m 59.4s | −21° 30′ 04″ | −29.41° (below the horizon) | 152.51° | 371 487 km | 32′ 09.4″ | 0.4 % | near new Moon | 7.6° W | not well-defined (near new Moon) |
| Last civil instant | 2101-01-01 00:00 *est.* | 19h 39m 47.8s | −23° 40′ 32″ | −32.59° (below the horizon) | 177.33° | 370 956 km | 32′ 12.1″ | 1.5 % | near new Moon | 14.0° E | not well-defined (near new Moon) |

## Alert (82.50° N, 62.34° W)

| Case | Instant (UTC) | Right ascension (J2000) | Declination (J2000) | Altitude (no refraction) | Azimuth (from north through east) | Distance | Apparent diameter | Illuminated | Phase | Elongation from the Sun | Lit side |
|---|---|---|---|---|---|---|---|---|---|---|---|
| New Moon, June 2026 (Espenak) | 2026-06-15 02:54 | 05h 31m 17.3s | +27° 00′ 06″ | 19.89° | 342.36° | 355 010 km | 33′ 38.9″ | 0.1 % | near new Moon | 3.7° W | not well-defined (near new Moon) |
| First quarter, June 2026 (Espenak) | 2026-06-21 21:55 | 11h 56m 44.1s | −3° 14′ 21″ | 4.10° | 176.99° | 387 942 km | 30′ 47.5″ | 50.5 % | near first quarter | 90.4° E | 293° (west-northwest) |
| Full Moon, June 2026 (Espenak) | 2026-06-29 23:57 | 18h 35m 49.7s | −28° 03′ 42″ | −24.60° (below the horizon) | 118.91° | 407 857 km | 29′ 17.3″ | 99.8 % | near full Moon | 175.1° W | not well-defined (near full Moon) |
| Last quarter, July 2026 (Espenak) | 2026-07-07 19:29 | 00h 50m 26.3s | +8° 26′ 24″ | 2.60° | 323.18° | 378 231 km | 31′ 35.0″ | 50.5 % | near last quarter | 90.4° W | 67° (east-northeast) |
| Nearest perigee of 2026 (Espenak) | 2026-06-14 23:18 | 05h 20m 32.7s | +26° 58′ 30″ | 24.24° | 293.23° | 354 540 km | 33′ 41.6″ | 0.2 % | near new Moon | 4.5° W | not well-defined (near new Moon) |
| Farthest apogee of 2026 (Espenak) | 2026-12-11 06:46 | 18h 59m 05.1s | −26° 40′ 43″ | −33.90° (below the horizon) | 15.08° | 409 952 km | 29′ 08.3″ | 4.6 % | waxing crescent | 24.6° E | 273° (west) |
| A chosen "today" | 2026-09-29 12:00 | 02h 31m 53.6s | +19° 16′ 30″ | 19.54° | 270.23° | 369 126 km | 32′ 21.7″ | 90.5 % | waning gibbous | 144.0° W | 66° (east-northeast) |
| Near local solar midnight, midsummer (23:00 UTC) | 2026-06-21 23:00 | 11h 58m 38.7s | −3° 30′ 34″ | 3.66° | 192.81° | 388 243 km | 30′ 46.1″ | 50.9 % | near first quarter | 90.9° E | 293° (west-northwest) |
| First civil instant | 1900-01-01 00:00 *est.* | 18h 16m 04.0s | −23° 09′ 43″ | −27.38° (below the horizon) | 302.41° | 371 268 km | 32′ 10.5″ | 0.5 % | near new Moon | 7.9° W | not well-defined (near new Moon) |
| Last civil instant | 2101-01-01 00:00 *est.* | 19h 39m 00.5s | −25° 23′ 11″ | −26.46° (below the horizon) | 278.30° | 370 336 km | 32′ 15.4″ | 1.5 % | near new Moon | 13.9° E | not well-defined (near new Moon) |

## Oslo, a daily range through one lunation: 2026-06-15 03:00 to 2026-07-14 09:44 UTC, every day

The end is not on the daily grid, so it is appended and marked †.

| Case | Instant (UTC) | Right ascension (J2000) | Declination (J2000) | Altitude (no refraction) | Azimuth (from north through east) | Distance | Apparent diameter | Illuminated | Phase | Elongation from the Sun | Lit side |
|---|---|---|---|---|---|---|---|---|---|---|---|
|  | 2026-06-15 03:00 | 05h 33m 41.1s | +27° 02′ 42″ | 8.03° | 47.71° | 356 288 km | 33′ 31.7″ | 0.1 % | near new Moon | 3.8° E | not well-defined (near new Moon) |
|  | 2026-06-16 03:00 | 06h 41m 52.7s | +26° 10′ 45″ | 1.89° | 34.65° | 358 246 km | 33′ 20.7″ | 1.7 % | near new Moon | 15.1° E | not well-defined (near new Moon) |
|  | 2026-06-17 03:00 | 07h 46m 57.3s | +23° 21′ 33″ | −4.50° (below the horizon) | 22.01° | 362 043 km | 32′ 59.7″ | 6.3 % | waxing crescent | 28.9° E | 277° (west) |
|  | 2026-06-18 03:00 | 08h 47m 04.6s | +19° 00′ 51″ | −10.75° (below the horizon) | 9.61° | 367 293 km | 32′ 31.4″ | 13.2 % | waxing crescent | 42.5° E | 284° (west-northwest) |
|  | 2026-06-19 03:00 | 09h 41m 59.0s | +13° 39′ 26″ | −16.52° (below the horizon) | 357.22° | 373 490 km | 31′ 59.0″ | 21.9 % | waxing crescent | 55.7° E | 289° (west-northwest) |
|  | 2026-06-20 03:00 | 10h 32m 28.0s | +7° 45′ 06″ | −21.51° (below the horizon) | 344.67° | 380 094 km | 31′ 25.7″ | 31.7 % | waxing crescent | 68.4° E | 292° (west-northwest) |
|  | 2026-06-21 03:00 | 11h 19m 46.0s | +1° 39′ 39″ | −25.51° (below the horizon) | 331.86° | 386 604 km | 30′ 53.9″ | 42.0 % | waxing crescent | 80.7° E | 294° (west-northwest) |
|  | 2026-06-22 03:00 | 12h 05m 11.7s | −4° 20′ 42″ | −28.32° (below the horizon) | 318.79° | 392 610 km | 30′ 25.6″ | 52.4 % | waxing gibbous | 92.5° E | 293° (west-northwest) |
|  | 2026-06-23 03:00 | 12h 49m 58.4s | −10° 03′ 28″ | −29.83° (below the horizon) | 305.60° | 397 810 km | 30′ 01.7″ | 62.3 % | waxing gibbous | 104.0° E | 292° (west-northwest) |
|  | 2026-06-24 03:00 | 13h 35m 10.6s | −15° 18′ 13″ | −29.99° (below the horizon) | 292.50° | 402 017 km | 29′ 42.8″ | 71.4 % | waxing gibbous | 115.3° E | 289° (west-northwest) |
|  | 2026-06-25 03:00 | 14h 21m 41.1s | −19° 54′ 59″ | −28.83° (below the horizon) | 279.78° | 405 143 km | 29′ 29.1″ | 79.7 % | waxing gibbous | 126.2° E | 285° (west-northwest) |
|  | 2026-06-26 03:00 | 15h 10m 06.7s | −23° 43′ 38″ | −26.46° (below the horizon) | 267.64° | 407 185 km | 29′ 20.2″ | 86.7 % | waxing gibbous | 137.1° E | 279° (west) |
|  | 2026-06-27 03:00 | 16h 00m 41.3s | −26° 33′ 50″ | −23.03° (below the horizon) | 256.22° | 408 193 km | 29′ 15.9″ | 92.4 % | waxing gibbous | 147.8° E | 272° (west) |
|  | 2026-06-28 03:00 | 16h 53m 09.8s | −28° 16′ 04″ | −18.70° (below the horizon) | 245.51° | 408 256 km | 29′ 15.6″ | 96.5 % | waxing gibbous | 158.4° E | 262° (west) |
|  | 2026-06-29 03:00 | 17h 46m 46.9s | −28° 43′ 19″ | −13.65° (below the horizon) | 235.42° | 407 470 km | 29′ 19.0″ | 99.0 % | near full Moon | 168.7° E | not well-defined (near full Moon) |
|  | 2026-06-30 03:00 | 18h 40m 27.9s | −27° 52′ 48″ | −8.05° (below the horizon) | 225.80° | 405 930 km | 29′ 25.7″ | 99.8 % | near full Moon | 175.1° W | not well-defined (near full Moon) |
|  | 2026-07-01 03:00 | 19h 33m 07.9s | −25° 46′ 53″ | −2.06° (below the horizon) | 216.46° | 403 711 km | 29′ 35.4″ | 98.8 % | near full Moon | 167.4° W | not well-defined (near full Moon) |
|  | 2026-07-02 03:00 | 20h 24m 01.8s | −22° 32′ 28″ | 4.17° | 207.17° | 400 867 km | 29′ 48.0″ | 96.0 % | waning gibbous | 156.8° W | 84° (east) |
|  | 2026-07-03 03:00 | 21h 12m 55.0s | −18° 19′ 32″ | 10.47° | 197.66° | 397 433 km | 30′ 03.4″ | 91.3 % | waning gibbous | 145.7° W | 76° (east-northeast) |
|  | 2026-07-04 03:00 | 22h 00m 02.3s | −13° 19′ 25″ | 16.64° | 187.64° | 393 438 km | 30′ 21.7″ | 85.0 % | waning gibbous | 134.3° W | 71° (east-northeast) |
|  | 2026-07-05 03:00 | 22h 46m 02.2s | −7° 43′ 49″ | 22.46° | 176.80° | 388 922 km | 30′ 42.9″ | 77.1 % | waning gibbous | 122.7° W | 68° (east-northeast) |
|  | 2026-07-06 03:00 | 23h 31m 49.8s | −1° 44′ 37″ | 27.60° | 164.81° | 383 961 km | 31′ 06.7″ | 67.9 % | waning gibbous | 110.9° W | 66° (east-northeast) |
|  | 2026-07-07 03:00 | 00h 18m 32.0s | +4° 25′ 27″ | 31.67° | 151.40° | 378 690 km | 31′ 32.7″ | 57.6 % | waning gibbous | 98.7° W | 66° (east-northeast) |
|  | 2026-07-08 03:00 | 01h 07m 23.9s | +10° 31′ 34″ | 34.19° | 136.56° | 373 324 km | 31′ 59.9″ | 46.7 % | waning crescent | 86.1° W | 68° (east-northeast) |
|  | 2026-07-09 03:00 | 01h 59m 43.0s | +16° 15′ 09″ | 34.69° | 120.70° | 368 163 km | 32′ 26.8″ | 35.6 % | waning crescent | 73.1° W | 71° (east-northeast) |
|  | 2026-07-10 03:00 | 02h 56m 35.2s | +21° 12′ 36″ | 32.89° | 104.62° | 363 585 km | 32′ 51.3″ | 24.9 % | waning crescent | 59.8° W | 76° (east-northeast) |
|  | 2026-07-11 03:00 | 03h 58m 27.9s | +24° 55′ 53″ | 28.80° | 89.22° | 360 013 km | 33′ 10.9″ | 15.3 % | waning crescent | 46.0° W | 83° (east) |
|  | 2026-07-12 03:00 | 05h 04m 33.6s | +26° 56′ 52″ | 22.74° | 75.05° | 357 853 km | 33′ 22.9″ | 7.7 % | waning crescent | 32.0° W | 91° (east) |
|  | 2026-07-13 03:00 | 06h 12m 34.3s | +26° 56′ 11″ | 15.23° | 62.21° | 357 424 km | 33′ 25.3″ | 2.4 % | waning crescent | 17.9° W | 102° (east-southeast) |
|  | 2026-07-14 03:00 | 07h 19m 22.2s | +24° 51′ 53″ | 6.82° | 50.45° | 358 879 km | 33′ 17.1″ | 0.1 % | near new Moon | 4.3° W | not well-defined (near new Moon) |
|  | 2026-07-14 09:44 † | 07h 36m 40.2s | +24° 15′ 34″ | 50.11° | 142.18° | 355 237 km | 33′ 37.6″ | 0.1 % | near new Moon | 2.7° E | not well-defined (near new Moon) |

## Oslo, an hourly range through a summer night: 2026-06-21 20:00 to 2026-06-22 04:00 UTC

| Case | Instant (UTC) | Right ascension (J2000) | Declination (J2000) | Altitude (no refraction) | Azimuth (from north through east) | Distance | Apparent diameter | Illuminated | Phase | Elongation from the Sun | Lit side |
|---|---|---|---|---|---|---|---|---|---|---|---|
|  | 2026-06-21 20:00 | 11h 51m 48.3s | −2° 39′ 05″ | 19.09° | 225.55° | 385 868 km | 30′ 57.5″ | 49.3 % | near first quarter | 89.1° E | 294° (west-northwest) |
|  | 2026-06-21 21:00 | 11h 53m 23.1s | −2° 53′ 53″ | 13.04° | 239.43° | 386 747 km | 30′ 53.2″ | 49.7 % | near first quarter | 89.5° E | 293° (west-northwest) |
|  | 2026-06-21 22:00 | 11h 55m 04.1s | −3° 08′ 33″ | 6.14° | 252.48° | 387 736 km | 30′ 48.5″ | 50.1 % | near first quarter | 90.0° E | 293° (west-northwest) |
|  | 2026-06-21 23:00 | 11h 56m 52.0s | −3° 23′ 07″ | −1.23° (below the horizon) | 265.07° | 388 788 km | 30′ 43.5″ | 50.5 % | near first quarter | 90.4° E | 293° (west-northwest) |
|  | 2026-06-22 00:00 | 11h 58m 47.1s | −3° 37′ 34″ | −8.72° (below the horizon) | 277.58° | 389 852 km | 30′ 38.5″ | 50.9 % | near first quarter | 90.9° E | 293° (west-northwest) |
|  | 2026-06-22 01:00 | 12h 00m 49.2s | −3° 51′ 57″ | −15.99° (below the horizon) | 290.44° | 390 875 km | 30′ 33.7″ | 51.4 % | near first quarter | 91.4° E | 293° (west-northwest) |
|  | 2026-06-22 02:00 | 12h 02m 57.7s | −4° 06′ 19″ | −22.66° (below the horizon) | 304.06° | 391 809 km | 30′ 29.3″ | 51.9 % | near first quarter | 92.0° E | 293° (west-northwest) |
|  | 2026-06-22 03:00 | 12h 05m 11.7s | −4° 20′ 42″ | −28.32° (below the horizon) | 318.79° | 392 610 km | 30′ 25.6″ | 52.4 % | waxing gibbous | 92.5° E | 293° (west-northwest) |
|  | 2026-06-22 04:00 | 12h 07m 29.7s | −4° 35′ 07″ | −32.52° (below the horizon) | 334.81° | 393 243 km | 30′ 22.6″ | 52.9 % | waxing gibbous | 93.1° E | 293° (west-northwest) |

Dates before 1972-01-01 and after 2027-06-28 use an estimated clock correction. After 2026, uncertainty in local horizon coordinates cannot yet be stated precisely.
