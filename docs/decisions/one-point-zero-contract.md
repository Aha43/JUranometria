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

- **Running the released application requires a Java runtime of
  version 21 or later** (any vendor's JDK/JRE distribution; the
  application uses only the Java SE platform plus its bundled
  libraries).
- **Building from source requires a JDK of version 21 or later**;
  sources compile with `--release 21`, so newer JDKs build the same
  bytecode. The build selects its own JDK 21+ toolchain rather than
  trusting `PATH` (#136), with `JAVA_HOME` as the override.
- No JRE is bundled; pointing users at a Java install is part of the
  documented launch path, and a missing or too-old Java must produce
  a readable diagnostic, not a stack trace collage (#144).

## Platform support — runtime versus contributor, stated separately

**Running the packaged release** is supported on:

- **macOS** (Apple silicon and Intel),
- **Linux** (x86-64 desktop distributions with a display),
- **Windows 11**,

in every case as **unpack-and-run**: extract the release archive,
launch with the bundled helper or `java -jar JUranometria.jar` — no
Make, Git, Bash, PowerShell, or source checkout, and paths
containing spaces must work. `java -jar` is the authoritative launch
path on all three systems; helpers (POSIX shell, Windows batch) are
conveniences.

**Building from source** is supported on **macOS and Linux**, and on
**Windows via WSL** — the contributor path needs POSIX sh, GNU Make,
and `curl` (`scripts/download-libs.sh`). A native-Windows
development workflow is **not** part of 1.0 (the PowerShell
bootstrap was retired in Sprint 10 and does not return to decorate a
support claim); Windows *runtime* support is delivered by the
archive, not by a build path.

Not in 1.0, recorded as post-1.0 candidates rather than silent
omissions: native installers (`.app`/`.dmg`/`.msi`/`.exe`), code
signing/notarization, bundled runtimes, app stores, package
registries, and update checking.

## Fully local operation

After unpacking, **everything works offline, permanently**: search,
charts, star identities, geography, the grid, About and its complete
licensing text. The application makes **no network requests at any
time** — there is no telemetry, no update check, no remote lookup.
The only network activity in the whole project is the contributor
bootstrap (`download-libs.sh`) and the maintainer-only catalogue
regeneration scripts, neither of which ships in the archive.

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

## The release artifact, at contract level

One deterministic **archive per release** (shape finalized by #144):

```
JUranometria-X.Y.Z/
  JUranometria.jar        launchable via java -jar (Class-Path manifest)
  lib/…                   the pinned runtime dependencies
  juranometria / .bat     restrained launch helpers (POSIX sh, Windows batch)
  LICENSE  LICENSING.md   MIT code licence and the full data licensing map
  NOTICE…                 every bundled data notice, including the
                          non-commercial statement
  README (launch note)    download → unpack → run, Java requirement,
                          troubleshooting for absent/old Java
```

Built by one command from a clean checkout; contents asserted
against the manifest (stale or undeclared files fail); verified by
CI on macOS, Linux, and Windows before any release.

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
