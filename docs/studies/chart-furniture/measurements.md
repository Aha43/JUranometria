# Chart furniture study (issue #179)

## What the key shows, at every supported limit

Radii come from StarSizePolicy.DEFAULT - the same mapping the star pass uses.

| limit | samples | circle diameters | smallest difference |
|---:|---|---|---:|
| V 4 | V 0, V 2, V 4 | 10.00, 8.81, 7.13 px | 1.19 px |
| V 5 | V 0, V 3, V 5 | 10.00, 8.00, 6.21 px | 1.79 px |
| V 6 | V 0, V 3, V 6 | 10.00, 8.00, 5.20 px | 2.00 px |
| V 7 | V 0, V 4, V 7 | 10.00, 7.13, 4.06 px | 2.87 px |
| V 8 | V 0, V 4, V 8 | 10.00, 7.13, 2.63 px | 2.87 px |

For contrast, a key stepping by ONE magnitude would place circles this close together:

| pair | diameter difference |
|---|---:|
| V 0 to V 1 | 0.40 px |
| V 1 to V 2 | 0.78 px |
| V 2 to V 3 | 0.82 px |
| V 3 to V 4 | 0.86 px |
| V 4 to V 5 | 0.92 px |
| V 5 to V 6 | 1.01 px |
| V 6 to V 7 | 1.14 px |
| V 7 to V 8 | 1.43 px |

## What the furniture costs the page

Ink is any pixel darker than the paper's threshold inside the key's box, on the page as it draws WITHOUT the key.

| page | key box | share of page | chart ink it would cover | of which star or symbol ink |
|---|---|---:|---:|---:|
| m31-08 | 160x72 px | 1.83% | 288 px | 0 px |
| sagittarius-08 | 160x72 px | 1.83% | 400 px | 0 px |
| orion-36 | 160x72 px | 1.83% | 635 px | 290 px |
| polaris-18 | 160x72 px | 1.83% | 445 px | 15 px |
| crux-18 | 160x72 px | 1.83% | 487 px | 112 px |
| quiet-08 | 160x72 px | 1.83% | 405 px | 0 px |
| m31-08-mag4 | 160x72 px | 1.83% | 288 px | 0 px |
| m31-08-mag6 | 160x72 px | 1.83% | 288 px | 0 px |

Study pages written to docs/studies/chart-furniture
