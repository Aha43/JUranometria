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

The bundled all-sky catalogue under `src/resources/catalog/bright-sky/`
is a generated resource. To reproduce it from the pinned upstream
inputs (never needed for normal building or running):

```sh
scripts/download-catalogue-sources.sh   # ~170 MB into gitignored imports/raw/
make import-allsky                      # verifies checksums, regenerates the pack
```

## Using the atlas

`make run` opens the M31 region chart. The toolbar above the page:

- **Zoom in / Zoom out** step the field width through 36°, 24°, 18°,
  12°, 8°, 6°, 4°, 3°, 2°, and 1°, always centred on the current
  target - zooming out from a searched object progressively reveals
  its celestial neighbourhood. Beyond 18°, deep-sky symbols draw only
  at their true projected size (Messier objects and the searched
  target always remain), so the wide pages stay readable.
- **Fewer stars / More stars** step the stellar limiting magnitude
  between V 4.0 and V 8.0 in whole magnitudes.
- **Search** finds bundled objects and coordinates entirely offline,
  across the whole sky: names and identifiers forgivingly (`M42`,
  `Messier 31`, `NGC 224`, `Orion Nebula`, `Pleiades`,
  `TYC 4628-237-1`), and coordinates in decimal degrees
  (`83.82 -5.39`) or sexagesimal (`0:42:44 +41:16:09`, RA in hours).
  Selecting a result recentres the chart anywhere under the bundled
  all-sky coverage, keeping the current field width.
- **Reset view** returns to the default M31 centre, 8° field, and
  stars to V 8.0, clearing the search.
- The readout on the right and the chart's title block always state the
  active field width and magnitude limit.

Controls disable at their bounds: the bundled bright-sky pack carries
the complete sky to stars of V 8.0 (45,630 Tycho-2 stars and 13,371
OpenNGC objects in about 2.5 MiB), and the atlas never claims deeper
coverage than it holds. Non-galaxy deep-sky types are searchable and
recentre the chart but await their chart symbols. A `--dark` argument runs the dark application
theme; the chart page itself stays white paper in both themes.

## Licensing

The code and documentation are MIT licensed ([LICENSE](LICENSE)).
Bundled data resources keep their own licenses — including a
Tycho-2-derived star resource under CC BY-NC 3.0 IGO that may not be
used commercially, which makes the packaged application
redistributable non-commercially only. See
[LICENSING.md](LICENSING.md) for the complete picture.

## Status

The project has an executable Swing foundation and is otherwise in
specification and visual-prototype stage. It follows the simple Java/Swing
structure proven in NamDesktop and the issue-driven sprint and release rhythm
proven in NamWeb.
