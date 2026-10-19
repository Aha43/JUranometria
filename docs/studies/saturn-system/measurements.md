# Saturn table and ring geometry, proposed (#574)

Every number below is read from the kept Horizons responses under `docs/studies/saturn-system/horizons/`
and the retained PDS ring table (`rings/pds-rings-table.tsv`); none is typed in the study.

Saturn's radii, as the Horizons response's physical data states them: 60268 km at the equator (1 bar), 54364 km polar.
The card's instant is the 2026 daily Oslo row nearest the Earth: 2026-10-05 00:00 UTC, 8.434258 AU.

## The ring geometry at the card's instant

- opening to the observer: Horizons' planetodetic ObsSub-LAT -9.102795° is -7.4277° planetocentric
- opening to the Sun: Horizons' SunSub-LAT -9.446212° is -7.7097° planetocentric
- north pole's position angle (Horizons' NP.ang, of date): 3.2326°; the rings' major axis lies at 93.2326°
- the globe: 19.705″ at the equator, 17.808″ pole to pole at this opening

| PDS feature | edge | radius km | major axis ″ | minor axis ″ |
|---|---|---:|---:|---:|
| C Ring | inner | 74,491 | 24.355 | 3.149 |
| B Ring | inner | 91,975 | 30.071 | 3.887 |
| B Ring | outer | 117,570 | 38.440 | 4.969 |
| Cassini Division | outer | 122,050 | 39.904 | 5.159 |
| A Ring | outer | 136,770 | 44.717 | 5.781 |

The near half of the rings - the half in front of the globe - lies on the side away from the projected north pole
when the observer is north of the ring plane (B > 0), and towards it when south: a ring point in the direction of
the observer projects at −sin B cos B along the projected pole.

Over 1900-2100 (10488 weekly Oslo rows) the widest planetocentric opening is -26.995° (2003-04-07), where Horizons' planetodetic
ObsSub-LAT reads -32.049°. In 248 rows the Earth and the Sun are on opposite sides of the ring plane: the unlit face is seen.

## The moon set, from the fixtures

Separations and Horizons' visibility codes from the weekly Oslo matrix 1900-2100; brightness from Horizons' geocentric
30-day magnitudes (airless, no extinction). Proposed: the bodies marked yes; the reasons for each are in the decision record.

| moon | proposed | separation ″ (min-max) | diameter ″ (max) | APmag (brightest-faintest) | rows not clear of Saturn |
|---|---|---|---:|---|---:|
| mimas | no | 0.3-32.2 | 0.0714 | 12.60-13.53 | 972 of 10488 |
| enceladus | no | 0.0-40.9 | 0.0881 | 11.40-12.33 | 553 of 10488 |
| tethys | yes | 0.6-50.4 | 0.1848 | 9.90-10.83 | 380 of 10488 |
| dione | yes | 0.5-64.6 | 0.1934 | 10.10-11.03 | 227 of 10488 |
| rhea | yes | 0.0-90.4 | 0.2627 | 9.40-10.33 | 121 of 10488 |
| titan | yes | 1.8-213.0 | 0.8849 | 8.02-8.95 | 28 of 10488 |
| hyperion | no | 2.3-283.2 | 0.0619 | 13.93-14.86 | 11 of 10488 |
| iapetus | yes | 7.9-625.5 | 0.2567 | 10.80-11.74 | 6 of 10488 |

## The cartography handoff

Over 1900-2100 the A Ring's outer edge spans 34.13″ (farthest, 11.0502 AU) to 46.97″ (nearest, 8.0304 AU) across;
the globe's equator 15.04″ to 20.70″. Pixels on the 900 × 700 page at its centre, each field's own projection,
the ladder's fields and then the closer fields:

| field | rings' major axis px (min-max) | globe px (min-max) |
|---:|---|---|
| 180° | 0.05-0.07 | 0.02-0.03 |
| 120° | 0.06-0.09 | 0.03-0.04 |
| 90° | 0.09-0.12 | 0.04-0.05 |
| 60° | 0.14-0.19 | 0.06-0.08 |
| 42° | 0.19-0.27 | 0.09-0.12 |
| 36° | 0.23-0.32 | 0.10-0.14 |
| 24° | 0.35-0.48 | 0.15-0.21 |
| 18° | 0.47-0.65 | 0.21-0.29 |
| 12° | 0.71-0.97 | 0.31-0.43 |
| 8° | 1.06-1.47 | 0.47-0.65 |
| 6° | 1.42-1.96 | 0.63-0.86 |
| 4° | 2.13-2.93 | 0.94-1.29 |
| 3° | 2.84-3.91 | 1.25-1.72 |
| 2° | 4.27-5.87 | 1.88-2.59 |
| 1° | 8.53-11.74 | 3.76-5.17 |
| 30′ | 17.07-23.48 | 7.52-10.35 |
| 15′ | 34.13-46.97 | 15.04-20.70 |

## The words, proposed

| key | English | Norsk bokmål |
|---|---|---|
| `saturn` | Saturn | Saturn |
| `ra` | Right ascension (J2000) | Rektascensjon (J2000) |
| `dec` | Declination (J2000) | Deklinasjon (J2000) |
| `altitude` | Altitude | Høyde |
| `azimuth` | Azimuth | Asimut |
| `distance` | Distance | Avstand |
| `diameter` | Apparent diameter, equator / poles | Tilsynelatende diameter, ekvator / poler |
| `illuminated` | Illuminated | Belyst |
| `pole` | North pole, position angle | Nordpol, posisjonsvinkel |
| `ringOpening` | Rings, opening to the observer | Ringene, åpning mot observatøren |
| `ringSun` | Rings, opening to the Sun | Ringene, åpning mot Solen |
| `ringExtent` | Rings, apparent size | Ringene, tilsynelatende størrelse |
| `faceNorth` | north face | nordsiden |
| `faceSouth` | south face | sørsiden |
| `unlitFace` | the unlit face is seen | den ubelyste siden sees |
| `edgeOn` | edge-on | på kant |
| `moonsHeading` | The five moons | De fem månene |
| `moon` | Moon | Måne |
| `side` | Side | Side |
| `eastwest` | East–west | Øst–vest |
| `northsouth` | North–south | Nord–sør |
| `separation` | Separation | Avstand fra Saturn |
| `radii` | Saturn radii | Saturnradier |
| `state` | State | Tilstand |
| `east` | east | øst |
| `west` | west | vest |
| `clear` | clear of Saturn | klar av Saturn |
| `inFront` | in front of Saturn | foran Saturn |
| `behind` | behind Saturn | bak Saturn |
| `shadow` | in Saturn's shadow | i Saturns skygge |
| `partlyShadow` | partly in Saturn's shadow | delvis i Saturns skygge |
| `behindShadow` | behind Saturn, in its shadow | bak Saturn, i skyggen |
| `mimas` | Mimas | Mimas |
| `enceladus` | Enceladus | Enceladus |
| `tethys` | Tethys | Tethys |
| `dione` | Dione | Dione |
| `rhea` | Rhea | Rhea |
| `titan` | Titan | Titan |
| `hyperion` | Hyperion | Hyperion |
| `iapetus` | Iapetus | Iapetus |
| `sun` | Sun | Solen |
| `mercury` | Mercury | Merkur |
| `venus` | Venus | Venus |
| `moonBody` | Moon | Månen |
| `jupiter` | Jupiter | Jupiter |
| `update` | Update from Place and Time | Oppdater fra Sted og tid |

## The images

- `controls-saturn-en-1-saturn-card.png`
- `controls-saturn-en-2-moons-table.png`
- `controls-saturn-en-3-controller-group.png`
- `controls-saturn-en-2-moons-table-dark.png`
- `controls-saturn-nb-NO-1-saturn-card.png`
- `controls-saturn-nb-NO-2-moons-table.png`
- `controls-saturn-nb-NO-3-controller-group.png`
- `controls-saturn-nb-NO-2-moons-table-dark.png`
- `controls-saturn-4-ring-geometry.png`
- `controls-saturn-5-ring-opening.png`
