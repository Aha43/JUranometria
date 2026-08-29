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
| Bright-sky pack DSO tiles (`src/resources/catalog/bright-sky/tiles/*/dsos.csv`) | CC-BY-SA-4.0 | [`src/resources/catalog/bright-sky/NOTICE-openngc.md`](src/resources/catalog/bright-sky/NOTICE-openngc.md), full text beside it |

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
