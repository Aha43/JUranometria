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
src/juranometria/project/   celestial projections and transforms
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

### Rendering

The renderer consumes a complete chart description and a drawing target. It
does not fetch data or mutate application state while painting. Deterministic
rendering makes visual regression tests and export practical.

### UI

Swing owns input events and window state. Panning and zooming update an
immutable viewport and request a repaint. The first vertical slice needs one
window and one custom chart component, with no general UI framework.

## Decisions deliberately deferred

- The minimum supported Java release (start from the installed current LTS or
  later release and record the decision in the first implementation issue).
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
