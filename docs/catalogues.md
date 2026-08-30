# Catalogue strategy

## Decisions

The Sprint 3 decision on concrete sources, licensing, magnitude
semantics, regional coverage, and the reproducible import contract is
recorded in [catalogue sources](decisions/catalogue-sources.md):
Tycho-2 (CDS I/259) for stars with Johnson V derived from BT/VT, and
OpenNGC (pinned release) for NGC/IC objects - originally over a
10-degree region around M31, extended in Sprint 5 to the bundled
all-sky bright-sky pack (V ≤ 8.0 stars and all OpenNGC objects in 72
tiles). The Sprint 6 regional zoom to 36-degree fields draws from the
same pack at the same depth ([regional
zoom](decisions/regional-zoom.md)). Gaia DR3 remains the intended
source for deeper star packs in a later sprint.

## Principle

Normal chart drawing should not depend on a public service being available.
Bundle or preprocess the data required at each supported scale, then use public
services selectively for discovery and future catalogue-building tools.

## Candidate sources

| Need | Candidate | Intended use |
|---|---|---|
| Named-object resolution | SIMBAD | Optional lookup for names absent locally |
| Precise stars | Gaia DR3 | Source for a reduced, preprocessed star catalogue |
| Catalogue discovery | VizieR | Access to specialist catalogues when needed |
| NGC/IC deep-sky objects | OpenNGC | Bundled, normalized DSO data |
| Constellation geometry | Public IAU-derived datasets | Bundled lines and boundaries |

Public-service terms, attribution requirements, query limits, and redistribution
licenses must be checked before data is added to a release.

## Staged data plan

### Stage 1: hand-curated M31 fixture

Store only enough stars and DSOs to design one chart. Keep it human-readable
and reviewable. It is a test fixture, not the beginning of a custom catalogue.

### Stage 2: reproducible preprocessing

Add a separate import tool that downloads or reads a pinned source release,
normalizes required fields, and emits a compact application resource. Generated
catalogue data must be reproducible and carry source/version metadata.

### Stage 3: spatially indexed local catalogue

Partition stars into sky tiles so a viewport reads only relevant records.
Choose the index and binary encoding after measuring realistic datasets. Avoid
loading all Gaia rows or issuing a remote query during each repaint.

Stages 1 and 2 are complete: the bundled regional resources are generated
reproducibly (see the decision document) and chart scenes query them
through the `Catalogue` boundary on view-state and window changes, never
during painting.

Stage 3 is complete:
[the all-sky tiling decision](decisions/all-sky-tiling.md) chose, from
real measurements, one built-in bright pack at V ≤ 8.0 (about 2.5 MiB)
on a fixed 30-degree RA/Dec grid of 72 tiles with a plain-properties
manifest, conservative padded tile selection, and home-tile uniqueness
so queries cannot duplicate — and the application now loads, queries,
and searches it: scene queries read only intersecting tiles (verified
against their manifest checksums at first read), search spans the
whole pack, and the Sprint 3 regional resource has retired. Measured:
about 33 ms to the first chart, 67 ms to load the complete pack for
search, 0.2 ms warm scene queries, 27 MiB of heap. Deeper packs remain
future options the same shape can carry.

From Sprint 13 a companion **star-identity pack** rides beside the
bright pack ([the star-identity decision](decisions/star-identity.md)):
4,805 rows of traditional names, Bayer designations, Flamsteed
numbers, and constellation memberships (d3-celestial, BSD-3-Clause),
joined to the pack's stars by Hipparcos number through the raw
Tycho-2 catalogue at import time and attached to star records at
load - so scenes carry structured identities as data, local search
answers to names and designations fully offline, and no remote
name-resolution service is needed for the bundled sky. Measured:
27 ms to load the identities, ~25 MiB total heap unchanged in order.

## Minimum data fields

Stars initially need an identifier, right ascension, declination, and a
brightness value. Gaia G magnitude is sufficient for the prototype, although
it is not visual V magnitude.

Deep-sky objects initially need identifiers and aliases, type, right ascension,
declination, apparent major/minor dimensions, position angle when known, and a
label priority.

## Network behaviour

Name resolution may use a remote service on an explicit search action. Drawing,
panning, zooming, printing, and reopening a known chart should remain local.
Failures must leave the atlas usable and explain that name resolution, rather
than chart rendering, is unavailable.

