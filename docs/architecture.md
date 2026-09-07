# Technical architecture

## Direction

JUranometria will start as a deliberately old-style Java desktop application,
using Swing for the window and Java2D for drawing. This combination keeps the
implementation close to the distinctive problem: celestial cartography.

Like NamDesktop, it will use plain source folders, downloaded JAR dependencies,
a Makefile, and direct `javac`/`java` commands. FlatLaf supplies the look and
feel; FlatLaf Extras and JSVG support bundled Tabler SVG icons. It will not use
Maven, Gradle, dependency injection, or another UI framework. Add machinery
only when the charting problem demonstrates a need for it.

The renderer must not depend on a Swing component. Screen display, printing,
and image export should call the same chart-rendering boundary.

## Proposed boundaries

```text
Swing UI
  -> application actions (open chart, pan, zoom)
    -> chart model and catalogue query ports
      -> projection and layout
        -> renderer
          -> Java2D drawing target
```

## Repository shape

```text
src/juranometria/app/       entry point and application wiring
src/juranometria/chart/     chart model, styling, and layout
src/juranometria/catalog/   catalogue interfaces and local implementations
src/juranometria/geo/       bundled constellation geography and its queries
src/juranometria/project/   celestial projections, transforms, and the
                            exact grab-to-pan solver
src/juranometria/ui/        Swing components and input handling
src/resources/              bundled catalogue fixtures and other resources
test/juranometria/          tests mirroring the source packages
lib/                        downloaded runtime dependencies
lib/test/                   downloaded test dependencies
build/                      generated classes and runnable application
Makefile                    build, run, test, and clean commands
VERSION                     application version
```

Package boundaries may grow with real code, but the directory model should
remain direct and unsurprising.

The initial dependency download script should pin FlatLaf, FlatLaf Extras,
JSVG, and the standalone JUnit console runner, mirroring NamDesktop's direct
dependency model. Add Jackson only when a real catalogue or settings format
requires it.

### Domain

Small immutable values describe sky positions, angular fields, stars,
deep-sky objects, labels, and chart style. Domain code does not know about
Swing, files, HTTP, or pixels except at explicit projection/render boundaries.

### Catalogue access

Catalogue interfaces answer bounded spatial queries. The first implementation
reads a tiny bundled fixture. Later implementations may use spatially tiled
local files and an optional remote name resolver.

### Projection and layout

Projection converts celestial coordinates into a chart plane. Layout decides
which labels and symbols deserve space. These are separate concerns: the same
projected position may be drawn differently at different chart scales.

**A module says what belongs on the sky. A projection says how the sky
becomes a page.** (Sprint 30, `docs/decisions/overview-projection.md`.)

The two halves of that sentence are different kinds of thing, and the
distinction is why a projection is not a module. A module contributes
domain knowledge as sky geometry — a pole, a position, a path — and must
not know, ask, or depend on how any of it lands on paper. A projection
decides the placement, the visibility, the curvature, the inverse lookup,
the navigation and the export of *everything* contributed, the modules'
geometry and the catalogue's alike. A module can be detached and the page
is the same page with less on it; change the projection and every mark
moves. One is an addition to a chart. The other is what a chart is.

This matters most where the two meet. A module names a great circle by
its pole and says what kind of line it is; it does not know whether that
circle will be drawn straight, as a circular arc, or as an ellipse,
because that is the projection's answer and it differs by projection for
the same pole. The seam is already shaped this way, and the Sprint 30
gate measured that it survives being asked a second projection.

**`juranometria.project` is that strategy** (Sprint 30, issue #297).
`Projection` is the boundary; `GnomonicProjection` and
`StereographicProjection` are the two real implementations behind it;
`Projections` is the only place in the atlas that turns a name into
one, so nothing else switches on which it holds. Which projection
draws a chart is part of the chart's own immutable state
(`ChartProjection` on the view state and the viewport), because two
charts of the same centre and field drawn differently are different
pages and a page that could not say which it was could not be
checked.

The package is geometry and nothing else: no toolkit, no preferences,
no files, no network, and — added with the strategy — no module seam
and no catalogue. A projection that named a module would invert the
rule above; one that named the catalogue would have opinions about
what is worth drawing. `RemovableModelBoundaryTest` reads the
compiled classes and holds all of it.

One calculation underlies both projections: `CentreFrame` answers
where a position lies relative to a centre, and both the placing of
positions and the naming of great circles ask it. That is not tidiness.
Sprint 30 spent five review rounds on the consequences of the same
arithmetic existing twice, and every one of them was a coordinate
degeneracy answered after the trigonometry instead of before it.

### Rendering

The renderer consumes a complete chart description and a drawing target. It
does not fetch data or mutate application state while painting. Deterministic
rendering makes visual regression tests and export practical.

### UI

Swing owns input events and window state. Panning and zooming update an
immutable viewport and request a repaint. The first vertical slice needs one
window and one custom chart component, with no general UI framework.

### Modules

The chart publishes services; removable modules consume them
(`juranometria.module`, decided in Sprint 24). A module owns its domain
state, contributes typed geometry — points, paths, regions, great circles
by their poles — under an ink role, and never receives a graphics context,
a pixel, a renderer, or the catalogue. The chart owns how each role is
inked, where it sits in the stack, and whether it draws at all.

Three modules now share the seam, which is the evidence the design asked
for: **On this page** (Sprint 24) reads the page inventory and contributes
interaction crosses; the **meridian module** (Sprint 25) owns an observer
and a frozen instant and contributes reference lines from the
UI-independent sky model in `juranometria.sky`; and the **ecliptic
module** (Sprint 28) owns one display choice and contributes a permanent
circle with four named landmarks from the same model. The chart core
learned none of those domains — it still knows nothing of tables,
observers, clocks, sidereal time, obliquity, seasons or the zodiac — and
any module detaches leaving the released page byte for byte, which the
packaged acceptance proves inside every native image. An architecture test
holds the boundaries executable: `juranometria.sky`, `juranometria.project`,
`juranometria.module`, `juranometria.meridian` and `juranometria.ecliptic`
are scanned at the class-file level for toolkit, preferences, file, and
network dependencies.

The third module also widened the vocabulary rather than the seam's
powers. A contributed great circle now says whether it is a line across
the sky, a boundary of what can be seen, or a **permanent circle of the
sphere**; a contributed point says whether it is a **place** with an up or
a **landmark** on a line. Both are statements about what the geometry is,
and neither is named for the ecliptic — a galactic-equator module would
use the same two words.

**The chart sheet** (`juranometria.sheet`, Sprint 29) is the one place a
chart stops being a thing on a screen. It plays the *production* render —
the same renderer, over a scene from the same assembler, at the same view
state, with the same module ink — into a recording `Graphics2D` that
refuses every method the cartography does not use. A writer takes that
recording and knows nothing about the sky; a module contributes geometry
and learns nothing about paper. Two things legitimately differ from the
screen and only two: the **extent**, because a chart rectangle is 271.6 mm
and not a window's shape, and the **ground**, because printing a black sky
asks a reader to lay down a sheet of toner. Both are decided in
[the printable-chart decision](decisions/printable-chart.md).

**The projection boundary** is where that sheet stops being possible
to draw honestly. The chart is gnomonic at every field it offers,
including the 42-degree sheet step Sprint 29 added, because a
gnomonic projection maps a great circle to a straight line *exactly* —
which is what lets the meridian, the horizon and the ecliptic be
clipped to the paper analytically rather than sampled. Sprint 6
declared the boundary as a corner radial scale of 1.25, near a
44-degree field; 42 degrees sits at 1.236, and a test holds it
there. Past it the chart would need a different projection and would
lose the analytic clipping three modules depend on, which is the
trade the printable-chart gate measured and refused.

Three writers sit on the far side of that boundary — SVG, PDF and
PNG — and one recording feeds all of them, so a reader who exports
the same chart three times gets the same chart three times. The
reader surface is one File-menu item and one dialog, in
`juranometria.app`; the writers know nothing of Swing, and the
dialog knows nothing of the sky.

**The Solar System road.** The ecliptic is the frame the Solar System is
described in, so this module is the frame arriving without any of the
bodies. A future module drawing the Sun, the Moon or a planet can express
its positions in ecliptic coordinates through `juranometria.sky.Ecliptic`
and contribute them as ordinary points and paths, while the chart stays
fixed to J2000 and learns no ephemeris. The transformation is already
here, held to an authority; what such a module would add is its own
ephemeris data and its own lifecycle, both removable, and the chart would
not need to change to accept them.

## Decisions deliberately deferred

- ~~The minimum supported Java release~~ — decided in issue #1 and
  settled by [the 1.0 contract](decisions/one-point-zero-contract.md):
  **Java 21**, with sources compiled `--release 21`.
- Catalogue binary format and sky-index scheme.
- SVG/PDF export libraries.
- Network client and caching policy.
- A projection for fields too wide for gnomonic rendering.

These decisions should be made when a small working slice provides evidence,
not embedded in the initial specification.

## Testing approach

- Unit tests for coordinate and projection mathematics using known points.
- Unit tests for magnitude and scale policies at their boundaries.
- Renderer tests against a deterministic in-memory or raster target.
- A small number of approved chart images for visual regression once the
  visual language stabilizes.
- UI tests only for behaviour that cannot be verified below Swing.
