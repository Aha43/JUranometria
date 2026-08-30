# Decision: the JUranometria 1.0 product and platform contract

**Sprint 16, issue #143.** Status: proposed for review. This gate
changes no product behavior; it states, in one place, exactly what
1.0 promises — so packaging (#144), the audit (#145), release
automation (#88), and the closing journey (#146) verify a settled
target instead of an implied one.

## What JUranometria 1.0 is

A quiet, fully offline desktop star atlas: white paper, black ink,
the whole sky to V 8.0, named stars, constellation geography, an
equatorial graticule, and physical navigation — search, grab-to-pan,
zoom where you point. Everything below is the promise; everything
not below is implementation detail or explicitly deferred.

## Java baseline

- **The primary downloads include their Java 21 runtime** (the
  amendment below): users of the four platform application images
  install nothing.
- **The portable fallback ZIP requires an installed Java runtime of
  version 21 or later** (any vendor's JDK/JRE distribution); its
  launch helpers must produce a readable diagnostic for a missing or
  too-old Java, not a stack trace collage (#144).
- **Building from source requires a JDK of version 21 or later**;
  sources compile with `--release 21`, so newer JDKs build the same
  bytecode. The build selects its own JDK 21+ toolchain rather than
  trusting `PATH` (#136), with `JAVA_HOME` as the override.

## Platform support — runtime versus contributor, stated separately

**Running the packaged release** is verified on a finite matrix:

- **macOS 14 or later, Apple silicon and Intel** — Apple silicon
  verified by CI (`macos-latest`, arm64) and the maintainer's
  primary machine; **Intel verified by a recorded run on the
  maintainer's Intel Mac** (the machine that produced PR #138's
  toolchain evidence), re-recorded for the 1.0 artifact at #144;
- **Ubuntu 24.04 LTS on x86-64** as the named Linux reference
  (CI `ubuntu-latest`),
- **Windows 11 on x86-64** (CI `windows-latest`),

with the honest wider expectation stated as exactly that: a pure
Java SE 21 desktop application is *expected* to run on other Linux
distributions, but 1.0's verification evidence covers the named
matrix and no claim is made beyond it. Support is
in every case **unpack-and-run** with no Make, Git, Bash,
PowerShell, or source checkout, and paths containing spaces must
work. **The primary artifact on each platform is the self-contained
application image** (the amendment below) launched by its native
launcher; the **portable fallback ZIP** launches with its bundled
helper or `java -jar JUranometria.jar`, which remains that
artifact's authoritative path.

**Building from source** is supported on **macOS and Linux**, and on
**Windows via WSL** — the contributor path needs POSIX sh, GNU Make,
and `curl` (`scripts/download-libs.sh`). A native-Windows
development workflow is **not** part of 1.0 (the PowerShell
bootstrap was retired in Sprint 10 and does not return to decorate a
support claim); Windows *runtime* support is delivered by the
archive, not by a build path.

Not in 1.0, recorded as post-1.0 candidates rather than silent
omissions: native installers (`.dmg`/`.msi`/installer `.exe`), code
signing/notarization, app stores, package registries, and update
checking. (Bundled runtimes moved INTO 1.0 by the amendment below.)

## Fully local operation

After unpacking, **everything works offline, permanently**: search,
charts, star identities, geography, the grid, and About with the
complete bundled-data and resource licensing. **The application makes no network requests at any
time** — no telemetry, no update check, no remote lookup; nothing in
the shipped archive opens a connection. (Development tooling — the
contributor bootstrap, catalogue regeneration, CI, and release
automation — uses the network as tooling does; none of it ships in
or runs from the archive, and the offline promise is about the
application the reader runs.)

## The data contract

1.0 bundles, in the **ICRS/J2000** frame throughout:

- the **bright all-sky pack**: 45,630 Tycho-2-derived stars to
  **V ≤ 8.0** and 13,371 OpenNGC deep-sky objects, whole-sky
  coverage on the fixed 30° tiling;
- **star identities**: 4,805 rows of traditional names, Bayer and
  Flamsteed designations (d3-celestial);
- **constellation geography**: the 88 IAU constellations — names,
  traditional figures, and Delporte's boundaries reconstructed along
  their B1875 arcs;
- every pack carries its checksummed manifest, provenance, and
  notices, surfaced in About.

**Licensing consequence, part of the contract**: the code and
documentation are MIT, but the Tycho-2-derived star tiles are
**CC BY-NC 3.0 IGO**, so **the packaged application is
redistributable and usable non-commercially only** for as long as
that data is bundled. This statement travels with the archive, the
release notes, About, and `LICENSING.md`. The identity and geography
layers are BSD-3-Clause; OpenNGC is CC-BY-SA-4.0.

**The archive also redistributes its runtime libraries**, and their
licences are part of the 1.0 licensing map: **FlatLaf and FlatLaf
Extras (Apache License 2.0)** and **JSVG (MIT)**, each shipped with
its licence text beside the `lib/` directory and listed in
`LICENSING.md`. The JUnit console runner is test-only and never
ships. **The division of licensing surfaces is deliberate**: the
About dialog carries the complete licensing of everything packaged
*inside* the application (code statement, data notices, icon
licence); the runtime-library licences live with the libraries
themselves, in the archive beside `lib/` and in `LICENSING.md` —
About does not restate them.

## Preferences and upgrade from 0.15

- Preferences live in the JDK preferences node `juranometria`:
  `appearance` and the seven `chart.*` option keys.
- **Every 0.15 preference loads into 1.0 unchanged.** A key that a
  newer version added is absent in an older store and loads as its
  documented default; a missing, corrupt, or unknown value always
  means the default — **never a launch failure**. Only the literal
  string `"false"` disables a chart layer; only a confirmed Settings
  dialog persists appearance; `--dark` stays a session-only override
  that never rewrites the stored choice.
- 1.0 makes no schema change: an upgrade is unpack-and-run over the
  same preference node.

## Stable 1.0 behavior

The following are **promised behavior** — changing any of them after
1.0 is a compatibility decision, not a refactor:

- **Projection and orientation**: gnomonic, north up, east left; the
  chart's frame stated in every title block.
- **Navigation**: the discrete field sequence 36°, 24°, 18°, 12°,
  8°, 6°, 4°, 3°, 2°, 1°; magnitude limit 4.0–8.0 in whole steps;
  grab-to-pan with exact reversal within a gesture; pointer-centred
  wheel zoom accepting only exact, reversible steps; centre-
  preserving toolbar/keyboard zoom on the platform menu mask; Home
  restoring the exact default view.
- **The default view**: the M31 region, 8° field, stars to V 8.0,
  all chart layers on (including star labels and the equatorial
  grid).
- **Target semantics**: a searched object titles the chart and
  survives centre-preserving zoom; the first pan or centre-moving
  pointer-zoom clears label and identity together, atomically.
- **Search**: Messier/NGC/IC/common aliases, TYC identifiers,
  coordinates (decimal and sexagesimal), traditional star names by
  prefix, Bayer (Greek or spelled, abbreviation or genitive), and
  Flamsteed forms; ambiguity lists candidates and never silently
  resolves.
- **Chart options**: the seven toggles with their dependency,
  target-exemption, repaint-only, and Restore Defaults contracts.
- **Accessibility surface**: every control and dialog carries an
  accessible name; dialogs are owned, single-instance where
  documented, and close on Escape; the full journeys are operable by
  keyboard plus pointer; both FlatLaf themes are supported with the
  chart itself theme-independent.

**Implementation detail, explicitly not promised**: exact pixel
output (the committed references are internal regression anchors,
deliberately updated when reviews accept a change), solver
tolerances, tile layout, label collision outcomes on any particular
page, and class/package structure.

## Amendment (issue #150): self-contained application images

**The primary 1.0 downloads are platform-specific application
images with the Java runtime included — users do not install
Java.** Measured prototypes (this gate, `make`-driven
`scripts/build-app-image.sh` + the `app-image` workflow) settle the
shape:

- **Four artifacts**, each built natively on its own platform,
  never cross-packaged: macOS Apple silicon and macOS Intel
  (`JUranometria.app`), Windows x86-64 (`JUranometria.exe` in an
  unpacked directory), Linux x86-64 (native launcher in an unpacked
  directory). The Intel image builds on the `macos-15-intel` runner
  and is additionally exercised on the maintainer's physical Intel
  Mac.
- **The mechanism**: JDK 21 `jpackage --type app-image` over the
  built JAR and its three libraries (non-modular, `--input` +
  `--main-jar`), with an explicit measured module list —
  `java.base, java.desktop, java.logging, java.prefs` by jdeps,
  resolving with its transitive closure to exactly six modules
  (`+ java.datatransfer, java.xml`), asserted from the runtime's
  `release` file. The launcher carries
  `--enable-native-access=ALL-UNNAMED`, passes `--dark` through,
  and is working-directory independent. Measured on Apple silicon:
  **76 MB unpacked, 25 MB compressed** — the trimmed runtime is
  appropriately restrained.
- **The runtime is pinned Temurin 21** (exact version recorded in
  the workflow pin and asserted against each image's `release`
  file), and each artifact carries a `build-info.txt` recording
  version, modules, packager, and runtime.
- **Runtime licensing travels**: OpenJDK/Temurin is GPLv2 with the
  Classpath Exception; the jlink runtime's complete generated
  `legal/` notice tree ships inside every image (asserted), and the
  distribution licensing map names it.
- **The packaged headless smoke path** is a second, inner launcher
  (`juranometria-smoke`) that renders the reference chart through
  the bundled runtime alone with no system Java on the PATH.
  **Enforcement is per-platform-honest**: on every cell the two
  same-runner builds' renders must be byte-identical to each other,
  and the two macOS architectures (arm64 and Intel) must render
  byte-identical smokes to each other under the same pinned runtime
  — a cross-architecture determinism claim. Equality with the
  committed M31 reference is recorded per environment but not
  required across environments: text rasterization varies between
  JDK builds and OS font stacks, and the contract already excludes
  exact pixels from its promises (it holds on the maintainer's
  machines, where it is verified).
- **Reproducibility, measured honestly**: the macOS image is
  byte-identical across two same-machine builds; every CI cell
  builds twice and records identical-or-not in the run summary. The
  release contract is the narrower honest one — asserted contents,
  pinned inputs and tool versions, one-build/many-consumer
  verification, and published SHA-256 — with byte-identity reported
  where it holds rather than promised universally.
- **The icon** is the Tabler `north-star` glyph (MIT, the pinned
  v3.46.0 the toolbar already uses), drawn in chart ink on the
  atlas's paper inside a quiet rounded square — a restrained
  identity, not a branding project. Generator and assets live under
  `packaging/icon/`.
- **Unsigned, stated plainly**: the images are not signed or
  notarized. macOS Gatekeeper will warn on first launch
  (right-click → Open, or approve under System Settings > Privacy &
  Security); Windows SmartScreen may interpose (More info → Run
  anyway). The launch documentation says exactly this and never
  implies trusted distribution. Installers, signing, notarization,
  update services, and app stores remain post-1.0.
- **The portable Java-dependent ZIP remains published as the
  fallback** for users who prefer their own Java 21+; it keeps its
  `java -jar` verification. The platform matrix, launch-diagnostics
  and offline promises above apply to the images; the ZIP's Java
  baseline section continues to apply to the fallback.

## The release artifacts, at contract level

**Five artifacts per release** (shapes finalized by #144):

1–4. **The four platform application images** — `JUranometria.app`
(macOS Apple silicon; macOS Intel), and the unpacked
`JUranometria/` directory with its native launcher (Windows x86-64;
Linux x86-64) — each zipped **with `build-info.txt` and the
unsigned-launch `README.txt` beside the image**. Inside every
image: the application JAR and its libraries under `app/`, the
pinned six-module Temurin runtime with its complete generated
`legal/` notice tree, the inner `juranometria-smoke` launcher, and
the application icon. The complete packaged licensing inventory —
the About summary, every data notice and licence text, and the icon
licence inside the JAR; a `legal/` directory per runtime module —
is **mechanically asserted at build time on every platform**.

5. **The portable fallback ZIP**:

```
JUranometria-X.Y.Z/
  JUranometria.jar        launchable via java -jar (Class-Path manifest)
  lib/…                   the pinned runtime dependencies
  juranometria / .bat     restrained launch helpers (POSIX sh, Windows batch)
  LICENSE  LICENSING.md   MIT code licence and the full licensing map
  licenses/…              every data notice and licence text, plus the
                          runtime-library texts (Apache-2.0, MIT)
  README (launch note)    download → unpack → run, Java requirement,
                          troubleshooting for absent/old Java
```

All five are built by one command each from a clean checkout,
contents asserted (stale or undeclared files fail), and verified by
CI on their platforms before any release. Interactive
About/preferences journeys through the native images are the
closing packaged journey's work (#146) on real machines; CI proves
the images' full startup — which itself verifies every bundled
pack's checksums on load — plus relaunch, and the mechanical
inventory above.

## Classifying Sprint 16 discoveries

Anything found during stabilization is labelled at discovery:

- **1.0 blocker**: contradicts a promise above (wrong claim, broken
  upgrade, inaccessible control, missing notice, non-working
  supported platform);
- **post-1.0**: everything else — recorded as an issue, never fixed
  silently inside the release sprint.

## Consequences

- README's introduction and requirements are aligned with this
  contract in the same change (the "planned application" wording
  retires; runtime and contributor support are stated separately).
- #136/PR #138 may merge once this gate confirms the Java 21+
  promise it implements; #97 refreshes CI under the same required
  check; #144 builds and verifies the archive on the named matrix;
  #145 audits the application against precisely this document; #88
  automates the release of the verified artifact; #146 closes with
  the packaged acceptance journey.
- This gate changes no product behavior; the M31 reference is
  byte-identical.
