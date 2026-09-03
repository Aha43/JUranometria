# JUranometria

JUranometria is a quiet, interactive star atlas for learning the geography of
the sky. It presents the fixed sky as a working celestial map: white paper,
black ink, restrained labels, and continuous movement between a local finder
chart and a broad regional atlas.

It is a Java desktop application (Swing interface, Java2D renderer)
carrying the whole sky to V 8.0 fully offline: 45,630 stars with
traditional names and designations, 13,371 deep-sky objects, the 88
constellations' names, figures, and true boundaries, and an
equatorial coordinate grid - searchable, grab-to-pan, and
zoom-where-you-point. The 1.0 promise is recorded in
[the 1.0 contract](docs/decisions/one-point-zero-contract.md).

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

**Running the released application** (verified on macOS 14+, Apple
silicon and Intel; Ubuntu 24.04 LTS x86-64; and Windows 11 x86-64):
**nothing at all** - each platform download carries its own Java
runtime, so there is no Java to install. Unpack it anywhere and
launch. A portable archive is published beside them for readers who
would rather use a Java runtime of version 21 or later that they
already have. Everything works offline; the application never
touches the network. Note the licensing consequence: the bundled
Tycho-2-derived star data is CC BY-NC 3.0 IGO, so the packaged
application is for non-commercial use only.

**Building from source** (contributors):

- JDK 21 or later (sources compile with `--release 21`; the build
  selects its own JDK 21+ toolchain, `JAVA_HOME` overrides)
- GNU Make
- `curl` (present on macOS and most Linux systems)

The supported contributor environments are macOS and Linux; on
Windows, use WSL. A native Windows development workflow is not part
of 1.0 - Windows is supported as a runtime through the release
archive.

## Download and run (users)

Take the download for your machine from the
[releases page](https://github.com/Aha43/JUranometria/releases) and
unpack it anywhere (paths with spaces are fine):

| Your machine | File | Launch |
|---|---|---|
| Mac, Apple silicon | `...-macos-arm64.zip` | open `JUranometria.app` |
| Mac, Intel | `...-macos-x64.zip` | open `JUranometria.app` |
| Windows 11 (x86-64) | `...-windows-x64.zip` | `JUranometria\JUranometria.exe` |
| Linux (x86-64) | `...-linux-x64.zip` | `JUranometria/bin/JUranometria` |
| Bring your own Java 21+ | `...-portable.zip` | `./juranometria`, `juranometria.bat`, or `java -jar JUranometria.jar` |

The first four include their own Java runtime - install nothing.
Each archive carries a `README.txt`; the portable one also covers
troubleshooting for a missing or too-old Java, and `SHA256SUMS.txt`
beside the downloads lets you verify what you got.

**These builds are unsigned.** On macOS, Gatekeeper may block the
first launch: right-click the app and choose Open, or approve it
under System Settings > Privacy & Security. On Windows, SmartScreen
may show "Windows protected your PC": choose More info, then Run
anyway. Installers, signing, and notarization are post-1.0 work.

## Build and run (contributors)

From the repository root:

```sh
scripts/download-libs.sh         # download pinned dependencies into lib/
make test                        # compile and run the test suite
make run                         # build and launch the application
```

The build selects its own JDK rather than trusting whichever one leads
your `PATH`: it prefers an explicit `JAVA_HOME`, then a local Homebrew
`openjdk@21`, then the `PATH` tools. A shell that still leads with an
older JDK therefore needs no setup, and a toolchain below the JDK 21
minimum stops the build with a message rather than compiler errors. To
build against a specific JDK:

```sh
make JAVA_HOME=/path/to/jdk21 test
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

The constellation geography under `src/resources/geo/constellations/`
and the star identities under `src/resources/catalog/star-identities/`
are generated the same way from the pinned d3-celestial sources (the
identity join also reads the raw Tycho-2 files above):

```sh
scripts/download-constellation-sources.sh   # pinned d3-celestial inputs
make import-constellations
make import-star-identities
```

## Using the atlas

`make run` opens the M31 region chart. The toolbar above the page:

- **Zoom in / Zoom out** step the field width through 36°, 24°, 18°,
  12°, 8°, 6°, 4°, 3°, 2°, and 1°, always centred on the current
  target. The **mouse wheel zooms where you point**: the sky beneath
  the pointer stays beneath the pointer, exactly, as the scale steps
  (trackpad scrolling accumulates to whole steps; near a celestial
  pole the chart honestly refuses a step it cannot anchor rather
  than drift). **⌘/Ctrl +** and **⌘/Ctrl −** (keypad forms too) zoom
  about the centre from the keyboard, even while Search has focus - zooming out from a searched object progressively reveals
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
  Stars answer to their traditional names by prefix (`betel`,
  `Polaris`), to Bayer designations as the Greek letter or its
  spelled-out name plus the constellation (`α Ori`, `alpha orionis`),
  and to Flamsteed numbers the same way (`58 Ori`) — abbreviation and
  genitive both accepted. A bare letter or number lists its
  candidates with each star's full identity rather than guessing.
  Selecting a result recentres the chart anywhere under the bundled
  all-sky coverage, keeping the current field width.
- **Point at anything to ask what it is.** Click a star or a
  deep-sky symbol and the **Inspector** - the toolbar's list button,
  or View → Inspector - names it:
  designations, magnitude with its band, size and orientation where
  the catalogue records them - and plainly *"not recorded"* where it
  does not, which for deep-sky objects is often. Where several marks
  overlap you are offered the choice rather than given a guess, and
  clicking empty sky answers with its coordinates. **Selecting never
  moves the chart**; `Center here` is the one action that does, and
  only when you press it. Searching for an object selects it too, so
  the inspector is reachable without a pointer.
- **Ask what is on the page.** The Inspector has a second mode, **On
  this page**, listing everything the atlas holds on the page in
  front of you — including what the page does not draw. Each row
  gives the object, its magnitude *with the band it was measured in*,
  its distance from the centre, and what it is doing on the chart:
  *drawn*, *hidden* by a chart option, *too faint* for the page's
  magnitude limit, *too small here* to draw honestly at this field,
  or *no symbol* because the atlas draws nothing for its type. Stars
  the catalogue never named are counted in one line beneath the
  table rather than listed. Sort by any column; the default order —
  Messier numbers, then brightness, then distance — sits underneath.
- **Mark rows to find them on the chart.** Choose rows with the
  ordinary gestures (click, ⌘/Ctrl-click, Shift-click, arrow keys)
  and the atlas draws a small cross at the position of each marked
  object **the page does not already draw**; an object with a symbol
  of its own keeps it and gains nothing. The row you last reached is
  the one the **Selected** facts describe. `Center here` moves the
  chart to it, and nothing else does — reading rows never moves the
  page. `Clear marks` removes them all. Marks are working notes for
  the page you are on: they are pruned when you move away, and
  nothing is remembered between sessions.
- **Find your own sky on the fixed chart.** **View → Place and
  Time…** takes a latitude, an east-positive longitude (the label
  says which way it counts — west is negative), and one frozen
  instant in UTC. Switch on any of three reference lines and the
  chart draws them in quiet grey, beneath every star: your
  **meridian** (a solid line through both celestial poles and the
  point over your head), your **zenith** (a small ring with an
  upward tick), and your **mathematical horizon** (a dashed line —
  *mathematical* because it is where the sky meets a perfectly
  flat, perfectly transparent Earth; your real horizon has hills
  and air in it, and the atlas does not pretend to know about
  either). On most pages none of the three crosses the paper, and
  the atlas draws nothing rather than promise a line that is not
  there. **Nothing ticks**: the chart is drawn for the instant you
  typed and stays there — **Now** re-freezes on the moment you
  press it, and pressing it again is how you move the sky forward.
  **Center on zenith** moves the chart to the point overhead, and
  is the only thing in the dialog that moves the page. Behind the
  lines sits real astronomy: the chart's fixed star positions are
  for the year-2000 reference frame, and your meridian is computed
  for the sky *of your date* — carrying precession and nutation
  properly, checked against the IAU's own reference code to a
  hundredth of an arcsecond — so on a polar page the meridian
  passes honestly *beside* the chart's pole, which by 2050 will be
  a third of a degree from the pole of that night's sky. One
  stated limit: the atlas reads your instant as UTC and ships no
  earth-rotation tables, which can place the lines up to about
  14 arcseconds off their true position — under half a pixel at
  the default 8° field, but as much as 3.6 pixels of line
  placement at the narrowest 1° field. The lines are reference
  furniture, not measuring instruments, and this is the honest
  price of an atlas that phones nobody. Your place is remembered between sessions; the instant
  and the switches deliberately are not, so every session begins
  with the ordinary chart and no stale saved clock can masquerade
  as now.
- **Reset view** returns to the default M31 centre, 8° field, and
  stars to V 8.0, clearing the search.
- The readout on the right and the chart's title block always state the
  active field width and magnitude limit.

The menu bar stays out of the way: **View** carries Zoom In/Zoom Out
with their platform shortcuts, and **View → Chart Options…** lets the
reader choose the chart's content and labels, in four tabs:

- **Deep sky** — deep-sky objects as a whole, then each of the five
  symbol families on its own (galaxies, open clusters, globular
  clusters, nebulae, planetary nebulae), each row carrying the mark
  the chart actually draws for it, and deep-sky labels;
- **Stars** — star names, Bayer letters and Flamsteed numbers,
  separately;
- **Constellations** — figures, boundaries and names;
- **Chart** — the equatorial (ICRS/J2000) coordinate grid, the
  **title block** in the lower left, and a **stellar-magnitude key**
  in the upper right, which shows the circle size the chart draws for
  three visual magnitudes including the page's own limit (the key
  stays off until you ask for it, because on a crowded page it covers
  stars).

All of it with a live preview, safe Cancel, a Restore Defaults
returning the released chart exactly, and choices remembered across
restarts (a searched target always stays drawn and labelled, whatever
the toggles);
**Help → About JUranometria** shows
the version, a short description, and the licensing of the code and
every bundled resource — including the full notices, offline — and
**File → Settings…** holds the persistent Light/Dark
appearance choice (the `--dark` launch flag remains a session-only
override that never rewrites the saved setting).

Controls disable at their bounds: the bundled bright-sky pack carries
the complete sky to stars of V 8.0 (45,630 Tycho-2 stars and 13,371
OpenNGC objects in about 2.5 MiB), and the atlas never claims deeper
coverage than it holds.

**Reading the deep sky.** Every deep-sky object the chart draws
carries one of five marks, and each mark is a family you can switch
on and off in Chart Options → Deep sky, where the marks themselves
are shown beside their names:

| mark | family | what it holds |
|---|---|---|
| oriented ellipse | **Galaxies** | galaxies, and close pairs, triplets and groups |
| dotted circle | **Open clusters** | loose clusters of young stars in the Milky Way's plane |
| crossed circle | **Globular clusters** | dense, ancient balls of stars in the galactic halo |
| outlined box | **Nebulae** | emission, reflection and dark nebulae, H II regions, supernova remnants, and clusters still wrapped in nebulosity |
| small crossed circle | **Planetary nebulae** | shells thrown off by dying stars |

A galaxy is drawn at its catalogued size and orientation, so the
ellipses lean the way the galaxies do. Stellar-type entries,
associations, and novae stay undrawn - a stellar entry would only
duplicate the star layer - though they remain searchable and
recentre the chart. A `--dark` argument runs the dark
application theme; the chart page itself stays white paper in both
themes.

## Licensing

The code and documentation are MIT licensed ([LICENSE](LICENSE)).
Bundled data resources keep their own licenses — including a
Tycho-2-derived star resource under CC BY-NC 3.0 IGO that may not be
used commercially, which makes the packaged application
redistributable non-commercially only. See
[LICENSING.md](LICENSING.md) for the complete picture.

## Status

**1.0.** The atlas is a finished instrument, distributed as four
self-contained platform applications — macOS Apple silicon, macOS
Intel, Windows x86-64, Linux x86-64 — plus a portable archive for
readers who bring their own Java 21+. It bundles the complete
bright sky offline (45,630 Tycho-2 stars to V 8.0 and 13,371
OpenNGC objects), searches it by name, designation, identifier, or
coordinates, letters the constellations with their Bayer and
Flamsteed notation, draws the IAU boundaries and an equatorial
grid, zooms from 1 to 36 degree fields where you point, and pans by
grabbing the paper with an exact projection-correct drag.

What 1.0 promises — platforms, data, preferences, stable behaviour,
licensing — is written down in
[the 1.0 contract](docs/decisions/one-point-zero-contract.md), and
[the audit](docs/reviews/one-point-zero-audit.md) records how the
application was held to it. Every release is preceded by an
independent review whose trail lives in `docs/reviews/`, and is
published from an annotated tag by
[the release workflow](.github/workflows/release.yml).

Beyond 1.0: installers, code signing and notarization, and update
checking remain deliberately out of scope, recorded as candidates
rather than silent omissions. The deliberately simple
plain-Java/Make organization proven in NamDesktop, and the
issue-driven sprint rhythm proven in NamWeb, both still hold.
