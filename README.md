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
- [First sprint](docs/sprint-01.md) — Sprint 1's product document;
  from Sprint 2 onward the sprint record is the GitHub milestones and
  the handover/review pairs in [docs/reviews/](docs/reviews/)

## Requirements

- JDK 21 or later (Java 21 is the recorded minimum; sources compile with
  `--release 21`)
- GNU Make
- `curl` (present on macOS and most Linux systems)

The supported contributor environments are macOS and Linux; on
Windows, use WSL. (A native Windows workflow may return later as a
complete, tested path if there is real demand.)

## Build and run

From the repository root:

```sh
scripts/download-libs.sh         # download pinned dependencies into lib/
make test                        # compile and run the test suite
make run                         # build and launch the application
```

Dependencies are pinned (versions and SHA-256) in
`scripts/lib-versions.env`: FlatLaf and FlatLaf
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
  at their true projected size, so the wide pages stay readable;
  Messier objects remain, and a searched target with an established
  deep-sky symbol remains drawn and labelled. (Types the chart
  deliberately never draws - stellar entries, associations, novae -
  still recentre and title the chart, and a searched star remains
  subject to the stellar magnitude limit.) From 12° the charts teach
  constellation geography: traditional line figures with Latin
  constellation names in quiet grey, joined from 18° by the official
  IAU boundaries as faint dotted lines - so zooming out from M42 ends
  with the whole of Orion named around it. The 8° and closer pages
  stay exactly as before. (Line figures follow the IAU/Sky & Telescope
  chart convention; stick figures are not an IAU standard - the
  boundaries and names are.)
- **Grab to pan**: press anywhere on the paper and drag - the sky
  position under the pointer follows the hand exactly, at every field,
  across RA 0, and honestly near the poles (a north-up chart lets a
  polar grab follow only as far as its geometry allows). A real drag
  leaves any searched target behind and titles the chart by its
  coordinates; clicks and small jitter change nothing. An open hand
  marks the draggable paper, a closed hand a live grab.
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

The menu bar stays out of the way: **Help → About JUranometria** shows
the version, a short description, and the licensing of the code and
every bundled resource — including the full notices, offline — and
**JUranometria → Settings…** holds the persistent Light/Dark
appearance choice (the `--dark` launch flag remains a session-only
override that never rewrites the saved setting).

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

Nine releases in (v0.1.0 through v0.9.0; see `CHANGELOG.md`), the
atlas is a working instrument: it bundles the complete bright sky
offline (45,630 Tycho-2 stars to V 8.0 and 13,371 OpenNGC objects),
searches it by name, identifier, or coordinates, zooms from 1 to 36
degree fields with scale-honest deep-sky and constellation-geography
policies, and pans by grabbing the paper with an exact
projection-correct drag. Every release is preceded by an independent
review whose trail lives in `docs/reviews/`. The deliberately simple
plain-Java/Make organization proven in NamDesktop, and the
issue-driven sprint rhythm proven in NamWeb, both still hold.
