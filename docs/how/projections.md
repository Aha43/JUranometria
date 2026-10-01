# Technical companion — From the celestial sphere to a page

This is the source and verification layer for the reader-facing article. It
is deliberately separate: the article should explain the sky without making
the reader audit a Java project, while the “How JUranometria works” series
should make every architectural claim traceable.

## Claim map

| article claim | implementation or record | verification note |
|---|---|---|
| gnomonic radius is `tan(t)` | `GnomonicProjection.planeRadius` | central scale tends to one; 90° is outside the finite plane |
| stereographic radius is `2 tan(t/2)` | `StereographicProjection.planeRadius` | antipode is the missing point; local angular distortion is zero |
| orthographic radius is `sin(t)` | `OrthographicProjection.planeRadius` | 90° is the included limb; the far hemisphere is outside the domain |
| half the field occupies half the page width | `ViewportMapping` | ordinary charts use the projection’s radius at half-field |
| globe sizing differs from rectangular fields | `ViewportMapping`, `celestial-globe.md` | the full disc fills the short page dimension |
| east maps left and north maps up | `ViewportMapping` | verify again in Figure 2; perspective artwork can easily reverse it |
| the page carries the projection that placed it | `DrawnPage` | prevents orthographic marks being scaled stereographically |
| great circles are analytic conics | `Projection.greatCircle`, `PlaneConic` | no sampled polyline is the source of truth |
| page chooses line/circle/ellipse | `ViewportMapping.onPage`, `PlaneCurve` | substitution is governed by measured page-space error |
| drawing and interaction share projection geometry | projection `project`/`unproject`, `PanSolver` | pointer-preserving zoom must use the destination projection |
| screen and export share the assembled page | chart/sheet recording path | describe as a shared input, not independent parsing of PDF/PNG text |

## Primary records

- `docs/decisions/overview-projection.md`
- `docs/decisions/celestial-globe.md`
- `docs/architecture.md`
- `src/juranometria/project/Projection.java`
- `src/juranometria/project/GnomonicProjection.java`
- `src/juranometria/project/StereographicProjection.java`
- `src/juranometria/project/OrthographicProjection.java`
- `src/juranometria/project/ViewportMapping.java`
- `src/juranometria/project/DrawnPage.java`
- `src/juranometria/project/PlaneConic.java`
- `src/juranometria/project/PlaneCurve.java`
- `src/juranometria/project/PanSolver.java`

## Editorial sidebars

### Module versus projection

Keep this in the technical-series version and consider shortening it for a
magazine version.

```text
catalogue + modules ──► sky geometry ──► projection ──► page ──► renderer
       what belongs          where every mark lands
```

Removing a module removes one contribution while leaving the other marks in
place. Changing projection moves every mark and changes domain, scale,
curvature and inverse navigation. That is the architectural reason projection
is part of the page rather than an optional sky module.

### East-left versus local east

The article’s Polaris/planetarium comparison rests on two independent facts:

1. increasing right ascension maps left on JUranometria’s page;
2. for an observer facing north, the local eastern horizon lies to the right.

The released mathematical-horizon module’s N/E/S/W marks show the second fact
without rewriting the first. Their placement is computed from the observer,
instant and view rather than illustrated from memory.

## Figure programme

The reader article keeps the physical gnomonic construction in the main
narrative. The architecture route remains useful here:

![A star direction passing through the local frame, projection plane and
viewport to become a mark on the page](../articles/images/star-direction-to-page.svg)

The projection turns direction into a plane point; the viewport turns that
point into page ink. Keeping those stages separate is what lets screen,
navigation and export share the same geometry.

The detailed production plan is embodied by
`../articles/from-celestial-sphere-to-page.md` and its committed figures.

1. Same sky under gnomonic, stereographic and orthographic projection.
2. Gnomonic projection as a lit sphere and tangent plane.
3. One great circle becoming a line, circle and ellipse.
4. Paper edge versus the orthographic limb.

The direction → frame → plane → page diagram above is the technical companion
to Figure 2 rather than one of the reader article’s four figures.

Figure 1 tests the article’s central promise with the real assembler and
renderer, identical sky content and typography. Its caption states that equal
rectangles and nominal fields do not imply identical corner coverage.

### Figure 1 draft record

The first composite uses the three existing
`docs/studies/overview-projection/orion-090-*.png` artifacts. They are
JUranometria output at the same Orion centre and nominal 90° field; the
composite scales each complete page uniformly and adds only headings around
them; no astronomical mark is redrawn or repositioned.
The finished comparison is committed as
`docs/articles/images/three-projections.png`.

The differing printed star counts are retained. They prevent the reader from
mistaking a common rectangle and nominal field for identical corner coverage.
This is a comparison plate, not the globe-limb illustration: the orthographic
90° panel compresses the outer field but does not reach its 90° radius limb.
Figure 4 shows that boundary using the released 180° globe beside a
stereographic page. Figure 2 is purpose-built vector artwork checked against
`GnomonicProjection.planeRadius`; the technical diagram above is checked
against `AzimuthalProjection` and `ViewportMapping`. Figure 3 is a schematic
of the analytic forms stated by `Projection.greatCircle`.

## Publication verification

- The reader article deliberately does not freeze the field ladder into prose;
  the current production choice remains `ChartProjection.forField`.
- The 60° and 80° radii were checked against the three production
  `planeRadius` implementations at the 3.0 publication boundary.
- The pointing sentence is limited to angular directions on the tangent-plane
  chart; it makes no claim about mount error, refraction or epoch correction.
- The local-east example now names the released mathematical-horizon cardinal
  marks; the global east-left statement remains chart-coordinate prose.
- Keep the orthographic outside-limb wording precise: it is outside the
  *visible hemisphere*, not outside the celestial sphere.
- All four captions were read against the committed figures; Figure 1 names
  its differing coverage, and Figure 4 distinguishes paper from sky.


[Return to the reader-facing article](../articles/from-celestial-sphere-to-page.md).
