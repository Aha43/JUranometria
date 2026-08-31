# Licensing

JUranometria's **code and documentation** are released under the MIT
license — see [`LICENSE`](LICENSE), which carries the unaltered MIT
text and covers everything in this repository except the bundled
third-party resources listed below. Each bundled resource keeps its
own license, with its notice shipped beside the data and packaged into
the application.

## Bundled resources and their licenses

| Resource | License | Notice |
|---|---|---|
| Tabler icons (`src/resources/icons/*.svg`) | MIT | [`src/resources/icons/LICENSE`](src/resources/icons/LICENSE), listed in [`ICONS.md`](src/resources/icons/ICONS.md) |
| Bright-sky pack star tiles (`src/resources/catalog/bright-sky/tiles/*/stars.csv`) | **CC BY-NC 3.0 IGO** — may not be used commercially | [`src/resources/catalog/bright-sky/NOTICE-tycho2.md`](src/resources/catalog/bright-sky/NOTICE-tycho2.md) |
| Constellation-geography pack (`src/resources/geo/constellations/*.csv`) | BSD-3-Clause | [`src/resources/geo/constellations/NOTICE-constellations.md`](src/resources/geo/constellations/NOTICE-constellations.md), full licence text beside it |
| Bright-sky pack DSO tiles (`src/resources/catalog/bright-sky/tiles/*/dsos.csv`) | CC-BY-SA-4.0 | [`src/resources/catalog/bright-sky/NOTICE-openngc.md`](src/resources/catalog/bright-sky/NOTICE-openngc.md), full text beside it |
| Star-identity pack (`src/resources/catalog/star-identities/star-identities.csv`) | BSD-3-Clause | [`src/resources/catalog/star-identities/NOTICE-star-identities.md`](src/resources/catalog/star-identities/NOTICE-star-identities.md), full licence text beside it |

## Redistributed runtime libraries

The release archive redistributes the pinned runtime libraries
beside the application JAR; each keeps its own licence:

| Library | License |
|---|---|
| FlatLaf and FlatLaf Extras (look and feel) | Apache License 2.0 |
| JSVG (SVG icon rendering) | MIT |

The JUnit console runner is a test-only dependency and is never
redistributed. Licence texts for the redistributed libraries ship in
the release archive beside the `lib/` directory.

## The bundled Java runtime

The four self-contained platform downloads (macOS Apple silicon,
macOS Intel, Windows x86-64, Linux x86-64) include a trimmed
**Eclipse Temurin 21** runtime so that readers install no Java:

| Component | License |
|---|---|
| Eclipse Temurin / OpenJDK runtime, `jlink`-trimmed | **GPLv2 with the Classpath Exception** |

The Classpath Exception is what keeps JUranometria's own MIT code
MIT while it travels with that runtime. The runtime's complete
generated `legal/` notice tree - one directory per included module -
ships inside every application image and is asserted at build time.
The portable archive contains no runtime and is unaffected: it runs
on the Java the reader already has.

The generated catalogue resources are reproduced from pinned upstream
inputs; see
[`docs/decisions/catalogue-sources.md`](docs/decisions/catalogue-sources.md)
and the generated `PROVENANCE.md` beside the data.

## The non-commercial exception, stated plainly

JUranometria's code and documentation are MIT licensed. Releases
currently bundle a Tycho-2-derived star catalogue under
CC BY-NC 3.0 IGO. That resource may not be used
commercially. Commercial redistribution or use of the packaged
application therefore requires removing or replacing the
Tycho-derived catalogue.

In consequence: the **packaged application** is redistributable
non-commercially only, for as long as it bundles that resource. The
restriction does not relicense the Java code — the code remains MIT —
but it affects practical use of the complete release, so JUranometria
is not described as unrestricted open source. A commercial use of the
complete package requires re-importing the star data from a source
with compatible terms; the import contract isolates the source
adapter so only it would change.

## The in-application About surface

The running application presents this licensing to end users via
Help - About JUranometria: a compact packaged summary
(`src/resources/about/licensing-summary.txt`) plus the full bundled
notice and licence texts, all offline. A test
(`AboutDialogTest.theSummaryAgreesWithTheRepositoryLicensingDocument`)
keeps the summary and this document in agreement.
