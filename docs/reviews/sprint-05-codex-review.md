# Sprint 5 Codex review

Reviewed 2026-08-29 at PR #47 head `9e6228f`, before the proposed
0.5.0 release.

## Result

Sprint 5 establishes a convincing all-sky data foundation: the
measured pack is small, reproducible, licensed, integrity checked,
spatially queried, and searchable offline. The regional resource has
been retired without changing the M31 reference chart. Three findings
remain before 0.5.0 should be cut.

## Findings

### P1 — Derive the object query margin from the all-sky pack

`SceneAssembler.OBJECT_EXTENT_MARGIN_DEGREES` remains 1.5° and its
comment still states that M31 is the largest catalogued object. That
was true for the regional extract, but it is false for the all-sky
pack. The pack contains, among others:

- `ESO056-115` / Large Magellanic Cloud: 646′ × 550′, requiring about
  5.38° from centre to major-axis edge;
- `NGC 292` / Small Magellanic Cloud: 299.92′ × 179.89′, requiring
  about 2.50°;
- several clusters and nebulae larger than M31.

The catalogue filters rows by centre position. A narrow chart whose
edge crosses one of these objects can therefore omit it when its centre
is more than 1.5° beyond the visible corner, even though the ellipse
should overlap the page. This breaks the sprint's complete-chart claim
and becomes more visible once additional DSO types receive symbols.

Make the maximum required semi-extent machine-readable in the pack
manifest or have the catalogue perform extent-aware selection. The
assembler's query margin and tests must cover the actual largest drawn
object rather than a retired regional assumption. Include a regression
with the LMC or another object larger than M31 whose centre lies outside
the old query cone while its symbol intersects the viewport.

### P1 — Do not label all-sky charts and search failures as M31 data

`Atlas.TITLE` is fixed to `M31 · Andromeda Galaxy region`, so every
recentred scene carries that title. Searching M42 produces correct
Orion coordinates under an explicitly false M31 title. `SearchField`
also still says `No match in the bundled M31-region data` and retains
the obsolete regional coverage message.

This is data presentation, not cosmetic polish: the formal title block
is meant to state which chart the user is looking at. Before release,
make the scene title follow the selected target or use an honest neutral
all-sky title when no target identity is available. Update the search
messages for all-sky coverage and test an M42 scene plus an unmatched
query so regional copy cannot return.

### P1 — Complete the flagship M42 journey before releasing 0.5.0

Search finds M42 and recentres on its exact catalogue position, but
`ChartRenderer.hasSymbol` draws and labels only `GALAXY`. M42 is
`CLUSTER_WITH_NEBULA`, so the chart provides no mark or label at its
centre. A successful object search that lands on an invisible target is
not yet a complete find-and-go journey.

Issue #46 already describes the proper symbol work and should be brought
into Sprint 5 before the release. This is not a request for general UI
polish: it is the minimum rendering needed to expose the all-sky DSO data
that this sprint deliberately added. At minimum, every searchable DSO
type needs a restrained fallback symbol and selected Messier targets
must be identifiable; preferably implement the symbol language already
specified in `docs/chart-conventions.md`. The intentional reference-image
change should be rendered and visually reviewed rather than preserving
the old image by hiding new catalogue content.

## Verification

- `make test`: 137/137 passed.
- `make chart-image`: byte-identical to the committed reference,
  SHA-256 `021a0e1e1cafbe95ce789d102d5ff7fe6c78911bb49c3f648a3e687068aa68aa`.
- `make jar`: 1.2 MiB packaged jar.
- `git diff --check`: clean before this review document was added.
- Loader scrutiny covered manifest validation, lazy checksums, missing
  and corrupt tiles, deterministic ordering, cache locality, RA wrap,
  poles, optional DSO measurements, and all-sky search construction.

## Sprint questions

The data architecture is ready: ordinary charting is local, queries read
only intersecting tiles, search cost is acceptable at this depth, and
catalogue provenance and integrity are machine-checkable. Lazy checksum
verification is acceptable; in the current application, construction of
the all-sky search index naturally reads and verifies every tile during
startup anyway.

The next data priorities remain star names and constellation geography,
followed by measured consideration of deeper optional packs. First,
0.5.0 should present the bright all-sky data honestly: query every drawn
object that can overlap a page, name the current chart truthfully, and
show the object a successful DSO search selected.
