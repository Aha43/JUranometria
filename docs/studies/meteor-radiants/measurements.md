# Meteor showers at chart time, as built (#716)

Sprint 60, issue #716, road #714; ruled on #715 on 2026-11-11 and at the hands-on checkpoint of 2026-11-14 (`docs/decisions/meteor-showers.md`). Written for the reader article #717. Every page is the application's own chart with its production modules attached, the selected shower drawn by the production meteor module; every sentence quoted is the panel's or the page's own; every value is read from the bundled pack or computed by the production services. No raw source is read. What depends on this machine's fonts is in `platform.md`. Regenerate with `make meteor-radiant-study`.

Source: International Meteor Organization (IMO) – www.imo.net. Edition: IMO Meteor Shower Calendar 2027, working list correct as of 2026-06. Identities and parent bodies: the IAU Meteor Data Center. Positions between the printed rows, instants and everything for a place are JUranometria's calculations.

## 1. The nine showers the atlas carries

As the bundled pack holds them, and as the panel words them. Activity and the maximum are the calendar's dates in its own year, 2027, kept in the pack as the calendar prints them and shown with each language's own month names; the solar longitudes beside them are what the atlas evaluates. ZHR, population index and entry speed are printed as the source prints them.

| code | name | Norwegian name | activity | activity, in Norwegian | solar longitude | maximum at | ZHR | population index | km/s | radiant rows | parent body |
|---|---|---|---|---|---|---|---|---|---|---|---|
| QUA | Quadrantids | Kvadrantidene | Dec 28 - Jan 12 | 28. des. - 12. jan. | 275.9 - 292.2 | 283.15 | 80+ | 2.1 | 41 | 4 | 2003 EH1 |
| LYR | Lyrids | Lyridene | Apr 14 - Apr 30 | 14. apr. - 30. apr. | 23.5 - 40.0 | 32.32 | 18 | 2.1 | 49 | 4 | C/1861 G1 (Thatcher) |
| ETA | Eta Aquariids | Eta-akvaridene | Apr 19 - May 28 | 19. apr. - 28. mai | 28.3 - 67.1 | 45.5 | 50 | 2.4 | 66 | 8 | 1P/Halley |
| SDA | Southern Delta Aquariids | Sørlige delta-akvarider | Jul 12 - Aug 23 | 12. juli - 23. aug. | 109.1 - 150.2 | 128 | 25 | 2.5 | 41 | 9 | - |
| PER | Perseids | Perseidene | Jul 17 - Aug 24 | 17. juli - 24. aug. | 113.9 - 151.2 | 140.0 | 110+ | 2.2 | 59 | 9 | 109P/Swift-Tuttle |
| ORI | Orionids | Orionidene | Oct 02 - Nov 07 | 2. okt. - 7. nov. | 188.2 - 225.0 | 208 | 20 | 2.5 | 66 | 7 | 1P/Halley |
| LEO | Leonids | Leonidene | Nov 06 - Nov 30 | 6. nov. - 30. nov. | 223.0 - 248.2 | 235.27 | 15+ | 2.5 | 71 | 5 | 55P/Tempel–Tuttle |
| GEM | Geminids | Geminidene | Dec 04 - Dec 20 | 4. des. - 20. des. | 251.2 - 268.5 | 262.2 | 150 | 2.6 | 35 | 4 | (3200) Phaethon |
| URS | Ursids | Ursidene | Dec 17 - Dec 26 | 17. des. - 26. des. | 264.4 - 274.6 | 270.7 | 10 | 2.8 | 33 | 2 | 8P/Tuttle |

## 2. The pages

Every page is from Oslo (59.91 N, 10.75 E). An ordinary page is 60 degrees wide and 1440 by 900 pixels; the whole sky is 2000 by 1000. The horizon is drawn and Subdue below horizon is on. Pages of one shower share one centre, the radiant at the first page's chart time, so that the mark's movement is the radiant's.

| page | what it shows | chart time (UTC) | shower | solar longitude | radiant J2000 | footing | altitude | Sun altitude | drawn at | drawn as below the horizon |
|---|---|---|---|---|---|---|---|---|---|---|
| `perseids-night-en-white.png` | the night of the nominal maximum | 2027-08-13 00:00 | PER | 139.67 | 48.6 +57.6 | between rows | +52.3 | -14.9 | 720, 450 | no |
| `perseids-night-nb-black.png` | the same page, Norwegian, black sky | 2027-08-13 00:00 | PER | 139.67 | 48.6 +57.6 | between rows | +52.3 | -14.9 | 720, 450 | no |
| `perseids-early-en-white.png` | the same field 24 days earlier | 2027-07-20 00:00 | PER | 116.71 | 11.0 +52.0 | between rows | +55.3 | -9.0 | 1243, 437 | no |
| `perseids-late-en-white.png` | the same field 7 days later | 2027-08-20 00:00 | PER | 146.40 | 57.0 +58.0 | between rows | +51.8 | -17.1 | 615, 435 | no |
| `eta-aquariids-below-horizon-en-white.png` | a radiant below the horizon | 2027-05-06 23:00 | ETA | 45.82 | 338.6 -0.2 | between rows | -14.0 | -13.4 | 720, 450 | yes |
| `geminids-evening-en-white.png` | a radiant with the Moon in the field | 2027-12-14 20:00 | GEM | 262.20 | 112.8 +33.0 | between rows | +33.3 | -40.5 | 720, 450 | no |
| `geminids-whole-sky-en-white.png` | the whole sky at the same chart time | 2027-12-14 20:00 | GEM | 262.20 | 112.8 +33.0 | between rows | +33.3 | -40.5 | 705, 437 | no |

On the three Perseid pages of one field the mark stands 524 pixels apart between 2027-07-20 and 2027-08-13, and 106 between 2027-08-13 and 2027-08-20: on the sky 22.1 and 4.5 degrees.

## 3. Selected, and not drawn

The Perseids stay selected while the chart time leaves their activity. The module then offers no radiant, and the page draws none.

| chart time (UTC) | solar longitude | active | radiants the module offers |
|---|---|---|---|
| 2027-08-13 00:00 | 139.67 | yes | 1 |
| 2027-08-26 00:00 | 152.18 | no | 0 |
| 2027-12-14 20:00 | 262.20 | no | 0 |

## 4. The radiant moves

The Perseid radiant at 00:00 UTC through the 2027 activity, as the production service places it, and its distance from where it stands at the nominal maximum.

| date (00:00 UTC) | solar longitude | right ascension | declination | footing | from its place at maximum |
|---|---|---|---|---|---|
| 2027-07-18 | 114.80 | 8.9 | +51.2 | between rows | 23.8 |
| 2027-07-20 | 116.71 | 11.0 | +52.0 | between rows | 22.3 |
| 2027-07-25 | 121.48 | 22.0 | +53.0 | between rows | 15.9 |
| 2027-07-30 | 126.26 | 29.0 | +54.0 | between rows | 11.8 |
| 2027-08-04 | 131.05 | 35.6 | +55.7 | between rows | 7.6 |
| 2027-08-09 | 135.84 | 43.4 | +56.8 | between rows | 3.1 |
| 2027-08-13 | 139.67 | 48.6 | +57.6 | between rows | 0.2 |
| 2027-08-17 | 143.51 | 53.4 | +58.0 | between rows | 2.4 |
| 2027-08-20 | 146.40 | 57.0 | +58.0 | between rows | 4.3 |
| 2027-08-24 | 150.25 | 61.8 | +58.0 | between rows | 6.8 |

Every shower, from the first to the last solar longitude of its activity: how far the radiant travels on the sky, and its mean rate. One degree of solar longitude is about one day.

| shower | at the start | at the end | travels | degrees of solar longitude | per degree |
|---|---|---|---|---|---|
| QUA | 222.0 +49.9 | 235.7 +47.4 | 9.6 | 16.3 | 0.59 |
| LYR | 261.8 +34.0 | 280.0 +34.0 | 15.1 | 16.6 | 0.91 |
| ETA | 322.0 -7.4 | 356.2 +8.6 | 37.7 | 38.7 | 0.97 |
| SDA | 326.6 -19.0 | 359.2 -10.2 | 32.8 | 41.2 | 0.80 |
| PER | 7.9 +50.8 | 63.0 +58.0 | 32.5 | 37.4 | 0.87 |
| ORI | 83.2 +13.4 | 107.0 +17.5 | 23.5 | 36.8 | 0.64 |
| LEO | 144.6 +24.8 | 159.6 +18.8 | 15.3 | 25.2 | 0.61 |
| GEM | 102.0 +33.0 | 119.0 +31.8 | 14.4 | 17.3 | 0.83 |
| URS | 217.0 +77.2 | 217.0 +73.2 | 4.0 | 10.2 | 0.39 |

Across the nine the radiant travels between 4.0 and 37.7 degrees during the activity.

## 5. The nominal maximum is an instant, and it is not a date

The instant at which the production Sun reaches each shower's listed solar longitude, in three consecutive years, and as the panel words it: no finer than the listing warrants.

| shower | listed solar longitude | 2026 (UTC) | 2027 (UTC) | 2028 (UTC) | the panel, for 2027 |
|---|---|---|---|---|---|
| QUA | 283.15 | 2026-01-03 21:16 | 2027-01-04 03:26 | 2028-01-04 09:31 | 2027-01-04 03:30 UTC, known to about ten minutes |
| LYR | 32.32 | 2026-04-22 19:37 | 2027-04-23 01:41 | 2028-04-22 07:52 | 2027-04-23 01:40 UTC, known to about ten minutes |
| ETA | 45.5 | 2026-05-06 09:13 | 2027-05-06 15:11 | 2028-05-05 21:21 | 2027-05-06, about 15 h UTC, known to an hour or two |
| SDA | 128 | 2026-07-31 13:26 | 2027-07-31 19:37 | 2028-07-31 01:47 | around 2027-07-31 UTC, known to about a day |
| PER | 140.0 | 2026-08-13 02:00 | 2027-08-13 08:11 | 2028-08-12 14:28 | 2027-08-13, about 08 h UTC, known to an hour or two |
| ORI | 208 | 2026-10-21 18:19 | 2027-10-22 00:37 | 2028-10-21 06:39 | around 2027-10-22 UTC, known to about a day |
| LEO | 235.27 | 2026-11-17 23:43 | 2027-11-18 05:59 | 2028-11-17 12:00 | 2027-11-18 06:00 UTC, known to about ten minutes |
| GEM | 262.2 | 2026-12-14 13:41 | 2027-12-14 19:53 | 2028-12-14 01:57 | 2027-12-14, about 20 h UTC, known to an hour or two |
| URS | 270.7 | 2026-12-22 22:09 | 2027-12-23 04:21 | 2028-12-22 10:19 | 2027-12-23, about 04 h UTC, known to an hour or two |

For 5 of the nine the nominal maximum falls on a different UTC calendar date in 2027 than in 2026.

## 6. At the nominal maximum, from two places

What the panel's action *Set chart time to nominal maximum* finds in 2027: the radiant's altitude and the Sun's at that instant, from Oslo (59.91 N, 10.75 E) and from Cape Town (33.93 S, 18.42 E).

| shower | nominal maximum 2027 (UTC) | radiant altitude, Oslo | Sun altitude, Oslo | radiant altitude, Cape Town | Sun altitude, Cape Town |
|---|---|---|---|---|---|
| QUA | 2027-01-04 03:26 | +52.5 | -33.9 | -7.3 | -3.5 |
| LYR | 2027-04-23 01:41 | +59.3 | -12.0 | +20.0 | -44.6 |
| ETA | 2027-05-06 15:11 | -16.6 | +29.4 | -33.5 | +8.6 |
| SDA | 2027-07-31 19:37 | -12.0 | +0.4 | +16.7 | -43.8 |
| PER | 2027-08-13 08:11 | +66.8 | +33.1 | -12.0 | +28.2 |
| ORI | 2027-10-22 00:37 | +35.1 | -37.9 | +28.3 | -36.0 |
| LEO | 2027-11-18 05:59 | +51.5 | -9.6 | +33.1 | +28.5 |
| GEM | 2027-12-14 19:53 | +32.5 | -39.9 | -5.8 | -20.4 |
| URS | 2027-12-23 04:21 | +67.0 | -26.9 | -21.9 | +8.2 |

From Oslo, 3 of the nine nominal maxima of 2027 fall with the Sun above the horizon and 2 with the radiant below it.

## 7. The night around the nominal maximum

The panel's night summary with the chart time at each 2027 nominal maximum: the hours the Sun stands more than 6, 12 and 18 degrees below the horizon from local mean noon to the next, sampled every five minutes, and the radiant's highest altitude while the Sun is more than 12 degrees down and the shower is active. The sine of that altitude is geometry, not a rate: it is the share of a stream arriving from that height that crosses level ground, against the same stream arriving from overhead.

From Oslo (59.91 N, 10.75 E):

| shower | night of (UTC) | Sun below 6 | below 12 | below 18 | Sun's lowest | radiant's highest in the dark | at (UTC) | sine of that altitude |
|---|---|---|---|---|---|---|---|---|
| QUA | 2027-01-03 / 2027-01-04 | 16.0 h | 14.1 h | 12.4 h | -52.9 | +73.8 | 06:22 | 0.96 |
| LYR | 2027-04-22 / 2027-04-23 | 7.2 h | 4.8 h | none | -17.7 | +58.9 | 01:37 | 0.86 |
| ETA | 2027-05-06 / 2027-05-07 | 5.7 h | 2.4 h | none | -13.4 | -4.1 | 00:22 | - |
| SDA | 2027-07-31 / 2027-08-01 | 5.2 h | none | none | -12.0 | no such dark | - | - |
| PER | 2027-08-12 / 2027-08-13 | 6.4 h | 3.8 h | none | -15.3 | +60.3 | 01:12 | 0.87 |
| ORI | 2027-10-21 / 2027-10-22 | 12.9 h | 11.3 h | 9.8 h | -41.0 | +46.1 | 03:42 | 0.72 |
| LEO | 2027-11-17 / 2027-11-18 | 14.9 h | 13.3 h | 11.6 h | -49.2 | +51.7 | 05:37 | 0.78 |
| GEM | 2027-12-14 / 2027-12-15 | 16.1 h | 14.3 h | 12.6 h | -53.3 | +63.0 | 01:17 | 0.89 |
| URS | 2027-12-22 / 2027-12-23 | 16.2 h | 14.3 h | 12.6 h | -53.5 | +73.7 | 06:22 | 0.96 |

From Cape Town (33.93 S, 18.42 E):

| shower | night of (UTC) | Sun below 6 | below 12 | below 18 | Sun's lowest | radiant's highest in the dark | at (UTC) | sine of that altitude |
|---|---|---|---|---|---|---|---|---|
| QUA | 2027-01-03 / 2027-01-04 | 8.8 h | 7.6 h | 6.3 h | -33.3 | -13.5 | 02:36 | - |
| LYR | 2027-04-22 / 2027-04-23 | 12.2 h | 11.2 h | 10.2 h | -68.4 | +22.0 | 02:51 | 0.38 |
| ETA | 2027-05-06 / 2027-05-07 | 12.5 h | 11.5 h | 10.6 h | -72.7 | +46.3 | 04:26 | 0.72 |
| SDA | 2027-07-31 / 2027-08-01 | 12.7 h | 11.7 h | 10.7 h | -74.2 | +71.3 | 00:56 | 0.95 |
| PER | 2027-08-12 / 2027-08-13 | 12.4 h | 11.4 h | 10.4 h | -70.9 | -1.7 | 04:31 | - |
| ORI | 2027-10-21 / 2027-10-22 | 10.0 h | 9.0 h | 7.9 h | -45.2 | +40.0 | 02:56 | 0.64 |
| LEO | 2027-11-17 / 2027-11-18 | 9.1 h | 8.1 h | 6.8 h | -37.0 | +23.1 | 02:31 | 0.39 |
| GEM | 2027-12-14 / 2027-12-15 | 8.6 h | 7.4 h | 6.1 h | -32.8 | +23.1 | 00:46 | 0.39 |
| URS | 2027-12-22 / 2027-12-23 | 8.6 h | 7.4 h | 6.1 h | -32.6 | -27.7 | 02:26 | - |

## 8. The Moon at the nominal maximum, year by year

The Moon at the instant of each nominal maximum in three consecutive years, from Oslo (59.91 N, 10.75 E): its illuminated fraction, its altitude and its distance from the radiant, from the production Moon. The shower's listed values are the same in every year; the Moon is not.

| shower | 2026: lit, altitude, from the radiant | 2027 | 2028 |
|---|---|---|---|
| QUA | 100%, +46.0, 88 | 12%, -15.6, 76 | 44%, -3.2, 112 |
| LYR | 35%, +42.5, 118 | 95%, +4.0, 69 | 6%, +32.6, 87 |
| ETA | 82%, -27.1, 65 | 0%, +33.7, 67 | 89%, +21.6, 153 |
| SDA | 96%, -41.5, 10 | 4%, -2.1, 128 | 74%, -28.4, 90 |
| PER | 0%, -12.3, 84 | 86%, -56.7, 134 | 59%, -8.5, 40 |
| ORI | 78%, +17.6, 123 | 58%, +34.0, 16 | 14%, -31.4, 154 |
| LEO | 54%, -10.3, 170 | 80%, +40.5, 39 | 1%, +4.4, 102 |
| GEM | 25%, +12.4, 146 | 98%, +32.6, 15 | 5%, -30.8, 128 |
| URS | 98%, +56.8, 76 | 21%, +2.7, 94 | 36%, -0.9, 102 |

## 9. Words

The panel, word for word, for the chart's place and time. Each block is the whole of what the panel says.

**The Perseids on the night of the nominal maximum (`perseids-night-en-white`)**

> Perseids. IAU 007 PER, Perseids.
> Parent body, as the IAU Meteor Data Center states it: 109P/Swift-Tuttle.
> Active at chart time 2027-08-13 00:00 UTC (the Sun's ecliptic longitude, solar longitude, is 139.7°).
> Activity period: Jul 17 to Aug 24 in the calendar, solar longitude 113.9° to 151.2°. The ends are the calendar's convention, known to about a day.
> Nominal maximum at solar longitude 140.0°; this activity period's: 2027-08-13, about 08 h UTC, known to an hour or two.
> Zenithal hourly rate (ZHR) 110+: the rate for an ideal dark sky with the radiant overhead, not what an observer will count. Population index 2.2; entry speed 59 km/s.
>
> At chart time 2027-08-13 00:00 UTC, 59.91° north, 10.75° east:
> Radiant: right ascension 49°, declination +58° (J2000, to a whole degree as the source prints it); altitude +52°.
> Sun: altitude −15°.
> Moon: 84 % lit (waxing gibbous), altitude −10°, 137° from the radiant.
>
> The night around chart time, 2027-08-12 11:17 to 2027-08-13 11:17 UTC (local mean noon to noon, sampled every five minutes):
> Sun more than 6° below the horizon: 6.4 h; more than 12°: 3.8 h; more than 18°: none. The Sun's lowest: −15°.
> The radiant's highest while the Sun is more than 12° down: +60° at 01:12 UTC.
>
> Values are nominal, from the IMO Meteor Shower Calendar 2027 (working list correct as of 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Positions between the printed rows, instants and everything for this place are computed by JUranometria.

**The same, in Norwegian (`perseids-night-nb-black`)**

> Perseidene. IAU 007 PER, Perseids.
> Opphavslegeme, slik IAU Meteor Data Center oppgir det: 109P/Swift-Tuttle.
> Aktiv på karttidspunktet 2027-08-13 00:00 UTC (solens ekliptiske lengde, sollengde, er 139.7°).
> Aktivitetsperiode: 17. juli til 24. aug. i kalenderen, sollengde 113.9° til 151.2°. Endene er kalenderens konvensjon, riktige til omtrent et døgn.
> Nominelt maksimum ved sollengde 140.0°; denne aktivitetsperiodens: 2027-08-13, rundt kl. 08 UTC, kjent til en time eller to.
> Zenital timerate (ZHR) 110+: raten for en ideell mørk himmel med radianten i senit, ikke det en observatør vil telle. Populasjonsindeks 2.2; inngangsfart 59 km/s.
>
> På karttidspunktet 2027-08-13 00:00 UTC, 59.91° nord, 10.75° øst:
> Radiant: rektascensjon 49°, deklinasjon +58° (J2000, til hele grader slik kilden trykker den); høyde +52°.
> Solen: høyde −15°.
> Månen: 84 % belyst (tiltagende måne, mer enn halv), høyde −10°, 137° fra radianten.
>
> Natten rundt karttiden, 2027-08-12 11:17 til 2027-08-13 11:17 UTC (fra lokal middeltid kl. 12 til kl. 12, prøvd hvert femte minutt):
> Solen mer enn 6° under horisonten: 6.4 t; mer enn 12°: 3.8 t; mer enn 18°: ingen. Solens laveste: −15°.
> Radiantens høyeste mens Solen er mer enn 12° nede: +60° kl. 01:12 UTC.
>
> Verdiene er nominelle, fra IMO Meteor Shower Calendar 2027 (arbeidslisten riktig per 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Posisjoner mellom de trykte radene, tidspunkter og alt for dette stedet er beregnet av JUranometria.

**The Perseids a week later, their maximum passed (`perseids-late-en-white`)**

> Perseids. IAU 007 PER, Perseids.
> Parent body, as the IAU Meteor Data Center states it: 109P/Swift-Tuttle.
> Active at chart time 2027-08-20 00:00 UTC (the Sun's ecliptic longitude, solar longitude, is 146.4°).
> Activity period: Jul 17 to Aug 24 in the calendar, solar longitude 113.9° to 151.2°. The ends are the calendar's convention, known to about a day.
> Nominal maximum at solar longitude 140.0°; this activity period's has passed: 2027-08-13, about 08 h UTC, known to an hour or two.
> Zenithal hourly rate (ZHR) 110+: the rate for an ideal dark sky with the radiant overhead, not what an observer will count. Population index 2.2; entry speed 59 km/s.
>
> At chart time 2027-08-20 00:00 UTC, 59.91° north, 10.75° east:
> Radiant: right ascension 57°, declination +58° (J2000, to a whole degree as the source prints it); altitude +52°.
> Sun: altitude −17°.
> Moon: 93 % lit (waning gibbous), altitude +30°, 75° from the radiant.
>
> The night around chart time, 2027-08-19 11:17 to 2027-08-20 11:17 UTC (local mean noon to noon, sampled every five minutes):
> Sun more than 6° below the horizon: 7.1 h; more than 12°: 4.8 h; more than 18°: none. The Sun's lowest: −17°.
> The radiant's highest while the Sun is more than 12° down: +63° at 01:42 UTC.
>
> Values are nominal, from the IMO Meteor Shower Calendar 2027 (working list correct as of 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Positions between the printed rows, instants and everything for this place are computed by JUranometria.

**The Eta Aquariids below the horizon (`eta-aquariids-below-horizon-en-white`)**

> Eta Aquariids. IAU 031 ETA, eta-Aquariids.
> Parent body, as the IAU Meteor Data Center states it: 1P/Halley.
> Active at chart time 2027-05-06 23:00 UTC (the Sun's ecliptic longitude, solar longitude, is 45.8°).
> Activity period: Apr 19 to May 28 in the calendar, solar longitude 28.3° to 67.1°. The ends are the calendar's convention, known to about a day.
> Nominal maximum at solar longitude 45.5°; this activity period's has passed: 2027-05-06, about 15 h UTC, known to an hour or two.
> Zenithal hourly rate (ZHR) 50: the rate for an ideal dark sky with the radiant overhead, not what an observer will count. Population index 2.4; entry speed 66 km/s.
>
> At chart time 2027-05-06 23:00 UTC, 59.91° north, 10.75° east:
> Radiant: right ascension 339°, declination 0° (J2000, to a whole degree as the source prints it); altitude −14°.
> Sun: altitude −13°.
> Moon: 0 % lit (near new Moon), altitude −7°, 72° from the radiant.
>
> The night around chart time, 2027-05-06 11:17 to 2027-05-07 11:17 UTC (local mean noon to noon, sampled every five minutes):
> Sun more than 6° below the horizon: 5.7 h; more than 12°: 2.4 h; more than 18°: none. The Sun's lowest: −13°.
> The radiant's highest while the Sun is more than 12° down: −4° at 00:22 UTC.
>
> Values are nominal, from the IMO Meteor Shower Calendar 2027 (working list correct as of 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Positions between the printed rows, instants and everything for this place are computed by JUranometria.

**The Geminids with the Moon near the radiant (`geminids-evening-en-white`)**

> Geminids. IAU 004 GEM, Geminids.
> Parent body, as the IAU Meteor Data Center states it: (3200) Phaethon.
> Active at chart time 2027-12-14 20:00 UTC (the Sun's ecliptic longitude, solar longitude, is 262.2°).
> Activity period: Dec 04 to Dec 20 in the calendar, solar longitude 251.2° to 268.5°. The ends are the calendar's convention, known to about a day.
> Nominal maximum at solar longitude 262.2°; this activity period's has passed: 2027-12-14, about 20 h UTC, known to an hour or two.
> Zenithal hourly rate (ZHR) 150: the rate for an ideal dark sky with the radiant overhead, not what an observer will count. Population index 2.6; entry speed 35 km/s.
>
> At chart time 2027-12-14 20:00 UTC, 59.91° north, 10.75° east:
> Radiant: right ascension 113°, declination +33° (J2000, to a whole degree as the source prints it); altitude +33°.
> Sun: altitude −41°.
> Moon: 98 % lit (near full Moon), altitude +33°, 15° from the radiant.
>
> The night around chart time, 2027-12-14 11:17 to 2027-12-15 11:17 UTC (local mean noon to noon, sampled every five minutes):
> Sun more than 6° below the horizon: 16.1 h; more than 12°: 14.3 h; more than 18°: 12.6 h. The Sun's lowest: −53°.
> The radiant's highest while the Sun is more than 12° down: +63° at 01:17 UTC.
>
> Values are nominal, from the IMO Meteor Shower Calendar 2027 (working list correct as of 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Positions between the printed rows, instants and everything for this place are computed by JUranometria.

**The Geminids, in Norwegian, at the same chart time**

> Geminidene. IAU 004 GEM, Geminids.
> Opphavslegeme, slik IAU Meteor Data Center oppgir det: (3200) Phaethon.
> Aktiv på karttidspunktet 2027-12-14 20:00 UTC (solens ekliptiske lengde, sollengde, er 262.2°).
> Aktivitetsperiode: 4. des. til 20. des. i kalenderen, sollengde 251.2° til 268.5°. Endene er kalenderens konvensjon, riktige til omtrent et døgn.
> Nominelt maksimum ved sollengde 262.2°; denne aktivitetsperiodens er passert: 2027-12-14, rundt kl. 20 UTC, kjent til en time eller to.
> Zenital timerate (ZHR) 150: raten for en ideell mørk himmel med radianten i senit, ikke det en observatør vil telle. Populasjonsindeks 2.6; inngangsfart 35 km/s.
>
> På karttidspunktet 2027-12-14 20:00 UTC, 59.91° nord, 10.75° øst:
> Radiant: rektascensjon 113°, deklinasjon +33° (J2000, til hele grader slik kilden trykker den); høyde +33°.
> Solen: høyde −41°.
> Månen: 98 % belyst (nær fullmåne), høyde +33°, 15° fra radianten.
>
> Natten rundt karttiden, 2027-12-14 11:17 til 2027-12-15 11:17 UTC (fra lokal middeltid kl. 12 til kl. 12, prøvd hvert femte minutt):
> Solen mer enn 6° under horisonten: 16.1 t; mer enn 12°: 14.3 t; mer enn 18°: 12.6 t. Solens laveste: −53°.
> Radiantens høyeste mens Solen er mer enn 12° nede: +63° kl. 01:17 UTC.
>
> Verdiene er nominelle, fra IMO Meteor Shower Calendar 2027 (arbeidslisten riktig per 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Posisjoner mellom de trykte radene, tidspunkter og alt for dette stedet er beregnet av JUranometria.

**The Perseids selected in December, not active**

> Perseids. IAU 007 PER, Perseids.
> Parent body, as the IAU Meteor Data Center states it: 109P/Swift-Tuttle.
> Not active at chart time 2027-12-14 20:00 UTC (the Sun's ecliptic longitude, solar longitude, is 262.2°). No radiant is drawn or described.
> Activity period: Jul 17 to Aug 24 in the calendar, solar longitude 113.9° to 151.2°. The ends are the calendar's convention, known to about a day.
> Nominal maximum at solar longitude 140.0°; next: 2028-08-12, about 14 h UTC, known to an hour or two.
> Zenithal hourly rate (ZHR) 110+: the rate for an ideal dark sky with the radiant overhead, not what an observer will count. Population index 2.2; entry speed 59 km/s.
>
> Values are nominal, from the IMO Meteor Shower Calendar 2027 (working list correct as of 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Positions between the printed rows, instants and everything for this place are computed by JUranometria.

**The same, in Norwegian**

> Perseidene. IAU 007 PER, Perseids.
> Opphavslegeme, slik IAU Meteor Data Center oppgir det: 109P/Swift-Tuttle.
> Ikke aktiv på karttidspunktet 2027-12-14 20:00 UTC (solens ekliptiske lengde, sollengde, er 262.2°). Ingen radiant tegnes eller beskrives.
> Aktivitetsperiode: 17. juli til 24. aug. i kalenderen, sollengde 113.9° til 151.2°. Endene er kalenderens konvensjon, riktige til omtrent et døgn.
> Nominelt maksimum ved sollengde 140.0°; neste: 2028-08-12, rundt kl. 14 UTC, kjent til en time eller to.
> Zenital timerate (ZHR) 110+: raten for en ideell mørk himmel med radianten i senit, ikke det en observatør vil telle. Populasjonsindeks 2.2; inngangsfart 59 km/s.
>
> Verdiene er nominelle, fra IMO Meteor Shower Calendar 2027 (arbeidslisten riktig per 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Posisjoner mellom de trykte radene, tidspunkter og alt for dette stedet er beregnet av JUranometria.

**No shower selected, in December**

> Active at chart time 2027-12-14 20:00 UTC: Geminids.
> Values are nominal, from the IMO Meteor Shower Calendar 2027 (working list correct as of 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Positions between the printed rows, instants and everything for this place are computed by JUranometria.

**The same, in Norwegian**

> Aktive på karttidspunktet 2027-12-14 20:00 UTC: Geminidene.
> Verdiene er nominelle, fra IMO Meteor Shower Calendar 2027 (arbeidslisten riktig per 2026-06). Source: International Meteor Organization (IMO) – www.imo.net. Posisjoner mellom de trykte radene, tidspunkter og alt for dette stedet er beregnet av JUranometria.

The page, aloud. The spoken description of three pages, whole:

- `perseids-night-en-white`: "3h 14.2m, +57° 38′. Centre RA 48.5598, Dec +57.6341 (ICRS J2000). Field 60.0 degrees wide, stereographic projection. Stars to V 5.0. Celestial north is up, and celestial east is left. Perseids (the radiant of a meteor shower)."
- `perseids-night-nb-black`: "3h 14.2m, +57° 38′. Sentrum RA 48.5598, Dec +57.6341 (ICRS J2000). Felt 60.0 grader bredt, stereografisk projeksjon. Stjerner ned til V 5.0. Himmelsk nord er opp, og himmelsk øst er til venstre. Perseidene (radianten til en meteorsverm)."
- `eta-aquariids-below-horizon-en-white`: "22h 34.3m, −0° 13′. Centre RA 338.5679, Dec -0.2161 (ICRS J2000). Field 60.0 degrees wide, stereographic projection. Stars to V 5.0. Celestial north is up, and celestial east is left. East on your horizon. Neptune (below the horizon) (a cartographic symbol, not Neptune's apparent diameter). Eta Aquariids (below the horizon) (the radiant of a meteor shower)."
- the Eta Aquariid page composed in Norwegian (not saved): "22h 34.3m, −0° 13′. Sentrum RA 338.5679, Dec -0.2161 (ICRS J2000). Felt 60.0 grader bredt, stereografisk projeksjon. Stjerner ned til V 5.0. Himmelsk nord er opp, og himmelsk øst er til venstre. Øst på horisonten din. Neptun (under horisonten) (et kartografisk symbol, ikke Neptuns tilsynelatende diameter). Eta-akvaridene (under horisonten) (radianten til en meteorsverm)."

The interface's own names for what the article mentions:

| key | English | Norwegian |
|---|---|---|
| `meteor.title` | Meteor showers | Meteorsvermer |
| `menu.meteorshowers.label` | Meteor showers... | Meteorsvermer... |
| `meteor.selector.label` | Shower: | Sverm: |
| `meteor.selector.none` | None | Ingen |
| `meteor.setMaximum.label` | Set chart time to nominal maximum | Sett karttiden til nominelt maksimum |
| `meteor.centre.label` | Centre on radiant | Sentrer på radianten |
| `meteor.explain` | Choose one of nine annual meteor showers. Everything said of it is for the chart time and place; its radiant is drawn on the chart only while the chart time is inside its activity period. Nothing here judges the night or forecasts the weather. | Velg én av ni årlige meteorsvermer. Alt som sies om den, gjelder karttiden og stedet; radianten tegnes på kartet bare mens karttiden ligger innenfor aktivitetsperioden. Ingenting her vurderer natten eller varsler været. |
| `selected.radiant` | meteor shower radiant | radiant for meteorsverm |
| `inspector.occurrence.radiant` | Meteor shower radiant: the point the shower's meteors appear to come from, at the chart time stated. To a whole degree, as its source prints it. | Radiant for meteorsverm: punktet svermens meteorer ser ut til å komme fra, ved karttiden som er oppgitt. Til hele grader, slik kilden trykker den. |

