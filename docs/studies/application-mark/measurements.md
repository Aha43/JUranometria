# The application mark: four candidates

Measured by `make application-mark-study`. Every image is drawn at its own pixel size from the one geometry in `ApplicationMark`, never resampled from a larger one, so what is inspected at 16 px is what a dock would actually draw.

## The four candidates

| mark | what it is | galaxies | stars |
|---|---|---:|---:|
| **Rift** | A galaxy crossing the corner and leaving the frame, three stars in the sky above it. | 1 | 3 |
| **Companion** | The galaxy and one companion, as the default page draws them, with two stars. | 2 | 2 |
| **Field** | A complete galaxy centred among four stars - the generic arrangement, included as the control. | 1 | 4 |
| **Crown** | A broad galaxy entering from the top edge, three stars beneath it. | 1 | 3 |

### Silhouette and clearance

**Occupied area** is the share of the card the mark's ink covers - too little and the icon is a white square, too much and it is a blot. **Edge clearance** is the smallest gap between ink and the card's border, in pixels at 1024, and a cropped ellipse deliberately has none: it is meant to leave the frame.

| mark | occupied area | edge clearance | ellipse leaves the frame |
|---|---:|---:|---|
| Rift | 36.5% | none, by design | **yes** |
| Companion | 35.6% | none, by design | **yes** |
| Field | 21.3% | 154 px | no |
| Crown | 42.8% | none, by design | **yes** |

## At the sizes that decide it

**Surviving stars** is measured, not counted: each dot is left out of a second rendering at the same size, and a dot survives when leaving it out changes the image. A dot that changes nothing is a dot the reader does not have.

**Ink islands** counts the separate pieces of ink a reader can see. A mark whose ellipse stays continuous reads as one shape plus its stars; one that breaks up reads as more, and looks like scattered dirt at small sizes.

| mark | 16 px stars | 24 px stars | 32 px stars | 48 px stars | 16 px islands | 24 px islands | 32 px islands | 48 px islands |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Rift | 3 of 3 | 3 of 3 | 3 of 3 | 3 of 3 | 2 | 2 | 2 | 3 |
| Companion | 2 of 2 | 2 of 2 | 2 of 2 | 2 of 2 | 2 | 2 | 3 | 3 |
| Field | 4 of 4 | 4 of 4 | 4 of 4 | 4 of 4 | 3 | 3 | 4 | 4 |
| Crown | 3 of 3 | 3 of 3 | 3 of 3 | 3 of 3 | 5 | 5 | 5 | 5 |

## Told apart from what it replaces

At 16 px, against the mark the atlas ships today (the Tabler north-star in a rounded square) and against a bare card - the shape a reader sees when an application has no identity at all. The figure is the share of the card's pixels that differ.

| mark | unlike a bare card | unlike today's north star |
|---|---:|---:|
| Rift | 77.0% | 77.4% |
| Companion | 76.6% | 77.0% |
| Field | 68.7% | 69.0% |
| Crown | 75.4% | 80.6% |

## What the images are

- `contact-sheet.png` - every candidate at every size, each drawn at its own dimensions.
- `grounds-light.png`, `grounds-dark.png` - the candidates at 32 and 64 px on light and dark desktop grounds, which is where a reader meets them. The mark itself stays white paper in both: the atlas does not follow the desktop's theme.
- `<candidate>-<size>.png` - the native exports, for inspection at their own size.

A recommendation is written in [the decision](../../decisions/application-mark.md). **The choice is the owner's**; this issue ends there, and #202 carries the chosen geometry into the platforms.
