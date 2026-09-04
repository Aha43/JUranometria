# Decision: the public atlas gallery, decided and curated

Decided 2026-09-04 for Sprint 27, issue #252. The gallery is a
documentation surface, not an application feature: the chart
remains a chart, each module keeps a room of its own, and
publication (issue #253) begins only after this curation is
approved and merged.

## The one manifest

`docs/gallery/manifest.json` is the single record: per slide a
title, a reader-language caption, sky region and field where
meaningful, an ink attribution stating whether ink is core chart
or which module contributes it, the source artifact path, the
producing release/tree, and alt text written for what matters. The
HTML is **derived** from the manifest by `make gallery`
(`GalleryMain`), so the pages and the record cannot drift: the
gallery test regenerates them into scratch and holds the committed
pages byte-identical, and a curation change is a manifest change.

## The selection: nine slides, three rooms

**Core chart** — the ordinary atlas as itself, four pages spanning
the atlas's range: the canonical released Andromeda page
(`docs/reference/m31-stars.png`, the file CI holds renders to),
wide Orion with its figure and named neighbours, the Southern
Cross under strongly curved grid parallels, and the celestial pole
with the parallels closed into rings. All renderer-drawn
production output, regenerated and compared against their
generators by `make evidence-contracts`.

**On This Page** — the inventory panel (labelled UI) whose
highlighted row says *NGC 206 · not recorded · no symbol*, and a
chart slide of working crosses on the Andromeda page: plain
crosses over M 32 and M 110, and the ringed lead cross standing
alone on NGC 206 — an object the chart records but draws nothing
for, which is what a working cross is for.

**Place and Time** — the dialog (labelled UI, a session
photograph: Oslo, east-positive 10.752, one frozen equinox
instant), the meridian running through the zenith ring inside the
Big Dipper's bowl, and the dashed mathematical horizon crossing a
field toward Cygnus. The chart slides use the same place and
instant the dialog photograph shows, so the room tells one story.

## The rejection that changed the rule

The obvious sources for the module rooms were rejected, and the
rejection is the decision's core: the place-and-time ink study's
`page-*.png` files predate #227 and paint a **candidate**
reference treatment — sampled polylines, its own strokes and label
placement — not the shipped `ReferenceInk`; and no committed
artifact showed working crosses at all, because crosses and
reference ink exist only in the live component's paint path. The
issue forbids promoting study previews as application output, so
the module chart slides are composed by `GalleryPageMain`: a real
`ChartComponent` with the real overlay registry, the real
`MeridianModule` and the real ink painters, painted offscreen —
the exact composition a reader's window performs. The slides live
under `docs/studies/gallery/` with the renderer-drawn contract,
regenerated and compared like every other production render.

The rule this leaves behind: **a gallery chart slide is either a
committed production render or composed by a registered generator
through production painters; a UI image appears only labelled
"Application UI", in its caption and its ink attribution.** The
gallery test executes both halves against the artifact classes of
`docs/decisions/test-evidence.md`, so a silently promoted mock-up
fails the suite, not a review.

Black sky is deliberately absent from this first set: the issue
scopes the gallery to what 1.6.0 released and was filed before
1.7.0 published. A black-sky room or slide is a natural later
curation, by manifest change, not a silent scope growth here.

## Interaction and visual contract

- One index, three sections with their room names; every slide a
  static page of its own with a direct URL, fully readable with
  no JavaScript — the previous/next controls are plain links.
- The whole script is `gallery.js`: arrow keys follow the page's
  own prev/next links, nothing more. No autoplay exists to force.
- Semantic landmarks and controls, visible focus outlines, alt
  text from the manifest, fluid single-column layout at narrow
  widths, and `prefers-reduced-motion` honoured by a stylesheet
  that animates nothing and forbids itself the possibility.
- No framework, analytics, cookies, remote fonts, trackers, or
  third-party assets — executable: the gallery test refuses any
  remote script, stylesheet or image reference and any `http`
  reach inside the committed assets. Downloads link to the GitHub
  release rather than duplicating artifacts.
- The presentation wears the atlas's manner: white paper, dark
  ink, quiet greys, system faces, frames instead of decoration.

## Evidence

Every selected image was inspected at native size during
curation. The responsive contact captures — the index and a slide
page, desktop and narrow, in a real browser served locally — are
committed under `docs/studies/gallery/` as digest-pinned captured
evidence (`screenshot-gallery-*`), captured 2026-09-04 on the
maintainer's machine with the browser chrome cropped away. The
keyboard route (arrow keys crossing slide pages) was exercised
live in the same session.

## Consequences

- #253 publishes exactly these pages with GitHub Pages and
  nothing else; the downloads keep pointing at the release.
- A new slide arrives by a manifest change plus, where the source
  is new, a registered generator or a reviewed committed artifact
  — never by an unregistered file.
- Production application behaviour and chart/reference bytes are
  untouched by this issue, held by the ordinary suite.
