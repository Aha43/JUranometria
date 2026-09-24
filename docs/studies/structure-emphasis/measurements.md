# Emphasis accents, measured (#361)

Distance is Euclidean in linear RGB scaled to 100 - a plain separation number, not a perceptual claim; 0 is identical ink. Contrast is WCAG relative-luminance ratio.

## WHITE_PAPER

| structure | accent | vs ground | canonical ink | vs canonical | protan | deutan | tritan | limiting |
|---|---|---|---|---|---|---|---|---|
| MERIDIAN | #a42e2e | 6.98:1 | #787878 | 25.7 | 22.4 | 19.6 | 25.7 | DEUTERANOPIA |
| ECLIPTIC | #82610f | 5.73:1 | #787878 | 24.4 | 24.3 | 25.6 | 10.1 | TRITANOPIA |
| EQUATORIAL_GRID | #566e96 | 5.16:1 | #d8d8d8 | 40.8 | 37.7 | 38.9 | 44.8 | PROTANOPIA |
| HORIZON | #4a6c52 | 5.90:1 | #787878 | 13.8 | 9.9 | 10.5 | 10.5 | PROTANOPIA |
| CONSTELLATION_BOUNDARIES | #6d4c8d | 6.84:1 | #bebebe | 33.6 | 36.6 | 34.8 | 38.3 | DEUTERANOPIA |
| CONSTELLATION_FIGURES | #944f30 | 6.14:1 | #787878 | 19.8 | 18.7 | 17.9 | 15.6 | TRITANOPIA |

Monochrome separation (luminance-only distance accent to canonical ink):
- MERIDIAN: 12.2 (plus the stroke gain of 0.6 px)
- ECLIPTIC: 7.1 (plus the stroke gain of 0.6 px)
- EQUATORIAL_GRID: 42.0 (plus the stroke gain of 0.6 px)
- HORIZON: 7.8 (plus the stroke gain of 0.6 px)
- CONSTELLATION_BOUNDARIES: 38.8 (plus the stroke gain of 0.6 px)
- CONSTELLATION_FIGURES: 8.6 (plus the stroke gain of 0.6 px)

Accent pairs (distance raw, then under the pair's own limiting simulation):
- MERIDIAN / ECLIPTIC: 15.6, 7.3 under DEUTERANOPIA
- MERIDIAN / EQUATORIAL_GRID: 32.8, 25.4 under DEUTERANOPIA
- MERIDIAN / HORIZON: 26.1, 10.2 under DEUTERANOPIA
- MERIDIAN / CONSTELLATION_BOUNDARIES: 25.8, 20.2 under TRITANOPIA
- MERIDIAN / CONSTELLATION_FIGURES: 8.3, 2.1 under DEUTERANOPIA
- ECLIPTIC / EQUATORIAL_GRID: 32.3, 15.7 under TRITANOPIA
- ECLIPTIC / HORIZON: 19.9, 13.3 under TRITANOPIA
- ECLIPTIC / CONSTELLATION_BOUNDARIES: 29.3, 7.9 under TRITANOPIA
- ECLIPTIC / CONSTELLATION_FIGURES: 9.4, 5.8 under TRITANOPIA
- EQUATORIAL_GRID / HORIZON: 15.6, 3.9 under TRITANOPIA
- EQUATORIAL_GRID / CONSTELLATION_BOUNDARIES: 9.5, 5.8 under DEUTERANOPIA
- EQUATORIAL_GRID / CONSTELLATION_FIGURES: 27.9, 21.3 under TRITANOPIA
- HORIZON / CONSTELLATION_BOUNDARIES: 17.1, 6.8 under TRITANOPIA
- HORIZON / CONSTELLATION_FIGURES: 19.6, 8.9 under PROTANOPIA
- CONSTELLATION_BOUNDARIES / CONSTELLATION_FIGURES: 22.8, 12.3 under TRITANOPIA

## BLACK_SKY

| structure | accent | vs ground | canonical ink | vs canonical | protan | deutan | tritan | limiting |
|---|---|---|---|---|---|---|---|---|
| MERIDIAN | #e8907e | 8.72:1 | #737373 | 27.4 | 13.7 | 19.6 | 28.1 | PROTANOPIA |
| ECLIPTIC | #cfa84e | 9.36:1 | #737373 | 25.5 | 20.4 | 23.0 | 26.5 | PROTANOPIA |
| EQUATORIAL_GRID | #7a98c7 | 7.14:1 | #282828 | 47.8 | 50.1 | 49.0 | 41.6 | TRITANOPIA |
| HORIZON | #8bb092 | 8.72:1 | #737373 | 16.4 | 19.6 | 17.9 | 19.4 | DEUTERANOPIA |
| CONSTELLATION_BOUNDARIES | #a384c7 | 6.68:1 | #3a3a3a | 43.2 | 40.5 | 41.7 | 34.9 | TRITANOPIA |
| CONSTELLATION_FIGURES | #cc7447 | 6.16:1 | #737373 | 22.5 | 10.9 | 15.7 | 20.6 | PROTANOPIA |

Monochrome separation (luminance-only distance accent to canonical ink):
- MERIDIAN: 20.4 (plus the stroke gain of 0.6 px)
- ECLIPTIC: 22.7 (plus the stroke gain of 0.6 px)
- EQUATORIAL_GRID: 43.1 (plus the stroke gain of 0.6 px)
- HORIZON: 20.4 (plus the stroke gain of 0.6 px)
- CONSTELLATION_BOUNDARIES: 34.1 (plus the stroke gain of 0.6 px)
- CONSTELLATION_FIGURES: 9.4 (plus the stroke gain of 0.6 px)

Accent pairs (distance raw, then under the pair's own limiting simulation):
- MERIDIAN / ECLIPTIC: 13.4, 6.9 under TRITANOPIA
- MERIDIAN / EQUATORIAL_GRID: 29.9, 16.5 under PROTANOPIA
- MERIDIAN / HORIZON: 22.7, 6.3 under DEUTERANOPIA
- MERIDIAN / CONSTELLATION_BOUNDARIES: 22.9, 17.6 under PROTANOPIA
- MERIDIAN / CONSTELLATION_FIGURES: 15.3, 11.7 under TRITANOPIA
- ECLIPTIC / EQUATORIAL_GRID: 33.7, 23.5 under TRITANOPIA
- ECLIPTIC / HORIZON: 21.8, 15.4 under PROTANOPIA
- ECLIPTIC / CONSTELLATION_BOUNDARIES: 30.3, 14.6 under TRITANOPIA
- ECLIPTIC / CONSTELLATION_FIGURES: 11.9, 10.5 under DEUTERANOPIA
- EQUATORIAL_GRID / HORIZON: 13.7, 8.6 under TRITANOPIA
- EQUATORIAL_GRID / CONSTELLATION_BOUNDARIES: 10.3, 0.6 under DEUTERANOPIA
- EQUATORIAL_GRID / CONSTELLATION_FIGURES: 35.4, 26.8 under TRITANOPIA
- HORIZON / CONSTELLATION_BOUNDARIES: 16.5, 9.2 under TRITANOPIA
- HORIZON / CONSTELLATION_FIGURES: 26.3, 19.3 under DEUTERANOPIA
- CONSTELLATION_BOUNDARIES / CONSTELLATION_FIGURES: 30.6, 16.0 under TRITANOPIA

## Combinations (multiple emphasis)

The three crossing pairs the combination contracts hold, and all six at once; each is only as tellable as its least-separated pair of accents.

Finding, accepted by the owner: multiple emphasis does not promise that every active structure can be identified by hue alone. Under the common dichromacies some co-raised accents sit close together - most of all when all six are raised. Every accent stays separated from its own canonical ink (the tables above), and geometry, dash pattern, position and the menu's checked identities remain part of the reading system.

On WHITE_PAPER:
- equatorial_grid+constellation_figures: weakest pair EQUATORIAL_GRID / CONSTELLATION_FIGURES at 27.9 raw, 21.3 under TRITANOPIA
- ecliptic+equatorial_grid: weakest pair ECLIPTIC / EQUATORIAL_GRID at 32.3 raw, 15.7 under TRITANOPIA
- meridian+horizon: weakest pair MERIDIAN / HORIZON at 26.1 raw, 10.2 under DEUTERANOPIA
- meridian+ecliptic+equatorial_grid+horizon+constellation_boundaries+constellation_figures: weakest pair MERIDIAN / CONSTELLATION_FIGURES at 8.3 raw, 2.1 under DEUTERANOPIA

On BLACK_SKY:
- equatorial_grid+constellation_figures: weakest pair EQUATORIAL_GRID / CONSTELLATION_FIGURES at 35.4 raw, 26.8 under TRITANOPIA
- ecliptic+equatorial_grid: weakest pair ECLIPTIC / EQUATORIAL_GRID at 33.7 raw, 23.5 under TRITANOPIA
- meridian+horizon: weakest pair MERIDIAN / HORIZON at 22.7 raw, 6.3 under DEUTERANOPIA
- meridian+ecliptic+equatorial_grid+horizon+constellation_boundaries+constellation_figures: weakest pair EQUATORIAL_GRID / CONSTELLATION_BOUNDARIES at 10.3 raw, 0.6 under DEUTERANOPIA
