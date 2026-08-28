# JUranometria

JUranometria is a quiet, interactive star atlas for learning the geography of
the sky. It presents the fixed sky as a working celestial map: white paper,
black ink, restrained labels, and continuous movement between a local finder
chart and a broad regional atlas.

The project is planned as a Java desktop application with a Swing interface
and a Java2D chart renderer. The first milestone is deliberately smaller than
a usable astronomy application: render a convincing, deterministic chart of
the M31 region from bundled sample data.

## Project documents

- [Product vision](docs/product-vision.md)
- [Chart conventions](docs/chart-conventions.md)
- [Application appearance](docs/application-appearance.md)
- [Technical architecture](docs/architecture.md)
- [Catalogue strategy](docs/catalogues.md)
- [Development workflow](docs/development.md)
- [First sprint](docs/sprint-01.md)

## Requirements

- JDK 21 or later (Java 21 is the recorded minimum; sources compile with
  `--release 21`)
- GNU Make
- PowerShell (`pwsh`) for the dependency download script

## Build and run

From the repository root:

```sh
pwsh scripts/download-libs.ps1   # download pinned dependencies into lib/
make test                        # compile and run the test suite
make run                         # build and launch the application
```

Dependencies are pinned in `scripts/download-libs.ps1`: FlatLaf and FlatLaf
Extras for the look and feel, JSVG for SVG icon rendering, and the standalone
JUnit console runner (test only). Downloaded JARs live in `lib/` and
`lib/test/` and are not committed.

## Status

The project has an executable Swing foundation and is otherwise in
specification and visual-prototype stage. It follows the simple Java/Swing
structure proven in NamDesktop and the issue-driven sprint and release rhythm
proven in NamWeb.
