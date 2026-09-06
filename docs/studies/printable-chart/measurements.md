# How far past 36 degrees the chart can honestly go

Measured by `make printable-chart-study`. Reproduced byte-for-byte by the evidence contracts. Pure geometry: no fonts, no rendering, no clock, no catalogue in these numbers.

## What was asked

> Jeg laget egne stjernekart for print til Fanafjellet sist. Da kodet eg noe i python og det var mye knot, dette med en eksport til SVG ville vært veldig nyttig.

*("I made my own star charts for printing for Fanafjellet last time. I coded something in Python and it was a lot of hassle; an export to SVG would have been very useful.")*

And, separately: a little past 36°, not dramatically — accepting more projection error for a practical bright-star overview.

**"A little" is the whole question.** The atlas stops at 36° because that is where its gnomonic projection stops being quiet, not because 36 is a round number. So the gate measures what each further degree costs, and where the cost stops being something a reader would accept without being told.

## How much sky a page holds

Field width is the *horizontal* extent. On a 900×700 page the corner is further out than the edge, and that corner is where every distortion below is worst — so it is the corner, not the field width, that the rest of this measures.

| field | half-width | corner, gnomonic | corner, stereographic |
|---:|---:|---:|---:|
| 36° | 18.0° | **22.4°** | 22.7° |
| 40° | 20.0° | **24.8°** | 25.2° |
| 42° | 21.0° | **25.9°** | 26.4° |
| 45° | 22.5° | **27.7°** | 28.3° |
| 48° | 24.0° | **29.4°** | 30.1° |

The two are not the same page. Holding the left and right edges to the same sky, the stereographic page reaches slightly *further* into the corners, because its scale grows more slowly.

## What the corner costs

Two different costs, and conflating them is how a page that looks fine hides a false claim:

- **scale growth** — a degree of sky near the corner occupies more paper than a degree at the centre, so distances read wrong;
- **anisotropy** — the radial and tangential scales differ, so *shapes* are stretched: a round cluster becomes an ellipse pointing at the page centre.

Measured numerically, by projecting small offsets and comparing pixel distances — the implementation is being measured here, not only the formula.

| field | gnomonic: scale at corner | anisotropy | stereographic: scale | anisotropy |
|---:|---:|---:|---:|---:|
| 36° | +16.9% | +8.1% | +4.0% | +0.00% |
| 40° | +21.3% | +10.1% | +5.0% | +0.00% |
| 42° | +23.6% | +11.2% | +5.5% | +0.00% |
| 45° | +27.5% | +12.9% | +6.4% | +0.00% |
| 48° | +31.8% | +14.8% | +7.3% | +0.00% |

**The stereographic anisotropy column is the point of that projection.** It is conformal: shapes are preserved everywhere, at every field, and the residual above is the measurement's own noise rather than a distortion. Gnomonic stretches radially, and the stretch is what a reader sees as elongated clusters near the corners.

## What a straight line costs

The atlas draws its reference circles — the meridian, the mathematical horizon, the ecliptic — by clipping an **infinite great circle to the paper analytically**, with no sampling and no tolerance. That is possible because a gnomonic projection maps every great circle to a straight line exactly. `GreatCirclePage.clip` is built on it, and so is the Sprint 25 finding that a polyline cannot answer a page lying between its own vertices.

A stereographic projection maps great circles to **circles**. The straightness is not approximately lost; it is lost. Measured as the greatest departure of a projected great circle from the straight chord joining where it leaves the paper:

| field | chord measured | gnomonic | stereographic |
|---:|---:|---:|---:|
| 36° | 897 px | 0.0000 px | **7.4 px** |
| 40° | 896 px | 0.0000 px | **8.2 px** |
| 42° | 899 px | 0.0000 px | **8.6 px** |
| 45° | 896 px | 0.0000 px | **9.3 px** |
| 48° | 897 px | 0.0000 px | **9.9 px** |

So adopting stereographic is not a change of formula. **It does not end analytic clipping**: a circle and a rectangle intersect exactly too. What it ends is the existing *straight-line* clipper, which returns a chord between two page crossings, and an arc is not a chord. The cost is an exact arc representation and clipper carried through the module seam - a new geometry kind every reference-ink consumer must learn - not a forced return to sampled polylines.

## What a reader would mis-measure

The costs above are properties of the projection. This is what they do to somebody holding the printed sheet: a separation measured with a ruler near the corner, read against the scale the page centre implies.

A one-degree pair at the corner, measured as if the page had one scale:

| field | gnomonic reads | error | stereographic reads | error |
|---:|---:|---:|---:|---:|
| 36° | 1.169° | +16.9% | 1.040° | +4.0% |
| 40° | 1.213° | +21.3% | 1.050° | +5.0% |
| 42° | 1.236° | +23.6% | 1.055° | +5.5% |
| 45° | 1.275° | +27.5% | 1.064° | +6.4% |
| 48° | 1.318° | +31.8% | 1.073° | +7.3% |

At 36° the atlas already asks a reader to accept 15% in the corner and says so by stopping there. The question the gate answers is how much further that is honest on **paper**, where there is no zooming out of a mistake and no tooltip to correct it.

