# Changelog

All notable changes to JUranometria will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- **The suite's shared-state consolidation** (Sprint 26, issue
  #224). `SwingSession` grew the whole restoring vocabulary — a
  locale guard, a time-zone guard, a repaint-manager guard, a
  scratch-preferences guard whose removal survives a failing body,
  and a `capture()`/`restore()` pair for disturbances that span
  JUnit fixtures — and every JVM-global state in the suite now
  flows through it. The gate's five flagged files are settled,
  including the two real leaks: the Sprint-1 smoke test that had
  themed every later test in the JVM for twenty-six sprints, and a
  scratch preference node that outlived every run. The exit probe
  cleans up in a JVM shutdown hook, the only place that runs on a
  path whose success is `System.exit`. The gate test now pins the
  settled state: zero unprotected, and the only local restorer of
  a JVM-global is the guard's own body.

### Added

- **The test-and-evidence friction gate** (Sprint 26, issue #241).
  A mechanical scan of the test suite (`make test-evidence-study`)
  and the decision document `docs/decisions/test-evidence.md`: what
  process-wide state the suite touches and how it is protected (two
  real leaks found: a look and feel installed by the first smoke
  test and never restored, and a scratch preference node written to
  the developer's real store on every run), which display tests state their
  premises, where the event-thread traffic runs, what each kind of
  generated evidence must promise, and five guards — each proven
  against a deliberately broken fixture, with the remaining debt
  pinned so it can shrink under its named owner but never grow.
  Production is untouched.

## [1.6.0] - 2026-09-03

Sprint 25 — Orient the chart to place and time. The atlas has
always drawn one fixed sky for every reader alike; it can now
answer the first question an observer standing outside asks of a
chart: *where is all this, for me, tonight?* A removable module
lays the reader's own meridian, zenith and mathematical horizon
over the unchanged chart, for one frozen instant at one stated
place — computed properly across the reference frames, checked
against the IAU's own code to a hundredth of an arcsecond, drawn
as exact straight lines, and honestly absent from every page they
do not cross. Nothing ticks, nothing phones anywhere, and with the
module quiet the atlas is the released atlas, byte for byte.

### Added

- **The place-and-time journey and packaged evidence** (Sprint 25,
  issue #229). One production-path journey from the View menu to a
  byte-identical homecoming, packaged acceptance that proves the
  model, the module, the painter and the stored-place restart inside
  every native image, reader documentation for the new controls, and
  the sprint handover in `docs/reviews/sprint-25-handover.md`.
- **Place and Time controls** (Sprint 25, issue #228). A dialog on
  the View menu, beside Chart Options: latitude, east-positive
  longitude (the label says which way it counts), and a frozen UTC
  instant, with switches for the meridian, the mathematical horizon
  and the zenith, and exactly two actions — **Now**, which re-freezes
  on the moment it is pressed, and **Center on zenith**, the one
  thing that moves the page. Typing applies nothing until the field
  is committed. The place is remembered between sessions; the
  instant and the switches deliberately are not.
- **The removable meridian module and reference-line ink** (Sprint
  25, issue #227). The atlas's first sky-reading module: it owns an
  observer and offers the meridian, the mathematical horizon and the
  zenith to the chart as typed geometry, which the chart inks in its
  own quiet vocabulary — a solid grey line, a dashed boundary, a
  small ring for the point overhead — above the grid and beneath
  every star. Clipped analytically, silent off the page, and
  removable to the byte: with the module absent or quiet the chart
  is the released page exactly.
- **The observer, sidereal time, and the sky's orientation** (Sprint
  25, issue #226). The atlas can now be told where a reader is and
  when they are looking, and work out what is overhead, where their
  meridian runs and where the mathematical horizon lies — all in the
  chart's own J2000 frame, carrying precession and nutation properly
  rather than taking the shortcut the gate measured and rejected.
  UI-independent: no window, no preferences, no clock, no network.
  Held to IAU SOFA to a hundredth of an arcsecond.

## [1.5.0] - 2026-09-03

Sprint 24 — Discover what is on this page. The atlas could always
draw the sky; it could not tell a reader what was on the page in
front of them, and least of all about the things it had decided not
to draw. A reader looking at Andromeda saw M31 and had no way to
learn that NGC 206 was there too, because the atlas draws nothing
for its type and therefore said nothing at all.

Four issues: a design gate that measured the feature before building
it, a page inventory and the chart's first module seam, the table
and its working crosses, and the closing journey.

### Added

- **On this page** (Sprint 24, issues #214–#217). The Inspector can
  now answer *what is on the page in front of me* — including what
  the page does not draw. It lists every catalogued object on the
  paper with its magnitude and band, its distance from the centre,
  and why it can or cannot be seen; marking a row draws a small
  cross wherever the chart shows nothing, so an object that is
  present and invisible can still be found. Nothing is remembered
  between sessions, and a page with nothing marked is the page the
  atlas has always drawn.
- **The table and the crosses, in detail** (issue #216). The Inspector gains a second mode listing everything the
  atlas holds on the page in front of the reader - object, magnitude
  with its band, distance from centre, and what it is doing on the
  chart - sortable by any column with the decided order beneath, and
  worked entirely by the platform's own gestures. Marking rows draws
  restrained crosses at the positions of the objects the page does
  <em>not</em> already draw; a visible object keeps its own symbol
  and gains nothing. The row a reader last reached leads, and its
  facts appear where they always have. **Center here** and **Clear
  marks** are explicit: reading a row never moves the page. Panning,
  zooming or searching elsewhere prunes the marks and the rows
  together, and nothing is remembered between sessions.

- **A page inventory, and the reader's working marks** (Sprint 24,
  issue #215). The chart can now be asked what is on the page it is
  showing: every catalogue object whose recorded ellipse reaches the
  paper, each carrying production's own answer for why it can or
  cannot be seen there - drawn, hidden by a chart option, fainter
  than the magnitude limit, no symbol for its type, or too small at
  this field. Presence is a fact about the sky and visibility is a
  fact about the reader's choices, and the two are now separable.
  Alongside it, a UI-independent model of the reader's *working
  marks*: an ordered set with one lead, the lead feeding the chart's
  existing singular selection, pruned in one transition when the
  page moves, and never persisted.
- **The chart's first module seam** (issue #215). The chart
  publishes services; modules consume them. A module contributes
  typed geometry with an ink role - a point, a path, a region, each
  with an identity and an accessible name - and never a graphics
  context, so the chart keeps its cartography. Every contribution is
  owned: a module registers under its own name and holds the one
  handle that withdraws it, so modules cannot overwrite each other
  and a detaching module takes back its own ink and nothing else.
  Asserted rather than described: the core imports neither the
  services nor the seam, and the atlas builds and draws its ordinary
  chart with every module absent.

### Changed

- The renderer's paper rectangle is now published as
  `ChartRenderer.paperOf` and shared, rather than copied wherever
  "on the paper" is asked. The Sprint 24 study also gave up its own
  copies of the page geometry, the visibility rules and the ordering
  - it consumes the production services now, and its report
  reproduces byte for byte through the change.

## [1.4.0] - 2026-09-02

Sprint 23 — Polish the instrument. A maintenance sprint, and the
kind only possible once an application is real enough to be used:
most of it came from opening the atlas and finding it wanting. The
founding Andromeda page named three galaxies and drew two, a switch
labelled "Galaxies" left one galaxy behind, the Inspector had no way
to close itself, the version needed a dialog to read, and the
application arrived in the dock wearing Java's default cup. All of
it decided at measured gates and reviewed at every step
([docs/reviews/sprint-23-handover.md](docs/reviews/sprint-23-handover.md)).

### Added

- **JUranometria has its own application mark**
  ([#200](https://github.com/Aha43/JUranometria/issues/200),
  [#202](https://github.com/Aha43/JUranometria/issues/202)). A
  cropped piece of Andromeda cartography - a galaxy crossing the
  corner and leaving the frame, three stars above it - drawn in the
  chart's own palette. Chosen at a coded gate over four candidates
  and generated by `make icons` from **one geometry**, so the window
  icon the application draws at run time and every container it
  installs are the same drawing rather than a resampling. It replaces
  the Tabler `north-star` glyph the images carried through 1.3.0, and
  it is the project's own MIT-licensed source rather than a third
  party's.
- The mark is now the **real window icon** on every launch, packaged
  and portable alike; before this the application set none at all and
  a task switcher showed Java's default cup. It appears in Help -
  About beside the version, decoratively: the version, the licensing
  summary and the way to the notices all keep their room, and
  assistive technology is told the application's name rather than
  where three stars sit.

- **The running version and a way out, at the end of the toolbar**
  ([#198](https://github.com/Aha43/JUranometria/issues/198)). The
  version is quiet status text - `v1.3.0`, not focusable, handed to
  the toolbar from the same `AppInfo.version()` About prints, so
  there is no second copy and no second way to format one. When the
  bar is squeezed the version is the first thing to go, hidden whole
  rather than truncated into an ambiguous number, and every control
  stays. **Exit JUranometria** sits after it, at the far right.
- **One shutdown path** ([#198](https://github.com/Aha43/JUranometria/issues/198)).
  The toolbar button, the window's close box and the platform's Quit
  all take the same route: detach, flush preferences, dispose every
  window, terminate - in that order, once, however many times it is
  asked. Nothing terminates on its own any more.

### Fixed

- **Packaging can no longer ship a broken or substituted application
  icon** ([#202](https://github.com/Aha43/JUranometria/issues/202)).
  A missing icon was a silent fallback rather than a failure, and a
  container present but wrong passed everything: eight bytes of
  rubbish named `JUranometria.icns` built a complete, passing
  application image, because jpackage copies the container verbatim
  and every check compared that copy against the same rubbish.
  `scripts/verify-icons.sh` now regenerates every container from the
  chosen geometry and compares it byte for byte with what is
  committed, and the build refuses to run without it.

- **Every toolbar control is reachable by keyboard again**
  (found while building [#198](https://github.com/Aha43/JUranometria/issues/198)).
  Each button is built asking to be focusable and FlatLaf's toolbars
  take it away when the button is added, by their own convention -
  so in practice no control on the bar could be reached without a
  pointer, though the code had said otherwise since the toolbar was
  written. The intent is now re-asserted after the bar is built, and
  a regression checks it under the look and feel the application
  actually runs.

- **A close button in the Inspector's own heading**
  ([#197](https://github.com/Aha43/JUranometria/issues/197)). The
  toolbar toggle remains the obvious way to reopen the pane, but once
  it is open a reader looks inside it - at the upper-right corner -
  for the way to dismiss it, as they would in any other side pane.
  The button writes the same requested visibility the toggle writes,
  so there is one wish and one switch: the toolbar and the View menu
  follow immediately, and a pane closed deliberately does not
  reappear when the window widens. Closing leaves navigation, the
  searched target, the chart options and the selection exactly as
  they were, and the chart relays itself out for the width it gets
  back - as it always has when the pane is toggled from the toolbar.
  Escape and the button share one dismissal, which hands the reader
  back to the chart. The mark is the pinned Tabler `x`, added
  through the existing icon and notice workflow.

### Fixed

- **Hiding a deep-sky family now hides its searched target too**
  ([#196](https://github.com/Aha43/JUranometria/issues/196)). Search
  M 33, switch **Galaxies** off, and every galaxy disappeared except
  M 33 - because a chart titled for an object drew that object, which
  was internally consistent and read as one unexplained galaxy on a
  page with galaxies switched off. The explicit hide now wins: the
  target retires where it stands, its label and identity clearing
  together, the chart falling back to its honest coordinate title,
  and the centre, field width, limiting magnitude and every unrelated
  option untouched - the same transition panning already makes. The
  master switch follows the same rule; selection survives and the
  Inspector says plainly that what is selected is no longer on the
  page; hiding an unrelated family is still repaint-only. Decided at
  [docs/decisions/target-retirement.md](docs/decisions/target-retirement.md).

- **Large deep-sky symbols no longer hide their smaller companions**
  ([#201](https://github.com/Aha43/JUranometria/issues/201)). The
  default Andromeda page named three galaxies and showed two: the
  bundled rows arrive as NGC 205, NGC 221, NGC 224, and the galaxy
  symbol fills opaquely, so M 31's 178-arcminute disc was painted last
  and covered M 32 completely. Its label still drew, leaving a name
  with no mark to attach it to. The renderer now paints deep-sky
  symbols by one cartographic rule - the larger painted footprint goes
  behind, ties broken by catalogue identity - measured from the drawn
  axes so it neither turns with a rotated ellipse nor ignores the
  practical-minimum clamp. Across 18 measured pages, storage order
  fully buried 60 symbols; the rule leaves none. Decided at
  [docs/decisions/deep-sky-stacking.md](docs/decisions/deep-sky-stacking.md).
  `docs/reference/m31-stars.png` changes by 201 pixels, all of them
  M 31's companions becoming visible.

- A duplicate delivery of one tag push no longer leaves a red release
  run beside a correct release ([#195](https://github.com/Aha43/JUranometria/issues/195)).
  Releasing 1.3.0 produced two runs from a single `git push origin
  v1.3.0`; the guard serialised them, the first published, and the
  second refused - correctly, since a release people may already have
  downloaded is never silently replaced. The refusal is now two
  answers rather than one. The publishing job reads the existing
  release back and finishes green, having changed nothing, when that
  release is this same delivery; a release it did not build still
  fails, loudly, naming what differs. The identity is carried by the
  portable archive, which `make dist` builds reproducibly from source
  - the four native images cannot carry it, because jpackage does not
  build them byte-identically across runners, as the two 1.3.0 runs
  themselves demonstrated. Every published archive is downloaded and
  hashed against the published checksums, so an archive substituted
  under its own name is caught; and each application image now
  records the **source commit** it was packaged from, which is what
  notices a change in the packaging scripts or the bundled runtime
  that an identical portable archive would not.

## [1.3.0] - 2026-09-01

Sprint 21 — Read the deep sky. The chart has drawn five different
deep-sky symbols since Sprint 6 and never said what any of them
meant. Now each one is a family you can switch on and off, and the
dialog that switches them is where you learn them. Decided at a
measured gate
([docs/decisions/deep-sky-vocabulary.md](docs/decisions/deep-sky-vocabulary.md))
over all 13,371 bundled deep-sky rows, and reviewed at every step.

### Added

- **The five deep-sky families are yours to choose.** Galaxies, open
  clusters, globular clusters, nebulae and planetary nebulae each have
  their own switch, beneath the deep-sky master that still governs
  them all. A family you switch off is remembered while the master is
  off, and comes back exactly as you left it.
- **Chart Options now has four tabs** — Deep sky, Stars,
  Constellations, Chart. The Deep sky tab is a legend as much as a
  control: every family shows the mark the chart actually draws for
  it, with a sentence saying what the family holds and objects you
  may know.

### Changed

- **Nebula boxes are drawn a little darker** (grey 150 to 132). They
  are still the quietest mark on the page, but a mark you are asked
  to recognise has to be visible: the old grey fell just under the
  contrast floor for a graphical object.
- The Inspector now writes **H II region** rather than "hii region".

## [1.2.0] - 2026-09-01

Sprint 20 — Chart furniture. The chart has always drawn brightness as
circle size without saying so; now it can. The Inspector moves to the
toolbar, and the page's own furniture becomes the reader's to choose.
Decided at a measured gate
([docs/decisions/chart-furniture.md](docs/decisions/chart-furniture.md)),
reviewed at every step, and deliberately small.

### Added

- A **stellar-magnitude key**: the chart draws brightness as circle
  size, and now it can say so. Three circles in the upper right at
  exactly the sizes the chart uses, including the page's own limiting
  magnitude. Off until you ask for it in Chart Options, because on a
  crowded page it covers stars.
- The **title block** in the lower left is now a Chart Option too,
  for a reader who wants the bare page.
- The **Inspector** has a toolbar button beside the other essential
  controls. It, the View menu item, and the window's width always
  agree about whether the panel is showing - and a window too narrow
  to show it says so rather than claiming otherwise.

## [1.1.0] - 2026-09-01

Sprint 19 — Explore the map. The atlas could always find Betelgeuse;
now it can answer the opposite question. Point at any mark and it
tells you what it is, states plainly what the catalogue does not
record, and offers the choice where marks overlap — without moving
the chart or adding a line of permanent ink to it. Decided at a
measured gate
([docs/decisions/point-and-identify.md](docs/decisions/point-and-identify.md))
and reviewed by Codex at every step.

### Added

- **Point at the chart and ask what it is.** Clicking a star or a
  deep-sky symbol names it in a new Inspector panel
  (View → Inspector): designations, magnitude with its band, size
  and orientation where the catalogue records them, and "not
  recorded" where it does not. Overlapping marks are offered as a
  choice rather than resolved silently; empty sky answers with its
  coordinates. Selecting never moves the chart - `Center here` is
  the only action that does - and searching for an object selects
  it, so the inspector is reachable from the keyboard.
- Deep-sky records now keep what the source actually measured beside
  the values the chart draws, so a substituted size or a blue
  magnitude is never presented as a recorded visual one.

## [1.0.0] - 2026-08-31

**JUranometria 1.0.** A quiet, fully offline desktop star atlas:
white paper, black ink, north up and east left, the whole sky to
V 8.0. Search it by name, designation, identifier, or coordinates;
grab the paper and pan; zoom where you point; read the
constellations by their own Bayer and Flamsteed notation under an
equatorial grid.

Everything 1.0 promises is written down in
[the 1.0 contract](docs/decisions/one-point-zero-contract.md), and
the audit that held the application to it is in
[docs/reviews/one-point-zero-audit.md](docs/reviews/one-point-zero-audit.md).
This release changes no chart behaviour: it is 0.17.0, audited,
with its distribution and release path proven.

### Added

- Releases are published by pushing an annotated `vX.Y.Z` tag. The
  tag, `VERSION`, and the changelog section must agree before
  anything is built; the five artifacts are produced by the same
  workflows that gate every pull request, verified as a set, and
  published once with their checksums.

### Fixed

- A damaged or incomplete download now says so. Previously a
  corrupt bundled file threw before the window existed, printing a
  stack trace to a console a packaged reader does not have and
  leaving the application running with nothing on screen; it now
  names the file that failed to verify, gives the remedy (download
  again, check the published SHA-256, unpack fresh), and exits.

## [0.17.0] - 2026-08-31

Sprint 17 — Letter the stars. The Bayer and Flamsteed identities
that Sprint 13 bundled and made searchable become restrained chart
notation, so a constellation page now names the pattern it draws.
Reviewed by Codex at the design gate and at every implementation
step, including the closing journey.

### Added

- The stars are lettered: Bayer designations now reach the wide
  constellation pages in conventional notation - the letter with
  its component digits raised (π³, α¹) - and travel beside a
  traditional name where a star has both ("Betelgeuse α",
  "Gacrux γ"). Greek letters draw at every field; the post-omega
  Latin letters wait for the regional charts, where Flamsteed
  numbers keep their released limit.
- Three independent Chart Options controls replace the single star
  label toggle: **Star names**, **Bayer letters**, and **Flamsteed
  numbers**. A preference store from an earlier release carries its
  single choice into all three, and from the first confirmation
  each layer answers to its own setting.

## [0.16.0] - 2026-08-30

Sprint 16, in progress — the distribution work of the reviewed 1.0
contract, released early so the atlas can be installed and used
while the remaining stabilization issues close. Reviewed by Codex
at the distribution gate, the contract amendment, and each
implementation step.

### Added

- **Self-contained application images: users no longer install
  Java.** Each platform download - macOS Apple silicon, macOS
  Intel, Windows x86-64, Linux x86-64 - carries its own pinned
  Temurin 21 runtime, trimmed by `jlink` to the six modules the
  atlas actually uses (about 80 MB unpacked, 25 MB compressed).
  Built natively on each platform, never cross-packaged, and
  verified there: launch with no system Java at all, light and
  `--dark`, from paths containing spaces, with the About surface
  and a real preference change-and-reload exercised through the
  bundled runtime, the complete licensing inventory and the
  runtime's own legal notices asserted mechanically, and the images
  byte-reproducible across repeated builds - the two macOS
  architectures even render identical charts to each other. The
  applications are unsigned; the packaged README explains
  Gatekeeper and SmartScreen plainly.
- The unpack-and-run release archive: `make dist` deterministically
  builds `JUranometria-X.Y.Z.zip` - the application JAR, its pinned
  libraries, POSIX and Windows launch helpers with readable
  missing/old-Java diagnostics, and every licence and notice
  including the runtime libraries' - verified for exact contents and
  a packaged headless smoke render, then exercised on macOS, Linux,
  and Windows CI with real GUI launches from paths containing
  spaces.

### Fixed

- The build selects its own JDK instead of trusting whichever one
  leads the shell `PATH`: `make` resolves an explicit `JAVA_HOME`
  first, then a local Homebrew `openjdk@21`, then the `PATH` tools, so
  a shell whose `PATH` still leads with an older release builds and
  runs the atlas with no manual environment setup. A toolchain older
  than the recorded JDK 21 minimum stops the build with a message
  naming the required and detected versions instead of compiler
  errors.

## [0.15.0] - 2026-08-30

Sprint 15 — Read the coordinates. Reviewed by Codex at the design
gate, at each implementation step, and at the sprint close; the
review trail lives in `docs/reviews/`.

### Added

- The charts read their coordinates: a quiet equatorial (ICRS/J2000)
  grid - projection-correct meridians and parallels drawn beneath
  everything, with right-ascension labels along the bottom edge and
  signed declination labels along the left, adapting its intervals
  to every field width and converging cleanly at the poles. One new
  Chart Options control, "Equatorial coordinate grid" (default on,
  Content group), gates it repaint-only; the M31 reference
  deliberately gains the reviewed graticule.

## [0.14.0] - 2026-08-30

Sprint 14 — Zoom where you point. Reviewed by Codex at the design
gate, at each implementation step, and at the sprint close; the
review trail lives in `docs/reviews/`.

### Added

- Zoom where you point: the mouse wheel steps the field width with
  the sky beneath the pointer held exactly beneath the pointer -
  projection-correct through the pan solver's closed-form geometry,
  never a screen-space approximation. One notch is one step;
  trackpad rotations accumulate to whole steps; each accepted step
  is exact and preflight-reversible (a step the chart cannot anchor
  or reverse - near a celestial pole - honestly refuses rather than
  drift), and a pointer step that moves the centre clears the
  searched target like a pan. Platform zoom shortcuts (Cmd/Ctrl
  with +, -, =, and the keypad forms) and View-menu Zoom In/Out
  zoom about the centre, keeping the target, even while Search has
  focus - unmodified typing is never intercepted.

## [0.13.0] - 2026-08-30

Sprint 13 — Name the stars. Reviewed by Codex at the design gate, at
each implementation step, and at the sprint close; the review trail
lives in `docs/reviews/`.

### Added

- The charts name their stars: a restrained, scale-sensitive label
  pass renders traditional names, Bayer letters, and Flamsteed
  numbers per the reviewed policy - names V ≤ 2.5 on the widest
  pages, names and Bayer V ≤ 3.0 from 12-18°, all three forms on
  regional pages (Flamsteed to V 5.0) - brightest first,
  deterministic, yielding to deep-sky labels and the title block
  (prefer omission). A searched star always keeps its best identity
  label, exempt from thresholds and collisions, with no new symbol.
  One new Chart Options control, "Star names and identifiers"
  (default on), gates the pass; the M31 reference deliberately gains
  its one accepted label (35 And).
- Stars are findable by identity, fully offline: traditional names by
  prefix ("betel", "Polaris"), Bayer designations as the Greek letter
  or its spelled-out name plus the constellation ("α Ori",
  "alpha orionis" - IAU abbreviation and genitive both accepted, with
  and without a component digit), and Flamsteed numbers the same way
  ("58 Ori"). A bare letter or number lists its candidates rather
  than guessing, and every star result shows its full identity
  ("Betelgeuse · α Ori · V 0.6"). Star records carry a structured
  identity loaded from the checksummed pack; existing object,
  coordinate, and TYC searches are unchanged.
- Bundled star-identity pack (`star-identities.csv`, 4,805 rows):
  traditional star names, Bayer designations, Flamsteed numbers, and
  constellation memberships from the pinned d3-celestial
  `starnames.json` (BSD-3-Clause), joined to the bright-sky pack by
  Hipparcos number through the raw Tycho-2 catalogue - the reviewed
  decision's join, reproduced by `make import-star-identities` with
  every exception category counted and a checksummed manifest. The
  application does not read the new fields yet.

## [0.12.0] - 2026-08-30

Sprint 12 — Let the reader choose the chart. Reviewed by Codex before
release; the review trail lives in `docs/reviews/`.

### Added

- The reader chooses the chart: View > Chart Options... offers five
  content and label toggles - deep-sky objects, deep-sky labels,
  constellation figures, boundaries, and names - previewing live on
  the chart as they change, confirmed with OK, abandoned safely with
  Cancel or Escape, and remembered across restarts. Restore Defaults
  brings back the released chart exactly. Every scale rule stays
  automatic inside an enabled layer, and a searched target with an
  established symbol is always drawn and labelled whatever the
  toggles - the chart never titles itself by an object it hides.
  Labels follow their symbols and names follow their figures; Home
  still resets navigation only.

## [0.11.0] - 2026-08-30

Sprint 11 — Give the application a public face. Reviewed by Codex
before release; the review trail lives in `docs/reviews/`.

### Added

- A restrained menu bar: Help > About JUranometria shows the packaged
  version, the product description, and the licensing of the code and
  every bundled resource - with the complete notices readable offline -
  and File > Settings... holds a persistent Light/Dark
  appearance choice, applied live on OK and remembered across
  restarts. The --dark launch flag remains a session-only override
  that never rewrites the saved setting; the chart page keeps its own
  white paper and dark ink in both appearances.

## [0.10.0] - 2026-08-30

Sprint 10 — Refresh the foundations. A maintenance release; reviewed
by Codex before release, the trail lives in `docs/reviews/`.

### Changed

- Dependencies refreshed to current releases: FlatLaf and FlatLaf
  Extras 3.4.1 -> 3.7.2 and JSVG 1.7.2 -> 2.1.0 (moved together -
  FlatLaf Extras 3.7.2 requires JSVG 2), and the JUnit console runner
  1.10.2 -> 6.1.3. The application's chrome, icons, themes, and the
  chart itself are unchanged; the M31 reference remains
  byte-identical.
- The PowerShell bootstrap script is retired: the supported
  contributor environments are macOS and Linux (Windows via WSL), and
  scripts/lib-versions.env with scripts/download-libs.sh is the one
  bootstrap authority.

## [0.9.0] - 2026-08-30

Sprint 9 — Make a clean checkout dependable. A maintenance release;
reviewed by Codex before release, the trail lives in `docs/reviews/`.

### Added

- A POSIX dependency bootstrap: `scripts/download-libs.sh` fetches the
  pinned dependencies on macOS and Linux without PowerShell, sharing
  one authoritative version-and-checksum file
  (`scripts/lib-versions.env`) with the PowerShell script and the
  Makefile. Both scripts verify every existing and downloaded jar
  against its pinned SHA-256 (repairing partial or corrupt files,
  downloading atomically), and a build without the dependencies stops
  with a one-line instruction naming the script instead of compiler
  errors.
- A required GitHub Actions check: every pull request runs the full
  test suite on JDK 21 with the same command a contributor runs
  locally, and branch protection on `main` requires it before any
  merge.

### Fixed

- The application grants FlatLaf explicit native access (launcher flag
  on `make run`, manifest attribute in the packaged jar), so JDK 24+
  launches carry no restricted-native-access warning and keep native
  window integration under the stricter future default. The packaged
  jar's manifest also carries the runtime `Class-Path`, so
  `java -jar build/app/JUranometria.jar` now actually launches.
- The README's status, document list, and the development guide's
  sprint-record convention describe the repository as it exists after
  eight releases; a dead `.gitignore` section is gone.

## [0.8.0] - 2026-08-30

Sprint 8 — Pan across the local sky. Reviewed by Codex before release;
the review trail lives in `docs/reviews/`.

### Added

- The chart is now directly draggable: press on the paper and the sky
  position under the pointer follows the hand exactly - anchored to
  the press-time grab for the whole gesture, solved with the real
  projection (never degrees-per-pixel), smooth across RA 0 and honest
  near the poles, where the sky follows as far as a north-up chart
  allows. A real drag departs a searched target into an honest
  coordinate title; clicks and jitter change nothing; field width and
  magnitude ride along untouched; Reset view still returns exactly
  home. An open hand marks the draggable paper, a closed hand the
  live grab.

## [0.7.0] - 2026-08-29

Sprint 7 — Give the wider sky its geography. Reviewed by Codex before
release; the review trail lives in `docs/reviews/`.

### Added

- Regional charts now teach constellation geography: at 12 degrees and
  wider, traditional line figures draw in quiet grey with constellation
  names (Latin, by the atlas convention) placed on each figure's
  visible ink; at 18 degrees and wider, the official IAU boundaries
  join as faint dotted lines - reconstructed true arcs that curve
  smoothly around the poles and across RA 0. Searching M42 and zooming
  out now ends with the whole of Orion named around the still-centred
  nebula. Geography sits under stars, symbols, labels, and the title
  block; the released 1-8 degree pages keep exactly their shipped ink,
  and the M31 reference chart is unchanged.
- The application now bundles reproducible constellation geography:
  the 88 IAU constellation identities (Latin names, genitives,
  abbreviations), traditional line-figure segments, and the official
  IAU boundaries reconstructed along their constant-coordinate B1875
  arcs to within one arcminute - generated offline from pinned,
  checksum-verified sources (d3-celestial, BSD-3-Clause; provenance
  and notices bundled) via make import-constellations. Nothing draws
  yet; the rendering arrives with the constellation layer.

## [0.6.0] - 2026-08-29

Sprint 6 — Reveal the wider sky. Reviewed by Codex before release; the
review trail lives in `docs/reviews/`.

### Added

- Regional zoom: the field width now extends through 12, 18, 24, and 36
  degrees, so zooming out from a searched object progressively reveals
  its celestial neighbourhood - Orion whole around M42, Cassiopeia and
  the Perseus Milky Way around M31 - while the searched target's exact
  position, title, and identity remain the centre through every step.
  Reset still restores exactly M31 at 8 degrees and V 8.0.
- Regional detail policy: at fields wider than 18 degrees, deep-sky
  symbols draw only at their true projected size - the practical-minimum
  clamp no longer inflates hundreds of sub-pixel objects into speckle
  (M13's 36-degree page drops from 256 drawn symbols to 2). Messier
  objects are always drawn, clamped when necessary, and the searched
  target is always drawn and labelled when its type has a chart symbol;
  labels otherwise attach only to Messier objects shown at true size,
  which dissolves the M31/M32/M110 label pile naturally. The 18-degree
  and narrower pages, and the user's star magnitude limit, are untouched.

## [0.5.0] - 2026-08-29

Sprint 5 — Build the local sky. Reviewed by Codex before release; the
review trail lives in `docs/reviews/`.

### Added

- Every deep-sky type now draws its chart symbol from the chart
  conventions: open clusters as dotted circles, globular clusters as
  circles with a central cross, nebulae of every kind as restrained
  light-grey outlined boxes, planetary nebulae as small crossed circles,
  and galaxy pairs and groups as oriented ellipses - so M42, the
  Pleiades, M13, and M57 appear on their charts with their Messier
  labels. Stellar-type NGC entries and associations remain searchable
  but undrawn. The M31 reference chart gains two honest marks (a small
  globular symbol and the NGC 317 pair) and was regenerated and
  visually reviewed.

### Fixed

- The chart title follows the current view instead of always claiming
  the M31 region: a search target titles its chart (for example
  "M 42 · Great Orion Nebula region"), a coordinate recenter titles by
  its position, zooming keeps the target, and reset restores the exact
  M31 default. Search messages now state what is true of the whole
  bundled catalogue.
- Large objects can no longer be silently omitted from a chart: the
  query margin now comes from the pack manifest's declared maximum
  object semi-extent (5.39 degrees, the Large Magellanic Cloud)
  instead of a constant sized for M31, so the LMC, the Hyades, and
  other giants appear whenever their symbols reach into the visible
  frame.

### Added

- The local sky: the application now loads, queries, and searches the
  bundled bright all-sky catalogue pack - the complete sky to stars of
  V 8.0 with 13,371 deep-sky objects - so offline searches such as M42,
  the Pleiades, or 47 Tuc recenter onto complete local charts anywhere,
  including across the RA wrap and near the poles. Scene queries read
  only the tiles a view needs, each verified against its manifest
  checksum at first read, and a missing, corrupt, or incompatible pack
  fails with a clear diagnostic instead of a sparse sky. Non-galaxy
  deep-sky types are searchable and recentre the chart but await their
  chart symbols. The Sprint 3 M31 regional resource retired; the M31
  reference chart reproduces from the pack byte-identically.

## [0.4.0] - 2026-08-29

Sprint 4 — Search and recenter. Reviewed by Codex before release; the
review trail lives in `docs/reviews/`.

### Added

- Search and recenter: a compact toolbar search resolves bundled object
  names, identifiers, and coordinates entirely offline and recentres the
  atlas. The current field width is preserved when the bundled coverage
  allows; otherwise the chart steps to the widest field that remains
  complete, and a result beyond local coverage leaves the chart unchanged
  with a concise message. Reset restores the M31 default and clears the
  search. Windows taller than the honest page letterbox the atlas page on
  the application surface.

## [0.3.0] - 2026-08-29

Sprint 3 — Local catalogue foundation. Reviewed by Codex before release;
the review trail lives in `docs/reviews/`.

### Changed

- Chart scenes are now assembled by querying the local catalogue from the
  current view state and window geometry: zooming, magnitude changes, and
  window resizes issue a fresh bounded query whose radius always covers
  the visible chart corners (including tall or wide windows), while plain
  repaints reuse the assembled scene without touching the catalogue.
- The chart now draws from the bundled regional catalogue generated from
  Tycho-2 and OpenNGC (3204 stars to V 10.0 and 47 galaxies within 10
  degrees of M31) instead of the hand-curated SIMBAD fixture. M31's
  ellipse takes its OpenNGC dimensions (177.8 by 69.7 arcminutes), M110
  now shows its true oriented shape, Messier objects are labelled by
  their Messier names, and fainter catalogue galaxies appear as
  unlabelled symbols.

## [0.2.0] - 2026-08-28

Sprint 2 — Navigate the chart. Reviewed by Codex before release; the
review trail lives in `docs/reviews/`.

### Added

- The first interactive controls: a compact atlas toolbar with zoom in,
  zoom out, and reset view, stepping the field width through 8, 6, 4, 3,
  2, and 1 degrees centred on M31, with a live field-width readout,
  bundled Tabler icons, and controls that disable at the fixture bounds.
- A limiting-magnitude control: fewer/more stars step the stellar limit
  between V 4.0 and V 8.0 in whole magnitudes with immediate chart
  updates; the toolbar readout, title block, and rendered stars always
  agree, and reset restores the complete 8°/V 8.0 default view.

## [0.1.0] - 2026-08-28

Sprint 1 — The first convincing chart. Reviewed by Codex before release;
the review trail lives in `docs/reviews/`.

### Added

- Initial product, chart, architecture, catalogue, development, and Sprint 1
  specifications.
- Application appearance specification based on FlatLaf and bundled Tabler SVG
  icons.
- Executable project foundation: Make-based build targeting Java 21, pinned
  dependency download script (FlatLaf, FlatLaf Extras, JSVG, JUnit console
  runner), and a minimal FlatLaf application window launched on the EDT.
- The first rendered chart: the application window now shows a fixed
  8-degree M31-region star chart on white paper with a restrained frame,
  drawn from the bundled fixture, with a tunable magnitude-to-radius star
  scale and a `make chart-image` target that writes a deterministic
  reference image.
- The complete Sprint 1 chart: M31, M32, and M110 drawn as oriented
  ellipses from their catalogued dimensions and position angles, restrained
  labels, and a formal title block stating target, centre, ICRS J2000,
  field width, limiting magnitude, and orientation. A `--dark` flag runs
  the dark application theme; the chart stays white paper in both.
