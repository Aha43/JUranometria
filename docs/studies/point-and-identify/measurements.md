# Point-and-identify study (issue #168)

Measured through ChartRenderer.drawnMarks on 9 pages at 900x700.

## What each page draws

| page | field | stars | symbols | why |
|---|---:|---:|---:|---|
| m31-08 | 8° | 444 | 77 | the released default page |
| orion-36 | 36° | 3473 | 22 | the widest field, a constellation at a glance |
| sagittarius-08 | 8° | 475 | 72 | the densest sky the pack carries |
| sagittarius-01 | 1° | 180 | 39 | the narrowest field, where marks are far apart |
| crux-18 | 18° | 1755 | 102 | far southern, with overlapping cluster symbols |
| polaris-18 | 18° | 919 | 113 | a polar page, where projection distorts most |
| wrap-18 | 18° | 644 | 406 | across RA 0 |
| virgo-08 | 8° | 224 | 800 | a galaxy cluster: many symbols, few stars |
| empty-08 | 8° | 265 | 70 | quiet sky, where clicks often hit nothing |

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
| m31-08 | 10.8% | 0.2% | 11.4% | 0.3% | 11.7% | 0.3% | 12.1% | 0.3% | 13.0% | 0.6% | 13.8% | 0.8% | 16.4% | 1.3% |
| orion-36 | 4.2% | 0.4% | 12.5% | 1.4% | 18.0% | 2.4% | 24.4% | 4.3% | 38.3% | 9.7% | 52.4% | 18.9% | 75.9% | 43.1% |
| sagittarius-08 | 2.3% | 0.7% | 3.0% | 0.9% | 3.3% | 1.0% | 3.9% | 1.1% | 5.2% | 1.3% | 6.8% | 1.6% | 10.8% | 2.3% |
| sagittarius-01 | 34.7% | 21.0% | 34.8% | 21.1% | 34.9% | 21.2% | 34.9% | 21.2% | 35.1% | 21.4% | 35.3% | 21.5% | 35.7% | 21.8% |
| crux-18 | 1.6% | 0.1% | 4.6% | 0.4% | 6.9% | 0.6% | 9.8% | 1.0% | 17.1% | 2.5% | 24.8% | 4.9% | 41.3% | 12.8% |
| polaris-18 | 0.6% | 0.0% | 2.1% | 0.1% | 3.3% | 0.1% | 4.7% | 0.3% | 8.1% | 0.6% | 12.3% | 1.1% | 22.7% | 3.3% |
| wrap-18 | 0.7% | 0.0% | 2.5% | 0.3% | 3.6% | 0.4% | 5.3% | 0.6% | 8.7% | 1.1% | 13.0% | 1.9% | 23.6% | 4.7% |
| virgo-08 | 1.7% | 0.0% | 4.5% | 0.2% | 6.3% | 0.4% | 8.4% | 0.6% | 13.6% | 1.4% | 19.5% | 2.7% | 32.6% | 7.7% |
| empty-08 | 0.2% | 0.0% | 0.5% | 0.0% | 0.8% | 0.0% | 1.1% | 0.0% | 1.9% | 0.0% | 2.8% | 0.1% | 5.3% | 0.3% |

## Aimed clicks: does the reader get the mark they pointed at?

Every drawn mark on every page is clicked at its centre and on rings of 1.5, 3.5 and 5.5 px in eight directions (25 clicks each). The ring radii deliberately match NO swept tolerance: an earlier version jittered by exactly ±3 px, which made 'listed@3 = 100%' true by construction rather than by measurement. 'first' counts the intended mark ranked first; 'listed' counts it present at all.

| page | first@0 | listed@0 | first@2 | listed@2 | first@3 | listed@3 | first@4 | listed@4 | first@6 | listed@6 | first@8 | listed@8 | first@12 | listed@12 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| m31-08 | 33.7% | 34.8% | 62.7% | 64.9% | 71.0% | 74.5% | 90.9% | 94.6% | 96.3% | 100.0% | 96.3% | 100.0% | 96.3% | 100.0% |
| orion-36 | 28.7% | 30.2% | 56.5% | 62.1% | 65.9% | 73.3% | 80.6% | 93.3% | 85.6% | 100.0% | 85.6% | 100.0% | 85.6% | 100.0% |
| sagittarius-08 | 37.9% | 39.1% | 63.7% | 66.6% | 73.6% | 77.1% | 88.7% | 93.5% | 95.0% | 100.0% | 95.0% | 100.0% | 95.0% | 100.0% |
| sagittarius-01 | 51.1% | 51.1% | 76.0% | 76.0% | 83.3% | 83.3% | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% | 100.0% |
| crux-18 | 29.7% | 31.2% | 58.6% | 62.7% | 69.6% | 75.0% | 85.3% | 93.8% | 90.7% | 100.0% | 90.7% | 100.0% | 90.7% | 100.0% |
| polaris-18 | 29.2% | 29.8% | 60.0% | 61.7% | 70.7% | 73.3% | 89.8% | 93.4% | 96.1% | 100.0% | 96.1% | 100.0% | 96.1% | 100.0% |
| wrap-18 | 31.0% | 32.8% | 59.4% | 64.9% | 73.2% | 82.0% | 86.5% | 96.5% | 89.9% | 100.0% | 89.9% | 100.0% | 89.9% | 100.0% |
| virgo-08 | 37.1% | 38.2% | 66.2% | 69.4% | 89.9% | 95.9% | 93.2% | 99.4% | 93.7% | 100.0% | 93.7% | 100.0% | 93.7% | 100.0% |
| empty-08 | 31.6% | 31.6% | 62.8% | 62.9% | 76.8% | 77.4% | 93.6% | 94.1% | 99.4% | 100.0% | 99.4% | 100.0% | 99.4% | 100.0% |

## How ambiguous is ambiguous

Aimed clicks only, at the tolerance under consideration. 'worst' is the most candidates any single click produced.

| page | tol | 1 candidate | 2 | 3 | 4+ | worst |
|---|---:|---:|---:|---:|---:|---:|
| m31-08 | 3 | 91.6% | 5.8% | 2.1% | 0.6% | 4 |
| m31-08 | 4 | 90.8% | 6.3% | 2.1% | 0.8% | 5 |
| m31-08 | 6 | 88.7% | 6.9% | 2.9% | 1.5% | 7 |
| orion-36 | 3 | 77.1% | 17.4% | 3.5% | 2.0% | 10 |
| orion-36 | 4 | 71.7% | 21.1% | 4.5% | 2.7% | 10 |
| orion-36 | 6 | 59.1% | 27.5% | 8.7% | 4.7% | 12 |
| sagittarius-08 | 3 | 90.9% | 6.6% | 1.8% | 0.7% | 4 |
| sagittarius-08 | 4 | 90.5% | 6.9% | 1.3% | 1.3% | 4 |
| sagittarius-08 | 6 | 88.5% | 8.2% | 1.8% | 1.5% | 4 |
| sagittarius-01 | 3 | 87.2% | 7.8% | 4.6% | 0.5% | 4 |
| sagittarius-01 | 4 | 87.2% | 7.3% | 5.0% | 0.5% | 4 |
| sagittarius-01 | 6 | 87.2% | 7.3% | 5.0% | 0.5% | 4 |
| crux-18 | 3 | 84.4% | 11.3% | 2.5% | 1.9% | 7 |
| crux-18 | 4 | 81.0% | 13.8% | 2.8% | 2.4% | 7 |
| crux-18 | 6 | 75.3% | 16.9% | 4.3% | 3.5% | 7 |
| polaris-18 | 3 | 91.9% | 8.1% | 0.0% | 0.0% | 2 |
| polaris-18 | 4 | 90.5% | 9.3% | 0.2% | 0.0% | 3 |
| polaris-18 | 6 | 86.0% | 12.8% | 1.1% | 0.1% | 4 |
| wrap-18 | 3 | 80.4% | 9.2% | 5.8% | 4.6% | 6 |
| wrap-18 | 4 | 78.4% | 10.8% | 5.9% | 5.0% | 7 |
| wrap-18 | 6 | 75.2% | 11.0% | 7.0% | 6.8% | 7 |
| virgo-08 | 3 | 83.1% | 8.3% | 5.0% | 3.6% | 7 |
| virgo-08 | 4 | 81.1% | 9.6% | 5.3% | 4.1% | 7 |
| virgo-08 | 6 | 76.9% | 12.4% | 5.8% | 5.0% | 9 |
| empty-08 | 3 | 94.0% | 6.0% | 0.0% | 0.0% | 2 |
| empty-08 | 4 | 93.1% | 6.9% | 0.0% | 0.0% | 2 |
| empty-08 | 6 | 93.1% | 6.9% | 0.0% | 0.0% | 2 |

