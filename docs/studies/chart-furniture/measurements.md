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

Two measurements, and the difference matters. **Chart ink** is any pixel darker than the paper inside the key's box, on the page as it draws WITHOUT the key. **Star and symbol ink** is that same box measured on a page rendered with only those two layers switched on - derived from the layers themselves, not guessed from how dark a pixel is, which would count labels, figures and constellation names as stars (Sprint 20 review).

What survives here is what does not depend on a font: that the key is
drawn, that it is the same box on every page rather than one that grows with
the page's contents, and that it stays a corner. **What it costs in covered
ink is a measurement and nothing else** - it is the number this study exists
to report, it differs on every page and on every machine, and there is no portable
claim underneath it. It is recorded in `platform.md` beside this, with the machine
that measured it.


| page | key | one box for every page | under 5.0% of the page |
|---|---|---|---|
| m31-08 | drawn | yes | yes |
| sagittarius-08 | drawn | yes | yes |
| orion-36 | drawn | yes | yes |
| polaris-18 | drawn | yes | yes |
| crux-18 | drawn | yes | yes |
| quiet-08 | drawn | yes | yes |
| m31-08-mag4 | drawn | yes | yes |
| m31-08-mag6 | drawn | yes | yes |

Study pages written to docs/studies/chart-furniture
