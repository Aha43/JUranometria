# Meteor showers at chart time: measurements (#715)

Sprint 60, issue #715, road #714. Numbers only: every value below is read from an owner-approved raw source held to its checksum, or computed by the atlas's production services (the DE440 Sun and Moon, the J2000 ecliptic, the local sky). Nothing is typed from memory. The proposals these support are in `docs/decisions/meteor-showers.md`. Regenerate with `make meteor-shower-study`.

Credit for every IMO value: Source: International Meteor Organization (IMO) - www.imo.net. IAU values: the IAU Meteor Data Center shower database. GMN values: the Global Meteor Network, CC BY 4.0.

## 1. Inputs

| file under `imports/raw/meteor-showers/` | bytes | SHA-256 |
|---|---|---|
| `imo-cal2027.pdf` | 837985 | `9e6b9a1609fb89ebb8571756b15670b2ccf22e64ed22ae999ff604705d773eec` |
| `imo-cal2026.pdf` | 988146 | `fde5388889ebda9fe13436d793da5e9935ae46b99edf20e0b19f7fe32ce1ed9f` |
| `iaumdc-streamestablisheddata.txt` | 555332 | `67fdf79e83c2e690f35cb0eb6e0b8a5db7b4416bedc264eb61a5fc056dcad0f4` |
| `gmn_shower_table_20230518.txt` | 1736023 | `0f9ae2675c9f6b48b3cc1ea6f83a4b7d39e8910b1eace4f7fd73e1dbe5a98595` |
| `transcribed/imo-2026-table5.tsv` | 2045 | `ba7b4859f24c9934c3eb1ff95a67cb0a64b8bb46412b0b4b59b03205e584a3b7` |
| `transcribed/imo-2027-table5.tsv` | 2092 | `8a4760ba8bfafee56061dd34ab71ec446585ceb66255f905f662cb75c31b2833` |
| `transcribed/imo-2027-table6.tsv` | 4221 | `37a44e58ebfc8bab69b1ca50d28bc9c836e0ec6f395b61f012f17c230961ecc1` |

The IAU list states its own date: last update Nov 10, 20:00:00 UTC, 2026.

## 2. What each source covers

| source | showers | what it holds for each |
|---|---|---|
| IMO calendar 2027, Table 5 | 38 and the Antihelion Source | activity dates, maximum date and solar longitude, radiant at maximum (whole degrees), V-infinity, population index r, ZHR |
| IMO calendar 2027, Table 6 | 37 of them and the Antihelion Source | the radiant in whole degrees at dates five days apart: 234 rows |
| IAU MDC established list | 172 codes, 775 solutions (first-solution status: {1=172}) | name, code, status, and per published solution: solar longitude, radiant, often a daily drift, geocentric velocity, orbit, sometimes a parent body |
| GMN shower table | 426 codes, 36123 samples | the Sun-centred ecliptic radiant, geocentric velocity and measured dispersion every 0.2 degrees of solar longitude |

- IMO working-list showers absent from the IAU established list: ACE, SLY, PUP.
- IMO working-list showers absent from the GMN table: PPU, PUP.
- IMO working-list showers with fewer than two rows in Table 6 (rows): OCT (1), DRA (1), AND (0).

## 3. What changed between the 2026 and 2027 editions of the IMO working list

| shower | field | 2026 edition | 2027 edition |
|---|---|---|---|
| QUA | date of maximum | Jan 03 | Jan 04 |
| QUA | ZHR | 80 | 80+ |
| LYR | date of maximum | Apr 22 | Apr 23 |
| ELY | activity | May 03 - May 14 | May 05 - May 14 |
| ARI | date of maximum | Jun 07 | Jun 08 |
| ERI | date of maximum | Aug 07 | Aug 08 |
| PER | ZHR | 100 | 110+ |
| SPE | date of maximum | Sep 09 | Sep 10 |
| DRA | radiant at maximum | 262 +54 | 263 +56 |
| EGE | date of maximum | Oct 18 | Oct 19 |
| ORI | date of maximum | Oct 21 | Oct 22 |
| LMI | date of maximum | Oct 24 | Oct 25 |
| STA | date of maximum | Nov 05 | Nov 06 |
| NTA | date of maximum | Nov 12 | Nov 13 |
| LEO | date of maximum | Nov 17 | Nov 18 |
| LEO | ZHR | 15 | 15+ |
| NOO | activity | Nov 13 - Dec 06 | Nov 14 - Dec 06 |
| NOO | V-infinity | 44 | 43 |
| PHO | activity | Dec 01 - Dec 05 | Nov 20 - Dec 05 |
| AND | (whole row) | absent | added |
| URS | date of maximum | Dec 22 | Dec 23 |

Of the 37 showers in both editions, the solar longitude of maximum changed for 0 and the calendar date of maximum for 12. The radiant table (Table 6) differs between the editions in one entry (DRA, Oct 10).

## 4. What the listed date of maximum is

The instant in 2027 at which the production Sun reaches each listed solar longitude (equinox J2000), against the date the 2027 edition prints. The calendar's text states one time of day outright - the Quadrantids peak "on January 4 shortly after 03h UT" (p. 3) - which is the one printed clock time this section can be held to.

| shower | listed solar longitude | Sun reaches it (UTC) | listed date | same UTC date |
|---|---|---|---|---|
| QUA | 283.15 | 2027-01-04 03:23 | Jan 04 | yes |
| LYR | 32.32 | 2027-04-23 01:39 | Apr 23 | yes |
| ETA | 45.5 | 2027-05-06 15:14 | May 06 | yes |
| SDA | 128 | 2027-07-31 19:40 | Jul 31 | yes |
| PER | 140.0 | 2027-08-13 08:08 | Aug 13 | yes |
| ORI | 208 | 2027-10-22 00:36 | Oct 22 | yes |
| LEO | 235.27 | 2027-11-18 05:55 | Nov 18 | yes |
| GEM | 262.2 | 2027-12-14 19:57 | Dec 14 | yes |
| URS | 270.7 | 2027-12-23 04:18 | Dec 23 | yes |

The rows are #564's nine candidates; the count is the whole working list. 38 of 38 listed dates are the UTC date on which the Sun reaches the listed solar longitude. A longitude printed to a whole degree is known to half a degree, which is 12 hours; to one decimal, 1.2 hours.

## 5. A solar longitude is not a calendar date

The UTC instant at which the Sun reaches the listed solar longitude of maximum, by year, for #564's candidate showers. The pack serves 1900-2100.

| shower | 1900 | 1950 | 2000 | 2024 | 2025 | 2026 | 2027 | 2028 | 2050 | 2100 | span |
|---|---|---|---|---|---|---|---|---|---|---|---|
| QUA | Jan 02 13h | Jan 03 09h | Jan 04 05h | Jan 04 08h | Jan 03 15h | Jan 03 21h | Jan 04 03h | Jan 04 09h | Jan 04 00h | Jan 04 20h | 2.3 d |
| LYR | Apr 21 12h | Apr 22 07h | Apr 22 03h | Apr 22 07h | Apr 22 13h | Apr 22 19h | Apr 23 01h | Apr 22 07h | Apr 22 23h | Apr 23 18h | 2.3 d |
| ETA | May 05 01h | May 05 21h | May 05 17h | May 05 20h | May 06 02h | May 06 09h | May 06 15h | May 05 21h | May 06 12h | May 07 08h | 2.3 d |
| SDA | Jul 30 06h | Jul 31 01h | Jul 30 21h | Jul 31 01h | Jul 31 07h | Jul 31 13h | Jul 31 19h | Jul 31 01h | Jul 31 17h | Aug 01 12h | 2.3 d |
| PER | Aug 11 19h | Aug 12 14h | Aug 12 10h | Aug 12 13h | Aug 12 19h | Aug 13 01h | Aug 13 08h | Aug 12 14h | Aug 13 05h | Aug 14 01h | 2.2 d |
| ORI | Oct 20 11h | Oct 21 06h | Oct 21 02h | Oct 21 06h | Oct 21 12h | Oct 21 18h | Oct 22 00h | Oct 21 06h | Oct 21 22h | Oct 22 17h | 2.3 d |
| LEO | Nov 16 16h | Nov 17 12h | Nov 17 07h | Nov 17 11h | Nov 17 17h | Nov 17 23h | Nov 18 05h | Nov 17 12h | Nov 18 03h | Nov 18 23h | 2.3 d |
| GEM | Dec 13 06h | Dec 14 01h | Dec 13 21h | Dec 14 01h | Dec 14 07h | Dec 14 13h | Dec 14 19h | Dec 14 01h | Dec 14 17h | Dec 15 13h | 2.3 d |
| URS | Dec 21 14h | Dec 22 10h | Dec 22 06h | Dec 22 09h | Dec 22 16h | Dec 22 22h | Dec 23 04h | Dec 22 10h | Dec 23 01h | Dec 23 21h | 2.3 d |

The same reading the other way: the Sun's longitude (J2000) at 00:00 UTC on 13 August.

| 1900 | 1950 | 2000 | 2024 | 2025 | 2026 | 2027 | 2028 | 2050 | 2100 |
|---|---|---|---|---|---|---|---|---|---|
| 141.15 | 140.38 | 140.55 | 140.41 | 140.17 | 139.92 | 139.67 | 140.38 | 139.77 | 139.00 |

## 6. The radiant table against the working list

Table 6 read at the listed solar longitude of maximum, against the radiant Table 5 prints for the maximum. Both are whole degrees; a difference under a degree is rounding.

| shower | Table 5 at maximum | Table 6 read at maximum | footing | apart |
|---|---|---|---|---|
| QUA | 230 +49 | 230.5 +49.2 | between rows | 0.4 |
| LYR | 271 +34 | 272.1 +34.0 | between rows | 0.9 |
| ETA | 338 -1 | 338.3 -0.3 | between rows | 0.7 |
| SDA | 340 -16 | 341.5 -15.4 | between rows | 1.6 |
| PER | 48 +58 | 49.0 +57.7 | between rows | 0.6 |
| ORI | 95 +16 | 95.6 +16.0 | between rows | 0.6 |
| LEO | 152 +22 | 152.0 +21.7 | between rows | 0.3 |
| GEM | 112 +33 | 112.8 +33.0 | between rows | 0.7 |
| URS | 217 +76 | 217.0 +74.7 | between rows | 1.3 |

The rows are the nine candidates. Across the whole working list of 38: 28 within 1 degree, 35 within 2 degrees; farther apart: ERI 2.1, KCG 4.6, SLY 7.5.

The calendar states the equinox of its solar longitudes (2000.0) and prints none for its radiants. Were the printed radiants of the equinox of date instead of J2000, each would lie this far from where the study plots it, at the 2027 maximum (the production precession and nutation):

| QUA | LYR | ETA | SDA | PER | ORI | LEO | GEM | URS |
|---|---|---|---|---|---|---|---|---|
| 0.17 | 0.21 | 0.38 | 0.39 | 0.31 | 0.39 | 0.39 | 0.39 | 0.13 |

## 7. Two evaluation rules against the printed rows and two independent sources

At every printed row of Table 6: how far the row lies from (a) the listed maximum carried at a fixed Sun-centred ecliptic position, and (b) the GMN table at the same solar longitude, with GMN's own measured dispersion beside it. Then, at the listed maximum: Table 5 against GMN and against the IAU list's solutions, each moved to the same solar longitude by its own daily drift where it states one (a solution with no drift is compared only within 1.5 degrees of solar longitude). Degrees of arc.

| shower | rows | fixed Sun-centred: mean / worst | GMN: rows met, mean / worst (dispersion) | at maximum: GMN | at maximum: IAU solutions, median / worst (n) |
|---|---|---|---|---|---|
| QUA | 4 | 0.5 / 0.7 | 1, 0.7 / 0.7 (4.0) | 0.7 | 0.8 / 1.2 (8) |
| GUM | 4 | 1.4 / 2.7 | 3, 1.4 / 2.4 (2.8) | 1.6 | 1.9 / 2.2 (5) |
| ACE | 5 | 1.1 / 1.6 | 1, 3.9 / 3.9 (3.3) | 3.9 | none |
| LYR | 4 | 1.7 / 3.0 | none | 1.2 | 1.3 / 1.9 (9) |
| PPU | 3 | 0.7 / 1.1 | none | no sample | 4.8 / 4.8 (2) |
| ETA | 8 | 0.9 / 1.7 | 7, 1.9 / 3.1 (1.9) | 0.2 | 0.4 / 2.4 (10) |
| ELY | 3 | 1.4 / 2.2 | 1, 0.6 / 0.6 (3.2) | 0.7 | 0.7 / 1.0 (6) |
| ARI | 2 | 2.0 / 2.1 | 2, 1.3 / 2.0 (2.5) | 0.6 | 0.6 / 2.9 (6) |
| JBO | 2 | 2.0 / 2.7 | none | 0.4 | 0.8 / 4.4 (4) |
| JPE | 4 | 1.2 / 1.8 | 4, 0.7 / 1.1 (2.4) | 0.6 | 0.6 / 1.8 (6) |
| GDR | 2 | 0.9 / 1.0 | 1, 1.4 / 1.4 (1.9) | 0.4 | 0.6 / 0.7 (6) |
| CAP | 8 | 2.8 / 3.9 | 5, 1.8 / 3.8 (1.9) | 1.2 | 1.3 / 5.3 (12) |
| SDA | 9 | 3.0 / 6.7 | 6, 1.5 / 2.3 (1.8) | 0.8 | 1.0 / 4.4 (9) |
| ERI | 4 | 1.8 / 2.2 | 3, 0.8 / 1.2 (2.7) | 2.0 | 1.9 / 2.3 (5) |
| PER | 9 | 2.2 / 5.7 | 7, 1.1 / 4.0 (2.3) | 0.2 | 0.3 / 1.0 (8) |
| KCG | 6 | 5.9 / 12.9 | 6, 1.9 / 2.9 (3.5) | 7.0 | 5.1 / 15.8 (11) |
| AUR | 4 | 1.5 / 2.7 | 3, 1.2 / 1.5 (1.7) | 0.3 | 0.5 / 5.2 (6) |
| SPE | 4 | 1.0 / 1.4 | 4, 0.9 / 1.4 (1.7) | 0.8 | 1.0 / 1.1 (4) |
| SLY | 5 | 6.3 / 6.9 | none | 0.2 | none |
| DSX | 3 | 2.2 / 3.2 | 2, 1.9 / 2.6 (3.1) | 0.4 | 0.6 / 1.2 (7) |
| OCT | 1 | 0.7 / 0.7 | 1, 0.1 / 0.1 (3.0) | 0.9 | 1.1 / 1.9 (4) |
| DRA | 1 | 0.1 / 0.1 | 1, 1.3 / 1.3 (3.3) | 0.4 | 0.3 / 1.4 (4) |
| EGE | 3 | 0.9 / 1.3 | 3, 2.3 / 2.6 (3.1) | 1.5 | 1.2 / 1.3 (4) |
| ORI | 7 | 3.4 / 7.3 | 7, 1.5 / 3.1 (1.8) | 0.3 | 0.4 / 0.9 (7) |
| LMI | 3 | 0.9 / 0.9 | 3, 1.1 / 1.7 (1.7) | 0.8 | 1.0 / 1.8 (5) |
| STA | 13 | 5.1 / 11.7 | 8, 1.5 / 2.0 (3.2) | 1.4 | 1.3 / 9.2 (11) |
| NTA | 9 | 2.4 / 5.0 | 4, 2.0 / 2.3 (3.4) | 1.4 | 1.8 / 7.3 (10) |
| LEO | 5 | 2.4 / 4.5 | 4, 1.8 / 2.0 (1.6) | 1.7 | 1.7 / 2.1 (10) |
| AMO | 3 | 1.2 / 1.8 | 2, 1.0 / 1.1 (3.0) | 0.2 | 0.2 / 0.2 (3) |
| NOO | 4 | 1.9 / 4.3 | 3, 0.9 / 1.2 (2.3) | 1.0 | 0.9 / 1.6 (7) |
| PHO | 3 | 2.3 / 4.4 | none | no sample | 0.9 / 0.9 (1) |
| AND | 0 | - | none | no sample | 13.7 / 21.9 (5) |
| PUP | 4 | 1.2 / 1.8 | none | no sample | none |
| MON | 5 | 1.4 / 2.7 | 3, 1.5 / 1.9 (1.8) | 0.4 | 0.6 / 1.6 (7) |
| HYD | 4 | 0.9 / 1.5 | 4, 0.5 / 0.7 (2.0) | 1.1 | 1.1 / 1.3 (5) |
| GEM | 4 | 1.2 / 2.5 | 3, 0.8 / 1.0 (1.7) | 1.5 | 1.6 / 1.9 (7) |
| URS | 2 | 1.2 / 1.4 | none | 0.7 | 0.9 / 2.0 (6) |
| COM | 13 | 1.3 / 2.8 | 8, 1.0 / 1.7 (1.6) | 0.4 | 0.3 / 17.9 (5) |

## 8. How much of each activity interval the printed rows cover

The activity interval as dated in the 2027 edition, in solar longitude, against the first and last printed row: how many days of activity lie before the first row and after the last, where a rule must carry the radiant on. Where GMN has a sample at the activity's end, the carried-on radiant is compared with it.

| shower | activity (2027) | days before first row | days after last row | carried on vs GMN at start / end |
|---|---|---|---|---|
| QUA | Dec 28 - Jan 12 | 1.8 | 3.1 | - / - |
| LYR | Apr 14 - Apr 30 | 1.0 | 1.0 | - / - |
| ETA | Apr 19 - May 28 | 1.0 | 3.9 | - / 3.4 |
| SDA | Jul 12 - Aug 23 | 0 | 3.9 | - / - |
| PER | Jul 17 - Aug 24 | 0 | 0 | - / - |
| ORI | Oct 02 - Nov 07 | 3.0 | 3.1 | - / - |
| LEO | Nov 06 - Nov 30 | 4.1 | 1.0 | - / - |
| GEM | Dec 04 - Dec 20 | 1.0 | 1.0 | - / - |
| URS | Dec 17 - Dec 26 | 3.1 | 2.1 | - / - |

The rows are the nine candidates. In the rest of the working list, the showers whose activity runs more than five days beyond their printed rows (days): ARI 21.4, CAP 5.8, SLY 9.9, NTA 11.3, NOO 7.2, PHO 10.3.

## 9. Which showers a strength rule selects

The 2027 working list counted by listed ZHR, and the showers at 10 or more in full, with what the IAU list says of each. `Var` is the calendar's word for a possibly periodic shower with no annual rate. The rest of the working list is counted, not reproduced: it is the IMO's to publish.

| listed ZHR | showers |
|---|---|
| 10 or more | 11 |
| 5 to 9 | 12 |
| under 5 | 11 |
| `Var` | 4 |

| shower | ZHR | activity | maximum | V-inf km/s | r | in the IAU established list | IAU name | IAU parent | #564 |
|---|---|---|---|---|---|---|---|---|---|
| GEM | 150 | Dec 04 - Dec 20 | Dec 14 | 35 | 2.6 | yes | Geminids | (3200) Phaethon | yes |
| PER | 110+ | Jul 17 - Aug 24 | Aug 13 | 59 | 2.2 | yes | Perseids | 109P/Swift-Tuttle | yes |
| QUA | 80+ | Dec 28 - Jan 12 | Jan 04 | 41 | 2.1 | yes | Quadrantids | 2003 EH1 | yes |
| ETA | 50 | Apr 19 - May 28 | May 06 | 66 | 2.4 | yes | eta-Aquariids | 1P/Halley | yes |
| ARI | 30 | May 14 - Jun 24 | Jun 08 | 38 | 2.8 | yes | Daytime Arietids | P/1999 J6 |  |
| SDA | 25 | Jul 12 - Aug 23 | Jul 31 | 41 | 2.5 | yes | Southern delta-Aquariids | 2008 Y12, Marsden group | yes |
| ORI | 20 | Oct 02 - Nov 07 | Oct 22 | 66 | 2.5 | yes | Orionids | 1P/Halley | yes |
| LYR | 18 | Apr 14 - Apr 30 | Apr 23 | 49 | 2.1 | yes | April Lyrids | C/1861 G1 (Thatcher) | yes |
| LEO | 15+ | Nov 06 - Nov 30 | Nov 18 | 71 | 2.5 | yes | Leonids | 55P/Temple-Tuttle | yes |
| PUP | 10 | Dec 01 - Dec 15 | Dec 07 | 44 | 2.9 | no | - | - |  |
| URS | 10 | Dec 17 - Dec 26 | Dec 23 | 33 | 2.8 | yes | Ursids | 8P/Tuttle | yes |

Most working-list showers active on one date of 2027: 9, first on 2027-12-04.

## 10. What the atlas can already say about a shower night

For #564's candidates at the instant of the listed 2027 maximum, from Oslo (59.91 N, 10.75 E): the radiant by the rule of section 6, its geometric altitude from the production local sky, and the Sun and Moon from the production service. After 2027-06-28 the pinned leap-second file has expired and altitudes carry the service's stated estimate.

| shower | chart time (UTC) | radiant J2000 | radiant altitude | Sun altitude | Moon lit | Moon altitude | Moon to radiant |
|---|---|---|---|---|---|---|---|
| QUA | 2027-01-04 03:23 | 230.5 +49.2 | +52.1 | -34.3 | 12% (waning crescent) | -15.9 | 76 |
| LYR | 2027-04-23 01:39 | 272.1 +34.0 | +59.2 | -12.1 | 95% (waning gibbous) | +4.0 | 69 |
| ETA | 2027-05-06 15:14 | 338.3 -0.3 | -16.8 | +29.1 | 0% (near new) | +33.4 | 67 |
| SDA | 2027-07-31 19:40 | 341.5 -15.4 | -11.6 | +0.1 | 4% (waning crescent) | -2.3 | 128 |
| PER | 2027-08-13 08:08 | 49.0 +57.7 | +67.2 | +32.8 | 86% (waxing gibbous) | -56.7 | 134 |
| ORI | 2027-10-22 00:36 | 95.6 +16.0 | +35.0 | -37.9 | 58% (waning gibbous) | +34.0 | 16 |
| LEO | 2027-11-18 05:55 | 152.0 +21.7 | +51.5 | -10.0 | 80% (waning gibbous) | +40.9 | 39 |
| GEM | 2027-12-14 19:57 | 112.8 +33.0 | +32.9 | -40.2 | 98% (near full) | +32.9 | 15 |
| URS | 2027-12-23 04:18 | 217.0 +74.7 | +66.8 | -27.3 | 21% (waning crescent) | +2.5 | 94 |

The night around each maximum, from Oslo, sampled every five minutes from 12:00 UTC before the maximum's date to 12:00 UTC after it: how long the Sun stands more than 18, 12 and 6 degrees below the horizon, and the radiant's highest altitude while the Sun is more than 12 degrees down. The atlas has no darkness search today; this is the production Sun sampled.

| shower | night of (UTC) | Sun below -18 | below -12 | below -6 | Sun's lowest | radiant's highest in the -12 dark |
|---|---|---|---|---|---|---|
| QUA | 2027-01-03 / 2027-01-04 | 12.4 h | 14.2 h | 16.0 h | -52.9 | +74.2 |
| LYR | 2027-04-22 / 2027-04-23 | none | 4.9 h | 7.1 h | -17.7 | +59.2 |
| ETA | 2027-05-06 / 2027-05-07 | none | 2.4 h | 5.7 h | -13.4 | -3.7 |
| SDA | 2027-07-31 / 2027-08-01 | none | none | 5.1 h | -12.0 | no such dark |
| PER | 2027-08-12 / 2027-08-13 | none | 3.7 h | 6.3 h | -15.3 | +60.1 |
| ORI | 2027-10-21 / 2027-10-22 | 9.7 h | 11.3 h | 12.9 h | -41.0 | +46.1 |
| LEO | 2027-11-17 / 2027-11-18 | 11.6 h | 13.2 h | 15.0 h | -49.2 | +51.7 |
| GEM | 2027-12-14 / 2027-12-15 | 12.5 h | 14.3 h | 16.2 h | -53.3 | +63.0 |
| URS | 2027-12-22 / 2027-12-23 | 12.6 h | 14.3 h | 16.3 h | -53.5 | +73.7 |

## 11. Why a direction alone is not membership

The IMO calendar (section 7, by Rainer Arlt, Table 1) gives the radiant diameter to assume when a plotted meteor is associated with a shower, by the meteor's distance from the radiant. For a meteor that belongs to no shower, travelling in a direction unrelated to the radiant, the chance that its path prolonged backwards crosses that circle follows from geometry alone: the circle of radius rho seen from distance D spans 2 asin(sin rho / sin D) of the 360 degrees of possible directions.

| distance from radiant | IMO optimum diameter | directions that line up | chance for an unrelated meteor |
|---|---|---|---|
| 15 | 14 | 56.2 of 360 | 15.6% |
| 30 | 17 | 34.4 of 360 | 9.6% |
| 50 | 20 | 26.2 of 360 | 7.3% |
| 70 | 23 | 24.5 of 360 | 6.8% |

The calendar adds that direction is not the only criterion: the meteor's angular speed must also match the shower's velocity (its Tables 2 and 3). An atlas that holds only a plotted direction holds neither the speed nor the path length.

## 12. A shower at a chart time in the version-1 bookmark document

The production reader of 6.5.0, given a bookmark whose subject is a shower's identity in the existing `body` kind, with the chart time and place:

```json
{
  "format": "juranometria-bookmarks",
  "version": 1,
  "writtenBy": "study",
  "root": [
    {
      "type": "bookmark",
      "id": "00000000-0000-4000-8000-000000000715",
      "title": "Perseids 2027 from Oslo",
      "view": {
        "ra": 48.97,
        "dec": 57.7,
        "field": 60
      },
      "subjects": [
        {
          "kind": "body",
          "id": "meteor-shower.PER"
        }
      ],
      "primary": {
        "kind": "body",
        "id": "meteor-shower.PER"
      },
      "time": {
        "instant": "2027-08-13T08:08:20Z",
        "purpose": "event"
      },
      "place": {
        "latitude": 59.91,
        "eastLongitude": 10.75
      }
    }
  ]
}
```

- read as: a usable bookmark
- written back byte for byte: yes

