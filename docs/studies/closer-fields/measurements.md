# The closer fields: foundations and the offer

Sprint 50, issues #543 and #544 (#481). The four foundations ruling 5 on #539 requires before any surface offers 0.5° or 0.25° (#543), proven on production pages: the production `ChartComponent` over the bundled catalogue, set to a state at the field; then the offer itself (#544, section 5): zoom in at the floor is the closer step where Mercury, Venus or Jupiter, as drawn, lies on the page that step would display, through the same controller and component the application wires. The instants are the cited rows in `docs/studies/mercury/mercury-events.txt` and `docs/studies/venus/venus-events.txt`; positions are the services' for Oslo at sea level. Where names and grid figures found room is in `platform.md`. Regenerate with `make closer-fields-study`.

## 1. The field's words on every surface

The ladder's forms are unchanged - one decimal in the title block and the spoken description, whole degrees in the toolbar, the export's description and its file name - and a closer field prints to its own precision in all of them (`FieldWidthText`). The Pleiades, V 8.0.

| field | title block (en) | title block (nb-NO) | spoken | toolbar | export description | file name | view report |
|---:|---|---|---|---|---|---|---|
| 1° | Field 1.0° | Felt 1.0° | Field 1.0 degrees wide, gnomonic projection. | Field 1° · Stars to V 8.0 | Field 1 degrees wide, gnomonic. | `juranometria-3h-47-0m-24-07-1deg.png` | `field-degrees: 1.0` |
| 0.5° | Field 0.5° | Felt 0.5° | 3h 47.0m, +24° 07′. Centre RA 56.7500, Dec +24.1200 (ICRS J2000). Closer field 0.5 degrees wide, gnomonic projection. Stars to V 8.0. Celestial north is up, and celestial east is left. | Field 0.5° · Stars to V 8.0 | 3h 47.0m, +24° 07′. Centre RA 56.7500, Dec +24.1200 (ICRS/J2000). Closer field 0.5 degrees wide, gnomonic. Stars to V 8.0. A4 landscape, 297.0 x 210.0 mm, 12.7 mm margins, chart 271.6 x 184.6 mm. Ground: white-paper. | `juranometria-3h-47-0m-24-07-0.5deg.png` | `field-degrees: 0.5` |
| 0.25° | Field 0.25° | Felt 0.25° | 3h 47.0m, +24° 07′. Centre RA 56.7500, Dec +24.1200 (ICRS J2000). Closer field 0.25 degrees wide, gnomonic projection. Stars to V 8.0. Celestial north is up, and celestial east is left. | Field 0.25° · Stars to V 8.0 | 3h 47.0m, +24° 07′. Centre RA 56.7500, Dec +24.1200 (ICRS/J2000). Closer field 0.25 degrees wide, gnomonic. Stars to V 8.0. A4 landscape, 297.0 x 210.0 mm, 12.7 mm margins, chart 271.6 x 184.6 mm. Ground: white-paper. | `juranometria-3h-47-0m-24-07-0.25deg.png` | `field-degrees: 0.25` |

## 2. The grid's finer steps

The ladder's finest steps (1m of time, 15′ of arc) are a page apart at a closer field. Below the floor the same spacing rule (at least 110 px between lines at the page centre) is offered 15s and 30s of time and 2′, 5′ and 10′ of arc first; a ladder page never is, so no released grid moves. Meridians and parallels counted on the page; their figures' placement is in `platform.md`.

| field | RA step | Dec step | meridians | parallels |
|---:|---|---|---:|---:|
| 1° | 1m | 15′ | 5 | 4 |
| 0.5° | 30s | 5′ | 5 | 4 |
| 0.25° | 15s | 2′ | 5 | 6 |

![](pleiades-1.png)

![](pleiades-0.5.png)

![](pleiades-0.25.png)

## 3. The catalogue at the page's own field

The #539 study drew its closer pages from the 1° page's stars. Production asks the catalogue for the closer page itself: its own corners plus the catalogue's object margin, about its own centre. Stars returned by that query, and those of them that land on the page.

| page | field | stars queried | stars on the page |
|---|---:|---:|---:|
| the Pleiades | 1° | 156 | 17 |
| the Pleiades | 0.5° | 149 | 6 |
| the Pleiades | 0.25° | 142 | 2 |

## 4. Mercury, Venus and Jupiter at the closer fields

### 4.1 Mercury at its 2026 greatest eastern elongation, 2026-02-19T18:00:00Z, Oslo

Centred on the body; rule M's mark is 6 px, and the true disc takes over where it is the larger.

| field | stars queried | stars on the page | drawn |
|---:|---:|---:|---|
| 1° | 93 | 1 | Mercury symbol 6.0 px |
| 0.5° | 85 | 0 | Mercury symbol 6.0 px |
| 0.25° | 78 | 0 | Mercury disc 7.2 px |

![](mercury-elongation-1.png)

![](mercury-elongation-0.5.png)

![](mercury-elongation-0.25.png)

### 4.2 Venus three weeks before its 2026 inferior conjunction, 2026-10-03T03:00:00Z, Oslo

Centred on the body; rule M's mark is 6 px, and the true disc takes over where it is the larger.

| field | stars queried | stars on the page | drawn |
|---:|---:|---:|---|
| 1° | 79 | 1 | Venus disc 12.4 px |
| 0.5° | 69 | 0 | Venus disc 24.8 px |
| 0.25° | 66 | 0 | Venus disc 49.6 px |

![](venus-crescent-1.png)

![](venus-crescent-0.5.png)

![](venus-crescent-0.25.png)

### 4.3 The 2019 transit of Mercury, greatest transit, 2019-11-11T15:20:00Z, Oslo

Centred on the body; rule M's mark is 6 px, and the true disc takes over where it is the larger. For #544: at 0.25° the page lies wholly inside the Sun's disc - its ring is off the page and its ground is the page's - so what reads is Mercury and the Sun's centre dot, capped by #541; nothing on the page itself says the reader is looking at the Sun.

| field | stars queried | stars on the page | drawn |
|---:|---:|---:|---|
| 1° | 96 | 1 | Sun disc 484.4 px; Mercury symbol 6.0 px, mercury.inFront |
| 0.5° | 83 | 0 | Sun disc 968.9 px; Mercury symbol 6.0 px, mercury.inFront |
| 0.25° | 77 | 0 | Sun disc 1937.8 px; Mercury disc 10.0 px, mercury.inFront |

![](transit-2019-1.png)

![](transit-2019-0.5.png)

![](transit-2019-0.25.png)

### 4.4 Jupiter at the 11 December 2026 triple transit, 2026-12-11T22:45:00Z, Oslo

Jupiter joined the closer fields at the owner's #544 checkpoint (Saturn waits for its own cartography study). Its oblate disc at true scale, the Galilean moons by the Jovian layer's own rules (a moon in Jupiter's shadow a hollow ring). The shadows the moons cast on Jupiter's disc are not drawn; they remain a future motivation for closer zoom, not part of #544.

| field | stars queried | stars on the page | drawn |
|---:|---:|---:|---|
| 1° | 80 | 0 | Jupiter disc 10.1 px; Ganymede symbol 3.0 px; Europa symbol 3.0 px; Io symbol 3.0 px; Callisto symbol 3.0 px |
| 0.5° | 71 | 0 | Jupiter disc 20.1 px; Ganymede symbol 3.0 px; Europa symbol 3.0 px; Io symbol 3.0 px; Callisto symbol 3.0 px |
| 0.25° | 68 | 0 | Jupiter disc 40.1 px; Ganymede symbol 3.0 px; Europa symbol 3.0 px; Io symbol 3.0 px; Callisto symbol 3.0 px |

![](jupiter-triple-1.png)

![](jupiter-triple-0.5.png)

![](jupiter-triple-0.25.png)

## 5. The closer step offered, and the way back

Mercury at its 2026 greatest eastern elongation, wired as the application wires the chart (#544): the controller asks the component whether Mercury, Venus or Jupiter, as the Solar System module contributes them, lies on the page a closer step would display. The toolbar, the View menu, the shortcuts and the wheel all make these controller calls; what the zoom-in control explains is the toolbar's own sentence. A pan of a degree then takes Mercury off the 0.25° page.

| step | field | toolbar | zoom in | zoom in explains | title note |
|---|---:|---|---|---|---|
| Mercury hidden | 1° | Field 1° · Stars to V 8.0 | unavailable | Unavailable: a closer field is offered only when Mercury, Venus or Jupiter, shown on the chart, is on the page the step would show | - |
| Mercury shown | 1° | Field 1° · Stars to V 8.0 | to 0.5° | The next step in is a closer field, offered because Mercury, Venus or Jupiter is on the page it would show | - |
| zoom in | 0.5° | Field 0.5° · Stars to V 8.0 | to 0.25° | The next step in is a closer field, offered because Mercury, Venus or Jupiter is on the page it would show | - |
| zoom in | 0.25° | Field 0.25° · Stars to V 8.0 | unavailable | Unavailable: this is the narrowest field the atlas draws | - |
| pan a degree east | 0.25° | Field 0.25° · Stars to V 8.0 | unavailable | Unavailable: this is the narrowest field the atlas draws | No Mercury, Venus or Jupiter on this page: no closer step from here |
| zoom out | 0.5° | Field 0.5° · Stars to V 8.0 | unavailable | Unavailable: a closer field is offered only when Mercury, Venus or Jupiter, shown on the chart, is on the page the step would show | No Mercury, Venus or Jupiter on this page: no closer step from here |
| zoom out | 1° | Field 1° · Stars to V 8.0 | unavailable | Unavailable: a closer field is offered only when Mercury, Venus or Jupiter, shown on the chart, is on the page the step would show | - |
| zoom out | 2° | Field 2° · Stars to V 8.0 | to 1° | Shows a narrower field, with fainter stars on it | - |

The page Mercury has left, at 0.25° - the field kept, the title block saying why there is no closer step:

![](mercury-left-0.25.png)

Nothing persists the view: a new controller - every start - opens at the released default, 8° on M31 (held), so a closer field is never saved.

### 5.1 The same steps for Jupiter, at the triple transit

| step | field | toolbar | zoom in | zoom in explains | title note |
|---|---:|---|---|---|---|
| Jupiter hidden | 1° | Field 1° · Stars to V 8.0 | unavailable | Unavailable: a closer field is offered only when Mercury, Venus or Jupiter, shown on the chart, is on the page the step would show | - |
| Jupiter shown | 1° | Field 1° · Stars to V 8.0 | to 0.5° | The next step in is a closer field, offered because Mercury, Venus or Jupiter is on the page it would show | - |
| zoom in | 0.5° | Field 0.5° · Stars to V 8.0 | to 0.25° | The next step in is a closer field, offered because Mercury, Venus or Jupiter is on the page it would show | - |
| zoom in | 0.25° | Field 0.25° · Stars to V 8.0 | unavailable | Unavailable: this is the narrowest field the atlas draws | - |
| pan a degree east | 0.25° | Field 0.25° · Stars to V 8.0 | unavailable | Unavailable: this is the narrowest field the atlas draws | No Mercury, Venus or Jupiter on this page: no closer step from here |
| zoom out | 0.5° | Field 0.5° · Stars to V 8.0 | unavailable | Unavailable: a closer field is offered only when Mercury, Venus or Jupiter, shown on the chart, is on the page the step would show | No Mercury, Venus or Jupiter on this page: no closer step from here |
| zoom out | 1° | Field 1° · Stars to V 8.0 | unavailable | Unavailable: a closer field is offered only when Mercury, Venus or Jupiter, shown on the chart, is on the page the step would show | - |

## 6. The exported sheet at 0.25°

Mercury's 2026 greatest eastern elongation at 0.25°, recorded by `ChartSheet` with the screen's own reference and bodies layers (`SheetInk`), A4, written at 150 dpi for the record. It says of itself:

> 23h 17.0m, −3° 20′. Centre RA 349.2584, Dec -3.3344 (ICRS/J2000). Closer field 0.25 degrees wide, gnomonic. Stars to V 8.0. A4 landscape, 297.0 x 210.0 mm, 12.7 mm margins, chart 271.6 x 184.6 mm. Ground: white-paper.

and would be offered as `juranometria-23h-17-0m-3-20-0.25deg.png`.

![](sheet-mercury-0.25-a4.png)

### 6.1 Jupiter's sheet at 0.25°, the triple transit

> 9h 56.7m, +13° 26′. Centre RA 149.1636, Dec +13.4293 (ICRS/J2000). Closer field 0.25 degrees wide, gnomonic. Stars to V 8.0. A4 landscape, 297.0 x 210.0 mm, 12.7 mm margins, chart 271.6 x 184.6 mm. Ground: white-paper.

![](sheet-jupiter-0.25-a4.png)

