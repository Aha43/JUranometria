# The whole sky at one chart time, as built (#687)

Sprint 59, issue #687, road #703; ruled on #686 on 2026-11-08 (`docs/decisions/all-sky-projection.md`, section 7). Every page here is the application's own: a production chart with the production modules attached, entered through the one Whole sky switch, drawn by the Hammer projection as a real kind. Geometry, counts that follow from geometry, and words are pinned here; what depends on this machine's fonts is in `platform.md`. Regenerate with `make whole-sky-study`.

## 1. The ruling, held

| ruling | as built | held by |
|---|---|---|
| R-1 admit | the whole sky is a chart the application draws | every page here |
| R-2 Hammer | `ChartProjection.HAMMER`, `HammerProjection` in `juranometria.project` | `HammerProjectionTest` (round trip, equal area, the outline) |
| R-3 ecliptic horizontal | the ecliptic is the middle line, drawn straight in one run | section 5 |
| R-4 observer-aligned | the central meridian is the zenith's ecliptic longitude; the zenith keeps its latitude | section 4 |
| R-5 the seam | a join rule (`Projection.mayJoin`), never a hidden sliver: every curve cut once, no chord across the page, no name stranded | sections 5 and 10, `platform.md` |
| R-6 curves | the sampled route (`GreatCirclePage`, `CurveRun.Polyline`) for the one projection without conics; every conic page byte-identical | `SampledCurveRouteTest`, the interface evidence gate |
| R-7 horizon and Subdue | the true horizon, line ink split by its drawn runs | section 6 |
| R-8 Open here | the ordinary chart at the field left, through the controller's recentre | section 9 |
| R-9 zoom, pan, Home | refused with their explanations; Home the ordinary Home; the meridian readout refused | `WholeSkyControllerTest`, `WholeSkyPageTest` |
| R-10 not persisted | the session holds the mode; nothing stores it | `WholeSkySession` |
| R-11 bookmarks B-2 | `"view": {"mode": "whole-sky"}`, time and place required, schema version 1 | section 8, `WholeSkyBookmarkTest` |
| R-12 words | the ruled sentence and the central longitude; no Centre or Field claim | section 7 |
| R-13 one toggle | Whole sky on the toolbar, the Controller and the Chart menu, over one `WholeSkySession` | `ChartControls`, `AppMenuBar` |
| R-14 On this page | refused in the mode, pointing to Open here | `WholeSkyPageTest` |
| R-15 grid notation | outline-aware, refusing independently: RA figures on the paper where a meridian meets the outline, outward along its normal; Dec figures inside the sky where a parallel crosses the central meridian; none on the outline, none across the seam | section 5, `WholeSkyGridTest` |
| architecture A | one `Projection` interface, widened by an outline, a join rule, a sampled curve route and a whole-sphere statement; the modules unchanged | `HammerProjectionTest` |

## 2. Cases

Oslo (59.91 N, 10.75 E), the three chart times the #686 study found by searching the modules' own contributions (`docs/studies/all-sky/measurements.md`, section 2), carried by citation.

| case | instant (UTC) | found as |
|---|---|---|
| evening | 2026-12-15T21:00:00Z | an Oslo evening with six of the eight bodies other than the Sun up in darkness |
| gathering | 2027-04-25T00:00:00Z | the seven planets within 138.7 degrees of ecliptic longitude |
| spread | 2029-01-03T00:00:00Z | no gap among the nine bodies wider than 64.2 degrees |

The reader's limit before entering is V 8.0; the whole sky takes the brighter of that and its own V 5.0. The ordinary field left is 60 degrees.

## 3. Accounting: nine of nine

Every enabled body on every page, with its place in the map's frame: ecliptic longitude from the central meridian (positive left), ecliptic latitude, and its altitude.

### evening: 9 bodies drawn

| body | from the central meridian | ecliptic latitude | altitude | drawn as |
|---|---|---|---|---|
| neptune | -63.7 | -1.4 | +18.9 | symbol |
| uranus | -2.5 | -0.1 | +50.0 | symbol |
| saturn | -57.4 | -2.5 | +22.7 | symbol |
| jovian.jupiter | +81.7 | +0.9 | +6.9 | symbol |
| mercury | -170.8 | -0.2 | -49.5 (below) | symbol |
| mars | +91.4 | +2.7 | +0.7 | symbol |
| sun | -161.4 | +0.0 | -46.7 (below) | disc |
| venus | +153.2 | +2.9 | -40.8 (below) | symbol |
| moon | -87.7 | +0.8 | +2.3 | disc |

### gathering: 9 bodies drawn

| body | from the central meridian | ecliptic latitude | altitude | drawn as |
|---|---|---|---|---|
| neptune | -170.5 | -1.4 | -22.4 (below) | symbol |
| uranus | -111.8 | -0.1 | -7.9 (below) | symbol |
| saturn | -155.9 | -2.2 | -21.6 (below) | symbol |
| jovian.jupiter | -38.5 | +1.0 | +17.6 | symbol |
| venus | -169.8 | -1.6 | -22.6 (below) | symbol |
| mercury | -145.5 | -0.9 | -18.4 (below) | symbol |
| mars | -31.8 | +2.3 | +20.3 | symbol |
| sun | -141.1 | -0.0 | -16.5 (below) | disc |
| moon | +87.1 | -4.9 | -3.6 (below) | disc |

### spread: 9 bodies drawn

| body | from the central meridian | ecliptic latitude | altitude | drawn as |
|---|---|---|---|---|
| neptune | -98.5 | -1.5 | -7.7 (below) | symbol |
| uranus | -33.7 | -0.0 | +41.2 | symbol |
| saturn | -70.3 | -2.5 | +13.9 | symbol |
| jovian.jupiter | +100.5 | +1.3 | -7.5 (below) | symbol |
| venus | +158.9 | +0.5 | -47.1 (below) | symbol |
| mars | +80.3 | +2.5 | +9.2 | symbol |
| sun | +178.2 | +0.0 | -52.3 (below) | disc |
| mercury | -162.7 | -0.6 | -49.7 (below) | symbol |
| moon | +28.2 | -3.2 | +41.5 | disc |

## 4. The central meridian (R-4)

Observer-aligned: the central meridian is the ecliptic longitude of the zenith, and the zenith lies on it at its own ecliptic latitude - not at the map's geometric centre.

| case | central longitude | zenith's ecliptic longitude | zenith's ecliptic latitude | zenith from the map centre |
|---|---|---|---|---|
| evening | 64.9659 | 64.9659 | +39.82 | 39.82 degrees |
| gathering | 175.3130 | 175.3130 | +68.63 | 68.63 degrees |
| spread | 104.3135 | 104.3135 | +37.67 | 37.67 degrees |

## 5. The seam and the sampled curves (R-5, R-6)

Each reference great circle as the curve seam draws it on the whole sky: the number of runs (one, when the seam cuts it once), whether any closed, how far a run's end lies off the outline (`x^2/a^2 + y^2/b^2 - 1`, refined to the seam), and the longest chord between samples in pixels. Figure pieces and grid pieces that would cross more than 40 per cent of the page are counted as jumps.

| page | curve | runs | closed | end off the outline | longest chord px |
|---|---|---|---|---|---|
| `evening-en-white` | ecliptic | 1 | no | 1.3e-15 | 1.39 |
| `evening-en-white` | celestial equator | 1 | no | 1.3e-15 | 1.44 |
| `evening-en-white` | horizon | 1 | no | 1.1e-15 | 1.68 |
| `evening-en-white` | meridian | 1 | no | 1.1e-16 | 3.17 |
| `evening-nb-black` | ecliptic | 1 | no | 1.3e-15 | 1.39 |
| `evening-nb-black` | celestial equator | 1 | no | 1.3e-15 | 1.44 |
| `evening-nb-black` | horizon | 1 | no | 1.1e-15 | 1.68 |
| `evening-nb-black` | meridian | 1 | no | 1.1e-16 | 3.17 |
| `evening-en-black` | ecliptic | 1 | no | 1.3e-15 | 1.39 |
| `evening-en-black` | celestial equator | 1 | no | 1.3e-15 | 1.44 |
| `evening-en-black` | horizon | 1 | no | 1.1e-15 | 1.68 |
| `evening-en-black` | meridian | 1 | no | 1.1e-16 | 3.17 |
| `evening-nb-white` | ecliptic | 1 | no | 1.3e-15 | 1.39 |
| `evening-nb-white` | celestial equator | 1 | no | 1.3e-15 | 1.44 |
| `evening-nb-white` | horizon | 1 | no | 1.1e-15 | 1.68 |
| `evening-nb-white` | meridian | 1 | no | 1.1e-16 | 3.17 |
| `evening-en-white-laptop` | ecliptic | 1 | no | 1.3e-15 | 1.00 |
| `evening-en-white-laptop` | celestial equator | 1 | no | 1.7e-15 | 1.04 |
| `evening-en-white-laptop` | horizon | 1 | no | 1.1e-15 | 1.21 |
| `evening-en-white-laptop` | meridian | 1 | no | 8.9e-16 | 2.28 |
| `evening-en-white-subdue-off` | ecliptic | 1 | no | 1.3e-15 | 1.39 |
| `evening-en-white-subdue-off` | celestial equator | 1 | no | 1.3e-15 | 1.44 |
| `evening-en-white-subdue-off` | horizon | 1 | no | 1.1e-15 | 1.68 |
| `evening-en-white-subdue-off` | meridian | 1 | no | 1.1e-16 | 3.17 |
| `gathering-en-white` | ecliptic | 1 | no | 1.3e-15 | 1.39 |
| `gathering-en-white` | celestial equator | 1 | no | 1.1e-15 | 1.39 |
| `gathering-en-white` | horizon | 1 | no | 1.1e-15 | 1.43 |
| `gathering-en-white` | meridian | 1 | no | 8.9e-16 | 2.49 |
| `spread-en-white` | ecliptic | 1 | no | 1.3e-15 | 1.39 |
| `spread-en-white` | celestial equator | 1 | no | 8.9e-16 | 1.44 |
| `spread-en-white` | horizon | 1 | no | 1.6e-15 | 1.71 |
| `spread-en-white` | meridian | 1 | no | 2.2e-16 | 3.42 |

| page | figure jumps | grid jumps | constellation names anchored | stranded |
|---|---|---|---|---|
| `evening-en-white` | 0 | 0 | 88 | none |
| `evening-nb-black` | 0 | 0 | 88 | none |
| `evening-en-black` | 0 | 0 | 88 | none |
| `evening-nb-white` | 0 | 0 | 88 | none |
| `evening-en-white-laptop` | 0 | 0 | 88 | none |
| `evening-en-white-subdue-off` | 0 | 0 | 88 | none |
| `gathering-en-white` | 0 | 0 | 88 | none |
| `spread-en-white` | 0 | 0 | 88 | none |

**The graticule and its notation (R-15).** The grid's own seam ends are refined to the outline by bisection on the join rule, so every piece end is either on the outline to 1e-9 or one of its curve's two sampling ends (a meridian's pole ends, a parallel's start and stop at 0h): the ends off the outline are exactly two per curve. The notation is outline-aware and refuses independently - an RA figure stands on the paper outward along the outline's normal where a meridian meets it, which is only ever at the seam; a Dec figure stands inside the sky where a parallel crosses the central meridian; a figure whose box would reach the outline, the paper's edge, furniture or another figure is not placed. No figure's box reaches the outline, so none straddles the seam. The counts of figures placed depend on this machine's fonts and are in `platform.md`; what is pinned here is the geometry.

| page | grid curves | piece ends on the outline | ends off the outline | figures reaching the outline |
|---|---|---|---|---|
| `evening-en-white` | 23 | 34 | 46 (two per curve) | 0 |
| `evening-nb-black` | 23 | 34 | 46 (two per curve) | 0 |
| `evening-en-black` | 23 | 34 | 46 (two per curve) | 0 |
| `evening-nb-white` | 23 | 34 | 46 (two per curve) | 0 |
| `evening-en-white-laptop` | 23 | 34 | 46 (two per curve) | 0 |
| `evening-en-white-subdue-off` | 23 | 34 | 46 (two per curve) | 0 |
| `gathering-en-white` | 23 | 30 | 46 (two per curve) | 0 |
| `spread-en-white` | 23 | 34 | 46 (two per curve) | 0 |

## 6. Subdue below horizon (R-7)

Line ink is split by the region below the drawn horizon; marks and labels are toned by their own position. The region is compared with the true side of the horizon at every eighth pixel inside the outline.

| page | sample pixels disagreeing |
|---|---|
| `evening-en-white` | 0.070% of the sky |
| `evening-nb-black` | 0.070% of the sky |
| `evening-en-black` | 0.070% of the sky |
| `evening-nb-white` | 0.070% of the sky |
| `evening-en-white-laptop` | 0.117% of the sky |
| `evening-en-white-subdue-off` | Subdue off: nothing is quieted |
| `gathering-en-white` | 0.040% of the sky |
| `spread-en-white` | 0.141% of the sky |

## 7. Words (R-12)

### `evening-en-white`

Title block:

- Whole sky · Hammer · ecliptic north up · ecliptic longitude increases left at centre
- Central meridian: ecliptic longitude 65.0°, the zenith's · Stars to V 5.0

Spoken: "Whole sky. Hammer projection, ecliptic north up, ecliptic longitude increases left at centre. Central meridian at ecliptic longitude 65.0 degrees, the longitude of the zenith. Stars to V 5.0. Shows the entire sky at the chart's time and place; outside the elliptical outline is paper, not sky. East on your horizon. North on your horizon. South on your horizon. West on your horizon. Neptune (a cartographic symbol, not Neptune's apparent diameter). Uranus (a cartographic symbol, not Uranus's apparent diameter). Saturn (a cartographic symbol, not Saturn's apparent diameter). Jupiter (a cartographic symbol, not Jupiter's apparent diameter). Mercury (below the horizon) (a cartographic symbol, not Mercury's apparent diameter). Mars (a cartographic symbol, not Mars's apparent diameter). Sun (below the horizon). Venus (below the horizon) (a cartographic symbol, not Venus's apparent diameter). Moon. Phobos, too close to tell apart at this field, not drawn. Deimos, too close to tell apart at this field, not drawn. Miranda, too close to tell apart at this field, not drawn. Ariel, too close to tell apart at this field, not drawn. Umbriel, too close to tell apart at this field, not drawn. Titania, too close to tell apart at this field, not drawn. Oberon, too close to tell apart at this field, not drawn."

Exported sheet description: "Whole sky. Hammer, ecliptic north up, ecliptic longitude increases left at centre. Central meridian at ecliptic longitude 65.0 degrees, the zenith's. Stars to V 5.0. A4 landscape, 297.0 x 210.0 mm, 12.7 mm margins, chart 271.6 x 184.6 mm. Ground: white-paper."

### `evening-nb-black`

Title block:

- Hele himmelen · Hammer · ekliptisk nord opp · ekliptisk lengde øker mot venstre i midten
- Sentralmeridian: ekliptisk lengde 65.0°, senits · Stjerner til V 5.0

Spoken: "Hele himmelen. Hammer-projeksjon, ekliptisk nord opp, ekliptisk lengde øker mot venstre i midten. Sentralmeridian ved ekliptisk lengde 65.0 grader, senits lengde. Stjerner til V 5.0. Viser hele himmelen ved kartets tid og sted; utenfor det elliptiske omrisset er det papir, ikke himmel. Øst på horisonten din. Nord på horisonten din. Sør på horisonten din. Vest på horisonten din. Neptun (et kartografisk symbol, ikke Neptuns tilsynelatende diameter). Uranus (et kartografisk symbol, ikke Uranus' tilsynelatende diameter). Saturn (et kartografisk symbol, ikke Saturns tilsynelatende diameter). Jupiter (et kartografisk symbol, ikke Jupiters tilsynelatende diameter). Merkur (under horisonten) (et kartografisk symbol, ikke Merkurs tilsynelatende diameter). Mars (et kartografisk symbol, ikke Mars' tilsynelatende diameter). Solen (under horisonten). Venus (under horisonten) (et kartografisk symbol, ikke Venus' tilsynelatende diameter). Månen. Phobos, for nær til å skjelnes i dette feltet, ikke tegnet. Deimos, for nær til å skjelnes i dette feltet, ikke tegnet. Miranda, for nær til å skjelnes i dette feltet, ikke tegnet. Ariel, for nær til å skjelnes i dette feltet, ikke tegnet. Umbriel, for nær til å skjelnes i dette feltet, ikke tegnet. Titania, for nær til å skjelnes i dette feltet, ikke tegnet. Oberon, for nær til å skjelnes i dette feltet, ikke tegnet."

Exported sheet description: "Hele himmelen. Hammer, ekliptisk nord opp, ekliptisk lengde øker mot venstre i midten. Sentralmeridian ved ekliptisk lengde 65.0 grader, senits. Stjerner til V 5.0. A4 i liggende format, 297.0 x 210.0 mm, marger på 12.7 mm, kart 271.6 x 184.6 mm. Bakgrunn: svart himmel."

The view report's whole-sky lines (`field-degrees: whole-sky`, `view`, `orientation`, `central-meridian`, `central-longitude-degrees`) are held by `WholeSkyPageTest`; the report is a text feature outside the rendering closure and is not drawn here.

## 8. A whole-sky bookmark (R-11, B-2)

Written in the version-1 schema with the one optional member, no centre and no field; its chart time and place are kept, because the page derives from them. An older atlas keeps the record exactly as needing attention (`an unknown field "mode"`); every ordinary bookmark is written as before.

```json
{
  "format": "juranometria-bookmarks",
  "version": 1,
  "writtenBy": "6.5.0-study",
  "root": [
    {
      "type": "bookmark",
      "id": "8a1f0f2e-0000-4000-8000-000000000687",
      "title": "The whole sky, an Oslo evening",
      "view": {
        "mode": "whole-sky"
      },
      "subjects": [
        {
          "kind": "body",
          "id": "saturn"
        }
      ],
      "time": {
        "instant": "2026-12-15T21:00:00Z",
        "purpose": "observation"
      },
      "place": {
        "latitude": 59.91,
        "eastLongitude": 10.75
      }
    }
  ]
}
```

Read back: a whole-sky bookmark, byte-identical when written again: true

## 9. The journey: Open here (R-8)

From the evening page, Saturn is selected a pixel off its mark (true) and Open here is taken. The controller leaves the whole sky for the ordinary chart at the field the reader left:

- field before entering: 60 degrees; field after Open here: 60 degrees; whole sky after: false
- centred 0.000000 degrees from Saturn's position, by the stereographic projection
- `journey-1-whole-sky.png` before, `journey-2-open-here.png` after

Leaving the whole sky without choosing restores the ordinary page exactly: true (the state after Open here was already ordinary, so there was nothing to restore; the session's return is held by `WholeSkyControllerTest`).

## 10. Pages

| page | case | extent | palette | language | Subdue | bodies |
|---|---|---|---|---|---|---|
| `evening-en-white.png` | evening | 2000x1000 | white-paper | en | on | 9 |
| `evening-nb-black.png` | evening | 2000x1000 | black-sky | nb-NO | on | 9 |
| `evening-en-black.png` | evening | 2000x1000 | black-sky | en | on | 9 |
| `evening-nb-white.png` | evening | 2000x1000 | white-paper | nb-NO | on | 9 |
| `evening-en-white-laptop.png` | evening | 1440x720 | white-paper | en | on | 9 |
| `evening-en-white-subdue-off.png` | evening | 2000x1000 | white-paper | en | off | 9 |
| `gathering-en-white.png` | gathering | 2000x1000 | white-paper | en | on | 9 |
| `spread-en-white.png` | spread | 2000x1000 | white-paper | en | on | 9 |
| `journey-1-whole-sky.png` | evening | 2000x1000 | white-paper | en | on | 9 |
| `journey-2-open-here.png` | evening | 2000x1000 | white-paper | en | on | the ordinary 60-degree chart about Saturn |

- `evening-en-white.png`: the evening case, English, white paper, external monitor
- `evening-nb-black.png`: the same, Norwegian, black sky
- `evening-en-black.png`: English, black sky
- `evening-nb-white.png`: Norwegian, white paper
- `evening-en-white-laptop.png`: the evening case at a laptop's extent
- `evening-en-white-subdue-off.png`: Subdue below horizon off
- `gathering-en-white.png`: the gathering case
- `spread-en-white.png`: the spread case

## 11. Cost

Draw times per page and the heap peak are printed to standard error by the run, and pinned nowhere: the #686 study measured 8 s and 0.95 GB for 20 candidate pages on the owner's machine, and the evidence heap limit is re-measured before any evidence-heavy road (`juranometria-heap-limit`).

