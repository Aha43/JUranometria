# Point-and-identify study (issue #168)

Measured through ChartRenderer.drawnMarks on 9 pages at 900x700.

## What each page draws

| page | field | stars | symbols | why |
|---|---:|---:|---:|---|
| m31-08 | 8° | 48 | 7 | the released default page |
| orion-36 | 36° | 1567 | 12 | the widest field, a constellation at a glance |
| sagittarius-08 | 8° | 82 | 21 | the densest sky the pack carries |
| sagittarius-01 | 1° | 7 | 3 | the narrowest field, where marks are far apart |
| crux-18 | 18° | 576 | 43 | far southern, with overlapping cluster symbols |
| polaris-18 | 18° | 260 | 16 | a polar page, where projection distorts most |
| wrap-18 | 18° | 195 | 111 | across RA 0 |
| virgo-08 | 8° | 26 | 348 | a galaxy cluster: many symbols, few stars |
| empty-08 | 8° | 45 | 10 | quiet sky, where clicks often hit nothing |

## What the pack knows, and what survives loading

Of **13,371** deep-sky rows in the bundled pack:

| fact | rows where the source records nothing | share |
|---|---:|---:|
| major axis | 1,300 | 9.7% |
| minor axis | 2,279 | 17.0% |
| position angle | 2,596 | 19.4% |
| V magnitude | 9,103 | 68.1% |
| ...of which a B magnitude exists | 7,276 | 54.4% |
| ...no photometry at all | 1,827 | 13.7% |

The runtime `DeepSkyObject` keeps none of these distinctions: the loader substitutes a nominal extent for an absent size, `minor = major` for an absent minor axis, `0.0` for an absent position angle, and stores V-or-B in one unlabelled `magnitude` field. An inspector built on today's model would state a size no one measured, a position angle of exactly zero, and a B magnitude labelled V.

## The ink a reader aims at

Star dot radii in page pixels, by magnitude (StarSizePolicy.DEFAULT):

| V | radius px | diameter px |
|---:|---:|---:|
| 0.0 | 5.00 | 10.00 |
| 2.0 | 4.41 | 8.81 |
| 4.0 | 3.57 | 7.13 |
| 6.0 | 2.60 | 5.20 |
| 8.0 | 1.32 | 2.63 |

A V 8 star is the smallest thing on the page, and it is what a reader most often wants to name.

## Unaimed clicks: what a grid over the page hits

| page | hit@0 | amb@0 | hit@2 | amb@2 | hit@3 | amb@3 | hit@4 | amb@4 | hit@6 | amb@6 | hit@8 | amb@8 | hit@12 | amb@12 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| m31-08 | 14.5% | 0.2% | 15.0% | 0.3% | 15.4% | 0.4% | 15.8% | 0.4% | 16.8% | 0.6% | 17.7% | 0.9% | 20.4% | 1.5% |
| orion-36 | 4.2% | 0.4% | 12.5% | 1.4% | 18.0% | 2.4% | 24.4% | 4.3% | 38.3% | 9.7% | 52.4% | 18.9% | 75.8% | 43.0% |
| sagittarius-08 | 2.3% | 0.7% | 3.0% | 0.9% | 3.3% | 1.0% | 3.9% | 1.1% | 5.2% | 1.3% | 6.8% | 1.6% | 10.8% | 2.3% |
| sagittarius-01 | 34.7% | 21.0% | 34.8% | 21.1% | 34.9% | 21.2% | 34.9% | 21.2% | 35.1% | 21.4% | 35.3% | 21.5% | 35.7% | 21.8% |
| crux-18 | 1.6% | 0.1% | 4.6% | 0.4% | 6.9% | 0.6% | 9.8% | 1.0% | 17.1% | 2.5% | 24.8% | 4.9% | 41.3% | 12.8% |
| polaris-18 | 0.6% | 0.0% | 2.1% | 0.1% | 3.3% | 0.1% | 4.7% | 0.3% | 8.2% | 0.6% | 12.3% | 1.1% | 22.7% | 3.3% |
| wrap-18 | 0.7% | 0.1% | 2.6% | 0.3% | 3.7% | 0.4% | 5.4% | 0.6% | 8.8% | 1.1% | 13.1% | 1.9% | 23.7% | 4.8% |
| virgo-08 | 1.9% | 0.0% | 4.8% | 0.2% | 6.6% | 0.4% | 8.8% | 0.6% | 14.1% | 1.5% | 19.9% | 2.8% | 33.0% | 8.0% |
| empty-08 | 0.2% | 0.0% | 0.6% | 0.0% | 0.9% | 0.0% | 1.1% | 0.0% | 1.9% | 0.0% | 2.8% | 0.1% | 5.4% | 0.3% |

## Aimed clicks: does the reader get the mark they pointed at?

Every drawn mark on every page is clicked at its centre and on rings of 1.5, 3.5 and 5.5 px in eight directions (25 clicks each). The ring radii deliberately match NO swept tolerance: an earlier version jittered by exactly ±3 px, which made 'listed@3 = 100%' true by construction rather than by measurement. 'first' counts the intended mark ranked first; 'listed' counts it present at all.

| page | first@0 | listed@0 | first@2 | listed@2 | first@3 | listed@3 | first@4 | listed@4 | first@6 | listed@6 | first@8 | listed@8 | first@12 | listed@12 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| m31-08 | 33.5% | 34.9% | 61.4% | 65.2% | 69.6% | 74.5% | 88.3% | 94.7% | 93.5% | 100.0% | 93.5% | 100.0% | 93.5% | 100.0% |
| orion-36 | 28.7% | 30.3% | 56.3% | 62.2% | 65.5% | 73.4% | 80.1% | 93.4% | 85.1% | 100.0% | 85.1% | 100.0% | 85.1% | 100.0% |
| sagittarius-08 | 38.0% | 39.1% | 60.7% | 66.6% | 69.6% | 77.1% | 82.8% | 93.5% | 89.1% | 100.0% | 89.1% | 100.0% | 89.1% | 100.0% |
| sagittarius-01 | 51.1% | 51.1% | 54.8% | 76.0% | 54.8% | 83.3% | 58.4% | 100.0% | 58.4% | 100.0% | 58.4% | 100.0% | 58.4% | 100.0% |
| crux-18 | 29.6% | 31.2% | 58.6% | 62.7% | 69.5% | 75.1% | 85.3% | 93.8% | 90.6% | 100.0% | 90.6% | 100.0% | 90.6% | 100.0% |
| polaris-18 | 29.4% | 30.0% | 60.1% | 61.8% | 70.9% | 73.5% | 89.8% | 93.4% | 96.0% | 100.0% | 96.0% | 100.0% | 96.0% | 100.0% |
| wrap-18 | 31.0% | 32.9% | 59.3% | 65.0% | 73.4% | 82.6% | 86.4% | 96.6% | 89.7% | 100.0% | 89.7% | 100.0% | 89.7% | 100.0% |
| virgo-08 | 37.3% | 38.5% | 66.5% | 69.7% | 91.9% | 98.1% | 93.2% | 99.4% | 93.7% | 100.0% | 93.7% | 100.0% | 93.7% | 100.0% |
| empty-08 | 31.6% | 31.6% | 62.8% | 62.9% | 76.8% | 77.4% | 93.6% | 94.1% | 99.4% | 100.0% | 99.4% | 100.0% | 99.4% | 100.0% |

## How ambiguous is ambiguous

Aimed clicks only, at the tolerance under consideration. 'worst' is the most candidates any single click produced.

| page | tol | 1 candidate | 2 | 3 | 4+ | worst |
|---|---:|---:|---:|---:|---:|---:|
| m31-08 | 3 | 76.4% | 18.2% | 5.5% | 0.0% | 3 |
| m31-08 | 4 | 76.4% | 18.2% | 5.5% | 0.0% | 3 |
| m31-08 | 6 | 76.4% | 14.5% | 9.1% | 0.0% | 3 |
| orion-36 | 3 | 73.8% | 17.9% | 4.5% | 3.9% | 10 |
| orion-36 | 4 | 68.7% | 21.0% | 5.4% | 4.9% | 10 |
| orion-36 | 6 | 55.2% | 27.2% | 9.4% | 8.2% | 12 |
| sagittarius-08 | 3 | 78.6% | 9.7% | 7.8% | 3.9% | 4 |
| sagittarius-08 | 4 | 77.7% | 10.7% | 4.9% | 6.8% | 4 |
| sagittarius-08 | 6 | 77.7% | 9.7% | 4.9% | 7.8% | 4 |
| sagittarius-01 | 3 | 10.0% | 20.0% | 60.0% | 10.0% | 4 |
| sagittarius-01 | 4 | 10.0% | 20.0% | 60.0% | 10.0% | 4 |
| sagittarius-01 | 6 | 10.0% | 20.0% | 60.0% | 10.0% | 4 |
| crux-18 | 3 | 82.1% | 13.4% | 2.3% | 2.3% | 7 |
| crux-18 | 4 | 78.4% | 16.0% | 3.1% | 2.6% | 7 |
| crux-18 | 6 | 72.7% | 18.1% | 5.2% | 4.0% | 7 |
| polaris-18 | 3 | 89.9% | 10.1% | 0.0% | 0.0% | 2 |
| polaris-18 | 4 | 89.1% | 10.9% | 0.0% | 0.0% | 2 |
| polaris-18 | 6 | 85.5% | 14.1% | 0.4% | 0.0% | 3 |
| wrap-18 | 3 | 78.1% | 10.5% | 7.5% | 3.9% | 6 |
| wrap-18 | 4 | 76.1% | 11.8% | 7.8% | 4.2% | 7 |
| wrap-18 | 6 | 72.9% | 12.7% | 9.2% | 5.2% | 7 |
| virgo-08 | 3 | 84.8% | 9.9% | 4.3% | 1.1% | 4 |
| virgo-08 | 4 | 82.1% | 12.3% | 4.5% | 1.1% | 4 |
| virgo-08 | 6 | 78.3% | 15.2% | 5.3% | 1.1% | 4 |
| empty-08 | 3 | 100.0% | 0.0% | 0.0% | 0.0% | 1 |
| empty-08 | 4 | 96.4% | 3.6% | 0.0% | 0.0% | 2 |
| empty-08 | 6 | 96.4% | 3.6% | 0.0% | 0.0% | 2 |

